package com.mj.tcpoverhttp.http.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * @author Mike_Chen
 * @date 2023/9/8
 * @apiNote
 */
public class HttpClient extends Thread {
    public ChannelHandlerContext ctxOrigin;
    public boolean closed;
    public HttpClient(ChannelHandlerContext ctx) {
        this.ctxOrigin = ctx;
        uniqueId = new Random().nextLong();
    }

    public HttpClient connect() {
        start();
        return this;
    }

    public List<byte[]> byteBufList = new ArrayList<>();

    private boolean connected;

    private final long uniqueId;

    private Long msgSendIndex = 0L;

    public void send(byte[] msg) {
        flush();
        if(connected) {
            writeAndFlush(msg);
        } else {
            byteBufList.add(msg);
        }
    }
    public void flush() {
        if(!connected)
            return;
        if (byteBufList.size() > 0) {
            synchronized (byteBufList) {
                for (byte[] bytes : byteBufList) {
                    writeAndFlush(bytes);
                }
                byteBufList.clear();
            }
        }
    }
    public void sendBack(byte[] msg) {
        if(ClientHandler.ctxList.containsKey(ctxOrigin)) {
            ByteBuf buffer = ctxOrigin.alloc().buffer();
            buffer.writeBytes(msg);
            ctxOrigin.writeAndFlush(buffer);
            //ctxOrigin.writeAndFlush(Utils.bytes2ByteBuf(msg));
        }
    }

    public Integer processing = 0;

    public int state = 0;

    public long remoteIndex = 0;

    @Override
    public void run() {
        org.apache.http.client.HttpClient client = new DefaultHttpClient();
        try {
            /*HttpGet httpGet = new HttpGet(Client.url+"get?uniqueId="+ uniqueId+"&state="+(state++));
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(10000)  //连接超时时间
                    .setSocketTimeout(10000)   //读取数据超时时间
                    .build();
            httpGet.setConfig(requestConfig);*/
            //System.out.println(httpGet.getURI());
            JSONObject jsonObject = JSON.parseObject(get(Client.url+"get?uniqueId="+ uniqueId+"&state="+(state++)));//EntityUtils.toString(client.execute(httpGet).getEntity()));
            connected = true;
            flush();
            if(jsonObject.containsKey("bytes")) {
                while (remoteIndex!=jsonObject.getLong("index")) {
                    try {
                        Thread.sleep(0);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                sendBack(jsonObject.getBytes("bytes"));
                remoteIndex++;
            }
            while (connected) {
                Thread.sleep(0);
                synchronized (processing) {
                    if (processing < Client.processingCount) {
                        processing++;
                        new Thread(() -> {
                            /*CloseableHttpClient httpClient = new DefaultHttpClient();
                            HttpGet get = new HttpGet(Client.url + "get?uniqueId=" + uniqueId + "&state=" + state);
                            JSONObject json;
                            try {
                                json = JSON.parseObject(EntityUtils.toString(httpClient.execute(get).getEntity()));
                            } catch (IOException e) {
                                e.printStackTrace();
                                return;
                            }*/
                            JSONObject json;
                            try {
                                json = JSON.parseObject(get(Client.url + "get?uniqueId=" + uniqueId + "&state=" + state));
                            } catch (IOException e) {
                                e.printStackTrace();
                                return;
                            }
                            //System.out.println(json);
                            new Thread(() -> {
                                if (json.containsKey("bytes")) {
                                    while (remoteIndex != json.getLong("index")) {
                                        try {
                                            Thread.sleep(0);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    sendBack(json.getBytes("bytes"));
                                    remoteIndex++;
                                }
                                synchronized (processing) {
                                    processing--;
                                }
                            }).start();
                        }).start();
                    }
                }
            }
            client.execute(new HttpGet(Client.url+"get?uniqueId="+ uniqueId+"&state="+(state=3)));
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private String get(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(10000);
        con.setReadTimeout(10000);
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();
        return content.toString();
    }

    public void writeAndFlush(byte[] bytes) {
        try {
            synchronized (msgSendIndex) {
                HttpPost httpPost = new HttpPost(Client.url + "post?uniqueId=" + uniqueId + "&index="+(msgSendIndex++));
                httpPost.setEntity(new ByteArrayEntity(bytes));
                new Thread(() -> {
                    try {
                        new DefaultHttpClient().execute(httpPost);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void close() {
        if(closed) {
            return;
        }
        connected = false;
        closed = true;
        byteBufList.clear();
        ClientHandler.ctxList.remove(ctxOrigin);
        ctxOrigin.close();
    }
}
