package halla.icsw.pocha;


import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BuyerMain extends AppCompatActivity
        implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback {

    private GoogleMap mMap;
    private Marker currentMarker=null;
    private static final int GPS_ENABLE_REQUEST_CODE=2001;
    private static final int UPDATE_INTERVAL_MS=1000; //1초
    private static final int FASTEST_UPDATE_INTERVAL_MS=500;
    private static final int PERMISSIONS_REQUEST_CODE=100;
    boolean needRequest = false;
    String[] REQUIRED_PERMISSIONS =
            {Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION}; //외부 저장소
    TextView tx;

    JSONArray jsonArray;

    private ArrayList<String>menuList = new ArrayList<String>();
    private ArrayList<String>shopList = new ArrayList<String>();
    private ArrayList<String>priceList = new ArrayList<String>();


    Location currentlocation;
    LatLng currentposition;
    private Marker getCurrentMarker;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private  Location location;
    private View Layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.buyer);
        NetworkUtil.setNetworkPolicy();


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        locationRequest= new LocationRequest().
                setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)// 위치설정 변경
                .setInterval(UPDATE_INTERVAL_MS)//
                .setFastestInterval(FASTEST_UPDATE_INTERVAL_MS);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();

        builder.addLocationRequest(locationRequest); //위치 설정 요청

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);//권한 요청 인스턴스

        SupportMapFragment supportMapFragment =(SupportMapFragment)getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);//콜백 해줄려고
    }

    public void bts(View v) {
        tx = (TextView)findViewById(R.id.tx);
        SharedPreferences pref = getSharedPreferences("memberInformation",MODE_PRIVATE);
        try {
            PHPRequest request = new PHPRequest("http://101.101.210.207/getMenu.php");
            String result = request.getMenu(pref.getString("id",""));
            Log.i("데이터",result);
            try {
                jsonArray = new JSONArray(result);
                for(int i = 0 ; i<jsonArray.length(); i++){
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    menuList.add(jsonObject.getString("shopname"));
                    shopList.add(jsonObject.getString("menuname"));
                    priceList.add(jsonObject.getString("price"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            tx.setText(menuList.toString()+shopList.toString()+priceList.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }


    }


        @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;


        //퍼미션 있는지 확인
        int FineLocationPermission = ContextCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        // 이미있으면 허용된거 ㅇㅇ
        int CoarseLocationPermission = ContextCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);

        if (FineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                CoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdate(); //위치 업데이트
        } else {
            //요청
            if (ActivityCompat.shouldShowRequestPermissionRationale
                    (this, REQUIRED_PERMISSIONS[0])) {
                Snackbar.make(Layout, "위치 권한이 필요",
                        Snackbar.LENGTH_INDEFINITE).setAction("확인", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ActivityCompat.requestPermissions(BuyerMain.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
                    }
                }).show();

            } else {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }
        LatLng SEOUL = new LatLng(37.56, 126.97);//기본은 서울
        mMap.getUiSettings().setMyLocationButtonEnabled(true); //위치 버튼 가능
        MarkerOptions markerOptions = new MarkerOptions(); // 마커 생성
        markerOptions.position(SEOUL);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(SEOUL));  // 초기 위치
//        mMap.setMyLocationEnabled(true); //내위치 표시
        mMap.animateCamera(CameraUpdateFactory.zoomTo(18));
        UiSettings settings = mMap.getUiSettings();
        settings.setZoomControlsEnabled(true); //줌 버튼
    }

    LocationCallback locationCallback = new LocationCallback(){

        public void onLocationResult(LocationResult locationResult){
            super.onLocationResult(locationResult);

            List<Location> locationList = locationResult.getLocations();


            if(locationList.size()>0){
                currentlocation= locationList.get(locationList.size()-1);
                location= locationList.get(0);

                //현재위치!!!!! 사용할땐 setcurrentposition
                currentposition= new LatLng(location.getLatitude(),location.getLongitude());

                String markerTitle = getGeocoder(currentposition);// 지오코드 사용



            }
        }
    };

    private void getStore(){//스토어 가져오기
        ArrayList<store> storelist = new ArrayList<>();



    }

    public Marker addMarker(store store){
        LatLng position = new LatLng(store.getLat(),store.getLng());
        String name = store.getName();
        String adr = store.getAdr();
        int price = store.getPrice();
        //마커
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.title(name);
        markerOptions.snippet(adr);// 타이틀 아래에ㅇㅇ 요약
        markerOptions.position(position);//위치



        return mMap.addMarker(markerOptions);

    }

    public boolean onMarkerClick(Marker marker){ //마커 선택되면 가운대로
        CameraUpdate center = CameraUpdateFactory.newLatLng(marker.getPosition());
        mMap.animateCamera(center);

        return true;
    }



    public String getGeocoder(LatLng latLng){

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(
                    latLng.latitude, latLng.longitude, 1);
        }catch (IOException e){
            return "지오코드 서비스 불가";
        }catch (IllegalArgumentException e){
            return"잘못된 gps";
        }

        if (addresses==null ||addresses.size()==0){
            return "주소 잘못됨";
        }else {
            Address address = addresses.get(0);
            return address.getAddressLine(0).toString();
        }
    }

    private void startLocationUpdate(){//업데이트 해줄거
        if (!checkLocationServicesStatus()){
            GpsActivation();
        }
        else {
            int FinePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
            int CoarsePermission = ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION);

            if(FinePermission!= PackageManager.PERMISSION_GRANTED ||
                    CoarsePermission != PackageManager.PERMISSION_GRANTED){
                return;
            }
            fusedLocationClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);//현재위치 표시

        }

    }


    public boolean checkLocationServicesStatus() {//네트워크 and gps켯는지 ㅇㅇ
        LocationManager locationManager =(LocationManager)getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)||
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }


    private  void GpsActivation(){//gps키자
        AlertDialog.Builder builder = new AlertDialog.Builder(BuyerMain.this);
        builder.setTitle("위치 비활성화");
        builder.setMessage("위치 서비스를 켜주세요");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", (dialog, which) -> {
            Intent callGps= new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(callGps,GPS_ENABLE_REQUEST_CODE);
        });

        //gps 안킬꺼야
        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());
        builder.create().show();
    }

    protected void onActivityResult(int requestCode,int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){//gps켯는지 검사

            case GPS_ENABLE_REQUEST_CODE:
                if(checkLocationServicesStatus()){
                    if(checkLocationServicesStatus()){
                        needRequest=true;
                        return;
                    }
                }
                break;
        }
    }


    private void setDefaultKeyMode() {
    }

}
