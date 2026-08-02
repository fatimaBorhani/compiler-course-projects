package main.visitor;

import main.ast.core.Program;
import main.ast.declarations.Module;
import main.ast.declarations.Member;
import main.ast.declarations.Method;
import main.ast.declarations.MethodDecl;
import main.ast.declarations.ModuleDecl;
import main.ast.declarations.Parameter;
import main.ast.declarations.Struct;
import main.ast.declarations.StructDecl;
import main.ast.declarations.TopLevelDecl;
import main.ast.declarations.Var;
import main.ast.declarations.VarDecl;
import main.ast.expressions.BinaryExpression;
import main.ast.expressions.ConstantExpression;
import main.ast.expressions.ConstructorCall;
import main.ast.expressions.Expression;
import main.ast.expressions.Location;
import main.ast.expressions.MemberLoc;
import main.ast.expressions.MethodCall;
import main.ast.expressions.MethodCallLoc;
import main.ast.expressions.ParanthesisExpr;
import main.ast.expressions.SimpleLoc;
import main.ast.expressions.ThisLoc;
import main.ast.expressions.UnaryExpression;
import main.ast.expressions.UnaryOpExpr;
import main.ast.expressions.literals.BoolLiteral;
import main.ast.expressions.literals.CharLiteral;
import main.ast.expressions.literals.DoubleLiteral;
import main.ast.expressions.literals.FloatLiteral;
import main.ast.expressions.literals.IntLiteral;
import main.ast.expressions.operators.BinaryOperator;
import main.ast.expressions.operators.UnaryOperator;
import main.ast.statements.AssignStmt;
import main.ast.statements.Block;
import main.ast.statements.BreakJump;
import main.ast.statements.ContinueJump;
import main.ast.statements.ForStmt;
import main.ast.statements.IfStmt;
import main.ast.statements.InputStmt;
import main.ast.statements.MethodCallStmt;
import main.ast.statements.OutputStmt;
import main.ast.statements.ReturnStmt;
import main.ast.statements.Statement;
import main.ast.statements.VarDeclStmt;
import main.ast.statements.WhileStmt;
import main.ast.types.Identifier;
import main.ast.types.PrimitiveType;
import main.ast.types.Type;
import main.ast.types.UserDefinedType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Phase 3 - Code Generation.
 *
 * For every module / struct of the input program one jasmin file (.j) is
 * emitted inside ./codeGenOutput/ . The generated files can be assembled with
 *      java -jar jasmin.jar codeGenOutput/*.j
 * and executed with
 *      java Main
 *
 * Only the Visitor pattern is used (no ANTLR listener) and AST node kinds are
 * discriminated exclusively with `instanceof` (no enum based node dispatch).
 */
public class CodeGenerator extends Visitor<String> {

    /* ------------------------------------------------------------------ */
    /*  output                                                             */
    /* ------------------------------------------------------------------ */

    private final String outputPath;
    private StringBuilder code;               // buffer of the class being generated

    /* ------------------------------------------------------------------ */
    /*  global program indexes (built once, in visit(Program))             */
    /* ------------------------------------------------------------------ */

    /** class name -> its members (methods + fields) */
    private final Map<String, List<Member>> memberIndex = new LinkedHashMap<>();
    /** class name -> super class name (first `includes`), or null */
    private final Map<String, String> superIndex = new LinkedHashMap<>();
    /** class name -> the `includes` that could not be mapped to inheritance */
    private final Map<String, List<String>> extraIncludes = new LinkedHashMap<>();

    /* ------------------------------------------------------------------ */
    /*  per class / per method state                                       */
    /* ------------------------------------------------------------------ */

    private String currentClass;
    private Type currentReturnType;

    /** local variable name -> slot number (slot 0 is always `this`) */
    private final Map<String, Integer> slots = new HashMap<>();
    /** local variable name -> jvm descriptor */
    private final Map<String, String> localTypes = new HashMap<>();
    private int nextSlot;

    private int labelCounter;
    private final Deque<String> breakLabels = new ArrayDeque<>();
    private final Deque<String> continueLabels = new ArrayDeque<>();

    public CodeGenerator() {
        this.outputPath = "./codeGenOutput/";
        prepareOutputDirectory();
    }

