#!/system/bin/sh
# -------------------------------------------------------------
# 免 Root (ADB Shell / Shizuku rish, UID 2000) 模拟 NFC 碰一下触发脚本
# -------------------------------------------------------------
BASE_DIR="/data/local/tmp"
DEX_FILE="$BASE_DIR/nfc-dispatcher.dex"
NDEF_FILE="$BASE_DIR/test.ndef"

if [ ! -f "$DEX_FILE" ]; then
    echo "[!] 未在 $BASE_DIR 找到 nfc-dispatcher.dex，请先推送 dex 文件！"
    exit 1
fi

echo "[*] 启动 app_process 执行 NFC 注入..."
export CLASSPATH="$DEX_FILE"
app_process "$BASE_DIR" TestDispatch
