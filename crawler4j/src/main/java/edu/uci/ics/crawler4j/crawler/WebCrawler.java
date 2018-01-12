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

package edu.uci.ics.crawler4j.crawler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

//httpclient的类：1XX是信息；2XX是成功；3XX是重定向；4XX是客户端错误；5XX是服务器错误；
import org.apache.http.HttpStatus;
//英文 原因短语目录：应该保存着那些Not Found之类的话
import org.apache.http.impl.EnglishReasonPhraseCatalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uci.ics.crawler4j.crawler.exceptions.ContentFetchException;
import edu.uci.ics.crawler4j.crawler.exceptions.PageBiggerThanMaxSizeException;
import edu.uci.ics.crawler4j.crawler.exceptions.ParseException;
import edu.uci.ics.crawler4j.fetcher.PageFetchResult;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.frontier.DocIDServer;
import edu.uci.ics.crawler4j.frontier.Frontier;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.crawler.exceptions.NotAllowedContentException;
import edu.uci.ics.crawler4j.parser.ParseData;
import edu.uci.ics.crawler4j.parser.Parser;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import edu.uci.ics.crawler4j.url.WebURL;

/**
 * WebCrawler继承Runnable接口，被每一个爬虫线程执行
 * WebCrawler class in the Runnable class that is executed by each crawler thread.
 *
 * @author Yasser Ganjisaffar
 */
public class WebCrawler implements Runnable {

    protected static final Logger logger = LoggerFactory.getLogger(WebCrawler.class);

    /**
     * 与运行此实例想噶是你的爬虫线程相关的id
     * The id associated to the crawler thread running this instance
     */
    protected int myId;

    /**
     * The controller instance that has created this crawler thread. This
     * reference to the controller can be used for getting configurations of the
     * current crawl or adding new seeds during runtime.
     */
    protected CrawlController myController;

    /**
     * 此爬虫实例运行的线程
     * The thread within which this crawler instance is running.
     */
    private Thread myThread;

    /**
     * 这个解析器用户这个爬虫实例解析爬取的内容
     * The parser that is used by this crawler instance to parse the content of the fetched pages.
     */
    private Parser parser;

    /**
     * The fetcher that is used by this crawler instance to fetch the content of pages from the web.
     */
    private PageFetcher pageFetcher;

    /**
     * 爬虫实例用“机器人文本服务”实例来检测爬虫是否允许爬去页面的内容
     * The RobotstxtServer instance that is used by this crawler instance to
     * determine whether the crawler is allowed to crawl the content of each page.
     */
    private RobotstxtServer robotstxtServer;

    /**
     * The DocIDServer that is used by this crawler instance to map each URL to a unique docid.
     */
    private DocIDServer docIdServer;

    /**
     * Frontier用于管理爬虫队列
     * The Frontier object that manages the crawl queue.
     */
    private Frontier frontier;

    /**
     * 现在的爬虫实例是否在等待着新的URLs</p>
     * 这个变量的主要作用是让CrawlerController识别是否所有爬虫实例都在等待新的
     * URLs，如果没有URLs待爬去的话，爬虫工作便可以停了。
     * Is the current crawler instance waiting for new URLs? This field is
     * mainly used by the controller to detect whether all of the crawler
     * instances are waiting for new URLs and therefore there is no more work
     * and crawling can be stopped.
     */
    private boolean isWaitingForNewURLs;

    /**
     * Initializes the current instance of the crawler
     * 初始化现在的爬虫实例
     * @param id  爬虫实例的id，也是开启的线程的序号
     *            the id of this crawler instance
     * @param crawlController   管理这次爬虫会话的控制器
     *            the controller that manages this crawling session
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public void init(int id, CrawlController crawlController)
        throws InstantiationException, IllegalAccessException {
        this.myId = id;
        this.pageFetcher = crawlController.getPageFetcher();
        this.robotstxtServer = crawlController.getRobotstxtServer();
        this.docIdServer = crawlController.getDocIdServer();
        this.frontier = crawlController.getFrontier();
        this.parser = new Parser(crawlController.getConfig());
        this.myController = crawlController;
        this.isWaitingForNewURLs = false;
    }

    /**
     * Get the id of the current crawler instance
     *
     * @return the id of the current crawler instance
     */
    public int getMyId() {
        return myId;
    }

    public CrawlController getMyController() {
        return myController;
    }

