#include <stdio.h>
#define MAX_ALUNOS 3
#define NOTA_CORTE 70

struct Aluno {
    int id;
    int nota;
    int aprovado;
};

union Dados {
    int inteiro;
    float decimal;
};

int fibonacci(int n) {
    // CORRIGIDO: Adicionadas chaves {}
    if (n <= 1) {
        return n;
    }
    return fibonacci(n - 1) + fibonacci(n - 2);
}

int verificarAprovacao(int nota) {
    // CORRIGIDO: Adicionadas chaves {}
    if (nota >= NOTA_CORTE) {
        return 1;
    }
    return 0;
}

void imprimirCabecalho() {
    puts("========================================");
    puts("   SISTEMA DE GESTAO ACADEMICA V1.0");
    puts("========================================");
}

int main() {
    imprimirCabecalho();

    char usuario[50];
    printf("Login do Administrador: ");
    gets(usuario);
    printf("Bem-vindo, admin %s!\n", usuario);

    struct Aluno banco[3];

    struct Aluno a1;
    a1.id = 101;
    a1.nota = 85;
    a1.aprovado = verificarAprovacao(a1.nota);
    banco[0] = a1;

    struct Aluno a2;
    a2.id = 102;
    a2.nota = 45;
    a2.aprovado = verificarAprovacao(a2.nota);
    banco[1] = a2;

    struct Aluno a3;
    a3.id = 103;
    a3.nota = fibonacci(11);
    a3.aprovado = verificarAprovacao(a3.nota);
    banco[2] = a3;

    puts("\n[LOG] Dados carregados. Iniciando relatorio...\n");

    int i;
    struct Aluno temp;

    for (i = 0; i < MAX_ALUNOS; i = i + 1) {
        temp = banco[i];
        printf("Aluno ID: %d | Nota: %d | Status: ", temp.id, temp.nota);

        // CORRIGIDO: Adicionadas chaves {}
        if (temp.aprovado) {
            puts("APROVADO");
        } else {
            puts("REPROVADO");
        }
    }

    puts("\n[ADMIN] Alteracao manual de nota via ponteiro...");

    int *ptrNota;
    ptrNota = &a2.nota;

    printf("Nota anterior: %d\n", *ptrNota);

    *ptrNota = 100;
    printf("Nova nota (alterada via *ptr): %d\n", a2.nota);

    // CORRIGIDO: Adicionadas chaves {}
    if (a2.nota >= NOTA_CORTE) {
        a2.aprovado = 1;
    }

    printf("Status atualizado para Aluno %d: %d (1=Sim)\n", a2.id, a2.aprovado);

    puts("\n[SISTEMA] Convertendo codigo de saida...");
    union Dados conversor;
    conversor.inteiro = 0;
    conversor.decimal = 99.9;

    printf("Saida do sistema (float): %f\n", conversor.decimal);

    puts("========================================");

    return 0;
}