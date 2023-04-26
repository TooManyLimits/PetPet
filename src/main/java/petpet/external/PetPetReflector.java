package petpet.external;

import petpet.lang.run.JavaFunction;
import petpet.lang.run.PetPetClass;
import petpet.lang.run.PetPetException;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;


//Cached classes do not have parents, because you generally would
//want to make copies of the classes, and also you wouldn't want
//reference copies of the parents, you'd want deep copies, so you
//would need to set the parents of the copy to be your own copy of
//the superclass which is likely a local variable
public class PetPetReflector {
    //Ensuring that things don't need to be reflected twice to generate the classes
    public static final Map<Class<?>, PetPetClass> CACHE = new IdentityHashMap<>();

    public static PetPetClass reflect(Class<?> clazz, String name) {
        if (!CACHE.containsKey(clazz))
            CACHE.put(clazz, reflectInner(clazz, name));
        return CACHE.get(clazz);
    }

    private static PetPetClass reflectInner(Class<?> clazz, String name) {
        if (!clazz.isAnnotationPresent(PetPetWhitelist.class)) {
            throw new RuntimeException("Can only automatically reflect classes with the @PetPetWhitelist annotation, for safety reasons.");
        }
        PetPetClass result = new PetPetClass(name == null ? clazz.getSimpleName() : name);
        for (Field f : clazz.getFields()) {
            if (f.isAnnotationPresent(PetPetWhitelist.class)) {
                throw new IllegalArgumentException("Cannot whitelist fields - unsupported");
            }
        }
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.isAnnotationPresent(PetPetWhitelist.class)) {
                boolean isMethod = !Modifier.isStatic(m.getModifiers()); //Static whitelisted methods are treated as non-methods
                if (result.methods.containsKey(m.getName()))
                    throw new RuntimeException("Failed to reflect class " + clazz.getName() + ": unfortunately, method overloads are not implemented yet (2 methods with same name) :(");
                result.addMethod(m.getName(), new JavaFunction(m, isMethod));
            }
        }
        return result;
    }

}
