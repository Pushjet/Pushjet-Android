package net.Azise.pushjet.PushjetApi;

public class PushjetException extends Exception {
    public int code;

    public PushjetException(String message, int code) {
        super(message);
        this.code = code;
    }
}
