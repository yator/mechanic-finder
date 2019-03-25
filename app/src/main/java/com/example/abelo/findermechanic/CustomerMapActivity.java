package com.example.abelo.findermechanic;

import android.*;
import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
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

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private Button mLogout, mRequest, mSettings;
    private LatLng repairLocation;
    private SupportMapFragment mapFragment;
    private boolean requestBol = false;
    private LinearLayout mMechanicInfo;

    private ImageView mMechanicProfileImage;

    private TextView mMechanicName, mMechanicPhone, mMechanicIdNO, mMechanicSpecialisation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        mMechanicInfo = (LinearLayout) findViewById(R.id.mechanicInfo);

        mMechanicProfileImage = (ImageView) findViewById(R.id.mechanicProfileImage);

        mMechanicName = (TextView) findViewById(R.id.mechanicName);
        mMechanicPhone = (TextView) findViewById(R.id.mechanicPhone);
        mMechanicIdNO = (TextView) findViewById(R.id.mechanicIdNo);

        mMechanicSpecialisation = (TextView) findViewById(R.id.mechanicSpecialisation);


        mLogout = (Button) findViewById(R.id.logout);
        mRequest = (Button) findViewById(R.id.request);
        mSettings = (Button) findViewById(R.id.settings);


        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestBol) {
                    requestBol = false;
                    geoQuery.removeAllListeners();
                    mechanicLocationRef.removeEventListener(mechanicLocationRefListener);


                } else {
                    requestBol = true;

                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

                    repairLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(repairLocation).title("repair here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.du)));
                    mRequest.setText("Getting your Mechanic...");
                    getClosestMechanic();

                }
            }
        });

        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomerMapActivity.this, CustomerSettingsActivity.class);
                startActivity(intent);
                return;
            }
        });
    }

    private int radius = 1;
    private boolean mechanicFound = false;
    private String mechanicFoundID;

    GeoQuery geoQuery;

    private void getClosestMechanic() {
        DatabaseReference MechanicLocation = FirebaseDatabase.getInstance().getReference().child("mechanicsAvailable");
        GeoFire geoFire = new GeoFire(MechanicLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(repairLocation.latitude, repairLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!mechanicFound && requestBol) {
                    DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Mechanics").child(key);
                    mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                                                                         @Override
                                                                         public void onDataChange(DataSnapshot dataSnapshot) {
                                                                             if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                                                                                 Map<String, Object> mechanicMap = (Map<String, Object>) dataSnapshot.getValue();
                                                                                 if (mechanicFound) {
                                                                                     return;
                                                                                 }
                                                                                 mechanicFound = true;
                                                                                 mechanicFoundID = dataSnapshot.getKey();

                                                                                 DatabaseReference mechanicRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Mechanics").child(mechanicFoundID).child("customerRequest");
                                                                                 String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                                                                 HashMap map = new HashMap();
                                                                                 map.put("customerRepairId", customerId);
                                                                                 mechanicRef.updateChildren(map);

                                                                                 getMechanicLocation();
                                                                                 getMechanicInfo();
                                                                                 mRequest.setText("looking for Mechanic Location......");

                                                                             }

                                                                         }

                                                                         @Override
                                                                         public void onCancelled(DatabaseError databaseError) {

                                                                         }

                                                                     }
                    );
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
                if (!mechanicFound) {
                    radius++;
                    getClosestMechanic();

                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    private Marker mMechanicMarker;

    private DatabaseReference mechanicLocationRef;
    private ValueEventListener mechanicLocationRefListener;

    private void getMechanicLocation() {
        mechanicLocationRef = FirebaseDatabase.getInstance().getReference().child("mechanicsWorking").child(mechanicFoundID).child("l");
        mechanicLocationRefListener = mechanicLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && requestBol) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    if (map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng mechanicLatLng = new LatLng(locationLat, locationLng);
                    if (mMechanicMarker != null) {
                        mMechanicMarker.remove();
                    }
                    Location loc1 = new Location("");
                    loc1.setLatitude(repairLocation.latitude);
                    loc1.setLongitude(repairLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(mechanicLatLng.latitude);
                    loc2.setLongitude(mechanicLatLng.longitude);

                    float distance = loc1.distanceTo(loc2);

                    if (distance < 100) {
                        mRequest.setText("Mechanic is here");
                    } else {
                        mRequest.setText("Mechanic found: " + String.valueOf(distance));
                    }


                    mMechanicMarker = mMap.addMarker(new MarkerOptions().position(mechanicLatLng).title("your Mechanic").icon(BitmapDescriptorFactory.fromResource(R.mipmap.mec2)));
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }


    private void getMechanicInfo() {
        mMechanicInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Mechanics").child(mechanicFoundID);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {


            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    if (dataSnapshot.child("name") != null) {
                        mMechanicName.setText(dataSnapshot.child("name").getValue().toString());
                    }
                    if (dataSnapshot.child("phone") != null) {
                        mMechanicPhone.setText(dataSnapshot.child("phone").getValue().toString());
                    }
                    if (dataSnapshot.child("IDno") != null) {
                        mMechanicIdNO.setText(dataSnapshot.child("IDno").getValue().toString());
                    }

                    if (dataSnapshot.child("specialisation") != null) {
                        mMechanicSpecialisation.setText(dataSnapshot.child("specialisation").getValue().toString());
                    }
                    if (dataSnapshot.child("profileImageUrl").getValue() != null) {
                        Glide.with(getApplication()).load(dataSnapshot.child("profileImageUrl").getValue().toString()).into(mMechanicProfileImage);
                    }
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
        mMap = googleMap;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

            }else{
                checkLocationPermission();
            }
        }
    }



    LocationCallback mLocationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for(Location location : locationResult.getLocations()){
                if(getApplicationContext()!=null){
                    mLastLocation = location;

                    LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());

                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

                }
            }
        }
    };

    private void checkLocationPermission() {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                new android.app.AlertDialog.Builder(this)
                        .setTitle("give permission")
                        .setMessage("give permission message")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        })
                        .create()
                        .show();
            }
            else{
                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case 1:{
                if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                } else{
                    Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }




    @Override
    protected void onStop() {
        super.onStop();

    }
}
