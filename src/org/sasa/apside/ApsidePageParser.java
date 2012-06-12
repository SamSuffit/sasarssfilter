package org.sasa.apside;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.Tag;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.lexer.Page;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;

public class ApsidePageParser {

	private Parser parser;
	
	private List<Params> params = new ArrayList<Params>() ;
	
	public ApsidePageParser(InputStream content, String contentEncoding) throws Exception {
		parser = new Parser(new Lexer(new Page(content,contentEncoding)));
		parser.visitAllNodesWith(new LoginFormVisitor(params));
	}
	
	public List<Params> getParams() {
		return params;
	}

	class LoginFormVisitor extends NodeVisitor {
		
		private List<Params> params;

		public LoginFormVisitor (List<Params> params) {
			this.params = params;
		}

		 public void visitTag (Tag tag)
	     {
	         if("FORM".equals(tag.getTagName())) {
	        	 if("login".equals(tag.getAttribute("name"))) {
	        		NodeFilter filter = new TagNameFilter("input");
					NodeList list = tag.getChildren().extractAllNodesThatMatch(filter );
					try {
						
						list.visitAllNodesWith(new InputNodeVisitor(params));
					} catch (ParserException e) {
						e.printStackTrace();
					}
	        	 }
	         }
	     }
	}
	
	class InputNodeVisitor extends NodeVisitor {
		private List<Params> params;
		public InputNodeVisitor (List<Params> params) {
			this.params = params;
		}
		public void visitTag (Tag tag)
	     {
			 if("INPUT".equals(tag.getTagName())) {
				 params.add(new Params(tag.getAttribute("name"),tag.getAttribute("value")));
			 }
	     }
	}
	
	
}
