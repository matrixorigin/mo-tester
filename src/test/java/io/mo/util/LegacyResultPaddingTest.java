package io.mo.util;

import io.mo.result.RSSet;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

public class LegacyResultPaddingTest {
    private RSSet parse(String text) throws Exception {
        Method method = ResultParser.class.getDeclaredMethod("convertToRSSet", String.class, String.class);
        method.setAccessible(true);
        return (RSSet) method.invoke(null, text, "space");
    }

    @Test
    public void stripsProvenHeaderPaddingWithoutChangingValues() throws Exception {
        RSSet result = parse("a    b    \n1    2");
        assertEquals(2, result.getMeta().getColumnCount());
        assertEquals("2", result.getRows().get(0).getRowCellString(1));
    }

    @Test
    public void preservesRealEmptyLastColumn() throws Exception {
        RSSet result = parse("a    b    \n1    2    ");
        assertEquals(3, result.getMeta().getColumnCount());
        assertEquals("", result.getRows().get(0).getRowCellString(2));
    }

    @Test
    public void doesNotGuessForMixedRowsOrNoRows() throws Exception {
        assertEquals(3, parse("a    b    \n1    2\n3    4    ").getMeta().getColumnCount());
        assertEquals(3, parse("a    b    ").getMeta().getColumnCount());
    }

    @Test
    public void preservesNewFormat() throws Exception {
        RSSet result = parse("➤ a[12,4,0]  ¦  [12,4,0]  𝄀\n1  ¦  ");
        assertEquals(2, result.getMeta().getColumnCount());
    }
}
