package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    Button openMain, openUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        openMain = (Button) findViewById(R.id.gotoMain);
        openUser = (Button) findViewById(R.id.author);

        openMain.setOnClickListener(this);
        openUser.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.gotoMain:
                Intent gotoMain = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(gotoMain);
                break;
            case R.id.author:
                Intent gotoAuthor = new Intent(LoginActivity.this, AuthorActivity.class);
                startActivity(gotoAuthor);
                break;
        }
    }
}