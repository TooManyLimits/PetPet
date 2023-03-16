package language.compile;

import language.bytecoderunner.LangFunction;
import language.parse.Expression;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Vector;

/**
 * A compiler can translate an AST structure into bytecodes!
 */
public class Compiler {

    private final List<Local> locals = new ArrayList<>(); //map name to depth
    private int scopeDepth = 0;

    private Chunk.Builder chunkBuilder;

    public LangFunction compile(List<Expression> exprs) {
        locals.clear();
        scopeDepth = 0;
        chunkBuilder = Chunk.builder();

        try {
            for (Expression e : exprs) {
                e.scanForDeclarations(this);
                e.writeBytecode(this);
                chunkBuilder.write(Bytecode.POP);
            }
        } catch (CompilationException e) {
            throw new RuntimeException(e);
        }
        chunkBuilder.write(Bytecode.RETURN);
        return new LangFunction(chunkBuilder.build(), 0);
    }

    public void beginScope() {
        scopeDepth++;
    }

    public void endScope() {
        scopeDepth--;
        while (locals.size() > 0 && locals.get(locals.size()-1).depth() > scopeDepth) {
            //Offset 1 because at the end of the scope, the result of the block expression is on the stack
            chunkBuilder.write(Bytecode.POP_OFFSET_1);
            locals.remove(locals.size()-1);
        }
    }

    public void registerLocal(String varName) throws CompilationException {
        if (locals.size() >= 256)
            throw new CompilationException("Too many local variables!");
        locals.add(new Local(varName, scopeDepth));
    }

    public int indexOfLocal(String varName) {
        for (int i = locals.size()-1; i >= 0; i--) {
            if (locals.get(i).name().equals(varName))
                return i;
        }
        return -1;
    }

    public int emitJump(byte instruction) { //returns the location of the jump
        int res = chunkBuilder.getByteIndex()+1;
        chunkBuilder.writeWithShortArg(instruction, -1);
        return res;
    }

    public void patchJump(int jumpLocation) throws CompilationException {
        int jump = chunkBuilder.getByteIndex() - jumpLocation - 2;
        if (jump < Short.MIN_VALUE || jump > Short.MAX_VALUE) throw new CompilationException("Too much code to jump over!");
        chunkBuilder.writeShortAt(jumpLocation, jump);
    }

    public void bytecode(byte code) {
        getChunkBuilder().write(code);
    }

    public void bytecodeWithByteArg(byte code, byte arg) {
        getChunkBuilder().writeWithByteArg(code, arg);
    }

    public void bytecodeWithShortArg(byte code, short arg) {
        getChunkBuilder().writeWithShortArg(code, arg);
    }

    public int registerConstant(Object value) {
        return getChunkBuilder().registerConstant(value);
    }

    private Chunk.Builder getChunkBuilder() {
        return chunkBuilder;
    }

    public static class CompilationException extends Exception {

        public CompilationException(String str) {
            super(str);
        }
    }

    private record Local(String name, int depth) {}

}
