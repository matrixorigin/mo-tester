package io.mo.cases;

import io.mo.constant.COMMON;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TestScript {

    private ArrayList<SqlCommand> commands = new ArrayList<SqlCommand>();
    private ArrayList<SqlCommand> errorcmds = new ArrayList<SqlCommand>();
    private ArrayList<SqlCommand> noexeccmds = new ArrayList<SqlCommand>();

    private String fileName;
    private String id;

    private boolean executed = true;

    private float duration = 0;

    public TestScript(){

    }

    public void addCommand(SqlCommand command){
        commands.add(command);
        command.setScriptFile(fileName);
    }

    public void addCommand(String command){
        SqlCommand sqlCommand = new SqlCommand();
        sqlCommand.append(command);
        commands.add(sqlCommand);
    }

    public void addErrorCmd(SqlCommand command){
        errorcmds.add(command);
    }
    public void addNoExecCmd(SqlCommand command){
        noexeccmds.add(command);
    }

    public int getSize(){
        int count = 0;
        for(int i = 0;i < commands.size();i++){
            if(!commands.get(i).isIgnore())
                count++;
        }

        return  count;
    }

    public ArrayList<SqlCommand>  getCommands(){
        return commands;
    }

    public String getCommand(int i){
        return commands.get(i).getCommand();
    }

    public String getScript(){
        String script = "";
        for(int i = 0;i < commands.size();i++){
            script += commands.get(i).getCommand();
        }
        return script;
    }

    public void print(){
        for(int i = 0; i < commands.size();i++){
            commands.get(i).print();
        }
    }


    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setExecStatus(boolean status){
        this.executed = status;
    }

    public boolean getExecStatus(){
        return this.executed;
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

    public int getErrorCount(){
        return errorcmds.size();
    }

    public List<SqlCommand> getErrorList(){
        return errorcmds;
    }

    public List<SqlCommand> getNoExecList(){
        return noexeccmds;
    }


}
