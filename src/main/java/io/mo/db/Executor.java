package io.mo.db;

import io.mo.cases.SqlCommand;
import io.mo.cases.TestCase;
import io.mo.cases.TestScript;
import io.mo.cases.TestSuite;
import io.mo.constant.COMMON;
import io.mo.constant.RESULT;
import io.mo.result.TestResult;
import io.mo.util.ResultParser;
import org.apache.log4j.Logger;

import java.io.*;
import java.sql.*;
import java.sql.Connection;
import java.util.ArrayList;

public class Executor {
    private static PrintWriter logWriter;
    //private static SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS");

    private static Logger LOG = Logger.getLogger(Executor.class.getName());

    public static void run(TestScript script){
        LOG.info("Now start to run the file["+script.getFileName()+"]....................................................");
        ConnectionManager.reset();
        Connection connection = ConnectionManager.getConnection();
        if(connection == null){
            LOG.error("No valid connnection,please check the config..");
            //System.exit(1);
        }

        boolean hasResults = false;
        Statement statement = null;

        //check whether the result file exists
        File rsf = new File(script.getFileName().replaceFirst(COMMON.CASES_PATH,COMMON.RESULT_PATH).replaceAll("\\.[A-Za-z]+",COMMON.R_FILE_SUFFIX));
        if(!rsf.exists()) {
            LOG.warn("The result of the test script file["+script.getFileName()+"] does not exists,please check....");
            //set the execution status of test script to  false
            script.setExecStatus(false);
            return;
        }

        //create a database named filename for test;
        String def_db = rsf.getName().substring(0,rsf.getName().indexOf(COMMON.R_FILE_SUFFIX));
        createTestDB(connection,def_db);

        ResultParser.reset();
        ResultParser.parse(rsf.getPath());
        ArrayList<SqlCommand> commands = script.getCommands();

        long start = System.currentTimeMillis();

        for (int j = 0; j < commands.size(); j++) {
            SqlCommand command = null;
            String exp_res = null;
            String act_res = null;

            try{
                command = commands.get(j);

                //if the the command is marked to ignore flag and the IGNORE_MODEL = true
                //skip the command directly
                if(COMMON.IGNORE_MODEL && command.isIgnore()){
                    LOG.info("["+script.getFileName()+"]["+command.getCommand().trim()+"] is ignored");
                    //to skip the read pos in the expected result
                    ResultParser.skip(command.getCommand());
                    /*command.getResult().setErrorCode(RESULT.ERROR_CASE_IGNORE_CODE);
                    command.getResult().setErrorDesc(RESULT.ERROR_CASE_IGNORE_DESC);
                    command.getResult().setResult(RESULT.RESULT_TYPE_NOEXEC);
                    script.addNoExecCmd(command);*/
                    continue;
                }

                connection = getConnection(command);
                if(connection == null){
                    LOG.error("No valid connnection,please check the config..");
                    //System.exit(1);
                    LOG.error("The connection has been lost,please check the logs .");
                    script.addNoExecCmd(command);
                    command.getResult().setErrorCode(RESULT.ERROR_CONNECTION_LOST_CODE);
                    command.getResult().setErrorDesc(RESULT.ERROR_CONNECTION_LOST_DESC);
                    command.getResult().setResult(RESULT.RESULT_TYPE_NOEXEC);
                    command.getResult().setExpResult(exp_res);
                    command.getResult().setActResult(RESULT.ERROR_CONNECTION_LOST_DESC);
                    command.getResult().setRemark(command.getCommand()+"\n"+
                            "[EXPECT RESULT]:\n"+exp_res+"\n"+
                            "[ACTUAL RESULT]:\n"+RESULT.ERROR_CONNECTION_LOST_DESC+"\n");
                    LOG.error("["+script.getFileName()+"]["+command.getCommand().trim()+"] is executed failed");
                    LOG.error("[EXPECT RESULT]:\n"+exp_res);
                    LOG.error("[ACTUAL RESULT]:\n"+RESULT.ERROR_CONNECTION_LOST_DESC);
                    continue;
                }
                statement = connection.createStatement();
                //statement.setQueryTimeout(30);
                if (command.isUpdate()) {
                    //if no-query-type statement is executed successfully,do not need check
                    int num = statement.executeUpdate(command.getCommand());
                    LOG.info("["+script.getFileName()+"]["+command.getCommand().trim()+"]: row affect: "+num);
                    //but need to get the expected result,to skip the read pos
                    ResultParser.skip(command.getCommand());
                    LOG.info("["+script.getFileName()+"]["+command.getCommand().trim()+"] is executed successfully");
                } else {
                    //if query-type statment is executed successfully,need compare the expected result and the actual result
                    hasResults = statement.execute(command.getCommand());
                    //statement.executeQuery(command.getCommand());
                    act_res = getRS(statement.getResultSet());
                    if( j < commands.size() -1)
                        exp_res = ResultParser.getRS(command.getCommand(),commands.get(j + 1).getCommand());
                    else
                        exp_res = ResultParser.getRS(command.getCommand(),null);

                    if(act_res == null){
                        if(exp_res == null || exp_res.equalsIgnoreCase("")){
                            LOG.info("["+script.getFileName()+"]["+command.getCommand().trim()+"] is executed successfully");
                            continue;
                        }else {
                            script.addErrorCmd(command);
                            command.getResult().setErrorCode(RESULT.ERROR_CHECK_FAILED_CODE);
                            command.getResult().setErrorDesc(RESULT.ERROR_CHECK_FAILED_DESC);
                            command.getResult().setResult(RESULT.RESULT_TYPE_FAILED);
                            command.getResult().setExpResult(exp_res);
                            command.getResult().setActResult(act_res);
                            command.getResult().setRemark(command.getCommand()+"\n"+
                                    "[EXPECT RESULT]:\n"+exp_res+"\n"+
                                    "[ACTUAL RESULT]:\n"+act_res+"\n");
                            LOG.error("["+script.getFileName()+"]["+command.getCommand().trim()+"] is executed failed");
                            LOG.error("[EXPECT RESULT]:\n"+exp_res);
                            LOG.error("[ACTUAL RESULT]:\n"+act_res);
                            continue;
                        }
                    }

                    //if compare failed
                    if(!act_res.equalsIgnoreCase(exp_res)){
                        script.addErrorCmd(command);
                        command.getResult().setErrorCode(RESULT.ERROR_CHECK_FAILED_CODE);
                        command.getResult().setErrorDesc(RESULT.ERROR_CHECK_FAILED_DESC);
                        command.getResult().setResult(RESULT.RESULT_TYPE_FAILED);
                        command.getResult().setExpResult(exp_res);
                        command.getResult().setActResult(act_res);
                        command.getResult().setRemark(command.getCommand()+"\n"+
                                "[EXPECT RESULT]:\n"+exp_res+"\n"+
                                "[ACTUAL RESULT]:\n"+act_res+"\n");
                        LOG.error("["+script.getFileName()+"]["+command.getCommand().trim()+"] is executed failed");
                        LOG.error("[EXPECT RESULT]:\n"+exp_res);
                        LOG.error("[ACTUAL RESULT]:\n"+act_res);
                    }else {
                        //compare successfully
                        LOG.info("["+script.getFileName()+"]["+command.getCommand().trim()+"] is executed successfully");
                    }
                }
                statement.close();
            }catch (SQLException e) {
                if(null == command){
                    break;
                }



                act_res = e.getMessage();
                if( j < commands.size() -1)
                    exp_res = ResultParser.getRS(command.getCommand(),commands.get(j + 1).getCommand());
                else
                    exp_res = ResultParser.getRS(command.getCommand(),null);

                try {
                    if(connection == null || connection.isClosed()){
                        LOG.error("The connection has been lost,please check the logs .");
                        script.addNoExecCmd(command);
                        command.getResult().setErrorCode(RESULT.ERROR_CONNECTION_LOST_CODE);
                        command.getResult().setErrorDesc(RESULT.ERROR_CONNECTION_LOST_DESC);
                        command.getResult().setResult(RESULT.RESULT_TYPE_NOEXEC);
                        command.getResult().setExpResult(exp_res);
                        command.getResult().setActResult(RESULT.ERROR_CONNECTION_LOST_DESC);
                        command.getResult().setRemark(command.getCommand()+"\n"+
                                "[EXPECT RESULT]:\n"+exp_res+"\n"+
                                "[ACTUAL RESULT]:\n"+RESULT.ERROR_CONNECTION_LOST_DESC+"\n");
                        LOG.error("["+script.getFileName()+"]["+command.getCommand().trim()+"] is executed failed");
                        LOG.error("[EXPECT RESULT]:\n"+exp_res);
                        LOG.error("[ACTUAL RESULT]:\n"+RESULT.ERROR_CONNECTION_LOST_DESC);
                        continue;
                    }
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }

                if(act_res == null){
                    if(exp_res == null || exp_res.equalsIgnoreCase("")){
                        LOG.info("["+script.getFileName()+"]["+command.getCommand().trim()+"] is executed successfully");
                        continue;
                    }else {
                        script.addErrorCmd(command);
                        command.getResult().setErrorCode(RESULT.ERROR_CHECK_FAILED_CODE);
                        command.getResult().setErrorDesc(RESULT.ERROR_CHECK_FAILED_DESC);
                        command.getResult().setResult(RESULT.RESULT_TYPE_FAILED);
                        command.getResult().setExpResult(exp_res);
                        command.getResult().setActResult(act_res);
                        command.getResult().setRemark(command.getCommand()+"\n"+
                                "[EXPECT RESULT]:\n"+exp_res+"\n"+
                                "[ACTUAL RESULT]:\n"+act_res+"\n");
                        LOG.error("["+script.getFileName()+"]["+command.getCommand().trim()+"] is executed failed");
                        LOG.error("[EXPECT RESULT]:\n"+exp_res);
                        LOG.error("[ACTUAL RESULT]:\n"+act_res);
                        continue;
                    }
                }
                //if compare failed

                if(!act_res.equalsIgnoreCase(exp_res)){
                    script.addErrorCmd(command);
                    command.getResult().setErrorCode(RESULT.ERROR_CHECK_FAILED_CODE);
                    command.getResult().setErrorDesc(RESULT.ERROR_CHECK_FAILED_DESC);
                    command.getResult().setResult(RESULT.RESULT_TYPE_FAILED);
                    command.getResult().setExpResult(exp_res);
                    command.getResult().setActResult(act_res);
                    command.getResult().setRemark(command.getCommand()+"\n"+
                            "[EXPECT RESULT]:\n"+exp_res+"\n"+
                            "[ACTUAL RESULT]:\n"+act_res+"\n");
                    LOG.error("["+script.getFileName()+"]["+command.getCommand().trim()+"] is executed failed");
                    LOG.error("[EXPECT RESULT]:\n"+exp_res);
                    LOG.error("[ACTUAL RESULT]:\n"+act_res);
                }else {
                    //compare successfully
                    LOG.info("["+script.getFileName()+"]["+command.getCommand().trim()+"] is executed successfully");
                }
            }
        }

        //drop the test db
        dropTestDB(connection,def_db);

        long end = System.currentTimeMillis();
        script.setDuration((float)(end - start)/1000);
    }

