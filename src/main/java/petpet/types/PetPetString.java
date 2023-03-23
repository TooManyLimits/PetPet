package petpet.types;

import petpet.lang.run.Interpreter;
import petpet.lang.run.JavaFunction;
import petpet.lang.run.PetPetClass;

public class PetPetString {

    public static void registerToInterpreter(Interpreter i) {
        i.classMap.put(String.class, PETPET_CLASS);
        i.globals.put("str", STR);
    }

    private static final PetPetClass PETPET_CLASS;
    private static final JavaFunction STR = new JavaFunction(PetPetString.class, "valueOf", false, Object.class);

    static {
        PETPET_CLASS = new PetPetClass("str");

        PETPET_CLASS.addMethod("__get_num", new JavaFunction(PetPetString.class, "charAt", false));

        //i love arbitrarily guessing on cost penalties with no benchmarking whatsoever ! :D
        PETPET_CLASS.addMethod("sub", new JavaFunction(String.class, "substring", true,
                i -> Math.max(0, (int) ((Double) i.peek() - (Double) i.peek(1)) / 8),
                int.class, int.class
        ));
        PETPET_CLASS.addMethod("len", new JavaFunction(String.class, "length", true));
        PETPET_CLASS.addMethod("startsWith", new JavaFunction(String.class, "startsWith", true,
                i -> ((String) i.peek()).length() / 16,
                String.class
        ));
        PETPET_CLASS.addMethod("endsWith", new JavaFunction(String.class, "endsWith", true,
                i -> ((String) i.peek()).length() / 16,
                String.class
        ));

    }

    //I didn't like that doubles without any decimal component printed with the .0
    public static String valueOf(Object o) {
        if (o instanceof Double d && d.longValue() == d)
            return String.valueOf(d.longValue());
        return String.valueOf(o);
    }

    //fake charAt because can't be bothered to add a char class
    public static String charAt(String x, int i) {
        return x.substring(i, i+1);
    }

    public static String concatAny(String x, Object other) {
        return x + other;
    }
}
