import language.Lexer;
import language.bytecoderunner.Interpreter;
import language.bytecoderunner.JavaFunction;
import language.bytecoderunner.LangClass;
import language.bytecoderunner.LangFunction;
import language.compile.Compiler;
import language.parse.Expression;
import language.parse.Parser;

import java.util.HashMap;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        String src =
        """
        printFunny(2)
        """;

        LangClass tableClass = new LangClass() {
            @Override
            public Object get(Object instance, Object indexer) {
                return ((HashMap<?, ?>) instance).get(indexer);
            }

            @Override
            public void set(Object instance, Object indexer, Object value) {
                ((HashMap) instance).put(indexer, value);
            }
        };

        Lexer.Token[] toks = Lexer.lex(src);
        List<Expression> exprs = new Parser(toks).parseChunk();
        Compiler comp = new Compiler(null);
        new Expression.BlockExpression(0, exprs).writeBytecode(comp);
        LangFunction f = comp.finish("script", 0);

        System.out.println(f.prettyBytecode());
        Interpreter i = new Interpreter();
        i.classMap.put(HashMap.class, tableClass);
        i.globals.put("table", new HashMap<>());
        i.globals.put("printFunny", new JavaFunction(Main.class, "printFunny", false));
        long before = System.nanoTime();
        i.run(f);
        System.out.println((System.nanoTime() - before) / 1000000d + " ms");

//        JavaFunction.generateCode();
    }

    public static void printFunny(Object s) {
        System.out.println(s + " funny");
    }
}
