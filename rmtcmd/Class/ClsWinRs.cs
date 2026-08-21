using System;
using System.Management.Automation;
using System.Management.Automation.Runspaces;
using System.Collections.ObjectModel;
using System.Text;
using CmnClsLib.Class;
using CmnClsLib.Module;

// 2026/08/08 Gemini 3.6 Flash (High) Review & Modified

namespace rmtcmd.Class
{
    public partial class ClsWinRs
    {
        public const int EXEC_MODE_NORMAL = 0;
        public const int EXEC_MODE_CMD = 1;
        public const int EXEC_MODE_PS = 2;
        public const int EXEC_MODE_ES = 3;
        public const int EXEC_MODE_EC = 4;
        public const int EXEC_MODE_ECMD = 5;
        public const int EXEC_MODE_EXE = 6;

        private ClsLogger _logger;
        private ClsCmdStatus _cmdStatus;
        private StringBuilder _stringBuilder;
        private string _remoteHost = "localhost";
        private string _username = "";
        private string _password = "";
        private string _comSpec = "cmd";
        private string _workDir = "";
        private string _teePath = "";
        private string _addProcEnvPath = "";
        private bool _isLogWrite = false;
        private bool _isStackTrace = false;
        private int _execMode = EXEC_MODE_CMD;
        private int _verbose = 0;
        private int _port = 5985;
        private int _authMechanism = 0;
        private int _operationTimeout = 180;
        private int _openTimeout = 120;
        private Dictionary<string, string> _procEnvsDic = new();
        private bool _isException = false;
        private bool _isMoreRetry = false;
        private int _retryMax = 0;
        private int _retrySleep = 5;
        private bool _isRetryRemoteCmd = false;
        private int _cmdExitStatus = 0;

        /// <summary>
        /// ClsWinRs クラスの新しいインスタンスを初期化します。
        /// </summary>
        /// <param name="logger">ログ出力に使用する ClsLogger オブジェクト。</param>
        /// <example>
        /// <code>
        /// ClsLogger logger = new ClsLogger();
        /// ClsWinRs winRs = new ClsWinRs(logger);
        /// </code>
        /// </example>
        public ClsWinRs(ClsLogger logger)
        {
            _logger = logger;
            _cmdStatus = new(_logger);
            _stringBuilder = new();
            // MS932等有効化（.NET で使用できる標準の文字エンコーディングは、ASCII、UTF-7、UTF-8、UTF-16、UTF-32のみ）
            System.Text.Encoding.RegisterProvider(System.Text.CodePagesEncodingProvider.Instance);
            _comSpec = Environment.GetEnvironmentVariable("ComSpec") ?? "cmd";
        }

