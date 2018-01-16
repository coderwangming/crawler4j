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

package edu.uci.ics.crawler4j.frontier;

import java.util.ArrayList;
import java.util.List;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;

import edu.uci.ics.crawler4j.url.WebURL;
import edu.uci.ics.crawler4j.util.Util;

/**
 * @author Yasser Ganjisaffar
 */
public class WorkQueues {
    //数据库句柄
    private final Database urlsDB;
    //数据库环境，包括缓存、事务、日志以及锁的支持。
    private final Environment env;
    //是否可恢复
    private final boolean resumable;
    //webURL元组绑定：继承自抽象类 TupleBinding<WebURL>，并重写父类两个方法
    private final WebURLTupleBinding webURLBinding;
    //互斥体
    protected final Object mutex = new Object();

    /**
     *
     * @param env
     * @param dbName
     * @param resumable
     */
    public WorkQueues(Environment env, String dbName, boolean resumable) {
        //构造参数注入值
        this.env = env;
        this.resumable = resumable;
        //初始化
        DatabaseConfig dbConfig = new DatabaseConfig();
        //Configures the Environment.openDatabase method to create the database if it does not already exist.
        dbConfig.setAllowCreate(true);
        dbConfig.setTransactional(resumable);//是否开启事务
        dbConfig.setDeferredWrite(!resumable);//是否延迟撰写
        urlsDB = env.openDatabase(null, dbName, dbConfig);
        webURLBinding = new WebURLTupleBinding();
    }

    /**
     * 开启事务，如果构造参数中设置不开启事务，则返回null；
     * @return
     */
    protected Transaction beginTransaction() {
        return resumable ? env.beginTransaction(null, null) : null;
    }

    /**
     * 调用Transaction的commit()方法，前边加个对事务对象的判空，因为可能构造参数中没有设置事务
     * <p></p>结束事务。如果Environment对象被配置为同步提交，则事务将会在“呼叫返回/call returns”
     * 被同步提交给“稳定的存储/stable storage”。则意味着事务将会被禁止所有的ACID属性。
     * @param tnx
     */
    protected static void commit(Transaction tnx) {
        if (tnx != null) {
            //结束事务。如果Environment对象被配置为同步提交，则事务将会在“呼叫返回/call returns”
            // 被同步提交给“稳定的存储/stable storage”。则意味着事务将会被禁止所有的ACID属性。
            tnx.commit();
        }
    }

    /**
     * 打开关联此数据库的游标。指定事物或者游标配置；
     * 提交事务，所进行的操作开始生效
     * @param txn 事务
     * @return
     */
    protected Cursor openCursor(Transaction txn) {
        return urlsDB.openCursor(txn, null);
    }

    /**
     * 同步方法：取出工作队列中数量为max的url，队列中url小于max则全部返回
     * @param max 取出url的最大值
     * @return 返回队列中取出的urls集合
     */
    public List<WebURL> get(int max) {
        synchronized (mutex) {//相比synchronized方法，这种方式指定了加锁对象

            //初始化使用的对象
            List<WebURL> results = new ArrayList<>(max);//返回结果
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry value = new DatabaseEntry();
            Transaction txn = beginTransaction();

            try (Cursor cursor = openCursor(txn)) {//打开关联此类变量数据库的游标
                //将游标指向数据库的第一个键值对，并返回这个键值对：修改了参数。
                // 成功者返回OperationStatus.SUCCESS,否则返回XXX.NOTFOUND
                OperationStatus result = cursor.getFirst(key, value, null);
                int matches = 0;
                //当取出键值对数量小于指定最大值而且成功获取数据时
                while ((matches < max) && (result == OperationStatus.SUCCESS)) {
                    if (value.getData().length > 0) {//如果键值对的值存储了数据
                        results.add(webURLBinding.entryToObject(value));
                        matches++;
                    }
                    result = cursor.getNext(key, value, null);//将游标指向下一个键值对，并且返回(即放在参数中
                }
            }
            commit(txn);//结束事物，即提交事务，所进行的操作开始生效
            return results;
        }
    }

    public void delete(int count) {
        synchronized (mutex) {//指定加锁对象
            //创建使用类对象
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry value = new DatabaseEntry();
            Transaction txn = beginTransaction();

            //如果有事务，则以开启事务的方式打开指向数据的游标
            try (Cursor cursor = openCursor(txn)) {
                OperationStatus result = cursor.getFirst(key, value, null);
                int matches = 0;
                while ((matches < count) && (result == OperationStatus.SUCCESS)) {
                    cursor.delete();//删除游标指向的键值对
                    matches++;
                    result = cursor.getNext(key, value, null);
                }
            }
            commit(txn);//结束事务，即提交事务，所进行的操作开始生效
        }
    }

    /*
     * The key that is used for storing URLs determines the order
     * they are crawled. Lower key values results in earlier crawling.
     * Here our keys are 6 bytes. The first byte comes from the URL priority.
     * The second byte comes from depth of crawl at which this URL is first found.
     * The rest of the 4 bytes come from the docid of the URL. As a result,
     * URLs with lower priority numbers will be crawled earlier. If priority
     * numbers are the same, those found at lower depths will be crawled earlier.
     * If depth is also equal, those found earlier (therefore, smaller docid) will
     * be crawled earlier.
     */
    protected static DatabaseEntry getDatabaseEntryKey(WebURL url) {
        byte[] keyData = new byte[6];
        keyData[0] = url.getPriority();
        keyData[1] = ((url.getDepth() > Byte.MAX_VALUE) ? Byte.MAX_VALUE : (byte) url.getDepth());
        Util.putIntInByteArray(url.getDocid(), keyData, 2);
        return new DatabaseEntry(keyData);
    }

    public void put(WebURL url) {
        DatabaseEntry value = new DatabaseEntry();
        webURLBinding.objectToEntry(url, value);
        Transaction txn = beginTransaction();
        urlsDB.put(txn, getDatabaseEntryKey(url), value);
        commit(txn);
    }

    public long getLength() {
        return urlsDB.count();
    }

    public void close() {
        urlsDB.close();
    }
}