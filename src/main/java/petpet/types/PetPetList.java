package petpet.types;

import petpet.lang.run.Interpreter;
import petpet.lang.run.JavaFunction;
import petpet.lang.run.PetPetCallable;
import petpet.lang.run.PetPetClass;

import java.util.ArrayList;

/**
 * List type
 */
public class PetPetList extends ArrayList<Object> {

    //Equals, hashcode, toString() overwritten
    //Leaving them as-is led to stack overflow crashes
    //with recursive structures
    public PetPetList() {super();}
    public PetPetList(int size) {super(size);}

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
        return "<List(size=" + size() + ")>";
    }

    public static void registerToInterpreter(Interpreter i) {
        i.classMap.put(PetPetList.class, PETPET_CLASS);
    }

    private static final PetPetClass PETPET_CLASS;

    static {
        PETPET_CLASS = new PetPetClass("list");

        //get and set by indices
        PETPET_CLASS.addMethod("__get_num", new JavaFunction(PetPetList.class, "get", true));
        PETPET_CLASS.addMethod("__set_num", new JavaFunction(PetPetList.class, "set", true));

        //other arraylist methods
        PETPET_CLASS.addMethod("push", new JavaFunction(PetPetList.class, "add", true, Object.class));
        PETPET_CLASS.addMethod("insert", new JavaFunction(PetPetList.class, "add", true, int.class, Object.class));
        PETPET_CLASS.addMethod("len", new JavaFunction(PetPetList.class, "size", true));
        PETPET_CLASS.addMethod("empty", new JavaFunction(PetPetList.class, "isEmpty", true));
        PETPET_CLASS.addMethod("remove", new JavaFunction(PetPetList.class, "remove", true, int.class));
        PETPET_CLASS.addMethod("clear", new JavaFunction(PetPetList.class, "clear", true));
        PETPET_CLASS.addMethod("copy", new JavaFunction(PetPetList.class, "clone", true));

        //Our own methods
        PETPET_CLASS.addMethod("take", new JavaFunction(PetPetList.class, "dequeue", false));
        PETPET_CLASS.addMethod("pop", new JavaFunction(PetPetList.class, "pop", false));
        PETPET_CLASS.addMethod("swap", new JavaFunction(PetPetList.class, "swap", false));
        PETPET_CLASS.addMethod("map", new JavaFunction(PetPetList.class, "map", false));
        PETPET_CLASS.addMethod("each", new JavaFunction(PetPetList.class, "each", false));
        PETPET_CLASS.addMethod("eachIndexed", new JavaFunction(PetPetList.class, "eachIndexed", false));
    }

    public static Object pop(ArrayList list) { return list.remove(list.size()-1); }
    public static Object dequeue(ArrayList list) { return list.remove(0); }
    public static ArrayList swap(ArrayList list, int index1, int index2) {
        Object temp = list.get(index1);
        list.set(index1, list.get(index2));
        list.set(index2, temp);
        return list;
    }
    public static ArrayList map(ArrayList list, PetPetCallable func) {
        for (int i = 0; i < list.size(); i++)
            list.set(i, func.call(list.get(i)));
        return list;
    }
    public static ArrayList each(ArrayList list, PetPetCallable func) {
        for (Object o : list)
            func.call(o);
        return list;
    }
    public static ArrayList eachIndexed(ArrayList list, PetPetCallable func) {
        for (int i = 0; i < list.size(); i++)
            func.call((double) i, list.get(i));
        return list;
    }

    @Override
    public Object get(int index) {
        return super.get((index % size() + size()) % size());
    }

    @Override
    public Object set(int index, Object element) {
        return super.set((index % size() + size()) % size(), element);
    }

    @Override
    public Object clone() {
        PetPetList newList = new PetPetList(this.size());
        newList.addAll(this);
        return newList;
    }
}
