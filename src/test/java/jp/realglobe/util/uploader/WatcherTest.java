/**
 *
 */
package jp.realglobe.util.uploader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * テスト
 */
public class WatcherTest {

    /**
     * ファイル作成を検知できるか
     * @throws Exception エラー
     */
    @Test
    public void testDetectFileCreation() throws Exception {
        final int n = 10;
        final BlockingQueue<String> queue = new SynchronousQueue<>();

        final Path directoryPath = Files.createTempDirectory("");
        try {
            final Watcher watcher = new Watcher(directoryPath, path -> {
                queue.put(path.toString());
            });

            final ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(watcher);
            Thread.sleep(1_000);
            try {
                for (int i = 0; i < n; i++) {
                    Files.write(directoryPath.resolve("" + i), new byte[] { (byte) i });
                    Assert.assertEquals(directoryPath.resolve("" + i).toString(), queue.poll(1, TimeUnit.SECONDS));
                }
            } finally {
                executor.shutdownNow();
            }

        } finally {
            FileUtils.deleteDirectory(directoryPath.toFile());
        }
    }

}
