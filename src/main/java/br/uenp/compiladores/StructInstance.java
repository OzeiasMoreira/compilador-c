package br.uenp.compiladores;

import java.util.HashMap;
import java.util.Map;

/**
 * Armazena uma *instância* de uma struct (ex: struct Ponto p1;)
 * Isto guarda os *valores* atuais dos membros.
 */
public class StructInstance {
    private final StructDefinition definition;
    private final Map<String, Object> memberValues; // <NomeMembro, ValorMembro>

    public StructInstance(StructDefinition definition) {
        this.definition = definition;
        this.memberValues = new HashMap<>();
    }

    public void write(String memberName, Object value) {
        if (!definition.hasMember(memberName)) {
            throw new RuntimeException("Erro: Struct não possui membro '" + memberName + "'.");
        }
        // TODO: Adicionar checagem de tipo (ex: não atribuir float a p1.x se x é int)
        memberValues.put(memberName, value);
    }

    public Object read(String memberName) {
        if (!definition.hasMember(memberName)) {
            throw new RuntimeException("Erro: Struct não possui membro '" + memberName + "'.");
        }
        Object value = memberValues.get(memberName);
        if (value == null) {
            throw new RuntimeException("Erro: Lendo membro de struct ('" + memberName + "') não inicializado.");
        }
        return value;
    }
}