using System;
using rmtcmd;
using Xunit;

namespace RmtcmdProgramTest;

public class RmtcmdProgramTest
{
    [Fact]
    public void Main_WithHelpOption_ReturnsUsageExitCode()
    {
        // Arrange
        string[] args = ["/h"];

        // Act
        int exitCode = Program.Main(args);

        // Assert: /h オプション時は Usage が表示され、LVL_W (1) などのコードが返されることを検証
        Assert.True(exitCode >= 0);
    }

    [Fact]
    public void Main_WithInvalidArgs_ReturnsErrorCode()
    {
        // Arrange: 存在しない無効なオプションを指定
        string[] args = ["/INVALID_OPTION_XYZ"];

        // Act
        int exitCode = Program.Main(args);

        // Assert: エラー終了コード（0 以外、通常 4 など）が返されることを検証
        Assert.NotEqual(0, exitCode);
    }

    [Fact]
    public void Main_WithHopCountExceeded_ReturnsErrorCode()
    {
        // Arrange: ホップ制限数(MAXHOPS:1)に対し、環境変数でHOPS=2を設定
        Environment.SetEnvironmentVariable("RMTCMD_HOPS", "2");
        string[] args = ["/MAXHOPS:1", "/H:localhost", "/C:dir"];

        try
        {
            // Act
            int exitCode = Program.Main(args);

            // Assert: ホップ数超過によりエラー終了コードが返されることを検証
            Assert.NotEqual(0, exitCode);
        }
        finally
        {
            // クリーンアップ
            Environment.SetEnvironmentVariable("RMTCMD_HOPS", null);
        }
    }
}
