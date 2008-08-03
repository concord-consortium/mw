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

package org.concord.mw3d.models;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.concord.modeler.ConnectionManager;
import org.concord.modeler.draw.FillMode;
import org.concord.modeler.event.ScriptEvent;
import org.concord.modeler.event.ScriptExecutionEvent;
import org.concord.modeler.event.ScriptExecutionListener;
import org.concord.modeler.process.AbstractLoadable;
import org.concord.modeler.script.AbstractEval;
import org.concord.modeler.script.Compiler;
import org.concord.modeler.text.XMLCharacterDecoder;
import org.concord.modeler.util.DataQueue;
import org.concord.modeler.util.DataQueueUtilities;
import org.concord.modeler.util.EvaluationException;
import org.concord.modeler.util.FileUtilities;
import org.concord.modeler.util.FloatQueue;
import org.concord.mw3d.MolecularView;
import org.concord.mw3d.UserAction;

import static java.util.regex.Pattern.compile;
import static org.concord.modeler.script.Compiler.*;

class Eval3D extends AbstractEval {

	private final static byte BY_ATOM = 11;
	private final static byte BY_ELEMENT = 12;
	private final static byte BY_RBOND = 13;
	private final static byte BY_ABOND = 14;
	private final static byte BY_TBOND = 15;
	private final static byte BY_MOLECULE = 16;

	// deprecated, replaced by set camera [index]
	private final static Pattern CAMERA = compile("(^(?i)camera\\b){1}");

	private final static float V_CONVERTER = 100000;
	private final static float IV_CONVERTER = 1.0f / V_CONVERTER;

	private static Map<String, Byte> actionIDMap;

	private MolecularModel model;
	private MolecularView view;
	private Atom[] atom;
	private List<ScriptExecutionListener> executionListeners;

	public Eval3D(MolecularModel model) {
		super();
		this.model = model;
		atom = model.atom;
		view = model.getView();
	}

	void addExecutionListener(ScriptExecutionListener l) {
		if (executionListeners == null)
			executionListeners = new ArrayList<ScriptExecutionListener>();
		if (!executionListeners.contains(l))
			executionListeners.add(l);
	}

	void removeExecutionListener(ScriptExecutionListener l) {
		if (executionListeners == null)
			return;
		executionListeners.remove(l);
	}

	private void notifyExecution(String description) {
		ScriptExecutionEvent e = new ScriptExecutionEvent(this, description);
		for (ScriptExecutionListener x : executionListeners)
			x.scriptExecuted(e);
	}

	protected Object getModel() {
		return model;
	}

	protected synchronized void out(byte status, String description) {
		if (status == ScriptEvent.FAILED) {
			stop();
			notifyScriptListener(new ScriptEvent(model, status, "Aborted: " + description));
		}
		else {
			notifyScriptListener(new ScriptEvent(model, status, description));
		}
	}

	private String useSystemVariables(String s) {
		s = replaceAll(s, "%number_of_atoms", model.getAtomCount());
		s = replaceAll(s, "%length", model.getLength());
		s = replaceAll(s, "%width", model.getWidth());
		s = replaceAll(s, "%height", model.getHeight());
		s = replaceAll(s, "%loop_count", iLoop);
		s = replaceAll(s, "%loop_times", nLoop);
		return s;
	}

	private String useElementVariables(String s) {
		int lb = s.indexOf("%element[");
		int rb = s.indexOf("].", lb);
		String v;
		int i;
		while (lb != -1 && rb != -1) {
			v = s.substring(lb + 9, rb);
			double x = parseMathExpression(v);
			if (Double.isNaN(x))
				break;
			i = (int) Math.round(x);
			if (i < 0 || i >= 4) {
				out(ScriptEvent.FAILED, i + " is an invalid index: must be between 0 and 3 (inclusive).");
				break;
			}
			v = escapeMetaCharacters(v);
			// s = s.replaceAll("(?i)%element\\[" + v + "\\]\\.mass", "" + model.getElement(i).getMass());
			// s = s.replaceAll("(?i)%element\\[" + v + "\\]\\.sigma", "" + model.getElement(i).getSigma());
			// s = s.replaceAll("(?i)%element\\[" + v + "\\]\\.epsilon", "" + model.getElement(i).getEpsilon());
			lb = s.indexOf("%element[");
			rb = s.indexOf("].", lb);
		}
		return s;
	}

	private String useAtomVariables(String s, int frame) {
		if (frame >= model.getTapePointer()) {
			out(ScriptEvent.FAILED, "There is no such frame: " + frame + ". (Total frames: " + model.getTapePointer()
					+ ".)");
			return null;
		}
		int n = model.getAtomCount();
		int lb = s.indexOf("%atom[");
		int rb = s.indexOf("].", lb);
		int i;
		String v;
		while (lb != -1 && rb != -1) {
			v = s.substring(lb + 6, rb);
			double x = parseMathExpression(v);
			if (Double.isNaN(x))
				break;
			i = (int) Math.round(x);
			if (i < 0 || i >= n) {
				out(ScriptEvent.FAILED, i + " is an invalid index: must be between 0 and " + (n - 1) + " (inclusive).");
				break;
			}
			v = escapeMetaCharacters(v);
			s = replaceAll(s, "%atom\\[" + v + "\\]\\.mass", atom[i].mass);
			if (frame < 0) {
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.rx", atom[i].rx);
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.ry", atom[i].ry);
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.vx", atom[i].vx);
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.vy", atom[i].vy);
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.ax", atom[i].ax);
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.ay", atom[i].ay);
			}
			else {
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.rx", atom[i].rQ.getQueue1().getData(frame));
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.ry", atom[i].rQ.getQueue2().getData(frame));
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.vx", atom[i].vQ.getQueue1().getData(frame));
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.vy", atom[i].vQ.getQueue2().getData(frame));
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.ax", atom[i].aQ.getQueue1().getData(frame));
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.ay", atom[i].aQ.getQueue2().getData(frame));
			}
			lb = s.indexOf("%atom[");
			rb = s.indexOf("].", lb);
		}
		return s;
	}

	// TODO
	private String useObstacleVariables(String s, int frame) {
		return s;
	}

