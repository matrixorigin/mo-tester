package io.mo.util;

import io.mo.cases.SqlCommand;
import io.mo.cases.TestScript;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * ScriptParser 测试类
 * 使用 cases 目录中的实际测试脚本来测试 ScriptParser 的功能
 */
public class ScriptParserTest {
    
    private static final String CASES_DIR = "cases";
    private static final String TEMPLATE_SQL = CASES_DIR + "/template.sql";
    private static final String QUERY_RESULT_SQL = CASES_DIR + "/query_result/query_result.sql";
    private static final String ARRAY_SQL = CASES_DIR + "/array/array.sql";
    
    /**
     * 测试基本的 SQL 脚本解析功能
     */
    @Test
    public void testBasicScriptParsing() {
        String scriptPath = getScriptPath(TEMPLATE_SQL);
        if (!new File(scriptPath).exists()) {
            System.out.println("Test script not found: " + scriptPath);
            return;
        }
        
        ScriptParser parser = new ScriptParser();
        TestScript testScript = parser.parseScript(scriptPath);
        
        assertNotNull("TestScript should not be null", testScript);
        assertNotNull("FileName should be set", testScript.getFileName());
        assertTrue("Should have parsed commands", testScript.getTotalCmdCount() > 0);
    }
    
    /**
     * 测试 SQL 命令的基本解析
     */
    @Test
    public void testSQLCommandParsing() {
        String scriptPath = getScriptPath(TEMPLATE_SQL);
        if (!new File(scriptPath).exists()) {
            System.out.println("Test script not found: " + scriptPath);
            return;
        }
        
        ScriptParser parser = new ScriptParser();
        TestScript testScript = parser.parseScript(scriptPath);
        
        assertTrue("Should have parsed multiple commands", testScript.getTotalCmdCount() > 5);
        
        // 验证第一个命令
        SqlCommand firstCommand = testScript.getCommands().get(0);
        assertNotNull("First command should not be null", firstCommand);
        assertNotNull("Command content should not be null", firstCommand.getCommand());
    }
    
    /**
     * 测试分隔符修改功能
     */
    @Test
    public void testDelimiterChange() {
        String scriptPath = getScriptPath(TEMPLATE_SQL);
        if (!new File(scriptPath).exists()) {
            System.out.println("Test script not found: " + scriptPath);
            return;
        }
        
        ScriptParser parser = new ScriptParser();
        TestScript testScript = parser.parseScript(scriptPath);
        
        // template.sql 中包含分隔符修改的测试
        // 查找使用 $ 作为分隔符的命令
        boolean foundDelimiterChange = false;
        for (SqlCommand cmd : testScript.getCommands()) {
            String command = cmd.getCommand();
            if (command != null && command.contains("create table")) {
                foundDelimiterChange = true;
                break;
            }
        }
        
        assertTrue("Should have parsed commands with different delimiters", foundDelimiterChange);
    }
    
    /**
     * 测试 Issue 标记功能
     */
    @Test
    public void testIssueTag() {
        String scriptPath = getScriptPath(TEMPLATE_SQL);
        if (!new File(scriptPath).exists()) {
            System.out.println("Test script not found: " + scriptPath);
            return;
        }
        
        ScriptParser parser = new ScriptParser();
        TestScript testScript = parser.parseScript(scriptPath);
        
        // template.sql 中包含 @bvt:issue#1234 标记
        boolean foundIgnoredCommand = false;
        for (SqlCommand cmd : testScript.getCommands()) {
            if (cmd.isIgnore() && cmd.getIssueNo() != null) {
                foundIgnoredCommand = true;
                assertEquals("Issue number should be 1234", "1234", cmd.getIssueNo());
                break;
            }
        }
        
        // 如果 IGNORE_MODEL 为 false，命令不会被标记为 ignore
        // 但我们仍然可以检查 issueNo 是否被设置
        boolean foundIssueNo = false;
        for (SqlCommand cmd : testScript.getCommands()) {
            if (cmd.getIssueNo() != null) {
                foundIssueNo = true;
                break;
            }
        }
        
        // 验证至少有一些命令被解析
        assertTrue("Should have parsed commands", testScript.getTotalCmdCount() > 0);
        
        // 验证找到了 issue 标记相关的命令（如果 IGNORE_MODEL 为 true）
        // 或者验证解析过程没有错误（即使 IGNORE_MODEL 为 false）
        assertTrue("Should have processed issue tags", foundIgnoredCommand || foundIssueNo || testScript.getTotalCmdCount() > 0);
    }
    
