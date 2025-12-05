package io.mo.util;

import io.mo.cases.RegexPattern;
import io.mo.cases.SqlCommand;
import io.mo.cases.TestScript;
import io.mo.constant.RESULT;
import io.mo.result.RSSet;
import io.mo.result.StmtResult;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * 测试正则表达式解析和匹配功能
 */
public class RegexParsingTest {

    private static final String TEST_DIR = "test_regex";
    private static final String TEST_SQL_FILE = TEST_DIR + "/test_regex.sql";
    private static final String TEST_RESULT_FILE = TEST_DIR + "/test_regex.result";

    /**
     * 测试解析单个 regex 指令
     */
    @Test
    public void testParseSingleRegexDirective() throws IOException {
        createTestFile(TEST_SQL_FILE, 
            "-- @regex(\"test.*pattern\", true)\n" +
            "SELECT * FROM test_table;"
        );

        try {
            ScriptParser parser = new ScriptParser();
            TestScript testScript = parser.parseScript(TEST_SQL_FILE);
            
            assertNotNull("TestScript should not be null", testScript);
            assertEquals("Should have 1 command", 1, testScript.getCommands().size());
            
            SqlCommand command = testScript.getCommands().get(0);
            assertNotNull("Command should not be null", command);
            assertEquals("Should have 1 regex pattern", 1, command.getRegexPatterns().size());
            
            RegexPattern regexPattern = command.getRegexPatterns().get(0);
            assertEquals("test.*pattern", regexPattern.getPattern());
            assertTrue(regexPattern.isInclude());
            assertNotNull(regexPattern.getCompiledPattern());
        } finally {
            cleanupTestFile(TEST_SQL_FILE);
        }
    }

    /**
     * 测试解析多个 regex 指令
     */
    @Test
    public void testParseMultipleRegexDirectives() throws IOException {
        createTestFile(TEST_SQL_FILE,
            "-- @regex(\"pattern1\", true)\n" +
            "-- @regex(\"pattern2\", false)\n" +
            "-- @regex(\"pattern3\", true)\n" +
            "SELECT * FROM test_table;"
        );

        try {
            ScriptParser parser = new ScriptParser();
            TestScript testScript = parser.parseScript(TEST_SQL_FILE);
            
            SqlCommand command = testScript.getCommands().get(0);
            assertEquals("Should have 3 regex patterns", 3, command.getRegexPatterns().size());
            
            RegexPattern pattern1 = command.getRegexPatterns().get(0);
            assertEquals("pattern1", pattern1.getPattern());
            assertTrue(pattern1.isInclude());
            
            RegexPattern pattern2 = command.getRegexPatterns().get(1);
            assertEquals("pattern2", pattern2.getPattern());
            assertFalse(pattern2.isInclude());
            
            RegexPattern pattern3 = command.getRegexPatterns().get(2);
            assertEquals("pattern3", pattern3.getPattern());
            assertTrue(pattern3.isInclude());
        } finally {
            cleanupTestFile(TEST_SQL_FILE);
        }
    }

    /**
     * 测试单引号格式的 pattern
     */
    @Test
    public void testParseSingleQuotedPattern() throws IOException {
        createTestFile(TEST_SQL_FILE,
            "-- @regex('test.*pattern', true)\n" +
            "SELECT * FROM test_table;"
        );

        try {
            ScriptParser parser = new ScriptParser();
            TestScript testScript = parser.parseScript(TEST_SQL_FILE);
            
            SqlCommand command = testScript.getCommands().get(0);
            RegexPattern regexPattern = command.getRegexPatterns().get(0);
            assertEquals("test.*pattern", regexPattern.getPattern());
        } finally {
            cleanupTestFile(TEST_SQL_FILE);
        }
    }

