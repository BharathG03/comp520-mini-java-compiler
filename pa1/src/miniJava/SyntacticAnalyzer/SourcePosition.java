package miniJava.SyntacticAnalyzer;

public class SourcePosition {
    int _line;

    public SourcePosition(int line) {
        _line = line;
    }

    @Override
    public String toString() {
        return "Line: " + _line;
    }
}
