/*----------------------------------------------------------------------
 * Copyright 2017 realglobe Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *----------------------------------------------------------------------*/

package jp.realglobe.util.uploader;

/**
 * 生成・取得した動作用データを保存する
 */
public interface Store {

    /**
     * データを取り出す
     * @param key 取り出すデータのキー
     * @return データ値。キーに対応するデータが無い場合は null
     * @throws Exception エラー
     */
    public String load(String key) throws Exception;

    /**
     * データを消す
     * @param key 消すデータのキー
     * @throws Exception エラー
     */
    public void clear(String key) throws Exception;

    /**
     * データを保存する
     * @param key データのキー
     * @param value 保存するデータ値
     * @throws Exception エラー
     */
    public void store(String key, String value) throws Exception;

}
