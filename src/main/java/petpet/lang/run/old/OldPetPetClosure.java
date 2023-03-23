package main.java.petpet.lang.run.old;

import main.java.petpet.lang.run.PetPetCallable;
import main.java.petpet.lang.run.PetPetFunction;

public class OldPetPetClosure implements PetPetCallable {
    public final PetPetFunction function;
    public final OldUpvalue[] upvalues;
    public final OldInterpreter interpreter;

    public OldPetPetClosure(PetPetFunction function, OldInterpreter interpreter) {
        this.function = function;
        this.upvalues = new OldUpvalue[function.numUpvalues];
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
