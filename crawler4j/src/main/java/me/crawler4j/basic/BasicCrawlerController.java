package me.crawler4j.basic;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dugenkui
 *         on 2017/12/6.
 */
public class BasicCrawlerController {
    private static final Logger logger= LoggerFactory.getLogger(BasicCrawlerController.class);


    public static void main(String[] args) throws Exception {
        String crawlStorageFolder="/data/crawl/root";
        int numberOfCrawlers=Integer.parseInt("7");

        CrawlConfig config=new CrawlConfig();
        config.setCrawlStorageFolder(crawlStorageFolder);
        //fixme: how many requests are send per second.
        config.setPolitenessDelay(100);
        config.setMaxDepthOfCrawling(10);
//        config.setMaxPagesToFetch(1000);//default -1 for unlimited number
        config.setIncludeBinaryContentInCrawling(false);
        config.setResumableCrawling(true);

        //initiate the controller of basicCrawl
        PageFetcher pageFetcher=new PageFetcher(config);

        RobotstxtConfig robotstxtConfig=new RobotstxtConfig();
        RobotstxtServer robotstxtServer=new RobotstxtServer(robotstxtConfig,pageFetcher);

        CrawlController controller=new CrawlController(config,pageFetcher,robotstxtServer);

        controller.addSeed("http://www.mtime.com/top/movie/top100/");

        controller.start(BasicCrawler.class,numberOfCrawlers);
    }
}
