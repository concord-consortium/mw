/*
 *   Copyright (C) 2008  The Concord Consortium, Inc.,
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

package org.concord.mw2d.models;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.concord.modeler.ConnectionManager;
import org.concord.modeler.draw.FillMode;
import org.concord.modeler.draw.LineStyle;
import org.concord.modeler.draw.Triangle;
import org.concord.modeler.event.ModelEvent;
import org.concord.modeler.event.PageComponentEvent;
import org.concord.modeler.event.ScriptEvent;
import org.concord.modeler.process.DelayModelTimeLoadable;
import org.concord.modeler.script.AbstractEval;
import org.concord.modeler.script.Compiler;
import org.concord.modeler.text.XMLCharacterDecoder;
import org.concord.modeler.util.DataQueue;
import org.concord.modeler.util.DataQueueUtilities;
import org.concord.modeler.util.EvaluationException;
import org.concord.modeler.util.FileUtilities;
import org.concord.modeler.util.FloatQueue;
import org.concord.mw2d.AtomisticView;
import org.concord.mw2d.MDView;
import org.concord.mw2d.MesoView;
import org.concord.mw2d.UserAction;

import static java.util.regex.Pattern.compile;
import static org.concord.modeler.script.Compiler.*;

class Eval2D extends AbstractEval {

	private final static byte BY_ATOM = 11;
	private final static byte BY_ELEMENT = 12;
	private final static byte BY_RBOND = 13;
	private final static byte BY_ABOND = 14;
	private final static byte BY_MOLECULE = 15;
	private final static byte BY_OBSTACLE = 16;
	private final static byte BY_IMAGE = 17;
	private final static byte BY_TEXTBOX = 18;
	private final static byte BY_LINE = 19;
	private final static byte BY_RECTANGLE = 20;
	private final static byte BY_ELLIPSE = 21;
	private final static byte BY_TRIANGLE = 22;

	private final static Pattern PCF = compile("(^(?i)pcf\\b){1}");
	private final static Pattern TCF = compile("(^(?i)tcf\\b){1}");
	private final static Pattern MVD = compile("(^(?i)mvd\\b){1}");
	private final static Pattern LAC = compile("(^(?i)lac\\b){1}");
	private final static Pattern LAT = compile("(^(?i)lat\\b){1}");
	private final static Pattern LAP = compile("(^(?i)lap\\b){1}");
	private final static Pattern AVERAGE_POSITION = compile("(^(?i)(averageposition|avpos)\\b){1}");
	private final static Pattern AVERAGE_FORCE = compile("(^(?i)(averageforce|avfor)\\b){1}");
	private final static Pattern LIGHT_SOURCE = compile("(^(?i)lightsource\\b){1}");

	// converters to convert the internal units to normal units.
	private final static float R_CONVERTER = 0.1f;
	private final static float V_CONVERTER = 10000;
	private final static float A_CONVERTER = 0.1f;
	private final static float IR_CONVERTER = 1.0f / R_CONVERTER;
	private final static float IV_CONVERTER = 1.0f / V_CONVERTER;
	private final static float M_CONVERTER = 120;

	private static Map<String, Short> actionIDMap;
	private MDModel model;
	private MDView view;

	public Eval2D(MDModel model, boolean asTask) {
		super();
		this.model = model;
		view = (MDView) model.getView();
		setAsTask(asTask);
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
					model.notifyModelListeners(new ModelEvent(model, ModelEvent.SCRIPT_END));
				if (model.initializationScriptToRun) {
					model.setInitializationScriptToRun(false);
				}
				else {
					notifyChange();
				}
			}
		});
	}

	protected synchronized void out(byte status, String description) {
		if (status == ScriptEvent.FAILED) {
			notifyScriptListener(new ScriptEvent(model, status, "Aborted: " + description));
			stop();
		}
		else {
			notifyScriptListener(new ScriptEvent(model, status, description));
		}
	}

	private String useSystemVariables(String s) {
		int iosp = model.getIndexOfSelectedParticle();
		s = replaceAll(s, "%mouse_x", mouseLocation.x);
		s = replaceAll(s, "%mouse_y", mouseLocation.y);
		s = replaceAll(s, "%keycode", keyCode);
		s = replaceAll(s, "%model_time", model.modelTime);
		s = replaceAll(s, "%temperature", model.getTemperature());
		s = replaceAll(s, "%index_of_selected_particle", iosp);
		s = replaceAll(s, "%number_of_particles", model.getNumberOfParticles());
		s = replaceAll(s, "%number_of_obstacles", model.obstacles.size());
		s = replaceAll(s, "%number_of_images", view.getNumberOfInstances(ImageComponent.class));
		s = replaceAll(s, "%number_of_textboxes", view.getNumberOfInstances(TextBoxComponent.class));
		s = replaceAll(s, "%number_of_lines", view.getNumberOfInstances(LineComponent.class));
		s = replaceAll(s, "%number_of_rectangles", view.getNumberOfInstances(RectangleComponent.class));
		s = replaceAll(s, "%number_of_triangles", view.getNumberOfInstances(TriangleComponent.class));
		s = replaceAll(s, "%number_of_ellipses", view.getNumberOfInstances(EllipseComponent.class));
		s = replaceAll(s, "%width", model.boundary.getView().getBounds().width * R_CONVERTER);
		s = replaceAll(s, "%height", model.boundary.getView().getBounds().height * R_CONVERTER);
		s = replaceAll(s, "%loop_count", iLoop);
		s = replaceAll(s, "%loop_times", nLoop);
		s = replaceAll(s, "%cell_x", model.boundary.x * R_CONVERTER);
		s = replaceAll(s, "%cell_y", model.boundary.y * R_CONVERTER);
		s = replaceAll(s, "%cell_width", model.boundary.width * R_CONVERTER);
		s = replaceAll(s, "%cell_height", model.boundary.height * R_CONVERTER);
		if (model instanceof MolecularModel) {
			s = replaceAll(s, "%number_of_atoms", model.getNumberOfParticles());
			s = replaceAll(s, "%number_of_rbonds", ((MolecularModel) model).bonds.size());
			s = replaceAll(s, "%number_of_abonds", ((MolecularModel) model).bends.size());
			s = replaceAll(s, "%number_of_molecules", ((MolecularModel) model).molecules.size());
			s = replaceAll(s, "%index_of_selected_atom", iosp);
		}
		return s;
	}

	private String useElementVariables(String s) {
		if (!(model instanceof AtomicModel))
			return s;
		int lb = s.indexOf("%element[");
		int rb = s.indexOf("].", lb);
		int lb0 = -1;
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
			Element e = ((AtomicModel) model).getElement(i);
			s = replaceAll(s, "%element\\[" + v + "\\]\\.mass", e.getMass() * M_CONVERTER);
			s = replaceAll(s, "%element\\[" + v + "\\]\\.sigma", e.getSigma() * R_CONVERTER);
			s = replaceAll(s, "%element\\[" + v + "\\]\\.epsilon", e.getEpsilon());
			lb0 = lb;
			lb = s.indexOf("%element[");
			if (lb0 == lb) // infinite loop
				break;
			rb = s.indexOf("].", lb);
		}
		return s;
	}

	private String useParticleVariables(String s, int frame) {
		if (frame >= model.getTapePointer()) {
			out(ScriptEvent.FAILED, "There is no such frame: " + frame + ". (Total frames: " + model.getTapePointer()
					+ ".)");
			return null;
		}
		int n = model.getNumberOfParticles();
		int lb = s.indexOf("%particle[");
		int rb = s.indexOf("].", lb);
		int lb0 = -1;
		String v;
		int i;
		while (lb != -1 && rb != -1) {
			v = s.substring(lb + 10, rb);
			double x = parseMathExpression(v);
			if (Double.isNaN(x))
				break;
			i = (int) Math.round(x);
			if (i < 0 || i >= n) {
				out(ScriptEvent.FAILED, i + " is an invalid index: must be between 0 and " + (n - 1) + " (inclusive).");
				break;
			}
			v = escapeMetaCharacters(v);
			if (model instanceof MesoModel) {
				GayBerneParticle p = ((MesoModel) model).gb[i];
				s = replaceAll(s, "%particle\\[" + v + "\\]\\.charge", p.charge);
				s = replaceAll(s, "%particle\\[" + v + "\\]\\.friction", p.friction);
				s = replaceAll(s, "%particle\\[" + v + "\\]\\.restraint", p.restraint != null ? p.restraint.k * 100 : 0);
				s = replaceAll(s, "%particle\\[" + v + "\\]\\.mass", p.mass * M_CONVERTER);
				s = replaceAll(s, "%particle\\[" + v + "\\]\\.hx", p.hx * IR_CONVERTER);
				s = replaceAll(s, "%particle\\[" + v + "\\]\\.hy", p.hy * IR_CONVERTER);
				s = replaceAll(s, "%particle\\[" + v + "\\]\\.gamma", p.gamma);
				s = replaceAll(s, "%particle\\[" + v + "\\]\\.custom", p.custom);
				if (frame < 0) {
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.rx", p.rx * R_CONVERTER);
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.ry", p.ry * R_CONVERTER);
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.vx", p.vx * V_CONVERTER);
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.vy", p.vy * V_CONVERTER);
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.ax", p.ax * A_CONVERTER);
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.ay", p.ay * A_CONVERTER);
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.theta", p.theta);
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.omega", p.omega);
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.alpha", p.alpha);
				}
				else {
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.rx", p.rQ.getQueue1().getData(frame) * R_CONVERTER);
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.ry", p.rQ.getQueue2().getData(frame) * R_CONVERTER);
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.vx", p.vQ.getQueue1().getData(frame) * V_CONVERTER);
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.vy", p.vQ.getQueue2().getData(frame) * V_CONVERTER);
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.ax", p.aQ.getQueue1().getData(frame) * A_CONVERTER);
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.ay", p.aQ.getQueue2().getData(frame) * A_CONVERTER);
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.theta", p.thetaQ.getData(frame));
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.omega", p.omegaQ.getData(frame));
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.alpha", p.alphaQ.getData(frame));
				}
			}
			else if (model instanceof AtomicModel) {
				Atom a = ((AtomicModel) model).atom[i];
				s = replaceAll(s, "%particle\\[" + v + "\\]\\.charge", a.charge);
				s = replaceAll(s, "%particle\\[" + v + "\\]\\.friction", a.friction);
				s = replaceAll(s, "%particle\\[" + v + "\\]\\.restraint", a.restraint != null ? a.restraint.k * 100 : 0);
				s = replaceAll(s, "%particle\\[" + v + "\\]\\.mass", a.mass * M_CONVERTER);
				s = replaceAll(s, "%particle\\[" + v + "\\]\\.hx", a.hx * IR_CONVERTER);
				s = replaceAll(s, "%particle\\[" + v + "\\]\\.hy", a.hy * IR_CONVERTER);
				s = replaceAll(s, "%particle\\[" + v + "\\]\\.custom", a.custom);
				if (frame < 0) {
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.rx", a.rx * R_CONVERTER);
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.ry", a.ry * R_CONVERTER);
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.vx", a.vx * V_CONVERTER);
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.vy", a.vy * V_CONVERTER);
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.ax", a.ax * A_CONVERTER);
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.ay", a.ay * A_CONVERTER);
				}
				else {
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.rx", a.rQ.getQueue1().getData(frame) * R_CONVERTER);
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.ry", a.rQ.getQueue2().getData(frame) * R_CONVERTER);
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.vx", a.vQ.getQueue1().getData(frame) * V_CONVERTER);
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.vy", a.vQ.getQueue2().getData(frame) * V_CONVERTER);
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.ax", a.aQ.getQueue1().getData(frame) * A_CONVERTER);
					s = replaceAll(s, "%particle\\[" + v + "\\]\\.ay", a.aQ.getQueue2().getData(frame) * A_CONVERTER);
				}
			}
			lb0 = lb;
			lb = s.indexOf("%particle[");
			if (lb0 == lb) // infinite loop
				break;
			rb = s.indexOf("].", lb);
		}
		if (model instanceof AtomicModel) {
			lb = s.indexOf("%atom[");
			rb = s.indexOf("].", lb);
			while (lb != -1 && rb != -1) {
				v = s.substring(lb + 6, rb);
				double x = parseMathExpression(v);
				if (Double.isNaN(x))
					break;
				i = (int) Math.round(x);
				if (i < 0 || i >= n) {
					out(ScriptEvent.FAILED, i + " is an invalid index: must be between 0 and " + (n - 1)
							+ " (inclusive).");
					break;
				}
				v = escapeMetaCharacters(v);
				Atom a = ((AtomicModel) model).atom[i];
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.id", a.id);
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.mass", a.mass * M_CONVERTER);
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.charge", a.charge);
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.sigma", a.sigma * 0.1);
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.epsilon", a.epsilon);
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.friction", a.friction);
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.restraint", a.restraint != null ? a.restraint.k * 100 : 0);
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.hx", a.hx * IR_CONVERTER);
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.hy", a.hy * IR_CONVERTER);
				s = replaceAll(s, "%atom\\[" + v + "\\]\\.custom", a.custom);
				if (frame < 0) {
					s = replaceAll(s, "%atom\\[" + v + "\\]\\.rx", a.rx * R_CONVERTER);
					s = replaceAll(s, "%atom\\[" + v + "\\]\\.ry", a.ry * R_CONVERTER);
					s = replaceAll(s, "%atom\\[" + v + "\\]\\.vx", a.vx * V_CONVERTER);
					s = replaceAll(s, "%atom\\[" + v + "\\]\\.vy", a.vy * V_CONVERTER);
					s = replaceAll(s, "%atom\\[" + v + "\\]\\.ax", a.ax * A_CONVERTER);
					s = replaceAll(s, "%atom\\[" + v + "\\]\\.ay", a.ay * A_CONVERTER);
				}
				else {
					s = replaceAll(s, "%atom\\[" + v + "\\]\\.rx", a.rQ.getQueue1().getData(frame) * R_CONVERTER);
					s = replaceAll(s, "%atom\\[" + v + "\\]\\.ry", a.rQ.getQueue2().getData(frame) * R_CONVERTER);
					s = replaceAll(s, "%atom\\[" + v + "\\]\\.vx", a.vQ.getQueue1().getData(frame) * V_CONVERTER);
					s = replaceAll(s, "%atom\\[" + v + "\\]\\.vy", a.vQ.getQueue2().getData(frame) * V_CONVERTER);
					s = replaceAll(s, "%atom\\[" + v + "\\]\\.ax", a.aQ.getQueue1().getData(frame) * A_CONVERTER);
					s = replaceAll(s, "%atom\\[" + v + "\\]\\.ay", a.aQ.getQueue2().getData(frame) * A_CONVERTER);
				}
				lb0 = lb;
				lb = s.indexOf("%atom[");
				if (lb0 == lb) // infinite loop
					break;
				rb = s.indexOf("].", lb);
			}
		}
		return s;
	}

	private String useRbondVariables(String s, int frame) {
		if (!(model instanceof MolecularModel))
			return s;
		MolecularModel m = (MolecularModel) model;
		int n = m.bonds.size();
		if (n <= 0)
			return s;
		int lb = s.indexOf("%rbond[");
		int rb = s.indexOf("].", lb);
		int lb0 = -1;
		String v;
		int i;
		RadialBond bond;
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
			bond = m.bonds.get(i);
			s = replaceAll(s, "%rbond\\[" + v + "\\]\\.length", bond.getLength(frame) * R_CONVERTER);
			s = replaceAll(s, "%rbond\\[" + v + "\\]\\.strength", bond.getBondStrength());
			s = replaceAll(s, "%rbond\\[" + v + "\\]\\.bondlength", bond.getBondLength() * R_CONVERTER);
			s = replaceAll(s, "%rbond\\[" + v + "\\]\\.atom1", bond.atom1.getIndex());
			s = replaceAll(s, "%rbond\\[" + v + "\\]\\.atom2", bond.atom2.getIndex());
			s = replaceAll(s, "%rbond\\[" + v + "\\]\\.custom", bond.custom);
			lb0 = lb;
			lb = s.indexOf("%rbond[");
			if (lb0 == lb) // infinite loop
				break;
			rb = s.indexOf("].", lb);
		}
		return s;
	}

	private String useAbondVariables(String s, int frame) {
		if (!(model instanceof MolecularModel))
			return s;
		MolecularModel m = (MolecularModel) model;
		int n = m.bends.size();
		if (n <= 0)
			return s;
		int lb = s.indexOf("%abond[");
		int rb = s.indexOf("].", lb);
		int lb0 = -1;
		String v;
		int i;
		AngularBond bond;
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
			bond = m.bends.get(i);
			s = replaceAll(s, "%abond\\[" + v + "\\]\\.angle", bond.getAngle(frame));
			s = replaceAll(s, "%abond\\[" + v + "\\]\\.strength", bond.getBondStrength());
			s = replaceAll(s, "%abond\\[" + v + "\\]\\.bondangle", bond.getBondAngle());
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

	private String useObstacleVariables(String s, int frame) {
		if (!(model instanceof AtomicModel))
			return s;
		AtomicModel m = (AtomicModel) model;
		if (m.obstacles == null)
			return s;
		int n = m.obstacles.size();
		if (n <= 0)
			return s;
		int lb = s.indexOf("%obstacle[");
		int rb = s.indexOf("].", lb);
		int lb0 = -1;
		String v;
		int i;
		RectangularObstacle o;
		while (lb != -1 && rb != -1) {
			v = s.substring(lb + 10, rb);
			double x = parseMathExpression(v);
			if (Double.isNaN(x))
				break;
			i = (int) Math.round(x);
			if (i < 0 || i >= n) {
				out(ScriptEvent.FAILED, "Obstacle " + i + " does not exist.");
				break;
			}
			v = escapeMetaCharacters(v);
			o = m.obstacles.get(i);
			if (frame < 0) {
				s = replaceAll(s, "%obstacle\\[" + v + "\\]\\.x", o.x * R_CONVERTER);
				s = replaceAll(s, "%obstacle\\[" + v + "\\]\\.y", o.y * R_CONVERTER);
				s = replaceAll(s, "%obstacle\\[" + v + "\\]\\.vx", o.vx * V_CONVERTER);
				s = replaceAll(s, "%obstacle\\[" + v + "\\]\\.vy", o.vy * V_CONVERTER);
			}
			else {
				s = replaceAll(s, "%obstacle\\[" + v + "\\]\\.x", o.rxryQ.getQueue1().getData(frame) * R_CONVERTER);
				s = replaceAll(s, "%obstacle\\[" + v + "\\]\\.y", o.rxryQ.getQueue2().getData(frame) * R_CONVERTER);
				s = replaceAll(s, "%obstacle\\[" + v + "\\]\\.vx", o.vxvyQ.getQueue1().getData(frame) * V_CONVERTER);
				s = replaceAll(s, "%obstacle\\[" + v + "\\]\\.vy", o.vxvyQ.getQueue2().getData(frame) * V_CONVERTER);
			}
			s = replaceAll(s, "%obstacle\\[" + v + "\\]\\.width", o.width * R_CONVERTER);
			s = replaceAll(s, "%obstacle\\[" + v + "\\]\\.height", o.height * R_CONVERTER);
			s = replaceAll(s, "%obstacle\\[" + v + "\\]\\.friction", o.friction);
			s = replaceAll(s, "%obstacle\\[" + v + "\\]\\.elasticity", o.elasticity);
			s = replaceAll(s, "%obstacle\\[" + v + "\\]\\.hx", o.getHx() * 1000);
			s = replaceAll(s, "%obstacle\\[" + v + "\\]\\.hy", o.getHy() * 1000);
			s = replaceAll(s, "%obstacle\\[" + v + "\\]\\.externalfx", o.getHx() * 1000); // deprecated
			s = replaceAll(s, "%obstacle\\[" + v + "\\]\\.externalfy", o.getHy() * 1000); // deprecated
			s = replaceAll(s, "%obstacle\\[" + v + "\\]\\.custom", o.custom);
			lb0 = lb;
			lb = s.indexOf("%obstacle[");
			if (lb0 == lb) // infinite loop
				break;
			rb = s.indexOf("].", lb);
		}
		return s;
	}

	private String useMoleculeVariables(String s, int frame) {
		if (!(model instanceof MolecularModel))
			return s;
		MolecularModel m = (MolecularModel) model;
		if (m.molecules == null)
			return s;
		int n = m.molecules.size();
		if (n <= 0)
			return s;
		int lb = s.indexOf("%molecule[");
		int rb = s.indexOf("].", lb);
		int lb0 = -1;
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
			mol = m.molecules.get(i);
			int nmol = mol.size();
			if (frame < 0) {
				Point2D com = mol.getCenterOfMass2D();
				s = replaceAll(s, "%molecule\\[" + v + "\\]\\.x", com.getX() * R_CONVERTER);
				s = replaceAll(s, "%molecule\\[" + v + "\\]\\.y", com.getY() * R_CONVERTER);
			}
			else {
				double xc = 0, yc = 0;
				Atom at = null;
				for (int k = 0; k < nmol; k++) {
					at = mol.getAtom(k);
					xc += at.rQ.getQueue1().getData(frame);
					yc += at.rQ.getQueue2().getData(frame);
				}
				xc /= nmol;
				yc /= nmol;
				s = replaceAll(s, "%molecule\\[" + v + "\\]\\.x", xc * R_CONVERTER);
				s = replaceAll(s, "%molecule\\[" + v + "\\]\\.y", yc * R_CONVERTER);
			}
			s = replaceAll(s, "%molecule\\[" + v + "\\]\\.n", nmol);
			lb0 = lb;
			lb = s.indexOf("%molecule[");
			if (lb0 == lb) // infinite loop
				break;
			rb = s.indexOf("].", lb);
		}
		return s;
	}

	private String useImageVariables(String s) {
		int n = view.getNumberOfInstances(ImageComponent.class);
		if (n <= 0)
			return s;
		int lb = s.indexOf("%image[");
		int rb = s.indexOf("].", lb);
		int lb0 = -1;
		String v;
		int i;
		ImageComponent ic;
		while (lb != -1 && rb != -1) {
			v = s.substring(lb + 7, rb);
			double x = parseMathExpression(v);
			if (Double.isNaN(x))
				break;
			i = (int) Math.round(x);
			if (i < 0 || i >= n) {
				out(ScriptEvent.FAILED, "Image " + i + " does not exist.");
				break;
			}
			v = escapeMetaCharacters(v);
			ic = view.getImage(i);
			s = replaceAll(s, "%image\\[" + v + "\\]\\.x", ic.getRx() * R_CONVERTER);
			s = replaceAll(s, "%image\\[" + v + "\\]\\.y", ic.getRy() * R_CONVERTER);
			s = replaceAll(s, "%image\\[" + v + "\\]\\.angle", Math.toDegrees(ic.getAngle()));
			s = replaceAll(s, "%image\\[" + v + "\\]\\.width", ic.getWidth() * R_CONVERTER);
			s = replaceAll(s, "%image\\[" + v + "\\]\\.height", ic.getHeight() * R_CONVERTER);
			s = replaceAll(s, "%image\\[" + v + "\\]\\.custom", ic.custom);
			lb0 = lb;
			lb = s.indexOf("%image[");
			if (lb0 == lb) // infinite loop
				break;
			rb = s.indexOf("].", lb);
		}
		return s;
	}

	private String useLineVariables(String s) {
		int n = view.getNumberOfInstances(LineComponent.class);
		if (n <= 0)
			return s;
		int lb = s.indexOf("%line[");
		int rb = s.indexOf("].", lb);
		int lb0 = -1;
		String v;
		int i;
		LineComponent line;
		while (lb != -1 && rb != -1) {
			v = s.substring(lb + 6, rb);
			double x = parseMathExpression(v);
			if (Double.isNaN(x))
				break;
			i = (int) Math.round(x);
			if (i < 0 || i >= n) {
				out(ScriptEvent.FAILED, "Line " + i + " does not exist.");
				break;
			}
			v = escapeMetaCharacters(v);
			line = view.getLine(i);
			s = replaceAll(s, "%line\\[" + v + "\\]\\.x1", line.getX1() * R_CONVERTER);
			s = replaceAll(s, "%line\\[" + v + "\\]\\.y1", line.getY1() * R_CONVERTER);
			s = replaceAll(s, "%line\\[" + v + "\\]\\.x2", line.getX2() * R_CONVERTER);
			s = replaceAll(s, "%line\\[" + v + "\\]\\.y2", line.getY2() * R_CONVERTER);
			lb0 = lb;
			lb = s.indexOf("%line[");
			if (lb0 == lb) // infinite loop
				break;
			rb = s.indexOf("].", lb);
		}
		return s;
	}

	private String useRectangleVariables(String s) {
		int n = view.getNumberOfInstances(RectangleComponent.class);
		if (n <= 0)
			return s;
		int lb = s.indexOf("%rectangle[");
		int rb = s.indexOf("].", lb);
		int lb0 = -1;
		String v;
		int i;
		RectangleComponent rect;
		while (lb != -1 && rb != -1) {
			v = s.substring(lb + 11, rb);
			double x = parseMathExpression(v);
			if (Double.isNaN(x))
				break;
			i = (int) Math.round(x);
			if (i < 0 || i >= n) {
				out(ScriptEvent.FAILED, "Rectangle " + i + " does not exist.");
				break;
			}
			v = escapeMetaCharacters(v);
			rect = view.getRectangle(i);
			s = replaceAll(s, "%rectangle\\[" + v + "\\]\\.x", rect.getX() * R_CONVERTER);
			s = replaceAll(s, "%rectangle\\[" + v + "\\]\\.y", rect.getY() * R_CONVERTER);
			s = replaceAll(s, "%rectangle\\[" + v + "\\]\\.width", rect.getWidth() * R_CONVERTER);
			s = replaceAll(s, "%rectangle\\[" + v + "\\]\\.height", rect.getHeight() * R_CONVERTER);
			s = replaceAll(s, "%rectangle\\[" + v + "\\]\\.angle", rect.getAngle());
			s = replaceAll(s, "%rectangle\\[" + v + "\\]\\.particlecount", rect.getParticleCount());
			VectorField vf = rect.getVectorField();
			if (vf instanceof ElectricField)
				s = replaceAll(s, "%rectangle\\[" + v + "\\]\\.efield", vf.getIntensity());
			else if (vf instanceof MagneticField)
				s = replaceAll(s, "%rectangle\\[" + v + "\\]\\.bfield", vf.getIntensity());
			else {
				s = replaceAll(s, "%rectangle\\[" + v + "\\]\\.efield", 0);
				s = replaceAll(s, "%rectangle\\[" + v + "\\]\\.bfield", 0);
			}
			lb0 = lb;
			lb = s.indexOf("%rectangle[");
			if (lb0 == lb) // infinite loop
				break;
			rb = s.indexOf("].", lb);
		}
		return s;
	}

	private String useTriangleVariables(String s) {
		int n = view.getNumberOfInstances(TriangleComponent.class);
		if (n <= 0)
			return s;
		int lb = s.indexOf("%triangle[");
		int rb = s.indexOf("].", lb);
		int lb0 = -1;
		String v;
		int i;
		TriangleComponent triangle;
		while (lb != -1 && rb != -1) {
			v = s.substring(lb + 10, rb);
			double x = parseMathExpression(v);
			if (Double.isNaN(x))
				break;
			i = (int) Math.round(x);
			if (i < 0 || i >= n) {
				out(ScriptEvent.FAILED, "Triangle " + i + " does not exist.");
				break;
			}
			v = escapeMetaCharacters(v);
			triangle = view.getTriangle(i);
			s = replaceAll(s, "%triangle\\[" + v + "\\]\\.x1", triangle.getVertex(0).x * R_CONVERTER);
			s = replaceAll(s, "%triangle\\[" + v + "\\]\\.y1", triangle.getVertex(0).y * R_CONVERTER);
			s = replaceAll(s, "%triangle\\[" + v + "\\]\\.x2", triangle.getVertex(1).x * R_CONVERTER);
			s = replaceAll(s, "%triangle\\[" + v + "\\]\\.y2", triangle.getVertex(1).y * R_CONVERTER);
			s = replaceAll(s, "%triangle\\[" + v + "\\]\\.x3", triangle.getVertex(2).x * R_CONVERTER);
			s = replaceAll(s, "%triangle\\[" + v + "\\]\\.y3", triangle.getVertex(2).y * R_CONVERTER);
			s = replaceAll(s, "%triangle\\[" + v + "\\]\\.particlecount", triangle.getParticleCount());
			VectorField vf = triangle.getVectorField();
			if (vf instanceof ElectricField)
				s = replaceAll(s, "%triangle\\[" + v + "\\]\\.efield", vf.getIntensity());
			else if (vf instanceof MagneticField)
				s = replaceAll(s, "%triangle\\[" + v + "\\]\\.bfield", vf.getIntensity());
			else {
				s = replaceAll(s, "%triangle\\[" + v + "\\]\\.efield", 0);
				s = replaceAll(s, "%triangle\\[" + v + "\\]\\.bfield", 0);
			}
			lb0 = lb;
			lb = s.indexOf("%triangle[");
			if (lb0 == lb) // infinite loop
				break;
			rb = s.indexOf("].", lb);
		}
		return s;
	}

	private String useEllipseVariables(String s) {
		int n = view.getNumberOfInstances(EllipseComponent.class);
		if (n <= 0)
			return s;
		int lb = s.indexOf("%ellipse[");
		int rb = s.indexOf("].", lb);
		int lb0 = -1;
		String v;
		int i;
		EllipseComponent ellipse;
		while (lb != -1 && rb != -1) {
			v = s.substring(lb + 9, rb);
			double x = parseMathExpression(v);
			if (Double.isNaN(x))
				break;
			i = (int) Math.round(x);
			if (i < 0 || i >= n) {
				out(ScriptEvent.FAILED, "Ellipse " + i + " does not exist.");
				break;
			}
			v = escapeMetaCharacters(v);
			ellipse = view.getEllipse(i);
			s = replaceAll(s, "%ellipse\\[" + v + "\\]\\.x", ellipse.getX() * R_CONVERTER);
			s = replaceAll(s, "%ellipse\\[" + v + "\\]\\.y", ellipse.getY() * R_CONVERTER);
			s = replaceAll(s, "%ellipse\\[" + v + "\\]\\.width", ellipse.getWidth() * R_CONVERTER);
			s = replaceAll(s, "%ellipse\\[" + v + "\\]\\.height", ellipse.getHeight() * R_CONVERTER);
			s = replaceAll(s, "%ellipse\\[" + v + "\\]\\.angle", ellipse.getAngle());
			s = replaceAll(s, "%ellipse\\[" + v + "\\]\\.particlecount", ellipse.getParticleCount());
			VectorField vf = ellipse.getVectorField();
			if (vf instanceof ElectricField)
				s = replaceAll(s, "%ellipse\\[" + v + "\\]\\.efield", vf.getIntensity());
			else if (vf instanceof MagneticField)
				s = replaceAll(s, "%ellipse\\[" + v + "\\]\\.bfield", vf.getIntensity());
			else {
				s = replaceAll(s, "%ellipse\\[" + v + "\\]\\.efield", 0);
				s = replaceAll(s, "%ellipse\\[" + v + "\\]\\.bfield", 0);
			}
			lb0 = lb;
			lb = s.indexOf("%ellipse[");
			if (lb0 == lb) // infinite loop
				break;
			rb = s.indexOf("].", lb);
		}
		return s;
	}

	private String useTextBoxVariables(String s) {
		int n = view.getNumberOfInstances(TextBoxComponent.class);
		if (n <= 0)
			return s;
		int lb = s.indexOf("%textbox[");
		int rb = s.indexOf("].", lb);
		int lb0 = -1;
		String v;
		int i;
		TextBoxComponent text;
		while (lb != -1 && rb != -1) {
			v = s.substring(lb + 9, rb);
			double x = parseMathExpression(v);
			if (Double.isNaN(x))
				break;
			i = (int) Math.round(x);
			if (i < 0 || i >= n) {
				out(ScriptEvent.FAILED, "Text box " + i + " does not exist.");
				break;
			}
			v = escapeMetaCharacters(v);
			text = view.getTextBox(i);
			s = replaceAll(s, "%textbox\\[" + v + "\\]\\.x", text.getRx() * R_CONVERTER);
			s = replaceAll(s, "%textbox\\[" + v + "\\]\\.y", text.getRy() * R_CONVERTER);
			s = replaceAll(s, "%textbox\\[" + v + "\\]\\.angle", text.getAngle());
			lb0 = lb;
			lb = s.indexOf("%textbox[");
			if (lb0 == lb) // infinite loop
				break;
			rb = s.indexOf("].", lb);
		}
		return s;
	}

	protected String useDefinitions(String s) {
		s = useSystemVariables(s);
		s = useElementVariables(s);
		s = useParticleVariables(s, -1);
		s = useObstacleVariables(s, -1);
		s = useMoleculeVariables(s, -1);
		s = useRbondVariables(s, -1);
		s = useAbondVariables(s, -1);
		s = useImageVariables(s);
		s = useTextBoxVariables(s);
		s = useLineVariables(s);
		s = useRectangleVariables(s);
		s = useTriangleVariables(s);
		s = useEllipseVariables(s);
		s = super.useDefinitions(s);
		// user-definition should go last (e.g. for set %model_time 0; not to override system variables)
		return s;
	}

	/*
	 * the thread that calls this method goes to "wait" until it is notified. When it returns, it will evaluate the
	 * script. If you want to do a different script, pass in through the script setter. The "stop" flag indicates the
	 * end of executing the current script.
	 */
	void evaluate() throws InterruptedException {
		while (true) {
			evaluate2();
			synchronized (this) {
				wait();
			}
		}
	}

	void evaluate2() throws InterruptedException {
		if (!getAsTask()) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					model.notifyModelListeners(new ModelEvent(model, ModelEvent.SCRIPT_START));
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
		script = storeKeyScripts(script);
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
			if (view instanceof AtomisticView) {
				AtomisticView av = (AtomisticView) view;
				if (av.getUseJmol()) {
					av.refreshJmol();
					av.repaint();
				}
			}
		}
	}

	protected boolean evalCommand(String ci) throws InterruptedException {

		String ciLC = ci.toLowerCase();

		// skip
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

		// System.out.println("Before using definitions --> " + ci);
		// System.out.println(definition);
		// System.out.println("After using definitions ---> " + useDefinitions(ci));

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
				ex.printStackTrace();
				return false;
			}
		}

		// System.out.println(ci);
		// System.out.println("-------------------------");

		// load
		matcher = LOAD.matcher(ci);
		if (matcher.find()) {
			if (evaluateLoadClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// source
		matcher = SOURCE.matcher(ci);
		if (matcher.find()) {
			if (evaluateSourceClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// select
		matcher = SELECT.matcher(ci);
		if (matcher.find()) {
			if (evaluateSelectClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// background
		matcher = BACKGROUND.matcher(ci);
		if (matcher.find()) {
			if (evaluateBackgroundClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// mark selected objects with a specified color
		matcher = MARK_COLOR.matcher(ci);
		if (matcher.find()) {
			if (evaluateMarkClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// charge
		matcher = CHARGE.matcher(ci);
		if (matcher.find()) {
			if (evaluateChargeClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// restrain
		matcher = RESTRAIN.matcher(ci);
		if (matcher.find()) {
			if (evaluateRestrainClause(ci.substring(matcher.end()).trim()))
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

		// store
		matcher = STORE.matcher(ci);
		if (matcher.find()) {
			if (evaluateStoreClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// averageposition
		matcher = AVERAGE_POSITION.matcher(ci);
		if (matcher.find()) {
			if (evaluateAvposClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// averageforce
		matcher = AVERAGE_FORCE.matcher(ci);
		if (matcher.find()) {
			if (evaluateAvforClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// local area counter
		matcher = LAC.matcher(ci);
		if (matcher.find()) {
			if (evaluateLacClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// local area temperature
		matcher = LAT.matcher(ci);
		if (matcher.find()) {
			if (evaluateLatClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// local area pressure
		matcher = LAP.matcher(ci);
		if (matcher.find()) {
			if (evaluateLapClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// pcf
		matcher = PCF.matcher(ci);
		if (matcher.find()) {
			if (evaluatePcfClause(ci.substring(matcher.end()).trim().toLowerCase()))
				return true;
		}

		// tcf
		matcher = TCF.matcher(ci);
		if (matcher.find()) {
			if (evaluateTcfClause(ci.substring(matcher.end()).trim().toLowerCase()))
				return true;
		}

		// mvd
		matcher = MVD.matcher(ci);
		if (matcher.find()) {
			if (evaluateMvdClause(ci.substring(matcher.end()).trim().toLowerCase()))
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

		// attach
		matcher = ATTACH.matcher(ci);
		if (matcher.find()) {
			if (evaluateAttachClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// sound
		matcher = SOUND.matcher(ci);
		if (matcher.find()) {
			if (evaluateSoundClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// mouse cursor
		matcher = CURSOR.matcher(ci);
		if (matcher.find()) {
			if (evaluateCursorClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// move
		matcher = MOVE.matcher(ci);
		if (matcher.find()) {
			if (evaluateMoveClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// rotate
		matcher = ROTATE.matcher(ci);
		if (matcher.find()) {
			if (evaluateRotateClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// delay
		matcher = DELAY.matcher(ci);
		if (matcher.find()) {
			if (evaluateDelayClause(ci.substring(matcher.end()).trim()))
				return true;
		}

		// print
		matcher = PRINT.matcher(ci);
		if (matcher.find()) {
			if (evaluatePrintClause(ci.substring(matcher.end()).trim().toLowerCase()))
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
					String codeBase = FileUtilities.getCodeBase((String) model.getProperty("url"));
					showMessageDialog(info2, codeBase, view);
				}
			});
			return true;
		}

		if (model instanceof MolecularModel) {

			// minimize
			matcher = MINIMIZE.matcher(ci);
			if (matcher.find()) {
				if (evaluateMinimizeClause(ci.substring(matcher.end()).trim()))
					return true;
			}

			// build radial bond
			matcher = BUILD_RBOND.matcher(ci);
			if (matcher.find()) {
				if (evaluateBuildBondClause(ci.substring(ci.startsWith("rbond") ? 5 : 4).trim()))
					return true;
			}

			// build angular bond
			matcher = BUILD_ABOND.matcher(ci);
			if (matcher.find()) {
				if (evaluateBuildBendClause(ci.substring(ci.startsWith("abond") ? 5 : 4).trim()))
					return true;
			}

		}

		out(ScriptEvent.FAILED, "Unrecognized command: " + ci);
		return false;

	}

	private void notifyChange() {
		if (getNotifySaver())
			model.notifyChange();
	}

	protected boolean evaluateSingleKeyword(String str) throws InterruptedException {
		if (super.evaluateSingleKeyword(str))
			return true;
		String strLC = str.toLowerCase();
		if ("paint".equals(strLC)) { // paint
			model.computeForce(-1);
			view.repaint();
			return true;
		}
		if ("snapshot".equals(strLC)) { // snapshot
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					model.notifyPageComponentListeners(new PageComponentEvent(view, PageComponentEvent.SNAPSHOT_TAKEN));
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
			// must be invoked later for it to work properly, as it does from a button
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					model.run();
				}
			});
			return true;
		}
		if (strLC.startsWith("stop")) {
			if ("stop".equals(strLC)) { // stop
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						model.stop();
					}
				});
				return true;
			}
			if ("immediately".equals(strLC.substring(4).trim())) { // stop immediately
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						model.stopImmediately();
					}
				});
				return true;
			}
		}
		if (strLC.startsWith("reset")) {
			if ("reset".equals(strLC)) { // reset
				copyMouseAndKeyScripts();
				evaluateLoadClause((String) model.getProperty("url"));
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						readdMouseAndKeyScripts();
						model.notifyModelListeners(new ModelEvent(model, ModelEvent.MODEL_RESET));
						model.notifyPageComponentListeners(new PageComponentEvent(model,
								PageComponentEvent.COMPONENT_RESET));
					}
				});
				return true;
			}
			if ("silently".equals(strLC.substring(5).trim())) { // reset silently
				evaluateLoadClause((String) model.getProperty("url"));
				return true;
			}
		}
		if ("undo".equals(strLC)) { // undo
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					if (model.getUndoManager().canUndo()) {
						try {
							model.getUndoManager().undo();
						}
						catch (CannotUndoException ex) {
							ex.printStackTrace();
						}
					}
				}
			});
			return true;
		}
		if ("redo".equals(strLC)) { // redo
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					if (model.getUndoManager().canRedo()) {
						try {
							model.getUndoManager().redo();
						}
						catch (CannotRedoException ex) {
							ex.printStackTrace();
						}
					}
				}
			});
			return true;
		}
		if ("mark".equals(strLC)) { // mark
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					model.markSelection();
				}
			});
			notifyChange();
			return true;
		}
		if ("remove".equals(strLC)) { // remove selected objects
			// CAUTION!!!!!!!!! PUTTING INTO EVENTQUEUE is dangerous if there are commands following it!
			removeSelectedObjects();
			// EventQueue.invokeLater(new Runnable() { public void run() { removeSelectedObjects(); } });
			notifyChange();
			return true;
		}
		return false;
	}

	private void deselectAll() {
		view.setImageSelectionSet(null);
		view.setLineSelectionSet(null);
		view.setRectangleSelectionSet(null);
		view.setEllipseSelectionSet(null);
		view.setTextBoxSelectionSet(null);
		model.setParticleSelectionSet(null);
		if (model instanceof MolecularModel) {
			MolecularModel mm = (MolecularModel) model;
			mm.bonds.setSelectionSet(null);
			mm.bends.setSelectionSet(null);
			mm.molecules.setSelectionSet(null);
			mm.obstacles.setSelectionSet(null);
		}
	}

	private boolean evaluateSelectClause(String clause) {

		if (clause == null || clause.equals(""))
			return false;

		// "none" clause is a special case for clearing the selection status of all types
		if ("none".equalsIgnoreCase(clause)) {
			deselectAll();
			return true;
		}

		if (!NOT_SELECTED.matcher(clause).find()) { // "not selected" clause is a special case.
			deselectAll();
		}

		Matcher matcher = IMAGE.matcher(clause); // select by image
		if (matcher.find()) {
			String str = clause.substring(matcher.end()).trim();
			BitSet selection = null;
			if (LOGICAL_OPERATOR.matcher(str).find()) { // logical expressions
				selection = parseLogicalExpression(str, BY_IMAGE);
			}
			else {
				selection = selectImages(str);
			}
			if (!getAsTask())
				out(ScriptEvent.SUCCEEDED, (selection != null ? selection.cardinality() : 0) + " images are selected.");
			return true;
		}

		matcher = TEXTBOX.matcher(clause); // select by text box
		if (matcher.find()) {
			String str = clause.substring(matcher.end()).trim();
			BitSet selection = null;
			if (LOGICAL_OPERATOR.matcher(str).find()) { // logical expressions
				selection = parseLogicalExpression(str, BY_TEXTBOX);
			}
			else {
				selection = selectTextBoxes(str);
			}
			if (!getAsTask())
				out(ScriptEvent.SUCCEEDED, (selection != null ? selection.cardinality() : 0)
						+ " text boxes are selected.");
			return true;
		}

		matcher = LINE.matcher(clause); // select by line
		if (matcher.find()) {
			String str = clause.substring(matcher.end()).trim();
			BitSet selection = null;
			if (LOGICAL_OPERATOR.matcher(str).find()) { // logical expressions
				selection = parseLogicalExpression(str, BY_LINE);
			}
			else {
				selection = selectLines(str);
			}
			if (!getAsTask())
				out(ScriptEvent.SUCCEEDED, (selection != null ? selection.cardinality() : 0) + " lines are selected.");
			return true;
		}

		matcher = RECTANGLE.matcher(clause); // select by rectangle
		if (matcher.find()) {
			String str = clause.substring(matcher.end()).trim();
			BitSet selection = null;
			if (LOGICAL_OPERATOR.matcher(str).find()) { // logical expressions
				selection = parseLogicalExpression(str, BY_RECTANGLE);
			}
			else {
				selection = selectRectangles(str);
			}
			if (!getAsTask())
				out(ScriptEvent.SUCCEEDED, (selection != null ? selection.cardinality() : 0)
						+ " rectangles are selected.");
			return true;
		}

		matcher = TRIANGLE.matcher(clause); // select by triangle
		if (matcher.find()) {
			String str = clause.substring(matcher.end()).trim();
			BitSet selection = null;
			if (LOGICAL_OPERATOR.matcher(str).find()) { // logical expressions
				selection = parseLogicalExpression(str, BY_TRIANGLE);
			}
			else {
				selection = selectTriangles(str);
			}
			if (!getAsTask())
				out(ScriptEvent.SUCCEEDED, (selection != null ? selection.cardinality() : 0)
						+ " triangles are selected.");
			return true;
		}

		matcher = ELLIPSE.matcher(clause); // select by ellipse
		if (matcher.find()) {
			String str = clause.substring(matcher.end()).trim();
			BitSet selection = null;
			if (LOGICAL_OPERATOR.matcher(str).find()) { // logical expressions
				selection = parseLogicalExpression(str, BY_ELLIPSE);
			}
			else {
				selection = selectEllipses(str);
			}
			if (!getAsTask())
				out(ScriptEvent.SUCCEEDED, (selection != null ? selection.cardinality() : 0)
						+ " ellipses are selected.");
			return true;
		}

		matcher = ATOM.matcher(clause); // select by atom
		if (matcher.find()) {
			BitSet selection = null;
			String str = clause.substring(matcher.end()).trim();
			if (LOGICAL_OPERATOR.matcher(str).find()) { // logical expressions
				selection = parseLogicalExpression(str, BY_ATOM);
			}
			else {
				selection = selectParticles(str);
			}
			if (!getAsTask())
				out(ScriptEvent.SUCCEEDED, (selection != null ? selection.cardinality() : 0) + " atoms are selected.");
			return true;
		}

		if (model instanceof MolecularModel) {

			matcher = RBOND.matcher(clause); // select by radial bond
			if (matcher.find()) {
				String str = clause.substring(matcher.end()).trim();
				BitSet selection = null;
				if (LOGICAL_OPERATOR.matcher(str).find()) {// logical expressions
					selection = parseLogicalExpression(str, BY_RBOND);
				}
				else {
					selection = selectRadialBonds(str);
				}
				if (!getAsTask())
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
					selection = selectAngularBonds(str);
				}
				if (!getAsTask())
					out(ScriptEvent.SUCCEEDED, (selection != null ? selection.cardinality() : 0)
							+ " angular bonds are selected.");
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
				if (!getAsTask())
					out(ScriptEvent.SUCCEEDED, (selection != null ? selection.cardinality() : 0)
							+ " molecules are selected.");
				return true;
			}

			matcher = OBSTACLE.matcher(clause); // select by obstacle
			if (matcher.find()) {
				String str = clause.substring(matcher.end()).trim();
				BitSet selection = null;
				if (LOGICAL_OPERATOR.matcher(str).find()) { // logical expressions
					selection = parseLogicalExpression(str, BY_OBSTACLE);
				}
				else {
					selection = selectObstacles(str);
				}
				if (!getAsTask())
					out(ScriptEvent.SUCCEEDED, (selection != null ? selection.cardinality() : 0)
							+ " obstacles are selected.");
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
				if (!getAsTask())
					out(ScriptEvent.SUCCEEDED, (selection != null ? selection.cardinality() : 0)
							+ " atoms are selected.");
				return true;
			}

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
					String base = (String) model.getProperty("url");
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
						e.printStackTrace();
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
		notifyChange();
		return true;
	}

	private boolean evaluateMarkClause(String str) {
		Color c = parseRGBColor(str);
		if (c == null)
			return false;
		model.markSelection(c);
		notifyChange();
		return true;
	}

	private boolean evaluateChargeClause(String str) {
		double x = parseMathExpression(str);
		if (Double.isNaN(x))
			return false;
		int n = model.getNumberOfParticles();
		if (n <= 0)
			return true;
		float c = (float) x;
		for (int k = 0; k < n; k++) {
			Particle p = model.getParticle(k);
			if (p.isSelected())
				p.setCharge(c);
		}
		if (model instanceof AtomicModel)
			((AtomicModel) model).checkCharges();
		view.repaint();
		notifyChange();
		return true;
	}

	private boolean evaluateRestrainClause(String str) {
		double x = parseMathExpression(str);
		if (Double.isNaN(x))
			return false;
		if (x < 0) {
			out(ScriptEvent.FAILED, "Restraint cannot be negative: " + str);
			return false;
		}
		int nop = model.getNumberOfParticles();
		if (nop <= 0)
			return true;
		float c = (float) (x * 0.01);
		for (int k = 0; k < nop; k++) {
			Particle p = model.getParticle(k);
			if (p.isSelected()) {
				if (c < ZERO) {
					p.setRestraint(null);
				}
				else {
					if (p.restraint == null) {
						p.setRestraint(new PointRestraint(c, p.rx, p.ry));
					}
					else {
						p.restraint.k = c;
					}
				}
			}
		}
		view.repaint();
		notifyChange();
		return true;
	}

	private boolean evaluateDampClause(String str) {
		double x = parseMathExpression(str);
		if (x < 0) {
			out(ScriptEvent.FAILED, "Friction cannot be negative: " + str);
			return false;
		}
		int n = model.getNumberOfParticles();
		if (n <= 0)
			return true;
		float c = (float) x;
		for (int k = 0; k < n; k++) {
			Particle p = model.getParticle(k);
			if (p.isSelected())
				p.setFriction(c);
		}
		view.repaint();
		notifyChange();
		return true;
	}

	private boolean evaluateHeatClause(String str) {
		double x = parseMathExpression(str);
		if (Double.isNaN(x))
			return false;
		if (x == 0)
			return true;
		int n = model.getNumberOfParticles();
		if (n <= 0)
			return true;
		List<Particle> list = new ArrayList<Particle>();
		for (int k = 0; k < n; k++) {
			Particle p = model.getParticle(k);
			if (p.isSelected())
				list.add(p);
		}
		model.transferHeatToParticles(list, x);
		view.repaint();
		notifyChange();
		return true;
	}

	private boolean evaluateBuildBondClause(String str) {
		String[] s = str.split(REGEX_SEPARATOR);
		if (s.length != 3)
			return false;
		int n = model.getNumberOfParticles();
		double x = parseMathExpression(s[0]);
		if (Double.isNaN(x))
			return false;
		int i = (int) Math.round(x);
		if (i >= n) {
			out(ScriptEvent.FAILED, "Atom index out of limit: i=" + i + ">=" + n);
			return false;
		}
		x = parseMathExpression(s[1]);
		if (Double.isNaN(x))
			return false;
		int j = (int) Math.round(x);
		if (j >= n) {
			out(ScriptEvent.FAILED, "Atom index out of limit: j=" + j + ">=" + n);
			return false;
		}
		if (j == i) {
			out(ScriptEvent.FAILED, "Cannot build a bond between a pair of identical atoms: i=j=" + i);
			return false;
		}
		x = parseMathExpression(s[2]);
		if (Double.isNaN(x))
			return false;
		MolecularModel mm = (MolecularModel) model;
		Atom at1 = mm.atom[i];
		Atom at2 = mm.atom[j];
		RadialBond rb = mm.bonds.getBond(at1, at2);
		if (rb == null) {
			if (x > ZERO) {
				rb = new RadialBond.Builder(at1, at2).bondLength(Math.sqrt(at1.distanceSquare(at2))).bondStrength(x)
						.build();
				mm.bonds.add(rb);
				MoleculeCollection.sort(mm);
				view.repaint();
			}
		}
		else {
			if (x > ZERO) {
				rb.setBondStrength(x);
			}
			else {
				mm.bonds.remove(rb);
				mm.notifyBondChangeListeners();
				MoleculeCollection.sort(mm);
				view.repaint();
			}
		}
		notifyChange();
		return true;
	}

	private boolean evaluateBuildBendClause(String str) {
		String[] s = str.split(REGEX_SEPARATOR);
		if (s.length != 4)
			return false;
		int n = model.getNumberOfParticles();
		double x = parseMathExpression(s[0]);
		if (Double.isNaN(x))
			return false;
		int i = (int) Math.round(x);
		if (i >= n) {
			out(ScriptEvent.FAILED, "Atom index out of limit: i=" + i + ">=" + n);
			return false;
		}
		x = parseMathExpression(s[1]);
		if (Double.isNaN(x))
			return false;
		int j = (int) Math.round(x);
		if (j >= n) {
			out(ScriptEvent.FAILED, "Atom index out of limit: j=" + j + ">=" + n);
			return false;
		}
		if (j == i) {
			out(ScriptEvent.FAILED, "Cannot build an angular bond for identical atoms: i=j=" + i);
			return false;
		}
		x = parseMathExpression(s[2]);
		if (Double.isNaN(x))
			return false;
		int k = (int) Math.round(x);
		if (k >= n) {
			out(ScriptEvent.FAILED, "Atom index out of limit: k=" + k + ">=" + n);
			return false;
		}
		if (k == i || k == j) {
			out(ScriptEvent.FAILED, "Cannot build an angular bond for identical atoms: " + i + "," + j + "," + k);
			return false;
		}
		x = parseMathExpression(s[3]);
		if (Double.isNaN(x))
			return false;
		MolecularModel mm = (MolecularModel) model;
		Atom at1 = mm.atom[i];
		Atom at2 = mm.atom[j];
		Atom at3 = mm.atom[k];
		if (mm.bonds.getBond(at1, at2) == null || mm.bonds.getBond(at2, at3) == null) {
			out(ScriptEvent.FAILED, "Cannot build an angular bond for atom " + at2.getIndex()
					+ " that has only one radial bond.");
			return false;
		}
		AngularBond ab = mm.bends.getBond(at1, at2, at3);
		if (ab == null) {
			if (x > ZERO) {
				ab = new AngularBond(at1, at3, at2, Math.abs(AngularBond.getAngle(at1, at2, at3)), x);
				mm.bends.add(ab);
				ab.setSelected(true);
			}
		}
		else {
			if (x > ZERO) {
				ab.setBondStrength(x);
				ab.setSelected(true);
			}
			else {
				mm.bends.remove(ab);
			}
		}
		view.repaint();
		notifyChange();
		return true;
	}

	private boolean evaluateMvdClause(String clause) {
		if (model instanceof MesoModel)
			return false;
		if (clause == null || clause.equals(""))
			return false;
		String[] sub = clause.split(REGEX_AND);
		Mvd.Parameter[] par = new Mvd.Parameter[sub.length];
		boolean scalar = false;
		byte id;
		float vmax;
		String direction;
		for (int n = 0; n < sub.length; n++) {
			int i = sub[n].indexOf("within");
			String str = i == -1 ? sub[n] : sub[n].substring(0, i).trim();
			try {
				String[] s = str.split(REGEX_SEPARATOR + "+");
				scalar = Boolean.valueOf(s[0].trim()).booleanValue();
				direction = s[1].trim();
				vmax = Float.valueOf(s[2].trim()).floatValue();
				id = Float.valueOf(s[3].trim()).byteValue();
			}
			catch (Exception e) {
				out(ScriptEvent.FAILED, "Script error at: " + str + "\n" + e);
				return false;
			}
			if (i >= 0) {
				str = sub[n].substring(i).trim();
				Matcher matcher = WITHIN_RECTANGLE.matcher(str);
				if (matcher.find()) {
					Rectangle2D area = getWithinArea(str);
					if (area == null)
						return false;
					par[n] = new Mvd.Parameter(scalar, direction, vmax, id, area);
				}
			}
			else {
				par[n] = new Mvd.Parameter(scalar, direction, vmax, id, model.boundary);
			}
		}
		((AtomicModel) model).showMVD(par);
		return true;
	}

	private boolean evaluateTcfClause(String clause) {
		if (model instanceof MesoModel)
			return false;
		if (clause == null || clause.equals(""))
			return false;
		String[] sub = clause.split(REGEX_AND);
		Tcf.Parameter[] par = new Tcf.Parameter[sub.length];
		String funx, funy;
		byte id;
		short length;
		for (int n = 0; n < sub.length; n++) {
			int i = sub[n].indexOf("within");
			String str = i == -1 ? sub[n] : sub[n].substring(0, i).trim();
			try {
				String[] s = str.split(REGEX_SEPARATOR + "+");
				funx = s[0];
				funy = s[1];
				length = Float.valueOf(s[2].trim()).shortValue();
				id = Float.valueOf(s[3].trim()).byteValue();
			}
			catch (Exception e) {
				out(ScriptEvent.FAILED, "Script error at: " + str + "\n" + e);
				return false;
			}
			if (i >= 0) {
				str = sub[n].substring(i).trim();
				Matcher matcher = WITHIN_RECTANGLE.matcher(str);
				if (matcher.find()) {
					Rectangle2D area = getWithinArea(str);
					if (area == null)
						return false;
					par[n] = new Tcf.Parameter(funx, funy, id, length, area);
				}
			}
			else {
				par[n] = new Tcf.Parameter(funx, funy, id, length, model.boundary);
			}
		}
		((AtomicModel) model).showTCF(par);
		return true;
	}

	private boolean evaluatePcfClause(String clause) {
		if (model instanceof MesoModel)
			return false;
		if (clause == null || clause.equals(""))
			return false;
		String[] sub = clause.split(REGEX_AND);
		Pcf.Parameter[] par = new Pcf.Parameter[sub.length];
		byte id1 = 0, id2 = 0;
		int length = 0;
		for (int n = 0; n < sub.length; n++) {
			int i = sub[n].indexOf("within");
			String str = i == -1 ? sub[n] : sub[n].substring(0, i).trim();
			Matcher matcher = INTEGER_GROUP.matcher(str);
			if (matcher.find()) {
				try {
					String[] s = str.split(REGEX_SEPARATOR + "+");
					id1 = Float.valueOf(s[0].trim()).byteValue();
					id2 = Float.valueOf(s[1].trim()).byteValue();
					length = Math.round((Float.valueOf(s[2].trim()) * IR_CONVERTER));
				}
				catch (Exception e) {
					out(ScriptEvent.FAILED, "Script error at: " + str + "\n" + e);
					return false;
				}
			}
			else {
				out(ScriptEvent.FAILED, "Unrecognized clause: " + clause);
				return false;
			}
			if (i >= 0) {
				str = sub[n].substring(i).trim();
				matcher = WITHIN_RECTANGLE.matcher(str);
				if (matcher.find()) {
					Rectangle2D area = getWithinArea(str);
					if (area == null)
						return false;
					par[n] = new Pcf.Parameter(id1, id2, length, area);
				}
			}
			else {
				par[n] = new Pcf.Parameter(id1, id2, length, model.boundary);
			}
		}
		((AtomicModel) model).showPCF(par);
		return true;
	}

	private boolean evaluateLacClause(final String clause) {
		if (model instanceof MesoModel)
			return false;
		if (clause == null || clause.equals(""))
			return false;
		String[] sub = clause.split(REGEX_AND);
		TimeSeriesGenerator.Parameter[] par = new TimeSeriesGenerator.Parameter[sub.length];
		for (int n = 0; n < sub.length; n++) {
			int iw = sub[n].toLowerCase().indexOf("within");
			Rectangle2D area = getWithinArea(sub[n].substring(iw).trim());
			if (area == null)
				return false;
			byte[] elements = getElements(sub[n].substring(0, iw - 1));
			if (elements == null)
				return false;
			par[n] = new TimeSeriesGenerator.Parameter(TimeSeriesGenerator.LAC, elements, area);
		}
		if (model.getTapePointer() > 0) {
			((AtomicModel) model).showTimeSeries(par);
		}
		else {
			short[] count = new short[par.length];
			boolean b = false;
			for (int x = 0; x < par.length; x++) {
				for (int k = 0; k < model.getNumberOfParticles(); k++) {
					b = false;
					for (int i = 0; i < par[x].elements.length; i++) {
						if (par[x].elements[i] == ((Atom) model.getParticle(k)).getID()) {
							b = true;
							break;
						}
					}
					if (!b)
						continue;
					if (model.getParticle(k).isCenterOfMassContained(par[x].area))
						count[x]++;
				}
			}
			String s1 = "";
			for (int i = 0; i < par.length; i++) {
				s1 += count[i] + " atoms of type " + sub[i] + " were counted.\n";
			}
			final String s2 = s1;
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(view), s2, "Counting results",
							JOptionPane.INFORMATION_MESSAGE);
				}
			});
		}
		return true;
	}

	private boolean evaluateLatClause(final String clause) {
		if (model instanceof MesoModel)
			return false;
		if (clause == null || clause.equals(""))
			return false;
		String[] sub = clause.split(REGEX_AND);
		TimeSeriesGenerator.Parameter[] par = new TimeSeriesGenerator.Parameter[sub.length];
		for (int n = 0; n < sub.length; n++) {
			int iw = sub[n].toLowerCase().indexOf("within");
			Rectangle2D area = getWithinArea(sub[n].substring(iw).trim());
			if (area == null)
				return false;
			byte[] elements = getElements(sub[n].substring(0, iw - 1));
			if (elements == null)
				return false;
			par[n] = new TimeSeriesGenerator.Parameter(TimeSeriesGenerator.LAT, elements, area);
		}
		((AtomicModel) model).showTimeSeries(par);
		return true;
	}

	private boolean evaluateLapClause(final String clause) {
		if (model instanceof MesoModel)
			return false;
		if (clause == null || clause.equals(""))
			return false;
		String[] sub = clause.split(REGEX_AND);
		TimeSeriesGenerator.Parameter[] par = new TimeSeriesGenerator.Parameter[sub.length];
		for (int n = 0; n < sub.length; n++) {
			int iw = sub[n].toLowerCase().indexOf("within");
			Rectangle2D area = getWithinArea(sub[n].substring(iw).trim());
			if (area == null)
				return false;
			byte[] elements = getElements(sub[n].substring(0, iw - 1));
			if (elements == null)
				return false;
			par[n] = new TimeSeriesGenerator.Parameter(TimeSeriesGenerator.LAP, elements, area);
		}
		((AtomicModel) model).showTimeSeries(par);
		return true;
	}

	private String evaluateCountFunction(final String clause) {
		if (clause == null || clause.equals(""))
			return null;
		int i = clause.indexOf("(");
		int j = clause.lastIndexOf(")");
		if (i == -1 || j == -1) {
			out(ScriptEvent.FAILED, "function must be enclosed within parenthesis: " + clause);
			return null;
		}
		String s = clause.substring(i + 1, j);
		String[] t = s.split(",");
		int n = t.length;
		switch (n) {
		case 1:
			try {
				i = Integer.parseInt(t[0]);
			}
			catch (NumberFormatException e) {
				out(ScriptEvent.FAILED, t[0] + " cannot be parsed as an integer.");
				return null;
			}
			return "" + model.getParticleCount((byte) i, null);
		case 2:
			float[] x = parseArray(2, t);
			if (x != null) {
				x[1] *= IR_CONVERTER;
				return "" + model.getParticleCountWithin((int) x[0], x[1]);
			}
			break;
		case 5:
			x = parseArray(5, t);
			if (x != null) {
				for (int k = 1; k < 5; k++)
					x[k] *= IR_CONVERTER;
				Shape shape = new Rectangle2D.Float(x[1], x[2], x[3], x[4]);
				return "" + model.getParticleCount((byte) x[0], shape);
			}
			break;
		case 7:
			x = parseArray(7, t);
			if (x != null) {
				for (int k = 1; k < 7; k++)
					x[k] *= IR_CONVERTER;
				Shape shape = new Triangle(x[1], x[2], x[3], x[4], x[5], x[6]).getShape();
				return "" + model.getParticleCount((byte) x[0], shape);
			}
			break;
		case 4:
			x = parseArray(4, t);
			if (x != null) {
				for (int k = 1; k < 4; k++)
					x[k] *= IR_CONVERTER;
				Shape shape = new Ellipse2D.Float(x[1] - x[3], x[2] - x[3], x[3] * 2, x[3] * 2);
				return "" + model.getParticleCount((byte) x[0], shape);
			}
			break;
		default:
			out(ScriptEvent.FAILED, "argument error: " + clause);
			return null;
		}
		return null;
	}

	private static double distanceSquare(double dx, double dy) {
		return dx * dx + dy * dy;
	}

	private String[] evaluateWithinFunction(final String clause) {
		if (clause == null || clause.equals(""))
			return null;
		int i = clause.indexOf("(");
		int j = clause.lastIndexOf(")");
		if (i == -1 || j == -1) {
			out(ScriptEvent.FAILED, "function must be enclosed within parenthesis: " + clause);
			return null;
		}
		String s = clause.substring(i + 1, j);
		String[] t = s.split(",");
		int n = t.length;
		if (n != 4) {
			out(ScriptEvent.FAILED, "argument error: " + clause);
			return null;
		}
		double z = 0;
		float[] x = new float[3];
		for (i = 1; i < 4; i++) {
			z = parseMathExpression(t[i]);
			if (Double.isNaN(z)) {
				out(ScriptEvent.FAILED, "Cannot parse : " + t[i]);
				return null;
			}
			x[i - 1] = (float) z * IR_CONVERTER;
		}
		t[0] = t[0].trim();
		List<Integer> result = null;
		if (t[0].equalsIgnoreCase("PARTICLE") || t[0].equalsIgnoreCase("ATOM")) {
			result = getParticlesWithin(x[0], x[1], x[2]);
		}
		else if (t[0].equalsIgnoreCase("RECTANGLE")) {
			result = getRectanglesWithin(x[0], x[1], x[2]);
		}
		else if (t[0].equalsIgnoreCase("TRIANGLE")) {
			result = getTrianglesWithin(x[0], x[1], x[2]);
		}
		if (result == null || result.isEmpty())
			return null;
		t = new String[result.size()];
		for (i = 0; i < t.length; i++)
			t[i] = "" + result.get(i);
		return t;
	}

	private List<Integer> getParticlesWithin(float x, float y, float r) {
		int n = model.getNumberOfParticles();
		if (n == 0)
			return null;
		List<Integer> list = new ArrayList<Integer>();
		Particle p = null;
		double dx, dy;
		for (int i = 0; i < n; i++) {
			p = model.getParticle(i);
			dx = p.getRx() - x;
			dy = p.getRy() - y;
			if (dx * dx + dy * dy < r * r) {
				list.add(i);
			}
		}
		return list;
	}

	private List<Integer> getRectanglesWithin(float x, float y, float r) {
		RectangleComponent[] rc = view.getRectangles();
		if (rc == null || rc.length == 0)
			return null;
		List<Integer> list = new ArrayList<Integer>();
		Rectangle2D.Float rect = new Rectangle2D.Float(x - r, y - r, 2 * r, 2 * r);
		for (int i = 0; i < rc.length; i++) {
			if (rc[i].getBounds().intersects(rect)) {
				list.add(i);
			}
		}
		return list;
	}

	private List<Integer> getTrianglesWithin(float x, float y, float r) {
		TriangleComponent[] tc = view.getTriangles();
		if (tc == null || tc.length == 0)
			return null;
		List<Integer> list = new ArrayList<Integer>();
		Rectangle2D.Float rect = new Rectangle2D.Float(x - r, y - r, 2 * r, 2 * r);
		for (int i = 0; i < tc.length; i++) {
			if (tc[i].getShape().intersects(rect)) {
				list.add(i);
			}
		}
		return list;
	}

	private String evaluateNearestParticleFunction(final String clause) {
		if (clause == null || clause.equals(""))
			return null;
		int nop = model.getNumberOfParticles();
		if (nop <= 1)
			return "" + (nop - 1);
		int i = clause.indexOf("(");
		int j = clause.lastIndexOf(")");
		if (i == -1 || j == -1) {
			out(ScriptEvent.FAILED, "function must be enclosed within parenthesis: " + clause);
			return null;
		}
		String s = clause.substring(i + 1, j);
		String[] t = s.split(",");
		int n = t.length;
		switch (n) {
		case 2: // nearest(x, y)
			float[] x = parseArray(2, t);
			if (x != null) {
				for (int k = 0; k < x.length; k++)
					x[k] *= IR_CONVERTER;
				double dmin = Double.MAX_VALUE;
				int imin = -1;
				double r = 0;
				Particle p = null;
				for (int k = 0; k < nop; k++) {
					p = model.getParticle(k);
					r = distanceSquare(p.rx - x[0], p.ry - x[1]);
					if (r < dmin) {
						dmin = r;
						imin = k;
					}
				}
				return "" + imin;
			}
			break;
		case 3: // nearest(id, x, y)
			x = parseArray(3, t);
			if (x != null) {
				int id = (int) x[0];
				for (int k = 1; k < x.length; k++)
					x[k] *= IR_CONVERTER;
				double dmin = Double.MAX_VALUE;
				int imin = -1;
				double r = 0;
				Particle p = null;
				for (int k = 0; k < nop; k++) {
					p = model.getParticle(k);
					if (p instanceof Atom) {
						if (((Atom) p).id == id || id == -1) {
							r = distanceSquare(p.rx - x[1], p.ry - x[2]);
							if (r < dmin) {
								dmin = r;
								imin = k;
							}
						}
					}
					else if (p instanceof GayBerneParticle) {
						r = distanceSquare(p.rx - x[1], p.ry - x[2]);
						if (r < dmin) {
							dmin = r;
							imin = k;
						}
					}
				}
				return "" + imin;
			}
			break;
		case 4: // nearest(id, x, y, radius)
			x = parseArray(4, t);
			if (x != null) {
				int id = (int) x[0];
				for (int k = 1; k < x.length; k++)
					x[k] *= IR_CONVERTER;
				double dmin = Double.MAX_VALUE;
				int imin = -1;
				double r = 0;
				Particle p = null;
				x[3] *= x[3];
				for (int k = 0; k < nop; k++) {
					p = model.getParticle(k);
					if (p instanceof Atom) {
						if (((Atom) p).id == id || id == -1) {
							r = distanceSquare(p.rx - x[1], p.ry - x[2]);
							if (r < x[3] && r < dmin) {
								dmin = r;
								imin = k;
							}
						}
					}
					else if (p instanceof GayBerneParticle) {
						r = distanceSquare(p.rx - x[1], p.ry - x[2]);
						if (r < x[3] && r < dmin) {
							dmin = r;
							imin = k;
						}
					}
				}
				return "" + imin;
			}
			break;
		case 7: // nearest(id, x, y, xrect, yrect, wrect, hrect)
			x = parseArray(7, t);
			if (x != null) {
				int id = (int) x[0];
				for (int k = 1; k < x.length; k++)
					x[k] *= IR_CONVERTER;
				double dmin = Double.MAX_VALUE;
				int imin = -1;
				double r = 0;
				Particle p = null;
				for (int k = 0; k < nop; k++) {
					p = model.getParticle(k);
					if (!p.isCenterOfMassContained(x[3], x[4], x[5], x[6]))
						continue;
					if (p instanceof Atom) {
						if (((Atom) p).id == id || id == -1) {
							r = distanceSquare(p.rx - x[1], p.ry - x[2]);
							if (r < dmin) {
								dmin = r;
								imin = k;
							}
						}
					}
					else if (p instanceof GayBerneParticle) {
						r = distanceSquare(p.rx - x[1], p.ry - x[2]);
						if (r < dmin) {
							dmin = r;
							imin = k;
						}
					}
				}
				return "" + imin;
			}
			break;
		default:
			out(ScriptEvent.FAILED, "argument error: " + clause);
			return null;
		}
		return null;
	}

	private String evaluateNearestMoleculeFunction(final String clause) {
		if (clause == null || clause.equals(""))
			return null;
		if (!(model instanceof MolecularModel))
			return null;
		MolecularModel mm = (MolecularModel) model;
		if (mm.molecules.isEmpty())
			return null;
		int i = clause.indexOf("(");
		int j = clause.lastIndexOf(")");
		if (i == -1 || j == -1) {
			out(ScriptEvent.FAILED, "function must be enclosed within parenthesis: " + clause);
			return null;
		}
		int nom = mm.molecules.size();
		String s = clause.substring(i + 1, j);
		String[] t = s.split(",");
		int n = t.length;
		switch (n) {
		case 3:
			float[] x = parseArray(3, t);
			if (x != null) {
				for (int k = 0; k < x.length; k++)
					x[k] *= IR_CONVERTER;
				x[2] *= x[2];
				double dmin = Double.MAX_VALUE;
				int imin = -1;
				double r = 0;
				Molecule m = null;
				for (int k = 0; k < nom; k++) {
					m = mm.molecules.get(k);
					Point2D p = m.getCenterOfMass2D();
					r = distanceSquare(p.getX() - x[0], p.getY() - x[1]);
					if (r < x[2] && r < dmin) {
						dmin = r;
						imin = k;
					}
				}
				return "" + imin;
			}
			break;
		default:
			out(ScriptEvent.FAILED, "argument error: " + clause);
			return null;
		}
		return null;
	}

	private String evaluateNearestToAtomFunction(final String clause) {
		if (clause == null || clause.equals(""))
			return null;
		if (!(model instanceof MolecularModel))
			return null;
		MolecularModel mm = (MolecularModel) model;
		int noa = mm.getNumberOfAtoms();
		if (noa <= 1)
			return "" + (noa - 1);
		int i = clause.indexOf("(");
		int j = clause.lastIndexOf(")");
		if (i == -1 || j == -1) {
			out(ScriptEvent.FAILED, "function must be enclosed within parenthesis: " + clause);
			return null;
		}
		String s = clause.substring(i + 1, j);
		String[] t = s.split(",");
		int n = t.length;
		switch (n) {
		case 3: // nearesttoatom(id, atom, radius)
			float[] x = parseArray(3, t);
			if (x != null) {
				int id = (int) x[0];
				int iat = (int) x[1];
				if (iat < 0 || iat >= noa) {
					out(ScriptEvent.FAILED, "index of atom out of bound: " + iat + " , in " + clause);
					return null;
				}
				x[2] *= IR_CONVERTER;
				x[2] *= x[2];
				double dmin = Double.MAX_VALUE;
				int imin = -1;
				double r = 0;
				Atom a = null, b = mm.atom[iat];
				for (int k = 0; k < noa; k++) {
					a = mm.atom[k];
					if (a == b)
						continue;
					if (a.id == id || id == -1) {
						r = a.distanceSquare(b);
						if (r < x[2] && r < dmin) {
							dmin = r;
							imin = k;
						}
					}
				}
				return "" + imin;
			}
			break;
		case 4: // nearesttoatom(id, atom, radius, option)
			x = parseArray(4, t);
			if (x != null) {
				int id = (int) x[0];
				int iat = (int) x[1];
				if (iat < 0 || iat >= noa) {
					out(ScriptEvent.FAILED, "index of atom out of bound: " + iat + " , in " + clause);
					return null;
				}
				x[2] *= IR_CONVERTER;
				x[2] *= x[2];
				int option = Math.round(x[3]);
				double dmin = Double.MAX_VALUE;
				int imin = -1;
				double r = 0;
				Atom a = null, b = mm.atom[iat];
				Molecule mol = mm.molecules.getMolecule(b);
				for (int k = 0; k < noa; k++) {
					a = mm.atom[k];
					if (a == b)
						continue;
					if (a.id == id || id == -1) {
						r = a.distanceSquare(b);
						if (r < x[2] && r < dmin) {
							switch (option) {
							case 0: // all-atom search
								dmin = r;
								imin = k;
								break;
							case 1: // intermolecular search
								if (mol != mm.molecules.getMolecule(a)) {
									dmin = r;
									imin = k;
								}
								break;
							case 2: // intramolecular search
								if (mol == mm.molecules.getMolecule(a)) {
									dmin = r;
									imin = k;
								}
								break;
							}
						}
					}
				}
				return "" + imin;
			}
			break;
		case 6: // nearesttoatom(id, atom, rect_x, rect_y, rect_width, rect_height)
			x = parseArray(6, t);
			if (x != null) {
				int id = (int) x[0];
				int iat = (int) x[1];
				if (iat < 0 || iat >= noa) {
					out(ScriptEvent.FAILED, "index of atom out of bound: " + iat + " , in " + clause);
					return null;
				}
				for (int k = 2; k < x.length; k++)
					x[k] *= IR_CONVERTER;
				double dmin = Double.MAX_VALUE;
				int imin = -1;
				double r = 0;
				Atom a = null, b = mm.atom[iat];
				for (int k = 0; k < noa; k++) {
					a = mm.atom[k];
					if (a == b)
						continue;
					if (!a.isCenterOfMassContained(x[2], x[3], x[4], x[5]))
						continue;
					if (a.id == id || id == -1) {
						r = a.distanceSquare(b);
						if (r < dmin) {
							dmin = r;
							imin = k;
						}
					}
				}
				return "" + imin;
			}
			break;
		default:
			out(ScriptEvent.FAILED, "argument error: " + clause);
			return null;
		}
		return null;
	}

	private String evaluateComplementaryTypeFunction(final String clause) {
		if (clause == null || clause.equals(""))
			return null;
		if (!(model instanceof MolecularModel))
			return null;
		int i = clause.indexOf("(");
		int j = clause.lastIndexOf(")");
		if (i == -1 || j == -1) {
			out(ScriptEvent.FAILED, "function must be enclosed within parenthesis: " + clause);
			return null;
		}
		String s = clause.substring(i + 1, j).trim();
		String[] t = s.split(",");
		int n = t.length;
		switch (n) {
		case 2:
			double x = parseMathExpression(t[0]);
			if (Double.isNaN(x)) {
				out(ScriptEvent.FAILED, "Cannot parse : " + s);
				return null;
			}
			byte id = (byte) Math.round((float) x);
			t[1] = t[1].trim();
			if ("DNA".equalsIgnoreCase(t[1])) { // complementary type is DNA
				switch (id) {
				case Element.ID_C:
					return "" + Element.ID_G;
				case Element.ID_G:
					return "" + Element.ID_C;
				case Element.ID_U:
				case Element.ID_T:
					return "" + Element.ID_A;
				case Element.ID_A:
					return "" + Element.ID_T;
				}
			}
			if ("RNA".equalsIgnoreCase(t[1])) { // complementary type is RNA
				switch (id) {
				case Element.ID_C:
					return "" + Element.ID_G;
				case Element.ID_G:
					return "" + Element.ID_C;
				case Element.ID_U:
				case Element.ID_T:
					return "" + Element.ID_A;
				case Element.ID_A:
					return "" + Element.ID_U;
				}
			}
			break;
		case 4:
			float[] y = parseArray(3, t);
			if (y != null) {
				char[] codon = new char[3];
				for (int m = 0; m < 3; m++) {
					id = (byte) Math.round(y[m]);
					if (id < Element.ID_A || id > Element.ID_U) {
						out(ScriptEvent.FAILED, "particle not a nucleotide : " + clause);
						return null;
					}
					codon[m] = Element.idToName(id).charAt(0);
				}
				t[3] = t[3].trim();
				if ("PROTEIN".equalsIgnoreCase(t[3])) { // complementary type is protein
					id = AminoAcidAdapter.getElementID(AminoAcidAdapter.expressFromRNA(codon));
					if (id == -1)
						return null;
					return "" + id;
				}
			}
			break;
		default:
			out(ScriptEvent.FAILED, "argument error, cannot parse " + clause);
			return null;
		}
		return null;
	}

	private String evaluateWhichParticleFunction(final String clause) {
		if (clause == null || clause.equals(""))
			return null;
		int nop = model.getNumberOfParticles();
		if (nop <= 0)
			return "-1";
		int i = clause.indexOf("(");
		int j = clause.lastIndexOf(")");
		if (i == -1 || j == -1) {
			out(ScriptEvent.FAILED, "function must be enclosed within parenthesis: " + clause);
			return null;
		}
		String s = clause.substring(i + 1, j);
		String[] t = s.split(",");
		int n = t.length;
		switch (n) {
		case 2:
			float[] x = parseArray(2, t);
			if (x != null) {
				for (int k = 0; k < x.length; k++)
					x[k] *= IR_CONVERTER;
				Particle p = null;
				for (int k = 0; k < nop; k++) {
					p = model.getParticle(k);
					if (p.contains(x[0], x[1]))
						return "" + k;
				}
				return "-1";
			}
			break;
		default:
			out(ScriptEvent.FAILED, "argument error: " + clause);
			return null;
		}
		return null;
	}

	private String evaluateWhichRBondFunction(final String clause) {
		if (clause == null || clause.equals(""))
			return null;
		if (!(model instanceof MolecularModel))
			return null;
		MolecularModel mm = (MolecularModel) model;
		int nrb = mm.bonds.size();
		if (nrb <= 0)
			return null;
		int i = clause.indexOf("(");
		int j = clause.lastIndexOf(")");
		if (i == -1 || j == -1) {
			out(ScriptEvent.FAILED, "function must be enclosed within parenthesis: " + clause);
			return null;
		}
		String s = clause.substring(i + 1, j);
		String[] t = s.split(",");
		int n = t.length;
		switch (n) {
		case 1:
			float[] x = parseArray(1, t);
			if (x != null) {
				Atom a = mm.atom[Math.round(x[0])];
				List<RadialBond> list = mm.bonds.getBonds(a);
				if (list.isEmpty())
					return null;
				return "" + list.get(0).getIndex();
			}
			break;
		case 2:
			x = parseArray(2, t);
			if (x != null) {
				Atom a = mm.atom[Math.round(x[0])];
				Atom b = mm.atom[Math.round(x[1])];
				RadialBond rb = mm.bonds.getBond(a, b);
				if (rb == null)
					return null;
				return "" + rb.getIndex();
			}
			break;
		default:
			out(ScriptEvent.FAILED, "argument error: " + clause);
			return null;
		}
		return null;
	}

	private String evaluateCountRBondFunction(final String clause) {
		if (clause == null || clause.equals(""))
			return null;
		if (!(model instanceof MolecularModel))
			return null;
		MolecularModel mm = (MolecularModel) model;
		if (mm.bonds.isEmpty())
			return null;
		int i = clause.indexOf("(");
		int j = clause.lastIndexOf(")");
		if (i == -1 || j == -1) {
			out(ScriptEvent.FAILED, "function must be enclosed within parenthesis: " + clause);
			return null;
		}
		String s = clause.substring(i + 1, j);
		double x = parseMathExpression(s);
		if (Double.isNaN(x)) {
			out(ScriptEvent.FAILED, "Cannot parse : " + s);
			return null;
		}
		i = Math.round((float) x);
		if (i < 0 || i >= mm.getNumberOfAtoms()) {
			out(ScriptEvent.FAILED, "Index of atom out of bound : " + i + " in " + clause);
			return null;
		}
		Atom a = mm.atom[i];
		return mm.bonds.getBondedPartnerCount(a) + "";
	}

	private String evaluateWhichMoleculeFunction(final String clause) {
		if (clause == null || clause.equals(""))
			return null;
		if (!(model instanceof MolecularModel))
			return null;
		MolecularModel mm = (MolecularModel) model;
		int nmol = mm.molecules.size();
		if (nmol <= 0)
			return null;
		int i = clause.indexOf("(");
		int j = clause.lastIndexOf(")");
		if (i == -1 || j == -1) {
			out(ScriptEvent.FAILED, "function must be enclosed within parenthesis: " + clause);
			return null;
		}
		String s = clause.substring(i + 1, j).trim();
		double x = parseMathExpression(s);
		if (Double.isNaN(x)) {
			out(ScriptEvent.FAILED, "Cannot parse : " + s);
			return null;
		}
		i = Math.round((float) x);
		if (i < 0 || i >= mm.getNumberOfAtoms()) {
			out(ScriptEvent.FAILED, "Index of atom out of bound : " + i + " in " + clause);
			return null;
		}
		Atom a = mm.atom[i];
		Molecule mol = mm.molecules.getMolecule(a);
		if (mol == null)
			return null;
		return "" + mm.molecules.indexOf(mol);
	}

	private String evaluateAtomOfMoleculeFunction(final String clause) {
		if (clause == null || clause.equals(""))
			return null;
		if (!(model instanceof MolecularModel))
			return null;
		MolecularModel mm = (MolecularModel) model;
		int nmol = mm.molecules.size();
		if (nmol <= 0)
			return null;
		int i = clause.indexOf("(");
		int j = clause.lastIndexOf(")");
		if (i == -1 || j == -1) {
			out(ScriptEvent.FAILED, "function must be enclosed within parenthesis: " + clause);
			return null;
		}
		String s = clause.substring(i + 1, j).trim();
		String[] t = s.split(",");
		int n = t.length;
		if (n != 2) {
			out(ScriptEvent.FAILED, "Cannot parse : " + clause);
			return null;
		}
		double x = parseMathExpression(t[0]);
		if (Double.isNaN(x)) {
			out(ScriptEvent.FAILED, "Cannot parse : " + t[0]);
			return null;
		}
		i = Math.round((float) x);
		if (i < 0 || i >= nmol) {
			// out(ScriptEvent.FAILED, "Index of molecule out of bound : " + i + " in " + clause);
			return null;
		}
		x = parseMathExpression(t[1]);
		if (Double.isNaN(x)) {
			out(ScriptEvent.FAILED, "Cannot parse : " + t[1]);
			return null;
		}
		j = Math.round((float) x);
		Molecule mol = mm.molecules.get(i);
		if (j < 0 || j >= mol.size()) {
			// out(ScriptEvent.FAILED, "Index of atom out of bound : " + j + " in " + clause);
			return null;
		}
		Atom a = mol.getAtom(j);
		return "" + a.getIndex();
	}

	private String evaluateWhichImageIsAttachedFunction(final String clause) {
		if (clause == null || clause.equals(""))
			return null;
		int nop = model.getNumberOfParticles();
		if (nop <= 0)
			return "-1";
		int i = clause.indexOf("(");
		int j = clause.lastIndexOf(")");
		if (i == -1 || j == -1) {
			out(ScriptEvent.FAILED, "In whichimageisattached: function must be enclosed within parenthesis: " + clause);
			return null;
		}
		String s = clause.substring(i + 1, j);
		String[] t = s.split(",");
		int n = t.length;
		switch (n) {
		case 2:
			float[] x = parseArray(2, t);
			if (x != null) {
				int index = (int) x[0];
				ModelComponent mc = null;
				switch (Math.round(x[1])) {
				case 0:
					mc = model.getParticle(index);
					break;
				case 1:
					if (model instanceof MolecularModel) {
						mc = ((MolecularModel) model).bonds.get(index);
					}
					break;
				case 2:
					mc = model.obstacles.get(index);
				}
				if (mc == null) {
					out(ScriptEvent.FAILED, "In whichimageisattached: object doesn't exist: " + clause);
					return "-1";
				}
				ImageComponent[] images = view.getImages();
				if (images != null) {
					int nimg = images.length;
					if (nimg != 0) {
						for (int k = 0; k < nimg; k++) {
							if (images[k].getHost() == mc)
								return "" + k;
						}
					}
				}
				return "-1";
			}
			break;
		default:
			out(ScriptEvent.FAILED, "In whichimageisattached: argument error: " + clause);
			return null;
		}
		return null;
	}

	private String evaluateFindIndexByCustomFunction(final String clause) {
		if (clause == null || clause.equals(""))
			return null;
		int i = clause.indexOf("(");
		int j = clause.lastIndexOf(")");
		if (i == -1 || j == -1) {
			out(ScriptEvent.FAILED, "In findIndexByCustom: function must be enclosed within parenthesis: " + clause);
			return null;
		}
		String s = clause.substring(i + 1, j);
		String[] t = s.split(",");
		int n = t.length;
		if (n != 2) {
			out(ScriptEvent.FAILED, "In findIndexByCustom: argument error: " + clause);
			return null;
		}
		double x = parseMathExpression(t[1]);
		if (Double.isNaN(x)) {
			out(ScriptEvent.FAILED, "In findIndexByCustom: cannot parse : " + t[1]);
			return null;
		}
		float custom = (float) x;
		if ("PARTICLE".equalsIgnoreCase(t[0]) || "ATOM".equalsIgnoreCase(t[0])) {
			int m = model.getNumberOfParticles();
			if (m <= 0)
				return "-1";
			for (i = 0; i < m; i++) {
				if (Math.abs(custom - model.getParticle(i).custom) < ZERO)
					return "" + i;
			}
		}
		else if ("IMAGE".equalsIgnoreCase(t[0])) {
			int m = view.getNumberOfInstances(ImageComponent.class);
			if (m <= 0)
				return "-1";
			for (i = 0; i < m; i++) {
				if (Math.abs(custom - view.getImage(i).custom) < ZERO)
					return "" + i;
			}
		}
		else if ("OBSTACLE".equalsIgnoreCase(t[0])) {
			if (model instanceof AtomicModel) {
				AtomicModel am = (AtomicModel) model;
				int m = am.obstacles.size();
				if (m <= 0)
					return "-1";
				for (i = 0; i < m; i++) {
					if (Math.abs(custom - am.obstacles.get(i).custom) < ZERO)
						return "" + i;
				}
			}
		}
		else if ("RADIAL_BOND".equalsIgnoreCase(t[0])) {
			if (model instanceof MolecularModel) {
				MolecularModel mm = (MolecularModel) model;
				int m = mm.bonds.size();
				if (m <= 0)
					return "-1";
				for (i = 0; i < m; i++) {
					if (Math.abs(custom - mm.bonds.get(i).custom) < ZERO)
						return "" + i;
				}
			}
		}
		return null;
	}

	private String evaluateSpeedFunction(final String clause) {
		if (clause == null || clause.equals(""))
			return null;
		int i = clause.indexOf("(");
		int j = clause.lastIndexOf(")");
		if (i == -1 || j == -1) {
			out(ScriptEvent.FAILED, "function must be enclosed within parenthesis: " + clause);
			return null;
		}
		String s = clause.substring(i + 1, j);
		String[] t = s.split(",");
		int n = t.length;
		switch (n) {
		case 2:
			try {
				i = Integer.parseInt(t[1].trim());
			}
			catch (NumberFormatException e) {
				out(ScriptEvent.FAILED, t[1] + " cannot be parsed as an integer.");
				return null;
			}
			return "" + model.getAverageSpeed(t[0], (byte) i, null) * V_CONVERTER;
		case 6:
			String[] t2 = new String[5];
			for (i = 0; i < 5; i++)
				t2[i] = t[i + 1];
			float[] x = parseArray(5, t2);
			if (x != null) {
				for (int k = 1; k < 5; k++)
					x[k] *= IR_CONVERTER;
				Shape shape = new Rectangle2D.Float(x[1], x[2], x[3], x[4]);
				return "" + model.getAverageSpeed(t[0], (byte) x[0], shape) * V_CONVERTER;
			}
			break;
		case 5:
			t2 = new String[4];
			for (i = 0; i < 4; i++)
				t2[i] = t[i + 1];
			x = parseArray(4, t);
			if (x != null) {
				for (int k = 1; k < 4; k++)
					x[k] *= IR_CONVERTER;
				Shape shape = new Ellipse2D.Float(x[1] - x[3], x[2] - x[3], x[3] * 2, x[3] * 2);
				return "" + model.getAverageSpeed(t[0], (byte) x[0], shape) * V_CONVERTER;
			}
			break;
		default:
			out(ScriptEvent.FAILED, "argument error: " + clause);
			return null;
		}
		return null;
	}

	private String evaluateKineticEnergyFunction(final String clause) {
		if (clause == null || clause.equals(""))
			return null;
		int i = clause.indexOf("(");
		int j = clause.lastIndexOf(")");
		if (i == -1 || j == -1) {
			out(ScriptEvent.FAILED, "function must be enclosed within parenthesis: " + clause);
			return null;
		}
		String s = clause.substring(i + 1, j);
		String[] t = s.split(",");
		int n = t.length;
		switch (n) {
		case 1:
			try {
				i = Integer.parseInt(t[0]);
			}
			catch (NumberFormatException e) {
				out(ScriptEvent.FAILED, t[0] + " cannot be parsed as an integer.");
				return null;
			}
			return "" + model.getKineticEnergy((byte) i, null);
		case 5:
			float[] x = parseArray(5, t);
			if (x != null) {
				for (int k = 1; k < 5; k++)
					x[k] *= IR_CONVERTER;
				Shape shape = new Rectangle2D.Float(x[1], x[2], x[3], x[4]);
				return "" + model.getKineticEnergy((byte) x[0], shape);
			}
			break;
		case 4:
			x = parseArray(4, t);
			if (x != null) {
				for (int k = 1; k < 4; k++)
					x[k] *= IR_CONVERTER;
				Shape shape = new Ellipse2D.Float(x[1] - x[3], x[2] - x[3], x[3] * 2, x[3] * 2);
				return "" + model.getKineticEnergy((byte) x[0], shape);
			}
			break;
		default:
			out(ScriptEvent.FAILED, "argument error: " + clause);
			return null;
		}
		return null;
	}

	private String evaluateTemperatureFunction(final String clause) {
		if (clause == null || clause.equals(""))
			return null;
		int i = clause.indexOf("(");
		int j = clause.lastIndexOf(")");
		if (i == -1 || j == -1) {
			out(ScriptEvent.FAILED, "function must be enclosed within parenthesis: " + clause);
			return null;
		}
		String s = clause.substring(i + 1, j);
		String[] t = s.split(",");
		int n = t.length;
		switch (n) {
		case 5:
			float[] x = parseArray(5, t);
			if (x != null) {
				for (int k = 1; k < 5; k++)
					x[k] *= IR_CONVERTER;
				Shape shape = new Rectangle2D.Float(x[1], x[2], x[3], x[4]);
				return "" + model.getTemperature((byte) Math.round(x[0]), shape);
			}
			break;
		case 4:
			x = parseArray(4, t);
			if (x != null) {
				for (int k = 1; k < 4; k++)
					x[k] *= IR_CONVERTER;
				Shape shape = new Ellipse2D.Float(x[1] - x[3], x[2] - x[3], x[3] * 2, x[3] * 2);
				return "" + model.getTemperature((byte) Math.round(x[0]), shape);
			}
			break;
		case 1:
			double z = parseMathExpression(t[0]);
			if (Double.isNaN(z)) {
				out(ScriptEvent.FAILED, "Cannot parse : " + t[0]);
				return null;
			}
			return "" + model.getTemperature((byte) Math.round(z), null);
		default:
			out(ScriptEvent.FAILED, "argument error: " + clause);
			return null;
		}
		return null;
	}

	private String evaluatePtSegDistSqFunction(final String clause) {
		if (clause == null || clause.equals(""))
			return null;
		int i = clause.indexOf("(");
		int j = clause.lastIndexOf(")");
		if (i == -1 || j == -1) {
			out(ScriptEvent.FAILED, "function must be enclosed within parenthesis: " + clause);
			return null;
		}
		String s = clause.substring(i + 1, j).trim();
		String[] t = s.split(",");
		float[] x = parseArray(6, t);
		if (x != null) {
			return "" + Line2D.ptSegDistSq(x[0], x[1], x[2], x[3], x[4], x[5]);
		}
		return null;
	}

	private Rectangle2D getWithinArea(String within) {
		int lp = within.indexOf("(");
		if (lp == -1)
			lp = within.indexOf("[");
		int rp = within.indexOf(")");
		if (rp == -1)
			rp = within.indexOf("]");
		float x = 0, y = 0, w = 0, h = 0;
		try {
			within = within.substring(lp + 1, rp).trim();
			String[] s = within.split(REGEX_SEPARATOR + "+");
			x = Float.valueOf(s[0].trim()).floatValue() * IR_CONVERTER;
			y = Float.valueOf(s[1].trim()).floatValue() * IR_CONVERTER;
			w = Float.valueOf(s[2].trim()).floatValue() * IR_CONVERTER;
			h = Float.valueOf(s[3].trim()).floatValue() * IR_CONVERTER;
		}
		catch (Exception e) {
			out(ScriptEvent.FAILED, "Within clause error at: " + within + "\n" + e);
			return null;
		}
		return new Rectangle2D.Float(x, y, w, h);
	}

	private byte[] getElements(String str) {
		byte[] elements = null;
		boolean found = false;
		Matcher matcher = RANGE.matcher(str);
		if (matcher.find()) {
			found = true;
			String[] s = str.substring(0, matcher.end()).split("-");
			byte start, end;
			try {
				start = Byte.valueOf(s[0].trim()).byteValue();
				end = Byte.valueOf(s[1].trim()).byteValue();
			}
			catch (NumberFormatException e) {
				out(ScriptEvent.FAILED, "Element limits must be integers (<256): " + str);
				return null;
			}
			if (end < start) {
				out(ScriptEvent.FAILED, "The second number of a range must be greater than the first: " + str);
				return null;
			}
			elements = new byte[end - start + 1];
			for (byte i = start; i <= end; i++)
				elements[i - start] = i;
		}
		if (!found) {
			matcher = INTEGER_GROUP.matcher(str);
			if (matcher.find()) {
				found = true;
				String[] s = str.split(REGEX_SEPARATOR + "+");
				elements = new byte[s.length];
				try {
					for (int i = 0; i < s.length; i++)
						elements[i] = Byte.valueOf(s[i].trim()).byteValue();
				}
				catch (NumberFormatException e) {
					out(ScriptEvent.FAILED, "Element types must be integers (<256): " + str);
					return null;
				}
			}
		}
		if (!found) {
			matcher = INDEX.matcher(str);
			if (matcher.find()) {
				found = true;
				byte id = 0;
				try {
					id = Byte.valueOf(str.substring(0, matcher.end()).trim()).byteValue();
				}
				catch (NumberFormatException e) {
					out(ScriptEvent.FAILED, "Element type must be an integer (<256): " + str);
					return null;
				}
				elements = new byte[] { id };
			}
		}
		if (!found)
			return null;
		return elements;
	}

	private boolean evaluateAvposClause(String str) {
		if (str == null || str.equals(""))
			return false;
		boolean on = str.equalsIgnoreCase("on");
		int n = model.getNumberOfParticles();
		if (n <= 0)
			return true;
		for (int k = 0; k < n; k++) {
			Particle p = model.getParticle(k);
			if (p.isSelected())
				p.setShowRMean(on);
		}
		view.repaint();
		notifyChange();
		return true;
	}

	private boolean evaluateAvforClause(String str) {
		if (str == null || str.equals(""))
			return false;
		boolean on = str.equalsIgnoreCase("on");
		int n = model.getNumberOfParticles();
		if (n <= 0)
			return true;
		for (int k = 0; k < n; k++) {
			Particle p = model.getParticle(k);
			if (p.isSelected())
				p.setShowFMean(on);
		}
		view.repaint();
		notifyChange();
		return true;
	}

	private boolean evaluateTrajectoryClause(String str) {
		if (str == null || str.equals(""))
			return false;
		boolean on = str.equalsIgnoreCase("on");
		int n = model.getNumberOfParticles();
		if (n <= 0)
			return true;
		for (int k = 0; k < n; k++) {
			Particle p = model.getParticle(k);
			if (p.isSelected())
				p.setShowRTraj(on);
		}
		view.repaint();
		notifyChange();
		return true;
	}

	private boolean evaluateShowClause(String str) {
		String s = str.toLowerCase();
		byte result = parseOnOff("charge", s);
		if (result != -1) {
			if (result != 0) {
				view.setDrawCharge(result == ON);
			}
			return result != 0;
		}
		result = parseOnOff("index", s);
		if (result != -1) {
			view.setShowParticleIndex(result == ON);
			return true;
		}
		result = parseOnOff("clock", s);
		if (result != -1) {
			view.setShowClock(result == ON);
			return true;
		}
		result = parseOnOff("selectionhalo", s);
		if (result != -1) {
			view.setShowSelectionHalo(result == ON);
			return true;
		}
		if (model instanceof MolecularModel) {
			if (s.startsWith("style")) {
				((AtomisticView) view).setDisplayStyle(s.substring(6).trim());
				return true;
			}
			result = parseOnOff("velocity", s);
			if (result != -1) {
				((AtomisticView) view).showVelocityVector(result == ON);
				return true;
			}
			result = parseOnOff("momentum", s);
			if (result != -1) {
				((AtomisticView) view).showMomentumVector(result == ON);
				return true;
			}
			result = parseOnOff("acceleration", s);
			if (result != -1) {
				((AtomisticView) view).showAccelerationVector(result == ON);
				return true;
			}
			result = parseOnOff("force", s);
			if (result != -1) {
				((AtomisticView) view).showForceVector(result == ON);
				return true;
			}
			result = parseOnOff("vdwline", s);
			if (result != -1) {
				((AtomisticView) view).showVDWLines(result == ON);
				return true;
			}
			result = parseOnOff("keshading", s);
			if (result != -1) {
				((AtomisticView) view).showShading(result == ON);
				return true;
			}
			result = parseOnOff("chargeshading", s);
			if (result != -1) {
				((AtomisticView) view).showChargeShading(result == ON);
				return true;
			}
			double param = parseKeywordValue("grid", s);
			if (!Double.isNaN(param)) {
				((AtomisticView) view).setGridMode((byte) param);
				((MolecularModel) model).setupGrid();
				view.repaint();
				return true;
			}
			param = parseKeywordValue("contour", s);
			if (!Double.isNaN(param)) {
				Atom probe = ((AtomicModel) model).createAtomOfElement(Element.ID_NT);
				probe.setCharge(param);
				((AtomisticView) view).showContourPlot(true, probe);
				view.repaint();
				return true;
			}
			param = parseKeywordValue("efield", s);
			if (!Double.isNaN(param)) {
				((AtomisticView) view).showEFieldLines(true, (int) param);
				view.repaint();
				return true;
			}
		}
		else if (model instanceof MesoModel) {
			result = parseOnOff("dipole", s);
			if (result != -1) {
				((MesoView) view).setDrawDipole(result == ON);
				return true;
			}
		}
		out(ScriptEvent.FAILED, "Unrecognized keyword: " + str);
		return false;
	}

	private boolean evaluateSetClause(String str) {

		String[] s = null;

		// action
		Matcher matcher = ACTION.matcher(str);
		if (matcher.find()) {
			str = str.substring(matcher.end()).trim().toUpperCase();
			matcher = ACTION_ID.matcher(str);
			if (matcher.find()) {
				fillActionIDMap();
				if (!actionIDMap.containsKey(str)) {
					out(ScriptEvent.FAILED, "Unrecognized parameter: " + str);
					return false;
				}
				view.setAction(actionIDMap.get(str));
				return true;
			}
		}

		// particle field
		matcher = PARTICLE_FIELD.matcher(str);
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
					setParticleField(s2, s[0], "on".equalsIgnoreCase(s[1]));
				}
				else if ("movable".equalsIgnoreCase(s[0])) {
					setParticleField(s2, s[0], "on".equalsIgnoreCase(s[1]));
				}
				else if ("draggable".equalsIgnoreCase(s[0])) {
					setParticleField(s2, s[0], "on".equalsIgnoreCase(s[1]));
				}
			}
			else {
				double x = parseMathExpression(s[1]);
				if (Double.isNaN(x))
					return false;
				setParticleField(s2, s[0], x);
			}
			return true;
		}

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
					setParticleField(s2, s[0], "on".equalsIgnoreCase(s[1]));
				}
				else if ("movable".equalsIgnoreCase(s[0])) {
					setParticleField(s2, s[0], "on".equalsIgnoreCase(s[1]));
				}
				else if ("draggable".equalsIgnoreCase(s[0])) {
					setParticleField(s2, s[0], "on".equalsIgnoreCase(s[1]));
				}
			}
			else {
				double x = parseMathExpression(s[1]);
				if (Double.isNaN(x))
					return false;
				setAtomField(s2, s[0], x);
			}
			return true;
		}

		// rbond field
		matcher = RBOND_FIELD.matcher(str);
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
			if ("visible".equalsIgnoreCase(s[0]) || "style".equalsIgnoreCase(s[0])
					|| "torquetype".equalsIgnoreCase(s[0])) {
				setRbondField(s2, s[0], s[1]);
			}
			else {
				double x = parseMathExpression(s[1]);
				if (Double.isNaN(x))
					return false;
				setRbondField(s2, s[0], x);
			}
			return true;
		}

		// abond field
		matcher = ABOND_FIELD.matcher(str);
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
			double x = parseMathExpression(s[1]);
			if (Double.isNaN(x))
				return false;
			setAbondField(s2, s[0], x);
			return true;
		}

		// element field
		matcher = ELEMENT_FIELD.matcher(str);
		if (matcher.find()) {
			int end = matcher.end();
			s = str.substring(end).trim().split(REGEX_WHITESPACE + "+");
			if (s.length != 2) {
				out(ScriptEvent.FAILED, "Argument error: " + str);
				return false;
			}
			double x = parseMathExpression(s[1]);
			if (Double.isNaN(x))
				return false;
			setElementField(str.substring(0, end - 1), s[0], x);
			return true;
		}

		// obstacle field
		matcher = OBSTACLE_FIELD.matcher(str);
		if (matcher.find()) {
			int end = matcher.end();
			s = str.substring(end).trim().split(REGEX_WHITESPACE + "+");
			if (s.length != 2) {
				out(ScriptEvent.FAILED, "Argument error: " + str);
				return false;
			}
			if (s[0].startsWith("%")) {
				s[0] = s[0].substring(1);
			}
			if ("on".equalsIgnoreCase(s[1]) || "off".equalsIgnoreCase(s[1])) {
				if ("visible".equalsIgnoreCase(s[0])) {
					setObstacleField(str.substring(0, end - 1), s[0], "on".equalsIgnoreCase(s[1]));
				}
				else if ("draggable".equalsIgnoreCase(s[0])) {
					setObstacleField(str.substring(0, end - 1), s[0], "on".equalsIgnoreCase(s[1]));
				}
			}
			else {
				double x = parseMathExpression(s[1]);
				if (Double.isNaN(x))
					return false;
				setObstacleField(str.substring(0, end - 1), s[0], x);
			}
			return true;
		}

		// light source field
		matcher = LIGHT_SOURCE.matcher(str);
		if (matcher.find()) {
			if (model instanceof MolecularModel) {
				MolecularModel mm = (MolecularModel) model;
				String t = str.substring(matcher.end()).toLowerCase();
				if (t.startsWith(".angle")) {
					t = t.substring(6).trim();
					double x = parseMathExpression(t);
					if (Double.isNaN(x))
						return false;
					mm.getLightSource().setAngleOfIncidence((float) Math.toRadians(x));
				}
				return true;
			}
			return false;
		}

		// image field
		matcher = IMAGE_FIELD.matcher(str);
		if (matcher.find()) {
			int end = matcher.end();
			s = str.substring(end).trim().split(REGEX_WHITESPACE + "+");
			if (s.length != 2) {
				out(ScriptEvent.FAILED, "Argument error: " + str);
				return false;
			}
			setImageField(str.substring(0, end - 1), s[0], s[1]);
			return true;
		}

		// line field
		matcher = LINE_FIELD.matcher(str);
		if (matcher.find()) {
			int end = matcher.end();
			s = str.substring(end).trim().split(REGEX_WHITESPACE + "+");
			if (s.length < 2) {
				out(ScriptEvent.FAILED, "Argument error: " + str);
				return false;
			}
			else if (s.length == 5) {
				s[1] += s[2] + s[3] + s[4];
			}
			setLineField(str.substring(0, end - 1), s[0], s[1]);
			return true;
		}

		// rectangle field
		matcher = RECTANGLE_FIELD.matcher(str);
		if (matcher.find()) {
			int end = matcher.end();
			s = str.substring(end).trim().split(REGEX_WHITESPACE + "+");
			if (s.length < 2) {
				out(ScriptEvent.FAILED, "Argument error: " + str);
				return false;
			}
			else if (s.length == 5) {
				s[1] += s[2] + s[3] + s[4];
			}
			setRectangleField(str.substring(0, end - 1), s[0], s[1]);
			return true;
		}

		// triangle field
		matcher = TRIANGLE_FIELD.matcher(str);
		if (matcher.find()) {
			int end = matcher.end();
			s = str.substring(end).trim().split(REGEX_WHITESPACE + "+");
			if (s.length < 2) {
				out(ScriptEvent.FAILED, "Argument error: " + str);
				return false;
			}
			setTriangleField(str.substring(0, end - 1), s[0], s[1]);
			return true;
		}

		// ellipse field
		matcher = ELLIPSE_FIELD.matcher(str);
		if (matcher.find()) {
			int end = matcher.end();
			s = str.substring(end).trim().split(REGEX_WHITESPACE + "+");
			if (s.length < 2) {
				out(ScriptEvent.FAILED, "Argument error: " + str);
				return false;
			}
			else if (s.length == 5) {
				s[1] += s[2] + s[3] + s[4];
			}
			setEllipseField(str.substring(0, end - 1), s[0], s[1]);
			return true;
		}

		// textbox field
		matcher = TEXTBOX_FIELD.matcher(str);
		if (matcher.find()) {
			int end = matcher.end();
			String s1 = str.substring(0, end - 1);
			String s2 = str.substring(end).trim();
			int i = s2.indexOf(" ");
			setTextBoxField(s1, s2.substring(0, i), s2.substring(i).trim());
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
				exp = evaluateTemperatureFunction(exp);
				if (exp != null)
					storeDefinition(isStatic, var, exp);
			}
			else if (exp.startsWith("kineticenergy(")) {
				exp = evaluateKineticEnergyFunction(exp);
				if (exp != null)
					storeDefinition(isStatic, var, exp);
			}
			else if (exp.startsWith("speed(")) {
				exp = evaluateSpeedFunction(exp);
				if (exp != null)
					storeDefinition(isStatic, var, exp);
			}
			else if (exp.startsWith("count(")) {
				exp = evaluateCountFunction(exp);
				storeDefinition(isStatic, var, exp != null ? exp : "0");
			}
			else if (exp.startsWith("within(")) {
				removeDefinition(var);
				String[] array = evaluateWithinFunction(exp);
				if (array != null) {
					createArray(var, array.length);
					for (int i = 0; i < array.length; i++) {
						storeDefinition(isStatic, var + "[" + i + "]", array[i] != null ? array[i] : "-1");
					}
				}
				else {
					storeDefinition(isStatic, var + ".length", "0");
				}
			}
			else if (exp.startsWith("nearest(")) {
				exp = evaluateNearestParticleFunction(exp);
				storeDefinition(isStatic, var, exp != null ? exp : "-1");
			}
			else if (exp.startsWith("nearestmolecule(")) {
				exp = evaluateNearestMoleculeFunction(exp);
				storeDefinition(isStatic, var, exp != null ? exp : "-1");
			}
			else if (exp.startsWith("nearesttoatom(")) {
				exp = evaluateNearestToAtomFunction(exp);
				storeDefinition(isStatic, var, exp != null ? exp : "-1");
			}
			else if (exp.startsWith("complementarytype(")) {
				exp = evaluateComplementaryTypeFunction(exp);
				storeDefinition(isStatic, var, exp != null ? exp : "-1");
			}
			else if (exp.startsWith("whichparticle(")) {
				exp = evaluateWhichParticleFunction(exp);
				storeDefinition(isStatic, var, exp != null ? exp : "-1");
			}
			else if (exp.startsWith("whichrbond(")) {
				exp = evaluateWhichRBondFunction(exp);
				storeDefinition(isStatic, var, exp != null ? exp : "-1");
			}
			else if (exp.startsWith("countrbond(")) {
				exp = evaluateCountRBondFunction(exp);
				storeDefinition(isStatic, var, exp != null ? exp : "0");
			}
			else if (exp.startsWith("whichmolecule(")) {
				exp = evaluateWhichMoleculeFunction(exp);
				storeDefinition(isStatic, var, exp != null ? exp : "-1");
			}
			else if (exp.startsWith("atomofmolecule(")) {
				exp = evaluateAtomOfMoleculeFunction(exp);
				storeDefinition(isStatic, var, exp != null ? exp : "-1");
			}
			else if (exp.startsWith("whichimageisattached(")) {
				exp = evaluateWhichImageIsAttachedFunction(exp);
				storeDefinition(isStatic, var, exp != null ? exp : "-1");
			}
			else if (exp.startsWith("findindexbycustom(")) {
				exp = evaluateFindIndexByCustomFunction(exp);
				storeDefinition(isStatic, var, exp != null ? exp : "-1");
			}
			else if (exp.startsWith("ptsegdistsq(")) {
				exp = evaluatePtSegDistSqFunction(exp);
				storeDefinition(isStatic, var, exp != null ? exp : "-1");
			}
			else {
				evaluateDefineMathexClause(isStatic, var, exp);
			}
			return true;
		}

		s = str.trim().split(REGEX_WHITESPACE + "+");

		if (s.length == 2) {

			String s0 = s[0].trim().toLowerCase().intern();
			if ("on".equalsIgnoreCase(s[1].trim()) || "off".equalsIgnoreCase(s[1].trim())) {
				if (s0 == "visible") {
					boolean b = "on".equalsIgnoreCase(s[1].trim());
					int n = model.getNumberOfParticles();
					for (int i = 0; i < n; i++) {
						Particle p = model.getParticle(i);
						if (p.isSelected())
							p.visible = b;
					}
					LineComponent[] lc = view.getLines();
					if (lc != null && lc.length > 0) {
						for (LineComponent x : lc) {
							if (x.isSelected())
								x.setVisible(b);
						}
					}
					RectangleComponent[] rc = view.getRectangles();
					if (rc != null && rc.length > 0) {
						for (RectangleComponent x : rc) {
							if (x.isSelected())
								x.setVisible(b);
						}
					}
					EllipseComponent[] ec = view.getEllipses();
					if (ec != null && ec.length > 0) {
						for (EllipseComponent x : ec) {
							if (x.isSelected())
								x.setVisible(b);
						}
					}
					TextBoxComponent[] tc = view.getTextBoxes();
					if (tc != null && tc.length > 0) {
						for (TextBoxComponent x : tc) {
							if (x.isSelected())
								x.setVisible(b);
						}
					}
					ImageComponent[] ic = view.getImages();
					if (ic != null && ic.length > 0) {
						for (ImageComponent x : ic) {
							if (x.isSelected())
								x.setVisible(b);
						}
					}
					if (model instanceof MolecularModel) {
						MolecularModel mm = (MolecularModel) model;
						RadialBond rb;
						boolean rbChanged = false;
						synchronized (mm.bonds.getSynchronizationLock()) {
							for (Iterator it = mm.bonds.iterator(); it.hasNext();) {
								rb = (RadialBond) it.next();
								if (rb.isSelected()) {
									rb.setVisible(b);
									if (!rbChanged)
										rbChanged = true;
								}
							}
						}
						if (rbChanged) // notify the bond change listener so that JmolRender can set visibility
							mm.notifyBondChangeListeners();
						RectangularObstacle obs;
						synchronized (mm.obstacles.getSynchronizationLock()) {
							for (Iterator it = mm.obstacles.iterator(); it.hasNext();) {
								obs = (RectangularObstacle) it.next();
								if (obs.isSelected())
									obs.setVisible(b);
							}
						}
					}
					notifyChange();
					view.repaint();
					return true;
				}
				if (s0 == "draggable") {
					boolean b = "on".equalsIgnoreCase(s[1].trim());
					int n = model.getNumberOfParticles();
					for (int i = 0; i < n; i++) {
						Particle p = model.getParticle(i);
						if (p.isSelected())
							p.setDraggable(b);
					}
					LineComponent[] lc = view.getLines();
					if (lc != null && lc.length > 0) {
						for (LineComponent x : lc) {
							if (x.isSelected())
								x.setDraggable(b);
						}
					}
					RectangleComponent[] rc = view.getRectangles();
					if (rc != null && rc.length > 0) {
						for (RectangleComponent x : rc) {
							if (x.isSelected())
								x.setDraggable(b);
						}
					}
					EllipseComponent[] ec = view.getEllipses();
					if (ec != null && ec.length > 0) {
						for (EllipseComponent x : ec) {
							if (x.isSelected())
								x.setDraggable(b);
						}
					}
					TextBoxComponent[] tc = view.getTextBoxes();
					if (tc != null && tc.length > 0) {
						for (TextBoxComponent x : tc) {
							if (x.isSelected())
								x.setDraggable(b);
						}
					}
					ImageComponent[] ic = view.getImages();
					if (ic != null && ic.length > 0) {
						for (ImageComponent x : ic) {
							if (x.isSelected())
								x.setDraggable(b);
						}
					}
					if (model instanceof MolecularModel) {
						MolecularModel mm = (MolecularModel) model;
						RectangularObstacle obs;
						synchronized (mm.obstacles.getSynchronizationLock()) {
							for (Iterator it = mm.obstacles.iterator(); it.hasNext();) {
								obs = (RectangularObstacle) it.next();
								if (obs.isSelected())
									obs.setDraggable(b);
							}
						}
					}
					notifyChange();
					return true;
				}
				if (s0 == "movable") {
					boolean b = "on".equalsIgnoreCase(s[1].trim());
					int n = model.getNumberOfParticles();
					for (int i = 0; i < n; i++) {
						Particle p = model.getParticle(i);
						if (p.isSelected())
							p.setMovable(b);
					}
					notifyChange();
					return true;
				}
				if (s0 == "translucent") {
					if (model instanceof MolecularModel) {
						((AtomisticView) view).setTranslucent("on".equalsIgnoreCase(s[1].trim()));
						view.repaint();
						notifyChange();
						return true;
					}
				}
			}
			if (s0 == "bondstyle") {
				if (model instanceof MolecularModel) {
					MolecularModel mm = (MolecularModel) model;
					if (!mm.bonds.isEmpty()) {
						byte style = -1;
						for (Field f : RadialBond.class.getFields()) {
							if (f.getName().equalsIgnoreCase(s[1].trim())) {
								try {
									style = f.getByte(null);
								}
								catch (IllegalArgumentException e) {
									e.printStackTrace();
									out(ScriptEvent.FAILED, "Incorrect bond style input.");
									return false;
								}
								catch (IllegalAccessException e) {
									e.printStackTrace();
									out(ScriptEvent.FAILED, "Incorrect bond style input.");
									return false;
								}
							}
						}
						if (style == -1) {
							out(ScriptEvent.FAILED, "Incorrect bond style input.");
							return false;
						}
						RadialBond rb;
						synchronized (mm.bonds.getSynchronizationLock()) {
							for (Iterator it = mm.bonds.iterator(); it.hasNext();) {
								rb = (RadialBond) it.next();
								if (rb.isSelected())
									rb.setBondStyle(style);
							}
						}
						notifyChange();
					}
				}
				return true;
			}
			else if (s0 == "bondcolor") {
				if (model instanceof MolecularModel) {
					MolecularModel mm = (MolecularModel) model;
					if (!mm.bonds.isEmpty()) {
						Color c = parseRGBColor(s[1].trim());
						if (c == null) {
							out(ScriptEvent.FAILED, "Incorrect bond color input.");
							return false;
						}
						RadialBond rb;
						synchronized (mm.bonds.getSynchronizationLock()) {
							for (Iterator it = mm.bonds.iterator(); it.hasNext();) {
								rb = (RadialBond) it.next();
								if (rb.isSelected())
									rb.setBondColor(c);
							}
						}
						notifyChange();
					}
				}
				return true;
			}
			else if (s0 == "efielddirection") {
				ElectricField eField = (ElectricField) model.getNonLocalField(ElectricField.class.getName());
				if (eField != null) {
					try {
						Field fie = VectorField.class.getField(s[1]);
						eField.setOrientation(fie.getShort(null));
					}
					catch (Exception e) {
						e.printStackTrace();
						return false;
					}
					notifyChange();
				}
				return true;
			}
			double x = parseMathExpression(s[1]);
			if (Double.isNaN(x))
				return false;
			if (s0 == "vx") {
				int n = model.getNumberOfParticles();
				for (int i = 0; i < n; i++) {
					Particle p = model.getParticle(i);
					if (p.isSelected())
						p.vx = x * IV_CONVERTER;
				}
				notifyChange();
				return true;
			}
			else if (s0 == "vy") {
				int n = model.getNumberOfParticles();
				for (int i = 0; i < n; i++) {
					Particle p = model.getParticle(i);
					if (p.isSelected())
						p.vy = x * IV_CONVERTER;
				}
				notifyChange();
				return true;
			}
			else if (s0 == "hx") {
				int n = model.getNumberOfParticles();
				for (int i = 0; i < n; i++) {
					Particle p = model.getParticle(i);
					if (p.isSelected())
						p.hx = (float) x * R_CONVERTER;
				}
				notifyChange();
				return true;
			}
			else if (s0 == "hy") {
				int n = model.getNumberOfParticles();
				for (int i = 0; i < n; i++) {
					Particle p = model.getParticle(i);
					if (p.isSelected())
						p.hy = (float) x * R_CONVERTER;
				}
				notifyChange();
				return true;
			}
			else if (s0 == "kelvin") {
				int n = model.getNumberOfParticles();
				if (model instanceof AtomicModel) {
					AtomicModel am = (AtomicModel) model;
					List<Atom> list = new ArrayList<Atom>();
					for (int i = 0; i < n; i++) {
						Atom a = am.atom[i];
						if (a.isSelected())
							list.add(a);
					}
					if (!list.isEmpty()) {
						am.setTemperature(list, x);
						notifyChange();
					}
				}
				return true;
			}
			else if (s0 == "charge") {
				int n = model.getNumberOfParticles();
				for (int i = 0; i < n; i++) {
					Particle p = model.getParticle(i);
					if (p.isSelected())
						p.charge = x;
				}
				notifyChange();
				return true;
			}
			else if (s0 == "omega") {
				int n = model.getNumberOfParticles();
				for (int i = 0; i < n; i++) {
					Particle p = model.getParticle(i);
					if (p.isSelected() && p instanceof UnitedAtom) {
						((UnitedAtom) p).omega = x;
					}
				}
				notifyChange();
				return true;
			}
			else if (s0 == "dipole") {
				int n = model.getNumberOfParticles();
				for (int i = 0; i < n; i++) {
					Particle p = model.getParticle(i);
					if (p.isSelected() && p instanceof UnitedAtom) {
						((UnitedAtom) p).dipoleMoment = x;
					}
				}
				notifyChange();
				return true;
			}
			else if (s0 == "strength") {
				if (model instanceof MolecularModel) {
					MolecularModel mm = (MolecularModel) model;
					if (!mm.bonds.isEmpty()) {
						RadialBond rb;
						synchronized (mm.bonds.getSynchronizationLock()) {
							for (Iterator it = mm.bonds.iterator(); it.hasNext();) {
								rb = (RadialBond) it.next();
								if (rb.isSelected()) {
									if (x > ZERO) {
										rb.setBondStrength(x);
									}
									else {
										it.remove();
									}
								}
							}
						}
					}
					if (!mm.bends.isEmpty()) {
						AngularBond ab;
						synchronized (mm.bends.getSynchronizationLock()) {
							for (Iterator it = mm.bends.iterator(); it.hasNext();) {
								ab = (AngularBond) it.next();
								if (ab.isSelected()) {
									if (x > ZERO) {
										ab.setBondStrength(x);
									}
									else {
										it.remove();
									}
								}
							}
						}
					}
					mm.removeGhostAngularBonds();
					notifyChange();
				}
				return true;
			}
			else if (s0 == "bondlength") {
				if (model instanceof MolecularModel) {
					MolecularModel mm = (MolecularModel) model;
					if (!mm.bonds.isEmpty()) {
						RadialBond rb;
						synchronized (mm.bonds.getSynchronizationLock()) {
							for (Iterator it = mm.bonds.iterator(); it.hasNext();) {
								rb = (RadialBond) it.next();
								if (rb.isSelected())
									rb.setBondLength(x * IR_CONVERTER);
							}
						}
						notifyChange();
					}
				}
				return true;
			}
			else if (s0 == "bondangle") {
				if (model instanceof MolecularModel) {
					MolecularModel mm = (MolecularModel) model;
					if (!mm.bends.isEmpty()) {
						AngularBond ab;
						synchronized (mm.bends.getSynchronizationLock()) {
							for (Iterator it = mm.bends.iterator(); it.hasNext();) {
								ab = (AngularBond) it.next();
								if (ab.isSelected())
									ab.setBondAngle(Math.toRadians(x));
							}
						}
						notifyChange();
					}
				}
				return true;
			}
			else if (s0 == "torque") {
				if (model instanceof MolecularModel) {
					MolecularModel mm = (MolecularModel) model;
					if (!mm.molecules.isEmpty()) {
						Molecule mol;
						synchronized (mm.molecules.getSynchronizationLock()) {
							for (Iterator it = mm.molecules.iterator(); it.hasNext();) {
								mol = (Molecule) it.next();
								if (mol.isSelected()) {
									if (Math.abs(x) > Particle.ZERO) {
										MolecularTorque mt = mol.getTorque();
										if (mt == null) {
											mt = new MolecularTorque();
											mol.setTorque(mt);
										}
										mt.setForce((float) (x * IV_CONVERTER));
									}
									else {
										mol.setTorque(null);
									}
								}
							}
						}
						notifyChange();
					}
				}
				return true;
			}
			else if (s0 == "muscle_amplitude") {
				if (model instanceof MolecularModel) {
					MolecularModel mm = (MolecularModel) model;
					if (!mm.bonds.isEmpty()) {
						RadialBond rb;
						synchronized (mm.bonds.getSynchronizationLock()) {
							for (Iterator it = mm.bonds.iterator(); it.hasNext();) {
								rb = (RadialBond) it.next();
								if (rb.isSelected()) {
									rb.setAmplitude((float) (x * 0.001));
								}
							}
						}
						notifyChange();
					}
				}
				return true;
			}
			else if (s0 == "muscle_period") {
				if (model instanceof MolecularModel) {
					MolecularModel mm = (MolecularModel) model;
					if (!mm.bonds.isEmpty()) {
						RadialBond rb;
						synchronized (mm.bonds.getSynchronizationLock()) {
							for (Iterator it = mm.bonds.iterator(); it.hasNext();) {
								rb = (RadialBond) it.next();
								if (rb.isSelected()) {
									rb.setPeriod((int) x);
								}
							}
						}
						notifyChange();
					}
				}
				return true;
			}
			else if (s0 == "model_time") {
				model.setModelTime((float) x);
				notifyChange();
				return true;
			}
			else if (s0 == "temperature") {
				model.setTemperature(x);
				notifyChange();
				return true;
			}
			else if (s0 == "dielectric") {
				model.universe.setDielectricConstant((float) x);
				notifyChange();
				return true;
			}
			else if (s0 == "viscosity") {
				model.universe.setViscosity((float) x);
				notifyChange();
				return true;
			}
			else if (s0 == "timestep") {
				model.setTimeStepAndAdjustReminder(x);
				notifyChange();
				return true;
			}
			else if (s0 == "heatbath") {
				model.activateHeatBath(x > 0);
				if (model.heatBathActivated())
					model.getHeatBath().setExpectedTemperature(x);
				notifyChange();
				return true;
			}
			else if (s0 == "cell_x") {
				if (model.boundary.getType() == Boundary.DBC_ID) {
					out(ScriptEvent.HARMLESS, "No effect for default boundary.");
				}
				else {
					model.boundary.x = x * IR_CONVERTER;
					notifyChange();
				}
				return true;
			}
			else if (s0 == "cell_y") {
				if (model.boundary.getType() == Boundary.DBC_ID) {
					out(ScriptEvent.HARMLESS, "No effect for default boundary.");
				}
				else {
					model.boundary.y = x * IR_CONVERTER;
					notifyChange();
				}
				return true;
			}
			else if (s0 == "cell_width") {
				if (model.boundary.getType() == Boundary.DBC_ID) {
					out(ScriptEvent.HARMLESS, "No effect for default boundary.");
				}
				else {
					model.boundary.width = x * IR_CONVERTER;
					notifyChange();
				}
				return true;
			}
			else if (s0 == "cell_height") {
				if (model.boundary.getType() == Boundary.DBC_ID) {
					out(ScriptEvent.HARMLESS, "No effect for default boundary.");
				}
				else {
					model.boundary.height = x * IR_CONVERTER;
					notifyChange();
				}
				return true;
			}
			else if (s0 == "width") {
				final Dimension dim = new Dimension((int) (x * IR_CONVERTER), view.getHeight());
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						view.setSize(dim);
					}
				});
				notifyChange();
				return true;
			}
			else if (s0 == "height") {
				final Dimension dim = new Dimension(view.getWidth(), (int) (x * IR_CONVERTER));
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						view.setSize(dim);
					}
				});
				notifyChange();
				return true;
			}
			else if (s0 == "gfield") {
				if (x < 0) {
					out(ScriptEvent.FAILED, "Illegal parameter: gravitational acceleration cannot be negative: " + x);
					return false;
				}
				if (x < ZERO) {
					model.removeField(GravitationalField.class.getName());
				}
				else {
					if (model.getNonLocalField(GravitationalField.class.getName()) == null)
						model.addNonLocalField(new GravitationalField(view.getBounds()));
					model.setGField(x);
				}
				notifyChange();
				return true;
			}
			else if (s0 == "efield") {
				if (Math.abs(x) < ZERO) {
					model.removeField(ElectricField.class.getName());
				}
				else {
					if (model.getNonLocalField(ElectricField.class.getName()) == null)
						model.addNonLocalField(new ElectricField(view.getBounds()));
					model.setEField(x);
				}
				notifyChange();
				return true;
			}
			else if (s0 == "bfield") {
				if (Math.abs(x) < ZERO) {
					model.removeField(MagneticField.class.getName());
				}
				else {
					if (model.getNonLocalField(MagneticField.class.getName()) == null)
						model.addNonLocalField(new MagneticField(view.getBounds()));
					model.setBField(x);
				}
				notifyChange();
				return true;
			}
			else if (s0 == "weight") {
				LineComponent[] lines = view.getLines();
				for (LineComponent lc : lines) {
					if (lc.isSelected())
						lc.setLineWeight((byte) x);
				}
				RectangleComponent[] rects = view.getRectangles();
				for (RectangleComponent rc : rects) {
					if (rc.isSelected())
						rc.setLineWeight((byte) x);
				}
				EllipseComponent[] ellipses = view.getEllipses();
				for (EllipseComponent ec : ellipses) {
					if (ec.isSelected())
						ec.setLineWeight((byte) x);
				}
				return true;
			}
		}
		else {

			if (s.length == 4) {

				if (model instanceof AtomicModel) {
					AtomicModel am = (AtomicModel) model;
					matcher = SET_EPSILON.matcher(str);
					if (matcher.find()) {
						double x = parseMathExpression(s[1]);
						if (Double.isNaN(x))
							return false;
						int i1 = (int) Math.round(x);
						if (i1 < 0 || i1 > Element.ID_CK) {
							out(ScriptEvent.FAILED, "Unsupported element: type " + i1);
							return false;
						}
						x = parseMathExpression(s[2]);
						if (Double.isNaN(x))
							return false;
						int i2 = (int) Math.round(x);
						if (i2 < 0 || i2 > Element.ID_CK) {
							out(ScriptEvent.FAILED, "Unsupported element: type " + i2);
							return false;
						}
						double eps = parseMathExpression(s[3]);
						if (Double.isNaN(eps))
							return false;
						if (i1 == i2) {
							am.getElement(i1).setEpsilon(eps);
						}
						else {
							Element e1 = am.getElement(i1);
							Element e2 = am.getElement(i2);
							am.getAffinity().setLBMixing(e1, e2, false);
							am.getAffinity().setEpsilon(e1, e2, eps);
						}
						notifyChange();
						return true;
					}
				}

			}

		}

		out(ScriptEvent.FAILED, "Unrecognized type of parameter to set: " + str);
		return false;

	}

	private boolean evaluateAddClause(String str) {

		Matcher matcher = ATOM.matcher(str);
		if (matcher.find()) {
			if (!(model instanceof AtomicModel))
				return false;
			str = str.substring(matcher.end()).trim();
			float x = 1, y = 1;
			String s1 = null;
			int space = str.indexOf(" ");
			if (space < 0) {
				s1 = str.substring(0).trim();
				RectangularBoundary boundary = model.getBoundary();
				x = (float) (Math.random() * boundary.width + boundary.x);
				y = (float) (Math.random() * boundary.height + boundary.y);
			}
			else {
				s1 = str.substring(0, space).trim();
				String s2 = str.substring(space + 1).trim();
				float[] r = parseCoordinates(s2);
				if (r != null) {
					x = r[0];
					y = r[1];
				}
				else {
					out(ScriptEvent.FAILED, "Error: Cannot parse " + str);
					return false;
				}
			}
			double a = parseMathExpression(s1);
			if (Double.isNaN(a))
				return false;
			int id = (int) Math.round(a);
			if (((AtomisticView) view).insertAnAtom(x, y, id, true, false)) {
				view.repaint();
				notifyChange();
			}
			else {
				out(ScriptEvent.HARMLESS, "Cannot insert an atom to the specified location: " + str);
			}
			return true;
		}

		if (model instanceof MolecularModel) {
			matcher = OBSTACLE.matcher(str);
			if (matcher.find()) {
				str = str.substring(matcher.end()).trim();
				float[] x = parseArray(4, str);
				if (x != null) {
					for (int i = 0; i < 4; i++)
						x[i] *= IR_CONVERTER;
					RectangularObstacle obs = new RectangularObstacle(x[0], x[1], x[2], x[3]);
					if (((AtomisticView) view).intersects(obs)) {
						out(ScriptEvent.FAILED,
								"Cannot add an obstacle of the specified size to the specified location: " + str);
					}
					else {
						model.obstacles.add(obs);
					}
					return true;
				}
			}
		}

		matcher = IMAGE.matcher(str);
		if (matcher.find()) {
			str = str.substring(matcher.end()).trim();
			matcher = IMAGE_EXTENSION.matcher(str);
			if (matcher.find()) {
				String address = str.substring(0, matcher.end()).trim();
				if (FileUtilities.isRelative(address)) {
					String base = (String) model.getProperty("url");
					if (base == null) {
						out(ScriptEvent.FAILED, "No directory has been specified. Save the page first.");
						return false;
					}
					address = FileUtilities.getCodeBase(base) + address;
					if (System.getProperty("os.name").startsWith("Windows"))
						address = address.replace('\\', '/');
				}
				address = useDefinitions(address);
				ImageComponent ic = null;
				try {
					ic = new ImageComponent(address);
				}
				catch (Exception e) {
					e.printStackTrace();
					final String errorAddress = address;
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(view), "Image "
									+ errorAddress + " was not found.", "Image not found", JOptionPane.ERROR_MESSAGE);
						}
					});
					return false;
				}
				str = str.substring(matcher.end()).trim();
				int i = str.indexOf(",");
				if (i >= 0) {
					float[] x = parseCoordinates(str);
					if (x != null) {
						ic.setLocation(x[0], x[1]);
						view.addLayeredComponent(ic);
						view.repaint();
						return true;
					}
				}
				else {
					str = str.toLowerCase();
					if (str.startsWith("atomno")) {
						i = str.indexOf("=");
						if (i >= 0) {
							str = str.substring(i + 1);
							double z = parseMathExpression(str);
							if (!Double.isNaN(z)) {
								ic.setHost(model.getParticle((int) Math.round(z)));
								view.addLayeredComponent(ic);
								view.repaint();
								return true;
							}
						}
					}
				}
			}
		}

		matcher = TEXTBOX.matcher(str);
		if (matcher.find()) {
			str = str.substring(matcher.end()).trim();
			matcher = TEXT_EXTENSION.matcher(str);
			if (matcher.find()) {
				String address = str.substring(0, matcher.end()).trim();
				if (FileUtilities.isRelative(address)) {
					String base = (String) model.getProperty("url");
					if (base == null) {
						out(ScriptEvent.FAILED, "No directory has been specified. Save the page first.");
						return false;
					}
					address = FileUtilities.getCodeBase(base) + address;
					if (System.getProperty("os.name").startsWith("Windows"))
						address = address.replace('\\', '/');
				}
				TextBoxComponent t = null;
				try {
					t = new TextBoxComponent(readText(address, view));
				}
				catch (InterruptedException e) {
				}
				if (t != null) {
					str = str.substring(matcher.end()).trim();
					float[] x = parseCoordinates(str);
					if (x != null) {
						t.setLocation(x[0], x[1]);
						view.addLayeredComponent(t);
						view.repaint();
						return true;
					}
				}
			}
			else {
				String slc = str.toLowerCase();
				int a = slc.indexOf("<t>");
				int b = slc.indexOf("</t>");
				if (a != -1 && b != -1) {
					TextBoxComponent t = new TextBoxComponent(str.substring(a + 3, b));
					view.addLayeredComponent(t);
					str = str.substring(b + 4).trim();
					float[] x = parseCoordinates(str);
					if (x != null) {
						t.setLocation(x[0], x[1]);
						view.repaint();
						return true;
					}
					out(ScriptEvent.FAILED, "Coordinate error: " + str);
					return false;
				}
				return true;
			}
		}

		matcher = LINE.matcher(str);
		if (matcher.find()) {
			str = str.substring(matcher.end()).trim();
			float[] x = parseArray(4, str);
			if (x != null) {
				for (int i = 0; i < 4; i++)
					x[i] *= IR_CONVERTER;
				LineComponent line = new LineComponent();
				line.setEndPoint1(x[0], x[1]);
				line.setEndPoint2(x[2], x[3]);
				view.addLayeredComponent(line);
				return true;
			}
		}

		matcher = RECTANGLE.matcher(str);
		if (matcher.find()) {
			str = str.substring(matcher.end()).trim();
			float[] x = parseArray(4, str);
			if (x != null) {
				for (int i = 0; i < 4; i++)
					x[i] *= IR_CONVERTER;
				RectangleComponent rect = new RectangleComponent();
				rect.setRect(x[0], x[1], x[2], x[3]);
				view.addLayeredComponent(rect);
				return true;
			}
		}

		matcher = ELLIPSE.matcher(str);
		if (matcher.find()) {
			str = str.substring(matcher.end()).trim();
			float[] x = parseArray(4, str);
			if (x != null) {
				for (int i = 0; i < 4; i++)
					x[i] *= IR_CONVERTER;
				EllipseComponent ellipse = new EllipseComponent();
				ellipse.setOval(x[0], x[1], x[2], x[3]);
				view.addLayeredComponent(ellipse);
				return true;
			}
		}

		out(ScriptEvent.FAILED, "Unrecognized type of object to add: " + str);
		return false;

	}

	private boolean evaluateAttachClause(String str) {
		if (!(model instanceof MolecularModel))
			return false;
		if (str == null)
			return false;
		if (str.trim().length() == 0)
			return false;
		String lcStr = str.toLowerCase();
		if (lcStr.startsWith("line")) {
			str = lcStr.substring(4).trim();
			String[] s = str.split("\\s+");
			if (s.length < 4)
				return false;
			for (int i = 0; i < s.length; i++)
				s[i] = s[i].trim();
			int i1 = Math.round(Float.parseFloat(s[0]));
			LineComponent lc = view.getLine(i1);
			if (lc == null) {
				out(ScriptEvent.FAILED, "Line " + i1 + " does not exist.");
				return false;
			}
			int i2 = Math.round(Float.parseFloat(s[3]));
			if ("atom".equals(s[2])) {
				Atom at = ((MolecularModel) model).getAtom(i2);
				if (at == null) {
					out(ScriptEvent.FAILED, "Atom " + i2 + " does not exist.");
					return false;
				}
				lc.setHost(at);
				view.repaint();
			}
			else if ("bond".equals(s[2])) {
				RadialBond rb = ((MolecularModel) model).getBonds().get(i2);
				if (rb == null) {
					out(ScriptEvent.FAILED, "Radial bond " + i2 + " does not exist.");
					return false;
				}
				lc.setHost(rb);
				view.repaint();
			}
			return true;
		}
		else if (lcStr.startsWith("rectangle")) {
			str = lcStr.substring(9).trim();
			String[] s = str.split("\\s+");
			if (s.length < 4)
				return false;
			for (int i = 0; i < s.length; i++)
				s[i] = s[i].trim();
			int i1 = Math.round(Float.parseFloat(s[0]));
			RectangleComponent rc = view.getRectangle(i1);
			if (rc == null) {
				out(ScriptEvent.FAILED, "Rectangle " + i1 + " does not exist.");
				return false;
			}
			int i2 = Math.round(Float.parseFloat(s[3]));
			if ("atom".equals(s[2])) {
				Atom at = ((MolecularModel) model).getAtom(i2);
				if (at == null) {
					out(ScriptEvent.FAILED, "Atom " + i2 + " does not exist.");
					return false;
				}
				rc.setHost(at);
				view.repaint();
			}
			return true;
		}
		else if (lcStr.startsWith("ellipse")) {
			str = lcStr.substring(7).trim();
			String[] s = str.split("\\s+");
			if (s.length < 4)
				return false;
			for (int i = 0; i < s.length; i++)
				s[i] = s[i].trim();
			int i1 = Math.round(Float.parseFloat(s[0]));
			EllipseComponent ec = view.getEllipse(i1);
			if (ec == null) {
				out(ScriptEvent.FAILED, "Ellipse " + i1 + " does not exist.");
				return false;
			}
			int i2 = Math.round(Float.parseFloat(s[3]));
			if ("atom".equals(s[2])) {
				Atom at = ((MolecularModel) model).getAtom(i2);
				if (at == null) {
					out(ScriptEvent.FAILED, "Atom " + i2 + " does not exist.");
					return false;
				}
				ec.setHost(at);
				view.repaint();
			}
			return true;
		}
		else if (lcStr.startsWith("image")) {
			str = lcStr.substring(5).trim();
			String[] s = str.split("\\s+");
			if (s.length < 4)
				return false;
			for (int i = 0; i < s.length; i++)
				s[i] = s[i].trim();
			int i1 = Math.round(Float.parseFloat(s[0]));
			ImageComponent ic = view.getImage(i1);
			if (ic == null) {
				out(ScriptEvent.FAILED, "Image " + i1 + " does not exist.");
				return false;
			}
			int i2 = Math.round(Float.parseFloat(s[3]));
			if ("atom".equals(s[2])) {
				Atom at = ((MolecularModel) model).getAtom(i2);
				ic.setHost(null);
				ic.setHost(at);
				view.repaint();
			}
			else if ("bond".equals(s[2])) {
				RadialBond rb = ((MolecularModel) model).getBonds().get(i2);
				if (rb == null) {
					out(ScriptEvent.FAILED, "Radial bond " + i2 + " does not exist.");
					return false;
				}
				ic.setHost(rb);
				view.repaint();
			}
			return true;
		}
		else if (lcStr.startsWith("textbox")) {
			str = lcStr.substring(7).trim();
			String[] s = str.split("\\s+");
			if (s.length < 4)
				return false;
			for (int i = 0; i < s.length; i++)
				s[i] = s[i].trim();
			int i1 = Math.round(Float.parseFloat(s[0]));
			TextBoxComponent tc = view.getTextBox(i1);
			if (tc == null) {
				out(ScriptEvent.FAILED, "Text box " + i1 + " does not exist.");
				return false;
			}
			int i2 = Math.round(Float.parseFloat(s[3]));
			if ("atom".equals(s[2])) {
				Atom at = ((MolecularModel) model).getAtom(i2);
				if (at == null) {
					out(ScriptEvent.FAILED, "Atom " + i2 + " does not exist.");
					return false;
				}
				tc.setHost(at);
				tc.setAttachmentPosition(TextBoxComponent.BOX_CENTER);
				view.repaint();
			}
			return true;
		}
		return false;
	}

	private boolean evaluateSoundClause(String address) {
		if (address == null)
			return false;
		if (FileUtilities.isRelative(address)) {
			Object o = model.getProperty("url");
			if (o == null) {
				out(ScriptEvent.FAILED, "Codebase missing.");
				return false;
			}
			address = FileUtilities.getCodeBase((String) o) + address;
		}
		playSound(address);
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
			Object o = model.getProperty("url");
			if (o == null) {
				out(ScriptEvent.FAILED, "Codebase missing.");
				return false;
			}
			s = FileUtilities.getCodeBase((String) o) + s;
		}
		Cursor c = loadCursor(s, hotspot != null ? (int) hotspot[0] : 0, hotspot != null ? (int) hotspot[1] : 0);
		if (c == null) {
			out(ScriptEvent.FAILED, "Failed in loading cursor image: " + s);
			return false;
		}
		view.setExternalCursor(c);
		return true;
	}

	private boolean evaluateMoveClause(String str) {
		float[] x = parseCoordinates(str);
		if (x == null)
			return false;
		int n = model.getNumberOfParticles();
		for (int k = 0; k < n; k++) {
			if (model.getParticle(k).isSelected())
				model.getParticle(k).translateBy(x[0], x[1]);
		}
		if (model.obstacles != null && !model.obstacles.isEmpty()) {
			RectangularObstacle obs;
			synchronized (model.obstacles.getSynchronizationLock()) {
				for (Iterator it = model.obstacles.iterator(); it.hasNext();) {
					obs = (RectangularObstacle) it.next();
					if (obs.isSelected())
						obs.translateBy(x[0], x[1]);
				}
			}
		}
		Layered[] lc = view.getLayeredComponents();
		for (int k = 0; k < lc.length; k++)
			if (((ModelComponent) lc[k]).isSelected())
				lc[k].translateBy(x[0], x[1]);
		if (view instanceof AtomisticView) {
			((AtomisticView) view).refreshJmol();
		}
		view.repaint();
		notifyChange();
		return true;
	}

	private boolean evaluateRotateClause(String str) {
		double x = parseMathExpression(str);
		if (Double.isNaN(x))
			return false;
		if (!model.rotateSelectedParticles(x)) {
			out(ScriptEvent.FAILED,
					"Rotation by this angle is not permitted, because some particles may be placed in inappropriate locations.");
		}
		if (view instanceof AtomisticView) {
			((AtomisticView) view).refreshJmol();
		}
		view.repaint();
		notifyChange();
		return true;
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
				int i = Math.round(Float.valueOf(str).floatValue() / (float) model.getTimeStep());
				int step0 = model.job != null ? model.job.getIndexOfStep() : 0;
				DelayModelTimeLoadable l = new DelayModelTimeLoadable(i) {
					public void execute() {
						// if (model.job.getIndexOfStep() - step0 < i - 1) return; // what the hell is this?
						synchronized (Eval2D.this) {
							Eval2D.this.notifyAll();
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
						Eval2D.this.wait();
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
		if (model.isLoading())
			return false;
		try {
			Thread.sleep(100); // sleep 100 ms in order for the force calculation to finish
		}
		catch (InterruptedException e) {
			throw new InterruptedException();
		}
		if (FileUtilities.isRelative(address)) {
			Object o = model.getProperty("url");
			if (o == null) {
				out(ScriptEvent.FAILED, "Codebase missing.");
				return false;
			}
			address = FileUtilities.getCodeBase((String) o) + address;
		}
		view.removeAllLayeredComponents(); // make sure they are gone in case of an exception
		if (FileUtilities.isRemote(address)) {
			URL url = null;
			try {
				url = new URL(FileUtilities.httpEncode(address));
			}
			catch (MalformedURLException e) {
				e.printStackTrace();
			}
			if (url != null) {
				ConnectionManager.sharedInstance().setCheckUpdate(true);
				model.input(url);
			}
		}
		else {
			model.input(new File(address));
		}
		/* Do we really need to sleep in the following? */
		try {
			Thread.sleep(100);
		}
		catch (InterruptedException e) {
			stop();
			throw new InterruptedException();
		}
		notifyChange();
		model.setProgress(0, "Done");
		return true;
	}

	// synchronization
	private synchronized boolean evaluateSourceClause(String address) throws InterruptedException {
		String s = readText(address, view);
		if (s != null) {
			stop = true;
			appendScript(s);
			evaluate();
			return true;
		}
		return false;
	}

	// synchronization prevents two minimizers to run at the same time.
	private synchronized boolean evaluateMinimizeClause(String str) throws InterruptedException {
		if (!(model instanceof MolecularModel))
			return false;
		if (str == null || str.trim().equals(""))
			return false;
		String[] s = str.split(REGEX_WHITESPACE);
		List<String> list = new ArrayList<String>();
		for (String si : s) {
			if (si.trim().equals(""))
				continue;
			list.add(si.trim());
		}
		float x = 0;
		int y = 0;
		String t = "SD";
		switch (list.size()) {
		case 2:
			try {
				x = Float.parseFloat(list.get(0)) * 10;
				y = (int) Float.parseFloat(list.get(1));
			}
			catch (NumberFormatException e) {
				out(ScriptEvent.FAILED, "Unable to parse number: " + str);
				return false;
			}
			break;
		case 3:
			try {
				x = Float.parseFloat(list.get(1)) * 10;
				y = (int) Float.parseFloat(list.get(2));
			}
			catch (NumberFormatException e) {
				out(ScriptEvent.FAILED, "Unable to parse number: " + str);
				return false;
			}
			t = list.get(0);
			break;
		}
		if (x > 0 && y > 0) {
			Minimizer m = new Minimizer((MolecularModel) model);
			float pot = 0;
			boolean b = t.equalsIgnoreCase("SD");
			for (int i = 0; i < y; i++) {
				pot = b ? (float) m.sd(x) : (float) m.cg(x);
				if (i % 20 == 0) {
					view.getGraphics().drawString(i + " steps, V = " + pot + " eV", 5, 20);
					view.repaint();
					try {
						Thread.sleep(50);
					}
					catch (InterruptedException e) {
						stop();
						throw new InterruptedException();
					}
				}
			}
			notifyChange();
			return true;
		}
		out(ScriptEvent.FAILED, "Syntax error: " + str);
		return false;
	}

	private boolean evaluatePrintClause(String str) {
		if (str == null)
			return false;
		str = format(str);
		// average over the selected particles
		Matcher matcher = MEAN.matcher(str);
		if (matcher.find()) {
			out(ScriptEvent.SUCCEEDED, str + " = " + getAverage(str.substring(1, str.length() - 1)));
			return true;
		}
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
		model.setChannel(i, x);
		return true;
	}

	private double getAverage(String s) {
		if (s == null || s.trim().equals(""))
			return 0;
		int n = model.getNumberOfParticles();
		Particle[] p = null;
		if (model instanceof MesoModel)
			p = ((MesoModel) model).getParticles();
		else p = ((AtomicModel) model).getAtoms();
		double value = 0;
		if ("rx".equalsIgnoreCase(s)) {
			value = Statistics.getMeanRx(0, n, p, true) * R_CONVERTER;
		}
		else if ("ry".equalsIgnoreCase(s)) {
			value = Statistics.getMeanRy(0, n, p, true) * R_CONVERTER;
		}
		else if ("vx".equalsIgnoreCase(s)) {
			value = Statistics.getMeanVx(0, n, p, true) * V_CONVERTER;
		}
		else if ("vy".equalsIgnoreCase(s)) {
			value = Statistics.getMeanVy(0, n, p, true) * V_CONVERTER;
		}
		else if ("rxsq".equalsIgnoreCase(s)) {
			value = Statistics.getMeanRx(0, n, p, true) * R_CONVERTER;
			value *= value;
		}
		else if ("rysq".equalsIgnoreCase(s)) {
			value = Statistics.getMeanRy(0, n, p, true) * R_CONVERTER;
			value *= value;
		}
		else if ("vxsq".equalsIgnoreCase(s)) {
			value = Statistics.getMeanVx(0, n, p, true) * V_CONVERTER;
			value *= value;
		}
		else if ("vysq".equalsIgnoreCase(s)) {
			value = Statistics.getMeanVy(0, n, p, true) * V_CONVERTER;
			value *= value;
		}
		return value;
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
		String[] s = str.split(",");
		for (int i = 0; i < s.length; i++)
			s[i] = s[i].trim();
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
				y[i].setName(expression[0] + " : " + expression[i + 1]);
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
			s = useParticleVariables(str, k);
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

	private void setParticleField(String str1, String str2, double x) {
		if (model instanceof MolecularModel) {
			setAtomField(str1, str2, x);
			return;
		}
		MesoModel m = (MesoModel) model;
		int lb = str1.indexOf("[");
		int rb = str1.indexOf("]");
		double z = parseMathExpression(str1.substring(lb + 1, rb));
		if (Double.isNaN(z))
			return;
		int i = (int) Math.round(z);
		if (i < 0 || i >= m.numberOfParticles) {
			out(ScriptEvent.FAILED, "Particle " + i + " doesn't exisit.");
			return;
		}
		String s = str2.toLowerCase().intern();
		boolean b = true;
		if (s == "color")
			m.gb[i].color = new Color((int) x);
		else if (s == "custom")
			m.gb[i].custom = (float) x;
		else if (s == "rx")
			m.gb[i].rx = x * IR_CONVERTER;
		else if (s == "ry")
			m.gb[i].ry = x * IR_CONVERTER;
		else if (s == "theta")
			m.gb[i].theta = x;
		else if (s == "vx")
			m.gb[i].vx = x * IV_CONVERTER;
		else if (s == "vy")
			m.gb[i].vy = x * IV_CONVERTER;
		else if (s == "omega")
			m.gb[i].omega = x;
		else if (s == "ax")
			m.gb[i].ax = x;
		else if (s == "ay")
			m.gb[i].ay = x;
		else if (s == "alpha")
			m.gb[i].alpha = x;
		else if (s == "hx")
			m.gb[i].hx = (float) x * R_CONVERTER;
		else if (s == "hy")
			m.gb[i].hy = (float) x * R_CONVERTER;
		else if (s == "gamma")
			m.gb[i].gamma = (float) x;
		else if (s == "charge")
			m.gb[i].charge = x;
		else if (s == "dipole")
			m.gb[i].dipoleMoment = x;
		else if (s == "mass")
			m.gb[i].mass = x / M_CONVERTER;
		else if (s == "friction")
			m.gb[i].friction = (float) x;
		else if (s == "inertia")
			m.gb[i].inertia = x;
		else if (s == "breadth")
			m.gb[i].breadth = x * IR_CONVERTER;
		else if (s == "length")
			m.gb[i].length = x * IR_CONVERTER;
		else if (s == "epsilon")
			m.gb[i].epsilon0 = x;
		else if (s == "restraint") {
			if (m.gb[i].restraint == null)
				m.gb[i].restraint = new PointRestraint(x * 0.01, m.gb[i].rx, m.gb[i].ry);
			else m.gb[i].restraint.k = x * 0.01;
		}
		else if (s == "restraint.x") {
			if (m.gb[i].restraint != null)
				m.gb[i].restraint.x0 = x * IR_CONVERTER;
		}
		else if (s == "restraint.y") {
			if (m.gb[i].restraint != null)
				m.gb[i].restraint.y0 = x * IR_CONVERTER;
		}
		else {
			out(ScriptEvent.FAILED, "Cannot set propery: " + str2);
			b = false;
		}
		if (b)
			notifyChange();
	}

	private void setAtomField(String str1, String str2, double x) {
		if (!(model instanceof MolecularModel))
			return;
		MolecularModel m = (MolecularModel) model;
		int lb = str1.indexOf("[");
		int rb = str1.indexOf("]");
		double z = parseMathExpression(str1.substring(lb + 1, rb));
		if (Double.isNaN(z))
			return;
		int i = (int) Math.round(z);
		if (i < 0 || i >= m.numberOfAtoms) {
			out(ScriptEvent.FAILED, "Atom " + i + " doesn't exisit.");
			return;
		}
		String s = str2.toLowerCase().intern();
		boolean b = true;
		if (s == "color")
			m.atom[i].color = new Color((int) x);
		else if (s == "custom")
			m.atom[i].custom = (float) x;
		else if (s == "id")
			m.atom[i].setElement(m.getElement((int) x));
		else if (s == "rx")
			m.atom[i].rx = x * IR_CONVERTER;
		else if (s == "ry")
			m.atom[i].ry = x * IR_CONVERTER;
		else if (s == "vx")
			m.atom[i].vx = x * IV_CONVERTER;
		else if (s == "vy")
			m.atom[i].vy = x * IV_CONVERTER;
		else if (s == "ax")
			m.atom[i].ax = x;
		else if (s == "ay")
			m.atom[i].ay = x;
		else if (s == "hx")
			m.atom[i].hx = (float) x * R_CONVERTER;
		else if (s == "hy")
			m.atom[i].hy = (float) x * R_CONVERTER;
		else if (s == "charge") {
			m.atom[i].charge = x;
			m.checkCharges();
		}
		else if (s == "friction")
			m.atom[i].friction = (float) x;
		else if (s == "restraint") {
			if (m.atom[i].restraint == null) {
				m.atom[i].restraint = new PointRestraint(x * 0.01, m.atom[i].rx, m.atom[i].ry);
			}
			else {
				m.atom[i].restraint.k = x * 0.01;
			}
		}
		else if (s == "restraint.x") {
			if (m.atom[i].restraint != null)
				m.atom[i].restraint.x0 = x * IR_CONVERTER;
		}
		else if (s == "restraint.y") {
			if (m.atom[i].restraint != null)
				m.atom[i].restraint.y0 = x * IR_CONVERTER;
		}
		else {
			out(ScriptEvent.FAILED, "Cannot set propery: " + str2);
			b = false;
		}
		if (b)
			notifyChange();
	}

	private void setParticleField(String str1, String str2, boolean x) {
		int lb = str1.indexOf("[");
		int rb = str1.indexOf("]");
		double z = parseMathExpression(str1.substring(lb + 1, rb));
		if (Double.isNaN(z))
			return;
		int i = (int) Math.round(z);
		if (i < 0 || i >= model.getNumberOfParticles()) {
			out(ScriptEvent.FAILED, "Particle " + i + " doesn't exisit.");
			return;
		}
		String s = str2.toLowerCase().intern();
		boolean b = true;
		if (s == "visible")
			model.getParticle(i).setVisible(x);
		else if (s == "movable")
			model.getParticle(i).setMovable(x);
		else if (s == "draggable")
			model.getParticle(i).setDraggable(x);
		else {
			out(ScriptEvent.FAILED, "Cannot set propery: " + str2);
			b = false;
		}
		if (b) {
			view.repaint();
			notifyChange();
		}
	}

	private void setRbondField(String str1, String str2, double x) {
		if (!(model instanceof MolecularModel))
			return;
		MolecularModel m = (MolecularModel) model;
		int lb = str1.indexOf("[");
		int rb = str1.indexOf("]");
		double z = parseMathExpression(str1.substring(lb + 1, rb));
		if (Double.isNaN(z))
			return;
		int i = (int) Math.round(z);
		if (i >= m.bonds.size() || i < 0) {
			out(ScriptEvent.FAILED, "Radial bond " + i + " doesn't exisit.");
			return;
		}
		String s = str2.toLowerCase().intern();
		boolean b = true;
		if (s == "color")
			m.bonds.get(i).setBondColor(new Color((int) x));
		else if (s == "strength") {
			if (Math.abs(x) < ZERO) {
				m.bonds.remove(m.bonds.get(i));
				MoleculeCollection.sort(m);
			}
			else {
				m.bonds.get(i).setBondStrength(x);
			}
		}
		else if (s == "custom")
			m.bonds.get(i).custom = (float) x;
		else if (s == "bondlength")
			m.bonds.get(i).setBondLength(x * IR_CONVERTER);
		else if (s == "amplitude")
			m.bonds.get(i).setAmplitude(0.001f * (float) x);
		else if (s == "period")
			m.bonds.get(i).setPeriod((int) x);
		else if (s == "phase")
			m.bonds.get(i).setPhase((float) (x * Math.PI / 180));
		else if (s == "torque") {
			if (Math.abs(x) < ZERO) {
				m.bonds.get(i).setTorque(0);
			}
			else {
				m.bonds.get(i).setTorque((float) x);
			}
		}
		else {
			out(ScriptEvent.FAILED, "Cannot set propery: " + str2);
			b = false;
		}
		if (b)
			notifyChange();
	}

	private void setRbondField(String str1, String str2, String x) {
		if (!(model instanceof MolecularModel))
			return;
		MolecularModel m = (MolecularModel) model;
		int lb = str1.indexOf("[");
		int rb = str1.indexOf("]");
		double z = parseMathExpression(str1.substring(lb + 1, rb));
		if (Double.isNaN(z))
			return;
		int i = (int) Math.round(z);
		if (i >= m.bonds.size() || i < 0) {
			out(ScriptEvent.FAILED, "Radial bond " + i + " doesn't exisit.");
			return;
		}
		String s = str2.toLowerCase().intern();
		boolean b = true;
		if (s == "visible") {
			m.bonds.get(i).setVisible("on".equalsIgnoreCase(x) || "true".equalsIgnoreCase(x));
			m.notifyBondChangeListeners();
		}
		else if (s == "style") {
			for (Field f : RadialBond.class.getFields()) {
				if (f.getName().equalsIgnoreCase(x)) {
					try {
						m.bonds.get(i).setBondStyle(f.getByte(null));
					}
					catch (IllegalArgumentException e) {
						e.printStackTrace();
					}
					catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
		}
		else if (s == "torquetype") {
			for (Field f : RadialBond.class.getFields()) {
				if (f.getName().equalsIgnoreCase(x)) {
					try {
						m.bonds.get(i).setTorqueType(f.getByte(null));
					}
					catch (IllegalArgumentException e) {
						e.printStackTrace();
					}
					catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
		}
		else {
			out(ScriptEvent.FAILED, "Cannot set propery: " + str2);
			b = false;
		}
		if (b) {
			view.repaint();
			notifyChange();
		}
	}

	private void setAbondField(String str1, String str2, double x) {
		if (!(model instanceof MolecularModel))
			return;
		MolecularModel m = (MolecularModel) model;
		int lb = str1.indexOf("[");
		int rb = str1.indexOf("]");
		double z = parseMathExpression(str1.substring(lb + 1, rb));
		if (Double.isNaN(z))
			return;
		int i = (int) Math.round(z);
		if (i >= m.bends.size() || i < 0) {
			out(ScriptEvent.FAILED, "Angular bond " + i + " doesn't exisit.");
			return;
		}
		String s = str2.toLowerCase().intern();
		boolean b = true;
		if (s == "strength")
			if (Math.abs(x) < ZERO) {
				m.bends.remove(m.bends.get(i));
			}
			else {
				m.bends.get(i).setBondStrength(x);
			}
		else if (s == "bondangle")
			m.bends.get(i).setBondAngle(x);
		else {
			out(ScriptEvent.FAILED, "Cannot set propery: " + str2);
			b = false;
		}
		if (b)
			notifyChange();
	}

	private void setElementField(String str1, String str2, double x) {
		if (!(model instanceof MolecularModel))
			return;
		MolecularModel m = (MolecularModel) model;
		int lb = str1.indexOf("[");
		int rb = str1.indexOf("]");
		double z = parseMathExpression(str1.substring(lb + 1, rb));
		if (Double.isNaN(z))
			return;
		int i = (int) Math.round(z);
		if (i >= Element.NMAX || i < 0) {
			out(ScriptEvent.FAILED, "Element " + i + " doesn't exisit.");
			return;
		}
		String s = str2.toLowerCase().intern();
		boolean b = true;
		if (s == "mass")
			m.getElement(i).setMass(x / M_CONVERTER);
		else if (s == "sigma")
			m.getElement(i).setSigma(x * IR_CONVERTER);
		else if (s == "epsilon")
			m.getElement(i).setEpsilon(x);
		else {
			out(ScriptEvent.FAILED, "Cannot set propery: " + str2);
			b = false;
		}
		if (b)
			notifyChange();
	}

	private void setObstacleField(String str1, String str2, double x) {
		if (!(model instanceof MolecularModel))
			return;
		MolecularModel m = (MolecularModel) model;
		if (m.obstacles == null)
			return;
		int lb = str1.indexOf("[");
		int rb = str1.indexOf("]");
		double z = parseMathExpression(str1.substring(lb + 1, rb));
		if (Double.isNaN(z))
			return;
		int i = (int) Math.round(z);
		if (i < 0 || i >= m.obstacles.size()) {
			out(ScriptEvent.FAILED, "Obstacle " + i + " doesn't exisit.");
			return;
		}
		RectangularObstacle obs = m.obstacles.get(i);
		String s = str2.toLowerCase().intern();
		boolean b = true;
		if (s == "x")
			obs.translateTo(x * IR_CONVERTER, obs.y);
		else if (s == "y")
			obs.translateTo(obs.x, x * IR_CONVERTER);
		else if (s == "width")
			obs.setRect(obs.x, obs.y, x * IR_CONVERTER, obs.height);
		else if (s == "height")
			obs.setRect(obs.x, obs.y, obs.width, x * IR_CONVERTER);
		else if (s == "vx")
			obs.setVx(x * IV_CONVERTER);
		else if (s == "vy")
			obs.setVy(x * IV_CONVERTER);
		else if (s == "custom")
			obs.custom = (float) x;
		else if (s == "density")
			obs.setDensity(x * 0.01);
		else if (s == "friction")
			obs.setFriction((float) x);
		else if (s == "hx")
			obs.setHx((float) (x * 0.001));
		else if (s == "hy")
			obs.setHy((float) (x * 0.001));
		else if (s == "externalfx") // deprecated
			obs.setHx((float) (x * 0.001));
		else if (s == "externalfy") // deprecated
			obs.setHy((float) (x * 0.001));
		else if (s == "elasticity") {
			if (x > 1)
				x = 1;
			else if (x < 0)
				x = 0;
			obs.setElasticity((float) x);
		}
		else {
			out(ScriptEvent.FAILED, "Cannot set propery: " + str2);
			b = false;
		}
		if (b)
			notifyChange();
	}

	private void setObstacleField(String str1, String str2, boolean x) {
		if (!(model instanceof MolecularModel))
			return;
		MolecularModel m = (MolecularModel) model;
		if (m.obstacles == null)
			return;
		int lb = str1.indexOf("[");
		int rb = str1.indexOf("]");
		double z = parseMathExpression(str1.substring(lb + 1, rb));
		if (Double.isNaN(z))
			return;
		int i = (int) Math.round(z);
		if (i < 0 || i >= m.obstacles.size()) {
			out(ScriptEvent.FAILED, "Obstacle " + i + " doesn't exisit.");
			return;
		}
		RectangularObstacle obs = m.obstacles.get(i);
		String s = str2.toLowerCase().intern();
		boolean b = true;
		if (s == "visible") {
			obs.setVisible(x);
		}
		else if (s == "draggable") {
			obs.setDraggable(x);
		}
		else {
			out(ScriptEvent.FAILED, "Cannot set propery: " + str2);
			b = false;
		}
		if (b)
			notifyChange();
	}

	private void setImageField(String str1, String str2, String str3) {
		int lb = str1.indexOf("[");
		int rb = str1.indexOf("]");
		double z = parseMathExpression(str1.substring(lb + 1, rb));
		if (Double.isNaN(z))
			return;
		int i = (int) Math.round(z);
		ImageComponent[] ic = view.getImages();
		if (i < 0 || i >= ic.length) {
			out(ScriptEvent.FAILED, "Image " + i + " doesn't exisit.");
			return;
		}
		String s = str2.toLowerCase().intern();
		boolean b = true;
		if (s == "visible")
			ic[i].setVisible("on".equalsIgnoreCase(str3));
		else if (s == "draggable")
			ic[i].setDraggable("on".equalsIgnoreCase(str3));
		else if (s == "custom") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				ic[i].custom = (float) x;
		}
		else if (s == "x") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				ic[i].translateTo(x * IR_CONVERTER, ic[i].getRy());
		}
		else if (s == "y") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				ic[i].translateTo(ic[i].getRx(), x * IR_CONVERTER);
		}
		else if (s == "angle") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				ic[i].setAngle((float) Math.toRadians(x));
		}
		else if (s == "frame") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				ic[i].setCurrentFrame((int) Math.round(x));
		}
		else if (s == "loop") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				ic[i].setLoopCount((int) Math.round(x));
		}
		else if (s == "layer") {
			if ("in_front_of_particles".equalsIgnoreCase(str3)) {
				ic[i].setLayer(Layered.IN_FRONT_OF_PARTICLES);
			}
			else if ("behind_particles".equalsIgnoreCase(str3)) {
				ic[i].setLayer(Layered.BEHIND_PARTICLES);
			}
			else if ("back".equalsIgnoreCase(str3)) {
				view.sendLayerToBack(ic[i]);
			}
			else if ("front".equalsIgnoreCase(str3)) {
				view.bringLayerToFront(ic[i]);
			}
		}
		else {
			out(ScriptEvent.FAILED, "Cannot set propery: " + str2);
			b = false;
		}
		if (b) {
			view.repaint();
			notifyChange();
		}
	}

	private void setLineField(String str1, String str2, String str3) {
		int lb = str1.indexOf("[");
		int rb = str1.indexOf("]");
		double z = parseMathExpression(str1.substring(lb + 1, rb));
		if (Double.isNaN(z))
			return;
		int i = (int) Math.round(z);
		LineComponent[] c = view.getLines();
		if (i < 0 || i >= c.length) {
			out(ScriptEvent.FAILED, "Line " + i + " doesn't exisit.");
			return;
		}
		String s = str2.toLowerCase().intern();
		boolean b = true;
		if (s == "weight") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].setLineWeight((byte) x);
		}
		else if (s == "x1") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].setEndPoint1((float) (x * IR_CONVERTER), c[i].getY1());
		}
		else if (s == "y1") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].setEndPoint1(c[i].getX1(), (float) (x * IR_CONVERTER));
		}
		else if (s == "x2") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].setEndPoint2((float) (x * IR_CONVERTER), c[i].getY2());
		}
		else if (s == "y2") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].setEndPoint2(c[i].getX2(), (float) (x * IR_CONVERTER));
		}
		else if (s == "coordinates") {
			float[] x = parseArray(4, str3);
			if (x != null && x.length == 4) {
				c[i].setEndPoint1(x[0] * IR_CONVERTER, x[1] * IR_CONVERTER);
				c[i].setEndPoint2(x[2] * IR_CONVERTER, x[3] * IR_CONVERTER);
			}
		}
		else if (s == "stroke") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				if (x >= 0 && x < LineStyle.STROKES.length)
					c[i].setLineStyle((byte) x);
		}
		else if (s == "color") {
			Color color = parseRGBColor(str3);
			if (color != null)
				c[i].setColor(color);
		}
		else if (s == "visible") {
			c[i].setVisible("on".equalsIgnoreCase(str3));
		}
		else if (s == "draggable") {
			c[i].setDraggable("on".equalsIgnoreCase(str3));
		}
		else if (s == "layer") {
			if ("in_front_of_particles".equalsIgnoreCase(str3)) {
				c[i].setLayer(Layered.IN_FRONT_OF_PARTICLES);
			}
			else if ("behind_particles".equalsIgnoreCase(str3)) {
				c[i].setLayer(Layered.BEHIND_PARTICLES);
			}
			else if ("back".equalsIgnoreCase(str3)) {
				view.sendLayerToBack(c[i]);
			}
			else if ("front".equalsIgnoreCase(str3)) {
				view.bringLayerToFront(c[i]);
			}
		}
		else {
			out(ScriptEvent.FAILED, "Cannot set propery: " + str2);
			b = false;
		}
		if (b) {
			notifyChange();
			view.repaint();
		}
	}

	private void setRectangleField(String str1, String str2, String str3) {
		int lb = str1.indexOf("[");
		int rb = str1.indexOf("]");
		double z = parseMathExpression(str1.substring(lb + 1, rb));
		if (Double.isNaN(z))
			return;
		int i = (int) Math.round(z);
		RectangleComponent[] c = view.getRectangles();
		if (i < 0 || i >= c.length) {
			out(ScriptEvent.FAILED, "Rectangle " + i + " doesn't exisit.");
			return;
		}
		String s = str2.toLowerCase().intern();
		boolean b = true;
		if (s == "alpha") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].setAlpha((short) x);
		}
		else if (s == "x") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].setX((float) (x * IR_CONVERTER));
		}
		else if (s == "y") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].setY((float) (x * IR_CONVERTER));
		}
		else if (s == "width") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].setWidth((float) (x * IR_CONVERTER));
		}
		else if (s == "height") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].setHeight((float) (x * IR_CONVERTER));
		}
		else if (s == "size") {
			float[] x = parseArray(4, str3);
			if (x != null && x.length == 4)
				c[i].setRect(x[0] * IR_CONVERTER, x[1] * IR_CONVERTER, x[2] * IR_CONVERTER, x[3] * IR_CONVERTER);
		}
		else if (s == "efield") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x)) {
				VectorField vf = c[i].getVectorField();
				if (vf instanceof ElectricField) {
					vf.setIntensity(x);
				}
				else {
					out(ScriptEvent.FAILED, "Please turn on the efield for rectangle " + i + " and set its direction.");
					return;
				}
			}
		}
		else if (s == "bfield") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x)) {
				VectorField vf = c[i].getVectorField();
				if (vf instanceof MagneticField) {
					vf.setIntensity(x);
				}
				else {
					out(ScriptEvent.FAILED, "Please turn on the bfield for rectangle " + i + " and set its direction.");
					return;
				}
			}
		}
		else if (s == "viscosity") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].setViscosity((float) x);
		}
		else if (s == "angle") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].setAngle((float) x);
		}
		else if (s == "stroke") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x)) {
				if (x >= 0 && x < LineStyle.STROKES.length)
					c[i].setLineStyle((byte) x);
			}
		}
		else if (s == "color") {
			Color color = parseRGBColor(str3);
			if (color != null)
				c[i].setFillMode(new FillMode.ColorFill(color));
		}
		else if (s == "visible") {
			c[i].setVisible("on".equalsIgnoreCase(str3));
		}
		else if (s == "draggable") {
			c[i].setDraggable("on".equalsIgnoreCase(str3));
		}
		else if (s == "layer") {
			if ("in_front_of_particles".equalsIgnoreCase(str3)) {
				c[i].setLayer(Layered.IN_FRONT_OF_PARTICLES);
			}
			else if ("behind_particles".equalsIgnoreCase(str3)) {
				c[i].setLayer(Layered.BEHIND_PARTICLES);
			}
			else if ("back".equalsIgnoreCase(str3)) {
				view.sendLayerToBack(c[i]);
			}
			else if ("front".equalsIgnoreCase(str3)) {
				view.bringLayerToFront(c[i]);
			}
		}
		else {
			out(ScriptEvent.FAILED, "Cannot set propery: " + str2);
			b = false;
		}
		if (b) {
			notifyChange();
			view.repaint();
		}
	}

	private void setTriangleField(String str1, String str2, String str3) {
		int lb = str1.indexOf("[");
		int rb = str1.indexOf("]");
		double z = parseMathExpression(str1.substring(lb + 1, rb));
		if (Double.isNaN(z))
			return;
		int i = (int) Math.round(z);
		TriangleComponent[] c = view.getTriangles();
		if (i < 0 || i >= c.length) {
			out(ScriptEvent.FAILED, "Triangle " + i + " doesn't exisit.");
			return;
		}
		String s = str2.toLowerCase().intern();
		boolean b = true;
		if (s == "alpha") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].setAlpha((short) x);
		}
		else if (s == "x1") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].getVertex(0).x = (float) (x * IR_CONVERTER);
		}
		else if (s == "y1") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].getVertex(0).y = (float) (x * IR_CONVERTER);
		}
		else if (s == "x2") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].getVertex(1).x = (float) (x * IR_CONVERTER);
		}
		else if (s == "y2") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].getVertex(1).y = (float) (x * IR_CONVERTER);
		}
		else if (s == "x3") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].getVertex(2).x = (float) (x * IR_CONVERTER);
		}
		else if (s == "y3") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].getVertex(2).y = (float) (x * IR_CONVERTER);
		}
		else if (s == "efield") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x)) {
				VectorField vf = c[i].getVectorField();
				if (vf instanceof ElectricField) {
					vf.setIntensity(x);
				}
				else {
					out(ScriptEvent.FAILED, "Please turn on the efield for triangle " + i + " and set its direction.");
					return;
				}
			}
		}
		else if (s == "bfield") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x)) {
				VectorField vf = c[i].getVectorField();
				if (vf instanceof MagneticField) {
					vf.setIntensity(x);
				}
				else {
					out(ScriptEvent.FAILED, "Please turn on the bfield for triangle " + i + " and set its direction.");
					return;
				}
			}
		}
		else if (s == "viscosity") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].setViscosity((float) x);
		}
		else if (s == "stroke") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x)) {
				if (x >= 0 && x < LineStyle.STROKES.length)
					c[i].setLineStyle((byte) x);
			}
		}
		else if (s == "color") {
			Color color = parseRGBColor(str3);
			if (color != null)
				c[i].setFillMode(new FillMode.ColorFill(color));
		}
		else if (s == "visible") {
			c[i].setVisible("on".equalsIgnoreCase(str3));
		}
		else if (s == "draggable") {
			c[i].setDraggable("on".equalsIgnoreCase(str3));
		}
		else if (s == "layer") {
			if ("in_front_of_particles".equalsIgnoreCase(str3)) {
				c[i].setLayer(Layered.IN_FRONT_OF_PARTICLES);
			}
			else if ("behind_particles".equalsIgnoreCase(str3)) {
				c[i].setLayer(Layered.BEHIND_PARTICLES);
			}
			else if ("back".equalsIgnoreCase(str3)) {
				view.sendLayerToBack(c[i]);
			}
			else if ("front".equalsIgnoreCase(str3)) {
				view.bringLayerToFront(c[i]);
			}
		}
		else {
			out(ScriptEvent.FAILED, "Cannot set propery: " + str2);
			b = false;
		}
		if (b) {
			notifyChange();
			view.repaint();
		}
	}

	private void setEllipseField(String str1, String str2, String str3) {
		int lb = str1.indexOf("[");
		int rb = str1.indexOf("]");
		double z = parseMathExpression(str1.substring(lb + 1, rb));
		if (Double.isNaN(z))
			return;
		int i = (int) Math.round(z);
		EllipseComponent[] c = view.getEllipses();
		if (i < 0 || i >= c.length) {
			out(ScriptEvent.FAILED, "Ellipse " + i + " doesn't exisit.");
			return;
		}
		String s = str2.toLowerCase().intern();
		boolean b = true;
		if (s == "alpha") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].setAlphaAtCenter((short) x);
		}
		else if (s == "x") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].setX((float) (x * IR_CONVERTER));
		}
		else if (s == "y") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].setY((float) (x * IR_CONVERTER));
		}
		else if (s == "width") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].setWidth((float) (x * IR_CONVERTER));
		}
		else if (s == "height") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].setHeight((float) (x * IR_CONVERTER));
		}
		else if (s == "size") {
			float[] x = parseArray(4, str3);
			if (x != null && x.length == 4)
				c[i].setOval(x[0] * IR_CONVERTER, x[1] * IR_CONVERTER, x[2] * IR_CONVERTER, x[3] * IR_CONVERTER);
		}
		else if (s == "efield") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x)) {
				VectorField vf = c[i].getVectorField();
				if (vf instanceof ElectricField) {
					vf.setIntensity(x);
				}
				else {
					out(ScriptEvent.FAILED, "Please turn on the efield for ellipse " + i + " and set its direction.");
					return;
				}
			}
		}
		else if (s == "bfield") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x)) {
				VectorField vf = c[i].getVectorField();
				if (vf instanceof MagneticField) {
					vf.setIntensity(x);
				}
				else {
					out(ScriptEvent.FAILED, "Please turn on the bfield for ellipse " + i + " and set its direction.");
					return;
				}
			}
		}
		else if (s == "viscosity") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].setViscosity((float) x);
		}
		else if (s == "angle") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				c[i].setAngle((float) x);
		}
		else if (s == "stroke") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x)) {
				if (x >= 0 && x < LineStyle.STROKES.length)
					c[i].setLineStyle((byte) x);
			}
		}
		else if (s == "color") {
			Color color = parseRGBColor(str3);
			if (color != null)
				c[i].setFillMode(new FillMode.ColorFill(color));
		}
		else if (s == "visible") {
			c[i].setVisible("on".equalsIgnoreCase(str3));
		}
		else if (s == "draggable") {
			c[i].setDraggable("on".equalsIgnoreCase(str3));
		}
		else if (s == "layer") {
			if ("in_front_of_particles".equalsIgnoreCase(str3)) {
				c[i].setLayer(Layered.IN_FRONT_OF_PARTICLES);
			}
			else if ("behind_particles".equalsIgnoreCase(str3)) {
				c[i].setLayer(Layered.BEHIND_PARTICLES);
			}
			else if ("back".equalsIgnoreCase(str3)) {
				view.sendLayerToBack(c[i]);
			}
			else if ("front".equalsIgnoreCase(str3)) {
				view.bringLayerToFront(c[i]);
			}
		}
		else {
			out(ScriptEvent.FAILED, "Cannot set propery: " + str2);
			b = false;
		}
		if (b) {
			notifyChange();
			view.repaint();
		}
	}

	private void setTextBoxField(String str1, String str2, String str3) {
		if ((str3.startsWith("<t>") || str3.startsWith("<T>")) && (str3.endsWith("</t>") || str3.endsWith("<T>"))) {
			str3 = str3.substring(3, str3.length() - 4);
		}
		else {
			Matcher matcher = TEXT_EXTENSION.matcher(str3);
			if (matcher.find()) {
				String address = str3.substring(0, matcher.end()).trim();
				if (FileUtilities.isRelative(address)) {
					String base = (String) model.getProperty("url");
					if (base == null) {
						out(ScriptEvent.FAILED, "No directory has been specified. Save the page first.");
						return;
					}
					address = FileUtilities.getCodeBase(base) + address;
					if (System.getProperty("os.name").startsWith("Windows"))
						address = address.replace('\\', '/');
				}
				try {
					str3 = readText(address, view);
				}
				catch (InterruptedException e) {
					str3 = "Error in reading " + address;
				}
			}
		}
		int lb = str1.indexOf("[");
		int rb = str1.indexOf("]");
		double z = parseMathExpression(str1.substring(lb + 1, rb));
		if (Double.isNaN(z))
			return;
		int i = (int) Math.round(z);
		TextBoxComponent[] t = view.getTextBoxes();
		if (i >= t.length || i < 0) {
			out(ScriptEvent.FAILED, "Text box " + i + " doesn't exisit.");
			return;
		}
		String s = str2.toLowerCase().intern();
		boolean b = true;
		if (s == "text") {
			t[i].setText(format(str3));
		}
		else if (s == "callout") {
			t[i].setCallOut("true".equalsIgnoreCase(str3));
		}
		else if (s == "color") {
			Color c = parseRGBColor(str3);
			if (c != null)
				t[i].setForegroundColor(c);
		}
		else if (s == "transparent") {
			if ("true".equalsIgnoreCase(str3)) {
				t[i].setFillMode(FillMode.getNoFillMode());
			}
		}
		else if (s == "background") {
			Color c = parseRGBColor(str3);
			if (c != null)
				t[i].setFillMode(new FillMode.ColorFill(c));
		}
		else if (s == "visible") {
			t[i].setVisible("on".equalsIgnoreCase(str3));
		}
		else if (s == "draggable") {
			t[i].setDraggable("on".equalsIgnoreCase(str3));
		}
		else if (s == "font") {
			t[i].setFontFamily(str3);
		}
		else if (s == "size") {
			int n = t[i].getFont().getSize();
			try {
				t[i].setFontSize(parseInt(str3));
			}
			catch (Exception e) {
				t[i].setFontSize(n);
			}
		}
		else if (s == "style") {
			int n = t[i].getFont().getStyle();
			try {
				t[i].setFontStyle(parseInt(str3));
			}
			catch (Exception e) {
				t[i].setFontStyle(n);
			}
		}
		else if (s == "x") {
			double x = parseMathExpression(str3);
			if (!Double.isNaN(x))
				t[i].setRx(x * IR_CONVERTER);
		}
		else if (s == "y") {
			double y = parseMathExpression(str3);
			if (!Double.isNaN(y))
				t[i].setRy(y * IR_CONVERTER);
		}
		else if (s == "angle") {
			double a = parseMathExpression(str3);
			if (!Double.isNaN(a))
				t[i].setAngle((float) a);
		}
		else if (s == "border") {
			byte n = t[i].getBorderType();
			try {
				t[i].setBorderType((byte) parseInt(str3));
			}
			catch (Exception e) {
				t[i].setBorderType(n);
			}
		}
		else if (s == "shadow") {
			byte n = t[i].getShadowType();
			try {
				t[i].setShadowType((byte) parseInt(str3));
			}
			catch (Exception e) {
				t[i].setShadowType(n);
			}
		}
		else if (s == "layer") {
			if ("in_front_of_particles".equalsIgnoreCase(str3)) {
				t[i].setLayer(Layered.IN_FRONT_OF_PARTICLES);
			}
			else if ("behind_particles".equalsIgnoreCase(str3)) {
				t[i].setLayer(Layered.BEHIND_PARTICLES);
			}
			else if ("back".equalsIgnoreCase(str3)) {
				view.sendLayerToBack(t[i]);
			}
			else if ("front".equalsIgnoreCase(str3)) {
				view.bringLayerToFront(t[i]);
			}
		}
		else {
			out(ScriptEvent.FAILED, "Cannot set propery: " + str2);
			b = false;
		}
		if (b) {
			view.repaint();
			notifyChange();
		}
	}

	private BitSet genericSelect(String str) {
		boolean found = false;
		int nop = model.getNumberOfParticles();
		BitSet bs = new BitSet(nop);
		Matcher matcher = ALL.matcher(str);
		if (matcher.find()) {
			for (int k = 0; k < nop; k++)
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
			model.setParticleSelectionSet(bs);
		return found ? bs : null;
	}

	private BitSet selectParticles(String str) {

		if ("selected".equalsIgnoreCase(str)) {
			return model.getParticleSelectionSet();
		}

		BitSet bs = genericSelect(str);
		if (bs != null)
			return bs;

		boolean found = false;
		int nop = model.getNumberOfParticles();
		bs = new BitSet(nop);

		Matcher matcher = WITHIN_RECTANGLE.matcher(str);
		if (matcher.find()) {
			found = true;
			int lp = str.indexOf("(");
			if (lp == -1)
				lp = str.indexOf("[");
			int rp = str.indexOf(")");
			if (rp == -1)
				rp = str.indexOf("]");
			float x = 0, y = 0, w = 0, h = 0;
			try {
				str = str.substring(lp + 1, rp).trim();
				String[] s = str.split(REGEX_SEPARATOR + "+");
				x = Float.valueOf(s[0].trim()).floatValue() * IR_CONVERTER;
				y = Float.valueOf(s[1].trim()).floatValue() * IR_CONVERTER;
				w = Float.valueOf(s[2].trim()).floatValue() * IR_CONVERTER;
				h = Float.valueOf(s[3].trim()).floatValue() * IR_CONVERTER;
			}
			catch (Exception e) {
				out(ScriptEvent.FAILED, "Script error at: " + str + "\n" + e);
				return null;
			}
			Particle p;
			for (int k = 0; k < nop; k++) {
				p = model.getParticle(k);
				if (p.getRx() >= x && p.getRx() <= x + w && p.getRy() >= y && p.getRy() <= y + h)
					bs.set(k);
			}
		}

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
					float r = Float.valueOf(s.substring(0, s.indexOf(",")).trim()).floatValue() * IR_CONVERTER;
					r *= r;
					s = s.substring(s.indexOf(",") + 1);
					int i0 = Math.round(Float.valueOf(s.substring(0, s.indexOf("-")).trim()).floatValue());
					int i1 = Math.round(Float.valueOf(s.substring(s.indexOf("-") + 1).trim()).floatValue());
					if (i0 < nop && i0 >= 0 && i1 < nop && i1 >= 0 && i1 >= i0) {
						Particle c;
						for (int k = i0; k <= i1; k++) {
							bs.set(k);
							c = model.getParticle(k);
							for (int m = 0; m < i0; m++) {
								if (bs.get(m))
									continue;
								if (model.getParticle(m).distanceSquare(c) < r)
									bs.set(m);
							}
							for (int m = i1 + 1; m < nop; m++) {
								if (bs.get(m))
									continue;
								if (model.getParticle(m).distanceSquare(c) < r)
									bs.set(m);
							}
						}
					}
				}
				else {
					int lp = str.indexOf("(");
					int rp = str.indexOf(")");
					s = str.substring(lp + 1, rp).trim();
					float r = Float.valueOf(s.substring(0, s.indexOf(",")).trim()).floatValue() * IR_CONVERTER;
					r *= r;
					int center = Math.round(Float.valueOf(s.substring(s.indexOf(",") + 1).trim()).floatValue());
					if (center < nop && center >= 0) {
						Particle c = model.getParticle(center);
						for (int k = 0; k < nop; k++) {
							if (k == center) {
								bs.set(k);
							}
							else {
								if (model.getParticle(k).distanceSquare(c) < r)
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
				end = end < nop ? end : nop - 1;
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
				for (String k : s) {
					int x = -1;
					try {
						x = Math.round(Float.valueOf(k.trim()).floatValue());
					}
					catch (NumberFormatException e) {
						out(ScriptEvent.FAILED, k + " cannot be parsed as an integer.");
						return null;
					}
					if (x >= 0 && x < nop)
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
				if (x >= 0 && x < nop)
					bs.set(x);
			}
		}

		if (found) {
			model.setParticleSelectionSet(bs);
		}
		else {
			out(ScriptEvent.FAILED, "Unrecognized expression: " + str);
		}

		return found ? bs : null;

	}

	private BitSet selectElements(String str) {

		if ("selected".equalsIgnoreCase(str))
			return model.getParticleSelectionSet();

		BitSet bs = genericSelect(str);
		if (bs != null)
			return bs;

		boolean found = false;
		int nop = model.getNumberOfParticles();
		bs = new BitSet(nop);

		Matcher matcher = RANGE.matcher(str);
		if (matcher.find()) {
			found = true;
			String[] s = str.split("-");
			int start = Integer.valueOf(s[0].trim()).intValue();
			int end = Integer.valueOf(s[1].trim()).intValue();
			MolecularModel mm = (MolecularModel) model;
			for (int k = 0; k < nop; k++) {
				int id = mm.getAtom(k).getID();
				if (id >= start && id <= end)
					bs.set(k);
			}
		}

		if (!found) {
			matcher = INTEGER_GROUP.matcher(str);
			if (matcher.find()) {
				found = true;
				MolecularModel mm = (MolecularModel) model;
				String[] s = str.split(REGEX_SEPARATOR + "+");
				int index;
				for (int m = 0; m < s.length; m++) {
					index = Integer.valueOf(s[m]).intValue();
					for (int k = 0; k < nop; k++) {
						if (mm.getAtom(k).getID() == index)
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
					MolecularModel mm = (MolecularModel) model;
					for (int k = 0; k < nop; k++) {
						if (mm.getAtom(k).getID() == index)
							bs.set(k);
					}
				}
			}
		}

		if (!found) {
			matcher = RANGE_WITHIN_RECTANGLE.matcher(str);
			if (matcher.find()) {
				found = true;
				matcher = RANGE_LEADING.matcher(str);
				if (matcher.find()) {
					int iw = str.toLowerCase().indexOf("within");
					String[] s = str.substring(0, iw).split("-");
					int start = Integer.valueOf(s[0].trim()).intValue();
					int end = Integer.valueOf(s[1].trim()).intValue();
					str = str.substring(matcher.end()).trim();
					int lp = str.indexOf("(");
					if (lp == -1)
						lp = str.indexOf("[");
					int rp = str.indexOf(")");
					if (rp == -1)
						rp = str.indexOf("]");
					float x = 0, y = 0, w = 0, h = 0;
					try {
						str = str.substring(lp + 1, rp).trim();
						s = str.split(REGEX_SEPARATOR + "+");
						x = Float.valueOf(s[0].trim()).floatValue() * IR_CONVERTER;
						y = Float.valueOf(s[1].trim()).floatValue() * IR_CONVERTER;
						w = Float.valueOf(s[2].trim()).floatValue() * IR_CONVERTER;
						h = Float.valueOf(s[3].trim()).floatValue() * IR_CONVERTER;
					}
					catch (Exception e) {
						out(ScriptEvent.FAILED, "Script error at: " + str + "\n" + e);
						return null;
					}
					Atom p;
					for (int k = 0; k < nop; k++) {
						p = (Atom) model.getParticle(k);
						if (p.getID() <= end && p.getID() >= start && p.getRx() >= x && p.getRx() <= x + w
								&& p.getRy() >= y && p.getRy() <= y + h)
							bs.set(k);
					}
				}
			}
		}

		if (!found) {
			matcher = INDEX_WITHIN_RECTANGLE.matcher(str);
			if (matcher.find()) {
				found = true;
				matcher = NNI.matcher(str);
				if (matcher.find()) {
					int id = 0;
					try {
						id = Integer.valueOf(str.substring(0, matcher.end()).trim()).intValue();
					}
					catch (NumberFormatException e) {
						out(ScriptEvent.FAILED, "Element type must be an integer: " + str);
						return null;
					}
					str = str.substring(matcher.end()).trim();
					int lp = str.indexOf("(");
					if (lp == -1)
						lp = str.indexOf("[");
					int rp = str.indexOf(")");
					if (rp == -1)
						rp = str.indexOf("]");
					float x = 0, y = 0, w = 0, h = 0;
					try {
						str = str.substring(lp + 1, rp).trim();
						String[] s = str.split(REGEX_SEPARATOR + "+");
						x = Float.valueOf(s[0].trim()).floatValue() * IR_CONVERTER;
						y = Float.valueOf(s[1].trim()).floatValue() * IR_CONVERTER;
						w = Float.valueOf(s[2].trim()).floatValue() * IR_CONVERTER;
						h = Float.valueOf(s[3].trim()).floatValue() * IR_CONVERTER;
					}
					catch (Exception e) {
						out(ScriptEvent.FAILED, "Script error at: " + str + "\n" + e);
						return null;
					}
					Atom p;
					for (int k = 0; k < nop; k++) {
						p = (Atom) model.getParticle(k);
						if (p.getID() == id && p.getRx() >= x && p.getRx() <= x + w && p.getRy() >= y
								&& p.getRy() <= y + h)
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
					float r = Float.valueOf(s.substring(0, s.indexOf(",")).trim()).floatValue() * IR_CONVERTER;
					r *= r;
					s = s.substring(s.indexOf(",") + 1);
					int i0 = Integer.valueOf(s.substring(0, s.indexOf("-")).trim()).intValue();
					int i1 = Integer.valueOf(s.substring(s.indexOf("-") + 1).trim()).intValue();
					if (i1 >= i0) {
						MolecularModel mm = (MolecularModel) model;
						Atom at;
						for (int k = 0; k < nop; k++) {
							at = mm.getAtom(k);
							if (at.getID() >= i0 && at.getID() <= i1) {
								bs.set(k);
								for (int m = 0; m < nop; m++) {
									if (m == k || bs.get(m))
										continue;
									if (mm.getAtom(m).distanceSquare(at) < r)
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
					float r = Float.valueOf(s.substring(0, s.indexOf(",")).trim()).floatValue() * IR_CONVERTER;
					r *= r;
					int center = 0;
					try {
						center = Integer.valueOf(s.substring(s.indexOf(",") + 1).trim()).intValue();
					}
					catch (NumberFormatException nfe) {
						out(ScriptEvent.FAILED, str + " is not an integer number.");
						return null;
					}
					Atom at;
					MolecularModel mm = (MolecularModel) model;
					for (int m = 0; m < nop; m++) {
						at = mm.getAtom(m);
						if (at.getID() == center) {
							bs.set(m);
							for (int k = 0; k < nop; k++) {
								if (k == m || bs.get(k))
									continue;
								if (mm.getAtom(k).distanceSquare(at) < r)
									bs.set(k);
							}
						}
					}
				}
			}
		}

		if (found) {
			model.setParticleSelectionSet(bs);
		}
		else {
			out(ScriptEvent.FAILED, "Unrecognized expression: " + str);
		}

		return found ? bs : null;

	}

	private BitSet selectRadialBonds(String str) {
		MolecularModel mm = (MolecularModel) model;
		int n = mm.bonds.size();
		if (n == 0)
			return null;
		BitSet bs = new BitSet(n);
		if ("selected".equalsIgnoreCase(str)) {
			synchronized (mm.bonds.getSynchronizationLock()) {
				for (int i = 0; i < n; i++) {
					if (mm.bonds.get(i).isSelected())
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
					mm.bonds.setSelectionSet(bs);
					return bs;
				}
			}
		}
		if (selectFromCollection(str, n, bs)) {
			mm.bonds.setSelectionSet(bs);
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
			MolecularModel mm = (MolecularModel) model;
			RadialBond rb = null;
			synchronized (mm.bonds.getSynchronizationLock()) {
				for (Iterator it = mm.bonds.iterator(); it.hasNext();) {
					rb = (RadialBond) it.next();
					if (inRangeInclusive(rb.atom1.getIndex(), beg, end)
							|| inRangeInclusive(rb.atom1.getIndex(), beg, end)) {
						bs.set(rb.getIndex());
					}
				}
			}
			return true;
		}
		if (INTEGER_GROUP.matcher(str).find()) {
			String[] s = str.split(REGEX_SEPARATOR + "+");
			int index;
			MolecularModel mm = (MolecularModel) model;
			RadialBond rb = null;
			for (int m = 0; m < s.length; m++) {
				index = Float.valueOf(s[m]).intValue();
				synchronized (mm.bonds.getSynchronizationLock()) {
					for (Iterator it = mm.bonds.iterator(); it.hasNext();) {
						rb = (RadialBond) it.next();
						if (rb.contains(mm.getAtom(index)))
							bs.set(rb.getIndex());
					}
				}
			}
			return true;
		}
		if (INDEX.matcher(str).find()) {
			int index = Float.valueOf(str.trim()).intValue();
			MolecularModel mm = (MolecularModel) model;
			RadialBond rb = null;
			synchronized (mm.bonds.getSynchronizationLock()) {
				for (Iterator it = mm.bonds.iterator(); it.hasNext();) {
					rb = (RadialBond) it.next();
					if (rb.contains(mm.getAtom(index)))
						bs.set(rb.getIndex());
				}
			}
			return true;
		}
		return false;
	}

	private BitSet selectAngularBonds(String str) {
		MolecularModel mm = (MolecularModel) model;
		int n = mm.bends.size();
		if (n <= 0)
			return null;
		BitSet bs = new BitSet(n);
		if ("selected".equalsIgnoreCase(str)) {
			synchronized (mm.bends.getSynchronizationLock()) {
				for (int i = 0; i < n; i++) {
					if (mm.bends.get(i).isSelected())
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
					mm.bends.setSelectionSet(bs);
					return bs;
				}
			}
		}
		if (selectFromCollection(str, n, bs)) {
			mm.bends.setSelectionSet(bs);
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
			MolecularModel mm = (MolecularModel) model;
			AngularBond ab = null;
			synchronized (mm.bends.getSynchronizationLock()) {
				for (Iterator it = mm.bends.iterator(); it.hasNext();) {
					ab = (AngularBond) it.next();
					if (inRangeInclusive(ab.atom1.getIndex(), beg, end)
							|| inRangeInclusive(ab.atom2.getIndex(), beg, end)
							|| inRangeInclusive(ab.atom3.getIndex(), beg, end)) {
						bs.set(ab.getIndex());
					}
				}
			}
			return true;
		}
		if (INTEGER_GROUP.matcher(str).find()) {
			String[] s = str.split(REGEX_SEPARATOR + "+");
			int index;
			MolecularModel mm = (MolecularModel) model;
			AngularBond ab = null;
			for (int m = 0; m < s.length; m++) {
				index = Float.valueOf(s[m]).intValue();
				synchronized (mm.bends.getSynchronizationLock()) {
					for (Iterator it = mm.bends.iterator(); it.hasNext();) {
						ab = (AngularBond) it.next();
						if (ab.contains(mm.getAtom(index)))
							bs.set(ab.getIndex());
					}
				}
			}
			return true;
		}
		if (INDEX.matcher(str).find()) {
			int index = Float.valueOf(str.trim()).intValue();
			MolecularModel mm = (MolecularModel) model;
			AngularBond ab = null;
			synchronized (mm.bends.getSynchronizationLock()) {
				for (Iterator it = mm.bends.iterator(); it.hasNext();) {
					ab = (AngularBond) it.next();
					if (ab.contains(mm.getAtom(index)))
						bs.set(ab.getIndex());
				}
			}
			return true;
		}
		return false;
	}

	private BitSet selectMolecules(String str) {
		MolecularModel mm = (MolecularModel) model;
		if (mm.molecules == null)
			return null;
		int n = mm.molecules.size();
		if (n <= 0)
			return null;
		BitSet bs = new BitSet(n);
		if ("selected".equalsIgnoreCase(str)) {
			synchronized (mm.molecules.getSynchronizationLock()) {
				for (int i = 0; i < n; i++) {
					if (mm.molecules.get(i).isSelected())
						bs.set(i);
				}
			}
			return bs;
		}
		if (selectFromCollection(str, n, bs)) {
			mm.molecules.setSelectionSet(bs);
			return bs;
		}
		out(ScriptEvent.FAILED, "Unrecognized expression: " + str);
		return null;
	}

	private BitSet selectObstacles(String str) {
		MolecularModel mm = (MolecularModel) model;
		if (mm.obstacles == null)
			return null;
		int n = mm.obstacles.size();
		if (n <= 0)
			return null;
		BitSet bs = new BitSet(n);
		if ("selected".equalsIgnoreCase(str)) {
			synchronized (mm.obstacles.getSynchronizationLock()) {
				for (int i = 0; i < n; i++) {
					if (mm.obstacles.get(i).isSelected())
						bs.set(i);
				}
			}
			return bs;
		}
		if (selectFromCollection(str, n, bs)) {
			mm.obstacles.setSelectionSet(bs);
			return bs;
		}
		out(ScriptEvent.FAILED, "Unrecognized expression: " + str);
		return null;
	}

	private BitSet selectImages(String str) {
		int n = view.getNumberOfInstances(ImageComponent.class);
		if (n == 0)
			return null;
		BitSet bs = new BitSet(n);
		if ("selected".equalsIgnoreCase(str)) {
			for (int i = 0; i < n; i++) {
				if (view.getImage(i).isSelected())
					bs.set(i);
			}
			return bs;
		}
		if (selectFromCollection(str, n, bs)) {
			view.setImageSelectionSet(bs);
			return bs;
		}
		Matcher matcher = WITHIN_RECTANGLE.matcher(str);
		if (matcher.find()) {
			Rectangle2D area = getWithinArea(str);
			for (int i = 0; i < n; i++) {
				if (area.contains(view.getImage(i).getCenter())) {
					bs.set(i);
				}
			}
			view.setImageSelectionSet(bs);
			return bs;
		}
		out(ScriptEvent.FAILED, "Unrecognized expression: " + str);
		return null;
	}

	private BitSet selectTextBoxes(String str) {
		int n = view.getNumberOfInstances(TextBoxComponent.class);
		if (n == 0)
			return null;
		BitSet bs = new BitSet(n);
		if ("selected".equalsIgnoreCase(str)) {
			for (int i = 0; i < n; i++) {
				if (view.getTextBox(i).isSelected())
					bs.set(i);
			}
			return bs;
		}
		if (selectFromCollection(str, n, bs)) {
			view.setTextBoxSelectionSet(bs);
			return bs;
		}
		Matcher matcher = WITHIN_RECTANGLE.matcher(str);
		if (matcher.find()) {
			Rectangle2D area = getWithinArea(str);
			for (int i = 0; i < n; i++) {
				if (area.contains(view.getTextBox(i).getCenter())) {
					bs.set(i);
				}
			}
			view.setTextBoxSelectionSet(bs);
			return bs;
		}
		out(ScriptEvent.FAILED, "Unrecognized expression: " + str);
		return null;
	}

	private BitSet selectLines(String str) {
		int n = view.getNumberOfInstances(LineComponent.class);
		if (n == 0)
			return null;
		BitSet bs = new BitSet(n);
		if ("selected".equalsIgnoreCase(str)) {
			for (int i = 0; i < n; i++) {
				if (view.getLine(i).isSelected())
					bs.set(i);
			}
			return bs;
		}
		if (selectFromCollection(str, n, bs)) {
			view.setLineSelectionSet(bs);
			return bs;
		}
		Matcher matcher = WITHIN_RECTANGLE.matcher(str);
		if (matcher.find()) {
			Rectangle2D area = getWithinArea(str);
			for (int i = 0; i < n; i++) {
				if (area.contains(view.getLine(i).getCenter())) {
					bs.set(i);
				}
			}
			view.setLineSelectionSet(bs);
			return bs;
		}
		out(ScriptEvent.FAILED, "Unrecognized expression: " + str);
		return null;
	}

	private BitSet selectRectangles(String str) {
		int n = view.getNumberOfInstances(RectangleComponent.class);
		if (n == 0)
			return null;
		BitSet bs = new BitSet(n);
		if ("selected".equalsIgnoreCase(str)) {
			for (int i = 0; i < n; i++) {
				if (view.getRectangle(i).isSelected())
					bs.set(i);
			}
			return bs;
		}
		if (selectFromCollection(str, n, bs)) {
			view.setRectangleSelectionSet(bs);
			return bs;
		}
		Matcher matcher = WITHIN_RECTANGLE.matcher(str);
		if (matcher.find()) {
			Rectangle2D area = getWithinArea(str);
			for (int i = 0; i < n; i++) {
				if (area.contains(view.getRectangle(i).getCenter())) {
					bs.set(i);
				}
			}
			view.setRectangleSelectionSet(bs);
			return bs;
		}
		out(ScriptEvent.FAILED, "Unrecognized expression: " + str);
		return null;
	}

	private BitSet selectTriangles(String str) {
		int n = view.getNumberOfInstances(TriangleComponent.class);
		if (n == 0)
			return null;
		BitSet bs = new BitSet(n);
		if ("selected".equalsIgnoreCase(str)) {
			for (int i = 0; i < n; i++) {
				if (view.getTriangle(i).isSelected())
					bs.set(i);
			}
			return bs;
		}
		if (selectFromCollection(str, n, bs)) {
			view.setTriangleSelectionSet(bs);
			return bs;
		}
		Matcher matcher = WITHIN_RECTANGLE.matcher(str);
		if (matcher.find()) {
			Rectangle2D area = getWithinArea(str);
			for (int i = 0; i < n; i++) {
				if (area.contains(view.getTriangle(i).getCenter())) {
					bs.set(i);
				}
			}
			view.setTriangleSelectionSet(bs);
			return bs;
		}
		out(ScriptEvent.FAILED, "Unrecognized expression: " + str);
		return null;
	}

	private BitSet selectEllipses(String str) {
		int n = view.getNumberOfInstances(EllipseComponent.class);
		if (n == 0)
			return null;
		BitSet bs = new BitSet(n);
		if ("selected".equalsIgnoreCase(str)) {
			for (int i = 0; i < n; i++) {
				if (view.getEllipse(i).isSelected())
					bs.set(i);
			}
			return bs;
		}
		if (selectFromCollection(str, n, bs)) {
			view.setEllipseSelectionSet(bs);
			return bs;
		}
		Matcher matcher = WITHIN_RECTANGLE.matcher(str);
		if (matcher.find()) {
			Rectangle2D area = getWithinArea(str);
			for (int i = 0; i < n; i++) {
				if (area.contains(view.getEllipse(i).getCenter())) {
					bs.set(i);
				}
			}
			view.setEllipseSelectionSet(bs);
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
					bs = selectParticles(s);
					break;
				case BY_ELEMENT:
					bs = selectElements(s);
					break;
				case BY_OBSTACLE:
					bs = selectObstacles(s);
					break;
				case BY_IMAGE:
					bs = selectImages(s);
					break;
				case BY_TEXTBOX:
					bs = selectTextBoxes(s);
					break;
				case BY_LINE:
					bs = selectLines(s);
					break;
				case BY_RECTANGLE:
					bs = selectRectangles(s);
					break;
				case BY_TRIANGLE:
					bs = selectTriangles(s);
					break;
				case BY_ELLIPSE:
					bs = selectEllipses(s);
					break;
				case BY_RBOND:
					if (model instanceof MolecularModel)
						bs = selectRadialBonds(s);
					break;
				case BY_ABOND:
					if (model instanceof MolecularModel)
						bs = selectAngularBonds(s);
					break;
				case BY_MOLECULE:
					if (model instanceof MolecularModel)
						bs = selectMolecules(s);
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
			model.setParticleSelectionSet(bs);
			break;
		case BY_OBSTACLE:
			model.obstacles.setSelectionSet(bs);
			break;
		case BY_IMAGE:
			view.setImageSelectionSet(bs);
			break;
		case BY_TEXTBOX:
			view.setTextBoxSelectionSet(bs);
			break;
		case BY_LINE:
			view.setLineSelectionSet(bs);
			break;
		case BY_RECTANGLE:
			view.setRectangleSelectionSet(bs);
			break;
		case BY_RBOND:
			if (model instanceof MolecularModel)
				((MolecularModel) model).bonds.setSelectionSet(bs);
			break;
		case BY_ABOND:
			if (model instanceof MolecularModel)
				((MolecularModel) model).bends.setSelectionSet(bs);
			break;
		case BY_MOLECULE:
			if (model instanceof MolecularModel)
				((MolecularModel) model).molecules.setSelectionSet(bs);
			break;
		}

		return bs;

	}

	private int getNumberOfObjects(byte type) {
		int n = 0;
		switch (type) {
		case BY_ATOM:
		case BY_ELEMENT:
			n = model.getNumberOfParticles();
			break;
		case BY_OBSTACLE:
			n = model.obstacles.size();
			break;
		case BY_IMAGE:
			n = view.getNumberOfInstances(ImageComponent.class);
			break;
		case BY_TEXTBOX:
			n = view.getNumberOfInstances(TextBoxComponent.class);
			break;
		case BY_LINE:
			n = view.getNumberOfInstances(LineComponent.class);
			break;
		case BY_RECTANGLE:
			n = view.getNumberOfInstances(RectangleComponent.class);
			break;
		case BY_ELLIPSE:
			n = view.getNumberOfInstances(EllipseComponent.class);
			break;
		case BY_RBOND:
			if (model instanceof MolecularModel)
				n = ((MolecularModel) model).bonds.size();
			break;
		case BY_ABOND:
			if (model instanceof MolecularModel)
				n = ((MolecularModel) model).bends.size();
			break;
		case BY_MOLECULE:
			if (model instanceof MolecularModel)
				n = ((MolecularModel) model).molecules.size();
			break;
		}
		return n;
	}

	private void removeSelectedObjects() {
		int n = model.getNumberOfParticles();
		List<Integer> list = new ArrayList<Integer>();
		for (int k = 0; k < n; k++) {
			if (model.getParticle(k).isSelected())
				list.add(k);
		}
		if (model instanceof MolecularModel) {
			if (!list.isEmpty())
				((AtomisticView) view).removeMarkedAtoms(list);
			MolecularModel mm = (MolecularModel) model;
			boolean b = false;
			synchronized (mm.bonds.getSynchronizationLock()) {
				for (Iterator it = mm.bonds.iterator(); it.hasNext();) {
					RadialBond rb = (RadialBond) it.next();
					if (rb.isSelected()) {
						it.remove();
						rb.removeAttachedLayeredComponents();
						b = true;
					}
				}
			}
			synchronized (mm.bends.getSynchronizationLock()) {
				for (Iterator it = mm.bends.iterator(); it.hasNext();) {
					if (((AngularBond) it.next()).isSelected())
						it.remove();
				}
			}
			mm.removeGhostAngularBonds();
			if (b) {
				mm.view.bondChanged(null);
				MoleculeCollection.sort(mm);
			}
			// mm.view.removeSelectedComponent();
		}
		else if (model instanceof MesoModel) {
			((MesoView) view).removeMarkedParticles(list);
		}
		if (!model.obstacles.isEmpty()) {
			RectangularObstacle obs = null;
			synchronized (model.obstacles.getSynchronizationLock()) {
				for (Iterator it = model.obstacles.iterator(); it.hasNext();) {
					obs = (RectangularObstacle) it.next();
					if (obs.isSelected())
						it.remove();
				}
			}
		}
		view.removeAllSelectedLayeredComponents();
		view.repaint();
	}

	protected String readText(String address, Component parent) throws InterruptedException {
		if (FileUtilities.isRelative(address)) {
			address = FileUtilities.getCodeBase((String) model.getProperty("url")) + address;
		}
		return super.readText(address, parent);
	}

	private static void fillActionIDMap() {
		if (actionIDMap == null) {
			actionIDMap = new HashMap<String, Short>();
			try {
				for (Field f : UserAction.class.getFields()) {
					actionIDMap.put(f.getName(), f.getShort(null));
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private float[] parseCoordinates(String str) {
		float[] x = parseArray(2, str);
		if (x != null)
			for (int i = 0; i < x.length; i++)
				x[i] *= IR_CONVERTER;
		return x;
	}

}