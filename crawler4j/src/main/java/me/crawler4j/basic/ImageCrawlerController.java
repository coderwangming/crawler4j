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
public class ImageCrawlerController {
    private static final Logger logger= LoggerFactory.getLogger(ImageCrawlerController.class);

    public static void main(String[] args) throws Exception {
        String rootFolder="/data/crawler/image/rootFolder/";
        int numberOfCrawlers=1;
        String storageFolder="/data/crawler/image/storageFolder/";

        CrawlConfig config=new CrawlConfig();
        config.setCrawlStorageFolder(rootFolder);
        config.setIncludeBinaryContentInCrawling(true);

        String [] crawlDomains={"http://www.mtime.com"};

        PageFetcher pageFetcher=new PageFetcher(config);
        RobotstxtConfig robotstxtConfig=new RobotstxtConfig();
        RobotstxtServer robotstxtServer=new RobotstxtServer(robotstxtConfig,pageFetcher);
        CrawlController controller=new CrawlController(config,pageFetcher,robotstxtServer);
        for(String domain:crawlDomains){
            controller.addSeed(domain);
        }

        ImageCrawler.configure(crawlDomains,storageFolder);
        controller.start(ImageCrawler.class,numberOfCrawlers);
    }
}