    public static void run(ArrayList<TestSuite> suites,String path) {
        ConnectionManager.reset();
        Connection connection = ConnectionManager.getConnection();
        if(connection == null){
            LOG.error("No valid connnection,please check the config..");
            //System.exit(1);
        }

        boolean hasResults = false;
        Statement statement = null;

        //check whether the result file exists
        File rsf = new File(path.replaceFirst(COMMON.CASES_PATH,COMMON.RESULT_PATH).replaceAll("\\.[A-Za-z]+",COMMON.R_FILE_SUFFIX));
        if(!rsf.exists()) {
            LOG.warn("The result of the test script file["+path+"] does not exists,please check....");

            //set all the suites and cases to RESULT_TYPE_NOEXEC
            TestResult result = new TestResult();
            result.setResult(RESULT.RESULT_TYPE_NOEXEC);
            result.setErrorCode(RESULT.ERROR_NOT_EXEC_CODE);
            result.setErrorDesc(RESULT.ERROR_NOT_EXEC_DESC);
            for(int i = 0; i < suites.size();i++){
                suites.get(i).setResult(result);
            }
            return;
        }

        //create a database named filename for test;
        String def_db = rsf.getName().substring(0,rsf.getName().indexOf(COMMON.R_FILE_SUFFIX));
        createTestDB(connection,def_db);

        ResultParser.reset();
        ResultParser.parse(rsf.getPath());

        String exp_res = null;
        String act_res = null;

        for(int i = 0; i < suites.size();i++){
            TestSuite suite = suites.get(i);
            ArrayList<SqlCommand> setups = suite.getSetupSqls();
            LOG.info("["+path+"][suite]["+i+"] is now being executed.......................................................");
            for(int j = 0; j < setups.size();j++){
                SqlCommand command = null;
                try {
                    command = setups.get(j);
                    connection = getConnection(command);
                    if(connection == null){
                        LOG.error("No valid connnection,please check the config..");
                        //System.exit(1);
                    }
                    statement = connection.createStatement();
                    if (command.isUpdate()) {
                        //if no-query-type statement is executed successfully,do not need check
                        int num = statement.executeUpdate(command.getCommand());
                        //but need to get the expected result,to skip the read pos
                        ResultParser.skip(command.getCommand());
                        LOG.info("["+path+"][suite]["+i+"][setup]"+command.getCommand().trim()+"] is executed successfully");
                    }
                    statement.close();
                }catch (SQLException e) {
                    LOG.error("["+path+"][suite]["+i+"][setup]"+command.getCommand().trim()+"] is executed failed,maybe will cause the following cases error");
                    ResultParser.skip(command.getCommand());
                }
            }

            ArrayList<TestCase> cases = suite.getCases();
            for(int j = 0; j < cases.size(); j++){
                TestCase testCase = cases.get(j);
                LOG.info("["+path+"][suite]["+i+"][case:"+testCase.getDesc()+"] is now being executed.......................................................");
                ArrayList<SqlCommand> sqlCommands = testCase.getCommands();
                for(int k = 0; k < sqlCommands.size();k++){
                    SqlCommand command = sqlCommands.get(k);
                    connection = getConnection(command);
                    try {
                        statement = connection.createStatement();
                        if (command.isUpdate()) {
                            //if no-query-type statement is executed successfully,do not need check
                            int num = statement.executeUpdate(command.getCommand());
                            //but need to get the expected result,to skip the read pos
                            ResultParser.skip(command.getCommand());
                            LOG.info("["+path+"]["+command.getCommand().trim()+"] is executed successfully");
                        } else {
                            //if query-type statment is executed successfully,need compare the expected result and the actual result
                            hasResults = statement.execute(command.getCommand());
                            act_res = getRS(statement.getResultSet());
                            /*if( k < sqlCommands.size() -1)
                                exp_res = ResultParser.getRS(command.getCommand(),sqlCommands.get(k + 1).getCommand());
                            else
                                exp_res = ResultParser.getRS(command.getCommand(),null);*/
                            exp_res =  ResultParser.getRS(command.getCommand(),command.getNext().getCommand());

                            if(act_res == null){
                                if(exp_res == null || exp_res.equalsIgnoreCase("")){
                                    LOG.info("["+path+"]["+command.getCommand().trim()+"] is executed successfully");
                                    continue;
                                }else {
                                    command.getResult().setErrorCode(RESULT.ERROR_CHECK_FAILED_CODE);
                                    command.getResult().setErrorDesc(RESULT.ERROR_CHECK_FAILED_DESC);
                                    command.getResult().setResult(RESULT.RESULT_TYPE_FAILED);
                                    command.getResult().setExpResult(exp_res);
                                    command.getResult().setActResult(act_res);
                                    command.getResult().setRemark(
                                            "#sql#:\n"+command.getCommand()+
                                                    "#excpect#:\n"+exp_res+"\n"+
                                                    "#actual #:\n"+act_res+"\n");
                                    testCase.setResult(command.getResult());
                                    LOG.error("["+path+"]["+command.getCommand().trim()+"] is executed failed");
                                    LOG.error("[EXPECT RESULT]:\n"+exp_res);
                                    LOG.error("[ACTUAL RESULT]:\n"+act_res);
                                }
                            }


                            //if compare failed
                            if(!act_res.equalsIgnoreCase(exp_res)){
                                command.getResult().setErrorCode(RESULT.ERROR_CHECK_FAILED_CODE);
                                command.getResult().setErrorDesc(RESULT.ERROR_CHECK_FAILED_DESC);
                                command.getResult().setResult(RESULT.RESULT_TYPE_FAILED);
                                command.getResult().setExpResult(exp_res);
                                command.getResult().setActResult(act_res);
                                command.getResult().setRemark(
                                        "#sql#:\n"+command.getCommand()+
                                        "#excpect#:\n"+exp_res+"\n"+
                                        "#actual #:\n"+act_res+"\n");

                                testCase.setResult(command.getResult());
                                testCase.addRemark(command.getResult().getRemark());

                                LOG.error("["+path+"]["+command.getCommand().trim()+"] is executed failed");
                                LOG.error("[EXPECT RESULT]:\n"+exp_res);
                                LOG.error("[ACTUAL RESULT]:\n"+act_res);
                            }else {
                                //compare successfully
                                LOG.info("["+path+"]["+command.getCommand().trim()+"] is executed successfully");
                            }
                        }
                        statement.close();
                    }catch (SQLException e) {
                        if(null == command){
                            break;
                        }



                        act_res = e.getMessage();
                        /*if( k < sqlCommands.size() -1)
                            exp_res = ResultParser.getRS(command.getCommand(),sqlCommands.get(k + 1).getCommand());
                        else
                            exp_res = ResultParser.getRS(command.getCommand(),null);*/
                        exp_res =  ResultParser.getRS(command.getCommand(),command.getNext().getCommand());

                        try {
                            if(connection == null || connection.isClosed()){
                                LOG.error("The connection has been lost,please check the logs .");
                                testCase.setResult(command.getResult());
                                command.getResult().setErrorCode(RESULT.ERROR_CONNECTION_LOST_CODE);
                                command.getResult().setErrorDesc(RESULT.ERROR_CONNECTION_LOST_DESC);
                                command.getResult().setResult(RESULT.RESULT_TYPE_NOEXEC);
                                command.getResult().setExpResult(exp_res);
                                command.getResult().setActResult(RESULT.ERROR_CONNECTION_LOST_DESC);
                                command.getResult().setRemark(command.getCommand()+"\n"+
                                        "[EXPECT RESULT]:\n"+exp_res+"\n"+
                                        "[ACTUAL RESULT]:\n"+RESULT.ERROR_CONNECTION_LOST_DESC+"\n");
                                LOG.error("["+path+"]["+command.getCommand().trim()+"] is executed failed");
                                LOG.error("[EXPECT RESULT]:\n"+exp_res);
                                LOG.error("[ACTUAL RESULT]:\n"+RESULT.ERROR_CONNECTION_LOST_DESC);
                                continue;
                            }
                        } catch (SQLException ex) {
                            throw new RuntimeException(ex);
                        }

                        if(act_res == null){
                            if(exp_res == null || exp_res.equalsIgnoreCase("")){
                                LOG.info("["+path+"]["+command.getCommand().trim()+"] is executed successfully");
                                continue;
                            }else {
                                command.getResult().setErrorCode(RESULT.ERROR_CHECK_FAILED_CODE);
                                command.getResult().setErrorDesc(RESULT.ERROR_CHECK_FAILED_DESC);
                                command.getResult().setResult(RESULT.RESULT_TYPE_FAILED);
                                command.getResult().setExpResult(exp_res);
                                command.getResult().setActResult(act_res);
                                command.getResult().setRemark(
                                        "#sql#:\n"+command.getCommand()+
                                                "#excpect#:\n"+exp_res+"\n"+
                                                "#actual #:\n"+act_res+"\n");
                                testCase.setResult(command.getResult());
                                LOG.error("["+path+"]["+command.getCommand().trim()+"] is executed failed");
                                LOG.error("[EXPECT RESULT]:\n"+exp_res);
                                LOG.error("[ACTUAL RESULT]:\n"+act_res);
                            }
                        }

                        //if compare failed
                        if(!act_res.equalsIgnoreCase(exp_res)){
                            command.getResult().setErrorCode(RESULT.ERROR_CHECK_FAILED_CODE);
                            command.getResult().setErrorDesc(RESULT.ERROR_CHECK_FAILED_DESC);
                            command.getResult().setResult(RESULT.RESULT_TYPE_FAILED);
                            command.getResult().setExpResult(exp_res);
                            command.getResult().setActResult(act_res);
                            command.getResult().setRemark(
                                    "#sql#:\n"+command.getCommand()+
                                            "#excpect#:\n"+exp_res+"\n"+
                                            "#actual #:\n"+act_res+"\n");
                            testCase.setResult(command.getResult());
                            LOG.error("["+path+"]["+command.getCommand().trim()+"] is executed failed");
                            LOG.error("[EXPECT RESULT]:\n"+exp_res);
                            LOG.error("[ACTUAL RESULT]:\n"+act_res);
                        }else {
                            //compare successfully
                            LOG.info("["+path+"]["+command.getCommand().trim()+"] is executed successfully");
                        }
                    }
                }
            }
        }

        //drop the test db
        dropTestDB(connection,def_db);
    }

