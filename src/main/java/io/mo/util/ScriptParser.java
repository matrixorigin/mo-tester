package io.mo.util;

import io.mo.cases.SqlCommand;
import io.mo.cases.TestCase;
import io.mo.cases.TestScript;
import io.mo.cases.TestSuite;
import io.mo.constant.COMMON;
import io.mo.db.Executor;
import jdk.jfr.internal.LogLevel;
import org.apache.log4j.Logger;
import org.apache.poi.ss.formula.functions.T;

import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class ScriptParser {
    private static final String LINE_SEPARATOR = "\n";
    private static String delimiter = COMMON.DEFAUT_DELIMITER;
    private static int case_id = 0;

    private static BufferedReader lineReader;
    private static ArrayList<TestSuite> testSuites;

    private static TestScript testScript = new TestScript();;

    private String path;
    private static Logger LOG = Logger.getLogger(ScriptParser.class.getName());

    public ScriptParser(String path){
    }


    public static void parseScript(String path){
        testScript = new TestScript();
        testScript.setFileName(path);
        int rowNum = 1;
        try {
            lineReader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
            SqlCommand command = new SqlCommand();
            String line = lineReader.readLine();
            String trimmedLine = null;
            boolean ignore = false;
            int con_id = 0;

            while (line != null) {
                line = new String(line.getBytes(), "utf-8");
                trimmedLine = line.trim();

                //extract sql commands from the script file
                if (trimmedLine.equals("") || lineIsComment(trimmedLine)) {

                    /*//if line is  mark to ignore flag,ignore
                    if(trimmedLine.startsWith(COMMON.IGNORE_START_FLAG) && COMMON.IGNORE_MODEL)
                        ignore = true;
                    if(trimmedLine.startsWith(COMMON.IGNORE_END_FLAG))
                        ignore = false;*/
                    //deal the tag bvt:issue:{issue number},when cases with this tag,will be ignored
                    if(trimmedLine.startsWith(COMMON.BVT_ISSUE_START_FLAG) && COMMON.IGNORE_MODEL)
                        ignore = true;
                    if(trimmedLine.equalsIgnoreCase(COMMON.BVT_ISSUE_END_FLAG))
                        ignore = false;


                    //if line is mark to start a new conneciton
                    if(trimmedLine.startsWith(COMMON.NEW_SESSION_START_FLAG)){
                        if(trimmedLine.indexOf("id=") == -1){
                            LOG.warn("["+path+"][row:"+rowNum+"]The new connection flag doesn't specify the connection id by [id=X],and the id will be set to default value 1");
                            command.setConn_id(1);
                        }
                        con_id = Integer.parseInt(trimmedLine.substring(trimmedLine.indexOf("id=") + 3,trimmedLine.indexOf("id=") + 4));
                    }

                    if(trimmedLine.startsWith(COMMON.NEW_SESSION_END_FLAG)){
                        con_id = 0;
                    }

                    line = lineReader.readLine();
                    rowNum++;
                    continue;
                }

                if(trimmedLine.contains(delimiter) || trimmedLine.equals(delimiter)){
                    command.append(trimmedLine);
                    command.setConn_id(con_id);
                    command.setIgnore(ignore);
                    command.setPosition(rowNum);
                    testScript.addCommand(command);
                    command = new SqlCommand();
                }else {
                    command.append(trimmedLine);
                }
                line = lineReader.readLine();
                rowNum++;
            }
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void parseSuite(String path){
        testScript = new TestScript();
        testScript.setFileName(path);

        testSuites = new ArrayList<TestSuite>();

        SqlCommand sqlCommand = new SqlCommand();
        SqlCommand preCommand = sqlCommand;
        try {
            lineReader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
            SqlCommand command = new SqlCommand();
            String line = lineReader.readLine();
            String trimmedLine = null;

            while(line != null){
                line = new String(line.getBytes(),"utf-8");
                if(line.equals("")) {
                    line = lineReader.readLine();
                    continue;
                }

                trimmedLine = line.trim();

                //if suite
                if(trimmedLine.startsWith(COMMON.SUITE_FLAG)) {
                    //handleTestSuite();
                    TestSuite suite = new TestSuite();
                    suite.setFileName(testScript.getFileName());
                    testSuites.add(suite);
                    line = lineReader.readLine();

                    while(line != null){
                        line = new String(line.getBytes(),"utf-8");
                        if(line.equals("")) {
                            line = lineReader.readLine();
                            continue;
                        }
                        trimmedLine = line.trim();

                        if(lineIsComment(trimmedLine)){
                            if(trimmedLine.startsWith(COMMON.SETUP_SUITE_FLAG)){
                                line = lineReader.readLine();
                                while(line != null){
                                    line = new String(line.getBytes(),"utf-8");
                                    if(line.equals("")) {
                                        line = lineReader.readLine();
                                        continue;
                                    }

                                    trimmedLine = line.trim();
                                    if(!lineIsComment(trimmedLine)){
                                        sqlCommand.setScriptFile(testScript.getFileName());
                                        sqlCommand.setConn_id(COMMON.CONNECTION_ID);
                                        if(trimmedLine.contains(delimiter) || trimmedLine.equals(delimiter)){
                                            sqlCommand.append(trimmedLine);
                                            suite.addSetupCommand(sqlCommand);
                                            testScript.addCommand(sqlCommand);
                                            sqlCommand = new SqlCommand();
                                            preCommand.setNext(sqlCommand);
                                            preCommand = sqlCommand;
                                        }else {
                                            sqlCommand.append(trimmedLine);
                                        }
                                    }else {
                                        if(trimmedLine.startsWith(COMMON.CASE_START_FLAG))
                                            break;
                                    }
                                    line = lineReader.readLine();
                                }
                            }

                            if(trimmedLine.startsWith(COMMON.CASE_START_FLAG)){
                                TestCase testCase = new TestCase();
                                testCase.setFileName(testScript.getFileName());
                                testCase.setSuite(suite);
                                suite.addTestCase(testCase);
                                line = lineReader.readLine();
                                while(line != null){
                                    line = new String(line.getBytes(),"utf-8");
                                    if(line.equals("")) {
                                        line = lineReader.readLine();
                                        continue;
                                    }
                                    trimmedLine = line.trim();
                                    if(!lineIsComment(trimmedLine)){
                                        sqlCommand.setScriptFile(testScript.getFileName());
                                        command.setConn_id(COMMON.CONNECTION_ID);
                                        if(trimmedLine.contains(delimiter) || trimmedLine.equals(delimiter)){
                                            sqlCommand.append(trimmedLine);
                                            testCase.addCommand(sqlCommand);
                                            testScript.addCommand(sqlCommand);
                                            sqlCommand = new SqlCommand();
                                            preCommand.setNext(sqlCommand);
                                            preCommand = sqlCommand;
                                        }else
                                            sqlCommand.append(trimmedLine);

                                    }else {
                                        if(trimmedLine.startsWith(COMMON.CASE_DESC_FLAG)){
                                            testCase.setDesc(trimmedLine.substring(COMMON.CASE_DESC_FLAG.length(),trimmedLine.length()));
                                        }

                                        if(trimmedLine.startsWith(COMMON.CASE_LABEL_FLAG)){
                                            testCase.addLabel(trimmedLine.substring(COMMON.CASE_LABEL_FLAG.length(),trimmedLine.length()));
                                        }

                                        if(trimmedLine.startsWith(COMMON.CASE_START_FLAG) || trimmedLine.startsWith(COMMON.SUITE_FLAG))
                                            break;
                                    }
                                    line = lineReader.readLine();
                                }
                            }

                            if(trimmedLine.startsWith(COMMON.SUITE_FLAG)){
                                break;
                            }
                        }
                    }
                }
            }
            //return testSuite;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //return null;
    }

    private static boolean lineIsComment(String trimmedLine) {
        return trimmedLine.startsWith("//") || trimmedLine.startsWith("--") || trimmedLine.startsWith("#");
    }

    public static TestScript getTestScript(){
        return testScript;
    }

    public static ArrayList<TestSuite> getTestSuites(){
        return testSuites;
    }

    //recovery to the initial state
    public static void clear() {

        try {
            delimiter = COMMON.DEFAUT_DELIMITER;
            case_id = 0;
            lineReader.close();
            COMMON.CONNECTION_ID = 0;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    public static void main(String[] args){
        //ScriptParser.initSuiteId("c:\\fdasf\\fdsafd\\sdfa.sql");
        ScriptParser.parseSuite("cases/test/builtin.sql");
        ArrayList<TestSuite> suites = ScriptParser.getTestSuites();
        for(int i = 0; i < suites.size();i++){
            TestSuite suite = suites.get(i);
            ArrayList<SqlCommand> setups = suite.getSetupSqls();
            for(int j = 0; j < setups.size();j++){
                System.out.println(setups.get(j).getCommand().trim());
            }

            ArrayList<TestCase> cases = suite.getCases();
            for(int j = 0; j < cases.size(); j++){
                TestCase testCase = cases.get(j);
                ArrayList<SqlCommand> sqlCommands = testCase.getCommands();
                for(int k = 0; k < sqlCommands.size();k++){
                    SqlCommand command = sqlCommands.get(k);
                    System.out.println(command.getCommand().trim());
                }
            }

            ArrayList<SqlCommand> teardowns = suite.getTeardownSqls();
            for(int j = 0; j < teardowns.size();j++){
                System.out.println(teardowns.get(j).getCommand().trim());
            }
        }

    }
}
