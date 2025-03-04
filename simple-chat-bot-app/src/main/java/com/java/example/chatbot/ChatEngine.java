package com.java.example.chatbot;

public interface ChatEngine {

    void initialize(String userId);

    String chat(String message);

}
