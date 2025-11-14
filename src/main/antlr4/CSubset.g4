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
    | scanfStatement
    | doWhileStatement
    | switchStatement // <-- ADICIONADO
    ;

ifStatement:
    IF LPAREN expression RPAREN block (ELSE block)?
    ;

printfStatement:
    PRINTF LPAREN STRING_LITERAL (COMMA expression)? RPAREN SEMI
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

// Nova regra para o switch
switchStatement:
    SWITCH LPAREN expression RPAREN LBRACE caseBlock* defaultBlock? RBRACE
    ;

// Bloco 'case', simplificado para INT
caseBlock:
    CASE INT COLON statement* (BREAK SEMI)?
    ;

// Bloco 'default'
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
    | FLOAT
    | CHAR_LITERAL
    | ID
    | LPAREN expression RPAREN
    ;

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
SWITCH: 'switch'; // <-- ADICIONADO
CASE: 'case';     // <-- ADICIONADO
DEFAULT: 'default'; // <-- ADICIONADO
BREAK: 'break';   // <-- ADICIONADO
EQ: '==';
NEQ: '!=';
GT: '>';
LT: '<';

// Tokens do Printf e Scanf
PRINTF: 'printf';
SCANF: 'scanf';
COMMA: ',';
AMPERSAND: '&';
COLON: ':';     // <-- ADICIONADO

ID: [a-zA-Z_] [a-zA-Z_0-9]*;
INT: [0-9]+;
FLOAT: [0-9]+ '.' [0-9]+;
CHAR_LITERAL: '\'' . '\'';
STRING_LITERAL: '"' ( '\\' . | ~('\\'|'"') )* '"';

WS: [ \t\r\n]+ -> skip;
LINE_COMMENT: '//' .*? '\n' -> skip;
BLOCK_COMMENT: '/*' .*? '*/' -> skip;