/* $Author: qxie $
 * $Date: 2007-03-27 18:22:42 $
 * $Revision: 1.13 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.viewer;

import org.jmol.util.Logger;
import org.jmol.util.CommandHistory;
import org.jmol.g3d.Graphics3D;

import java.util.Vector;
import java.util.BitSet;

import javax.vecmath.Point3f;

class Compiler {

	Viewer viewer;
	String filename;
	String script;

	short[] lineNumbers;
	short[] lineIndices;
	Token[][] aatokenCompiled;

	boolean error;
	String errorMessage;
	String errorLine;
	boolean preDefining;

	boolean logMessages = false;

	private void log(String message) {
		if (logMessages)
			Logger.debug(message);
	}

	Compiler(Viewer viewer) {
		this.viewer = viewer;
	}

	boolean compile(String filename, String script, boolean isPredefining) {
		this.filename = filename;
		this.script = script;
		logMessages = (!isPredefining && Logger.isActiveLevel(Logger.LEVEL_DEBUG));
		lineNumbers = lineIndices = null;
		aatokenCompiled = null;
		errorMessage = errorLine = null;
		preDefining = (filename == "#predefine");
		if (compile0())
			return true;
		int icharEnd;
		if ((icharEnd = script.indexOf('\r', ichCurrentCommand)) == -1
				&& (icharEnd = script.indexOf('\n', ichCurrentCommand)) == -1)
			icharEnd = script.length();
		errorLine = script.substring(ichCurrentCommand, icharEnd);
		return false;
	}

	short[] getLineNumbers() {
		return lineNumbers;
	}

	short[] getLineIndices() {
		return lineIndices;
	}

	Token[][] getAatokenCompiled() {
		return aatokenCompiled;
	}

	String getErrorMessage() {
		String strError = errorMessage;
		strError += " : " + errorLine + "\n";
		if (filename != null)
			strError += filename;
		strError += " line#" + lineCurrent;
		viewer.addCommand(errorLine + CommandHistory.ERROR_FLAG);
		return strError;
	}

	int cchScript;
	short lineCurrent;

	int ichToken;
	int cchToken;
	Token[] atokenCommand;

	int ichCurrentCommand;

	boolean iHaveQuotedString = false;

	@SuppressWarnings("unchecked")
	boolean compile0() {
		cchScript = script.length();
		ichToken = 0;
		lineCurrent = 1;
		int lnLength = 8;
		lineNumbers = new short[lnLength];
		lineIndices = new short[lnLength];
		error = false;

		Vector lltoken = new Vector();
		Vector ltoken = new Vector();
		// Token tokenCommand = null;
		int tokCommand = Token.nada;
		for (int ptrToken = 0; true; ichToken += cchToken, ptrToken++) {
			if (lookingAtLeadingWhitespace())
				continue;
			if (lookingAtComment())
				continue;
			boolean endOfLine = lookingAtEndOfLine();
			if (endOfLine || lookingAtEndOfStatement()) {
				ptrToken = 0;
				if (tokCommand != Token.nada) {
					if (!compileCommand(ltoken))
						return false;
					lltoken.addElement(atokenCommand);
					int iCommand = lltoken.size();
					if (iCommand == lnLength) {
						short[] lnT = new short[lnLength * 2];
						System.arraycopy(lineNumbers, 0, lnT, 0, lnLength);
						lineNumbers = lnT;
						lnT = new short[lnLength * 2];
						System.arraycopy(lineIndices, 0, lnT, 0, lnLength);
						lineIndices = lnT;
						lnLength *= 2;
					}
					lineNumbers[iCommand] = lineCurrent;
					lineIndices[iCommand] = (short) ichCurrentCommand;
					ltoken.setSize(0);
					tokCommand = Token.nada;
					iHaveQuotedString = false;
				}
				if (ichToken < cchScript) {
					if (endOfLine)
						++lineCurrent;
					continue;
				}
				break;
			}
			if (tokCommand == Token.nada) {
				bracketsOpen = false;
			}
			else {
				if (lookingAtString()) {
					String str = (tokCommand == Token.load && !iHaveQuotedString ? script.substring(ichToken + 1,
							ichToken + cchToken - 1) : getUnescapedStringLiteral());
					ltoken.addElement(new Token(Token.string, str));
					iHaveQuotedString = true;
					if (tokCommand == Token.data)
						getData(ltoken, str);
					continue;
				}
				if (tokCommand == Token.load) {
					if (lookingAtLoadFormat()) {
						// String strFormat = script.substring(ichToken, ichToken + cchToken);
						// strFormat = strFormat.toLowerCase();
						// ltoken.addElement(new Token(Token.identifier, strFormat));
						continue;
					}
					if (!iHaveQuotedString && lookingAtSpecialString()) {
						String str = script.substring(ichToken, ichToken + cchToken).trim();
						int pt = str.indexOf(" ");
						if (pt > 0) {
							cchToken = pt;
							str = str.substring(0, pt);
						}
						ltoken.addElement(new Token(Token.string, str));
						iHaveQuotedString = true;
						continue;
					}
				}
				if (((tokCommand & Token.specialstring) != 0) && lookingAtSpecialString()) {
					String str = script.substring(ichToken, ichToken + cchToken);
					ltoken.addElement(new Token(Token.string, str));
					continue;
				}
				float value;
				if (!Float.isNaN(value = lookingAtExponential())) {
					ltoken.addElement(new Token(Token.decimal, new Float(value)));
					continue;
				}
				if (lookingAtDecimal((tokCommand & Token.negnums) != 0)) {
					value =
					// can't use parseFloat with jvm 1.1
					// Float.parseFloat(script.substring(ichToken, ichToken + cchToken));
					Float.valueOf(script.substring(ichToken, ichToken + cchToken)).floatValue();
					ltoken.addElement(new Token(Token.decimal, new Float(value)));
					continue;
				}
				if (lookingAtSeqcode()) {
					char ch = script.charAt(ichToken);
					int seqNum = (ch == '*' || ch == '^' ? 0 : Integer.parseInt(script.substring(ichToken, ichToken
							+ cchToken - 2)));
					char insertionCode = script.charAt(ichToken + cchToken - 1);
					if (insertionCode == '^')
						insertionCode = ' ';
					int seqcode = Group.getSeqcode(seqNum, insertionCode);
					ltoken.addElement(new Token(Token.seqcode, seqcode, "seqcode"));
					continue;
				}
				if (lookingAtInteger((tokCommand & Token.negnums) != 0)) {
					String intString = script.substring(ichToken, ichToken + cchToken);
					int val = Integer.parseInt(intString);
					ltoken.addElement(new Token(Token.integer, val, intString));
					continue;
				}
			}
			if (lookingAtLookupToken()) {
				String ident = script.substring(ichToken, ichToken + cchToken);
				Token token;
				// hack to support case sensitive alternate locations and chains
				// if an identifier is a single character long, then
				// allocate a new Token with the original character preserved
				if (ident.length() == 1) {
					token = (Token) Token.map.get(ident);
					if (token == null) {
						String lowerCaseIdent = ident.toLowerCase();
						token = (Token) Token.map.get(lowerCaseIdent);
						if (token != null)
							token = new Token(token.tok, token.intValue, ident);
					}
				}
				else {
					ident = ident.toLowerCase();
					token = bracketsOpen ? null : (Token) Token.map.get(ident);
				}
				if (token == null)
					token = new Token(Token.identifier, ident);
				int tok = token.tok;
				switch (tokCommand) {
				case Token.nada:
					ichCurrentCommand = ichToken;
					// tokenCommand = token;
					tokCommand = tok;
					if ((tokCommand & Token.command) == 0)
						return commandExpected(script.substring(ichCurrentCommand));
					break;
				case Token.set:
					if (ltoken.size() == 1) {
						if ((tok & Token.setspecial) != 0) {
							// tokenCommand = token;
							tokCommand = tok;
							ltoken.removeAllElements();
							break;
						}
						if ((tok & Token.setparam) == 0 && tok != Token.identifier)
							return cannotSet(ident);
					}
					break;
				case Token.show:
					if ((tok & Token.showparam) == 0 && ptrToken == 2)
						return cannotShow(ident);
					break;
				case Token.define:
					if (ltoken.size() == 1) {
						// we are looking at the variable name

						if (!preDefining && tok != Token.identifier) {
							if ((tok & Token.predefinedset) != Token.predefinedset) {
								Logger.warn("WARNING: redefining " + ident + "; was " + token);
								tok = token.tok = Token.identifier;
								Token.map.put(ident, token);
								Logger
										.warn("WARNING: not all commands may continue to be functional for the life of the applet!");
							}
							else {
								Logger.warn("WARNING: predefined term '" + ident
										+ "' has been redefined by the user until the next file load.");
							}
						}

						if (tok != Token.identifier && (tok & Token.predefinedset) != Token.predefinedset)
							return invalidExpressionToken(ident);
					}
					else {
						// we are looking at the expression
						if (tok != Token.identifier && tok != Token.set
								&& (tok & (Token.expression | Token.predefinedset)) == 0)
							return invalidExpressionToken(ident);
					}
					break;
				case Token.center:
					if (tok != Token.identifier && tok != Token.dollarsign && (tok & Token.expression) == 0)
						return invalidExpressionToken(ident);
					break;
				case Token.restrict:
				case Token.select:
				case Token.display:
					if (tok != Token.identifier && (tok & Token.expression) == 0)
						return invalidExpressionToken(ident);
					break;
				}
				ltoken.addElement(token);
				continue;
			}
			if (ltoken.size() == 0)
				return commandExpected();
			return unrecognizedToken(script);
		}
		aatokenCompiled = new Token[lltoken.size()][];
		lltoken.copyInto(aatokenCompiled);
		return true;
	}

	@SuppressWarnings("unchecked")
	void getData(Vector ltoken, String key) {
		ichToken += key.length() + 2;
		if (script.length() > ichToken && script.charAt(ichToken) == '\r')
			ichToken++;
		if (script.length() > ichToken && script.charAt(ichToken) == '\n')
			ichToken++;
		int i = script.indexOf("end \"" + key + "\"", ichToken);
		if (i < 0)
			i = script.length();
		String str = script.substring(ichToken, i);
		ltoken.addElement(new Token(Token.data, str));
		cchToken = i - ichToken + 6 + key.length();
	}

	private final static boolean isSpaceOrTab(char ch) {
		return ch == ' ' || ch == '\t';
	}

	boolean lookingAtLeadingWhitespace() {
		// log("lookingAtLeadingWhitespace");
		int ichT = ichToken;
		while (ichT < cchScript && isSpaceOrTab(script.charAt(ichT)))
			++ichT;
		cchToken = ichT - ichToken;
		// log("leadingWhitespace cchScript=" + cchScript + " cchToken=" + cchToken);
		return cchToken > 0;
	}

	boolean lookingAtComment() {
		// log ("lookingAtComment ichToken=" + ichToken + " cchToken=" + cchToken);
		// first, find the end of the statement and scan for # (sharp) signs
		char ch;
		int ichEnd = ichToken;
		int ichFirstSharp = -1;
		while (ichEnd < cchScript && (ch = script.charAt(ichEnd)) != ';' && ch != '\r' && ch != '\n') {
			if (ch == '#' && ichFirstSharp == -1) {
				ichFirstSharp = ichEnd;
				// Logger.debug("I see a first sharp @ " + ichFirstSharp);
			}
			++ichEnd;
		}
		if (ichFirstSharp == -1) // there were no sharps found
			return false;

		/***************************************************************************************************************
		 * check for #jc comment if it occurs anywhere in the statement, then the statement is not executed. This allows
		 * statements which are executed in RasMol but are comments in Jmol
		 **************************************************************************************************************/

		if (cchScript - ichFirstSharp >= 3 && script.charAt(ichFirstSharp + 1) == 'j'
				&& script.charAt(ichFirstSharp + 2) == 'c') {
			// statement contains a #jc before then end ... strip it all
			cchToken = ichEnd - ichToken;
			return true;
		}

		// if the sharp was not the first character then it isn't a comment
		if (ichFirstSharp != ichToken)
			return false;

		/***************************************************************************************************************
		 * check for leading #jx <space> or <tab> if you see it, then only strip those 4 characters if they put in #jx
		 * <newline> then they are not going to execute anything, and the regular code will take care of it
		 **************************************************************************************************************/
		if (cchScript > ichToken + 3 && script.charAt(ichToken + 1) == 'j' && script.charAt(ichToken + 2) == 'x'
				&& isSpaceOrTab(script.charAt(ichToken + 3))) {
			cchToken = 4; // #jx[\s\t]
			return true;
		}

		// first character was a sharp, but was not #jx ... strip it all
		cchToken = ichEnd - ichToken;
		return true;
	}

	boolean lookingAtEndOfLine() {
		// log("lookingAtEndOfLine");
		if (ichToken >= cchScript)
			return true;
		int ichT = ichToken;
		char ch = script.charAt(ichT);
		if (ch == '\r') {
			++ichT;
			if (ichT < cchScript && script.charAt(ichT) == '\n')
				++ichT;
		}
		else if (ch == '\n') {
			++ichT;
		}
		else {
			return false;
		}
		cchToken = ichT - ichToken;
		return true;
	}

	boolean lookingAtEndOfStatement() {
		if (ichToken == cchScript || script.charAt(ichToken) != ';')
			return false;
		cchToken = 1;
		return true;
	}

	boolean lookingAtString() {
		if (ichToken == cchScript)
			return false;
		if (script.charAt(ichToken) != '"')
			return false;
		// remove support for single quote
		// in order to use it in atom expressions
		// char chFirst = script.charAt(ichToken);
		// if (chFirst != '"' && chFirst != '\'')
		// return false;
		int ichT = ichToken + 1;
		// while (ichT < cchScript && script.charAt(ichT++) != chFirst)
		char ch;
		boolean previousCharBackslash = false;
		while (ichT < cchScript) {
			ch = script.charAt(ichT++);
			if (ch == '"' && !previousCharBackslash)
				break;
			previousCharBackslash = ch == '\\' ? !previousCharBackslash : false;
		}
		cchToken = ichT - ichToken;
		return true;
	}

	String getUnescapedStringLiteral() {
		if (cchToken < 2)
			return "";
		StringBuffer sb = new StringBuffer(cchToken - 2);
		int ichMax = ichToken + cchToken - 1;
		int ich = ichToken + 1;
		while (ich < ichMax) {
			char ch = script.charAt(ich++);
			if (ch == '\\' && ich < ichMax) {
				ch = script.charAt(ich++);
				switch (ch) {
				case 'b':
					ch = '\b';
					break;
				case 'n':
					ch = '\n';
					break;
				case 't':
					ch = '\t';
					break;
				case 'r':
					ch = '\r';
					// fall into
				case '"':
				case '\\':
				case '\'':
					break;
				case 'x':
				case 'u':
					int digitCount = ch == 'x' ? 2 : 4;
					if (ich < ichMax) {
						int unicode = 0;
						for (int k = digitCount; --k >= 0 && ich < ichMax;) {
							char chT = script.charAt(ich);
							int hexit = getHexitValue(chT);
							if (hexit < 0)
								break;
							unicode <<= 4;
							unicode += hexit;
							++ich;
						}
						ch = (char) unicode;
					}
				}
			}
			sb.append(ch);
		}
		return "" + sb;
	}

	static int getHexitValue(char ch) {
		if (ch >= '0' && ch <= '9')
			return ch - '0';
		else if (ch >= 'a' && ch <= 'f')
			return 10 + ch - 'a';
		else if (ch >= 'A' && ch <= 'F')
			return 10 + ch - 'A';
		else return -1;
	}

	// note that these formats include a space character
	String[] loadFormats = { "alchemy ", "mol2 ", "mopac ", "nmrpdb ", "charmm ", "xyz ", "mdl ", "pdb " };

	boolean lookingAtLoadFormat() {
		for (int i = loadFormats.length; --i >= 0;) {
			String strFormat = loadFormats[i];
			int cchFormat = strFormat.length();
			if (script.regionMatches(true, ichToken, strFormat, 0, cchFormat)) {
				cchToken = cchFormat - 1; // subtract off the space character
				return true;
			}
		}
		return false;
	}

	boolean lookingAtSpecialString() {
		int ichT = ichToken;
		char ch;
		while (ichT < cchScript && (ch = script.charAt(ichT)) != ';' && ch != '\r' && ch != '\n')
			++ichT;
		cchToken = ichT - ichToken;
		log("lookingAtSpecialString cchToken=" + cchToken);
		return cchToken > 0;
	}

	float lookingAtExponential() {
		if (ichToken == cchScript)
			return Float.NaN; // end
		int ichT = ichToken;
		boolean isNegative = (script.charAt(ichT) == '-');
		if (isNegative)
			++ichT;
		int pt0 = ichT;
		boolean digitSeen = false;
		char ch = 'X';
		while (ichT < cchScript && Character.isDigit(ch = script.charAt(ichT))) {
			++ichT;
			digitSeen = true;
		}
		if (ichT < cchScript && ch == '.')
			++ichT;
		while (ichT < cchScript && Character.isDigit(ch = script.charAt(ichT))) {
			++ichT;
			digitSeen = true;
		}
		if (ichT == cchScript || !digitSeen)
			return Float.NaN; // integer
		int ptE = ichT;
		int factor = 1;
		int exp = 0;
		boolean isExponential = (ch == 'E' || ch == 'e');
		if (!isExponential || ++ichT == cchScript)
			return Float.NaN;
		ch = script.charAt(ichT);
		// I THOUGHT we only should allow "E+" or "E-" here, not "2E1" because
		// "2E1" might be a PDB het group by that name. BUT it turns out that
		// any HET group starting with a number is unacceptable and must
		// be given as [nXm], in brackets.

		if (ch == '-' || ch == '+') {
			ichT++;
			factor = (ch == '-' ? -1 : 1);
		}
		while (ichT < cchScript && Character.isDigit(ch = script.charAt(ichT))) {
			ichT++;
			exp = (exp * 10 + ch - '0');
		}
		if (exp == 0)
			return Float.NaN;
		cchToken = ichT - ichToken;
		double value = Float.valueOf(script.substring(pt0, ptE)).doubleValue();
		value *= (isNegative ? -1 : 1) * Math.pow(10, factor * exp);
		return (float) value;
	}

	boolean lookingAtDecimal(boolean allowNegative) {
		if (ichToken == cchScript)
			return false;
		int ichT = ichToken;
		if (script.charAt(ichT) == '-')
			++ichT;
		boolean digitSeen = false;
		char ch = 'X';
		while (ichT < cchScript && Character.isDigit(ch = script.charAt(ichT))) {
			++ichT;
			digitSeen = true;
		}
		if (ichT == cchScript || ch != '.')
			return false;
		// to support 1.ca, let's check the character after the dot
		// to determine if it is an alpha
		if (ch == '.' && (ichT + 1 < cchScript)
				&& (Character.isLetter(script.charAt(ichT + 1)) || script.charAt(ichT + 1) == '?'))
			return false;
		// well, guess what? we also have to look for 86.1Na, so...
		if (ch == '.' && (ichT + 2 < cchScript)
				&& (Character.isLetter(script.charAt(ichT + 2)) || script.charAt(ichT + 2) == '?'))
			return false;

		++ichT;
		while (ichT < cchScript && Character.isDigit(script.charAt(ichT))) {
			++ichT;
			digitSeen = true;
		}
		cchToken = ichT - ichToken;
		return digitSeen;
	}

	boolean lookingAtSeqcode() {
		int ichT = ichToken;
		char ch = ' ';
		if (ichT + 1 < cchScript && script.charAt(ichT) == '*' && script.charAt(ichT + 1) == '^') {
			ch = '^';
			++ichT;
		}
		else {
			while (ichT < cchScript && Character.isDigit(ch = script.charAt(ichT)))
				++ichT;
		}
		if (ch != '^')
			return false;
		ichT++;
		if (ichT == cchScript)
			ch = ' ';
		else ch = script.charAt(ichT++);
		if (ch != ' ' && ch != '*' && ch != '?' && !Character.isLetter(ch))
			return false;
		cchToken = ichT - ichToken;
		return true;
	}

	boolean lookingAtInteger(boolean allowNegative) {
		if (ichToken == cchScript)
			return false;
		int ichT = ichToken;
		if (allowNegative && script.charAt(ichToken) == '-')
			++ichT;
		int ichBeginDigits = ichT;
		while (ichT < cchScript && Character.isDigit(script.charAt(ichT)))
			++ichT;
		if (ichBeginDigits == ichT)
			return false;
		cchToken = ichT - ichToken;
		return true;
	}

	boolean bracketsOpen;

	boolean lookingAtLookupToken() {
		if (ichToken == cchScript)
			return false;
		int ichT = ichToken;
		char ch;
		switch (ch = script.charAt(ichT++)) {
		case '(':
		case ')':
		case ',':
		case '*':
		case '-':
		case '{':
		case '}':
		case '$':
		case '+':
		case ':':
		case '@':
		case '.':
		case '%':
			break;
		case '[':
			bracketsOpen = true;
			break;
		case ']':
			bracketsOpen = false;
			break;
		case '&':
		case '|':
			if (ichT < cchScript && script.charAt(ichT) == ch)
				++ichT;
			break;
		case '<':
		case '=':
		case '>':
			if (ichT < cchScript && ((ch = script.charAt(ichT)) == '<' || ch == '=' || ch == '>'))
				++ichT;
			break;
		case '/':
		case '!':
			if (ichT < cchScript && script.charAt(ichT) == '=')
				++ichT;
			break;
		default:
			if (!Character.isLetter(ch))
				return false;
			// fall through
		case '~':
		case '_':
		case '?': // include question marks in identifier for atom expressions
			while (ichT < cchScript
					&& (Character.isLetterOrDigit(ch = script.charAt(ichT)) || ch == '_' || ch == '?' || ch == '~') ||
					// hack for insertion codes embedded in an atom expression :-(
					// select c3^a
					(ch == '^' && ichT > ichToken && Character.isDigit(script.charAt(ichT - 1))))
				++ichT;
			break;
		}
		cchToken = ichT - ichToken;
		return true;
	}

	private boolean commandExpected() {
		return compileError("command expected");
	}

	private boolean commandExpected(String cmd) {
		int i = cmd.indexOf(" ");
		if (i < 0)
			i = cmd.length();
		return compileError("command expected: " + cmd.substring(0, i));
	}

	private boolean cannotSet(String ident) {
		return compileError("cannot SET: " + ident);
	}

	private boolean cannotShow(String ident) {
		return compileError("cannot SHOW: " + ident);
	}

	private boolean invalidExpressionToken(String ident) {
		return compileError("invalid expression token: " + ident);
	}

	private boolean unrecognizedToken(String ident) {
		return compileError("unrecognized token: " + ident);
	}

	private boolean badArgumentCount() {
		return compileError("bad argument count");
	}

	private boolean endOfExpressionExpected() {
		return compileError("end of expression expected");
	}

	private boolean leftParenthesisExpected() {
		return compileError("left parenthesis expected");
	}

	private boolean rightParenthesisExpected() {
		return compileError("right parenthesis expected");
	}

	private boolean coordinateExpected() {
		return compileError("{number, number, number} expected");
	}

	private boolean commaExpected() {
		return compileError("comma expected");
	}

	private boolean commaOrCloseExpected() {
		return compileError("comma or right parenthesis expected");
	}

	private boolean stringExpected() {
		return compileError("double-quoted string expected");
	}

	private boolean unrecognizedExpressionToken() {
		return compileError("unrecognized expression token:" + valuePeek());
	}

	/*
	 * private boolean integerExpectedAfterHyphen() { return compileError("integer expected after hyphen"); }
	 */
	private boolean comparisonOperatorExpected() {
		return compileError("comparison operator expected");
	}

	private boolean equalSignExpected() {
		return compileError("equal sign expected");
	}

	private boolean integerExpected() {
		return compileError("integer expected");
	}

	private boolean nonnegativeIntegerExpected() {
		return compileError("nonnegative integer expected");
	}

	private boolean numberExpected() {
		return compileError("number expected");
	}

	private boolean numberOrKeywordExpected() {
		return compileError("number or keyword expected");
	}

	private boolean badRGBColor() {
		return compileError("bad [R,G,B] color");
	}

	private boolean identifierOrResidueSpecificationExpected() {
		return compileError("identifier or residue specification expected");
	}

	private boolean residueSpecificationExpected() {
		return compileError("residue specification (ALA, AL?, A*) expected");
	}

	/*
	 * private boolean resnumSpecificationExpected() { return compileError("residue number specification expected"); }
	 * private boolean invalidResidueNameSpecification(String strResName) { return compileError("invalid residue name
	 * specification:" + strResName); }
	 */
	private boolean invalidChainSpecification() {
		return compileError("invalid chain specification");
	}

	private boolean invalidModelSpecification() {
		return compileError("invalid model specification");
	}

	private boolean invalidAtomSpecification() {
		return compileError("invalid atom specification");
	}

	private boolean compileError(String errorMessage) {
		Logger.error("compileError(" + errorMessage + ")");
		error = true;
		this.errorMessage = errorMessage;
		return false;
	}

	@SuppressWarnings("unchecked")
	private boolean compileCommand(Vector ltoken) {
		Token tokenCommand = (Token) ltoken.firstElement();
		// Logger.debug(tokenCommand + script);
		int tokCommand = tokenCommand.tok;
		int size = ltoken.size();
		if ((tokenCommand.intValue & Token.onDefault1) == Token.onDefault1 && size == 1)
			ltoken.addElement(Token.tokenOn);
		if (tokCommand == Token.set) {
			if (size < 2)
				return badArgumentCount();
			/*
			 * miguel 2005 01 01 - setDefaultOn is not used if (size == 2 && (((Token)ltoken.elementAt(1)).tok &
			 * Token.setDefaultOn) != 0) ltoken.addElement(Token.tokenOn);
			 */
		}
		atokenCommand = new Token[ltoken.size()];
		ltoken.copyInto(atokenCommand);
		int tok = (size == 1 ? Token.nada : atokenCommand[1].tok);
		if (logMessages) {
			for (int i = 0; i < atokenCommand.length; i++)
				Logger.debug(i + ": " + atokenCommand[i]);
		}
		if ((tokCommand & Token.colorparam) != 0 && !compileColorParam())
			return false;
		if ((tok == Token.leftbrace || tok == Token.dollarsign)
				&& ((tokCommand & Token.coordOrSet) != Token.coordOrSet))
			return true; // $ or { at beginning disallow expression checking for center command
		if ((tokCommand & (Token.expressionCommand | Token.embeddedExpression)) != 0 && !compileExpression())
			return false;
		if ((tokenCommand.intValue & Token.varArgCount) == 0
				&& (tokenCommand.intValue & 0x0F) + 1 != atokenCommand.length)
			return badArgumentCount();
		return true;
	}

	/*
	 * mth -- I think I am going to be sick the grammer is not context-free what does the string cys120 mean? if you
	 * have previously defined a variable, as in define cys120 carbon then when you use cys120 it refers to the previous
	 * definition. however, if cys120 was *not* previously defined, then it refers to the residue of type cys at number
	 * 120. what a disaster.
	 * 
	 * expression :: = clauseOr
	 * 
	 * clauseOr ::= clauseAnd {OR|XOR|OrNot clauseAnd}*
	 * 
	 * clauseAnd ::= clauseNot {AND clauseNot}*
	 * 
	 * clauseNot ::= NOT clauseNot | clausePrimitive
	 * 
	 * clausePrimitive ::= clauseComparator | clauseCell | // RMH 6/06 clauseWithin | clauseConnected | // RMH 3/06
	 * clauseResidueSpec | none | all | ( clauseOr )
	 * 
	 * clauseComparator ::= atomproperty comparatorop integer
	 * 
	 * clauseWithin ::= WITHIN ( clauseDistance , expression )
	 * 
	 * clauseDistance ::= integer | decimal
	 * 
	 * clauseConnected ::= CONNECTED ( integer , integer , expression ) | CONNECTED ( integer , expression ) | CONNECTED (
	 * integer , integer ) | CONNECTED ( expression ) | CONNECTED ( integer ) | CONNECTED ()
	 * 
	 * clauseResidueSpec::= { clauseResNameSpec } { clauseResNumSpec } { clauseChainSpec } { clauseAtomSpec } {
	 * clauseAlternateSpec } { clauseModelSpec }
	 * 
	 * clauseResNameSpec::= * | [ resNamePattern ] | resNamePattern
	 *  // question marks are part of identifiers // they get split up and dealt with as wildcards at runtime // and the
	 * integers which are residue number chains get bundled // in with the identifier and also split out at runtime //
	 * iff a variable of that name does not exist
	 * 
	 * resNamePattern ::= up to 3 alphanumeric chars with * and ?
	 * 
	 * clauseResNumSpec ::= * | clauseSequenceRange
	 * 
	 * clauseSequenceRange ::= clauseSequenceCode { - clauseSequenceCode }
	 * 
	 * clauseSequenceCode ::= seqcode | {-} integer
	 * 
	 * clauseChainSpec ::= {:} * | identifier | integer
	 * 
	 * clauseAtomSpec ::= . * | . identifier {*} // note that in atom spec context a * is *not* a wildcard // rather, it
	 * denotes a 'prime'
	 * 
	 * clauseAlternateSpec ::= {%} identifier | integer
	 * 
	 * clauseModelSpec ::= {:|/} * | integer
	 * 
	 */

	private boolean compileExpression() {
		int tokCommand = atokenCommand[0].tok;
		boolean isMultipleOK = ((tokCommand & Token.embeddedExpression) != 0);
		int expPtr = 1;
		if (tokCommand == Token.define)
			expPtr = 2;
		while (expPtr > 0 && expPtr < atokenCommand.length) {
			if (isMultipleOK)
				while (expPtr < atokenCommand.length && atokenCommand[expPtr].tok != Token.leftparen)
					++expPtr;
			// 0 here means OK; -1 means error;
			// > 0 means pointer to the next expression
			if (expPtr >= atokenCommand.length || (expPtr = compileExpression(expPtr)) <= 0)
				break;
			if (!isMultipleOK)
				return endOfExpressionExpected();
		}
		return (expPtr == atokenCommand.length || expPtr == 0);
	}

	Vector ltokenPostfix = null;
	Token[] atokenInfix;
	int itokenInfix;

	@SuppressWarnings("unchecked")
	boolean addTokenToPostfix(Token token) {
		if (logMessages)
			log("addTokenToPostfix" + token);
		ltokenPostfix.addElement(token);
		return true;
	}

	int compileExpression(int itoken) {
		int expPtr = 0;
		ltokenPostfix = new Vector();
		for (int i = 0; i < itoken; ++i)
			addTokenToPostfix(atokenCommand[i]);
		atokenInfix = atokenCommand;
		itokenInfix = itoken;

		addTokenToPostfix(Token.tokenExpressionBegin);
		if (!clauseOr())
			return -1;
		addTokenToPostfix(Token.tokenExpressionEnd);
		if (itokenInfix != atokenInfix.length) {
			/*
			 * Logger.debug("itokenInfix=" + itokenInfix + " atokenInfix.length=" + atokenInfix.length); for (int i = 0;
			 * i < atokenInfix.length; ++i) Logger.debug("" + i + ":" + atokenInfix[i]);
			 */
			// not a problem!
			expPtr = ltokenPostfix.size();
			for (int i = itokenInfix; i < atokenInfix.length; ++i)
				addTokenToPostfix(atokenCommand[i]);
		}
		atokenCommand = new Token[ltokenPostfix.size()];
		ltokenPostfix.copyInto(atokenCommand);
		return expPtr;
	}

	int savedPtr;

	void savePtr() {
		savedPtr = itokenInfix;
	}

	void restorePtr() {
		itokenInfix = savedPtr;
	}

	Token tokenNext() {
		if (itokenInfix == atokenInfix.length)
			return null;
		return atokenInfix[itokenInfix++];
	}

	boolean tokenNext(int tok) {
		Token token = tokenNext();
		return (token != null && token.tok == tok);
	}

	Object valuePeek() {
		if (itokenInfix == atokenInfix.length)
			return null;
		return atokenInfix[itokenInfix].value;
	}

	int intPeek() {
		if (itokenInfix == atokenInfix.length)
			return Integer.MAX_VALUE;
		return atokenInfix[itokenInfix].intValue;
	}

	int tokPeek() {
		if (itokenInfix == atokenInfix.length)
			return 0;
		return atokenInfix[itokenInfix].tok;
	}

	boolean clauseOr() {
		if (!clauseAnd())
			return false;
		// for simplicity, giving XOR (toggle) same precedence as OR
		// OrNot: First OR, but if that makes no change, then NOT (special toggle)
		while (tokPeek() == Token.opOr || tokPeek() == Token.opXor || tokPeek() == Token.opToggle) {
			Token tokenOr = tokenNext();
			if (!clauseAnd())
				return false;
			addTokenToPostfix(tokenOr);
		}
		return true;
	}

	boolean clauseAnd() {
		if (!clauseNot())
			return false;
		while (tokPeek() == Token.opAnd) {
			Token tokenAnd = tokenNext();
			if (!clauseNot())
				return false;
			addTokenToPostfix(tokenAnd);
		}
		return true;
	}

	boolean clauseNot() {
		if (tokPeek() == Token.opNot) {
			Token tokenNot = tokenNext();
			if (!clauseNot())
				return false;
			return addTokenToPostfix(tokenNot);
		}
		return clausePrimitive();
	}

	boolean clausePrimitive() {
		int tok = tokPeek();
		switch (tok) {
		case Token.bonds:
		case Token.monitor:
			return clauseSpecial(tok);
		case Token.cell:
			return clauseCell();
		case Token.within:
			return clauseWithin();
		case Token.connected:
			return clauseConnected();
		case Token.substructure:
			return clauseSubstructure();
		case Token.hyphen: // selecting a negative residue spec
		case Token.integer:
		case Token.seqcode:
		case Token.asterisk:
		case Token.leftsquare:
		case Token.identifier:
		case Token.colon:
		case Token.percent:
			if (clauseResidueSpec())
				return true;
		default:
			if ((tok & Token.atomproperty) == Token.atomproperty)
				return clauseComparator();
			if ((tok & Token.predefinedset) != Token.predefinedset)
				break;
			// fall into the code and below and just add the token
		case Token.all:
		case Token.none:
			return addTokenToPostfix(tokenNext());
		case Token.leftparen:
			tokenNext();
			if (!clauseOr())
				return false;
			if (!tokenNext(Token.rightparen))
				return rightParenthesisExpected();
			return true;
		case Token.leftbrace:
			if (!bitset())
				return false;
			return true;
		}
		return unrecognizedExpressionToken();
	}

	float floatValue(Token token) {
		switch (token.tok) {
		case Token.integer:
			return token.intValue;
		case Token.decimal:
			return ((Float) token.value).floatValue();
		}
		return 0;
	}

	boolean bitset() {
		Token token = tokenNext();
		int iPrev = -1;
		BitSet bs = new BitSet();
		out: while ((token = tokenNext()) != null) {
			switch (token.tok) {
			case Token.none:
			case Token.all:
				bs = null;
				if (tokenNext().tok != Token.rightbrace || iPrev >= 0)
					return endOfExpressionExpected();
				break out;
			case Token.rightbrace:
			case Token.integer:
				if (iPrev >= 0)
					bs.set(iPrev);
				if (token.tok == Token.rightbrace)
					break out;
				iPrev = token.intValue;
				break;
			case Token.colon:
				if (iPrev >= 0) {
					token = tokenNext();
					if (token.tok != Token.integer)
						return invalidExpressionToken(token.toString());
					for (int i = token.intValue; i >= iPrev; i--)
						bs.set(i);
					break;
				}
				// fall through
			default:
				return invalidExpressionToken(token.toString());
			}
		}
		return addTokenToPostfix(new Token(Token.bitset, bs));
	}

	boolean clauseComparator() {
		Token tokenAtomProperty = tokenNext();
		Token tokenComparator = tokenNext();
		if ((tokenComparator.tok & Token.comparator) == 0)
			return comparisonOperatorExpected();
		Token tokenValue = tokenNext();
		boolean isNegative = (tokenValue.tok == Token.hyphen);
		if (isNegative)
			tokenValue = tokenNext();
		int val = Integer.MAX_VALUE;
		if (tokenValue.tok == Token.decimal) {
			float vf = ((Float) tokenValue.value).floatValue();
			switch (tokenAtomProperty.tok) {
			case Token.radius:
				val = (int) (vf * 250);
				break;
			case Token.occupancy:
			case Token.temperature:
				val = (int) (vf * 100);
			}
		}
		else if (tokenValue.tok == Token.integer) {
			switch (tokenAtomProperty.tok) {
			case Token.temperature:
				val = tokenValue.intValue * 100;
				break;
			default:
				val = tokenValue.intValue;
			}
		}
		// note that a comparator instruction is a complicated instruction
		// int intValue is the tok of the property you are comparing
		// the value against which you are comparing is stored as an Integer or Float
		// in the object value

		if (val == Integer.MAX_VALUE)
			return numberExpected();
		return addTokenToPostfix(new Token(tokenComparator.tok, tokenAtomProperty.tok, new Integer(val
				* (isNegative ? -1 : 1))));
	}

	boolean clauseCell() {
		Point3f cell = new Point3f();
		tokenNext(); // CELL
		if (!tokenNext(Token.opEQ)) // =
			return equalSignExpected();
		Token coord = tokenNext(); // 555 == {1 1 1}
		if (coord.tok == Token.integer) {
			int nnn = coord.intValue;
			cell.x = nnn / 100 - 4;
			cell.y = (nnn % 100) / 10 - 4;
			cell.z = (nnn % 10) - 4;
			return addTokenToPostfix(new Token(Token.cell, cell));
		}
		if (coord.tok != Token.leftbrace) // {
			return coordinateExpected();
		coord = tokenNext(); // i
		if (coord == null || coord.tok != Token.integer && coord.tok != Token.decimal)
			return integerExpected();
		if (coord.tok == Token.integer)
			cell.x = coord.intValue;
		else cell.x = ((Float) coord.value).floatValue();
		if (tokPeek() == Token.opOr) // ,
			tokenNext();
		coord = tokenNext(); // j
		if (coord == null || coord.tok != Token.integer && coord.tok != Token.decimal)
			return integerExpected();
		if (coord.tok == Token.integer)
			cell.y = coord.intValue;
		else cell.y = ((Float) coord.value).floatValue();
		if (tokPeek() == Token.opOr) // ,
			tokenNext();
		coord = tokenNext(); // k
		if (coord == null || coord.tok != Token.integer && coord.tok != Token.decimal)
			return integerExpected();
		if (coord.tok == Token.integer)
			cell.z = coord.intValue;
		else cell.z = ((Float) coord.value).floatValue();
		if (!tokenNext(Token.rightbrace)) // }
			return coordinateExpected();
		return addTokenToPostfix(new Token(Token.cell, cell));
	}

	/**
	 * used strictly for serialization
	 * 
	 * @param tok
	 *            Token.bonds or Token.measure
	 * @return true or fail
	 */

	boolean clauseSpecial(int tok) {
		if (itokenInfix != 1)
			return invalidExpressionToken(tokenNext().toString());
		tokenNext(); // BONDS
		if (!tokenNext(Token.leftparen)) // (
			return leftParenthesisExpected();
		addTokenToPostfix(new Token(tok));
		if (!bitset())
			return false;
		if (!tokenNext(Token.rightparen)) // )
			return rightParenthesisExpected();
		if (tokenNext() != null)
			return endOfExpressionExpected();
		return true;
	}

	boolean clauseWithin() {
		tokenNext(); // WITHIN
		if (!tokenNext(Token.leftparen)) // (
			return leftParenthesisExpected();
		Object distance = null;
		Token tokenDistance = tokenNext(); // distance
		if (tokenDistance == null)
			return numberOrKeywordExpected();
		switch (tokenDistance.tok) {
		case Token.integer:
			distance = new Float((tokenDistance.intValue * 4) / 1000f);
			break;
		case Token.decimal:
		case Token.group:
		case Token.chain:
		case Token.molecule:
		case Token.model:
		case Token.site:
		case Token.element:
		case Token.string:
			distance = tokenDistance.value; // really "group" "chain" etc.
			break;
		default:
			return numberOrKeywordExpected();
		}
		if (!tokenNext(Token.opOr)) // ,
			return commaExpected();
		if (tokPeek() == Token.leftbrace) {
			return addTokenToPostfix(new Token(Token.within, new Float(Float.NaN)));
		}
		if (!clauseOr()) // *expression*
			return false;
		if (!tokenNext(Token.rightparen)) // )T
			return rightParenthesisExpected();
		return addTokenToPostfix(new Token(Token.within, distance));
	}

	boolean clauseConnected() {
		int min = 1;
		int max = 100;
		int tok;
		boolean iHaveExpression = false;
		Token token;
		tokenNext(); // Connected
		while (!iHaveExpression) {
			if (tokPeek() != Token.leftparen)
				break;
			tokenNext(); // (
			tok = tokPeek();
			if (tok == Token.integer) {
				token = tokenNext(); // minimum # or exact # of bonds (optional)
				if (token.intValue < 0)
					return nonnegativeIntegerExpected();
				min = max = token.intValue;
				token = tokenNext();
				tok = token.tok;
				if (tok == Token.rightparen) // )
					break;
				if (tok != Token.opOr) // ,
					return commaOrCloseExpected();
				tok = tokPeek();
			}
			if (tok == Token.integer) {
				token = tokenNext(); // maximum # of bonds (optional)
				if (token.intValue < 0)
					return nonnegativeIntegerExpected();
				max = token.intValue;
				token = tokenNext();
				tok = token.tok;
				if (tok == Token.rightparen) // )
					break;
				if (tok != Token.opOr) // ,
					return commaOrCloseExpected();
				tok = tokPeek();
			}
			if (tok == Token.rightparen) // )
				break;
			if (!clauseOr()) // *expression*
				return false;
			if (!tokenNext(Token.rightparen)) // )T
				return rightParenthesisExpected();
			iHaveExpression = true;
		}
		if (!iHaveExpression)
			addTokenToPostfix(new Token(Token.all));
		return addTokenToPostfix(new Token(Token.connected, min, new Integer(max)));
	}

	boolean clauseSubstructure() {
		tokenNext(); // substructure
		if (!tokenNext(Token.leftparen)) // (
			return leftParenthesisExpected();
		Token tokenSmiles = tokenNext(); // "smiles"
		if (tokenSmiles == null || tokenSmiles.tok != Token.string)
			return stringExpected();
		if (!tokenNext(Token.rightparen)) // )
			return rightParenthesisExpected();
		return addTokenToPostfix(new Token(Token.substructure, tokenSmiles.value));
	}

	boolean residueSpecCodeGenerated;

	boolean generateResidueSpecCode(Token token) {
		addTokenToPostfix(token);
		if (residueSpecCodeGenerated)
			addTokenToPostfix(Token.tokenAnd);
		residueSpecCodeGenerated = true;
		return true;
	}

	boolean clauseResidueSpec() {
		boolean specSeen = false;
		residueSpecCodeGenerated = false;
		int tok = tokPeek();
		if (tok == Token.asterisk || tok == Token.leftsquare || tok == Token.identifier) {

			// note: there are many groups that could
			// in principle be escaped here, for example:
			// "AND" "SET" and others
			// rather than do this, just have people
			// use [AND] [SET], which is no problem.

			if (!clauseResNameSpec())
				return false;
			specSeen = true;
			tok = tokPeek();
		}
		if (tok == Token.asterisk || tok == Token.hyphen || tok == Token.integer || tok == Token.seqcode) {
			if (!clauseResNumSpec())
				return false;
			specSeen = true;
			tok = tokPeek();
		}
		if (tok == Token.colon || tok == Token.asterisk || tok == Token.identifier || tok == Token.integer) {
			if (!clauseChainSpec(tok))
				return false;
			specSeen = true;
			tok = tokPeek();
		}
		if (tok == Token.dot) {
			if (!clauseAtomSpec())
				return false;
			specSeen = true;
			tok = tokPeek();
		}
		if (tok == Token.percent) {
			if (!clauseAlternateSpec())
				return false;
			specSeen = true;
			tok = tokPeek();
		}
		if (tok == Token.colon || tok == Token.slash) {
			if (!clauseModelSpec())
				return false;
			specSeen = true;
			tok = tokPeek();
		}
		if (!specSeen)
			return residueSpecificationExpected();
		if (!residueSpecCodeGenerated) {
			// nobody generated any code, so everybody was a * (or equivalent)
			addTokenToPostfix(Token.tokenAll);
		}
		return true;
	}

	boolean clauseResNameSpec() {
		if (tokPeek() == Token.asterisk) {
			tokenNext();
			return true;
		}
		Token tokenT = tokenNext();
		if (tokenT == null)
			return false;
		if (tokenT.tok == Token.leftsquare) {
			String strSpec = "";
			int tok = 0;
			while ((tokenT = tokenNext()) != null && tokenT.tok != Token.rightsquare) {
				strSpec += tokenT.value;
			}
			tok = tokenT.tok;
			if (tok != Token.rightsquare)
				return false;
			if (strSpec == "")
				return true;
			int pt;
			if (strSpec.length() > 0 && (pt = strSpec.indexOf("*")) >= 0 && pt != strSpec.length() - 1)
				return residueSpecificationExpected();
			strSpec = strSpec.toUpperCase();
			return generateResidueSpecCode(new Token(Token.spec_name_pattern, strSpec));
		}

		// no [ ]:

		if (tokenT.tok != Token.identifier)
			return identifierOrResidueSpecificationExpected();

		// check for a * in the next token, which
		// would indicate this must be a name with wildcard

		if (tokPeek() == Token.asterisk) {
			tokenNext();
			return generateResidueSpecCode(new Token(Token.identifier, tokenT.value + "*"));
		}
		return generateResidueSpecCode(tokenT);
	}

	boolean clauseResNumSpec() {
		log("clauseResNumSpec()");
		if (tokPeek() == Token.asterisk) {
			tokenNext();
			return true;
		}
		return clauseSequenceRange();
	}

	boolean clauseSequenceRange() {
		if (!clauseSequenceCode())
			return false;
		int tok = tokPeek();
		if (tok == Token.hyphen || tok == Token.integer && intPeek() < 0) {
			// must allow for a negative number here when this is an embedded expression
			// in a command that allows for negInts.
			if (tok == Token.hyphen)
				tokenNext();
			int seqcodeA = seqcode;
			if (!clauseSequenceCode())
				seqcode = Integer.MAX_VALUE;
			return generateResidueSpecCode(new Token(Token.spec_seqcode_range, seqcodeA, new Integer(seqcode)));
		}
		return generateResidueSpecCode(new Token(Token.spec_seqcode, seqcode, "seqcode"));
	}

	int seqcode;

	boolean clauseSequenceCode() {
		boolean negative = false;
		int tokPeek = tokPeek();
		if (tokPeek == Token.hyphen) {
			tokenNext();
			negative = true;
			tokPeek = tokPeek();
		}
		if (tokPeek == Token.seqcode)
			seqcode = tokenNext().intValue;
		else if (tokPeek == Token.integer) {
			int val = tokenNext().intValue;
			seqcode = Group.getSeqcode(Math.abs(val), ' ');
		}
		else return false;
		if (negative)
			seqcode = -seqcode;
		return true;
	}

	boolean clauseChainSpec(int tok) {
		if (tok == Token.colon) {
			tokenNext();
			tok = tokPeek();
			if (isSpecTerminator(tok))
				return generateResidueSpecCode(new Token(Token.spec_chain, '\0', "spec_chain"));
		}
		if (tok == Token.asterisk) {
			tokenNext();
			return true;
		}
		Token tokenChain;
		char chain;
		switch (tok) {
		// case Token.colon:
		// case Token.percent:
		// case Token.nada:
		// case Token.dot: I think this was incorrect. :.C??
		// chain = '\0';
		// break;
		case Token.integer:
			tokenChain = tokenNext();
			if (tokenChain.intValue < 0 || tokenChain.intValue > 9)
				return invalidChainSpecification();
			chain = (char) ('0' + tokenChain.intValue);
			break;
		case Token.identifier:
			tokenChain = tokenNext();
			String strChain = (String) tokenChain.value;
			if (strChain.length() != 1)
				return invalidChainSpecification();
			chain = strChain.charAt(0);
			if (chain == '?')
				return true;
			break;
		default:
			return invalidChainSpecification();
		}
		return generateResidueSpecCode(new Token(Token.spec_chain, chain, "spec_chain"));
	}

	boolean isSpecTerminator(int tok) {
		switch (tok) {
		case Token.nada:
		case Token.slash:
		case Token.opAnd:
		case Token.opOr:
		case Token.opNot:
		case Token.percent:
		case Token.rightparen:
			return true;
		}
		return false;
	}

	boolean clauseAlternateSpec() {
		tokenNext();
		int tok = tokPeek();
		if (isSpecTerminator(tok))
			return generateResidueSpecCode(new Token(Token.spec_alternate, null));
		Token tokenAlternate = tokenNext();
		String alternate = (String) tokenAlternate.value;
		switch (tokenAlternate.tok) {
		case Token.asterisk:
		case Token.string:
		case Token.integer:
		case Token.identifier:
			break;
		default:
			return invalidModelSpecification();
		}
		// Logger.debug("alternate specification seen:" + alternate);
		return generateResidueSpecCode(new Token(Token.spec_alternate, alternate));
	}

	boolean clauseModelSpec() {
		int tok = tokPeek();
		if (tok == Token.colon || tok == Token.slash)
			tokenNext();
		if (tokPeek() == Token.asterisk) {
			tokenNext();
			return true;
		}
		Token tokenModel = tokenNext();
		if (tokenModel == null)
			return invalidModelSpecification();
		switch (tokenModel.tok) {
		case Token.string:
		case Token.integer:
		case Token.identifier:
			break;
		default:
			return invalidModelSpecification();
		}
		return generateResidueSpecCode(new Token(Token.spec_model, tokenModel.value));
	}

	boolean clauseAtomSpec() {
		if (!tokenNext(Token.dot))
			return invalidAtomSpecification();
		Token tokenAtomSpec = tokenNext();
		if (tokenAtomSpec == null)
			return true;
		String atomSpec = "";
		if (tokenAtomSpec.tok == Token.integer) {
			atomSpec += "" + tokenAtomSpec.intValue;
			tokenAtomSpec = tokenNext();
			if (tokenAtomSpec == null)
				return invalidAtomSpecification();
		}
		switch (tokenAtomSpec.tok) {
		case Token.asterisk:
			return true;
		case Token.identifier:
			break;
		default:
			return invalidAtomSpecification();
		}
		atomSpec += (String) tokenAtomSpec.value;
		if (tokPeek() == Token.asterisk) {
			tokenNext();
			// this one is a '*' as a prime, not a wildcard
			atomSpec += "*";
		}
		return generateResidueSpecCode(new Token(Token.spec_atom, atomSpec));
	}

	boolean compileColorParam() {
		for (int i = 1; i < atokenCommand.length; ++i) {
			Token token = atokenCommand[i];
			// Logger.debug(token + " atokenCommand: " + atokenCommand.length);
			if (token.tok == Token.leftsquare) {
				if (!compileRGB(i))
					return false;
			}
			else if (token.tok == Token.dollarsign) {
				i++; // skip identifier
			}
			else if (token.tok == Token.identifier) {
				String id = (String) token.value;
				int argb = Graphics3D.getArgbFromString(id);
				if (argb != 0) {
					token.tok = Token.colorRGB;
					token.intValue = argb;
				}
			}
		}
		return true;
	}

	boolean compileRGB(int i) {
		Token[] atoken = atokenCommand;
		if (atoken.length >= i + 7 && atoken[i].tok == Token.leftsquare && atoken[i + 1].tok == Token.integer
				&& atoken[i + 2].tok == Token.opOr && atoken[i + 3].tok == Token.integer
				&& atoken[i + 4].tok == Token.opOr && atoken[i + 5].tok == Token.integer
				&& atoken[i + 6].tok == Token.rightsquare) {
			int argb = (0xFF000000 | atoken[i + 1].intValue << 16 | atoken[i + 3].intValue << 8 | atoken[i + 5].intValue);
			atoken[i++] = new Token(Token.colorRGB, argb, "[R,G,B]");
			for (int ipt = i + 6; ipt < atoken.length; ipt++)
				atoken[i++] = atoken[ipt];
			Token[] atokenNew = new Token[i];
			System.arraycopy(atoken, 0, atokenNew, 0, i);
			atokenCommand = atokenNew;
			return true;
		}
		// chime also accepts [xRRGGBB]
		if (atoken.length >= i + 3 && atoken[i].tok == Token.leftsquare && atoken[i + 1].tok == Token.identifier
				&& atoken[i + 2].tok == Token.rightsquare) {
			String hex = (String) atoken[i + 1].value;
			if (hex.length() == 7 && hex.charAt(0) == 'x') {
				try {
					int argb = 0xFF000000 | Integer.parseInt(hex.substring(1), 16);
					atoken[i++] = new Token(Token.colorRGB, argb, "[xRRGGBB]");
					for (int ipt = i + 2; ipt < atoken.length; ipt++)
						atoken[i++] = atoken[ipt];
					Token[] atokenNew = new Token[i];
					System.arraycopy(atoken, 0, atokenNew, 0, i);
					atokenCommand = atokenNew;
					return true;
				}
				catch (NumberFormatException e) {
				}
			}
		}
		return badRGBColor();
	}
}