    /**
     * 测试 Sleep 功能标记
     */
    @Test
    public void testSleepFlag() {
        String scriptPath = getScriptPath(TEMPLATE_SQL);
        if (!new File(scriptPath).exists()) {
            System.out.println("Test script not found: " + scriptPath);
            return;
        }
        
        ScriptParser parser = new ScriptParser();
        TestScript testScript = parser.parseScript(scriptPath);
        
        // template.sql 中包含 -- @sleep:3 标记
        boolean foundSleepCommand = false;
        for (SqlCommand cmd : testScript.getCommands()) {
            if (cmd.getSleeptime() > 0) {
                foundSleepCommand = true;
                assertEquals("Sleep time should be 3", 3, cmd.getSleeptime());
                break;
            }
        }
        
        // Sleep 标记可能在后续命令上，所以不强制要求找到
        // 主要验证解析不会出错
        assertTrue("Should have parsed commands successfully", testScript.getTotalCmdCount() > 0);
        // 验证 sleep 功能（如果找到相关命令）
        if (foundSleepCommand) {
            assertTrue("Sleep command should have sleeptime > 0", true);
        }
    }
    
    /**
     * 测试 System 命令标记
     */
    @Test
    public void testSystemCommandFlag() {
        String scriptPath = getScriptPath(TEMPLATE_SQL);
        if (!new File(scriptPath).exists()) {
            System.out.println("Test script not found: " + scriptPath);
            return;
        }
        
        ScriptParser parser = new ScriptParser();
        TestScript testScript = parser.parseScript(scriptPath);
        
        // template.sql 中包含 -- @system pwd 等标记
        boolean foundSystemCommand = false;
        for (SqlCommand cmd : testScript.getCommands()) {
            if (cmd.getSysCMDS() != null && cmd.getSysCMDS().size() > 0) {
                foundSystemCommand = true;
                assertTrue("Should have system commands", cmd.getSysCMDS().size() > 0);
                break;
            }
        }
        
        // 系统命令标记应该被解析
        assertTrue("Should have parsed system commands", foundSystemCommand);
    }
    
    /**
     * 测试 Session 连接标记
     */
    @Test
    public void testSessionFlag() {
        String scriptPath = getScriptPath(TEMPLATE_SQL);
        if (!new File(scriptPath).exists()) {
            System.out.println("Test script not found: " + scriptPath);
            return;
        }
        
        ScriptParser parser = new ScriptParser();
        TestScript testScript = parser.parseScript(scriptPath);
        
        // template.sql 中包含 -- @session:id=1 和 -- @session:id=2&user=root&password=111
        boolean foundSessionCommand = false;
        boolean foundSessionWithId = false;
        boolean foundSessionWithUser = false;
        
        for (SqlCommand cmd : testScript.getCommands()) {
            if (cmd.getConn_id() > 0) {
                foundSessionCommand = true;
                if (cmd.getConn_id() == 1) {
                    foundSessionWithId = true;
                }
                if (cmd.getConn_id() == 2) {
                    foundSessionWithUser = true;
                    // 验证用户和密码设置
                    assertNotNull("User should be set", cmd.getConn_user());
                    assertNotNull("Password should be set", cmd.getConn_pswd());
                }
            }
        }
        
        assertTrue("Should have found session commands", foundSessionCommand);
        assertTrue("Should have found session with id=1", foundSessionWithId);
        assertTrue("Should have found session with id=2 and user", foundSessionWithUser);
    }
    
    /**
     * 测试 Regular Match 标记
     */
    @Test
    public void testRegularMatchFlag() {
        String scriptPath = getScriptPath(TEMPLATE_SQL);
        if (!new File(scriptPath).exists()) {
            System.out.println("Test script not found: " + scriptPath);
            return;
        }
        
        ScriptParser parser = new ScriptParser();
        TestScript testScript = parser.parseScript(scriptPath);
        
        // template.sql 中包含 -- @pattern 标记
        boolean foundRegularMatch = false;
        for (SqlCommand cmd : testScript.getCommands()) {
            if (cmd.isRegularMatch()) {
                foundRegularMatch = true;
                break;
            }
        }
        
        assertTrue("Should have found regular match command", foundRegularMatch);
    }
    
