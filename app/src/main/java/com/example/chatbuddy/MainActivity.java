package com.example.chatbuddy;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity {

    ImageButton btnMenu, btnSol;
    // 드로어
    TextView tvDrawerName;
    LinearLayout mainActivity;
    LinearLayout btnFirst, btnSetting, btnThird;

    // firebase authentication
    GoogleSignInClient mGoogleSignInClient;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;

    // realtime database
    FirebaseDatabase database;
    DatabaseReference userRef, uidRef;

    // chat
    RecyclerView recyclerView;
    EditText messageEditText;
    ImageButton sendButton, btnBack;
    List<Message> messageList;
    MessageAdapter messageAdapter;
    String openai_api_key;
    public final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client;
    String userName;
    JSONArray messagesArray;
    JSONObject messageObject;
    int failed, selfHarm;
    String res_embedding;

    LinearLayout ll_bot, ll_user;
    Button btn1, btn2, btn3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Firebase 인증 객체 초기화
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Write a message to the database
        database = FirebaseDatabase.getInstance();
        userRef = database.getReference("users");
        if (currentUser != null) {
            String uid = currentUser.getUid();
            uidRef = userRef.child(uid);
        }


        // 드로어
        mainActivity = findViewById(R.id.MainActivity);
        btnMenu = findViewById(R.id.btnMenu1);
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout ll = (LinearLayout)inflater.inflate(R.layout.drawer, null);

        AtomicInteger state = new AtomicInteger(); // 메뉴 상태 - 0이면 닫힌 상태, 1이면 열린 상태
        final Animation animOpen = AnimationUtils.loadAnimation(this, R.anim.anim_translate_left);
        final Animation animClose = AnimationUtils.loadAnimation(this, R.anim.anim_translate_right);

        btnSol = findViewById(R.id.btnSolution);


        // 드로어 밖
        mainActivity.setOnClickListener(new View.OnClickListener() {
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

        View rootView = findViewById(android.R.id.content).getRootView();
        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: 이벤트 처리
                ll.startAnimation(animClose);
                ((ViewManager)ll.getParent()).removeView(ll);
                state.set(0);
            }
        });

        btnSol.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSolutionActivity();
            }
        });


        client = new OkHttpClient();
        messageList = new ArrayList<>();
        messagesArray = new JSONArray();
        selfHarm = 0;

        // chat
        recyclerView = findViewById(R.id.recycler_view);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_btn);
        // setup recycler view
        messageAdapter = new MessageAdapter(messageList);
        sendButton.setEnabled(false);

        messageEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    // EditText가 선택될 때
                    btnSol.setVisibility(View.GONE);
                    sendButton.setEnabled(true);
                    sendButton.setVisibility(View.VISIBLE);
                } else {
                    // EditText가 선택 해제될 때
                    btnSol.setVisibility(View.VISIBLE);
                    sendButton.setEnabled(false);
                    sendButton.setVisibility(View.INVISIBLE);
                }
            }
        });

        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);

        failed = 0;
        sendButton.setOnClickListener((v)->{
            String question = messageEditText.getText().toString().trim();

            if (question.length() == 0) {
                // do nothing
            } else {
                addToChat(question, Message.SENT_BY_ME);
                messageEditText.setText("");
                blockEdit(false);
                try {
                    callChatAPI(question);
                    callModAPI(question);
                    //embeddings();
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });


        // api key
        database.getReference("api_key").child("open_ai").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                }
                else {
                    Log.d("firebase", String.valueOf(task.getResult().getValue()));
                    openai_api_key = String.valueOf(task.getResult().getValue());
                }
            }
        });

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
                            userName = value.toString();
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

    // 키보드 이외 영역 터치했을 때 키보드 끄기
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        View v = getCurrentFocus();
        boolean ret = super.dispatchTouchEvent(event);
        if (v instanceof EditText) {
            View w = getCurrentFocus();
            int scrCoords[] = new int[2];
            w.getLocationOnScreen(scrCoords);
            float x = event.getRawX() + w.getLeft() - scrCoords[0];
            float y = event.getRawY() + w.getTop() - scrCoords[1];

            if (event.getAction() == MotionEvent.ACTION_UP && (x < w.getLeft() || x >= w.getRight() || y < w.getTop() || y > w.getBottom())) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getWindow().getCurrentFocus().getWindowToken(), 0);
                messageEditText.clearFocus();
                sendButton.setEnabled(false);
                sendButton.setVisibility(View.INVISIBLE);
            }
        }
        return ret;
    }

    void startSolutionActivity() {
        Intent intent = new Intent(getApplicationContext(), SolutionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }


    // 사용자 입력 제어
    void blockEdit(boolean bool) {
        messageEditText.setEnabled(bool);
        messageEditText.setFocusable(bool);
        messageEditText.setFocusableInTouchMode(bool);
        sendButton.setEnabled(bool);
        sendButton.setClickable(bool);
    }

    void addToChat(String message, String sentBy) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageList.add(new Message(message, sentBy));
                messageAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
                blockEdit(true);
            }
        });
    }

    void addResponse(String response) {
        messageList.remove(messageList.size() - 1);
        addToChat(response, Message.SENT_BY_BOT);
    }

    void embeddings() {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "text-embedding-ada-002");
            jsonBody.put("input", messagesArray.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/embeddings")
                .header("Authorization", "Bearer " + openai_api_key)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                System.out.println("Failed to load response due to " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("data");
                        res_embedding = jsonArray.getJSONObject(0).getString("embedding");

                        System.out.println("embeddings: " + res_embedding);
                        //callChatAPI(res_embedding);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Failed to load response due to " + response.body().string());
                    //messagesArray.remove(messagesArray.length() - 1);
                }
            }
        });
    }

    void callChatAPI(String question) throws JSONException {
        // 시스템 메세지
        removeSys();
        messageObject = new JSONObject();
        messageObject.put("role", "system");
        messageObject.put("content", "You are a chatbot named Chuddy. " +
                "Speak as if you are a real human. Do not mention you are a chatbot or AI." +
                "Chuddy is a best friend with " + userName + " who is talking with you. " +
                "Chuddy speaks friendly and informally. Speak the same language as " + userName + ".");
        messagesArray.put(messageObject);

        //okhttp
        messageList.add(new Message("입력 중... ", Message.SENT_BY_BOT));

        // 유저 메세지
        messageObject = new JSONObject();
        messageObject.put("role", "user");
        messageObject.put("content", question);
        messagesArray.put(messageObject);

        // db에 대화 내용 저장
        userRef.child(currentUser.getUid()).child("chat").child("user").child(String.valueOf(System.currentTimeMillis())).setValue(question);

        // reqest 메세지
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "gpt-3.5-turbo");
            jsonBody.put("messages", messagesArray);
            jsonBody.put("max_tokens", 3500);
            jsonBody.put("temperature", 0.8);
            jsonBody.put("frequency_penalty", 1);
            jsonBody.put("presence_penalty", 1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + openai_api_key)
                .post(body)
                .build();

        // request 요청
        client.newCall(request).enqueue(new Callback() {
            // timeout 등 오류
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                failed++;

                System.out.println("(1) Failed to load response due to " + e.getMessage());
                // 이전 question 제거
                messageList.remove(messageList.size() - 1);
                messagesArray.remove(messagesArray.length() - 1);

                // 여러번 실패하면 이전 내용 제거
                if (failed > 3 && messagesArray.length() > 4) {
                    messagesArray.remove(0);
                }

                try {
                    // question 재전송
                    callChatAPI(question);
                } catch (JSONException ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                JSONObject jsonObject = null;

                if (response.isSuccessful()) {
                    try {
                        jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        String result = jsonArray.getJSONObject(0).getJSONObject("message").getString("content");
                        addResponse(result.trim());

                        JSONObject resMessageObject = new JSONObject();
                        resMessageObject.put("role", "assistant");
                        resMessageObject.put("content", result.trim());
                        userRef.child(currentUser.getUid()).child("chat").child("assistant").child(String.valueOf(System.currentTimeMillis())).setValue(result.trim());
                        messagesArray.put(resMessageObject);

                        // 결과 확인
                        String jsonString = messagesArray.toString();
                        int jsonLength = jsonString.length();
                        System.out.println(jsonLength + ": " + jsonString);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    failed++;

                    System.out.println("(2) Failed to load response due to " + response.body().string());
                    // 이전 메세지 제거
                    messageList.remove(messageList.size() - 1);
                    messagesArray.remove(messagesArray.length() - 1);

                    if (failed > 3 && messagesArray.length() > 4) {
                        messagesArray.remove(1);
                    }

                    try {
                        callChatAPI(question);
                    } catch (JSONException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });
    }

    public void removeSys() {
        try {
            for (int i = 0; i < messagesArray.length(); i++) {
                JSONObject jsonObject = messagesArray.getJSONObject(i);

                if (jsonObject.getString("role") == "system") {
                    messagesArray.remove(i);
                }
            }
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    void callModAPI(String question) throws JSONException {

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("input", question);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/moderations")
                .header("Authorization", "Bearer " + openai_api_key)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                System.out.println("Failed to load response due to " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("results");

                        //String result = jsonArray.getJSONObject(0).getJSONObject("category_scores").getString("self-harm");
                        //double score = Double.parseDouble(result.trim());
                        //System.out.println("score: " + score);

                        String result = jsonArray.getJSONObject(0).getJSONObject("categories").getString("self-harm");
                        System.out.println("self-harm t/f: " + result.trim());

                        if (result == "true") {
                            selfHarm++;
                        }

                        System.out.println(selfHarm);

                        if (selfHarm > 3) {
                            String str = "지금 서울시에서는 서울시 청년 '마음건강사업'을 제공하고 있어." +
                                    "서울시에 거주하고 있는 만19세~39세 청년의 마음건강을 지원해. 최대 10회기*(1회기 50분) 심리상담 전문가와 함께 마음을 돌아볼 수 있어." +
                                    "더 알아볼래?";
                            messageObject = new JSONObject();
                            messageObject.put("role", "assistant");
                            messageObject.put("content", str);
                            messagesArray.put(messageObject);

                            btn1.setVisibility(View.VISIBLE);

                            addToChat(str, Message.SENT_BY_BOT);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Failed to load response due to " + response.body().string());
                    messagesArray.remove(messagesArray.length() - 1);
                }
            }
        });
    }

}