package com.example.chatbuddy;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.auth.User;

public class SignInActivity extends AppCompatActivity {

    LinearLayout btn_SignIn;
    Button btnHelp, btnX;

    // firebase authentication
    private static final int RC_SIGN_IN = 123;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    FirebaseUser currentUser;

    // realtime database
    FirebaseDatabase database;
    DatabaseReference userRef, uidRef;
    User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Firebase 앱 초기화
        FirebaseApp.initializeApp(getApplicationContext());
        // Firebase 인증 객체 초기화
        mAuth = FirebaseAuth.getInstance();
        // Google 로그인 클라이언트 초기화
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        currentUser = mAuth.getCurrentUser();

        // Write a message to the database
        database = FirebaseDatabase.getInstance();
        userRef = database.getReference("users");

        setContentView(R.layout.activity_sign_in);
        btn_SignIn = findViewById(R.id.btnSignIn);
        btn_SignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btn_SignIn.setEnabled(false);

                if (currentUser == null) {
                    // 로그아웃 상태 & 탈퇴 상태
                    signIn();
                } else {
                    // 유효한 계정입니다.
                    // 이메일을 사용하여 Firebase에서 해당 계정이 존재하는지 확인합니다.
                    String email = currentUser.getEmail();
                    String uid = currentUser.getUid();

                    mAuth.fetchSignInMethodsForEmail(email)
                            .addOnCompleteListener(task -> {
                                boolean isNewUser = task.getResult().getSignInMethods().isEmpty();
                                if (isNewUser) {
                                    // 존재하지 않는 계정입니다.
                                    Toast.makeText(getApplicationContext(), "회원가입이 필요합니다.", Toast.LENGTH_SHORT).show();
                                    startSignUpActivity();
                                    btn_SignIn.setEnabled(true);
                                } else {
                                    // 존재하는 계정입니다.
                                    userRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            if (dataSnapshot.exists()) {
                                                // uid가 존재하는 경우
                                                signIn();
                                                btn_SignIn.setEnabled(true);
                                            } else {
                                                // uid가 존재하지 않는 경우
                                                Toast.makeText(getApplicationContext(), "회원가입이 필요합니다.", Toast.LENGTH_SHORT).show();
                                                startSignUpActivity();
                                                btn_SignIn.setEnabled(true);
                                            }
                                        }
                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {
                                            // 쿼리가 취소된 경우
                                            btn_SignIn.setEnabled(true);
                                        }
                                    });
                                }
                            });
                }
            }
        });

        btnHelp = findViewById(R.id.btnHelp);
        btnHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BottomSheetDialog bottomSheetDialog = BottomSheetDialog.getInstance();
                bottomSheetDialog.show(getSupportFragmentManager(),"bottomSheet");
            }
        });
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    // onActivityResult() 메서드에서 로그인 결과 처리
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 구글 로그인 시
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // 로그인 성공 시
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // 로그인 실패 시
                Log.w("TAG", "Google sign in failed", e);
                Toast.makeText(getApplicationContext(), "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Firebase 인증을 위한 GoogleSignInAccount를 사용하여 로그인하는 메서드
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        String uid = acct.getId();
        Log.d("TAG", "firebaseAuthWithGoogle:" + uid);

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // 로그인 성공 시
                            Log.d("TAG", "signInWithCredential:success");
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();

                            String uid = firebaseUser.getUid();
                            uidRef = userRef.child(firebaseUser.getUid());
                            userRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        // uid가 존재하는 경우
                                        if (firebaseUser != null) {
                                            uidRef.child("password").child("on").addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    if (snapshot.exists()) {
                                                        Boolean on = snapshot.getValue(Boolean.class);
                                                        if (on.equals(true)) {
                                                            startPasswordActivity();
                                                        } else if (on.equals(false)) {
                                                            startMainActivity();
                                                            Toast.makeText(getApplicationContext(), "로그인 성공", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                }
                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {
                                                    // 데이터베이스 액세스 중 오류 발생 시 처리 방법

                                                }
                                            });
                                        }
                                    } else {
                                        // uid가 존재하지 않는 경우
                                        Toast.makeText(getApplicationContext(), "회원가입이 필요합니다.", Toast.LENGTH_SHORT).show();
                                        startSignUpActivity();
                                    }
                                }
                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    // 쿼리가 취소된 경우
                                    Toast.makeText(getApplicationContext(), "오류 발생", Toast.LENGTH_SHORT).show();
                                }
                            });



                        } else {
                            // 로그인 실패 시
                            Log.w("TAG", "signInWithCredential:failure", task.getException());
                            Toast.makeText(getApplicationContext(), "로그인 실패", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void startMainActivity() {
        finishAffinity();
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    private void startSignUpActivity() {
        finishAffinity();
        Intent intent = new Intent(getApplicationContext(), SignUpActivity.class);
        startActivity(intent);
    }

    private void startPasswordActivity() {
        finishAffinity();
        Intent intent = new Intent(getApplicationContext(), PasswordActivity.class);
        intent.putExtra("preAct", "SignInActivity");
        setResult(Activity.RESULT_OK, intent);
        startActivity(intent);
    }


    /*
    // 원탭 로그인
    SignInClient oneTapClient;
    BeginSignInRequest signUpRequest;
    private static final int REQ_ONE_TAP = 2;  // Can be any integer unique to the Activity.
    private boolean showOneTapUI = true;
    SignInCredential credential;
    GoogleSignInOptions gso;
    AccountManager mAccountManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);

        oneTapClient = Identity.getSignInClient(this);
        // 로그인에 필요한 옵션들을 설정
        signUpRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        // Your server's client ID, not your Android client ID.
                        .setServerClientId(getString(R.string.web_client_id))
                        // Show all accounts on the device.
                        .setFilterByAuthorizedAccounts(false)
                        .build())
                .build();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
            }
        }
    }

    ActivityResultLauncher<IntentSenderRequest> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == Activity.RESULT_OK) {
                        // one-tap 로그인이 성공한 경우
                        try {
                            // 로그인 정보를 가져옵니다.
                            credential = oneTapClient.getSignInCredentialFromIntent(result.getData());
                            String idToken = credential.getGoogleIdToken();
                            if (idToken !=  null) {
                                // ID 토큰이 유효한 경우, 서버로 전달하여 인증 처리
                                // Got an ID token from Google. Use it to authenticate
                                // with your backend.
                                Log.d("TAG", "Got ID token.");
                                String email = credential.getId();
                                Toast.makeText(getApplicationContext(), "Your Email: " + email, Toast.LENGTH_SHORT).show();

                                startMainActivity();
                            }
                        } catch (ApiException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

    private void signIn() {
        oneTapClient.beginSignIn(signUpRequest)
                .addOnSuccessListener(FirstActivity.this, new OnSuccessListener<BeginSignInResult>() {
                    @Override
                    public void onSuccess(BeginSignInResult result) {
                        IntentSenderRequest intentSenderRequest =
                                new IntentSenderRequest.Builder(result.getPendingIntent().getIntentSender()).build();
                        activityResultLauncher.launch(intentSenderRequest);

                        Toast.makeText(getApplicationContext(), "로그인", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(FirstActivity.this, new OnFailureListener() {
                    // 로그인에 실패한 경우 처리
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // No Google Accounts found. Just continue presenting the signed-out UI.
                        Log.d("TAG", e.getLocalizedMessage());
                        Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
     */
}