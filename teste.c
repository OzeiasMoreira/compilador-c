int main() {
    int x = 10;

    if (x == 10) {
        int y = 20; // 'y' é criado
        printf("y dentro do if = %d\n", y);
    }

    // Problema: 'y' ainda existe aqui!
    printf("y fora do if = %d\n", y); // Nosso interpretador vai imprimir 20
                                     // Um compilador C real daria "Erro: 'y' não declarado"

    for (int i = 0; i < 5; i = i + 1) {
        // ...
    }

    // Problema: 'i' também "vazou" e existe aqui.
    printf("i = %d\n", i);
}