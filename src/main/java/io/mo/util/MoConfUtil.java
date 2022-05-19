package io.mo.util;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MoConfUtil {
    private static YamlUtil mo_conf = new YamlUtil();
    private static Map conf = null;

    public static void init(){
        try {
            conf = mo_conf.getInfo("mo.yml");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    public static String getURL(){

        if(conf == null) init();

        String URL = "jdbc:mysql://";
        Map jdbc = (Map)conf.get("jdbc");
        List gates = (ArrayList)jdbc.get("server");

        for(int i = 0; i < gates.size();i++){
            Map gate = (Map)gates.get(i);
            URL += gate.get("addr");
            if(i < gates.size() - 1)
                URL += ",";
            else
                URL += "/";
        }

        URL += getDefaultDatabase()+"?";

        Map paras = (Map)jdbc.get("paremeter");
        Iterator it_para_key = paras.keySet().iterator();
        Iterator it_para_value = paras.entrySet().iterator();
        while(it_para_value.hasNext()){
            URL += it_para_value.next();
            if(it_para_value.hasNext())
                URL += "&";
        }

        return URL;

    }

    public static String getDriver(){
        if(conf == null) init();

        Map jdbc = (Map)conf.get("jdbc");
        String driver = jdbc.get("driver").toString();
        return driver;
    }

    public static String getUserName(){
        if(conf == null) init();

        Map user = (Map)conf.get("user");
        String name = user.get("name").toString();
        return name;
    }

    public static String getUserpwd(){
        if(conf == null) init();

        Map user = (Map)conf.get("user");
        String pwd = user.get("passwrod").toString();
        return pwd;
    }

    public static String getDefaultDatabase(){
        if(conf == null) init();

        Map jdbc = (Map)conf.get("jdbc");
        Map database = (Map)jdbc.get("database");
        String def = database.get("default").toString();
        return def;
    }



    public static void main(String[] args){
        System.out.println(getDriver());
        System.out.println(getURL());

    }
}
