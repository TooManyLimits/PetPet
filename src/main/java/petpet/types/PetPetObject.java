package petpet.types;

import petpet.lang.run.PetPetClass;

import java.util.Map;

/**
 * An object that's an instance of a user-defined class
 * Extends table, but this is just a java side trick, doesn't
 * extend table on the petpet side
 */
public class PetPetObject extends PetPetTable<Object, Object> {

    public final PetPetClass clazz; //generally a user defined class

    public PetPetObject(PetPetClass clazz) {
        this.clazz = clazz;
    }

    @Override
    public String toString() {
        return "object(type=" + clazz.name + ")";
    }
}
