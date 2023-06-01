package petpet.lang.run;

import petpet.external.PetPetWhitelist;

@PetPetWhitelist
public abstract class PetPetCallable {
    public abstract Object call(Object... args);
    public abstract Object callInvoking(Object... args);

    @PetPetWhitelist
    public abstract int paramCount();
}
