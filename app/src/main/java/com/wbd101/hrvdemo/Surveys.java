package com.wbd101.hrvdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

public class Surveys extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_surveys);

        Intent intent = getIntent();
//        int age = intent.getIntExtra("age",0);
//        String id = intent.getStringExtra("student_id");
//
//        TextView student_id = (TextView)findViewById(R.id.student_id);
//        String display = "ID: "+ id;
//        student_id.setText(display);
//
//        TextView student_age = (TextView)findViewById(R.id.age);
//        display = "Age: "+ String.format(Locale.US,"%d", age);
//        student_age.setText(display);

        findViewById(R.id.start_1_button).setOnClickListener(this::onClick);
        findViewById(R.id.start_2_button).setOnClickListener(this::onClick);
        findViewById(R.id.start_3_button).setOnClickListener(this::onClick);
        findViewById(R.id.start_4_button).setOnClickListener(this::onClick);
        findViewById(R.id.start_5_button).setOnClickListener(this::onClick);
        findViewById(R.id.start_6_button).setOnClickListener(this::onClick);
        findViewById(R.id.start_7_button).setOnClickListener(this::onClick);
        findViewById(R.id.start_8_button).setOnClickListener(this::onClick);
    }

    @Override
    public void onClick(View v) {
        String url = "";
        switch(v.getId()) {
            case R.id.start_1_button:
                // Code for button 1 click
                url = "https://www.surveymonkey.com/r/KDYFBPW";
                break;

            case R.id.start_2_button:
                // Code for button 2 click
                url = "https://www.surveymonkey.com/r/KFG79JG";
                break;

            case R.id.start_3_button:
                // Code for button 3 click
                url = "https://www.surveymonkey.com/r/KF7DV9V";
                break;

            case R.id.start_4_button:
                // Code for button 3 click
                url = "https://www.surveymonkey.com/r/K3XWKLS";
                break;

            case R.id.start_5_button:
                // Code for button 3 click
                url = "https://www.surveymonkey.com/r/K38Z7ZJ";
                break;

            case R.id.start_6_button:
                // Code for button 3 click
                url = "https://www.surveymonkey.com/r/K39WT5K";
                break;

            case R.id.start_7_button:
                // Code for button 3 click
                url = "https://www.surveymonkey.com/r/K3QJ8QT";
                break;

            case R.id.start_8_button:
                // Code for button 3 click
                url = "https://www.surveymonkey.com/r/K33G6P3";
                break;
        }
        Intent web_view = new Intent(Surveys.this, SurveyWebView.class);
        web_view.putExtra("url", url);
        startActivity(web_view);
    }
}