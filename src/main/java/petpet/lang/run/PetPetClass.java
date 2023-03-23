package main.java.petpet.lang.run;

import main.java.petpet.external.PetPetReflector;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class PetPetClass {
    public final String name;
    public PetPetClass(String name) {
        this.name = name;
        addMethod("type", new JavaFunction(false, 1) {
            @Override
            public Object invoke(Object arg0) {
                return name;
            }
        });
    }
    //method object is JavaFunction or LangClosure
    //currently only JavaFunction, since user-defined classes aren't a thing yet
    public final Map<String, Object> methods = new HashMap<>();
    public final Map<String, Function> fieldGetters = new HashMap<>();
    public final Map<String, BiConsumer> fieldSetters = new HashMap<>();

    public void addMethod(String name, JavaFunction method) {
        methods.put(name, method);
    }

    public void addField(String name, Field field) {
        fieldGetters.put(name, PetPetReflector.unreflectGetter(field));
        if (!Modifier.isFinal(field.getModifiers()))
            fieldSetters.put(name, PetPetReflector.unreflectSetter(field));
    }

}