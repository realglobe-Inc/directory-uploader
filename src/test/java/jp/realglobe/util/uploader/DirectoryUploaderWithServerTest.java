package jp.realglobe.util.uploader;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * モックアップサーバーを使った DirectoryUploader のテスト
 */
public class DirectoryUploaderWithServerTest {

    private static class MemoryStore implements Store {

        private final Map<String, String> store = new HashMap<>();

        @Override
        public String load(final String key) {
            return this.store.get(key);
        }

        @Override
        public void clear(final String key) throws Exception {
            this.store.remove(key);
        }

        @Override
        public void store(final String key, final String value) throws Exception {
            this.store.put(key, value);
        }

    }

    private static class HttpRequest {

        private final String path;
        private final byte[] body;

        HttpRequest(final HttpExchange exchange) throws IOException {
            this.path = exchange.getRequestURI().getPath();
            this.body = Utils.readAll(exchange.getRequestBody());
        }

        public String getPath() {
            return this.path;
        }

        public byte[] getBody() {
            return this.body;
        }

    }

    BlockingQueue<HttpRequest> requestQueue;
    ExecutorService executor;
    String uploaderId;

    Path targetDirectory;
    HttpServer server;

    /**
     * モックアップサーバーを立てる
     * @throws IOException エラー
     * @throws InterruptedException せっかち
     */
    @Before
    public void setUp() throws IOException, InterruptedException {
        this.requestQueue = new LinkedBlockingQueue<>();
        this.executor = Executors.newCachedThreadPool();
        this.uploaderId = String.valueOf(Math.abs(System.nanoTime()));

        this.targetDirectory = Files.createTempDirectory("");
        try {
            this.server = HttpServer.create(new InetSocketAddress(0), 0);
            this.server.createContext("/", this::handle);
            this.server.start();
        } catch (final IOException e) {
            FileUtils.deleteDirectory(this.targetDirectory.toFile());
            throw e;
        }
        // サーバー起動待ち
        Thread.sleep(1_000);
    }

