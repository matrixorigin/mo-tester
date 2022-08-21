package io.mo.result;

import io.mo.constant.RESULT;
import org.apache.log4j.Logger;

import java.util.ArrayList;

public class RSRow {
    private ArrayList<RSCell> cells; //values of each cell in a row
    private String text = null; //concat all values to a plain text in the order of columns and is used to sort rows

    private String separator = RESULT.COLUMN_SEPARATOR_SPACE;

    private int index = 0;//the row index in the RSSet
    private static Logger LOG = Logger.getLogger(RSRow.class.getName());

    public RSRow(int colCount){
        cells = new ArrayList<RSCell>();
        text = "";
    }

    public void addCell(RSCell cell){
        cells.add(cell);
        text += cell.toString();
    }

    public RSCell getCell(int index){
        return cells.get(index);
    }
    
    public String getRowValue(int index){
        return cells.get(index).toString();
    }

    public String getRowText(){
        return text;
    }

    public ArrayList<RSCell> getRowValues(){
        return cells;
    }

    /**
     * compare whether this equals the argument rsRow
     * @param rsRow
     * @return
     */
    public boolean equals(RSRow rsRow){

        //At first,compare texts directly,,if two texts equal,return true
        if(this.text.equals(rsRow.getRowText()))
            return true;
        //Or,compare each values
        for(int i = 0; i < cells.size(); i++){
            RSCell ct = cells.get(i);
            RSCell cc = rsRow.getRowValues().get(i);
            if(!ct.equals(cc)){
                LOG.error("The value of [row:" + index + ",column:" + i + "] does not equal with each other,one is ["+ct.toString()+"],but the other is ["+cc.toString()+"]");
                return false;
            }
        }
        return true;
    }

    /**
     * compare the order between this and th other row by the attribute [text]
     */
    public int compareTo(RSRow row){
        return this.text.compareTo(row.text);
    }

    /**
     * compare the order between this and th other row by the attribute [text]
     */
    public int compareTo(RSRow row,ArrayList sortKeyIndexs){
        String sortKeyText1 = "";
        String sortKeyText2 = "";
        for(int i = 0; i < sortKeyIndexs.size();i++){
            int index = (int)sortKeyIndexs.get(i);
            //if index is out of the cells.size,it is invalid index,ignore it
            if( index < cells.size()) {
                sortKeyText1 += this.getRowValue((int) sortKeyIndexs.get(i));
                sortKeyText2 += row.getRowValue((int) sortKeyIndexs.get(i));
            }
        }

        if(sortKeyText1.equals(sortKeyText2))
            return this.text.compareTo(row.text);
        else
            return 0;
    }

    public String toString(){
        if(cells.size() == 0)
            return null;

        StringBuffer buffer = new StringBuffer();
        for(int i = 0; i < cells.size() - 1;i++){
            buffer.append(cells.get(i).toString() + this.separator);
        }

        buffer.append(cells.get(cells.size() - 1).toString());
        return buffer.toString();
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

}
