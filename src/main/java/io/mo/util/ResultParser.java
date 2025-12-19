package io.mo.util;

import io.mo.cases.SqlCommand;
import io.mo.cases.TestScript;
import io.mo.constant.COMMON;
import io.mo.constant.DATATYPE;
import io.mo.constant.RESULT;
import io.mo.result.*;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ResultParser {
    private static BufferedReader lineReader;
    private static final StringBuffer resultText = new StringBuffer();
    private static final Logger LOG = Logger.getLogger(ResultParser.class.getName());
    private static boolean succeeded = true;

    /**
     * Get result text of the specific SqlCommand from the result file
     */
    public static String getCommandResult(SqlCommand command) throws Exception{
        if(command.getNext() != null) {
            //System.out.println(command.getPosition()+":command = "+command.getCommand());
            return getCommandResult(command.getCommand(), command.getNext().getCommand());
        }
        else
            return getCommandResult(command.getCommand(),null);
    }

    /**
     * Get result text of the specific SqlCommand from the result file
     * The result text is the content between the cmd and the next cmd in the reulst file
     */
    public static String getCommandResult(String cmd,String nextcmd) throws Exception{
        String cmdResult;
        if(nextcmd == null){
            //if the nextcmd is null,means cmd is the last one,return the content of the resultText directedly

            if(cmd.length() == resultText.length())
                //the last cmd has no result
                return null;
            else {
                return resultText.substring(cmd.length() + 1, resultText.length());
            }
        }else {
            //normally,the resultText should start with the command
            //so the command result which is the content between the cmd and the nextcmd should be the substring of the resultText from cmd.length() to the position of the nextcmd
            int from = cmd.length() + 1;
            int index = cmd.length();
            int to   = resultText.indexOf(nextcmd,index);
            if(to < from){
                throw new Exception("Parse error");
            }
            if(from == to) {
                resultText.delete(0,to);
                return null;
            }else {
                cmdResult = resultText.substring(from, to - 1);
                //System.out.println("cmdResult = " + cmdResult);
                //make the resultText starting with the nextcmd
                resultText.delete(0, to);
                return cmdResult;
            }
        }
    }

    public static void parse(TestScript script){
        reset();
        //check whether the result file exists
        String rsFilePath = null;
        File resFile;
        rsFilePath = script.getFileName().replaceAll("\\.[A-Za-z]+",COMMON.R_FILE_SUFFIX);
        resFile = new File(rsFilePath);
        if(!resFile.exists()){
            rsFilePath = script.getFileName().replaceFirst(COMMON.CASES_DIR,COMMON.RESULT_DIR).replaceAll("\\.[A-Za-z]+",COMMON.R_FILE_SUFFIX);
            resFile = new File(rsFilePath);
            if(!resFile.exists()){
                LOG.warn("The result of the test script file["+rsFilePath+"] does not exists,please check and this test script file will be skipped.");
                //set the test script file invalid
                script.invalid();
                succeeded = false;
                return; 
            }
        }
        
        try {
            lineReader = new BufferedReader(new InputStreamReader(new FileInputStream(rsFilePath)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        
        //read result file
        read();

        for(int i = 0; i < script.getTotalCmdCount(); i++){
            SqlCommand command = script.getCommands().get(i);
            if(resultText.indexOf(command.getCommand()) != 0){
                LOG.error("[Exceptional command]["+script.getFileName()+"]["+command.getPosition()+"]:"+command.getCommand().trim() + ",it does not exist in result file");
                //set the test script file invalid
                script.invalid();
                succeeded = false;
                return;
            }
            
            int fromIndex = command.getCommand().length();
            
            if(command.getNext() != null && resultText.indexOf(command.getNext().getCommand(),fromIndex) == -1){
                LOG.error("[Exceptional command]["+script.getFileName()+"]["+command.getNext().getPosition()+"]:"+command.getNext().getCommand().trim() + ",it does not exist in result file");
                //set the test script file invalid
                script.invalid();
                succeeded = false;
                return;
            }

            String resText = null;
            try {
                resText = getCommandResult(command);
            } catch (Exception e) {
                LOG.error("[Exceptional command]["+script.getFileName()+"]["+command.getPosition()+"]:"+command.getCommand().trim());
                //set the test script file invalid
                script.invalid();
                succeeded = false;
                return;
            }
            StmtResult expResult = new StmtResult();
            expResult.setCommand(command);
            if(resText == null || resText.equals("")){
                expResult.setType(RESULT.STMT_RESULT_TYPE_NONE);
            }
            else{
                expResult.setOrginalRSText(resText);
            }
            command.setExpResult(expResult);
        }
    }

    /**
     * read content from result file
     */
    public static void read(){
        try {
            if(lineReader == null || !lineReader.ready()){
                return;
            }

            String line = lineReader.readLine();
            while(line != null) {
                line = new String(line.getBytes(), StandardCharsets.UTF_8);
                resultText.append(line);
                line = lineReader.readLine();
                if(line != null)
                    resultText.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * check whether test file matches result file
     */
    public static void check(TestScript script){
        reset();
        //check whether the result file exists
        String rsFilePath = null;
        File resFile;
        rsFilePath = script.getFileName().replaceAll("\\.[A-Za-z]+",COMMON.R_FILE_SUFFIX);
        resFile = new File(rsFilePath);
        if(!resFile.exists()){
            rsFilePath = script.getFileName().replaceFirst(COMMON.CASES_DIR,COMMON.RESULT_DIR).replaceAll("\\.[A-Za-z]+",COMMON.R_FILE_SUFFIX);
            resFile = new File(rsFilePath);
            if(!resFile.exists()){
                LOG.warn("The result of the test script file["+rsFilePath+"] does not exists,please check and this test script file will be skipped.");
                //set the test script file invalid
                script.invalid();
                succeeded = false;
                return;
            }
        }

        try {
            lineReader = new BufferedReader(new InputStreamReader(new FileInputStream(rsFilePath)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        //read result file
        read();

        for(int i = 0; i < script.getTotalCmdCount(); i++){
            SqlCommand command = script.getCommands().get(i);
            if(resultText.indexOf(command.getCommand()) != 0){
                LOG.error("[Exceptional command]["+script.getFileName()+"]["+command.getPosition()+"]:"+command.getCommand().trim() + ",it does not exist in result file");
                return;
            }

            int fromIndex = command.getCommand().length();

            if(command.getNext() != null && resultText.indexOf(command.getNext().getCommand(),fromIndex) == -1){
                LOG.error("[Exceptional command]["+script.getFileName()+"]["+command.getNext().getPosition()+"]:"+command.getNext().getCommand().trim() + ",it does not exist in result file");
                return;
            }

            try {
                getCommandResult(command);
            } catch (Exception e) {
                LOG.error("[Exceptional command]["+script.getFileName()+"]["+command.getPosition()+"]:"+command.getCommand().trim());
                //set the test script file invalid
                script.invalid();
                succeeded = false;
                return;
            }
        }
    }

    /**
     * convert the result text to a RSSet instance
     * @param separator column separator,can be 3 values:
     * 1、table,separator is \t
     * 2、space,separator is 4 spaces
     * 3、both,separator is \t or 4 spaces
     */
    public static RSSet convertToRSSet(String rsText,String separator){
        
        if(rsText == null){
            return null;
        }
        
        StringBuilder buffer = new StringBuilder();
        //first,replace separator to the system designated separator
        //Use Pattern.quote to treat the separator as literal string, not regex
        if(separator.equals("both"))
            buffer.append(rsText.replaceAll(java.util.regex.Pattern.quote(RESULT.COLUMN_SEPARATOR_SPACE),RESULT.COLUMN_SEPARATOR_SYSTEM).replaceAll(java.util.regex.Pattern.quote(RESULT.COLUMN_SEPARATOR_TABLE),RESULT.COLUMN_SEPARATOR_SYSTEM));
        if(separator.equals("table"))
            buffer.append(rsText.replaceAll(java.util.regex.Pattern.quote(RESULT.COLUMN_SEPARATOR_TABLE),RESULT.COLUMN_SEPARATOR_SYSTEM));
        if(separator.equals("space"))
            buffer.append(rsText.replaceAll(java.util.regex.Pattern.quote(RESULT.COLUMN_SEPARATOR_SPACE),RESULT.COLUMN_SEPARATOR_SYSTEM));
        RSSet rsSet = new RSSet();
        
        //first line is meta info
        String labelline;
        boolean lastrow = false;

        if(buffer.indexOf("\n") == -1){
            labelline = buffer.toString();
            buffer.delete(0,buffer.length());
            lastrow = true;
        }else {
            labelline = buffer.substring(0,buffer.indexOf("\n"));
            buffer.delete(0,buffer.indexOf("\n") + 1);
        }
        //deal with meta data
        String[] labels = labelline.split(RESULT.COLUMN_SEPARATOR_SYSTEM);
        int columnCount = labels.length;
        RSMetaData rsMetaData = new RSMetaData(columnCount);
        rsSet.setMeta(rsMetaData);
        for(String label : labels){
            rsMetaData.addMetaInfo(label,label);
        }
        //deal with rows data
        while(!lastrow){
            String rowline;
            if(buffer.indexOf("\n") == -1){
                rowline = buffer.toString();
                buffer.delete(0,buffer.length());
                lastrow = true;
            }else {
                rowline = buffer.substring(0,buffer.indexOf("\n"));
                buffer.delete(0,buffer.indexOf("\n") + 1);
            }
            RSRow rsRow = new RSRow(columnCount);
            rsSet.addRow(rsRow);
            // Use limit parameter to split into exactly columnCount parts
            // This prevents splitting values that contain the separator
            String[] values = rowline.split(java.util.regex.Pattern.quote(RESULT.COLUMN_SEPARATOR_SYSTEM), columnCount);
            for(int i = 0; i < columnCount; i++){
                RSCell rsCell = new RSCell();
                rsCell.setType(DATATYPE.TYPE_STRING);
                if(i < values.length){
                    rsCell.setValue(values[i]);
                }else{
                    rsCell.setValue("");
                }

                rsRow.addCell(rsCell);
            }
        }
        return rsSet;
    }

    public static void reset(){
        succeeded = true;
        resultText.delete(0,resultText.length());
        if(lineReader != null){
            try {
                lineReader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            lineReader = null;
        }
    }

    public static boolean isSucceeded(){
        return succeeded;
    }
    
    public static int subStrCount(String str, String sub){
        if(str == null || sub == null)
            return 0;
        
        if(!str.contains(sub)){
            return 0;
        }else {
            int i = 1;
            String temp = str.replace(sub,"");
            while(temp.contains(sub)){
                i++;
                temp = temp.replace(sub,"");
            }
            return i;
        }
    }
    
    public static void main(String[] args){
    }
}
