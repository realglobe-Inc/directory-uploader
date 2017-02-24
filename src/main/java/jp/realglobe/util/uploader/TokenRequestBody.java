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

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

/**
 * 認証トークンの取得リクエストボディ
 */
final class TokenRequestBody {

    private static final String KEY_USER_ID = "owner";
    private static final String KEY_ID = "uuid";
    private static final String KEY_NAME = "name";

    private final String userId;
    private final String id;
    private final String name;

    TokenRequestBody(final String userId, final String id, final String name) {
        super();
        this.userId = userId;
        this.id = id;
        this.name = name;
    }

    String getUserId() {
        return this.userId;
    }

    String getId() {
        return this.id;
    }

    String getName() {
        return this.name;
    }

    String toJson() {
        final Map<String, Object> data = new HashMap<>();
        data.put(KEY_USER_ID, this.userId);
        data.put(KEY_ID, this.id);
        data.put(KEY_NAME, this.name);
        return (new JSONObject(data)).toString();
    }

}
