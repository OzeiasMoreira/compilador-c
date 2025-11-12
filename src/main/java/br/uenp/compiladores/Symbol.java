package br.uenp.compiladores;

public class Symbol {
    String type;
    Object value;

    public Symbol(String type, Object value) {
        this.type = type;
        this.value = value;
    }
}