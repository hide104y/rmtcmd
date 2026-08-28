# rmtcmd

## 事前作業
1. JDKがインストールされていない場合はインストール：winget install -e --id Amazon.Corretto.17.JDK
1. Github CLIがインストールされていない場合はインストール：winget install -e --id GitHub.cli
1. Powershellプロンプトを開く

## 変数設定
```shell
$base_dir = "D:\Github\workspace.jre11"
$branch = "java11"
$solution = "rmtcmd"
$groupid="tool"
```

## リポジトリ作成（未作成の場合）
```shell
# サインイン状態の確認
gh auth status
# 初回サインインしていない場合はサインイン
gh auth login
# 削除権限付与
gh auth refresh -h github.com -s delete_repo
# リポジトリの削除
gh repo delete hide104y/${solution} --yes
# リポジトリの作成
gh repo create ${solution} --private
# 確認
gh repo list | Select-String ${solution}
```

## リモートリポジトリ（mainブランチ）の取得
```shell
# CD
cd ${base_dir}
# フォルダが存在する場合は削除
if (Test-Path -Path ".\${solution}"){rmdir ".\${solution}"}
# クローン実行
git clone https://github.com/hide104y/${solution}.git
```

## リモートリポジトリ（mainブランチ）にREADME.mdが存在しない場合
```shell
# CD
cd ${base_dir}\${solution}
# ファイル作成
$enc = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText("${base_dir}\${solution}\README.md", "# ${solution}", $enc)
cat "${base_dir}\${solution}\README.md"
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
git checkout -b ${branch}
# 作成したブランチをリモートにプッシュ
git push -u origin ${branch}
```

## Java、Mavenの切り替え
```shell
# PATHの設定
$Env:JAVA_HOME="${Env:USERPROFILE}\App\Java\jdkjdk11.0.29_7"
$Env:MAVEN_HOME="${Env:USERPROFILE}\App\Maven\apache-maven-3.9.11"
$Env:PATH="${Env:JAVA_HOME}\bin;${Env:MAVEN_HOME}\bin;${Env:PATH}"
# 確認
java -version
mvn -version
```

## MAVENプロジェクトの作成
```shell
mvn archetype:generate `
-DarchetypeArtifactId=maven-archetype-quickstart `
-DinteractiveMode=false `
-DgroupId=${groupid} `
-DartifactId=${solution}
```

## 手動ビルドが必要な依存ライブラリー
- 次をMAVENローカルリポジトリに「mvn clean install」して下さい
  - git clone -b java11 https://github.com/hide104y/CmnClsLib.git

