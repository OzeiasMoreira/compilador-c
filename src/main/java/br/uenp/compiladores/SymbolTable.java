package br.uenp.compiladores;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    private Map<String, Symbol> table = new HashMap<>();

    public void add(String name, String type) {
        if (table.containsKey(name)) {
            throw new RuntimeException("Error: Variable '" + name + "' already declared.");
        }
        table.put(name, new Symbol(type, null));
    }

    public boolean contains(String name) {
        return table.containsKey(name);
    }

    public String getType(String name) {
        if (!contains(name)) {
            throw new RuntimeException("Error: Variable '" + name + "' not declared.");
        }
        return table.get(name).type;
    }

    public void assign(String name, Object value) {
        if (!contains(name)) {
            throw new RuntimeException("Error: Variable '" + name + "' not declared.");
        }
        table.get(name).value = value;
    }

    public Object resolve(String name) {
        if (!contains(name)) {
            throw new RuntimeException("Error: Variable '" + name + "' not declared.");
        }
        // Lidar com valor nulo se a variável foi declarada mas não inicializada
        Object value = table.get(name).value;
        if (value == null) {
            throw new RuntimeException("Error: Variable '" + name + "' may not have been initialized.");
        }
        return value;
    }
}