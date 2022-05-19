package io.mo.cases;

import io.mo.constant.RESULT;
import io.mo.result.TestResult;

import java.util.ArrayList;

public class TestCase {
    private String desc;
    private TestResult result;
    private String fileName;

    private TestSuite suite;

    private float duration = 0;

    private boolean executed = true;
    private StringBuffer remark = new StringBuffer();

    private ArrayList<SqlCommand> commands = new ArrayList<SqlCommand>();
    private ArrayList<String> labels = new ArrayList<String>();

    public TestCase(){
        result = new TestResult();
    }

    public ArrayList<SqlCommand> getCommands() {
        return commands;
    }

    public void setCommands(ArrayList<SqlCommand> commands) {
        this.commands = commands;
    }

    public ArrayList<String> getLabels() {
        return labels;
    }

    public void setLabels(ArrayList<String> labels) {
        this.labels = labels;
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



    public void addCommand(SqlCommand command){
        commands.add(command);
    }

    public void addLabel(String label){
        labels.add(label);
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getRemark() {
        return remark.toString();
    }

    public void addRemark(String remark) {
        this.remark.append(remark);

    }

    public TestResult getResult() {
        return result;
    }

    public void setResult(TestResult result) {
        this.result = result;
    }

    public TestSuite getSuite() {
        return suite;
    }

    public void setSuite(TestSuite suite) {
        this.suite = suite;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public String getSqlCommands(){
        StringBuffer buffer = new StringBuffer();
        for(int i = 0; i < suite.getSetupSqls().size();i++){
            buffer.append(suite.getSetupSqls().get(i).getCommand());
        }

        for(int i = 0; i < commands.size();i++){
            buffer.append(commands.get(i).getCommand());
        }

        return buffer.toString();
    }


    public String toString(){
        String _case = "CASE: ";
        _case += desc + "\t" + result.getResult();
        if(!result.getResult().equalsIgnoreCase(RESULT.RESULT_TYPE_PASS)){
            _case += "\n\t[ERROR DESC]: "+result.getErrorDesc();
            if(result.getErrorCode().equalsIgnoreCase(RESULT.ERROR_CHECK_FAILED_CODE)){
                _case += "\n\t[EXPECT]: "+result.getExpResult();
                _case += "\n\t[ACTUAL]: "+result.getActResult();
            }

            if(result.getRemark() != null){
                _case += "\n\t[REMARK]: "+result.getRemark();
            }
            _case += "\n";
        }
        return _case;
    }

    public void print(){
        System.out.println("----------case: desc = "+desc+"-------------");
    }


}
