package miniJava.ContextualAnalysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

public class TypeChecking implements Visitor<Object, TypeDenoter> {
    private ErrorReporter _errors;

    private Map<String, Stack<Declaration>> IDTable = new HashMap<>();
    private Stack<Declaration> localDeclMap;

    private String currClass = "";
    private boolean qRefFlag = false;
    private Stack<Declaration> helper = null;
    private Declaration methodCalls = null;

    public TypeChecking(ErrorReporter errors) {
        this._errors = errors;
    }

    public void parse(Package prog) {
        prog.visit(this, null);
    }

    private void reportTypeError(AST ast, String errMsg) {
        _errors.reportError(ast.posn == null
                ? "*** " + errMsg
                : "*** " + ast.posn.toString() + ": " + errMsg);
    }

    @Override
    public TypeDenoter visitPackage(Package prog, Object arg) {        
        this.IDTable.put("System", new Stack<>());
        this.IDTable.get("System").push(new FieldDecl(false, true, new ClassType(new Identifier(new Token(TokenType.Identifier, "_PrintStream", null)), null), "out", null, "_PrintStream"));

        this.IDTable.put("_PrintStream", new Stack<>());
        ParameterDeclList temp = new ParameterDeclList();
        temp.add(new ParameterDecl(new BaseType(TypeKind.INT, null), "n", null));
        this.IDTable.get("_PrintStream").push(new MethodDecl(new FieldDecl(false, false, new BaseType(TypeKind.VOID, null), "println", null), temp, new StatementList(), null));

        this.IDTable.put("String", new Stack<>());

        for (ClassDecl c : prog.classDeclList) {
            IDTable.put(c.name, new Stack<>());

            for (FieldDecl f : c.fieldDeclList) {
                IDTable.get(c.name).push(f);
            }

            for (MethodDecl m : c.methodDeclList) {
                IDTable.get(c.name).push(m);
            }
        }

        for (ClassDecl c : prog.classDeclList) {
            currClass = c.name;
            c.visit(this, arg);
        }
        return null;
    }

    @Override
    public TypeDenoter visitClassDecl(ClassDecl cd, Object arg) {
        for (MethodDecl m : cd.methodDeclList) {
            this.localDeclMap = new Stack<>();
            m.visit(this, arg);
        }

        return null; 
    }

