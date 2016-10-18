package jp.realglobe.util.uploader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ファイルに保存
 */
public class FileStore implements Store {

    private final Path directoryPath;

    /**
     * 作成する
     * @param directoryPath ファイルを保存するディレクトリのパス
     */
    public FileStore(final Path directoryPath) {
        this.directoryPath = directoryPath;
    }

    /**
     * データキーからファイルパスを生成する
     * @param key データキー
     * @return ファイルパス
     */
    private Path makePath(final String key) {
        return this.directoryPath.resolve(key);
    }

    @Override
    public String load(final String key) throws IOException {
        final Path path = makePath(key);
        if (!Files.isReadable(path)) {
            return null;
        }
        return new String(Files.readAllBytes(path), Constants.UTF8);
    }

    @Override
    public void clear(final String key) throws IOException {
        final Path path = makePath(key);
        if (!Files.exists(path)) {
            return;
        }
        Files.delete(path);
    }

    @Override
    public void store(final String key, final String value) throws IOException {
        final Path path = makePath(key);
        try {
            Files.write(path, value.getBytes(Constants.UTF8));
        } catch (final IOException e) {
            // ディレクトリが無かったのかもしれない
            final Path parent = path.getParent();
            if (parent == null) {
                throw e;
            } else if (Files.exists(parent)) {
                throw e;
            }
            Files.createDirectories(parent);
            Files.write(path, value.getBytes(Constants.UTF8));
        }
    }

}
