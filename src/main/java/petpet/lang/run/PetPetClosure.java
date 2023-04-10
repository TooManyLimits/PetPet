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
        //Convert numbers to double
        for (int i = 0; i < args.length; i++)
            if (args[i] instanceof Number n)
                args[i] = n.doubleValue();
        //calling a function this way might impose a penalty unfortunately
        //i love randomly deciding on penalties without any testing or reason! yayy
        interpreter.cost += args.length / 4 + 1;
        return interpreter.run(this, args);
    }

    @Override
    public String toString() {
        return "closure(function=" + function + ")";
    }
}
