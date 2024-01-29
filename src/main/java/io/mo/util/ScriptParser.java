package io.mo.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import freemarker.template.utility.NumberUtil;
import io.mo.cases.SqlCommand;
import io.mo.cases.TestScript;
import io.mo.constant.COMMON;
import io.mo.stream.TopicAndRecords;
import org.apache.log4j.Logger;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ScriptParser {
    private static String delimiter = COMMON.DEFAUT_DELIMITER;
    private static TestScript testScript = new TestScript();
    private static final Logger LOG = Logger.getLogger(ScriptParser.class.getName());

    public static void parseScript(String path){
        
        //reset delimiter to default value
        delimiter = COMMON.DEFAUT_DELIMITER;
        
        testScript = new TestScript();
        testScript.setFileName(path);
        int rowNum = 1;
        try {
            BufferedReader lineReader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(path))));
            SqlCommand command = new SqlCommand();
            TopicAndRecords tar = new TopicAndRecords();
            String line = lineReader.readLine();
            String trimmedLine;
            String issueNo = null;
            boolean ignore = false;
            boolean isProduceRecord = false;
            int con_id = 0;
            String con_user = null;
            String con_pswd = null;
            
            StringBuffer messages = new StringBuffer();

            while (line != null) {
                line = new String(line.getBytes(), StandardCharsets.UTF_8);
                trimmedLine = line.trim();
                //trimmedLine = line.replaceAll("\\s+$", "");

                //extract sql commands from the script file
                if (trimmedLine.equals("") || lineIsComment(trimmedLine)) {
                    
                    //if line is  mark to need to be skipped
                    if(trimmedLine.startsWith(COMMON.BVT_SKIP_FILE_FLAG)) {
                        issueNo = trimmedLine.substring(COMMON.BVT_SKIP_FILE_FLAG.length());
                        testScript.setSkiped(true);
                        LOG.info(String.format("The script file [%s] is marked to be skiped for issue#%s, and it will not be executed.",path,issueNo));
                        return ;
                    }

                    if(trimmedLine.startsWith(COMMON.NEW_DELIMITER_FLAG)) {
                        delimiter = trimmedLine.substring(COMMON.NEW_DELIMITER_FLAG.length());
                        LOG.info(String.format("The delimiter has been set to [%s].",delimiter));
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

                    if(trimmedLine.startsWith(COMMON.FUNC_SLEEP_FLAG)){
                        int time = Integer.parseInt(trimmedLine.substring(COMMON.FUNC_SLEEP_FLAG.length()));
                        command.setSleeptime(time);
                    }

                    if(trimmedLine.startsWith(COMMON.SYSTEM_CMD_FLAG)){
                        String sysCmd = trimmedLine.substring(COMMON.SYSTEM_CMD_FLAG.length());
                        command.addSysCMD(sysCmd);
                    }

                    if(trimmedLine.startsWith(COMMON.REGULAR_MATCH_FLAG)){
                        command.setRegularMatch(true);
                    }
                    
//                    if(trimmedLine.startsWith(COMMON.KAFKA_PRODUCE_START_FLAG)){
//                        String topic = trimmedLine.substring(COMMON.KAFKA_PRODUCE_START_FLAG.length());
//                        if(topic == null || topic.equalsIgnoreCase("")){
//                            LOG.error(String.format("[%s][row:%s]No topic info in kafka produce tag.",path,rowNum));
//                            continue;
//                        }
//                        tar.setTopic(topic);
//                    }

                    if(trimmedLine.startsWith(COMMON.KAFKA_PRODUCE_START_FLAG)){
                        String topic = trimmedLine.substring(COMMON.KAFKA_PRODUCE_START_FLAG.length());
                        if(topic == null || topic.equalsIgnoreCase("")){
                            LOG.error(String.format("[%s][row:%s]No topic info in kafka produce tag.",path,rowNum));
                            continue;
                        }
                        tar.setTopic(topic);
                        isProduceRecord = true;
                    }

                    if(trimmedLine.equalsIgnoreCase(COMMON.KAFKA_PRODUCE_END_FLAG)){
                        isProduceRecord = false;
                        JSONArray array = JSON.parseArray(messages.toString());
                        for(int i = 0; i < array.size();i++){
                            tar.addRecord(JSON.toJSONString(array.get(i),SerializerFeature.NotWriteDefaultValue));
                        }
                        int index = testScript.getCommands().size();
                        testScript.addKafkaProduceRecord(index,tar);
                        messages.delete(0,messages.length());
                        tar = new TopicAndRecords();
                    }
                    

                    if(trimmedLine.startsWith(COMMON.IGNORE_COLUMN_FLAG)){
                        String ignores = trimmedLine.substring(COMMON.IGNORE_COLUMN_FLAG.length());
                        if(ignores != null || !ignores.equalsIgnoreCase("")){
                            String[] ignore_ids = ignores.split(",");
                            for(int i = 0; i < ignore_ids.length;i++){
                                command.addIgnoreColumn(Integer.parseInt(ignore_ids[i]));
                            }
                        }
                    }

                    //if line is mark to set wait paras
                    if(trimmedLine.startsWith(COMMON.WAIT_FLAG)){
                        String[] items = trimmedLine.split(":");
                        //if flas is not formated like <-- @wait:1:commit >, ignore
                        if(items.length != 3)
                            continue;
                        else{
                            if(StringUtils.isNumeric(items[1])){
                                command.setWaitConnId(Integer.parseInt(items[1]));
                            }else {
                                LOG.warn(String.format("The connection id in flag[%s] is not a number, the flag is not valid.",trimmedLine));
                                continue;
                            }
                            
                            if(items[2].equalsIgnoreCase("commit") || 
                                    items[2].equalsIgnoreCase("rollback")){
                                command.setWaitOperation(items[2]);
                            }else {
                                LOG.warn(String.format("The operation in flag[%s] is not [commit] or [rollback], the flag is not valid.",trimmedLine));
                                continue;
                            }
                            
                            command.setNeedWait(true);
                        }
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
                
                if(isProduceRecord){
                    messages.append(trimmedLine);
                    line = lineReader.readLine();
                    rowNum++;
                    continue;
                }
                
                if(trimmedLine.contains(delimiter)){
                    if(delimiter.equalsIgnoreCase(COMMON.DEFAUT_DELIMITER))
                        command.append(trimmedLine);
                    else 
                        command.trim();
                    
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

    private static boolean lineIsComment(String trimmedLine) {
        return trimmedLine.startsWith("//") || trimmedLine.startsWith("--") || trimmedLine.startsWith("#");
    }

    public static TestScript getTestScript(){
        return testScript;
    }

    public static void main(String[] args){
        String str = "[{\"c1\":\"yyjjuejf\",\"c2\":\"中国\",\"c3\":\"##$%^&@\",\"c4\":\"\"},\n" +
                "{\"c1\":NULL,\"c2\":\"0xDERFW9883\",\"c3\":\"5727362\",\"c4\":\"x'612543'\"}]";
        System.out.println(str);
        JSONArray array = JSON.parseArray(str);
        for(int i = 0 ; i < array.size();i++) {
            System.out.println(JSON.toJSONString(array.get(i),SerializerFeature.WriteMapNullValue));
        }
    }
}
