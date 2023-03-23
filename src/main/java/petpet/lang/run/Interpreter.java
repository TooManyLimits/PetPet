package main.java.petpet.lang.run;

import main.java.petpet.types.PetPetList;
import main.java.petpet.types.PetPetString;
import main.java.petpet.types.PetPetTable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static main.java.petpet.lang.compile.Bytecode.*;

/**
 * Runs functions and maintains a global state
 */
public class Interpreter {

    //temp public
    public final Map<Class<?>, PetPetClass> classMap = new IdentityHashMap<>(); //keys are classes, identity works
    public final Map<String, Object> globals = new HashMap<>();

    private Object[] stack = new Object[16];
    private int stackTop = 0;

    public int maxStackFrames = 256; //256 default
    private CallFrame[] callStack = new CallFrame[8];
    private int callStackTop = 0;

    public Interpreter() {
        for (int i = 0; i < callStack.length; i++)
            callStack[i] = new CallFrame();
    }

    private Upvalue upvalueList; //upvalues form a linked list

    public int cost;

    public Object run(PetPetClosure closure, Object... args) {
        if (closure.function.paramCount != args.length)
            runtimeException("Expected " + closure.function.paramCount + " args, got " + args.length);
        push(closure);
        for (Object arg : args)
            push(arg);
        makeCall(closure, args.length, true, false);
        run();
        return pop();
    }

