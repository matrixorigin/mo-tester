package io.mo;

import io.mo.cases.TestScript;
import io.mo.constant.COMMON;
import io.mo.db.ConnectionManager;
import io.mo.db.Debugger;
import io.mo.db.Executor;
import io.mo.result.TestReport;
import io.mo.util.ResultParser;
import io.mo.util.RunConfUtil;
import io.mo.util.ScriptParser;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
        
        COMMON.RESOURCE_PATH = RunConfUtil.getResourcePath();
        COMMON.WAIT_TIMEOUT = RunConfUtil.getWaitTime();

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
                    COMMON.RESOURCE_LOCAL_PATH = srcPath;
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

                //get pprof info
                if (arg.equalsIgnoreCase("pprof")) {
                    COMMON.NEEDPPROF = true;
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
            LOG.info("Now start to clean up databaes and outfiles.");
            cleanDatabases();
            removeOutfiles();
            
            LOG.info("The method is [run],now start to run the scripts in the path["+ path +"].");
            run(file);
            LOG.info("All the scripts in the path["+ path +"] have been excuted.Now start to create the test report.");
            report.write();
            LOG.info("The test report has been generated in files[report.txt,report.xml].");
            removeOutfiles();

            if(report.getRate() < rate){
                LOG.error("The execution success rate is "+ report.getRate()+"%, and less than config value "+ rate +"%,this test fail.");
                System.exit(1);
            }else {
                LOG.info("The execution success rate is "+ report.getRate()+"%, and not less than config value "+ rate +"%,this test succeed.");
                System.exit(0);
            }
        }

        if(method.equalsIgnoreCase("debug")){
            LOG.info("Now start to clean up databaes and outfiles.");
            cleanDatabases();
            removeOutfiles();
            debug(file);
        }

        if(method.equalsIgnoreCase("check")){
            check(file);
        }

        if(method.equalsIgnoreCase("genrs")){
            LOG.info("Now start to clean up databaes and outfiles.");
            cleanDatabases();
            removeOutfiles();
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
            if(!(file.getName().endsWith(".sql") || file.getName().endsWith(".test"))) {
                return;
            }
            
            if(isInclude(file.getPath())) {
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
            if(!(file.getName().endsWith(".sql") || file.getName().endsWith(".test"))) {
                return;
            }
            
            if(isInclude(file.getPath())) {
                ScriptParser.parseScript(file.getPath());
                TestScript script = ScriptParser.getTestScript();
                if(!COMMON.FORCE_UPDATE){
                    if(Executor.genRS(script))
                        LOG.info("The results for the test script file["+file.getPath()+"] have been generated or updated successfully.");
                    else
                        LOG.info("The results for the test script file["+file.getPath()+"] have been generated or updated failed.");
                }else {
                    Executor.genRSForOnlyNotMatch(script);
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
            
            if(isInclude(file.getPath())) {
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
            if(!(file.getName().endsWith(".sql") || file.getName().endsWith(".test"))) {
                return;
            }
            if(isInclude(file.getPath())) {
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
    
    public static void cleanDatabases(){
        Connection connection = ConnectionManager.getConnection();
        if(connection == null){
            LOG.error("Failed to clean databases,please check the error,the program will exit.");
            System.exit(1);
        }
        String dropDB = "DROP DATABASE IF EXISTS `%s`";
        String[] dbs = RunConfUtil.getBuiltinDb();
        try {
            Statement show = connection.createStatement();
            Statement drop = connection.createStatement();
            ResultSet resultSet = show.executeQuery("show databases;");
            ArrayList<String> dbList = new ArrayList<>();
            while (resultSet.next()){
                String db = resultSet.getString(1);
                for(int i = 0; i < dbs.length; i++){
                    if(db.equalsIgnoreCase(dbs[i]))
                        break;
                    
                    //db is not in dbs
                    if(i == (dbs.length - 1)){
                        dbList.add(db);
                    }
                }
            }
            
            show.close();

            for(int j = 0; j < dbList.size(); j++){
                LOG.debug(String.format(dropDB, dbList.get(j)));
                drop.execute(String.format(dropDB, dbList.get(j)));
                LOG.debug(String.format("The database [%s] has been cleaned.", dbList.get(j)));
            }
            
            drop.close();
            connection.close();
            
        } catch (SQLException e) {
            LOG.error("Unexpected exception has happened when cleaning up databases, the error message is: " + e.getMessage());
            LOG.error("Clean up databases failed, the test will be terminated.");
            System.exit(1);
        }
    }
    
    public static void removeOutfiles(){
        String[] files = RunConfUtil.getOutFiles();
        for(int i = 0; i < files.length; i++){
            Path path = Paths.get(COMMON.RESOURCE_PATH + "/" + files[i]);
            if(!Files.exists(path))
                continue;
            
            boolean isdir = Files.isDirectory(path);
            
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (!file.endsWith(".gitignore")) {
                            Files.delete(file);
                            LOG.debug(String.format("The outfile or path [%s] has been removed.", file));
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        if (dir != path && dir.toFile().list().length == 0) {
                            Files.delete(dir);
                            LOG.debug(String.format("The outfile or path [%s] has been removed.", dir));
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch(IOException e){
                e.printStackTrace();
                LOG.error(String.format("The outfile or path [%s] has failed to be removed, the test will be terminated ", path));
                System.exit(1);
            }
            
            
        }
    }
}
