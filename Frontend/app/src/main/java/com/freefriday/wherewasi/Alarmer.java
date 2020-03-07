/*
알람 설정 클래스. Static으로 함수들이 선언되어 객체가 반드시 필요하지는 않음
 */

package com.freefriday.wherewasi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

import androidx.annotation.RequiresApi;

import java.util.Calendar;

public class Alarmer {
    public static String ALARM_INTENT = "com.freefriday.wherewasi.alarm";

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static void setalarm(Context context, int requestcode, int freq){ //시간 설정 알람, Requestcode는 0으로 사용
        AlarmManager alm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmRecv.class);
        intent.setAction(ALARM_INTENT);
        PendingIntent pintent = PendingIntent.getBroadcast(context,requestcode,intent,PendingIntent.FLAG_CANCEL_CURRENT);
        long repeattime = getreptime(freq);
        alm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+repeattime, repeattime, pintent);
        //alm.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+repeattime, pintent); //디버깅용 코드
        System.out.println("Alarm Set: after "+repeattime+"ms");
    }
    public static void delalarm(Context context, int requestcode){ //알람 설정 해제
        AlarmManager alm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmRecv.class);
        intent.setAction(ALARM_INTENT);
        PendingIntent pintent = PendingIntent.getBroadcast(context,requestcode,intent,PendingIntent.FLAG_CANCEL_CURRENT);
        alm.cancel(pintent);
        System.out.println("Alarm cancel");
    }
    public static long getreptime(int freq){ //알람 반복 구간 설정
        switch (freq){
            case 0: return AlarmManager.INTERVAL_HALF_HOUR;
            case 1: return AlarmManager.INTERVAL_FIFTEEN_MINUTES;
            case 2: return 5*60*1000;
                //return 10000; //디버깅 값
            default: return AlarmManager.INTERVAL_HALF_HOUR;
        }
    }
}
