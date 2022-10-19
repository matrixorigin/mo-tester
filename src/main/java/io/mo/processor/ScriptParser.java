package io.mo.processor;

import io.mo.cases.SqlCommand;
import io.mo.cases.TestScript;
import io.mo.constant.COMMON;
import io.mo.util.MoConfUtil;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ScriptParser {
    private  String delimiter = COMMON.DEFAUT_DELIMITER;
    private  TestScript testScript = new TestScript();
    private static final Logger LOG = Logger.getLogger(ScriptParser.class.getName());

    public void parseScript(String path){
        testScript = new TestScript();
        testScript.setFileName(path);
        int rowNum = 1;
        try {
            BufferedReader lineReader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(path))));
            SqlCommand command = new SqlCommand();
            String line = lineReader.readLine();
            String trimmedLine;
            String issueNo = null;
            boolean ignore = false;
            int con_id = 0;
            String con_user = null;
            String con_pswd = null;

            while (line != null) {
                line = new String(line.getBytes(), StandardCharsets.UTF_8);
                trimmedLine = line.trim();
                //trimmedLine = line.replaceAll("\\s+$", "");
    
                //extract sql commands from the script file
                if (trimmedLine.equals("") || lineIsComment(trimmedLine)) {
                    //if line is  mark to need to be skipped
                    if(trimmedLine.startsWith(COMMON.BVT_SKIP_FILE_FLAG) && COMMON.IGNORE_MODEL) {
                        issueNo = trimmedLine.substring(COMMON.BVT_SKIP_FILE_FLAG.length());
                        testScript.setSkiped(true);
                        return ;
                    }
                    
                    //if line is  mark to relate to a bvt issue
                    //deal the tag bvt:issue:{issue number},when cases with this tag,will be ignored
                    if(trimmedLine.startsWith(COMMON.BVT_ISSUE_START_FLAG) && COMMON.IGNORE_MODEL) {
                        issueNo = trimmedLine.substring(COMMON.BVT_ISSUE_START_FLAG.length());
                        ignore = true;
                    }
                    if(trimmedLine.equalsIgnoreCase(COMMON.BVT_ISSUE_END_FLAG)) {
                        issueNo = null;
                        ignore = false;
                    }

                    //if line is mark to start a new connection
                    if(trimmedLine.startsWith(COMMON.NEW_SESSION_START_FLAG)){

                        String conInfo = null;
                        if(trimmedLine.endsWith("{"))
                            conInfo = trimmedLine.substring(COMMON.NEW_SESSION_START_FLAG.length(),trimmedLine.length() - 1);
                        else
                            conInfo = trimmedLine.substring(COMMON.NEW_SESSION_START_FLAG.length(),trimmedLine.length());
                        
                        if (conInfo == null || conInfo.equalsIgnoreCase("")){
                            LOG.warn("["+path+"][row:"+rowNum+"]The new connection flag doesn't designate the connection id by [id=X],and the id will be set to default value 1");
                            //command.setConn_id(COMMON.NEW_SEESION_DEFAULT_ID);
                            con_id = COMMON.NEW_SEESION_DEFAULT_ID;
                        }else {
                            String[] paras = conInfo.split("&");
                            for (String para:paras) {
                                if(para.startsWith("id=")){
                                    String id = para.substring(3);
                                    if(id.equalsIgnoreCase("")){
                                        LOG.warn("["+path+"][row:"+rowNum+"]The new connection flag doesn't designate the connection id by [id=X],and the id will be set to default value 1");
                                        //command.setConn_id(COMMON.NEW_SEESION_DEFAULT_ID);
                                        con_id = COMMON.NEW_SEESION_DEFAULT_ID;
                                    }else{
                                        if(id.matches("[0-9]")){
                                            con_id = Integer.parseInt(id);
                                        }else {
                                            LOG.warn("["+path+"][row:"+rowNum+"]The new connection flag designate a invalid connection id by [id=X],and the id will be set to default value 1");
                                            //command.setConn_id(COMMON.NEW_SEESION_DEFAULT_ID);
                                            con_id = COMMON.NEW_SEESION_DEFAULT_ID;
                                        }
                                    }
                                }
                                
                                if(para.startsWith("user=")){
                                    String user = para.substring(5);
                                    if(user.equalsIgnoreCase("")){
                                        LOG.warn("["+path+"][row:"+rowNum+"]The new connection flag doesn't designate the connection user by [user=X],and the id will be set to value from mo.yml");
                                        //command.setConn_user(MoConfUtil.getUserName());
                                        con_user = MoConfUtil.getUserName();
                                    }else {
                                        //command.setConn_user(user);
                                        con_user = user;
                                    }
                                }
                                
                                if(para.startsWith("password=")){
                                    String pwd = para.substring(9);
                                    if(pwd.equalsIgnoreCase("")){
                                        LOG.warn("["+path+"][row:"+rowNum+"]The new connection flag doesn't designate the connection password by [password=X],and the id will be set to value from mo.yml");
                                        //command.setConn_pswd(MoConfUtil.getUserpwd());
                                        con_pswd = MoConfUtil.getUserpwd();
                                    }else {
                                        //command.setConn_pswd(pwd);
                                        con_pswd = pwd;
                                    }
                                }
                            }
                        }
                    }

                    if(trimmedLine.equalsIgnoreCase(COMMON.NEW_SESSION_END_FLAG) || trimmedLine.equalsIgnoreCase(COMMON.NEW_SESSION_END_FLAG + "}")){
                        con_id = 0;
                        con_user = null;
                        con_pswd = null;
                    }

                    //if line is mark to set sort key index
                    if(trimmedLine.startsWith(COMMON.SORT_KEY_INDEX_FLAG)){
                        String[] indexes = trimmedLine.replaceAll(COMMON.SORT_KEY_INDEX_FLAG,"").split(",");
                        for (String index : indexes) {
                            command.addSortKeyIndex(Integer.parseInt(index));
                        }
                    }

                    //if line is mark to set column separator
                    if(trimmedLine.startsWith(COMMON.COLUMN_SEPARATOR_FLAG)){
                        String separator = trimmedLine.replaceAll(COMMON.COLUMN_SEPARATOR_FLAG,"");
                        command.setSeparator(separator);
                    }

                    line = lineReader.readLine();
                    rowNum++;
                    continue;
                }

                if(trimmedLine.contains(delimiter)){
                    command.append(trimmedLine);
                    command.setConn_id(con_id);
                    command.setConn_user(con_user);
                    command.setConn_pswd(con_pswd);
                    command.setIgnore(ignore);
                    command.setIssueNo(issueNo);
                    command.setPosition(rowNum);
                    testScript.addCommand(command);
                    command = new SqlCommand();
                }else {
                    command.append(trimmedLine);
                    command.append(COMMON.LINE_SEPARATOR);
                }
                line = lineReader.readLine();
                rowNum++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean lineIsComment(String trimmedLine) {
        return trimmedLine.startsWith("//") || trimmedLine.startsWith("--") || trimmedLine.startsWith("#");
    }

    public TestScript getTestScript(){
        return testScript;
    }

    public static void main(String[] args){
    }
}
