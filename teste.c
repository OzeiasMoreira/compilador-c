// Ficheiro teste.c para recursão

// Função recursiva para calcular fatorial
int fatorial(int n) {
    if (n < 2) {
        return 1; // Caso base: fatorial(0) ou fatorial(1) é 1
    }

    // Chamada recursiva: n * fatorial(n-1)
    return n * fatorial(n - 1);
}

int main() {
    int num = 5;
    int resultado;

    resultado = fatorial(num);

    printf("O fatorial de %d eh: %d\n", num, resultado); // Deve ser 120
}