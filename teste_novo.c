// Em teste.c
int main() {
    int x = 10;
    int y = 0;
    int *ptr;

    printf("x inicial = %d\n", x);

    ptr = &x; // ptr agora "aponta" para x

    *ptr = 20; // Modifica x através de ptr

    printf("x apos *ptr = 20: %d\n", x);

    y = *ptr + 5; // Lê x (20) através de ptr e soma 5

    printf("y vale: %d\n", y);
}