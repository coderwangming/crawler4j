package me.crawler4j.basic;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * quickStart for crawler4j
 *
 * @author dugenkui
 *         on 2017/12/6.
 */
public class MyCrawler extends WebCrawler{

//    urlStr end with the flowing postfix
    private final static Pattern FILTERS= Pattern.compile("(http://movie.mtime.com/)(\\d+)/");

    public MyCrawler(Map map, Object repository) {
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href=url.getURL().toLowerCase();
        return FILTERS.matcher(href).matches();//wether the url matches the pattern
    }

    @Override
    public void visit(Page page) {
        String url=page.getWebURL().getURL();

        if(page.getParseData() instanceof HtmlParseData){
            HtmlParseData htmlParseData=(HtmlParseData)page.getParseData();
            Set<WebURL> links=htmlParseData.getOutgoingUrls();

            links.stream().forEach(x-> {
                if(FILTERS.matcher(x.getURL()).matches()){
                    System.out.println(x.getURL());
                }
            });
        }
    }
}
