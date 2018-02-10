package com.example.admin.ghr;

import android.*;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VictimMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

private GoogleMap mMap;
        GoogleApiClient mGoogleApiClient;
        Location mLastLocation;
        LocationRequest mLocationRequest;
private Button mLogOut,mRequest;
private LatLng pickUp;
private Boolean requestBol=false;
private Marker pickUPMarker;
private Button mSetting,mInfo;
public static final int REQUEST_LOCATION_CODE=99;
private Button mHospital;
double latitude,longitude;
int PROXIMITY_RADIUS=10000;
private Boolean isLoggingout=false;
private int infoClicked=0;
private LinearLayout mAmbulanceInfo;
private ImageView mAmbulanceProfileImage;
private TextView mAmbulanceName,mAmbulancePhone,mAmbulanceNo,mAmbulanceHospital;
private RadioGroup mRadioGroup;
private String mService;


@Override
protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_victim_map);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ){
                checkLocationPermission();
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
        .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mLogOut=(Button)findViewById(R.id.logout);
        mLogOut.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
                        isLoggingout=true;
                        FirebaseAuth.getInstance().signOut();
                        Intent intent= new Intent(VictimMapActivity.this,MainActivity.class);
                        startActivity(intent);
                        finish();
                        return;
                }
        });

        mRadioGroup=(RadioGroup)findViewById(R.id.radioGroup);


        mAmbulanceInfo=(LinearLayout)findViewById(R.id.ambulance_info);
        mAmbulanceProfileImage=(ImageView)findViewById(R.id.ambulanceProfileImage);
        mAmbulanceName=(TextView)findViewById(R.id.ambulanceName);
        mAmbulancePhone=(TextView)findViewById(R.id.ambulancePhone);
        mAmbulanceNo=(TextView)findViewById(R.id.ambulanceNo);
        mAmbulanceHospital=(TextView)findViewById(R.id.ambulanceHospital);

        mRequest=(Button)findViewById(R.id.request);
        mRequest.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                        if(requestBol){//cancelling the ambulance......................................
                                requestBol=false;
                                geoQuery.removeAllListeners();
                                driverLocationRef.removeEventListener(driverLocationRefListner);
                                if(driverId!=null){
                                        DatabaseReference driverRef=FirebaseDatabase.getInstance().getReference().child("Users").child("Ambulance").child(driverId);
                                        driverRef.setValue(true);
                                        driverId=null;
                                }
                                driverFound=false;
                                radius=1;
                                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                DatabaseReference ref =FirebaseDatabase.getInstance().getReference("VictimRequest");
                                GeoFire geoFire=new GeoFire(ref);
                                geoFire.removeLocation(userId);
                                if(pickUPMarker!=null){
                                        pickUPMarker.remove();
                                        mdriverMarker.remove();
                                }

                                mAmbulanceNo.setText("");
                                mAmbulanceHospital.setText("");
                                mAmbulancePhone.setText("");
                                mAmbulanceName.setText("");
                                mAmbulanceProfileImage.setImageResource(R.mipmap.ic_launcher_user);
                                mRequest.setText("Call Ambulance");
                        }//..........................................................................
                        else {//calling ambulance........................................................
                                requestBol=true;
                                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                DatabaseReference ref =FirebaseDatabase.getInstance().getReference("VictimRequest");
                                GeoFire geoFire=new GeoFire(ref);
                                geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude()));
                                int selectId = mRadioGroup.getCheckedRadioButtonId();
                                final RadioButton radioButton=(RadioButton)findViewById(selectId);
                                mService=radioButton.getText().toString();
                                DatabaseReference ref1 =FirebaseDatabase.getInstance().getReference("VictimRequest").child(userId).child("Caller");
                                ref1.setValue(mService);
                                pickUp= new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude());
                                pickUPMarker = mMap.addMarker(new MarkerOptions().position(pickUp).title("Pick Up Here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_victim)));
                                mRequest.setText("Getting Ambulance");
                                getClosestAmbulance();
                        }//...............................................................................

                }
        });

        mInfo=(Button)findViewById(R.id.info) ;
        mInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                        infoClicked++;
                        if (infoClicked%2!=0 || infoClicked==0){
                                getAssignedAmbulanceInfo();
                        }
                        else {
                                mAmbulanceInfo.setVisibility(View.GONE);
                        }

                }
        });


        mHospital=(Button)findViewById(R.id.hospital);
        mHospital.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                        Intent intent=new Intent(VictimMapActivity.this,VictimHospitalActivity.class);
                        startActivity(intent);
                        return;
                }
        });

        mSetting=(Button)findViewById(R.id.setting);
        mSetting.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                        Intent intent=new Intent(VictimMapActivity.this,VictimSettingActivity.class);
                        startActivity(intent);
                        return;
                }
        });

        }

        private void getAssignedAmbulanceInfo() {
                mAmbulanceInfo.setVisibility(View.VISIBLE);
                DatabaseReference mAmbulanceDatabase= FirebaseDatabase.getInstance().getReference().child("Users").child("Ambulance").child(driverId);
                mAmbulanceDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                                        Map<String,Object> map=(Map<String,Object>)dataSnapshot.getValue();
                                        if(map.get("name")!=null){
                                                mAmbulanceName.setText(map.get("name").toString());
                                        }
                                        if(map.get("phone")!=null){
                                                mAmbulancePhone.setText(map.get("phone").toString());
                                        }
                                        if(map.get("ambulance no.")!=null){
                                                mAmbulanceNo.setText(map.get("ambulance no.").toString());
                                        }
                                        if(map.get("hospital")!=null){
                                                mAmbulanceHospital.setText(map.get("hospital").toString());
                                        }
                                        if(map.get("profileImageUrl")!=null){
                                                Glide.with(getApplicationContext()).load(map.get("profileImageUrl").toString()).into(mAmbulanceProfileImage);
                                        }

                                }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                });
        }


        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
                switch(requestCode){
                        case REQUEST_LOCATION_CODE:
                                if(grantResults.length>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                                        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
                                                if(mGoogleApiClient==null){
                                                        buildGoogleApiClient();
                                                }
                                                mMap.setMyLocationEnabled(true);
                                        }
                                }
                                else{
                                        Toast.makeText(this,"Permission Denied",Toast.LENGTH_LONG).show();
                                }
                                return;
                }
        }





        private int radius=1;
        private Boolean driverFound=false;
        private String driverId;

        GeoQuery geoQuery;
        private void getClosestAmbulance() {
                DatabaseReference ambulanceLocation=FirebaseDatabase.getInstance().getReference().child("AmbulanceAvailable");
                GeoFire geoFire=new GeoFire(ambulanceLocation);

                geoQuery=geoFire.queryAtLocation(new GeoLocation(pickUp.latitude,pickUp.longitude),radius);
                geoQuery.removeAllListeners();

                geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                        @Override
                        public void onKeyEntered(String key, GeoLocation location) {
                                if(driverFound==false && requestBol){
                                        driverFound=true;
                                        driverId=key;

                                        DatabaseReference driverRef=FirebaseDatabase.getInstance().getReference().child("Users").child("Ambulance").child(driverId);
                                        String customerId=FirebaseAuth.getInstance().getCurrentUser().getUid();

                                        HashMap hashMap=new HashMap();
                                        hashMap.put("victimRideId",customerId);
                                        driverRef.updateChildren(hashMap);

                                        getDriverLocation();
                                        mRequest.setText("Looking for driver's location");

                                }
                        }

                        @Override
                        public void onKeyExited(String key) {

                        }

                        @Override
                        public void onKeyMoved(String key, GeoLocation location) {

                        }

                        @Override
                        public void onGeoQueryReady() {
                                if(driverFound==false){
                                        radius++;
                                        getClosestAmbulance();
                                }
                        }

                        @Override
                        public void onGeoQueryError(DatabaseError error) {

                        }
                });
        }


        private Marker mdriverMarker;
        private DatabaseReference driverLocationRef;
        private ValueEventListener driverLocationRefListner;

        private void getDriverLocation() {
                driverLocationRef = FirebaseDatabase.getInstance().getReference().child("AmbulanceWorking").child(driverId).child("l");
                driverLocationRefListner = driverLocationRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists()){
                                        List<Object> map= (List<Object>) dataSnapshot.getValue();
                                        double LocationLat=0;
                                        double LocationLng=0;
                                        mRequest.setText("Ambulance Found");
                                        if(map.get(0)!=null){
                                                LocationLat=Double.parseDouble(map.get(0).toString());
                                        }
                                        if(map.get(1)!=null){
                                                LocationLng=Double.parseDouble(map.get(1).toString());
                                        }
                                       LatLng driverLatLng= new LatLng(LocationLat,LocationLng);
                                        if(mdriverMarker!=null){
                                                mdriverMarker.remove();
                                        }

                                        Location loc1=new Location("");
                                        loc1.setLatitude(pickUp.latitude);
                                        loc1.setLongitude(pickUp.longitude);

                                        Location loc2=new Location("");
                                        loc2.setLatitude(driverLatLng.latitude);
                                        loc2.setLongitude(driverLatLng.longitude);

                                        float distance=loc1.distanceTo(loc2);

                                        if(distance<100){
                                                mRequest.setText("Ambulance is Here");
                                        }else{
                                                mRequest.setText("Ambulance Found, Distance:"+String.valueOf(distance));
                                        }

                                        mdriverMarker=mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Your Ambulance").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_ambulance)));
                                }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                });
        }


        @Override
