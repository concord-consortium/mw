/*
 *   Copyright (C) 2006  The Concord Consortium, Inc.,
 *   25 Love Lane, Concord, MA 01742
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * END LICENSE */
package org.concord.modeler.script;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import org.concord.modeler.ConnectionManager;
import org.concord.modeler.MidiPlayer;
import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.SampledAudioPlayer;
import org.concord.modeler.ScriptCallback;
import org.concord.modeler.Scriptable;
import org.concord.modeler.event.ScriptEvent;
import org.concord.modeler.event.ScriptListener;
import org.concord.modeler.ui.HTMLPane;
import org.concord.modeler.util.Evaluator;
import org.concord.modeler.util.FileUtilities;

import static java.util.regex.Pattern.*;
import static org.concord.modeler.script.Compiler.*;

/**
 * <p>
 * This class evaluates MW scripts and translates them into actions on molecular models.
 * </p>
 * <p>
 * Known problems: (a) Some script commands cannot be put into EventQueue to be invoked later because subsequent
 * commands depend on the results of them.
 * </p>
 * 
 * @see org.concord.modeler.script.Compiler
 * @author Charles Xie
 */

public abstract class AbstractEval {

	protected final static float ZERO = 1000000 * Float.MIN_VALUE;
	protected final static byte ON = 51;
	protected final static byte OFF = 52;
	protected final static Pattern NNI = compile("\\d+");
	protected final static short DELAY_FRACTION = 500;

	protected static Evaluator mathEval;
	protected Stack<Object> logicalStack;
	protected Map<String, String> definition;
	protected static Map<Class, Map<String, String>> sharedDefinition;
	protected List<String> postfix;
	protected short nLoop = 10;
	protected short iLoop;
	protected volatile boolean stop = true;
	protected volatile String script;
	protected volatile boolean interrupted;
	protected List<String[]> loopList;
	protected Map<String, String> withinMap;
	protected Map<Byte, String> externalScripts;
	protected Map<Integer, String> mouseScripts, keyScripts;
	protected Point mouseLocation;
	protected int keyCode;
	protected static Map<String, Integer> cursorIDMap;
	protected LinkedList<String> scriptQueue;

	// used in translateInfixToPostfix, parentheses are parsed along with the logical operators
	private final static Pattern LOGICAL_EXPRESSION = compile("(" + REGEX_NOT + ")|(" + REGEX_OR + ")|(" + REGEX_AND
			+ ")|\\(|\\)");

	private final static int MAX_NESTED_DEPTH = 10;
	private int ifLevel;
	private boolean[] foundIf = new boolean[MAX_NESTED_DEPTH];
	private boolean[] foundElse = new boolean[MAX_NESTED_DEPTH];
	private boolean[] ifTrue = new boolean[MAX_NESTED_DEPTH];
	private volatile boolean stopWhile;
	private int ifLevelBeforeWhile;
	private boolean firstWhileFalse;

	private List<ScriptListener> listenerList;
	private boolean asTask;
	private ScriptCallback externalScriptCallback;
	private List<String> commentedOutScripts;

	private MidiPlayer midiPlayer;
	private SampledAudioPlayer sampledAudioPlayer;

	private boolean notifySaver = true;

	protected AbstractEval() {
		scriptQueue = new LinkedList<String>();
		if (mathEval == null)
			mathEval = new Evaluator();
		logicalStack = new Stack<Object>();
		definition = Collections.synchronizedMap(new HashMap<String, String>());
		if (sharedDefinition == null)
			sharedDefinition = Collections.synchronizedMap(new HashMap<Class, Map<String, String>>());
		loopList = Collections.synchronizedList(new ArrayList<String[]>());
		externalScripts = Collections.synchronizedMap(new TreeMap<Byte, String>());
		commentedOutScripts = Collections.synchronizedList(new ArrayList<String>());
		mouseScripts = Collections.synchronizedMap(new HashMap<Integer, String>());
		keyScripts = Collections.synchronizedMap(new HashMap<Integer, String>());
		mouseLocation = new Point();
	}

	protected abstract Object getModel();

	public void setNotifySaver(boolean b) {
		notifySaver = b;
	}

	public boolean getNotifySaver() {
		return notifySaver;
	}

	public void setExternalScriptCallback(ScriptCallback c) {
		externalScriptCallback = c;
	}

	public ScriptCallback getExternalScriptCallback() {
		return externalScriptCallback;
	}

	protected void setAsTask(boolean b) {
		asTask = b;
	}

	protected boolean getAsTask() {
		return asTask;
	}

	public void setDefinition(Map<String, String> definition) {
		this.definition = definition;
	}

	public Map<String, String> getDefinition() {
		return definition;
	}

	public void addScriptListener(ScriptListener listener) {
		if (listenerList == null)
			listenerList = new CopyOnWriteArrayList<ScriptListener>();
		if (!listenerList.contains(listener))
			listenerList.add(listener);
	}

	public void removeScriptListener(ScriptListener listener) {
		if (listenerList == null)
			return;
		listenerList.remove(listener);
	}

	public void removeAllScriptListeners() {
		if (listenerList == null)
			return;
		listenerList.clear();
	}

	protected void notifyScriptListener(ScriptEvent e) {
		if (interrupted)
			return;
		if (listenerList == null)
			return;
		synchronized (listenerList) {
			for (ScriptListener l : listenerList) {
				l.outputScriptResult(e);
			}
		}
	}

