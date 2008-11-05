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

import javax.swing.JOptionPane;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.concord.modeler.ConnectionManager;
import org.concord.modeler.draw.FillMode;
import org.concord.modeler.event.ScriptEvent;
import org.concord.modeler.event.ScriptExecutionEvent;
import org.concord.modeler.event.ScriptExecutionListener;
import org.concord.modeler.process.DelayModelTimeLoadable;
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

	public Eval3D(MolecularModel model, boolean asTask) {
		super();
		this.model = model;
		atom = model.atom;
		view = model.getView();
		setAsTask(asTask);
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

	public void stop() {
		super.stop();
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				view.repaint();
				if (!getAsTask())
					notifyExecution("script end");
				if (model.initializationScriptToRun) {
					model.setInitializationScriptToRun(false);
				}
				else {
					model.notifyChange();
				}
			}
		});
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
		s = replaceAll(s, "%model_time", model.modelTime);
		s = replaceAll(s, "%number_of_atoms", model.getAtomCount());
		s = replaceAll(s, "%number_of_rbonds", model.getRBondCount());
		s = replaceAll(s, "%number_of_abonds", model.getABondCount());
		s = replaceAll(s, "%number_of_tbonds", model.getTBondCount());
		s = replaceAll(s, "%number_of_molecules", model.getMoleculeCount());
		s = replaceAll(s, "%number_of_obstacles", model.getObstacleCount());
		s = replaceAll(s, "%width", model.view.getWidth());
		s = replaceAll(s, "%height", model.view.getHeight());
		s = replaceAll(s, "%cell_length", model.getLength());
		s = replaceAll(s, "%cell_width", model.getWidth());
		s = replaceAll(s, "%cell_height", model.getHeight());
		s = replaceAll(s, "%loop_count", iLoop);
		s = replaceAll(s, "%loop_times", nLoop);
		s = replaceAll(s, "%index_of_selected_atom", model.view.getIndexOfSelectedAtom());
		s = replaceAll(s, "%temperature", model.getTemperature());
		s = replaceAll(s, "%mouse_x", mouseLocation.x);
		s = replaceAll(s, "%mouse_y", mouseLocation.y);
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
			String symbol = model.getSymbol(i);
			if (symbol == null) {
				out(ScriptEvent.FAILED, i + " is an invalid element id.");
				break;
			}
			v = escapeMetaCharacters(v);
			s = s.replaceAll("(?i)%element\\[" + v + "\\]\\.mass", "" + model.getElementMass(symbol));
			s = s.replaceAll("(?i)%element\\[" + v + "\\]\\.sigma", "" + model.getElementSigma(symbol));
			s = s.replaceAll("(?i)%element\\[" + v + "\\]\\.epsilon", "" + model.getElementEpsilon(symbol));
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
		int lb0 = -1;
		String v;
		int i;
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
			Atom a = model.atom[i];
			s = replaceAll(s, "%atom\\[" + v + "\\]\\.id", a.getElementNumber());
			s = replaceAll(s, "%atom\\[" + v + "\\]\\.mass", a.mass);
			s = replaceAll(s, "%atom\\[" + v + "\\]\\.charge", a.charge);
			s = replaceAll(s, "%atom\\[" + v + "\\]\\.sigma", a.sigma * 0.1);
			s = replaceAll(s, "%atom\\[" + v + "\\]\\.epsilon", a.epsilon);
			s = replaceAll(s, "%atom\\[" + v + "\\]\\.friction", a.damp);
			// s = replaceAll(s, "%atom\\[" + v + "\\]\\.hx", a.hx);
			// s = replaceAll(s, "%atom\\[" + v + "\\]\\.hy", a.hy);
			if (frame < 0) {
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.rx", a.rx);
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.ry", a.ry);
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.rz", a.rz);
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.vx", a.vx * V_CONVERTER);
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.vy", a.vy * V_CONVERTER);
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.vz", a.vz * V_CONVERTER);
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.ax", a.ax);
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.ay", a.ay);
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.az", a.az);
			}
			else {
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.rx", a.rQ.getQueue1().getData(frame));
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.ry", a.rQ.getQueue2().getData(frame));
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.rz", a.rQ.getQueue3().getData(frame));
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.vx", a.vQ.getQueue1().getData(frame) * V_CONVERTER);
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.vy", a.vQ.getQueue2().getData(frame) * V_CONVERTER);
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.vz", a.vQ.getQueue3().getData(frame) * V_CONVERTER);
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.ax", a.aQ.getQueue1().getData(frame));
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.ay", a.aQ.getQueue2().getData(frame));
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.az", a.aQ.getQueue3().getData(frame));
			}
			lb0 = lb;
			lb = s.indexOf("%atom[");
			if (lb0 == lb) // infinite loop
				break;
			rb = s.indexOf("].", lb);
		}
		return s;
	}

	private String useRbondVariables(String s, int frame) {
		int n = model.getRBondCount();
		if (n <= 0)
			return s;
		int lb = s.indexOf("%rbond[");
		int rb = s.indexOf("].", lb);
		int lb0 = -1;
		String v;
		int i;
		RBond bond;
		while (lb != -1 && rb != -1) {
			v = s.substring(lb + 7, rb);
			double x = parseMathExpression(v);
			if (Double.isNaN(x))
				break;
			i = (int) Math.round(x);
			if (i < 0 || i >= n) {
				out(ScriptEvent.FAILED, "Radial bond " + i + " does not exist.");
				break;
			}
			v = escapeMetaCharacters(v);
			bond = model.getRBond(i);
			s = replaceAll(s, "%rbond\\[" + v + "\\]\\.length", bond.getLength(frame));
			s = replaceAll(s, "%rbond\\[" + v + "\\]\\.strength", bond.getStrength());
			s = replaceAll(s, "%rbond\\[" + v + "\\]\\.bondlength", bond.getLength());
			s = replaceAll(s, "%rbond\\[" + v + "\\]\\.atom1", bond.atom1.getIndex());
			s = replaceAll(s, "%rbond\\[" + v + "\\]\\.atom2", bond.atom2.getIndex());
			lb0 = lb;
			lb = s.indexOf("%rbond[");
			if (lb0 == lb) // infinite loop
				break;
			rb = s.indexOf("].", lb);
		}
		return s;
	}

	private String useAbondVariables(String s, int frame) {
		int n = model.getABondCount();
		if (n <= 0)
			return s;
		int lb = s.indexOf("%abond[");
		int rb = s.indexOf("].", lb);
		int lb0 = -1;
		String v;
		int i;
		ABond bond;
		while (lb != -1 && rb != -1) {
			v = s.substring(lb + 7, rb);
			double x = parseMathExpression(v);
			if (Double.isNaN(x))
				break;
			i = (int) Math.round(x);
			if (i < 0 || i >= n) {
				out(ScriptEvent.FAILED, "Angular bond " + i + " does not exist.");
				break;
			}
			v = escapeMetaCharacters(v);
			bond = model.getABond(i);
			s = replaceAll(s, "%abond\\[" + v + "\\]\\.angle", bond.getAngle(frame));
			s = replaceAll(s, "%abond\\[" + v + "\\]\\.strength", bond.getStrength());
			s = replaceAll(s, "%abond\\[" + v + "\\]\\.bondangle", bond.getAngle());
			s = replaceAll(s, "%abond\\[" + v + "\\]\\.atom1", bond.atom1.getIndex());
			s = replaceAll(s, "%abond\\[" + v + "\\]\\.atom2", bond.atom2.getIndex());
			s = replaceAll(s, "%abond\\[" + v + "\\]\\.atom3", bond.atom3.getIndex());
			lb0 = lb;
			lb = s.indexOf("%abond[");
			if (lb0 == lb) // infinite loop
				break;
			rb = s.indexOf("].", lb);
		}
		return s;
	}

	private String useTbondVariables(String s, int frame) {
		int n = model.getTBondCount();
		if (n <= 0)
			return s;
		int lb = s.indexOf("%tbond[");
		int rb = s.indexOf("].", lb);
		int lb0 = -1;
		String v;
		int i;
		TBond bond;
		while (lb != -1 && rb != -1) {
			v = s.substring(lb + 7, rb);
			double x = parseMathExpression(v);
			if (Double.isNaN(x))
				break;
			i = (int) Math.round(x);
			if (i < 0 || i >= n) {
				out(ScriptEvent.FAILED, "Torsional bond " + i + " does not exist.");
				break;
			}
			v = escapeMetaCharacters(v);
			bond = model.getTBond(i);
			s = replaceAll(s, "%tbond\\[" + v + "\\]\\.angle", bond.getAngle(frame));
			s = replaceAll(s, "%tbond\\[" + v + "\\]\\.strength", bond.getStrength());
			s = replaceAll(s, "%tbond\\[" + v + "\\]\\.bondangle", bond.getAngle());
			s = replaceAll(s, "%tbond\\[" + v + "\\]\\.atom1", bond.atom1.getIndex());
			s = replaceAll(s, "%tbond\\[" + v + "\\]\\.atom2", bond.atom2.getIndex());
			s = replaceAll(s, "%tbond\\[" + v + "\\]\\.atom3", bond.atom3.getIndex());
			s = replaceAll(s, "%tbond\\[" + v + "\\]\\.atom4", bond.atom4.getIndex());
			lb0 = lb;
			lb = s.indexOf("%tbond[");
			if (lb0 == lb) // infinite loop
				break;
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
		s = useRbondVariables(s, -1);
		s = useAbondVariables(s, -1);
		s = useTbondVariables(s, -1);
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
	void evaluate2() throws InterruptedException {
		if (!getAsTask()) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					notifyExecution("script start");
				}
			});
		}
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
		script = storeMouseScripts(script);
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

		// call external scripts
		if (ciLC.startsWith("external")) {
			String address = ci.substring(8).trim();
			if (address != null && !address.equals("")) {
				Matcher matcher = NNI.matcher(address);
				if (matcher.find()) {
					byte i = Byte.parseByte(address);
					String s = externalScripts.get(i);
					if (s == null) {
						out(ScriptEvent.FAILED, "External command error: " + ci);
						return false;
					}
					evaluateExternalClause(s);
				}
				else {
					evaluateExternalClause(readText(address, view));
				}
			}
			return true;
		}

		logicalStack.clear();

		if (!checkParenthesisBalance(ci))
			return false;

		// plot
		Matcher matcher = PLOT.matcher(ci);
		if (matcher.find()) {
			if (evaluatePlotClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// store
		matcher = STORE.matcher(ci);
		if (matcher.find()) {
			if (evaluateStoreClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// increment or decrement operator
		matcher = INCREMENT_DECREMENT.matcher(ci);
		if (matcher.find())
			return evaluateIncrementOperator(ci);

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
			if (evaluateLoadClause(ci.substring(matcher.end()).trim(), false))
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
		matcher = BUILD_RBOND.matcher(ci);
		if (matcher.find()) {
			if (evaluateBuildRBondClause(ci.substring(ci.startsWith("rbond") ? 5 : 4).trim()))
				return true;
		}

		// build angular bond
		matcher = BUILD_ABOND.matcher(ci);
		if (matcher.find()) {
			if (evaluateBuildABondClause(ci.substring(ci.startsWith("abond") ? 5 : 4).trim()))
				return true;
		}

		// build torsional bond
		matcher = BUILD_TBOND.matcher(ci);
		if (matcher.find()) {
			if (evaluateBuildTBondClause(ci.substring(ci.startsWith("tbond") ? 5 : 4).trim()))
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
		String strLC = str.toLowerCase();
		if ("paint".equals(strLC)) { // paint
			view.paintImmediately(0, 0, view.getWidth(), view.getHeight());
			return true;
		}
		if ("snapshot".equals(strLC)) { // snapshot
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					if (view.getSnapshotListener() != null)
						view.getSnapshotListener().actionPerformed(null);
				}
			});
			return true;
		}
		if ("focus".equals(strLC)) { // focus
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					view.requestFocusInWindow();
				}
			});
			return true;
		}
		if ("run".equals(strLC)) { // run
			// FIXME: Why do we need to do this to make "delay modeltime" to work with a prior "run" command?
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e) {
			}
			model.run();
			model.notifyChange();
			notifyExecution("run");
			return true;
		}
		if (strLC.startsWith("stop")) {
			if ("stop".equals(strLC)) { // stop
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						model.stop();
						notifyExecution("stop");
					}
				});
				return true;
			}
			if ("immediately".equals(strLC.substring(4).trim())) { // stop immediately
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						model.stopImmediately();
						notifyExecution("stop");
					}
				});
				return true;
			}
		}
		if (strLC.startsWith("reset")) {
			if ("reset".equals(strLC)) { // reset
				evaluateLoadClause(view.getResourceAddress(), true);
				notifyExecution("reset");
				return true;
			}
			if ("silently".equals(strLC.substring(5).trim())) { // reset silently
				evaluateLoadClause(view.getResourceAddress(), true);
				return true;
			}
		}
		if ("remove".equals(strLC)) { // remove selected objects
			// CAUTION!!!!!!!!! PUTTING INTO EVENTQUEUE is dangerous if there are commands following it!
			removeSelectedObjects();
			// EventQueue.invokeLater(new Runnable() { public void run() { removeSelectedObjects(); } });
			model.notifyChange();
			return true;
		}
		return false;
	}

	private void deselectAll() {
		// view.setImageSelectionSet(null);
		// view.setLineSelectionSet(null);
		// view.setRectangleSelectionSet(null);
		// view.setEllipseSelectionSet(null);
		// view.setTextBoxSelectionSet(null);
		// model.setObstacleSelectionSet(null);
		model.setAtomSelectionSet(null);
		model.setRBondSelectionSet(null);
		model.setABondSelectionSet(null);
		model.setTBondSelectionSet(null);
		model.setMoleculeSelectionSet(null);
	}

	private boolean evaluateSelectClause(String clause) {

		// "none" clause is a special case for clearing the selection status of all types
		if ("none".equalsIgnoreCase(clause)) {
			deselectAll();
			return true;
		}

		if (!NOT_SELECTED.matcher(clause).find()) { // "not selected" clause is a special case.
			deselectAll();
		}

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
		matcher = ELEMENT.matcher(clause); // select by element
		if (matcher.find()) {
			String str = clause.substring(matcher.end()).trim();
			BitSet selection = null;
			if (LOGICAL_OPERATOR.matcher(str).find()) {// logical expressions
				selection = parseLogicalExpression(str, BY_ELEMENT);
			}
			else {
				selection = selectElements(str);
			}
			out(ScriptEvent.SUCCEEDED, (selection != null ? selection.cardinality() : 0) + " atoms are selected.");
			return true;
		}
		matcher = RBOND.matcher(clause); // select by radial bond
		if (matcher.find()) {
			String str = clause.substring(matcher.end()).trim();
			BitSet selection = null;
			if (LOGICAL_OPERATOR.matcher(str).find()) {// logical expressions
				selection = parseLogicalExpression(str, BY_RBOND);
			}
			else {
				selection = selectRBonds(str);
			}
			out(ScriptEvent.SUCCEEDED, (selection != null ? selection.cardinality() : 0)
					+ " radial bonds are selected.");
			return true;
		}
		matcher = ABOND.matcher(clause); // select by angular bond
		if (matcher.find()) {
			String str = clause.substring(matcher.end()).trim();
			BitSet selection = null;
			if (LOGICAL_OPERATOR.matcher(str).find()) {// logical expressions
				selection = parseLogicalExpression(str, BY_ABOND);
			}
			else {
				selection = selectABonds(str);
			}
			out(ScriptEvent.SUCCEEDED, (selection != null ? selection.cardinality() : 0)
					+ " angular bonds are selected.");
			return true;
		}
		matcher = TBOND.matcher(clause); // select by torsional bond
		if (matcher.find()) {
			String str = clause.substring(matcher.end()).trim();
			BitSet selection = null;
			if (LOGICAL_OPERATOR.matcher(str).find()) {// logical expressions
				selection = parseLogicalExpression(str, BY_TBOND);
			}
			else {
				selection = selectTBonds(str);
			}
			out(ScriptEvent.SUCCEEDED, (selection != null ? selection.cardinality() : 0)
					+ " torsional bonds are selected.");
			return true;
		}
		matcher = MOLECULE.matcher(clause); // select by molecule
		if (matcher.find()) {
			String str = clause.substring(matcher.end()).trim();
			BitSet selection = null;
			if (LOGICAL_OPERATOR.matcher(str).find()) { // logical expressions
				selection = parseLogicalExpression(str, BY_MOLECULE);
			}
			else {
				selection = selectMolecules(str);
			}
			out(ScriptEvent.SUCCEEDED, (selection != null ? selection.cardinality() : 0) + " molecules are selected.");
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
				view.setFillMode(new FillMode.ImageFill(address));
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
				view.setCharge(k, c);
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
		float h = 0;
		try {
			h = Float.valueOf(str).floatValue();
		}
		catch (NumberFormatException e) {
			out(ScriptEvent.FAILED, str + " cannot be parsed as a number.");
			return false;
		}
		if (h == 0)
			return true;
		int nop = model.getAtomCount();
		if (nop <= 0)
			return true;
		List<Atom> list = new ArrayList<Atom>();
		for (int k = 0; k < nop; k++) {
			if (atom[k].isSelected())
				list.add(atom[k]);
		}
		model.heatAtoms(list, h);
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

	private boolean evaluateBuildRBondClause(String str) {
		String[] s = str.split(REGEX_SEPARATOR);
		if (s.length != 3)
			return false;
		int n = model.getAtomCount();
		double x = parseMathExpression(s[0]); // index of atom 1
		if (Double.isNaN(x))
			return false;
		int i = (int) x;
		if (i >= n) {
			out(ScriptEvent.FAILED, "Atom index out of limit: i=" + i + ">=" + n);
			return false;
		}
		x = parseMathExpression(s[1]); // index of atom 2
		if (Double.isNaN(x))
			return false;
		int j = (int) x;
		if (j >= n) {
			out(ScriptEvent.FAILED, "Atom index out of limit: j=" + j + ">=" + n);
			return false;
		}
		if (j == i) {
			out(ScriptEvent.FAILED, "Cannot build a radial bond between a pair of identical atoms: i=j=" + i);
			return false;
		}
		x = parseMathExpression(s[2]); // strength
		if (Double.isNaN(x))
			return false;
		Atom a1 = model.getAtom(i);
		Atom a2 = model.getAtom(j);
		if (x > ZERO) {
			RBond rb = view.addRBond(a1, a2);
			rb.setStrength((float) x);
			view.repaint();
			model.notifyChange();
		}
		return true;
	}

	private boolean evaluateBuildABondClause(String str) {
		String[] s = str.split(REGEX_SEPARATOR);
		if (s.length != 4)
			return false;
		int n = model.getAtomCount();
		double x = parseMathExpression(s[0]); // index of atom 1
		if (Double.isNaN(x))
			return false;
		int i = (int) x;
		if (i >= n) {
			out(ScriptEvent.FAILED, "Atom index out of limit: i=" + i + ">=" + n);
			return false;
		}
		x = parseMathExpression(s[1]); // index of atom 2
		if (Double.isNaN(x))
			return false;
		int j = (int) x;
		if (j >= n) {
			out(ScriptEvent.FAILED, "Atom index out of limit: j=" + j + ">=" + n);
			return false;
		}
		if (j == i) {
			out(ScriptEvent.FAILED, "Cannot build an angular bond for identical atoms: i=j=" + i);
			return false;
		}
		x = parseMathExpression(s[2]); // index of atom 3
		if (Double.isNaN(x))
			return false;
		int k = (int) x;
		if (k >= n) {
			out(ScriptEvent.FAILED, "Atom index out of limit: k=" + k + ">=" + n);
			return false;
		}
		if (k == i || k == j) {
			out(ScriptEvent.FAILED, "Cannot build an angular bond for identical atoms: " + i + "," + j + "," + k);
			return false;
		}
		x = parseMathExpression(s[3]); // strength
		if (Double.isNaN(x))
			return false;
		Atom a1 = model.getAtom(i);
		Atom a2 = model.getAtom(j);
		Atom a3 = model.getAtom(k);
		if (x > ZERO) {
			RBond r1 = model.getRBond(a1, a2);
			if (r1 == null) {
				out(ScriptEvent.FAILED, "There must be a radial bond between " + i + " and " + j);
				return false;
			}
			RBond r2 = model.getRBond(a2, a3);
			if (r2 == null) {
				out(ScriptEvent.FAILED, "There must be a radial bond between " + j + " and " + k);
				return false;
			}
			ABond ab = view.addABond(r1, r2);
			ab.setStrength((float) x);
			view.repaint();
			model.notifyChange();
		}
		return true;
	}

	private boolean evaluateBuildTBondClause(String str) {
		String[] s = str.split(REGEX_SEPARATOR);
		if (s.length != 5)
			return false;
		int n = model.getAtomCount();
		double x = parseMathExpression(s[0]); // index of atom 1
		if (Double.isNaN(x))
			return false;
		int i = (int) x;
		if (i >= n) {
			out(ScriptEvent.FAILED, "Atom index out of limit: i=" + i + ">=" + n);
			return false;
		}
		x = parseMathExpression(s[1]); // index of atom 2
		if (Double.isNaN(x))
			return false;
		int j = (int) x;
		if (j >= n) {
			out(ScriptEvent.FAILED, "Atom index out of limit: j=" + j + ">=" + n);
			return false;
		}
		if (j == i) {
			out(ScriptEvent.FAILED, "Cannot build a torsional bond for identical atoms: i=j=" + i);
			return false;
		}
		x = parseMathExpression(s[2]); // index of atom 3
		if (Double.isNaN(x))
			return false;
		int k = (int) x;
		if (k >= n) {
			out(ScriptEvent.FAILED, "Atom index out of limit: k=" + k + ">=" + n);
			return false;
		}
		if (k == i || k == j) {
			out(ScriptEvent.FAILED, "Cannot build a torsional bond for identical atoms: " + i + "," + j + "," + k);
			return false;
		}
		x = parseMathExpression(s[3]); // index of atom 4
		if (Double.isNaN(x))
			return false;
		int l = (int) x;
		if (l >= n) {
			out(ScriptEvent.FAILED, "Atom index out of limit: l=" + l + ">=" + n);
			return false;
		}
		if (l == i || l == j || l == k) {
			out(ScriptEvent.FAILED, "Cannot build a torsional bond for identical atoms: " + i + "," + j + "," + k + ","
					+ l);
			return false;
		}
		x = parseMathExpression(s[4]); // strength
		if (Double.isNaN(x))
			return false;
		Atom a1 = model.getAtom(i);
		Atom a2 = model.getAtom(j);
		Atom a3 = model.getAtom(k);
		Atom a4 = model.getAtom(l);
		if (x > ZERO) {
			ABond b1 = model.getABond(a1, a2, a3);
			if (b1 == null) {
				out(ScriptEvent.FAILED, "There must be an angular bond among " + a1 + ", " + a2 + " and " + a3);
				return false;
			}
			ABond b2 = model.getABond(a2, a3, a4);
			if (b2 == null) {
				out(ScriptEvent.FAILED, "There must be an angular bond among " + a2 + ", " + a3 + " and " + a4);
				return false;
			}
			TBond tb = view.addTBond(b1, b2);
			if (tb != null) {
				tb.setStrength((float) x);
				view.repaint();
				model.notifyChange();
			}
		}
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
		result = parseOnOff("selectionhalo", s);
		if (result != -1) {
			view.getViewer().setSelectionHaloEnabled(result == ON);
			view.repaint();
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

		String[] s = null;

		// atom field
		matcher = ATOM_FIELD.matcher(str);
		if (matcher.find()) {
			int end = matcher.end();
			String s2 = str.substring(end).trim();
			int i = s2.indexOf(" ");
			if (i < 0) {
				out(ScriptEvent.FAILED, "Argument error: " + str);
				return false;
			}
			s = new String[] { s2.substring(0, i).trim(), s2.substring(i + 1).trim() };
			s2 = str.substring(0, end - 1);
			if (s2.startsWith("%")) {
				s2 = s2.substring(1);
			}
			if ("on".equalsIgnoreCase(s[1]) || "off".equalsIgnoreCase(s[1])) {
				if ("visible".equalsIgnoreCase(s[0])) {
					setAtomField(s2, s[0], "on".equalsIgnoreCase(s[1]));
				}
				else if ("movable".equalsIgnoreCase(s[0])) {
					setAtomField(s2, s[0], "on".equalsIgnoreCase(s[1]));
				}
			}
			else {
				double x = parseMathExpression(s[1]);
				if (Double.isNaN(x))
					return false;
				setAtomField(s2, s[0], (float) x);
			}
			return true;
		}

		if (str.trim().startsWith("%")) { // change the value of a defined variable
			int whitespace = str.indexOf(" ");
			String var = str.substring(0, whitespace).trim().toLowerCase();
			String exp = str.substring(whitespace).trim().toLowerCase();
			boolean isStatic = false;
			if (!sharedDefinition.isEmpty()) {
				Map<String, String> map = sharedDefinition.get(model.getClass());
				if (map != null && !map.isEmpty()) {
					if (map.containsKey(var)) {
						isStatic = true;
					}
				}
			}
			if (exp.startsWith("temperature(")) {
				// exp = evaluateTemperatureFunction(exp);
				// if (exp != null)
				// storeDefinition(isStatic, var, exp);
			}
			else {
				evaluateDefineMathexClause(isStatic, var, exp);
			}
			return true;
		}

		s = str.trim().split(REGEX_SEPARATOR + "+");

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
						if (a.isSelected()) {
							a.setVisible(b);
							view.setVisible(a, b);
						}
					}
					n = model.getRBondCount();
					for (int i = 0; i < n; i++) {
						RBond rb = model.getRBond(i);
						if (rb.isSelected()) {
							rb.setVisible(b);
							view.setVisible(rb, b);
						}
					}
					model.notifyChange();
					return true;
				}
				else if (s0 == "translucent") {
					boolean b = "on".equalsIgnoreCase(s[1].trim());
					int n = model.getAtomCount();
					for (int i = 0; i < n; i++) {
						Atom a = model.getAtom(i);
						if (a.isSelected()) {
							view.setTranslucent(a, b);
						}
					}
					n = model.getRBondCount();
					for (int i = 0; i < n; i++) {
						RBond rb = model.getRBond(i);
						if (rb.isSelected()) {
						}
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
			else if (s0 == "timestep") {
				model.setTimeStep((float) x);
				model.notifyChange();
				return true;
			}
			else if (s0 == "heatbath") {
				model.activateHeatBath(x > 0);
				if (model.heatBathActivated())
					model.getHeatBath().setExpectedTemperature((float) x);
				model.notifyChange();
				return true;
			}
			else if (s0 == "model_time") {
				model.setModelTime((float) x);
				model.notifyChange();
				return true;
			}
			else if (s0 == "temperature") {
				model.setTemperature((float) x);
				model.notifyChange();
				return true;
			}
			else if (s0 == "cell_length") {
				model.setLength((float) x);
				view.setSimulationBox();
				model.notifyChange();
				return true;
			}
			else if (s0 == "cell_width") {
				model.setWidth((float) x);
				view.setSimulationBox();
				model.notifyChange();
				return true;
			}
			else if (s0 == "cell_height") {
				model.setHeight((float) x);
				view.setSimulationBox();
				model.notifyChange();
				return true;
			}
		}

		else if (s.length == 4) {

			String s0 = s[0].trim().toLowerCase().intern();

			if (s0 == "gfield") {
				s[1] = s[1].trim();
				if (s[1].startsWith("("))
					s[1] = s[1].substring(1);
				double x = parseMathExpression(s[1]);
				if (Double.isNaN(x))
					return false;
				double y = parseMathExpression(s[2]);
				if (Double.isNaN(y))
					return false;
				s[3] = s[3].trim();
				if (s[3].endsWith(")"))
					s[3] = s[3].substring(0, s[3].length() - 1);
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
				s[1] = s[1].trim();
				if (s[1].startsWith("("))
					s[1] = s[1].substring(1);
				double x = parseMathExpression(s[1]);
				if (Double.isNaN(x))
					return false;
				double y = parseMathExpression(s[2]);
				if (Double.isNaN(y))
					return false;
				s[3] = s[3].trim();
				if (s[3].endsWith(")"))
					s[3] = s[3].substring(0, s[3].length() - 1);
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
				s[1] = s[1].trim();
				if (s[1].startsWith("("))
					s[1] = s[1].substring(1);
				double x = parseMathExpression(s[1]);
				if (Double.isNaN(x))
					return false;
				double y = parseMathExpression(s[2]);
				if (Double.isNaN(y))
					return false;
				s[3] = s[3].trim();
				if (s[3].endsWith(")"))
					s[3] = s[3].substring(0, s[3].length() - 1);
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
		Matcher matcher = ATOM.matcher(str);
		if (matcher.find()) {
			str = str.substring(matcher.end()).trim();
			Point3f p = new Point3f();
			String s1 = null;
			int space = str.indexOf(" ");
			if (space < 0) {
				s1 = str.substring(0).trim();
				p.x = (float) (Math.random() - 0.5f) * model.getLength();
				p.y = (float) (Math.random() - 0.5f) * model.getWidth();
				p.z = (float) (Math.random() - 0.5f) * model.getHeight();
			}
			else {
				s1 = str.substring(0, space).trim();
				String s2 = str.substring(space + 1).trim();
				float[] r = parseCoordinates(s2);
				if (r != null) {
					p.x = r[0];
					p.y = r[1];
					p.z = r[2];
				}
				else {
					out(ScriptEvent.FAILED, "Error: Cannot parse " + str);
					return false;
				}
			}
			String symbol = null;
			for (String s : model.paramMap.keySet()) {
				if (s.equalsIgnoreCase(s1)) {
					symbol = s;
					break;
				}
			}
			if (symbol == null) {
				double a = parseMathExpression(s1);
				if (!Double.isNaN(a)) {
					symbol = model.getSymbol((int) Math.round(a));
				}
			}
			if (symbol == null) {
				out(ScriptEvent.FAILED, "Unrecognized element to add: " + str);
				return false;
			}
			if (view.addAtom(p, symbol)) {
				view.repaint();
			}
			else {
				out(ScriptEvent.HARMLESS, "Cannot insert an atom to the specified location: " + str);
			}
			return true;
		}
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
				DelayModelTimeLoadable l = new DelayModelTimeLoadable(i) {
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
	private synchronized boolean evaluateLoadClause(String address, boolean reset) throws InterruptedException {
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
			URL u = ConnectionManager.sharedInstance().getRemoteLocation(address);
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
				view.getContainer().input(url, reset);
			}
		}
		else {
			view.getContainer().input(new File(address), reset);
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
		if (str == null || str.trim().equals(""))
			return false;
		String[] s = str.split(REGEX_WHITESPACE);
		List<String> list = new ArrayList<String>();
		for (String si : s) {
			if (si.trim().equals(""))
				continue;
			list.add(si.trim());
		}
		float steplength = 0;
		int nstep = 0;
		switch (list.size()) {
		case 2:
			try {
				steplength = Float.parseFloat(list.get(0)) * 10;
				nstep = (int) Float.parseFloat(list.get(1));
			}
			catch (NumberFormatException e) {
				out(ScriptEvent.FAILED, "Unable to parse number: " + str);
				return false;
			}
			break;
		case 3:
			try {
				steplength = Float.parseFloat(list.get(1)) * 10;
				nstep = (int) Float.parseFloat(list.get(2));
			}
			catch (NumberFormatException e) {
				out(ScriptEvent.FAILED, "Unable to parse number: " + str);
				return false;
			}
			break;
		}
		if (steplength > 0 && nstep > 0) {
			model.minimize(nstep, steplength, 10);
			model.notifyChange();
			return true;
		}
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

	private boolean evaluateStoreClause(String str) {
		int i = str.indexOf(" ");
		if (i == -1) {
			out(ScriptEvent.FAILED, "Syntax error: store " + str);
			return false;
		}
		String s = str.substring(0, i);
		String t = str.substring(i).trim();
		try {
			i = Integer.parseInt(s);
		}
		catch (NumberFormatException e) {
			out(ScriptEvent.FAILED, "Expected integer: " + s);
			return false;
		}
		double x = parseMathExpression(t);
		if (Double.isNaN(x))
			return false;
		model.setChannel(i, (float) x);
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

	private void setAtomField(String str1, String str2, boolean x) {
		int lb = str1.indexOf("[");
		int rb = str1.indexOf("]");
		double z = parseMathExpression(str1.substring(lb + 1, rb));
		if (Double.isNaN(z))
			return;
		int i = (int) Math.round(z);
		if (i < 0 || i >= model.getAtomCount()) {
			out(ScriptEvent.FAILED, "Atom " + i + " doesn't exisit.");
			return;
		}
		String s = str2.toLowerCase().intern();
		boolean b = true;
		if (s == "visible") {
			model.atom[i].setVisible(x);
			view.setVisible(model.atom[i], x);
		}
		else if (s == "movable")
			model.atom[i].setMovable(x);
		else {
			out(ScriptEvent.FAILED, "Cannot set propery: " + str2);
			b = false;
		}
		if (b) {
			view.repaint();
			model.notifyChange();
		}
	}

	private void setAtomField(String str1, String str2, float x) {
		int lb = str1.indexOf("[");
		int rb = str1.indexOf("]");
		double z = parseMathExpression(str1.substring(lb + 1, rb));
		if (Double.isNaN(z))
			return;
		int i = (int) Math.round(z);
		if (i < 0 || i >= model.getAtomCount()) {
			out(ScriptEvent.FAILED, "Atom " + i + " doesn't exisit.");
			return;
		}
		String s = str2.toLowerCase().intern();
		boolean b = true;
		if (s == "id") {
			String symbol = model.getSymbol((int) x);
			atom[i].setSymbol(symbol);
			view.getViewer().setAtomType(i, (short) x, symbol);
			// FIXME: this doesn't change the view
		}
		else if (s == "rx")
			atom[i].rx = x;
		else if (s == "ry")
			atom[i].ry = x;
		else if (s == "rz")
			atom[i].rz = x;
		else if (s == "vx")
			atom[i].vx = x * IV_CONVERTER;
		else if (s == "vy")
			atom[i].vy = x * IV_CONVERTER;
		else if (s == "vz")
			atom[i].vz = x * IV_CONVERTER;
		else if (s == "ax")
			atom[i].ax = x;
		else if (s == "ay")
			atom[i].ay = x;
		else if (s == "az")
			atom[i].az = x;
		else if (s == "charge")
			atom[i].charge = x;
		else if (s == "friction")
			atom[i].damp = x;
		else {
			out(ScriptEvent.FAILED, "Cannot set propery: " + str2);
			b = false;
		}
		if (b)
			model.notifyChange();
	}

	private void removeSelectedObjects() {
		List<TBond> deletedTBond = new ArrayList<TBond>();
		synchronized (model.tBonds) {
			for (TBond tb : model.tBonds) {
				if (tb.isSelected())
					deletedTBond.add(tb);
			}
		}
		if (!deletedTBond.isEmpty()) {
			for (TBond tb : deletedTBond)
				view.removeTBond(tb);
		}
		List<ABond> deletedABond = new ArrayList<ABond>();
		synchronized (model.aBonds) {
			for (ABond ab : model.aBonds) {
				if (ab.isSelected())
					deletedABond.add(ab);
			}
		}
		if (!deletedABond.isEmpty()) {
			for (ABond ab : deletedABond)
				view.removeABond(ab);
		}
		List<RBond> deletedRBond = new ArrayList<RBond>();
		synchronized (model.rBonds) {
			for (RBond rb : model.rBonds) {
				if (rb.isSelected()) {
					deletedRBond.add(rb);
				}
			}
		}
		if (!deletedRBond.isEmpty()) {
			for (RBond rb : deletedRBond)
				view.removeRBond(rb);
		}
		int n = model.getAtomCount();
		BitSet bs = new BitSet(n);
		for (int k = 0; k < n; k++) {
			if (atom[k].isSelected())
				bs.set(k);
		}
		if (bs.cardinality() > 0)
			view.removeAtoms(bs);
		model.formMolecules();
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
			model.setAtomSelectionSet(bs);
		return found ? bs : null;
	}

	private BitSet selectAtoms(String str) {

		if ("selected".equalsIgnoreCase(str)) {
			return model.getAtomSelectionSet();
		}

		BitSet bs = genericSelect(str);
		if (bs != null)
			return bs;

		boolean found = false;
		int noa = model.getAtomCount();
		bs = new BitSet(noa);

		Matcher matcher = WITHIN_RADIUS.matcher(str);
		if (matcher.find()) {
			found = true;
			String s = str.substring(matcher.end()).trim();
			s = s.substring(0, s.indexOf(")"));
			if (RANGE.matcher(s).find()) {
				int lp = str.lastIndexOf("(");
				int rp = str.indexOf(")");
				s = str.substring(lp + 1, rp).trim();
				float r = Float.valueOf(s.substring(0, s.indexOf(",")).trim()).floatValue();
				r *= r;
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
				r *= r;
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
			model.setAtomSelectionSet(bs);
		}
		else {
			out(ScriptEvent.FAILED, "Unrecognized expression: " + str);
		}

		return found ? bs : null;

	}

	private BitSet selectElements(String str) {

		if ("selected".equalsIgnoreCase(str))
			return model.getAtomSelectionSet();

		BitSet bs = genericSelect(str);
		if (bs != null)
			return bs;

		int noa = model.getAtomCount();
		bs = new BitSet(noa);

		String[] s = str.split(",");
		for (int i = 0; i < s.length; i++)
			s[i] = s[i].trim();

		for (int i = 0; i < noa; i++) {
			Atom a = model.getAtom(i);
			for (String x : s) {
				if (a.getSymbol().equalsIgnoreCase(x)) {
					bs.set(i);
					break;
				}
			}
		}

		model.setAtomSelectionSet(bs);

		return bs;

	}

	private BitSet selectRBonds(String str) {
		int n = model.rBonds.size();
		if (n == 0)
			return null;
		BitSet bs = new BitSet(n);
		if ("selected".equalsIgnoreCase(str)) {
			synchronized (model.rBonds) {
				for (int i = 0; i < n; i++) {
					if (model.getRBond(i).isSelected())
						bs.set(i);
				}
			}
			return bs;
		}
		String strLC = str.toLowerCase();
		if (strLC.indexOf("involve") != -1) {
			str = str.substring(7).trim();
			strLC = str.toLowerCase();
			if (strLC.indexOf("atom") != -1) {
				str = str.substring(4).trim();
				if (selectRbondsInvolving(str, bs)) {
					model.setRBondSelectionSet(bs);
					return bs;
				}
			}
			else {
				out(ScriptEvent.FAILED, "Unrecognized expression: " + str);
				return null;
			}
		}
		if (selectFromCollection(str, n, bs)) {
			model.setRBondSelectionSet(bs);
			return bs;
		}
		out(ScriptEvent.FAILED, "Unrecognized expression: " + str);
		return null;
	}

	private boolean selectRbondsInvolving(String str, BitSet bs) {
		if (RANGE_LEADING.matcher(str).find()) {
			String[] s = str.split("-");
			int beg = Float.valueOf(s[0].trim()).intValue();
			int end = Float.valueOf(s[1].trim()).intValue();
			synchronized (model.rBonds) {
				for (RBond rb : model.rBonds) {
					if (inRangeInclusive(rb.getAtom1().getIndex(), beg, end)
							|| inRangeInclusive(rb.getAtom2().getIndex(), beg, end)) {
						bs.set(model.rBonds.indexOf(rb));
					}
				}
			}
			return true;
		}
		if (INTEGER_GROUP.matcher(str).find()) {
			String[] s = str.split(REGEX_SEPARATOR + "+");
			int index;
			for (int m = 0; m < s.length; m++) {
				index = Float.valueOf(s[m]).intValue();
				synchronized (model.rBonds) {
					for (RBond rb : model.rBonds) {
						if (rb.contains(index))
							bs.set(model.rBonds.indexOf(rb));
					}
				}
			}
			return true;
		}
		if (INDEX.matcher(str).find()) {
			int index = Float.valueOf(str.trim()).intValue();
			synchronized (model.rBonds) {
				for (RBond rb : model.rBonds) {
					if (rb.contains(index))
						bs.set(model.rBonds.indexOf(rb));
				}
			}
			return true;
		}
		str = str.trim();
		if (str.length() > 0) {
			String[] s = str.split(REGEX_SEPARATOR + "+");
			for (int i = 0; i < s.length; i++) {
				s[i] = standardizeElementName(s[i]);
				Object o = model.paramMap.get(s[i]);
				if (o != null) {
					synchronized (model.rBonds) {
						for (RBond rb : model.rBonds) {
							if (rb.containsElement(s[i])) {
								bs.set(model.rBonds.indexOf(rb));
							}
						}
					}
				}
			}
			return true;
		}
		return false;
	}

	private BitSet selectABonds(String str) {
		int n = model.aBonds.size();
		if (n == 0)
			return null;
		BitSet bs = new BitSet(n);
		if ("selected".equalsIgnoreCase(str)) {
			synchronized (model.aBonds) {
				for (int i = 0; i < n; i++) {
					if (model.getABond(i).isSelected())
						bs.set(i);
				}
			}
			return bs;
		}
		String strLC = str.toLowerCase();
		if (strLC.indexOf("involve") != -1) {
			str = str.substring(7).trim();
			strLC = str.toLowerCase();
			if (strLC.indexOf("atom") != -1) {
				str = str.substring(4).trim();
				if (selectAbondsInvolving(str, bs)) {
					model.setABondSelectionSet(bs);
					return bs;
				}
			}
			else {
				out(ScriptEvent.FAILED, "Unrecognized expression: " + str);
				return null;
			}
		}
		if (selectFromCollection(str, n, bs)) {
			model.setABondSelectionSet(bs);
			return bs;
		}
		out(ScriptEvent.FAILED, "Unrecognized expression: " + str);
		return null;
	}

	private boolean selectAbondsInvolving(String str, BitSet bs) {
		if (RANGE_LEADING.matcher(str).find()) {
			String[] s = str.split("-");
			int beg = Float.valueOf(s[0].trim()).intValue();
			int end = Float.valueOf(s[1].trim()).intValue();
			synchronized (model.aBonds) {
				for (ABond ab : model.aBonds) {
					if (inRangeInclusive(ab.getAtom1().getIndex(), beg, end)
							|| inRangeInclusive(ab.getAtom2().getIndex(), beg, end)
							|| inRangeInclusive(ab.getAtom3().getIndex(), beg, end)) {
						bs.set(model.aBonds.indexOf(ab));
					}
				}
			}
			return true;
		}
		if (INTEGER_GROUP.matcher(str).find()) {
			String[] s = str.split(REGEX_SEPARATOR + "+");
			int index;
			for (int m = 0; m < s.length; m++) {
				index = Float.valueOf(s[m]).intValue();
				synchronized (model.aBonds) {
					for (ABond ab : model.aBonds) {
						if (ab.contains(index))
							bs.set(model.aBonds.indexOf(ab));
					}
				}
			}
			return true;
		}
		if (INDEX.matcher(str).find()) {
			int index = Float.valueOf(str.trim()).intValue();
			synchronized (model.aBonds) {
				for (ABond ab : model.aBonds) {
					if (ab.contains(index))
						bs.set(model.aBonds.indexOf(ab));
				}
			}
			return true;
		}
		str = str.trim();
		if (str.length() > 0) {
			String[] s = str.split(REGEX_SEPARATOR + "+");
			for (int i = 0; i < s.length; i++) {
				s[i] = standardizeElementName(s[i]);
				Object o = model.paramMap.get(s[i]);
				if (o != null) {
					synchronized (model.aBonds) {
						for (ABond ab : model.aBonds) {
							if (ab.containsElement(s[i])) {
								bs.set(model.aBonds.indexOf(ab));
							}
						}
					}
				}
			}
			return true;
		}
		return false;
	}

	private BitSet selectTBonds(String str) {
		int n = model.tBonds.size();
		if (n == 0)
			return null;
		BitSet bs = new BitSet(n);
		if ("selected".equalsIgnoreCase(str)) {
			synchronized (model.tBonds) {
				for (int i = 0; i < n; i++) {
					if (model.getTBond(i).isSelected())
						bs.set(i);
				}
			}
			return bs;
		}
		String strLC = str.toLowerCase();
		if (strLC.indexOf("involve") != -1) {
			str = str.substring(7).trim();
			strLC = str.toLowerCase();
			if (strLC.indexOf("atom") != -1) {
				str = str.substring(4).trim();
				if (selectTbondsInvolving(str, bs)) {
					model.setTBondSelectionSet(bs);
					return bs;
				}
			}
			else {
				out(ScriptEvent.FAILED, "Unrecognized expression: " + str);
				return null;
			}
		}
		if (selectFromCollection(str, n, bs)) {
			model.setTBondSelectionSet(bs);
			return bs;
		}
		out(ScriptEvent.FAILED, "Unrecognized expression: " + str);
		return null;
	}

	private boolean selectTbondsInvolving(String str, BitSet bs) {
		if (RANGE_LEADING.matcher(str).find()) {
			String[] s = str.split("-");
			int beg = Float.valueOf(s[0].trim()).intValue();
			int end = Float.valueOf(s[1].trim()).intValue();
			synchronized (model.tBonds) {
				for (TBond tb : model.tBonds) {
					if (inRangeInclusive(tb.getAtom1().getIndex(), beg, end)
							|| inRangeInclusive(tb.getAtom2().getIndex(), beg, end)
							|| inRangeInclusive(tb.getAtom3().getIndex(), beg, end)
							|| inRangeInclusive(tb.getAtom4().getIndex(), beg, end)) {
						bs.set(model.tBonds.indexOf(tb));
					}
				}
			}
			return true;
		}
		if (INTEGER_GROUP.matcher(str).find()) {
			String[] s = str.split(REGEX_SEPARATOR + "+");
			int index;
			for (int m = 0; m < s.length; m++) {
				index = Float.valueOf(s[m]).intValue();
				synchronized (model.tBonds) {
					for (TBond tb : model.tBonds) {
						if (tb.contains(index))
							bs.set(model.tBonds.indexOf(tb));
					}
				}
			}
			return true;
		}
		if (INDEX.matcher(str).find()) {
			int index = Float.valueOf(str.trim()).intValue();
			synchronized (model.tBonds) {
				for (TBond tb : model.tBonds) {
					if (tb.contains(index))
						bs.set(model.tBonds.indexOf(tb));
				}
			}
			return true;
		}
		str = str.trim();
		if (str.length() > 0) {
			String[] s = str.split(REGEX_SEPARATOR + "+");
			for (int i = 0; i < s.length; i++) {
				s[i] = standardizeElementName(s[i]);
				Object o = model.paramMap.get(s[i]);
				if (o != null) {
					synchronized (model.tBonds) {
						for (TBond tb : model.tBonds) {
							if (tb.containsElement(s[i])) {
								bs.set(model.tBonds.indexOf(tb));
							}
						}
					}
				}
			}
			return true;
		}
		return false;
	}

	private BitSet selectMolecules(String str) {
		if (model.molecules == null)
			return null;
		int n = model.molecules.size();
		if (n <= 0)
			return null;
		BitSet bs = new BitSet(n);
		if ("selected".equalsIgnoreCase(str)) {
			synchronized (model.molecules) {
				for (int i = 0; i < n; i++) {
					if (model.molecules.get(i).isSelected())
						bs.set(i);
				}
			}
			return bs;
		}
		if (selectFromCollection(str, n, bs)) {
			model.setMoleculeSelectionSet(bs);
			return bs;
		}
		out(ScriptEvent.FAILED, "Unrecognized expression: " + str);
		return null;
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
					bs = selectRBonds(s);
					break;
				case BY_ABOND:
					bs = selectABonds(s);
					break;
				case BY_TBOND:
					bs = selectTBonds(s);
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
			model.setAtomSelectionSet(bs);
			break;
		case BY_RBOND:
			model.setRBondSelectionSet(bs);
			break;
		case BY_ABOND:
			model.setABondSelectionSet(bs);
			break;
		case BY_TBOND:
			model.setTBondSelectionSet(bs);
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

	private float[] parseCoordinates(String str) {
		return parseArray(3, str);
	}

	private static String standardizeElementName(String str) {
		str = str.toLowerCase();
		char[] array = str.toCharArray();
		array[0] = Character.toUpperCase(array[0]);
		str = new String(array);
		return str;
	}

}