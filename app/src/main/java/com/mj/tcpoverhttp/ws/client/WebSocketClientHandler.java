package com.mj.tcpoverhttp.ws.client;

import com.mj.tcpoverhttp.ws.Utils;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;

public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
    private WebSocketClientHandshaker handshaker = null;
    private ChannelPromise handshakeFuture = null;
    public WebSocketClient webSocketClient;

    public WebSocketClientHandler(WebSocketClient webSocketClient) {
        this.webSocketClient = webSocketClient;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        //webSocketClient.clientHandler.setCtx(ctx);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        this.handshakeFuture = ctx.newPromise();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        //System.out.println("WebSocketClientHandler::channelRead0: ");
        // 握手协议返回，设置结束握手
        if (!this.handshaker.isHandshakeComplete()){
            FullHttpResponse response = (FullHttpResponse)msg;
            this.handshaker.finishHandshake(ctx.channel(), response);
            this.handshakeFuture.setSuccess();
            webSocketClient.ctx = ctx;
            //System.out.println("WebSocketClientHandler::channelRead0 HandshakeComplete...");
            webSocketClient.flush();
            //webSocketClient.clientHandler.sendBasicData();
            //webSocketClient.clientHandler.startHeartbeat();
            return;
        }
        if(msg instanceof BinaryWebSocketFrame) {
            webSocketClient.sendBack(Utils.byteBuf2Bytes(((BinaryWebSocketFrame) msg).content()));
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        webSocketClient.ctx = null;
        webSocketClient.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //super.exceptionCaught(ctx, cause);
    }

    public void setHandshaker(WebSocketClientHandshaker handshaker) {
        this.handshaker = handshaker;
    }

    public ChannelFuture handshakeFuture() {
        return this.handshakeFuture;
    }

    public ChannelPromise getHandshakeFuture() {
        return handshakeFuture;
    }

    public void setHandshakeFuture(ChannelPromise handshakeFuture) {
        this.handshakeFuture = handshakeFuture;
    }

}