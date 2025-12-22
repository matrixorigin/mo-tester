package io.mo.result;

import io.mo.constant.RESULT;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestResult {
    private String result = RESULT.RESULT_TYPE_PASS;
    private String actResult;
    private String expResult;
}
