#include <stdio.h> // Teste de include (deve ser ignorado)
#define MAX 100    // Teste de define
#define PI 3.1415

// 1. Definição de Struct
struct Ponto {
    int x;
    int y;
};

// 2. Definição de Union
union Conversor {
    int i;
    float f;
};

// 3. Função Recursiva
int fatorial(int n) {
    if (n <= 1) {
        return 1;
    }
    return n * fatorial(n - 1);
}

// 4. Função Void
void linha() {
    puts("--------------------------------------------------");
}

int main() {
    // --- TESTE DE I/O e STRINGS ---
    char nome[50];
    int idade;

    linha();
    puts("       TESTE FINAL DO COMPILADOR - OZI       ");
    linha();

    printf("Digite seu nome: ");
    gets(nome); // Teste de gets

    printf("Digite sua idade: ");
    scanf("%d", &idade); // Teste de scanf com ponteiro (&)

    printf("\nBem-vindo, %s! Voce tem %d anos.\n", nome, idade);

    // --- TESTE DE OPERADORES LÓGICOS ---
    if (idade >= 18 && idade < 100) {
        puts("Status: Maior de idade (Validado com && e >=)");
    } else {
        if (!0) { // Teste do NOT (!)
            puts("Status: Menor de idade ou Centenario (Validado com !)");
        }
    }

    // --- TESTE DE LOOPS ---
    printf("\n[Loop For] Contando ate 3: ");
    int i;
    for (i = 1; i <= 3; i = i + 1) {
        printf("%d ", i);
    }
    puts(""); // Nova linha

    printf("[Do-While] Executa pelo menos uma vez: ");
    int k = 10;
    do {
        printf("%d", k);
    } while (k < 5); // Condição falsa logo no início
    puts("");

    // --- TESTE DE SWITCH ---
    printf("\n[Switch] Testando com valor 2: ");
    int op = 2;
    switch (op) {
        case 1: puts("Um"); break;
        case 2: puts("Dois (Correto)"); break;
        default: puts("Outro");
    }

    // --- TESTE DE ARRAYS ---
    printf("\n[Arrays] Preenchendo vetor...\n");
    int numeros[3];
    numeros[0] = 10;
    numeros[1] = 20;
    numeros[2] = 30;
    printf("Valor no indice 1: %d\n", numeros[1]);

    // --- TESTE DE PONTEIROS ---
    printf("\n[Ponteiros] Manipulacao de memoria:\n");
    int valor = 50;
    int *ptr;
    ptr = &valor; // ptr aponta para 'valor'

    printf("Valor original: %d\n", valor);
    *ptr = 999; // Altera 'valor' atraves do ponteiro
    printf("Novo valor (via *ptr): %d\n", valor);

    // --- TESTE DE STRUCT ---
    printf("\n[Structs] Ponto cartesiano:\n");
    struct Ponto p;
    p.x = 10;
    p.y = 20;
    printf("Coordenadas: (%d, %d)\n", p.x, p.y);

    // --- TESTE DE UNION ---
    printf("\n[Unions] Memoria partilhada:\n");
    union Conversor u;
    u.i = 42;
    printf("Union como int: %d\n", u.i);
    u.f = 3.14; // Sobrescreve o inteiro!
    printf("Union como float: %f\n", u.f);
    // printf("Union int (lixo): %d\n", u.i); // Isto daria erro de tipo no nosso interpretador

    // --- TESTE DE FUNÇÃO RECURSIVA ---
    printf("\n[Recursao] Calculando fatorial de 5:\n");
    int fat = fatorial(5);
    printf("Resultado: %d\n", fat);

    linha();
    puts("TESTE CONCLUIDO COM SUCESSO!");
    return 0;
}