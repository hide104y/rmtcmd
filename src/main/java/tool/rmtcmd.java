package tool;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

import tool.cmnclslib.cls.ClsLogger;
import tool.cmnclslib.mdl.MdlConst;
import tool.cmnclslib.mdl.MdlDate;
import tool.cmnclslib.mdl.MdlUtil;

/**
 * リモートコマンド実行ツールのメインエントリーポイントクラスです。
 *
 * <p>コマンドライン引数を解析し、指定されたリモートホストに対して WinRM / WinRS を用いた
 * リモートコマンド実行、結果ステータス評価、および終了コードの返却を行います。</p>
 *
 * <p><b>使用例:</b></p>
 * <pre>{@code
 * String[] args = {"-h", "remoteHost", "-u", "admin", "-p", "password", "-cmd", "hostname"};
 * int exitCode = rmtcmd.run(args);
 * System.exit(exitCode);
 * }</pre>
 */
public final class rmtcmd {

    /**
     * ユーティリティクラスのため、外部からのインスタンス化を禁止します。
     */
    private rmtcmd() {
    }

    /**
     * アプリケーションのメインエントリーポイントです。
     *
     * <p>コマンドライン引数を受け取って {@link #run(String[])} を実行し、その終了コードでプロセスを終了します。</p>
     *
     * @param args コマンドライン引数の配列。
     */
    public static void main(final String[] args) {
        final int exitCode = run(args);
        System.exit(exitCode);
    }