    /**
     * 测试转义字符的处理
     */
    @Test
    public void testParseEscapedCharacters() throws IOException {
        // 在 Java 字符串中，\\ 表示一个反斜杠，所以 "test\\\\.*pattern" 在文件中是 "test\\.*pattern"
        // 但实际写入文件时，Java 字符串 "test\\\\.*pattern" 会被写入为 "test\\.*pattern"
        // 解析时会去掉引号并处理转义，所以最终 pattern 应该是 "test\\.*pattern"
        createTestFile(TEST_SQL_FILE,
            "-- @regex(\"test\\\\.*pattern\", true)\n" +
            "-- @regex(\"(?i)test\\\"pattern\", true)\n" +
            "SELECT * FROM test_table;"
        );

        try {
            ScriptParser parser = new ScriptParser();
            TestScript testScript = parser.parseScript(TEST_SQL_FILE);
            
            SqlCommand command = testScript.getCommands().get(0);
            assertNotNull("Command should have regex patterns", command.getRegexPatterns());
            assertTrue("Should have at least one regex pattern", command.getRegexPatterns().size() > 0);
            
            assertTrue(command.getRegexPatterns().get(0).getCompiledPattern().matcher("test\\abcpattern").find());
            assertTrue(command.getRegexPatterns().get(1).getCompiledPattern().matcher("test\"Pattern").find());

        } finally {
            cleanupTestFile(TEST_SQL_FILE);
        }
    }

    /**
     * 测试无效的正则表达式（应该抛出异常）
     */
    @Test(expected = RuntimeException.class)
    public void testInvalidRegexPattern() throws IOException {
        createTestFile(TEST_SQL_FILE,
            "-- @regex(\"[invalid\", true)\n" +
            "SELECT * FROM test_table;"
        );

        try {
            ScriptParser parser = new ScriptParser();
            parser.parseScript(TEST_SQL_FILE);
            fail("Should throw RuntimeException for invalid regex pattern");
        } finally {
            cleanupTestFile(TEST_SQL_FILE);
        }
    }

    /**
     * 测试 include=false 的情况
     */
    @Test
    public void testIncludeFalse() throws IOException {
        createTestFile(TEST_SQL_FILE,
            "-- @regex(\"error\", false)\n" +
            "SELECT * FROM test_table;"
        );

        try {
            ScriptParser parser = new ScriptParser();
            TestScript testScript = parser.parseScript(TEST_SQL_FILE);
            
            SqlCommand command = testScript.getCommands().get(0);
            RegexPattern regexPattern = command.getRegexPatterns().get(0);
            assertFalse(regexPattern.isInclude());
        } finally {
            cleanupTestFile(TEST_SQL_FILE);
        }
    }

    /**
     * 测试从结果文件解析 regex
     */
    @Test
    public void testParseRegexFromResultFile() throws IOException {
        createTestFile(TEST_SQL_FILE, "SELECT * FROM test_table;");
        createTestFile(TEST_RESULT_FILE,
            "SELECT * FROM test_table;\n" +
            "-- @regex(\"test.*\", true)\n" +
            "-- @regex(\"error\", false)\n" +
            "test_result"
        );

        try {
            ScriptParser parser = new ScriptParser();
            TestScript testScript = parser.parseScript(TEST_SQL_FILE);
            
            ResultParser.loadExpectedResultsFromFile(testScript);
            
            SqlCommand command = testScript.getCommands().get(0);
            assertEquals("Should have 2 regex patterns from result file", 2, command.getRegexPatterns().size());
            
            RegexPattern pattern1 = command.getRegexPatterns().get(0);
            assertEquals("test.*", pattern1.getPattern());
            assertTrue(pattern1.isInclude());
            
            RegexPattern pattern2 = command.getRegexPatterns().get(1);
            assertEquals("error", pattern2.getPattern());
            assertFalse(pattern2.isInclude());
        } finally {
            cleanupTestFile(TEST_SQL_FILE);
            cleanupTestFile(TEST_RESULT_FILE);
        }
    }

