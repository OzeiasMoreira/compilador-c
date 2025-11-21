package br.uenp.compiladores;

import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Interpretador completo para o subconjunto de C.
 * Implementa: Variáveis, Arrays, Structs, Unions, Ponteiros, Funções, I/O e Controlo de Fluxo.
 */
public class MyVisitor extends CSubsetBaseVisitor<Object> {

    private SymbolTable symbolTable = new SymbolTable();
    private Scanner inputScanner = new Scanner(System.in);
    private FunctionSymbol currentFunction = null;

    // ============================================================
    //               FUNÇÕES AUXILIARES
    // ============================================================

    private Number promoteToNumber(Object obj) {
        if (obj instanceof Number) { return (Number) obj; }
        throw new RuntimeException("Erro de tipo: esperado um número, mas recebido " + obj);
    }

    private boolean forceBoolean(Object obj) {
        if (obj instanceof Boolean) { return (Boolean) obj; }
        if (obj instanceof Integer) { return ((Integer) obj) != 0; }
        if (obj instanceof Double) { return ((Double) obj) != 0.0; }
        if (obj instanceof Character) { return ((Character) obj) != '\0'; }
        throw new RuntimeException("Erro de tipo: não é possível avaliar a expressão como booleana.");
    }

    // ============================================================
    //               PONTO DE ENTRADA (PROGRAMA)
    // ============================================================

    @Override
    public Object visitProgram(CSubsetParser.ProgramContext ctx) {
        // 1. Registar Definições Globais
        for (CSubsetParser.DefineDirectiveContext defineCtx : ctx.defineDirective()) {
            visit(defineCtx);
        }
        for (CSubsetParser.IncludeDirectiveContext includeCtx : ctx.includeDirective()) {
            visit(includeCtx);
        }
        for (CSubsetParser.StructDefinitionContext structCtx : ctx.structDefinition()) {
            visit(structCtx);
        }
        for (CSubsetParser.UnionDefinitionContext unionCtx : ctx.unionDefinition()) {
            visit(unionCtx);
        }
        for (CSubsetParser.FunctionDeclarationContext funcCtx : ctx.functionDeclaration()) {
            visit(funcCtx);
        }

        // 2. Encontrar e Executar a função 'main'
        FunctionSymbol mainFunction = symbolTable.resolveFunction("main");
        if (mainFunction == null) {
            throw new RuntimeException("Erro: Função 'main' não encontrada.");
        }
        try {
            executeFunction(mainFunction, new ArrayList<>());
        } catch (ReturnException re) {
            // O main terminou normalmente
        }
        return null;
    }

    // ============================================================
    //               FUNÇÕES E CHAMADAS
    // ============================================================

    @Override
    public Object visitFunctionDeclaration(CSubsetParser.FunctionDeclarationContext ctx) {
        String funcType = ctx.type().getText();
        String funcName = ctx.ID().getText();

        List<Map.Entry<String, String>> params = new ArrayList<>();
        if (ctx.paramList() != null) {
            for (CSubsetParser.ParamContext paramCtx : ctx.paramList().param()) {
                String paramType = paramCtx.type().getText();
                String paramName = paramCtx.ID().getText();
                params.add(Map.entry(paramType, paramName));
            }
        }

        FunctionSymbol func = new FunctionSymbol(funcName, funcType, params, ctx.block());
        symbolTable.addFunction(funcName, func);
        System.out.println("SEMÂNTICA: Registando função '" + funcName + "'");
        return null;
    }

    // Método auxiliar para executar qualquer função
    private Object executeFunction(FunctionSymbol function, List<Object> args) {
        if (function.getParameters().size() != args.size()) {
            throw new RuntimeException("Erro: Número incorreto de argumentos para a função '" + function.getName() + "'.");
        }

        // Guarda o contexto anterior (para recursão funcionar)
        FunctionSymbol previousFunction = this.currentFunction;
        this.currentFunction = function;

        symbolTable.enterScope();
        try {
            // Registra os parâmetros no escopo da função
            for (int i = 0; i < args.size(); i++) {
                String paramType = function.getParameters().get(i).getKey();
                String paramName = function.getParameters().get(i).getValue();
                Object paramValue = args.get(i);
                symbolTable.add(paramName, paramType);
                symbolTable.assign(paramName, paramValue);
            }

            // Executa o corpo da função
            visit(function.getBody());

        } finally {
            symbolTable.exitScope();
            this.currentFunction = previousFunction; // Restaura
        }

        // Validação de retorno para funções não-void
        if (!function.getType().equals("void")) {
            throw new RuntimeException("Erro: Função não-void '" + function.getName() + "' chegou ao fim sem 'return'.");
        }
        return null;
    }

