# Interpretador para Subconjunto da Linguagem C

Projeto final da disciplina de **Compiladores**, do curso de Ciência da Computação da **UENP (Universidade Estadual do Norte do Paraná)**.

Este projeto implementa um **interpretador** capaz de processar, analisar e executar códigos escritos num subconjunto significativo da linguagem C. O sistema foi desenvolvido em **Java** utilizando a ferramenta **ANTLR4** para geração do analisador léxico e sintático.

---

## 🛠️ Tecnologias Utilizadas

* **Linguagem:** Java 17+
* **Parser Generator:** ANTLR 4.13.1
* **Build System:** Apache Maven
* **IDE Recomendada:** IntelliJ IDEA

---

## ✨ Funcionalidades Implementadas

O interpretador suporta uma vasta gama de recursos da linguagem C:

### 1. Tipos de Dados e Memória
* **Tipos Primitivos:** `int`, `float`, `char`, `void`.
* **Arrays:** Declaração (`int v[5]`), acesso e modificação (`v[0] = 10`).
* **Ponteiros:** Declaração (`int *ptr`), endereço (`&x`) e desreferência (`*ptr` para leitura e escrita).
* **Structs:** Definição, instanciação e acesso a membros (`p.x`).
* **Unions:** Suporte a memória partilhada entre membros.

### 2. Estruturas de Controlo
* **Condicionais:** `if`, `else`.
* **Repetição:** `while`, `do-while`, `for`.
* **Seleção:** `switch`, `case`, `default`, `break`.

### 3. Funções e Escopo
* Definição e chamada de funções com parâmetros.
* Suporte a **Recursão** (ex: cálculo de fatorial).
* Instrução `return` (com e sem valor).
* **Escopo Léxico:** Variáveis declaradas dentro de blocos (`{...}`) são destruídas ao sair do escopo (implementado via Pilha de Escopos).

### 4. Entrada e Saída (I/O)
* `printf`: Suporta formatação `%d` (int), `%f` (float), `%c` (char) e `%s` (string/char array).
* `scanf`: Leitura de dados para variáveis (`scanf("%d", &x)`).
* `gets`: Leitura de strings (arrays de char) completas.
* `puts`: Impressão de strings com quebra de linha automática.

### 5. Pré-processador e Outros
* `#define`: Definição de constantes globais.
* `#include`: Reconhecimento e tratamento de inclusões (ex: `<stdio.h>`).
* **Operadores:** Aritméticos (`+ - * /`), Relacionais (`> < >= <= == !=`) e Lógicos (`&& || !`).

---

## ⚙️ Arquitetura do Projeto

O projeto segue o padrão **Visitor** sobre a Árvore Sintática Abstrata (AST) gerada pelo ANTLR.

* **`CSubset.g4`:** Gramática que define as regras léxicas e sintáticas.
* **`MyVisitor.java`:** O "motor" do interpretador. Visita os nós da árvore e executa a lógica Java correspondente.
* **`SymbolTable.java`:** Gerencia a memória utilizando uma **Pilha de Escopos (`Stack<Map>`)**. Isso permite que variáveis locais ocultem variáveis globais e garante a limpeza de memória ao fim de funções ou blocos.
* **`FunctionSymbol.java`:** Armazena a assinatura e o corpo (AST) das funções para execução posterior.
* **`StructDefinition` / `StructInstance`:** Classes auxiliares para gerir a definição (molde) e a memória (instância) de estruturas e uniões.

---

## 🚀 Como Executar

### Pré-requisitos
* **Java JDK 17** ou superior instalado.
* **Apache Maven** instalado e configurado no `PATH` (para execução via terminal).
* (Opcional) **IntelliJ IDEA** para execução via IDE.

### Opção 1: Via Linha de Comando (Terminal)

1.  **Compilar o Projeto:**
    Navegue até a pasta raiz do projeto e execute o comando para limpar compilações anteriores e gerar os ficheiros do compilador:
    ```bash
    mvn clean compile
    ```
    *Aguarde a mensagem "BUILD SUCCESS".*

2.  **Executar o Interpretador:**
    Use o comando `java` definindo o *classpath* para a pasta `target/classes` e indicando o ficheiro de entrada (ex: `teste_completo.c`):
    ```bash
    java -cp target/classes br.uenp.compiladores.Main teste_completo.c
    ```

3.  **Interagir com o Programa:**
    Se o código C contiver `scanf` ou `gets`, o terminal ficará aguardando entrada. Digite o valor e pressione **Enter**.

### Opção 2: Via IntelliJ IDEA

1.  Abra o projeto no IntelliJ (abra o ficheiro `pom.xml`).
2.  No painel lateral **Maven**, execute **Lifecycle -> compile**.
3.  Abra a classe `src/main/java/br/uenp/compiladores/Main.java`.
4.  Vá em **Edit Configurations...** (no topo) e no campo **Program arguments**, insira o nome do ficheiro de teste (ex: `teste_completo.c`).
5.  Execute a classe `Main`.
6.  **Importante:** Clique na aba **Console** na parte inferior da IDE para digitar os dados quando o programa solicitar (para `scanf`/`gets`).

---

## 🧪 Arquivos de Teste Incluídos

* **`teste.c`**: Demonstração completa de todas as funcionalidades (structs, ponteiros, recursão, arrays).