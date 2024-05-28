package io.mo.constant;

public class COMMON {

    public static String T_FILE_SUFFIX = ".sql";

    //if IGNORE_MODEL is true,menas all of sql commands that are marked with bvt:issue tag will be not executed
    public static boolean IGNORE_MODEL = false;

    public static boolean IS_COMPARE_META = true;

    public static boolean FORCE_UPDATE = false;
    
    public static String R_FILE_SUFFIX = ".result";

    public static int CONNECTION_ID = 0;//
    public static int DEFAULT_CONNECTION_NUM = 10;
    
    public static int DEFAULT_MAX_EXECUTE_TIME = 10000;

    public static String DEFAUT_DELIMITER = ";";
    public static String LINE_SEPARATOR = "\n";

    public static String NEW_DELIMITER_FLAG = "-- @delimiter ";
    public static String SYSTEM_CMD_FLAG = "-- @system ";

    public static String NEW_SESSION_START_FLAG = "-- @session:";
    public static int NEW_SEESION_DEFAULT_ID = 1;
    public static String NEW_SESSION_END_FLAG = "-- @session";
    public static String BVT_SKIP_FILE_FLAG = "-- @skip:issue#";
    public static String FUNC_SLEEP_FLAG = "-- @sleep:";

    public static String BVT_ISSUE_START_FLAG = "-- @bvt:issue#";
    public static String BVT_ISSUE_END_FLAG = "-- @bvt:issue";

    public static String SORT_KEY_INDEX_FLAG = "-- @sortkey:";

    public static String COLUMN_SEPARATOR_FLAG = "-- @separator:";
    public static String RESOURCE_PATH_FLAG = "\\$resources";
    public static String RESOURCE_LOCAL_PATH_FLAG= "\\$resources_local";
    
    public static String WAIT_FLAG = "-- @wait:";
    
    //if result is type of error, and not unique, can use this flag to regular match
    public static String REGULAR_MATCH_FLAG = "-- @pattern";
    
    public static String KAFKA_PRODUCE_START_FLAG = "-- @kafka:produce:";
    public static String KAFKA_PRODUCE_END_FLAG = "-- @kafka:produce";
    
    public static String IGNORE_COLUMN_FLAG = "-- @ignore:";

    public static String LOG_DIR = "log";
    public static String RESULT_DIR = "result";
    public static String CASES_DIR = "cases";
    public static String REPORT_DIR = "report";
    public static String RESOURCE_DIR = "resources";

    public static String RESOURCE_PATH = "./resources";
    public static String RESOURCE_LOCAL_PATH = "./resources";
    public static String UPDATE_RESULT_TEMPLATE = "Query OK, {num} row affected";

    public static int WAIT_TIMEOUT = 5000;
    
    public static int MAX_ROW_COUNT_IN_RS = 100;//the max row count in the resultset

    public static String THIS_IS_MO = "THIS IS MO";

    //事务相关
    public static String START_TRX = "begin";
    public static String COMMIT_TRX = "commit";
    public static String ROLLBACK_TRX = "rollback";
    
    public static boolean NEEDPPROF = false;

    public static String[] SPECIAL_CHARS = new String[]{"+","-","*","/","%","&",">","<","(",")","!","=","\'","\""};

    public static double SCALE_TOLERABLE_ERROR = 0.0000009;
    public static double INT_TOLERABLE_ERROR = 0.000000000000001;
}