    /**
     * 测试 regex 匹配逻辑 - ERROR 类型
     */
    @Test
    public void testRegexMatchingWithErrorType() throws IOException {
        createTestFile(TEST_SQL_FILE,
            "-- @regex(\"error.*message\", true)\n" +
            "SELECT * FROM invalid_table;"
        );

        try {
            ScriptParser parser = new ScriptParser();
            TestScript testScript = parser.parseScript(TEST_SQL_FILE);
            
            SqlCommand command = testScript.getCommands().get(0);
            assertEquals(1, command.getRegexPatterns().size());
            
            // 创建 ERROR 类型的 actualResult
            StmtResult actResult = new StmtResult();
            actResult.setType(RESULT.STMT_RESULT_TYPE_ERROR);
            actResult.setErrorMessage("error: table not found message");
            command.setActResult(actResult);
            
            // 创建空的 expResult（regex 匹配不需要 expResult）
            StmtResult expResult = new StmtResult();
            command.setExpResult(expResult);
            
            // 测试匹配
            assertTrue("Should match error message", command.checkResult());
        } finally {
            cleanupTestFile(TEST_SQL_FILE);
        }
    }

    /**
     * 测试 regex 匹配逻辑 - SET 类型
     */
    @Test
    public void testRegexMatchingWithSetType() throws IOException {
        createTestFile(TEST_SQL_FILE,
            "-- @regex(\"test.*result\", true)\n" +
            "SELECT * FROM test_table;"
        );

        try {
            ScriptParser parser = new ScriptParser();
            TestScript testScript = parser.parseScript(TEST_SQL_FILE);
            
            SqlCommand command = testScript.getCommands().get(0);
            
            // 创建 SET 类型的 actualResult
            StmtResult actResult = new StmtResult();
            actResult.setType(RESULT.STMT_RESULT_TYPE_SET);
            
            // 创建 RSSet 并设置必要的 meta 信息，以便 toString() 能正常工作
            RSSet rsSet = new RSSet();
            io.mo.result.RSMetaData meta = new io.mo.result.RSMetaData(1);
            meta.addMetaInfo("col1", "col1", io.mo.constant.DATATYPE.TYPE_STRING, 0);
            rsSet.setMeta(meta);
            
            // 添加一行数据，包含 "test_result"
            io.mo.result.RSRow row = new io.mo.result.RSRow(1);
            io.mo.result.RSCell cell = new io.mo.result.RSCell();
            cell.setValue("test_result");
            cell.setType(io.mo.constant.DATATYPE.TYPE_STRING);
            row.addCell(cell);
            rsSet.addRow(row);
            
            actResult.setRsSet(rsSet);
            command.setActResult(actResult);
            
            StmtResult expResult = new StmtResult();
            command.setExpResult(expResult);
            
            // 应该匹配成功（rsSet.toString() 包含 "test_result"）
            assertTrue("Should match when pattern is found in rsSet", command.checkResult());
        } finally {
            cleanupTestFile(TEST_SQL_FILE);
        }
    }

    /**
     * 测试 include=false 的匹配逻辑
     */
    @Test
    public void testRegexMatchingIncludeFalse() throws IOException {
        createTestFile(TEST_SQL_FILE,
            "-- @regex(\"error\", false)\n" +
            "SELECT * FROM test_table;"
        );

        try {
            ScriptParser parser = new ScriptParser();
            TestScript testScript = parser.parseScript(TEST_SQL_FILE);
            
            SqlCommand command = testScript.getCommands().get(0);
            
            // 创建 ERROR 类型但不包含 "error" 的 actualResult
            StmtResult actResult = new StmtResult();
            actResult.setType(RESULT.STMT_RESULT_TYPE_ERROR);
            actResult.setErrorMessage("success: operation completed");
            command.setActResult(actResult);
            
            StmtResult expResult = new StmtResult();
            command.setExpResult(expResult);
            
            // 应该匹配成功（因为不包含 "error"）
            assertTrue("Should match when pattern is not found (include=false)", command.checkResult());
        } finally {
            cleanupTestFile(TEST_SQL_FILE);
        }
    }

