grammar simpleLang;

@header{
    package main.grammar;
    import main.ast.*;
    import java.util.ArrayList;
}

// ==========================================
// 1. Parser Rules
// ==========================================

program returns [Program programRet]
    @init { $programRet = new Program(); }
    : (m=moduleDeclaration { $programRet.addDeclaration($m.moduleRet); }
      | s=structDeclaration { $programRet.addDeclaration($s.structRet); })* EOF
    ;

moduleDeclaration returns [ModuleDeclaration moduleRet]
    : MODULE ID (INCLUDES inc=ID)? BEGIN 
      { $moduleRet = new ModuleDeclaration($ID.text); 
        if($inc != null) $moduleRet.setIncludedModule($inc.text); }
      (m=member { $moduleRet.addMember($m.memberRet); })* END
    ;

structDeclaration returns [StructDeclaration structRet]
    : STRUCT ID BEGIN { $structRet = new StructDeclaration($ID.text); }
      (f=fieldDeclaration { $structRet.addField($f.fieldRet); })* END
    ;

member returns [Node memberRet]
    : accessModifier? (f=fieldDeclaration { $memberRet = $f.fieldRet; }
                     | m=methodDeclaration { $memberRet = $m.methodRet; }
                     | c=constructorDeclaration { $memberRet = $c.methodRet; })
    ;

accessModifier
    : PUBLIC | PRIVATE
    ;

fieldDeclaration returns [FieldDeclaration fieldRet]
    : (m=MUT)? t=type ID (ASSIGN e=expression)? SEMI
      { $fieldRet = new FieldDeclaration($t.text, $ID.text, $m != null); }
    ;

methodDeclaration returns [MethodDeclaration methodRet]
    : t=type? ID LPAR parameterList? RPAR BEGIN 
      { 
   
        String typeName = ($t.ctx != null) ? $t.text : "void";
        $methodRet = new MethodDeclaration($ID.text, typeName); 
      }
      (s=statement { $methodRet.addStatement($s.nodeRet); })* END
    ;

constructorDeclaration returns [MethodDeclaration methodRet]
    : ID LPAR parameterList? RPAR BEGIN 
      { $methodRet = new MethodDeclaration($ID.text, "void"); }
      (s=statement { $methodRet.addStatement($s.nodeRet); })* END
    ;

parameterList
    : parameter (COMMA parameter)*
    ;

parameter
    : MUT? type ID
    ;

statement returns [Node nodeRet]
    : v=varDeclarationStatement { $nodeRet = $v.nodeRet; }
    | a=assignmentStatement     { $nodeRet = $a.nodeRet; }
    | i=ifStatement             { $nodeRet = $i.nodeRet; }
    | w=whileStatement          { $nodeRet = $w.nodeRet; }
    | f=forStatement            { $nodeRet = $f.nodeRet; }
    | r=returnStatement         { $nodeRet = $r.nodeRet; }
    | o=outputStatement         { $nodeRet = $o.nodeRet; }
    | e=expression SEMI         { $nodeRet = $e.nodeRet; } 
    | b=breakStatement          { $nodeRet = $b.nodeRet; }
    | c=continueStatement       { $nodeRet = $c.nodeRet; }
    ;

varDeclarationStatement returns [Node nodeRet]
    : (m=MUT)? t=type (LPAR arg=argumentList? RPAR)? ID (ASSIGN e=expression)? SEMI
      { $nodeRet = new FieldDeclaration($t.text, $ID.text, $m != null); }
    ;

assignmentStatement returns [AssignmentStatement nodeRet]
    : (THIS DOT)? ID ASSIGN e=expression SEMI 
      { $nodeRet = new AssignmentStatement($ID.text, $e.nodeRet); }
    ;

outputStatement returns [OutputStatement nodeRet]
    : OUTPUT e=expression SEMI 
      { $nodeRet = new OutputStatement($e.nodeRet); }
    ;

returnStatement returns [Node nodeRet]
    : RETURN expression? SEMI 
    ;

breakStatement returns [Node nodeRet]
    : BREAK SEMI 
    ;

continueStatement returns [Node nodeRet]
    : CONTINUE SEMI 
    ;

ifStatement returns [IfStatement nodeRet]
    : IF LPAR cond=expression RPAR BEGIN { $nodeRet = new IfStatement($cond.nodeRet); }
      (s1=statement { $nodeRet.addThenStatement($s1.nodeRet); })* END
      (ELIF LPAR cond2=expression RPAR BEGIN (s2=statement)* END)*
      (ELSE BEGIN (s3=statement { $nodeRet.addElseStatement($s3.nodeRet); })* END)?
    ;

