package io.mo.cases;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import io.mo.constant.RESULT;
import io.mo.stream.TopicAndRecords;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestScript {

    private ArrayList<SqlCommand> commands = new ArrayList<>();
    private ArrayList<SqlCommand> successCommands = new ArrayList<>();
    private ArrayList<SqlCommand> failedCommands = new ArrayList<>();
    private ArrayList<SqlCommand> ignoredCommands = new ArrayList<>();
    private ArrayList<SqlCommand> abnormalCommands = new ArrayList<>();
    
    private Map<Integer, TopicAndRecords> produceRecords = new HashMap<Integer,TopicAndRecords>();

    private String fileName;

    private String useDB;

    private String id;

    private float duration = 0;

    private boolean skiped = false;

    public TestScript(){
        
    }

    public void addCommand(SqlCommand command){
        if(commands.size() != 0)
            commands.get(commands.size() - 1).setNext(command);
        commands.add(command);
        command.setScriptFile(fileName);
        command.setUseDB(this.useDB);
    }
    
    public void addSuccessCmd(SqlCommand command) { successCommands.add(command); }
    public void addFailedCmd(SqlCommand command){
        failedCommands.add(command);
    }
    public void addAbnoramlCmd(SqlCommand command){
        abnormalCommands.add(command);
    }
    public void addIgnoredCmd(SqlCommand command) { ignoredCommands.add(command); }

    public void invalid(){
        abnormalCommands = commands;
        for(SqlCommand command : abnormalCommands){
            command.getTestResult().setResult(RESULT.RESULT_TYPE_ABNORMAL);
            command.getTestResult().setErrorCode(RESULT.ERROR_PARSE_RESULT_FILE_FAILED_CODE);
            command.getTestResult().setErrorDesc(RESULT.ERROR_PARSE_RESULT_FILE_FAILED_DESC);
            command.getTestResult().setActResult(RESULT.ERROR_PARSE_RESULT_FILE_FAILED_DESC);
            command.getTestResult().setExpResult(RESULT.ERROR_PARSE_RESULT_FILE_FAILED_DESC);
        }
    }

    public String getScript(){
        StringBuilder script = new StringBuilder();
        for (SqlCommand command : commands) {
            script.append(command.getCommand());
        }
        return script.toString();
    }
    
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
        File file = new File(fileName);
        useDB = file.getName().substring(0,file.getName().lastIndexOf("."));
    }
    

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getTotalCmdCount() { return commands.size(); }
    public int getSuccessCmdCount() { return successCommands.size(); }
    public int getFailedCmdCount(){
        return failedCommands.size();
    }
    public int getIgnoredCmdCount(){
        return ignoredCommands.size();
    }
    public int getAbnormalCmdCount(){
        return abnormalCommands.size();
    }
    public ArrayList<SqlCommand>  getCommands(){
        return commands;
    }
    public String getCommand(int i){
        return commands.get(i).getCommand();
    }
    public List<SqlCommand> getSuccessCmdList() { return successCommands; }
    public List<SqlCommand> getFailedCmdList(){
        return failedCommands;
    }
    public List<SqlCommand> getIgnoredCmdList() { return ignoredCommands;}
    public List<SqlCommand> getAbnormalCmdList(){
        return abnormalCommands;
    }


    public String getUseDB() {
        return useDB;
    }

    public void setUseDB(String useDB) {
        this.useDB = useDB;
    }


    public boolean isSkiped() {
        return skiped;
    }

    public void setSkiped(boolean skiped) {
        this.skiped = skiped;
    }
    
    public boolean isKafkaProduceCmd(int pos){
        return produceRecords.containsKey(pos);
    }
    
    public void addKafkaProduceRecord(int pos, TopicAndRecords tar){
        produceRecords.put(pos,tar);
    }
    
    public TopicAndRecords getTopicAndRecord(int pos){
        return produceRecords.get(pos);
    }
}
