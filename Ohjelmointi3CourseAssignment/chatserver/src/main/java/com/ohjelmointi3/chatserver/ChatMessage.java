package com.ohjelmointi3.chatserver;

import org.json.JSONObject;

public class ChatMessage {
    public String timeSent;
    public String username;
    public String mess;

    public ChatMessage(JSONObject receivedJson) {
        username = receivedJson.getString("user");
        mess = receivedJson.getString("message");
        timeSent = receivedJson.getString("sent");
    }

    
}
