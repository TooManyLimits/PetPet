import petpet.external.PetPetInstance;

public class Test {

    public static void main(String[] args) throws Exception {
        String hello = "print(\"Hello, cuties!\")";
        new PetPetInstance().runScript("script", hello);
    }

}
