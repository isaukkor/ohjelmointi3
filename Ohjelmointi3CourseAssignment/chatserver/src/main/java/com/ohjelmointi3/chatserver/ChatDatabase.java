package com.ohjelmointi3.chatserver;

import java.io.File;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

import org.apache.commons.codec.digest.Crypt;

public class ChatDatabase {

    SecureRandom secureRand = new SecureRandom();

    private static ChatDatabase singleton = null;
    Connection connection;
    String dbWithPath;
    PreparedStatement stmt = null;
    String dbPasswd;

    public static synchronized ChatDatabase getInstance() {
        if (null == singleton) {
            singleton = new ChatDatabase();
        }
        return singleton;
    }

    private ChatDatabase() {

    }
    

    public void open(String dbName) throws SQLException {
        dbWithPath = System.getProperty("user.dir") + "\\" + dbName;
        String jdbcString = "jdbc:sqlite:" + dbWithPath;
        connection = DriverManager.getConnection(jdbcString);
        if (new File(dbWithPath).length() == 0) {
            initializeDatabase();
        }
    }


    public void initializeDatabase() {
        String usertb = "CREATE TABLE usertb (\n"
        + "USERNAME TEXT NOT NULL,\n"
        + "PASSWORD STRING NOT NULL,\n"
        + "EMAIL TEXT NOT NULL, \n"
        + "PRIMARY KEY (USERNAME) \n"
        + ")";
        String messagetb = "CREATE TABLE messagetb (\n"
        + "USER TEXT NOT NULL,\n"
        + "MESSAGE TEXT NOT NULL, \n"
        + "SENT LONG NOT NULL, \n"
        + "PRIMARY KEY (SENT) \n"
        + ")";

        if (!insertIntoDatabase(usertb)) {
            ChatServer.log("*****Error: database usertb creation failed");
        }
        else {
            ChatServer.log("usertb creaed");
        }
        if (!insertIntoDatabase(messagetb)) {
            ChatServer.log("*****Error: database usertb creation failed");
        }
        else {
            ChatServer.log("messagetb creaed");
        }
    }

    public boolean insertIntoDatabase(String dbString) {
        try {
            stmt = connection.prepareStatement(dbString);
            stmt.executeUpdate();
            return true;
        }
        catch (SQLException e) {
            ChatServer.log("*** Error database fail: " + e);
            return false;
        }
    }

    
    public boolean addUserIntoDatabase(User userCredentials) {
        ChatServer.log("adding user ");
        byte bytes[] = new byte[13];
        secureRand.nextBytes(bytes);
        String saltBytes = new String(Base64.getEncoder().encode(bytes));
        String salt = "$6$" + saltBytes;
        String hashedPassword = Crypt.crypt(userCredentials.password, salt);
        try {
            String insertString = "INSERT INTO usertb VALUES('" + 
                                    userCredentials.username + "','" +
                                    hashedPassword + "','" + 
                                    userCredentials.email +
                                    "')";
            stmt = connection.prepareStatement(insertString);
            stmt.executeUpdate();
            return true;
        }
        catch (SQLException e) {
            ChatServer.log("user already exist: " + e);
            return false;
        }
    }


    public boolean checkCredentialsDatabase(String username, String passw) {
        try {
            String readString = "SELECT password FROM usertb WHERE username = '" + username + "'";
            stmt = connection.prepareStatement(readString);
            ResultSet readStmt = stmt.executeQuery();
            while (readStmt.next()) {
                dbPasswd = readStmt.getString("password");
            }
            if (dbPasswd.equals(Crypt.crypt(passw, dbPasswd))) {
                return true;
            }
            else {
                return false;
            }
        }
        catch (SQLException e) {
            ChatServer.log("*** Error database user credentials read fail: " + e);
            return false;
        }
    }


    public ResultSet readFromDatabase(long amount) {
        try {
            String readString;
            if (amount == 0) {
                readString = "SELECT * FROM messagetb ORDER BY SENT DESC LIMIT 100";
            }
            else {
                readString = "SELECT * FROM messagetb WHERE sent > " + amount + "ORDER BY SENT DESC";
            }
            stmt = connection.prepareStatement(readString);
            ResultSet readStmt = stmt.executeQuery();
            ChatServer.log("dbes: " + readStmt);
            return readStmt;
        }
        catch (SQLException e) {
            ChatServer.log("*** Error database read fail: " + e);
            ResultSet readStmt = null;
            return readStmt;
        }
    }


}
