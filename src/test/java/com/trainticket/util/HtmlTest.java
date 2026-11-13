package com.trainticket.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HtmlTest {
    @Test void dynamicTextCannotBecomeMarkup() {
        assertEquals("&lt;script&gt;&quot;&#39;&amp;",Html.escape("<script>\"'&"));
        assertEquals("",Html.escape(null));
        assertEquals("42",Html.escape(42));
    }
}
