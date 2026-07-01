package com.shop.common.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class XssSanitizerTest {

    @Test
    void removesScriptTags() {
        String input = "<p>Hello</p><script>alert('xss')</script>";
        String output = XssSanitizer.sanitize(input);
        assertFalse(output.contains("<script>"));
        assertTrue(output.contains("<p>Hello</p>") || output.contains("Hello"));
    }

    @Test
    void preservesSafeTags() {
        String input = "<p>text</p><strong>bold</strong><em>italic</em><img src=\"x.jpg\" alt=\"pic\">";
        String output = XssSanitizer.sanitize(input);
        assertTrue(output.contains("bold"));
        assertTrue(output.contains("italic"));
    }

    @Test
    void nullReturnsNull() {
        assertNull(XssSanitizer.sanitize(null));
    }

    @Test
    void emptyReturnsEmpty() {
        String output = XssSanitizer.sanitize("");
        assertNotNull(output);
    }
}
