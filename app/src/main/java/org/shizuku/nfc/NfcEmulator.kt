package org.shizuku.nfc

import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.util.Log
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.io.File
import java.io.InputStream
import java.lang.reflect.Method
import java.nio.charset.StandardCharsets

object NfcEmulator {

    private const val TAG = "NfcEmulator"

    const val DEFAULT_ALIPAY_URL =
        "https://render.alipay.com/p/s/ulink/axdk?s=dc&scheme=alipay%3A%2F%2Fnfc%2Fapp%3Fid%3D20002153%26t%3Dsc0756bctbf4"
    const val DEFAULT_ALIPAY_PKG = "com.eg.android.AlipayGphone"

    /**
     * 构建默认的支付宝“碰一下” 2 条 Record 的 NdefMessage
     * Record 1: TNF_WELL_KNOWN, RTD_URI (0x04 = https://)
     * Record 2: TNF_EXTERNAL_TYPE, android.com:pkg (AAR)
     */
    fun createDefaultAlipayNdefMessage(): NdefMessage {
        val uriRecord = NdefRecord.createUri(DEFAULT_ALIPAY_URL)
        val aarRecord = NdefRecord.createApplicationRecord(DEFAULT_ALIPAY_PKG)
        return NdefMessage(arrayOf(uriRecord, aarRecord))
    }

    /**
     * 从原始二进制字节解析 NdefMessage
     */
    fun parseNdefBytes(bytes: ByteArray): NdefMessage {
        return NdefMessage(bytes)
    }

    /**
     * 从 HEX 字符串解析 NdefMessage
     */
    fun parseNdefHex(hex: String): NdefMessage {
        val clean = hex.replace("[^0-9a-fA-F]".toRegex(), "")
        require(clean.length % 2 == 0) { "HEX 字符串长度必须为偶数" }
        val data = ByteArray(clean.length / 2)
        for (i in 0 until clean.length step 2) {
            data[i / 2] = ((Character.digit(clean[i], 16) shl 4) +
                    Character.digit(clean[i + 1], 16)).toByte()
        }
        return NdefMessage(data)
    }

    /**
     * 从 Assets 读取内置的 .ndef 文件
     */
    fun loadFromAssets(context: Context, filename: String = "xiaoyuanpao-extracted.ndef"): NdefMessage {
        context.assets.open(filename).use { stream: InputStream ->
            val bytes = stream.readBytes()
            return NdefMessage(bytes)
        }
    }

    /**
     * 通过反射调用 android.nfc.Tag.createMockTag 构造虚拟 Tag
     * 兼容 Android 12+ (4 参数) 和旧版 Android (3 参数)
     */
    fun createMockTag(ndefMessage: NdefMessage): Tag {
        // 虚拟 NFC-A UID
        val uid = byteArrayOf(0x04.toByte(), 0x12.toByte(), 0x34.toByte(), 0x56.toByte(), 0x78.toByte(), 0x9A.toByte(), 0xBC.toByte())

        // TechList: NFC_A = 1, NDEF = 6
        val techList = intArrayOf(1, 6)
        val techExtras = arrayOfNulls<Bundle>(2)

        // NFC_A extras (sak + atqa)
        val nfcAExtra = Bundle()
        nfcAExtra.putShort("sak", 0x08.toShort())
        nfcAExtra.putByteArray("atqa", byteArrayOf(0x44.toByte(), 0x00.toByte()))
        techExtras[0] = nfcAExtra

        // NDEF extras
        val ndefExtra = Bundle()
        ndefExtra.putParcelable("ndefmsg", ndefMessage)
        ndefExtra.putInt("ndefmaxlength", 1024)
        ndefExtra.putInt("ndefcardstate", 2) // Ndef.NDEF_MODE_READ_ONLY = 2
        ndefExtra.putInt("ndeftype", 2)      // Ndef.TYPE_2 = 2
        techExtras[1] = ndefExtra

        return try {
            // Android 12+ (API 31+) 签名: (byte[] id, int[] techList, Bundle[] techListExtras, long cookie)
            val method: Method = Tag::class.java.getDeclaredMethod(
                "createMockTag",
                ByteArray::class.java,
                IntArray::class.java,
                Array<Bundle>::class.java,
                Long::class.javaPrimitiveType
            )
            method.isAccessible = true
            method.invoke(null, uid, techList, techExtras, 0L) as Tag
        } catch (e: NoSuchMethodException) {
            // Android 11 及以下签名: (byte[] id, int[] techList, Bundle[] techListExtras)
            val method: Method = Tag::class.java.getDeclaredMethod(
                "createMockTag",
                ByteArray::class.java,
                IntArray::class.java,
                Array<Bundle>::class.java
            )
            method.isAccessible = true
            method.invoke(null, uid, techList, techExtras) as Tag
        }
    }

