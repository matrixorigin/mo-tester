package io.mo.result;

import io.mo.constant.COMMON;
import io.mo.constant.RESULT;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;

public class RSMetaData {
    private static Logger LOG = Logger.getLogger(RSMetaData.class.getName());
    private static int VARFlag = 715827882;

    private String[] columnNames; // column names
    private String[] columnLabels; // column labels

    private int pos = 0;

    @Getter
    @Setter
    private boolean fullMetaInfo = false;

    @Getter
    private int columnCount = 0;
    private int[] types; // column data type,remain attr
    private int[] precisions; // column value precision,remain attr
    private int[] scales; // column value scale,remain attr

    public RSMetaData(int columnCount) {
        this.columnCount = columnCount;
        columnNames = new String[columnCount];
        columnLabels = new String[columnCount];
        types = new int[columnCount];
        precisions = new int[columnCount];
        scales = new int[columnCount];
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

    public int getScale(int index) {
        return scales[index];
    }


    public void addMetaInfo(String name, String label, int type, int precision, int scale) {
        columnNames[pos] = name;
        columnLabels[pos] = label;
        types[pos] = type;
        precisions[pos] = precision;
        scales[pos] = scale;
        pos++;
    }

    public boolean equals(RSMetaData meta) {
        for (int i = 0; i < columnCount; i++) {
            if (this.fullMetaInfo) {
                String f1 = this.stringAt(i);
                String f2 = meta.stringAt(i);
                if (!f1.equalsIgnoreCase(f2)) {
                    LOG.error("The meta info does not equal with each other,one is ["
                            + f1 + "],but the other is [" + f2 + "]");
                    return false;
                }
            } else if (!containSpecialChar(this.columnLabels[i]))  {
                if (!this.columnLabels[i].equalsIgnoreCase(meta.getColumnLable(i))) {
                    LOG.error("The column label[index:" + i + "] does not equal with each other,one is ["
                            + this.columnLabels[i] + "],but the other is [" + meta.getColumnLable(i) + "]");
                    return false;
                }
            }
        }
        return true;
    }

    public String stringAt(int index) {
        StringBuilder result = new StringBuilder();
        result.append(columnLabels[index])
                  .append("[").append(types[index])
                  .append(",").append(precisions[index] == VARFlag ? -1 : precisions[index])
                  .append(",").append(scales[index])
                  .append("]");
        return result.toString();
    }

    public String fullString() {
        StringBuilder result = new StringBuilder();
        result.append(RESULT.FULL_HEADER_LEAD);
        for (int i = 0; i < columnCount; i++) {
            result.append(stringAt(i));
            if (i < columnCount - 1) {
                result.append(RESULT.COLUMN_SEPARATOR_NEW);
            }
        }
        return result.toString();
    }

    public boolean containSpecialChar(String str) {
        return str != null && java.util.Arrays.stream(COMMON.SPECIAL_CHARS).anyMatch(str::contains);
    }

}