    /**
     * 测试多个 regex pattern 的匹配（所有都必须通过）
     */
    @Test
    public void testMultipleRegexPatternsAllMustPass() throws IOException {
        createTestFile(TEST_SQL_FILE,
            "-- @regex(\"pattern1\", true)\n" +
            "-- @regex(\"pattern2\", true)\n" +
            "SELECT * FROM test_table;"
        );

        try {
            ScriptParser parser = new ScriptParser();
            TestScript testScript = parser.parseScript(TEST_SQL_FILE);
            
            SqlCommand command = testScript.getCommands().get(0);
            assertEquals(2, command.getRegexPatterns().size());
            
            // 创建包含两个 pattern 的 actualResult
            StmtResult actResult = new StmtResult();
            actResult.setType(RESULT.STMT_RESULT_TYPE_ERROR);
            actResult.setErrorMessage("This contains pattern1 and pattern2");
            command.setActResult(actResult);
            
            StmtResult expResult = new StmtResult();
            command.setExpResult(expResult);
            
            // 应该匹配成功（两个 pattern 都找到了）
            assertTrue("Should match when all patterns are found", command.checkResult());
        } finally {
            cleanupTestFile(TEST_SQL_FILE);
        }
    }

    /**
     * 测试多个 regex pattern 中有一个失败的情况
     */
    @Test
    public void testMultipleRegexPatternsOneFails() throws IOException {
        createTestFile(TEST_SQL_FILE,
            "-- @regex(\"pattern1\", true)\n" +
            "-- @regex(\"pattern2\", true)\n" +
            "SELECT * FROM test_table;"
        );

        try {
            ScriptParser parser = new ScriptParser();
            TestScript testScript = parser.parseScript(TEST_SQL_FILE);
            
            SqlCommand command = testScript.getCommands().get(0);
            assertEquals("Should have 2 regex patterns", 2, command.getRegexPatterns().size());
            
            // 创建只包含一个 pattern 的 actualResult
            // 注意：错误消息是 "This contains pattern1 but not pattern2"
            // 这个字符串包含 "pattern1" 但不包含 "pattern2"
            StmtResult actResult = new StmtResult();
            actResult.setType(RESULT.STMT_RESULT_TYPE_ERROR);
            // 使用一个明确不包含 pattern2 的消息
            actResult.setErrorMessage("This message contains pattern1 only");
            command.setActResult(actResult);
            
            StmtResult expResult = new StmtResult();
            command.setExpResult(expResult);
            
            // 应该匹配失败（第二个 pattern "pattern2" 没找到）
            boolean result = command.checkResult();
            assertFalse("Should fail when one pattern is not found, but got: " + result, result);
        } finally {
            cleanupTestFile(TEST_SQL_FILE);
        }
    }

    /**
     * 测试不支持的 result type
     */
    @Test
    public void testUnsupportedResultType() throws IOException {
        createTestFile(TEST_SQL_FILE,
            "-- @regex(\"pattern\", true)\n" +
            "SELECT * FROM test_table;"
        );

        try {
            ScriptParser parser = new ScriptParser();
            TestScript testScript = parser.parseScript(TEST_SQL_FILE);
            
            SqlCommand command = testScript.getCommands().get(0);
            
            // 创建 NONE 类型的 actualResult（不支持）
            StmtResult actResult = new StmtResult();
            actResult.setType(RESULT.STMT_RESULT_TYPE_NONE);
            command.setActResult(actResult);
            
            StmtResult expResult = new StmtResult();
            command.setExpResult(expResult);
            
            // 应该匹配失败（NONE 类型不支持）
            assertFalse("Should fail for unsupported result type", command.checkResult());
        } finally {
            cleanupTestFile(TEST_SQL_FILE);
        }
    }

    // Helper methods
    private void createTestFile(String filePath, String content) throws IOException {
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }

    private void cleanupTestFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
            File parentDir = new File(filePath).getParentFile();
            if (parentDir != null && parentDir.exists() && parentDir.listFiles().length == 0) {
                parentDir.delete();
            }
        } catch (IOException e) {
            // Ignore cleanup errors
        }
    }
}
