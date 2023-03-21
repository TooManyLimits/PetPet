package main.java.petpet.lang.run;

public class PetPetClosure implements PetPetCallable {
    public final PetPetFunction function;
    public final Upvalue[] upvalues;
    public final Interpreter interpreter;

    public PetPetClosure(PetPetFunction function, Interpreter interpreter) {
        this.function = function;
        this.upvalues = new Upvalue[function.numUpvalues];
        this.interpreter = interpreter;
    }

    public Object call(Object... args) {
        return interpreter.run(this, args);
    }

    @Override
    public String toString() {
        return "closure(function=" + function + ")";
    }
}
