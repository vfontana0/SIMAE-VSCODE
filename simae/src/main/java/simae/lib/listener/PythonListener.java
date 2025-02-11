package simae.lib.listener;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import simae.lib.AnotacionMarca;

import simae.grammars.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;



public class PythonListener extends Python3ParserBaseListener {

	HashMap<String, String> strings;
	
	//declarar y asignar atributo de lista de marcas
	private final List<AnotacionMarca> marcas = new ArrayList<>();
	private final String nl = System.lineSeparator();
	int ultimoSuiteLine = -1;
	int ultimoSuiteCharPosLine = -1;

	public PythonListener(Python3Parser parser, HashMap<String, String> strings) {
		this.strings = strings;
	}

	private String getOriginalCode(Token start, Token stop) {
		return getOriginalCode(start,stop,0);
	}

	private String getOriginalCode(Token start, Token stop, int adicion) {
		//Se crea un int que sera el index siguiente al stop.
		int indexNuevo = stop.getStopIndex() + adicion;
		Interval interval = new Interval(start.getStartIndex(), indexNuevo);
		String retorno = start.getInputStream().getText(interval);

		//reemplazar todos los espacios, tabulaciones
		retorno = retorno.replaceAll("//.*" + nl, "");
		retorno = retorno.replaceAll("/\\*.*\\*/", "");
		retorno = retorno.replaceAll("(\n|\r|\t| )+", " ");
		return retorno;
	}

	public List<AnotacionMarca> getMarcas() {
		return marcas;
	}

	@Override
	public void exitSuite(Python3Parser.SuiteContext ctx) {
		if(ctx.DEDENT() != null) {
			Python3Parser.StmtContext ultimoStatement = ctx.stmt(ctx.stmt().size() - 1);
			Token ultimoToken = ultimoStatement.getStop();
			if(ultimoToken.getType() != ctx.DEDENT().getSymbol().getType()) {
				Token token;
				ParseTree simpleStatementContext = ultimoStatement.simple_stmt().getChild(ultimoStatement.simple_stmt().getChildCount() - 2);
				if(simpleStatementContext instanceof ParserRuleContext) {
					token = ((ParserRuleContext)simpleStatementContext).getStop();
				}
				else {
					token = ((TerminalNode)simpleStatementContext).getSymbol();
				}
				ultimoSuiteLine = token.getLine();
				ultimoSuiteCharPosLine = token.getCharPositionInLine() + (token.getText().length() - 1);
			}
			//FIXME: asegurarse que ultimoSuiteCharPosLine sea justo antes de un token newline.
		}
	}

	@Override
	public void enterClassdef(Python3Parser.ClassdefContext ctx) {
		//'class' NAME ('(' (arglist)? ')')? ':' suite;
		String texto = strings.get("endsOn") + ctx.getStop().getLine();
		Token dosPuntos = (Token) ctx.getChild(ctx.getChildCount() - 2).getPayload();
		marcas.add(new AnotacionMarca(dosPuntos.getLine(),
				dosPuntos.getCharPositionInLine(),
				texto, "# /", "/"));
	}

	@Override
	public void exitClassdef(Python3Parser.ClassdefContext ctx) {
		//'class' NAME ('(' (arglist)? ')')? ':' suite;
		String classCompleto = getOriginalCode(ctx.getStart(), ctx.NAME().getSymbol());
		String texto = strings.get("closes") + classCompleto + " DE LINEA " + ctx.getStart().getLine();

		marcas.add(new AnotacionMarca(ultimoSuiteLine,
				ultimoSuiteCharPosLine,
				texto, "# /", "/"));
	}

	@Override
	public void enterFuncdef(Python3Parser.FuncdefContext ctx) {
		//funcdef: 'def' NAME parameters ('->' test)? ':' suite;
		String texto = strings.get("endsOn") + ctx.getStop().getLine();
		Token dosPuntos = (Token) ctx.getChild(ctx.getChildCount() - 2).getPayload();
		marcas.add(new AnotacionMarca(dosPuntos.getLine(),
				dosPuntos.getCharPositionInLine(),
				texto, "# /", "/"));
	}

	@Override
	public void exitFuncdef(Python3Parser.FuncdefContext ctx) {
		//funcdef: 'def' NAME parameters ('->' test)? ':' suite;
		String funcCompleto = getOriginalCode(ctx.getStart(), ctx.NAME().getSymbol());
		String texto = strings.get("closes") + funcCompleto + " DE LINEA " + ctx.getStart().getLine();

		marcas.add(new AnotacionMarca(ultimoSuiteLine,
				ultimoSuiteCharPosLine,
				texto, "# /", "/"));
	}


	@Override
	public void enterIf_stmt_if(Python3Parser.If_stmt_ifContext ctx) {
		//'if' test ':' suite; //agregado para implementacion simae
		String texto = strings.get("endsOn") + ctx.getStop().getLine();
		Token dosPuntos = (Token) ctx.getChild(2).getPayload();

		marcas.add(new AnotacionMarca(dosPuntos.getLine(),
				dosPuntos.getCharPositionInLine(),
				texto, "# /", "/"));
	}

