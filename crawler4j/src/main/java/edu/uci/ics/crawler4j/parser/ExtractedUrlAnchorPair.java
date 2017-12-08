package edu.uci.ics.crawler4j.parser;

import java.util.HashMap;
import java.util.Map;

/**
 * 类名直译：提取url的锚节点对
 */
public class ExtractedUrlAnchorPair {

    private String href;//引用
    private String anchor;//锚节点
    private String tag;//标志
    private Map<String, String> attributes = new HashMap<String, String>();//属性对

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getAnchor() {
        return anchor;
    }

    public void setAnchor(String anchor) {
        this.anchor = anchor;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getAttribute(String name) {
        return attributes.get(name);
    }

    public void setAttribute(String name, String val) {
        attributes.put(name, val);
    }
}