package petpet.lang.run;

import petpet.external.PetPetReflector;
import petpet.external.PetPetWhitelist;
import petpet.types.PetPetList;
import petpet.types.PetPetTable;
import petpet.types.immutable.PetPetTableView;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

@PetPetWhitelist
public class PetPetClass {

    public static final PetPetClass PET_PET_CLASS_CLASS;

    static {
        PET_PET_CLASS_CLASS = PetPetReflector.reflect(PetPetClass.class, "class");

        ((JavaFunction) PET_PET_CLASS_CLASS.methods.get("fieldInfo")).costPenalizer =
                i -> ((PetPetClass) i.peek()).fieldGetters.size();
    }

    @PetPetWhitelist
    public final String name;

    private boolean isEditable;

    public PetPetClass(String name) {
        this.name = name;
        addMethod("type", new JavaFunction(false, 1) {
            @Override
            public Object invoke(Object arg0) {
                return name;
            }
        });
        addMethod("class", new JavaFunction(false, 1) {
            @Override
            public Object invoke(Object arg0) {
                return PetPetClass.this;
            }
        });
    }
    //method object is JavaFunction or LangClosure
    //currently only JavaFunction, since user-defined classes aren't a thing yet
    public final PetPetTable<String, PetPetCallable> methods = new PetPetTable<>();
    public final PetPetTable<String, Function> fieldGetters = new PetPetTable<>();
    public final PetPetTable<String, BiConsumer> fieldSetters = new PetPetTable<>();

    public void addMethod(String name, PetPetCallable method) {
        methods.put(name, method);
    }

    public void addField(String name, Field field, boolean forceImmutable) {
        fieldGetters.put(name, PetPetReflector.unreflectGetter(field));
        if (!forceImmutable && !Modifier.isFinal(field.getModifiers()))
            fieldSetters.put(name, PetPetReflector.unreflectSetter(field));
    }

    public PetPetClass copy() {
        PetPetClass newClass = new PetPetClass(name);
        newClass.methods.putAll(methods);
        newClass.fieldGetters.putAll(fieldGetters);
        newClass.fieldSetters.putAll(fieldSetters);
        return newClass;
    }

    @PetPetWhitelist
    public PetPetTable<String, PetPetCallable> methods() {
        return isEditable ? methods : new PetPetTableView<>(methods);
    }

    @PetPetWhitelist
    public PetPetTable<String, Boolean> fieldInfo() {
        PetPetTable<String, Boolean> fields = new PetPetTable<>();
        for (Map.Entry<String, Function> field : fieldGetters.entrySet()) {
            fields.put(field.getKey(), fieldSetters.containsKey(field.getKey()));
        }
        return fields;
    }

    /**
     * Make sure not to call this on classes shared between scripts!
     * copy() them first!
     */
    public PetPetClass makeEditable() {
        isEditable = true;
        return this;
    }

}