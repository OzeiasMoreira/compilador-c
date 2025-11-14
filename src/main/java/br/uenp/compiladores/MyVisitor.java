package br.uenp.compiladores;

import java.util.Scanner;

public class MyVisitor extends CSubsetBaseVisitor<Object> {

    private SymbolTable symbolTable = new SymbolTable();
    private Scanner inputScanner = new Scanner(System.in);

    // --- FUNÇÕES AUXILIARES ---

    private Number promoteToNumber(Object obj) {
        if (obj instanceof Number) {
            return (Number) obj;
        }
        throw new RuntimeException("Erro de tipo: esperado um número, mas recebido " + obj);
    }

    private boolean forceBoolean(Object obj) {
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        if (obj instanceof Integer) {
            return ((Integer) obj) != 0;
        }
        if (obj instanceof Double) {
            return ((Double) obj) != 0.0;
        }
        if (obj instanceof Character) {
            return ((Character) obj) != '\0';
        }
        throw new RuntimeException("Erro de tipo: não é possível avaliar a expressão como booleana.");
    }

    // --- MÉTODOS (&& e ||) CORRIGIDOS ---

    @Override
    public Object visitLogicalOrExpr(CSubsetParser.LogicalOrExprContext ctx) {
        // CORREÇÃO: Se não há 'OR' (só um logicalAndExpr), apenas passa o valor para cima.
        if (ctx.logicalAndExpr().size() < 2) {
            return visit(ctx.logicalAndExpr(0));
        }

        // Avalia o primeiro lado
        Object left = visit(ctx.logicalAndExpr(0));
        boolean leftBool = forceBoolean(left);

        // Curto-Circuito (OR)
        if (leftBool) {
            return true;
        }

        // Avalia os próximos
        for (int i = 1; i < ctx.logicalAndExpr().size(); i++) {
            Object right = visit(ctx.logicalAndExpr(i));
            boolean rightBool = forceBoolean(right);

            if (rightBool) {
                return true; // Encontrou um 'true'
            }
        }

        return false; // Ninguém foi 'true'
    }

    @Override
    public Object visitLogicalAndExpr(CSubsetParser.LogicalAndExprContext ctx) {
        // CORREÇÃO: Se não há 'AND' (só um relExpr), apenas passa o valor para cima.
        if (ctx.relExpr().size() < 2) {
            return visit(ctx.relExpr(0));
        }

        // Avalia o primeiro lado
        Object left = visit(ctx.relExpr(0));
        boolean leftBool = forceBoolean(left);

        // Curto-Circuito (AND)
        if (!leftBool) {
            return false;
        }

        // Avalia os próximos
        for (int i = 1; i < ctx.relExpr().size(); i++) {
            Object right = visit(ctx.relExpr(i));
            boolean rightBool = forceBoolean(right);

            if (!rightBool) {
                return false; // Encontrou um 'false'
            }
        }

        return true; // Ninguém foi 'false'
    }

    // --- MÉTODOS EXISTENTES (Traduções Aplicadas) ---

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
    public Object visitPrintfStatement(CSubsetParser.PrintfStatementContext ctx) {
        String formatString = ctx.STRING_LITERAL().getText();
        formatString = formatString.substring(1, formatString.length() - 1);

        if (ctx.expression() != null) {
            Object value = visit(ctx.expression());

            if (formatString.contains("%d") && value instanceof Integer) {
                formatString = formatString.replaceFirst("%d", value.toString());
            } else if (formatString.contains("%f") && value instanceof Double) {
                formatString = formatString.replaceFirst("%f", value.toString());
            } else if (formatString.contains("%f") && value instanceof Integer) {
                formatString = formatString.replaceFirst("%f", value.toString());
            } else if (formatString.contains("%c") && value instanceof Character) {
                formatString = formatString.replaceFirst("%c", value.toString());
            }
        }

        formatString = formatString.replace("\\n", "\n");
        System.out.print(formatString);

        return null;
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
                return symbolTable.resolve(ctx.ID().getText());
            } catch (RuntimeException e) {
                System.err.println(e.getMessage());
                return null;
            }
        }
        if (ctx.expression() != null) {
            return visit(ctx.expression());
        }
        return null;
    }

    @Override
    public Object visitSimpleDeclaration(CSubsetParser.SimpleDeclarationContext ctx) {
        String varType = ctx.type().getText();
        String varName = ctx.ID().getText();
        System.out.println("SEMÂNTICA: Declarando variável '" + varName + "' do tipo '" + varType + "'");
        try {
            symbolTable.add(varName, varType);
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    @Override
    public Object visitSimpleAssignment(CSubsetParser.SimpleAssignmentContext ctx) {
        String varName = ctx.ID().getText();
        Object value = visit(ctx.expression());
        System.out.println("INTERPRETADOR: Atribuindo " + value + " para '" + varName + "'");
        try {
            symbolTable.assign(varName, value);
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
        }
        return null;
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
    public Object visitDeclaration(CSubsetParser.DeclarationContext ctx) {
        return visit(ctx.simpleDeclaration());
    }

    @Override
    public Object visitAssignment(CSubsetParser.AssignmentContext ctx) {
        return visit(ctx.simpleAssignment());
    }

    @Override
    public Object visitForStatement(CSubsetParser.ForStatementContext ctx) {
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