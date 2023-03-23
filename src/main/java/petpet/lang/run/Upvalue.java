package petpet.lang.run;

import java.util.List;

public final class Upvalue {
    public Object obj;
    public int idx;
    public Upvalue next; //it is also a linked list node
    public Upvalue(Interpreter interpreter, int index) {
        this.obj = interpreter;
        this.idx = index;
        next = null;
    }
    public Object get() {
        if (idx != -1) return ((Interpreter) obj).get(idx);
        return obj;
    }
    public void set(Object o) {
        if (idx != -1) ((Interpreter) obj).set(idx, o);
        else obj = o;
    }
    public void close() {
        obj = get();
        idx = -1;
    }
}
