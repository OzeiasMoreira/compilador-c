package br.uenp.compiladores;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Armazena o "plano" de uma struct (ex: struct Ponto { int x; char c; })
 * Guarda os nomes e tipos dos membros.
 */
public class StructDefinition {
    // Usamos LinkedHashMap para preservar a ordem dos membros (boa prática)
    private final Map<String, String> members; // <NomeMembro, TipoMembro>

    public StructDefinition() {
        this.members = new LinkedHashMap<>();
    }

    public void addMember(String name, String type) {
        if (members.containsKey(name)) {
            throw new RuntimeException("Erro: Membro '" + name + "' já declarado na struct.");
        }
        members.put(name, type);
    }

    public boolean hasMember(String name) {
        return members.containsKey(name);
    }

    public String getMemberType(String name) {
        return members.get(name);
    }

    public Map<String, String> getMembers() {
        return members;
    }
}