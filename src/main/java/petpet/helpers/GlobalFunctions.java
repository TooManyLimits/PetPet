package main.java.petpet.helpers;

import main.java.petpet.lang.run.JavaFunction;

import java.util.ArrayList;
import java.util.HashMap;

public class GlobalFunctions {

    public static final HashMap<String, JavaFunction> DEFAULT_GLOBALS = new HashMap<>();

    static {
        //DEFAULT_GLOBALS.put("range", new JavaFunction(GlobalFunctions.class, "range", false));
        DEFAULT_GLOBALS.put("rand", new JavaFunction(Math.class, "random", false));
        DEFAULT_GLOBALS.put("randIn", new JavaFunction(GlobalFunctions.class, "randIn", false));
    }

    public static ArrayList range(double min, double max) {
        ArrayList result = new ArrayList();
        for (double i = min; i < max; i++)
            result.add(i);
        return result;
    }
    public static double randIn(double min, double max) {return Math.random() * (max - min) + min;}


}
