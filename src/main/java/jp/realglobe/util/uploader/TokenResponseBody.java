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

import org.json.JSONObject;

/**
 * 認証トークンレスポンスのボディ
 */
final class TokenResponseBody {

    private final String token;

    private TokenResponseBody(final String token) {
        this.token = token;
    }

    String getToken() {
        return this.token;
    }

    /**
     * 読み取る
     * @param raw 認証トークンレスポンスのボディ
     * @return 認証トークンレスポンスのボディ
     */
    static TokenResponseBody parse(final byte[] raw) {
        final JSONObject object = new JSONObject(new String(raw, Constants.UTF8));
        return new TokenResponseBody(object.getJSONObject(Constants.TOKEN_RESPONSE_KEY_DATA).getString(Constants.TOKEN_RESPONSE_KEY_TOKEN));
    }

}
