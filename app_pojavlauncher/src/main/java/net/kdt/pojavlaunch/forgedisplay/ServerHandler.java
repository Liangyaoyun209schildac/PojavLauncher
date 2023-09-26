package net.kdt.pojavlaunch.forgedisplay;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.StandardCharsets;
public class ServerHandler extends ChannelInboundHandlerAdapter {
    private final IForgeUpdate update;
    private int maxMemory, totalMemory, freeMemory;
    private int step, steps;
    private String title, message;

    public ServerHandler(IForgeUpdate update) {
        this.update = update;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf byteBuf = (ByteBuf) msg;
        int type = byteBuf.readInt();
        switch (type) {
            case 0:
                maxMemory = byteBuf.readInt();
                totalMemory = byteBuf.readInt();
                freeMemory = byteBuf.readInt();
                update.forgeUpdate(0, true, title, message, step, steps, maxMemory, totalMemory, freeMemory);
                break;
            case 1:
            case 2:
            case 3:
                boolean have = byteBuf.readBoolean();
                if (have) {
                    title = readString(byteBuf);
                    message = readString(byteBuf);
                    step = byteBuf.readInt();
                    steps = byteBuf.readInt();
                }
                update.forgeUpdate(type, have, title, message, step, steps, maxMemory, totalMemory, freeMemory);
                break;
            case 5:
                update.forgeUpdate(5, false, null, null, 0, 0, 0, 0, 0);
                break;
        }
    }

    public String readString(ByteBuf buffer) {
        int length = buffer.readInt();
        if (length == 0)
            return "";
        byte[] temp = new byte[length];
        buffer.readBytes(temp, 0, length);
        return new String(temp, StandardCharsets.UTF_8);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
    }
}