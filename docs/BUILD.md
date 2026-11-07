# 构建与验证

## 1. 工具链

| 组件 | 版本 |
|---|---|
| JDK | 17（Temurin 或 OpenJDK） |
| Android SDK Platform | 35 |
| Android Build-Tools | 35.0.0 |
| Gradle | 8.11.1（Wrapper） |
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.0.21 |

> 构建环境不提供 `/dev/kvm`，无法运行 Android 模拟器，因此没有执行 instrumented / Compose UI 测试。

## 2. 本地环境准备

1. 安装 JDK 17，设置 `JAVA_HOME`。
2. 安装 Android SDK（命令行工具即可）：

```bash
sdkmanager --sdk_root=$ANDROID_HOME \
  "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

3. 设置 `ANDROID_HOME` / `ANDROID_SDK_ROOT`，或在 `android/local.properties` 写入：

```properties
sdk.dir=/path/to/android-sdk
```

`local.properties` 已被 `.gitignore` 排除。

## 3. 构建命令

```bash
cd android
./gradlew lint
./gradlew test
./gradlew assembleDebug
./gradlew assembleRelease   # 必须配置签名，见第 5 节

# 可选：实网验证（凭据只放在环境变量，测试默认跳过）
XINGCLASS_TEST_ACCOUNT=... XINGCLASS_TEST_PASSWORD=... \
  ./gradlew testDebugUnitTest --tests '*LiveChaoxingApiTest' --no-daemon
```

首次构建会下载 AGP、Compose、OkHttp 等依赖，需要网络。Gradle 配置已设置 `-Xmx2560m`，
若 CI 内存更小，可在 `gradle.properties` 中降低。

Gradle 已启用 parallel、build cache 与 configuration cache；如遇第三方插件不兼容，
可用 `--no-configuration-cache` 单次排查，而不是直接关闭全局配置。

### 产物位置

```text
app/build/outputs/apk/debug/XingClass-arm64-v8a-debug.apk
app/build/outputs/apk/release/XingClass-arm64-v8a-release.apk
```

文件名由 `applicationVariants` 统一设置，避免出现 `app-release.apk` 这类不合规名称。

## 4. 产物与 ABI 校验

```bash
# APK 是否存在、是否是合法 APK
ls -l app/build/outputs/apk/release/XingClass-arm64-v8a-release.apk
$ANDROID_HOME/build-tools/35.0.0/aapt2 dump badging \
  app/build/outputs/apk/release/XingClass-arm64-v8a-release.apk | sed -n '1,5p'

# 列出所有原生库（当前应只有 lib/arm64-v8a/libandroidx.graphics.path.so）
unzip -l app/build/outputs/apk/release/XingClass-arm64-v8a-release.apk \
  | awk '$4 ~ /^lib\// { print $4 }'

# 非 arm64-v8a 的原生库必须为空
unzip -l app/build/outputs/apk/release/XingClass-arm64-v8a-release.apk \
  | awk '$4 ~ /^lib\// && $4 !~ /^lib\/arm64-v8a\// { print $4 }'
```

当前 APK 携带的原生库只有 AndroidX 的 `lib/arm64-v8a/libandroidx.graphics.path.so`，
没有其他 ABI。`abiFilters += "arm64-v8a"` 作为未来依赖引入其他 ABI 时的护栏；
CI 会对非 `lib/arm64-v8a/` 的原生库直接失败。

## 5. 签名

Release 签名不写死在仓库中。

本地（`android/keystore.properties`，已 git 忽略）：

```properties
storeFile=/absolute/path/xingclass-release.jks
storePassword=...
keyAlias=xingclass
keyPassword=...
```

CI（GitHub Secrets）：

```text
RELEASE_KEYSTORE_BASE64     # base64 < xingclass-release.jks
RELEASE_KEYSTORE_PASSWORD
RELEASE_KEY_ALIAS
RELEASE_KEY_PASSWORD
```

四个值缺一不可。缺少任一配置时，`assembleRelease` **直接失败**，不会回退到 debug 签名，
也不会产出可被误认为正式发行包的 APK。Release workflow 在构建前同样会检查四个 Secrets。

签名相关环境变量：

```text
XINGCLASS_KEYSTORE_PATH
XINGCLASS_KEYSTORE_PASSWORD
XINGCLASS_KEY_ALIAS
XINGCLASS_KEY_PASSWORD
```

`android/keystore.properties` 与 `XINGCLASS_*` 环境变量二选一即可；环境变量优先。

## 6. GitHub Actions

- `debug.yml`：push / PR / 手动触发；lint → test → assembleDebug → ABI 校验 → 上传 artifact；
  不创建 Release。
- `release.yml`：**仅 `workflow_dispatch`**；输入版本号；校验签名 Secrets 与版本规则；
  lint → test → assembleRelease → APK 校验 → 创建 GitHub Release，上传 APK + SHA-256。

Release 规则：

1. 输入版本必须等于 `v` + `android/app/build.gradle.kts` 中的 `versionName`。
2. `versionCode` 必须大于最近一个 `v*` tag 中记录的 `versionCode`；没有历史 tag 时跳过。
3. 必须配置四个签名 Secrets；缺少任一配置时在构建前失败。
4. job 使用 `environment: release` 作为审批卡点；需要在仓库 Settings → Environments
   创建该环境并配置审批人。
5. 两个 workflow 都设置了 `timeout-minutes`，避免 Gradle 挂起占满 runner。

## 7. 常见问题

| 现象 | 处理 |
|---|---|
| `SDK location not found` | 设置 `ANDROID_HOME` 或 `local.properties` |
| `Unsupported class file major version` | 使用 JDK 17 |
| Gradle 下载失败 | 配置镜像或代理；不要伪造构建成功 |
| 内存不足 | 降低 `org.gradle.jvmargs` / `org.gradle.workers.max` |
| `Release signing is required` | 配置 `XINGCLASS_*` 或 `android/keystore.properties` |
| Lint 失败 | 先看报告 `app/build/reports/lint-results-debug.html`；不要直接关闭检查 |
| ABI 校验失败 | 检查新依赖是否带入非 arm64-v8a 的 `lib/` |

## 8. 构建失败的处理原则

如果依赖无法下载或 SDK 缺失：

- 不得声称构建成功；
- 明确报告阻塞原因；
- 保留完整构建配置；
- 不生成伪造 APK。
