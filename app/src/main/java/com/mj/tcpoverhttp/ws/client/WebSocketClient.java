package com.mj.tcpoverhttp.ws.client;

import com.mj.tcpoverhttp.ws.MyHandshaker;
import com.mj.tcpoverhttp.ws.Utils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mike_Chen
 * @date 2023/9/8
 * @apiNote
 */
public class WebSocketClient extends Thread {
    public ChannelHandlerContext ctxOrigin;
    public ChannelHandlerContext ctx;
    public boolean closed;
    public WebSocketClient(ChannelHandlerContext ctx) {
        this.ctxOrigin = ctx;
    }
    public WebSocketClient connect() {
        start();
        return this;
    }

    public List<byte[]> byteBufList = new ArrayList<>();

    public void send(byte[] msg) {
        flush();
        if(ctx!=null) {
            ctx.writeAndFlush(new BinaryWebSocketFrame(Utils.bytes2ByteBuf(msg)));
        } else {
            byteBufList.add(msg);
        }
    }
    public void flush() {
        if(ctx==null)
            return;
        if(byteBufList.size()>0){
            for(byte[] bytes : byteBufList) {
                ctx.writeAndFlush(new BinaryWebSocketFrame(Utils.bytes2ByteBuf(bytes)));
            }
            byteBufList.clear();
        }
    }
    public void sendBack(byte[] msg) {
        if(ClientHandler.ctxList.containsKey(ctxOrigin)) {
            ctxOrigin.writeAndFlush(Utils.bytes2ByteBuf(msg));
        }
    }

    @Override
    public void run() {
        EventLoopGroup client = new NioEventLoopGroup();
        try{
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(client);
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            bootstrap.option(ChannelOption.TCP_NODELAY, true);
            bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
            bootstrap.channel(NioSocketChannel.class);
            final WebSocketClientHandler[] handler = {new WebSocketClientHandler(this)};
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel channel) {
                    ChannelPipeline pipeline = channel.pipeline();
                    //TODO add proxy support
                    //pipeline.addFirst("httpCLientProxy", new HttpProxyHandler(new InetSocketAddress("127.0.0.1", 54377)));
                    pipeline.addLast(new HttpClientCodec(), new HttpObjectAggregator(2155380*10));
                    //pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(2155380*10, 0, 4, 0, 4));
                    pipeline.addLast("handler", handler[0]);
                }
            });
            URI uri = new URI(Client.url);
            ChannelFuture cf = bootstrap.connect(uri.getHost(), uri.getPort()).sync();
            cf.addListener((GenericFutureListener<ChannelFuture>) channelFuture -> {
                //String log = String.format("连接websocket服务器: %s isSuccess=%s", Client.url, channelFuture.isSuccess());
                //System.out.println(log);
                if(channelFuture.isSuccess()){
                    //进行握手
                    Channel channel = channelFuture.channel();
                    handler[0] = (WebSocketClientHandler)channel.pipeline().get("handler");
                    WebSocketClientHandshaker handshaker =
                            new MyHandshaker(uri, new DefaultHttpHeaders(), 2155380*10, Client.host);
                    handler[0].setHandshaker(handshaker);
                    handshaker.handshake(channel);
                    // 阻塞等待是否握手成功?
                    //handler.handshakeFuture().sync();
                    handler[0].handshakeFuture();
                }
            });
            cf.channel().closeFuture().sync();
        } catch (Exception ex){
            ex.printStackTrace();
        } finally {
            client.shutdownGracefully();
        }
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
