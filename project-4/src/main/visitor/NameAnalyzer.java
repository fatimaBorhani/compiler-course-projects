package main.visitor;

import main.ast.core.*;
import main.ast.declarations.Module;
import main.ast.declarations.*;
import main.ast.statements.*;
import main.ast.expressions.*;
import main.ast.expressions.literals.*;
import main.ast.types.*;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemAlreadyExistsException;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.*;

import java.util.*;

/**
 * Phase 2 - Name analysis.
 *
 * Responsibilities:
 *   1. Build the symbol table (modules, structs, their fields and methods, and
 *      the local scopes of every method body).
 *   2. Report the following errors:
 *        - <Name> not declared
 *        - <Name> already defined
 *        - <Name> is private
 *        - <Name> is uninitialized
 *   3. (Bonus) Reachability analysis from main: prune unreachable modules /
 *      structs and emit "<Name> is unreachable" warnings.
 *
 * The {@code modules} / {@code structs} maps and the member-lookup helpers are
 * reused by {@link TypeAnalyzer}.
 */
public class NameAnalyzer extends Visitor<Void> {

    public final List<String> errors = new ArrayList<>();
    public final List<String> warnings = new ArrayList<>();

    // Top level declarations, keyed by name.
    public final Map<String, ModuleItem> modules = new LinkedHashMap<>();
    public final Map<String, StructItem> structs = new LinkedHashMap<>();

    private SymbolTable rootScope;
    private SymbolTable currentScope;

    // The module / struct whose body is currently being analysed.
    private String currentTypeName;
    private boolean inModule;

    private Program program;

    // Line ranges (1-based, inclusive) of unreachable top-level declarations,
    // used to emit the optimized source file. end == Integer.MAX_VALUE means EOF.
    private final List<int[]> removedLineRanges = new ArrayList<>();

    // ---------------------------------------------------------------------
    // Error helpers
    // ---------------------------------------------------------------------

    private void error(int line, String message) {
        errors.add("Line " + line + " : " + message);
    }

    private void notDeclared(int line, String name) {
        error(line, name + " not declared");
    }

    private void alreadyDefined(int line, String name) {
        error(line, name + " already defined");
    }

    private void isPrivate(int line, String name) {
        error(line, name + " is private");
    }

    private void uninitialized(int line, String name) {
        error(line, name + " is uninitialized");
    }

    // ---------------------------------------------------------------------
    // Pass 0/1 : collect declarations
    // ---------------------------------------------------------------------

    @Override
    public Void visit(Program program) {
        this.program = program;
        rootScope = new SymbolTable();
        SymbolTable.root = rootScope;
        currentScope = rootScope;

        // Pass 1: register every module / struct together with its members.
        for (TopLevelDecl decl : program.getTopLevelDeclarations()) {
            if (decl instanceof ModuleDecl) {
                collectModule(((ModuleDecl) decl).getModule());
            } else if (decl instanceof StructDecl) {
                collectStruct(((StructDecl) decl).getStruct());
            }
        }

        // Pass 2: analyse the bodies.
        for (TopLevelDecl decl : program.getTopLevelDeclarations()) {
            decl.accept(this);
        }

        // Bonus: reachability based dead code elimination.
        analyzeReachability();
        return null;
    }

    private String visibilityOf(Member member) {
        AccessModifier am = member.getAccessModifier();
        if (am == AccessModifier.PRIVATE) {
            return "private";
        }
        return "public"; // default when no modifier is present
    }

    /** The declared type of a variable, taking constructor-calls into account. */
    private Type declaredType(Var var) {
        if (var.getType() != null) {
            return var.getType();
        }
        if (var.getConstructorCall() != null) {
            return new UserDefinedType(var.getConstructorCall().getName());
        }
        return null;
    }

    private void collectModule(Module module) {
        String name = module.getName().getName();
        ModuleItem item = new ModuleItem(name);
        item.setModule(module);
        try {
            rootScope.put(item);
            modules.put(name, item);
        } catch (ItemAlreadyExistsException e) {
            alreadyDefined(module.getLine(), name);
            return; // keep the first definition
        }
        collectMembers(module.getMembers(), item.getModuleSymbolTable());
    }

