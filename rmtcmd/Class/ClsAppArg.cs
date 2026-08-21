using System;
using CmnClsLib.Class;
using CmnClsLib.Module;

// 2026/08/08 Gemini 3.6 Flash (High) Review & Modified

namespace rmtcmd.Class
{
    public class ClsAppArg
    {
        public const short NONE = 0;                                // 判定しない
        public const short RETURN_CODE = 1;                         // 閾値による判定
        public const short ALWAYS_NORMAL = 2;                       // 常に正常終了
        public const short ALWAYS_WARN = 3;                         // 常に警告終了
        public const short ALWAYS_ERROR = 4;                        // 常に異常終了

        private ClsLogger _logger;
        private ClsCmmnArgs _cmmnArgs;
        private string _usernameWithoutDomain = "";
        private string _addProcEnvStr = "";

        /// <summary>
        /// <see cref="ClsAppArg"/> クラスの新しいインスタンスを初期化します。
        /// </summary>
        /// <param name="logger">ログ出力を行うためのロガーオブジェクト。</param>
        /// <example>
        /// <code>
        /// var logger = new ClsLogger();
        /// var appArg = new ClsAppArg(logger);
        /// </code>
        /// </example>
        public ClsAppArg(ClsLogger logger)
        {
            _logger = logger;
            _cmmnArgs = new(_logger);
            _cmmnArgs.GetModuleInfo(System.Diagnostics.Process.GetCurrentProcess().MainModule?.FileName ?? "");
            ExeDir = _cmmnArgs.ExeDir;
            ExeBaseName = _cmmnArgs.ExeBaseName;
            Pid = _cmmnArgs.Pid;
            ComSpec = Environment.GetEnvironmentVariable("ComSpec") ?? "cmd";
        }

        public string ExeBaseName { get; set; } = "";
        public string ExeDir { get; set; } = "";
        public int Pid { get; set; }
        public string ComSpec { get; set; } = "";
        public bool IsUsage { get; private set; }
        public int ReturnCode { get; set; } = MdlConst.LVL_I;
        public int Verbose { get; set; }
        public bool IsStackTrace { get; set; }
        public string WorkDir { get; set; } = "";
        public string CmdPath { get; set; } = "hostname";
        public string RemoteHost { get; set; } = "localhost";
        public string TeePath { get; set; } = "";
        public string AddProcEnvPath { get; set; } = "";
        public Dictionary<string, string> ProcEnvsDic { get; set; } = [];
        public int ExecMode { get; set; } = ClsWinRs.EXEC_MODE_CMD;
        public int Port { get; set; } = 5985;
        public int OperationTimeout { get; set; } = 180;
        public int OpenTimeout { get; set; } = 120;
        public int RetryMax { get; set; }
        public int RetrySleep { get; set; } = 5;
        public bool IsRetryRemoteCmd { get; set; }
        public string Username { get; set; } = "";
        public string Password { get; set; } = "";
        public bool IsSwitchUser { get; set; }
        public bool IsLogonAlwaysOk { get; set; }
        public int AuthMechanism { get; set; }
        public string OkReturnCodeCsv { get; set; } = "0";
        public string WarnReturnCodeCsv { get; set; } = "";
        public string ErrorReturnCodeCsv { get; set; } = "";
        public string OkMessageCsv { get; set; } = "";
        public string WarnMessageCsv { get; set; } = "";
        public string ErrorMessageCsv { get; set; } = "";
        public int ResultJudgment { get; set; } = NONE;
        public int WarnThreshold { get; set; } = MdlConst.INT_NULL;
        public int ErrorThreshold { get; set; } = MdlConst.INT_NULL;
        public bool IsErrorAtNegativeValue { get; set; }
        public int ErrorCode { get; set; } = MdlConst.INT_NULL;
        public int WarnCode { get; set; } = MdlConst.INT_NULL;
        public bool IsEchoRetcode { get; set; }
        public bool IsAjsJob { get; set; }
        public string Hostname { get; set; } = Environment.MachineName;
        public int MaxHops { get; set; } = 3;
        public int CurHops { get; set; }
        public bool IsLoopCheck { get; set; } = true;

