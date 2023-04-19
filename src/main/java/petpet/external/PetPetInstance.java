package petpet.external;

import petpet.types.immutable.PetPetListView;
import petpet.types.immutable.PetPetTableView;
import petpet.types.libraries.GlobalFunctions;
import petpet.types.PetPetList;
import petpet.types.PetPetString;
import petpet.types.PetPetTable;
import petpet.lang.compile.Compiler;
import petpet.lang.lex.Lexer;
import petpet.lang.parse.Expression;
import petpet.lang.parse.Parser;
import petpet.lang.run.*;
import petpet.types.libraries.MathLibrary;

import java.util.List;
import java.util.Map;

/**
 * A class that encapsulates, well, an instance of
 * a PetPet runtime. Contains convenience methods
 * and acts as an interface for a script. This is generally
 * the class that users should... well, use.
 */
public class PetPetInstance {
    public final Interpreter interpreter;
    public boolean debugTime, debugBytecode, debugCost;

    public PetPetInstance() {
        this.interpreter = new Interpreter();
        loadBuiltinLibrary();
    }

    /**
     * Compiles the given string into a closure
     * and runs it immediately.
     */
    public Object runScript(String scriptName, String source, Object... args) throws Lexer.LexingException, Parser.ParserException, Compiler.CompilationException {
        PetPetClosure closure = compile(scriptName, source);

        long before = 0;
        if (debugTime) before = System.nanoTime();
        Object result = closure.call(args);
        if (debugTime) System.out.println((System.nanoTime() - before) / 1000000d + " ms to execute");
        if (debugCost) System.out.println("Cost was " + interpreter.cost);
        return result;
    }

    public PetPetClosure compile(String name, String script) throws Lexer.LexingException, Parser.ParserException, Compiler.CompilationException {
        long before = 0;
        if (debugTime) before = System.nanoTime();

        Lexer.Token[] toks = Lexer.lex(script);
        List<Expression> exprs = new Parser(toks).parseChunk();
        Compiler comp = new Compiler(null);
        new Expression.BlockExpression(0, exprs).compile(comp);
        PetPetFunction compiled = comp.finish(name, 0, 0);

        if (debugTime) System.out.println((System.nanoTime() - before) / 1000000d + " ms to compile ");
        if (debugBytecode) System.out.println(compiled.prettyBytecode());
        return new PetPetClosure(compiled, interpreter);
    }

    public void setGlobal(String key, Object value) {
        interpreter.globals.put(key, value);
    }

    public Object getGlobal(String key) {
        return interpreter.globals.get(key);
    }

    private void loadBuiltinLibrary() {
        //Num class
        interpreter.classMap.put(Double.class, new PetPetClass("num").makeEditable());
        interpreter.classMap.put(Boolean.class, new PetPetClass("bool").makeEditable());
        interpreter.classMap.put(String.class, PetPetString.STRING_CLASS.copy().makeEditable());
        interpreter.classMap.put(JavaFunction.class, PetPetReflector.reflect(JavaFunction.class, "jfn").copy().makeEditable());
        interpreter.classMap.put(PetPetClosure.class, PetPetReflector.reflect(PetPetClosure.class, "fn").copy().makeEditable());
        interpreter.classMap.put(PetPetClass.class, PetPetClass.PET_PET_CLASS_CLASS.copy().makeEditable());

        interpreter.classMap.put(PetPetList.class, PetPetList.LIST_CLASS.copy().makeEditable());
        interpreter.classMap.put(PetPetListView.class, PetPetListView.LIST_VIEW_CLASS.copy().makeEditable());
        interpreter.classMap.put(PetPetTable.class, PetPetTable.TABLE_CLASS.copy().makeEditable());
        interpreter.classMap.put(PetPetTableView.class, PetPetTableView.TABLE_VIEW_CLASS.copy().makeEditable());


        //Global values
        for (Map.Entry<String, JavaFunction> entry : GlobalFunctions.DEFAULT_GLOBALS.entrySet())
            setGlobal(entry.getKey(), entry.getValue());
        setGlobal("math", MathLibrary.createNewMathTable());
        setGlobal("_G", interpreter.globals);

        //Load the extra math functions as well which are better defined through
        //petpet, such as math:lerp and math:map
        try { runScript("mathExtras", MathLibrary.EXTRA_MATH_FUNCTIONS);
        } catch (Exception impossible) {throw new IllegalStateException(impossible);}
    }

    public void registerClass(Class<?> clazz, PetPetClass petPetClass) {
        interpreter.classMap.put(clazz, petPetClass);
    }

    public void deregisterClass(Class<?> clazz) {
        interpreter.classMap.remove(clazz);
    }

    public void setMaxStackFrames(int cap) {interpreter.maxStackFrames = cap;}
}
