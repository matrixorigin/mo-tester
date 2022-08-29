package io.mo.db;

import io.mo.cases.SqlCommand;
import io.mo.cases.TestScript;
import io.mo.constant.COMMON;
import io.mo.constant.RESULT;
import io.mo.result.RSSet;
import io.mo.result.StmtResult;
import io.mo.util.ResultParser;
import org.apache.log4j.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.sql.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class Executor {

    private static final Logger LOG = Logger.getLogger(Executor.class.getName());
    
    /**
     * run test file function
     */
    public static void run(TestScript script){
        LOG.info("Start to execute the script file["+script.getFileName()+"] now, and it will take a few moment,pleas wait......");
        ConnectionManager.reset();

        //create a database named filename for test;
        Connection connection = ConnectionManager.getConnection();
        if(connection == null){
            LOG.error("No valid connnection,please check the config..");
        }
        createTestDB(connection,script);

        //parse the result file
        ResultParser.reset();
        ResultParser.parse(script);

        //if result file is parsed failed,return
        if(!ResultParser.isSucceeded()) {
            LOG.info("The script file["+script.getFileName()+"] has been executed," +
                    ", and cost: " + script.getDuration() +"s" +
                    "total:" + script.getCommands().size() +
                    ", success:" + script.getSuccessCmdCount() +
                    ", failed:" + script.getFailedCmdCount() +
                    ", ignored:" + script.getIgnoredCmdCount() +
                    ", abnoraml:" + script.getAbnormalCmdCount());
            return;
        }

        //run all the sql commands
        Statement statement = null;
        ArrayList<SqlCommand> commands = script.getCommands();
        long start = System.currentTimeMillis();

        for (SqlCommand command : commands) {
            //if the the command is marked to ignore flag and the IGNORE_MODEL = true
            //skip the command directly
            if (COMMON.IGNORE_MODEL && command.isIgnore()) {
                LOG.warn("Ignored sql command: [issue#" + command.getIssueNo() + "][" + script.getFileName() + "][row:" + command.getPosition() + "][" + command.getCommand().trim() + "]");
                script.addIgnoredCmd(command);
                command.getTestResult().setResult(RESULT.RESULT_TYPE_IGNORED);
                command.getTestResult().setErrorCode(RESULT.ERROR_CASE_IGNORE_CODE);
                command.getTestResult().setErrorDesc(RESULT.ERROR_CASE_IGNORE_DESC);
                continue;
            }

            connection = getConnection(command);
        
            //if can not get valid connection,put the command to the abnormal commands array
            if (connection == null) {
                LOG.error("No valid connnection,please check the config..");
                script.addAbnoramlCmd(command);
                command.getTestResult().setErrorCode(RESULT.ERROR_CONNECTION_LOST_CODE);
                command.getTestResult().setErrorDesc(RESULT.ERROR_CONNECTION_LOST_DESC);
                command.getTestResult().setResult(RESULT.RESULT_TYPE_ABNORMAL);
                command.getTestResult().setActResult(RESULT.ERROR_CONNECTION_LOST_DESC);
                command.getTestResult().setExpResult(command.getExpResult().toString());
                command.getTestResult().setRemark(command.getCommand() + "\n" +
                        "[EXPECT RESULT]:\n" + command.getTestResult().getExpResult() + "\n" +
                        "[ACTUAL RESULT]:\n" + command.getTestResult().getExpResult() + "\n");
                LOG.error("[" + script.getFileName() + "][row:" + command.getPosition() + "][" + command.getCommand().trim() + "] was executed failed");
                LOG.error("[EXPECT RESULT]:\n" + command.getTestResult().getExpResult());
                LOG.error("[ACTUAL RESULT]:\n" + command.getTestResult().getExpResult());
                continue;
            }

            try {
                statement = connection.createStatement();
                statement.execute(command.getCommand());
                ResultSet resultSet = statement.getResultSet();
                if (resultSet != null) {
                    RSSet rsSet = new RSSet(resultSet);
                    StmtResult actResult = new StmtResult(rsSet);
                    command.setActResult(actResult);
                    command.getTestResult().setActResult(actResult.toString());

                    StmtResult expResult = command.getExpResult();
                    expResult.setType(RESULT.STMT_RESULT_TYPE_SET);
                    expResult.setRsSet(ResultParser.convertToRSSet(expResult.getOrginalRSText(), command.getSeparator()));
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
                    LOG.debug("[" + script.getFileName() + "][row:" + command.getPosition() + "][" + command.getCommand().trim() + "] was executed successfully");
                } else {
                    script.addFailedCmd(command);
                    command.getTestResult().setErrorCode(RESULT.ERROR_CHECK_FAILED_CODE);
                    command.getTestResult().setErrorDesc(RESULT.ERROR_CHECK_FAILED_DESC);
                    command.getTestResult().setResult(RESULT.RESULT_TYPE_FAILED);
                    command.getTestResult().setRemark(command.getCommand() + "\n" +
                            "[EXPECT RESULT]:\n" + command.getTestResult().getExpResult() + "\n" +
                            "[ACTUAL RESULT]:\n" + command.getTestResult().getActResult() + "\n");
                    LOG.error("[" + script.getFileName() + "][row:" + command.getPosition() + "][" + command.getCommand().trim() + "] was executed failed");
                    LOG.error("[EXPECT RESULT]:\n" + command.getTestResult().getExpResult());
                    LOG.error("[ACTUAL RESULT]:\n" + command.getTestResult().getActResult());
                }
                statement.close();
            } catch (SQLException e) {
                try {
                    if (connection.isClosed() || !connection.isValid(10)) {
                        LOG.error("The connection has been lost,please check the logs .");
                        script.addAbnoramlCmd(command);
                        command.getTestResult().setErrorCode(RESULT.ERROR_CONNECTION_LOST_CODE);
                        command.getTestResult().setErrorDesc(RESULT.ERROR_CONNECTION_LOST_DESC);
                        command.getTestResult().setResult(RESULT.RESULT_TYPE_ABNORMAL);
                        command.getTestResult().setActResult(RESULT.ERROR_CONNECTION_LOST_DESC);
                        command.getTestResult().setRemark(command.getCommand() + "\n" +
                                "[EXPECT RESULT]:\n" + command.getTestResult().getExpResult() + "\n" +
                                "[ACTUAL RESULT]:\n" + command.getTestResult().getExpResult() + "\n");
                        LOG.error("[" + script.getFileName() + "][row:" + command.getPosition() + "][" + command.getCommand().trim() + "] was executed failed");
                        LOG.error("[EXPECT RESULT]:\n" + command.getTestResult().getExpResult());
                        LOG.error("[ACTUAL RESULT]:\n" + command.getTestResult().getExpResult());
                        continue;
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
                        LOG.debug("[" + script.getFileName() + "][row:" + command.getPosition() + "][" + command.getCommand().trim() + "] was executed successfully");
                    } else {
                        script.addFailedCmd(command);
                        command.getTestResult().setErrorCode(RESULT.ERROR_CHECK_FAILED_CODE);
                        command.getTestResult().setErrorDesc(RESULT.ERROR_CHECK_FAILED_DESC);
                        command.getTestResult().setResult(RESULT.RESULT_TYPE_FAILED);
                        command.getTestResult().setRemark(command.getCommand() + "\n" +
                                "[EXPECT RESULT]:\n" + command.getTestResult().getExpResult() + "\n" +
                                "[ACTUAL RESULT]:\n" + command.getTestResult().getActResult() + "\n");
                        LOG.error("[" + script.getFileName() + "][row:" + command.getPosition() + "][" + command.getCommand().trim() + "] was executed failed");
                        LOG.error("[EXPECT RESULT]:\n" + command.getTestResult().getExpResult());
                        LOG.error("[ACTUAL RESULT]:\n" + command.getTestResult().getActResult());
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

        LOG.info("The script file["+script.getFileName()+"] has been executed" +
                ", and cost: " + script.getDuration() +"s" +
                ", total:" + script.getCommands().size() +
                ", success:" + script.getSuccessCmdCount() +
                ", failed:" + script.getFailedCmdCount() +
                ", ignored:" + script.getIgnoredCmdCount() +
                ", abnoraml:" + script.getAbnormalCmdCount());
    }

    public static boolean genRS(TestScript script){
        ConnectionManager.reset();
        Connection connection = ConnectionManager.getConnection();

        boolean isUpdate = false;
        Statement statement;
        BufferedWriter rs_writer;
        //check whether the result dir exists
        File rsf = new File(script.getFileName().replaceAll("\\.[A-Za-z]+",COMMON.R_FILE_SUFFIX));

        //if the result file exists,menas it is to update old result file
        //and in this case,result of sqlcommand with bvt:issue tag will be not updated,because the result get from system is not correct
        //so it need to parse the old result file first, and read the original result
        if(rsf.exists()){
            isUpdate = true;
            ResultParser.reset();
            ResultParser.parse(script);
            //if result file is parsed failed,return
            if(!ResultParser.isSucceeded()) {
                LOG.warn("The test file["+script.getFileName()+"] does not match its result file");
                LOG.warn("The mo-tester will generate new result file for ["+script.getFileName()+"] ");
            }
        }

        //create a database named filename for test;
        createTestDB(connection,script);

        try {
            rs_writer = new BufferedWriter(new FileWriter(rsf.getPath()));
            ArrayList<SqlCommand> commands = script.getCommands();
            for (int j = 0; j < commands.size(); j++) {
                SqlCommand command = null;
                try{
                    command = commands.get(j);
                    connection = getConnection(command);
                    statement = connection.createStatement();

                    statement.execute(command.getCommand());
                    ResultSet resultSet = statement.getResultSet();
                    if(resultSet != null){
                        RSSet rsSet = new RSSet(resultSet);
                        StmtResult actResult = new StmtResult(rsSet);
                        actResult.setCommand(command);
                        rs_writer.write(command.getCommand().trim());
                        rs_writer.newLine();
                        if(command.isIgnore()) {
                            if (isUpdate) {
                                if (command.getExpResult() != null) {
                                    if (command.getExpResult().getType() != RESULT.STMT_RESULT_TYPE_NONE)
                                        rs_writer.write(command.getExpResult().getOrginalRSText());
                                    else 
                                        continue;
                                }
                                else 
                                    continue;
                            } else {
                                rs_writer.write("[unknown result because it is related to issue#" + command.getIssueNo() + "]");
                            }
                        }
                        else {
                            rs_writer.write(actResult.toString());
                        }

                        if(j < commands.size() -1)
                            rs_writer.newLine();
                    }else{
                        rs_writer.write(command.getCommand().trim());
                        if(command.isIgnore() ){
                            if (isUpdate) {
                                if (command.getExpResult() != null) {
                                    if (command.getExpResult().getType() != RESULT.STMT_RESULT_TYPE_NONE) {
                                        rs_writer.newLine();
                                        rs_writer.write(command.getExpResult().getOrginalRSText());
                                    }
                                }
                            } else {
                                rs_writer.newLine();
                                rs_writer.write("[unknown result because it is related to issue#" + command.getIssueNo() + "]");
                            }
                        }
                        if(j < commands.size() -1)
                            rs_writer.newLine();
                    }
                    statement.close();
                }catch (SQLException e) {
                    if(null == command){
                        break;
                    }
                    rs_writer.write(command.getCommand().trim());
                    rs_writer.newLine();
                    if(command.isIgnore()){
                        if (isUpdate) {
                            if (command.getExpResult() != null) {
                                if (command.getExpResult().getType() != RESULT.STMT_RESULT_TYPE_NONE)
                                    rs_writer.write(command.getExpResult().getOrginalRSText());
                                else 
                                    continue;
                            }
                            else 
                                continue;
                        } else {
                            rs_writer.write("[unknown result because it is related to issue#" + command.getIssueNo() + "]");
                        }
                    } else
                        rs_writer.write(e.getMessage());
                    
                    if(j < commands.size() -1)
                        rs_writer.newLine();
                }
            }
            rs_writer.newLine();
            rs_writer.flush();
            rs_writer.close();
            //drop the test db
            dropTestDB(connection,script);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static Connection getConnection(SqlCommand command){
        Connection connection;
        if(command.getConn_id() != 0){
            if(command.getConn_user() == null){
                connection = ConnectionManager.getConnection(command.getConn_id());
                return connection;
            }else {
                connection = ConnectionManager.getConnection(command.getConn_id(),command.getConn_user(),command.getConn_pswd());
                return connection;
            }
        }
        connection = ConnectionManager.getConnection();
        return connection;
    }

    public static  void createTestDB(Connection connection,String name){
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

    public static  void createTestDB(Connection connection,TestScript script){
       File file = new File(script.getFileName());
       String dbName = file.getName().substring(0,file.getName().lastIndexOf("."));
       createTestDB(connection,dbName);
    }

    public static  void dropTestDB(Connection connection,String name){
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

    public static  void dropTestDB(Connection connection,TestScript script){
        File file = new File(script.getFileName());
        String dbName = file.getName().substring(0,file.getName().lastIndexOf("."));
        dropTestDB(connection,dbName);
    }

    public static void main(String[] args){
        String exp = "You have an error in your SQL syntax; check the manual that corresponds to your MatrixOne server version for the right syntax to use. syntax error at position 55 near 'abc';";
        String act = "You have an error in your SQL syntax; check the manual that corresponds to your MatrixOne server version for the right syntax to use. syntax error at position 55 near 'abc';";
        System.out.println(exp.equalsIgnoreCase(act));
    }


}
