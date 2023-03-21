package main.java.petpet.helpers;

import main.java.petpet.lang.run.JavaFunction;
import main.java.petpet.lang.run.PetPetCallable;
import main.java.petpet.lang.run.PetPetClass;

import java.util.HashMap;
import java.util.Map;

/**
 * Container for code involving the
 * built-in class for the Table type
 */
public class TableClass {

    public static final Class<?> JAVA_CLASS = HashMap.class;
    public static final PetPetClass PETPET_CLASS = new PetPetClass("Table");

    public static final JavaFunction NEW = new JavaFunction(TableClass.class, "newTable", false);

    static {
        PETPET_CLASS.addMethod("__get", new JavaFunction(HashMap.class, "get", true));
        PETPET_CLASS.addMethod("__set", new JavaFunction(HashMap.class, "put", true));

        PETPET_CLASS.addMethod("remove", new JavaFunction(HashMap.class, "remove", true, Object.class));
        PETPET_CLASS.addMethod("clear", new JavaFunction(HashMap.class, "clear", true));

        PETPET_CLASS.addMethod("each", new JavaFunction(TableClass.class, "each", false));
    }

    public static HashMap newTable() {
        return new HashMap();
    }

    public static HashMap each(HashMap<?,?> map, PetPetCallable func) {
        for (Map.Entry<?,?> entry : map.entrySet())
            func.call(entry.getKey(), entry.getValue());
        return map;
    }

}
