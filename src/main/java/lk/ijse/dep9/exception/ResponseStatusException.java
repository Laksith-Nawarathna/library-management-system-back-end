package lk.ijse.dep9.exception;

public class ResponseStatusException extends RuntimeException{

    private int status;

    public ResponseStatusException(int status, String message, Throwable t){
        super(message, t);
        this.status = status;
    }

    public ResponseStatusException(int status, Throwable t){
        super(t);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
