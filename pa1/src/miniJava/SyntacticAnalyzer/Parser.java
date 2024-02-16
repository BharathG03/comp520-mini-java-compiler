package miniJava.SyntacticAnalyzer;

import java.text.ParseException;

import miniJava.ErrorReporter;

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
	
	public void parse() {
		try {
			// The first thing we need to parse is the Program symbol
			parseProgram();
		} catch( SyntaxError e ) {}
	}
	
	// Program ::= (ClassDeclaration)* eot
	private void parseProgram() throws SyntaxError {
		// TODO: Keep parsing class declarations until eot
		while (_currentToken != null && _currentToken.getTokenType() != TokenType.EOT) {
			parseClassDeclaration();
		}
	}
	
	// ClassDeclaration ::= class identifier { (FieldDeclaration|MethodDeclaration)* }
	private void parseClassDeclaration() throws SyntaxError {
		// TODO: Take in a "class" token (check by the TokenType)
		//  What should be done if the first token isn't "class"?

		accept(TokenType.Class);
		
		// TODO: Take in an identifier token

		accept(TokenType.Identifier);
		
		// TODO: Take in a {

		accept(TokenType.LCurly);
		
		// TODO: Parse either a FieldDeclaration or MethodDeclaration
		
		while (!acceptOptional(TokenType.RCurly)) {
			parseFieldDeclaration();
		}
		
		// TODO: Take in a }
	}

	private void parseFieldDeclaration() throws SyntaxError {
		boolean isMethodDeclaration = false;

		acceptOptional(TokenType.Visibility);

		acceptOptional(TokenType.Access);

		if (acceptOptional(TokenType.Void)) {
			isMethodDeclaration = true;
		}
		else {
			parseType();
		}

		accept(TokenType.Identifier);
		
		if (isMethodDeclaration || _currentToken.getTokenType() == TokenType.LParen) {
			parseMethodDeclaration();
			return;
		}
		
		accept(TokenType.Semicolon);
	}

	private void parseMethodDeclaration() throws SyntaxError {
		accept(TokenType.LParen);

		if (!acceptOptional(TokenType.RParen)) {
			parseParameters();
			accept(TokenType.RParen);
		}

		accept(TokenType.LCurly);

		while (!acceptOptional(TokenType.RCurly)) {
				parseStatement();
		}
	}

	private void parseStatement() {
		if (acceptOptional(TokenType.Return)) {
			if (_currentToken.getTokenType() != TokenType.Semicolon) {
				parseExpression();
			}
			accept(TokenType.Semicolon);
		}
		else if (acceptOptional(TokenType.While)) {
			accept(TokenType.LParen);
			parseExpression();
			accept(TokenType.RParen);
			accept(TokenType.LCurly);

			while (!acceptOptional(TokenType.RCurly)) {
				parseStatement();
			}
		}
		else if (acceptOptional(TokenType.If)) {
			accept(TokenType.LParen);
			parseExpression();
			accept(TokenType.RParen);
			
			if (acceptOptional(TokenType.LCurly)) {
				while (!acceptOptional(TokenType.RCurly)) {
					parseStatement();
				}
			}
			else {
				parseStatement();
			}

			if (acceptOptional(TokenType.Else)) {
				if (acceptOptional(TokenType.LCurly)) {
					while (!acceptOptional(TokenType.RCurly)) {
						parseStatement();
					}
				}
				else {
					parseStatement();
				}
			}
		}
		else if (_currentToken.getTokenType() == TokenType.Identifier || _currentToken.getTokenType() == TokenType.This) {
			parseReference();

			if (acceptOptional(TokenType.Assignment)) {
				parseExpression();
			}
			else if (acceptOptional(TokenType.LBracket)) {
				if (!acceptOptional(TokenType.RBracket)) {
					parseExpression();
					accept(TokenType.RBracket);
				} 
				
				if (acceptOptional(TokenType.Identifier)) {
					accept(TokenType.Assignment);
					parseExpression();
				}
			}
			else if (acceptOptional(TokenType.LParen)) {
				if (!acceptOptional(TokenType.RParen)) {
					parseArgumentList();
					accept(TokenType.RParen);
				}
			}
			else {
				accept(TokenType.Identifier);
				accept(TokenType.Assignment);
				parseExpression();
			}

			accept(TokenType.Semicolon);
		}
		else {
			parseType();
			accept(TokenType.Identifier);
			accept(TokenType.Assignment);
			parseExpression();
			accept(TokenType.Semicolon);
		}
	}

	private void parseExpression() {
		if (acceptOptional(TokenType.New)) {
			TokenType[] param = { TokenType.Identifier, TokenType.Int };
			TokenType acceptedTokenType = acceptMultiple(param);

			if (acceptedTokenType == TokenType.Identifier) {
				if (acceptOptional(TokenType.LParen)) {
					accept(TokenType.RParen);
				}
				else {
					accept(TokenType.LBracket);
					parseExpression();
					accept(TokenType.RBracket);
				}
			}
			else {
				accept(TokenType.LBracket);
				parseExpression();
				accept(TokenType.RBracket);
			}
		}
		else if (acceptOptional(TokenType.LParen)) {
			parseExpression();
			accept(TokenType.RParen);
		}
		else if (acceptOptional(TokenType.Minus) || acceptOptional(TokenType.LogicalUnOperator)) {
			parseExpression();
		}
		else if (_currentToken.getTokenType() == TokenType.Identifier || _currentToken.getTokenType() == TokenType.This) {
			parseReference();

			if (acceptOptional(TokenType.LBracket)) {
				parseExpression();
				accept(TokenType.RBracket);
			} else if (acceptOptional(TokenType.LParen)) {
				if (!acceptOptional(TokenType.RParen)) {
					parseArgumentList();
					accept(TokenType.RParen);
				}
			}
		}
		else if (acceptOptional(TokenType.Num) || acceptOptional(TokenType.Logic)) {}
		else {
			_errors.reportError("Unexpected Token " + _currentToken.getTokenType() + " detected");
			throw new SyntaxError();
		}

		TokenType[] binop = { TokenType.Operator, TokenType.LogicalBiOperator, TokenType.Minus, TokenType.Equality,
				TokenType.NotEquality, TokenType.Comparator };
		if (acceptMultipleOptional(binop) != null) {
			parseExpression();
		}
	}

	private void parseType() {
		TokenType[] param = { TokenType.Int, TokenType.Boolean, TokenType.Identifier };

		TokenType type = acceptMultiple(param);

		if (type == TokenType.Boolean && acceptOptional(TokenType.LBracket)) {
			_errors.reportError("Boolean Array is not a valid type");
		} else if (acceptOptional(TokenType.LBracket)) {
			accept(TokenType.RBracket);
		}
	}

	private void parseParameters() throws SyntaxError {
		boolean comma = true;

		while (_currentToken.getTokenType() != TokenType.RParen) {
			if (!comma) {
				_errors.reportError("Syntax Error: Missing Comma Detected");
				throw new SyntaxError();
			}

			parseType();
			accept(TokenType.Identifier);
			comma = acceptOptional(TokenType.Comma);
		}

		if (comma) {
			_errors.reportError("Syntax Error: Unexpected Comma Detected");

			throw new SyntaxError();
		}
	}

	private void parseReference() {
		TokenType[] refTokenTypes = { TokenType.Identifier, TokenType.This };
		acceptMultiple(refTokenTypes);

		while (acceptOptional(TokenType.Dot)) {
			accept(TokenType.Identifier);
		}
	}

	private void parseArgumentList() {
		boolean comma = true;

		while (_currentToken.getTokenType() != TokenType.RParen) {
			if (!comma) {
				_errors.reportError("Syntax Error: Missing Comma Detected");
				throw new SyntaxError();
			}

			parseExpression();
			comma = acceptOptional(TokenType.Comma);
		}

		if (comma) {
			_errors.reportError("Syntax Error: Unexpected Comma Detected");

			throw new SyntaxError();
		}
	}
	
	// This method will accept the token and retrieve the next token.
	//  Can be useful if you want to error check and accept all-in-one.
	private void accept(TokenType expectedType) throws SyntaxError {
		if (_currentToken.getTokenType() == expectedType) {
			_currentToken = _scanner.scan();
			return;
		}
		
		// TODO: Report an error here.
		//  "Expected token X, but got Y"
		_errors.reportError("Syntax Error: Expected token " + expectedType + ", but got " + _currentToken.getTokenType());
		throw new SyntaxError();
	}

	private TokenType acceptMultiple(TokenType[] expectedTypes) throws SyntaxError {
		for (TokenType expectedType : expectedTypes) {
			if (_currentToken.getTokenType() == expectedType) {
				_currentToken = _scanner.scan();
				return expectedType;
			}
		}

		_errors.reportError("Syntax Error: Unexpected token " + _currentToken.getTokenType() + " detected");
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
