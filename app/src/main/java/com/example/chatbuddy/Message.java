package com.example.chatbuddy;

public class Message {
    public static String SENT_BY_ME = "me";
    public static String SENT_BY_BOT = "bot";
    public static String ASK1 = "aks1";
    public static String ASK2 = "aks2";
    public static String ANSWER1 = "answer1";
    public static String ANSWER2 = "answer2";
    public static String ANSWER3 = "answer3";

    String message;
    String sentBy;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSentBy() {
        return sentBy;
    }

    public void setSentBy(String sentBy) {
        this.sentBy = sentBy;
    }

    public Message(String message, String sentBy) {
        this.message = message;
        this.sentBy = sentBy;
    }
}
