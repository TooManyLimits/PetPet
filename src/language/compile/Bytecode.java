package language.compile;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public class Bytecode {


    public static final byte CONSTANT = 0; //Pushes a constant value on the stack

    //Basic binary operators. Push left arg, push right arg, run this, and result is on the stack.
    public static final byte ADD = 1;
    public static final byte SUB = 2;
    public static final byte MUL = 3;
    public static final byte DIV = 4;

    public static final byte RETURN = 5; //Escapes the current function, using the top value of the stack as the return

    public static final byte POP = 6; //Pops a value off the stack

    public static final byte PRINT = 7; //temporary

    public static final byte PUSH_NULL = 8;
    public static final byte SET_GLOBAL = 9; //Takes an int param, index into the chunk's constants. The constant is a string name. Peeks the value at the top of stack, puts it in the globals at that name.
    public static final byte LOAD_GLOBAL = 10; //Takes an int param, index into the chunk's constants. The constant is a string name. Pushes the global at that name onto the stack.
    public static final byte SET_LOCAL = 11; //Takes an int param, index into the stack. peeks the value at the top of the stack and stores into that slot.
    public static final byte LOAD_LOCAL = 12; //Takes an int param, index into the stack. pushes the value at that index onto the stack.
    public static final byte POP_OFFSET_1 = 13; //A "pop" operation that instead of removing the top element of the stack, removes the one just below it.

    public static final byte JUMP_IF_FALSE = 14;
    public static final byte JUMP = 15;

    public static final byte EQ = 16;


    public static final byte CALL = 17; //takes a byte arg for the number of arguments

    public static final byte LT = 18;
    public static final byte GT = 19;
    public static final byte LTE = 20;
    public static final byte GTE = 21;

    public static final byte NEQ = 22;
    public static final byte MOD = 23;
    public static final byte CLOSURE = 24; //2 bytes per closed variable, as in Lox.
    public static final byte SET_UPVALUE = 25;
    public static final byte LOAD_UPVALUE = 26;


    //Lookup for bytecode printouts
    public static final String[] NAMES = Arrays.stream(Bytecode.class.getFields()).filter(f -> Modifier.isStatic(f.getModifiers())).filter(f -> f.getType() == byte.class)
            .sorted((a, b) -> {
                try {
                    return Integer.compare((byte) a.get(null) & 0xFF, (byte) b.get(null) & 0xFF);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }).map(Field::getName).toArray(String[]::new);

}
