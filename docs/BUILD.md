# 构建与验证

## 1. 已验证的工具链

本项目在以下环境中完成 `lint / test / assembleDebug / assembleRelease`：

| 组件 | 版本 |
|---|---|
| OS | Debian GNU/Linux 12 (bookworm) |
| JDK | OpenJDK 17.0.21（Temurin 亦可） |
| Android SDK Platform | 35 |
| Android Build-Tools | 35.0.0 |
| Gradle | 8.11.1（Wrapper） |
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.0.21 |

> 环境不提供 `/dev/kvm`，无法运行 Android 模拟器，因此没有执行 instrumented / Compose UI 测试。

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
./gradlew assembleRelease
```

首次构建会下载 AGP、Compose、OkHttp 等依赖，需要网络。Gradle 配置已设置 `-Xmx2560m`，
若 CI 内存更小，可在 `gradle.properties` 中降低。

### 产物位置

```text
app/build/outputs/apk/debug/XingClass-arm64-v8a-debug.apk
app/build/outputs/apk/release/XingClass-arm64-v8a-release.apk
```

文件名由 `androidComponents.onVariants` 统一设置，避免出现 `app-release.apk` 这类不合规名称。

## 4. 架构与 ABI 校验

```bash
# APK 是否存在、是否是合法 APK
ls -l app/build/outputs/apk/release/XingClass-arm64-v8a-release.apk
$ANDROID_HOME/build-tools/35.0.0/aapt2 dump badging \
  app/build/outputs/apk/release/XingClass-arm64-v8a-release.apk | head

# 是否混入其他 ABI 的原生库（应为空）
unzip -l app/build/outputs/apk/release/XingClass-arm64-v8a-release.apk | grep ' lib/' || echo "no native libs"
```

当前 APK 携带的原生库只有 Compose 依赖提供的
`lib/arm64-v8a/libandroidx.graphics.path.so`，没有其他 ABI。`abiFilters = ["arm64-v8a"]`
同时作为未来依赖引入其他 ABI 时的护栏；APK 的 `native-code` 字段实测为 `'arm64-v8a'`。

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

四个值缺一不可。缺少任一项时，`assembleRelease` 会回退到 **debug 签名**，保证产物可安装，
但 Release workflow 会输出警告；正式分发前必须配置真实 keystore。

## 6. GitHub Actions

- `debug.yml`：push / PR / 手动触发；lint → test → assembleDebug → 上传 artifact；不创建 Release。
- `release.yml`：**仅 `workflow_dispatch`**；输入版本号 `vX.Y.Z`；lint → test → assembleRelease →
  APK 校验 → 创建 GitHub Release 并上传 APK + SHA-256。

## 7. 常见问题

| 现象 | 处理 |
|---|---|
| `SDK location not found` | 设置 `ANDROID_HOME` 或 `local.properties` |
| `Unsupported class file major version` | 使用 JDK 17，不要用 JDK 21 以下的错误组合 |
| Gradle 下载失败 | 配置镜像或代理；不要伪造构建成功 |
| 内存不足 | 降低 `org.gradle.jvmargs` / `org.gradle.workers.max` |
| Lint 失败 | 先看报告 `app/build/reports/lint-results-debug.html`；不要直接关闭检查 |
| Release 签名警告 | 属预期回退；配置签名 Secrets 后消失 |

## 8. 构建失败的处理原则

如果依赖无法下载或 SDK 缺失：

- 不得声称构建成功；
- 明确报告阻塞原因；
- 保留完整构建配置；
- 不生成伪造 APK。

## 9. 本次构建记录

时间：2026-11-06 · 环境：Debian 12 / JDK 17.0.21 / Gradle 8.11.1 / AGP 8.7.3 / SDK 35。

| 命令 | 结果 | 说明 |
|---|---|---|
| `./gradlew lint` | PASS | 0 issues（`app/build/reports/lint-results-debug.html`） |
| `./gradlew test` | PASS | 81 tests / 0 failures |
| `./gradlew assembleDebug` | PASS | 11 MB，arm64-v8a |
| `./gradlew assembleRelease` | PASS | 1.3 MB，R8 + resource shrinking，arm64-v8a |

Release 签名说明：本次未提供 keystore，按设计回退为 debug 签名（`CN=Android Debug`）。
这不影响 APK 结构/ABI 验证，但该文件不能作为正式发行包。
