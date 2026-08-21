using System;
using System.Text;
using CmnClsLib.Class;
using rmtcmd.Class;
using Xunit;

namespace ClsWinRsTest
{
    public class ClsWinRsTests
    {
        private readonly ClsLogger _logger;

        public ClsWinRsTests()
        {
            _logger = new ClsLogger();
        }

        [Fact]
        public void Test_GetInvokeCommand_ExecModeNormal()
        {
            var winRs = new ClsWinRs(_logger);
            winRs.ExecMode = ClsWinRs.EXEC_MODE_NORMAL;
            string command = "hostname";
            string invokeCmd = winRs.GetInvokeCommand(command);

            Assert.Equal("hostname 2>&1", invokeCmd);
        }

        [Fact]
        public void Test_GetInvokeCommand_ExecModeExe()
        {
            var winRs = new ClsWinRs(_logger);
            winRs.ExecMode = ClsWinRs.EXEC_MODE_EXE;
            string command = "ping 127.0.0.1";
            string invokeCmd = winRs.GetInvokeCommand(command);

            Assert.Equal("ping 127.0.0.1", invokeCmd);
        }

        [Fact]
        public void Test_GetInvokeCommand_WithTeePath()
        {
            var winRs = new ClsWinRs(_logger);
            winRs.ExecMode = ClsWinRs.EXEC_MODE_NORMAL;
            winRs.TeePath = @"C:\tmp\log.txt";
            string command = "dir";
            string invokeCmd = winRs.GetInvokeCommand(command);

            Assert.Equal(@"dir 2>&1 | Tee-Object -FilePath C:\tmp\log.txt", invokeCmd);
        }

        [Fact]
        public void Test_IsRetryableError()
        {
            var winRs = new ClsWinRs(_logger);
            string retryableMsg = "EXCEPTION : リモート サーバー SERVERNAME への接続に失敗し、次のエラー メッセージが返されました: 削除の対象としてマークされているレジストリ キーに対して無効な操作を実行しようとしました。";
            string normalMsg = "EXCEPTION : アクセスが拒否されました。";

            Assert.True(winRs.IsRetryableError(retryableMsg));
            Assert.False(winRs.IsRetryableError(normalMsg));
        }

        [Fact]
        public void Test_ClearBuffer()
        {
            var winRs = new ClsWinRs(_logger);
            winRs.OutputBuffer.AppendLine("Test Output Line 1");
            winRs.OutputBuffer.AppendLine("Test Output Line 2");

            Assert.True(winRs.OutputBuffer.Length > 0);

            winRs.ClearBuffer();

            Assert.Equal(0, winRs.OutputBuffer.Length);
        }

        [Fact]
        public void Test_Properties()
        {
            var winRs = new ClsWinRs(_logger);
            winRs.RemoteHost = "192.168.1.10";
            winRs.Username = "admin";
            winRs.Password = "secret";
            winRs.WorkDir = @"C:\work";
            winRs.Port = 5986;

            Assert.Equal("192.168.1.10", winRs.RemoteHost);
            Assert.Equal("admin", winRs.Username);
            Assert.Equal("secret", winRs.Password);
            Assert.Equal(@"C:\work", winRs.WorkDir);
            Assert.Equal(5986, winRs.Port);
        }
    }
}
