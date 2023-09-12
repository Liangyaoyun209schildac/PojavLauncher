
package net.kdt.pojavview.forgedisplay;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;

public class SocketDisplay {
    public int port;
    private ChannelFuture channelFuture;
    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    private final IForgeUpdate update;

    public SocketDisplay(IForgeUpdate runnable) {
        update = runnable;
        new Thread(this::start).start();
    }

    public void start() {
        Random random = new Random();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(@NotNull SocketChannel socketChannel) {
                            socketChannel.pipeline()
                                    .addLast(new LengthFieldPrepender(4))
                                    .addLast(new LengthFieldBasedFrameDecoder(2048, 0, 4, 0, 4))
                                    .addLast(new ServerHandler(update));
                        }
                    });
            int count = 0;
            while (true) {
                port = random.nextInt() % 65535;
                try {
                    channelFuture = bootstrap.bind(port);
                    break;
                } catch (Exception e) {
                    count++;
                    if (count > 10) {
                        return;
                    }
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {

        }
    }

    public void close() {
        channelFuture.channel().close();
    }
}
