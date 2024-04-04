package com.mj.tcpoverhttp.http.client;

import com.mj.tcpoverhttp.http.Utils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Mike_Chen
 * @date 2023/9/8
 * @apiNote
 */
public class ClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
    public static Map<ChannelHandlerContext, HttpClient> ctxList = new HashMap<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctxList.put(ctx, new HttpClient(ctx).connect());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        //System.out.println(byteBuf);
        if(!ctxList.containsKey(ctx)) {
            ctx.close();
        }
        ctxList.get(ctx).send(Utils.byteBuf2Bytes(byteBuf));
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
