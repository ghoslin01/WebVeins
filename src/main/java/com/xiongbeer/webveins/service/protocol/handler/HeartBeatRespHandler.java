package com.xiongbeer.webveins.service.protocol.handler;


import com.xiongbeer.webveins.service.protocol.message.MessageType;
import com.xiongbeer.webveins.service.protocol.message.ProcessDataProto.ProcessData;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;


/**
 * Created by shaoxiong on 17-5-28.
 */
public class HeartBeatRespHandler extends ChannelInboundHandlerAdapter {

    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        ProcessData resp = (ProcessData)msg;
        if (resp.getType() == MessageType.HEART_BEAT_REQ.getValue()) {
            ProcessData heartBeat = buildHeartBeat();
            ctx.writeAndFlush(heartBeat);
        }
    }

    public ProcessData buildHeartBeat() {
        ProcessData.Builder builder = ProcessData.newBuilder();
        builder.setType(MessageType.HEART_BEAT_RESP.getValue());
        return builder.build();
    }
}
