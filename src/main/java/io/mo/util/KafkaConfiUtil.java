package io.mo.util;

import java.util.Map;

public class KafkaConfiUtil {
    private static final YamlUtil mo_conf = new YamlUtil();
    private static Map conf = null;

    public static void init(){
        conf = mo_conf.getInfo("kafka.yml");
    }
    
    public static String getServerAddr(){
        if(conf == null) init();
        String addr = (String)conf.get("server");
        if(addr == null || addr.equalsIgnoreCase(""))
            return null;
        
        return addr;
    }

    public static String getTopic(){
        if(conf == null) init();
        String topic = (String)conf.get("topic");
        if(topic == null || topic.equalsIgnoreCase(""))
            return null;

        return topic;
    }

    public static String getClientId(){
        if(conf == null) init();
        String clientId = (String)conf.get("client_id");
        if(clientId == null || clientId.equalsIgnoreCase(""))
            return null;

        return clientId;
    }
    
    public static void main(String args[]){
        System.out.println(getServerAddr());
        System.out.println(getTopic());
    }
}
