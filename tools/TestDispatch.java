import android.nfc.NdefMessage;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.IBinder;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;

public class TestDispatch {
    public static void main(String[] args) {
        try {
            System.out.println("Reading NDEF payload...");
            File f = new File("/data/local/tmp/test.ndef");
            byte[] bytes = new byte[(int) f.length()];
            FileInputStream fis = new FileInputStream(f);
            fis.read(bytes);
            fis.close();
            
            NdefMessage msg = new NdefMessage(bytes);
            System.out.println("NdefMessage parsed successfully, records: " + msg.getRecords().length);

            byte[] id = new byte[] { (byte) 0x04, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9A, (byte) 0xBC };
            int[] techList = new int[] { 1, 6 }; // NFC_A = 1, NDEF = 6
            Bundle[] techExtras = new Bundle[2];
            
            // Extras for NFC_A
            Bundle nfcAExtra = new Bundle();
            nfcAExtra.putShort("sak", (short) 0x08);
            nfcAExtra.putByteArray("atqa", new byte[] { 0x44, 0x00 });
            techExtras[0] = nfcAExtra;
            
            // Extras for NDEF
            Bundle ndefExtra = new Bundle();
            ndefExtra.putParcelable("ndefmsg", msg);
            ndefExtra.putInt("ndefmaxlength", 256);
            ndefExtra.putInt("ndefcardstate", 2); // READ_ONLY
            ndefExtra.putInt("ndeftype", 2); // TYPE_2
            techExtras[1] = ndefExtra;

            Tag tag = null;
            Method mockMethod = null;
            try {
                mockMethod = Tag.class.getMethod("createMockTag", byte[].class, int[].class, Bundle[].class, long.class);
                tag = (Tag) mockMethod.invoke(null, id, techList, techExtras, 0L);
            } catch (NoSuchMethodException e) {
                mockMethod = Tag.class.getMethod("createMockTag", byte[].class, int[].class, Bundle[].class);
                tag = (Tag) mockMethod.invoke(null, id, techList, techExtras);
            }
            System.out.println("Mock Tag created: " + tag);

            Class<?> sm = Class.forName("android.os.ServiceManager");
            Method getService = sm.getMethod("getService", String.class);
            IBinder binder = (IBinder) getService.invoke(null, "nfc");
            
            Class<?> stub = Class.forName("android.nfc.INfcAdapter$Stub");
            Method asInterface = stub.getMethod("asInterface", IBinder.class);
            Object nfcAdapter = asInterface.invoke(null, binder);

            System.out.println("Calling INfcAdapter.dispatch(tag)...");
            Method dispatchMethod = nfcAdapter.getClass().getMethod("dispatch", Tag.class);
            dispatchMethod.invoke(nfcAdapter, tag);
            System.out.println("Successfully called dispatch(tag)!");

        } catch (Throwable t) {
            t.printStackTrace(System.out);
        }
    }
}