    /**
     * This function is called just before starting the crawl by this crawler
     * instance. It can be used for setting up the data structures or
     * initializations needed by this crawler instance.
     */
    public void onStart() {
        // Do nothing by default
        // Sub-classed can override this to add their custom functionality
    }

    /**
     * This function is called just before the termination of the current
     * crawler instance. It can be used for persisting in-memory data or other
     * finalization tasks.
     */
    public void onBeforeExit() {
        // Do nothing by default
        // Sub-classed can override this to add their custom functionality
    }

    /**
     * 可以被子类继承已处理——当得到返回结果PageResult并获取其对应的状态码时，根据不同的状态码进行不同的处理方式
     * This function is called once the header of a page is fetched. It can be
     * overridden by sub-classes to perform custom logic for different status
     * codes. For example, 404 pages can be logged, etc.
     *
     * @param webUrl WebUrl containing the statusCode
     * @param statusCode Html Status Code number
     * @param statusDescription Html Status COde description
     */
    protected void handlePageStatusCode(WebURL webUrl, int statusCode, String statusDescription) {
        // Do nothing by default
        // Sub-classed can override this to add their custom functionality
    }

    /**
     * This function is called before processing of the page's URL
     * It can be overridden by subclasses for tweaking of the url before processing it.
     * For example, http://abc.com/def?a=123 - http://abc.com/def
     * <p></p>使用url之前先处理他，比如去掉查询参数
     * @param curURL current URL which can be tweaked before processing
     * @return tweaked WebURL
     */
    protected WebURL handleUrlBeforeProcess(WebURL curURL) {
        return curURL;
    }

    /**
     * This function is called if the content of a url is bigger than allowed size.
     *
     * @param urlStr - The URL which it's content is bigger than allowed size
     */
    protected void onPageBiggerThanMaxSize(String urlStr, long pageSize) {
        logger.warn("Skipping a URL: {} which was bigger ( {} ) than max allowed size", urlStr,
                    pageSize);
    }

    /**
     * 这个方法用户爬虫遇到返回状态码为3XX的情况
     * This function is called if the crawler encounters a page with a 3xx status code
     *
     * @param page Partial page object
     */
    protected void onRedirectedStatusCode(Page page) {
        //Subclasses can override this to add their custom functionality
    }

    /**
     * This function is called if the crawler encountered an unexpected http status code ( a
     * status code other than 3xx)
     *
     * @param urlStr URL in which an unexpected error was encountered while crawling
     * @param statusCode Html StatusCode
     * @param contentType Type of Content
     * @param description Error Description
     */
    protected void onUnexpectedStatusCode(String urlStr, int statusCode, String contentType,
                                          String description) {
        logger.warn("Skipping URL: {}, StatusCode: {}, {}, {}", urlStr, statusCode, contentType,
                    description);
        // Do nothing by default (except basic logging)
        // Sub-classed can override this to add their custom functionality
    }

    /**
     * This function is called if the content of a url could not be fetched.
     *
     * @param webUrl URL which content failed to be fetched
     */
    protected void onContentFetchError(WebURL webUrl) {
        logger.warn("Can't fetch content of: {}", webUrl.getURL());
        // Do nothing by default (except basic logging)
        // Sub-classed can override this to add their custom functionality
    }

    /**
     * This function is called when a unhandled exception was encountered during fetching
     *
     * @param webUrl URL where a unhandled exception occured
     */
    protected void onUnhandledException(WebURL webUrl, Throwable e) {
        String urlStr = (webUrl == null ? "NULL" : webUrl.getURL());
        logger.warn("Unhandled exception while fetching {}: {}", urlStr, e.getMessage());
        logger.info("Stacktrace: ", e);
        // Do nothing by default (except basic logging)
        // Sub-classed can override this to add their custom functionality
    }

    /**
     * This function is called if there has been an error in parsing the content.
     *
     * @param webUrl URL which failed on parsing
     */
    protected void onParseError(WebURL webUrl) {
        logger.warn("Parsing error of: {}", webUrl.getURL());
        // Do nothing by default (Except logging)
        // Sub-classed can override this to add their custom functionality
    }

    /**
     * The CrawlController instance that has created this crawler instance will
     * call this function just before terminating this crawler thread. Classes
     * that extend WebCrawler can override this function to pass their local
     * data to their controller. The controller then puts these local data in a
     * List that can then be used for processing the local data of crawlers (if needed).
     *
     * @return currently NULL
     */
    public Object getMyLocalData() {
        return null;
    }

