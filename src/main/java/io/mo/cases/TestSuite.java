package io.mo.cases;

import io.mo.result.TestResult;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class TestSuite {

    private String fileName;
    private boolean executed = true;

    private ArrayList<SqlCommand> setupSqls = new ArrayList<SqlCommand>();
    private ArrayList<SqlCommand> teardownSqls = new ArrayList<SqlCommand>();
    private ArrayList<TestCase> cases = new ArrayList<TestCase>();

    public void addTestCase(TestCase _case){

        cases.add(_case);
    }

    public void setResult(TestResult result){
        for(int i = 0;i < cases.size();i++){
            cases.get(i).setResult(result);
        }
    }

    public ArrayList<SqlCommand> getSetupSqls() {
        return setupSqls;
    }

    public void setSetupSqls(ArrayList<SqlCommand> setupSqls) {
        this.setupSqls = setupSqls;
    }

    public ArrayList<SqlCommand> getTeardownSqls() {
        return teardownSqls;
    }

    public void setTeardownSqls(ArrayList<SqlCommand> teardownSqls) {
        this.teardownSqls = teardownSqls;
    }

    public ArrayList<TestCase> getCases() {
        return cases;
    }

    public void setCases(ArrayList<TestCase> cases) {
        this.cases = cases;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName.replaceAll(".sql",".log");
    }

    public void addSetupCommand(SqlCommand command){
        setupSqls.add(command);
    }

    public void addTeardownCommand(SqlCommand command){
        teardownSqls.add(command);
    }
    public void setExecStatus(boolean status){
        this.executed = status;
        for(int i = 0; i < cases.size();i++){
            cases.get(i).setExecStatus(status);
        }
    }

    public boolean getExecStatus(){
        return this.executed;
    }


    public void print(){
        System.out.println("----------preSQLCommands-------------");
        //preSqls.print();
        System.out.println("----------preSQLCommands-------------");

        System.out.print("\n\n");

        for(int i = 0; i < cases.size();i++){
            System.out.println("----------TestCases-------------");
            cases.get(i).print();
            System.out.print("\n\n");
            System.out.println("----------TestCases-------------");
        }

        System.out.print("\n\n");

        System.out.println("----------postSQLCommands-------------");
        //preSqls.print();
        System.out.println("----------postSQLCommands-------------");

    }


}
