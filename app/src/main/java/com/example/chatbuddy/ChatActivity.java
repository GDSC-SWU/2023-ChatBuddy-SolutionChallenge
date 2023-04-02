package com.example.chatbuddy;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

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
import com.google.firebase.firestore.auth.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    // firebase authentication
    GoogleSignInClient mGoogleSignInClient;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;

    // realtime database
    FirebaseDatabase database;
    DatabaseReference userRef, uidRef;
    User user;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Firebase 인증 객체 초기화
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Write a message to the database
        database = FirebaseDatabase.getInstance();
        userRef = database.getReference("users");
        uidRef = userRef.child(currentUser.getUid());


        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { finish();
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
        messageObject.put("content", "You are a chatbot named Chuddy. 한국어 이름은 츄디라고 해. " +
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

                        if (selfHarm > 2) {
                            messageObject = new JSONObject();
                            messageObject.put("role", "system");
                            messageObject.put("content", "Have a deep conversation with her before recommend a counseling center. " +
                                    "However, if she is about to self-harm, recommend a consultation with an expert. " +
                                    "Let her know that the suicide prevention consultation call is '1393'.");
                            messagesArray.put(messageObject);
                            selfHarm = 0;
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