    /**
     * fixme 爬虫工作的代码
     * 前置处理方法有两个：
     *      1.一个是每个线程开始run方法时的潜质处理方法onStart()
     *      2.一个是处理工作队列中每个WebURL对象时的前置处理方法 curURL = handleUrlBeforeProcess(curURL)
     */
    @Override
    public void run() {
        onStart();//爬虫爬取工作之前调用，可以在子类重写此方法，然后进行一些自己的“前置设置”；重新的方法还有visit和shouldVisit

        while (true) {
            //存储根据种子地址找到的urlList
            List<WebURL> assignedURLs = new ArrayList<>(50);
            //现在的爬虫实例是否在等待新的url，如果所有的都没有在等待，则CrawlerController可以停止爬去工作了
            isWaitingForNewURLs = true;

            //fixme Frontier对象用于控制爬虫实例队列。下行代码用于从工作队列取出50个url放进第二个参数
            frontier.getNextURLs(50, assignedURLs);
            isWaitingForNewURLs = false;
            if (assignedURLs.isEmpty()) {//如果url列表为空，即工作队列为空
                if (frontier.isFinished()) {//而且frontier已经结束，则停止run方法。
                    return;
                }
                try {
                    Thread.sleep(3000);//工作队列为空，但是frontier没有结束，则睡一会，重新开始循环：从工作队列中去url，进行判定...
                } catch (InterruptedException e) {
                    logger.error("Error occurred", e);
                }
            } else {//如果url列表不为空
                for (WebURL curURL : assignedURLs) { //遍历url
                    if (myController.isShuttingDown()) {//为true表示爬虫需要停止工作
                        logger.info("Exiting because of controller shutdown.");
                        return;
                    }
                    if (curURL != null) {//todo 经过多个判定，终于达到了调用工作线程的地方
                        curURL = handleUrlBeforeProcess(curURL);//前置处理，比如去掉参数
                        processPage(curURL);//fetcher包中的东西，执行请求并处理返回结果
                        frontier.setProcessed(curURL);
                    }
                }
            }
        }
    }

    /**
     *
     * 原始方法内容为如果连接包含“诺follow”标签则不再爬去
     * Classes that extends WebCrawler should overwrite this function to tell the
     * crawler whether the given url should be crawled or not. The following
     * default implementation indicates that all urls should be included in the crawl
     * except those with a nofollow flag.
     *
     * @param url
     *            the url which we are interested to know whether it should be
     *            included in the crawl or not.
     * @param referringPage
     *           The Page in which this url was found.
     * @return if the url should be included in the crawl it returns true,
     *         otherwise false is returned.
     */
    public boolean shouldVisit(Page referringPage, WebURL url) {
        if (myController.getConfig().isRespectNoFollow()) {
            return !((referringPage != null &&
                    referringPage.getContentType() != null &&
                    referringPage.getContentType().contains("html") &&
                    ((HtmlParseData)referringPage.getParseData())
                        .getMetaTagValue("robots")
                        .contains("nofollow")) ||
                    url.getAttribute("rel").contains("nofollow"));
        }

        return true;
    }

    /**
     * 检测所给的URL包含的链接是否应该加到爬去队列中，默认一直是true，子类可以实现具体的逻辑
     * Determine whether links found at the given URL should be added to the queue for crawling.
     * By default this method returns true always, but classes that extend WebCrawler can
     * override it in order to implement particular policies about which pages should be
     * mined for outgoing links and which should not.
     *
     * If links from the URL are not being followed, then we are not operating as
     * a web crawler and need not check robots.txt before fetching the single URL.
     * (see definition at http://www.robotstxt.org/faq/what.html).  Thus URLs that
     * return false from this method will not be subject to robots.txt filtering.
     *
     * @param url the URL of the page under consideration
     * @return true if outgoing links from this page should be added to the queue.
     */
    protected boolean shouldFollowLinksIn(WebURL url) {
        return true;
    }

    /**
     * 此方法对象为爬去的页面数据，继承此方法的类应该重写此方法来判定怎样处理爬取的页面
     * Classes that extends WebCrawler should overwrite this function to process
     * the content of the fetched and parsed page.
     *
     * @param page
     *            the page object that is just fetched and parsed.
     */
    public void visit(Page page) {
        // Do nothing by default
        // Sub-classed should override this to add their custom functionality
    }