    @Override
    public Object visitFunctionCall(CSubsetParser.FunctionCallContext ctx) {
        String funcName = ctx.ID().getText();
        List<Object> args = new ArrayList<>();
        if (ctx.argList() != null) {
            for (CSubsetParser.ExpressionContext exprCtx : ctx.argList().expression()) {
                args.add(visit(exprCtx));
            }
        }
        FunctionSymbol function = symbolTable.resolveFunction(funcName);
        try {
            return executeFunction(function, args);
        } catch (ReturnException re) {
            return re.getValue();
        }
    }

    @Override
    public Object visitFunctionCallStatement(CSubsetParser.FunctionCallStatementContext ctx) {
        visit(ctx.functionCall()); // Apenas visita e ignora o retorno
        return null;
    }

    @Override
    public Object visitReturnStatement(CSubsetParser.ReturnStatementContext ctx) {
        if (currentFunction == null) {
            throw new RuntimeException("Erro: 'return' encontrado fora de uma função.");
        }
        String funcType = currentFunction.getType();

        if (ctx.expression() == null) {
            if (!funcType.equals("void")) {
                throw new RuntimeException("Erro: Função não-void ("+ funcType +") deve retornar um valor.");
            }
            throw new ReturnException(null);
        } else {
            if (funcType.equals("void")) {
                throw new RuntimeException("Erro: Função 'void' não pode retornar um valor.");
            }
            Object returnValue = visit(ctx.expression());
            throw new ReturnException(returnValue);
        }
    }

    // ============================================================
    //               ENTRADA E SAÍDA (I/O)
    // ============================================================

    @Override
    public Object visitPrintfStatement(CSubsetParser.PrintfStatementContext ctx) {
        String formatString = ctx.STRING_LITERAL().getText();
        formatString = formatString.substring(1, formatString.length() - 1);

        if (ctx.argList() != null) {
            for (CSubsetParser.ExpressionContext exprCtx : ctx.argList().expression()) {
                Object value = visit(exprCtx);

                if (formatString.contains("%d") && value instanceof Integer) {
                    formatString = formatString.replaceFirst("%d", value.toString());
                } else if (formatString.contains("%f") && (value instanceof Double || value instanceof Integer)) {
                    formatString = formatString.replaceFirst("%f", value.toString());
                } else if (formatString.contains("%c") && value instanceof Character) {
                    formatString = formatString.replaceFirst("%c", value.toString());
                } else if (formatString.contains("%s") && value instanceof Object[]) {
                    // Tratamento de Strings (char[])
                    Object[] arr = (Object[]) value;
                    StringBuilder sb = new StringBuilder();
                    for (Object o : arr) {
                        if (o == null || (o instanceof Character && (Character)o == '\0')) break;
                        sb.append(o);
                    }
                    formatString = formatString.replaceFirst("%s", sb.toString());
                }
            }
        }

        formatString = formatString.replace("\\n", "\n");
        System.out.print(formatString);
        System.out.flush(); // Importante!
        return null;
    }

