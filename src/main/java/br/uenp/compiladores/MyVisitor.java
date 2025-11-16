package br.uenp.compiladores;

// ... (imports existentes)
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

    // --- PONTO DE ENTRADA (Atualizado) ---

    @Override
    public Object visitProgram(CSubsetParser.ProgramContext ctx) {
        // 1º Passo: Registar todas as definições globais

        for (CSubsetParser.StructDefinitionContext structCtx : ctx.structDefinition()) {
            visit(structCtx);
        }

        // NOVO: Registar Unions
        for (CSubsetParser.UnionDefinitionContext unionCtx : ctx.unionDefinition()) {
            visit(unionCtx);
        }

        for (CSubsetParser.FunctionDeclarationContext funcCtx : ctx.functionDeclaration()) {
            visit(funcCtx);
        }

        // 2º Passo: Encontrar e executar 'main'
        FunctionSymbol mainFunction = symbolTable.resolveFunction("main");
        if (mainFunction == null) {
            throw new RuntimeException("Erro: Função 'main' não encontrada.");
        }
        try {
            executeFunction(mainFunction, new ArrayList<>());
        } catch (ReturnException re) {
            // Ignorar
        }
        return null;
    }

    // --- MÉTODOS DE STRUCT/UNION (Atualizados) ---

    @Override
    public Object visitStructDefinition(CSubsetParser.StructDefinitionContext ctx) {
        String structName = ctx.ID().getText();
        System.out.println("SEMÂNTICA: Registando struct '" + structName + "'");

        StructDefinition def = new StructDefinition();
        for (CSubsetParser.StructMemberContext memberCtx : ctx.structMember()) {
            String type = memberCtx.type().getText();
            String name = memberCtx.ID().getText();
            def.addMember(name, type);
        }

        symbolTable.addStructDefinition(structName, def);
        return null;
    }

    // NOVO MÉTODO
    @Override
    public Object visitUnionDefinition(CSubsetParser.UnionDefinitionContext ctx) {
        String unionName = ctx.ID().getText();
        System.out.println("SEMÂNTICA: Registando union '" + unionName + "'");

        // Reutilizamos a StructDefinition para o "plano"
        StructDefinition def = new StructDefinition();
        for (CSubsetParser.StructMemberContext memberCtx : ctx.structMember()) {
            String type = memberCtx.type().getText();
            String name = memberCtx.ID().getText();
            def.addMember(name, type);
        }

        symbolTable.addUnionDefinition(unionName, def);
        return null;
    }

    @Override
    public Object visitMemberAccess(CSubsetParser.MemberAccessContext ctx) {
        String instanceName = ctx.ID(0).getText();
        String memberName = ctx.ID(1).getText();

        try {
            Object obj = symbolTable.resolve(instanceName);

            // ATUALIZADO: Verifica se é Struct OU Union
            if (obj instanceof StructInstance) {
                StructInstance instance = (StructInstance) obj;
                System.out.println("INTERPRETADOR: Lendo " + instanceName + "." + memberName);
                return instance.read(memberName);
            } else if (obj instanceof UnionInstance) {
                UnionInstance instance = (UnionInstance) obj;
                System.out.println("INTERPRETADOR: Lendo " + instanceName + "." + memberName);
                return instance.read(memberName);
            } else {
                throw new RuntimeException("Erro: Tentando aceder a membro '" + memberName + "' de algo que não é uma struct ou union.");
            }

        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    @Override
    public Object visitSimpleAssignment(CSubsetParser.SimpleAssignmentContext ctx) {
        Object value = visit(ctx.expression());

        if (ctx.ID() != null) {
            String varName = ctx.ID().getText();
            System.out.println("INTERPRETADOR: Atribuindo " + value + " para '" + varName + "'");
            try {
                symbolTable.assign(varName, value);
            } catch (RuntimeException e) {
                System.err.println(e.getMessage());
            }
        }
        else if (ctx.arrayAccess() != null) {
            // ... (lógica do array, sem mudanças)
            String varName = ctx.arrayAccess().ID().getText();
            try {
                Object arrayObj = symbolTable.resolve(varName);
                if (!(arrayObj instanceof Object[])) {
                    throw new RuntimeException("Erro: Tentando indexar uma variável ('" + varName + "') que não é um array.");
                }
                Object[] array = (Object[]) arrayObj;
                Object indexObj = visit(ctx.arrayAccess().expression());
                if (!(indexObj instanceof Integer)) {
                    throw new RuntimeException("Erro: Índice do array deve ser um inteiro.");
                }
                int index = (Integer) indexObj;
                System.out.println("INTERPRETADOR: Atribuindo " + value + " para '" + varName + "[" + index + "]'");
                array[index] = value;
            } catch (RuntimeException e) {
                System.err.println(e.getMessage());
            }
        }
        else if (ctx.memberAccess() != null) {
            String instanceName = ctx.memberAccess().ID(0).getText();
            String memberName = ctx.memberAccess().ID(1).getText();

            try {
                Object obj = symbolTable.resolve(instanceName);

                // ATUALIZADO: Verifica se é Struct OU Union
                if (obj instanceof StructInstance) {
                    StructInstance instance = (StructInstance) obj;
                    System.out.println("INTERPRETADOR: Atribuindo " + value + " para " + instanceName + "." + memberName);
                    instance.write(memberName, value);
                } else if (obj instanceof UnionInstance) {
                    UnionInstance instance = (UnionInstance) obj;
                    System.out.println("INTERPRETADOR: Atribuindo " + value + " para " + instanceName + "." + memberName);
                    instance.write(memberName, value);
                } else {
                    throw new RuntimeException("Erro: Tentando aceder a membro '" + memberName + "' de algo que não é uma struct ou union.");
                }

            } catch (RuntimeException e) {
                System.err.println(e.getMessage());
            }
        }
        return null;
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
        if (ctx.ID() != null) {
            try {
                // 'resolve' agora pode retornar int, float, array, StructInstance ou UnionInstance
                return symbolTable.resolve(ctx.ID().getText());
            } catch (RuntimeException e) {
                System.err.println(e.getMessage());
                return null;
            }
        }
        if (ctx.functionCall() != null) {
            return visit(ctx.functionCall());
        }
        if (ctx.arrayAccess() != null) {
            return visit(ctx.arrayAccess());
        }
        if (ctx.memberAccess() != null) {
            return visit(ctx.memberAccess());
        }
        if (ctx.expression() != null) {
            return visit(ctx.expression());
        }
        return null;
    }

    // --- MÉTODOS RESTANTES (Sem mudanças) ---

    @Override
    public Object visitSimpleDeclaration(CSubsetParser.SimpleDeclarationContext ctx) {
        String varType = ctx.type().getText(); // Agora pode ser 'structPonto' ou 'unionValor'
        String varName = ctx.ID().getText();
        System.out.println("SEMÂNTICA: Declarando variável '" + varName + "' do tipo '" + varType + "'");
        try {
            // 'add' agora trata a criação de StructInstance e UnionInstance
            symbolTable.add(varName, varType);

            if (ctx.LBRACKET() != null) {
                int size = Integer.parseInt(ctx.INT().getText());
                Object[] array = new Object[size];
                System.out.println("INTERPRETADOR: Alocando array '" + varName + "' com tamanho " + size);
                symbolTable.assign(varName, array);
            } else if (ctx.ASSIGN() != null) {
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
    public Object visitUnaryExpr(CSubsetParser.UnaryExprContext ctx) {
        Object value = visit(ctx.relExpr());
        int notCount = ctx.NOT().size();
        if (notCount == 0) {
            return value;
        }
        boolean boolValue = forceBoolean(value);
        if (notCount % 2 != 0) {
            return !boolValue;
        } else {
            return boolValue;
        }
    }
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
        if (ctx.unaryExpr().size() < 2) {
            return visit(ctx.unaryExpr(0));
        }
        Object left = visit(ctx.unaryExpr(0));
        boolean leftBool = forceBoolean(left);
        if (!leftBool) {
            return false;
        }
        for (int i = 1; i < ctx.unaryExpr().size(); i++) {
            Object right = visit(ctx.unaryExpr(i));
            boolean rightBool = forceBoolean(right);
            if (!rightBool) {
                return false;
            }
        }
        return true;
    }
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