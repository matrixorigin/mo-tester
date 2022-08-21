package io.mo;

import io.mo.cases.TestScript;
import io.mo.constant.COMMON;
import io.mo.db.Debugger;
import io.mo.db.Executor;
import io.mo.result.TestReport;
import io.mo.util.ResultParser;
import io.mo.util.RunConfUtil;
import io.mo.util.ScriptParser;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;

public class Tester {
    private static final TestReport report = new TestReport();
    private static final Logger LOG = Logger.getLogger(Tester.class.getName());
    private static String[] includes = null;
    private static String[] excludes = null;

    public static void main(String[] args){

        String path = RunConfUtil.getPath();
        String method = RunConfUtil.getMethod();
        int rate = RunConfUtil.getRate();

        //parse the paras
        if(args != null){
            for (String arg : args) {

                //get path
                if (arg.startsWith("path")) {
                    path = arg.split("=")[1];
                }

                //get method
                if (arg.startsWith("method")) {
                    method = arg.split("=")[1];
                }

                //get sucess rate
                if (arg.startsWith("rate")) {
                    rate = Integer.parseInt(arg.split("=")[1]);
                }

                //get ignore
                if (arg.equalsIgnoreCase("ignore")) {
                    COMMON.IGNORE_MODEL = true;
                }

                //get includes
                if (arg.startsWith("include")) {
                    includes = arg.split("=")[1].split(",");
                }

                //get excludes
                if (arg.startsWith("exclude")) {
                    excludes = arg.split("=")[1].split(",");
                }

                //get nometa info
                if (arg.equalsIgnoreCase("nometa")) {
                    COMMON.IS_COMPARE_META = false;
                }

                //get check info
                if (arg.equalsIgnoreCase("check")) {
                    method = "check";
                }
            }
        }

        if(path == null){
            LOG.error("The scripts file path is not configured,pleas check the config file conf/run.yml.");
            return;
        }

        if(method == null){
            LOG.error("The method of execution is not configured,pleas check the config file conf/run.yml.");
            return;
        }

        File file = new File(path);

        if(!file.exists()){
            LOG.error("The scripts file path: "+ path +" does not exist,please check.");
            return;
        }


        if(method.equalsIgnoreCase("run")){
            LOG.info("The method is [run],now start to run the scripts in the path["+ path +"].");
            run(file);
            LOG.info("All the scripts in the path["+ path +"] have been excuted.Now start to create the test report.");
            report.write();
            LOG.info("The test report has been generated in files[report.txt,report.xml].");

            if(report.getRate() < rate){
                LOG.error("The execution success rate is "+ report.getRate()+"%, and less than config value "+ rate +"%,this test fail.");
                System.exit(1);
            }else {
                LOG.info("The execution success rate is "+ report.getRate()+"%, and not less than config value "+ rate +"%,this test succeed.");
                System.exit(0);
            }
        }

        if(method.equalsIgnoreCase("debug")){
            debug(file);
        }

        if(method.equalsIgnoreCase("check")){
            check(file);
        }

        if(method.equalsIgnoreCase("genrs")){
            LOG.info("The method is [genrs],now start to generate the checkpoints in the path["+ path +"].");
            generateRs(file);
            //LOG.info("ALL the results in the path["+path+"] have been generated or updated.");
        }

        if(!method.equalsIgnoreCase("genrs")
                &&!method.equalsIgnoreCase("debug")
                &&!method.equalsIgnoreCase("run")
                &&!method.equalsIgnoreCase("check")){
            LOG.info("The method is ["+ method +"] can not been supported.Only[run,debug,genrs] can be supported.");
        }

    }

    public static void run(File file){
        if(file.isFile()){
            if(file.getName().endsWith(".result"))
                return;
            
            if(isInclude(file.getName())) {
                ScriptParser.parseScript(file.getPath());
                TestScript script = ScriptParser.getTestScript();
                Executor.run(script);
                report.collect(script);
            }
            return;
        }
        File[] fs = file.listFiles();
        sort(fs);
        assert fs != null;
        for (File f : fs) {
            run(f);
        }
    }

    public static void generateRs(File file){
        if(file.isFile()){
            if(file.getName().endsWith(".result"))
                return;
            
            if(isInclude(file.getName())) {
                ScriptParser.parseScript(file.getPath());
                TestScript script = ScriptParser.getTestScript();
                if(Executor.genRS(script))
                    LOG.info("The results for the test script file["+file.getPath()+"] have been generated or updated successfully.");
                else
                    LOG.info("The results for the test script file["+file.getPath()+"] have been generated or updated failed.");
            }
            return;
        }
        File[] fs = file.listFiles();
        sort(fs);
        assert fs != null;
        for (File f : fs) {
            generateRs(f);
        }
    }

    public static void debug(File file){
        if(file.isFile()){
            if(file.getName().endsWith(".result"))
                return;
            
            if(isInclude(file.getName())) {
                ScriptParser.parseScript(file.getPath());
                TestScript script = ScriptParser.getTestScript();
                Debugger.run(script);
            }
            return;
        }
        File[] fs = file.listFiles();
        assert fs != null;
        for (File f : fs) {
            debug(f);
        }
    }

    public static void check(File file){
        if(file.isFile()){
            if(file.getName().endsWith(".result"))
                return;
            if(isInclude(file.getName())) {
                ScriptParser.parseScript(file.getPath());
                TestScript script = ScriptParser.getTestScript();
                ResultParser.check(script);
            }
            return;
        }
        File[] fs = file.listFiles();
        assert fs != null;
        for (File f : fs) {
            check(f);
        }
    }

    public static void sort(File[] files){
        List<File> fileList = Arrays.asList(files);
        fileList.sort((Comparator) (o1, o2) -> {
            File f1 = (File) o1;
            File f2 = (File) o2;
            if (f1.isDirectory() && f2.isFile())
                return -1;

            if (f1.isFile() && f2.isDirectory())
                return 1;

            return f1.getName().compareTo(f2.getName());
        });
    }

    public static boolean isInclude(String name){
        if(includes == null){
            if(excludes == null)
                return true;
            for (String exclude : excludes) {
                if (name.contains(exclude))
                    return false;
            }
            return true;
        }

        for (String include : includes) {
            if (name.contains(include))
                return true;
        }
        return false;
    }
}
