package io.mo.util;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * 配置工具基类，提供通用的配置加载和访问方法
 */
public abstract class BaseConfigUtil {
    private volatile Map<String, Object> config;
    private final String configFileName;
    
    protected BaseConfigUtil(String configFileName) {
        this.configFileName = configFileName;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYamlConfig() {
        Yaml yaml = new Yaml();
        
        // 优先从项目根目录加载配置文件
        java.nio.file.Path rootPath = Paths.get(System.getProperty("user.dir"), configFileName);
        if (Files.exists(rootPath)) {
            try {
                Map<?, ?> raw = yaml.load(Files.newInputStream(rootPath));
                return raw != null ? (Map<String, Object>) raw : null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        // 如果根目录不存在，尝试从 classpath 加载（向后兼容）
        URL url = BaseConfigUtil.class.getClassLoader().getResource(configFileName);
        if (url != null) {
            try {
                Map<?, ?> raw = yaml.load(Files.newInputStream(
                    Paths.get(URLDecoder.decode(url.getFile(), "utf-8"))));
                return raw != null ? (Map<String, Object>) raw : null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        return null;
    }
    
    private Map<String, Object> config() {
        if (config == null) {
            synchronized (this) {
                if (config == null) {
                    config = loadYamlConfig();
                }
            }
        }
        return config;
    }
    
    // 基础访问方法
    protected String str(String key) {
        return str(config(), key);
    }
    
    protected Integer integer(String key) {
        return integer(config(), key);
    }
    
    protected Map<String, Object> map(String key) {
        return map(config(), key);
    }
    
    // 从指定Map中获取值
    protected String str(Map<String, Object> map, String key) {
        Object val = get(map, key);
        return val != null ? val.toString() : null;
    }
    
    protected Integer integer(Map<String, Object> map, String key) {
        Object val = get(map, key);
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.parseInt(val.toString());
    }
    
    @SuppressWarnings("unchecked")
    protected Map<String, Object> map(Map<String, Object> map, String key) {
        Object val = get(map, key);
        return val instanceof Map ? (Map<String, Object>) val : null;
    }
    
    private Object get(Map<String, Object> map, String key) {
        return map != null ? map.get(key) : null;
    }
    
    // 嵌套路径访问（链式访问）
    protected String str(String... path) {
        return str(config(), path);
    }
    
    protected Integer integer(String... path) {
        return integer(config(), path);
    }
    
    protected Map<String, Object> map(String... path) {
        return map(config(), path);
    }
    
    private String str(Map<String, Object> map, String... path) {
        Object val = navigate(map, path);
        return val != null ? val.toString() : null;
    }
    
    private Integer integer(Map<String, Object> map, String... path) {
        Object val = navigate(map, path);
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.parseInt(val.toString());
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Map<String, Object> map, String... path) {
        Object val = navigate(map, path);
        return val instanceof Map ? (Map<String, Object>) val : null;
    }
    
    private Object navigate(Map<String, Object> map, String... path) {
        if (map == null || path == null || path.length == 0) return null;
        Object current = map;
        for (String key : path) {
            if (!(current instanceof Map)) return null;
            @SuppressWarnings("unchecked")
            Map<String, Object> currentMap = (Map<String, Object>) current;
            current = currentMap.get(key);
            if (current == null) return null;
        }
        return current;
    }
    
    // 工具方法
    protected String[] split(String value, String delimiter) {
        return value != null && !value.isEmpty() ? value.split(delimiter) : null;
    }
}

