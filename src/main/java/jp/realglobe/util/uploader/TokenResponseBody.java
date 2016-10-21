package jp.realglobe.util.uploader;

import org.json.JSONObject;

/**
 * 認証トークンレスポンスのボディ
 */
final class TokenResponseBody {

    private static final String KEY_DATA = "created";
    private static final String KEY_TOKEN = "token";

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
        return new TokenResponseBody(object.getJSONObject(KEY_DATA).getString(KEY_TOKEN));
    }

}
