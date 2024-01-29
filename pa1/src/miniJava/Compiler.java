package miniJava;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

public class Compiler {
	// Main function, the file to compile will be an argument.
	public static void main(String[] args) throws FileNotFoundException {
		// TODO: Instantiate the ErrorReporter object

		ErrorReporter _errorReporter = new ErrorReporter();
		
		// TODO: Check to make sure a file path is given in args
		
		// TODO: Create the inputStream using new FileInputStream

		FileInputStream _fileInputStream = new FileInputStream(args[0]);
		
		// TODO: Instantiate the scanner with the input stream and error object

		Scanner _Scanner = new Scanner(_fileInputStream, _errorReporter);
		/*
		Token temp = _Scanner.scan();

		(while (temp != null) {
			System.out.print(temp.getTokenText() + " , ");
			System.out.println(temp.getTokenType());
			temp = _Scanner.scan();
		}
		*/
		
		// TODO: Instantiate the parser with the scanner and error object

		Parser _parser = new Parser(_Scanner, _errorReporter);
		
		// TODO: Call the parser's parse function

		_parser.parse();
		
		// TODO: Check if any errors exist, if so, println("Error")
		//  then output the errors

		if (_errorReporter.hasErrors()) {
			System.out.println("Error");
			_errorReporter.outputErrors();
		}
		else {
			System.out.println("Success");
		}
		
		// TODO: If there are no errors, println("Success")
	}
}
