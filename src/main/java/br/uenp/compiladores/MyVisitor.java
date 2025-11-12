package br.uenp.compiladores;

public class MyVisitor extends CSubsetBaseVisitor<Object> {

    private SymbolTable symbolTable = new SymbolTable();

    // --- NOVA FUNÇÃO AUXILIAR ---
    // Converte um objeto para Number, se possível
    private Number promoteToNumber(Object obj) {
        if (obj instanceof Number) {
            return (Number) obj;
        }
        throw new RuntimeException("Type error: expected a number but got " + obj);
    }

    // --- MÉTODOS EXISTENTES (ATUALIZADOS) ---

    @Override
    public Object visitSimpleDeclaration(CSubsetParser.SimpleDeclarationContext ctx) {
        String varType = ctx.type().getText(); // Agora pode ser 'int' ou 'float'
        String varName = ctx.ID().getText();

        System.out.println("SEMANTIC: Declaring variable '" + varName + "' of type '" + varType + "'");

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

        // TODO: Adicionar checagem de tipo (ex: não permitir 'int x = 5.5;')

        System.out.println("INTERPRETER: Assigning " + value + " to '" + varName + "'");

        try {
            symbolTable.assign(varName, value);
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

            // ATUALIZADO: Checa por %d (Integer) ou %f (Double)
            if (formatString.contains("%d") && value instanceof Integer) {
                formatString = formatString.replaceFirst("%d", value.toString());
            } else if (formatString.contains("%f") && value instanceof Double) {
                formatString = formatString.replaceFirst("%f", value.toString());
            } else if (formatString.contains("%f") && value instanceof Integer) {
                // Permite imprimir int com %f (ex: 10 -> 10.000000)
                formatString = formatString.replaceFirst("%f", value.toString());
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

        // Promove ambos para Number
        Number leftNum = promoteToNumber(left);
        Number rightNum = promoteToNumber(right);

        // Promove para Double para fazer a comparação
        double leftVal = leftNum.doubleValue();
        double rightVal = rightNum.doubleValue();

        switch (op) {
            case ">": return leftVal > rightVal;
            case "<": return leftVal < rightVal;
            case "==": return leftVal == rightVal;
            case "!=": return leftVal != rightVal;
            default:
                throw new RuntimeException("Unknown relational operator: " + op);
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

            // Promoção de Tipo: Se algum for Double, o resultado é Double
            if (leftNum instanceof Double || rightNum instanceof Double) {
                double leftVal = leftNum.doubleValue();
                double rightVal = rightNum.doubleValue();
                switch (op) {
                    case "+": left = leftVal + rightVal; break;
                    case "-": left = leftVal - rightVal; break;
                }
            } else { // Ambos são Integer
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

            // Promoção de Tipo
            if (leftNum instanceof Double || rightNum instanceof Double) {
                double leftVal = leftNum.doubleValue();
                double rightVal = rightNum.doubleValue();
                switch (op) {
                    case "*": left = leftVal * rightVal; break;
                    case "/":
                        if (rightVal == 0.0) throw new RuntimeException("Error: Division by zero.");
                        left = leftVal / rightVal;
                        break;
                }
            } else { // Ambos são Integer
                int leftVal = leftNum.intValue();
                int rightVal = rightNum.intValue();
                switch (op) {
                    case "*": left = leftVal * rightVal; break;
                    case "/":
                        if (rightVal == 0) throw new RuntimeException("Error: Division by zero.");
                        left = leftVal / rightVal;
                        break;
                }
            }
        }

        return left;
    }

    @Override
    public Object visitPrimaryExpr(CSubsetParser.PrimaryExprContext ctx) {
        if (ctx.INT() != null) {
            return Integer.parseInt(ctx.INT().getText());
        }
        if (ctx.FLOAT() != null) { // <-- ADICIONADO
            return Double.parseDouble(ctx.FLOAT().getText());
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

    // --- MÉTODOS ANTIGOS (SEM MUDANÇAS) ---

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
        // ... (código do forStatement... sem mudanças)
        if (ctx.init != null) {
            visit(ctx.init);
        }
        boolean isTrue = true;
        if (ctx.cond != null) {
            Object conditionResult = visit(ctx.cond);
            if (!(conditionResult instanceof Boolean)) {
                throw new RuntimeException("Error: 'for' loop condition must evaluate to a boolean.");
            }
            isTrue = (Boolean) conditionResult;
        }
        while (isTrue) {
            visit(ctx.block());
            if (ctx.inc != null) {
                visit(ctx.inc);
            }
            if (ctx.cond != null) {
                Object conditionResult = visit(ctx.cond);
                isTrue = (Boolean) conditionResult;
            }
        }
        return null;
    }

    @Override
    public Object visitIfStatement(CSubsetParser.IfStatementContext ctx) {
        // ... (código do ifStatement... sem mudanças)
        Object conditionResult = visit(ctx.expression());
        if (!(conditionResult instanceof Boolean)) {
            throw new RuntimeException("Error: 'if' condition must evaluate to a boolean.");
        }
        boolean isTrue = (Boolean) conditionResult;
        if (isTrue) {
            visit(ctx.block(0));
        } else if (ctx.ELSE() != null) {
            visit(ctx.block(1));
        }
        return null;
    }

    @Override
    public Object visitWhileStatement(CSubsetParser.WhileStatementContext ctx) {
        // ... (código do whileStatement... sem mudanças)
        Object conditionResult = visit(ctx.expression());
        if (!(conditionResult instanceof Boolean)) {
            throw new RuntimeException("Error: 'while' condition must evaluate to a boolean.");
        }
        boolean isTrue = (Boolean) conditionResult;
        while (isTrue) {
            visit(ctx.block());
            conditionResult = visit(ctx.expression());
            isTrue = (Boolean) conditionResult;
        }
        return null;
    }
}