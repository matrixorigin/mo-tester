package io.mo.result;

import io.mo.constant.COMMON;
import io.mo.constant.RESULT;
import org.apache.log4j.Logger;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;

public class RSSet {

    private RSMetaData meta;
    private ArrayList<RSRow> rows = new ArrayList<RSRow>();

    private ArrayList<Integer> sortKeyIndexs = new ArrayList<Integer>();

    private String abnormalError; //to store the abnormal reason why the RSSet instanse is generated failed.

    private String separator = RESULT.COLUMN_SEPARATOR_SPACE;

    private static Logger LOG = Logger.getLogger(RSSet.class.getName());

    public RSSet(){

    }

    public RSSet(ResultSet resultSet){
        ResultSetMetaData md = null;
        StringBuffer result = new StringBuffer();
        try {
            md = resultSet.getMetaData();
            int cols = md.getColumnCount();
            this.meta = new RSMetaData(cols);
            for(int i = 0; i < cols; ++i) {
                this.meta.addMetaInfo(md.getColumnName(i + 1),md.getColumnLabel(i + 1),
                        md.getColumnType(i + 1),md.getPrecision(i + 1));
            }

            int i = 0;
            while(resultSet.next()) {
                RSRow rsRow = new RSRow(cols);
                for(int j = 0; j < cols; ++j) {
                    RSCell rsCell = new RSCell<String>();
                    String value = resultSet.getString(j + 1);
                    if(value == null)
                        value = "null";
                    //if the value contain \n, replace to string "\n"
                    value = value.replaceAll("\n","\\\\n");
                    rsCell.setValue(value);
                    rsCell.setType(this.meta.getType(j));
                    rsCell.setPrecision(this.meta.getPrecision(j));
                    rsRow.addCell(rsCell);
                }
                this.addRow(rsRow);
            }
        } catch(NumberFormatException e){
            e.printStackTrace();
            this.abnormalError =  RESULT.ERROR_RESULTSET_INVALID_DESC + e.getMessage();
        }catch (SQLException e) {
            e.printStackTrace();
            this.abnormalError = RESULT.ERROR_RESULTSET_INVALID_DESC + e.getMessage();
        }
    }


    public RSSet(RSMetaData meta){
        this.meta = meta;
    }

    public RSMetaData getMeta() {
        return meta;
    }

    public void setMeta(RSMetaData meta) {
        this.meta = meta;
        this.meta.setSeparator(this.separator);
    }

    public void addRow(RSRow rsRow){
        rows.add(rsRow);
        rsRow.setIndex(rows.size() - 1);
    }

    public void addSortKeyIndex(int index){
        sortKeyIndexs.add(index);
    }

    public ArrayList<RSRow> getRows(){
        return rows;
    }

    public String getAbnormalError() {
        return abnormalError;
    }

    public void setAbnormalError(String abnormalError) {
        this.abnormalError = abnormalError;
    }

    /**
     * sort rows by attribute [text] of the RSRow
     */
    public void sort(){
        rows.sort(new Comparator<RSRow>() {
            @Override
            public int compare(RSRow o1, RSRow o2) {
                return o1.compareTo(o2);
            }
        });
    }

    /**
     * sort rows, but need to keep sort-key columns remainning original order
     */
    public void sort(ArrayList sortKeyIndexs) {
        rows.sort(new Comparator<RSRow>() {
            @Override
            public int compare(RSRow o1, RSRow o2) {
                return o1.compareTo(o2,sortKeyIndexs);
            }
        });
    }

    /**
     * compare whether two RSSet instances equal with each other
     * @param set
     * @return
     */
    public boolean equals(RSSet set){
        //if need to check the meatainfo of resultset,compare whether meta equals with each other
        if(COMMON.IS_COMPARE_META ){
           if(!this.meta.equals(set.getMeta()))
               return false;
        }
        //if row count does not equal,return false
        if(this.rows.size() != set.getRows().size()){
            LOG.error("The row count does not equal with each other, one is " + rows.size() + ", the other is " + set.getRows().size());
            return false;
        }
        //sort the rows
        if(sortKeyIndexs.size() == 0){
            this.sort();
            set.sort();
        }else {
            this.sort(sortKeyIndexs);
            set.sort(sortKeyIndexs);
        }

        for(int i = 0; i < rows.size();i++){
            if(!this.rows.get(i).equals(set.getRows().get(i))){
                return false;
            }
        }

        return true;
    }



    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
        this.meta.setSeparator(this.separator);
        for(RSRow row : rows){
            row.setSeparator(this.separator);
        }
    }

    public String toString(){
        if(this.abnormalError != null)
            return abnormalError;

        StringBuffer result = new StringBuffer();
        result.append(this.meta.getColumnLabels());
        if(rows.size() == 0)
            return result.toString();
        result.append("\n");
        for(int i = 0; i < rows.size() - 1;i++){
            result.append(rows.get(i).toString() + "\n");
        }
        result.append(rows.get(rows.size() -1 ).toString());
        return result.toString();
    }

}
