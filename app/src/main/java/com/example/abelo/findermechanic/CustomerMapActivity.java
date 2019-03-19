package com.example.abelo.findermechanic;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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

import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;

    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private Button mLogout, mRequest, mSettings;
    private LatLng repairLocation;
    private boolean requestBol = false;
    private LinearLayout mMechanicInfo;

    private ImageView mMechanicProfileImage;

    private TextView mMechanicName, mMechanicPhone, mMechanicIdNO, mMechanicSpecialisation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
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
                    mMap.addMarker(new MarkerOptions().position(repairLocation).title("repair here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.cu)));
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
        DatabaseReference MechanicLocation = FirebaseDatabase.getInstance().getReference().child("MechanicAvailable");
        GeoFire geoFire = new GeoFire(MechanicLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(repairLocation.latitude, repairLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!mechanicFound && requestBol) {
                    mechanicFound = true;
                    mechanicFoundID = key;

                    DatabaseReference mechanicRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Mechanics").child("mechanicFoundID");
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
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!mechanicFound) {
                    radius = 2;
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
        mechanicLocationRef = FirebaseDatabase.getInstance().getReference().child("MechanicsWorking").child(mechanicFoundID).child("l");
        mechanicLocationRefListener = mechanicLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && requestBol) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationlat = 0;
                    double locationlng = 0;
                    mRequest.setText("Mechanic found");
                    if (map.get(0) != null) {
                        locationlat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null) {
                        locationlng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng mechanicLatLng = new LatLng(locationlat, locationlng);
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
                        mRequest.setText("Mechanic found" + String.valueOf(distance));


                    }


                    mMechanicMarker = mMap.addMarker(new MarkerOptions().position(mechanicLatLng).title("your mechanic").icon(BitmapDescriptorFactory.fromResource(R.mipmap.mec2)));
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
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) ;

                Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                if (map.get("name") != null) {
                    mMechanicName.setText(map.get("name").toString());

                }
                if (map.get("phone") != null) {
                    mMechanicPhone.setText(map.get("phone").toString());
                }

                if (map.get("IDno") != null) {
                    mMechanicIdNO.setText(map.get("IDno").toString());
                }

                if (map.get("specialisation") != null) {
                    mMechanicSpecialisation.setText(map.get("specialisation").toString());
                }


                if (map.get("profileImageUrl") != null) {
                    Glide.with(getApplication()).load(map.get("ProfileImageUrl").toString()).into(mMechanicProfileImage);
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
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);

    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));


    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onStop() {
        super.onStop();

    }
}
