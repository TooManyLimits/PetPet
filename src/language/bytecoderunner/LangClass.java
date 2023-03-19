package language.bytecoderunner;

public abstract class LangClass {
    public abstract Object get(Object instance, Object indexer);
    public abstract void set(Object instance, Object indexer, Object value);
}