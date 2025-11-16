// Em teste.c
struct Ponto {
    int x;
    int y;
};

int main() {
    struct Ponto p1;

    p1.x = 10;
    p1.y = 20;

    printf("O valor de p1.x eh: %d\n", p1.x);
    printf("O valor de p1.y eh: %d\n", p1.y);
}