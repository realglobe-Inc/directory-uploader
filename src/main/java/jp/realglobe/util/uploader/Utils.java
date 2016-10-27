package jp.realglobe.util.uploader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * ちょっとした便利関数
 */
final class Utils {

    /**
     * 全部読む
     * @param inputStream 入力
     * @return 読んだバイト列
     * @throws IOException 読み込みエラー
     */
    static byte[] readAll(final InputStream inputStream) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final byte[] buff = new byte[DirectoryUploader.BUFFER_SIZE];
        while (true) {
            final int length = inputStream.read(buff);
            if (length < 0) {
                break;
            }
            output.write(buff, 0, length);
        }
        return output.toByteArray();
    }

}