public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
                if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                        buildGoogleApiClient();
                        mMap.setMyLocationEnabled(true);
                }

        }

protected synchronized void buildGoogleApiClient(){
        mGoogleApiClient=new GoogleApiClient.Builder(this)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .addApi(LocationServices.API)
        .build();
        mGoogleApiClient.connect();


        }
@Override
public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest,this);
        }
        }

@Override
public void onLocationChanged(Location location) {
        mLastLocation = location;
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        LatLng latlng=new LatLng(location.getLatitude(),location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(18));

        if(mGoogleApiClient!=null){
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,this);
        }
        }



public boolean checkLocationPermission(){
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)){
                        ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_LOCATION_CODE);
                }
                else {
                        ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_LOCATION_CODE);
                }
                return false;
        }
        else
                return true;
}


@Override
public void onConnectionSuspended(int i) {

        }

@Override
public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        }

        private void disconnectDriver(){
               // String userid= FirebaseAuth.getInstance().getCurrentUser().getUid();

               // DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("VictimRequest");
               // ref.child(userid).child("l").child("0").removeValue();
               // ref.child(userid).child("l").child("1").removeValue();
               // ref.child(userid).child("g").removeValue();
               // GeoFire geofire=new GeoFire(ref);
               // geofire.removeLocation(userid);
        }

        @Override
        protected void onStop() {
                super.onStop();
                if(!isLoggingout){
                        disconnectDriver();
                }


        }

}
