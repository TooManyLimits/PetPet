package petpet.types;

import petpet.external.PetPetReflector;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.Interpreter;
import petpet.lang.run.JavaFunction;
import petpet.lang.run.PetPetClass;

import java.util.ArrayList;
import java.util.function.ToIntFunction;

@PetPetWhitelist
public class PetPetString {

    public static final PetPetClass STRING_CLASS;

    static {
        STRING_CLASS = PetPetReflector.reflect(PetPetString.class, "str");

        STRING_CLASS.addMethod("__get_num", new JavaFunction(PetPetString.class, "charAt", false));

        //i love arbitrarily guessing on cost penalties with no benchmarking whatsoever ! :D

        STRING_CLASS.addMethod("sub", new JavaFunction(PetPetString.class, "sub", false,
                i -> Math.max(0, (int) ((Double) i.peek() - (Double) i.peek(1)) / 8)
        ));
        STRING_CLASS.addMethod("len", new JavaFunction(String.class, "length", true));
        STRING_CLASS.addMethod("starts", new JavaFunction(String.class, "startsWith", true, oneLengthPenalizer(8), String.class));
        STRING_CLASS.addMethod("ends", new JavaFunction(String.class, "endsWith", true, oneLengthPenalizer(8), String.class));

        ToIntFunction<Interpreter> indexOfPenalizer = i -> {
            int argLen = ((String) i.peek()).length();
            int instanceLen = ((String) i.peek(1)).length();
            return Math.max(0, (instanceLen - argLen) * argLen / 16);
        };

        STRING_CLASS.addMethod("find", new JavaFunction(String.class, "indexOf", true, indexOfPenalizer, String.class));
        STRING_CLASS.addMethod("has", new JavaFunction(String.class, "contains", true, indexOfPenalizer));

        STRING_CLASS.addMethod("upper", new JavaFunction(String.class, "toUpperCase", true, oneLengthPenalizer(8), new Class[0]));
        STRING_CLASS.addMethod("lower", new JavaFunction(String.class, "toLowerCase", true, oneLengthPenalizer(8), new Class[0]));

        STRING_CLASS.addMethod("format", new JavaFunction(PetPetString.class, "format", false,
                i ->
                        ((PetPetList) i.peek()).size() / 2 +
                        ((String) i.peek(1)).length() / 8
        ));
        STRING_CLASS.addMethod("parseNum", new JavaFunction(PetPetString.class, "parseNum", false));
    }

    //Penalizes based on the length of string on top of the stack, divided by the given divisor
    private static ToIntFunction<Interpreter> oneLengthPenalizer(int divisor) {
        return i -> ((String) i.peek()).length() / divisor;
    }

    //I didn't like that doubles without any decimal component printed with the .0
    public static String valueOf(Object o) {
        if (o instanceof Double d && d.longValue() == d)
            return String.valueOf(d.longValue());
        return String.valueOf(o);
    }

    //fake charAt because can't be bothered to add a char class
    public static String charAt(String x, int i) {
        int len = x.length();
        int wrapped = ((i % len) + len) % len;
        return x.substring(wrapped, wrapped+1);
    }

    public static String sub(String x, int a, int b) {
        int len = x.length();
        if (a < 0) a += len;
        if (b < 0) b += len;
        if (a >= b) return "";
        return x.substring(a, b);
    }

    /**
     * Return the parsed number, or null if it failed
     */
    public static Double parseNum(String str) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String format(String x, ArrayList<?> objects) {
        return String.format(x, objects.toArray());
    }
}
