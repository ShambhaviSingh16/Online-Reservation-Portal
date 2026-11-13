package com.trainticket.util;
public final class Html {
    private Html() { }
    public static String escape(Object value) {
        return value==null?"":org.springframework.web.util.HtmlUtils.htmlEscape(String.valueOf(value));
    }
}
