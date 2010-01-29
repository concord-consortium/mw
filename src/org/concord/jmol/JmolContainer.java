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

package org.concord.jmol;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

import org.concord.modeler.ConnectionManager;
import org.concord.modeler.draw.CallOut;
import org.concord.modeler.draw.FillMode;
import org.concord.modeler.event.PageComponentEvent;
import org.concord.modeler.event.PageComponentListener;
import org.concord.modeler.ui.IconPool;
import org.concord.modeler.util.FileChooser;
import org.concord.modeler.util.FileUtilities;
import org.jmol.api.Attachment;
import org.jmol.api.InteractionCenter;
import org.jmol.api.SiteAnnotation;
import org.myjmol.api.Scene;

public abstract class JmolContainer extends JPanel implements LoadMoleculeListener, CommandListener {

	// schemes

	public final static String SPACE_FILLING = "CPK Space Fill";
	public final static String BALL_AND_STICK = "Ball and Stick";
	public final static String STICKS = "Sticks";
	public final static String WIREFRAME = "Wireframe";
	public final static String CARTOON = "Cartoon";
	public final static String RIBBON = "Ribbon";
	public final static String MESHRIBBON = "Mesh Ribbon";
	public final static String ROCKET = "Rocket";
	public final static String TRACE = "Trace";
	public final static String STRANDS = "Strands";
	public final static String BACKBONE = "Backbone";

	// atom coloring

	private final static String[] ATOM_COLORING_OPTIONS = { "cpk", // 0
			"amino", // 1
			"structure", // 2
			"chain", // 3
			"formalcharge",// 4
			"partialcharge"// 5
	};
	public final static byte COLOR_ATOM_BY_ELEMENT = 0;
	public final static byte COLOR_ATOM_BY_AMINO_ACID = 1;
	public final static byte COLOR_ATOM_BY_STRUCTURE = 2;
	public final static byte COLOR_ATOM_BY_CHAIN = 3;
	public final static byte COLOR_ATOM_BY_FORMAL_CHARGE = 4;
	public final static byte COLOR_ATOM_BY_PARTIAL_CHARGE = 5;
	public final static byte COLOR_ATOM_CUSTOM = 6;

	// atom selection

	private final static String[] SELECTION_OPTIONS = { "all", // 0
			"not selected", // 1
			"protein", // 2
			"protein and backbone", // 3
			"protein and not backbone", // 4
			"protein and polar", // 5
			"protein and not polar", // 6
			"protein and basic", // 7
			"protein and acidic",// 8
			"protein and not (basic, acidic)", // 9
			"nucleic", // 10
			"nucleic and backbone", // 11
			"nucleic and not backbone", // 12
			"a", // 13: adenine
			"c", // 14: cytosine
			"g", // 15: guanine
			"t", // 16: thymine
			"u", // 17: uracil
			"a,t", // 18: AT pairs
			"c,g", // 19: CG pairs
			"a,u" // 20: AU pairs
	};
	public final static byte SELECT_ALL = 0;
	public final static byte SELECT_NOT_SELECTED = 1;
	public final static byte SELECT_PROTEIN = 2;
	public final static byte SELECT_PROTEIN_BACKBONE = 3;
	public final static byte SELECT_PROTEIN_SIDECHAIN = 4;
	public final static byte SELECT_PROTEIN_POLAR = 5;
	public final static byte SELECT_PROTEIN_NONPOLAR = 6;
	public final static byte SELECT_PROTEIN_BASIC = 7;
	public final static byte SELECT_PROTEIN_ACIDIC = 8;
	public final static byte SELECT_PROTEIN_NEUTRAL = 9;
	public final static byte SELECT_NUCLEIC_ALL = 10;
	public final static byte SELECT_NUCLEIC_BACKBONE = 11;
	public final static byte SELECT_NUCLEIC_BASE = 12;
	public final static byte SELECT_NUCLEIC_ADENINE = 13;
	public final static byte SELECT_NUCLEIC_CYTOSINE = 14;
	public final static byte SELECT_NUCLEIC_GUANINE = 15;
	public final static byte SELECT_NUCLEIC_THYMINE = 16;
	public final static byte SELECT_NUCLEIC_URACIL = 17;
	public final static byte SELECT_NUCLEIC_AT = 18;
	public final static byte SELECT_NUCLEIC_CG = 19;
	public final static byte SELECT_NUCLEIC_AU = 20;

	private static ResourceBundle bundle;
	private static boolean isUSLocale;

	Jmol jmol;
	List<Scene> scenes;
	Rover rover;
	MotionGenerator motionGenerator;

	/* basic states to save for Jmol */
	int modelIndex, modelCount;
	Hashtable modelSetInfo;
	private boolean hasAnimationControls;
	private String initializationScript, customInitializationScript;
	private String resourceAddress;
	private String scheme = BALL_AND_STICK;
	private boolean spin;
	private boolean dots;
	private boolean showHBonds;
	private boolean showSSBonds;
	private boolean showSelectionHalos;
	private boolean isRunning;
	private byte atomSelection = SELECT_ALL;
	private byte atomColoring = COLOR_ATOM_BY_ELEMENT;
	private Scene currentScene;
	private volatile boolean roverMode;
	private volatile boolean roverGo;
	private final Object flyMonitor = new Object();
	private volatile boolean isMoving, requestStopMoveTo;

	/* managers */
	private final Object lock = new Object();
	StructureReader opener;
	private ItineraryManager itineraryManager;
	private InformationManager informationManager;
	private AnnotationManager annotationManager;
	private InteractionManager interactionManager;
	private RoverManager roverManager;

	/* navigation */
	private Thread navThread;
	Map<Integer, SiteAnnotation> atomAnnotations, bondAnnotations;
	Map<Integer, InteractionCenter> atomInteractions, bondInteractions;
	private Thread blinkInteractionThread;
	private volatile boolean blinkInteractionFlag;
	private volatile boolean stopBlinkingInteraction;
	private boolean blinkInteractionCenters = true;
	private Thread flyThread;
	private Matrix3f inverseRotationMatrix;
	private float motionStep = 0.01f;

	/* event handling */
	private List<NavigationListener> navigationListeners;
	private List<PageComponentListener> pageComponentListenerList;
	private PageComponentEvent modelChangeEvent;

	/* GUI components */
	private JmolMenuBar menuBar;
	private JmolToolBar toolBar;
	private JPanel topPanel;
	private BottomBar bottomBar;

	private static boolean asApplet;

	public JmolContainer() {

		super(new BorderLayout());

		if (bundle == null) {
			isUSLocale = Locale.getDefault().equals(Locale.US);
			try {
				bundle = ResourceBundle.getBundle("org.concord.jmol.resources.Jmol", Locale.getDefault());
			}
			catch (MissingResourceException e) {
			}
		}

		jmol = new Jmol(this);
		jmol.addLoadMoleculeListener(this);
		jmol.addCommandListener(this);
		add(jmol, BorderLayout.CENTER);

		createActions();

		topPanel = new JPanel(new BorderLayout());
		add(topPanel, BorderLayout.NORTH);

		menuBar = new JmolMenuBar(this);
		topPanel.add(menuBar, BorderLayout.NORTH);

		toolBar = new JmolToolBar(this);
		topPanel.add(toolBar, BorderLayout.CENTER);

		bottomBar = new BottomBar(this);
		addNavigationListener(bottomBar);
		add(bottomBar, BorderLayout.SOUTH);

		setPreferredSize(new Dimension(400, 300));

		rover = new Rover();
		scenes = new ArrayList<Scene>();
		itineraryManager = new ItineraryManager(this);
		informationManager = new InformationManager(jmol);
		atomAnnotations = new HashMap<Integer, SiteAnnotation>();
		bondAnnotations = new HashMap<Integer, SiteAnnotation>();
		annotationManager = new AnnotationManager(this);
		atomInteractions = new HashMap<Integer, InteractionCenter>();
		bondInteractions = new HashMap<Integer, InteractionCenter>();
		interactionManager = new InteractionManager(this);
		roverManager = new RoverManager(this);
		motionGenerator = new MotionGenerator(this);

	}

	public static void setApplet(boolean b) {
		asApplet = b;
	}

	public static boolean isApplet() {
		return asApplet;
	}

	protected static String getInternationalText(String name) {
		if (bundle == null)
			return null;
		if (name == null)
			return null;
		if (isUSLocale)
			return null;
		String s = null;
		try {
			s = bundle.getString(name);
		}
		catch (MissingResourceException e) {
			s = null;
		}
		return s;
	}