	@Override
	public void exitIf_stmt_if(Python3Parser.If_stmt_ifContext ctx) {
		//'if' test ':' suite; //agregado para implementacion simae
		int linea = ctx.getStop().getLine();
		int posicionEnCaracter = ctx.getStop().getCharPositionInLine();
		String lineaCompleta = ctx.getStop().getText();

		String ifCompleto = getOriginalCode(ctx.getStart(), ctx.test().getStop());
		String texto = strings.get("closes") + ifCompleto + " DE LINEA " + ctx.getStart().getLine();

		if(ctx.suite().DEDENT() != null) {
			marcas.add(new AnotacionMarca(ultimoSuiteLine,
					ultimoSuiteCharPosLine,
					texto, "# /", "/"));
		}

	}


	@Override
	public void enterIf_stmt_elif(Python3Parser.If_stmt_elifContext ctx) {
		//'elif' test ':' suite; //agregado para implementacion simae
		String texto = strings.get("endsOn") + ctx.getStop().getLine();
		Token dosPuntos = (Token) ctx.getChild(2).getPayload();

		marcas.add(new AnotacionMarca(dosPuntos.getLine(),
				dosPuntos.getCharPositionInLine(),
				texto, "# /", "/"));
	}

	@Override
	public void exitIf_stmt_elif(Python3Parser.If_stmt_elifContext ctx) {
		//'elif' test ':' suite; //agregado para implementacion simae
		int linea = ctx.getStop().getLine();
		int posicionEnCaracter = ctx.getStop().getCharPositionInLine();
		String lineaCompleta = ctx.getStop().getText();

		String ifCompleto = getOriginalCode(ctx.getStart(), ctx.test().getStop());
		String texto = strings.get("closes") + ifCompleto + " DE LINEA " + ctx.getStart().getLine();

		if(ctx.suite().DEDENT() != null) {
			marcas.add(new AnotacionMarca(ultimoSuiteLine,
					ultimoSuiteCharPosLine,
					texto, "# /", "/"));
		}
	}

	@Override
	public void enterIf_stmt_else(Python3Parser.If_stmt_elseContext ctx) {
		//'else' ':' suite; //agregado para implementacion simae
		String texto = strings.get("endsOn") + ctx.getStop().getLine();
		Token dosPuntos = (Token) ctx.getChild(1).getPayload();

		marcas.add(new AnotacionMarca(dosPuntos.getLine(),
				dosPuntos.getCharPositionInLine(),
				texto, "# /", "/"));
	}

	@Override
	public void exitIf_stmt_else(Python3Parser.If_stmt_elseContext ctx) {
		//'else' ':' suite; //agregado para implementacion simae

		String texto = strings.get("closes") + "else" + strings.get("ofLine") + ctx.getStart().getLine();

		if(ctx.suite().DEDENT() != null) {
			marcas.add(new AnotacionMarca(ultimoSuiteLine,
					ultimoSuiteCharPosLine,
					texto, "# /", "/"));
		}
	}

	@Override
	public void enterWhile_stmt_while(Python3Parser.While_stmt_whileContext ctx) {
		//while' test ':' suite (if_stmt_else)?;
		String texto = strings.get("endsOn") + ctx.getStop().getLine();
		Token dosPuntos = (Token) ctx.getChild(2).getPayload();

		marcas.add(new AnotacionMarca(dosPuntos.getLine(),
				dosPuntos.getCharPositionInLine(),
				texto, "# /", "/"));
	}

	@Override
	public void exitWhile_stmt_while(Python3Parser.While_stmt_whileContext ctx) {
		//while' test ':' suite (if_stmt_else)?;
		String whileCompleto = getOriginalCode(ctx.getStart(), ctx.test().getStop());
		String texto = strings.get("closes") + whileCompleto + strings.get("ofLine") + ctx.getStart().getLine();

		marcas.add(new AnotacionMarca(ultimoSuiteLine,
				ultimoSuiteCharPosLine,
				texto, "# /", "/"));
	}

	@Override
	public void enterFor_stmt_for(Python3Parser.For_stmt_forContext ctx) {
		//for_stmt_for: 'for' exprlist 'in' testlist ':' suite;
		String texto = strings.get("endsOn") + ctx.getStop().getLine();
		Token dosPuntos = (Token) ctx.getChild(4).getPayload();
		marcas.add(new AnotacionMarca(dosPuntos.getLine(),
				dosPuntos.getCharPositionInLine(),
				texto, "# /", "/"));
	}

	@Override
	public void exitFor_stmt_for(Python3Parser.For_stmt_forContext ctx) {
		//for_stmt_for: 'for' exprlist 'in' testlist ':' suite;
		String forCompleto = getOriginalCode(ctx.getStart(), ctx.testlist().getStop());
		String texto = strings.get("closes") + forCompleto + strings.get("ofLine") + ctx.getStart().getLine();

		marcas.add(new AnotacionMarca(ultimoSuiteLine,
				ultimoSuiteCharPosLine,
				texto, "# /", "/"));
	}
}
