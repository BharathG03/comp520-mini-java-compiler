package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;


public class Parser {
	private Scanner _scanner;
	private ErrorReporter _errors;
	private Token _currentToken;
	
	public Parser( Scanner scanner, ErrorReporter errors ) {
		this._scanner = scanner;
		this._errors = errors;
		this._currentToken = this._scanner.scan();
	}
	
	class SyntaxError extends Error {
		private static final long serialVersionUID = -6461942006097999362L;
	}
	
	public AST parse() {
		try {
			// The first thing we need to parse is the Program symbol
			return parseProgram();
		} catch( SyntaxError e ) {}
		return null;
	}
	
	// Program ::= (ClassDeclaration)* eot
	private Package parseProgram() throws SyntaxError {
		ClassDeclList classList = new ClassDeclList();

		while (_currentToken != null && _currentToken.getTokenType() != TokenType.EOT) {
			classList.add(parseClassDeclaration());
		}

		return new Package(classList, new SourcePosition(0));
	}
	
	// ClassDeclaration ::= class identifier { (FieldDeclaration|MethodDeclaration)* }
	private ClassDecl parseClassDeclaration() throws SyntaxError {
		accept(TokenType.Class);

		ClassDecl Class = new ClassDecl(_currentToken.getTokenText(), new FieldDeclList(), new MethodDeclList(),
				_currentToken.getTokenPosition());

		accept(TokenType.Identifier);
		accept(TokenType.LCurly);
		
		while (!acceptOptional(TokenType.RCurly)) {
			FieldDecl tempField = null;
			MethodDecl tempMethod = null;

			boolean isPrivate = false;
			TypeDenoter type = null;
			ParameterDeclList paramaters = new ParameterDeclList();
			StatementList statements = new StatementList();

			boolean isMethodDeclaration = false;

			Token optionalVisibility = _currentToken;
			if (acceptOptional(TokenType.Visibility) && optionalVisibility.getTokenText() == "private") {
				isPrivate = true;
			}

			boolean isStatic = acceptOptional(TokenType.Access);

			SourcePosition typePos = _currentToken.getTokenPosition();
			if (acceptOptional(TokenType.Void)) {
				isMethodDeclaration = true;
				type = new BaseType(TypeKind.VOID, typePos);
			} else {
				type = parseType();
			}
			
			String id = _currentToken.getTokenText();
			accept(TokenType.Identifier);

			if (isMethodDeclaration || _currentToken.getTokenType() == TokenType.LParen) {
				accept(TokenType.LParen);

				if (!acceptOptional(TokenType.RParen)) {
					paramaters = parseParameters();
					accept(TokenType.RParen);
				}

				accept(TokenType.LCurly);

				statements = parseMethodDeclaration();

				tempField = new FieldDecl(isPrivate, isStatic, type, id, _currentToken.getTokenPosition());
				tempMethod = new MethodDecl(tempField, paramaters, statements, _currentToken.getTokenPosition());

				Class.methodDeclList.add(tempMethod);
			} else {
				accept(TokenType.Semicolon);
				Class.fieldDeclList.add(tempField);
			}
		}
		
		return Class;
	}

	private StatementList parseMethodDeclaration() throws SyntaxError {
		StatementList statementList = new StatementList();

		while (!acceptOptional(TokenType.RCurly)) {
				statementList.add(parseStatement());
		}

		return statementList;
	}

