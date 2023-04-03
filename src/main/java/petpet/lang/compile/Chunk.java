package petpet.lang.compile;

import petpet.lang.run.PetPetFunction;

import java.util.ArrayList;

import static petpet.lang.compile.Bytecode.*;

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
                    result.append("(").append(bytes[++i] & 0xff).append(") = ");
                    if (constants[bytes[i]] instanceof PetPetFunction func) {
                        result.append(func).append(":\n").append(func.chunk.toString(indent + digits + 1));
                        dontNewLine = true;
                        constFunc = func;
                    } else
                        result.append("'").append(constants[bytes[i]]).append("'");
                }
                case SET_GLOBAL,LOAD_GLOBAL -> {
                    int idx = bytes[++i] & 0xff;
                    System.out.println(idx);
                    result.append("(").append(idx).append(") = '").append(constants[idx]).append("'");
                }
                case BIG_SET_GLOBAL,BIG_LOAD_GLOBAL -> {
                    int idx = readUnsignedShort(bytes, i); i += 2;
//                    System.out.println(idx >> 8);
//                    System.out.println(idx & 0xff);
//                    System.out.println((short) idx);
                    result.append("(").append(idx).append(") = '").append(constants[idx]).append("'");
                }
                case SET_LOCAL,LOAD_LOCAL,SET_UPVALUE,LOAD_UPVALUE -> {
                    int idx = bytes[++i] & 0xff;
                    result.append("(").append(idx).append(")");
                }
                case BIG_SET_LOCAL,BIG_LOAD_LOCAL,BIG_SET_UPVALUE,BIG_LOAD_UPVALUE -> {
                    int idx = readUnsignedShort(bytes, i); i += 2;
                    result.append("(").append(idx).append(")");
                }
                case JUMP, JUMP_IF_FALSE, JUMP_IF_TRUE -> result.append(" by ").append(extendSignwise(bytes[++i] << 8 | bytes[++i], 3));
                case CALL, INVOKE -> result.append(" with ").append(bytes[++i] & 0xff).append(" args");
                case CLOSURE -> {
                    if (constFunc == null) throw new RuntimeException("Failed to print closure bytecode");
                    result.append(" over: \n");
                    for (int j = 0; j < constFunc.numUpvalues; j++)
                        result.append((bytes[++i] & 0xff) == 0 ? "| Upvalue " : "| Local ").append(bytes[++i] & 0xff).append("\n");
                    dontNewLine = true;
                }
                case BIG_CLOSURE -> {
                    if (constFunc == null) throw new RuntimeException("Failed to print big closure bytecode");
                    result.append(" over: \n");
                    for (int j = 0; j < constFunc.numUpvalues; j++)
                        result.append((bytes[++i]) == 0 ? "| BigUpvalue " : "| BigLocal ").append(((bytes[++i] << 8) | (bytes[++i])) & 0xffff).append("\n");
                    dontNewLine = true;
                }
                default -> {}
            }
            if (code != CONSTANT) constFunc = null;
            if (!dontNewLine) result.append("\n");
        }
        return result.toString();
    }

    private static int readUnsignedShort(byte[] bytes, int i) {
        return (((bytes[++i] << 8) & 0xffff) | (bytes[++i] & 0xff)) & 0xffff;
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

        public void writeShort(int s) {
            write(s >>> 8); //big endian
            write(s);
        }

        public void writeWithByteArg(int code, int operand) {
            write(code);
            write(operand);
        }

        public void writeWithShortArg(int code, int operand) {
            write(code);
            writeShort(operand);
        }

        public void writeWithIntArg(int code, int operand) {
            write(code);
            write(operand >>> 24);
            write(operand >>> 16);
            write(operand >>> 8);
            write(operand);
        }

        public Chunk build() {
            byte[] shrunk = new byte[cur];
            System.arraycopy(bytes, 0, shrunk, 0, cur);
            return new Chunk(constants.toArray(new Object[0]), shrunk);
        }

    }

}
