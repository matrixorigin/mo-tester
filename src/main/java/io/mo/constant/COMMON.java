package io.mo.constant;

public class COMMON {

    public static String[] RESOURCES_DIR = new String[]{"conf","cases","checkpoints","resources","result"};

    public static String T_FILE_SUFFIX = ".sql";

    public static boolean IGNORE_MODEL = false;

    public static boolean IS_COMPARE_META = true;

    public static String R_FILE_SUFFIX = ".result";

    public static int CONNECTION_ID = 0;//
    public static int DEFAULT_CONNECTION_NUM = 10;

    public static String DEFAUT_DELIMITER = ";";
    public static String LINE_SEPARATOR = "\n";

    public static String SUITE_FLAG = "-- @suite";
    public static String SETUP_SUITE_FLAG = "-- @setup";
    public static String TEARDOWN_SUITE_FLAG = "-- @teardown";

    public static String CASE_START_FLAG = "-- @case";
    public static String CASE_DESC_FLAG = "-- @desc:";
    public static String CASE_LABEL_FLAG = "-- @label:";

    public static String DELIMITER_FLAGE = "-- @delimiter:";

    public static String NEW_SESSION_START_FLAG = "-- @session{";
    public static String NEW_SESSION_END_FLAG = "-- session}";

    public static String IGNORE_START_FLAG = "-- @ignore{";
    public static String IGNORE_END_FLAG = "-- @ignore}";

    public static String LOG_PATH = "log";
    public static String RESULT_PATH = "result";
    public static String TEST_PATH = "test";
    public static String CASES_PATH = "cases";
    public static String REPORT_PATH = "report";

    public static String UPDATE_RESULT_TEMPLATE = "Query OK, {num} row affected";


    public static int MAX_ROW_COUNT_IN_RS = 100;//the max row count in the resultset

    //事务相关
    public static String START_TRX = "begin";
    public static String COMMIT_TRX = "commit";
    public static String ROLLBACK_TRX = "rollback";







}