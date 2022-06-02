package io.mo.db;

import io.mo.constant.COMMON;
import io.mo.util.MoConfUtil;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionManager {

    private static String jdbcURL = MoConfUtil.getURL();
    private static String userName = MoConfUtil.getUserName();
    private static String pwd = MoConfUtil.getUserpwd();
    private static String driver = MoConfUtil.getDriver();

    private static Connection[] connections = new Connection[COMMON.DEFAULT_CONNECTION_NUM];
    private static Logger LOG = Logger.getLogger(Executor.class.getName());
    private static boolean server_up = true;

    static {
        try {
            Class.forName(driver);
            connections[0] = DriverManager.getConnection(jdbcURL, userName, pwd);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection(){
        //if mo server crash,return null;
        if(!server_up) return null;

        try {
            Class.forName(driver);
            if(connections[0] == null || connections[0].isClosed()){
                connections[0] = DriverManager.getConnection(jdbcURL, userName, pwd);
                return connections[0];
            }
            return connections[0];
        } catch (SQLException e) {
            //e.printStackTrace();
            LOG.error("The mo-tester can not get valid conneciton from mo,will wait 30 seconds,and retry one time ...");
            try {
                Thread.sleep(30000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            try {
                connections[0] = DriverManager.getConnection(jdbcURL, userName, pwd);
            } catch (SQLException ex) {
                LOG.error("The mo-tester still can not get valid conneciton from mo,the following cases wil not be executed!");
                server_up = false;
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Connection getConnection(int i){

        try {
            if(connections[i] == null){
                connections[i] = DriverManager.getConnection(jdbcURL, userName, pwd);
                return connections[i];
            }
            return connections[i];
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Connection getConnection(int i,String userName, String pwd){
        try {
            if(connections[i] == null){
                connections[i] = DriverManager.getConnection(jdbcURL, userName, pwd);
                return connections[i];
            }
            return connections[i];
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void reset(){
        for(int i = 1; i < connections.length;i++){
            if(connections[i] != null){
                try {
                    connections[i].close();
                    connections[i] = null;
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
