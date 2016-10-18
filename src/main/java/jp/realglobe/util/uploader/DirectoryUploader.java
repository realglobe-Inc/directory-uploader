package jp.realglobe.util.uploader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.http.HttpException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * ディレクトリの中身をアップロードする
 */
public class DirectoryUploader implements Runnable {

    private static final Logger LOG = Logger.getLogger(DirectoryUploader.class.getName());

    private static final int BUFFER_SIZE = 4096;

    private static final String PART_KEY_TOKEN = "token";
    private static final String PART_KEY_DATA = "image";

    private static final String URL_PATH_TOKEN = "/cameras";
    private static final String URL_PATH_UPLOAD_PREFIX = "/cameras";
    private static final String URL_PATH_UPLOAD_SUFFIX = "/photos";

    private static final String DATA_KEY_ID = "id";
    private static final String DATA_KEY_TOKEN = "token";

    // 投稿するデータが置かれるディレクトリ
    private final Path watchDirectoryPath;
    // 紐付くユーザーの ID
    private final String userId;
    // 自身の ID
    private final String id;
    // 自身の表示名
    private final String name;
    // 認証トークンを取得するための URL
    private final URI tokenUrl;
    // データを投稿するための URL
    private final URI uploadUrl;
    // 運用データの保管庫
    private final Store store;

    /**
     * 作成する
     * @param watchDirectoryPath 監視するディレクトリのパス
     * @param urlBase アップロード先サーバーの URL
     * @param userId 紐付くユーザーの ID
     * @param name 表示名
     * @param store 運用データの保管庫
     * @throws Exception データ読み書きエラー
     */
    public DirectoryUploader(final Path watchDirectoryPath, final String urlBase, final String userId, final String name, final Store store) throws Exception {
        this.watchDirectoryPath = watchDirectoryPath;
        this.userId = userId;
        this.id = getId(store);
        this.name = name;
        this.tokenUrl = (new URL(urlBase + URL_PATH_TOKEN)).toURI();
        this.uploadUrl = (new URL(urlBase + URL_PATH_UPLOAD_PREFIX + "/" + this.id + URL_PATH_UPLOAD_SUFFIX)).toURI();
        this.store = store;

        LOG.info("ID is " + this.id);
    }

    /**
     * ID を返す
     * @param store 運用データの保管庫
     * @return ID
     * @throws Exception データ読み書きエラー
     */
    private static String getId(final Store store) throws Exception {
        final String loadedId = store.load(DATA_KEY_ID);
        if (loadedId != null) {
            return loadedId;
        }
        final String newId = UUID.randomUUID().toString();
        store.store(DATA_KEY_ID, newId);
        return newId;
    }

    @Override
    public void run() {
        final String token;
        try {
            token = getToken();
        } catch (final InterruptedException e) {
            // 終了
            return;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        LOG.info("Use token " + token);

        (new Watcher(this.watchDirectoryPath, path -> {
            upload(token, path);
        })).run();
    }

    /**
     * 認証トークンを返す
     * 認証トークンのキャッシュ管理も行う
     * @return 認証トークン
     * @throws Exception データ読み書きエラー
     */
    private String getToken() throws Exception {
        final String localToken = this.store.load(DATA_KEY_TOKEN);
        if (localToken != null) {
            if (isValidToken(localToken)) {
                return localToken;
            }
            this.store.clear(DATA_KEY_TOKEN);
        }
        final String token = getRemoteToken();
        if (token == null) {
            throw new RuntimeException("Cannot get token");
        }
        this.store.store(DATA_KEY_TOKEN, token);
        return token;
    }

    /**
     * 有効な認証トークンかどうか調べる
     * @param token 調べる認証トークン
     * @return 有効なら true
     */
    private boolean isValidToken(final String token) {
        // TODO 実際にサーバーにつなげて検査
        LOG.info("Token check is not implemented");
        return true;
    }

    /**
     * サーバーからトークンを取得する
     * @return トークン
     * @throws IOException ネットワークエラー
     */
    private String getRemoteToken() throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {

            final HttpPost post = new HttpPost(this.tokenUrl);
            post.setEntity(new StringEntity((new TokenRequestBody(this.userId, this.id, this.name)).toJson(), ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = client.execute(post)) {
                if (response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_CREATED) {
                    return TokenResponseBody.parse(readAll(response.getEntity().getContent())).getToken();
                }

                LOG.warning(response.toString());
                LOG.info("Retrying request is not implemented");
                return null;
            }
        }
    }

    /**
     * ファイルをアップロードする
     * @param token 認証トークン
     * @param path アップロードするファイルのパス
     * @throws IOException ファイルが無かったり、通信異常だったり
     * @throws HttpException HTTP エラー
     */
    private void upload(final String token, final Path path) throws HttpException, IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {

            final HttpPost post = new HttpPost(this.uploadUrl);
            post.setEntity(MultipartEntityBuilder.create()
                    .addTextBody("info", "{}")
                    .addTextBody(PART_KEY_TOKEN, token)
                    .addBinaryBody(PART_KEY_DATA, path.toFile())
                    .build());

            LOG.info("Upload " + path);

            try (CloseableHttpResponse response = client.execute(post)) {
                if (response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_CREATED) {
                    return;
                }

                LOG.warning(response.toString());
            }
        }
    }

    /**
     * 全部読む
     * @param inputStream 入力
     * @return 読んだバイト列
     * @throws IOException 読み込みエラー
     */
    private byte[] readAll(final InputStream inputStream) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final byte[] buff = new byte[BUFFER_SIZE];
        while (true) {
            final int length = inputStream.read(buff);
            if (length < 0) {
                break;
            }
            output.write(buff, 0, length);
        }
        return output.toByteArray();
    }

    /**
     * 認証トークンを取得する
     * @throws Exception データ読み書きエラー
     */
    public void prepareToken() throws Exception {
        getToken();
    }

}
