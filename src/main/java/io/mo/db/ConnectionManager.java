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
    
    private static String sysuser = MoConfUtil.getSysUserName();
    private static String syspass = MoConfUtil.getSyspwd();

    private static Connection[] connections = new Connection[COMMON.DEFAULT_CONNECTION_NUM];
    private static Connection sysconn = null;
    
    private static final Logger LOG = Logger.getLogger(ConnectionManager.class.getName());
    private static boolean server_up = true;

    public static Connection getConnection(){
        return getConnection(0);
    }

    public static Connection getConnection(int index){

        //if mo server crash,return null;
        if(!server_up) return null;

        //get db connection,if failed,retry 3 times 10 s interval 
        //for(int i = 0; i < 3; i++) {
            try {
                Class.forName(driver);
                if (connections[index] == null || connections[index].isClosed()) {
                    connections[index] = DriverManager.getConnection(jdbcURL, userName, pwd);
                    return connections[index];
                }
                return connections[index];
            } catch (SQLException e) {
                //LOG.error("The mo-tester can not get valid conneciton from mo with[user="+userName+", pwd="+pwd+"], and will wait 10 seconds and retry...");
                LOG.error(e.getMessage());
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        //}
        
        LOG.error("The mo-tester can not get valid conneciton from mo with[user="+userName+", pwd="+pwd+"], the following cases wil not be executed!");
        server_up = false;
        return null;
    }

    public static Connection getConnection(int index, String userName, String pwd){
        //if mo server crash,return null;
        if(!server_up) return null;

        //get db connection,if failed,retry 3 times 10 s interval 
        //for(int i = 0; i < 3; i++) {
            try {
                Class.forName(driver);
                if (connections[index] == null || connections[index].isClosed()) {
                    connections[index] = DriverManager.getConnection(jdbcURL, userName, pwd);
                    LOG.debug("New conneciton from mo with[user="+userName+", pwd="+pwd+"] has been initialized.");
                    return connections[index];
                }
                return connections[index];
            } catch (SQLException e) {
                //LOG.error("The mo-tester can not get valid conneciton from mo with[user="+userName+", pwd="+pwd+"], and will wait 10 seconds and retry...");
                LOG.error(e.getMessage());
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        //}

        LOG.error("The mo-tester can not get valid conneciton from mo with[user="+userName+", pwd="+pwd+"], the following cases wil not be executed!");
        server_up = false;
        return null;
    }
    
    public static Connection getConnectionForSys(){
        //if mo server crash,return null;
        if(!server_up) return null;

        //get db connection,if failed,retry 3 times 10 s interval 
        //for(int i = 0; i < 3; i++) {
            try {
                Class.forName(driver);
                if (sysconn == null || sysconn.isClosed()) {
                    sysconn = DriverManager.getConnection(jdbcURL, sysuser, syspass);
                }
                return sysconn;
            } catch (SQLException e) {
                //LOG.error("The mo-tester can not get valid conneciton from mo with[user="+userName+", pwd="+pwd+"] for sys user, and will wait 10 seconds and retry...");
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        //}

        LOG.error("The mo-tester can not get valid conneciton from mo with[user="+userName+", pwd="+pwd+"], the following cases wil not be executed!");
        server_up = false;
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
