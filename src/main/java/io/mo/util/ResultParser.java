package io.mo.util;

import io.mo.cases.RegexPattern;
import io.mo.cases.SqlCommand;
import io.mo.cases.TestScript;
import io.mo.constant.COMMON;
import io.mo.constant.DATATYPE;
import io.mo.constant.RESULT;
import io.mo.result.*;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parses result files and loads expected results into TestScript objects.
 * This class reads result files corresponding to test scripts and updates
 * each SqlCommand in the script with its expected result.
 * 
 * <p><b>Thread Safety:</b>
 * This class is thread-safe for concurrent use when each thread operates on
 * a different TestScript instance. All methods are static and stateless,
 * but they modify the TestScript and SqlCommand objects passed as parameters.
 * Multiple threads can safely call methods of this class simultaneously
 * as long as they do not share TestScript instances.
 * 
 * <p><b>Example:</b>
 * <pre>{@code
 * // Thread-safe: each thread has its own TestScript
 * TestScript script1 = parser.parseScript("file1.sql");
 * TestScript script2 = parser.parseScript("file2.sql");
 * 
 * // Thread 1
 * ResultParser.loadExpectedResultsFromFile(script1);
 * 
 * // Thread 2 (safe to run concurrently)
 * ResultParser.loadExpectedResultsFromFile(script2);
 * }</pre>
 */
public class ResultParser {
    private static final Logger LOG = Logger.getLogger(ResultParser.class.getName());

    /**
     * Loads expected results from the result file and updates the TestScript object.
     * This method finds the corresponding result file, reads it, parses the content,
     * and updates each SqlCommand in the script with its expected result.
     *
     * @param script The TestScript to load expected results for
     * @return ParseResult indicating success or failure, with error message and file path
     */
    public static ParseResult loadExpectedResultsFromFile(TestScript script) {
        String filePath = findResultFile(script);
        if (filePath == null) {
            String errorMsg = "Result file not found";
            markFailed(script, errorMsg);
            return ParseResult.failure(errorMsg);
        }
        
        String resultText;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            resultText = reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            LOG.error("Failed to read result file: " + filePath, e);
            String errorMsg = "Failed to read result file: " + e.getMessage();
            markFailed(script, errorMsg);
            return ParseResult.failure(errorMsg, filePath);
        }
        
        String remainingText = resultText;
        for (SqlCommand cmd : script.getCommands()) {
            ParseResult parseResult = parseCommand(cmd, remainingText, script);
            if (parseResult.isFailure()) {
                return parseResult;
            }
            remainingText = advanceToNextCommand(remainingText, cmd);
        }
        
