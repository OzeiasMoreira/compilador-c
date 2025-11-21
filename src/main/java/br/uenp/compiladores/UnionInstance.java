package br.uenp.compiladores;

public class UnionInstance {
    private final StructDefinition definition;
    private Object value; // Apenas um valor para todos os membros

    public UnionInstance(StructDefinition definition) {
        // Reutilizamos StructDefinition porque o "plano" é o mesmo
        this.definition = definition;
        this.value = null;
    }

    public void write(String memberName, Object value) {
        if (!definition.hasMember(memberName)) {
            throw new RuntimeException("Erro: Union não possui membro '" + memberName + "'.");
        }
        this.value = value;
    }

    public Object read(String memberName) {
        if (!definition.hasMember(memberName)) {
            throw new RuntimeException("Erro: Union não possui membro '" + memberName + "'.");
        }
        if (value == null) {
            throw new RuntimeException("Erro: Lendo membro de union ('" + memberName + "') não inicializado.");
        }
        return value;
    }
}