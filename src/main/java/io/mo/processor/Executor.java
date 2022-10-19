package io.mo.processor;

import io.mo.cases.SqlCommand;
import io.mo.cases.TestScript;
import io.mo.constant.COMMON;
import io.mo.constant.RESULT;
import io.mo.constant.SQL;
import io.mo.db.ConnectionPool;
import io.mo.result.RSSet;
import io.mo.result.StmtResult;
import io.mo.result.TestReport;
import io.mo.util.MoConfUtil;
import org.apache.log4j.Logger;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

public class Executor implements Runnable{
    private  Logger LOG = Logger.getLogger(Executor.class.getName());
    private  ConnectionPool connectionPool;
    private ArrayList<File> files = new ArrayList<>();
    private ScriptParser scriptParser = new ScriptParser();
    private ResultParser resultParser = new ResultParser();
    
    private CountDownLatch latch;
    
    private int tid ;
    
    public Executor(){
        this.tid = 0;
        this.latch = new CountDownLatch(1);
        connectionPool = new ConnectionPool();
    }
    
    public Executor(int tid,CountDownLatch latch){
        this.latch = latch;
        this.tid = tid;
        Connection con;
        String driver = MoConfUtil.getDriver();
        String userName = MoConfUtil.getUserName();
        String pwd = MoConfUtil.getUserpwd();
        String jdbcURL = MoConfUtil.getURL();
        String account = COMMON.PREFIX_ACCOUT_DEF + tid;

        try {
            Class.forName(driver);
            con = DriverManager.getConnection(jdbcURL, userName, pwd);
            Statement stmt = con.createStatement();
            stmt.execute(String.format(SQL.CREATE_ACCOUNT,account,userName,pwd));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        connectionPool = new ConnectionPool(account+":"+userName,pwd);
        LOG.info(String.format(String.format("The executor[tid=%d] has been initialized.",tid)));
    }
    
    /**
     * run test file function
     */
    public void run(){
        
        for(File sFile : files){
            scriptParser.parseScript(sFile.getPath());
            TestScript script = scriptParser.getTestScript();
            if(script.isSkiped()){
                LOG.info(String.format("[executor id=%d] Has skipped the script file [%s]",tid,script.getFileName()));
                continue;
            }
            LOG.info(String.format("[executor id=%d] Start to execute the script file[%s] now, and it will take a few moment,pleas wait......",tid,script.getFileName()));
            connectionPool.reset();
            //create a database named filename for test;
            Connection connection = connectionPool.getConnection();
            if(connection == null){
                LOG.error(String.format("[executor id=%d] No valid connnection,please check the config..",tid));
            }
            createTestDB(connection,script);

            //parse the result file
            resultParser.reset();
            resultParser.parse(script);

            //if result file is parsed failed,return
            if(!resultParser.isSucceeded()) {
                LOG.info(String.format("[executor id=%d] The script file[%s] has been executed, and cost: %f s, total: %d, success: %d, failed: %d, ignored: %d, abnoraml: %d",
                        tid,
                        script.getFileName(),
                        script.getDuration(),
                        script.getCommands().size(),
                        script.getSuccessCmdCount(),
                        script.getFailedCmdCount(),
                        script.getIgnoredCmdCount(),
                        script.getAbnormalCmdCount()));
                TestReport.collect(script);
                continue;
            }

            //run all the sql commands
            Statement statement = null;
            ArrayList<SqlCommand> commands = script.getCommands();
            long start = System.currentTimeMillis();

            for (SqlCommand command : commands) {
                //if the the command is marked to ignore flag and the IGNORE_MODEL = true
                //skip the command directly
                if (COMMON.IGNORE_MODEL && command.isIgnore()) {
                    LOG.warn(String.format("[executor id=%d] Ignored sql command: [issue#%s][%s][row:%d][%s]",
                            tid,command.getIssueNo(),
                            script.getFileName(),
                            command.getPosition(),
                            command.getCommand().trim()));
                    
                    script.addIgnoredCmd(command);
                    command.getTestResult().setResult(RESULT.RESULT_TYPE_IGNORED);
                    command.getTestResult().setErrorCode(RESULT.ERROR_CASE_IGNORE_CODE);
                    command.getTestResult().setErrorDesc(RESULT.ERROR_CASE_IGNORE_DESC);
                    continue;
                }

                connection = getConnection(command);
                //if can not get valid connection,put the command to the abnormal commands array
                if (connection == null) {
                    LOG.error(String.format("[executor id=%d] No valid connnection,please check the config..",tid));
                    script.addAbnoramlCmd(command);
                    command.getTestResult().setErrorCode(RESULT.ERROR_CONNECTION_LOST_CODE);
                    command.getTestResult().setErrorDesc(RESULT.ERROR_CONNECTION_LOST_DESC);
                    command.getTestResult().setResult(RESULT.RESULT_TYPE_ABNORMAL);
                    command.getTestResult().setActResult(RESULT.ERROR_CONNECTION_LOST_DESC);
                    command.getTestResult().setExpResult(command.getExpResult().toString());
                    command.getTestResult().setRemark(command.getCommand() + "\n" +
                            "[EXPECT RESULT]:\n" + command.getTestResult().getExpResult() + "\n" +
                            "[ACTUAL RESULT]:\n" + command.getTestResult().getActResult() + "\n");
                    LOG.error(String.format("[executor id=%d] [%s][row:%d][%s] was executed failed",
                            tid,
                            script.getFileName(),
                            command.getPosition(),
                            command.getCommand().trim()));
                    LOG.error(String.format("[executor id=%d] [EXPECT RESULT]:\n%s",tid, command.getTestResult().getExpResult()));
                    LOG.error(String.format("[executor id=%d] [ACTUAL RESULT]:\n%s",tid, command.getTestResult().getActResult()));
                    continue;
                }

                try {
                    statement = connection.createStatement();
                    String sqlCmd = command.getCommand().replaceAll(COMMON.RESOURCE_PATH_FLAG,COMMON.RESOURCE_PATH);
                    statement.execute(sqlCmd);
                    ResultSet resultSet = statement.getResultSet();
                    if (resultSet != null) {
                        RSSet rsSet = new RSSet(resultSet);
                        StmtResult actResult = new StmtResult(rsSet);
                        command.setActResult(actResult);
                        command.getTestResult().setActResult(actResult.toString());

                        StmtResult expResult = command.getExpResult();
                        expResult.setType(RESULT.STMT_RESULT_TYPE_SET);
                        expResult.setRsSet(resultParser.convertToRSSet(expResult.getOrginalRSText(), command.getSeparator()));
                        command.getTestResult().setExpResult(expResult.toString());
                    } else {
                        StmtResult actResult = new StmtResult();
                        actResult.setType(RESULT.STMT_RESULT_TYPE_NONE);
                        command.setActResult(actResult);
                    }

                    //check whether the execution result is successful
                    if (command.checkResult()) {
                        script.addSuccessCmd(command);
                        command.getTestResult().setResult(RESULT.RESULT_TYPE_PASS);
                        LOG.debug(String.format("[executor id=%d] [%s][row:%d][%s] was executed successfully, con[id=%d, user=%s, pwd=%s].",
                                tid,
                                script.getFileName(),
                                command.getPosition(),
                                command.getCommand().trim(),
                                command.getConn_id(),
                                command.getConn_user(),
                                command.getConn_pswd()
                                ));
                    } else {
                        script.addFailedCmd(command);
                        command.getTestResult().setErrorCode(RESULT.ERROR_CHECK_FAILED_CODE);
                        command.getTestResult().setErrorDesc(RESULT.ERROR_CHECK_FAILED_DESC);
                        command.getTestResult().setResult(RESULT.RESULT_TYPE_FAILED);
                        command.getTestResult().setRemark(command.getCommand() + "\n" +
                                "[EXPECT RESULT]:\n" + command.getTestResult().getExpResult() + "\n" +
                                "[ACTUAL RESULT]:\n" + command.getTestResult().getActResult() + "\n");
                        LOG.error(String.format("[executor id=%d] [%s][row:%d][%s] was executed failed, con[id=%d, user=%s, pwd=%s].",
                                tid,
                                script.getFileName(),
                                command.getPosition(),
                                command.getCommand().trim(),
                                command.getConn_id(),
                                command.getConn_user(),
                                command.getConn_pswd()
                        ));
                        LOG.error(String.format("[executor id=%d] [EXPECT RESULT]:\n%s",tid, command.getTestResult().getExpResult()));
                        LOG.error(String.format("[executor id=%d] [ACTUAL RESULT]:\n%s",tid, command.getTestResult().getActResult()));
                    }
                    statement.close();
                } catch (SQLException e) {
                    try {
                        if (connection.isClosed() || !connection.isValid(10)) {
                            LOG.error(String.format("[executor id=%d] The connection has been lost,please check the logs .",tid));
                            script.addAbnoramlCmd(command);
                            command.getTestResult().setErrorCode(RESULT.ERROR_CONNECTION_LOST_CODE);
                            command.getTestResult().setErrorDesc(RESULT.ERROR_CONNECTION_LOST_DESC);
                            command.getTestResult().setResult(RESULT.RESULT_TYPE_ABNORMAL);
                            command.getTestResult().setActResult(RESULT.ERROR_CONNECTION_LOST_DESC);
                            command.getTestResult().setRemark(command.getCommand() + "\n" +
                                    "[EXPECT RESULT]:\n" + command.getTestResult().getExpResult() + "\n" +
                                    "[ACTUAL RESULT]:\n" + command.getTestResult().getActResult() + "\n");
                            LOG.error(String.format("[executor id=%d] [%s][row:%d][%s] was executed failed, con[id=%d, user=%s, pwd=%s].",
                                    tid,
                                    script.getFileName(),
                                    command.getPosition(),
                                    command.getCommand().trim(),
                                    command.getConn_id(),
                                    command.getConn_user(),
                                    command.getConn_pswd()
                            ));
                            LOG.error(String.format("[executor id=%d] [EXPECT RESULT]:\n%s",tid, command.getTestResult().getExpResult()));
                            LOG.error(String.format("[executor id=%d] [ACTUAL RESULT]:\n%s",tid, command.getTestResult().getActResult()));
                        }

                        StmtResult actResult = new StmtResult();
                        actResult.setType(RESULT.STMT_RESULT_TYPE_ERROR);
                        actResult.setErrorMessage(e.getMessage());

                        command.getExpResult().setType(RESULT.STMT_RESULT_TYPE_ERROR);
                        command.getExpResult().setErrorMessage(command.getExpResult().getOrginalRSText());

                        command.setActResult(actResult);

                        //check whether the execution result is successful
                        if (command.checkResult()) {
                            script.addSuccessCmd(command);
                            command.getTestResult().setResult(RESULT.RESULT_TYPE_PASS);
                            LOG.debug(String.format("[executor id=%d] [%s][row:%d][%s] was executed successfully, con[id=%d, user=%s, pwd=%s].",
                                    tid,
                                    script.getFileName(),
                                    command.getPosition(),
                                    command.getCommand().trim(),
                                    command.getConn_id(),
                                    command.getConn_user(),
                                    command.getConn_pswd()
                            ));
                        } else {
                            script.addFailedCmd(command);
                            command.getTestResult().setErrorCode(RESULT.ERROR_CHECK_FAILED_CODE);
                            command.getTestResult().setErrorDesc(RESULT.ERROR_CHECK_FAILED_DESC);
                            command.getTestResult().setResult(RESULT.RESULT_TYPE_FAILED);
                            command.getTestResult().setRemark(command.getCommand() + "\n" +
                                    "[EXPECT RESULT]:\n" + command.getTestResult().getExpResult() + "\n" +
                                    "[ACTUAL RESULT]:\n" + command.getTestResult().getActResult() + "\n");
                            LOG.error(String.format("[executor id=%d] [%s][row:%d][%s] was executed failed, con[id=%d, user=%s, pwd=%s].",
                                    tid,
                                    script.getFileName(),
                                    command.getPosition(),
                                    command.getCommand().trim(),
                                    command.getConn_id(),
                                    command.getConn_user(),
                                    command.getConn_pswd()
                            ));
                            LOG.error(String.format("[executor id=%d] [EXPECT RESULT]:\n%s",tid, command.getTestResult().getExpResult()));
                            LOG.error(String.format("[executor id=%d] [ACTUAL RESULT]:\n%s",tid, command.getTestResult().getActResult()));
                        }

                        assert statement != null;
                        statement.close();
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }

            //drop the test db
            dropTestDB(connection,script);

            long end = System.currentTimeMillis();
            script.setDuration((float)(end - start)/1000);

            LOG.info(String.format("[executor id=%d] The script file[%s] has been executed, and cost: %f s, total: %d, success: %d, failed: %d, ignored: %d, abnoraml: %d",
                    tid,
                    script.getFileName(),
                    script.getDuration(),
                    script.getCommands().size(),
                    script.getSuccessCmdCount(),
                    script.getFailedCmdCount(),
                    script.getIgnoredCmdCount(),
                    script.getAbnormalCmdCount()));
            
            TestReport.collect(script);
            
        }
        latch.countDown();
    }
    
    public  Connection getConnection(SqlCommand command){
        Connection connection;
        if(command.getConn_user() == null){
            connection = connectionPool.getConnection(command.getConn_id());
            return connection;
        }else {
            String user = command.getConn_user();
            String pwd = command.getConn_pswd();
            connection = connectionPool.getConnection(command.getConn_id(),command.getConn_user(),command.getConn_pswd());
            return connection;
        }
    }
    
    public  void createTestDB(Connection connection,String name){
        Logger LOG = Logger.getLogger(Executor.class.getName());
        if(connection == null) return;
        Statement statement;
        try {
            statement = connection.createStatement();
            statement.executeUpdate("create database IF NOT EXISTS `"+name+"`;");
            statement.executeUpdate("use `"+name+"`;");
        } catch (SQLException e) {
            LOG.error("create database "+name+" is failed.cause: "+e.getMessage());
        }
    }

    public  void createTestDB(Connection connection,TestScript script){
        File file = new File(script.getFileName());
        String dbName = file.getName().substring(0,file.getName().lastIndexOf("."));
        createTestDB(connection,dbName);
    }

    public  void dropTestDB(Connection connection,String name){
        Logger LOG = Logger.getLogger(Executor.class.getName());
        if(connection == null) return;
        Statement statement;
        try {
            statement = connection.createStatement();
            statement.executeUpdate("drop database IF EXISTS `"+name+"`;");
        } catch (SQLException e) {
            //e.printStackTrace();
            LOG.error("drop database "+name+" is failed.cause: "+e.getMessage());
        }
    }

    public void dropTestDB(Connection connection,TestScript script){
        File file = new File(script.getFileName());
        String dbName = file.getName().substring(0,file.getName().lastIndexOf("."));
        dropTestDB(connection,dbName);
    }
    
    public void addScriptFile(File file){
        files.add(file);
        Collections.addAll(files);
        Collections.sort(files);
    }
    
    public ArrayList<File> getScriptFiles(){
        return files;
    }
    
    public int getTid(){
        return tid;
    }

    public static void main(String[] args){
        String exp = "You have an error in your SQL syntax; check the manual that corresponds to your MatrixOne server version for the right syntax to use. syntax error at position 55 near 'abc';";
        String act = "You have an error in your SQL syntax; check the manual that corresponds to your MatrixOne server version for the right syntax to use. syntax error at position 55 near 'abc';";
        System.out.println(exp.equalsIgnoreCase(act));
    }
    
}
