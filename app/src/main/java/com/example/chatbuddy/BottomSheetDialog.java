package com.example.chatbuddy;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class BottomSheetDialog extends BottomSheetDialogFragment implements View.OnClickListener{

    public static BottomSheetDialog getInstance() { return new BottomSheetDialog(); }

    private Button btnEmail;
    ImageButton btnX;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_manual, container,false);


        btnEmail = (Button) view.findViewById(R.id.btnEmail);
        btnX = (ImageButton) view.findViewById(R.id.btnClose);

        btnEmail.setOnClickListener(this);
        btnX.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnEmail:
                Toast.makeText(getContext(),"이메일 전송하기", Toast.LENGTH_SHORT).show();
                dismiss();
            case R.id.btnClose:
                dismiss();
        }
    }
}
