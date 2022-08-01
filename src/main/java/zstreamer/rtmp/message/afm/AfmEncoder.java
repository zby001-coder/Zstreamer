package zstreamer.rtmp.message.afm;

import io.netty.buffer.ByteBuf;

import java.util.List;

/**
 * @author 张贝易
 * Afm对象的编码工具
 * 内部维护一个双向链表，链表内容为各种类型的Afm对象的编码器
 * 整个编码工具使用责任链模式
 */
public class AfmEncoder {
    private AfmEncoderDelegate head;
    private AfmEncoderDelegate tail;

    public AfmEncoder() {
        //创建各种编码器
        AfmEncoderDelegate[] encoderDelegates = new AfmEncoderDelegate[]{
                new AfmEncoderDelegate.NumberEncoder(),
                new AfmEncoderDelegate.BooleanEncoder(),
                new AfmEncoderDelegate.StringEncoder(),
                new AfmEncoderDelegate.ComplexObjectEncoder(),
                new AfmEncoderDelegate.NullObjectEncoder(),
                new AfmEncoderDelegate.EcmaArrayEncoder()};
        head = encoderDelegates[0];
        tail = encoderDelegates[encoderDelegates.length - 1];
        //创建编码器的责任链
        for (AfmEncoderDelegate encoderDelegate : encoderDelegates) {
            encoderDelegate.setHead(encoderDelegates[0]);
        }
        for (int i = 1; i < encoderDelegates.length; i++) {
            encoderDelegates[i].insertAfter(encoderDelegates[i - 1]);
        }
    }

    public void encode(List<AfmObject> afmObject, ByteBuf out) {
        for (AfmObject object : afmObject) {
            head.encode(object, out, true);
        }
    }
}
