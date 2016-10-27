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
