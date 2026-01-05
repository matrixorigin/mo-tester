package io.mo.constant;

import java.sql.Types;

public class RESULT {
    public static String SUCCESS_CODE = "0000";
    public static String ERROR_CHECK_FAILED_CODE = "0001";
    public static String ERROR_CASE_IGNORE_CODE = "0002";
    public static String ERROR_CONNECTION_LOST_CODE = "0003";
    public static String ERROR_PARSE_RESULT_FILE_FAILED_CODE = "0100";

    public static String ERROR_EXECUTE_TIMEOUT_CODE = "0101";

    public static String ERROR_CAN_NOT_GET_CONNECTION_CODE = "0102";

    public static String ERROR_CHECK_FAILED_DESC = "The actual result is not in accordance with the expectation.More,See the detail.";
    public static String ERROR_PARSE_RESULT_FILE_FAILED_DESC = "The expected result file file can not be parsed.Please check this.";
    public static String ERROR_CASE_IGNORE_DESC = "The command is marked to ignore flag,do not be executed.";
    public static String ERROR_CONNECTION_LOST_DESC = "The connection has been lost because of unknown reason.";
    public static String ERROR_RESULTSET_INVALID_DESC = "The result set is not correct because of unexpected reason,Message: ";

    public static String ERROR_CAN_NOT_GET_CONNECTION_DESC = "The mo-tester can not get valid connection to mo";

    public static String ERROR_EXECUTE_TIMEOUT_DESC = "MO does not return result in %d ms. ";

    public static String RESULT_TYPE_PASS = "PASS";
    public static String RESULT_TYPE_FAILED = "FAILED";
    public static String RESULT_TYPE_ABNORMAL = "ABNORMAL";
    public static String RESULT_TYPE_IGNORED = "IGNORED";

    public static String COLUMN_SEPARATOR_TABLE = "\t";
    public static String COLUMN_SEPARATOR_SPACE = "    ";

    // two spaces + broken bar(html: &#166; unicode: U+00A6) + two spaces
    public static String COLUMN_SEPARATOR_NEW = "  ¦  ";
    // two spaces + musical barline(html: &#119040; unicode: U+1D100) + newline
    public static String ROW_SEPARATOR_NEW = "  𝄀\n";
    public static String FULL_HEADER_LEAD = "➤ ";

    public static int STMT_RESULT_TYPE_NONE = 3;
    public static int STMT_RESULT_TYPE_ERROR = 2;
    public static int STMT_RESULT_TYPE_SET = 1;
    public static int STMT_RESULT_TYPE_INIT = 0;

    public static int STMT_RESULT_TYPE_ABNORMAL = 4;

    public static final java.util.Map<Integer, String> TYPE_NAME_MAP;
    static {
        java.util.Map<Integer, String> map = new java.util.HashMap<>();
        map.put(Types.BIT, "BIT"); // -7
        map.put(Types.TINYINT, "TINYINT"); // -6
        map.put(Types.SMALLINT, "SMALLINT"); // 5
        map.put(Types.INTEGER, "INTEGER"); // 4
        map.put(Types.BIGINT, "BIGINT"); // -5
        map.put(Types.FLOAT, "FLOAT"); // 6
        map.put(Types.REAL, "REAL"); // 7
        map.put(Types.DOUBLE, "DOUBLE"); // 8
        map.put(Types.NUMERIC, "NUMERIC"); // 2
        map.put(Types.DECIMAL, "DECIMAL"); // 3
        map.put(Types.CHAR, "CHAR"); // 1
        map.put(Types.VARCHAR, "VARCHAR"); // 12
        map.put(Types.LONGVARCHAR, "LONGVARCHAR"); // -1
        map.put(Types.DATE, "DATE"); // 91
        map.put(Types.TIME, "TIME"); // 92
        map.put(Types.TIMESTAMP, "TIMESTAMP"); // 93
        map.put(Types.BINARY, "BINARY"); // -2
        map.put(Types.VARBINARY, "VARBINARY"); // -3
        map.put(Types.LONGVARBINARY, "LONGVARBINARY"); // -4
        map.put(Types.NULL, "NULL"); // 0
        map.put(Types.OTHER, "OTHER"); // 1111
        map.put(Types.JAVA_OBJECT, "JAVA_OBJECT"); // 2000
        map.put(Types.DISTINCT, "DISTINCT"); // 2001
        map.put(Types.STRUCT, "STRUCT"); // 2002
        map.put(Types.ARRAY, "ARRAY"); // 2003
        map.put(Types.BLOB, "BLOB"); // 2004
        map.put(Types.CLOB, "CLOB"); // 2005
        map.put(Types.REF, "REF"); // 2006
        map.put(Types.DATALINK, "DATALINK"); // 70
        map.put(Types.BOOLEAN, "BOOLEAN"); // 16
        map.put(Types.ROWID, "ROWID"); // -8
        map.put(Types.NCHAR, "NCHAR"); // -15
        map.put(Types.NVARCHAR, "NVARCHAR"); // -9
        map.put(Types.LONGNVARCHAR, "LONGNVARCHAR"); // -16
        map.put(Types.NCLOB, "NCLOB"); // 2011
        map.put(Types.SQLXML, "SQLXML"); // 2009
        TYPE_NAME_MAP = java.util.Collections.unmodifiableMap(map);
    }
}
