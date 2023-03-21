import main.java.petpet.external.PetPetInstance;
import main.java.petpet.lang.lex.Lexer;
import main.java.petpet.lang.run.*;
import main.java.petpet.lang.compile.Compiler;
import main.java.petpet.lang.parse.Expression;
import main.java.petpet.lang.parse.Parser;
import main.java.petpet.external.PetPetReflector;
import main.java.petpet.external.PetPetWhitelist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        String src =
        """
        x = List()
        i = 0
        while i < 50 {
            x.add(i)
            i = i + 1
        }
        print(x)
        {
            fib = fun(j) {
                y = if j < 2
                    j
                else
                    x[j-1] + x[j-2]
                j = j + 1
                y
            }
            x.map(fib)
        }
        print(x)
        """;

        PetPetInstance instance = new PetPetInstance();
        instance.debugTime = true;
        instance.debugBytecode = true;
        instance.debugCost = true;
        instance.setGlobal("print", new JavaFunction(Main.class, "print", false));

        instance.runScriptOrThrow("script", src);
    }

    public static void print(Object o) {
        System.out.println(o);
    }
}
