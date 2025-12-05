package io.mo.constant;

public class COMMON {

    public static String T_FILE_SUFFIX = ".sql";

    //if IGNORE_MODEL is true,menas all of sql commands that are marked with bvt:issue tag will be not executed
    public static boolean IGNORE_MODEL = false;

    public static boolean IS_COMPARE_META = true;
    
    public static String R_FILE_SUFFIX = ".result";

    public static int CONNECTION_ID = 0;//
    public static int DEFAULT_CONNECTION_NUM = 1000;
    
    public static int DEFAULT_MAX_EXECUTE_TIME = 10000;

    public static String DEFAUT_DELIMITER = ";";
    public static String LINE_SEPARATOR = "\n";

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
    
    public static String WAIT_FLAG = "-- @wait:";
    
    //if result is type of error, and not unique, can use this flag to regular match
    public static String REGULAR_MATCH_FLAG = "-- @pattern";
    
    public static String REGEX_FLAG = "-- @regex";
    
    public static String IGNORE_COLUMN_FLAG = "-- @ignore:";
    
    //if result only need to check whether it contains some keywords, can use this flag
    public static String HINT_FLAG = "-- @hint:";

    public static String LOG_DIR = "log";
    public static String RESULT_DIR = "result";
    public static String CASES_DIR = "cases";

    /**
     * 资源路径相关常量
     * 
     * 约定：resources 和 cases 目录必须位于同一层级
     * 约定高于配置：系统会根据用例路径自动推导资源路径，无需手动配置
     * 
     * 目录结构约定示例：
     *   project/
     *   ├── cases/          # 测试用例目录
     *   │   ├── array/
     *   │   │   └── array.sql
     *   │   └── function/
     *   │       └── test.sql
     *   └── resources/      # 资源文件目录（与 cases 同级）
     *       ├── data/
     *       │   └── test.csv
     *       └── config/
     *           └── config.json
     * 
     * 路径推导示例：
     *   - 用例路径: /project/cases/array/array.sql
     *     推导结果: /project/resources
     *   
     *   - 用例路径: ./cases/function/test.sql
     *     推导结果: ./resources
     */
    
    /** SQL 命令中的占位符，用于替换为资源路径（全局路径） */
    public static String RESOURCE_PATH_FLAG = "\\$resources";
    
    
    public static String REPORT_DIR = "report";
    
    /** 资源目录名称常量 */
    public static String RESOURCE_DIR = "resources";
    
    /** 资源路径（全局），默认值 "./resources"，运行时根据用例路径自动推导 */
    public static String RESOURCE_PATH = "./resources";
    


    public static String UPDATE_RESULT_TEMPLATE = "Query OK, {num} row affected";

    public static int WAIT_TIMEOUT = 5000;
    
    public static int MAX_ROW_COUNT_IN_RS = 100;//the max row count in the resultset
    
    public static boolean NEED_PPROF = false;

    public static String[] SPECIAL_CHARS = new String[]{"+","-","*","/","%","&",">","<","(",")","!","=","\'","\""};

    public static double SCALE_TOLERABLE_ERROR = 0.0000009;
    public static double INT_TOLERABLE_ERROR = 0.000000000000001;
}