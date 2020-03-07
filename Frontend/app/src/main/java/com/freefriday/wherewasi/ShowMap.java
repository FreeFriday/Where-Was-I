/*
지도에 마커를 설정해 화면에 표시해줌
*/

package com.freefriday.wherewasi;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.StringTokenizer;


public class ShowMap extends AppCompatActivity implements OnMapReadyCallback {
    public GoogleMap gmap; //지도
    public ArrayList<Locations> points; //위치 정보를 담는 배열
    public ArrayList<Marker> markers; //마커들을 담는 배열
    public ArrayList<Circle> circles; //유효범위 표현원을 담는 배열
    public ArrayList<Polygon> polys; //마커 사이 직선 경로들을 담는 배열: i번째 값은 i-1와 i사이 직선임. i=0 또는 i=n+1인 경우 null값이 있음
    public static float markeralpha = 0.5f; //마커가 포커싱되지 않은 경우의 투명도
    public static int circlecolor = 0x7f73d1f7; //유효범위 표현원의 내부 색
    public static int circlestrokecolor = 0x8f73d1f7; //유효범위 표현원의 둘레 색
    public static float initzoom = 15; //처음 카메라의 줌 정도 (15=거리 정도 수준)
    public static int polycolor = 0x2f000000; //마커 사이 직선 경로가 포커싱되지 않은 경우의 색
    public static int polyfocuscolor = 0x8f000000; //마커 사이 직선 경로가 포커싱된 경우의 색
    public static float polywidth = 10f; //마커 사이 직선 경로의 두께

    public Button nextbt; //다음 버튼
    public Button prevbt; //이전 버튼
    public Touchtimer nexttt; //다음 버튼이 길게 눌렸는지 확인하는 객체
    public Touchtimer prevtt; //이전 버튼이 길게 눌렸는지 확인하는 객체
    public TextView nowtx; //몇 번째 마커를 보는 중인지 표시하는 텍스트 뷰
    public static long longpressinter = 1000; //길게 누르는 사이 간격. 1000= 1000ms이상 눌렸는지 확인
    public static int longerjumpcount = 5; //더욱 많이 움직일 때까지 필요한 길게 눌려 이동한 횟수. 5= 길게 눌려 5번째까지는 일반 점프, 6번째부터는 길게 점프
    public int nowindex = 0; //현재 카메라가 포커싱하는 마커의 번호

    public static int jumpplus = 10; //일반 점프 거리
    public static int longjump = 100; //긴 점프 거리

