package petpet.external;

import petpet.lang.run.JavaFunction;
import petpet.lang.run.PetPetClass;
import petpet.lang.run.PetPetException;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

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
                result.addField(f.getName(), f, f.getAnnotation(PetPetWhitelist.class).forceImmutable());
            }
        }
        for (Method m : clazz.getMethods()) {
            if (m.isAnnotationPresent(PetPetWhitelist.class)) {
                if (result.methods.containsKey(m.getName()))
                    throw new RuntimeException("Failed to reflect class " + clazz.getName() + ": unfortunately, method overloads are not implemented yet :(");
                result.methods.put(m.getName(), new JavaFunction(m, true));
            }
        }
        return result;
    }

    //LambdaMetafactory "doesn't currently support" getters/setters, so we use method handles like chumps
    public static Function<Object, Object> unreflectGetter(Field f) {
        try {
            f.setAccessible(true);
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handle = lookup.unreflectGetter(f);
            return a -> {
                try {
                    return handle.invoke(a);
                } catch (WrongMethodTypeException | ClassCastException e) {
                    throw new RuntimeException("Failed to get object: this shouldn't ever happen. Contact devs with your script so we can fix it!");
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            };
        } catch (Throwable t) {
            throw new RuntimeException("Failed to unreflect getter for field " + f.getName() + " in " + f.getDeclaringClass().getName(), t);
        }
    }

    public static BiConsumer<Object, Object> unreflectSetter(Field f) {
        try {
            f.setAccessible(true);
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle primHandle = lookup.unreflectSetter(f);
            //wrap primitives
            MethodHandle handle = primHandle.asType(primHandle.type().wrap().changeReturnType(void.class));

            Class<?> fieldType = handle.type().lastParameterType();
            if (Number.class.isAssignableFrom(fieldType)) {
                if (fieldType == Float.class)
                    return (a, b) -> setterInvokeHelper(handle, a, ((Number) b).floatValue(), f.getName());
                else if (fieldType == Long.class)
                    return (a, b) -> setterInvokeHelper(handle, a, ((Number) b).longValue(), f.getName());
                else if (fieldType == Integer.class)
                    return (a, b) -> setterInvokeHelper(handle, a, ((Number) b).intValue(), f.getName());
                else if (fieldType == Short.class)
                    return (a, b) -> setterInvokeHelper(handle, a, ((Number) b).shortValue(), f.getName());
                else if (fieldType == Byte.class)
                    return (a, b) -> setterInvokeHelper(handle, a, ((Number) b).byteValue(), f.getName());
            }

            return (a, b) -> setterInvokeHelper(handle, a, b, f.getName());
        } catch (Throwable t) {
            throw new RuntimeException("Failed to unreflect setter for field " + f.getName() + " in " + f.getDeclaringClass().getName(), t);
        }
    }

    private static void setterInvokeHelper(MethodHandle handle, Object a, Object b, String fieldName) {
        try {
            handle.invoke(a, b);
        } catch (WrongMethodTypeException | ClassCastException e) {
            String message = String.format(
                    "Attempt to set on invalid type: expected %s.%s = %s, got %s.%s = %s",
                    handle.type().parameterType(0).getSimpleName(),
                    fieldName,
                    handle.type().parameterType(1).getSimpleName(),
                    a.getClass().getSimpleName(),
                    fieldName,
                    b.getClass().getSimpleName()
            );
            throw new PetPetException(message, e);
        } catch (Exception e) {
            throw new PetPetException(e);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

}
