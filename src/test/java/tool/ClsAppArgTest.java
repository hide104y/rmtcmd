package tool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
 * {@link ClsAppArg} の単体テストクラスです。
 */
public class ClsAppArgTest {

    private static Path tempDir;

    private ClsLogger logger;
    private ClsAppArg appArg;

    /**
     * テストクラス実行前の初期化処理を行います。規約に基づき専用の一時ディレクトリを作成します。
     *
     * @throws IOException 一時ディレクトリ作成に失敗した場合
     */
    @BeforeClass
    public static void setUpBeforeClass() throws IOException {
        tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "UnitTest", "rmtcmd", "ClsAppArgTest");
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
        appArg = new ClsAppArg(logger);
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
        assertNotNull(appArg.getExeBaseName());
        assertNotNull(appArg.getExeDir());
        assertNotNull(appArg.getComSpec());
        assertFalse(appArg.isUsage());
        assertEquals(MdlConst.LVL_I, appArg.getReturnCode());
        assertEquals(0, appArg.getVerbose());
        assertFalse(appArg.isStackTrace());
        assertEquals("", appArg.getWorkDir());
        assertEquals("hostname", appArg.getCmdPath());
        assertEquals("localhost", appArg.getRemoteHost());
        assertEquals("", appArg.getTeePath());
        assertEquals("", appArg.getAddEnvPath());
        assertNotNull(appArg.getProcEnvs());
        assertTrue(appArg.getProcEnvs().isEmpty());
        assertEquals(ClsWinRs.EXEC_MODE_CMD, appArg.getExecMode());
        assertEquals(5985, appArg.getPort());
        assertEquals(180, appArg.getOpTimeout());
        assertEquals(120, appArg.getOpenTimeout());
        assertEquals(0, appArg.getRetryMax());
        assertEquals(5, appArg.getRetrySleep());
        assertFalse(appArg.isRetryRmtCmd());
        assertEquals("", appArg.getDomain());
        assertEquals("", appArg.getUserNoDomain());
        assertEquals("", appArg.getUsername());
        assertEquals("", appArg.getPassword());
        assertEquals(0, appArg.getAuthMechanism());
        assertEquals("0", appArg.getOkRetCsv());
        assertEquals("", appArg.getWarnRetCsv());
        assertEquals("", appArg.getErrRetCsv());
        assertEquals("", appArg.getOkMsgCsv());
        assertEquals("", appArg.getWarnMsgCsv());
        assertEquals("", appArg.getErrMsgCsv());
        assertEquals(ClsAppArg.NONE, appArg.getJudgeMode());
        assertEquals(MdlConst.INT_NULL, appArg.getWarnThreshold());
        assertEquals(MdlConst.INT_NULL, appArg.getErrThreshold());
        assertFalse(appArg.isErrAtNegative());
        assertEquals(MdlConst.INT_NULL, appArg.getErrorCode());
        assertEquals(MdlConst.INT_NULL, appArg.getWarnCode());
        assertFalse(appArg.isEchoRetcode());
        assertFalse(appArg.isAjsJob());
        assertNotNull(appArg.getHostname());
        assertEquals(3, appArg.getMaxHops());
        assertEquals(0, appArg.getCurHops());
        assertTrue(appArg.isLoopCheck());
    }

    /**
     * Getter / Setter の動作検証テストです。
     */
    @Test
    public void testGettersAndSetters() {
        appArg.setExeBaseName("TestExe");
        assertEquals("TestExe", appArg.getExeBaseName());

        appArg.setExeDir("C:\\temp");
        assertEquals("C:\\temp", appArg.getExeDir());

        appArg.setComSpec("cmd.exe");
        assertEquals("cmd.exe", appArg.getComSpec());

        appArg.setUsage(true);
        assertTrue(appArg.isUsage());

        appArg.setReturnCode(10);
        assertEquals(10, appArg.getReturnCode());

        appArg.setVerbose(2);
        assertEquals(2, appArg.getVerbose());

        appArg.setStackTrace(true);
        assertTrue(appArg.isStackTrace());

        appArg.setWorkDir("C:\\work");
        assertEquals("C:\\work", appArg.getWorkDir());

        appArg.setCmdPath("dir");
        assertEquals("dir", appArg.getCmdPath());

        appArg.setRemoteHost("192.168.1.100");
        assertEquals("192.168.1.100", appArg.getRemoteHost());

        appArg.setTeePath("C:\\temp\\tee.log");
        assertEquals("C:\\temp\\tee.log", appArg.getTeePath());

        appArg.setAddEnvPath("C:\\custom\\bin");
        assertEquals("C:\\custom\\bin", appArg.getAddEnvPath());

        final Map<String, String> map = new LinkedHashMap<>();
        map.put("KEY1", "VAL1");
        appArg.setProcEnvs(map);
        assertEquals(1, appArg.getProcEnvs().size());
        assertEquals("VAL1", appArg.getProcEnvs().get("KEY1"));

        appArg.setExecMode(ClsWinRs.EXEC_MODE_PS);
        assertEquals(ClsWinRs.EXEC_MODE_PS, appArg.getExecMode());

        appArg.setPort(5986);
        assertEquals(5986, appArg.getPort());

        appArg.setOpTimeout(200);
        assertEquals(200, appArg.getOpTimeout());

        appArg.setOpenTimeout(100);
        assertEquals(100, appArg.getOpenTimeout());

        appArg.setRetryMax(5);
        assertEquals(5, appArg.getRetryMax());

        appArg.setRetrySleep(15);
        assertEquals(15, appArg.getRetrySleep());

        appArg.setRetryRmtCmd(true);
        assertTrue(appArg.isRetryRmtCmd());

        appArg.setDomain("WORKGROUP");
        assertEquals("WORKGROUP", appArg.getDomain());

        appArg.setUserNoDomain("myUser");
        assertEquals("myUser", appArg.getUserNoDomain());

        appArg.setUsername("testUser");
        assertEquals("testUser", appArg.getUsername());

        appArg.setPassword("testPass");
        assertEquals("testPass", appArg.getPassword());

        appArg.setAuthMechanism(2);
        assertEquals(2, appArg.getAuthMechanism());

        appArg.setOkRetCsv("0,1");
        assertEquals("0,1", appArg.getOkRetCsv());

        appArg.setWarnRetCsv("2");
        assertEquals("2", appArg.getWarnRetCsv());

        appArg.setErrRetCsv("3,4");
        assertEquals("3,4", appArg.getErrRetCsv());

        appArg.setOkMsgCsv("OK");
        assertEquals("OK", appArg.getOkMsgCsv());

        appArg.setWarnMsgCsv("WARN");
        assertEquals("WARN", appArg.getWarnMsgCsv());

        appArg.setErrMsgCsv("ERR");
        assertEquals("ERR", appArg.getErrMsgCsv());

        appArg.setJudgeMode(ClsAppArg.RETURN_CODE);
        assertEquals(ClsAppArg.RETURN_CODE, appArg.getJudgeMode());

        appArg.setWarnThreshold(5);
        assertEquals(5, appArg.getWarnThreshold());

        appArg.setErrThreshold(10);
        assertEquals(10, appArg.getErrThreshold());

        appArg.setErrAtNegative(true);
        assertTrue(appArg.isErrAtNegative());

        appArg.setErrorCode(20);
        assertEquals(20, appArg.getErrorCode());

        appArg.setWarnCode(10);
        assertEquals(10, appArg.getWarnCode());

        appArg.setEchoRetcode(true);
        assertTrue(appArg.isEchoRetcode());

        appArg.setAjsJob(true);
        assertTrue(appArg.isAjsJob());

        appArg.setHostname("HostA");
        assertEquals("HostA", appArg.getHostname());

        appArg.setMaxHops(5);
        assertEquals(5, appArg.getMaxHops());

        appArg.setCurHops(2);
        assertEquals(2, appArg.getCurHops());

        appArg.setLoopCheck(false);
        assertFalse(appArg.isLoopCheck());
    }

    /**
     * 有効なコマンドライン引数の解析テストです。
     */
    @Test
    public void testGetArgsSuccess() {
        final String[] args = {
                "-h", "remote-host",
                "-port", "5986",
                "-u", "admin",
                "-p", "password123",
                "-cwd", "D:\\workspace",
                "-cmd", "echo test",
                "-rtee", "C:\\temp\\out.txt",
                "-optimeout", "240",
                "-opentimeout", "150",
                "-am", "2",
                "-retry-rmtcmd",
                "-penv", "ENV1=A,ENV2=B",
                "-add-penv-path", "C:\\tools",
                "-hop", "4",
                "-nlc",
                "-ret", "retcode",
                "-w", "5",
                "-e", "10",
                "-negative",
                "-warn", "15",
                "-err", "25",
                "-ok-ret", "0,1",
                "-warn-ret", "2",
                "-ng-ret", "9",
                "-ok-str", "DONE",
                "-warn-str", "ALERT",
                "-ng-str", "FATAL",
                "-echo-retcd"
        };

        final boolean result = appArg.getArgs(args);
        assertTrue(result);
        assertEquals("remote-host", appArg.getRemoteHost());
        assertEquals(5986, appArg.getPort());
        assertTrue(appArg.getUsername().endsWith("admin"));
        assertEquals("password123", appArg.getPassword());
        assertEquals("D:\\workspace", appArg.getWorkDir());
        assertEquals(ClsWinRs.EXEC_MODE_CMD, appArg.getExecMode());
        assertEquals("echo test", appArg.getCmdPath());
        assertEquals("C:\\temp\\out.txt", appArg.getTeePath());
        assertEquals(240, appArg.getOpTimeout());
        assertEquals(150, appArg.getOpenTimeout());
        assertEquals(2, appArg.getAuthMechanism());
        assertTrue(appArg.isRetryRmtCmd());
        assertEquals("C:\\tools", appArg.getAddEnvPath());
        assertEquals(2, appArg.getProcEnvs().size());
        assertEquals("A", appArg.getProcEnvs().get("ENV1"));
        assertEquals("B", appArg.getProcEnvs().get("ENV2"));
        assertEquals(4, appArg.getMaxHops());
        assertFalse(appArg.isLoopCheck());
        assertEquals(ClsAppArg.RETURN_CODE, appArg.getJudgeMode());
        assertEquals(5, appArg.getWarnThreshold());
        assertEquals(10, appArg.getErrThreshold());
        assertTrue(appArg.isErrAtNegative());
        assertEquals(15, appArg.getWarnCode());
        assertEquals(25, appArg.getErrorCode());
        assertEquals("0,1", appArg.getOkRetCsv());
        assertEquals("2", appArg.getWarnRetCsv());
        assertEquals("9", appArg.getErrRetCsv());
        assertEquals("DONE", appArg.getOkMsgCsv());
        assertEquals("ALERT", appArg.getWarnMsgCsv());
        assertEquals("FATAL", appArg.getErrMsgCsv());
        assertTrue(appArg.isEchoRetcode());
    }

    /**
     * 各実行モード指定オプションの解析テストです。
     */
    @Test
    public void testGetArgsExecModes() {
        // -ps
        appArg = new ClsAppArg(logger);
        assertTrue(appArg.getArgs(new String[]{"-p", "pass", "-ps", "Get-Process"}));
        assertEquals(ClsWinRs.EXEC_MODE_PS, appArg.getExecMode());
        assertEquals("Get-Process", appArg.getCmdPath());

        // -ec
        appArg = new ClsAppArg(logger);
        assertTrue(appArg.getArgs(new String[]{"-p", "pass", "-ec", "encoded_c"}));
        assertEquals(ClsWinRs.EXEC_MODE_EC, appArg.getExecMode());
        assertEquals("encoded_c", appArg.getCmdPath());

        // -ecmd
        appArg = new ClsAppArg(logger);
        assertTrue(appArg.getArgs(new String[]{"-p", "pass", "-ecmd", "encoded_cmd"}));
        assertEquals(ClsWinRs.EXEC_MODE_ECMD, appArg.getExecMode());
        assertEquals("encoded_cmd", appArg.getCmdPath());

        // -es
        appArg = new ClsAppArg(logger);
        assertTrue(appArg.getArgs(new String[]{"-p", "pass", "-es", "encoded_ps"}));
        assertEquals(ClsWinRs.EXEC_MODE_ES, appArg.getExecMode());
        assertEquals("encoded_ps", appArg.getCmdPath());

        // -exe
        appArg = new ClsAppArg(logger);
        assertTrue(appArg.getArgs(new String[]{"-p", "pass", "-exe", "notepad.exe"}));
        assertEquals(ClsWinRs.EXEC_MODE_EXE, appArg.getExecMode());
        assertEquals("notepad.exe", appArg.getCmdPath());

        // -c (default -> EXEC_MODE_NORMAL)
        appArg = new ClsAppArg(logger);
        assertTrue(appArg.getArgs(new String[]{"-p", "pass", "-c", "whoami"}));
        assertEquals(ClsWinRs.EXEC_MODE_NORMAL, appArg.getExecMode());
        assertEquals("whoami", appArg.getCmdPath());
    }

    /**
     * 結果判定モード指定オプション（-ret）の解析テストです。
     */
    @Test
    public void testGetArgsResultJudgment() {
        // -ret none
        appArg = new ClsAppArg(logger);
        assertTrue(appArg.getArgs(new String[]{"-p", "pass", "-ret", "none"}));
        assertEquals(ClsAppArg.NONE, appArg.getJudgeMode());

        // -ret normal
        appArg = new ClsAppArg(logger);
        assertTrue(appArg.getArgs(new String[]{"-p", "pass", "-ret", "normal"}));
        assertEquals(ClsAppArg.ALWAYS_NORMAL, appArg.getJudgeMode());

        // -ret warn
        appArg = new ClsAppArg(logger);
        assertTrue(appArg.getArgs(new String[]{"-p", "pass", "-ret", "warn"}));
        assertEquals(ClsAppArg.ALWAYS_WARN, appArg.getJudgeMode());

        // -ret error
        appArg = new ClsAppArg(logger);
        assertTrue(appArg.getArgs(new String[]{"-p", "pass", "-ret", "error"}));
        assertEquals(ClsAppArg.ALWAYS_ERROR, appArg.getJudgeMode());

        // -ret retcode (デフォルトで errorCode=20, warnCode=10 が設定されること)
        appArg = new ClsAppArg(logger);
        assertTrue(appArg.getArgs(new String[]{"-p", "pass", "-ret", "retcode"}));
        assertEquals(ClsAppArg.RETURN_CODE, appArg.getJudgeMode());
        assertEquals(MdlConst.LVL_E, appArg.getErrorCode());
        assertEquals(MdlConst.LVL_W, appArg.getWarnCode());
    }

    /**
     * パスワード未指定時のバリデーションエラーのテストです。
     */
    @Test
    public void testGetArgsMissingPassword() {
        final boolean result = appArg.getArgs(new String[]{"-h", "remote-host", "-u", "admin"});
        assertFalse(result);
    }

    /**
     * 結果判定モード説明テキスト取得メソッドのテストです。
     */
    @Test
    public void testGetJudgeText() {
        assertEquals("retcode：閾値による判定", appArg.getJudgeText(ClsAppArg.RETURN_CODE));
        assertEquals("nomal：常に正常終了", appArg.getJudgeText(ClsAppArg.ALWAYS_NORMAL));
        assertEquals("warn：常に警告終了", appArg.getJudgeText(ClsAppArg.ALWAYS_WARN));
        assertEquals("error：常に異常終了", appArg.getJudgeText(ClsAppArg.ALWAYS_ERROR));
        assertEquals("none：コマンドの戻り値を返却", appArg.getJudgeText(ClsAppArg.NONE));
    }

    /**
     * ドメイン付きユーザー名のパース検証テストです。
     */
    @Test
    public void testDomainUsernameParsing() {
        // DOMAIN/user 形式
        appArg = new ClsAppArg(logger);
        assertTrue(appArg.getArgs(new String[]{"-p", "pass", "-u", "DOMAIN\\user1"}));
        assertEquals("DOMAIN", appArg.getDomain());
        assertEquals("user1", appArg.getUserNoDomain());
        assertEquals("DOMAIN\\user1", appArg.getUsername());

        // user@domain 形式
        appArg = new ClsAppArg(logger);
        assertTrue(appArg.getArgs(new String[]{"-p", "pass", "-u", "user2@corp.local"}));
        assertEquals("corp.local", appArg.getDomain());
        assertEquals("user2", appArg.getUserNoDomain());

        // -d オプション
        appArg = new ClsAppArg(logger);
        assertTrue(appArg.getArgs(new String[]{"-p", "pass", "-d", "192.168.0.12", "-u", "testuser"}));
        assertEquals("192.168.0.12", appArg.getDomain());
        assertEquals("testuser", appArg.getUserNoDomain());
        assertEquals("192.168.0.12\\testuser", appArg.getUsername());

        // -domain オプション
        appArg = new ClsAppArg(logger);
        assertTrue(appArg.getArgs(new String[]{"-p", "pass", "-domain", "MYDOM", "-u", "admin"}));
        assertEquals("MYDOM", appArg.getDomain());
        assertEquals("admin", appArg.getUserNoDomain());
        assertEquals("MYDOM\\admin", appArg.getUsername());
    }

    /**
     * ジョブ名およびUsageメソッドのテストです。
     */
    @Test
    public void testJobNameAndUsage() {
        assertTrue(appArg.getArgs(new String[]{"-p", "testPass"}));
        assertEquals("", appArg.getJobName());
        appArg.usage();
    }
}