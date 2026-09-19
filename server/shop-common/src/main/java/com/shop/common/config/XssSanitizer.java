package com.shop.common.config;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;

import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class XssSanitizer {

    private static final Safelist XSS_SAFELIST;
    private static final Set<String> STYLE_TAGS = Set.of("p", "h1", "h2", "h3", "h4", "h5", "h6",
            "blockquote", "span", "div", "li", "img");
    private static final Pattern CSS_LENGTH = Pattern.compile("(?i)^(?:\\d+(?:\\.\\d+)?)(?:px|pt|em|rem|%)$");
    private static final Pattern CSS_LINE_HEIGHT = Pattern.compile("(?i)^(?:\\d+(?:\\.\\d+)?)(?:px|pt|em|rem|%)?$");
    private static final Pattern CSS_COLOR = Pattern.compile("(?i)^(?:#[0-9a-f]{3,8}|rgba?\\(\\s*[\\d.]+%?(?:\\s*,\\s*[\\d.]+%?){2}(?:\\s*,\\s*(?:0|1|0?\\.[\\d]+))?\\s*\\)|[a-z]{3,20})$");

    static {
        XSS_SAFELIST = Safelist.basic()
                .preserveRelativeLinks(true)
                .addTags("img", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "span", "div", "s", "del")
                .addAttributes("img", "src", "alt", "width", "height", "style")
                .addAttributes("a", "title")
                .addProtocols("img", "src", "http", "https");
        for (String tag : STYLE_TAGS) {
            XSS_SAFELIST.addAttributes(tag, "style");
        }
    }

    /** 对 HTML 内容做 XSS 过滤；返回安全的 HTML；输入 null 返 null */
    public static String sanitize(String html) {
        if (html == null) return null;
        Document document = Jsoup.parseBodyFragment(Jsoup.clean(html, XSS_SAFELIST));
        for (Element element : document.select("[style]")) {
            String style = sanitizeStyle(element.attr("style"));
            if (style.isEmpty()) element.removeAttr("style");
            else element.attr("style", style);
        }
        for (Element link : document.select("a[href]")) {
            if (!isSafeLink(link.attr("href"))) link.removeAttr("href");
        }
        for (Element image : document.select("img[src]")) {
            if (!isSafeImageSource(image.attr("src"))) image.removeAttr("src");
        }
        return document.body().html();
    }

    private static String sanitizeStyle(String style) {
        StringBuilder safeStyle = new StringBuilder();
        for (String declaration : style.split(";")) {
            int colon = declaration.indexOf(':');
            if (colon <= 0) continue;
            String property = declaration.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = declaration.substring(colon + 1).trim();
            if (!isSafeStyle(property, value)) continue;
            if (!safeStyle.isEmpty()) safeStyle.append(' ');
            safeStyle.append(property).append(": ").append(value).append(';');
        }
        return safeStyle.toString();
    }

    private static boolean isSafeStyle(String property, String value) {
        return switch (property) {
            case "color", "background-color" -> CSS_COLOR.matcher(value).matches();
            case "font-size", "width", "max-width" -> CSS_LENGTH.matcher(value).matches();
            case "line-height" -> CSS_LINE_HEIGHT.matcher(value).matches();
            case "font-weight" -> value.matches("(?i)(normal|bold|[1-9]00)");
            case "font-style" -> value.matches("(?i)(normal|italic|oblique)");
            case "text-decoration" -> value.matches("(?i)(none|underline|line-through|overline)");
            case "text-align" -> value.matches("(?i)(left|right|center|justify|start|end)");
            default -> false;
        };
    }

    private static boolean isSafeLink(String href) {
        String value = href.trim();
        if (value.matches("^/pages/[A-Za-z0-9_./?=&%+-]+$") && !value.contains("..")) return true;
        try {
            URI uri = URI.create(value);
            return uri.getHost() != null
                    && "https".equalsIgnoreCase(uri.getScheme());
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static boolean isSafeImageSource(String source) {
        String value = source.trim();
        if (!value.startsWith("//") && !value.contains("..") && !value.contains(":")) return true;
        try {
            URI uri = URI.create(value);
            return uri.getHost() != null
                    && ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
