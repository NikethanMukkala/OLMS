package utils;

public class Sanitize {
    /**
     * Replaces HTML characters with their equivalent entities to mitigate XSS.
     */
    public static String html(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }
}
