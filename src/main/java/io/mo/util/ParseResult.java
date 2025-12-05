package io.mo.util;

/**
 * Represents the result of parsing a result file.
 * Contains information about whether the parsing was successful,
 * any error messages, and the file path that was parsed.
 */
public class ParseResult {
    private final boolean success;
    private final String errorMessage;
    private final String filePath;

    private ParseResult(boolean success, String errorMessage, String filePath) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.filePath = filePath;
    }

    /**
     * Creates a successful parse result.
     * @param filePath The path of the file that was successfully parsed
     * @return A successful ParseResult
     */
    public static ParseResult success(String filePath) {
        return new ParseResult(true, null, filePath);
    }

    /**
     * Creates a failed parse result.
     * @param errorMessage The error message describing why parsing failed
     * @param filePath The path of the file that failed to parse (may be null)
     * @return A failed ParseResult
     */
    public static ParseResult failure(String errorMessage, String filePath) {
        return new ParseResult(false, errorMessage, filePath);
    }

    /**
     * Creates a failed parse result when the file was not found.
     * @param errorMessage The error message
     * @return A failed ParseResult with null filePath
     */
    public static ParseResult failure(String errorMessage) {
        return new ParseResult(false, errorMessage, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getFilePath() {
        return filePath;
    }
}

