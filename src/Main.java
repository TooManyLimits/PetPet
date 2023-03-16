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
        count = fun(n) if n > 0 {print(n) this(n-1)}
        count(5)
        """;

        Lexer.Token[] toks = Lexer.lex(src);
        List<Expression> exprs = new Parser(toks).parseChunk();
        Compiler comp = new Compiler(null);
        new Expression.BlockExpression(0, exprs).writeBytecode(comp);
        LangFunction f = comp.finish("script", 0);

        System.out.println(f.prettyBytecode());
        new Interpreter().run(f);
    }
}