        /// <summary>
        /// コマンドライン引数を解析し、本クラスのプロパティおよび共通引数オブジェクトを設定します。
        /// </summary>
        /// <param name="args">コマンドライン引数の配列。</param>
        /// <returns>引数の解析および検証が正常に完了した場合は <c>true</c>、失敗した場合は <c>false</c>。</returns>
        /// <example>
        /// <code>
        /// string[] args = ["-h", "192.168.1.1", "-u", "admin", "-p", "password"];
        /// bool isSuccess = appArg.GetArgs(args);
        /// </code>
        /// </example>
        public bool GetArgs(string[] args)
        {
            Dictionary<string, string> namedArgs = [];
            bool isOk = true;
            string tempStr = "";
            bool tempFlg = false;

            // -----------------------------------------------------------------
            // ClsCmmnParams初期値設定
            // -----------------------------------------------------------------
            _cmmnArgs.RetryMax = RetryMax;
            _cmmnArgs.RetrySleep = RetrySleep;

            // -----------------------------------------------------------------
            // ClsCmmnParams処理
            // -----------------------------------------------------------------
            namedArgs = MdlArg.GetNamedArgs(args);
            _cmmnArgs.NamedArgs = namedArgs;
            isOk = _cmmnArgs.GetCommonArgs();

            // -----------------------------------------------------------------
            // ClsCmmnParams引数取得：ETC
            // -----------------------------------------------------------------
            IsUsage = _cmmnArgs.IsUsage;
            Verbose = _cmmnArgs.Verbose;
            IsStackTrace = _cmmnArgs.IsStackTrace;
            IsAjsJob = _cmmnArgs.IsAjsJob;
            RetryMax = _cmmnArgs.RetryMax;
            RetrySleep = Math.Min(_cmmnArgs.RetrySleep, 3600);

            // -----------------------------------------------------------------
            // ClsCmmnParams引数取得：認証情報
            // -----------------------------------------------------------------
            isOk = _cmmnArgs.GetArgsForAuth();
            string domainName = _cmmnArgs.DomainName;
            Username = _cmmnArgs.Username;
            _usernameWithoutDomain = _cmmnArgs.UsernameWithoutDomain;
            Password = _cmmnArgs.Password;
            IsLogonAlwaysOk = _cmmnArgs.IsLogonAlwaysOk;
            IsSwitchUser = _cmmnArgs.IsSwitchUser;

            // -----------------------------------------------------------------
            // Basic Option：
            // -----------------------------------------------------------------
            if (!string.IsNullOrEmpty(_cmmnArgs.Host))
            {
                RemoteHost = _cmmnArgs.Host;
                if (IsAjsJob) RemoteHost = _cmmnArgs.Jp1.ConvertStringFromEnvironment(RemoteHost);
            }
            else
            {
                RemoteHost = "localhost";
            }

            // -port port          ：WinRMポート番号
            if (MdlArg.ContainsKey(namedArgs, "port"))
            {
                tempStr = MdlArg.GetValue(namedArgs, "port");
                if (!string.IsNullOrEmpty(tempStr))
                {
                    int tempInt = MdlUtil.ParseInt(tempStr, MdlConst.INT_NULL);
                    if (tempInt != MdlConst.INT_NULL)
                    {
                        Port = tempInt;
                    }
                }
            }

            // -u username         ：認証ユーザー名
            if (!string.IsNullOrEmpty(domainName))
            {
                Username = $"{domainName}\\{_usernameWithoutDomain}";
            }

            // -p password         ：認証パスワード
            if (string.IsNullOrEmpty(Password))
            {
                _logger.WriteLine(MdlConst.LVL_NONE, $"Invalid Argument -p password   : 認証パスワード（現状値：{Password}）");
                isOk = false;
            }

            // -cwd path           ：WORKING DIR
            if (MdlArg.ContainsKey(namedArgs, "cwd"))
            {
                tempStr = MdlArg.GetValue(namedArgs, "cwd");
                if (!string.IsNullOrEmpty(tempStr))
                {
                    WorkDir = tempStr;
                }
            }

            // -c|cmd cmd / -ps cmdlet / -ecmd encoded / -es encoded など
            ReadOnlySpan<string> cmdKeys = ["c", "cmd", "ps", "ec", "ecmd", "es", "exe"];
            foreach (string key in cmdKeys)
            {
                if (MdlArg.ContainsKey(namedArgs, key))
                {
                    tempStr = MdlArg.GetValue(namedArgs, key);
                    if (!string.IsNullOrEmpty(tempStr))
                    {
                        ExecMode = key.ToLowerInvariant() switch
                        {
                            "cmd" => ClsWinRs.EXEC_MODE_CMD,
                            "ps" => ClsWinRs.EXEC_MODE_PS,
                            "ec" => ClsWinRs.EXEC_MODE_EC,
                            "ecmd" => ClsWinRs.EXEC_MODE_ECMD,
                            "es" => ClsWinRs.EXEC_MODE_ES,
                            "exe" => ClsWinRs.EXEC_MODE_EXE,
                            _ => ClsWinRs.EXEC_MODE_NORMAL
                        };
                        CmdPath = tempStr.Trim();
                        break;
                    }
                }
            }

            // -rtee path          ：tee-objectファイルパス
            if (MdlArg.ContainsKey(namedArgs, "rtee"))
            {
                tempStr = MdlArg.GetValue(namedArgs, "rtee");
                if (!string.IsNullOrEmpty(tempStr))
                {
                    TeePath = tempStr;
                }
            }

            // -optimeout int      ：Operation Timeout
            if (MdlArg.ContainsKey(namedArgs, "optimeout"))
            {
                tempStr = MdlArg.GetValue(namedArgs, "optimeout");
                if (!string.IsNullOrEmpty(tempStr))
                {
                    int tempInt = MdlUtil.ParseInt(tempStr, MdlConst.INT_NULL);
                    if (tempInt != MdlConst.INT_NULL) OperationTimeout = tempInt;
                }
            }

            // -opentimeout int    ：Open timeout
            if (MdlArg.ContainsKey(namedArgs, "opentimeout"))
            {
                tempStr = MdlArg.GetValue(namedArgs, "opentimeout");
                if (!string.IsNullOrEmpty(tempStr))
                {
                    int tempInt = MdlUtil.ParseInt(tempStr, MdlConst.INT_NULL);
                    if (tempInt != MdlConst.INT_NULL) OpenTimeout = tempInt;
                }
            }

            // -am num
            if (MdlArg.ContainsKey(namedArgs, "am"))
            {
                tempStr = MdlArg.GetValue(namedArgs, "am");
                if (!string.IsNullOrEmpty(tempStr))
                {
                    int tempInt = MdlUtil.ParseInt(tempStr, MdlConst.INT_NULL);
                    if (tempInt != MdlConst.INT_NULL) AuthMechanism = tempInt;
                }
            }

            // -retry-rmtcmd       ：リモートコマンドリトライフラグ
            if (MdlArg.ContainsKey(namedArgs, "retry-rmtcmd"))
            {
                IsRetryRemoteCmd = true;
            }

            // -penv n1=v1,n2=v2   ：環境変数([,|]区切り）
            if (MdlArg.ContainsKey(namedArgs, "penv"))
            {
                tempStr = MdlArg.GetValue(namedArgs, "penv");
                if (!string.IsNullOrEmpty(tempStr))
                {
                    ProcEnvsDic = MdlUtil.ParseCsvToDictionary(ProcEnvsDic, tempStr, @"[,|]", @"=", Verbose, false, false);
                    _addProcEnvStr = tempStr;
                }
            }

            // -add-penv-path path ：環境変数PATH先頭追加内容
            if (MdlArg.ContainsKey(namedArgs, "add-penv-path"))
            {
                tempStr = MdlArg.GetValue(namedArgs, "add-penv-path");
                if (!string.IsNullOrEmpty(tempStr))
                {
                    AddProcEnvPath = tempStr;
                }
            }

            // -hop num            ：最大ホップ数（0=無効化）
            if (MdlArg.ContainsKey(namedArgs, "hop"))
            {
                tempStr = MdlArg.GetValue(namedArgs, "hop");
                if (!string.IsNullOrEmpty(tempStr))
                {
                    int tempInt = MdlUtil.ParseInt(tempStr, MdlConst.INT_NULL);
                    if (tempInt != MdlConst.INT_NULL) MaxHops = tempInt;
                }
            }

            // -nlc                ：ループ検出無効化フラグ
            if (MdlArg.ContainsKey(namedArgs, "nlc"))
            {
                IsLoopCheck = false;
            }

            // -----------------------------------------------------------------
            // Option Result Judgement：
            // -----------------------------------------------------------------
            if (MdlArg.ContainsKey(namedArgs, "ret"))
            {
                tempStr = MdlArg.GetValue(namedArgs, "ret");
                if (!string.IsNullOrEmpty(tempStr))
                {
                    tempFlg = true;
                    ResultJudgment = tempStr.ToLowerInvariant() switch
                    {
                        "none" => NONE,
                        "0" or "normal" or "always_normal" => ALWAYS_NORMAL,
                        "10" or "warn" or "always_warn" => ALWAYS_WARN,
                        "20" or "error" or "always_error" => ALWAYS_ERROR,
                        "ret" or "retcode" or "return_code" => RETURN_CODE,
                        _ => NONE
                    };
                }
            }
            if (!tempFlg)
            {
                ResultJudgment = NONE;
            }
            if (RETURN_CODE == ResultJudgment)
            {
                ErrorCode = (ErrorCode == MdlConst.INT_NULL ? MdlConst.LVL_E : ErrorCode);
                WarnCode = (WarnCode == MdlConst.INT_NULL ? MdlConst.LVL_W : WarnCode);
            }

            // -w 数字             ：警告終了閾値
            if (MdlArg.ContainsKey(namedArgs, "w"))
            {
                tempStr = MdlArg.GetValue(namedArgs, "w");
                if (!string.IsNullOrEmpty(tempStr))
                {
                    int tempInt = MdlUtil.ParseInt(tempStr, MdlConst.INT_NULL);
                    if (tempInt != MdlConst.INT_NULL) WarnThreshold = tempInt;
                }
            }

            // -e 数字             ：異常終了閾値
            if (MdlArg.ContainsKey(namedArgs, "e"))
            {
                tempStr = MdlArg.GetValue(namedArgs, "e");
                if (!string.IsNullOrEmpty(tempStr))
                {
                    int tempInt = MdlUtil.ParseInt(tempStr, MdlConst.INT_NULL);
                    if (tempInt != MdlConst.INT_NULL) ErrorThreshold = tempInt;
                }
            }

            // -negative           ：負値のエラー判定有無
            if (MdlArg.ContainsKey(namedArgs, "negative"))
            {
                IsErrorAtNegativeValue = true;
            }

            // -warn 数字          ：警告時の終了コード
            if (MdlArg.ContainsKey(namedArgs, "warn"))
            {
                tempStr = MdlArg.GetValue(namedArgs, "warn");
                if (!string.IsNullOrEmpty(tempStr))
                {
                    int tempInt = MdlUtil.ParseInt(tempStr, MdlConst.INT_NULL);
                    if (tempInt != MdlConst.INT_NULL) WarnCode = tempInt;
                }
            }

            // -err 数字           ：異常終了時の終了コード
            if (MdlArg.ContainsKey(namedArgs, "err"))
            {
                tempStr = MdlArg.GetValue(namedArgs, "err");
                if (!string.IsNullOrEmpty(tempStr))
                {
                    int tempInt = MdlUtil.ParseInt(tempStr, MdlConst.INT_NULL);
                    if (tempInt != MdlConst.INT_NULL) ErrorCode = tempInt;
                }
            }

            // -ok-ret 数字,数値   ：正常終了判定戻り値リスト
            if (MdlArg.ContainsKey(namedArgs, "ok-ret"))
            {
                tempStr = MdlArg.GetValue(namedArgs, "ok-ret");
                if (!string.IsNullOrEmpty(tempStr))
                {
                    OkReturnCodeCsv = tempStr.Trim();
                }
            }

            // -warn-ret 数字,数値 ：警告終了判定戻り値リスト
            if (MdlArg.ContainsKey(namedArgs, "warn-ret"))
            {
                tempStr = MdlArg.GetValue(namedArgs, "warn-ret");
                if (!string.IsNullOrEmpty(tempStr))
                {
                    WarnReturnCodeCsv = tempStr.Trim();
                }
            }

            // -ng-ret 数字,数値 ：異常終了判定戻り値リスト
            ReadOnlySpan<string> errRetKeys = ["ng-ret", "err-ret", "error-ret"];
            foreach (string key in errRetKeys)
            {
                if (MdlArg.ContainsKey(namedArgs, key))
                {
                    tempStr = MdlArg.GetValue(namedArgs, key);
                    if (!string.IsNullOrEmpty(tempStr))
                    {
                        ErrorReturnCodeCsv = tempStr.Trim();
                        break;
                    }
                }
            }

            // -ok-str 文字列      ：正常終了判定出力文字列
            if (MdlArg.ContainsKey(namedArgs, "ok-str"))
            {
                tempStr = MdlArg.GetValue(namedArgs, "ok-str");
                if (!string.IsNullOrEmpty(tempStr))
                {
                    OkMessageCsv = tempStr.Trim();
                }
            }

            // -warn-str 文字列    ：警告終了判定出力文字列
            if (MdlArg.ContainsKey(namedArgs, "warn-str"))
            {
                tempStr = MdlArg.GetValue(namedArgs, "warn-str");
                if (!string.IsNullOrEmpty(tempStr))
                {
                    WarnMessageCsv = tempStr.Trim();
                }
            }

            // -ng-str 文字列      ：異常終了判定出力文字列
            ReadOnlySpan<string> errStrKeys = ["ng-str", "err-str", "error-str"];
            foreach (string key in errStrKeys)
            {
                if (MdlArg.ContainsKey(namedArgs, key))
                {
                    tempStr = MdlArg.GetValue(namedArgs, key);
                    if (!string.IsNullOrEmpty(tempStr))
                    {
                        ErrorMessageCsv = tempStr.Trim();
                        break;
                    }
                }
            }

            // -echo-retcd         ：終了コード表示フラグ
            if (MdlArg.ContainsKey(namedArgs, "echo-retcd"))
            {
                IsEchoRetcode = true;
            }

            // -----------------------------------------------------------------
            // 掃除
            // -----------------------------------------------------------------
            namedArgs.Clear();

            return isOk;
        }

