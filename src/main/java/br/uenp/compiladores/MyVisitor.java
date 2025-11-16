package br.uenp.compiladores;

import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MyVisitor extends CSubsetBaseVisitor<Object> {

    private SymbolTable symbolTable = new SymbolTable();
    private Scanner inputScanner = new Scanner(System.in);

    // --- FUNÇÕES AUXILIARES (Sem mudanças) ---
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

    // --- LÓGICA DE DECLARAÇÃO E ATRIBUIÇÃO (Atualizada para Arrays) ---

    @Override
    public Object visitSimpleDeclaration(CSubsetParser.SimpleDeclarationContext ctx) {
        String varType = ctx.type().getText();
        String varName = ctx.ID().getText();
        System.out.println("SEMÂNTICA: Declarando variável '" + varName + "' do tipo '" + varType + "'");

        try {
            // 1. Adiciona a variável à tabela de símbolos
            symbolTable.add(varName, varType);

            // 2. Verifica se é uma declaração de array (ex: int arr[5])
            if (ctx.LBRACKET() != null) {
                if (ctx.ASSIGN() != null) {
                    throw new RuntimeException("Erro: Inicialização de array na declaração não é suportada.");
                }

                // Aloca o array
                int size = Integer.parseInt(ctx.INT().getText());
                Object[] array = new Object[size];
                System.out.println("INTERPRETADOR: Alocando array '" + varName + "' com tamanho " + size);
                symbolTable.assign(varName, array); // Guarda o array Java no 'value' do Símbolo

                // 3. Verifica se é uma inicialização de escalar (ex: int x = 10)
            } else if (ctx.ASSIGN() != null) {
                Object value = visit(ctx.expression());
                System.out.println("INTERPRETADOR: Inicializando '" + varName + "' com " + value);
                symbolTable.assign(varName, value);
            }
            // 4. Se for só 'int x;', não faz nada (valor fica 'null')

        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    @Override
    public Object visitSimpleAssignment(CSubsetParser.SimpleAssignmentContext ctx) {
        // Visita a expressão à direita (o valor a ser atribuído)
        Object value = visit(ctx.expression());

        // Verifica se é uma atribuição a um escalar (ex: x = 10)
        if (ctx.ID() != null) {
            String varName = ctx.ID().getText();
            System.out.println("INTERPRETADOR: Atribuindo " + value + " para '" + varName + "'");
            try {
                symbolTable.assign(varName, value);
            } catch (RuntimeException e) {
                System.err.println(e.getMessage());
            }
        }
        // Verifica se é uma atribuição a um array (ex: arr[0] = 10)
        else if (ctx.arrayAccess() != null) {
            String varName = ctx.arrayAccess().ID().getText();

            try {
                // Pega o array da memória
                Object arrayObj = symbolTable.resolve(varName);
                if (!(arrayObj instanceof Object[])) {
                    throw new RuntimeException("Erro: Tentando indexar uma variável ('" + varName + "') que não é um array.");
                }
                Object[] array = (Object[]) arrayObj;

                // Calcula o índice
                Object indexObj = visit(ctx.arrayAccess().expression());
                if (!(indexObj instanceof Integer)) {
                    throw new RuntimeException("Erro: Índice do array deve ser um inteiro.");
                }
                int index = (Integer) indexObj;

                System.out.println("INTERPRETADOR: Atribuindo " + value + " para '" + varName + "[" + index + "]'");

                // Atribui o valor ao índice
                array[index] = value;

            } catch (RuntimeException e) {
                System.err.println(e.getMessage());
            }
        }
        return null;
    }

    // --- LÓGICA DE EXPRESSÕES (Atualizada) ---

    // NOVO MÉTODO: Chamado quando lemos um valor (ex: x = arr[0])
    @Override
    public Object visitArrayAccess(CSubsetParser.ArrayAccessContext ctx) {
        String varName = ctx.ID().getText();

        try {
            // Pega o array da memória
            Object arrayObj = symbolTable.resolve(varName);
            if (!(arrayObj instanceof Object[])) {
                throw new RuntimeException("Erro: Tentando indexar uma variável ('" + varName + "') que não é um array.");
            }
            Object[] array = (Object[]) arrayObj;

            // Calcula o índice
            Object indexObj = visit(ctx.expression());
            if (!(indexObj instanceof Integer)) {
                throw new RuntimeException("Erro: Índice do array deve ser um inteiro.");
            }
            int index = (Integer) indexObj;

            System.out.println("INTERPRETADOR: Lendo valor de '" + varName + "[" + index + "]'");

            // Retorna o valor do índice
            Object value = array[index];
            if (value == null) {
                throw new RuntimeException("Erro: Lendo índice de array não inicializado.");
            }
            return value;

        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            return null; // Retorna null em caso de erro
        }
    }

    @Override
    public Object visitPrimaryExpr(CSubsetParser.PrimaryExprContext ctx) {
        if (ctx.INT() != null) {
            return Integer.parseInt(ctx.INT().getText());
        }
        if (ctx.FLOAT() != null) {
            return Double.parseDouble(ctx.FLOAT().getText());
        }
        if (ctx.CHAR_LITERAL() != null) {
            String text = ctx.CHAR_LITERAL().getText();
            return text.charAt(1);
        }
        if (ctx.ID() != null) { // É uma variável escalar
            try {
                return symbolTable.resolve(ctx.ID().getText());
            } catch (RuntimeException e) {
                System.err.println(e.getMessage());
                return null;
            }
        }
        if (ctx.functionCall() != null) { // É uma chamada de função
            return visit(ctx.functionCall());
        }
        if (ctx.arrayAccess() != null) { // É uma leitura de array
            return visit(ctx.arrayAccess());
        }
        if (ctx.expression() != null) {
            return visit(ctx.expression());
        }
        return null;
    }

    // --- MÉTODOS RESTANTES (Sem o visitUnaryExpr) ---

    @Override
    public Object visitDeclaration(CSubsetParser.DeclarationContext ctx) {
        return visit(ctx.simpleDeclaration());
    }
    @Override
    public Object visitAssignment(CSubsetParser.AssignmentContext ctx) {
        return visit(ctx.simpleAssignment());
    }
    @Override
    public Object visitLogicalOrExpr(CSubsetParser.LogicalOrExprContext ctx) {
        if (ctx.logicalAndExpr().size() < 2) {
            return visit(ctx.logicalAndExpr(0));
        }
        Object left = visit(ctx.logicalAndExpr(0));
        boolean leftBool = forceBoolean(left);
        if (leftBool) {
            return true;
        }
        for (int i = 1; i < ctx.logicalAndExpr().size(); i++) {
            Object right = visit(ctx.logicalAndExpr(i));
            boolean rightBool = forceBoolean(right);
            if (rightBool) {
                return true;
            }
        }
        return false;
    }
    @Override
    public Object visitLogicalAndExpr(CSubsetParser.LogicalAndExprContext ctx) {
        if (ctx.relExpr().size() < 2) {
            return visit(ctx.relExpr(0));
        }
        Object left = visit(ctx.relExpr(0));
        boolean leftBool = forceBoolean(left);
        if (!leftBool) {
            return false;
        }
        for (int i = 1; i < ctx.relExpr().size(); i++) {
            Object right = visit(ctx.relExpr(i));
            boolean rightBool = forceBoolean(right);
            if (!rightBool) {
                return false;
            }
        }
        return true;
    }

    // O MÉTODO visitUnaryExpr FOI REMOVIDO DESTA VERSÃO

    @Override
    public Object visitRelExpr(CSubsetParser.RelExprContext ctx) {
        Object left = visit(ctx.addExpr(0));
        if (ctx.addExpr().size() < 2) {
            return left;
        }
        Object right = visit(ctx.addExpr(1));
        String op = ctx.getChild(1).getText();
        if (left instanceof Number && right instanceof Number) {
            Number leftNum = (Number) left;
            Number rightNum = (Number) right;
            double leftVal = leftNum.doubleValue();
            double rightVal = rightNum.doubleValue();
            switch (op) {
                case ">": return leftVal > rightVal;
                case "<": return leftVal < rightVal;
                case "==": return leftVal == rightVal;
                case "!=": return leftVal != rightVal;
                default:
                    throw new RuntimeException("Operador relacional desconhecido para números: " + op);
            }
        }
        else if (left instanceof Character && right instanceof Character) {
            Character leftChar = (Character) left;
            Character rightChar = (Character) right;
            switch (op) {
                case ">": return leftChar > rightChar;
                case "<": return leftChar < rightChar;
                case "==": return leftChar == rightChar;
                case "!=": return leftChar != rightChar;
                default:
                    throw new RuntimeException("Operador relacional desconhecido para caracteres: " + op);
            }
        }
        else {
            throw new RuntimeException("Erro de tipo: não é possível comparar " + left.getClass().getName() + " com " + right.getClass().getName());
        }
    }
    @Override
    public Object visitAddExpr(CSubsetParser.AddExprContext ctx) {
        Object left = visit(ctx.multExpr(0));
        for (int i = 1; i < ctx.multExpr().size(); i++) {
            Object right = visit(ctx.multExpr(i));
            String op = ctx.getChild(i * 2 - 1).getText();
            Number leftNum = promoteToNumber(left);
            Number rightNum = promoteToNumber(right);
            if (leftNum instanceof Double || rightNum instanceof Double) {
                double leftVal = leftNum.doubleValue();
                double rightVal = rightNum.doubleValue();
                switch (op) {
                    case "+": left = leftVal + rightVal; break;
                    case "-": left = leftVal - rightVal; break;
                }
            } else {
                int leftVal = leftNum.intValue();
                int rightVal = rightNum.intValue();
                switch (op) {
                    case "+": left = leftVal + rightVal; break;
                    case "-": left = leftVal - rightVal; break;
                }
            }
        }
        return left;
    }
    @Override
    public Object visitMultExpr(CSubsetParser.MultExprContext ctx) {
        Object left = visit(ctx.primaryExpr(0));
        for (int i = 1; i < ctx.primaryExpr().size(); i++) {
            Object right = visit(ctx.primaryExpr(i));
            String op = ctx.getChild(i * 2 - 1).getText();
            Number leftNum = promoteToNumber(left);
            Number rightNum = promoteToNumber(right);
            if (leftNum instanceof Double || rightNum instanceof Double) {
                double leftVal = leftNum.doubleValue();
                double rightVal = rightNum.doubleValue();
                switch (op) {
                    case "*": left = leftVal * rightVal; break;
                    case "/":
                        if (rightVal == 0.0) throw new RuntimeException("Erro: Divisão por zero.");
                        left = leftVal / rightVal;
                        break;
                }
            } else {
                int leftVal = leftNum.intValue();
                int rightVal = rightNum.intValue();
                switch (op) {
                    case "*": left = leftVal * rightVal; break;
                    case "/":
                        if (rightVal == 0) throw new RuntimeException("Erro: Divisão por zero.");
                        left = leftVal / rightVal;
                        break;
                }
            }
        }
        return left;
    }
    @Override
    public Object visitProgram(CSubsetParser.ProgramContext ctx) {
        for (CSubsetParser.FunctionDeclarationContext funcCtx : ctx.functionDeclaration()) {
            visit(funcCtx);
        }
        FunctionSymbol mainFunction = symbolTable.resolveFunction("main");
        if (mainFunction == null) {
            throw new RuntimeException("Erro: Função 'main' não encontrada.");
        }
        try {
            executeFunction(mainFunction, new ArrayList<>());
        } catch (ReturnException re) {
            // Ignorar valor de retorno do main
        }
        return null;
    }
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
        FunctionSymbol func = new FunctionSymbol(funcType, params, ctx.block());
        symbolTable.addFunction(funcName, func);
        System.out.println("SEMÂNTICA: Registando função '" + funcName + "'");
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
    private Object executeFunction(FunctionSymbol function, List<Object> args) {
        if (function.getParameters().size() != args.size()) {
            throw new RuntimeException("Erro: Número incorreto de argumentos para a função.");
        }
        symbolTable.enterScope();
        for (int i = 0; i < args.size(); i++) {
            String paramType = function.getParameters().get(i).getKey();
            String paramName = function.getParameters().get(i).getValue();
            Object paramValue = args.get(i);
            symbolTable.add(paramName, paramType);
            symbolTable.assign(paramName, paramValue);
        }
        visit(function.getBody());
        symbolTable.exitScope();
        return null;
    }
    @Override
    public Object visitReturnStatement(CSubsetParser.ReturnStatementContext ctx) {
        Object returnValue = null;
        if (ctx.expression() != null) {
            returnValue = visit(ctx.expression());
        }
        throw new ReturnException(returnValue);
    }
    @Override
    public Object visitBlock(CSubsetParser.BlockContext ctx) {
        symbolTable.enterScope();
        Object result = super.visitChildren(ctx);
        symbolTable.exitScope();
        return result;
    }
    @Override
    public Object visitForStatement(CSubsetParser.ForStatementContext ctx) {
        symbolTable.enterScope();
        if (ctx.init != null) {
            visit(ctx.init);
        }
        boolean isTrue = true;
        if (ctx.cond != null) {
            Object conditionResult = visit(ctx.cond);
            isTrue = forceBoolean(conditionResult);
        }
        while (isTrue) {
            visit(ctx.block());
            if (ctx.inc != null) {
                visit(ctx.inc);
            }
            if (ctx.cond != null) {
                Object conditionResult = visit(ctx.cond);
                isTrue = forceBoolean(conditionResult);
            }
        }
        symbolTable.exitScope();
        return null;
    }
    @Override
    public Object visitSwitchStatement(CSubsetParser.SwitchStatementContext ctx) {
        Object switchValue = visit(ctx.expression());
        if (!(switchValue instanceof Integer)) {
            throw new RuntimeException("Erro de tipo: expressão 'switch' deve ser um inteiro.");
        }
        int switchInt = (Integer) switchValue;
        boolean caseFound = false;
        boolean breakFound = false;
        for (CSubsetParser.CaseBlockContext caseCtx : ctx.caseBlock()) {
            int caseInt = Integer.parseInt(caseCtx.INT().getText());
            if (caseFound || switchInt == caseInt) {
                caseFound = true;
                for (CSubsetParser.StatementContext stmtCtx : caseCtx.statement()) {
                    visit(stmtCtx);
                }
                if (caseCtx.BREAK() != null) {
                    breakFound = true;
                    break;
                }
            }
        }
        if (ctx.defaultBlock() != null && !breakFound) {
            for (CSubsetParser.StatementContext stmtCtx : ctx.defaultBlock().statement()) {
                visit(stmtCtx);
            }
        }
        return null;
    }
    @Override
    public Object visitDoWhileStatement(CSubsetParser.DoWhileStatementContext ctx) {
        boolean isTrue;
        visit(ctx.block());
        Object conditionResult = visit(ctx.expression());
        isTrue = forceBoolean(conditionResult);
        while (isTrue) {
            visit(ctx.block());
            conditionResult = visit(ctx.expression());
            isTrue = forceBoolean(conditionResult);
        }
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
    public Object visitIfStatement(CSubsetParser.IfStatementContext ctx) {
        Object conditionResult = visit(ctx.expression());
        boolean isTrue = forceBoolean(conditionResult);
        if (isTrue) {
            visit(ctx.block(0));
        } else if (ctx.ELSE() != null) {
            visit(ctx.block(1));
        }
        return null;
    }
    @Override
    public Object visitWhileStatement(CSubsetParser.WhileStatementContext ctx) {
        Object conditionResult = visit(ctx.expression());
        boolean isTrue = forceBoolean(conditionResult);
        while (isTrue) {
            visit(ctx.block());
            conditionResult = visit(ctx.expression());
            isTrue = forceBoolean(conditionResult);
        }
        return null;
    }
}