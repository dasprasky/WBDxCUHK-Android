package com.wbd101.hrvdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.onesignal.OneSignal;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter = null;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    SharedPreferences sp;
    private static final String ONESIGNAL_APP_ID = "eefd8e75-bc11-4803-bd66-5f67de015007";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Enable verbose OneSignal logging to debug issues if needed.
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);

        // OneSignal Initialization
        OneSignal.initWithContext(this);
        OneSignal.setAppId(ONESIGNAL_APP_ID);

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "ble_not_supported", Toast.LENGTH_SHORT).show();
            finish();
        }
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        sp = getSharedPreferences("login",MODE_PRIVATE);
//        sp.edit().clear().apply();
        if(sp.getBoolean("logged",false)){
            goToTabActivity(sp.getInt("age",0), sp.getString("id", "0"));
        }

        EditText id_value = (EditText)findViewById(R.id.id_value);
        EditText age_value = (EditText)findViewById(R.id.age_value);
        Button agree_continue = (Button)findViewById(R.id.continue_button);
        agree_continue.setOnClickListener( v -> {
            TextInputLayout id_layout = (TextInputLayout)findViewById(R.id.id_input_layout);
            TextInputLayout age_layout = (TextInputLayout)findViewById(R.id.age_input_layout);
            id_layout.setError(null);
            age_layout.setError(null);
            if(age_value.getText()!=null || id_value.getText()!=null){
                if(id_value.getText().toString().isEmpty()) {id_layout.setError("Enter a valid student ID"); return;}
                if(age_value.getText().toString().isEmpty()||Integer.parseInt(age_value.getText().toString())==0) {age_layout.setError("Enter a valid age"); return;}
                if(id_value.getText().toString().length() != 10) {id_layout.setError("Invalid Student ID! Student ID must be 10 digits"); return;}
                String id = id_value.getText().toString();
                int age = Integer.parseInt(age_value.getText().toString());

                // can check values to age and id here, depending on requirements

                //using shared preferences so that the same user doesn't neet to enter student id and age again
                goToTabActivity(age, id);
                sp.edit().putBoolean("logged", true).apply();
                sp.edit().putString("id", id).apply();
                sp.edit().putInt("age", age).apply();

            }
        });
    }
    public void goToTabActivity(int age, String id){
//        if(age==0 || id==0){
//            Toast.makeText(this, "STUDENT ID and AGE cannot be 0",Toast.LENGTH_SHORT).show();
//            return;
//        }
        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
        intent.putExtra("age", age);
        intent.putExtra("student_id", id);
        startActivity(intent);
    }
    @Override
    protected void onResume() {
        super.onResume();

        if (!mBluetoothAdapter.isEnabled()) {
            final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_PHONE_STATE)
                        != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSIONS);
        }

    }
}