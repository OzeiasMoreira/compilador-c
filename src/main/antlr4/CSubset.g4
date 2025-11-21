grammar CSubset;

@header {
package br.uenp.compiladores;
}

program: (defineDirective | includeDirective | structDefinition | unionDefinition | functionDeclaration)+;

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
includeDirective:
    HASH INCLUDE INCLUDE_HEADER
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
    | scanfStatement
    | putsStatement
    | whileStatement
    | forStatement
    | doWhileStatement
    | switchStatement
    | returnStatement
    | functionCallStatement // <-- Necessário para 'funcao();'
    ;

returnStatement:
    RETURN expression? SEMI
    ;

// Permite que uma chamada de função seja uma instrução
functionCallStatement:
    functionCall SEMI
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
putsStatement:
    PUTS LPAREN STRING_LITERAL RPAREN SEMI
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
    relExpr (AND relExpr)*
    ;

relExpr:
    addExpr ( (GT | GTE | LT | LTE | EQ | NEQ) addExpr )*
    ;

addExpr:
    multExpr ( (PLUS | MINUS) multExpr )*
    ;

multExpr:
    unaryExpr ( (STAR | DIV) unaryExpr )*
    ;

unaryExpr:
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
    (T_INT | T_FLOAT | T_CHAR | T_VOID) (STAR)?
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
T_VOID: 'void';
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
GTE: '>=';
LT: '<';
LTE: '<=';
AND: '&&';
OR: '||';
NOT: '!';
PRINTF: 'printf';
SCANF: 'scanf';
PUTS: 'puts';
COMMA: ',';
AMPERSAND: '&';
COLON: ':';
HASH: '#';
DEFINE: 'define';
INCLUDE: 'include';

ID: [a-zA-Z_] [a-zA-Z_0-9]*;
INT: [0-9]+;
FLOAT: [0-9]+ '.' [0-9]+;
CHAR_LITERAL: '\'' . '\'';
STRING_LITERAL: '"' ( '\\' . | ~('\\'|'"') )* '"';
INCLUDE_HEADER: '<' [a-zA-Z_0-9]+ ('.' 'h')? '>';

WS: [ \t\r\n]+ -> skip;
LINE_COMMENT: '//' .*? '\n' -> skip;
BLOCK_COMMENT: '/*' .*? '*/' -> skip;