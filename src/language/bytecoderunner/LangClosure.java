package language.bytecoderunner;

import language.Upvalue;

public class LangClosure {
    public final LangFunction function;
    public final Upvalue[] upvalues;

    public LangClosure(LangFunction function) {
        this.function = function;
        this.upvalues = new Upvalue[function.numUpvalues];
    }
}
