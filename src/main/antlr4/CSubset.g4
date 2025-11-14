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
simpleDeclaration: type ID (ASSIGN expression)?
    ;
assignment: simpleAssignment SEMI;
simpleAssignment: ID ASSIGN expression;
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
primaryExpr:
      INT
    | FLOAT
    | CHAR_LITERAL
    | ID
    | functionCall
    | LPAREN expression RPAREN
    ;

functionCall:
    ID LPAREN argList? RPAREN
    ;

argList:
    expression (COMMA expression)*
    ;

type: T_INT | T_FLOAT | T_CHAR;

T_INT: 'int';
T_FLOAT: 'float';
T_CHAR: 'char';
ASSIGN: '=';
LPAREN: '(';
RPAREN: ')';
LBRACE: '{';
RBRACE: '}'; // <-- ESTA É A LINHA CORRIGIDA
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