    /**
     * リモートコマンド実行処理を実行し、終了コードを返します。
     *
     * <p>コマンドライン引数の解析、多段ホップ制御、接続無限ループチェック、
     * リモートコマンドの実行および戻り値/メッセージの判定を行い、最終的な終了コードを返却します。</p>
     *
     * <p><b>使用例:</b></p>
     * <pre>{@code
     * String[] args = {"-h", "192.168.1.10", "-u", "Administrator", "-p", "Pass123", "-cmd", "dir"};
     * int exitCode = rmtcmd.run(args);
     * if (exitCode == 0) {
     *     System.out.println("正常終了");
     * }
     * }</pre>
     *
     * @param args コマンドライン引数の配列。
     * @return プログラムの実行結果を示す終了コード。0: 正常終了、0以外: 警告コードまたはエラーコード。
     */
    public static int run(final String[] args) {
        final LocalDateTime startTime = LocalDateTime.now();
        final ClsLogger logger = new ClsLogger();
        final ClsAppArg appArg = new ClsAppArg(logger);
        final ClsWinRs winRs = new ClsWinRs(logger);

        ClsWinRs.configureLogging(0);
        boolean isOk = appArg.getArgs(args);
        ClsWinRs.configureLogging(appArg.getVerbose());

        if (appArg.getVerbose() > 0) {
            logger.writeLine(MdlConst.LVL_NONE, "===<<< [" + appArg.getExeBaseName() + "] START : "
                    + MdlDate.getFormattedDate(startTime, "yyyy/MM/dd HH:mm:ss") + ">>>===");
        }

        if (isOk && !appArg.isUsage()) {
            // 接続元情報の取得
            final String envFrom = System.getenv("RMTCMD_FROM");
            final String connectFrom = envFrom != null ? envFrom : "";
            appArg.setCurHops(MdlUtil.parseInt(System.getenv("RMTCMD_HOPS"), -1) + 1);

            // 接続無限ループチェック
            if (appArg.isLoopCheck() && !connectFrom.isBlank()) {
                if (MdlUtil.parseCsvToList(null, connectFrom, "[,/|]", appArg.getVerbose(), true).contains(appArg.getHostname())) {
                    logger.writeLine(MdlConst.LVL_NONE, "");
                    logger.writeLine(MdlConst.LVL_NONE, "[ERROR] LOOP DETECTED : CURRENT HOST = " + appArg.getHostname() + " / PREVIOUS HOSTS = " + connectFrom);
                    logger.writeLine(MdlConst.LVL_NONE, "");
                    isOk = false;
                }
            }

            // ホップ数チェック
            if (appArg.getMaxHops() > 0 && appArg.getCurHops() >= appArg.getMaxHops()) {
                logger.writeLine(MdlConst.LVL_NONE, "");
                logger.writeLine(MdlConst.LVL_NONE, "[ERROR] HOP COUNT EXCEEDED LIMIT : CURRENT HOPS = " + appArg.getCurHops() + " / LIMIT HOPS = " + appArg.getMaxHops());
                logger.writeLine(MdlConst.LVL_NONE, "");
                isOk = false;
            }

            // 環境変数設定
            appArg.getProcEnvs().put("RMTCMD_FROM", connectFrom.isBlank() ? appArg.getHostname() : connectFrom + "/" + appArg.getHostname());
            appArg.getProcEnvs().put("RMTCMD_HOPS", String.valueOf(appArg.getCurHops()));
            if (appArg.isAjsJob()) {
                appArg.getProcEnvs().put("AJSJOBNAME", appArg.getJobName());
            }
        }

        // 処理
        if (isOk && !appArg.isUsage()) {
            if (appArg.getVerbose() > 1) {
                logger.writeLine(MdlConst.LVL_NONE, "== SRC HOST    = " + appArg.getProcEnvs().get("RMTCMD_FROM"));
                logger.writeLine(MdlConst.LVL_NONE, "== REMOTE HOST = " + appArg.getRemoteHost());
                if (appArg.getDomain() != null && !appArg.getDomain().isBlank()) {
                    logger.writeLine(MdlConst.LVL_NONE, "== DOMAIN      = " + appArg.getDomain());
                }
                final String effectiveUser = (appArg.getUserNoDomain() != null && !appArg.getUserNoDomain().isBlank())
                        ? appArg.getUserNoDomain()
                        : appArg.getUsername();
                logger.writeLine(MdlConst.LVL_NONE, "== USER        = " + effectiveUser);
            }
            if (appArg.getVerbose() > 3) {
                logger.writeLine(MdlConst.LVL_NONE, "== PASSWORD    = " + appArg.getPassword());
            }
            if (appArg.getVerbose() > 1) {
                logger.writeLine(MdlConst.LVL_NONE, "== MODE        = " + appArg.getExecMode());
                logger.writeLine(MdlConst.LVL_NONE, "== CmdPath     = " + appArg.getCmdPath());
                if (appArg.getWorkDir() != null && !appArg.getWorkDir().isBlank()) {
                    logger.writeLine(MdlConst.LVL_NONE, "== CWD         = " + appArg.getWorkDir());
                }
                if (!appArg.getProcEnvs().isEmpty()) {
                    for (final Map.Entry<String, String> entry : appArg.getProcEnvs().entrySet()) {
                        logger.writeLine(MdlConst.LVL_NONE, "== SET ENV     = " + entry.getKey() + "=" + entry.getValue());
                    }
                }
                if (appArg.getAddEnvPath() != null && !appArg.getAddEnvPath().isBlank()) {
                    logger.writeLine(MdlConst.LVL_NONE, "== +ENV:PATH   = " + appArg.getAddEnvPath());
                }
                if (appArg.isAjsJob()) {
                    logger.writeLine(MdlConst.LVL_NONE, "== AJSJOBNAME  = " + appArg.getJobName());
                }
            }

            // プロパティ設定
            winRs.setLogWrite(true);
            winRs.setRemoteHost(appArg.getRemoteHost());
            winRs.setExecMode(appArg.getExecMode());
            winRs.setComSpec(appArg.getComSpec());
            winRs.setVerbose(appArg.getVerbose());
            winRs.setPort(appArg.getPort());
            winRs.setWorkDir(appArg.getWorkDir());
            winRs.setTeePath(appArg.getTeePath());
            winRs.setProcEnvs(appArg.getProcEnvs());
            winRs.setAddEnvPath(appArg.getAddEnvPath());
            winRs.setStackTrace(appArg.isStackTrace());
            winRs.setOpTimeout(appArg.getOpTimeout());
            winRs.setOpenTimeout(appArg.getOpenTimeout());

            winRs.setRetryMax(appArg.getRetryMax());
            winRs.setRetrySleep(appArg.getRetrySleep());
            winRs.setRetryRmtCmd(appArg.isRetryRmtCmd());

            winRs.setOkRetCsv(appArg.getOkRetCsv());
            winRs.setWarnRetCsv(appArg.getWarnRetCsv());
            winRs.setErrRetCsv(appArg.getErrRetCsv());
            winRs.setOkMsgCsv(appArg.getOkMsgCsv());
            winRs.setWarnMsgCsv(appArg.getWarnMsgCsv());
            winRs.setErrMsgCsv(appArg.getErrMsgCsv());
            winRs.setWarnThreshold(appArg.getWarnThreshold());
            winRs.setErrThreshold(appArg.getErrThreshold());
            winRs.setErrAtNegative(appArg.isErrAtNegative());
            winRs.setAlwaysNormal(appArg.getJudgeMode() == ClsAppArg.ALWAYS_NORMAL);

            winRs.setWarnCode(appArg.getWarnCode());
            winRs.setErrorCode(appArg.getErrorCode());

            winRs.setDomain(appArg.getDomain());
            final String targetUser = (appArg.getUserNoDomain() != null && !appArg.getUserNoDomain().isBlank())
                    ? appArg.getUserNoDomain()
                    : appArg.getUsername();
            winRs.setUsername(targetUser);
            winRs.setPassword(appArg.getPassword());
            winRs.setAuthMechanism(appArg.getAuthMechanism());

            // 初期化
            winRs.initialize();

            // 実行
            if (appArg.getVerbose() > 5) {
                logger.writeLine(MdlConst.LVL_NONE, "TRY : winRs.execute(CmdPath)");
            }
            appArg.setReturnCode(winRs.execute(appArg.getCmdPath()));
            if (appArg.getVerbose() > 5) {
                logger.writeLine(MdlConst.LVL_NONE, "RET : winRs.execute(CmdPath) => " + appArg.getReturnCode());
            }

            switch (appArg.getJudgeMode()) {
                case ClsAppArg.NONE:
                    appArg.setReturnCode(winRs.getCmdExitCode());
                    break;
                case ClsAppArg.RETURN_CODE:
                    appArg.setReturnCode(winRs.getMethodExit());
                    break;
                case ClsAppArg.ALWAYS_WARN:
                    if (appArg.isErrAtNegative() && winRs.getCmdExitCode() < 0) {
                        appArg.setReturnCode(appArg.getErrorCode());
                    } else {
                        appArg.setReturnCode(appArg.getWarnCode());
                    }
                    break;
                case ClsAppArg.ALWAYS_ERROR:
                    appArg.setReturnCode(appArg.getErrorCode());
                    break;
                default:
                    // 実行戻り値をそのまま維持
                    break;
            }
        } else {
            if (appArg.isUsage()) {
                appArg.setReturnCode(appArg.getWarnCode() == MdlConst.INT_NULL ? MdlConst.LVL_W : appArg.getWarnCode());
                appArg.usage();
            } else {
                appArg.setReturnCode(appArg.getErrorCode() == MdlConst.INT_NULL ? MdlConst.LVL_E : appArg.getErrorCode());
            }
        }

        if (appArg.getVerbose() > 0) {
            final LocalDateTime endTime = LocalDateTime.now();
            final double elapsedTime = Duration.between(startTime, endTime).toNanos() / 1_000_000_000.0;
            logger.writeLine(MdlConst.LVL_NONE, String.format(Locale.ROOT,
                    "===<<< [%s] EXIT (%d) : %s : %.3f sec>>>===",
                    appArg.getExeBaseName(),
                    appArg.getReturnCode(),
                    MdlDate.getFormattedDate(endTime, "yyyy/MM/dd HH:mm:ss"),
                    elapsedTime));
        }

        if (appArg.isEchoRetcode()) {
            logger.writeLine(MdlConst.LVL_NONE, String.valueOf(appArg.getReturnCode()));
        }

        return appArg.getReturnCode();
    }
}
