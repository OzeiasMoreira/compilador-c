// Em teste.c

// 1. Definição da Union
union Valor {
    int i;
    float f;
};

int main() {
    // 2. Instanciação
    union Valor v;

    // 3. Escreve um inteiro
    v.i = 10;
    printf("Valor como inteiro: %d\n", v.i);

    // 4. Escreve um float (sobrescreve o inteiro)
    v.f = 2.5;

    printf("Valor como float: %f\n", v.f);

    // 5. Lê o inteiro (deve falhar no nosso interpretador)
    printf("Valor como inteiro (lido apos float): %d\n", v.i);
}