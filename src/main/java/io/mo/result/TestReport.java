package io.mo.result;

import io.mo.cases.SqlCommand;
import io.mo.cases.TestScript;
import io.mo.constant.COMMON;
import io.mo.constant.RESULT;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;


public class TestReport {
    private ArrayList<TestScript> scripts = new ArrayList<TestScript>();
    private int totalCmds = 0;
    private int successCmds = 0;
    private int failedCmds = 0;
    private int ignoredCmds = 0;
    private int abnormalCmds = 0;
    private long duration = 0;
    private static Logger LOG = Logger.getLogger(TestReport.class.getName());

    private int rate = 0;

    public TestReport(){
        File dir = new File(COMMON.REPORT_DIR);
        if(!dir.exists())
            dir.mkdir();

    }

    public void write(){
        writeTXTReport();
    }


    public void writeTXTReport(){
        if(totalCmds != 0)
            rate  = (successCmds*100)/ (totalCmds - ignoredCmds);
        else
            rate = 0;

        ArrayList<SqlCommand> e_commands = new ArrayList<SqlCommand>();
        try {
            BufferedWriter r_writer = new BufferedWriter(new FileWriter(COMMON.REPORT_DIR +"/report.txt"));
            BufferedWriter e_writer = new BufferedWriter(new FileWriter(COMMON.REPORT_DIR +"/error.txt"));
            BufferedWriter s_writer = new BufferedWriter(new FileWriter(COMMON.REPORT_DIR +"/success.txt"));
            r_writer.write(getReportSummaryTXT(totalCmds, successCmds, failedCmds, ignoredCmds, abnormalCmds));
            LOG.info(getReportSummaryTXT(totalCmds, successCmds, failedCmds, ignoredCmds, abnormalCmds).trim());

            for(int i = 0; i < scripts.size();i++){
                TestScript script = scripts.get(i);
                r_writer.write(getScriptSummaryTXT(script));
                LOG.info(getScriptSummaryTXT(script).trim());

                List<SqlCommand> commands = script.getCommands();
                for(SqlCommand command : commands){
                    if(!(command.getTestResult().getResult().equals(RESULT.RESULT_TYPE_PASS) ||
                         command.getTestResult().getResult().equals(RESULT.RESULT_TYPE_IGNORED))) {
                        e_writer.write(getErrorInfo(command));
                        e_writer.newLine();
                        e_commands.add(command);
                    }
                }

                List<SqlCommand> successCmdList = script.getSuccessCmdList();
                for(SqlCommand command : successCmdList){
                    s_writer.write("["+script.getFileName()+"]:"+command.getCommand().replaceAll("\r"," ").replaceAll("\n"," "));
                    s_writer.newLine();
                }
            }

            if(e_commands.size() > 0){
                LOG.info("[ERROR SQL LIST]");
                for(int i = 0; i < e_commands.size(); i++){
                    LOG.info("["+e_commands.get(i).getScriptFile()+"] SQL: "+e_commands.get(i).getCommand().trim());
                }
            }
            r_writer.flush();
            e_writer.flush();
            s_writer.flush();
            r_writer.close();
            e_writer.close();
            s_writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void collect(TestScript script){
        scripts.add(script);
        totalCmds += script.getTotalCmdCount();
        successCmds += script.getSuccessCmdCount();
        failedCmds += script.getFailedCmdCount();
        ignoredCmds += script.getIgnoredCmdCount();
        abnormalCmds += script.getAbnormalCmdCount();
        duration += script.getDuration();
    }

    public String getReportSummaryTXT(int total, int success, int failed, int ignored, int abnormal){
        StringBuffer buffer = new StringBuffer();
        buffer.append("[SUMMARY] COST : "+ duration + "s");
        buffer.append(", ");
        buffer.append("TOTAL :" + total);
        buffer.append(", ");
        buffer.append("SUCCESS : " + success);
        buffer.append(", ");
        buffer.append("FAILED :" + failed);
        buffer.append(", ");
        buffer.append("IGNORED :" + ignored);
        buffer.append(", ");
        buffer.append("ABNORAML :" + abnormal);
        buffer.append(", ");
        if(total != 0)
            buffer.append("SUCCESS RATE : " + (success*100)/(total - ignored) + "%\n");
        else
            buffer.append("SUCCESS RATE : 0%\n");
        return buffer.toString();
    }

    public String getScriptSummaryTXT(TestScript script){
        StringBuffer buffer = new StringBuffer();
        buffer.append("["+script.getFileName()+"] COST : " + script.getDuration() + "s");
        buffer.append(", ");
        buffer.append("TOTAL :" + script.getTotalCmdCount());
        buffer.append(", ");
        buffer.append("SUCCESS :" + script.getSuccessCmdCount());
        buffer.append(", ");
        buffer.append("FAILED :" + script.getFailedCmdCount());
        buffer.append(", ");
        buffer.append("IGNORED :" + script.getIgnoredCmdCount());
        buffer.append(", ");
        buffer.append("ABNORAML :" + script.getAbnormalCmdCount());
        buffer.append(", ");
        if(script.getTotalCmdCount() != 0) {
            if (script.getTotalCmdCount() - script.getIgnoredCmdCount() == 0)
                buffer.append("SUCCESS RATE : 0%\n");
            else
                buffer.append("SUCCESS RATE : " + (script.getSuccessCmdCount() * 100) / (script.getTotalCmdCount() - script.getIgnoredCmdCount()) + "%\n");
        }
        else
            buffer.append("SUCCESS RATE : 0%\n");
        return buffer.toString();
    }

    public String getErrorInfo(SqlCommand command){
        StringBuffer buffer = new StringBuffer();
        buffer.append("[ERROR]\n");
        buffer.append("[SCRIPT   FILE]: "+command.getScriptFile()+"\n");
        buffer.append("[ROW    NUMBER]: "+command.getPosition()+"\n");
        buffer.append("[SQL STATEMENT]: "+command.getCommand()+"\n");
        buffer.append("[EXPECT RESULT]:\n"+command.getTestResult().getExpResult()+"\n");
        buffer.append("[ACTUAL RESULT]:\n"+command.getTestResult().getActResult()+"\n");
        return buffer.toString();
    }

    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public static void main(String args[]){
    }


}
