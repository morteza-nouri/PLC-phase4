package main.visitor.codeGenerator;

import main.ast.nodes.Program;
import main.ast.nodes.declaration.classDec.ClassDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.ConstructorDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.FieldDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.MethodDeclaration;
import main.ast.nodes.declaration.variableDec.VariableDeclaration;
import main.ast.nodes.expression.*;
import main.ast.nodes.expression.operators.BinaryOperator;
import main.ast.nodes.expression.operators.UnaryOperator;
import main.ast.nodes.expression.values.NullValue;
import main.ast.nodes.expression.values.primitive.BoolValue;
import main.ast.nodes.expression.values.primitive.IntValue;
import main.ast.nodes.statement.*;
import main.ast.nodes.statement.EachStmt;
import main.ast.types.NullType;
import main.ast.types.Type;
import main.ast.types.array.ArrayType;
import main.ast.types.functionPointer.FptrType;
import main.ast.types.primitives.BoolType;
import main.ast.types.primitives.ClassType;
import main.ast.types.primitives.IntType;
import main.ast.types.primitives.VoidType;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.ClassSymbolTableItem;
import main.symbolTable.items.FieldSymbolTableItem;
import main.symbolTable.utils.graph.Graph;
import main.util.ArgPair;
import main.visitor.Visitor;
import main.visitor.typeChecker.ExpressionTypeChecker;

import java.io.*;
import java.util.ArrayList;

public class CodeGenerator extends Visitor<String> {
    ExpressionTypeChecker expressionTypeChecker;
    Graph<String> classHierarchy;
    private String outputPath;
    private FileWriter currentFile;
    private ClassDeclaration currentClass;
    private MethodDeclaration currentMethod;

    private int labelCount;

    public CodeGenerator(Graph<String> classHierarchy) {
        this.classHierarchy = classHierarchy;
        this.expressionTypeChecker = new ExpressionTypeChecker(classHierarchy);
        this.prepareOutputFolder();

        this.labelCount = 0;
    }

    private void prepareOutputFolder() {
        this.outputPath = "output/";
        String jasminPath = "utilities/jarFiles/jasmin.jar";
        String arrayClassPath = "utilities/codeGenerationUtilityClasses/Array.j";
        String fptrClassPath = "utilities/codeGenerationUtilityClasses/Fptr.j";
        try{
            File directory = new File(this.outputPath);
            File[] files = directory.listFiles();
            if(files != null)
                for (File file : files)
                    file.delete();
            directory.mkdir();
        }
        catch(SecurityException e) {
            e.printStackTrace();
        }
        copyFile(jasminPath, this.outputPath + "jasmin.jar");
        copyFile(arrayClassPath, this.outputPath + "Array.j");
        copyFile(fptrClassPath, this.outputPath + "Fptr.j");
    }