    /**
     * 核心派发逻辑：
     * 利用 ShizukuBinderWrapper 包装系统的 "nfc" Binder，
     * 通过反射将 mockTag 注入 android.nfc.INfcAdapter.dispatch(Tag)
     */
    fun dispatchNfcTagViaShizuku(tag: Tag, logger: (String) -> Unit): Result<Unit> {
        return runCatching {
            logger("1. 正在获取系统 nfc Binder 服务...")
            val rawBinder = SystemServiceHelper.getSystemService("nfc")
                ?: throw IllegalStateException("未能获取到系统 nfc 服务 Binder，请确认设备支持 NFC 且服务已启动")
            
            logger("2. 使用 ShizukuBinderWrapper 进行特权凭证包装...")
            val privilegedBinder: IBinder = ShizukuBinderWrapper(rawBinder)

            logger("3. 反射获取 INfcAdapter\$Stub 代理...")
            val stubClass = Class.forName("android.nfc.INfcAdapter\$Stub")
            val asInterfaceMethod = stubClass.getMethod("asInterface", IBinder::class.java)
            val nfcAdapterProxy = asInterfaceMethod.invoke(null, privilegedBinder)
                ?: throw IllegalStateException("未能实例化 INfcAdapter 代理对象")

            logger("4. 反射查找 INfcAdapter.dispatch(Tag) 方法...")
            val dispatchMethod = nfcAdapterProxy.javaClass.getMethod("dispatch", Tag::class.java)

            logger("5. 调用 dispatch(tag) 注入系统 NFC 事件...")
            dispatchMethod.invoke(nfcAdapterProxy, tag)
            logger("✓ 派发成功！系统已广播 NFC NDEF 事件！")
        }
    }

    /**
     * 备用方案：通过 Context 直接发送带 EXTRA_TAG 与 EXTRA_NDEF_MESSAGES 的显式/隐式 Intent
     */
    fun dispatchViaIntentDirect(context: Context, tag: Tag, ndefMessage: NdefMessage, logger: (String) -> Unit): Result<Unit> {
        return runCatching {
            logger("执行备用方案：构造显式 NDEF_DISCOVERED Intent...")
            val firstRecord = ndefMessage.records.firstOrNull()
            val uri = firstRecord?.toUri()

            val intent = Intent(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                data = uri
                setPackage(DEFAULT_ALIPAY_PKG)
                putExtra(NfcAdapter.EXTRA_TAG, tag)
                putExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, arrayOf<Parcelable>(ndefMessage))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            logger("✓ 备用 Intent 发送完成！")
        }
    }

    /**
     * 解析 NDEF Record 便于在 UI 界面可视化呈现
     */
    data class RecordInfo(
        val index: Int,
        val tnfName: String,
        val type: String,
        val payloadPreview: String,
        val length: Int
    )

    fun inspectRecords(message: NdefMessage): List<RecordInfo> {
        return message.records.mapIndexed { idx, r ->
            val tnfStr = when (r.tnf) {
                NdefRecord.TNF_WELL_KNOWN -> "WELL_KNOWN (0x01)"
                NdefRecord.TNF_MIME_MEDIA -> "MIME_MEDIA (0x02)"
                NdefRecord.TNF_ABSOLUTE_URI -> "ABSOLUTE_URI (0x03)"
                NdefRecord.TNF_EXTERNAL_TYPE -> "EXTERNAL_TYPE (0x04)"
                NdefRecord.TNF_UNKNOWN -> "UNKNOWN (0x05)"
                NdefRecord.TNF_UNCHANGED -> "UNCHANGED (0x06)"
                else -> "EMPTY (0x00)"
            }
            val typeStr = String(r.type, StandardCharsets.US_ASCII)
            val preview = try {
                if (r.tnf == NdefRecord.TNF_WELL_KNOWN && r.type.contentEquals(NdefRecord.RTD_URI)) {
                    r.toUri().toString()
                } else if (r.tnf == NdefRecord.TNF_EXTERNAL_TYPE) {
                    String(r.payload, StandardCharsets.UTF_8)
                } else {
                    String(r.payload, StandardCharsets.UTF_8).take(60)
                }
            } catch (_: Exception) {
                r.payload.joinToString(" ") { "%02X".format(it) }.take(60)
            }

            RecordInfo(
                index = idx + 1,
                tnfName = tnfStr,
                type = typeStr,
                payloadPreview = preview,
                length = r.payload.size
            )
        }
    }
}
