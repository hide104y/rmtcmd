package tool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import tool.cmnclslib.cls.ClsLogger;
import tool.cmnclslib.mdl.MdlConst;

/**
 * {@link ClsWinRs} の単体テストクラスです。
 */
public class ClsWinRsTest {

    private static Path tempDir;

    private ClsLogger logger;
    private ClsWinRs winRs;

    /**
     * テストクラス実行前の初期化処理を行います。規約に基づき専用の一時ディレクトリを作成します。
     *
     * @throws IOException 一時ディレクトリ作成に失敗した場合
     */
    @BeforeClass
    public static void setUpBeforeClass() throws IOException {
        tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "UnitTest", "rmtcmd", "ClsWinRsTest");
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }
    }

    /**
     * テストクラス実行後のクリーンアップ処理を行います。作成した一時ディレクトリを削除します。
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

    @Before
    public void setUp() {
        logger = new ClsLogger();
        winRs = new ClsWinRs(logger);
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
     * デフォルト値の検証テストです。
     */
    @Test
    public void testDefaultValues() {
        assertNotNull(winRs.getRemoteHost());
        assertEquals("localhost", winRs.getRemoteHost());
        assertEquals(5985, winRs.getPort());
        assertFalse(winRs.isUseHttps());
        assertEquals("", winRs.getDomain());
        assertEquals("", winRs.getUsername());
        assertEquals("", winRs.getPassword());
        assertEquals("", winRs.getWorkDir());
        assertEquals("", winRs.getTeePath());
        assertEquals("", winRs.getAddEnvPath());
        assertNotNull(winRs.getProcEnvs());
        assertTrue(winRs.getProcEnvs().isEmpty());
        assertEquals(ClsWinRs.EXEC_MODE_CMD, winRs.getExecMode());
        assertEquals(0, winRs.getVerbose());
        assertEquals(0, winRs.getAuthMechanism());
        assertEquals(180, winRs.getOpTimeout());
        assertEquals(120, winRs.getOpenTimeout());
        assertFalse(winRs.isLogWrite());
        assertFalse(winRs.isStackTrace());
        assertNotNull(winRs.getOutputBuffer());
        assertEquals(0, winRs.getRetryMax());
        assertEquals(5, winRs.getRetrySleep());
        assertFalse(winRs.isRetryRmtCmd());
        assertEquals(0, winRs.getCmdExitCode());
    }

    /**
     * Getter / Setter の動作検証テストです。
     */
    @Test
    public void testGettersAndSetters() {
        winRs.setRemoteHost("192.168.1.50");
        assertEquals("192.168.1.50", winRs.getRemoteHost());

        winRs.setPort(5986);
        assertEquals(5986, winRs.getPort());

        winRs.setUseHttps(true);
        assertTrue(winRs.isUseHttps());

        winRs.setUseHttps(false);
        assertFalse(winRs.isUseHttps());

        winRs.setDomain("WORKGROUP");
        assertEquals("WORKGROUP", winRs.getDomain());

        winRs.setDomain("MYDOMAIN");
        assertEquals("MYDOMAIN", winRs.getDomain());

        winRs.setUsername("adminUser");
        assertEquals("adminUser", winRs.getUsername());

        winRs.setPassword("secretPass");
        assertEquals("secretPass", winRs.getPassword());

        winRs.setComSpec("cmd.exe");
        assertEquals("cmd.exe", winRs.getComSpec());

        winRs.setWorkDir("C:\\work");
        assertEquals("C:\\work", winRs.getWorkDir());

        winRs.setTeePath("C:\\temp\\log.txt");
        assertEquals("C:\\temp\\log.txt", winRs.getTeePath());

        winRs.setAddEnvPath("C:\\tools\\bin");
        assertEquals("C:\\tools\\bin", winRs.getAddEnvPath());

        final Map<String, String> envMap = new LinkedHashMap<>();
        envMap.put("ENV1", "VAL1");
        winRs.setProcEnvs(envMap);
        assertEquals(1, winRs.getProcEnvs().size());
        assertEquals("VAL1", winRs.getProcEnvs().get("ENV1"));

        winRs.setExecMode(ClsWinRs.EXEC_MODE_PS);
        assertEquals(ClsWinRs.EXEC_MODE_PS, winRs.getExecMode());

        winRs.setVerbose(3);
        assertEquals(3, winRs.getVerbose());

        winRs.setAuthMechanism(1);
        assertEquals(1, winRs.getAuthMechanism());

        winRs.setOpTimeout(300);
        assertEquals(300, winRs.getOpTimeout());

        winRs.setOpenTimeout(60);
        assertEquals(60, winRs.getOpenTimeout());

        winRs.setLogWrite(true);
        assertTrue(winRs.isLogWrite());

        winRs.setLogWrite(false);
        assertFalse(winRs.isLogWrite());

        winRs.setStackTrace(true);
        assertTrue(winRs.isStackTrace());

        winRs.setStackTrace(false);
        assertFalse(winRs.isStackTrace());

        final StringBuilder sb = new StringBuilder("test output");
        winRs.setOutputBuffer(sb);
        assertEquals(sb, winRs.getOutputBuffer());

        winRs.setRetryMax(3);
        assertEquals(3, winRs.getRetryMax());

        winRs.setRetrySleep(10);
        assertEquals(10, winRs.getRetrySleep());

        winRs.setRetryRmtCmd(true);
        assertTrue(winRs.isRetryRmtCmd());

        winRs.setRetryRmtCmd(false);
        assertFalse(winRs.isRetryRmtCmd());

        winRs.setCmdExitCode(123);
        assertEquals(123, winRs.getCmdExitCode());
    }

    /**
     * CmdStatus 連携の Getter / Setter の動作検証テストです。
     */
    @Test
    public void testCmdStatusProperties() {
        winRs.setOkRetCsv("0,1");
        assertEquals("0,1", winRs.getOkRetCsv());

        winRs.setWarnRetCsv("2");
        assertEquals("2", winRs.getWarnRetCsv());

        winRs.setErrRetCsv("3,4");
        assertEquals("3,4", winRs.getErrRetCsv());

        winRs.setOkMsgCsv("SUCCESS");
        assertEquals("SUCCESS", winRs.getOkMsgCsv());

        winRs.setWarnMsgCsv("WARNING");
        assertEquals("WARNING", winRs.getWarnMsgCsv());

        winRs.setErrMsgCsv("ERROR");
        assertEquals("ERROR", winRs.getErrMsgCsv());

        winRs.setWarnThreshold(10);
        assertEquals(10, winRs.getWarnThreshold());

        winRs.setErrThreshold(20);
        assertEquals(20, winRs.getErrThreshold());

        winRs.setErrAtNegative(true);
        assertTrue(winRs.isErrAtNegative());

        winRs.setErrAtNegative(false);
        assertFalse(winRs.isErrAtNegative());

        winRs.setAlwaysNormal(true);
        assertTrue(winRs.isAlwaysNormal());

        winRs.setAlwaysNormal(false);
        assertFalse(winRs.isAlwaysNormal());

        winRs.setErrorCode(99);
        assertEquals(99, winRs.getErrorCode());

        winRs.setWarnCode(88);
        assertEquals(88, winRs.getWarnCode());

        winRs.setMethodExit(10);
        assertEquals(10, winRs.getMethodExit());

        winRs.setReturnLevel(MdlConst.LVL_W);
        assertEquals(MdlConst.LVL_W, winRs.getReturnLevel());
    }

    /**
     * 初期化メソッドの動作確認テストです。
     */
    @Test
    public void testInitialize() {
        winRs.setVerbose(2);
        winRs.initialize();
    }

    /**
     * 各実行モードでのコマンド生成（getInvokeCmd）を検証するテストです。
     */
    @Test
    public void testGetInvokeCommandModes() {
        winRs.setComSpec("cmd.exe");

        // EXEC_MODE_NORMAL (0)
        winRs.setExecMode(ClsWinRs.EXEC_MODE_NORMAL);
        assertEquals("dir C:\\ 2>&1", winRs.getInvokeCmd("dir C:\\"));

        // EXEC_MODE_CMD (1)
        winRs.setExecMode(ClsWinRs.EXEC_MODE_CMD);
        assertEquals("cmd.exe /c dir C:\\ 2>&1", winRs.getInvokeCmd("dir C:\\"));

        // EXEC_MODE_EXE (6)
        winRs.setExecMode(ClsWinRs.EXEC_MODE_EXE);
        assertEquals("dir C:\\", winRs.getInvokeCmd("dir C:\\"));

        // EXEC_MODE_EC (4): Base64 encoded (MS932)
        winRs.setExecMode(ClsWinRs.EXEC_MODE_EC);
        final String originalCmd = "echo 日本語";
        final String base64Ms932 = Base64.getEncoder().encodeToString(originalCmd.getBytes(Charset.forName("MS932")));
        assertEquals(originalCmd, winRs.getInvokeCmd(base64Ms932));

        // EXEC_MODE_ECMD (5): cmd.exe /c <decoded> 2>&1
        winRs.setExecMode(ClsWinRs.EXEC_MODE_ECMD);
        assertEquals("cmd.exe /c " + originalCmd + " 2>&1", winRs.getInvokeCmd(base64Ms932));

        // EXEC_MODE_PS (2): cmd.exe /c powershell -encodedCommand "<Base64(UTF-16LE)>" 2>&1
        winRs.setExecMode(ClsWinRs.EXEC_MODE_PS);
        final String psCmd = "Get-Process";
        final String psBase64 = Base64.getEncoder().encodeToString(psCmd.getBytes(StandardCharsets.UTF_16LE));
        assertEquals("cmd.exe /c powershell -encodedCommand \"" + psBase64 + "\" 2>&1", winRs.getInvokeCmd(psCmd));

        // EXEC_MODE_ES (3): cmd.exe /c powershell -encodedCommand "<encoded_raw>" 2>&1
        winRs.setExecMode(ClsWinRs.EXEC_MODE_ES);
        assertEquals("cmd.exe /c powershell -encodedCommand \"" + psBase64 + "\" 2>&1", winRs.getInvokeCmd(psBase64));
    }

    /**
     * TeePath を設定した際のコマンド生成を検証するテストです。
     */
    @Test
    public void testGetInvokeCommandWithTeePath() {
        winRs.setComSpec("cmd.exe");
        winRs.setExecMode(ClsWinRs.EXEC_MODE_CMD);
        winRs.setTeePath("C:\\temp\\output.log");
        assertEquals("cmd.exe /c hostname 2>&1 | Tee-Object -FilePath C:\\temp\\output.log", winRs.getInvokeCmd("hostname"));
    }

    /**
     * リトライ対象エラーメッセージの判定テストです。
     */
    @Test
    public void testIsRetryableError() {
        final String retryableMsgJa = "リモート サーバー SERVER01 への接続に失敗し、次のエラー メッセージが返されました: "
                + "削除の対象としてマークされているレジストリ キーに対して無効な操作を実行しようとしました。";
        assertTrue("日本語リトライ対象のエラーメッセージに合致すること", winRs.isRetryError(retryableMsgJa));

        final String retryableMsgEn1 = "The operation attempted on a registry key that has been marked for deletion.";
        assertTrue("英語リトライ対象のエラーメッセージ（パターン1）に合致すること", winRs.isRetryError(retryableMsgEn1));

        final String retryableMsgEn2 = "Failed to connect: key is marked for deletion.";
        assertTrue("英語リトライ対象のエラーメッセージ（パターン2）に合致すること", winRs.isRetryError(retryableMsgEn2));

        final String nonRetryableMsg = "アクセスが拒否されました。";
        assertFalse("通常エラーはリトライ対象外であること", winRs.isRetryError(nonRetryableMsg));

        final String nonRetryableMsgEn = "Access is denied.";
        assertFalse("英語通常エラーはリトライ対象外であること", winRs.isRetryError(nonRetryableMsgEn));

        assertFalse("null はリトライ対象外であること", winRs.isRetryError(null));
        assertFalse("空文字列はリトライ対象外であること", winRs.isRetryError(""));
    }

    /**
     * バッファクリアのテストです。
     */
    @Test
    public void testClearBuffer() {
        winRs.getOutputBuffer().append("test message 1\n");
        winRs.getOutputBuffer().append("test message 2\n");
        assertTrue(winRs.getOutputBuffer().length() > 0);

        winRs.clearBuffer();
        assertEquals(0, winRs.getOutputBuffer().length());
    }

    /**
     * execute メソッドのリトライおよび終了コード評価のテストです。
     * （無効なホストへの接続試行時に例外ハンドリングと終了コード評価が行われることを検証）
     */
    @Test
    public void testExecuteExceptionHandling() {
        winRs.setRemoteHost("invalid-host-for-test.invalid");
        winRs.setPort(5985);
        winRs.setRetryMax(0);
        winRs.setErrorCode(20);
        winRs.initialize();

        final int exitCode = winRs.execute("hostname");
        assertEquals(20, exitCode);
        assertEquals(MdlConst.LVL_E, winRs.getReturnLevel());
    }

    /**
     * execReturnText メソッドの例外時動作テストです。
     */
    @Test
    public void testExecReturnTextExceptionHandling() {
        winRs.setRemoteHost("invalid-host-for-test.invalid");
        winRs.setPort(5985);
        winRs.setRetryMax(0);
        winRs.initialize();

        final String result = winRs.execReturnText("hostname");
        assertNotNull(result);
        assertEquals("", result);
    }

    /**
     * configureLogging メソッドによる Verbose レベルごとのログレベルプロパティ設定を検証するテストです。
     */
    @Test
    public void testConfigureLogging() {
        // Verbose < 3 (warn)
        ClsWinRs.configureLogging(0);
        assertEquals("warn", System.getProperty("org.slf4j.simpleLogger.defaultLogLevel"));
        assertEquals("warn", System.getProperty("org.slf4j.simpleLogger.log.org.apache.cxf"));

        ClsWinRs.configureLogging(3);
        assertEquals("warn", System.getProperty("org.slf4j.simpleLogger.defaultLogLevel"));
        assertEquals("warn", System.getProperty("org.slf4j.simpleLogger.log.org.apache.cxf"));

        // Verbose 3, 4 (info)
        ClsWinRs.configureLogging(4);
        assertEquals("info", System.getProperty("org.slf4j.simpleLogger.defaultLogLevel"));
        assertEquals("info", System.getProperty("org.slf4j.simpleLogger.log.org.apache.cxf"));

        ClsWinRs.configureLogging(8);
        assertEquals("info", System.getProperty("org.slf4j.simpleLogger.defaultLogLevel"));
        assertEquals("info", System.getProperty("org.slf4j.simpleLogger.log.org.apache.cxf"));

        // Verbose >= 5 (debug)
        ClsWinRs.configureLogging(9);
        assertEquals("debug", System.getProperty("org.slf4j.simpleLogger.defaultLogLevel"));
        assertEquals("debug", System.getProperty("org.slf4j.simpleLogger.log.org.apache.cxf"));

        // 後始末 (デフォルトのwarnへ復元)
        ClsWinRs.configureLogging(0);
    }
}
