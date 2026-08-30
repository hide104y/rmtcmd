package tool;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.http.client.config.AuthSchemes;

import io.cloudsoft.winrm4j.client.PayloadEncryptionMode;
import io.cloudsoft.winrm4j.client.WinRmClientContext;
import io.cloudsoft.winrm4j.winrm.WinRmTool;
import io.cloudsoft.winrm4j.winrm.WinRmToolResponse;
import tool.cmnclslib.cls.ClsCmdStatus;
import tool.cmnclslib.cls.ClsLogger;
import tool.cmnclslib.mdl.MdlConst;

/**
 * WinRMプロトコル経由でのリモートコマンド実行および実行制御を管理するクラスです。
 *
 * <p>接続先ホスト、ポート番号、認証方式、実行対象コマンド（DOSコマンド/PowerShell）、タイムアウト、
 * 自動リトライ制御、環境変数の受け渡し、および結果判定ステータスの評価を行います。</p>
 *
 * <p><b>使用例:</b></p>
 * <pre>{@code
 * ClsLogger logger = new ClsLogger();
 * ClsWinRs winRs = new ClsWinRs(logger);
 * winRs.setRemoteHost("server01");
 * winRs.setUsername("admin");
 * winRs.setPassword("secret");
 * winRs.initialize();
 * int exitCode = winRs.execute("hostname");
 * }</pre>
 */
public class ClsWinRs {

    /** 実行モード: 生コマンド文字列をそのまま実行します。 */
    public static final int EXEC_MODE_NORMAL = 0;
    /** 実行モード: cmd.exe /c 経由でDOSコマンドを実行します。 */
    public static final int EXEC_MODE_CMD = 1;
    /** 実行モード: powershell -encodedCommand 経由でPowerShellスクリプトを実行します。 */
    public static final int EXEC_MODE_PS = 2;
    /** 実行モード: Base64エンコード済みPowerShell文字列を実行します。 */
    public static final int EXEC_MODE_ES = 3;
    /** 実行モード: Base64エンコード済みDOSコマンドをデコードして実行します。 */
    public static final int EXEC_MODE_EC = 4;
    /** 実行モード: Base64エンコード済みDOSコマンドをデコードし cmd.exe /c 経由で実行します。 */
    public static final int EXEC_MODE_ECMD = 5;
    /** 実行モード: 実行ファイルを直接起動します。 */
    public static final int EXEC_MODE_EXE = 6;

