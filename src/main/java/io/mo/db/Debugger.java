package io.mo.db;

import io.mo.cases.SqlCommand;
import io.mo.cases.TestCase;
import io.mo.cases.TestScript;
import io.mo.cases.TestSuite;
import io.mo.constant.COMMON;

import java.io.*;
import java.sql.Connection;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Debugger {
    private static PrintWriter logWriter;
    private static SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS");

    public static void run(TestScript script){

        initWriter(COMMON.LOG_PATH+"/"+format.format(new Date()),script.getId()+".log");
        ConnectionManager.reset();
        boolean hasResults = false;
        Statement statement = null;
        Connection connection = ConnectionManager.getConnection();

        String def_db = "debug";
        createTestDB(connection,def_db);

        ArrayList<SqlCommand> commands = script.getCommands();
        for (int j = 0; j < commands.size(); j++) {
            SqlCommand command = commands.get(j);
            connection = getConnection(command);
            try{
                statement = connection.createStatement();
                println("MySQL> "+command.getCommand());
                if (command.isUpdate()) {
                    int num = statement.executeUpdate(command.getCommand());
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
                continue;
            }
        }
        dropTestDB(connection,def_db);
        //script.print();
    }

    public static void run(String command) {

        initWriter(COMMON.LOG_PATH,format.format(new Date()));

        Connection connection = ConnectionManager.getConnection();

        boolean hasResults = false;
        Statement statement = null;
        try {
            statement = connection.createStatement();
            SqlCommand sqlCommand = new SqlCommand();
            sqlCommand.append(command);
            println("MySQL [kundb]> "+command);
            if(sqlCommand.isUpdate()){
                int num = statement.executeUpdate(sqlCommand.getCommand());
                String res = COMMON.UPDATE_RESULT_TEMPLATE.replace("{num}",String.valueOf(num));
                printWithLineSeperator(res);
            } else {
                hasResults = statement.execute(sqlCommand.getCommand());
                printResults(statement, hasResults);
            }
            statement.close();
        } catch (SQLException e) {
            printlnError(e.getMessage());
        }

        try {
            statement.close();
        } catch (Exception e) {
            printErrorWithLineSeperator(e.getMessage());
        }

    }


    private static void initWriter(String path,String name){
        //String dir = path.replace(".","/");
        String dir = path;
        File directories = new File(dir);
        if(!directories.exists())
            directories.mkdirs();

        try {
            logWriter = new PrintWriter(new FileWriter(new File(dir+ "/" + name )));
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
            }else {

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
            System.out.println("");
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
            }else {

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

    private static void startCase(String case_desc){
        if (logWriter != null) {
            logWriter.println("--------------------------------------case: "+case_desc+" start---------------------------------------\n");
            System.out.println("--------------------------------------case: "+case_desc+" start---------------------------------------\n");
            logWriter.flush();
        }
    }

    private static void finishCase(String case_desc){
        if (logWriter != null) {
            logWriter.println("--------------------------------------case: "+case_desc+" end-----------------------------------------\n");
            System.out.println("--------------------------------------case: "+case_desc+" end-----------------------------------------\n");
            logWriter.flush();
        }
    }

    private static void startPreSql(){
        if (logWriter != null) {
            logWriter.println("--------------------------------------preSql start---------------------------------------\n");
            System.out.println("--------------------------------------preSql start---------------------------------------\n");
            logWriter.flush();
        }
    }

    private static void finishPreSql(){
        if (logWriter != null) {
            logWriter.println("--------------------------------------preSql end-----------------------------------------\n");
            System.out.println("--------------------------------------preSql end-----------------------------------------\n");
            logWriter.flush();
        }
    }

    private static void startPostSql(){
        if (logWriter != null) {
            logWriter.println("--------------------------------------postSql start---------------------------------------\n");
            System.out.println("--------------------------------------postSql start---------------------------------------\n");
            logWriter.flush();
        }
    }

    private static void finishPostSql(){
        if (logWriter != null) {
            logWriter.println("--------------------------------------postSql end-----------------------------------------\n");
            System.out.println("--------------------------------------postSql end-----------------------------------------\n");
            logWriter.flush();
        }
    }


    public static Connection getConnection(SqlCommand command){
        Connection connection = null;
        if(command.getConn_id() != 0){
            if(command.getConn_user() == null){
                System.out.println(command.getConn_id());
                connection = ConnectionManager.getConnection(command.getConn_id());
                try {
                    if (isTrxBeginning(command)) {
                        connection.setAutoCommit(false);
                    }

                    if(isTrxEndding(command)){
                        connection.setAutoCommit(true);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

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

    public static boolean isTrxBeginning(SqlCommand command){
        if(command.getCommand().toLowerCase().startsWith(COMMON.START_TRX))
            return true;

        return false;
    }

    public static boolean isTrxEndding(SqlCommand command){
        if(command.getCommand().toLowerCase().startsWith(COMMON.COMMIT_TRX) || command.getCommand().toLowerCase().startsWith(COMMON.ROLLBACK_TRX))
            return true;

        return false;
    }

    public static  void createTestDB(Connection connection,String name){
        if(connection == null) return;
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.executeUpdate("create database IF NOT EXISTS `"+name+"`;");
            statement.executeUpdate("use `"+name+"`;");
        } catch (SQLException e) {
            System.out.println("create database "+name+"is failed.cause: "+e.getMessage());
        }
    }

    public static  void dropTestDB(Connection connection,String name){
        if(connection == null) return;
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.executeUpdate("drop database IF EXISTS `"+name+"`;");
        } catch (SQLException e) {
            //e.printStackTrace();
            System.out.println("drop database "+name+"is failed.cause: "+e.getMessage());
        }

    }

}
