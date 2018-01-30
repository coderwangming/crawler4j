package me.crawler4j.basic;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.url.WebURL;
import org.apache.http.Header;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

/**
 * @author dugenkui
 *         on 2017/12/6.
 */
public class BasicCrawler extends WebCrawler {

//    private static final Pattern IMAGE_EXTENSIONS=Pattern.compile(".*\\.(bmp|gif|jpg|png)$");


    /**
     *visit if start with "http://www.mtime.com/" or isn't picture
     *
     * @param referringPage
     *           The Page in which this url was found.
     * @param url
     *            the url which we are interested to know whether it should be
     *            included in the crawl or not.
     * @return
     */
    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href=url.getURL().toLowerCase();
//        if(IMAGE_EXTENSIONS.matcher(url.getURL()).matches()){
//            return false;
//        }
        return (href.startsWith("http://www.mtime.com/top/movie/top100/")||href.startsWith("http://movie.mtime.com"));
    }

    @Override
    public void visit(Page page) {
        System.out.println(page.getWebURL().getURL());
        System.out.println(page.getContentData());
    }
}