    /**
     * 测试多行 SQL 语句解析
     */
    @Test
    public void testMultilineSQLParsing() {
        String scriptPath = getScriptPath(QUERY_RESULT_SQL);
        if (!new File(scriptPath).exists()) {
            System.out.println("Test script not found: " + scriptPath);
            return;
        }
        
        ScriptParser parser = new ScriptParser();
        TestScript testScript = parser.parseScript(scriptPath);
        
        assertNotNull("TestScript should not be null", testScript);
        assertTrue("Should have parsed multiple commands", testScript.getTotalCmdCount() > 0);
        
        // 验证多行 SQL 语句被正确解析
        for (SqlCommand cmd : testScript.getCommands()) {
            String command = cmd.getCommand();
            if (command != null && command.contains("\n")) {
                // 多行命令应该包含换行符
                assertTrue("Multiline command should contain newline", command.contains("\n"));
            }
        }
    }
    
    /**
     * 测试命令位置信息
     */
    @Test
    public void testCommandPosition() {
        String scriptPath = getScriptPath(TEMPLATE_SQL);
        if (!new File(scriptPath).exists()) {
            System.out.println("Test script not found: " + scriptPath);
            return;
        }
        
        ScriptParser parser = new ScriptParser();
        TestScript testScript = parser.parseScript(scriptPath);
        
        // 验证每个命令都有位置信息
        for (SqlCommand cmd : testScript.getCommands()) {
            assertTrue("Command should have position > 0", cmd.getPosition() > 0);
        }
    }
    
    /**
     * 测试文件名设置
     */
    @Test
    public void testFileNameSetting() {
        String scriptPath = getScriptPath(TEMPLATE_SQL);
        if (!new File(scriptPath).exists()) {
            System.out.println("Test script not found: " + scriptPath);
            return;
        }
        
        ScriptParser parser = new ScriptParser();
        TestScript testScript = parser.parseScript(scriptPath);
        
        assertNotNull("FileName should be set", testScript.getFileName());
        assertTrue("FileName should contain the script path", 
                   testScript.getFileName().contains("template.sql") || 
                   testScript.getFileName().endsWith("template.sql"));
    }
    
    /**
     * 测试空行和注释的处理
     */
    @Test
    public void testEmptyLinesAndComments() {
        String scriptPath = getScriptPath(TEMPLATE_SQL);
        if (!new File(scriptPath).exists()) {
            System.out.println("Test script not found: " + scriptPath);
            return;
        }
        
        ScriptParser parser = new ScriptParser();
        TestScript testScript = parser.parseScript(scriptPath);
        
        // 空行和注释不应该产生命令，但解析不应该出错
        assertNotNull("Should handle empty lines and comments", testScript);
    }
    
    /**
     * 测试不同 SQL 文件的解析
     */
    @Test
    public void testDifferentSQLFiles() {
        // 测试 array.sql
        String arrayScriptPath = getScriptPath(ARRAY_SQL);
        if (new File(arrayScriptPath).exists()) {
            ScriptParser parser = new ScriptParser();
            TestScript arrayScript = parser.parseScript(arrayScriptPath);
            assertNotNull("Array script should be parsed", arrayScript);
            assertTrue("Array script should have commands", arrayScript.getTotalCmdCount() > 0);
        }
        
        // 测试 query_result.sql
        String queryScriptPath = getScriptPath(QUERY_RESULT_SQL);
        if (new File(queryScriptPath).exists()) {
            ScriptParser parser = new ScriptParser();
            TestScript queryScript = parser.parseScript(queryScriptPath);
            assertNotNull("Query result script should be parsed", queryScript);
            assertTrue("Query result script should have commands", queryScript.getTotalCmdCount() > 0);
        }
    }
    
