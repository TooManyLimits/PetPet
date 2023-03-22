import main.java.petpet.external.PetPetInstance;
import main.java.petpet.lang.run.*;

public class Main {

    public static void main(String[] args) throws Exception {
        String src =
        """
        x = "hello aple :D"
        print(x.concat(" ").concat(x))
        print(x[" "]:cutie)
        print(x.concat(" ").cuite_.snuggle)
        print(x.len())
        """;

        PetPetInstance instance = new PetPetInstance();
        instance.debugTime = true;
        instance.debugBytecode = true;
        instance.debugCost = true;
        instance.setGlobal("print", new JavaFunction(Main.class, "print", false));
        instance.setGlobal("print2", new JavaFunction(Main.class, "print2", false));

        try {
            instance.runScript("script", src); //normally need error handling here, but eh
        } catch (PetPetException petpet) {
            System.err.println(petpet.getMessage());
        }
    }

    public static void print(Object a) {
        System.out.println(a);
    }
    public static void print2(Object a, Object b) {
        System.out.println(a + ",\t" + b);
    }
}
