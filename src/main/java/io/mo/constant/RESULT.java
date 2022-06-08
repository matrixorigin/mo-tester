package io.mo.constant;

public class RESULT {
    public static String SUCCESS_CODE = "0000";
    public static String ERROR_EXEC_FAILED_CODE = "0001";
    public static String ERROR_CHECK_FAILED_CODE = "0002";
    public static String ERROR_PRECONDITION_FAILED_CODE = "0003";
    public static String ERROR_POSTCONDITION_FAILED_CODE = "0004";
    public static String ERROR_CKFILE_NOT_VALID_CODE = "0005";
    public static String ERROR_CKFILE_CONTENT_WRONG_CODE = "0006";
    public static String ERROR_NOT_EXEC_CODE = "0007";
    public static String ERROR_CONNECTION_LOST_CODE = "0008";
    public static String ERROR_CASE_IGNORE_CODE = "0100";

    public static String ERROR_EXEC_FAILED_DESC = "Some exceptions occured when executing this test case.More,See the detail.";
    public static String ERROR_CHECK_FAILED_DESC = "The actual result is not in accordance with the expectation.More,See the detail.";
    public static String ERROR_PRECONDITIONFAILED_DESC = "Some exceptions occured when executing the pre-condition .More,See the detail.";
    public static String ERROR_POSTCONDITIONFAILED_DESC = "Some exceptions occured when executing the post-condition .More,See the detail.";
    public static String ERROR_CKFILE_NOT_VALID_DESC = "The expected result file does not exist or is not valid.Please check this.";
    public static String ERROR_CKFILE_CONTENT_WRONG_DESC = "The content of the expected result file can not be parsed.Please check this.";
    public static String ERROR_NOT_EXEC_DESC = "The script does not been executed because the result file does not exists.";
    public static String ERROR_CASE_IGNORE_DESC = "The command is marked to ignore flag,do not be executed.";
    public static String ERROR_CONNECTION_LOST_DESC = "The connection has been lost becuase of unknown reason.";
    public static String ERROR_UNKNOWN_DESC = "The result is not correct because of unexpected reason,Message: ";


    public static String RESULT_TYPE_PASS = "PASS";
    public static String RESULT_TYPE_FAILED="FAILED";
    public static String RESULT_TYPE_NOEXEC="NOEXECUTED";
    public static String RESULT_TYPE_UNKNOWN="UNKNOWN";

}

