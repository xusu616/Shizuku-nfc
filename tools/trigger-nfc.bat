@echo off
chcp 65001 >nul
echo ===================================================
echo [Shizuku-nfc] 一键免 Root 模拟支付宝“碰一下” NFC 触发
echo ===================================================

where adb >nul 2>nul
if %errorlevel% neq 0 (
    echo [!] 未检测到 adb 命令，请确保 Android SDK platform-tools 已加入系统 PATH！
    pause
    exit /b 1
)

echo [*] 正在推送 nfc-dispatcher.dex 与 目标 NDEF 数据...
adb push "%~dp0nfc-dispatcher.dex" /data/local/tmp/test_dispatch.dex
if exist "%~dp0..\app\src\main\assets\xiaoyuanpao-extracted.ndef" (
    adb push "%~dp0..\app\src\main\assets\xiaoyuanpao-extracted.ndef" /data/local/tmp/test.ndef
)

echo [*] 正在调用 INfcAdapter.dispatch 触发碰一下事件...
adb shell "export CLASSPATH=/data/local/tmp/test_dispatch.dex; app_process /data/local/tmp TestDispatch"

echo.
echo [*] 执行完毕，请查看手机端支付宝是否已自动拉起碰一下界面！
pause
