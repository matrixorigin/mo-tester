package io.mo.db;

import io.mo.cases.SqlCommand;
import io.mo.cases.TestScript;
import io.mo.constant.COMMON;

import java.io.*;
import java.sql.Connection;
import java.sql.*;
import java.util.ArrayList;

public class Debugger {
    private static PrintWriter logWriter;

    public static void run(TestScript script){

        initWriter(COMMON.LOG_PATH+"/","debug.log");
        ConnectionManager.reset();
        boolean hasResults;
        Statement statement;
        Connection connection = ConnectionManager.getConnection();

        createTestDB(connection,script);

        ArrayList<SqlCommand> commands = script.getCommands();
        for (int j = 0; j < commands.size(); j++) {
            SqlCommand command = commands.get(j);
            connection = getConnection(command);
            try{
                statement = connection.createStatement();
                statement.execute(command.getCommand());
                ResultSet resultSet = statement.getResultSet();
                println("MySQL> "+command.getCommand());
                if (resultSet == null) {
                    int num = statement.getUpdateCount();
                    String res = COMMON.UPDATE_RESULT_TEMPLATE.replace("{num}", String.valueOf(num));
                    if(j < commands.size() -1 )
                        printWithLineSeperator(res+"\n");
                    else
                        println(res+"\n");
                } else {
                    hasResults = statement.execute(command.getCommand());
                    if(j < commands.size() -1 )
                        printResultsWithLineSeperator(statement, hasResults);
                    else
                        printResults(statement, hasResults);
                }
                statement.close();
            }catch (SQLException e) {
                printlnError(e.getMessage());
            }
        }
        dropTestDB(connection,script);
    }

    private static void initWriter(String path,String name){
        //String dir = path.replace(".","/");
        File directories = new File(path);
        if(!directories.exists())
            directories.mkdirs();

        try {
            logWriter = new PrintWriter(new FileWriter(path + "/" + name));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printResults(Statement statement, boolean hasResults) {
        try {
            if (hasResults) {
                ResultSet rs = statement.getResultSet();
                if (rs != null) {
                    ResultSetMetaData md = rs.getMetaData();
                    int cols = md.getColumnCount();

                    int i;
                    String value;
                    for(i = 0; i < cols; ++i) {
                        value = md.getColumnLabel(i + 1);
                        print(value + "\t");
                    }

                    println("");

                    while(rs.next()) {
                        for(i = 0; i < cols; ++i) {
                            value = rs.getString(i + 1);
                            print(value + "\t");
                        }

                        println("");
                    }
                }
            }
        } catch (SQLException var8) {
            printlnError("Error printing results: " + var8.getMessage());
        }
        //logWriter.close();

    }

    private static void print(Object o) {
        if (logWriter != null) {
            logWriter.print(o);
            logWriter.flush();
            System.out.print(o);
        }

    }

    private static void println(Object o) {
        if (logWriter != null) {
            logWriter.println(o);
            logWriter.flush();
            System.out.println(o);
        }

    }

    private static void printlnError(Object o) {
        if (logWriter != null) {
            logWriter.println(o);
            logWriter.println("");
            logWriter.flush();
            System.out.println(o);
            System.out.println();
        }
    }

    private static void printResultsWithLineSeperator(Statement statement, boolean hasResults) {
        try {
            if (hasResults) {
                ResultSet rs = statement.getResultSet();
                if (rs != null) {
                    ResultSetMetaData md = rs.getMetaData();
                    int cols = md.getColumnCount();

                    int i;
                    String value;
                    for(i = 0; i < cols; ++i) {
                        value = md.getColumnLabel(i + 1);
                        print(value + "\t");
                    }

                    println("");

                    while(rs.next()) {
                        for(i = 0; i < cols; ++i) {
                            value = rs.getString(i + 1);
                            print(value + "\t");
                        }

                        println("");
                    }
                }
                logWriter.println("------------------------------------------------------------------------------------\n");
                System.out.println("------------------------------------------------------------------------------------\n");
            }
        } catch (SQLException var8) {
            printErrorWithLineSeperator("Error printing results: " + var8.getMessage());
        }
        //logWriter.close();

    }

    private static void printWithLineSeperator(Object o) {
        if (logWriter != null) {
            logWriter.println(o);
            System.out.println(o);
            logWriter.println("------------------------------------------------------------------------------------\n");
            System.out.println("------------------------------------------------------------------------------------\n");
            logWriter.flush();
        }

    }

    private static void printErrorWithLineSeperator(Object o) {
        if (logWriter != null) {
            logWriter.println(o);
            System.out.println(o);
            logWriter.println("------------------------------------------------------------------------------------\n");
            System.out.println("------------------------------------------------------------------------------------\n");
            logWriter.flush();
        }
    }

    public static Connection getConnection(SqlCommand command){
        Connection connection;
        if(command.getConn_id() != 0){
            if(command.getConn_user() == null){
                System.out.println(command.getConn_id());
                connection = ConnectionManager.getConnection(command.getConn_id());
                return connection;
            }else {
                System.out.println(command.getConn_user()+"   "+command.getConn_id()+"   "+command.getConn_pswd());
                connection = ConnectionManager.getConnection(command.getConn_id(),command.getConn_user(),command.getConn_pswd());
                return connection;
            }
        }
        connection = ConnectionManager.getConnection();
        return connection;
    }

    public static  void createTestDB(Connection connection,String name){
        if(connection == null) return;
        Statement statement;
        try {
            statement = connection.createStatement();
            statement.executeUpdate("create database IF NOT EXISTS `"+name+"`;");
            statement.executeUpdate("use `"+name+"`;");
        } catch (SQLException e) {
            System.out.println("create database "+name+" is failed.cause: "+e.getMessage());
        }
    }

    public static  void createTestDB(Connection connection,TestScript script){
        File file = new File(script.getFileName());
        String dbName = file.getName().substring(0,file.getName().lastIndexOf("."));
        createTestDB(connection,dbName);
    }

    public static  void dropTestDB(Connection connection,String name){
        if(connection == null) return;
        Statement statement;
        try {
            statement = connection.createStatement();
            statement.executeUpdate("drop database IF EXISTS `"+name+"`;");
        } catch (SQLException e) {
            //e.printStackTrace();
            System.out.println("drop database "+name+" is failed.cause: "+e.getMessage());
        }
    }

    public static  void dropTestDB(Connection connection,TestScript script){
        File file = new File(script.getFileName());
        String dbName = file.getName().substring(0,file.getName().lastIndexOf("."));
        dropTestDB(connection,dbName);
    }

}
