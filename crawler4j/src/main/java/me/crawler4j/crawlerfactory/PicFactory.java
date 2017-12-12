package me.crawler4j.crawlerfactory;

import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import me.crawler4j.basic.MyCrawler;

import java.util.Map;

/**
 * ���󹤳�����crawler����ʾ��
 * @author ���޿�
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
        return new MyCrawler(metadata,repository);//���ܹ���һ����Ҫ������������ʾ�����˸������
    }
}
