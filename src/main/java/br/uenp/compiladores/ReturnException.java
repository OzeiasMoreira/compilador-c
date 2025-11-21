package br.uenp.compiladores;

public class ReturnException extends RuntimeException {

    private final Object value;

    public ReturnException(Object value) {
        super();
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}