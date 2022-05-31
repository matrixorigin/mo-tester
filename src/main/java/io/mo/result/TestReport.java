package io.mo.result;

import io.mo.cases.SqlCommand;
import io.mo.cases.TestCase;
import io.mo.cases.TestScript;
import io.mo.cases.TestSuite;
import io.mo.constant.COMMON;
import io.mo.constant.RESULT;
import io.mo.util.RunConfUtil;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.util.*;


public class TestReport {

    //private ArrayList<TestSuite> suites = new ArrayList<TestSuite>();
    Map<String,ArrayList<TestCase>> script_cases = new HashMap<String,ArrayList<TestCase>>();
    private ArrayList<TestCase> _cases = new ArrayList<TestCase>();
    private ArrayList<TestScript> scripts = new ArrayList<TestScript>();


    private int total_cmd = 0;
    private int error_cmd = 0;
    private int noexec_cmd = 0;

    private int total_case = 0;
    private int error_case = 0;
    private int noexec_case = 0;

    private static Logger LOG = Logger.getLogger(TestReport.class.getName());

    private String type = null;

    private int rate = 0;

    public TestReport(){
        File dir = new File(COMMON.REPORT_PATH);
        if(!dir.exists())
            dir.mkdir();

    }

    public void write(){

        //write typed-txt report
        writeTXTReport();

        //write typed-xml report
        writeXMLReport();
    }

    public void writeXMLReport(){
        int total = 0;
        int errors = 0;
        int failures = 0;
        int tests = 0;
        float time = 0;
        /*if(total == 0){
            LOG.info("There are no test results.................. ");
            return;
        }*/

        type = type = RunConfUtil.getType();
        if(type.equalsIgnoreCase("script")){
            total = scripts.size();
            Document doc = DocumentHelper.createDocument();
            Element testscripts = doc.addElement("testsuite");
            testscripts.addAttribute("name","test");

            OutputFormat xmlFormat = new OutputFormat();
            xmlFormat.setEncoding("UTF-8");
            xmlFormat.setNewlines(true);
            xmlFormat.setIndent(true);
            xmlFormat.setIndent("    ");
            for(int i = 0;i < total;i++){
                TestScript script = scripts.get(i);
                Element testscript = testscripts.addElement("testcase");
                testscript.addAttribute("classname",script.getId());
                testscript.addAttribute("name",script.getFileName());
                testscript.addAttribute("time",String.valueOf(script.getDuration()));

                time = time + script.getDuration();

                if(script.getExecStatus() == false){
                    Element failure  = testscript.addElement("failure");
                    failure.addAttribute("type",RESULT.ERROR_NOT_EXEC_CODE);
                    failure.addAttribute("message",RESULT.ERROR_NOT_EXEC_DESC);
                    continue;
                }


                StringBuffer error = new StringBuffer();
                for(int j = 0; j < script.getSize();j++){
                    SqlCommand command = script.getCommands().get(j);
                    if(!command.getResult().getResult().equalsIgnoreCase(RESULT.RESULT_TYPE_PASS)){
                        error.append(command.getResult().getRemark()+"\n");
                    }
                }
                if(error.length() > 0){
                    Element failure  = testscript.addElement("failure");
                    failure.addAttribute("type",RESULT.ERROR_CHECK_FAILED_CODE);
                    failure.addAttribute("message",RESULT.ERROR_CHECK_FAILED_DESC);
                    failure.addCDATA(error.toString());
                    errors++;
                }
                tests++;
            }

            testscripts.addAttribute("errors",String.valueOf(errors));
            testscripts.addAttribute("failures",String.valueOf(failures));
            testscripts.addAttribute("tests",String.valueOf(tests));
            testscripts.addAttribute("time",String.valueOf(time));

            try {
                PrintWriter pw = new PrintWriter(COMMON.REPORT_PATH+"/report.xml");
                XMLWriter xw = new XMLWriter(pw,xmlFormat);
                xw.write(doc);
                xw.flush();
                xw.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                System.out.println("Over");
            }
        }

        if(type.equalsIgnoreCase("suite")){
            total = _cases.size();
            Document doc = DocumentHelper.createDocument();
            Element testsuite = doc.addElement("testsuite");
            testsuite.addAttribute("name","test");

            OutputFormat xmlFormat = new OutputFormat();
            xmlFormat.setEncoding("UTF-8");
            xmlFormat.setNewlines(true);
            xmlFormat.setIndent(true);
            xmlFormat.setIndent("    ");
            for(int i = 0;i < total;i++){
                TestCase _case = _cases.get(i);
                Element testcase = testsuite.addElement("testcase");
               // testcase.addAttribute("classname",_case.getSuite().getId());
                testcase.addAttribute("name",_case.getDesc());
                testcase.addAttribute("time",String.valueOf(_case.getDuration()));
                time = time + _case.getDuration();

                if(!_case.getResult().getResult().equalsIgnoreCase(RESULT.RESULT_TYPE_PASS)){
                    Element failure  = testcase.addElement("failure");
                    failure.addAttribute("type",_case.getResult().getErrorCode());
                    failure.addAttribute("message",_case.getResult().getErrorDesc());
                    if(null != _case.getResult().getRemark()) {
                        //failure.addText(_case.getResult().getErrorDesc());
                        failure.addCDATA(_case.getResult().getRemark());
                    }
                /*if(null != _case.getResult().getExpResult()) {
                    failure.addText("[EXPECT]:");
                    failure.addText(_case.getResult().getExpResult());
                }
                if(null != _case.getResult().getActResult()) {
                    failure.addText("[ACTUAL]:");
                    failure.addText(_case.getResult().getActResult());
                }*/
                    failure.addText("\n");
                   // failure.addText("[CASE SCRIPT]:\n"+_case.getOrignal_script());
                    errors += 1;
                    continue;
                }
                tests += 1;
            }

            testsuite.addAttribute("errors",String.valueOf(errors));
            testsuite.addAttribute("failures",String.valueOf(failures));
            testsuite.addAttribute("tests",String.valueOf(tests));
            testsuite.addAttribute("time",String.valueOf(time));

            try {
                PrintWriter pw = new PrintWriter(COMMON.REPORT_PATH+"/report.xml");
                XMLWriter xw = new XMLWriter(pw,xmlFormat);
                xw.write(doc);
                xw.flush();
                xw.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                System.out.println("Over");
            }
        }

    }

