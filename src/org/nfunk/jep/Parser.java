package org.nfunk.jep;

import java.io.Reader;
import java.util.Vector;

import org.nfunk.jep.function.Add;
import org.nfunk.jep.function.Comparative;
import org.nfunk.jep.function.Divide;
import org.nfunk.jep.function.Logical;
import org.nfunk.jep.function.Modulus;
import org.nfunk.jep.function.Multiply;
import org.nfunk.jep.function.Not;
import org.nfunk.jep.function.PostfixMathCommandI;
import org.nfunk.jep.function.Power;
import org.nfunk.jep.function.Subtract;
import org.nfunk.jep.function.UMinus;

public class Parser implements ParserTreeConstants, ParserConstants {

	protected JJTParserState jjtree = new JJTParserState();
	private JEP jep;

	public Node parseStream(Reader stream, JEP jep_in) throws ParseException {
		ReInit(stream);
		jep = jep_in;

		// Parse the expression, and return the
		return Start().jjtGetChild(0);
	}

	@SuppressWarnings("unchecked")
	private void addToErrorList(String errorStr) {
		jep.errorList.addElement(errorStr);
	}

	/**
	 * Translate all escape sequences to characters. Inspired by Rob Millar's unescape() method in rcm.util.Str fron the
	 * Web Sphinx project.
	 * 
	 * @param inputStr
	 *            String containing escape characters.
	 * @return String with all escape sequences replaced.
	 */
	private String replaceEscape(String inputStr) {
		int len = inputStr.length();
		int p = 0;
		int i;
		String metachars = "tnrbf\\\"'";
		String chars = "\t\n\r\b\f\\\"'";

		StringBuffer output = new StringBuffer();

		while ((i = inputStr.indexOf('\\', p)) != -1) {
			output.append(inputStr.substring(p, i));

			if (i + 1 == len)
				break;

			// find metacharacter
			char metac = inputStr.charAt(i + 1);

			// find the index of the metac
			int k = metachars.indexOf(metac);
			if (k == -1) {
				// didn't find the metachar, leave sequence as found.
				// This code should be unreachable if the parser
				// is functioning properly because strings containing
				// unknown escape characters should not be accepted.
				output.append('\\');
				output.append(metac);
			}
			else {
				// its corresponding true char
				output.append(chars.charAt(k));
			}

			// skip over both escape character & metacharacter
			p = i + 2;
		}

		// add the end of the input string to the output
		if (p < len)
			output.append(inputStr.substring(p));

		return output.toString();
	}

