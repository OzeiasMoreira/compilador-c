package br.uenp.compiladores;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class SymbolTable {

    private Stack<Map<String, Symbol>> scopeStack = new Stack<>();
    private Map<String, FunctionSymbol> functionTable = new HashMap<>();
    private Map<String, StructDefinition> structTable = new HashMap<>();
    private Map<String, StructDefinition> unionTable = new HashMap<>();

    // NOVO: Tabela para constantes #define (globais)
    private Map<String, Object> defineTable = new HashMap<>();

    public SymbolTable() {
        enterScope(); // Adiciona o escopo global
    }

    // --- Métodos de Escopo (sem mudanças) ---
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

    // --- Métodos de Função (sem mudanças) ---
    public void addFunction(String name, FunctionSymbol function) {
        if (defineTable.containsKey(name) || functionTable.containsKey(name) || structTable.containsKey(name) || unionTable.containsKey(name)) { // <-- ATUALIZADO
            throw new RuntimeException("Erro: Conflito de nome. Já existe um define/função/struct/union chamada '" + name + "'.");
        }
        functionTable.put(name, function);
    }
    public FunctionSymbol resolveFunction(String name) {
        if (!functionTable.containsKey(name)) {
            throw new RuntimeException("Erro: Função '" + name + "' não declarada.");
        }
        return functionTable.get(name);
    }

    // --- Métodos de Struct (sem mudanças) ---
    public void addStructDefinition(String name, StructDefinition def) {
        if (defineTable.containsKey(name) || structTable.containsKey(name) || functionTable.containsKey(name) || unionTable.containsKey(name)) {
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

    // --- Métodos de Union (sem mudanças) ---
    public void addUnionDefinition(String name, StructDefinition def) {
        if (defineTable.containsKey(name) || unionTable.containsKey(name) || structTable.containsKey(name) || functionTable.containsKey(name)) {
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
            String name = typeName.substring(5);
            return unionTable.containsKey(name);
        }
        return false;
    }

    // --- MÉTODOS DE DEFINE (NOVOS) ---

    public void addDefine(String name, Object value) {
        if (defineTable.containsKey(name)) {
            throw new RuntimeException("Erro: Constante '" + name + "' já definida.");
        }
        if (functionTable.containsKey(name) || structTable.containsKey(name) || unionTable.containsKey(name)) {
            throw new RuntimeException("Erro: Conflito de nome. Já existe uma função/struct/union chamada '" + name + "'.");
        }
        defineTable.put(name, value);
    }

    public boolean isDefine(String name) {
        return defineTable.containsKey(name);
    }

    public Object resolveDefine(String name) {
        return defineTable.get(name);
    }


    // --- Métodos de Variáveis (Atualizados) ---

    public void add(String name, String type) {
        Map<String, Symbol> currentScope = getCurrentScope();

        // Conflito com #define?
        if (defineTable.containsKey(name)) {
            throw new RuntimeException("Erro: Conflito de nome. Já existe uma constante '#define' chamada '" + name + "'.");
        }
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

    public String getType(String name) {
        Symbol symbol = find(name);
        if (symbol == null) {
            throw new RuntimeException("Erro: Variável '" + name + "' não declarada.");
        }
        return symbol.type;
    }

    public boolean isFunction(String name) {
        return functionTable.containsKey(name);
    }
}