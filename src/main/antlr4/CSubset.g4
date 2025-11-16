grammar CSubset;

@header {
package br.uenp.compiladores;
}

// Definições de topo agora podem ser structs ou funções
program: (structDefinition | functionDeclaration)+;

functionDeclaration:
    type ID LPAREN paramList? RPAREN block
    ;
paramList:
    param (COMMA param)*
    ;
param:
    type ID
    ;

//
// --- NOVAS REGRAS PARA STRUCT ---
//
// Define uma struct (ex: struct Ponto { int x; };)
structDefinition:
    STRUCT ID LBRACE structMember+ RBRACE SEMI
    ;

structMember:
    type ID SEMI
    ;

// Acesso a membro (ex: p1.x)
memberAccess:
    ID DOT ID
    ;
// --- FIM DAS NOVAS REGRAS ---

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

// ... (regras 'return' até 'for' sem mudanças) ...
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

// Declaração de variável (sem mudanças, já suporta 'type ID')
declaration: simpleDeclaration SEMI;
simpleDeclaration:
    type ID ( (LBRACKET INT RBRACKET) | (ASSIGN expression) )?
    ;

// Atribuição agora suporta p1.x = 10
assignment: simpleAssignment SEMI;
simpleAssignment:
    (ID | arrayAccess | memberAccess) ASSIGN expression // <-- ATUALIZADO
    ;

expression: logicalOrExpr;
logicalOrExpr:
    logicalAndExpr (OR logicalAndExpr)*
    ;
logicalAndExpr:
    unaryExpr (AND unaryExpr)*
    ;
unaryExpr:
    (NOT)* relExpr
    ;
relExpr:
    addExpr ( (GT | LT | EQ | NEQ) addExpr )*
    ;
addExpr:
    multExpr ( (PLUS | MINUS) multExpr )*
    ;
multExpr:
    primaryExpr ( (MULT | DIV) primaryExpr )*
    ;

// Leitura de p1.x adicionada
primaryExpr:
      INT
    | FLOAT
    | CHAR_LITERAL
    | ID
    | functionCall
    | arrayAccess
    | memberAccess // <-- ADICIONADO
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

//
// --- REGRA 'type' ATUALIZADA ---
//
type:
    (T_INT | T_FLOAT | T_CHAR) // Tipos primitivos
    | structType               // Ou um tipo struct
    ;

structType: // ex: struct Ponto
    STRUCT ID
    ;

// --- TOKENS ---
T_INT: 'int';
T_FLOAT: 'float';
T_CHAR: 'char';
STRUCT: 'struct'; // <-- ADICIONADO
ASSIGN: '=';
LPAREN: '(';
RPAREN: ')';
LBRACE: '{';
RBRACE: '}';
SEMI: ';';
LBRACKET: '[';
RBRACKET: ']';
DOT: '.'; // <-- ADICIONADO

// ... (restante dos tokens sem mudanças) ...
PLUS: '+';
MINUS: '-';
MULT: '*';
DIV: '/';
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

ID: [a-zA-Z_] [a-zA-Z_0-9]*;
INT: [0-9]+;
FLOAT: [0-9]+ '.' [0-9]+;
CHAR_LITERAL: '\'' . '\'';
STRING_LITERAL: '"' ( '\\' . | ~('\\'|'"') )* '"';

WS: [ \t\r\n]+ -> skip;
LINE_COMMENT: '//' .*? '\n' -> skip;
BLOCK_COMMENT: '/*' .*? '*/' -> skip;