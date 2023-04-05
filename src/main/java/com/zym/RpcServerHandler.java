package com.zym;

import com.zym.protobuf.ProtoBufRequest;
import com.zym.protobuf.ProtoBufResponse;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RpcServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ProtoBufRequest.Request req = (ProtoBufRequest.Request) msg;
        log.info(req.toString());
        //do something
        //...

        ProtoBufResponse.Response.Builder res = ProtoBufResponse.Response.newBuilder();
        res.setCode(ProtoBufResponse.Response.Code.SUCCESS);
        res.setMessage("success");
        Channel channel = ctx.channel();
        ChannelFuture f = channel.writeAndFlush(res.build());
        f.addListener(ChannelFutureListener.CLOSE);
    }
}
