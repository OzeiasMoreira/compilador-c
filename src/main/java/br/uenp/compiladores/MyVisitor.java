package br.uenp.compiladores;

import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MyVisitor extends CSubsetBaseVisitor<Object> {

    private SymbolTable symbolTable = new SymbolTable();
    private Scanner inputScanner = new Scanner(System.in);
    private FunctionSymbol currentFunction = null;

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

    //
    // --- NOVO MÉTODO ADICIONADO (Para corrigir o seu erro de 'imprimir(123)') ---
    //
    @Override
    public Object visitFunctionCallStatement(CSubsetParser.FunctionCallStatementContext ctx) {
        // Simplesmente visita a chamada de função e ignora o seu retorno
        visit(ctx.functionCall());
        return null;
    }

    // --- MÉTODOS DE FUNÇÃO (Atualizados) ---

    @Override
    public Object visitIncludeDirective(CSubsetParser.IncludeDirectiveContext ctx) {
        System.out.println("PRÉ-PROCESSADOR: Ignorando '" + ctx.getText() + "'");
        return null;
    }

    private Object executeFunction(FunctionSymbol function, List<Object> args) {
        if (function.getParameters().size() != args.size()) {
            throw new RuntimeException("Erro: Número incorreto de argumentos para a função.");
        }
        this.currentFunction = function;
        symbolTable.enterScope();
        try {
            for (int i = 0; i < args.size(); i++) {
                String paramType = function.getParameters().get(i).getKey();
                String paramName = function.getParameters().get(i).getValue();
                Object paramValue = args.get(i);
                symbolTable.add(paramName, paramType);
                symbolTable.assign(paramName, paramValue);
            }
            visit(function.getBody());
        } finally {
            symbolTable.exitScope();
            this.currentFunction = null;
        }
        // CORRIGIDO: Agora usa os métodos 'getName' e 'getType'
        if (!function.getType().equals("void")) {
            throw new RuntimeException("Erro: Função não-void '" + function.getName() + "' chegou ao fim sem 'return'.");
        }
        return null;
    }

    @Override
    public Object visitReturnStatement(CSubsetParser.ReturnStatementContext ctx) {
        if (currentFunction == null) {
            throw new RuntimeException("Erro: 'return' encontrado fora de uma função.");
        }
        // CORRIGIDO: Agora usa o método 'getType'
        String funcType = currentFunction.getType();

        if (ctx.expression() == null) {
            if (!funcType.equals("void")) {
                throw new RuntimeException("Erro: Função não-void ("+ funcType +") deve retornar um valor.");
            }
            throw new ReturnException(null);
        }
        else {
            if (funcType.equals("void")) {
                throw new RuntimeException("Erro: Função 'void' não pode retornar um valor.");
            }
            Object returnValue = visit(ctx.expression());
            throw new ReturnException(returnValue);
        }
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

        // CORRIGIDO: Passa 'funcName' para o novo construtor
        FunctionSymbol func = new FunctionSymbol(funcName, funcType, params, ctx.block());
        symbolTable.addFunction(funcName, func);

        System.out.println("SEMÂNTICA: Registando função '" + funcName + "'");
        return null;
    }

    // --- MÉTODOS RESTANTES (Sem mudanças) ---
    // (Apenas colados para garantir que o ficheiro está completo)

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
                }
            }
        }
        formatString = formatString.replace("\\n", "\n");
        System.out.print(formatString);
        System.out.flush();
        return null;
    }
    @Override
    public Object visitPutsStatement(CSubsetParser.PutsStatementContext ctx) {
        String str = ctx.STRING_LITERAL().getText();
        str = str.substring(1, str.length() - 1);
        str = str.replace("\\n", "\n");
        System.out.println(str);
        return null;
    }
    @Override
    public Object visitDefineDirective(CSubsetParser.DefineDirectiveContext ctx) {
        String name = ctx.ID().getText();
        Object value;
        if (ctx.INT() != null) {
            value = Integer.parseInt(ctx.INT().getText());
        } else if (ctx.FLOAT() != null) {
            value = Double.parseDouble(ctx.FLOAT().getText());
        } else {
            throw new RuntimeException("#define não suportado para este valor.");
        }
        System.out.println("PRÉ-PROCESSADOR: Definindo '" + name + "' como " + value);
        symbolTable.addDefine(name, value);
        return null;
    }
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
    @Override
    public Object visitUnionDefinition(CSubsetParser.UnionDefinitionContext ctx) {
        String unionName = ctx.ID().getText();
        System.out.println("SEMÂNTICA: Registando union '" + unionName + "'");
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
    public Object visitSimpleDeclaration(CSubsetParser.SimpleDeclarationContext ctx) {
        String varType = ctx.type().getText();
        String varName = ctx.ID().getText();
        System.out.println("SEMÂNTICA: Declarando variável '" + varName + "' do tipo '" + varType + "'");
        try {
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
    public Object visitSimpleAssignment(CSubsetParser.SimpleAssignmentContext ctx) {
        Object rhsValue = visit(ctx.expression());
        CSubsetParser.LvalueContext lvalue = ctx.lvalue();
        try {
            if (lvalue.ID() != null) {
                String varName = lvalue.ID().getText();
                System.out.println("INTERPRETADOR: Atribuindo " + rhsValue + " para '" + varName + "'");
                symbolTable.assign(varName, rhsValue);
            }
            else if (lvalue.arrayAccess() != null) {
                String varName = lvalue.arrayAccess().ID().getText();
                Object arrayObj = symbolTable.resolve(varName);
                if (!(arrayObj instanceof Object[])) {
                    throw new RuntimeException("Erro: Tentando indexar uma variável ('" + varName + "') que não é um array.");
                }
                Object[] array = (Object[]) arrayObj;
                Object indexObj = visit(lvalue.arrayAccess().expression());
                if (!(indexObj instanceof Integer)) {
                    throw new RuntimeException("Erro: Índice do array deve ser um inteiro.");
                }
                int index = (Integer) indexObj;
                System.out.println("INTERPRETADOR: Atribuindo " + rhsValue + " para '" + varName + "[" + index + "]'");
                array[index] = rhsValue;
            }
            else if (lvalue.memberAccess() != null) {
                String instanceName = lvalue.memberAccess().ID(0).getText();
                String memberName = lvalue.memberAccess().ID(1).getText();
                Object obj = symbolTable.resolve(instanceName);
                if (obj instanceof StructInstance) {
                    StructInstance instance = (StructInstance) obj;
                    System.out.println("INTERPRETADOR: Atribuindo " + rhsValue + " para " + instanceName + "." + memberName);
                    instance.write(memberName, rhsValue);
                } else if (obj instanceof UnionInstance) {
                    UnionInstance instance = (UnionInstance) obj;
                    System.out.println("INTERPRETADOR: Atribuindo " + rhsValue + " para " + instanceName + "." + memberName);
                    instance.write(memberName, rhsValue);
                } else {
                    throw new RuntimeException("Erro: Tentando aceder a membro '" + memberName + "' de algo que não é uma struct ou union.");
                }
            }
            else if (lvalue.unaryExpr() != null) {
                Object resolvedName = visit(lvalue.unaryExpr());
                if (!(resolvedName instanceof String)) {
                    throw new RuntimeException("Erro: Tentativa de desreferência (escrita) em algo que não é um ponteiro.");
                }
                String varName = (String) resolvedName;
                System.out.println("INTERPRETADOR: Atribuindo (via ponteiro) " + rhsValue + " para '" + varName + "'");
                symbolTable.assign(varName, rhsValue);
            }
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }
    @Override
    public Object visitArrayAccess(CSubsetParser.ArrayAccessContext ctx) {
        String varName = ctx.ID().getText();
        try {
            Object arrayObj = symbolTable.resolve(varName);
            if (!(arrayObj instanceof Object[])) {
                throw new RuntimeException("Erro: Tentando indexar uma variável ('" + varName + "') que não é um array.");
            }
            Object[] array = (Object[]) arrayObj;
            Object indexObj = visit(ctx.expression());
            if (!(indexObj instanceof Integer)) {
                throw new RuntimeException("Erro: Índice do array deve ser um inteiro.");
            }
            int index = (Integer) indexObj;
            System.out.println("INTERPRETADOR: Lendo valor de '" + varName + "[" + index + "]'");
            Object value = array[index];
            if (value == null) {
                throw new RuntimeException("Erro: Lendo índice de array não inicializado.");
            }
            return value;
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            return null;
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
        if (ctx.ID() != null) {
            String name = ctx.ID().getText();
            if (symbolTable.isDefine(name)) {
                return symbolTable.resolveDefine(name);
            }
            try {
                return symbolTable.resolve(name);
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
    @Override
    public Object visitUnaryExpr(CSubsetParser.UnaryExprContext ctx) {
        if (ctx.NOT() != null) {
            Object value = visit(ctx.unaryExpr());
            boolean boolValue = forceBoolean(value);
            return !boolValue;
        }
        else if (ctx.AMPERSAND() != null) {
            String varName = ctx.unaryExpr().getText();
            System.out.println("INTERPRETADOR: Obtendo endereço de '" + varName + "'");
            return varName;
        }
        else if (ctx.STAR() != null) {
            Object ptrValue = visit(ctx.unaryExpr());
            if (!(ptrValue instanceof String)) {
                throw new RuntimeException("Erro: Tentativa de desreferência (leitura) em algo que não é um ponteiro.");
            }
            String varName = (String) ptrValue;
            System.out.println("INTERPRETADOR: Lendo (via ponteiro) o valor de '" + varName + "'");
            return symbolTable.resolve(varName);
        }
        else {
            return visit(ctx.primaryExpr());
        }
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
                case ">=": return leftVal >= rightVal;
                case "<": return leftVal < rightVal;
                case "<=": return leftVal <= rightVal;
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
                case ">=": return leftChar >= rightChar;
                case "<": return leftChar < rightChar;
                case "<=": return leftChar <= rightChar;
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
        Object left = visit(ctx.unaryExpr(0));
        for (int i = 1; i < ctx.unaryExpr().size(); i++) {
            Object right = visit(ctx.unaryExpr(i));
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