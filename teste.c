int main() {
    int x;
    float y;

    x = 10;
    y = 5.5;

    float z;
    z = x + y; // Teste de promoção (10 + 5.5 = 15.5)

    if (z > 15.0) {
        printf("z vale: %f\n", z); // Deve imprimir 15.5
    } else {
        printf("z eh menor ou igual a 15.0\n");
    }
}