package br.uenp.compiladores;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class SymbolTable {

    private Stack<Map<String, Symbol>> scopeStack = new Stack<>();

    // Tabela separada para Funções (sempre global)
    private Map<String, FunctionSymbol> functionTable = new HashMap<>();

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
        if (functionTable.containsKey(name)) {
            throw new RuntimeException("Erro: Função '" + name + "' já declarada.");
        }
        functionTable.put(name, function);
    }

    public FunctionSymbol resolveFunction(String name) {
        if (!functionTable.containsKey(name)) {
            throw new RuntimeException("Erro: Função '" + name + "' não declarada.");
        }
        return functionTable.get(name);
    }


    // --- Métodos de Variáveis (Atualizados) ---

    public void add(String name, String type) {
        Map<String, Symbol> currentScope = getCurrentScope();

        if (currentScope.containsKey(name)) {
            throw new RuntimeException("Erro: Variável '" + name + "' já declarada neste escopo.");
        }
        if (functionTable.containsKey(name)) {
            throw new RuntimeException("Erro: Conflito de nome. Já existe uma função chamada '" + name + "'.");
        }
        currentScope.put(name, new Symbol(type, null));
    }

    /**
     * Procura por uma variável na pilha de escopos.
     */
    private Symbol find(String name) {
        // Itera de cima para baixo na pilha
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            Map<String, Symbol> scope = scopeStack.get(i);
            if (scope.containsKey(name)) {
                //
                // --- ESTA É A LINHA QUE FOI CORRIGIDA ---
                //
                return scope.get(name); // Estava scope.get(i)
            }
        }
        return null; // Não encontrou em nenhum escopo
    }

    public void assign(String name, Object value) {
        Symbol symbol = find(name);
        if (symbol == null) {
            throw new RuntimeException("Erro: Variável '" + name + "' não declarada.");
        }
        symbol.value = value;
    }

    public Object resolve(String name) {
        Symbol symbol = find(name);
        if (symbol == null) {
            throw new RuntimeException("Erro: Variável '" + name + "' não declarada.");
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