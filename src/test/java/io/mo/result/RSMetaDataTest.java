package io.mo.result;

import org.junit.Test;
import java.sql.Types;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RSMetaDataTest {
    private RSMetaData metadata(int columns, boolean full) {
        RSMetaData meta = new RSMetaData(columns);
        meta.setFullMetaInfo(full);
        for (int i = 0; i < columns; i++) {
            meta.addMetaInfo("c" + i, "c" + i, Types.INTEGER, 10, 0);
        }
        return meta;
    }

    @Test
    public void extraExpectedColumnIsMismatchNotException() {
        for (boolean full : new boolean[]{false, true}) {
            assertFalse(metadata(8, full).equals(metadata(7, full)));
        }
    }

    @Test
    public void extraActualColumnMustNotBeSilentlyAccepted() {
        for (boolean full : new boolean[]{false, true}) {
            assertFalse(metadata(7, full).equals(metadata(8, full)));
        }
    }

    @Test
    public void emptyVersusNonemptyIsMismatchInBothDirections() {
        for (boolean full : new boolean[]{false, true}) {
            assertFalse(metadata(0, full).equals(metadata(1, full)));
            assertFalse(metadata(1, full).equals(metadata(0, full)));
        }
    }

    @Test
    public void matchingMetadataStillComparesEqual() {
        for (boolean full : new boolean[]{false, true}) {
            assertTrue(metadata(0, full).equals(metadata(0, full)));
            assertTrue(metadata(7, full).equals(metadata(7, full)));
        }
    }

    @Test
    public void labelsAndTypesStillUseExistingComparisonPolicy() {
        RSMetaData changedType = new RSMetaData(1);
        changedType.addMetaInfo("c0", "c0", Types.VARCHAR, 10, 0);
        assertTrue(metadata(1, false).equals(changedType));
        assertFalse(metadata(1, true).equals(changedType));
        RSMetaData changedLabel = new RSMetaData(1);
        changedLabel.addMetaInfo("other", "other", Types.INTEGER, 10, 0);
        assertFalse(metadata(1, false).equals(changedLabel));
    }
}
