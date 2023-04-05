package com.zym;

import com.zym.protobuf.ProtoBufResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RpcClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ProtoBufResponse.Response res = (ProtoBufResponse.Response) msg;
        log.info(res.toString());
        ctx.close();
    }
}
