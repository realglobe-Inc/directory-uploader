package jp.realglobe.util.uploader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * テスト
 */
public class DelayedWatcherTest {

    private Path directory;
    private ExecutorService executor;
    private BlockingQueue<Path> detected;

    /**
     * @throws Exception エラー
     */
    @Before
    public void setUp() throws Exception {
        this.directory = Files.createTempDirectory(DelayedWatcherTest.class.getSimpleName());
        this.executor = Executors.newCachedThreadPool();
        this.detected = new LinkedBlockingQueue<>();
    }

    /**
     * @throws Exception エラー
     */
    @After
    public void tearDown() throws Exception {
        this.executor.shutdownNow();
        FileUtils.deleteDirectory(this.directory.toFile());
    }

    /**
     * ファイル作成を検知できるか
     * @throws Exception エラー
     */
    @Test
    public void testCreateEmpty() throws Exception {
        final long delay = 1_000L;
        final DelayedWatcher watcher = new DelayedWatcher(this.directory, delay, false, path -> {
            this.detected.offer(path);
        });
        this.executor.submit(watcher);
        Thread.sleep(1_000L);

        final int n = 10;
        for (int i = 0; i < n; i++) {
            Files.createFile(this.directory.resolve("" + i));
        }
        for (int i = 0; i < n; i++) {
            Assert.assertEquals(this.directory.resolve("" + i), this.detected.poll(1_000L + delay, TimeUnit.MILLISECONDS));
        }
        Assert.assertNull(this.detected.poll(1_000L, TimeUnit.MILLISECONDS));
    }

    /**
     * ファイルの作成と書き込みを検知できるか
     * @throws Exception エラー
     */
    @Test
    public void testCreate() throws Exception {
        final long delay = 1_000L;
        final DelayedWatcher watcher = new DelayedWatcher(this.directory, delay, false, path -> {
            this.detected.offer(path);
        });
        this.executor.submit(watcher);
        Thread.sleep(1_000L);

        final int n = 10;
        for (int i = 0; i < n; i++) {
            Files.write(this.directory.resolve("" + i), "abcdefg".getBytes());
        }
        for (int i = 0; i < n; i++) {
            Assert.assertEquals(this.directory.resolve("" + i), this.detected.poll(1_000L + delay, TimeUnit.MILLISECONDS));
        }
        Assert.assertNull(this.detected.poll(1_000L, TimeUnit.MILLISECONDS));
    }

    /**
     * ファイルの移動を検知できるか
     * @throws Exception エラー
     */
    @Test
    public void testMove() throws Exception {
        final long delay = 1_000L;
        final DelayedWatcher watcher = new DelayedWatcher(this.directory, delay, false, path -> {
            this.detected.offer(path);
        });
        this.executor.submit(watcher);
        Thread.sleep(1_000L);

        final int n = 10;
        for (int i = 0; i < n; i++) {
            Files.write(this.directory.resolve("" + i), "abcdefg".getBytes());
        }
        for (int i = 0; i < n; i++) {
            Assert.assertEquals(this.directory.resolve("" + i), this.detected.poll(1_000L + delay, TimeUnit.MILLISECONDS));
        }

        for (int i = 0; i < n; i++) {
            Files.move(this.directory.resolve("" + i), this.directory.resolve("m" + i));
        }
        for (int i = 0; i < n; i++) {
            Assert.assertEquals(this.directory.resolve("m" + i), this.detected.poll(1_000L + delay, TimeUnit.MILLISECONDS));
        }

        Assert.assertNull(this.detected.poll(1_000L, TimeUnit.MILLISECONDS));
    }

    /**
     * 処理落ちしたらイベントをスキップできるか
     * @throws Exception エラー
     */
    @Test
    public void testLatestOnly() throws Exception {
        final long delay = 1_000L;
        final CountDownLatch stopper = new CountDownLatch(1);
        final DelayedWatcher watcher = new DelayedWatcher(this.directory, delay, true, path -> {
            stopper.await();
            this.detected.offer(path);
        });
        this.executor.submit(watcher);
        Thread.sleep(1_000L);

        Files.createFile(this.directory.resolve("0"));
        Thread.sleep(1_000L + delay);
        // 0 に対する関数実行中

        final int n = 10;
        for (int i = 1; i < n; i++) {
            Files.createFile(this.directory.resolve("" + i));
        }

        Thread.sleep(1_000L);
        // 1,...,9 の検知イベントが溜まる

        stopper.countDown();
        // 0 に対する関数実行が終わる

        Assert.assertEquals(this.directory.resolve("0"), this.detected.poll(1_000L, TimeUnit.MILLISECONDS));
        Assert.assertEquals(this.directory.resolve("9"), this.detected.poll(1_000L + delay, TimeUnit.MILLISECONDS));
        Assert.assertNull(this.detected.poll(1_000L, TimeUnit.MILLISECONDS));
    }

}
