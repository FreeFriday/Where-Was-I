/*
메인 화면 클래스
 */

package com.freefriday.wherewasi;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.text.Layout;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    public Switch onswitch; //활성화 스위치
    public LinearLayout details; //스위치 활성화 시 보이는 세부 설정
    public TextView textday; //위치 기록 기간 텍스트
    public SeekBar dayslider; //위치 기록 기간 설정 슬라이더
    public TextView textfreq; //위치 기록 빈도 텍스트
    public SeekBar freqslider; //위치 기록 빈도 설정 슬라이더
    public Button showres; //지도로 보여주기 버튼
    public Button delres; //데이터베이스 삭제 버튼

    public boolean ison = false; //활성화 여부

    public int trackdays = 3; //위치 기록 기간
    public static int MINDAY = 3; //위치 기록 기간 최소치 3=3일
    public static int MAXDAY = 7; //위치 기록 기간 최대치 7=7일

    public int freqency = 0; //위치 기록 빈도 0=최소
    public static int MAXFREQ = 2; //위치 기록 최대 빈도

    public SharedPreferences sp;

    public static String SPNAME = "Settings";
    public static String SP_ISON = "ison";
    public static String SP_DAYS = "trackdays";
    public static String SP_FREQ = "freqency";
    public static String SP_PERM = "permission";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE); //타이틀 상단바 없게
        setContentView(R.layout.activity_main);

        //설정 사항 불러오기
        sp = getSharedPreferences(SPNAME, MODE_PRIVATE);
        if(sp.contains(SP_ISON))ison = sp.getBoolean(SP_ISON, false);
        if(sp.contains(SP_DAYS))trackdays = sp.getInt(SP_DAYS, MINDAY);
        if(sp.contains(SP_FREQ))freqency = sp.getInt(SP_FREQ, 0);

        //위치 권한 요청
        if(ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED
        ||ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            ){}else{
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
            }
        }

        //뷰 바인딩
        onswitch = findViewById(R.id.sw_on);
        details = findViewById(R.id.details_layout);
        textday = findViewById(R.id.tx_day);
        dayslider = findViewById(R.id.sb_day);
        textfreq = findViewById(R.id.tx_freq);
        freqslider = findViewById(R.id.sb_freq);
        showres = findViewById(R.id.bt_show);
        delres = findViewById(R.id.bt_del);

        //위치 기록일 사전 설정
        {
            String showtext = getResources().getString(R.string.tracking_days)+" "+Integer.toString(trackdays)+getResources().getString(R.string.generic_days);
            textday.setText(showtext);
        }

        //위치 기록 빈도 사전 설정
        {
            String showtext = getResources().getString(R.string.frequency)+" "+freqtostr(freqency);
            textfreq.setText(showtext);
        }

        //활성화에 따른 보이기 여부 설정
        if(ison){
            //details.setVisibility(ison?View.VISIBLE:View.INVISIBLE);
            onswitch.setChecked(ison);
        }

        //활성화 스위치 설정
        onswitch.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                ison = onswitch.isChecked();
                writesp(SP_ISON, ison);
                //details.setVisibility(ison?View.VISIBLE:View.INVISIBLE);

                if(ison){
                    //위치 기록 예약
                    Alarmer.setalarm(getApplicationContext(), 0, freqency);
                }
                else{
                    //백그라운드 서비스 취소
                    Alarmer.delalarm(getApplicationContext(), 0);
                }
            }
        });

        //데이터 베이스 삭제 버튼
        delres.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog ad = new AlertDialog.Builder(MainActivity.this).create();
                ad.setTitle(getResources().getString(R.string.delete));
                ad.setMessage(getResources().getString(R.string.deletealert));
                ad.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.cont), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        DBOpenHelper dboh = new DBOpenHelper(getApplicationContext());
                        dboh.open();
                        dboh.DelDB();
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.donedelete), Toast.LENGTH_SHORT).show();
                    }
                });
                ad.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                ad.show();
            }
        });

        //지도 보기 버튼
        showres.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ShowMap.class);
                startActivity(intent);
            }
        });

        //위치 기록 기간 슬라이더 설정
        dayslider.setMax(MAXDAY-MINDAY);
        dayslider.setProgress(trackdays-MINDAY);
        dayslider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                trackdays = progress + MINDAY;
                writesp(SP_DAYS, trackdays);
                String showtext = getResources().getString(R.string.tracking_days)+" "+Integer.toString(trackdays)+getResources().getString(R.string.generic_days);;
                textday.setText(showtext);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //위치 기록 빈도 슬라이더 설정
        freqslider.setMax(MAXFREQ);
        freqslider.setProgress(freqency);
        freqslider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                freqency = progress;
                writesp(SP_FREQ, freqency);
                String showtext = getResources().getString(R.string.tracking_days)+" "+freqtostr(freqency);
                textfreq.setText(showtext);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    //Shared Preferences에 쓰기
    public void writesp(String key, boolean b){
        SharedPreferences.Editor sped = sp.edit();
        sped.putBoolean(key, b);
        sped.apply();
    }
    public void writesp(String key, int i){
        SharedPreferences.Editor sped = sp.edit();
        sped.putInt(key, i);
        sped.apply();
    }
    public String freqtostr(int fr){
        int resnum = R.string.low;
        switch (fr){
            case 0: resnum = R.string.low;
                break;
            case 1: resnum = R.string.medium;
                break;
            case 2: resnum = R.string.high;
                break;
        }
        return getResources().getString(resnum);
    }
}
