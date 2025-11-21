int main() {
    char nome[20]; // Declara um array de caracteres (uma string)

    printf("Digite seu primeiro nome: ");

    // Lê uma palavra do teclado e guarda em 'nome'
    gets(nome);

    // Imprime a string lida
    printf("Ola, %s!\n", nome);

    // Teste extra: Aceder a um caractere individual
    printf("A primeira letra do seu nome eh: %c\n", nome[0]);

    return 0;
}