package io.mo.cases;

import io.mo.constant.RESULT;
import io.mo.result.RSSet;
import io.mo.result.StmtResult;
import io.mo.result.TestResult;
import io.mo.util.MoConfUtil;
import org.apache.log4j.Logger;

import java.lang.StringBuffer;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class SqlCommand {

    private static Logger LOG = Logger.getLogger(SqlCommand.class.getName());

    private StringBuffer command;
    private boolean ignore = false;
    private int conn_id = 0;

    private String useDB = null;

    private String conn_user = null;
    private String conn_pswd = null;
    private String issueNo = null;


    // deprecated, compatibility for old result file
    // column separator in result file for this command,can be 3 values:
    // * 1、table,separator is \t
    // * 2、space,separator is 4 spaces
    // * 3、both,separator is \t or 4 spaces
    // default value is both
    private String separator = "both";
    // deprecated, compatibility for old result file
    private boolean regularMatch = false;

    private String scriptFile;
    private int position = 0;
    private ArrayList<Integer> sortKeyIndexs = new ArrayList<>();
    private ArrayList<String> syscmds = new ArrayList<>();
    private ArrayList<Integer> ignoreColumns = new ArrayList<>();
    private ArrayList<RegexPattern> regexPatterns = new ArrayList<>();

    private TestResult testResult;
    private StmtResult expResult;
    private StmtResult actResult;

    private SqlCommand next;
    private int sleeptime = 0;
    private boolean needWait = false;
    private int waitConnId = 0;
    private String waitOperation = "commit";

    public SqlCommand() {
        command = new StringBuffer();
        testResult = new TestResult();
    }

    public void append(String command) {
        this.command.append(command);
    }

    public void addSysCMD(String cmd) {
        this.syscmds.add(cmd);
    }

    public ArrayList<String> getSysCMDS() {
        return this.syscmds;
    }

    public String getCommand() {
        if (command.length() == 0)
            return null;
        return command.toString();
    }

    public int getConn_id() {
        return conn_id;
    }

    public void setConn_id(int conn_id) {
        this.conn_id = conn_id;
    }

    public String getConn_user() {
        if (conn_user == null)
            return MoConfUtil.getUserName();
        return conn_user;
    }

    public String getConn_pswd() {
        if (conn_pswd == null)
            return MoConfUtil.getUserpwd();
        return conn_pswd;
    }

    public String getScriptFile() {
        return scriptFile;
    }

    public void setScriptFile(String scriptFile) {
        this.scriptFile = scriptFile;
    }

    public TestResult getTestResult() {
        return testResult;
    }

    public boolean isIgnore() {
        return ignore;
    }

    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public SqlCommand getNext() {
        return next;
    }

    public void setNext(SqlCommand next) {
        this.next = next;
    }

    public ArrayList<Integer> getSortKeyIndexs() {
        return sortKeyIndexs;
    }

    public void addSortKeyIndex(int index) {
        sortKeyIndexs.add(index);
    }

    public StmtResult getExpResult() {
        return expResult;
    }

    public void setExpResult(StmtResult expResult) {
        this.expResult = expResult;
        if (expResult != null) {
            this.testResult.setExpResult(expResult.toString());
        }
    }

    public StmtResult getActResult() {
        return actResult;
    }

    public void setActResult(StmtResult actResult) {
        this.actResult = actResult;
        if (actResult != null) {
            this.testResult.setActResult(actResult.toString());
        }
    }

    public boolean checkResult() {
        // Check if regex patterns are set
        if (this.regexPatterns != null && !this.regexPatterns.isEmpty()) {
            return checkRegexResult();
        }
        
        // compatibility for old result file
        if (this.regularMatch)
            return expResult.regularMatch(actResult);

        return expResult.equals(actResult);
    }

    private boolean checkRegexResult() {
        if (actResult == null) {
            LOG.error("Actual result is null, cannot perform regex check");
            return false;
        }

        int actualType = actResult.getType();
        String textToCheck = null;

        // Determine which text to check based on actualResult type
        if (actualType == RESULT.STMT_RESULT_TYPE_ERROR) {
            // If type is ERROR, check errorMessage
            textToCheck = actResult.getErrorMessage();
            if (textToCheck == null || textToCheck.isEmpty()) {
                LOG.error("Actual result type is ERROR but errorMessage is null or empty");
                return false;
            }
        } else if (actualType == RESULT.STMT_RESULT_TYPE_SET) {
            // If type is SET, check rsSet.toString()
            RSSet rsSet = actResult.getRsSet();
            if (rsSet == null) {
                LOG.error("Actual result type is SET but rsSet is null");
                return false;
            }
            textToCheck = rsSet.toString();
            if (textToCheck == null || textToCheck.isEmpty()) {
                LOG.error("Actual result type is SET but rsSet.toString() is null or empty");
                return false;
            }
        } else {
            // Other types (NONE, ABNORMAL, etc.) are not supported for regex matching
            LOG.error(String.format("Actual result type %d is not supported for regex matching. Only ERROR and SET types are supported.", actualType));
            return false;
        }

        // Check each regex pattern in order
        for (RegexPattern regexPattern : regexPatterns) {
            Pattern compiledPattern = regexPattern.getCompiledPattern();
            String pattern = regexPattern.getPattern();
            boolean include = regexPattern.isInclude();

            // Check pattern in the appropriate text
            boolean patternMatches = compiledPattern.matcher(textToCheck).find();

            if (include) {
                // include=true: pattern must be found
                if (!patternMatches) {
                    LOG.error(String.format("Regex pattern '%s' not found (include=true). Type: %s, Text: %s", 
                            pattern, 
                            actualType == RESULT.STMT_RESULT_TYPE_ERROR ? "ERROR" : "SET",
                            textToCheck.length() > 100 ? textToCheck.substring(0, 100) + "..." : textToCheck));
                    return false;
                }
            } else {
                // include=false: pattern must NOT be found
                if (patternMatches) {
                    LOG.error(String.format("Regex pattern '%s' found but should not be present (include=false). Type: %s, Text: %s", 
                            pattern,
                            actualType == RESULT.STMT_RESULT_TYPE_ERROR ? "ERROR" : "SET",
                            textToCheck.length() > 100 ? textToCheck.substring(0, 100) + "..." : textToCheck));
                    return false;
                }
            }
        }

        // All patterns passed
        return true;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public String getIssueNo() {
        return issueNo;
    }

    public void setIssueNo(String issueNo) {
        this.issueNo = issueNo;
    }

    public void setConn_user(String conn_user) {
        this.conn_user = conn_user;
    }

    public void setConn_pswd(String conn_pswd) {
        this.conn_pswd = conn_pswd;
    }

    public void sleep() {
        if (sleeptime == 0)
            return;

        try {
            Thread.sleep(sleeptime * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public int getSleeptime() {
        return sleeptime;
    }

    public void setSleeptime(int sleeptime) {
        this.sleeptime = sleeptime;
    }

    public String getUseDB() {
        return useDB;
    }

    public void setUseDB(String useDB) {
        this.useDB = useDB;
    }

    public boolean isRegularMatch() {
        return regularMatch;
    }

    public void setRegularMatch(boolean regularMatch) {
        this.regularMatch = regularMatch;
    }

    public boolean isNeedWait() {
        return needWait;
    }

    public void setNeedWait(boolean needWait) {
        this.needWait = needWait;
    }

    public int getWaitConnId() {
        return waitConnId;
    }

    public void setWaitConnId(int waitConnId) {
        this.waitConnId = waitConnId;
    }

    public String getWaitOperation() {
        return waitOperation;
    }

    public void setWaitOperation(String waitOperation) {
        this.waitOperation = waitOperation;
    }

    public ArrayList<Integer> getIgnoreColumns() {
        return this.ignoreColumns;
    }

    public void addIgnoreColumn(int id) {
        this.ignoreColumns.add(id);
    }

    public ArrayList<RegexPattern> getRegexPatterns() {
        return regexPatterns;
    }

    public void addRegexPattern(RegexPattern pattern) {
        this.regexPatterns.add(pattern);
    }
}
