package language.run;

import language.Upvalue;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

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

    public int cost;

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
            cost++;
//            System.out.println(NAMES[curBytes[frame.ip]]);
            switch (curBytes[frame.ip++]) {
                case CONSTANT -> push(constants[curBytes[frame.ip++]]);
                case PUSH_NULL -> push(null);
                case POP -> pop();

                //Temp operators
                case ADD -> push((Double) pop() + (Double) pop());
                case SUB -> {double r = (Double) pop(); double l = (Double) pop(); push(l-r);}
                case MUL -> push((Double) pop() * (Double) pop());
                case DIV -> {double r = (Double) pop(); double l = (Double) pop(); push(l/r);}
                case MOD -> {double r = (Double) pop(); double l = (Double) pop(); push(l%r);}
                case EQ -> push(Objects.equals(pop(), pop()));
                case NEQ -> push(!Objects.equals(pop(), pop()));
                case LT -> push((Double) pop() > (Double) pop());
                case GT -> push((Double) pop() < (Double) pop());
                case LTE -> push((Double) pop() >= (Double) pop());
                case GTE -> push((Double) pop() <= (Double) pop());

                case NEGATE -> push(-(Double) pop());

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
                case JUMP_IF_FALSE -> {int offset = ((curBytes[frame.ip++] << 8) | (curBytes[frame.ip++])); if (isFalsy(peek())) frame.ip += offset;}
                case JUMP_IF_TRUE -> {int offset = ((curBytes[frame.ip++] << 8) | (curBytes[frame.ip++])); if (isTruthy(peek())) frame.ip += offset;}

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

                case SET_UPVALUE -> {
                    cost++; //upvalues more expensive than locals
                    frame.closure.upvalues[curBytes[frame.ip++]].set(peek());
                }
                case LOAD_UPVALUE -> {
                    cost++;
                    push(frame.closure.upvalues[curBytes[frame.ip++]].get());
                }
                case CLOSE_UPVALUE -> {
                    closeUpvalues(stack.size()-1);
                }

                case GET -> {
                    Object indexer = peek();
                    Object instance = peek(1);
                    LangClass langClass = classMap.get(instance.getClass());

                    if (indexer instanceof String name) {
                        Function field = langClass.fieldGetters.get(name);
                        if (field != null)  {
                            pop(); pop();
                            try {
                                Object result = field.apply(instance);
                                if (result instanceof Number n)
                                    push(n.doubleValue());
                                else push(result);
                            } catch (Exception e) {
                                runtimeException(e.toString());
                            }
                            break;
                        }
                        cost++;
                    }

                    String indexerTypeName = classMap.get(indexer.getClass()).name;
                    String specialString = "__get_" + indexerTypeName;
                    Object getMethod = langClass.methods.get(specialString);
                    if (getMethod != null) {
                        makeCall(getMethod, 2);
                        break;
                    }
                    cost++;
                    getMethod = langClass.methods.get("__get");
                    if (getMethod != null) {
                        makeCall(getMethod, 2);
                        break;
                    }
                    runtimeException("Tried to get from " + instance + " with illegal key " + indexer);
                }
                case SET -> {
                    Object value = peek();
                    Object indexer = peek(1);
                    Object instance = peek(2);
                    LangClass langClass = classMap.get(instance.getClass());

                    if (indexer instanceof String name) {
                        BiConsumer field = langClass.fieldSetters.get(name);
                        if (field != null) {
                            pop(); pop(); pop();
                            try {
                                field.accept(instance, value);
                            } catch (Exception e) {
                                runtimeException(e.toString());
                            }
                            push(value);
                            break;
                        }
                    }

                    String indexerTypeName = classMap.get(indexer.getClass()).name;
                    String specialString = "__set_" + indexerTypeName;
                    Object setMethod = langClass.methods.get(specialString);
                    if (setMethod != null) {
                        makeCall(setMethod, 3);
                        break;
                    }
                    setMethod = langClass.methods.get("__set");
                    if (setMethod != null) {
                        makeCall(setMethod, 3);
                        break;
                    }
                    runtimeException("Tried to set to " + instance + " with illegal key " + indexer);
                }

                case INVOKE -> {
                    int argCount = curBytes[frame.ip++];

                    Object indexer = peek(argCount);
                    Object instance = peek(argCount+1);
                    stack.remove(stack.size()-1-argCount);
                    LangClass langClass = classMap.get(instance.getClass());
                    if (indexer instanceof String name) {
                        Object method = langClass.methods.get(name);
                        makeCall(method, argCount+1);
                        break;
                    }
                    runtimeException("Attempt to invoke " + instance + " with non-string method name, " + indexer);
                }
            }
        }
    }

    private boolean isFalsy(Object o) {
        return o == Boolean.FALSE || o == null || (o instanceof Double d && d == 0);
    }

    private boolean isTruthy(Object o) {
        return !isFalsy(o);
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
            try {
                Object result;
                if (jFunction.needsNumberConversion()) {
                    result = switch(argCount) {
                        case 0 -> jFunction.invoke();
                        case 1 -> jFunction.invoke(jFunction.castNumber(peek(), 0));
                        case 2 -> jFunction.invoke(jFunction.castNumber(peek(1), 0), jFunction.castNumber(peek(), 1));
                        case 3 -> jFunction.invoke(jFunction.castNumber(peek(2), 0), jFunction.castNumber(peek(1), 1), jFunction.castNumber(peek(), 2));
                        case 4 -> jFunction.invoke(jFunction.castNumber(peek(3), 0), jFunction.castNumber(peek(2), 1), jFunction.castNumber(peek(1), 2), jFunction.castNumber(peek(), 3));
                        case 5 -> jFunction.invoke(jFunction.castNumber(peek(4), 0), jFunction.castNumber(peek(3), 1), jFunction.castNumber(peek(2), 2), jFunction.castNumber(peek(1), 3), jFunction.castNumber(peek(), 4));
                        case 6 -> jFunction.invoke(jFunction.castNumber(peek(5), 0), jFunction.castNumber(peek(4), 1), jFunction.castNumber(peek(3), 2), jFunction.castNumber(peek(2), 3), jFunction.castNumber(peek(1), 4), jFunction.castNumber(peek(), 5));
                        case 7 -> jFunction.invoke(jFunction.castNumber(peek(6), 0), jFunction.castNumber(peek(5), 1), jFunction.castNumber(peek(4), 2), jFunction.castNumber(peek(3), 3), jFunction.castNumber(peek(2), 4), jFunction.castNumber(peek(1), 5), jFunction.castNumber(peek(), 6));
                        case 8 -> jFunction.invoke(jFunction.castNumber(peek(7), 0), jFunction.castNumber(peek(6), 1), jFunction.castNumber(peek(5), 2), jFunction.castNumber(peek(4), 3), jFunction.castNumber(peek(3), 4), jFunction.castNumber(peek(2), 5), jFunction.castNumber(peek(1), 6), jFunction.castNumber(peek(), 7));
                        case 9 -> jFunction.invoke(jFunction.castNumber(peek(8), 0), jFunction.castNumber(peek(7), 1), jFunction.castNumber(peek(6), 2), jFunction.castNumber(peek(5), 3), jFunction.castNumber(peek(4), 4), jFunction.castNumber(peek(3), 5), jFunction.castNumber(peek(2), 6), jFunction.castNumber(peek(1), 7), jFunction.castNumber(peek(), 8));
                        case 10 -> jFunction.invoke(jFunction.castNumber(peek(9), 0), jFunction.castNumber(peek(8), 1), jFunction.castNumber(peek(7), 2), jFunction.castNumber(peek(6), 3), jFunction.castNumber(peek(5), 4), jFunction.castNumber(peek(4), 5), jFunction.castNumber(peek(3), 6), jFunction.castNumber(peek(2), 7), jFunction.castNumber(peek(1), 8), jFunction.castNumber(peek(), 9));
                        case 11 -> jFunction.invoke(jFunction.castNumber(peek(10), 0), jFunction.castNumber(peek(9), 1), jFunction.castNumber(peek(8), 2), jFunction.castNumber(peek(7), 3), jFunction.castNumber(peek(6), 4), jFunction.castNumber(peek(5), 5), jFunction.castNumber(peek(4), 6), jFunction.castNumber(peek(3), 7), jFunction.castNumber(peek(2), 8), jFunction.castNumber(peek(1), 9), jFunction.castNumber(peek(), 10));
                        case 12 -> jFunction.invoke(jFunction.castNumber(peek(11), 0), jFunction.castNumber(peek(10), 1), jFunction.castNumber(peek(9), 2), jFunction.castNumber(peek(8), 3), jFunction.castNumber(peek(7), 4), jFunction.castNumber(peek(6), 5), jFunction.castNumber(peek(5), 6), jFunction.castNumber(peek(4), 7), jFunction.castNumber(peek(3), 8), jFunction.castNumber(peek(2), 9), jFunction.castNumber(peek(1), 10), jFunction.castNumber(peek(), 11));
                        case 13 -> jFunction.invoke(jFunction.castNumber(peek(12), 0), jFunction.castNumber(peek(11), 1), jFunction.castNumber(peek(10), 2), jFunction.castNumber(peek(9), 3), jFunction.castNumber(peek(8), 4), jFunction.castNumber(peek(7), 5), jFunction.castNumber(peek(6), 6), jFunction.castNumber(peek(5), 7), jFunction.castNumber(peek(4), 8), jFunction.castNumber(peek(3), 9), jFunction.castNumber(peek(2), 10), jFunction.castNumber(peek(1), 11), jFunction.castNumber(peek(), 12));
                        case 14 -> jFunction.invoke(jFunction.castNumber(peek(13), 0), jFunction.castNumber(peek(12), 1), jFunction.castNumber(peek(11), 2), jFunction.castNumber(peek(10), 3), jFunction.castNumber(peek(9), 4), jFunction.castNumber(peek(8), 5), jFunction.castNumber(peek(7), 6), jFunction.castNumber(peek(6), 7), jFunction.castNumber(peek(5), 8), jFunction.castNumber(peek(4), 9), jFunction.castNumber(peek(3), 10), jFunction.castNumber(peek(2), 11), jFunction.castNumber(peek(1), 12), jFunction.castNumber(peek(), 13));
                        case 15 -> jFunction.invoke(jFunction.castNumber(peek(14), 0), jFunction.castNumber(peek(13), 1), jFunction.castNumber(peek(12), 2), jFunction.castNumber(peek(11), 3), jFunction.castNumber(peek(10), 4), jFunction.castNumber(peek(9), 5), jFunction.castNumber(peek(8), 6), jFunction.castNumber(peek(7), 7), jFunction.castNumber(peek(6), 8), jFunction.castNumber(peek(5), 9), jFunction.castNumber(peek(4), 10), jFunction.castNumber(peek(3), 11), jFunction.castNumber(peek(2), 12), jFunction.castNumber(peek(1), 13), jFunction.castNumber(peek(), 14));
                        default -> throw new IllegalStateException("function has too many args??");
                    };
                } else {
                    result = switch(argCount) {
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
                }

                if (result instanceof Number n)
                    result = n.doubleValue();

                for (int i = 0; i < argCount; i++)
                    pop();
                cost += argCount;
                push(result);
            } catch (Exception e) {
                e.printStackTrace();
                runtimeException(e.toString());
            }
            return false;
        } else {
            runtimeException("Attempt to call non-function value: " + callee);
        }
        return false;
    }

    private Upvalue captureUpvalue(int index) {
        cost++; //cost for capturing an upvalue
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
            messageBuilder.append("\n in: ")
                    .append(frame.closure.function)
                    .append(" at line ")
                    .append(frame.lineNumber());
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

        public int lineNumber() {
            int[] lines = closure.function.lineNumberTable;
            int index = 0;
            while (++index < lines.length && lines[index] <= ip);
            return index + closure.function.lineNumberOffset;
        }
    }

}