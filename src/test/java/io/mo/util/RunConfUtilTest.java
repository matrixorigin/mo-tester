package io.mo.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * RunConfUtil 测试类
 */
public class RunConfUtilTest {
    
    @Test
    public void testGetPath() {
        String path = RunConfUtil.getPath();
        assertNotNull("Path should not be null", path);
        assertEquals("cases/", path);
    }
    
    @Test
    public void testGetMethod() {
        String method = RunConfUtil.getMethod();
        assertNotNull("Method should not be null", method);
        assertEquals("run", method);
    }
    
    @Test
    public void testGetRate() {
        int rate = RunConfUtil.getRate();
        assertTrue("Rate should be positive", rate > 0);
        assertEquals(100, rate);
    }
    
    @Test
    public void testGetWaitTime() {
        int waitTime = RunConfUtil.getWaitTime();
        assertTrue("WaitTime should be positive", waitTime > 0);
        assertEquals(2000, waitTime);
    }
    
    @Test
    public void testGetBuiltinDb() {
        String[] dbs = RunConfUtil.getBuiltinDb();
        assertNotNull("BuiltinDb should not be null", dbs);
        assertTrue("Should have multiple builtin databases", dbs.length > 0);
        
        // 验证包含预期的数据库
        boolean found = false;
        for (String db : dbs) {
            if ("mo_catalog".equals(db) || "information_schema".equals(db)) {
                found = true;
                break;
            }
        }
        assertTrue("Should contain expected databases", found);
        
        // 验证没有空字符串
        for (String db : dbs) {
            assertFalse("Database name should not be empty", db.trim().isEmpty());
        }
    }
    
    @Test
    public void testGetOutFiles() {
        String[] outFiles = RunConfUtil.getOutFiles();
        assertNotNull("OutFiles should not be null", outFiles);
        assertTrue("Should have output files", outFiles.length > 0);
        
        // 验证文件列表格式
        for (String file : outFiles) {
            assertNotNull("File path should not be null", file);
            assertFalse("File path should not be empty", file.trim().isEmpty());
        }
    }
    

    
    @Test
    public void testConfigurationLoad() {
        // 测试所有配置项都能正常加载
        String path = RunConfUtil.getPath();
        String method = RunConfUtil.getMethod();
        int rate = RunConfUtil.getRate();
        int waitTime = RunConfUtil.getWaitTime();
        String[] dbs = RunConfUtil.getBuiltinDb();
        String[] outFiles = RunConfUtil.getOutFiles();
        
        assertNotNull("Path should be loaded", path);
        assertNotNull("Method should be loaded", method);
        assertTrue("Rate should be valid", rate > 0);
        assertTrue("WaitTime should be valid", waitTime > 0);
        assertNotNull("BuiltinDb should be loaded", dbs);
        assertNotNull("OutFiles should be loaded", outFiles);
    }
    
    @Test
    public void testGetBuiltinDbFormat() {
        String[] dbs = RunConfUtil.getBuiltinDb();
        assertNotNull(dbs);
        
        // 验证数据库名称格式（不包含空格，不包含特殊字符）
        for (String db : dbs) {
            assertFalse("Database name should not contain spaces", db.contains(" "));
            assertFalse("Database name should not be empty", db.isEmpty());
        }
    }
    
    @Test
    public void testGetOutFilesFormat() {
        String[] outFiles = RunConfUtil.getOutFiles();
        assertNotNull(outFiles);
        
        // 验证文件路径格式
        for (String file : outFiles) {
            assertFalse("File path should not be empty", file.isEmpty());
            assertFalse("File path should not be blank", file.trim().isEmpty());
        }
    }
    

    
    @Test
    public void testAllConfigurations() {
        // 综合测试所有配置方法
        assertNotNull("Path", RunConfUtil.getPath());
        assertNotNull("Method", RunConfUtil.getMethod());
        assertTrue("Rate > 0", RunConfUtil.getRate() > 0);
        assertTrue("WaitTime > 0", RunConfUtil.getWaitTime() > 0);
        assertNotNull("BuiltinDb", RunConfUtil.getBuiltinDb());
        assertNotNull("OutFiles", RunConfUtil.getOutFiles());
    }
}
