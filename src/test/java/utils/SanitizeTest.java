package utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SanitizeTest {
    
    @Test
    public void testHtmlEscaping() {
        String input = "<script>alert('XSS');</script>";
        String expected = "&lt;script&gt;alert(&#x27;XSS&#x27;);&lt;/script&gt;";
        assertEquals(expected, Sanitize.html(input));
    }

    @Test
    public void testNullInput() {
        assertEquals("", Sanitize.html(null));
    }

    @Test
    public void testPunctuationEscaping() {
        String input = "\"Admin\" & 'User'";
        String expected = "&quot;Admin&quot; &amp; &#x27;User&#x27;";
        assertEquals(expected, Sanitize.html(input));
    }
}
