package miniJava.ContextualAnalysis;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class Identification implements Visitor<Object,Object> {
	private ErrorReporter _errors;

    private Map<String, Map<Declaration, Map<String, Declaration>>> IDTable = new HashMap<>();
    private Map<Declaration, Map<String, Declaration>> memberDeclMap;
    private Map<String, Declaration> localDeclMap;
    private String currClass = "";
    private Map<Declaration, Map<String, Declaration>> helperMap;
    
    private Stack<String> localAssigns;
    private Map<String, Stack<Declaration>> privateValues = new HashMap<>();
    private Stack<Declaration> privates;
	
	public Identification(ErrorReporter errors) {
		this._errors = errors;
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
            this.memberDeclMap = new HashMap<>();
            this.privateValues.put(c.name, new Stack<>());

            if (IDTable.containsKey(c.name)) {
                _errors.reportError("Duplication Declaration of class " + c.name);
                return null;
            }
            IDTable.put(c.name, this.memberDeclMap);

            for (FieldDecl f : c.fieldDeclList) {
                if (containsHelper(memberDeclMap, f.name) != null) {
                    _errors.reportError("Duplication Declaration of member " + f.name);
                    return null;
                }
                
                if (f.isPrivate) {
                    this.privateValues.get(c.name).add(f);
                }

                memberDeclMap.put(f, null);
            }

            for (MethodDecl m : c.methodDeclList) {
                if (containsHelper(memberDeclMap, m.name) != null) {
                    _errors.reportError("Duplication Declaration of member " + m.name);
                    return null;
                }

                if (m.isPrivate) {
                    this.privateValues.get(c.name).add(m);
                }

                memberDeclMap.put(m, null);
            }
        }

        for (ClassDecl c : prog.classDeclList) {
            this.currClass = c.name;
            this.helperMap = null;
            c.visit(this, pfx);
        }
        return null;
    }

    @Override
    public Object visitClassDecl(ClassDecl cd, Object arg) {
        String pfx = arg + "  . ";

        for (MethodDecl m : cd.methodDeclList) {
            this.localDeclMap = new HashMap<>();
            this.localAssigns = new Stack<String>();
            m.visit(this, pfx);

            while (!this.localAssigns.empty()) {
                if (!this.localDeclMap.containsKey(this.localAssigns.peek()) && containsHelper(this.IDTable.get(cd.name), this.localAssigns.peek()) == null) {
                    _errors.reportError("Local variable " + this.localAssigns.peek() + " cannot be found");
                }
                this.localAssigns.pop();
            }

            memberDeclMap.replace(m, localDeclMap);
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

        ParameterDeclList pdl = md.parameterDeclList;
        

        for (ParameterDecl pd : pdl) {
            pd.visit(this, pfx);
        }

        for (Statement s : sl) {
            s.visit(this, pfx);
        }
        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Object arg) {
        if (localDeclMap.containsKey(pd.name)) {
            _errors.reportError("Local variable " + pd.name + " declared multiple times");
            return null;
        }

       pd.type.visit(this, indent((String) arg));

        localDeclMap.put(pd.name, null);

        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl decl, Object arg) {
        decl.type.visit(this, indent((String) arg));
        return null;
    }

    @Override
    public Object visitBaseType(BaseType type, Object arg) {
        return null;
    }

    @Override
    public Object visitClassType(ClassType type, Object arg) {
        if (!IDTable.containsKey(type.className.spelling)) {
            _errors.reportError("Object of type " + type.className.spelling + " cannot be created");
        }

        return null;
    }

    @Override
    public Object visitArrayType(ArrayType type, Object arg) {
        type.eltType.visit(this, indent((String) arg));
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
        stmt.initExp.visit(this, indent((String) arg));
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        stmt.ref.visit(this, arg + "  ");
        stmt.val.visit(this, arg + "  ");
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        stmt.ref.visit(this, indent((String) arg));
        stmt.ix.visit(this, indent((String) arg));
        stmt.exp.visit(this, indent((String) arg));
        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Object arg) {
        stmt.methodRef.visit(this, arg);

        ExprList al = stmt.argList;
        String pfx = arg + "  . ";
        for (Expression e : al) {
            e.visit(this, pfx);
        }

        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
        stmt.returnExpr.visit(this, indent((String) arg));

        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, Object arg) {
        stmt.cond.visit(this, indent((String) arg));
        stmt.thenStmt.visit(this, indent((String) arg));

        if (stmt.elseStmt != null)
            stmt.elseStmt.visit(this, indent((String) arg));

        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Object arg) {
        stmt.cond.visit(this, indent((String) arg));
        stmt.body.visit(this, indent((String) arg));

        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
        expr.expr.visit(this, indent((String) arg));
        
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
        expr.left.visit(this, indent((String) arg));
        expr.right.visit(this, indent((String) arg));

        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, Object arg) {
        expr.ref.visit(this, indent((String) arg));

        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, Object arg) {
        expr.ref.visit(this, indent((String) arg));
        expr.ixExpr.visit(this, indent((String) arg));

        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Object arg) {
        expr.functionRef.visit(this, indent((String) arg));
        ExprList al = expr.argList;
        String pfx = arg + "  . ";
        for (Expression e : al) {
            e.visit(this, pfx);
        }

        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
        expr.lit.visit(this, indent((String) arg));

        return null;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        expr.classtype.visit(this, indent((String) arg));

        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        expr.eltType.visit(this, indent((String) arg));
        expr.sizeExpr.visit(this, indent((String) arg));
        
        return null;
    }

    @Override
    public Object visitThisRef(ThisRef ref, Object arg) {
        this.helperMap = IDTable.get(this.currClass);
        this.privates = privateValues.get(this.currClass);
        return this.helperMap;
    }

    @Override
    public Object visitIdRef(IdRef ref, Object arg) {
        this.localAssigns.push(ref.id.spelling);
        return null;
    }

    @Override
    public Object visitQRef(QualRef ref, Object arg) {
        Object temp = ref.ref.visit(this, indent((String) arg));

        if (this.helperMap == null) {
            String id = this.localAssigns.pop();

            if (!IDTable.containsKey(id)) {
                _errors.reportError("Invalid Identifier Found");
                return null;
            }
            this.helperMap = IDTable.get(id);
            this.privates = privateValues.get(id);
        } 
        
        if (containsHelper(helperMap, ref.id.spelling) == null) {
            _errors.reportError("Invalid Identifier Found");
        } else if (temp == null && this.privates.contains(containsHelper(helperMap, ref.id.spelling))) {
            _errors.reportError("Invalid Identifier Found");
        } else {
            for (Declaration key : this.helperMap.keySet()) {
                if (key.name.equals(ref.id.spelling)) {
                    this.helperMap = IDTable.get(((FieldDecl) key).className);
                }
            }
        }

        return null;
    }

    @Override
    public Object visitIdentifier(Identifier id, Object arg) {
        return id.spelling;
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

    private String indent(String prefix) {
        return prefix + "  ";
    }

    private Declaration containsHelper(Map<Declaration, Map<String, Declaration>> temp, String searchKey) {
        for (Declaration key : temp.keySet()) {
            if (key.name.equals(searchKey)) {
                return key;
            }
        }

        return null;
    }
}