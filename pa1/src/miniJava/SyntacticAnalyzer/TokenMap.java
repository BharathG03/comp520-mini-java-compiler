package miniJava.SyntacticAnalyzer;
import java.util.HashMap;
import java.util.Map;

public class TokenMap {
    private Map<String, TokenType> map;

    public TokenMap() {
        map = new HashMap<>();

        map.put("private", TokenType.Visibility);
        map.put("public", TokenType.Visibility);
        map.put("static", TokenType.Access);
        map.put("class", TokenType.Class);
        map.put("void", TokenType.Void);
        map.put("this", TokenType.This);
        map.put("return", TokenType.Return);
        map.put("new", TokenType.New);
        map.put("int", TokenType.Int);
        map.put("boolean", TokenType.Boolean);
        map.put("if", TokenType.If);
        map.put("else", TokenType.Else);
        map.put("while", TokenType.While);
        map.put("true", TokenType.Logic);
        map.put("false", TokenType.Logic);
    }

    public TokenType getTokenType(String s) {
        if (map.containsKey(s)) {
            return map.get(s);
        }

        return TokenType.Identifier;
    }
}
