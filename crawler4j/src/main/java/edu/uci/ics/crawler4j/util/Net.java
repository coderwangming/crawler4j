package edu.uci.ics.crawler4j.util;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.uci.ics.crawler4j.url.WebURL;

/**
 * Created by Avi Hayun on 9/22/2014.
 * Net related Utils
 */
public class Net {
    private static final Pattern pattern = initializePattern();


    /**
     * Singleton like one time call to initialize the Pattern
     *
     * 返回一个匹配网址的Pattern对象
     * @return
     */
    private static Pattern initializePattern() {
        return Pattern.compile("\\b(((ht|f)tp(s?)\\:\\/\\/|~\\/|\\/)|www.)" +
                               "(\\w+:\\w+@)?(([-\\w]+\\.)+(com|org|net|gov" +
                               "|mil|biz|info|mobi|name|aero|jobs|museum" +
                               "|travel|[a-z]{2}))(:[\\d]{1,5})?" +
                               "(((\\/([-\\w~!$+|.,=]|%[a-f\\d]{2})+)+|\\/)+|\\?|#)?" +
                               "((\\?([-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?" +
                               "([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)" +
                               "(&(?:[-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?" +
                               "([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)*)*" +
                               "(#([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)?\\b");
    }

    /**
     * 从一个字符集中抽取出网址列表，并返回
     * @param input
     * @return
     */
    public static Set<WebURL> extractUrls(String input) {
        Set<WebURL> extractedUrls = new HashSet<>();

        if (input != null) {
            Matcher matcher = pattern.matcher(input);
            //XX.find()和XX.group()使用示例
            while (matcher.find()) {//如果还有匹配的字符串
                WebURL webURL = new WebURL();
                String urlStr = matcher.group();//找到这个匹配的字符串
                if (!urlStr.startsWith("http")) {//如果不是以http开头，这在最前边加上http://
                    urlStr = "http://" + urlStr;
                }

                webURL.setURL(urlStr);
                extractedUrls.add(webURL);//放进结果集
            }
        }

        return extractedUrls;
    }
}