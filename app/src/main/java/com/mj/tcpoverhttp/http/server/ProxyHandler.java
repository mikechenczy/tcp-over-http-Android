package com.mj.tcpoverhttp.http.server;

import com.mj.tcpoverhttp.http.Utils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author Mike_Chen
 * @date 2023/9/8
 * @apiNote
 */
public class ProxyHandler extends SimpleChannelInboundHandler<ByteBuf> {
    public ProxyClient proxyClient;
    public ProxyHandler(ProxyClient proxyClient) {
        this.proxyClient = proxyClient;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        proxyClient.ctx = ctx;
        proxyClient.flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        proxyClient.ctx = null;
        proxyClient.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
        proxyClient.sendBack(Utils.byteBuf2Bytes(byteBuf));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //super.exceptionCaught(ctx, cause);
    }
}
