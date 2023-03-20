package language.compile;

import language.run.PetPetFunction;

import java.util.ArrayList;

import static language.compile.Bytecode.*;

public class Chunk {

    public final Object[] constants;
    public final byte[] bytes;

    private Chunk(Object[] constants, byte[] bytes) {
        this.constants = constants; this.bytes = bytes;
    }

    public String toString(int indent) {
        StringBuilder result = new StringBuilder();
        int digits = ((int) Math.log10(bytes.length + 1)+1);
        String numFormatString = " ".repeat(indent) + "%0" + digits + "d | ";
        PetPetFunction constFunc = null;
        for (int i = 0; i < bytes.length; i++) {
            result.append(String.format(numFormatString, i));
            result.append(Bytecode.NAMES[bytes[i]]);
            boolean dontNewLine = false;
            byte code = bytes[i];
            switch (code) {
                case CONSTANT -> {
                    result.append("(").append(bytes[++i]).append(") = ");
                    if (constants[bytes[i]] instanceof PetPetFunction func) {
                        result.append(func).append(":\n").append(func.chunk.toString(indent + digits + 1));
                        dontNewLine = true;
                        constFunc = func;
                    } else
                        result.append("'").append(constants[bytes[i]]).append("'");
                }
                case SET_GLOBAL,LOAD_GLOBAL -> result.append("(").append(bytes[++i]).append(") = '").append(constants[bytes[i]]).append("'");
                case SET_LOCAL,LOAD_LOCAL,SET_UPVALUE,LOAD_UPVALUE -> result.append("(").append(bytes[++i]).append(")");
                case JUMP, JUMP_IF_FALSE, JUMP_IF_TRUE -> result.append(" by ").append(extendSignwise(bytes[++i] << 8 | bytes[++i], 3));
                case CALL, INVOKE -> result.append(" with ").append(bytes[++i]).append(" args");
                case CLOSURE -> {
                    if (constFunc == null) throw new RuntimeException("Failed to print closure bytecode");
                    result.append(" over: \n");
                    for (int j = 0; j < constFunc.numUpvalues; j++)
                        result.append(bytes[++i] == 0 ? "| Upvalue " : "| Local ").append(bytes[++i]).append("\n");
                    dontNewLine = true;
                }
                default -> {}
            }
            if (code != CONSTANT) constFunc = null;
            if (!dontNewLine) result.append("\n");
        }
        return result.toString();
    }

    private static int extendSignwise(int v, int o) {
        return v < 0 ? v - o : v > 0 ? v + o : v;
    }

    public static Builder builder() {return new Builder();}

    /**
     * Not an actual builder since it doesn't use the "return this" pattern?
     * It's literally just a helper object that allows you to construct a
     * Chunk piece by piece instead of all at once... couldn't think of a
     * different word though
     */
    public static class Builder {
        private final ArrayList<Object> constants = new ArrayList<>();
        private byte[] bytes;
        private int cur;

        private Builder() {
            cur = 0;
            bytes = new byte[100];
        }

        public int getByteIndex() {
            return cur;
        }

        public void writeByteAt(int index, int b) {
            bytes[index] = (byte) b;
        }

        public void writeShortAt(int index, int s) {
            writeByteAt(index, s >>> 8);
            writeByteAt(index + 1, s);
        }

        //Adds a value as a constant if not already there, then returns the index
        //of said constant in the constants list.
        public int registerConstant(Object value) {
            int i = constants.indexOf(value);
            if (i != -1)
                return i;
            constants.add(value);
            return constants.size()-1;
        }

        public void write(int b) {
            //Realloc and copy bytes if needed
            if (cur == bytes.length) {
                byte[] grown = new byte[bytes.length * 2];
                System.arraycopy(bytes, 0, grown, 0, cur);
                bytes = grown;
            }
            bytes[cur++] = (byte) b;
        }

        public void writeWithByteArg(int code, int operand) {
            write(code);
            write(operand);
        }

        public void writeWithShortArg(int code, int operand) {
            write(code);
            write(operand >>> 8); //big endian
            write(operand);
        }

        public Chunk build() {
            byte[] shrunk = new byte[cur];
            System.arraycopy(bytes, 0, shrunk, 0, cur);
            return new Chunk(constants.toArray(new Object[0]), shrunk);
        }

    }

}