    @Override
    public Object visitScanfStatement(CSubsetParser.ScanfStatementContext ctx) {
        String formatString = ctx.STRING_LITERAL().getText();
        String varName = ctx.ID().getText();
        formatString = formatString.substring(1, formatString.length() - 1);
        try {
            String varType = symbolTable.getType(varName);
            if (formatString.equals("%d") && varType.equals("int")) {
                System.out.println("INTERPRETADOR: Aguardando entrada (int)...");
                int value = inputScanner.nextInt();
                symbolTable.assign(varName, value);
            } else if (formatString.equals("%f") && varType.equals("float")) {
                System.out.println("INTERPRETADOR: Aguardando entrada (float)...");
                double value = inputScanner.nextDouble();
                symbolTable.assign(varName, value);
            } else if (formatString.equals("%c") && varType.equals("char")) {
                System.out.println("INTERPRETADOR: Aguardando entrada (char)...");
                char value = inputScanner.next().charAt(0);
                symbolTable.assign(varName, value);
            } else {
                throw new RuntimeException("Erro de tipo no scanf ou formato não suportado: " + formatString);
            }
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    @Override
    public Object visitGetsStatement(CSubsetParser.GetsStatementContext ctx) {
        String varName = ctx.ID().getText();
        try {
            Object obj = symbolTable.resolve(varName);
            if (!(obj instanceof Object[])) {
                throw new RuntimeException("Erro: 'gets' espera um array (string) como argumento.");
            }
            Object[] array = (Object[]) obj;

            System.out.println("INTERPRETADOR: Aguardando entrada de texto (gets)...");
            String input = inputScanner.next();

            for (int i = 0; i < array.length && i < input.length(); i++) {
                array[i] = input.charAt(i);
            }
            if (input.length() < array.length) {
                array[input.length()] = '\0';
            }
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    @Override
    public Object visitPutsStatement(CSubsetParser.PutsStatementContext ctx) {
        String str = ctx.STRING_LITERAL().getText();
        str = str.substring(1, str.length() - 1).replace("\\n", "\n");
        System.out.println(str);
        return null;
    }

    // ============================================================
    //               DECLARAÇÕES E ATRIBUIÇÕES (Memória)
    // ============================================================

    @Override
    public Object visitSimpleDeclaration(CSubsetParser.SimpleDeclarationContext ctx) {
        String varType = ctx.type().getText();
        String varName = ctx.ID().getText();
        System.out.println("SEMÂNTICA: Declarando variável '" + varName + "' do tipo '" + varType + "'");
        try {
            symbolTable.add(varName, varType);

            // Declaração de Array
            if (ctx.LBRACKET() != null) {
                if (ctx.ASSIGN() != null) {
                    throw new RuntimeException("Erro: Inicialização de array na declaração não é suportada.");
                }
                int size = Integer.parseInt(ctx.INT().getText());
                System.out.println("INTERPRETADOR: Alocando array '" + varName + "' com tamanho " + size);
                symbolTable.assign(varName, new Object[size]);
            }
            // Declaração com Inicialização
            else if (ctx.ASSIGN() != null) {
                Object value = visit(ctx.expression());
                System.out.println("INTERPRETADOR: Inicializando '" + varName + "' com " + value);
                symbolTable.assign(varName, value);
            }
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    @Override
    public Object visitSimpleAssignment(CSubsetParser.SimpleAssignmentContext ctx) {
        Object rhsValue = visit(ctx.expression());
        CSubsetParser.LvalueContext lvalue = ctx.lvalue();
        try {
            // Atribuição Simples
            if (lvalue.ID() != null) {
                String varName = lvalue.ID().getText();
                System.out.println("INTERPRETADOR: Atribuindo " + rhsValue + " para '" + varName + "'");
                symbolTable.assign(varName, rhsValue);
            }
            // Atribuição a Array
            else if (lvalue.arrayAccess() != null) {
                String varName = lvalue.arrayAccess().ID().getText();
                Object arrayObj = symbolTable.resolve(varName);
                if (!(arrayObj instanceof Object[])) throw new RuntimeException("Erro: Variável não é um array.");
                Object[] array = (Object[]) arrayObj;
                int index = (Integer) visit(lvalue.arrayAccess().expression());
                System.out.println("INTERPRETADOR: Atribuindo " + rhsValue + " para '" + varName + "[" + index + "]'");
                array[index] = rhsValue;
            }
            // Atribuição a Struct/Union
            else if (lvalue.memberAccess() != null) {
                String instanceName = lvalue.memberAccess().ID(0).getText();
                String memberName = lvalue.memberAccess().ID(1).getText();
                Object obj = symbolTable.resolve(instanceName);
                if (obj instanceof StructInstance) {
                    ((StructInstance) obj).write(memberName, rhsValue);
                } else if (obj instanceof UnionInstance) {
                    ((UnionInstance) obj).write(memberName, rhsValue);
                } else {
                    throw new RuntimeException("Erro: Não é struct nem union.");
                }
                System.out.println("INTERPRETADOR: Atribuindo " + rhsValue + " para " + instanceName + "." + memberName);
            }
            // Atribuição a Ponteiro (*ptr = 10)
            else if (lvalue.unaryExpr() != null) {
                Object resolvedName = visit(lvalue.unaryExpr());
                if (!(resolvedName instanceof String)) throw new RuntimeException("Erro: Desreferência inválida.");
                String varName = (String) resolvedName;
                System.out.println("INTERPRETADOR: Atribuindo (via ponteiro) " + rhsValue + " para '" + varName + "'");
                symbolTable.assign(varName, rhsValue);
            }
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    // ============================================================
    //               EXPRESSÕES (Aritmética, Lógica, Ponteiros)
    // ============================================================

    @Override
    public Object visitUnaryExpr(CSubsetParser.UnaryExprContext ctx) {
        if (ctx.NOT() != null) { // !x
            return !forceBoolean(visit(ctx.unaryExpr()));
        } else if (ctx.AMPERSAND() != null) { // &x
            String varName = ctx.unaryExpr().getText();
            System.out.println("INTERPRETADOR: Obtendo endereço de '" + varName + "'");
            return varName;
        } else if (ctx.STAR() != null) { // *ptr
            Object ptrValue = visit(ctx.unaryExpr());
            if (!(ptrValue instanceof String)) throw new RuntimeException("Erro: Tentativa de desreferência em não-ponteiro.");
            String varName = (String) ptrValue;
            System.out.println("INTERPRETADOR: Lendo (via ponteiro) o valor de '" + varName + "'");
            return symbolTable.resolve(varName);
        } else {
            return visit(ctx.primaryExpr());
        }
    }

    @Override
    public Object visitPrimaryExpr(CSubsetParser.PrimaryExprContext ctx) {
        if (ctx.INT() != null) return Integer.parseInt(ctx.INT().getText());
        if (ctx.FLOAT() != null) return Double.parseDouble(ctx.FLOAT().getText());
        if (ctx.CHAR_LITERAL() != null) return ctx.CHAR_LITERAL().getText().charAt(1);

        if (ctx.ID() != null) {
            String name = ctx.ID().getText();
            if (symbolTable.isDefine(name)) return symbolTable.resolveDefine(name);
            try { return symbolTable.resolve(name); } catch (RuntimeException e) { System.err.println(e.getMessage()); return null; }
        }
        if (ctx.functionCall() != null) return visit(ctx.functionCall());
        if (ctx.arrayAccess() != null) return visit(ctx.arrayAccess());
        if (ctx.memberAccess() != null) return visit(ctx.memberAccess());
        if (ctx.expression() != null) return visit(ctx.expression());

        return null;
    }

    @Override
    public Object visitMultExpr(CSubsetParser.MultExprContext ctx) {
        Object left = visit(ctx.unaryExpr(0));
        for (int i = 1; i < ctx.unaryExpr().size(); i++) {
            Object right = visit(ctx.unaryExpr(i));
            String op = ctx.getChild(i * 2 - 1).getText();
            Number l = promoteToNumber(left);
            Number r = promoteToNumber(right);

            if (l instanceof Double || r instanceof Double) {
                double v1 = l.doubleValue(), v2 = r.doubleValue();
                switch(op) {
                    case "*": left = v1 * v2; break;
                    case "/": if (v2 == 0.0) throw new RuntimeException("Erro: Divisão por zero."); left = v1 / v2; break;
                }
            } else {
                int v1 = l.intValue(), v2 = r.intValue();
                switch(op) {
                    case "*": left = v1 * v2; break;
                    case "/": if (v2 == 0) throw new RuntimeException("Erro: Divisão por zero."); left = v1 / v2; break;
                }
            }
        }
        return left;
    }

    @Override
    public Object visitAddExpr(CSubsetParser.AddExprContext ctx) {
        Object left = visit(ctx.multExpr(0));
        for (int i = 1; i < ctx.multExpr().size(); i++) {
            Object right = visit(ctx.multExpr(i));
            String op = ctx.getChild(i * 2 - 1).getText();
            Number l = promoteToNumber(left);
            Number r = promoteToNumber(right);

            if (l instanceof Double || r instanceof Double) {
                double v1 = l.doubleValue(), v2 = r.doubleValue();
                left = op.equals("+") ? v1 + v2 : v1 - v2;
            } else {
                int v1 = l.intValue(), v2 = r.intValue();
                left = op.equals("+") ? v1 + v2 : v1 - v2;
            }
        }
        return left;
    }

    @Override
    public Object visitRelExpr(CSubsetParser.RelExprContext ctx) {
        Object left = visit(ctx.addExpr(0));
        if (ctx.addExpr().size() < 2) return left;
        Object right = visit(ctx.addExpr(1));
        String op = ctx.getChild(1).getText();

        if (left instanceof Number && right instanceof Number) {
            double v1 = ((Number)left).doubleValue();
            double v2 = ((Number)right).doubleValue();
            switch (op) {
                case ">": return v1 > v2; case ">=": return v1 >= v2;
                case "<": return v1 < v2; case "<=": return v1 <= v2;
                case "==": return v1 == v2; case "!=": return v1 != v2;
            }
        } else if (left instanceof Character && right instanceof Character) {
            char v1 = (Character)left;
            char v2 = (Character)right;
            switch (op) {
                case ">": return v1 > v2; case ">=": return v1 >= v2;
                case "<": return v1 < v2; case "<=": return v1 <= v2;
                case "==": return v1 == v2; case "!=": return v1 != v2;
            }
        }
        throw new RuntimeException("Erro de tipo na comparação.");
    }

    @Override
    public Object visitLogicalAndExpr(CSubsetParser.LogicalAndExprContext ctx) {
        if (ctx.relExpr().size() < 2) return visit(ctx.relExpr(0)); // Correção: Usa relExpr
        Object left = visit(ctx.relExpr(0));
        if (!forceBoolean(left)) return false; // Curto-circuito
        for (int i = 1; i < ctx.relExpr().size(); i++) {
            if (!forceBoolean(visit(ctx.relExpr(i)))) return false;
        }
        return true;
    }

    @Override
    public Object visitLogicalOrExpr(CSubsetParser.LogicalOrExprContext ctx) {
        if (ctx.logicalAndExpr().size() < 2) return visit(ctx.logicalAndExpr(0));
        Object left = visit(ctx.logicalAndExpr(0));
        if (forceBoolean(left)) return true; // Curto-circuito
        for (int i = 1; i < ctx.logicalAndExpr().size(); i++) {
            if (forceBoolean(visit(ctx.logicalAndExpr(i)))) return true;
        }
        return false;
    }

    // ============================================================
    //               ESTRUTURAS E DEFINIÇÕES
    // ============================================================

    @Override
    public Object visitStructDefinition(CSubsetParser.StructDefinitionContext ctx) {
        String name = ctx.ID().getText();
        System.out.println("SEMÂNTICA: Registando struct '" + name + "'");
        StructDefinition def = new StructDefinition();
        for (CSubsetParser.StructMemberContext m : ctx.structMember()) {
            def.addMember(m.ID().getText(), m.type().getText());
        }
        symbolTable.addStructDefinition(name, def);
        return null;
    }

    @Override
    public Object visitUnionDefinition(CSubsetParser.UnionDefinitionContext ctx) {
        String name = ctx.ID().getText();
        System.out.println("SEMÂNTICA: Registando union '" + name + "'");
        StructDefinition def = new StructDefinition();
        for (CSubsetParser.StructMemberContext m : ctx.structMember()) {
            def.addMember(m.ID().getText(), m.type().getText());
        }
        symbolTable.addUnionDefinition(name, def);
        return null;
    }

    @Override
    public Object visitDefineDirective(CSubsetParser.DefineDirectiveContext ctx) {
        String name = ctx.ID().getText();
        Object val = (ctx.INT()!=null) ? Integer.parseInt(ctx.INT().getText()) : Double.parseDouble(ctx.FLOAT().getText());
        System.out.println("PRÉ-PROCESSADOR: Definindo '" + name + "' como " + val);
        symbolTable.addDefine(name, val);
        return null;
    }

    @Override
    public Object visitIncludeDirective(CSubsetParser.IncludeDirectiveContext ctx) {
        System.out.println("PRÉ-PROCESSADOR: Ignorando '" + ctx.getText() + "'");
        return null;
    }

    // --- ACESSO A MEMBROS E ARRAYS ---

    @Override public Object visitArrayAccess(CSubsetParser.ArrayAccessContext ctx) {
        try {
            Object[] arr = (Object[]) symbolTable.resolve(ctx.ID().getText());
            return arr[(Integer)visit(ctx.expression())];
        } catch (Exception e) { System.err.println(e.getMessage()); return null; }
    }
    @Override public Object visitMemberAccess(CSubsetParser.MemberAccessContext ctx) {
        try {
            Object obj = symbolTable.resolve(ctx.ID(0).getText());
            String member = ctx.ID(1).getText();
            if (obj instanceof StructInstance) return ((StructInstance)obj).read(member);
            if (obj instanceof UnionInstance) return ((UnionInstance)obj).read(member);
            throw new RuntimeException("Erro: Não é struct/union.");
        } catch (Exception e) { System.err.println(e.getMessage()); return null; }
    }

    // --- BOILERPLATE (Delegar Declaração e Atribuição) ---
    @Override public Object visitDeclaration(CSubsetParser.DeclarationContext ctx) { return visit(ctx.simpleDeclaration()); }
    @Override public Object visitAssignment(CSubsetParser.AssignmentContext ctx) { return visit(ctx.simpleAssignment()); }

    // ============================================================
    //               ESTRUTURAS DE CONTROLO
    // ============================================================

    @Override public Object visitBlock(CSubsetParser.BlockContext ctx) {
        symbolTable.enterScope();
        Object res = super.visitChildren(ctx);
        symbolTable.exitScope();
        return res;
    }
    @Override public Object visitIfStatement(CSubsetParser.IfStatementContext ctx) {
        if (forceBoolean(visit(ctx.expression()))) visit(ctx.block(0));
        else if (ctx.ELSE() != null) visit(ctx.block(1));
        return null;
    }
    @Override public Object visitWhileStatement(CSubsetParser.WhileStatementContext ctx) {
        while (forceBoolean(visit(ctx.expression()))) visit(ctx.block());
        return null;
    }
    @Override public Object visitDoWhileStatement(CSubsetParser.DoWhileStatementContext ctx) {
        do { visit(ctx.block()); } while (forceBoolean(visit(ctx.expression())));
        return null;
    }
    @Override public Object visitForStatement(CSubsetParser.ForStatementContext ctx) {
        symbolTable.enterScope();
        if (ctx.init != null) visit(ctx.init);
        while (ctx.cond == null || forceBoolean(visit(ctx.cond))) {
            visit(ctx.block());
            if (ctx.inc != null) visit(ctx.inc);
        }
        symbolTable.exitScope();
        return null;
    }
    @Override public Object visitSwitchStatement(CSubsetParser.SwitchStatementContext ctx) {
        int val = (Integer) visit(ctx.expression());
        boolean found = false, brk = false;
        for (CSubsetParser.CaseBlockContext c : ctx.caseBlock()) {
            if (found || val == Integer.parseInt(c.INT().getText())) {
                found = true;
                for (CSubsetParser.StatementContext s : c.statement()) visit(s);
                if (c.BREAK() != null) { brk = true; break; }
            }
        }
        if (!found && !brk && ctx.defaultBlock() != null) {
            for (CSubsetParser.StatementContext s : ctx.defaultBlock().statement()) visit(s);
        }
        return null;
    }
}