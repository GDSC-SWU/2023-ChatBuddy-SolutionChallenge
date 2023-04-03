package com.example.chatbuddy;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.snu.ids.kkma.index.Keyword;
import org.snu.ids.kkma.index.KeywordExtractor;
import org.snu.ids.kkma.index.KeywordList;
import org.snu.ids.kkma.ma.MExpression;
import org.snu.ids.kkma.ma.MorphemeAnalyzer;
import org.snu.ids.kkma.ma.Sentence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SolutionActivity extends AppCompatActivity {

    // firebase authentication
    GoogleSignInClient mGoogleSignInClient;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;

    // realtime database
    FirebaseDatabase database;
    DatabaseReference userRef, uidRef, chatRef;

    static ImageView CloudView;
    static TextView tvWelcome;

    static ViewGroup layout;
    StringBuilder builder;
    String words[];
    ArrayList<String> wordsList;
    Map<String, Integer> freq;
    List<Map.Entry<String, Integer>> entryList;
    int cnt;

    ImageButton btnMenu;

    // 드로어
    TextView tvDrawerName;
    RelativeLayout solLayout;
    LinearLayout btnFirst, btnSetting, btnThird;

    ScrollView sv_Solution;
    ConstraintLayout solActivity, cl_MindMap;
    LinearLayout ll_Todo, todo1, todo2, todo3;

    ImageButton btnChat;
    ImageView rd1, rd2, rd3;
    private static final int REQUEST_CODE = 1;
    private Boolean res;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solution);


        // Firebase 인증 객체 초기화
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Write a message to the database
        database = FirebaseDatabase.getInstance();
        userRef = database.getReference("users");
        uidRef = userRef.child(currentUser.getUid());
        chatRef = uidRef.child("chat");

        tvWelcome = findViewById(R.id.welcome_text);
        layout = findViewById(R.id.MindMapLayout);
        //CloudView = findViewById(R.id.cloudView);
        builder = new StringBuilder();
        ArrayList<String> wordsList = new ArrayList<>();


        sv_Solution = findViewById(R.id.scrollSolution);
        cl_MindMap = findViewById(R.id.MindMapLayout);
        ll_Todo = findViewById(R.id.todoLayout);

        chatRef.child("user").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // uid 노드의 모든 데이터를 가져옴
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String key = snapshot.getKey(); // 하위 노드의 키값을 가져옴
                    String value = snapshot.getValue(String.class); // 하위 노드의 값을 가져옴

                    //builder.append(value.toString());
                    //builder.append(" ");

                    words = value.split(" ");
                    System.out.println("words: " + words);
                    wordsList.addAll(Arrays.asList(words));
                    System.out.println("wordsList: " + wordsList);
                }

                if (wordsList.size() < 10) {
                    for (int i = 0; i < layout.getChildCount(); i++) {
                        Button child = (Button) layout.getChildAt(i);

                        tvWelcome.setVisibility(VISIBLE);
                        tvWelcome.setText("대화 정보가 없습니다\n츄디와 대화를 시작하세요");
                        child.setText("");
                    }
                } else {
                    tvWelcome.setVisibility(VISIBLE);
                    tvWelcome.setText("당신의 응어리를 보듬어주세요");
                    layout.setVisibility(VISIBLE);
                    freq = new HashMap<>();
                    // 배열을 반복하면서 각 원소의 등장 횟수를 계산합니다.
                    for (String element : wordsList) {
                        if (freq.containsKey(element)) {
                            freq.put(element, freq.get(element) + 1);
                        } else {
                            freq.put(element, 1);
                        }
                    }

                    System.out.println("freq: " + freq);

                    //extractAnal(builder.toString());
                    //draw(freq);
                    Map<String, Integer> sortedMap = sortByValue(freq);
                    System.out.println("sortedMap: " + sortedMap);

                    changeText(sortedMap);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                // 데이터 로드에 실패한 경우 호출됨
                Log.w("TAG", "loadPost:onCancelled", databaseError.toException());
            }
        });

        /*cnt = 0;
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            final Animation pop = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.anim_pop);
            child.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.startAnimation(pop);
                    child.setEnabled(false);
                    cnt++;

                    if (cnt == 10) {
                        cl_MindMap.setVisibility(GONE);
                    }
                }
            });
        }*/


        ll_Todo.setVisibility(INVISIBLE);

        final Animation animFadeOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.anim_fade_out);
        final Animation animFadeIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.anim_fade_in);

        sv_Solution.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                float alpha1 = (float) (sv_Solution.getHeight() - scrollY) / (float) sv_Solution.getHeight();
                float alpha2 = 1 - alpha1;

                // 스크롤 위치가 상단인 경우
                if (scrollY <= 0) {
                    cl_MindMap.setVisibility(VISIBLE);
                    ll_Todo.setVisibility(INVISIBLE);
                }
                // 스크롤 위치가 하단인 경우
                else if (scrollY >= (sv_Solution.getChildAt(0).getHeight() - sv_Solution.getHeight())) {
                    cl_MindMap.setVisibility(INVISIBLE);
                    ll_Todo.setVisibility(VISIBLE);
                }
                // 그 외의 경우
                else {

                    if (scrollY > oldScrollY) { // 스크롤을 내릴 때
                        ll_Todo.setVisibility(VISIBLE);
                        cl_MindMap.animate().alpha(0f).setDuration(500).start(); // layout1 서서히 안보이게 하기
                        ll_Todo.animate().alpha(1f).setDuration(500).start(); // layout2 서서히 나타내기
                    } else if (scrollY < oldScrollY) { // 스크롤을 올릴 때
                        cl_MindMap.setVisibility(VISIBLE);
                        ll_Todo.animate().alpha(0f).setDuration(500).start(); // layout2 서서히 안보이게 하기
                        cl_MindMap.animate().alpha(1f).setDuration(500).start(); // layout1 서서히 나타내기
                    }

                }

            }
        });

        todo1 = findViewById(R.id.todo1);
        todo2 = findViewById(R.id.todo2);
        todo3 = findViewById(R.id.todo3);

        todo2.setEnabled(false);
        todo3.setEnabled(false);

        rd1 = findViewById(R.id.chkRadio1);
        rd2 = findViewById(R.id.chkRadio2);
        rd3 = findViewById(R.id.chkRadio3);

        todo1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), TodoActivity.class);
                startActivityForResult(intent, REQUEST_CODE);
            }
        });

        todo2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "사진 찍기",Toast.LENGTH_LONG).show();
            }
        });


        // 드로어
        solActivity = findViewById(R.id.SolutionActivity);
        solLayout = solActivity.findViewById(R.id.SolutionLayout);
        btnMenu = findViewById(R.id.btnMenu2);
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout ll = (LinearLayout)inflater.inflate(R.layout.drawer, null);

        AtomicInteger state = new AtomicInteger(); // 메뉴 상태 - 0이면 닫힌 상태, 1이면 열린 상태
        final Animation animOpen = AnimationUtils.loadAnimation(this, R.anim.anim_translate_left);
        final Animation animClose = AnimationUtils.loadAnimation(this, R.anim.anim_translate_right);

        // 드로어 밖
        sv_Solution.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (state.get() == 1) {
                    ll.startAnimation(animClose);
                    ((ViewManager) ll.getParent()).removeView(ll);
                    state.set(0);
                }
            }
        });

        // 드로어
        btnMenu.setOnClickListener(view -> {
            if(state.get() == 0) {
                LinearLayout.LayoutParams paramll = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                addContentView(ll, paramll);
                ll.startAnimation(animOpen);
                //ll.setBackgroundColor(Color.parseColor("#70000000"));
                state.set(1);

                tvDrawerName = findViewById(R.id.drawerName);

                // 이름
                if (currentUser != null) {
                    uidRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            // uid 노드의 모든 데이터를 가져옴
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                String key = snapshot.getKey(); // 하위 노드의 키값을 가져옴
                                Object value = snapshot.getValue(); // 하위 노드의 값을 가져옴
                                // 이곳에서 하위 노드의 키와 값을 사용하여 원하는 작업을 수행할 수 있음
                                switch (key) {
                                    case "nickName":
                                        tvDrawerName.setText(value.toString() + " 님");
                                        break;
                                }
                            }
                        }
                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            // 데이터 로드에 실패한 경우 호출됨
                            Log.w("TAG", "loadPost:onCancelled", databaseError.toException());
                        }
                    });
                }

                btnFirst = findViewById(R.id.first);
                btnFirst.setOnClickListener(firstview ->{
                    Toast.makeText(getApplicationContext(), "Selected First", Toast.LENGTH_LONG).show();
                });

                btnSetting = findViewById(R.id.second);
                btnSetting.setOnClickListener(firstview ->{
                    Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                    startActivity(intent);
                });

                btnThird = findViewById(R.id.third);
                btnThird.setOnClickListener(firstview ->{
                    Toast.makeText(getApplicationContext(), "Selected Third", Toast.LENGTH_LONG).show();
                });

            } else {
                ll.startAnimation(animClose);
                ((ViewManager)ll.getParent()).removeView(ll);
                state.set(0);
            }
        });

        btnChat = findViewById(R.id.btnChat);
        btnChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMainActivity();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // requestCode가 REQUEST_CODE_SECOND_ACTIVITY이고 resultCode가 RESULT_OK일 경우에만 실행합니다.
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            // SecondActivity에서 전달받은 결과 값을 변수에 저장합니다.
            res = data.getBooleanExtra("result_key", false);

            if (res == true) {
                todo1.setEnabled(false);
                todo2.setEnabled(true);
                rd1.setImageResource(R.drawable.icon_solution_done);
                rd2.setImageResource(R.drawable.icon_solution_lock1);
            }
        }
    }


    void startMainActivity() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    public static void changeText(Map<String, Integer> sortedMap) {
        List<String> topKeys = new ArrayList<>(); // 상위 7개의 key를 담을 List
        int count = 0;
        for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
            if (count >= 10) {
                break;
            }
            topKeys.add(entry.getKey()); // 상위 key를 List에 추가합니다.
            count++;
        }

        for (int i = 0; i < layout.getChildCount(); i++) {
            Button child = (Button) layout.getChildAt(i);

            child.setText(topKeys.get(i));
        }
    }

    public static void formAnal()
    {
        String string = "형태소 분석기.";
        try {
            MorphemeAnalyzer ma = new MorphemeAnalyzer();
            ma.createLogger(null);
            List<MExpression> ret = ma.analyze(string);
            ret = ma.postProcess(ret);
            ret = ma.leaveJustBest(ret);
            List<Sentence> stl = ma.divideToSentences(ret);
            for (int i = 0; i < stl.size(); i++) {
                Sentence st = stl.get(i);
                System.out.println("=============================================  " + st.getSentence());
                for (int j = 0; j < st.size(); j++) {
                    System.out.println(st.get(j));
                }
            }
            ma.closeLogger();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void extractAnal(String sentence){
        String string = sentence;
        KeywordExtractor ke = new KeywordExtractor();
        KeywordList kl = ke.extractKeyword(string, true);
        Map<String, Integer> aFreq = new HashMap<>();

        for( int i = 0; i < kl.size(); i++ ){
            Keyword kwrd = kl.get(i);
            System.out.println(kwrd.getString() + "\t" + kwrd.getCnt());

            System.out.println(kwrd.getCnt());
            aFreq.put(kwrd.getString(), kwrd.getCnt());
        }

        Map<String, Integer> sortedMap = sortByValue(aFreq);

        changeText(sortedMap);
        //draw(sortedMap);
    }

    // Map을 Value 값으로 정렬
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, (o1, o2) -> (o2.getValue()).compareTo(o1.getValue()));

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    static void draw(Map map) {
        WordCloud wd = new WordCloud(map, 250, 250, Color.rgb(1, 171, 180), Color.TRANSPARENT);
        wd.setWordColorOpacityAuto(true);
        wd.setPaddingX(5);
        wd.setPaddingY(5);

        Bitmap generatedWordCloudBmp = wd.generate();
        CloudView.setImageBitmap(generatedWordCloudBmp);
    }
}