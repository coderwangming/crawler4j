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
     * ����һ��ƥ����ַ��Pattern����
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
     * ��һ���ַ����г�ȡ����ַ�б�������
     * @param input
     * @return
     */
    public static Set<WebURL> extractUrls(String input) {
        Set<WebURL> extractedUrls = new HashSet<>();

        if (input != null) {
            Matcher matcher = pattern.matcher(input);
            //XX.find()��XX.group()ʹ��ʾ��
            while (matcher.find()) {//�������ƥ����ַ���
                WebURL webURL = new WebURL();
                String urlStr = matcher.group();//�ҵ����ƥ����ַ���
                if (!urlStr.startsWith("http")) {//���������http��ͷ��������ǰ�߼���http://
                    urlStr = "http://" + urlStr;
                }

                webURL.setURL(urlStr);
                extractedUrls.add(webURL);//�Ž������
            }
        }

        return extractedUrls;
    }
}