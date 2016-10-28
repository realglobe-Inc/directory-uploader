package jp.realglobe.util.uploader;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
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

    static final int BUFFER_SIZE = 4096;

    // 監視するディレクトリ
    private final Path watchDirectoryPath;
    // 検知の猶予期間
    private final long delay;
    // アップロード対象の拡張子
    private final Set<String> targetExtensions;
    // アップロード対象の最小ファイルサイズ
    private final long minSize;
    // アップロード対象の最大ファイルサイズ
    private final long maxSize;
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
     * @param delay ファイルの変更からアップロードまでの猶予期間（ミリ秒）。
     *            細かい更新に対して毎回アップロードを行わないため
     * @param targetExtensions アップロード対象の拡張子
     * @param minSize アップロード対象の最小ファイルサイズ（バイト）
     * @param maxSize アップロード対象の最大ファイルサイズ（バイト）
     * @param urlBase アップロード先サーバーの URL
     * @param userId 紐付くユーザーの ID
     * @param name 表示名
     * @param store 運用データの保管庫
     * @throws Exception データ読み書きエラー
     */
    public DirectoryUploader(final Path watchDirectoryPath, final long delay, final Collection<String> targetExtensions, final long minSize, final long maxSize,
            final String urlBase, final String userId, final String name, final Store store) throws Exception {
        this.watchDirectoryPath = watchDirectoryPath;
        this.delay = delay;
        this.targetExtensions = (targetExtensions == null ? Collections.emptySet() : new HashSet<>(targetExtensions));
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.userId = userId;
        this.id = getId(store);
        this.name = name;
        this.tokenUrl = (new URL(urlBase + Constants.URL_PATH_TOKEN)).toURI();
        this.uploadUrl = (new URL(urlBase + Constants.URL_PATH_UPLOAD_PREFIX + "/" + this.id + Constants.URL_PATH_UPLOAD_SUFFIX)).toURI();
        this.store = store;

        LOG.info("ID is " + this.id);
    }

    /**
     * 作成する
     * @param watchDirectoryPath 監視するディレクトリのパス
     * @param targetExtensions アップロード対象の拡張子
     * @param minSize アップロード対象の最小ファイルサイズ（バイト）
     * @param maxSize アップロード対象の最大ファイルサイズ（バイト）
     * @param urlBase アップロード先サーバーの URL
     * @param userId 紐付くユーザーの ID
     * @param name 表示名
     * @param store 運用データの保管庫
     * @throws Exception データ読み書きエラー
     */
    public DirectoryUploader(final Path watchDirectoryPath, final Collection<String> targetExtensions, final long minSize, final long maxSize,
            final String urlBase, final String userId, final String name, final Store store) throws Exception {
        this(watchDirectoryPath, 1_000L, targetExtensions, minSize, maxSize, urlBase, userId, name, store);
    }

    /**
     * アップロード対象のファイルに制限を付けずに作成する
     * @param watchDirectoryPath 監視するディレクトリのパス
     * @param urlBase アップロード先サーバーの URL
     * @param userId 紐付くユーザーの ID
     * @param name 表示名
     * @param store 運用データの保管庫
     * @throws Exception データ読み書きエラー
     */
    public DirectoryUploader(final Path watchDirectoryPath, final String urlBase, final String userId, final String name, final Store store) throws Exception {
        this(watchDirectoryPath, Collections.emptyList(), 0, 0, urlBase, userId, name, store);
    }

    String getId() {
        return this.id;
    }

    /**
     * ID を返す
     * @param store 運用データの保管庫
     * @return ID
     * @throws Exception データ読み書きエラー
     */
    private static String getId(final Store store) throws Exception {
        final String loadedId = store.load(Constants.STORE_KEY_ID);
        if (loadedId != null) {
            return loadedId;
        }
        final String newId = UUID.randomUUID().toString();
        store.store(Constants.STORE_KEY_ID, newId);
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

        (new DelayedWatcher(this.watchDirectoryPath, this.delay, path -> {
            if (!Files.isReadable(path)) {
                LOG.info("Cannot read " + path);
                return;
            }
            final File file = path.toFile();
            if (!this.targetExtensions.isEmpty() && !this.targetExtensions.contains(FilenameUtils.getExtension(file.getName()))) {
                LOG.info("Skip non target file " + path);
                return;
            } else if (this.minSize > 0 && file.length() < this.minSize) {
                LOG.info("Skip too small file " + path);
                return;
            } else if (this.maxSize > 0 && file.length() > this.maxSize) {
                LOG.info("Skip too large file " + path);
                return;
            }
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
        final String localToken = this.store.load(Constants.STORE_KEY_TOKEN);
        if (localToken != null) {
            if (isValidToken(localToken)) {
                return localToken;
            }
            this.store.clear(Constants.STORE_KEY_TOKEN);
        }
        final String token = getRemoteToken();
        if (token == null) {
            throw new RuntimeException("Cannot get token");
        }
        this.store.store(Constants.STORE_KEY_TOKEN, token);
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
                    return TokenResponseBody.parse(Utils.readAll(response.getEntity().getContent())).getToken();
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
                    .addTextBody(Constants.UPLOAD_REQUEST_PART_TOKEN, token)
                    .addBinaryBody(Constants.UPLOAD_REQUEST_PART_DATA, path.toFile())
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
     * 認証トークンを取得する
     * @throws Exception データ読み書きエラー
     */
    public void prepareToken() throws Exception {
        getToken();
    }

}
