import language.bytecoderunner.Interpreter;
import language.bytecoderunner.LangFunction;
import language.compile.Chunk;
import language.compile.Compiler;
import language.parse.Expression;
import language.Lexer;
import language.parse.Parser;

import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        String src =
        """
        if 3 == 5 y = 2 else x = 4
        print(x)
        print(y)
        """;

        Lexer.Token[] toks = Lexer.lex(src);
        List<Expression> exprs = new Parser(toks).parseChunk();
        LangFunction f = new Compiler().compile(exprs);
        f.chunk.printBytecode();
        new Interpreter().run(f);
    }
}
