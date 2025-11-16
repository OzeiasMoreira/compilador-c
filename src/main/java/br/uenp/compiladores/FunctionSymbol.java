package br.uenp.compiladores;

import org.antlr.v4.runtime.tree.ParseTree;
import java.util.List;
import java.util.Map;

public class FunctionSymbol extends Symbol {

    private final String name; // <-- ADICIONADO
    private final List<Map.Entry<String, String>> parameters;
    private final ParseTree body;

    // Construtor atualizado
    public FunctionSymbol(String name, String type, List<Map.Entry<String, String>> params, ParseTree body) {
        super(type, null);
        this.name = name; // <-- ADICIONADO
        this.parameters = params;
        this.body = body;
    }

    public String getName() { // <-- NOVO MÉTODO
        return name;
    }

    public String getType() { // <-- NOVO MÉTODO
        return super.type;
    }

    public List<Map.Entry<String, String>> getParameters() {
        return parameters;
    }

    public ParseTree getBody() {
        return body;
    }
}