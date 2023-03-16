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
        push(function);
        callStack.push(new CallFrame(function, 0, 0));
        run();
    }

    private void run() {
        CallFrame frame = callStack.peek();
        byte[] curBytes = frame.function.chunk.bytes;
        Object[] constants = frame.function.chunk.constants;
        while (true) {
            switch (curBytes[frame.ip++]) {
                case CONSTANT -> push(constants[curBytes[frame.ip++]]);
                case PUSH_NULL -> push(null);
                case POP -> pop();

                //Temp operators
                case ADD -> push((Integer) pop() + (Integer) pop());
                case SUB -> {int r = (Integer) pop(); int l = (Integer) pop(); push(l-r);}
                case MUL -> push((Integer) pop() * (Integer) pop());
                case DIV -> {int r = (Integer) pop(); int l = (Integer) pop(); push(l/r);}
                case MOD -> {int r = (Integer) pop(); int l = (Integer) pop(); push(l%r);}
                case EQ -> push(Objects.equals(pop(), pop()));
                case NEQ -> push(!Objects.equals(pop(), pop()));
                case LT -> push((Integer) pop() >= (Integer) pop());
                case GT -> push((Integer) pop() <= (Integer) pop());
                case LTE -> push((Integer) pop() > (Integer) pop());
                case GTE -> push((Integer) pop() < (Integer) pop());

                case PRINT -> System.out.println(pop());
                case RETURN -> {
                    Object result = pop();
                    callStack.pop();
                    if (callStack.size() == 0) return; //return for real

                    while (frame.fp > stack.size()) stack.remove(stack.size()-1);
                    push(result);
                    frame = callStack.peek();
                    curBytes = frame.function.chunk.bytes;
                    constants = frame.function.chunk.constants;
                }

                case SET_GLOBAL -> globals.put((String) constants[curBytes[frame.ip++]], peek());
                case LOAD_GLOBAL -> push(globals.get((String) constants[curBytes[frame.ip++]]));

                case SET_LOCAL -> stack.set(frame.fp+curBytes[frame.ip++], peek());
                case LOAD_LOCAL -> push(stack.get(frame.fp+curBytes[frame.ip++]));

                case POP_OFFSET_1 -> stack.remove(stack.size()-2);

                case JUMP -> {int offset = ((curBytes[frame.ip++] << 8) | (curBytes[frame.ip++])); frame.ip += offset; }
                case JUMP_IF_FALSE -> {int offset = ((curBytes[frame.ip++] << 8) | (curBytes[frame.ip++])); if (isFalsy(pop())) frame.ip += offset;}

                case CALL -> {
                    int argCount = curBytes[frame.ip++];
                    makeCall(peek(argCount), argCount);
                    frame = callStack.peek();
                    curBytes = frame.function.chunk.bytes;
                    constants = frame.function.chunk.constants;
                }
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
        return peek(0);
    }

    private Object peek(int offset) {
        return stack.get(stack.size()-1-offset);
    }

    private void makeCall(Object callee, int argCount) {
        if (callee instanceof LangFunction f) {
            if (argCount != f.argCount)
                runtimeException(String.format("Expected %d args, got %d", f.argCount, argCount));
            callStack.push(new CallFrame(f, 0, stack.size() - argCount - 1));
        } else {
            runtimeException("Attempt to call non-function value: " + callee);
        }
    }

    private void runtimeException(String message) {
        StringBuilder messageBuilder = new StringBuilder(message);
        for (CallFrame frame : callStack) {
            messageBuilder.append("\n in: ").append(frame.function.toString());
        }
        message = messageBuilder.toString();

        throw new RuntimeException(message);
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