    @Override
    public TypeDenoter visitFieldDecl(FieldDecl fd, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitMethodDecl(MethodDecl md, Object arg) {
        StatementList sl = md.statementList;
        TypeDenoter temp = null;

        ParameterDeclList pdl = md.parameterDeclList;

        for (ParameterDecl pd : pdl) {
            pd.visit(this, arg);
        }

        for (Statement s : sl) {
            temp = s.visit(this, arg);

            if (temp != null && (md.type.typeKind == TypeKind.VOID && (temp.typeKind != TypeKind.NULL && temp.typeKind != TypeKind.VOID))) {
                reportTypeError(md, "Wrong return type for method " + md.name);
            } else if (temp != null && (temp.typeKind != md.type.typeKind && temp.typeKind != TypeKind.NULL)) {
                reportTypeError(md, "Wrong return type for method " + md.name);
            }
        }

        return null;
    }

    @Override
    public TypeDenoter visitParameterDecl(ParameterDecl pd, Object arg) {
        localDeclMap.push(pd);

        return pd.type;
    }

    @Override
    public TypeDenoter visitVarDecl(VarDecl decl, Object arg) {
        localDeclMap.push(decl);
        return decl.type;
    }

    @Override
    public TypeDenoter visitBaseType(BaseType type, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitClassType(ClassType type, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitArrayType(ArrayType type, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitBlockStmt(BlockStmt stmt, Object arg) {
        StatementList sl = stmt.sl;
        TypeDenoter temp = null;

        for (Statement s : sl) {
            if (temp == null) {
                temp = s.visit(this, arg);
            } else {
                s.visit(this, arg);
            }
        }

        return temp;
    }

    @Override
    public TypeDenoter visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        localDeclMap.push(stmt.varDecl);

        TypeDenoter left = stmt.varDecl.visit(this, arg);
        TypeDenoter right = stmt.initExp.visit(this, arg);

        if (right.typeKind == TypeKind.NULL) {
            return null;
        }

        if (left.typeKind != right.typeKind) {
            reportTypeError(stmt, stmt.varDecl.name + " has an invalid assignment type");
            return null;
        }

        if (left.typeKind == TypeKind.CLASS && !stmt.varDecl.className.equals(((ClassType) right).className.spelling)) {
            reportTypeError(stmt, stmt.varDecl.name + " has an invalid assignment");
        }

        if (left.typeKind == TypeKind.ARRAY) {
            if (((ArrayType) left).eltType.typeKind != ((ArrayType) right).eltType.typeKind) {
                reportTypeError(stmt, "Assignment statement has an invalid assignment");
            } else if (((ArrayType) left).eltType.typeKind == TypeKind.CLASS
                    && !((((ClassType) ((ArrayType) left).eltType).className.spelling)
                            .equals(((ClassType) ((ArrayType) right).eltType).className.spelling))) {
                reportTypeError(stmt, "Assignment statement has an invalid assignment");
            }
        }

        return null;
    }

    @Override
    public TypeDenoter visitAssignStmt(AssignStmt stmt, Object arg) {
        this.qRefFlag = false;
        TypeDenoter left = stmt.ref.visit(this, arg);
        this.helper = null;

        TypeDenoter right = stmt.val.visit(this, arg);

        if (right.typeKind == TypeKind.NULL) {
            return null;
        }

        if (left.typeKind != right.typeKind) {
            reportTypeError(stmt, "Assignment statement has an invalid assignment type");
            return null;
        }

        if (left.typeKind == TypeKind.CLASS && !((((ClassType) left).className.spelling).equals(((ClassType) right).className.spelling))) {
            reportTypeError(stmt, "Assignment statement has an invalid assignment");
        }

        if (left.typeKind == TypeKind.ARRAY) {
            if (((ArrayType) left).eltType.typeKind != ((ArrayType) right).eltType.typeKind) {
                reportTypeError(stmt, "Assignment statement has an invalid assignment");
            } else if (((ArrayType) left).eltType.typeKind == TypeKind.CLASS && !((((ClassType) ((ArrayType) left).eltType).className.spelling).equals(((ClassType) ((ArrayType) right).eltType).className.spelling))) {
                reportTypeError(stmt, "Assignment statement has an invalid assignment");
            }
        }
        
        return null;
    }

    @Override
    public TypeDenoter visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        this.qRefFlag = false;
        TypeDenoter ref = stmt.ref.visit(this, arg);
        this.helper = null;

        TypeDenoter exp1 = stmt.ix.visit(this, arg);
        TypeDenoter exp2 = stmt.exp.visit(this, arg);

        if (exp2.typeKind == TypeKind.ARRAY) {
            exp2 = ((ArrayType) exp2).eltType;
        }

        if (ref.typeKind != TypeKind.ARRAY) {
            reportTypeError(stmt, "Reference is not an Array");
        }
        if (exp1.typeKind != TypeKind.INT) {
            reportTypeError(stmt, "Expression has to be of type Integer");
        }
        if (((ArrayType) ref).eltType.typeKind != exp2.typeKind) {
            reportTypeError(stmt, "Array Type does not match Assignment");
        }
        if (ref.typeKind == TypeKind.CLASS && !(((ClassType) ref).className.spelling.equals(((ClassType) exp2).className.spelling))) {
            reportTypeError(stmt, "Array Type does not match Assignment");
        } 
        
        return null;
    }

    @Override
    public TypeDenoter visitCallStmt(CallStmt stmt, Object arg) {
        // TODO
        this.qRefFlag = false;
        TypeDenoter temp = stmt.methodRef.visit(this, arg);
        this.helper = null;

        MethodDecl method = (MethodDecl) this.methodCalls;
        ParameterDeclList parameters = method.parameterDeclList;

        if (parameters.size() != stmt.argList.size()) {
            reportTypeError(method, "Call Expression does not contain Right number of Parameters");
        } else {
            for (int i = 0; i < parameters.size(); i++) {
                TypeDenoter left = parameters.get(i).visit(this, arg);
                TypeDenoter right = stmt.argList.get(i).visit(this, arg);

                if (left.typeKind != right.typeKind) {
                    reportTypeError(right, "Call Expression does not have matching method");
                }

                if (left.typeKind == TypeKind.CLASS
                        && !((ClassType) left).className.spelling.equals(((ClassType) right).className.spelling)) {
                    reportTypeError(right, "Call Expression does not have matching method");
                }

                if (left.typeKind == TypeKind.ARRAY) {
                    if (((ArrayType) left).eltType.typeKind != ((ArrayType) right).eltType.typeKind) {
                        reportTypeError(right, "Call Expression does not have matching method");
                    } else if (((ArrayType) left).eltType.typeKind == TypeKind.CLASS
                            && !((((ClassType) ((ArrayType) left).eltType).className.spelling)
                                    .equals(((ClassType) ((ArrayType) right).eltType).className.spelling))) {
                        reportTypeError(right, "Call Expression does not have matching method");
                    }
                }
            }
        }

        return temp;
    }

    @Override
    public TypeDenoter visitReturnStmt(ReturnStmt stmt, Object arg) {
        return stmt.returnExpr.visit(this, arg);
    }

    @Override
    public TypeDenoter visitIfStmt(IfStmt stmt, Object arg) {
        TypeDenoter condition = stmt.cond.visit(this, arg);

        if (condition.typeKind != TypeKind.BOOLEAN) {
            reportTypeError(stmt, "If statement condition is not a boolean");
        }
        
        return stmt.thenStmt.visit(this, arg);
    }

    @Override
    public TypeDenoter visitWhileStmt(WhileStmt stmt, Object arg) {
        TypeDenoter condition = stmt.cond.visit(this, arg);

        if (condition.typeKind != TypeKind.BOOLEAN) {
            reportTypeError(stmt, "While statement condition is not a boolean");
        }

        return stmt.body.visit(this, arg);
    }

    @Override
    public TypeDenoter visitUnaryExpr(UnaryExpr expr, Object arg) {
        TypeDenoter exTypeDenoter = expr.expr.visit(this, arg);
        this.helper = null;

        if (expr.operator.kind == TokenType.Minus) {
            if (exTypeDenoter.typeKind != TypeKind.INT) {
                reportTypeError(exTypeDenoter, "Unary Expression needs a integer expression");
            }
            return new BaseType(TypeKind.INT, null);
        } else {
            if (exTypeDenoter.typeKind != TypeKind.BOOLEAN) {
                reportTypeError(exTypeDenoter, "Unary Expression needs a integer expression");
            }
            return new BaseType(TypeKind.BOOLEAN, null);
        }
    }

    @Override
    public TypeDenoter visitBinaryExpr(BinaryExpr expr, Object arg) {
        TypeDenoter leftTypeDenoter = expr.left.visit(this, arg);
        TypeDenoter righTypeDenoter = expr.right.visit(this, arg);


        if (expr.operator.kind == TokenType.LogicalBiOperator) {
            if (leftTypeDenoter.typeKind == TypeKind.BOOLEAN && righTypeDenoter.typeKind == TypeKind.BOOLEAN) {
                return new BaseType(TypeKind.BOOLEAN, null);
            } else {
                reportTypeError(expr, "Left and Right Expressions have to both be boolean");
                return new BaseType(TypeKind.UNSUPPORTED, null);
            }
        } else if (expr.operator.kind == TokenType.Equality || expr.operator.kind == TokenType.NotEquality) {
            if (leftTypeDenoter.typeKind == righTypeDenoter.typeKind) {
                if (leftTypeDenoter.typeKind != TypeKind.CLASS && leftTypeDenoter.typeKind != TypeKind.ARRAY) {
                    return new BaseType(TypeKind.BOOLEAN, null);
                } else if (leftTypeDenoter.typeKind == TypeKind.CLASS) {
                    if (((ClassType) leftTypeDenoter).className.equals(((ClassType) righTypeDenoter).className)) {
                        return new BaseType(TypeKind.BOOLEAN, null);
                    } else {
                        return new BaseType(TypeKind.UNSUPPORTED, null);
                    }
                } else {
                    if (((ArrayType) leftTypeDenoter).eltType.typeKind != TypeKind.CLASS) {
                        if (((ArrayType) leftTypeDenoter).eltType.typeKind == ((ArrayType) righTypeDenoter).eltType.typeKind) {
                            return new BaseType(TypeKind.BOOLEAN, null);
                        }

                        return new BaseType(TypeKind.UNSUPPORTED, null);
                    } else if (((ArrayType) leftTypeDenoter).eltType.typeKind == TypeKind.CLASS) {
                        if (((ClassType) ((ArrayType) leftTypeDenoter).eltType).className.equals(((ClassType) ((ArrayType) righTypeDenoter).eltType).className)) {
                            return new BaseType(TypeKind.BOOLEAN, null);
                        }

                        return new BaseType(TypeKind.UNSUPPORTED, null);
                    } else {
                        return new BaseType(TypeKind.UNSUPPORTED, null);
                    }
                }
            } else {
                reportTypeError(expr, "Left and Right Expressions have to both be the same when checking equality");
                return new BaseType(TypeKind.UNSUPPORTED, null);
            }
        } else if (expr.operator.kind == TokenType.Comparator) {
            if (leftTypeDenoter.typeKind == TypeKind.INT && righTypeDenoter.typeKind == TypeKind.INT) {
                return new BaseType(TypeKind.BOOLEAN, null);
            } else {
                reportTypeError(expr, "Left and Right Expressions have to both be INT");
                return new BaseType(TypeKind.UNSUPPORTED, null);
            }
        } else if (expr.operator.kind == TokenType.Operator || expr.operator.kind == TokenType.Minus) {
            if (leftTypeDenoter.typeKind == TypeKind.INT && righTypeDenoter.typeKind == TypeKind.INT) {
                return new BaseType(TypeKind.INT, null);
            } else {
                reportTypeError(expr, "Left and Right Expressions have to both be INT");
                return new BaseType(TypeKind.UNSUPPORTED, null);
            }
        } else {
            return new BaseType(TypeKind.BOOLEAN, null);
        }
    }

    @Override
    public TypeDenoter visitRefExpr(RefExpr expr, Object arg) {
        this.qRefFlag = false;
        TypeDenoter temp = expr.ref.visit(this, arg);
        this.helper = null;

        return temp;
    }

    @Override
    public TypeDenoter visitIxExpr(IxExpr expr, Object arg) {
       this.qRefFlag = false;
       TypeDenoter exp = expr.ref.visit(this, arg);
       this.helper = null;

       TypeDenoter num = expr.ixExpr.visit(this, arg);

        if (exp.typeKind != TypeKind.ARRAY) {
            reportTypeError(num, "IX Expression reference must be an Array");
             return new BaseType(TypeKind.UNSUPPORTED, null);
        }
    
       if (num.typeKind != TypeKind.INT) {
        reportTypeError(num, "IX Expressions must have a INT value for size");
        return new BaseType(TypeKind.UNSUPPORTED, null);
       }

       return ((ArrayType) exp).eltType;
    }

    @Override
    public TypeDenoter visitCallExpr(CallExpr expr, Object arg) {
        // TODO
        this.qRefFlag = false;
        TypeDenoter temp = expr.functionRef.visit(this, arg);                
        this.helper = null;

        MethodDecl method = (MethodDecl) this.methodCalls;
        ParameterDeclList parameters = method.parameterDeclList;

        if (parameters.size() != expr.argList.size()) {
            reportTypeError(method, "Call Expression does not contain Right number of Parameters");
        } else {
            for (int i = 0; i < parameters.size(); i++) {
                TypeDenoter left = parameters.get(i).visit(this, arg);
                TypeDenoter right = expr.argList.get(i).visit(this, arg);

                if (left.typeKind != right.typeKind) {
                    reportTypeError(right, "Call Expression does not have matching method");
                }

                if (left.typeKind == TypeKind.CLASS
                        && !((ClassType) left).className.spelling.equals(((ClassType) right).className.spelling)) {
                    reportTypeError(right, "Call Expression does not have matching method");
                }

                if (left.typeKind == TypeKind.ARRAY) {
                    if (((ArrayType) left).eltType.typeKind != ((ArrayType) right).eltType.typeKind) {
                        reportTypeError(right, "Call Expression does not have matching method");
                    } else if (((ArrayType) left).eltType.typeKind == TypeKind.CLASS
                            && !((((ClassType) ((ArrayType) left).eltType).className.spelling)
                                    .equals(((ClassType) ((ArrayType) right).eltType).className.spelling))) {
                        reportTypeError(right, "Call Expression does not have matching method");
                    }
                }
            }
        }        
        
        return temp;
    }

    @Override
    public TypeDenoter visitLiteralExpr(LiteralExpr expr, Object arg) {
        return expr.lit.visit(this, arg);
    }

    @Override
    public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, Object arg) {
       return expr.classtype;
    }

    @Override
    public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        TypeDenoter type = expr.eltType;
        TypeDenoter sizeExpr = expr.sizeExpr.visit(this, arg);
        
        if (sizeExpr.typeKind != TypeKind.INT) {
            reportTypeError(sizeExpr, "Size Expression in new array Declaration has to be of type Int");
            return new BaseType(TypeKind.UNSUPPORTED, null);
        }
        if (type.typeKind != TypeKind.INT && type.typeKind != TypeKind.CLASS) {
            reportTypeError(sizeExpr, "Array type must be INT or CLASS");
            return new BaseType(TypeKind.UNSUPPORTED, null);
        }

        return new ArrayType(type, null);
    }

