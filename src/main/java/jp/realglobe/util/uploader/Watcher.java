package jp.realglobe.util.uploader;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import jp.realglobe.lib.util.StackTraces;

/**
 * ディレクトリを監視する
 */
final class Watcher implements Runnable {

    private static final Logger LOG = Logger.getLogger(Watcher.class.getName());

    private final Path target;
    private final Callback callback;

    /**
     * @param target 監視するディレクトリ
     * @param callback 変更されたファイルを受け取る関数
     */
    Watcher(final Path target, final Callback callback) {
        this.target = target;
        this.callback = callback;
    }

    @Override
    public void run() {
        try (final WatchService watcher = FileSystems.getDefault().newWatchService()) {
            // this.target.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
            // 作成されただけの空ファイルは無視
            this.target.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);

            while (true) {
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (final InterruptedException e) {
                    // 終了
                    break;
                }

                final Set<Path> names = new HashSet<>();
                for (final WatchEvent<?> event : key.pollEvents()) {
                    final WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        LOG.info("Event overflowed");
                        continue;
                    }

                    final Object context = event.context();
                    if (!(context instanceof Path)) {
                        LOG.warning("Not path");
                        continue;
                    }

                    names.add((Path) context);
                }

                for (final Path name : names) {
                    final Path path = this.target.resolve(name);
                    LOG.fine("Call callback for " + path);
                    try {
                        this.callback.call(path);
                    } catch (final Exception e) {
                        LOG.warning("Callback failed: " + e);
                        LOG.finest(StackTraces.getString(e));
                    }
                }

                if (!key.reset()) {
                    throw new RuntimeException("Reset error");
                }
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 作成・変更されたファイルのパスを受け取る関数
     */
    static interface Callback {
        /**
         * @param path 作成・変更されたファイルのパス
         * @throws Exception エラー
         */
        void call(Path path) throws Exception;
    }

    public static void main(final String[] args) {
        (new Watcher(Paths.get(args[0]), path -> System.out.println(path))).run();
    }

}
