package zstreamer.rtmp.message.messageType.command;

import zstreamer.rtmp.message.afm.AfmEncoder;
import zstreamer.rtmp.message.afm.AfmObject;
import zstreamer.rtmp.message.messageType.RtmpMessage;
import io.netty.buffer.ByteBuf;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author 张贝易
 * 命令类型的Message，对应type为20
 * 消息体由一个string类型的command、一个number类型的transactionId、不定个Afm对象组成
 * @see AfmEncoder
 * @see AfmEncoder
 */
public class CommandMessage extends RtmpMessage {
    public static final byte TYPE_ID = 20;
    /**
     * 各种命令的字符串
     */
    public static final String CONNECT = "connect";
    public static final String RESULT = "_result";
    public static final String ON_STATUS = "onStatus";
    public static final String RELEASE = "releaseStream";
    public static final String FCP_PUBLISH = "FCPublish";
    public static final String CREATE_STREAM = "createStream";
    public static final String PUBLISH = "publish";
    public static final String FC_UNPUBLISH = "FCUnpublish";
    public static final String DELETE_STREAM = "deleteStream";

    private AfmObject.StringObject command;
    private AfmObject.NumberObject transactionId;
    private List<AfmObject> params;

    public CommandMessage(ByteBuf buf) {
        //由于这个类型的message必须在已知byteBuf的情况下才能设置长度，所以无法在preInitialize中直接写，要在构造中执行
        this.messageLength = buf.readableBytes();
        decodeBuf(buf);
    }

    public CommandMessage(AfmObject.StringObject command, AfmObject.NumberObject transactionId, List<AfmObject> params) {
        this.command = command;
        this.transactionId = transactionId;
        this.params = params;
    }

    public CommandMessage(RtmpMessage message, ByteBuf buf) {
        super(message);
        this.messageLength = buf.readableBytes();
        decodeBuf(buf);
    }

    public CommandMessage(RtmpMessage message, AfmObject.StringObject command,
                          AfmObject.NumberObject transactionId, List<AfmObject> params) {
        super(message);
        this.command = command;
        this.transactionId = transactionId;
        this.params = params;
    }

    @Override
    protected void preInitialize() {
        this.messageTypeId = TYPE_ID;
        this.chunkStreamId = 3;
    }

    private void decodeBuf(ByteBuf buf) {
        try {
            List<AfmObject> list = AFM_DECODER.decode(buf);
            Iterator<AfmObject> iterator = list.iterator();
            command = (AfmObject.StringObject) iterator.next();
            transactionId = (AfmObject.NumberObject) iterator.next();
            params = new LinkedList<>();
            while (iterator.hasNext()) {
                params.add(iterator.next());
            }
        } catch (Exception ignored) {
        }
    }

    public void setCommand(AfmObject.StringObject command) {
        this.command = command;
    }

    public void setTransactionId(AfmObject.NumberObject transactionId) {
        this.transactionId = transactionId;
    }

    public List<AfmObject> getParams() {
        return params;
    }

    public void setParams(List<AfmObject> params) {
        this.params = params;
    }

    public AfmObject.StringObject getCommand() {
        return command;
    }

    public AfmObject.NumberObject getTransactionId() {
        return transactionId;
    }

    @Override
    protected void doEncode(ByteBuf out) {
        LinkedList<AfmObject> list = new LinkedList<>();
        list.add(command);
        list.add(transactionId);
        list.addAll(params);
        AFM_ENCODER.encode(list, out);
    }

    @Override
    public String toString() {
        return "CommandMessage{" +
                "command=" + command.getValue() +
                '}';
    }
}