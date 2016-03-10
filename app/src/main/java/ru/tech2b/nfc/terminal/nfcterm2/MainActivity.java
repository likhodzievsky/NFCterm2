package ru.tech2b.nfc.terminal.nfcterm2;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;

public class MainActivity extends Activity {

    private Tag tag;
    private IsoDep tagcomm;
    public byte[] callBack;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        resolveIntent(getIntent());
    }

    void resolveIntent(Intent intent) {
        setContentView(R.layout.activity_main);
        TextView log = (TextView) findViewById(R.id.textView);
        HashMap <String,String> tags = new HashMap<>();
        String action = intent.getAction();
        String recieve = new String();
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            String idTag = byteArrayToHexString(intent.getExtras().getByteArray("android.nfc.extra.ID"));
            tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Toast.makeText(this, idTag,Toast.LENGTH_LONG).show();

            tagcomm = IsoDep.get(tag);

            if (tagcomm == null) {
                Toast.makeText(this, "Tagcomm",Toast.LENGTH_LONG).show();
                return;
            }
            try {
                tagcomm.connect();
            } catch (IOException e) {
                Toast.makeText(this, "Tagcomm"+ (e.getMessage() != null ? e.getMessage() : "-"),Toast.LENGTH_LONG).show();

                return;
            }

            String amount = "10000";
            String tvr = "8000008800";
            String ttq = "22C04000";
            String termCountryCode = "643";
            String tranCurrCode = "643";
            Calendar c = Calendar.getInstance();
            DateFormat dateFormat = new SimpleDateFormat("yyMMdd");
            String tranDate = dateFormat.format(c.getTime());
            String tranType = "00";
            Random random = new Random();
            String unpredNum = Integer.toHexString(random.nextInt(16777215)).toUpperCase();

            tags.put ("9F02",amount);
            tags.put("9F1A",termCountryCode);
            tags.put("95",tvr);
            tags.put("5F2A",tranCurrCode);
            tags.put("9A",tranDate);
            tags.put("9C",tranType);
            tags.put("9F37",unpredNum);
            tags.put("9F35","21");
            tags.put("9F34","5E0300");
            tags.put("9F66",ttq);

            try {
                //select PSE
                callBack = send("00A404000E325041592E5359532E444446303100", tagcomm);
                log.append("00A404000E325041592E5359532E444446303100"+" \n" +
                        "\n");
                recieve = byteArrayToHexString(callBack);
                log.append(recieve + " \n" +
                        "\n");
                tags.putAll(BerTLV.getBertlv(recieve));
                //select AID
                callBack = send(selectAid(tags.get("4F")), tagcomm);
                log.append(selectAid(tags.get("4F")+" \n" +
                        "\n"));
                recieve = byteArrayToHexString(callBack);
                log.append(recieve + " \n" +
                        "\n");
                tags.putAll(BerTLV.getBertlv(recieve));
                if (tags.get("9F38") == null) {
                    //get proc opt with empty PDOL (processing data object list)
                    callBack = send("80A8000002830000", tagcomm);
                    log.append("80A8000002830000"+" \n" +
                            "\n");
                }
                else {
                    //get proc opt with PDOL
                    String pdol = BerTLV.getBertlvDOL(tags.get("9F38"),tags);
                    String pdollc = Integer.toHexString(pdol.length()/2).toUpperCase();
                    String lCdol = Integer.toHexString(("83"+pdollc+pdol).length()/2).toUpperCase();
                    String getpdol = "80A80000"+lCdol+"83"+pdollc+pdol+"00";
                    callBack = send(getpdol, tagcomm);
                    log.append(getpdol+" \n" +
                            "\n");

                }


                recieve = byteArrayToHexString(callBack);
                log.append(recieve+" \n" +
                        "\n");
                tags.putAll(BerTLV.getBertlv(recieve));
                //get records
                String [] records = readRecords(tags.get("94"));
                for (int i=0;i<records.length;i++){
                    callBack = send(records[i], tagcomm);
                    log.append(records[i]+" \n" +
                            "\n");
                    recieve = byteArrayToHexString(callBack);
                    log.append(recieve+" \n\n");
                    tags.putAll(BerTLV.getBertlv(recieve));
                }
                //send CDOL for generate AC
                String cdol = BerTLV.getBertlvDOL(tags.get("8C"),tags);
                String lc = Integer.toHexString(cdol.length()/2).toUpperCase();
                String getcdol = "80AE8000"+lc+cdol+"00";
                callBack = send(getcdol, tagcomm);
                log.append(getcdol+" ");
                recieve = byteArrayToHexString(callBack);
                log.append(recieve+" ");



            } catch (IOException e) {
                e.printStackTrace();
            }


            Toast.makeText(this, recieve,Toast.LENGTH_LONG).show();
        }
    }

    public static String selectAid(String aid) {
        Integer lc = aid.length()/2;
        String strLc;
        if (lc < 10) {strLc = "0"+lc.toString();} else {strLc = lc.toString();}
        String message = "00A40400"+strLc+aid+"00";
        return message;
    }

    public static String[] readRecords (String s){
        String[] order = new String[s.length()/8];
        Integer [] parts = new Integer[s.length()/8];
        int recnum = 0;
        for (int i = 0;i<s.length()/8; i++) {
            order[i]=s.substring(i*8,8+i*8);
            int rec1 = Integer.parseInt(order[i].substring(2,4),16);
            int rec2 = Integer.parseInt(order[i].substring(4, 6), 16);
            parts[i] = 1 + (rec2-rec1);
            recnum = recnum + parts[i];
        }

        String[] recs = new String[recnum];
        recnum = 0;
        for (int i=0;i<s.length()/8;i++){
            String sfi = getSFI(order[i].substring(0, 2));
            String p1;
            for (int j=0;j<parts[i];j++) {
                int rec = Integer.parseInt(order[i].substring(2,4))+j;
                if (rec <17) {
                    p1 ="0"+Integer.toHexString(rec);
                }
                else {p1 =Integer.toHexString(rec);}
                recs[recnum] = "00B2"+p1+sfi+"00";
                recs[recnum]=recs[recnum].toUpperCase();
                recnum++;

            }
        }
        return recs;
    }

    public static String getSFI (String s) {
        String bin = hexToBinary (s);
        bin = "00000000".substring(0,8-bin.length())+bin;
        String res = bin.substring(0,5)+"100";
        Integer intres = Integer.parseInt(res, 2);
        if (intres < 17) {
            res = "0"+Integer.toHexString(intres);
        }
        else {
            res = Integer.toHexString(intres);
        }
        return res;
    }


    public static String hexToBinary(String hex) {
        int i = Integer.parseInt(hex, 16);
        String bin = Integer.toBinaryString(i);
        return bin;
    }

    protected static byte[] send(String hexstr, IsoDep tagcomm) throws IOException {
        String[] hexbytes = new String[hexstr.length()/2]; //= hexstr.split("\\s");
        for (int i = 0; i < hexstr.length()/2;i++){
            hexbytes[i] = hexstr.substring(2*i,2+2*i);
        }
        byte[] bytes = new byte[hexbytes.length];
        for (int i = 0; i < hexbytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hexbytes[i], 16);
        }

        byte[] recv = tagcomm.transceive(bytes);

        return recv;
    }

    public static String byteArrayToHexString(byte[] bytes) {
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        resolveIntent(intent);
    }


}
