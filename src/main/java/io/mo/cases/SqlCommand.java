package io.mo.cases;

import io.mo.result.StmtResult;
import io.mo.result.TestResult;
import io.mo.util.MoConfUtil;

import java.awt.image.AreaAveragingScaleFilter;
import java.lang.StringBuffer;
import java.util.ArrayList;

public class SqlCommand {

    private String id;
    private StringBuffer command;
    private boolean ignore = false;
    private boolean error = false;
    private int conn_id = 0;

    private String useDB = null;

    private String conn_user = null;
    private String conn_pswd = null;
    private String delimiter;
    private String issueNo = null;

    //column separator in result file for this command,can be 3 values:
    //     * 1、table,separator is \t
    //     * 2、space,separator is 4 spaces
    //     * 3、both,separator is \t or 4 spaces
    //default value is both
    private String separator = "both";
    private String scriptFile;
    private int position = 0;
    private ArrayList<Integer> sortKeyIndexs = new ArrayList<>();
    
    private ArrayList<String> syscmds = new ArrayList<>();

    private TestResult testResult;
    private StmtResult expResult;
    private StmtResult actResult;

    private SqlCommand next;
    
    private int sleeptime = 0;

    private boolean regularMatch = false;

    public SqlCommand(){
        command = new StringBuffer();
        testResult = new TestResult();
    }

    public void append(String command){
        this.command.append(command);
    }
    
    public void deleteCharAt(int len){
        this.command.deleteCharAt(len);
    }
    
    public void addSysCMD(String cmd){
        this.syscmds.add(cmd);
    }
    
    public ArrayList<String> getSysCMDS(){
        return this.syscmds;
    }
    
    public int size(){
        return this.command.length();
    }
    
    public void trim(){
        if(this.command.length() == 0)
            return;
        
        if(this.command.charAt(this.command.length() - 1) == ' ' || 
           this.command.charAt(this.command.length() - 1) == '\n'){
           this.command.deleteCharAt(this.command.length() - 1);
        }
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
    
    public int getConn_id() {
        return conn_id;
    }

    public void setConn_id(int conn_id) {
        this.conn_id = conn_id;
    }

    public String getConn_user() {
        if(conn_user == null)
            return MoConfUtil.getUserName();
        return conn_user;
    }
    public String getConn_pswd() {
        if(conn_pswd == null)
            return MoConfUtil.getUserpwd();
        return conn_pswd;
    }
    public String getScriptFile() {
        return scriptFile;
    }

    public void setScriptFile(String scriptFile) {
        this.scriptFile = scriptFile;
    }

    public TestResult getTestResult() {
        return testResult;
    }

    public void setTestResult(TestResult testResult) {
        this.testResult = testResult;
    }

    public boolean isIgnore() {
        return ignore;
    }

    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public SqlCommand getNext() {
        return next;
    }

    public void setNext(SqlCommand next) {
        this.next = next;
    }

    public ArrayList<Integer> getSortKeyIndexs(){
        return sortKeyIndexs;
    }

    public void addSortKeyIndex(int index){
        sortKeyIndexs.add(index);
    }

    public StmtResult getExpResult() {
        return expResult;
    }

    public void setExpResult(StmtResult expResult) {
        this.expResult = expResult;
        this.testResult.setExpResult(expResult.toString());
    }
    
    public StmtResult getActResult() {
        return actResult;
    }

    public void setActResult(StmtResult actResult) {
        this.actResult = actResult;
        this.testResult.setActResult(actResult.toString());
    }

    public boolean checkResult(){
        if(this.regularMatch)
            return expResult.regularMatch(actResult);
        
        return expResult.equals(actResult);
    }
    public String getSeparator() {
        return separator;
    }
    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public String getIssueNo() {
        return issueNo;
    }

    public void setIssueNo(String issueNo) {
        this.issueNo = issueNo;
    }


    public void setConn_user(String conn_user) {
        this.conn_user = conn_user;
    }

    public void setConn_pswd(String conn_pswd) {
        this.conn_pswd = conn_pswd;
    }
    
    public void sleep(){
        if(sleeptime == 0)
            return;
        else {
            try {
                Thread.sleep(sleeptime*1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }



    public int getSleeptime() {
        return sleeptime;
    }

    public void setSleeptime(int sleeptime) {
        this.sleeptime = sleeptime;
    }



    public String getUseDB() {
        return useDB;
    }

    public void setUseDB(String useDB) {
        this.useDB = useDB;
    }



    public boolean isRegularMatch() {
        return regularMatch;
    }

    public void setRegularMatch(boolean regularMatch) {
        this.regularMatch = regularMatch;
    }

}
