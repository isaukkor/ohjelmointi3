package chatclient;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.Certificate;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.sound.sampled.AudioFormat.Encoding;

import org.json.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

public class chatClient {
    public static void main( String[] args ) throws Exception {

        try {
            String cer = "C:\\Users\\kone\\Desktop\\ohejlmointi3\\testit\\O3-chat-client-main\\localhost.cer";
            URL url;
            java.security.cert.Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(new FileInputStream(cer));
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, null);
            keyStore.setCertificateEntry("localhost", certificate);
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            trustManagerFactory.init(keyStore);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            HttpsURLConnection connection;
            byte[] out;
            int length;
            OutputStream outputStream;

            boolean start = true, end = false;
            Scanner scan = new Scanner(System.in);
            String s, u, p, e, message, outString, responseMessage, username = null, password = null;
            System.out.println( "type 'reg' for registration, \n'chat' for chatting, \n'get' for getting, \n'user' for existing user or \n'end' to quit" );
            s = scan.nextLine();
            while (!end) {
                if (!start) {
                    System.out.println( "\nlogged in as " + username + ". Type 'reg' for registration, \n'chat' for chatting, \n'get' for getting, \n'user' for existing user or \n'end' to quit" );
                    s = scan.nextLine();
                }
                if (s.equals("end")) {
                    end = true;
                }
                else if (s.equals("user") || (start && !(s.equals("reg")))) {
                    System.out.println( "give existing username" );
                    s = scan.nextLine();
                    username = s;
                    System.out.println( "give password" );
                    s = scan.nextLine();
                    password = s;
                }
                else if (s.equals("reg")) {
                    url = new URL("https://localhost:8001/registration");
                    System.out.println( "give username, pass, email" );
                    u = scan.nextLine();
                    p = scan.nextLine();
                    e = scan.nextLine();
                    connection = (HttpsURLConnection) url.openConnection();
                    connection.setSSLSocketFactory(sslContext.getSocketFactory());
                    // All requests use these common timeouts.
                    connection.setConnectTimeout(1000);
                    connection.setReadTimeout(1000);
                    connection.setRequestMethod("POST"); // PUT is another valid option
                    connection.setDoOutput(true);
                    outString = "{\"username\":\"" + u + "\",\"password\":\"" + p + "\",\"email\":\"" + e + "\"}";
                    byte [] bytes = outString.getBytes("UTF-8");
                    length = bytes.length;
                    
                    connection.setFixedLengthStreamingMode(length);
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.connect();
                    outputStream = connection.getOutputStream();
                    outputStream.write(bytes);
                    outputStream.close();
                    responseMessage = connection.getResponseMessage();
                    System.out.println( responseMessage );
                }
                else if (s.equals("chat")) {                    
                    System.out.println( "posting, give message." );
                    message = scan.nextLine();
                    url = new URL("https://localhost:8001/chat");
                    connection = (HttpsURLConnection) url.openConnection();
                    connection.setSSLSocketFactory(sslContext.getSocketFactory());
                    // All requests use these common timeouts.
                    connection.setConnectTimeout(1000);
                    connection.setReadTimeout(1000);
                    connection.setRequestMethod("POST"); // PUT is another valid option
                    connection.setDoOutput(true);

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
                    ZonedDateTime now = ZonedDateTime.now();
                    String nowTime = now.format(formatter);

                    String auth = username + ":" + password;
                    String encodedString = Base64.getEncoder().encodeToString(auth.getBytes());
                    connection.setRequestProperty  ("Authorization", "Basic " + encodedString);
                    outString = "{\"user\":\"" + username + "\",\"message\":\"" + message + "\",\"sent\":\"" + nowTime + "\"}";
                    byte [] bytes = outString.getBytes("UTF-8");
                    length = bytes.length;
                    
                    connection.setFixedLengthStreamingMode(length);
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.addRequestProperty("If-Modified-Since", nowTime);
                    connection.addRequestProperty("Last-Modified", nowTime);
                    connection.connect();
                    outputStream = connection.getOutputStream();
                    outputStream.write(bytes);
                    outputStream.close();
                    responseMessage = connection.getResponseMessage();
                    System.out.println( responseMessage );
                }
                else if (s.equals("get")) {
                    System.out.println( "getting." );
                    message = "GET";
                    url = new URL("https://localhost:8001/chat");
                    connection = (HttpsURLConnection) url.openConnection();
                    connection.setSSLSocketFactory(sslContext.getSocketFactory());
                    connection.setConnectTimeout(1000);
                    connection.setReadTimeout(1000);
                    connection.setRequestMethod("GET");
                    //connection.getRequestMethod();
                    connection.setDoOutput(true);
            
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
                    ZonedDateTime now = ZonedDateTime.now();
                    String nowTime = now.format(formatter);
                    String auth = username + ":" + password;
                    String encodedString = Base64.getEncoder().encodeToString(auth.getBytes());
                    connection.setRequestProperty  ("Authorization", "Basic " + encodedString);
                    outString = "{\"user\":\"" + username + "\",\"message\":\"" + message + "\",\"sent\":\"" + nowTime + "\"}";
                    byte [] bytes = outString.getBytes("UTF-8");
                    length = bytes.length;
                    
                    connection.setFixedLengthStreamingMode(length);
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.addRequestProperty("If-Modified-Since", null);
                    connection.addRequestProperty("Last-Modified", nowTime);
                    connection.connect();

                    outputStream = connection.getOutputStream();
                    outputStream.write(bytes);
                    responseMessage = connection.getResponseMessage();
                    System.out.println( "response: " + responseMessage );
                    String readLine = null;
                    int responseCode = connection.getResponseCode();
                    System.out.println( "responseCode: " + responseCode );
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        //JSONObject in = connection.getInputStream();
                        System.out.println("JSON String Result1 " + in);
                        System.out.println("JSON String Result2 " + in.toString());
                        //System.out.println("JSON String Result2 " + in.getString("user"));
                        StringBuffer response = new StringBuffer();
                        System.out.println("JSON String Result3 " + response);
                        while ((readLine = in .readLine()) != null) {
                            response.append(readLine);
                        } 
                        in .close();
                        // print result
                        System.out.println("JSON String Result " + response.toString());
                        //GetAndPost.POSTRequest(response.toString());
                    } else {
                        System.out.println("GET NOT WORKED");
                    }
                }
                else {
                    System.out.println( "give proper input" );
                }
                start = false;
            }
            scan.close();
        }
        catch (Exception e) {
            log("Error exception: " + e);
            e.printStackTrace();
        }

    }
    public static void log(String message) {
        System.out.println(LocalDateTime.now() + ": " + message);
    }
    
}
