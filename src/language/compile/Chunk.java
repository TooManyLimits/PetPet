package language.compile;

import java.util.ArrayList;

import static language.compile.Bytecode.*;

public class Chunk {

    public final Object[] constants;
    public final byte[] bytes;

    private Chunk(Object[] constants, byte[] bytes) {
        this.constants = constants; this.bytes = bytes;
    }

    public void printBytecode() {
        String numFormatString = "%0" + ((int) Math.log10(bytes.length + 1)+1) + "d | ";
        for (int i = 0; i < bytes.length; i++) {
            System.out.printf(numFormatString, i);
            System.out.print(Bytecode.NAMES[bytes[i]]);
            switch (bytes[i]) {
                case CONSTANT,SET_GLOBAL,LOAD_GLOBAL -> System.out.print("(" + bytes[++i] + ") = '" + constants[bytes[i]] + "'");
                case SET_LOCAL,LOAD_LOCAL -> System.out.print("(" + bytes[++i] + ")");
                case JUMP, JUMP_IF_FALSE -> System.out.print(" by " + ((bytes[++i] << 8 | bytes[++i])+3));
                default -> {}
            }
            System.out.println();
        }
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
