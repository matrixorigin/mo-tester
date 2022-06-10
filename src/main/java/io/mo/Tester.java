package io.mo;

import io.mo.cases.TestScript;
import io.mo.cases.TestSuite;
import io.mo.constant.COMMON;
import io.mo.db.Debugger;
import io.mo.db.Executor;
import io.mo.result.TestReport;
import io.mo.util.RunConfUtil;
import io.mo.util.ScriptParser;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;

public class Tester {
    private static ArrayList<TestSuite> suites = new ArrayList<TestSuite>();

    private static TestReport report = new TestReport();

    private static Logger LOG = Logger.getLogger(Tester.class.getName());

    private static String path = null;
    private static String method = null;
    private static String type = null;
    private static int rate = 100;

    private static String[] includes = null;
    private static String[] excludes = null;

    public static void main(String[] args){

        path = RunConfUtil.getPath();
        method = RunConfUtil.getMethod();
        type = RunConfUtil.getType();
        rate = RunConfUtil.getRate();

        //parse the paras
        if(args != null){
            for(int i = 0; i < args.length; i++){

                //get path
                if(args[i].startsWith("path")){
                    path = args[i].split("=")[1];
                }

                //get method
                if(args[i].startsWith("method")){
                    method = args[i].split("=")[1];
                }

                //get type
                if(args[i].startsWith("type")){
                    type = args[i].split("=")[1];
                }

                //get sucess rate
                if(args[i].startsWith("rate")){
                    rate = Integer.parseInt(args[i].split("=")[1]);
                }

                //get ignore
                if(args[i].equalsIgnoreCase("ignore")){
                    COMMON.IGNORE_MODEL = true;
                }

                //get includes
                if(args[i].startsWith("include")){
                    includes = args[i].split("=")[1].split(",");
                }

                //get excludes
                if(args[i].startsWith("exclude")){
                    excludes = args[i].split("=")[1].split(",");
                }
                
                //get nometa info
                if(args[i].equalsIgnoreCase("nometa")){
                    COMMON.IS_COMPARE_META = false;
                }
            }
        }

        if(path == null){
            LOG.error("The scripts file path is not configured,pleas check the config file conf/run.yml.");
            return;
        }

        /*if(version == null){
            LOG.error("The kundb version is not configured,pleas check the config file conf/run.yml.");
            return;
        }*/

        if(method == null){
            LOG.error("The method of execution is not configured,pleas check the config file conf/run.yml.");
            return;
        }

        File file = new File(path);

        if(!file.exists()){
            LOG.error("The scripts file path: "+path+" does not exist,please check.");
            return;
        }


        if(method.equalsIgnoreCase("run")){
            LOG.info("The method is [run],now start to run the scripts in the path["+path+"].");
            run(file,type);
            LOG.info("All the scripts in the path["+path+"] have been excuted.Now start to create the test report.");
            report.write();
            LOG.info("The test report has been generated in files[report.txt,report.xml].");

            if(report.getRate() < rate){
                LOG.error("The execution success rate is "+ report.getRate()+"%,and less than config value "+rate+"%,this test fail.");
                System.exit(1);
            }else {
                LOG.error("The execution success rate is "+ report.getRate()+"%,and not less than config value "+rate+"%,this test succeed.");
                System.exit(0);
            }
        }

        if(method.equalsIgnoreCase("debug")){
            debug(file,type);
        }

        if(method.equalsIgnoreCase("genrs")){
            LOG.info("The method is [genrs],now start to generate the checkpoints in the path["+path+"].");
            generateRs(file);
            //LOG.info("ALL the results in the path["+path+"] have been generated or updated.");
        }

        if(!method.equalsIgnoreCase("genrs")&&!method.equalsIgnoreCase("debug")&&!method.equalsIgnoreCase("run")){
            LOG.info("The method is ["+method+"] can not been supported.Only[run,debug,genrs] can be supported.");
            return;
        }

    }

    public static void run(File file,String type){
        if(file.isFile()){
            if(type.equalsIgnoreCase("script")){
                if(isInclude(file.getName())) {
                    ScriptParser.parseScript(file.getPath());
                    TestScript script = ScriptParser.getTestScript();
                    Executor.run(script);
                    report.collect(script);
                }
            }

            if(type.equalsIgnoreCase("suite")){
                if(isInclude(file.getName())) {
                    ScriptParser.parseSuite(file.getPath());
                    ArrayList<TestSuite> suites = ScriptParser.getTestSuites();
                    Executor.run(suites, file.getPath());
                    report.collect(suites);
                }
            }
            return;
        }
        File[] fs = file.listFiles();
        sort(fs);
        for(int i = 0;i < fs.length;i++){
            run(fs[i], type);
            //System.out.println(fs[i].getPath());

        }
    }

    public static void generateRs(File file){
        if(file.isFile()){
            if(isInclude(file.getName())) {
                ScriptParser.parseScript(file.getPath());
                TestScript script = ScriptParser.getTestScript();
                Executor.genRS(script);
                LOG.info("The results for the test script file["+file.getPath()+"] have been generated or updated.");
            }
            return;
        }
        File[] fs = file.listFiles();
        for(int i = 0;i < fs.length;i++){
            generateRs(fs[i]);
        }
    }

    public static void debug(File file,String type){
        if(file.isFile()){
            if(isInclude(file.getName())) {
                ScriptParser.parseScript(file.getPath());
                TestScript script = ScriptParser.getTestScript();
                Debugger.run(script);
            }
            return;
        }
        File[] fs = file.listFiles();
        for(int i = 0;i < fs.length;i++){
            debug(fs[i],type);
        }
    }

    public static void sort(File[] files){
        List fileList = Arrays.asList(files);
        Collections.sort(fileList, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                File f1 = (File)o1;
                File f2 = (File)o2;
                if (f1.isDirectory() && f2.isFile())
                    return -1;

                if (f1.isFile() && f2.isDirectory())
                    return 1;

                return f1.getName().compareTo(f2.getName());
            }
        });
    }

    public static boolean isInclude(String name){
        if(includes == null){
            if(excludes == null)
                return true;
            for(int i = 0; i < excludes.length;i++){
                if(name.indexOf(excludes[i]) != -1)
                    return false;
            }
            return true;
        }

        for(int i = 0; i < includes.length;i++){
            if(name.indexOf(includes[i]) != -1)
                return true;
        }

        return false;
    }
}
