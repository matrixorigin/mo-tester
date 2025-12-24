package io.mo.util;

import io.mo.constant.COMMON;

import java.io.File;

/**
 * 运行配置工具类
 */
public class RunConfUtil extends BaseConfigUtil {
    private static final RunConfUtil INSTANCE = new RunConfUtil();
    
    private RunConfUtil() {
        super("run.yml");
    }
    
    private static RunConfUtil getInstance() {
        return INSTANCE;
    }
    
    public static String getPath() {
        return getInstance().str("path");
    }
    
    public static String getMethod() {
        return getInstance().str("method");
    }
    
    public static int getRate() {
        Integer rate = getInstance().integer("rate");
        return rate != null ? rate : 0;
    }
    
    public static int getWaitTime() {
        Integer waitTime = getInstance().integer("waittime");
        return waitTime != null ? waitTime : 0;
    }
    
    /**
     * 根据测试用例路径推导并设置资源路径静态变量
     * 
     * 约定：resources 和 cases 目录必须位于同一层级
     * 约定高于配置：如果路径符合约定，自动推导；否则使用默认值
     * 
     * 推导规则：
     * 1. 如果用例路径包含 "cases" 目录，则自动推导对应的 "resources" 目录
     * 2. 推导方式：将路径中的 "cases" 替换为 "resources"，并截取到 resources 目录末尾
     * 3. 如果路径不符合约定，使用默认值 "./resources"
     * 
     * 示例：
     * - 用例路径: /project/cases/array/array.sql
     *   设置结果: COMMON.RESOURCE_PATH = "/project/resources"
     *   
     * - 用例路径: /home/test/cases/function/test.sql  
     *   设置结果: COMMON.RESOURCE_PATH = "/home/test/resources"
     *   
     * - 用例路径: ./test.sql (不包含 cases)
     *   设置结果: COMMON.RESOURCE_PATH = "./resources" (使用默认值)
     * 
     * @param casePath 测试用例文件路径（可为 null）
     */
    public static void setDerivedStaticResourcePath(String casePath) {
        if (casePath == null) {
            return;
        }
        
        File caseFile = new File(casePath);
        String absPath = caseFile.getAbsolutePath();
        String srcPath = null;
        
        // 约定：如果路径包含 cases 目录，则推导对应的 resources 目录
        if (absPath.contains(COMMON.CASES_DIR)) {
            srcPath = absPath.replace(COMMON.CASES_DIR, COMMON.RESOURCE_DIR);
            int idx = srcPath.indexOf(COMMON.RESOURCE_DIR);
            if (idx >= 0) {
                srcPath = srcPath.substring(0, idx + COMMON.RESOURCE_DIR.length());
            }
        }
        
        COMMON.RESOURCE_PATH = srcPath != null ? srcPath : COMMON.RESOURCE_PATH;
    }
    
    /**
     * 从配置文件读取路径并设置资源路径静态变量
     */
    public static void setStaticResourcePathFromConfig() {
        String path = getInstance().str("path");
        setDerivedStaticResourcePath(path);
    }
    
    public static String[] getBuiltinDb() {
        return getInstance().split(getInstance().str("builtindb"), ",");
    }
    
    public static String[] getOutFiles() {
        return getInstance().split(getInstance().str("outfiles"), ",");
    }
    
    public static void main(String[] args) {
        System.out.println(getPath());
        System.out.println(getMethod());
    }
}