	protected JMenu createSelectMenu(boolean popup) {
		return menuBar.createSelectMenu(popup);
	}

	protected JMenu createSchemeMenu() {
		return menuBar.createSchemeMenu();
	}

	protected JMenu createColorMenu(boolean popup) {
		return menuBar.createColorMenu(popup);
	}

	protected void updateComputedMenus(boolean popup) {
		menuBar.updateComputedMenus(popup);
	}

	void showAnimationPanel(boolean b) {
		hasAnimationControls = b;
		bottomBar.showAnimationControls(b);
		notifyChange();
	}

	boolean hasAnimationPanel() {
		if (EventQueue.isDispatchThread())
			return bottomBar.hasAnimationControls();
		return hasAnimationControls;
	}

	public Color getViewerBackground() {
		return jmol.getBackground();
	}

	public List<PageComponentListener> getPageComponentListeners() {
		return pageComponentListenerList;
	}

	public void addPageComponentListener(PageComponentListener pcl) {
		if (pcl == null)
			throw new IllegalArgumentException("null input");
		if (pageComponentListenerList == null) {
			pageComponentListenerList = new ArrayList<PageComponentListener>();
		}
		else {
			if (pageComponentListenerList.contains(pcl))
				return;
		}
		pageComponentListenerList.add(pcl);
	}

	public void removePageComponentListener(PageComponentListener pcl) {
		if (pcl == null)
			throw new IllegalArgumentException("null input");
		if (pageComponentListenerList == null)
			return;
		pageComponentListenerList.remove(pcl);
	}

	public void notifyPageComponentListeners(PageComponentEvent e) {
		if (pageComponentListenerList == null || pageComponentListenerList.isEmpty())
			return;
		for (PageComponentListener l : pageComponentListenerList) {
			l.pageComponentChanged(e);
		}
	}

	protected void notifyChange() {
		if (modelChangeEvent == null)
			modelChangeEvent = new PageComponentEvent(this, PageComponentEvent.COMPONENT_CHANGED);
		notifyPageComponentListeners(modelChangeEvent);
	}

	protected abstract void setViewerSize(Dimension dim);

	public void changeFillMode(FillMode fm) {
		jmol.changeFillMode(fm);
	}

	public void runScreensaver(boolean b) {
		setSpinOn(b);
		runScript(b ? Screensaver.getRandomScript("0.2") : "exit; loop off;");
	}

	public boolean isEmpty() {
		return jmol.viewer.getAtomCount() <= 0;
	}

	/**
	 * This method MUST be called when a user leaves the current page. All animations must be stopped if this Jmol
	 * instance is not currently used.
	 */
	public void stopImmediately() {
		stopBlinkingInteraction = true;
		if (blinkInteractionThread != null) {
			blinkInteractionThread.interrupt();
			blinkInteractionThread = null;
		}
		if (navThread != null) {
			navThread.interrupt();
			navThread = null;
		}
		setRoverMode(false);
		jmol.viewer.stopMotion(true);
		jmol.viewer.runScriptImmediatelyWithoutThread("anim off");
		isRunning = false;
		requestStopMoveTo = true;
		setSpinOn(false);
		jmol.viewer.clearScriptQueue(); // stop pending scripts immediately
		jmol.viewer.setHoverEnabled(false);
	}

	public void clear() {
		roverGo = false;
		rover.reset();
		motionGenerator.reset();
		jmol.viewer.setRoverColor(rover.getColor());
		if (jmol.viewer.getAtomCount() < 1)
			return; // already cleared?
		stopImmediately();
		jmol.clear();
		informationManager.clear();
		scenes.clear();
		clearAnnotations();
		clearInteractions();
		setFillMode(FillMode.getNoFillMode());
		setAtomColoring(COLOR_ATOM_BY_ELEMENT);
		setScheme(BALL_AND_STICK);
		setHydrogenBondsShown(false);
		setSSBondsShown(false);
		setDotsEnabled(false);
		setSelectionHaloEnabled(false);
		setAxesShown(false);
		setBoundBoxShown(false);
		setNavigationMode(false);
		jmol.setPerspectiveDepth(true);
		currentScene = null;
		initializationScript = null;
		customInitializationScript = null;
		bottomBar.setResourceName(resourceAddress != null ? resourceAddress : "None");
		bottomBar.setSceneIndex(-1, 0);
		setClickedAtom(-1);
		jmol.viewer.setZDepthMagnification(5);
		hasAnimationControls = false;
	}

	// interaction center support

	protected void setRoverMass(float mass) {
		rover.setMass(mass);
	}

	protected void setRoverMomentOfInertia(float moi) {
		rover.setMomentOfInertia(moi);
	}

	protected void setRoverCharge(float charge) {
		rover.setCharge(charge);
	}

	protected void setRoverFriction(float friction) {
		rover.setFriction(friction);
	}

	protected void setRoverColor(int rgb) {
		rover.setColor(rgb);
		jmol.viewer.setRoverColor(rover.getColor());
		jmol.repaint();
	}

	protected void setRoverVisible(boolean b) {
		jmol.viewer.setRoverPainted(b);
		jmol.repaint();
	}

	protected void setBlinkInteractionCenters(boolean b) {
		blinkInteractionCenters = b;
		jmol.viewer.setInteractionCentersVisible(b);
		if (b) {
			blinkInteractions();
		}
		else {
			stopBlinkingInteraction = true;
			if (blinkInteractionThread != null) {
				blinkInteractionThread.interrupt();
				blinkInteractionThread = null;
			}
		}
	}

	protected boolean getBlinkInteractionCenters() {
		return blinkInteractionCenters;
	}

	protected boolean hasInteractions() {
		return !atomInteractions.isEmpty() || !bondInteractions.isEmpty() || getPauliRepulsionForAllAtoms();
	}

	void clearInteractions() {
		atomInteractions.clear();
		bondInteractions.clear();
		motionGenerator.clearCenters();
		jmol.viewer.clearKeys(InteractionCenter.class);
		jmol.repaint();
	}

	protected void setCollisionDetectionForAllAtoms(boolean b) {
		motionGenerator.setCollisionDetectionForAllAtoms(b);
	}

	protected boolean getPauliRepulsionForAllAtoms() {
		return motionGenerator.getCollisionDetectionForAllAtoms();
	}

	protected void setInteraction(int i, InteractionCenter c) {
		if (c == null)
			return;
		motionGenerator.addInteractionCenter(c);
		switch (c.getHostType()) {
		case Attachment.ATOM_HOST:
			atomInteractions.put(i, c);
			Point3f p = jmol.viewer.getAtomPoint3f(i);
			if (p != null)
				c.setCoordinates(p.x, p.y, p.z);
			break;
		case Attachment.BOND_HOST:
			bondInteractions.put(i, c);
			p = jmol.viewer.getBondPoint3f1(i);
			if (p != null) {
				Point3f q = jmol.viewer.getBondPoint3f2(i);
				c.setCoordinates(0.5f * (p.x + q.x), 0.5f * (p.y + q.y), 0.5f * (p.z + q.z));
			}
			break;
		}
		c.setHost(i);
		jmol.viewer.setAttachment(c.getHostType(), i, true, InteractionCenter.class);
		jmol.viewer.setAttachmentKeyColor(c.getHostType(), i, c.getKeyRgb(), InteractionCenter.class);
		jmol.repaint();
	}

	public void removeInteraction(int i, byte hostType) {
		InteractionCenter c = atomInteractions.get(i);
		if (c != null)
			motionGenerator.removeInteractionCenter(c);
		switch (hostType) {
		case Attachment.ATOM_HOST:
			atomInteractions.remove(i);
			break;
		case Attachment.BOND_HOST:
			bondInteractions.remove(i);
			break;
		}
		jmol.viewer.setAttachment(hostType, i, false, InteractionCenter.class);
		jmol.repaint();
	}

	public InteractionCenter getInteraction(int i, byte hostType) {
		switch (hostType) {
		case Attachment.ATOM_HOST:
			return atomInteractions.get(i);
		case Attachment.BOND_HOST:
			return bondInteractions.get(i);
		}
		return null;
	}

	public int geInteractionHost(byte type, int x, int y) {
		return jmol.viewer.getAttachmentHost(type, x, y, InteractionCenter.class);
	}

	public boolean hasInteraction(byte type, int i) {
		return jmol.viewer.hasAttachment(type, i, InteractionCenter.class);
	}

