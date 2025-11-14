package br.uenp.compiladores;

// Uma exceção especial que não é um "erro",
// mas sim um mecanismo de controlo de fluxo para o 'return'.
public class ReturnException extends RuntimeException {

    private final Object value;

    public ReturnException(Object value) {
        super(); // Não precisamos de uma mensagem
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    // Otimização: Isto não é um erro real, por isso não precisamos
    // de gastar tempo a construir o stack trace.
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}