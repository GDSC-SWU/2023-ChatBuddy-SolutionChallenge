package com.example.chatbuddy;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class TodoActivity extends AppCompatActivity {

    private static final long START_TIME_IN_MILLIS = 600000; // 카운트다운 시간 10분
    ImageButton btnBack, btnStart, btnSetting, btnMusic;
    TextView tvTime, tvComplete;

    private CountDownTimer mCountDownTimer;
    private boolean mTimerRunning;
    private long mTimeLeftInMillis = START_TIME_IN_MILLIS;
    public static Boolean is_success; // 다른 Activity에서 접근할 변수
    MediaPlayer mediaPlayer;
    Boolean is_playing;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        is_success = false;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo);


        btnBack = findViewById(R.id.btnBack);
        btnStart = findViewById(R.id.btnStart);
        btnSetting = findViewById(R.id.btnSetting);
        btnMusic = findViewById(R.id.btnMusic);
        tvComplete = findViewById(R.id.tvComplete);
        tvTime = findViewById(R.id.tvTime);

        mediaPlayer = MediaPlayer.create(this, R.raw.canon);
        mediaPlayer.setLooping(true);
        is_playing = false;

        btnMusic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (is_playing == false) {
                    mediaPlayer.start();
                    is_playing = true;
                } else {
                    mediaPlayer.pause();
                    is_playing = false;
                }
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.putExtra("result_key", is_success);
                setResult(RESULT_OK, intent);

                mediaPlayer.pause();
                mediaPlayer.release();
                finish();
            }
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
                is_success = true;
                is_playing = true;
                mediaPlayer.pause();
                mediaPlayer.release();

                if(tvTime != null){tvTime.setVisibility(View.INVISIBLE);}
                if(tvComplete != null){tvComplete.setVisibility(View.VISIBLE);}
                if(btnSetting != null){btnSetting.setVisibility(View.INVISIBLE);}
                if(btnMusic != null){btnMusic.setVisibility(View.INVISIBLE);}

                btnStart.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent();
                        intent.putExtra("result_key", is_success);
                        setResult(RESULT_OK, intent);

                        finish();
                    }
                });
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