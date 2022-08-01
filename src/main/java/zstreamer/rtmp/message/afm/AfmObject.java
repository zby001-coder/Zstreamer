package zstreamer.rtmp.message.afm;

import java.util.HashMap;

/**
 * @author 张贝易
 * adobe的flash中的Afm对象
 */
public abstract class AfmObject {
    private static final Exception WRONG_TYPE_ERROR = new Exception("WRONG_AFM_TYPE");
    private final Object value;

    public AfmObject(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    /**
     * 获取当前Afm对象的类型
     *
     * @return 对象类型
     */
    public abstract int getType();

    public static class NumberObject extends AfmObject {
        public static final int TYPE = 0;

        public NumberObject(Double value) {
            super(value);
        }

        @Override
        public int getType() {
            return TYPE;
        }
    }

    public static class BooleanObject extends AfmObject {
        public static final int TYPE = 1;

        public BooleanObject(Boolean value) {
            super(value);
        }

        @Override
        public int getType() {
            return TYPE;
        }
    }

    public static class StringObject extends AfmObject {
        public static final int TYPE = 2;

        public StringObject(String value) {
            super(value);
        }

        @Override
        public int getType() {
            return TYPE;
        }
    }

    public static class NullObject extends AfmObject {
        public static final int TYPE = 5;

        public NullObject() {
            super(null);
        }

        @Override
        public int getType() {
            return TYPE;
        }
    }

    public static class ComplexObject extends AfmObject {
        public static final int TYPE = 3;

        public ComplexObject(HashMap<String, AfmObject> value) {
            super(value);
        }

        @Override
        public int getType() {
            return TYPE;
        }
    }

    public static class UndefObject extends AfmObject {
        public static final int TYPE = 6;

        public UndefObject(Object value) throws Exception {
            super(value);
            if (value != null) {
                throw WRONG_TYPE_ERROR;
            }
        }

        @Override
        public int getType() {
            return TYPE;
        }
    }

    public static class ReferenceObject extends AfmObject {
        public static final int TYPE = 7;

        public ReferenceObject(Number value) {
            super(value);
        }

        @Override
        public int getType() {
            return TYPE;
        }
    }

    public static class EcmaArrayObject extends AfmObject {
        static class EcmaEntity {
            private final String key;
            private final AfmObject value;

            public EcmaEntity(String key, AfmObject value) {
                this.key = key;
                this.value = value;
            }

            public String getKey() {
                return key;
            }

            public AfmObject getValue() {
                return value;
            }
        }

        public static final int TYPE = 8;

        public EcmaArrayObject(EcmaEntity[] value){
            super(value);
        }

        @Override
        public int getType() {
            return TYPE;
        }
    }

    public static class ArrayObject extends AfmObject {
        public static final int TYPE = 10;

        public ArrayObject(AfmObject[] value){
            super(value);
        }

        @Override
        public int getType() {
            return TYPE;
        }
    }

    public static class LongStringObject extends AfmObject {
        public static final int TYPE = 12;

        public LongStringObject(Object value) throws Exception {
            super(value);
            if (!(value instanceof String)) {
                throw WRONG_TYPE_ERROR;
            }
        }

        @Override
        public int getType() {
            return TYPE;
        }
    }
}
