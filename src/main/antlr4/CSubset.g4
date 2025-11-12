grammar CSubset;

@header {
package br.uenp.compiladores;
}

program: functionDeclaration+;

functionDeclaration: type ID LPAREN RPAREN LBRACE statement* RBRACE;

statement:
    declaration
    | assignment
    | block
    | ifStatement
    | printfStatement
    | whileStatement
    | forStatement
    ;

ifStatement:
    IF LPAREN expression RPAREN block (ELSE block)?
    ;

printfStatement:
    PRINTF LPAREN STRING_LITERAL (COMMA expression)? RPAREN SEMI
    ;

whileStatement:
    WHILE LPAREN expression RPAREN block
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
simpleDeclaration: type ID;

assignment: simpleAssignment SEMI;
simpleAssignment: ID ASSIGN expression;

expression: relExpr;

relExpr:
    addExpr ( (GT | LT | EQ | NEQ) addExpr )*
    ;

addExpr:
    multExpr ( (PLUS | MINUS) multExpr )*
    ;

multExpr:
    primaryExpr ( (MULT | DIV) primaryExpr )*
    ;

primaryExpr:
      INT
    | FLOAT       // <-- ADICIONADO
    | ID
    | LPAREN expression RPAREN
    ;

type: T_INT | T_FLOAT; // <-- ATUALIZADO

T_INT: 'int';
T_FLOAT: 'float'; // <-- ADICIONADO
ASSIGN: '=';
LPAREN: '(';
RPAREN: ')';
LBRACE: '{';
RBRACE: '}';
SEMI: ';';

// Tokens Aritméticos
PLUS: '+';
MINUS: '-';
MULT: '*';
DIV: '/';

// Tokens de Controle e Relacionais
IF: 'if';
ELSE: 'else';
WHILE: 'while';
FOR: 'for';
EQ: '==';
NEQ: '!=';
GT: '>';
LT: '<';

// Tokens do Printf
PRINTF: 'printf';
COMMA: ',';

ID: [a-zA-Z_] [a-zA-Z_0-9]*;
INT: [0-9]+;
FLOAT: [0-9]+ '.' [0-9]+; // <-- ADICIONADO
STRING_LITERAL: '"' ( '\\' . | ~('\\'|'"') )* '"';

WS: [ \t\r\n]+ -> skip;
LINE_COMMENT: '//' .*? '\n' -> skip;
BLOCK_COMMENT: '/*' .*? '*/' -> skip;