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
        i = 0
        while i < 10 {
            nums.add(i)
            i = i + 1
        }
        print(nums)
        print(nums.copy().map(fun(x) x*x))
        print(nums)
        print(nums.map(fun(x) x*x))
        print(nums)
        """;
        PetPetInstance instance = new PetPetInstance();
        instance.debugMode = true;
        instance.setGlobal("print", new JavaFunction(Main.class, "print", false));

        long before = System.nanoTime();
        instance.runScriptOrThrow("script", src);
        System.out.println((System.nanoTime() - before) / 1000000d + " ms");
    }

    public static void print(Object o) {
        System.out.println(o);
    }
}
