package edu.pdx.capstone.smartirrigation;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ConfigActivity extends AppCompatActivity implements ConnectionCallbacks, OnConnectionFailedListener
{
    GoogleApiClient mGoogleApiClient = null;
    Location mLastLocation;
    final int REQUEST_CODE_LOCATION_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null)
        {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }


    @Override
    public void onConnected(Bundle connectionHint)
    {
        /* New permissions model for API 23 and beyond only */
        int api_version = android.os.Build.VERSION.SDK_INT;
        if (api_version >= Build.VERSION_CODES.M)
        {
            int hasLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
            if (hasLocationPermission != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_CODE_LOCATION_PERMISSION);
                return;
            }
        }

    }

    @Override
    public void onConnectionSuspended(int param)
    {
        // Stub
    }

    @Override
    public void onConnectionFailed(ConnectionResult result)
    {
        // Stub
    }

    protected void onStart()
    {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop()
    {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    public void onClickButtonLatitudeGet(View view)
    {
        EditText input_latitude = (EditText) findViewById(R.id.input_latitude);

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null)
        {
            double latitude = mLastLocation.getLatitude();
            DecimalFormat decimal_format = new DecimalFormat("#.##");
            latitude = Double.valueOf(decimal_format.format(latitude));

            input_latitude.setText(String.valueOf(latitude));
        }
        else
        {
            input_latitude.setText(0);
        }
    }

    public void onClickButtonDateGet(View view)
    {
        EditText input_date = (EditText) findViewById(R.id.input_date);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat date_format = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        String date = date_format.format(calendar.getTime());

        input_date.setText(date);
    }

    /* Permissions for API 23 + */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        switch (requestCode)
        {
            case REQUEST_CODE_LOCATION_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_DENIED)
                {
                    Toast.makeText(ConfigActivity.this, "Access to Location Denied", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