    private void run() {
        CallFrame frame = peekCallStack();
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
                case ADD -> {
                    Object r = pop();
                    Object l = pop();
                    if (l instanceof String s)
                        push(s + PetPetString.valueOf(r));
                    else if (r instanceof String s)
                        push(PetPetString.valueOf(l) + s);
                    else
                        push((Double) l + (Double) r);
                }
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
                case NOT -> push(isFalsy(pop()));

                case PRINT -> System.out.println(pop());
                case RETURN -> {
                    Object result = pop();
                    closeUpvalues(frame.fp-1);
                    popCallStack();

                    while (frame.fp < stackTop) stackTop--;
                    push(result);

                    if (frame.wasJavaCall) return; //return for real
                    frame = peekCallStack();
                    curBytes = frame.closure.function.chunk.bytes;
                    constants = frame.closure.function.chunk.constants;
                }

                case SET_GLOBAL -> globals.put((String) constants[curBytes[frame.ip++]], peek());
                case LOAD_GLOBAL -> push(globals.get((String) constants[curBytes[frame.ip++]]));

                case SET_LOCAL -> stack[frame.fp+curBytes[frame.ip++]] = peek();
                case LOAD_LOCAL -> push(stack[frame.fp+curBytes[frame.ip++]]);

                case POP_OFFSET_1 -> stack[stackTop-2] = stack[--stackTop];

                case JUMP -> {int offset = ((curBytes[frame.ip++] << 8) | (curBytes[frame.ip++])); frame.ip += offset; }
                case JUMP_IF_FALSE -> {int offset = ((curBytes[frame.ip++] << 8) | (curBytes[frame.ip++])); if (isFalsy(peek())) frame.ip += offset;}
                case JUMP_IF_TRUE -> {int offset = ((curBytes[frame.ip++] << 8) | (curBytes[frame.ip++])); if (isTruthy(peek())) frame.ip += offset;}

                case NEW_LIST -> push(new PetPetList());
                case LIST_ADD -> ((PetPetList) peek(1)).add(pop());

                case NEW_TABLE -> push(new PetPetTable());
                case TABLE_SET -> ((PetPetTable) peek(2)).put(pop(), pop()); //value was pushed, then key

                case CALL -> {
                    int argCount = curBytes[frame.ip++];
                    if (makeCall(peek(argCount), argCount, false, false)) {
                        frame = peekCallStack();
                        curBytes = frame.closure.function.chunk.bytes;
                        constants = frame.closure.function.chunk.constants;
                    }
                }

                case CLOSURE -> {
                    PetPetClosure closure = new PetPetClosure((PetPetFunction) pop(), this);
                    for (int i = 0; i < closure.upvalues.length; i++) {
                        boolean isLocal = curBytes[frame.ip++] > 0;
                        int index = curBytes[frame.ip++];
                        if (isLocal) {
                            closure.upvalues[i] = captureUpvalue(frame.fp + index);
                        } else {
                            closure.upvalues[i] = frame.closure.upvalues[index];
                        }
                    }
                    pushNoCheck(closure);
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
                    closeUpvalues(stackTop-1);
                }

                case GET -> {
                    Object indexer = peek();
                    Object instance = peek(1);
                    if (instance == null)
                        runtimeException("Attempt to get from null value");

                    PetPetClass langClass = classMap.get(instance.getClass());

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
                    String indexerTypeName;
                    if (indexer == null) {
                        indexerTypeName = "null";
                    } else {
                        PetPetClass indexerClass = classMap.get(indexer.getClass());
                        if (indexerClass == null)
                            runtimeException("Environment error: java object of type " + indexer.getClass() + " is in the environment, but it has no PetPetClass associated.");
                        indexerTypeName = indexerClass.name;
                    }

                    String specialString = "__get_" + indexerTypeName;
                    Object getMethod = langClass.methods.get(specialString);
                    if (getMethod != null) {
                        makeCall(getMethod, 2, false, true);
                        break;
                    }
                    cost++;
                    getMethod = langClass.methods.get("__get");
                    if (getMethod != null) {
                        makeCall(getMethod, 2, false, true);
                        break;
                    }
                    runtimeException("Tried to get from " + instance + " with illegal key " + indexer);
                }
                case SET -> {
                    Object value = peek();
                    Object indexer = peek(1);
                    Object instance = peek(2);
                    if (instance == null)
                        runtimeException("Attempt to set key " + PetPetString.valueOf(indexer) + " on null value");

                    PetPetClass langClass = classMap.get(instance.getClass());

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

                    String indexerTypeName;
                    if (indexer == null) {
                        indexerTypeName = "null";
                    } else {
                        PetPetClass indexerClass = classMap.get(indexer.getClass());
                        if (indexerClass == null)
                            runtimeException("Environment error: java object of type " + indexer.getClass() + " is in the environment, but it has no PetPetClass associated.");
                        indexerTypeName = classMap.get(indexer.getClass()).name;
                    }

                    String specialString = "__set_" + indexerTypeName;
                    Object setMethod = langClass.methods.get(specialString);
                    if (setMethod != null) {
                        makeCall(setMethod, 3, false, true);
                        break;
                    }
                    setMethod = langClass.methods.get("__set");
                    if (setMethod != null) {
                        makeCall(setMethod, 3, false, true);
                        break;
                    }
                    runtimeException("Tried to set to " + instance + " with illegal key " + indexer);
                }

                case INVOKE -> {
                    int argCount = curBytes[frame.ip++];

                    Object indexer = peek(argCount);
                    Object instance = peek(argCount+1);

                    System.arraycopy(stack, stackTop-argCount, stack, (stackTop--)-argCount-1, argCount+1);
                    if (instance == null)
                        runtimeException("Attempt to invoke method on null value (key = " + indexer + ")");
                    PetPetClass langClass = classMap.get(instance.getClass());
                    if (langClass == null)
                        runtimeException("Invalid environment - object " + instance + "has no class. Contact developers of the application! (not petpet's fault... probably)");
                    if (indexer instanceof String name) {
                        Object method = langClass.methods.get(name);
                        if (method == null)
                            runtimeException("Method " + name + " does not exist for type " + langClass.name);
                        makeCall(method, argCount+1, false, true);
                        break;
                    }
                    runtimeException("Attempt to invoke " + instance + " with non-string method name, " + indexer);
                }
            }
        }
    }

    public boolean isFalsy(Object o) {
        return o == Boolean.FALSE || o == null || (o instanceof Double d && d == 0);
    }

    public boolean isTruthy(Object o) {
        return !isFalsy(o);
    }

    private void pushNoCheck(Object o) {
        stack[stackTop++] = o;
    }

    private void push(Object o) {
        if (stackTop == stack.length) {
            Object[] newStack = new Object[stackTop * 2];
            System.arraycopy(stack, 0, newStack, 0, stackTop);
            stack = newStack;
        }
        stack[stackTop++] = o;
    }

    private Object pop() {
        return stack[--stackTop];
    }

    public Object peek() {
        return stack[stackTop-1];
    }

    public Object peek(int offset) {
        return stack[stackTop-1-offset];
    }

    public Object get(int index) {
        return stack[index];
    }

    public void set(int index, Object value) {
        stack[index] = value;
    }

    public CallFrame peekCallStack() {
        return callStack[callStackTop-1];
    }

    public void popCallStack() {
        callStackTop--;
    }

    public void pushCallStack(PetPetClosure closure, int ip, int fp, boolean calledFromJava) {
        if (callStackTop == callStack.length) {
            CallFrame[] newStack = new CallFrame[callStackTop * 2];
            System.arraycopy(callStack, 0, newStack, 0, callStackTop);
            for (int i = callStackTop; i < newStack.length; i++)
                newStack[i] = new CallFrame();
            callStack = newStack;
        }
        CallFrame frame = callStack[callStackTop++];
        frame.closure = closure;
        frame.ip = ip;
        frame.fp = fp;
        frame.wasJavaCall = calledFromJava;
    }

    //Returns true if this was a petpet function, false if a java function
    private boolean makeCall(Object callee, int argCount, boolean calledFromJava, boolean isInvocation) {
//        System.out.println(stack);
        if (callee == null)
            runtimeException("Attempt to call null value");
        if (callee instanceof PetPetClosure closure) {
            if (argCount != closure.function.paramCount)
                runtimeException(String.format("Expected %d args, got %d", closure.function.paramCount, argCount));
            if (callStackTop == maxStackFrames)
                runtimeException("Stack overflow! More than the max stack frames of " + maxStackFrames);
            pushCallStack(closure, 0, stackTop-argCount-1, calledFromJava);
            return true;
        } else if (callee instanceof JavaFunction jFunction) {
            if (jFunction.paramCount != argCount)
                runtimeException(String.format("Expected %d args, got %d", jFunction.paramCount, argCount));
            if (jFunction.costPenalizer != null)
                cost += jFunction.costPenalizer.applyAsInt(this);
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
                int numToPop = isInvocation ? argCount : argCount + 1;
                for (int i = 0; i < numToPop; i++)
                    pop();
                cost += argCount;
                push(result);
            } catch (Exception e) {
                e.printStackTrace();
                runtimeException("Java exception occurred: " + e.getMessage());
            }
            return false;
        } else {
            runtimeException("Attempt to call non-callable value: " + callee);
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

        Upvalue result = new Upvalue(this, index);

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
        for (int i = 0; i < callStackTop; i++) {
            CallFrame frame = callStack[i];
            messageBuilder.append("\n in: ")
                    .append(frame.closure.function)
                    .append(" at line ")
                    .append(frame.lineNumber());
        }
        message = messageBuilder.toString();

        throw new PetPetException(message);
    }

    private static class CallFrame {
        private PetPetClosure closure;
        private int ip; //instruction pointer
        private int fp; //frame pointer
        private boolean wasJavaCall; //whether this function was called from java itself, or inside the function

        public CallFrame() {
            //defaults
        }
//        public CallFrame(PetPetClosure closure, int ip, int fp, boolean wasJavaCall) {
//            this.closure = closure;
//            this.ip = ip;
//            this.fp = fp;
//            this.wasJavaCall = wasJavaCall;
//        }

        public int lineNumber() {
            int[] lines = closure.function.lineNumberTable;
            int index = 0;
            while (++index < lines.length && lines[index] <= ip);
            return index + 0;//closure.function.lineNumberOffset;
        }
    }

}
