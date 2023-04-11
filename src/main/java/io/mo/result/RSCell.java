package io.mo.result;

import io.mo.constant.COMMON;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.regex.Pattern;

public class RSCell<T> {
    private T value; //cell value
    private int type; // column data type,remain attr
    private int precision = 0; //column value precision,remain attr
    private static Logger LOG = Logger.getLogger(RSCell.class.getName());

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public String toString(){
        return String.valueOf(value);
    }

    /**
     * compare whether this cell equals the other cell
     */
    public boolean equals(RSCell cell){
        if(compareTo(cell) == 0)
            return true;
        else{
            //precision toleration code
            String v1 = (String)this.value;
            String v2 = (String)cell.getValue();
            
            //if one is NULL,return false
            if(v1.equalsIgnoreCase("null") || v2.equalsIgnoreCase("null"))
                return false;
            

            if(cell.type == Types.FLOAT ||
               cell.type == Types.REAL  ||
               cell.type == Types.DOUBLE||
               cell.type == Types.DECIMAL||
               cell.type == Types.BIGINT ||
               (cell.type == Types.VARCHAR && isNumeric(v1) &&isNumeric(v2))) {
                

                BigDecimal bd1 = BigDecimal.valueOf(Double.valueOf(v1)).stripTrailingZeros();
                BigDecimal bd2 = BigDecimal.valueOf(Double.valueOf(v2)).stripTrailingZeros();
                //System.out.println("bd1 = " + bd1);
                //System.out.println("bd2 = " + bd2);
                
                
                if(bd1.compareTo(bd2) == 0)
                    return true;
                else{
                    //if(bd1.equals(BigDecimal.ZERO) || bd2.equals(BigDecimal.ZERO))
                    if(bd1.compareTo(BigDecimal.ZERO) == 0 || bd2.compareTo(BigDecimal.ZERO) == 0)
                        return false;
                }

                //round to one with samll scale, and compare
                int scal1 = bd1.scale();
                int scal2 = bd2.scale();

                //System.out.println("bd1.p = " + bd1.scale());
                //System.out.println("bd2.p = " + bd2.scale());
                if(scal1 > scal2)
                    bd1 = bd1.setScale(scal2,BigDecimal.ROUND_HALF_UP);
                if(scal1 < scal2)
                    bd2 = bd2.setScale(scal1,BigDecimal.ROUND_HALF_UP);
                //System.out.println("bd1 = " + bd1);
                //System.out.println("bd2 = " + bd2);
                if(bd1.compareTo(bd2) == 0) {
                    LOG.debug("After rounding, value[" + v1 +"] equals to value[" + v2 +"]");
                    return true;
                }else{
                    //if(bd1.equals(BigDecimal.ZERO) || bd2.equals(BigDecimal.ZERO))
                    if(bd1.compareTo(BigDecimal.ZERO) == 0 || bd2.compareTo(BigDecimal.ZERO) == 0)
                        return false;
                }

                //if error beteen bd1 and bd2 is less than SCALE_TOLERABLE_ERROR(0.0000009),return true;
                BigDecimal error = bd1.subtract(bd2).abs();
                BigDecimal toleration = BigDecimal.valueOf(COMMON.SCALE_TOLERABLE_ERROR);
                if(error.compareTo(toleration) <= 0) {
                    LOG.debug("value[" + v1 +"] and value[" + v2 +"] match the scale tolerable error");
                    return true;
                }
                
                error =  error.divide(bd1,BigDecimal.ROUND_HALF_UP);
                toleration = BigDecimal.valueOf(COMMON.INT_TOLERABLE_ERROR);
                //if error beteen bd1 and bd2 divided bd1 or db2 is less than INT_TOLERABLE_ERROR(0.0.000000000000001),return true;
                if(error.compareTo(toleration) <= 0){
                    LOG.debug("value[" + v1 +"] and value[" + v2 +"] match the scale tolerable error");
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * compare the order between this and th other cell
     */
    public int compareTo(RSCell cell){
        String v1 = (String)this.value;
        String v2 = (String)cell.getValue();
        if(v1.equalsIgnoreCase("null")){
            v1 = "null";
        }
        if(v2.equalsIgnoreCase("null")){
            v2 = "null";
        }
        return v1.compareTo(v2);
    }


    /**
     * check whether str is a number
     * @param str
     * @return
     */
    public static boolean isNumeric(String str){
        if(str == null || str == ""){
            return false;
        }
        if (null == str || "".equals(str)) {
            return false;
        }
        String regx = "[+-]*\\d+\\.?\\d*[Ee]*[+-]*\\d+";
        Pattern pattern = Pattern.compile(regx);
        boolean isNumber = pattern.matcher(str).matches();
        if (isNumber) {
            return isNumber;
        }
        regx = "^[-\\+]?[.\\d]*$";
        pattern = Pattern.compile(regx);
        return pattern.matcher(str).matches();
        //return str.matches("^[+\\-]?\\d*[.]?\\d+$");
    }
    
    public static void main(String[] args){
        BigDecimal a = new BigDecimal("0.00");
        System.out.println(a.compareTo(BigDecimal.ZERO));
    }
}