	// GRAMMAR START
	final public ASTStart Start() throws ParseException {
		/* @bgen(jjtree) Start */
		ASTStart jjtn000 = new ASTStart(JJTSTART);
		boolean jjtc000 = true;
		jjtree.openNodeScope(jjtn000);
		try {
			if (jj_2_1(1)) {
				Expression();
				jj_consume_token(0);
				jjtree.closeNodeScope(jjtn000, true);
				jjtc000 = false;
				return jjtn000;
			}
			else {
				switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
				case 0:
					jj_consume_token(0);
					jjtree.closeNodeScope(jjtn000, true);
					jjtc000 = false;
					throw new ParseException("No expression entered");
				default:
					jj_la1[0] = jj_gen;
					jj_consume_token(-1);
					throw new ParseException();
				}
			}
		}
		catch (Throwable jjte000) {
			if (jjtc000) {
				jjtree.clearNodeScope(jjtn000);
				jjtc000 = false;
			}
			else {
				jjtree.popNode();
			}
			if (jjte000 instanceof RuntimeException) {
				throw (RuntimeException) jjte000;
			}
			if (jjte000 instanceof ParseException) {
				throw (ParseException) jjte000;
			}
			throw (Error) jjte000;
		}
		finally {
			if (jjtc000) {
				jjtree.closeNodeScope(jjtn000, true);
			}
		}
	}

	final public void Expression() throws ParseException {
		OrExpression();
	}

	final public void OrExpression() throws ParseException {
		AndExpression();
		label_1: while (true) {
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case OR:
				break;
			default:
				jj_la1[1] = jj_gen;
				break label_1;
			}
			ASTFunNode jjtn001 = new ASTFunNode(JJTFUNNODE);
			boolean jjtc001 = true;
			jjtree.openNodeScope(jjtn001);
			try {
				jj_consume_token(OR);
				AndExpression();
				jjtree.closeNodeScope(jjtn001, 2);
				jjtc001 = false;
				jjtn001.setFunction(tokenImage[OR], new Logical(1));
			}
			catch (Throwable jjte001) {
				if (jjtc001) {
					jjtree.clearNodeScope(jjtn001);
					jjtc001 = false;
				}
				else {
					jjtree.popNode();
				}
				if (jjte001 instanceof RuntimeException) {
					throw (RuntimeException) jjte001;
				}
				if (jjte001 instanceof ParseException) {
					throw (ParseException) jjte001;
				}
				throw (Error) jjte001;
			}
			finally {
				if (jjtc001) {
					jjtree.closeNodeScope(jjtn001, 2);
				}
			}
		}
	}

	final public void AndExpression() throws ParseException {
		EqualExpression();
		label_2: while (true) {
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case AND:
				break;
			default:
				jj_la1[2] = jj_gen;
				break label_2;
			}
			ASTFunNode jjtn001 = new ASTFunNode(JJTFUNNODE);
			boolean jjtc001 = true;
			jjtree.openNodeScope(jjtn001);
			try {
				jj_consume_token(AND);
				EqualExpression();
				jjtree.closeNodeScope(jjtn001, 2);
				jjtc001 = false;
				jjtn001.setFunction(tokenImage[AND], new Logical(0));
			}
			catch (Throwable jjte001) {
				if (jjtc001) {
					jjtree.clearNodeScope(jjtn001);
					jjtc001 = false;
				}
				else {
					jjtree.popNode();
				}
				if (jjte001 instanceof RuntimeException) {
					throw (RuntimeException) jjte001;
				}
				if (jjte001 instanceof ParseException) {
					throw (ParseException) jjte001;
				}
				throw (Error) jjte001;
			}
			finally {
				if (jjtc001) {
					jjtree.closeNodeScope(jjtn001, 2);
				}
			}
		}
	}

	final public void EqualExpression() throws ParseException {
		RelationalExpression();
		label_3: while (true) {
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case EQ:
			case NE:
				break;
			default:
				jj_la1[3] = jj_gen;
				break label_3;
			}
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case NE:
				ASTFunNode jjtn001 = new ASTFunNode(JJTFUNNODE);
				boolean jjtc001 = true;
				jjtree.openNodeScope(jjtn001);
				try {
					jj_consume_token(NE);
					RelationalExpression();
					jjtree.closeNodeScope(jjtn001, 2);
					jjtc001 = false;
					jjtn001.setFunction(tokenImage[NE], new Comparative(4));
				}
				catch (Throwable jjte001) {
					if (jjtc001) {
						jjtree.clearNodeScope(jjtn001);
						jjtc001 = false;
					}
					else {
						jjtree.popNode();
					}
					if (jjte001 instanceof RuntimeException) {
						throw (RuntimeException) jjte001;
					}
					if (jjte001 instanceof ParseException) {
						throw (ParseException) jjte001;
					}
					throw (Error) jjte001;
				}
				finally {
					if (jjtc001) {
						jjtree.closeNodeScope(jjtn001, 2);
					}
				}
				break;
			case EQ:
				ASTFunNode jjtn002 = new ASTFunNode(JJTFUNNODE);
				boolean jjtc002 = true;
				jjtree.openNodeScope(jjtn002);
				try {
					jj_consume_token(EQ);
					RelationalExpression();
					jjtree.closeNodeScope(jjtn002, 2);
					jjtc002 = false;
					jjtn002.setFunction(tokenImage[EQ], new Comparative(5));
				}
				catch (Throwable jjte002) {
					if (jjtc002) {
						jjtree.clearNodeScope(jjtn002);
						jjtc002 = false;
					}
					else {
						jjtree.popNode();
					}
					if (jjte002 instanceof RuntimeException) {
						throw (RuntimeException) jjte002;
					}
					if (jjte002 instanceof ParseException) {
						throw (ParseException) jjte002;
					}
					throw (Error) jjte002;
				}
				finally {
					if (jjtc002) {
						jjtree.closeNodeScope(jjtn002, 2);
					}
				}
				break;
			default:
				jj_la1[4] = jj_gen;
				jj_consume_token(-1);
				throw new ParseException();
			}
		}
	}

	final public void RelationalExpression() throws ParseException {
		AdditiveExpression();
		label_4: while (true) {
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case GT:
			case LT:
			case LE:
			case GE:
				break;
			default:
				jj_la1[5] = jj_gen;
				break label_4;
			}
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case LT:
				ASTFunNode jjtn001 = new ASTFunNode(JJTFUNNODE);
				boolean jjtc001 = true;
				jjtree.openNodeScope(jjtn001);
				try {
					jj_consume_token(LT);
					AdditiveExpression();
					jjtree.closeNodeScope(jjtn001, 2);
					jjtc001 = false;
					jjtn001.setFunction(tokenImage[LT], new Comparative(0));
				}
				catch (Throwable jjte001) {
					if (jjtc001) {
						jjtree.clearNodeScope(jjtn001);
						jjtc001 = false;
					}
					else {
						jjtree.popNode();
					}
					if (jjte001 instanceof RuntimeException) {
						throw (RuntimeException) jjte001;
					}
					if (jjte001 instanceof ParseException) {
						throw (ParseException) jjte001;
					}
					throw (Error) jjte001;
				}
				finally {
					if (jjtc001) {
						jjtree.closeNodeScope(jjtn001, 2);
					}
				}
				break;
			case GT:
				ASTFunNode jjtn002 = new ASTFunNode(JJTFUNNODE);
				boolean jjtc002 = true;
				jjtree.openNodeScope(jjtn002);
				try {
					jj_consume_token(GT);
					AdditiveExpression();
					jjtree.closeNodeScope(jjtn002, 2);
					jjtc002 = false;
					jjtn002.setFunction(tokenImage[GT], new Comparative(1));
				}
				catch (Throwable jjte002) {
					if (jjtc002) {
						jjtree.clearNodeScope(jjtn002);
						jjtc002 = false;
					}
					else {
						jjtree.popNode();
					}
					if (jjte002 instanceof RuntimeException) {
						throw (RuntimeException) jjte002;
					}
					if (jjte002 instanceof ParseException) {
						throw (ParseException) jjte002;
					}
					throw (Error) jjte002;
				}
				finally {
					if (jjtc002) {
						jjtree.closeNodeScope(jjtn002, 2);
					}
				}
				break;
			case LE:
				ASTFunNode jjtn003 = new ASTFunNode(JJTFUNNODE);
				boolean jjtc003 = true;
				jjtree.openNodeScope(jjtn003);
				try {
					jj_consume_token(LE);
					AdditiveExpression();
					jjtree.closeNodeScope(jjtn003, 2);
					jjtc003 = false;
					jjtn003.setFunction(tokenImage[LE], new Comparative(2));
				}
				catch (Throwable jjte003) {
					if (jjtc003) {
						jjtree.clearNodeScope(jjtn003);
						jjtc003 = false;
					}
					else {
						jjtree.popNode();
					}
					if (jjte003 instanceof RuntimeException) {
						throw (RuntimeException) jjte003;
					}
					if (jjte003 instanceof ParseException) {
						throw (ParseException) jjte003;
					}
					throw (Error) jjte003;
				}
				finally {
					if (jjtc003) {
						jjtree.closeNodeScope(jjtn003, 2);
					}
				}
				break;
			case GE:
				ASTFunNode jjtn004 = new ASTFunNode(JJTFUNNODE);
				boolean jjtc004 = true;
				jjtree.openNodeScope(jjtn004);
				try {
					jj_consume_token(GE);
					AdditiveExpression();
					jjtree.closeNodeScope(jjtn004, 2);
					jjtc004 = false;
					jjtn004.setFunction(tokenImage[GE], new Comparative(3));
				}
				catch (Throwable jjte004) {
					if (jjtc004) {
						jjtree.clearNodeScope(jjtn004);
						jjtc004 = false;
					}
					else {
						jjtree.popNode();
					}
					if (jjte004 instanceof RuntimeException) {
						throw (RuntimeException) jjte004;
					}
					if (jjte004 instanceof ParseException) {
						throw (ParseException) jjte004;
					}
					throw (Error) jjte004;
				}
				finally {
					if (jjtc004) {
						jjtree.closeNodeScope(jjtn004, 2);
					}
				}
				break;
			default:
				jj_la1[6] = jj_gen;
				jj_consume_token(-1);
				throw new ParseException();
			}
		}
	}

	final public void AdditiveExpression() throws ParseException {
		MultiplicativeExpression();
		label_5: while (true) {
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case PLUS:
			case MINUS:
				break;
			default:
				jj_la1[7] = jj_gen;
				break label_5;
			}
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case PLUS:
				ASTFunNode jjtn001 = new ASTFunNode(JJTFUNNODE);
				boolean jjtc001 = true;
				jjtree.openNodeScope(jjtn001);
				try {
					jj_consume_token(PLUS);
					MultiplicativeExpression();
					jjtree.closeNodeScope(jjtn001, 2);
					jjtc001 = false;
					jjtn001.setFunction(tokenImage[PLUS], new Add());
				}
				catch (Throwable jjte001) {
					if (jjtc001) {
						jjtree.clearNodeScope(jjtn001);
						jjtc001 = false;
					}
					else {
						jjtree.popNode();
					}
					if (jjte001 instanceof RuntimeException) {
						throw (RuntimeException) jjte001;
					}
					if (jjte001 instanceof ParseException) {
						throw (ParseException) jjte001;
					}
					throw (Error) jjte001;
				}
				finally {
					if (jjtc001) {
						jjtree.closeNodeScope(jjtn001, 2);
					}
				}
				break;
			case MINUS:
				ASTFunNode jjtn002 = new ASTFunNode(JJTFUNNODE);
				boolean jjtc002 = true;
				jjtree.openNodeScope(jjtn002);
				try {
					jj_consume_token(MINUS);
					MultiplicativeExpression();
					jjtree.closeNodeScope(jjtn002, 2);
					jjtc002 = false;
					jjtn002.setFunction(tokenImage[MINUS], new Subtract());
				}
				catch (Throwable jjte002) {
					if (jjtc002) {
						jjtree.clearNodeScope(jjtn002);
						jjtc002 = false;
					}
					else {
						jjtree.popNode();
					}
					if (jjte002 instanceof RuntimeException) {
						throw (RuntimeException) jjte002;
					}
					if (jjte002 instanceof ParseException) {
						throw (ParseException) jjte002;
					}
					throw (Error) jjte002;
				}
				finally {
					if (jjtc002) {
						jjtree.closeNodeScope(jjtn002, 2);
					}
				}
				break;
			default:
				jj_la1[8] = jj_gen;
				jj_consume_token(-1);
				throw new ParseException();
			}
		}
	}

	final public void MultiplicativeExpression() throws ParseException {
		UnaryExpression();
		label_6: while (true) {
			if (!jj_2_2(1)) {
				break label_6;
			}
			if (jj_2_3(1)) {
				ASTFunNode jjtn001 = new ASTFunNode(JJTFUNNODE);
				boolean jjtc001 = true;
				jjtree.openNodeScope(jjtn001);
				try {
					PowerExpression();
					jjtree.closeNodeScope(jjtn001, 2);
					jjtc001 = false;
					if (!jep.implicitMul) {
						throw new ParseException("Syntax Error (implicit multiplication not enabled)");
					}

					jjtn001.setFunction(tokenImage[MUL], new Multiply());
				}
				catch (Throwable jjte001) {
					if (jjtc001) {
						jjtree.clearNodeScope(jjtn001);
						jjtc001 = false;
					}
					else {
						jjtree.popNode();
					}
					if (jjte001 instanceof RuntimeException) {
						throw (RuntimeException) jjte001;
					}
					if (jjte001 instanceof ParseException) {
						throw (ParseException) jjte001;
					}
					throw (Error) jjte001;
				}
				finally {
					if (jjtc001) {
						jjtree.closeNodeScope(jjtn001, 2);
					}
				}
			}
			else {
				switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
				case MUL:
					ASTFunNode jjtn002 = new ASTFunNode(JJTFUNNODE);
					boolean jjtc002 = true;
					jjtree.openNodeScope(jjtn002);
					try {
						jj_consume_token(MUL);
						UnaryExpression();
						jjtree.closeNodeScope(jjtn002, 2);
						jjtc002 = false;
						jjtn002.setFunction(tokenImage[MUL], new Multiply());
					}
					catch (Throwable jjte002) {
						if (jjtc002) {
							jjtree.clearNodeScope(jjtn002);
							jjtc002 = false;
						}
						else {
							jjtree.popNode();
						}
						if (jjte002 instanceof RuntimeException) {
							throw (RuntimeException) jjte002;
						}
						if (jjte002 instanceof ParseException) {
							throw (ParseException) jjte002;
						}
						throw (Error) jjte002;
					}
					finally {
						if (jjtc002) {
							jjtree.closeNodeScope(jjtn002, 2);
						}
					}
					break;
				case DIV:
					ASTFunNode jjtn003 = new ASTFunNode(JJTFUNNODE);
					boolean jjtc003 = true;
					jjtree.openNodeScope(jjtn003);
					try {
						jj_consume_token(DIV);
						UnaryExpression();
						jjtree.closeNodeScope(jjtn003, 2);
						jjtc003 = false;
						jjtn003.setFunction(tokenImage[DIV], new Divide());
					}
					catch (Throwable jjte003) {
						if (jjtc003) {
							jjtree.clearNodeScope(jjtn003);
							jjtc003 = false;
						}
						else {
							jjtree.popNode();
						}
						if (jjte003 instanceof RuntimeException) {
							throw (RuntimeException) jjte003;
						}
						if (jjte003 instanceof ParseException) {
							throw (ParseException) jjte003;
						}
						throw (Error) jjte003;
					}
					finally {
						if (jjtc003) {
							jjtree.closeNodeScope(jjtn003, 2);
						}
					}
					break;
				case MOD:
					ASTFunNode jjtn004 = new ASTFunNode(JJTFUNNODE);
					boolean jjtc004 = true;
					jjtree.openNodeScope(jjtn004);
					try {
						jj_consume_token(MOD);
						UnaryExpression();
						jjtree.closeNodeScope(jjtn004, 2);
						jjtc004 = false;
						jjtn004.setFunction(tokenImage[MOD], new Modulus());
					}
					catch (Throwable jjte004) {
						if (jjtc004) {
							jjtree.clearNodeScope(jjtn004);
							jjtc004 = false;
						}
						else {
							jjtree.popNode();
						}
						if (jjte004 instanceof RuntimeException) {
							throw (RuntimeException) jjte004;
						}
						if (jjte004 instanceof ParseException) {
							throw (ParseException) jjte004;
						}
						throw (Error) jjte004;
					}
					finally {
						if (jjtc004) {
							jjtree.closeNodeScope(jjtn004, 2);
						}
					}
					break;
				default:
					jj_la1[9] = jj_gen;
					jj_consume_token(-1);
					throw new ParseException();
				}
			}
		}
	}

	final public void UnaryExpression() throws ParseException {
		switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
		case PLUS:
			jj_consume_token(PLUS);
			UnaryExpression();
			break;
		case MINUS:
			ASTFunNode jjtn001 = new ASTFunNode(JJTFUNNODE);
			boolean jjtc001 = true;
			jjtree.openNodeScope(jjtn001);
			try {
				jj_consume_token(MINUS);
				UnaryExpression();
				jjtree.closeNodeScope(jjtn001, 1);
				jjtc001 = false;
				jjtn001.setFunction(tokenImage[MINUS], new UMinus());
			}
			catch (Throwable jjte001) {
				if (jjtc001) {
					jjtree.clearNodeScope(jjtn001);
					jjtc001 = false;
				}
				else {
					jjtree.popNode();
				}
				if (jjte001 instanceof RuntimeException) {
					throw (RuntimeException) jjte001;
				}
				if (jjte001 instanceof ParseException) {
					throw (ParseException) jjte001;
				}
				throw (Error) jjte001;
			}
			finally {
				if (jjtc001) {
					jjtree.closeNodeScope(jjtn001, 1);
				}
			}
			break;
		case NOT:
			ASTFunNode jjtn002 = new ASTFunNode(JJTFUNNODE);
			boolean jjtc002 = true;
			jjtree.openNodeScope(jjtn002);
			try {
				jj_consume_token(NOT);
				UnaryExpression();
				jjtree.closeNodeScope(jjtn002, 1);
				jjtc002 = false;
				jjtn002.setFunction(tokenImage[NOT], new Not());
			}
			catch (Throwable jjte002) {
				if (jjtc002) {
					jjtree.clearNodeScope(jjtn002);
					jjtc002 = false;
				}
				else {
					jjtree.popNode();
				}
				if (jjte002 instanceof RuntimeException) {
					throw (RuntimeException) jjte002;
				}
				if (jjte002 instanceof ParseException) {
					throw (ParseException) jjte002;
				}
				throw (Error) jjte002;
			}
			finally {
				if (jjtc002) {
					jjtree.closeNodeScope(jjtn002, 1);
				}
			}
			break;
		default:
			jj_la1[10] = jj_gen;
			if (jj_2_4(1)) {
				PowerExpression();
			}
			else {
				jj_consume_token(-1);
				throw new ParseException();
			}
		}
	}

	final public void PowerExpression() throws ParseException {
		UnaryExpressionNotPlusMinus();
		switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
		case POWER:
			ASTFunNode jjtn001 = new ASTFunNode(JJTFUNNODE);
			boolean jjtc001 = true;
			jjtree.openNodeScope(jjtn001);
			try {
				jj_consume_token(POWER);
				UnaryExpression();
				jjtree.closeNodeScope(jjtn001, 2);
				jjtc001 = false;
				jjtn001.setFunction(tokenImage[POWER], new Power());
			}
			catch (Throwable jjte001) {
				if (jjtc001) {
					jjtree.clearNodeScope(jjtn001);
					jjtc001 = false;
				}
				else {
					jjtree.popNode();
				}
				if (jjte001 instanceof RuntimeException) {
					throw (RuntimeException) jjte001;
				}
				if (jjte001 instanceof ParseException) {
					throw (ParseException) jjte001;
				}
				throw (Error) jjte001;
			}
			finally {
				if (jjtc001) {
					jjtree.closeNodeScope(jjtn001, 2);
				}
			}
			break;
		default:
			jj_la1[11] = jj_gen;
		}
	}

	final public void UnaryExpressionNotPlusMinus() throws ParseException {
		switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
		case INTEGER_LITERAL:
		case FLOATING_POINT_LITERAL:
		case STRING_LITERAL:
		case 31:
			AnyConstant();
			break;
		default:
			jj_la1[13] = jj_gen;
			if (jj_2_5(1)) {
				if (getToken(1).kind == IDENTIFIER && jep.funTab.containsKey(getToken(1).image)) {
					Function();
				}
				else {
					switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
					case IDENTIFIER:
						Variable();
						break;
					default:
						jj_la1[12] = jj_gen;
						jj_consume_token(-1);
						throw new ParseException();
					}
				}
			}
			else {
				switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
				case 28:
					jj_consume_token(28);
					Expression();
					jj_consume_token(29);
					break;
				default:
					jj_la1[14] = jj_gen;
					jj_consume_token(-1);
					throw new ParseException();
				}
			}
		}
	}

	final public void Variable() throws ParseException {
		String identString = "";
		ASTVarNode jjtn001 = new ASTVarNode(JJTVARNODE);
		boolean jjtc001 = true;
		jjtree.openNodeScope(jjtn001);
		try {
			identString = Identifier();
			jjtree.closeNodeScope(jjtn001, true);
			jjtc001 = false;
			if (jep.symTab.containsKey(identString)) {
				jjtn001.setName(identString);
			}
			else {
				if (jep.allowUndeclared) {
					jep.symTab.put(identString, new Double(0));
					jjtn001.setName(identString);
				}
				else {
					addToErrorList("Unrecognized symbol \"" + identString + "\"");
				}
			}
		}
		catch (Throwable jjte001) {
			if (jjtc001) {
				jjtree.clearNodeScope(jjtn001);
				jjtc001 = false;
			}
			else {
				jjtree.popNode();
			}
			if (jjte001 instanceof RuntimeException) {
				throw (RuntimeException) jjte001;
			}
			if (jjte001 instanceof ParseException) {
				throw (ParseException) jjte001;
			}
			throw (Error) jjte001;
		}
		finally {
			if (jjtc001) {
				jjtree.closeNodeScope(jjtn001, true);
			}
		}
	}

	final public void Function() throws ParseException {
		int reqArguments = 0;
		String identString = "";
		ASTFunNode jjtn001 = new ASTFunNode(JJTFUNNODE);
		boolean jjtc001 = true;
		jjtree.openNodeScope(jjtn001);
		try {
			identString = Identifier();
			if (jep.funTab.containsKey(identString)) {
				// Set number of required arguments
				reqArguments = ((PostfixMathCommandI) jep.funTab.get(identString)).getNumberOfParameters();
				jjtn001.setFunction(identString, (PostfixMathCommandI) jep.funTab.get(identString));
			}
			else {
				addToErrorList("!!! Unrecognized function \"" + identString + "\"");
			}
			jj_consume_token(28);
			ArgumentList(reqArguments, identString);
			jj_consume_token(29);
		}
		catch (Throwable jjte001) {
			if (jjtc001) {
				jjtree.clearNodeScope(jjtn001);
				jjtc001 = false;
			}
			else {
				jjtree.popNode();
			}
			if (jjte001 instanceof RuntimeException) {
				throw (RuntimeException) jjte001;
			}
			if (jjte001 instanceof ParseException) {
				throw (ParseException) jjte001;
			}
			throw (Error) jjte001;
		}
		finally {
			if (jjtc001) {
				jjtree.closeNodeScope(jjtn001, true);
			}
		}
	}

	final public void ArgumentList(int reqArguments, String functionName) throws ParseException {
		int count = 0;
		String errorStr = "";
		if (jj_2_6(1)) {
			Expression();
			count++;
			label_7: while (true) {
				switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
				case 30:
					break;
				default:
					jj_la1[15] = jj_gen;
					break label_7;
				}
				jj_consume_token(30);
				Expression();
				count++;
			}
		}
		if (reqArguments != count && reqArguments != -1) {
			errorStr = "Function \"" + functionName + "\" requires " + reqArguments + " parameter";
			if (reqArguments != 1)
				errorStr += "s";
			addToErrorList(errorStr);
		}
	}

	final public String Identifier() throws ParseException {
		Token t;
		t = jj_consume_token(IDENTIFIER);
		return t.image;
	}

	final public void AnyConstant() throws ParseException {
		/* @bgen(jjtree) Constant */
		ASTConstant jjtn000 = new ASTConstant(JJTCONSTANT);
		boolean jjtc000 = true;
		jjtree.openNodeScope(jjtn000);
		Token t;
		Object value;
		try {
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case STRING_LITERAL:
				t = jj_consume_token(STRING_LITERAL);
				jjtree.closeNodeScope(jjtn000, true);
				jjtc000 = false;
				// strip away double quotes at end of string
				String temp = (t.image).substring(1, t.image.length() - 1);

				// replace escape characters
				temp = replaceEscape(temp);

				jjtn000.setValue(temp);
				break;
			case INTEGER_LITERAL:
			case FLOATING_POINT_LITERAL:
				value = RealConstant();
				jjtree.closeNodeScope(jjtn000, true);
				jjtc000 = false;
				jjtn000.setValue(value);
				break;
			case 31:
				value = Array();
				jjtree.closeNodeScope(jjtn000, true);
				jjtc000 = false;
				jjtn000.setValue(value);
				break;
			default:
				jj_la1[16] = jj_gen;
				jj_consume_token(-1);
				throw new ParseException();
			}
		}
		catch (Throwable jjte000) {
			if (jjtc000) {
				jjtree.clearNodeScope(jjtn000);
				jjtc000 = false;
			}
			else {
				jjtree.popNode();
			}
			if (jjte000 instanceof RuntimeException) {
				throw (RuntimeException) jjte000;
			}
			if (jjte000 instanceof ParseException) {
				throw (ParseException) jjte000;
			}
			throw (Error) jjte000;
		}
		finally {
			if (jjtc000) {
				jjtree.closeNodeScope(jjtn000, true);
			}
		}
	}

	@SuppressWarnings("unchecked")
	final public Vector Array() throws ParseException {
		Object value;
		Vector result = new Vector();
		jj_consume_token(31);
		value = RealConstant();
		result.addElement(value);
		label_8: while (true) {
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case 30:
				break;
			default:
				jj_la1[17] = jj_gen;
				break label_8;
			}
			jj_consume_token(30);
			value = RealConstant();
			result.addElement(value);
		}
		jj_consume_token(32);
		return result;
	}

	final public Object RealConstant() throws ParseException {
		Token t;
		Object value;
		switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
		case INTEGER_LITERAL:
			t = jj_consume_token(INTEGER_LITERAL);
			break;
		case FLOATING_POINT_LITERAL:
			t = jj_consume_token(FLOATING_POINT_LITERAL);
			break;
		default:
			jj_la1[18] = jj_gen;
			jj_consume_token(-1);
			throw new ParseException();
		}
		try {
			Double temp = new Double(t.image);
			value = jep.getNumberFactory().createNumber(temp.doubleValue());
		}
		catch (Exception e) {
			value = null;
			addToErrorList("Can't parse \"" + t.image + "\"");
		}
		return value;
	}

	final private boolean jj_2_1(int xla) {
		jj_la = xla;
		jj_lastpos = jj_scanpos = token;
		boolean retval = !jj_3_1();
		jj_save(0, xla);
		return retval;
	}

	final private boolean jj_2_2(int xla) {
		jj_la = xla;
		jj_lastpos = jj_scanpos = token;
		boolean retval = !jj_3_2();
		jj_save(1, xla);
		return retval;
	}

	final private boolean jj_2_3(int xla) {
		jj_la = xla;
		jj_lastpos = jj_scanpos = token;
		boolean retval = !jj_3_3();
		jj_save(2, xla);
		return retval;
	}

	final private boolean jj_2_4(int xla) {
		jj_la = xla;
		jj_lastpos = jj_scanpos = token;
		boolean retval = !jj_3_4();
		jj_save(3, xla);
		return retval;
	}

	final private boolean jj_2_5(int xla) {
		jj_la = xla;
		jj_lastpos = jj_scanpos = token;
		boolean retval = !jj_3_5();
		jj_save(4, xla);
		return retval;
	}

	final private boolean jj_2_6(int xla) {
		jj_la = xla;
		jj_lastpos = jj_scanpos = token;
		boolean retval = !jj_3_6();
		jj_save(5, xla);
		return retval;
	}

	final private boolean jj_3R_15() {
		if (jj_3R_19())
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_33() {
		if (jj_3R_36())
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_22() {
		if (jj_scan_token(28))
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_25() {
		Token xsp;
		xsp = jj_scanpos;
		if (jj_3R_27()) {
			jj_scanpos = xsp;
			if (jj_3R_28()) {
				jj_scanpos = xsp;
				if (jj_3R_29())
					return true;
				if (jj_la == 0 && jj_scanpos == jj_lastpos)
					return false;
			}
			else if (jj_la == 0 && jj_scanpos == jj_lastpos)
				return false;
		}
		else if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_27() {
		if (jj_scan_token(STRING_LITERAL))
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_16() {
		if (jj_3R_20())
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_14() {
		if (jj_3R_18())
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3_5() {
		Token xsp;
		xsp = jj_scanpos;
		lookingAhead = true;
		jj_semLA = getToken(1).kind == IDENTIFIER && jep.funTab.containsKey(getToken(1).image);
		lookingAhead = false;
		if (!jj_semLA || jj_3R_14()) {
			jj_scanpos = xsp;
			if (jj_3R_15())
				return true;
			if (jj_la == 0 && jj_scanpos == jj_lastpos)
				return false;
		}
		else if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_17() {
		Token xsp;
		xsp = jj_scanpos;
		if (jj_3R_21()) {
			jj_scanpos = xsp;
			if (jj_3_5()) {
				jj_scanpos = xsp;
				if (jj_3R_22())
					return true;
				if (jj_la == 0 && jj_scanpos == jj_lastpos)
					return false;
			}
			else if (jj_la == 0 && jj_scanpos == jj_lastpos)
				return false;
		}
		else if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_21() {
		if (jj_3R_25())
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_9() {
		if (jj_3R_16())
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_23() {
		if (jj_scan_token(IDENTIFIER))
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_30() {
		if (jj_3R_33())
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3_1() {
		if (jj_3R_9())
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_13() {
		if (jj_3R_17())
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3_6() {
		if (jj_3R_9())
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3_4() {
		if (jj_3R_13())
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_35() {
		if (jj_scan_token(FLOATING_POINT_LITERAL))
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_34() {
		if (jj_scan_token(INTEGER_LITERAL))
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_31() {
		Token xsp;
		xsp = jj_scanpos;
		if (jj_3R_34()) {
			jj_scanpos = xsp;
			if (jj_3R_35())
				return true;
			if (jj_la == 0 && jj_scanpos == jj_lastpos)
				return false;
		}
		else if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_39() {
		if (jj_scan_token(NOT))
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_26() {
		if (jj_3R_30())
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_38() {
		if (jj_scan_token(MINUS))
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_36() {
		Token xsp;
		xsp = jj_scanpos;
		if (jj_3R_37()) {
			jj_scanpos = xsp;
			if (jj_3R_38()) {
				jj_scanpos = xsp;
				if (jj_3R_39()) {
					jj_scanpos = xsp;
					if (jj_3_4())
						return true;
					if (jj_la == 0 && jj_scanpos == jj_lastpos)
						return false;
				}
				else if (jj_la == 0 && jj_scanpos == jj_lastpos)
					return false;
			}
			else if (jj_la == 0 && jj_scanpos == jj_lastpos)
				return false;
		}
		else if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_37() {
		if (jj_scan_token(PLUS))
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_18() {
		if (jj_3R_23())
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_12() {
		if (jj_scan_token(MOD))
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_24() {
		if (jj_3R_26())
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_32() {
		if (jj_scan_token(31))
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_11() {
		if (jj_scan_token(DIV))
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_10() {
		if (jj_scan_token(MUL))
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_20() {
		if (jj_3R_24())
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_29() {
		if (jj_3R_32())
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_19() {
		if (jj_3R_23())
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3R_28() {
		if (jj_3R_31())
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3_2() {
		Token xsp;
		xsp = jj_scanpos;
		if (jj_3_3()) {
			jj_scanpos = xsp;
			if (jj_3R_10()) {
				jj_scanpos = xsp;
				if (jj_3R_11()) {
					jj_scanpos = xsp;
					if (jj_3R_12())
						return true;
					if (jj_la == 0 && jj_scanpos == jj_lastpos)
						return false;
				}
				else if (jj_la == 0 && jj_scanpos == jj_lastpos)
					return false;
			}
			else if (jj_la == 0 && jj_scanpos == jj_lastpos)
				return false;
		}
		else if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	final private boolean jj_3_3() {
		if (jj_3R_13())
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			return false;
		return false;
	}

	public ParserTokenManager token_source;
	JavaCharStream jj_input_stream;
	public Token token, jj_nt;
	private int jj_ntk;
	private Token jj_scanpos, jj_lastpos;
	private int jj_la;
	public boolean lookingAhead = false;
	private boolean jj_semLA;
	private int jj_gen;
	final private int[] jj_la1 = new int[19];
	final private int[] jj_la1_0 = { 0x1, 0x100000, 0x80000, 0x48000, 0x48000, 0x36000, 0x36000, 0x600000, 0x600000,
			0x3800000, 0x4600000, 0x8000000, 0x400, 0x800002a0, 0x10000000, 0x40000000, 0x800002a0, 0x40000000, 0xa0, };
	final private int[] jj_la1_1 = { 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
			0x0, 0x0, 0x0, };
	final private JJCalls[] jj_2_rtns = new JJCalls[6];
	private boolean jj_rescan = false;
	private int jj_gc = 0;

	public Parser(java.io.InputStream stream) {
		jj_input_stream = new JavaCharStream(stream, 1, 1);
		token_source = new ParserTokenManager(jj_input_stream);
		token = new Token();
		jj_ntk = -1;
		jj_gen = 0;
		for (int i = 0; i < 19; i++)
			jj_la1[i] = -1;
		for (int i = 0; i < jj_2_rtns.length; i++)
			jj_2_rtns[i] = new JJCalls();
	}

	public void ReInit(java.io.InputStream stream) {
		jj_input_stream.ReInit(stream, 1, 1);
		token_source.ReInit(jj_input_stream);
		token = new Token();
		jj_ntk = -1;
		jjtree.reset();
		jj_gen = 0;
		for (int i = 0; i < 19; i++)
			jj_la1[i] = -1;
		for (int i = 0; i < jj_2_rtns.length; i++)
			jj_2_rtns[i] = new JJCalls();
	}

	public Parser(java.io.Reader stream) {
		jj_input_stream = new JavaCharStream(stream, 1, 1);
		token_source = new ParserTokenManager(jj_input_stream);
		token = new Token();
		jj_ntk = -1;
		jj_gen = 0;
		for (int i = 0; i < 19; i++)
			jj_la1[i] = -1;
		for (int i = 0; i < jj_2_rtns.length; i++)
			jj_2_rtns[i] = new JJCalls();
	}

	public void ReInit(java.io.Reader stream) {
		jj_input_stream.ReInit(stream, 1, 1);
		token_source.ReInit(jj_input_stream);
		token = new Token();
		jj_ntk = -1;
		jjtree.reset();
		jj_gen = 0;
		for (int i = 0; i < 19; i++)
			jj_la1[i] = -1;
		for (int i = 0; i < jj_2_rtns.length; i++)
			jj_2_rtns[i] = new JJCalls();
	}

	public Parser(ParserTokenManager tm) {
		token_source = tm;
		token = new Token();
		jj_ntk = -1;
		jj_gen = 0;
		for (int i = 0; i < 19; i++)
			jj_la1[i] = -1;
		for (int i = 0; i < jj_2_rtns.length; i++)
			jj_2_rtns[i] = new JJCalls();
	}

	public void ReInit(ParserTokenManager tm) {
		token_source = tm;
		token = new Token();
		jj_ntk = -1;
		jjtree.reset();
		jj_gen = 0;
		for (int i = 0; i < 19; i++)
			jj_la1[i] = -1;
		for (int i = 0; i < jj_2_rtns.length; i++)
			jj_2_rtns[i] = new JJCalls();
	}

	final private Token jj_consume_token(int kind) throws ParseException {
		Token oldToken;
		if ((oldToken = token).next != null)
			token = token.next;
		else token = token.next = token_source.getNextToken();
		jj_ntk = -1;
		if (token.kind == kind) {
			jj_gen++;
			if (++jj_gc > 100) {
				jj_gc = 0;
				for (int i = 0; i < jj_2_rtns.length; i++) {
					JJCalls c = jj_2_rtns[i];
					while (c != null) {
						if (c.gen < jj_gen)
							c.first = null;
						c = c.next;
					}
				}
			}
			return token;
		}
		token = oldToken;
		jj_kind = kind;
		throw generateParseException();
	}

	final private boolean jj_scan_token(int kind) {
		if (jj_scanpos == jj_lastpos) {
			jj_la--;
			if (jj_scanpos.next == null) {
				jj_lastpos = jj_scanpos = jj_scanpos.next = token_source.getNextToken();
			}
			else {
				jj_lastpos = jj_scanpos = jj_scanpos.next;
			}
		}
		else {
			jj_scanpos = jj_scanpos.next;
		}
		if (jj_rescan) {
			int i = 0;
			Token tok = token;
			while (tok != null && tok != jj_scanpos) {
				i++;
				tok = tok.next;
			}
			if (tok != null)
				jj_add_error_token(kind, i);
		}
		return (jj_scanpos.kind != kind);
	}

	final public Token getNextToken() {
		if (token.next != null)
			token = token.next;
		else token = token.next = token_source.getNextToken();
		jj_ntk = -1;
		jj_gen++;
		return token;
	}

	final public Token getToken(int index) {
		Token t = lookingAhead ? jj_scanpos : token;
		for (int i = 0; i < index; i++) {
			if (t.next != null)
				t = t.next;
			else t = t.next = token_source.getNextToken();
		}
		return t;
	}

	final private int jj_ntk() {
		if ((jj_nt = token.next) == null)
			return (jj_ntk = (token.next = token_source.getNextToken()).kind);
		return (jj_ntk = jj_nt.kind);
	}

	private java.util.Vector jj_expentries = new java.util.Vector();
	private int[] jj_expentry;
	private int jj_kind = -1;
	private int[] jj_lasttokens = new int[100];
	private int jj_endpos;

	@SuppressWarnings("unchecked")
	private void jj_add_error_token(int kind, int pos) {
		if (pos >= 100)
			return;
		if (pos == jj_endpos + 1) {
			jj_lasttokens[jj_endpos++] = kind;
		}
		else if (jj_endpos != 0) {
			jj_expentry = new int[jj_endpos];
			for (int i = 0; i < jj_endpos; i++) {
				jj_expentry[i] = jj_lasttokens[i];
			}
			boolean exists = false;
			for (java.util.Enumeration enum1 = jj_expentries.elements(); enum1.hasMoreElements();) {
				int[] oldentry = (int[]) (enum1.nextElement());
				if (oldentry.length == jj_expentry.length) {
					exists = true;
					for (int i = 0; i < jj_expentry.length; i++) {
						if (oldentry[i] != jj_expentry[i]) {
							exists = false;
							break;
						}
					}
					if (exists)
						break;
				}
			}
			if (!exists)
				jj_expentries.addElement(jj_expentry);
			if (pos != 0)
				jj_lasttokens[(jj_endpos = pos) - 1] = kind;
		}
	}

	@SuppressWarnings("unchecked")
	final public ParseException generateParseException() {
		jj_expentries.removeAllElements();
		boolean[] la1tokens = new boolean[33];
		for (int i = 0; i < 33; i++) {
			la1tokens[i] = false;
		}
		if (jj_kind >= 0) {
			la1tokens[jj_kind] = true;
			jj_kind = -1;
		}
		for (int i = 0; i < 19; i++) {
			if (jj_la1[i] == jj_gen) {
				for (int j = 0; j < 32; j++) {
					if ((jj_la1_0[i] & (1 << j)) != 0) {
						la1tokens[j] = true;
					}
					if ((jj_la1_1[i] & (1 << j)) != 0) {
						la1tokens[32 + j] = true;
					}
				}
			}
		}
		for (int i = 0; i < 33; i++) {
			if (la1tokens[i]) {
				jj_expentry = new int[1];
				jj_expentry[0] = i;
				jj_expentries.addElement(jj_expentry);
			}
		}
		jj_endpos = 0;
		jj_rescan_token();
		jj_add_error_token(0, 0);
		int[][] exptokseq = new int[jj_expentries.size()][];
		for (int i = 0; i < jj_expentries.size(); i++) {
			exptokseq[i] = (int[]) jj_expentries.elementAt(i);
		}
		return new ParseException(token, exptokseq, tokenImage);
	}

	final public void enable_tracing() {
	}

	final public void disable_tracing() {
	}

	final private void jj_rescan_token() {
		jj_rescan = true;
		for (int i = 0; i < 6; i++) {
			JJCalls p = jj_2_rtns[i];
			do {
				if (p.gen > jj_gen) {
					jj_la = p.arg;
					jj_lastpos = jj_scanpos = p.first;
					switch (i) {
					case 0:
						jj_3_1();
						break;
					case 1:
						jj_3_2();
						break;
					case 2:
						jj_3_3();
						break;
					case 3:
						jj_3_4();
						break;
					case 4:
						jj_3_5();
						break;
					case 5:
						jj_3_6();
						break;
					}
				}
				p = p.next;
			} while (p != null);
		}
		jj_rescan = false;
	}

	final private void jj_save(int index, int xla) {
		JJCalls p = jj_2_rtns[index];
		while (p.gen > jj_gen) {
			if (p.next == null) {
				p = p.next = new JJCalls();
				break;
			}
			p = p.next;
		}
		p.gen = jj_gen + xla - jj_la;
		p.first = token;
		p.arg = xla;
	}

	static final class JJCalls {
		int gen;
		Token first;
		int arg;
		JJCalls next;
	}

}
