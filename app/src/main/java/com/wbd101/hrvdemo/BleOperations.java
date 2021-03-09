package com.wbd101.hrvdemo;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class BleOperations extends AppCompatActivity {
    private BluetoothGatt gatt;
    private static final int GATT_MAX_MTU_SIZE = 517;
    private ArrayList<BluetoothGattCharacteristic> characteristics = new ArrayList<>();
    private static final UUID DEVICE_NAME = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");
    private static final UUID HEART_RATE_MEASURE = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private static final UUID TEMPERATURE_MEASURE = UUID.fromString("00002a1c-0000-1000-8000-00805f9b34fb");
    private int age = 23;
    private int rri_progress = 0;
    private ProgressBar progressBar;
    private Handler handler = new Handler();
    private String hr_csv, resp_csv, hrv_csv;
    private ProgressBar stressBar, fatigueBar;
    private final int max_window = 160;
    private boolean record = false;
    private Button disconnect_button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_operations);
        Intent intent  = getIntent();

        // Register for broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        registerReceiver(mReceiver, filter);

        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        int age = intent.getIntExtra("age",0);
        String id = intent.getStringExtra("student_id");

//        Toast.makeText(getApplicationContext(), "Connecting to device "+ device.getAddress(), Toast.LENGTH_SHORT).show();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    gatt = device.connectGatt(getApplicationContext(), false, gattCallback, BluetoothDevice.TRANSPORT_LE); // handle devices with same MAC for both classic BT and BLE

                } else {
                    gatt = device.connectGatt(getApplicationContext(), false, gattCallback);
                }
            }
        });

        TextView device_name = (TextView) findViewById(R.id.device);
        device_name.setText(device.getName());
        TextView student_id = (TextView)findViewById(R.id.student_id_value);
        student_id.setText(id);
        TextView student_age = (TextView)findViewById(R.id.age_value);
        student_age.setText(String.format(Locale.US,"%d", age));

        //native methods
        AndroidHRVAPI.init_hrv_analysis();
        AndroidRespirationAPI.init_respiration_analysis();

        //UI
        disconnect_button = (Button)findViewById(R.id.disconnect_button);
        disconnect_button.setOnClickListener(v -> {
            gatt.disconnect();
            finish();
        });
        Button reset_button = (Button)findViewById(R.id.data_record_button);
        reset_button.setOnClickListener(v -> {

        });
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setMax(max_window);
        progressBar.getProgressDrawable().setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_IN);
        //stressbar
        stressBar = (ProgressBar)findViewById(R.id.progressBar3);
        fatigueBar = (ProgressBar)findViewById(R.id.progressBar4);
        AnimationDrawable ad = getProgressBarAnimation();
        stressBar.getProgressDrawable().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
        fatigueBar.getProgressDrawable().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
        stressBar.setBackgroundDrawable(ad);
        fatigueBar.setBackgroundDrawable(ad);
        stressBar.setMax(100);
        stressBar.setProgress(10);
        fatigueBar.setMax(100);
        fatigueBar.setProgress(10);

        Button record_button = (Button)findViewById(R.id.data_record_button);
        record_button.setOnClickListener(v -> {
            String rec_button_default_text = getString(R.string.record_data);
            if (record_button.getText().equals(rec_button_default_text)){
                record = true;
                try {
                    Date dNow = new Date();
                    SimpleDateFormat timeStamp =
                            new SimpleDateFormat("yyyy-MM-dd'_'HH:mm:ss");
                    String folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/tws";
                    File directory = new File(folder);
                    if (!directory.exists()) {
                        directory.mkdir();
                    }
                    String hr_filename = "/tws/"+id+"_hr_" + timeStamp.format(dNow.getTime()) + ".csv";
                    hr_csv = (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + hr_filename);
                    new File(hr_csv);
                    String respiratory_filename = "/tws/"+id+"_resp_" + timeStamp.format(dNow.getTime()) + ".csv";
                    resp_csv = (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + respiratory_filename);
                    new File(resp_csv);
                    String hrv_filename = "/tws/"+id+"_hrv_" + timeStamp.format(dNow.getTime()) + ".csv";
                    hrv_csv = (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + hrv_filename);
                    new File(hrv_csv);

                    Log.w("onInfo", "new files created");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                record_button.setText("SAVE");
            }
            else{
                String bucketName = "cuhk-wbd-app-files";
                record_button.setText(rec_button_default_text);
                record = false;
                AsyncTaskRunner runner = new AsyncTaskRunner();
                runner.execute();
            }
        });
    }
    @Override
    public void onBackPressed(){
        disconnect_button.performClick();
    }
    private class AsyncTaskRunner extends AsyncTask<String, String, String> {

        private String resp;
        ProgressDialog progressDialog;

        @Override
        protected String doInBackground(String... params) {
            try {
                final String accessKey = "AKIAQ47DEEO5EIKJOJXY";
                final String secret = "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";
                BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey,secret);
                AmazonS3 s3Client = new AmazonS3Client(credentials);
                s3Client.setRegion(Region.getRegion(Regions.AP_NORTHEAST_1));
                s3Client.setEndpoint("https://s3-ap-northeast-1.amazonaws.com/");
                List<Bucket> buckets= s3Client.listBuckets();
                Log.w("bucket_size - ", "s "+buckets.size());
                TransferUtility transferUtility = TransferUtility.builder()
                        .defaultBucket("cuhk-wbd-android-files").context(getApplicationContext()).s3Client(s3Client).build();

                Date dNow = new Date();
                SimpleDateFormat timeStamp =
                        new SimpleDateFormat("yyyy-MM-dd'_'HH:mm:ss");
                String filename = "hr_" + timeStamp.format(dNow.getTime()) + ".csv";
                TransferObserver transferObserver = transferUtility.upload("cuhk-wbd-android-files",filename, new File(hr_csv), CannedAccessControlList.BucketOwnerRead);
                transferObserver.setTransferListener(new TransferListener() {

                    @Override
                    public void onStateChanged(int id, TransferState state) {
                        if (state.COMPLETED.equals(transferObserver.getState())) {
                            Toast.makeText(getApplicationContext(), "File Upload Complete", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                        Log.i("onProgressChanged ", String.format(Locale.US,"%.2f", (((float)bytesCurrent/ bytesCurrent) * 100.0)));
                    }

                    @Override
                    public void onError(int id, Exception exception) {
                        Log.e("upload-error", exception.getMessage());
                    }

                });
            } catch (Exception e) {
                e.printStackTrace();
                resp = e.getMessage();
            }
            return resp;
        }


        @Override
        protected void onPostExecute(String result) {
            // execution of result of Long time consuming operation
            progressDialog.dismiss();
        }


        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(BleOperations.this,
                    "ProgressDialog",
                    "Wait");
        }


        @Override
        protected void onProgressUpdate(String... text) {
        }
    }
    // Create a BroadcastReceiver for bluetooth actions
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Toast.makeText(getApplicationContext(), "Connected to "+ device.getAddress(), Toast.LENGTH_SHORT).show();
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)){
                Toast.makeText(getApplicationContext(), "Device Disconnected", Toast.LENGTH_SHORT).show();
            }
        }
    };


    private AnimationDrawable getProgressBarAnimation(){

        GradientDrawable rainbow1 = new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT,
                new int[] {Color.RED,   Color.YELLOW, Color.GREEN});

        GradientDrawable rainbow2 = new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT,
                new int[] { Color.GREEN, Color.RED,   Color.YELLOW});

        GradientDrawable rainbow3 = new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT,
                new int[] { Color.YELLOW, Color.GREEN, Color.RED });

