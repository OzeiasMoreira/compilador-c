grammar CSubset;

@header {
package br.uenp.compiladores;
}

program: functionDeclaration+;

functionDeclaration:
    type ID LPAREN paramList? RPAREN block
    ;

paramList:
    param (COMMA param)*
    ;

param:
    type ID
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

// ... (regras 'return', 'if', 'printf', 'scanf', 'while', 'do-while', 'switch' sem mudanças) ...
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

//
// --- ATUALIZAÇÕES PARA O ARRAY ---
//

// Declaração pode ser 'int x', 'int x = 10', ou 'int arr[5]'
// Mas não 'int arr[5] = ...' (por enquanto)
simpleDeclaration:
    type ID ( (LBRACKET INT RBRACKET) | (ASSIGN expression) )?
    ;

// Atribuição pode ser 'x = 10' ou 'arr[0] = 10'
assignment: simpleAssignment SEMI;
simpleAssignment:
    (ID | arrayAccess) ASSIGN expression // <-- ATUALIZADO
    ;

expression: logicalOrExpr;
logicalOrExpr:
    logicalAndExpr (OR logicalAndExpr)*
    ;
logicalAndExpr:
    relExpr (AND relExpr)*
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

// 'primaryExpr' pode ser ler 'x' ou ler 'arr[0]'
primaryExpr:
      INT
    | FLOAT
    | CHAR_LITERAL
    | ID
    | functionCall
    | arrayAccess // <-- ADICIONADO
    | LPAREN expression RPAREN
    ;

functionCall:
    ID LPAREN argList? RPAREN
    ;

// Nova regra para acesso a array (ex: arr[i])
arrayAccess:
    ID LBRACKET expression RBRACKET
    ;

argList:
    expression (COMMA expression)*
    ;
// --- FIM DAS ATUALIZAÇÕES DO ARRAY ---

type: T_INT | T_FLOAT | T_CHAR;

T_INT: 'int';
T_FLOAT: 'float';
T_CHAR: 'char';
ASSIGN: '=';
LPAREN: '(';
RPAREN: ')';
LBRACE: '{';
RBRACE: '}';
SEMI: ';';
LBRACKET: '['; // <-- ADICIONADO
RBRACKET: ']'; // <-- ADICIONADO

// Tokens Aritméticos
PLUS: '+';
MINUS: '-';
MULT: '*';
DIV: '/';

// Tokens de Controlo e Relacionais
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

// Tokens do Printf e Scanf
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