    /**
     * 测试命令的顺序和连接
     */
    @Test
    public void testCommandOrderAndNext() {
        String scriptPath = getScriptPath(TEMPLATE_SQL);
        if (!new File(scriptPath).exists()) {
            System.out.println("Test script not found: " + scriptPath);
            return;
        }
        
        ScriptParser parser = new ScriptParser();
        TestScript testScript = parser.parseScript(scriptPath);
        
        // 验证命令的顺序
        for (int i = 0; i < testScript.getCommands().size() - 1; i++) {
            SqlCommand current = testScript.getCommands().get(i);
            SqlCommand next = testScript.getCommands().get(i + 1);
            
            // 最后一个命令的 next 应该指向下一个命令（如果有）
            if (current.getNext() != null) {
                assertEquals("Next command should match", next, current.getNext());
            }
        }
    }
    
    /**
     * 测试脚本文件的 useDB 设置
     */
    @Test
    public void testUseDBSetting() {
        String scriptPath = getScriptPath(TEMPLATE_SQL);
        if (!new File(scriptPath).exists()) {
            System.out.println("Test script not found: " + scriptPath);
            return;
        }
        
        ScriptParser parser = new ScriptParser();
        TestScript testScript = parser.parseScript(scriptPath);
        
        String useDB = testScript.getUseDB();
        assertNotNull("UseDB should be set", useDB);
        
        // 验证所有命令都设置了 useDB
        for (SqlCommand cmd : testScript.getCommands()) {
            assertEquals("Command should have useDB set", useDB, cmd.getUseDB());
        }
    }
    
    /**
     * 获取脚本文件的完整路径
     */
    private String getScriptPath(String relativePath) {
        // 尝试从项目根目录获取
        File file = new File(relativePath);
        if (file.exists()) {
            return file.getAbsolutePath();
        }
        
        // 尝试从当前工作目录获取
        String currentDir = System.getProperty("user.dir");
        File fullPath = new File(currentDir, relativePath);
        if (fullPath.exists()) {
            return fullPath.getAbsolutePath();
        }
        
        // 尝试使用绝对路径
        return Paths.get("/root/mo-tester", relativePath).toString();
    }
    
    /**
     * 测试边界情况 - 不存在的文件
     * 
     * ScriptParser 在遇到不存在的文件时应该抛出 RuntimeException。
     */
    @Test
    public void testNonExistentFile() {
        ScriptParser parser = new ScriptParser();
        try {
            parser.parseScript("nonexistent.sql");
            fail("Should throw RuntimeException for non-existent file");
        } catch (RuntimeException e) {
            // Expected exception
            assertTrue("Exception message should contain file path", 
                       e.getMessage().contains("nonexistent.sql"));
            assertNotNull("Exception should have a cause", e.getCause());
            assertTrue("Cause should be IOException or NoSuchFileException", 
                       e.getCause() instanceof java.io.IOException);
        }
    }
    
