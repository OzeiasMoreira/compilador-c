// Em teste.c
int main() {
    int x = 10;
    int y = 0;
    int *ptr; // Declara o ponteiro

    printf("x inicial = %d\n", x); // Deve ser 10

    ptr = &x; // Ponteiro agora guarda o "endereço" de x

    *ptr = 20; // Modifica o valor de x ATRAVÉS do ponteiro

    printf("x apos *ptr = 20: %d\n", x); // Deve ser 20

    y = *ptr + 5; // Lê o valor de x (20) e soma 5

    printf("y vale: %d\n", y); // Deve ser 25
}