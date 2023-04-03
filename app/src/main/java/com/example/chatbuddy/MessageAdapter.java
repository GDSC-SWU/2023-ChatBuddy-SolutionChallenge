package com.example.chatbuddy;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MyViewHolder> {

    List<Message> messageList;
    private Activity mActivity;

    public MessageAdapter(List<Message> messageList, Activity activity) {
        this.messageList = messageList;
        mActivity = activity;
    }


    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View chatView = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item,null);
        MyViewHolder myViewHolder = new MyViewHolder(chatView);

        return myViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Message message = messageList.get(position);
        if(message.getSentBy().equals(Message.SENT_BY_ME)){
            holder.leftChatView.setVisibility(View.GONE);
            holder.rightChatView.setVisibility(View.VISIBLE);
            holder.rightTextView.setText(message.getMessage());

            holder.leftAsk1View.setVisibility(View.GONE);
            holder.leftAsk2View.setVisibility(View.GONE);
        }
        else if (message.getSentBy().equals(Message.ASK1)) {
            holder.leftChatView.setVisibility(View.GONE);
            holder.rightChatView.setVisibility(View.GONE);
            holder.leftAsk1View.setVisibility(View.VISIBLE);
            holder.leftAsk2View.setVisibility(View.GONE);

            holder.ask1_btn1.setVisibility(View.VISIBLE);
            holder.ask1_btn1.setText("확인");
            holder.leftAsk1TextView.setText(message.getMessage());

            holder.ask1_btn1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.ask1_btn1.setBackgroundColor(Color.parseColor("#01ABB4"));
                    holder.ask1_btn1.setEnabled(false);


                    String str = "아래 버튼 중 궁금한 내용을 선택해줘.";
                    messageList.add(new Message(str, Message.ASK2));

                    notifyDataSetChanged();
                    holder.recyclerView.smoothScrollToPosition(messageList.size() - 1);
                }
            });

        } else if (message.getSentBy().equals(Message.ASK2)) {
            holder.leftChatView.setVisibility(View.GONE);
            holder.rightChatView.setVisibility(View.GONE);
            holder.leftAsk1View.setVisibility(View.GONE);
            holder.leftAsk2View.setVisibility(View.VISIBLE);


            holder.ask2_btn1.setVisibility(View.VISIBLE);
            holder.ask2_btn2.setVisibility(View.VISIBLE);
            holder.ask2_btn3.setVisibility(View.VISIBLE);

            holder.ask2_btn1.setText("신청 자격");
            holder.ask2_btn2.setText("신청 기간");
            holder.ask2_btn3.setText("상담 방법");

            holder.leftAsk2TextView.setText(message.getMessage());

            holder.ask2_btn1.setOnClickListener(new View.OnClickListener() {
                @SuppressLint("ResourceAsColor")
                @Override
                public void onClick(View v) {
                    String str = "[신청 자격에 대한 안내]";
                    messageList.add(new Message(str, Message.SENT_BY_BOT));

                    notifyDataSetChanged();
                    holder.recyclerView.smoothScrollToPosition(messageList.size() - 1);
                }
            });

            holder.ask2_btn2.setOnClickListener(new View.OnClickListener() {
                @SuppressLint("ResourceAsColor")
                @Override
                public void onClick(View v) {
                    String str = "2023년 3월(1차), 5월(2차), 7월(3차), 9월(4차)로 총 4회 신청할 수 있어.\n" +
                            "\n" +
                            "※ 구체적인 일정은 회차별 상담진행에 따라 달라질 수 있어.";

                    messageList.add(new Message(str, Message.ANSWER2));

                    notifyDataSetChanged();
                    holder.recyclerView.smoothScrollToPosition(messageList.size() - 1);
                }
            });

            holder.ask2_btn3.setOnClickListener(new View.OnClickListener() {
                @SuppressLint("ResourceAsColor")
                @Override
                public void onClick(View v) {
                    String str = "[상담 방법에 대한 안내]";
                    messageList.add(new Message(str, Message.SENT_BY_BOT));

                    notifyDataSetChanged();
                    holder.recyclerView.smoothScrollToPosition(messageList.size() - 1);
                }
            });
        }
        else if (message.getSentBy().equals(Message.ANSWER2)) {
            holder.leftChatView.setVisibility(View.GONE);
            holder.rightChatView.setVisibility(View.GONE);
            holder.leftAsk1View.setVisibility(View.VISIBLE);
            holder.leftAsk2View.setVisibility(View.GONE);

            holder.ask1_btn1.setVisibility(View.VISIBLE);
            holder.ask1_btn1.setText("신청하러 가기");
            holder.leftAsk1TextView.setText(message.getMessage());

            holder.ask1_btn1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(v.getContext(), "신청하러 가기", Toast.LENGTH_SHORT).show();
                }
            });

        }
        else if (message.getSentBy().equals(Message.SENT_BY_BOT)) {
            holder.rightChatView.setVisibility(View.GONE);
            holder.leftChatView.setVisibility(View.VISIBLE);
            holder.leftTextView.setText(message.getMessage());

            holder.leftAsk1View.setVisibility(View.GONE);
            holder.leftAsk2View.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{
        LinearLayout leftChatView,rightChatView, leftAsk1View, leftAsk2View;
        TextView leftTextView,rightTextView, leftAsk1TextView, leftAsk2TextView;
        Button ask1_btn1, ask2_btn1, ask2_btn2, ask2_btn3;
        RecyclerView recyclerView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            leftChatView  = itemView.findViewById(R.id.left_chat_view);
            rightChatView = itemView.findViewById(R.id.right_chat_view);
            leftTextView = itemView.findViewById(R.id.left_chat_text_view);
            rightTextView = itemView.findViewById(R.id.right_chat_text_view);

            leftAsk1View = itemView.findViewById(R.id.left_ask1_view);
            leftAsk1TextView = itemView.findViewById(R.id.left_ask1_text_view);
            ask1_btn1 = itemView.findViewById(R.id.left_ask1_btn1);

            leftAsk2View = itemView.findViewById(R.id.left_ask2_view);
            leftAsk2TextView = itemView.findViewById(R.id.left_ask2_text_view);
            ask2_btn1 = itemView.findViewById(R.id.left_ask2_btn1);
            ask2_btn2 = itemView.findViewById(R.id.left_ask2_btn2);
            ask2_btn3 = itemView.findViewById(R.id.left_ask2_btn3);

            recyclerView = mActivity.findViewById(R.id.recycler_view);
        }
    }
}