    private void prepareOutputDirectory() {
        File dir = new File(outputPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /* ==================================================================== */
    /*  Program                                                             */
    /* ==================================================================== */

    @Override
    public String visit(Program program) {
        buildIndexes(program);

        for (String className : orderedClasses()) {
            TopLevelDecl decl = declOf(program, className);
            if (decl != null) {
                decl.accept(this);
            }
        }
        return null;
    }

    private TopLevelDecl declOf(Program program, String name) {
        for (TopLevelDecl decl : program.getTopLevelDeclarations()) {
            if (decl instanceof ModuleDecl) {
                if (((ModuleDecl) decl).getModule().getName().getName().equals(name)) {
                    return decl;
                }
            } else if (decl instanceof StructDecl) {
                if (((StructDecl) decl).getStruct().getName().getName().equals(name)) {
                    return decl;
                }
            }
        }
        return null;
    }

    /** collects, for every module/struct, its members and its super class. */
    private void buildIndexes(Program program) {
        for (TopLevelDecl decl : program.getTopLevelDeclarations()) {
            if (decl instanceof ModuleDecl) {
                Module module = ((ModuleDecl) decl).getModule();
                String name = module.getName().getName();
                memberIndex.put(name, module.getMembers());
                List<Identifier> includes = module.getIncludes();
                if (includes != null && !includes.isEmpty()) {
                    superIndex.put(name, includes.get(0).getName());
                    List<String> rest = new ArrayList<>();
                    for (int i = 1; i < includes.size(); i++) {
                        rest.add(includes.get(i).getName());
                    }
                    extraIncludes.put(name, rest);
                } else {
                    superIndex.put(name, null);
                }
            } else if (decl instanceof StructDecl) {
                Struct struct = ((StructDecl) decl).getStruct();
                String name = struct.getName().getName();
                memberIndex.put(name, struct.getMembers());
                superIndex.put(name, null);
            }
        }
    }

    /** included modules are emitted before the modules that include them. */
    private List<String> orderedClasses() {
        List<String> ordered = new ArrayList<>();
        Set<String> done = new LinkedHashSet<>();
        for (String name : memberIndex.keySet()) {
            addOrdered(name, ordered, done, new LinkedHashSet<String>());
        }
        return ordered;
    }

    private void addOrdered(String name, List<String> ordered, Set<String> done, Set<String> visiting) {
        if (done.contains(name) || visiting.contains(name)) {
            return;
        }
        visiting.add(name);
        String parent = superIndex.get(name);
        if (parent != null && memberIndex.containsKey(parent)) {
            addOrdered(parent, ordered, done, visiting);
        }
        visiting.remove(name);
        if (!done.contains(name)) {
            done.add(name);
            ordered.add(name);
        }
    }

    /* ==================================================================== */
    /*  Declarations                                                        */
    /* ==================================================================== */

    @Override
    public String visit(ModuleDecl moduleDecl) {
        return moduleDecl.getModule().accept(this);
    }

    @Override
    public String visit(StructDecl structDecl) {
        return structDecl.getStruct().accept(this);
    }

    @Override
    public String visit(Module module) {
        String name = module.getName().getName();
        generateClass(name, superIndex.get(name), module.getMembers());
        return null;
    }

    @Override
    public String visit(Struct struct) {
        generateClass(struct.getName().getName(), null, struct.getMembers());
        return null;
    }

    private void generateClass(String name, String parent, List<Member> members) {
        currentClass = name;
        code = new StringBuilder();

        String superName = (parent == null) ? "java/lang/Object" : parent;

        line(".class public " + name);
        line(".super " + superName);
        List<String> rest = extraIncludes.get(name);
        if (rest != null && !rest.isEmpty()) {
            for (String other : rest) {
                line("; note: `includes " + other + "` is not expressible with JVM single inheritance");
            }
        }
        line("");

        /* fields ------------------------------------------------------- */
        boolean hasField = false;
        for (Member member : members) {
            if (member instanceof VarDecl) {
                Var var = ((VarDecl) member).getVar();
                line(".field public " + var.getName().getName() + " " + descriptorOf(typeOfVar(var)));
                hasField = true;
            }
        }
        if (hasField) {
            line("");
        }

        /* constructor -------------------------------------------------- */
        generateConstructor(superName);

        /* methods ------------------------------------------------------ */
        Method entryPoint = null;
        for (Member member : members) {
            if (member instanceof MethodDecl) {
                Method method = ((MethodDecl) member).getMethod();
                method.accept(this);
                if (method.getName().getName().equals("main") && method.getParameters().isEmpty()) {
                    entryPoint = method;
                }
            }
        }

        /* jvm entry point ---------------------------------------------- */
        if (entryPoint != null) {
            generateStaticMain(name, entryPoint);
        }

        writeClassFile(name);
        currentClass = null;
    }

    private void generateConstructor(String superName) {
        line(".method public <init>()V");
        line("    .limit stack 128");
        line("    .limit locals 128");
        emit("aload_0");
        emit("invokespecial " + superName + "/<init>()V");
        emit("return");
        line(".end method");
        line("");
    }

    private void generateStaticMain(String className, Method entryPoint) {
        String returnDescriptor = descriptorOf(entryPoint.getReturnType());
        line(".method public static main([Ljava/lang/String;)V");
        line("    .limit stack 128");
        line("    .limit locals 128");
        emit("new " + className);
        emit("dup");
        emit("invokespecial " + className + "/<init>()V");
        emit("invokevirtual " + className + "/main()" + returnDescriptor);
        if (!"V".equals(returnDescriptor)) {
            emit(popFor(returnDescriptor));
        }
        emit("return");
        line(".end method");
        line("");
    }

    private void writeClassFile(String name) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(outputPath + name + ".j");
            writer.write(code.toString());
        } catch (IOException e) {
            System.err.println("Cannot write jasmin file for " + name + " : " + e.getMessage());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /* ==================================================================== */
    /*  Method                                                              */
    /* ==================================================================== */

    @Override
    public String visit(MethodDecl methodDecl) {
        return methodDecl.getMethod().accept(this);
    }

    @Override
    public String visit(Method method) {
        /* --- fresh frame for this method ------------------------------ */
        slots.clear();
        localTypes.clear();
        nextSlot = 0;
        slotOf("this", "L" + currentClass + ";");   // slot 0 belongs to the class itself

        currentReturnType = method.getReturnType();

        StringBuilder signature = new StringBuilder("(");
        for (Parameter parameter : method.getParameters()) {
            String descriptor = descriptorOf(parameter.getType());
            signature.append(descriptor);
            slotOf(parameter.getName().getName(), descriptor);
        }
        signature.append(")").append(descriptorOf(method.getReturnType()));

        line(".method public " + method.getName().getName() + signature);
        line("    .limit stack 128");
        line("    .limit locals 128");

        if (method.getBody() != null) {
            method.getBody().accept(this);
        }

        /* a jasmin method must always end with a return instruction, and a
           label must never be the last line (that would be an illegal jump
           target), so the default return is appended unless the generated code
           already ends with a return. */
        if (!lastInstructionIsReturn()) {
            emitDefaultReturn(descriptorOf(method.getReturnType()));
        }

        line(".end method");
        line("");
        return null;
    }

    /** true when the last line already emitted is a return instruction. */
    private boolean lastInstructionIsReturn() {
        String text = code.toString();
        int end = text.length();
        while (end > 0 && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        if (end == 0) {
            return false;
        }
        int start = text.lastIndexOf('\n', end - 1) + 1;
        String last = text.substring(start, end).trim();
        return last.equals("return") || last.equals("ireturn") || last.equals("freturn")
                || last.equals("dreturn") || last.equals("areturn") || last.equals("lreturn");
    }

    private void emitDefaultReturn(String descriptor) {
        if ("V".equals(descriptor)) {
            emit("return");
        } else if ("D".equals(descriptor)) {
            emit("ldc2_w 0.0");
            emit("dreturn");
        } else if ("F".equals(descriptor)) {
            emit("ldc 0.0");
            emit("freturn");
        } else if (isIntLike(descriptor)) {
            emit("ldc 0");
            emit("ireturn");
        } else {
            emit("aconst_null");
            emit("areturn");
        }
    }

    @Override
    public String visit(Parameter parameter) {
        return descriptorOf(parameter.getType());
    }

    @Override
    public String visit(VarDecl varDecl) {
        return null;    // fields are emitted by generateClass
    }

    @Override
    public String visit(Var var) {
        return descriptorOf(typeOfVar(var));
    }

    /* ==================================================================== */
    /*  slots                                                               */
    /* ==================================================================== */

    /**
     * Returns the slot of `varName`. Slot 0 is reserved for the class itself
     * (`this`), then the arguments of the method follow in order. If the
     * variable does not exist yet it is appended to the slot table and its new
     * slot is returned. The table is emptied at the beginning of every method.
     */
    public int slotOf(String varName) {
        return slotOf(varName, localTypes.containsKey(varName) ? localTypes.get(varName) : "I");
    }

    private int slotOf(String varName, String descriptor) {
        Integer slot = slots.get(varName);
        if (slot == null) {
            slot = nextSlot;
            slots.put(varName, slot);
            localTypes.put(varName, descriptor);
            nextSlot += "D".equals(descriptor) ? 2 : 1;   // a double occupies two slots
        }
        return slot;
    }

    /* ==================================================================== */
    /*  Statements                                                          */
    /* ==================================================================== */

    @Override
    public String visit(Block block) {
        for (Statement statement : block.getStatements()) {
            statement.accept(this);
        }
        return null;
    }

    @Override
    public String visit(VarDeclStmt varDeclStmt) {
        Var var = varDeclStmt.getVar();
        String name = var.getName().getName();
        String descriptor = descriptorOf(typeOfVar(var));
        int slot = slotOf(name, descriptor);

        Expression initial = varDeclStmt.getInitial();
        if (initial == null && var.getConstructorCall() != null) {
            initial = var.getConstructorCall();
        }
        if (initial != null) {
            String actual = generateExpression(initial);
            convert(actual, descriptor);
            emit(storeInstruction(descriptor) + " " + slot);
        }
        return null;
    }

    @Override
    public String visit(AssignStmt assignStmt) {
        Location target = assignStmt.getLeft();
        String descriptor = prepareStore(target);
        String actual = generateExpression(assignStmt.getRight());
        convert(actual, descriptor);
        finishStore(target, descriptor);
        return null;
    }

    @Override
    public String visit(MethodCallStmt methodCallStmt) {
        String descriptor = generateExpression(methodCallStmt.getMethodCall());
        if (!"V".equals(descriptor)) {
            emit(popFor(descriptor));
        }
        return null;
    }

    @Override
    public String visit(ReturnStmt returnStmt) {
        String expected = descriptorOf(currentReturnType);
        if (returnStmt.getValue() == null) {
            emit("return");
            return null;
        }
        String actual = generateExpression(returnStmt.getValue());
        convert(actual, expected);
        if ("D".equals(expected)) {
            emit("dreturn");
        } else if ("F".equals(expected)) {
            emit("freturn");
        } else if (isIntLike(expected)) {
            emit("ireturn");
        } else if ("V".equals(expected)) {
            emit("return");
        } else {
            emit("areturn");
        }
        return null;
    }

    @Override
    public String visit(IfStmt ifStmt) {
        String elseLabel = newLabel("else");
        String endLabel = newLabel("endif");

        generateExpression(ifStmt.getCondition());
        emit("ifeq " + (ifStmt.getElseBranch() != null ? elseLabel : endLabel));

        if (ifStmt.getThenBranch() != null) {
            ifStmt.getThenBranch().accept(this);
        }

        if (ifStmt.getElseBranch() != null) {
            emit("goto " + endLabel);
            label(elseLabel);
            ifStmt.getElseBranch().accept(this);
        }
        label(endLabel);
        return null;
    }

    @Override
    public String visit(WhileStmt whileStmt) {
        String startLabel = newLabel("while");
        String endLabel = newLabel("endwhile");

        label(startLabel);
        generateExpression(whileStmt.getCondition());
        emit("ifeq " + endLabel);

        breakLabels.push(endLabel);
        continueLabels.push(startLabel);
        if (whileStmt.getBody() != null) {
            whileStmt.getBody().accept(this);
        }
        continueLabels.pop();
        breakLabels.pop();

        emit("goto " + startLabel);
        label(endLabel);
        return null;
    }

    @Override
    public String visit(ForStmt forStmt) {
        String condLabel = newLabel("for");
        String updateLabel = newLabel("forupdate");
        String endLabel = newLabel("endfor");

        for (Statement initializer : forStmt.getInitializers()) {
            initializer.accept(this);
        }

        label(condLabel);
        if (forStmt.getCondition() != null) {
            generateExpression(forStmt.getCondition());
            emit("ifeq " + endLabel);
        }

        breakLabels.push(endLabel);
        continueLabels.push(updateLabel);
        if (forStmt.getBody() != null) {
            forStmt.getBody().accept(this);
        }
        continueLabels.pop();
        breakLabels.pop();

        label(updateLabel);
        for (AssignStmt updater : forStmt.getUpdaters()) {
            updater.accept(this);
        }
        emit("goto " + condLabel);
        label(endLabel);
        return null;
    }

    @Override
    public String visit(BreakJump breakJump) {
        if (!breakLabels.isEmpty()) {
            emit("goto " + breakLabels.peek());
        }
        return null;
    }

    @Override
    public String visit(ContinueJump continueJump) {
        if (!continueLabels.isEmpty()) {
            emit("goto " + continueLabels.peek());
        }
        return null;
    }

    @Override
    public String visit(OutputStmt outputStmt) {
        emit("getstatic java/lang/System/out Ljava/io/PrintStream;");
        String descriptor = generateExpression(outputStmt.getValue());
        emit("invokevirtual java/io/PrintStream/println(" + printlnDescriptor(descriptor) + ")V");
        return null;
    }

    @Override
    public String visit(InputStmt inputStmt) {
        Location target = inputStmt.getLoc();
        String descriptor = prepareStore(target);

        emit("new java/util/Scanner");
        emit("dup");
        emit("getstatic java/lang/System/in Ljava/io/InputStream;");
        emit("invokespecial java/util/Scanner/<init>(Ljava/io/InputStream;)V");

        if ("D".equals(descriptor)) {
            emit("invokevirtual java/util/Scanner/nextDouble()D");
        } else if ("F".equals(descriptor)) {
            emit("invokevirtual java/util/Scanner/nextFloat()F");
        } else if ("Z".equals(descriptor)) {
            emit("invokevirtual java/util/Scanner/nextBoolean()Z");
        } else if ("C".equals(descriptor)) {
            emit("invokevirtual java/util/Scanner/next()Ljava/lang/String;");
            emit("ldc 0");
            emit("invokevirtual java/lang/String/charAt(I)C");
        } else {
            emit("invokevirtual java/util/Scanner/nextInt()I");
        }

        finishStore(target, descriptor);
        return null;
    }

    /* ==================================================================== */
    /*  Expressions                                                         */
    /* ==================================================================== */

    /** visits an expression and returns the descriptor of the value pushed. */
    private String generateExpression(Expression expression) {
        if (expression == null) {
            return "V";
        }
        String descriptor = expression.accept(this);
        return descriptor == null ? "I" : descriptor;
    }

    @Override
    public String visit(ConstantExpression constantExpression) {
        Object value = constantExpression.getValue();
        if (value instanceof Boolean) {
            emit("ldc " + (((Boolean) value).booleanValue() ? 1 : 0));
            return "Z";
        }
        if (value instanceof Character) {
            emit("ldc " + (int) ((Character) value).charValue());
            return "C";
        }
        if (value instanceof Double) {
            emit("ldc2_w " + value);
            return "D";
        }
        if (value instanceof Float) {
            emit("ldc " + value);
            return "F";
        }
        emit("ldc " + value);
        return "I";
    }

    @Override
    public String visit(IntLiteral intLiteral) {
        emit("ldc " + intLiteral.getValue());
        return "I";
    }

    @Override
    public String visit(FloatLiteral floatLiteral) {
        emit("ldc " + (float) floatLiteral.getValue());
        return "F";
    }

    @Override
    public String visit(DoubleLiteral doubleLiteral) {
        emit("ldc2_w " + doubleLiteral.getValue());
        return "D";
    }

    @Override
    public String visit(CharLiteral charLiteral) {
        emit("ldc " + (int) charLiteral.getValue());
        return "C";
    }

    @Override
    public String visit(BoolLiteral boolLiteral) {
        emit("ldc " + (boolLiteral.getValue() ? 1 : 0));
        return "Z";
    }

    @Override
    public String visit(ParanthesisExpr paranthesisExpr) {
        return generateExpression(paranthesisExpr.getExpression());
    }

    @Override
    public String visit(UnaryExpression unaryExpression) {
        UnaryOperator operator = unaryExpression.getOperand();
        String descriptor = generateExpression(unaryExpression.getExpression());
        if (operator == UnaryOperator.NOT) {
            emit("ldc 1");
            emit("ixor");
            return "Z";
        }
        emit(negateInstruction(descriptor));
        return descriptor;
    }

    @Override
    public String visit(UnaryOpExpr unaryOpExpr) {
        String descriptor = generateExpression(unaryOpExpr.getOperand());
        if (unaryOpExpr.getOperator() == UnaryOpExpr.Operator.NOT) {
            emit("ldc 1");
            emit("ixor");
            return "Z";
        }
        emit(negateInstruction(descriptor));
        return descriptor;
    }

    @Override
    public String visit(BinaryExpression binaryExpression) {
        BinaryOperator operator = binaryExpression.getOperator();
        Expression left = binaryExpression.getLeftOperand();
        Expression right = binaryExpression.getRightOperand();

        if (operator == BinaryOperator.AND) {
            return generateAnd(left, right);
        }
        if (operator == BinaryOperator.OR) {
            return generateOr(left, right);
        }

        String common = promote(inferType(left), inferType(right));

        String leftDescriptor = generateExpression(left);
        convert(leftDescriptor, common);
        String rightDescriptor = generateExpression(right);
        convert(rightDescriptor, common);

        if (isArithmetic(operator)) {
            emit(arithmeticInstruction(operator, common));
            return common;
        }
        generateComparison(operator, common);
        return "Z";
    }

    private String generateAnd(Expression left, Expression right) {
        String falseLabel = newLabel("andfalse");
        String endLabel = newLabel("andend");
        generateExpression(left);
        emit("ifeq " + falseLabel);
        generateExpression(right);
        emit("ifeq " + falseLabel);
        emit("ldc 1");
        emit("goto " + endLabel);
        label(falseLabel);
        emit("ldc 0");
        label(endLabel);
        return "Z";
    }

    private String generateOr(Expression left, Expression right) {
        String trueLabel = newLabel("ortrue");
        String endLabel = newLabel("orend");
        generateExpression(left);
        emit("ifne " + trueLabel);
        generateExpression(right);
        emit("ifne " + trueLabel);
        emit("ldc 0");
        emit("goto " + endLabel);
        label(trueLabel);
        emit("ldc 1");
        label(endLabel);
        return "Z";
    }

    private void generateComparison(BinaryOperator operator, String common) {
        String trueLabel = newLabel("cmptrue");
        String endLabel = newLabel("cmpend");

        if ("D".equals(common)) {
            emit("dcmpl");
            emit(branchOnZero(operator) + " " + trueLabel);
        } else if ("F".equals(common)) {
            emit("fcmpl");
            emit(branchOnZero(operator) + " " + trueLabel);
        } else if (isIntLike(common)) {
            emit(intCompareBranch(operator) + " " + trueLabel);
        } else {
            /* reference comparison */
            emit((operator == BinaryOperator.INEQUALITY ? "if_acmpne " : "if_acmpeq ") + trueLabel);
        }

        emit("ldc 0");
        emit("goto " + endLabel);
        label(trueLabel);
        emit("ldc 1");
        label(endLabel);
    }

    private String intCompareBranch(BinaryOperator operator) {
        if (operator == BinaryOperator.LESS_THAN) {
            return "if_icmplt";
        }
        if (operator == BinaryOperator.GREATER_THAN) {
            return "if_icmpgt";
        }
        if (operator == BinaryOperator.LESS_THAN_OR_EQUAL_TO) {
            return "if_icmple";
        }
        if (operator == BinaryOperator.GREATER_THAN_OR_EQUAL_TO) {
            return "if_icmpge";
        }
        if (operator == BinaryOperator.INEQUALITY) {
            return "if_icmpne";
        }
        return "if_icmpeq";
    }

    private String branchOnZero(BinaryOperator operator) {
        if (operator == BinaryOperator.LESS_THAN) {
            return "iflt";
        }
        if (operator == BinaryOperator.GREATER_THAN) {
            return "ifgt";
        }
        if (operator == BinaryOperator.LESS_THAN_OR_EQUAL_TO) {
            return "ifle";
        }
        if (operator == BinaryOperator.GREATER_THAN_OR_EQUAL_TO) {
            return "ifge";
        }
        if (operator == BinaryOperator.INEQUALITY) {
            return "ifne";
        }
        return "ifeq";
    }

    private boolean isArithmetic(BinaryOperator operator) {
        return operator == BinaryOperator.ADDITION
                || operator == BinaryOperator.SUBTRACTION
                || operator == BinaryOperator.MULTIPLICATION
                || operator == BinaryOperator.DIVISION;
    }

    private String arithmeticInstruction(BinaryOperator operator, String descriptor) {
        String prefix = "D".equals(descriptor) ? "d" : ("F".equals(descriptor) ? "f" : "i");
        if (operator == BinaryOperator.ADDITION) {
            return prefix + "add";
        }
        if (operator == BinaryOperator.SUBTRACTION) {
            return prefix + "sub";
        }
        if (operator == BinaryOperator.MULTIPLICATION) {
            return prefix + "mul";
        }
        return prefix + "div";
    }

    private String negateInstruction(String descriptor) {
        if ("D".equals(descriptor)) {
            return "dneg";
        }
        if ("F".equals(descriptor)) {
            return "fneg";
        }
        return "ineg";
    }

    /* ==================================================================== */
    /*  Locations                                                           */
    /* ==================================================================== */

    @Override
    public String visit(SimpleLoc simpleLoc) {
        return loadBase(simpleLoc.getId().getName());
    }

    @Override
    public String visit(ThisLoc thisLoc) {
        emit("aload_0");
        String descriptor = "L" + currentClass + ";";
        if (thisLoc.getLoc() != null) {
            descriptor = loadMemberChain(descriptor, thisLoc.getLoc());
        }
        return descriptor;
    }

    @Override
    public String visit(MemberLoc memberLoc) {
        /* the grammar builds  a.b  as MemberLoc(memberName = a, loc = b) */
        String baseDescriptor = loadBase(memberLoc.getMemberName().getName());
        return loadMemberChain(baseDescriptor, memberLoc.getLoc());
    }

    @Override
    public String visit(MethodCallLoc methodCallLoc) {
        return generateExpression(methodCallLoc.getMethodCall());
    }

    /** pushes the value of a plain identifier (local variable or field of `this`). */
    private String loadBase(String name) {
        if (slots.containsKey(name)) {
            String descriptor = localTypes.get(name);
            emit(loadInstruction(descriptor) + " " + slots.get(name));
            return descriptor;
        }
        String fieldDescriptor = findFieldDescriptor(currentClass, name);
        if (fieldDescriptor != null) {
            emit("aload_0");
            emit("getfield " + currentClass + "/" + name + " " + fieldDescriptor);
            return fieldDescriptor;
        }
        /* unknown name: phase 2 already reported it, emit a neutral value */
        emit("ldc 0");
        return "I";
    }

    /** given an object already on the stack, follows a `.field.field` chain. */
    private String loadMemberChain(String ownerDescriptor, Location location) {
        if (location instanceof SimpleLoc) {
            String owner = classOf(ownerDescriptor);
            String field = ((SimpleLoc) location).getId().getName();
            String descriptor = findFieldDescriptor(owner, field);
            if (descriptor == null) {
                descriptor = "I";
            }
            emit("getfield " + owner + "/" + field + " " + descriptor);
            return descriptor;
        }
        if (location instanceof MemberLoc) {
            MemberLoc member = (MemberLoc) location;
            String owner = classOf(ownerDescriptor);
            String field = member.getMemberName().getName();
            String descriptor = findFieldDescriptor(owner, field);
            if (descriptor == null) {
                descriptor = "I";
            }
            emit("getfield " + owner + "/" + field + " " + descriptor);
            return loadMemberChain(descriptor, member.getLoc());
        }
        if (location instanceof MethodCallLoc) {
            return generateExpression(((MethodCallLoc) location).getMethodCall());
        }
        return ownerDescriptor;
    }

    /* ------------------------- storing ---------------------------------- */

    /**
     * Emits whatever has to sit on the stack *below* the value (an object
     * reference, for a field assignment) and returns the descriptor of the
     * assignment target.
     */
    private String prepareStore(Location target) {
        if (target instanceof SimpleLoc) {
            String name = ((SimpleLoc) target).getId().getName();
            if (slots.containsKey(name)) {
                return localTypes.get(name);
            }
            String fieldDescriptor = findFieldDescriptor(currentClass, name);
            if (fieldDescriptor != null) {
                emit("aload_0");
                return fieldDescriptor;
            }
            return "I";
        }
        if (target instanceof ThisLoc) {
            emit("aload_0");
            return descriptorOfStoreTarget("L" + currentClass + ";", ((ThisLoc) target).getLoc());
        }
        if (target instanceof MemberLoc) {
            MemberLoc member = (MemberLoc) target;
            String baseDescriptor = loadBase(member.getMemberName().getName());
            return descriptorOfStoreTarget(baseDescriptor, member.getLoc());
        }
        return "I";
    }

    /**
     * Walks the intermediate members of a store target (emitting getfield for
     * every step but the last) and returns the descriptor of the final field.
     */
    private String descriptorOfStoreTarget(String ownerDescriptor, Location location) {
        if (location instanceof SimpleLoc) {
            String descriptor = findFieldDescriptor(classOf(ownerDescriptor),
                    ((SimpleLoc) location).getId().getName());
            return descriptor == null ? "I" : descriptor;
        }
        if (location instanceof MemberLoc) {
            MemberLoc member = (MemberLoc) location;
            String owner = classOf(ownerDescriptor);
            String field = member.getMemberName().getName();
            String descriptor = findFieldDescriptor(owner, field);
            if (descriptor == null) {
                descriptor = "I";
            }
            emit("getfield " + owner + "/" + field + " " + descriptor);
            return descriptorOfStoreTarget(descriptor, member.getLoc());
        }
        return "I";
    }

    /** emits the actual store instruction (istore / putfield / ...). */
    private void finishStore(Location target, String descriptor) {
        if (target instanceof SimpleLoc) {
            String name = ((SimpleLoc) target).getId().getName();
            if (slots.containsKey(name)) {
                emit(storeInstruction(descriptor) + " " + slots.get(name));
                return;
            }
            String fieldDescriptor = findFieldDescriptor(currentClass, name);
            if (fieldDescriptor != null) {
                emit("putfield " + currentClass + "/" + name + " " + fieldDescriptor);
                return;
            }
            emit(storeInstruction(descriptor) + " " + slotOf(name, descriptor));
            return;
        }
        if (target instanceof ThisLoc) {
            putFieldOf("L" + currentClass + ";", ((ThisLoc) target).getLoc());
            return;
        }
        if (target instanceof MemberLoc) {
            MemberLoc member = (MemberLoc) target;
            putFieldOf(staticTypeOfBase(member.getMemberName().getName()), member.getLoc());
        }
    }

    private void putFieldOf(String ownerDescriptor, Location location) {
        if (location instanceof SimpleLoc) {
            String owner = classOf(ownerDescriptor);
            String field = ((SimpleLoc) location).getId().getName();
            String descriptor = findFieldDescriptor(owner, field);
            if (descriptor == null) {
                descriptor = "I";
            }
            emit("putfield " + owner + "/" + field + " " + descriptor);
            return;
        }
        if (location instanceof MemberLoc) {
            MemberLoc member = (MemberLoc) location;
            String descriptor = findFieldDescriptor(classOf(ownerDescriptor),
                    member.getMemberName().getName());
            if (descriptor == null) {
                descriptor = "I";
            }
            putFieldOf(descriptor, member.getLoc());
        }
    }

    private String staticTypeOfBase(String name) {
        if (localTypes.containsKey(name)) {
            return localTypes.get(name);
        }
        String fieldDescriptor = findFieldDescriptor(currentClass, name);
        return fieldDescriptor != null ? fieldDescriptor : "L" + currentClass + ";";
    }

    /* ==================================================================== */
    /*  Calls                                                               */
    /* ==================================================================== */

    @Override
    public String visit(MethodCall methodCall) {
        String ownerClass;
        Location instance = methodCall.getInstance();
        String callee = methodCall.getCallee().getName();

        /* `Foo()` is parsed as a bare method call, but when `Foo` is a declared
           module/struct it really is an instantiation. */
        if (instance == null && memberIndex.containsKey(callee)) {
            return generateInstantiation(callee, methodCall.getArguments());
        }

        if (instance == null) {
            emit("aload_0");
            ownerClass = currentClass;
        } else if (instance instanceof ThisLoc && ((ThisLoc) instance).getLoc() == null) {
            emit("aload_0");
            ownerClass = currentClass;
        } else {
            ownerClass = classOf(generateExpression(instance));
        }

        Method target = findMethod(ownerClass, callee);

        StringBuilder signature = new StringBuilder("(");
        List<Expression> arguments = methodCall.getArguments();
        for (int i = 0; i < arguments.size(); i++) {
            String expected = (target != null && i < target.getParameters().size())
                    ? descriptorOf(target.getParameters().get(i).getType())
                    : inferType(arguments.get(i));
            String actual = generateExpression(arguments.get(i));
            convert(actual, expected);
            signature.append(expected);
        }
        String returnDescriptor = (target != null) ? descriptorOf(target.getReturnType()) : "V";
        signature.append(")").append(returnDescriptor);

        emit("invokevirtual " + ownerClass + "/" + callee + signature);
        return returnDescriptor;
    }

    @Override
    public String visit(ConstructorCall constructorCall) {
        return generateInstantiation(constructorCall.getName().getName(), constructorCall.getArguments());
    }

    private String generateInstantiation(String name, List<Expression> arguments) {
        emit("new " + name);
        emit("dup");
        emit("invokespecial " + name + "/<init>()V");

        /* MOL declares no explicit constructor; when arguments are supplied and
           the type owns an `init` method, that method plays the role. */
        Method init = findMethod(name, "init");
        if (!arguments.isEmpty() && init != null) {
            emit("dup");
            StringBuilder signature = new StringBuilder("(");
            for (int i = 0; i < arguments.size(); i++) {
                String expected = (i < init.getParameters().size())
                        ? descriptorOf(init.getParameters().get(i).getType())
                        : inferType(arguments.get(i));
                String actual = generateExpression(arguments.get(i));
                convert(actual, expected);
                signature.append(expected);
            }
            String returnDescriptor = descriptorOf(init.getReturnType());
            signature.append(")").append(returnDescriptor);
            emit("invokevirtual " + name + "/init" + signature);
            if (!"V".equals(returnDescriptor)) {
                emit(popFor(returnDescriptor));
            }
        }
        return "L" + name + ";";
    }

    /* ==================================================================== */
    /*  Types                                                               */
    /* ==================================================================== */

    @Override
    public String visit(PrimitiveType primitiveType) {
        return descriptorOf(primitiveType);
    }

    @Override
    public String visit(UserDefinedType userDefinedType) {
        return descriptorOf(userDefinedType);
    }

    @Override
    public String visit(Identifier identifier) {
        return identifier.getName();
    }

    private Type typeOfVar(Var var) {
        if (var.getType() != null) {
            return var.getType();
        }
        if (var.getConstructorCall() != null) {
            return new UserDefinedType(var.getConstructorCall().getName());
        }
        return null;
    }

    private String descriptorOf(Type type) {
        if (type == null) {
            return "V";
        }
        if (type instanceof PrimitiveType) {
            PrimitiveType.Primitive primitive = ((PrimitiveType) type).getPrimitive();
            if (primitive == PrimitiveType.Primitive.INT) {
                return "I";
            }
            if (primitive == PrimitiveType.Primitive.FLOAT) {
                return "F";
            }
            if (primitive == PrimitiveType.Primitive.DOUBLE) {
                return "D";
            }
            if (primitive == PrimitiveType.Primitive.CHAR) {
                return "C";
            }
            if (primitive == PrimitiveType.Primitive.BOOL) {
                return "Z";
            }
            return "V";
        }
        if (type instanceof UserDefinedType) {
            return "L" + ((UserDefinedType) type).getId().getName() + ";";
        }
        return "V";
    }

    private String classOf(String descriptor) {
        if (descriptor != null && descriptor.startsWith("L") && descriptor.endsWith(";")) {
            return descriptor.substring(1, descriptor.length() - 1);
        }
        return currentClass;
    }

    private boolean isIntLike(String descriptor) {
        return "I".equals(descriptor) || "Z".equals(descriptor) || "C".equals(descriptor);
    }

    private String loadInstruction(String descriptor) {
        if ("D".equals(descriptor)) {
            return "dload";
        }
        if ("F".equals(descriptor)) {
            return "fload";
        }
        if (isIntLike(descriptor)) {
            return "iload";
        }
        return "aload";
    }

    private String storeInstruction(String descriptor) {
        if ("D".equals(descriptor)) {
            return "dstore";
        }
        if ("F".equals(descriptor)) {
            return "fstore";
        }
        if (isIntLike(descriptor)) {
            return "istore";
        }
        return "astore";
    }

    private String popFor(String descriptor) {
        return "D".equals(descriptor) ? "pop2" : "pop";
    }

    private String printlnDescriptor(String descriptor) {
        if ("I".equals(descriptor) || "Z".equals(descriptor) || "C".equals(descriptor)
                || "F".equals(descriptor) || "D".equals(descriptor)) {
            return descriptor;
        }
        return "Ljava/lang/Object;";
    }

    private String promote(String left, String right) {
        if ("D".equals(left) || "D".equals(right)) {
            return "D";
        }
        if ("F".equals(left) || "F".equals(right)) {
            return "F";
        }
        if (isIntLike(left) && isIntLike(right)) {
            return "I";
        }
        return isIntLike(left) ? "I" : left;
    }

    /** emits a numeric conversion if the value on the stack needs one. */
    private void convert(String actual, String expected) {
        if (actual == null || expected == null || actual.equals(expected)) {
            return;
        }
        if (isIntLike(actual) && isIntLike(expected)) {
            return;
        }
        if (isIntLike(actual) && "F".equals(expected)) {
            emit("i2f");
        } else if (isIntLike(actual) && "D".equals(expected)) {
            emit("i2d");
        } else if ("F".equals(actual) && "D".equals(expected)) {
            emit("f2d");
        } else if ("D".equals(actual) && "F".equals(expected)) {
            emit("d2f");
        } else if ("F".equals(actual) && isIntLike(expected)) {
            emit("f2i");
        } else if ("D".equals(actual) && isIntLike(expected)) {
            emit("d2i");
        }
    }

    /* --------------------- static type inference ------------------------ */

    /** computes the descriptor an expression will leave on the stack. */
    private String inferType(Expression expression) {
        if (expression == null) {
            return "V";
        }
        if (expression instanceof ConstantExpression) {
            Object value = ((ConstantExpression) expression).getValue();
            if (value instanceof Boolean) {
                return "Z";
            }
            if (value instanceof Character) {
                return "C";
            }
            if (value instanceof Double) {
                return "D";
            }
            if (value instanceof Float) {
                return "F";
            }
            return "I";
        }
        if (expression instanceof IntLiteral) {
            return "I";
        }
        if (expression instanceof FloatLiteral) {
            return "F";
        }
        if (expression instanceof DoubleLiteral) {
            return "D";
        }
        if (expression instanceof CharLiteral) {
            return "C";
        }
        if (expression instanceof BoolLiteral) {
            return "Z";
        }
        if (expression instanceof ParanthesisExpr) {
            return inferType(((ParanthesisExpr) expression).getExpression());
        }
        if (expression instanceof UnaryExpression) {
            UnaryExpression unary = (UnaryExpression) expression;
            return unary.getOperand() == UnaryOperator.NOT ? "Z" : inferType(unary.getExpression());
        }
        if (expression instanceof UnaryOpExpr) {
            UnaryOpExpr unary = (UnaryOpExpr) expression;
            return unary.getOperator() == UnaryOpExpr.Operator.NOT ? "Z" : inferType(unary.getOperand());
        }
        if (expression instanceof BinaryExpression) {
            BinaryExpression binary = (BinaryExpression) expression;
            if (isArithmetic(binary.getOperator())) {
                return promote(inferType(binary.getLeftOperand()), inferType(binary.getRightOperand()));
            }
            return "Z";
        }
        if (expression instanceof ConstructorCall) {
            return "L" + ((ConstructorCall) expression).getName().getName() + ";";
        }
        if (expression instanceof MethodCall) {
            MethodCall call = (MethodCall) expression;
            String owner = currentClass;
            Location instance = call.getInstance();
            if (instance == null && memberIndex.containsKey(call.getCallee().getName())) {
                return "L" + call.getCallee().getName() + ";";
            }
            if (instance != null && !(instance instanceof ThisLoc && ((ThisLoc) instance).getLoc() == null)) {
                owner = classOf(inferType(instance));
            }
            Method target = findMethod(owner, call.getCallee().getName());
            return target != null ? descriptorOf(target.getReturnType()) : "V";
        }
        if (expression instanceof MethodCallLoc) {
            return inferType(((MethodCallLoc) expression).getMethodCall());
        }
        if (expression instanceof ThisLoc) {
            ThisLoc thisLoc = (ThisLoc) expression;
            String descriptor = "L" + currentClass + ";";
            return thisLoc.getLoc() == null ? descriptor : inferMemberChain(descriptor, thisLoc.getLoc());
        }
        if (expression instanceof SimpleLoc) {
            String name = ((SimpleLoc) expression).getId().getName();
            if (localTypes.containsKey(name)) {
                return localTypes.get(name);
            }
            String fieldDescriptor = findFieldDescriptor(currentClass, name);
            return fieldDescriptor != null ? fieldDescriptor : "I";
        }
        if (expression instanceof MemberLoc) {
            MemberLoc member = (MemberLoc) expression;
            return inferMemberChain(staticTypeOfBase(member.getMemberName().getName()), member.getLoc());
        }
        return "I";
    }

    private String inferMemberChain(String ownerDescriptor, Location location) {
        if (location instanceof SimpleLoc) {
            String descriptor = findFieldDescriptor(classOf(ownerDescriptor),
                    ((SimpleLoc) location).getId().getName());
            return descriptor == null ? "I" : descriptor;
        }
        if (location instanceof MemberLoc) {
            MemberLoc member = (MemberLoc) location;
            String descriptor = findFieldDescriptor(classOf(ownerDescriptor),
                    member.getMemberName().getName());
            if (descriptor == null) {
                descriptor = "I";
            }
            return inferMemberChain(descriptor, member.getLoc());
        }
        if (location instanceof MethodCallLoc) {
            return inferType(((MethodCallLoc) location).getMethodCall());
        }
        return ownerDescriptor;
    }

    /* --------------------- symbol lookup -------------------------------- */

    /** looks a field up in `className` and, transitively, in its super classes. */
    private String findFieldDescriptor(String className, String fieldName) {
        String current = className;
        Set<String> visited = new LinkedHashSet<>();
        while (current != null && memberIndex.containsKey(current) && visited.add(current)) {
            for (Member member : memberIndex.get(current)) {
                if (member instanceof VarDecl) {
                    Var var = ((VarDecl) member).getVar();
                    if (var.getName().getName().equals(fieldName)) {
                        return descriptorOf(typeOfVar(var));
                    }
                }
            }
            current = superIndex.get(current);
        }
        return null;
    }

    /** looks a method up in `className` and, transitively, in its super classes. */
    private Method findMethod(String className, String methodName) {
        String current = className;
        Set<String> visited = new LinkedHashSet<>();
        while (current != null && memberIndex.containsKey(current) && visited.add(current)) {
            for (Member member : memberIndex.get(current)) {
                if (member instanceof MethodDecl) {
                    Method method = ((MethodDecl) member).getMethod();
                    if (method.getName().getName().equals(methodName)) {
                        return method;
                    }
                }
            }
            current = superIndex.get(current);
        }
        /* fallback: a bare call may target a method of any other declaration */
        for (Map.Entry<String, List<Member>> entry : memberIndex.entrySet()) {
            for (Member member : entry.getValue()) {
                if (member instanceof MethodDecl) {
                    Method method = ((MethodDecl) member).getMethod();
                    if (method.getName().getName().equals(methodName)) {
                        return method;
                    }
                }
            }
        }
        return null;
    }

    /* ==================================================================== */
    /*  low level emission                                                  */
    /* ==================================================================== */

    /** one instruction per line, indented (never two commands on the same line). */
    private void emit(String instruction) {
        code.append("    ").append(instruction).append("\n");
    }

    private void line(String text) {
        code.append(text).append("\n");
    }

    private void label(String name) {
        code.append(name).append(":\n");
    }

    private String newLabel(String prefix) {
        return "Label_" + prefix + "_" + (labelCounter++);
    }
}