    private void collectStruct(Struct struct) {
        String name = struct.getName().getName();
        StructItem item = new StructItem(name);
        item.setStruct(struct);
        try {
            rootScope.put(item);
            structs.put(name, item);
        } catch (ItemAlreadyExistsException e) {
            alreadyDefined(struct.getLine(), name);
            return;
        }
        collectMembers(struct.getMembers(), item.getStructSymbolTable());
    }

    private void collectMembers(List<Member> members, SymbolTable table) {
        for (Member member : members) {
            if (member instanceof VarDecl) {
                Var var = ((VarDecl) member).getVar();
                String vName = var.getName().getName();
                VarItem vItem = new VarItem(vName, declaredType(var),
                        var.getIsMutable(), visibilityOf(member));
                try {
                    table.put(vItem);
                } catch (ItemAlreadyExistsException e) {
                    alreadyDefined(var.getLine(), vName);
                }
            } else if (member instanceof MethodDecl) {
                Method method = ((MethodDecl) member).getMethod();
                String mName = method.getName().getName();
                MethodItem mItem = new MethodItem(mName, method.getReturnType(),
                        visibilityOf(member));
                mItem.setMethod(method);
                for (Parameter p : method.getParameters()) {
                    mItem.addParameterType(p.getType());
                }
                try {
                    table.put(mItem);
                } catch (ItemAlreadyExistsException e) {
                    alreadyDefined(method.getLine(), mName);
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Member / method lookup helpers (also used by TypeAnalyzer)
    // ---------------------------------------------------------------------

    public boolean isModuleOrStruct(String name) {
        return modules.containsKey(name) || structs.containsKey(name);
    }

    private SymbolTable memberTableOf(String typeName) {
        if (modules.containsKey(typeName)) {
            return modules.get(typeName).getModuleSymbolTable();
        }
        if (structs.containsKey(typeName)) {
            return structs.get(typeName).getStructSymbolTable();
        }
        return null;
    }

    private List<String> includesOf(String typeName) {
        List<String> result = new ArrayList<>();
        if (modules.containsKey(typeName)) {
            for (Identifier inc : modules.get(typeName).getModule().getIncludes()) {
                result.add(inc.getName());
            }
        }
        return result;
    }

    /** Look up a member in {@code typeName}'s own table only. */
    private SymbolTableItem lookupOwn(String typeName, String memberName) {
        SymbolTable table = memberTableOf(typeName);
        if (table == null) {
            return null;
        }
        try {
            return table.get(memberName);
        } catch (ItemNotFoundException e) {
            return null;
        }
    }

    /**
     * Look up a member in {@code typeName} and (transitively) in its included
     * modules. Members inherited through {@code includes} are only visible when
     * they are public.
     */
    public SymbolTableItem lookupMember(String typeName, String memberName) {
        return lookupMember(typeName, memberName, new HashSet<>(), false);
    }

    private SymbolTableItem lookupMember(String typeName, String memberName,
                                         Set<String> visited, boolean throughInclude) {
        if (typeName == null || visited.contains(typeName)) {
            return null;
        }
        visited.add(typeName);

        SymbolTableItem own = lookupOwn(typeName, memberName);
        if (own != null) {
            if (throughInclude && isPrivateItem(own)) {
                return null; // private members are not inherited
            }
            return own;
        }
        for (String inc : includesOf(typeName)) {
            SymbolTableItem found = lookupMember(inc, memberName, visited, true);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private boolean isPrivateItem(SymbolTableItem item) {
        if (item instanceof VarItem) {
            return "private".equals(((VarItem) item).getVisibility());
        }
        if (item instanceof MethodItem) {
            return "private".equals(((MethodItem) item).getVisibility());
        }
        return false;
    }

    /**
     * Resolves an unqualified (bare) method call. First searches {@code owner}
     * and its includes, then falls back to a global search across every module
     * and struct, because a public method may be called from another module
     * without an explicit qualifier.
     */
    public MethodItem resolveBareMethod(String owner, String methodName) {
        if (owner != null) {
            SymbolTableItem local = lookupMember(owner, methodName);
            if (local instanceof MethodItem) {
                return (MethodItem) local;
            }
        }
        for (String m : modules.keySet()) {
            SymbolTableItem item = lookupOwn(m, methodName);
            if (item instanceof MethodItem) {
                return (MethodItem) item;
            }
        }
        for (String s : structs.keySet()) {
            SymbolTableItem item = lookupOwn(s, methodName);
            if (item instanceof MethodItem) {
                return (MethodItem) item;
            }
        }
        return null;
    }

    /** Marks every module / struct that declares {@code methodName} as referenced. */
    private void addBareCallOwners(String methodName, Set<String> refs) {
        for (String m : modules.keySet()) {
            if (lookupOwn(m, methodName) instanceof MethodItem) {
                refs.add(m);
            }
        }
        for (String s : structs.keySet()) {
            if (lookupOwn(s, methodName) instanceof MethodItem) {
                refs.add(s);
            }
        }
    }

    // ---------------------------------------------------------------------
    // Pass 2 : body analysis
    // ---------------------------------------------------------------------

    @Override
    public Void visit(ModuleDecl moduleDecl) {
        return moduleDecl.getModule().accept(this);
    }

    @Override
    public Void visit(StructDecl structDecl) {
        return structDecl.getStruct().accept(this);
    }

    @Override
    public Void visit(Module module) {
        currentTypeName = module.getName().getName();
        inModule = true;
        for (Member member : module.getMembers()) {
            member.accept(this);
        }
        currentTypeName = null;
        return null;
    }

    @Override
    public Void visit(Struct struct) {
        currentTypeName = struct.getName().getName();
        inModule = false;
        for (Member member : struct.getMembers()) {
            member.accept(this);
        }
        currentTypeName = null;
        return null;
    }

    @Override
    public Void visit(VarDecl varDecl) {
        // Fields are already registered in pass 1; nothing else to check here.
        return null;
    }

    @Override
    public Void visit(MethodDecl methodDecl) {
        Method method = methodDecl.getMethod();
        SymbolTable methodScope = new SymbolTable(rootScope);
        SymbolTable saved = currentScope;
        currentScope = methodScope;

        // Parameters live in the method scope and are considered initialised.
        for (Parameter p : method.getParameters()) {
            VarItem pItem = new VarItem(p.getName().getName(), p.getType(),
                    p.getIsMutable(), "public");
            pItem.setInitialized(true);
            try {
                currentScope.put(pItem);
            } catch (ItemAlreadyExistsException e) {
                alreadyDefined(p.getLine(), p.getName().getName());
            }
        }

        // Analyse the body in a child scope so that a local variable may
        // legitimately shadow a parameter (e.g. `void m(int b)` + `int b = 5;`).
        if (method.getBody() != null) {
            method.getBody().accept(this);
        }

        currentScope = saved;
        return null;
    }

    @Override
    public Void visit(Block block) {
        SymbolTable saved = currentScope;
        currentScope = new SymbolTable(saved);
        for (Statement stmt : block.getStatements()) {
            stmt.accept(this);
        }
        currentScope = saved;
        return null;
    }

    @Override
    public Void visit(VarDeclStmt varDeclStmt) {
        Var var = varDeclStmt.getVar();
        String name = var.getName().getName();
        Type type = declaredType(var);

        // Make sure a user defined type actually exists.
        checkTypeDeclared(type, var.getLine());

        boolean initialised = false;
        if (varDeclStmt.getInitial() != null) {
            varDeclStmt.getInitial().accept(this);
            initialised = true;
        }
        if (var.getConstructorCall() != null) {
            var.getConstructorCall().accept(this);
            initialised = true;
        }

        VarItem item = new VarItem(name, type, var.getIsMutable(), "public");
        item.setInitialized(initialised);
        try {
            currentScope.put(item);
        } catch (ItemAlreadyExistsException e) {
            alreadyDefined(var.getLine(), name);
        }
        return null;
    }

    private void checkTypeDeclared(Type type, int line) {
        if (type instanceof UserDefinedType) {
            String tName = ((UserDefinedType) type).getStr();
            if (!isModuleOrStruct(tName)) {
                notDeclared(line, tName);
            }
        }
    }

    @Override
    public Void visit(AssignStmt assignStmt) {
        if (assignStmt.getRight() != null) {
            assignStmt.getRight().accept(this);
        }
        typeOfLocation(assignStmt.getLeft(), true);
        return null;
    }

    @Override
    public Void visit(MethodCallStmt methodCallStmt) {
        return methodCallStmt.getMethodCall().accept(this);
    }

    @Override
    public Void visit(ReturnStmt returnStmt) {
        if (returnStmt.getValue() != null) {
            returnStmt.getValue().accept(this);
        }
        return null;
    }

    @Override
    public Void visit(IfStmt ifStmt) {
        if (ifStmt.getCondition() != null) {
            ifStmt.getCondition().accept(this);
        }
        if (ifStmt.getThenBranch() != null) {
            ifStmt.getThenBranch().accept(this);
        }
        if (ifStmt.getElseBranch() != null) {
            ifStmt.getElseBranch().accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ForStmt forStmt) {
        SymbolTable saved = currentScope;
        currentScope = new SymbolTable(saved);
        for (Statement init : forStmt.getInitializers()) {
            init.accept(this);
        }
        if (forStmt.getCondition() != null) {
            forStmt.getCondition().accept(this);
        }
        for (AssignStmt upd : forStmt.getUpdaters()) {
            upd.accept(this);
        }
        if (forStmt.getBody() != null) {
            forStmt.getBody().accept(this);
        }
        currentScope = saved;
        return null;
    }

    @Override
    public Void visit(WhileStmt whileStmt) {
        if (whileStmt.getCondition() != null) {
            whileStmt.getCondition().accept(this);
        }
        if (whileStmt.getBody() != null) {
            whileStmt.getBody().accept(this);
        }
        return null;
    }

    @Override
    public Void visit(InputStmt inputStmt) {
        // input reads a value into the location -> treat it as an assignment target.
        typeOfLocation(inputStmt.getLoc(), true);
        return null;
    }

    @Override
    public Void visit(OutputStmt outputStmt) {
        if (outputStmt.getValue() != null) {
            outputStmt.getValue().accept(this);
        }
        return null;
    }

    // -------------------- expressions --------------------

    @Override
    public Void visit(ParanthesisExpr paranthesisExpr) {
        if (paranthesisExpr.getExpression() != null) {
            paranthesisExpr.getExpression().accept(this);
        }
        return null;
    }

    @Override
    public Void visit(BinaryExpression binaryOpExpr) {
        if (binaryOpExpr.getLeftOperand() != null) {
            binaryOpExpr.getLeftOperand().accept(this);
        }
        if (binaryOpExpr.getRightOperand() != null) {
            binaryOpExpr.getRightOperand().accept(this);
        }
        return null;
    }

    @Override
    public Void visit(UnaryExpression unaryExpression) {
        if (unaryExpression.getExpression() != null) {
            unaryExpression.getExpression().accept(this);
        }
        return null;
    }

    @Override
    public Void visit(UnaryOpExpr unaryOpExpr) {
        if (unaryOpExpr.getOperand() != null) {
            unaryOpExpr.getOperand().accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ConstructorCall constructorCall) {
        String typeName = constructorCall.getName().getName();
        if (!isModuleOrStruct(typeName)) {
            notDeclared(constructorCall.getLine(), typeName);
        }
        for (Expression arg : constructorCall.getArguments()) {
            arg.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(SimpleLoc simpleLoc) {
        typeOfLocation(simpleLoc, false);
        return null;
    }

    @Override
    public Void visit(MemberLoc memberLoc) {
        typeOfLocation(memberLoc, false);
        return null;
    }

    @Override
    public Void visit(ThisLoc thisLoc) {
        typeOfLocation(thisLoc, false);
        return null;
    }

    @Override
    public Void visit(MethodCallLoc methodCallLoc) {
        return methodCallLoc.getMethodCall().accept(this);
    }

    @Override
    public Void visit(MethodCall methodCall) {
        handleMethodCall(methodCall);
        return null;
    }

    // ---------------------------------------------------------------------
    // Location / method-call resolution
    // ---------------------------------------------------------------------

    /**
     * Resolves a location, performing name / privacy / initialisation checks,
     * and returns its type (or null if it could not be resolved).
     *
     * @param target true when the location is the target of an assignment
     *               (initialisation is recorded, no "uninitialized" error).
     */
    private Type typeOfLocation(Location loc, boolean target) {
        if (loc instanceof SimpleLoc) {
            SimpleLoc s = (SimpleLoc) loc;
            VarItem v = resolveVar(s.getId().getName(), s.getLine(), target);
            return v == null ? null : v.getType();
        }
        if (loc instanceof ThisLoc) {
            ThisLoc t = (ThisLoc) loc;
            if (currentTypeName == null) {
                return null;
            }
            if (t.getLoc() == null) {
                return new UserDefinedType(new Identifier(currentTypeName));
            }
            // member access through "this" -> own private members allowed.
            return resolveMemberChain(currentTypeName, t.getLoc(), false, target);
        }
        if (loc instanceof MemberLoc) {
            MemberLoc m = (MemberLoc) loc;
            VarItem base = resolveVar(m.getMemberName().getName(), m.getLine(), false);
            if (base == null) {
                return null;
            }
            String baseType = TypeUtils.name(base.getType());
            // external access -> private members are forbidden.
            return resolveMemberChain(baseType, m.getLoc(), true, target);
        }
        if (loc instanceof MethodCallLoc) {
            handleMethodCall(((MethodCallLoc) loc).getMethodCall());
            return null; // return type handled by TypeAnalyzer
        }
        return null;
    }

    private VarItem resolveVar(String name, int line, boolean target) {
        SymbolTableItem item;
        try {
            item = currentScope.get(name);
        } catch (ItemNotFoundException e) {
            notDeclared(line, name);
            return null;
        }
        if (!(item instanceof VarItem)) {
            // e.g. a module / struct / method name used as a value.
            notDeclared(line, name);
            return null;
        }
        VarItem v = (VarItem) item;
        if (target) {
            v.setInitialized(true);
        } else if (!v.isInitialized() && v.getType() instanceof PrimitiveType) {
            // Only primitives are tracked; object variables are initialised
            // through constructor / init() calls that we do not model here.
            uninitialized(line, name);
        }
        return v;
    }

    /**
     * Resolves the member chain {@code loc} against the members of
     * {@code typeName}.
     *
     * @param external true for {@code obj.x} (privacy enforced), false for
     *                 {@code this.x} inside the owning type.
     */
    private Type resolveMemberChain(String typeName, Location loc,
                                    boolean external, boolean target) {
        if (typeName == null || memberTableOf(typeName) == null) {
            return null;
        }
        if (loc instanceof SimpleLoc) {
            SimpleLoc s = (SimpleLoc) loc;
            String mName = s.getId().getName();
            SymbolTableItem item = lookupMember(typeName, mName);
            if (item == null) {
                notDeclared(s.getLine(), mName);
                return null;
            }
            if (external && isPrivateItem(item)) {
                isPrivate(s.getLine(), mName);
            }
            if (item instanceof VarItem) {
                return ((VarItem) item).getType();
            }
            return null;
        }
        if (loc instanceof MemberLoc) {
            MemberLoc m = (MemberLoc) loc;
            String mName = m.getMemberName().getName();
            SymbolTableItem item = lookupMember(typeName, mName);
            if (item == null) {
                notDeclared(m.getLine(), mName);
                return null;
            }
            if (external && isPrivateItem(item)) {
                isPrivate(m.getLine(), mName);
            }
            if (!(item instanceof VarItem)) {
                return null;
            }
            String nextType = TypeUtils.name(((VarItem) item).getType());
            return resolveMemberChain(nextType, m.getLoc(), true, target);
        }
        if (loc instanceof MethodCallLoc) {
            handleMethodCall(((MethodCallLoc) loc).getMethodCall());
            return null;
        }
        return null;
    }

    private void handleMethodCall(MethodCall mc) {
        // Arguments first.
        for (Expression arg : mc.getArguments()) {
            arg.accept(this);
        }

        String callee = mc.getCallee().getName();
        int line = mc.getLine();
        Location inst = mc.getInstance();

        if (inst == null) {
            // `Foo()` where Foo is a declared module/struct is an instantiation,
            // not a method call (the grammar prefers the `methodcall` rule over
            // `cons`, so it reaches us as a bare MethodCall).
            if (isModuleOrStruct(callee)) {
                return;
            }
            // bare call -> current module/struct + includes, then global.
            if (resolveBareMethod(currentTypeName, callee) == null) {
                notDeclared(line, callee);
            }
            return;
        }

        if (inst instanceof SimpleLoc) {
            String iName = ((SimpleLoc) inst).getId().getName();
            VarItem v = tryGetVar(iName);
            if (v != null) {
                resolveMethodOn(TypeUtils.name(v.getType()), callee, line, true);
            } else if (isModuleOrStruct(iName)) {
                resolveMethodOn(iName, callee, line, true);
            } else {
                notDeclared(inst.getLine(), iName);
            }
            return;
        }

        if (inst instanceof ThisLoc) {
            ThisLoc t = (ThisLoc) inst;
            if (t.getLoc() == null) {
                resolveMethodOn(currentTypeName, callee, line, false);
            } else {
                Type baseType = resolveMemberChain(currentTypeName, t.getLoc(), false, false);
                if (baseType != null) {
                    resolveMethodOn(TypeUtils.name(baseType), callee, line, true);
                }
            }
            return;
        }

        if (inst instanceof MemberLoc) {
            Type baseType = typeOfLocation(inst, false);
            if (baseType != null) {
                resolveMethodOn(TypeUtils.name(baseType), callee, line, true);
            }
        }
    }

    private void resolveMethodOn(String typeName, String methodName, int line, boolean external) {
        SymbolTableItem item = lookupMember(typeName, methodName);
        if (!(item instanceof MethodItem)) {
            notDeclared(line, methodName);
            return;
        }
        if (external && isPrivateItem(item)) {
            isPrivate(line, methodName);
        }
    }

    private VarItem tryGetVar(String name) {
        try {
            SymbolTableItem item = currentScope.get(name);
            return item instanceof VarItem ? (VarItem) item : null;
        } catch (ItemNotFoundException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------------
    // Bonus : reachability analysis from main
    // ---------------------------------------------------------------------

    private void analyzeReachability() {
        // Locate the top level declaration that owns a "main" method.
        String entry = null;
        for (Map.Entry<String, ModuleItem> e : modules.entrySet()) {
            if (lookupOwn(e.getKey(), "main") instanceof MethodItem) {
                entry = e.getKey();
                break;
            }
        }
        if (entry == null) {
            for (Map.Entry<String, StructItem> e : structs.entrySet()) {
                if (lookupOwn(e.getKey(), "main") instanceof MethodItem) {
                    entry = e.getKey();
                    break;
                }
            }
        }
        if (entry == null) {
            return; // no entry point -> nothing to prune
        }

        Set<String> reachable = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        reachable.add(entry);
        queue.add(entry);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (String ref : referencedTypes(current)) {
                if (isModuleOrStruct(ref) && reachable.add(ref)) {
                    queue.add(ref);
                }
            }
        }

        // Warn about and remove every unreachable top level declaration.
        // Also record its source line span so the optimized file can be emitted.
        List<TopLevelDecl> all = program.getTopLevelDeclarations();
        int[] startLines = new int[all.size()];
        for (int i = 0; i < all.size(); i++) {
            startLines[i] = declLine(all.get(i));
        }

        List<TopLevelDecl> kept = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            TopLevelDecl decl = all.get(i);
            String name = declName(decl);
            int line = startLines[i];
            if (name != null && !reachable.contains(name)) {
                warnings.add("Warning Line " + line + " : " + name + " is unreachable");
                int end = (i + 1 < all.size()) ? startLines[i + 1] - 1 : Integer.MAX_VALUE;
                removedLineRanges.add(new int[]{line, end});
            } else {
                kept.add(decl);
            }
        }
        program.setTopLevelDeclarations(kept);
    }

    private int declLine(TopLevelDecl decl) {
        if (decl instanceof ModuleDecl) {
            return ((ModuleDecl) decl).getModule().getLine();
        }
        if (decl instanceof StructDecl) {
            return ((StructDecl) decl).getStruct().getLine();
        }
        return 0;
    }

    private String declName(TopLevelDecl decl) {
        if (decl instanceof ModuleDecl) {
            return ((ModuleDecl) decl).getModule().getName().getName();
        }
        if (decl instanceof StructDecl) {
            return ((StructDecl) decl).getStruct().getName().getName();
        }
        return null;
    }

    /**
     * Writes the optimized source (unreachable declarations removed) next to the
     * input as {@code <base>_optimized.mol}. Does nothing if nothing was pruned.
     */
    public void writeOptimizedSource(String inputPath) {
        if (removedLineRanges.isEmpty()) {
            return;
        }
        try {
            java.util.List<String> lines = java.nio.file.Files.readAllLines(
                    java.nio.file.Paths.get(inputPath));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                int lineNo = i + 1; // 1-based
                boolean removed = false;
                for (int[] range : removedLineRanges) {
                    if (lineNo >= range[0] && lineNo <= range[1]) {
                        removed = true;
                        break;
                    }
                }
                if (!removed) {
                    sb.append(lines.get(i)).append(System.lineSeparator());
                }
            }
            String base = inputPath;
            int dot = base.lastIndexOf('.');
            if (dot >= 0) {
                base = base.substring(0, dot);
            }
            java.nio.file.Files.write(java.nio.file.Paths.get(base + "_optimized.mol"),
                    sb.toString().getBytes());
        } catch (java.io.IOException e) {
            // Optimization output is best-effort; ignore I/O problems.
        }
    }

    /** All module / struct names referenced from the body of {@code typeName}. */
    private Set<String> referencedTypes(String typeName) {
        Set<String> refs = new HashSet<>();
        refs.addAll(includesOf(typeName));

        List<Member> members;
        if (modules.containsKey(typeName)) {
            members = modules.get(typeName).getModule().getMembers();
        } else if (structs.containsKey(typeName)) {
            members = structs.get(typeName).getStruct().getMembers();
        } else {
            return refs;
        }

        for (Member member : members) {
            if (member instanceof VarDecl) {
                collectTypeNames(declaredType(((VarDecl) member).getVar()), refs);
            } else if (member instanceof MethodDecl) {
                Method method = ((MethodDecl) member).getMethod();
                collectTypeNames(method.getReturnType(), refs);
                for (Parameter p : method.getParameters()) {
                    collectTypeNames(p.getType(), refs);
                }
                if (method.getBody() != null) {
                    new ReferenceCollector(refs).visit(method.getBody());
                }
            }
        }
        return refs;
    }

    private void collectTypeNames(Type type, Set<String> refs) {
        if (type instanceof UserDefinedType) {
            refs.add(((UserDefinedType) type).getStr());
        }
    }

    /** Walks a method body and collects every referenced module / struct name. */
    private final class ReferenceCollector extends Visitor<Void> {
        private final Set<String> refs;

        ReferenceCollector(Set<String> refs) {
            this.refs = refs;
        }

        @Override
        public Void visit(Block block) {
            for (Statement s : block.getStatements()) {
                if (s != null) s.accept(this);
            }
            return null;
        }

        @Override
        public Void visit(VarDeclStmt stmt) {
            collectTypeNames(declaredType(stmt.getVar()), refs);
            if (stmt.getVar().getConstructorCall() != null) {
                stmt.getVar().getConstructorCall().accept(this);
            }
            if (stmt.getInitial() != null) stmt.getInitial().accept(this);
            return null;
        }

        @Override
        public Void visit(AssignStmt stmt) {
            if (stmt.getLeft() != null) stmt.getLeft().accept(this);
            if (stmt.getRight() != null) stmt.getRight().accept(this);
            return null;
        }

        @Override
        public Void visit(MethodCallStmt stmt) {
            return stmt.getMethodCall().accept(this);
        }

        @Override
        public Void visit(ReturnStmt stmt) {
            if (stmt.getValue() != null) stmt.getValue().accept(this);
            return null;
        }

        @Override
        public Void visit(IfStmt stmt) {
            if (stmt.getCondition() != null) stmt.getCondition().accept(this);
            if (stmt.getThenBranch() != null) stmt.getThenBranch().accept(this);
            if (stmt.getElseBranch() != null) stmt.getElseBranch().accept(this);
            return null;
        }

        @Override
        public Void visit(ForStmt stmt) {
            for (Statement init : stmt.getInitializers()) init.accept(this);
            if (stmt.getCondition() != null) stmt.getCondition().accept(this);
            for (AssignStmt upd : stmt.getUpdaters()) upd.accept(this);
            if (stmt.getBody() != null) stmt.getBody().accept(this);
            return null;
        }

        @Override
        public Void visit(WhileStmt stmt) {
            if (stmt.getCondition() != null) stmt.getCondition().accept(this);
            if (stmt.getBody() != null) stmt.getBody().accept(this);
            return null;
        }

        @Override
        public Void visit(OutputStmt stmt) {
            if (stmt.getValue() != null) stmt.getValue().accept(this);
            return null;
        }

        @Override
        public Void visit(InputStmt stmt) {
            if (stmt.getLoc() != null) stmt.getLoc().accept(this);
            return null;
        }

        @Override
        public Void visit(ParanthesisExpr expr) {
            if (expr.getExpression() != null) expr.getExpression().accept(this);
            return null;
        }

        @Override
        public Void visit(BinaryExpression expr) {
            if (expr.getLeftOperand() != null) expr.getLeftOperand().accept(this);
            if (expr.getRightOperand() != null) expr.getRightOperand().accept(this);
            return null;
        }

        @Override
        public Void visit(UnaryExpression expr) {
            if (expr.getExpression() != null) expr.getExpression().accept(this);
            return null;
        }

        @Override
        public Void visit(UnaryOpExpr expr) {
            if (expr.getOperand() != null) expr.getOperand().accept(this);
            return null;
        }

        @Override
        public Void visit(ConstructorCall cc) {
            refs.add(cc.getName().getName());
            for (Expression a : cc.getArguments()) a.accept(this);
            return null;
        }

        @Override
        public Void visit(MethodCall mc) {
            if (mc.getInstance() == null) {
                String callee = mc.getCallee().getName();
                if (isModuleOrStruct(callee)) {
                    refs.add(callee);           // `Foo()` instantiation
                } else {
                    addBareCallOwners(callee, refs);
                }
            } else if (mc.getInstance() instanceof SimpleLoc) {
                String iName = ((SimpleLoc) mc.getInstance()).getId().getName();
                if (isModuleOrStruct(iName)) {
                    refs.add(iName);
                }
            }
            if (mc.getInstance() != null) mc.getInstance().accept(this);
            for (Expression a : mc.getArguments()) a.accept(this);
            return null;
        }

        @Override
        public Void visit(MethodCallLoc loc) {
            return loc.getMethodCall().accept(this);
        }

        @Override
        public Void visit(MemberLoc loc) {
            if (loc.getLoc() != null) loc.getLoc().accept(this);
            return null;
        }

        @Override
        public Void visit(ThisLoc loc) {
            if (loc.getLoc() != null) loc.getLoc().accept(this);
            return null;
        }
    }

    // ---------------------------------------------------------------------
    // Reporting
    // ---------------------------------------------------------------------

    public void printResults() {
        List<String> sortedWarnings = new ArrayList<>(warnings);
        List<String> sortedErrors = new ArrayList<>(errors);
        sortedWarnings.sort(Comparator.comparingInt(NameAnalyzer::lineOf));
        sortedErrors.sort(Comparator.comparingInt(NameAnalyzer::lineOf));
        for (String w : sortedWarnings) {
            System.out.println(w);
        }
        for (String e : sortedErrors) {
            System.out.println(e);
        }
    }

    /** Extracts the line number out of a "... Line N: ..." message for stable sorting. */
    static int lineOf(String message) {
        int idx = message.indexOf("Line ");
        if (idx < 0) {
            return Integer.MAX_VALUE;
        }
        int start = idx + "Line ".length();
        int end = start;
        while (end < message.length() && Character.isDigit(message.charAt(end))) {
            end++;
        }
        if (end == start) {
            return Integer.MAX_VALUE;
        }
        return Integer.parseInt(message.substring(start, end));
    }
}
