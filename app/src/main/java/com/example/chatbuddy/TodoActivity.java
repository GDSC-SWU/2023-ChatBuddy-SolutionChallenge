package com.example.chatbuddy;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class TodoActivity extends AppCompatActivity {

    private static final long START_TIME_IN_MILLIS = 10000; // 카운트다운 시간 10초
    ImageButton btnBack, btnStart, btnSetting, btnMusic;
    TextView tvTime, tvComplete;

    private CountDownTimer mCountDownTimer;
    private boolean mTimerRunning;
    private long mTimeLeftInMillis = START_TIME_IN_MILLIS;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo);

        btnBack = findViewById(R.id.btnBack);
        btnStart = findViewById(R.id.btnStart);
        btnSetting = findViewById(R.id.btnSetting);
        btnMusic = findViewById(R.id.btnMusic);
        tvComplete = findViewById(R.id.tvComplete);
        tvTime = findViewById(R.id.tvTime);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { finish(); }
        });

        // 시작 버튼 클릭 시
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mTimerRunning) {
                    pauseTimer();
                } else {
                    startTimer();
                }
            }
        });

        updateCountDownText();

    }

    private void startTimer() {
        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                mTimerRunning = false;
                btnStart.setImageResource(R.drawable.btn_solution_complete);
                if(tvTime != null){tvTime.setVisibility(View.INVISIBLE);}
                if(tvComplete != null){tvComplete.setVisibility(View.VISIBLE);}
                if(btnSetting != null){btnSetting.setVisibility(View.INVISIBLE);}
                if(btnMusic != null){btnMusic.setVisibility(View.INVISIBLE);}

            }
        }.start();

        mTimerRunning = true;
        btnStart.setImageResource(R.drawable.btn_solution_pause);
    }

    private void pauseTimer() {
        mCountDownTimer.cancel();
        mTimerRunning = false;
        btnStart.setImageResource(R.drawable.btn_solution_start);
    }

    private void updateCountDownText() {
        int minutes = (int) (mTimeLeftInMillis / 1000) / 60;
        int seconds = (int) (mTimeLeftInMillis / 1000) % 60;

        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

        tvTime.setText(timeLeftFormatted);
    }
}