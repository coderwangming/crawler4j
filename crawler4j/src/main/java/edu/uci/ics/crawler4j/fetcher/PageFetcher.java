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

package edu.uci.ics.crawler4j.fetcher;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.net.ssl.SSLContext;

/**
 * 使用httpclient中的类
 */
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uci.ics.crawler4j.crawler.Configurable;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.authentication.AuthInfo;
import edu.uci.ics.crawler4j.crawler.authentication.BasicAuthInfo;
import edu.uci.ics.crawler4j.crawler.authentication.FormAuthInfo;
import edu.uci.ics.crawler4j.crawler.authentication.NtAuthInfo;
import edu.uci.ics.crawler4j.crawler.exceptions.PageBiggerThanMaxSizeException;
import edu.uci.ics.crawler4j.url.URLCanonicalizer;
import edu.uci.ics.crawler4j.url.WebURL;

/**
 * 页面访问者：父类有变量CrawlConfig对象及其getter和setter方法
 * @author Yasser Ganjisaffar
 */
public class PageFetcher extends Configurable {
    protected static final Logger logger = LoggerFactory.getLogger(PageFetcher.class);//日志
    protected final Object mutex = new Object();//互斥锁对象

    //http客户端，fixme 重要的爬虫类，对目标网站执行请求
    protected CloseableHttpClient httpClient;
    protected long lastFetchTime = 0;//上一次访问的时间

    //http客户端池化连接管理器，主要用户设置总共的最大连接数了每个主机的最大连接数
    protected PoolingHttpClientConnectionManager connectionManager;

    protected IdleConnectionMonitorThread connectionMonitorThread = null;//空闲链接监视线程 todo 自定义类

    /**
     * 构造函数1：参数为“爬虫配置类”
     * @param config
     */
    public PageFetcher(CrawlConfig config) {
        super(config);//this.config=config;

        RequestConfig requestConfig = RequestConfig.custom()
                                                   .setExpectContinueEnabled(false)
                                                   .setCookieSpec(config.getCookiePolicy())
//                                                   .setRedirectsEnabled(false)
                                                    .setRedirectsEnabled(config.isFollowRedirects())//fixme:替代上一行代码
                                                   .setSocketTimeout(config.getSocketTimeout())
                                                   .setConnectTimeout(config.getConnectionTimeout())
                                                   .build();

        //RegistryBuilder：注册器实例的构建者；ConnectionSocketFactory：创建和链接“链接套接字”的工厂
        RegistryBuilder<ConnectionSocketFactory> connRegistryBuilder = RegistryBuilder.create();
        connRegistryBuilder.register("http", PlainConnectionSocketFactory.INSTANCE);

        if (config.isIncludeHttpsPages()) {
            try { // Fixing: https://code.google.com/p/crawler4j/issues/detail?id=174 By always trusting the ssl certificate
                //根据“信任策略”加载可信任的“材料”，其实就是新建了一个对象；SSL(security socket layer)：加密套接字协议层；custom:定制的；
                SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustStrategy() {
                        @Override
                        public boolean isTrusted(final X509Certificate[] chain, String authType) {
                            return true;
                        }
                    }).build();
                SSLConnectionSocketFactory sslsf = new SniSSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

                connRegistryBuilder.register("https", sslsf);
            } catch (Exception e) {
                logger.warn("Exception thrown while trying to register https");
                logger.debug("Stacktrace", e);
            }
        }

        Registry<ConnectionSocketFactory> connRegistry = connRegistryBuilder.build();
        connectionManager = new SniPoolingHttpClientConnectionManager(connRegistry, config.getDnsResolver());
        connectionManager.setMaxTotal(config.getMaxTotalConnections());
        connectionManager.setDefaultMaxPerRoute(config.getMaxConnectionsPerHost());

        //httpclient构建者类
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        clientBuilder.setDefaultRequestConfig(requestConfig);
        clientBuilder.setConnectionManager(connectionManager);
        clientBuilder.setUserAgent(config.getUserAgentString());
        clientBuilder.setDefaultHeaders(config.getDefaultHeaders());

        //如果指定了爬虫运行的代理服务器，设置代理服务器相关设置
        if (config.getProxyHost() != null) {
            if (config.getProxyUsername() != null) {//如果有用户名信息
                //“基本证书提供者”
                BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                //为“证书提供者”设置域和包含“用户名+密码”的类
                credentialsProvider.setCredentials(
                    new AuthScope(config.getProxyHost(), config.getProxyPort()),
                    new UsernamePasswordCredentials(config.getProxyUsername(), config.getProxyPassword())
                );
                //为httpclient构建者设置默认的“证书提供者”，也就是代理服务器信息和用户验证信息
                clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }
            //用 代理服务器ip和端口构造HttpHost类对象
            HttpHost proxy = new HttpHost(config.getProxyHost(), config.getProxyPort());
            //设置代理
            clientBuilder.setProxy(proxy);
            logger.debug("Working through Proxy: {}", proxy.getHostName());
        }

