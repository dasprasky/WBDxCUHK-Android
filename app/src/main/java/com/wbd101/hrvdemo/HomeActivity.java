package com.wbd101.hrvdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Intent intent = getIntent();
        int age = intent.getIntExtra("age",0);
        String id = intent.getStringExtra("student_id");

        TextView student_id = (TextView)findViewById(R.id.student_id);
        String display = "ID: "+ id;
        student_id.setText(display);

        TextView student_age = (TextView)findViewById(R.id.age);
        display = "Age: "+ String.format(Locale.US,"%d", age);
        student_age.setText(display);

        Button record_button = (Button)findViewById(R.id.record_button);
        record_button.setOnClickListener(v ->{
            Intent record_intent = new Intent(HomeActivity.this, ScanActivity.class);
            record_intent.putExtra("age", age);
            record_intent.putExtra("student_id", id);
            startActivity(record_intent);
        });

        Button survey1_button = (Button)findViewById(R.id.surveys_button);
        survey1_button.setOnClickListener(v ->{
            Intent survey_intent = new Intent(HomeActivity.this, Surveys.class);
            survey_intent.putExtra("age", age);
            survey_intent.putExtra("student_id", id);
            startActivity(survey_intent);
        });

    }
    @Override
    public void onBackPressed(){
        moveTaskToBack(true); //similar to pressing home button. so that user can't go back to MainActivity
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            SharedPreferences sp = getSharedPreferences("login",MODE_PRIVATE);
            sp.edit().clear().apply();
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}