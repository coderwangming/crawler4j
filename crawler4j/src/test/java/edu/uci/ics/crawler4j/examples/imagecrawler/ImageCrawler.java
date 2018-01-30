/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.uci.ics.crawler4j.examples.imagecrawler;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import com.google.common.io.Files;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.BinaryParseData;
import edu.uci.ics.crawler4j.url.WebURL;

/**
 * @author Yasser Ganjisaffar
 */

/*
 * This class shows how you can crawl images on the web and store them in a
 * folder. This is just for demonstration purposes and doesn't scale for large
 * number of images. For crawling millions of images you would need to store
 * downloaded images in a hierarchy of folders
 */
public class ImageCrawler extends WebCrawler {

    //二进制文件后缀
    private static final Pattern filters = Pattern.compile(
        ".*(\\.(css|js|mid|mp2|mp3|mp4|wav|avi|mov|mpeg|ram|m4v|pdf" +
        "|rm|smil|wmv|swf|wma|zip|rar|gz))$");

    //图片文件后缀
    private static final Pattern imgPatterns = Pattern.compile(".*(\\.(bmp|gif|jpe?g|png|tiff?))$");

    //存储文件夹和爬去的域名
    private static File storageFolder;
    private static String[] crawlDomains;

    /**
     * 设置爬去的域名和爬去数据放进的文件夹
     * @param domain
     * @param storageFolderName
     */
    public static void configure(String[] domain, String storageFolderName) {
        crawlDomains = domain;

        storageFolder = new File(storageFolderName);
        if (!storageFolder.exists()) {
            storageFolder.mkdirs();
        }
    }

    /**
     * 是否应该爬去这个页面
     * @param referringPage
     *           The Page in which this url was found.
     * @param url
     *            the url which we are interested to know whether it should be
     *            included in the crawl or not.
     * @return
     */
    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href = url.getURL().toLowerCase();//将网址小写化

        if (imgPatterns.matcher(href).matches()) {//图片，则TRUE，爬取
            return true;
        }

        for (String domain : crawlDomains) {//是想要爬去的域名，TRUE
            if (href.startsWith(domain)) {
                return true;
            }
        }

        if (filters.matcher(href).matches()) {//二进制，则FALSE
            return false;
        }
        return false;
    }

    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL();

        // We are only interested in processing images which are bigger than 10k
        if (!imgPatterns.matcher(url).matches() ||//如果不是图片后缀
            !((page.getParseData() instanceof BinaryParseData) ||//或者不是二进制文件
              (page.getContentData().length < (10 * 1024)))) {//或者大小小于10k（即1024*10byte）
            return;
        }

        // get a unique name for storing this image
        String extension = url.substring(url.lastIndexOf('.'));
        String hashedName = UUID.randomUUID() + extension;

        // store image
        String filename = storageFolder.getAbsolutePath() + "/" + hashedName;
        try {
            //用所给的字符流重写所给的目的文件
            Files.write(page.getContentData(), new File(filename));
//            logger.info("Stored: {}", url);
        } catch (IOException iox) {
            logger.error("Failed to write file: " + filename, iox);
        }
    }
}