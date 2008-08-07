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

package org.concord.mw3d;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

import org.myjmol.api.Cockpit;
import org.myjmol.api.JmolAdapter;
import org.myjmol.api.JmolViewer;
import org.myjmol.api.Navigator;
import org.myjmol.api.Scene;
import org.myjmol.viewer.JmolConstants;

import org.concord.modeler.ConnectionManager;
import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.draw.Draw;
import org.concord.modeler.draw.DrawingElement;
import org.concord.modeler.draw.FillMode;
import org.concord.modeler.draw.GradientFactory;
import org.concord.modeler.ui.IconPool;
import org.concord.modeler.util.FileUtilities;
import org.concord.mw3d.models.ABond;
import org.concord.mw3d.models.Atom;
import org.concord.mw3d.models.CuboidObstacle;
import org.concord.mw3d.models.CylinderObstacle;
import org.concord.mw3d.models.MolecularModel;
import org.concord.mw3d.models.Molecule;
import org.concord.mw3d.models.Obstacle;
import org.concord.mw3d.models.RBond;
import org.concord.mw3d.models.TBond;

import static org.concord.mw3d.UserAction.*;

public class MolecularView extends Draw {

	public final static byte SPACE_FILLING = 0x00;
	public final static byte BALL_AND_STICK = 0x01;
	public final static byte STICKS = 0x02;
	public final static byte WIREFRAME = 0x03;

	public final static byte FRONT_VIEW = 0x50;
	public final static byte BACK_VIEW = 0x51;
	public final static byte TOP_VIEW = 0x52;
	public final static byte BOTTOM_VIEW = 0x53;
	public final static byte RIGHT_VIEW = 0x54;
	public final static byte LEFT_VIEW = 0x55;

	final static float DEFAULT_VDW_LINE_RATIO = 1.67f;
	private final static float COS45 = 0.70710678f;
	private final static float ZERO = 1000 * Float.MIN_VALUE;

	private final static Font FONT_BOLD_18 = new Font(null, Font.BOLD, 18);
	private final static Font FONT_BOLD_15 = new Font(null, Font.BOLD, 15);
	private final static Font FONT_PLAIN_12 = new Font(null, Font.PLAIN, 12);
	final static NumberFormat FORMAT = NumberFormat.getNumberInstance();
	static {
		FORMAT.setMaximumFractionDigits(2);
		FORMAT.setMaximumIntegerDigits(3);
	}

	private static Map<String, Byte> nameIdMap;

	private final Dimension currentSize = new Dimension();
	private final Rectangle rectClip = new Rectangle();

	MolecularModel model;
	Object selectedComponent;
	short[] obstacleIndexAndFace;
	private MolecularContainer molecularContainer;
	private JmolViewer viewer;
	private JmolAdapter adapter;
	ErrorReminder errorReminder;

	private Cursor externalCursor;
	private boolean renderingCallTriggeredByLoading;
	private byte actionID = DEFA_ID;
	private Atom atomCopy;
	private int atomCopyIndex;
	private byte viewAngle = FRONT_VIEW;
	private String orientation;
	private Scene startingScene;
	private boolean showGlassSimulationBox = true;
	private boolean showEnergizer;
	private boolean showClock;
	private boolean showCharge;
	private boolean showVdwLines;
	private boolean showAtomIndex;
	private boolean keShading;
	private boolean fullSizeUnbondedAtoms;
	private float vdwLinesRatio = DEFAULT_VDW_LINE_RATIO;
	private short velocityScalingFactor = 1000;
	private String infoString;
	private int cameraAtom = -1;
	private Vector3f velocityVector, cameraVector;
	private KeyManager keyManager;
	Navigator navigator;
	private boolean isKeyNavigation;
	private Cockpit cockpit;
	private Map<String, Integer> genericElementColors;

	private String resourceAddress; // a copy from MolecularContainer
	private String codeBase; // different from resourceAddress, codeBase retains the remote form (not cached)
	private Energizer energizer;
	private Clock clock;
	private volatile boolean paintLoadingMessage;
	private final Object lock = new Object();
	private byte selectedFace;
	private int keyCode;
	private SelectedArea selectedArea;
	private Runnable rightClickJob;
	private final Object updateLock = new Object();
	private FillMode fillMode = FillMode.getNoFillMode();
	private Point3f clickedAtomPosition;
	private String currentElementToAdd = "X1";
	private int currentMoleculeToAdd;
	private byte molecularStyle = SPACE_FILLING;
	private ImageIcon backgroundImage;
	private int iconWidth, iconHeight;
	private Molecule newMolecule;

	private Point dragPoint = new Point();
	// used to moving mode to store the point of a component where it is clicked. Also used in PUSH mode.
	private Point clickPoint = new Point();
	private Point3f clickPoint3D = new Point3f();
	private Point3f dragPoint3D = new Point3f();
	private float absMax;
	private float originalValue;
	private boolean shapeWithinBounds;
	private float originalCenter, originalCorner;
	private float positionCenter, positionCorner;

	private BitSet velocityBitSet;
	private BitSet trajectoryBitSet;
	private BitSet translucentBitSet;
	private BitSet hidenBitSet;
	private BitSet sharedBitSet;
	private BitSet multiselectionBitSet;

	private DefaultPopupMenu defaultPopupMenu;
	private AtomPopupMenu atomPopupMenu;
	private RBondPopupMenu rbondPopupMenu;
	private ABondPopupMenu abondPopupMenu;
	private TBondPopupMenu tbondPopupMenu;
	private MoleculePopupMenu moleculePopupMenu;
	private ObstaclePopupMenu obstaclePopupMenu;
	private ImportModelPopupMenu importModelPopupMenu;
	private ViewProperties viewProp;

	private MouseMotionListener jmolMouseMotionListener;
	private ActionListener snapshotListener;

