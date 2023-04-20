package petpet.types;

import petpet.external.PetPetReflector;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.*;
import petpet.types.immutable.PetPetTableView;

import java.util.HashMap;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * Table type
 */
@PetPetWhitelist
public class PetPetTable<K, V> extends HashMap<K, V> {

    public PetPetTable(int initialCapacity) {
        super(initialCapacity);
    }

    public PetPetTable() {}

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

    public static final PetPetClass TABLE_CLASS;

    static {
        TABLE_CLASS = PetPetReflector.reflect(PetPetTable.class, "table");

        TABLE_CLASS.addMethod("__get", new JavaFunction(PetPetTable.class, "get", true));
        TABLE_CLASS.addMethod("__set", new JavaFunction(PetPetTable.class, "put", true));

        TABLE_CLASS.addMethod("del", new JavaFunction(PetPetTable.class, "remove", true, Object.class));
        TABLE_CLASS.addMethod("clear", new JavaFunction(PetPetTable.class, "clear", true));

        //penalties
        ((JavaFunction) TABLE_CLASS.getMethod("each")).costPenalizer = PetPetTable.costPenalty(1);
        ((JavaFunction) TABLE_CLASS.getMethod("eachK")).costPenalizer = PetPetTable.costPenalty(1);
        ((JavaFunction) TABLE_CLASS.getMethod("eachV")).costPenalizer = PetPetTable.costPenalty(1);
    }

    //Penalty function, charging the caller (a small price) for each
    //function call they make through the functional list methods
    private static ToIntFunction<Interpreter> costPenalty(int args) {
        return i -> ((PetPetTable) i.peek(args)).size() * 3;
    }

    private static void checkFunc(PetPetCallable func, int expectedArgs, String name) throws PetPetException {
        if (func.paramCount() != expectedArgs)
            throw new PetPetException("table." + name + "() expects " +
                    expectedArgs + "-arg function, got " + func.paramCount() + "-arg");
    }

    @Override
    public V put(K key, V value) {
        super.put(key, value);
        return value;
    }

    @PetPetWhitelist
    public PetPetTable<K, V> each(PetPetCallable func) {
        checkFunc(func, 2, "each");
        for (Map.Entry<K, V> entry : entrySet())
            func.call(entry.getKey(), entry.getValue());
        return this;
    }

    @PetPetWhitelist
    public PetPetTable<K, V> eachK(PetPetCallable func) {
        checkFunc(func, 1, "eachK");
        for (Object key : keySet())
            func.call(key);
        return this;
    }

    @PetPetWhitelist
    public PetPetTable<K, V> eachV(PetPetCallable func) {
        checkFunc(func, 1, "eachV");
        for (Object value : values())
            func.call(value);
        return this;
    }

    @PetPetWhitelist
    public PetPetTableView<K, V> view() {
        return new PetPetTableView<>(this);
    }

}
