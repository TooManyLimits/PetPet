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
        nums = List()
        nums.add(0)
        i = 0
        nums.map(fun(x) i = i + 1)
        print(nums)
        
        compose = fun(a, b) fun(x) a(b(x))
        inc = fun(x) x+1
        print1more = compose(print, inc)
        print1more(5)
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
