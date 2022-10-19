package io.mo;

import io.mo.cases.TestScript;
import io.mo.constant.COMMON;
import io.mo.processor.*;
import io.mo.result.TestReport;
import io.mo.util.MoConfUtil;
import io.mo.util.RunConfUtil;
import org.apache.log4j.Logger;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Tester {
    private static final Logger LOG = Logger.getLogger(Tester.class.getName());
    private static String[] includes = null;
    private static String[] excludes = null;

    private static String[] serials = null;
    
    private static ExecutorService service;
    
    private static Executor[] executors;
    
    private static CountDownLatch latch;
    
    private static int tid = 0;
    private static int tNum = 1;//default value 1
    private static String path = null;
    private static String method = null;
    private static int rate = 100;//default value 100
    

    private static Executor serialExecutor;// executor that run scripts that must be executed serially 

    private static ScriptParser scriptParser = new ScriptParser();
    private static ResultParser resultParser = new ResultParser();

    public static void main(String[] args){
        
        tNum = RunConfUtil.getTerminals();// ternimal number
        path = RunConfUtil.getPath();
        method = RunConfUtil.getMethod();
        rate = RunConfUtil.getRate();
        
        COMMON.RESOURCE_PATH = RunConfUtil.getResourcePath();
        serialExecutor = new Executor();
        
        //parse the paras
        if(args != null){
            for (String arg : args) {
                //get path
                if (arg.startsWith("path")) {
                    path = arg.split("=")[1];
                    File caseFile = new File(path);
                    String srcPath = null;
                    if(caseFile.getAbsolutePath().contains(COMMON.CASES_DIR)) {
                        srcPath = caseFile.getAbsolutePath();
                        srcPath = srcPath.replace(COMMON.CASES_DIR,COMMON.RESOURCE_DIR);
                        srcPath = srcPath.substring(0,srcPath.indexOf(COMMON.RESOURCE_DIR)+COMMON.RESOURCE_DIR.length());
                    }
                    COMMON.RESOURCE_PATH = srcPath;
                }

                //get method
                if (arg.startsWith("method")) {
                    if(!arg.contains("=")){
                        LOG.error("The format of para[method] is not valid,please check......");
                        System.exit(1);
                    }
                    method = arg.split("=")[1];
                }

                //get sucess rate
                if (arg.startsWith("rate")) {
                    if(!arg.contains("=")){
                        LOG.error("The format of para[rate] is not valid,please check......");
                        System.exit(1);
                    }
                    rate = Integer.parseInt(arg.split("=")[1]);
                }

                //get ignore
                if (arg.equalsIgnoreCase("ignore")) {
                    COMMON.IGNORE_MODEL = true;
                }

                //get includes
                if (arg.startsWith("include")) {
                    if(!arg.contains("=")){
                        LOG.error("The format of para[include] is not valid,please check......");
                        System.exit(1);
                    }
                    includes = arg.split("=")[1].split(",");
                }

                //get excludes
                if (arg.startsWith("exclude")) {
                    if(!arg.contains("=")){
                        LOG.error("The format of para[include] is not valid,please check......");
                        System.exit(1);
                    }
                    excludes = arg.split("=")[1].split(",");
                }

                //get serial scripts
                if (arg.startsWith("serial")) {
                    if(!arg.contains("=")){
                        LOG.error("The format of para[serial] is not valid,please check......");
                        System.exit(1);
                    }
                    serials = arg.split("=")[1].split(",");
                }

                //get resource path
                if (arg.startsWith("resource")) {
                    if(!arg.contains("=")){
                        LOG.error("The format of para[resource] is not valid,please check......");
                        System.exit(1);
                    }
                    
                    COMMON.RESOURCE_PATH = arg.split("=")[1];
                }

                //get force
                if (arg.equalsIgnoreCase("force")) {

                    COMMON.FORCE_UPDATE = true;
                }

                //get nometa info
                if (arg.equalsIgnoreCase("nometa")) {
                    COMMON.IS_COMPARE_META = false;
                }

                //get check info
                if (arg.equalsIgnoreCase("check")) {
                    method = "check";
                }

                //get ternimal number
                if (arg.startsWith("ternimals")) {
                    if(!arg.contains("=")){
                        LOG.error("The format of para[ternimals] is not valid,please check......");
                        System.exit(1);
                    }

                    
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
        
        //init executors
        
        if(method.equalsIgnoreCase("run")){

            //init executors
            latch = new CountDownLatch(tNum);
            executors = new Executor[tNum];
            for(int i = 0; i < tNum; i++){
                executors[i] = new Executor(i,latch);
            }
            init(file);

            service = Executors.newFixedThreadPool(tNum);
            Thread sThread = new Thread(serialExecutor);

            try {
                LOG.info(String.format("The method is [run],now start to run the scripts in the path[%s].",path));
                LOG.info(String.format("First execute all the serials scripts."));

                long start = System.currentTimeMillis();
                
                sThread.start();
                sThread.join();
    
                LOG.info(String.format("Now execute scripts in parallel."));
//                for(Executor executor : executors){
//                    service.submit(executor);
//                }
                for(int i = 0; i < executors.length;i++){
                    service.submit(executors[i]);
                    //executors[i].run();
                }
            
                latch.await();
                long end = System.currentTimeMillis();
                LOG.info(String.format("All the scripts in the path[%s] have been excuted.Now start to create the test report.",path));
                TestReport.setDuration((long)(end - start)/1000);
                TestReport.write();
                LOG.info("The test report has been generated in file[./report/report.txt].");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            
            if(TestReport.getRate() < rate){
                LOG.error(String.format("The execution success rate is %d%%, and less than config value %d%%,this test fail.",TestReport.getRate(),rate));
                System.exit(1);
            }else {
                LOG.info(String.format("The execution success rate is %d%%, and not less than config value %d%%,this test succeed.",TestReport.getRate(),rate));
                System.exit(0);
            }
            
            service.shutdown();
            
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
    
    
    
    public static void init(File file){
        
        if(file.isFile()){
            if(!(file.getName().endsWith(".sql") || file.getName().endsWith(".test"))) {
                return;
            }

            if(isInclude(file.getName())) {
                if(isSerial(file.getPath())){
                    serialExecutor.addScriptFile(file);
                }else{
                    executors[tid].addScriptFile(file);
                    if(tid == tNum -1)
                        tid = 0;
                    else
                        tid ++;
                }
            }
            return;
        }
        
        File[] fs = file.listFiles();
        assert fs != null;
        for (File f : fs) {
            init(f);
        }
    }

    public static void generateRs(File file){
        if(file.isFile()){
            if(!(file.getName().endsWith(".sql") || file.getName().endsWith(".test"))) {
                return;
            }
            
            if(isInclude(file.getName())) {
                scriptParser.parseScript(file.getPath());
                TestScript script = scriptParser.getTestScript();
                if(!COMMON.FORCE_UPDATE){
                    if(ResultGenerator.genRS(script))
                        LOG.info("The results for the test script file["+file.getPath()+"] have been generated or updated successfully.");
                    else
                        LOG.info("The results for the test script file["+file.getPath()+"] have been generated or updated failed.");
                }else {
                    ResultGenerator.genRSForOnlyNotMatch(script);
                }
                
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
            if(!(file.getName().endsWith(".sql") || file.getName().endsWith(".test"))) {
                return;
            }
            
            if(isInclude(file.getName())) {
                scriptParser.parseScript(file.getPath());
                TestScript script = scriptParser.getTestScript();
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
            if(!(file.getName().endsWith(".sql") || file.getName().endsWith(".test"))) {
                return;
            }
            if(isInclude(file.getName())) {
                scriptParser.parseScript(file.getPath());
                TestScript script = scriptParser.getTestScript();
                resultParser.check(script);
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
    
    public static boolean isSerial(String path){
        if(serials == null){
            return false;
        }
        
        for (String serial : serials) {
            if (path.contains(serial))
                return true;
        }
        
        return false;
    }
    
    
}