        /// <summary>
        /// 設定されているJP1ジョブ名を取得します。
        /// </summary>
        /// <returns>JP1ジョブ名を表す文字列。未設定の場合は空文字列。</returns>
        /// <example>
        /// <code>
        /// string jobName = appArg.GetJobName();
        /// </code>
        /// </example>
        public string GetJobName() => _cmmnArgs?.Jp1?.JobName ?? "";

        /// <summary>
        /// 指定された判定モードに対応する結果判定の説明テキストを取得します。
        /// </summary>
        /// <param name="mode">判定モード数値 (<see cref="NONE"/>, <see cref="RETURN_CODE"/>, <see cref="ALWAYS_NORMAL"/>, <see cref="ALWAYS_WARN"/>, <see cref="ALWAYS_ERROR"/>)。</param>
        /// <returns>判定モードの説明を表す日本語テキスト。</returns>
        /// <example>
        /// <code>
        /// string modeText = appArg.GetResultJudgmentText(ClsAppArg.RETURN_CODE);
        /// </code>
        /// </example>
        public string GetResultJudgmentText(int mode) => mode switch
        {
            RETURN_CODE => "retcode：閾値による判定",
            ALWAYS_NORMAL => "nomal：常に正常終了",
            ALWAYS_WARN => "warn：常に警告終了",
            ALWAYS_ERROR => "error：常に異常終了",
            _ => "none：コマンドの戻り値を返却"
        };

