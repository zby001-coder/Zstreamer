package zstreamer.rtmp.message.afm;

import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 张贝易
 * 真正编码Afm对象的编码器
 */
public abstract class AfmEncoderDelegate {
    private AfmEncoderDelegate next;
    private AfmEncoderDelegate prev;
    protected AfmEncoderDelegate head;

    public void insertAfter(AfmEncoderDelegate encoder) {
        AfmEncoderDelegate next = encoder.next;
        this.next = next;
        this.prev = encoder;
        encoder.next = this;
        if (next != null) {
            next.prev = this;
        }
    }

    public void insertBefore(AfmEncoderDelegate encoder) {
        AfmEncoderDelegate prev = encoder.prev;
        this.next = encoder;
        this.prev = prev;
        encoder.prev = this;
        if (prev != null) {
            prev.next = this;
        }
    }

    public void setHead(AfmEncoderDelegate head) {
        this.head = head;
    }

    /**
     * 模板方法，责任链模式
     * 选择某一个支持该类型的编码器去编码然后写入缓冲区
     *
     * @param object  需要解析的对象
     * @param out     缓冲区
     * @param addType 是否添加对象类型的那一个字节
     * @return 是否解析成功
     */
    public boolean encode(AfmObject object, ByteBuf out, boolean addType) {
        if (supportType(object)) {
            if (addType) {
                //如果需要添加Type，就给它写一次
                out.writeByte(object.getType());
            }
            return doEncode(object, out);
        } else if (next != null) {
            return next.encode(object, out, addType);
        }
        return false;

    }

    /**
     * 对Afm对象编码的真正逻辑，子类需要实现的
     * 此时的type已经确定，并且写入buffer过了
     *
     * @param object afm对象
     * @param out    缓冲区
     * @return 返回编码是否成功
     */
    protected abstract boolean doEncode(AfmObject object, ByteBuf out);

    /**
     * 检测该encoder是否支持这个Object
     * @param object 需要编码的对象
     * @return 是否支持
     */
    protected abstract boolean supportType(AfmObject object);

    /**
     * =========================真正的编码器=============================
     */
    public static class NumberEncoder extends AfmEncoderDelegate {
        @Override
        public boolean doEncode(AfmObject object, ByteBuf out) {
            out.writeDouble((Double) object.getValue());
            return true;
        }

        @Override
        protected boolean supportType(AfmObject object) {
            return object instanceof AfmObject.NumberObject;
        }
    }

    public static class BooleanEncoder extends AfmEncoderDelegate {

        @Override
        public boolean doEncode(AfmObject object, ByteBuf out) {
            out.writeByte((Boolean) object.getValue() ? 1 : 0);
            return true;
        }

        @Override
        protected boolean supportType(AfmObject object) {
            return object instanceof AfmObject.BooleanObject;
        }
    }

    public static class StringEncoder extends AfmEncoderDelegate {

        @Override
        protected boolean doEncode(AfmObject object, ByteBuf out) {
            //注意Afm用的是一个char一个字节的编码方式
            //它只支持最基础的Ascii
            String value = (String) object.getValue();
            out.writeShort(value.length());
            for (int i = 0; i < value.length(); i++) {
                out.writeByte((byte) value.charAt(i));
            }
            return true;
        }

        @Override
        protected boolean supportType(AfmObject object) {
            return object instanceof AfmObject.StringObject;
        }
    }

    public static class ComplexObjectEncoder extends AfmEncoderDelegate {

        @Override
        protected boolean doEncode(AfmObject object, ByteBuf out) {
            HashMap<String, AfmObject> map = (HashMap<String, AfmObject>) object.getValue();
            for (Map.Entry<String, AfmObject> entry : map.entrySet()) {
                //注意Afm的Object类型数据使用key-value形式保存
                //而且它的key必然是string，所以不带type的那一个byte
                head.encode(new AfmObject.StringObject(entry.getKey()), out, false);
                head.encode(entry.getValue(), out, true);
            }
            writeEnd(out);
            return true;
        }

        @Override
        protected boolean supportType(AfmObject object) {
            return object instanceof AfmObject.ComplexObject;
        }

        private void writeEnd(ByteBuf out) {
            out.writeBytes(new byte[]{0, 0, 9});
        }
    }

    public static class NullObjectEncoder extends AfmEncoderDelegate{

        @Override
        protected boolean doEncode(AfmObject object, ByteBuf out) {
            return true;
        }

        @Override
        protected boolean supportType(AfmObject object) {
            return object instanceof AfmObject.NullObject;
        }
    }

    public static class EcmaArrayEncoder extends AfmEncoderDelegate {

        @Override
        protected boolean doEncode(AfmObject object, ByteBuf out) {
            AfmObject.EcmaArrayObject.EcmaEntity[] ecmEntities = (AfmObject.EcmaArrayObject.EcmaEntity[]) object.getValue();
            out.writeInt(ecmEntities.length);
            for (AfmObject.EcmaArrayObject.EcmaEntity ecmEntity : ecmEntities) {
                head.encode(new AfmObject.StringObject(ecmEntity.getKey()), out, false);
                head.encode(ecmEntity.getValue(), out, true);
            }
            writeEnd(out);
            return true;
        }

        @Override
        protected boolean supportType(AfmObject object) {
            return object instanceof AfmObject.EcmaArrayObject;
        }

        private void writeEnd(ByteBuf out) {
            out.writeBytes(new byte[]{0, 0, 9});
        }
    }
}
