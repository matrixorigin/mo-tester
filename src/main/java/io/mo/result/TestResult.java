package io.mo.result;

import io.mo.constant.RESULT;

public class TestResult {
    private String result;
    private String actResult;
    private String expResult;

    public TestResult(){
        result = RESULT.RESULT_TYPE_PASS;
    }

    public String getActResult() {
        return actResult;
    }

    public void setActResult(String actResult) {
        this.actResult = actResult;
    }

    public String getExpResult() {
        return expResult;
    }

    public void setExpResult(String expResult) {
        this.expResult = expResult;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }


}
