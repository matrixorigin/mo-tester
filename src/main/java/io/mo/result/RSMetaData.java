package io.mo.result;

import io.mo.constant.COMMON;
import io.mo.constant.RESULT;
import org.apache.log4j.Logger;

public class RSMetaData {
    private String[] columnNames; // column names
    private String[] columnLabels; // column labels

    private int pos = 0;

    private int columnCount = 0;
    private int[] types; // column data type,remain attr
    private int[] precisions; // column value precision,remain attr

    private static Logger LOG = Logger.getLogger(RSMetaData.class.getName());

    public RSMetaData(int columnCount) {
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

    public void addMetaInfo(String name, String label, int type, int precision) {
        columnNames[pos] = name;
        columnLabels[pos] = label;
        types[pos] = type;
        precisions[pos] = precision;
        pos++;
    }

    public boolean equals(RSMetaData meta) {
        if (!COMMON.IS_COMPARE_META) {
            return true;
        }
        for (int i = 0; i < columnCount; i++) {
            // if the metainfo of the rs is not required to be compared, ignore this label
            if (containSpecialChar(this.columnLabels[i]))
                continue;

            if (!this.columnLabels[i].equalsIgnoreCase(meta.getColumnLable(i))) {
                LOG.error("The column label[index:" + i + "] does not equal with each other,one is ["
                        + this.columnLabels[i] + "],but the other is [" + meta.getColumnLable(i) + "]");
                return false;
            }
        }
        return true;
    }

    public String getColumnLabels() {
        return String.join(RESULT.COLUMN_SEPARATOR_NEW, columnLabels);
    }

    public boolean containSpecialChar(String str) {
        return str != null && java.util.Arrays.stream(COMMON.SPECIAL_CHARS).anyMatch(str::contains);
    }

}
