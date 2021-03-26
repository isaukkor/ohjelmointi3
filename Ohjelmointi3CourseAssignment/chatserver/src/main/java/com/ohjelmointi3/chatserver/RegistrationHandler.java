package com.ohjelmointi3.chatserver;

import java.io.IOException;

import com.sun.net.httpserver.*;
import java.io.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.json.JSONObject;


public class RegistrationHandler implements HttpHandler {
    
    ChatAuthenticator auth = null;
    //ChatAuthenticator auth;

    RegistrationHandler(ChatAuthenticator authenticator) {
        auth = authenticator;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {

            if (httpExchange.getRequestMethod().equalsIgnoreCase("POST")) {
                if (checkHeaders(httpExchange)) {
                    InputStream input = httpExchange.getRequestBody();
                    String text = new BufferedReader(new InputStreamReader(input,
                                                    StandardCharsets.UTF_8))
                                                    .lines()
                                                    .collect(Collectors.joining("\n"));
                    input.close();
                    
                    if (text.trim().length() > 0) {
                        try {
                            JSONObject registrationMsg = new JSONObject(text);
                            if (registrationMsg.has("username") && registrationMsg.has("password") && registrationMsg.has("email")) {
                                if (registrationMsg.getString("username").trim().length() > 0 && 
                                    registrationMsg.getString("password").trim().length() > 0 && 
                                    registrationMsg.getString("email").trim().length() > 0) {
                                    if (auth.addUser(new User(registrationMsg.getString("username"), 
                                                                registrationMsg.getString("password"), 
                                                                registrationMsg.getString("email")))) {
                                        httpExchange.sendResponseHeaders(200, -1);
                                        ChatServer.log("new user created: " + registrationMsg.getString("username"));
                                    }
                                    else {
                                        sendMessages(httpExchange, 400, "user already exists");
                                        ChatServer.log("user already exists/user creation failed");
                                    }
                                }
                                else {
                                    sendMessages(httpExchange, 400, "credentials empty");
                                    ChatServer.log("tried to feed space to credentials");
                                }
                            }
                            else {
                                sendMessages(httpExchange, 400, "not all credentials found");
                                ChatServer.log("not all credentials found");
                            }
                        }
                        catch (Exception e) {
                            ChatServer.log("** ERROR RegistrationHandler error: " + e);
                            sendMessages(httpExchange, 400, "wrong json");
                        }
                    }
                    else {
                        sendMessages(httpExchange, 411, "No content in request");
                        ChatServer.log("No content in request");
                    }
                }
            }
            else {
                sendMessages(httpExchange, 411, "only post supported");
                ChatServer.log("only post supported");
            }

    }

    private void sendMessages(HttpExchange httpExchange, int statusCode, String messageBody) throws IOException {
        byte [] bytes = messageBody.getBytes("UTF-8");
        httpExchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream outputStream = httpExchange.getResponseBody();
        outputStream.write(bytes);
        outputStream.close();
        ChatServer.log("message sent to client: "/* + bytes + " as string: "*/ + messageBody);
    }


    private boolean checkHeaders(HttpExchange httpExchange) throws IOException {
        Headers headers = httpExchange.getRequestHeaders();
        if (!headers.containsKey("Content-Length")) {
            sendMessages(httpExchange, 411, "** Error 411. No content lenght in header");
            ChatServer.log("** error Content-Length zero");
            return false;
        }
        if (!headers.containsKey("Content-Type")) {
            sendMessages(httpExchange, 400, "** Error: No content type in request");
            ChatServer.log("** Error 400. No content type in request");
            return false;
        }
        if (!headers.get("Content-Type").get(0).equalsIgnoreCase("application/json")) {
            ChatServer.log("wrong content type: ");
            ChatServer.log(headers.get("Content-Type").get(0).toString());
            sendMessages(httpExchange, 411, "**** Error: Content-Type must be application/json");
            ChatServer.log("** Error 411. content type not application/json");
            return false;
        }
        return true;
    }

}