    public void writeTXTReport(){
        type = type = RunConfUtil.getType();
        if(type.equalsIgnoreCase("script")){
            rate  = (total_cmd - error_cmd - noexec_cmd)*100/total_cmd;
            ArrayList<SqlCommand> e_commands = new ArrayList<SqlCommand>();
            try {
                BufferedWriter r_writer = new BufferedWriter(new FileWriter(COMMON.REPORT_PATH+"/report.txt"));
                BufferedWriter e_writer = new BufferedWriter(new FileWriter(COMMON.REPORT_PATH+"/error.txt"));
                BufferedWriter s_writer = new BufferedWriter(new FileWriter(COMMON.REPORT_PATH+"/success.txt"));
                r_writer.write(getReportSummaryTXT(total_cmd,error_cmd,noexec_cmd));
                LOG.info(getReportSummaryTXT(total_cmd,error_cmd,noexec_cmd).trim());
                for(int i = 0; i < scripts.size();i++){
                    TestScript script = scripts.get(i);
                    if(script.getExecStatus() == true){
                        r_writer.write(getScriptSummaryTXT(script));
                        LOG.info(getScriptSummaryTXT(script).trim());
                    }else {
                        r_writer.write("["+script.getId()+"] did not been executed because result file dose not exists.\n");
                        LOG.info(getScriptSummaryTXT(script).trim());
                    }
                    List<SqlCommand> errors = script.getErrorList();
                    for(int j = 0 ; j < errors.size();j++){
                        e_writer.write(getErrorInfo(errors.get(j)));
                        e_writer.newLine();
                        e_commands.add(errors.get(j));
                    }
                    List<SqlCommand> commands = script.getCommands();
                    for(SqlCommand command : commands){
                        if(command.getResult().getResult().equalsIgnoreCase(RESULT.RESULT_TYPE_PASS)){
                            s_writer.write("["+script.getFileName()+"]:"+command.getCommand().replaceAll("\r"," ").replaceAll("\n"," "));
                            s_writer.newLine();
                        }
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

        if(type.equalsIgnoreCase("suite")){
            rate  = (total_case - error_case - noexec_case)*100/total_case;
            try {
                BufferedWriter r_writer = new BufferedWriter(new FileWriter(COMMON.REPORT_PATH+"/report.txt"));
                BufferedWriter e_writer = new BufferedWriter(new FileWriter(COMMON.REPORT_PATH+"/error.txt"));
                r_writer.write(getReportSummaryTXT(total_case,error_case,noexec_case));
                LOG.info(getReportSummaryTXT(total_case,error_case,noexec_case).trim());

                for(Object key : script_cases.keySet()){
                    r_writer.write(getScriptSummaryTXT(key.toString()));
                    LOG.info(getScriptSummaryTXT(key.toString()).trim());
                }

                if(error_case > 0)
                    LOG.info("[ERROR SQL LIST]");
                for(int i = 0; i < _cases.size(); i++){
                    if(_cases.get(i).getResult().getResult().equalsIgnoreCase(RESULT.RESULT_TYPE_FAILED)){
                        e_writer.write(getErrorInfo(_cases.get(i)));
                        e_writer.newLine();
                        for(int j = 0; j < _cases.get(i).getCommands().size();j++){
                            SqlCommand command = _cases.get(i).getCommands().get(j);
                            if(command.getResult().getResult().equalsIgnoreCase(RESULT.RESULT_TYPE_FAILED))
                                LOG.info("["+command.getScriptFile()+"] SQL: "+command.getCommand().trim());
                        }
                    }
                }

                r_writer.flush();
                e_writer.flush();
                r_writer.close();
                e_writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    public void collect(TestCase _case){
        _cases.add(_case);
        total_case++;
        if(!_case.getResult().getResult().equalsIgnoreCase(RESULT.RESULT_TYPE_PASS)){
            if(_case.getResult().getResult().equalsIgnoreCase(RESULT.RESULT_TYPE_NOEXEC)
                || _case.getResult().getResult().equalsIgnoreCase(RESULT.RESULT_TYPE_NOEXEC))
                noexec_case++;

            if(_case.getResult().getResult().equalsIgnoreCase(RESULT.RESULT_TYPE_FAILED))
                error_case++;
        }

        if(script_cases.containsKey(_case.getFileName())){
            script_cases.get(_case.getFileName()).add(_case);
        }else {
            ArrayList<TestCase> cases = new ArrayList<TestCase>();
            cases.add(_case);
            script_cases.put(_case.getFileName(),cases);
        }
    }

    public void collect(TestSuite suite){
        //suites.add(suite);
        for(int i = 0; i < suite.getCases().size(); i++){
            collect(suite.getCases().get(i));
        }
    }

    public void collect(ArrayList<TestSuite> suites){
        for(int i = 0; i < suites.size(); i++){
            collect(suites.get(i));
        }
    }

    public void collect(TestScript script){
        scripts.add(script);
        total_cmd += script.getSize();
        error_cmd += script.getErrorCount();
        noexec_cmd += script.getNoExecList().size();
        if(!script.getExecStatus())
            noexec_cmd += script.getSize();

    }

    public String getReportSummaryTXT(int total,int error,int noexec){
        StringBuffer buffer = new StringBuffer();
        buffer.append("[SUMMARY] TOTAL : "+total);
        buffer.append(", ");
        buffer.append("SUCCESS : "+(total - error - noexec));
        buffer.append(", ");
        buffer.append("ERROR :"+error);
        buffer.append(", ");
        buffer.append("NOEXE :"+noexec);
        buffer.append(", ");
        buffer.append("SUCCESS RATE : "+(((total - error - noexec)*100/total))+"%\n");
        return buffer.toString();
    }

    public String getScriptSummaryTXT(TestScript script){
        StringBuffer buffer = new StringBuffer();
        buffer.append("["+script.getFileName()+"] TOTAL : " + script.getSize());
        buffer.append(", ");
        buffer.append("SUCCESS : "+(script.getSize() - script.getErrorCount() - script.getNoExecList().size()));
        buffer.append(", ");
        buffer.append("ERROR :"+script.getErrorCount());
        buffer.append(", ");
        buffer.append("NOEXE :"+script.getNoExecList().size());
        buffer.append(", ");
        if(script.getSize() != 0)
            buffer.append("SUCCESS RATE : "+((script.getSize() - script.getErrorCount() -script.getNoExecList().size())*100/script.getSize())+"%\n");
        else
            buffer.append("SUCCESS RATE : 0%\n");
        return buffer.toString();
    }

    public String getScriptSummaryTXT(String fileName){
        ArrayList<TestCase> cases = script_cases.get(fileName);
        int suit_total_case = cases.size();
        int suit_error_case = 0;
        int suit_noexec_case = 0;
        for(int i = 0; i < cases.size();i++){
            TestCase testCase = cases.get(i);
            if(!testCase.getResult().getResult().equalsIgnoreCase(RESULT.RESULT_TYPE_PASS)){
                if(testCase.getResult().getResult().equalsIgnoreCase(RESULT.RESULT_TYPE_NOEXEC))
                    suit_noexec_case++;

                if(testCase.getResult().getResult().equalsIgnoreCase(RESULT.RESULT_TYPE_FAILED))
                    suit_error_case++;
            }
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append("["+fileName+"] TOTAL : " + suit_total_case);
        buffer.append(", ");
        buffer.append("SUCCESS : "+(suit_total_case - suit_error_case - suit_noexec_case));
        buffer.append(", ");
        buffer.append("ERROR :"+suit_error_case);
        buffer.append(", ");
        buffer.append("NOEXE :"+suit_noexec_case);
        buffer.append(", ");
        buffer.append("SUCCESS RATE : "+((suit_total_case - suit_error_case -  suit_noexec_case)*100/suit_total_case)+"%\n");
        return buffer.toString();
    }

    public String getErrorInfo(SqlCommand command){
        StringBuffer buffer = new StringBuffer();
        buffer.append("[ERROR]\n");
        buffer.append("[SCRIPT   FILE]: "+command.getScriptFile()+"\n");
        buffer.append("[SQL STATEMENT]: "+command.getCommand()+"\n");
        buffer.append("[EXPECT RESULT]:\n"+command.getResult().getExpResult()+"\n");
        buffer.append("[ACTUAL RESULT]:\n"+command.getResult().getActResult()+"\n");
        return buffer.toString();
    }

    public String getErrorInfo(TestCase _case){
        StringBuffer buffer = new StringBuffer();
        buffer.append("[ERROR]\n");
        buffer.append("[TESTCASE FILE]: "+_case.getFileName()+"\n");
        buffer.append("[TESTCASE DESC]: "+_case.getDesc()+"\n");
        buffer.append("[TESTCASE  SQL]:\n"+_case.getSqlCommands()+"\n");
        buffer.append("[ERROR INFO]:\n"+_case.getRemark()+"\n");
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
