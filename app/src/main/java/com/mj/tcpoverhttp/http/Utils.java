package com.mj.tcpoverhttp.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * @author Mike_Chen
 * @date 2023/9/8
 * @apiNote
 */
public class Utils {
    public static byte[] byteBuf2Bytes(ByteBuf byteBuf) {
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        return bytes;
    }

    public static ByteBuf bytes2ByteBuf(byte[] bytes) {
        return Unpooled.wrappedBuffer(bytes);
    }
}