    @Override
    public TypeDenoter visitThisRef(ThisRef ref, Object arg) {
        this.helper = IDTable.get(currClass);

        return new ClassType(new Identifier(new Token(TokenType.Identifier, currClass, null)), null);
    }

    @Override
    public TypeDenoter visitIdRef(IdRef ref, Object arg) {
        String id = ref.id.spelling;

        if (this.helper == null) {
            if (this.qRefFlag && IDTable.containsKey(id)) {
                this.helper = IDTable.get(id);
                return new ClassType(ref.id, null);
            } else {
                for (Declaration d : localDeclMap) {
                    if (d.name.equals(id)) {
                        if (d.toString().equals("VarDecl")) {
                            this.helper = IDTable.get(((VarDecl) d).className);
                        } else {
                            this.helper = IDTable.get(((ParameterDecl) d).className);
                        }
                        if (d.type.typeKind == TypeKind.INT || d.type.typeKind == TypeKind.BOOLEAN) {
                            return new BaseType(d.type.typeKind, null);
                        } else if (d.type.typeKind == TypeKind.CLASS) {
                            return new ClassType(((ClassType) d.type).className, null);
                        } else if (d.type.typeKind == TypeKind.ARRAY) {
                            if (((ArrayType) (d.type)).eltType.typeKind == TypeKind.CLASS) {
                                return new ArrayType(
                                        new ClassType(((ClassType) ((ArrayType) d.type).eltType).className, null),
                                        null);
                            }
                            return new ArrayType(new BaseType(((ArrayType) (d.type)).eltType.typeKind, null), null);
                        } else {
                            return new BaseType(TypeKind.UNSUPPORTED, null);
                        }
                    }
                }

                for (Declaration d : IDTable.get(currClass)) {
                    if (d.name.equals(id)) {
                        if (d.toString().equals("FieldDecl")) {
                            this.helper = IDTable.get(((FieldDecl) d).className);
                        } else {
                            this.methodCalls = d;
                            this.helper = new Stack<>();
                        }

                        if (d.type.typeKind == TypeKind.INT || d.type.typeKind == TypeKind.BOOLEAN) {
                            return new BaseType(d.type.typeKind, null);
                        } else if (d.type.typeKind == TypeKind.CLASS) {
                            return new ClassType(((ClassType) d.type).className, null);
                        } else if (d.type.typeKind == TypeKind.ARRAY) {
                            if (((ArrayType) (d.type)).eltType.typeKind == TypeKind.CLASS) {
                                return new ArrayType(
                                        new ClassType(((ClassType) ((ArrayType) d.type).eltType).className, null),
                                        null);
                            }
                            return new ArrayType(new BaseType(((ArrayType) (d.type)).eltType.typeKind, null), null);
                        } else if (d.type.typeKind == TypeKind.VOID) {
                            return d.type;
                        } else {
                            return new BaseType(TypeKind.UNSUPPORTED, null);
                        }
                    }
                }
            }

            reportTypeError(ref, "Variable Not Found");
            return new BaseType(TypeKind.UNSUPPORTED, null);
        }
        
        for (Declaration d : this.helper) {
            if (d.name.equals(id)) {
                if (d.toString().equals("FieldDecl")) {
                    this.helper = IDTable.get(((FieldDecl) d).className);
                } else {
                    this.methodCalls = d;
                    this.helper = new Stack<>();
                }

                if (d.type.typeKind == TypeKind.INT || d.type.typeKind == TypeKind.BOOLEAN) {
                    return new BaseType(d.type.typeKind, null);
                } else if (d.type.typeKind == TypeKind.CLASS) {
                    return new ClassType(((ClassType) d.type).className, null);
                } else if (d.type.typeKind == TypeKind.CLASS) {
                    if (((ArrayType) (d.type)).eltType.typeKind == TypeKind.CLASS) {
                        return new ArrayType(
                                new ClassType(((ClassType) ((ArrayType) d.type).eltType).className, null),
                                null);
                    }
                    return new ArrayType(new BaseType(((ArrayType) (d.type)).eltType.typeKind, null), null);
                } else if (d.type.typeKind == TypeKind.VOID) {
                    return d.type;
                } else {
                    return new BaseType(TypeKind.UNSUPPORTED, null);
                }
            }
        }

        reportTypeError(ref, "Variable Not Found");
        return new BaseType(TypeKind.UNSUPPORTED, null);
    }

