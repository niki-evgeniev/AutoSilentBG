package nevg.nirton.Service;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException() {
        super("Вече съществува профил с този имейл адрес.");
    }
}
