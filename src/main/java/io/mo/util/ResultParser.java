package io.mo.util;

import io.mo.cases.SqlCommand;
import io.mo.db.Executor;
import org.apache.log4j.Logger;

import java.io.*;

public class ResultParser {
    private static BufferedReader lineReader;
    private static Logger LOG = Logger.getLogger(ResultParser.class.getName());
    private static boolean skip = false;
    public static void parse(String path){
        try {
            lineReader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            LOG.warn("The result of the test script file["+path+"] does not exists,please check....");
        }
    }

    public static String getRS(String cmd,String nextcmd){
        String line = null;
        StringBuffer buffer = new StringBuffer();
        boolean cmd_deleted = false;

        try {
            while((line = lineReader.readLine()) != null) {
                line = new String(line.getBytes(), "utf-8");
                //if (line.equals("")) continue;
                buffer.append(line);
                buffer.append("\n");
                if(buffer.indexOf(cmd) != -1 && !cmd_deleted && !skip){
                    buffer.delete(0,buffer.length());
                    cmd_deleted = true;
                }

                if(nextcmd != null){
                    if(buffer.indexOf(nextcmd) != -1){
                        buffer.delete(buffer.length()-nextcmd.length(),buffer.length());
                        //skip = true,means has read the next command,if call the skip() func,will do nothing;
                        skip = true;
                        return buffer.toString().trim();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return buffer.toString().trim();
    }

    public static void skip(String cmd){

        //skip = true,means has read the next command,if call the skip() func,will do nothing;
        if (skip) {
            skip = false;
            return;
        }

        String line = null;
        StringBuffer buffer = new StringBuffer();
        try {
            while((line = lineReader.readLine()) != null) {
                line = new String(line.getBytes(), "utf-8");
                if (line.equals("")) continue;
                buffer.append(line);
                buffer.append("\n");
                if(buffer.indexOf(cmd) != -1){
                    buffer.delete(0,buffer.length());
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void reset(){
        skip = false;
        if(lineReader != null){
            try {
                lineReader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            lineReader = null;
        }
    }
}
