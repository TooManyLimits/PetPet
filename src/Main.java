import language.Lexer;
import language.bytecoderunner.Interpreter;
import language.bytecoderunner.LangFunction;
import language.compile.Compiler;
import language.parse.Expression;
import language.parse.Parser;

import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        String src =
        """
        print(3)
        this()
        """;

        Lexer.Token[] toks = Lexer.lex(src);
        List<Expression> exprs = new Parser(toks).parseChunk();
        Compiler comp = new Compiler(null);
        new Expression.BlockExpression(0, exprs).writeBytecode(comp);
        LangFunction f = comp.finish("script", 0);

        System.out.println(f.prettyBytecode());
        Interpreter i = new Interpreter();
        long before = System.nanoTime();
        i.run(f);
        System.out.println((System.nanoTime() - before) / 1000000d + " ms");
    }
}
