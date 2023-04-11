package petpet.lang.compile;

import petpet.lang.run.PetPetFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * A compiler can translate an AST structure into bytecodes!
 */
public class Compiler {

    private final List<Local> locals = new ArrayList<>(); //map name to depth
    private final List<CompileTimeUpvalue> upvalues = new ArrayList<>();

    //The nth number in the list is the bytecode index of the first byte at line n or higher
    private final ArrayList<Integer> lineNumberTable = new ArrayList<>();
    private int latestLine = 0;
    private int scopeDepth = 0;

    private final Chunk.Builder chunkBuilder;

    private final Compiler parent;

    public Compiler(Compiler parent) {
        this.parent = parent;
        chunkBuilder = Chunk.builder();
        try {registerLocal("");} catch (Exception neverHappens) {
            throw new RuntimeException("Something broke deep in PetPet, report this please");
        }
    }

    public PetPetFunction finish(String name, int lineNumber, int paramCount) {
        bytecode(Bytecode.RETURN);
        int[] lineNumberArr = new int[lineNumberTable.size()];
        for (int i = 0; i < lineNumberArr.length; i++)
            lineNumberArr[i] = lineNumberTable.get(i);
        return new PetPetFunction(name, chunkBuilder.build(), lineNumber, lineNumberArr, paramCount, upvalues.size());
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
        if (locals.size() >= 65500)
            throw new CompilationException("Too many local variables! Max 65500", latestLine);
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
        if (upvalues.size() >= 65500)
            throw new CompilationException("Too many upvalues! Max 65500", latestLine);

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
        if (jump < Short.MIN_VALUE || jump > Short.MAX_VALUE) throw new CompilationException("Too much code to jump over! Max 32k bytecodes either direction", latestLine);
        chunkBuilder.writeShortAt(jumpLocation, jump);
    }

    public int startLoop() {
        return chunkBuilder.getByteIndex();
    }

    //loopstart is the index of the first bytecode of condition
    public void endLoop(int loopStart) throws CompilationException {
        int jump = loopStart - chunkBuilder.getByteIndex() - 3;
        if (jump < Short.MIN_VALUE || jump > Short.MAX_VALUE) throw new CompilationException("Too much code to jump over! Max 32k bytecodes either direction", latestLine);
        chunkBuilder.writeWithShortArg(Bytecode.JUMP, jump);
    }

    public void emitClosure(Compiler finishedCompiler) throws CompilationException {
        //First, check if *all* upvalue indices are below 250
        boolean allBelow250 = true;
        for (CompileTimeUpvalue upvalue : finishedCompiler.upvalues)
            if (upvalue.index >= 250) {
                allBelow250 = false;
                break;
            }

        //Then use that to decide if we use closure or big closure format.
        bytecode(allBelow250 ? Bytecode.CLOSURE : Bytecode.BIG_CLOSURE);
        for (CompileTimeUpvalue upvalue : finishedCompiler.upvalues) {
            bytecode(upvalue.isLocal ? (byte) 1 : 0);
            if (allBelow250)
                bytecode((byte) upvalue.index);
            else
                writeShort((short) upvalue.index);
        }
    }

    //Accepts the given line number and inserts it for lookup table
    public void acceptLineNumber(int line) {
        while (line > latestLine) {
            lineNumberTable.add(getChunkBuilder().getByteIndex());
            latestLine++;
        }
    }

    public void bytecode(byte code) {
        getChunkBuilder().write(code);
    }

    public void writeShort(short s) {
        getChunkBuilder().writeShort(s);
    }

    public void bytecodeWithByteArg(byte code, byte arg) {
        getChunkBuilder().writeWithByteArg(code, arg);
    }

    public void bytecodeWithShortArg(byte code, short arg) {
        getChunkBuilder().writeWithShortArg(code, arg);
    }

    public int registerConstant(Object value) throws CompilationException {
        int loc = getChunkBuilder().registerConstant(value);
        if (loc > 65500) throw new CompilationException("Too many literals! Max 65500", latestLine);
        return loc;
    }

    private Chunk.Builder getChunkBuilder() {
        return chunkBuilder;
    }

    public static class CompilationException extends Exception {

        public CompilationException(String message, int approxLine) {
            super(message + " (Approximate line: " + approxLine + ")");
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
