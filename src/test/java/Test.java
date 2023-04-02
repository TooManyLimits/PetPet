import petpet.external.PetPetInstance;
import petpet.types.PetPetList;

public class Test {

    public static void main(String[] args) throws Exception {
        String hello = """
            print("Hello, cuties!")
            funni.push(5)
            funni.push(6)
            funni.each(print)
        """;

        PetPetList<String> petPetList = new PetPetList<>();
        petPetList.add("cutie");
        PetPetInstance instance = new PetPetInstance();
        instance.setGlobal("funni", petPetList);
        instance.runScript("script", hello);
        System.out.println(petPetList.get(2));
    }

}