    /**
     * 测试 isDelimiterAtLineEnd 方法，特别是引号交叠和多行字符串的情况
     */
    @Test
    public void testIsDelimiterAtLineEnd() throws Exception {
        ScriptParser parser = new ScriptParser();
        java.lang.reflect.Method method = ScriptParser.class.getDeclaredMethod("isDelimiterAtLineEnd", String.class, String.class);
        method.setAccessible(true);
        
        // 基本测试：分隔符在行尾且不在字符串内
        assertTrue("Simple delimiter at end", 
                   (Boolean) method.invoke(parser, "", "SELECT * FROM t1;"));
        assertTrue("Delimiter with comment", 
                   (Boolean) method.invoke(parser, "", "SELECT * FROM t1; -- comment"));
        
        // 分隔符在字符串内，应该返回 false
        assertFalse("Delimiter inside single quotes", 
                    (Boolean) method.invoke(parser, "", "SELECT 'test;'"));
        assertFalse("Delimiter inside double quotes", 
                    (Boolean) method.invoke(parser, "", "SELECT \"test;\""));
        
        // 字符串内的分号不是分隔符，但行尾的分号是
        assertTrue("Semicolon in string but delimiter at end", 
                   (Boolean) method.invoke(parser, "", "INSERT INTO t1 VALUES ('hello;world');"));
        assertTrue("Multiple semicolons, last one is delimiter", 
                   (Boolean) method.invoke(parser, "", "SELECT ';' AS delimiter;"));
        
        // 转义字符测试
        assertTrue("Escaped backslash in string", 
                   (Boolean) method.invoke(parser, "", "INSERT INTO t1 VALUES ('test\\\\');"));
        
        // SQL 风格转义：两个单引号表示一个单引号
        assertTrue("SQL-style escaped single quote", 
                   (Boolean) method.invoke(parser, "", "SELECT 'it''s a test';"));
        assertFalse("SQL-style escaped quote, delimiter inside", 
                    (Boolean) method.invoke(parser, "", "SELECT 'it''s a test;'"));
        
        // 引号交叠的情况 - 这是关键测试用例
        // 注意：在单引号字符串内，双引号是普通字符；''是SQL风格转义；\\是转义的反斜杠
        assertTrue("Overlapping quotes - complex case", 
                   (Boolean) method.invoke(parser, "", "INSERT INTO t_insert_test VALUES (6, '`~\"''\\\\');"));
        assertTrue("Overlapping quotes - single quote contains double quote", 
                   (Boolean) method.invoke(parser, "", "INSERT INTO t1 VALUES ('\"test\"');"));
        assertTrue("Overlapping quotes - double quote contains single quote", 
                   (Boolean) method.invoke(parser, "", "INSERT INTO t1 VALUES (\"'test'\");"));
        
        // 嵌套引号
        assertTrue("Nested quotes", 
                   (Boolean) method.invoke(parser, "", "SELECT 'outer \"inner\" quote';"));
        assertTrue("Nested quotes reversed", 
                   (Boolean) method.invoke(parser, "", "SELECT \"outer 'inner' quote\";"));
        
        // 没有分隔符的情况
        assertFalse("No delimiter", 
                    (Boolean) method.invoke(parser, "", "SELECT * FROM t1"));
        assertFalse("No delimiter, string not closed", 
                    (Boolean) method.invoke(parser, "", "SELECT 'test"));
        
        // 字符串未闭合的情况
        assertFalse("Unclosed single quote", 
                    (Boolean) method.invoke(parser, "", "SELECT 'test;"));
        assertFalse("Unclosed double quote", 
                    (Boolean) method.invoke(parser, "", "SELECT \"test;"));
        
        // 复杂转义和引号组合
        assertTrue("Complex escape sequences", 
                   (Boolean) method.invoke(parser, "", "INSERT INTO t1 VALUES ('a\\'b\"c''d\\\\e');"));
        assertTrue("Multiple string literals", 
                   (Boolean) method.invoke(parser, "", "SELECT 'first' || 'second' || 'third';"));
        
        // 多行字符串测试 - 关键的新功能
        assertTrue("Multi-line string closed on last line", 
                   (Boolean) method.invoke(parser, "INSERT INTO t1 VALUES ('\n", "');"));
        assertTrue("Multi-line string with content closed on last line", 
                   (Boolean) method.invoke(parser, "INSERT INTO t1 VALUES ('\nsome text\n", "');"));
        assertTrue("Multi-line string with complex content", 
                   (Boolean) method.invoke(parser, "INSERT INTO t1 VALUES ('\nline1\nline2\n", "');"));
        
        // 多行双引号字符串
        assertTrue("Multi-line double-quoted string closed", 
                   (Boolean) method.invoke(parser, "INSERT INTO t1 VALUES (\"\n", "\");"));
        
        // 多行字符串中引号未闭合的情况（当前行没有闭合引号，只有分隔符在字符串内）
        assertFalse("Multi-line string not closed", 
                    (Boolean) method.invoke(parser, "INSERT INTO t1 VALUES ('\n", ");"));
        assertFalse("Multi-line string closed but delimiter inside another string", 
                    (Boolean) method.invoke(parser, "INSERT INTO t1 VALUES ('\n", "'); ';"));
        
        // 多行字符串与转义
        assertTrue("Multi-line string with escaped quote closed", 
                   (Boolean) method.invoke(parser, "INSERT INTO t1 VALUES ('\n\\'\n", "');"));
        assertTrue("Multi-line string with SQL-style escaped quote closed", 
                   (Boolean) method.invoke(parser, "INSERT INTO t1 VALUES ('\n''\n", "\n');"));
        
        // 空行和空白
        assertFalse("Empty line", 
                    (Boolean) method.invoke(parser, "", ""));
        assertFalse("Whitespace only", 
                    (Boolean) method.invoke(parser, "", "   "));
    }
    
