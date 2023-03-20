package language.run;

import language.Upvalue;

public class PetPetClosure {
    public final PetPetFunction function;
    public final Upvalue[] upvalues;

    public PetPetClosure(PetPetFunction function) {
        this.function = function;
        this.upvalues = new Upvalue[function.numUpvalues];
    }
}