## 依存ライブラリー一覧
```shell
PS D:\Github\workspace.jre11\rmtcmd> mvn dependency:tree
Picked up JAVA_TOOL_OPTIONS: -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8
[INFO] Scanning for projects...
[INFO]
[INFO] ----------------------------< tool:rmtcmd >-----------------------------
[INFO] Building rmtcmd 1.0
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO]
[INFO] --- dependency:3.7.0:tree (default-cli) @ rmtcmd ---
[INFO] tool:rmtcmd:jar:1.0
[INFO] +- junit:junit:jar:4.13.2:test
[INFO] |  \- org.hamcrest:hamcrest-core:jar:1.3:compile
[INFO] +- io.cloudsoft.windows:winrm4j:jar:0.12.3:compile
[INFO] |  \- io.cloudsoft.windows:winrm4j-client:jar:0.12.3:compile
[INFO] |     +- commons-io:commons-io:jar:2.4:compile
[INFO] |     \- org.xmlunit:xmlunit-matchers:jar:2.3.0:compile
[INFO] |        \- org.xmlunit:xmlunit-core:jar:2.3.0:compile
[INFO] +- org.apache.cxf:cxf-rt-frontend-jaxws:jar:3.6.4:compile
[INFO] |  +- xml-resolver:xml-resolver:jar:1.2:compile
[INFO] |  +- org.apache.cxf:cxf-core:jar:3.6.4:compile
[INFO] |  |  +- jakarta.annotation:jakarta.annotation-api:jar:1.3.5:compile
[INFO] |  |  +- com.fasterxml.woodstox:woodstox-core:jar:6.6.2:compile
[INFO] |  |  |  \- org.codehaus.woodstox:stax2-api:jar:4.2.2:compile
[INFO] |  |  \- org.apache.ws.xmlschema:xmlschema-core:jar:2.3.1:compile
[INFO] |  +- org.apache.cxf:cxf-rt-bindings-soap:jar:3.6.4:compile
[INFO] |  |  +- org.apache.cxf:cxf-rt-wsdl:jar:3.6.4:compile
[INFO] |  |  |  \- wsdl4j:wsdl4j:jar:1.6.3:compile
[INFO] |  |  \- org.apache.cxf:cxf-rt-databinding-jaxb:jar:3.6.4:compile
[INFO] |  +- org.apache.cxf:cxf-rt-bindings-xml:jar:3.6.4:compile
[INFO] |  +- org.apache.cxf:cxf-rt-frontend-simple:jar:3.6.4:compile
[INFO] |  \- org.apache.cxf:cxf-rt-ws-addr:jar:3.6.4:compile
[INFO] |     \- org.apache.cxf:cxf-rt-ws-policy:jar:3.6.4:compile
[INFO] |        \- org.apache.neethi:neethi:jar:3.2.0:compile
[INFO] +- org.apache.cxf:cxf-rt-transports-http-hc:jar:3.6.4:compile
[INFO] |  +- org.apache.cxf:cxf-rt-transports-http:jar:3.6.4:compile
[INFO] |  +- org.slf4j:slf4j-api:jar:1.7.36:compile
[INFO] |  +- org.slf4j:jcl-over-slf4j:jar:1.7.36:compile
[INFO] |  \- org.apache.httpcomponents:httpcore-nio:jar:4.4.16:compile
[INFO] +- org.apache.httpcomponents:httpclient:jar:4.5.14:compile
[INFO] |  +- org.apache.httpcomponents:httpcore:jar:4.4.16:compile
[INFO] |  +- commons-logging:commons-logging:jar:1.2:compile
[INFO] |  \- commons-codec:commons-codec:jar:1.11:compile
[INFO] +- org.apache.httpcomponents:httpasyncclient:jar:4.1.5:compile
[INFO] +- jakarta.xml.ws:jakarta.xml.ws-api:jar:2.3.3:compile
[INFO] |  +- jakarta.xml.bind:jakarta.xml.bind-api:jar:2.3.3:compile
[INFO] |  |  \- jakarta.activation:jakarta.activation-api:jar:1.2.2:compile
[INFO] |  +- jakarta.xml.soap:jakarta.xml.soap-api:jar:1.4.2:compile
[INFO] |  \- jakarta.jws:jakarta.jws-api:jar:2.1.0:compile
[INFO] +- org.glassfish.jaxb:jaxb-runtime:jar:2.3.9:compile
[INFO] |  +- org.glassfish.jaxb:txw2:jar:2.3.9:compile
[INFO] |  +- com.sun.istack:istack-commons-runtime:jar:3.0.12:compile
[INFO] |  \- com.sun.activation:jakarta.activation:jar:1.2.2:runtime
[INFO] +- com.sun.xml.messaging.saaj:saaj-impl:jar:1.5.3:compile
[INFO] |  \- org.jvnet.staxex:stax-ex:jar:1.8.3:compile
[INFO] +- org.ow2.asm:asm:jar:9.10.1:compile
[INFO] +- org.slf4j:slf4j-simple:jar:1.7.36:compile
[INFO] \- tool.cmnclslib:CmnClsLib:jar:1.0:compile
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  1.335 s
[INFO] Finished at: 2026-08-19T23:41:00+09:00
[INFO] ------------------------------------------------------------------------
PS D:\Github\workspace.jre11\rmtcmd>
```

## コーディング
- pom.xml
- src\main\java\tool\rmtcmd.java
- src\main\java\tool\ClsAppArg.java
- src\main\java\tool\ClsWinRs.java

## AIレビュー
```shell
# CD
cd ${base_dir}
agy
「.\rmtcmd\src」配下のソースに対して、スキル「source-review」を実行して
/exit
```

## ビルド
```shell
# CD
cd ${base_dir}\${solution}
# クリーン
mvn clean
# コンパイル
mvn compile
# 外部ライブラリの非推奨メソッド使用有無確認
mvn clean compile "-Dmaven.compiler.showDeprecation=true"
# テスト
mvn test
# jar化
mvn package "-Dmaven.test.skip=true"
# 依存ライブラリの更新確認
mvn versions:display-dependency-updates
# プロジェクトがどのような依存関係を持っているかをツリーで確認
mvn dependency:tree
# ローカルリポジトリにインストール
mvn clean install "-Dmaven.test.skip=true"
# Usage
java -jar target\rmtcmd-1.0-jre11.jar -h
```

## リポジトリにコミット
```shell
# CD
cd ${base_dir}\${solution}
# ブランチ切り替え
git switch ${branch}
# 修正ファイルの追加
git add .
git ls-files
# コミット
git commit -m "★修正コメントを記載★"
# 状態確認
git status
# リモートの変更を取得し、ローカルのコミットをその上に再配置
# git pull --rebase origin ${branch}
# リモートプッシュ
git push -u origin ${branch}
# chromeでリモートブランチへ接続
Invoke-Expression "C:\Progra~1\Google\Chrome\Application\chrome.exe https://github.com/hide104y/${solution}/tree/${branch}"

## リモートリポジトリを確認
- https://github.com/hide104y/rmtcmd/tree/java11
<br>※GitHubの画面で「Compare & pull request」が表示されるが放置

## リモートリポジトリ（指定ブランチ）の取得
```shell
# CD
cd ${base_dir}
# フォルダが存在する場合は削除
if (Test-Path -Path ".\${solution}"){rmdir ".\${solution}"}
# クローン実行
git clone -b ${branch} https://github.com/hide104y/${solution}.git
```

## License
- These codes are licensed under CC0.
- http://creativecommons.org/publicdomain/zero/1.0/deed.ja
