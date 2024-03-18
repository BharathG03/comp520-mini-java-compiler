package miniJava.ContextualAnalysis;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class Identification implements Visitor<Object,Object> {
	private ErrorReporter _errors;
    private Map<String, ClassDecl> classDeclMap;
    private Map<String, MemberDecl> memberDeclMap;
    private Map<String, LocalDecl> localDeclMap;
    private Stack<String> localAssigns;
	
	public Identification(ErrorReporter errors) {
		this._errors = errors;
		this.classDeclMap = new HashMap<>();

	}
	
	public void parse( Package prog ) {
		try {
			visitPackage(prog,null);
		} catch( IdentificationError e ) {
			_errors.reportError(e.toString());
		}
	}
	
	class IdentificationError extends Error {
		private static final long serialVersionUID = -441346906191470192L;
		private String _errMsg;
		
		public IdentificationError(AST ast, String errMsg) {
			super();
			this._errMsg = ast.posn == null
				? "*** " + errMsg
				: "*** " + ast.posn.toString() + ": " + errMsg;
		}
		
		@Override
		public String toString() {
			return _errMsg;
		}
	}

    @Override
    public Object visitPackage(Package prog, Object arg) throws IdentificationError {
        String pfx = arg + "  . ";

        for (ClassDecl c : prog.classDeclList) {
            if (classDeclMap.containsKey(c.name)) {
                _errors.reportError("Duplication Declaration of class " + c.name);
                return null;
            }

            classDeclMap.put(c.name, c);
        }

        for (ClassDecl c : prog.classDeclList) {
            this.memberDeclMap = new HashMap<>();
            c.visit(this, pfx);
        }
        return null;
    }

    @Override
    public Object visitClassDecl(ClassDecl cd, Object arg) {
        String pfx = arg + "  . ";
        for (FieldDecl f : cd.fieldDeclList) {
            if (memberDeclMap.containsKey(f.name)) {
                _errors.reportError("Duplication Declaration of member " + f.name);
                return null;
            }

            memberDeclMap.put(f.name, f);
        }

        for (MethodDecl m : cd.methodDeclList) {
            if (memberDeclMap.containsKey(m.name)) {
                _errors.reportError("Duplication Declaration of member " + m.name);
                return null;
            }

            memberDeclMap.put(m.name, m);
        }

        for (MethodDecl m : cd.methodDeclList) {
            this.localDeclMap = new HashMap<>();
            this.localAssigns = new Stack<String>();
            m.visit(this, pfx);

            while (!this.localAssigns.empty()) {
                if (!this.localAssigns.contains(this.localAssigns.peek())) {
                    _errors.reportError("Local variable " + this.localAssigns.peek() + " cannot be found");
                }
                this.localAssigns.pop();
            }
        }

        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, Object arg) {
        return null;
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, Object arg) {
        StatementList sl = md.statementList;

        String pfx = ((String) arg) + "  . ";

        for (Statement s : sl) {
            s.visit(this, pfx);
        }
        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Object arg) {
        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl decl, Object arg) {
        return null;
    }

    @Override
    public Object visitBaseType(BaseType type, Object arg) {
        return null;
    }

    @Override
    public Object visitClassType(ClassType type, Object arg) {
        return null;
    }

    @Override
    public Object visitArrayType(ArrayType type, Object arg) {
        return null;
    }

    @Override
    public Object visitBlockStmt(BlockStmt stmt, Object arg) {
        StatementList sl = stmt.sl;
        String pfx = arg + "  . ";

        for (Statement s : sl) {
            s.visit(this, pfx);
        }
        return null;
    }

    @Override
    public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        String name = stmt.varDecl.name;

        if (localDeclMap.containsKey(name)) {
            _errors.reportError("Local variable " + name + " declared multiple times");
            return null;
        }

        localDeclMap.put(name, stmt.varDecl);
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        stmt.ref.visit(this, arg);
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        stmt.ref.visit(this, arg);
        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Object arg) {
        stmt.methodRef.visit(this, arg);
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, Object arg) {
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Object arg) {
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, Object arg) {
        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, Object arg) {
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Object arg) {
        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
        return null;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        return null;
    }

    @Override
    public Object visitThisRef(ThisRef ref, Object arg) {
        return null;
    }

    @Override
    public Object visitIdRef(IdRef ref, Object arg) {
        this.localAssigns.push(ref.id.spelling);
        return null;
    }

    @Override
    public Object visitQRef(QualRef ref, Object arg) {
        this.localAssigns.push(ref.id.spelling);
        return null;
    }

    @Override
    public Object visitIdentifier(Identifier id, Object arg) {
        return null;
    }

    @Override
    public Object visitOperator(Operator op, Object arg) {
        return null;
    }

    @Override
    public Object visitIntLiteral(IntLiteral num, Object arg) {
        return null;
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
        return null;
    }

    @Override
    public Object visitNullLiteral(NullLiteral bool, Object arg) {
        return null;
    }
}