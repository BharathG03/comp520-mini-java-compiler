package miniJava.CodeGeneration;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGeneration.x64.*;
import miniJava.CodeGeneration.x64.ISA.*;
import miniJava.SyntacticAnalyzer.TokenType;

public class CodeGenerator implements Visitor<Object, Object> {
	private ErrorReporter _errors;
	private InstructionList _asm; // our list of instructions that are used to make the code section

	private Map<String, Map<String, Integer>> staticLocations = new HashMap<>();
	private Stack<Map<String, Integer>> localVariables = new Stack<>();
    private Stack<Map<String, Declaration>> localVarDecls = new Stack<>();
	private Stack<Integer> offsets = new Stack<>();

    private ClassDeclList classes = null;
    private ClassDecl helperClass = null;
	
	public CodeGenerator(ErrorReporter errors) {
		this._errors = errors;
	}

	class CodeGenerationError extends Error {
		private static final long serialVersionUID = -441346906191470192L;
		private String _errMsg;

		public CodeGenerationError(AST ast, String errMsg) {
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
	
	public void parse(Package prog) {
		_asm = new InstructionList();
		
		// If you haven't refactored the name "ModRMSIB" to something like "R",
		//  go ahead and do that now. You'll be needing that object a lot.
		// Here is some example code.
		
		// Simple operations:
		// _asm.add( new Push(0) ); // push the value zero onto the stack
		// _asm.add( new Pop(Reg64.RCX) ); // pop the top of the stack into RCX
		
		// Fancier operations:
		// _asm.add( new Cmp(new ModRMSIB(Reg64.RCX,Reg64.RDI)) ); // cmp rcx,rdi
		// _asm.add( new Cmp(new ModRMSIB(Reg64.RCX,0x10,Reg64.RDI)) ); // cmp [rcx+0x10],rdi
		// _asm.add( new Add(new ModRMSIB(Reg64.RSI,Reg64.RCX,4,0x1000,Reg64.RDX)) ); // add [rsi+rcx*4+0x1000],rdx
		
		// Thus:
		// new ModRMSIB( ... ) where the "..." can be:
		//  RegRM, RegR						== rm, r
		//  RegRM, int, RegR				== [rm+int], r
		//  RegRD, RegRI, intM, intD, RegR	== [rd+ ri*intM + intD], r
		// Where RegRM/RD/RI are just Reg64 or Reg32 or even Reg8
		//
		// Note there are constructors for ModRMSIB where RegR is skipped.
		// This is usually used by instructions that only need one register operand, and often have an immediate
		//   So they actually will set RegR for us when we create the instruction. An example is:
		// _asm.add( new Mov_rmi(new ModRMSIB(Reg64.RDX,true), 3) ); // mov rdx,3
		//   In that last example, we had to pass in a "true" to indicate whether the passed register
		//    is the operand RM or R, in this case, true means RM
		//  Similarly:
		// _asm.add( new Push(new ModRMSIB(Reg64.RBP,16)) );
		//   This one doesn't specify RegR because it is: push [rbp+16] and there is no second operand register needed
		
		// Patching example:
		// Instruction someJump = new Jmp((int)0); // 32-bit offset jump to nowhere
		// _asm.add( someJump ); // populate listIdx and startAddress for the instruction
		// ...
		// ... visit some code that probably uses _asm.add
		// ...
		// patch method 1: calculate the offset yourself
		//     _asm.patch( someJump.listIdx, new Jmp(asm.size() - someJump.startAddress - 5) );
		// -=-=-=-
		// patch method 2: let the jmp calculate the offset
		//  Note the false means that it is a 32-bit immediate for jumping (an int)
		//     _asm.patch( someJump.listIdx, new Jmp(asm.size(), someJump.startAddress, false) );
		
		prog.visit(this,null);
		
		// Output the file "a.out" if no errors
		if( !_errors.hasErrors() )
			makeElf("a.out");
	}

	@Override
	public Object visitPackage(Package prog, Object arg) {
		int staticAddress = 0;
		int numMain = 0;
		MethodDecl main = null;
        this.classes = prog.classDeclList;

		_asm.add(new Mov_rmr(new ModRMSIB(Reg64.R15, Reg64.RSP)));

		for (ClassDecl c: prog.classDeclList) {
			for (MethodDecl m: c.methodDeclList) {
				if (m.name.equals("main") && m.isPrivate == false && m.isStatic == true && m.type.typeKind == TypeKind.VOID && 
					m.parameterDeclList.size() == 1 && m.parameterDeclList.get(0).type.typeKind == TypeKind.ARRAY && 
					((ArrayType) m.parameterDeclList.get(0).type).eltType.typeKind == TypeKind.CLASS && 
					((ClassType) ((ArrayType) m.parameterDeclList.get(0).type).eltType).className.spelling.equals("String")) 
				{
					main = m;
					numMain += 1;
				}
			}

			staticLocations.put(c.name, new HashMap<>());
		}

		if (numMain != 1) {
			throw new CodeGenerationError(prog, "Program must have 1 main method");
		}

		for (ClassDecl c: prog.classDeclList) {
			for (FieldDecl f: c.fieldDeclList) {
				if (f.isStatic == true) {
					staticLocations.get(c.name).put(f.name, staticAddress);
					_asm.add(new Push(0));
					staticAddress -= 8;
				}
			}
		}

		main.visit(this, null);

		//TODO Change this exit statement
		_asm.add(new Mov_rmi(new ModRMSIB(Reg64.RAX, true), 60));
		_asm.add(new Xor(new ModRMSIB(Reg64.RDI, Reg64.RDI)));
		_asm.add(new Syscall());

		return null;
	}
	
	public void makeElf(String fname) {
		ELFMaker elf = new ELFMaker(_errors, _asm.getSize(), 8); // bss ignored until PA5, set to 8
		elf.outputELF(fname, _asm.getBytes(), 0);
	}
	
	private int makeMalloc() {
		int idxStart = _asm.add( new Mov_rmi(new ModRMSIB(Reg64.RAX,true),0x09) ); // mmap
		
		_asm.add( new Xor(		new ModRMSIB(Reg64.RDI,Reg64.RDI)) 	); // addr=0
		_asm.add( new Mov_rmi(	new ModRMSIB(Reg64.RSI,true),0x1000) ); // 4kb alloc
		_asm.add( new Mov_rmi(	new ModRMSIB(Reg64.RDX,true),0x03) 	); // prot read|write
		_asm.add( new Mov_rmi(	new ModRMSIB(Reg64.R10,true),0x22) 	); // flags= private, anonymous
		_asm.add( new Mov_rmi(	new ModRMSIB(Reg64.R8, true),-1) 	); // fd= -1
		_asm.add( new Xor(		new ModRMSIB(Reg64.R9,Reg64.R9)) 	); // offset=0
		_asm.add( new Syscall() );
		
		// pointer to newly allocated memory is in RAX
		// return the index of the first instruction in this method, if needed
		return idxStart;
	}
	
	private int makePrintln() {
		// TODO: how can we generate the assembly to println?
		_asm.add(new Mov_rmi(new ModRMSIB(Reg64.RDI, true), 1)); // Move 1 to RDI (file descriptor for stdout)
		_asm.add(new Mov_rmi(new ModRMSIB(Reg64.RDX, true), 1)); // Move 1 to RDX (length of the integer)
        //_asm.add(new Mov_rmi(new ModRMSIB(Reg64.RSI, true), 53)); // Move 1 to RDX (length of the integer)
		_asm.add(new Mov_rmi(new ModRMSIB(Reg64.RAX, true), 1)); // Move 1 to RAX (syscall number for sys_write)
		_asm.add(new Syscall()); // Make the syscall to print the integer

		return -1;
	}

	@Override
	public Object visitClassDecl(ClassDecl cd, Object arg) {
		return null;
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, Object arg) {
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg) {
		_asm.add(new Mov_rmr(new ModRMSIB(Reg64.RBP, Reg64.RSP)));
		this.localVariables.push(new HashMap<>());
        this.localVarDecls.push(new HashMap<>());
		this.offsets.push(0);

		for (Statement s: md.statementList) {
			s.visit(this, null);
		}

		_asm.add(new Mov_rmr(new ModRMSIB(Reg64.RBP, Reg64.RSP)));
		
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
		for (Statement s: stmt.sl) {
			s.visit(this, null);
		}

		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		localVariables.peek().put(stmt.varDecl.name, this.offsets.peek());
        localVarDecls.peek().put(stmt.varDecl.name, stmt.varDecl);
		this.offsets.push(this.offsets.pop() - 8);

		_asm.add(new Push(0));
		stmt.initExp.visit(this, null);

		_asm.add(new Xor(new ModRMSIB(Reg64.RAX, Reg64.RAX)));
		_asm.add(new Pop(Reg64.RAX));
		_asm.add(new Mov_rmr(new ModRMSIB(Reg64.RBP, localVariables.peek().get(stmt.varDecl.name), Reg64.RAX)));

		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        stmt.ref.visit(this, null);
        stmt.val.visit(this, null);

        _asm.add(new Pop(Reg64.RCX));
        _asm.add(new Pop(Reg64.RAX));
        _asm.add(new Mov_rmr(new ModRMSIB(Reg64.RAX, 0, Reg64.RCX))); // Value of RCX needs to be stored in location in RAX

		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
		// TODO Auto-generated method stub
        stmt.cond.visit(this, null);


		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		expr.expr.visit(this, null);
		_asm.add(new Xor(new ModRMSIB(Reg64.RAX, Reg64.RAX)));
		_asm.add(new Pop(Reg64.RAX));

		if (expr.operator.kind == TokenType.Minus) {
			_asm.add(new Xor(new ModRMSIB(Reg64.RCX, Reg64.RCX)));
			_asm.add(new Sub(new ModRMSIB(Reg64.RCX, Reg64.RAX)));
			_asm.add(new Push(Reg64.RCX));
		} else if (expr.operator.kind == TokenType.LogicalUnOperator) {
			_asm.add(new Xor(new ModRMSIB(Reg64.RAX, true), 1));
			_asm.add(new Push(Reg64.RAX));
		}

		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		expr.left.visit(this, null);
        expr.right.visit(this, null);

        _asm.add(new Pop(Reg64.RCX));
        _asm.add(new Pop(Reg64.RAX));

        if (expr.operator.spelling.equals("+")) {
            _asm.add(new Add(new ModRMSIB(Reg64.RAX, Reg64.RCX)));
            _asm.add(new Push(Reg64.RAX));
        } else if (expr.operator.spelling.equals("-")) {
            _asm.add(new Sub(new ModRMSIB(Reg64.RAX, Reg64.RCX)));
            _asm.add(new Push(Reg64.RAX));
        } else if (expr.operator.spelling.equals("/")) {
            _asm.add(new Xor(new ModRMSIB(Reg64.RDX, Reg64.RDX)));
            _asm.add(new Idiv(new ModRMSIB(Reg64.RCX, true)));
        } else if (expr.operator.spelling.equals("*")) {
            _asm.add(new Imul(Reg64.RAX, new ModRMSIB(Reg64.RCX, true)));
        } else if (expr.operator.kind == TokenType.Comparator || expr.operator.kind == TokenType.Equality || expr.operator.kind == TokenType.NotEquality) {
            _asm.add(new Cmp(new ModRMSIB(Reg64.RAX, Reg64.RCX)));
            _asm.add(new Xor(new ModRMSIB(Reg64.RAX, Reg64.RAX)));

            if (expr.operator.spelling.equals("<")) {
                _asm.add(new SetCond(Condition.LT, Reg8.AL));
            } else if (expr.operator.spelling.equals(">")) {
                _asm.add(new SetCond(Condition.GT, Reg8.AL));
            } else if (expr.operator.spelling.equals("<=")) {
                _asm.add(new SetCond(Condition.LTE, Reg8.AL));
            } else if (expr.operator.spelling.equals(">=")) {
                _asm.add(new SetCond(Condition.GTE, Reg8.AL));
            } else if (expr.operator.spelling.equals("==")) {
                _asm.add(new SetCond(Condition.E, Reg8.AL));
            } else if (expr.operator.spelling.equals("!=")) {
                _asm.add(new SetCond(Condition.NE, Reg8.AL));
            }
        } else if (expr.operator.spelling.equals("&&")) {
            _asm.add(new And(new ModRMSIB(Reg64.RAX, Reg64.RCX)));
        } else if (expr.operator.spelling.equals("||")) {
            _asm.add(new Or(new ModRMSIB(Reg64.RAX, Reg64.RCX)));
        }

        _asm.add(new Push(Reg64.RAX));

		return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, Object arg) {
        expr.ref.visit(this, null);
        return null;
	}

	@Override
	public Object visitIxExpr(IxExpr expr, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
        expr.lit.visit(this, null);
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		this.makeMalloc();
        _asm.add(new Push(Reg64.RAX));
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitIdRef(IdRef ref, Object arg) {
		// TODO Auto-generated method stub

        if (localVariables.peek().containsKey(ref.id.spelling)) {
            String typeClass = null;

            if (this.localVarDecls.peek().get(ref.id.spelling).toString().equals("VarDecl")) {
                typeClass = ((VarDecl) this.localVarDecls.peek().get(ref.id.spelling)).className;
            } else {
                typeClass = ((FieldDecl) this.localVarDecls.peek().get(ref.id.spelling)).className;
            }

            for (ClassDecl c: this.classes) {
                if (c.name.equals(typeClass)) {
                    this.helperClass = c;
                    break;
                }
            }

            _asm.add(new Mov_rmr(new ModRMSIB(Reg64.RCX, Reg64.RBP)));
            _asm.add(new Add(new ModRMSIB(Reg64.RCX, localVariables.peek().get(ref.id.spelling))));

            _asm.add(new Mov_rmr(new ModRMSIB(Reg64.RAX, Reg64.RCX))); //RAX needs to store location of RBP + offset
            _asm.add(new Push(Reg64.RAX));
        } else {

        }

		return null;
	}

	@Override
	public Object visitQRef(QualRef ref, Object arg) {
		// TODO Auto-generated method stub
        ref.ref.visit(this, null);
        ref.id.visit(this, null);
		return null;
	}

	@Override
	public Object visitIdentifier(Identifier id, Object arg) {
		// TODO Auto-generated method stub
        if (this.helperClass == null) {

        } else {
            _asm.add(new Pop(Reg64.RAX));
            
            int fieldOffset = 0;
            boolean field = false;

            for (FieldDecl f: this.helperClass.fieldDeclList) {
                if (!f.name.equals(id.spelling)) {
                    fieldOffset += 8;
                } else {
                    field = true;
                    for (ClassDecl c: this.classes) {
                        if (c.name.equals(f.className)) {
                            this.helperClass = c;
                            break;
                        }
                    }
                    break;
                }
            }

            if (!field) {
                //TODO: IT IS A METHOD KILL YOURSELF
            } else {
                _asm.add(new Mov_rmr(new ModRMSIB(Reg64.RCX, Reg64.RBP)));
                _asm.add(new Add(new ModRMSIB(Reg64.RCX, fieldOffset)));

                _asm.add(new Mov_rmr(new ModRMSIB(Reg64.RAX, Reg64.RCX))); // RAX needs to store RBP + offset
                _asm.add(new Push(Reg64.RAX));
            }
        }

		return null;
	}

	@Override
	public Object visitOperator(Operator op, Object arg) {
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, Object arg) {
		_asm.add(new Push(Integer.valueOf(num.spelling)));
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		if (bool.spelling.equals("true")) {
            _asm.add(new Push(1));
        } else {
            _asm.add(new Push(0));
        }
        
		return null;
	}

	@Override
	public Object visitNullLiteral(NullLiteral bool, Object arg) {
        // TODO: Potential Issue
		_asm.add(new Push(0));

		return null;
	}
}
