package io.mo.db;

import io.mo.constant.COMMON;
import io.mo.processor.Executor;
import io.mo.util.MoConfUtil;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionPool {

    private   String jdbcURL = MoConfUtil.getURL();
    private  String userName = MoConfUtil.getUserName();
    private  String pwd = MoConfUtil.getUserpwd();
    private  String driver = MoConfUtil.getDriver();

    private  Connection[] connections = new Connection[COMMON.DEFAULT_CONNECTION_NUM];
    private  final Logger LOG = Logger.getLogger(ConnectionPool.class.getName());
    private  boolean server_up = true;
    
    public ConnectionPool(){

        try {
            Class.forName(driver);
            connections[0] = DriverManager.getConnection(jdbcURL, userName, pwd);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    
    public ConnectionPool(String userName,String pwd){
        this.userName = userName;
        this.pwd = pwd;
        
        try {
            Class.forName(driver);
            connections[0] = DriverManager.getConnection(jdbcURL, this.userName, this.pwd);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Connection getConnection(){
        return getConnection(0);
    }

    public Connection getConnection(int index){

        //if mo server crash,return null;
        if(!server_up) return null;

        //get db connection,if failed,retry 3 times 10 s interval 
        for(int i = 0; i < 3; i++) {
            try {
                Class.forName(driver);
                if (connections[index] == null || connections[index].isClosed()) {
                    connections[index] = DriverManager.getConnection(jdbcURL, userName, pwd);
                    return connections[index];
                }
                return connections[index];
            } catch (SQLException e) {
                LOG.error("The mo-tester can not get valid conneciton from mo with[user="+userName+", pwd="+pwd+"], and will wait 10 seconds and retry...");
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        LOG.error("The mo-tester still can not get valid conneciton from mo, the following cases wil not be executed!");
        server_up = false;
        return null;
    }

    public Connection getConnection(int index, String userName, String pwd){
        //if mo server crash,return null;
        if(!server_up) return null;
        
        //get db connection,if failed,retry 3 times 10 s interval 
        for(int i = 0; i < 3; i++) {
            try {
                Class.forName(driver);
                if (connections[index] == null || connections[0].isClosed()) {
                    connections[index] = DriverManager.getConnection(jdbcURL, userName, pwd);
                    LOG.info("New conneciton from mo with[user="+userName+", pwd="+pwd+"] has been initialized.");
                    return connections[index];
                }
                return connections[index];
            } catch (SQLException e) {
                LOG.error("The mo-tester can not get valid conneciton from mo with[user="+userName+", pwd="+pwd+"], and will wait 10 seconds and retry...");
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        LOG.error("The mo-tester still can not get valid conneciton from mo, the following cases wil not be executed!");
        server_up = false;
        return null;
    }

    public void reset(){
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
