package br.uenp.compiladores;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class SymbolTable {

    private Stack<Map<String, Symbol>> scopeStack = new Stack<>();
    private Map<String, FunctionSymbol> functionTable = new HashMap<>();
    private Map<String, StructDefinition> structTable = new HashMap<>();

    // Tabela para definições de union
    private Map<String, StructDefinition> unionTable = new HashMap<>();

    public SymbolTable() {
        enterScope(); // Adiciona o escopo global
    }

    // --- Métodos de Escopo ---
    public void enterScope() {
        scopeStack.push(new HashMap<String, Symbol>());
    }
    public void exitScope() {
        if (!scopeStack.isEmpty()) {
            scopeStack.pop();
        } else {
            throw new RuntimeException("Erro interno: Não há escopo para sair.");
        }
    }
    private Map<String, Symbol> getCurrentScope() {
        if (scopeStack.isEmpty()) {
            throw new RuntimeException("Erro interno: A pilha de escopos está vazia.");
        }
        return scopeStack.peek();
    }

    // --- Métodos de Função ---
    public void addFunction(String name, FunctionSymbol function) {
        if (functionTable.containsKey(name) || structTable.containsKey(name) || unionTable.containsKey(name)) {
            throw new RuntimeException("Erro: Conflito de nome. Já existe uma função, struct ou union chamada '" + name + "'.");
        }
        functionTable.put(name, function);
    }
    public FunctionSymbol resolveFunction(String name) {
        if (!functionTable.containsKey(name)) {
            throw new RuntimeException("Erro: Função '" + name + "' não declarada.");
        }
        return functionTable.get(name);
    }

    // --- Métodos de Struct ---
    public void addStructDefinition(String name, StructDefinition def) {
        if (structTable.containsKey(name) || functionTable.containsKey(name) || unionTable.containsKey(name)) {
            throw new RuntimeException("Erro: Nome '" + name + "' já definido.");
        }
        structTable.put(name, def);
    }
    public StructDefinition resolveStructDefinition(String name) {
        if (!structTable.containsKey(name)) {
            throw new RuntimeException("Erro: Tipo struct '" + name + "' não definido.");
        }
        return structTable.get(name);
    }
    public boolean isStructType(String typeName) {
        if (typeName.startsWith("struct")) {
            String name = typeName.substring(6);
            return structTable.containsKey(name);
        }
        return false;
    }

    // --- MÉTODOS DE UNION ---
    public void addUnionDefinition(String name, StructDefinition def) {
        if (unionTable.containsKey(name) || structTable.containsKey(name) || functionTable.containsKey(name)) {
            throw new RuntimeException("Erro: Nome '" + name + "' já definido.");
        }
        unionTable.put(name, def);
    }
    public StructDefinition resolveUnionDefinition(String name) {
        if (!unionTable.containsKey(name)) {
            throw new RuntimeException("Erro: Tipo union '" + name + "' não definido.");
        }
        return unionTable.get(name);
    }
    public boolean isUnionType(String typeName) {
        if (typeName.startsWith("union")) {
            String name = typeName.substring(5); // Remove "union"
            return unionTable.containsKey(name);
        }
        return false;
    }


    // --- Métodos de Variáveis (Corrigidos, sem @Override) ---

    /**
     * Adiciona uma variável ao escopo atual.
     */
    // @Override <- REMOVIDO
    public void add(String name, String type) {
        Map<String, Symbol> currentScope = getCurrentScope();

        if (currentScope.containsKey(name)) {
            throw new RuntimeException("Erro: Variável '" + name + "' já declarada neste escopo.");
        }
        if (functionTable.containsKey(name) || structTable.containsKey(name) || unionTable.containsKey(name)) {
            throw new RuntimeException("Erro: Conflito de nome. Já existe uma função, struct ou union chamada '" + name + "'.");
        }

        if (isStructType(type)) {
            String structName = type.substring(6);
            StructDefinition def = resolveStructDefinition(structName);
            StructInstance instance = new StructInstance(def);
            currentScope.put(name, new Symbol(type, instance));
        }
        else if (isUnionType(type)) {
            String unionName = type.substring(5);
            StructDefinition def = resolveUnionDefinition(unionName);
            UnionInstance instance = new UnionInstance(def);
            currentScope.put(name, new Symbol(type, instance));
        }
        else {
            currentScope.put(name, new Symbol(type, null));
        }
    }

    private Symbol find(String name) {
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            Map<String, Symbol> scope = scopeStack.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null;
    }

    // @Override <- REMOVIDO
    public void assign(String name, Object value) {
        Symbol symbol = find(name);
        if (symbol == null) {
            throw new RuntimeException("Erro: Variável '" + name + "' não declarada.");
        }
        if (symbol.value instanceof StructInstance || symbol.value instanceof UnionInstance) {
            throw new RuntimeException("Erro: Não é possível atribuir a uma variável struct/union inteira. Use '.membro'.");
        }
        symbol.value = value;
    }

    // @Override <- REMOVIDO
    public Object resolve(String name) {
        Symbol symbol = find(name);
        if (symbol == null) {
            throw new RuntimeException("Erro: Variável '" + name + "' não declarada.");
        }

        if (symbol.value instanceof StructInstance || symbol.value instanceof UnionInstance) {
            return symbol.value;
        }

        Object value = symbol.value;
        if (value == null) {
            throw new RuntimeException("Erro: Variável '" + name + "' pode não ter sido inicializada.");
        }
        return value;
    }

    // @Override <- REMOVIDO
    public String getType(String name) {
        Symbol symbol = find(name);
        if (symbol == null) {
            throw new RuntimeException("Erro: Variável '" + name + "' não declarada.");
        }
        return symbol.type;
    }

    // @Override <- REMOVIDO
    public boolean isFunction(String name) {
        return functionTable.containsKey(name);
    }
}