        public string RemoteHost { get { return _remoteHost; } set { _remoteHost = value; } }
        public string Username { get { return _username; } set { _username = value; } }
        public string Password { get { return _password; } set { _password = value; } }
        public string WorkDir { get { return _workDir; } set { _workDir = value; } }
        public string TeePath { get { return _teePath; } set { _teePath = value; } }
        public string AddProcEnvPath { get { return _addProcEnvPath; } set { _addProcEnvPath = value; } }
        public Dictionary<string, string> ProcEnvsDic { get { return _procEnvsDic; } set { _procEnvsDic = value; } }
        public int ExecMode { get { return _execMode; } set { _execMode = value; } }
        public int Verbose { get { return _verbose; } set { _verbose = value; } }
        public int Port { get { return _port; } set { _port = value; } }
        public int AuthMechanism { get { return _authMechanism; } set { _authMechanism = value; } }
        public int OperationTimeout { get { return _operationTimeout; } set { _operationTimeout = value; } }
        public int OpenTimeout { get { return _openTimeout; } set { _openTimeout = value; } }
        public bool IsLogWrite { get { return _isLogWrite; } set { _isLogWrite = value; } }
        public bool IsStackTrace { get { return _isStackTrace; } set { _isStackTrace = value; } }
        public StringBuilder OutputBuffer { get { return _stringBuilder; } set { _stringBuilder = value; } }
        public int RetryMax { get { return _retryMax; } set { _retryMax = value; } }
        public int RetrySleep { get { return _retrySleep; } set { _retrySleep = value; } }
        public bool IsRetryRemoteCmd { get { return _isRetryRemoteCmd; } set { _isRetryRemoteCmd = value; } }
        public string OkReturnCodeCsv { get { return _cmdStatus.OkReturnCodeCsv; } set { _cmdStatus.OkReturnCodeCsv = value; } }
        public string WarnReturnCodeCsv { get { return _cmdStatus.WarnReturnCodeCsv; } set { _cmdStatus.WarnReturnCodeCsv = value; } }
        public string ErrorReturnCodeCsv { get { return _cmdStatus.ErrorReturnCodeCsv; } set { _cmdStatus.ErrorReturnCodeCsv = value; } }
        public string OkMessageCsv { get { return _cmdStatus.OkMessageCsv; } set { _cmdStatus.OkMessageCsv = value; } }
        public string WarnMessageCsv { get { return _cmdStatus.WarnMessageCsv; } set { _cmdStatus.WarnMessageCsv = value; } }
        public string ErrorMessageCsv { get { return _cmdStatus.ErrorMessageCsv; } set { _cmdStatus.ErrorMessageCsv = value; } }
        public int WarnThreshold { get { return _cmdStatus.WarnThreshold; } set { _cmdStatus.WarnThreshold = value; } }
        public int ErrorThreshold { get { return _cmdStatus.ErrorThreshold; } set { _cmdStatus.ErrorThreshold = value; } }
        public bool IsErrorAtNegativeValue { get { return _cmdStatus.IsErrorAtNegativeValue; } set { _cmdStatus.IsErrorAtNegativeValue = value; } }
        public bool IsAlwaysNormal { get { return _cmdStatus.IsAlwaysNormal; } set { _cmdStatus.IsAlwaysNormal = value; } }
        public int ErrorCode { get { return _cmdStatus.ErrorCode; } set { _cmdStatus.ErrorCode = value; } }
        public int WarnCode { get { return _cmdStatus.WarnCode; } set { _cmdStatus.WarnCode = value; } }
        public int CmdExitStatus { get { return _cmdExitStatus; } set { _cmdExitStatus = value; } }
        public int MethodExitStatus { get { return _cmdStatus.MethodExitStatus; } set { _cmdStatus.MethodExitStatus = value; } }
        public int ReturnLevel { get { return _cmdStatus.ReturnLevel; } set { _cmdStatus.ReturnLevel = value; } }

        /// <summary>
        /// クラスおよび内部コマンドステータスオブジェクトの初期化を行います。
        /// </summary>
        /// <example>
        /// <code>
        /// winRs.Initialize();
        /// </code>
        /// </example>
        public void Initialize()
        {
            _cmdStatus.Verbose = _verbose;
            _cmdStatus.DebugLevel = MdlConst.LVL_NONE;
            _cmdStatus.Initialize();
        }

        /// <summary>
        /// 指定されたコマンドを実行します。設定された再試行回数およびエラー条件に従い自動リトライを行います。
        /// </summary>
        /// <param name="command">実行するコマンド文字列。</param>
        /// <returns>メソッドの最終的な終了ステータスコードを返します。</returns>
        /// <example>
        /// <code>
        /// int status = winRs.Execute("dir C:\\");
        /// </code>
        /// </example>
        public int Execute(string command)
        {
            _cmdExitStatus = -1;
            _cmdStatus.MethodExitStatus = (_cmdStatus.ErrorCode == MdlConst.INT_NULL ? MdlConst.LVL_E : _cmdStatus.ErrorCode);
            _cmdStatus.ReturnLevel = MdlConst.LVL_I;

            for (int i = 0; i < _retryMax + 1; i++)
            {
                if (_isLogWrite && _retryMax > 0 && _verbose > 0 && i > 0) _logger.WriteLine(MdlConst.LVL_NONE, $"== RETRY       = {i}/{_retryMax}");

                try
                {
                    ExecuteOnce(command);
                }
                catch (Exception ex)
                {
                    _logger.WriteLine(MdlConst.LVL_NONE, "EXCEPTION-1 : " + ex.Message);
                    if (_isStackTrace)
                    {
                        _logger.WriteLine(MdlConst.LVL_NONE, "");
                        _logger.WriteLine(MdlConst.LVL_NONE, ex.StackTrace ?? "");
                        _logger.WriteLine(MdlConst.LVL_NONE, "");
                    }
                }

                if (!_isException)
                {
                    if (_isRetryRemoteCmd)
                    {
                        if (_cmdStatus.ReturnLevel != MdlConst.LVL_E) break;
                    }
                    else
                    {
                        break;
                    }
                }
                // Wait
                if (i < _retryMax)
                {
                    _logger.WriteLine(MdlConst.LVL_NONE, "");
                    Thread.Sleep(_retrySleep * 1000);
                }
            }

            if (0 == _retryMax && _isMoreRetry)
            {
                try
                {
                    ExecuteOnce(command);
                }
                catch (Exception ex)
                {
                    _logger.WriteLine(MdlConst.LVL_NONE, "EXCEPTION-2 : " + ex.Message);
                    if (_isStackTrace)
                    {
                        _logger.WriteLine(MdlConst.LVL_NONE, "");
                        _logger.WriteLine(MdlConst.LVL_NONE, ex.StackTrace ?? "");
                        _logger.WriteLine(MdlConst.LVL_NONE, "");
                    }
                }
            }
            return _cmdStatus.MethodExitStatus;
        }

