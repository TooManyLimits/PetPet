package petpet.lang.run;

/**
 *
 * TODO: Make stack trace a separate variable
 *
 */
public class PetPetException extends RuntimeException {
    private String stackTrace; //The petpet stack trace component of the message
    public PetPetException(Exception e) {
        super(e);
    }
    public PetPetException() {
        super();
    }
    public PetPetException(String message) {
        super(message);
    }
    public PetPetException(String message, String stackTrace) {
        super(message);
        this.stackTrace = stackTrace;
    }
    public PetPetException(String message, Exception cause) {
        super(message, cause);
    }

    public String getPetPetStackTrace() {
        return stackTrace;
    }
}
