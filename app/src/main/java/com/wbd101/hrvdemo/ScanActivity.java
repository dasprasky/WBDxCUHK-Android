package com.wbd101.hrvdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import java.util.Iterator;
import java.util.LinkedList;

public class ScanActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothLeScanner bleScanner=null;
    private ScanCallback scanCallback;
    private scanResultAdapter scanAdapter;
    private LinkedList<ScanResult> scanResults = new LinkedList<>();

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private boolean isScanning = false;

    private Button scan_btn;
    private CheckBox checkbox_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        Intent intent = getIntent();
        int age = intent.getIntExtra("age",0);
        String id = intent.getStringExtra("student_id");

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        scan_btn = (Button) findViewById(R.id.search_button);
        checkbox_btn = (CheckBox) findViewById(R.id.checkBox);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        scanAdapter = new scanResultAdapter(this, scanResults, id, age);
        recyclerView.setAdapter(scanAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

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
    @Override
    protected void onPause() {
        if (bleScanner != null) {
            stopScan();
            bleScanner = null;
        }
        super.onPause();
    }
    @Override
    protected void onStop() {
        if (bleScanner != null) {
            stopScan();
            bleScanner = null;
        }
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        if (bleScanner != null) {
            stopScan();
            bleScanner = null;
        }
        super.onDestroy();

    }

    private void startScan(){
        bleScanner = mBluetoothAdapter.getBluetoothLeScanner();
        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        scanAdapter.notifyDataSetChanged();
        boolean unnamed = checkbox_btn.isChecked();
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                Iterator<ScanResult> it = scanResults.iterator();
                boolean present = false;
                while(it.hasNext()){
                    ScanResult item = it.next();
                    if(item.getDevice().getAddress().equals(result.getDevice().getAddress()))
                        present = true;
                }
                if(!present){
                    Log.w("ScanCallback", "Found unique BLE device! Name : " + result.getDevice().getName() + " address: " +result.getDevice().getAddress());
                    ScanRecord scanRecord = result.getScanRecord();
                    if(scanRecord != null){
                        int flag = scanRecord.getAdvertiseFlags();
                        String bytes = bytesToHex(scanRecord.getBytes());
                        SparseArray<byte[]> manufacturerData = scanRecord.getManufacturerSpecificData();
//                        int transmissionPower = scanRecord.getTxPowerLevel();
                        Log.i("ScanRecord-flag: ", " "+flag);
                        Log.i("ScanRecord-bytes: ", bytes);
                        for( int i =0; i < manufacturerData.size(); i++){
                            int id = manufacturerData.keyAt(i);
                            byte[] obj = manufacturerData.get(id);
                            Log.i("ScanRecord-MFData: ", "id: "+ id+" obj: "+bytesToHex(obj));
                        }
//                        Log.i("ScanRecord-txPower: ", " "+transmissionPower);
                    }
                    if(result.getDevice().getName()==null){
                        if(unnamed){
                            scanResults.add(result);
                            scanAdapter.notifyItemInserted(scanResults.size()-1);
                        }
                        //don't add unnamed devices if checkbox is not clicked
                    }
                    else{
                        scanResults.add(result);
                        scanAdapter.notifyItemInserted(scanResults.size()-1);
                    }
                }

            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.e("ScanCallback", "onScanFailed: code " + errorCode);
            }
        };
        scanResults.clear();
        bleScanner.startScan(null, scanSettings, scanCallback);
        isScanning = true;

    }
    private void stopScan(){
        bleScanner.stopScan(scanCallback);
        isScanning = false;
        scan_btn.setText("SEARCH FOR DEVICES");
    }

    public void search_button(View v) {
        if(isScanning){
            stopScan();
        }
        else{
            startScan();
            scan_btn.setText("STOP SEARCH");
        }
    }
    /**
     * Convert bytes into hex string.
     */
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        if ((bytes == null) || (bytes.length <= 0)) {
            return "";
        }

        char[] hexChars = new char[bytes.length * 3 - 1];

        for (int j=0; j<bytes.length; ++j) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            if (j < bytes.length - 1) {
                hexChars[j * 3 + 2] = 0x20;           // hard coded space
            }
        }

        return new String(hexChars);
    }
}