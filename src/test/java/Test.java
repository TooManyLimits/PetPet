import petpet.external.PetPetInstance;
import petpet.types.PetPetList;

public class Test {

    public static void main(String[] args) throws Exception {
        String script = "";
        for (int i = 1; i <= 300; i++) {
            script += ("a".repeat(i) + "=5\n");
        }
//        System.out.println(script);

        script = """
                x = fun() print(3)
                x()
                """;

        PetPetList<String> petPetList = new PetPetList<>();
        petPetList.add("cutie");
        PetPetInstance instance = new PetPetInstance();
        instance.debugBytecode = true;
        instance.setGlobal("funni", petPetList);
        instance.runScript("script", script);
        System.out.println(petPetList.get(2));


    }

}
