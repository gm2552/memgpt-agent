package com.example.memgptagent.service;

public interface MutableAgent extends Agent {

    void refreshState();

    void setFinalUserMessage(String string);
}