        return ParseResult.success(filePath);
    }

    /**
     * Finds the result file corresponding to the test script.
     * Tries two strategies:
     * 1. Same directory with result file suffix
     * 2. Result directory with same relative path
     * 
     * @param script The test script to find result file for
     * @return The path of the result file, or null if not found
     */
    private static String findResultFile(TestScript script) {
        // Strategy 1: Look in the same directory as the script file
        String path1 = script.getFileName().replaceAll("\\.[A-Za-z]+", COMMON.R_FILE_SUFFIX);
        if (new File(path1).exists()) return path1;
        
        // Strategy 2: Look in the result directory with same relative path
        String path2 = script.getFileName().replaceFirst(COMMON.CASES_DIR, COMMON.RESULT_DIR)
                .replaceAll("\\.[A-Za-z]+", COMMON.R_FILE_SUFFIX);
        return new File(path2).exists() ? path2 : null;
    }

    /**
     * Parses a single command's expected result from the result file text.
     * Extracts the result text between the current command and the next command,
     * then creates and attaches a StmtResult to the command.
     * 
     * @param cmd The SQL command to parse result for
     * @param text The remaining text from the result file
     * @param script The test script containing the command
     * @return ParseResult indicating success or failure
     */
    private static ParseResult parseCommand(SqlCommand cmd, String text, TestScript script) {
        String cmdText = cmd.getCommand();
        // Verify the command exists in the result file
        if (!text.startsWith(cmdText)) {
            String errorMsg = String.format("Command does not exist in result file: [%s][%d]:%s",
                    script.getFileName(), cmd.getPosition(), cmd.getCommand().trim());
            logError(script, cmd, "does not exist in result file");
            return ParseResult.failure(errorMsg, findResultFile(script));
        }
        
        // Verify the next command exists (if any) to determine result boundaries
        String nextCmd = cmd.getNext() != null ? cmd.getNext().getCommand() : null;
        if (nextCmd != null && !text.contains(nextCmd)) {
            String errorMsg = String.format("Next command does not exist in result file: [%s][%d]:%s",
                    script.getFileName(), cmd.getNext().getPosition(), cmd.getNext().getCommand().trim());
            logError(script, cmd.getNext(), "does not exist in result file");
            return ParseResult.failure(errorMsg, findResultFile(script));
        }
        
        // Extract result text between current command and next command
        String result = extractResult(cmdText, nextCmd, text);
        
        // Parse regex patterns from result text if any
        String remainingResult = parseRegexPatternsFromResult(result, cmd);
        
        StmtResult expResult = new StmtResult();
        expResult.setCommand(cmd);
        if (remainingResult == null || remainingResult.isEmpty()) {
            expResult.setType(RESULT.STMT_RESULT_TYPE_NONE);
        } else {
            expResult.setExpectRSText(remainingResult);
            // Convert result text to RSSet for complete expResult processing
            RSSet rsSet = convertToRSSet(remainingResult, cmd.getSeparator());
            if (rsSet != null) {
                expResult.setType(RESULT.STMT_RESULT_TYPE_SET);
                expResult.setRsSet(rsSet);
            } else {
                // If conversion fails, treat as empty result
                expResult.setType(RESULT.STMT_RESULT_TYPE_NONE);
            }
        }
        // Attach expected result to the command
        cmd.setExpResult(expResult);
        return ParseResult.success(findResultFile(script));
    }

    /**
     * Extracts the result text between a command and its next command.
     * The result is the text between the end of current command and the start of next command.
     * 
     * @param cmd The current command text
     * @param nextCmd The next command text (null if this is the last command)
     * @param text The full result file text
     * @return The extracted result text, or null if not found
     */
    private static String extractResult(String cmd, String nextCmd, String text) {
        int cmdEnd = cmd.length();
        // If no next command, return everything after current command
        if (nextCmd == null) {
            return cmdEnd >= text.length() ? null : text.substring(cmdEnd + 1);
        }
        // Find the position of next command and extract text between them
        int nextPos = text.indexOf(nextCmd, cmdEnd);
        if (nextPos < 0) return null;
        int resultStart = cmdEnd + 1;
        return resultStart >= nextPos ? null : text.substring(resultStart, nextPos - 1);
    }

    /**
     * Advances the text pointer to the start of the next command.
     * This is used to efficiently process commands sequentially without re-scanning.
     * 
     * @param text The remaining result file text
     * @param cmd The current command
     * @return The remaining text starting from the next command, or empty string if not found
     */
    private static String advanceToNextCommand(String text, SqlCommand cmd) {
        String nextCmd = cmd.getNext() != null ? cmd.getNext().getCommand() : null;
        if (nextCmd == null) return "";
        // Find next command position and return text from that point
        int pos = text.indexOf(nextCmd, cmd.getCommand().length());
        return pos >= 0 ? text.substring(pos) : "";
    }

    /**
     * Converts result text string to RSSet object.
     * Parses tabular data with headers and rows, normalizes separators,
     * and creates a structured result set representation.
     * 
     * @param rsText The result text to convert (header row + data rows)
     * @param separator The separator type: "table", "space", or "both"
     * @return RSSet object, or null if input is empty
     */
    private static RSSet convertToRSSet(String rsText, String separator) {
        if (rsText == null || rsText.isEmpty()) return null;

        String rowSeparator = rsText.contains(RESULT.ROW_SEPARATOR_NEW) ? RESULT.ROW_SEPARATOR_NEW : "\n";

        String normalized = rsText;
        if (!rsText.contains(RESULT.ROW_SEPARATOR_NEW)) {
            // compatibility for old result file
            normalized = normalizeSeparators(rsText, separator);
        }

        String[] lines = normalized.split(rowSeparator, -1);
        if (lines.length == 0) return null;
        
        // First line contains column headers
        String[] labels = lines[0].split(RESULT.COLUMN_SEPARATOR_NEW, -1);
        RSSet rsSet = new RSSet();
        RSMetaData meta = new RSMetaData(labels.length);
        rsSet.setMeta(meta);
        for (String label : labels) {
            meta.addMetaInfo(label, label, DATATYPE.TYPE_STRING, 0);
        }
        
        // Remaining lines contain data rows
        for (int i = 1; i < lines.length; i++) {
            RSRow row = new RSRow(labels.length);
            rsSet.addRow(row);
            String[] values = lines[i].split(RESULT.COLUMN_SEPARATOR_NEW, -1);
            for (int j = 0; j < labels.length; j++) {
                RSCell cell = new RSCell();
                cell.setType(DATATYPE.TYPE_STRING);
                cell.setValue(j < values.length ? values[j] : "");
                row.addCell(cell);
            }
        }
        return rsSet;
    }

    /**
     * Normalizes column separators in result text to a unified separator.
     * Supports tab, space (4 spaces), or both separator types.
     * 
     * @param text The text to normalize
     * @param separator The separator type: "table" (tab), "space" (4 spaces), or "both"
     * @return Text with normalized separators
     */
    private static String normalizeSeparators(String text, String separator) {
        if ("both".equals(separator)) {
            // Replace both tab and space separators
            return text.replaceAll(RESULT.COLUMN_SEPARATOR_SPACE, RESULT.COLUMN_SEPARATOR_NEW)
                    .replaceAll(RESULT.COLUMN_SEPARATOR_TABLE, RESULT.COLUMN_SEPARATOR_NEW);
        } else if ("table".equals(separator)) {
            // Replace only tab separators
            return text.replaceAll(RESULT.COLUMN_SEPARATOR_TABLE, RESULT.COLUMN_SEPARATOR_NEW);
        } else if ("space".equals(separator)) {
            // Replace only space separators (4 spaces)
            return text.replaceAll(RESULT.COLUMN_SEPARATOR_SPACE, RESULT.COLUMN_SEPARATOR_NEW);
        }
        return text;
    }

    /**
     * Marks the test script as invalid and logs a warning.
     * Used when result file parsing fails at the script level.
     */
    private static void markFailed(TestScript script, String reason) {
        LOG.warn("The result of the test script file[" + script.getFileName() + "] " + reason + 
                ", please check and this test script file will be skipped.");
        script.invalid();
    }

    /**
     * Logs an error for a specific command and marks the script as invalid.
     * Used when a command cannot be found or parsed in the result file.
     */
    private static void logError(TestScript script, SqlCommand cmd, String msg) {
        LOG.error(String.format("[Exceptional command][%s][%d]:%s, %s",
                script.getFileName(), cmd.getPosition(), cmd.getCommand().trim(), msg));
        script.invalid();
    }

    /**
     * Parses regex patterns from result text and removes them from the result.
     * Regex patterns are expected to be on separate lines starting with "-- @regex("
     * and should appear before the actual result content.
     * 
     * @param result The result text that may contain regex patterns
     * @param cmd The SqlCommand to add parsed regex patterns to
     * @return The result text with regex patterns removed
     */
    private static String parseRegexPatternsFromResult(String result, SqlCommand cmd) {
        if (result == null || result.isEmpty()) {
            return result;
        }

        String[] lines = result.split("\n", -1);
        StringBuilder remainingResult = new StringBuilder();
        boolean foundNonRegexLine = false;

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.startsWith(COMMON.REGEX_FLAG)) {
                // Parse regex pattern
                String regexStr = trimmedLine.substring(COMMON.REGEX_FLAG.length()).trim();
                
                // Check if it starts with '(' and ends with ')'
                if (!regexStr.startsWith("(") || !regexStr.endsWith(")")) {
                    LOG.warn(String.format("Invalid regex format in result file: %s", trimmedLine));
                    // Treat as regular result line
                    if (foundNonRegexLine || remainingResult.length() > 0) {
                        remainingResult.append("\n");
                    }
                    remainingResult.append(line);
                    foundNonRegexLine = true;
                    continue;
                }
                
                // Remove parentheses
                String content = regexStr.substring(1, regexStr.length() - 1);
                
                // Find the last comma that separates pattern and include flag
                int lastCommaIndex = content.lastIndexOf(',');
                if (lastCommaIndex == -1) {
                    LOG.warn(String.format("Invalid regex format in result file: %s", trimmedLine));
                    // Treat as regular result line
                    if (foundNonRegexLine || remainingResult.length() > 0) {
                        remainingResult.append("\n");
                    }
                    remainingResult.append(line);
                    foundNonRegexLine = true;
                    continue;
                }
                
                // Extract pattern and include flag
                String patternStr = content.substring(0, lastCommaIndex).trim();
                String includeStr = content.substring(lastCommaIndex + 1).trim();
                
                // Parse pattern - must be a quoted string literal
                String pattern = null;
                if (patternStr.startsWith("\"") && patternStr.endsWith("\"")) {
                    // Double-quoted string
                    pattern = patternStr.substring(1, patternStr.length() - 1);
                    // Unescape escaped characters
                    pattern = pattern.replace("\\\"", "\"").replace("\\\\", "\\");
                } else if (patternStr.startsWith("'") && patternStr.endsWith("'")) {
                    // Single-quoted string
                    pattern = patternStr.substring(1, patternStr.length() - 1);
                    // Unescape escaped characters
                    pattern = pattern.replace("\\'", "'").replace("\\\\", "\\");
                } else {
                    LOG.warn(String.format("Invalid regex format in result file: pattern must be a quoted string literal. Got: %s", patternStr));
                    // Treat as regular result line
                    if (foundNonRegexLine || remainingResult.length() > 0) {
                        remainingResult.append("\n");
                    }
                    remainingResult.append(line);
                    foundNonRegexLine = true;
                    continue;
                }
                
                // Parse boolean value
                boolean include;
                if ("true".equalsIgnoreCase(includeStr)) {
                    include = true;
                } else if ("false".equalsIgnoreCase(includeStr)) {
                    include = false;
                } else {
                    LOG.warn(String.format("Invalid regex include value in result file: %s", includeStr));
                    // Treat as regular result line
                    if (foundNonRegexLine || remainingResult.length() > 0) {
                        remainingResult.append("\n");
                    }
                    remainingResult.append(line);
                    foundNonRegexLine = true;
                    continue;
                }
                
                // Compile pattern immediately - fail fast if invalid
                Pattern compiledPattern;
                try {
                    compiledPattern = Pattern.compile(pattern);
                } catch (Exception e) {
                    LOG.error(String.format("Failed to compile regex pattern '%s' from result file: %s", pattern, e.getMessage()));
                    // Treat as regular result line
                    if (foundNonRegexLine || remainingResult.length() > 0) {
                        remainingResult.append("\n");
                    }
                    remainingResult.append(line);
                    foundNonRegexLine = true;
                    continue;
                }
                
                // Create and add RegexPattern with compiled pattern
                RegexPattern regexPattern = new RegexPattern(pattern, include, compiledPattern);
                cmd.addRegexPattern(regexPattern);
            } else {
                // This is a regular result line
                if (foundNonRegexLine || remainingResult.length() > 0) {
                    remainingResult.append("\n");
                }
                remainingResult.append(line);
                foundNonRegexLine = true;
            }
        }

        return remainingResult.toString();
    }
}