	private Statement parseStatement() {
		Token curr = _currentToken;
		SourcePosition pos = _currentToken.getTokenPosition();
		Expression expression = null;

		if (acceptOptional(TokenType.Return)) {
			if (_currentToken.getTokenType() != TokenType.Semicolon) {
				expression = parseExpression();
			}
			accept(TokenType.Semicolon);
			
			return new ReturnStmt(expression, pos);
		}
		else if (acceptOptional(TokenType.While)) {
			Statement whileStmt = null;

			accept(TokenType.LParen);
			expression = parseExpression();
			accept(TokenType.RParen);
			
			if (!acceptOptional(TokenType.LCurly)) {
				whileStmt = parseStatement();
			} else {
				StatementList list = new StatementList();

				while (!acceptOptional(TokenType.RCurly)) {
					list.add(parseStatement());
				}

				whileStmt = new BlockStmt(list, pos);
			}
			
			return new WhileStmt(expression, whileStmt, pos);
		}
		else if (acceptOptional(TokenType.If)) {
			Statement ifStmt = null;
			Statement elseStmt = null;

			accept(TokenType.LParen);
			expression = parseExpression();
			accept(TokenType.RParen);
			
			if (acceptOptional(TokenType.LCurly)) {
				StatementList list = new StatementList();

				while (!acceptOptional(TokenType.RCurly)) {
					list.add(parseStatement());
				}
				ifStmt = new BlockStmt(list, pos);
			}
			else {
				ifStmt = parseStatement();
			}

			if (acceptOptional(TokenType.Else)) {
				if (acceptOptional(TokenType.LCurly)) {
					StatementList list = new StatementList();

					while (!acceptOptional(TokenType.RCurly)) {
						list.add(parseStatement());
					}

					elseStmt = new BlockStmt(list, pos);
				}
				else {
					elseStmt = parseStatement();
				}
			}
			
			return new IfStmt(expression, ifStmt, elseStmt, pos);

		}
		else if (_currentToken.getTokenType() == TokenType.Identifier || _currentToken.getTokenType() == TokenType.This) {
			Reference reference = parseReference();
			Statement statement = null;

			if (acceptOptional(TokenType.Assignment)) {
				Expression assignExpression = parseExpression();
				statement = new AssignStmt(reference, assignExpression, pos);
			}
			else if (acceptOptional(TokenType.LBracket)) {
				if (!acceptOptional(TokenType.RBracket)) {
					Expression ex1 = parseExpression();
					accept(TokenType.RBracket);
					accept(TokenType.Assignment);
					Expression ex2 = parseExpression();

					statement = new IxAssignStmt(reference, ex1, ex2, pos);
				}
				else {
					Token idToken = _currentToken;

					accept(TokenType.Identifier);
					accept(TokenType.Assignment);
					Expression valDecal = parseExpression();

					statement = new VarDeclStmt(
							new VarDecl(new ArrayType(new ClassType(new Identifier(curr),
									pos), pos), idToken.getTokenText(), pos),
							valDecal, pos);
				}
			}
			else if (acceptOptional(TokenType.LParen)) {
				if (!acceptOptional(TokenType.RParen)) {
					ExprList list = parseArgumentList();
					accept(TokenType.RParen);

					statement = new CallStmt(reference, list, pos);
				}
			}
			else {
				Token idToken = _currentToken;

				accept(TokenType.Identifier);
				accept(TokenType.Assignment);
				Expression valDecal = parseExpression();

				statement = new VarDeclStmt(new VarDecl(new ClassType(new Identifier(curr), pos), idToken.getTokenText(), pos),valDecal, pos);
			}

			accept(TokenType.Semicolon);
			return statement;
		}
		else {
			TypeDenoter type = parseType();

			Token idToken = _currentToken;
			accept(TokenType.Identifier);
			accept(TokenType.Assignment);

			Expression valDecal = parseExpression();
			accept(TokenType.Semicolon);

			return new VarDeclStmt(
					new VarDecl(type, idToken.getTokenText(), pos), valDecal, pos);
		}
	}

