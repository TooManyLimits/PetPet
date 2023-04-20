package petpet.lang.run;

import petpet.external.PetPetReflector;
import petpet.external.PetPetWhitelist;
import petpet.types.PetPetList;
import petpet.types.PetPetTable;
import petpet.types.immutable.PetPetTableView;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

@PetPetWhitelist
public class PetPetClass {

    public static final PetPetClass PET_PET_CLASS_CLASS;

    static {
        PET_PET_CLASS_CLASS = PetPetReflector.reflect(PetPetClass.class, "class");

//        ((JavaFunction) PET_PET_CLASS_CLASS.methods.get("fieldInfo")).costPenalizer =
//                i -> ((PetPetClass) i.peek()).fieldGetters.size();
    }

    public final String name;
    public final PetPetClass parent;

    @PetPetWhitelist
    public String name() {return name;}
    @PetPetWhitelist
    public PetPetClass parent() {return parent;}


    private boolean isEditable;

    public PetPetClass(String name, PetPetClass parent) {
        this.name = name;
        this.parent = parent;
        addMethod("class", new JavaFunction(false, 1) {
            @Override
            public Object invoke(Object arg0) {
                return PetPetClass.this;
            }
        });
    }

    public PetPetClass(String name) {
        this(name, null);
    }

    //method object is JavaFunction or LangClosure
    //currently only JavaFunction, since user-defined classes aren't a thing yet
    public final PetPetTable<String, PetPetCallable> methods = new PetPetTable<>();
//    public final PetPetTable<String, Function> fieldGetters = new PetPetTable<>();
//    public final PetPetTable<String, BiConsumer> fieldSetters = new PetPetTable<>();

    public Object getMethod(String name) {
        Object result = methods.get(name);
        if (result == null)
            return parent == null ? null : parent.getMethod(name);
        return result;
    }

    public boolean doesExtend(PetPetClass possibleParent) {
        PetPetClass cur = this;
        while (cur != null) {
            if (cur == possibleParent)
                return true;
            cur = cur.parent;
        }
        return false;
    }

    public void addMethod(String name, PetPetCallable method) {
        methods.put(name, method);
    }

    public PetPetClass copy() {
        PetPetClass newClass = new PetPetClass(name);
        newClass.methods.putAll(methods);
        newClass.addMethod("class", new JavaFunction(false, 1) {
            @Override
            public Object invoke(Object arg0) {
                return newClass;
            }
        });
        return newClass;
    }

    @PetPetWhitelist
    public PetPetTable<String, PetPetCallable> methods() {
        return isEditable ? methods : new PetPetTableView<>(methods);
    }

    /**
     * Make sure not to call this on classes shared between scripts!
     * copy() them first!
     */
    public PetPetClass makeEditable() {
        isEditable = true;
        return this;
    }

    @Override
    public String toString() {
        return "class(name=" + name + ")";
    }
}