        /// <summary>
        /// コマンドライン引数の使用方法（Usage）および各オプションの現在値をログに出力します。
        /// </summary>
        /// <example>
        /// <code>
        /// appArg.Usage();
        /// </code>
        /// </example>
        public void Usage()
        {
            _logger.WriteLine(MdlConst.LVL_NONE, "");
            _logger.WriteLine(MdlConst.LVL_NONE, $"Usage : {ExeDir}{Path.DirectorySeparatorChar}{ExeBaseName}.exe [Option] [Option]...");
            _logger.WriteLine(MdlConst.LVL_NONE, "");
            _logger.WriteLine(MdlConst.LVL_NONE, "Option ：");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -h hostname         ：リモートホスト （現状値={RemoteHost}）");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -port port          ：WinRMポート番号（現状値={Port}）");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -u username         ：認証ユーザー名 （現状値={Username}）");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -p password         ：認証パスワード （現状値={Password}）");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -cwd path           ：WORKING DIR    （現在値={WorkDir}）");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -c|cmd cmd          ：DOSコマンド    （現状値={CmdPath}）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -ps cmdlet          ：Powershellコマンドレット");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -ecmd encoded       ：BASE64 DOSコマンド");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -es encoded         ：BASE64 Powershellコマンドレット");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -rtee path          ：tee-objectファイルパス    （現在値={TeePath}）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -log path           ：ログ出力ファイルパス");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -ldir path          ：ログ出力ディレクトリパス");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -optimeout int      ：Operation Timeout         （現在値={OperationTimeout}）");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -opentimeout int    ：Open timeout              （現在値={OpenTimeout}）");
            _logger.WriteLine(MdlConst.LVL_NONE, "Option ：Decode Password");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -def path           ：アカウント設定ファイルパス");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -ep password        ：認証暗号化パスワード");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -key key            ：暗号鍵");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -size 128|256       ：鍵長");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -am num             ：0:Default / 1:Basic 2:Negotiate / 3:NegotiateWithImplicitCredential / 4:Credssp / 5:Digest / 6:Kerberos（現状値={AuthMechanism}）");
            _logger.WriteLine(MdlConst.LVL_NONE, "Option ：Retry");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -retry num          ：リトライ回数（現状値={RetryMax}）");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -sleep sec          ：待ち秒数    （現状値={RetrySleep}）");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -retry-rmtcmd       ：リモートコマンドリトライ  （現状値={IsRetryRemoteCmd}）");
            _logger.WriteLine(MdlConst.LVL_NONE, "Option ：Process Environment");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -penv n1=v1,n2=v2   ：環境変数([,|]区切り）     （現状値={_addProcEnvStr}）");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -add-penv-path path ：環境変数PATH先頭追加内容  （現状値={_addProcEnvStr}）");
            _logger.WriteLine(MdlConst.LVL_NONE, "Option ：Hops");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -hop num            ：最大ホップ数（0=無効化）  （現在値={MaxHops}）");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -nlc                ：ループ検出無効化フラグ    （現在値={!IsLoopCheck}）");
            _logger.WriteLine(MdlConst.LVL_NONE, "Option Result Judgement：");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -ret none|retcode   ：CMD戻り値判定有無         （現在値={GetResultJudgmentText(ResultJudgment)}）");
            _logger.WriteLine(MdlConst.LVL_NONE, "Option Result Judgement with Retcode：");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -w 数字             ：警告終了閾値              （現在値={(WarnThreshold == MdlConst.INT_NULL ? "" : WarnThreshold.ToString())}）");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -e 数字             ：異常終了閾値              （現在値={(ErrorThreshold == MdlConst.INT_NULL ? "" : ErrorThreshold.ToString())}）");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -negative           ：負値のエラー判定有無      （現在値={IsErrorAtNegativeValue}）");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -warn 数字          ：警告時の終了コード        （現在値={(WarnCode == MdlConst.INT_NULL ? "" : WarnCode.ToString())}）");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -err 数字           ：エラー時の終了コード      （現在値={(ErrorCode == MdlConst.INT_NULL ? "" : ErrorCode.ToString())}）");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -ok-ret 数字,数値   ：正常終了判定戻り値リスト  （現在値={OkReturnCodeCsv}）");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -warn-ret 数字,数値 ：警告終了判定戻り値リスト  （現在値={WarnReturnCodeCsv}）");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -ng-ret 数字,数値   ：異常終了判定戻り値リスト  （現在値={ErrorReturnCodeCsv}）");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -ok-str 文字列      ：正常終了判定出力文字列    （現在値={OkMessageCsv}）");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -warn-str 文字列    ：警告終了判定出力文字列    （現在値={WarnMessageCsv}）");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -ng-str 文字列      ：異常終了判定出力文字列    （現在値={ErrorMessageCsv}）");
            _logger.WriteLine(MdlConst.LVL_NONE, $"   -echo-retcd         ：終了コード表示フラグ      （現在値={IsEchoRetcode}）");
            _logger.WriteLine(MdlConst.LVL_NONE, "");
            _logger.WriteLine(MdlConst.LVL_NONE, "Exit code              ：リモート実行コマンドの戻り値（-ret指定時を除く）");
            _logger.WriteLine(MdlConst.LVL_NONE, "");
        }

    }
}
