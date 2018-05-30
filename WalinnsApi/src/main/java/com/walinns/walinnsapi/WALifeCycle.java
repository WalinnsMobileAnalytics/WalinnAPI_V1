package com.walinns.walinnsapi;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.Socket;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Created by walinnsinnovation on 30/12/17.
 */

public class WALifeCycle  implements Application.ActivityLifecycleCallbacks {
    private static final WALog logger = WALog.getLogger();
    protected WalinnsAPIClient mInstance;
    Context mContext;
    private static Double sStartSessionTime;
    public static String starttime;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable check;
    private boolean mIsForeground = true;
    private boolean mPaused = true;
    private final WAConfig mConfig;
   // WAClient waClient;
    private String project_token;
    private WAPref waPref;
    private int app_launch_count = 0;


    public WALifeCycle(WalinnsAPIClient instance, WAConfig config, Context context,String projecttoken) {
        this.mInstance = instance;
        this.mConfig = config;
        mContext = context;
        waPref = new WAPref(mContext);
        if (sStartSessionTime == null) {
            sStartSessionTime = Double.valueOf((double) System.currentTimeMillis());
            starttime = WAUtils.getCurrentUTC();
            //waClient = new WAClient("182.156.249.254", 3000);
            this.project_token = projecttoken;
            //SocketConnection(project_token);
            WALifeCycle.this.mInstance.track_(WALifeCycle.starttime, true);
            logger.e("WAClient session_start_time", String.valueOf(sStartSessionTime));
            logger.e("WAClient", "life_cycle_method" + "inside_lifecycle" + project_token);

            if(!waPref.getValue(WAPref.app_launch_count).isEmpty()){
                waPref.save(WAPref.app_launch_count, String.valueOf(Integer.valueOf(waPref.getValue(WAPref.app_launch_count))+1));
            }else {
                waPref.save(WAPref.app_launch_count, String.valueOf(app_launch_count + 1));
            }

            logger.e("WAClient", "app_launch" + waPref.getValue(WAPref.app_launch_count));
            WALifeCycle.this.mInstance.track("default_event","App Launch");

            if(waPref.getValue(WAPref.app_install)!=null && !waPref.getValue(WAPref.app_install).isEmpty()){
                logger.e("WAClient", "app_install " + waPref.getValue(WAPref.app_install));

            }else {
                WALifeCycle.this.mInstance.track("default_event","App Install");
                waPref.save(WAPref.app_install,"installed");
                WALifeCycle.this.mInstance.install_refferer();
            }

        }

    }


    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        logger.e("WAClient Lifecycle", "onActivityCreated");

    }

    @Override
    public void onActivityStarted(Activity activity) {
        logger.e("WAClient Lifecycle", "onActivityStarted");
        new WAGesture(this.mInstance, activity);


    }

    @Override
    public void onActivityResumed(Activity activity) {
        logger.e("WAClient Lifecycle", "onActivityResumed");
        this.mPaused = false;
        boolean wasBackground = !this.mIsForeground;
        this.mIsForeground = true;
        if (this.check != null) {
            this.mHandler.removeCallbacks(this.check);
        }
        logger.e("WAClient session length background", String.valueOf(wasBackground));
        sStartSessionTime = Double.valueOf((double) System.currentTimeMillis());
        starttime = WAUtils.getCurrentUTC();
        //SocketConnection(project_token);
        logger.e("WAClient session length session start time", String.valueOf(sStartSessionTime));
        mContext.startService(new Intent(mContext, WAService.class)); //start service which is MyService.java

        WALifeCycle.this.mInstance.track_(WAUtils.getCurrentUTC(), true);
        if(!waPref.getValue(WAPref.app_launch_count).isEmpty()){
            waPref.save(WAPref.app_launch_count, String.valueOf(Integer.valueOf(waPref.getValue(WAPref.app_launch_count))+1));
        }else {
            waPref.save(WAPref.app_launch_count, String.valueOf(app_launch_count + 1));
        }

        logger.e("WAClient", "app_launch" + waPref.getValue(WAPref.app_launch_count));


        WALifeCycle.this.mInstance.track("default_event","App Launch");

    }

    @Override
    public void onActivityPaused(Activity activity) {
        logger.e("WAClient Lifecycle", "onActivityPaused");

        this.mPaused = true;
        if (this.check != null) {
            this.mHandler.removeCallbacks(this.check);
        }
        mContext.stopService(new Intent(mContext, WAService.class)); //start service which is MyService.java

        this.mHandler.postDelayed(this.check = new Runnable() {
            public void run() {
                logger.e("WAClient session length_if", String.valueOf(WALifeCycle.this.mIsForeground) + WALifeCycle.this.mPaused);
                WALifeCycle.this.mInstance.sendpush();
                System.out.println("WalinnsTrackerClient  tracker_session in"+ WALifeCycle.this.mIsForeground+"...."+WALifeCycle.this.mPaused);
                if (WALifeCycle.this.mIsForeground && WALifeCycle.this.mPaused) {
                    WALifeCycle.this.mIsForeground = false;

                    try {
                        double e = (double) System.currentTimeMillis() - WALifeCycle.sStartSessionTime.doubleValue();
                        if (e >= (double) WALifeCycle.this.mConfig.getMinimumSessionDuration() && e < (double) WALifeCycle.this.mConfig.getSessionTimeoutDuration()) {
                            NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
                            nf.setMaximumFractionDigits(1);
                            logger.e("WAClient session differ", String.valueOf(((double) System.currentTimeMillis())) + "....." + (WALifeCycle.sStartSessionTime.doubleValue()) / 1000.0D);

                            String sessionLengthString = nf.format(((double) System.currentTimeMillis() - WALifeCycle.sStartSessionTime.doubleValue()) / 1000.0D);
                            logger.e("WAClient session length", sessionLengthString);
                            JSONObject sessionProperties = new JSONObject();
                            sessionProperties.put("$ae_session_length", sessionLengthString);
                            sessionProperties.put("$start_time", WALifeCycle.starttime);
                            sessionProperties.put("$end_time", WAUtils.getCurrentUTC());
                             WALifeCycle.this.mInstance.track_("$ae_session", sessionProperties, true);
                            WALifeCycle.this.mInstance.track_(WAUtils.getCurrentUTC(), false);
                            //waClient.disconnect();

                        }
                    } catch (JSONException var6) {
                        var6.printStackTrace();
                    }

                    // WALifeCycle.this.mInstance.onBackground();
                }

            }
        }, 500L);

    }

    @Override
    public void onActivityStopped(Activity activity) {
        logger.e("WAClient Lifecycle", "onActivityStopped");
         mContext.stopService(new Intent(mContext, WAService.class)); //start service which is MyService.java
       // waClient.disconnect();

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        logger.e("WAClient Lifecycle", "onActivitySaveInstanceState");

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        logger.e("WAClient Lifecycle", "onActivityDestroyed");
        mContext.stopService(new Intent(mContext, WAService.class)); //start service which is MyService.java
        //waClient.disconnect();


    }
   /* protected void SocketConnection(final String project_token){

        System.out.println("SocketConnection" + "inside method" + project_token);


        waClient.setClientCallback(new WAClient.ClientCallback() {
            @Override
            public void onMessage(String message) {
                System.out.println("Socket_connection_msg" + message);
            }

            @Override
            public void onConnect(Socket sockett) {
                try {
                    JSONObject jsonObject=new JSONObject();
                    jsonObject.put("project_token" ,project_token);

                    waClient.send(project_token);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                System.out.println("Socket_connection" + sockett.isConnected());
                //socket.disconnect();


            }

            @Override
            public void onDisconnect(Socket socket, String message) {
                System.out.println("Socket_connection_disconnect" + socket.isConnected()+ message);
            }

            @Override
            public void onConnectError(Socket socket, String message) {
                System.out.println("Socket_connection_error" + message);
            }
        });
        waClient.connect();
    }*/

}
