package zstreamer.rtmp.message.afm;

import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @author 张贝易
 * 真正编码Afm对象的解码器
 */
public abstract class AfmDecoderDelegate {
    private AfmDecoderDelegate next;
    private AfmDecoderDelegate prev;
    protected AfmDecoderDelegate head;

    public void insertAfter(AfmDecoderDelegate encoder) {
        AfmDecoderDelegate next = encoder.next;
        this.next = next;
        this.prev = encoder;
        encoder.next = this;
        if (next != null) {
            next.prev = this;
        }
    }

    public void insertBefore(AfmDecoderDelegate encoder) {
        AfmDecoderDelegate prev = encoder.prev;
        this.next = encoder;
        this.prev = prev;
        encoder.prev = this;
        if (prev != null) {
            prev.next = this;
        }
    }

    public void setHead(AfmDecoderDelegate head) {
        this.head = head;
    }

    /**
     * 解析buffer中的所有Afm对象
     * 注意: 这里认为整个buffer都是Afm对象
     * 如果buffer中还带有其他内容，需要先切分
     *
     * @param in 需要解析的缓冲区
     * @return 解析出来的Afm对象链表
     */
    public List<AfmObject> decodeAll(ByteBuf in) {
        LinkedList<AfmObject> result = new LinkedList<>();
        while (in.readableBytes() > 0) {
            result.add(decode(in));
        }
        return result;
    }

    /**
     * 解析buffer中下一个Afm对象
     * 注意：这里认为buffer中有该对象的type字段，所以会先解析type字段
     *
     * @param in 缓冲区
     * @return 解析出来的Afm对象
     */
    protected AfmObject decode(ByteBuf in) {
        byte type = in.readByte();
        return decode(in, type);
    }

    /**
     * 模板方法，责任链模式，根据type选择合适的解析器解析下一个Afm对象
     * 注意：这里认为buffer的type字段已经被解析出来了
     *
     * @param in   需要解析的缓冲区
     * @param type 当前需要解析的类型
     * @return 解析出来的Afm对象
     */
    protected AfmObject decode(ByteBuf in, int type) {
        if (supportType(type)) {
            return doDecode(in);
        } else if (next != null) {
            return next.decode(in, type);
        }
        return null;
    }

    /**
     * 子类需要实现的方法，直接解析buffer中的内容
     * 注意：这里认为buffer中的type字段已经被解析过了
     *
     * @param in 缓冲区
     * @return 解析完的Afm对象
     */
    protected abstract AfmObject doDecode(ByteBuf in);

    /**
     * 检测解码器是否支持该类型
     *
     * @param type 类型
     * @return 是否支持
     */
    protected abstract boolean supportType(int type);

    /**
     * =====================各种Afm数据结构的解析器=======================
     */

    public static class NumberDecoder extends AfmDecoderDelegate {

        @Override
        protected AfmObject doDecode(ByteBuf in) {
            return new AfmObject.NumberObject(in.readDouble());
        }

        @Override
        protected boolean supportType(int type) {
            return type == AfmObject.NumberObject.TYPE;
        }
    }

    public static class BooleanDecoder extends AfmDecoderDelegate {

        @Override
        protected AfmObject doDecode(ByteBuf in) {
            return new AfmObject.BooleanObject(in.readByte() == 1);
        }

        @Override
        protected boolean supportType(int type) {
            return type == AfmObject.BooleanObject.TYPE;
        }
    }

    public static class StringDecoder extends AfmDecoderDelegate {

        @Override
        protected AfmObject doDecode(ByteBuf in) {
            //注意Afm用的是一个char一个字节的编码方式
            //它只支持最基础的Ascii
            int len = in.readUnsignedShort();
            StringBuilder builder = new StringBuilder(len);
            for (int i = 0; i < len; i++) {
                builder.append((char) in.readByte());
            }
            return new AfmObject.StringObject(builder.toString());
        }

        @Override
        protected boolean supportType(int type) {
            return type == AfmObject.StringObject.TYPE;
        }
    }

    public static class NullObjectDecoder extends AfmDecoderDelegate {

        @Override
        protected AfmObject doDecode(ByteBuf in) {
            return new AfmObject.NullObject();
        }

        @Override
        protected boolean supportType(int type) {
            return type == AfmObject.NullObject.TYPE;
        }
    }

    public static class ComplexObjectDecoder extends AfmDecoderDelegate {

        @Override
        protected AfmObject doDecode(ByteBuf in) {
            HashMap<String, AfmObject> result = new HashMap<>();
            while (!checkEnd(in)) {
                AfmObject key = head.decode(in, AfmObject.StringObject.TYPE);
                AfmObject value = head.decode(in);
                result.put((String) key.getValue(), value);
            }
            return new AfmObject.ComplexObject(result);

        }

        @Override
        protected boolean supportType(int type) {
            return type == AfmObject.ComplexObject.TYPE;
        }

        private boolean checkEnd(ByteBuf in) {
            in.markReaderIndex();
            if (in.readByte() == 0 && in.readByte() == 0 && in.readByte() == 9) {
                return true;
            }
            in.resetReaderIndex();
            return false;
        }
    }

    public static class EcmaArrayDecoder extends AfmDecoderDelegate {

        @Override
        protected AfmObject doDecode(ByteBuf in) {
            int size = in.readInt();
            AfmObject.EcmaArrayObject.EcmaEntity[] entities = new AfmObject.EcmaArrayObject.EcmaEntity[size];
            for (int i = 0; i < size; i++) {
                AfmObject key = head.decode(in, AfmObject.StringObject.TYPE);
                AfmObject value = head.decode(in);
                entities[i] = new AfmObject.EcmaArrayObject.EcmaEntity((String) key.getValue(), value);
            }
            if (checkEnd(in)) {
                return new AfmObject.EcmaArrayObject(entities);
            } else {
                return null;
            }
        }

        @Override
        protected boolean supportType(int type) {
            return type == AfmObject.EcmaArrayObject.TYPE;
        }

        private boolean checkEnd(ByteBuf in) {
            in.markReaderIndex();
            if (in.readByte() == 0 && in.readByte() == 0 && in.readByte() == 9) {
                return true;
            }
            in.resetReaderIndex();
            return false;
        }
    }
}
