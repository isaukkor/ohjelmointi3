
package com.ohjelmointi3.chatserver;

import com.sun.net.httpserver.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


public class ChatHandler implements HttpHandler {

    ChatDatabase database = ChatDatabase.getInstance();

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try {
            if (httpExchange.getRequestMethod().equalsIgnoreCase("POST")) {
                handlePOSTRequestFromClient(httpExchange);
            }
            else if (httpExchange.getRequestMethod().equalsIgnoreCase("GET")) {
                handleGETRequestFromClient(httpExchange);
            }
            else {
                sendMessages(httpExchange, 400, "only get and post supported");
                ChatServer.log("only get and post supported");
            }
        }
        catch (Exception e) {
            ChatServer.log("** Error in ChatHandler: " + e);
        }
    }

    private void handlePOSTRequestFromClient(HttpExchange httpExchange) throws Exception {
        int statusCode = 200;
        ChatServer.log("handling POST");
        if (checkHeaders(httpExchange)) {
            InputStream input = httpExchange.getRequestBody();
            String receivedJson = new BufferedReader(new InputStreamReader(input,
                                            StandardCharsets.UTF_8))
                                            .lines()
                                            .collect(Collectors.joining("\n"));
            input.close();
            if (receivedJson.trim().length() > 0) {
                try {
                    JSONObject receivedAsJsonObj = new JSONObject(receivedJson);
                    if (checkPost(httpExchange, receivedAsJsonObj)) {

                        String sent = receivedAsJsonObj.getString("sent");
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
                        LocalDateTime ldt = LocalDateTime.parse(sent, formatter);
                        java.time.ZonedDateTime zdt = ldt.atZone(ZoneId.of("UTC"));
                        long millis = zdt.toInstant().toEpochMilli();
                        //System.out.println( "millis " + millis );
                        String dbString = "INSERT INTO messagetb VALUES('" + 
                                            receivedAsJsonObj.getString("user") + "','" +
                                            receivedAsJsonObj.getString("message") + "','" + 
                                            millis +
                                            "')";

                        if (database.insertIntoDatabase(dbString)) {
                            httpExchange.sendResponseHeaders(statusCode, -1);
                            ChatServer.log("POST done");
                            ChatServer.log("message saved: " + receivedAsJsonObj.getString("user") + receivedAsJsonObj.getString("message") + receivedAsJsonObj.getString("sent"));
                        }
                        else {
                            ChatServer.log("*** error chatHandler, message adding to db fail");
                            sendMessages(httpExchange, 431, "adding message to database failed");
                        }
                    }
                    else {
                        ChatServer.log("correct key elements not found in clients message:  user, message, sent");
                        sendMessages(httpExchange, 411, "correct key elements not found in message: user, message, sent");
                    }
                }
                catch (JSONException e) {
                    ChatServer.log("** Error JSON error" + e);
                    sendMessages(httpExchange, 411, "JSON exception" + e);
                }
            }
            else {
                sendMessages(httpExchange, 411, "No content in request");
                ChatServer.log("No content in request");
            }
        }
    }


    private void handleGETRequestFromClient(HttpExchange httpExchange) throws Exception {
        int statusCode = 200;
        ChatServer.log("handling GET request");
        boolean messagesInDb = false;
        ResultSet readDb = null;
        if (httpExchange.getResponseHeaders().containsKey("If-Modified-Since")) {
            String modifiedSince = httpExchange.getResponseHeaders().getFirst("If-Modified-Since");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            LocalDateTime ldt = LocalDateTime.parse(modifiedSince, formatter);
            java.time.ZonedDateTime zdt = ldt.atZone(ZoneId.of("UTC"));
            long millis = zdt.toInstant().toEpochMilli();
            //System.out.println( "millis " + millis );
            readDb = database.readFromDatabase(millis);
        }
        else {
            readDb = database.readFromDatabase(0);
        }
        System.out.println( readDb );
        JSONArray responseMessages = new JSONArray();
        JSONObject jsonMessage;
        String arrayAsString;
        long latestRead = 0;
        while (readDb.next()) {
            messagesInDb = true;

            Date date = new Date(readDb.getLong("sent"));
            if (readDb.getLong("sent") > latestRead) {
                latestRead = readDb.getLong("sent");
            }
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            String dateFormatted = formatter.format(date);

            jsonMessage = new JSONObject("{\"message\":\"" + readDb.getString("message") + "\"," + 
                                            "\"user\":\"" + readDb.getString("user") + "\"," + 
                                            "\"sent\":\"" + dateFormatted + "\"}");
            responseMessages.put(jsonMessage);
        }

        if(!messagesInDb) {
            sendMessages(httpExchange, 204, "No messages in server");
            ChatServer.log("client tried to GET when no messages stored");
        }
        else {
            Date latest = new Date(latestRead);
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            String dateFormatted = formatter.format(latest);
            httpExchange.getResponseHeaders().add("Last-Modified", dateFormatted);
            arrayAsString = responseMessages.toString();
            sendMessages(httpExchange, statusCode, arrayAsString);
            
            ChatServer.log("GET done. "/* + arrayAsString*/);
        }
    }


    private void sendMessages(HttpExchange httpExchange, int statusCode, String messageBody) throws Exception {
        byte [] bytes = messageBody.getBytes("UTF-8");
        httpExchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream outputStream = httpExchange.getResponseBody();
        outputStream.write(bytes);
        outputStream.close();
        ChatServer.log("message sent to client: " + messageBody);
    }


    private boolean checkPost(HttpExchange httpExchange, JSONObject message) throws Exception{
        if(message.length() == 3){
            if(message.has("user") && message.has("message") && message.has("sent")){
                return true;
            } else{
                sendMessages(httpExchange, 400, "JSON object has wrong keys");
            }
        } else{
            sendMessages(httpExchange, 400, "JSON object is wrong lenght");
        }
        return false;
    }


    private boolean checkHeaders(HttpExchange httpExchange) throws Exception {
        Headers headers = httpExchange.getRequestHeaders();
        if (!headers.containsKey("Content-Length")) {
            sendMessages(httpExchange, 411, "** Error 411. No content lenght in header");
            ChatServer.log("error Content-Length zero");
            return false;
        }
        if (!headers.containsKey("Content-Type")) {
            sendMessages(httpExchange, 400, "** Error: No content type in request");
            ChatServer.log("No content type in request");
            return false;
        }
        if (!headers.get("Content-Type").get(0).equalsIgnoreCase("application/json")) {
            sendMessages(httpExchange, 411, "** Error: Content-Type must be application/json");
            ChatServer.log("content type not application/json");
            return false;
        }
        return true;
    }
}
