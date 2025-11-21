package br.uenp.compiladores;

import java.util.LinkedHashMap;
import java.util.Map;

public class StructDefinition {
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