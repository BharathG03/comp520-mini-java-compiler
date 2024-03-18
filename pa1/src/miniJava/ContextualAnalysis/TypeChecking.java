package miniJava.ContextualAnalysis;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class TypeChecking implements Visitor<Object, TypeDenoter> {
    private ErrorReporter _errors;

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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitPackage'");
    }

    @Override
    public TypeDenoter visitClassDecl(ClassDecl cd, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitClassDecl'");
    }

    @Override
    public TypeDenoter visitFieldDecl(FieldDecl fd, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitFieldDecl'");
    }

    @Override
    public TypeDenoter visitMethodDecl(MethodDecl md, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitMethodDecl'");
    }

    @Override
    public TypeDenoter visitParameterDecl(ParameterDecl pd, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitParameterDecl'");
    }

    @Override
    public TypeDenoter visitVarDecl(VarDecl decl, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitVarDecl'");
    }

    @Override
    public TypeDenoter visitBaseType(BaseType type, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBaseType'");
    }

    @Override
    public TypeDenoter visitClassType(ClassType type, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitClassType'");
    }

    @Override
    public TypeDenoter visitArrayType(ArrayType type, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitArrayType'");
    }

    @Override
    public TypeDenoter visitBlockStmt(BlockStmt stmt, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBlockStmt'");
    }

    @Override
    public TypeDenoter visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitVardeclStmt'");
    }

    @Override
    public TypeDenoter visitAssignStmt(AssignStmt stmt, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitAssignStmt'");
    }

    @Override
    public TypeDenoter visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitIxAssignStmt'");
    }

    @Override
    public TypeDenoter visitCallStmt(CallStmt stmt, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitCallStmt'");
    }

    @Override
    public TypeDenoter visitReturnStmt(ReturnStmt stmt, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitReturnStmt'");
    }

    @Override
    public TypeDenoter visitIfStmt(IfStmt stmt, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitIfStmt'");
    }

    @Override
    public TypeDenoter visitWhileStmt(WhileStmt stmt, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitWhileStmt'");
    }

    @Override
    public TypeDenoter visitUnaryExpr(UnaryExpr expr, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitUnaryExpr'");
    }

    @Override
    public TypeDenoter visitBinaryExpr(BinaryExpr expr, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBinaryExpr'");
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitCallExpr'");
    }

    @Override
    public TypeDenoter visitLiteralExpr(LiteralExpr expr, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitLiteralExpr'");
    }

    @Override
    public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitNewObjectExpr'");
    }

    @Override
    public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitNewArrayExpr'");
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitIdentifier'");
    }

    @Override
    public TypeDenoter visitOperator(Operator op, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitOperator'");
    }

    @Override
    public TypeDenoter visitIntLiteral(IntLiteral num, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitIntLiteral'");
    }

    @Override
    public TypeDenoter visitBooleanLiteral(BooleanLiteral bool, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBooleanLiteral'");
    }

    @Override
    public TypeDenoter visitNullLiteral(NullLiteral bool, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitNullLiteral'");
    }
}