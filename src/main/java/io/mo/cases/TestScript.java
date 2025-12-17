package io.mo.cases;

import io.mo.constant.RESULT;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TestScript {

    @Getter
    private ArrayList<SqlCommand> commands = new ArrayList<>();
    
    @Getter
    private ArrayList<SqlCommand> successCommands = new ArrayList<>();
    
    @Getter
    private ArrayList<SqlCommand> failedCommands = new ArrayList<>();
    
    @Getter
    private ArrayList<SqlCommand> ignoredCommands = new ArrayList<>();
    
    @Getter
    private ArrayList<SqlCommand> abnormalCommands = new ArrayList<>();

    // 保留自定义 setter，因为有额外逻辑（设置 useDB）
    @Getter
    private String fileName;
    
    @Getter
    @Setter
    private String useDB;
    
    @Getter
    @Setter
    private float duration = 0;
    
    @Getter
    @Setter
    private boolean skiped = false;
    
    @Getter
    @Setter
    private Boolean compareMeta = null; // Document-level meta comparison flag (null means use global default)

    public TestScript() {
    }

    public void addCommand(SqlCommand command) {
        if (commands.size() != 0)
            commands.get(commands.size() - 1).setNext(command);
        commands.add(command);
        command.setScriptFile(this.fileName);
        command.setUseDB(this.useDB);
        command.setTestScript(this); // Set reference to TestScript
    }

    public void addSuccessCmd(SqlCommand command) {
        successCommands.add(command);
    }

    public void addFailedCmd(SqlCommand command) {
        failedCommands.add(command);
    }

    public void addAbnoramlCmd(SqlCommand command) {
        abnormalCommands.add(command);
    }

    public void addIgnoredCmd(SqlCommand command) {
        ignoredCommands.add(command);
    }

    public void invalid() {
        abnormalCommands = commands;
        for (SqlCommand command : abnormalCommands) {
            command.getTestResult().setResult(RESULT.RESULT_TYPE_ABNORMAL);
            command.getTestResult().setActResult(RESULT.ERROR_PARSE_RESULT_FILE_FAILED_DESC);
            command.getTestResult().setExpResult(RESULT.ERROR_PARSE_RESULT_FILE_FAILED_DESC);
        }
    }

    public String getScript() {
        StringBuilder script = new StringBuilder();
        for (SqlCommand command : commands) {
            script.append(command.getCommand());
        }
        return script.toString();
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
        File file = new File(fileName);
        this.setUseDB(file.getName().substring(0, file.getName().lastIndexOf(".")));
    }

    public int getTotalCmdCount() {
        return commands.size();
    }

    public int getSuccessCmdCount() {
        return successCommands.size();
    }

    public int getFailedCmdCount() {
        return failedCommands.size();
    }

    public int getIgnoredCmdCount() {
        return ignoredCommands.size();
    }

    public int getAbnormalCmdCount() {
        return abnormalCommands.size();
    }

    public String getCommand(int i) {
        return commands.get(i).getCommand();
    }

    public List<SqlCommand> getSuccessCmdList() {
        return successCommands;
    }

    public List<SqlCommand> getFailedCmdList() {
        return failedCommands;
    }

    public List<SqlCommand> getIgnoredCmdList() {
        return ignoredCommands;
    }

    public List<SqlCommand> getAbnormalCmdList() {
        return abnormalCommands;
    }

}