	private String useMoleculeVariables(String s, int frame) {
		if (model.molecules == null)
			return s;
		int n = model.molecules.size();
		if (n <= 0)
			return s;
		int lb = s.indexOf("%molecule[");
		int rb = s.indexOf("].", lb);
		String v;
		int i;
		Molecule mol;
		while (lb != -1 && rb != -1) {
			v = s.substring(lb + 10, rb);
			double x = parseMathExpression(v);
			if (Double.isNaN(x))
				break;
			i = (int) Math.round(x);
			if (i < 0 || i >= n) {
				out(ScriptEvent.FAILED, "Molecule " + i + " does not exist.");
				break;
			}
			v = escapeMetaCharacters(v);
			mol = model.molecules.get(i);
			int nmol = mol.getAtomCount();
			if (frame < 0) {
				Point3f com = mol.getCenterOfMass();
				s = replaceAll(s, "%molecule\\[" + v + "\\]\\.x", com.x);
				s = replaceAll(s, "%molecule\\[" + v + "\\]\\.y", com.y);
				s = replaceAll(s, "%molecule\\[" + v + "\\]\\.z", com.z);
			}
			else {
				float xc = 0, yc = 0, zc = 0;
				Atom at = null;
				for (int k = 0; k < nmol; k++) {
					at = mol.getAtom(k);
					xc += at.rQ.getQueue1().getData(frame);
					yc += at.rQ.getQueue2().getData(frame);
					zc += at.rQ.getQueue3().getData(frame);
				}
				xc /= nmol;
				yc /= nmol;
				zc /= nmol;
				s = replaceAll(s, "%molecule\\[" + v + "\\]\\.x", xc);
				s = replaceAll(s, "%molecule\\[" + v + "\\]\\.y", yc);
				s = replaceAll(s, "%molecule\\[" + v + "\\]\\.z", zc);
			}
			s = replaceAll(s, "%molecule\\[" + v + "\\]\\.n", nmol);
			lb = s.indexOf("%molecule[");
			rb = s.indexOf("].", lb);
		}
		return s;
	}

	protected String useDefinitions(String s) {
		s = useSystemVariables(s);
		s = useElementVariables(s);
		s = useAtomVariables(s, -1);
		s = useObstacleVariables(s, -1);
		s = useMoleculeVariables(s, -1);
		s = super.useDefinitions(s);
		return s;
	}

	void evaluate() throws InterruptedException {
		while (true) {
			evaluate2();
			synchronized (this) {
				wait();
			}
		}
	}

	/*
	 * the thread that calls this method goes to "wait" until it is notified. When it returns, it will evaluate the
	 * script. If you want to do a different script, pass in through the script setter. The "stop" flag indicates the
	 * end of executing the current script.
	 */
	private void evaluate2() throws InterruptedException {
		stop = false;
		interrupted = false;
		if (script == null) {
			out(ScriptEvent.FAILED, "No script.");
			return;
		}
		script = script.trim();
		if (script.equals("")) {
			out(ScriptEvent.FAILED, "No script.");
			return;
		}
		script = removeCommentedOutScripts(script);
		script = separateExternalScripts(script);
		String[] command = COMMAND_BREAK.split(script);
		if (command.length < 1) {
			out(ScriptEvent.FAILED, "No script.");
			return;
		}
		evalDefinitions(command);
		evalCommandSet(command);
		String s = null;
		try {
			s = scriptQueue.removeFirst();
		}
		catch (Exception e) {
			s = null;
		}
		if (s != null) {
			setScript(s);
			evaluate2();
		}
		else {
			stop();
		}
	}

