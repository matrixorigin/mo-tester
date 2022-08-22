package io.mo.result;

import io.mo.cases.SqlCommand;
import io.mo.constant.RESULT;
import org.apache.log4j.Logger;

public class StmtResult {
    /**
     * result type, value can be:
     * 0,means result is type of ResultSet
     * 1,means result is type of ErrorMessage
     * 2,means result is NULL
     */
    private int type = RESULT.STMT_RESULT_TYPE_INIT;
    private String errorMessage = null;

    private RSSet rsSet = null;

    private SqlCommand command;

    //restore the orginal result text read from the test result file
    //if instance is generated by the real ResultSet from the jdbc, it does not make any senses
    private String orginalRSText = null;

    private static Logger LOG = Logger.getLogger(StmtResult.class.getName());

    public StmtResult(String errorMessage){
        this.type = RESULT.STMT_RESULT_TYPE_ERROR;
        this.errorMessage = errorMessage;
    }

    public StmtResult(RSSet rsSet){
        this.rsSet = rsSet;
        if(this.command != null) {
            if (command.getSeparator().equals("both") || command.getSeparator().equals("space"))
                this.rsSet.setSeparator(RESULT.COLUMN_SEPARATOR_SPACE);
            else
                this.rsSet.setSeparator(RESULT.COLUMN_SEPARATOR_TABLE);

            //add sort key indexes
            for(int i = 0; i < command.getSortKeyIndexs().size();i++){
                this.rsSet.addSortKeyIndex(command.getSortKeyIndexs().get(i));
            }
        }
        if(rsSet.getAbnormalError() == null)
            this.type = RESULT.STMT_RESULT_TYPE_SET;
        else
            this.type = RESULT.STMT_RESULT_TYPE_ABNORMAL;
    }

    public StmtResult(){
    }

    /**
     * compare whether this equals the argument stmtResult
     * @param stmtResult
     * @return
     */
    public boolean equals(StmtResult stmtResult){
        if(this.type != stmtResult.getType()){
            return false;
        }else {
            if(this.type == RESULT.STMT_RESULT_TYPE_ERROR){
                if(this.errorMessage == null){
                    if(stmtResult.getErrorMessage() != null){
                        return false;
                    }
                }
                
                if(!this.errorMessage.trim().equals(stmtResult.getErrorMessage().trim())){
                    return false;
                }
            }

            if(this.type == RESULT.STMT_RESULT_TYPE_SET){

                if(this.rsSet.getAbnormalError() != null || stmtResult.getRsSet().getAbnormalError() != null){
                    return false;
                }

                //System.out.println("act : " + stmtResult.getRsSet().toString());
                if(!this.rsSet.equals(stmtResult.getRsSet())){
                    return false;
                }
            }
        }

        return true;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public RSSet getRsSet() {
        return rsSet;
    }

    public void setRsSet(RSSet rsSet) {
        this.rsSet = rsSet;
        if(this.command != null) {
            if (command.getSeparator().equals("both") || command.getSeparator().equals("space"))
                this.rsSet.setSeparator(RESULT.COLUMN_SEPARATOR_SPACE);
            else
                this.rsSet.setSeparator(RESULT.COLUMN_SEPARATOR_TABLE);

            //add sort key indexes
            for(int i = 0; i < command.getSortKeyIndexs().size();i++){
                this.rsSet.addSortKeyIndex(command.getSortKeyIndexs().get(i));
            }
        }
    }

    public String getOrginalRSText() {
        return orginalRSText;
    }

    public void setOrginalRSText(String orginalRSText) {
        this.orginalRSText = orginalRSText;
        //this.rsSet = ResultParser.convertToRSSet(orginalRSText,command.getSeparator());
    }

    public SqlCommand getCommand() {
        return command;
    }

    public void setCommand(SqlCommand command) {
        this.command = command;
        if(this.rsSet != null) {
            if (command.getSeparator().equals("both") || command.getSeparator().equals("space"))
                this.rsSet.setSeparator(RESULT.COLUMN_SEPARATOR_SPACE);
            else
                this.rsSet.setSeparator(RESULT.COLUMN_SEPARATOR_TABLE);

            //add sort key indexes
            for(int i = 0; i < command.getSortKeyIndexs().size();i++){
                this.rsSet.addSortKeyIndex(command.getSortKeyIndexs().get(i));
            }
        }
    }

    public String toString(){
        if(this.type == RESULT.STMT_RESULT_TYPE_SET)
            return rsSet.toString();

        else if(this.type == RESULT.STMT_RESULT_TYPE_ERROR)
            return errorMessage;

        else if(this.type == RESULT.STMT_RESULT_TYPE_NONE)
            return null;

        else
            return orginalRSText;
    }
}
