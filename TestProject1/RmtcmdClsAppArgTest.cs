using System;
using System.Collections.Generic;
using CmnClsLib.Class;
using rmtcmd.Class;
using Xunit;

namespace RmtcmdClsAppArgTest
{
    public class RmtcmdClsAppArgTest
    {
        private ClsAppArg CreateSut()
        {
            var logger = new ClsLogger();
            return new ClsAppArg(logger);
        }

        [Fact]
        public void Constructor_InitializesDefaultValues()
        {
            // Arrange & Act
            var sut = CreateSut();

            // Assert
            Assert.Equal("localhost", sut.RemoteHost);
            Assert.Equal("hostname", sut.CmdPath);
            Assert.Equal(5985, sut.Port);
            Assert.Equal(180, sut.OperationTimeout);
            Assert.Equal(120, sut.OpenTimeout);
            Assert.Equal(ClsAppArg.NONE, sut.ResultJudgment);
            Assert.True(sut.IsLoopCheck);
        }

        [Fact]
        public void GetArgs_ValidBasicOptions_ReturnsTrueAndSetsProperties()
        {
            // Arrange
            var sut = CreateSut();
            string[] args = ["-h", "192.168.1.100", "-port", "5986", "-u", "testuser", "-p", "secret123", "-cwd", @"C:\Work", "-optimeout", "60"];

            // Act
            bool result = sut.GetArgs(args);

            // Assert
            Assert.True(result);
            Assert.Equal("192.168.1.100", sut.RemoteHost);
            Assert.Equal(5986, sut.Port);
            Assert.EndsWith("testuser", sut.Username);
            Assert.Equal("secret123", sut.Password);
            Assert.Equal(@"C:\Work", sut.WorkDir);
            Assert.Equal(60, sut.OperationTimeout);
        }

        [Fact]
        public void GetArgs_MissingPassword_ReturnsFalse()
        {
            // Arrange
            var sut = CreateSut();
            string[] args = ["-h", "localhost", "-u", "testuser"];

            // Act
            bool result = sut.GetArgs(args);

            // Assert
            Assert.False(result);
        }

        [Theory]
        [InlineData("-cmd", 1)]
        [InlineData("-ps", 2)]
        [InlineData("-ec", 4)]
        [InlineData("-ecmd", 5)]
        [InlineData("-es", 3)]
        [InlineData("-exe", 6)]
        public void GetArgs_ExecModes_SetsCorrectExecMode(string optionFlag, int expectedExecMode)
        {
            // Arrange
            var sut = CreateSut();
            string[] args = ["-p", "secret", optionFlag, "dir"];

            // Act
            bool result = sut.GetArgs(args);

            // Assert
            Assert.True(result);
            Assert.Equal(expectedExecMode, sut.ExecMode);
            Assert.Equal("dir", sut.CmdPath);
        }

        [Theory]
        [InlineData("none", ClsAppArg.NONE)]
        [InlineData("retcode", ClsAppArg.RETURN_CODE)]
        [InlineData("normal", ClsAppArg.ALWAYS_NORMAL)]
        [InlineData("warn", ClsAppArg.ALWAYS_WARN)]
        [InlineData("error", ClsAppArg.ALWAYS_ERROR)]
        public void GetArgs_ResultJudgmentOptions_SetsCorrectJudgment(string judgmentOption, int expectedJudgment)
        {
            // Arrange
            var sut = CreateSut();
            string[] args = ["-p", "pass", "-ret", judgmentOption];

            // Act
            bool result = sut.GetArgs(args);

            // Assert
            Assert.True(result);
            Assert.Equal(expectedJudgment, sut.ResultJudgment);
        }

        [Theory]
        [InlineData(ClsAppArg.NONE, "none：コマンドの戻り値を返却")]
        [InlineData(ClsAppArg.RETURN_CODE, "retcode：閾値による判定")]
        [InlineData(ClsAppArg.ALWAYS_NORMAL, "nomal：常に正常終了")]
        [InlineData(ClsAppArg.ALWAYS_WARN, "warn：常に警告終了")]
        [InlineData(ClsAppArg.ALWAYS_ERROR, "error：常に異常終了")]
        public void GetResultJudgmentText_ReturnsExpectedDescriptions(int mode, string expectedText)
        {
            // Arrange
            var sut = CreateSut();

            // Act
            string actual = sut.GetResultJudgmentText(mode);

            // Assert
            Assert.Equal(expectedText, actual);
        }

        [Fact]
        public void Usage_ExecutesWithoutException()
        {
            // Arrange
            var sut = CreateSut();

            // Act & Assert
            var ex = Record.Exception(() => sut.Usage());
            Assert.Null(ex);
        }
    }
}
