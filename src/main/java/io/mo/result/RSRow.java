package io.mo.result;

import io.mo.constant.RESULT;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;

import java.util.ArrayList;

public class RSRow {
    private static Logger LOG = Logger.getLogger(RSRow.class.getName());

    @Getter
    private ArrayList<RSCell> cells; // values of each cell in a row
    
    @Getter
    private String rowText = null; // concat all values to a plain text in the order of columns and is used to sort rows
    
    @Getter
    @Setter
    private int index = 0;// the row index in the RSSet

    public RSRow(int colCount) {
        cells = new ArrayList<RSCell>();
        rowText = "";
    }

    public void addCell(RSCell cell) {
        cells.add(cell);
        rowText += cell.toString();
    }

    public String getRowCellString(int index) {
        return cells.get(index).toString();
    }



    public boolean equals(RSRow rsRow) {
        // At first,compare texts directly,,if two texts equal,return true
        if (this.rowText.equals(rsRow.getRowText()))
            return true;

        // Then,compare whether the column counts are equal to each other
        if (cells.size() != rsRow.getCells().size()) {
            LOG.error("The column count does not equal with each other,one is [" + cells.size() + "],but the other is ["
                    + rsRow.getCells().size() + "]");
            return false;
        }
        // Or,compare each values
        for (int i = 0; i < cells.size(); i++) {
            RSCell ct = cells.get(i);
            RSCell cc = rsRow.getCells().get(i);
            if (!ct.isNeedcheck() || !cc.isNeedcheck()) {
                continue;
            }
            if (!ct.equals(cc)) {
                LOG.error("The value of [row:" + index + ",column:" + i + "] does not equal with each other,one is ["
                        + ct.toString() + "],but the other is [" + cc.toString() + "]");
                return false;
            }
        }
        return true;
    }

    public int compareTo(RSRow row) {
        return this.rowText.compareTo(row.rowText);
    }

    /**
     * compare the order between this and th other row by the attribute [text]
     */
    public int compareTo(RSRow row, ArrayList<Integer> sortKeyIndexs) {
        String sortKeyText1 = "";
        String sortKeyText2 = "";
        for (int i = 0; i < sortKeyIndexs.size(); i++) {
            int index = sortKeyIndexs.get(i);
            // if index is out of the cells.size,it is invalid index,ignore it
            if (index < cells.size()) {
                sortKeyText1 += this.getRowCellString((int) sortKeyIndexs.get(i));
                sortKeyText2 += row.getRowCellString((int) sortKeyIndexs.get(i));
            }
        }

        if (sortKeyText1.equals(sortKeyText2))
            return this.rowText.compareTo(row.rowText);
        else
            return 0;
    }

    public String toString() {
        return cells.stream().map(Object::toString).reduce((a, b) -> a + RESULT.COLUMN_SEPARATOR_NEW + b).orElse(null);
    }
}