	private Expression parseExpression() {
		Token curr = _currentToken;
		Expression exp = null;
		
		if (acceptOptional(TokenType.New)) {
			Expression newExp = null;

			TokenType[] param = { TokenType.Identifier, TokenType.Int };
			TokenType acceptedTokenType = acceptMultiple(param);

			if (acceptedTokenType == TokenType.Identifier) {
				if (acceptOptional(TokenType.LParen)) {
					accept(TokenType.RParen);

					newExp = new NewObjectExpr(new ClassType(new Identifier(curr), curr.getTokenPosition()), curr.getTokenPosition());
				}
				else {
					accept(TokenType.LBracket);
					Expression idArrExp = parseExpression();
					accept(TokenType.RBracket);

					newExp = new NewArrayExpr(new ArrayType(new ClassType(new Identifier(curr), curr.getTokenPosition()), curr.getTokenPosition()), idArrExp, curr.getTokenPosition());
				}
			}
			else {
				accept(TokenType.LBracket);
				Expression intArrExp = parseExpression();
				accept(TokenType.RBracket);

				newExp = new NewArrayExpr(new ArrayType(new BaseType(TypeKind.ARRAY, curr.getTokenPosition()), curr.getTokenPosition()), intArrExp, curr.getTokenPosition());
			}

			exp = newExp;
		}
		else if (acceptOptional(TokenType.LParen)) {
			exp = parseExpression();
			accept(TokenType.RParen);
		}
		else if (acceptOptional(TokenType.Minus) || acceptOptional(TokenType.LogicalUnOperator)) {
			Expression unopExp = parseExpression();
			exp = new UnaryExpr(new Operator(curr), unopExp, curr.getTokenPosition());
		}
		else if (_currentToken.getTokenType() == TokenType.Identifier || _currentToken.getTokenType() == TokenType.This) {
			Reference reference = parseReference();

			if (acceptOptional(TokenType.LBracket)) {
				Expression ixExp = parseExpression();
				accept(TokenType.RBracket);

				exp = new IxExpr(reference, ixExp, curr.getTokenPosition());
			} else if (acceptOptional(TokenType.LParen)) {
				if (!acceptOptional(TokenType.RParen)) {
					ExprList argumentList = parseArgumentList();
					accept(TokenType.RParen);

					exp = new CallExpr(reference, argumentList, curr.getTokenPosition());
				}
			}
			else {
				exp = new RefExpr(reference, curr.getTokenPosition());
			}
		}
		else if (acceptOptional(TokenType.Num)) {
			exp = new LiteralExpr(new IntLiteral(curr), curr.getTokenPosition());
		}
		else if (acceptOptional(TokenType.Logic)) {
			exp = new LiteralExpr(new BooleanLiteral(curr), curr.getTokenPosition());
		}
		else {
			_errors.reportError("Unexpected Token " + _currentToken.getTokenType() + " detected on " + _currentToken.getTokenPosition().toString());
			throw new SyntaxError();
		}

		TokenType[] binop = { TokenType.Operator, TokenType.LogicalBiOperator, TokenType.Minus, TokenType.Equality,
				TokenType.NotEquality, TokenType.Comparator };

		Token binopToken = _currentToken;

		if (acceptMultipleOptional(binop) != null) {
			Expression binopExp = parseExpression();
			return new BinaryExpr(new Operator(binopToken), exp, binopExp, curr.getTokenPosition());
		}

		return exp;
	}

	private TypeDenoter parseType() {
		TokenType[] param = { TokenType.Int, TokenType.Boolean, TokenType.Identifier };
		Token typeToken = _currentToken;
		TokenType type = acceptMultiple(param);

		if (type == TokenType.Boolean && acceptOptional(TokenType.LBracket)) {
			_errors.reportError("Boolean Array is not a valid type");
			return new BaseType(TypeKind.UNSUPPORTED, typeToken.getTokenPosition());
		} else if (acceptOptional(TokenType.LBracket)) {
			accept(TokenType.RBracket);
			return new ArrayType(new BaseType(TypeKind.ARRAY, typeToken.getTokenPosition()), typeToken.getTokenPosition());
		}
		
		if (type == TokenType.Int) {
			return new BaseType(TypeKind.INT, typeToken.getTokenPosition());
		} else if (type == TokenType.Boolean) {
			return new BaseType(TypeKind.BOOLEAN, typeToken.getTokenPosition());
		}

		return new ClassType(new Identifier(typeToken), typeToken.getTokenPosition());
	}

