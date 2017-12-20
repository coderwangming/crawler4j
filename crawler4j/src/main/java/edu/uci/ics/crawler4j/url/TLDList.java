package edu.uci.ics.crawler4j.url;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 这个类是一个包含一些列来自网上或者本地的定居域名集的“单例模式”——以便可以和“这些”顶级域名比较
 * <p></p>This class is a singleton which obtains a list of TLDs (from online or a local file) in order
 * to compare against
 * those TLDs
 */
public class TLDList {

    //在线顶级域名集合
    private static final String TLD_NAMES_ONLINE_URL =
        "https://publicsuffix.org/list/effective_tld_names.dat";
    //本地顶级域名集合
    private static final String TLD_NAMES_TXT_FILENAME = "tld-names.txt";

    private static final Logger logger = LoggerFactory.getLogger(TLDList.class);
    //是否用网址TLD_NAMES_ONLINE_URL在线更新顶级域名数据集，FALSE则表示在本地获取数据集
    private static boolean onlineUpdate = false;
    //顶级域名数据集合
    private final Set<String> tldSet = new HashSet<>(10000);

    private static final TLDList instance = new TLDList(); // Singleton，饿汉式初始化

    //饿汉式初始化单利调用的构造函数
    private TLDList() {
        if (onlineUpdate) {//如果在线更新顶级域名数据集
            URL url;
            try {
                url = new URL(TLD_NAMES_ONLINE_URL);
            } catch (MalformedURLException e) {
                // This cannot happen... No need to treat it
                logger.error("Invalid URL: {}", TLD_NAMES_ONLINE_URL);
                throw new RuntimeException(e);
            }

            try (InputStream stream = url.openStream()) {
                logger.debug("Fetching the most updated TLD list online");
                int n = readStream(stream);//这一步将“网络中顶级域名数据集”放在了类变量中
                logger.info("Obtained {} TLD from URL {}", n, TLD_NAMES_ONLINE_URL);
                return;
            } catch (Exception e) {
                logger.error("Couldn't fetch the online list of TLDs from: {}",
                             TLD_NAMES_ONLINE_URL, e);
            }
        }
        //用本地文件获取“顶级域名数据集”
        File f = new File(TLD_NAMES_TXT_FILENAME);
        if (f.exists()) {
            logger.debug("Fetching the list from a local file {}", TLD_NAMES_TXT_FILENAME);
            try (InputStream tldFile = new FileInputStream(f)) {
                int n = readStream(tldFile);
                logger.info("Obtained {} TLD from local file {}", n, TLD_NAMES_TXT_FILENAME);
                return;
            } catch (IOException e) {
                logger.error("Couldn't read the TLD list from local file", e);
            }
        }
        try (InputStream tldFile = getClass().getClassLoader()
                                             .getResourceAsStream(TLD_NAMES_TXT_FILENAME)) {
            int n = readStream(tldFile);
            logger.info("Obtained {} TLD from packaged file {}", n, TLD_NAMES_TXT_FILENAME);
        } catch (IOException e) {
            logger.error("Couldn't read the TLD list from file");
            throw new RuntimeException(e);
        }
    }

    /**
     * 将网址指定的网站内容读取到 顶级域名数据集合 中，并返回数据集大小
     * @param stream 网址对应的InputStream对象
     * @return
     */
    private int readStream(InputStream stream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("//")) {//如果是空行或者注释行
                    continue;
                }
                tldSet.add(line);//不是空行或者注释行，将数据添加到类变量 顶级域名数据集合 中
            }
        } catch (IOException e) {
            logger.warn("Error while reading TLD-list: {}", e.getMessage());
        }
        return tldSet.size();
    }

    public static TLDList getInstance() {
        return instance;
    }

    /**
     * If {@code online} is set to true, the list of TLD files will be downloaded and refreshed,
     * otherwise the one cached in src/main/resources/tld-names.txt will be used.
     */
    public static void setUseOnline(boolean online) {
        onlineUpdate = online;
    }

    /**
     * 顶级域名数据集是否包含str
     * @param str
     * @return
     */
    public boolean contains(String str) {
        return tldSet.contains(str);
    }
}
