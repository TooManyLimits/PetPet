package petpet.lang.run;

import petpet.types.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static petpet.lang.compile.Bytecode.*;

/**
 * Runs functions and maintains a global state
 */
public class Interpreter {

    //temp public
    public final Map<Class<?>, PetPetClass> classMap = new IdentityHashMap<>(); //keys are classes, identity works
    public final Map<String, Object> globals = new PetPetTable<>();

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
//        if (closure.function.paramCount != args.length)
//            runtimeException("Expected " + closure.function.paramCount + " args, got " + args.length);
        push(closure);
        for (Object arg : args)
            push(arg);
        makeCall(closure, args.length, true, false);
        run();
        return pop();
    }

    //Unsigned Byte: curBytes[frame.ip++] & 0xff
    //Signed Short: (short) (((curBytes[frame.ip++] & 0xff) << 8) + (curBytes[frame.ip++] & 0xff))
    //Unsigned short: (((curBytes[frame.ip++] << 8) & 0xffff) | (curBytes[frame.ip++] & 0xff)) & 0xffff

    private void run() {
        CallFrame frame = peekCallStack();
        byte[] curBytes = frame.closure.function.chunk.bytes;
        Object[] constants = frame.closure.function.chunk.constants;
        while (true) {
            cost++;
//            System.out.println(NAMES[curBytes[frame.ip]]);
            switch (curBytes[frame.ip++]) {
                case CONSTANT -> push(constants[curBytes[frame.ip++] & 0xff]);
                case BIG_CONSTANT -> push(constants[(((curBytes[frame.ip++] << 8) & 0xff) | (curBytes[frame.ip++] & 0xff)) & 0xffff]);

                case PUSH_NULL -> push(null);
                case POP -> pop();

                //Temp operators
                case ADD -> {
                    Object r = pop();
                    Object l = pop();
                    if (l instanceof String s)
                        pushNoCheck(s + getString(r));
                    else if (r instanceof String s)
                        pushNoCheck(getString(l) + s);
                    else if (l instanceof Double dl && r instanceof Double dr)
                        pushNoCheck(dl + dr);
                    else {
                        if (callMetaBinary(l, r, "add")) {
                            frame = peekCallStack();
                            curBytes = frame.closure.function.chunk.bytes;
                            constants = frame.closure.function.chunk.constants;
                        }
                    }
                }
                case SUB -> {
                    Object r = pop();
                    Object l = pop();
                    if (l instanceof Double dl && r instanceof Double dr)
                        pushNoCheck(dl - dr);
                    else if (callMetaBinary(l, r, "sub")) {
                        frame = peekCallStack();
                        curBytes = frame.closure.function.chunk.bytes;
                        constants = frame.closure.function.chunk.constants;
                    }
                }
                case MUL -> {
                    Object r = pop();
                    Object l = pop();
                    if (l instanceof Double dl && r instanceof Double dr)
                        pushNoCheck(dl * dr);
                    else if (callMetaBinary(l, r, "mul")) {
                        frame = peekCallStack();
                        curBytes = frame.closure.function.chunk.bytes;
                        constants = frame.closure.function.chunk.constants;
                    }
                }
                case DIV -> {
                    Object r = pop();
                    Object l = pop();
                    if (l instanceof Double dl && r instanceof Double dr)
                        pushNoCheck(dl / dr);
                    else if (callMetaBinary(l, r, "div")) {
                        frame = peekCallStack();
                        curBytes = frame.closure.function.chunk.bytes;
                        constants = frame.closure.function.chunk.constants;
                    }
                }
                case MOD -> {
                    Object r = pop();
                    Object l = pop();
                    if (l instanceof Double dl && r instanceof Double dr)
                        pushNoCheck(dl % dr);
                    else if (callMetaBinary(l, r, "mod")) {
                        frame = peekCallStack();
                        curBytes = frame.closure.function.chunk.bytes;
                        constants = frame.closure.function.chunk.constants;
                    }
                }
                case EQ -> push(Objects.equals(pop(), pop()));
                case NEQ -> push(!Objects.equals(pop(), pop()));
                case LT -> {
                    Object r = pop();
                    Object l = pop();
                    if (l instanceof Double dl && r instanceof Double dr)
                        pushNoCheck(dl < dr);
                    else if (callMetaBinary(l, r, "lt")) {
                        frame = peekCallStack();
                        curBytes = frame.closure.function.chunk.bytes;
                        constants = frame.closure.function.chunk.constants;
                    }
                }
                case GT -> {
                    Object r = pop();
                    Object l = pop();
                    if (l instanceof Double dl && r instanceof Double dr)
                        pushNoCheck(dl > dr);
                    else if (callMetaBinary(l, r, "gt")) {
                        frame = peekCallStack();
                        curBytes = frame.closure.function.chunk.bytes;
                        constants = frame.closure.function.chunk.constants;
                    }
                }
                case LTE -> {
                    Object r = pop();
                    Object l = pop();
                    if (l instanceof Double dl && r instanceof Double dr)
                        pushNoCheck(dl <= dr);
                    else if (callMetaBinary(l, r, "lte")) {
                        frame = peekCallStack();
                        curBytes = frame.closure.function.chunk.bytes;
                        constants = frame.closure.function.chunk.constants;
                    }
                }
                case GTE -> {
                    Object r = pop();
                    Object l = pop();
                    if (l instanceof Double dl && r instanceof Double dr)
                        pushNoCheck(dl >= dr);
                    else if (callMetaBinary(l, r, "gte")) {
                        frame = peekCallStack();
                        curBytes = frame.closure.function.chunk.bytes;
                        constants = frame.closure.function.chunk.constants;
                    }
                }

                case NEGATE -> {
                    Object o = pop();
                    if (o instanceof Double dl)
                        pushNoCheck(-dl);
                    else if (callMetaUnary(o, "neg")) {
                        frame = peekCallStack();
                        curBytes = frame.closure.function.chunk.bytes;
                        constants = frame.closure.function.chunk.constants;
                    }
                }
                case NOT -> pushNoCheck(isFalsy(pop()));

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

                case SET_GLOBAL -> globals.put((String) constants[curBytes[frame.ip++] & 0xff], peek());
                case BIG_SET_GLOBAL -> globals.put((String) constants[(((curBytes[frame.ip++] << 8) & 0xffff) | (curBytes[frame.ip++] & 0xff)) & 0xffff], peek());

                case LOAD_GLOBAL -> push(globals.get((String) constants[curBytes[frame.ip++] & 0xff]));
                case BIG_LOAD_GLOBAL -> push(globals.get((String) constants[(((curBytes[frame.ip++] << 8) & 0xffff) | (curBytes[frame.ip++] & 0xff)) & 0xffff]));

                case SET_LOCAL -> stack[frame.fp+(curBytes[frame.ip++] & 0xff)] = peek();
                case BIG_SET_LOCAL -> stack[frame.fp+((((curBytes[frame.ip++] << 8) & 0xffff) | (curBytes[frame.ip++] & 0xff)) & 0xffff)] = peek();

                case LOAD_LOCAL -> push(stack[frame.fp+(curBytes[frame.ip++] & 0xff)]);
                case BIG_LOAD_LOCAL -> push(stack[frame.fp+((((curBytes[frame.ip++] << 8) & 0xffff) | (curBytes[frame.ip++] & 0xff)) & 0xffff)]);

                case POP_OFFSET_1 -> stack[stackTop-2] = pop();

                //Do not make the jumps unsigned
                case JUMP -> {int offset = (short) (((curBytes[frame.ip++] & 0xff) << 8) + (curBytes[frame.ip++] & 0xff)); frame.ip += offset; }
                case JUMP_IF_FALSE -> {int offset = (short) (((curBytes[frame.ip++] & 0xff) << 8) + (curBytes[frame.ip++] & 0xff)); if (isFalsy(peek())) frame.ip += offset;}
                case JUMP_IF_TRUE -> {int offset = (short) (((curBytes[frame.ip++] & 0xff) << 8) + (curBytes[frame.ip++] & 0xff)); if (isTruthy(peek())) frame.ip += offset;}

                case NEW_LIST -> push(new PetPetList());
                case LIST_ADD -> ((PetPetList) peek(1)).add(pop());

                case NEW_TABLE -> push(new PetPetTable());
                case TABLE_SET -> ((PetPetTable) peek(2)).put(pop(), pop()); //value was pushed, then key

                case CALL -> {
                    int argCount = curBytes[frame.ip++] & 0xff;
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
                        int index = curBytes[frame.ip++] & 0xff;
                        if (isLocal) {
                            closure.upvalues[i] = captureUpvalue(frame.fp + index);
                        } else {
                            closure.upvalues[i] = frame.closure.upvalues[index];
                        }
                    }
                    pushNoCheck(closure);
                }

                case BIG_CLOSURE -> {
                    PetPetClosure closure = new PetPetClosure((PetPetFunction) pop(), this);
                    for (int i = 0; i < closure.upvalues.length; i++) {
                        boolean isLocal = curBytes[frame.ip++] > 0;
                        int index = (((curBytes[frame.ip++] << 8) & 0xffff) | (curBytes[frame.ip++] & 0xff)) & 0xffff; //Short read instead of byte, only difference
                        if (isLocal) {
                            closure.upvalues[i] = captureUpvalue(frame.fp + index);
                        } else {
                            closure.upvalues[i] = frame.closure.upvalues[index];
                        }
                    }
                    pushNoCheck(closure);
                }

                case SET_UPVALUE -> {
                    //cost++; //upvalues more expensive than locals
                    frame.closure.upvalues[curBytes[frame.ip++] & 0xff].set(peek());
                }
                case BIG_SET_UPVALUE -> {
                    //cost++; //upvalues more expensive than locals
                    frame.closure.upvalues[(((curBytes[frame.ip++] << 8) & 0xffff) | (curBytes[frame.ip++] & 0xff)) & 0xffff].set(peek());
                }
                case LOAD_UPVALUE -> {
                    //cost++;
                    push(frame.closure.upvalues[curBytes[frame.ip++] & 0xff].get());
                }
                case BIG_LOAD_UPVALUE -> {
                    //cost++;
                    push(frame.closure.upvalues[(((curBytes[frame.ip++] << 8) & 0xffff) | (curBytes[frame.ip++] & 0xff)) & 0xffff].get());
                }
                case CLOSE_UPVALUE -> {
                    closeUpvalues(stackTop-2); //element on top of the stack is the result of the block expression
                }

                case GET -> {
                    Object indexer = peek();
                    Object instance = peek(1);
                    if (instance == null)
                        runtimeException("Attempt to get from null value with key: " + getString(indexer));

                    PetPetClass langClass = getPetPetClass(instance);

                    if (indexer instanceof String name) {
                        Function field = langClass.fieldGetters.get(name);
                        if (field != null)  {
                            pop(); pop();
                            try {
                                Object result = field.apply(instance);
                                if (result instanceof Number n)
                                    pushNoCheck(n.doubleValue());
                                else pushNoCheck(result);
                            } catch (Exception e) {
                                runtimeException("Should never happen unless poorly made environment function: " + e);
                            }
                            break;
                        }
                        cost++;
                    }
                    String indexerTypeName = getPetPetClass(indexer).name;

                    String specialString = "__get_" + indexerTypeName;
                    Object getMethod = langClass.methods.get(specialString);
                    if (getMethod != null) {
                        if (makeCall(getMethod, 2, false, true)) {
                            frame = peekCallStack();
                            curBytes = frame.closure.function.chunk.bytes;
                            constants = frame.closure.function.chunk.constants;
                        }
                        break;
                    }
                    cost++;
                    getMethod = langClass.methods.get("__get");
                    if (getMethod != null) {
                        if (makeCall(getMethod, 2, false, true)) {
                            frame = peekCallStack();
                            curBytes = frame.closure.function.chunk.bytes;
                            constants = frame.closure.function.chunk.constants;
                        }
                        break;
                    }
                    runtimeException("Tried to get from (" + getString(instance) + ") with illegal key (" + getString(indexer) + ")");
                }
                case SET -> {
                    Object value = peek();
                    Object indexer = peek(1);
                    Object instance = peek(2);
                    if (instance == null)
                        runtimeException("Attempt to set on null value with key: " + getString(indexer));

                    PetPetClass langClass = getPetPetClass(instance);

                    if (indexer instanceof String name) {
                        BiConsumer field = langClass.fieldSetters.get(name);
                        if (field != null) {
                            pop(); pop(); pop();
                            try {
                                field.accept(instance, value);
                            } catch (Exception e) {
                                runtimeException(e.getMessage());
                            }
                            pushNoCheck(value);
                            break;
                        }
                    }

                    String indexerTypeName = getPetPetClass(indexer).name;

                    String specialString = "__set_" + indexerTypeName;
                    Object setMethod = langClass.methods.get(specialString);
                    if (setMethod != null) {
                        if (makeCall(setMethod, 3, false, true)) {
                            frame = peekCallStack();
                            curBytes = frame.closure.function.chunk.bytes;
                            constants = frame.closure.function.chunk.constants;
                        }
                        break;
                    }
                    setMethod = langClass.methods.get("__set");
                    if (setMethod != null) {
                        if (makeCall(setMethod, 3, false, true)) {
                            frame = peekCallStack();
                            curBytes = frame.closure.function.chunk.bytes;
                            constants = frame.closure.function.chunk.constants;
                        }
                        break;
                    }
                    runtimeException("Tried to set to " + instance + " with illegal key " + indexer);
                }

                case INVOKE -> {
                    int argCount = curBytes[frame.ip++];

                    Object indexer = peek(argCount);
                    Object instance = peek(argCount+1);

                    if (doInvoke(argCount, instance, indexer)) {
                        frame = peekCallStack();
                        curBytes = frame.closure.function.chunk.bytes;
                        constants = frame.closure.function.chunk.constants;
                    }
                }
            }
        }
    }

    private boolean callMetaBinary(Object l, Object r, String name) {
        //This function always runs after popping twice, so we have at least 2 spaces on the call stack left

        PetPetClass leftClass = getPetPetClass(l);
        PetPetClass rightClass = getPetPetClass(r);

        String underscored = "__" + name;
        String underscoredR = underscored + "R";

        Boolean done;

        done = metaBinaryHelper(l, r, leftClass.methods.get(underscored + "_" + rightClass.name));
        if (done != null) return done;
        done = metaBinaryHelper(l, r, leftClass.methods.get(underscored));
        if (done != null) return done;
        done = metaBinaryHelper(r, l, leftClass.methods.get(underscoredR + "_" + leftClass.name));
        if (done != null) return done;
        done = metaBinaryHelper(r, l, leftClass.methods.get(underscoredR));
        if (done != null) return done;

        runtimeException("Cannot " + name + " types " + leftClass.name + " and " + rightClass.name);
        return false; //return value unimportant
    }

    //return null if nothing happened, return false if it was a java function, return true if petpet function
    private Boolean metaBinaryHelper(Object first, Object second, Object func) {
        if (func != null) {
            pushNoCheck(first);
            pushNoCheck(second);
            return makeCall(func, 2, false, true);
        }
        return null;
    }

    private boolean callMetaUnary(Object o, String name) {
        //Called after popping 1 arg, so we have 1 arg of space on the stack, can pushNoCheck
        PetPetClass objClass = getPetPetClass(o);
        PetPetCallable func = objClass.methods.get("__" + name);
        if (func != null) {
            pushNoCheck(o);
            return makeCall(func, 1, false, true);
        }
        runtimeException("Cannot perform operator " + name + " on type " + objClass.name);
        return false; //doesnt matter
    }

    /**
     * Objects on the stack are:
     * lastArg
     * ...
     * firstArg
     * indexer
     * instance
     *
     * return true if it was a petpet function
     */
    private boolean doInvoke(int argCount, Object instance, Object indexer) {
        System.arraycopy(stack, stackTop-argCount, stack, (stackTop--)-argCount-1, argCount);
        if (instance == null)
            runtimeException("Attempt to invoke method on null value (key = " + indexer + ")");
        PetPetClass langClass = getPetPetClass(instance);
        if (indexer instanceof String name) {
            //First try with _argCount
            Object method = langClass.methods.get(name + "_" + argCount);
            if (method != null) {
                return makeCall(method, argCount+1, false, true);
            }
            //If there wasn't one with the given arg count, then just do it with the regular one
            method = langClass.methods.get(name);
            if (method == null)
                runtimeException("Method " + name + " does not exist for type " + langClass.name + " with " + argCount + " args");
            return makeCall(method, argCount+1, false, true);
        }
        runtimeException("Attempt to invoke " + instance + " with non-string method name, " + indexer);
        return false; //doesnt matter
    }

    /**
     * Gets the string of the object, calling its metamethod if it exists
     */
    public String getString(Object o) {
        if (o instanceof String || o instanceof Double || o instanceof Boolean || o == null)
            return PetPetString.valueOf(o);
        PetPetCallable method = getPetPetClass(o).methods.get("__tostring");
        if (method != null)
            return (String) method.call(o);
        return o.toString();
    }

    public PetPetClass getPetPetClass(Object o) {
        if (o == null)
            return PetPetNull.PET_PET_CLASS;
        if (o instanceof PetPetObject obj)
            return obj.type;
        return classMap.computeIfAbsent(o.getClass(), c -> {
            Class<?> cur = c.getSuperclass();
            while (cur != null) {
                if (classMap.containsKey(cur)) {
                    classMap.put(c, classMap.get(cur));
                    return classMap.get(cur);
                }
                cur = cur.getSuperclass();
            }
            runtimeException("Environment error: java object of type " + o.getClass() + " is in the environment, but it has no PetPetClass associated.");
            return null; //never happens, runtimeException() errors, but need anyway
        });
    }

    public boolean isFalsy(Object o) {
        return o == Boolean.FALSE || o == null || (o instanceof Double d && d == 0);
    }

    public boolean isTruthy(Object o) {
        return !isFalsy(o);
    }

    /**
     * Usable when we just popped from the stack, so we know
     * there must be space already allocated, so no need to
     * check for fullness
     */
    private void pushNoCheck(Object o) {
        stack[stackTop++] = o;
//        printStack();
    }

    private void push(Object o) {
        if (stackTop >= stack.length - 1) { //expand 1 earlier so pop() never needs to check
            Object[] newStack = new Object[stackTop * 2];
            System.arraycopy(stack, 0, newStack, 0, stackTop);
            stack = newStack;
        }
        stack[stackTop++] = o;
//        printStack();
    }

    private void swapTop() {
        Object temp = stack[stackTop-1];
        stack[stackTop-1] = stack[stackTop-2];
        stack[stackTop-2] = temp;
    }

    private Object pop() {
        stack[stackTop] = null; //to allow GC
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
    //Stack contract:
    //INVOCATION:
    //lastArg
    //...
    //firstArg (the instance, or "this")
    //argCount is equal to the number of args, including the instance
    //After invocation, everything listed above is gone, and the result of the call is left in its place.
    //NOT INVOCATION
    //lastArg
    //...
    //firstArg
    //function itself (this)
    //argCount is equal to the number of args, not including the function
    //after invocation, everything listed above is gone, and the result of the call is left in its place.
    private boolean makeCall(Object callee, int argCount, boolean calledFromJava, boolean isInvocation) {
//        printStack();
//        System.out.println(argCount);
//        System.out.println(stack);
        if (callee == null)
            runtimeException("Attempt to call null value");
        if (callee instanceof PetPetClosure closure) {
            int diff = isInvocation ? 1 : 0;
            pushCallStack(closure, 0, stackTop-argCount-1+diff, calledFromJava);
            if (callStackTop > maxStackFrames)
                runtimeException("Stack overflow! More than the max stack frames of " + maxStackFrames);
            if (argCount != closure.function.paramCount + diff) {
                runtimeException(String.format("Expected %d args, got %d", closure.function.paramCount - diff, argCount - diff));
            }
            return true;
        } else if (callee instanceof JavaFunction jFunction) {
            if (jFunction.paramCount != argCount) {
                int diff = isInvocation ? 1 : 0;
                runtimeException(String.format("Expected %d args, got %d", jFunction.paramCount - diff, argCount - diff));
            }
            if (jFunction.costPenalizer != null)
                cost += jFunction.costPenalizer.applyAsInt(this);
            try {
                Object result;
                if (jFunction.needsNumberConversion()) {
                    result = switch (argCount) {
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
                    result = switch (argCount) {
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
            } catch (PetPetException e) {
                e.printStackTrace();
                runtimeException(e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                runtimeException("Java exception occurred: " + e.getMessage());
            }
            return false;
        } else {
            runtimeException("Attempt to call non-callable value: " + getString(callee));
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

    private void printStack() {
        System.out.print("[");
        for (int i = 0; i < stackTop; i++) {
            System.out.print(stack[i]);
            if (i != stackTop-1)
                System.out.print(", ");
        }
        System.out.println("]");
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

//        public int lineNumber() {
//            int[] lines = closure.function.lineNumberTable;
//            int index = 0;
//            while (++index < lines.length && lines[index] <= ip);
//            return index + 0;//closure.function.lineNumberOffset;
//        }

        public int lineNumber() {
            int[] lines = closure.function.lineNumberTable;
            int index = 0;
            while (index < lines.length && lines[index] < ip) index++;
            return index;
        }
    }

}
