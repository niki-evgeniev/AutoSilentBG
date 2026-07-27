package nevg.autosilent.Service.Exception;

public class IncorrectPasswordException extends RuntimeException {
    public IncorrectPasswordException() {
        super("Текущата парола е неправилна.");
    }
}
