/* $RCSfile: Eval.java,v $
 * $Author: qxie $
 * $Date: 2007-03-27 18:22:42 $
 * $Revision: 1.11 $
 *
 * Copyright (C) 2003-2006  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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
package org.myjmol.viewer;

import java.io.*;
import java.util.BitSet;
import java.util.Vector;
import java.util.Hashtable;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point4f;

import org.myjmol.g3d.Font3D;
import org.myjmol.g3d.Graphics3D;
import org.myjmol.i18n.*;
import org.myjmol.smiles.InvalidSmilesException;
import org.myjmol.util.CommandHistory;
import org.myjmol.util.Logger;

class Context {
	String filename;
	String script;
	short[] linenumbers;
	short[] lineIndices;
	Token[][] aatoken;
	int pc;
}

class Eval { // implements Runnable {
	Compiler compiler;
	final static int scriptLevelMax = 10;
	int scriptLevel;
	Context[] stack = new Context[scriptLevelMax];
	String filename;
	String script;
	short[] linenumbers;
	short[] lineIndices;
	Token[][] aatoken;
	int pc; // program counter
	long timeBeginExecution;
	long timeEndExecution;
	boolean error;
	String errorMessage;
	Token[] statement;
	int statementLength;
	Viewer viewer;
	BitSet bsSubset;

	// Thread myThread;

	boolean tQuiet;
	boolean logMessages = false;

	Eval(Viewer viewer) {
		compiler = new Compiler(viewer);
		this.viewer = viewer;
		clearDefinitionsAndLoadPredefined();
	}

	void haltExecution() {
		resumePausedExecution();
		interruptExecution = Boolean.TRUE;
	}

	boolean isScriptExecuting() {
		return isExecuting && !interruptExecution.booleanValue();
	}

	static Boolean interruptExecution = Boolean.FALSE;
	static Boolean executionPaused = Boolean.FALSE;
	boolean isExecuting = false;

	Thread currentThread = null;

	public void runEval() { // only one reference now -- in Viewer
		// refresh();
		viewer.pushHoldRepaint();
		interruptExecution = Boolean.FALSE;
		executionPaused = Boolean.FALSE;
		isExecuting = true;
		currentThread = Thread.currentThread();

		timeBeginExecution = System.currentTimeMillis();
		try {
			instructionDispatchLoop();
		}
		catch (ScriptException e) {
			error = true;
			errorMessage = "" + e;
			viewer.scriptStatus("script ERROR: " + errorMessage);
		}
		timeEndExecution = System.currentTimeMillis();

		if (errorMessage == null && interruptExecution.booleanValue())
			errorMessage = "execution interrupted";
		// if (errorMessage != null)
		// viewer.scriptStatus("script ERROR: " + errorMessage);
		else if (!tQuiet)
			viewer.scriptStatus("Script completed");
		isExecuting = false;
		viewer.setTainted(true);
		viewer.popHoldRepaint();

	}

	boolean hadRuntimeError() {
		return error;
	}

	String getErrorMessage() {
		return errorMessage;
	}

	int getExecutionWalltime() {
		return (int) (timeEndExecution - timeBeginExecution);
	}

	void runScript(String script) throws ScriptException {
		pushContext();
		if (loadScript(null, script))
			instructionDispatchLoop();
		popContext();
	}

	void pushContext() throws ScriptException {
		if (scriptLevel == scriptLevelMax)
			evalError(GT._("too many script levels"));
		Context context = new Context();
		context.filename = filename;
		context.script = script;
		context.linenumbers = linenumbers;
		context.lineIndices = lineIndices;
		context.aatoken = aatoken;
		context.pc = pc;
		stack[scriptLevel++] = context;
	}

	void popContext() throws ScriptException {
		if (scriptLevel == 0)
			evalError("RasMol virtual machine error - stack underflow");
		Context context = stack[--scriptLevel];
		stack[scriptLevel] = null;
		filename = context.filename;
		script = context.script;
		linenumbers = context.linenumbers;
		lineIndices = context.lineIndices;
		aatoken = context.aatoken;
		pc = context.pc;
	}

	boolean loadScript(String filename, String script) {
		this.filename = filename;
		this.script = script;
		if (!compiler.compile(filename, script, false)) {
			error = true;
			errorMessage = compiler.getErrorMessage();
			viewer.scriptStatus("script compiler ERROR: " + errorMessage);
			return false;
		}
		pc = 0;
		aatoken = compiler.getAatokenCompiled();
		linenumbers = compiler.getLineNumbers();
		lineIndices = compiler.getLineIndices();
		return true;
	}

	boolean loadTokenInfo(String script, Vector tokenInfo) {
		this.filename = null;
		this.script = script;
		errorMessage = null;
		pc = 0;
		aatoken = (Token[][]) tokenInfo.get(0);
		linenumbers = (short[]) tokenInfo.get(1);
		lineIndices = (short[]) tokenInfo.get(2);
		return true;
	}

	@SuppressWarnings("unchecked")
	Object checkScript(String script) {
		if (!compiler.compile(null, script, false))
			return compiler.getErrorMessage();
		Vector info = new Vector();
		info.add(compiler.getAatokenCompiled());
		info.add(compiler.getLineNumbers());
		info.add(compiler.getLineIndices());
		return info;
	}

	void clearState(boolean tQuiet) {
		for (int i = scriptLevelMax; --i >= 0;)
			stack[i] = null;
		scriptLevel = 0;
		error = false;
		errorMessage = null;
		this.tQuiet = tQuiet;
	}

	boolean loadScriptString(String script, boolean tQuiet) {
		clearState(tQuiet);
		return loadScript(null, script);
	}

	boolean loadScriptFile(String filename, boolean tQuiet) {
		clearState(tQuiet);
		return loadScriptFileInternal(filename);
	}

	boolean loadScriptFileInternal(String filename) {
		if (filename.toLowerCase().indexOf("javascript:") == 0)
			return loadScript(filename, viewer.eval(filename.substring(11)));
		Object t = viewer.getInputStreamOrErrorMessageFromName(filename);
		if (!(t instanceof InputStream))
			return loadError((String) t);
		BufferedReader reader = new BufferedReader(new InputStreamReader((InputStream) t));
		StringBuffer script = new StringBuffer();
		try {
			while (true) {
				String command = reader.readLine();
				if (command == null)
					break;
				script.append(command);
				script.append("\n");
			}
		}
		catch (IOException e) {
			try {
				reader.close();
			}
			catch (IOException ioe) {
			}
			return ioError(filename);
		}
		try {
			reader.close();
		}
		catch (IOException ioe) {
		}
		return loadScript(filename, script.toString());
	}

	boolean loadError(String msg) {
		error = true;
		errorMessage = msg;
		return false;
	}

	boolean fileNotFound(String filename) {
		return loadError("file not found:" + filename);
	}

	boolean ioError(String filename) {
		return loadError("io error reading:" + filename);
	}

	public String toString() {
		StringBuffer str = new StringBuffer();
		str.append("Eval\n pc:");
		str.append(pc);
		str.append("\n");
		str.append(aatoken.length);
		str.append(" statements\n");
		for (int i = 0; i < aatoken.length; ++i) {
			str.append(" |");
			Token[] atoken = aatoken[i];
			for (int j = 0; j < atoken.length; ++j) {
				str.append(' ');
				str.append(atoken[j]);
			}
			str.append("\n");
		}
		str.append("END\n");
		return str.toString();
	}

	void clearDefinitionsAndLoadPredefined() {
		// executed each time a file is loaded; like clear() for the managers
		variables.clear();
		bsSubset = null;
		viewer.setSelectionSubset(null);

		int cPredef = JmolConstants.predefinedSets.length;
		for (int iPredef = 0; iPredef < cPredef; iPredef++)
			predefine(JmolConstants.predefinedSets[iPredef]);
		// Now, define all the elements as predefined sets
		// hydrogen is handled specially, so don't define it

		int firstIsotope = JmolConstants.firstIsotope;
		// name ==> e_=n for all standard elements
		for (int i = JmolConstants.elementNumberMax; --i > 1;) {
			String definition = "@" + JmolConstants.elementNameFromNumber(i) + " _e=" + i;
			predefine(definition);
		}
		// _Xx ==> name for of all elements, isotope-blind
		for (int i = JmolConstants.elementNumberMax; --i >= 1;) {
			String definition = "@_" + JmolConstants.elementSymbolFromNumber(i) + " "
					+ JmolConstants.elementNameFromNumber(i);
			predefine(definition);
		}
		// name ==> _e=nn for each alternative element
		for (int i = firstIsotope; --i >= 0;) {
			String definition = "@" + JmolConstants.altElementNameFromIndex(i) + " _e="
					+ JmolConstants.altElementNumberFromIndex(i);
			predefine(definition);
		}
		// these variables _e, _x can't be more than two characters
		// name ==> _isotope=iinn for each isotope
		// _T ==> _isotope=iinn for each isotope
		// _3H ==> _isotope=iinn for each isotope
		for (int i = JmolConstants.altElementMax; --i >= firstIsotope;) {
			String def = " element=" + JmolConstants.altElementNumberFromIndex(i);
			String definition = "@_" + JmolConstants.altElementSymbolFromIndex(i);
			predefine(definition + def);
			definition = "@_" + JmolConstants.altIsotopeSymbolFromIndex(i);
			predefine(definition + def);
			definition = "@" + JmolConstants.altElementNameFromIndex(i);
			if (definition.length() > 1)
				predefine(definition + def);
		}
	}

	@SuppressWarnings("unchecked")
	void predefine(String script) {
		if (compiler.compile("#predefine", script, true)) {
			Token[][] aatoken = compiler.getAatokenCompiled();
			if (aatoken.length != 1) {
				viewer
						.scriptStatus("JmolConstants.java ERROR: predefinition does not have exactly 1 command:"
								+ script);
				return;
			}
			Token[] statement = aatoken[0];
			if (statement.length > 2) {
				int tok = statement[1].tok;
				if (tok == Token.identifier || (tok & Token.predefinedset) == Token.predefinedset) {
					String variable = (String) statement[1].value;
					variables.put(variable, statement);
				}
				else {
					viewer.scriptStatus("JmolConstants.java ERROR: invalid variable name:" + script);
				}
			}
			else {
				viewer.scriptStatus("JmolConstants.java ERROR: bad predefinition length:" + script);
			}
		}
		else {
			viewer.scriptStatus("JmolConstants.java ERROR: predefined set compile error:" + script + "\ncompile error:"
					+ compiler.getErrorMessage());
		}
	}

	/*******************************************************************************************************************
	 * ============================================================== command dispatch
	 * ==============================================================
	 */

	void pauseExecution() {
		delay(100);
		executionPaused = Boolean.TRUE;
	}

	void resumePausedExecution() {
		executionPaused = Boolean.FALSE;
	}

	boolean checkContinue() {
		if (!interruptExecution.booleanValue()) {
			if (!executionPaused.booleanValue())
				return true;
			Logger.debug("script execution paused at this command: " + getCommand());
			try {
				while (executionPaused.booleanValue())
					Thread.sleep(100);
			}
			catch (Exception e) {
			}
			Logger.debug("script execution resumed");
		}
		// once more to trap quit during pause
		return !interruptExecution.booleanValue();
	}

	int commandHistoryLevelMax = 0;

	final static int MAX_IF_DEPTH = 10; // should be plenty
	boolean[] ifs = new boolean[MAX_IF_DEPTH + 1];

	void instructionDispatchLoop() throws ScriptException {
		long timeBegin = 0;
		int ifLevel = 0;
		ifs[0] = true;
		logMessages = Logger.isActiveLevel(Logger.LEVEL_DEBUG);
		if (logMessages) {
			timeBegin = System.currentTimeMillis();
			viewer.scriptStatus("Eval.instructionDispatchLoop():" + timeBegin);
			viewer.scriptStatus(toString());
		}
		if (scriptLevel <= commandHistoryLevelMax)
			viewer.addCommand(script);
		while (pc < aatoken.length) {
			if (!checkContinue())
				break;
			Token token = aatoken[pc][0];
			// if (token.tok == Token.load)
			// viewer.getSetHistory(-2); //just clear -- no, this is very useful
			statement = aatoken[pc++];
			statementLength = statement.length;
			if (logMessages)
				logDebugScript();
			Logger.debug(token.toString());
			if (ifLevel > 0 && !ifs[ifLevel] && token.tok != Token.endifcmd && token.tok != Token.ifcmd
					&& token.tok != Token.elsecmd)
				continue;
			switch (token.tok) {
			case Token.ifcmd:
				if (++ifLevel == MAX_IF_DEPTH)
					evalError(GT._("Too many nested {0} commands", "IF"));
				ifs[ifLevel] = (ifs[ifLevel - 1] && ifCmd());
				break;
			case Token.elsecmd:
				if (ifLevel < 1)
					evalError(GT._("Invalid {0} command", "ELSE"));
				ifs[ifLevel] = !ifs[ifLevel];
				break;
			case Token.endifcmd:
				if (--ifLevel < 0)
					evalError(GT._("Invalid {0} command", "ENDIF"));
				break;
			case Token.backbone:
				proteinShape(JmolConstants.SHAPE_BACKBONE);
				break;
			case Token.background:
				background();
				break;
			case Token.center:
				center(1);
				break;
			case Token.color:
				color();
				break;
			case Token.data:
				data();
				break;
			case Token.define:
				define();
				break;
			case Token.echo:
				echo();
				break;
			case Token.message:
				message();
				break;
			case Token.exit: // flush the queue and...
				if (pc > 1)
					viewer.clearScriptQueue();
			case Token.quit: // quit this only if it isn't the first command
				interruptExecution = ((pc > 1 || !viewer.usingScriptQueue()) ? Boolean.TRUE : Boolean.FALSE);
				break;
			case Token.label:
				label();
				break;
			case Token.hover:
				hover();
				break;
			case Token.load:
				load();
				break;
			case Token.monitor:
				monitor();
				break;
			case Token.refresh:
				refresh();
				break;
			case Token.initialize:
				initialize();
				break;
			case Token.reset:
				reset();
				break;
			case Token.rotate:
				rotate(false);
				break;
			case Token.script:
				script();
				break;
			case Token.history:
				history(1);
				break;
			case Token.select:
				select();
				break;
			case Token.translate:
				translate();
				break;
			case Token.translateSelected:
				translateSelected();
				break;
			case Token.zap:
				zap();
				break;
			case Token.zoom:
				zoom(false);
				break;
			case Token.zoomTo:
				zoom(true);
				break;
			case Token.delay:
				delay();
				break;
			case Token.loop:
				delay(); // a loop is just a delay followed by ...
				pc = 0; // ... resetting the program counter
				break;
			case Token.move:
				move();
				break;
			case Token.display:
				display();
				break;
			case Token.hide:
				hide();
				break;
			case Token.restrict:
				restrict();
				break;
			case Token.subset:
				subset();
				break;
			case Token.selectionHalo:
				setSelectionHalo(1);
				break;
			case Token.set:
				set();
				break;
			case Token.slab:
				slab();
				break;
			case Token.depth:
				depth();
				break;
			case Token.star:
				star();
				break;
			case Token.halo:
				halo();
				break;
			case Token.cpk:
				spacefill();
				break;
			case Token.wireframe:
				wireframe();
				break;
			case Token.vector:
				vector();
				break;
			case Token.dipole:
				dipole();
				break;
			case Token.animation:
				animation();
				break;
			case Token.vibration:
				vibration();
				break;
			case Token.calculate:
				calculate();
				break;
			case Token.dots:
				dots(1, Dots.DOTS_MODE_DOTS);
				break;
			case Token.strands:
				proteinShape(JmolConstants.SHAPE_STRANDS);
				break;
			case Token.meshRibbon:
				proteinShape(JmolConstants.SHAPE_MESHRIBBON);
				break;
			case Token.ribbon:
				proteinShape(JmolConstants.SHAPE_RIBBONS);
				break;
			case Token.trace:
				proteinShape(JmolConstants.SHAPE_TRACE);
				break;
			case Token.cartoon:
				proteinShape(JmolConstants.SHAPE_CARTOON);
				break;
			case Token.rocket:
				proteinShape(JmolConstants.SHAPE_ROCKETS);
				break;
			case Token.spin:
				rotate(true);
				break;
			case Token.ssbond:
				ssbond();
				break;
			case Token.hbond:
				hbond(true);
				break;
			case Token.show:
				show();
				break;
			case Token.frame:
				frame(1, false);
				break;
			case Token.model:
				frame(1, true);
				break;
			case Token.font:
				font();
				break;
			case Token.moveto:
				moveto();
				break;
			case Token.bondorder:
				bondorder();
				break;
			case Token.console:
				console();
				break;
			case Token.pmesh:
				pmesh();
				break;
			case Token.draw:
				draw();
				break;
			case Token.polyhedra:
				polyhedra();
				break;
			case Token.geosurface:
				dots(1, Dots.DOTS_MODE_SURFACE);
				break;
			case Token.centerAt:
				centerAt();
				break;
			case Token.isosurface:
				isosurface(JmolConstants.SHAPE_ISOSURFACE);
				break;
			case Token.lcaocartoon:
				lcaoCartoon();
				break;
			case Token.mo:
				mo();
				break;
			case Token.stereo:
				stereo();
				break;
			case Token.connect:
				connect();
				break;
			case Token.getproperty:
				getProperty();
				break;
			case Token.configuration:
				configuration();
				break;
			case Token.axes:
				setAxes(1);
				break;
			case Token.boundbox:
				setBoundbox(1);
				break;
			case Token.unitcell:
				setUnitcell(1);
				break;
			case Token.frank:
				setFrank(1);
				break;
			case Token.help:
				help();
				break;
			case Token.save:
				save();
				break;
			case Token.restore:
				restore();
				break;
			case Token.write:
				write();
				break;
			case Token.pause: // resume is done differently
				pauseExecution();
				break;

			// not implemented
			case Token.structure:
			case Token.bond:
			case Token.clipboard:
			case Token.molecule:
			case Token.print:
			case Token.renumber:
			case Token.unbond:
				// chime extended commands
			case Token.view:
			case Token.list:
			case Token.display3d:
				viewer.scriptStatus("script ERROR: command not implemented:" + token.value);
				break;
			default:
				unrecognizedCommand(token);
				return;
			}
		}
	}

	boolean ifCmd() throws ScriptException {
		if (statementLength < 1)
			badArgumentCount();
		boolean value = false;
		boolean isNot = false;
		int i = 1;
		if (i < statementLength && statement[i].tok == Token.leftparen)
			i++;
		if (i < statementLength && statement[i].tok == Token.opNot) {
			i++;
			isNot = true;
		}
		if (i == statementLength)
			badArgumentCount();
		String str = (String) statement[i].value;
		value = viewer.getBooleanProperty(str);
		return (isNot ? !value : value);
	}

	int getLinenumber() {
		return linenumbers[pc];
	}

	String getLine() {
		int ichBegin = lineIndices[pc];
		int ichEnd;
		if ((ichEnd = script.indexOf('\r', ichBegin)) == -1 && (ichEnd = script.indexOf('\n', ichBegin)) == -1)
			ichEnd = script.length();
		return script.substring(ichBegin, ichEnd);

	}

	String getCommand() {
		int ichBegin = lineIndices[pc];
		int ichEnd = (pc + 1 == lineIndices.length || lineIndices[pc + 1] == 0 ? script.length() : lineIndices[pc + 1]);
		while ("\n\r;".indexOf(script.charAt(ichEnd - 1)) >= 0)
			ichEnd--;
		return script.substring(ichBegin, ichEnd) + ";";
	}

	final StringBuffer strbufLog = new StringBuffer(80);

	void logDebugScript() {
		strbufLog.setLength(0);
		Logger.debug(statement[0].toString());
		for (int i = 1; i < statementLength; ++i) {
			strbufLog.append(statement[i] + "\n");
			Logger.debug(statement[i].toString());
		}
		strbufLog.append(statement[0].value.toString());
		for (int i = 1; i < statementLength; ++i) {
			strbufLog.append(' ');
			Token token = statement[i];
			switch (token.tok) {
			case Token.integer:
				strbufLog.append(token.intValue);
				continue;
			case Token.spec_seqcode:
				strbufLog.append(Group.getSeqcodeString(token.intValue));
				continue;
			case Token.spec_chain:
				strbufLog.append(':');
				strbufLog.append((char) token.intValue);
				continue;
			case Token.spec_alternate:
				strbufLog.append("%");
				strbufLog.append("" + token.value);
				break;
			case Token.spec_model:
				strbufLog.append("/");
				strbufLog.append("" + token.value);
				break;
			case Token.spec_resid:
				strbufLog.append('[');
				strbufLog.append(Group.getGroup3((short) token.intValue));
				strbufLog.append(']');
				continue;
			case Token.spec_name_pattern:
				strbufLog.append('[');
				strbufLog.append(token.value);
				strbufLog.append(']');
				continue;
			case Token.bitset:
				strbufLog.append(StateManager.escape((BitSet) token.value));
				continue;
			case Token.spec_atom:
				strbufLog.append('.');
				break;
			case Token.spec_seqcode_range:
				strbufLog.append(Group.getSeqcodeString(token.intValue));
				strbufLog.append('-');
				strbufLog.append(Group.getSeqcodeString(((Integer) token.value).intValue()));
				break;
			case Token.within:
				strbufLog.append("within ");
				break;
			case Token.connected:
				strbufLog.append("connected ");
				break;
			case Token.substructure:
				strbufLog.append("substructure ");
				break;
			case Token.cell:
				Point3f pt = (Point3f) token.value;
				strbufLog.append("cell={" + pt.x + " " + pt.y + " " + pt.z + "}");
				continue;
			case Token.string:
				strbufLog.append("\"" + token.value + "\"");
				continue;
			default:
				strbufLog.append(token.toString());
			}
			strbufLog.append("" + token.value);
		}
		viewer.scriptStatus(strbufLog.toString());
	}

	/*******************************************************************************************************************
	 * ============================================================== expression processing
	 * ==============================================================
	 */

	int pcLastExpressionInstruction;
	boolean isExpressionBitSet;

	BitSet expression(Token[] code, int pcStart) throws ScriptException {
		isExpressionBitSet = false;
		BitSet bs;
		BitSet[] stack = new BitSet[10];
		int sp = 0;
		Point3f thisCoordinate = null;
		boolean refreshed = false;
		pcLastExpressionInstruction = 1000;
		boolean isSubsetDefinition = (pcStart < 0);
		if (isSubsetDefinition)
			pcStart = -pcStart;
		if (logMessages)
			viewer.scriptStatus("start to evaluate expression");
		expression_loop: for (int pc = pcStart;; ++pc) {
			Token instruction = code[pc];
			if (logMessages)
				viewer.scriptStatus("instruction=" + instruction);
			switch (instruction.tok) {
			case Token.expressionBegin:
				break;
			case Token.expressionEnd:
				pcLastExpressionInstruction = pc;
				break expression_loop;
			case Token.leftbrace:
				thisCoordinate = getCoordinate(pc, true);
				pc = pcLastExpressionInstruction;
				break;
			case Token.all:
				bs = stack[sp++] = bsAll();
				break;
			case Token.none:
				stack[sp++] = new BitSet();
				break;
			case Token.opOr:
				bs = stack[--sp];
				stack[sp - 1].or(bs);
				break;
			case Token.opXor:
				bs = stack[--sp];
				stack[sp - 1].xor(bs);
				break;
			case Token.opAnd:
				bs = stack[--sp];
				stack[sp - 1].and(bs);
				break;
			case Token.opNot:
				bs = stack[sp - 1];
				notSet(bs);
				break;
			case Token.opToggle:
				bs = stack[--sp];
				toggle(stack[sp - 1], bs);
				break;
			case Token.within:
				if (thisCoordinate != null) {
					Object withinSpec = instruction.value;
					if (!(withinSpec instanceof Float))
						numberExpected();
					stack[sp++] = viewer.getAtomsWithin(((Float) withinSpec).floatValue(), thisCoordinate);
					thisCoordinate = null;
					break;
				}
				bs = stack[sp - 1];
				stack[sp - 1] = within(instruction, bs);
				break;
			case Token.connected:
				bs = stack[sp - 1];
				stack[sp - 1] = connected(instruction, bs);
				break;
			case Token.substructure:
				stack[sp++] = getSubstructureSet((String) instruction.value);
				break;
			case Token.selected:
				stack[sp++] = copyBitSet(viewer.getSelectionSet());
				break;
			case Token.subset:
				stack[sp++] = copyBitSet(bsSubset == null ? bsAll() : bsSubset);
				break;
			case Token.hidden:
				stack[sp++] = copyBitSet(viewer.getHiddenSet());
				break;
			case Token.displayed:
				stack[sp++] = invertBitSet(viewer.getHiddenSet());
				break;
			case Token.visible:
				if (!refreshed)
					viewer.setModelVisibility();
				refreshed = true;
				stack[sp++] = viewer.getVisibleSet();
				break;
			case Token.clickable:
				refresh();
				stack[sp++] = viewer.getClickableSet();
				break;
			case Token.specialposition:
			case Token.symmetry:
			case Token.unitcell:
			case Token.hetero:
			case Token.hydrogen:
			case Token.protein:
			case Token.nucleic:
			case Token.dna:
			case Token.rna:
			case Token.carbohydrate:
			case Token.purine:
			case Token.pyrimidine:
				stack[sp++] = viewer.getAtomBits((String) instruction.value);
				break;
			case Token.spec_atom:
				stack[sp++] = viewer.getAtomBits("SpecAtom", (String) instruction.value);
				break;
			case Token.spec_name_pattern:
				stack[sp++] = viewer.getAtomBits("SpecName", (String) instruction.value);
				break;
			case Token.bitset:
				stack[sp++] = (BitSet) instruction.value;
				isExpressionBitSet = true;
				break;
			case Token.spec_alternate:
				stack[sp++] = viewer.getAtomBits("SpecAlternate", (String) instruction.value);
				break;
			case Token.spec_model:
				stack[sp++] = viewer.getAtomBits("SpecModel", (String) instruction.value);
				break;
			case Token.spec_resid:
				stack[sp++] = viewer.getAtomBits("SpecResid", instruction.intValue);
				break;
			case Token.spec_seqcode:
				stack[sp++] = viewer.getAtomBits("SpecSeqcode", instruction.intValue);
				break;
			case Token.spec_chain:
				stack[sp++] = viewer.getAtomBits("SpecChain", instruction.intValue);
				break;
			case Token.spec_seqcode_range:
				int seqcodeA = instruction.intValue;
				int seqcodeB = ((Integer) instruction.value).intValue();
				stack[sp++] = viewer.getAtomBits("SpecSeqcodeRange", new int[] { seqcodeA, seqcodeB });
				break;
			case Token.cell:
				Point3f pt = (Point3f) instruction.value;
				stack[sp++] = viewer.getAtomBits("Cell", new int[] { (int) (pt.x * 1000), (int) (pt.y * 1000),
						(int) (pt.z * 1000) });
				break;
			case Token.identifier:
			case Token.amino:
			case Token.backbone:
			case Token.solvent:
			case Token.sidechain:
			case Token.surface:
				stack[sp++] = lookupIdentifierValue((String) instruction.value);
				break;
			case Token.opLT:
			case Token.opLE:
			case Token.opGE:
			case Token.opGT:
			case Token.opEQ:
			case Token.opNE:
				bs = stack[sp++] = new BitSet();
				comparatorInstruction(instruction, bs);
				break;
			default:
				unrecognizedExpression();
			}
		}
		if (sp != 1)
			evalError(GT._("atom expression compiler error - stack over/underflow"));
		if (!isSubsetDefinition && bsSubset != null)
			stack[0].and(bsSubset);
		return stack[0];
	}

	void toggle(BitSet A, BitSet B) {
		for (int i = viewer.getAtomCount(); --i >= 0;) {
			if (!B.get(i))
				continue;
			if (A.get(i)) { // both set --> clear A
				A.clear(i);
			}
			else {
				A.or(B); // A is not set --> return all on
				return;
			}
		}
	}

	void notSet(BitSet bs) {
		for (int i = viewer.getAtomCount(); --i >= 0;) {
			if (bs.get(i))
				bs.clear(i);
			else bs.set(i);
		}
	}

	BitSet lookupIdentifierValue(String identifier) throws ScriptException {
		// all variables and possible residue names for PDB
		// or atom names for non-pdb atoms are processed here.

		// priority is given to a defined variable.

		BitSet bs = lookupValue(identifier, false);
		if (bs != null)
			return copyBitSet(bs);

		// next we look for names of groups (PDB) or atoms (non-PDB)
		bs = viewer.getAtomBits("IdentifierOrNull", identifier);
		return (bs == null ? new BitSet() : bs);
	}

	@SuppressWarnings("unchecked")
	BitSet lookupValue(String variable, boolean plurals) throws ScriptException {
		if (logMessages)
			viewer.scriptStatus("lookupValue(" + variable + ")");
		Object value = variables.get(variable);
		if (value != null) {
			if (value instanceof Token[]) {
				value = expression((Token[]) value, 2);
				variables.put(variable, value);
			}
			return (BitSet) value;
		}
		if (plurals)
			return null;
		int len = variable.length();
		if (len < 5) // iron is the shortest
			return null;
		if (variable.charAt(len - 1) != 's')
			return null;
		if (variable.endsWith("ies"))
			variable = variable.substring(0, len - 3) + 'y';
		else variable = variable.substring(0, len - 1);
		return lookupValue(variable, true);
	}

	void comparatorInstruction(Token instruction, BitSet bs) throws ScriptException {
		int comparator = instruction.tok;
		int property = instruction.intValue;
		float propertyValue = Float.NaN;
		int comparisonValue = ((Integer) instruction.value).intValue();
		BitSet propertyBitSet = null;
		int bitsetComparator = comparator;
		int bitsetBaseValue = comparisonValue;
		int atomCount = viewer.getAtomCount();
		int imax = 0;
		int imin = 0;
		Frame frame = viewer.getFrame();
		for (int i = 0; i < atomCount; ++i) {
			boolean match = false;
			Atom atom = frame.getAtomAt(i);
			switch (property) {
			case Token.atomno:
				propertyValue = atom.getAtomNumber();
				break;
			case Token.atomIndex:
				propertyValue = i;
				break;
			case Token.elemno:
				propertyValue = atom.getElementNumber();
				break;
			case Token.element:
				propertyValue = atom.getAtomicAndIsotopeNumber();
				break;
			case Token.formalCharge:
				propertyValue = atom.getFormalCharge();
				break;
			case Token.site:
				propertyValue = atom.getAtomSite();
				break;
			case Token.symop:
				propertyBitSet = atom.getAtomSymmetry();
				if (bitsetBaseValue >= 1000) {
					/*
					 * symop>=1000 indicates symop*1000 + lattice_translation(555) for this the comparision is only with
					 * the translational component; the symop itself must match thus: select symop!=1655 selects all
					 * symop=1 and translation !=655 select symo >=2555 selects all symop=2 and translation >555
					 * 
					 * Note that when normalization is not done, symop=1555 may not be in the base unit cell. Everything
					 * is relative to wherever the base atoms ended up, usually in 555, but not necessarily.
					 * 
					 * The reason this is tied together an atom may have one translation for one symop and another for a
					 * different one.
					 * 
					 * Bob Hanson - 10/2006
					 */

					comparisonValue = bitsetBaseValue % 1000;
					int symop = bitsetBaseValue / 1000 - 1;
					if (symop < 0 || !(match = propertyBitSet.get(symop)))
						continue;
					bitsetComparator = Token.none;
					propertyValue = atom.getSymmetryTranslation(symop);
				}
				break;
			case Token.molecule:
				propertyValue = atom.getMoleculeNumber();
				break;
			case Token.temperature: // 0 - 9999
				propertyValue = atom.getBfactor100();
				if (propertyValue < 0)
					continue;
				break;
			case Token.surfacedistance:
				if (frame.getSurfaceDistanceMax() == 0)
					dots(statementLength, Dots.DOTS_MODE_CALCONLY);
				propertyValue = atom.getSurfaceDistance();
				if (propertyValue < 0)
					continue;
				break;
			case Token.occupancy:
				propertyValue = atom.getOccupancy();
				break;
			case Token.polymerLength:
				propertyValue = atom.getPolymerLength();
				break;
			case Token.resno:
				propertyValue = atom.getResno();
				if (propertyValue == -1)
					continue;
				break;
			case Token._groupID:
				propertyValue = atom.getGroupID();
				if (propertyValue < 0)
					continue;
				break;
			case Token._atomID:
				propertyValue = atom.getSpecialAtomID();
				if (propertyValue < 0)
					continue;
				break;
			case Token._structure:
				propertyValue = getProteinStructureType(atom);
				if (propertyValue == -1)
					continue;
				break;
			case Token.radius:
				propertyValue = atom.getRasMolRadius();
				break;
			case Token.psi:
				propertyValue = atom.getGroupPsi();
				break;
			case Token.phi:
				propertyValue = atom.getGroupPhi();
				break;
			case Token._bondedcount:
				propertyValue = atom.getCovalentBondCount();
				break;
			case Token.model:
				propertyValue = atom.getModelTagNumber();
				break;
			default:
				unrecognizedAtomProperty(property);
			}
			// note that a symop property can be both LE and GT !
			if (propertyBitSet != null) {
				switch (bitsetComparator) {
				case Token.opLT:
					imax = comparisonValue - 1;
					imin = 0;
					break;
				case Token.opLE:
					imax = comparisonValue;
					imin = 0;
					break;
				case Token.opGE:
					imax = propertyBitSet.size();
					imin = comparisonValue - 1;
					break;
				case Token.opGT:
					imax = propertyBitSet.size();
					imin = comparisonValue;
					break;
				case Token.opEQ:
					imax = comparisonValue;
					imin = comparisonValue - 1;
					break;
				case Token.opNE:
					match = !propertyBitSet.get(comparisonValue);
					break;
				}
				if (imin < 0)
					imin = 0;
				if (imax > propertyBitSet.size())
					imax = propertyBitSet.size();
				for (int iBit = imin; iBit < imax; iBit++) {
					if (propertyBitSet.get(iBit)) {
						match = true;
						break;
					}
				}
				if (!match || Float.isNaN(propertyValue))
					comparator = Token.none;
			}
			switch (comparator) {
			case Token.opLT:
				match = propertyValue < comparisonValue;
				break;
			case Token.opLE:
				match = propertyValue <= comparisonValue;
				break;
			case Token.opGE:
				match = propertyValue >= comparisonValue;
				break;
			case Token.opGT:
				match = propertyValue > comparisonValue;
				break;
			case Token.opEQ:
				match = propertyValue == comparisonValue;
				break;
			case Token.opNE:
				match = propertyValue != comparisonValue;
				break;
			}
			if (match)
				bs.set(i);
		}
	}

	BitSet within(Token instruction, BitSet bs) throws ScriptException {
		Object withinSpec = instruction.value;
		if (withinSpec instanceof Float)
			return viewer.getAtomsWithin(((Float) withinSpec).floatValue(), bs);
		if (withinSpec instanceof String) {
			String withinStr = (String) withinSpec;
			if (withinStr.equals("element") || withinStr.equals("site") || withinStr.equals("group")
					|| withinStr.equals("chain") || withinStr.equals("molecule") || withinStr.equals("model"))
				return viewer.getAtomsWithin(withinStr, bs);
			return viewer.getAtomsWithin("sequence", withinStr, bs);
		}
		evalError(GT._("Unrecognized {0} parameter", "WITHIN") + ":" + withinSpec);
		return null; // can't get here
	}

	BitSet connected(Token instruction, BitSet bs) {
		int min = instruction.intValue;
		int max = ((Integer) instruction.value).intValue();
		return viewer.getAtomsConnected(min, max, bs);
	}

	BitSet getSubstructureSet(String smiles) throws ScriptException {
		PatternMatcher matcher = new PatternMatcher(viewer);
		try {
			return matcher.getSubstructureSet(smiles);
		}
		catch (InvalidSmilesException e) {
			evalError(e.getMessage());
		}
		return null;
	}

	int getProteinStructureType(Atom atom) {
		return atom.getProteinStructureType();
	}

	/*******************************************************************************************************************
	 * ============================================================== checks and parameter retrieval
	 * ==============================================================
	 */

	void checkStatementLength(int length) throws ScriptException {
		if (statementLength != length)
			badArgumentCount();
	}

	void checkLength34() throws ScriptException {
		if (statementLength < 3 || statementLength > 4)
			badArgumentCount();
	}

	void checkLength23() throws ScriptException {
		if (statementLength < 2 || statementLength > 3)
			badArgumentCount();
	}

	void checkLength2() throws ScriptException {
		checkStatementLength(2);
	}

	void checkLength3() throws ScriptException {
		checkStatementLength(3);
	}

	void checkLength4() throws ScriptException {
		checkStatementLength(4);
	}

	String parameterAsString(int i) {
		return (statementLength <= i ? "" : statement[i].tok == Token.integer ? "" + statement[i].intValue : ""
				+ statement[i].value);
	}

	int intParameter(int index) throws ScriptException {
		if (index >= statementLength || statement[index].tok != Token.integer)
			integerExpected();
		return statement[index].intValue;
	}

	float floatParameter(int index) throws ScriptException {
		if (index >= statementLength)
			badArgumentCount();
		float floatValue = 0;
		switch (statement[index].tok) {
		case Token.integer:
			floatValue = statement[index].intValue;
			break;
		case Token.decimal:
			floatValue = ((Float) statement[index].value).floatValue();
			break;
		default:
			numberExpected();
		}
		return floatValue;
	}

	/**
	 * Based on the form of the parameters, returns and encoded radius as follows:
	 * 
	 * script meaning range encoded
	 * 
	 * +1.2 offset [0 - 10] x -1.2 offset 0) x 1.2 absolute (0 - 10] x + 10 -30% 70% (-100 - 0) x + 200 +30% 130% (0 x +
	 * 200 80% percent (0 x + 100
	 * 
	 * in each case, numbers can be integer or float
	 * 
	 * @param index
	 * @param defaultValue
	 *            a default value or Float.NaN
	 * @return one of the above possibilities
	 * @throws ScriptException
	 */
	float radiusParameter(int index, float defaultValue) throws ScriptException {
		if (index >= statementLength)
			badArgumentCount();
		float v = Float.NaN;
		boolean isOffset = (statement[index].tok == Token.plus);
		if (isOffset)
			index++;
		boolean isPercent = (index + 1 < statementLength && statement[index + 1].tok == Token.percent);
		int tok = (index < statementLength ? statement[index].tok : 0);
		switch (tok) {
		case Token.integer:
			v = statement[index].intValue;
		case Token.decimal:
			if (Float.isNaN(v))
				v = ((Float) statement[index].value).floatValue();
			if (v < 0)
				isOffset = true;
			break;
		default:
			v = defaultValue;
			index--;
		}
		pcLastExpressionInstruction = index + (isPercent ? 1 : 0);
		if (Float.isNaN(v))
			numberExpected();
		if (v == 0)
			return 0;
		if (isPercent) {
			if (v <= -100)
				invalidArgument();
			v += (isOffset ? 200 : 100);
		}
		else if (isOffset) {
		}
		else {
			if (v < 0 || v > 10)
				numberOutOfRange(0f, 10f);
			v += 10;
		}
		return v;
	}

	int floatParameterSet(int i, float[] fparams) throws ScriptException {
		if (i < statementLength && statement[i].tok == Token.leftbrace)
			i++;
		for (int j = 0; j < fparams.length; j++)
			fparams[j] = floatParameter(i++);
		if (i < statementLength && statement[i].tok != Token.rightbrace)
			i++;
		return i;
	}

	boolean isFloatParameter(int index) {
		if (index >= statementLength)
			return false;
		switch (statement[index].tok) {
		case Token.integer:
		case Token.decimal:
			return true;
		}
		return false;
	}

	String stringParameter(int index) throws ScriptException {
		if (index >= statementLength)
			badArgumentCount();
		if (statement[index].tok != Token.string)
			stringExpected();
		return (String) statement[index].value;
	}

	String objectNameParameter(int index) throws ScriptException {
		if (index >= statementLength || statement[index].tok != Token.identifier)
			objectNameExpected();
		return (String) statement[index].value;
	}

	int setShapeByNameParameter(int index) throws ScriptException {
		String objectName = objectNameParameter(index);
		int shapeType = viewer.getShapeIdFromObjectName(objectName);
		if (shapeType < 0)
			objectNameExpected();
		viewer.setShapeProperty(shapeType, "thisID", objectName);
		return shapeType;
	}

	float getRasmolAngstroms(int i) throws ScriptException {
		Token token = getToken(i);
		switch (token.tok) {
		case Token.integer:
			return token.intValue / 250f;
		case Token.decimal:
			return ((Float) token.value).floatValue();
		default:
			numberExpected();
		}
		return -1; // impossible return
	}

	boolean booleanParameter(int i) throws ScriptException {
		if (statementLength == i)
			return true;
		checkStatementLength(i + 1);
		switch (statement[i].tok) {
		case Token.on:
			return true;
		case Token.off:
			return false;
		default:
			booleanExpected();
		}
		return false;
	}

	boolean isAtomCenterOrCoordinateNext(int i) {
		return (i != statementLength && (statement[i].tok == Token.leftbrace || statement[i].tok == Token.expressionBegin));
	}

	Point3f atomCenterOrCoordinateParameter(int i) throws ScriptException {
		if (i >= statementLength)
			badArgumentCount();
		switch (statement[i].tok) {
		case Token.expressionBegin:
			return viewer.getAtomSetCenter(expression(statement, ++i));
		case Token.leftbrace:
			return getCoordinate(i, true);
		}
		invalidArgument();
		// impossible return
		return null;
	}

	Point4f planeParameter(int i) throws ScriptException {
		Vector3f vAB = new Vector3f();
		Vector3f vAC = new Vector3f();
		while (true) {
			if (i >= statementLength)
				break;
			switch (statement[i].tok) {
			case Token.leftbrace:
				if (!isCoordinate3(i))
					return getPoint4f(i);
			case Token.expressionBegin:
				Point3f pt1 = atomCenterOrCoordinateParameter(i);
				i = pcLastExpressionInstruction;
				Point3f pt2 = atomCenterOrCoordinateParameter(++i);
				i = pcLastExpressionInstruction;
				Point3f pt3 = atomCenterOrCoordinateParameter(++i);
				i = pcLastExpressionInstruction;
				Vector3f plane = new Vector3f();
				float w = Graphics3D.getPlaneThroughPoints(pt1, pt2, pt3, plane, vAB, vAC);
				Point4f p = new Point4f(plane.x, plane.y, plane.z, w);
				Logger.info("defined plane: " + p);
				return p;
			case Token.identifier:
			case Token.string:
				String str = (String) statement[i].value;
				pcLastExpressionInstruction = i;
				if (str.equalsIgnoreCase("xy"))
					return new Point4f(0, 0, 1, 0);
				if (str.equalsIgnoreCase("xz"))
					return new Point4f(0, 1, 0, 0);
				if (str.equalsIgnoreCase("yz"))
					return new Point4f(1, 0, 0, 0);
				pcLastExpressionInstruction += 2;
				if (str.equalsIgnoreCase("x")) {
					if (++i == statementLength || statement[i++].tok != Token.opEQ)
						evalError("x=?");
					return new Point4f(1, 0, 0, -floatParameter(i));
				}
				if (str.equalsIgnoreCase("y")) {
					if (++i == statementLength || statement[i++].tok != Token.opEQ)
						evalError("y=?");
					return new Point4f(0, 1, 0, -floatParameter(i));
				}
				if (str.equalsIgnoreCase("z")) {
					if (++i == statementLength || statement[i++].tok != Token.opEQ)
						evalError("z=?");
					return new Point4f(0, 0, 1, -floatParameter(i));
				}
			default:
				break;
			}
		}
		evalError(GT._("plane expected -- either three points or atom expressions or {0} or {1}", new Object[] {
				"{a b c d}", "\"xy\" \"xz\" \"yz\"" }));
		// impossible return
		return null;
	}

	Point4f hklParameter(int i) throws ScriptException {
		Point3f offset = viewer.getCurrentUnitCellOffset();
		if (offset == null)
			evalError(GT._("No unit cell"));
		Vector3f vAB = new Vector3f();
		Vector3f vAC = new Vector3f();
		Point3f pt = getCoordinate(i, true, false, true);
		Point3f pt1 = new Point3f(pt.x == 0 ? 1 : 1 / pt.x, 0, 0);
		Point3f pt2 = new Point3f(0, pt.y == 0 ? 1 : 1 / pt.y, 0);
		Point3f pt3 = new Point3f(0, 0, pt.z == 0 ? 1 : 1 / pt.z);
		// trick for 001 010 100 is to define the other points on other edges

		if (pt.x == 0 && pt.y == 0 && pt.z == 0) {
			evalError(GT._("Miller indices cannot all be zero."));
		}
		else if (pt.x == 0 && pt.y == 0) {
			pt1.set(1, 0, pt3.z);
			pt2.set(0, 1, pt3.z);
		}
		else if (pt.y == 0 && pt.z == 0) {
			pt2.set(pt1.x, 0, 1);
			pt3.set(pt1.x, 1, 0);
		}
		else if (pt.z == 0 && pt.x == 0) {
			pt3.set(0, pt2.y, 1);
			pt1.set(1, pt2.y, 0);
		}
		else if (pt.x == 0) {
			pt1.set(1, pt2.y, 0);
		}
		else if (pt.y == 0) {
			pt2.set(0, 1, pt3.z);
		}
		else if (pt.z == 0) {
			pt3.set(pt1.x, 0, 1);
		}
		viewer.convertFractionalCoordinates(pt1);
		viewer.convertFractionalCoordinates(pt2);
		viewer.convertFractionalCoordinates(pt3);
		pt1.add(offset);
		pt2.add(offset);
		pt3.add(offset);
		Vector3f plane = new Vector3f();
		float w = Graphics3D.getPlaneThroughPoints(pt1, pt2, pt3, plane, vAB, vAC);
		Point4f p = new Point4f(plane.x, plane.y, plane.z, w);
		Logger.info("defined plane: " + p);
		return p;
	}

	short getMadParameter() throws ScriptException {
		int tok = statement[1].tok;
		short mad = 1;
		switch (tok) {
		case Token.on:
			break;
		case Token.off:
			mad = 0;
			break;
		case Token.integer:
			mad = getMadInteger(statement[1].intValue);
			break;
		case Token.decimal:
			mad = getMadFloat(floatParameter(1));
			break;
		default:
			booleanOrNumberExpected();
		}
		return mad;
	}

	short getMadInteger(int radiusRasMol) throws ScriptException {
		// interesting question here about negatives... what if?
		if (radiusRasMol < 0 || radiusRasMol > 750)
			numberOutOfRange(0, 750);
		return (short) (radiusRasMol * 4 * 2);
	}

	short getMadFloat(float angstroms) throws ScriptException {
		if (angstroms < 0 || angstroms > 3)
			numberOutOfRange(0f, 3f);
		return (short) (angstroms * 1000 * 2);
	}

	short getSetAxesTypeMad(int cmdPt) throws ScriptException {
		if (cmdPt == 2)
			checkLength3();
		if (cmdPt == 1)
			checkLength2();

		int tok = statement[cmdPt].tok;
		short mad = 0;
		switch (tok) {
		case Token.on:
			mad = 1;
		case Token.off:
			break;
		case Token.integer:
			int diameterPixels = statement[cmdPt].intValue;
			if (diameterPixels < -1 || diameterPixels >= 20)
				numberOutOfRange(-1, 19);
			mad = (short) diameterPixels;
			break;
		case Token.decimal:
			float angstroms = floatParameter(cmdPt);
			if (angstroms < 0 || angstroms >= 2)
				numberOutOfRange(0.01f, 1.99f);
			mad = (short) (angstroms * 1000 * 2);
			break;
		case Token.dotted:
			mad = -1;
			break;
		default:
			booleanOrNumberExpected("DOTTED");
		}
		return mad;
	}

	static BitSet copyBitSet(BitSet bitSet) {
		BitSet copy = new BitSet();
		copy.or(bitSet);
		return copy;
	}

	private BitSet invertBitSet(BitSet bitSet) {
		BitSet copy = bsAll();
		copy.andNot(bitSet);
		return copy;
	}

	BitSet getAtomBitSet(String atomExpression) throws ScriptException {
		BitSet bs = new BitSet();
		if (!loadScript(null, "select (" + atomExpression + ")"))
			return bs;
		bs = expression(aatoken[0], 1);
		return bs;
	}

	int getArgbParam(int itoken) throws ScriptException {
		if (itoken >= statementLength)
			colorExpected();
		if (statement[itoken].tok != Token.colorRGB)
			colorExpected();
		return statement[itoken].intValue;
	}

	int getArgbOrNoneParam(int itoken) throws ScriptException {
		if (itoken >= statementLength)
			colorExpected();
		if (statement[itoken].tok == Token.colorRGB)
			return statement[itoken].intValue;
		if (statement[itoken].tok != Token.none)
			colorExpected();
		return 0;
	}

	int getArgbOrPaletteParam(int itoken) throws ScriptException {
		if (itoken < statementLength) {
			switch (statement[itoken].tok) {
			case Token.colorRGB:
				return statement[itoken].intValue;
			case Token.rasmol:
				return Token.rasmol;
			case Token.none:
			case Token.jmol:
				return Token.jmol;
			}
		}
		evalError(GT._("a color or palette name (Jmol, Rasmol) is required"));
		// unattainable
		return 0;
	}

	boolean coordinatesAreFractional;

	boolean isCoordinate3(int i) {
		ignoreError = true;
		boolean isOK = true;
		try {
			getCoordinate(i, true, true, false);
		}
		catch (Exception e) {
			isOK = false;
		}
		ignoreError = false;
		return isOK;
	}

	Point3f getCoordinate(int i, boolean allowFractional) throws ScriptException {
		return getCoordinate(i, allowFractional, true, false);
	}

	Point3f getCoordinate(int i, boolean allowFractional, boolean doConvert, boolean implicitFractional)
			throws ScriptException {
		// syntax: {1/2, 1/2, 1/3} or {0.5/, 0.5, 0.5}
		// ONE fractional sign anywhere is enough to make ALL fractional;
		// denominator of 1 can be implied;
		// commas not necessary if denominator is present or not a fraction
		if (i >= statementLength)
			coordinateExpected();
		coordinatesAreFractional = implicitFractional;
		if (statement[i++].tok != Token.leftbrace)
			coordinateExpected();
		Point3f pt = new Point3f();
		out: for (int j = i; j + 1 < statementLength; j++) {
			switch (statement[j].tok) {
			case Token.slash:
				coordinatesAreFractional = true;
			case Token.rightbrace:
				break out;
			}
		}
		if (coordinatesAreFractional && !allowFractional)
			evalError(GT._("fractional coordinates are not allowed in this context"));
		pt.x = coordinateValue(i);
		pt.y = coordinateValue(++pcLastExpressionInstruction);
		pt.z = coordinateValue(++pcLastExpressionInstruction);
		if (statement[++pcLastExpressionInstruction].tok != Token.rightbrace)
			coordinateExpected();
		if (coordinatesAreFractional && doConvert)
			viewer.convertFractionalCoordinates(pt);
		return pt;
	}

	Point4f getPoint4f(int i) throws ScriptException {
		coordinatesAreFractional = false;
		if (statement[i++].tok != Token.leftbrace)
			coordinateExpected();
		Point4f pt = new Point4f();
		out: for (int j = i; j + 1 < statementLength; j++) {
			switch (statement[j].tok) {
			case Token.slash:
				coordinatesAreFractional = true;
			case Token.rightbrace:
				break out;
			}
		}
		if (coordinatesAreFractional)
			evalError(GT._("fractional coordinates are not allowed in this context"));
		pt.x = coordinateValue(i);
		pt.y = coordinateValue(++pcLastExpressionInstruction);
		pt.z = coordinateValue(++pcLastExpressionInstruction);
		pt.w = coordinateValue(++pcLastExpressionInstruction);
		if (statement[++pcLastExpressionInstruction].tok != Token.rightbrace)
			coordinateExpected();
		return pt;
	}

	float coordinateValue(int i) throws ScriptException {
		// includes support for fractional coordinates
		float val = floatParameter(i++);
		Token token = getToken(i);
		if (token.tok == Token.slash) {
			token = getToken(++i);
			if (token.tok == Token.integer || token.tok == Token.decimal) {
				val /= floatParameter(i++);
				token = getToken(i);
			}
		}
		pcLastExpressionInstruction = (token.tok == Token.opOr ? i : i - 1);
		return val;
	}

	Token getToken(int i) throws ScriptException {
		if (i >= statementLength)
			endOfStatementUnexpected();
		return statement[i];
	}

	/*******************************************************************************************************************
	 * ============================================================== command implementations
	 * ==============================================================
	 */

	void help() throws ScriptException {
		if (!viewer.isApplet())
			evalError(GT._("Currently the {0} command only works for the applet", "help"));
		String what = (statementLength == 1 ? "" : stringParameter(1));
		viewer.getHelp(what);
	}

	void moveto() throws ScriptException {
		// moveto time
		// moveto [time] { x y z deg} zoom xTrans yTrans
		// moveto [time] front|back|left|right|top|bottom
		if (statementLength < 2)
			badArgumentCount();
		if (statementLength == 2 && isFloatParameter(1)) {
			refresh();
			viewer.moveTo(floatParameter(1), null, new Point3f(0, 0, 1), 0, 100, 0, 0, 0);
			return;
		}
		Point3f pt = new Point3f();
		Point3f center = null;
		int i = 1;
		float floatSecondsTotal = (isFloatParameter(1) ? floatParameter(i++) : 2.0f);
		float zoom = 100;
		float xTrans = 0;
		float yTrans = 0;
		float degrees = 90;
		switch (statement[i].tok) {
		case Token.leftbrace:
			// {X, Y, Z} deg or {x y z deg}
			if (isCoordinate3(i)) {
				pt = getCoordinate(i, true);
				i = pcLastExpressionInstruction + 1;
				degrees = floatParameter(i++);
			}
			else {
				Point4f pt4 = getPoint4f(i);
				pt.set(pt4.x, pt4.y, pt4.z);
				degrees = pt4.w;
				i = pcLastExpressionInstruction + 1;
			}
			break;
		case Token.front:
			pt.set(1, 0, 0);
			degrees = 0f;
			i++;
			break;
		case Token.back:
			pt.set(0, 1, 0);
			degrees = 180f;
			i++;
			break;
		case Token.left:
			pt.set(0, 1, 0);
			i++;
			break;
		case Token.right:
			pt.set(0, -1, 0);
			i++;
			break;
		case Token.top:
			pt.set(1, 0, 0);
			i++;
			break;
		case Token.bottom:
			pt.set(-1, 0, 0);
			i++;
			break;
		default:
			// X Y Z deg
			pt = new Point3f(floatParameter(i++), floatParameter(i++), floatParameter(i++));
			degrees = floatParameter(i++);
		}
		if (i != statementLength && !isAtomCenterOrCoordinateNext(i))
			zoom = floatParameter(i++);
		if (i != statementLength && !isAtomCenterOrCoordinateNext(i)) {
			xTrans = floatParameter(i++);
			yTrans = floatParameter(i++);
		}
		float rotationRadius = 0;
		if (i != statementLength) {
			center = atomCenterOrCoordinateParameter(i);
			i = pcLastExpressionInstruction + 1;
			if (i != statementLength)
				rotationRadius = floatParameter(i++);
		}
		refresh();
		viewer.moveTo(floatSecondsTotal, center, pt, degrees, zoom, xTrans, yTrans, rotationRadius);
	}

	void bondorder() throws ScriptException {
		Token tokenArg = statement[1];
		short order = 0;
		switch (tokenArg.tok) {
		case Token.integer:
			order = (short) tokenArg.intValue;
			if (order < 0 || order > 3)
				invalidArgument();
			break;
		case Token.hbond:
			order = JmolConstants.BOND_H_REGULAR;
			break;
		case Token.decimal:
			float f = ((Float) tokenArg.value).floatValue();
			if (f == (short) f) {
				order = (short) f;
				if (order < 0 || order > 3)
					invalidArgument();
			}
			else if (f == 0.5f)
				order = JmolConstants.BOND_H_REGULAR;
			else if (f == 1.5f)
				order = JmolConstants.BOND_AROMATIC;
			else invalidArgument();
			break;
		case Token.identifier:
			order = JmolConstants.getBondOrderFromString((String) tokenArg.value);
			if (order >= 1)
				break;
			// fall into
		default:
			invalidArgument();
		}
		viewer.setShapeProperty(JmolConstants.SHAPE_STICKS, "bondOrder", new Short(order), viewer
				.getSelectedAtomsOrBonds());
	}

	void console() throws ScriptException {
		switch (statement[1].tok) {
		case Token.off:
			viewer.showConsole(false);
			break;
		case Token.on:
			viewer.showConsole(true);
			viewer.clearConsole();
			break;
		default:
			evalError("console ON|OFF");
		}
	}

	void centerAt() throws ScriptException {
		if (statementLength < 2)
			badArgumentCount();
		String relativeTo = null;
		switch (statement[1].tok) {
		case Token.absolute:
			relativeTo = "absolute";
			break;
		case Token.average:
			relativeTo = "average";
			break;
		case Token.boundbox:
			relativeTo = "boundbox";
			break;
		default:
			unrecognizedSubcommand(statement[1].toString());
		}
		Point3f pt = new Point3f(0, 0, 0);
		if (statementLength == 5) {
			// centerAt xxx x y z
			pt.x = floatParameter(2);
			pt.y = floatParameter(3);
			pt.z = floatParameter(4);
		}
		else if (statement[2].tok == Token.leftbrace) {
			pt = getCoordinate(2, true);
		}
		viewer.setCenter(relativeTo, pt);
	}

	void stereo() throws ScriptException {
		int stereoMode = JmolConstants.STEREO_DOUBLE;
		// see www.usm.maine.edu/~rhodes/0Help/StereoViewing.html
		float degrees = -5;
		boolean degreesSeen = false;
		int[] colors = new int[2];
		int colorpt = 0;
		for (int i = 1; i < statementLength; ++i) {
			switch (statement[i].tok) {
			case Token.on:
				checkLength2();
				stereoMode = JmolConstants.STEREO_DOUBLE;
				break;
			case Token.off:
				checkLength2();
				stereoMode = JmolConstants.STEREO_NONE;
				break;
			case Token.colorRGB:
				if (colorpt > 1)
					badArgumentCount();
				if (!degreesSeen)
					degrees = 3;
				colors[colorpt++] = getArgbParam(i);
				if (colorpt == 1)
					colors[colorpt] = ~colors[0];
				break;
			case Token.integer:
			case Token.decimal:
				degrees = floatParameter(i);
				degreesSeen = true;
				break;
			case Token.identifier:
				String id = (String) statement[i].value;
				if (!degreesSeen)
					degrees = 3;
				if (id.equalsIgnoreCase("redblue")) {
					stereoMode = JmolConstants.STEREO_REDBLUE;
					break;
				}
				if (id.equalsIgnoreCase("redcyan")) {
					stereoMode = JmolConstants.STEREO_REDCYAN;
					break;
				}
				if (id.equalsIgnoreCase("redgreen")) {
					stereoMode = JmolConstants.STEREO_REDGREEN;
					break;
				}
				// fall into
			default:
				booleanOrNumberExpected();
			}
		}
		viewer.setFloatProperty("stereoDegrees", degrees);
		if (colorpt > 0) {
			viewer.setStereoMode(colors, StateManager.escapeColor(colors[0]) + " "
					+ StateManager.escapeColor(colors[1]));
		}
		else {
			viewer.setStereoMode(stereoMode, (String) statement[1].value);
		}
	}

	void connect() throws ScriptException {

		final float[] distances = new float[2];
		BitSet[] atomSets = new BitSet[2];
		atomSets[0] = atomSets[1] = viewer.getSelectionSet();

		int distanceCount = 0;
		int atomSetCount = 0;
		short bondOrder = JmolConstants.BOND_ORDER_NULL;
		int operation = JmolConstants.MODIFY_OR_CREATE;
		boolean isDelete = false;
		boolean haveType = false;
		int nAtomSets = 0;
		int nDistances = 0;
		/*
		 * connect [<=2 distance parameters] [<=2 atom sets] [<=1 bond type] [<=1 operation]
		 * 
		 */

		if (statementLength == 1) {
			viewer.rebond();
			return;
		}

		for (int i = 1; i < statementLength; ++i) {
			switch_tag: switch (statement[i].tok) {
			case Token.on:
			case Token.off:
				if (statementLength != 2)
					badArgumentCount();
				viewer.rebond();
				return;
			case Token.integer:
			case Token.decimal:
				if (++nDistances > 2)
					badArgumentCount();
				if (nAtomSets > 0 || haveType)
					invalidParameterOrder();
				distances[distanceCount++] = floatParameter(i);
				break;
			case Token.expressionBegin:
				if (++nAtomSets > 2)
					badArgumentCount();
				if (haveType)
					invalidParameterOrder();
				atomSets[atomSetCount++] = expression(statement, i);
				i = pcLastExpressionInstruction; // the for loop will increment i
				break;
			case Token.identifier:
			case Token.hbond:
				String cmd = (String) statement[i].value;
				for (int j = JmolConstants.bondOrderNames.length; --j >= 0;) {
					if (cmd.equalsIgnoreCase(JmolConstants.bondOrderNames[j])) {
						if (haveType)
							incompatibleArguments();
						cmd = JmolConstants.bondOrderNames[j];
						bondOrder = JmolConstants.getBondOrderFromString(cmd);
						haveType = true;
						break switch_tag;
					}
				}
				if (++i != statementLength)
					invalidParameterOrder();
				if ("modify".equalsIgnoreCase(cmd))
					operation = JmolConstants.connectOperationFromString(cmd);
				else if ("create".equalsIgnoreCase(cmd))
					operation = JmolConstants.connectOperationFromString(cmd);
				else if ("modifyOrCreate".equalsIgnoreCase(cmd))
					operation = JmolConstants.connectOperationFromString(cmd);
				else if ("auto".equalsIgnoreCase(cmd))
					operation = JmolConstants.connectOperationFromString(cmd);
				else unrecognizedSubcommand(cmd);
				break;
			case Token.none:
			case Token.delete:
				if (++i != statementLength)
					invalidParameterOrder();
				operation = JmolConstants.connectOperationFromString("delete");
				isDelete = true;
				break;
			default:
				invalidArgument();
			}
		}
		if (distanceCount < 2) {
			if (distanceCount == 0)
				distances[0] = JmolConstants.DEFAULT_MAX_CONNECT_DISTANCE;
			distances[1] = distances[0];
			distances[0] = JmolConstants.DEFAULT_MIN_CONNECT_DISTANCE;
		}
		int n = viewer.makeConnections(distances[0], distances[1], bondOrder, operation, atomSets[0], atomSets[1]);
		if (isDelete)
			viewer.scriptStatus(GT._("{0} connections deleted", n));
		else viewer.scriptStatus(GT._("{0} connections modified or created", n));
	}

	void getProperty() {
		String retValue = "";
		String property = (statementLength < 2 ? "" : (String) statement[1].value);
		String param = (statementLength < 3 ? "" : (String) statement[2].value);
		retValue = (String) viewer.getProperty("readable", property, param);
		showString(retValue);
	}

	void background() throws ScriptException {
		if (statementLength < 2 || statementLength > 3)
			badArgumentCount();
		int tok = statement[1].tok;
		if (tok == Token.colorRGB || tok == Token.none)
			viewer.setBackgroundArgb(getArgbOrNoneParam(1));
		else viewer.setShapePropertyArgb(getShapeType(tok), "bgcolor", getArgbOrNoneParam(2));
	}

	void center(int i) throws ScriptException {
		// from center (atom) or from zoomTo under conditions of not windowCentered()
		if (statementLength == 1) {
			viewer.setCenterBitSet(null, true);
			return;
		}

		if (statement[i].tok == Token.dollarsign) {
			// center $ id
			String axisID = objectNameParameter(i + 1);
			viewer.setNewRotationCenter(axisID);
			return;
		}

		if (statement[i].tok == Token.leftbrace) {
			// center { x y z }
			Point3f pt = getCoordinate(i, true);
			viewer.setNewRotationCenter(pt);
			return;
		}
		viewer.setCenterBitSet(expression(statement, i), true);
	}

	void color() throws ScriptException {
		int argb;
		if (statementLength > 5 || statementLength < 2)
			badArgumentCount();
		int tok = statement[1].tok;
		switch (tok) {
		case Token.dollarsign:
			colorNamedObject(2);
			return;
		case Token.colorRGB:
		case Token.none:
		case Token.cpk:
		case Token.amino:
		case Token.chain:
		case Token.group:
		case Token.shapely:
		case Token.structure:
		case Token.temperature:
		case Token.fixedtemp:
		case Token.formalCharge:
		case Token.partialCharge:
		case Token.surfacedistance:
		case Token.user:
		case Token.monomer:
		case Token.molecule:
		case Token.altloc:
		case Token.insertion:
		case Token.translucent:
		case Token.opaque:
			colorObject(Token.atom, 1);
			return;
		case Token.jmol:
		case Token.rasmol:
			colorObject(Token.atom, 1);
			return;
		case Token.rubberband:
			viewer.setRubberbandArgb(getArgbParam(2));
			return;
		case Token.background:
			viewer.setBackgroundArgb(getArgbOrNoneParam(2));
			return;
		case Token.selectionHalo:
			argb = getArgbOrNoneParam(2);
			viewer.loadShape(JmolConstants.SHAPE_HALOS);
			viewer.setShapeProperty(JmolConstants.SHAPE_HALOS, "argbSelection", new Integer(argb));
			return;
		case Token.identifier:
		case Token.hydrogen:
			// color element
			argb = getArgbOrPaletteParam(2);
			String str = (String) statement[1].value;
			for (int i = JmolConstants.elementNumberMax; --i >= 0;) {
				if (str.equalsIgnoreCase(JmolConstants.elementNameFromNumber(i))) {
					viewer.setElementArgb(i, argb);
					return;
				}
			}
			for (int i = JmolConstants.altElementMax; --i >= 0;) {
				if (str.equalsIgnoreCase(JmolConstants.altElementNameFromIndex(i))) {
					viewer.setElementArgb(JmolConstants.altElementNumberFromIndex(i), argb);
					return;
				}
			}
			if (str.charAt(0) == '_') {
				for (int i = JmolConstants.elementNumberMax; --i >= 0;) {
					if (str.equalsIgnoreCase("_" + JmolConstants.elementSymbolFromNumber(i))) {
						viewer.setElementArgb(i, argb);
						return;
					}
				}
				for (int i = JmolConstants.altElementMax; --i >= JmolConstants.firstIsotope;) {
					if (str.equalsIgnoreCase("_" + JmolConstants.altElementSymbolFromIndex(i))) {
						viewer.setElementArgb(JmolConstants.altElementNumberFromIndex(i), argb);
						return;
					}
					if (str.equalsIgnoreCase("_" + JmolConstants.altIsotopeSymbolFromIndex(i))) {
						viewer.setElementArgb(JmolConstants.altElementNumberFromIndex(i), argb);
						return;
					}
				}
			}
			invalidArgument();
		default:
			if (tok == Token.bond) // special hack for bond/bonds confusion
				tok = Token.bonds;
			colorObject(tok, 2);
		}
	}

	void colorNamedObject(int index) throws ScriptException {
		// color $ whatever green
		int shapeType = setShapeByNameParameter(index);
		colorShape(shapeType, index + 1);
	}

	void colorObject(int tokObject, int itoken) throws ScriptException {
		colorShape(getShapeType(tokObject), itoken);
	}

	void colorShape(int shapeType, int itoken) throws ScriptException {
		if (itoken >= statementLength)
			badArgumentCount();
		String translucentOrOpaque = null;
		Object colorvalue = null;
		String colorOrBgcolor = "color";
		int tok = statement[itoken].tok;
		if (tok == Token.background) {
			colorOrBgcolor = "bgcolor";
			++itoken;
			tok = statement[itoken].tok;
		}
		if (tok == Token.translucent || tok == Token.opaque) {
			translucentOrOpaque = (String) (statement[itoken].value);
			++itoken;
		}
		String modifier = "";
		if (shapeType < 0) {
			// geosurface
			shapeType = -shapeType;
			modifier = "Surface";
		}
		if (itoken < statementLength) {
			tok = statement[itoken].tok;
			if (tok == Token.colorRGB) {
				int argb = getArgbParam(itoken);
				colorvalue = (argb == 0 ? null : new Integer(argb));
			}
			else {
				// "cpk" value would be "spacefill"
				byte pid = (tok == Token.cpk ? JmolConstants.PALETTE_CPK : JmolConstants
						.getPaletteID((String) statement[itoken].value));
				if (pid == JmolConstants.PALETTE_UNKNOWN || pid == JmolConstants.PALETTE_TYPE
						&& shapeType != JmolConstants.SHAPE_HSTICKS)
					invalidArgument();
				colorvalue = new Byte(pid);
			}
			// ok, the following five options require precalculation.
			// the state must not save them as paletteIDs, only as pure
			// color values.

			switch (tok) {
			case Token.surfacedistance:
				if (viewer.getFrame().getSurfaceDistanceMax() == 0)
					dots(statementLength, Dots.DOTS_MODE_CALCONLY);
				break;
			case Token.temperature:
				if (viewer.isRangeSelected())
					viewer.clearBfactorRange();
				break;
			case Token.group:
				viewer.calcSelectedGroupsCount();
				break;
			case Token.monomer:
				viewer.calcSelectedMonomersCount();
				break;
			case Token.molecule:
				viewer.calcSelectedMoleculesCount();
				break;
			}
			viewer.loadShape(shapeType);
			if (shapeType == JmolConstants.SHAPE_STICKS)
				viewer.setShapeProperty(shapeType, colorOrBgcolor + modifier, colorvalue, viewer
						.getSelectedAtomsOrBonds());
			else viewer.setShapeProperty(shapeType, colorOrBgcolor + modifier, colorvalue);
		}
		if (translucentOrOpaque != null)
			viewer.setShapeProperty(shapeType, "translucency" + modifier, translucentOrOpaque);
	}

	Hashtable variables = new Hashtable();
	String[] dataLabelString;

	void data() throws ScriptException {
		String dataString = null;
		String dataLabel = null;
		switch (statementLength) {
		case 4:
		case 3:
			dataString = (String) statement[2].value;
		case 2:
			dataLabel = (String) statement[1].value;
			if (dataLabel.equalsIgnoreCase("clear")) {
				viewer.setData(null, null);
				return;
			}
			if (statementLength > 2)
				break;
		default:
			badArgumentCount();
		}
		String dataType = dataLabel + " ";
		dataType = dataType.substring(0, dataType.indexOf(" "));
		dataLabelString = new String[2];
		dataLabelString[0] = dataLabel;
		dataLabelString[1] = dataString;
		viewer.setData(dataType, dataLabelString);
		if (dataType.equalsIgnoreCase("model")) {
			// only if first character is "|" do we consider "|" to be new line
			char newLine = viewer.getInlineChar();
			if (dataString.length() > 0 && dataString.charAt(0) != newLine)
				newLine = '\0';
			viewer.loadInline(dataString, newLine);
			return;
		}
	}

	@SuppressWarnings("unchecked")
	void define() throws ScriptException {
		if (statementLength == 1)
			keywordExpected();
		// note that this definition depends upon the
		// current state.
		String variable = (String) statement[1].value;
		BitSet bs = expression(statement, 2);
		variables.put(variable, bs);
		// viewer.addStateScript("#" + getCommand());
		viewer.setStringProperty("@" + variable, StateManager.escape(bs));
	}

	void echo() {
		String text = "";
		if (statementLength == 2 && statement[1].tok == Token.string)
			text = (String) statement[1].value;
		if (viewer.getEchoStateActive())
			viewer.setShapeProperty(JmolConstants.SHAPE_ECHO, "text", text);
		viewer.scriptEcho(text);
	}

	void message() {
		String text = "";
		if (statementLength == 2 && statement[1].tok == Token.string)
			text = (String) statement[1].value;
		viewer.scriptStatus(text);
	}

	void label() {
		String strLabel = (String) statement[1].value;
		if (strLabel.equalsIgnoreCase("on")) {
			strLabel = viewer.getStandardLabelFormat();
		}
		else if (strLabel.equalsIgnoreCase("off"))
			strLabel = null;
		viewer.loadShape(JmolConstants.SHAPE_LABELS);
		viewer.setLabel(strLabel);
	}

	void hover() {
		if (!viewer.isHoverEnabled())
			viewer.setHoverEnabled(true);
		String strLabel = (String) statement[1].value;
		if (strLabel.equalsIgnoreCase("on")) {
			strLabel = "%U";
		}
		else if (strLabel.equalsIgnoreCase("off")) {
			strLabel = null;
		}
		viewer.loadShape(JmolConstants.SHAPE_HOVER);
		viewer.setShapeProperty(JmolConstants.SHAPE_HOVER, "label", strLabel);
	}

	void load() throws ScriptException {
		StringBuffer loadScript = new StringBuffer("load");
		int[] params = new int[4];
		Point3f unitCells = viewer.getDefaultLattice();
		params[1] = (int) unitCells.x;
		params[2] = (int) unitCells.y;
		params[3] = (int) unitCells.z;
		int i = 1;
		// ignore optional file format
		String filename = "fileset";
		if (statementLength == 1) {
			i = 0;
		}
		else {
			if (statement[1].tok == Token.identifier)
				i = 2;
			if (statement[i].tok != Token.string)
				filenameExpected();
		}
		// long timeBegin = System.currentTimeMillis();
		if (statementLength == i + 1) {
			filename = (String) statement[i].value;
			if (i == 0 || filename.length() == 0)
				filename = viewer.getFullPathName();
			loadScript.append(" " + StateManager.escape(filename) + ";");
			viewer.openFile(filename, params, loadScript.toString());
		}
		else if (statement[i + 1].tok == Token.leftbrace || statement[i + 1].tok == Token.integer) {
			filename = (String) statement[i++].value;
			if (filename.length() == 0)
				filename = viewer.getFullPathName();
			loadScript.append(" " + StateManager.escape(filename));
			if (statement[i].tok == Token.integer) {
				params[0] = statement[i++].intValue;
				loadScript.append(" " + params[0]);
			}
			if (i < statementLength && statement[i].tok == Token.leftbrace) {
				unitCells = getCoordinate(i, false);
				params[1] = (int) unitCells.x;
				params[2] = (int) unitCells.y;
				params[3] = (int) unitCells.z;
				loadScript.append(" " + StateManager.escape(unitCells));
				i = pcLastExpressionInstruction + 1;
				int iGroup = -1;
				int[] p;
				if (i < statementLength && statement[i].tok == Token.spacegroup) {
					++i;
					String spacegroup = viewer.simpleReplace(stringParameter(i++), "''", "\"");
					loadScript.append(" " + StateManager.escape(spacegroup));
					if (spacegroup.equalsIgnoreCase("ignoreOperators")) {
						iGroup = -999;
					}
					else {
						if (spacegroup.indexOf(",") >= 0) // Jones Faithful
							if ((unitCells.x < 9 && unitCells.y < 9 && unitCells.z == 0))
								spacegroup += "#doNormalize=0";
						iGroup = viewer.getSpaceGroupIndexFromName(spacegroup);
						if (iGroup == -1)
							evalError(GT._("space group {0} was not found.", spacegroup));
					}
					p = new int[5];
					for (int j = 0; j < 4; j++)
						p[j] = params[j];
					p[4] = iGroup;
					params = p;
				}
				if (i < statementLength && statement[i].tok == Token.unitcell) {
					++i;
					p = new int[11];
					for (int j = 0; j < params.length; j++)
						p[j] = params[j];
					p[4] = iGroup;
					float[] fparams = new float[6];
					i = floatParameterSet(i, fparams);
					loadScript.append(" {");
					for (int j = 0; j < 6; j++) {
						p[5 + j] = (int) (fparams[j] * 10000f);
						loadScript.append((j == 0 ? "" : " ") + p[5 + j]);
					}
					loadScript.append("}");
					params = p;
				}
			}
			loadScript.append(";");
			viewer.openFile(filename, params, loadScript.toString());
		}
		else {
			String modelName = (String) statement[i].value;
			i++;
			loadScript.append(" " + StateManager.escape(modelName));
			String[] filenames = new String[statementLength - i];
			while (i < statementLength) {
				modelName = (String) statement[i].value;
				filenames[filenames.length - statementLength + i] = modelName;
				loadScript.append(" " + StateManager.escape(modelName));
				i++;
			}
			loadScript.append(";");
			viewer.openFiles(modelName, filenames, loadScript.toString());
		}
		String errMsg = viewer.getOpenFileError();
		// int millis = (int)(System.currentTimeMillis() - timeBegin);
		// Logger.debug("!!!!!!!!! took " + millis + " ms");
		if (errMsg != null)
			evalError(errMsg);
		if (logMessages)
			viewer.scriptStatus("Successfully loaded:" + filename);
		String defaultScript = viewer.getDefaultLoadScript();
		String msg = "";
		if (defaultScript.length() > 0)
			msg += "\nUsing defaultLoadScript: " + defaultScript;
		String script = viewer.getModelSetProperty("jmolscript");
		if (script != null) {
			msg += "\nAdding embedded #jmolscript: " + script;
			defaultScript += ";" + script;
		}
		if (msg.length() > 0)
			Logger.info(msg);
		if (defaultScript.length() > 0)
			runScript(defaultScript);
	}

	// measure() see monitor()

	@SuppressWarnings("unchecked")
	void monitor() throws ScriptException {
		int[] countPlusIndexes = new int[5];
		float[] rangeMinMax = new float[2];
		Token token;
		if (statementLength == 1) {
			viewer.hideMeasurements(false);
			return;
		}
		token = getToken(1);
		switch (statementLength) {
		case 2:
			switch (token.tok) {
			case Token.on:
				viewer.hideMeasurements(false);
				return;
			case Token.off:
				viewer.hideMeasurements(true);
				return;
			case Token.delete:
				viewer.clearAllMeasurements();
				return;
			case Token.string:
				viewer.setMeasurementFormats((String) token.value);
				return;
			default:
				keywordExpected("ON, OFF, or DELETE");
			}
		case 3: // measure delete N
			if (token.tok == Token.delete) {
				if (statement[2].tok == Token.all)
					viewer.clearAllMeasurements();
				else viewer.deleteMeasurement(intParameter(2) - 1);
				return;
			}
		}
		countPlusIndexes[0] = 0;
		int argCount = statementLength - 1;
		int expressionCount = 0;
		int atomIndex = -1;
		int atomNumber = 0;
		int ptFloat = -1;
		rangeMinMax[0] = Float.MAX_VALUE;
		rangeMinMax[1] = Float.MAX_VALUE;
		boolean isAll = false;
		boolean isAllConnected = false;
		boolean isExpression = false;
		boolean isDelete = false;
		boolean isRange = true;
		boolean isON = false;
		boolean isOFF = false;
		String strFormat = null;
		Vector monitorExpressions = new Vector();

		BitSet bs = new BitSet();

		for (int i = 1; i <= argCount; ++i) {
			token = getToken(i);
			switch (token.tok) {
			case Token.on:
				if (isON || isOFF || isDelete)
					invalidArgument();
				isON = true;
				continue;
			case Token.off:
				if (isON || isOFF || isDelete)
					invalidArgument();
				isOFF = true;
				continue;
			case Token.delete:
				if (isON || isOFF || isDelete)
					invalidArgument();
				isDelete = true;
				continue;
			case Token.range:
				isRange = true; // unnecessary
				atomIndex = -1;
				isAll = true;
				continue;
			case Token.identifier:
				if (((String) token.value).equalsIgnoreCase("ALLCONNECTED"))
					isAllConnected = true;
				else keywordExpected("ALL, ALLCONNECTED, or DELETE");
				// fall through
			case Token.all:
				atomIndex = -1;
				isAll = true;
				continue;
			case Token.string:
				// measures "%a1 %a2 %v %u"
				strFormat = (String) token.value;
				continue;
			case Token.decimal:
				isAll = true;
				isRange = true;
				ptFloat = (ptFloat + 1) % 2;
				rangeMinMax[ptFloat] = ((Float) token.value).floatValue();
				continue;
			case Token.integer:
				isRange = true; // irrelevant if just four integers
				atomNumber = token.intValue;
				atomIndex = viewer.getAtomIndexFromAtomNumber(atomNumber);
				ptFloat = (ptFloat + 1) % 2;
				rangeMinMax[ptFloat] = atomNumber;
				break;
			case Token.expressionBegin:
				isExpression = true;
				bs = expression(statement, i);
				atomIndex = viewer.firstAtomOf(bs);
				i = pcLastExpressionInstruction;
				break;
			default:
				expressionOrIntegerExpected();
			}
			// only here for point definition
			if (atomIndex == -1)
				badAtomNumber();
			if (isAll) {
				if (bs == null || bs.size() == 0)
					badAtomNumber();
				if (++expressionCount > 4)
					badArgumentCount();
				monitorExpressions.add(bs);
			}
			else {
				if (++countPlusIndexes[0] > 4)
					badArgumentCount();
				countPlusIndexes[countPlusIndexes[0]] = atomIndex;
			}
		}
		if (isAll) {
			if (!isExpression)
				expressionExpected();
			if (isRange && rangeMinMax[1] < rangeMinMax[0]) {
				rangeMinMax[1] = rangeMinMax[0];
				rangeMinMax[0] = (rangeMinMax[1] == Float.MAX_VALUE ? Float.MAX_VALUE : -200F);
			}
			viewer.defineMeasurement(monitorExpressions, rangeMinMax, isDelete, isAllConnected, isON || isOFF, isOFF,
					strFormat);
		}
		else if (isDelete)
			viewer.deleteMeasurement(countPlusIndexes);
		else if (isON)
			viewer.showMeasurement(countPlusIndexes, true);
		else if (isOFF)
			viewer.showMeasurement(countPlusIndexes, false);
		else viewer.toggleMeasurement(countPlusIndexes, strFormat);
	}

	void refresh() {
		viewer.setTainted(true);
		viewer.requestRepaintAndWait();
	}

	void reset() {
		viewer.reset();
	}

	void initialize() {
		viewer.initialize();
		zap();
	}

	void restrict() throws ScriptException {
		select();
		BitSet bsSelected = copyBitSet(viewer.getSelectionSet());
		viewer.invertSelection();
		if (bsSubset != null) {
			BitSet bs = new BitSet();
			bs.or(bsSelected);
			bs.and(bsSubset);
			viewer.setSelectionSet(bs);
		}
		boolean bondmode = viewer.getBondSelectionModeOr();
		viewer.setBooleanProperty("bondModeOr", true);
		viewer.setShapeSize(JmolConstants.SHAPE_STICKS, 0);

		// also need to turn off backbones, ribbons, strands, cartoons
		for (int shapeType = JmolConstants.SHAPE_MIN_SELECTION_INDEPENDENT; --shapeType >= 0;)
			if (shapeType != JmolConstants.SHAPE_MEASURES)
				viewer.setShapeSize(shapeType, 0);
		viewer.setShapeProperty(JmolConstants.SHAPE_POLYHEDRA, "delete", null);
		viewer.setLabel(null);

		viewer.setBooleanProperty("bondModeOr", bondmode);
		viewer.setSelectionSet(bsSelected);
	}

	void rotate(boolean isSpin) throws ScriptException {

		/*
		 * The Chime spin method:
		 * 
		 * set spin x 10;set spin y 30; set spin z 10; spin | spin ON spin OFF
		 * 
		 * Jmol does these "first x, then y, then z" I don't know what Chime does.
		 * 
		 * spin and rotate are now consolidated here.
		 * 
		 * far simpler is
		 * 
		 * spin x 10 spin y 10
		 * 
		 * these are pure x or y spins or
		 * 
		 * spin axisangle {1 1 0} 10
		 * 
		 * this is the same as the old "spin x 10; spin y 10" -- or is it? anyway, it's better!
		 * 
		 * note that there are many defaults
		 * 
		 * spin # defaults to spin y 10 spin 10 # defaults to spin y 10 spin x # defaults to spin x 10
		 * 
		 * and several new options
		 * 
		 * spin -x spin axisangle {1 1 0} 10 spin 10 (atomno=1)(atomno=2) spin 20 {0 0 0} {1 1 1}
		 * 
		 * spin MOLECULAR {0 0 0} 20
		 * 
		 * The MOLECULAR keyword indicates that spins or rotations are to be carried out in the internal molecular
		 * coordinate frame, not the fixed room frame. Fractional coordinates may be indicated:
		 * 
		 * spin 20 {0 0 0/} {1 1 1/}
		 * 
		 * In association with this, TransformManager and associated functions are TOTALLY REWRITTEN and consolideated.
		 * It is VERY clean now - just two methods here -- one fixed and one molecular, two in Viewer, and two in
		 * TransformManager. All the centering stuff has been carefully inspected are reorganized as well.
		 * 
		 * Bob Hanson 5/21/06
		 * 
		 * 
		 */

		if (statementLength == 2)
			switch (statement[1].tok) {
			case Token.on:
				viewer.setSpinOn(true);
				return;
			case Token.off:
				viewer.setSpinOn(false);
				return;
			}

		float degrees = Float.MIN_VALUE;
		int nPoints = 0;
		float endDegrees = Float.MAX_VALUE;
		boolean isAxisAngle = false;
		boolean isInternal = false;
		Point3f[] points = new Point3f[3];
		Point3f rotCenter = null;
		Vector3f rotAxis = new Vector3f(0, 1, 0);
		String axisID;
		int direction = 1;
		boolean axesOrientationRasmol = viewer.getAxesOrientationRasmol();

		for (int i = 0; i < 3; ++i)
			points[i] = new Point3f(0, 0, 0);
		for (int i = 1; i < statementLength; ++i) {
			Token token = statement[i];
			switch (token.tok) {
			case Token.hyphen:
				direction = -1;
				break;
			case Token.axisangle:
				isAxisAngle = true;
				break;
			case Token.identifier:
				String str = (String) statement[i].value;
				if (str.equalsIgnoreCase("x")) {
					rotAxis.set(direction, 0, 0);
					break;
				}
				if (str.equalsIgnoreCase("y")) {
					if (axesOrientationRasmol)
						direction = -direction;
					rotAxis.set(0, direction, 0);
					break;
				}
				if (str.equalsIgnoreCase("z")) {
					rotAxis.set(0, 0, direction);
					break;
				}
				if (str.equalsIgnoreCase("internal") || str.equalsIgnoreCase("molecular"))
					isInternal = true;
				break;
			case Token.leftbrace:
				// {X, Y, Z}
				Point3f pt = getCoordinate(i, true);
				i = pcLastExpressionInstruction;
				if (isAxisAngle) {
					if (axesOrientationRasmol)
						pt.y = -pt.y;
					rotAxis.set(pt);
					isAxisAngle = false;
				}
				else {
					points[nPoints++].set(pt);
				}
				break;
			case Token.dollarsign:
				// $drawObject
				isInternal = true;
				axisID = objectNameParameter(++i);
				rotCenter = viewer.getDrawObjectCenter(axisID);
				rotAxis = viewer.getDrawObjectAxis(axisID);
				if (rotCenter == null)
					drawObjectNotDefined(axisID);
				points[nPoints++].set(rotCenter);
				break;
			case Token.opOr:
				break;
			case Token.integer:
			case Token.decimal:
				// spin: degrees per second followed by final value
				// rotate: end degrees followed by degrees per second
				// rotate is a full replacement for spin
				// spin is DEPRECATED

				if (degrees == Float.MIN_VALUE)
					degrees = floatParameter(i);
				else {
					endDegrees = degrees;
					degrees = floatParameter(i);
					isSpin = true;
				}
				break;
			case Token.expressionBegin:
				BitSet bs = expression(statement, i + 1);
				rotCenter = viewer.getAtomSetCenter(bs);
				points[nPoints++].set(rotCenter);
				i = pcLastExpressionInstruction;
				break;
			default:
				invalidArgument();
			}
			if (nPoints >= 3) // only 2 allowed for rotation -- for now
				tooManyRotationPoints();
		}
		if (nPoints < 2 && !isInternal) {
			// simple, standard fixed-frame rotation
			// rotate x 10
			// rotate axisangle {0 1 0} 10

			if (nPoints == 1)
				rotCenter = new Point3f(points[0]);

			// point-centered rotation, but not internal -- "frieda"
			// rotate x 10 (atoms)
			// rotate x 10 $object
			// rotate x 10
			if (degrees == Float.MIN_VALUE)
				degrees = 10;
			viewer.rotateAxisAngleAtCenter(rotCenter, rotAxis, degrees, endDegrees, isSpin);
			return;
		}
		if (nPoints < 2) {
			// rotate MOLECULAR
			// rotate MOLECULAR (atom1)
			// rotate MOLECULAR x 10 (atom1)
			// rotate axisangle MOLECULAR (atom1)
			points[1].set(points[0]);
			points[1].sub(rotAxis);
		}
		else {
			// rotate 10 (atom1) (atom2)
			// rotate 10 {x y z} {x y z}
			// rotate 10 (atom1) {x y z}
		}

		if (points[0].distance(points[1]) == 0)
			rotationPointsIdentical();
		if (degrees == Float.MIN_VALUE)
			degrees = 10;
		viewer.rotateAboutPointsInternal(points[0], points[1], degrees, endDegrees, isSpin);
	}

	void script() throws ScriptException {
		// token allows for only 1 parameter
		if (statement[1].tok != Token.string)
			filenameExpected();
		pushContext();
		String filename = stringParameter(1);
		if (!loadScriptFileInternal(filename))
			errorLoadingScript(errorMessage);
		instructionDispatchLoop();
		popContext();
	}

	void history(int pt) throws ScriptException {
		if (statementLength == 1) {
			// show it
			showString(viewer.getSetHistory(Integer.MAX_VALUE));
			return;
		}
		if (pt == 2) {
			// set history n; n' = -2 - n; if n=0, then set history OFF
			checkLength3();
			int n = intParameter(2);
			if (n < 0)
				invalidArgument();
			viewer.getSetHistory(n == 0 ? 0 : -2 - n);
			return;
		}
		switch (statement[1].tok) {
		// pt = 1 history ON/OFF/CLEAR
		case Token.on:
		case Token.clear:
			viewer.getSetHistory(Integer.MIN_VALUE);
			return;
		case Token.off:
			viewer.getSetHistory(0);
			break;
		default:
			keywordExpected();
		}
	}

	void hide() throws ScriptException {
		viewer.hide(statementLength == 1 ? null : expression(statement, 1), tQuiet);
	}

	void display() throws ScriptException {
		viewer.display(bsAll(), statementLength == 1 ? null : expression(statement, 1), tQuiet);
	}

	BitSet bsAll() {
		int atomCount = viewer.getAtomCount();
		BitSet bs = new BitSet(atomCount);
		for (int i = atomCount; --i >= 0;)
			bs.set(i);
		return bs;
	}

	void select() throws ScriptException {
		// NOTE this is called by restrict()
		if (statementLength == 5 && statement[2].tok == Token.bonds && statement[3].tok == Token.bitset) {
			viewer.selectBonds((BitSet) statement[3].value);
			return;
		}

		if (statementLength == 5 && statement[2].tok == Token.monitor && statement[3].tok == Token.bitset) {
			viewer.setShapeProperty(JmolConstants.SHAPE_MEASURES, "select", statement[3].value);
			return;
		}

		viewer.select(statementLength == 1 ? null : expression(statement, 1), tQuiet || isExpressionBitSet);
	}

	void subset() throws ScriptException {
		bsSubset = (statementLength == 1 ? null : expression(statement, -1));
		viewer.setSelectionSubset(bsSubset);
	}

	void translate() throws ScriptException {
		if (statementLength < 3)
			badArgumentCount();
		if (statement[2].tok != Token.integer)
			integerExpected();
		int percent = statement[2].intValue;
		if (percent > 100 || percent < -100)
			numberOutOfRange(-100, 100);
		if (statement[1].tok == Token.identifier) {
			String str = (String) statement[1].value;
			if (str.equalsIgnoreCase("x")) {
				viewer.translateToXPercent(percent);
				return;
			}
			if (str.equalsIgnoreCase("y")) {
				viewer.translateToYPercent(percent);
				return;
			}
			if (str.equalsIgnoreCase("z")) {
				viewer.translateToZPercent(percent);
				return;
			}
		}
		axisExpected();
	}

	void translateSelected() throws ScriptException {
		// translateSelected {x y z}
		viewer.setAtomCoordRelative(getCoordinate(1, true));
	}

	void zap() {
		viewer.zap();
		refresh();
	}

	void zoom(boolean isZoomTo) throws ScriptException {
		// zoom
		if (statementLength == 1) {
			if (isZoomTo)
				viewer.moveTo(1, null, new Point3f(0, 0, 0), 0, viewer.getZoomPercentFloat() * 2f, 0, 0, 0);
			else viewer.setBooleanProperty("zoomEnabled", true);
			return;
		}
		// zoom on|off
		if (!isZoomTo)
			switch (statement[1].tok) {
			case Token.on:
				viewer.setBooleanProperty("zoomEnabled", true);
				return;
			case Token.off:
				viewer.setBooleanProperty("zoomEnabled", false);
				return;
			}
		float time = (isZoomTo ? 1f : 0f);
		float zoom = viewer.getZoomPercentFloat();
		float factor = 0;
		float radius = viewer.getRotationRadius();
		Point3f center = null;
		Point3f currentCenter = viewer.getRotationCenter();
		int i = 1;
		// zoomTo time-sec
		if (isFloatParameter(i) && isZoomTo)
			time = floatParameter(i++);
		// zoom {x y z} or (atomno=3)
		int ptCenter = 0;
		if (isAtomCenterOrCoordinateNext(i)) {
			ptCenter = i;
			center = atomCenterOrCoordinateParameter(i);
			i = pcLastExpressionInstruction + 1;
		}

		boolean isSameAtom = (center != null && currentCenter.distance(center) < 0.1);

		// zoom/zoomTo percent|-factor|+factor|*factor|/factor
		if (isFloatParameter(i))
			factor = floatParameter(i++);
		if (factor < 0)
			factor += zoom;
		if (factor == 0) {
			factor = zoom;
			if (isFloatParameter(i + 1)) {
				float value = floatParameter(i + 1);
				switch (statement[i].tok) {
				case Token.slash:
					factor /= value;
					break;
				case Token.asterisk:
					factor *= value;
					break;
				case Token.plus:
					factor += value;
					break;
				default:
					evalError(GT._("Invalid {0} command", "ZOOM"));
				}
			}
			else if (isZoomTo) {
				// no factor -- check for no center (zoom out) or same center (zoom in)
				if (center == null)
					factor /= 2;
				else if (isSameAtom)
					factor *= 2;
			}
		}
		float xTrans = 0;
		float yTrans = 0;
		float max = viewer.getMaxZoomPercent();
		if (factor < 5 || factor > max)
			numberOutOfRange(5, max);
		if (!viewer.isWindowCentered()) {
			// do a smooth zoom only if not windowCentered
			if (center != null)
				viewer.setCenterBitSet(expression(statement, ptCenter), false);
			center = viewer.getRotationCenter();
			xTrans = viewer.getTranslationXPercent();
			yTrans = viewer.getTranslationYPercent();
		}
		viewer.moveTo(time, center, new Point3f(0, 0, 0), Float.NaN, factor, xTrans, yTrans, radius);
	}

	void delay() throws ScriptException {
		long millis = 0;
		// token has ondefault1
		Token token = statement[1];
		switch (token.tok) {
		case Token.integer:
		case Token.on: // this is auto-provided as a default
			millis = token.intValue * 1000;
			break;
		case Token.decimal:
			millis = (long) (((Float) token.value).floatValue() * 1000);
			break;
		default:
			numberExpected();
		}
		delay(millis);
	}

	void delay(long millis) {
		long timeBegin = System.currentTimeMillis();
		refresh();
		millis -= System.currentTimeMillis() - timeBegin;
		int seconds = (int) millis / 1000;
		millis -= seconds * 1000;
		if (millis <= 0)
			millis = 1;
		while (seconds >= 0 && millis > 0 && !interruptExecution.booleanValue()
				&& currentThread == Thread.currentThread()) {
			viewer.popHoldRepaint();
			try {
				Thread.sleep((seconds--) > 0 ? 1000 : millis);
			}
			catch (InterruptedException e) {
			}
			viewer.pushHoldRepaint();
		}
	}

	void move() throws ScriptException {
		if (statementLength < 10 || statementLength > 12)
			badArgumentCount();
		// rotx roty rotz, transx transy transz slab seconds fps
		Vector3f dRot = new Vector3f(floatParameter(1), floatParameter(2), floatParameter(3));
		int dZoom = intParameter(4);
		Vector3f dTrans = new Vector3f(intParameter(5), intParameter(6), intParameter(7));
		int dSlab = intParameter(8);
		float floatSecondsTotal = floatParameter(9);
		int fps = 30/* , maxAccel = 5 */;
		if (statementLength > 10) {
			fps = statement[10].intValue;
			if (statementLength > 11) {
				// maxAccel = statement[11].intValue;
			}
		}
		refresh();
		viewer.move(dRot, dZoom, dTrans, dSlab, floatSecondsTotal, fps);
	}

	void slab() throws ScriptException {
		// token has ondefault1
		if (statement[1].tok == Token.integer) {
			int percent = statement[1].intValue;
			if (percent < 0 || percent > 100)
				numberOutOfRange(0, 100);
			viewer.slabToPercent(percent);
			return;
		}
		switch (statement[1].tok) {
		case Token.on:
			viewer.setBooleanProperty("slabEnabled", true);
			break;
		case Token.off:
			viewer.setBooleanProperty("slabEnabled", false);
			break;
		default:
			booleanOrPercentExpected();
		}
	}

	void depth() throws ScriptException {
		viewer.depthToPercent(intParameter(1));
	}

	void star() throws ScriptException {
		short mad = 0; // means back to selection business
		int tok = Token.on;
		if (statementLength > 1) {
			tok = statement[1].tok;
			if (!((statementLength == 2) || (statementLength == 3 && tok == Token.integer && statement[2].tok == Token.percent))) {
				badArgumentCount();
			}
		}
		switch (tok) {
		case Token.on:
		case Token.vanderwaals:
			mad = -100; // cpk with no args goes to 100%
			break;
		case Token.off:
			break;
		case Token.integer:
			int radiusRasMol = statement[1].intValue;
			if (statementLength == 2) {
				if (radiusRasMol >= 750 || radiusRasMol < -100)
					numberOutOfRange(-100, 749);
				mad = (short) radiusRasMol;
				if (radiusRasMol > 0)
					mad *= 4 * 2;
			}
			else {
				if (radiusRasMol < 0 || radiusRasMol > 100)
					numberOutOfRange(0, 100);
				mad = (short) -radiusRasMol; // use a negative number to specify %vdw
			}
			break;
		case Token.decimal:
			float angstroms = floatParameter(1);
			if (angstroms < 0 || angstroms > 3)
				numberOutOfRange(0f, 3f);
			mad = (short) (angstroms * 1000 * 2);
			break;
		case Token.temperature:
			mad = -1000;
			break;
		case Token.ionic:
			mad = -1001;
			break;
		default:
			booleanOrNumberExpected();
		}
		viewer.setShapeSize(JmolConstants.SHAPE_STARS, mad);
	}

	void halo() throws ScriptException {
		short mad = 0;
		int tok = Token.on;
		if (statementLength > 1) {
			tok = statement[1].tok;
			if (!((statementLength == 2) || (statementLength == 3 && tok == Token.integer && statement[2].tok == Token.percent))) {
				badArgumentCount();
			}
		}
		switch (tok) {
		case Token.on:
			mad = -20; // on goes to 25%
			break;
		case Token.vanderwaals:
			mad = -100; // cpk with no args goes to 100%
			break;
		case Token.off:
			break;
		case Token.integer:
			int radiusRasMol = statement[1].intValue;
			if (statementLength == 2) {
				if (radiusRasMol >= 750 || radiusRasMol < -100)
					numberOutOfRange(-100, 749);
				mad = (short) radiusRasMol;
				if (radiusRasMol > 0)
					mad *= 4 * 2;
			}
			else {
				if (radiusRasMol < 0 || radiusRasMol > 100)
					numberOutOfRange(0, 100);
				mad = (short) -radiusRasMol; // use a negative number to specify %vdw
			}
			break;
		case Token.decimal:
			float angstroms = floatParameter(1);
			if (angstroms < 0 || angstroms > 3)
				numberOutOfRange(0f, 3f);
			mad = (short) (angstroms * 1000 * 2);
			break;
		case Token.temperature:
			mad = -1000;
			break;
		case Token.ionic:
			mad = -1001;
			break;
		default:
			booleanOrNumberExpected();
		}
		viewer.setShapeSize(JmolConstants.SHAPE_HALOS, mad);
	}

	// / aka cpk
	void spacefill() throws ScriptException {
		short mad = 0;
		boolean isSolventAccessibleSurface = false;
		int tok = Token.on;
		if (statementLength > 1) {
			tok = statement[1].tok;
			if (!(statementLength == 2 || (statementLength == 3 && (tok == Token.plus && isFloatParameter(2) || tok == Token.integer
					&& statement[2].tok == Token.percent)))) {
				badArgumentCount();
			}
		}
		int i = 1;
		switch (tok) {
		case Token.on:
		case Token.vanderwaals:
			mad = -100; // cpk with no args goes to 100%
			break;
		case Token.off:
			break;
		case Token.plus:
			isSolventAccessibleSurface = true;
			i++;
			// integer would be OK here as well, thus "floatParameter(i)"
		case Token.decimal:
			float angstroms = floatParameter(i);
			if (angstroms < 0 || angstroms > 3)
				numberOutOfRange(0f, 3f);
			mad = (short) (angstroms * 1000 * 2);
			if (isSolventAccessibleSurface)
				mad += 10000;
			break;
		case Token.integer:
			int radiusRasMol = statement[1].intValue;
			if (statementLength == 2) {
				if (radiusRasMol >= 750 || radiusRasMol < -200)
					numberOutOfRange(-200, 749);
				mad = (short) radiusRasMol;
				if (radiusRasMol > 0)
					mad *= 4 * 2;
			}
			else {
				if (radiusRasMol < 0 || radiusRasMol > 200)
					numberOutOfRange(0, 200);
				mad = (short) -radiusRasMol; // use a negative number to specify %vdw
			}
			break;
		case Token.temperature:
			mad = -1000;
			break;
		case Token.ionic:
			mad = -1001;
			break;
		default:
			booleanOrNumberExpected();
		}
		viewer.setShapeSize(JmolConstants.SHAPE_BALLS, mad);
	}

	void wireframe() throws ScriptException {
		viewer.setShapeSize(JmolConstants.SHAPE_STICKS, getMadParameter(), viewer.getSelectedAtomsOrBonds());
	}

	void ssbond() throws ScriptException {
		viewer.loadShape(JmolConstants.SHAPE_SSSTICKS);
		viewer.setShapeSize(JmolConstants.SHAPE_SSSTICKS, getMadParameter());
	}

	void hbond(boolean isCommand) throws ScriptException {
		if (statementLength == 2 && statement[1].tok == Token.calculate) {
			viewer.autoHbond();
			return;
		}
		viewer.setShapeSize(JmolConstants.SHAPE_HSTICKS, getMadParameter());
	}

	void configuration() throws ScriptException {
		if (viewer.getDisplayModelIndex() <= -2)
			evalError(GT._("{0} not allowed with background model displayed", "\"CONFIGURATION\""));
		BitSet bsConfigurations;
		if (statementLength == 1) {
			bsConfigurations = viewer.setConformation();
			viewer.addStateScript("configuration;");
		}
		else {
			checkLength2();
			int n = intParameter(1);
			bsConfigurations = viewer.setConformation(n - 1);
			viewer.addStateScript("configuration " + n + ";");
		}
		boolean addHbonds = viewer.hbondsAreVisible();
		viewer.setShapeSize(JmolConstants.SHAPE_HSTICKS, 0, bsConfigurations);
		if (addHbonds)
			viewer.autoHbond(bsConfigurations, bsConfigurations);
		viewer.select(bsConfigurations, tQuiet);
	}

	void vector() throws ScriptException {
		short mad = 1;
		if (statementLength > 1) {
			switch (statement[1].tok) {
			case Token.on:
				break;
			case Token.off:
				mad = 0;
				break;
			case Token.integer:
				int diameterPixels = statement[1].intValue;
				if (diameterPixels < 0 || diameterPixels >= 20)
					numberOutOfRange(0, 19);
				mad = (short) diameterPixels;
				break;
			case Token.decimal:
				float angstroms = floatParameter(1);
				if (angstroms > 3)
					numberOutOfRange(0f, 3f);
				mad = (short) (angstroms * 1000 * 2);
				break;
			case Token.identifier:
				String cmd = (String) statement[1].value;
				if (cmd.equalsIgnoreCase("scale")) {
					checkLength3();
					float scale = floatParameter(2);
					if (scale < -10 || scale > 10)
						numberOutOfRange(-10f, 10f);
					viewer.loadShape(JmolConstants.SHAPE_VECTORS);
					viewer.setShapeProperty(JmolConstants.SHAPE_VECTORS, "scale", new Float(scale));
					return;
				}
				unrecognizedSubcommand(cmd);
			default:
				booleanOrNumberExpected();
			}
			checkLength2();
		}
		viewer.setShapeSize(JmolConstants.SHAPE_VECTORS, mad);
	}

	void dipole() throws ScriptException {
		// dipole intWidth floatMagnitude OFFSET floatOffset {atom1} {atom2}
		String propertyName = null;
		Object propertyValue = null;
		boolean iHaveAtoms = false;
		boolean iHaveCoord = false;

		viewer.loadShape(JmolConstants.SHAPE_DIPOLES);
		viewer.setShapeProperty(JmolConstants.SHAPE_DIPOLES, "init", null);
		if (statementLength == 1) {
			viewer.setShapeProperty(JmolConstants.SHAPE_DIPOLES, "thisID", null);
			return;
		}
		for (int i = 1; i < statementLength; ++i) {
			propertyName = null;
			propertyValue = null;
			Token token = statement[i];
			switch (token.tok) {
			case Token.on:
				propertyName = "on";
				break;
			case Token.off:
				propertyName = "off";
				break;
			case Token.delete:
				propertyName = "delete";
				break;
			case Token.integer:
			case Token.decimal:
				propertyName = "dipoleValue";
				propertyValue = new Float(floatParameter(i));
				break;
			case Token.expressionBegin:
				propertyName = (statement[i + 1].tok == Token.bitset ? "atomBitset"
						: iHaveAtoms || iHaveCoord ? "endSet" : "startSet");
				propertyValue = expression(statement, i);
				i = pcLastExpressionInstruction;
				iHaveAtoms = true;
				break;
			case Token.leftbrace:
				// {X, Y, Z}
				Point3f pt = getCoordinate(i, true);
				i = pcLastExpressionInstruction;
				propertyName = (iHaveAtoms || iHaveCoord ? "endCoord" : "startCoord");
				propertyValue = pt;
				iHaveCoord = true;
				break;
			case Token.bond:
			case Token.bonds:
				propertyName = "bonds";
				break;
			case Token.calculate:
				continue; // ignored
			case Token.identifier:
				String cmd = (String) token.value;
				if (cmd.equalsIgnoreCase("cross")) {
					propertyName = "cross";
					propertyValue = Boolean.TRUE;
					break;
				}
				if (cmd.equalsIgnoreCase("noCross")) {
					propertyName = "cross";
					propertyValue = Boolean.FALSE;
					break;
				}
				if (cmd.equalsIgnoreCase("offset")) {
					float v = floatParameter(++i);
					if (statement[i].tok == Token.integer) {
						propertyName = "dipoleOffsetPercent";
						propertyValue = new Integer((int) v);
					}
					else {
						propertyName = "dipoleOffset";
						propertyValue = new Float(v);
					}
					break;
				}
				if (cmd.equalsIgnoreCase("value")) {
					propertyName = "dipoleValue";
					propertyValue = new Float(floatParameter(++i));
					break;
				}
				if (cmd.equalsIgnoreCase("offsetSide")) {
					propertyName = "offsetSide";
					propertyValue = new Float(floatParameter(++i));
					break;
				}
				if (cmd.equalsIgnoreCase("width")) {
					propertyName = "dipoleWidth";
					propertyValue = new Float(floatParameter(++i));
					break;
				}
				propertyName = "thisID"; // might be "molecular"
				propertyValue = ((String) token.value).toLowerCase();
				break;
			default:
				invalidArgument();
			}
			if (propertyName != null)
				viewer.setShapeProperty(JmolConstants.SHAPE_DIPOLES, propertyName, propertyValue);
		}
		if (iHaveCoord || iHaveAtoms)
			viewer.setShapeProperty(JmolConstants.SHAPE_DIPOLES, "set", null);
	}

	void animationMode() throws ScriptException {
		float startDelay = 1, endDelay = 1;
		if (statementLength < 3 || statementLength > 5)
			badArgumentCount();
		int animationMode = 0;
		switch (statement[2].tok) {
		case Token.loop:
			++animationMode;
			break;
		case Token.identifier:
			String cmd = (String) statement[2].value;
			if (cmd.equalsIgnoreCase("once")) {
				startDelay = endDelay = 0;
				break;
			}
			if (cmd.equalsIgnoreCase("palindrome")) {
				animationMode = 2;
				break;
			}
			unrecognizedSubcommand(cmd);
		}
		if (statementLength >= 4) {
			startDelay = endDelay = floatParameter(3);
			if (statementLength == 5)
				endDelay = floatParameter(4);
		}
		viewer.setAnimationReplayMode(animationMode, startDelay, endDelay);
	}

	void vibration() throws ScriptException {
		if (statementLength < 2)
			subcommandExpected();
		Token token = statement[1];
		float period = 0;
		switch (token.tok) {
		case Token.on:
			period = viewer.getDefaultVibrationPeriod();
			break;
		case Token.off:
		case Token.integer:
			period = token.intValue;
			break;
		case Token.decimal:
			period = floatParameter(1);
			break;
		case Token.identifier:
			String cmd = (String) statement[1].value;
			if (cmd.equalsIgnoreCase("scale")) {
				checkLength3();
				float scale = floatParameter(2);
				if (scale < -10 || scale > 10)
					numberOutOfRange(-10f, 10f);
				viewer.setFloatProperty("vibrationScale", scale);
				return;
			}
		default:
			unrecognizedSubcommand(token.toString());
		}
		viewer.setFloatProperty("vibrationPeriod", period);
	}

	void animationDirection() throws ScriptException {
		checkStatementLength(4);
		boolean negative = false;
		if (statement[2].tok == Token.hyphen)
			negative = true;
		else if (statement[2].tok != Token.plus)
			invalidArgument();
		if (statement[3].tok != Token.integer)
			invalidArgument();
		int direction = statement[3].intValue;
		if (direction != 1)
			numberMustBe(-1, 1);
		if (negative)
			direction = -direction;
		viewer.setAnimationDirection(direction);
	}

	void calculate() throws ScriptException {
		if (statementLength == 1)
			evalError(GT._("Calculate what?") + "hbonds?  surface? structure?");
		switch (statement[1].tok) {
		case Token.surface:
			dots(2, Dots.DOTS_MODE_CALCONLY);
			viewer.addStateScript("calculate surface");
			return;
		case Token.hbond:
			viewer.autoHbond();
			return;
		case Token.structure:
			viewer.calculateStructures();
			return;
		}
	}

	void dots(int ipt, int dotsMode) throws ScriptException {
		viewer.loadShape(JmolConstants.SHAPE_DOTS);
		viewer.setShapeProperty(JmolConstants.SHAPE_DOTS, "init", new Integer(dotsMode));
		if (statementLength == ipt) {
			viewer.setShapeSize(JmolConstants.SHAPE_DOTS, 1);
			return;
		}
		short mad = 0;
		float radius;
		switch (statement[ipt].tok) {
		case Token.on:
		case Token.vanderwaals:
			mad = 1;
			break;
		case Token.ionic:
			mad = -1;
			break;
		case Token.off:
			break;
		case Token.plus:
			radius = floatParameter(++ipt);
			if (radius < 0f || radius > 10f)
				numberOutOfRange(0f, 2f);
			mad = (short) (radius == 0f ? 0 : radius * 1000f + 11002);
			break;
		case Token.decimal:
			radius = floatParameter(ipt);
			if (radius < 0f || radius > 10f)
				numberOutOfRange(0f, 10f);
			mad = (short) (radius == 0f ? 0 : radius * 1000f + 1002);
			break;
		case Token.integer:
			int dotsParam = intParameter(ipt);
			if (statementLength > ipt + 1 && statement[ipt + 1].tok == Token.radius) {
				viewer.setShapeProperty(JmolConstants.SHAPE_DOTS, "atom", new Integer(dotsParam));
				ipt++;
				viewer.setShapeProperty(JmolConstants.SHAPE_DOTS, "radius", new Float(floatParameter(++ipt)));
				if (statementLength > ipt + 1 && statement[++ipt].tok == Token.color)
					viewer.setShapeProperty(JmolConstants.SHAPE_DOTS, "colorRGB", new Integer(getArgbParam(++ipt)));
				if (statement[++ipt].tok != Token.bitset)
					invalidArgument();
				viewer.setShapeProperty(JmolConstants.SHAPE_DOTS, "dots", statement[ipt].value);
				return;
			}

			if (dotsParam < 0 || dotsParam > 1000)
				numberOutOfRange(0, 1000);
			mad = (short) (dotsParam == 0 ? 0 : dotsParam + 1);
			break;
		default:
			booleanOrNumberExpected();
		}
		viewer.setShapeSize(JmolConstants.SHAPE_DOTS, mad);
	}

	void proteinShape(int shapeType) throws ScriptException {
		short mad = 0;
		// token has ondefault1
		int tok = statement[1].tok;
		switch (tok) {
		case Token.on:
			mad = -1; // means take default
			break;
		case Token.off:
			break;
		case Token.structure:
			mad = -2;
			break;
		case Token.temperature:
			// MTH 2004 03 15
			// Let temperature return the mean positional displacement
			// see what people think
			// mad = -3;
			// break;
		case Token.displacement:
			mad = -4;
			break;
		case Token.integer:
			int radiusRasMol = statement[1].intValue;
			if (radiusRasMol >= 500)
				numberOutOfRange(0, 499);
			mad = (short) (radiusRasMol * 4 * 2);
			break;
		case Token.decimal:
			float angstroms = ((Float) statement[1].value).floatValue();
			if (angstroms > 4)
				numberOutOfRange(0f, 4f);
			mad = (short) (angstroms * 1000 * 2);
			break;
		default:
			booleanOrNumberExpected();
		}
		viewer.setShapeSize(shapeType, mad);
	}

	void animation() throws ScriptException {
		if (statementLength < 2)
			subcommandExpected();
		int tok = statement[1].tok;
		boolean animate = false;
		switch (tok) {
		case Token.on:
			animate = true;
		case Token.off:
			viewer.setAnimationOn(animate);
			break;
		case Token.frame:
			frame(2, false);
			break;
		case Token.mode:
			animationMode();
			break;
		case Token.direction:
			animationDirection();
			break;
		case Token.identifier:
			String str = (String) statement[1].value;
			if (str.equalsIgnoreCase("fps")) {
				checkLength3();
				viewer.setIntProperty("animationFps", intParameter(2));
				break;
			}
		default:
			frameControl(statement[1], true);
		}
	}

	void frame(int offset, boolean useModelNumber) throws ScriptException {
		useModelNumber = true;
		// for now -- as before -- remove to implement
		// frame/model difference
		if (statementLength <= offset)
			badArgumentCount();
		if (statement[offset].tok == Token.hyphen) {
			++offset;
			checkStatementLength(offset + 1);
			if (statement[offset].tok != Token.integer || statement[offset].intValue != 1)
				invalidArgument();
			viewer.setAnimationPrevious();
			return;
		}
		int frameNumber = -1;
		int frameNumber2 = -1;
		boolean isPlay = false;
		boolean isRange = false;
		boolean isAll = false;
		while (offset < statementLength) {
			Token cmd = statement[offset];
			int tok = cmd.tok;
			switch (tok) {
			case Token.all:
				isAll = true;
			case Token.asterisk:
				break;
			case Token.none:
				break;
			case Token.integer:
				if (frameNumber == -1) {
					frameNumber = cmd.intValue;
				}
				else {
					frameNumber2 = cmd.intValue;
				}
				break;
			case Token.play:
				isPlay = true;
				break;
			case Token.range:
				isRange = true;
				break;
			default:
				frameControl(cmd, false);
				return;
			}

			if (offset == statementLength - 1) {
				if (isAll) {
					viewer.setAnimationRange(-1, -1);
					viewer.setDisplayModelIndex(-1);
					return;
				}
				int modelIndex = (useModelNumber ? viewer.getModelNumberIndex(frameNumber) : frameNumber - 1);
				if (!isPlay && !isRange || modelIndex >= 0) {
					viewer.setDisplayModelIndex(modelIndex);
				}
				if (isPlay || isRange) {
					if (isRange || frameNumber2 >= 0) {
						int modelIndex2 = (useModelNumber ? viewer.getModelNumberIndex(frameNumber2) : frameNumber2 - 1);
						viewer.setAnimationDirection(1);
						viewer.setAnimationRange(modelIndex, modelIndex2);
					}
					if (isPlay)
						viewer.resumeAnimation();
				}
			}
			offset++;
		}
	}

	void frameControl(Token token, boolean isSubCmd) throws ScriptException {
		switch (token.tok) {
		case Token.playrev:
			viewer.reverseAnimation();
		case Token.play:
		case Token.resume:
			viewer.resumeAnimation();
			return;
		case Token.pause:
			viewer.pauseAnimation();
			return;
		case Token.next:
			viewer.setAnimationNext();
			return;
		case Token.prev:
			viewer.setAnimationPrevious();
			return;
		case Token.rewind:
			viewer.rewindAnimation();
			return;
		default:
			evalError(GT._("invalid {0} control keyword", "frame") + ": " + token.toString());
		}
	}

	int getShapeType(int tok) throws ScriptException {
		if (tok == Token.geosurface)
			return -JmolConstants.shapeTokenIndex(Token.dots);
		int iShape = JmolConstants.shapeTokenIndex(tok);
		if (iShape < 0)
			unrecognizedObject();
		return iShape;
	}

	void font() throws ScriptException {
		int shapeType = 0;
		int fontsize = 0;
		String fontface = "SansSerif";
		String fontstyle = "Plain";
		switch (statementLength) {
		case 5:
			if (statement[4].tok != Token.identifier)
				keywordExpected();
			fontstyle = (String) statement[4].value;
		case 4:
			if (statement[3].tok != Token.identifier)
				keywordExpected();
			fontface = (String) statement[3].value;
		case 3:
			if (statement[2].tok != Token.integer)
				integerExpected();
			fontsize = statement[2].intValue;
			shapeType = getShapeType(statement[1].tok);
			break;
		default:
			badArgumentCount();
		}
		Font3D font3d = viewer.getFont3D(fontface, fontstyle, fontsize);
		viewer.setShapeProperty(shapeType, "font", font3d);
	}

	/*******************************************************************************************************************
	 * ============================================================== SET implementations
	 * ==============================================================
	 */

	void set() throws ScriptException {
		switch (statement[1].tok) {
		case Token.axes:
			setAxes(2);
			break;
		case Token.bondmode:
			setBondmode();
			break;
		case Token.boundbox:
			setBoundbox(2);
			break;
		case Token.color:
		case Token.defaultColors:
			setDefaultColors();
			break;
		case Token.display:// deprecated
		case Token.selectionHalo:
			setSelectionHalo(2);
			break;
		case Token.echo:
			setEcho();
			break;
		case Token.fontsize:
			setFontsize();
			break;
		case Token.hbond:
			setHbond();
			break;
		case Token.history:
			history(2);
			break;
		case Token.monitor:
			setMonitor(2);
			break;
		case Token.property: // huh? why?
			setProperty();
			break;
		case Token.scale3d:
			setScale3d();
			break;
		case Token.strands:
			setStrands();
			break;
		case Token.spin:
			setSpin();
			break;
		case Token.ssbond:
			setSsbond();
			break;
		case Token.unitcell:
			setUnitcell(2);
			break;
		case Token.picking:
			setPicking();
			break;
		case Token.pickingStyle:
			setPickingStyle();
			break;
		case Token.formalCharge:
			viewer.setFormalCharges(intParameter(2));
			break;

		// not implemented
		case Token.backfade:
		case Token.cartoon:
		case Token.hourglass:
		case Token.kinemage:
		case Token.menus:
		case Token.mouse:
		case Token.shadow:
		case Token.slabmode:
		case Token.transparent:
		case Token.vectps:
		case Token.write:
			// fall through to identifier

		case Token.ambient:
		case Token.bonds:
		case Token.debugscript:
		case Token.diffuse:
		case Token.frank:
		case Token.help:
		case Token.hetero:
		case Token.hydrogen:
		case Token.identifier:
		case Token.radius:
		case Token.solvent:
		case Token.specular:
		case Token.specpower:
			String str = (String) statement[1].value;
			if (str.toLowerCase().indexOf("label") == 0) {
				setLabel(str.substring(5));
				break;
			}
			if (str.equalsIgnoreCase("toggleLabel")) {
				viewer.togglePickingLabel(expression(statement, 2));
				break;
			}
			if (str.equalsIgnoreCase("measurementNumbers")) {
				setMonitor(2);
				break;
			}
			if (str.equalsIgnoreCase("historyLevel")) {
				commandHistoryLevelMax = intParameter(2);
				break;
			}
			if (str.equalsIgnoreCase("defaultLattice")) {
				if (statementLength < 3)
					badArgumentCount();
				Point3f pt;
				if (statement[2].tok == Token.integer) {
					int i = statement[2].intValue;
					pt = new Point3f(i, i, i);
				}
				else {
					pt = getCoordinate(2, false);
				}
				viewer.setDefaultLattice(pt);
				break;
			}
			if (str.equalsIgnoreCase("dipoleScale")) {
				checkLength3();
				float scale = floatParameter(2);
				if (scale < -10 || scale > 10)
					numberOutOfRange(-10f, 10f);
				viewer.setFloatProperty("dipoleScale", scale);
				break;
			}
			if (str.equalsIgnoreCase("logLevel")) {
				// set logLevel n
				// we have 5 levels 0 - 4 debug -- error
				// n = 0 -- no messages -- turn all off
				// n = 1 add level 4, error
				// n = 2 add level 3, warn
				// etc.

				int ilevel = intParameter(2);
				Viewer.setLogLevel(ilevel);
				Logger.info("logging level set to " + ilevel);
				break;
			}
			if (statementLength == 2) {
				viewer.setBooleanProperty((String) statement[1].value, booleanParameter(2));
				break;
			}
			checkLength3();
			int tok = statement[2].tok;
			if (tok == Token.decimal) {
				viewer.setFloatProperty((String) statement[1].value, ((Float) statement[2].value).floatValue());
				break;
			}
			if (tok == Token.integer) {
				viewer.setIntProperty((String) statement[1].value, statement[2].intValue);
				break;
			}
			if (tok != Token.on && tok != Token.off && statement[2].value instanceof String) {
				viewer.setStringProperty((String) statement[1].value, (String) statement[2].value);
				break;
			}
			viewer.setBooleanProperty((String) statement[1].value, booleanParameter(2));
			break;
		default:
			unrecognizedSetParameter();
		}
	}

	void setAxes(int cmdPt) throws ScriptException {
		if (statementLength == 1) {
			viewer.setShapeSize(JmolConstants.SHAPE_AXES, 1);
			return;
		}
		// set axes scale x.xxx
		if (statementLength == cmdPt + 2 && statement[cmdPt].tok == Token.identifier
				&& ((String) statement[cmdPt].value).equalsIgnoreCase("scale")) {
			viewer.setShapeProperty(JmolConstants.SHAPE_AXES, "scale", new Float(floatParameter(cmdPt + 1)));
			return;
		}
		viewer.setShapeSize(JmolConstants.SHAPE_AXES, getSetAxesTypeMad(cmdPt));
	}

	void setBoundbox(int cmdPt) throws ScriptException {
		viewer.setShapeSize(JmolConstants.SHAPE_BBCAGE, getSetAxesTypeMad(cmdPt));
	}

	void setUnitcell(int cmdPt) throws ScriptException {
		if (statementLength == cmdPt + 1) {
			if (statement[cmdPt].tok == Token.integer && statement[cmdPt].intValue >= 111)
				viewer.setCurrentUnitCellOffset(intParameter(cmdPt));
			else viewer.setShapeSize(JmolConstants.SHAPE_UCCAGE, getSetAxesTypeMad(cmdPt));
			return;
		}
		viewer.setCurrentUnitCellOffset(getCoordinate(cmdPt, true, false, true));
	}

	void setFrank(int cmdPt) throws ScriptException {
		viewer.setBooleanProperty("frank", booleanParameter(cmdPt));
	}

	void setDefaultColors() throws ScriptException {
		checkLength3();
		switch (statement[2].tok) {
		case Token.rasmol:
		case Token.jmol:
			viewer.setStringProperty("defaultColorScheme", (String) statement[2].value);
			break;
		default:
			invalidArgument();
		}
	}

	void setBondmode() throws ScriptException {
		checkLength3();
		boolean bondmodeOr = false;
		switch (statement[2].tok) {
		case Token.opAnd:
			break;
		case Token.opOr:
			bondmodeOr = true;
			break;
		default:
			invalidArgument();
		}
		viewer.setBooleanProperty("bondSelelectionModeOr", bondmodeOr);
	}

	void setSelectionHalo(int pt) throws ScriptException {
		if (pt == statementLength) {
			viewer.setBooleanProperty("selectionHalos", true);
			return;
		}
		if (pt + 1 < statementLength)
			checkLength3();
		boolean showHalo = false;
		switch (statement[pt].tok) {
		case Token.on:
		case Token.selected:
			showHalo = true;
		case Token.off:
		case Token.none:
		case Token.normal:
			viewer.setBooleanProperty("selectionHalos", showHalo);
			break;
		default:
			keywordExpected();
		}
	}

	void setEcho() throws ScriptException {
		String propertyName = "target";
		Object propertyValue = null;
		boolean echoShapeActive = true;
		if (statementLength < 3)
			badArgumentCount();
		// set echo xxx
		switch (statement[2].tok) {
		case Token.off:
			checkLength3();
			echoShapeActive = false;
			propertyName = "allOff";
			break;
		case Token.none:
			checkLength3();
			echoShapeActive = false;
		case Token.all:
			checkLength3();
		case Token.left:
		case Token.right:
		case Token.top:
		case Token.bottom:
		case Token.center:
		case Token.identifier:
			propertyValue = statement[2].value;
			break;
		default:
			keywordExpected();
		}
		viewer.setEchoStateActive(echoShapeActive);
		viewer.loadShape(JmolConstants.SHAPE_ECHO);
		viewer.setShapeProperty(JmolConstants.SHAPE_ECHO, propertyName, propertyValue);
		if (statementLength == 3)
			return;
		propertyName = "align";
		// set echo name xxx
		if (statementLength == 4) {
			switch (statement[3].tok) {
			case Token.off:
				propertyName = "off";
				break;
			case Token.left:
			case Token.right:
			case Token.top:
			case Token.bottom:
			case Token.center:
			case Token.identifier: // middle
				propertyValue = statement[3].value;
				break;
			default:
				keywordExpected();
			}
			viewer.setShapeProperty(JmolConstants.SHAPE_ECHO, propertyName, propertyValue);
			return;
		}
		// set echo name x-pos y-pos
		if (statementLength < 5)
			badArgumentCount();
		int i = 3;
		// set echo name {x y z}
		if (isAtomCenterOrCoordinateNext(i)) {
			viewer.setShapeProperty(JmolConstants.SHAPE_ECHO, "xyz", atomCenterOrCoordinateParameter(i));
			return;
		}
		int pos = intParameter(i++);
		String type;
		propertyValue = new Integer(pos);
		if (i < statementLength && statement[i].tok == Token.percent) {
			type = "%xpos";
			i++;
		}
		else {
			type = "xpos";
		}
		viewer.setShapeProperty(JmolConstants.SHAPE_ECHO, type, propertyValue);
		pos = intParameter(i++);
		propertyValue = new Integer(pos);
		if (i < statementLength && statement[i].tok == Token.percent) {
			type = "%ypos";
			i++;
		}
		else {
			type = "ypos";
		}
		viewer.setShapeProperty(JmolConstants.SHAPE_ECHO, type, propertyValue);
	}

	void setFontsize() throws ScriptException {
		int rasmolSize = 8;
		if (statementLength == 3) {
			rasmolSize = intParameter(2);
			// this is a kludge/hack to be somewhat compatible with RasMol
			rasmolSize += 5;

			if (rasmolSize < JmolConstants.LABEL_MINIMUM_FONTSIZE || rasmolSize > JmolConstants.LABEL_MAXIMUM_FONTSIZE)
				numberOutOfRange(JmolConstants.LABEL_MINIMUM_FONTSIZE, JmolConstants.LABEL_MINIMUM_FONTSIZE);
		}
		viewer.loadShape(JmolConstants.SHAPE_LABELS);
		viewer.setShapeProperty(JmolConstants.SHAPE_LABELS, "fontsize", new Integer(rasmolSize));
	}

	void setLabel(String str) throws ScriptException {
		viewer.loadShape(JmolConstants.SHAPE_LABELS);
		if (str.equals("offset")) {
			checkLength4();
			int xOffset = intParameter(2);
			int yOffset = intParameter(3);
			if (xOffset > 100 || yOffset > 100 || xOffset < -100 || yOffset < -100)
				numberOutOfRange(-100, 100);
			int offset = ((xOffset & 0xFF) << 8) | (yOffset & 0xFF);
			viewer.setShapeProperty(JmolConstants.SHAPE_LABELS, "offset", new Integer(offset));
			return;
		}
		if (str.equals("alignment")) {
			checkLength3();
			switch (statement[2].tok) {
			case Token.left:
			case Token.right:
			case Token.center:
				viewer.setShapeProperty(JmolConstants.SHAPE_LABELS, "align", statement[2].value);
				return;
			}
			invalidArgument();
		}
		if (str.equals("pointer")) {
			checkLength3();
			int flags = Text.POINTER_NONE;
			switch (statement[2].tok) {
			case Token.off:
			case Token.none:
				break;
			case Token.background:
				flags |= Text.POINTER_BACKGROUND;
			case Token.on:
				flags |= Text.POINTER_ON;
				break;
			default:
				invalidArgument();
			}
			viewer.setShapeProperty(JmolConstants.SHAPE_LABELS, "pointer", new Integer(flags));
			return;
		}
		checkLength2();
		if (str.equals("atom")) {
			viewer.setShapeProperty(JmolConstants.SHAPE_LABELS, "front", Boolean.FALSE);
			return;
		}
		if (str.equals("front")) {
			viewer.setShapeProperty(JmolConstants.SHAPE_LABELS, "front", Boolean.TRUE);
			return;
		}
		if (str.equals("group")) {
			viewer.setShapeProperty(JmolConstants.SHAPE_LABELS, "group", Boolean.TRUE);
			return;
		}
		invalidArgument();
	}

	void setMonitor(int cmdPt) throws ScriptException {
		// on off here incompatible with "monitor on/off" so this is just a SET option.
		// cmdPt will be 2 here.
		boolean showMeasurementNumbers = false;
		checkLength3();
		switch (statement[cmdPt].tok) {
		case Token.on:
			showMeasurementNumbers = true;
		case Token.off:
			viewer.setShapeProperty(JmolConstants.SHAPE_MEASURES, "showMeasurementNumbers",
					showMeasurementNumbers ? Boolean.TRUE : Boolean.FALSE);
			return;
		case Token.identifier:
			if (!viewer.setMeasureDistanceUnits((String) statement[cmdPt].value))
				unrecognizedSetParameter();
			return;
		}
		viewer.setShapeSize(JmolConstants.SHAPE_MEASURES, getSetAxesTypeMad(cmdPt));
	}

	void setProperty() throws ScriptException {
		// what possible good is this?
		// set property foo bar is identical to
		// set foo bar

		checkLength4();
		if (statement[2].tok != Token.identifier)
			propertyNameExpected();
		String propertyName = (String) statement[2].value;
		switch (statement[3].tok) {
		case Token.on:
			viewer.setBooleanProperty(propertyName, true);
			break;
		case Token.off:
			viewer.setBooleanProperty(propertyName, false);
			break;
		case Token.integer:
			viewer.setIntProperty(propertyName, statement[3].intValue);
			break;
		case Token.decimal:
			viewer.setFloatProperty(propertyName, floatParameter(3));
			break;
		case Token.string:
			viewer.setStringProperty(propertyName, stringParameter(3));
			break;
		default:
			unrecognizedSetParameter();
		}
	}

	void setStrands() throws ScriptException {
		int strandCount = 5;
		if (statementLength == 3) {
			if (statement[2].tok != Token.integer)
				integerExpected();
			strandCount = statement[2].intValue;
			if (strandCount < 0 || strandCount > 20)
				numberOutOfRange(0, 20);
		}
		viewer.setShapeProperty(JmolConstants.SHAPE_STRANDS, "strandCount", new Integer(strandCount));
	}

	void setSpin() throws ScriptException {
		checkLength4();
		int value = (int) floatParameter(3);
		if (statement[2].tok == Token.identifier) {
			String str = (String) statement[2].value;
			if (str.equalsIgnoreCase("x")) {
				viewer.setSpinX(value);
				return;
			}
			if (str.equalsIgnoreCase("y")) {
				viewer.setSpinY(value);
				return;
			}
			if (str.equalsIgnoreCase("z")) {
				viewer.setSpinZ(value);
				return;
			}
			if (str.equalsIgnoreCase("fps")) {
				viewer.setSpinFps(value);
				return;
			}
		}
		unrecognizedSetParameter();
	}

	void setSsbond() throws ScriptException {
		checkLength3();
		boolean ssbondsBackbone = false;
		viewer.loadShape(JmolConstants.SHAPE_SSSTICKS);
		switch (statement[2].tok) {
		case Token.backbone:
			ssbondsBackbone = true;
			break;
		case Token.sidechain:
			break;
		default:
			invalidArgument();
		}
		viewer.setBooleanProperty("ssbondsBackbone", ssbondsBackbone);
	}

	void setHbond() throws ScriptException {
		checkLength3();
		boolean bool = false;
		switch (statement[2].tok) {
		case Token.backbone:
			bool = true;
			// fall into
		case Token.sidechain:
			viewer.setBooleanProperty("hbondsBackbone", bool);
			break;
		case Token.solid:
			bool = true;
			// falll into
		case Token.dotted:
			viewer.setBooleanProperty("hbondsSolid", bool);
			break;
		default:
			invalidArgument();
		}
	}

	void setScale3d() throws ScriptException {
		checkLength3();
		switch (statement[2].tok) {
		case Token.decimal:
		case Token.integer:
			break;
		default:
			numberExpected();
		}
		viewer.setFloatProperty("scaleAngstromsPerInch", floatParameter(2));
	}

	void setPicking() throws ScriptException {
		if (statementLength == 2) {
			viewer.setStringProperty("picking", "ident");
			return;
		}
		checkLength34();
		switch (statement[2].tok) {
		case Token.select:
		case Token.monitor:
		case Token.spin:
			break;
		default:
			checkLength3();
		}
		String str = null;
		Token token = statement[statementLength - 1];
		int tok = token.tok;
		switch (tok) {
		case Token.on:
		case Token.normal:
			str = "ident";
			break;
		case Token.none:
			str = "off";
			break;
		case Token.select:
			str = "atom";
			break;
		case Token.bonds: // not implemented
			str = "bond";
			break;
		case Token.spin:
			int rate = 10;
			if (statementLength == 4) {
				rate = intParameter(3);
			}
			viewer.setIntProperty("pickingSpinRate", rate);
			return;
		}
		if (token.value instanceof String)
			str = (String) token.value;
		else invalidArgument();
		if (JmolConstants.GetPickingMode(str) < 0)
			invalidArgument();
		viewer.setStringProperty("picking", str);
	}

	void setPickingStyle() throws ScriptException {
		checkLength34();
		boolean isMeasure = (statement[2].tok == Token.monitor);
		String str = null;
		Token token = statement[statementLength - 1];
		int tok = token.tok;
		switch (tok) {
		case Token.none:
		case Token.off:
			str = (isMeasure ? "measureoff" : "toggle");
			break;
		case Token.on:
			if (!isMeasure)
				invalidArgument();
			str = "measure";
			break;
		}
		try {
			if (str == null)
				str = (String) token.value;
		}
		catch (Exception e) {
			invalidArgument();
		}
		if (JmolConstants.GetPickingStyle(str) < 0)
			invalidArgument();
		viewer.setStringProperty("pickingStyle", str);
	}

	/*******************************************************************************************************************
	 * ============================================================== SAVE/RESTORE
	 * ==============================================================
	 */

	void save() throws ScriptException {
		if (statementLength > 1) {
			String saveName = parameterAsString(2);
			switch (statement[1].tok) {
			case Token.orientation:
				viewer.saveOrientation(saveName);
				return;
			case Token.bonds:
				viewer.saveBonds(saveName);
				return;
			case Token.state:
				viewer.saveState(saveName);
				return;
			case Token.identifier:
				if (((String) statement[1].value).equalsIgnoreCase("selection")) {
					viewer.saveSelection(saveName);
					return;
				}
			}
		}
		evalError(GT._("save what?") + " bonds? orientation? selection? state?");
	}

	void restore() throws ScriptException {
		if (statementLength > 1) {
			String saveName = parameterAsString(2);
			switch (statement[1].tok) {
			case Token.orientation:
				float timeSeconds = (statementLength > 3 ? floatParameter(3) : 0);
				viewer.restoreOrientation(saveName, timeSeconds);
				return;
			case Token.bonds:
				viewer.restoreBonds(saveName);
				return;
			case Token.state:
				String state = viewer.getSavedState(saveName);
				if (state == null)
					invalidArgument();
				runScript(state);
				return;
			case Token.identifier:
				if (((String) statement[1].value).equalsIgnoreCase("selection")) {
					viewer.restoreSelection(saveName);
					return;
				}
			}
		}
		evalError(GT._("restore what?") + " bonds? orientation? selection?");
	}

	void write() throws ScriptException {
		if (viewer.isApplet())
			evalError(GT._("The {0} command is not available for the applet.", "WRITE"));
		int tok = (statementLength == 1 ? Token.clipboard : statement[1].tok);
		int pt = 1;
		String type = "SPT";
		switch (tok) {
		case Token.script:
			pt++;
			break;
		case Token.identifier:
			type = ((String) statement[1].value).toLowerCase();
			if (type.equals("image"))
				pt++;
			else type = "image";
			break;
		}
		if (pt == statementLength)
			badArgumentCount();

		// write [image|script] clipboard

		if ((tok = statement[pt].tok) == Token.clipboard) {
			viewer.createImage(null, type, 100);
			return;
		}

		// write [optional image|script] [JPG|JPG64|PNG|PPM|SPT] "filename"
		// write script "filename"

		if (pt + 2 == statementLength) {
			type = ((tok == Token.identifier ? (String) statement[pt].value : stringParameter(pt))).toUpperCase();
			if (";JPEG;JPG64;JPG;PDF;PNG;SPT;".indexOf(";" + type + ";") < 0)
				evalError(GT._("write what? {0} or {1} \"filename\"", new Object[] { "SCRIPT|IMAGE CLIPBOARD",
						"JPG|JPG64|PNG|PPM|SPT" }));
		}
		String fileName = null;
		switch (statement[statementLength - 1].tok) {
		case Token.identifier:
		case Token.string:
			fileName = (String) statement[statementLength - 1].value;
			break;
		default:
			invalidArgument();
		}
		viewer.createImage(fileName, type, 100);
	}

	/*******************************************************************************************************************
	 * ============================================================== SHOW
	 * ==============================================================
	 */

	void show() throws ScriptException {
		if (statementLength == 1)
			badArgumentCount();
		switch (statement[1].tok) {
		case Token.state:
			checkLength23();
			if (statementLength == 2) {
				showString(viewer.getStateInfo());
				return;
			}
			showString(viewer.getSavedState(parameterAsString(2)));
			return;
		case Token.save:
			showString(viewer.listSavedStates());
			return;
		case Token.data:
			String type = (statementLength == 3 ? stringParameter(2) : null);
			String[] data = (type == null ? dataLabelString : viewer.getData(type));
			showString(data == null ? "no data" : "data \"" + data[0] + "\"\n" + data[1]);
			return;
		case Token.unitcell:
			showString(viewer.getUnitCellInfoText());
			return;
		case Token.spacegroup:
			if (statementLength == 2) {
				showString(viewer.getSpaceGroupInfoText(null));
				return;
			}
			if (statementLength == 3 && statement[2].tok == Token.string) {
				String sg = viewer.simpleReplace((String) statement[2].value, "''", "\"");
				showString(viewer.getSpaceGroupInfoText(sg));
				return;
			}
			invalidArgument();
		case Token.dollarsign:
			int shapeType = setShapeByNameParameter(2);
			showString((String) viewer.getShapeProperty(shapeType,
					(shapeType == JmolConstants.SHAPE_ISOSURFACE ? "jvxlFileData" : "command")));
			return;
		case Token.boundbox:
			showString("boundbox: " + viewer.getBoundBoxCenter() + " " + viewer.getBoundBoxCornerVector());
			return;
		case Token.center:
			Point3f pt = viewer.getRotationCenter();
			showString("center {" + pt.x + " " + pt.y + " " + pt.z + "}");
			return;
		case Token.draw:
			showString((String) viewer.getShapeProperty(JmolConstants.SHAPE_DRAW, "command"));
			return;
		case Token.file:
			// as as string
			if (statementLength == 2) {
				showString(viewer.getCurrentFileAsString());
				return;
			}
			if (statementLength == 3 && statement[2].tok == Token.string) {
				String fileName = (String) statement[2].value;
				showString(viewer.getFileAsString(fileName));
				return;
			}
			invalidArgument();
		case Token.history:
			int n = (statementLength == 2 ? Integer.MAX_VALUE : intParameter(2));
			if (n < 1)
				invalidArgument();
			viewer.removeCommand();
			showString(viewer.getSetHistory(n));
			return;
		case Token.isosurface:
			showString((String) viewer.getShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "jvxlFileData"));
			return;
		case Token.mo:
			// 0: all; Integer.MAX_VALUE: current;
			viewer.loadShape(JmolConstants.SHAPE_MO);
			int modelIndex = viewer.getDisplayModelIndex();
			if (modelIndex < 0)
				evalError(GT._("MO isosurfaces require that only one model be displayed"));
			Hashtable moData = (Hashtable) viewer.getModelAuxiliaryInfo(modelIndex, "moData");
			if (moData == null)
				evalError(GT._("no MO basis/coefficient data available for this frame"));
			viewer.setShapeProperty(JmolConstants.SHAPE_MO, "moData", moData);
			showString((String) viewer.getShapeProperty(JmolConstants.SHAPE_MO, "showMO",
					statementLength > 2 ? intParameter(2) : Integer.MAX_VALUE));
			return;
		case Token.model:
			showString(viewer.getModelInfoAsString());
			return;
		case Token.monitor:
			showString(viewer.getMeasurementInfoAsString());
			return;
		case Token.orientation:
			showString(viewer.getOrientationText());
			return;
		case Token.pdbheader:
			showString(viewer.getPDBHeader());
			return;
		case Token.symmetry:
			showString(viewer.getSymmetryInfoAsString());
			return;
		case Token.transform:
			showString("transform:\n" + viewer.getTransformText());
			return;
		case Token.url:
			// in a new window
			if (statementLength == 2) {
				viewer.showUrl(viewer.getFullPathName());
				return;
			}
			if (statementLength == 3 && statement[2].tok == Token.string) {
				String fileName = (String) statement[2].value;
				viewer.showUrl(fileName);
				return;
			}
			invalidArgument();
		case Token.zoom:
			showString("zoom " + (viewer.getZoomEnabled() ? ("" + viewer.getZoomPercentSetting()) : "off"));
			return;
			// not implemented
		case Token.translation:
		case Token.rotation:
			evalError(GT._("use {0}") + "\"show ORIENTATION\"");
		case Token.chain:
		case Token.group:
		case Token.sequence:
		case Token.residue:
			evalError(GT._("unrecognized {0} parameter --  use {1}",
					new Object[] { "SHOW", "\"getProperty CHAININFO\"" }));
		case Token.selected:
			evalError(GT._("unrecognized {0} parameter --  use {1}", new Object[] { "SHOW",
					"\"getProperty ATOMINFO (selected)\"" }));
		case Token.atom:
			evalError(GT._("unrecognized {0} parameter --  use {1}", new Object[] { "SHOW",
					"\"getProperty ATOMINFO (atom expression)\"" }));
		case Token.spin:
		case Token.list:
		case Token.mlp:
		case Token.information:
		case Token.phipsi:
		case Token.ramprint:
		case Token.all:
			notImplemented(1);
			break;
		default:
			evalError(GT._("unrecognized {0} parameter", "SHOW"));
		}
	}

	void showString(String str) {
		Logger.warn(str);
		viewer.scriptEcho(str);
	}

	/*******************************************************************************************************************
	 * ============================================================== MESH implementations
	 * ==============================================================
	 */

	void pmesh() throws ScriptException {
		viewer.loadShape(JmolConstants.SHAPE_PMESH);
		viewer.setShapeProperty(JmolConstants.SHAPE_PMESH, "init", getCommand());
		Object t;
		for (int i = 1; i < statementLength; ++i) {
			String propertyName = null;
			Object propertyValue = null;
			int tok = statement[i].tok;
			switch (tok) {
			case Token.identifier:
				propertyValue = statement[i].value;
				String str = ((String) propertyValue);
				if (str.equalsIgnoreCase("FIXED")) {
					propertyName = "fixed";
					propertyValue = Boolean.TRUE;
					break;
				}
				if (str.equalsIgnoreCase("MODELBASED")) {
					propertyName = "fixed";
					propertyValue = Boolean.FALSE;
					break;
				}
				propertyName = "thisID";
				break;
			case Token.string:
				String filename = (String) statement[i].value;
				propertyName = "bufferedReader";
				if (filename.equalsIgnoreCase("inline")) {
					if (i + 1 < statementLength && statement[i + 1].tok == Token.string) {
						String data = (String) statement[++i].value;
						if (data.indexOf("|") < 0 && data.indexOf("\n") < 0) {
							// space separates -- so set isOnePerLine
							data = viewer.simpleReplace(data, " ", "\n");
							propertyName = "bufferedReaderOnePerLine";
						}
						data = viewer.simpleReplace(data, "{", " ");
						data = viewer.simpleReplace(data, ",", " ");
						data = viewer.simpleReplace(data, "}", " ");
						data = viewer.simpleReplace(data, "|", "\n");
						data = viewer.simpleReplace(data, "\n\n", "\n");
						if (logMessages)
							Logger.debug("pmesh inline data:\n" + data);
						t = viewer.getBufferedReaderForString(data);
					}
					else {
						stringOrIdentifierExpected();
						break;
					}
				}
				else {
					t = viewer.getUnzippedBufferedReaderOrErrorMessageFromName(filename);
					if (t instanceof String)
						fileNotFoundException(filename + ":" + t);
				}
				propertyValue = t;
				break;
			default:
				if (!setMeshDisplayProperty(JmolConstants.SHAPE_PMESH, tok))
					invalidArgument(i, statement[i] + " not recognized");
				continue;
			}
			viewer.setShapeProperty(JmolConstants.SHAPE_PMESH, propertyName, propertyValue);
		}
	}

	void draw() throws ScriptException {
		viewer.loadShape(JmolConstants.SHAPE_DRAW);
		viewer.setShapeProperty(JmolConstants.SHAPE_DRAW, "init", null);
		boolean havePoints = false;

		boolean isInitialized = false;
		int intScale = 0;
		for (int i = 1; i < statementLength; ++i) {
			String propertyName = null;
			Object propertyValue = null;
			int tok = statement[i].tok;
			switch (tok) {
			case Token.identifier:
				propertyName = "thisID";
				propertyValue = statement[i].value;
				String str = (String) propertyValue;
				if (str.equalsIgnoreCase("FIXED")) {
					propertyName = "fixed";
					propertyValue = Boolean.TRUE;
					break;
				}
				if (str.equalsIgnoreCase("MODELBASED")) {
					propertyName = "fixed";
					propertyValue = Boolean.FALSE;
					break;
				}
				if (str.equalsIgnoreCase("PLANE")) {
					propertyName = "plane";
					break;
				}
				if (str.equalsIgnoreCase("CROSSED")) {
					propertyName = "crossed";
					break;
				}
				if (str.equalsIgnoreCase("CURVE")) {
					propertyName = "curve";
					break;
				}
				if (str.equalsIgnoreCase("ARROW")) {
					propertyName = "arrow";
					break;
				}
				if (str.equalsIgnoreCase("CIRCLE")) { // circle <center>; not yet implemented
					propertyName = "circle";
					break;
				}
				if (str.equalsIgnoreCase("VERTICES")) {
					propertyName = "vertices";
					break;
				}
				if (str.equalsIgnoreCase("REVERSE")) {
					propertyName = "reverse";
					break;
				}
				if (str.equalsIgnoreCase("ROTATE45")) {
					propertyName = "rotate45";
					break;
				}
				if (str.equalsIgnoreCase("PERP") || str.equalsIgnoreCase("PERPENDICULAR")) {
					propertyName = "perp";
					break;
				}
				if (str.equalsIgnoreCase("OFFSET")) {
					Point3f pt = getCoordinate(++i, true);
					i = pcLastExpressionInstruction;
					propertyName = "offset";
					propertyValue = pt;
					break;
				}
				if (str.equalsIgnoreCase("SCALE")) {
					checkStatementLength((++i) + 1);
					switch (statement[i].tok) {
					case Token.integer:
						intScale = intParameter(i);
						break;
					case Token.decimal:
						intScale = (int) (floatParameter(i) * 100);
						break;
					default:
						numberExpected();
					}
					break;
				}
				if (str.equalsIgnoreCase("LENGTH")) {
					propertyValue = new Float(floatParameter(++i));
					propertyName = "length";
					break;
				}
				break;
			case Token.dollarsign:
				// $drawObject
				propertyValue = objectNameParameter(++i);
				propertyName = "identifier";
				havePoints = true;
				break;
			case Token.color:
				if (++i < statementLength && statement[i].tok == Token.colorRGB) {
					viewer.setShapeProperty(JmolConstants.SHAPE_DRAW, "colorRGB", new Integer(getArgbParam(i)));
					continue;
				}
				invalidArgument();
			case Token.decimal:
				// $drawObject
				propertyValue = new Float(floatParameter(i));
				propertyName = "length";
				break;
			case Token.integer:
				intScale = intParameter(i);
				break;
			case Token.leftbrace:
				// {X, Y, Z}
				Point3f pt = getCoordinate(i, true);
				i = pcLastExpressionInstruction;
				propertyName = "coord";
				propertyValue = pt;
				havePoints = true;
				break;
			case Token.expressionBegin:
				propertyName = "atomSet";
				propertyValue = expression(statement, i + 1);
				i = pcLastExpressionInstruction;
				havePoints = true;
				break;
			default:
				if (!setMeshDisplayProperty(JmolConstants.SHAPE_DRAW, tok))
					invalidArgument();
			}
			if (havePoints && !isInitialized) {
				viewer.setShapeProperty(JmolConstants.SHAPE_DRAW, "points", new Integer(intScale));
				isInitialized = true;
			}
			if (propertyName != null)
				viewer.setShapeProperty(JmolConstants.SHAPE_DRAW, propertyName, propertyValue);
		}
		if (havePoints) {
			viewer.setShapeProperty(JmolConstants.SHAPE_DRAW, "set", null);
		}
		else if (intScale != 0) {
			viewer.setShapeProperty(JmolConstants.SHAPE_DRAW, "scale", new Integer(intScale));
		}
	}

	void polyhedra() throws ScriptException {
		/*
		 * needsGenerating:
		 * 
		 * polyhedra [number of vertices and/or basis] [at most two selection sets] [optional type and/or edge]
		 * [optional design parameters]
		 * 
		 * OR else:
		 * 
		 * polyhedra [at most one selection set] [type-and/or-edge or on/off/delete]
		 * 
		 */
		boolean needsGenerating = false;
		boolean onOffDelete = false;
		boolean typeSeen = false;
		boolean edgeParameterSeen = false;
		boolean isDesignParameter = false;
		int nAtomSets = 0;
		viewer.loadShape(JmolConstants.SHAPE_POLYHEDRA);
		viewer.setShapeProperty(JmolConstants.SHAPE_POLYHEDRA, "init", null);
		String setPropertyName = "centers";
		String decimalPropertyName = "radius_";
		String translucency = "";
		int color = Integer.MIN_VALUE;
		for (int i = 1; i < statementLength; ++i) {
			String propertyName = null;
			Object propertyValue = null;
			Token token = statement[i];
			switch (token.tok) {
			case Token.opEQ:
			case Token.opOr:
				continue;
			case Token.bonds:
				if (nAtomSets > 0)
					invalidParameterOrder();
				needsGenerating = true;
				propertyName = "bonds";
				break;
			case Token.radius:
				decimalPropertyName = "radius";
				continue;
			case Token.translucent:
				translucency = "translucent";
				continue;
			case Token.opaque:
				translucency = "opaque";
				continue;
			case Token.colorRGB:
				color = getArgbParam(i);
				continue;
			case Token.identifier:
				String str = (String) token.value;
				if ("collapsed".equalsIgnoreCase(str)) {
					propertyName = "collapsed";
					propertyValue = Boolean.TRUE;
					if (typeSeen)
						incompatibleArguments();
					typeSeen = true;
					break;
				}
				if ("flat".equalsIgnoreCase(str)) {
					propertyName = "collapsed";
					propertyValue = Boolean.FALSE;
					if (typeSeen)
						incompatibleArguments();
					typeSeen = true;
					break;
				}
				if ("edges".equalsIgnoreCase(str) || "noedges".equalsIgnoreCase(str)
						|| "frontedges".equalsIgnoreCase(str)) {
					if (edgeParameterSeen)
						incompatibleArguments();
					propertyName = (String) token.value;
					edgeParameterSeen = true;
					break;
				}
				if (!needsGenerating)
					insufficientArguments();
				if ("to".equalsIgnoreCase(str)) {
					if (nAtomSets > 1)
						invalidParameterOrder();
					if (statementLength > i + 2 && statement[i + 2].tok == Token.bitset) {
						propertyName = "toBitSet";
						propertyValue = statement[i + 2].value;
						i += 3;
						needsGenerating = true;
						break;
					}
					setPropertyName = "to";
					continue;
				}
				if ("faceCenterOffset".equalsIgnoreCase(str)) {
					decimalPropertyName = "faceCenterOffset";
					isDesignParameter = true;
					continue;
				}
				if ("distanceFactor".equalsIgnoreCase(str)) {
					decimalPropertyName = "distanceFactor";
					isDesignParameter = true;
					continue;
				}
				unrecognizedSubcommand(str);
			case Token.integer:
				if (nAtomSets > 0 && !isDesignParameter)
					invalidParameterOrder();
				// no reason not to allow integers when explicit
				if (decimalPropertyName == "radius_") {
					propertyName = "nVertices";
					propertyValue = new Integer(token.intValue);
					needsGenerating = true;
					break;
				}
			case Token.decimal:
				if (nAtomSets > 0 && !isDesignParameter)
					invalidParameterOrder();
				propertyName = (decimalPropertyName == "radius_" ? "radius" : decimalPropertyName);
				propertyValue = new Float(floatParameter(i));
				decimalPropertyName = "radius_";
				isDesignParameter = false;
				needsGenerating = true;
				break;
			case Token.delete:
			case Token.on:
			case Token.off:
				if (++i != statementLength || needsGenerating || nAtomSets > 1 || nAtomSets == 0
						&& setPropertyName == "to")
					incompatibleArguments();
				propertyName = (String) token.value;
				onOffDelete = true;
				break;
			case Token.expressionBegin:
				if (typeSeen)
					invalidParameterOrder();
				if (++nAtomSets > 2)
					badArgumentCount();
				if (setPropertyName == "to")
					needsGenerating = true;
				propertyName = setPropertyName;
				setPropertyName = "to";
				propertyValue = expression(statement, ++i);
				i = pcLastExpressionInstruction; // the for loop will increment i
				break;
			default:
				invalidArgument();
			}
			viewer.setShapeProperty(JmolConstants.SHAPE_POLYHEDRA, propertyName, propertyValue);
			if (onOffDelete)
				return;
		}
		if (!needsGenerating && !typeSeen && !edgeParameterSeen)
			insufficientArguments();
		if (needsGenerating)
			viewer.setShapeProperty(JmolConstants.SHAPE_POLYHEDRA, "generate", null);
		if (color != Integer.MIN_VALUE)
			viewer.setShapeProperty(JmolConstants.SHAPE_POLYHEDRA, "colorThis", new Integer(color));
		if (translucency.length() > 0)
			viewer.setShapeProperty(JmolConstants.SHAPE_POLYHEDRA, "translucencyThis", translucency);
	}

	void lcaoCartoon() throws ScriptException {
		viewer.loadShape(JmolConstants.SHAPE_LCAOCARTOON);
		viewer.setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, "init", null);
		if (statementLength == 1) {
			viewer.setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, "lcaoID", null);
			return;
		}
		for (int i = 1; i < statementLength; i++) {
			Token token = statement[i];
			int tok = token.tok;
			String propertyName = null;
			Object propertyValue = null;
			switch (tok) {
			case Token.center:
				// serialized lcaoCartoon in isosurface format
				isosurface(JmolConstants.SHAPE_LCAOCARTOON);
				return;
			case Token.on:
				propertyName = "on";
				break;
			case Token.off:
				propertyName = "off";
				break;
			case Token.delete:
				propertyName = "delete";
				break;
			case Token.integer:
			case Token.decimal:
				propertyName = "scale";
				propertyValue = new Float(floatParameter(++i));
				break;
			case Token.expressionBegin:
				propertyName = "select";
				propertyValue = expression(statement, ++i);
				i = pcLastExpressionInstruction;
				break;
			case Token.color:
				if (++i < statementLength && statement[i].tok == Token.colorRGB) {
					viewer.setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, "colorRGB", new Integer(getArgbParam(i)));
					viewer.setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, "colorRGB", new Integer(
							getArgbParam(i + 1 < statementLength && statement[i + 1].tok == Token.colorRGB ? ++i : i)));
					continue;
				}
				invalidArgument();
			case Token.string:
				propertyName = "create";
				propertyValue = stringParameter(i);
				if (i + 1 < statementLength && statement[i + 1].tok == Token.identifier
						&& ((String) (statement[i + 1].value)).equalsIgnoreCase("molecular")) {
					viewer.setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, "molecular", null);
				}
				break;
			case Token.select:
				propertyName = "selectType";
				propertyValue = stringParameter(++i);
				break;
			case Token.identifier:
				String str = (String) token.value;
				if (str.equalsIgnoreCase("SCALE")) {
					propertyName = "scale";
					propertyValue = new Float(floatParameter(++i));
					break;
				}
				if (str.equalsIgnoreCase("CREATE")) {
					propertyName = "create";
					propertyValue = stringParameter(++i);
					break;
				}
				propertyValue = str;
			case Token.all:
				propertyName = "lcaoID";
				break;
			}
			if (propertyName == null)
				invalidArgument(i, token.toString());
			viewer.setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, propertyName, propertyValue);
		}
	}

	int lastMoNumber = 0;

	void mo() throws ScriptException {
		int modelIndex = viewer.getDisplayModelIndex();
		if (modelIndex < 0)
			evalError(GT._("MO isosurfaces require that only one model be displayed"));
		viewer.loadShape(JmolConstants.SHAPE_MO);
		viewer.setShapeProperty(JmolConstants.SHAPE_MO, "init", new Integer(modelIndex));
		Integer index = null;
		String title = null;
		try {
			index = (Integer) viewer.getShapeProperty(JmolConstants.SHAPE_MO, "moNumber");
		}
		catch (Exception e) {
			// could just be the string "no current mesh"
		}
		int moNumber = (index == null ? Integer.MAX_VALUE : index.intValue());
		if (moNumber == Integer.MAX_VALUE)
			lastMoNumber = 0;
		String str;
		if (statementLength == 1)
			endOfStatementUnexpected();
		Token token = statement[1];
		int tok = token.tok;
		String propertyName = null;
		Object propertyValue = null;
		switch (tok) {
		case Token.integer:
			moNumber = intParameter(1);
			break;
		case Token.next:
			moNumber = lastMoNumber + 1;
			break;
		case Token.prev:
			moNumber = lastMoNumber - 1;
			break;
		case Token.color:
			// mo color color1 color2
			if (2 < statementLength && statement[2].tok == Token.colorRGB) {
				viewer.setShapeProperty(JmolConstants.SHAPE_MO, "colorRGB", new Integer(getArgbParam(2)));
				if (3 < statementLength && statement[3].tok == Token.colorRGB)
					viewer.setShapeProperty(JmolConstants.SHAPE_MO, "colorRGB", new Integer(getArgbParam(3)));
				break;
			}
			invalidArgument();
		case Token.identifier:
			str = (String) token.value;
			if (str.equalsIgnoreCase("CUTOFF")) {
				if (2 < statementLength && statement[2].tok == Token.plus) {
					propertyName = "cutoffPositive";
					propertyValue = new Float(floatParameter(3));
				}
				else {
					propertyName = "cutoff";
					propertyValue = new Float(floatParameter(2));
				}
				break;
			}
			if (str.equalsIgnoreCase("RESOLUTION")) {
				propertyName = "resolution";
				propertyValue = new Float(floatParameter(2));
				break;
			}
			if (str.equalsIgnoreCase("SCALE")) {
				propertyName = "scale";
				propertyValue = new Float(floatParameter(2));
				break;
			}
			if (str.equalsIgnoreCase("TITLEFORMAT")) {
				if (2 < statementLength && statement[2].tok == Token.string) {
					propertyName = "titleFormat";
					propertyValue = stringParameter(2);
				}
				break;
			}
			if (str.equalsIgnoreCase("DEBUG")) {
				propertyName = "debug";
				break;
			}
			if (str.equalsIgnoreCase("plane")) {
				// plane {X, Y, Z, W}
				propertyName = "plane";
				propertyValue = planeParameter(2);
				break;
			}
			if (str.equalsIgnoreCase("noplane")) {
				propertyName = "plane";
				propertyValue = null;
				break;
			}
			invalidArgument(1, str);
		default:
			if (!setMeshDisplayProperty(JmolConstants.SHAPE_MO, tok))
				invalidArgument(1, statement[1] + " not recognized");
			// if (tok == Token.delete || tok == Token.off)
			return;
		}
		if (propertyName != null)
			viewer.setShapeProperty(JmolConstants.SHAPE_MO, propertyName, propertyValue);
		if (moNumber != Integer.MAX_VALUE) {
			if (2 < statementLength && statement[2].tok == Token.string)
				title = stringParameter(2);
			setMoData(JmolConstants.SHAPE_MO, moNumber, title);
		}
	}

	void setMoData(int shape, int moNumber, String title) throws ScriptException {
		int modelIndex = viewer.getDisplayModelIndex();
		if (modelIndex < 0)
			evalError(GT._("MO isosurfaces require that only one model be displayed"));
		Hashtable moData = (Hashtable) viewer.getModelAuxiliaryInfo(modelIndex, "moData");
		Hashtable surfaceInfo = (Hashtable) viewer.getModelAuxiliaryInfo(modelIndex, "jmolSurfaceInfo");
		if (surfaceInfo != null && ((String) surfaceInfo.get("surfaceDataType")).equals("mo")) {
			viewer.loadShape(JmolConstants.SHAPE_ISOSURFACE);
			viewer.setShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "init", null);
			viewer.setShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "sign", Boolean.TRUE);
			viewer.setShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "getSurface", surfaceInfo);

			return;
		}
		if (moData == null)
			evalError(GT._("no MO basis/coefficient data available for this frame"));
		Vector mos = (Vector) (moData.get("mos"));
		int nOrb = (mos == null ? 0 : mos.size());
		if (nOrb == 0)
			evalError(GT._("no MO coefficient data available"));
		if (nOrb == 1 && moNumber > 1)
			evalError(GT._("Only one molecular orbital is available in this file"));
		if (moNumber < 1 || moNumber > nOrb)
			evalError(GT._("An MO index from 1 to {0} is required", nOrb));
		lastMoNumber = moNumber;
		viewer.setShapeProperty(shape, "moData", moData);
		if (title != null)
			viewer.setShapeProperty(shape, "title", title);
		viewer.setShapeProperty(shape, "molecularOrbital", new Integer(moNumber));
	}

	@SuppressWarnings("unchecked")
	void isosurface(int iShape) throws ScriptException {
		viewer.loadShape(iShape);
		viewer.setShapeProperty(iShape, "init", getCommand());
		viewer.setShapeProperty(iShape, "title", new String[] { script });
		int colorRangeStage = 0;
		int signPt = 0;
		boolean surfaceObjectSeen = false;
		boolean planeSeen = false;
		float[] nlmZ = new float[5];
		String str;
		int modelIndex = viewer.getDisplayModelIndex();
		if (modelIndex < 0)
			evalError(GT._("the {0} command requires that only one model be displayed", "ISOSURFACE"));
		for (int i = 1; i < statementLength; ++i) {
			String propertyName = null;
			Object propertyValue = null;
			Token token = statement[i];
			switch (token.tok) {
			case Token.select:
				propertyName = "select";
				++i;
				propertyValue = expression(statement, i + 1);
				i = pcLastExpressionInstruction;
				break;
			case Token.center:
				propertyName = "center";
				switch (statement[++i].tok) {
				case Token.dollarsign:
					String id = objectNameParameter(++i);
					propertyValue = viewer.getDrawObjectCenter(id);
					if (propertyValue == null)
						drawObjectNotDefined(id);
					break;
				case Token.expressionBegin:
				case Token.leftbrace:
					propertyValue = atomCenterOrCoordinateParameter(i);
					i = pcLastExpressionInstruction;
					break;
				default:
					coordinateOrNameOrExpressionRequired();
				}
				break;
			case Token.color:
				/*
				 * "color" now is just used as an equivalent to "sign" and as an introduction to "absolute" any other
				 * use is superfluous; it has been replaced with MAP for indicating "use the current surface" because
				 * the term COLOR is too general.
				 * 
				 */
				if (i + 1 == statementLength)
					invalidArgument(i, " COLOR keyword not in context");
				colorRangeStage = 0;
				switch (statement[i + 1].tok) {
				case Token.absolute:
					++i;
					colorRangeStage = 1;
					continue;
				case Token.colorRGB:
					signPt = i + 1;
				}
				continue;
			case Token.file:
				continue;
			case Token.plus:
				if (colorRangeStage == 0) {
					propertyName = "cutoffPositive";
					propertyValue = new Float(floatParameter(++i));
				}
				break;
			case Token.decimal:
			case Token.integer:
				// default is "cutoff"
				propertyName = (colorRangeStage == 1 ? "red" : colorRangeStage == 2 ? "blue" : "cutoff");
				propertyValue = new Float(floatParameter(i));
				if (colorRangeStage > 0)
					++colorRangeStage;
				break;
			case Token.colorRGB:
				if (i != signPt && i != signPt + 1)
					invalidParameterOrder();
				propertyName = "colorRGB";
				propertyValue = new Integer(getArgbParam(i));
				break;
			case Token.ionic:
				propertyName = "ionicRadius";
				propertyValue = new Float(radiusParameter(++i, 0));
				i = pcLastExpressionInstruction;
				break;
			case Token.vanderwaals:
				propertyName = "vdwRadius";
				propertyValue = new Float(radiusParameter(++i, 0));
				i = pcLastExpressionInstruction;
				break;
			case Token.identifier:
				str = (String) token.value;
				if (str.equalsIgnoreCase("REMAPPABLE")) { // testing only
					propertyName = "remappable";
					break;
				}
				if (str.equalsIgnoreCase("IGNORE")) {
					propertyName = "ignore";
					++i;
					propertyValue = expression(statement, i + 1);
					i = pcLastExpressionInstruction;
					break;
				}
				if (str.equalsIgnoreCase("CUTOFF")) {
					if (++i < statementLength && statement[i].tok == Token.plus) {
						propertyName = "cutoffPositive";
						propertyValue = new Float(floatParameter(++i));
					}
					else {
						propertyName = "cutoff";
						propertyValue = new Float(floatParameter(i));
					}
					break;
				}
				if (str.equalsIgnoreCase("SCALE")) {
					propertyName = "scale";
					propertyValue = new Float(floatParameter(++i));
					break;
				}
				if (str.equalsIgnoreCase("ANGSTROMS")) {
					propertyName = "angstroms";
					break;
				}
				if (str.equalsIgnoreCase("RESOLUTION")) {
					propertyName = "resolution";
					propertyValue = new Float(floatParameter(++i));
					break;
				}
				if (str.equalsIgnoreCase("ANISOTROPY")) {
					propertyName = "anisotropy";
					propertyValue = getCoordinate(i + 1, false);
					i = pcLastExpressionInstruction;
					break;
				}
				if (str.equalsIgnoreCase("ECCENTRICITY")) {
					propertyName = "eccentricity";
					propertyValue = getPoint4f(i + 1);
					i = pcLastExpressionInstruction;
					break;
				}
				if (str.equalsIgnoreCase("FIXED")) {
					propertyName = "fixed";
					propertyValue = Boolean.TRUE;
					break;
				}
				if (str.equalsIgnoreCase("MODELBASED")) {
					propertyName = "fixed";
					propertyValue = Boolean.FALSE;
					break;
				}
				if (str.equalsIgnoreCase("SIGN")) {
					signPt = i + 1;
					propertyName = "sign";
					propertyValue = Boolean.TRUE;
					colorRangeStage = 1;
					break;
				}
				if (str.equalsIgnoreCase("INSIDEOUT")) { // no longer of use?
					propertyName = "insideOut";
					break;
				}
				if (str.equalsIgnoreCase("REVERSECOLOR")) {
					propertyName = "reverseColor";
					propertyValue = Boolean.TRUE;
					break;
				}
				if (str.equalsIgnoreCase("ADDHYDROGENS")) {
					propertyName = "addHydrogens";
					propertyValue = Boolean.TRUE;
					break;
				}
				if (str.equalsIgnoreCase("COLORSCHEME")) {
					propertyName = "setColorScheme";
					propertyValue = stringParameter(++i);
					break;
				}
				if (str.equalsIgnoreCase("DEBUG") || str.equalsIgnoreCase("NODEBUG")) {
					propertyName = "debug";
					propertyValue = (str.equalsIgnoreCase("DEBUG") ? Boolean.TRUE : Boolean.FALSE);
					break;
				}
				if (str.equalsIgnoreCase("GRIDPOINTS")) {
					propertyName = "gridPoints";
					break;
				}
				if (str.equalsIgnoreCase("CONTOUR")) {
					propertyName = "contour";
					propertyValue = new Integer(
							i + 1 < statementLength && statement[i + 1].tok == Token.integer ? intParameter(++i) : 0);
					break;
				}
				if (str.equalsIgnoreCase("PHASE")) {
					propertyName = "phase";
					propertyValue = (i + 1 < statementLength && statement[i + 1].tok == Token.string ? stringParameter(++i)
							: "_orb");
					break;
				}
				// surface objects
				if (str.equalsIgnoreCase("MAP")) { // "use current"
					surfaceObjectSeen = true;
					propertyName = "map";
					break;
				}
				if (str.equalsIgnoreCase("plane")) {
					// plane {X, Y, Z, W}
					planeSeen = true;
					propertyName = "plane";
					propertyValue = planeParameter(++i);
					i = pcLastExpressionInstruction;
					break;
				}
				if (str.equalsIgnoreCase("hkl")) {
					// miller indices hkl
					planeSeen = true;
					propertyName = "plane";
					propertyValue = hklParameter(++i);
					i = pcLastExpressionInstruction;
					break;
				}
				if (str.equalsIgnoreCase("sphere")) {
					// sphere [radius]
					surfaceObjectSeen = true;
					propertyName = "sphere";
					propertyValue = new Float(floatParameter(++i));
					break;
				}
				if (str.equalsIgnoreCase("ellipsoid")) {
					// ellipsoid {xc yc zc f} where a = b and f = a/c
					surfaceObjectSeen = true;
					propertyName = "ellipsoid";
					propertyValue = getPoint4f(i + 1);
					i = pcLastExpressionInstruction;
					break;
				}
				if (str.equalsIgnoreCase("lobe")) {
					// lobe {eccentricity}
					surfaceObjectSeen = true;
					propertyName = "lobe";
					propertyValue = getPoint4f(i + 1);
					i = pcLastExpressionInstruction;
					break;
				}
				if (str.equalsIgnoreCase("AtomicOrbital") || str.equalsIgnoreCase("orbital")) {
					surfaceObjectSeen = true;
					nlmZ[0] = intParameter(++i);
					nlmZ[1] = intParameter(++i);
					nlmZ[2] = intParameter(++i);
					nlmZ[3] = (isFloatParameter(i + 1) ? floatParameter(++i) : 6f);
					propertyName = "hydrogenOrbital";
					propertyValue = nlmZ;
					break;
				}
				if (str.equalsIgnoreCase("functionXY")) {
					surfaceObjectSeen = true;
					Vector v = new Vector();
					if (++i == statementLength)
						badArgumentCount();
					if (statement[i].tok != Token.string)
						invalidArgument(i, "function name in quotation marks expected");
					v.add(statement[i++].value);
					v.add(getCoordinate(i, false));
					v.add(getPoint4f(pcLastExpressionInstruction + 1));
					v.add(getPoint4f(pcLastExpressionInstruction + 1));
					v.add(getPoint4f(pcLastExpressionInstruction + 1));
					i = pcLastExpressionInstruction;
					propertyName = "functionXY";
					propertyValue = v;
					break;
				}
				if (str.equalsIgnoreCase("molecular")) {
					surfaceObjectSeen = true;
					propertyName = "molecular";
					propertyValue = new Float(1.4);
					break;
				}
				propertyValue = token.value;
				// fall through for identifiers
			case Token.all:
				propertyName = "thisID";
				break;
			case Token.lcaocartoon:
				surfaceObjectSeen = true;
				String lcaoType = stringParameter(++i);
				viewer.setShapeProperty(iShape, "lcaoType", lcaoType);
				switch (statement[++i].tok) {
				case Token.expressionBegin:
					propertyName = "lcaoCartoon";
					int atomIndex = viewer.firstAtomOf(expression(statement, ++i));
					if (atomIndex < 0)
						expressionExpected();
					viewer.setShapeProperty(iShape, "modelIndex", new Integer(viewer.getAtomModelIndex(atomIndex)));
					Vector3f[] axes = { new Vector3f(), new Vector3f(), new Vector3f(viewer.getAtomPoint3f(atomIndex)) };
					viewer.getPrincipalAxes(atomIndex, axes[0], axes[1], lcaoType, false);
					i = pcLastExpressionInstruction;
					propertyValue = axes;
					break;
				default:
					expressionExpected();
				}
				break;
			case Token.mo:
				// mo 1-based-index
				if (++i == statementLength)
					badArgumentCount();
				int moNumber = intParameter(i);
				setMoData(iShape, moNumber, null);
				surfaceObjectSeen = true;
				continue;
			case Token.mep:
				float[] partialCharges = null;
				try {
					partialCharges = viewer.getFrame().partialCharges;
				}
				catch (Exception e) {
				}
				if (partialCharges == null)
					evalError(GT
							._("No partial charges were read from the file; Jmol needs these to render the MEP data."));
				surfaceObjectSeen = true;
				propertyName = "mep";
				propertyValue = partialCharges;
				break;
			case Token.sasurface:
			case Token.solvent:
				surfaceObjectSeen = true;
				propertyName = (token.tok == Token.sasurface ? "sasurface" : "solvent");
				float radius = (isFloatParameter(i + 1) ? floatParameter(++i) : viewer.getSolventProbeRadius());
				propertyValue = new Float(radius);
				break;
			case Token.string:
				propertyName = surfaceObjectSeen || planeSeen ? "mapColor" : "getSurface";
				/*
				 * a file name, optionally followed by an integer file index. OR empty. In that case, if the model
				 * auxiliary info has the data stored in it, we use that. There are two possible structures:
				 * 
				 * jmolSurfaceInfo jmolMappedDataInfo
				 * 
				 * Both can be present, but if jmolMappedDataInfo is missing, then jmolSurfaceInfo is used by default.
				 * 
				 */
				String filename = (String) token.value;
				if (filename.length() == 0) {
					if (surfaceObjectSeen || planeSeen)
						propertyValue = viewer.getModelAuxiliaryInfo(modelIndex, "jmolMappedDataInfo");
					if (propertyValue == null)
						propertyValue = viewer.getModelAuxiliaryInfo(modelIndex, "jmolSurfaceInfo");
					surfaceObjectSeen = true;
					if (propertyValue != null)
						break;
					filename = viewer.getFullPathName();
				}
				surfaceObjectSeen = true;
				if (i + 1 < statementLength && statement[i + 1].tok == Token.integer)
					viewer.setShapeProperty(iShape, "fileIndex", new Integer(intParameter(++i)));
				Object t = viewer.getUnzippedBufferedReaderOrErrorMessageFromName(filename);
				if (t instanceof String)
					fileNotFoundException(filename + ":" + t);
				Logger.info("reading isosurface data from " + filename);
				propertyValue = t;
				break;
			default:
				if (!setMeshDisplayProperty(iShape, token.tok))
					invalidArgument(i, statement[i] + " not recognized");
			}
			if (propertyName != null)
				viewer.setShapeProperty(iShape, propertyName, propertyValue);
		}
		if (planeSeen && !surfaceObjectSeen) {
			viewer.setShapeProperty(iShape, "nomap", new Float(0));
		}
	}

	boolean setMeshDisplayProperty(int shape, int tok) {
		String propertyName = null;
		Object propertyValue = null;
		switch (tok) {
		case Token.on:
			propertyName = "on";
			break;
		case Token.off:
			propertyName = "off";
			break;
		case Token.delete:
			propertyName = "delete";
			break;
		case Token.dots:
			propertyValue = Boolean.TRUE;
		case Token.nodots:
			propertyName = "dots";
			break;
		case Token.mesh:
			propertyValue = Boolean.TRUE;
		case Token.nomesh:
			propertyName = "mesh";
			break;
		case Token.fill:
			propertyValue = Boolean.TRUE;
		case Token.nofill:
			propertyName = "fill";
			break;
		case Token.translucent:
			propertyName = "translucency";
			propertyValue = "translucent";
			break;
		case Token.opaque:
			propertyName = "translucency";
			propertyValue = "opaque";
			break;
		}
		if (propertyName == null)
			return false;
		viewer.setShapeProperty(shape, propertyName, propertyValue);
		return true;
	}

	// //// script exceptions ///////

	boolean ignoreError;

	void evalError(String message) throws ScriptException {
		if (ignoreError)
			throw new NullPointerException();
		String s = viewer.removeCommand();
		viewer.addCommand(s + CommandHistory.ERROR_FLAG);
		throw new ScriptException(message, getLine(), filename, getLinenumber());
	}

	void unrecognizedCommand(Token token) throws ScriptException {
		evalError(GT._("unrecognized command") + ": " + token.value);
	}

	void unrecognizedAtomProperty(int propnum) throws ScriptException {
		evalError(GT._("unrecognized atom property") + ": " + propnum);
	}

	void filenameExpected() throws ScriptException {
		evalError(GT._("filename expected"));
	}

	void booleanExpected() throws ScriptException {
		evalError(GT._("boolean expected"));
	}

	void booleanOrPercentExpected() throws ScriptException {
		evalError(GT._("boolean or percent expected"));
	}

	void booleanOrNumberExpected() throws ScriptException {
		evalError(GT._("boolean or number expected"));
	}

	void booleanOrNumberExpected(String orWhat) throws ScriptException {
		evalError(GT._("boolean, number, or {0} expected", "\"" + orWhat + "\""));
	}

	void expressionOrDecimalExpected() throws ScriptException {
		evalError(GT._("(atom expression) or decimal number expected"));
	}

	void expressionOrIntegerExpected() throws ScriptException {
		evalError(GT._("(atom expression) or integer expected"));
	}

	void expressionExpected() throws ScriptException {
		evalError(GT._("valid (atom expression) expected"));
	}

	void rotationPointsIdentical() throws ScriptException {
		evalError(GT._("rotation points cannot be identical"));
	}

	void integerExpected() throws ScriptException {
		evalError(GT._("integer expected"));
	}

	void numberExpected() throws ScriptException {
		evalError(GT._("number expected"));
	}

	void stringExpected() throws ScriptException {
		evalError(GT._("quoted string expected"));
	}

	void stringOrIdentifierExpected() throws ScriptException {
		evalError(GT._("quoted string or identifier expected"));
	}

	void propertyNameExpected() throws ScriptException {
		evalError(GT._("property name expected"));
	}

	void axisExpected() throws ScriptException {
		evalError(GT._("x y z axis expected"));
	}

	void colorExpected() throws ScriptException {
		evalError(GT._("color expected"));
	}

	void keywordExpected() throws ScriptException {
		evalError(GT._("keyword expected"));
	}

	void unrecognizedObject() throws ScriptException {
		evalError(GT._("unrecognized object"));
	}

	void unrecognizedExpression() throws ScriptException {
		evalError(GT._("runtime unrecognized expression"));
	}

	void undefinedVariable(String varName) throws ScriptException {
		evalError(GT._("variable undefined") + ": " + varName);
	}

	void endOfStatementUnexpected() throws ScriptException {
		evalError(GT._("unexpected end of script command"));
	}

	void badArgumentCount() throws ScriptException {
		evalError(GT._("bad argument count"));
	}

	void invalidArgument() throws ScriptException {
		String str = "";
		for (int i = 0; i < statementLength; i++)
			str += "\n" + statement[i].toString();
		evalError(GT._("invalid argument") + str);
	}

	void invalidArgument(int ipt, String info) throws ScriptException {
		String str = "";
		for (int i = 0; i <= ipt; i++)
			str += "\n" + statement[i].toString();
		evalError(GT._("invalid argument") + " - " + info + str);
	}

	void unrecognizedSetParameter() throws ScriptException {
		evalError(GT._("unrecognized {0} parameter", "SET"));
	}

	void unrecognizedSubcommand(String cmd) throws ScriptException {
		evalError(GT._("unrecognized subcommand") + ": " + cmd);
	}

	void subcommandExpected() throws ScriptException {
		evalError(GT._("subcommand expected"));
	}

	void setspecialShouldNotBeHere() throws ScriptException {
		evalError(GT._("interpreter error - setspecial should not be here"));
	}

	void numberOutOfRange() throws ScriptException {
		evalError(GT._("number out of range"));
	}

	void numberOutOfRange(int min, int max) throws ScriptException {
		evalError(GT._("integer out of range ({0} - {1})", new Object[] { new Integer(min), new Integer(max) }));
	}

	void numberOutOfRange(float min, float max) throws ScriptException {
		evalError(GT._("decimal number out of range ({0} - {1})", new Object[] { new Float(min), new Float(max) }));
	}

	void numberMustBe(int a, int b) throws ScriptException {
		evalError(GT._("number must be ({0} or {1})", new Object[] { new Integer(a), new Integer(b) }));
	}

	void badAtomNumber() throws ScriptException {
		evalError(GT._("bad atom number"));
	}

	void errorLoadingScript(String msg) throws ScriptException {
		evalError(GT._("error loading script") + " -> " + msg);
	}

	void fileNotFoundException(String filename) throws ScriptException {
		evalError(GT._("file not found") + ": " + filename);
	}

	void drawObjectNotDefined(String drawID) throws ScriptException {
		evalError(GT._("draw object not defined") + ": " + drawID);
	}

	void objectNameExpected() throws ScriptException {
		evalError(GT._("object name expected after '$'"));
	}

	void coordinateExpected() throws ScriptException {
		evalError(GT._("{ number number number } expected"));
	}

	void coordinateOrNameOrExpressionRequired() throws ScriptException {
		evalError(GT._(" {x y z} or $name or (atom expression) required"));
	}

	void tooManyRotationPoints() throws ScriptException {
		evalError(GT._("too many rotation points were specified"));
	}

	void keywordExpected(String what) throws ScriptException {
		evalError(GT._("keyword expected") + ": " + what);
	}

	void notImplemented(int itoken) {
		notImplemented(statement[itoken]);
	}

	void notImplemented(Token token) {
		viewer.scriptStatus("script ERROR: " + token.value + " not implemented in command:" + statement[0].value);
	}

	void invalidParameterOrder() throws ScriptException {
		evalError(GT._("invalid parameter order"));
	}

	void incompatibleArguments() throws ScriptException {
		evalError(GT._("incompatible arguments"));
	}

	void insufficientArguments() throws ScriptException {
		evalError(GT._("insufficient arguments"));
	}

	class ScriptException extends Exception {

		String message;
		String line;
		String fileName;
		int linenumber;

		ScriptException(String message, String line, String filename, int linenumber) {
			this.message = message;
			this.line = line;
			this.fileName = filename;
			this.linenumber = linenumber;
			Logger.error(toString());
		}

		public String toString() {
			String str = "ScriptException:" + message;
			if (line != null)
				str += "\n    Script line:" + line;
			if (fileName != null)
				str += "\n           File:" + fileName + " Line number:" + linenumber;
			return str;
		}
	}
}
