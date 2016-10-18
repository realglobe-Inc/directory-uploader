package jp.realglobe.util.uploader;

/**
 * 生成・取得した動作用データを保存する
 */
public interface Store {

    /**
     * データを取り出す
     * @param key 取り出すデータのキー
     * @return データ値。キーに対応するデータが無い場合は null
     * @throws Exception エラー
     */
    public String load(String key) throws Exception;

    /**
     * データを消す
     * @param key 消すデータのキー
     * @throws Exception エラー
     */
    public void clear(String key) throws Exception;

    /**
     * データを保存する
     * @param key データのキー
     * @param value 保存するデータ値
     * @throws Exception エラー
     */
    public void store(String key, String value) throws Exception;

}
