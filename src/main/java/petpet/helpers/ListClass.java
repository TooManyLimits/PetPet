package main.java.petpet.helpers;

import main.java.petpet.lang.run.JavaFunction;
import main.java.petpet.lang.run.PetPetClass;
import main.java.petpet.lang.run.PetPetClosure;

import java.util.ArrayList;

/**
 * File containing code involving the
 * built-in class for the List type
 */
public class ListClass {

    public static final Class<?> JAVA_CLASS = ArrayList.class;
    public static final PetPetClass PETPET_CLASS;

    public static final JavaFunction NEW_LIST = new JavaFunction(ListClass.class, "newList", false);

    static {
        PETPET_CLASS = new PetPetClass("List");

        //get and set by indices
        PETPET_CLASS.addMethod("__get_num", new JavaFunction(JAVA_CLASS, "get", true));
        PETPET_CLASS.addMethod("__set_num", new JavaFunction(JAVA_CLASS, "set", true));

        //other arraylist methods
        PETPET_CLASS.addMethod("size", new JavaFunction(JAVA_CLASS, "size", true));
        JavaFunction add = new JavaFunction(JAVA_CLASS, "add", true, Object.class);
        PETPET_CLASS.addMethod("add", add);
        PETPET_CLASS.addMethod("push", add);
        PETPET_CLASS.addMethod("enqueue", add);
        PETPET_CLASS.addMethod("grow", new JavaFunction(JAVA_CLASS, "ensureCapacity", true));
        PETPET_CLASS.addMethod("empty", new JavaFunction(JAVA_CLASS, "isEmpty", true));
        PETPET_CLASS.addMethod("remove", new JavaFunction(JAVA_CLASS, "remove", true, int.class));
        PETPET_CLASS.addMethod("clear", new JavaFunction(JAVA_CLASS, "clear", true));
        PETPET_CLASS.addMethod("copy", new JavaFunction(JAVA_CLASS, "clone", true));

        //Our own methods
        PETPET_CLASS.addMethod("dequeue", new JavaFunction(ListClass.class, "dequeue", false));
        PETPET_CLASS.addMethod("pop", new JavaFunction(ListClass.class, "pop", false));
        PETPET_CLASS.addMethod("swap", new JavaFunction(ListClass.class, "swap", false));
        PETPET_CLASS.addMethod("map", new JavaFunction(ListClass.class, "map", false));
    }

    public static ArrayList newList() {
        return new ArrayList();
    }

    public static Object pop(ArrayList list) { return list.remove(list.size()-1); }
    public static Object dequeue(ArrayList list) { return list.remove(0); }
    public static ArrayList swap(ArrayList list, int index1, int index2) {
        Object temp = list.get(index1);
        list.set(index1, list.get(index2));
        list.set(index2, temp);
        return list;
    }
    public static ArrayList map(ArrayList list, PetPetClosure func) {
        for (int i = 0; i < list.size(); i++)
            list.set(i, func.call(list.get(i)));
        return list;
    }

}
