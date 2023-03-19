package language.bytecoderunner;

import language.Upvalue;

import java.util.*;

import static language.compile.Bytecode.*;

/**
 * Runs functions and maintains a global state
 */
public class Interpreter {

    //temp public
    public final Map<Class<?>, LangClass> classMap = new IdentityHashMap<>(); //keys are classes, identity works
    public final Map<String, Object> globals = new HashMap<>();

    private final List<Object> stack = new ArrayList<>();

    private final int MAX_STACK_FRAMES = 64;
    private final Deque<CallFrame> callStack = new ArrayDeque<>();
    private Upvalue upvalueList; //upvalues are a linked list

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
                    closeUpvalues(frame.fp-1);
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
                    if (makeCall(peek(argCount), argCount)) {
                        frame = callStack.peek();
                        curBytes = frame.closure.function.chunk.bytes;
                        constants = frame.closure.function.chunk.constants;
                    }
                }

                case CLOSURE -> {
                    LangClosure closure = new LangClosure((LangFunction) pop());
                    for (int i = 0; i < closure.upvalues.length; i++) {
                        boolean isLocal = curBytes[frame.ip++] > 0;
                        int index = curBytes[frame.ip++];
                        if (isLocal) {
                            closure.upvalues[i] = captureUpvalue(frame.fp + index);
                        } else {
                            closure.upvalues[i] = frame.closure.upvalues[index];
                        }
                    }
                    push(closure);
                }

                case SET_UPVALUE -> frame.closure.upvalues[curBytes[frame.ip++]].set(peek());
                case LOAD_UPVALUE -> push(frame.closure.upvalues[curBytes[frame.ip++]].get());
                case CLOSE_UPVALUE -> {
                    closeUpvalues(stack.size()-1);
                }

                case GET -> {
                    Object indexer = pop();
                    Object indexee = pop();
                    push(classMap.get(indexee.getClass()).get(indexee, indexer));
                }
                case SET -> {
                    Object val = pop();
                    Object indexer = pop();
                    Object indexee = pop();
                    classMap.get(indexee.getClass()).set(indexee, indexer, val);
                    push(val);
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

    //Returns true if this was a masque function, false if a java function
    private boolean makeCall(Object callee, int argCount) {
        if (callee instanceof LangClosure closure) {
            if (argCount != closure.function.paramCount)
                runtimeException(String.format("Expected %d args, got %d", closure.function.paramCount, argCount));
            if (callStack.size() == MAX_STACK_FRAMES)
                runtimeException("Stack overflow! More than the max stack frames of " + MAX_STACK_FRAMES);
            callStack.push(new CallFrame(closure, 0, stack.size() - argCount - 1));
            return true;
        } else if (callee instanceof JavaFunction jFunction) {
            if (jFunction.paramCount != argCount)
                runtimeException(String.format("Expected %d args, got %d", jFunction.paramCount, argCount));
            Object result = switch(argCount) {
                case 0 -> jFunction.invoke();
                case 1 -> jFunction.invoke(peek());
                case 2 -> jFunction.invoke(peek(1), peek());
                case 3 -> jFunction.invoke(peek(2), peek(1), peek());
                case 4 -> jFunction.invoke(peek(3), peek(2), peek(1), peek());
                case 5 -> jFunction.invoke(peek(4), peek(3), peek(2), peek(1), peek());
                case 6 -> jFunction.invoke(peek(5), peek(4), peek(3), peek(2), peek(1), peek());
                case 7 -> jFunction.invoke(peek(6), peek(5), peek(4), peek(3), peek(2), peek(1), peek());
                case 8 -> jFunction.invoke(peek(7), peek(6), peek(5), peek(4), peek(3), peek(2), peek(1), peek());
                case 9 -> jFunction.invoke(peek(8), peek(7), peek(6), peek(5), peek(4), peek(3), peek(2), peek(1), peek());
                case 10 -> jFunction.invoke(peek(9), peek(8), peek(7), peek(6), peek(5), peek(4), peek(3), peek(2), peek(1), peek());
                case 11 -> jFunction.invoke(peek(10), peek(9), peek(8), peek(7), peek(6), peek(5), peek(4), peek(3), peek(2), peek(1), peek());
                case 12 -> jFunction.invoke(peek(11), peek(10), peek(9), peek(8), peek(7), peek(6), peek(5), peek(4), peek(3), peek(2), peek(1), peek());
                case 13 -> jFunction.invoke(peek(12), peek(11), peek(10), peek(9), peek(8), peek(7), peek(6), peek(5), peek(4), peek(3), peek(2), peek(1), peek());
                case 14 -> jFunction.invoke(peek(13), peek(12), peek(11), peek(10), peek(9), peek(8), peek(7), peek(6), peek(5), peek(4), peek(3), peek(2), peek(1), peek());
                case 15 -> jFunction.invoke(peek(14), peek(13), peek(12), peek(11), peek(10), peek(9), peek(8), peek(7), peek(6), peek(5), peek(4), peek(3), peek(2), peek(1), peek());
                default -> throw new IllegalStateException("function has too many args??");
            };
            for (int i = 0; i < argCount; i++)
                pop();
            push(result);
            return false;
        } else {
            runtimeException("Attempt to call non-function value: " + callee);
        }
        return false;
    }

    private Upvalue captureUpvalue(int index) {
        //Search if an open upvalue already exists for this local
        Upvalue prev = null;
        Upvalue cur = upvalueList;

        while (cur != null && upvalueList.idx > index) {
            prev = cur;
            cur = cur.next;
        }
        if (cur != null && upvalueList.idx == index)
            return cur;

        Upvalue result = new Upvalue(stack, index);

        result.next = cur;
        if (prev == null) {
            upvalueList = result;
        } else {
            prev.next = result;
        }
        return result;
    }

    //Closes upvalues at or above the given stack index
    private void closeUpvalues(int index) {
        while (upvalueList != null && upvalueList.idx >= index) {
            upvalueList.close();
            upvalueList = upvalueList.next;
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