	protected boolean evalCommand(String ci) throws InterruptedException {

		String ciLC = ci.toLowerCase();

		// skip the following commands
		if (ciLC.startsWith("define ") || ciLC.startsWith("static ") || ciLC.startsWith("cancel"))
			return true;

		logicalStack.clear();

		if (!checkParenthesisBalance(ci))
			return false;

		// plot
		Matcher matcher = PLOT.matcher(ci);
		if (matcher.find()) {
			if (evaluatePlotClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		matcher = SET_VAR.matcher(ci);
		if (!matcher.find()) {
			try {
				ci = replaceVariablesWithValues(useDefinitions(ci));
			}
			catch (EvaluationException ex) {
				ex.printStackTrace(System.err);
				return false;
			}
		}

		// select
		matcher = SELECT.matcher(ci);
		if (matcher.find()) {
			if (evaluateSelectClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// mouse cursor
		matcher = CURSOR.matcher(ci);
		if (matcher.find()) {
			if (evaluateCursorClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// background
		matcher = BACKGROUND.matcher(ci);
		if (matcher.find()) {
			if (evaluateBackgroundClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// charge
		matcher = CHARGE.matcher(ci);
		if (matcher.find()) {
			if (evaluateChargeClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// damp
		matcher = DAMP.matcher(ci);
		if (matcher.find()) {
			if (evaluateDampClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// heat
		matcher = HEAT.matcher(ci);
		if (matcher.find()) {
			if (evaluateHeatClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// trajectory
		matcher = TRAJECTORY.matcher(ci);
		if (matcher.find()) {
			if (evaluateTrajectoryClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// camera
		matcher = CAMERA.matcher(ci);
		if (matcher.find()) {
			if (evaluateCameraClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// show
		matcher = SHOW.matcher(ci);
		if (matcher.find()) {
			if (evaluateShowClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// set
		matcher = SET.matcher(ci);
		if (matcher.find()) {
			if (evaluateSetClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// add
		matcher = ADD.matcher(ci);
		if (matcher.find()) {
			if (evaluateAddClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// move
		matcher = MOVE.matcher(ci);
		if (matcher.find()) {
			if (evaluateMoveClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// delay
		matcher = DELAY.matcher(ci);
		if (matcher.find()) {
			if (evaluateDelayClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// load
		matcher = LOAD.matcher(ci);
		if (matcher.find()) {
			if (evaluateLoadClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// script/source
		matcher = SOURCE.matcher(ci);
		if (matcher.find()) {
			if (evaluateSourceClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// print
		matcher = PRINT.matcher(ci);
		if (matcher.find()) {
			if (evaluatePrintClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// minimize
		matcher = MINIMIZE.matcher(ci);
		if (matcher.find()) {
			if (evaluateMinimizeClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// build radial bond
		matcher = BUILD_BOND.matcher(ci);
		if (matcher.find()) {
			if (evaluateBuildRBondClause(ci.substring(ci.startsWith("rbond") ? 5 : 4).trim()))
				return true;
		}

		// build angular bond
		matcher = BUILD_BEND.matcher(ci);
		if (matcher.find()) {
			if (evaluateBuildABondClause(ci.substring(ci.startsWith("abond") ? 5 : 4).trim()))
				return true;
		}

		// show message
		matcher = MESSAGE.matcher(ci);
		if (matcher.find()) {
			String s = XMLCharacterDecoder.decode(ci.substring(matcher.end()).trim());
			String slc = s.toLowerCase();
			int a = slc.indexOf("<t>");
			int b = slc.indexOf("</t>");
			String info;
			if (a != -1 && b != -1) {
				info = s.substring(a, b + 4).trim();
				slc = info.toLowerCase();
				if (!slc.startsWith("<html>")) {
					info = "<html>" + info;
				}
				if (!slc.endsWith("</html>")) {
					info = info + "</html>";
				}
			}
			else {
				matcher = Compiler.HTML_EXTENSION.matcher(s);
				if (matcher.find()) {
					info = readText(s, view);
				}
				else {
					info = "Unknown text";
				}
			}
			final String info2 = format(info);
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					showMessageDialog(info2, view.getCodeBase(), view);
				}
			});
			return true;
		}

		out(ScriptEvent.FAILED, "Unrecognized command: " + ci);
		return false;

	}

	protected boolean evaluateSingleKeyword(String str) throws InterruptedException {
		if (super.evaluateSingleKeyword(str))
			return true;
		if ("paint".equalsIgnoreCase(str)) { // paint
			view.paintImmediately(0, 0, view.getWidth(), view.getHeight());
			return true;
		}
		if ("snapshot".equalsIgnoreCase(str)) { // snapshot
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					if (view.getSnapshotListener() != null)
						view.getSnapshotListener().actionPerformed(null);
				}
			});
			return true;
		}
		if ("focus".equalsIgnoreCase(str)) { // focus
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					view.requestFocusInWindow();
				}
			});
			return true;
		}
		if ("run".equalsIgnoreCase(str)) { // run
			// FIXME: Why do we need to do this to make "delay modeltime" to work with a prior "run" command?
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e) {
			}
			model.run();
			model.notifyChange();
			return true;
		}
		if ("stop".equalsIgnoreCase(str)) { // stop
			model.stop();
			return true;
		}
		if ("stop immediately".equalsIgnoreCase(str)) { // stop immediately
			model.stopImmediately();
			return true;
		}
		if ("reset".equalsIgnoreCase(str)) { // reset
			evaluateLoadClause(view.getResourceAddress());
			notifyExecution("reset");
			return true;
		}
		if ("remove".equalsIgnoreCase(str)) { // remove selected objects
			// CAUTION!!!!!!!!! PUTTING INTO EVENTQUEUE is dangerous if there are commands following it!
			removeSelectedObjects();
			// EventQueue.invokeLater(new Runnable() { public void run() { removeSelectedObjects(); } });
			model.notifyChange();
			return true;
		}
		return false;
	}

	private boolean evaluateSelectClause(String clause) {
		Matcher matcher = ATOM.matcher(clause); // select by atom
		if (matcher.find()) {
			BitSet selection = null;
			String str = clause.substring(matcher.end()).trim();
			if (LOGICAL_OPERATOR.matcher(str).find()) { // logical expressions
				selection = parseLogicalExpression(str, BY_ATOM);
			}
			else {
				selection = selectAtoms(str);
			}
			out(ScriptEvent.SUCCEEDED, (selection != null ? selection.cardinality() : 0) + " atoms are selected.");
			return true;
		}
		out(ScriptEvent.FAILED, "Unrecognized keyword in: " + clause);
		return false;
	}

	private boolean evaluateBackgroundClause(String str) {
		if (str.toLowerCase().startsWith("color")) {
			Color c = parseRGBColor(str.substring(5).trim());
			if (c == null)
				return false;
			view.setBackground(c);
			view.setFillMode(new FillMode.ColorFill(c));
		}
		else if (str.toLowerCase().startsWith("image")) {
			String s = str.substring(5).trim();
			Matcher matcher = IMAGE_EXTENSION.matcher(s);
			if (matcher.find()) {
				String address = s.substring(0, matcher.end()).trim();
				if (FileUtilities.isRelative(address)) {
					String base = view.getResourceAddress();
					if (base == null) {
						out(ScriptEvent.FAILED, "No directory has been specified. Save the page first.");
						return false;
					}
					address = FileUtilities.getCodeBase(base) + address;
					if (System.getProperty("os.name").startsWith("Windows"))
						address = address.replace('\\', '/');
				}
				ImageIcon icon = null;
				if (FileUtilities.isRemote(address)) {
					try {
						icon = ConnectionManager.sharedInstance().loadImage(new URL(FileUtilities.httpEncode(address)));
					}
					catch (MalformedURLException e) {
						e.printStackTrace(System.err);
						view.setBackgroundImage(null);
						return false;
					}
				}
				else {
					File file = new File(address);
					if (!file.exists())
						return false;
					icon = new ImageIcon(Toolkit.getDefaultToolkit().createImage(address));
				}
				if (icon != null) {
					icon.setDescription(FileUtilities.getFileName(s));
					view.setBackgroundImage(icon);
				}
			}
		}
		model.notifyChange();
		return true;
	}

	private boolean evaluateChargeClause(String str) {
		float c = 0;
		try {
			c = Float.valueOf(str).floatValue();
		}
		catch (NumberFormatException e) {
			out(ScriptEvent.FAILED, str + " cannot be parsed as a number.");
			return false;
		}
		int nop = model.getAtomCount();
		if (nop <= 0)
			return true;
		for (int k = 0; k < nop; k++) {
			if (atom[k].isSelected())
				atom[k].setCharge(c);
		}
		view.repaint();
		model.notifyChange();
		return true;
	}

	private boolean evaluateDampClause(String str) {
		double x = parseMathExpression(str);
		if (x < 0) {
			out(ScriptEvent.FAILED, "Friction cannot be negative: " + str);
			return false;
		}
		int n = model.getAtomCount();
		if (n <= 0)
			return true;
		float c = (float) x;
		for (int k = 0; k < n; k++) {
			Atom a = model.getAtom(k);
			if (a.isSelected())
				a.setDamp(c);
		}
		view.repaint();
		model.notifyChange();
		return true;
	}

	private boolean evaluateHeatClause(String str) {
		float c = 0;
		try {
			c = Float.valueOf(str).floatValue();
		}
		catch (NumberFormatException e) {
			out(ScriptEvent.FAILED, str + " cannot be parsed as a number.");
			return false;
		}
		if (c == 0)
			return true;
		int nop = model.getAtomCount();
		if (nop <= 0)
			return true;
		List<Atom> list = new ArrayList<Atom>();
		for (int k = 0; k < nop; k++) {
			if (atom[k].isSelected())
				list.add(atom[k]);
		}
		// model.heatAtoms(list, c);
		view.repaint();
		model.notifyChange();
		return true;
	}

	private boolean evaluateTrajectoryClause(String str) {
		if (str == null || str.equals(""))
			return false;
		boolean on = str.equalsIgnoreCase("on");
		int n = model.getAtomCount();
		if (n <= 0)
			return true;
		for (int k = 0; k < n; k++) {
			Atom a = model.getAtom(k);
			if (a.isSelected()) {
				model.view.showTrajectory(k, on);
			}
		}
		view.repaint();
		model.notifyChange();
		return true;
	}

	private boolean evaluateCameraClause(String str) {
		if (str == null || str.equals(""))
			return false;
		double x = parseMathExpression(str);
		if (Double.isNaN(x))
			return false;
		view.setCameraAtom((int) Math.round(x));
		view.repaint();
		model.notifyChange();
		return true;
	}

	private boolean evaluateCursorClause(String s) {
		if (s == null)
			return false;
		if ("null".equalsIgnoreCase(s)) {
			view.setExternalCursor(null);
			return true;
		}
		if (s.endsWith("_CURSOR")) {
			fillCursorIDMap();
			int id = cursorIDMap.get(s);
			Cursor c = Cursor.getPredefinedCursor(id);
			if (c == null)
				return false;
			view.setExternalCursor(c);
			return true;
		}
		int lp = s.indexOf("(");
		int rp = s.indexOf(")");
		float[] hotspot = null;
		if (lp != -1 && rp != -1) {
			hotspot = parseArray(2, s.substring(lp, rp));
		}
		else {
			out(ScriptEvent.FAILED, "Cursor's hot spot coordinate error: " + s);
			return false;
		}
		s = s.substring(0, lp).trim();
		if (FileUtilities.isRelative(s)) {
			String address = view.getResourceAddress();
			if (address == null) {
				out(ScriptEvent.FAILED, "Codebase missing.");
				return false;
			}
			s = FileUtilities.getCodeBase(address) + s;
		}
		Cursor c = loadCursor(s, hotspot != null ? (int) hotspot[0] : 0, hotspot != null ? (int) hotspot[1] : 0);
		if (c == null) {
			out(ScriptEvent.FAILED, "Failed in loading cursor image: " + s);
			return false;
		}
		view.setExternalCursor(c);
		return true;
	}

	// TODO
	private boolean evaluateBuildRBondClause(String str) {
		String[] s = str.split(REGEX_SEPARATOR);
		if (s.length != 3)
			return false;
		int n = model.getAtomCount();
		int i = Math.round(Float.parseFloat(s[0]));
		if (i >= n) {
			out(ScriptEvent.FAILED, "Atom index out of limit: i=" + i + ">=" + n);
			return false;
		}
		int j = Math.round(Float.parseFloat(s[1]));
		if (j >= n) {
			out(ScriptEvent.FAILED, "Atom index out of limit: j=" + j + ">=" + n);
			return false;
		}
		if (j == i) {
			out(ScriptEvent.FAILED, "Cannot build a bond between a pair of identical atoms: i=j=" + i);
			return false;
		}
		// float k = Float.parseFloat(s[2]);
		// Atom at1 = atom[i];
		// Atom at2 = atom[j];
		model.notifyChange();
		return true;
	}

	// TODO
	private boolean evaluateBuildABondClause(String str) {
		String[] s = str.split(REGEX_SEPARATOR);
		if (s.length != 4)
			return false;
		int n = model.getAtomCount();
		int i = Math.round(Float.parseFloat(s[0]));
		if (i >= n) {
			out(ScriptEvent.FAILED, "Atom index out of limit: i=" + i + ">=" + n);
			return false;
		}
		int j = Math.round(Float.parseFloat(s[1]));
		if (j >= n) {
			out(ScriptEvent.FAILED, "Atom index out of limit: j=" + j + ">=" + n);
			return false;
		}
		if (j == i) {
			out(ScriptEvent.FAILED, "Cannot build an angular bond for identical atoms: i=j=" + i);
			return false;
		}
		int k = Math.round(Float.parseFloat(s[2]));
		if (k >= n) {
			out(ScriptEvent.FAILED, "Atom index out of limit: k=" + k + ">=" + n);
			return false;
		}
		if (k == i || k == j) {
			out(ScriptEvent.FAILED, "Cannot build an angular bond for identical atoms: " + i + "," + j + "," + k);
			return false;
		}
		// float p = Float.parseFloat(s[3]);
		// Atom at1 = atom[i];
		// Atom at2 = atom[j];
		// Atom at3 = atom[k];
		view.repaint();
		model.notifyChange();
		return true;
	}

	private boolean evaluateShowClause(String str) {
		String s = str.toLowerCase();
		byte result = parseOnOff("charge", s);
		if (result != -1) {
			if (result != 0) {
				view.setShowCharge(result == ON);
			}
			return result != 0;
		}
		result = parseOnOff("index", s);
		if (result != -1) {
			view.setShowAtomIndex(result == ON);
			return true;
		}
		result = parseOnOff("clock", s);
		if (result != -1) {
			view.setShowClock(result == ON);
			return true;
		}
		result = parseOnOff("vdwline", s);
		if (result != -1) {
			view.setShowVdwLines(result == ON);
			return true;
		}
		result = parseOnOff("keshading", s);
		if (result != -1) {
			view.setKeShading(result == ON);
			return true;
		}
		out(ScriptEvent.FAILED, "Unrecognized keyword: " + str);
		return false;
	}

	private boolean evaluateSetClause(String str) {

		// action
		Matcher matcher = ACTION.matcher(str);
		if (matcher.find()) {
			str = str.substring(matcher.end()).trim().toUpperCase();
			matcher = ACTION_ID.matcher(str);
			if (matcher.find()) {
				if (actionIDMap == null)
					fillActionIDMap();
				if (!actionIDMap.containsKey(str)) {
					out(ScriptEvent.FAILED, "Unrecognized parameter: " + str);
					return false;
				}
				view.setActionID(actionIDMap.get(str));
				return true;
			}
		}

		String[] s = str.trim().split(REGEX_SEPARATOR + "+");

		if (s.length == 2) {

			String s0 = s[0].trim().toLowerCase().intern();
			if ("on".equalsIgnoreCase(s[1].trim()) || "off".equalsIgnoreCase(s[1].trim())) {
				if (s0 == "navigation") {
					boolean b = "on".equalsIgnoreCase(s[1].trim());
					view.getViewer().setNavigationMode(b);
					view.repaint();
					model.notifyChange();
					return true;
				}
				else if (s0 == "movable") {
					boolean b = "on".equalsIgnoreCase(s[1].trim());
					int n = model.getAtomCount();
					for (int i = 0; i < n; i++) {
						Atom a = model.getAtom(i);
						if (a.isSelected())
							a.setMovable(b);
					}
					model.notifyChange();
					return true;
				}
				else if (s0 == "visible") {
					boolean b = "on".equalsIgnoreCase(s[1].trim());
					int n = model.getAtomCount();
					for (int i = 0; i < n; i++) {
						Atom a = model.getAtom(i);
						if (a.isSelected())
							a.setVisible(b);
					}
					model.notifyChange();
					return true;
				}
			}
			double x = parseMathExpression(s[1]);
			if (Double.isNaN(x))
				return false;
			if (s0 == "camera") {
				view.setCameraAtom((int) Math.round(x));
				view.repaint();
				model.notifyChange();
				return true;
			}
			else if (s0 == "gfield") {
				if (x < 0) {
					out(ScriptEvent.FAILED, "Illegal parameter: gravitational acceleration cannot be negative: " + x);
					return false;
				}
				model.setGField((float) x, null);
				model.setRotationMatrix(view.getViewer().getRotationMatrix());
				return true;
			}
			else if (s0 == "vx") {
				int n = model.getAtomCount();
				for (int i = 0; i < n; i++) {
					Atom a = model.getAtom(i);
					if (a.isSelected())
						a.setVx((float) x * IV_CONVERTER);
				}
				model.notifyChange();
				return true;
			}
			else if (s0 == "vy") {
				int n = model.getAtomCount();
				for (int i = 0; i < n; i++) {
					Atom a = model.getAtom(i);
					if (a.isSelected())
						a.setVy((float) x * IV_CONVERTER);
				}
				model.notifyChange();
				return true;
			}
			else if (s0 == "vz") {
				int n = model.getAtomCount();
				for (int i = 0; i < n; i++) {
					Atom a = model.getAtom(i);
					if (a.isSelected())
						a.setVz((float) x * IV_CONVERTER);
				}
				model.notifyChange();
				return true;
			}
			else if (s0 == "charge") {
				int n = model.getAtomCount();
				for (int i = 0; i < n; i++) {
					Atom a = model.getAtom(i);
					if (a.isSelected())
						a.setCharge((float) x);
				}
				model.notifyChange();
				return true;
			}
		}

		else if (s.length == 4) {

			String s0 = s[0].trim().toLowerCase().intern();

			if (s0 == "gfield") {
				double x = parseMathExpression(s[1]);
				if (Double.isNaN(x))
					return false;
				double y = parseMathExpression(s[2]);
				if (Double.isNaN(y))
					return false;
				double z = parseMathExpression(s[3]);
				if (Double.isNaN(z))
					return false;
				float r = (float) Math.sqrt(x * x + y * y + z * z);
				if (r < ZERO) {
					model.setGField(0, null);
				}
				else {
					model.setGField(r, new Vector3f((float) x / r, (float) y / r, (float) z / r));
				}
				return true;
			}

			if (s0 == "efield") {
				double x = parseMathExpression(s[1]);
				if (Double.isNaN(x))
					return false;
				double y = parseMathExpression(s[2]);
				if (Double.isNaN(y))
					return false;
				double z = parseMathExpression(s[3]);
				if (Double.isNaN(z))
					return false;
				float r = (float) Math.sqrt(x * x + y * y + z * z);
				if (r < ZERO) {
					model.setEField(0, null);
				}
				else {
					model.setEField(r, new Vector3f((float) x / r, (float) y / r, (float) z / r));
				}
				return true;
			}

			if (s0 == "bfield") {
				double x = parseMathExpression(s[1]);
				if (Double.isNaN(x))
					return false;
				double y = parseMathExpression(s[2]);
				if (Double.isNaN(y))
					return false;
				double z = parseMathExpression(s[3]);
				if (Double.isNaN(z))
					return false;
				float r = (float) Math.sqrt(x * x + y * y + z * z);
				if (r < ZERO) {
					model.setBField(0, null);
				}
				else {
					model.setBField(r, new Vector3f((float) x / r, (float) y / r, (float) z / r));
				}
				return true;
			}

		}

		out(ScriptEvent.FAILED, "Unrecognized type of parameter to set: " + str);
		return false;

	}

	private boolean evaluateAddClause(String str) {
		out(ScriptEvent.FAILED, "Unrecognized type of object to add: " + str);
		return false;
	}

	private boolean evaluateMoveClause(String str) {
		out(ScriptEvent.FAILED, "Unable to parse number pairs: " + str);
		return false;
	}

	private boolean evaluateDelayClause(String str) throws InterruptedException {
		if (str.matches(REGEX_NONNEGATIVE_DECIMAL)) {
			float sec = Float.valueOf(str).floatValue();
			int millis = (int) (sec * 1000);
			while (!stop && millis > 0) {
				try {
					Thread.sleep(millis > DELAY_FRACTION ? DELAY_FRACTION : millis);
					millis -= DELAY_FRACTION;
				}
				catch (InterruptedException e) {
					stop();
					interrupted = true;
					throw new InterruptedException();
				}
			}
			view.repaint();
			return true;
		}
		if (str.toLowerCase().startsWith("modeltime")) {
			str = str.substring(9).trim();
			if (str.matches(REGEX_NONNEGATIVE_DECIMAL)) {
				int i = Math.round(Float.valueOf(str).floatValue() / model.getTimeStep());
				int step0 = model.job != null ? model.job.getIndexOfStep() : 0;
				AbstractLoadable l = new AbstractLoadable(i) {
					public void execute() {
						// if (model.job.getIndexOfStep() - step0 < i - 1) return; // what the hell is this?
						synchronized (Eval3D.this) {
							Eval3D.this.notifyAll();
						}
						setCompleted(true);
					}
				};
				l.setPriority(Thread.NORM_PRIORITY);
				l.setName("Delay " + i + " steps from step " + step0);
				l.setDescription("This task delays the script execution for " + i + " steps.");
				model.job.add(l);
				try {
					synchronized (this) {
						Eval3D.this.wait();
					}
				}
				catch (InterruptedException e) {
					interrupted = true;
					stop();
					throw new InterruptedException();
				}
				return true;
			}
		}
		out(ScriptEvent.FAILED, "Unable to parse number: " + str);
		return false;
	}

	/*
	 * It is important to synchronized this method so that we do not have two loading processes running at the same
	 * time, which causes the corruption of the model's states.
	 */
	private synchronized boolean evaluateLoadClause(String address) throws InterruptedException {
		if (address == null || address.equals("")) {
			out(ScriptEvent.FAILED, "Missing an address to load.");
			return false;
		}
		model.stopImmediately();
		try {
			Thread.sleep(100); // sleep 100 ms in order for the force calculation to finish
		}
		catch (InterruptedException e) {
			throw new InterruptedException();
		}
		if (FileUtilities.isRelative(address)) {
			if (view.getResourceAddress() == null) {
				out(ScriptEvent.FAILED, "Codebase missing.");
				return false;
			}
			address = FileUtilities.getCodeBase(view.getResourceAddress()) + address;
			URL u = ConnectionManager.sharedInstance().getRemoteCopy(address);
			if (u != null)
				address = u.toString();
		}
		if (FileUtilities.isRemote(address)) {
			URL url = null;
			try {
				url = new URL(FileUtilities.httpEncode(address));
			}
			catch (MalformedURLException e) {
				e.printStackTrace(System.err);
			}
			if (url != null) {
				ConnectionManager.sharedInstance().setCheckUpdate(true);
				view.getContainer().input(url);
			}
		}
		else {
			view.getContainer().input(new File(address));
		}
		/* Do we really need to sleep in the following? */
		try {
			Thread.sleep(100);
		}
		catch (InterruptedException e) {
			stop();
			throw new InterruptedException();
		}
		model.notifyChange();
		return true;
	}

	private synchronized boolean evaluateSourceClause(String address) {
		out(ScriptEvent.FAILED, "Syntax error: " + address);
		return false;
	}

	// synchronization prevents two minimizers to run at the same time.
	private synchronized boolean evaluateMinimizeClause(String str) {
		out(ScriptEvent.FAILED, "Syntax error: " + str);
		return false;
	}

	private boolean evaluatePrintClause(String str) {
		if (str == null)
			return false;
		str = format(str);
		out(ScriptEvent.SUCCEEDED, str);
		return true;
	}

	private boolean evaluatePlotClause(String str) {
		if (!model.hasEmbeddedMovie()) {
			out(ScriptEvent.FAILED, "No data is recorded or model not in recording mode.");
			return false;
		}
		byte averageFlag = 0;
		if (str.toLowerCase().startsWith("-ra")) {
			str = str.substring(3).trim();
			averageFlag = 1;
		}
		else if (str.toLowerCase().startsWith("-ca")) {
			str = str.substring(3).trim();
			averageFlag = 2;
		}
		boolean xyFlag = false;
		if (str.startsWith("(") && str.endsWith(")")) {
			str = str.substring(1, str.length() - 1);
			xyFlag = true;
		}
		String[] s = str.split("\"" + REGEX_SEPARATOR + "+\"");
		DataQueue[] q = plotMathExpression(s, averageFlag, xyFlag);
		if (q != null) {
			DataQueueUtilities.show(q, JOptionPane.getFrameForComponent(view));
			return true;
		}
		out(ScriptEvent.FAILED, "Unrecognized keyword: " + str);
		return false;
	}

	private DataQueue[] plotMathExpression(String[] expression, byte averageFlag, boolean xyFlag) {
		int n = model.getTapePointer();
		if (n <= 0)
			return null;
		if (xyFlag) {
			if (expression.length < 2)
				return null;
			FloatQueue x = computeQueue(expression[0], averageFlag);
			FloatQueue[] y = new FloatQueue[expression.length - 1];
			for (int i = 0; i < y.length; i++) {
				y[i] = computeQueue(expression[i + 1], averageFlag);
				y[i].setCoordinateQueue(x);
			}
			return y;
		}
		FloatQueue[] q = new FloatQueue[expression.length];
		for (int i = 0; i < expression.length; i++) {
			q[i] = computeQueue(expression[i], averageFlag);
			if (q[i] != null)
				q[i].setCoordinateQueue(model.getModelTimeQueue());
		}
		return q;
	}

	private FloatQueue computeQueue(String expression, byte averageFlag) {
		FloatQueue q = new FloatQueue(model.getTapeLength());
		if (expression.startsWith("\""))
			expression = expression.substring(1);
		if (expression.endsWith("\""))
			expression = expression.substring(0, expression.length() - 1);
		q.setName(expression);
		String str = useSystemVariables(expression);
		float result = 0;
		float sum = 0;
		String s = null;
		int n = model.getTapePointer();
		for (int k = 0; k < n; k++) {
			s = useAtomVariables(str, k);
			s = useMoleculeVariables(s, k);
			double x = parseMathExpression(s);
			if (Double.isNaN(x))
				return null;
			result = (float) x;
			if (averageFlag == 1) {
				sum = k == 0 ? result : 0.05f * result + 0.95f * sum;
				q.update(sum);
			}
			else if (averageFlag == 2) {
				sum += result;
				q.update(sum / (k + 1));
			}
			else {
				q.update(result);
			}
		}
		return q;
	}

	void setAtomField(String str1, String str2, float x) {
		int lb = str1.indexOf("[");
		int rb = str1.indexOf("]");
		double z = parseMathExpression(str1.substring(lb + 1, rb));
		if (Double.isNaN(z))
			return;
		int i = (int) Math.round(z);
		if (i >= model.getAtomCount()) {
			out(ScriptEvent.FAILED, "Atom " + i + " doesn't exisit.");
			return;
		}
		String s = str2.toLowerCase().intern();
		boolean b = true;
		if (s == "rx")
			atom[i].rx = x;
		else if (s == "ry")
			atom[i].ry = x;
		else if (s == "rz")
			atom[i].rz = x;
		else if (s == "vx")
			atom[i].vx = x;
		else if (s == "vy")
			atom[i].vy = x;
		else if (s == "vz")
			atom[i].vz = x;
		else if (s == "ax")
			atom[i].ax = x;
		else if (s == "ay")
			atom[i].ay = x;
		else if (s == "az")
			atom[i].az = x;
		else if (s == "charge")
			atom[i].charge = x;
		else {
			out(ScriptEvent.FAILED, "Cannot set propery: " + str2);
			b = false;
		}
		if (b)
			model.notifyChange();
	}

	private void removeSelectedObjects() {
		int n = model.getAtomCount();
		List<Integer> list = new ArrayList<Integer>();
		for (int k = 0; k < n; k++) {
			if (atom[k].isSelected())
				list.add(k);
		}
		view.repaint();
	}

	private BitSet genericSelect(String str) {
		boolean found = false;
		int n = model.getAtomCount();
		BitSet bs = new BitSet(n);
		Matcher matcher = ALL.matcher(str);
		if (matcher.find()) {
			for (int k = 0; k < n; k++)
				bs.set(k);
			found = true;
		}
		if (!found) {
			matcher = NONE.matcher(str);
			if (matcher.find()) {
				found = true;
			}
		}
		if (found)
			model.setSelectionSet(bs);
		return found ? bs : null;
	}

	private BitSet selectAtoms(String str) {

		if ("selected".equalsIgnoreCase(str)) {
			return model.getSelectionSet();
		}

		BitSet bs = genericSelect(str);
		if (bs != null)
			return bs;

		boolean found = false;
		int noa = model.getAtomCount();
		bs = new BitSet(noa);

		Matcher matcher = null;
		if (!found) {
			matcher = WITHIN_RADIUS.matcher(str);
			if (matcher.find()) {
				found = true;
				String s = str.substring(matcher.end()).trim();
				s = s.substring(0, s.indexOf(")"));
				if (RANGE.matcher(s).find()) {
					int lp = str.lastIndexOf("(");
					int rp = str.indexOf(")");
					s = str.substring(lp + 1, rp).trim();
					float r = Float.valueOf(s.substring(0, s.indexOf(",")).trim()).floatValue();
					r *= r * 100;
					s = s.substring(s.indexOf(",") + 1);
					int i0 = Math.round(Float.valueOf(s.substring(0, s.indexOf("-")).trim()).floatValue());
					int i1 = Math.round(Float.valueOf(s.substring(s.indexOf("-") + 1).trim()).floatValue());
					if (i0 < noa && i0 >= 0 && i1 < noa && i1 >= 0 && i1 >= i0) {
						Atom c;
						for (int k = i0; k <= i1; k++) {
							bs.set(k);
							c = model.getAtom(k);
							for (int m = 0; m < i0; m++) {
								if (bs.get(m))
									continue;
								if (model.getAtom(m).distanceSquare(c) < r)
									bs.set(m);
							}
							for (int m = i1 + 1; m < noa; m++) {
								if (bs.get(m))
									continue;
								if (model.getAtom(m).distanceSquare(c) < r)
									bs.set(m);
							}
						}
					}
				}
				else {
					int lp = str.indexOf("(");
					int rp = str.indexOf(")");
					s = str.substring(lp + 1, rp).trim();
					float r = Float.valueOf(s.substring(0, s.indexOf(",")).trim()).floatValue();
					r *= r * 100;
					int center = Math.round(Float.valueOf(s.substring(s.indexOf(",") + 1).trim()).floatValue());
					if (center < noa && center >= 0) {
						Atom c = model.getAtom(center);
						for (int k = 0; k < noa; k++) {
							if (k == center) {
								bs.set(k);
							}
							else {
								if (model.getAtom(k).distanceSquare(c) < r)
									bs.set(k);
							}
						}
					}
				}
			}
		}

		if (!found) {
			matcher = RANGE.matcher(str);
			if (matcher.find()) {
				found = true;
				String[] s = str.split("-");
				int start = Math.round(Float.valueOf(s[0].trim()).floatValue());
				int end = Math.round(Float.valueOf(s[1].trim()).floatValue());
				start = start < 0 ? 0 : start;
				end = end < noa ? end : noa - 1;
				for (int k = start; k <= end; k++)
					bs.set(k);
			}
		}

		if (!found) {
			matcher = INTEGER_GROUP.matcher(str);
			if (matcher.find()) {
				found = true;
				int lp = str.lastIndexOf("(");
				int rp = str.indexOf(")");
				if (lp != -1 && rp != -1) {
					str = str.substring(lp + 1, rp).trim();
				}
				else {
					if (lp != -1 || rp != -1) {
						out(ScriptEvent.FAILED, "Unbalanced parenthesis: " + str);
						return null;
					}
				}
				String[] s = str.split(REGEX_SEPARATOR + "+");
				for (int k = 0; k < s.length; k++) {
					int x = -1;
					try {
						x = Math.round(Float.valueOf(s[k].trim()).floatValue());
					}
					catch (NumberFormatException e) {
						out(ScriptEvent.FAILED, s[k] + " cannot be parsed as an integer.");
						return null;
					}
					if (x >= 0 && x < noa)
						bs.set(x);
				}
			}
		}

		if (!found) {
			matcher = INDEX.matcher(str);
			if (matcher.find()) {
				found = true;
				int x = -1;
				try {
					x = Math.round(Float.valueOf(str.trim()).floatValue());
				}
				catch (NumberFormatException e) {
					out(ScriptEvent.FAILED, str + " cannot be parsed as an integer.");
					return null;
				}
				if (x >= 0 && x < noa)
					bs.set(x);
			}
		}

		if (found) {
			model.setSelectionSet(bs);
		}
		else {
			out(ScriptEvent.FAILED, "Unrecognized expression: " + str);
		}

		return found ? bs : null;

	}

	private BitSet selectElements(String str) {

		if ("selected".equalsIgnoreCase(str))
			return model.getSelectionSet();

		BitSet bs = genericSelect(str);
		if (bs != null)
			return bs;

		boolean found = false;
		int noa = model.getAtomCount();
		bs = new BitSet(noa);

		Matcher matcher = RANGE.matcher(str);
		if (matcher.find()) {
			found = true;
			String[] s = str.split("-");
			int start = Integer.valueOf(s[0].trim()).intValue();
			int end = Integer.valueOf(s[1].trim()).intValue();
			for (int k = 0; k < noa; k++) {
				int id = model.atom[k].getElementNumber();
				if (id >= start && id <= end)
					bs.set(k);
			}
		}

		if (!found) {
			matcher = INTEGER_GROUP.matcher(str);
			if (matcher.find()) {
				found = true;
				String[] s = str.split(REGEX_SEPARATOR + "+");
				int index;
				for (int m = 0; m < s.length; m++) {
					index = Integer.valueOf(s[m]).intValue();
					for (int k = 0; k < noa; k++) {
						if (model.atom[k].getElementNumber() == index)
							bs.set(k);
					}
				}
			}
		}

		if (!found) {
			matcher = INDEX.matcher(str);
			if (matcher.find() && str.indexOf("within") == -1) {
				int index = 0;
				found = true;
				try {
					index = Integer.valueOf(str.trim()).intValue();
				}
				catch (NumberFormatException nfe) {
					nfe.printStackTrace();
					out(ScriptEvent.FAILED, "Element index cannot be parsed as an integer: " + str);
					found = false;
				}
				if (found) {
					for (int k = 0; k < noa; k++) {
						if (model.atom[k].getElementNumber() == index)
							bs.set(k);
					}
				}
			}
		}

		if (!found) {
			matcher = WITHIN_RADIUS.matcher(str);
			if (matcher.find()) {
				found = true;
				String s = str.substring(matcher.end()).trim();
				s = s.substring(0, s.indexOf(")"));
				if (RANGE.matcher(s).find()) {
					int lp = str.indexOf("(");
					int rp = str.indexOf(")");
					s = str.substring(lp + 1, rp).trim();
					float r = Float.valueOf(s.substring(0, s.indexOf(",")).trim()).floatValue();
					r *= r * 100;
					s = s.substring(s.indexOf(",") + 1);
					int i0 = Integer.valueOf(s.substring(0, s.indexOf("-")).trim()).intValue();
					int i1 = Integer.valueOf(s.substring(s.indexOf("-") + 1).trim()).intValue();
					if (i1 >= i0) {
						Atom at;
						for (int k = 0; k < noa; k++) {
							at = model.atom[k];
							if (at.getElementNumber() >= i0 && at.getElementNumber() <= i1) {
								bs.set(k);
								for (int m = 0; m < noa; m++) {
									if (m == k || bs.get(m))
										continue;
									if (model.atom[m].distanceSquare(at) < r)
										bs.set(m);
								}
							}
						}
					}
				}
				else {
					int lp = str.indexOf("(");
					int rp = str.indexOf(")");
					s = str.substring(lp + 1, rp).trim();
					float r = Float.valueOf(s.substring(0, s.indexOf(",")).trim()).floatValue();
					r *= r * 100;
					int center = 0;
					try {
						center = Integer.valueOf(s.substring(s.indexOf(",") + 1).trim()).intValue();
					}
					catch (NumberFormatException nfe) {
						out(ScriptEvent.FAILED, str + " is not an integer number.");
						return null;
					}
					Atom at;
					for (int m = 0; m < noa; m++) {
						at = model.atom[m];
						if (at.getElementNumber() == center) {
							bs.set(m);
							for (int k = 0; k < noa; k++) {
								if (k == m || bs.get(k))
									continue;
								if (model.atom[k].distanceSquare(at) < r)
									bs.set(k);
							}
						}
					}
				}
			}
		}

		if (found) {
			model.setSelectionSet(bs);
		}
		else {
			out(ScriptEvent.FAILED, "Unrecognized expression: " + str);
		}

		return found ? bs : null;

	}

	/*
	 * parse the logical expressions, identified by the "or" and "and" keywords, contained in the string.
	 */
	private BitSet parseLogicalExpression(String str, byte type) {

		if (AND_NOT.matcher(str).find() || OR_NOT.matcher(str).find()) {
			out(ScriptEvent.FAILED, "Illegal usage: " + str + ". Add parentheses to the not expression.");
			return null;
		}

		str = str.trim();

		if (str.toLowerCase().indexOf("within") != -1) {
			translateInfixToPostfix(translateWithinClauses(str));
		}
		else {
			translateInfixToPostfix(str);
		}

		// evaluate the postfix expression

		logicalStack.clear(); // make sure that the stack is empty before it is reused.

		int n = postfix.size();
		for (int i = 0; i < n; i++) {
			String s = postfix.get(i).trim();
			if (s.equalsIgnoreCase("not")) {
				BitSet bs = (BitSet) logicalStack.pop();
				bs.flip(0, getNumberOfObjects(type));
				logicalStack.push(bs);
			}
			else if (s.equalsIgnoreCase("or") || s.equalsIgnoreCase("and")) {
				BitSet bs1 = null, bs2 = null;
				try {
					bs2 = (BitSet) logicalStack.pop();
					bs1 = (BitSet) logicalStack.pop();
				}
				catch (EmptyStackException e) {
					e.printStackTrace();
					continue;
				}
				BitSet bs = new BitSet();
				if (s.equalsIgnoreCase("or")) {
					bs.or(bs2);
					bs.or(bs1);
				}
				else if (s.equalsIgnoreCase("and")) {
					bs.or(bs2);
					bs.and(bs1);
				}
				logicalStack.push(bs);
			}
			else {
				BitSet bs = null;
				if (s.toLowerCase().indexOf("within") != -1) {
					boolean startsWithNot = false;
					if (s.toLowerCase().startsWith("not")) {
						startsWithNot = true;
						s = s.substring(3).trim();
					}
					if (withinMap != null && withinMap.containsKey(s)) {
						s = withinMap.get(s);
					}
					if (startsWithNot)
						s = "not " + s;
				}
				switch (type) {
				case BY_ATOM:
					bs = selectAtoms(s);
					break;
				case BY_ELEMENT:
					bs = selectElements(s);
					break;
				case BY_RBOND:
					break;
				case BY_ABOND:
					break;
				case BY_TBOND:
					break;
				case BY_MOLECULE:
					break;
				}
				if (bs != null) {
					logicalStack.push(bs);
				}
				else {
					System.err.println("null bitset");
				}
			}
		}

		BitSet bs = (BitSet) logicalStack.pop();
		switch (type) {
		case BY_ATOM:
		case BY_ELEMENT:
			model.setSelectionSet(bs);
			break;
		case BY_RBOND:
			break;
		case BY_ABOND:
			break;
		case BY_TBOND:
			break;
		case BY_MOLECULE:
			break;
		}

		return bs;

	}

	private int getNumberOfObjects(byte type) {
		int n = 0;
		switch (type) {
		case BY_ATOM:
		case BY_ELEMENT:
			n = model.getAtomCount();
			break;
		case BY_RBOND:
			n = model.getRBondCount();
			break;
		case BY_ABOND:
			n = model.getABondCount();
			break;
		case BY_TBOND:
			n = model.getTBondCount();
			break;
		case BY_MOLECULE:
			n = model.getMoleculeCount();
			break;
		}
		return n;
	}

	protected String readText(String address, Component parent) throws InterruptedException {
		if (FileUtilities.isRelative(address))
			address = view.getCodeBase() + address;
		return super.readText(address, parent);
	}

	private static void fillActionIDMap() {
		if (actionIDMap == null)
			actionIDMap = new HashMap<String, Byte>();
		try {
			for (Field f : UserAction.class.getFields()) {
				actionIDMap.put(f.getName(), f.getByte(null));
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}