package com.xiongbeer.webveins.service.protocol;

import com.xiongbeer.webveins.Configuration;
import com.xiongbeer.webveins.saver.HDFSManager;
import com.xiongbeer.webveins.service.protocol.handler.HeartBeatRespHandler;
import com.xiongbeer.webveins.service.protocol.handler.LoginAuthRespHandler;
import com.xiongbeer.webveins.service.protocol.handler.ShellRespHandler;
import com.xiongbeer.webveins.service.protocol.handler.WorkerProxyHandler;
import com.xiongbeer.webveins.service.protocol.message.ProcessDataProto.ProcessData;
import com.xiongbeer.webveins.utils.InitLogger;
import com.xiongbeer.webveins.zk.worker.Worker;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.internal.ConcurrentSet;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Created by shaoxiong on 17-5-28.
 */
public class Server {
    private static Set<Channel> channels = new ConcurrentSet<>();
    private Logger logger = LoggerFactory.getLogger(Server.class);
    private final int port;
    private Worker worker;
    private CuratorFramework client;
    private HDFSManager hdfsManager;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public Server(int port, CuratorFramework client
            , HDFSManager hdfsManager, Worker worker) {
        this.port = port;
        this.worker = worker;
        this.client = client;
        this.hdfsManager = hdfsManager;
    }

    public void bind() {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        try {
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                            ch.pipeline().addLast(new ProtobufDecoder(ProcessData.getDefaultInstance()));
                            ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                            ch.pipeline().addLast(new ProtobufEncoder());
                            ch.pipeline().addLast(new ReadTimeoutHandler(60));
                            ch.pipeline().addLast(new LoginAuthRespHandler(channels));
                            ch.pipeline().addLast(new ShellRespHandler(client, hdfsManager));
                            ch.pipeline().addLast(new WorkerProxyHandler(worker));
                            ch.pipeline().addLast(new HeartBeatRespHandler());
                        }
                    });
            ChannelFuture f = b.bind(port).sync();
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        if(workerGroup != null && bossGroup != null) {
            try {
                workerGroup.shutdownGracefully().sync();
                bossGroup.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }

        }
    }

    public static void main(String[] args) {
        Configuration.getInstance();
        InitLogger.init();
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework client = CuratorFrameworkFactory.newClient("127.0.0.1:2181/webveins", retryPolicy);
        client.start();
        Worker worker = new Worker(client, "123");

        int port = 8080;
        new Server(port, client, new HDFSManager(Configuration.HDFS_SYSTEM_CONF, Configuration.HDFS_SYSTEM_PATH), worker).bind();
    }
}
