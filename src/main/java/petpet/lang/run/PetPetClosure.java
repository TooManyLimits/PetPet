package petpet.lang.run;

public class PetPetClosure implements PetPetCallable {
    public final PetPetFunction function;
    public final Upvalue[] upvalues;
    public final Interpreter interpreter;

    @Override
    public int paramCount() {
        return function.paramCount;
    }

    public PetPetClosure(PetPetFunction function, Interpreter interpreter) {
        this.function = function;
        this.upvalues = new Upvalue[function.numUpvalues];
        this.interpreter = interpreter;
    }

    public Object call(Object... args) {
        //calling a function this way might impose a great penalty unfortunately
        interpreter.cost += args.length + 2;
        return interpreter.run(this, args);
    }

    @Override
    public String toString() {
        return "closure(function=" + function + ")";
    }
}
