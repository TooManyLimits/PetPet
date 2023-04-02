package petpet.external;

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
    private final Interpreter interpreter;
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

    private void loadBuiltinLibrary() {
        //Num class
        interpreter.classMap.put(Double.class, new PetPetClass("num"));
        interpreter.classMap.put(Boolean.class, new PetPetClass("bool"));
        interpreter.classMap.put(JavaFunction.class, new PetPetClass("jfunc"));
        interpreter.classMap.put(PetPetClosure.class, new PetPetClass("func"));

        PetPetString.registerToInterpreter(this.interpreter);
        PetPetList.registerToInterpreter(this.interpreter);
        PetPetTable.registerToInterpreter(this.interpreter);

        //Global functions
        for (Map.Entry<String, JavaFunction> entry : GlobalFunctions.DEFAULT_GLOBALS.entrySet())
            setGlobal(entry.getKey(), entry.getValue());

        MathLibrary.registerToInterpreter(this.interpreter);
    }

    public void registerClass(Class<?> clazz, PetPetClass petPetClass) {
        interpreter.classMap.put(clazz, petPetClass);
    }

    public void deregisterClass(Class<?> clazz) {
        interpreter.classMap.remove(clazz);
    }

    public void setMaxStackFrames(int cap) {interpreter.maxStackFrames = cap;}
}
