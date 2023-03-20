import language.Lexer;
import language.bytecoderunner.*;
import language.compile.Compiler;
import language.parse.Expression;
import language.parse.Parser;
import language.reflect.MasqueReflector;
import language.reflect.MasqueWhitelist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Main {

    public int abcd = 0;
    public static void main(String[] args) throws Exception {
        String src =
        """
        
        """;

        Lexer.Token[] toks = Lexer.lex(src);
        List<Expression> exprs = new Parser(toks).parseChunk();
        Compiler comp = new Compiler(null);
        new Expression.BlockExpression(0, exprs).compile(comp);
        LangFunction compiled = comp.finish("script", 0, 0);

        JavaFunction.generateCode();

        System.out.println(compiled.prettyBytecode());
        Interpreter i = new Interpreter();

        LangClass tableClass = new LangClass("Table");
        tableClass.methods.put("__get", new JavaFunction(HashMap.class, "get", true));
        tableClass.methods.put("__set", new JavaFunction(HashMap.class, "put", true));
        i.classMap.put(HashMap.class, tableClass);
        i.globals.put("table", new JavaFunction(Main.class, "makeTable", false));

        LangClass listClass = new LangClass("List");
        listClass.methods.put("__get_num", new JavaFunction(ArrayList.class, "get", true));
        listClass.methods.put("__set_num", new JavaFunction(ArrayList.class, "set", true));
        listClass.methods.put("add", new JavaFunction(ArrayList.class, "add", true, Object.class));
        listClass.methods.put("size", new JavaFunction(ArrayList.class, "size", true));
        i.classMap.put(ArrayList.class, listClass);
        i.globals.put("list", new JavaFunction(Main.class, "makeList", false));

        LangClass stringClass = new LangClass("str");
        JavaFunction charAt = new JavaFunction(String.class, "charAt", true);
        stringClass.methods.put("charAt", charAt);
        stringClass.methods.put("__get_num", charAt);
        i.classMap.put(String.class, stringClass);

        LangClass numberClass = new LangClass("num");
        i.classMap.put(Double.class, numberClass);

        i.classMap.put(Point.class, MasqueReflector.reflect(Point.class, null));
        i.globals.put("point", new JavaFunction(Point.class, "newPoint", false));

        i.globals.put("print", new JavaFunction(Main.class, "print", false));

        long before = System.nanoTime();
        i.run(compiled);
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

    @MasqueWhitelist
    public static class Point {
        @MasqueWhitelist
        public double x, y;
        public static Point newPoint() {
            return new Point();
        }

        public void deleteYourOs() {
            throw new RuntimeException("Death");
        }

        @MasqueWhitelist
        public double length() {
            return Math.sqrt(x*x + y*y);
        }
    }
}
