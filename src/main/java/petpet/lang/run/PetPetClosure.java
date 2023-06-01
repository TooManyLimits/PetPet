package petpet.lang.run;

public class PetPetClosure extends PetPetCallable {
    public final PetPetFunction function;
    public final Upvalue[] upvalues;
    public final Interpreter interpreter;

    public final int paramCount;

    @Override
    public int paramCount() {
        return paramCount;
    }

    public PetPetClosure(PetPetFunction function, Interpreter interpreter) {
        this.function = function;
        this.upvalues = new Upvalue[function.numUpvalues];
        this.interpreter = interpreter;
        paramCount = function.paramCount;
    }

    public Object call(Object... args) {
        //Convert numbers to double
        for (int i = 0; i < args.length; i++)
            if (args[i] instanceof Number n)
                args[i] = n.doubleValue();
        return interpreter.run(this, false, args);
    }

    @Override
    public Object callInvoking(Object... args) { //same as otherwise, change boolean variable
        //Convert numbers to double
        for (int i = 0; i < args.length; i++)
            if (args[i] instanceof Number n)
                args[i] = n.doubleValue();
        return interpreter.run(this, true, args);
    }

    @Override
    public String toString() {
        return "Function[" + function.name + "]";
    }

}
