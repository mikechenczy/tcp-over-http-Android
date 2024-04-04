package com.mj.tcpoverhttp.ws.server;

import com.mj.tcpoverhttp.ws.Utils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Mike_Chen
 * @date 2023/9/8
 * @apiNote
 */
public class ProxyClient extends Thread {
    public ChannelHandlerContext ctxOrigin;
    public ChannelHandlerContext ctx;
    public boolean closed;
    public ProxyClient(ChannelHandlerContext ctx) {
        this.ctxOrigin = ctx;
    }
    public ProxyClient connect() {
        start();
        return this;
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

    public void sendBack(byte[] msg) {
        if(ClientHandler.ctxList.containsKey(ctxOrigin)) {
            ctxOrigin.writeAndFlush(new BinaryWebSocketFrame(Utils.bytes2ByteBuf(msg)));
        }
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
        ClientHandler.ctxList.remove(ctxOrigin);
        ctxOrigin.close();
    }
}
