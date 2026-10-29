package io.mo.util;

import io.mo.result.RSSet;
import java.lang.reflect.Method;
import java.sql.Types;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ResultParserEmptyMetadataTest {
    @Test
    public void parsesMetadataForEmptyResultBeforeNextCommand() throws Exception {
        Method extract = ResultParser.class.getDeclaredMethod("extractResult", String.class, String.class, String.class);
        Method convert = ResultParser.class.getDeclaredMethod("convertToRSSet", String.class, String.class);
        extract.setAccessible(true);
        convert.setAccessible(true);
        String command = "execute clipped_range using @lo,@hi;";
        String next = "set @lo = 1.5;";
        String input = command + "\n" + "➤ a[4,32,0]  𝄀\n" + next;
        String extracted = (String) extract.invoke(null, command, next, input);
        RSSet result = (RSSet) convert.invoke(null, extracted, "space");
        assertEquals(Types.INTEGER, result.getMeta().getType(0));
        assertEquals("a[4,32,0]", result.getMeta().stringAt(0));
        assertEquals(0, result.getRows().size());
    }

    @Test
    public void preservesModernResultRowsAndLegacyResults() throws Exception {
        Method convert = ResultParser.class.getDeclaredMethod("convertToRSSet", String.class, String.class);
        convert.setAccessible(true);
        RSSet modern = (RSSet) convert.invoke(null, "➤ a[4,32,0]  𝄀\n1", "space");
        assertEquals(Types.INTEGER, modern.getMeta().getType(0));
        assertEquals(1, modern.getRows().size());
        RSSet legacy = (RSSet) convert.invoke(null, "a\n1", "space");
        assertEquals(1, legacy.getRows().size());
    }
}
