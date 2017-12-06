package me.crawler4j.basic;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.BinaryParseData;
import edu.uci.ics.crawler4j.url.WebURL;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * @author dugenkui
 *         on 2017/12/6.
 */
public class ImageCrawler extends WebCrawler{

    private static Pattern filters= Pattern.compile(
            ".*(\\.(css|js|mid|mp2|mp3|mp4|wav|avi|mov|mpeg|ram|m4v|pdf" +
                    "|rm|smil|wmv|swf|wma|zip|rar|gz))$"
    );

    private static final Pattern imgPatterns=Pattern.compile(".*(\\.(bmp|gif|jpe?g|png|tiff?))$");

    private static File storageFolder;
    private static String[] crawlDomains;

    /**
     * 配置有效url连接和存放数据的文件夹
     * @param domain
     * @param storageFolderName
     */
    public static void configure(String[] domain,String storageFolderName){
        crawlDomains=domain;

        storageFolder=new File(storageFolderName);
        if(!storageFolder.exists()){
            storageFolder.mkdir();//if directory don't exists,create it;
        }
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href=url.getURL().toLowerCase();
        if(filters.matcher(href).matches()){
            return false;
        }

        if(imgPatterns.matcher(href).matches()){
            return true;
        }

        for(String domain:crawlDomains){
            if(href.startsWith(domain)){
                return true;
            }
        }
        return false;
    }

    @Override
    public void visit(Page page) {
        String url=page.getWebURL().getURL();

        //only interested in images witch are bigger than 1kb
        if(!imgPatterns.matcher(url).matches()||!((page.getParseData() instanceof BinaryParseData))
                ||(page.getContentData().length<(1024))){
            return ;
        }

        String extension=url.substring(url.lastIndexOf('.'));
        String hashedName= UUID.randomUUID()+extension;//也可以用md5命名

        String filename=storageFolder.getAbsolutePath()+"/"+hashedName;//todo "/" for linux
        try{
            Files.write(page.getContentData(),new File(filename));
            logger.info("Stored: {}", url);
        }catch (IOException e){
            logger.error("Failed to write file: "+filename,e);
        }

    }
}
