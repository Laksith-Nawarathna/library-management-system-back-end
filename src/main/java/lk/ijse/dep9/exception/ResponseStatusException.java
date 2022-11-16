package lk.ijse.dep9.exception;

public class ResponseStatusException extends RuntimeException{  // here use unchecked type since checked ones have to be catching or specify in our code

    private int status;

    public ResponseStatusException(int status, String message, Throwable t){
        super(message, t);
        this.status = status;
    }

    public ResponseStatusException(int status, String message){
        super(message);
        this.status = status;
    }

    public ResponseStatusException(int status, Throwable t){
        super(t);
        this.status = status;
    }

    public ResponseStatusException(int status){
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
