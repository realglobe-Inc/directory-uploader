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

import java.nio.charset.Charset;

/**
 * 固定値
 */
final class Constants {

    // インスタンス化防止
    private Constants() {}

    static final Charset UTF8 = Charset.forName("UTF-8");

    static final String STORE_KEY_ID = "id";
    static final String STORE_KEY_TOKEN = "token";

    static final String URL_PATH_TOKEN = "/cameras";
    static final String URL_PATH_UPLOAD_PREFIX = "/cameras";
    static final String URL_PATH_UPLOAD_SUFFIX = "/photos";

    static final String TOKEN_RESPONSE_KEY_DATA = "created";
    static final String TOKEN_RESPONSE_KEY_TOKEN = "token";

    static final String UPLOAD_REQUEST_PART_TOKEN = "token";
    static final String UPLOAD_REQUEST_PART_DATA = "image";

}
