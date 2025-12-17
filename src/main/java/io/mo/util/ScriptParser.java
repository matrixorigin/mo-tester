package io.mo.util;

import io.mo.cases.RegexPattern;
import io.mo.cases.SqlCommand;
import io.mo.cases.TestScript;
import io.mo.constant.COMMON;
import org.apache.log4j.Logger;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class ScriptParser {
    private TestScript testScript;
    private static final Logger LOG = Logger.getLogger(ScriptParser.class.getName());

    public ScriptParser() {
        this.testScript = new TestScript();
    }

    public TestScript parseScript(String path){
        testScript = new TestScript();
        testScript.setFileName(path);
        int rowNum = 1;
        try (BufferedReader lineReader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(Paths.get(path))))) {
            SqlCommand command = new SqlCommand();
            String line;
            ConnectionInfo conInfo = new ConnectionInfo();
            IssueInfo issueInfo = new IssueInfo();

            while ((line = lineReader.readLine()) != null) {
                line = new String(line.getBytes(), StandardCharsets.UTF_8);
                String trimmedLine = line.trim();

                if (trimmedLine.isEmpty() || lineIsComment(trimmedLine)) {
                    if (processCommentLine(trimmedLine, path, command, conInfo, issueInfo, rowNum)) {
                        return testScript; // File marked to skip
                    }
                    rowNum++;
                    continue;
                }
                
                String accumulatedCommand = command.getCommand();
                if (isDelimiterAtLineEnd(accumulatedCommand != null ? accumulatedCommand : "", trimmedLine)) {
                    command.append(trimmedLine);
                    finalizeCommand(command, conInfo, issueInfo, rowNum);
                    testScript.addCommand(command);
                    command = new SqlCommand();
                    rowNum++;
                    continue;
                }

                command.append(trimmedLine);
                command.append("\n");
                rowNum++;
            }
        } catch (IOException e) {
            LOG.error("Failed to parse script: " + path, e);
            throw new RuntimeException("Failed to parse script: " + path, e);
        }
        return testScript;
    }

    /**
     * Process comment lines and flags. Returns true if file should be skipped.
     */
    private boolean processCommentLine(String trimmedLine, String path, SqlCommand command, 
                                      ConnectionInfo conInfo, IssueInfo issueInfo, int rowNum) {
        // Skip file flag
        if (trimmedLine.startsWith(COMMON.BVT_SKIP_FILE_FLAG)) {
            String issueNo = trimmedLine.substring(COMMON.BVT_SKIP_FILE_FLAG.length());
            testScript.setSkiped(true);
            LOG.info(String.format("The script file [%s] is marked to be skiped for issue#%s, and it will not be executed.", path, issueNo));
            return true;
        }
        
        // Meta comparison flags: --- @metacmp(boolean) for document-level, -- @metacmp(boolean) for SQL-level
        if (trimmedLine.contains(COMMON.METACMP_FLAG)) {
            Boolean value = parseMetacmpFlag(trimmedLine);
            if (value != null) {
                if (trimmedLine.startsWith("---")) {
                    testScript.setCompareMeta(value);
                } else if (trimmedLine.startsWith("--")) {
                    command.setCompareMeta(value);
                }
            }
            return false;
        }
        
        // BVT issue flags
        if (trimmedLine.startsWith(COMMON.BVT_ISSUE_START_FLAG) && COMMON.IGNORE_MODEL) {
            issueInfo.start(trimmedLine.substring(COMMON.BVT_ISSUE_START_FLAG.length()));
        } else if (trimmedLine.equalsIgnoreCase(COMMON.BVT_ISSUE_END_FLAG)) {
            issueInfo.end();
        }
        
        // Simple flag handlers
        if (trimmedLine.startsWith(COMMON.FUNC_SLEEP_FLAG)) {
            command.setSleeptime(Integer.parseInt(trimmedLine.substring(COMMON.FUNC_SLEEP_FLAG.length())));
        } else if (trimmedLine.startsWith(COMMON.SYSTEM_CMD_FLAG)) {
            command.addSysCMD(trimmedLine.substring(COMMON.SYSTEM_CMD_FLAG.length()));
        } else if (trimmedLine.startsWith(COMMON.REGULAR_MATCH_FLAG)) {
            command.setRegularMatch(true);
        } else if (trimmedLine.startsWith(COMMON.IGNORE_COLUMN_FLAG)) {
            parseIgnoreColumns(trimmedLine.substring(COMMON.IGNORE_COLUMN_FLAG.length()), command);
        } else if (trimmedLine.startsWith(COMMON.WAIT_FLAG)) {
            parseWaitFlag(trimmedLine, command);
        } else if (trimmedLine.startsWith(COMMON.NEW_SESSION_START_FLAG)) {
            parseConnectionInfo(trimmedLine, conInfo, path, rowNum);
        } else if (trimmedLine.equalsIgnoreCase(COMMON.NEW_SESSION_END_FLAG) || 
                   trimmedLine.equalsIgnoreCase(COMMON.NEW_SESSION_END_FLAG + "}")) {
            conInfo.reset();
        } else if (trimmedLine.startsWith(COMMON.SORT_KEY_INDEX_FLAG)) {
            parseSortKeyIndexes(trimmedLine.substring(COMMON.SORT_KEY_INDEX_FLAG.length()), command);
        } else if (trimmedLine.startsWith(COMMON.COLUMN_SEPARATOR_FLAG)) {
            command.setSeparator(trimmedLine.substring(COMMON.COLUMN_SEPARATOR_FLAG.length()));
        } else if (trimmedLine.startsWith(COMMON.REGEX_FLAG)) {
            parseRegexFlag(trimmedLine.substring(COMMON.REGEX_FLAG.length()), command);
        }
        
        return false;
    }

    private void parseIgnoreColumns(String ignores, SqlCommand command) {
        if (ignores != null && !ignores.isEmpty()) {
            for (String id : ignores.split(",")) {
                command.addIgnoreColumn(Integer.parseInt(id.trim()));
            }
        }
    }

    private void parseWaitFlag(String trimmedLine, SqlCommand command) {
        String[] items = trimmedLine.split(":");
        if (items.length != 3) {
            return;
        }
        
        if (!StringUtils.isNumeric(items[1])) {
            LOG.warn(String.format("The connection id in flag[%s] is not a number, the flag is not valid.", trimmedLine));
            return;
        }
        
        String operation = items[2].toLowerCase();
        if (!operation.equals("commit") && !operation.equals("rollback")) {
            LOG.warn(String.format("The operation in flag[%s] is not [commit] or [rollback], the flag is not valid.", trimmedLine));
            return;
        }
        
        command.setWaitConnId(Integer.parseInt(items[1]));
        command.setWaitOperation(operation);
        command.setNeedWait(true);
    }

    private void parseConnectionInfo(String trimmedLine, ConnectionInfo conInfo, String path, int rowNum) {
        String conInfoStr = trimmedLine.endsWith("{") 
            ? trimmedLine.substring(COMMON.NEW_SESSION_START_FLAG.length(), trimmedLine.length() - 1)
            : trimmedLine.substring(COMMON.NEW_SESSION_START_FLAG.length());
        
        if (conInfoStr == null || conInfoStr.isEmpty()) {
            LOG.warn(String.format("[%s][row:%d]The new connection flag doesn't designate the connection id by [id=X],and the id will be set to default value 1", path, rowNum));
            conInfo.id = COMMON.NEW_SEESION_DEFAULT_ID;
            return;
        }
        
        String[] paras = conInfoStr.split("&");
        for (String para : paras) {
            if (para.startsWith("id=")) {
                conInfo.id = parseConnectionId(para.substring(3), path, rowNum);
            } else if (para.startsWith("user=")) {
                conInfo.user = parseConnectionUser(para.substring(5), path, rowNum);
            } else if (para.startsWith("password=")) {
                conInfo.password = parseConnectionPassword(para.substring(9), path, rowNum);
            }
        }
    }

    private int parseConnectionId(String id, String path, int rowNum) {
        if (id.isEmpty()) {
            LOG.warn(String.format("[%s][row:%d]The new connection flag doesn't designate the connection id by [id=X],and the id will be set to default value 1", path, rowNum));
            return COMMON.NEW_SEESION_DEFAULT_ID;
        }
        if (id.matches("[0-9]+")) {
            return Integer.parseInt(id);
        }
        LOG.warn(String.format("[%s][row:%d]The new connection flag designate a invalid connection id by [id=X],and the id will be set to default value 1", path, rowNum));
        return COMMON.NEW_SEESION_DEFAULT_ID;
    }

    private String parseConnectionUser(String user, String path, int rowNum) {
        if (user.isEmpty()) {
            LOG.warn(String.format("[%s][row:%d]The new connection flag doesn't designate the connection user by [user=X],and the id will be set to value from mo.yml", path, rowNum));
            return MoConfUtil.getUserName();
        }
        return user;
    }

    private String parseConnectionPassword(String pwd, String path, int rowNum) {
        if (pwd.isEmpty()) {
            LOG.warn(String.format("[%s][row:%d]The new connection flag doesn't designate the connection password by [password=X],and the id will be set to value from mo.yml", path, rowNum));
            return MoConfUtil.getUserpwd();
        }
        return pwd;
    }

    private void parseSortKeyIndexes(String indexes, SqlCommand command) {
        for (String index : indexes.split(",")) {
            command.addSortKeyIndex(Integer.parseInt(index.trim()));
        }
    }

    /**
     * Parse @metacmp(boolean) flag from a comment line.
     * @param trimmedLine The trimmed comment line
     * @return Boolean value if successfully parsed, null otherwise
     */
    private Boolean parseMetacmpFlag(String trimmedLine) {
        int openParen = trimmedLine.indexOf('(');
        int closeParen = trimmedLine.indexOf(')', openParen);
        if (openParen == -1 || closeParen == -1) {
            return null;
        }
        
        String value = trimmedLine.substring(openParen + 1, closeParen).trim();
        if ("true".equalsIgnoreCase(value)) {
            return true;
        } else if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        return null;
    }

    private void parseRegexFlag(String regexStr, SqlCommand command) {
        // Parse format: -- @regex("pattern", true/false)
        // Remove leading/trailing whitespace
        regexStr = regexStr.trim();
        
        // Check if it starts with '(' and ends with ')'
        if (!regexStr.startsWith("(") || !regexStr.endsWith(")")) {
            throw new RuntimeException(String.format("Invalid regex flag format: -- @regex%s. Expected format: -- @regex(\"pattern\", true/false)", regexStr));
        }
        
        // Remove parentheses
        String content = regexStr.substring(1, regexStr.length() - 1);
        
        // Find the last comma that separates pattern and include flag
        // We need to find the last comma because pattern itself may contain commas
        int lastCommaIndex = content.lastIndexOf(',');
        if (lastCommaIndex == -1) {
            throw new RuntimeException(String.format("Invalid regex flag format: -- @regex%s. Missing comma separator.", regexStr));
        }
        
        // Extract pattern (everything before last comma) and include flag (after last comma)
        String patternStr = content.substring(0, lastCommaIndex).trim();
        String includeStr = content.substring(lastCommaIndex + 1).trim();
        
        // Parse pattern - must be a quoted string literal
        String pattern = null;
        if (patternStr.startsWith("\"") && patternStr.endsWith("\"")) {
            // Double-quoted string
            pattern = patternStr.substring(1, patternStr.length() - 1);
        } else if (patternStr.startsWith("'") && patternStr.endsWith("'")) {
            // Single-quoted string
            pattern = patternStr.substring(1, patternStr.length() - 1);
        } else {
            throw new RuntimeException(String.format("Invalid regex flag format: pattern must be a quoted string literal. Got: %s", patternStr));
        }
        
        // Parse boolean value
        boolean include;
        if ("true".equalsIgnoreCase(includeStr)) {
            include = true;
        } else if ("false".equalsIgnoreCase(includeStr)) {
            include = false;
        } else {
            throw new RuntimeException(String.format("Invalid regex flag include value: %s. Expected 'true' or 'false'.", includeStr));
        }
        
        // Compile pattern immediately - fail fast if invalid
        Pattern compiledPattern;
        try {
            compiledPattern = Pattern.compile(pattern);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Invalid regex pattern '%s': %s", pattern, e.getMessage()), e);
        }
        
        // Create and add RegexPattern with compiled pattern
        RegexPattern regexPattern = new RegexPattern(pattern, include, compiledPattern);
        command.addRegexPattern(regexPattern);
    }

    private void finalizeCommand(SqlCommand command, ConnectionInfo conInfo, IssueInfo issueInfo, int rowNum) {
        command.setConn_id(conInfo.id);
        command.setConn_user(conInfo.user);
        command.setConn_pswd(conInfo.password);
        command.setIgnore(issueInfo.ignore);
        command.setIssueNo(issueInfo.issueNo);
        command.setPosition(rowNum);
    }

    /**
     * Helper class to track connection information
     */
    private static class ConnectionInfo {
        int id = 0;
        String user = null;
        String password = null;
        
        void reset() {
            id = 0;
            user = null;
            password = null;
        }
    }

    /**
     * Helper class to track issue information
     */
    private static class IssueInfo {
        String issueNo = null;
        boolean ignore = false;
        
        void start(String issueNo) {
            this.issueNo = issueNo;
            this.ignore = true;
        }
        
        void end() {
            this.issueNo = null;
            this.ignore = false;
        }
    }

    private boolean lineIsComment(String trimmedLine) {
        return trimmedLine.startsWith("//") || trimmedLine.startsWith("--") || trimmedLine.startsWith("#");
    }

    /**
     * Check if delimiter is at the end of line and not inside a string literal.
     * This method handles complex cases including nested quotes, overlapping quotes,
     * and multi-line strings.
     * 
     * <p><b>Algorithm:</b>
     * <ol>
     *   <li>Find the last delimiter position, ignoring inline comments</li>
     *   <li>Track string state from accumulated command through current line to delimiter position</li>
     *   <li>If delimiter is outside any string literal, return true</li>
     * </ol>
     * 
     * <p><b>Examples:</b>
     * <ul>
     *   <li>{@code "SELECT * FROM t1;"} → returns {@code true}</li>
     *   <li>{@code "INSERT INTO t1 VALUES ('hello;world');"} → returns {@code true}</li>
     *   <li>{@code "INSERT INTO t1 VALUES (6, '`~\"\''\\');"} → returns {@code true} (handles overlapping quotes)</li>
     *   <li>{@code accumulated: "INSERT INTO t1 VALUES ('\n", current: "');"} → returns {@code true} (multi-line string closed)</li>
     *   <li>{@code "SELECT 'test;'"} → returns {@code false} (delimiter inside string)</li>
     *   <li>{@code "SELECT \"test;\""} → returns {@code false} (delimiter inside string)</li>
     * </ul>
     * 
     * @param accumulatedCommand the accumulated command from previous lines (for multi-line string tracking)
     * @param currentLine the current line to check
     * @return true if delimiter is at end of line and not in a string, false otherwise
     */
    private boolean isDelimiterAtLineEnd(String accumulatedCommand, String currentLine) {
        String trimmed = currentLine.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        
        String delimiter = ";";
        StringState state = trackStringState(accumulatedCommand);
        int commentStart = findCommentStart(trimmed, state);
        int delimiterPos = findDelimiterPosition(trimmed, delimiter, commentStart);
        
        if (delimiterPos == -1) {
            return false;
        }
        
        // Track string state up to delimiter position
        state = trackStringState(accumulatedCommand);
        for (int i = 0; i < delimiterPos; i++) {
            i = processChar(trimmed, i, state);
        }
        
        return !state.inSingleQuote && !state.inDoubleQuote;
    }

    private int findCommentStart(String trimmed, StringState state) {
        for (int i = 0; i < trimmed.length(); i++) {
            i = processChar(trimmed, i, state);
            if (i < trimmed.length() - 1 && trimmed.charAt(i) == '-' && trimmed.charAt(i + 1) == '-' 
                && !state.inSingleQuote && !state.inDoubleQuote) {
                return i;
            }
        }
        return -1;
    }

    private int findDelimiterPosition(String trimmed, String delimiter, int searchEnd) {
        int end = searchEnd == -1 ? trimmed.length() : searchEnd;
        for (int i = end - delimiter.length(); i >= 0; i--) {
            if (i + delimiter.length() <= trimmed.length() && 
                trimmed.substring(i, i + delimiter.length()).equals(delimiter)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Process a character and update string state accordingly.
     * Handles:
     * - Backslash escapes: \', \", \\
     * - SQL-style escaped single quotes: ''
     * - Special case: \'' in single-quoted strings (needs context-aware handling)
     * - Quote toggling: ', "
     * 
     * @param text the text being processed
     * @param pos the current position
     * @param state the current string state
     * @return the new position (may be advanced if escape sequences are processed)
     */
    private int processChar(String text, int pos, StringState state) {
        if (pos >= text.length()) {
            return pos;
        }
        
        char c = text.charAt(pos);
        
        // Handle backslash escape - only valid inside the corresponding quote type
        if (c == '\\' && pos + 1 < text.length() && (state.inSingleQuote || state.inDoubleQuote)) {
            return handleBackslashEscape(text, pos, state);
        }
        
        // Handle SQL-style escaped single quote ('') - only valid inside single quote string
        if (c == '\'' && state.inSingleQuote && pos + 1 < text.length() && text.charAt(pos + 1) == '\'') {
            return pos + 1; // Skip the second quote
        }
        
        // Handle single quote - only toggle if not in double quote string
        if (c == '\'' && !state.inDoubleQuote) {
            state.inSingleQuote = !state.inSingleQuote;
            return pos;
        }
        
        // Handle double quote - only toggle if not in single quote string
        if (c == '"' && !state.inSingleQuote) {
            state.inDoubleQuote = !state.inDoubleQuote;
            return pos;
        }
        
        return pos;
    }

    private int handleBackslashEscape(String text, int pos, StringState state) {
        // Special case: \'' in single-quoted string
        if (state.inSingleQuote && pos + 2 < text.length() && 
            text.charAt(pos + 1) == '\'' && text.charAt(pos + 2) == '\'') {
            // Check if \'' is followed by more string content (not SQL syntax terminators)
            if (pos + 3 < text.length()) {
                char afterSecondQuote = text.charAt(pos + 3);
                if (afterSecondQuote == ')' || afterSecondQuote == ';') {
                    return pos + 1; // Normal escape, string ends
                } else if (afterSecondQuote == ',') {
                    // Check if comma is followed by more content on same line
                    int j = pos + 4;
                    while (j < text.length() && Character.isWhitespace(text.charAt(j)) && 
                           text.charAt(j) != '\n' && text.charAt(j) != '\r') {
                        j++;
                    }
                    if (j < text.length() && text.charAt(j) != '\n' && text.charAt(j) != '\r') {
                        return pos + 2; // More content, treat as \' + ''
                    }
                } else {
                    return pos + 2; // More content, treat as \' + ''
                }
            }
        }
        // Normal backslash escape: skip the escaped character
        return pos + 1;
    }
    
    /**
     * Track string literal state through a text segment.
     * Returns the final state (whether we're inside single or double quoted string).
     * 
     * @param text the text to track through
     * @return StringState object containing the final state
     */
    private StringState trackStringState(String text) {
        StringState state = new StringState();
        if (text == null || text.isEmpty()) {
            return state;
        }
        
        for (int i = 0; i < text.length(); i++) {
            i = processChar(text, i, state);
        }
        
        return state;
    }
    
    /**
     * Helper class to track string literal state.
     */
    private static class StringState {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
    }

    public TestScript getTestScript(){
        return testScript;
    }

    public static void main(String[] args){
       
    }
}

