package br.uenp.compiladores;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    private Map<String, Symbol> table = new HashMap<>();

    public void add(String name, String type) {
        if (table.containsKey(name)) {
            throw new RuntimeException("Erro: Variável '" + name + "' já declarada.");
        }
        table.put(name, new Symbol(type, null));
    }

    public boolean contains(String name) {
        return table.containsKey(name);
    }

    public String getType(String name) {
        if (!contains(name)) {
            throw new RuntimeException("Erro: Variável '" + name + "' não declarada.");
        }
        return table.get(name).type;
    }

    public void assign(String name, Object value) {
        if (!contains(name)) {
            throw new RuntimeException("Erro: Variável '" + name + "' não declarada.");
        }
        table.get(name).value = value;
    }

    public Object resolve(String name) {
        if (!contains(name)) {
            throw new RuntimeException("Erro: Variável '" + name + "' não declarada.");
        }
        Object value = table.get(name).value;
        if (value == null) {
            // Em C, ler uma variável não inicializada é um comportamento indefinido.
            // Para um interpretador, podemos lançar um erro ou retornar um valor padrão (ex: 0).
            // Lançar erro é mais seguro para depuração.
            throw new RuntimeException("Erro: Variável '" + name + "' pode não ter sido inicializada.");
        }
        return value;
    }
}