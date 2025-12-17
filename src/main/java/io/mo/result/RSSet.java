package io.mo.result;

import io.mo.cases.SqlCommand;
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

    private String abnormalError; // to store the abnormal reason why the RSSet instanse is generated failed.

    private SqlCommand command;

    public SqlCommand getCommand() {
        return command;
    }

    public void setCommand(SqlCommand command) {
        this.command = command;
    }

    private static Logger LOG = Logger.getLogger(RSSet.class.getName());

    public RSSet() {
    }

    public void setMeta(RSMetaData meta) {
        this.meta = meta;
    }

    public void addRow(RSRow rsRow) {
        rows.add(rsRow);
        rsRow.setIndex(rows.size() - 1);
    }

    // ResultSet 来自 SQL 语句的返回，这里在构造 ActualResult
    public RSSet(ResultSet resultSet, SqlCommand command) {
        this.command = command;
        ResultSetMetaData md = null;
        try {
            md = resultSet.getMetaData();
            int colsCnt = md.getColumnCount();
            this.meta = new RSMetaData(colsCnt);
            for (int i = 0; i < colsCnt; ++i) {
                this.meta.addMetaInfo(
                    md.getColumnName(i + 1), 
                    md.getColumnLabel(i + 1),
                    md.getColumnType(i + 1), 
                    md.getPrecision(i + 1),
                    md.getScale(i + 1));
            }

            System.out.println("meta: " + this.meta.fullString());

            while (resultSet.next()) {
                RSRow rsRow = new RSRow(colsCnt);
                for (int j = 0; j < colsCnt; ++j) {
                    RSCell rsCell = new RSCell();
                    if (this.command.getIgnoreColumns().size() != 0) {
                        if (this.command.getIgnoreColumns().contains(j)) {
                            rsCell.setNeedcheck(false);
                        }
                    }
                    String value = resultSet.getString(j + 1);
                    if (value == null)
                        value = "null";
                    
                    // compatibility for old result file
                    StmtResult expResult = this.command.getExpResult();
                    if (expResult != null && !expResult.getExpectRSText().contains(RESULT.ROW_SEPARATOR_NEW)) {
                        //if the value contain \n, replace to string "\n"
                        value = value.replaceAll("\n","\\\\n");
                    }
                  
                    rsCell.setValue(value);
                    rsCell.setType(this.meta.getType(j));
                    rsCell.setPrecision(this.meta.getPrecision(j));
                    rsCell.setScale(this.meta.getScale(j));
                    rsRow.addCell(rsCell);
                }
                this.addRow(rsRow);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            this.abnormalError = RESULT.ERROR_RESULTSET_INVALID_DESC + e.getMessage();
        } catch (SQLException e) {
            e.printStackTrace();
            this.abnormalError = RESULT.ERROR_RESULTSET_INVALID_DESC + e.getMessage();
        }
    }
   

    public void addSortKeyIndex(int index) {
        sortKeyIndexs.add(index);
    }

    public ArrayList<RSRow> getRows() {
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
    public void sort() {
        rows.sort(Comparator.comparing(RSRow::getRowText));
    }

    /**
     * sort rows, but need to keep sort-key columns remainning original order
     */
    public void sort(ArrayList<Integer> sortKeyIndexs) {
        rows.sort((a, b) -> a.compareTo(b, sortKeyIndexs));
    }

    public boolean equals(RSSet other) {
        // if need to check the meatainfo of resultset,compare whether meta equals with
        // each other
        if (COMMON.IS_COMPARE_META && !this.meta.equals(other.meta)) {
            return false;
        }
        // if row count does not equal,return false
        if (this.rows.size() != other.getRows().size()) {
            LOG.error("The row count does not equal with each other, one is " + rows.size() + ", the other is "
                    + other.getRows().size());
            return false;
        }
        // sort the rows
        if (sortKeyIndexs.size() == 0) {
            this.sort();
            other.sort();
        } else {
            this.sort(sortKeyIndexs);
            other.sort(sortKeyIndexs);
        }
        for (int i = 0; i < rows.size(); i++) {
            if (!this.rows.get(i).equals(other.getRows().get(i))) {
                return false;
            }
        }
        return true;
    }


    public String toString() {
        if (this.abnormalError != null)
            return abnormalError;

        ArrayList<String> rowsText = new ArrayList<>();
        rowsText.add(this.meta.fullString());
        for (int i = 0; i < rows.size(); i++) {
            rowsText.add(rows.get(i).toString());
        }

        return String.join(RESULT.ROW_SEPARATOR_NEW, rowsText);
    }

}