	public MolecularView() {

		if (nameIdMap == null) {
			nameIdMap = new HashMap<String, Byte>();
			String[] s = JmolConstants.elementSymbols;
			for (byte i = 0; i < s.length; i++) {
				nameIdMap.put(s[i], i);
			}
		}

		adapter = new Mw3dJmolAdapter(null);
		viewer = JmolViewer.allocateExtendedViewer(this, adapter);
		viewer.setPerspectiveDepth(true);
		viewer.setCameraSpin(true);
		viewer.setShowAxes(true);
		viewer.setAxisStyle((byte) 1);
		viewer.setAxisDiameter(200);
		viewer.setAutoBond(false);
		viewer.setShowRebondTime(false);
		viewer.setSelectionHaloEnabled(true);
		viewer.setDisablePopupMenu(true);
		viewer.setColorBackground(Color.black);
		setMolecularStyle(SPACE_FILLING);

		jmolMouseMotionListener = getMouseMotionListeners()[1];

		atomCopy = new Atom();
		errorReminder = new ErrorReminder(this);
		velocityBitSet = new BitSet(MolecularModel.SIZE);
		viewer.setVelocityBitSet(velocityBitSet);
		trajectoryBitSet = new BitSet(MolecularModel.SIZE);
		viewer.setTrajectoryBitSet(trajectoryBitSet);
		translucentBitSet = new BitSet(MolecularModel.SIZE);
		viewer.setTranslucentBitSet(translucentBitSet);
		hidenBitSet = new BitSet(MolecularModel.SIZE);
		viewer.setHidenBitSet(hidenBitSet);
		multiselectionBitSet = new BitSet(MolecularModel.SIZE);

		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				updateSize();
			}
		});

		selectedArea = new SelectedArea();

		Action a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				removeAtoms(viewer.getSelectionSet());
				if (selectedComponent instanceof Atom) {
					atomCopy.setAtom((Atom) selectedComponent);
					atomCopyIndex = ((Atom) selectedComponent).getIndex();
					setPastingObject(atomCopy);
				}
				else if (selectedComponent instanceof RBond) {
					removeRBond((RBond) selectedComponent);
				}
				else if (selectedComponent instanceof ABond) {
					removeABond((ABond) selectedComponent);
				}
				else if (selectedComponent instanceof TBond) {
					removeTBond((TBond) selectedComponent);
				}
				if (obstacleIndexAndFace != null)
					removeObstacle(obstacleIndexAndFace[0]);
				repaint();
				model.notifyChange();
			}
		};
		a.putValue(Action.NAME, "Cut");
		a.putValue(Action.SMALL_ICON, IconPool.getIcon("cut"));
		a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_U);
		a.putValue(Action.ACCELERATOR_KEY, System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(
				KeyEvent.VK_X, KeyEvent.META_MASK, true) : KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_MASK,
				true));
		getInputMap().put((KeyStroke) a.getValue(Action.ACCELERATOR_KEY), "cut");
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, true), "cut");
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0, true), "cut");
		getActionMap().put("cut", a);

		a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (selectedComponent instanceof Atom) {
					atomCopy.setAtom((Atom) selectedComponent);
					atomCopyIndex = ((Atom) selectedComponent).getIndex();
					setPastingObject(atomCopy);
				}
				else {
					setPastingObject(selectedComponent);
				}
			}
		};
		a.putValue(Action.NAME, "Copy");
		a.putValue(Action.SMALL_ICON, IconPool.getIcon("copy"));
		a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
		a.putValue(Action.ACCELERATOR_KEY, System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(
				KeyEvent.VK_C, KeyEvent.META_MASK, true) : KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK,
				true));
		getInputMap().put((KeyStroke) a.getValue(Action.ACCELERATOR_KEY), "copy");
		getActionMap().put("copy", a);

		a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				Object o = getPastingObject();
				if (o instanceof Atom) {
					pasteAtom(getPressedPoint());
				}
				else if (o instanceof DrawingElement) {
					pasteElement(getPressedPoint().x, getPressedPoint().y);
				}
			}
		};
		a.putValue(Action.NAME, "Paste");
		a.putValue(Action.SMALL_ICON, IconPool.getIcon("paste"));
		a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_P);
		a.putValue(Action.SHORT_DESCRIPTION, "Paste to the last clicked point");
		a.putValue(Action.ACCELERATOR_KEY, System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(
				KeyEvent.VK_V, KeyEvent.META_MASK, true) : KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_MASK,
				true));
		getInputMap().put((KeyStroke) a.getValue(Action.ACCELERATOR_KEY), "paste");
		getActionMap().put("paste", a);

		a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				invertSelection();
			}
		};
		a.putValue(Action.NAME, "Invert Selection");
		a.putValue(Action.SMALL_ICON, new ImageIcon(MolecularContainer.class.getResource("resources/yingyang.gif")));
		a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_I);
		a.putValue(Action.SHORT_DESCRIPTION, "Invert selection");
		a.putValue(Action.ACCELERATOR_KEY, System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(
				KeyEvent.VK_I, KeyEvent.META_MASK, true) : KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_MASK,
				true));
		getInputMap().put((KeyStroke) a.getValue(Action.ACCELERATOR_KEY), "invert selection");
		getActionMap().put("invert selection", a);

		navigator = new Navigator(viewer) {
			public void home() {
				MolecularView.this.home();
			}
		};
		navigator.setBackground(Color.black);
		navigator.setLocation(4, 4);
		keyManager = new KeyManager();

		cockpit = new Cockpit(viewer);
		cockpit.setBackground(Color.black);

		genericElementColors = new HashMap<String, Integer>();
		genericElementColors.put("X1", 0xffffffff);
		genericElementColors.put("X2", 0xff00ff00);
		genericElementColors.put("X3", 0xff0000ff);
		genericElementColors.put("X4", 0xff00ffff);

	}

	private void home() {
		if (viewer.getNavigationMode()) {
			if (startingScene != null) {
				Thread t = new Thread() {
					public void run() {
						viewer.moveCameraToScene(startingScene, false);
					}
				};
				t.setName("Resetting View");
				t.setPriority(Thread.MIN_PRIORITY);
				t.start();
			}
		}
		else {
			runJmolScript(startingScene != null ? "moveto " + 1 + " " + startingScene.rotationToString()
					: "moveto 1 0 0 0 0");
		}
	}

	void setSnapshotListener(ActionListener a) {
		snapshotListener = a;
	}

	public ActionListener getSnapshotListener() {
		return snapshotListener;
	}

	void setResourceAddress(String s) {
		resourceAddress = s;
	}

	public String getResourceAddress() {
		return resourceAddress;
	}

	void setCodeBase(String s) {
		codeBase = s;
	}

	public String getCodeBase() {
		return codeBase;
	}

	void setContainer(MolecularContainer mc) {
		molecularContainer = mc;
	}

	public MolecularContainer getContainer() {
		return molecularContainer;
	}

	private void pasteAtom(Point p) {
		if (atomCopy.getSymbol() == null)
			return;
		boolean b = false;
		currentElementToAdd = atomCopy.getSymbol();
		switch (actionID) {
		case XADD_ID:
			b = addAtom(viewer.findPointOnDropPlane('x', p.x, p.y));
			break;
		case YADD_ID:
			b = addAtom(viewer.findPointOnDropPlane('y', p.x, p.y));
			break;
		case ZADD_ID:
			b = addAtom(viewer.findPointOnDropPlane('z', p.x, p.y));
			break;
		}
		if (b) {
			Atom a2 = model.getAtom(model.getAtomCount() - 1);
			a2.setVx(atomCopy.getVx());
			a2.setVy(atomCopy.getVy());
			a2.setVz(atomCopy.getVz());
			viewer.setAtomVelocities(a2.getIndex(), a2.getVx(), a2.getVy(), a2.getVz());
			showVelocity(a2.getIndex(), velocityShown(atomCopyIndex));
			showTrajectory(a2.getIndex(), hasTrajectory(atomCopyIndex));
			repaint();
		}
	}

	BitSet getTrajectoryBitSet() {
		return trajectoryBitSet;
	}

	BitSet getVelocityBitSet() {
		return velocityBitSet;
	}

	BitSet getTranslucentBitSet() {
		return translucentBitSet;
	}

	int getElementArgb(String element) {
		return genericElementColors.get(element);
	}

	void setElementArgb(String element, int argb) {
		genericElementColors.put(element, argb);
	}

	Color getElementColor(Atom a) {
		return new Color(viewer.getAtomArgb(a.getIndex()));
	}

	void setElementColor(byte id, Color color) {
		int n = viewer.getAtomCount();
		String element = null;
		for (int i = 0; i < n; i++) {
			if (model.getAtom(i).getElementNumber() == id) {
				viewer.setAtomColor(i, color.getRGB());
				if (element == null)
					element = model.getAtom(i).getSymbol();
			}
		}
		if (element != null)
			genericElementColors.put(element, color.getRGB());
	}

	void setObstacleColor(Obstacle obs, Color color, boolean translucent) {
		obs.setColor(color);
		obs.setTranslucent(translucent);
		viewer.setObstacleColor(model.indexOfObstacle(obs), color, translucent);
		repaint();
	}

	void createPopupMenusRelatedToContainer(MolecularContainer c) {
		if (defaultPopupMenu == null)
			defaultPopupMenu = new DefaultPopupMenu(c);
		if (importModelPopupMenu == null)
			importModelPopupMenu = new ImportModelPopupMenu(c);
	}

	void showViewProperties() {
		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("must be called in the event thread.");
		if (viewProp == null)
			viewProp = new ViewProperties(this);
		viewProp.setCurrentValues();
		viewProp.setVisible(true);
	}

	public void setKeShading(boolean b) {
		keShading = b;
		if (!b)
			viewer.resetDefaultAtomColors();
		refresh();
		repaint();
	}

	public boolean getKeShading() {
		return keShading;
	}

	public void setFullSizeUnbondedAtoms(boolean b) {
		fullSizeUnbondedAtoms = b;
		setCpkPercent(viewer.getPercentVdwAtom());
		repaint();
	}

	public boolean getFullSizeUnbondedAtoms() {
		return fullSizeUnbondedAtoms;
	}

	public void setShowVdwLines(boolean b) {
		showVdwLines = b;
		if (!b)
			viewer.setVdwForceLines(null);
		refresh();
		repaint();
	}

	public boolean getShowVdwLines() {
		return showVdwLines;
	}

	public void setVdwLinesRatio(float ratio) {
		if (vdwLinesRatio == ratio)
			return;
		vdwLinesRatio = ratio;
		model.notifyChange();
		refresh();
		repaint();
	}

	public float getVdwLinesRatio() {
		return vdwLinesRatio;
	}

	public void setVelocityVectorScalingFactor(short s) {
		velocityScalingFactor = s;
		viewer.setVelocityVectorScalingFactor(s);
	}

	public short getVelocityVectorScalingFactor() {
		return velocityScalingFactor;
	}

	public void setShowCharge(boolean b) {
		showCharge = b;
		viewer.setShowCharge(b);
		repaint();
	}

	public boolean getShowCharge() {
		return showCharge;
	}

	public void setShowAtomIndex(boolean b) {
		showAtomIndex = b;
		viewer.setShowAtomIndex(b);
		repaint();
	}

	public boolean getShowAtomIndex() {
		return showAtomIndex;
	}

	public void setMolecularStyle(byte style) {
		molecularStyle = style;
		switch (molecularStyle) {
		case SPACE_FILLING:
			setCpkPercent(100);
			viewer.setMarBond((short) 1000);
			break;
		case BALL_AND_STICK:
			setCpkPercent(25);
			viewer.setMarBond((short) 400);
			break;
		case STICKS:
			setCpkPercent(10);
			viewer.setMarBond((short) 400);
			break;
		case WIREFRAME:
			setCpkPercent(5);
			viewer.setMarBond((short) 1);
			break;
		}
		repaint();
	}

	public byte getMolecularStyle() {
		return molecularStyle;
	}

	public void setViewAngle(byte i) {
		synchronized (lock) {
			viewAngle = i;
		}
		int delta = 5;
		switch (viewAngle) {
		case FRONT_VIEW:
			// special treatment because the rotation center is that of the atom set but not the box center
			if (viewer.getNavigationMode()) {
				viewer.runScriptImmediatelyWithoutThread("reset");
				viewer.setCameraPosition(0, 0, model.getHeight() + delta);
			}
			else {
				viewer.frontView(model.getHeight());
			}
			break;
		case BACK_VIEW:
			if (viewer.getNavigationMode()) {
				viewer.runScriptImmediatelyWithoutThread("reset");
				viewer.setCameraPosition(0, 0, -(model.getHeight() + delta));
				viewer.evalStringQuiet("moveto 0 back");
			}
			else {
				viewer.backView(-model.getHeight());
			}
			break;
		case TOP_VIEW:
			if (viewer.getNavigationMode()) {
				viewer.runScriptImmediatelyWithoutThread("reset");
				viewer.setCameraPosition(0, (model.getWidth() + delta), 0);
				viewer.evalStringQuiet("moveto 0 top");
			}
			else {
				viewer.topView(model.getWidth());
			}
			break;
		case BOTTOM_VIEW:
			if (viewer.getNavigationMode()) {
				viewer.runScriptImmediatelyWithoutThread("reset");
				viewer.setCameraPosition(0, -(model.getWidth() + delta), 0);
				viewer.evalStringQuiet("moveto 0 bottom");
			}
			else {
				viewer.bottomView(model.getWidth());
			}
			break;
		case LEFT_VIEW:
			if (viewer.getNavigationMode()) {
				viewer.runScriptImmediatelyWithoutThread("reset");
				viewer.setCameraPosition(-(model.getLength() + delta), 0, 0);
				viewer.evalStringQuiet("moveto 0 left");
			}
			else {
				viewer.leftView(model.getLength());
			}
			break;
		case RIGHT_VIEW:
			if (viewer.getNavigationMode()) {
				viewer.runScriptImmediatelyWithoutThread("reset");
				viewer.setCameraPosition(model.getLength() + delta, 0, 0);
				viewer.evalStringQuiet("moveto 0 right");
			}
			else {
				viewer.rightView(model.getLength());
			}
			break;
		}
	}

	boolean isAddingAtomMode() {
		return actionID >= XADD_ID && actionID <= ZADD_ID;
	}

	boolean isAddingMoleculeMode() {
		return actionID >= XMOL_ID && actionID <= ZMOL_ID;
	}

	private boolean isImportingModelMode() {
		return actionID >= XFIL_ID && actionID <= ZFIL_ID;
	}

	private boolean isAddingObstacleMode() {
		return actionID >= XREC_ID && actionID <= ZOVL_ID;
	}

	private boolean isBuildingBondMode() {
		return actionID == RBND_ID || actionID == ABND_ID || actionID == TBND_ID;
	}

	private boolean isChargingMode() {
		return actionID == PCHG_ID || actionID == NCHG_ID;
	}

	public void setActionID(byte id) {
		actionID = id;
		viewer.setMeasurementEnabled(actionID == DEFA_ID);
		boolean addingAtom = isAddingAtomMode();
		boolean addingObstacle = isAddingObstacleMode();
		boolean buildingBond = isBuildingBondMode();
		boolean addingMolecule = isAddingMoleculeMode();
		boolean chargingAtom = isChargingMode();
		boolean importingModel = isImportingModelMode();
		if (id != DEFA_ID && id != SLAT_ID && id != VVEL_ID && !addingAtom && !addingMolecule && !importingModel
				&& !addingObstacle && !buildingBond && !chargingAtom) {
			setRotationEnabled(false);
		}
		else {
			setRotationEnabled(true);
			setMode(DEFAULT_MODE);
		}
		if (id != VVEL_ID) {
			viewer.setIndexOfAtomOfSelectedVelocity(-1);
		}
		if (!addingAtom && !addingObstacle && !buildingBond && !addingMolecule && !importingModel) {
			viewer.setDropPlaneVisible('x', false);
			viewer.setDropPlaneVisible('y', false);
			viewer.setDropPlaneVisible('z', false);
		}
		else {
			switch (id) {
			case XADD_ID:
			case XMOL_ID:
			case XFIL_ID:
			case XREC_ID:
			case XOVL_ID:
				viewer.setDropPlaneVisible('x', true);
				break;
			case YADD_ID:
			case YMOL_ID:
			case YFIL_ID:
			case YREC_ID:
			case YOVL_ID:
				viewer.setDropPlaneVisible('y', true);
				break;
			case ZADD_ID:
			case ZMOL_ID:
			case ZFIL_ID:
			case ZREC_ID:
			case ZOVL_ID:
				viewer.setDropPlaneVisible('z', true);
				break;
			}
		}
		setCursor(UserAction.getCursor(id));
		repaint();
	}

	private synchronized void setRotationEnabled(boolean b) {
		if (b) {
			if (!hasJmolMouseMotionListener())
				addMouseMotionListener(jmolMouseMotionListener);
		}
		else {
			removeMouseMotionListener(jmolMouseMotionListener);
		}
	}

	private synchronized boolean hasJmolMouseMotionListener() {
		MouseMotionListener[] mml = getMouseMotionListeners();
		for (MouseMotionListener i : mml) {
			if (i == jmolMouseMotionListener)
				return true;
		}
		return false;
	}

	byte getActionID() {
		return actionID;
	}

	public byte getViewAngle() {
		synchronized (lock) {
			return viewAngle;
		}
	}

	public void setCameraAtom(int index) {
		cameraAtom = index;
		if (cameraAtom >= 0) {
			if (velocityVector == null)
				velocityVector = new Vector3f();
			if (cameraVector == null)
				cameraVector = new Vector3f();
			if (!viewer.isCameraSpin())
				viewer.setCameraSpin(true);
		}
		refresh();
	}

	public int getCameraAtom() {
		return cameraAtom;
	}

	/**
	 * Sending the current coordinates to JmolViewer and refresh the screen image. NOTE that this method does NOT need
	 * to be called every time a repaint() method is called!!!!! (If you do so, you will slow the repainting method down
	 * if there is no change of coordinates.)
	 */
	public void refresh() {
		synchronized (viewer) {
			int n = model.getAtomCount();
			Atom at;
			for (int i = 0; i < n; i++) {
				at = model.getAtom(i);
				if (keShading) {
					viewer.setAtomCoordinates(i, at.getRx(), at.getRy(), at.getRz(), getKeShadingColor(at.getKe()));
				}
				else {
					viewer.setAtomCoordinates(i, at.getRx(), at.getRy(), at.getRz());
				}
				if (velocityBitSet.get(i))
					viewer.setAtomVelocities(i, at.getVx(), at.getVy(), at.getVz());
			}
			if (showVdwLines) {
				viewer.setVdwForceLines(model.getVdwPairs());
			}
			if (viewer.getNavigationMode() && cameraAtom >= 0) {
				at = model.getAtom(cameraAtom);
				if (at != null) {
					if (hasCameraMoved(at)) {
						if (!mountCameraOn(at)) {
							at.setVx(at.getVx() + 0.000001f); // shift a bit to handle singularity
							mountCameraOn(at);
						}
					}
				}
			}
			viewer.refresh();
		}
	}

	private boolean mountCameraOn(Atom at) {
		velocityVector.set(at.getVx(), at.getVy(), at.getVz());
		cameraVector.set(0, 0, 1);
		float angle = (float) Math.PI - velocityVector.angle(cameraVector);
		cameraVector.cross(cameraVector, velocityVector);
		if (cameraVector.lengthSquared() < ZERO) // special case: camera exactly lies in z-axis
			return false;
		viewer.moveCameraTo(0, cameraVector.x, cameraVector.y, cameraVector.z, (float) Math.toDegrees(angle), at
				.getRx(), at.getRy(), at.getRz());
		return true;
	}

	private boolean hasCameraMoved(Atom at) {
		if (Math.abs(velocityVector.x - at.getVx()) > ZERO || Math.abs(velocityVector.y - at.getVy()) > ZERO
				|| Math.abs(velocityVector.z - at.getVz()) > ZERO)
			return true;
		Point3f p = viewer.getCameraPosition();
		if (Math.abs(p.x - at.getRx()) > ZERO || Math.abs(p.y - at.getRy()) > ZERO || Math.abs(p.z - at.getRz()) > ZERO)
			return true;
		return false;
	}

	public void refreshTrajectories() {
		Atom a = null;
		int m = model.getTapePointer();
		int n = model.getAtomCount();
		for (int i = 0; i < n; i++) {
			if (trajectoryBitSet.get(i)) {
				a = model.getAtom(i);
				viewer.setTrajectory(i, m, a.getTrajectoryRx(), a.getTrajectoryRy(), a.getTrajectoryRz());
			}
		}
	}

	void reactToTapeReset() {
		refreshTrajectories();
	}

	private int getKeShadingColor(float ke) {
		int icolor = 0;
		icolor = (int) (ke * 64000);
		if (icolor > 0xff)
			icolor = 0xff;
		return (0xff << 24) | (0xff << 16) | ((0xff ^ icolor) << 8) | (0xff ^ icolor);
	}

	void setLoadingMessagePainted(boolean b) {
		paintLoadingMessage = b;
	}

	boolean isLoadingMessagePainted() {
		return paintLoadingMessage;
	}

	public JmolViewer getViewer() {
		return viewer;
	}

	boolean isRenderingCallTriggeredByLoading() {
		return renderingCallTriggeredByLoading;
	}

	void renderModel(boolean renderingCallTriggeredByLoading) {
		this.renderingCallTriggeredByLoading = renderingCallTriggeredByLoading;
		viewer.setSpinOn(false);
		try {
			viewer.openClientObject(model);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		postReadingClientObject();
	}

	// FIXME: find a way to bypass the annoying JmolAdapter
	private void postReadingClientObject() {
		viewer.setTainted(true);
		viewer.clearSelection();
		if (startingScene != null) {
			viewer.moveCameraToScene(startingScene, true);
		}
		else {
			viewer.moveTo(0, 0, 0, 1, 0, 100, 0, 0);
		}
		Atom a;
		int natom = model.getAtomCount();
		if (natom > 0) {
			for (int i = 0; i < natom; i++) {
				a = model.getAtom(i);
				viewer.setCharge(i, a.getCharge());
			}
		}
		setAxesShown(areAxesShown());
		setMolecularStyle(molecularStyle);
		model.formMolecules();
	}

	void setStartingSceneWhenCameraIsOnAtom() {
		if (cameraAtom >= 0) {
			refresh();
			storeCurrentOrientation();
			String[] t = orientation.split("\\s");
			float[] x = new float[7];// x[0], x[1], x[2] - rotation axis; x[3] - rotation degrees
			x[4] = 100; // zoom percent
			x[5] = 0; // x translation
			x[6] = 0; // y translation
			for (int i = 0; i < Math.min(t.length, 7); i++) {
				x[i] = Float.parseFloat(t[i]);
			}
			if (startingScene == null) {
				startingScene = new Scene(viewer.getCameraPosition(), new Vector3f(x[0], x[1], x[2]), x[3], x[4]);
			}
			else {
				startingScene.getRotationAxis().set(x[0], x[1], x[2]);
				startingScene.setRotationAngle(x[3]);
				startingScene.setZoomPercent(x[4]);
			}
		}
	}

	void setSimulationBox() {
		viewer.setSimulationBox(model.getLength(), model.getWidth(), model.getHeight());
	}

	void setShowGlassSimulationBox(boolean b) {
		showGlassSimulationBox = b;
		viewer.setSimulationBoxVisible(b);
		repaint();
	}

	boolean getShowGlassSimulationBox() {
		return showGlassSimulationBox;
	}

	public void setModel(MolecularModel mm) {
		model = mm;
	}

	public MolecularModel getModel() {
		return model;
	}

	public String runJmolScript(String str) {
		if (str == null)
			return null;
		return viewer.evalStringQuiet(str);
	}

	private void runJmolScriptImmediatelyWithoutThread(String str) {
		if (str != null)
			viewer.runScriptImmediatelyWithoutThread(str);
	}

	private void storeCurrentOrientation() {
		orientation = viewer.getCurrentOrientation();
	}

	public String getOrientation() {
		return orientation;
	}

	public void setOrientation(String s) {
		orientation = s;
	}

	void setStartingScene(Vector3f rotationAxis, float degrees, float zoomPercent) {
		startingScene = new Scene(viewer.getCameraPosition(), rotationAxis, degrees, zoomPercent);
		startingScene.setTransitionTime((short) 1);
	}

	public FillMode getFillMode() {
		return fillMode;
	}

	public void setFillMode(FillMode fm) {
		fillMode = fm;
		if (fillMode == FillMode.getNoFillMode()) {
			setBackground(Color.black);
			setBackgroundImage(null);
		}
		else if (fillMode instanceof FillMode.ColorFill) {
			setBackground(((FillMode.ColorFill) fillMode).getColor());
			setBackgroundImage(null);
		}
		else if (fillMode instanceof FillMode.ImageFill) {
			setBackground(new Color(0x00000000, true));
			String s = ((FillMode.ImageFill) fillMode).getURL();
			if (FileUtilities.isRelative(s)) {
				if (resourceAddress == null) {
					setFillMode(FillMode.getNoFillMode());
				}
				s = FileUtilities.getCodeBase(resourceAddress) + s;
			}
			URL remoteCopy = ConnectionManager.sharedInstance().getRemoteCopy(s);
			if (remoteCopy != null)
				s = remoteCopy.toString();
			if (FileUtilities.isRemote(s)) {
				URL url = null;
				try {
					url = new URL(s);
				}
				catch (MalformedURLException e) {
					setBackgroundImage(null);
					repaint();
					return;
				}
				ImageIcon icon = ConnectionManager.sharedInstance().loadImage(url);
				if (icon != null && icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
					setBackgroundImage(icon);
				}
				else {
					setBackgroundImage(null);
				}
			}
			else {
				setBackgroundImage(new ImageIcon(Toolkit.getDefaultToolkit().createImage(s), s));
			}
		}
		else {
			setBackground(new Color(0x00000000, true));
			setBackgroundImage(null);
		}
		repaint();
	}

	public void setBackground(final Color c) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				MolecularView.super.setBackground(c);
			}
		});
		viewer.setColorBackground(c);
		navigator.setBackground(c);
		cockpit.setBackground(c);
	}

	public void changeFillMode(FillMode fm) {
		if (fm == null)
			return;
		if (fm.equals(getFillMode()))
			return;
		setFillMode(fm);
	}

	public void setBackgroundImage(ImageIcon icon) {
		if (icon == null) {
			backgroundImage = null;
			return;
		}
		backgroundImage = new ImageIcon(icon.getImage());
		backgroundImage.setDescription(icon.getDescription());
		iconWidth = backgroundImage.getIconWidth();
		iconHeight = backgroundImage.getIconHeight();
	}

	public void setCpkPercent(int percent) {
		synchronized (lock) {
			viewer.setPercentVdwAtom(percent);
			if (model != null) {
				int n = model.getAtomCount();
				if (fullSizeUnbondedAtoms) {
					for (int i = 0; i < n; i++) {
						if (model.getAtom(i).isBonded()) {
							viewer.setCpkPercent(i, percent);
						}
						else {
							viewer.setCpkPercent(i, 100);
						}
					}
				}
				else {
					for (int i = 0; i < n; i++) {
						viewer.setCpkPercent(i, percent);
					}
				}
			}
		}
	}

	public int getCpkPercent() {
		synchronized (lock) {
			return viewer.getPercentVdwAtom();
		}
	}

	public void setSpinOn(boolean b) {
		synchronized (lock) {
			viewer.setSpinOn(b);
		}
	}

	public boolean isSpinOn() {
		synchronized (lock) {
			return viewer.getSpinOn();
		}
	}

	public void setAxesShown(boolean b) {
		synchronized (lock) {
			viewer.setShowAxes(b);
		}
	}

	public boolean areAxesShown() {
		synchronized (lock) {
			return viewer.getShowAxes();
		}
	}

	public void setShowEnergizer(boolean b) {
		synchronized (lock) {
			showEnergizer = b;
		}
		if (b) {
			if (energizer == null)
				energizer = new Energizer(getWidth() - 18, 20, 100, model);
		}
		else {
			if (energizer != null)
				energizer.buttonPressed = false;
		}
		repaint();
	}

	public boolean getShowEnergizer() {
		synchronized (lock) {
			return showEnergizer;
		}
	}

	public void setShowClock(boolean b) {
		synchronized (lock) {
			showClock = b;
		}
		if (b) {
			if (clock == null)
				clock = new Clock(model);
		}
		repaint();
	}

	public boolean getShowClock() {
		synchronized (lock) {
			return showClock;
		}
	}

	void setRightClickJob(Runnable r) {
		rightClickJob = r;
	}

	public void showTrajectory(int index, boolean on) {
		if (index < 0 || index >= MolecularModel.SIZE)
			return;
		trajectoryBitSet.set(index, on);
		Atom a = model.getAtom(index);
		viewer.setTrajectory(index, model.getTapePointer(), a.getTrajectoryRx(), a.getTrajectoryRy(), a
				.getTrajectoryRz());
		repaint();
	}

	public boolean hasTrajectory(int index) {
		return trajectoryBitSet.get(index);
	}

	public void showVelocity(int index, boolean on) {
		if (index < 0 || index >= MolecularModel.SIZE)
			return;
		if (on) {
			Atom at = model.getAtom(index);
			viewer.setAtomVelocities(index, at.getVx(), at.getVy(), at.getVz());
		}
		velocityBitSet.set(index, on);
		repaint();
	}

	public boolean velocityShown(int index) {
		if (index < 0 || index >= MolecularModel.SIZE)
			return false;
		return velocityBitSet.get(index);
	}

	public void setSelectedElement(DrawingElement e) {
		super.setSelectedElement(e);
		selectAtom(-1);
	}

	void deselectAll() {
		selectAtom(-1);
		viewer.setHighlightCylinderVisible(false);
		viewer.setHighlightTriangleVisible(false);
		viewer.setHighlightTBondVisible(false);
	}

	private void addAtomSelection(int i) {
		if (i < 0)
			return;
		multiselectionBitSet.set(i);
		viewer.setSelectionSet(multiselectionBitSet);
		selectedComponent = model.getAtom(i);
	}

	private void selectAtom(int i) {
		multiselectionBitSet.clear();
		if (i >= 0) {
			selectedComponent = model.getAtom(i);
			multiselectionBitSet.set(i);
		}
		else {
			selectedComponent = null;
		}
		viewer.setSelectionSet(multiselectionBitSet);
	}

	public void setAtomSelected(int i) {
		if (i >= 0) {
			multiselectionBitSet.set(i);
		}
		else {
			multiselectionBitSet.clear();
		}
		viewer.setSelectionSet(multiselectionBitSet);
	}

	private void selectMolecule(int x, int y) {
		int i = viewer.findNearestAtomIndex(x, y);
		if (i >= 0) {
			selectRBond(-1);
			selectABond(-1);
			selectTBond(-1);
			Atom at = model.getAtom(i);
			Molecule mol = model.getMolecule(at);
			if (mol != null) {
				multiselectionBitSet.clear();
				int n = mol.getAtomCount();
				for (int k = 0; k < n; k++) {
					multiselectionBitSet.set(mol.getAtom(k).getIndex());
				}
				viewer.setSelectionSet(multiselectionBitSet);
			}
			selectedComponent = mol;
			setRotationEnabled(false);
		}
		else {
			selectAtom(-1);
			selectedComponent = null;
			setRotationEnabled(true);
		}
	}

	private void selectRBond(int i) {
		if (i >= 0) {
			selectedComponent = model.getRBond(i);
		}
		else {
			selectedComponent = null;
			viewer.setHighlightCylinderVisible(false);
		}
	}

	private void selectABond(int i) {
		if (i >= 0) {
			selectedComponent = model.getABond(i);
		}
		else {
			selectedComponent = null;
			viewer.setHighlightTriangleVisible(false);
		}
	}

	private void selectTBond(int i) {
		if (i >= 0) {
			selectedComponent = model.getTBond(i);
		}
		else {
			selectedComponent = null;
			viewer.setHighlightTBondVisible(false);
		}
	}

	private void dragRect(int x, int y) {
		if (x > selectedArea.getX0()) {
			selectedArea.width = x - selectedArea.getX0();
			selectedArea.x = selectedArea.getX0();
		}
		else {
			selectedArea.width = selectedArea.getX0() - x;
			selectedArea.x = selectedArea.getX0() - selectedArea.width;
		}
		if (y > selectedArea.y) {
			selectedArea.height = y - selectedArea.getY0();
			selectedArea.y = selectedArea.getY0();
		}
		else {
			selectedArea.height = selectedArea.getY0() - y;
			selectedArea.y = selectedArea.getY0() - selectedArea.height;
		}
		repaint();
	}

	public void setSize(int w, int h) {
		super.setSize(w, h);
		updateSize();
	}

	public void setSize(Dimension d) {
		super.setSize(d);
		updateSize();
	}

	void updateSize() {
		getSize(currentSize);
		// viewer.clearBounds();
		viewer.setScreenDimension(currentSize);
		refresh();
	}

	public void reset() {
		super.clear();
		viewer.clearScriptQueue(); // stop pending scripts immediately
		viewer.removeAll();
		viewer.setNavigationMode(false);
		vdwLinesRatio = DEFAULT_VDW_LINE_RATIO;
		showCharge = false;
		showVdwLines = false;
		showAtomIndex = false;
		keShading = false;
		setVelocityVectorScalingFactor((short) 1000);
		resetBitSets();
		shapeWithinBounds = false;
		selectedComponent = null;
		cameraAtom = -1;
		if (velocityVector != null)
			velocityVector.set(0, 0, 0);
		if (navigator != null)
			navigator.clear();
		startingScene = null;
		setActionID(DEFA_ID);
		externalCursor = null;
	}

	public void setExternalCursor(Cursor cursor) {
		externalCursor = cursor;
	}

	public void setCursor(Cursor cursor) {
		super.setCursor(externalCursor == null ? cursor : externalCursor);
	}

	void fitIntoWindow() {
		if (viewer.getNavigationMode())
			viewer.setCameraPosition(0, 0, 2 * model.getHeight());
		viewer.moveTo(0, 0, 0, 1, 0, 100, 0, 0);
		viewer.setCenter(0, 0, 0);
		viewer.fit2DScreen(5);
	}

	private void resetBitSets() {
		multiselectionBitSet.clear();
		trajectoryBitSet.clear();
		velocityBitSet.clear();
		translucentBitSet.clear();
		showAllAtoms();
	}

	void showAllAtoms() {
		hidenBitSet.clear();
		repaint();
	}

	public void setInfoString(String s) {
		infoString = s;
	}

	public void paintComponent(Graphics g) {
		if (backgroundImage != null) {
			int imax = getWidth() / iconWidth + 1;
			int jmax = getHeight() / iconHeight + 1;
			for (int i = 0; i < imax; i++) {
				for (int j = 0; j < jmax; j++) {
					backgroundImage.paintIcon(this, g, i * iconWidth, j * iconHeight);
				}
			}
		}
		if (fillMode instanceof FillMode.GradientFill) {
			FillMode.GradientFill gfm = (FillMode.GradientFill) fillMode;
			GradientFactory.paintRect((Graphics2D) g, gfm.getStyle(), gfm.getVariant(), gfm.getColor1(), gfm
					.getColor2(), 0, 0, getWidth(), getHeight());
		}
		else if (fillMode instanceof FillMode.PatternFill) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setPaint(((FillMode.PatternFill) fillMode).getPaint());
			g2.fillRect(0, 0, getWidth(), getHeight());
		}
		super.paintComponent(g);
		update(g);
	}

	public void update(Graphics g) {
		if (paintLoadingMessage) {
			Color bg = viewer.getColorBackground();
			g.setColor(bg);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(contrastBackground());
			g.setFont(FONT_BOLD_18);
			FontMetrics fm = g.getFontMetrics();
			String s = MolecularContainer.getInternationalText("PleaseWait");
			if (s == null)
				s = "Loading data, please wait...";
			g.drawString(s, (getWidth() - fm.stringWidth(s)) >> 1, (getHeight() - fm.getHeight()) >> 1);
			g.setFont(FONT_BOLD_15);
			fm = g.getFontMetrics();
			s = "3D Simulator, Molecular Workbench";
			g.drawString(s, (getWidth() - fm.stringWidth(s)) >> 1, (getHeight() >> 1) + fm.getHeight());
			return;
		}
		synchronized (updateLock) {
			viewer.setScreenDimension(getSize(currentSize));
			g.getClipBounds(rectClip);
			viewer.renderScreenImage(g, currentSize, rectClip);
			if (showEnergizer && energizer != null)
				energizer.paint(g);
			if (showClock)
				clock.paint(g);
			if (model.heatBathActivated()) {
				IconPool.getIcon("heat bath").paintIcon(this, g, 8, 8);
			}
			if (!model.isRunning()) {
				if (actionID == SLRT_ID || actionID == DELR_ID || actionID == FIXR_ID || actionID == XIFR_ID
						|| actionID == TSLC_ID || actionID == CLST_ID || actionID == HIDE_ID || actionID == EDIH_ID) {
					g.setColor(contrastBackground());
					g.drawRect(selectedArea.x, selectedArea.y, selectedArea.width, selectedArea.height);
				}
				else if (actionID == SLOV_ID || actionID == DELC_ID || actionID == FIXC_ID || actionID == XIFC_ID) {
					g.setColor(contrastBackground());
					g.drawOval(selectedArea.x, selectedArea.y, selectedArea.width, selectedArea.height);
				}
				else if (isAddingAtomMode() || isAddingObstacleMode() || isAddingMoleculeMode()) {
					paintDropPlaneInfo(g);
				}
			}
			paintInfoString(g);
			if (viewer.getNavigationMode()) {
				navigator.paint(g);
				cockpit.paint(g);
			}
			super.update(g);
		}
	}

	private void paintInfoString(Graphics g) {
		if (infoString == null)
			return;
		g.setColor(contrastBackground());
		int sw = g.getFontMetrics().stringWidth(infoString);
		g.drawString(infoString, (getWidth() - sw) / 2, getHeight() / 2);
	}

	private void paintDropPlaneInfo(Graphics g) {
		g.setColor(Color.orange);
		g.fillRoundRect(10, 6, 70, 20, 10, 10);
		g.setColor(contrastBackground());
		g.drawRoundRect(10, 6, 70, 20, 10, 10);
		g.setColor(viewer.getColorBackground());
		g.setFont(FONT_PLAIN_12);
		switch (actionID) {
		case XADD_ID:
		case XMOL_ID:
		case XFIL_ID:
		case XREC_ID:
		case XOVL_ID:
			g.drawString("x = " + FORMAT.format(viewer.getDropPlanePosition('x')), 20, 20);
			break;
		case YADD_ID:
		case YMOL_ID:
		case YFIL_ID:
		case YREC_ID:
		case YOVL_ID:
			g.drawString("y = " + FORMAT.format(viewer.getDropPlanePosition('y')), 20, 20);
			break;
		case ZADD_ID:
		case ZMOL_ID:
		case ZFIL_ID:
		case ZREC_ID:
		case ZOVL_ID:
			g.drawString("z = " + FORMAT.format(viewer.getDropPlanePosition('z')), 20, 20);
			break;
		}
	}

	/** return the lock to block the update(Graphics g) method. */
	public Object getUpdateLock() {
		return updateLock;
	}

	protected void processMousePressed(MouseEvent e) {

		super.processMousePressed(e);

		int x = e.getX();
		int y = e.getY();
		int clickCount = e.getClickCount();

		if (ModelerUtilities.isRightClick(e)) {
			if (getSelectedElement() == null || e.isShiftDown()) {
				if (obstacleIndexAndFace != null)
					obstacleIndexAndFace[0] = obstacleIndexAndFace[1] = -1;
				int i = viewer.findNearestAtomIndex(x, y);
				if (i >= 0) {
					selectRBond(-1);
					selectABond(-1);
					selectTBond(-1);
					viewer.setHighlightPlaneVisible(false);
					Atom at = model.getAtom(i);
					Molecule mol = model.getMolecule(at);
					if (e.isShiftDown() || mol == null) {
						selectAtom(i);
						if (atomPopupMenu == null)
							atomPopupMenu = new AtomPopupMenu(this);
						atomPopupMenu.setVeloSelected(velocityShown(i));
						atomPopupMenu.setTrajSelected(hasTrajectory(i));
						atomPopupMenu.setCameraAttached(cameraAtom == i);
						atomPopupMenu.show(this, x, y);
					}
					else {
						selectedComponent = mol;
						multiselectionBitSet.clear();
						int n = mol.getAtomCount();
						for (int k = 0; k < n; k++) {
							multiselectionBitSet.set(mol.getAtom(k).getIndex());
						}
						viewer.setSelectionSet(multiselectionBitSet);
						if (moleculePopupMenu == null)
							moleculePopupMenu = new MoleculePopupMenu(this);
						moleculePopupMenu.show(this, x, y);
					}
				}
				else if ((i = viewer.findNearestBondIndex(x, y)) >= 0) {
					selectAtom(-1);
					selectABond(-1);
					selectTBond(-1);
					selectRBond(i);
					if (rbondPopupMenu == null)
						rbondPopupMenu = new RBondPopupMenu(this);
					rbondPopupMenu.show(this, x, y);
				}
				else if ((i = viewer.findNearestABondIndex(x, y)) >= 0) {
					selectAtom(-1);
					selectRBond(-1);
					selectTBond(-1);
					selectABond(i);
					if (abondPopupMenu == null)
						abondPopupMenu = new ABondPopupMenu(this);
					abondPopupMenu.show(this, x, y);
				}
				else if ((i = viewer.findNearestTBondIndex(x, y)) >= 0) {
					selectAtom(-1);
					selectRBond(-1);
					selectABond(-1);
					selectTBond(i);
					if (tbondPopupMenu == null)
						tbondPopupMenu = new TBondPopupMenu(this);
					tbondPopupMenu.show(this, x, y);
				}
				else {
					obstacleIndexAndFace = viewer.findNearestObstacleIndexAndFace(x, y);
					if (obstacleIndexAndFace != null && obstacleIndexAndFace[0] >= 0 && obstacleIndexAndFace[1] >= 0) {
						selectAtom(-1);
						selectedComponent = model.getObstacle(obstacleIndexAndFace[0]);
						if (obstaclePopupMenu == null)
							obstaclePopupMenu = new ObstaclePopupMenu(this);
						obstaclePopupMenu.setObstacle(model.getObstacle(obstacleIndexAndFace[0]));
						obstaclePopupMenu.show(this, x, y);
					}
					else {
						defaultPopupMenu.show(this, x + 5, y + 5);
					}
				}
				if (rightClickJob != null)
					rightClickJob.run();
				return;
			}
		}

		if (viewer.getNavigationMode()) {
			if (navigator.navigate(x, y))
				return;
		}

		switch (actionID) {
		case DEFA_ID:
			setRotationEnabled(getSelectedElement() == null);
			break;
		case PANN_ID:
			dragPoint.setLocation(x, y);
			break;
		case SLAT_ID:
			int iat = viewer.findNearestAtomIndex(x, y);
			if (iat >= 0) {
				if (e.isShiftDown()) {
					addAtomSelection(iat);
				}
				else {
					multiselectionBitSet.clear();
					multiselectionBitSet.set(iat);
					selectAtom(iat);
				}
			}
			break;
		case PCHG_ID:
			iat = viewer.findNearestAtomIndex(x, y);
			if (iat >= 0) {
				model.getAtom(iat).addCharge(e.isShiftDown() ? -0.1f : 0.1f);
				runJmolScriptImmediatelyWithoutThread("label OFF;select atomno=" + (iat + 1) + "; label "
						+ FORMAT.format(model.getAtom(iat).getCharge()));
				model.notifyChange();
			}
			else {
				runJmolScriptImmediatelyWithoutThread("label OFF");
			}
			break;
		case NCHG_ID:
			iat = viewer.findNearestAtomIndex(x, y);
			if (iat >= 0) {
				model.getAtom(iat).addCharge(e.isShiftDown() ? 0.1f : -0.1f);
				runJmolScriptImmediatelyWithoutThread("label OFF;select atomno=" + (iat + 1) + "; label "
						+ FORMAT.format(model.getAtom(iat).getCharge()));
				model.notifyChange();
			}
			else {
				runJmolScriptImmediatelyWithoutThread("label OFF");
			}
			break;
		case ROTA_ID:
		case TRAN_ID:
		case DUPL_ID:
			dragPoint.setLocation(x, y);
			selectMolecule(x, y);
			break;
		case SBOX_ID:
			clickPoint.setLocation(x, y);
			selectedFace = viewer.getSimulationBoxFace(x, y);
			setRotationEnabled(selectedFace < 0);
			switch (selectedFace) {
			case 0x00: // front
			case 0x01: // rear
				clickPoint3D.set(viewer.findPointOnPlane('x', x, y, 0));
				absMax = model.getAbsZmax();
				originalValue = model.getHeight();
				break;
			case 0x02: // top
			case 0x03: // bottom
				clickPoint3D.set(viewer.findPointOnPlane('z', x, y, 0));
				absMax = model.getAbsYmax();
				originalValue = model.getWidth();
				break;
			case 0x04: // right
			case 0x05: // left
				clickPoint3D.set(viewer.findPointOnPlane('y', x, y, 0));
				absMax = model.getAbsXmax();
				originalValue = model.getLength();
				break;
			}
			break;
		case EXOB_ID:
			clickPoint.setLocation(x, y);
			obstacleIndexAndFace = viewer.findNearestObstacleIndexAndFace(x, y);
			if (obstacleIndexAndFace != null && obstacleIndexAndFace[0] >= 0 && obstacleIndexAndFace[1] >= 0) {
				setRotationEnabled(false);
				Obstacle obstacle = model.getObstacle(obstacleIndexAndFace[0]);
				if (obstacle instanceof CuboidObstacle) {
					CuboidObstacle obs = (CuboidObstacle) obstacle;
					switch (obstacleIndexAndFace[1]) {
					case 0x00: // front
					case 0x01: // rear
						clickPoint3D.set(viewer.findPointOnPlane('x', x, y, 0));
						originalCenter = obs.getCenter().z;
						originalCorner = obs.getCorner().z;
						break;
					case 0x02: // top
					case 0x03: // bottom
						clickPoint3D.set(viewer.findPointOnPlane('z', x, y, 0));
						originalCenter = obs.getCenter().y;
						originalCorner = obs.getCorner().y;
						break;
					case 0x04: // right
					case 0x05: // left
						clickPoint3D.set(viewer.findPointOnPlane('y', x, y, 0));
						originalCenter = obs.getCenter().x;
						originalCorner = obs.getCorner().x;
						break;
					}
				}
				else if (obstacle instanceof CylinderObstacle) {
					CylinderObstacle obs = (CylinderObstacle) obstacle;
					switch (obs.getAxis()) {
					case 'x':
						clickPoint3D.set(viewer.findPointOnPlane('y', x, y, 0));
						switch (obstacleIndexAndFace[1]) {
						case 0x00: // top
						case 0x01: // bottom
							originalCenter = obs.getCenter().x;
							originalCorner = 0.5f * obs.getHeight();
							break;
						case 0x02: // lateral
							originalCorner = obs.getRadius();
							break;
						}
						break;
					case 'y':
						clickPoint3D.set(viewer.findPointOnPlane('z', x, y, 0));
						switch (obstacleIndexAndFace[1]) {
						case 0x00: // top
						case 0x01: // bottom
							originalCenter = obs.getCenter().y;
							originalCorner = 0.5f * obs.getHeight();
							break;
						case 0x02: // lateral
							originalCorner = obs.getRadius();
							break;
						}
						break;
					case 'z':
						clickPoint3D.set(viewer.findPointOnPlane('x', x, y, 0));
						switch (obstacleIndexAndFace[1]) {
						case 0x00: // top
						case 0x01: // bottom
							originalCenter = obs.getCenter().z;
							originalCorner = 0.5f * obs.getHeight();
							break;
						case 0x02: // lateral
							originalCorner = obs.getRadius();
							break;
						}
						break;
					}
				}
			}
			else {
				setRotationEnabled(true);
			}
			break;
		case VVEL_ID:
			if (selectedComponent instanceof Atom) {
				Atom at = (Atom) selectedComponent;
				int i = at.getIndex();
				viewer.setIndexOfAtomOfSelectedVelocity(i);
				viewer.setAtomVelocities(i, at.getVx(), at.getVy(), at.getVz());
				clickPoint.setLocation(x, y);
				selectedFace = viewer.getVectorBoxFace(x, y);
				switch (selectedFace) {
				case 0x00: // front
				case 0x01: // rear
					clickPoint3D.set(viewer.findPointOnPlane('x', x, y, 0));
					originalValue = ((Atom) selectedComponent).getVz() * velocityScalingFactor;
					break;
				case 0x02: // top
				case 0x03: // bottom
					clickPoint3D.set(viewer.findPointOnPlane('z', x, y, 0));
					originalValue = ((Atom) selectedComponent).getVy() * velocityScalingFactor;
					break;
				case 0x04: // right
				case 0x05: // left
					clickPoint3D.set(viewer.findPointOnPlane('y', x, y, 0));
					originalValue = ((Atom) selectedComponent).getVx() * velocityScalingFactor;
					break;
				}
				setRotationEnabled(selectedFace < 0);
			}
			break;
		case XADD_ID:
			selectAtomOnDropPlane('x', x, y);
			break;
		case YADD_ID:
			selectAtomOnDropPlane('y', x, y);
			break;
		case ZADD_ID:
			selectAtomOnDropPlane('z', x, y);
			break;
		case XREC_ID:
			Point3f p = viewer.findPointOnDropPlane('x', x, y);
			shapeWithinBounds = Math.abs(2 * p.y) < model.getWidth() + 2 && Math.abs(2 * p.z) < model.getHeight() + 2;
			setRotationEnabled(!shapeWithinBounds);
			if (shapeWithinBounds) {
				if (Math.abs(2 * p.y) > model.getWidth())
					p.y = model.getWidth() * (p.y < 0 ? -0.5f : 0.5f);
				if (Math.abs(2 * p.z) > model.getHeight())
					p.z = model.getHeight() * (p.z < 0 ? -0.5f : 0.5f);
				clickPoint3D.set(p);
				dragPoint3D.set(p);
				viewer.setRectangleVisible(true);
			}
			break;
		case YREC_ID:
			p = viewer.findPointOnDropPlane('y', x, y);
			shapeWithinBounds = Math.abs(2 * p.x) < model.getLength() + 2 && Math.abs(2 * p.z) < model.getHeight() + 2;
			setRotationEnabled(!shapeWithinBounds);
			if (shapeWithinBounds) {
				if (Math.abs(2 * p.x) > model.getLength())
					p.x = model.getLength() * (p.x < 0 ? -0.5f : 0.5f);
				if (Math.abs(2 * p.z) > model.getHeight())
					p.z = model.getHeight() * (p.z < 0 ? -0.5f : 0.5f);
				clickPoint3D.set(p);
				dragPoint3D.set(p);
				viewer.setRectangleVisible(true);
			}
			break;
		case ZREC_ID:
			p = viewer.findPointOnDropPlane('z', x, y);
			shapeWithinBounds = Math.abs(2 * p.x) < model.getLength() + 2 && Math.abs(2 * p.y) < model.getWidth() + 2;
			setRotationEnabled(!shapeWithinBounds);
			if (shapeWithinBounds) {
				if (Math.abs(2 * p.x) > model.getLength())
					p.x = model.getLength() * (p.x < 0 ? -0.5f : 0.5f);
				if (Math.abs(2 * p.y) > model.getWidth())
					p.y = model.getWidth() * (p.y < 0 ? -0.5f : 0.5f);
				clickPoint3D.set(p);
				dragPoint3D.set(p);
				viewer.setRectangleVisible(true);
			}
			break;
		case XOVL_ID:
			p = viewer.findPointOnDropPlane('x', x, y);
			shapeWithinBounds = Math.abs(2 * p.y) < model.getWidth() + 2 && Math.abs(2 * p.z) < model.getHeight() + 2;
			setRotationEnabled(!shapeWithinBounds);
			if (shapeWithinBounds) {
				if (Math.abs(2 * p.y) > model.getWidth())
					p.y = model.getWidth() * (p.y < 0 ? -0.5f : 0.5f);
				if (Math.abs(2 * p.z) > model.getHeight())
					p.z = model.getHeight() * (p.z < 0 ? -0.5f : 0.5f);
				clickPoint3D.set(p);
				dragPoint3D.set(p);
				viewer.setEllipseVisible(true);
			}
			break;
		case YOVL_ID:
			p = viewer.findPointOnDropPlane('y', x, y);
			shapeWithinBounds = Math.abs(2 * p.x) < model.getLength() + 2 && Math.abs(2 * p.z) < model.getHeight() + 2;
			setRotationEnabled(!shapeWithinBounds);
			if (shapeWithinBounds) {
				if (Math.abs(2 * p.x) > model.getLength())
					p.x = model.getLength() * (p.x < 0 ? -0.5f : 0.5f);
				if (Math.abs(2 * p.z) > model.getHeight())
					p.z = model.getHeight() * (p.z < 0 ? -0.5f : 0.5f);
				clickPoint3D.set(p);
				dragPoint3D.set(p);
				viewer.setEllipseVisible(true);
			}
			break;
		case ZOVL_ID:
			p = viewer.findPointOnDropPlane('z', x, y);
			shapeWithinBounds = Math.abs(2 * p.x) < model.getLength() + 2 && Math.abs(2 * p.y) < model.getWidth() + 2;
			setRotationEnabled(!shapeWithinBounds);
			if (shapeWithinBounds) {
				if (Math.abs(2 * p.x) > model.getLength())
					p.x = model.getLength() * (p.x < 0 ? -0.5f : 0.5f);
				if (Math.abs(2 * p.y) > model.getWidth())
					p.y = model.getWidth() * (p.y < 0 ? -0.5f : 0.5f);
				clickPoint3D.set(p);
				dragPoint3D.set(p);
				viewer.setEllipseVisible(true);
			}
			break;
		case SLRT_ID:
		case SLOV_ID:
		case DELR_ID:
		case DELC_ID:
		case FIXR_ID:
		case FIXC_ID:
		case XIFR_ID:
		case XIFC_ID:
		case TSLC_ID:
		case CLST_ID:
		case HIDE_ID:
		case EDIH_ID:
			if (clickCount == 1)
				selectedArea.setLocation(x, y);
			break;
		case RBND_ID:
			if (e.isAltDown()) {
				if (selectedComponent instanceof Atom) {
					int i = viewer.findNearestAtomIndex(x, y);
					if (i >= 0) {
						Atom a1 = (Atom) selectedComponent;
						Atom a2 = model.getAtom(i);
						addRBond(a1, a2);
					}
				}
			}
			else {
				int i = viewer.findNearestAtomIndex(x, y);
				if (i >= 0) {
					selectAtom(-1);
					selectAtom(i);
				}
			}
			break;
		case ABND_ID:
			if (e.isAltDown()) {
				if (selectedComponent instanceof RBond) {
					int i = viewer.findNearestBondIndex(x, y);
					if (i >= 0) {
						RBond rbond1 = (RBond) selectedComponent;
						RBond rbond2 = model.getRBond(i);
						addABond(rbond1, rbond2);
					}
				}
			}
			else {
				int i = viewer.findNearestBondIndex(x, y);
				if (i >= 0)
					selectRBond(i);
			}
			break;
		case TBND_ID:
			if (e.isAltDown()) {
				if (selectedComponent instanceof ABond) {
					int i = viewer.findNearestABondIndex(x, y);
					if (i >= 0) {
						ABond abond1 = (ABond) selectedComponent;
						ABond abond2 = model.getABond(i);
						addTBond(abond1, abond2);
					}
				}
			}
			else {
				int i = viewer.findNearestABondIndex(x, y);
				if (i >= 0)
					selectABond(i);
			}
			break;
		}

		if (showEnergizer && energizer != null) {
			energizer.energize(x, y);
		}

		repaint();

	}

	private void selectAtomOnDropPlane(char direction, int x, int y) {
		int iselected = viewer.findNearestAtomIndexOnDropPlane(direction, x, y);
		if (iselected < 0) {
			selectAtom(-1);
			setRotationEnabled(true);
		}
		else {
			selectAtom(iselected);
			if (selectedComponent instanceof Atom) {
				setRotationEnabled(false);
				if (clickedAtomPosition == null)
					clickedAtomPosition = new Point3f();
				Atom at = (Atom) selectedComponent;
				clickedAtomPosition.x = at.getRx();
				clickedAtomPosition.y = at.getRy();
				clickedAtomPosition.z = at.getRz();
				Point3i p = viewer.getAtomScreen(at.getIndex());
				if (p != null)
					clickPoint.setLocation(x - p.x, y - p.y);
			}
		}
	}

	protected void processMouseDragged(MouseEvent e) {
		super.processMouseDragged(e);
		int x = e.getX();
		int y = e.getY();
		switch (actionID) {
		case DEFA_ID:
			model.setRotationMatrix(viewer.getRotationMatrix());
			break;
		case PANN_ID:
			int dx = x - dragPoint.x;
			int dy = y - dragPoint.y;
			dragPoint.setLocation(x, y);
			viewer.translateBy(dx, dy);
			break;
		case ROTA_ID:
			if (selectedComponent != null) {
				BitSet bs = getSelectionSet();
				if (bs.cardinality() > 0) {
					dx = x - dragPoint.x;
					dy = y - dragPoint.y;
					dragPoint.setLocation(x, y);
					model.rotateSelectedAtomsXYBy(viewer.getRotationMatrix(), bs, dx, dy);
					if (shouldRejectMoving(bs)) { // roll back rotation
						model.rotateSelectedAtomsXYBy(viewer.getRotationMatrix(), bs, -dx, -dy);
					}
					else {
						refresh();
						model.notifyChange();
					}
				}
			}
			break;
		case TRAN_ID:
			if (selectedComponent != null) {
				BitSet bs = getSelectionSet();
				if (bs.cardinality() > 0) {
					dx = x - dragPoint.x;
					dy = y - dragPoint.y;
					dragPoint.setLocation(x, y);
					model.translateSelectedAtomsXYBy(viewer.getRotationMatrix(), bs, dx, dy);
					if (shouldRejectMoving(bs)) { // roll back translation
						model.translateSelectedAtomsXYBy(viewer.getRotationMatrix(), bs, -dx, -dy);
					}
					else {
						refresh();
						model.notifyChange();
					}
				}
			}
			break;
		case DUPL_ID:
			if (selectedComponent != null) {
				BitSet bs = getSelectionSet();
				if (bs.cardinality() > 0 && selectedComponent instanceof Molecule) {
					if (newMolecule == null) {
						duplicateMolecule();
					}
					dx = x - dragPoint.x;
					dy = y - dragPoint.y;
					dragPoint.setLocation(x, y);
					model.translateSelectedAtomsXYBy(viewer.getRotationMatrix(), bs, dx, dy);
					model.notifyChange();
					refresh();
				}
			}
			break;
		case SLRT_ID:
		case SLOV_ID:
		case DELR_ID:
		case DELC_ID:
		case FIXR_ID:
		case FIXC_ID:
		case XIFR_ID:
		case XIFC_ID:
		case TSLC_ID:
		case CLST_ID:
		case HIDE_ID:
		case EDIH_ID:
			dragRect(x, y);
			break;
		case XADD_ID:
			if (selectedComponent instanceof Atom) {
				dragAtom((Atom) selectedComponent, 'x', x - clickPoint.x, y - clickPoint.y, false);
			}
			break;
		case YADD_ID:
			if (selectedComponent instanceof Atom) {
				dragAtom((Atom) selectedComponent, 'y', x - clickPoint.x, y - clickPoint.y, false);
			}
			break;
		case ZADD_ID:
			if (selectedComponent instanceof Atom) {
				dragAtom((Atom) selectedComponent, 'z', x - clickPoint.x, y - clickPoint.y, false);
			}
			break;
		case XREC_ID:
			if (shapeWithinBounds) {
				Point3f p = viewer.findPointOnDropPlane('x', x, y);
				p.y = Math.abs(2 * p.y) < model.getWidth() ? p.y : model.getWidth() * (p.y < 0 ? -0.5f : 0.5f);
				p.z = Math.abs(2 * p.z) < model.getHeight() ? p.z : model.getHeight() * (p.z < 0 ? -0.5f : 0.5f);
				dragPoint3D.set(p);
				viewer.setRectangle('x', clickPoint3D, dragPoint3D);
			}
			break;
		case YREC_ID:
			if (shapeWithinBounds) {
				Point3f p = viewer.findPointOnDropPlane('y', x, y);
				p.x = Math.abs(2 * p.x) < model.getLength() ? p.x : model.getLength() * (p.x < 0 ? -0.5f : 0.5f);
				p.z = Math.abs(2 * p.z) < model.getHeight() ? p.z : model.getHeight() * (p.z < 0 ? -0.5f : 0.5f);
				dragPoint3D.set(p);
				viewer.setRectangle('y', clickPoint3D, dragPoint3D);
			}
			break;
		case ZREC_ID:
			if (shapeWithinBounds) {
				Point3f p = viewer.findPointOnDropPlane('z', x, y);
				p.x = Math.abs(2 * p.x) < model.getLength() ? p.x : model.getLength() * (p.x < 0 ? -0.5f : 0.5f);
				p.y = Math.abs(2 * p.y) < model.getWidth() ? p.y : model.getWidth() * (p.y < 0 ? -0.5f : 0.5f);
				dragPoint3D.set(p);
				viewer.setRectangle('z', clickPoint3D, dragPoint3D);
			}
			break;
		case XOVL_ID:
			if (shapeWithinBounds) {
				Point3f p = viewer.findPointOnDropPlane('x', x, y);
				p.y = Math.abs(2 * p.y) < model.getWidth() ? p.y : model.getWidth() * (p.y < 0 ? -0.5f : 0.5f);
				p.z = Math.abs(2 * p.z) < model.getHeight() ? p.z : model.getHeight() * (p.z < 0 ? -0.5f : 0.5f);
				dragPoint3D.set(p);
				viewer.setEllipse('x', clickPoint3D, dragPoint3D);
			}
			break;
		case YOVL_ID:
			if (shapeWithinBounds) {
				Point3f p = viewer.findPointOnDropPlane('y', x, y);
				p.x = Math.abs(2 * p.x) < model.getLength() ? p.x : model.getLength() * (p.x < 0 ? -0.5f : 0.5f);
				p.z = Math.abs(2 * p.z) < model.getHeight() ? p.z : model.getHeight() * (p.z < 0 ? -0.5f : 0.5f);
				dragPoint3D.set(p);
				viewer.setEllipse('y', clickPoint3D, dragPoint3D);
			}
			break;
		case ZOVL_ID:
			if (shapeWithinBounds) {
				Point3f p = viewer.findPointOnDropPlane('z', x, y);
				p.x = Math.abs(2 * p.x) < model.getLength() ? p.x : model.getLength() * (p.x < 0 ? -0.5f : 0.5f);
				p.y = Math.abs(2 * p.y) < model.getWidth() ? p.y : model.getWidth() * (p.y < 0 ? -0.5f : 0.5f);
				dragPoint3D.set(p);
				viewer.setEllipse('z', clickPoint3D, dragPoint3D);
			}
			break;
		case SBOX_ID:
			if (selectedFace != -1) {
				moveSimulationBoxFace(selectedFace, x, y);
				model.notifyChange();
			}
			break;
		case EXOB_ID:
			moveObstacleFace(x, y, false);
			break;
		case VVEL_ID:
			if (selectedFace != -1) {
				moveVectorBoxFace(selectedFace, x, y);
			}
			break;
		}
		repaint();
	}

	protected void processMouseReleased(MouseEvent e) {
		super.processMouseReleased(e);
		int x = e.getX();
		int y = e.getY();
		int clickCount = e.getClickCount();
		BitSet bs = null;
		switch (actionID) {
		case SLRT_ID:
			if (Math.abs(selectedArea.width) > 5 && Math.abs(selectedArea.height) > 5) {
				bs = viewer.findAtomsInRectangle(selectedArea);
				viewer.setSelectionSet(bs);
				multiselectionBitSet.clear();
				multiselectionBitSet.or(bs);
				selectedArea.setSize(0, 0);
				if (bs.cardinality() == 0)
					selectedComponent = null;
			}
			break;
		case SLOV_ID:
			if (Math.abs(selectedArea.width) > 5 && Math.abs(selectedArea.height) > 5) {
				bs = viewer.findAtomsInOval(selectedArea);
				viewer.setSelectionSet(bs);
				multiselectionBitSet.clear();
				multiselectionBitSet.or(bs);
				selectedArea.setSize(0, 0);
				if (bs.cardinality() == 0)
					selectedComponent = null;
			}
			break;
		case DELR_ID:
			removeAtoms(viewer.findAtomsInRectangle(selectedArea));
			selectedArea.setSize(0, 0);
			model.notifyChange();
			break;
		case DELC_ID:
			removeAtoms(viewer.findAtomsInOval(selectedArea));
			selectedArea.setSize(0, 0);
			model.notifyChange();
			break;
		case FIXR_ID:
			bs = viewer.findAtomsInRectangle(selectedArea);
			if (bs.cardinality() > 0) {
				model.immobilizeAtoms(bs);
				model.notifyChange();
			}
			selectedArea.setSize(0, 0);
			break;
		case FIXC_ID:
			bs = viewer.findAtomsInOval(selectedArea);
			if (bs.cardinality() > 0) {
				model.immobilizeAtoms(bs);
				model.notifyChange();
			}
			selectedArea.setSize(0, 0);
			break;
		case XIFR_ID:
			bs = viewer.findAtomsInRectangle(selectedArea);
			if (bs.cardinality() > 0) {
				model.mobilizeAtoms(bs);
				model.notifyChange();
			}
			selectedArea.setSize(0, 0);
			break;
		case XIFC_ID:
			bs = viewer.findAtomsInOval(selectedArea);
			if (bs.cardinality() > 0) {
				model.mobilizeAtoms(bs);
				model.notifyChange();
			}
			selectedArea.setSize(0, 0);
			break;
		case TSLC_ID:
			bs = viewer.findAtomsInRectangle(selectedArea);
			if (bs.cardinality() > 0) {
				translucentBitSet.or(bs);
				hidenBitSet.andNot(bs);
				model.notifyChange();
			}
			selectedArea.setSize(0, 0);
			break;
		case CLST_ID:
			bs = viewer.findAtomsInRectangle(selectedArea);
			if (bs.cardinality() > 0) {
				translucentBitSet.andNot(bs);
				hidenBitSet.andNot(bs);
				model.notifyChange();
			}
			selectedArea.setSize(0, 0);
			break;
		case HIDE_ID:
			bs = viewer.findAtomsInRectangle(selectedArea);
			if (bs.cardinality() > 0) {
				hidenBitSet.or(bs);
				model.notifyChange();
			}
			selectedArea.setSize(0, 0);
			break;
		case EDIH_ID:
			bs = viewer.findAtomsInRectangle(selectedArea);
			if (bs.cardinality() > 0) {
				hidenBitSet.andNot(bs);
				model.notifyChange();
			}
			selectedArea.setSize(0, 0);
			break;
		case XADD_ID:
			if (clickCount >= 2) {
				addAtom(viewer.findPointOnDropPlane('x', x, y));
			}
			else {
				if (selectedComponent instanceof Atom) {
					dragAtom((Atom) selectedComponent, 'x', x - clickPoint.x, y - clickPoint.y, true);
				}
			}
			break;
		case YADD_ID:
			if (clickCount >= 2) {
				addAtom(viewer.findPointOnDropPlane('y', x, y));
			}
			else {
				if (selectedComponent instanceof Atom) {
					dragAtom((Atom) selectedComponent, 'y', x - clickPoint.x, y - clickPoint.y, true);
				}
			}
			break;
		case ZADD_ID:
			if (clickCount >= 2) {
				addAtom(viewer.findPointOnDropPlane('z', x, y));
			}
			else {
				if (selectedComponent instanceof Atom) {
					dragAtom((Atom) selectedComponent, 'z', x - clickPoint.x, y - clickPoint.y, true);
				}
			}
			break;
		case XMOL_ID:
			if (clickCount >= 2) {
				addMolecule(viewer.findPointOnDropPlane('x', x, y));
			}
			break;
		case YMOL_ID:
			if (clickCount >= 2) {
				addMolecule(viewer.findPointOnDropPlane('y', x, y));
			}
			break;
		case ZMOL_ID:
			if (clickCount >= 2) {
				addMolecule(viewer.findPointOnDropPlane('z', x, y));
			}
			break;
		case XFIL_ID:
			if (e.getClickCount() >= 2) {
				importModelPopupMenu.setPosition(viewer.findPointOnDropPlane('x', x, y));
				importModelPopupMenu.show(this, x, y);
			}
			break;
		case YFIL_ID:
			if (e.getClickCount() >= 2) {
				importModelPopupMenu.setPosition(viewer.findPointOnDropPlane('y', x, y));
				importModelPopupMenu.show(this, x, y);
			}
			break;
		case ZFIL_ID:
			if (e.getClickCount() >= 2) {
				importModelPopupMenu.setPosition(viewer.findPointOnDropPlane('z', x, y));
				importModelPopupMenu.show(this, x, y);
			}
			break;
		case XREC_ID:
			if (shapeWithinBounds) {
				float yc = (clickPoint3D.y + dragPoint3D.y) * 0.5f;
				float zc = (clickPoint3D.z + dragPoint3D.z) * 0.5f;
				float ly = Math.abs(clickPoint3D.y - dragPoint3D.y) * 0.5f;
				float lz = Math.abs(clickPoint3D.z - dragPoint3D.z) * 0.5f;
				if (ly >= Obstacle.MIN_THICKNESS && lz >= Obstacle.MIN_THICKNESS) {
					addCuboidObstacle(clickPoint3D.x, yc, zc, Obstacle.MIN_THICKNESS, ly, lz);
				}
				dragPoint3D.set(clickPoint3D);
				shapeWithinBounds = false;
			}
			break;
		case YREC_ID:
			if (shapeWithinBounds) {
				float xc = (clickPoint3D.x + dragPoint3D.x) * 0.5f;
				float zc = (clickPoint3D.z + dragPoint3D.z) * 0.5f;
				float lx = Math.abs(clickPoint3D.x - dragPoint3D.x) * 0.5f;
				float lz = Math.abs(clickPoint3D.z - dragPoint3D.z) * 0.5f;
				if (lx >= Obstacle.MIN_THICKNESS && lz >= Obstacle.MIN_THICKNESS) {
					addCuboidObstacle(xc, clickPoint3D.y, zc, lx, Obstacle.MIN_THICKNESS, lz);
				}
				dragPoint3D.set(clickPoint3D);
			}
			break;
		case ZREC_ID:
			if (shapeWithinBounds) {
				float xc = (clickPoint3D.x + dragPoint3D.x) * 0.5f;
				float yc = (clickPoint3D.y + dragPoint3D.y) * 0.5f;
				float lx = Math.abs(clickPoint3D.x - dragPoint3D.x) * 0.5f;
				float ly = Math.abs(clickPoint3D.y - dragPoint3D.y) * 0.5f;
				if (lx >= Obstacle.MIN_THICKNESS && ly >= Obstacle.MIN_THICKNESS) {
					addCuboidObstacle(xc, yc, clickPoint3D.z, lx, ly, Obstacle.MIN_THICKNESS);
				}
				dragPoint3D.set(clickPoint3D);
			}
			break;
		case XOVL_ID:
			if (shapeWithinBounds) {
				float yc = (clickPoint3D.y + dragPoint3D.y) * 0.5f;
				float zc = (clickPoint3D.z + dragPoint3D.z) * 0.5f;
				float ly = Math.abs(clickPoint3D.y - dragPoint3D.y) * 0.5f;
				float lz = Math.abs(clickPoint3D.z - dragPoint3D.z) * 0.5f;
				if (ly >= Obstacle.MIN_THICKNESS && lz >= Obstacle.MIN_THICKNESS) {
					addCylinderObstacle(clickPoint3D.x, yc, zc, 'x', Obstacle.MIN_THICKNESS, ly, lz);
				}
				dragPoint3D.set(clickPoint3D);
			}
			break;
		case YOVL_ID:
			if (shapeWithinBounds) {
				float xc = (clickPoint3D.x + dragPoint3D.x) * 0.5f;
				float zc = (clickPoint3D.z + dragPoint3D.z) * 0.5f;
				float lx = Math.abs(clickPoint3D.x - dragPoint3D.x) * 0.5f;
				float lz = Math.abs(clickPoint3D.z - dragPoint3D.z) * 0.5f;
				if (lx >= Obstacle.MIN_THICKNESS && lz >= Obstacle.MIN_THICKNESS) {
					addCylinderObstacle(xc, clickPoint3D.y, zc, 'y', Obstacle.MIN_THICKNESS, lx, lz);
				}
				dragPoint3D.set(clickPoint3D);
			}
			break;
		case ZOVL_ID:
			if (shapeWithinBounds) {
				float xc = (clickPoint3D.x + dragPoint3D.x) * 0.5f;
				float yc = (clickPoint3D.y + dragPoint3D.y) * 0.5f;
				float lx = Math.abs(clickPoint3D.x - dragPoint3D.x) * 0.5f;
				float ly = Math.abs(clickPoint3D.y - dragPoint3D.y) * 0.5f;
				if (lx >= Obstacle.MIN_THICKNESS && ly >= Obstacle.MIN_THICKNESS) {
					addCylinderObstacle(xc, yc, clickPoint3D.z, 'z', Obstacle.MIN_THICKNESS, lx, ly);
				}
				dragPoint3D.set(clickPoint3D);
			}
			break;
		case EXOB_ID:
			moveObstacleFace(x, y, true);
			model.notifyChange();
			break;
		case DUPL_ID:
			if (newMolecule != null) {
				bs = getSelectionSet();
				if (shouldRejectMoving(bs)) { // roll back duplication
					removeAtoms(bs);
				}
				else {
					refresh();
				}
				newMolecule = null;
			}
			break;
		}
		if (showEnergizer && energizer != null) {
			energizer.buttonPressed = false;
		}
		if (navigator != null)
			navigator.clear();
		repaint();
	}

	private void addCuboidObstacle(float xc, float yc, float zc, float lx, float ly, float lz) {
		if (model.atomsOverlapCuboid(xc, yc, zc, lx, ly, lz)) {
			errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
			viewer.setRectangleVisible(false);
			return;
		}
		viewer.addCuboidObstacle();
		model.addObstacle(new CuboidObstacle(xc, yc, zc, lx, ly, lz));
		model.notifyChange();
	}

	private void addCylinderObstacle(float xc, float yc, float zc, char axis, float h, float a, float b) {
		float r = (float) Math.sqrt(a * a + b * b);
		if (model.atomsOverlapCylinder(xc, yc, zc, axis, h, r)) {
			errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
			viewer.setEllipseVisible(false);
			return;
		}
		viewer.addCylinderObstacle();
		model.addObstacle(new CylinderObstacle(xc, yc, zc, axis, h, r));
		model.notifyChange();
	}

	private void removeObstacle(short index) {
		if (obstacleIndexAndFace != null) {
			obstacleIndexAndFace[0] = obstacleIndexAndFace[1] = -1;
		}
		if (index < 0 || index >= model.getObstacleCount())
			return;
		model.removeObstacle(index);
		viewer.removeObstacle(index);
	}

	// FIXME: There is a problem when the dragged point is near the edge of the face
	private void moveSimulationBoxFace(byte face, int x, int y) {
		if (face < 0)
			return;
		switch (face) {
		case 0x00: // front
		case 0x01: // rear
			Point3f p = viewer.findPointOnPlane('x', x, y, 0);
			int delta = (int) (2 * (p.z - clickPoint3D.z));
			float h = clickPoint3D.z < 0 ? originalValue - delta : originalValue + delta;
			if (h < 2 * absMax)
				h = 2 * absMax;
			model.setHeight((int) (h + 1));
			setSimulationBox();
			break;
		case 0x02: // top
		case 0x03: // bottom
			p = viewer.findPointOnPlane('z', x, y, 0);
			delta = (int) (2 * (p.y - clickPoint3D.y));
			float w = clickPoint3D.y < 0 ? originalValue - delta : originalValue + delta;
			if (w < 2 * absMax)
				w = 2 * absMax;
			model.setWidth((int) (w + 1));
			setSimulationBox();
			break;
		case 0x04: // right
		case 0x05: // left
			p = viewer.findPointOnPlane('y', x, y, 0);
			delta = (int) (2 * (p.x - clickPoint3D.x));
			float l = clickPoint3D.x < 0 ? originalValue - delta : originalValue + delta;
			if (l < 2 * absMax)
				l = 2 * absMax;
			model.setLength((int) (l + 1));
			setSimulationBox();
			break;
		}
		viewer.updateSimulationBoxFace(face);
	}

	private void checkObstacleBounds(boolean positiveSide, float delta, float half) {
		positionCenter = originalCenter + delta;
		if (positiveSide) {
			positionCorner = Math.abs(originalCorner + delta);
			delta = 0.5f * (positionCenter + positionCorner - half);
			if (delta > 0) {
				positionCenter -= delta;
				positionCorner -= delta;
			}
			else {
				delta = 0.5f * (positionCenter - positionCorner + half);
				if (delta < 0) {
					positionCenter -= delta;
					positionCorner += delta;
				}
				if (positionCenter < -half) {
					positionCorner = 0.5f * Obstacle.MIN_THICKNESS;
					positionCenter = positionCorner - half;
				}
			}
		}
		else {
			positionCorner = Math.abs(originalCorner - delta);
			// a negative delta means enlargement in the negative zone
			delta = 0.5f * (positionCenter - positionCorner + half);
			if (delta < 0) {
				positionCenter -= delta;
				positionCorner += delta;
			}
			else {
				delta = 0.5f * (positionCenter + positionCorner - half);
				if (delta > 0) {
					positionCenter -= delta;
					positionCorner -= delta;
				}
				if (positionCenter > half) {
					positionCorner = 0.5f * Obstacle.MIN_THICKNESS;
					positionCenter = half - positionCorner;
				}
			}
		}
	}

	private void checkCylindericObstacleBounds(char axis, float delta, Point3f center) {
		positionCorner = originalCorner + delta; // new radius
		if (positionCorner < CylinderObstacle.MIN_RADIUS) {
			positionCorner = CylinderObstacle.MIN_RADIUS;
		}
		else {
			float min = Float.MAX_VALUE;
			float dis;
			switch (axis) {
			case 'x':
				dis = 0.5f * model.getWidth() - center.y;
				if (dis < min)
					min = dis;
				dis = center.y + 0.5f * model.getWidth();
				if (dis < min)
					min = dis;
				dis = 0.5f * model.getHeight() - center.z;
				if (dis < min)
					min = dis;
				dis = center.z + 0.5f * model.getHeight();
				if (dis < min)
					min = dis;
				break;
			case 'y':
				dis = 0.5f * model.getLength() - center.x;
				if (dis < min)
					min = dis;
				dis = center.x + 0.5f * model.getLength();
				if (dis < min)
					min = dis;
				dis = 0.5f * model.getHeight() - center.z;
				if (dis < min)
					min = dis;
				dis = center.z + 0.5f * model.getHeight();
				if (dis < min)
					min = dis;
				break;
			case 'z':
				dis = 0.5f * model.getLength() - center.x;
				if (dis < min)
					min = dis;
				dis = center.x + 0.5f * model.getLength();
				if (dis < min)
					min = dis;
				dis = 0.5f * model.getWidth() - center.y;
				if (dis < min)
					min = dis;
				dis = center.y + 0.5f * model.getWidth();
				if (dis < min)
					min = dis;
				break;
			}
			if (positionCorner > min)
				positionCorner = min;
		}
	}

	// the releasing flag is used to avoid overlap calculations while dragging
	private void moveObstacleFace(int x, int y, boolean releasing) {
		if (obstacleIndexAndFace == null || obstacleIndexAndFace[0] < 0 || obstacleIndexAndFace[1] < 0)
			return;
		Obstacle o = model.getObstacle(obstacleIndexAndFace[0]);
		if (o instanceof CuboidObstacle) {
			CuboidObstacle obs = (CuboidObstacle) o;
			byte face = (byte) obstacleIndexAndFace[1];
			switch (face) {
			case 0x00: // front
			case 0x01: // rear
				Point3f p = viewer.findPointOnPlane('x', x, y, 0);
				checkObstacleBounds(face % 2 == 0, p.z - clickPoint3D.z, 0.5f * model.getHeight());
				if (releasing) {
					if (model.atomsOverlapCuboid(obs.getCenter().x, obs.getCenter().y, positionCenter,
							obs.getCorner().x, obs.getCorner().y, positionCorner)) {
						errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
						viewer.updateCuboidObstacleFace('z', obs.getCenter().z, obs.getCorner().z);
						return;
					}
					obs.getCenter().z = positionCenter;
					obs.getCorner().z = positionCorner;
				}
				viewer.updateCuboidObstacleFace('z', positionCenter, positionCorner);
				break;
			case 0x02: // top
			case 0x03: // bottom
				p = viewer.findPointOnPlane('z', x, y, 0);
				checkObstacleBounds(face % 2 == 0, p.y - clickPoint3D.y, 0.5f * model.getWidth());
				if (releasing) {
					if (model.atomsOverlapCuboid(obs.getCenter().x, positionCenter, obs.getCenter().z,
							obs.getCorner().x, positionCorner, obs.getCorner().z)) {
						errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
						viewer.updateCuboidObstacleFace('y', obs.getCenter().y, obs.getCorner().y);
						return;
					}
					obs.getCenter().y = positionCenter;
					obs.getCorner().y = positionCorner;
				}
				viewer.updateCuboidObstacleFace('y', positionCenter, positionCorner);
				break;
			case 0x04: // right
			case 0x05: // left
				p = viewer.findPointOnPlane('y', x, y, 0);
				checkObstacleBounds(face % 2 == 0, p.x - clickPoint3D.x, 0.5f * model.getLength());
				if (releasing) {
					if (model.atomsOverlapCuboid(positionCenter, obs.getCenter().y, obs.getCenter().z, positionCorner,
							obs.getCorner().y, obs.getCorner().z)) {
						errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
						viewer.updateCuboidObstacleFace('x', obs.getCenter().x, obs.getCorner().x);
						return;
					}
					obs.getCenter().x = positionCenter;
					obs.getCorner().x = positionCorner;
				}
				viewer.updateCuboidObstacleFace('x', positionCenter, positionCorner);
				break;
			}
		}
		else if (o instanceof CylinderObstacle) {
			CylinderObstacle obs = (CylinderObstacle) o;
			float a = obs.getRadius() * COS45;
			byte face = (byte) obstacleIndexAndFace[1];
			switch (obs.getAxis()) {
			case 'x':
				Point3f p = viewer.findPointOnPlane('y', x, y, 0);
				if (face != 2) {// top and bottom
					checkObstacleBounds(face % 2 == 0, p.x - clickPoint3D.x, 0.5f * model.getLength());
					if (releasing) {
						if (model.atomsOverlapCylinder(positionCenter, obs.getCenter().y, obs.getCenter().z, obs
								.getAxis(), 2 * positionCorner, obs.getRadius())) {
							errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
							viewer.updateCylinderObstacleFace('x', obs.getCenter().x, a, a, obs.getHeight());
							return;
						}
						obs.getCenter().x = positionCenter;
						obs.setHeight(2 * positionCorner);
					}
					viewer.updateCylinderObstacleFace('x', positionCenter, a, a, 2 * positionCorner);
				}
				else { // lateral
					checkCylindericObstacleBounds('x', p.z - clickPoint3D.z, obs.getCenter());
					if (releasing) {
						if (model.atomsOverlapCylinder(obs.getCenter().x, obs.getCenter().y, obs.getCenter().z, obs
								.getAxis(), obs.getHeight(), positionCorner)) {
							errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
							viewer.updateCylinderObstacleFace('x', obs.getCenter().x, a, a, obs.getHeight());
							return;
						}
						obs.setRadius(positionCorner);
					}
					a = positionCorner * COS45;
					viewer.updateCylinderObstacleFace('x', obs.getCenter().x, a, a, obs.getHeight());
				}
				break;
			case 'y':
				p = viewer.findPointOnPlane('z', x, y, 0);
				if (face != 2) {// top and bottom
					checkObstacleBounds(face % 2 == 0, p.y - clickPoint3D.y, 0.5f * model.getWidth());
					if (releasing) {
						if (model.atomsOverlapCylinder(obs.getCenter().x, positionCenter, obs.getCenter().z, obs
								.getAxis(), 2 * positionCorner, obs.getRadius())) {
							errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
							viewer.updateCylinderObstacleFace('y', obs.getCenter().y, a, a, obs.getHeight());
							return;
						}
						obs.getCenter().y = positionCenter;
						obs.setHeight(2 * positionCorner);
					}
					viewer.updateCylinderObstacleFace('y', positionCenter, a, a, 2 * positionCorner);
				}
				else { // lateral
					checkCylindericObstacleBounds('y', p.x - clickPoint3D.x, obs.getCenter());
					if (releasing) {
						if (model.atomsOverlapCylinder(obs.getCenter().x, obs.getCenter().y, obs.getCenter().z, obs
								.getAxis(), obs.getHeight(), positionCorner)) {
							errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
							viewer.updateCylinderObstacleFace('y', obs.getCenter().y, a, a, obs.getHeight());
							return;
						}
						obs.setRadius(positionCorner);
					}
					a = positionCorner * COS45;
					viewer.updateCylinderObstacleFace('y', obs.getCenter().y, a, a, obs.getHeight());
				}
				break;
			case 'z':
				p = viewer.findPointOnPlane('x', x, y, 0);
				if (face != 2) {// top and bottom
					checkObstacleBounds(face % 2 == 0, p.z - clickPoint3D.z, 0.5f * model.getHeight());
					if (releasing) {
						if (model.atomsOverlapCylinder(obs.getCenter().x, obs.getCenter().y, positionCenter, obs
								.getAxis(), 2 * positionCorner, obs.getRadius())) {
							errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
							viewer.updateCylinderObstacleFace('z', obs.getCenter().z, a, a, obs.getHeight());
							return;
						}
						obs.getCenter().z = positionCenter;
						obs.setHeight(2 * positionCorner);
					}
					viewer.updateCylinderObstacleFace('z', positionCenter, a, a, 2 * positionCorner);
				}
				else {
					checkCylindericObstacleBounds('z', p.y - clickPoint3D.y, obs.getCenter());
					if (releasing) {
						if (model.atomsOverlapCylinder(obs.getCenter().x, obs.getCenter().y, obs.getCenter().z, obs
								.getAxis(), obs.getHeight(), positionCorner)) {
							errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
							viewer.updateCylinderObstacleFace('z', obs.getCenter().z, a, a, obs.getHeight());
							return;
						}
						obs.setRadius(positionCorner);
					}
					a = positionCorner * COS45;
					viewer.updateCylinderObstacleFace('z', obs.getCenter().z, a, a, obs.getHeight());
				}
				break;
			}
		}
	}

	private void moveVectorBoxFace(byte face, int x, int y) {
		if (face < 0)
			return;
		if (!(selectedComponent instanceof Atom))
			return;
		Atom at = (Atom) selectedComponent;
		switch (face) {
		case 0x00: // front
		case 0x01: // rear
			Point3f p = viewer.findPointOnPlane('x', x, y, 0);
			int delta = (int) (2 * (p.z - clickPoint3D.z));
			at.setVz((originalValue + delta) / velocityScalingFactor);
			break;
		case 0x02: // top
		case 0x03: // bottom
			p = viewer.findPointOnPlane('z', x, y, 0);
			delta = (int) (2 * (p.y - clickPoint3D.y));
			at.setVy((originalValue + delta) / velocityScalingFactor);
			break;
		case 0x04: // right
		case 0x05: // left
			p = viewer.findPointOnPlane('y', x, y, 0);
			delta = (int) (2 * (p.x - clickPoint3D.x));
			at.setVx((originalValue + delta) / velocityScalingFactor);
			break;
		}
		viewer.setAtomVelocities(at.getIndex(), at.getVx(), at.getVy(), at.getVz());
		viewer.updateVectorBoxFace(face);
	}

	private void dragAtom(Atom a, char direction, int x, int y, boolean releasing) {
		if (a == null)
			return;
		int k = model.getAtomIndex(a);
		if (k == -1)
			return;
		float c = 0;
		switch (direction) {
		case 'x':
			c = a.getRx();
			break;
		case 'y':
			c = a.getRy();
			break;
		case 'z':
			c = a.getRz();
			break;
		}
		Point3f p = viewer.findPointOnPlane(direction, x, y, c);
		if (!model.contains(p))
			return;
		if (releasing) {
			int n = model.getAtomCount();
			boolean tooClose = false;
			for (int i = 0; i < n; i++) {
				if (i == k)
					continue;
				if (model.getAtom(i).isTooClose(p)) {
					tooClose = true;
					break;
				}
			}
			n = model.getObstacleCount();
			if (n > 0) {
				for (int i = 0; i < n; i++) {
					if (model.getObstacle(i).contains(a)) {
						tooClose = true;
						break;
					}
				}
			}
			if (tooClose) {
				errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
				a.setLocation(clickedAtomPosition);
				viewer.setAtomCoordinates(k, clickedAtomPosition);
				repaint();
				return;
			}
		}
		switch (direction) {
		case 'x':
			a.setRy(p.y);
			a.setRz(p.z);
			viewer.setAtomCoordinates(k, c, p.y, p.z);
			break;
		case 'y':
			a.setRx(p.x);
			a.setRz(p.z);
			viewer.setAtomCoordinates(k, p.x, c, p.z);
			break;
		case 'z':
			a.setRx(p.x);
			a.setRy(p.y);
			viewer.setAtomCoordinates(k, p.x, p.y, c);
			break;
		}
		refresh();
		repaint();
		if (releasing)
			model.notifyChange();
	}

	void setCurrentElementToAdd(String s) {
		currentElementToAdd = s;
	}

	String getCurrentElementToAdd() {
		return currentElementToAdd;
	}

	void setCurrentMoleculeToAdd(int i) {
		currentMoleculeToAdd = i;
	}

	int getCurrentMoleculeToAdd() {
		return currentMoleculeToAdd;
	}

	private void removeAtoms(BitSet bs) {
		if (bs.cardinality() <= 0)
			return;
		handleBitSet(translucentBitSet, sharedBitSet, bs);
		handleBitSet(trajectoryBitSet, sharedBitSet, bs);
		handleBitSet(velocityBitSet, sharedBitSet, bs);
		model.removeAtoms(bs);
		viewer.removeAtoms(bs);
		int n = model.getABondCount();
		if (n > 0) {
			viewer.clearABonds();
			ABond abond;
			for (int i = 0; i < n; i++) {
				abond = model.getABond(i);
				viewer.addABond(abond.getAtom1().getIndex(), abond.getAtom2().getIndex(), abond.getAtom3().getIndex());
			}
		}
		n = model.getTBondCount();
		if (n > 0) {
			viewer.clearTBonds();
			TBond tbond;
			for (int i = 0; i < n; i++) {
				tbond = model.getTBond(i);
				viewer.addTBond(tbond.getAtom1().getIndex(), tbond.getAtom2().getIndex(), tbond.getAtom3().getIndex(),
						tbond.getAtom4().getIndex());
			}
		}
		storeCurrentOrientation();
		renderModel(false);
		refresh();
		refreshTrajectories();
		repaint();
	}

	private static void handleBitSet(BitSet orig, BitSet copy, BitSet removed) {
		if (orig.cardinality() <= 0)
			return;
		if (copy == null) {
			copy = new BitSet(orig.size());
		}
		else {
			copy.clear();
		}
		int n = orig.length();
		int m = 0;
		for (int i = 0; i < n; i++) {
			if (removed.get(i))
				continue;
			copy.set(m++, orig.get(i));
		}
		copyBitSet(copy, orig);
	}

	private static void copyBitSet(BitSet src, BitSet des) {
		des.clear();
		int n = Math.min(src.length(), des.size());
		for (int i = 0; i < n; i++) {
			des.set(i, src.get(i));
		}
	}

	public BitSet getSelectionSet() {
		return viewer.getSelectionSet();
	}

	public void invertSelection() {
		int n = model.getAtomCount();
		BitSet bs = viewer.getSelectionSet();
		bs.flip(0, n);
		repaint();
	}

	void setCharge(int index, float charge) {
		if (index < 0 || index >= model.getAtomCount())
			return;
		model.getAtom(index).setCharge(charge);
		viewer.setCharge(index, charge);
		repaint();
	}

	private void duplicateMolecule() {
		if (!(selectedComponent instanceof Molecule))
			return;
		if (model.getAtomCount() + ((Molecule) selectedComponent).getAtomCount() > MolecularModel.SIZE) {
			errorReminder.show(ErrorReminder.EXCEED_CAPACITY);
			return;
		}
		int nRbond0 = model.getRBondCount();
		int nAbond0 = model.getABondCount();
		int nTbond0 = model.getTBondCount();
		newMolecule = ((Molecule) selectedComponent).duplicate(model);
		Atom a;
		for (int i = 0; i < newMolecule.getAtomCount(); i++) {
			a = newMolecule.getAtom(i);
			viewer.addAtom(a, a.getElementNumber(), a.getSymbol(), 0, a.getCharge(), a.getRx(), a.getRy(), a.getRz(),
					0, 0, 0, a);
		}
		int nRbond1 = model.getRBondCount();
		if (nRbond1 > nRbond0) {
			RBond rbond;
			for (int i = nRbond0; i < nRbond1; i++) {
				rbond = model.getRBond(i);
				viewer.addRBond(rbond.getAtom1(), rbond.getAtom2());
			}
		}
		int nAbond1 = model.getABondCount();
		if (nAbond1 > nAbond0) {
			ABond abond;
			for (int i = nAbond0; i < nAbond1; i++) {
				abond = model.getABond(i);
				viewer.addABond(abond.getAtom1().getIndex(), abond.getAtom2().getIndex(), abond.getAtom3().getIndex());
			}
		}
		int nTbond1 = model.getTBondCount();
		if (nTbond1 > nTbond0) {
			TBond tbond;
			for (int i = nTbond0; i < nTbond1; i++) {
				tbond = model.getTBond(i);
				viewer.addTBond(tbond.getAtom1().getIndex(), tbond.getAtom2().getIndex(), tbond.getAtom3().getIndex(),
						tbond.getAtom4().getIndex());
			}
		}
		viewer.setTainted(true);
	}

	private boolean addMolecule(Point3f p) {
		if (!model.contains(p))
			return false;
		int n0 = model.getAtomCount();
		int nRbond0 = model.getRBondCount();
		int nAbond0 = model.getABondCount();
		int nTbond0 = model.getTBondCount();
		if (!model.importMolecule(currentMoleculeToAdd, p)) {
			errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
			return false;
		}
		int n1 = model.getAtomCount();
		Atom a;
		for (int i = n0; i < n1; i++) {
			a = model.getAtom(i);
			viewer.addAtom(a, a.getElementNumber(), a.getSymbol(), 0, a.getCharge(), a.getRx(), a.getRy(), a.getRz(),
					0, 0, 0, a);
		}
		int nRbond1 = model.getRBondCount();
		if (nRbond1 > nRbond0) {
			RBond rbond;
			for (int i = nRbond0; i < nRbond1; i++) {
				rbond = model.getRBond(i);
				viewer.addRBond(rbond.getAtom1(), rbond.getAtom2());
			}
		}
		int nAbond1 = model.getABondCount();
		if (nAbond1 > nAbond0) {
			ABond abond;
			for (int i = nAbond0; i < nAbond1; i++) {
				abond = model.getABond(i);
				viewer.addABond(abond.getAtom1().getIndex(), abond.getAtom2().getIndex(), abond.getAtom3().getIndex());
			}
		}
		int nTbond1 = model.getTBondCount();
		if (nTbond1 > nTbond0) {
			TBond tbond;
			for (int i = nTbond0; i < nTbond1; i++) {
				tbond = model.getTBond(i);
				viewer.addTBond(tbond.getAtom1().getIndex(), tbond.getAtom2().getIndex(), tbond.getAtom3().getIndex(),
						tbond.getAtom4().getIndex());
			}
		}
		if (n0 == 0) {
			storeCurrentOrientation();
			renderModel(false);
		}
		else {
			viewer.setTainted(true);
		}
		model.notifyChange();
		return true;
	}

	private boolean addAtom(Point3f p) {
		if (!model.contains(p))
			return false;
		int n = model.getAtomCount();
		for (int i = 0; i < n; i++) {
			if (model.getAtom(i).isTooClose(p)) {
				errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
				return false;
			}
		}
		n = model.getObstacleCount();
		if (n > 0) {
			for (int i = 0; i < n; i++) {
				if (model.getObstacle(i).contains(p)) {
					errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
					return false;
				}
			}
		}
		if (!model.addAtom(currentElementToAdd, p.x, p.y, p.z, 0, 0, 0, 0))
			return false;
		int count = model.getAtomCount();
		Atom a = model.getAtom(count - 1);
		Byte id = nameIdMap.get(currentElementToAdd);
		if (id == null)
			return false;
		viewer.addAtom(a, id.byteValue(), a.getSymbol(), 0, a.getCharge(), a.getRx(), a.getRy(), a.getRz(), 0, 0, 0, a);
		int currentIndex = count - 1;
		viewer.setAtomSize(currentIndex, model.getElementSigma(currentElementToAdd) * 1000);
		viewer.setAtomColor(currentIndex, getElementArgb(currentElementToAdd));
		if (fullSizeUnbondedAtoms)
			viewer.setCpkPercent(currentIndex, 100);
		if (count <= 1) {
			storeCurrentOrientation();
			renderModel(false);
		}
		else {
			viewer.setTainted(true);
		}
		model.notifyChange();
		return true;
	}

	private void removeRBond(RBond rbond) {
		viewer.removeRBond(model.getRBonds().indexOf(rbond));
		model.removeRBond(rbond);
		if (selectedComponent == rbond) {
			viewer.setHighlightCylinderVisible(false);
			selectedComponent = null;
		}
		model.formMolecules();
		model.notifyChange();
	}

	private void addRBond(Atom a1, Atom a2) {
		RBond rbond = new RBond(a1, a2);
		rbond.setLength(a1.distance(a2));
		model.addRBond(rbond);
		viewer.addRBond(a1, a2);
		model.formMolecules();
		model.notifyChange();
	}

	private void removeABond(ABond abond) {
		viewer.removeABond(model.getABonds().indexOf(abond));
		model.removeABond(abond);
		if (selectedComponent == abond) {
			viewer.setHighlightTriangleVisible(false);
			selectedComponent = null;
		}
		model.notifyChange();
	}

	private void addABond(RBond r1, RBond r2) {
		if (r1.equals(r2))
			return;
		ABond abond = new ABond(r1, r2); // angle already set in the constructor
		model.addABond(abond);
		viewer.addABond(abond.getAtom1().getIndex(), abond.getAtom2().getIndex(), abond.getAtom3().getIndex());
		Point3f p1 = new Point3f(abond.getAtom1().getRx(), abond.getAtom1().getRy(), abond.getAtom1().getRz());
		Point3f p2 = new Point3f(abond.getAtom2().getRx(), abond.getAtom2().getRy(), abond.getAtom2().getRz());
		Point3f p3 = new Point3f(abond.getAtom3().getRx(), abond.getAtom3().getRy(), abond.getAtom3().getRz());
		viewer.setHighlightTriangleVertices(p1, p2, p3);
		viewer.setHighlightTriangleVisible(true);
		model.notifyChange();
	}

	private void removeTBond(TBond tbond) {
		viewer.removeTBond(model.getTBonds().indexOf(tbond));
		model.removeTBond(tbond);
		if (selectedComponent == tbond) {
			viewer.setHighlightTBondVisible(false);
			selectedComponent = null;
		}
		model.notifyChange();
	}

	private void addTBond(ABond a1, ABond a2) {
		if (a1.equals(a2))
			return;
		if (a1.getAtom2() == a2.getAtom2())
			return;
		Atom[] at = ABond.getSharedAtom(a1, a2);
		if (at == null || at[0] == null || at[1] == null)
			return;
		TBond tbond = new TBond(a1, a2); // angle already set in the constructor
		if (model.addTBond(tbond)) {
			int i = tbond.getAtom1().getIndex();
			int j = tbond.getAtom2().getIndex();
			int k = tbond.getAtom3().getIndex();
			int l = tbond.getAtom4().getIndex();
			viewer.addTBond(i, j, k, l);
			viewer.setHighlightTBond(i, j, k, l);
			viewer.setHighlightTBondVisible(true);
		}
		model.notifyChange();
	}

	protected void processMouseMoved(MouseEvent e) {
		// WHY? if (!hasFocus()) return;
		super.processMouseMoved(e);
		int x = e.getX();
		int y = e.getY();
		if (viewer.getNavigationMode()) {
			if (navigator.mouseHover(x, y)) {
				repaint();
				return;
			}
		}
		switch (actionID) {
		case SBOX_ID:
			viewer.getSimulationBoxFace(x, y);
			break;
		case EXOB_ID:
			obstacleIndexAndFace = viewer.findNearestObstacleIndexAndFace(x, y);
			break;
		case VVEL_ID:
			int i = viewer.findNearestAtomIndex(x, y);
			if (i >= 0) {
				selectAtom(i);
				Atom at = (Atom) selectedComponent;
				viewer.setAtomVelocities(i, at.getVx(), at.getVy(), at.getVz());
			}
			if (selectedComponent instanceof Atom) {
				viewer.setIndexOfAtomOfSelectedVelocity(i);
				setCursor(viewer.getVectorBoxFace(x, y) >= 0 ? UserAction.getExtrusionCursor() : UserAction
						.getCursor(actionID));
			}
			break;
		case DEFA_ID:
			if (showEnergizer) {
				if (energizer != null) {
					if (x >= energizer.x) {
						energizer.mouseEntered(x, y);
						energizer.paint(getGraphics());
					}
					else {
						energizer.mouseExited();
						energizer.paint(getGraphics());
					}
				}
			}
			break;
		}
		repaint();
	}

	private void translateDropPlane() {
		float dx = 0;
		float dy = 0;
		float dz = 0;
		switch (actionID) {
		case XADD_ID:
		case XMOL_ID:
		case XFIL_ID:
		case XREC_ID:
		case XOVL_ID:
			dx = 0.5f;
			break;
		case YADD_ID:
		case YMOL_ID:
		case YFIL_ID:
		case YREC_ID:
		case YOVL_ID:
			dy = 0.5f;
			break;
		case ZADD_ID:
		case ZMOL_ID:
		case ZFIL_ID:
		case ZREC_ID:
		case ZOVL_ID:
			dz = 0.5f;
			break;
		}
		if ((keyCode & Navigator.UP_PRESSED) == Navigator.UP_PRESSED
				|| (keyCode & Navigator.RIGHT_PRESSED) == Navigator.RIGHT_PRESSED) {
			viewer.translateDropPlane(dx, dy, dz);
		}
		else if ((keyCode & Navigator.DOWN_PRESSED) == Navigator.DOWN_PRESSED
				|| (keyCode & Navigator.LEFT_PRESSED) == Navigator.LEFT_PRESSED) {
			viewer.translateDropPlane(-dx, -dy, -dz);
		}
		repaint();
	}

	private void translateAtom(Atom a, boolean mx, boolean my, boolean mz, boolean checkBounds, float t) {
		if (a == null)
			return;
		if (mx) {
			if (checkBounds && Math.abs(a.getRx() + t) >= (model.getLength() - a.getSigma()) * 0.5f)
				return;
			a.translate(t, 0, 0);
		}
		if (my) {
			if (checkBounds && Math.abs(a.getRy() + t) >= (model.getWidth() - a.getSigma()) * 0.5f)
				return;
			a.translate(0, t, 0);
		}
		if (mz) {
			if (checkBounds && Math.abs(a.getRz() + t) >= (model.getHeight() - a.getSigma()) * 0.5f)
				return;
			a.translate(0, 0, t);
		}
	}

	private void translateAtoms(BitSet bs) {
		int n = model.getAtomCount();
		if (n <= 0)
			return;
		boolean pgUp = (keyCode & Navigator.PGUP_PRESSED) == Navigator.PGUP_PRESSED;
		boolean pgDn = (keyCode & Navigator.PGDN_PRESSED) == Navigator.PGDN_PRESSED;
		boolean up = pgUp ? true : (keyCode & Navigator.UP_PRESSED) == Navigator.UP_PRESSED;
		boolean dn = pgDn ? true : (keyCode & Navigator.DOWN_PRESSED) == Navigator.DOWN_PRESSED;
		if (!up)
			up = (keyCode & Navigator.RIGHT_PRESSED) == Navigator.RIGHT_PRESSED;
		if (!dn)
			dn = (keyCode & Navigator.LEFT_PRESSED) == Navigator.LEFT_PRESSED;
		boolean mx = (keyCode & Navigator.X_PRESSED) == Navigator.X_PRESSED;
		boolean my = (keyCode & Navigator.Y_PRESSED) == Navigator.Y_PRESSED;
		boolean mz = (keyCode & Navigator.Z_PRESSED) == Navigator.Z_PRESSED;
		float t = KeyboardParameterManager.sharedInstance().getAtomDisplacement(
				pgUp || pgDn ? KeyboardParameterManager.FAST : KeyboardParameterManager.SLOW);
		if (!up)
			t = -t;
		if (bs.cardinality() > 1) {
			for (int i = 0; i < n; i++) {
				if (bs.get(i)) {
					translateAtom(model.getAtom(i), mx, my, mz, false, t);
				}
			}
			if (shouldRejectMoving(bs)) { // roll back the translation
				for (int i = 0; i < n; i++) {
					if (bs.get(i)) {
						translateAtom(model.getAtom(i), mx, my, mz, false, -t);
					}
				}
			}
		}
		else {
			int i = bs.nextSetBit(0);
			if (i >= 0) {
				translateAtom(model.getAtom(i), mx, my, mz, true, t);
				if (model.getMinimumDistance(bs) < 0 || model.overlapWithObstacles(bs)) {
					// roll back the translation
					translateAtom(model.getAtom(i), mx, my, mz, true, -t);
				}
			}
		}
		refresh();
	}

	private void rotateAtoms(BitSet bs) {
		boolean pgUp = (keyCode & Navigator.PGUP_PRESSED) == Navigator.PGUP_PRESSED;
		boolean pgDn = (keyCode & Navigator.PGDN_PRESSED) == Navigator.PGDN_PRESSED;
		boolean up = pgUp ? true : (keyCode & Navigator.UP_PRESSED) == Navigator.UP_PRESSED;
		boolean dn = pgDn ? true : (keyCode & Navigator.DOWN_PRESSED) == Navigator.DOWN_PRESSED;
		if (!up)
			up = (keyCode & Navigator.RIGHT_PRESSED) == Navigator.RIGHT_PRESSED;
		if (!dn)
			dn = (keyCode & Navigator.LEFT_PRESSED) == Navigator.LEFT_PRESSED;
		char axis = 'z';
		if ((keyCode & Navigator.X_PRESSED) == Navigator.X_PRESSED)
			axis = 'x';
		else if ((keyCode & Navigator.Y_PRESSED) == Navigator.Y_PRESSED)
			axis = 'y';
		float t = KeyboardParameterManager.sharedInstance().getRotationAngle(
				pgUp || pgDn ? KeyboardParameterManager.FAST : KeyboardParameterManager.SLOW);
		if (!up)
			t = -t;
		model.rotateSelectedAtoms(bs, axis, t);
		if (shouldRejectMoving(bs)) { // roll back the last rotation step
			model.rotateSelectedAtoms(bs, axis, -t);
		}
		refresh();
	}

	private boolean shouldRejectMoving(BitSet bs) {
		return model.outOfSimulationBox(bs) || model.overlapWithObstacles(bs) || model.getMinimumDistance(bs) < 0;
	}

	private void transformAtoms(BitSet bs, boolean shiftDown) {
		if (bs.cardinality() <= 0)
			return;
		if (shiftDown) {
			rotateAtoms(bs);
		}
		else {
			translateAtoms(bs);
		}
	}

	private void transformObstacle(int index) {
		if (index < 0 || index >= model.getObstacleCount())
			return;
		boolean pgUp = (keyCode & Navigator.PGUP_PRESSED) == Navigator.PGUP_PRESSED;
		boolean pgDn = (keyCode & Navigator.PGDN_PRESSED) == Navigator.PGDN_PRESSED;
		boolean up = pgUp ? true : (keyCode & Navigator.UP_PRESSED) == Navigator.UP_PRESSED;
		boolean dn = pgDn ? true : (keyCode & Navigator.DOWN_PRESSED) == Navigator.DOWN_PRESSED;
		if (!up)
			up = (keyCode & Navigator.RIGHT_PRESSED) == Navigator.RIGHT_PRESSED;
		if (!dn)
			dn = (keyCode & Navigator.LEFT_PRESSED) == Navigator.LEFT_PRESSED;
		boolean mx = (keyCode & Navigator.X_PRESSED) == Navigator.X_PRESSED;
		boolean my = (keyCode & Navigator.Y_PRESSED) == Navigator.Y_PRESSED;
		boolean mz = (keyCode & Navigator.Z_PRESSED) == Navigator.Z_PRESSED;
		float t = KeyboardParameterManager.sharedInstance().getAtomDisplacement(
				pgUp || pgDn ? KeyboardParameterManager.FAST : KeyboardParameterManager.SLOW);
		if (!up)
			t = -t;
		Obstacle obs = model.getObstacle(index);
		if (mx) {
			obs.getCenter().x += t;
			if (!obs.isContained('x') || obs.overlapWithAtoms()) {
				obs.getCenter().x -= t;
			}
			else {
				viewer.setObstacleLocation(index, obs.getCenter());
			}
		}
		if (my) {
			obs.getCenter().y += t;
			if (!obs.isContained('y') || obs.overlapWithAtoms()) {
				obs.getCenter().y -= t;
			}
			else {
				viewer.setObstacleLocation(index, obs.getCenter());
			}
		}
		if (mz) {
			obs.getCenter().z += t;
			if (!obs.isContained('z') || obs.overlapWithAtoms()) {
				obs.getCenter().z -= t;
			}
			else {
				viewer.setObstacleLocation(index, obs.getCenter());
			}
		}
		repaint();
	}

	protected void processKeyPressed(KeyEvent e) {
		super.processKeyPressed(e);
		if (!e.isControlDown()) {
			isKeyNavigation = true;
			if ((keyCode = keyManager.keyPressed(e)) != 0) {
				if (isAddingAtomMode() || isAddingMoleculeMode() || isImportingModelMode() || isAddingObstacleMode()) {
					translateDropPlane();
					isKeyNavigation = false;
				}
				else {
					if (obstacleIndexAndFace != null && obstacleIndexAndFace[0] >= 0) {
						transformObstacle(obstacleIndexAndFace[0]);
						isKeyNavigation = false;
					}
					else {
						BitSet bs = viewer.getSelectionSet();
						if (bs.cardinality() > 0) {
							transformAtoms(bs, e.isShiftDown());
							isKeyNavigation = false;
						}
					}
				}
			}
			if (isKeyNavigation) {
				navigator.keyPressed(e);
				model.setRotationMatrix(viewer.getRotationMatrix());
			}
		}
		// MUST consume in order to stop the event from propogating to the parent components
		e.consume();
	}

	protected void processKeyReleased(KeyEvent e) {
		super.processKeyReleased(e);
		// remove pressed bits
		if (isKeyNavigation) {
			navigator.keyReleased(e);
		}
		else {
			keyCode = keyManager.keyReleased(e);
		}
		// MUST not consume this event and leave this to the key binding to work. As a result, key binding
		// must set KeyStroke with onKeyRelease flag set to true.
		// e.consume();
	}

}