	protected void editInteraction(final int i, final byte hostType) {
		if (i < 0)
			return;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				interactionManager.getEditor(i, hostType).setVisible(true);
			}
		});
	}

	protected void editRover() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				roverManager.getEditor().setVisible(true);
			}
		});
	}

	protected void setRoverGo(boolean b) {
		if (flyThread == null)
			return;
		roverGo = b;
		if (roverGo) {
			synchronized (flyMonitor) {
				flyMonitor.notifyAll();
			}
		}
	}

	protected boolean getRoverGo() {
		return roverGo;
	}

	void moveRoverTo(Point3f p) {
		p = getChasePlaneLocation(p, -rover.getChasePlaneDistance());
		motionGenerator.setPosition(p);
		jmol.viewer.setRoverPosition(p.x, p.y, p.z);
	}

	protected void setRoverMode(boolean b) {
		if (b) {
			if (flyThread != null && flyThread.isAlive())
				return;
			setChasePlaneView(true);
			roverMode = true;
			jmol.navigator.setSteering(true);
			// move forward by chasePlaneDistance to get the Rover's starting position and pass it to the
			// MotionGenerator.
			moveRoverTo(jmol.viewer.getCameraPosition());
			flyThread = new Thread("Rover") {
				public void run() {
					while (roverMode) {
						if (roverGo) {
							motionGenerator.move(motionStep);
							if (motionGenerator.readyToPaint()) {
								Point3f p = motionGenerator.getPosition();
								jmol.viewer.setRoverPosition(p.x, p.y, p.z);
								p = getChasePlaneLocation(p, rover.getChasePlaneDistance());
								jmol.viewer.setCameraPosition(p.x, p.y, p.z);
								jmol.repaint();
								try {
									Thread.sleep(10);
								}
								catch (InterruptedException e) {
									// swallowing this interrupt seems harmless
								}
							}
						}
						else {
							try {
								synchronized (flyMonitor) {
									flyMonitor.wait();
								}
							}
							catch (InterruptedException e) {
							}
						}
					}
				}
			};
			flyThread.setPriority(Thread.NORM_PRIORITY);
			flyThread.start();
		}
		else {
			if (roverMode)
				setChasePlaneView(false);
			roverMode = false;
			jmol.navigator.setSteering(false);
			if (flyThread != null) {
				flyThread.interrupt();
			}
		}
		// notifyChange();
	}

	protected boolean isRoverMode() {
		return roverMode;
	}

	private Point3f getChasePlaneLocation(Point3f p, float distance) {
		if (inverseRotationMatrix == null)
			inverseRotationMatrix = new Matrix3f();
		inverseRotationMatrix.invert(jmol.viewer.getRotationMatrix());
		Point3f x = new Point3f(0, 0, distance);
		inverseRotationMatrix.transform(x);
		x.add(p);
		return x;
	}

	private void setChasePlaneView(boolean b) {
		jmol.viewer.setRoverPainted(b);
		if (b)
			jmol.viewer.setRoverPosition(0, 0, 0);
	}

	void blinkInteractions() {
		if (blinkInteractionThread != null && blinkInteractionThread.isAlive())
			return;
		stopBlinkingInteraction = false;
		blinkInteractionThread = new Thread("Blinking interactions") {
			public void run() {
				while (!stopBlinkingInteraction) {
					if (!atomInteractions.isEmpty()) {
						for (InteractionCenter c : atomInteractions.values()) {
							jmol.viewer.setAttachmentKeyColor(c.getHostType(), c.getHost(),
									blinkInteractionFlag ? jmol.viewer.getBackgroundArgb() : c.getKeyRgb(),
									InteractionCenter.class);
						}
					}
					if (!bondInteractions.isEmpty()) {
						for (InteractionCenter c : bondInteractions.values()) {
							jmol.viewer.setAttachmentKeyColor(c.getHostType(), c.getHost(),
									blinkInteractionFlag ? jmol.viewer.getBackgroundArgb() : c.getKeyRgb(),
									InteractionCenter.class);
						}
					}
					jmol.repaint();
					try {
						Thread.sleep(500);
					}
					catch (InterruptedException e) {
					}
					blinkInteractionFlag = !blinkInteractionFlag;
				}
			}
		};
		blinkInteractionThread.setPriority(Thread.NORM_PRIORITY);
		blinkInteractionThread.start();
	}

	// annotation support

	void clearAnnotations() {
		atomAnnotations.clear();
		bondAnnotations.clear();
		jmol.viewer.clearKeys(SiteAnnotation.class);
		jmol.repaint();
	}

	public Point getAnnotationScreenCenter(byte type, int i) {
		return jmol.viewer.getAttachmentScreenCenter(type, i, SiteAnnotation.class);
	}

	public void setCallOutWindow(CallOut c) {
		jmol.setCallOutWindow(c);
	}

	public CallOut getCallOutWindow() {
		return jmol.getCallOutWindow();
	}

	public void showPopupText(String text, int bgRgb, int x, int y, int w, int h, int xCallOut, int yCallOut) {
		jmol.showPopupText(text, bgRgb, x, y, w, h, xCallOut, yCallOut);
	}

	public void hidePopupText() {
		jmol.hidePopupText();
	}

	public int findNearestAtomIndex(int x, int y) {
		return jmol.viewer.findNearestAtomIndex(x, y);
	}

	public int findNearestBondIndex(int x, int y) {
		return jmol.viewer.findNearestBondIndex(x, y);
	}

	public Point3i getAtomScreen(int i) {
		return jmol.viewer.getAtomScreen(i);
	}

	public Point3i getBondCenterScreen(int i) {
		return jmol.viewer.getBondCenterScreen(i);
	}

	public int[] getBondAtoms(int i) {
		return jmol.viewer.getBondAtoms(i);
	}

	public void setClickedAtom(int i) {
		jmol.viewer.setClickedAtom(i);
	}

	public void setClickedBond(int i) {
		jmol.viewer.setClickedBond(i);
	}

	protected void editAnnotation(final int i, final byte hostType) {
		if (i < 0)
			return;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				annotationManager.getEditor(i, hostType).setVisible(true);
			}
		});
	}

	public void setSiteAnnotation(int i, SiteAnnotation a) {
		if (a == null)
			return;
		switch (a.getHostType()) {
		case Attachment.ATOM_HOST:
			atomAnnotations.put(i, a);
			break;
		case Attachment.BOND_HOST:
			bondAnnotations.put(i, a);
			break;
		}
		jmol.viewer.setAttachment(a.getHostType(), i, true, SiteAnnotation.class);
		jmol.viewer.setAttachmentKeyColor(a.getHostType(), i, a.getKeyRgb(), SiteAnnotation.class);
		jmol.repaint();
	}

	public void removeSiteAnnotation(int i, byte hostType) {
		switch (hostType) {
		case SiteAnnotation.ATOM_HOST:
			atomAnnotations.remove(i);
			break;
		case SiteAnnotation.BOND_HOST:
			bondAnnotations.remove(i);
			break;
		}
		jmol.viewer.setAttachment(hostType, i, false, SiteAnnotation.class);
		jmol.repaint();
	}

	public SiteAnnotation getSiteAnnotation(int i, byte hostType) {
		switch (hostType) {
		case Attachment.ATOM_HOST:
			return atomAnnotations.get(i);
		case Attachment.BOND_HOST:
			return bondAnnotations.get(i);
		}
		return null;
	}

	public int getAnnotationHost(byte type, int x, int y) {
		return jmol.viewer.getAttachmentHost(type, x, y, SiteAnnotation.class);
	}

	public boolean hasAnnotation(byte type, int i) {
		return jmol.viewer.hasAttachment(type, i, SiteAnnotation.class);
	}

	// Molecular Animation Studio support

	void addNavigationListener(NavigationListener listener) {
		if (navigationListeners == null)
			navigationListeners = new CopyOnWriteArrayList<NavigationListener>();
		if (!navigationListeners.contains(listener))
			navigationListeners.add(listener);
	}

	void removeNavigationListener(NavigationListener listener) {
		if (navigationListeners != null)
			navigationListeners.remove(listener);
	}

	void notifyNavigationListeners(NavigationEvent e) {
		if (navigationListeners == null || navigationListeners.isEmpty())
			return;
		for (NavigationListener listener : navigationListeners) {
			switch (e.getType()) {
			case NavigationEvent.ARRIVAL:
				listener.arrive(e);
				break;
			case NavigationEvent.DEPARTURE:
				listener.depart(e);
				break;
			}
		}
	}

	public void setCameraAtom(int i) {
		jmol.setCameraAtom(i);
	}

	public int getCameraAtom() {
		return jmol.getCameraAtom();
	}

	int getSceneCount() {
		return scenes.size();
	}

	Scene getCurrentScene() {
		return currentScene;
	}

	int getCurrentSceneIndex() {
		return scenes.indexOf(currentScene);
	}

	private void setSceneProperties(Scene s) {
		s.setProperty("selection", new Byte(atomSelection));
		s.setProperty("atomcoloring", new Byte(atomColoring));
		s.setProperty("scheme", scheme);
	}

	void saveScene() {
		if (!scenes.contains(currentScene)) {
			saveAsNewScene();
			return;
		}
		if (JOptionPane.showConfirmDialog(this, "Are you sure you want to overwrite the current scene?",
				"Warning: Overwriting scene", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
			String s = getCurrentOrientation();
			if (s != null) {
				String[] t = s.split("\\s");
				float[] x = new float[7];// x[0], x[1], x[2] - rotation axis; x[3] - rotation degrees
				x[4] = 100; // zoom percent
				x[5] = 0; // x translation
				x[6] = 0; // y translation
				for (int i = 0; i < Math.min(t.length, 7); i++) {
					x[i] = Float.parseFloat(t[i]);
				}
				currentScene.getCameraPosition().set(jmol.viewer.getCameraPosition());
				currentScene.getRotationAxis().set(x[0], x[1], x[2]);
				currentScene.setRotationAngle(x[3]);
				currentScene.setZoomPercent(x[4]);
				currentScene.setXTrans(jmol.viewer.getTranslationXPercent());
				currentScene.setYTrans(jmol.viewer.getTranslationYPercent());
				setSceneProperties(currentScene);
				int index = scenes.indexOf(currentScene);
				notifyNavigationListeners(new NavigationEvent(this, NavigationEvent.ARRIVAL, index, index, scenes
						.size(), null, null));
			}
		}
	}

	void saveAsNewScene() {
		String s = getCurrentOrientation();
		if (s != null) {
			String[] t = s.split("\\s");
			float[] x = new float[7];// x[0], x[1], x[2] - rotation axis; x[3] - rotation degrees
			x[4] = 100; // zoom percent
			x[5] = 0; // x translation
			x[6] = 0; // y translation
			for (int i = 0; i < Math.min(t.length, 7); i++) {
				x[i] = Float.parseFloat(t[i]);
			}
			currentScene = new Scene(jmol.viewer.getCameraPosition(), new Vector3f(x[0], x[1], x[2]), x[3], x[4]);
			currentScene.setXTrans(jmol.viewer.getTranslationXPercent());
			currentScene.setYTrans(jmol.viewer.getTranslationYPercent());
			setSceneProperties(currentScene);
			scenes.add(currentScene);
			notifyNavigationListeners(new NavigationEvent(this, NavigationEvent.ARRIVAL, scenes.size() - 1, scenes
					.size() - 1, scenes.size(), null, null));
		}
	}

	private boolean moveToNextScene() {
		if (scenes.isEmpty())
			return false;
		int i = 0;
		if (currentScene != null) {
			i = scenes.indexOf(currentScene);
			if (i >= scenes.size() - 1)
				return false;
			i++;
		}
		Scene s0 = currentScene;
		currentScene = scenes.get(i);
		currentScene.setPrevious(s0);
		moveCameraToScene(currentScene);
		return true;
	}

	private void moveToPreviousScene() {
		if (scenes.isEmpty())
			return;
		Scene s0 = currentScene;
		if (currentScene != null) {
			int i = scenes.indexOf(currentScene);
			if (i == 0) {
				currentScene = jmol.getStartingScene();
				if (currentScene != null) {
					currentScene.setPrevious(s0);
					moveCameraToScene(currentScene);
				}
			}
			else {
				currentScene = scenes.get(--i);
				currentScene.setPrevious(s0);
				moveCameraToScene(currentScene);
			}
		}
	}

	void moveToScene(final int index, final boolean immediately) {
		if (index >= 0 && scenes.isEmpty())
			return;
		if (isMoving && !immediately)
			return;
		if (index < -1 || index >= scenes.size())
			return;
		if (index > 0 && index == scenes.indexOf(currentScene))
			return;
		if (navThread != null) {
			navThread.interrupt();
		}
		navThread = new Thread("Navigate to scene #" + index) {
			public void run() {
				setMoving(true);
				int i = scenes.indexOf(currentScene);
				notifyNavigationListeners(new NavigationEvent(this, NavigationEvent.DEPARTURE, i, index, scenes.size(),
						null, null));
				Scene s0 = currentScene;
				currentScene = index >= 0 ? scenes.get(index) : jmol.getStartingScene();
				if (currentScene != null) {
					currentScene.setPrevious(s0);
					moveCameraToScene(currentScene, immediately);
				}
				notifyNavigationListeners(new NavigationEvent(this, NavigationEvent.ARRIVAL, index, index, scenes
						.size(), null, null));
				setMoving(false);
			}
		};
		navThread.setPriority(immediately ? Thread.NORM_PRIORITY + 1 : Thread.NORM_PRIORITY);
		navThread.start();
	}

	void moveOneStep(final boolean next) {
		if (scenes.isEmpty())
			return;
		if (isMoving)
			return;
		navThread = new Thread(next ? "Navigate to next" : "Fly to previous") {
			public void run() {
				setMoving(true);
				int i = scenes.indexOf(currentScene);
				notifyNavigationListeners(new NavigationEvent(this, NavigationEvent.DEPARTURE, i, next ? i + 1 : i - 1,
						scenes.size(), null, null));
				if (next) {
					moveToNextScene();
				}
				else {
					moveToPreviousScene();
				}
				i = scenes.indexOf(currentScene);
				notifyNavigationListeners(new NavigationEvent(this, NavigationEvent.ARRIVAL, i, i, scenes.size(), null,
						null));
				setMoving(false);
			}
		};
		navThread.setPriority(Thread.NORM_PRIORITY);
		navThread.start();
	}

	private void setViewerProperties(Scene s) {
		if (s != null) {
			Object o = s.getProperty("selection");
			if (o instanceof Byte) {
				setAtomSelection((Byte) o);
			}
			o = s.getProperty("atomcoloring");
			if (o instanceof Byte) {
				setAtomColoring((Byte) o);
			}
			o = s.getProperty("scheme");
			if (o instanceof String) {
				setScheme((String) o);
			}
		}
	}

	private boolean isScriptSet(Scene s) {
		String t = s.getDepartScript();
		if (t != null && !t.trim().equals(""))
			return true;
		t = s.getArriveScript();
		if (t != null && !t.trim().equals(""))
			return true;
		return false;
	}

	private void moveCameraToScene(Scene scene) {
		moveCameraToScene(scene, false);
	}

	private void moveCameraToScene(Scene scene, boolean immediately) {
		if (scene == null)
			return;
		if (!isScriptSet(scene))
			setViewerProperties(scene);
		jmol.viewer.moveCameraToScene(scene, immediately);
	}

	void requestStopMoveTo() {
		requestStopMoveTo = true;
	}

	void moveNonstop() {
		if (scenes.isEmpty())
			return;
		if (isMoving)
			return;
		requestStopMoveTo = false;
		navThread = new Thread("Navigate continuously") {
			public void run() {
				setMoving(true);
				boolean b = false;
				int i = -1;
				do {
					if (requestStopMoveTo)
						break;
					i = scenes.indexOf(currentScene);
					notifyNavigationListeners(new NavigationEvent(this, NavigationEvent.DEPARTURE, i, i + 1, scenes
							.size(), null, null));
					b = moveToNextScene();
					if (currentScene != null) {
						try {
							Thread.sleep(currentScene.getStopTime() * 1000);
						}
						catch (InterruptedException e) {
						}
					}
				} while (b);
				i = scenes.indexOf(currentScene);
				notifyNavigationListeners(new NavigationEvent(this, NavigationEvent.ARRIVAL, i, i, scenes.size(), null,
						null));
				setMoving(false);
			}
		};
		navThread.setPriority(Thread.NORM_PRIORITY);
		navThread.start();
	}

	private void setMoving(boolean b) {
		isMoving = b;
		jmol.navigator.setEnabled(!b);
		jmol.repaint();
	}

	boolean isMoving() {
		return isMoving;
	}

	// jmol script support

	public void compilerErrorReported(final CommandEvent e) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(JmolContainer.this), e.getDescription(),
						"Script Error", JOptionPane.ERROR_MESSAGE);
			}
		});
	}

	/** run string scripts using Jmol's script queue, which invokes a thread per batch of scripts. */
	public String runScript(String s) {
		jmol.setScriptRunning(true);
		return jmol.viewer.evalString(s);
	}

	public void haltScriptExecution() {
		jmol.haltScriptExecution();
	}

	public void setScriptConsole(Component console) {
		jmol.setAlternativeScriptConsole(console);
	}

	// I/O support

	public void setResourceAddress(String s) {
		synchronized (lock) {
			resourceAddress = s;
		}
		jmol.setResourceAddress(s);
	}

	public String getResourceAddress() {
		synchronized (lock) {
			return resourceAddress;
		}
	}

	protected void setLoadingMessagePainted(boolean b) {
		jmol.setLoadingMessagePainted(b);
	}

	/*
	 * Set the screen dimension of the viewer to the preferred size. On some OS (e.g. Mac) the viewer might not have
	 * been given a non-zero size (i.e. made visible) when we are going to create the jmol screen image in the following
	 * code. The following line is not an ideal solution to fix this problem, because the preferred size is not
	 * necessarily the final size (although in the usual mode, the final width should be 2 pixels shorter than the
	 * preferred with).
	 */
	private void presetSize() {
		if (jmol.getSize().width == 0 || jmol.getSize().height == 0)
			jmol.viewer.setScreenDimension(getPreferredSize());
	}

	public void loadCurrentResource() {
		presetSize();
		if (resourceAddress != null) {
			jmol.setLoadingMessagePainted(true);
			restoreStates(FileUtilities.removeSuffix(getPageAddress()) + "$" + getIndex() + ".jms");
			jmol.load(resourceAddress);
		}
	}

	void loadCurrentStructure() {
		if (resourceAddress != null) {
			jmol.setLoadingMessagePainted(true);
			jmol.load(resourceAddress);
		}
	}

	public void input(File file) {
		if (file == null)
			return;
		setResourceAddress(file.getAbsolutePath());
		loadCurrentResource();
	}

	public void input(URL url) {
		if (url == null)
			return;
		setResourceAddress(url.toString());
		loadCurrentResource();
	}

	public void output(File file) {
		MyImageSaver.saveImages(this, file.getParentFile());
		OutputStream os = null;
		try {
			os = new BufferedOutputStream(new FileOutputStream(file));
		}
		catch (IOException e) {
			e.printStackTrace();
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(JmolContainer.this),
							"Error in writing to " + resourceAddress, "Write Error", JOptionPane.ERROR_MESSAGE);
				}
			});
			return;
		}
		XMLEncoder out = new XMLEncoder(os);
		try {
			encode(out);
		}
		catch (Exception e) {
			e.printStackTrace();
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(JmolContainer.this),
							"Encoding error: " + resourceAddress, "Write Error", JOptionPane.ERROR_MESSAGE);
				}
			});
		}
		finally {
			out.close();
			try {
				os.close();
			}
			catch (IOException iox) {
			}
		}
	}

	public abstract int getIndex();

	public abstract String getPageAddress();

	public void moleculeLoaded(LoadMoleculeEvent e) {

		modelSetInfo = jmol.viewer.getModelSetAuxiliaryInfo();
		modelIndex = jmol.viewer.getDisplayModelIndex();
		modelCount = jmol.viewer.getModelCount();

		if (spin) // spin is disabled while loading, restore it here after loading
			jmol.viewer.setSpinOn(true);
		StringBuffer script = new StringBuffer("selectionHalos off;");

		currentScene = scenes.isEmpty() ? jmol.getStartingScene() : scenes.get(0);
		// currentScene = jmol.getStartingScene();
		if (currentScene != null) {
			Point3f cp = currentScene.getCameraPosition();
			if (cp != null) {
				jmol.viewer.setCameraPosition(cp.x, cp.y, cp.z);
			}
			if (getNavigationMode()) {
				script.append("moveto 0 " + currentScene.rotationToString() + ";");
			}
			else {
				script.append("moveto 0 " + currentScene.rotationToString() + " " + currentScene.getXTrans() + " "
						+ currentScene.getYTrans() + ";");
			}
			setViewerProperties(currentScene);
		}
		else {
			// backward compatible
			script.append("select all;");
			setAtomColoring(COLOR_ATOM_BY_ELEMENT);
			setScheme(BALL_AND_STICK);
		}

		String s = getSchemeScript();
		if (s != null)
			script.append(s);
		script.append(dots ? "dots on;" : "dots off;");
		script.append(jmol.viewer.getShowAxes() ? "set axes 0.1;" : "set axes 0;");
		script.append(jmol.viewer.getShowBbcage() ? "set boundbox 0.2;" : "set boundbox 0;");
		initializationScript = script.toString();

		jmol.setLoadingMessagePainted(false);

		if (!atomAnnotations.isEmpty()) {
			for (Integer o : atomAnnotations.keySet()) {
				setSiteAnnotation(o, atomAnnotations.get(o));
			}
		}

		if (!bondAnnotations.isEmpty()) {
			for (Integer o : bondAnnotations.keySet()) {
				setSiteAnnotation(o, bondAnnotations.get(o));
			}
		}

		motionGenerator.clearCenters();
		if (!atomInteractions.isEmpty()) {
			for (Integer i : atomInteractions.keySet()) {
				setInteraction(i, atomInteractions.get(i));
			}
		}

		if (!bondInteractions.isEmpty()) {
			for (Integer i : bondInteractions.keySet()) {
				setInteraction(i, bondInteractions.get(i));
			}
		}

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				bottomBar.showAnimationControls(hasAnimationControls);
				jmol.updateSize(); // update size so that screenPixels can be known
				bottomBar.setResourceName(resourceAddress);
				bottomBar.setSceneIndex(scenes.isEmpty() ? -1 : 0, scenes.size());
				informationManager.displayInformation();
				jmol.waitForInitializationScript();
				runScript(initializationScript + (customInitializationScript == null ? "" : customInitializationScript));
				menuBar.updateMenusAfterLoading();
				toolBar.updateButtonsAfterLoading();
				jmol.viewer.loadingCompleted();
			}
		});

	}

	void initializationScriptCompleted() {
		setRoverMode(roverMode);
		jmol.setResizeListener(true);
	}

	private void restoreStates(String s) {
		scenes.clear();
		if (s == null)
			return;
		if (isApplet() || FileUtilities.isRemote(s)) {
			s = FileUtilities.httpEncode(s);
			URL u = null;
			try {
				u = new URL(s);
			}
			catch (MalformedURLException e) {
				e.printStackTrace();
				return;
			}
			if (ConnectionManager.sharedInstance().isCachingAllowed()) {
				File file = null;
				try {
					file = ConnectionManager.sharedInstance().shouldUpdate(u);
					if (file == null)
						file = ConnectionManager.sharedInstance().cache(u);
					restoreStates(file);
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			else {
				restoreStates(u);
			}
		}
		else {
			restoreStates(new File(s));
		}
	}

	private void restoreStates(final URL url) {
		XMLDecoder in = null;
		try {
			in = new XMLDecoder(new BufferedInputStream(url.openStream()));
		}
		catch (IOException e) {
			e.printStackTrace();
			return;
		}
		try {
			decode(in);
		}
		catch (Exception e) {
			e.printStackTrace();
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(JmolContainer.this), url
							+ " was not found or has a problem.", "File error", JOptionPane.ERROR_MESSAGE);
				}
			});
		}
	}

	private void restoreStates(final File file) {
		if (file == null || !file.exists())
			return;
		XMLDecoder in = null;
		try {
			in = new XMLDecoder(new BufferedInputStream(new FileInputStream(file)));
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		try {
			decode(in);
		}
		catch (Exception e) {
			e.printStackTrace();
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(JmolContainer.this), file
							+ " was not found or has a problem.", "File error", JOptionPane.ERROR_MESSAGE);
				}
			});
		}
	}

	private void decode(XMLDecoder in) throws Exception {
		jmol.viewer.loadingStarted();
		ModelState state = (ModelState) in.readObject();
		hasAnimationControls = state.getShowAnimationControls();
		jmol.viewer.setZDepthMagnification(state.getZDepthMagnification());
		jmol.viewer.setAxisStyle(state.getAxisStyle());
		jmol.setFillMode(state.getFillMode());
		informationManager.title = state.getTitle();
		informationManager.subtitle = state.getSubtitle();
		SceneState ss = state.getStartingScene();
		if (ss != null)
			jmol.setStartingScene(ss);
		int n = state.getSceneCount();
		if (n > 0) {
			Scene s = null;
			for (int i = 0; i < n; i++) {
				ss = state.getSceneState(i);
				s = getScene(ss.getGeoData());
				s.setArriveInformation(ss.getArriveInfo());
				s.setDepartInformation(ss.getDepartInfo());
				s.setArriveScript(ss.getArriveScript());
				s.setDepartScript(ss.getDepartScript());
				s.setTransitionTime(ss.getTransitionTime());
				s.setStopTime(ss.getStopTime());
				s.setProperty("selection", new Byte(ss.getAtomSelection()));
				s.setProperty("atomcoloring", new Byte(ss.getAtomColoring()));
				s.setProperty("scheme", ss.getScheme());
				s.setXTrans(ss.getXTrans());
				s.setYTrans(ss.getYTrans());
				scenes.add(s);
			}
			notifyNavigationListeners(new NavigationEvent(this, NavigationEvent.ARRIVAL, 0, 0, scenes.size(), null,
					null));
		}
		setCollisionDetectionForAllAtoms(state.getPauliRepulsionForAllAtoms());
		Map<Integer, SiteAnnotation> map1 = state.getAtomAnnotations();
		if (map1 != null && !map1.isEmpty()) {
			atomAnnotations.putAll(map1);
		}
		map1 = state.getBondAnnotations();
		if (map1 != null && !map1.isEmpty()) {
			bondAnnotations.putAll(map1);
		}
		Map<Integer, InteractionCenter> map2 = state.getAtomInteractions();
		if (map2 != null && !map2.isEmpty()) {
			atomInteractions.putAll(map2);
		}
		map2 = state.getBondInteractions();
		if (map2 != null && !map2.isEmpty()) {
			bondInteractions.putAll(map2);
		}
		blinkInteractionCenters = state.getBlinkInteractionCenters();
		jmol.viewer.setInteractionCentersVisible(blinkInteractionCenters);
		if (blinkInteractionCenters) {
			if (!atomInteractions.isEmpty() || !bondInteractions.isEmpty()) {
				blinkInteractions();
			}
		}
		roverMode = state.isRoverMode();
		if (roverMode) {
			rover.setMass(state.getRoverMass());
			rover.setColor(state.getRoverRgb());
			rover.setCharge(state.getRoverCharge());
			rover.setDipole(state.getRoverDipole());
			rover.setMomentOfInertia(state.getRoverMomentOfInertia());
			rover.setFriction(state.getRoverFriction());
			rover.setTurningOption(state.getRoverTurning());
			rover.setChasePlaneDistance(state.getChasePlaneDistance());
			jmol.viewer.setRoverColor(rover.getColor());
		}
		setHoverEnabled(state.isHoverEnabled());
	}

	private Scene getScene(String s) {
		Point3f p = new Point3f();
		Vector3f v = new Vector3f();
		float[] x = new float[] { 0, 0, 0, 0, 0, 0, 0, 100 };
		String[] t = s.split("\\s");
		for (int j = 0; j < Math.min(t.length, x.length); j++)
			x[j] = Float.parseFloat(t[j]);
		p.set(x[0], x[1], x[2]);
		v.set(x[3], x[4], x[5]);
		return new Scene(p, v, x[6], x[7]);
	}

	private void encode(XMLEncoder out) throws Exception {
		ModelState state = new ModelState();
		String s = getCurrentOrientation();
		if (s != null) {
			String[] t = s.split("\\s");
			float[] x = new float[7];// x[0], x[1], x[2] - rotation axis; x[3] - rotation degrees
			x[4] = 100; // zoom percent
			x[5] = 0; // x translation
			x[6] = 0; // y translation
			for (int i = 0; i < Math.min(t.length, 7); i++) {
				x[i] = Float.parseFloat(t[i]);
			}
			if (scenes.isEmpty()) {
				SceneState ss = new SceneState(new Scene(jmol.viewer.getCameraPosition(),
						new Vector3f(x[0], x[1], x[2]), x[3], x[4]));
				ss.setAtomSelection(atomSelection);
				ss.setAtomColoring(atomColoring);
				ss.setScheme(scheme);
				ss.setXTrans(jmol.viewer.getTranslationXPercent());
				ss.setYTrans(jmol.viewer.getTranslationYPercent());
				state.setStartingScene(ss);
			}
			else {
				state.setStartingScene(new SceneState(scenes.get(0)));
			}
		}
		state.setPauliRepulsionForAllAtoms(getPauliRepulsionForAllAtoms());
		state.setBlinkInteractionCenters(blinkInteractionCenters);
		state.setShowAnimationControls(hasAnimationControls);
		state.setZDepthMagnification(jmol.viewer.getZDepthMagnification());
		state.setHoverEnabled(isHoverEnabled());
		state.setAxisStyle(jmol.viewer.getAxisStyle());
		state.setFillMode(jmol.getFillMode());
		state.setTitle(informationManager.title);
		state.setSubtitle(informationManager.subtitle);
		for (Scene sn : scenes) {
			state.addScene(new SceneState(sn));
		}
		state.setAtomAnnotations(atomAnnotations);
		state.setBondAnnotations(bondAnnotations);
		state.setAtomInteractions(atomInteractions);
		state.setBondInteractions(bondInteractions);
		state.setRoverMode(roverMode);
		if (roverMode) {
			state.setRoverMass(rover.getMass());
			state.setRoverRgb(rover.getColor());
			state.setRoverCharge(rover.getCharge());
			state.setRoverMomentOfInertia(rover.getMomentOfInertia());
			state.setRoverFriction(rover.getFriction());
			state.setRoverTurning(rover.getTurningOption());
			state.setChasePlaneDistance(rover.getChasePlaneDistance());
		}
		out.writeObject(state);
	}

	// image layer support

	public void addImage(String filename, float x, float y) {
		ImageComponent ic = null;
		try {
			ic = new ImageComponent(filename);
		}
		catch (Exception ioe) {
			ioe.printStackTrace();
		}
		if (ic != null) {
			ic.setLocation(x, y);
			jmol.addImage(ic);
		}
	}

	public void addImage(URL url, float x, float y) {
		ImageComponent ic = null;
		try {
			ic = new ImageComponent(url);
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
		if (ic != null) {
			ic.setLocation(x, y);
			jmol.addImage(ic);
		}
	}

	public int getNumberOfImages() {
		return jmol.getNumberOfImages();
	}

	public void setImageSelectionSet(BitSet bs) {
		if (bs == null)
			return;
		if (getNumberOfImages() == 0)
			return;
		int n = getNumberOfImages();
		for (int i = 0; i < n; i++) {
			jmol.getImage(i).setSelected(bs.get(i));
		}
	}

	public void removeSelectedImages() {
		Iterator it = jmol.getImageIterator();
		if (it == null)
			return;
		while (it.hasNext()) {
			if (((ImageComponent) it.next()).isSelected())
				it.remove();
		}
	}

	// Methods to store and restore states

	public String getInitializationScript() {
		return initializationScript;
	}

	public String getCustomInitializationScript() {
		return customInitializationScript;
	}

	public void setCustomInitializationScript(String s) {
		customInitializationScript = s;
	}

	public void setNavigationMode(boolean b) {
		synchronized (lock) {
			jmol.viewer.setNavigationMode(b);
		}
	}

	public boolean getNavigationMode() {
		synchronized (lock) {
			return jmol.viewer.getNavigationMode();
		}
	}

	public void setDotsEnabled(boolean b) {
		synchronized (lock) {
			dots = b;
		}
	}

	public boolean isDotsEnabled() {
		synchronized (lock) {
			return dots;
		}
	}

	public void setBoundBoxShown(boolean b) {
		synchronized (lock) {
			jmol.viewer.setShowBbcage(b);
		}
		jmol.viewer.runScriptImmediatelyWithoutThread(b ? "set boundbox 0.2" : "set boundbox 0");
	}

	public boolean isBoundBoxShown() {
		synchronized (lock) {
			return jmol.viewer.getShowBbcage();
		}
	}

	public void setAxesShown(boolean b) {
		synchronized (lock) {
			jmol.viewer.setShowAxes(b);
		}
		jmol.viewer.runScriptImmediatelyWithoutThread(b ? "set axes 0.1" : "set axes 0");
	}

	public boolean areAxesShown() {
		synchronized (lock) {
			return jmol.viewer.getShowAxes();
		}
	}

	public void setHoverEnabled(boolean b) {
		synchronized (lock) {
			jmol.viewer.setHoverEnabled(b);
		}
	}

	public boolean isHoverEnabled() {
		synchronized (lock) {
			return jmol.viewer.isHoverEnabled();
		}
	}

	public void setHydrogenBondsShown(boolean b) {
		synchronized (lock) {
			showHBonds = b;
		}
		jmol.viewer.runScriptImmediatelyWithoutThread(b ? "hbonds 0.1" : "hbonds off");
	}

	public boolean areHydrogenBondsShown() {
		synchronized (lock) {
			return showHBonds;
		}
	}

	public void setSSBondsShown(boolean b) {
		synchronized (lock) {
			showSSBonds = b;
		}
		jmol.viewer.runScriptImmediatelyWithoutThread(b ? "ssbonds 0.1" : "ssbonds off");
	}

	public boolean areSSBondsShown() {
		synchronized (lock) {
			return showSSBonds;
		}
	}

	public void setSpinOn(boolean b) {
		spin = b; // spin is the state parameter for this jmol container
		jmol.viewer.setSpinOn(b);
	}

	public boolean getSpinOn() {
		return jmol.viewer.getSpinOn();
	}

	public void setSelectionHaloEnabled(boolean b) {
		synchronized (lock) {
			showSelectionHalos = b;
		}
		jmol.viewer.setSelectionHaloEnabled(b);
	}

	public boolean isSelectionHaloEnabled() {
		synchronized (lock) {
			return showSelectionHalos;
		}
	}

	public void setCPKRadius(int i) {
		synchronized (lock) {
			jmol.viewer.setPercentVdwAtom(i);
		}
	}

	public int getCPKRadius() {
		synchronized (lock) {
			return jmol.viewer.getPercentVdwAtom();
		}
	}

	public void setAtomColoring(byte b) {
		synchronized (lock) {
			atomColoring = b;
		}
		if (b >= 0 && b < ATOM_COLORING_OPTIONS.length)
			jmol.viewer.runScriptImmediatelyWithoutThread("color atoms " + ATOM_COLORING_OPTIONS[b]);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				menuBar.setAtomColorMenu(atomColoring);
			}
		});
	}

	public byte getAtomColoring() {
		synchronized (lock) {
			return atomColoring;
		}
	}

	public void setAtomSelection(byte b) {
		synchronized (lock) {
			atomSelection = b;
		}
		if (b >= 0 && b < SELECTION_OPTIONS.length) {
			jmol.viewer.runScriptImmediatelyWithoutThread("select " + SELECTION_OPTIONS[b]);
		}
		else {
			jmol.viewer.clearSelection();
		}
	}

	public byte getAtomSelection() {
		synchronized (lock) {
			return atomSelection;
		}
	}

	public String getCurrentOrientation() {
		return jmol.viewer.getCurrentOrientation();
	}

	public Scene getStartingScene() {
		return jmol.getStartingScene();
	}

	/** backward compatibility */
	public void setRotationAndZoom(String s) {
		if (s == null)
			return;
		String[] t = s.trim().split("\\s");
		float[] x = new float[] { 0, 0, 0, 0, 100 };
		for (int i = 0; i < Math.min(t.length, 5); i++)
			x[i] = Float.parseFloat(t[i]);
		if (x[0] == 0 && x[1] == 0 && x[2] == 0) {// rotation axis (0, 0, 0) has no meaning, set it to z-axis
			x[2] = 1;
		}
		jmol.setRotationAndZoom(new Vector3f(x[0], x[1], x[2]), x[3], x[4]);
	}

	/** set the display scheme. */
	public void setScheme(String s) {
		synchronized (lock) {
			scheme = s;
		}
		String script = getSchemeScript();
		if (script != null) {
			jmol.viewer.runScriptImmediatelyWithoutThread(script);
		}
	}

	/** @see org.concord.jmol.JmolContainer#setScheme */
	public String getScheme() {
		synchronized (lock) {
			return scheme;
		}
	}

	private String getSchemeScript() {
		if (scheme != null)
			scheme = scheme.intern();
		if (scheme == BALL_AND_STICK)
			return "backbone off;spacefill 20%;wireframe 0.15;cartoon off;ribbon off;rocket off;trace off;strands off;meshribbon off;";
		if (scheme == SPACE_FILLING)
			return "backbone off;wireframe off;spacefill 100%;cartoon off;ribbon off;rocket off;trace off;strands off;meshribbon off;";
		if (scheme == STICKS)
			return "backbone off;spacefill off;wireframe 0.3;cartoon off;ribbon off;rocket off;trace off;strands off;meshribbon off;";
		if (scheme == WIREFRAME)
			return "backbone off;spacefill off;wireframe on;cartoon off;ribbon off;rocket off;trace off;strands off;meshribbon off;";
		if (scheme == CARTOON)
			return "backbone off;spacefill off;wireframe off;cartoon on;ribbon off;rocket off;trace off;strands off;meshribbon off;";
		if (scheme == RIBBON)
			return "backbone off;spacefill off;wireframe off;cartoon off;ribbon on;rocket off;trace off;strands off;meshribbon off;";
		if (scheme == ROCKET)
			return "backbone off;spacefill off;wireframe off;cartoon off;ribbon off;rocket on;trace off;strands off;meshribbon off;";
		if (scheme == TRACE)
			return "backbone off;spacefill off;wireframe off;cartoon off;ribbon off;rocket off;trace on;strands off;meshribbon off;";
		if (scheme == STRANDS)
			return "backbone off;spacefill off;wireframe off;cartoon off;ribbon off;rocket off;trace off;strands 0.99;meshribbon off;";
		if (scheme == BACKBONE)
			return "backbone 0.8;spacefill off;wireframe off;cartoon off;ribbon off;rocket off;trace off;strands off;meshribbon off;";
		if (scheme == MESHRIBBON)
			return "backbone off;spacefill off;wireframe off;cartoon off;ribbon off;rocket off;trace off;strands off;meshribbon on;";
		return null;
	}

	protected void runAnimation() {
		isRunning = true;
		jmol.viewer.runScriptImmediatelyWithoutThread("anim on");
	}

	public boolean isRunning() {
		return isRunning;
	}

	public void setScreenshotAction(Action a) {
		menuBar.setScreenshotAction(a);
	}

	public void setSnapshotListener(ActionListener a) {
		menuBar.setSnapshotListener(a);
	}

	public JComponent getView() {
		return jmol;
	}

	public Color getAtomColor(int index) {
		return jmol.getAtomColor(index);
	}

	/** @see org.concord.jmol.Jmol#getBondColor */
	public Color getBondColor(int index) {
		return jmol.getBondColor(index);
	}

	protected void selectBondColorInheritMode(final boolean b) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				menuBar.selectBondColorInheritMode(b);
			}
		});
	}

	protected void selectHBondColorInheritMode(final boolean b) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				menuBar.selectHBondColorInheritMode(b);
			}
		});
	}

	protected void selectSSBondColorInheritMode(final boolean b) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				menuBar.selectSSBondColorInheritMode(b);
			}
		});
	}

	protected void selectAtomSingleColorMode(final boolean b) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				menuBar.selectAtomSingleColorMode(b);
			}
		});
	}

	public void setFillMode(FillMode fm) {
		jmol.setFillMode(fm);
	}

	public FillMode getFillMode() {
		return jmol.getFillMode();
	}

	// The following is for setting up the container's GUI

	private void createActions() {

		// open action
		opener = new StructureReader();
		opener.setMolecularViewer(this);
		jmol.getInputMap().put((KeyStroke) opener.getValue(Action.ACCELERATOR_KEY), "open");
		jmol.getActionMap().put("open", opener);

		// reset action
		Action a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				jmol.home();
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.NAME, "Reset Position");
		a.putValue(Action.SHORT_DESCRIPTION, "Reset to starting position");
		a.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
		a.putValue(Action.SMALL_ICON, IconPool.getIcon("house"));
		jmol.getActionMap().put("reset", a);

		// spin action
		a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				setSpinOn(!getSpinOn());
				notifyChange();
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.NAME, "Spin");
		a.putValue(Action.SHORT_DESCRIPTION, "Spin the structure");
		a.putValue("state", getSpinOn() ? Boolean.TRUE : Boolean.FALSE);
		jmol.getActionMap().put("spin", a);

		// boundbox action
		a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				setBoundBoxShown(((AbstractButton) e.getSource()).isSelected());
				notifyChange();
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.NAME, "Show Boundbox");
		a.putValue(Action.SHORT_DESCRIPTION, "Show boundbox");
		a.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_B));
		a.putValue("state", isBoundBoxShown() ? Boolean.TRUE : Boolean.FALSE);
		jmol.getActionMap().put("bound box", a);

		// axis action
		a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				setAxesShown(((AbstractButton) e.getSource()).isSelected());
				notifyChange();
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.NAME, "Show Axes");
		a.putValue(Action.SHORT_DESCRIPTION, "Show axes");
		a.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_X));
		a.putValue("state", areAxesShown() ? Boolean.TRUE : Boolean.FALSE);
		jmol.getActionMap().put("axes", a);

		// hover action
		a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				setHoverEnabled(((AbstractButton) e.getSource()).isSelected());
				notifyChange();
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.NAME, "Enable Hover Text");
		a.putValue(Action.SHORT_DESCRIPTION, "Enable hover text over atoms");
		a.putValue("state", isHoverEnabled() ? Boolean.TRUE : Boolean.FALSE);
		jmol.getActionMap().put("hover", a);

		// show selection action
		a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				AbstractButton ab = (AbstractButton) e.getSource();
				boolean b = ab.isSelected();
				setSelectionHaloEnabled(b);
				jmol.repaint();
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.NAME, "Show Selection");
		a.putValue(Action.SHORT_DESCRIPTION, "Show selection");
		a.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		a.putValue(Action.SMALL_ICON, new ImageIcon(JmolContainer.class.getResource("resources/showselection.gif")));
		a.putValue("state", isSelectionHaloEnabled() ? Boolean.TRUE : Boolean.FALSE);
		jmol.getActionMap().put("show selection", a);

		// show hydrogen bonds action
		a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				setHydrogenBondsShown(((AbstractButton) e.getSource()).isSelected());
				notifyChange();
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.NAME, "Show Hydrogen Bonds");
		a.putValue(Action.SHORT_DESCRIPTION, "Show hydrogen bonds");
		a.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_H));
		a.putValue("state", areHydrogenBondsShown() ? Boolean.TRUE : Boolean.FALSE);
		jmol.getActionMap().put("show hbonds", a);

		// show disulfide bonds action
		a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				setSSBondsShown(((AbstractButton) e.getSource()).isSelected());
				notifyChange();
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.NAME, "Show Disulfide Bonds");
		a.putValue(Action.SHORT_DESCRIPTION, "Show disulfide bonds");
		a.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		a.putValue("state", areSSBondsShown() ? Boolean.TRUE : Boolean.FALSE);
		jmol.getActionMap().put("show ssbonds", a);

	}

	protected void setAtomColorSelectionMenu(JMenu menu, boolean popup) {
		menuBar.setAtomColorSelectionMenu(menu, popup);
	}

	protected void setBondColorSelectionMenu(JMenu menu, boolean popup) {
		menuBar.setBondColorSelectionMenu(menu, popup);
	}

	protected void setHBondColorSelectionMenu(JMenu menu, boolean popup) {
		menuBar.setHBondColorSelectionMenu(menu, popup);
	}

	protected void setSSBondColorSelectionMenu(JMenu menu, boolean popup) {
		menuBar.setSSBondColorSelectionMenu(menu, popup);
	}

	protected void setBackgroundMenu(JMenu menu, boolean popup) {
		menuBar.setBackgroundMenu(menu, popup);
	}

	public void addMouseListenerToViewer(MouseListener ml) {
		if (ml == null)
			return;
		jmol.addMouseListener(ml);
	}

	public void removeMouseListenerFromViewer(MouseListener ml) {
		if (ml == null)
			return;
		jmol.removeMouseListener(ml);
	}

	public MouseListener[] getMouseListenersOfViewer() {
		return jmol.getMouseListeners();
	}

	public void enableMenuBar(boolean b) {
		if (b) {
			topPanel.add(menuBar, BorderLayout.NORTH);
		}
		else {
			topPanel.remove(menuBar);
		}
		validate();
		jmol.updateSize();
		jmol.repaint();
	}

	public boolean isMenuBarEnabled() {
		int n = topPanel.getComponentCount();
		for (int i = 0; i < n; i++) {
			if (menuBar == topPanel.getComponent(i))
				return true;
		}
		return false;
	}

	public void enableToolBar(boolean b) {
		if (b) {
			topPanel.add(toolBar, BorderLayout.CENTER);
		}
		else {
			topPanel.remove(toolBar);
		}
		validate();
		jmol.updateSize();
		jmol.repaint();
	}

	public boolean isToolBarEnabled() {
		int n = topPanel.getComponentCount();
		for (int i = 0; i < n; i++) {
			if (toolBar == topPanel.getComponent(i))
				return true;
		}
		return false;
	}

	public void enableBottomBar(boolean b) {
		if (b) {
			add(bottomBar, BorderLayout.SOUTH);
		}
		else {
			remove(bottomBar);
		}
		validate();
		jmol.updateSize();
		jmol.repaint();
	}

	public boolean isBottomBarEnabled() {
		int n = getComponentCount();
		for (int i = 0; i < n; i++) {
			if (bottomBar == getComponent(i))
				return true;
		}
		return false;
	}

	public void setFileChooser(FileChooser fc) {
		opener.setFileChooser(fc);
	}

	protected void changeNavigationMode(boolean b) {
		if (b) {
			String s1 = getInternationalText("Center");
			String s2 = getInternationalText("FrontView");
			// String s3 = getInternationalText("BackView");
			String s4 = getInternationalText("CameraPosition");
			String s5 = getInternationalText("WhereToMoveCamera");
			String[] s = { s1 != null ? s1 : "Center", s2 != null ? s2 : "Front" };
			int i = JOptionPane.showOptionDialog(JmolContainer.this, s5 != null ? s5
					: "Where do you want to move the camera?", s4 != null ? s4 : "Camera Position",
					JOptionPane.OK_OPTION, JOptionPane.QUESTION_MESSAGE, null, s, s[0]);
			jmol.viewer.setNavigationMode(true);
			String orie = jmol.viewer.getCurrentOrientation();
			jmol.viewer.runScriptImmediatelyWithoutThread("reset");
			Point3f p = jmol.viewer.getRotationCenter();
			float r = jmol.viewer.getRotationRadius();
			if (i == JOptionPane.CLOSED_OPTION || i == 0) {
				// jmol.viewer.setCameraPosition(p.x, p.y, p.z);
				// keep the orientation when centered
				float[] x = new float[7]; // x[0], x[1], x[2] - rotation axis; x[3] - rotation degrees
				x[0] = 0;
				x[1] = 1;
				x[2] = 0;
				x[3] = 0.1f;
				String[] t = orie.split("\\s");
				for (int k = 0; k < Math.min(t.length, 4); k++) {
					x[k] = Float.parseFloat(t[k]);
				}
				jmol.viewer.moveCameraTo(0, x[0], x[1], x[2], x[3], p.x, p.y, p.z);
			}
			else if (i == 1) {
				jmol.viewer.setCameraPosition(p.x, p.y, p.z + r * 2);
			}
		}
		else {
			jmol.viewer.setNavigationMode(false);
			if (isRoverMode())
				setRoverMode(false);
		}
		jmol.viewer.refresh();
		jmol.repaint();
	}

	void editInformation() {
		JDialog d = informationManager.getEditor(this);
		if (d != null) {
			d.setLocationRelativeTo(this);
			d.setVisible(true);
			notifyChange();
		}
	}

	void editItinerary() {
		if (scenes.isEmpty())
			return;
		JDialog d = itineraryManager.getEditor();
		if (d != null) {
			d.setLocationRelativeTo(JOptionPane.getFrameForComponent(this));
			d.setVisible(true);
			if (itineraryManager.returnHome) {
				if (jmol.getStartingScene() != null) {
					moveToScene(scenes.isEmpty() ? -1 : 0, false);
				}
				else {
					jmol.home();
				}
				notifyNavigationListeners(new NavigationEvent(this, NavigationEvent.ARRIVAL, 0, 0, scenes.size(), null,
						null));
			}
			notifyChange();
		}
	}

}