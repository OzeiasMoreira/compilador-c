package br.uenp.compiladores;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Uso: java -jar compilador.jar <arquivo_fonte.c>");
            System.exit(1);
        }

        try {
            String filePath = args[0];
            CSubsetLexer lexer = new CSubsetLexer(CharStreams.fromFileName(filePath));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            CSubsetParser parser = new CSubsetParser(tokens);

            ParseTree tree = parser.program();

            System.out.println("Análise sintática concluída com sucesso.");

            // Visitor ativado para iniciar a interpretação
            MyVisitor visitor = new MyVisitor();
            visitor.visit(tree);

        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            // Erros de sintaxe ou outros erros
            System.err.println("Erro durante a análise: " + e.getMessage());
            System.exit(1);
        }
    }
}