package io.mo.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MoConfUtil {
    private static final YamlUtil mo_conf = new YamlUtil();
    private static Map conf = null;

    public static void init(){
            conf = mo_conf.getInfo("mo.yml");
    }


    public static String getURL(){

        if(conf == null) init();

        StringBuilder URL = new StringBuilder("jdbc:mysql://");
        Map jdbc = (Map)conf.get("jdbc");
        List gates = (ArrayList)jdbc.get("server");

        for(int i = 0; i < gates.size();i++){
            Map gate = (Map)gates.get(i);
            URL.append(gate.get("addr"));
            if(i < gates.size() - 1)
                URL.append(",");
            else
                URL.append("/");
        }

        URL.append(getDefaultDatabase()).append("?");

        Map paras = (Map)jdbc.get("paremeter");
        Iterator it_para_value = paras.entrySet().iterator();
        while(it_para_value.hasNext()){
            URL.append(it_para_value.next());
            if(it_para_value.hasNext())
                URL.append("&");
        }

        return URL.toString();
    }

    public static String getDriver(){
        if(conf == null) init();

        Map jdbc = (Map)conf.get("jdbc");
        return jdbc.get("driver").toString();
    }

    public static String getUserName(){
        if(conf == null) init();

        Map user = (Map)conf.get("user");
        return user.get("name").toString();
    }

    public static String getUserpwd(){
        if(conf == null) init();

        Map user = (Map)conf.get("user");
        return user.get("password").toString();
    }

    public static String getDefaultDatabase(){
        if(conf == null) init();

        Map jdbc = (Map)conf.get("jdbc");
        Map database = (Map)jdbc.get("database");
        return database.get("default").toString();
    }



    public static void main(String[] args){
        System.out.println(getDriver());
        System.out.println(getURL());

    }
}
