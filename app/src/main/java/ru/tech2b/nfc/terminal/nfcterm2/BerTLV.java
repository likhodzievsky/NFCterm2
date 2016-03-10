package ru.tech2b.nfc.terminal.nfcterm2;

/**
 * Created by User on 23.01.2016.
 */

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class BerTLV {

    public static String[] getDoubleArray (String s) {
        HashMap<String, String> hashmap = new HashMap();
        hashmap.put("0", "0000");
        hashmap.put("1","0001");
        hashmap.put("2","0010");
        hashmap.put("3","0011");
        hashmap.put("4","0100");
        hashmap.put("5","0101");
        hashmap.put("6","0110");
        hashmap.put("7","0111");
        hashmap.put("8","1000");
        hashmap.put("9","1001");
        hashmap.put("A","1010");
        hashmap.put("B","1011");
        hashmap.put("C","1100");
        hashmap.put("D","1101");
        hashmap.put("E","1110");
        hashmap.put("F","1111");

        String res = "";
        int end = 0;
        String[] str = new String[s.length()];
        for (int i=0;i<s.length();i++) {
            if (i+1 >s.length()){
                end = s.length();
            }
            else end = i+1;
            str[i] = s.substring(i,end);
            res = res+hashmap.get(str[i]);
        }
        String[] dbl = new String[res.length()];
        for (int i=0;i<res.length();i++) {
            if (i+1 >res.length()){
                end = res.length();
            }
            else end = i+1;
            dbl[i] = res.substring(i,end);
        }
        hashmap.clear();
        return dbl;
    }

    public static boolean constructive (String tag) {
        boolean res = false;
        String[] dbl = getDoubleArray(tag);
        res = dbl[2].equals("1");
        return res;
    }

    public static HashMap <String,String> getBertlv (String s){
        HashMap <String,String> tags = new HashMap();
        HashMap <String,String> bigtags = new HashMap();
        String tag = "";
        String len = "";
        String value = "";
        int start =0;



        for (int i = 0; i<s.length()-4;i++) {
            start =i;
            tag = getTag(s, start);
            start+=tag.length();
            len = getTagLen(s, start);
            start+=len.length();
            value = s.substring(start,start+getLenValue(len));
            if (constructive(tag)) {bigtags.put(tag,value);}
            else {tags.put(tag,value);}
            i=start+value.length()-1;
        }

        while (bigtags.size()>0) {
            HashMap<String, String> itr = new HashMap<String, String>();
            //tags.putAll(bigtags);
            itr.putAll(bigtags);
            bigtags.clear();

            for (String key: itr.keySet()) {

                s = itr.get(key);
                for (int i = 0; i < s.length(); i++) {
                    start = i;
                    tag = getTag(s, start);
                    start += tag.length();
                    len = getTagLen(s, start);
                    start += len.length();
                    value = s.substring(start, start + getLenValue(len));
                    if (constructive(tag)) {
                        bigtags.put(tag, value);
                    } else {
                        tags.put(tag, value);
                    }
                    i = start + value.length() - 1;
                }
            }

        }
        return tags;

    }

    public static String getBertlvDOL (String s, HashMap<String,String> tags) {
        String zero = "000000000000000000000000000000";
        String dol = new String();
        String tag = "";
        String len = "";
        int start = 0;


        for (int i = 0; i < s.length(); i++) {
            start = i;
            tag = getTag(s, start);
            start += tag.length();
            len = getTagLen(s, start);
            String val = tags.get(tag);
            Integer intLen = Integer.parseInt(len)*2;
            if (val==null) {
                val = zero.substring(0,intLen);

            }
            if (val.length()<intLen){
                val = zero.substring(0,intLen-val.length())+val;
            }
            dol = dol+val;
            start += len.length();
            i = start - 1;
        }

        return dol;
    }



    public static boolean longtag (String tag) {
        boolean res = true;
        String[] dbl = getDoubleArray(tag);
        for (int i=dbl.length-5;i<dbl.length;i++) {
            res = res && dbl[i].equals("1");
        }
        return  res;
    }

    public static String getTag (String s,int start) {
        String tag = s.substring(start, start + 2);
        String[] dbl;
        if (longtag(tag)) do {
            start += 2;
            String subtag = s.substring(start, start + 2);
            tag = tag + subtag;
            dbl = getDoubleArray(subtag);
        } while (dbl[0].equals("1"));

        return tag;
    }

    public static String getTagLen (String s,int start) {
        String len = "";
        int n=0;
        len = len + s.substring(start, start + 2);
        String[] dbl = getDoubleArray(len);
        if (dbl[0].equals("1")) {
            n = Integer.parseInt(len.substring(1),16);
            len = len+s.substring(start+2,start+2+2*n);
        }
        return len;
    }

    private static Integer getLenValue(String len) {
        Integer res = 0;
        String[] dbl = getDoubleArray(len);
        if (dbl[0].equals("1")) {
            int n = Integer.parseInt(len.substring(1,2),16);
            res = Integer.parseInt(len.substring(2,2+2*n),16);
        }
        else {res = Integer.parseInt(len,16);}
        return res*2;
    }





}
