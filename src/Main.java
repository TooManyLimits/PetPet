import language.Lexer;
import language.bytecoderunner.*;
import language.compile.Compiler;
import language.parse.Expression;
import language.parse.Parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Main {

    public int abcd = 0;
    public static void main(String[] args) throws Exception {
        String src =
        """        
        if (1 == 3 || 4 >= 2) print(x)
        """;

        Lexer.Token[] toks = Lexer.lex(src);
        List<Expression> exprs = new Parser(toks).parseChunk();
        Compiler comp = new Compiler(null);
        new Expression.BlockExpression(0, exprs).writeBytecode(comp);
        LangFunction f = comp.finish("script", 0);

        System.out.println(f.prettyBytecode());
        Interpreter i = new Interpreter();

        LangClass tableClass = new LangClass();
        tableClass.methods.put("__get", new JavaFunction(HashMap.class, "get", true));
        tableClass.methods.put("__set", new JavaFunction(HashMap.class, "put", true));
        i.classMap.put(HashMap.class, tableClass);
        i.globals.put("table", new JavaFunction(Main.class, "makeTable", false));

        LangClass listClass = new LangClass();
        listClass.methods.put("__get_Integer", new JavaFunction(ArrayList.class, "get", true));
        listClass.methods.put("__set_Integer", new JavaFunction(ArrayList.class, "set", true));
        listClass.methods.put("add", new JavaFunction(ArrayList.class, "add", true, Object.class));
        i.classMap.put(ArrayList.class, listClass);
        i.globals.put("list", new JavaFunction(Main.class, "makeList", false));

        i.globals.put("print", new JavaFunction(Main.class, "print", false));

        long before = System.nanoTime();
        i.run(new LangClosure(f));
        System.out.println((System.nanoTime() - before) / 1000000d + " ms");

        System.out.println(i.cost + " cost");
//        JavaFunction.generateCode();
    }

    public static void printFunny(Object s) {
        System.out.println(s + " funny");
    }

    public static HashMap makeTable() {
        return new HashMap();
    }

    public static ArrayList makeList() {
        return new ArrayList();
    }

    public static void print(Object o) {
        System.out.println(o);
    }
}
