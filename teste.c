// Em teste.c
int main() {
    int dia;
    printf("Digite um numero de 1 a 3: ");
    scanf("%d", &dia);

    switch (dia) {
        case 1:
            printf("Voce escolheu 1\n");
            break;
        case 2:
            printf("Voce escolheu 2 (caindo...) \n");
            // Teste de fallthrough
        case 3:
            printf("Voce escolheu 2 ou 3\n");
            break;
        default:
            printf("Valor padrao\n");
    }
}