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

package org.concord.mw2d;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Line2D;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.concord.modeler.ConnectionManager;
import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.VectorFlavor;
import org.concord.modeler.draw.Draw;
import org.concord.modeler.draw.FillMode;
import org.concord.modeler.draw.GradientFactory;
import org.concord.modeler.draw.LineStyle;
import org.concord.modeler.draw.StrokeFactory;
import org.concord.modeler.event.ImageEvent;
import org.concord.modeler.event.ImageImporter;
import org.concord.modeler.event.ModelEvent;
import org.concord.modeler.event.PageComponentEvent;
import org.concord.modeler.math.Vector2D;
import org.concord.modeler.process.Executable;
import org.concord.modeler.process.Job;
import org.concord.modeler.text.XMLCharacterDecoder;
import org.concord.modeler.ui.IconPool;
import org.concord.modeler.ui.PrintableComponent;
import org.concord.modeler.util.ComponentPrinter;
import org.concord.modeler.util.FileUtilities;
import org.concord.modeler.util.ImageReader;
import org.concord.modeler.util.ScreenshotSaver;
import org.concord.mw2d.models.EllipseComponent;
import org.concord.mw2d.models.FieldArea;
import org.concord.mw2d.models.ImageComponent;
import org.concord.mw2d.models.Layered;
import org.concord.mw2d.models.LineComponent;
import org.concord.mw2d.models.MDModel;
import org.concord.mw2d.models.MesoModel;
import org.concord.mw2d.models.ModelComponent;
import org.concord.mw2d.models.ObstacleCollection;
import org.concord.mw2d.models.Particle;
import org.concord.mw2d.models.RectangleComponent;
import org.concord.mw2d.models.RectangularBoundary;
import org.concord.mw2d.models.RectangularObstacle;
import org.concord.mw2d.models.TextBoxComponent;
import org.concord.mw2d.models.TriangleComponent;
import org.concord.mw2d.models.UserField;
import org.concord.mw2d.ui.MDContainer;

import static org.concord.mw2d.UserAction.*;

public abstract class MDView extends PrintableComponent {

	final static int UP_PRESSED = 1;
	final static int DOWN_PRESSED = 2;
	final static int LEFT_PRESSED = 4;
	final static int RIGHT_PRESSED = 8;
	final static double ZERO = 0.000000001;
	final static int MINIMUM_MOUSE_DRAG_RESPONSE_INTERVAL = 20;
	final static int SPEED_RENDERING = 1;
	final static int ANTIALIASING_OFF = 2;

	final static String CUT = "Cut";
	final static String COPY = "Copy";
	final static String PASTE = "Paste";
	final static String CLEAR = "Clear";

	private final static String UNDO = "Undo";
	private final static String REDO = "Redo";
	private static ResourceBundle bundle;
	private static boolean isUSLocale = Locale.getDefault().equals(Locale.US);

	private int renderingMethod;
	private Color contrastBgColor;
	private static Icon clockIcon;
	private static Icon waitIcon;
	private Color markColor = new Color(204, 204, 255);
	private Point mousePressedPoint; // unlike clickPoint, this point is not necessarily contained by a component.
	private Point dragPoint = new Point();
	Point anchorPoint = new Point();
	private boolean dragging;
	boolean doNotFireUndoEvent;

	Object pasteBuffer;
	SelectedArea selectedArea = new SelectedArea();

	ViewProperties viewProp;
	ModelProperties modelProp;
	Energizer energizer;
	SteeringForceController steeringForceController;
	boolean energizerButtonPressed;
	PointHeater pointHeater = new PointHeater();
	Point clickPoint = new Point(); // the point of a component where it is clicked
	VectorFlavor velocityFlavor, momentumFlavor, accelerationFlavor, forceFlavor;

	/*
	 * if this flag is set true, calling the <tt>repaint()</tt> method of this view will not paint anything of the
	 * view's content. This mechanism is set to prevent clash of the painting thread with any other thread on accessing
	 * collections such as bonds.
	 */
	boolean dragSelected;

	boolean readyToAdjustVelocityVector;
	boolean readyToAdjustDistanceVector;
	int indexOfSelectedMeasurement = -1;
	boolean repaintBlocked;
	int keyPressedCode;
	long mousePressedTime;
	int mouseHeldX, mouseHeldY;
	boolean popupMenuEnabled = true;
	boolean showHeatBath = true;
	double relativeKEForShading = 1.0;
	boolean showClock = true, showParticleIndex, drawCharge = true, showSelectionHalo = true;
	boolean drawExternalForce;
	boolean showMirrorImages = true;
	float chargeIncrement = 0.5f;
	short actionID = SELE_ID;
	Cursor previousCursor;
	private Cursor externalCursor;
	RectangularBoundary boundary;
	Map<String, Action> booleanSwitches;
	Map<String, Action> multipleChoices;
	ImageIcon backgroundImage;
	FillMode fillMode;
	byte restraintStyle = StyleConstant.RESTRAINT_CROSS_STYLE;
	byte trajectoryStyle = StyleConstant.TRAJECTORY_LINE_STYLE;
	ModelComponent selectedComponent;
	AbstractButton undoUIComponent, redoUIComponent;
	private JPopupMenu tipPopupMenu;
	private List<ActionStateListener> actionStateListeners;

	/*
	 * The ancestor component accomodating this view. When the size of the view changes, the ancestor component should
	 * resize accordingly. The ancestor is not necessarily the immediate parent of the view, nor is it the root in the
	 * UI hierarchy. Use your own discretion to determine which level of ancestor should respond to the change of view
	 * scope.
	 */
	Component ancestor;

	ErrorReminder errorReminder;

	JPopupMenu popupMenuForLayeredComponent;

	/* place to hold image components added to this view */
	List<Layered> layerBasket;

	/* a friction field to counter the steering force on a particle. */
	double steerFriction = 0.1;

	private boolean editable;
	private boolean clockPainted = true;
	private String colorCoding = "None";
	private boolean energizerOn;
	private JComponent toolBar;
	private boolean actionTipEnabled;
	private boolean propertyDialogEnabled = true;
	private final Object updateLock = new Object();

	public MDView() {

		if (bundle == null && !isUSLocale) {
			try {
				bundle = ResourceBundle.getBundle("org.concord.mw2d.images.ViewBundle", Locale.getDefault());
			}
			catch (MissingResourceException e) {
			}
		}

		setBackground(Color.white);
		setBorder(BorderFactory.createLoweredBevelBorder());
		velocityFlavor = new VectorFlavor(Color.black, ViewAttribute.THIN, 100);
		momentumFlavor = new VectorFlavor(Color.gray, ViewAttribute.THIN, 1);
		accelerationFlavor = new VectorFlavor(Color.red, ViewAttribute.THIN, 100);
		forceFlavor = new VectorFlavor(Color.orange, ViewAttribute.THIN, 1);
		previousCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
		boundary = new RectangularBoundary(0, 0, 0, 0, getModel());
		setPreferredSize(new Dimension(MDModel.DEFAULT_WIDTH, MDModel.DEFAULT_HEIGHT));

		layerBasket = Collections.synchronizedList(new ArrayList<Layered>());

		addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				processKeyPressed(e);
			}

