package br.uenp.compiladores;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class SymbolTable {

    private Stack<Map<String, Symbol>> scopeStack = new Stack<>();
    private Map<String, FunctionSymbol> functionTable = new HashMap<>();

    // NOVO: Tabela para definições de struct (sempre global)
    private Map<String, StructDefinition> structTable = new HashMap<>();

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
        if (functionTable.containsKey(name) || structTable.containsKey(name)) {
            throw new RuntimeException("Erro: Função '" + name + "' já declarada ou nome conflita com struct.");
        }
        functionTable.put(name, function);
    }
    public FunctionSymbol resolveFunction(String name) {
        if (!functionTable.containsKey(name)) {
            throw new RuntimeException("Erro: Função '" + name + "' não declarada.");
        }
        return functionTable.get(name);
    }

    // --- MÉTODOS DE STRUCT (NOVOS) ---

    /**
     * Adiciona uma nova definição de struct (plano) à tabela global.
     */
    public void addStructDefinition(String name, StructDefinition def) {
        if (structTable.containsKey(name) || functionTable.containsKey(name)) {
            throw new RuntimeException("Erro: Nome '" + name + "' já definido (como struct ou função).");
        }
        structTable.put(name, def);
    }

    /**
     * Procura por uma definição de struct.
     */
    public StructDefinition resolveStructDefinition(String name) {
        if (!structTable.containsKey(name)) {
            throw new RuntimeException("Erro: Tipo struct '" + name + "' não definido.");
        }
        return structTable.get(name);
    }

    /**
     * Verifica se um nome de tipo (ex: "struct Ponto") é um tipo struct válido.
     */
    public boolean isStructType(String typeName) {
        // O tipo vem da gramática como "structPonto"
        if (typeName.startsWith("struct")) {
            String name = typeName.substring(6); // Remove "struct"
            return structTable.containsKey(name);
        }
        return false;
    }

    // --- Métodos de Variáveis (Atualizados) ---

    /**
     * Adiciona uma variável ao escopo atual.
     * Se for um tipo struct, já instancia o objeto StructInstance.
     */
    public void add(String name, String type) {
        Map<String, Symbol> currentScope = getCurrentScope();

        if (currentScope.containsKey(name)) {
            throw new RuntimeException("Erro: Variável '" + name + "' já declarada neste escopo.");
        }
        if (functionTable.containsKey(name) || structTable.containsKey(name)) {
            throw new RuntimeException("Erro: Conflito de nome. Já existe uma função ou struct chamada '" + name + "'.");
        }

        // Se for um tipo struct (ex: "structPonto")
        if (isStructType(type)) {
            String structName = type.substring(6); // Remove "struct"
            StructDefinition def = resolveStructDefinition(structName);
            // Cria a instância e armazena-a como o "valor" inicial do símbolo
            StructInstance instance = new StructInstance(def);
            currentScope.put(name, new Symbol(type, instance));
        } else {
            // Comportamento normal para int, float, char
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
        // Não podemos atribuir um valor normal a uma struct inteira
        if (symbol.value instanceof StructInstance) {
            throw new RuntimeException("Erro: Não é possível atribuir a uma variável struct inteira. Use 'struct.membro'.");
        }
        symbol.value = value;
    }

    public Object resolve(String name) {
        Symbol symbol = find(name);
        if (symbol == null) {
            throw new RuntimeException("Erro: Variável '" + name + "' não declarada.");
        }

        // Se for uma struct, retornamos a própria instância
        if (symbol.value instanceof StructInstance) {
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