package com.mj.tcpoverhttp.http.server;

import com.alibaba.fastjson.JSONObject;
import com.mj.tcpoverhttp.http.Utils;
import com.mj.tcpoverhttp.http.server.controller.ProxyController;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Mike_Chen
 * @date 2023/9/8
 * @apiNote
 */
public class ProxyClient extends Thread {
    public long uniqueId;
    public ChannelHandlerContext ctx;
    public boolean closed;
    public ProxyClient(long uniqueId) {
        this.uniqueId = uniqueId;
    }

    public List<byte[]> byteBufList = new ArrayList<>();

    public void send(byte[] msg) {
        flush();
        if(ctx!=null) {
            ctx.writeAndFlush(Utils.bytes2ByteBuf(msg));
        } else {
            byteBufList.add(msg);
        }
    }
    public void flush() {
        if(ctx==null)
            return;
        if(byteBufList.size()>0){
            for(byte[] bytes : byteBufList) {
                ctx.writeAndFlush(Utils.bytes2ByteBuf(bytes));
            }
            byteBufList.clear();
        }
    }

    public final List<Byte> sendBackDump = new ArrayList<>();

    public void sendBack(byte[] msg) {
        synchronized (sendBackDump) {
            Byte[] bytes = new Byte[msg.length];
            for (int i = 0; i < msg.length; i++) {
                bytes[i] = msg[i];
            }
            Collections.addAll(sendBackDump, bytes);
        }
    }

    public long index = 0;

    public String getNeedToSend() {
        if(sendBackDump.size()==0)
            return "{}";
        synchronized (sendBackDump) {
            Byte[] msg = sendBackDump.toArray(new Byte[]{});
            byte[] bytes = new byte[msg.length];
            for (int i = 0; i < msg.length; i++) {
                bytes[i] = msg[i];
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("index", index++);
            jsonObject.put("bytes", bytes);
            sendBackDump.clear();
            return jsonObject.toJSONString();
        }
    }

    private void write(HttpServletResponse response, byte[] bytes) throws IOException {
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
    }

    @Override
    public void run() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup()).channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new ProxyHandler(ProxyClient.this));
                    }
                });
        bootstrap.connect(Server.ip, Server.port).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                close();
            }
        });
    }

    public void close() {
        if(closed) {
            return;
        }
        closed = true;
        byteBufList.clear();
        if(ctx!=null) {
            ctx.close();
        }
        ProxyController.map.remove(uniqueId);
    }
}
