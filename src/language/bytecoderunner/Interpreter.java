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
        LangClosure closure = new LangClosure(function);
        push(closure);
        callStack.push(new CallFrame(closure, 0, 0));
        run();
    }

    private void run() {
        CallFrame frame = callStack.peek();
        byte[] curBytes = frame.closure.function.chunk.bytes;
        Object[] constants = frame.closure.function.chunk.constants;
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
                case LT -> push((Integer) pop() > (Integer) pop());
                case GT -> push((Integer) pop() < (Integer) pop());
                case LTE -> push((Integer) pop() >= (Integer) pop());
                case GTE -> push((Integer) pop() <= (Integer) pop());

                case PRINT -> System.out.println(pop());
                case RETURN -> {
                    Object result = pop();
                    callStack.pop();
                    if (callStack.size() == 0) return; //return for real

                    while (frame.fp < stack.size()) stack.remove(stack.size()-1);
                    push(result);
                    frame = callStack.peek();
                    curBytes = frame.closure.function.chunk.bytes;
                    constants = frame.closure.function.chunk.constants;
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
                    curBytes = frame.closure.function.chunk.bytes;
                    constants = frame.closure.function.chunk.constants;
                }

                case CLOSURE -> {
                    LangClosure closure = new LangClosure((LangFunction) pop());
                    for (int i = 0; i < closure.upvalues.length; i++) {

                    }
                }

                case SET_UPVALUE -> frame.closure.upvalues[curBytes[frame.ip++]].value = peek();
                case LOAD_UPVALUE -> push(frame.closure.upvalues[curBytes[frame.ip++]].value);
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
        if (callee instanceof LangClosure closure) {
            if (argCount != closure.function.paramCount)
                runtimeException(String.format("Expected %d args, got %d", closure.function.paramCount, argCount));
            callStack.push(new CallFrame(closure, 0, stack.size() - argCount - 1));
        } else {
            runtimeException("Attempt to call non-closure value: " + callee);
        }
    }

    private void runtimeException(String message) {
        StringBuilder messageBuilder = new StringBuilder(message);
        for (CallFrame frame : callStack) {
            messageBuilder.append("\n in: ").append(frame.closure.function);
        }
        message = messageBuilder.toString();

        throw new RuntimeException(message);
    }

    private static class CallFrame {
        private LangClosure closure;
        private int ip; //instruction pointer
        private int fp; //frame pointer
        public CallFrame(LangClosure closure, int ip, int fp) {
            this.closure = closure;
            this.ip = ip;
            this.fp = fp;
        }
    }

}
