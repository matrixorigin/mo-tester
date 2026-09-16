package io.mo.result;

import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RSCellTest {

    @Test
    public void numericTextMustNotTreatIpv4AsNumber() {
        assertFalse(RSCell.isNumeric("10.0.5.9"));
        assertTrue(RSCell.isNumeric("-12.50"));
        assertTrue(RSCell.isNumeric(".5"));
        assertTrue(RSCell.isNumeric("1e+3"));

        RSCell expected = cell("10.0.5.9");
        RSCell actual = cell("10.0.5.10");
        assertFalse(expected.equals(actual));
    }

    private static RSCell cell(String value) {
        RSCell cell = new RSCell();
        cell.setValue(value);
        cell.setType(Types.VARCHAR);
        return cell;
    }
}
