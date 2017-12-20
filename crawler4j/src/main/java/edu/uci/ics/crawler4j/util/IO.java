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

package edu.uci.ics.crawler4j.util;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * �����ࣺɾ���ļ��к��ļ�
 * @author Yasser Ganjisaffar
 */
public class IO {
    private static final Logger logger = LoggerFactory.getLogger(IO.class);

    /**
     * ɾ���ļ���
     * @param folder ��ɾ�����ļ���
     * @return  �ļ����Ƿ�ɾ��
     */
    public static boolean deleteFolder(File folder) {
        //ɾ���ļ���������ݲ���ɾ���ļ���
        return deleteFolderContents(folder) && folder.delete();
    }

    /**
     * ɾ���ļ����������
     * @param folder ɾ���������ڵ��ļ���
     * @return
     */
    public static boolean deleteFolderContents(File folder) {
        logger.debug("Deleting content of: " + folder.getAbsolutePath());
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                if (!file.delete()) {//ɾ��ʧ�ܣ��򷵻�FALSE
                    return false;
                }
            } else {
                if (!deleteFolder(file)) {
                    return false;
                }
            }
        }
        return true;
    }
}