package jp.realglobe.util.uploader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
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
        final DelayedWatcher watcher = new DelayedWatcher(this.directory, delay, path -> {
            this.detected.offer(path);
        });
        this.executor.submit(watcher);
        Thread.sleep(1_000);

        final int n = 10;
        for (int i = 0; i < n; i++) {
            Files.createFile(this.directory.resolve("" + i));
        }
        for (int i = 0; i < n; i++) {
            Assert.assertEquals(this.directory.resolve("" + i), this.detected.poll(delay + 1_000, TimeUnit.MILLISECONDS));
        }
        Assert.assertNull(this.detected.poll());
    }

    /**
     * ファイルの作成と書き込みを検知できるか
     * @throws Exception エラー
     */
    @Test
    public void testCreate() throws Exception {
        final long delay = 1_000L;
        final DelayedWatcher watcher = new DelayedWatcher(this.directory, delay, path -> {
            this.detected.offer(path);
        });
        this.executor.submit(watcher);
        Thread.sleep(1_000);

        final int n = 10;
        for (int i = 0; i < n; i++) {
            Files.write(this.directory.resolve("" + i), "abcdefg".getBytes());
        }
        for (int i = 0; i < n; i++) {
            Assert.assertEquals(this.directory.resolve("" + i), this.detected.poll(delay + 1_000, TimeUnit.MILLISECONDS));
        }
        Assert.assertNull(this.detected.poll());
    }

    /**
     * ファイルの移動を検知できるか
     * @throws Exception エラー
     */
    @Test
    public void testMove() throws Exception {
        final long delay = 1_000L;
        final DelayedWatcher watcher = new DelayedWatcher(this.directory, delay, path -> {
            this.detected.offer(path);
        });
        this.executor.submit(watcher);
        Thread.sleep(1_000);

        final int n = 10;
        for (int i = 0; i < n; i++) {
            Files.write(this.directory.resolve("" + i), "abcdefg".getBytes());
        }
        for (int i = 0; i < n; i++) {
            Assert.assertEquals(this.directory.resolve("" + i), this.detected.poll(delay + 1_000, TimeUnit.MILLISECONDS));
        }

        for (int i = 0; i < n; i++) {
            Files.move(this.directory.resolve("" + i), this.directory.resolve("m" + i));
        }
        for (int i = 0; i < n; i++) {
            Assert.assertEquals(this.directory.resolve("m" + i), this.detected.poll(delay + 1_000, TimeUnit.MILLISECONDS));
        }

        Assert.assertNull(this.detected.poll());
    }

}
