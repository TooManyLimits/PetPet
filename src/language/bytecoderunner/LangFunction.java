package language.bytecoderunner;

import language.compile.Chunk;

/**
 * Represents a function that's written in the language itself,
 * !not! a function that's a proxy for an external java call.
 */
public class LangFunction {

    public final String name;
    public final Chunk chunk;
    public final int argCount;

    public LangFunction(String name, Chunk chunk, int args) {
        this.name = name;
        this.chunk = chunk;
        argCount = args;
    }


    public String toString() {
        return name;
    }

    public String prettyBytecode() {
        return chunk.toString(0);
    }
}