        /// <summary>
        /// 指定されたコマンドをリモート環境（WS-Man / PowerShell Runspace）で一度だけ実行します。
        /// </summary>
        /// <param name="command">実行するコマンド文字列。</param>
        /// <returns>コマンド実行結果のステータスレベルを返します。</returns>
        /// <example>
        /// <code>
        /// int returnLevel = winRs.ExecuteOnce("hostname");
        /// </code>
        /// </example>
        public int ExecuteOnce(string command)
        {
            Runspace? runspace = null;
            System.Security.SecureString securePassword = new System.Security.SecureString();
            WSManConnectionInfo? connectionInfo = null;
            _isException = false;
            _isMoreRetry = false;
            _cmdExitStatus = -1;
            _cmdStatus.MethodExitStatus = (_cmdStatus.ErrorCode == MdlConst.INT_NULL ? MdlConst.LVL_E : _cmdStatus.ErrorCode);
            _cmdStatus.ReturnLevel = MdlConst.LVL_I;
            _cmdStatus.ResetFlags();
            try
            {
                if (_isLogWrite && _verbose > 4) _logger.WriteLine(MdlConst.LVL_NONE, "[ExecuteOnce()][CP-001] START");
                Uri remoteComputerUri = new Uri("http://" + _remoteHost + ":" + _port + "/WSMAN");
                string shellUri = "http://schemas.microsoft.com/powershell/Microsoft.PowerShell";
                if (!string.IsNullOrEmpty(_password))
                {
                    foreach (char ch in _password.ToCharArray())
                    {
                        securePassword.AppendChar(ch);
                    }
                }
                PSCredential credential = new(_username, securePassword);

                if (_isLogWrite && _verbose > 4) _logger.WriteLine(MdlConst.LVL_NONE, "[ExecuteOnce()][CP-002] TRY : connectionInfo = new WSManConnectionInfo()");
                connectionInfo = new WSManConnectionInfo(remoteComputerUri, shellUri, credential);

                if (_operationTimeout > 0) connectionInfo.OperationTimeout = _operationTimeout * 1000;
                if (_openTimeout > 0) connectionInfo.OpenTimeout = _openTimeout * 1000;
                if (_isLogWrite && _verbose > 2) _logger.WriteLine(MdlConst.LVL_NONE, "OperationTimeout = " + connectionInfo.OperationTimeout + " (milisec) / OpenTimeout = " + connectionInfo.OpenTimeout + " (milisec)");

                connectionInfo.AuthenticationMechanism = _authMechanism switch
                {
                    0 => AuthenticationMechanism.Default,
                    1 => AuthenticationMechanism.Basic,
                    2 => AuthenticationMechanism.Negotiate,
                    3 => AuthenticationMechanism.NegotiateWithImplicitCredential,
                    4 => AuthenticationMechanism.Credssp,
                    5 => AuthenticationMechanism.Digest,
                    6 => AuthenticationMechanism.Kerberos,
                    _ => (AuthenticationMechanism)_authMechanism
                };

                if (_isLogWrite && _verbose > 4) _logger.WriteLine(MdlConst.LVL_NONE, "[ExecuteOnce()][CP-003] TRY : runspace = RunspaceFactory.CreateRunspace()");
                runspace = RunspaceFactory.CreateRunspace(connectionInfo);

                // OPEN
                if (_isLogWrite && _verbose > 4) _logger.WriteLine(MdlConst.LVL_NONE, "[ExecuteOnce()][CP-004] TRY : runspace.Open()");
                runspace.Open();

                // プロセス環境変数の設定
                if (_procEnvsDic.Count > 0)
                {
                    if (_isLogWrite && _verbose > 4) _logger.WriteLine(MdlConst.LVL_NONE, "[ExecuteOnce()][CP-005] TRY : 環境変数の設定");
                    foreach (KeyValuePair<string, string> envVar in _procEnvsDic)
                    {
                        if (string.Equals(envVar.Key, "+PATH", StringComparison.OrdinalIgnoreCase))
                        {
                            if (string.IsNullOrEmpty(_addProcEnvPath)) _addProcEnvPath = envVar.Value;
                        }
                        else
                        {
                            // 環境変数の設定
                            using (PowerShell powerShell = PowerShell.Create())
                            {
                                powerShell.Runspace = runspace;
                                powerShell.AddScript("[Environment]::SetEnvironmentVariable('" + envVar.Key + "', '" + envVar.Value + "', [System.EnvironmentVariableTarget]::Process)");
                                Collection<PSObject> results = powerShell.Invoke();
                                foreach (PSObject result in results)
                                {
                                    if (_isLogWrite) _logger.WriteLine(MdlConst.LVL_NONE, result.ToString());
                                }
                            }
                        }
                    }
                }

                // プロセス環境変数PATHの追加
                if (!string.IsNullOrEmpty(_addProcEnvPath))
                {
                    if (_isLogWrite && _verbose > 4) _logger.WriteLine(MdlConst.LVL_NONE, "[ExecuteOnce()][CP-006] TRY : プロセス環境変数PATHの追加");
                    string path = "";
                    // 環境変数PATHの取得
                    using (PowerShell powerShell = PowerShell.Create())
                    {
                        powerShell.Runspace = runspace;
                        powerShell.AddScript("[Environment]::GetEnvironmentVariable('PATH', [System.EnvironmentVariableTarget]::Process)");
                        Collection<PSObject> results = powerShell.Invoke();
                        foreach (PSObject result in results)
                        {
                            path = _addProcEnvPath + ";" + result.ToString();
                        }
                    }
                    // 環境変数PATHの設定
                    using (PowerShell powerShell = PowerShell.Create())
                    {
                        powerShell.Runspace = runspace;
                        powerShell.AddScript("[Environment]::SetEnvironmentVariable('PATH', '" + path + "', [System.EnvironmentVariableTarget]::Process)");
                        Collection<PSObject> results = powerShell.Invoke();
                        foreach (PSObject result in results)
                        {
                            if (_isLogWrite) _logger.WriteLine(MdlConst.LVL_NONE, result.ToString());
                        }
                    }
                }

                // カレントディレクトリの変更
                if (!string.IsNullOrEmpty(_workDir))
                {
                    if (_isLogWrite && _verbose > 4) _logger.WriteLine(MdlConst.LVL_NONE, "[ExecuteOnce()][CP-007] TRY : カレントディレクトリの変更");
                    // PSDriveの設定
                    using (PowerShell powerShell = PowerShell.Create())
                    {
                        powerShell.Runspace = runspace;
                        powerShell.AddScript("Set-Location " + _workDir);
                        Collection<PSObject> results = powerShell.Invoke();
                        foreach (PSObject result in results)
                        {
                            if (_isLogWrite) _logger.WriteLine(MdlConst.LVL_NONE, result.ToString());
                        }
                    }
                    // PSDriveの設定をFileSystemへの適用
                    using (PowerShell powerShell = PowerShell.Create())
                    {
                        powerShell.Runspace = runspace;
                        powerShell.AddScript("[System.IO.Directory]::SetCurrentDirectory((Get-Location -PSProvider FileSystem).Path)");
                        Collection<PSObject> results = powerShell.Invoke();
                        foreach (PSObject result in results)
                        {
                            if (_isLogWrite) _logger.WriteLine(MdlConst.LVL_NONE, result.ToString());
                        }
                    }
                }

                // コマンドの実行
                if (_isLogWrite && _verbose > 4) _logger.WriteLine(MdlConst.LVL_NONE, "[ExecuteOnce()][CP-011] TRY : コマンドの実行");
                using (PowerShell powerShell = PowerShell.Create())
                {
                    powerShell.Runspace = runspace;
                    powerShell.AddScript(GetInvokeCommand(command));
                    Collection<PSObject> results = powerShell.Invoke();
                    foreach (PSObject result in results)
                    {
                        string line = result.ToString();
                        if (_isLogWrite) _logger.WriteLine(MdlConst.LVL_NONE, line);
                        if (_stringBuilder is not null) _stringBuilder.AppendLine(line);
                        _cmdStatus.CheckMessageLine(line);
                    }
                }

                // 戻り値の取得
                if (_isLogWrite && _verbose > 4) _logger.WriteLine(MdlConst.LVL_NONE, "[ExecuteOnce()][CP-012] TRY : 戻り値の取得");
                using (PowerShell powerShell = PowerShell.Create())
                {
                    powerShell.Runspace = runspace;
                    powerShell.AddScript("$LASTEXITCODE");
                    Collection<PSObject> results = powerShell.Invoke();
                    foreach (PSObject result in results)
                    {
                        if (_isLogWrite && _verbose > 6) _logger.WriteLine(MdlConst.LVL_NONE, "戻り値取得 : " + result.ToString());
                        string returnCode = result.ToString();
                        if (!string.IsNullOrEmpty(returnCode) && MdlUtil.IsNumeric(returnCode)) _cmdExitStatus = int.Parse(returnCode);
                    }
                }
            }
            catch (Exception ex)
            {
                _cmdStatus.MethodExitStatus = (_cmdStatus.ErrorCode == MdlConst.INT_NULL ? MdlConst.LVL_E : _cmdStatus.ErrorCode);
                _cmdStatus.ReturnLevel = MdlConst.LVL_E;
                _isException = true;
                _isMoreRetry = IsRetryableError(ex.Message);
                _logger.WriteLine(MdlConst.LVL_NONE, "EXCEPTION : " + ex.Message);
                if (_isStackTrace)
                {
                    _logger.WriteLine(MdlConst.LVL_NONE, "");
                    _logger.WriteLine(MdlConst.LVL_NONE, ex.StackTrace ?? "");
                    _logger.WriteLine(MdlConst.LVL_NONE, "");
                }
            }
            finally
            {
                if (_isLogWrite && _verbose > 4) _logger.WriteLine(MdlConst.LVL_NONE, "[ExecuteOnce()][CP-021] TRY : 事後処理");
                if (null != runspace)
                {
                    try
                    {
                        runspace.Close();
                    }
                    catch (Exception ex)
                    {
                        _logger.WriteLine(MdlConst.LVL_NONE, "EXCEPTION-CLOSE : " + ex.Message);
                        if (_isStackTrace)
                        {
                            _logger.WriteLine(MdlConst.LVL_NONE, "");
                            _logger.WriteLine(MdlConst.LVL_NONE, ex.StackTrace ?? "");
                            _logger.WriteLine(MdlConst.LVL_NONE, "");
                        }
                        Thread.Sleep(3000);
                        try
                        {
                            runspace.Close();
                        }
                        catch { }
                    }
                    try
                    {
                        runspace.Dispose();
                    }
                    catch { }
                }
            }
            // コマンド終了コードの判定
            if (!_isException)
            {
                // コマンド終了コードチェック
                _cmdStatus.CheckCommandExitCode(_cmdExitStatus);

                // メソッド戻り値の評価
                _cmdStatus.Evaluate();

                if (_isLogWrite && _verbose > 0) _logger.WriteLine(MdlConst.LVL_NONE, "");
                if (_isLogWrite && _verbose > 0) _logger.WriteLine(MdlConst.LVL_NONE, "==> リモートコマンド終了コード = " + _cmdExitStatus + " => メソッド終了コード = " + _cmdStatus.MethodExitStatus);
            }

            return _cmdStatus.ReturnLevel;
        }

