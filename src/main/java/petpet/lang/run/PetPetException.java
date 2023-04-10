package petpet.lang.run;

public class PetPetException extends RuntimeException {
    public PetPetException(Exception e) {
        super(e);
    }
    public PetPetException() {
        super();
    }
    public PetPetException(String message) {
        super(message);
    }

    public PetPetException(String message, Exception cause) {
        super(message, cause);
    }
}
