package io.mo.cases;

import io.mo.constant.RESULT;
import io.mo.result.RSSet;
import io.mo.result.StmtResult;
import io.mo.result.TestResult;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;

import java.lang.StringBuffer;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class SqlCommand {

    private static Logger LOG = Logger.getLogger(SqlCommand.class.getName());

    private StringBuffer command;
    
    @Getter
    @Setter
    private boolean ignore = false;
    
    @Getter
    @Setter
    private int conn_id = 0;

    @Getter
    @Setter
    private String useDB = null;

    @Setter
    @Getter
    private String conn_user = null;
    
    @Setter
    @Getter
    private String conn_pswd = null;
    
    @Getter
    @Setter
    private String issueNo = null;


    // deprecated, compatibility for old result file
    // column separator in result file for this command,can be 3 values:
    // * 1、table,separator is \t
    // * 2、space,separator is 4 spaces
    // * 3、both,separator is \t or 4 spaces
    // default value is both
    @Getter
    @Setter
    private String separator = "both";
    // deprecated, compatibility for old result file
    @Getter
    @Setter
    private boolean regularMatch = false;

    @Getter
    @Setter
    private String scriptFile;
    
    @Getter
    @Setter
    private TestScript testScript; // Reference to TestScript for accessing document-level flags
    
    @Getter
    @Setter
    private int position = 0;
    
    @Getter
    private ArrayList<Integer> sortKeyIndexs = new ArrayList<>();
    
    private ArrayList<String> syscmds = new ArrayList<>();
    
    @Getter
    private ArrayList<Integer> ignoreColumns = new ArrayList<>();
    
    @Getter
    private ArrayList<RegexPattern> regexPatterns = new ArrayList<>();

    @Getter
    private TestResult testResult;
    
    // 保留自定义 setter，因为有额外逻辑
    @Getter
    private StmtResult expResult;
    private StmtResult actResult;

    @Getter
    @Setter
    private SqlCommand next;
    
    @Getter
    @Setter
    private int sleeptime = 0;
    
    @Getter
    @Setter
    private boolean needWait = false;
    
    @Getter
    @Setter
    private int waitConnId = 0;
    
    @Getter
    @Setter
    private String waitOperation = "commit";
    
    @Getter
    @Setter
    private Boolean compareMeta = null; // SQL-level meta comparison flag (null means use document/global default)

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

    // 保留原方法名以保持兼容性（Lombok 会生成 getSyscmds()，但这里需要 getSysCMDS()）
    public ArrayList<String> getSysCMDS() {
        return this.syscmds;
    }

    public String getCommand() {
        if (command.length() == 0)
            return null;
        return command.toString();
    }

    public void addSortKeyIndex(int index) {
        sortKeyIndexs.add(index);
    }

    public void setExpResult(StmtResult expResult) {
        this.expResult = expResult;
        if (expResult != null) {
            this.testResult.setExpResult(expResult.toString());
        }
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

    public void sleep() {
        if (sleeptime == 0)
            return;

        try {
            Thread.sleep(sleeptime * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void addIgnoreColumn(int id) {
        this.ignoreColumns.add(id);
    }

    public void addRegexPattern(RegexPattern pattern) {
        this.regexPatterns.add(pattern);
    }
}