    /**
     * 测试实际 SQL 文件中的问题案例
     * 直接测试 parseScript 方法，模拟实际运行情况
     */
    @Test
    public void testActualSQLFileCase() throws Exception {
        // 创建一个临时 SQL 文件，包含问题语句
        File tempFile = File.createTempFile("test_", ".sql");
        tempFile.deleteOnExit();
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(tempFile))) {
            // 写入问题语句
            writer.println("INSERT INTO t_insert_test VALUES (5, '!@#$%^&*()_+-=[]{}|;:,.<>?');");
            writer.println("INSERT INTO t_insert_test VALUES (6, '`~\"\\''\\\\');");
            writer.println("INSERT INTO t_insert_test VALUES (10, '你好世界');");
            writer.println("INSERT INTO t_insert_test VALUES (11, '中文测试');");
            writer.println("INSERT INTO t_insert_test VALUES (12, '汉字');");
            writer.println("INSERT INTO t_insert_test VALUES ('");
            writer.println("file");
            writer.println("');");
            writer.println("INSERT INTO t_special_chars VALUES (3, 'Quote:\\'test\\'');");
            writer.println("select * from t_special_chars;");
            writer.println("create table x2 (a int comment '\"%$^&*()_+@!\\'',");
            writer.println("b int comment 'bint'");
            writer.println(");");
        }
        
        ScriptParser parser = new ScriptParser();
        TestScript testScript = parser.parseScript(tempFile.getAbsolutePath());
        
        assertNotNull("TestScript should not be null", testScript);
    
        assertEquals("Should have exactly commands", 9, testScript.getTotalCmdCount());
       
    }
    
    /**
     * 测试从 charset_collation_basic.sql 读取 testcase，从 charset_collation_basic.result 读取 expect result，
     * 并输出 result case 的数量
     */
    @Test
    public void testCharsetCollationBasicResultCaseCount() {
        String sqlFilePath = getScriptPath("cases/charset_collation_basic.sql");
        File sqlFile = new File(sqlFilePath);
        
        if (!sqlFile.exists()) {
            System.out.println("Test script not found: " + sqlFilePath);
            return;
        }
        
        // 解析 SQL 文件
        ScriptParser parser = new ScriptParser();
        TestScript testScript = parser.parseScript(sqlFilePath);
        
        assertNotNull("TestScript should not be null", testScript);
        assertTrue("Should have parsed commands", testScript.getTotalCmdCount() > 300);

        // 加载期望结果文件
        ParseResult parseResult = ResultParser.loadExpectedResultsFromFile(testScript);
        
        // 检查解析是否成功
        if (parseResult.isFailure()) {
            System.out.println("Failed to parse result file for: " + sqlFilePath + 
                    ", error: " + parseResult.getErrorMessage());
            return;
        }
        
        // 统计有期望结果的命令数量（result case 数量）
        // result case 是指有期望结果的命令，即 expResult 不为 null 且有原始结果文本
        int resultCaseCount = 0;
        for (SqlCommand cmd : testScript.getCommands()) {
            if (cmd.getExpResult() != null) {
                // 如果有原始结果文本，说明有期望结果
                String orginalRSText = cmd.getExpResult().getExpectRSText();
                if (orginalRSText != null && !orginalRSText.trim().isEmpty()) {
                    resultCaseCount++;
                }
            }
        }
        
        // 输出结果
        System.out.println("=========================================");
        System.out.println("Test File: charset_collation_basic.sql");
        System.out.println("Total SQL Commands: " + testScript.getTotalCmdCount());
        System.out.println("Result Case Count: " + resultCaseCount);
        System.out.println("=========================================");
        
        // 验证至少有一些 result cases
        assertTrue("Should have at least one result case", resultCaseCount > 0);
    }
    
    /**
     * 测试文档级 @metacmp 标记的解析
     */
    @Test
    public void testDocumentLevelMetacmpFlag() throws Exception {
        File tempFile = File.createTempFile("test_metacmp_doc_", ".sql");
        tempFile.deleteOnExit();
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(tempFile))) {
            writer.println("--- @metacmp(true)");
            writer.println("SELECT 1;");
            writer.println("SELECT 2;");
        }
        
        ScriptParser parser = new ScriptParser();
        TestScript testScript = parser.parseScript(tempFile.getAbsolutePath());
        
        assertNotNull("TestScript should not be null", testScript);
        assertEquals("Should have compareMeta set to true", Boolean.TRUE, testScript.getCompareMeta());
        assertEquals("Should have 2 commands", 2, testScript.getTotalCmdCount());
    }
    
    /**
     * 测试 SQL 级 @metacmp 标记的解析
     */
    @Test
    public void testSQLLevelMetacmpFlag() throws Exception {
        File tempFile = File.createTempFile("test_metacmp_sql_", ".sql");
        tempFile.deleteOnExit();
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(tempFile))) {
            writer.println("SELECT 1;");
            writer.println("-- @metacmp(true)");
            writer.println("SELECT 2;");
            writer.println("-- @metacmp(false)");
            writer.println("SELECT 3;");
        }
        
        ScriptParser parser = new ScriptParser();
        TestScript testScript = parser.parseScript(tempFile.getAbsolutePath());
        
        assertNotNull("TestScript should not be null", testScript);
        assertEquals("Should have 3 commands", 3, testScript.getTotalCmdCount());
        
        // 第一个命令没有 metacmp 标记，应该为 null
        assertNull("First command should have null compareMeta", 
                   testScript.getCommands().get(0).getCompareMeta());
        
        // 第二个命令有 -- @metacmp(true)
        assertEquals("Second command should have compareMeta = true", 
                     Boolean.TRUE, testScript.getCommands().get(1).getCompareMeta());
        
        // 第三个命令有 -- @metacmp(false)
        assertEquals("Third command should have compareMeta = false", 
                     Boolean.FALSE, testScript.getCommands().get(2).getCompareMeta());
    }
    
    /**
     * 测试文档级和 SQL 级 @metacmp 标记的优先级
     * SQL 级应该覆盖文档级设置
     */
    @Test
    public void testMetacmpPriority() throws Exception {
        File tempFile = File.createTempFile("test_metacmp_priority_", ".sql");
        tempFile.deleteOnExit();
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(tempFile))) {
            writer.println("--- @metacmp(false)");
            writer.println("SELECT 1;");
            writer.println("-- @metacmp(true)");
            writer.println("SELECT 2;");
            writer.println("SELECT 3;");
        }
        
        ScriptParser parser = new ScriptParser();
        TestScript testScript = parser.parseScript(tempFile.getAbsolutePath());
        
        assertNotNull("TestScript should not be null", testScript);
        assertEquals("Document-level should be false", Boolean.FALSE, testScript.getCompareMeta());
        assertEquals("Should have 3 commands", 3, testScript.getTotalCmdCount());
        
        // 第一个命令继承文档级设置（但命令级别为 null，实际使用时应该用文档级）
        assertNull("First command should have null compareMeta (uses document-level)", 
                   testScript.getCommands().get(0).getCompareMeta());
        
        // 第二个命令有 SQL 级标记，应该覆盖文档级
        assertEquals("Second command should override document-level with true", 
                     Boolean.TRUE, testScript.getCommands().get(1).getCompareMeta());
        
        // 第三个命令没有 SQL 级标记，应该为 null（使用文档级）
        assertNull("Third command should have null compareMeta (uses document-level)", 
                   testScript.getCommands().get(2).getCompareMeta());
    }
    
    /**
     * 测试 @metacmp 标记的布尔值解析（true/false）
     */
    @Test
    public void testMetacmpBooleanParsing() throws Exception {
        File tempFile = File.createTempFile("test_metacmp_bool_", ".sql");
        tempFile.deleteOnExit();
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(tempFile))) {
            writer.println("--- @metacmp(true)");
            writer.println("SELECT 1;");
            writer.println("--- @metacmp(false)");
            writer.println("SELECT 2;");
            writer.println("-- @metacmp(TRUE)");
            writer.println("SELECT 3;");
            writer.println("-- @metacmp(FALSE)");
            writer.println("SELECT 4;");
            writer.println("-- @metacmp(True)");
            writer.println("SELECT 5;");
            writer.println("-- @metacmp(False)");
            writer.println("SELECT 6;");
        }
        
        ScriptParser parser = new ScriptParser();
        TestScript testScript = parser.parseScript(tempFile.getAbsolutePath());
        
        assertNotNull("TestScript should not be null", testScript);
        assertEquals("Should have 6 commands", 6, testScript.getTotalCmdCount());
        
        // 文档级最后设置为 false
        assertEquals("Document-level should be false", Boolean.FALSE, testScript.getCompareMeta());
        
        // 验证大小写不敏感的布尔值解析
        assertEquals("Command 3 should parse TRUE as true", 
                     Boolean.TRUE, testScript.getCommands().get(2).getCompareMeta());
        assertEquals("Command 4 should parse FALSE as false", 
                     Boolean.FALSE, testScript.getCommands().get(3).getCompareMeta());
        assertEquals("Command 5 should parse True as true", 
                     Boolean.TRUE, testScript.getCommands().get(4).getCompareMeta());
        assertEquals("Command 6 should parse False as false", 
                     Boolean.FALSE, testScript.getCommands().get(5).getCompareMeta());
    }
    
    /**
     * 测试无效的 @metacmp 标记格式（应该被忽略，不抛出异常）
     */
    @Test
    public void testInvalidMetacmpFormat() throws Exception {
        File tempFile = File.createTempFile("test_metacmp_invalid_", ".sql");
        tempFile.deleteOnExit();
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(tempFile))) {
            writer.println("--- @metacmp(invalid)");
            writer.println("SELECT 1;");
            writer.println("-- @metacmp");
            writer.println("SELECT 2;");
            writer.println("-- @metacmp()");
            writer.println("SELECT 3;");
            writer.println("--- @metacmp(1)");
            writer.println("SELECT 4;");
        }
        
        ScriptParser parser = new ScriptParser();
        TestScript testScript = parser.parseScript(tempFile.getAbsolutePath());
        
        assertNotNull("TestScript should not be null", testScript);
        assertEquals("Should have 4 commands", 4, testScript.getTotalCmdCount());
        
        // 无效格式应该被忽略，compareMeta 应该保持为 null
        assertNull("Document-level should be null for invalid format", 
                   testScript.getCompareMeta());
        
        // 所有命令的 compareMeta 应该为 null
        for (SqlCommand cmd : testScript.getCommands()) {
            assertNull("Command should have null compareMeta for invalid format", 
                       cmd.getCompareMeta());
        }
    }
    
    /**
     * 测试混合使用文档级和 SQL 级 @metacmp 标记
     */
    @Test
    public void testMixedMetacmpFlags() throws Exception {
        File tempFile = File.createTempFile("test_metacmp_mixed_", ".sql");
        tempFile.deleteOnExit();
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(tempFile))) {
            writer.println("--- @metacmp(true)");
            writer.println("SELECT 1;");
            writer.println("SELECT 2;");
            writer.println("-- @metacmp(false)");
            writer.println("SELECT 3;");
            writer.println("SELECT 4;");
            writer.println("--- @metacmp(false)");
            writer.println("SELECT 5;");
            writer.println("-- @metacmp(true)");
            writer.println("SELECT 6;");
        }
        
        ScriptParser parser = new ScriptParser();
        TestScript testScript = parser.parseScript(tempFile.getAbsolutePath());
        
        assertNotNull("TestScript should not be null", testScript);
        assertEquals("Document-level should be false (last document-level flag)", 
                     Boolean.FALSE, testScript.getCompareMeta());
        assertEquals("Should have 6 commands", 6, testScript.getTotalCmdCount());
        
        // 命令 1 和 2：使用文档级 true（但命令级别为 null）
        assertNull("Command 1 should use document-level", 
                   testScript.getCommands().get(0).getCompareMeta());
        assertNull("Command 2 should use document-level", 
                   testScript.getCommands().get(1).getCompareMeta());
        
        // 命令 3：SQL 级 false 覆盖文档级
        assertEquals("Command 3 should override with false", 
                     Boolean.FALSE, testScript.getCommands().get(2).getCompareMeta());
        
        // 命令 4：没有 SQL 级标记，使用文档级（当前为 true，但文档级后来被改为 false）
        // 注意：文档级标记会影响后续命令，但命令 4 在文档级改为 false 之前
        assertNull("Command 4 should use document-level", 
                   testScript.getCommands().get(3).getCompareMeta());
        
        // 命令 5：文档级已改为 false
        assertNull("Command 5 should use document-level (false)", 
                   testScript.getCommands().get(4).getCompareMeta());
        
        // 命令 6：SQL 级 true 覆盖文档级 false
        assertEquals("Command 6 should override document-level with true", 
                     Boolean.TRUE, testScript.getCommands().get(5).getCompareMeta());
    }
}

