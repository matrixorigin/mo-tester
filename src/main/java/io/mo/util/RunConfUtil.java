package io.mo.util;

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

    public static void main(String[] args){
        System.out.println(getPath());
        System.out.println(getMethod());
    }
}