			public void keyReleased(KeyEvent e) {
				processKeyReleased(e);
			}
		});

		addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
				processMouseEntered(e);
			}

			public void mouseExited(MouseEvent e) {
				processMouseExited(e);
			}

			public void mousePressed(MouseEvent e) {
				processMousePressed(e);
			}

			public void mouseReleased(MouseEvent e) {
				processMouseReleased(e);
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				processMouseMoved(e);
			}

			public void mouseDragged(MouseEvent e) {
				processMouseDragged(e);
			}
		});

		errorReminder = new ErrorReminder(this);

		booleanSwitches = Collections.synchronizedMap(new TreeMap<String, Action>());
		multipleChoices = Collections.synchronizedMap(new TreeMap<String, Action>());

		Action a = new AbstractAction() {
			public Object getValue(String key) {
				if (key.equalsIgnoreCase("state"))
					return getShowParticleIndex() ? Boolean.TRUE : Boolean.FALSE;
				return super.getValue(key);
			}

			public void actionPerformed(ActionEvent e) {
				if (ModelerUtilities.stopFiring(e))
					return;
				Object o = e.getSource();
				if (o instanceof JToggleButton)
					setShowParticleIndex(((JToggleButton) o).isSelected());
			}

			public String toString() {
				return (String) getValue(Action.NAME);
			}
		};
		a.putValue(Action.NAME, "Show Particle Index");
		a.putValue(Action.SHORT_DESCRIPTION, "Show Particle Index");
		booleanSwitches.put(a.toString(), a);

		a = new AbstractAction() {
			public Object getValue(String key) {
				if (key.equalsIgnoreCase("state"))
					return getDrawCharge() ? Boolean.TRUE : Boolean.FALSE;
				return super.getValue(key);
			}

			public void actionPerformed(ActionEvent e) {
				if (ModelerUtilities.stopFiring(e))
					return;
				Object o = e.getSource();
				if (o instanceof JToggleButton)
					setDrawCharge(((JToggleButton) o).isSelected());
			}

			public String toString() {
				return (String) getValue(Action.NAME);
			}
		};
		a.putValue(Action.NAME, "Show Charge");
		a.putValue(Action.SHORT_DESCRIPTION, "Show Charge");
		booleanSwitches.put(a.toString(), a);

		a = new AbstractAction() {
			public Object getValue(String key) {
				if (key.equalsIgnoreCase("state"))
					return getDrawExternalForce() ? Boolean.TRUE : Boolean.FALSE;
				return super.getValue(key);
			}

			public void actionPerformed(ActionEvent e) {
				if (ModelerUtilities.stopFiring(e))
					return;
				Object o = e.getSource();
				if (o instanceof JToggleButton)
					setDrawExternalForce(((JToggleButton) o).isSelected());
			}

			public String toString() {
				return (String) getValue(Action.NAME);
			}
		};
		a.putValue(Action.NAME, "Show External Force");
		a.putValue(Action.SHORT_DESCRIPTION, "Show External Force");
		booleanSwitches.put(a.toString(), a);

		if (!MDContainer.isApplet()) {
			ComponentPrinter printer = new ComponentPrinter(this, "Printing a Model");
			printer.putValue(Action.NAME, "Print Model");
			getInputMap().put((KeyStroke) printer.getValue(Action.ACCELERATOR_KEY), "Print");
			getActionMap().put("Print", printer);

			ImageReader imageReader = new ImageReader("Input Image", ModelerUtilities.fileChooser, this);
			imageReader.addImageImporter(new ImageImporter() {
				public void imageImported(ImageEvent e) {
					addImage(e.getPath());
					repaint();
				}
			});
			getInputMap().put((KeyStroke) imageReader.getValue(Action.ACCELERATOR_KEY), "Input Image");
			getActionMap().put("Input Image", imageReader);

			a = new ScreenshotSaver(ModelerUtilities.fileChooser, this, false);
			a.putValue(Action.NAME, "Save a Screenshot of Model");
			getInputMap().put((KeyStroke) a.getValue(Action.ACCELERATOR_KEY), "Screenshot");
			getActionMap().put("Screenshot", a);
		}

		a = new InputTextBoxAction(this);
		getInputMap().put((KeyStroke) a.getValue(Action.ACCELERATOR_KEY), "Input Text Box");
		getActionMap().put("Input Text Box", a);

		boolean macOS = System.getProperty("os.name").startsWith("Mac");
		a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (getModel().getUndoManager().canUndo()) {
					try {
						getModel().getUndoManager().undo();
					}
					catch (CannotUndoException ex) {
						ex.printStackTrace();
					}
				}
				if (undoUIComponent != null) {
					undoUIComponent.setEnabled(getModel().getUndoManager().canUndo());
					undoUIComponent.setText(getModel().getUndoManager().getUndoPresentationName());
				}
				if (redoUIComponent != null) {
					redoUIComponent.setEnabled(getModel().getUndoManager().canRedo());
					redoUIComponent.setText(getModel().getUndoManager().getRedoPresentationName());
				}
			}
		};
		a.putValue(Action.NAME, UNDO);
		a.putValue(Action.SMALL_ICON, IconPool.getIcon("undo"));
		a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_U);
		a.putValue(Action.ACCELERATOR_KEY, macOS ? KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.META_MASK, true)
				: KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_MASK, true));
		getInputMap().put((KeyStroke) a.getValue(Action.ACCELERATOR_KEY), UNDO);
		getActionMap().put(UNDO, a);

		a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (getModel().getUndoManager().canRedo()) {
					try {
						getModel().getUndoManager().redo();
					}
					catch (CannotRedoException ex) {
						ex.printStackTrace();
					}
				}
				if (undoUIComponent != null) {
					undoUIComponent.setEnabled(getModel().getUndoManager().canUndo());
					undoUIComponent.setText(getModel().getUndoManager().getUndoPresentationName());
				}
				if (redoUIComponent != null) {
					redoUIComponent.setEnabled(getModel().getUndoManager().canRedo());
					redoUIComponent.setText(getModel().getUndoManager().getRedoPresentationName());
				}
			}

		};
		a.putValue(Action.NAME, REDO);
		a.putValue(Action.SMALL_ICON, IconPool.getIcon("redo"));
		a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
		a.putValue(Action.ACCELERATOR_KEY, macOS ? KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.META_MASK
				| KeyEvent.SHIFT_MASK, true) : KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_MASK, true));
		getInputMap().put((KeyStroke) a.getValue(Action.ACCELERATOR_KEY), REDO);
		getActionMap().put(REDO, a);

		a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (energizerOn) {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(MDView.this),
							"The thermal energizer is already on.");
				}
				else {
					setEnergizer(true);
					repaint();
				}
			}
		};
		a.putValue(Action.NAME, "Quick Heat and Cool");
		a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_Q);
		a.putValue(Action.SHORT_DESCRIPTION, "Heat and cool the system at exponential rate");
		a.putValue(Action.SMALL_ICON, IconPool.getIcon("thermometer"));
		a.putValue(Action.ACCELERATOR_KEY, macOS ? KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.ALT_MASK
				| KeyEvent.META_MASK, true) : KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.ALT_MASK
				| KeyEvent.CTRL_MASK, true));
		getInputMap().put((KeyStroke) a.getValue(Action.ACCELERATOR_KEY), "Energizer");
		getActionMap().put("Energizer", a);

		a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				new HeatBathPanel(getModel()).createDialog().setVisible(true);
			}
		};
		a.putValue(Action.NAME, "Heat Bath");
		a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_B);
		a.putValue(Action.SHORT_DESCRIPTION, "Set heat bath");
		a.putValue(Action.SMALL_ICON, IconPool.getIcon("heat bath"));
		a.putValue(Action.ACCELERATOR_KEY, macOS ? KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.ALT_MASK
				| KeyEvent.META_MASK, true) : KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.ALT_MASK
				| KeyEvent.CTRL_MASK, true));
		getInputMap().put((KeyStroke) a.getValue(Action.ACCELERATOR_KEY), "Heat Bath");
		getActionMap().put("Heat Bath", a);

		a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				showViewProperties();
			}
		};
		a.putValue(Action.NAME, "Show View Options");
		a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_V);
		a.putValue(Action.SHORT_DESCRIPTION, "Show view options");
		a.putValue(Action.SMALL_ICON, IconPool.getIcon("view"));
		a.putValue(Action.ACCELERATOR_KEY, macOS ? KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.ALT_MASK
				| KeyEvent.META_MASK, true) : KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.ALT_MASK
				| KeyEvent.CTRL_MASK, true));
		getInputMap().put((KeyStroke) a.getValue(Action.ACCELERATOR_KEY), "View Options");
		getActionMap().put("View Options", a);

		a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (modelProp == null) {
					if (getModel() instanceof MesoModel) {
						modelProp = new MesoModelProperties(JOptionPane.getFrameForComponent(MDView.this));
					}
					else {
						modelProp = new MolecularModelProperties(JOptionPane.getFrameForComponent(MDView.this));
					}
				}
				modelProp.setModel(getModel());
				if (e == null)
					modelProp.selectInitializationScriptTab();
				modelProp.setLocationRelativeTo(MDView.this);
				modelProp.setVisible(true);
			}
		};
		a.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		a.putValue(Action.SMALL_ICON, IconPool.getIcon("properties"));
		a.putValue(Action.NAME, "Access Model Properties");
		a.putValue(Action.SHORT_DESCRIPTION, "Access model properties");
		a.putValue(Action.ACCELERATOR_KEY, macOS ? KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.ALT_MASK
				| KeyEvent.META_MASK, true) : KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.ALT_MASK
				| KeyEvent.CTRL_MASK, true));
		getInputMap().put((KeyStroke) a.getValue(Action.ACCELERATOR_KEY), "Properties");
		getActionMap().put("Properties", a);

		a = new AbstractAction() {
			public Object getValue(String key) {
				if (key.equalsIgnoreCase("state"))
					return showClock ? Boolean.TRUE : Boolean.FALSE;
				return super.getValue(key);
			}

			public void actionPerformed(ActionEvent e) {
				Object o = e.getSource();
				if (o instanceof JToggleButton)
					setShowClock(((JToggleButton) o).isSelected());
			}

			public String toString() {
				return (String) getValue(Action.NAME);
			}
		};
		a.putValue(Action.NAME, "Show Clock");
		a.putValue(Action.SHORT_DESCRIPTION, "Show Clock");
		getActionMap().put("Show Clock", a);

	}

	public static String getInternationalText(String name) {
		if (bundle == null)
			return null;
		if (isUSLocale)
			return null;
		if (name == null)
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

	/** destroy this object to minimize memory leaks. */
	public void destroy() {
		getInputMap().clear();
		getActionMap().clear();
		booleanSwitches.clear();
		multipleChoices.clear();
		if (viewProp != null) {
			viewProp.destroy();
			viewProp = null;
		}
		if (modelProp != null) {
			modelProp.destroy();
			modelProp = null;
		}
		energizer = null;
		steeringForceController = null;
		undoUIComponent = null;
		redoUIComponent = null;
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
					for (ActionListener l : al)
						b.removeActionListener(l);
				}
				ItemListener[] il = b.getItemListeners();
				if (il != null) {
					for (ItemListener l : il)
						b.removeItemListener(l);
				}
			}
		}
		PopupMenuListener[] pml = pm.getPopupMenuListeners();
		if (pml != null) {
			for (PopupMenuListener l : pml)
				pm.removePopupMenuListener(l);
		}
		pm.removeAll();
	}

	public void setEditable(boolean b) {
		editable = b;
	}

	public boolean isEditable() {
		return editable;
	}

	public void addActionStateListener(ActionStateListener a) {
		if (a == null)
			return;
		if (actionStateListeners == null)
			actionStateListeners = new ArrayList<ActionStateListener>();
		actionStateListeners.add(a);
	}

	public void removeActionStateListener(ActionStateListener a) {
		if (a == null)
			return;
		if (actionStateListeners == null)
			return;
		actionStateListeners.remove(a);
	}

	void notifyActionStateListeners(ActionStateEvent e) {
		if (e == null)
			return;
		if (actionStateListeners == null)
			return;
		for (ActionStateListener a : actionStateListeners)
			a.actionStateChanged(e);
	}

	/**
	 * return true if there is no particle, no obstacle and no image in this view
	 */
	public boolean isEmpty() {
		if (getModel() == null)
			return false;
		return getModel().getNumberOfParticles() <= 0 && getModel().getObstacles().isEmpty() && layerBasket.isEmpty();
	}

	public void setUndoUIComponent(AbstractButton c) {
		undoUIComponent = c;
	}

	public void setRedoUIComponent(AbstractButton c) {
		redoUIComponent = c;
	}

	public void setRenderingMethod(int i) {
		renderingMethod = i;
	}

	public int getRenderingMethod() {
		return renderingMethod;
	}

	public void setBackground(final Color c) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				MDView.super.setBackground(c);
				if (pointHeater != null)
					pointHeater.setForeground(contrastBackground());
			}
		});
	}

	/* Caution: This method must be called in the event thread. */
	public void showTaskManager() {
		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("must be called in event thread.");
		Job job = getModel().getJob();
		if (job != null) {
			job.show(this);
		}
		else {
			JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(this),
					"There is no task yet. Please run the model.", "No task assigned", JOptionPane.WARNING_MESSAGE);
		}
	}

	private void showViewProperties() {
		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("must be called in the event thread.");
		if (viewProp == null)
			viewProp = new ViewProperties(this);
		viewProp.setCurrentValues();
		viewProp.setVisible(true);
	}

	/**
	 * called only by the model decoders and the org.concord.mw2d.activity.ResizeModelAction.
	 */
	public Dimension resize(final Dimension newSize, final boolean isLoading) {
		if (ancestor == null)
			return newSize;
		float[] objectBounds = getModel().getBoundsOfObjects();
		if (objectBounds != null) {
			float xmin = objectBounds[0];
			float ymin = objectBounds[1];
			float xmax = objectBounds[2];
			float ymax = objectBounds[3];
			RectangularBoundary boundary = getModel().getBoundary();
			if (isLoading) {
				// leave 1 margin in checking if every object is contained within the boundary,
				// because particles may slightly penetrate into the walls
				if (!boundary.contains(new Rectangle((int) (xmin + 1), (int) (ymin + 1), (int) (xmax - xmin - 2),
						(int) (ymax - ymin - 2)))) {
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							JOptionPane
									.showMessageDialog(
											JOptionPane.getFrameForComponent(MDView.this),
											"Some objects overlap with the boundary lines. This may have been caused by changing periodic boundary\nto reflecting boundary. Please move those particles off the border and re-save.",
											"Size Error", JOptionPane.ERROR_MESSAGE);
						}
					});
					return newSize;
				}
			}
			else {
				if (boundary.getType() == RectangularBoundary.DBC_ID) {
					if (xmax > newSize.width + 1 || ymax > newSize.height + 1) {
						Dimension d = getSize();
						resizeTo(d, false, false);
						EventQueue.invokeLater(new Runnable() {
							public void run() {
								JOptionPane
										.showMessageDialog(
												JOptionPane.getFrameForComponent(MDView.this),
												"The Model Container cannot be resized to the specified dimension.\nSome objects would be out of boundary.",
												"Resizing Error", JOptionPane.ERROR_MESSAGE);
							}
						});
						return d;
					}
				}
			}
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				resizeTo(newSize, isLoading, !doNotFireUndoEvent);
			}
		});
		return newSize;
	}

	private void resizeTo(Dimension newSize, boolean isLoading, boolean fireUndoEvent) {
		setPreferredSize(newSize);
		Dimension oldSize = getSize();
		if (oldSize.width == 0 || oldSize.height == 0)
			System.err.println("view size error");
		ancestor.setBounds(ancestor.getX(), ancestor.getY(), ancestor.getWidth() + newSize.width - oldSize.width,
				ancestor.getHeight() + newSize.height - oldSize.height);
		ancestor.validate();
		if (!getSize().equals(getPreferredSize())) {
			getModel().notifyModelListeners(new ModelEvent(getModel(), "Resize error", getSize(), getPreferredSize()));
		}
		else {
			if (!isLoading) {
				getModel().notifyPageComponentListeners(
						new PageComponentEvent(getModel(), PageComponentEvent.COMPONENT_RESIZED));
				if (fireUndoEvent) {
					getModel().getUndoManager().undoableEditHappened(
							new UndoableEditEvent(getModel(), new UndoableResizing(oldSize, newSize)));
					updateUndoUIComponents();
				}
			}
		}
	}

	public void clear() {
		propertyDialogEnabled = true;
		externalCursor = null;
		setFillMode(FillMode.getNoFillMode());
		setBackgroundImage(null);
		destroyAllLayeredComponents();
		setEnergizer(false);
		setAction(SELE_ID);
		drawCharge = true;
		showParticleIndex = false;
		showClock = true;
		showSelectionHalo = true;
		showMirrorImages = true;
		setColorCoding("None");
		pasteBuffer = null;
		setSelectedComponent(null);
		restraintStyle = StyleConstant.RESTRAINT_CROSS_STYLE;
		velocityFlavor.set(Color.black, ViewAttribute.THIN, 100);
		momentumFlavor.set(Color.blue, ViewAttribute.THIN, 1);
		accelerationFlavor.set(Color.red, ViewAttribute.THIN, 100);
		forceFlavor.set(Color.magenta, ViewAttribute.THIN, 1);
		pointHeater.reset();
		resetKeyPressedCode();
	}

	private void resetKeyPressedCode() {
		keyPressedCode = 0;
	}

	/** remove all the objects in the view. */
	public void removeAllObjects() {
		if (selectedArea == null) {
			selectedArea = new SelectedArea(0, 0, getWidth(), getHeight());
		}
		else {
			selectedArea.setRect(0, 0, getWidth(), getHeight());
		}
		removeSelectedArea();
	}

	void removeSelectedArea() {
		if (selectedComponent != null) {
			selectedComponent.setSelected(false);
			selectedComponent = null;
		}
	}

	/* remove any LayeredComponent intersecting the selected area */
	List<Layered> removeSelectedLayeredComponents() {
		if (layerBasket.isEmpty())
			return null;
		List<Layered> a = null;
		Layered c = null;
		boolean intersected = false;
		synchronized (layerBasket) {
			for (Iterator it = layerBasket.iterator(); it.hasNext();) {
				c = (Layered) it.next();
				if (c.getHost() != null)
					continue;
				if (c instanceof LineComponent) {
					intersected = ((LineComponent) c).intersects(selectedArea);
				}
				else if (c instanceof RectangleComponent) {
					intersected = ((RectangleComponent) c).intersects(selectedArea);
				}
				else if (c instanceof TriangleComponent) {
					intersected = ((TriangleComponent) c).intersects(selectedArea);
				}
				else if (c instanceof EllipseComponent) {
					intersected = ((EllipseComponent) c).intersects(selectedArea);
				}
				else if (c instanceof ImageComponent) {
					ImageComponent ic = (ImageComponent) c;
					intersected = selectedArea.intersects(ic.getRx(), ic.getRy(), ic.getLogicalScreenWidth(), ic
							.getLogicalScreenHeight());
				}
				else if (c instanceof TextBoxComponent) {
					TextBoxComponent tc = (TextBoxComponent) c;
					intersected = selectedArea.intersects(tc.getRx(), tc.getRy(), c.getWidth(), c.getHeight());
				}
				if (intersected) {
					if (a == null)
						a = new ArrayList<Layered>();
					a.add(c);
					it.remove();
				}
			}
		}
		return a;
	}

	void dragRect(int x, int y) {
		if (x > selectedArea.getX0()) {
			selectedArea.width = x - selectedArea.getX0();
			selectedArea.x = selectedArea.getX0();
		}
		else {
			selectedArea.width = selectedArea.getX0() - x;
			selectedArea.x = selectedArea.getX0() - selectedArea.width;
		}
		if (y > selectedArea.getY0()) {
			selectedArea.height = y - selectedArea.getY0();
			selectedArea.y = selectedArea.getY0();
		}
		else {
			selectedArea.height = selectedArea.getY0() - y;
			selectedArea.y = selectedArea.getY0() - selectedArea.height;
		}
		repaint();
	}

	public PointHeater getPointHeater() {
		return pointHeater;
	}

	/** the input model will be associated with this view */
	public void setModel(MDModel m) {

		getModel().setView(this);
		setBoundary(getModel().getBoundary());
		setPreferredSize(new Dimension((int) getModel().getBoundary().getWidth(), (int) getModel().getBoundary()
				.getHeight()));

		boolean macOS = System.getProperty("os.name").startsWith("Mac");
		Action a = new DefaultModelAction(getModel(), new Executable() {
			public void execute() {
				removeSelectedComponent();
			}
		});
		a.putValue(Action.NAME, CUT);
		a.putValue(Action.SMALL_ICON, IconPool.getIcon("cut"));
		a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_U);
		a.putValue(Action.ACCELERATOR_KEY, macOS ? KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.META_MASK, true)
				: KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_MASK, true));
		getInputMap().put((KeyStroke) a.getValue(Action.ACCELERATOR_KEY), CUT);
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, true), CUT);
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0, true), CUT);
		getActionMap().put(CUT, a);

		a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				copySelectedComponent();
			}
		};
		a.putValue(Action.NAME, COPY);
		a.putValue(Action.SMALL_ICON, getIcon(DUPL_ID));
		a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
		a.putValue(Action.ACCELERATOR_KEY, macOS ? KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_MASK, true)
				: KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK, true));
		getInputMap().put((KeyStroke) a.getValue(Action.ACCELERATOR_KEY), COPY);
		getActionMap().put(COPY, a);

		a = new DefaultModelAction(getModel(), new Executable() {
			public void execute() {
				if (mousePressedPoint == null)
					return;
				pasteBufferedComponent(mousePressedPoint.x, mousePressedPoint.y);
			}
		});
		a.putValue(Action.NAME, PASTE);
		a.putValue(Action.SMALL_ICON, IconPool.getIcon("paste"));
		a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_P);
		a.putValue(Action.SHORT_DESCRIPTION, "Paste to the last clicked point");
		a.putValue(Action.ACCELERATOR_KEY, macOS ? KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_MASK, true)
				: KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_MASK, true));
		getInputMap().put((KeyStroke) a.getValue(Action.ACCELERATOR_KEY), PASTE);
		getActionMap().put(PASTE, a);

		a = new DefaultModelAction(getModel(), new Executable() {
			public void execute() {
				if (JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(MDView.this),
						"Do you really want to remove all the objects?", "Object removal", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
					removeAllObjects();
				}
			}
		});
		a.putValue(Action.NAME, CLEAR);
		a.putValue(Action.SMALL_ICON, IconPool.getIcon("erase"));
		a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_L);
		a.putValue(Action.SHORT_DESCRIPTION, "Remove all objects");
		a.putValue(Action.ACCELERATOR_KEY, macOS ? KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.META_MASK, true)
				: KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_MASK, true));
		getInputMap().put((KeyStroke) a.getValue(Action.ACCELERATOR_KEY), CLEAR);
		getActionMap().put(CLEAR, a);

	}

	/** @return the <tt>Model</tt> associated with this view */
	public abstract MDModel getModel();

	public void enableScripterMenu(boolean b) {
	}

	/** @see org.concord.mw2d.MDView#repaintBlocked */
	public void setRepaintBlocked(boolean b) {
		repaintBlocked = b;
	}

	/** @see org.concord.mw2d.MDView#repaintBlocked */
	public boolean getRepaintBlocked() {
		return repaintBlocked;
	}

	public final Object getLayeredComponentSynchronizationLock() {
		return layerBasket;
	}

	void paintLayeredComponents(Graphics g, int layer) {
		if (layerBasket.isEmpty())
			return;
		synchronized (layerBasket) {
			for (Layered c : layerBasket) {
				if (c != null && c.getLayer() == layer)
					c.paint(g);
			}
		}
	}

	public int getLayerPosition(Layered c) {
		synchronized (layerBasket) {
			return layerBasket.indexOf(c);
		}
	}

	/** reset those animated gif images to the first frame */
	public void resetImages() {
		if (layerBasket.isEmpty())
			return;
		synchronized (layerBasket) {
			for (Layered c : layerBasket) {
				if (c instanceof ImageComponent)
					((ImageComponent) c).reset();
			}
		}
	}

	/** show the next frames of the images. */
	public void showNextFrameOfImages() {
		if (layerBasket.isEmpty())
			return;
		synchronized (layerBasket) {
			for (Layered c : layerBasket) {
				if (c instanceof ImageComponent)
					((ImageComponent) c).nextFrame();
			}
		}
	}

	/** show the n-th frames of the images. */
	public void showFrameOfImages(int index) {
		if (layerBasket.isEmpty())
			return;
		synchronized (layerBasket) {
			for (Layered c : layerBasket) {
				if (c instanceof ImageComponent)
					((ImageComponent) c).setCurrentFrame(index);
			}
		}
	}

	public void suppressErrorReminder(boolean b) {
		errorReminder.setSuppressed(b);
	}

	public boolean errorReminderSuppressed() {
		return errorReminder.isSuppressed();
	}

	public void setRestraintStyle(byte i) {
		restraintStyle = i;
	}

	public byte getRestraintStyle() {
		return restraintStyle;
	}

	public void setTrajectoryStyle(byte i) {
		trajectoryStyle = i;
	}

	public byte getTrajectoryStyle() {
		return trajectoryStyle;
	}

	public void setRelativeKEForShading(double d) {
		relativeKEForShading = d;
	}

	public double getRelativeKEForShading() {
		return relativeKEForShading;
	}

	public void setMarkColor(Color c) {
		markColor = c;
	}

	public Color getMarkColor() {
		return markColor;
	}

	public void setColorCoding(String s) {
		colorCoding = s;
	}

	public String getColorCoding() {
		return colorCoding;
	}

	// this method should be called in the first time and every time <code>setModel()</code> gets called.
	void initEditFieldActions() {
		if (getModel() == null)
			throw new NullPointerException("model is null");
		Action a = new EditGFieldAction(getModel());
		getModel().getActions().put(a.toString(), a);
		getActionMap().put("Edit Gravitational Field", a);
		a = new EditEFieldAction(getModel(), false);
		getModel().getActions().put(a.toString(), a);
		getActionMap().put("Edit Electric Field", a);
		a = new EditBFieldAction(getModel(), false);
		getModel().getActions().put(a.toString(), a);
		getActionMap().put("Edit Magnetic Field", a);
	}

	public void setSelectedComponent(ModelComponent mc) {
		if (selectedComponent != null) {
			if (getModel().getExclusiveSelection()) {
				if (selectedComponent != mc)
					selectedComponent.setSelected(false);
			}
		}
		selectedComponent = mc;
		repaint();
	}

	public ModelComponent getSelectedComponent() {
		return selectedComponent;
	}

	public Object getPasteBuffer() {
		return pasteBuffer;
	}

	public void setAction(short i) {
		if (!getModel().getExclusiveSelection()) {
			getModel().setExclusiveSelection(true);
			clearSelection();
		}
		notifyActionStateListeners(new ActionStateEvent(this, new Short(actionID), new Short(i)));
		if (actionID != i)
			externalCursor = null;
		actionID = i;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				setCursor(UserAction.getCursor(actionID));
				previousCursor = getCursor();
			}
		});
	}

	public short getAction() {
		return actionID;
	}

	public void setActionTipEnabled(boolean b) {
		actionTipEnabled = b;
	}

	public boolean isActionTipEnabled() {
		return actionTipEnabled;
	}

	public void setPropertyDialogEnabled(boolean b) {
		propertyDialogEnabled = b;
	}

	public boolean isPropertyDialogEnabled() {
		return propertyDialogEnabled;
	}

	/**
	 * translate all the components of the model by the specified displacements. This is an undoable wrapper of the
	 * method with the same signature of <code>org.concord.mw2d.models.MDModel</code>.
	 * 
	 * @see org.concord.mw2d.models.MDModel#translateWholeModel
	 */
	public boolean translateWholeModel(double dx, double dy) {
		if (getModel().translateWholeModel(dx, dy)) {
			if (!layerBasket.isEmpty()) {
				synchronized (layerBasket) {
					for (Layered c : layerBasket) {
						if (c instanceof ModelComponent)
							((ModelComponent) c).storeCurrentState();
						c.translateBy(dx, dy);
					}
				}
			}
			if (!doNotFireUndoEvent) {
				getModel().getUndoManager().undoableEditHappened(
						new UndoableEditEvent(getModel(),
								new UndoableModelOperation(UndoAction.TRANSLATE_MODEL, dx, dy)));
				updateUndoUIComponents();
			}
			return true;
		}
		return false;
	}

	/**
	 * rotate all the particles of the model by the specified angles. This is an undoable wrapper of the method with the
	 * same signature of <code>org.concord.mw2d.models.MDModel</code>.
	 * 
	 * @see org.concord.mw2d.models.MDModel#rotateWholeModel
	 */
	public boolean rotateWholeModel(double angle) {
		if (getModel().rotateWholeModel(angle)) {
			if (!doNotFireUndoEvent) {
				getModel().getUndoManager().undoableEditHappened(
						new UndoableEditEvent(getModel(), new UndoableModelOperation(UndoAction.ROTATE_MODEL, angle)));
				updateUndoUIComponents();
			}
			return true;
		}
		return false;
	}

	/**
	 * return the first instance of an image component represented by the URL. Note that there may be multiple instances
	 * of image with the same URL.
	 */
	public ImageComponent getImage(URL url) {
		if (layerBasket.isEmpty())
			return null;
		ImageComponent c = null;
		synchronized (layerBasket) {
			for (Layered l : layerBasket) {
				if (l instanceof ImageComponent) {
					c = (ImageComponent) l;
					if (c.toString().equals(url.toString()))
						return c;
				}
			}
		}
		return null;
	}

	public void addImage(URL url) {
		ImageComponent ic = null;
		try {
			ic = new ImageComponent(url.toString());
		}
		catch (Exception e) {
			e.printStackTrace();
			return;
		}
		ic.setModel(getModel());
		layerBasket.add(ic);
		ic.setLocation(10, 10);
		getModel().notifyChange();
		if (!doNotFireUndoEvent) {
			ic.setSelected(true);
			getModel().getUndoManager().undoableEditHappened(
					new UndoableEditEvent(getModel(), new UndoableLayeredComponentOperation(
							UndoAction.INSERT_LAYERED_COMPONENT, ic)));
			updateUndoUIComponents();
		}
	}

	public void addImage(String filename) {
		ImageComponent ic = null;
		try {
			ic = new ImageComponent(filename);
		}
		catch (Exception e) {
			e.printStackTrace();
			return;
		}
		ic.setModel(getModel());
		layerBasket.add(ic);
		ic.setLocation(10, 10);
		getModel().notifyChange();
		if (!doNotFireUndoEvent) {
			ic.setSelected(true);
			getModel().getUndoManager().undoableEditHappened(
					new UndoableEditEvent(getModel(), new UndoableLayeredComponentOperation(
							UndoAction.INSERT_LAYERED_COMPONENT, ic)));
			updateUndoUIComponents();
		}
	}

	/* send this LayeredComponent one frame backward in the stack */
	void layerBack(Layered c) {
		if (layerBasket.isEmpty())
			return;
		int i = layerBasket.indexOf(c);
		if (i <= 0)
			return;
		if (layerBasket.remove(c))
			layerBasket.add(i - 1, c);
		getModel().notifyChange();
		if (!doNotFireUndoEvent) {
			getModel().getUndoManager().undoableEditHappened(
					new UndoableEditEvent(getModel(), new UndoableLayeredComponentOperation(
							UndoAction.SEND_BACK_LAYERED_COMPONENT, c)));
			updateUndoUIComponents();
		}
	}

	public void sendLayerToBack(Layered c) {
		if (layerBasket.isEmpty())
			return;
		int i = layerBasket.indexOf(c);
		if (i <= 0)
			return;
		if (layerBasket.remove(c))
			layerBasket.add(0, c);
		getModel().notifyChange();
		if (!doNotFireUndoEvent) {
			getModel().getUndoManager().undoableEditHappened(
					new UndoableEditEvent(getModel(), new UndoableLayeredComponentOperation(
							UndoAction.SEND_BACK_LAYERED_COMPONENT, c)));
			updateUndoUIComponents();
		}
	}

	/* bring this LayeredComponent one frame forward in the stack */
	void layerForward(Layered c) {
		if (layerBasket.isEmpty())
			return;
		int i = layerBasket.indexOf(c);
		if (i == layerBasket.size() - 1)
			return;
		if (layerBasket.remove(c))
			layerBasket.add(i + 1, c);
		getModel().notifyChange();
		if (!doNotFireUndoEvent) {
			getModel().getUndoManager().undoableEditHappened(
					new UndoableEditEvent(getModel(), new UndoableLayeredComponentOperation(
							UndoAction.BRING_FORWARD_LAYERED_COMPONENT, c)));
			updateUndoUIComponents();
		}
	}

	public void bringLayerToFront(Layered c) {
		if (layerBasket.isEmpty())
			return;
		int i = layerBasket.indexOf(c);
		if (i == layerBasket.size() - 1)
			return;
		if (layerBasket.remove(c))
			layerBasket.add(c);
		getModel().notifyChange();
		if (!doNotFireUndoEvent) {
			getModel().getUndoManager().undoableEditHappened(
					new UndoableEditEvent(getModel(), new UndoableLayeredComponentOperation(
							UndoAction.BRING_FORWARD_LAYERED_COMPONENT, c)));
			updateUndoUIComponents();
		}
	}

	void sendBehindParticles(Layered c) {
		c.setLayer(Layered.BEHIND_PARTICLES);
		getModel().notifyChange();
		if (!doNotFireUndoEvent) {
			getModel().getUndoManager().undoableEditHappened(
					new UndoableEditEvent(getModel(), new UndoableLayeredComponentOperation(
							UndoAction.BACK_LAYERED_COMPONENT, c)));
			updateUndoUIComponents();
		}
	}

	void bringInFrontOfParticles(Layered c) {
		c.setLayer(Layered.IN_FRONT_OF_PARTICLES);
		getModel().notifyChange();
		if (!doNotFireUndoEvent) {
			getModel().getUndoManager().undoableEditHappened(
					new UndoableEditEvent(getModel(), new UndoableLayeredComponentOperation(
							UndoAction.FRONT_LAYERED_COMPONENT, c)));
			updateUndoUIComponents();
		}
	}

	void attach(Layered c) {
		int n = getModel().getNumberOfParticles();
		int imin = -1;
		double rmin = 100000000;
		double rcur = 0;
		Particle a = null;
		if (c instanceof ImageComponent || c instanceof RectangleComponent || c instanceof EllipseComponent
				|| c instanceof TriangleComponent || c instanceof LineComponent) {
			Point p = c.getCenter();
			for (int i = 0; i < n; i++) {
				a = getModel().getParticle(i);
				if (c.contains(a.getRx(), a.getRy())) {
					rcur = (a.getRx() - p.x) * (a.getRx() - p.x) + (a.getRy() - p.y) * (a.getRy() - p.y);
					if (rcur < rmin) {
						rmin = rcur;
						imin = i;
					}
				}
			}
		}
		else if (c instanceof TextBoxComponent) {
			if (((TextBoxComponent) c).isCallOut()) {
				Point p = ((TextBoxComponent) c).getCallOutPoint();
				for (int i = 0; i < n; i++) {
					a = getModel().getParticle(i);
					if (a.contains(p.x, p.y)) {
						imin = a.getIndex();
						break;
					}
				}
			}
			else {
				Point p = c.getCenter();
				for (int i = 0; i < n; i++) {
					a = getModel().getParticle(i);
					if (c.contains(a.getRx(), a.getRy())) {
						rcur = (a.getRx() - p.x) * (a.getRx() - p.x) + (a.getRy() - p.y) * (a.getRy() - p.y);
						if (rcur < rmin) {
							rmin = rcur;
							imin = i;
						}
					}
				}
			}
		}
		if (imin >= 0) {
			c.setHost(getModel().getParticle(imin));
			repaint();
			getModel().notifyChange();
			if (!doNotFireUndoEvent) {
				getModel().getUndoManager().undoableEditHappened(
						new UndoableEditEvent(getModel(), new UndoableLayeredComponentOperation(
								UndoAction.ATTACH_LAYERED_COMPONENT, c)));
				updateUndoUIComponents();
			}
		}
	}

	void attachTo(Layered c) {
		Object oldHost = c.getHost();
		AttachDialog aid = new AttachDialog(getModel());
		aid.pack();
		aid.setVisible(true);
		if (oldHost == c.getHost())
			return;
		getModel().notifyChange();
		if (!doNotFireUndoEvent) {
			getModel().getUndoManager().undoableEditHappened(
					new UndoableEditEvent(getModel(), new UndoableLayeredComponentOperation(
							UndoAction.ATTACH_LAYERED_COMPONENT, c)));
			updateUndoUIComponents();
		}
	}

	void detach(Layered c) {
		if (!doNotFireUndoEvent) {
			getModel().getUndoManager().undoableEditHappened(
					new UndoableEditEvent(getModel(), new UndoableLayeredComponentOperation(
							UndoAction.DETACH_LAYERED_COMPONENT, c)));
			updateUndoUIComponents();
		}
		c.setHost(null);
		repaint();
		getModel().notifyChange();
	}

	/**
	 * remove the first instance of image with the specified URL. Note that there may be multiple instances of image
	 * with the same URL.
	 */
	public void removeImage(URL url) {
		ImageComponent c = null;
		synchronized (layerBasket) {
			for (Layered l : layerBasket) {
				if (l instanceof ImageComponent) {
					c = (ImageComponent) l;
					if (c.toString().equals(url.toString()))
						break;
				}
			}
		}
		if (c != null) {
			layerBasket.remove(c);
			getModel().notifyChange();
			if (!doNotFireUndoEvent) {
				getModel().getUndoManager().undoableEditHappened(
						new UndoableEditEvent(getModel(), new UndoableLayeredComponentOperation(
								UndoAction.REMOVE_LAYERED_COMPONENT, c)));
				updateUndoUIComponents();
			}
		}
	}

	public List<Layered> getLayeredComponentHostedBy(ModelComponent mc) {
		if (layerBasket == null || layerBasket.isEmpty())
			return null;
		List<Layered> l = null;
		synchronized (layerBasket) {
			for (Layered c : layerBasket) {
				if (c.getHost() == mc) {
					if (l == null)
						l = new ArrayList<Layered>();
					l.add(c);
				}
			}
		}
		return l;
	}

	public void removeAttachedLayeredComponents(ModelComponent source) {
		List<Layered> l = getLayeredComponentHostedBy(source);
		if (l != null && !l.isEmpty()) {
			for (Layered a : l)
				removeLayeredComponent(a);
		}
	}

	public void copyAttachedLayeredComponents(ModelComponent source, ModelComponent acceptor) {
		List<Layered> l = getLayeredComponentHostedBy(source);
		if (l != null && !l.isEmpty()) {
			for (Layered x : l) {
				Layered y = null;
				if (x instanceof ImageComponent) {
					try {
						y = new ImageComponent((ImageComponent) x);
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}
				else if (x instanceof RectangleComponent) {
					y = new RectangleComponent((RectangleComponent) x);
				}
				else if (x instanceof TriangleComponent) {
					y = new TriangleComponent((TriangleComponent) x);
				}
				else if (x instanceof EllipseComponent) {
					y = new EllipseComponent((EllipseComponent) x);
				}
				else if (x instanceof LineComponent) {
					y = new LineComponent((LineComponent) x);
				}
				else if (x instanceof TextBoxComponent) {
					y = new TextBoxComponent((TextBoxComponent) x);
				}
				if (y != null) {
					addLayeredComponent(y);
					y.setHost(acceptor);
				}
			}
		}
	}

	public ImageComponent[] getImages() {
		int n = getNumberOfInstances(ImageComponent.class);
		ImageComponent[] t = new ImageComponent[n];
		n = 0;
		synchronized (layerBasket) {
			for (Layered c : layerBasket) {
				if (c instanceof ImageComponent)
					t[n++] = (ImageComponent) c;
			}
		}
		return t;
	}

	/** return the i-th image component added to this view */
	public ImageComponent getImage(int i) {
		if (layerBasket.isEmpty())
			return null;
		int n = 0;
		synchronized (layerBasket) {
			for (Object o : layerBasket) {
				if (o instanceof ImageComponent) {
					if (i == n)
						return (ImageComponent) o;
					n++;
				}
			}
		}
		return null;
	}

	public TextBoxComponent[] getTextBoxes() {
		int n = getNumberOfInstances(TextBoxComponent.class);
		TextBoxComponent[] t = new TextBoxComponent[n];
		n = 0;
		synchronized (layerBasket) {
			for (Layered c : layerBasket) {
				if (c instanceof TextBoxComponent)
					t[n++] = (TextBoxComponent) c;
			}
		}
		return t;
	}

	/** return the i-th text box added to this view */
	public TextBoxComponent getTextBox(int i) {
		if (layerBasket.isEmpty())
			return null;
		int n = 0;
		synchronized (layerBasket) {
			for (Layered o : layerBasket) {
				if (o instanceof TextBoxComponent) {
					if (i == n)
						return (TextBoxComponent) o;
					n++;
				}
			}
		}
		return null;
	}

	public LineComponent[] getLines() {
		int n = getNumberOfInstances(LineComponent.class);
		LineComponent[] l = new LineComponent[n];
		n = 0;
		synchronized (layerBasket) {
			for (Layered c : layerBasket) {
				if (c instanceof LineComponent)
					l[n++] = (LineComponent) c;
			}
		}
		return l;
	}

	/** return the i-th line component added to this view */
	public LineComponent getLine(int i) {
		if (layerBasket.isEmpty())
			return null;
		int n = 0;
		synchronized (layerBasket) {
			for (Layered o : layerBasket) {
				if (o instanceof LineComponent) {
					if (i == n)
						return (LineComponent) o;
					n++;
				}
			}
		}
		return null;
	}

	public RectangleComponent[] getRectangles() {
		int n = getNumberOfInstances(RectangleComponent.class);
		RectangleComponent[] l = new RectangleComponent[n];
		n = 0;
		synchronized (layerBasket) {
			for (Layered c : layerBasket) {
				if (c instanceof RectangleComponent)
					l[n++] = (RectangleComponent) c;
			}
		}
		return l;
	}

	/** return the i-th rectangle component added to this view */
	public RectangleComponent getRectangle(int i) {
		if (layerBasket.isEmpty())
			return null;
		int n = 0;
		synchronized (layerBasket) {
			for (Layered o : layerBasket) {
				if (o instanceof RectangleComponent) {
					if (i == n)
						return (RectangleComponent) o;
					n++;
				}
			}
		}
		return null;
	}

	public EllipseComponent[] getEllipses() {
		int n = getNumberOfInstances(EllipseComponent.class);
		EllipseComponent[] l = new EllipseComponent[n];
		n = 0;
		synchronized (layerBasket) {
			for (Layered c : layerBasket) {
				if (c instanceof EllipseComponent)
					l[n++] = (EllipseComponent) c;
			}
		}
		return l;
	}

	/** return the i-th ellipse component added to this view */
	public EllipseComponent getEllipse(int i) {
		if (layerBasket.isEmpty())
			return null;
		int n = 0;
		synchronized (layerBasket) {
			for (Layered o : layerBasket) {
				if (o instanceof EllipseComponent) {
					if (i == n)
						return (EllipseComponent) o;
					n++;
				}
			}
		}
		return null;
	}

	public TriangleComponent[] getTriangles() {
		int n = getNumberOfInstances(TriangleComponent.class);
		TriangleComponent[] l = new TriangleComponent[n];
		n = 0;
		synchronized (layerBasket) {
			for (Layered c : layerBasket) {
				if (c instanceof TriangleComponent)
					l[n++] = (TriangleComponent) c;
			}
		}
		return l;
	}

	/** return the i-th triangle component added to this view */
	public TriangleComponent getTriangle(int i) {
		if (layerBasket.isEmpty())
			return null;
		int n = 0;
		synchronized (layerBasket) {
			for (Layered o : layerBasket) {
				if (o instanceof TriangleComponent) {
					if (i == n)
						return (TriangleComponent) o;
					n++;
				}
			}
		}
		return null;
	}

	public FieldArea[] getFieldAreas() {
		int n = getNumberOfInstances(FieldArea.class);
		if (n == 0)
			return null;
		FieldArea[] fa = new FieldArea[n];
		n = 0;
		synchronized (layerBasket) {
			for (Layered c : layerBasket) {
				if (c instanceof FieldArea)
					fa[n++] = (FieldArea) c;
			}
		}
		return fa;
	}

	public Layered[] getLayeredComponents() {
		int n = layerBasket.size();
		Layered[] l = new Layered[n];
		synchronized (layerBasket) {
			for (int i = 0; i < n; i++)
				l[i] = layerBasket.get(i);
		}
		return l;
	}

	public int getLayeredComponentIndex(Layered a) {
		if (a == null)
			return -1;
		int n = 0;
		synchronized (layerBasket) {
			for (Layered o : layerBasket) {
				if (a.getClass().isInstance(o)) {
					if (o == a)
						return n;
					n++;
				}
			}
		}
		return -1;
	}

	public int getNumberOfInstances(Class c) {
		if (layerBasket.isEmpty())
			return 0;
		int n = 0;
		synchronized (layerBasket) {
			for (Layered l : layerBasket) {
				if (c.isInstance(l))
					n++;
			}
		}
		return n;
	}

	ModelComponent whichLayeredComponent(int x, int y) {
		if (layerBasket.isEmpty())
			return null;
		synchronized (layerBasket) {
			Layered o = null;
			int n = layerBasket.size();
			for (int i = n - 1; i >= 0; i--) {
				o = layerBasket.get(i);
				if (o instanceof ModelComponent) {
					ModelComponent t = (ModelComponent) o;
					if (t.contains(x, y)) {
						return t;
					}
				}
			}
		}
		return null;
	}

	public void setImageSelectionSet(BitSet bs) {
		if (layerBasket == null || layerBasket.isEmpty())
			return;
		getModel().setExclusiveSelection(false);
		ImageComponent[] im = getImages();
		for (int i = 0; i < im.length; i++)
			im[i].setSelected(bs == null ? false : bs.get(i));
	}

	public void addLayeredComponents(Layered[] a) {
		for (Layered l : a) {
			if (l != null)
				layerBasket.add(l);
		}
	}

	public void addLayeredComponent(Layered a) {
		if (!(a instanceof ModelComponent))
			return;
		((ModelComponent) a).setModel(getModel());
		layerBasket.add(a);
		getModel().notifyChange();
		if (!doNotFireUndoEvent) {
			((ModelComponent) a).setSelected(true);
			getModel().getUndoManager().undoableEditHappened(
					new UndoableEditEvent(getModel(), new UndoableLayeredComponentOperation(
							UndoAction.INSERT_LAYERED_COMPONENT, a)));
			updateUndoUIComponents();
		}
	}

	public void removeAllLayeredComponents() {
		layerBasket.clear();
	}

	public void removeLayeredComponent(Layered a) {
		layerBasket.remove(a);
		getModel().notifyChange();
		if (!doNotFireUndoEvent) {
			getModel().getUndoManager().undoableEditHappened(
					new UndoableEditEvent(getModel(), new UndoableLayeredComponentOperation(
							UndoAction.REMOVE_LAYERED_COMPONENT, a)));
			updateUndoUIComponents();
		}
	}

	public void removeAllSelectedLayeredComponents() {
		if (layerBasket.isEmpty())
			return;
		synchronized (layerBasket) {
			ModelComponent mc = null;
			for (Iterator it = layerBasket.iterator(); it.hasNext();) {
				mc = (ModelComponent) it.next();
				if (mc.isSelected())
					it.remove();
			}
		}
	}

	public void setTextBoxSelectionSet(BitSet bs) {
		if (layerBasket == null || layerBasket.isEmpty())
			return;
		getModel().setExclusiveSelection(false);
		TextBoxComponent[] t = getTextBoxes();
		for (int i = 0; i < t.length; i++)
			t[i].setSelected(bs == null ? false : bs.get(i));
	}

	public void setLineSelectionSet(BitSet bs) {
		if (layerBasket == null || layerBasket.isEmpty())
			return;
		getModel().setExclusiveSelection(false);
		LineComponent[] t = getLines();
		for (int i = 0; i < t.length; i++)
			t[i].setSelected(bs == null ? false : bs.get(i));
	}

	public void setRectangleSelectionSet(BitSet bs) {
		if (layerBasket == null || layerBasket.isEmpty())
			return;
		getModel().setExclusiveSelection(false);
		RectangleComponent[] t = getRectangles();
		for (int i = 0; i < t.length; i++)
			t[i].setSelected(bs == null ? false : bs.get(i));
	}

	public void setEllipseSelectionSet(BitSet bs) {
		if (layerBasket == null || layerBasket.isEmpty())
			return;
		getModel().setExclusiveSelection(false);
		EllipseComponent[] t = getEllipses();
		for (int i = 0; i < t.length; i++)
			t[i].setSelected(bs == null ? false : bs.get(i));
	}

	public void setTriangleSelectionSet(BitSet bs) {
		if (layerBasket == null || layerBasket.isEmpty())
			return;
		getModel().setExclusiveSelection(false);
		TriangleComponent[] t = getTriangles();
		for (int i = 0; i < t.length; i++)
			t[i].setSelected(bs == null ? false : bs.get(i));
	}

	public void destroyAllLayeredComponents() {
		if (layerBasket.isEmpty())
			return;
		synchronized (layerBasket) {
			for (Layered o : layerBasket) {
				if (o instanceof ModelComponent)
					((ModelComponent) o).destroy();
			}
		}
		layerBasket.clear();
	}

	public void setToolBar(JComponent tb) {
		toolBar = tb;
	}

	public JComponent getToolBar() {
		return toolBar;
	}

	public void setAncestor(Component c) {
		ancestor = c;
	}

	public Component getAncestor() {
		return ancestor;
	}

	public Map<String, Action> getSwitches() {
		return booleanSwitches;
	}

	public Map<String, Action> getChoices() {
		return multipleChoices;
	}

	public abstract void notifyNOPChange();

	public void clearSelection() {
		if (selectedComponent != null) {
			selectedComponent.setSelected(false);
			selectedComponent = null;
		}
	}

	/** disable/enable editing mode of this view */
	public void enableEditor(final boolean b) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (toolBar != null) {
					if (!b) {
						try {
							((AbstractButton) toolBar.getClientProperty("Select button")).doClick();
						}
						catch (Exception e) {
							System.err.println("No selection button was found.");
						}
					}
					for (int i = 0, n = toolBar.getComponentCount(); i < n; i++) {
						toolBar.getComponent(i).setEnabled(b);
					}
				}
			}
		});
		if (!b) {
			clearSelection();
			actionID = SELE_ID;
			resetUndoManager();
			repaint();
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				}
			});
		}
		doNotFireUndoEvent = !b;
	}

	void resetUndoManager() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				getModel().getUndoManager().discardAllEdits();
				if (undoUIComponent != null) {
					undoUIComponent.setEnabled(false);
					undoUIComponent.setText("Undo");
				}
				if (redoUIComponent != null) {
					redoUIComponent.setEnabled(false);
					redoUIComponent.setText("Redo");
				}
			}
		});
	}

	void updateUndoUIComponents() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (undoUIComponent != null) {
					undoUIComponent.setEnabled(getModel().getUndoManager().canUndo());
					undoUIComponent.setText(getModel().getUndoManager().getUndoPresentationName());
				}
				if (redoUIComponent != null) {
					redoUIComponent.setEnabled(getModel().getUndoManager().canRedo());
					redoUIComponent.setText(getModel().getUndoManager().getRedoPresentationName());
				}
			}
		});
	}

	abstract JPopupMenu[] getPopupMenus();

	/*
	 * return the popup menu associated with this view with the given label, if the popup menu has been initialized;
	 * return null if no popup menu with the given label is not found, or the popup menu with the given name has not
	 * been initialized yet. Each popup menu controls a specific object in the view. You may want to customize these
	 * menus. <p> For an <code>AtomisticView</code>, the labels for the popup menus are "Default", "Atom", "Radial
	 * Bond", "Angular Bond", "Molecule", "Molecular Surface", "Obstacle", "Image", and "Amino Acid", respectively. </p>
	 * <p> For a <code>MesoView</code>, the labels for the popup menus are "Default", "Gay-Berne" and "Image",
	 * respectively. </p>
	 */
	JPopupMenu getPopupMenu(String label) {
		JPopupMenu[] pm = getPopupMenus();
		for (JPopupMenu m : pm) {
			if (m == null)
				continue;
			if (label.equalsIgnoreCase(m.getLabel()))
				return m;
		}
		return null;
	}

	/**
	 * return the default popup menu. The default popup menu is the one that pops up when no object is clicked.
	 */
	public JPopupMenu getDefaultPopupMenu() {
		return getPopupMenu("default");
	}

	/**
	 * @param b
	 *            true if selection should also be cleared
	 */
	public void clearEditor(boolean b) {
		if (b)
			clearSelection();
		actionID = SELE_ID;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				firePropertyChange(UserAction.getName(actionID), false, true);
				if (toolBar == null)
					return;
				try {
					((AbstractButton) toolBar.getClientProperty("Select button")).doClick();
				}
				catch (Exception e) {
					System.err.println("No selection button was found.");
				}
			}
		});
	}

	public void setFillMode(FillMode fm) {
		fillMode = fm;
		if (fillMode == null || fillMode == FillMode.getNoFillMode()) {
			setBackground(Color.white);
			setBackgroundImage(null);
		}
		else if (fillMode instanceof FillMode.ColorFill) {
			setBackground(((FillMode.ColorFill) fillMode).getColor());
			setBackgroundImage(null);
		}
		else if (fillMode instanceof FillMode.ImageFill) {
			String codeBase = FileUtilities.getCodeBase((String) getModel().getProperty("url"));
			if (codeBase != null) {
				String fileName = FileUtilities.getFileName(((FillMode.ImageFill) fillMode).getURL());
				if (fileName != null) {
					String s = codeBase + fileName;
					ImageIcon icon = null;
					if (MDContainer.isApplet() || FileUtilities.isRemote(s)) {
						try {
							icon = ConnectionManager.sharedInstance().loadImage(new URL(FileUtilities.httpEncode(s)));
						}
						catch (MalformedURLException e) {
							e.printStackTrace();
							setBackgroundImage(null);
							return;
						}
					}
					else {
						File f1 = new File(s);
						if (!f1.exists()) {
							/*
							 * if a copy of the background image does not exist in the model's directory, create one
							 */
							String u1 = ((FillMode.ImageFill) fillMode).getURL();
							if (!u1.equals(f1.toString())) {
								ModelerUtilities.copyResourceToDirectory(u1, f1.getParentFile());
							}
						}
						/*
						 * For using ImageIcon, we are not supposed to change the source dynamically. When we do need to
						 * do so, MUST use the Toolkit method to create a new image and pass it to the ImageIcon. That
						 * is the only way to refresh an ImageIcon's content. This is presumably due to the fact that
						 * the MediaTracker used to load image in the ImageIcon class is static (shared by all live
						 * ImageIcon instances). MediaTracker might look for what it holds in memory first instead of
						 * loading from the HD or Web. In the latter case, this may not be a problem because the source
						 * is usually stable. So we normally do not recreate an ImageIcon from the Web.
						 */
						icon = new ImageIcon(Toolkit.getDefaultToolkit().createImage(s));
					}
					icon.setDescription(fileName);
					setBackgroundImage(icon);
				}
				else {
					setBackgroundImage(null);
				}
			}
			else {
				setBackgroundImage(null);
			}
		}
	}

	public FillMode getFillMode() {
		return fillMode;
	}

	public void setBackgroundImage(ImageIcon icon) {
		if (icon == null) {
			backgroundImage = null;
			repaint();
			return;
		}
		backgroundImage = new ImageIcon(icon.getImage());
		backgroundImage.setDescription(icon.getDescription());
		if (fillMode instanceof FillMode.ImageFill) {
			((FillMode.ImageFill) fillMode).setURL(FileUtilities.getFileName(icon.getDescription()));
		}
		repaint();
	}

	public void loadImageComponents(String parent, ImageComponent.Delegate[] icd) {
		if (parent == null || icd == null)
			return;
		Map<String, ImageComponent> map = new HashMap<String, ImageComponent>();
		String s = null;
		ImageComponent ic = null;
		for (ImageComponent.Delegate i : icd) {
			s = FileUtilities.getCodeBase(parent) + XMLCharacterDecoder.decode(FileUtilities.getFileName(i.getURI()));
			ic = null;
			try {
				if (map.containsKey(s)) {
					ic = new ImageComponent(map.get(s));
				}
				else {
					ic = new ImageComponent(s);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				break;
			}
			map.put(s, ic);
			ic.setModel(getModel());
			ic.set(i);
			layerBasket.add(ic);
		}
	}

	public void loadTextBoxComponents(TextBoxComponent.Delegate[] tbd) {
		if (tbd == null)
			return;
		for (TextBoxComponent.Delegate i : tbd) {
			TextBoxComponent t = new TextBoxComponent();
			t.setModel(getModel());
			t.set(i);
			layerBasket.add(t);
		}
	}

	public void loadLineComponents(LineComponent.Delegate[] lcd) {
		if (lcd == null)
			return;
		for (LineComponent.Delegate i : lcd) {
			if (i.getX12() == 0 && i.getY12() == 0)
				continue;
			LineComponent l = new LineComponent();
			l.setModel(getModel());
			l.set(i);
			layerBasket.add(l);
		}
	}

	public void loadRectangleComponents(RectangleComponent.Delegate[] rcd) {
		if (rcd == null)
			return;
		for (RectangleComponent.Delegate i : rcd) {
			if (i.getWidth() <= 0 || i.getHeight() <= 0)
				continue;
			RectangleComponent r = new RectangleComponent();
			r.setModel(getModel());
			r.set(i);
			layerBasket.add(r);
		}
	}

	public void loadEllipseComponents(EllipseComponent.Delegate[] rcd) {
		if (rcd == null)
			return;
		for (EllipseComponent.Delegate i : rcd) {
			if (i.getWidth() <= 0 || i.getHeight() <= 0)
				continue;
			EllipseComponent r = new EllipseComponent();
			r.setModel(getModel());
			r.set(i);
			layerBasket.add(r);
		}
	}

	public void loadTriangleComponents(TriangleComponent.Delegate[] tcd) {
		if (tcd == null)
			return;
		for (TriangleComponent.Delegate i : tcd) {
			TriangleComponent t = new TriangleComponent();
			t.setModel(getModel());
			t.set(i);
			layerBasket.add(t);
		}
	}

	/** return the 24-bit color that is in contrast to the background. */
	public Color contrastBackground() {
		int i = 0xffffff ^ getBackground().getRGB();
		if (contrastBgColor == null || i != contrastBgColor.getRGB())
			contrastBgColor = new Color(i);
		return contrastBgColor;
	}

	/**
	 * return the color that is in contrast to the background with the given alpha value
	 */
	public Color contrastBackground(int alpha) {
		return new Color((alpha << 24) | 0xffffffff ^ getBackground().getRGB());
	}

	void showActionTip(String msg, int x, int y) {
		if (!actionTipEnabled && actionID != WHAT_ID)
			return;
		showTip(msg, x, y, 1000);
	}

	void showTip(String msg, int x, int y, int time) {
		if (tipPopupMenu == null) {
			tipPopupMenu = new JPopupMenu("Tip");
			tipPopupMenu.setBorder(BorderFactory.createLineBorder(Color.black));
			tipPopupMenu.setBackground(SystemColor.info);
			JLabel l = new JLabel(msg);
			l.setFont(ViewAttribute.SMALL_FONT);
			l.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
			tipPopupMenu.add(l);
		}
		else {
			((JLabel) tipPopupMenu.getComponent(0)).setText(msg);
		}
		tipPopupMenu.show(this, x, y);
		if (time > 0) {
			Timer timer = new Timer(time, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					tipPopupMenu.setVisible(false);
				}
			});
			timer.setRepeats(false);
			timer.setInitialDelay(time);
			timer.start();
		}
	}

	public void setClockPainted(boolean b) {
		clockPainted = b;
	}

	public boolean getClockPainted() {
		return clockPainted;
	}

	public abstract void resetAddObjectIndicator();

	abstract int getSteeringForceScale();

	void paintSteering(Graphics2D g) {
		if (UserField.isRenderable()) {
			int x = getSteeringForceScale();
			if (x != 0) {
				if (steeringForceController == null)
					steeringForceController = new SteeringForceController();
				steeringForceController.setCurrentReading(x);
				steeringForceController.paint(g);
			}
			else {
				if (steeringForceController != null)
					steeringForceController.setCurrentReading(0);
			}
		}
	}

	void paintInfo(Graphics g) {
		if (getModel().isActionScriptRunning()) {
			if (waitIcon == null)
				waitIcon = new ImageIcon(getClass().getResource("images/wait.gif"));
			waitIcon.paintIcon(this, g, getWidth() - waitIcon.getIconWidth() - 5, getHeight()
					- waitIcon.getIconHeight() - 5);
		}
		if (showClock) {
			if (clockIcon == null)
				clockIcon = new ImageIcon(getClass().getResource("images/clock.gif"));
			clockIcon.paintIcon(this, g, 5, getHeight() - clockIcon.getIconHeight() - 5);
			g.setColor(contrastBackground());
			g.setFont(ViewAttribute.SMALL_FONT);
			synchronized (getModel()) {
				g.drawString((int) getModel().getModelTime() + " fs", 25, getHeight() - 5);
			}
		}
	}

	void paintPleaseWait(Graphics g) {
		g.setFont(ViewAttribute.FONT_BOLD_18);
		String s = getInternationalText("PleaseWait");
		if (s == null)
			s = "Loading model, please wait...";
		FontMetrics fm = g.getFontMetrics();
		g.drawString(s, (getWidth() - fm.stringWidth(s)) >> 1, (getHeight() - fm.getHeight()) >> 1);
		g.setFont(ViewAttribute.FONT_BOLD_15);
		fm = g.getFontMetrics();
		s = "2D Simulator, Molecular Workbench";
		g.drawString(s, (getWidth() - fm.stringWidth(s)) >> 1, (getHeight() >> 1) + fm.getHeight());
	}

	/** this method being public is a side effect of interface implementation */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		update(g);
	}

	/** this method being public is a side effect of interface implementation */
	public void update(Graphics g) {
		synchronized (updateLock) {
			update2(g);
		}
	}

	/** return the lock to block the update(Graphics g) method. */
	public Object getUpdateLock() {
		return updateLock;
	}

	void update2(Graphics g) {

		Graphics2D g2 = (Graphics2D) g;
		if ((renderingMethod & SPEED_RENDERING) == SPEED_RENDERING) {
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
		}
		else {
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		}
		if ((renderingMethod & ANTIALIASING_OFF) == ANTIALIASING_OFF) {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		}
		else {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}

		g2.setColor(getBackground());
		g2.fillRect(0, 0, getWidth(), getHeight());
		if (backgroundImage != null) {
			int imax = getWidth() / backgroundImage.getIconWidth() + 1;
			int jmax = getHeight() / backgroundImage.getIconHeight() + 1;
			for (int i = 0; i < imax; i++) {
				for (int j = 0; j < jmax; j++) {
					if (backgroundImage != null)
						backgroundImage.paintIcon(null, g2, i * backgroundImage.getIconWidth(), j
								* backgroundImage.getIconHeight());
				}
			}
		}
		if (fillMode instanceof FillMode.GradientFill) {
			FillMode.GradientFill gfm = (FillMode.GradientFill) fillMode;
			GradientFactory.paintRect(g2, gfm.getStyle(), gfm.getVariant(), gfm.getColor1(), gfm.getColor2(), 0, 0,
					getWidth(), getHeight());
		}
		else if (fillMode instanceof FillMode.PatternFill) {
			g2.setPaint(((FillMode.PatternFill) fillMode).getPaint());
			g2.fillRect(0, 0, getWidth(), getHeight());
		}

		paintLayeredComponents(g, Layered.BEHIND_PARTICLES);

	}

	void paintShapeDrawing(Graphics g) {
		switch (actionID) {
		case LINE_ID:
			if (dragging && mousePressedPoint != null) {
				g.setColor(contrastBackground());
				g.drawLine(mousePressedPoint.x, mousePressedPoint.y, dragPoint.x, dragPoint.y);
			}
			break;
		case RECT_ID:
			g.drawRect(selectedArea.x, selectedArea.y, selectedArea.width, selectedArea.height);
			break;
		case TRIA_ID:
			g.drawLine(selectedArea.x + selectedArea.width / 2, selectedArea.y, selectedArea.x, selectedArea.y
					+ selectedArea.height);
			g.drawLine(selectedArea.x + selectedArea.width / 2, selectedArea.y, selectedArea.x + selectedArea.width,
					selectedArea.y + selectedArea.height);
			g.drawLine(selectedArea.x, selectedArea.y + selectedArea.height, selectedArea.x + selectedArea.width,
					selectedArea.y + selectedArea.height);
			break;
		case ELLI_ID:
			g.drawOval(selectedArea.x, selectedArea.y, selectedArea.width, selectedArea.height);
			break;
		}
	}

	public void setShowSelectionHalo(boolean b) {
		showSelectionHalo = b;
		repaint();
	}

	public boolean getShowSelectionHalo() {
		return showSelectionHalo;
	}

	public void setShowClock(boolean b) {
		if (showClock == b)
			return;
		showClock = b;
		getModel().notifyChange();
		repaint();
	}

	public boolean getShowClock() {
		return showClock;
	}

	public void setShowHeatBath(boolean b) {
		if (showHeatBath == b)
			return;
		showHeatBath = b;
		getModel().notifyChange();
		repaint();
	}

	public boolean getShowHeatBath() {
		return showHeatBath;
	}

	public void setShowMirrorImages(boolean b) {
		if (showMirrorImages == b)
			return;
		showMirrorImages = b;
		getModel().notifyChange();
		repaint();
	}

	public boolean getShowMirrorImages() {
		return showMirrorImages;
	}

	public void setShowParticleIndex(boolean b) {
		if (showParticleIndex == b)
			return;
		showParticleIndex = b;
		getModel().notifyChange();
		repaint();
	}

	public boolean getShowParticleIndex() {
		return showParticleIndex;
	}

	public void setDrawCharge(boolean b) {
		if (drawCharge == b)
			return;
		drawCharge = b;
		getModel().notifyChange();
		repaint();
	}

	public boolean getDrawCharge() {
		return drawCharge;
	}

	public void setDrawExternalForce(boolean b) {
		if (drawExternalForce == b)
			return;
		drawExternalForce = b;
		getModel().notifyChange();
		repaint();
	}

	public boolean getDrawExternalForce() {
		return drawExternalForce;
	}

	public void setVelocityFlavor(VectorFlavor vf) {
		velocityFlavor = vf;
		if (vf.getWidth() != 1.0f || vf.getStyle() != LineStyle.STROKE_NUMBER_1) {
			velocityFlavor.setStroke(StrokeFactory.createStroke(vf.getWidth(), LineStyle.STROKES[vf.getStyle()]
					.getDashArray()));
		}
	}

	public VectorFlavor getVelocityFlavor() {
		return velocityFlavor;
	}

	public void setMomentumFlavor(VectorFlavor vf) {
		momentumFlavor = vf;
		if (vf.getWidth() != 1.0f || vf.getStyle() != LineStyle.STROKE_NUMBER_1) {
			momentumFlavor.setStroke(StrokeFactory.createStroke(vf.getWidth(), LineStyle.STROKES[vf.getStyle()]
					.getDashArray()));
		}
	}

	public VectorFlavor getMomentumFlavor() {
		return momentumFlavor;
	}

	public void setAccelerationFlavor(VectorFlavor vf) {
		accelerationFlavor = vf;
		if (vf.getWidth() != 1.0f || vf.getStyle() != LineStyle.STROKE_NUMBER_1) {
			accelerationFlavor.setStroke(StrokeFactory.createStroke(vf.getWidth(), LineStyle.STROKES[vf.getStyle()]
					.getDashArray()));
		}
	}

	public VectorFlavor getAccelerationFlavor() {
		return accelerationFlavor;
	}

	public void setForceFlavor(VectorFlavor vf) {
		forceFlavor = vf;
		if (vf.getWidth() != 1.0f || vf.getStyle() != LineStyle.STROKE_NUMBER_1) {
			forceFlavor.setStroke(StrokeFactory.createStroke(vf.getWidth(), LineStyle.STROKES[vf.getStyle()]
					.getDashArray()));
		}
	}

	public VectorFlavor getForceFlavor() {
		return forceFlavor;
	}

	/** override JComponent.setSize(int w, int h) */
	public void setSize(int width, int height) {
		setSize(new Dimension(width, height));
	}

	/** override JComponent.setSize(Dimension d) */
	public void setSize(Dimension d) {
		Dimension d2 = resize(d, false);
		boundary.setView(new Rectangle(0, 0, d2.width, d2.height));
	}

	/** override JComponent.setPreferredSize(int w, int h) */
	public void setPreferredSize(int w, int h) {
		setPreferredSize(new Dimension(w, h));
	}

	/** override JComponent.setPreferredSize(Dimension d) */
	public void setPreferredSize(Dimension d) {
		super.setPreferredSize(d);
		boundary.setView(new Rectangle(0, 0, d.width, d.height));
	}

	public void enablePopupMenu(boolean b) {
		popupMenuEnabled = b;
	}

	public boolean popupMenuEnabled() {
		return popupMenuEnabled;
	}

	public int getBoundaryType() {
		return boundary.getType();
	}

	public void setBoundaryType(int i) {
		boundary.setType(i);
	}

	public RectangularBoundary getBoundary() {
		return boundary;
	}

	public void setBoundary(RectangularBoundary b) {
		boundary = b;
	}

	public void setExternalCursor(Cursor cursor) {
		externalCursor = cursor;
	}

	public void setCursor(Cursor cursor) {
		super.setCursor(externalCursor == null ? cursor : externalCursor);
	}

	public void setEnergizer(boolean b) {
		energizerOn = b;
		if (b) {
			if (energizer == null)
				energizer = new Energizer(getWidth() - 18, 20, 100);
		}
		else {
			energizerButtonPressed = false;
		}
		repaint();
	}

	public boolean getEnergizer() {
		return energizerOn;
	}

	void processMousePressed(MouseEvent e) {
		requestFocusInWindow();
		mouseHeldX = e.getX();
		mouseHeldY = e.getY();
		getModel().runMouseScript(MouseEvent.MOUSE_PRESSED, mouseHeldX, mouseHeldY);
		mousePressedTime = System.currentTimeMillis();
		if (mousePressedPoint == null) {
			mousePressedPoint = new Point(mouseHeldX, mouseHeldY);
		}
		else {
			mousePressedPoint.x = mouseHeldX;
			mousePressedPoint.y = mouseHeldY;
		}
		if (steeringForceController != null && steeringForceController.currentReading != 0) {
			if (steeringForceController.incrButton.contains(mouseHeldX, mouseHeldY)) {
				steeringForceController.increase();
			}
			else if (steeringForceController.decrButton.contains(mouseHeldX, mouseHeldY)) {
				steeringForceController.decrease();
			}
		}
		if (energizerOn && energizer != null) {
			if (energizer.heatButton.contains(mouseHeldX, mouseHeldY)) {
				if (selectedComponent != null) {
					selectedComponent.setSelected(false);
					selectedComponent = null;
				}
				energizerButtonPressed = true;
				Thread t = new Thread(new Runnable() {
					public void run() {
						while (energizerButtonPressed && getModel().getTemperature() < 100000) {
							energizer.heat();
							repaint();
							try {
								Thread.sleep(50);
							}
							catch (InterruptedException e) {
								energizerButtonPressed = false;
								return;
							}
						}
						getModel().notifyChange();
					}
				});
				t.setName("Quick Heater");
				t.setPriority(Thread.NORM_PRIORITY);
				t.start();
			}
			else if (energizer.coolButton.contains(mouseHeldX, mouseHeldY)) {
				if (selectedComponent != null) {
					selectedComponent.setSelected(false);
					selectedComponent = null;
				}
				energizerButtonPressed = true;
				Thread t = new Thread(new Runnable() {
					public void run() {
						while (energizerButtonPressed) {
							energizer.cool();
							repaint();
							try {
								Thread.sleep(50);
							}
							catch (InterruptedException e) {
								energizerButtonPressed = false;
								return;
							}
						}
						getModel().notifyChange();
					}
				});
				t.setName("Quick Cooler");
				t.setPriority(Thread.NORM_PRIORITY);
				t.start();
			}
			else if (energizer.exitButton.contains(mouseHeldX, mouseHeldY)) {
				energizer.exit();
				energizerButtonPressed = false;
			}
		}
	}

	boolean callOutMousePressed(int x, int y) {
		if (selectedComponent instanceof TextBoxComponent) {
			boolean b = ((TextBoxComponent) selectedComponent).nearCallOutPoint(x, y);
			((TextBoxComponent) selectedComponent).setChangingCallOut(b);
			return b;
		}
		return false;
	}

	private void setAnchorPointForRectangularShape(byte i, float x, float y, float w, float h) {
		switch (i) {
		case RectangleComponent.UPPER_LEFT:
			anchorPoint.setLocation(x + w, y + h);
			break;
		case RectangleComponent.UPPER_RIGHT:
			anchorPoint.setLocation(x, y + h);
			break;
		case RectangleComponent.LOWER_RIGHT:
			anchorPoint.setLocation(x, y);
			break;
		case RectangleComponent.LOWER_LEFT:
			anchorPoint.setLocation(x + w, y);
			break;
		case RectangleComponent.TOP:
			anchorPoint.setLocation(x, y + h);
			break;
		case RectangleComponent.RIGHT:
			anchorPoint.setLocation(x, y);
			break;
		case RectangleComponent.BOTTOM:
			anchorPoint.setLocation(x, y);
			break;
		case RectangleComponent.LEFT:
			anchorPoint.setLocation(x + w, y);
			break;
		}
	}

	boolean handleMousePressed(int x, int y) {
		if (selectedComponent instanceof LineComponent) {
			LineComponent lc = (LineComponent) selectedComponent;
			int i = lc.nearEndPoint(x, y);
			lc.setSelectedEndPoint(i);
			lc.storeCurrentState();
			return i > 0;
		}
		else if (selectedComponent instanceof RectangleComponent) {
			RectangleComponent rc = (RectangleComponent) selectedComponent;
			byte i = rc.nearHandle(x, y);
			rc.setSelectedHandle(i);
			setAnchorPointForRectangularShape(i, rc.getX(), rc.getY(), rc.getWidth(), rc.getHeight());
			if (i >= 0)
				rc.storeCurrentState();
			return i >= 0;
		}
		else if (selectedComponent instanceof EllipseComponent) {
			EllipseComponent ec = (EllipseComponent) selectedComponent;
			byte i = ec.nearHandle(x, y);
			ec.setSelectedHandle(i);
			setAnchorPointForRectangularShape(i, ec.getX(), ec.getY(), ec.getWidth(), ec.getHeight());
			if (i >= 0)
				ec.storeCurrentState();
			return i >= 0;
		}
		else if (selectedComponent instanceof TriangleComponent) {
			TriangleComponent tc = (TriangleComponent) selectedComponent;
			byte i = tc.nearHandle(x, y);
			tc.setSelectedHandle(i);
			Rectangle rt = tc.getBounds();
			setAnchorPointForRectangularShape(i, rt.x, rt.y, rt.width, rt.height);
			if (i >= 0)
				tc.storeCurrentState();
			return i >= 0;
		}
		return false;
	}

	void processMouseEntered(MouseEvent e) {
		getModel().runMouseScript(MouseEvent.MOUSE_ENTERED, e.getX(), e.getY());
	}

	void processMouseExited(MouseEvent e) {
		getModel().runMouseScript(MouseEvent.MOUSE_EXITED, e.getX(), e.getY());
	}

	void processMouseDragged(MouseEvent e) {
		mouseHeldX = e.getX();
		mouseHeldY = e.getY();
		mousePressedTime = System.currentTimeMillis();
		switch (actionID) {
		case LINE_ID:
			dragging = true;
			dragPoint.setLocation(mouseHeldX, mouseHeldY);
			repaint();
			break;
		}
	}

	void processMouseMoved(MouseEvent e) {
		if (!hasFocus())
			return;
		int x = e.getX();
		int y = e.getY();
		getModel().runMouseScript(MouseEvent.MOUSE_MOVED, x, y);
		if (energizerOn) {
			if (energizer != null) {
				if (x >= energizer.x) {
					energizer.mouseEntered(x, y);
				}
				else {
					energizer.mouseExited();
				}
				energizer.paint((Graphics2D) getGraphics());
			}
		}
		if (steeringForceController != null && steeringForceController.currentReading != 0) {
			if (y >= steeringForceController.incrButton.y)
				steeringForceController.mouseEntered(x, y);
			else steeringForceController.mouseExited();
			steeringForceController.paint((Graphics2D) getGraphics());
		}
	}

	boolean callOutMouseMoved(int x, int y) {
		if (selectedComponent instanceof TextBoxComponent) {
			if (((TextBoxComponent) selectedComponent).nearCallOutPoint(x, y)) {
				setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				return true;
			}
			setCursor(UserAction.getCursor(actionID));
		}
		return false;
	}

	boolean handleMouseMoved(int x, int y) {
		if (selectedComponent instanceof LineComponent) {
			if (((LineComponent) selectedComponent).nearEndPoint(x, y) > 0) {
				setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				return true;
			}
		}
		else if (selectedComponent instanceof RectangleComponent) {
			if (Draw.selectHandleCursorForRectangularShape(this, ((RectangleComponent) selectedComponent).nearHandle(x,
					y)))
				return true;
		}
		else if (selectedComponent instanceof EllipseComponent) {
			if (Draw.selectHandleCursorForRectangularShape(this, ((EllipseComponent) selectedComponent)
					.nearHandle(x, y)))
				return true;
		}
		else if (selectedComponent instanceof TriangleComponent) {
			if (((TriangleComponent) selectedComponent).nearHandle(x, y) != -1) {
				setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				return true;
			}
		}
		setCursor(UserAction.getCursor(actionID));
		return false;
	}

	boolean callOutMouseDragged(int x, int y) {
		if (selectedComponent instanceof TextBoxComponent) {
			if (isEditable() || selectedComponent.isDraggable()) {
				if (((TextBoxComponent) selectedComponent).isChangingCallOut()) {
					((TextBoxComponent) selectedComponent).setCallOutLocation(x, y);
					repaint();
					return true;
				}
			}
		}
		return false;
	}

	boolean handleMouseDragged(int x, int y) {
		if (!isEditable() && selectedComponent != null && !selectedComponent.isDraggable())
			return false;
		if (selectedComponent instanceof LineComponent) {
			LineComponent lc = (LineComponent) selectedComponent;
			boolean b = false;
			switch (lc.getSelectedEndPoint()) {
			case 1:
				lc.setEndPoint1(x, y);
				b = true;
				break;
			case 2:
				lc.setEndPoint2(x, y);
				b = true;
				break;
			}
			if (b) {
				repaint();
				getModel().notifyChange();
				if (!doNotFireUndoEvent) {
					getModel().getUndoManager().undoableEditHappened(
							new UndoableEditEvent(getModel(), new UndoableLayeredComponentOperation(
									UndoAction.RESIZE_LINE, lc)));
					updateUndoUIComponents();
				}
				return true;
			}
		}
		else if (selectedComponent instanceof RectangleComponent) {
			RectangleComponent rc = (RectangleComponent) selectedComponent;
			if (rc.getSelectedHandle() >= 0) {
				switch (rc.getSelectedHandle()) {
				case RectangleComponent.LOWER_LEFT:
				case RectangleComponent.LOWER_RIGHT:
				case RectangleComponent.UPPER_LEFT:
				case RectangleComponent.UPPER_RIGHT:
					rc.setRect(Math.min(x, anchorPoint.x), Math.min(y, anchorPoint.y), Math.abs(x - anchorPoint.x),
							Math.abs(y - anchorPoint.y));
					break;
				case RectangleComponent.TOP:
				case RectangleComponent.BOTTOM:
					rc.setY(Math.min(y, anchorPoint.y));
					rc.setHeight(Math.abs(y - anchorPoint.y));
					break;
				case RectangleComponent.LEFT:
				case RectangleComponent.RIGHT:
					rc.setX(Math.min(x, anchorPoint.x));
					rc.setWidth(Math.abs(x - anchorPoint.x));
					break;
				case RectangleComponent.ARC_HANDLE:
					float arc = x - rc.getX();
					if (arc < 0)
						arc = 0;
					else arc = Math.min(arc, 0.5f * Math.min(rc.getWidth(), rc.getHeight()));
					rc.setArcWidth(2 * arc);
					rc.setArcHeight(2 * arc);
					break;
				}
				repaint();
				getModel().notifyChange();
				if (!doNotFireUndoEvent) {
					getModel().getUndoManager().undoableEditHappened(
							new UndoableEditEvent(getModel(), new UndoableLayeredComponentOperation(
									UndoAction.RESIZE_RECTANGLE, rc)));
					updateUndoUIComponents();
				}
				return true;
			}
		}
		else if (selectedComponent instanceof EllipseComponent) {
			EllipseComponent ec = (EllipseComponent) selectedComponent;
			if (ec.getSelectedHandle() >= 0) {
				switch (ec.getSelectedHandle()) {
				case RectangleComponent.LOWER_LEFT:
				case RectangleComponent.LOWER_RIGHT:
				case RectangleComponent.UPPER_LEFT:
				case RectangleComponent.UPPER_RIGHT:
					ec.setOval(Math.min(x, anchorPoint.x), Math.min(y, anchorPoint.y), Math.abs(x - anchorPoint.x),
							Math.abs(y - anchorPoint.y));
					break;
				case RectangleComponent.TOP:
				case RectangleComponent.BOTTOM:
					ec.setY(Math.min(y, anchorPoint.y));
					ec.setHeight(Math.abs(y - anchorPoint.y));
					break;
				case RectangleComponent.LEFT:
				case RectangleComponent.RIGHT:
					ec.setX(Math.min(x, anchorPoint.x));
					ec.setWidth(Math.abs(x - anchorPoint.x));
					break;
				}
				repaint();
				getModel().notifyChange();
				if (!doNotFireUndoEvent) {
					getModel().getUndoManager().undoableEditHappened(
							new UndoableEditEvent(getModel(), new UndoableLayeredComponentOperation(
									UndoAction.RESIZE_ELLIPSE, ec)));
					updateUndoUIComponents();
				}
				return true;
			}
		}
		else if (selectedComponent instanceof TriangleComponent) {
			TriangleComponent tc = (TriangleComponent) selectedComponent;
			byte i = tc.getSelectedHandle();
			if (i != -1) {
				tc.setVertex(i, x, y);
				repaint();
				getModel().notifyChange();
				if (!doNotFireUndoEvent) {
					getModel().getUndoManager().undoableEditHappened(
							new UndoableEditEvent(getModel(), new UndoableLayeredComponentOperation(
									UndoAction.RESIZE_TRIANGLE, tc)));
					updateUndoUIComponents();
				}
				return true;
			}
		}
		return false;
	}

	void processMouseReleased(MouseEvent e) {
		getModel().runMouseScript(MouseEvent.MOUSE_RELEASED, e.getX(), e.getY());
		if (energizerOn) {
			energizerButtonPressed = false;
		}
		if (selectedComponent instanceof TextBoxComponent) {
			((TextBoxComponent) selectedComponent).setChangingCallOut(false);
		}
		switch (actionID) {
		case WHAT_ID:
			if (tipPopupMenu != null)
				tipPopupMenu.setVisible(false);
			break;
		case LINE_ID:
			if (dragging) {
				if (selectedComponent != null)
					selectedComponent.setSelected(false);
				LineComponent lc = new LineComponent();
				lc.setLine(mousePressedPoint.x, mousePressedPoint.y, dragPoint.x, dragPoint.y);
				addLayeredComponent(lc);
				repaint();
				dragging = false;
			}
			break;
		case RECT_ID:
			if (selectedArea.width > 0 && selectedArea.height > 0) {
				if (selectedComponent != null)
					selectedComponent.setSelected(false);
				RectangleComponent rc = new RectangleComponent();
				rc.setRect(selectedArea.x, selectedArea.y, selectedArea.width, selectedArea.height);
				addLayeredComponent(rc);
				selectedArea.setSize(0, 0);
				repaint();
			}
			break;
		case ELLI_ID:
			if (selectedArea.width > 0 && selectedArea.height > 0) {
				if (selectedComponent != null)
					selectedComponent.setSelected(false);
				EllipseComponent ec = new EllipseComponent();
				ec.setOval(selectedArea.x, selectedArea.y, selectedArea.width, selectedArea.height);
				addLayeredComponent(ec);
				selectedArea.setSize(0, 0);
				repaint();
			}
			break;
		case TRIA_ID:
			if (selectedArea.width > 0 && selectedArea.height > 0) {
				if (selectedComponent != null)
					selectedComponent.setSelected(false);
				TriangleComponent tc = new TriangleComponent();
				tc.setVertex(0, selectedArea.x + selectedArea.width / 2, selectedArea.y);
				tc.setVertex(1, selectedArea.x, selectedArea.y + selectedArea.height);
				tc.setVertex(2, selectedArea.x + selectedArea.width, selectedArea.y + selectedArea.height);
				addLayeredComponent(tc);
				selectedArea.setSize(0, 0);
				repaint();
			}
			break;
		}
	}

	void processKeyPressed(KeyEvent e) {
		getModel().runKeyScript(KeyEvent.KEY_PRESSED, e.getKeyCode());
		switch (e.getKeyChar()) {
		case 'z':
			if (energizerOn)
				getModel().changeTemperature(0.1);
			break;
		case 'x':
			if (energizerOn)
				getModel().changeTemperature(-0.1);
			break;
		case 'a':
		case 'A':
			changeUserField(true);
			break;
		case 'd':
		case 'D':
			changeUserField(false);
			break;
		}
	}

	void processKeyReleased(KeyEvent e) {
		getModel().runKeyScript(KeyEvent.KEY_RELEASED, e.getKeyCode());
		boolean b = false;
		switch (e.getKeyCode()) {
		case KeyEvent.VK_UP:
			keyPressedCode = keyPressedCode ^ UP_PRESSED;
			b = true;
			break;
		case KeyEvent.VK_DOWN:
			keyPressedCode = keyPressedCode ^ DOWN_PRESSED;
			b = true;
			break;
		case KeyEvent.VK_LEFT:
			keyPressedCode = keyPressedCode ^ LEFT_PRESSED;
			b = true;
			break;
		case KeyEvent.VK_RIGHT:
			keyPressedCode = keyPressedCode ^ RIGHT_PRESSED;
			b = true;
			break;
		}
		if (b) {
			processUserFieldsUponKeyOrMouseReleased();
		}
		if (getModel().getJob() == null || getModel().getJob().isStopped())
			repaint();
		// MUST not consume this event and leave this to the key binding to work. As a result, key binding
		// must set KeyStroke with onKeyRelease flag set to true.
		// if(hasFocus()) e.consume();
	}

	private void changeUserField(boolean increase) {
		UserField uf = null;
		for (int i = 0, n = getModel().getNumberOfParticles(); i < n; i++) {
			uf = getModel().getParticle(i).getUserField();
			if (uf != null) {
				if (increase)
					uf.increaseGear(1);
				else uf.decreaseGear(1);
				if (Math.abs(uf.getIntensity()) > ZERO) { // currently steering
					uf.setIntensity(UserField.INCREMENT * uf.getGear());
				}
			}
		}
		ObstacleCollection oc = getModel().getObstacles();
		if (oc != null && !oc.isEmpty()) {
			synchronized (oc.getSynchronizationLock()) {
				for (Iterator it = oc.iterator(); it.hasNext();) {
					uf = ((RectangularObstacle) it.next()).getUserField();
					if (uf != null) {
						if (increase)
							uf.increaseGear(1);
						else uf.decreaseGear(1);
						if (Math.abs(uf.getIntensity()) > ZERO) {// currently steering
							uf.setIntensity(UserField.INCREMENT * uf.getGear());
						}
					}
				}
			}
		}
	}

	// dx and dy are mouse position increments
	void steerParticleUsingMouse(int dx, int dy) {
		UserField uf = null;
		int n = getModel().getNumberOfParticles();
		for (int i = 0; i < n; i++) {
			uf = getModel().getParticle(i).getUserField();
			if (uf != null) {
				double r = 1.0 / Math.hypot(dx, dy);
				uf.setAngle(dx * r, dy * r);
				switch (uf.getMode()) {
				case UserField.FORCE_MODE:
					uf.setIntensity(UserField.INCREMENT * uf.getGear());
					break;
				case UserField.IMPULSE1_MODE:
					uf.increaseGear(0.1f);
					break;
				}
			}
		}
	}

	void processUserFieldsUponKeyOrMouseReleased() {
		UserField uf = null;
		Particle p = null;
		for (int i = 0, n = getModel().getNumberOfParticles(); i < n; i++) {
			p = getModel().getParticle(i);
			uf = p.getUserField();
			if (uf != null) {
				switch (uf.getMode()) {
				case UserField.FORCE_MODE:
					uf.setIntensity(0.0);
					break;
				case UserField.IMPULSE1_MODE:
					uf.addImpulse(p);
					uf.setGear(1);
					break;
				case UserField.IMPULSE2_MODE:
					uf.addImpulse(p);
					break;
				}
			}
		}
		ObstacleCollection oc = getModel().getObstacles();
		if (oc != null && !oc.isEmpty()) {
			RectangularObstacle obs = null;
			synchronized (oc.getSynchronizationLock()) {
				for (Iterator it = oc.iterator(); it.hasNext();) {
					obs = (RectangularObstacle) it.next();
					uf = obs.getUserField();
					if (uf != null) {
						switch (uf.getMode()) {
						case UserField.FORCE_MODE:
							uf.setIntensity(0.0);
							break;
						case UserField.IMPULSE1_MODE:
							uf.addImpulse(obs);
							uf.setGear(1);
							break;
						case UserField.IMPULSE2_MODE:
							uf.addImpulse(obs);
							break;
						}
					}
				}
			}
		}
		refreshForces();
	}

	abstract void refreshForces();

	byte showFrictionOptions(boolean on) {
		return new SteeringDialog(this).show(on);
	}

	public void removeSelectedComponent() {
		pasteBuffer = null;
		if (selectedComponent instanceof Layered) {
			removeLayeredComponent((Layered) selectedComponent);
			copySelectedComponent();
		}
	}

	protected void copySelectedComponent() {
		if (selectedComponent instanceof ImageComponent) {
			try {
				pasteBuffer = new ImageComponent(((ImageComponent) selectedComponent).toString());
			}
			catch (Exception e) {
				// ignore
			}
		}
		else if (selectedComponent instanceof TextBoxComponent) {
			pasteBuffer = new TextBoxComponent((TextBoxComponent) selectedComponent);
		}
		else if (selectedComponent instanceof LineComponent) {
			pasteBuffer = new LineComponent((LineComponent) selectedComponent);
		}
		else if (selectedComponent instanceof RectangleComponent) {
			pasteBuffer = new RectangleComponent((RectangleComponent) selectedComponent);
		}
		else if (selectedComponent instanceof EllipseComponent) {
			pasteBuffer = new EllipseComponent((EllipseComponent) selectedComponent);
		}
		else if (selectedComponent instanceof TriangleComponent) {
			pasteBuffer = new TriangleComponent((TriangleComponent) selectedComponent);
		}
	}

	protected void pasteBufferedComponent(int x, int y) {
		if (pasteBuffer instanceof ImageComponent) {
			ImageComponent ic = null;
			try {
				ic = new ImageComponent(((ImageComponent) pasteBuffer).toString());
			}
			catch (Exception e) {
				e.printStackTrace();
				return;
			}
			ic.setLocation(x, y);
			addLayeredComponent(ic);
		}
		else if (pasteBuffer instanceof TextBoxComponent) {
			TextBoxComponent t = new TextBoxComponent((TextBoxComponent) pasteBuffer);
			t.setLocation(x, y);
			if (t.isCallOut())
				t.setCallOutLocation(x - 10, y - 10);
			addLayeredComponent(t);
		}
		else if (pasteBuffer instanceof LineComponent) {
			LineComponent l = new LineComponent((LineComponent) pasteBuffer);
			l.setLocation(x, y);
			addLayeredComponent(l);
		}
		else if (pasteBuffer instanceof RectangleComponent) {
			RectangleComponent r = new RectangleComponent((RectangleComponent) pasteBuffer);
			r.setLocation(x, y);
			addLayeredComponent(r);
		}
		else if (pasteBuffer instanceof EllipseComponent) {
			EllipseComponent e = new EllipseComponent((EllipseComponent) pasteBuffer);
			e.setLocation(x, y);
			addLayeredComponent(e);
		}
		else if (pasteBuffer instanceof TriangleComponent) {
			TriangleComponent t = new TriangleComponent((TriangleComponent) pasteBuffer);
			t.setLocation(x, y);
			addLayeredComponent(t);
		}
	}

	boolean openLayeredComponentPopupMenus(int x, int y, int layer) {
		if (layerBasket.isEmpty())
			return false;
		Layered c = null;
		synchronized (layerBasket) {
			int n = layerBasket.size();
			for (int i = n - 1; i >= 0; i--) {
				c = layerBasket.get(i);
				if (c.getLayer() == layer) {
					if (c.contains(x, y)) {
						((ModelComponent) c).setSelected(true);
						if (popupMenuForLayeredComponent == null) {
							popupMenuForLayeredComponent = new LayeredComponentPopupMenu(this);
							popupMenuForLayeredComponent.pack();
						}
						popupMenuForLayeredComponent.show(this, x, y);
						return true;
					}
				}
			}
		}
		return false;
	}

	abstract void prepareToUndoPositioning();

	static boolean isSelfCrossed(Polygon p) {
		if (p == null || p.npoints == 0)
			return false;
		int n = p.npoints;
		Line2D[] lines = new Line2D.Float[n];
		for (int i = 0; i < n - 1; i++) {
			lines[i] = new Line2D.Float(p.xpoints[i], p.ypoints[i], p.xpoints[i + 1], p.ypoints[i + 1]);
		}
		lines[n - 1] = new Line2D.Float(p.xpoints[n - 1], p.ypoints[n - 1], p.xpoints[0], p.ypoints[0]);
		for (int i = 0; i < n - 1; i++) {
			for (int j = i + 2; j < n - 1; j++) {
				if (lines[i].intersectsLine(lines[j]))
					return true;
			}
		}
		return false;
	}

	static boolean isShapeFine(Polygon p, int min) {
		if (p == null || p.npoints == 0)
			return false;
		int n = p.npoints;
		int xij, yij;
		for (int i = 0; i < n - 1; i++) {
			for (int j = i + 1; j < n; j++) {
				xij = p.xpoints[i] - p.xpoints[j];
				yij = p.ypoints[i] - p.ypoints[j];
				if (xij * xij + yij * yij < min * min)
					return false;
			}
		}
		return true;
	}

	static float getPerimeter(Polygon p) {
		if (p == null)
			throw new NullPointerException("p cannot be null");
		double perimeter = 0.0;
		int dx, dy;
		int n1 = p.npoints - 1;
		for (int i = 0; i < n1; i++) {
			dx = p.xpoints[i] - p.xpoints[i + 1];
			dy = p.ypoints[i] - p.ypoints[i + 1];
			perimeter += Math.hypot(dx, dy);
		}
		dx = p.xpoints[n1] - p.xpoints[0];
		dy = p.ypoints[n1] - p.ypoints[0];
		perimeter += Math.hypot(dx, dy);
		return (float) perimeter;
	}

	/*
	 * pull a spring-restrained object. Springs cannot be pulled to arbitrary long distance, because they are harmonical
	 * forces. Instead of disallowing springs to be stretched or compressed, we would like to allow the user to play
	 * with restrained objects a little bit.
	 * 
	 * @param x the x-coordinate of the mouse cursor location @param y the y-coordinate of the mouse cursor location
	 * 
	 * @param xc the x-coordinate of the equilibrium position @param yc the y-coordinate of the equilibrium position
	 * 
	 * @param r0 the equilibrium length of the spring @param d the maximum displacement
	 */
	static Vector2D moveSpring(int x, int y, int xc, int yc, int r0, int d) {
		double r = Math.hypot(x - xc, y - yc);
		if (r > r0 + d) {
			double x1 = (r0 + d) * (x - xc) / r + xc;
			double y1 = (r0 + d) * (y - yc) / r + yc;
			return new Vector2D(x1, y1);
		}
		else if (r < r0 - d) {
			double x1 = (r0 - d) * (x - xc) / r + xc;
			double y1 = (r0 - d) * (y - yc) / r + yc;
			return new Vector2D(x1, y1);
		}
		else {
			return new Vector2D(x, y);
		}
	}

	class Energizer {

		private int length = 100;
		private int x, y;
		private Rectangle exitButton, heatButton, coolButton;
		private Color exitButtonColor = Color.lightGray;
		private Color heatButtonColor = Color.lightGray;
		private Color coolButtonColor = Color.lightGray;

		Energizer(int x, int y, int length) {
			this.x = x;
			this.y = y;
			this.length = length;
			exitButton = new Rectangle(x, y - 10, 10, 10);
			heatButton = new Rectangle(x, y + length, 10, 10);
			coolButton = new Rectangle(x, y + length + 10, 10, 10);
		}

		void setX(int x) {
			this.x = exitButton.x = heatButton.x = coolButton.x = x;
		}

		void exit() {
			setEnergizer(false);
		}

		void heat() {
			getModel().changeTemperature(0.1);
		}

		void cool() {
			getModel().changeTemperature(-0.1);
		}

		void mouseEntered(int ex, int ey) {
			if (exitButton.contains(ex, ey)) {
				exitButtonColor = Color.gray;
				coolButtonColor = heatButtonColor = Color.lightGray;
			}
			else if (heatButton.contains(ex, ey)) {
				heatButtonColor = Color.gray;
				coolButtonColor = exitButtonColor = Color.lightGray;
			}
			else if (coolButton.contains(ex, ey)) {
				coolButtonColor = Color.gray;
				heatButtonColor = exitButtonColor = Color.lightGray;
			}
			else {
				mouseExited();
			}
		}

		void mouseExited() {
			coolButtonColor = heatButtonColor = exitButtonColor = Color.lightGray;
		}

		void paint(Graphics2D g) {
			setX(getWidth() - 18);
			int sk = (int) (100.0 * Math.log(1.0 + getModel().getKE()));
			g.setStroke(ViewAttribute.THIN);
			g.setColor(exitButtonColor);
			g.fill(exitButton);
			g.setColor(heatButtonColor);
			g.fill(heatButton);
			g.setColor(coolButtonColor);
			g.fill(coolButton);
			g.setColor(contrastBackground());
			g.draw(exitButton);
			g.draw(heatButton);
			g.draw(coolButton);
			g.setColor(Color.black);
			g.drawLine(exitButton.x, exitButton.y, exitButton.x + exitButton.width, exitButton.y + exitButton.height);
			g.drawLine(exitButton.x, exitButton.y + exitButton.height, exitButton.x + exitButton.width, exitButton.y);
			g.setColor(Color.red);
			Polygon poly = new Polygon();
			poly.addPoint(heatButton.x + heatButton.width / 2, heatButton.y + 2);
			poly.addPoint(heatButton.x + 2, heatButton.y + heatButton.height - 2);
			poly.addPoint(heatButton.x + heatButton.width - 2, heatButton.y + heatButton.height - 2);
			g.fill(poly);
			g.setColor(Color.blue);
			poly = new Polygon();
			poly.addPoint(coolButton.x + coolButton.width / 2, coolButton.y + coolButton.height - 2);
			poly.addPoint(coolButton.x + 2, coolButton.y + 2);
			poly.addPoint(coolButton.x + coolButton.width - 2, coolButton.y + 2);
			g.fill(poly);
			g.setColor(Color.white);
			g.fillRect(x, y, 10, length);
			g.setColor(contrastBackground());
			g.drawRect(x, y, 10, length);
			g.setColor(Color.red);
			g.fillRect(x + 1, y + length - sk, 9, sk);
		}

	}

	class SteeringForceController {

		private int currentReading;
		private Rectangle incrButton, decrButton;
		private Color incrButtonColor = Color.lightGray;
		private Color decrButtonColor = Color.lightGray;

		SteeringForceController() {
			incrButton = new Rectangle(0, 0, 10, 10);
			decrButton = new Rectangle(0, 0, 10, 10);
		}

		void setCurrentReading(int currentReading) {
			this.currentReading = currentReading;
		}

		void setX(int x) {
			incrButton.x = x + 10;
			decrButton.x = x;
		}

		void setY(int y) {
			incrButton.y = decrButton.y = y;
		}

		void increase() {
			changeUserField(true);
		}

		void decrease() {
			changeUserField(false);
		}

		void mouseEntered(int x, int y) {
			if (incrButton.contains(x, y)) {
				incrButtonColor = Color.gray;
				decrButtonColor = Color.lightGray;
			}
			else if (decrButton.contains(x, y)) {
				decrButtonColor = Color.gray;
				incrButtonColor = Color.lightGray;
			}
			else {
				incrButtonColor = decrButtonColor = Color.lightGray;
			}
		}

		void mouseExited() {
			incrButtonColor = decrButtonColor = Color.lightGray;
		}

		void paint(Graphics2D g) {
			if (currentReading == 0)
				return;
			setX(getWidth() - 110);
			setY(getHeight() - 15);

			// draw the rectangular area of the steering force meter
			g.setColor(Color.white);
			g.fillRect(decrButton.x + 20, decrButton.y, 80, 10);
			g.setColor(Color.orange);
			g.fillRect(decrButton.x + 21, decrButton.y + 1, currentReading, 9);

			// draw the buttons
			g.setColor(decrButtonColor);
			g.fill(decrButton);
			g.setColor(incrButtonColor);
			g.fill(incrButton);
			g.setColor(Color.black);
			Polygon poly = new Polygon();
			poly.addPoint(incrButton.x + 8, incrButton.y + incrButton.height / 2);
			poly.addPoint(incrButton.x + 2, incrButton.y + 2);
			poly.addPoint(incrButton.x + 2, incrButton.y + incrButton.height - 2);
			g.fill(poly);
			poly = new Polygon();
			poly.addPoint(decrButton.x + 2, decrButton.y + decrButton.height / 2);
			poly.addPoint(decrButton.x + decrButton.width - 2, decrButton.y + 2);
			poly.addPoint(decrButton.x + decrButton.width - 2, decrButton.y + decrButton.height - 2);
			g.fill(poly);

			// draw frames
			g.setStroke(ViewAttribute.THIN);
			g.setColor(contrastBackground());
			g.draw(incrButton);
			g.draw(decrButton);
			g.setColor(contrastBackground());
			g.drawRect(incrButton.x + 10, incrButton.y, 85, 10);
			g.setFont(ViewAttribute.LITTLE_FONT);
			g.drawString("Steering strength", incrButton.x + 15, incrButton.y + 8);

		}

	}

	/**
	 * This class defines the state of a view. An instance of this class can be serialized to save the state of the
	 * user's current work with a view. A persistent object of this class can be deserialized to retrieve the user's
	 * last state of work on a view.
	 */
	public static abstract class State extends MDState {

		private int renderingMethod;
		private byte restraintStyle = StyleConstant.RESTRAINT_CROSS_STYLE;
		private byte trajectoryStyle = StyleConstant.TRAJECTORY_LINE_STYLE;
		private String colorCode;
		private Color background = Color.white;
		private int markColor = 0xffccccff;
		private boolean energizer;
		private boolean showParticleIndex, showClock = true, showHeatBath = true, drawCharge = true,
				showMirrorImages = true, drawExternalForce;
		private boolean propertyDialogEnabled = true;
		private FillMode fillMode;
		private VectorFlavor velocityFlavor;
		private VectorFlavor momentumFlavor;
		private VectorFlavor accelerationFlavor;
		private VectorFlavor forceFlavor;
		private ImageComponent.Delegate[] icds;
		private TextBoxComponent.Delegate[] tbds;
		private LineComponent.Delegate[] lcds;
		private RectangleComponent.Delegate[] rcds;
		private EllipseComponent.Delegate[] ecds;
		private TriangleComponent.Delegate[] tcds;

		public State() {
			super();
			energizer = false;
			velocityFlavor = new VectorFlavor(Color.black, ViewAttribute.THIN, 100);
			momentumFlavor = new VectorFlavor(Color.gray, ViewAttribute.THIN, 1);
			accelerationFlavor = new VectorFlavor(Color.red, ViewAttribute.THIN, 100);
			forceFlavor = new VectorFlavor(Color.orange, ViewAttribute.THIN, 1);
		}

		public void setPropertyDialogEnabled(boolean b) {
			propertyDialogEnabled = b;
		}

		public boolean isPropertyDialogEnabled() {
			return propertyDialogEnabled;
		}

		public void setRenderingMethod(int i) {
			renderingMethod = i;
		}

		public int getRenderingMethod() {
			return renderingMethod;
		}

		public void setRestraintStyle(byte i) {
			restraintStyle = i;
		}

		public byte getRestraintStyle() {
			return restraintStyle;
		}

		public void setTrajectoryStyle(byte i) {
			trajectoryStyle = i;
		}

		public byte getTrajectoryStyle() {
			return trajectoryStyle;
		}

		public void setImages(ImageComponent.Delegate[] icds) {
			this.icds = icds;
		}

		public ImageComponent.Delegate[] getImages() {
			return icds;
		}

		public void setTextBoxes(TextBoxComponent.Delegate[] tbds) {
			this.tbds = tbds;
		}

		public TextBoxComponent.Delegate[] getTextBoxes() {
			return tbds;
		}

		public void setLines(LineComponent.Delegate[] lcds) {
			this.lcds = lcds;
		}

		public LineComponent.Delegate[] getLines() {
			return lcds;
		}

		public void setRectangles(RectangleComponent.Delegate[] rcds) {
			this.rcds = rcds;
		}

		public RectangleComponent.Delegate[] getRectangles() {
			return rcds;
		}

		public void setEllipses(EllipseComponent.Delegate[] ecds) {
			this.ecds = ecds;
		}

		public EllipseComponent.Delegate[] getEllipses() {
			return ecds;
		}

		public void setTriangles(TriangleComponent.Delegate[] tcds) {
			this.tcds = tcds;
		}

		public TriangleComponent.Delegate[] getTriangles() {
			return tcds;
		}

		public void setBackground(Color c) {
			background = c;
		}

		public Color getBackground() {
			return background;
		}

		public void setFillMode(FillMode fm) {
			fillMode = fm;
		}

		public FillMode getFillMode() {
			return fillMode;
		}

		public void setEnergizer(boolean b) {
			energizer = b;
		}

		public boolean getEnergizer() {
			return energizer;
		}

		public void setShowParticleIndex(boolean b) {
			showParticleIndex = b;
		}

		public boolean getShowParticleIndex() {
			return showParticleIndex;
		}

		public void setDrawCharge(boolean b) {
			drawCharge = b;
		}

		public boolean getDrawCharge() {
			return drawCharge;
		}

		public void setDrawExternalForce(boolean b) {
			drawExternalForce = b;
		}

		public boolean getDrawExternalForce() {
			return drawExternalForce;
		}

		public void setShowClock(boolean b) {
			showClock = b;
		}

		public boolean getShowClock() {
			return showClock;
		}

		public void setShowHeatBath(boolean b) {
			showHeatBath = b;
		}

		public boolean getShowHeatBath() {
			return showHeatBath;
		}

		public void setShowMirrorImages(boolean b) {
			showMirrorImages = b;
		}

		public boolean getShowMirrorImages() {
			return showMirrorImages;
		}

		public void setColorCode(String s) {
			colorCode = s;
		}

		public String getColorCode() {
			return colorCode;
		}

		public void setVelocityFlavor(VectorFlavor vf) {
			velocityFlavor = vf;
		}

		public VectorFlavor getVelocityFlavor() {
			return velocityFlavor;
		}

		public void setMomentumFlavor(VectorFlavor vf) {
			momentumFlavor = vf;
		}

		public VectorFlavor getMomentumFlavor() {
			return momentumFlavor;
		}

		public void setAccelerationFlavor(VectorFlavor vf) {
			accelerationFlavor = vf;
		}

		public VectorFlavor getAccelerationFlavor() {
			return accelerationFlavor;
		}

		public void setForceFlavor(VectorFlavor vf) {
			forceFlavor = vf;
		}

		public VectorFlavor getForceFlavor() {
			return forceFlavor;
		}

		public void setMarkColor(int i) {
			markColor = i;
		}

		public int getMarkColor() {
			return markColor;
		}

	}

	private class UndoableResizing extends AbstractUndoableEdit {

		private Dimension oldDim, newDim;

		UndoableResizing(Dimension oldDim, Dimension newDim) {
			this.oldDim = oldDim;
			this.newDim = newDim;
		}

		public String getPresentationName() {
			return "Resizing";
		}

		public void undo() {
			super.undo();
			doNotFireUndoEvent = true;
			setSize(oldDim);
			doNotFireUndoEvent = false;
			getModel().notifyChange();
			repaint();
		}

		public void redo() {
			super.redo();
			doNotFireUndoEvent = true;
			setSize(newDim);
			doNotFireUndoEvent = false;
			getModel().notifyChange();
			repaint();
		}

	}

	private class UndoableModelOperation extends AbstractUndoableEdit {

		private int undoID;
		private String presentationName = "";
		private double dx, dy;
		private double angle;

		UndoableModelOperation(int undoID, double angle) {
			this.undoID = undoID;
			this.angle = angle;
			switch (undoID) {
			case UndoAction.ROTATE_MODEL:
				presentationName = "Model Rotation";
				break;
			}
		}

		UndoableModelOperation(int undoID, double dx, double dy) {
			this.undoID = undoID;
			this.dx = dx;
			this.dy = dy;
			switch (undoID) {
			case UndoAction.TRANSLATE_MODEL:
				presentationName = "Model Translation";
				break;
			}
		}

		public String getPresentationName() {
			return presentationName;
		}

		public void undo() {
			super.undo();
			doNotFireUndoEvent = true;
			int n = getModel().getNumberOfParticles();
			switch (undoID) {
			case UndoAction.TRANSLATE_MODEL:
				for (int i = 0; i < n; i++) {
					getModel().getParticle(i).restoreState();
				}
				n = getModel().getObstacles().size();
				for (int i = 0; i < n; i++) {
					getModel().getObstacles().get(i).restoreState();
				}
				if (!layerBasket.isEmpty()) {
					synchronized (layerBasket) {
						for (Layered l : layerBasket) {
							((ModelComponent) l).restoreState();
						}
					}
				}
				break;
			case UndoAction.ROTATE_MODEL:
				for (int i = 0; i < n; i++) {
					getModel().getParticle(i).restoreState();
				}
				break;
			}
			doNotFireUndoEvent = false;
			getModel().notifyChange();
			if (MDView.this instanceof AtomisticView)
				((AtomisticView) MDView.this).refreshJmol();
			repaint();
		}

		public void redo() {
			super.redo();
			doNotFireUndoEvent = true;
			switch (undoID) {
			case UndoAction.TRANSLATE_MODEL:
				translateWholeModel(dx, dy);
				break;
			case UndoAction.ROTATE_MODEL:
				getModel().rotateWholeModel(angle);
				break;
			}
			doNotFireUndoEvent = false;
			getModel().notifyChange();
			if (MDView.this instanceof AtomisticView)
				((AtomisticView) MDView.this).refreshJmol();
			repaint();
		}

	}

	class UndoableLayeredComponentOperation extends AbstractUndoableEdit {

		private ModelComponent mc;
		private Layered lc;
		private int undoID;
		private String presentationName = "";
		private float x1, y1, x2, y2, x3, y3;

		UndoableLayeredComponentOperation(int undoID, Layered lc) {
			this.lc = lc;
			this.undoID = undoID;
			switch (undoID) {
			case UndoAction.INSERT_LAYERED_COMPONENT:
				if (lc instanceof ImageComponent)
					presentationName = "Inserting Image";
				else if (lc instanceof TextBoxComponent)
					presentationName = "Inserting Text Box";
				else if (lc instanceof LineComponent)
					presentationName = "Inserting Line";
				else if (lc instanceof RectangleComponent)
					presentationName = "Inserting Rectangle";
				else if (lc instanceof TriangleComponent)
					presentationName = "Inserting Triangle";
				else if (lc instanceof EllipseComponent)
					presentationName = "Inserting Ellipse";
				break;
			case UndoAction.REMOVE_LAYERED_COMPONENT:
				if (lc instanceof ImageComponent)
					presentationName = "Removing Image";
				else if (lc instanceof TextBoxComponent)
					presentationName = "Removing Text Box";
				else if (lc instanceof LineComponent)
					presentationName = "Removing Line";
				else if (lc instanceof RectangleComponent)
					presentationName = "Removing Rectangle";
				else if (lc instanceof TriangleComponent)
					presentationName = "Removing Triangle";
				else if (lc instanceof EllipseComponent)
					presentationName = "Removing Ellipse";
				break;
			case UndoAction.SEND_BACK_LAYERED_COMPONENT:
				if (lc instanceof ImageComponent)
					presentationName = "Sending Back Image";
				else if (lc instanceof TextBoxComponent)
					presentationName = "Sending Back Text Box";
				else if (lc instanceof LineComponent)
					presentationName = "Sending Back Line";
				else if (lc instanceof RectangleComponent)
					presentationName = "Sending Back Rectangle";
				else if (lc instanceof TriangleComponent)
					presentationName = "Sending Back Triangle";
				else if (lc instanceof EllipseComponent)
					presentationName = "Sending Back Ellipse";
				break;
			case UndoAction.BRING_FORWARD_LAYERED_COMPONENT:
				if (lc instanceof ImageComponent)
					presentationName = "Bringing Up Image";
				else if (lc instanceof TextBoxComponent)
					presentationName = "Bringing Up Text Box";
				else if (lc instanceof LineComponent)
					presentationName = "Bringing Up Line";
				else if (lc instanceof RectangleComponent)
					presentationName = "Bringing Up Rectangle";
				else if (lc instanceof TriangleComponent)
					presentationName = "Bringing Up Triangle";
				else if (lc instanceof EllipseComponent)
					presentationName = "Bringing Up Ellipse";
				break;
			case UndoAction.FRONT_LAYERED_COMPONENT:
				if (lc instanceof ImageComponent)
					presentationName = "Bringing Image in Front of Objects";
				else if (lc instanceof TextBoxComponent)
					presentationName = "Bringing Text Box in Front of Objects";
				else if (lc instanceof LineComponent)
					presentationName = "Bringing Line in Front of Objects";
				else if (lc instanceof RectangleComponent)
					presentationName = "Bringing Rectangle in Front of Objects";
				else if (lc instanceof TriangleComponent)
					presentationName = "Bringing Triangle in Front of Objects";
				else if (lc instanceof EllipseComponent)
					presentationName = "Bringing Ellipse in Front of Objects";
				break;
			case UndoAction.BACK_LAYERED_COMPONENT:
				if (lc instanceof ImageComponent)
					presentationName = "Sending Image Behind Objects";
				else if (lc instanceof TextBoxComponent)
					presentationName = "Sending Text Box Behind Objects";
				else if (lc instanceof LineComponent)
					presentationName = "Sending Line Behind Objects";
				else if (lc instanceof RectangleComponent)
					presentationName = "Sending Rectangle Behind Objects";
				else if (lc instanceof TriangleComponent)
					presentationName = "Sending Triangle Behind Objects";
				else if (lc instanceof EllipseComponent)
					presentationName = "Sending Ellipse Behind Objects";
				break;
			case UndoAction.ATTACH_LAYERED_COMPONENT:
				if (lc instanceof ImageComponent)
					presentationName = "Attaching Image";
				else if (lc instanceof TextBoxComponent)
					presentationName = "Attaching Text Box";
				else if (lc instanceof RectangleComponent)
					presentationName = "Attaching Rectangle";
				else if (lc instanceof TriangleComponent)
					presentationName = "Attaching Triangle";
				else if (lc instanceof EllipseComponent)
					presentationName = "Attaching Ellipse";
				mc = lc.getHost();
				break;
			case UndoAction.DETACH_LAYERED_COMPONENT:
				if (lc instanceof ImageComponent)
					presentationName = "Detaching Image";
				else if (lc instanceof TextBoxComponent)
					presentationName = "Detaching Text Box";
				else if (lc instanceof RectangleComponent)
					presentationName = "Detaching Rectangle";
				else if (lc instanceof TriangleComponent)
					presentationName = "Detaching Triangle";
				else if (lc instanceof EllipseComponent)
					presentationName = "Detaching Ellipse";
				mc = lc.getHost();
				break;
			case UndoAction.RESIZE_LINE:
				if (lc instanceof LineComponent) {
					presentationName = "Resizing Line";
					x1 = ((LineComponent) lc).getX1();
					x2 = ((LineComponent) lc).getX2();
					y1 = ((LineComponent) lc).getY1();
					y2 = ((LineComponent) lc).getY2();
				}
				break;
			case UndoAction.RESIZE_RECTANGLE:
				if (lc instanceof RectangleComponent) {
					presentationName = "Resizing Rectangle";
					x1 = ((RectangleComponent) lc).getX();
					y1 = ((RectangleComponent) lc).getY();
					x2 = ((RectangleComponent) lc).getWidth();
					y2 = ((RectangleComponent) lc).getHeight();
				}
				break;
			case UndoAction.RESIZE_TRIANGLE:
				if (lc instanceof TriangleComponent) {
					presentationName = "Resizing Triangle";
					TriangleComponent tc = (TriangleComponent) lc;
					x1 = tc.getVertex(0).x;
					y1 = tc.getVertex(0).y;
					x2 = tc.getVertex(1).x;
					y2 = tc.getVertex(1).y;
					x3 = tc.getVertex(2).x;
					y3 = tc.getVertex(2).y;
				}
				break;
			case UndoAction.RESIZE_ELLIPSE:
				if (lc instanceof EllipseComponent) {
					presentationName = "Resizing Ellipse";
					x1 = ((EllipseComponent) lc).getX();
					y1 = ((EllipseComponent) lc).getY();
					x2 = ((EllipseComponent) lc).getWidth();
					y2 = ((EllipseComponent) lc).getHeight();
				}
				break;
			}
		}

		public String getPresentationName() {
			return presentationName;
		}

		public void undo() {
			super.undo();
			doNotFireUndoEvent = true;
			switch (undoID) {
			case UndoAction.INSERT_LAYERED_COMPONENT:
				layerBasket.remove(lc);
				if (((ModelComponent) lc).isSelected())
					setSelectedComponent(null);
				break;
			case UndoAction.REMOVE_LAYERED_COMPONENT:
				layerBasket.add(lc);
				((ModelComponent) lc).setSelected(true);
				break;
			case UndoAction.SEND_BACK_LAYERED_COMPONENT:
				layerForward(lc);
				break;
			case UndoAction.BRING_FORWARD_LAYERED_COMPONENT:
				layerBack(lc);
				break;
			case UndoAction.FRONT_LAYERED_COMPONENT:
				lc.setLayer(Layered.BEHIND_PARTICLES);
				break;
			case UndoAction.BACK_LAYERED_COMPONENT:
				lc.setLayer(Layered.IN_FRONT_OF_PARTICLES);
				break;
			case UndoAction.DETACH_LAYERED_COMPONENT:
				lc.setHost(mc);
				break;
			case UndoAction.ATTACH_LAYERED_COMPONENT:
				lc.setHost(null);
				if (lc instanceof ModelComponent)
					((ModelComponent) lc).restoreState();
				break;
			case UndoAction.RESIZE_LINE:
				if (lc instanceof LineComponent)
					((LineComponent) lc).restoreState();
				break;
			case UndoAction.RESIZE_RECTANGLE:
				if (lc instanceof RectangleComponent)
					((RectangleComponent) lc).restoreState();
				break;
			case UndoAction.RESIZE_TRIANGLE:
				if (lc instanceof TriangleComponent)
					((TriangleComponent) lc).restoreState();
				break;
			case UndoAction.RESIZE_ELLIPSE:
				if (lc instanceof EllipseComponent)
					((EllipseComponent) lc).restoreState();
				break;
			}
			doNotFireUndoEvent = false;
			getModel().notifyChange();
			repaint();
		}

		public void redo() {
			super.redo();
			doNotFireUndoEvent = true;
			switch (undoID) {
			case UndoAction.INSERT_LAYERED_COMPONENT:
				layerBasket.add(lc);
				((ModelComponent) lc).setSelected(true);
				break;
			case UndoAction.REMOVE_LAYERED_COMPONENT:
				layerBasket.remove(lc);
				if (((ModelComponent) lc).isSelected())
					setSelectedComponent(null);
				break;
			case UndoAction.SEND_BACK_LAYERED_COMPONENT:
				layerBack(lc);
				break;
			case UndoAction.BRING_FORWARD_LAYERED_COMPONENT:
				layerForward(lc);
				break;
			case UndoAction.FRONT_LAYERED_COMPONENT:
				lc.setLayer(Layered.IN_FRONT_OF_PARTICLES);
				break;
			case UndoAction.BACK_LAYERED_COMPONENT:
				lc.setLayer(Layered.BEHIND_PARTICLES);
				break;
			case UndoAction.ATTACH_LAYERED_COMPONENT:
				lc.setHost(mc);
				break;
			case UndoAction.DETACH_LAYERED_COMPONENT:
				lc.setHost(null);
				break;
			case UndoAction.RESIZE_LINE:
				if (lc instanceof LineComponent)
					((LineComponent) lc).setLine(x1, y1, x2, y2);
				break;
			case UndoAction.RESIZE_RECTANGLE:
				if (lc instanceof RectangleComponent)
					((RectangleComponent) lc).setRect(x1, y1, x2, y2);
				break;
			case UndoAction.RESIZE_TRIANGLE:
				if (lc instanceof TriangleComponent) {
					TriangleComponent tc = (TriangleComponent) lc;
					tc.setVertex(0, x1, y1);
					tc.setVertex(1, x2, y2);
					tc.setVertex(2, x3, y3);
				}
				break;
			case UndoAction.RESIZE_ELLIPSE:
				if (lc instanceof EllipseComponent)
					((EllipseComponent) lc).setOval(x1, y1, x2, y2);
				break;
			}
			doNotFireUndoEvent = false;
			getModel().notifyChange();
			repaint();
		}

	}

}