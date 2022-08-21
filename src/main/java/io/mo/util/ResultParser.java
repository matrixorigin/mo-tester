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
import java.nio.file.Files;
import java.nio.file.Paths;

public class ResultParser {
    private static BufferedReader lineReader;
    private static final StringBuffer resultText = new StringBuffer();
    private static final Logger LOG = Logger.getLogger(ResultParser.class.getName());
    private static boolean succeeded = true;

    /**
     * Get result text of the specific SqlCommand from the result file
     */
    public static String getCommandResult(SqlCommand command){
        if(command.getNext() != null)
            return getCommandResult(command.getCommand(),command.getNext().getCommand());
        else
            return getCommandResult(command.getCommand(),null);
    }

    /**
     * Get result text of the specific SqlCommand from the result file
     * The result text is the content between the cmd and the next cmd in the reulst file
     */
    public static String getCommandResult(String cmd,String nextcmd){
        String cmdResult;
        if(nextcmd == null){
            //if the nextcmd is null,means cmd is the last one,return the content of the resultText directedly

            if(cmd.length() == resultText.length())
                //the last cmd has no result
                return null;
            else
                return resultText.substring(cmd.length() + 1,resultText.length());
        }else {
            //normally,the resultText should start with the command
            //so the command result which is the content between the cmd and the nextcmd should be the substring of the resultText from cmd.length() to the position of the nextcmd
            int from = cmd.length() + 1;
            int to   = resultText.indexOf(nextcmd,cmd.length());
            //if from == to,means result is null
            if(from == to) {
                resultText.delete(0,to);
                return null;
            }else {
                cmdResult = resultText.substring(from, to - 1);
                //make the resultText starting with the nextcmd
                resultText.delete(0, to);
                return cmdResult;
            }
        }
    }

    public static void parse(TestScript script){

        //check whether the result file exists
        String rsFilePath = null;
        File resFile;
        rsFilePath = script.getFileName().replaceAll("\\.[A-Za-z]+",COMMON.R_FILE_SUFFIX);
        resFile = new File(rsFilePath);
        if(!resFile.exists()){
            rsFilePath = script.getFileName().replaceFirst(COMMON.CASES_PATH,COMMON.RESULT_PATH).replaceAll("\\.[A-Za-z]+",COMMON.R_FILE_SUFFIX);
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
            if(resultText.indexOf(command.getCommand()) == -1){
                LOG.error("[Exceptional command]["+script.getFileName()+"]["+command.getPosition()+"]:"+command.getCommand().trim() + ",it does not exist in result file");
                //set the test script file invalid
                script.invalid();
                succeeded = false;
                return;
            }

            if(command.getNext() != null && resultText.indexOf(command.getNext().getCommand()) == -1){
                //System.out.println("resultText = \n" + resultText);
                //System.out.println("nextCommand = \n" + command.getNext().getCommand());
                LOG.error("[Exceptional command]["+script.getFileName()+"]["+command.getNext().getPosition()+"]:"+command.getNext().getCommand().trim() + ",it does not exist in result file");
                //set the test script file invalid
                script.invalid();
                succeeded = false;
                return;
            }

            String resText = getCommandResult(command);
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
        LOG.info("Now start to check the file["+script.getFileName()+"]....................................................");
        String rsFilePath = null;
        File resFile;
        rsFilePath = script.getFileName().replaceAll("\\.[A-Za-z]+",COMMON.R_FILE_SUFFIX);
        resFile = new File(rsFilePath);
        if(!resFile.exists()){
            rsFilePath = script.getFileName().replaceFirst(COMMON.CASES_PATH,COMMON.RESULT_PATH).replaceAll("\\.[A-Za-z]+",COMMON.R_FILE_SUFFIX);
            resFile = new File(rsFilePath);
            if(!resFile.exists()){
                LOG.warn("The result of the test script file["+rsFilePath+"] does not exists,please check and this test script file will be skipped.");
                //set the test script file invalid
                script.invalid();
                succeeded = false;
                return;
            }
        }

        StringBuilder result = new StringBuilder();

        try {
            BufferedReader lineReader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(resFile.getPath()))));
            while(true){
                String line = lineReader.readLine();
                if(line == null)
                    break;
                result.append(new String(line.getBytes(), StandardCharsets.UTF_8));
                result.append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(script.getTotalCmdCount() == 0)
            return;

        String firstcmd = script.getCommand(0);
        if(result.indexOf(firstcmd) != 0){
            LOG.error("["+script.getFileName()+"]:The first command in case file and result file does not match.Check failed,please check the files");
            LOG.error("[Exceptional command]["+script.getFileName()+"]["+script.getCommands().get(0).getPosition()+"]:"+firstcmd.trim()+"");
            return;
        }

        for(int i = 0; i < script.getTotalCmdCount(); i++){
            SqlCommand cmd = script.getCommands().get(i);
            int pos = result.indexOf(cmd.getCommand());
            if(pos == -1){
                //LOG.error("["+script.getFileName()+"]["+cmd.trim()+"] does not exist in the result file.Check failed,please check the files");
                LOG.error("[Exceptional command]["+script.getFileName()+"]["+cmd.getPosition()+"]:"+cmd.getCommand().trim());
                return;
            }
            result.delete(0,pos+cmd.getCommand().length());
        }

        if(result.length() != 0){
            if(result.indexOf(COMMON.DEFAUT_DELIMITER) != -1){
                SqlCommand last = script.getCommands().get(script.getTotalCmdCount() - 1);
                LOG.error("["+rsFilePath+"]:There are some sqls and resutls which are not in the case file");
                LOG.error("The last sql in the ["+script.getFileName()+"][row:"+last.getPosition()+"]: "+last.getCommand().trim());
                LOG.error("["+rsFilePath+"]Exceptional content:\n"+ result.substring(0,result.indexOf(COMMON.DEFAUT_DELIMITER)+1)+".............");
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
        StringBuilder buffer = new StringBuilder();
        //first,replace separator to the system designated separator
        if(separator.equals("both"))
            buffer.append(rsText.replaceAll(RESULT.COLUMN_SEPARATOR_SPACE,RESULT.COLUMN_SEPARATOR_SYSTEM).replaceAll(RESULT.COLUMN_SEPARATOR_TABLE,RESULT.COLUMN_SEPARATOR_SYSTEM));
        if(separator.equals("table"))
            buffer.append(rsText.replaceAll(RESULT.COLUMN_SEPARATOR_TABLE,RESULT.COLUMN_SEPARATOR_SYSTEM));
        if(separator.equals("space"))
            buffer.append(rsText.replaceAll(RESULT.COLUMN_SEPARATOR_SPACE,RESULT.COLUMN_SEPARATOR_SYSTEM));
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
            String[] values = rowline.split(RESULT.COLUMN_SEPARATOR_SYSTEM);
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
}
