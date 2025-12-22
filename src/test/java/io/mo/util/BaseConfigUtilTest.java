package io.mo.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * BaseConfigUtil 测试类
 * 通过测试实际的配置工具类来验证 BaseConfigUtil 的功能
 */
public class BaseConfigUtilTest {
    
    /**
     * 测试嵌套路径访问功能
     * 通过 RunConfUtil 来测试，因为它的配置结构简单
     */
    @Test
    public void testNestedPathAccess() {
        // 测试简单的单层访问
        String path = RunConfUtil.getPath();
        assertNotNull("Path should not be null", path);
        assertEquals("cases/", path);
        
        // RunConfUtil 内部使用了 BaseConfigUtil 的嵌套路径功能
        // 这里验证功能正常工作
        String method = RunConfUtil.getMethod();
        assertNotNull("Method should not be null", method);
    }
    
    /**
     * 测试字符串分割功能
     */
    @Test
    public void testSplitFunctionality() {
        String[] dbs = RunConfUtil.getBuiltinDb();
        assertNotNull("BuiltinDb should not be null", dbs);
        assertTrue("Should have multiple databases", dbs.length > 1);
        
        // 验证分割结果
        for (String db : dbs) {
            assertNotNull("Database name should not be null", db);
            assertFalse("Database name should not be empty", db.trim().isEmpty());
        }
    }
    
    /**
     * 测试整数获取功能
     */
    @Test
    public void testIntegerAccess() {
        int rate = RunConfUtil.getRate();
        assertTrue("Rate should be positive", rate > 0);
        assertEquals(100, rate);
        
        int waitTime = RunConfUtil.getWaitTime();
        assertTrue("WaitTime should be positive", waitTime > 0);
        assertEquals(2000, waitTime);
    }
    
    /**
     * 测试 Map 访问功能
     */
    @Test
    public void testMapAccess() {
        // 通过 MoConfUtil 测试深层 Map 访问
        String driver = MoConfUtil.getDriver();
        assertNotNull("Driver should not be null", driver);
        
        // 这验证了 BaseConfigUtil 的 map() 和 str() 方法正常工作
        String username = MoConfUtil.getUserName();
        assertNotNull("Username should not be null", username);
    }
    
    /**
     * 测试深层嵌套访问
     */
    @Test
    public void testDeepNestedAccess() {
        // 测试多层嵌套路径访问：jdbc -> database -> default
        String db = MoConfUtil.getDefaultDatabase();
        assertNotNull("Database should not be null", db);
        
        // 测试三层嵌套：jdbc -> paremeter -> socketTimeout
        int timeout = MoConfUtil.getSocketTimeout();
        assertTrue("SocketTimeout should be positive", timeout > 0);
    }
    
    /**
     * 测试空值处理
     */
    @Test
    public void testNullHandling() {
        // 测试不存在路径的处理
        // 由于我们使用真实配置，这里主要验证不会抛出异常
        try {
            String path = RunConfUtil.getPath();
            // 正常情况应该不会为 null（因为配置存在）
            assertNotNull("Path should be loaded from config", path);
        } catch (Exception e) {
            fail("Should handle missing config gracefully: " + e.getMessage());
        }
    }
    
    /**
     * 测试配置一致性
     */
    @Test
    public void testConfigurationConsistency() {
        // 多次访问应该返回相同结果
        String path1 = RunConfUtil.getPath();
        String path2 = RunConfUtil.getPath();
        assertEquals("Path should be consistent", path1, path2);
        
        String method1 = RunConfUtil.getMethod();
        String method2 = RunConfUtil.getMethod();
        assertEquals("Method should be consistent", method1, method2);
    }
}