    public static void genRS(TestScript script){
        ConnectionManager.reset();
        Connection connection = ConnectionManager.getConnection();

        boolean hasResults = false;
        Statement statement = null;
        BufferedWriter rs_writer;
        //check whether the result dir exists
        File rsf = new File(script.getFileName().replaceFirst(COMMON.CASES_PATH,COMMON.RESULT_PATH).replaceAll("\\.[A-Za-z]+",COMMON.R_FILE_SUFFIX));
        if(!rsf.getParentFile().exists()){
            rsf.getParentFile().mkdirs();
        }

        //create a database named filename for test;
        String def_db = rsf.getName().substring(0,rsf.getName().indexOf(COMMON.R_FILE_SUFFIX));
        createTestDB(connection,def_db);

        try {
            rs_writer = new BufferedWriter(new FileWriter(rsf.getPath()));
            ArrayList<SqlCommand> commands = script.getCommands();
            for (int j = 0; j < commands.size(); j++) {
                SqlCommand command = null;
                try{
                    command = commands.get(j);
                    connection = getConnection(command);
                    statement = connection.createStatement();
                    if (command.isUpdate()) {
                        int num = statement.executeUpdate(command.getCommand());
                        rs_writer.write(command.getCommand().trim());
                        rs_writer.newLine();
                    } else {
                        hasResults = statement.execute(command.getCommand());
                        rs_writer.write(command.getCommand().trim());
                        rs_writer.newLine();
                        rs_writer.write(getRS(statement.getResultSet()));
                        rs_writer.newLine();
                        //writeRS(rs_writer,statement.getResultSet());
                    }
                    statement.close();
                }catch (SQLException e) {
                    if(null == command){
                        break;
                    }
                    rs_writer.write(command.getCommand().trim());
                    rs_writer.newLine();
                    rs_writer.write(e.getMessage());
                    rs_writer.newLine();
                }
            }
            rs_writer.flush();
            rs_writer.close();
            //drop the test db
            dropTestDB(connection,def_db);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    public static void writeRS(BufferedWriter writer, ResultSet rs){
        ResultSetMetaData md = null;
        StringBuffer labels = new StringBuffer();
        StringBuffer[] values;
        try {
            md = rs.getMetaData();
            int cols = md.getColumnCount();
            values = new StringBuffer[COMMON.MAX_ROW_COUNT_IN_RS];
            for(int i = 0; i < cols; ++i) {
                labels.append(md.getColumnLabel(i + 1));
                if(i < cols -1)
                    labels.append("\t");
            }

            writer.write(labels.toString());
            writer.newLine();

            int i = 0;
            while(rs.next()) {
                values[i] = new StringBuffer();
                for(int j = 0; j < cols; ++j) {
                    values[i].append(rs.getString(j+1));
                    if(j < cols -1 )
                        values[i].append("\t");
                }
                writer.write(values[i].toString());
                writer.newLine();
                i++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }catch(NumberFormatException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static String getRS(ResultSet rs){
        ResultSetMetaData md = null;
        StringBuffer result = new StringBuffer();

        if(rs == null){
            LOG.warn("The MO return a wired resultset,it should not be null ,but it is.");
            return null;
        }

        try {
            md = rs.getMetaData();
            int cols = md.getColumnCount();
            for(int i = 0; i < cols; ++i) {
                result.append(md.getColumnLabel(i + 1));
                if(i < cols -1)
                    result.append("\t");
            }
            result.append("\n");

            int i = 0;
            while(rs.next()) {
                for(int j = 0; j < cols; ++j) {
                    result.append(rs.getString(j+1));
                    if(j < cols -1 )
                        result.append("\t");
                }
                result.append("\n");
                i++;
            }
            //delete the last "\n"
            return result.toString().trim();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }catch(NumberFormatException e){
            return RESULT.ERROR_UNKNOWN_DESC + e.getMessage();
        }
        return null;
    }


    public static Connection getConnection(SqlCommand command){
        Connection connection = null;
        if(command.getConn_id() != 0){
            if(command.getConn_user() == null){
                connection = ConnectionManager.getConnection(command.getConn_id());
                try {
                    if (isTrxBeginning(command)) {
                        connection.setAutoCommit(false);
                    }

                    if(isTrxEndding(command)){
                        connection.setAutoCommit(true);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                return connection;
            }else {
                connection = ConnectionManager.getConnection(command.getConn_id(),command.getConn_user(),command.getConn_pswd());
                return connection;
            }
        }
        connection = ConnectionManager.getConnection();
        return connection;
    }

    public static boolean isTrxBeginning(SqlCommand command){
        if(command.getCommand().toLowerCase().startsWith(COMMON.START_TRX))
            return true;

        return false;
    }

    public static boolean isTrxEndding(SqlCommand command){
        if(command.getCommand().toLowerCase().startsWith(COMMON.COMMIT_TRX) || command.getCommand().toLowerCase().startsWith(COMMON.ROLLBACK_TRX))
            return true;

        return false;
    }

    public static  void createTestDB(Connection connection,String name){
        if(connection == null) return;
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.executeUpdate("create database IF NOT EXISTS `"+name+"`;");
            statement.executeUpdate("use `"+name+"`;");
        } catch (SQLException e) {
            LOG.error("create database "+name+"is failed.cause: "+e.getMessage());
        }
    }

    public static  void dropTestDB(Connection connection,String name){
        if(connection == null) return;
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.executeUpdate("drop database IF EXISTS `"+name+"`;");
        } catch (SQLException e) {
            //e.printStackTrace();
            LOG.error("drop database "+name+"is failed.cause: "+e.getMessage());
        }

    }
}
