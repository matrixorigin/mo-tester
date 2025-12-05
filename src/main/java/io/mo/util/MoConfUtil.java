package io.mo.util;

import io.mo.constant.COMMON;

import java.util.List;
import java.util.Map;

/**
 * MO配置工具类
 */
public class MoConfUtil extends BaseConfigUtil {
    private static final MoConfUtil INSTANCE = new MoConfUtil();
    
    private MoConfUtil() {
        super("mo.yml");
    }
    
    private static MoConfUtil getInstance() {
        return INSTANCE;
    }
    
    /**
     * 构建JDBC URL
     */
    public static String getURL() {
        MoConfUtil u = getInstance();
        Map<String, Object> jdbc = u.map("jdbc");
        if (jdbc == null) return null;
        
        StringBuilder url = new StringBuilder("jdbc:mysql://");
        
        // 构建服务器地址列表
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> servers = (List<Map<String, Object>>) jdbc.get("server");
        if (servers != null && !servers.isEmpty()) {
            for (int i = 0; i < servers.size(); i++) {
                String addr = u.str(servers.get(i), "addr");
                if (addr != null) {
                    url.append(addr).append(i < servers.size() - 1 ? "," : "/");
                }
            }
        }
        
        url.append(getDefaultDatabase()).append("?");
        
        // 添加参数
        Map<String, Object> params = u.map(jdbc, "paremeter");
        if (params != null && !params.isEmpty()) {
            boolean first = true;
            for (Map.Entry<String, Object> e : params.entrySet()) {
                if (!first) url.append("&");
                url.append(e.getKey()).append("=").append(e.getValue());
                first = false;
            }
        }
        
        return url.toString();
    }
    
    public static String getDriver() {
        return getInstance().str("jdbc", "driver");
    }
    
    public static String getUserName() {
        return getInstance().str("user", "name");
    }
    
    public static String getUserpwd() {
        return getInstance().str("user", "password");
    }
    
    public static String getSysUserName() {
        return getInstance().str("user", "sysuser");
    }
    
    public static String getSyspwd() {
        return getInstance().str("user", "syspass");
    }
    
    public static String getDefaultDatabase() {
        return getInstance().str("jdbc", "database", "default");
    }
    
    public static int getSocketTimeout() {
        Integer timeout = getInstance().integer("jdbc", "paremeter", "socketTimeout");
        return timeout != null ? timeout : COMMON.DEFAULT_MAX_EXECUTE_TIME;
    }
    
    public static String[] getDebugServers() {
        return getInstance().split(getInstance().str("debug", "serverIP"), ",");
    }
    
    public static int getDebugPort() {
        Integer port = getInstance().integer("debug", "port");
        return port != null ? port : 0;
    }
    
    public static void main(String[] args) {
        System.out.println(getDriver());
        System.out.println(getURL());
    }
}
