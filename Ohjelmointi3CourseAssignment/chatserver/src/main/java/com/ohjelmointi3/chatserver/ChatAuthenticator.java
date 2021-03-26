package com.ohjelmointi3.chatserver;


import com.sun.net.httpserver.BasicAuthenticator;

public class ChatAuthenticator extends BasicAuthenticator {
    
    ChatDatabase database = ChatDatabase.getInstance();


    public ChatAuthenticator() {
        super("chat");
    }

    @Override
    public boolean checkCredentials(String username, String passwd) {
        return database.checkCredentialsDatabase(username, passwd);
    }

    
    public boolean addUser(User userCredentials) {
        if (database.addUserIntoDatabase(userCredentials)) {
            return true;
        }
        return false;
    }

}
