package br.uenp.compiladores;

import org.antlr.v4.runtime.tree.ParseTree;
import java.util.List;
import java.util.Map;

// Esta classe armazena a "assinatura" de uma função
public class FunctionSymbol extends Symbol {

    // Lista de parâmetros. Map<Tipo, Nome>
    // Usamos Map para facilitar a procura, mas List<Pair<String, String>> seria mais correto para ordem
    private final List<Map.Entry<String, String>> parameters;

    // O nó da árvore de sintaxe que contém o corpo (o 'block') da função
    private final ParseTree body;

    public FunctionSymbol(String type, List<Map.Entry<String, String>> params, ParseTree body) {
        super(type, null); // O 'valor' de uma função é tratado na chamada
        this.parameters = params;
        this.body = body;
    }

    public List<Map.Entry<String, String>> getParameters() {
        return parameters;
    }

    public ParseTree getBody() {
        return body;
    }
}