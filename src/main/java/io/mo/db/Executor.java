package io.mo.db;

import io.mo.cases.RegexPattern;
import io.mo.cases.SqlCommand;
import io.mo.cases.TestScript;
import io.mo.constant.COMMON;
import io.mo.constant.RESULT;
import io.mo.result.RSSet;
import io.mo.result.StmtResult;
import io.mo.util.MoConfUtil;
import io.mo.util.ParseResult;
import io.mo.util.ResultParser;
import io.mo.util.RunConfUtil;
import org.apache.log4j.Logger;

import java.io.*;
import java.sql.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Executor {

    /**
     * Functional interface for post-SQL execution processing
     */
    @FunctionalInterface
    public interface PostSqlHandler {
        /**
         * Process the result after SQL execution
         * @param statement The executed statement
         * @param updateCount The update count for DDL/DML statements, or -1 for queries
         * @param resultSet The result set for queries, or null for DDL/DML statements
         * @throws SQLException if processing fails
         */
        void handle(Statement statement, int updateCount, ResultSet resultSet) throws SQLException;
    }

    /**
     * Pre-compiled regex patterns for better performance
     */
    private static final Pattern FLUSH_SQL_PATTERN = Pattern.compile(
        "(?i)(select\\s+mo_ctl\\s*\\(\\s*['\"]dn['\"]\\s*,\\s*['\"]flush['\"]\\s*,\\s*['\"])([^'\"]+)\\.([^'\"]+)(['\"]\\s*\\))",
        Pattern.CASE_INSENSITIVE
    );

    private final ConnectionManager connectionManager;
    private final Logger logger;
    private Thread waitThread = null;
    private int accountId = 0;

    public Executor() {
        this.connectionManager = new ConnectionManager();
        this.logger = Logger.getLogger(Executor.class.getName());
    }

    public Executor(String userName, String password) {
        this.connectionManager = new ConnectionManager(userName, password);
        this.logger = Logger.getLogger(Executor.class.getName()+"2");
    }

    /**
     * run test file function
     */
    public void run(TestScript script) {

        int last_commit_id = 0;

        if (script.isSkiped()) {
            logger.info(String.format("Has skipped the script file [%s]", script.getFileName()));
            return;
        }

        logger.info("Start to execute the script file[" + script.getFileName()
                + "] now, and it will take a few moment,pleas wait......");
        connectionManager.reset();

        // create a database named filename for test;
        Connection connection = connectionManager.getConnection();
        createTestDB(connection, script.getUseDB());

        // load expected results from result file
        ParseResult parseResult = ResultParser.loadExpectedResultsFromFile(script);

        // if result file is parsed failed,return
        if (parseResult.isFailure()) {
            logger.info("The script file[" + script.getFileName() + "] has been executed" +
                    ", and cost: " + script.getDuration() + "s" +
                    ", total:" + script.getCommands().size() +
                    ", success:" + script.getSuccessCmdCount() +
                    ", failed:" + script.getFailedCmdCount() +
                    ", ignored:" + script.getIgnoredCmdCount() +
                    ", abnoraml:" + script.getAbnormalCmdCount());
            dropTestDB(connection, script.getUseDB());
            return;
        }

        // run all the sql commands
        Statement statement = null;
        ArrayList<SqlCommand> commands = script.getCommands();
        long start = System.currentTimeMillis();
        boolean[] transformed = new boolean[1];

        // for (SqlCommand command : commands) {
        for (int i = 0; i < commands.size(); i++) {
            transformed[0] = false;
            SqlCommand command = commands.get(i);

            // if need to sleep
            if (command.getSleeptime() > 0) {
                logger.info(String.format("The tester will sleep for %s s, please wait....", command.getSleeptime()));
                command.sleep();
            }

            // if there are some system commnds need to be executed
            if (command.getSysCMDS().size() != 0) {
                for (String cmd : command.getSysCMDS()) {
                    logger.info(String.format("Start to execute system command [ %s ] in script file[%s]", cmd,
                            script.getFileName()));
                    executeSysCmd(cmd);
                }
            }

            // if the the command is marked to ignore flag and the IGNORE_MODEL = true
            // skip the command directly
            if (COMMON.IGNORE_MODEL && command.isIgnore()) {
                logger.debug(
                        "Ignored sql command: [issue#" + command.getIssueNo() + "][" + script.getFileName() + "][row:"
                                + command.getPosition() + "][" + command.getCommand().trim() + "]");
                script.addIgnoredCmd(command);
                command.getTestResult().setResult(RESULT.RESULT_TYPE_IGNORED);
                continue;
            }

            String cmd = command.getCommand();
            if (cmd == null || cmd.trim().isEmpty()) {
                logger.warn("[" + script.getFileName() + "][row:" + command.getPosition()
                        + "] Command is empty, skip it.");
                script.addIgnoredCmd(command);
                command.getTestResult().setResult(RESULT.RESULT_TYPE_IGNORED);
                continue;
            }

            String sqlCmd = transformFlushSql(cmd, transformed);
            if (transformed[0]) {
                connection = connectionManager.getConnectionForSys();
            } else {
                connection = getConnection(command);
            }

            // if can not get valid connection,put the command to the abnormal commands
            // array
            if (connection == null) {
                logger.error("[" + script.getFileName() + "][row:" + command.getPosition() + "]["
                        + command.getCommand().trim() + "] can not get invalid connection,con[id="
                        + command.getConn_id() + ", user=" + command.getConn_user() + ", pwd=" + command.getConn_pswd()
                        + "].");
                script.addAbnoramlCmd(command);
                command.getTestResult().setResult(RESULT.RESULT_TYPE_ABNORMAL);
                command.getTestResult().setActResult(RESULT.ERROR_CAN_NOT_GET_CONNECTION_DESC);
                command.getTestResult().setExpResult(command.getExpResult().toString());
                logger.error("[" + script.getFileName() + "][row:" + command.getPosition() + "]["
                        + command.getCommand().trim() + "] was executed failed");
                logger.error("[EXPECT RESULT]:\n" + command.getTestResult().getExpResult());
                logger.error("[ACTUAL RESULT]:\n" + command.getTestResult().getActResult());
                continue;
            }

            if (last_commit_id != command.getConn_id()) {
                logger.debug(String.format("[%s][row:%d][%s]Connection id had been turned from %d to %d",
                        command.getScriptFile(), command.getPosition(), command.getCommand(),
                        last_commit_id, command.getConn_id()));
                syncCommit();
            }

            last_commit_id = command.getConn_id();

            try {
                command.setUseDB(connection.getCatalog());
                statement = connection.createStatement();
                sqlCmd = sqlCmd.replaceAll(COMMON.RESOURCE_PATH_FLAG, COMMON.RESOURCE_PATH);
                if (command.isNeedWait()) {
                    execWaitOperation(command);
                }
                statement.execute(sqlCmd);
                if (command.isNeedWait()) {
                    Thread.sleep(COMMON.WAIT_TIMEOUT / 10);
                    if (waitThread != null && waitThread.isAlive()) {
                        try {
                            logger.error(String.format(
                                    "Command[%s][row:%d] has been executed before connection[id=%d] commit.\nBut still need to wait for connection[id=%d] being committed",
                                    command.getCommand(), command.getPosition(), command.getWaitConnId(),
                                    command.getWaitConnId()));
                            waitThread.join();
                            script.addFailedCmd(command);
                            command.getTestResult().setResult(RESULT.RESULT_TYPE_FAILED);
                            logger.error("[" + script.getFileName() + "][row:" + command.getPosition() + "]["
                                    + command.getCommand().trim() + "] was executed failed, con[id="
                                    + command.getConn_id() + ", user=" + command.getConn_user() + ", pwd="
                                    + command.getConn_pswd() + "].");
                            continue;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                ResultSet resultSet = statement.getResultSet();
                if (resultSet != null) {
                    RSSet rsSet = new RSSet(resultSet, command);
                    StmtResult actResult = new StmtResult(rsSet);
                    command.setActResult(actResult);
                    command.getTestResult().setActResult(actResult.toString());
                    // expResult is already fully processed in ResultParser.parseCommand
                } else {
                    StmtResult actResult = new StmtResult();
                    actResult.setType(RESULT.STMT_RESULT_TYPE_NONE);
                    command.setActResult(actResult);
                }

                checkResult(command, script);
                statement.close();

            } catch (SQLException e) {
                try {
                    if (connection.isClosed() || !connection.isValid(10)) {
                        logger.error("[" + script.getFileName() + "][row:" + command.getPosition() + "]["
                                + command.getCommand().trim() + "] MO does not return result in "
                                + MoConfUtil.getSocketTimeout() + " ms,con[id="
                                + command.getConn_id() + ", user=" + command.getConn_user() + ", pwd="
                                + command.getConn_pswd() + "].");
                        script.addAbnoramlCmd(command);
                        command.getTestResult().setResult(RESULT.RESULT_TYPE_ABNORMAL);
                        command.getTestResult().setActResult(
                                String.format(RESULT.ERROR_EXECUTE_TIMEOUT_DESC, MoConfUtil.getSocketTimeout()));
                        logger.error("[" + script.getFileName() + "][row:" + command.getPosition() + "]["
                                + command.getCommand().trim() + "] was executed failed, con[id="
                                + command.getConn_id() + ", user=" + command.getConn_user() + ", pwd="
                                + command.getConn_pswd() + "].");
                        logger.error("[EXPECT RESULT]:\n" + command.getTestResult().getExpResult());
                        logger.error("[ACTUAL RESULT]:\n" + command.getTestResult().getActResult());

                        if (COMMON.NEED_PPROF) {
                            logger.info("Start to collect pprof information,please wait........");
                            pprof();
                            logger.info("Finish to collect pprof information,the test will continue");
                        }

                        // reconnect to mo, and set db to last use db
                        logger.warn(String.format(
                                "The mo-tester tries to re-connect to mo, con[id=%d, user=%s, pwd=%s, db=%s], please wait.....",
                                command.getConn_id(), command.getConn_user(), command.getConn_pswd(),
                                command.getUseDB()));
                        try {
                            connection.close();
                        } catch (SQLException ex) {
                            logger.warn(String.format("Failed to close connection[id=%d], but effect nothing.",
                                    command.getConn_id()));
                        }
                        connection = getConnection(command);
                        if (connection != null && !connection.isClosed()) {
                            connection.setCatalog(command.getUseDB());
                            syncCommit();
                        }
                        continue;
                    }

                    StmtResult actResult = new StmtResult();
                    actResult.setType(RESULT.STMT_RESULT_TYPE_ERROR);
                    actResult.setErrorMessage(e.getMessage());
                    command.setActResult(actResult);

                    command.getExpResult().setType(RESULT.STMT_RESULT_TYPE_ERROR);
                    command.getExpResult().setErrorMessage(command.getExpResult().getExpectRSText());

                    checkResult(command, script);
                    statement.close();

                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // drop the test db
        dropTestDB(connection, script.getUseDB());

        long end = System.currentTimeMillis();
        script.setDuration((float) (end - start) / 1000);

        logger.info("The script file[" + script.getFileName() + "] has been executed" +
                ", and cost: " + script.getDuration() + "s" +
                ", total:" + script.getCommands().size() +
                ", success:" + script.getSuccessCmdCount() +
                ", failed:" + script.getFailedCmdCount() +
                ", ignored:" + script.getIgnoredCmdCount() +
                ", abnoraml:" + script.getAbnormalCmdCount());
    }

    private void checkResult(SqlCommand command, TestScript script) {
        // check whether the execution result is successful
        if (command.checkResult()) {
            script.addSuccessCmd(command);
            command.getTestResult().setResult(RESULT.RESULT_TYPE_PASS);
        } else {
            script.addFailedCmd(command);
            command.getTestResult().setResult(RESULT.RESULT_TYPE_FAILED);
            logger.error("[" + script.getFileName() + "][row:" + command.getPosition() + "]["
                    + command.getCommand().trim() + "] was executed failed, con[id="
                    + command.getConn_id() + ", user=" + command.getConn_user() + ", pwd="
                    + command.getConn_pswd() + "].");
            logger.error("[EXPECT RESULT]:\n" + command.getTestResult().getExpResult());
            logger.error("[ACTUAL RESULT]:\n" + command.getTestResult().getActResult());
        }
    }

    public boolean genRS(TestScript script) {
        connectionManager.reset();
        Connection connection = connectionManager.getConnection();

        Statement statement;
        // check whether the result file exists
        File rsf = new File(script.getFileName().replaceAll("\\.[A-Za-z]+", COMMON.R_FILE_SUFFIX));

        if (!rsf.exists()) {
            rsf = new File(script.getFileName().replaceFirst(COMMON.CASES_DIR, COMMON.RESULT_DIR)
                    .replaceAll("\\.[A-Za-z]+", COMMON.R_FILE_SUFFIX));
        }

        if (!rsf.exists()) {
            rsf = new File(script.getFileName().replaceAll("\\.[A-Za-z]+", COMMON.R_FILE_SUFFIX));
        }

        // create a database named filename for test;
        createTestDB(connection, script.getUseDB());

        try (BufferedWriter rs_writer = new BufferedWriter(new FileWriter(rsf.getPath()))) {
            ArrayList<SqlCommand> commands = script.getCommands();
            for (int j = 0; j < commands.size(); j++) {
                SqlCommand command = null;

                // if need to sleep

                try {
                    command = commands.get(j);

                    if (command.getSleeptime() > 0) {
                        logger.info(String.format("The tester will sleep for %s s, please wait....",
                                command.getSleeptime()));
                        command.sleep();
                    }

                    // if there are some system commnds need to be executed
                    if (command.getSysCMDS().size() != 0) {
                        for (String cmd : command.getSysCMDS()) {
                            logger.info(String.format("Start to execute system command [ %s ] in script file[%s]", cmd,
                                    script.getFileName()));
                            executeSysCmd(cmd);
                        }
                    }

                    if (command.isIgnore()) {
                        rs_writer.write(command.getCommand().trim());
                        rs_writer.newLine();
                        writeRegexPatterns(rs_writer, command);
                        rs_writer.write(
                                "[unknown result because it is related to issue#" + command.getIssueNo() + "]");
                        if (j < commands.size() - 1)
                            rs_writer.newLine();
                        continue;
                    }

                    connection = getConnection(command);
                    statement = connection.createStatement();

                    String sqlCmd = command.getCommand().replaceAll("\\$resources", COMMON.RESOURCE_PATH);
                    if (command.isNeedWait()) {
                        execWaitOperation(command);
                    }
                    statement.execute(sqlCmd);
                    if (command.isNeedWait()) {
                        Thread.sleep(COMMON.WAIT_TIMEOUT / 10);
                        if (waitThread != null && waitThread.isAlive()) {
                            try {
                                logger.error(String.format(
                                        "Command[%s][row:%d] has been executed before connection[id=%d] commit.\nBut still need to wait for connection[id=%d] being committed",
                                        command.getCommand(), command.getPosition(), command.getWaitConnId(),
                                        command.getWaitConnId()));
                                waitThread.join();
                                script.addFailedCmd(command);
                                command.getTestResult().setResult(RESULT.RESULT_TYPE_FAILED);
                                logger.error("[" + script.getFileName() + "][row:" + command.getPosition() + "]["
                                        + command.getCommand().trim() + "] was executed failed, con[id="
                                        + command.getConn_id() + ", user=" + command.getConn_user() + ", pwd="
                                        + command.getConn_pswd() + "].");
                                continue;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    ResultSet resultSet = statement.getResultSet();
                    if (resultSet != null) {
                        RSSet rsSet = new RSSet(resultSet, command);
                        StmtResult actResult = new StmtResult(rsSet);
                        actResult.setCommand(command);
                        rs_writer.write(command.getCommand().trim());
                        rs_writer.newLine();
                        writeRegexPatterns(rs_writer, command);
                        rs_writer.write(actResult.toString());

                        if (j < commands.size() - 1)
                            rs_writer.newLine();
                    } else {
                        rs_writer.write(command.getCommand().trim());
                        writeRegexPatterns(rs_writer, command);
                        if (command.isIgnore()) {
                            rs_writer.newLine();
                            rs_writer.write(
                                    "[unknown result because it is related to issue#" + command.getIssueNo() + "]");
                        }
                        if (j < commands.size() - 1)
                            rs_writer.newLine();
                    }
                    statement.close();
                } catch (SQLException e) {
                    rs_writer.write(command.getCommand().trim());
                    rs_writer.newLine();
                    writeRegexPatterns(rs_writer, command);
                    if (command.isIgnore()) {
                        rs_writer.write(
                                "[unknown result because it is related to issue#" + command.getIssueNo() + "]");
                    } else
                        rs_writer.write(e.getMessage());

                    if (j < commands.size() - 1)
                        rs_writer.newLine();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            rs_writer.newLine();
            rs_writer.flush();
            // drop the test db
            dropTestDB(connection, script.getUseDB());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public Connection getConnection(SqlCommand command) {
        if (command.getConn_user() == null) {
            return connectionManager.getConnection(command.getConn_id());
        } else {
            return connectionManager.getConnection(command.getConn_id(), command.getConn_user(),
                    command.getConn_pswd());
        }
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public int createAccountForTest() {
        final int[] accountId = new int[1];
        executeSqlInternal(null, "create account if not exists shuyuan ADMIN_NAME 'kongzi' IDENTIFIED BY '111';", null);
        boolean success = executeSqlInternal(null, 
            "select account_id from mo_catalog.mo_account where account_name = 'shuyuan';",
            (stmt, updateCount, rs) -> {
                if (rs != null && rs.next()) {
                    accountId[0] = rs.getInt(1);
                }
            });
        
        if (success && accountId[0] > 0) {
            return accountId[0];
        }
        throw new RuntimeException("Failed to create account for test");
    }

    public void dropAccountForTest() {
        executeSqlInternal(null, "drop account if exists shuyuan;", null);
    }

    /**
     * Transform SQL: if it's select mo_ctl('dn', 'flush', '<db>.<table>'),
     * convert it to select mo_ctl('dn', 'flush', '<db>.<table>.<account_id>')
     * @param sql The original SQL statement
     * @param transformed Output parameter: will be set to true if transformation occurred
     * @return The transformed SQL, or original SQL if pattern doesn't match
     */
    public String transformFlushSql(String sql, boolean[] transformed) {
        if (transformed != null && transformed.length > 0) {
            transformed[0] = false;
        }
        
        if (accountId <= 0) {
            return sql;
        }
        
        Matcher matcher = FLUSH_SQL_PATTERN.matcher(sql);
        if (matcher.find()) {
            String prefix = matcher.group(1);
            String db = matcher.group(2);
            String table = matcher.group(3);
            String quote = matcher.group(4);
            String replacement = prefix + db + "." + table + "." + accountId + quote;
            if (transformed != null && transformed.length > 0) {
                transformed[0] = true;
            }
            return matcher.replaceFirst(Matcher.quoteReplacement(replacement));
        }
        
        return sql;
    }
    
    /**
     * Internal method to execute SQL without exiting on error
     * @param connection The database connection (null to use system connection)
     * @param sql The SQL statement to execute
     * @param postHandler Optional handler for post-execution processing
     * @param errorMessage Custom error message prefix (null to use default)
     * @return true if executed successfully, false otherwise
     */
    private boolean executeSqlInternal(Connection connection, String sql, PostSqlHandler postHandler) {
        Connection conn = connection == null ? connectionManager.getConnectionForSys() : connection;
        if (conn == null) {
            logger.error("Failed to execute sql " + sql + ", no connection available, noop");
            return false;
        }
        try (Statement statement = conn.createStatement()) {
            boolean hasResultSet = statement.execute(sql);
            int updateCount = -1;
            ResultSet resultSet = null;
            
            if (hasResultSet) {
                resultSet = statement.getResultSet();
            } else {
                updateCount = statement.getUpdateCount();
            }
            
            // Execute post-sql handler if provided
            if (postHandler != null) {
                postHandler.handle(statement, updateCount, resultSet);
            }
            
            // Close result set if it was opened and handler didn't close it
            if (resultSet != null && !resultSet.isClosed()) {
                resultSet.close();
            }
            return true;
        } catch (SQLException e) {
            logger.error("Failed to execute sql " + sql + ", cause: " + e.getMessage());
            return false;
        }
    }

    public void createTestDB(Connection connection, String name) {
        if (connection == null)
            return;
        executeSqlInternal(connection, "create database IF NOT EXISTS `" + name + "`;",
            (stmt, updateCount, rs) -> {
                try {
                    connection.setCatalog(name);
                } catch (SQLException e) {
                    logger.error("Failed to set catalog to " + name + ", cause: " + e.getMessage());
                }
            });
    }

    public void dropTestDB(Connection connection, String name) {
        if (connection == null)
            return;
        executeSqlInternal(connection, "drop database IF EXISTS `" + name + "`;", null);
    }

    public void syncCommit() {
        executeSqlInternal(null, "select mo_ctl('cn','synccommit','')",
            (stmt, updateCount, rs) -> {
                logger.debug("select mo_ctl('cn','synccommit','') with sys user[" 
                    + MoConfUtil.getSysUserName()  + "] successfully.");
            });
    }

    public void pprof() {
        String[] debugServers = MoConfUtil.getDebugServers();
        if (debugServers != null) {
            int port = MoConfUtil.getDebugPort();
            Thread[] threads = new Thread[debugServers.length];
            for (int i = 0; i < debugServers.length; i++) {
                int index = i;
                threads[index] = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Process p = Runtime.getRuntime()
                                    .exec(String.format("./pprof.sh -h %s -p %d", debugServers[index], port));
                            InputStream is = p.getInputStream();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                            p.waitFor();

                            StringBuffer execResut = new StringBuffer();
                            String str = reader.readLine();
                            while (str != null) {
                                execResut.append(str + "\n");
                                str = reader.readLine();
                            }

                            if (p.exitValue() != 0) {
                                logger.error(String.format("The pprof operation has been executed failed.\n%s",
                                        execResut.toString()));
                            } else {
                                logger.info(String.format("The pprof operation has been executed successfully.\n%s",
                                        execResut.toString()));
                                logger.info(String.format("The result is in the dir ./report/pprof/%s/",
                                        debugServers[index]));
                            }

                        } catch (IOException e) {
                            logger.error("The pprof operation has been executed failed.");
                            logger.error(String.format("The output of pprof operation is \n%s.", e.getMessage()));
                        } catch (InterruptedException e) {
                            logger.error("The pprof operation has been executed failed.");
                            logger.error(String.format("The output of pprof operation is \n%s.", e.getMessage()));
                        }
                    }
                });
                threads[index].start();
            }

            boolean finished = false;
            while (!finished) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                for (Thread thread : threads) {
                    if (thread.isAlive()) {
                        finished = false;
                        break;
                    }
                    finished = true;
                }
            }
        }

    }

    public void executeSysCmd(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            InputStream is = p.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            p.waitFor();

            StringBuffer execResut = new StringBuffer();
            String str = reader.readLine();
            while (str != null) {
                execResut.append(str + "\n");
                str = reader.readLine();
            }

            if (p.exitValue() != 0) {
                logger.error(String.format("The system command [ %s ] has been executed failed.", cmd));
                logger.error(String.format("The output of system command [ %s ] is \n%s.", cmd, execResut.toString()));
            } else
                logger.info(String.format("The output of system command [ %s ] is \n%s.", cmd, execResut.toString()));

        } catch (IOException e) {
            logger.error(String.format("The system command [ %s ] has been executed failed.", cmd));
            logger.error(String.format("The output of system command [ %s ] is \n %s.", cmd, e.getMessage()));
        } catch (InterruptedException e) {
            logger.error(String.format("The system command[%s] has been executed failed.", cmd));
            logger.error(String.format("The output of system command [ %s ] is \n [ %s ].", cmd, e.getMessage()));
        }
    }

    public void execWaitOperation(SqlCommand command) {
        waitThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (command.isNeedWait()) {
                    try {
                        logger.info(String.format("Command[%s][row:%d] needs to wait connection[id:%d] %s",
                                command.getCommand(), command.getPosition(), command.getWaitConnId(),
                                command.getWaitOperation()));
                        Thread.sleep(COMMON.WAIT_TIMEOUT);
                        Connection conn = connectionManager.getConnection(command.getWaitConnId());
                        if (command.getWaitOperation().equalsIgnoreCase("commit")) {
                            if (!conn.getAutoCommit())
                                conn.commit();
                            else {
                                Statement statement = conn.createStatement();
                                statement.execute("commit");
                            }

                            logger.info(String.format(
                                    "Connection[id=%d] has committed automatically,for command[%s][row:%d]",
                                    command.getWaitConnId(), command.getCommand(), command.getPosition()));
                        }

                        if (command.getWaitOperation().equalsIgnoreCase("rollback")) {
                            if (!conn.getAutoCommit())
                                conn.rollback();
                            else {
                                Statement statement = conn.createStatement();
                                statement.execute("rollback");
                            }
                            logger.info(String.format(
                                    "Connection[id=%d] has rollback automatically,for command[%s][row:%d]",
                                    command.getWaitConnId(), command.getCommand(), command.getPosition()));
                        }

                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        waitThread.start();
    }

    /**
     * Writes regex patterns to the result file writer.
     * 
     * @param writer  The BufferedWriter to write to
     * @param command The SqlCommand containing regex patterns
     * @throws IOException if an I/O error occurs
     */
    private void writeRegexPatterns(BufferedWriter writer, SqlCommand command) throws IOException {
        if (command.getRegexPatterns() != null && !command.getRegexPatterns().isEmpty()) {
            for (RegexPattern regexPattern : command.getRegexPatterns()) {
                // Escape pattern for writing: escape backslashes and quotes
                String pattern = regexPattern.getPattern();
                writer.write(String.format("-- @regex(\"%s\", %s)", pattern, regexPattern.isInclude()));
                writer.newLine();
            }
        }
    }

    /**
     * 清理数据库：删除所有非内置数据库
     */
    public void cleanDatabases() {
        Connection connection = connectionManager.getConnection();
        if (connection == null) {
            logger.error("Failed to clean databases,please check the error,the program will exit.");
            System.exit(1);
        }
        Set<String> builtinDbSet = new HashSet<>(Arrays.asList(RunConfUtil.getBuiltinDb()));
        final String DROP_DB_TEMPLATE = "DROP DATABASE IF EXISTS `%s`";

        try (Statement showStmt = connection.createStatement();
                Statement dropStmt = connection.createStatement()) {
            List<String> databasesToDrop = new ArrayList<>();
            try (ResultSet rs = showStmt.executeQuery("show databases;")) {
                while (rs.next()) {
                    String db = rs.getString(1);
                    if (!builtinDbSet.contains(db)) {
                        databasesToDrop.add(db);
                    }
                }
            }
            databasesToDrop.forEach(db -> {
                try {
                    dropStmt.execute(String.format(DROP_DB_TEMPLATE, db));
                    logger.debug(String.format("The database [%s] has been cleaned.", db));
                } catch (SQLException e) {
                    logger.error("Failed to drop database [" + db + "]: " + e.getMessage());
                }
            });
        } catch (SQLException e) {
            logger.error("Unexpected exception has happened when cleaning up databases: " + e.getMessage());
            System.exit(1);
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.error("Failed to close connection: " + e.getMessage());
            }
        }
    }
}