        //构造参数为HttpClient类对象赋值
        httpClient = clientBuilder.build();
        //如果验证信息不为空，则进行验证
        if ((config.getAuthInfos() != null) && !config.getAuthInfos().isEmpty()) {
            doAuthetication(config.getAuthInfos());
        }
        if (connectionMonitorThread == null) {
            connectionMonitorThread = new IdleConnectionMonitorThread(connectionManager);
        }
        connectionMonitorThread.start();
    }

    private void doAuthetication(List<AuthInfo> authInfos) {
        for (AuthInfo authInfo : authInfos) {//对需要进行验证的信息一一进行验证
            //如果是基本的验证类型，则进行基本登录
            if (authInfo.getAuthenticationType() == AuthInfo.AuthenticationType.BASIC_AUTHENTICATION) {
                doBasicLogin((BasicAuthInfo) authInfo);
            } //如果是NT_AUTHENTICATION验证类型，则进行doNtLogin
            else if (authInfo.getAuthenticationType() == AuthInfo.AuthenticationType.NT_AUTHENTICATION) {
                doNtLogin((NtAuthInfo) authInfo);
            }//其他这是进行表单验证
            else {
                doFormLogin((FormAuthInfo) authInfo);
            }
        }
    }

    /**
     * BASIC authentication<br/>
     * Official Example: https://hc.apache
     * .org/httpcomponents-client-ga/httpclient/examples/org/apache/http/examples
     * /client/ClientAuthentication.java
     * */
    private void doBasicLogin(BasicAuthInfo authInfo) {
        logger.info("BASIC authentication for: " + authInfo.getLoginTarget());

        //主机、端口和协议构造HttpHost类
        HttpHost targetHost = new HttpHost(authInfo.getHost(), authInfo.getPort(), authInfo.getProtocol());

        //目标主机名称、目标主机端口和用户名、密码设置“信任证书提供者”对象
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()),
                                     new UsernamePasswordCredentials(authInfo.getUsername(), authInfo.getPassword())
        );
        //用“信任证书提供者”信息设置HttpClient对象
        httpClient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
    }

    /**
     * 对于Microsoft网站的验证，代码同“基本验证方式”，只是多了try_catch块
     * Do NT auth for Microsoft AD sites.
     */
    private void doNtLogin(NtAuthInfo authInfo) {
        logger.info("NT authentication for: " + authInfo.getLoginTarget());
        //主机、端口和协议构造HttpHost类，同基本验证方式
        HttpHost targetHost = new HttpHost(authInfo.getHost(), authInfo.getPort(), authInfo.getProtocol());

        //信任证书提供者
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        try {
            credsProvider.setCredentials(
                new AuthScope(targetHost.getHostName(), targetHost.getPort()),
                new NTCredentials(authInfo.getUsername(), authInfo.getPassword(),
                                  InetAddress.getLocalHost().getHostName(), authInfo.getDomain()));
        } catch (UnknownHostException e) {
            logger.error("Error creating NT credentials", e);
        }
        httpClient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
    }

    /**
     * FORM authentication<br/>
     * Official Example:
     *  https://hc.apache.org/httpcomponents-client-ga/httpclient/examples/org/apache/http
     *  /examples/client/ClientFormLogin.java
     */
    private void doFormLogin(FormAuthInfo authInfo) {
        logger.info("FORM authentication for: " + authInfo.getLoginTarget());

        // fixme：httpHost  用验证“网址信息”设置HttpPost
        String fullUri = authInfo.getProtocol() + "://" + authInfo.getHost() + ":" + authInfo.getPort() + authInfo.getLoginTarget();
        HttpPost httpPost = new HttpPost(fullUri);

        //fixme： List<NameValuePair> formParams 参数为被验证信息，以下为将验证信息里边的用户和密码信息放进formParams对象中
        List<NameValuePair> formParams = new ArrayList<>();
        formParams.add(new BasicNameValuePair(authInfo.getUsernameFormStr(), authInfo.getUsername()));
        formParams.add(new BasicNameValuePair(authInfo.getPasswordFormStr(), authInfo.getPassword()));

        try {
            //fixme：UrlEncodedFormEntity用指定的参数对链表和编码格式构造UrlEncodedFormEntity（StringEntity子类）对象
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams, "UTF-8");
            httpPost.setEntity(entity);
            //用默认的上下文执行http请求，TODO 不接受返回结果？
            httpClient.execute(httpPost);
            logger.debug("Successfully Logged in with user: " + authInfo.getUsername() + " to: " +
                         authInfo.getHost());
        } catch (UnsupportedEncodingException e) {
            logger.error("Encountered a non supported encoding while trying to login to: " +
                         authInfo.getHost(), e);
        } catch (ClientProtocolException e) {
            logger.error("While trying to login to: " + authInfo.getHost() +
                         " - Client protocol not supported", e);
        } catch (IOException e) {
            logger.error(
                "While trying to login to: " + authInfo.getHost() + " - Error making request", e);
        }
    }

    /**
     * 爬去指定的URL，主要做了一下三件事儿：
     *      1）检测距离上次爬取是否达到“礼貌时间”，没有的话sleep一段时间；
     *      2）执行请求，并将请求结果的Entity和Headers放在自定义结果类中；
     *      3）将返回结果的Status以适当的方式设置自定义结果类中：2XX成功、3XX重定向
     * @param webUrl
     * @return
     * @throws InterruptedException
     * @throws IOException
     * @throws PageBiggerThanMaxSizeException
     */
    public PageFetchResult fetchPage(WebURL webUrl)
        throws InterruptedException, IOException, PageBiggerThanMaxSizeException {
        // Getting URL, setting headers & content
        PageFetchResult fetchResult = new PageFetchResult();
        HttpUriRequest request = null;
        try {
            String toFetchURL = webUrl.getURL();//原先在try块外
            request = newHttpUriRequest(toFetchURL);

            //如果距离上次爬去url时间较短，则线程停止到指定“礼貌时间”再去爬去 Applying Politeness delay。
            synchronized (mutex) {
                long now = (new Date()).getTime();
                if ((now - lastFetchTime) < config.getPolitenessDelay()) {
                    Thread.sleep(config.getPolitenessDelay() - (now - lastFetchTime));
                }
                lastFetchTime = (new Date()).getTime();
            }

            //fixme 此处执行了请求代码，并将获取到结果的Entity和Headers放在自定义结果类中
            CloseableHttpResponse response = httpClient.execute(request);
            fetchResult.setEntity(response.getEntity());
            fetchResult.setResponseHeaders(response.getAllHeaders());

            // 将返回结果的Status以适当的方式设置自定义结果类；Setting HttpStatus
            int statusCode = response.getStatusLine().getStatusCode();
            // If Redirect ( 3xx )
            if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY ||
                statusCode == HttpStatus.SC_MOVED_TEMPORARILY ||
                statusCode == HttpStatus.SC_MULTIPLE_CHOICES ||
                statusCode == HttpStatus.SC_SEE_OTHER ||
                statusCode == HttpStatus.SC_TEMPORARY_REDIRECT || statusCode ==
                                                                  308) { // todo follow
              // https://issues.apache.org/jira/browse/HTTPCORE-389

                Header header = response.getFirstHeader("Location");
                if (header != null) {
                    String movedToUrl =
                        URLCanonicalizer.getCanonicalURL(header.getValue(), toFetchURL);
                    fetchResult.setMovedToUrl(movedToUrl);
                }
            } else if (statusCode >= 200 && statusCode <= 299) { // is 2XX, everything looks ok
                fetchResult.setFetchedUrl(toFetchURL);
                String uri = request.getURI().toString();
                if (!uri.equals(toFetchURL)) {
                    if (!URLCanonicalizer.getCanonicalURL(uri).equals(toFetchURL)) {
                        fetchResult.setFetchedUrl(uri);
                    }
                }

                // Checking maximum size
                if (fetchResult.getEntity() != null) {
                    long size = fetchResult.getEntity().getContentLength();
                    if (size == -1) {
                        Header length = response.getLastHeader("Content-Length");
                        if (length == null) {
                            length = response.getLastHeader("Content-length");
                        }
                        if (length != null) {
                            size = Integer.parseInt(length.getValue());
                        }
                    }
                    if (size > config.getMaxDownloadSize()) {
                        //fix issue #52 - consume entity
                        response.close();
                        throw new PageBiggerThanMaxSizeException(size);
                    }
                }
            }

            fetchResult.setStatusCode(statusCode);
            return fetchResult;

        } finally { // occurs also with thrown exceptions
            //如果请求信息不为空而且爬取结果为null，终止request的运行
            if ((fetchResult.getEntity() == null) && (request != null)) {
                request.abort();
            }
        }
    }

//    关闭这两个类对象，后者继承了Thread类
//    protected PoolingHttpClientConnectionManager connectionManager;//http客户端池化连接管理器
//    protected IdleConnectionMonitorThread connectionMonitorThread = null;//空闲链接监视线程 todo 自定义类
    public synchronized void shutDown() {
        if (connectionMonitorThread != null) {
            connectionManager.shutdown();
            connectionMonitorThread.shutdown();
        }
    }

    /**
     * 返回指定url的HttpUriRequest；HttpGet的父类实现了接口HttpUriRequest
     * Creates a new HttpUriRequest for the given url. The default is to create a HttpGet without
     * any further configuration. Subclasses may override this method and provide their own logic.
     *
     * @param url the url to be fetched
     * @return the HttpUriRequest for the given url
     */
    protected HttpUriRequest newHttpUriRequest(String url) {
        return new HttpGet(url);
    }

}
