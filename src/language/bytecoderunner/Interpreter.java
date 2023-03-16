package language.bytecoderunner;

import java.util.*;

import static language.compile.Bytecode.*;

/**
 * Runs functions and maintains a global state
 */
public class Interpreter {

    private final Map<Class<?>, LangClass> classMap = new IdentityHashMap<>(); //keys are classes, identity works
    private final Map<String, Object> globals = new HashMap<>();

    private final List<Object> stack = new ArrayList<>();

    private final int MAX_STACK_FRAMES = 64;
    private final Deque<CallFrame> callStack = new ArrayDeque<>();

    public void run(LangFunction function) {
        callStack.push(new CallFrame(function, 0, 0));
        run();
    }

    private void run() {
        CallFrame frame = callStack.getLast();
        LangFunction function = frame.function;
        byte[] curBytes = function.chunk.bytes;
        Object[] constants = function.chunk.constants;
        while (true) {
            switch (curBytes[frame.ip++]) {
                case CONSTANT -> push(constants[curBytes[frame.ip++]]);
                case PUSH_NULL -> push(null);
                case POP -> pop();

                //Temp operators
                case ADD -> push((Integer) pop() + (Integer) pop());
                case SUB -> {int a = (Integer) pop(); int b = (Integer) pop(); push(b-a);}
                case MUL -> push((Integer) pop() * (Integer) pop());
                case DIV -> {int a = (Integer) pop(); int b = (Integer) pop(); push(b/a);}
                case EQ -> push(pop().equals(pop()));

                case PRINT -> System.out.println(pop());
                case RETURN -> {return;}

                case SET_GLOBAL -> globals.put((String) constants[curBytes[frame.ip++]], peek());
                case LOAD_GLOBAL -> push(globals.get((String) constants[curBytes[frame.ip++]]));

                case SET_LOCAL -> stack.set(frame.fp+curBytes[frame.ip++], peek());
                case LOAD_LOCAL -> push(stack.get(frame.fp+curBytes[frame.ip++]));

                case POP_OFFSET_1 -> stack.remove(stack.size()-2);

                case JUMP -> {int offset = ((curBytes[frame.ip++] << 8) | (curBytes[frame.ip++])); frame.ip += offset; }
                case JUMP_IF_FALSE -> {int offset = ((curBytes[frame.ip++] << 8) | (curBytes[frame.ip++])); if (isFalsy(pop())) frame.ip += offset;}
            }
        }
    }

    private boolean isFalsy(Object o) {
        return !(Boolean) o;
    }

    private void push(Object o) {
        stack.add(o);
    }

    private Object pop() {
        return stack.remove(stack.size()-1);
    }

    private Object peek() {
        return stack.get(stack.size()-1);
    }

    private static class CallFrame {
        private LangFunction function;
        private int ip; //instruction pointer
        private int fp; //frame pointer
        public CallFrame(LangFunction f, int ip, int fp) {
            function = f;
            this.ip = ip;
            this.fp = fp;
        }
    }

}
