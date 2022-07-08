# SimpleLOOP Compiler
This is a compiler for a new Object Oriented language called SimpleLOOP.

### Phases:
* Phase1 -> Lexical Analyzer and Parser: SimpleLOOP.g4

* Phase2:
    * Making AST Tree In Grammer
    * AST Tree Nodes
    * Name Analyzing
    * Printing Some Errors Realted To Name And Scoping
    * Printing AST Tree If There Was No Error

* Phase3:
    * Type checking
    * Checking base rules such as `Main` class conditions
    * Checking accessibilities

* Phase4:
    * Code generation using `Jasmin`
