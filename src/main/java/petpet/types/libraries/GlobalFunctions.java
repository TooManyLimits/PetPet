package petpet.types.libraries;

import petpet.lang.run.JavaFunction;
import petpet.types.PetPetList;
import petpet.types.PetPetString;

import java.util.HashMap;

public class GlobalFunctions {

    public static final HashMap<String, JavaFunction> DEFAULT_GLOBALS = new HashMap<>();

    static {
        //A little lonely in here, but it's ok
        DEFAULT_GLOBALS.put("range", new JavaFunction(GlobalFunctions.class, "range", false,
                i -> Math.max(0, (int) ((double) i.peek() - (double) i.peek(1))) //cost penalty
        ));
        DEFAULT_GLOBALS.put("print", new JavaFunction(GlobalFunctions.class, "print", false));
    }

    public static PetPetList range(double min, double max) {
        PetPetList result = new PetPetList((int) (max-min+1));
        for (double i = min; i < max; i++)
            result.add(i);
        return result;
    }

    public static void print(Object o) {
        System.out.println(PetPetString.valueOf(o));
    }

}
