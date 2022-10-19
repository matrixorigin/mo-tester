package io.mo.constant;

public class SQL {
    public static String CREATE_DATABASE = "CREATE DATABSE IF NOT EXISTS `%s`;";
    public static String DROP_DATABASE = "DROP DATABASE IF EXISTS `%s`";
    public static String CREATE_ACCOUNT = "CREATE ACCOUNT IF NOT EXISTS %s admin_name = '%s' IDENTIFIED BY '%s';";
    public static String DROP_ACCOUNT = "DROP ACCOUNT IF EXISTS %s;";
}
