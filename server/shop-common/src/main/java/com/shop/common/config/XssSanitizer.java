package com.shop.common.config;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public class XssSanitizer {

    private static final Safelist XSS_SAFELIST;

    static {
        XSS_SAFELIST = Safelist.basic()
                .addTags("img", "h1", "h2", "h3", "h4", "blockquote", "span", "div")
                .addAttributes("img", "src", "alt", "width", "height")
                .addAttributes("span", "style")
                .addAttributes("div", "style")
                .addProtocols("img", "src", "http", "https");
    }

    /** 对 HTML 内容做 XSS 过滤；返回安全的 HTML；输入 null 返 null */
    public static String sanitize(String html) {
        if (html == null) return null;
        return Jsoup.clean(html, XSS_SAFELIST);
    }
}
