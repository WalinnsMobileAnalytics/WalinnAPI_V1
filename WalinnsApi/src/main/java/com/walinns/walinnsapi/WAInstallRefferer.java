package com.walinns.walinnsapi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by walinnsinnovation on 23/05/18.
 */

public class WAInstallRefferer extends BroadcastReceiver {

    private static final WALog logger = WALog.getLogger();

    @Override
    public void onReceive(Context context, Intent intent) {
        final Bundle extras = intent.getExtras();
        if (null == extras) {
            System.out.println("install_refferer :" + "null");
            final SharedPreferences referralInfo = context.getSharedPreferences("WAInstall_refferer", Context.MODE_PRIVATE);
            final SharedPreferences.Editor editor = referralInfo.edit();
            editor.clear();
            editor.putString("utm_source", "Direct");
            editor.putString("referrer","Google play");
            editor.apply();
            return;
        } else {
            System.out.println("install_refferer else :" + extras.toString());
        }
        System.out.println("install_refferer :" + "called");

        final String referrer = extras.getString("referrer");
        final String direct = extras.getString("id");
        if (null == referrer) {
            return;
        }else {
            System.out.println("install_refferer else:" + "called");
            final Map<String, String> newPrefs = new HashMap<String, String>();
            newPrefs.put("referrer", referrer);

            try {
                String result = URLDecoder.decode(referrer, "UTF-8");
                if (result != null && !result.equals("")) {
                    System.out.println("install_refferer source decode url :" + result);
                    String[] referrerParts = result.split("&");
                    String utmSource = getData(KEY_UTM_SOURCE, referrerParts);
                    String utm_medium = getData(KEY_UTM_MEDIUM, referrerParts);
                    String utm_term = getData(KEY_UTM_TERM, referrerParts);
                    newPrefs.put("utm_source", utmSource);
                    newPrefs.put("utm_medium", utm_medium);
                    newPrefs.put("utm_term", utm_term);
                    System.out.println("install_refferer source decode url.. :" + utmSource + "..." + utm_medium + "..." + utm_term);
                }

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            final SharedPreferences referralInfo = context.getSharedPreferences("WAInstall_refferer", Context.MODE_PRIVATE);
            final SharedPreferences.Editor editor = referralInfo.edit();
            editor.clear();
            for (final Map.Entry<String, String> entry : newPrefs.entrySet()) {
                editor.putString(entry.getKey(), entry.getValue());
                System.out.println("install_refferer source refferer value .....:" +entry.getKey() +"....Value:"+ entry.getValue());

            }
            editor.apply();
        }





    }





    private String find(Matcher matcher) {
        if (matcher.find()) {
            final String encoded = matcher.group(2);
            if (null != encoded) {
                try {
                    return URLDecoder.decode(encoded, "UTF-8");
                } catch (final UnsupportedEncodingException e) {
                    logger.e(LOGTAG, "Could not decode a parameter into UTF-8");
                }
            }
        }
        return null;
    }
    private String getData(String key, String[] allData) {
        for (String selected : allData)
            if (selected.contains(key)) {
                return selected.split("=")[1];
            }
        return "";
    }
    public static final String KEY_UTM_SOURCE = "utm_source";
    public static final String KEY_UTM_MEDIUM = "utm_medium";
    public static final String KEY_UTM_TERM= "utm_term";


    private static final String LOGTAG = "WalinnsAPi.InstRfrRcvr";
}