	public void appendScript(String s) {
		if (stop) {
			halt();
			setScript(s);
		}
		else {
			s = s.trim();
			// cancel current and pending scripts
			if (s.length() >= 6 && s.substring(0, 6).equalsIgnoreCase("cancel")) {
				halt();
				clearScriptQueue();
				try {
					Thread.sleep(100); // give it a little while to halt
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
				setScript(s.substring(6));
			}
			else {
				scriptQueue.addLast(s);
			}
		}
	}

	protected void setScript(String script) {
		this.script = script;
		resetNestedConstruct();
	}

	/**
	 * The following was used to interrupt the evalThread to get out from the sleep method, which is used in delay. The
	 * consequence is that a new thread has to be created after the current one is interrupted. An alternative is to
	 * slice the sleeping time into a fraction of second so that the script thread does not get blocked for too long in
	 * the sleep method.
	 * 
	 * <pre>
	 * if (evalThread != null) {
	 * 	if (!eval.isStopped()) {
	 * 		evalThread.interrupt();
	 * 		evalThread = null;
	 * 	}
	 * }
	 * </pre>
	 */
	public void halt() {
		if (!isStopped()) {
			stop();
		}
		stopLoops();
	}

	public void clearScriptQueue() {
		scriptQueue.clear();
	}

	public double parseMathExpression(String expression) {
		if (expression == null)
			return Double.NaN;
		if (expression.indexOf("%") >= 0)
			expression = replaceVariablesWithValues(useDefinitions(expression));
		mathEval.setExpression(expression);
		double x = 0;
		try {
			x = mathEval.eval();
		}
		catch (Exception e) {
			out(ScriptEvent.FAILED, expression + " cannot be parsed as a number.");
			return Double.NaN;
		}
		return x;
	}

	protected double parseKeywordValue(String keyword, String s) {
		int i = s.indexOf(keyword);
		if (i != -1) {
			s = s.substring(i + keyword.length()).trim();
			return parseMathExpression(s);
		}
		return Double.NaN;
	}

	protected int parseInt(String s) throws Exception {
		try {
			return Float.valueOf(s).intValue();
		}
		catch (NumberFormatException e) {
			out(ScriptEvent.FAILED, s + " cannot be parsed as integer number.");
			throw new NumberFormatException(e.getMessage());
		}
	}

	protected static boolean inRangeInclusive(int x, int beg, int end) {
		return x <= end && x >= beg;
	}

	protected static byte parseOnOff(String keyword, String s) {
		if (!s.startsWith(keyword))
			return (byte) -1;
		int i = s.indexOf(keyword);
		if (i != -1) {
			s = s.substring(i + keyword.length()).trim();
			if (s.equals("") || s.equals("on"))
				return ON;
			if (s.equals("off"))
				return OFF;
		}
		return (byte) -1;
	}

	protected Color parseRGBColor(String str) {
		if (RGB_COLOR.matcher(str).find())
			return parseRGBA(str);
		if (RGBA_COLOR.matcher(str).find())
			return parseRGBA(str);
		if (str.startsWith("0x")) {
			try {
				return new Color(Integer.valueOf(str.substring(2), 16));
			}
			catch (NumberFormatException e) {
				out(ScriptEvent.FAILED, str + " cannot be parsed as color.");
				return null;
			}
		}
		if (str.startsWith("#")) {
			try {
				return new Color(Integer.valueOf(str.substring(1), 16));
			}
			catch (NumberFormatException e) {
				out(ScriptEvent.FAILED, str + " cannot be parsed as color.");
				return null;
			}
		}
		Color c = parseRGBA(str);
		if (c == null)
			out(ScriptEvent.FAILED, str + " cannot be parsed as color.");
		return c;
	}

	private Color parseRGBA(String str) {
		int i0 = str.indexOf("[");
		if (i0 == -1)
			i0 = str.indexOf("(");
		int i1 = str.indexOf("]");
		if (i1 == -1)
			i1 = str.indexOf(")");
		str = str.substring(i0 + 1, i1);
		String[] s = str.split(REGEX_SEPARATOR + "+");
		if (s.length < 3) {
			out(ScriptEvent.FAILED, "Cannot parse color from less than 3 parameters: " + str);
			return null;
		}
		double x = parseMathExpression(s[0]);
		if (Double.isNaN(x)) {
			out(ScriptEvent.FAILED, "Cannot parse red color: " + s[0]);
			return null;
		}
		int r = (int) Math.round(x);
		x = parseMathExpression(s[1]);
		if (Double.isNaN(x)) {
			out(ScriptEvent.FAILED, "Cannot parse green color: " + s[1]);
			return null;
		}
		int g = (int) Math.round(x);
		x = parseMathExpression(s[2]);
		if (Double.isNaN(x)) {
			out(ScriptEvent.FAILED, "Cannot parse blue color: " + s[2]);
			return null;
		}
		int b = (int) Math.round(x);
		if (s.length < 4)
			return new Color(r % 256, g % 256, b % 256);
		x = parseMathExpression(s[3]);
		if (Double.isNaN(x)) {
			out(ScriptEvent.FAILED, "Cannot parse alpha value: " + s[3]);
			return null;
		}
		int a = (int) Math.round(x);
		return new Color(r % 256, g % 256, b % 256, a % 256);
	}

	protected float[] parseArray(final int n, String str) {
		str = str.trim();
		if (str.startsWith("("))
			str = str.substring(1);
		if (str.endsWith(")"))
			str = str.substring(0, str.length() - 1);
		String[] s = str.split(",");
		if (s.length != n) {
			out(ScriptEvent.FAILED, "Cannot split into " + n + " arguments: " + str);
			return null;
		}
		return parseArray(n, s);
	}

	protected float[] parseArray(int n, String[] s) {
		if (n > s.length) {
			out(ScriptEvent.FAILED, "index out of bound: " + n + " > " + s.length);
			return null;
		}
		float[] x = new float[n];
		double z = 0;
		for (int i = 0; i < n; i++) {
			z = parseMathExpression(s[i]);
			if (Double.isNaN(z)) {
				out(ScriptEvent.FAILED, "Cannot parse : " + s[i]);
				return null;
			}
			x[i] = (float) z;
		}
		return x;
	}

	private void resetNestedConstruct() {
		ifLevel = 0;
		ifLevelBeforeWhile = 0;
		Arrays.fill(foundIf, false);
		Arrays.fill(foundElse, false);
		Arrays.fill(ifTrue, false);
	}

	public synchronized void stopLoops() {
		stopWhile = true;
	}

	/**
	 * do not synchronize this method, because the event thread needs to call this to paint a busy icon on the view
	 * window to indicate the script is being executed. The script execution may take a long time and freeze the
	 * view-painting process.
	 */
	public boolean isStopped() {
		return stop;
	}

	/** same as above, do not synchronize this method. */
	public void stop() {
		stop = true;
	}

	/**
	 * If the script is "command 1; ...; loop 10; command k; ...; loop 20; command p; ...", break it down into segments
	 * separated by the loop keyword, such as: command 1; ...; loop 10; | command k; ...; loop 20; | command p; ...; If
	 * there is no loop-keyword separator, do not break it down. The commands before a loop statement will be executed
	 * as many times as specified by the statement.
	 */
	protected void evalCommandSet(String[] command) throws InterruptedException {
		ifLevelBeforeWhile = 0;
		processLoop(command);
	}

	// processing loop
	private void processLoop(String[] command) throws InterruptedException {
		loopList.clear();
		int n = command.length;
		int k = 0;
		String ci;
		String[] s = null;
		for (int i = 0; i < n; i++) {
			if (interrupted || stop)
				return;
			ci = command[i].trim().toLowerCase();
			if (ci.startsWith("loop")) {
				s = new String[k + 1];
				for (int j = 0; j < k + 1; j++)
					s[j] = command[i - k + j];
				loopList.add(s);
				k = 0;
			}
			else {
				k++;
			}
		}
		if (loopList.isEmpty()) {
			evalCommandSet2(command);
		}
		else {
			if (k > 0) {
				s = new String[k];
				for (int j = 0; j < k; j++)
					s[j] = command[n - k + j];
				loopList.add(s);
			}
			synchronized (loopList) {
				for (String[] t : loopList) {
					if (interrupted || stop)
						return;
					if (t.length == 1 && t[0].trim().toLowerCase().startsWith("loop"))
						continue;
					iLoop = 0;
					nLoop = 10;
					evalCommandSet2(t);
				}
			}
		}
	}

	private boolean testWhile(String command) {
		int lp = command.indexOf("(");
		int rp = command.lastIndexOf(")");
		String whileExpression = command.substring(lp + 1, rp);
		return evaluateLogicalExpression(whileExpression);
	}

	/**
	 * evaluate the set of commands broken from the script. Return a boolean to indicate the scripts (e.g. those within
	 * a while loop) should continue.
	 */
	protected boolean evalCommandSet2(String[] command) throws InterruptedException {

		String ci = null;
		int n = command.length;
		int iWhile = -1;
		String[] whileLoopCommands = null;
		for (int i = 0; i < n; i++) {
			if (interrupted || stop)
				return false;
			ci = command[i].trim();
			if (ci.equals(""))
				continue;

			// handling while
			if (WHILE.matcher(ci).find()) {
				iWhile = i;
				firstWhileFalse = !testWhile(command[iWhile]);
				stopWhile = false;
				ifLevelBeforeWhile = ifLevel;
				whileLoopCommands = null;
				continue;
			}
			else if (ENDWHILE.matcher(ci).find()) {
				if (iWhile < 0) {
					out(ScriptEvent.FAILED, "No matching while is found.");
					return false;
				}
				if (ifLevel > 0 && skipIf()) {
					iWhile = -1;
					firstWhileFalse = false;
					ifLevelBeforeWhile = 0;
					continue;
				}
				whileLoopCommands = new String[i - iWhile - 1];
				for (int j = 0; j < whileLoopCommands.length; j++)
					whileLoopCommands[j] = command[iWhile + 1 + j];
				while (testWhile(command[iWhile])) {
					if (stopWhile) {
						// System.out.println("interrupting the while loop: "+command[iWhile]);
						break;
					}
					if (!evalCommandSet2(whileLoopCommands))
						break;
					if (!getAsTask()) {
						// for action scripts, we had better sleep briefly for it to be interruptible
						try {
							Thread.sleep(1);
						}
						catch (InterruptedException e) {
						}
					}
				}
				iWhile = -1;
				firstWhileFalse = false;
				ifLevelBeforeWhile = 0;
				continue;
			}

			// handling if
			if (IF.matcher(ci).find()) {
				if (ifLevel > MAX_NESTED_DEPTH - 1) {
					out(ScriptEvent.FAILED, "Too many nested if commands (maximum = " + MAX_NESTED_DEPTH + ").");
					return false;
				}
				foundIf[ifLevel] = true;
				foundElse[ifLevel] = false;
				int lp = ci.indexOf("(");
				int rp = ci.lastIndexOf(")");
				String ifExpression = ci.substring(lp + 1, rp);
				ifTrue[ifLevel] = skipIf() ? false : evaluateLogicalExpression(ifExpression);
				// ifTrue[ifLevel] = evaluateLogicalExpression(ifExpression);
				ifLevel++;
				continue;
			}
			if (ELSE.matcher(ci).find()) {
				if (ifLevel > ifLevelBeforeWhile) {
					int i1 = ifLevel - 1;
					foundElse[i1] = true;
					foundIf[i1] = false;
				}
				continue;
			}
			if (ENDIF.matcher(ci).find()) {
				if (ifLevel > ifLevelBeforeWhile) {
					ifLevel--;
					foundIf[ifLevel] = foundElse[ifLevel] = ifTrue[ifLevel] = false;
				}
				continue;
			}

			if (ifLevel > 0 && skipIf())
				continue;
			if (firstWhileFalse) // if the while command is evaluated false for the first time
				continue;

			if ("return".equalsIgnoreCase(ci)) {
				stop();
				ifLevel = ifLevelBeforeWhile;
				return false;
			}
			if (COMMENT.matcher(ci).find()) // comments
				continue;
			if (evaluateSingleKeyword(ci))
				continue;
			if (ci.toLowerCase().startsWith("loop")) { // loop
				nLoop = getNLoop(ci);
				if (++iLoop < nLoop) {
					evalCommandSet2(command);
				}
				continue;
			}
			if (!evalCommand(ci))
				return false;
		}
		if (ifLevel != ifLevelBeforeWhile) {
			out(ScriptEvent.FAILED, "Missing endif. If level = " + ifLevel + "(" + ifLevelBeforeWhile + ")");
			return false;
		}
		return true;
	}

	private boolean skipIf() {
		if (ifLevel < 1)
			return false;
		for (int i = 0; i < ifLevel; i++) {
			if ((foundIf[i] && !ifTrue[i]) || (foundElse[i] && ifTrue[i]))
				return true;
		}
		return false;
	}

	/** evaluate individual command. */
	protected abstract boolean evalCommand(String ci) throws InterruptedException;

	@SuppressWarnings("unused")
	protected boolean evaluateSingleKeyword(String str) throws InterruptedException {
		if ("break".equalsIgnoreCase(str)) {
			stopLoops();
			return true;
		}
		return false;
	}

	protected boolean evaluateLogicalExpression(String str) {
		// System.out.println("Expression=" + str);
		if (str == null)
			return false;
		if (AND_NOT.matcher(str).find() || OR_NOT.matcher(str).find()) {
			out(ScriptEvent.FAILED, "Illegal usage: " + str + ". Add parentheses to the not expression.");
			return false;
		}
		str = str.trim();
		str = useDefinitions(str);
		Matcher matcher = LOGICAL_OPERATOR.matcher(str);
		if (!matcher.find())
			return evaluateEquality(str);
		translateInfixToPostfix(str);
		// System.out.println(postfix);
		logicalStack.clear(); // make sure that the stack is empty before it is reused.
		int n = postfix.size();
		for (int i = 0; i < n; i++) {
			String s = postfix.get(i);
			s = s.trim();
			if (s.equalsIgnoreCase("not")) {
				boolean b = (Boolean) logicalStack.pop();
				logicalStack.push(!b ? Boolean.TRUE : Boolean.FALSE);
			}
			else if (s.equalsIgnoreCase("or") || s.equalsIgnoreCase("and")) {
				boolean b1 = false, b2 = false;
				try {
					b2 = (Boolean) logicalStack.pop();
					b1 = (Boolean) logicalStack.pop();
				}
				catch (EmptyStackException e) {
					e.printStackTrace();
					continue;
				}
				boolean b = false;
				if (s.equalsIgnoreCase("or")) {
					b = b || b2;
					b = b || b1;
				}
				else if (s.equalsIgnoreCase("and")) {
					b = b || b2;
					b = b && b1;
				}
				logicalStack.push(b ? Boolean.TRUE : Boolean.FALSE);
			}
			else {
				boolean b = evaluateEquality(s);
				logicalStack.push(b ? Boolean.TRUE : Boolean.FALSE);
			}
		}
		boolean b = (Boolean) logicalStack.pop();
		// System.out.println(str + "=" + b);
		return b;
	}

	private boolean evaluateEquality(String str) {
		if (str.startsWith("\""))
			str = str.substring(1);
		if (str.endsWith("\""))
			str = str.substring(0, str.length() - 1);
		boolean ge = false, gt = false, le = false, lt = false, ne = false;
		boolean eq = str.indexOf("==") != -1;
		if (eq) {
			str = str.replace("==", "-(") + ")";
		}
		else if (ne = (str.indexOf("!=") != -1)) {
			str = str.replace("!=", "-(") + ")";
		}
		else if (ge = (str.indexOf(">=") != -1)) {
			str = str.replace(">=", "-(") + ")";
		}
		else if (gt = (str.indexOf(">") != -1)) {
			str = str.replace(">", "-(") + ")";
		}
		else if (le = (str.indexOf("<=") != -1)) {
			str = str.replace("<=", "-(") + ")";
		}
		else if (lt = (str.indexOf("<") != -1)) {
			str = str.replace("<", "-(") + ")";
		}
		double x = parseMathExpression(str);
		if (Double.isNaN(x))
			return false;
		if (!eq && !ge && !gt && !le && !lt)
			return x != 0;
		return (eq && Math.abs(x) < ZERO) || (ne && Math.abs(x) > ZERO) || (ge && x >= 0) || (gt && x > 0)
				|| (le && x <= 0) || (lt && x < 0);
	}

	protected String removeCommentedOutScripts(String script) {
		commentedOutScripts.clear();
		if (script == null)
			return null;
		int beg = script.indexOf("/*");
		int end = script.indexOf("*/");
		int beg0 = -1;
		while (beg != -1 && end != -1) {
			String s = script.substring(beg + 2, end).trim();
			commentedOutScripts.add(s);
			beg0 = beg;
			beg = script.indexOf("/*", end);
			if (beg0 == beg) // infinite loop
				break;
			end = script.indexOf("*/", beg);
		}
		String[] split = script.split("(/\\*|\\*/)");
		int n = split.length;
		for (int i = 0; i < n; i++)
			split[i] = split[i].trim();
		int m = 0;
		for (String s : commentedOutScripts) {
			for (int i = m; i < n; i++) {
				if (split[i].equals(s)) {
					split[i] = "";
					m = i + 1;
				}
			}
		}
		script = "";
		for (String s : split) {
			script += s;
		}
		return script;
	}

	protected void evaluateExternalClause(String s) {
		if (externalScriptCallback == null)
			return;
		if (s == null)
			return;
		externalScriptCallback.setScript(s);
		String msg = externalScriptCallback.execute();
		if (msg != null) {
			if (msg.startsWith(Scriptable.ERROR_HEADER)) {
				out(ScriptEvent.FAILED, "Error in " + s);
			}
		}
	}

	/** separate external scripts and store them */
	protected String separateExternalScripts(String script) {
		// externalScripts.clear();
		// when external blocks intertwine with mouse blocks, they will be reduced to "external #". When
		// they are passed to the mouse callback, setScript(String) will be called, which subsequently
		// calls this method. If we clear the externalScripts map every time, then the "external #" stored
		// in the previous setScript call will be lost.
		if (script == null)
			return null;
		String lowerCase = script.toLowerCase();
		int beg = lowerCase.indexOf("beginexternal");
		int end = lowerCase.indexOf("endexternal");
		int beg0 = -1;
		byte index = 0;
		while (beg != -1 && end != -1) {
			String s = script.substring(beg + 14, end).trim();
			externalScripts.put(index++, s);
			beg0 = beg;
			beg = lowerCase.indexOf("beginexternal", end);
			if (beg0 == beg) // infinite loop
				break;
			end = lowerCase.indexOf("endexternal", beg);
		}
		String[] split = script.split("(?i)(beginexternal|endexternal)[\\s&&[^\\r\\n]]*;");
		int n = split.length;
		for (int i = 0; i < n; i++)
			split[i] = split[i].trim();
		int m = 0;
		for (Byte x : externalScripts.keySet()) {
			for (int i = m; i < n; i++) {
				if (split[i].equals(externalScripts.get(x))) {
					split[i] = "external " + x + ";";
					m = i + 1;
				}
			}
		}
		script = "";
		for (String s : split) {
			script += s;
		}
		return script;
	}

	private String storeMouseScript(int eventType, String beginmouse, String endmouse, String script) {
		String lowerCase = script.toLowerCase();
		int beg = lowerCase.lastIndexOf(beginmouse);
		int end = lowerCase.lastIndexOf(endmouse);
		if (beg != -1 && end != -1 && end > beg) {
			String s = script.substring(beg + beginmouse.length() + 1, end).trim();
			mouseScripts.put(eventType, s);
			return script.substring(0, beg) + script.substring(end + endmouse.length() + 1);
		}
		return script;
	}

	/** store mouse scripts */
	protected String storeMouseScripts(String s) {
		if (s == null)
			return null;
		s = storeMouseScript(MouseEvent.MOUSE_ENTERED, "beginmouse:entered", "endmouse:entered", s);
		s = storeMouseScript(MouseEvent.MOUSE_EXITED, "beginmouse:exited", "endmouse:exited", s);
		s = storeMouseScript(MouseEvent.MOUSE_PRESSED, "beginmouse:pressed", "endmouse:pressed", s);
		s = storeMouseScript(MouseEvent.MOUSE_RELEASED, "beginmouse:released", "endmouse:released", s);
		s = storeMouseScript(MouseEvent.MOUSE_MOVED, "beginmouse:moved", "endmouse:moved", s);
		s = storeMouseScript(MouseEvent.MOUSE_DRAGGED, "beginmouse:dragged", "endmouse:dragged", s);
		return s;
	}

	public void clearMouseScripts() {
		mouseScripts.clear();
	}

	public String getMouseScript(int eventType) {
		return mouseScripts.get(eventType);
	}

	public void setMouseLocation(int x, int y) {
		mouseLocation.setLocation(x, y);
	}

	private String storeKeyScript(int eventType, String beginkey, String endkey, String script) {
		String lowerCase = script.toLowerCase();
		int beg = lowerCase.lastIndexOf(beginkey);
		int end = lowerCase.lastIndexOf(endkey);
		if (beg != -1 && end != -1 && end > beg) {
			String s = script.substring(beg + beginkey.length() + 1, end).trim();
			keyScripts.put(eventType, s);
			return script.substring(0, beg) + script.substring(end + endkey.length() + 1);
		}
		return script;
	}

	/** store keyboard scripts */
	protected String storeKeyScripts(String s) {
		if (s == null)
			return null;
		s = storeKeyScript(KeyEvent.KEY_PRESSED, "beginkey:pressed", "endkey:pressed", s);
		s = storeKeyScript(KeyEvent.KEY_RELEASED, "beginkey:released", "endkey:released", s);
		return s;
	}

	public void clearKeyScripts() {
		keyScripts.clear();
	}

	public String getKeyScript(int eventType) {
		return keyScripts.get(eventType);
	}

	public void setKeyCode(int keyCode) {
		this.keyCode = keyCode;
	}

	/** format string */
	protected String format(String str) {
		Matcher matcher = FORMAT_VARIABLE.matcher(str);
		if (!matcher.find())
			return str;
		Map<String, String> map = new HashMap<String, String>();
		String lowerCase = str.toLowerCase();
		int i = lowerCase.indexOf("formatvar");
		while (i != -1) {
			int a = i + 9;
			int lp = lowerCase.indexOf("(", a);
			int rp = lowerCase.indexOf(")", lp + 1);
			String t = str.substring(lp + 1, rp);
			String[] s = t.split(",");
			if (s.length == 2) {
				boolean success = true;
				try {
					t = String.format(s[0], Double.parseDouble(s[1]));
				}
				catch (Exception e) {
					e.printStackTrace();
					out(ScriptEvent.FAILED, "Format error: " + e.getMessage());
					success = false;
				}
				if (success)
					map.put(str.substring(i, rp + 1), t);
			}
			i = lowerCase.indexOf("formatvar", rp);
		}
		for (String key : map.keySet()) {
			str = str.replace(key, map.get(key));
		}
		return str;
	}

	/** evaluate definitions first before evaluating anything else. */
	protected void evalDefinitions(String[] command) {
		Matcher matcher;
		boolean isStatic = false;
		for (String ci : command) {
			ci = ci.trim();
			if (ci.equals(""))
				continue;
			if (ci.toLowerCase().startsWith("static")) {
				ci = ci.substring(6).trim();
				isStatic = true;
			}
			else {
				isStatic = false;
			}
			matcher = DEFINE_VAR.matcher(ci);
			if (matcher.find()) {
				int end = matcher.end();
				String variable = ci.substring(ci.indexOf("%"), end).toLowerCase();
				String value = ci.substring(end).trim().toLowerCase();
				if (value.startsWith("<t>")) {
					String s;
					if (value.endsWith("</t>")) {
						s = value.substring(3, value.length() - 4);
					}
					else {
						s = value.substring(3);
					}
					if (isStatic) {
						storeSharedDefinition(variable, s);
					}
					else {
						definition.put(variable, s);
					}
				}
				else if (value.startsWith("array")) {
					String size = value.substring(5).trim();
					size = size.substring(1, size.length() - 1);
					double x = parseMathExpression(size);
					if (Double.isNaN(x))
						return;
					createArray(variable, (int) Math.round(x));
				}
				else {
					evaluateDefineMathexClause(isStatic, variable, value);
				}
			}
		}
	}

	protected void createArray(String variable, int length) {
		for (int i = 0; i < length; i++)
			definition.put(variable + "[" + i + "]", "0");
		definition.put(variable + ".length", "" + length);
	}

	public int getArraySize(String variable) {
		String x = definition.get(variable + ".length");
		if (x == null)
			return 0;
		return Integer.parseInt(x);
	}

	public void removeDefinition(String variable) {
		definition.remove(variable);
		String key = null;
		for (Iterator<String> it = definition.keySet().iterator(); it.hasNext();) {
			key = it.next();
			if (key.startsWith(variable + "[") || key.equals(variable + ".length"))
				it.remove();
		}
	}

	public void storeDefinition(boolean isStatic, String variable, String value) {
		int i1 = variable.indexOf("[");
		if (i1 != -1) {
			int i2 = variable.indexOf("]");
			if (i2 != -1) {
				int size = getArraySize(variable.substring(0, i1));
				String index = variable.substring(i1 + 1, i2).trim();
				int ix = 0;
				String s = definition.get(index);
				if (s != null) {
					ix = (int) Double.parseDouble(s);
					variable = variable.substring(0, i1 + 1) + ix + variable.substring(i2);
				}
				else {
					ix = (int) Double.parseDouble(index);
				}
				if (ix >= size) {
					out(ScriptEvent.FAILED, "Array index out of bound (" + size + "): " + ix + " in " + variable);
					return;
				}
			}
		}
		if (isStatic) {
			storeSharedDefinition(variable, value);
		}
		else {
			definition.put(variable, value);
		}
	}

	private void storeSharedDefinition(String variable, String value) {
		Class klass = getModel().getClass();
		Map<String, String> map = sharedDefinition.get(klass);
		if (map == null) {
			map = new HashMap<String, String>();
			sharedDefinition.put(klass, map);
		}
		map.put(variable, value);
	}

	protected void evaluateDefineMathexClause(boolean isStatic, String variable, String value) {
		if (value == null || value.equals(""))
			return;
		value = useDefinitions(value); // make use of the previous definitions
		Matcher matcher = LOGICAL_OPERATOR.matcher(value);
		if (matcher.find()) {
			boolean b = evaluateLogicalExpression(value);
			storeDefinition(isStatic, variable, b ? "1" : "0");
			return;
		}
		else if (value.indexOf("==") != -1 || value.indexOf("!=") != -1 || value.indexOf("<=") != -1
				|| value.indexOf(">=") != -1 || value.indexOf("<") != -1 || value.indexOf(">") != -1) {
			boolean b = evaluateEquality(value);
			storeDefinition(isStatic, variable, b ? "1" : "0");
			return;
		}
		String expression = null;
		int lq = value.indexOf('"');
		int rq = value.indexOf('"', lq + 1);
		if (lq != -1 && rq != -1 && lq != rq) {
			expression = value.substring(lq + 1, rq).trim();
		}
		else {
			if (lq != -1) {
				out(ScriptEvent.FAILED, "Unbalanced quote.");
			}
			else {
				expression = value.trim();
			}
		}
		if (expression != null) {
			double x = parseMathExpression(expression);
			if (Double.isNaN(x))
				return;
			String result;
			if (Math.abs(x - Math.round(x)) < 0.000000001 || expression.startsWith("round(")) {
				result = String.format("%d", Math.round(x));
			}
			else if (expression.startsWith("int(")) {
				result = String.format("%d", (int) x);
			}
			else {
				result = x + "";
			}
			storeDefinition(isStatic, variable, result);
		}
	}

	// when we use a defined array element, we have to evaluate the indices first.
	private String evaluateIndicesOfArrayElements(String s) {
		int lq = s.indexOf('[');
		int rq = s.indexOf(']', lq + 1);
		while (lq != -1 && rq != -1 && lq != rq) {
			double x = parseMathExpression(s.substring(lq + 1, rq).trim());
			if (Double.isNaN(x)) {
				lq = s.indexOf('[', rq + 1);
				rq = s.indexOf(']', lq + 1);
				continue;
			}
			s = s.substring(0, lq + 1) + Math.round(x) + s.substring(rq);
			lq = s.indexOf('[', lq + 1);
			rq = s.indexOf(']', lq + 1);
			if (lq != -1 && rq == -1)
				out(ScriptEvent.FAILED, "Unbalanced square bracket.");
		}
		return s;
	}

	/**
	 * FIXME: what do we do with expression that does not have an obvious word separator between variables, such as
	 * %varone%vartwo? Should it be (%var)one(%var)two, or (%varone)(%vartwo), or something else?
	 */
	protected String useDefinitions(String s) {
		if (s.indexOf("%") == -1)
			return s;
		if (s.startsWith("add image")) { // FIXME: should we generate to all commands?
			StringBuilder sb = new StringBuilder();
			Matcher matcher = DEFINED_VARIABLE.matcher(s);
			int beg = 0;
			int end = 0;
			while (matcher.find()) {
				beg = matcher.start();
				sb.append(s.substring(end, beg));
				end = matcher.end();
				sb.append(s.substring(beg, end).toLowerCase());
			}
			sb.append(s.substring(end));
			s = sb.toString();
		}
		else {
			s = s.toLowerCase();
		}
		s = evaluateIndicesOfArrayElements(s);
		if (!definition.isEmpty()) {
			synchronized (definition) {
				for (String variable : definition.keySet()) {
					if (variable.indexOf("[") != -1) {
						// variable is an element of an array
						s = s.replace(variable, definition.get(variable));
					}
					else {
						// \\b is for avoiding errors when %xx and %x are both present
						s = s.replaceAll(variable + "\\b", definition.get(variable));
					}
				}
			}
		}
		if (!sharedDefinition.isEmpty()) {
			synchronized (sharedDefinition) {
				for (Class klass : sharedDefinition.keySet()) {
					if (klass.isInstance(getModel())) {
						Map<String, String> map = sharedDefinition.get(klass);
						for (String variable : map.keySet()) {
							s = s.replaceAll(variable + "\\b", map.get(variable));
							// \\b is for avoiding errors when %xx and %x are both present
						}
					}
				}
			}
		}
		return s;
	}

	protected String replaceVariablesWithValues(String s) {
		int lq = s.indexOf('"');
		int rq = s.indexOf('"', lq + 1);
		while (lq != -1 && rq != -1 && lq != rq) {
			double x = parseMathExpression(s.substring(lq + 1, rq).trim());
			if (Double.isNaN(x)) {
				lq = s.indexOf('"', rq + 1);
				rq = s.indexOf('"', lq + 1);
				continue;
			}
			s = s.substring(0, lq) + x + s.substring(rq + 1);
			lq = s.indexOf('"');
			rq = s.indexOf('"', lq + 1);
			if (lq != -1 && rq == -1)
				out(ScriptEvent.FAILED, "Unbalanced quote.");
		}
		return s;
	}

	/** translate infix into postfix (stored in a list). */
	protected void translateInfixToPostfix(String str) {

		if (postfix == null) {
			postfix = new ArrayList<String>();
		}
		else {
			postfix.clear();
		}

		Matcher matcher = LOGICAL_EXPRESSION.matcher(str);
		int operandStart = 0;
		String operand, operator;

		while (matcher.find()) {

			operand = str.substring(operandStart, matcher.start());
			if (!operand.equals(""))
				postfix.add(operand);
			operator = matcher.group().trim();
			operandStart = matcher.end();
			if (operator.equalsIgnoreCase("not") || operator.equalsIgnoreCase("or") || operator.equalsIgnoreCase("and")) {
				while (!logicalStack.isEmpty()) {
					String s = (String) logicalStack.pop();
					if (s.equals("(")) {
						logicalStack.push(s);
						break;
					}
					postfix.add(s);
				}
				logicalStack.push(operator);
			}
			else if (operator.equals("(")) {
				logicalStack.push(operator);
			}
			else if (operator.equals(")")) {
				while (!logicalStack.isEmpty()) {
					String s = (String) logicalStack.pop();
					if (s.equals("(")) {
						break;
					}
					postfix.add(s);
				}
			}

		}

		operand = str.substring(operandStart);
		if (!operand.equals(""))
			postfix.add(operand);
		while (!logicalStack.isEmpty()) {
			String s = (String) logicalStack.pop();
			if (s.equals("(") || s.equals(")")) {
				out(ScriptEvent.FAILED, "Script Error: Unbalanced parenthesis: " + str);
			}
			postfix.add(s);
		}

	}

	/** some selection operations by index, commonly applied to collections */
	public static boolean selectFromCollection(String str, int n, BitSet bs) {
		if (str == null || bs == null)
			return false;
		if (NONE.matcher(str).find())
			return true;
		if (ALL.matcher(str).find()) {
			for (int k = 0; k < n; k++)
				bs.set(k);
			return true;
		}
		if (RANGE_LEADING.matcher(str).find()) {
			String[] s = str.split("-");
			int start = Float.valueOf(s[0].trim()).intValue();
			int end = Float.valueOf(s[1].trim()).intValue();
			if (start < n && start >= 0 && end > 0) {
				end = Math.min(end, n - 1);
				for (int k = start; k <= end; k++)
					bs.set(k);
			}
			return true;
		}
		if (INTEGER_GROUP.matcher(str).find()) {
			String[] s = str.split(REGEX_SEPARATOR + "+");
			int index;
			for (int m = 0; m < s.length; m++) {
				index = Float.valueOf(s[m]).intValue();
				if (index < n && index >= 0)
					bs.set(index);
			}
			return true;
		}
		if (INDEX.matcher(str).find()) {
			int index = Float.valueOf(str.trim()).intValue();
			if (index < n && index >= 0)
				bs.set(index);
			return true;
		}
		return false;
	}

	/**
	 * replace the within clauses in a statement with strings with unique characters so that the content in the within
	 * clauses does not interfere with expression parsing.
	 */
	protected String translateWithinClauses(String str) {
		if (withinMap == null) {
			withinMap = new HashMap<String, String>();
		}
		else {
			withinMap.clear();
		}
		Matcher matcher = WITHIN_RADIUS.matcher(str);
		while (matcher.find()) {
			int rparen = matcher.end() + str.substring(matcher.end()).indexOf(")") + 1;
			String withinClause = str.substring(matcher.start(), rparen);
			if (WITHIN_RECTANGLE.matcher(withinClause).find())
				continue;
			withinMap.put("within" + matcher.start(), withinClause);
		}
		str = replaceAllWithinClauses(str);
		matcher = RANGE_WITHIN_RECTANGLE.matcher(str);
		while (matcher.find()) {
			int rparen = matcher.end() + str.substring(matcher.end()).indexOf(")") + 1;
			String withinClause = str.substring(matcher.start(), rparen);
			withinMap.put("within" + matcher.start(), withinClause);
		}
		str = replaceAllWithinClauses(str);
		matcher = INDEX_WITHIN_RECTANGLE.matcher(str);
		while (matcher.find()) {
			int rparen = matcher.end() + str.substring(matcher.end()).indexOf(")") + 1;
			String withinClause = str.substring(matcher.start(), rparen);
			withinMap.put("within" + matcher.start(), withinClause);
		}
		str = replaceAllWithinClauses(str);
		matcher = WITHIN_RECTANGLE.matcher(str);
		while (matcher.find()) {
			int rparen = matcher.end() + str.substring(matcher.end()).indexOf(")") + 1;
			String withinClause = str.substring(matcher.start(), rparen);
			withinMap.put("within" + matcher.start(), withinClause);
		}
		return replaceAllWithinClauses(str);
	}

	private String replaceAllWithinClauses(String str) {
		if (withinMap.isEmpty())
			return str;
		String withinClause = null;
		for (String withinReplacement : withinMap.keySet()) {
			withinClause = withinMap.get(withinReplacement);
			withinClause = escapeParenthesis(withinClause);
			str = str.replaceAll(withinClause, withinReplacement);
		}
		return str;
	}

	/** check if the parentheses contained in the string is balanced. */
	protected boolean checkParenthesisBalance(String str) {
		int nlp = 0;
		int nrp = 0;
		for (int i = 0; i < str.length(); i++) {
			switch (str.charAt(i)) {
			case '(':
				nlp++;
				break;
			case ')':
				nrp++;
				break;
			}
		}
		if (nlp != nrp) {
			out(ScriptEvent.FAILED, "Unbalanced parenthesis: " + str);
			return false;
		}
		return true;
	}

	public static String escapeParenthesis(String s) {
		int lp = s.indexOf("(");
		int rp = s.indexOf(")");
		return s.substring(0, lp) + "\\(" + s.substring(lp + 1, rp) + "\\)" + s.substring(rp + 1);
	}

	protected abstract void out(byte status, String description);

	protected static String escapeMetaCharacters(String s) {
		if (s == null)
			return null;
		s = s.replaceAll("\\(", "\\\\(");
		s = s.replaceAll("\\)", "\\\\)");
		s = s.replaceAll("\\[", "\\\\[");
		s = s.replaceAll("\\]", "\\\\]");
		s = s.replaceAll("\\{", "\\\\{");
		s = s.replaceAll("\\}", "\\\\}");
		s = s.replaceAll("\\*", "\\\\*");
		s = s.replaceAll("\\+", "\\\\+");
		s = s.replaceAll("\\^", "\\\\^");
		s = s.replaceAll("\\$", "\\\\$");
		s = s.replaceAll("\\|", "\\\\|");
		s = s.replaceAll("\\?", "\\\\?");
		s = s.replaceAll("\\.", "\\\\.");
		return s;
	}

	protected static String replaceAll(String expression, String variable, double value) {
		return expression.replaceAll("(?i)" + variable, "" + value);
	}

	protected static String replaceAll(String expression, String variable, float value) {
		return expression.replaceAll("(?i)" + variable, "" + value);
	}

	protected static String replaceAll(String expression, String variable, int value) {
		return expression.replaceAll("(?i)" + variable, "" + value);
	}

	private short getNLoop(String ci) {
		int i = ci.toLowerCase().lastIndexOf("loop") + 4;
		if (i < ci.length() - 1) {
			String s = ci.substring(i).trim();
			if (s != null && !s.equals("")) {
				if (s.matches(REGEX_NONNEGATIVE_DECIMAL)) {
					return Double.valueOf(s).shortValue();
				}
				s = replaceVariablesWithValues(useDefinitions(s));
				double x = parseMathExpression(s);
				if (Double.isNaN(x))
					return -1;
				return (short) x;
			}
		}
		return (short) 10;
	}

	protected void showMessageDialog(String message, String basePath, Component parent) {
		HTMLPane h = new HTMLPane("text/html", message);
		h.setEditable(false);
		if (basePath != null) {
			try {
				h.setBase(FileUtilities.isRemote(basePath) ? new URL(basePath) : new File(basePath).toURI().toURL());
			}
			catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(parent), new JScrollPane(h));
	}

	/** read text from the specified address that must be a valid absolute path. */
	protected synchronized String readText(String address, final Component parent) throws InterruptedException {
		if (address == null || address.equals("") || FileUtilities.isRelative(address)) {
			out(ScriptEvent.FAILED, "Need a valid address to import the text source.");
			return null;
		}
		InputStream is = null;
		if (FileUtilities.isRemote(address)) {
			URL url = null;
			try {
				url = new URL(address);
			}
			catch (MalformedURLException e) {
				e.printStackTrace();
			}
			if (url != null) {
				File file = null;
				if (ConnectionManager.sharedInstance().isCachingAllowed()) {
					ConnectionManager.sharedInstance().setCheckUpdate(true);
					try {
						file = ConnectionManager.sharedInstance().shouldUpdate(url);
						if (file == null)
							file = ConnectionManager.sharedInstance().cache(url);
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (file == null) {
					try {
						is = url.openStream();
					}
					catch (IOException ioe) {
						ioe.printStackTrace();
					}
				}
				else {
					try {
						is = new FileInputStream(file);
					}
					catch (IOException ioe) {
						ioe.printStackTrace();
					}
				}
			}
		}
		else {
			try {
				is = new FileInputStream(new File(address));
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		if (is == null) {
			out(ScriptEvent.FAILED, "Text source not found: " + address);
			final String errorAddress = address;
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(parent), "File " + errorAddress
							+ " was not found.", "File not found", JOptionPane.ERROR_MESSAGE);
				}
			});
			return null;
		}
		StringBuffer buffer = new StringBuffer();
		byte[] b = new byte[1024];
		int n = -1;
		try {
			while ((n = is.read(b)) != -1) {
				buffer.append(new String(b, 0, n));
			}
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
		finally {
			try {
				is.close();
			}
			catch (IOException e) {
			}
		}
		try {
			Thread.sleep(100);
		}
		catch (InterruptedException e) {
			throw new InterruptedException();
		}
		return buffer.toString();
	}

	protected void playSound(String address) {
		stopSound();
		if (address == null)
			return;
		File f = null;
		if (FileUtilities.isRemote(address)) {
			URL u = null;
			try {
				u = new URL(address);
			}
			catch (MalformedURLException e) {
				e.printStackTrace();
				return;
			}
			ConnectionManager.sharedInstance().setCheckUpdate(true);
			try {
				f = ConnectionManager.sharedInstance().shouldUpdate(u);
				if (f == null)
					f = ConnectionManager.sharedInstance().cache(u);
			}
			catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
		else {
			f = new File(address);
		}
		try {
			if (address.toLowerCase().endsWith(".mid")) {
				if (midiPlayer == null)
					midiPlayer = new MidiPlayer();
				midiPlayer.setLoopCount(0);
				midiPlayer.play(f);
			}
			else {
				if (sampledAudioPlayer == null)
					sampledAudioPlayer = new SampledAudioPlayer();
				sampledAudioPlayer.setLoopCount(0);
				sampledAudioPlayer.play(f);
			}
		}
		catch (Throwable t) { // in case there is a codec error
			t.printStackTrace();
		}
	}

	private void stopSound() {
		try {
			if (midiPlayer != null)
				midiPlayer.stop();
			if (sampledAudioPlayer != null)
				sampledAudioPlayer.stop();
		}
		catch (Throwable t) { // in case the players have unexpected errors
			t.printStackTrace();
		}
	}

	protected Cursor loadCursor(String address, int x, int y) {
		if (address == null)
			return null;
		File f = null;
		if (FileUtilities.isRemote(address)) {
			URL u = null;
			try {
				u = new URL(address);
			}
			catch (MalformedURLException e) {
				e.printStackTrace();
				return null;
			}
			ConnectionManager.sharedInstance().setCheckUpdate(true);
			try {
				f = ConnectionManager.sharedInstance().shouldUpdate(u);
				if (f == null)
					f = ConnectionManager.sharedInstance().cache(u);
			}
			catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		else {
			f = new File(address);
		}
		if (f == null || !f.exists()) {
			return null;
		}
		URL u = null;
		try {
			u = f.toURI().toURL();
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
		return ModelerUtilities.createCursor(u, new Point(x, y), FileUtilities.getFileName(address));
	}

	protected static void fillCursorIDMap() {
		if (cursorIDMap == null) {
			cursorIDMap = new HashMap<String, Integer>();
			try {
				for (Field f : Cursor.class.getFields()) {
					cursorIDMap.put(f.getName(), f.getInt(null));
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	protected boolean evaluateIncrementOperator(String ci) {
		String s = ci.replaceFirst("(\\+\\+)|(\\-\\-)", "").trim().toLowerCase();
		String t = definition.get(s);
		if (t == null) {
			out(ScriptEvent.FAILED, "Undefined variable: " + s);
			return false;
		}
		double x = 0;
		try {
			x = Double.parseDouble(t);
		}
		catch (NumberFormatException e) {
			e.printStackTrace();
			out(ScriptEvent.FAILED, "Data type not numeric: " + s);
		}
		if (ci.indexOf("++") != -1) {
			x++;
		}
		else {
			x--;
		}
		definition.put(s, x + "");
		return true;
	}

}