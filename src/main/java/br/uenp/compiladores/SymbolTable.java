package br.uenp.compiladores;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class SymbolTable {

    private Stack<Map<String, Symbol>> scopeStack = new Stack<>();
    private Map<String, FunctionSymbol> functionTable = new HashMap<>();
    private Map<String, StructDefinition> structTable = new HashMap<>();
    private Map<String, StructDefinition> unionTable = new HashMap<>();
    private Map<String, Object> defineTable = new HashMap<>();

    public SymbolTable() {
        enterScope();
    }

    // --- Métodos de Escopo ---
    public void enterScope() { scopeStack.push(new HashMap<String, Symbol>()); }
    public void exitScope() { scopeStack.pop(); }
    private Map<String, Symbol> getCurrentScope() { return scopeStack.peek(); }

    // --- Métodos de Função ---
    public void addFunction(String name, FunctionSymbol function) {
        if (defineTable.containsKey(name) || functionTable.containsKey(name) || structTable.containsKey(name) || unionTable.containsKey(name)) {
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

    // --- Métodos de Struct/Union/Define (Sem alterações de lógica) ---
    public void addStructDefinition(String name, StructDefinition def) {
        if (defineTable.containsKey(name) || structTable.containsKey(name) || functionTable.containsKey(name) || unionTable.containsKey(name)) throw new RuntimeException("Erro: Nome já definido.");
        structTable.put(name, def);
    }
    public StructDefinition resolveStructDefinition(String name) {
        if (!structTable.containsKey(name)) throw new RuntimeException("Erro: Tipo struct '" + name + "' não definido.");
        return structTable.get(name);
    }
    public boolean isStructType(String typeName) {
        return typeName.startsWith("struct") && structTable.containsKey(typeName.substring(6));
    }

    public void addUnionDefinition(String name, StructDefinition def) {
        if (defineTable.containsKey(name) || unionTable.containsKey(name) || structTable.containsKey(name) || functionTable.containsKey(name)) throw new RuntimeException("Erro: Nome já definido.");
        unionTable.put(name, def);
    }
    public StructDefinition resolveUnionDefinition(String name) {
        if (!unionTable.containsKey(name)) throw new RuntimeException("Erro: Tipo union '" + name + "' não definido.");
        return unionTable.get(name);
    }
    public boolean isUnionType(String typeName) {
        return typeName.startsWith("union") && unionTable.containsKey(typeName.substring(5));
    }

    public void addDefine(String name, Object value) {
        if (defineTable.containsKey(name)) throw new RuntimeException("Erro: Constante já definida.");
        if (functionTable.containsKey(name) || structTable.containsKey(name) || unionTable.containsKey(name)) throw new RuntimeException("Erro: Conflito de nome.");
        defineTable.put(name, value);
    }
    public boolean isDefine(String name) { return defineTable.containsKey(name); }
    public Object resolveDefine(String name) { return defineTable.get(name); }


    // --- MÉTODOS DE VARIÁVEIS (COM A CORREÇÃO PARA PONTEIROS DE STRUCT) ---

    public void add(String name, String type) {
        Map<String, Symbol> currentScope = getCurrentScope();
        if (defineTable.containsKey(name) || functionTable.containsKey(name) || structTable.containsKey(name) || unionTable.containsKey(name)) throw new RuntimeException("Erro: Conflito de nome.");
        if (currentScope.containsKey(name)) throw new RuntimeException("Erro: Variável '" + name + "' já declarada neste escopo.");
        if (name.equals("void")) throw new RuntimeException("Erro: 'void' inválido.");

        if (isStructType(type)) {
            currentScope.put(name, new Symbol(type, new StructInstance(resolveStructDefinition(type.substring(6)))));
        } else if (isUnionType(type)) {
            currentScope.put(name, new Symbol(type, new UnionInstance(resolveUnionDefinition(type.substring(5)))));
        } else {
            currentScope.put(name, new Symbol(type, null));
        }
    }

    private Symbol find(String name) {
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            Map<String, Symbol> scope = scopeStack.get(i);
            if (scope.containsKey(name)) return scope.get(name);
        }
        return null;
    }

    public void assign(String name, Object value) {
        // CORREÇÃO: Suporte para ponteiros apontando para membros (ex: "p1.x")
        if (name.contains(".")) {
            String[] parts = name.split("\\.");
            String instanceName = parts[0];
            String memberName = parts[1];
            Symbol symbol = find(instanceName);
            if (symbol != null) {
                if (symbol.value instanceof StructInstance) {
                    ((StructInstance) symbol.value).write(memberName, value);
                    return;
                } else if (symbol.value instanceof UnionInstance) {
                    ((UnionInstance) symbol.value).write(memberName, value);
                    return;
                }
            }
            // Se não encontrou ou não é struct, cai no erro padrão abaixo
        }

        Symbol symbol = find(name);
        if (symbol == null) {
            throw new RuntimeException("Erro: Variável '" + name + "' não declarada.");
        }

        if (symbol.value instanceof StructInstance || symbol.value instanceof UnionInstance) {
            boolean isArrayInit = (value instanceof Object[]);
            boolean isStructCopy = (value instanceof StructInstance);
            boolean isUnionCopy = (value instanceof UnionInstance);
            if (!isArrayInit && !isStructCopy && !isUnionCopy) {
                throw new RuntimeException("Erro: Tipo incompatível na atribuição.");
            }
        }
        symbol.value = value;
    }

    public Object resolve(String name) {
        // CORREÇÃO: Suporte para ler ponteiros de membros (ex: "p1.x")
        if (name.contains(".")) {
            String[] parts = name.split("\\.");
            String instanceName = parts[0];
            String memberName = parts[1];
            Symbol symbol = find(instanceName);
            if (symbol != null) {
                if (symbol.value instanceof StructInstance) {
                    return ((StructInstance) symbol.value).read(memberName);
                } else if (symbol.value instanceof UnionInstance) {
                    return ((UnionInstance) symbol.value).read(memberName);
                }
            }
        }

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
        // Nota: getType simples não suporta "p1.x" facilmente,
        // mas para o interpretador básico isto raramente é chamado para membros.
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