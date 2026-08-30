package tool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import tool.cmnclslib.mdl.MdlConst;

/**
 * {@link rmtcmd} の単体テストクラスです。
 */
public final class rmtcmdTest {

    /** 単体テスト用の一時ディレクトリパス */
    private static Path tempDir;

    /**
     * テストクラス実行前の初期化処理を行います。
     *
     * @throws IOException 一時ディレクトリ作成に失敗した場合
     */
    @BeforeClass
    public static void setUpBeforeClass() throws IOException {
        tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "UnitTest", "rmtcmd", "rmtcmdTest");
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }
    }

    /**
     * テストクラス実行後のクリーンアップ処理を行います。
     *
     * @throws IOException ファイル削除処理時に予期せぬエラーが発生した場合
     */
    @AfterClass
    public static void tearDownAfterClass() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            try (Stream<Path> stream = Files.walk(tempDir)) {
                stream.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (final IOException e) {
                                // ignore
                            }
                        });
            }
        }
    }

    /**
     * 一時ディレクトリ規約のパス検証テストです。
     */
    @Test
    public void testTempDirectoryRule() {
        assertNotNull("一時ディレクトリが正しく作成されていること", tempDir);
        assertTrue("一時ディレクトリが存在すること", Files.exists(tempDir));
    }

    /**
     * ヘルプオプション指定時の Usage 表示および終了コードを検証するテストです。
     */
    @Test
    public void testHelpOptionReturnsWarnLevel() {
        final int exitCode = rmtcmd.run(new String[]{"--help"});
        assertEquals(MdlConst.LVL_W, exitCode);

        final int exitCodeAlias = rmtcmd.run(new String[]{"-?"});
        assertEquals(MdlConst.LVL_W, exitCodeAlias);
    }

    /**
     * ヘルプオプションとカスタム警告コード指定時の終了コードを検証するテストです。
     */
    @Test
    public void testHelpOptionWithCustomWarnCode() {
        final int exitCode = rmtcmd.run(new String[]{"--help", "-warn", "15"});
        assertEquals(15, exitCode);
    }

    /**
     * パスワード未指定など引数不正時のエラーコードを検証するテストです。
     */
    @Test
    public void testInvalidArgumentsReturnsErrorLevel() {
        // -p が指定されていないため isOk == false となり LVL_E (20) が返る
        final int exitCode = rmtcmd.run(new String[]{"-h", "localhost", "-u", "user1"});
        assertEquals(MdlConst.LVL_E, exitCode);
    }

    /**
     * カスタムエラーコード指定かつ引数不正時の終了コードを検証するテストです。
     */
    @Test
    public void testInvalidArgumentsWithCustomErrorCode() {
        final int exitCode = rmtcmd.run(new String[]{"-h", "localhost", "-u", "user1", "-err", "25"});
        assertEquals(25, exitCode);
    }

    /**
     * 詳細ログフラグおよび終了コードエコーフラグ指定時のテストです。
     */
    @Test
    public void testVerboseAndEchoRetcodeOptions() {
        final int exitCode = rmtcmd.run(new String[]{"--help", "-v", "3", "-echo-retcd"});
        assertEquals(MdlConst.LVL_W, exitCode);
    }

    /**
     * コマンドライン引数の Verbose 指定に応じた SLF4J ログレベル設定の連動を検証するテストです。
     */
    @Test
    public void testVerboseLevelConfiguresLogging() {
        // Verbose < 4 (-vv 3) -> warn
        rmtcmd.run(new String[]{"--help", "-vv", "3"});
        assertEquals("warn", System.getProperty("org.slf4j.simpleLogger.defaultLogLevel"));
        assertEquals("warn", System.getProperty("org.slf4j.simpleLogger.log.org.apache.cxf"));

        // Verbose >= 4 (--vv 4) -> info
        rmtcmd.run(new String[]{"--help", "--vv", "4"});
        assertEquals("info", System.getProperty("org.slf4j.simpleLogger.defaultLogLevel"));
        assertEquals("info", System.getProperty("org.slf4j.simpleLogger.log.org.apache.cxf"));

        // Verbose >= 9 (-vv 9) -> debug
        rmtcmd.run(new String[]{"--help", "-vv", "9"});
        assertEquals("debug", System.getProperty("org.slf4j.simpleLogger.defaultLogLevel"));
        assertEquals("debug", System.getProperty("org.slf4j.simpleLogger.log.org.apache.cxf"));

        // 後始末
        ClsWinRs.configureLogging(0);
    }
}