	private ParameterDeclList parseParameters() throws SyntaxError {
		ParameterDeclList paramList = new ParameterDeclList();

		boolean comma = true;

		while (_currentToken.getTokenType() != TokenType.RParen) {
			if (!comma) {
				_errors.reportError("Syntax Error: Missing Comma Detected on " + _currentToken.getTokenPosition().toString());
				throw new SyntaxError();
			}

			ParameterDecl param = new ParameterDecl(parseType(), _currentToken.getTokenText(), _currentToken.getTokenPosition());
	
			accept(TokenType.Identifier);
			comma = acceptOptional(TokenType.Comma);

			paramList.add(param);
		}

		if (comma) {
			_errors.reportError("Syntax Error: Unexpected Comma Detected on " + _currentToken.getTokenPosition().toString());

			throw new SyntaxError();
		}

		return paramList;
	}

	private Reference parseReference() {
		Reference reference = null;
		Token curr = _currentToken;

		TokenType[] refTokenTypes = { TokenType.Identifier, TokenType.This };

		if (acceptMultiple(refTokenTypes) == TokenType.Identifier) {
			reference = new IdRef(new Identifier(curr), curr.getTokenPosition());
		}
		else {
			reference = new ThisRef(curr.getTokenPosition());
		}

		while (acceptOptional(TokenType.Dot)) {
			curr = _currentToken;
			accept(TokenType.Identifier);

			reference = new QualRef(reference, new Identifier(curr), curr.getTokenPosition());
		}

		return reference;
	}

	private ExprList parseArgumentList() {
		boolean comma = true;
		ExprList list = new ExprList();

		while (_currentToken.getTokenType() != TokenType.RParen) {
			if (!comma) {
				_errors.reportError("Syntax Error: Missing Comma Detected on " + _currentToken.getTokenPosition().toString());
				throw new SyntaxError();
			}

			list.add(parseExpression());
			comma = acceptOptional(TokenType.Comma);
		}

		if (comma) {
			_errors.reportError("Syntax Error: Unexpected Comma Detected on " + _currentToken.getTokenPosition().toString());

			throw new SyntaxError();
		}

		return list;
	}
	
	// This method will accept the token and retrieve the next token.
	//  Can be useful if you want to error check and accept all-in-one.
	private void accept(TokenType expectedType) throws SyntaxError {
		if (_currentToken.getTokenType() == expectedType) {
			_currentToken = _scanner.scan();
			return;
		}
		
		//  "Expected token X, but got Y"
		_errors.reportError("Syntax Error: Expected token " + expectedType + ", but got " + _currentToken.getTokenType() + " on " + _currentToken.getTokenPosition().toString());
		throw new SyntaxError();
	}

	private TokenType acceptMultiple(TokenType[] expectedTypes) throws SyntaxError {
		for (TokenType expectedType : expectedTypes) {
			if (_currentToken.getTokenType() == expectedType) {
				_currentToken = _scanner.scan();
				return expectedType;
			}
		}

		_errors.reportError("Syntax Error: Unexpected token " + _currentToken.getTokenType() + " detected on " + _currentToken.getTokenPosition().toString());
		throw new SyntaxError();
	}
	
	private TokenType acceptMultipleOptional(TokenType[] expectedTypes) {
		for (TokenType expectedType : expectedTypes) {
			if (_currentToken.getTokenType() == expectedType) {
				_currentToken = _scanner.scan();
				return _currentToken.getTokenType();
			}
		}

		return null;
	}

	private boolean acceptOptional(TokenType expectedType) {
		if (_currentToken.getTokenType() == expectedType) {
			_currentToken = _scanner.scan();
			return true;
		}

		return false;
	}
}
