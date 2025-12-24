package io.mo.util;

import org.junit.Test;

import io.mo.constant.COMMON;

import static org.junit.Assert.*;

/**
 * MoConfUtil 测试类
 */
public class MoConfUtilTest {
    
    @Test
    public void testGetDriver() {
        String driver = MoConfUtil.getDriver();
        assertNotNull("Driver should not be null", driver);
        assertEquals("com.mysql.cj.jdbc.Driver", driver);
    }
    
    @Test
    public void testGetUserName() {
        String username = MoConfUtil.getUserName();
        assertNotNull("Username should not be null", username);
        assertEquals("dump", username);
    }
    
    @Test
    public void testGetUserpwd() {
        String password = MoConfUtil.getUserpwd();
        assertNotNull("Password should not be null", password);
        assertEquals("111", password);
    }
    
    @Test
    public void testGetSysUserName() {
        String sysUsername = MoConfUtil.getSysUserName();
        assertNotNull("SysUsername should not be null", sysUsername);
        assertEquals("dump", sysUsername);
    }
    
    @Test
    public void testGetSyspwd() {
        String sysPassword = MoConfUtil.getSyspwd();
        assertNotNull("SysPassword should not be null", sysPassword);
        assertEquals("111", sysPassword);
    }
    
    @Test
    public void testGetDefaultDatabase() {
        String db = MoConfUtil.getDefaultDatabase();
        assertNotNull("Database should not be null", db);
        // 根据 mo.yml，default 是空字符串
        assertEquals("", db);
    }
    
    @Test
    public void testGetSocketTimeout() {
        int timeout = MoConfUtil.getSocketTimeout();
        assertTrue("SocketTimeout should be positive", timeout > 0);
        // 根据 mo.yml，socketTimeout 是 120000
        assertEquals(120000, timeout);
    }
    
    @Test
    public void testGetSocketTimeoutWithDefault() {
        // 如果配置中没有 socketTimeout，应该返回默认值
        int timeout = MoConfUtil.getSocketTimeout();
        // 如果配置存在，使用配置值；否则使用默认值
        assertTrue("Timeout should be at least default value", 
                   timeout >= COMMON.DEFAULT_MAX_EXECUTE_TIME || timeout == 120000);
    }
    
    @Test
    public void testGetDebugServers() {
        String[] servers = MoConfUtil.getDebugServers();
        assertNotNull("Debug servers should not be null", servers);
        assertEquals(1, servers.length);
        assertEquals("127.0.0.1", servers[0]);
    }
    
    @Test
    public void testGetDebugPort() {
        int port = MoConfUtil.getDebugPort();
        assertTrue("Debug port should be positive", port > 0);
        assertEquals(6060, port);
    }
    
    @Test
    public void testGetURL() {
        String url = MoConfUtil.getURL();
        assertNotNull("URL should not be null", url);
        assertTrue("URL should start with jdbc:mysql://", url.startsWith("jdbc:mysql://"));
        assertTrue("URL should contain server address", url.contains("127.0.0.1:6001"));
        assertTrue("URL should contain parameters", url.contains("?"));
        
        // 验证 URL 结构
        String[] parts = url.split("\\?");
        assertEquals(2, parts.length);
        assertTrue("URL path should end with /", parts[0].endsWith("/"));
        
        // 验证包含参数
        String params = parts[1];
        assertTrue("Should contain characterSetResults", params.contains("characterSetResults"));
    }
    
    @Test
    public void testGetURLStructure() {
        String url = MoConfUtil.getURL();
        assertNotNull(url);
        
        // 验证基本结构: jdbc:mysql://host:port/database?params
        String withoutProtocol = url.substring("jdbc:mysql://".length());
        assertTrue("Should contain server info", !withoutProtocol.isEmpty());
        
        // 验证数据库部分（即使是空的）
        assertTrue("Should have database separator", url.contains("/"));
        
        // 验证参数部分
        assertTrue("Should have query parameters", url.contains("?"));
    }
    
    @Test
    public void testConfigurationConsistency() {
        // 测试配置的一致性
        String driver = MoConfUtil.getDriver();
        String username = MoConfUtil.getUserName();
        String password = MoConfUtil.getUserpwd();
        
        assertNotNull("Driver should be configured", driver);
        assertNotNull("Username should be configured", username);
        assertNotNull("Password should be configured", password);
        
        // 验证所有配置项都已正确加载
        String url = MoConfUtil.getURL();
        assertNotNull("URL should be generated", url);
    }
    
    @Test
    public void testMultipleAccessConsistency() {
        // 多次访问应该返回相同结果（单例模式）
        String driver1 = MoConfUtil.getDriver();
        String driver2 = MoConfUtil.getDriver();
        assertEquals("Driver should be consistent", driver1, driver2);
        
        String url1 = MoConfUtil.getURL();
        String url2 = MoConfUtil.getURL();
        assertEquals("URL should be consistent", url1, url2);
    }
}
