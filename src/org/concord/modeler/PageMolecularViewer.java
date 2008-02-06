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

package org.concord.modeler;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.vecmath.Point3i;

import org.concord.jmol.LoadMoleculeEvent;
import org.concord.jmol.JmolContainer;
import org.concord.modeler.draw.FillMode;
import org.concord.modeler.event.AbstractChange;
import org.concord.modeler.event.ModelEvent;
import org.concord.modeler.event.ModelListener;
import org.concord.modeler.event.PageComponentEvent;
import org.concord.modeler.script.Compiler;
import org.concord.modeler.text.Page;
import org.concord.modeler.text.SaveReminder;
import org.concord.modeler.text.XMLCharacterEncoder;
import org.concord.modeler.ui.ColorMenu;
import org.concord.modeler.ui.IconPool;
import org.concord.modeler.util.Evaluator;
import org.concord.modeler.util.FileUtilities;
import org.concord.modeler.util.ScreenshotSaver;
import org.concord.modeler.util.SwingWorker;
import org.jmol.api.Attachment;
import org.jmol.api.InteractionCenter;
import org.jmol.api.SiteAnnotation;

import static javax.swing.Action.*;

public class PageMolecularViewer extends JmolContainer implements BasicModel, Embeddable, Scriptable, Engine,
		AutoResizable {

	private final static Pattern REMOVE = Pattern.compile("(^(?i)remove\\b){1}");

	Page page;
	boolean widthIsRelative, heightIsRelative;
	float widthRatio = 1, heightRatio = 1;
	private int index;
	private boolean marked;
	private boolean changable;
	private int selectedAtom = -1, selectedAnnotationHostAtom = -1;
	private int selectedBond = -1, selectedAnnotationHostBond = -1;

	private Map<String, ChangeListener> changeMap;
	private Map<String, Action> choiceMap;
	private Map<String, Action> actionMap;
	private Map<String, Action> switchMap;
	private Map<String, Action> multiSwitchMap;
	private List<ModelListener> modelListenerList;
	private Action snapshotAction, snapshotAction2, scriptAction, styleChoices, importStructure;
	private AbstractChange scriptChanger, cpkRadiusChanger;

	private JPopupMenu popupMenu;
	private JMenuItem miCustom, miRemove, miMenuBar, miToolBar, miStatusBar;
	private static PageMolecularViewerMaker maker;

	private MouseAdapter mouseAdapter = new MouseAdapter() {
		public void mouseReleased(MouseEvent e) {
			if (!ModelerUtilities.isRightClick(e)) {
				if (e.getClickCount() < 2) {
					handlePopupText(e.getX(), e.getY());
				}
			}
		}

		public void mousePressed(MouseEvent e) {
			if (ModelerUtilities.isRightClick(e)) {
				invokePopupMenu(e.getX(), e.getY());
			}
		}
	};

	public PageMolecularViewer() {

		super();

		createActions();

		addMouseListenerToViewer(mouseAdapter);
		setFileChooser(ModelerUtilities.fileChooser);

		setScreenshotAction(new ScreenshotSaver(ModelerUtilities.fileChooser, getView(), true));
		setSnapshotListener(snapshotAction);

		setAtomColorSelectionMenu(createColorSelectionMenu("atoms"), false);
		setBondColorSelectionMenu(createColorSelectionMenu("bonds"), false);
		setHBondColorSelectionMenu(createColorSelectionMenu("hbonds"), false);
		setSSBondColorSelectionMenu(createColorSelectionMenu("ssbonds"), false);
		setBackgroundMenu(createColorSelectionMenu("background"), false);

		actionMap = Collections.synchronizedMap(new TreeMap<String, Action>());
		actionMap.put((String) snapshotAction.getValue(SHORT_DESCRIPTION), snapshotAction);
		actionMap.put((String) snapshotAction2.getValue(SHORT_DESCRIPTION), snapshotAction2);
		actionMap.put((String) scriptAction.getValue(SHORT_DESCRIPTION), scriptAction);
		Action a = getView().getActionMap().get("reset");
		actionMap.put((String) a.getValue(SHORT_DESCRIPTION), a);

		switchMap = Collections.synchronizedMap(new TreeMap<String, Action>());
		a = getView().getActionMap().get("spin");
		switchMap.put((String) a.getValue(SHORT_DESCRIPTION), a);
		a = getView().getActionMap().get("show selection");
		switchMap.put((String) a.getValue(SHORT_DESCRIPTION), a);
		a = getView().getActionMap().get("axes");
		switchMap.put((String) a.getValue(SHORT_DESCRIPTION), a);
		a = getView().getActionMap().get("bound box");
		switchMap.put((String) a.getValue(SHORT_DESCRIPTION), a);
		a = getView().getActionMap().get("show hbonds");
		switchMap.put((String) a.getValue(SHORT_DESCRIPTION), a);
		a = getView().getActionMap().get("show ssbonds");
		switchMap.put((String) a.getValue(SHORT_DESCRIPTION), a);
		switchMap.put((String) scriptAction.getValue(SHORT_DESCRIPTION), scriptAction);

		changeMap = Collections.synchronizedMap(new TreeMap<String, ChangeListener>());
		changeMap.put((String) scriptChanger.getProperty(AbstractChange.SHORT_DESCRIPTION), scriptChanger);
		changeMap.put((String) cpkRadiusChanger.getProperty(AbstractChange.SHORT_DESCRIPTION), cpkRadiusChanger);

		choiceMap = Collections.synchronizedMap(new TreeMap<String, Action>());
		choiceMap.put((String) scriptAction.getValue(SHORT_DESCRIPTION), scriptAction);
		choiceMap.put((String) importStructure.getValue(SHORT_DESCRIPTION), importStructure);
		choiceMap.put((String) styleChoices.getValue(SHORT_DESCRIPTION), styleChoices);

		multiSwitchMap = Collections.synchronizedMap(new TreeMap<String, Action>());
		multiSwitchMap.put((String) scriptAction.getValue(SHORT_DESCRIPTION), scriptAction);

	}

	public PageMolecularViewer getCopy() {
		PageMolecularViewer mv = (PageMolecularViewer) InstancePool.sharedInstance().getUnusedInstance(getClass());
		mv.setPage(page);
		mv.setResourceAddress(getResourceAddress());
		mv.setScheme(getScheme());
		mv.setNavigationMode(getNavigationMode());
		mv.setRotationAndZoom(getCurrentOrientation());
		mv.setBoundBoxShown(isBoundBoxShown());
		mv.setAxesShown(areAxesShown());
		mv.setFillMode(getFillMode());
		mv.setSelectionHaloEnabled(isSelectionHaloEnabled());
		mv.setDotsEnabled(isDotsEnabled());
		mv.setCustomInitializationScript(getCustomInitializationScript());
		mv.setBorderType(getBorderType());
		mv.setWidthRelative(widthIsRelative);
		mv.setHeightRelative(heightIsRelative);
		int w = getPreferredSize().width;
		int h = getPreferredSize().height;
		if (widthIsRelative) {
			mv.setWidthRatio(widthRatio);
			w = (int) (page.getWidth() * widthRatio);
		}
		if (heightIsRelative) {
			mv.setHeightRatio(heightRatio);
			h = (int) (page.getHeight() * heightRatio);
		}
		mv.setPreferredSize(new Dimension(w, h));
		mv.setChangable(page.isEditable());
		return mv;
	}

	private void createActions() {

		// for buttons

		snapshotAction = new AbstractAction() {

			public void actionPerformed(ActionEvent e) {
				if (ModelerUtilities.stopFiring(e))
					return;
				SnapshotGallery.sharedInstance().takeSnapshot(page.getAddress(), getView());
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		snapshotAction.putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		snapshotAction.putValue(SMALL_ICON, IconPool.getIcon("camera"));
		snapshotAction.putValue(NAME, "Take a Snapshot");
		snapshotAction.putValue(SHORT_DESCRIPTION, "Take a snapshot");

		snapshotAction2 = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (ModelerUtilities.stopFiring(e))
					return;
				SnapshotGallery.sharedInstance().takeSnapshot(page.getAddress(), getView(), false);
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		snapshotAction2.putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		snapshotAction2.putValue(SMALL_ICON, IconPool.getIcon("camera"));
		snapshotAction2.putValue(NAME, "Take a Snapshot Without Description");
		snapshotAction2.putValue(SHORT_DESCRIPTION, "Take a snapshot without description");

		scriptAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (ModelerUtilities.stopFiring(e))
					return;
				Object o = e.getSource();
				if (o instanceof JCheckBox) {
					JCheckBox cb = (JCheckBox) o;
					if (cb.isSelected()) {
						Object o2 = cb.getClientProperty("selection script");
						if (o2 instanceof String) {
							String s = (String) o2;
							if (!s.trim().equals(""))
								runScript(s);
						}
					}
					else {
						Object o2 = cb.getClientProperty("deselection script");
						if (o2 instanceof String) {
							String s = (String) o2;
							if (!s.trim().equals(""))
								runScript(s);
						}
					}
				}
				else if (o instanceof AbstractButton) {
					Object o2 = ((AbstractButton) o).getClientProperty("script");
					if (o2 instanceof String) {
						String s = (String) o2;
						if (!s.trim().equals(""))
							runScript(s);
					}
				}
				else if (o instanceof JComboBox) {
					JComboBox cb = (JComboBox) o;
					Object s = cb.getClientProperty("script" + cb.getSelectedIndex());
					if (s == null)
						return;
					runScript((String) s);
				}
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		scriptAction.putValue(NAME, "Execute Jmol Script");
		scriptAction.putValue(SHORT_DESCRIPTION, "Execute Jmol script");

		// for combo boxes

		styleChoices = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (!isEnabled())
					return;
				Object o = e.getSource();
				if (o instanceof JComboBox) {
					if (!((JComboBox) o).isShowing())
						return;
					setScheme((String) (((JComboBox) o).getSelectedItem()));
					notifyChange();
				}
				repaint();
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		styleChoices.putValue(NAME, "Display Style");
		styleChoices.putValue(SHORT_DESCRIPTION, "Display style");
		styleChoices.putValue("options", new String[] { SPACE_FILLING, BALL_AND_STICK, STICKS, WIREFRAME, CARTOON,
				RIBBON, ROCKET, TRACE });
		styleChoices.putValue("state", getScheme());

		importStructure = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (!isEnabled())
					return;
				Object o = e.getSource();
				if (o instanceof JComboBox) {
					if (!((JComboBox) o).isShowing())
						return;
					final String s = (String) (((JComboBox) o).getSelectedItem());
					if (!"Select a model".equals(s)) {
						setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						new SwingWorker() {
							public Object construct() {
								setResourceAddress(FileUtilities.getCodeBase(page.getAddress()) + s);
								loadCurrentResource();
								// setCustomInitializationScript("reset;");
								return null;
							}

							public void finished() {
								setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
							}
						}.start();
					}
				}
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		importStructure.putValue(NAME, "Import Model");
		importStructure.putValue(SHORT_DESCRIPTION, "Import a model");

		// for sliders and spinners

		scriptChanger = new AbstractChange() {
			public void stateChanged(ChangeEvent e) {
				Object o = e.getSource();
				if (o instanceof JSlider) {
					JSlider source = (JSlider) o;
					Double scale = (Double) source.getClientProperty(SCALE);
					double s = scale == null ? 1.0 : 1.0 / scale.doubleValue();
					if (!source.getValueIsAdjusting()) {
						String script = (String) source.getClientProperty("Script");
						if (script != null) {
							String result = source.getValue() * s + "";
							if (result.endsWith(".0"))
								result = result.substring(0, result.length() - 2);
							script = script.replaceAll("(?i)%val", result);
							result = source.getMaximum() * s + "";
							if (result.endsWith(".0"))
								result = result.substring(0, result.length() - 2);
							script = script.replaceAll("(?i)%max", result);
							result = source.getMinimum() * s + "";
							if (result.endsWith(".0"))
								result = result.substring(0, result.length() - 2);
							script = script.replaceAll("(?i)%min", result);
							int lq = script.indexOf('"');
							int rq = script.indexOf('"', lq + 1);
							while (lq != -1 && rq != -1 && lq != rq) {
								String expression = script.substring(lq + 1, rq);
								Evaluator mathEval = new Evaluator(expression.trim());
								result = "" + mathEval.eval();
								if (result.endsWith(".0"))
									result = result.substring(0, result.length() - 2);
								script = script.substring(0, lq) + result + script.substring(rq + 1);
								lq = script.indexOf('"', rq + 1);
								rq = script.indexOf('"', lq + 1);
							}
							runScript(script);
						}
					}
				}
				else if (o instanceof JSpinner) {
					JSpinner source = (JSpinner) o;
					String script = (String) source.getClientProperty("Script");
					if (script != null) {
						String result = source.getValue() + "";
						if (result.endsWith(".0"))
							result = result.substring(0, result.length() - 2);
						script = script.replaceAll("(?i)%val", result);
						int lq = script.indexOf('"');
						int rq = script.indexOf('"', lq + 1);
						while (lq != -1 && rq != -1 && lq != rq) {
							String expression = script.substring(lq + 1, rq);
							Evaluator mathEval = new Evaluator(expression.trim());
							result = "" + mathEval.eval();
							if (result.endsWith(".0"))
								result = result.substring(0, result.length() - 2);
							script = script.substring(0, lq) + result + script.substring(rq + 1);
							lq = script.indexOf('"', rq + 1);
							rq = script.indexOf('"', lq + 1);
						}
						runScript(script);
					}
				}
			}

			public double getMinimum() {
				return 0.0;
			}

			public double getMaximum() {
				return 100.0;
			}

			public double getStepSize() {
				return 1.0;
			}

			public double getValue() {
				return 0.0;
			}

			public String toString() {
				return (String) getProperty(SHORT_DESCRIPTION);
			}
		};
		scriptChanger.putProperty(AbstractChange.SHORT_DESCRIPTION, "Execute Jmol script");

		cpkRadiusChanger = new AbstractChange() {
			public void stateChanged(ChangeEvent e) {
				Object o = e.getSource();
				if (o instanceof JSlider) {
					JSlider source = (JSlider) o;
					Double scale = (Double) source.getClientProperty(SCALE);
					double s = scale == null ? 1.0 : scale.doubleValue();
					if (!source.getValueIsAdjusting()) {
						setCPKRadius((byte) (source.getValue() / s));
						notifyChange();
					}
				}
				else if (o instanceof JSpinner) {
					setCPKRadius(((Number) ((JSpinner) o).getValue()).byteValue());
					notifyChange();
				}
			}

			public double getMinimum() {
				return 0.0;
			}

			public double getMaximum() {
				return 100.0;
			}

			public double getStepSize() {
				return 1.0;
			}

			public double getValue() {
				return getCPKRadius();
			}

			public String toString() {
				return (String) getProperty(SHORT_DESCRIPTION);
			}
		};
		cpkRadiusChanger.putProperty(AbstractChange.SHORT_DESCRIPTION, "CPK radius (precent of VDW)");

	}

	private static float parseFloat(String s) {
		try {
			return Float.parseFloat(s);
		}
		catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return Float.NaN;
	}

	private static Color parseColorString(String s) {
		if (s.toLowerCase().startsWith("0x")) {
			try {
				return new Color(Integer.valueOf(s.substring(2), 16));
			}
			catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		if (s.startsWith("#")) {
			try {
				return new Color(Integer.valueOf(s.substring(1), 16));
			}
			catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Scripts that can be run by this method go beyond the command set of the Jmol scripts. Some MW-specific commands
	 * were added. Some Jmol scripts are modified here to meet the MW standards before sending to Jmol's script
	 * interpreter. Commands that are not Jmol scripts are processed right away in this method. All Jmol commands are
	 * sent to the Jmol interpreter and processed later on. The drawback is that you cannot count on the first set of
	 * commands to run at a sequence as they appear in the script code.
	 */
	public String runScript(String s) {
		if (s == null)
			return "no script";
		s = s.trim();
		if (s.equals(""))
			return "no script";
		String[] command = Compiler.COMMAND_BREAK.split(s);
		int n = command.length;
		if (n < 1)
			return "no script";
		Matcher matcher = null;
		String standardJmolScript = "";
		String ci = null;
		for (int i = 0; i < n; i++) {
			ci = command[i].trim();
			if (ci.equalsIgnoreCase("remove")) {
				removeSelectedObjects();
				continue;
			}
			if (ci.equalsIgnoreCase("snapshot")) {
				SnapshotGallery.sharedInstance().takeSnapshot(page.getAddress(), getView());
				continue;
			}
			if (ci.equalsIgnoreCase("focus")) {
				getView().requestFocusInWindow();
				continue;
			}
			matcher = Compiler.SET.matcher(ci);
			if (matcher.find()) {
				String str = ci.substring(matcher.end()).trim().toLowerCase();
				if (str.startsWith("interaction.")) {
					String[] t = str.split("\\s+");
					if (t.length >= 4) {
						float value = 0;
						int index = -1;
						try {
							value = Float.parseFloat(t[1]);
							index = Integer.parseInt(t[3]);
						}
						catch (Exception e) {
							e.printStackTrace();
							continue;
						}
						if ("foratom".equals(t[2])) {
							InteractionCenter ic = getInteraction(index, Attachment.ATOM_HOST);
							if (ic == null) {
								ic = new InteractionCenter();
								ic.setHostType(Attachment.ATOM_HOST);
								setInteraction(index, ic);
							}
							if (t[0].endsWith("charge")) {
								ic.setCharge((int) value);
							}
							else if (t[0].endsWith("radius")) {
								ic.setRadius(value);
							}
						}
						else if ("forbond".equals(t[2])) {
							InteractionCenter ic = getInteraction(index, Attachment.BOND_HOST);
							if (ic == null) {
								ic = new InteractionCenter();
								ic.setHostType(Attachment.BOND_HOST);
								setInteraction(index, ic);
							}
							if (t[0].endsWith("charge")) {
								ic.setCharge((int) value);
							}
							else if (t[0].endsWith("radius")) {
								ic.setRadius(value);
							}
						}
					}
					continue;
				}
				if (str.startsWith("navigationmode")) {
					changeNavigationMode(!str.endsWith("false"));
					continue;
				}
				if (str.startsWith("rovermode")) {
					boolean b = !str.endsWith("false");
					setRoverMode(b);
					if (b && !getRoverGo())
						setRoverGo(true);
					continue;
				}
				if (str.startsWith("collisiondetection")) {
					setCollisionDetectionForAllAtoms(!str.endsWith("false"));
					continue;
				}
				if (str.startsWith("blinkinteractions")) {
					setBlinkInteractionCenters(!str.endsWith("false"));
					continue;
				}
				if (str.startsWith("rover.mass")) {
					int m = str.indexOf(" ");
					if (m != -1) {
						float x = parseFloat(str.substring(m).trim());
						if (!Float.isNaN(x))
							setRoverMass(x);
					}
					continue;
				}
				if (str.startsWith("rover.momentofinertia")) {
					int m = str.indexOf(" ");
					if (m != -1) {
						float x = parseFloat(str.substring(m).trim());
						if (!Float.isNaN(x))
							setRoverMomentOfInertia(x);
					}
					continue;
				}
				if (str.startsWith("rover.charge")) {
					int m = str.indexOf(" ");
					if (m != -1) {
						float x = parseFloat(str.substring(m).trim());
						if (!Float.isNaN(x))
							setRoverCharge(x);
					}
					continue;
				}
				if (str.startsWith("rover.friction")) {
					int m = str.indexOf(" ");
					if (m != -1) {
						float x = parseFloat(str.substring(m).trim());
						if (!Float.isNaN(x))
							setRoverFriction(x);
					}
					continue;
				}
				if (str.startsWith("rover.color")) {
					int m = str.indexOf(" ");
					if (m != -1) {
						Color x = parseColorString(str.substring(m).trim());
						if (x != null) {
							setRoverColor(x.getRGB());
						}
					}
					continue;
				}
				if (str.startsWith("rover.visible")) {
					int m = str.indexOf(" ");
					if (m != -1) {
						setRoverVisible("on".equalsIgnoreCase(str.substring(m).trim()));
					}
					continue;
				}
			}
			matcher = REMOVE.matcher(ci);
			if (matcher.find()) {
				String str = ci.substring(matcher.end()).trim().toLowerCase();
				if (str.startsWith("interaction")) {
					String t = str.substring(11).trim();
					if (t.startsWith("foratom")) {
						t = t.substring(7).trim();
						int index = -1;
						try {
							index = Integer.parseInt(t);
						}
						catch (NumberFormatException e) {
							e.printStackTrace();
						}
						if (index >= 0) {
							removeInteraction(index, InteractionCenter.ATOM_HOST);
						}
					}
					else if (t.startsWith("forbond")) {
						t = t.substring(7).trim();
						int index = -1;
						try {
							index = Integer.parseInt(t);
						}
						catch (NumberFormatException e) {
							e.printStackTrace();
						}
						if (index >= 0) {
							removeInteraction(index, InteractionCenter.BOND_HOST);
						}
					}
					continue;
				}
			}
			matcher = Compiler.ADD.matcher(ci);
			if (matcher.find()) {
				String str = ci.substring(matcher.end()).trim();
				matcher = Compiler.IMAGE.matcher(str);
				if (matcher.find()) {
					str = str.substring(matcher.end()).trim();
					matcher = Compiler.IMAGE_EXTENSION.matcher(str);
					if (matcher.find()) {
						String address = str.substring(0, matcher.end()).trim();
						if (FileUtilities.isRelative(address)) {
							String base = page.getAddress();
							if (base == null)
								continue;
							address = FileUtilities.getCodeBase(base) + address;
							if (System.getProperty("os.name").startsWith("Windows"))
								address = address.replace('\\', '/');
						}
						str = str.substring(matcher.end()).trim();
						if (str.matches(Compiler.REGEX_POSITIVE_NUMBER_PAIR)) {
							int lp = str.indexOf("(");
							int rp = str.indexOf(")");
							str = str.substring(lp + 1, rp).trim();
							String[] ss = str.split(Compiler.REGEX_SEPARATOR + "+");
							float x = Float.valueOf(ss[0].trim()).floatValue();
							float y = Float.valueOf(ss[1].trim()).floatValue();
							if (FileUtilities.isRemote(address)) {
								URL url = null;
								try {
									url = new URL(address);
								}
								catch (MalformedURLException mue) {
									mue.printStackTrace();
								}
								addImage(url, x, y);
							}
							else {
								addImage(address, x, y);
							}
						}
					}
					continue;
				}
			}
			matcher = Compiler.SELECT.matcher(ci);
			if (matcher.find()) {
				String str = ci.substring(matcher.end()).trim();
				matcher = Compiler.IMAGE.matcher(str);
				if (matcher.find()) {
					str = str.substring(matcher.end()).trim();
					selectImages(str);
					continue;
				}
			}
			matcher = Compiler.LOAD.matcher(ci); // load command is standard jmol script
			if (matcher.find()) {
				String oldAddress = ci.substring(matcher.end()).trim();
				if (FileUtilities.isRelative(oldAddress)) {
					String newAddress = FileUtilities.getCodeBase(page.getAddress()) + oldAddress;
					if (System.getProperty("os.name").startsWith("Windows"))
						newAddress = newAddress.replace('\\', '/');
					ci = ci.replaceAll(oldAddress, newAddress);
				}
			}
			matcher = Compiler.SOURCE.matcher(ci); // source/script command is standard jmol script
			if (matcher.find()) {
				String oldAddress = ci.substring(matcher.end()).trim();
				if (FileUtilities.isRelative(oldAddress)) {
					String newAddress = FileUtilities.getCodeBase(page.getAddress()) + oldAddress;
					if (System.getProperty("os.name").startsWith("Windows"))
						newAddress = newAddress.replace('\\', '/');
					ci = ci.replaceAll(oldAddress, newAddress);
				}
			}
			if (standardJmolScript.equals("") || standardJmolScript.endsWith(";")) {
				standardJmolScript += ci;
			}
			else {
				standardJmolScript += ";" + ci;
			}
		}
		if (!standardJmolScript.endsWith(";"))
			standardJmolScript += ";";
		return super.runScript(standardJmolScript);
	}

	private void invokePopupMenu(int x, int y) {
		selectedAnnotationHostAtom = getAnnotationHost(Attachment.ATOM_HOST, x, y);
		selectedAnnotationHostBond = getAnnotationHost(Attachment.BOND_HOST, x, y);
		if (selectedAnnotationHostAtom < 0 && selectedAnnotationHostBond < 0) {
			selectedAtom = findNearestAtomIndex(x, y);
			selectedBond = findNearestBondIndex(x, y);
			// when both selected
			if (selectedAtom >= 0 && selectedBond >= 0) {
				int[] bondPair = getBondAtoms(selectedBond);
				if (bondPair[0] == selectedAtom || bondPair[1] == selectedAtom) {
					// if the selected atom forms the selected bond, favor the selected atom
					selectedBond = -1;
				}
				else {
					// otherwise, compare the z-depth of the selected bond and the selected atom
					Point3i pAtom = getAtomScreen(selectedAtom);
					Point3i pBond = getBondCenterScreen(selectedBond);
					if (pAtom.z > pBond.z) {
						selectedAtom = -1;
					}
					else {
						selectedBond = -1;
					}
				}
			}
		}
		else {
			selectedAtom = -1;
			selectedBond = -1;
		}
		setClickedAtom(selectedAtom);
		setClickedBond(selectedBond);
		if (popupMenu == null)
			createPopupMenu();
		hidePopupText();
		popupMenu.show(popupMenu.getInvoker(), x + 5, y + 5);
	}

	private void handlePopupText(int x, int y) {
		if ((selectedAnnotationHostAtom = getAnnotationHost(Attachment.ATOM_HOST, x, y)) != -1) {
			Point p = getAnnotationScreenCenter(Attachment.ATOM_HOST, selectedAnnotationHostAtom);
			SiteAnnotation a = getSiteAnnotation(selectedAnnotationHostAtom, Attachment.ATOM_HOST);
			showPopupText(a, x, y, p.x, p.y);
		}
		else if ((selectedAnnotationHostBond = getAnnotationHost(Attachment.BOND_HOST, x, y)) != -1) {
			Point p = getAnnotationScreenCenter(Attachment.BOND_HOST, selectedAnnotationHostBond);
			SiteAnnotation a = getSiteAnnotation(selectedAnnotationHostBond, Attachment.BOND_HOST);
			showPopupText(a, x, y, p.x, p.y);
		}
		else {
			hidePopupText();
		}
	}

	private void showPopupText(SiteAnnotation a, int xPressed, int yPressed, int xCallOut, int yCallOut) {
		int wWindow = getView().getWidth();
		int hWindow = getView().getHeight();
		int x = xPressed;
		int y = yPressed;
		int w = a.getWidth() > 0 ? a.getWidth() : wWindow >> 1;
		int h = a.getHeight() > 0 ? a.getHeight() : hWindow >> 1;
		if (xPressed > wWindow >> 1) {
			x -= 10 + w;
		}
		else {
			x += 10;
		}
		if (yPressed > hWindow >> 1) {
			y -= 10 + h;
		}
		else {
			y += 10;
		}
		if (getCallOutWindow() == null) {
			PopupWindow window = new PopupWindow(a.getText(), x, y, w, h, xCallOut, yCallOut);
			window.addCloseButton(new Runnable() {
				public void run() {
					hidePopupText();
				}
			});
			if (page.isEditable()) {
				window.addEditButton(new Runnable() {
					public void run() {
						editSelectedAnnotation();
					}
				});
			}
			else {
				window.addEditButton(null);
			}
			window.setPage(page);
			setCallOutWindow(window);
		}
		else {
			PopupWindow window = (PopupWindow) getCallOutWindow();
			if (page.isEditable()) {
				window.addEditButton(new Runnable() {
					public void run() {
						editSelectedAnnotation();
					}
				});
			}
			else {
				window.addEditButton(null);
			}
			window.setContentType("text/html");
			try {
				window.setBase(page.getURL());
			}
			catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		showPopupText(a.getText(), a.getBackgroundRgb(), x, y, w, h, xCallOut, yCallOut);
	}

	private void editSelectedAnnotation() {
		hidePopupText();
		if (selectedAnnotationHostAtom >= 0)
			editAnnotation(selectedAnnotationHostAtom, Attachment.ATOM_HOST);
		else if (selectedAnnotationHostBond >= 0)
			editAnnotation(selectedAnnotationHostBond, Attachment.BOND_HOST);
		notifyChange();
	}

	private void editSelectedInteraction() {
		if (selectedAtom >= 0)
			editInteraction(selectedAtom, Attachment.ATOM_HOST);
		else if (selectedBond >= 0)
			editInteraction(selectedBond, Attachment.BOND_HOST);
		notifyChange();
	}

	private void removeSelectedObjects() {
		removeSelectedImages();
	}

	private void selectImages(String str) {
		int n = getNumberOfImages();
		if (n == 0)
			return;
		BitSet bs = new BitSet(n);
		boolean notPrefix = false;
		Matcher matcher = Pattern.compile("(^(?i)not\\b){1}").matcher(str);
		if (matcher.find()) {
			notPrefix = true;
			str = str.substring(matcher.end()).trim();
		}
		boolean found = false;
		matcher = Compiler.ALL.matcher(str);
		if (matcher.find()) {
			for (int k = 0; k < n; k++)
				bs.set(k);
			found = true;
		}
		if (!found) {
			matcher = Compiler.NONE.matcher(str);
			if (matcher.find()) {
				found = true;
			}
		}
		if (!found) {
			matcher = Compiler.RANGE.matcher(str);
			if (matcher.find()) {
				found = true;
				String[] s = str.split("-");
				int start = Integer.valueOf(s[0].trim());
				int end = Integer.valueOf(s[1].trim());
				if (start < n) {
					end = Math.min(end, n - 1);
					for (int k = start; k <= end; k++)
						bs.set(k);
				}
			}
		}
		if (!found) {
			matcher = Compiler.INTEGER_GROUP.matcher(str);
			if (matcher.find()) {
				found = true;
				String[] s = str.split(Compiler.REGEX_SEPARATOR + "+");
				int index;
				for (int m = 0; m < s.length; m++) {
					index = Integer.valueOf(s[m]);
					if (index < n)
						bs.set(index);
				}
			}
		}
		if (!found) {
			matcher = Compiler.INDEX.matcher(str);
			if (matcher.find()) {
				found = true;
				int index = Integer.valueOf(str.trim());
				if (index < n)
					bs.set(index);
			}
		}
		if (found) {
			if (notPrefix)
				bs.flip(0, n);
			setImageSelectionSet(bs);
		}
		else {
			System.out.println("Unrecognized expression: " + str);
		}
	}

	public void setWidthRelative(boolean b) {
		widthIsRelative = b;
	}

	public boolean isWidthRelative() {
		return widthIsRelative;
	}

	public void setWidthRatio(float wr) {
		widthRatio = wr;
	}

	public float getWidthRatio() {
		return widthRatio;
	}

	public void setHeightRelative(boolean b) {
		heightIsRelative = b;
	}

	public boolean isHeightRelative() {
		return heightIsRelative;
	}

	public void setHeightRatio(float hr) {
		heightRatio = hr;
	}

	public float getHeightRatio() {
		return heightRatio;
	}

	public Map<String, Action> getActions() {
		return actionMap;
	}

	public Map<String, Action> getSwitches() {
		return switchMap;
	}

	public Map<String, Action> getMultiSwitches() {
		return multiSwitchMap;
	}

	public Map<String, ChangeListener> getChanges() {
		return changeMap;
	}

	public Map<String, Action> getChoices() {
		return choiceMap;
	}

	public void run() {
		runAnimation();
	}

	public void stop() {
		stopImmediately();
	}

	public void reset() {
		widthIsRelative = heightIsRelative = false;
		widthRatio = heightRatio = 1;
		selectedAtom = -1;
		selectedAnnotationHostAtom = -1;
		selectedBond = -1;
		selectedAnnotationHostBond = -1;
		clear();
	}

	public String getPageAddress() {
		return page.getAddress();
	}

	/** This method uses the CacheManager. */
	public synchronized void loadCurrentResource() {
		String s = getResourceAddress();
		if (s == null) {
			setLoadingMessagePainted(false);
			return;
		}
		setLoadingMessagePainted(true);
		if (FileUtilities.isRemote(s)) {
			s = FileUtilities.httpEncode(s);
			if (ConnectionManager.sharedInstance().isCachingAllowed()) {
				URL u = null;
				try {
					u = new URL(s);
				}
				catch (MalformedURLException e) {
					e.printStackTrace();
				}
				if (u != null) {
					File file = null;
					try {
						file = ConnectionManager.sharedInstance().shouldUpdate(u);
						if (file == null)
							file = ConnectionManager.sharedInstance().cache(u);
						setResourceAddress(file.getAbsolutePath());
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		super.loadCurrentResource();
	}

	public void addModelListener(ModelListener ml) {
		if (modelListenerList == null)
			modelListenerList = new ArrayList<ModelListener>();
		modelListenerList.add(ml);
	}

	public void removeModelListener(ModelListener ml) {
		if (modelListenerList == null)
			return;
		modelListenerList.remove(ml);
	}

	public List<ModelListener> getModelListeners() {
		return modelListenerList;
	}

	public void notifyModelListeners(ModelEvent e) {
		if (modelListenerList == null)
			return;
		for (ModelListener l : modelListenerList) {
			l.modelUpdate(e);
		}
	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	public void createPopupMenu() {

		popupMenu = new JPopupMenu();
		popupMenu.setInvoker(this);

		String s = Modeler.getInternationalText("CustomizeMolecularViewer");
		miCustom = new JMenuItem((s != null ? s : "Customize This Jmol Viewer") + "...");
		miCustom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null) {
					maker = new PageMolecularViewerMaker(PageMolecularViewer.this);
				}
				else {
					maker.setObject(PageMolecularViewer.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(miCustom);

		s = Modeler.getInternationalText("RemoveMolecularViewer");
		miRemove = new JMenuItem(s != null ? s : "Remove This Jmol Viewer");
		miRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(PageMolecularViewer.this);
			}
		});
		popupMenu.add(miRemove);

		s = Modeler.getInternationalText("CopyMolecularViewer");
		JMenuItem miCopy = new JMenuItem(s != null ? s : "Copy This Jmol Viewer");
		miCopy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(PageMolecularViewer.this);
			}
		});
		popupMenu.add(miCopy);
		popupMenu.addSeparator();

		popupMenu.add(createSelectMenu(true));
		updateComputedMenus(true);

		popupMenu.add(createSchemeMenu());

		popupMenu.add(createColorMenu(true));
		setAtomColorSelectionMenu(createColorSelectionMenu("atoms"), true);
		setBondColorSelectionMenu(createColorSelectionMenu("bonds"), true);
		setHBondColorSelectionMenu(createColorSelectionMenu("hbonds"), true);
		setSSBondColorSelectionMenu(createColorSelectionMenu("ssbonds"), true);
		setBackgroundMenu(createColorSelectionMenu("background"), true);

		popupMenu.addSeparator();

		s = getInternationalText("NavigationMode");
		final JMenuItem miNavigation = new JCheckBoxMenuItem(s != null ? s : "Navigation Mode");
		miNavigation.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				changeNavigationMode(((JCheckBoxMenuItem) e.getSource()).isSelected());
				notifyChange();
			}
		});
		popupMenu.add(miNavigation);

		s = getInternationalText("NavigationOptions");
		JMenu menu = new JMenu(s != null ? s : "Navigation Options");
		popupMenu.add(menu);

		s = getInternationalText("AutonomousMode");
		final JMenuItem miRover = new JCheckBoxMenuItem(s != null ? s : "Autonomous Mode");
		miRover.setEnabled(getNavigationMode());
		miRover.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setRoverMode(miRover.isSelected());
			}
		});
		menu.add(miRover);

		s = getInternationalText("CollisionDetectionForAllAtoms");
		final JMenuItem miPauli = new JCheckBoxMenuItem(s != null ? s : "Collision Detection for All Atoms");
		miPauli.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				setCollisionDetectionForAllAtoms(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		menu.add(miPauli);

		s = getInternationalText("BlinkInteractionCenters");
		final JMenuItem miBlink = new JCheckBoxMenuItem(s != null ? s : "Blink Interaction Centers");
		miBlink.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				setBlinkInteractionCenters(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		menu.add(miBlink);

		s = getInternationalText("RoverSettings");
		final JMenuItem miRoverSettings = new JMenuItem(s != null ? s : "Rover Settings");
		miRoverSettings.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				editRover();
			}
		});
		menu.add(miRoverSettings);

		menu.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				miRover.setEnabled(getNavigationMode());
				miRoverSettings.setEnabled(isRoverMode());
				ModelerUtilities.setWithoutNotifyingListeners(miRover, isRoverMode());
				ModelerUtilities.setWithoutNotifyingListeners(miPauli, getPauliRepulsionForAllAtoms());
				ModelerUtilities.setWithoutNotifyingListeners(miBlink, getBlinkInteractionCenters());
			}
		});

		s = getInternationalText("JumpToClickedAtom");
		final JMenuItem miCamera = new JCheckBoxMenuItem(s != null ? s : "Jump to Clicked Atom");
		miCamera.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setCameraAtom(((JCheckBoxMenuItem) e.getSource()).isSelected() ? selectedAtom : -1);
				notifyChange();
			}
		});
		popupMenu.add(miCamera);
		popupMenu.addSeparator();

		s = getInternationalText("Annotation");
		menu = new JMenu(s != null ? s : "Annotation");
		popupMenu.add(menu);

		s = getInternationalText("AttachAnnotationToClickedAtom");
		final JMenuItem miAtomAnnotationKey = new JMenuItem((s != null ? s : "Attach Annotation to Clicked Atom")
				+ "...");
		miAtomAnnotationKey.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectedAnnotationHostAtom = selectedAtom;
				editSelectedAnnotation();
			}
		});
		menu.add(miAtomAnnotationKey);

		s = getInternationalText("AttachAnnotationToClickedBond");
		final JMenuItem miBondAnnotationKey = new JMenuItem((s != null ? s : "Attach Annotation to Clicked Bond")
				+ "...");
		miBondAnnotationKey.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectedAnnotationHostBond = selectedBond;
				editSelectedAnnotation();
			}
		});
		menu.add(miBondAnnotationKey);

		s = getInternationalText("EditSelectedAnnotation");
		final JMenuItem miEditAnnotation = new JMenuItem((s != null ? s : "Edit Selected Annotation") + "...");
		miEditAnnotation.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				editSelectedAnnotation();
			}
		});
		menu.add(miEditAnnotation);

		s = getInternationalText("RemoveSelectedAnnotation");
		final JMenuItem miRemoveAnnotation = new JMenuItem(s != null ? s : "Remove Selected Annotation");
		miRemoveAnnotation.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (selectedAnnotationHostAtom < 0 && selectedAnnotationHostBond < 0)
					return;
				String s2 = getInternationalText("RemoveSelectedAnnotation");
				if (JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(PageMolecularViewer.this),
						"Are you sure you want to remove this annotation?", s2 != null ? s2 : "Remove Annotation",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
					if (selectedAnnotationHostAtom >= 0)
						removeSiteAnnotation(selectedAnnotationHostAtom, Attachment.ATOM_HOST);
					if (selectedAnnotationHostBond >= 0)
						removeSiteAnnotation(selectedAnnotationHostBond, Attachment.BOND_HOST);
					notifyChange();
				}
			}
		});
		menu.add(miRemoveAnnotation);

		menu.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				boolean a = hasAnnotation(Attachment.ATOM_HOST, selectedAtom);
				boolean b = hasAnnotation(Attachment.BOND_HOST, selectedBond);
				miAtomAnnotationKey.setEnabled(selectedAtom != -1 && !a);
				miBondAnnotationKey.setEnabled(selectedBond != -1 && !b);
				boolean c = selectedAnnotationHostAtom != -1 || selectedAnnotationHostBond != -1;
				miEditAnnotation.setEnabled(c);
				miRemoveAnnotation.setEnabled(c);
				ModelerUtilities.setWithoutNotifyingListeners(miAtomAnnotationKey, a);
				ModelerUtilities.setWithoutNotifyingListeners(miBondAnnotationKey, b);
			}
		});

		s = getInternationalText("Interaction");
		menu = new JMenu(s != null ? s : "Interaction");
		popupMenu.add(menu);
		popupMenu.addSeparator();

		s = getInternationalText("AttachInteractionToClickedAtom");
		final JMenuItem miAtomInteractionKey = new JMenuItem((s != null ? s : "Attach Interaction to Clicked Atom")
				+ "...");
		miAtomInteractionKey.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				editSelectedInteraction();
			}
		});
		menu.add(miAtomInteractionKey);

		s = getInternationalText("AttachInteractionToClickedBond");
		final JMenuItem miBondInteractionKey = new JMenuItem((s != null ? s : "Attach Interaction to Clicked Bond")
				+ "...");
		miBondInteractionKey.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				editSelectedInteraction();
			}
		});
		menu.add(miBondInteractionKey);

		s = getInternationalText("EditSelectedInteraction");
		final JMenuItem miEditInteraction = new JMenuItem((s != null ? s : "Edit Selected Interaction") + "...");
		miEditInteraction.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				editSelectedInteraction();
			}
		});
		menu.add(miEditInteraction);

		s = getInternationalText("RemoveSelectedInteraction");
		final JMenuItem miRemoveInteraction = new JMenuItem(s != null ? s : "Remove Selected Interaction");
		miRemoveInteraction.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (selectedAtom < 0 && selectedBond < 0)
					return;
				String s2 = getInternationalText("RemoveSelectedInteraction");
				if (JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(PageMolecularViewer.this),
						"Are you sure you want to remove this interaction?", s2 != null ? s2 : "Remove Interaction",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
					if (selectedAtom >= 0 && hasInteraction(Attachment.ATOM_HOST, selectedAtom))
						removeInteraction(selectedAtom, Attachment.ATOM_HOST);
					if (selectedBond >= 0 && hasInteraction(Attachment.BOND_HOST, selectedBond))
						removeInteraction(selectedBond, Attachment.BOND_HOST);
					notifyChange();
				}
			}
		});
		menu.add(miRemoveInteraction);

		menu.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				boolean a = hasInteraction(Attachment.ATOM_HOST, selectedAtom);
				boolean b = hasInteraction(Attachment.BOND_HOST, selectedBond);
				miAtomInteractionKey.setEnabled(selectedAtom != -1 && !a);
				miBondInteractionKey.setEnabled(selectedBond != -1 && !b);
				boolean c = (selectedAtom != -1 && hasInteraction(Attachment.ATOM_HOST, selectedAtom))
						|| (selectedBond != -1 && hasInteraction(Attachment.BOND_HOST, selectedBond));
				miEditInteraction.setEnabled(c);
				miRemoveInteraction.setEnabled(c);
				ModelerUtilities.setWithoutNotifyingListeners(miAtomInteractionKey, a);
				ModelerUtilities.setWithoutNotifyingListeners(miBondInteractionKey, b);
			}
		});

		s = getInternationalText("Spin");
		final JMenuItem miSpin = new JCheckBoxMenuItem(s != null ? s : "Spin View");
		miSpin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setSpinOn(((JCheckBoxMenuItem) e.getSource()).isSelected());
				notifyChange();
			}
		});
		popupMenu.add(miSpin);

		JMenuItem mi = new JMenuItem(snapshotAction);
		mi.setIcon(null);
		s = Modeler.getInternationalText("TakeSnapshot");
		mi.setText((s != null ? s : mi.getText()) + "...");
		popupMenu.add(mi);
		popupMenu.addSeparator();

		s = Modeler.getInternationalText("ShowMenuBar");
		miMenuBar = new JCheckBoxMenuItem(s != null ? s : "Show Menu Bar");
		miMenuBar.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				enableMenuBar(e.getStateChange() == ItemEvent.SELECTED);
				notifyChange();
			}
		});
		popupMenu.add(miMenuBar);

		s = Modeler.getInternationalText("ShowToolBar");
		miToolBar = new JCheckBoxMenuItem(s != null ? s : "Show Tool Bar");
		miToolBar.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				enableToolBar(e.getStateChange() == ItemEvent.SELECTED);
				notifyChange();
			}
		});
		popupMenu.add(miToolBar);

		s = Modeler.getInternationalText("ShowStatusBar");
		miStatusBar = new JCheckBoxMenuItem(s != null ? s : "Show Bottom Bar");
		miStatusBar.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				enableBottomBar(e.getStateChange() == ItemEvent.SELECTED);
				notifyChange();
			}
		});
		popupMenu.add(miStatusBar);

		popupMenu.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				miCustom.setEnabled(changable);
				miRemove.setEnabled(changable);
				miCamera.setEnabled(getNavigationMode() && selectedAtom != -1);
				ModelerUtilities.setWithoutNotifyingListeners(miCamera, selectedAtom != -1
						&& selectedAtom == getCameraAtom());
				ModelerUtilities.setWithoutNotifyingListeners(miNavigation, getNavigationMode());
				ModelerUtilities.setWithoutNotifyingListeners(miSpin, getSpinOn());
				ModelerUtilities.setWithoutNotifyingListeners(miMenuBar, isMenuBarEnabled());
				ModelerUtilities.setWithoutNotifyingListeners(miToolBar, isToolBarEnabled());
				ModelerUtilities.setWithoutNotifyingListeners(miStatusBar, isBottomBarEnabled());
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}
		});

		popupMenu.pack();

	}

	public void destroy() {
		InstancePool.sharedInstance().setStatus(this, false);
		page = null;
		if (maker != null)
			maker.setObject(null);
	}

	static void destroyPopupMenu(JPopupMenu pm) {
		if (pm == null)
			return;
		pm.setInvoker(null);
		Component c;
		AbstractButton b;
		for (int i = 0, n = pm.getComponentCount(); i < n; i++) {
			c = pm.getComponent(i);
			if (c instanceof AbstractButton) {
				b = (AbstractButton) c;
				b.setAction(null);
				ActionListener[] al = b.getActionListeners();
				if (al != null) {
					for (ActionListener x : al)
						b.removeActionListener(x);
				}
				ItemListener[] il = b.getItemListeners();
				if (il != null) {
					for (ItemListener x : il)
						b.removeItemListener(x);
				}
			}
		}
		PopupMenuListener[] pml = pm.getPopupMenuListeners();
		if (pml != null) {
			for (PopupMenuListener x : pml)
				pm.removePopupMenuListener(x);
		}
		pm.removeAll();
	}

	public void setIndex(int i) {
		index = i;
	}

	public int getIndex() {
		return index;
	}

	public void setMarked(boolean b) {
		marked = b;
	}

	public boolean isMarked() {
		return marked;
	}

	public String getBorderType() {
		return BorderManager.getBorder(this);
	}

	public void setBorderType(String s) {
		BorderManager.setBorder(this, s, page.getBackground());
	}

	public void setPage(Page p) {
		page = p;
		if (getPageComponentListeners() != null) {
			Object o;
			for (Iterator it = getPageComponentListeners().iterator(); it.hasNext();) {
				o = it.next();
				if (o instanceof SaveReminder) {
					it.remove();
				}
			}
		}
		addPageComponentListener(p.getSaveReminder());
	}

	public Page getPage() {
		return page;
	}

	public void setChangable(boolean b) {
		changable = b;
	}

	public boolean isChangable() {
		return changable;
	}

	public static PageMolecularViewer create(Page page) {
		if (page == null)
			return null;
		PageMolecularViewer v = (PageMolecularViewer) InstancePool.sharedInstance().getUnusedInstance(
				PageMolecularViewer.class);
		v.reset();
		if (maker == null) {
			maker = new PageMolecularViewerMaker(v);
		}
		else {
			maker.setObject(v);
		}
		maker.invoke(page);
		if (maker.cancel) {
			InstancePool.sharedInstance().setStatus(v, false);
			return null;
		}
		v.enableMenuBar(true);
		v.enableToolBar(true);
		v.enableBottomBar(true);
		return v;
	}

	protected void setViewerSize(Dimension dim) {
		setPreferredSize(dim);
		if (page != null)
			page.reload();
	}

	public void setPreferredSize(Dimension dim) {
		if (dim == null)
			throw new IllegalArgumentException("null object");
		if (dim.width == 0 || dim.height == 0)
			throw new IllegalArgumentException("zero dimension");
		super.setPreferredSize(dim);
		super.setMinimumSize(dim);
		super.setMaximumSize(dim);
	}

	private ColorMenu createColorSelectionMenu(final String type) {
		String s = getInternationalText("CustomColor");
		String title = s != null ? s : "Custom Color";
		final boolean isBackgroundMenu = "background".equals(type);
		if (isBackgroundMenu)
			title = "Background";
		final ColorMenu colorMenu = isBackgroundMenu ? new ColorMenu(this, title, ModelerUtilities.colorChooser,
				ModelerUtilities.fillEffectChooser) : new ColorMenu(this, title, ModelerUtilities.colorChooser);
		colorMenu.setColorSelectionOnly(!isBackgroundMenu);
		if (isBackgroundMenu) {
			colorMenu.addNoFillListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					changeFillMode(FillMode.getNoFillMode());
				}
			});
			colorMenu.addFillEffectListeners(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					changeFillMode(colorMenu.getFillEffectChooser().getFillMode());
				}
			}, null);
		}
		colorMenu.addColorArrayListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (isBackgroundMenu) {
					changeFillMode(new FillMode.ColorFill(colorMenu.getColor()));
				}
				else {
					setColor(type, colorMenu.getColor());
				}
			}
		});
		colorMenu.addMoreColorListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (isBackgroundMenu) {
					changeFillMode(new FillMode.ColorFill(colorMenu.getColorChooser().getColor()));
				}
				else {
					setColor(type, colorMenu.getColorChooser().getColor());
				}
			}
		});
		colorMenu.addHexColorListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Color oldColor = null;
				if (isBackgroundMenu) {
					FillMode fm = getFillMode();
					if (fm instanceof FillMode.ColorFill)
						oldColor = ((FillMode.ColorFill) fm).getColor();
				}
				Color c = colorMenu.getHexInputColor(oldColor);
				if (c == null)
					return;
				if (isBackgroundMenu) {
					changeFillMode(new FillMode.ColorFill(c));
				}
				else {
					setColor(type, c);
				}
			}
		});
		colorMenu.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				if (type.equalsIgnoreCase("atoms"))
					colorMenu.setColor(getAtomColor(0));
				else if (type.equalsIgnoreCase("bonds"))
					colorMenu.setColor(getBondColor(0));
				else if (type.equalsIgnoreCase("background")) {
					FillMode fm = getFillMode();
					if (fm instanceof FillMode.ColorFill)
						colorMenu.setColor(((FillMode.ColorFill) fm).getColor());
				}
			}
		});
		return colorMenu;
	}

	private void setColor(String type, Color c) {
		if (type.equalsIgnoreCase("bonds")) {
			selectBondColorInheritMode(false);
			runScript("color bonds [" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + "];");
		}
		else if (type.equalsIgnoreCase("hbonds")) {
			selectHBondColorInheritMode(false);
			runScript("color hbonds [" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + "];");
		}
		else if (type.equalsIgnoreCase("ssbonds")) {
			selectSSBondColorInheritMode(false);
			runScript("color ssbonds [" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + "];");
		}
		else if (type.equalsIgnoreCase("atoms")) {
			selectAtomSingleColorMode(true);
			runScript("color atoms [" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + "];");
		}
		else if (type.equalsIgnoreCase("background")) {
			setFillMode(new FillMode.ColorFill(c));
			getView().repaint();
		}
	}

	public void moleculeLoaded(LoadMoleculeEvent e) {
		super.moleculeLoaded(e);
		notifyPageComponentListeners(new PageComponentEvent(this, PageComponentEvent.COMPONENT_LOADED));
		String address = page.getAddress().toLowerCase();
		if (address.endsWith(".pdb") || address.endsWith(".xyz"))
			page.getSaveReminder().setChanged(false);
		notifyModelListeners(new ModelEvent(this, ModelEvent.MODEL_INPUT));
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("<class>" + getClass().getName() + "</class>\n");
		String s = getCustomInitializationScript();
		if (s != null && !s.trim().equals(""))
			sb.append("<script>" + XMLCharacterEncoder.encode(s) + "</script>");
		if (getSpinOn())
			sb.append("<spin>true</spin>");
		if (areAxesShown())
			sb.append("<axes>true</axes>");
		if (isBoundBoxShown())
			sb.append("<boundbox>true</boundbox>");
		if (!isMenuBarEnabled())
			sb.append("<menubar>false</menubar>");
		if (!isToolBarEnabled())
			sb.append("<toolbar>false</toolbar>");
		if (!isBottomBarEnabled())
			sb.append("<statusbar>false</statusbar>");
		if (isDotsEnabled())
			sb.append("<dots>true</dots>\n");
		if (getNavigationMode())
			sb.append("<navigation>true</navigation>");
		s = getResourceAddress();
		if (s != null)
			sb.append("<resource>" + XMLCharacterEncoder.encode(FileUtilities.getFileName(s)) + "</resource>");
		s = FileUtilities.removeSuffix(page.getAddress()) + "$" + index + ".jms";
		sb.append("<state>" + XMLCharacterEncoder.encode(FileUtilities.getFileName(s)) + "</state>");
		sb.append("<width>" + (widthIsRelative ? (widthRatio > 1.0f ? 1 : widthRatio) : getWidth()) + "</width>\n");
		sb.append("<height>" + (heightIsRelative ? (heightRatio > 1.0f ? 1 : heightRatio) : getHeight())
				+ "</height>\n");
		if (!getBorderType().equals(BorderManager.BORDER_TYPE[0]))
			sb.append("<border>" + getBorderType() + "</border>\n");
		return sb.toString();
	}

}