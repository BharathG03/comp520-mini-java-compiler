package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;
import miniJava.ErrorReporter;

public class Scanner {
	private InputStream _in;
	private ErrorReporter _errors;
	private StringBuilder _currentText;
	private char _currentChar;
	private TokenMap TokenMap;

	private boolean eot = false;

	//private int line = 1;

	private final static char eolUnix = '\n';
	private final static char eolWindows = '\r';
	private final static char tab ='\t';
	
	public Scanner( InputStream in, ErrorReporter errors ) {
		this._in = in;
		this._errors = errors;
		this.TokenMap = new TokenMap();
		
		if (!eot) {
			nextChar();
		}
	}
	
	public Token scan() {
		// TODO: This function should check the current char to determine what the token could be.
		
		// TODO: Consider what happens if the current char is whitespace
		
		// TODO: Consider what happens if there is a comment (// or /* */)
		
		// TODO: What happens if there are no more tokens?
		
		// TODO: Determine what the token is. For example, if it is a number

		if (eot) {
			return null;
		}

		ignoreSpace();

		_currentText = new StringBuilder();
		TokenType kind = scanToken();
		String spelling = _currentText.toString();
		
		return makeToken(kind, spelling);
	}

	public TokenType scanToken() {
		if (eot)
			return(TokenType.EOT); 

		TokenType type = checkTokenType();

		if (type != TokenType.None) {
			return type;
		}

		return buildToken();
	}

	private TokenType checkTokenType() {
		switch (_currentChar) {
			case '+':
				takeIt();
				return TokenType.Operator;

			case '*':
				takeIt();
				return TokenType.Operator;

			case '/':
				takeIt();

				if (!eot && (_currentChar == '/' && _currentText.charAt(_currentText.length() - 1) == '/')) {
					ignoreSingleLineComment();
				} else if (!eot && (_currentChar == '*' && _currentText.charAt(_currentText.length() - 1) == '/')) {
					ignoreMultiLineComment();
				} else {
					return TokenType.Operator;
				}

				return scanToken();

			case '-':
				takeIt();
				return TokenType.Minus;

			case '=':
				takeIt();

				if (!eot && _currentChar == '=') {
					takeIt();
					return TokenType.Equality;
				}

				return TokenType.Assignment;

			case '!':
				takeIt();

				if (!eot && _currentChar == '=') {
					takeIt();
					return TokenType.NotEquality;
				}

				return TokenType.LogicalUnOperator;

			case '&':
				takeIt();

				if (!eot && _currentChar != '&') {
					scanError("Incorrect Token");
					return TokenType.Error;
				} else {
					takeIt();
					return TokenType.LogicalBiOperator;
				}

			case '|':
				takeIt();

				if (!eot && _currentChar != '|') {
					scanError("Incorrect Token");
					return TokenType.Error;
				} else {
					takeIt();
					return TokenType.LogicalBiOperator;
				}

			case '.':
				takeIt();
				return TokenType.Dot;

			case ',':
				if (_currentText.length() == 0) {
					takeIt();
				}
				return TokenType.Comma;

			case ';':
				if (_currentText.length() == 0) {
					takeIt();
				}
				return TokenType.Semicolon;

			case '<':
				takeIt();

				if (!eot && _currentChar == '=') {
					takeIt();
				}

				return TokenType.Comparator;

			case '>':
				takeIt();

				if (!eot && _currentChar == '=') {
					takeIt();
				}

				return TokenType.Comparator;

			case '(':
				if (_currentText.length() == 0) {
					takeIt();
				}
				return TokenType.LParen;

			case ')':
				if (_currentText.length() == 0) {
					takeIt();
				}
				return TokenType.RParen;

			case '[':
				if (_currentText.length() == 0) {
					takeIt();
				}
				return TokenType.LBracket;

			case ']':
				if (_currentText.length() == 0) {
					takeIt();
				}
				return TokenType.RBracket;

			case '{':
				if (_currentText.length() == 0) {
					takeIt();
				}
				return TokenType.LCurly;

			case '}':
				if (_currentText.length() == 0) {
					takeIt();
				}
				return TokenType.RCurly;

			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				while (isDigit(_currentChar))
					takeIt();
				return TokenType.Num;

			default:
				return TokenType.None;
		}
	}
	
	private void takeIt() {
		_currentText.append(_currentChar);
		nextChar();
	}
	
	private void skipIt() {
		if (!eot) {
			nextChar();
		}
	}

	private void scanError(String m) {
		_errors.reportError("Scan Error:  " +  m);
	}

	private TokenType buildToken() {
		while (_currentChar != ' ' && _currentChar != '\n' && _currentChar != '\r' && _currentChar != '\t' && checkTokenType() == TokenType.None && !eot) {
			if (_currentChar == '/' && _currentText.charAt(_currentText.length() - 1) == '/') {
				ignoreSingleLineComment();
			}
			else if (_currentChar == '*' && _currentText.charAt(_currentText.length() - 1) == '/') {
				ignoreMultiLineComment();
			}
			takeIt();
		}

		TokenType type = this.TokenMap.getTokenType(_currentText.toString());

		if (type == TokenType.Identifier && !Character.isLetter(_currentText.charAt(0))) {
			scanError("Invalid Identifier");

			return TokenType.Error;
		}

		return type;
	}

	private void nextChar() {
		try {
			int c = _in.read();

			_currentChar = (char)c;

			if (c == -1) {
				eot = true;
			} else if (c < 0 ||c > 127) {
				throw new IOException("Lexical Error");
			}

			//checkForNewLine();
			
			// TODO: What happens if c == -1?
			
			// TODO: What happens if c is not a regular ASCII character?			
		} catch( IOException e ) {
			_errors.reportError("Scan Error: " + e);
		}
	}
	
	private Token makeToken( TokenType toktype, String text ) {
		// TODO: return a new Token with the appropriate type and text

		return new Token(toktype, text);
	}

	private void ignoreSpace() {
		while (!eot && (_currentChar == ' ' || _currentChar == eolUnix || _currentChar == eolWindows || _currentChar == tab)) {
			skipIt();
		}
	}

	private void ignoreSingleLineComment() {
		_currentText.deleteCharAt(_currentText.length() - 1);
		
		while (!eot && (_currentChar != eolUnix && _currentChar != eolWindows)) {
			skipIt();
		}
		//line += 1;

		ignoreSpace();
	}

	private void ignoreMultiLineComment() {
		int deleteStart = _currentText.length() - 1;

		skipIt();

		while (!eot && _currentChar != '/' && _currentText.charAt(_currentText.length() - 1) != '*') {
			takeIt();
		}

		skipIt();

		_currentText.delete(deleteStart, _currentText.length());

		ignoreSpace();
	}

	/*
	private void checkForNewLine() {
		if (_currentChar == eolUnix || _currentChar == eolWindows) {
			line += 1;
			//System.out.println(_currentChar);
			//System.out.println(line);
		}
	}
	*/

	private boolean isDigit(char c) {
		return (c >= '0') && (c <= '9');
	}
}