    /** 自動再試行（リトライ）対象となる例外メッセージ検出用の正規表現パターン（日本語および英語メッセージ対応） */
    private static final Pattern RETRY_ERR_REGEX = Pattern.compile(
            "(接続.*失敗.*削除の対象としてマークされているレジストリ.*キーに対して無効な操作を実行)|"
            + "(registry.*key.*marked\\s+for\\s+deletion)|"
            + "(marked\\s+for\\s+deletion)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    /** IPv4アドレス形式検出用の正規表現パターン */
    private static final Pattern IPV4_REGEX = Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
    /** 日本語Windows用文字セット（環境に応じてMS932、Windows-31J、またはUTF-8へフォールバック） */
    private static final Charset CHARSET_MS932;

    static {
        if (Charset.isSupported("MS932")) {
            CHARSET_MS932 = Charset.forName("MS932");
        } else if (Charset.isSupported("Windows-31J")) {
            CHARSET_MS932 = Charset.forName("Windows-31J");
        } else {
            CHARSET_MS932 = StandardCharsets.UTF_8;
        }
    }

    /** ログ出力用ロガー */
    private final ClsLogger logger;
    /** コマンド実行結果・判定制御オブジェクト */
    private final ClsCmdStatus cmdStatus;
    /** 実行出力結果保持用バッファ */
    private StringBuilder outputBuffer;

    private String remoteHost = "localhost";
    private String domain = "";
    private String username = "";
    private String password = "";
    private String comSpec = "cmd";
    private String workDir = "";
    private String teePath = "";
    private String addEnvPath = "";
    private boolean logWrite;
    private boolean stackTrace;
    private boolean useHttps;
    private int execMode = EXEC_MODE_CMD;
    private int verbose;
    private int port = 5985;
    private int authMechanism;
    private int opTimeout = 180;
    private int openTimeout = 120;
    private Map<String, String> procEnvs = new LinkedHashMap<>();
    private boolean isException;
    private boolean isMoreRetry;
    private int retryMax;
    private int retrySleep = 5;
    private boolean retryRmtCmd;
    private int cmdExitCode;

    /**
     * ロガーを指定して {@link ClsWinRs} の新しいインスタンスを生成します。
     *
     * <p><b>使用例:</b></p>
     * <pre>{@code
     * ClsLogger logger = new ClsLogger();
     * ClsWinRs winRs = new ClsWinRs(logger);
     * }</pre>
     *
     * @param logger ログ出力に使用する {@link ClsLogger} オブジェクト。
     */
    public ClsWinRs(final ClsLogger logger) {
        this.logger = logger;
        this.cmdStatus = new ClsCmdStatus(this.logger);
        this.outputBuffer = new StringBuilder();
        final String comSpecEnv = System.getenv("ComSpec");
        if (comSpecEnv != null && !comSpecEnv.isBlank()) {
            this.comSpec = comSpecEnv;
        } else {
            this.comSpec = "cmd";
        }
    }

    /**
     * 接続先のリモートホスト名またはIPアドレスを取得します。
     *
     * @return リモートホスト名。
     */
    public String getRemoteHost() {
        return remoteHost;
    }

    /**
     * 接続先のリモートホスト名またはIPアドレスを設定します。
     *
     * @param remoteHost リモートホスト名。
     */
    public void setRemoteHost(final String remoteHost) {
        this.remoteHost = remoteHost != null ? remoteHost : "";
    }

    /**
     * WinRM接続ポート番号を取得します。
     *
     * @return ポート番号。
     */
    public int getPort() {
        return port;
    }

    /**
     * WinRM接続ポート番号を設定します。
     *
     * @param port ポート番号。
     */
    public void setPort(final int port) {
        this.port = port;
    }

    /**
     * HTTPS通信を使用するかどうかを取得します。
     *
     * @return HTTPS通信を使用する場合は {@code true}。
     */
    public boolean isUseHttps() {
        return useHttps;
    }

    /**
     * HTTPS通信使用フラグを設定します。
     *
     * @param useHttps HTTPS通信フラグ。
     */
    public void setUseHttps(final boolean useHttps) {
        this.useHttps = useHttps;
    }

    /**
     * 認証ドメイン名を取得します。
     *
     * @return ドメイン名。
     */
    public String getDomain() {
        return domain;
    }

    /**
     * 認証ドメイン名を設定します。
     *
     * @param domain ドメイン名。
     */
    public void setDomain(final String domain) {
        this.domain = domain != null ? domain : "";
    }

    /**
     * 認証ユーザー名を取得します。
     *
     * @return ユーザー名。
     */
    public String getUsername() {
        return username;
    }

    /**
     * 認証ユーザー名を設定します。
     *
     * @param username ユーザー名。
     */
    public void setUsername(final String username) {
        this.username = username != null ? username : "";
    }

    /**
     * 認証パスワードを取得します。
     *
     * @return パスワード文字列。
     */
    public String getPassword() {
        return password;
    }

    /**
     * 認証パスワードを設定します。
     *
     * @param password パスワード文字列。
     */
    public void setPassword(final String password) {
        this.password = password != null ? password : "";
    }

    /**
     * コマンドインタプリタの実行ファイルパス (ComSpec) を取得します。
     *
     * @return コマンドインタプリタパス。
     */
    public String getComSpec() {
        return comSpec;
    }

    /**
     * コマンドインタプリタの実行ファイルパスを設定します。
     *
     * @param comSpec コマンドインタプリタパス。
     */
    public void setComSpec(final String comSpec) {
        this.comSpec = comSpec != null ? comSpec : "cmd";
    }

    /**
     * リモート実行時の作業ディレクトリパスを取得します。
     *
     * @return 作業ディレクトリパス。
     */
    public String getWorkDir() {
        return workDir;
    }

    /**
     * リモート実行時の作業ディレクトリパスを設定します。
     *
     * @param workDir 作業ディレクトリパス。
     */
    public void setWorkDir(final String workDir) {
        this.workDir = workDir != null ? workDir : "";
    }

    /**
     * リモート出力を分岐保存する Tee-Object ファイルパスを取得します。
     *
     * @return Teeファイルパス。
     */
    public String getTeePath() {
        return teePath;
    }

    /**
     * リモート出力を分岐保存する Tee-Object ファイルパスを設定します。
     *
     * @param teePath Teeファイルパス。
     */
    public void setTeePath(final String teePath) {
        this.teePath = teePath != null ? teePath : "";
    }

    /**
     * 環境変数PATHの先頭に追加するディレクトリパスを取得します。
     *
     * @return 追加PATH文字列。
     */
    public String getAddEnvPath() {
        return addEnvPath;
    }

    /**
     * 環境変数PATHの先頭に追加するディレクトリパスを設定します。
     *
     * @param addEnvPath 追加PATH文字列。
     */
    public void setAddEnvPath(final String addEnvPath) {
        this.addEnvPath = addEnvPath != null ? addEnvPath : "";
    }

    /**
     * リモートプロセスに設定する追加環境変数のマップを取得します。
     *
     * @return 環境変数キーと値のマップ。
     */
    public Map<String, String> getProcEnvs() {
        return procEnvs;
    }

    /**
     * リモートプロセスに設定する追加環境変数のマップを設定します。
     *
     * @param procEnvs 環境変数キーと値のマップ。
     */
    public void setProcEnvs(final Map<String, String> procEnvs) {
        this.procEnvs = procEnvs != null ? procEnvs : new LinkedHashMap<>();
    }

    /**
     * コマンド実行モード (ExecMode) を取得します。
     *
     * @return 実行モードコード。
     */
    public int getExecMode() {
        return execMode;
    }

    /**
     * コマンド実行モード (ExecMode) を設定します。
     *
     * @param execMode 実行モードコード。
     */
    public void setExecMode(final int execMode) {
        this.execMode = execMode;
    }

    /**
     * 詳細ログ出力レベル (Verbose) を取得します。
     *
     * @return 詳細ログ出力レベル。
     */
    public int getVerbose() {
        return verbose;
    }

    /**
     * 詳細ログ出力レベル (Verbose) を設定します。
     *
     * @param verbose 詳細ログ出力レベル。
     */
    public void setVerbose(final int verbose) {
        this.verbose = verbose;
        configureLogging(verbose);
    }

    /**
     * 認証機構コードを取得します。
     *
     * @return 認証機構コード。
     */
    public int getAuthMechanism() {
        return authMechanism;
    }

    /**
     * 認証機構コードを設定します。
     *
     * @param authMechanism 認証機構コード。
     */
    public void setAuthMechanism(final int authMechanism) {
        this.authMechanism = authMechanism;
    }

    /**
     * WinRM操作タイムアウト秒数を取得します。
     *
     * @return 操作タイムアウト（秒）。
     */
    public int getOpTimeout() {
        return opTimeout;
    }

    /**
     * WinRM操作タイムアウト秒数を設定します。
     *
     * @param opTimeout 操作タイムアウト（秒）。
     */
    public void setOpTimeout(final int opTimeout) {
        this.opTimeout = opTimeout;
    }

    /**
     * WinRM接続オープンタイムアウト秒数を取得します。
     *
     * @return 接続オープンタイムアウト（秒）。
     */
    public int getOpenTimeout() {
        return openTimeout;
    }

    /**
     * WinRM接続オープンタイムアウト秒数を設定します。
     *
     * @param openTimeout 接続オープンタイムアウト（秒）。
     */
    public void setOpenTimeout(final int openTimeout) {
        this.openTimeout = openTimeout;
    }

    /**
     * コマンド実行時の出力をログへ書き出すかどうかを取得します。
     *
     * @return ログ出力を行う場合は {@code true}。
     */
    public boolean isLogWrite() {
        return logWrite;
    }

    /**
     * コマンド実行時のログ出力フラグを設定します。
     *
     * @param logWrite ログ出力フラグ。
     */
    public void setLogWrite(final boolean logWrite) {
        this.logWrite = logWrite;
    }

    /**
     * 例外発生時にスタックトレースを出力するかどうかを取得します。
     *
     * @return スタックトレース出力が有効な場合は {@code true}。
     */
    public boolean isStackTrace() {
        return stackTrace;
    }

    /**
     * 例外発生時のスタックトレース出力フラグを設定します。
     *
     * @param stackTrace スタックトレース出力フラグ。
     */
    public void setStackTrace(final boolean stackTrace) {
        this.stackTrace = stackTrace;
    }

    /**
     * コマンド実行の出力文字列を保持するバッファを取得します。
     *
     * @return 出力文字列バッファ。
     */
    public StringBuilder getOutputBuffer() {
        return outputBuffer;
    }

    /**
     * コマンド実行の出力文字列を保持するバッファを設定します。
     *
     * @param outputBuffer 出力文字列バッファ。
     */
    public void setOutputBuffer(final StringBuilder outputBuffer) {
        this.outputBuffer = outputBuffer;
    }

    /**
     * 最大リトライ回数を取得します。
     *
     * @return 最大リトライ回数。
     */
    public int getRetryMax() {
        return retryMax;
    }

    /**
     * 最大リトライ回数を設定します。
     *
     * @param retryMax 最大リトライ回数。
     */
    public void setRetryMax(final int retryMax) {
        this.retryMax = retryMax;
    }

    /**
     * リトライ試行間の待機秒数を取得します。
     *
     * @return リトライ待機秒数。
     */
    public int getRetrySleep() {
        return retrySleep;
    }

    /**
     * リトライ試行間の待機秒数を設定します。
     *
     * @param retrySleep リトライ待機秒数。
     */
    public void setRetrySleep(final int retrySleep) {
        this.retrySleep = retrySleep;
    }

    /**
     * リモートコマンド自体が異常終了した場合にもリトライを行うかどうかを取得します。
     *
     * @return コマンド異常時にもリトライする場合は {@code true}。
     */
    public boolean isRetryRmtCmd() {
        return retryRmtCmd;
    }

    /**
     * リモートコマンド異常終了時のリトライフラグを設定します。
     *
     * @param retryRmtCmd コマンドリトライフラグ。
     */
    public void setRetryRmtCmd(final boolean retryRmtCmd) {
        this.retryRmtCmd = retryRmtCmd;
    }

    /**
     * 正常終了判定用の戻り値CSVリストを取得します。
     *
     * @return 戻り値CSV文字列。
     */
    public String getOkRetCsv() {
        return cmdStatus.getOkReturnCodeCsv();
    }

    /**
     * 正常終了判定用の戻り値CSVリストを設定します。
     *
     * @param okRetCsv 戻り値CSV文字列。
     */
    public void setOkRetCsv(final String okRetCsv) {
        this.cmdStatus.setOkReturnCodeCsv(okRetCsv != null ? okRetCsv : "");
    }

    /**
     * 警告終了判定用の戻り値CSVリストを取得します。
     *
     * @return 戻り値CSV文字列。
     */
    public String getWarnRetCsv() {
        return cmdStatus.getWarnReturnCodeCsv();
    }

    /**
     * 警告終了判定用の戻り値CSVリストを設定します。
     *
     * @param warnRetCsv 戻り値CSV文字列。
     */
    public void setWarnRetCsv(final String warnRetCsv) {
        this.cmdStatus.setWarnReturnCodeCsv(warnRetCsv != null ? warnRetCsv : "");
    }

    /**
     * 異常終了判定用の戻り値CSVリストを取得します。
     *
     * @return 戻り値CSV文字列。
     */
    public String getErrRetCsv() {
        return cmdStatus.getErrRetCodeCsv();
    }

    /**
     * 異常終了判定用の戻り値CSVリストを設定します。
     *
     * @param errRetCsv 戻り値CSV文字列。
     */
    public void setErrRetCsv(final String errRetCsv) {
        this.cmdStatus.setErrRetCodeCsv(errRetCsv != null ? errRetCsv : "");
    }

    /**
     * 正常終了判定用の標準出力メッセージCSVリストを取得します。
     *
     * @return メッセージCSV文字列。
     */
    public String getOkMsgCsv() {
        return cmdStatus.getOkMessageCsv();
    }

    /**
     * 正常終了判定用の標準出力メッセージCSVリストを設定します。
     *
     * @param okMsgCsv メッセージCSV文字列。
     */
    public void setOkMsgCsv(final String okMsgCsv) {
        this.cmdStatus.setOkMessageCsv(okMsgCsv != null ? okMsgCsv : "");
    }

    /**
     * 警告終了判定用の標準出力メッセージCSVリストを取得します。
     *
     * @return メッセージCSV文字列。
     */
    public String getWarnMsgCsv() {
        return cmdStatus.getWarnMessageCsv();
    }

    /**
     * 警告終了判定用の標準出力メッセージCSVリストを設定します。
     *
     * @param warnMsgCsv メッセージCSV文字列。
     */
    public void setWarnMsgCsv(final String warnMsgCsv) {
        this.cmdStatus.setWarnMessageCsv(warnMsgCsv != null ? warnMsgCsv : "");
    }

    /**
     * 異常終了判定用の標準出力メッセージCSVリストを取得します。
     *
     * @return メッセージCSV文字列。
     */
    public String getErrMsgCsv() {
        return cmdStatus.getErrorMessageCsv();
    }

    /**
     * 異常終了判定用の標準出力メッセージCSVリストを設定します。
     *
     * @param errMsgCsv メッセージCSV文字列。
     */
    public void setErrMsgCsv(final String errMsgCsv) {
        this.cmdStatus.setErrorMessageCsv(errMsgCsv != null ? errMsgCsv : "");
    }

    /**
     * 警告終了判定の閾値を取得します。
     *
     * @return 警告閾値。
     */
    public int getWarnThreshold() {
        return cmdStatus.getWarnThreshold();
    }

    /**
     * 警告終了判定の閾値を設定します。
     *
     * @param warnThreshold 警告閾値。
     */
    public void setWarnThreshold(final int warnThreshold) {
        this.cmdStatus.setWarnThreshold(warnThreshold);
    }

    /**
     * 異常終了判定の閾値を取得します。
     *
     * @return 異常閾値。
     */
    public int getErrThreshold() {
        return cmdStatus.getErrorThreshold();
    }

    /**
     * 異常終了判定の閾値を設定します。
     *
     * @param errThreshold 異常閾値。
     */
    public void setErrThreshold(final int errThreshold) {
        this.cmdStatus.setErrorThreshold(errThreshold);
    }

    /**
     * 負の終了コードを異常として扱うかどうかを取得します。
     *
     * @return 負値を異常とする場合は {@code true}。
     */
    public boolean isErrAtNegative() {
        return cmdStatus.isErrAtNegative();
    }

    /**
     * 負の終了コードを異常として扱うかどうかのフラグを設定します。
     *
     * @param errAtNegative 負値異常フラグ。
     */
    public void setErrAtNegative(final boolean errAtNegative) {
        this.cmdStatus.setErrAtNegative(errAtNegative);
    }

    /**
     * コマンド成否にかかわらず常に正常終了とするかどうかを取得します。
     *
     * @return 常時正常終了の場合は {@code true}。
     */
    public boolean isAlwaysNormal() {
        return cmdStatus.isAlwaysNormal();
    }

    /**
     * コマンド成否にかかわらず常に正常終了とするフラグを設定します。
     *
     * @param alwaysNormal 常時正常終了フラグ。
     */
    public void setAlwaysNormal(final boolean alwaysNormal) {
        this.cmdStatus.setAlwaysNormal(alwaysNormal);
    }

    /**
     * 異常終了時に返却するカスタム終了コードを取得します。
     *
     * @return 異常終了コード。
     */
    public int getErrorCode() {
        return cmdStatus.getErrorCode();
    }

    /**
     * 異常終了時に返却するカスタム終了コードを設定します。
     *
     * @param errorCode 異常終了コード。
     */
    public void setErrorCode(final int errorCode) {
        this.cmdStatus.setErrorCode(errorCode);
    }

    /**
     * 警告終了時に返却するカスタム終了コードを取得します。
     *
     * @return 警告終了コード。
     */
    public int getWarnCode() {
        return cmdStatus.getWarnCode();
    }

    /**
     * 警告終了時に返却するカスタム終了コードを設定します。
     *
     * @param warnCode 警告終了コード。
     */
    public void setWarnCode(final int warnCode) {
        this.cmdStatus.setWarnCode(warnCode);
    }

    /**
     * リモートコマンドの実行ステータスコードを取得します。
     *
     * @return リモートコマンド終了コード。
     */
    public int getCmdExitCode() {
        return cmdExitCode;
    }

    /**
     * リモートコマンドの実行ステータスコードを設定します。
     *
     * @param cmdExitCode リモートコマンド終了コード。
     */
    public void setCmdExitCode(final int cmdExitCode) {
        this.cmdExitCode = cmdExitCode;
    }

    /**
     * メソッドの最終的な終了ステータスコードを取得します。
     *
     * @return メソッド終了コード。
     */
    public int getMethodExit() {
        return cmdStatus.getMethodExitStatus();
    }

    /**
     * メソッドの最終的な終了ステータスコードを設定します。
     *
     * @param methodExit メソッド終了コード。
     */
    public void setMethodExit(final int methodExit) {
        this.cmdStatus.setMethodExitStatus(methodExit);
    }

    /**
     * 実行結果のステータスレベルを取得します。
     *
     * @return ステータスレベル ({@link MdlConst#LVL_I}, {@link MdlConst#LVL_W}, {@link MdlConst#LVL_E})。
     */
    public int getReturnLevel() {
        return cmdStatus.getReturnLevel();
    }

    /**
     * 実行結果のステータスレベルを設定します。
     *
     * @param returnLevel ステータスレベル。
     */
    public void setReturnLevel(final int returnLevel) {
        this.cmdStatus.setReturnLevel(returnLevel);
    }

    /**
     * 詳細ログ出力レベル (Verbose) に応じて、SLF4J / Apache CXF 等の内部ロギングレベルを設定します。
     *
     * <p>Verbose が 4 未満（0〜3）の場合は INFO ログを非表示（WARN レベル）にし、
     * 3 ～ 8 の場合は INFO レベル、9 以上の場合は DEBUG レベルを設定します。</p>
     *
     * <p><b>使用例:</b></p>
     * <pre>{@code
     * ClsWinRs.configureLogging(3);
     * }</pre>
     *
     * @param verbose 詳細ログ出力レベル。
     */
    public static void configureLogging(final int verbose) {
        if (verbose < 4) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
            System.setProperty("org.slf4j.simpleLogger.log.org.apache.cxf", "warn");
            System.setProperty("org.slf4j.simpleLogger.log.org.apache.http", "warn");
            System.setProperty("org.slf4j.simpleLogger.log.io.cloudsoft", "warn");
        } else if (verbose < 9) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
            System.setProperty("org.slf4j.simpleLogger.log.org.apache.cxf", "info");
            System.setProperty("org.slf4j.simpleLogger.log.org.apache.http", "info");
            System.setProperty("org.slf4j.simpleLogger.log.io.cloudsoft", "info");
        } else {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
            System.setProperty("org.slf4j.simpleLogger.log.org.apache.cxf", "debug");
            System.setProperty("org.slf4j.simpleLogger.log.org.apache.http", "debug");
            System.setProperty("org.slf4j.simpleLogger.log.io.cloudsoft", "debug");
        }
    }

    /**
     * 本クラスおよび内部コマンドステータスオブジェクトの初期化を行います。
     *
     * <p><b>使用例:</b></p>
     * <pre>{@code
     * winRs.setVerbose(2);
     * winRs.initialize();
     * }</pre>
     */
    public void initialize() {
        configureLogging(verbose);
        cmdStatus.setVerbose(verbose);
        cmdStatus.setDebugLevel(MdlConst.LVL_NONE);
        cmdStatus.initialize();
    }

    /**
     * 指定されたコマンドを実行します。設定されたリトライ条件に従い自動再試行を行います。
     *
     * <p><b>使用例:</b></p>
     * <pre>{@code
     * int status = winRs.execute("dir C:\\");
     * }</pre>
     *
     * @param command 実行するコマンド文字列。
     * @return メソッドの最終的な終了ステータスコード。
     */
    public int execute(final String command) {
        cmdExitCode = -1;
        cmdStatus.setMethodExitStatus(cmdStatus.getErrorCode() == MdlConst.INT_NULL ? MdlConst.LVL_E : cmdStatus.getErrorCode());
        cmdStatus.setReturnLevel(MdlConst.LVL_I);

        for (int i = 0; i <= retryMax; i++) {
            if (logWrite && retryMax > 0 && verbose > 0 && i > 0) {
                logger.writeLine(MdlConst.LVL_NONE, "== RETRY       = " + i + "/" + retryMax);
            }

            try {
                executeOnce(command);
            } catch (final Exception ex) {
                logger.writeLine(MdlConst.LVL_NONE, "EXCEPTION-1 : " + ex.getMessage());
                if (stackTrace) {
                    logger.writeLine(MdlConst.LVL_NONE, "");
                    for (final StackTraceElement ste : ex.getStackTrace()) {
                        logger.writeLine(MdlConst.LVL_NONE, ste.toString());
                    }
                    logger.writeLine(MdlConst.LVL_NONE, "");
                }
            }

            if (!isException) {
                if (retryRmtCmd) {
                    if (cmdStatus.getReturnLevel() != MdlConst.LVL_E) {
                        break;
                    }
                } else {
                    break;
                }
            }
            // Wait
            if (i < retryMax) {
                logger.writeLine(MdlConst.LVL_NONE, "");
                try {
                    Thread.sleep(retrySleep * 1000L);
                } catch (final InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (retryMax == 0 && isMoreRetry) {
            try {
                executeOnce(command);
            } catch (final Exception ex) {
                logger.writeLine(MdlConst.LVL_NONE, "EXCEPTION-2 : " + ex.getMessage());
                if (stackTrace) {
                    logger.writeLine(MdlConst.LVL_NONE, "");
                    for (final StackTraceElement ste : ex.getStackTrace()) {
                        logger.writeLine(MdlConst.LVL_NONE, ste.toString());
                    }
                    logger.writeLine(MdlConst.LVL_NONE, "");
                }
            }
        }
        return cmdStatus.getMethodExitStatus();
    }

    /**
     * 指定されたコマンドをリモート環境（WinRM / PowerShell）で一度だけ実行します。
     *
     * <p><b>使用例:</b></p>
     * <pre>{@code
     * int level = winRs.executeOnce("whoami");
     * }</pre>
     *
     * @param command 実行するコマンド文字列。
     * @return コマンド実行結果のステータスレベル。
     */
    public int executeOnce(final String command) {
        isException = false;
        isMoreRetry = false;
        cmdExitCode = -1;
        cmdStatus.setMethodExitStatus(cmdStatus.getErrorCode() == MdlConst.INT_NULL ? MdlConst.LVL_E : cmdStatus.getErrorCode());
        cmdStatus.setReturnLevel(MdlConst.LVL_I);
        cmdStatus.resetFlags();
        WinRmClientContext context = null;

        try {
            if (logWrite && verbose > 4) {
                logger.writeLine(MdlConst.LVL_NONE, "[ExecuteOnce()][CP-001] START");
            }

            context = WinRmClientContext.newInstance();
            final String authScheme = getAuthSchemeString(authMechanism);

            if (logWrite && verbose > 4) {
                logger.writeLine(MdlConst.LVL_NONE, "[ExecuteOnce()][CP-002] TRY : WinRmTool.Builder.builder()");
            }

            String effectiveDomain = domain != null ? domain.trim() : "";
            String effectiveUsername = username != null ? username.trim() : "";

            // ユーザー名からドメイン部（DOMAIN/user または user@domain）を必ず分離
            if (!effectiveUsername.isBlank()) {
                final int slashIdx = effectiveUsername.indexOf('\\');
                final int atIdx = effectiveUsername.indexOf('@');
                if (slashIdx > 0) {
                    if (effectiveDomain.isBlank()) {
                        effectiveDomain = effectiveUsername.substring(0, slashIdx).trim();
                    }
                    effectiveUsername = effectiveUsername.substring(slashIdx + 1).trim();
                } else if (atIdx > 0) {
                    if (effectiveDomain.isBlank()) {
                        effectiveDomain = effectiveUsername.substring(atIdx + 1).trim();
                    }
                    effectiveUsername = effectiveUsername.substring(0, atIdx).trim();
                }
            }

            // WORKGROUP, IPアドレス, "." はローカル指定とみなし、NTLM / Basic の Domain 名としては渡さない (空にする)
            if ("WORKGROUP".equalsIgnoreCase(effectiveDomain) || ".".equals(effectiveDomain)
                    || effectiveDomain.equalsIgnoreCase(remoteHost)
                    || IPV4_REGEX.matcher(effectiveDomain).matches()) {
                effectiveDomain = "";
            }

            if (logWrite && verbose > 2) {
                logger.writeLine(MdlConst.LVL_NONE, "== EFFECTIVE DOMAIN = " + (effectiveDomain.isEmpty() ? "(none)" : effectiveDomain));
                logger.writeLine(MdlConst.LVL_NONE, "== EFFECTIVE USER   = " + effectiveUsername);
            }

            final WinRmTool.Builder builder;
            if (!effectiveDomain.isEmpty()) {
                builder = WinRmTool.Builder.builder(remoteHost, effectiveDomain, effectiveUsername, password);
            } else {
                builder = WinRmTool.Builder.builder(remoteHost, effectiveUsername, password);
            }

            builder.authenticationScheme(authScheme)
                    .port(port)
                    .useHttps(useHttps)
                    .disableCertificateChecks(true)
                    .payloadEncryptionMode(PayloadEncryptionMode.OPTIONAL)
                    .context(context);

            if (workDir != null && !workDir.isBlank()) {
                builder.workingDirectory(workDir);
            }

            final Map<String, String> effectiveEnvs = new LinkedHashMap<>();
            if (procEnvs != null && !procEnvs.isEmpty()) {
                for (final Map.Entry<String, String> entry : procEnvs.entrySet()) {
                    if ("+PATH".equalsIgnoreCase(entry.getKey())) {
                        if (addEnvPath == null || addEnvPath.isBlank()) {
                            addEnvPath = entry.getValue();
                        }
                    } else {
                        effectiveEnvs.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            if (addEnvPath != null && !addEnvPath.isBlank()) {
                String existingPath = effectiveEnvs.get("PATH");
                if (existingPath == null) {
                    existingPath = effectiveEnvs.get("Path");
                }
                if (existingPath != null && !existingPath.isBlank()) {
                    effectiveEnvs.put("PATH", addEnvPath + ";" + existingPath);
                } else {
                    effectiveEnvs.put("PATH", addEnvPath);
                }
            }
            if (!effectiveEnvs.isEmpty()) {
                builder.environment(effectiveEnvs);
            }

            final WinRmTool tool = builder.build();
            if (opTimeout > 0) {
                tool.setOperationTimeout((long) opTimeout * 1000L);
            }
            if (openTimeout > 0) {
                tool.setConnectionTimeout((long) openTimeout * 1000L);
            }

            if (logWrite && verbose > 2) {
                logger.writeLine(MdlConst.LVL_NONE, "OperationTimeout = " + (opTimeout * 1000L) + " (milisec) / OpenTimeout = " + (openTimeout * 1000L) + " (milisec)");
            }

            final String invokeCommand = getInvokeCmd(command);
            if (logWrite && verbose > 4) {
                logger.writeLine(MdlConst.LVL_NONE, "[ExecuteOnce()][CP-011] TRY : コマンドの実行");
            }

            final WinRmToolResponse response = tool.executePs(invokeCommand);
            if (response.getStdOut() != null && !response.getStdOut().isEmpty()) {
                response.getStdOut().lines().forEach(line -> {
                    if (logWrite) {
                        logger.writeLine(MdlConst.LVL_NONE, line);
                    }
                    if (outputBuffer != null) {
                        outputBuffer.append(line).append(System.lineSeparator());
                    }
                    cmdStatus.checkMessageLine(line);
                });
            }
            if (response.getStdErr() != null && !response.getStdErr().isEmpty()) {
                response.getStdErr().lines().forEach(line -> {
                    if (logWrite) {
                        logger.writeLine(MdlConst.LVL_NONE, line);
                    }
                    if (outputBuffer != null) {
                        outputBuffer.append(line).append(System.lineSeparator());
                    }
                    cmdStatus.checkMessageLine(line);
                });
            }

            cmdExitCode = response.getStatusCode();
        } catch (final Exception ex) {
            cmdStatus.setMethodExitStatus(cmdStatus.getErrorCode() == MdlConst.INT_NULL ? MdlConst.LVL_E : cmdStatus.getErrorCode());
            cmdStatus.setReturnLevel(MdlConst.LVL_E);
            isException = true;
            isMoreRetry = isRetryError(ex.getMessage());
            logger.writeLine(MdlConst.LVL_NONE, "EXCEPTION : " + ex.getMessage());
            if (stackTrace) {
                logger.writeLine(MdlConst.LVL_NONE, "");
                for (final StackTraceElement ste : ex.getStackTrace()) {
                    logger.writeLine(MdlConst.LVL_NONE, ste.toString());
                }
                logger.writeLine(MdlConst.LVL_NONE, "");
            }
        } finally {
            if (logWrite && verbose > 4) {
                logger.writeLine(MdlConst.LVL_NONE, "[ExecuteOnce()][CP-021] TRY : 事後処理");
            }
            if (context != null) {
                try {
                    context.shutdown();
                } catch (final Exception ignored) {
                    // ignore
                }
            }
        }

        // コマンド終了コードの判定
        if (!isException) {
            // コマンド終了コードチェック
            cmdStatus.checkCommandExitCode(cmdExitCode);

            // メソッド戻り値の評価
            cmdStatus.evaluate();

            if (logWrite && verbose > 0) {
                logger.writeLine(MdlConst.LVL_NONE, "");
                logger.writeLine(MdlConst.LVL_NONE, "==> リモートコマンド終了コード = " + cmdExitCode + " => メソッド終了コード = " + cmdStatus.getMethodExitStatus());
            }
        }

        return cmdStatus.getReturnLevel();
    }

    /**
     * コマンドを実行し、その標準出力・エラー出力結果を文字列テキストとして取得します。
     *
     * <p><b>使用例:</b></p>
     * <pre>{@code
     * String output = winRs.execReturnText("hostname");
     * }</pre>
     *
     * @param command 実行するコマンド文字列。
     * @return コマンド実行によって得られた出力テキスト文字列。
     */
    public String execReturnText(final String command) {
        clearBuffer();
        executeOnce(command);
        return outputBuffer.toString().stripTrailing();
    }

    /**
     * 設定された実行モード（ExecMode）に応じて、リモートで実行するためのコマンド文字列（エンコードやシェルラップを含む）を生成・取得します。
     *
     * <p><b>使用例:</b></p>
     * <pre>{@code
     * String invokeCmd = winRs.getInvokeCmd("dir");
     * }</pre>
     *
     * @param remoteCommand 実行対象のリモートコマンド。
     * @return 加工・構築された実行用コマンド文字列。
     */
    public String getInvokeCmd(final String remoteCommand) {
        final String effectiveCmd = remoteCommand != null ? remoteCommand.trim() : "";
        String invokeCommand;

        switch (execMode) {
            case EXEC_MODE_NORMAL:
                invokeCommand = effectiveCmd + " 2>&1";
                break;
            case EXEC_MODE_CMD:
                invokeCommand = comSpec + " /c " + effectiveCmd + " 2>&1";
                break;
            case EXEC_MODE_EC:
                try {
                    final byte[] decoded = Base64.getDecoder().decode(effectiveCmd);
                    invokeCommand = new String(decoded, CHARSET_MS932).trim();
                } catch (final Exception e) {
                    invokeCommand = effectiveCmd;
                }
                break;
            case EXEC_MODE_ECMD:
                try {
                    final byte[] decoded = Base64.getDecoder().decode(effectiveCmd);
                    final String decodedStr = new String(decoded, CHARSET_MS932).trim();
                    invokeCommand = comSpec + " /c " + decodedStr + " 2>&1";
                } catch (final Exception e) {
                    invokeCommand = comSpec + " /c " + effectiveCmd + " 2>&1";
                }
                break;
            case EXEC_MODE_PS:
                try {
                    final byte[] unicodeBytes = effectiveCmd.getBytes(StandardCharsets.UTF_16LE);
                    final String base64Encoded = Base64.getEncoder().encodeToString(unicodeBytes);
                    invokeCommand = comSpec + " /c powershell -encodedCommand \"" + base64Encoded + "\" 2>&1";
                } catch (final Exception e) {
                    invokeCommand = comSpec + " /c powershell -encodedCommand \"" + effectiveCmd + "\" 2>&1";
                }
                break;
            case EXEC_MODE_ES:
                invokeCommand = comSpec + " /c powershell -encodedCommand \"" + effectiveCmd + "\" 2>&1";
                break;
            case EXEC_MODE_EXE:
            default:
                invokeCommand = effectiveCmd;
                break;
        }

        if (teePath != null && !teePath.isBlank()) {
            invokeCommand = invokeCommand + " | Tee-Object -FilePath " + teePath;
        }
        if (logWrite && verbose > 2) {
            logger.writeLine(MdlConst.LVL_NONE, "== INVOKE CMD  = " + invokeCommand);
        }
        return invokeCommand;
    }

    /**
     * 発生した例外メッセージが、自動再試行（リトライ）の対象となるエラー条件に該当するかどうかを判定します。
     *
     * <p><b>使用例:</b></p>
     * <pre>{@code
     * boolean retryable = winRs.isRetryError(ex.getMessage());
     * }</pre>
     *
     * @param message 検証対象の例外メッセージ文字列。
     * @return 再試行可能なエラーの場合は {@code true}、それ以外の場合は {@code false}。
     */
    public boolean isRetryError(final String message) {
        if (message == null) {
            return false;
        }
        return RETRY_ERR_REGEX.matcher(message).find();
    }

    /**
     * 内部出力保持用バッファ（{@link StringBuilder}）の内容をクリアします。
     *
     * <p><b>使用例:</b></p>
     * <pre>{@code
     * winRs.clearBuffer();
     * }</pre>
     */
    public void clearBuffer() {
        outputBuffer.setLength(0);
    }

    /**
     * 認証機構コードに応じた認証スキーム文字列を取得します。
     *
     * @param authMechanism 認証機構コード。
     * @return 認証スキーム文字列 (例: {@link AuthSchemes#BASIC}, {@link AuthSchemes#NTLM})。
     */
    private String getAuthSchemeString(final int authMechanism) {
        switch (authMechanism) {
            case 1:
                return AuthSchemes.BASIC;
            case 2:
            case 3:
                return AuthSchemes.SPNEGO;
            case 4:
                return AuthSchemes.CREDSSP;
            case 5:
                return AuthSchemes.DIGEST;
            case 6:
                return AuthSchemes.KERBEROS;
            default:
                return AuthSchemes.NTLM;
        }
    }
}
