package petpet.types;

import petpet.lang.run.Interpreter;
import petpet.lang.run.JavaFunction;
import petpet.lang.run.PetPetCallable;
import petpet.lang.run.PetPetClass;

import java.util.HashMap;
import java.util.Map;

/**
 * Table type
 */
public class PetPetTable<K, V> extends HashMap<K, V> {

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
    }

    private static final Class<?> JAVA_CLASS = PetPetTable.class;
    private static final PetPetClass PETPET_CLASS = new PetPetClass("table");

    static {
        PETPET_CLASS.addMethod("__get", new JavaFunction(PetPetTable.class, "get", true));
        PETPET_CLASS.addMethod("__set", new JavaFunction(PetPetTable.class, "put", true));

        PETPET_CLASS.addMethod("remove", new JavaFunction(PetPetTable.class, "remove", true, Object.class));
        PETPET_CLASS.addMethod("clear", new JavaFunction(PetPetTable.class, "clear", true));

        PETPET_CLASS.addMethod("each", new JavaFunction(PetPetTable.class, "each", false));
        PETPET_CLASS.addMethod("eachKey", new JavaFunction(PetPetTable.class, "eachKey", false));
        PETPET_CLASS.addMethod("eachValue", new JavaFunction(PetPetTable.class, "eachValue", false));
    }

    public static <K, V> PetPetTable<K, V> each(PetPetTable<K, V> map, PetPetCallable func) {
        for (Map.Entry<K, V> entry : map.entrySet())
            func.call(entry.getKey(), entry.getValue());
        return map;
    }

    public static <K, V> PetPetTable<K, V> eachKey(PetPetTable<K, V> map, PetPetCallable func) {
        for (Object key : map.keySet())
            func.call(key);
        return map;
    }

    public static <K, V> PetPetTable<K, V> eachValue(PetPetTable<K, V> map, PetPetCallable func) {
        for (Object value : map.values())
            func.call(value);
        return map;
    }

}
