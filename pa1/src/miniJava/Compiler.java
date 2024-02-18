package miniJava;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;

public class Compiler {
	// Main function, the file to compile will be an argument.
	public static void main(String[] args) throws FileNotFoundException {
		ErrorReporter _errorReporter = new ErrorReporter();

		FileInputStream _fileInputStream = new FileInputStream(args[0]);
		
		Scanner _Scanner = new Scanner(_fileInputStream, _errorReporter);
		
		Parser _parser = new Parser(_Scanner, _errorReporter);
		
		AST ast = _parser.parse();

		ASTDisplay astDisplay = new ASTDisplay();

		if (_errorReporter.hasErrors()) {
			System.out.println("Error");
			_errorReporter.outputErrors();
		}
		else {
			astDisplay.showTree(ast);
		}
	}
}
