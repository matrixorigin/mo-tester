package io.mo.util;

import io.mo.constant.COMMON;

import java.io.File;
import java.util.Map;

public class RunConfUtil {
    private static final YamlUtil run_conf = new YamlUtil();
    private static Map conf = null;


    public static void init(){
            conf = run_conf.getInfo("run.yml");
    }


    public static String getPath(){
        if(conf == null) init();
        return (String)conf.get("path");
    }

    public static String getMethod(){
        if(conf == null) init();
        return (String)conf.get("method");
    }

    public static int getRate(){
        if(conf == null) init();
        return (int)conf.get("rate");
    }

    public static int getWaitTime(){
        if(conf == null) init();
        return (int)conf.get("waittime");
    }

    public static String getResourcePath(){
        if(conf == null) init();
        String path  = (String)conf.get("path");
        File caseFile = new File(path);
        String srcPath = null;
        if(caseFile.getAbsolutePath().contains(COMMON.CASES_DIR)) {
            srcPath = caseFile.getAbsolutePath();
            srcPath = srcPath.replace(COMMON.CASES_DIR,COMMON.RESOURCE_DIR);
            srcPath = srcPath.substring(0,srcPath.indexOf(COMMON.RESOURCE_DIR)+COMMON.RESOURCE_DIR.length());
        }
        COMMON.RESOURCE_LOCAL_PATH = srcPath;
        return srcPath == null?COMMON.RESOURCE_PATH:srcPath;
    }
    
    public static String[] getBuiltinDb(){
        if(conf == null) init();
        String builtindbs  = (String)conf.get("builtindb");
        String[] dbs = builtindbs.split(",");
        return dbs;
    }
    
    public static String[] getOutFiles(){
        if(conf == null) init();
        String outfiles  = (String)conf.get("outfiles");
        String[] files = outfiles.split(",");
        return files;
    }

    public static void main(String[] args){
        System.out.println(getPath());
        System.out.println(getMethod());
    }
}
