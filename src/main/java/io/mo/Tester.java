package io.mo;

import io.mo.cases.TestScript;
import io.mo.cases.TestSuite;
import io.mo.db.Debugger;
import io.mo.db.Executor;
import io.mo.result.TestReport;
import io.mo.util.RunConfUtil;
import io.mo.util.ScriptParser;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;

public class Tester {
    private static ArrayList<TestSuite> suites = new ArrayList<TestSuite>();

    private static TestReport report = new TestReport();

    private static Logger LOG = Logger.getLogger(Tester.class.getName());



    public static void main(String[] args){

        String path = RunConfUtil.getPath();
        String method = RunConfUtil.getMethod();
        String type = RunConfUtil.getType();

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

            if(report.getRate() < RunConfUtil.getRate()){
                LOG.error("The execution success rate is "+ report.getRate()+"%,and less than config value "+RunConfUtil.getRate()+"%,this test fail.");
                System.exit(1);
            }else {
                LOG.error("The execution success rate is "+ report.getRate()+"%,and not less than config value "+RunConfUtil.getRate()+"%,this test succeed.");
                System.exit(0);
            }
        }

        if(method.equalsIgnoreCase("debug")){
            debug(file,type);
        }

        if(method.equalsIgnoreCase("genrs")){
            LOG.info("The method is [genrs],now start to generate the checkpoints in the path["+path+"].");
            generateRs(file);
            LOG.info("ALL the results in the path["+path+"] have been generated or updated.");
        }

        if(!method.equalsIgnoreCase("genrs")&&!method.equalsIgnoreCase("debug")&&!method.equalsIgnoreCase("run")){
            LOG.info("The method is ["+method+"] can not been supported.Only[run,debug,genrs] can be supported.");
            return;
        }

    }

    public static void run(File file,String type){
        if(file.isFile()){
            if(type.equalsIgnoreCase("script")){
                ScriptParser.parseScript(file.getPath());
                TestScript script = ScriptParser.getTestScript();
                Executor.run(script);
                report.collect(script);
                return;
            }

            if(type.equalsIgnoreCase("suite")){
                ScriptParser.parseSuite(file.getPath());
                ArrayList<TestSuite> suites = ScriptParser.getTestSuites();
                Executor.run(suites,file.getPath());
                report.collect(suites);
                return;
            }
        }
        File[] fs = file.listFiles();
        for(int i = 0;i < fs.length;i++){
            run(fs[i], type);
        }
    }

    public static void generateRs(File file){
        if(file.isFile()){
            ScriptParser.parseScript(file.getPath());
            TestScript script = ScriptParser.getTestScript();
            Executor.genRS(script);
            return;
        }
        File[] fs = file.listFiles();
        for(int i = 0;i < fs.length;i++){
            generateRs(fs[i]);
        }
    }

    public static void debug(File file,String type){
        if(file.isFile()){
            ScriptParser.parseScript(file.getPath());
            TestScript script = ScriptParser.getTestScript();
            Debugger.run(script);
            return;
        }
        File[] fs = file.listFiles();
        for(int i = 0;i < fs.length;i++){
            debug(fs[i],type);
        }

    }
}
