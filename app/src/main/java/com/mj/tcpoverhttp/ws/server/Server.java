package com.mj.tcpoverhttp.ws.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * @author Mike_Chen
 * @date 2023/9/8
 * @apiNote
 */
public class Server {
    public static String ip = "127.0.0.1";
    public static int port = 3389;
    public static int serverPort = 8080;
    public static void main(String[] args) {
        if(args!=null) {
            if(args.length>=1)
                ip = args[0];
            try {
                if(args.length>=2)
                    port = Integer.parseInt(args[1]);
                if(args.length>=3)
                    serverPort = Integer.parseInt(args[2]);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ChannelFuture channelFuture = null;
        try {

            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new HttpObjectAggregator(Integer.MAX_VALUE));
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new ChunkedWriteHandler());
                            pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
                            pipeline.addLast(new ClientHandler());
                        }

                    });
            channelFuture = b.bind(serverPort).sync();

            /*channelFuture.addListener((ChannelFutureListener) channelFuture -> {
                // 服务器已启动
            });*/
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (channelFuture != null) {
                channelFuture.channel().close().syncUninterruptibly();
            }
            if ((bossGroup != null) && (!bossGroup.isShutdown())) {
                bossGroup.shutdownGracefully();
            }
            if ((workerGroup != null) && (!workerGroup.isShutdown())) {
                workerGroup.shutdownGracefully();
            }
            // 服务器已关闭
        }
    }
}
