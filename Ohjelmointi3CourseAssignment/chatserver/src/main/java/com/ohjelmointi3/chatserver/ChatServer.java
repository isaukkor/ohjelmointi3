package com.ohjelmointi3.chatserver;

//import com.ohjelmointi3.chatserver.ChatHandler;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.io.IOException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLParameters;

import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;

/**
 * Hello world!
 * KEytool:    keystore pass: ohjelmointi
 * Generating 2 048 bit RSA key pair and self-signed certificate (SHA256withRSA) with a validity of 90 days
        for: CN=localhost, OU=localhost, O=localhost, L=oulu, ST=oulu, C=fi
 *
 * java -jar target/chatserver-1.0-SNAPSHOT-jar-with-dependencies.jar
 * 
 * curl -k -d asd:asd https://localhost:8001/registration -H "Content-Type: text/plain" --trace-ascii out.txt
 * curl -k -u asd:asd -d 'messageasd'  https://localhost:8001/chat -H "Content-Type: text/plain" --trace-ascii out.txt
 * curl -k -u asd:asd https://localhost:8001/chat
 * 
 * 
 * curl -k -d "@testuser.json" https://localhost:8001/registration -H "Content-Type: application/json"
 * 
 * ###server:
 * cd C:\Users\kone\Desktop\ohejlmointi3\chatserver & mvn package & java -jar target\chatserver-1.0-SNAPSHOT-jar-with-dependencies.jar
 * ###tests:
 * cd C:\Users\kone\Desktop\ohejlmointi3\testit\O3-chat-client-main & mvn package -DskipTests & mvn test -Dtestsettings="test-config-1.xml"
 */
public class ChatServer 
{
    public static void main( String[] args ) throws Exception {

        ChatDatabase database = ChatDatabase.getInstance();

        try {
            //System.out.println("Working Directory = " + System.getProperty("user.dir"));
            String dbName = "database.db";
            database.open(dbName);
            log("Starting ChatServer");
            HttpsServer server = HttpsServer.create(new InetSocketAddress(8001), 0);
            SSLContext sslContext = chatServerSSLContext();
            server.setHttpsConfigurator (new HttpsConfigurator(sslContext) {
                public void configure (HttpsParameters params) {
                    InetSocketAddress remote = params.getClientAddress();
                    SSLContext c = getSSLContext();
                    SSLParameters sslparams = c.getDefaultSSLParameters();
                    params.setSSLParameters(sslparams);
                }
            });
            ChatAuthenticator auth = new ChatAuthenticator();
            HttpContext chatContext = server.createContext("/chat", new ChatHandler());
            chatContext.setAuthenticator(auth);
            server.createContext("/registration", new RegistrationHandler(auth));
            server.setExecutor(null); // creates a default executor
            server.start(); 
            log("Started ChatServer");
        }
        catch (Exception e) {
            log("Error exception: " + e);
        }
    }

    public static void log(String message) {
        System.out.println(LocalDateTime.now() + ": " + message);
    }

    private static SSLContext chatServerSSLContext() throws Exception {
        char[] passphrase = "ohjelmointi".toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream("keystore.jks"), passphrase);
     
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);
     
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);
     
        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return ssl;
    }
}
