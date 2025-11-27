#include <stdio.h>
#define MAX 5

struct Ponto {
    int x;
    int y;
};

void imprimirCabecalho() {
    puts("=== TESTE COMPLETO ===");
}

int fatorial(int n) {
    if (n <= 1) {
        return 1;
    }
    return n * fatorial(n - 1);
}

int main() {
    imprimirCabecalho();

    // Teste de tipos e I/O
    int x = 10;
    float pi = 3.14;
    char c = 'A';

    printf("Int: %d, Float: %f, Char: %c\n", x, pi, c);

    // Teste de array
    int arr[5];
    arr[0] = 100;
    printf("Array[0]: %d\n", arr[0]);

    // Teste de loop
    int i;
    printf("Contando: ");
    for (i = 0; i < 3; i = i + 1) {
        printf("%d ", i);
    }
    puts("");

    // Teste de ponteiro
    int *ptr;
    ptr = &x;
    *ptr = 20;
    printf("Novo valor de x (via ptr): %d\n", x);

    // Teste de struct
    struct Ponto p;
    p.x = 5;
    p.y = 10;
    printf("Ponto: (%d, %d)\n", p.x, p.y);

    // Teste de recursao
    printf("Fatorial de 5: %d\n", fatorial(5));

    return 0;
}