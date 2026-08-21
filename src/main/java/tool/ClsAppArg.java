package tool;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import tool.cmnclslib.cls.ClsCmmnArgs;
import tool.cmnclslib.cls.ClsLogger;
import tool.cmnclslib.mdl.MdlArg;
import tool.cmnclslib.mdl.MdlConst;
import tool.cmnclslib.mdl.MdlUtil;

/**
 * コマンドライン引数の解析および実行時オプションパラメータを保持・管理するクラスです。
 *
 * <p>WinRM経由でのリモート実行に必要な接続情報（ホスト名、ポート番号、認証情報等）、実行モード、
 * タイムアウト値、リトライ回数、結果判定ルールなどの引数文字列を解析し、型安全に保持します。</p>
 *
 * <p><b>使用例:</b></p>
 * <pre>{@code
 * ClsLogger logger = new ClsLogger();
 * ClsAppArg appArg = new ClsAppArg(logger);
 * boolean isOk = appArg.getArgs(args);
 * if (isOk && !appArg.isUsage()) {
 *     String host = appArg.getRemoteHost();
 *     int port = appArg.getPort();
 * }
 * }</pre>
 */
public class ClsAppArg {

    /** 結果判定モード: コマンド自体の戻り値をそのまま返却します。 */
    public static final short NONE = 0;
    /** 結果判定モード: 閾値または指定コードリストに基づき成否を判定します。 */
    public static final short RETURN_CODE = 1;
    /** 結果判定モード: コマンド成否にかかわらず常に正常終了(0)と判定します。 */
    public static final short ALWAYS_NORMAL = 2;
    /** 結果判定モード: コマンド成否にかかわらず常に警告終了と判定します。 */
    public static final short ALWAYS_WARN = 3;
    /** 結果判定モード: コマンド成否にかかわらず常に異常終了と判定します。 */
    public static final short ALWAYS_ERROR = 4;

    /** 実行コマンド指定キーのリスト */
    private static final List<String> CMD_KEYS = List.of("c", "cmd", "ps", "ec", "ecmd", "es", "exe");
    /** 異常終了判定戻り値指定キーのリスト */
    private static final List<String> ERR_RET_KEYS = List.of("ng-ret", "err-ret", "error-ret");
    /** 異常終了判定メッセージ指定キーのリスト */
    private static final List<String> ERR_STR_KEYS = List.of("ng-str", "err-str", "error-str");
    /** リトライ待機秒数の最大許容値（秒） */
    private static final int MAX_RETRY_SLEEP_SEC = 3600;

    /** ログ出力用ロガー */
    private final ClsLogger logger;
    /** 共通引数解析オブジェクト */
    private final ClsCmmnArgs cmmnArgs;

    private String domain = "";
    private String userNoDomain = "";
    private String addProcEnvStr = "";
    private String exeBaseName = "";
    private String exeDir = "";
    private int pid;
    private String comSpec = "";
    private boolean usage;
    private int returnCode = MdlConst.LVL_I;
    private int verbose;
    private boolean stackTrace;
    private String workDir = "";
    private String cmdPath = "hostname";
    private String remoteHost = "localhost";
    private String teePath = "";
    private String addEnvPath = "";
    private Map<String, String> procEnvs = new LinkedHashMap<>();
    private int execMode = ClsWinRs.EXEC_MODE_CMD;
    private int port = 5985;
    private int opTimeout = 180;
    private int openTimeout = 120;
    private int retryMax;
    private int retrySleep = 5;
    private boolean retryRmtCmd;
    private String username = "";
    private String password = "";
    private boolean switchUser;
    private boolean logonAlwaysOk;
    private int authMechanism;
    private String okRetCsv = "0";
    private String warnRetCsv = "";
    private String errRetCsv = "";
    private String okMsgCsv = "";
    private String warnMsgCsv = "";
    private String errMsgCsv = "";
    private int judgeMode = NONE;
    private int warnThreshold = MdlConst.INT_NULL;
    private int errThreshold = MdlConst.INT_NULL;
    private boolean errAtNegative;
    private int errorCode = MdlConst.INT_NULL;
    private int warnCode = MdlConst.INT_NULL;
    private boolean echoRetcode;
    private boolean ajsJob;
    private String hostname = "";
    private int maxHops = 3;
    private int curHops;
    private boolean loopCheck = true;

    /**
     * ロガーを指定して {@link ClsAppArg} の新しいインスタンスを生成します。
     *
     * <p><b>使用例:</b></p>
     * <pre>{@code
     * ClsLogger logger = new ClsLogger();
     * ClsAppArg appArg = new ClsAppArg(logger);
     * }</pre>
     *
     * @param logger ログ出力を行うためのロガーオブジェクト。
     */
    public ClsAppArg(final ClsLogger logger) {
        this.logger = logger;
        this.cmmnArgs = new ClsCmmnArgs(this.logger);
        this.cmmnArgs.getModuleInfo("");
        this.exeDir = this.cmmnArgs.getExeDir();
        this.exeBaseName = this.cmmnArgs.getExeBaseName();
        this.pid = (int) this.cmmnArgs.getPid();

        final String comSpecEnv = System.getenv("ComSpec");
        this.comSpec = (comSpecEnv != null && !comSpecEnv.isBlank()) ? comSpecEnv : "cmd";

        String machine = System.getenv("COMPUTERNAME");
        if (machine == null || machine.isBlank()) {
            try {
                machine = InetAddress.getLocalHost().getHostName();
            } catch (final UnknownHostException e) {
                machine = "";
            }
        }
        this.hostname = machine != null ? machine : "";
    }