    @Override
    public TypeDenoter visitQRef(QualRef ref, Object arg) {
        this.qRefFlag = true;

        TypeDenoter refDenoter = ref.ref.visit(this, arg);
        TypeDenoter idDenoter = ref.id.visit(this, arg);

        if (refDenoter.typeKind != TypeKind.CLASS) {
            reportTypeError(refDenoter, "Reference must be a class type");
            return new BaseType(TypeKind.UNSUPPORTED, null);
        }

        return idDenoter;
    }

    @Override
    public TypeDenoter visitIdentifier(Identifier id, Object arg) {
        if (this.helper == null) {
             if (this.qRefFlag && IDTable.containsKey(id.spelling)) {
                 this.helper = IDTable.get(id.spelling);
                return new ClassType(id, null);
            } else {
                for (Declaration d : localDeclMap) {
                    if (d.name.equals(id.spelling)) {
                        this.helper = this.IDTable.get(((VarDecl) d).className);
                        return d.type;
                    }
                }

                for (Declaration d : IDTable.get(currClass)) {
                    if (d.name.equals(id.spelling)) {
                        if (d.toString().equals("FieldDecl")) {
                            this.helper = this.IDTable.get(((FieldDecl) d).className);
                        } else {
                            this.methodCalls = d;
                            this.helper = new Stack<>();
                        }
                        return d.type;
                    }
                }
            }
        } else {
            for (Declaration d : this.helper) {
                if (d.name.equals(id.spelling)) {
                    if (d.toString().equals("FieldDecl")) {
                        this.helper = IDTable.get(((FieldDecl) d).className);
                    } else {
                        this.methodCalls = d;
                        this.helper = new Stack<>();
                    }

                    if (d.type.typeKind == TypeKind.INT || d.type.typeKind == TypeKind.BOOLEAN) {
                        return new BaseType(d.type.typeKind, null);
                    } else if (d.type.typeKind == TypeKind.CLASS) {
                        return new ClassType(((ClassType) d.type).className, null);
                    } else if (d.type.typeKind == TypeKind.ARRAY) {
                        if (((ArrayType) (d.type)).eltType.typeKind == TypeKind.CLASS) {
                            return new ArrayType(
                                    new ClassType(((ClassType) ((ArrayType) d.type).eltType).className, null),
                                    null);
                        }
                        return new ArrayType(new BaseType(((ArrayType) (d.type)).eltType.typeKind, null), null);
                    } else if (d.type.typeKind == TypeKind.VOID) {
                        return d.type;
                    } else {
                        return new BaseType(TypeKind.UNSUPPORTED, null);
                    }
                }
            }
        }

        return new BaseType(TypeKind.UNSUPPORTED, null);
    }

    @Override
    public TypeDenoter visitOperator(Operator op, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitIntLiteral(IntLiteral num, Object arg) {
        return new BaseType(TypeKind.INT, null);
    }

    @Override
    public TypeDenoter visitBooleanLiteral(BooleanLiteral bool, Object arg) {
        return new BaseType(TypeKind.BOOLEAN, null);
    }

    @Override
    public TypeDenoter visitNullLiteral(NullLiteral bool, Object arg) {
        return new BaseType(TypeKind.NULL, null);
    }
}