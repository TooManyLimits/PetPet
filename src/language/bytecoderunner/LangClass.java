package language.bytecoderunner;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class LangClass {

    public final Map<String, Object> methods = new HashMap<>(); //object is JavaFunction or LangClosure
    public final Map<String, Function> fieldGetters = new HashMap<>();
    public final Map<String, BiConsumer> fieldSetters = new HashMap<>();

}