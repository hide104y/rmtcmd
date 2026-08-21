using System;
using CmnClsLib.Class;
using CmnClsLib.Module;
using rmtcmd.Class;

// 2026/08/08 Gemini 3.6 Flash (High) Review & Modified

namespace rmtcmd;

public class Program
{
    /// <summary>
    /// リモートコマンド実行ツールのエントリーポイントメソッドです。
    /// コマンドライン引数を解析し、指定されたリモートホストに対して WinRS を用いたコマンド実行および結果判定を行います。
    /// </summary>
    /// <param name="args">コマンドライン引数の配列。</param>
    /// <returns>
    /// プログラムの実行結果を示す終了コード。
    /// 0: 正常終了、0以外: 警告コードまたはエラーコード。
    /// </returns>
    /// <example>
    /// <code>
    /// // コマンドプロンプトや PowerShell からの呼び出し例:
    /// rmtcmd.exe -h remote-server -c hostname -u admin -p password
    /// </code>
    /// </example>
    public static int Main(string[] args)
    {
        DateTime startTime = DateTime.Now;
        ClsLogger logger = new();
        ClsAppArg appArg = new(logger);
        ClsWinRs winRs = new(logger);

        bool isOk = appArg.GetArgs(args);

        if (appArg.Verbose > 0)
        {
            logger.WriteLine(MdlConst.LVL_NONE, $"===<<< [{appArg.ExeBaseName}] START : {MdlDate.GetFormattedDate(startTime, "yyyy/MM/dd HH:mm:ss")}>>>===");
        }

        if (isOk && !appArg.IsUsage)
        {
            // 接続元情報の取得
            string connectFrom = Environment.GetEnvironmentVariable("RMTCMD_FROM") ?? "";
            appArg.CurHops = MdlUtil.ParseInt(Environment.GetEnvironmentVariable("RMTCMD_HOPS"), -1) + 1;

            // 接続無限ループチェック
            if (appArg.IsLoopCheck && !string.IsNullOrEmpty(connectFrom))
            {
                if (MdlUtil.ParseCsvToList(null, connectFrom, @"[,\/|]", appArg.Verbose, true).Contains(appArg.Hostname))
                {
                    logger.WriteLine(MdlConst.LVL_NONE, "");
                    logger.WriteLine(MdlConst.LVL_NONE, $"[ERROR] LOOP DETECTED : CURRENT HOST = {appArg.Hostname} / PREVIOUS HOSTS = {connectFrom}");
                    logger.WriteLine(MdlConst.LVL_NONE, "");
                    isOk = false;
                }
            }

            // ホップ数チェック
            if (appArg.MaxHops > 0 && appArg.CurHops >= appArg.MaxHops)
            {
                logger.WriteLine(MdlConst.LVL_NONE, "");
                logger.WriteLine(MdlConst.LVL_NONE, $"[ERROR] HOP COUNT EXCEEDED LIMIT : CURRENT HOPS = {appArg.CurHops} / LIMIT HOPS = {appArg.MaxHops}");
                logger.WriteLine(MdlConst.LVL_NONE, "");
                isOk = false;
            }

            // 環境変数設定
            appArg.ProcEnvsDic["RMTCMD_FROM"] = string.IsNullOrEmpty(connectFrom) ? appArg.Hostname : $"{connectFrom}/{appArg.Hostname}";
            appArg.ProcEnvsDic["RMTCMD_HOPS"] = appArg.CurHops.ToString();
            if (appArg.IsAjsJob) appArg.ProcEnvsDic["AJSJOBNAME"] = appArg.GetJobName();
        }

        // 処理
        if (isOk && !appArg.IsUsage)
        {
            if (appArg.Verbose > 1)
            {
                logger.WriteLine(MdlConst.LVL_NONE, $"== SRC HOST    = {appArg.ProcEnvsDic["RMTCMD_FROM"]}");
                logger.WriteLine(MdlConst.LVL_NONE, $"== REMOTE HOST = {appArg.RemoteHost}");
                logger.WriteLine(MdlConst.LVL_NONE, $"== USER        = {appArg.Username}");
            }
            if (appArg.Verbose > 3)
            {
                logger.WriteLine(MdlConst.LVL_NONE, $"== PASSWORD    = {appArg.Password}");
            }
            if (appArg.Verbose > 1)
            {
                logger.WriteLine(MdlConst.LVL_NONE, $"== MODE        = {appArg.ExecMode}");
                logger.WriteLine(MdlConst.LVL_NONE, $"== CmdPath     = {appArg.CmdPath}");
                if (!string.IsNullOrEmpty(appArg.WorkDir))
                {
                    logger.WriteLine(MdlConst.LVL_NONE, $"== CWD         = {appArg.WorkDir}");
                }
                if (appArg.ProcEnvsDic.Count > 0)
                {
                    foreach (var (key, value) in appArg.ProcEnvsDic)
                    {
                        logger.WriteLine(MdlConst.LVL_NONE, $"== SET ENV     = {key}={value}");
                    }
                }
                if (!string.IsNullOrEmpty(appArg.AddProcEnvPath))
                {
                    logger.WriteLine(MdlConst.LVL_NONE, $"== +ENV:PATH   = {appArg.AddProcEnvPath}");
                }
                if (appArg.IsAjsJob)
                {
                    logger.WriteLine(MdlConst.LVL_NONE, $"== AJSJOBNAME  = {appArg.GetJobName()}");
                }
            }

            // プロパティ設定
            winRs.IsLogWrite = true;
            winRs.RemoteHost = appArg.RemoteHost;
            winRs.ExecMode = appArg.ExecMode;
            winRs.Verbose = appArg.Verbose;
            winRs.Port = appArg.Port;
            winRs.WorkDir = appArg.WorkDir;
            winRs.TeePath = appArg.TeePath;
            winRs.ProcEnvsDic = appArg.ProcEnvsDic;
            winRs.AddProcEnvPath = appArg.AddProcEnvPath;
            winRs.IsStackTrace = appArg.IsStackTrace;
            winRs.OperationTimeout = appArg.OperationTimeout;
            winRs.OpenTimeout = appArg.OpenTimeout;

            winRs.RetryMax = appArg.RetryMax;
            winRs.RetrySleep = appArg.RetrySleep;
            winRs.IsRetryRemoteCmd = appArg.IsRetryRemoteCmd;

            winRs.OkReturnCodeCsv = appArg.OkReturnCodeCsv;
            winRs.WarnReturnCodeCsv = appArg.WarnReturnCodeCsv;
            winRs.ErrorReturnCodeCsv = appArg.ErrorReturnCodeCsv;
            winRs.OkMessageCsv = appArg.OkMessageCsv;
            winRs.WarnMessageCsv = appArg.WarnMessageCsv;
            winRs.ErrorMessageCsv = appArg.ErrorMessageCsv;
            winRs.WarnThreshold = appArg.WarnThreshold;
            winRs.ErrorThreshold = appArg.ErrorThreshold;
            winRs.IsErrorAtNegativeValue = appArg.IsErrorAtNegativeValue;
            winRs.IsAlwaysNormal = appArg.ResultJudgment == ClsAppArg.ALWAYS_NORMAL;

            winRs.WarnCode = appArg.WarnCode;
            winRs.ErrorCode = appArg.ErrorCode;

            winRs.Username = appArg.Username;
            winRs.Password = appArg.Password;
            winRs.AuthMechanism = appArg.AuthMechanism;

            // 初期化
            winRs.Initialize();

            // 実行
            if (appArg.Verbose > 5) logger.WriteLine(MdlConst.LVL_NONE, "TRY : winRs.Execute(CmdPath)");
            appArg.ReturnCode = winRs.Execute(appArg.CmdPath);
            if (appArg.Verbose > 5) logger.WriteLine(MdlConst.LVL_NONE, $"RET : winRs.Execute(CmdPath) => {appArg.ReturnCode}");

            appArg.ReturnCode = appArg.ResultJudgment switch
            {
                ClsAppArg.NONE => winRs.CmdExitStatus,
                ClsAppArg.RETURN_CODE => winRs.MethodExitStatus,
                ClsAppArg.ALWAYS_WARN => appArg.IsErrorAtNegativeValue && winRs.CmdExitStatus < 0 ? appArg.ErrorCode : appArg.WarnCode,
                ClsAppArg.ALWAYS_ERROR => appArg.ErrorCode,
                _ => appArg.ReturnCode
            };
        }
        else
        {
            if (appArg.IsUsage)
            {
                appArg.ReturnCode = appArg.WarnCode == MdlConst.INT_NULL ? MdlConst.LVL_W : appArg.WarnCode;
                appArg.Usage();
            }
            else
            {
                appArg.ReturnCode = appArg.ErrorCode == MdlConst.INT_NULL ? MdlConst.LVL_E : appArg.ErrorCode;
            }
        }

        if (appArg.Verbose > 0)
        {
            DateTime endTime = DateTime.Now;
            double elapsedTime = (endTime - startTime).TotalSeconds;
            logger.WriteLine(MdlConst.LVL_NONE, $"===<<< [{appArg.ExeBaseName}] EXIT ({appArg.ReturnCode}) : {MdlDate.GetFormattedDate(endTime, "yyyy/MM/dd HH:mm:ss")} : {elapsedTime:F3} sec>>>===");
        }

        if (appArg.IsEchoRetcode)
        {
            logger.WriteLine(MdlConst.LVL_NONE, appArg.ReturnCode.ToString());
        }

        return appArg.ReturnCode;
    }
}
