package io.mo.result;

import io.mo.constant.COMMON;
import io.mo.constant.DATATYPE;
import io.mo.constant.RESULT;
import org.apache.log4j.Logger;

public class RSMetaData {
    private String[] columnNames; //column names
    private String[] columnLabels; // column labels

    private int pos = 0;


    private int columnCount = 0;
    private int[] types; // column data type,remain attr
    private int[] precisions; //column value precision,remain attr

    private String separator = RESULT.COLUMN_SEPARATOR_SPACE;

    private static Logger LOG = Logger.getLogger(RSMetaData.class.getName());

    public RSMetaData(int columnCount){
        this.columnCount = columnCount;
        columnNames = new String[columnCount];
        columnLabels = new String[columnCount];
        types = new int[columnCount];
        precisions = new int[columnCount];
    }



    public String getColumnName(int index) {
        return columnNames[index];
    }

    public String getColumnLable(int index) {
        return columnLabels[index];
    }

    public int getType(int index) {
        return types[index];
    }

    public int getPrecision(int index) {
        return precisions[index];
    }

    public int getColumnCount() {
        return columnCount;
    }

    public void addMetaInfo(String name,String label,int type,int precision){
        columnNames[pos] = name;
        columnLabels[pos] = label;
        types[pos] = type;
        precisions[pos] = precision;
        pos++;
    }

    public void addMetaInfo(String name,String label,int type){
        columnNames[pos] = name;
        columnLabels[pos] = label;
        types[pos] = type;
        precisions[pos] = 0;
        pos++;
    }

    public void addMetaInfo(String name,String label){
        columnNames[pos] = name;
        columnLabels[pos] = label;
        types[pos] = DATATYPE.TYPE_STRING;
        precisions[pos] = 0;
        pos++;
    }

    /**
     * compare whether two RSMetaData instances equal with each other
     * @param meta
     * @return
     */
    public boolean equals(RSMetaData meta){

        for(int i = 0; i < columnCount;i++){
            /*if(!this.columnNames[i].equals(meta.getColumnName(i))){
                LOG.error("The column name[index:"+i+"] does not equal with each other,one is ["+this.columnNames[i]+"],but the other is ["+meta.getColumnName(i)+"]");
                return false;
            }*/

            //if the metainfo of the rs is not required to be compared, ignore this label
            if(!COMMON.IS_COMPARE_META && containSpecialChar(this.columnLabels[i]))
                continue;

            if(!this.columnLabels[i].equalsIgnoreCase(meta.getColumnLable(i))){
                LOG.error("The column label[index:"+i+"] does not equal with each other,one is ["+this.columnLabels[i]+"],but the other is ["+meta.getColumnLable(i)+"]");
                return false;
            }

            /*
            if(this.types[i] != meta.getType(i)){
                LOG.error("The column type[index:"+i+"] does not equal with each other,one is ["+this.types[i]+"],but the other is ["+meta.getType(i)+"]");
                return false;
            }

            if(this.precisions[i] != meta.getPrecision(i)){
                LOG.error("The column precision[index:"+i+"] does not equal with each other,one is ["+this.precisions[i]+"],but the other is ["+meta.getPrecision(i)+"]");
                return false;
            }*/
        }
        return true;
    }

    public String getColumnLabels(){
        if(columnLabels.length == 0)
            return null;

        StringBuffer result = new StringBuffer();
        for(int i = 0; i < columnLabels.length - 1;i++){
            result.append(columnLabels[i] +  this.separator);
        }
        result.append(columnLabels[columnLabels.length - 1]);
        return result.toString();
    }



    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public boolean containSpecialChar(String str){
        if(str == null)
            return false;

        for(int i = 0; i < COMMON.SPECIAL_CHARS.length;i++){
            if(str.indexOf(COMMON.SPECIAL_CHARS[i]) != -1){
                return true;
            }
        }

        return false;
    }

}
