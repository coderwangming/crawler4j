package me.crawler4j.basic;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

/**
 * controller to start crawl
 *
 * @author dugenkui
 *         on 2017/12/6.
 */
public class MyCrawlerController {
    public static void main(String[] args) throws Exception {
        String crawStorageFolder="/data/crawl/root";
        int numberOfCrawlers=7;

        CrawlConfig config=new CrawlConfig();
        config.setCrawlStorageFolder(crawStorageFolder);

        /**
         * instantiate the controller for this crawl
         */
        PageFetcher pageFetcher=new PageFetcher(config);
        RobotstxtConfig robotstxtConfig=new RobotstxtConfig();
        RobotstxtServer robotstxtServer=new RobotstxtServer(robotstxtConfig,pageFetcher);
        CrawlController controller=new CrawlController(config,pageFetcher,robotstxtServer);

        /**
         * seed url to start crawl
         */
        controller.addSeed("http://www.mtime.com/top/movie/top100/");

        controller.start(MyCrawler.class,numberOfCrawlers);
    }
}
