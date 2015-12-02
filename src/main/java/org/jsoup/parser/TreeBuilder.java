package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;

/**
 * @author Jonathan Hedley
 */
abstract class TreeBuilder {
    CharacterReader reader;
    Tokeniser tokeniser;
    protected Document doc; // current doc we are building into
    protected ArrayList<Element> stack; // the stack of open elements
    protected String baseUri; // current base uri, for creating new elements
    protected Token currentToken; // currentToken is used only for error tracking.
    protected ParseErrorList errors; // null when not tracking errors

    private Token.StartTag start = new Token.StartTag(); // start tag to process
    private Token.EndTag end  = new Token.EndTag();

    protected void initialiseParse(String input, String baseUri, ParseErrorList errors) {
        Validate.notNull(input, "String input must not be null");
        Validate.notNull(baseUri, "BaseURI must not be null");

        doc = new Document(baseUri);
        doc.startPosition = 0;
        doc.endPosition = input.length();
        reader = new CharacterReader(input);
        this.errors = errors;
        tokeniser = new Tokeniser(reader, errors);
        stack = new ArrayList<Element>(32);
        this.baseUri = baseUri;
    }

    Document parse(String input, String baseUri) {
        return parse(input, baseUri, ParseErrorList.noTracking());
    }

    Document parse(String input, String baseUri, ParseErrorList errors) {
        initialiseParse(input, baseUri, errors);
        runParser();
        return doc;
    }

    protected void runParser() {
        while (true) {
            Token token = tokeniser.read();
//            System.out.print(token.tokenType() + " [" + token.startPosition + ", " + token.endPosition + "]\n");
            
            process(token);
            token.reset();

            if (token.type == Token.TokenType.EOF)
                break;
        }
    }

    protected abstract boolean process(Token token);

    protected boolean processStartTag(String name) {
        if (currentToken == start) { // don't recycle an in-use token
            return process(new Token.StartTag().name(name));
        }
        return process(start.reset().name(name));
    }

    public boolean processStartTag(String name, Attributes attrs) {
        if (currentToken == start) { // don't recycle an in-use token
            return process(new Token.StartTag().nameAttr(name, attrs));
        }
        start.reset();
        start.nameAttr(name, attrs);
        return process(start);
    }

    protected boolean processEndTag(String name) {
    	Token t;
        if (currentToken == end) { // don't recycle an in-use token
            t = new Token.EndTag().name(name);
        } else {
        	t = end.reset().name(name);
        }
        t.endPosition = reader.pos();
        return process(t);
    }


    protected Element currentElement() {
        int size = stack.size();
        return size > 0 ? stack.get(size-1) : null;
    }
}
