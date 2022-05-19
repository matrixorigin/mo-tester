package io.mo.cases;

import io.mo.constant.COMMON;
import io.mo.constant.ESCAPE;
import io.mo.result.TestResult;
import java.lang.StringBuffer;

import java.io.UnsupportedEncodingException;

public class SqlCommand {

    private String id;

    private String golabId;

    private TestResult result;

    private StringBuffer command;


    private boolean update = false;

    private int conn_id = 0;
    private String conn_user = null;
    private String conn_pswd = null;


    private String delimiter;

    private String scriptFile;

    public SqlCommand getNext() {
        return next;
    }

    public void setNext(SqlCommand next) {
        this.next = next;
    }

    private SqlCommand next;

    public SqlCommand(){
        this.delimiter = COMMON.DEFAUT_DELIMITER;
        command = new StringBuffer();
        result = new TestResult();
    }

    public SqlCommand(String id){
        this.id = id;
        this.delimiter = COMMON.DEFAUT_DELIMITER;
        result = new TestResult();
    }

    public SqlCommand(String id,StringBuffer command){
        this.command = command;
        this.id = id;
        this.delimiter = COMMON.DEFAUT_DELIMITER;
        check();
        result = new TestResult();
    }

    public void append(String command){
        this.command.append(command);
        this.command.append(COMMON.LINE_SEPARATOR);
        check();
    }

    private void check() {
        String command = this.command.toString();
        String opt = command.split(" ")[0];
        if (opt.equalsIgnoreCase("select") || opt.equalsIgnoreCase("show"))
            this.update = false;
        else
            this.update = true;

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCommand() {
        if(command.length() == 0)
            return null;
        return command.toString();
    }

    public void setCommand(StringBuffer command) {
        this.command = command;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }


    public int getConn_id() {
        return conn_id;
    }

    public void setConn_id(int conn_id) {
        this.conn_id = conn_id;
    }

    public String getConn_user() {
        return conn_user;
    }

    public void setConn_user(String conn_user) {
        try {
            this.conn_user = ESCAPE.parse(new String(conn_user.getBytes("utf-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public String getConn_pswd() {
        return conn_pswd;
    }

    public void setConn_pswd(String conn_pswd) {
        this.conn_pswd = conn_pswd;
    }


    public boolean isUpdate() {
        return update;
    }

    public String getScriptFile() {
        return scriptFile;
    }

    public void setScriptFile(String scriptFile) {
        this.scriptFile = scriptFile;
    }

    public String getGolabId() {
        return golabId;
    }

    public void setGolabId(String golabId) {
        this.golabId = golabId;
    }

    public TestResult getResult() {
        return result;
    }

    public void setResult(TestResult result) {
        this.result = result;
    }

    public void print(){
        if(id != null)
            System.out.println("----------command: id = "+id+"-------------");

        if(conn_id != 0) {
            System.out.println("----------command: conn_id = " + conn_id + "-------------");
            if(conn_user != null){
                System.out.println("----------command: user = " + conn_user + ",password = "+ conn_pswd +"-------------");
            }
        }
        System.out.println("----------command: delimiter = " + delimiter + "-------------");

        System.out.println(this.command);

        if(id != null)
            System.out.println("----------command: id = "+id+"-------------");
    }



}