    private void handle(final HttpExchange exchange) throws IOException {
        final String path = exchange.getRequestURI().getPath();

        if (path.equals(Constants.URL_PATH_TOKEN)) {
            final Map<String, Object> tokenData = new HashMap<>();
            tokenData.put(Constants.TOKEN_RESPONSE_KEY_TOKEN, "abcde");
            final Map<String, Object> data = new HashMap<>();
            data.put(Constants.TOKEN_RESPONSE_KEY_DATA, tokenData);
            Utils.readAll(exchange.getRequestBody());
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_CREATED, 0);
            exchange.getResponseBody().write((new JSONObject(data)).toString().getBytes());
        } else if (path.startsWith(Constants.URL_PATH_UPLOAD_PREFIX) && path.substring(Constants.URL_PATH_UPLOAD_PREFIX.length()).endsWith(Constants.URL_PATH_UPLOAD_SUFFIX)) {
            this.requestQueue.offer(new HttpRequest(exchange));
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_CREATED, 0);
        }
        exchange.close();
    }

    /**
     * モックアップサーバーを落とす
     * @throws IOException エラー
     * @throws InterruptedException せっかち
     */
    @After
    public void tearDown() throws IOException, InterruptedException {
        this.executor.shutdownNow();
        try {
            this.executor.awaitTermination(1, TimeUnit.SECONDS);
        } finally {
            this.server.stop(0);
            FileUtils.deleteDirectory(this.targetDirectory.toFile());
        }
    }

    private String getBaseUrl() {
        return "http://localhost:" + this.server.getAddress().getPort();
    }

    /**
     * アップロードテスト
     * @throws Exception エラー
     */
    @Test
    public void testUpload() throws Exception {
        final long delay = 1_000L;
        final DirectoryUploader uploader = new DirectoryUploader(this.targetDirectory, delay, false, null, 0, 0, getBaseUrl(), "user0", "test uploader", new MemoryStore());
        uploader.prepareToken();
        final Future<?> future = this.executor.submit(uploader);
        // 監視開始待ち
        Thread.sleep(1_000L);

        Files.write(this.targetDirectory.resolve("test"), new byte[] { (byte) 0 });
        final HttpRequest request = this.requestQueue.poll(1_000L + delay, TimeUnit.MILLISECONDS);
        try {
            future.get(0, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            // 異常停止してない
        }
        Assert.assertEquals(Constants.URL_PATH_UPLOAD_PREFIX + "/" + uploader.getId() + Constants.URL_PATH_UPLOAD_SUFFIX, request.getPath());
    }

    /**
     * 拡張子制限テスト
     * @throws Exception エラー
     */
    @Test
    public void testExtensionRestriction() throws Exception {
        final long delay = 1_000L;
        final DirectoryUploader uploader = new DirectoryUploader(this.targetDirectory, delay, false, Arrays.asList("jpg"), 0, 0, getBaseUrl(), "user0", "test uploader", new MemoryStore());
        uploader.prepareToken();
        final Future<?> future = this.executor.submit(uploader);
        // 監視開始待ち
        Thread.sleep(1_000L);

        Files.write(this.targetDirectory.resolve("test.png"), new byte[] { (byte) 0 });
        Files.write(this.targetDirectory.resolve("test.jpg"), new byte[] { (byte) 0 });

        final HttpRequest request = this.requestQueue.poll(1_000L + delay, TimeUnit.MILLISECONDS);
        Assert.assertNull(this.requestQueue.poll());
        try {
            future.get(0, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            // 異常停止してない
        }
        Assert.assertEquals(Constants.URL_PATH_UPLOAD_PREFIX + "/" + uploader.getId() + Constants.URL_PATH_UPLOAD_SUFFIX, request.getPath());
    }

    /**
     * 最小サイズ制限テスト
     * @throws Exception エラー
     */
    @Test
    public void testMinimumLimitation() throws Exception {
        final long delay = 1_000L;
        final DirectoryUploader uploader = new DirectoryUploader(this.targetDirectory, delay, false, null, 2, 0, getBaseUrl(), "user0", "test uploader", new MemoryStore());
        uploader.prepareToken();
        final Future<?> future = this.executor.submit(uploader);
        // 監視開始待ち
        Thread.sleep(1_000L);

        Files.write(this.targetDirectory.resolve("test0"), new byte[] { (byte) 0 });
        Files.write(this.targetDirectory.resolve("test1"), new byte[] { (byte) 0, (byte) 1 });

        final HttpRequest request = this.requestQueue.poll(1_000L + delay, TimeUnit.MILLISECONDS);
        Assert.assertNull(this.requestQueue.poll());
        Assert.assertNull(this.requestQueue.poll());
        try {
            future.get(0, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            // 異常停止してない
        }
        Assert.assertEquals(Constants.URL_PATH_UPLOAD_PREFIX + "/" + uploader.getId() + Constants.URL_PATH_UPLOAD_SUFFIX, request.getPath());
    }

    /**
     * 最大サイズ制限テスト
     * @throws Exception エラー
     */
    @Test
    public void testMaximumLimitation() throws Exception {
        final long delay = 1_000L;
        final DirectoryUploader uploader = new DirectoryUploader(this.targetDirectory, delay, false, null, 0, 1, getBaseUrl(), "user0", "test uploader", new MemoryStore());
        uploader.prepareToken();
        final Future<?> future = this.executor.submit(uploader);
        // 監視開始待ち
        Thread.sleep(1_000L);

        Files.write(this.targetDirectory.resolve("test0"), new byte[] { (byte) 0 });
        Files.write(this.targetDirectory.resolve("test1"), new byte[] { (byte) 0, (byte) 1 });

        final HttpRequest request = this.requestQueue.poll(1_000L + delay, TimeUnit.MILLISECONDS);
        Assert.assertNull(this.requestQueue.poll());
        try {
            future.get(0, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            // 異常停止してない
        }
        Assert.assertEquals(Constants.URL_PATH_UPLOAD_PREFIX + "/" + uploader.getId() + Constants.URL_PATH_UPLOAD_SUFFIX, request.getPath());
    }

}
