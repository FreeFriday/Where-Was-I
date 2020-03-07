/*
백그라운드에서 위치를 데이터베이스에 기록
startForeground 사용을 위해 Notification Channel을 형성하고 Notification을 내보내지만,
작업 수행 후 stopForeground을 해 Notification이 실제로 보이지는 않음.
 */

package com.freefriday.wherewasi;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.health.SystemHealthManager;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.util.Calendar;

public class WriteLocation extends Service {
    public static long dbpos = 0;
    public static int freq = 0;
    public static int days = 3;
    public static int maxcol = 0; //최대 행의 수
    public SharedPreferences sp;
    public static String SP_DBPOS = "dbpos";
    public static String chID = "com.freefriday.wherewasi";
    public static String chName = "Get Location";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("Service Started");
        //SharedPreferences에서 저장 값들 불러옴
        sp = getSharedPreferences(MainActivity.SPNAME, MODE_PRIVATE);
        if (sp.contains(SP_DBPOS)) dbpos = sp.getLong(SP_DBPOS, 0);
        if (sp.contains(MainActivity.SP_FREQ)) freq = sp.getInt(MainActivity.SP_FREQ, 0);
        if (sp.contains(MainActivity.SP_DAYS)) days = sp.getInt(MainActivity.SP_DAYS, MainActivity.MINDAY);

        //최대 행의 수 구함
        maxcol = getmaxcol(days, freq);

        //위치 측정 설정 사항
        Criteria crt = new Criteria();
        crt.setAccuracy(Criteria.ACCURACY_FINE);
        crt.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        crt.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
        crt.setAltitudeRequired(false);
        crt.setSpeedRequired(false);

        //위치 받아옴
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        }//위치 권한 있는지 확인, 없으면 에러

        lm.requestSingleUpdate(crt, new LocationListener() { //한번만 위치 요청하여 데이터베이스에 저장, 모든 값은 문자열로 저장
            @Override
            public void onLocationChanged(Location location) {//위치 저장

                System.out.println("Start getting location");

                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(location.getTime());
                String TIME = cal.get(Calendar.YEAR)+" "+cal.get(Calendar.MONTH)+" "+cal.get(Calendar.DATE)+" "+cal.get(Calendar.HOUR_OF_DAY)+" "+cal.get(Calendar.MINUTE)+" "+cal.get(Calendar.SECOND);

                String LONG = Double.toString(location.getLongitude());
                String LAT = Double.toString(location.getLatitude());
                String ACCR = Float.toString(location.getAccuracy());

                System.out.println("TIME="+TIME);
                System.out.println("LONGITUDE="+LONG);
                System.out.println("LAT="+LAT);
                System.out.println("ACCURACY="+ACCR);

                DBOpenHelper dboh = new DBOpenHelper(getApplicationContext());
                dboh.open();
                dboh.create();
                Cursor cur = dboh.selectcol();
                System.out.println("DB column count: "+dboh.getnums()+", Maxcol="+maxcol);
                if(dboh.getnums()>=maxcol){
                    cur.moveToFirst();
                    if(cur!=null)dboh.deletecol(cur.getLong(0));
                }
                long nowid = dboh.insertcolumn(TIME, LONG, LAT, ACCR);
                dboh.close();
                System.out.println("Inserted DB: id="+nowid);
            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
            @Override
            public void onProviderEnabled(String provider) {
            }
            @Override
            public void onProviderDisabled(String provider) {
            }
        }, null);
        stopForeground(true); //Notification 제거
        //Alarmer.setalarm(getApplicationContext(), 0, freq); //디버깅용 코드
        return super.onStartCommand(intent, flags, startId);
    }

    public void onCreate() {
        super.onCreate();
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){ //startForeground를 위한 Notification Channel 생성
            NotificationChannel nc = new NotificationChannel(chID, chName, NotificationManager.IMPORTANCE_MIN);
            NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            nm.createNotificationChannel(nc);
            Notification nt = new NotificationCompat.Builder(this, chID).setContentTitle("").setContentText("").setPriority(Notification.PRIORITY_MIN).build();
            startForeground(2, nt);
        }

    }
    public static int getmaxcol(int days, int freq){ //데이터베이스 최대 행수 반환
        switch (freq){
            case 0: return days*48;
            case 1: return days*96;
            case 2: return days*288;
            default: return days*48;
        }
        //return 10;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }
}
