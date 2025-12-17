package io.mo.constant;

import java.sql.Types;

public class DATATYPE {
     public static final java.util.Map<Integer, String> TYPE_NAME_MAP;
     static {
         java.util.Map<Integer, String> map = new java.util.HashMap<>();
         map.put(Types.BIT, "BIT");
         map.put(Types.TINYINT, "TINYINT");
         map.put(Types.SMALLINT, "SMALLINT");
         map.put(Types.INTEGER, "INTEGER");
         map.put(Types.BIGINT, "BIGINT");
         map.put(Types.FLOAT, "FLOAT");
         map.put(Types.REAL, "REAL");
         map.put(Types.DOUBLE, "DOUBLE");
         map.put(Types.NUMERIC, "NUMERIC");
         map.put(Types.DECIMAL, "DECIMAL");
         map.put(Types.CHAR, "CHAR");
         map.put(Types.VARCHAR, "VARCHAR");
         map.put(Types.LONGVARCHAR, "LONGVARCHAR");
         map.put(Types.DATE, "DATE");
         map.put(Types.TIME, "TIME");
         map.put(Types.TIMESTAMP, "TIMESTAMP");
         map.put(Types.BINARY, "BINARY");
         map.put(Types.VARBINARY, "VARBINARY");
         map.put(Types.LONGVARBINARY, "LONGVARBINARY");
         map.put(Types.NULL, "NULL");
         map.put(Types.OTHER, "OTHER");
         map.put(Types.JAVA_OBJECT, "JAVA_OBJECT");
         map.put(Types.DISTINCT, "DISTINCT");
         map.put(Types.STRUCT, "STRUCT");
         map.put(Types.ARRAY, "ARRAY");
         map.put(Types.BLOB, "BLOB");
         map.put(Types.CLOB, "CLOB");
         map.put(Types.REF, "REF");
         map.put(Types.DATALINK, "DATALINK");
         map.put(Types.BOOLEAN, "BOOLEAN");
         map.put(Types.ROWID, "ROWID");
         map.put(Types.NCHAR, "NCHAR");
         map.put(Types.NVARCHAR, "NVARCHAR");
         map.put(Types.LONGNVARCHAR, "LONGNVARCHAR");
         map.put(Types.NCLOB, "NCLOB");
         map.put(Types.SQLXML, "SQLXML");
         TYPE_NAME_MAP = java.util.Collections.unmodifiableMap(map);
     }
}


