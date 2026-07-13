package nevg.autosilent.Service.Exception;

public class ProductAlreadyExistsException extends RuntimeException {

    private final String field;

    public ProductAlreadyExistsException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
