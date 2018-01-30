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

import java.util.concurrent.TimeUnit;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/**
 * 类名“空闲连结监视线程”，继承线程类
 * 每过5秒关闭空闲的链接和过期的链接
 */
public class IdleConnectionMonitorThread extends Thread {

    private final PoolingHttpClientConnectionManager connMgr;
    private volatile boolean shutdown;

    public IdleConnectionMonitorThread(PoolingHttpClientConnectionManager connMgr) {
        super("Connection Manager");//线程名称“链接管理器”
        this.connMgr = connMgr;
    }

    @Override
    public void run() {
        try {
            while (!shutdown) {
                synchronized (this) {
                    wait(5000);//fixme 在其他线程调用此方法的notify()或者notifyAll()时，等待最长X毫秒
                    // Close expired connections
                    connMgr.closeExpiredConnections();//关闭过期的链接
                    // Optionally, close connections that have been idle longer than 30 sec
                    //关闭空闲的链接（空闲的时间长度，空闲的时间单位）
                    connMgr.closeIdleConnections(30, TimeUnit.SECONDS);
                }
            }
        } catch (InterruptedException ignored) {
            // terminate
        }
    }

    public void shutdown() {
        shutdown = true;
        synchronized (this) {
            notifyAll();//fixme 唤醒此对象监视器上的所有进程
        }
    }
}