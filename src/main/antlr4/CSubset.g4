grammar CSubset;

@header {
package br.uenp.compiladores;
}

program: (defineDirective | structDefinition | unionDefinition | functionDeclaration)+;

functionDeclaration:
    type ID LPAREN paramList? RPAREN block
    ;
paramList:
    param (COMMA param)*
    ;
param:
    type ID
    ;

defineDirective:
    HASH DEFINE ID (INT | FLOAT)
    ;
unionDefinition:
    UNION ID LBRACE structMember+ RBRACE SEMI
    ;
structDefinition:
    STRUCT ID LBRACE structMember+ RBRACE SEMI
    ;
structMember:
    type ID SEMI
    ;
memberAccess:
    ID DOT ID
    ;

statement:
    declaration
    | assignment
    | block
    | ifStatement
    | printfStatement
    | whileStatement
    | forStatement
    | scanfStatement
    | doWhileStatement
    | switchStatement
    | returnStatement
    ;

returnStatement:
    RETURN expression? SEMI
    ;
ifStatement:
    IF LPAREN expression RPAREN block (ELSE block)?
    ;
printfStatement:
    PRINTF LPAREN STRING_LITERAL (COMMA argList)? RPAREN SEMI
    ;
scanfStatement:
    SCANF LPAREN STRING_LITERAL COMMA AMPERSAND ID RPAREN SEMI
    ;
whileStatement:
    WHILE LPAREN expression RPAREN block
    ;
doWhileStatement:
    DO block WHILE LPAREN expression RPAREN SEMI
    ;
switchStatement:
    SWITCH LPAREN expression RPAREN LBRACE caseBlock* defaultBlock? RBRACE
    ;
caseBlock:
    CASE INT COLON statement* (BREAK SEMI)?
    ;
defaultBlock:
    DEFAULT COLON statement* (BREAK SEMI)?
    ;
forStatement:
    FOR LPAREN init=forInit?
    SEMI cond=expression?
    SEMI inc=simpleAssignment?
    RPAREN block
    ;
forInit:
    simpleDeclaration
    | simpleAssignment
    ;
block: LBRACE statement* RBRACE;
declaration: simpleDeclaration SEMI;
simpleDeclaration:
    type ID ( (LBRACKET INT RBRACKET) | (ASSIGN expression) )?
    ;
assignment: simpleAssignment SEMI;
simpleAssignment:
    lvalue ASSIGN expression
    ;
lvalue:
    ID
    | arrayAccess
    | memberAccess
    | (STAR unaryExpr)
    ;

//
// --- CADEIA DE EXPRESSÃO CORRIGIDA ---
//
expression: logicalOrExpr;

logicalOrExpr:
    logicalAndExpr (OR logicalAndExpr)*
    ;

logicalAndExpr:
    relExpr (AND relExpr)* // CORRIGIDO: Deve chamar 'relExpr'
    ;

relExpr:
    addExpr ( (GT | LT | EQ | NEQ) addExpr )*
    ;

addExpr:
    multExpr ( (PLUS | MINUS) multExpr )*
    ;

multExpr:
    unaryExpr ( (STAR | DIV) unaryExpr )* // CORRIGIDO: Deve chamar 'unaryExpr'
    ;

unaryExpr: // CORRIGIDO: 'primaryExpr' é a última opção
    (NOT | AMPERSAND) unaryExpr
    | STAR unaryExpr
    | primaryExpr
    ;
// --- FIM DA CORREÇÃO ---

primaryExpr:
      INT
    | FLOAT
    | CHAR_LITERAL
    | ID
    | functionCall
    | arrayAccess
    | memberAccess
    | LPAREN expression RPAREN
    ;
functionCall:
    ID LPAREN argList? RPAREN
    ;
arrayAccess:
    ID LBRACKET expression RBRACKET
    ;
argList:
    expression (COMMA expression)*
    ;

type:
    (T_INT | T_FLOAT | T_CHAR) (STAR)?
    | structType
    | unionType
    ;
structType:
    STRUCT ID
    ;
unionType:
    UNION ID
    ;

// --- TOKENS ---
T_INT: 'int';
T_FLOAT: 'float';
T_CHAR: 'char';
STRUCT: 'struct';
UNION: 'union';
ASSIGN: '=';
LPAREN: '(';
RPAREN: ')';
LBRACE: '{';
RBRACE: '}';
SEMI: ';';
LBRACKET: '[';
RBRACKET: ']';
DOT: '.';
PLUS: '+';
MINUS: '-';
DIV: '/';
STAR: '*';
IF: 'if';
ELSE: 'else';
WHILE: 'while';
FOR: 'for';
DO: 'do';
SWITCH: 'switch';
CASE: 'case';
DEFAULT: 'default';
BREAK: 'break';
RETURN: 'return';
EQ: '==';
NEQ: '!=';
GT: '>';
LT: '<';
AND: '&&';
OR: '||';
NOT: '!';
PRINTF: 'printf';
SCANF: 'scanf';
COMMA: ',';
AMPERSAND: '&';
COLON: ':';
HASH: '#';
DEFINE: 'define';

ID: [a-zA-Z_] [a-zA-Z_0-9]*;
INT: [0-9]+;
FLOAT: [0-9]+ '.' [0-9]+;
CHAR_LITERAL: '\'' . '\'';
STRING_LITERAL: '"' ( '\\' . | ~('\\'|'"') )* '"';

WS: [ \t\r\n]+ -> skip;
LINE_COMMENT: '//' .*? '\n' -> skip;
BLOCK_COMMENT: '/*' .*? '*/' -> skip;