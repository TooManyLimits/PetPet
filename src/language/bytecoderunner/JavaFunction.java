package language.bytecoderunner;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JavaFunction {
    public final boolean isVoid;
    public final int paramCount;
    private final Backing backing;

    private static final int MAX_PARAMS = 15;

    //boolean is whether this method should be converted to a Masque method
    //if true, it will have an implicit "this" parameter inserted
    public JavaFunction(Method method, boolean isMethod) {
        method.setAccessible(true);
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handle = lookup.unreflect(method);
            isVoid = method.getReturnType() == void.class;
            String methodName = isVoid ? "callVoid" : "callReturning";
            if (isMethod) {
                paramCount = method.getParameterCount() + 1;
                List<Class<?>> ptypes = new ArrayList<>(paramCount);
                for (int i = 0; i < paramCount; i++) ptypes.add(Object.class);
                MethodType samType = isVoid ? MethodType.methodType(void.class, ptypes) : MethodType.methodType(Object.class, ptypes);
                MethodType invocType = (isVoid ? handle.type().wrap().changeReturnType(void.class) : handle.type().wrap());
                MethodHandle site = LambdaMetafactory.metafactory(
                        lookup,
                        methodName, //The name of the method in the class we're implementing
                        MethodType.methodType(Backing.class), //Interface to return
                        samType, //Type of implemented method
                        handle, //A method handle for the function to wrap
                        invocType //The dynamic method type enforced at invocation time
                ).getTarget();
                backing = (Backing) site.invokeExact();
            } else {
                paramCount = method.getParameterCount();
                List<Class<?>> ptypes = new ArrayList<>(paramCount);
                for (int i = 0; i < paramCount; i++) ptypes.add(Object.class);
                MethodType samType = isVoid ? MethodType.methodType(void.class, ptypes) : MethodType.methodType(Object.class, ptypes);
                MethodType invocType = isVoid ? handle.type().wrap().changeReturnType(void.class) : handle.type().wrap();
                backing = (Backing) LambdaMetafactory.metafactory(
                        MethodHandles.lookup(),
                        methodName, //The name of the method in the class we're implementing
                        MethodType.methodType(Backing.class), //Interface to return
                        samType, //Type of implemented method
                        handle, //A method handle for the function to wrap
                        invocType //The dynamic method type enforced at invocation time
                ).getTarget().invokeExact();
            }
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
        if (paramCount > MAX_PARAMS)
            throw new IllegalArgumentException("Cannot create JavaFunction from method with over " + MAX_PARAMS + " params!");
    }

    public JavaFunction(Class<?> clazz, String name, boolean isMethod) {
        this(
                tryFindMethod(clazz, name, (Class<?>[]) null),
                isMethod
        );
    }

    public JavaFunction(Class<?> clazz, String name, boolean isMethod, Class<?>... argTypes) {
        this(
                tryFindMethod(clazz, name, argTypes),
                isMethod
        );
    }

    private static Method tryFindMethod(Class<?> clazz, String name, Class<?>... argTypes) {
        if (argTypes == null) {
            List<Method> methods = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(m -> m.getName().equals(name)).toList();
            if (methods.size() != 1)
                throw new RuntimeException("Ambiguous or incorrect JavaFunction constructor for method " + name);
            return methods.get(0);
        }
        try {
            return clazz.getDeclaredMethod(name, argTypes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Object invoke() {if (isVoid) {backing.callVoid();return null;}return backing.callReturning();}public Object invoke(Object arg0) {if (isVoid) {backing.callVoid(arg0);return null;}return backing.callReturning(arg0);}public Object invoke(Object arg0, Object arg1) {if (isVoid) {backing.callVoid(arg0, arg1);return null;}return backing.callReturning(arg0, arg1);}public Object invoke(Object arg0, Object arg1, Object arg2) {if (isVoid) {backing.callVoid(arg0, arg1, arg2);return null;}return backing.callReturning(arg0, arg1, arg2);}public Object invoke(Object arg0, Object arg1, Object arg2, Object arg3) {if (isVoid) {backing.callVoid(arg0, arg1, arg2, arg3);return null;}return backing.callReturning(arg0, arg1, arg2, arg3);}public Object invoke(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4) {if (isVoid) {backing.callVoid(arg0, arg1, arg2, arg3, arg4);return null;}return backing.callReturning(arg0, arg1, arg2, arg3, arg4);}public Object invoke(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {if (isVoid) {backing.callVoid(arg0, arg1, arg2, arg3, arg4, arg5);return null;}return backing.callReturning(arg0, arg1, arg2, arg3, arg4, arg5);}public Object invoke(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {if (isVoid) {backing.callVoid(arg0, arg1, arg2, arg3, arg4, arg5, arg6);return null;}return backing.callReturning(arg0, arg1, arg2, arg3, arg4, arg5, arg6);}public Object invoke(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {if (isVoid) {backing.callVoid(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);return null;}return backing.callReturning(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);}public Object invoke(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8) {if (isVoid) {backing.callVoid(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);return null;}return backing.callReturning(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);}public Object invoke(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9) {if (isVoid) {backing.callVoid(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);return null;}return backing.callReturning(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);}public Object invoke(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10) {if (isVoid) {backing.callVoid(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);return null;}return backing.callReturning(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);}public Object invoke(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11) {if (isVoid) {backing.callVoid(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11);return null;}return backing.callReturning(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11);}public Object invoke(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12) {if (isVoid) {backing.callVoid(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12);return null;}return backing.callReturning(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12);}public Object invoke(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13) {if (isVoid) {backing.callVoid(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13);return null;}return backing.callReturning(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13);}public Object invoke(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14) {if (isVoid) {backing.callVoid(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14);return null;}return backing.callReturning(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14);}
    public interface Backing {Object callReturning();void callVoid();Object callReturning(Object arg0);void callVoid(Object arg0);Object callReturning(Object arg0, Object arg1);void callVoid(Object arg0, Object arg1);Object callReturning(Object arg0, Object arg1, Object arg2);void callVoid(Object arg0, Object arg1, Object arg2);Object callReturning(Object arg0, Object arg1, Object arg2, Object arg3);void callVoid(Object arg0, Object arg1, Object arg2, Object arg3);Object callReturning(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4);void callVoid(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4);Object callReturning(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);void callVoid(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);Object callReturning(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6);void callVoid(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6);Object callReturning(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7);void callVoid(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7);Object callReturning(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8);void callVoid(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8);Object callReturning(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9);void callVoid(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9);Object callReturning(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10);void callVoid(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10);Object callReturning(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11);void callVoid(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11);Object callReturning(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12);void callVoid(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12);Object callReturning(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13);void callVoid(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13);Object callReturning(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14);void callVoid(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14);}

    public static void generateCode() {
        StringBuilder result = new StringBuilder("\t");
        for (int i = 0; i <= MAX_PARAMS; i++) {
            result.append("public Object invoke(");
            for (int j = 0; j < i; j++) {
                if (j != 0)
                    result.append(", ");
                result.append("Object arg").append(j);
            }
            result.append(") {if (isVoid) {backing.callVoid(");
            for (int j = 0; j < i; j++) {
                if (j != 0)
                    result.append(", ");
                result.append("arg").append(j);
            }
            result.append(");return null;}return backing.callReturning(");
            for (int j = 0; j < i; j++) {
                if (j != 0)
                    result.append(", ");
                result.append("arg").append(j);
            }
            result.append(");}");
        }
        result.append("\n\tpublic interface Backing {");
        for (int i = 0; i <= MAX_PARAMS; i++) {
            result.append("Object callReturning(");
            for (int j = 0; j < i; j++) {
                if (j != 0)
                    result.append(", ");
                result.append("Object arg").append(j);
            }
            result.append(");void callVoid(");
            for (int j = 0; j < i; j++) {
                if (j != 0)
                    result.append(", ");
                result.append("Object arg").append(j);
            }
            result.append(");");
        }
        result.append("}");
        System.out.println(result);
        System.out.println("-----------------Switch Statement----------------");
        result = new StringBuilder("Object result = switch(argCount) {\n");
        for (int i = 0; i <= MAX_PARAMS; i++) {
            result.append("\t\t\t\tcase ").append(i).append(" -> jFunction.invoke(");
            for (int j = i-1; j >= 0; j--) {
                if (j != i-1)
                    result.append(", ");
                if (j == 0)
                    result.append("peek()");
                else
                    result.append("peek(").append(j).append(")");
            }
            result.append(");\n");
        }
        result.append("\t\t\tdefault -> throw new IllegalStateException(\"function has too many args??\");\n");
        result.append("\t\t\t};");
        System.out.println(result);
    }
}


