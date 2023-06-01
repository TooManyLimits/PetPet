package petpet.types.libraries;

import petpet.lang.run.JavaFunction;
import petpet.types.PetPetTable;

public class MathLibrary {

    public static PetPetTable<String, Object> createNewMathTable() {
        //create a new math table each time, as it can be edited
        //we don't want one script to edit the math table,
        //and have that affect another script that runs after
        PetPetTable<String, Object> mathTable = new PetPetTable<>();
        mathTable.put("min", MIN);
        mathTable.put("max", MAX);
        mathTable.put("abs", ABS);
        mathTable.put("floor", FLOOR);
        mathTable.put("ceil", CEIL);
        mathTable.put("exp", EXP);
        mathTable.put("pow", POW);
        mathTable.put("round", ROUND);
        mathTable.put("log", LOG);
        mathTable.put("ln", LN);
        mathTable.put("sqrt", SQRT);
        mathTable.put("cbrt", CBRT);

        mathTable.put("rad", RAD);
        mathTable.put("deg", DEG);

        mathTable.put("rand", RAND);
        mathTable.put("randIn", RAND_IN);

        mathTable.put("sin", SIN);
        mathTable.put("asin", ASIN);
        mathTable.put("sinh", SINH);
        mathTable.put("cos", COS);
        mathTable.put("acos", ACOS);
        mathTable.put("cosh", COSH);
        mathTable.put("tan", TAN);
        mathTable.put("atan", ATAN);
        mathTable.put("atan2", ATAN2);
        mathTable.put("tanh", TANH);
        mathTable.put("pi", Math.PI);
        mathTable.put("e", Math.E);
        return mathTable;
    }

    //General
    private static final JavaFunction MIN = new JavaFunction(Math.class, "min", false, double.class, double.class);
    private static final JavaFunction MAX = new JavaFunction(Math.class, "max", false, double.class, double.class);
    private static final JavaFunction ABS = new JavaFunction(Math.class, "abs", false, double.class);
    private static final JavaFunction FLOOR = new JavaFunction(Math.class, "floor", false);
    private static final JavaFunction CEIL = new JavaFunction(Math.class, "ceil", false);
    private static final JavaFunction EXP = new JavaFunction(Math.class, "exp", false);
    private static final JavaFunction POW = new JavaFunction(Math.class, "pow", false);
    private static final JavaFunction ROUND = new JavaFunction(Math.class, "round", false, double.class);
    private static final JavaFunction LOG = new JavaFunction(Math.class, "log10", false);
    private static final JavaFunction LN = new JavaFunction(Math.class, "log", false);
    private static final JavaFunction SQRT = new JavaFunction(Math.class, "sqrt", false);
    private static final JavaFunction CBRT = new JavaFunction(Math.class, "cbrt", false);

    //Conversion
    private static final JavaFunction RAD = new JavaFunction(Math.class, "toRadians", false);
    private static final JavaFunction DEG = new JavaFunction(Math.class, "toDegrees", false);

    //Random
    private static final JavaFunction RAND = new JavaFunction(Math.class, "random", false);
    private static final JavaFunction RAND_IN = new JavaFunction(MathLibrary.class, "randIn", false);

    //Trig
    private static final JavaFunction SIN = new JavaFunction(Math.class, "sin", false);
    private static final JavaFunction ASIN = new JavaFunction(Math.class, "asin", false);
    private static final JavaFunction SINH = new JavaFunction(Math.class, "sinh", false);
    private static final JavaFunction COS = new JavaFunction(Math.class, "cos", false);
    private static final JavaFunction ACOS = new JavaFunction(Math.class, "acos", false);
    private static final JavaFunction COSH = new JavaFunction(Math.class, "cosh", false);
    private static final JavaFunction TAN = new JavaFunction(Math.class, "tan", false);
    private static final JavaFunction ATAN = new JavaFunction(Math.class, "atan", false);
    private static final JavaFunction ATAN2 = new JavaFunction(Math.class, "atan2", false);
    private static final JavaFunction TANH = new JavaFunction(Math.class, "tanh", false);


    public static double randIn(double min, double max) {
        return Math.random() * (max - min) + min;
    }

    public static final String EXTRA_MATH_FUNCTIONS = """
            fn math:lerp(a,b,t) a + (b-a)*t
            fn math:map(x,L1,R1,L2,R2) (x-L1)*(R2-L2)/(R1-L1) + L2
            fn math:clamp(v, a, b) if v < a a else if v > b b else v
            """;

}