    /**
     * 被run调用，对目标url执行请求并处理返回结果...
     * @param curURL
     */
    private void processPage(WebURL curURL) {
        PageFetchResult fetchResult = null;//执行请求的返回结果
        try {
            if (curURL == null) { return; }//如果目标url为null则结束函数的运行

            fetchResult = pageFetcher.fetchPage(curURL);//结果主要包含Entity、Header链表、statusCode以及需要重定向的url等信息

            int statusCode = fetchResult.getStatusCode();
            //对不同的状态码进行不同的处理，此方法被子类继承并重写逻辑
            handlePageStatusCode(curURL, statusCode,EnglishReasonPhraseCatalog.INSTANCE.getReason(statusCode, Locale.ENGLISH));

            //包含网页爬取和解析的数据,例如statusCode、内容、内容类型、重定向地址等
            Page page = new Page(curURL);
            page.setFetchResponseHeaders(fetchResult.getResponseHeaders());
            page.setStatusCode(statusCode);

            // Not 2XX: 2XX status codes indicate success;返回结果不表示成功
            if (statusCode < 200 || statusCode > 299) {
                // is 3xx  重定向 todo 暂时不做这么复杂的考虑
                if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY ||
                    statusCode == HttpStatus.SC_MOVED_TEMPORARILY ||
                    statusCode == HttpStatus.SC_MULTIPLE_CHOICES ||
                    statusCode == HttpStatus.SC_SEE_OTHER ||
                    statusCode == HttpStatus.SC_TEMPORARY_REDIRECT ||
                    statusCode == 308) {
                    // follow https://issues.apache.org/jira/browse/HTTPCORE-389

                    page.setRedirect(true);

                    String movedToUrl = fetchResult.getMovedToUrl();//重定向页面的设置在fetchPage(...)中被设置
                    if (movedToUrl == null) {
                        logger.warn("Unexpected error, URL: {} is redirected to NOTHING",
                                    curURL);
                        return;
                    }
                    page.setRedirectedToUrl(movedToUrl);
                    onRedirectedStatusCode(page);

                    if (myController.getConfig().isFollowRedirects()) {
                        int newDocId = docIdServer.getDocId(movedToUrl);
                        if (newDocId > 0) {
                            logger.debug("Redirect page: {} is already seen", curURL);
                            return;
                        }

                        WebURL webURL = new WebURL();
                        webURL.setURL(movedToUrl);
                        webURL.setParentDocid(curURL.getParentDocid());
                        webURL.setParentUrl(curURL.getParentUrl());
                        webURL.setDepth(curURL.getDepth());
                        webURL.setDocid(-1);
                        webURL.setAnchor(curURL.getAnchor());
                        if (shouldVisit(page, webURL)) {
                            if (!shouldFollowLinksIn(webURL) || robotstxtServer.allows(webURL)) {
                                webURL.setDocid(docIdServer.getNewDocID(movedToUrl));
                                frontier.schedule(webURL);
                            } else {
                                logger.debug(
                                    "Not visiting: {} as per the server's \"robots.txt\" policy",
                                    webURL.getURL());
                            }
                        } else {
                            logger.debug("Not visiting: {} as per your \"shouldVisit\" policy",
                                         webURL.getURL());
                        }
                    }
                } else { // All other http codes other than 3xx & 200；反正就是不成功
                    String description =
                        EnglishReasonPhraseCatalog.INSTANCE.getReason(fetchResult.getStatusCode(),
                                                                      Locale.ENGLISH); // Finds
                    // the status reason for all known statuses
                    String contentType = fetchResult.getEntity() == null ? "" :
                                         fetchResult.getEntity().getContentType() == null ? "" :
                                         fetchResult.getEntity().getContentType().getValue();
                    onUnexpectedStatusCode(curURL.getURL(), fetchResult.getStatusCode(),
                                           contentType, description);
                }

            } else { // if status code is 200 fixme 爬取页面成功
                if (!curURL.getURL().equals(fetchResult.getFetchedUrl())) {//？？如果爬取的是重定向的地址
                    //这个url是否被访问过，结束方法的执行 todo 不是应该在发送请求之前查看其是否被访问过吗
                    if (docIdServer.isSeenBefore(fetchResult.getFetchedUrl())) {
                        logger.debug("Redirect page: {} has already been seen", curURL);
                        return;
                    }
                    curURL.setURL(fetchResult.getFetchedUrl());
                    curURL.setDocid(docIdServer.getNewDocID(fetchResult.getFetchedUrl()));
                }

                //改变了参数Page page，从爬取网页的HttpEntity中加载网页的内容
                if (!fetchResult.fetchContent(page,myController.getConfig().getMaxDownloadSize())) {
                    throw new ContentFetchException();
                }

                //内容是否因为超过了可以接受的最大值而被删节。这里做一个提示而已
                if (page.isTruncated()) {
                    logger.warn("Warning: unknown page size exceeded max-download-size, truncated to: ({}), at URL: {}",
                        myController.getConfig().getMaxDownloadSize(), curURL.getURL());
                }

                //对网页进行解析，将相关数据放在参数page中。稍后进行展开讲
                parser.parse(page, curURL.getURL());

                if (shouldFollowLinksIn(page.getWebURL())) {   //如果这个网址包含的链接应该被访问，方法默认返回TRUE。应该被子类重新
                    ParseData parseData = page.getParseData();
                    //待爬取的网页链表
                    List<WebURL> toSchedule = new ArrayList<>();
                    int maxCrawlDepth = myController.getConfig().getMaxDepthOfCrawling();

                    for (WebURL webURL : parseData.getOutgoingUrls()) {//遍历这个网址包含的链接
                        //设置父类url信息
                        webURL.setParentDocid(curURL.getDocid());
                        webURL.setParentUrl(curURL.getURL());

                        int newdocid = docIdServer.getDocId(webURL.getURL());
                        if (newdocid > 0) {//如果之前被访问过，否则时候-1
                            // This is not the first time that this Url is visited. So, we set the depth to a negative number.
                            webURL.setDepth((short) -1);
                            webURL.setDocid(newdocid);
                        } else {//如果之前未被访问过
                            webURL.setDocid(-1);//？？？ todo 未被访问过的都是-1啊
                            webURL.setDepth((short) (curURL.getDepth() + 1));

                            //如果不限制深度或者现在深度小于最大深度
                            if ((maxCrawlDepth == -1) || (curURL.getDepth() < maxCrawlDepth)) {
                                if (shouldVisit(page, webURL)) {//调用子类一般会重写的方法：如果应该访问这个页面
                                    if (robotstxtServer.allows(webURL)) {//bad url的情况下会返回true。页面的内容是否允许被爬取
                                        webURL.setDocid(docIdServer.getNewDocID(webURL.getURL()));//内部方法为同步方法
                                        toSchedule.add(webURL);//添加到待爬取队列
                                    } else {
                                        logger.debug(//根据网站的robots政策不去爬去这个页面
                                            "Not visiting: {} as per the server's \"robots.txt\" " +
                                            "policy", webURL.getURL());
                                    }
                                } else {
                                    logger.debug(//根据自定义的shouldVisit函数不爬去这个页面
                                        "Not visiting: {} as per your \"shouldVisit\" policy",
                                        webURL.getURL());
                                }
                            }//end of “允许深度”
                        }//end of "如果之前未被访问过”
                    }//end of 遍历所有网址
                    frontier.scheduleAll(toSchedule);  //List<WebURL> toSchedule，待爬取网页
                } else {
                    //todo 为什么有了shouldVisit还有这个呢
                    logger.debug("Not looking for links in page {}, "
                                 + "as per your \"shouldFollowLinksInPage\" policy",
                                 page.getWebURL().getURL());
                }

                boolean noIndex = myController.getConfig().isRespectNoIndex() &&//遵循“不索引”策略
                    page.getContentType() != null &&
                    page.getContentType().contains("html") &&//内容类型包含“html”
                    ((HtmlParseData)page.getParseData()).getMetaTagValue("robots").contains("noindex");//内容包含noindex标识

                if (!noIndex) {//如果可以访问
                    visit(page);//fixme 被子类重写的方法
                }
            }
        } catch (PageBiggerThanMaxSizeException e) {
            onPageBiggerThanMaxSize(curURL.getURL(), e.getPageSize());
        } catch (ParseException pe) {
            onParseError(curURL);
        } catch (ContentFetchException cfe) {
            onContentFetchError(curURL);
        } catch (NotAllowedContentException nace) {
            logger.debug(
                "Skipping: {} as it contains binary content which you configured not to crawl",
                curURL.getURL());
        } catch (Exception e) {
            onUnhandledException(curURL, e);
        } finally {
            if (fetchResult != null) {//如果返回结果不为null
                fetchResult.discardContentIfNotConsumed();//丢弃内容...
            }
        }
    }

    public Thread getThread() {
        return myThread;
    }

    public void setThread(Thread myThread) {
        this.myThread = myThread;
    }

    public boolean isNotWaitingForNewURLs() {
        return !isWaitingForNewURLs;
    }
}
