package zstreamer.rtmp.message.messageType.media;

import io.netty.buffer.ByteBuf;
import zstreamer.rtmp.message.afm.AfmDecoder;
import zstreamer.rtmp.message.afm.AfmEncoder;
import zstreamer.rtmp.message.afm.AfmObject;
import zstreamer.rtmp.message.messageType.RtmpMessage;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author 张贝易
 * 处理元数据的message
 * 消息体由一个string类型的command、一个string类型的script、一个Ecma数组构成
 * @see AfmEncoder
 * @see AfmDecoder
 */
public class DataMessage extends RtmpMessage {
    public static final int TYPE_ID = 18;
    public static final String META_DATA = "@setDataFrame";
    private AfmObject.StringObject command;
    private AfmObject.StringObject script;
    private AfmObject.EcmaArrayObject param;

    public DataMessage(ByteBuf in) {
        //由于这个类型的message必须在已知byteBuf的情况下才能设置长度，所以无法在preInitialize中直接写
        this.messageLength = in.readableBytes();
        decodeBuf(in);
    }

    public DataMessage(RtmpMessage message, ByteBuf in) {
        super(message);
        this.messageLength = in.readableBytes();
        decodeBuf(in);
    }

    public DataMessage(RtmpMessage message, AfmObject.StringObject command,
                       AfmObject.StringObject script, AfmObject.EcmaArrayObject param) {
        super(message);
        this.command = command;
        this.script = script;
        this.param = param;
    }

    public DataMessage(AfmObject.StringObject command, AfmObject.StringObject script, AfmObject.EcmaArrayObject param) {
        this.command = command;
        this.script = script;
        this.param = param;
    }

    public AfmObject.StringObject getCommand() {
        return command;
    }

    public void setCommand(AfmObject.StringObject command) {
        this.command = command;
    }

    public AfmObject.StringObject getScript() {
        return script;
    }

    public void setScript(AfmObject.StringObject script) {
        this.script = script;
    }

    public AfmObject.EcmaArrayObject getParam() {
        return param;
    }

    public void setParam(AfmObject.EcmaArrayObject param) {
        this.param = param;
    }

    private void decodeBuf(ByteBuf buf) {
        try {
            List<AfmObject> list = AFM_DECODER.decode(buf);
            Iterator<AfmObject> iterator = list.iterator();
            command = (AfmObject.StringObject) iterator.next();
            script = (AfmObject.StringObject) iterator.next();
            param = (AfmObject.EcmaArrayObject) iterator.next();
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void preInitialize() {
        this.messageTypeId = TYPE_ID;
        this.chunkStreamId = 4;
    }

    @Override
    protected void doEncode(ByteBuf out) {
        LinkedList<AfmObject> list = new LinkedList<>();
        list.add(command);
        list.add(script);
        list.add(param);
        AFM_ENCODER.encode(list, out);
    }

    /**
     * 为FLV文件编码元数据，这是为FLV特殊处理过的格式
     * 不会将command放到缓冲区里
     *
     * @param out 输出缓冲区
     */
    public void encodeForFlv(ByteBuf out) {
        LinkedList<AfmObject> list = new LinkedList<>();
        list.add(this.script);
        list.add(param);
        AFM_ENCODER.encode(list, out);
    }

    @Override
    public String toString() {
        return "DataMessage{" +
                "command=" + command.getValue() +
                ", script=" + script.getValue() +
                '}';
    }
}
