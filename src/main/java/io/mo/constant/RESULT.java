package io.mo.constant;

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
    
    public static String ERROR_CAN_NOT_GET_CONNECTION_DESC =  "The mo-tester can not get valid connection to mo";

    public static String ERROR_EXECUTE_TIMEOUT_DESC =  "MO does not return result in %d ms. ";

    public static String RESULT_TYPE_PASS = "PASS";
    public static String RESULT_TYPE_FAILED = "FAILED";
    public static String RESULT_TYPE_ABNORMAL = "ABNORMAL";
    public static String RESULT_TYPE_IGNORED = "IGNORED";

    public static String RESULT_EMPTY_VALUE = "{EMPTY}";

    public static String COLUMN_SEPARATOR_TABLE = "\t";
    public static String COLUMN_SEPARATOR_SPACE = "    ";
    public static String COLUMN_SEPARATOR_SYSTEM = "THIS_IS_MO_SERPARATOR";

    public static int STMT_RESULT_TYPE_NONE = 3;
    public static int STMT_RESULT_TYPE_ERROR = 2;
    public static int STMT_RESULT_TYPE_SET = 1;

    public static int STMT_RESULT_TYPE_INIT = 0;
    public static int STMT_RESULT_TYPE_ABNORMAL = 4;

}

