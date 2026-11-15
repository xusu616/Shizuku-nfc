> 📦 **Release 下载提示**：请前往对应的 GitLab 仓库查看或下载 Release（GitHub 端暂不重复上传）👉 [https://gitlab.com/xusu616/Shizuku-nfc/-/releases](https://gitlab.com/xusu616/Shizuku-nfc/-/releases) （[GitLab 仓库主页](https://gitlab.com/xusu616/Shizuku-nfc)）

# Shizuku-nfc (支付宝“碰一下”免 Root NFC 事件模拟器)

基于 **ADB Shell / Shizuku 权限 (UID 2000)**，在免 Root 环境下模拟触发包含 2 条 Record 的支付宝“碰一下” NFC NDEF 事件，使支付宝能成功通过 `NdefMessage[]` 与 `Tag` 校验并跳转到对应的小程序界面。

---

## 🌟 核心原理解析

### 为什么常规方案不可行？
1. `am start -a android.nfc.action.NDEF_DISCOVERED -d "..."` 只会打开支付宝首页，因为支付宝 `20002153` 碰一下引擎强制校验 Intent 中的 `NfcAdapter.EXTRA_NDEF_MESSAGES` (`Parcelable[]`) 和 `NfcAdapter.EXTRA_TAG` (`Tag`)。
2. 直接 `am start` 跳转 `alipays://platformapi/startapp?appId=20002153...` 或 `20000067` 均会报错或 404。

### 本项目的破解方案
- 支付宝通过 `Tag` 对象校验 NFC 底层技术类型（要求包含 `TagTechnology.NFC_A` 与 `TagTechnology.NDEF`）以及 NDEF 负载。
- 本项目通过反射调用 `android.nfc.Tag.createMockTag(byte[] id, int[] techList, Bundle[] techExtras)` 构造合法虚拟标签：
  - `techList`: `TagTechnology.NFC_A (1)` 与 `TagTechnology.NDEF (6)`。
  - `techExtras`: NFC-A 注入 `sak (0x08)` 和 `atqa (0x44, 0x00)`；NDEF 注入 `ndefmsg` (`NdefMessage`)、`ndefmaxlength (1024)`、`ndefcardstate (2)`、`ndeftype (2)`。
- 通过 Shizuku 包装的系统服务 Binder (`ShizukuBinderWrapper(SystemServiceHelper.getSystemService("nfc"))`) 反射调用 `INfcAdapter.dispatch(Tag)`，完成系统级 NFC 硬件刷卡事件注入，系统将自动打包完整带校验的 Intent 唤醒支付宝并跳转至约炮营/碰一下小程序！

---

## 🚀 两种使用方式

### 方案一：免安装 App，电脑端一行命令触发（极简 Shell 方案）
直接使用项目内置的 `tools/` 单文件脚本：
- **Windows**:
  双击运行 `tools/trigger-nfc.bat` 即可一键推送 dex 并模拟触发。
- **命令行 / Linux / macOS**:
  ```bash
  adb push tools/nfc-dispatcher.dex /data/local/tmp/test_dispatch.dex
  adb push app/src/main/assets/xiaoyuanpao-extracted.ndef /data/local/tmp/test.ndef
  adb shell "export CLASSPATH=/data/local/tmp/test_dispatch.dex; app_process /data/local/tmp TestDispatch"
  ```

---

### 方案二：独立 Android App（基于 Shizuku & Jetpack Compose）
基于 Shizuku 权限与 Material 3 现代化 UI（样式参考并复刻自 `shizuku-ftp`）：
- **状态监控**：实时感知 Shizuku 服务运行状态与权限授权状态。
- **一键触发**：一键模拟触发碰一下事件，自动创建 Mock Tag 并通过 Shizuku 权限派发至系统 NFC 堆栈。
- **内置数据**：
  - **默认内置**：`https://render.alipay.com/p/s/ulink/axdk...` (axdk 统一链接) + `com.eg.android.AlipayGphone` (AAR)
  - **提取文件**：内置 `assets/xiaoyuanpao-extracted.ndef` (154 字节完整载荷)
  - **本地选择**：支持选择手机存储中的任意 `.ndef` 原始二进制文件进行解析
  - **自定义 Hex**：支持手动粘贴 HEX 格式的 NDEF 字节串
- **控制台日志**：实时显示反射注入各阶段的详细输出。

---

## 🛠️ 构建与编译
- **JDK**: Java 21
- **Gradle**: 9.3.0
- **Android SDK**: CompileSdk 36, MinSdk 26
- **构建命令**:
  ```bash
  ./gradlew assembleDebug
  ```
  产物位于 `app/build/outputs/apk/debug/app-debug.apk`。
