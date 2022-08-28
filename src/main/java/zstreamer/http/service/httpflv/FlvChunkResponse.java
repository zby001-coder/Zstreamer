package zstreamer.http.service.httpflv;

import io.netty.handler.codec.http.DefaultHttpResponse;
import zstreamer.MediaMessagePool;
import zstreamer.http.entity.request.WrappedRequest;
import zstreamer.http.entity.response.chunk.ChunkedResponse;
import zstreamer.http.entity.response.chunk.SuccessorChuck;
import zstreamer.http.service.httpflv.flv.FlvHeader;
import zstreamer.http.service.httpflv.flv.FlvTag;
import zstreamer.rtmp.message.messageType.media.MediaMessage;

/**
 * @author 张贝易
 * 生产flv的chunk的响应
 */
public class FlvChunkResponse extends ChunkedResponse {
    private int basicTimeStamp = -1;
    /**
     * 当前要推的message和上一个推的message
     */
    private MediaMessagePool.Node now;
    private MediaMessagePool.Node last;

    public FlvChunkResponse(DefaultHttpResponse header, WrappedRequest request, String roomName, int timeStamp) throws Exception {
        super(header, request);
        now = MediaMessagePool.pullMediaMessage(roomName, timeStamp);
        last = null;
    }

    @Override
    public SuccessorChuck generateChunk() {
        byte[] bytes = pullMessage();
        return bytes == null ? null : new SuccessorChuck(bytes);
    }

    /**
     * 用户从当前房间拉一段FLV数据并写出到channel中
     *
     * @return 返回1表明拉到了，0表示没拉到
     */
    private byte[] pullMessage() {
        if (now != null && now.getMessage() != null) {
            if (basicTimeStamp == -1) {
                return writeBasic(now);
            }
            FlvTag mediaTag = new FlvTag(now.getMessage(), basicTimeStamp);
            //IO线程刷新缓冲区的速度不一定跟得上服务器发送的速度
            //所以如果缓冲区满了就不继续发送
            last = now;
            now = now.getNext();
            return mediaTag.generateTag();
        } else if (last != null && last.hasNext()) {
            //这个分支是为了处理上一次拉流时 now.next = null的情况
            now = last.getNext();
            return pullMessage();
        }
        return null;
    }

    /**
     * 第一次拉流时会额外拉到一个FLVHeader和一个ScriptTag和三个metadata
     *
     * @param node 媒体信息节点
     */
    private byte[] writeBasic(MediaMessagePool.Node node) {
        MediaMessage firstMedia = node.getMessage();
        basicTimeStamp = firstMedia.getTimeStamp();

        FlvHeader header = new FlvHeader((byte) 1, (byte) 1);
        byte[] headerBytes = header.generateHeader();

        FlvTag scriptTag = new FlvTag(node.getMetaData());
        byte[] scriptBytes = scriptTag.generateTag();

        FlvTag avc = new FlvTag(node.getAvcSequenceHeader(), node.getAvcSequenceHeader().getTimeStamp());
        byte[] avcBytes = avc.generateTag();

        FlvTag aac = new FlvTag(node.getAacSequenceHeader(), node.getAacSequenceHeader().getTimeStamp());
        byte[] aacBytes = aac.generateTag();

        byte[] seiBytes = null;
        //由于sei信息不是必须的，所以要判空
        if (now.getSei() != null) {
            FlvTag sei = new FlvTag(node.getSei(), node.getSei().getTimeStamp());
            seiBytes = sei.generateTag();
        }

        byte[] result = new byte[headerBytes.length + scriptBytes.length + avcBytes.length + aacBytes.length + (seiBytes != null ? seiBytes.length : 0)];
        int destPos = 0;
        System.arraycopy(headerBytes, 0, result, destPos, headerBytes.length);
        destPos += headerBytes.length;
        System.arraycopy(scriptBytes, 0, result, destPos, scriptBytes.length);
        destPos += scriptBytes.length;
        System.arraycopy(avcBytes, 0, result, destPos, avcBytes.length);
        destPos += avcBytes.length;
        System.arraycopy(aacBytes, 0, result, destPos, aacBytes.length);
        destPos += aacBytes.length;
        if (seiBytes != null) {
            System.arraycopy(seiBytes, 0, result, destPos, seiBytes.length);
        }
        return result;
    }

}
