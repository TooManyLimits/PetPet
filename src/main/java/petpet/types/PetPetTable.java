package main.java.petpet.types;

import main.java.petpet.lang.run.Interpreter;
import main.java.petpet.lang.run.JavaFunction;
import main.java.petpet.lang.run.PetPetCallable;
import main.java.petpet.lang.run.PetPetClass;

import java.util.HashMap;
import java.util.Map;

/**
 * Table type
 */
public class PetPetTable extends HashMap<Object, Object> {

    //Equals, hashcode, toString() overwritten
    //Leaving them as-is led to stack overflow crashes
    //with recursive structures
    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "<table(entries=" + size() + ")>";
    }

    public static void registerToInterpreter(Interpreter i) {
        i.classMap.put(JAVA_CLASS, PETPET_CLASS);
        i.globals.put("table", NEW);
    }

    private static final Class<?> JAVA_CLASS = PetPetTable.class;
    private static final PetPetClass PETPET_CLASS = new PetPetClass("table");

    private static final JavaFunction NEW = new JavaFunction(PetPetTable.class, "newTable", false);

    static {
        PETPET_CLASS.addMethod("__get", new JavaFunction(PetPetTable.class, "get", true));
        PETPET_CLASS.addMethod("__set", new JavaFunction(PetPetTable.class, "put", true));

        PETPET_CLASS.addMethod("remove", new JavaFunction(PetPetTable.class, "remove", true, Object.class));
        PETPET_CLASS.addMethod("clear", new JavaFunction(PetPetTable.class, "clear", true));

        PETPET_CLASS.addMethod("each", new JavaFunction(PetPetTable.class, "each", false));
        PETPET_CLASS.addMethod("eachKey", new JavaFunction(PetPetTable.class, "eachKey", false));
        PETPET_CLASS.addMethod("eachValue", new JavaFunction(PetPetTable.class, "eachValue", false));
    }

    public static HashMap newTable() {
        return new PetPetTable();
    }

    public static HashMap each(HashMap<?,?> map, PetPetCallable func) {
        for (Map.Entry<?,?> entry : map.entrySet())
            func.call(entry.getKey(), entry.getValue());
        return map;
    }

    public static HashMap eachKey(HashMap<?,?> map, PetPetCallable func) {
        for (Object key : map.keySet())
            func.call(key);
        return map;
    }

    public static HashMap eachValue(HashMap<?,?> map, PetPetCallable func) {
        for (Object value : map.values())
            func.call(value);
        return map;
    }

}
