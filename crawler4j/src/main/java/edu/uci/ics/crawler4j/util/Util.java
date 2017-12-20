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

/**
 * 工具类，同名
 * @author Yasser Ganjisaffar
 */
public class Util {

    /**
     * byte类型为8位；long数据类型为64位，是前者8倍<p></p>
     * 16进制0xff = 1111 1111 = 2^8 - 1 = 255<p></p>
     * 返回结果为大端格式
     * @param num
     * @return
     */
    public static byte[] long2ByteArray(long num) {
        byte[] array = new byte[8];
        int i;
        int shift;
        for (i = 0, shift = 56; i < 8; i++, shift -= 8) {//fixme 注意，是从56位开始的，而不是64
            array[i] = (byte) (0xFF & (num >> shift));
        }
        return array;
    }

    //int类型32位
    public static byte[] int2ByteArray(int value) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            int offset = (3 - i) * 8;
            b[i] = (byte) ((value >>> offset) & 0xFF);
        }
        return b;
    }

    public static void putIntInByteArray(int value, byte[] buf, int offset) {
        for (int i = 0; i < 4; i++) {
            int valueOffset = (3 - i) * 8;
            buf[offset + i] = (byte) ((value >>> valueOffset) & 0xFF);
        }
    }

    public static int byteArray2Int(byte[] b) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i] & 0x000000FF) << shift;
        }
        return value;
    }

    public static long byteArray2Long(byte[] b) {
        int value = 0;
        for (int i = 0; i < 8; i++) {
            int shift = (8 - 1 - i) * 8;
            value += (b[i] & 0x000000FF) << shift;
        }
        return value;
    }

    /**
     * 根据“Response Headers”的“Content-Type”判定返回结果是否包含二进制数据
     * @param contentType 判定是否包含二进制数据的内容
     * @return true if contain binaryContent
     */
    public static boolean hasBinaryContent(String contentType) {
        String typeStr = (contentType != null) ? contentType.toLowerCase() : "";

        //如果包含图片、音频、视频或者应用其中一样，即二进制内容
        return typeStr.contains("image") || typeStr.contains("audio") ||
               typeStr.contains("video") || typeStr.contains("application");
    }

    /**
     *根据“Response Headers”的“Content-Type”判定返回结果是否是纯文本，
     * 如果返回结果是“text”而且不包含“html”
     * @param contentType
     * @return
     */
    public static boolean hasPlainTextContent(String contentType) {
        String typeStr = (contentType != null) ? contentType.toLowerCase() : "";

        return typeStr.contains("text") && !typeStr.contains("html");
    }

}