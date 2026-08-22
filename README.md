# rmtcmd

## 事前作業
1. .NET SDKがインストールされていない場合はインストール：winget install -e --id Microsoft.DotNet.SDK.10
1. Github CLIがインストールされていない場合はインストール：winget install -e --id GitHub.cli
1. Powershellプロンプトを開く

## リポジトリ作成（未作成の場合）
```shell
# サインイン状態の確認
gh auth status
# 初回サインインしていない場合はサインイン
gh auth login
# 削除権限付与
gh auth refresh -h github.com -s delete_repo
# 作成
gh repo create rmtcmd --private
# 確認
gh repo list | Select-String rmtcmd
```

## リモートリポジトリ（mainブランチ）の取得
```shell
# CD
cd D:\Github\Projects
# フォルダが存在する場合は削除
if (Test-Path -Path .\rmtcmd){rm -Recurse -Force .\rmtcmd}
# クローン実行
git clone https://github.com/hide104y/rmtcmd.git
```

## リモートリポジトリ（mainブランチ）にREADME.mdが存在しない場合
```shell
# CD
cd D:\Github\Projects\rmtcmd
# ファイル作成
ruby -e "File.write('README.md', '# rmtcmd', encoding: 'UTF-8')"
# コミット
git add README.md
git commit -m "add README.md"
# プッシュ
git push -u origin main
# ブランチの一覧表示
git branch -a
```

## ブランチの作成
```shell
# ブランチをmainに切り替え・復元
git checkout main
# ブランチ作成
git checkout -b dotnet10
# 作成したブランチをリモートにプッシュ
git push -u origin dotnet10
```

## プロジェクトの作成
```shell
# コンソールアプリ：.net 10.0
cd D:\Github\Projects\rmtcmd
dotnet new console --framework net10.0 -o rmtcmd
```

## ソリューションファイルの作成
.\rmtcmd\rmtcmd.slnx
```xml
<Solution>
  <Project Path="rmtcmd/rmtcmd.csproj" />
  <Project Path="TestProject1/TestProject1.csproj" />
</Solution>
```

## プロジェクトファイルの修正
.\rmtcmd\rmtcmd\rmtcmd.csproj
```xml
<Project Sdk="Microsoft.NET.Sdk">

  <PropertyGroup>
    <OutputType>Exe</OutputType>
    <TargetFramework>net10.0</TargetFramework>
    <ImplicitUsings>enable</ImplicitUsings>
    <Nullable>enable</Nullable>
    <InvariantGlobalization>false</InvariantGlobalization>
    <AssemblyVersion>1.0.0.0</AssemblyVersion>
    <FileVersion>1.0.0.0</FileVersion>
  </PropertyGroup>

  <ItemGroup>
    <Reference Include="CmnClsLib">
      <HintPath>..\..\CmnClsLib\CmnClsLib\bin\Release\net10.0\CmnClsLib.dll</HintPath>
    </Reference>
    <PackageReference Include="Microsoft.Windows.Compatibility" Version="10.0.11" />
    <PackageReference Include="System.Management.Automation" Version="7.6.4" />
    <PackageReference Include="System.Security.Cryptography.Xml" Version="10.0.11" />
    <FluentValidationExcludedCultures Include="ar;el;he;hi;no;ro;sk;be;cs;cs-CZ;da;de;es;fa;fi;fr;it;ko;mk;nl;pl;pt;ru;sv;tr;uk;zh-CN;zh-CHS;zh-CHT;zh;zh-Hans;zh-Hant;pt-BR;">
        <InProject>false</InProject>
    </FluentValidationExcludedCultures>
  </ItemGroup>

  <Target Name="RemoveTranslationsAfterBuild" AfterTargets="AfterBuild">
    <RemoveDir Directories="@(FluentValidationExcludedCultures->'$(OutputPath)%(Filename)')" />
  </Target>

</Project>
```

## 依存パッケージ
```shell
# CD
cd D:\Github\Projects
# 依存プロジェクト参照の追加
dotnet add .\rmtcmd\rmtcmd\rmtcmd.csproj reference .\CmnClsLib\CmnClsLib\CmnClsLib.csproj
dotnet add .\rmtcmd\rmtcmd\rmtcmd.csproj reference .\CmnWinLib\CmnWinLib\CmnWinLib.csproj
# 依存パッケージのインストール
#  https://www.nuget.org/packages/microsoft.windows.compatibility#versions-body-tab
#  https://www.nuget.org/packages/System.Management.Automation/#versions-body-tab
dotnet add .\rmtcmd\rmtcmd\rmtcmd.csproj package System.Management.Automation --version 7.6.4
dotnet add .\rmtcmd\rmtcmd\rmtcmd.csproj package Microsoft.Windows.Compatibility --version 10.0.11
dotnet add .\rmtcmd\rmtcmd\rmtcmd.csproj package System.Security.Cryptography.Xml --version 10.0.11
```

## コーディング
(省略)

## AIレビュー
```shell
# CD
cd D:\Github\Projects
agy
.\rmtcmd\rmtcmd\Class\ClsAppArg.csに対して、スキル「source-review」を実行して
/clear
.\rmtcmd\rmtcmd\Program.csに対して、スキル「source-review」を実行して
/exit
```

## ビルド
```shell
# CD
cd D:\Github\Projects
# パッケージの最新化
dotnet package update --file .\rmtcmd\rmtcmd.slnx
# ビルド
dotnet build .\rmtcmd\rmtcmd.slnx -c Release -p:InvariantGlobalization=false
dotnet build .\rmtcmd\TestProject1\TestProject1.csproj
# 単体テスト
dotnet test .\rmtcmd\TestProject1\TestProject1.csproj
# Usage
rmtcmd\rmtcmd\bin\Release\net10.0\rmtcmd.exe -h
```

## リポジトリにコミット
```shell
cd D:\Github\Projects\rmtcmd
git switch dotnet10
git add .
git commit -m "README.mdの修正"
git push -u origin dotnet10
```

## デプロイ
```shell
dotnet publish .\rmtcmd\rmtcmd\rmtcmd.csproj -c Release -o D:\Github\bin.n10 -r win-x64 --self-contained=false -p:PublishSingleFile=false -p:PublishReadyToRun=false -p:PublishTrimmed=false -p:PublishAot=false -p:InvariantGlobalization=false
```

## リモートリポジトリの確認
- https://github.com/hide104y/rmtcmd/tree/dotnet10
<br>※GitHubの画面で「Compare & pull request」が表示されるが放置

## リモートリポジトリ（dotnet10ブランチ）の取得
```shell
# CD
cd D:\Github\Projects
# フォルダが存在する場合は削除
if (Test-Path -Path .\rmtcmd){rm -Recurse -Force .\rmtcmd}
# クローン実行
git clone -b dotnet10 https://github.com/hide104y/rmtcmd.git
```

## License
- These codes are licensed under CC0.
- http://creativecommons.org/publicdomain/zero/1.0/deed.ja
