package io.mo.db;

import io.mo.constant.COMMON;
import io.mo.util.MoConfUtil;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionManager {

    private final String jdbcUrl;
    private final String driver;
    private final String sysUserName;
    private final String sysPassword;

    private final Connection[] connections;
    private Connection sysConnection;
    
    private final Logger logger;
    private boolean isServerUp;
    private String defaultUserName;
    private String defaultPassword;

    public ConnectionManager() {
        this.jdbcUrl = MoConfUtil.getURL();
        this.defaultUserName = MoConfUtil.getUserName();
        this.defaultPassword = MoConfUtil.getUserpwd();
        this.driver = MoConfUtil.getDriver();
        this.sysUserName = MoConfUtil.getSysUserName();
        this.sysPassword = MoConfUtil.getSyspwd();
        this.connections = new Connection[COMMON.DEFAULT_CONNECTION_NUM];
        this.logger = Logger.getLogger(ConnectionManager.class.getName());
        this.isServerUp = true;
    }

    // 可以复用已有的 ConnectionManager 无参构造函数，仅修改 defaultUserName 和 defaultPassword
    public ConnectionManager(String userName, String password) {
        this(); // 调用无参构造函数进行初始化
        this.defaultUserName = userName;
        this.defaultPassword = password;
    }

    public Connection getConnection() {
        return getConnection(0);
    }

    public Connection getConnection(int index) {
        return getConnection(index, defaultUserName, defaultPassword);
    }

    public Connection getConnection(int index, String userName, String password) {
        if (!isServerUp) {
            return null;
        }

        try {
            Class.forName(driver);
            Connection conn ;
            if (index == -1) {
                conn = sysConnection;
            } else {
                conn = connections[index];
            }
            if (conn == null || conn.isClosed()) {
                conn = DriverManager.getConnection(jdbcUrl, userName, password);
                if (!userName.equals(defaultUserName) || !password.equals(defaultPassword)) {
                    logger.debug("New connection from mo with[user=" + userName + ", pwd=" + password + "] has been initialized.");
                }
            }
            if (index == -1) {
                sysConnection = conn;
            } else {
                connections[index] = conn;
            }
            return conn;
        } catch (SQLException e) {
            logger.error(e.getMessage());
            handleConnectionError(userName, password);
        } catch (ClassNotFoundException e) {
            logger.error("Driver class not found: " + driver, e);
            e.printStackTrace();
        }
        
        return null;
    }
    
    public Connection getConnectionForSys() {
        return getConnection(-1, sysUserName, sysPassword);
    }

    private void handleConnectionError(String userName, String password) {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }
        logger.error("The mo-tester can not get valid connection from mo with[user=" + userName + ", pwd=" + password + "], the following cases will not be executed!");
        isServerUp = false;
    }

    public void reset() {
        for (int i = 1; i < connections.length; i++) {
            if (connections[i] != null) {
                try {
                    connections[i].close();
                    connections[i] = null;
                } catch (SQLException e) {
                    logger.warn("Failed to close connection at index " + i + ": " + e.getMessage());
                }
            }
        }
    }
}