    /**
     * 実行モジュールのベースファイル名を取得します。
     *
     * @return 実行モジュールのベース名。
     */
    public String getExeBaseName() {
        return exeBaseName;
    }

    /**
     * 実行モジュールのベースファイル名を設定します。
     *
     * @param exeBaseName 実行モジュールのベース名。
     */
    public void setExeBaseName(final String exeBaseName) {
        this.exeBaseName = exeBaseName != null ? exeBaseName : "";
    }

    /**
     * 実行モジュールが存在するディレクトリパスを取得します。
     *
     * @return 実行モジュールのディレクトリパス。
     */
    public String getExeDir() {
        return exeDir;
    }

    /**
     * 実行モジュールのディレクトリパスを設定します。
     *
     * @param exeDir 実行モジュールのディレクトリパス。
     */
    public void setExeDir(final String exeDir) {
        this.exeDir = exeDir != null ? exeDir : "";
    }

    /**
     * 自プロセスのプロセスID (PID) を取得します。
     *
     * @return プロセスID。
     */
    public int getPid() {
        return pid;
    }

    /**
     * 自プロセスのプロセスID (PID) を設定します。
     *
     * @param pid プロセスID。
     */
    public void setPid(final int pid) {
        this.pid = pid;
    }

    /**
     * コマンドインタプリタの実行ファイル名 (ComSpec) を取得します。
     *
     * @return コマンドインタプリタパス。
     */
    public String getComSpec() {
        return comSpec;
    }

    /**
     * コマンドインタプリタの実行ファイル名を設定します。
     *
     * @param comSpec コマンドインタプリタパス。
     */
    public void setComSpec(final String comSpec) {
        this.comSpec = comSpec != null ? comSpec : "";
    }

    /**
     * Usage（ヘルプ）表示要求フラグが有効か判定します。
     *
     * @return Usage表示要求がある場合は {@code true}、それ以外は {@code false}。
     */
    public boolean isUsage() {
        return usage;
    }

    /**
     * Usage（ヘルプ）表示要求フラグを設定します。
     *
     * @param usage Usage表示要求フラグ。
     */
    public void setUsage(final boolean usage) {
        this.usage = usage;
    }

    /**
     * アプリケーションの終了ステータスコードを取得します。
     *
     * @return 終了ステータスコード。
     */
    public int getReturnCode() {
        return returnCode;
    }

    /**
     * アプリケーションの終了ステータスコードを設定します。
     *
     * @param returnCode 終了ステータスコード。
     */
    public void setReturnCode(final int returnCode) {
        this.returnCode = returnCode;
    }

    /**
     * 詳細ログ出力レベル (Verbose) を取得します。
     *
     * @return 詳細ログ出力レベル数値。
     */
    public int getVerbose() {
        return verbose;
    }

