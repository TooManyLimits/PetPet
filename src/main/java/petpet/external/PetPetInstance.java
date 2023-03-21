package main.java.petpet.external;

import main.java.petpet.helpers.ListClass;
import main.java.petpet.lang.compile.Compiler;
import main.java.petpet.lang.lex.Lexer;
import main.java.petpet.lang.parse.Expression;
import main.java.petpet.lang.parse.Parser;
import main.java.petpet.lang.run.Interpreter;
import main.java.petpet.lang.run.PetPetClosure;
import main.java.petpet.lang.run.PetPetFunction;

import java.util.List;

/**
 * A class that encapsulates, well, an instance of
 * a PetPet runtime. Contains convenience methods
 * and acts as an interface for a script. This is generally
 * the class that users should... well, use.
 */
public class PetPetInstance {

    private static final int DEFAULT_MAX_STACK_FRAMES = 256;
    private final Interpreter interpreter;
    public boolean debugTime, debugBytecode, debugCost;

    public PetPetInstance() {
        this.interpreter = new Interpreter();
        loadDefaultClasses();
    }

    public Object runScript(String scriptName, String source, Object... args) throws Lexer.LexingException, Parser.ParserException, Compiler.CompilationException {
        long before = 0;
        if (debugTime) before = System.nanoTime();

        Lexer.Token[] toks = Lexer.lex(source);
        List<Expression> exprs = new Parser(toks).parseChunk();
        Compiler comp = new Compiler(null);
        new Expression.BlockExpression(0, exprs).compile(comp);
        PetPetFunction compiled = comp.finish(scriptName, 0, 0);

        if (debugTime) System.out.println((System.nanoTime() - before) / 1000000d + " ms to read code");
        if (debugBytecode) System.out.println(compiled.prettyBytecode());
        if (debugTime) before = System.nanoTime();

        PetPetClosure closure = new PetPetClosure(compiled, interpreter);
        Object result = closure.call(args);

        if (debugTime) System.out.println((System.nanoTime() - before) / 1000000d + " ms to execute");
        if (debugCost) System.out.println("Cost was " + interpreter.cost);
        return result;
    }

    public Object runScriptOrThrow(String scriptName, String source, Object... args) {
        try {
            return runScript(scriptName, source, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setGlobal(String key, Object value) {
        interpreter.globals.put(key, value);
    }

    private void loadDefaultClasses() {
        //List Class
        interpreter.classMap.put(ListClass.JAVA_CLASS, ListClass.PETPET_CLASS);
        setGlobal("List", ListClass.NEW_LIST);


    }

    public void setMaxStackFrames(int cap) {interpreter.maxStackFrames = cap;}
}
