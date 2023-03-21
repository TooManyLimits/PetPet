package main.java.petpet.lang.run;

import java.util.List;

public final class Upvalue {
    public Object obj;
    public int idx;
    public Upvalue next; //it is also a linked list node
    public Upvalue(List<Object> obj, int index) {
        this.obj = obj;
        this.idx = index;
        next = null;
    }
    public Object get() {
        if (idx != -1) return ((List<Object>) obj).get(idx);
        return obj;
    }
    public void set(Object o) {
        if (idx != -1) ((List<Object>) obj).set(idx, o);
        else obj = o;
    }
    public void close() {
        obj = get();
        idx = -1;
    }
}