        /// <summary>
        /// コマンドを実行し、その標準出力・エラー出力結果を文字列テキストとして取得します。
        /// </summary>
        /// <param name="command">実行するコマンド文字列。</param>
        /// <returns>コマンド実行によって得られた出力テキスト文字列。</returns>
        /// <example>
        /// <code>
        /// string output = winRs.ExecuteAndReturnText("ipconfig /all");
        /// </code>
        /// </example>
        public string ExecuteAndReturnText(string command)
        {
            ClearBuffer();
            ExecuteOnce(command);
            return _stringBuilder.ToString().TrimEnd();
        }

        /// <summary>
        /// 設定された実行モード（ExecMode）に応じて、リモートで実行するためのコマンド文字列（エンコードやシェルラップを含む）を生成・取得します。
        /// </summary>
        /// <param name="remoteCommand">実行対象のリモートコマンド。</param>
        /// <returns>加工・構築された実行用コマンド文字列。</returns>
        /// <example>
        /// <code>
        /// string invokeCmd = winRs.GetInvokeCommand("Get-Process");
        /// </code>
        /// </example>
        public string GetInvokeCommand(string remoteCommand)
        {
            string trimmedCommand = remoteCommand.Trim();
            string invokeCommand = _execMode switch
            {
                EXEC_MODE_NORMAL => $"{trimmedCommand} 2>&1",
                EXEC_MODE_CMD => $"{_comSpec} /c {trimmedCommand} 2>&1",
                EXEC_MODE_EC => Encoding.GetEncoding(932).GetString(Convert.FromBase64String(trimmedCommand)).Trim(),
                EXEC_MODE_ECMD => $"{_comSpec} /c {Encoding.GetEncoding(932).GetString(Convert.FromBase64String(trimmedCommand)).Trim()} 2>&1",
                EXEC_MODE_PS => $"{_comSpec} /c powershell -encodedCommand \"{Convert.ToBase64String(Encoding.GetEncoding("UNICODE").GetBytes(trimmedCommand))}\" 2>&1",
                EXEC_MODE_ES => $"{_comSpec} /c powershell -encodedCommand \"{trimmedCommand}\" 2>&1",
                EXEC_MODE_EXE => trimmedCommand,
                _ => trimmedCommand
            };

            if (!string.IsNullOrEmpty(_teePath)) invokeCommand = $"{invokeCommand} | Tee-Object -FilePath {_teePath}";
            if (_isLogWrite && _verbose > 2) _logger.WriteLine(MdlConst.LVL_NONE, $"== INVOKE CMD  = {invokeCommand}");
            return invokeCommand;
        }