    boolean maploaded = false; //맵이 다 불러와졌는지 여부

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);

        //배열들에 객체 연결
        points = new ArrayList<>();
        markers = new ArrayList<>();
        circles = new ArrayList<>();
        polys = new ArrayList<>();

        //뷰 바인딩
        nextbt = findViewById(R.id.bt_next);
        prevbt = findViewById(R.id.bt_prev);
        nowtx = findViewById(R.id.tv_now);

        //다음 버튼 짧게 눌림 설정
        nextbt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int tempindex = nowindex+1;
                if(tempindex>=markers.size())tempindex=0;
                if(markers.size()>0)SetCampos(tempindex);
            }
        });

        //다음 버튼 길게 눌림설정
        nextbt.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                System.out.println("Next Btn Long click");
                nexttt = new Touchtimer();
                nexttt.activate = true;
                nexttt.isnext = true;
                nexttt.inittime = SystemClock.uptimeMillis()-longpressinter-1; //바로 점프를 트리거하기 위함
                nexttt.execute();
                return true;
            }
        });

        //다음 버튼이 눌리지 않게 되는 경우 설정
        nextbt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_UP:
                    {
                        if(nexttt!=null){nexttt.activate=false;System.out.println("nexttt not null");}
                        System.out.println("Next Btn touch up");
                    }
                    break;
                }
                return false;
            }
        });

        //이전 버튼 짧게 눌림 설정
        prevbt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int tempindex = nowindex-1;
                if(tempindex<0)tempindex=markers.size()-1;
                if(markers.size()>0)SetCampos(tempindex);
            }
        });

        //이전 버튼 길게 눌림설정
        prevbt.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                System.out.println("Prev Btn Long click");
                prevtt = new Touchtimer();
                prevtt.activate = true;
                prevtt.isnext = false;
                prevtt.inittime = SystemClock.uptimeMillis()-longpressinter-1;
                prevtt.execute();
                return true;
            }
        });

        //이전 버튼이 눌리지 않게 되는 경우 설정
        prevbt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_UP:
                    {
                        if(prevtt!=null){prevtt.activate=false;System.out.println("nexttt not null");}
                        System.out.println("Prev Btn touch up");
                    }
                    break;
                }
                return false;
            }
        });

        //데이터 베이스 읽기 비동기로 수행
        ReadDB rdb = new ReadDB();
        rdb.execute();

        //맵 불러오기 수행
        SupportMapFragment mf = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        mf.getMapAsync(this);
    }
    @Override
    public void onMapReady(GoogleMap googleMap) { //맵이 불러와진 경우
        gmap = googleMap; //지도 연결

        System.out.println("Map ready");

        //마커가 직접 눌리면 포커싱 설정
        gmap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                SetMarkers(markers.indexOf(marker));
                return false;
            }
        });

        //방문 시간 순으로 위치 정렬
        Collections.sort(points);

        //마커 지정 시작
        maploaded = true;
    }
    public void SetCampos(int index){ //카메라 위치를 index 기준으로 설정
        SetMarkers(index);
        float tempzoom = gmap.getCameraPosition().zoom;
        gmap.moveCamera(CameraUpdateFactory.newLatLng(markers.get(index).getPosition()));
        gmap.moveCamera(CameraUpdateFactory.zoomTo(tempzoom));
    }
    public void SetMarkers(int index){ //index 번의 마커를 포커싱
        markers.get(nowindex).setAlpha(markeralpha);
        markers.get(nowindex).hideInfoWindow();
        circles.get(nowindex).setVisible(false);
        if(polys.get(nowindex)!=null)polys.get(nowindex).setStrokeColor(polycolor);
        if(polys.get(nowindex+1)!=null)polys.get(nowindex+1).setStrokeColor(polycolor);
        markers.get(index).setAlpha(1);
        markers.get(index).showInfoWindow();
        circles.get(index).setVisible(true);
        if(polys.get(index)!=null)polys.get(index).setStrokeColor(polyfocuscolor);
        if(polys.get(index+1)!=null)polys.get(index+1).setStrokeColor(polyfocuscolor);
        nowindex = index;
        nowtx.setText(Integer.toString(nowindex+1)+"/"+Integer.toString(markers.size()));
    }
    public void SetMarkers(int index, int markerssize){ //처음 index 번의 마커를 포커싱 하되, 마커 배열의 크기를 지정하지 못하는 경우 사용
        markers.get(index).setAlpha(1);
        markers.get(index).showInfoWindow();
        circles.get(index).setVisible(true);
        if(polys.get(index)!=null)polys.get(index).setStrokeColor(polyfocuscolor);
        if(index+1<polys.size()){if(polys.get(index+1)!=null)polys.get(index+1).setStrokeColor(polyfocuscolor);}
        nowindex = index;
        nowtx.setText(Integer.toString(nowindex+1)+"/"+Integer.toString(markerssize));
    }
    public class ReadDB extends AsyncTask<Void, Void, Void> { //비동기로 데이터베이스를 읽어 points 배열에 저장

        @Override
        protected Void doInBackground(Void... voids) {
            DBOpenHelper dboh = new DBOpenHelper(getApplicationContext());
            dboh.open();
            Cursor cur = dboh.selectcol();
            cur.moveToFirst();
            if(dboh.getnums()>0){
                do{
                    if(cur!=null){
                        double LONG = Double.parseDouble(cur.getString(2));
                        double LAT = Double.parseDouble(cur.getString(3));
                        float accr = Float.parseFloat(cur.getString(4));
                        String time = cur.getString(1);
                        points.add(new Locations(new LatLng(LAT, LONG), accr, time));
                    }
                }while(cur.moveToNext());
            }
            System.out.println("DB Read Complete: Total "+points.size()+" elements");
            dboh.close();
            return null;
        }
        protected void onPostExecute (Void voids){ //데이터 베이스 읽기가 모두 끝나면 마커 지정
            super.onPostExecute(voids);
            SetMarkers smkr = new SetMarkers();
            smkr.execute();
        }

    }

    public class SetMarkers extends AsyncTask<Void, Void, Void>{ //비동기로 points의 위치를 바탕으로 마커 설정
        LatLng initpos;
        @Override
        protected Void doInBackground(Void... voids) {
                while(!maploaded){ //지도를 다 불러오지 않았다면 대기
                    ;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() { //마커 설정
                        Locations prev=null;
                        for(int i=0;i<points.size();i++) {
                            Locations now = points.get(i);
                            //마커 옵션 설정
                            MarkerOptions mop = new MarkerOptions();
                            mop.position(now.latLng);
                            mop.alpha(markeralpha);
                            mop.title(Integer.toString(now.year) + "/" + Integer.toString(now.month) + "/" + Integer.toString(now.date));
                            mop.snippet(Integer.toString(now.hour > 12 ? now.hour - 12 : now.hour) + ":" + (now.min<10?"0":"")+Integer.toString(now.min)+(now.hour>=12?"PM":"AM"));
                            //유효범위 표시기 설정
                            CircleOptions cop = new CircleOptions();
                            cop.center(now.latLng);
                            cop.fillColor(circlecolor);
                            cop.strokeColor(circlestrokecolor);
                            cop.radius((double) now.accr);
                            cop.visible(false);

                            if(i==0){ //배열 범위 밖의 값 읽기 방지용 null 값
                                prev = now;
                                polys.add(null);
                            }
                            else{ //이전마커-현재마커 사이 직선 경로 설정
                                PolygonOptions pop = new PolygonOptions();
                                pop.add(prev.latLng);
                                pop.add(now.latLng);
                                pop.strokeColor(polycolor);
                                pop.strokeWidth(polywidth);
                                polys.add(gmap.addPolygon(pop));
                                prev = now;
                            }
                            markers.add(gmap.addMarker(mop));
                            circles.add(gmap.addCircle(cop));

                            if (i == 0) { //처음 카메라 위치를 첫 번째 마커로 설정하기 위해 initpos에 저장
                                initpos = now.latLng;
                                SetMarkers(i, points.size());
                            }
                        }
                        polys.add(null); //배열 범위 밖의 값 읽기 방지용 null 값
                    }
                });
            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(initpos!=null) { //첫 번째 마커로 카메라 설정
                gmap.moveCamera(CameraUpdateFactory.newLatLng(initpos));
                gmap.moveCamera(CameraUpdateFactory.zoomTo(initzoom));
            }

        }
    }
    public class Touchtimer extends AsyncTask<Void, Void, Void>{//버튼을 길게 누르는 경우를 처리 위한 비동기 객체
        public boolean activate = false; //버튼이 눌러졌을 때 참 값으로 설정
        public boolean isnext = true; //다음(참)/ 이전(거짓) 버튼
        public long inittime=0; //시간 측정
        int count = 0;
        @Override
        protected Void doInBackground(Void... voids) {
            while(!activate){ //버튼이 눌렸을 때까지 대기
            }
            while(activate){ //버튼이 눌린 경우
                if(SystemClock.uptimeMillis()-inittime>longpressinter){ //기준 간격보다 오래 누른 경우 많이 점프
                    inittime = SystemClock.uptimeMillis();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(isnext){ //다음 버튼인 경우
                                int tempindex = nowindex+(count>longerjumpcount?longjump:jumpplus); //기준 횟수보다 많이 지난 경우 longjump만큼 점프, 아닌 경우 jumpplus만큼 점프
                                if(tempindex>=markers.size())tempindex=0; //index가 배열을 넘어가는 경우 처음 객체로
                                if(markers.size()>0)SetCampos(tempindex);
                                System.out.println("Longclick Interval");
                            }
                            else{ //이전 버튼의 경우
                                int tempindex = nowindex-(count>longerjumpcount?longjump:jumpplus);;
                                if(tempindex<0)tempindex=markers.size()-1;
                                if(markers.size()>0)SetCampos(tempindex);
                                System.out.println("Longclick Interval");
                            }

                        }
                    });
                    if(count<=longerjumpcount)count++;
                }
            }
            return null;
        }
    }

    public class Locations implements Comparable<Locations>{ //마커를 지정하는데 필요한 정보들을 담을 객체
        public LatLng latLng;
        public float accr;
        public String time; //항목은 사이 공백으로 구분: 연도 월 일 시 분 초 (예: 2020 2 6 9 49 49)
        public long timemil; //밀리초로 표현된 시간

        public int year;
        public int month;
        public int date;
        public int hour; //24시간 기준
        public int min;
        public int sec;
        public Locations(LatLng latLng, float accr, String time){
            this.latLng = latLng;
            this.accr = accr;
            this.time = time;
            StringTokenizer st = new StringTokenizer(time, " ");
            year = Integer.parseInt(st.nextToken());
            month = Integer.parseInt(st.nextToken());
            date = Integer.parseInt(st.nextToken());
            hour = Integer.parseInt(st.nextToken());
            min = Integer.parseInt(st.nextToken());
            sec = Integer.parseInt(st.nextToken());
            Calendar temp = Calendar.getInstance();
            temp.set(year,month,date,hour,min,sec);
            timemil = temp.getTimeInMillis();
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public int compareTo(Locations o) {
            return Long.compare(this.timemil, o.timemil);
        } //정렬하는 경우 밀리초 기준으로 정렬
    }
}