whileStatement returns [WhileStatement nodeRet]
    : WHILE LPAR cond=expression RPAR BEGIN { $nodeRet = new WhileStatement($cond.nodeRet); }
      (s=statement { $nodeRet.addStatement($s.nodeRet); })* END
    ;

forStatement returns [Node nodeRet]
    : FOR LPAR 
      (init1=assignmentStatement | init2=varDeclarationStatement)? 
      cond=expression SEMI 
      (step1=assignmentStatement)? 
      RPAR BEGIN (s=statement)* END
      { 
      }
    ;

expression returns [Node nodeRet]
    : e1=expression DOT ID (LPAR args=argumentList? RPAR)? 
      { $nodeRet = new MethodCall($e1.nodeRet, $ID.text); }
    | e1=expression op=(MUL | DIV | MOD) e2=expression 
      { $nodeRet = new BinaryExpression($e1.nodeRet, $e2.nodeRet, $op.text); }
    | e1=expression op=(PLUS | MINUS) e2=expression 
      { $nodeRet = new BinaryExpression($e1.nodeRet, $e2.nodeRet, $op.text); }
    | e1=expression op=(LT | GT | LE | GE | EE | NE) e2=expression 
      { $nodeRet = new BinaryExpression($e1.nodeRet, $e2.nodeRet, $op.text); }
    | p=primary { $nodeRet = $p.nodeRet; }
    ;

argumentList returns [ArrayList<Node> argsRet]
    @init { $argsRet = new ArrayList<>(); }
    : e1=expression { $argsRet.add($e1.nodeRet); } (COMMA e2=expression { $argsRet.add($e2.nodeRet); })*
    ;

primary returns [Node nodeRet]
    : ID (lp=LPAR args=argumentList? RPAR)?  
      { 
        if($lp != null) {
            $nodeRet = new MethodCall(null, $ID.text);
            
            if($args.argsRet != null) {
                for(Node n : $args.argsRet) ((MethodCall)$nodeRet).addArgument(n);
            }
        } 
        else $nodeRet = new Identifier($ID.text); 
      }
    | INT_VAL   { $nodeRet = new IntValue(Integer.parseInt($INT_VAL.text)); }
    | FLOAT_VAL { $nodeRet = new IntValue(0); }
    | BOOL_VAL  { $nodeRet = new BoolValue(Boolean.parseBoolean($BOOL_VAL.text)); }
    | CHAR_VAL  { $nodeRet = new Identifier($CHAR_VAL.text); }
    | LPAR e=expression RPAR { $nodeRet = $e.nodeRet; }
    | THIS      { $nodeRet = new Identifier("this"); }
    ;

type : INT | FLOAT | DOUBLE | CHAR | BOOL | VOID | ID ;

// ==========================================
// 2. Lexer Rules
// ==========================================

MODULE   : 'module';
STRUCT   : 'struct';
INCLUDES : 'includes';
BEGIN    : 'begin';
END      : 'end';
MUT      : 'mut';
OUTPUT   : 'output';
IF       : 'if';
ELIF     : 'elif';
ELSE     : 'else';
WHILE    : 'while';
FOR      : 'for';
BREAK    : 'break';
CONTINUE : 'continue';
RETURN   : 'return';
PUBLIC   : 'public';
PRIVATE  : 'private';
THIS     : 'this';

INT    : 'int';
FLOAT  : 'float';
DOUBLE : 'double';
CHAR   : 'char';
BOOL   : 'bool';
VOID   : 'void';

BOOL_VAL : 'true' | 'false';

SEMI   : ';';
COMMA  : ',';
DOT    : '.';
ASSIGN : '=';
PLUS   : '+';
MINUS  : '-';
MUL    : '*';
DIV    : '/';
MOD    : '%';
EE     : '==';
NE     : '!=';
LT     : '<';
GT     : '>';
LE     : '<=';
GE     : '>=';
LPAR   : '(';
RPAR   : ')';

ID      : [a-zA-Z_][a-zA-Z0-9_]*;
INT_VAL : [0-9]+;
FLOAT_VAL : [0-9]+ '.' [0-9]+;
CHAR_VAL  : '\'' . '\'';

WHITE_SPACE   : [ \t\r\n]+ -> skip;
LINE_COMMENT  : '//' ~[\r\n]* -> skip;
BLOCK_COMMENT : '/*' .*? '*/' -> skip;