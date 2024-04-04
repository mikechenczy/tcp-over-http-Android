package com.mj.tcpoverhttp.ws.server;

import com.mj.tcpoverhttp.ws.Utils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Mike_Chen
 * @date 2023/9/8
 * @apiNote
 */
public class ClientHandler extends SimpleChannelInboundHandler<BinaryWebSocketFrame> {
    public static Map<ChannelHandlerContext, ProxyClient> ctxList = new HashMap<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctxList.put(ctx, new ProxyClient(ctx).connect());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BinaryWebSocketFrame msg) throws Exception {
        //System.out.println(msg);
        if(!ctxList.containsKey(ctx)) {
            ctx.close();
        }
        ctxList.get(ctx).send(Utils.byteBuf2Bytes(msg.content()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if(ctxList.containsKey(ctx)) {
            ctxList.get(ctx).close();
        }
        ctxList.remove(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //super.exceptionCaught(ctx, cause);
    }
}
