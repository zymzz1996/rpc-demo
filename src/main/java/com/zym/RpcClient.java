package com.zym;

import com.google.protobuf.Any;
import com.zym.protobuf.PersonTestProtos;
import com.zym.protobuf.ProtoBufRequest;
import com.zym.protobuf.ProtoBufResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RpcClient {

    public void run() throws InterruptedException {
        String host = "localhost";
        int port = 8081;

        BalanceServer balanceServer = BalanceServer.getInstance();
        String server = balanceServer.getServer();
        log.info("rpc url: " + server);
        String[] split = server.split(":");
        host = split[0];
        port = Integer.parseInt(split[1]);

        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap(); // (1)
            b.group(workerGroup); // (2)
            b.channel(NioSocketChannel.class); // (3)
            b.option(ChannelOption.SO_KEEPALIVE, true); // (4)
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast("frameDecoder",
                            new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
                    pipeline.addLast("protobufDecoder", new ProtobufDecoder(ProtoBufResponse.Response.getDefaultInstance()));
                    pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
                    pipeline.addLast("protobufEncoder", new ProtobufEncoder());

                    pipeline.addLast(new RpcClientHandler());
                }
            });

            // 启动客户端
            ChannelFuture f = b.connect(host, port).sync(); // (5)

            //构建消息
            ProtoBufRequest.Request.Builder builder = ProtoBufRequest.Request.newBuilder();
            builder.setCode(ProtoBufRequest.Request.Code.SUCCESS);
            builder.setMessage("Hello");

            PersonTestProtos.PersonTest.Builder personBuilder = PersonTestProtos.PersonTest.newBuilder();
            // personTest 赋值
            personBuilder.setName("zym");
            personBuilder.setEmail("123456@gmail.com");
            personBuilder.setSex(PersonTestProtos.PersonTest.Sex.MALE);

            // 生成 personTest 对象
            PersonTestProtos.PersonTest personTest = personBuilder.build();
            Any pack = Any.pack(personTest);
            builder.setDetails(pack);
            ProtoBufRequest.Request request = builder.build();

            f.channel().writeAndFlush(request);

            // 等待连接关闭
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }


}
