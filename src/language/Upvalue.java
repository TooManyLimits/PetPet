package language;

import java.util.List;

public interface Upvalue {
    Object get();
    void set(Object o);

    class Closed implements Upvalue {
        public Closed(Object val) {
            this.val = val;
        }
        private Object val;
        @Override
        public Object get() {
            return val;
        }
        @Override
        public void set(Object o) {
            val = o;
        }
    }

    class Open implements Upvalue {
        public Open(List<Object> list, int index) {
            this.list = list;
            this.index = index;
        }
        public Closed close() {
            return new Closed(get());
        }
        private final List<Object> list;
        private final int index;
        @Override
        public Object get() {
            return list.get(index);
        }
        @Override
        public void set(Object o) {
            list.set(index, o);
        }
    }
}
