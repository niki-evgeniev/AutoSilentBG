package nevg.nirton.Service.Exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException() {
        super("Вече съществува профил с този имейл адрес.");
    }
}
