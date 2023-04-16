package petpet.lang.run;

import petpet.external.PetPetWhitelist;

@PetPetWhitelist
public class PetPetClosure implements PetPetCallable {
    public final PetPetFunction function;
    public final Upvalue[] upvalues;
    public final Interpreter interpreter;

    @PetPetWhitelist
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
        return interpreter.run(this, args);
    }

    @Override
    public String toString() {
        return "closure(function=" + function + ")";
    }

    @PetPetWhitelist
    public String getName() {
        return function.name;
    }
}