    /**
     * 詳細ログ出力レベル (Verbose) を設定します。
     *
     * @param verbose 詳細ログ出力レベル数値。
     */
    public void setVerbose(final int verbose) {
        this.verbose = verbose;
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
     * 実行対象のコマンド文字列を取得します。
     *
     * @return 実行コマンド文字列。
     */
    public String getCmdPath() {
        return cmdPath;
    }

    /**
     * 実行対象のコマンド文字列を設定します。
     *
     * @param cmdPath 実行コマンド文字列。
     */
    public void setCmdPath(final String cmdPath) {
        this.cmdPath = cmdPath != null ? cmdPath : "";
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
     * @return 実行モードコード (例: {@link ClsWinRs#EXEC_MODE_CMD})。
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
     * WinRM接続先ポート番号を取得します。
     *
     * @return ポート番号。
     */
    public int getPort() {
        return port;
    }

    /**
     * WinRM接続先ポート番号を設定します。
     *
     * @param port ポート番号。
     */
    public void setPort(final int port) {
        this.port = port;
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
     * コマンド実行の最大リトライ回数を取得します。
     *
     * @return 最大リトライ回数。
     */
    public int getRetryMax() {
        return retryMax;
    }

    /**
     * コマンド実行の最大リトライ回数を設定します。
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
     * ドメイン部分を含まない純粋なユーザー名を取得します。
     *
     * @return ドメインなしユーザー名。
     */
    public String getUserNoDomain() {
        return userNoDomain;
    }

    /**
     * ドメイン部分を含まない純粋なユーザー名を設定します。
     *
     * @param userNoDomain ドメインなしユーザー名。
     */
    public void setUserNoDomain(final String userNoDomain) {
        this.userNoDomain = userNoDomain != null ? userNoDomain : "";
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
     * ユーザー切り替え実行フラグを取得します。
     *
     * @return ユーザー切り替えを行う場合は {@code true}。
     */
    public boolean isSwitchUser() {
        return switchUser;
    }

    /**
     * ユーザー切り替え実行フラグを設定します。
     *
     * @param switchUser ユーザー切り替え実行フラグ。
     */
    public void setSwitchUser(final boolean switchUser) {
        this.switchUser = switchUser;
    }

    /**
     * ログオン試行を常に正常扱いとするフラグを取得します。
     *
     * @return ログオン常時成功フラグ。
     */
    public boolean isLogonAlwaysOk() {
        return logonAlwaysOk;
    }

    /**
     * ログオン試行を常に正常扱いとするフラグを設定します。
     *
     * @param logonAlwaysOk ログオン常時成功フラグ。
     */
    public void setLogonAlwaysOk(final boolean logonAlwaysOk) {
        this.logonAlwaysOk = logonAlwaysOk;
    }

    /**
     * 認証機構コードを取得します。
     *
     * @return 認証機構コード (0: Default, 1: Basic, 2: Negotiate 等)。
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
     * 正常終了判定用の戻り値CSVリストを取得します。
     *
     * @return 戻り値CSV文字列。
     */
    public String getOkRetCsv() {
        return okRetCsv;
    }

    /**
     * 正常終了判定用の戻り値CSVリストを設定します。
     *
     * @param okRetCsv 戻り値CSV文字列。
     */
    public void setOkRetCsv(final String okRetCsv) {
        this.okRetCsv = okRetCsv != null ? okRetCsv : "";
    }

    /**
     * 警告終了判定用の戻り値CSVリストを取得します。
     *
     * @return 戻り値CSV文字列。
     */
    public String getWarnRetCsv() {
        return warnRetCsv;
    }

    /**
     * 警告終了判定用の戻り値CSVリストを設定します。
     *
     * @param warnRetCsv 戻り値CSV文字列。
     */
    public void setWarnRetCsv(final String warnRetCsv) {
        this.warnRetCsv = warnRetCsv != null ? warnRetCsv : "";
    }

    /**
     * 異常終了判定用の戻り値CSVリストを取得します。
     *
     * @return 戻り値CSV文字列。
     */
    public String getErrRetCsv() {
        return errRetCsv;
    }

    /**
     * 異常終了判定用の戻り値CSVリストを設定します。
     *
     * @param errRetCsv 戻り値CSV文字列。
     */
    public void setErrRetCsv(final String errRetCsv) {
        this.errRetCsv = errRetCsv != null ? errRetCsv : "";
    }

    /**
     * 正常終了判定用の標準出力メッセージCSVリストを取得します。
     *
     * @return メッセージCSV文字列。
     */
    public String getOkMsgCsv() {
        return okMsgCsv;
    }

    /**
     * 正常終了判定用の標準出力メッセージCSVリストを設定します。
     *
     * @param okMsgCsv メッセージCSV文字列。
     */
    public void setOkMsgCsv(final String okMsgCsv) {
        this.okMsgCsv = okMsgCsv != null ? okMsgCsv : "";
    }

    /**
     * 警告終了判定用の標準出力メッセージCSVリストを取得します。
     *
     * @return メッセージCSV文字列。
     */
    public String getWarnMsgCsv() {
        return warnMsgCsv;
    }

    /**
     * 警告終了判定用の標準出力メッセージCSVリストを設定します。
     *
     * @param warnMsgCsv メッセージCSV文字列。
     */
    public void setWarnMsgCsv(final String warnMsgCsv) {
        this.warnMsgCsv = warnMsgCsv != null ? warnMsgCsv : "";
    }

    /**
     * 異常終了判定用の標準出力メッセージCSVリストを取得します。
     *
     * @return メッセージCSV文字列。
     */
    public String getErrMsgCsv() {
        return errMsgCsv;
    }

    /**
     * 異常終了判定用の標準出力メッセージCSVリストを設定します。
     *
     * @param errMsgCsv メッセージCSV文字列。
     */
    public void setErrMsgCsv(final String errMsgCsv) {
        this.errMsgCsv = errMsgCsv != null ? errMsgCsv : "";
    }

    /**
     * 結果判定モード (JudgeMode) を取得します。
     *
     * @return 判定モード ({@link #NONE}, {@link #RETURN_CODE}, {@link #ALWAYS_NORMAL} 等)。
     */
    public int getJudgeMode() {
        return judgeMode;
    }

    /**
     * 結果判定モード (JudgeMode) を設定します。
     *
     * @param judgeMode 判定モード。
     */
    public void setJudgeMode(final int judgeMode) {
        this.judgeMode = judgeMode;
    }

    /**
     * 警告終了判定の閾値を取得します。
     *
     * @return 警告閾値。
     */
    public int getWarnThreshold() {
        return warnThreshold;
    }

    /**
     * 警告終了判定の閾値を設定します。
     *
     * @param warnThreshold 警告閾値。
     */
    public void setWarnThreshold(final int warnThreshold) {
        this.warnThreshold = warnThreshold;
    }

    /**
     * 異常終了判定の閾値を取得します。
     *
     * @return 異常閾値。
     */
    public int getErrThreshold() {
        return errThreshold;
    }

    /**
     * 異常終了判定の閾値を設定します。
     *
     * @param errThreshold 異常閾値。
     */
    public void setErrThreshold(final int errThreshold) {
        this.errThreshold = errThreshold;
    }

    /**
     * 負の終了コードを異常として扱うかどうかを取得します。
     *
     * @return 負値を異常とする場合は {@code true}。
     */
    public boolean isErrAtNegative() {
        return errAtNegative;
    }

    /**
     * 負の終了コードを異常として扱うかどうかのフラグを設定します。
     *
     * @param errAtNegative 負値異常フラグ。
     */
    public void setErrAtNegative(final boolean errAtNegative) {
        this.errAtNegative = errAtNegative;
    }

    /**
     * 異常終了時に返却するカスタム終了コードを取得します。
     *
     * @return 異常終了コード。
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * 異常終了時に返却するカスタム終了コードを設定します。
     *
     * @param errorCode 異常終了コード。
     */
    public void setErrorCode(final int errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * 警告終了時に返却するカスタム終了コードを取得します。
     *
     * @return 警告終了コード。
     */
    public int getWarnCode() {
        return warnCode;
    }

    /**
     * 警告終了時に返却するカスタム終了コードを設定します。
     *
     * @param warnCode 警告終了コード。
     */
    public void setWarnCode(final int warnCode) {
        this.warnCode = warnCode;
    }

    /**
     * 終了時に終了コードを標準出力へエコーするかどうかを取得します。
     *
     * @return 終了コードエコーが有効な場合は {@code true}。
     */
    public boolean isEchoRetcode() {
        return echoRetcode;
    }

    /**
     * 終了コードエコーフラグを設定します。
     *
     * @param echoRetcode 終了コードエコーフラグ。
     */
    public void setEchoRetcode(final boolean echoRetcode) {
        this.echoRetcode = echoRetcode;
    }

    /**
     * JP1/AJS ジョブ環境下での実行かどうかを取得します。
     *
     * @return JP1/AJSジョブの場合は {@code true}。
     */
    public boolean isAjsJob() {
        return ajsJob;
    }

    /**
     * JP1/AJS ジョブフラグを設定します。
     *
     * @param ajsJob JP1/AJSジョブフラグ。
     */
    public void setAjsJob(final boolean ajsJob) {
        this.ajsJob = ajsJob;
    }

    /**
     * 実行マシンのホスト名を取得します。
     *
     * @return ホスト名。
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * 実行マシンのホスト名を設定します。
     *
     * @param hostname ホスト名。
     */
    public void setHostname(final String hostname) {
        this.hostname = hostname != null ? hostname : "";
    }

    /**
     * リモート多段実行時の最大許容ホップ数を取得します。
     *
     * @return 最大ホップ数。
     */
    public int getMaxHops() {
        return maxHops;
    }

    /**
     * リモート多段実行時の最大許容ホップ数を設定します。
     *
     * @param maxHops 最大ホップ数。
     */
    public void setMaxHops(final int maxHops) {
        this.maxHops = maxHops;
    }

    /**
     * 現在のリモート実行ホップ数を取得します。
     *
     * @return 現在ホップ数。
     */
    public int getCurHops() {
        return curHops;
    }

    /**
     * 現在のリモート実行ホップ数を設定します。
     *
     * @param curHops 現在ホップ数。
     */
    public void setCurHops(final int curHops) {
        this.curHops = curHops;
    }

    /**
     * 接続無限ループ検出を行うかどうかを取得します。
     *
     * @return ループ検出を行う場合は {@code true}。
     */
    public boolean isLoopCheck() {
        return loopCheck;
    }

    /**
     * 接続無限ループ検出フラグを設定します。
     *
     * @param loopCheck ループ検出フラグ。
     */
    public void setLoopCheck(final boolean loopCheck) {
        this.loopCheck = loopCheck;
    }

    /**
     * コマンドライン引数の配列を解析し、本クラスの各プロパティに値を設定します。
     *
     * <p><b>使用例:</b></p>
     * <pre>{@code
     * String[] args = {"-h", "server01", "-u", "admin", "-p", "secret"};
     * boolean ok = appArg.getArgs(args);
     * if (!ok) {
     *     System.err.println("引数が不正です");
     * }
     * }</pre>
     *
     * @param args コマンドライン引数の配列。
     * @return 引数の解析および必須パラメータの検証が正常に完了した場合は {@code true}、失敗した場合は {@code false}。
     */
    public boolean getArgs(final String[] args) {
        boolean isOk;

        // -----------------------------------------------------------------
        // ClsCmmnParams初期値設定
        // -----------------------------------------------------------------
        cmmnArgs.setRetryMax(retryMax);
        cmmnArgs.setRetrySleep(retrySleep);

        // -----------------------------------------------------------------
        // ClsCmmnParams処理
        // -----------------------------------------------------------------
        final Map<String, String> namedArgs = MdlArg.getNamedArgs(args);
        cmmnArgs.setNamedArgs(namedArgs);
        isOk = cmmnArgs.getCommonArgs();

        // -----------------------------------------------------------------
        // ClsCmmnParams引数取得：ETC
        // -----------------------------------------------------------------
        usage = cmmnArgs.isUsage();
        verbose = cmmnArgs.getVerbose();
        stackTrace = cmmnArgs.isStackTrace();
        ajsJob = cmmnArgs.isAjsJob();
        retryMax = cmmnArgs.getRetryMax();
        retrySleep = Math.min(cmmnArgs.getRetrySleep(), MAX_RETRY_SLEEP_SEC);

        // -----------------------------------------------------------------
        // ClsCmmnParams引数取得：認証情報
        // -----------------------------------------------------------------
        isOk = cmmnArgs.getArgsForAuth();
        final String domainName = cmmnArgs.getDomainName();
        domain = domainName != null ? domainName : "";
        username = cmmnArgs.getUsername();
        userNoDomain = cmmnArgs.getUserNoDomain();
        password = cmmnArgs.getPassword();
        logonAlwaysOk = cmmnArgs.isLogonAlwaysOk();
        switchUser = cmmnArgs.isSwitchUser();

        // -----------------------------------------------------------------
        // Basic Option：
        // -----------------------------------------------------------------
        if (cmmnArgs.getHost() != null && !cmmnArgs.getHost().isBlank()) {
            remoteHost = cmmnArgs.getHost();
            if (ajsJob && cmmnArgs.getJp1() != null) {
                remoteHost = cmmnArgs.getJp1().convertFromEnv(remoteHost);
            }
        } else {
            remoteHost = "localhost";
        }

        // -port port          ：WinRMポート番号
        if (MdlArg.containsKey(namedArgs, "port")) {
            final String tempStr = MdlArg.getValue(namedArgs, "port");
            if (tempStr != null && !tempStr.isBlank()) {
                final int tempInt = MdlUtil.parseInt(tempStr, MdlConst.INT_NULL);
                if (tempInt != MdlConst.INT_NULL) {
                    port = tempInt;
                }
            }
        }

        // -d domain / -domain domain
        if (MdlArg.containsKey(namedArgs, "d")) {
            final String tempStr = MdlArg.getValue(namedArgs, "d");
            if (tempStr != null && !tempStr.isBlank()) {
                domain = tempStr.trim();
            }
        } else if (MdlArg.containsKey(namedArgs, "domain")) {
            final String tempStr = MdlArg.getValue(namedArgs, "domain");
            if (tempStr != null && !tempStr.isBlank()) {
                domain = tempStr.trim();
            }
        }

        // -u username         ：認証ユーザー名
        if (userNoDomain != null && !userNoDomain.isBlank()) {
            final int slashIdx = userNoDomain.indexOf('\\');
            final int atIdx = userNoDomain.indexOf('@');
            if (slashIdx > 0) {
                domain = userNoDomain.substring(0, slashIdx).trim();
                userNoDomain = userNoDomain.substring(slashIdx + 1).trim();
                username = domain + "\\" + userNoDomain;
            } else if (atIdx > 0) {
                domain = userNoDomain.substring(atIdx + 1).trim();
                userNoDomain = userNoDomain.substring(0, atIdx).trim();
                username = domain + "\\" + userNoDomain;
            } else if (!domain.isEmpty()) {
                username = domain + "\\" + userNoDomain;
            }
        } else if (!domain.isEmpty()) {
            username = domain + "\\" + userNoDomain;
        }

        // -p password         ：認証パスワード
        if (password == null || password.isBlank()) {
            logger.writeLine(MdlConst.LVL_NONE, "Invalid Argument -p password   : 認証パスワード（現状値：" + password + "）");
            isOk = false;
        }

        // -cwd path           ：WORKING DIR
        if (MdlArg.containsKey(namedArgs, "cwd")) {
            final String tempStr = MdlArg.getValue(namedArgs, "cwd");
            if (tempStr != null && !tempStr.isBlank()) {
                workDir = tempStr;
            }
        }

        // -c|cmd cmd / -ps cmdlet / -ecmd encoded / -es encoded など
        for (final String key : CMD_KEYS) {
            if (MdlArg.containsKey(namedArgs, key)) {
                final String tempStr = MdlArg.getValue(namedArgs, key);
                if (tempStr != null && !tempStr.isBlank()) {
                    final String lowerKey = key.toLowerCase(Locale.ROOT);
                    if ("cmd".equals(lowerKey)) {
                        execMode = ClsWinRs.EXEC_MODE_CMD;
                    } else if ("ps".equals(lowerKey)) {
                        execMode = ClsWinRs.EXEC_MODE_PS;
                    } else if ("ec".equals(lowerKey)) {
                        execMode = ClsWinRs.EXEC_MODE_EC;
                    } else if ("ecmd".equals(lowerKey)) {
                        execMode = ClsWinRs.EXEC_MODE_ECMD;
                    } else if ("es".equals(lowerKey)) {
                        execMode = ClsWinRs.EXEC_MODE_ES;
                    } else if ("exe".equals(lowerKey)) {
                        execMode = ClsWinRs.EXEC_MODE_EXE;
                    } else {
                        execMode = ClsWinRs.EXEC_MODE_NORMAL;
                    }
                    cmdPath = tempStr.trim();
                    break;
                }
            }
        }

        // -rtee path          ：tee-objectファイルパス
        if (MdlArg.containsKey(namedArgs, "rtee")) {
            final String tempStr = MdlArg.getValue(namedArgs, "rtee");
            if (tempStr != null && !tempStr.isBlank()) {
                teePath = tempStr;
            }
        }

        // -optimeout int      ：Operation Timeout
        if (MdlArg.containsKey(namedArgs, "optimeout")) {
            final String tempStr = MdlArg.getValue(namedArgs, "optimeout");
            if (tempStr != null && !tempStr.isBlank()) {
                final int tempInt = MdlUtil.parseInt(tempStr, MdlConst.INT_NULL);
                if (tempInt != MdlConst.INT_NULL) {
                    opTimeout = tempInt;
                }
            }
        }

        // -opentimeout int    ：Open timeout
        if (MdlArg.containsKey(namedArgs, "opentimeout")) {
            final String tempStr = MdlArg.getValue(namedArgs, "opentimeout");
            if (tempStr != null && !tempStr.isBlank()) {
                final int tempInt = MdlUtil.parseInt(tempStr, MdlConst.INT_NULL);
                if (tempInt != MdlConst.INT_NULL) {
                    openTimeout = tempInt;
                }
            }
        }

        // -am num
        if (MdlArg.containsKey(namedArgs, "am")) {
            final String tempStr = MdlArg.getValue(namedArgs, "am");
            if (tempStr != null && !tempStr.isBlank()) {
                final int tempInt = MdlUtil.parseInt(tempStr, MdlConst.INT_NULL);
                if (tempInt != MdlConst.INT_NULL) {
                    authMechanism = tempInt;
                }
            }
        }

        // -retry-rmtcmd       ：リモートコマンドリトライフラグ
        if (MdlArg.containsKey(namedArgs, "retry-rmtcmd")) {
            retryRmtCmd = true;
        }

        // -penv n1=v1,n2=v2   ：環境変数([,|]区切り）
        if (MdlArg.containsKey(namedArgs, "penv")) {
            final String tempStr = MdlArg.getValue(namedArgs, "penv");
            if (tempStr != null && !tempStr.isBlank()) {
                procEnvs = MdlUtil.parseCsvToMap(procEnvs, tempStr, "[,|]", "=", verbose, false, false);
                addProcEnvStr = tempStr;
            }
        }

        // -add-penv-path path ：環境変数PATH先頭追加内容
        if (MdlArg.containsKey(namedArgs, "add-penv-path")) {
            final String tempStr = MdlArg.getValue(namedArgs, "add-penv-path");
            if (tempStr != null && !tempStr.isBlank()) {
                addEnvPath = tempStr;
            }
        }

        // -hop num            ：最大ホップ数（0=無効化）
        if (MdlArg.containsKey(namedArgs, "hop")) {
            final String tempStr = MdlArg.getValue(namedArgs, "hop");
            if (tempStr != null && !tempStr.isBlank()) {
                final int tempInt = MdlUtil.parseInt(tempStr, MdlConst.INT_NULL);
                if (tempInt != MdlConst.INT_NULL) {
                    maxHops = tempInt;
                }
            }
        }

        // -nlc                ：ループ検出無効化フラグ
        if (MdlArg.containsKey(namedArgs, "nlc")) {
            loopCheck = false;
        }

        // -----------------------------------------------------------------
        // Option Result Judgement：
        // -----------------------------------------------------------------
        boolean hasRet = false;
        if (MdlArg.containsKey(namedArgs, "ret")) {
            final String tempStr = MdlArg.getValue(namedArgs, "ret");
            if (tempStr != null && !tempStr.isBlank()) {
                hasRet = true;
                final String lowerRet = tempStr.toLowerCase(Locale.ROOT);
                if ("0".equals(lowerRet) || "normal".equals(lowerRet) || "always_normal".equals(lowerRet)) {
                    judgeMode = ALWAYS_NORMAL;
                } else if ("10".equals(lowerRet) || "warn".equals(lowerRet) || "always_warn".equals(lowerRet)) {
                    judgeMode = ALWAYS_WARN;
                } else if ("20".equals(lowerRet) || "error".equals(lowerRet) || "always_error".equals(lowerRet)) {
                    judgeMode = ALWAYS_ERROR;
                } else if ("ret".equals(lowerRet) || "retcode".equals(lowerRet) || "return_code".equals(lowerRet)) {
                    judgeMode = RETURN_CODE;
                } else {
                    judgeMode = NONE;
                }
            }
        }
        if (!hasRet) {
            judgeMode = NONE;
        }
        if (RETURN_CODE == judgeMode) {
            errorCode = (errorCode == MdlConst.INT_NULL ? MdlConst.LVL_E : errorCode);
            warnCode = (warnCode == MdlConst.INT_NULL ? MdlConst.LVL_W : warnCode);
        }

        // -w 数字             ：警告終了閾値
        if (MdlArg.containsKey(namedArgs, "w")) {
            final String tempStr = MdlArg.getValue(namedArgs, "w");
            if (tempStr != null && !tempStr.isBlank()) {
                final int tempInt = MdlUtil.parseInt(tempStr, MdlConst.INT_NULL);
                if (tempInt != MdlConst.INT_NULL) {
                    warnThreshold = tempInt;
                }
            }
        }

        // -e 数字             ：異常終了閾値
        if (MdlArg.containsKey(namedArgs, "e")) {
            final String tempStr = MdlArg.getValue(namedArgs, "e");
            if (tempStr != null && !tempStr.isBlank()) {
                final int tempInt = MdlUtil.parseInt(tempStr, MdlConst.INT_NULL);
                if (tempInt != MdlConst.INT_NULL) {
                    errThreshold = tempInt;
                }
            }
        }

        // -negative           ：負値のエラー判定有無
        if (MdlArg.containsKey(namedArgs, "negative")) {
            errAtNegative = true;
        }

        // -warn 数字          ：警告時の終了コード
        if (MdlArg.containsKey(namedArgs, "warn")) {
            final String tempStr = MdlArg.getValue(namedArgs, "warn");
            if (tempStr != null && !tempStr.isBlank()) {
                final int tempInt = MdlUtil.parseInt(tempStr, MdlConst.INT_NULL);
                if (tempInt != MdlConst.INT_NULL) {
                    warnCode = tempInt;
                }
            }
        }

        // -err 数字           ：異常終了時の終了コード
        if (MdlArg.containsKey(namedArgs, "err")) {
            final String tempStr = MdlArg.getValue(namedArgs, "err");
            if (tempStr != null && !tempStr.isBlank()) {
                final int tempInt = MdlUtil.parseInt(tempStr, MdlConst.INT_NULL);
                if (tempInt != MdlConst.INT_NULL) {
                    errorCode = tempInt;
                }
            }
        }

        // -ok-ret 数字,数値   ：正常終了判定戻り値リスト
        if (MdlArg.containsKey(namedArgs, "ok-ret")) {
            final String tempStr = MdlArg.getValue(namedArgs, "ok-ret");
            if (tempStr != null && !tempStr.isBlank()) {
                okRetCsv = tempStr.trim();
            }
        }

        // -warn-ret 数字,数値 ：警告終了判定戻り値リスト
        if (MdlArg.containsKey(namedArgs, "warn-ret")) {
            final String tempStr = MdlArg.getValue(namedArgs, "warn-ret");
            if (tempStr != null && !tempStr.isBlank()) {
                warnRetCsv = tempStr.trim();
            }
        }

        // -ng-ret 数字,数値 ：異常終了判定戻り値リスト
        for (final String key : ERR_RET_KEYS) {
            if (MdlArg.containsKey(namedArgs, key)) {
                final String tempStr = MdlArg.getValue(namedArgs, key);
                if (tempStr != null && !tempStr.isBlank()) {
                    errRetCsv = tempStr.trim();
                    break;
                }
            }
        }

        // -ok-str 文字列      ：正常終了判定出力文字列
        if (MdlArg.containsKey(namedArgs, "ok-str")) {
            final String tempStr = MdlArg.getValue(namedArgs, "ok-str");
            if (tempStr != null && !tempStr.isBlank()) {
                okMsgCsv = tempStr.trim();
            }
        }

        // -warn-str 文字列    ：警告終了判定出力文字列
        if (MdlArg.containsKey(namedArgs, "warn-str")) {
            final String tempStr = MdlArg.getValue(namedArgs, "warn-str");
            if (tempStr != null && !tempStr.isBlank()) {
                warnMsgCsv = tempStr.trim();
            }
        }

        // -ng-str 文字列      ：異常終了判定出力文字列
        for (final String key : ERR_STR_KEYS) {
            if (MdlArg.containsKey(namedArgs, key)) {
                final String tempStr = MdlArg.getValue(namedArgs, key);
                if (tempStr != null && !tempStr.isBlank()) {
                    errMsgCsv = tempStr.trim();
                    break;
                }
            }
        }

        // -echo-retcd         ：終了コード表示フラグ
        if (MdlArg.containsKey(namedArgs, "echo-retcd")) {
            echoRetcode = true;
        }

        // -----------------------------------------------------------------
        // 掃除
        // -----------------------------------------------------------------
        namedArgs.clear();

        return isOk;
    }

    /**
     * 設定されているJP1ジョブ名を取得します。
     *
     * <p><b>使用例:</b></p>
     * <pre>{@code
     * String jobName = appArg.getJobName();
     * }</pre>
     *
     * @return JP1ジョブ名を表す文字列。未設定の場合は空文字列。
     */
    public String getJobName() {
        if (cmmnArgs != null && cmmnArgs.getJp1() != null) {
            final String jobName = cmmnArgs.getJp1().getJobName();
            return jobName != null ? jobName : "";
        }
        return "";
    }

    /**
     * 指定された判定モードに対応する結果判定の説明テキストを取得します。
     *
     * <p><b>使用例:</b></p>
     * <pre>{@code
     * String desc = appArg.getJudgeText(ClsAppArg.RETURN_CODE);
     * }</pre>
     *
     * @param mode 判定モード数値 ({@link #NONE}, {@link #RETURN_CODE}, {@link #ALWAYS_NORMAL}, {@link #ALWAYS_WARN}, {@link #ALWAYS_ERROR})。
     * @return 判定モードの説明を表す日本語テキスト。
     */
    public String getJudgeText(final int mode) {
        switch (mode) {
            case RETURN_CODE:
                return "retcode：閾値による判定";
            case ALWAYS_NORMAL:
                return "nomal：常に正常終了";
            case ALWAYS_WARN:
                return "warn：常に警告終了";
            case ALWAYS_ERROR:
                return "error：常に異常終了";
            default:
                return "none：コマンドの戻り値を返却";
        }
    }

    /**
     * コマンドライン引数の使用方法（Usage）および各オプションの現在値をログに出力します。
     *
     * <p><b>使用例:</b></p>
     * <pre>{@code
     * if (appArg.isUsage()) {
     *     appArg.usage();
     * }
     * }</pre>
     */
    public void usage() {
        logger.writeLine(MdlConst.LVL_NONE, "");
        logger.writeLine(MdlConst.LVL_NONE, "Usage : " + exeDir + File.separator + exeBaseName + ".exe [Option] [Option]...");
        logger.writeLine(MdlConst.LVL_NONE, "");
        logger.writeLine(MdlConst.LVL_NONE, "Option ：");
        logger.writeLine(MdlConst.LVL_NONE, "   -h hostname         ：リモートホスト （現状値=" + remoteHost + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -port port          ：WinRMポート番号（現状値=" + port + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -u username         ：認証ユーザー名 （現状値=" + username + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -p password         ：認証パスワード （現状値=" + password + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -cwd path           ：WORKING DIR    （現在値=" + workDir + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -c|cmd cmd          ：DOSコマンド    （現状値=" + cmdPath + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -ps cmdlet          ：Powershellコマンドレット");
        logger.writeLine(MdlConst.LVL_NONE, "   -ecmd encoded       ：BASE64 DOSコマンド");
        logger.writeLine(MdlConst.LVL_NONE, "   -es encoded         ：BASE64 Powershellコマンドレット");
        logger.writeLine(MdlConst.LVL_NONE, "   -rtee path          ：tee-objectファイルパス    （現在値=" + teePath + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -log path           ：ログ出力ファイルパス");
        logger.writeLine(MdlConst.LVL_NONE, "   -ldir path          ：ログ出力ディレクトリパス");
        logger.writeLine(MdlConst.LVL_NONE, "   -optimeout int      ：Operation Timeout         （現在値=" + opTimeout + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -opentimeout int    ：Open timeout              （現在値=" + openTimeout + "）");
        logger.writeLine(MdlConst.LVL_NONE, "Option ：Decode Password");
        logger.writeLine(MdlConst.LVL_NONE, "   -def path           ：アカウント設定ファイルパス");
        logger.writeLine(MdlConst.LVL_NONE, "   -ep password        ：認証暗号化パスワード");
        logger.writeLine(MdlConst.LVL_NONE, "   -key key            ：暗号鍵");
        logger.writeLine(MdlConst.LVL_NONE, "   -size 128|256       ：鍵長");
        logger.writeLine(MdlConst.LVL_NONE, "   -am num             ：0:Default / 1:Basic 2:Negotiate / 3:NegotiateWithImplicitCredential / 4:Credssp / 5:Digest / 6:Kerberos（現状値=" + authMechanism + "）");
        logger.writeLine(MdlConst.LVL_NONE, "Option ：Retry");
        logger.writeLine(MdlConst.LVL_NONE, "   -retry num          ：リトライ回数（現状値=" + retryMax + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -sleep sec          ：待ち秒数    （現状値=" + retrySleep + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -retry-rmtcmd       ：リモートコマンドリトライ  （現状値=" + retryRmtCmd + "）");
        logger.writeLine(MdlConst.LVL_NONE, "Option ：Process Environment");
        logger.writeLine(MdlConst.LVL_NONE, "   -penv n1=v1,n2=v2   ：環境変数([,|]区切り）     （現状値=" + addProcEnvStr + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -add-penv-path path ：環境変数PATH先頭追加内容  （現状値=" + addEnvPath + "）");
        logger.writeLine(MdlConst.LVL_NONE, "Option ：Hops");
        logger.writeLine(MdlConst.LVL_NONE, "   -hop num            ：最大ホップ数（0=無効化）  （現在値=" + maxHops + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -nlc                ：ループ検出無効化フラグ    （現在値=" + !loopCheck + "）");
        logger.writeLine(MdlConst.LVL_NONE, "Option Result Judgement：");
        logger.writeLine(MdlConst.LVL_NONE, "   -ret none|retcode   ：CMD戻り値判定有無         （現在値=" + getJudgeText(judgeMode) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "Option Result Judgement with Retcode：");
        logger.writeLine(MdlConst.LVL_NONE, "   -w 数字             ：警告終了閾値              （現在値=" + (warnThreshold == MdlConst.INT_NULL ? "" : String.valueOf(warnThreshold)) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -e 数字             ：異常終了閾値              （現在値=" + (errThreshold == MdlConst.INT_NULL ? "" : String.valueOf(errThreshold)) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -negative           ：負値のエラー判定有無      （現在値=" + errAtNegative + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -warn 数字          ：警告時の終了コード        （現在値=" + (warnCode == MdlConst.INT_NULL ? "" : String.valueOf(warnCode)) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -err 数字           ：エラー時の終了コード      （現在値=" + (errorCode == MdlConst.INT_NULL ? "" : String.valueOf(errorCode)) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -ok-ret 数字,数値   ：正常終了判定戻り値リスト  （現在値=" + okRetCsv + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -warn-ret 数字,数値 ：警告終了判定戻り値リスト  （現在値=" + warnRetCsv + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -ng-ret 数字,数値   ：異常終了判定戻り値リスト  （現在値=" + errRetCsv + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -ok-str 文字列      ：正常終了判定出力文字列    （現在値=" + okMsgCsv + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -warn-str 文字列    ：警告終了判定出力文字列    （現在値=" + warnMsgCsv + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -ng-str 文字列      ：異常終了判定出力文字列    （現在値=" + errMsgCsv + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -echo-retcd         ：終了コード表示フラグ      （現在値=" + echoRetcode + "）");
        logger.writeLine(MdlConst.LVL_NONE, "");
        logger.writeLine(MdlConst.LVL_NONE, "Exit code              ：リモート実行コマンドの戻り値（-ret指定時を除く）");
        logger.writeLine(MdlConst.LVL_NONE, "");
    }
}
