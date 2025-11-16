package br.uenp.compiladores;

/**
 * Armazena uma *instância* de uma union (ex: union Valor v;)
 * Todos os membros partilham o mesmo espaço de memória ('value').
 */
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
        // Escrever em *qualquer* membro sobrescreve o valor partilhado
        this.value = value;
    }

    public Object read(String memberName) {
        if (!definition.hasMember(memberName)) {
            throw new RuntimeException("Erro: Union não possui membro '" + memberName + "'.");
        }
        // Ler de *qualquer* membro lê o último valor escrito
        if (value == null) {
            throw new RuntimeException("Erro: Lendo membro de union ('" + memberName + "') não inicializado.");
        }
        // Nota: Um compilador C real reinterpretaria os bytes.
        // O nosso interpretador simplesmente retorna o objeto,
        // o que pode causar erros de tipo se o programador não for cuidadoso.
        return value;
    }
}