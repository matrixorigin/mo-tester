package io.mo.constant;

public class ESCAPE {

    //Escape character
    public static String SPACE = "&nbsp;";
    public static String DOUBLE_QOUTE = "&quot;";
    public static String SINGLE_QOUTE = "&acute;";
    public static String CROSSED = "&macr;";

    public static String parse(String str){
        return str
                .replaceAll(SPACE," ")
                .replaceAll(DOUBLE_QOUTE,"\"")
                .replaceAll(SINGLE_QOUTE,"'")
                .replaceAll(CROSSED,"-");
    }

}
