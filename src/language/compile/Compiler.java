package language.compile;

import language.bytecoderunner.LangFunction;
import language.parse.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * A compiler can translate an AST structure into bytecodes!
 */
public class Compiler {

    private final List<Local> locals = new ArrayList<>(); //map name to depth
    private final List<CompileTimeUpvalue> upvalues = new ArrayList<>();
    private int scopeDepth = 0;

    private final Chunk.Builder chunkBuilder;

    private final Compiler parent;

    public Compiler(Compiler parent) {
        this.parent = parent;
        chunkBuilder = Chunk.builder();
        try {registerLocal("");} catch (Exception ignored) {}
    }

    public LangFunction finish(String name, int paramCount) {
        bytecode(Bytecode.RETURN);
        return new LangFunction(name, chunkBuilder.build(), paramCount, upvalues.size());
    }

    public void beginScope() {
        scopeDepth++;
    }

    public void endScope() {
        scopeDepth--;
        while (locals.size() > 0) {
            Local local = locals.get(locals.size()-1);
            if (local.depth <= scopeDepth) break;
            if (local.isCaptured) {
                chunkBuilder.write(Bytecode.CLOSE_UPVALUE);
            } else {
                //Offset 1 because at the end of the scope, the result of the block expression is on the stack
                chunkBuilder.write(Bytecode.POP_OFFSET_1);
            }
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
            if (locals.get(i).name.equals(varName))
                return i;
        }
        return -1;
    }

    private int registerUpvalue(int index, boolean isLocal) throws CompilationException {
        for (int i = 0; i < upvalues.size(); i++) {
            if (upvalues.get(i).isLocal == isLocal && upvalues.get(i).index == index)
                return i;
        }
        if (upvalues.size() >= 255)
            throw new CompilationException("Too many upvalues! Max 255");

        upvalues.add(new CompileTimeUpvalue(index, isLocal));
        return upvalues.size()-1;
    }

    public int indexOfUpvalue(String varName) throws CompilationException {
        if (parent == null) return -1;
        int parentLocal = parent.indexOfLocal(varName);
        if (parentLocal != -1) {
            parent.locals.get(parentLocal).isCaptured = true;
            return registerUpvalue(parentLocal, true);
        }

        int parentUpvalue = parent.indexOfUpvalue(varName);
        if (parentUpvalue != -1) {
            return registerUpvalue(parentUpvalue, false);
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

    public void emitClosure(Compiler finishedCompiler) throws CompilationException {
        bytecode(Bytecode.CLOSURE);
        for (CompileTimeUpvalue upvalue : finishedCompiler.upvalues) {
            bytecode(upvalue.isLocal ? (byte) 1 : 0);
            bytecode((byte) upvalue.index);
        }
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

    public int registerConstant(Object value) throws CompilationException {
        int loc = getChunkBuilder().registerConstant(value);
        if (loc > 255) throw new CompilationException("Too many literals!");
        return loc;
    }

    private Chunk.Builder getChunkBuilder() {
        return chunkBuilder;
    }

    public static class CompilationException extends Exception {

        public CompilationException(String str) {
            super(str);
        }
    }

    private static class Local {
        String name;
        int depth;
        boolean isCaptured;
        public Local(String name, int depth) {
            this.name = name;
            this.depth = depth;
            isCaptured = false;
        }
    }
    private record CompileTimeUpvalue(int index, boolean isLocal) {}
}
