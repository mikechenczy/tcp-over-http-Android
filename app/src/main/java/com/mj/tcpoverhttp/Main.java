package com.mj.tcpoverhttp;

import java.io.*;
import java.net.*;

public class Main {
    static String targetUrl = "http://192.168.1.4:3389/";
    static int listenTcpPort = 8080;
    static String bindIp = "::";
    public static void main(String[] args) {
        if(args.length>=1)
            targetUrl = args[0];
        try {
            if(args.length>=2)
                listenTcpPort = Integer.parseInt(args[1]);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        try {
            startServer(bindIp, listenTcpPort, targetUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void startServer(String bindIp, int listenTcpPort, String targetUrl) throws IOException {

        ServerSocket serverSocket = new ServerSocket(listenTcpPort);
        System.out.println("Listening on " + bindIp + ":" + listenTcpPort);

        while (true) {
            new Handler(serverSocket.accept(), targetUrl).start();
        }
    }
}
