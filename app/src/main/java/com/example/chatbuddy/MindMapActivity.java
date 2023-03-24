package com.example.chatbuddy;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.auth.User;

import org.snu.ids.kkma.index.Keyword;
import org.snu.ids.kkma.index.KeywordExtractor;
import org.snu.ids.kkma.index.KeywordList;
import org.snu.ids.kkma.ma.MExpression;
import org.snu.ids.kkma.ma.MorphemeAnalyzer;
import org.snu.ids.kkma.ma.Sentence;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MindMapActivity extends AppCompatActivity {
    // firebase authentication
    GoogleSignInClient mGoogleSignInClient;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;

    // realtime database
    FirebaseDatabase database;
    DatabaseReference userRef, uidRef, chatRef;
    User user;

    static ImageView CloudView;

    StringBuilder builder;
    Map<String, Integer> freq;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mind_map);

        //CloudView = findViewById(R.id.cloudView);

        // Firebase 인증 객체 초기화
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Write a message to the database
        database = FirebaseDatabase.getInstance();
        userRef = database.getReference("users");
        uidRef = userRef.child(currentUser.getUid());
        chatRef = uidRef.child("chat");
        builder = new StringBuilder();

        chatRef.child("user").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // uid 노드의 모든 데이터를 가져옴
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String key = snapshot.getKey(); // 하위 노드의 키값을 가져옴
                    Object value = snapshot.getValue(); // 하위 노드의 값을 가져옴

                    builder.append(value.toString());
                    builder.append(" ");
                }

                String[] words = builder.toString().split(" ");
                freq = new HashMap<>();
                // 배열을 반복하면서 각 원소의 등장 횟수를 계산합니다.
                for (String element : words) {
                    if (freq.containsKey(element)) {
                        freq.put(element, freq.get(element) + 1);
                    } else {
                        freq.put(element, 1);
                    }
                }

                draw(freq);
                //extractAnal(builder.toString());
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                // 데이터 로드에 실패한 경우 호출됨
                Log.w("TAG", "loadPost:onCancelled", databaseError.toException());
            }
        });

        Animation pop = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.anim_pop);

        ViewGroup layout = findViewById(R.id.MindMapActivity);
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            child.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.startAnimation(pop);
                    child.setVisibility(View.GONE);
                }
            });
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
        Map<String, Integer> freq = new HashMap<>();

        for( int i = 0; i < kl.size(); i++ ){
            Keyword kwrd = kl.get(i);
            System.out.println(kwrd.getString() + "\t" + kwrd.getCnt());

            System.out.println(kwrd.getCnt());
            freq.put(kwrd.getString(), kwrd.getCnt());
        }

        Map<String, Integer> sortedMap = sortByValue(freq);

        draw(sortedMap);
    }

    // Map을 Value 값으로 정렬
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, (o1, o2) -> (o1.getValue()).compareTo(o2.getValue()));

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

        //CloudView.setImageBitmap(generatedWordCloudBmp);
    }


}