    private void copyFile(String toBeCopied, String toBePasted) {
        try {
            File readingFile = new File(toBeCopied);
            File writingFile = new File(toBePasted);
            InputStream readingFileStream = new FileInputStream(readingFile);
            OutputStream writingFileStream = new FileOutputStream(writingFile);
            byte[] buffer = new byte[1024];
            int readLength;
            while ((readLength = readingFileStream.read(buffer)) > 0)
                writingFileStream.write(buffer, 0, readLength);
            readingFileStream.close();
            writingFileStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createFile(String name) {
        try {
            String path = this.outputPath + name + ".j";
            File file = new File(path);
            file.createNewFile();
            FileWriter fileWriter = new FileWriter(path);
            this.currentFile = fileWriter;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addCommand(String command) {
        try {
            command = String.join("\n\t\t", command.split("\n"));
            if(command.startsWith("Label_"))
                this.currentFile.write("\t" + command + "\n");
            else if(command.startsWith("."))
                this.currentFile.write(command + "\n");
            else
                this.currentFile.write("\t\t" + command + "\n");
            this.currentFile.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addStaticMainMethod() {
        addCommand(".method public static main([Ljava/lang/String;)V");
        addCommand(".limit stack 128");
        addCommand(".limit locals 128");
        addCommand("new Main");
        addCommand("invokespecial Main/<init>()V");
        addCommand("return");
        addCommand(".end method");
    }

    @Override
    public String visit(Program program) {
        //todo
        //generate new class for global variables
        //using .field, add global variables as static fields to the class

        for(ClassDeclaration classDeclaration : program.getClasses()) {
            this.expressionTypeChecker.setCurrentClass(classDeclaration);
            this.currentClass = classDeclaration;
            classDeclaration.accept(this);
        }
        return null;
    }

    @Override
    public String visit(ClassDeclaration classDeclaration) {
        String name = classDeclaration.getClassName().getName();
        createFile(name);
        //todo: done

        this.currentClass = classDeclaration;

        addCommand(".class "+name);

        if (classDeclaration.getParentClassName() == null)
        {
            addCommand(".super java/lang/Object");
        }
        else
        {
            String class_name_parent = classDeclaration.getParentClassName().getName();
            addCommand(".super " + class_name_parent);
        }

        for(FieldDeclaration field : classDeclaration.getFields())
        {
            field.accept(this);
        }

        if(classDeclaration.getConstructor() != null)
        {
            this.currentMethod = classDeclaration.getConstructor();
            this.expressionTypeChecker.setCurrentMethod(classDeclaration.getConstructor());
            classDeclaration.getConstructor().accept(this);
        }
        else
            defaultConstructorDecl();

        for(MethodDeclaration m : classDeclaration.getMethods())
        {
            this.currentMethod = m;
            this.expressionTypeChecker.setCurrentMethod(m);
            m.accept(this);
        }

        return null;
    }

    // Not Done
    private void defaultConstructorDecl() {
        String className = currentClass.getClassName().getName();

        addCommand(".method public <init>()V");
        addCommand(".limit stack 128");
        addCommand(".limit locals 128");
        addCommand("aload 0");

        if (currentClass.getParentClassName() == null){
            addCommand("invokespecial java/lang/Object/<init>()V");
        }
        else{
            String parentClassName = currentClass.getParentClassName().getName();
            addCommand("invokespecial " + parentClassName + "/<init>()V");
        }


//        for(FieldDeclaration field : currentClass.getFields()){
//            String fieldName = field.getVarDeclaration().getVarName().getName();
//            Type fieldType = field.getVarDeclaration().getType();
//
//            if(fieldType instanceof ClassType || fieldType instanceof FptrType){
//                addCommand("aload 0");
//                addCommand("aconst_null");
//                addCommand("putfield " + className + "/" + fieldName + " L" + makeTypeFlag(fieldType) + ";\n");
//            }
//            else if(fieldType instanceof IntType || fieldType instanceof BoolType){
//                addCommand("aload 0");
//                addCommand("ldc 0");
//                if(fieldType instanceof IntType)
//                    addCommand("invokestatic java/lang/Integer/valueOf(I)Ljava/lang/Integer;");
//                if(fieldType instanceof BoolType)
//                    addCommand("invokestatic java/lang/Boolean/valueOf(Z)Ljava/lang/Boolean;");
//                addCommand("putfield " + className + "/" + fieldName + " L" + makeTypeFlag(fieldType) + ";\n");
//            }
//            else if(fieldType instanceof StringType){
//                addCommand("aload 0");
//                addCommand("ldc \"\"");
//                addCommand("putfield " + className + "/" + fieldName + " L" + makeTypeFlag(fieldType) + ";\n");
//            }
//            else{
//                addCommand("aload 0");
//                initializeList((ListType) fieldType);
//                addCommand("putfield " + className + "/" + fieldName + " L" + makeTypeFlag(fieldType) + ";\n");
//            }
//        }
        addCommand("return");
        addCommand(".end method");
    }

    // Not done yet
    private String makeTypeFlag(Type t)
    {
        if (t instanceof IntType)
            return "java/lang/Integer";
        if (t instanceof BoolType)
            return "java/lang/Boolean";
        if (t instanceof ArrayType)
            return "Array";
        if (t instanceof FptrType)
            return "Fptr";
        if (t instanceof ClassType)
            return ((ClassType)t).getClassName().getName();
        return null;
    }

    @Override
    public String visit(ConstructorDeclaration constructorDeclaration) {
        //todo: done
        if (constructorDeclaration.getArgs().size() > 0)
            defaultConstructorDecl();

        String class_name = this.currentClass.getClassName().getName();

        if (class_name.equals("Main"))
            addStaticMainMethod();

        this.visit((MethodDeclaration) constructorDeclaration);

        return null;
    }

    @Override
    public String visit(MethodDeclaration methodDeclaration) {
        //todo

        return null;
    }

    @Override
    public String visit(FieldDeclaration fieldDeclaration) {
        //todo: done
        String field_name = fieldDeclaration.getVarDeclaration().getVarName().getName();
        Type feild_type = fieldDeclaration.getVarDeclaration().getType();
        String flag = makeTypeFlag(feild_type);
        addCommand(".field " + field_name + " L" + flag + ";");
        return null;
    }

    @Override
    public String visit(VariableDeclaration variableDeclaration) {
        //todo
        return null;
    }

    @Override
    public String visit(AssignmentStmt assignmentStmt) {
        //todo: done
        BinaryExpression assign_expression = new BinaryExpression(assignmentStmt.getlValue(), assignmentStmt.getrValue(), BinaryOperator.assign);
        addCommand(assign_expression.accept(this));
        addCommand("pop");
        return null;
    }

    @Override
    public String visit(BlockStmt blockStmt) {
        //todo: done
        for (Statement stmt: blockStmt.getStatements())
            stmt.accept(this);
        return null;
    }

    private String getNewLabel(){
        String l = "Label_";
        l += this.labelCount;
        this.labelCount += 1;
        return l;
    }

    @Override
    public String visit(ConditionalStmt conditionalStmt) {
        //todo: done
        String false_label = getNewLabel();
        String after = getNewLabel();
        addCommand(conditionalStmt.getCondition().accept(this));
        addCommand("ifeq " + false_label);
        conditionalStmt.getThenBody().accept(this);
        addCommand("goto " + after);
        addCommand(false_label + ":");
        if (conditionalStmt.getElseBody() != null)
            conditionalStmt.getElseBody().accept(this);
        addCommand(after + ":");
        return null;
    }

    @Override
    public String visit(ElsifStmt elsifStmt) {
        //todo
        return null;
    }

    @Override
    public String visit(MethodCallStmt methodCallStmt) {
        //todo: done
        this.expressionTypeChecker.setIsInMethodCallStmt(true);
        addCommand(methodCallStmt.getMethodCall().accept(this));
        addCommand("pop");
        this.expressionTypeChecker.setIsInMethodCallStmt(false);
        return null;
    }

    @Override
    public String visit(PrintStmt print) {
        //todo: done
        addCommand("getstatic java/lang/System/out Ljava/io/PrintStream;");
        Type argument_type = print.getArg().accept(expressionTypeChecker);
        addCommand(print.getArg().accept(this));
        if (argument_type instanceof IntType)
            addCommand("invokevirtual java/io/PrintStream/print(I)V");
        if (argument_type instanceof BoolType)
            addCommand("invokevirtual java/io/PrintStream/print(Z)V");
        return null;
    }

    @Override
    public String visit(ReturnStmt returnStmt) {
        //todo: done
        Type return_type = returnStmt.getReturnedExpr().accept(expressionTypeChecker);
        if(return_type instanceof NullType) {
            addCommand("return");
            return null;
        }

        addCommand( returnStmt.getReturnedExpr().accept(this) );
        if(return_type instanceof IntType)
            addCommand("invokestatic java/lang/Integer/valueOf(I)Ljava/lang/Integer;");
        if(return_type instanceof BoolType)
            addCommand("invokestatic java/lang/Boolean/valueOf(Z)Ljava/lang/Boolean;");
        addCommand("areturn");
        return null;
    }

    @Override
    public String visit(EachStmt eachStmt) {
        //todo
        return null;
    }

    @Override
    public String visit(TernaryExpression ternaryExpression) {
        //todo
        return null;
    }

    @Override
    public String visit(RangeExpression rangeExpression) {
        //todo
        return null;
    }

    @Override
    public String visit(BinaryExpression binaryExpression) {
        //todo
        return null;
    }

    @Override
    public String visit(UnaryExpression unaryExpression) {
        //todo
        return null;
    }

    @Override
    public String visit(ObjectMemberAccess objectMemberAccess) {
        //todo
        return null;
    }

    @Override
    public String visit(Identifier identifier) {
        //todo
        return null;
    }

    @Override
    public String visit(ArrayAccessByIndex arrayAccessByIndex) {
        //todo
        return null;
    }

    @Override
    public String visit(MethodCall methodCall) {
        //todo
        return null;
    }

    @Override
    public String visit(NewClassInstance newClassInstance) {
        //todo
        return null;
    }

    @Override
    public String visit(SelfClass selfClass) {
        //todo
        return null;
    }

    @Override
    public String visit(NullValue nullValue) {
        //todo
        return null;
    }

    @Override
    public String visit(IntValue intValue) {
        //todo
        return null;
    }

    @Override
    public String visit(BoolValue boolValue) {
        //todo
        return null;
    }

}
