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

package edu.uci.ics.crawler4j.parser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

import edu.uci.ics.crawler4j.crawler.exceptions.NotAllowedContentException;
import org.apache.tika.language.LanguageIdentifier;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlMapper;
import org.apache.tika.parser.html.HtmlParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uci.ics.crawler4j.crawler.Configurable;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.exceptions.ParseException;
import edu.uci.ics.crawler4j.url.URLCanonicalizer;
import edu.uci.ics.crawler4j.url.WebURL;
import edu.uci.ics.crawler4j.util.Net;
import edu.uci.ics.crawler4j.util.Util;

/**
 * @author Yasser Ganjisaffar
 */
public class Parser extends Configurable {

    protected static final Logger logger = LoggerFactory.getLogger(Parser.class);

    //todo ？？"html解析器"和"解析内容"？？
    private final HtmlParser htmlParser;
    private final ParseContext parseContext;

    //super关键字初始化父类变量，然后初始化此类变量
    public Parser(CrawlConfig config) throws InstantiationException, IllegalAccessException {
        super(config);//this.config = config;.父类有且只有类变量CrawlConfig
        htmlParser = new HtmlParser();
        parseContext = new ParseContext();
        parseContext.set(HtmlMapper.class, AllTagMapper.class.newInstance());
    }

    /**
     * 判定page类型是二进制内容、纯文本还是html，并根据不同的类型进行不同的解析，并将结果放进Page中
     * todo 第三种情况，即html那种情况未做深入了解
     * @param page
     * @param contextURL
     * @throws NotAllowedContentException
     * @throws ParseException
     */
    public void parse(Page page, String contextURL)
        throws NotAllowedContentException, ParseException {
        // 如果Page包含二进制内容，例如图片、视频、音频和应用
        if (Util.hasBinaryContent(page.getContentType())) {
            BinaryParseData parseData = new BinaryParseData();
            if (config.isIncludeBinaryContentInCrawling()) { //config是父类变量：如果配置中不允许爬去二进制内容，则此网页内容是不允许爬取的
                if (config.isProcessBinaryContentInCrawling()) {
                    parseData.setBinaryContent(page.getContentData());//二进制内容放进BinaryParseData
                } else {
                    parseData.setHtml("<html></html>");
                }
                page.setParseData(parseData);//解析内容放进Page
                if (parseData.getHtml() == null) {
                    throw new ParseException();
                }
                parseData.setOutgoingUrls(Net.extractUrls(parseData.getHtml()));
            } else {
                throw new NotAllowedContentException();
            }
        }
        // 如果Page包含纯文本：返回contentType包含“text”而且不包含“html”
        else if (Util.hasPlainTextContent(page.getContentType())) {
            try {
                TextParseData parseData = new TextParseData();
                if (page.getContentCharset() == null) {
                    parseData.setTextContent(new String(page.getContentData()));
                } else {
                    parseData.setTextContent(
                        new String(page.getContentData(), page.getContentCharset()));
                }
                parseData.setOutgoingUrls(Net.extractUrls(parseData.getTextContent()));
                page.setParseData(parseData);
            } catch (Exception e) {
                logger.error("{}, while parsing: {}", e.getMessage(), page.getWebURL().getURL());
                throw new ParseException();
            }
        }
        //第三种情况，即html格式
        else { // isHTML
            Metadata metadata = new Metadata();
            HtmlContentHandler contentHandler = new HtmlContentHandler();
            try (InputStream inputStream = new ByteArrayInputStream(page.getContentData())) {
                htmlParser.parse(inputStream, contentHandler, metadata, parseContext);
            } catch (Exception e) {
                logger.error("{}, while parsing: {}", e.getMessage(), page.getWebURL().getURL());
                throw new ParseException();
            }

            if (page.getContentCharset() == null) {
                page.setContentCharset(metadata.get("Content-Encoding"));
            }

            HtmlParseData parseData = new HtmlParseData();
            parseData.setText(contentHandler.getBodyText().trim());
            parseData.setTitle(metadata.get(DublinCore.TITLE));
            parseData.setMetaTags(contentHandler.getMetaTags());
            // Please note that identifying language takes less than 10 milliseconds
            LanguageIdentifier languageIdentifier = new LanguageIdentifier(parseData.getText());
            page.setLanguage(languageIdentifier.getLanguage());

            Set<WebURL> outgoingUrls = new HashSet<>();

            String baseURL = contentHandler.getBaseUrl();
            if (baseURL != null) {
                contextURL = baseURL;
            }

            int urlCount = 0;
            for (ExtractedUrlAnchorPair urlAnchorPair : contentHandler.getOutgoingUrls()) {

                String href = urlAnchorPair.getHref();
                if ((href == null) || href.trim().isEmpty()) {
                    continue;
                }

                String hrefLoweredCase = href.trim().toLowerCase();
                if (!hrefLoweredCase.contains("javascript:") &&
                    !hrefLoweredCase.contains("mailto:") && !hrefLoweredCase.contains("@")) {
                    String url = URLCanonicalizer.getCanonicalURL(href, contextURL);
                    if (url != null) {
                        WebURL webURL = new WebURL();
                        webURL.setURL(url);
                        webURL.setTag(urlAnchorPair.getTag());
                        webURL.setAnchor(urlAnchorPair.getAnchor());
                        webURL.setAttributes(urlAnchorPair.getAttributes());
                        outgoingUrls.add(webURL);
                        urlCount++;
                        if (urlCount > config.getMaxOutgoingLinksToFollow()) {
                            break;
                        }
                    }
                }
            }
            parseData.setOutgoingUrls(outgoingUrls);

            try {
                if (page.getContentCharset() == null) {
                    parseData.setHtml(new String(page.getContentData()));
                } else {
                    parseData.setHtml(new String(page.getContentData(), page.getContentCharset()));
                }

                page.setParseData(parseData);
            } catch (UnsupportedEncodingException e) {
                logger.error("error parsing the html: " + page.getWebURL().getURL(), e);
                throw new ParseException();
            }
        }
    }
}
