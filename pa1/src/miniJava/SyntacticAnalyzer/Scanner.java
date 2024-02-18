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

	private int line = 1;

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

				if (!eot && _currentChar == '/') {
					ignoreSingleLineComment();
				} else if (!eot && _currentChar == '*') {
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
				takeIt();
				return TokenType.Comma;

			case ';':
				takeIt();
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
				takeIt();
				return TokenType.LParen;

			case ')':
				takeIt();
				return TokenType.RParen;

			case '[':
				takeIt();
				return TokenType.LBracket;

			case ']':
				takeIt();
				return TokenType.RBracket;

			case '{':
				takeIt();
				return TokenType.LCurly;

			case '}':
				takeIt();
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
		while (!breakLoop() && !eot) {
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
			
			if (c == 10) {
				line += 1;
			}

		} catch( IOException e ) {
			_errors.reportError("Scan Error: " + e);
		}
	}
	
	private Token makeToken( TokenType toktype, String text ) {
		return new Token(toktype, text, new SourcePosition(this.line));
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

		ignoreSpace();
	}

	private void ignoreMultiLineComment() {
		int deleteStart = _currentText.length() - 1;

		skipIt();

		while (!eot && (_currentChar != '/' || _currentText.charAt(_currentText.length() - 1) != '*')) {
			takeIt();
		}

		if (eot) {
			_errors.reportError("Invalid multi-line comment");
		}

		skipIt();

		_currentText.delete(deleteStart, _currentText.length());

		ignoreSpace();
	}

	private boolean isDigit(char c) {
		return (c >= '0') && (c <= '9');
	}

	private boolean breakLoop() {
		char[] breakChars = { ' ', '\n', '\r', '\t', '+', '-', '*', '/', '<', '>', '=', '!', '(', ')', '[', ']', '{', '}', ';', ',', '.' };

		for (char c : breakChars) {
			if (c == _currentChar) {
				return true;
			}
		}

		return false;
	}
}
