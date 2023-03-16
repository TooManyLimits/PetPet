package language.bytecoderunner;

import language.compile.Chunk;

/**
 * Represents a function that's written in the language itself,
 * !not! a function that's a proxy for an external java call.
 */
public class LangFunction {

    public final Chunk chunk;
    public final int argCount;

    public LangFunction(Chunk chunk, int args) {
        this.chunk = chunk;
        argCount = args;
    }



}
