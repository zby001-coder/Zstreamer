package zstreamer.rtmp.message.afm;

import io.netty.buffer.ByteBuf;

import java.util.List;

/**
 * @author 张贝易
 * Afm对象的解码工具
 * 内部维护一个双向链表，链表内容为各种类型的Afm对象的解码器
 * 整个解码工具使用责任链模式
 * @see AfmDecoderDelegate
 */
public class AfmDecoder {
    private AfmDecoderDelegate head;
    private AfmDecoderDelegate tail;

    public AfmDecoder() {
        //创建各种解码器
        AfmDecoderDelegate[] decoderDelegates = new AfmDecoderDelegate[]{
                new AfmDecoderDelegate.NumberDecoder(),
                new AfmDecoderDelegate.BooleanDecoder(),
                new AfmDecoderDelegate.StringDecoder(),
                new AfmDecoderDelegate.ComplexObjectDecoder(),
                new AfmDecoderDelegate.EcmaArrayDecoder(),
                new AfmDecoderDelegate.NullObjectDecoder()};
        head = decoderDelegates[0];
        tail = decoderDelegates[decoderDelegates.length - 1];
        //创建解码器的责任链
        for (AfmDecoderDelegate decoderDelegate : decoderDelegates) {
            decoderDelegate.setHead(decoderDelegates[0]);
        }
        for (int i = 1; i < decoderDelegates.length; i++) {
            decoderDelegates[i].insertAfter(decoderDelegates[i - 1]);
        }
    }

    public List<AfmObject> decode(ByteBuf in) {
        return head.decodeAll(in);
    }
}