//        GradientDrawable rainbow4 = new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT,
//                new int[] { Color.YELLOW, Color.GREEN, Color.RED});
//
//        GradientDrawable rainbow5 = new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT,
//                new int[] {  Color.YELLOW, Color.GREEN, Color.RED });
//
//        GradientDrawable rainbow6 = new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT,
//                new int[] {Color.MAGENTA, Color.YELLOW, Color.GREEN, Color.RED });

        GradientDrawable[]  gds = new GradientDrawable[] {rainbow1, rainbow2, rainbow3};

        AnimationDrawable animation = new AnimationDrawable();

        for (GradientDrawable gd : gds){
            animation.addFrame(gd, 100);
        }
        animation.setOneShot(false);

        return animation;
    }


    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(status == gatt.GATT_SUCCESS){
                if(newState == BluetoothProfile.STATE_CONNECTED){
                    Log.w("BluetoothGattCallback", "Successfully connected to device "+gatt.getDevice().getAddress());
                    new Handler(Looper.getMainLooper()).post(new Runnable(){
                        @Override
                        public void run(){
                            boolean ans = gatt.discoverServices();
                            Log.d("onConnectionStateChange", "Discover services started: "+ ans);
                            gatt.requestMtu(GATT_MAX_MTU_SIZE);
                        }
                    });
                }
                else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                    Log.w("BluetoothGattCallback", "Successfully disconnected form device "+ gatt.getDevice().getAddress());
                    gatt.close();
                }
                else{
                    Log.w("BluetoothGattCallback", "Error "+status+" encountered for "+gatt.getDevice().getAddress()+ "\nDisconnecting...");
                    gatt.close();
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            List<BluetoothGattService> services = gatt.getServices();
            Log.w("BluetoothGattCallback", "Discovered "+ services.size()+" services for "+gatt.getDevice().getAddress());

            for(int i = 0; i< services.size(); i++){
                List<BluetoothGattCharacteristic> characteristic = services.get(i).getCharacteristics();
                Log.w("BluetoothGattCallback", "Discovered Characteristic of size "+ characteristic.size()+" for "+ services.get(i).getUuid());
                for(int j = 0; j<characteristic.size(); j++){
                    characteristics.add(characteristic.get(j));
                    Log.w("BluetoothGattCallback", "Discovered Characteristic"+ characteristic.get(j).getUuid()+" for "+gatt.getDevice().getAddress());
                }
            }
            Log.w("BluetoothGattCallback", "Total Characteristics = "+ characteristics.size());
            characteristicsOperations(characteristics);

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if(status == BluetoothGatt.GATT_SUCCESS){
                Log.i("BluetoothGattCallback", "Read characteristic success for "+ characteristic.getUuid().toString() +" value: "+ new String(characteristic.getValue(), StandardCharsets.UTF_8));
            }
            else if(status == BluetoothGatt.GATT_READ_NOT_PERMITTED){
                Log.i("BluetoothGattCallback", "Read not permitted for  "+ characteristic.getUuid().toString());
            }
            else{
                Log.i("BluetoothGattCallback", "Characteristic read failed for  "+ characteristic.getUuid().toString());
                gatt.disconnect();
                finish();
            }

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] value = characteristic.getValue();
            broadcastUpdate(characteristic, value);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            boolean event = status ==BluetoothGatt.GATT_SUCCESS;
            Log.w("onMtuChanged", "ATT MTU changed to "+mtu+" "+ event);
        }
    };
    private void broadcastUpdate(final BluetoothGattCharacteristic characteristic, byte[] value) {
        if (characteristic.getUuid().equals(HEART_RATE_MEASURE)) {
            String value_str = bytesToHex(value);
            String[] line = value_str.split(" ");
            int heart_rate = Integer.parseInt(line[1], 16);
            Log.w("broadcastUpdate", "Heart Rate: "+heart_rate);
            if(heart_rate!=255) {
                TextView hr = (TextView) findViewById(R.id.hr_value);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hr.setText(Integer.toString(heart_rate));
                    }
                });

            }
            float rri_value = 0.0f;
            if(line.length>2) {
                rri_progress++;
                rri_value = (float)Integer.parseInt(line[3] + line[2], 16) * 1000 / 1024;
                Log.w("broadcastUpdate", "RRI value: "+rri_value);

                //native
                AndroidHRVAPI.hrv_analysis((int) (System.currentTimeMillis() / 1000L), (short)heart_rate, (short)rri_value);
                AndroidRespirationAPI.analyze_respiration(rri_value);

                hrv_analysis_results();
            }
            if(record){
                SimpleDateFormat format = new SimpleDateFormat ("HH.mm.ss.SSS");
                Date d = new Date();
                List<String> processed_line = new ArrayList<String>();
                processed_line.add(format.format(d.getTime()));
                processed_line.add(Integer.toString(heart_rate));
                processed_line.add(Float.toString(rri_value));

                String[] final_line = new String[processed_line.size()];
                processed_line.toArray(final_line);
                csv_writer(final_line, hr_csv);
            }
        }
        if (characteristic.getUuid().equals(TEMPERATURE_MEASURE)) {
            String result = "No data";
            TextView temp = (TextView)findViewById(R.id.temp_value);
            if (value.length >= 5) {
                boolean isFahrenheit = (value[0] & 0x01) == 1;
                int temperature_mantissa = (value[1] & 0xFF) | ((value[2] & 0xFF) << 8) | ((value[3] & 0xFF) << 16);
                double temperature = temperature_mantissa * Math.pow(10.0, 1.0 * value[4]);
                result = String.format(Locale.US, "%.2f°%s", temperature, (isFahrenheit) ? "F" : "C");
                if(temperature<220.0 && rri_progress>10){       //the initial value is always 30 so we wait for 10 rris and make asure we don't see any high values (so<220) that is returned before we get actual values
                    String finalResult = result.substring(0,5); //removing the unit because UI is programmed separately
                    runOnUiThread(new Runnable(){
                        @Override
                        public void run(){
                            temp.setText(finalResult);
                            TextView tempUnit = (TextView)findViewById(R.id.tempUnit);
                            if(!isFahrenheit){
                                tempUnit.setText(R.string.celsius);
                            }else tempUnit.setText(R.string.fahrenheit);
                        }
                    });
                }
            }else{
                runOnUiThread(new Runnable(){
                    @Override
                    public void run(){
                        temp.setText("-");
                    }
                });}


            Log.w("broadcastUpdate", "Temperature: "+ result);
        }

    }


    private void hrv_analysis_results(){
        TextView rr = (TextView)findViewById(R.id.rr_value);
        if(rri_progress > 30) {     //because rr takes aroud 30 mins to produce results

            respiration_result_t respiraResult = AndroidRespirationAPI.get_respiration_result();
            int resp_rate = respiraResult.getRespiratory_rate();
            float curr_depth = respiraResult.getRespiration_current_depth();
            String is_inspirating = (respiraResult.getIs_inspirating()) ? "True" : "False";
            Log.w("onCreate-HRV_Results", "Respiratory rate: " + resp_rate);
            Log.w("onCreate-HRV_Results", "Current depth: " + curr_depth);
            Log.w("onCreate-HRV_Results", "is_inspirating: " + is_inspirating);
            runOnUiThread(new Runnable(){
                @Override
                public void run(){
                    rr.setText(String.format(Locale.US,"%d", respiraResult.getRespiratory_rate()));
                }
            });
            if(record){
                SimpleDateFormat format = new SimpleDateFormat ("HH.mm.ss.SSS");
                Date d = new Date();
                List<String> processed_line = new ArrayList<String>();
                processed_line.add(format.format(d.getTime()));
                processed_line.add(Integer.toString(resp_rate));
                processed_line.add(Float.toString(curr_depth));
                processed_line.add(is_inspirating);

                String[] final_line = new String[processed_line.size()];
                processed_line.toArray(final_line);
                csv_writer(final_line, resp_csv);
            }

        }
        if(rri_progress <= max_window) {
            new Thread(new Runnable() {
                public void run() {

                    // Update the progress bar and display the
                    //current value in the text view
                    handler.post(new Runnable() {
                        public void run() {
                            progressBar.setProgress(rri_progress);
//                            textView.setText(rri_progress + "/" + progressBar.getMax());

                            //rr.setText("-");
                            //stress.setText("-");
                            //fatigue.setText("-");
                        }
                    });
                    try {
                        // Sleep for 200 milliseconds.
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        else{
            hrv_result_t currResult = AndroidHRVAPI.HRV_Get_Analysis_Result();
            int timestamp = currResult.getResult_timestamp();
            long total_rr_count =  currResult.getTotal_rr_cnt();
            int rr_window_count = currResult.getWindow_rr_cnt();
            int valid_rr_count = currResult.getValid_rr_cnt();
            int stress_index = currResult.getStress_index();
            int pNN50 = currResult.getPNN50();
            float rmssd = currResult.getRMSSD();
            float score = currResult.getHRV_Score();
            float dfa_slope1= currResult.getDfa_slope1();
            float dfa_slope2 = currResult.getDfa_slope2();
            float SDNN = currResult.getSDNN180();
            int respiratory_rate = currResult.getRespiratory_rate();
            float vlf = currResult.getVlf();
            float lf = currResult.getLf();
            float hf =  currResult.getHf();
            float lf_nu = currResult.getLf_nu();
            float hf_nu = currResult.getHf_nu();
            float lf_to_hf = currResult.getLf_to_hf();
            float tp = currResult.getTotal_power();
            float conf_lvl = currResult.getResult_conf_level();
            Log.w("onCreate-HRV_Results", "Timestamp: " + timestamp);
            Log.w("onCreate-HRV_Results", "Total RR count: " + total_rr_count);
            Log.w("onCreate-HRV_Results", "Window RR count: " + rr_window_count);
            Log.w("onCreate-HRV_Results", "Valid RR count: " + valid_rr_count);
            Log.w("onCreate-HRV_Results", "Stress index: " + stress_index);
            Log.w("onCreate-HRV_Results", "pNN50: " + pNN50);
            Log.w("onCreate-HRV_Results", "rMSSD: " + rmssd);
            Log.w("onCreate-HRV_Results", "HRV score: " + score );
            Log.w("onCreate-HRV_Results", "DFA slope 1: " + dfa_slope1 );
            Log.w("onCreate-HRV_Results", "DFA slope 2: " + dfa_slope2);
            Log.w("onCreate-HRV_Results", "SDNN: " + SDNN);
            Log.w("onCreate-HRV_Results", "Respiratory rate: " + respiratory_rate);
            Log.w("onCreate-HRV_Results", "VLF: " + vlf);
            Log.w("onCreate-HRV_Results", "LF: " + lf);
            Log.w("onCreate-HRV_Results", "HF: " + hf);
            Log.w("onCreate-HRV_Results", "LFnu: " + lf_nu);
            Log.w("onCreate-HRV_Results", "HFnu: " + hf_nu);
            Log.w("onCreate-HRV_Results", "LF/HF: " + lf_to_hf);
            Log.w("onCreate-HRV_Results", "TP: " + tp);
            Log.w("onCreate-HRV_Results", "Conf Lvl: " + conf_lvl);

            int percentage_stress = AndroidHRVStressAPI.get_percentage_stress(currResult.getRMSSD(), age);
            Log.w("onCreate-HRV_Results", "Stress Lvl: " + percentage_stress);


            runOnUiThread(new Runnable(){
                @Override
                public void run(){
                    //stress.setText(String.format(Locale.US,"%d",percentage_stress));
                    stressBar.setProgress((100 - percentage_stress));
                    fatigueBar.setProgress((int)(100 - (100*currResult.getLf_to_hf())));
                    //fatigue.setText(String.format(Locale.US,"%.2f",currResult.getLf_to_hf()));

                }
            });
            if(record){
                SimpleDateFormat format = new SimpleDateFormat ("HH.mm.ss.SSS");
                Date d = new Date();
                List<String> processed_line = new ArrayList<String>();
                processed_line.add(format.format(d.getTime()));processed_line.add(Integer.toString(timestamp));processed_line.add(Long.toString(total_rr_count));
                processed_line.add(Integer.toString(rr_window_count));processed_line.add(Integer.toString(valid_rr_count));processed_line.add(Integer.toString(stress_index));
                processed_line.add(Integer.toString(pNN50));processed_line.add(Float.toString(rmssd));processed_line.add(Float.toString(score));
                processed_line.add(Float.toString(dfa_slope2));processed_line.add(Float.toString(SDNN));processed_line.add(Integer.toString(respiratory_rate));
                processed_line.add(Float.toString(vlf));processed_line.add(Float.toString(lf));processed_line.add(Float.toString(hf));processed_line.add(Float.toString(lf_nu));
                processed_line.add(Float.toString(hf_nu));processed_line.add(Float.toString(lf_to_hf));processed_line.add(Float.toString(tp));processed_line.add(Float.toString(conf_lvl));
                processed_line.add(Integer.toString(percentage_stress));
                String[] final_line = new String[processed_line.size()];
                processed_line.toArray(final_line);
                csv_writer(final_line, hrv_csv);
            }

        }

    }


    public void csv_writer(String[] line, String csv){
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(csv, true));
            writer.writeNext(line, false);
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void characteristicsOperations(List<BluetoothGattCharacteristic> characteristics){
        if(characteristics.isEmpty()){
            Log.i("printGattTable", "No service and characteristic available, call discoverServices() first?");
            return;
        }
        Iterator<BluetoothGattCharacteristic> it = characteristics.iterator();
        while(it.hasNext()) {
            BluetoothGattCharacteristic character = it.next();
//            if(character.getUuid().equals(DEVICE_NAME)){
//                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        gatt.readCharacteristic(character);
//                        Log.w("characteristicsOps","Read characteristics");
//                    }
//                }, 2000);
//            }
            if(character.getUuid().equals(HEART_RATE_MEASURE)){
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setNotifications(character, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, true);
                    }
                }, 2000);

            }
            if(character.getUuid().equals(TEMPERATURE_MEASURE)){
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setNotifications(character, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, true);
                    }
                }, 500);
            }
        }

    }


    public void setNotifications(BluetoothGattCharacteristic characteristic, byte[] payload, boolean enable){
        String CCC_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb";
        UUID cccdUuid = UUID.fromString(CCC_DESCRIPTOR_UUID);

        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(cccdUuid);
        if(descriptor == null){
            Log.e("setNotification", "Could not get CCC descriptor for characteristic "+ characteristic.getUuid());
        }
        if(!gatt.setCharacteristicNotification(descriptor.getCharacteristic(), enable)){
            Log.e("setNotification", "setCharacteristicNotification failed");
        }
        descriptor.setValue(payload);
        boolean result = gatt.writeDescriptor(descriptor);
        if(!result){
            Log.e("setNotification", "writeDescriptor failed for descriptor");

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),"Descriptor failed! Device may not be connected. Try again!",Toast.LENGTH_LONG).show();
                }
            });
            gatt.disconnect();
            finish();
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


//    /** static constructor */
//    static {
//        System.loadLibrary("AndroidHRVStressAPI");
//        System.loadLibrary("AndroidHRVAPI");
//        System.loadLibrary("AndroidRespirationAPI");
//    }

}