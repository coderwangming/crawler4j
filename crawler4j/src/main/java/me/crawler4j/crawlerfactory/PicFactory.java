package me.crawler4j.crawlerfactory;

import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import me.crawler4j.basic.MyCrawler;

import java.util.Map;

/**
 * 抽象工厂创建crawler对象示例
 * @author 杜艮魁
 * @date 2017/12/11
 */
public class PicFactory implements CrawlController.WebCrawlerFactory{

    Map<String,String> metadata;
//    SqlRepository repository;
    Object repository;

    public PicFactory(Map<String, String> metadata, Object repository) {
        this.metadata = metadata;
        this.repository = repository;
    }


    @Override
    public WebCrawler newInstance() throws Exception {
        return new MyCrawler(metadata,repository);//可能工厂一般需要传参数，所以示例搞了个这个吧
    }
}
