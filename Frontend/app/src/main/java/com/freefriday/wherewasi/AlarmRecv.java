/*
일정 시간 후 알림 수신 시 위치 기록 클래스(WriteLocation) 실행
 */

package com.freefriday.wherewasi;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmRecv extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println("Alarm Received");
        Intent now = new Intent(context, WriteLocation.class);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            context.startForegroundService(now);
        }
        else context.startService(now);
    }
}
