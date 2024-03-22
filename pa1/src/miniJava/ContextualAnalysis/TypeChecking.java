package miniJava.ContextualAnalysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.sound.sampled.AudioFileFormat.Type;

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
    private Declaration currMethod = null;
    private Stack<Declaration> helper = null;

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
            this.currMethod = m;
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

            if (md.type.typeKind == TypeKind.VOID && temp != null) {
                reportTypeError(md, "Wrong return type for method " + md.name);
            } else if (temp != null && temp.typeKind != md.type.typeKind) {
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

        if (left != right) {
            reportTypeError(stmt, stmt.varDecl.name + " has an invalid assignment type");
            return null;
        }

        if (left.typeKind == TypeKind.CLASS && !stmt.varDecl.className.equals(((ClassType) right).className.spelling)) {
            reportTypeError(stmt, stmt.varDecl.name + " has an invalid assignment");
        }

        return null;
    }

    @Override
    public TypeDenoter visitAssignStmt(AssignStmt stmt, Object arg) {
        TypeDenoter left = stmt.ref.visit(this, arg);
        TypeDenoter right = stmt.val.visit(this, arg);

        if (left != right) {
            reportTypeError(stmt, "Assignment statement has an invalid assignment type");
            return null;
        }

        if (left.typeKind == TypeKind.CLASS && (((ClassType) left).className.spelling).equals(((ClassType) right).className.spelling)) {
            reportTypeError(stmt, "Assignment statement has an invalid assignment");
        }
        
        return null;
    }

    @Override
    public TypeDenoter visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        TypeDenoter ref = stmt.ref.visit(this, arg);
        TypeDenoter exp1 = stmt.ix.visit(this, arg);
        TypeDenoter exp2 = stmt.exp.visit(this, arg);

        if (ref.typeKind != TypeKind.ARRAY) {
            reportTypeError(stmt, "Reference is not an Array");
        }
        if (exp1.typeKind != TypeKind.INT) {
            reportTypeError(stmt, "Expression has to be of type Integer");
        }
        if (((ArrayType) ref).eltType.typeKind != exp2.typeKind) {
            reportTypeError(stmt, "Array Type does not match Assignment");
        }
        if (((ArrayType) ref).eltType.typeKind == TypeKind.CLASS && ((ClassType)((ArrayType) ref).eltType).className.spelling.equals(((ClassType) exp2).className.spelling)) {
            reportTypeError(stmt, "Array Type does not match Assignment");
        } 
        
        return null;
    }

    @Override
    public TypeDenoter visitCallStmt(CallStmt stmt, Object arg) {
        // TypeDenoter ref = stmt.methodRef.visit(this, arg);
        // MethodDecl temp = null;
        // ExprList l1 = stmt.argList;

        // if (ref.typeKind == TypeKind.CLASS) {

        // }

        // return null;

        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitIfStmt'");
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

        if (exTypeDenoter.typeKind != TypeKind.INT) {
            reportTypeError(exTypeDenoter, "Unary Expression needs a integer expression");
        }
        return new BaseType(TypeKind.BOOLEAN, null);
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
            }
        }

        return null;
    }

    @Override
    public TypeDenoter visitRefExpr(RefExpr expr, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitRefExpr'");
    }

    @Override
    public TypeDenoter visitIxExpr(IxExpr expr, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitIxExpr'");
    }

    @Override
    public TypeDenoter visitCallExpr(CallExpr expr, Object arg) {
        // if (this.helper == null) {
        //     this.helper = IDTable.get(this.currClass);
        // }

        // Declaration temp = expr.

        return null;
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
        TypeDenoter type = expr.eltType.visit(this, arg);
        TypeDenoter sizeExpr = expr.sizeExpr.visit(this, arg);
        
        if (sizeExpr.typeKind != TypeKind.INT) {
            reportTypeError(sizeExpr, "Size Expression in new array Declaration has to be of type Int");
        }
        if (type.typeKind != TypeKind.INT && type.typeKind != TypeKind.CLASS) {
            reportTypeError(sizeExpr, "Array type must be INT or CLASS");
        }

        return type;
    }

    @Override
    public TypeDenoter visitThisRef(ThisRef ref, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitThisRef'");
    }

    @Override
    public TypeDenoter visitIdRef(IdRef ref, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitIdRef'");
    }

    @Override
    public TypeDenoter visitQRef(QualRef ref, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitQRef'");
    }

    @Override
    public TypeDenoter visitIdentifier(Identifier id, Object arg) {
        //return new ClassType(new Identifier(id.type), null);
        for (Declaration d : localDeclMap) {
            if (d.name.equals(id.spelling)) {
                return d.type;
            }
        }

        for (Declaration d: IDTable.get(currClass)) {
            if (d.name.equals(id.spelling)) {
                return d.type;
            }
        }

        return null;
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