        [System.Text.RegularExpressions.GeneratedRegex(@"接続.*失敗.*削除の対象としてマークされているレジストリ.*キーに対して無効な操作を実行", System.Text.RegularExpressions.RegexOptions.IgnoreCase | System.Text.RegularExpressions.RegexOptions.Singleline)]
        private static partial System.Text.RegularExpressions.Regex RetryableErrorRegex();

        /// <summary>
        /// 発生した例外メッセージが、自動再試行（リトライ）の対象となるエラー条件に該当するかどうかを判定します。
        /// </summary>
        /// <param name="message">検証対象の例外メッセージ文字列。</param>
        /// <returns>再試行可能なエラーの場合は true。それ以外の場合は false。</returns>
        /// <example>
        /// <code>
        /// bool canRetry = winRs.IsRetryableError("接続に失敗しました...");
        /// </code>
        /// </example>
        public bool IsRetryableError(string message)
        {
            return RetryableErrorRegex().IsMatch(message);
        }

        /// <summary>
        /// 内部出力保持用バッファ（StringBuilder）の内容をクリアします。
        /// </summary>
        /// <example>
        /// <code>
        /// winRs.ClearBuffer();
        /// </code>
        /// </example>
        public void ClearBuffer()
        {
            try
            {
                _stringBuilder.Clear();
            }
            catch (Exception ex)
            {
                if (_isStackTrace)
                {
                    _logger.WriteLine(MdlConst.LVL_NONE, "");
                    _logger.WriteLine(MdlConst.LVL_NONE, ex.StackTrace ?? "");
                    _logger.WriteLine(MdlConst.LVL_NONE, "");
                }
                _stringBuilder.Length = 0;
            }
        }

    }
}
