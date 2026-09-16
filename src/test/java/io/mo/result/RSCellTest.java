package io.mo.result;

import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class RSCellTest {

    @Test
    public void ipv4TextIsNotNumeric() {
        assertFalse(RSCell.isNumeric("1.2.3.4"));
    }

    @Test
    public void differentIpv4VarcharValuesReturnFalseWithoutThrowing() {
        RSCell expected = varcharCell("1.2.3.4");
        RSCell actual = varcharCell("1.2.3.5");

        try {
            assertFalse(expected.equals(actual));
        } catch (NumberFormatException exception) {
            fail("IPv4 VARCHAR values must not be parsed as numbers: " + exception.getMessage());
        }
    }

    private RSCell varcharCell(String value) {
        RSCell cell = new RSCell();
        cell.setType(Types.VARCHAR);
        cell.setValue(value);
        return cell;
    }
}
