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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.AbstractUndoableEdit;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.draw.StrokeFactory;
import org.concord.modeler.event.ModelEvent;
import org.concord.modeler.g2d.ContourMap;
import org.concord.modeler.math.Vector2D;
import org.concord.modeler.process.Executable;
import org.concord.modeler.ui.IconPool;
import org.concord.modeler.util.FileUtilities;
import org.concord.mw2d.event.BondChangeEvent;
import org.concord.mw2d.event.BondChangeListener;
import org.concord.mw2d.models.AngularBond;
import org.concord.mw2d.models.AngularBondCollection;
import org.concord.mw2d.models.Atom;
import org.concord.mw2d.models.Benzene;
import org.concord.mw2d.models.ChainMolecule;
import org.concord.mw2d.models.CurvedRibbon;
import org.concord.mw2d.models.CurvedSurface;
import org.concord.mw2d.models.DiatomicMolecule;
import org.concord.mw2d.models.DNAStrand;
import org.concord.mw2d.models.ElectricField;
import org.concord.mw2d.models.ElectricForceField;
import org.concord.mw2d.models.Electron;
import org.concord.mw2d.models.Element;
import org.concord.mw2d.models.EllipseComponent;
import org.concord.mw2d.models.Grid;
import org.concord.mw2d.models.ImageComponent;
import org.concord.mw2d.models.Layered;
import org.concord.mw2d.models.LightSource;
import org.concord.mw2d.models.LineComponent;
import org.concord.mw2d.models.MDModel;
import org.concord.mw2d.models.ModelComponent;
import org.concord.mw2d.models.MolecularModel;
import org.concord.mw2d.models.MolecularObject;
import org.concord.mw2d.models.Molecule;
import org.concord.mw2d.models.MoleculeCollection;
import org.concord.mw2d.models.Obstacle;
import org.concord.mw2d.models.ObstacleCollection;
import org.concord.mw2d.models.Photon;
import org.concord.mw2d.models.PointRestraint;
import org.concord.mw2d.models.Polypeptide;
import org.concord.mw2d.models.PotentialContour;
import org.concord.mw2d.models.RadialBond;
import org.concord.mw2d.models.RadialBondCollection;
import org.concord.mw2d.models.ReactionModel;
import org.concord.mw2d.models.RectangleComponent;
import org.concord.mw2d.models.RectangularBoundary;
import org.concord.mw2d.models.RectangularObstacle;
import org.concord.mw2d.models.Rotatable;
import org.concord.mw2d.models.TextBoxComponent;
import org.concord.mw2d.models.TriangleComponent;
import org.concord.mw2d.models.TriatomicMolecule;
import org.concord.mw2d.models.UserField;
import org.concord.mw2d.ui.ChainConfigure;
import org.concord.mw2d.ui.DiatomicConfigure;
import org.concord.mw2d.ui.TriatomicConfigure;

import static org.concord.mw2d.UserAction.*;

public class AtomisticView extends MDView implements BondChangeListener {

	public final static short SQUARE_LATTICE = 6011;
	public final static short HEXAGONAL_LATTICE = 6012;

	private final static double TWO_TO_ONE_SIXTH = Math.pow(2.0, 1.0 / 6.0);
	private final static Object contourLock = new Object();

	/* managers */
	ColorManager colorManager;
	StyleManager styleManager;

	/* model data */
	MolecularModel model;
	Atom[] atom;
	RadialBondCollection bonds;
	AngularBondCollection bends;
	MoleculeCollection molecules;

	/* widgets */
	ElementEditor elementEditor;
	LightSourceEditor lightSourceEditor;
	QuantumDynamicsRuleEditor quantumDynamicsRuleEditor;

	private Action editElementAction;

	/* popup menus */
	private AtomPopupMenu atomPopupMenu;
	private RadialBondPopupMenu radialBondPopupMenu;
	private AngularBondPopupMenu angularBondPopupMenu;
	private MoleculePopupMenu moleculePopupMenu;
	private MolecularObjectPopupMenu molecularObjectPopupMenu;
	private ObstaclePopupMenu obstaclePopupMenu;
	private DefaultPopupMenu defaultPopupMenu;
	private AminoAcidPopupMenu acidPopupMenu;
	private NucleotidePopupMenu nucleotidePopupMenu;
	private AtomMutationPopupMenu atomMutationPopupMenu;

	/* action accessories */
	private int rotationHandle = -1;
	private boolean restoreMolecule;
	boolean bondBeingMade, bendBeingMade;
	private Ellipse2D.Float addAtomIndicator = new Ellipse2D.Float();
	private AddObjectIndicator addObjectIndicator;
	int nAtom;
	private int nAtomPBC;

	/* obstacle */
	private ObstacleCollection obstacles;
	private byte obsRectSelected = -1;

	/* boundary */
	private List mirrorBonds;

	/* the smart family */
	private Shape smartShape;
	private Polygon smartPoly;

	/* views */
	private boolean useJmol;
	private boolean shading, chargeShading, showVDWCircles, showVDWLines, showChargeLines, showSSLines, showBPLines,
			velocityVector, momentumVector, accelerationVector, forceVector, showExcitation = true;
	private float vdwLinesRatio = 2;
	private BasicStroke vdwLineStroke = (BasicStroke) ViewAttribute.THIN_DASHED;
	private boolean contourPlot, eFieldLines, showSites;
	private Atom probe;
	private Vector[] isoCurves;
	private GeneralPath contourLine;
	private PotentialContour contour;
	private ContourMap contourMap;
	private ElectricForceField eField;
	private byte gridMode = -1;
	private Grid grid;
	private static BufferedImage biRect;
	private Line2D.Double tempLine;
	private final static GradientPaint colorScale = new GradientPaint(10, 10, Color.red, 10, 50, Color.white);

	/* undo-redo */
	private Atom[] atomBufferArray;
	private List<RadialBond.Delegate> deadBonds = new ArrayList<RadialBond.Delegate>();
	private List<AngularBond.Delegate> deadBends = new ArrayList<AngularBond.Delegate>();
	private Map<Integer, Integer> liveAtomMap = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> deadAtomMap = new HashMap<Integer, Integer>();
	private Map<Object, List<Layered>> deadLayered = new HashMap<Object, List<Layered>>();

	private Mw2dRenderer mw2dRenderer;
	private JmolRenderer jmolRenderer;

	public AtomisticView() {

		super();

		// fix bugs on Mac OS X about GradientPaint (this bug still exists, 9/10/2007)
		if (System.getProperty("os.name").startsWith("Mac")) {
			if (biRect == null) {
				GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
				GraphicsDevice gd = ge.getDefaultScreenDevice();
				GraphicsConfiguration gc = gd.getDefaultConfiguration();
				biRect = gc.createCompatibleImage(10, 50);
				Graphics2D bimg = biRect.createGraphics();
				bimg.setPaint(colorScale);
				bimg.fillRect(0, 0, 10, 50);
				bimg.dispose();
			}
		}

		colorManager = new ColorManager();
		styleManager = new StyleManager(this);
		mw2dRenderer = new Mw2dRenderer(this);

		contour = new PotentialContour();

		Action a = new AbstractAction() {
			public Object getValue(String key) {
				if (key.equalsIgnoreCase("state"))
					return chargeLinesShown() ? Boolean.TRUE : Boolean.FALSE;
				return super.getValue(key);
			}

			public void actionPerformed(ActionEvent e) {
				if (ModelerUtilities.stopFiring(e))
					return;
				Object o = e.getSource();
				if (o instanceof JToggleButton) {
					// call the force routine once to build the neighbor list for the first time
					refreshForces();
					showChargeLines(((JToggleButton) o).isSelected());
				}
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.NAME, "Show Electrostatic Attractions");
		a.putValue(Action.SHORT_DESCRIPTION, "Show electrostatic attractions");
		booleanSwitches.put(a.toString(), a);

		a = new AbstractAction() {
			public Object getValue(String key) {
				if (key.equalsIgnoreCase("state"))
					return ssLinesShown() ? Boolean.TRUE : Boolean.FALSE;
				return super.getValue(key);
			}

			public void actionPerformed(ActionEvent e) {
				if (ModelerUtilities.stopFiring(e))
					return;
				Object o = e.getSource();
				if (o instanceof JToggleButton) {
					// call the force routine once to build the neighbor list for the first time
					refreshForces();
					showSSLines(((JToggleButton) o).isSelected());
				}
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.NAME, "Show Disulfide Bonds");
		a.putValue(Action.SHORT_DESCRIPTION, "Show disulfide bonds");
		booleanSwitches.put(a.toString(), a);

		a = new AbstractAction() {
			public Object getValue(String key) {
				if (key.equalsIgnoreCase("state"))
					return bpLinesShown() ? Boolean.TRUE : Boolean.FALSE;
				return super.getValue(key);
			}

			public void actionPerformed(ActionEvent e) {
				if (ModelerUtilities.stopFiring(e))
					return;
				Object o = e.getSource();
				if (o instanceof JToggleButton) {
					// call the force routine once to build the neighbor list for the first time
					refreshForces();
					showBPLines(((JToggleButton) o).isSelected());
				}
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.NAME, "Show Hydrogen Bonds Between Base Pairs");
		a.putValue(Action.SHORT_DESCRIPTION, "Show hydrogen bonds between base pairs");
		booleanSwitches.put(a.toString(), a);

		a = new AbstractAction() {
			public Object getValue(String key) {
				if (key.equalsIgnoreCase("state"))
					return excitationShown() ? Boolean.TRUE : Boolean.FALSE;
				return super.getValue(key);
			}

			public void actionPerformed(ActionEvent e) {
				if (ModelerUtilities.stopFiring(e))
					return;
				Object o = e.getSource();
				if (o instanceof JToggleButton)
					showExcitation(((JToggleButton) o).isSelected());
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.NAME, "Show Excitation");
		a.putValue(Action.SHORT_DESCRIPTION, "Show excitation");
		booleanSwitches.put(a.toString(), a);

		a = new AbstractAction() {
			public Object getValue(String key) {
				if (key.equalsIgnoreCase("state"))
					return vdwLinesShown() ? Boolean.TRUE : Boolean.FALSE;
				return super.getValue(key);
			}

			public void actionPerformed(ActionEvent e) {
				if (ModelerUtilities.stopFiring(e))
					return;
				Object o = e.getSource();
				if (o instanceof JToggleButton) {
					// call the force routine once to build the neighbor list for the first time
					refreshForces();
					showVDWLines(((JToggleButton) o).isSelected());
				}
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.NAME, "Show VDW Lines");
		a.putValue(Action.SHORT_DESCRIPTION, "Show van der Waals interactions");
		booleanSwitches.put(a.toString(), a);

		a = new AbstractAction() {
			public Object getValue(String key) {
				if (key.equalsIgnoreCase("state"))
					return shadingShown() ? Boolean.TRUE : Boolean.FALSE;
				return super.getValue(key);
			}

			public void actionPerformed(ActionEvent e) {
				if (ModelerUtilities.stopFiring(e))
					return;
				Object o = e.getSource();
				if (o instanceof JToggleButton) {
					boolean b = ((JToggleButton) o).isSelected();
					showShading(b);
					if (b)
						showChargeShading(false);
				}
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.NAME, "K. E. Shading");
		a.putValue(Action.SHORT_DESCRIPTION, "K. E. Shading");
		booleanSwitches.put(a.toString(), a);

		a = new AbstractAction() {
			public Object getValue(String key) {
				if (key.equalsIgnoreCase("state"))
					return velocityVectorShown() ? Boolean.TRUE : Boolean.FALSE;
				return super.getValue(key);
			}

			public void actionPerformed(ActionEvent e) {
				if (ModelerUtilities.stopFiring(e))
					return;
				Object o = e.getSource();
				if (o instanceof JToggleButton)
					showVelocityVector(((JToggleButton) o).isSelected());
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.NAME, "Velocity Vector");
		a.putValue(Action.SHORT_DESCRIPTION, "Velocity Vector");
		booleanSwitches.put(a.toString(), a);

		a = new AbstractAction() {
			public Object getValue(String key) {
				if (key.equalsIgnoreCase("state"))
					return momentumVectorShown() ? Boolean.TRUE : Boolean.FALSE;
				return super.getValue(key);
			}

			public void actionPerformed(ActionEvent e) {
				if (ModelerUtilities.stopFiring(e))
					return;
				Object o = e.getSource();
				if (o instanceof JToggleButton)
					showMomentumVector(((JToggleButton) o).isSelected());
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.NAME, "Momentum Vector");
		a.putValue(Action.SHORT_DESCRIPTION, "Momentum Vector");
		booleanSwitches.put(a.toString(), a);

		a = new AbstractAction() {
			public Object getValue(String key) {
				if (key.equalsIgnoreCase("state"))
					return accelerationVectorShown() ? Boolean.TRUE : Boolean.FALSE;
				return super.getValue(key);
			}

			public void actionPerformed(ActionEvent e) {
				if (ModelerUtilities.stopFiring(e))
					return;
				Object o = e.getSource();
				if (o instanceof JToggleButton)
					showAccelerationVector(((JToggleButton) o).isSelected());
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.NAME, "Acceleration Vector");
		a.putValue(Action.SHORT_DESCRIPTION, "Acceleration Vector");
		booleanSwitches.put(a.toString(), a);

		a = new AbstractAction() {
			public Object getValue(String key) {
				if (key.equalsIgnoreCase("state"))
					return forceVectorShown() ? Boolean.TRUE : Boolean.FALSE;
				return super.getValue(key);
			}

			public void actionPerformed(ActionEvent e) {
				if (ModelerUtilities.stopFiring(e))
					return;
				Object o = e.getSource();
				if (o instanceof JToggleButton)
					showForceVector(((JToggleButton) o).isSelected());
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.NAME, "Force Vector");
		a.putValue(Action.SHORT_DESCRIPTION, "Force Vector");
		booleanSwitches.put(a.toString(), a);

		a = new DisplayStyleAction(this);
		multipleChoices.put(a.toString(), a);
		a = new ShowPropertiesAction(this);
		multipleChoices.put(a.toString(), a);

		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				notifyJmol();
			}
		});

	}

	public void setModel(MDModel mod) {

		if (mod == null) {
			model = null;
			return;
		}
		if (!(mod instanceof MolecularModel))
			throw new IllegalArgumentException("Can't accept non-molecular model");
		model = (MolecularModel) mod;
		super.setModel(mod);
		model.addBondChangeListener(this);

		atom = model.getAtoms();
		obstacles = model.getObstacles();
		bonds = model.getBonds();
		bends = model.getBends();
		molecules = model.getMolecules();
		initEditFieldActions();

		if (elementEditor == null)
			elementEditor = new ElementEditor(model);
		editElementAction = new ModelAction(model, new Executable() {
			public void execute() {
				elementEditor.setModel(model);
				elementEditor.createDialog(AtomisticView.this, true).setVisible(true);
				resetAddObjectIndicator();
			}
		}) {
			public String toString() {
				return (String) getValue(Action.SHORT_DESCRIPTION);
			}
		};
		editElementAction.putValue(Action.NAME, "Change");
		editElementAction.putValue(Action.SHORT_DESCRIPTION, "Change the van der Waals parameters");
		model.getActions().put(editElementAction.toString(), editElementAction);

		EngineAction ea = new EngineAction(model);
		getActionMap().put(ea.toString(), ea);

		Action a = new ModelAction(model, new Executable() {
			public void execute() {
				final LightSource light = model.getLightSource();
				if (light == null)
					return;
				if (lightSourceEditor == null)
					lightSourceEditor = new LightSourceEditor();
				lightSourceEditor.createDialog(JOptionPane.getFrameForComponent(AtomisticView.this), model).setVisible(
						true);
			}
		}) {
			public String toString() {
				return (String) getValue(Action.SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_L);
		a.putValue(Action.NAME, "Light Source");
		a.putValue(Action.SHORT_DESCRIPTION, "Edit the light source");
		model.getActions().put(a.toString(), a);

		a = new ModelAction(model, new Executable() {
			public void execute() {
				if (quantumDynamicsRuleEditor == null)
					quantumDynamicsRuleEditor = new QuantumDynamicsRuleEditor();
				quantumDynamicsRuleEditor.createDialog(JOptionPane.getFrameForComponent(AtomisticView.this), model)
						.setVisible(true);
			}
		}) {
			public String toString() {
				return (String) getValue(Action.SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_D);
		a.putValue(Action.NAME, "Quantum Dynamics Rules");
		a.putValue(Action.SHORT_DESCRIPTION, "Edit quantum dynamics rules");
		model.getActions().put(a.toString(), a);

		if (!layerBasket.isEmpty()) {
			synchronized (layerBasket) {
				for (Iterator i = layerBasket.iterator(); i.hasNext();) {
					((ModelComponent) i.next()).setModel(model);
				}
			}
		}

		defaultPopupMenu = new DefaultPopupMenu(this);

	}

	public MDModel getModel() {
		return model;
	}

	public void destroy() {
		super.destroy();
		layerBasket.clear();
		if (atomBufferArray != null) {
			for (Atom a : atomBufferArray) {
				a.setModel(null);
				a = null;
			}
		}
		setModel(null);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				destroyPopupMenu(atomPopupMenu);
				destroyPopupMenu(radialBondPopupMenu);
				destroyPopupMenu(angularBondPopupMenu);
				destroyPopupMenu(moleculePopupMenu);
				destroyPopupMenu(molecularObjectPopupMenu);
				destroyPopupMenu(obstaclePopupMenu);
				destroyPopupMenu(defaultPopupMenu);
				destroyPopupMenu(acidPopupMenu);
				destroyPopupMenu(nucleotidePopupMenu);
				destroyPopupMenu(atomMutationPopupMenu);
			}
		});
	}

	public void clear() {
		super.clear();
		useJmol = false;
		styleManager.reset();
		colorManager.reset();
		accelerationVector = false;
		forceVector = false;
		velocityVector = false;
		momentumVector = false;
		shading = false;
		chargeShading = false;
		showExcitation = true;
		contourPlot = false;
		showVDWCircles = false;
		showVDWLines = false;
		vdwLinesRatio = 2;
		showChargeLines = false;
		showSSLines = false;
		showBPLines = false;
		showSites = false;
		setGrid(null);
		setGridMode((byte) -0x01);
		if (eField != null)
			eField.setCellSize(10);
		setTranslucent(false);
	}

	public int getColor(Atom a) {
		return colorManager.getColor(a);
	}

	public int[] getElementColors() {
		return colorManager.getElementColors();
	}

	public void setElementColors(int[] i) {
		colorManager.setElementColors(i);
	}

	public void setDisplayStyle(String s) {
		if (s == null)
			return;
		if (s.equals("space_filling"))
			setDisplayStyle(StyleConstant.SPACE_FILLING);
		else if (s.equals("ball_and_stick"))
			setDisplayStyle(StyleConstant.BALL_AND_STICK);
		else if (s.equals("wire_frame"))
			setDisplayStyle(StyleConstant.WIRE_FRAME);
		else if (s.equals("stick"))
			setDisplayStyle(StyleConstant.STICK);
	}

	public void setDisplayStyle(byte i) {
		if (i == styleManager.getStyle())
			return;
		styleManager.setStyle(i);
		if (useJmol && jmolRenderer != null)
			jmolRenderer.setDisplayStyle(i);
		model.notifyChange();
	}

	public byte getDisplayStyle() {
		return styleManager.getStyle();
	}

	public byte getVdwPercentage() {
		return styleManager.getVdwPercentage();
	}

	public Color getKeShadingColor(double ke) {
		return styleManager.getKeShadingColor(ke);
	}

	public Color getChargeShadingColor(double charge) {
		return styleManager.getChargeShadingColor(charge);
	}

	public void setVdwLineThickness(float thickness) {
		vdwLineStroke = StrokeFactory.changeThickness(vdwLineStroke, thickness);
	}

	public float getVdwLineThickness() {
		return vdwLineStroke.getLineWidth();
	}

	public Action editElements(int i) {
		elementEditor.setFocusedElement(i);
		return editElementAction;
	}

	public void enableEditor(boolean b) {
		if (!b) {
			smartShape = null;
			smartPoly = null;
		}
		super.enableEditor(b);
	}

	public void clearEditor(boolean b) {
		if (b) {
			smartShape = null;
			smartPoly = null;
		}
		super.clearEditor(b);
	}

	public void setAction(short id) {
		super.setAction(id);
		if (actionID == DELE_ID) {
			if (selectedComponent != null) {
				selectedComponent.setSelected(false);
				selectedComponent = null;
			}
		}
		if (selectedComponent instanceof Rotatable) {
			if (actionID == ROTA_ID) {
				// calling the following method should cause the positions of the rotation handles to be re-calculated
				selectedComponent.setSelected(true);
				((Rotatable) selectedComponent).setSelectedToRotate(true);
			}
			else {
				((Rotatable) selectedComponent).setSelectedToRotate(false);
			}
		}
		resetAddObjectIndicator();
		if (actionID != VELO_ID)
			selectVelocity(null);
		repaint();
	}

	public void resetAddObjectIndicator() {
		addObjectIndicator = null;
		switch (actionID) {
		case ADDA_ID:
			float sigma = (float) model.getElement(Element.ID_NT).getSigma();
			addAtomIndicator.setFrame(-1, -1, sigma, sigma);
			break;
		case ADDB_ID:
			sigma = (float) model.getElement(Element.ID_PL).getSigma();
			addAtomIndicator.setFrame(-1, -1, sigma, sigma);
			break;
		case ADDC_ID:
			sigma = (float) model.getElement(Element.ID_WS).getSigma();
			addAtomIndicator.setFrame(-1, -1, sigma, sigma);
			break;
		case ADDD_ID:
			sigma = (float) model.getElement(Element.ID_CK).getSigma();
			addAtomIndicator.setFrame(-1, -1, sigma, sigma);
			break;
		case ADDI_ID:
			addObjectIndicator = new AddObjectIndicator.AddDiatomicMoleculeIndicator(model.getElement(
					DiatomicConfigure.typeOfA).getSigma(), model.getElement(DiatomicConfigure.typeOfB).getSigma(),
					model.getElement(DiatomicConfigure.typeOfA).getMass(), model.getElement(DiatomicConfigure.typeOfB)
							.getMass(), DiatomicConfigure.distance);
			break;
		case WATE_ID:
			addObjectIndicator = new AddObjectIndicator.AddTriatomicMoleculeIndicator(model.getElement(
					TriatomicConfigure.typeOfA).getSigma(), model.getElement(TriatomicConfigure.typeOfB).getSigma(),
					model.getElement(TriatomicConfigure.typeOfC).getSigma(), model.getElement(
							TriatomicConfigure.typeOfA).getMass(), model.getElement(TriatomicConfigure.typeOfB)
							.getMass(), model.getElement(TriatomicConfigure.typeOfC).getMass(), TriatomicConfigure.d12,
					TriatomicConfigure.d23, TriatomicConfigure.angle);
			break;
		case BENZ_ID:
			addObjectIndicator = new AddObjectIndicator.AddBenzeneMoleculeIndicator(model.getElement(Element.ID_NT)
					.getSigma(), model.getElement(Element.ID_PL).getSigma());
			break;
		case ADCH_ID:
			if (ChainConfigure.growMode != ChainConfigure.RANDOM) {
				addObjectIndicator = new AddObjectIndicator.AddChainMoleculeIndicator(model.getElement(
						ChainConfigure.typeOfAtom).getSigma(), ChainConfigure.number, ChainConfigure.growMode,
						ChainConfigure.distance, ChainConfigure.angle);
			}
			break;
		}
	}

	public void setUseJmol(boolean b) {
		if (!b)
			styleManager.setStyle(StyleConstant.SPACE_FILLING);
		if (useJmol == b)
			return;
		useJmol = b;
		if (useJmol) {
			if (jmolRenderer == null)
				jmolRenderer = new JmolRenderer(this);
		}
		model.notifyChange();
		repaint();
	}

	public boolean getUseJmol() {
		return useJmol;
	}

	public void showShading(boolean state) {
		if (shading == state)
			return;
		shading = state;
		model.notifyChange();
		refreshJmol();
		repaint();
	}

	public boolean shadingShown() {
		return shading;
	}

	public void showChargeShading(boolean state) {
		if (chargeShading == state)
			return;
		chargeShading = state;
		model.notifyChange();
		refreshJmol();
		repaint();
	}

	public boolean chargeShadingShown() {
		return chargeShading;
	}

	public void showExcitation(boolean state) {
		if (showExcitation == state)
			return;
		showExcitation = state;
		model.notifyChange();
		repaint();
	}

	public boolean excitationShown() {
		return showExcitation;
	}

	public void setVDWLinesRatio(float ratio) {
		if (vdwLinesRatio == ratio)
			return;
		vdwLinesRatio = ratio;
		model.notifyChange();
		repaint();
	}

	public float getVDWLinesRatio() {
		return vdwLinesRatio;
	}

	public void showVDWLines(boolean state) {
		if (showVDWLines == state)
			return;
		showVDWLines = state;
		model.notifyChange();
		repaint();
	}

	public boolean vdwLinesShown() {
		return showVDWLines;
	}

	public void showVDWCircles(boolean state) {
		if (showVDWCircles == state)
			return;
		showVDWCircles = state;
		model.notifyChange();
		repaint();
	}

	public boolean vdwCirclesShown() {
		return showVDWCircles;
	}

	public void setTranslucent(boolean b) {
		styleManager.setTranslucent(b);
		if (jmolRenderer != null) {
			jmolRenderer.setTranslucent(b);
		}
	}

	public boolean isTranslucent() {
		return styleManager.isTranslucent();
	}

	public void setVDWCircleStyle(byte i) {
		styleManager.setVDWSphereStyle(i);
	}

	public byte getVDWCircleStyle() {
		return styleManager.getVDWSphereStyle();
	}

	public void showChargeLines(boolean state) {
		if (showChargeLines == state)
			return;
		showChargeLines = state;
		model.notifyChange();
		repaint();
	}

	public boolean chargeLinesShown() {
		return showChargeLines;
	}

	public void showSSLines(boolean state) {
		if (showSSLines == state)
			return;
		showSSLines = state;
		model.notifyChange();
		repaint();
	}

	public boolean ssLinesShown() {
		return showSSLines;
	}

	public void showBPLines(boolean state) {
		if (showBPLines == state)
			return;
		showBPLines = state;
		model.notifyChange();
		repaint();
	}

	public boolean bpLinesShown() {
		return showBPLines;
	}

	public void showVelocityVector(boolean state) {
		if (velocityVector == state)
			return;
		velocityVector = state;
		model.notifyChange();
		repaint();
	}

	public boolean velocityVectorShown() {
		return velocityVector;
	}

	public void showMomentumVector(boolean state) {
		if (momentumVector == state)
			return;
		momentumVector = state;
		model.notifyChange();
		repaint();
	}

	public boolean momentumVectorShown() {
		return momentumVector;
	}

	public void showAccelerationVector(boolean state) {
		if (accelerationVector == state)
			return;
		accelerationVector = state;
		model.notifyChange();
		repaint();
	}

	public boolean accelerationVectorShown() {
		return accelerationVector;
	}

	public void showForceVector(boolean state) {
		if (forceVector == state)
			return;
		forceVector = state;
		model.notifyChange();
		repaint();
	}

	public boolean forceVectorShown() {
		return forceVector;
	}

	public void setShowSites(boolean b) {
		showSites = b;
	}

	public boolean getShowSites() {
		return showSites;
	}

	private boolean insertAminoAcid(int x, int y, int id) {
		if (nAtom >= atom.length)
			return false;
		int oldNOA = nAtom;
		atom[oldNOA].translateTo(x, y);
		atom[oldNOA].setElement(model.getElement(id));
		atom[oldNOA].setModel(model);
		if (model.heatBathActivated()) {
			atom[oldNOA].setRandomVelocity();
		}
		else {
			atom[oldNOA].setVx(0.0);
			atom[oldNOA].setVy(0.0);
		}
		atom[oldNOA].setRestraint(null);
		atom[oldNOA].setFriction(0);
		atom[oldNOA].setUserField(null);
		atom[oldNOA].setShowRTraj(false);
		atom[oldNOA].setShowRMean(false);
		atom[oldNOA].setShowFMean(false);
		atom[oldNOA].setColor(null);
		if (!model.getRecorderDisabled())
			atom[oldNOA].initializeMovieQ(model.getMovie().getCapacity());
		model.setNumberOfParticles(++nAtom);
		return true;
	}

	/**
	 * Do NOT call this method unless you know what it is supposed to do. This is a method used to sequentially grow a
	 * polypeptide chain from the specified location (x, y).
	 * 
	 * @return true if succeeding in growing a residue at the given position with the given angle.
	 */
	public boolean growPolypeptide(int x, int y, double angle, int id) {
		MolecularModel m = model;
		int n = m.getNumberOfAtoms();
		int k = 0;
		Object o = m.getProperty("NOA_Translation");
		if (o instanceof Integer)
			k = (Integer) o;
		if (n <= k) {
			boolean b = insertAnAtom(x, y, id, true, false);
			if (b)
				PointRestraint.tetherParticle(m.getAtom(k), 100);
			return b;
		}
		else if (n == k + 1) {
			Atom a = m.getAtom(k);
			Element e = m.getElement(id);
			double d = RadialBond.PEPTIDE_BOND_LENGTH_PARAMETER * (a.getSigma() + e.getSigma());
			a.translateBy(d * Math.cos(angle), -d * Math.sin(angle));
			if (insertAnAtom(x, y, id, true, false)) {
				m.getBonds().add(new RadialBond.Builder(a, m.getAtom(n)).bondLength(d).build());
				MoleculeCollection.sort(m);
				PointRestraint.releaseParticle(a);
				PointRestraint.tetherParticle(m.getAtom(n), 100);
				return true;
			}
			a.translateBy(-d * Math.cos(angle), d * Math.sin(angle));
			return false;
		}
		else {
			Atom a = m.getAtom(n - 1);
			Element e = m.getElement(id);
			double d = RadialBond.PEPTIDE_BOND_LENGTH_PARAMETER * (a.getSigma() + e.getSigma()) * 0.5;
			Molecule mol = m.getMolecules().getMolecule(a);
			if (mol != null)
				mol.translateBy(d * Math.cos(angle), -d * Math.sin(angle));
			if (insertAminoAcid(x, y, id)) {
				m.getBonds().add(new RadialBond.Builder(a, m.getAtom(n)).bondLength(d * 2).build());
				MoleculeCollection.sort(m);
				PointRestraint.releaseParticle(a);
				PointRestraint.tetherParticle(m.getAtom(n), 100);
				return true;
			}
			if (mol != null)
				mol.translateBy(-d * Math.cos(angle), d * Math.sin(angle));
			return false;
		}
	}

	public void removeLayeredComponent(Layered a) {
		super.removeLayeredComponent(a);
		ModelComponent c = a.getHost();
		if (c != null) {
			if (c instanceof RadialBond) {
				Molecule m = ((RadialBond) c).getMolecule();
				if (m != null)
					m.setVisible(true);
			}
			a.setHost(null);
		}
	}

	public void removeSelectedComponent() {
		if (selectedComponent == null)
			return;
		super.removeSelectedComponent();
		if (selectedComponent instanceof Atom) {
			List<Integer> list = new ArrayList<Integer>();
			list.add(((Atom) selectedComponent).getIndex());
			selectedComponent.setSelected(false);
			removeMarkedAtoms(list);
			pasteBuffer = atomBufferArray[atomBufferArray.length - 1];
			repaint();
			model.setNumberOfParticles(nAtom);
			if (!doNotFireUndoEvent) {
				int id = UndoAction.BLOCK_REMOVE;
				if (actionID == SACD_ID)
					id = actionID;
				model.getUndoManager().undoableEditHappened(new UndoableEditEvent(model, new UndoableDeletion(id, 1)));
				updateUndoUIComponents();
			}
		}
		else if (selectedComponent instanceof Molecule) {
			restoreMolecule = true;
			Molecule mol = (Molecule) selectedComponent;
			int imol = molecules.indexOf(mol);
			molecules.remove(mol);
			List<Integer> list = new ArrayList<Integer>();
			synchronized (mol.getSynchronizedLock()) {
				for (Iterator it = mol.iterator(); it.hasNext();) {
					list.add(((Atom) it.next()).getIndex());
				}
			}
			if (selectedComponent instanceof Polypeptide) {
				model.notifyModelListeners(new ModelEvent(selectedComponent, ModelEvent.COMPONENT_REMOVED, null,
						new Integer(imol)));
			}
			removeMarkedAtoms(list);
			if (!list.isEmpty()) {
				pasteBuffer = mol;
				molecules.clearSelection();
				repaint();
				model.setNumberOfParticles(nAtom);
			}
			if (!doNotFireUndoEvent) {
				model.getUndoManager().undoableEditHappened(
						new UndoableEditEvent(model, new UndoableDeletion(UndoAction.BLOCK_REMOVE, list.size())));
				updateUndoUIComponents();
			}
		}
		else if (selectedComponent instanceof RectangularObstacle) {
			RectangularObstacle r = (RectangularObstacle) selectedComponent;
			obstacles.remove(r);
			if (!model.getRecorderDisabled())
				r.initializeMovieQ(-1);
			pasteBuffer = selectedComponent;
			if (!doNotFireUndoEvent) {
				model.getUndoManager().undoableEditHappened(
						new UndoableEditEvent(model,
								new UndoableDeletion(UndoAction.REMOVE_OBSTACLE, selectedComponent)));
				updateUndoUIComponents();
			}
		}
		else if (selectedComponent instanceof RadialBond) {
			bonds.remove((RadialBond) selectedComponent);
			bondChanged(null);
			List ab = removeAssociatedBends((RadialBond) selectedComponent);
			MoleculeCollection.sort(model);
			if (!doNotFireUndoEvent) {
				model.getUndoManager().undoableEditHappened(
						new UndoableEditEvent(model, new UndoableDeletion(UndoAction.REMOVE_RADIAL_BOND, new Object[] {
								selectedComponent, ab })));
				updateUndoUIComponents();
			}
		}
		else if (selectedComponent instanceof AngularBond) {
			bends.remove((AngularBond) selectedComponent);
			if (!doNotFireUndoEvent) {
				model.getUndoManager().undoableEditHappened(
						new UndoableEditEvent(model, new UndoableDeletion(UndoAction.REMOVE_ANGULAR_BOND,
								selectedComponent)));
				updateUndoUIComponents();
			}
		}
		if (selectedComponent != null) {
			selectedComponent.setSelected(false);
			selectedComponent = null;
		}
		repaint();
		model.notifyChange();
	}

	public void setIsoCurves(Vector[] v) {
		isoCurves = v;
	}

	public Vector[] getIsoCurves() {
		return isoCurves;
	}

	public void setGrid(Grid grid) {
		if (grid != null) {
			addComponentListener(grid);
		}
		else {
			removeComponentListener(this.grid);
		}
		this.grid = grid;
	}

	public Grid getGrid() {
		return grid;
	}

	public void setGridMode(byte value) {
		gridMode = value;
	}

	public byte getGridMode() {
		return gridMode;
	}

	public void showEFieldLines(boolean state, int cellSize) {
		eFieldLines = state;
		if (state) {
			if (eField == null)
				eField = new ElectricForceField();
			eField.setCellSize(cellSize);
		}
	}

	public boolean eFieldLinesShown() {
		return eFieldLines;
	}

	public int getCellSizeForEFieldLines() {
		if (eField == null)
			return 10;
		return eField.getCellSize();
	}

	public void setProbeAtom(Atom probe) {
		this.probe = probe;
	}

	public Atom getProbeAtom() {
		return probe;
	}

	public void showContourPlot(boolean state, Atom a) {
		showContourPlot(state, a, getSize());
	}

	public void showContourPlot(boolean state, Atom a, Dimension dim) {
		contourPlot = state;
		if (contourPlot) {
			if (a == null)
				return;
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				}
			});
			// model.stopImmediately();
			synchronized (contourLock) {
				probe = a;
				contour.setProbe(probe, dim.width, dim.height);
				contour.setConstant(model.getUniverse().getCoulombConstant(), model.getUniverse()
						.getDielectricConstant());
				contourMap = new ContourMap(dim.width / contour.getCellSize(), dim.height / contour.getCellSize(),
						contour.getContour(nAtom, atom, boundary.getType()));
				// contourMap.setNLevels(model.checkCharges()? 20 : 10);
				contourMap.setNLevels(20);
				contourMap.setRange(0, dim.width, 0, dim.height);
				contourMap.setLogLevels(false);
				contourMap.setExpLevels(false);
				setIsoCurves(contourMap.getCurves());
			}
			repaint();
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				}
			});
		}
	}

	public boolean contourPlotShown() {
		return contourPlot;
	}

	JPopupMenu[] getPopupMenus() {
		return new JPopupMenu[] { defaultPopupMenu, atomPopupMenu, radialBondPopupMenu, angularBondPopupMenu,
				moleculePopupMenu, obstaclePopupMenu, molecularObjectPopupMenu, popupMenuForLayeredComponent,
				acidPopupMenu, nucleotidePopupMenu, atomMutationPopupMenu };
	}

	private boolean insertAnAtom(int x, int y, int id) {
		return insertAnAtom(x, y, id, false, false);
	}

	public boolean insertAnAtom(double x, double y, int id, boolean silent) {
		return insertAnAtom(x, y, id, silent, true);
	}

	/**
	 * insert an atom of type <tt>id</tt> to a given position.
	 * 
	 * @param x
	 *            x coordinate of the position (in pixels)
	 * @param y
	 *            y coordinate of the position (in pixels)
	 * @param id
	 *            id of the inserted atom
	 * @param silent
	 *            suppress all error reminders and action/undo notifiers
	 * @param noOverlapTolerance
	 *            true if no overlap is tolerated
	 * @return 'true' if the action is done, 'false' if not
	 */
	public boolean insertAnAtom(double x, double y, int id, boolean silent, boolean noOverlapTolerance) {
		if (nAtom >= atom.length) {
			if (!silent)
				errorReminder.show(ErrorReminder.EXCEED_CAPACITY);
			return false;
		}
		if (boundary.contains(x, y)) {
			int oldNOA = nAtom;
			atom[oldNOA].translateTo(x, y);
			atom[oldNOA].setElement(model.getElement(id));
			if (finalizeAtomLocation(atom[oldNOA], noOverlapTolerance)) {
				atom[oldNOA].setModel(model);
				if (model.heatBathActivated()) {
					atom[oldNOA].setRandomVelocity();
				}
				else {
					atom[oldNOA].setVx(0.0);
					atom[oldNOA].setVy(0.0);
				}
				atom[oldNOA].setRestraint(null);
				atom[oldNOA].setRadical(true);
				atom[oldNOA].setFriction(0);
				atom[oldNOA].setUserField(null);
				atom[oldNOA].setShowRTraj(false);
				atom[oldNOA].setShowRMean(false);
				atom[oldNOA].setShowFMean(false);
				atom[oldNOA].setColor(null);
				if (!model.getRecorderDisabled())
					atom[oldNOA].initializeMovieQ(model.getMovie().getCapacity());
				model.setNumberOfParticles(++nAtom);
				if (!silent) {
					atom[oldNOA].setSelected(true);
					model.notifyChange();
					if (!doNotFireUndoEvent) {
						model.getUndoManager()
								.undoableEditHappened(
										new UndoableEditEvent(model, new UndoableInsertion(
												UndoAction.INSERT_A_PARTICLE, x, y)));
						updateUndoUIComponents();
					}
				}
				return true;
			}
			if (!silent)
				errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
		}
		else {
			if (!silent)
				errorReminder.show(ErrorReminder.OUT_OF_BOUND);
		}
		return false;
	}

	/**
	 * insert a molecule so that its center of mass locates at the given position.
	 * 
	 * @param x
	 *            x coordinate of the position
	 * @param y
	 *            y coordinate of the position
	 * @param molecule
	 *            a specified molecule
	 * @return 'true' if the action is done, 'false' if not
	 * @see org.concord.mw2d.models.Benzene
	 * @see org.concord.mw2d.models.ChainMolecule
	 * @see org.concord.mw2d.models.DiatomicMolecule
	 * @see org.concord.mw2d.models.TriatomicMolecule
	 */
	public boolean insertAMolecule(double x, double y, Molecule molecule) {
		if (model.getNumberOfParticles() >= atom.length - molecule.size()) {
			errorReminder.show(ErrorReminder.EXCEED_CAPACITY);
			return false;
		}
		int oldNOA = nAtom;
		molecule.init(model);
		molecule.translateTo(x, y);
		int noa = model.getNumberOfParticles();
		for (int i = oldNOA; i < noa; i++) {
			if (!boundary.contains(atom[i].getRx(), atom[i].getRy())) {
				errorReminder.show(ErrorReminder.OUT_OF_BOUND);
				model.setNumberOfParticles(oldNOA);
				nAtom = oldNOA;
				return false;
			}
		}
		if (intersects(molecule)) {
			errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
			model.setNumberOfParticles(oldNOA);
			nAtom = oldNOA;
			return false;
		}
		bonds.addAll(molecule.buildBonds(model));
		bends.addAll(molecule.buildBends(model));
		molecules.add(molecule);
		// FIXME: the torque implementation is ugly
		int i = molecules.size() - 1;
		model.removeProperty("torque_mol" + i);
		model.removeProperty("rotor_mol" + i);
		nAtom = noa;
		molecule.setSelected(true);
		if (useJmol)
			notifyJmol();
		repaint();
		model.notifyChange();
		if (!doNotFireUndoEvent) {
			model.getUndoManager().undoableEditHappened(
					new UndoableEditEvent(model, new UndoableInsertion(actionID, x, y)));
			updateUndoUIComponents();
		}
		return true;
	}

	/**
	 * remove the atoms whose indices are given by the list, and the associated bonds. This method involves using
	 * another atom array of the same length with the main one, to back up the main atom array for restoring later for
	 * purposes such as undo/redoing. The deleted atoms in the action are placed at the rear end of the backup array.
	 * The retained atoms are trimmed and re-aligned from the first position of the backup array. The equal length of
	 * the backup array ensures that both the retained and removed atoms can be put into it. When reverting this
	 * deletion action, look for the deleted atoms from the rear end of the backup array.
	 */
	public void removeMarkedAtoms(List<Integer> list) {

		if (selectedComponent != null) {
			selectedComponent.setSelected(false);
			selectedComponent = null;
		}

		if (atomBufferArray == null)
			atomBufferArray = new Atom[MolecularModel.getMaximumNumberOfAtoms()];
		// pasteBuffer=null;
		// if deleted atoms is a single atom or a whole molecule, set the paste buffer in the subsquent action

		liveAtomMap.clear();
		deadAtomMap.clear();
		deadLayered.clear();
		int temp = 0, temq = 0, ii = 0;
		for (int i = 0; i < nAtom; i++) {
			if (!list.contains(i)) {
				int nMeasure = atom[i].getNumberOfMeasurements();
				if (nMeasure > 0) {
					Object measure;
					for (int nm = 0; nm < nMeasure; nm++) {
						measure = atom[i].getMeasurement(nm);
						if (measure instanceof Integer && list.contains(measure)) {
							atom[i].removeMeasurement(nm);
						}
					}
				}
				if (atomBufferArray[temp] == null) {
					atomBufferArray[temp] = new Atom(model.getElement(Element.ID_NT));
					atomBufferArray[temp].setModel(model);
					atomBufferArray[temp].setIndex(temp);
				}
				atomBufferArray[temp].set(atom[i]);
				atomBufferArray[temp].setSelected(atom[i].isSelected());
				atomBufferArray[temp].setUserField(atom[i].getUserField());
				List<Layered> l = getLayeredComponentHostedBy(atom[i]);
				if (l != null) {
					for (Layered c : l)
						c.setHost(atomBufferArray[temp]);
				}
				// map the old indices of the surviving atoms to the new ones
				liveAtomMap.put(i, temp);
				temp++;
			}
			else {
				List<Layered> l = getLayeredComponentHostedBy(atom[i]);
				if (l != null) {
					for (Layered c : l) {
						c.setHost(null);
						layerBasket.remove(c); // WHAT IS THE IMPACT of this??
					}
				}
				ii = atom.length - 1 - temq;
				if (atomBufferArray[ii] == null) {
					atomBufferArray[ii] = new Atom(model.getElement(Element.ID_NT));
					atomBufferArray[ii].setModel(model);
					atomBufferArray[ii].setIndex(ii);
				}
				atomBufferArray[ii].set(atom[i]);
				atomBufferArray[ii].setUserField(atom[i].getUserField());
				atom[i].eraseProperties();
				// map the original indices of the dead atoms to those of their backups
				deadAtomMap.put(i, ii);
				if (l != null)
					deadLayered.put(atomBufferArray[ii], l);
				temq++;
			}
		}
		for (int i = temp; i < nAtom; i++)
			atom[i].erase();
		nAtom = temp;
		for (int i = 0; i < nAtom; i++) {
			atom[i].set(atomBufferArray[i]);
			atom[i].setSelected(atomBufferArray[i].isSelected());
			atom[i].setUserField(atomBufferArray[i].getUserField());
			List<Layered> l = getLayeredComponentHostedBy(atomBufferArray[i]);
			if (l != null) {
				for (Layered c : l)
					c.setHost(atom[i]);
			}
		}
		model.setNumberOfParticles(nAtom);
		model.computeForce(-1);

		deadBonds.clear();
		deadBends.clear();
		if (bonds.isEmpty())
			return;

		/* remove the associated radial bonds. */

		Set liveSet = liveAtomMap.keySet();
		Set deadSet = deadAtomMap.keySet();
		RadialBond rBond = null;
		Integer origin, destin;
		synchronized (bonds.getSynchronizationLock()) {
			for (Iterator i = bonds.iterator(); i.hasNext();) {
				rBond = (RadialBond) i.next();
				origin = rBond.getAtom1().getIndex();
				destin = rBond.getAtom2().getIndex();
				if (liveSet.contains(origin) && liveSet.contains(destin)) {
					rBond.setAtom1(atom[liveAtomMap.get(origin)]);
					rBond.setAtom2(atom[liveAtomMap.get(destin)]);
				}
				else {
					i.remove();
					List<Layered> l = getLayeredComponentHostedBy(rBond);
					if (l != null) {
						for (Layered x : l)
							layerBasket.remove(x);
					}
					/*
					 * store dead bonds for undoing: if a dead bond involves a live atom, rise that atomic index of the
					 * bond delegate to the ghost area. The ghost area starts from the length of the atom array, so
					 * there will not be any indicing conflict. Remember this when restoring the dead bonds.
					 */
					if (deadSet.contains(origin) && deadSet.contains(destin)) {
						if (model instanceof ReactionModel) {
							atom[deadAtomMap.get(origin)].setRadical(true);
							atom[deadAtomMap.get(destin)].setRadical(true);
						}
						RadialBond.Delegate d = addDeadBond(deadAtomMap.get(origin), deadAtomMap.get(destin), rBond);
						if (l != null)
							deadLayered.put(d, l);
					}
					else if (deadSet.contains(origin) && liveSet.contains(destin)) {
						if (model instanceof ReactionModel) {
							atom[deadAtomMap.get(origin)].setRadical(true);
							atom[liveAtomMap.get(destin)].setRadical(true);
						}
						RadialBond.Delegate d = addDeadBond(deadAtomMap.get(origin), liveAtomMap.get(destin)
								+ atom.length, rBond);
						if (l != null)
							deadLayered.put(d, l);
					}
					else if (liveSet.contains(origin) && deadSet.contains(destin)) {
						if (model instanceof ReactionModel) {
							atom[liveAtomMap.get(origin)].setRadical(true);
							atom[deadAtomMap.get(destin)].setRadical(true);
						}
						RadialBond.Delegate d = addDeadBond(liveAtomMap.get(origin) + atom.length, deadAtomMap
								.get(destin), rBond);
						if (l != null)
							deadLayered.put(d, l);
					}
				}
			}
		}

		if (!deadBonds.isEmpty()) {
			if (useJmol && jmolRenderer != null) {
				jmolRenderer.renderBonds();
			}
		}

		MoleculeCollection.sort(model);

		/* remove the associated angular bonds */

		if (bends.isEmpty())
			return;
		AngularBond aBond = null;
		Integer middle;
		synchronized (bends.getSynchronizationLock()) {
			for (Iterator i = bends.iterator(); i.hasNext();) {
				aBond = (AngularBond) i.next();
				origin = aBond.getAtom1().getIndex();
				destin = aBond.getAtom2().getIndex();
				middle = aBond.getAtom3().getIndex();
				if (liveSet.contains(origin) && liveSet.contains(destin) && liveSet.contains(middle)) {
					aBond.setAtom1(atom[liveAtomMap.get(origin)]);
					aBond.setAtom2(atom[liveAtomMap.get(destin)]);
					aBond.setAtom3(atom[liveAtomMap.get(middle)]);
				}
				else {
					i.remove();
					/* explanation see the corresponding code of radial bonds */
					if (deadSet.contains(origin) && deadSet.contains(destin) && deadSet.contains(middle)) {
						addDeadBend(deadAtomMap.get(origin), deadAtomMap.get(destin), deadAtomMap.get(middle), aBond);
					}
					else if (liveSet.contains(origin) && deadSet.contains(destin) && deadSet.contains(middle)) {
						addDeadBend(liveAtomMap.get(origin) + atom.length, deadAtomMap.get(destin), deadAtomMap
								.get(middle), aBond);
					}
					else if (deadSet.contains(origin) && liveSet.contains(destin) && deadSet.contains(middle)) {
						addDeadBend(deadAtomMap.get(origin), liveAtomMap.get(destin) + atom.length, deadAtomMap
								.get(middle), aBond);
					}
					else if (deadSet.contains(origin) && deadSet.contains(destin) && liveSet.contains(middle)) {
						addDeadBend(deadAtomMap.get(origin), deadAtomMap.get(destin), liveAtomMap.get(middle)
								+ atom.length, aBond);
					}
					else if (liveSet.contains(origin) && liveSet.contains(destin) && deadSet.contains(middle)) {
						addDeadBend(liveAtomMap.get(origin) + atom.length, liveAtomMap.get(destin) + atom.length,
								deadAtomMap.get(middle), aBond);
					}
					else if (liveSet.contains(origin) && deadSet.contains(destin) && liveSet.contains(middle)) {
						addDeadBend(liveAtomMap.get(origin) + atom.length, deadAtomMap.get(destin), liveAtomMap
								.get(middle)
								+ atom.length, aBond);
					}
					else if (deadSet.contains(origin) && liveSet.contains(destin) && liveSet.contains(middle)) {
						addDeadBend(deadAtomMap.get(origin), liveAtomMap.get(destin) + atom.length, liveAtomMap
								.get(middle)
								+ atom.length, aBond);
					}
				}
			}
		}

		if (molecules == null || molecules.isEmpty())
			setShowSites(false);
		boolean b = false;
		synchronized (molecules.getSynchronizationLock()) {
			for (Iterator it = molecules.iterator(); it.hasNext();) {
				if (it.next() instanceof MolecularObject) {
					b = true;
					break;
				}
			}
		}
		if (!b)
			setShowSites(false);

		// any non-bonded atom is supposed to be a radical
		if (model instanceof ReactionModel) {
			((ReactionModel) model).reassertFreeRadicals();
		}

	}

	private RadialBond.Delegate addDeadBond(int i, int j, RadialBond r) {
		RadialBond.Delegate d = new RadialBond.Delegate(i, j, r.getBondLength(), r.getBondStrength(), r.isSmart(), r
				.isSolid(), r.isClosed());
		d.setColor(r.getBondColor());
		d.setStyle(r.getBondStyle());
		d.setVisible(r.isVisible());
		if (r.getAmplitude() > 0) {
			d.setAmplitude(r.getAmplitude());
			d.setPeriod(r.getPeriod());
			d.setPhase(r.getPhase());
		}
		d.setTorqueType(r.getTorqueType());
		d.setTorque(r.getTorque());
		d.setCustom(r.getCustom());
		deadBonds.add(d);
		return d;
	}

	private RadialBond restoreDeadBond(int i, int j, RadialBond.Delegate d) {
		RadialBond rb = new RadialBond.Builder(atom[i], atom[j]).bondLength(d.getBondLength()).bondStrength(
				d.getBondStrength()).smart(d.isSmart()).solid(d.isSolid()).closed(d.isClosed()).build();
		rb.setBondColor(d.getColor());
		rb.setVisible(d.isVisible());
		if (d.getAmplitude() > 0) {
			rb.setAmplitude(d.getAmplitude());
			rb.setPeriod(d.getPeriod());
			rb.setPhase(d.getPhase());
		}
		rb.setTorque(d.getTorque());
		rb.setTorqueType(d.getTorqueType());
		List<Layered> l = deadLayered.get(d);
		if (l != null) {
			for (Layered x : l) {
				layerBasket.add(x);
				x.setHost(rb);
			}
		}
		bonds.add(rb);
		return rb;
	}

	private AngularBond.Delegate addDeadBend(int i, int j, int k, AngularBond a) {
		AngularBond.Delegate d = new AngularBond.Delegate(i, j, k, a.getBondAngle(), a.getBondStrength());
		deadBends.add(d);
		return d;
	}

	private AngularBond restoreDeadBend(int i, int j, int k, AngularBond.Delegate d) {
		AngularBond a = new AngularBond(atom[i], atom[j], atom[k], d.getBondAngle(), d.getBondStrength());
		bends.add(a);
		return a;
	}

	/* remove any obstacles intersecting the selected area */
	List<RectangularObstacle> removeSelectedObstacles() {
		if (obstacles == null || obstacles.isEmpty())
			return null;
		List<RectangularObstacle> a = null;
		RectangularObstacle r = null;
		synchronized (obstacles.getSynchronizationLock()) {
			for (Iterator it = obstacles.iterator(); it.hasNext();) {
				r = (RectangularObstacle) it.next();
				if (r.intersects(selectedArea)) {
					if (a == null)
						a = new ArrayList<RectangularObstacle>();
					a.add(r);
					it.remove();
				}
			}
		}
		return a;
	}

	void removeSelectedArea() {

		super.removeSelectedArea();

		List lay = removeSelectedLayeredComponents();
		List obs = removeSelectedObstacles();
		List<Integer> list = null;

		// remove any atoms in the selected area
		for (int k = 0; k < nAtom; k++) {
			if (selectedArea.contains(atom[k].getRx(), atom[k].getRy())) {
				if (list == null)
					list = new ArrayList<Integer>();
				list.add(k);
			}
		}

		// remove any molecular surfaces intersecting the selected area
		Molecule mol;
		Integer i;
		synchronized (molecules.getSynchronizationLock()) {
			for (Iterator it = molecules.iterator(); it.hasNext();) {
				mol = (Molecule) it.next();
				if (mol instanceof MolecularObject) {
					if (mol.intersects(selectedArea)) {
						for (Iterator i2 = mol.iterator(); i2.hasNext();) {
							i = ((Atom) i2.next()).getIndex();
							if (list == null)
								list = new ArrayList<Integer>();
							if (!list.contains(i))
								list.add(i);
						}
					}
				}
			}
		}

		boolean atomRemoved = list != null && !list.isEmpty();
		boolean obstacleRemoved = obs != null && !obs.isEmpty();
		boolean layerRemoved = lay != null && !lay.isEmpty();

		if (atomRemoved) {
			removeMarkedAtoms(list);
			model.setNumberOfParticles(nAtom); // is this needed?
		}

		// if anything is removed, notify Page and UndoManager
		if (atomRemoved || obstacleRemoved || layerRemoved) {
			model.notifyChange();
			if (!doNotFireUndoEvent) {
				RectangularObstacle[] r2 = null;
				if (obstacleRemoved && obs != null) {
					int n = obs.size();
					if (n > 0) {
						r2 = new RectangularObstacle[n];
						for (int j = 0; j < n; j++)
							r2[j] = (RectangularObstacle) obs.get(j);
					}
				}
				Layered[] i2 = null;
				if (layerRemoved && lay != null) {
					int n = lay.size();
					if (n > 0) {
						i2 = new Layered[n];
						for (int j = 0; j < n; j++)
							i2[j] = (Layered) lay.get(j);
					}
				}
				model.getUndoManager().undoableEditHappened(
						new UndoableEditEvent(model, new UndoableDeletion(UndoAction.BLOCK_REMOVE, list != null ? list
								.size() : 0, r2, i2)));
				updateUndoUIComponents();
			}
		}
		selectedArea.setSize(0, 0);
		repaint();

	}

	public void clearSelection() {
		super.clearSelection();
		for (int i = 0; i < nAtom; i++)
			atom[i].setSelected(false);
		if (!bonds.isEmpty())
			bonds.clearSelection();
		if (!bends.isEmpty())
			bends.clearSelection();
		if (!molecules.isEmpty())
			molecules.clearSelection();
		if (!obstacles.isEmpty())
			obstacles.clearSelection();
	}

	public Color contrastBackground() {
		return gridMode <= Grid.ATOMIC ? super.contrastBackground() : Color.white;
	}

	public Atom whichAtom(int x, int y) {
		for (int i = 0; i < nAtom; i++)
			if (atom[i].contains(x, y))
				return atom[i];
		return null;
	}

	public Molecule whichMolecule(int x, int y) {
		if (selectedComponent instanceof Molecule)
			if (actionID == ROTA_ID && ((Molecule) selectedComponent).getRotationHandle(x, y) != -1)
				return (Molecule) selectedComponent;
		Atom at = whichAtom(x, y);
		if (at == null)
			return null;
		return molecules.getMolecule(at);
	}

	public MolecularObject whichMolecularObject(int x, int y) {
		Molecule mol;
		synchronized (molecules.getSynchronizationLock()) {
			for (Iterator i = molecules.iterator(); i.hasNext();) {
				mol = (Molecule) i.next();
				if (mol instanceof MolecularObject) {
					if (((MolecularObject) mol).contains(x, y))
						return (MolecularObject) mol;
				}
			}
		}
		return null;
	}

	public RadialBond whichBond(int x, int y) {
		if (bonds.isEmpty())
			return null;
		RadialBond rBond;
		synchronized (bonds.getSynchronizationLock()) {
			for (Iterator it = bonds.iterator(); it.hasNext();) {
				rBond = (RadialBond) it.next();
				if (rBond.contains(x, y))
					return rBond;
			}
		}
		return null;
	}

	public AngularBond whichAngle(int x, int y) {
		if (bends.isEmpty())
			return null;
		AngularBond aBond = null;
		synchronized (bends.getSynchronizationLock()) {
			for (Iterator it = bends.iterator(); it.hasNext();) {
				aBond = (AngularBond) it.next();
				if (aBond.contains(x, y))
					return aBond;
			}
		}
		return null;
	}

	public RectangularObstacle whichObstacle(int x, int y) {
		if (obstacles != null && !obstacles.isEmpty()) {
			RectangularObstacle r2d = null;
			synchronized (obstacles.getSynchronizationLock()) {
				for (Iterator it = obstacles.iterator(); it.hasNext();) {
					r2d = (RectangularObstacle) it.next();
					if (r2d.contains(x, y))
						return r2d;
				}
			}
		}
		return null;
	}

	/**
	 * create a radial bond with the selected atom and the atom at (x, y).
	 * 
	 * @param x
	 *            x coordinate of the position
	 * @param y
	 *            y coordinate of the position
	 * @return 'true' if the action is done, 'false' if not
	 */
	protected boolean createABond(int x, int y) {
		if (!(selectedComponent instanceof Atom))
			return false;
		Atom at0 = (Atom) selectedComponent;
		Atom at = whichAtom(x, y);
		if (at == null || at == at0)
			return false;
		if (!bonds.isEmpty()) {
			RadialBond rBond;
			Atom origin, destin;
			synchronized (bonds.getSynchronizationLock()) {
				for (Iterator it = bonds.iterator(); it.hasNext();) {
					rBond = (RadialBond) it.next();
					origin = rBond.getAtom1();
					destin = rBond.getAtom2();
					if ((at == origin && at0 == destin) || (at0 == origin && at == destin))
						return false;
				}
			}
		}
		double xij = at.getRx() - at0.getRx();
		double yij = at.getRy() - at0.getRy();
		bonds.add(new RadialBond.Builder(at0, at).bondLength(Math.hypot(xij, yij)).build());
		MoleculeCollection.sort(model);
		model.notifyChange();
		if (!doNotFireUndoEvent) {
			model.getUndoManager().undoableEditHappened(
					new UndoableEditEvent(model, new UndoableInsertion(BBON_ID, x, y)));
			updateUndoUIComponents();
		}
		repaint();
		return true;
	}

	/**
	 * create an angular bond with a preselected radial bond.
	 * 
	 * @param x
	 *            x coordinate of the position
	 * @param y
	 *            y coordinate of the position
	 * @return 'true' if the action is done, 'false' if not
	 */
	protected boolean createABend(int x, int y) {
		if (!(selectedComponent instanceof RadialBond))
			return false;
		RadialBond firstBond = (RadialBond) selectedComponent;
		int middle = -1, origin = -1, destin = -1;
		double x0, y0, x1, y1;
		Atom hold1 = firstBond.getAtom1();
		Atom hold2 = firstBond.getAtom2();
		Atom atom1, atom2;
		RadialBond rBond;
		synchronized (bonds.getSynchronizationLock()) {
			for (Iterator i = bonds.iterator(); i.hasNext();) {
				rBond = (RadialBond) i.next();
				atom1 = rBond.getAtom1();
				atom2 = rBond.getAtom2();
				if (((atom1 == hold1 && atom2 != hold2) || (atom1 != hold1 && atom2 == hold2))
						|| ((atom1 == hold2 && atom2 != hold1) || (atom1 != hold2 && atom2 == hold1))) {
					x0 = Math.min(atom1.getRx(), atom2.getRx()) - 2;
					y0 = Math.min(atom1.getRy(), atom2.getRy()) - 2;
					x1 = Math.max(atom1.getRx(), atom2.getRx()) + 2;
					y1 = Math.max(atom1.getRy(), atom2.getRy()) + 2;
					if (x >= x0 && x <= x1 && y >= y0 && y <= y1) {
						if (!rBond.equals(firstBond)) {
							if (atom1 == hold1) {
								middle = atom1.getIndex();
								origin = atom2.getIndex();
								destin = hold2.getIndex();
							}
							else if (atom1 == hold2) {
								middle = atom1.getIndex();
								origin = atom2.getIndex();
								destin = hold1.getIndex();
							}
							else if (atom2 == hold2) {
								middle = atom2.getIndex();
								origin = atom1.getIndex();
								destin = hold1.getIndex();
							}
							else if (atom2 == hold1) {
								middle = atom2.getIndex();
								origin = hold2.getIndex();
								destin = atom1.getIndex();
							}
							else {
								return false;
							}
							break;
						}
					}
				}
			}
		}
		if (middle < 0 || origin < 0 || destin < 0)
			return false;
		if (!bends.isEmpty()) {
			synchronized (bends.getSynchronizationLock()) {
				AngularBond aBond = null;
				for (Iterator it = bends.iterator(); it.hasNext();) {
					aBond = (AngularBond) it.next();
					if (aBond.contains(atom[origin]) && aBond.contains(atom[destin]) && aBond.contains(atom[middle]))
						return false;
				}
			}
		}
		double xij = atom[middle].getRx() - atom[origin].getRx();
		double yij = atom[middle].getRy() - atom[origin].getRy();
		double xjk = atom[destin].getRx() - atom[middle].getRx();
		double yjk = atom[destin].getRy() - atom[middle].getRy();
		double rij = Math.hypot(xij, yij);
		double rjk = Math.hypot(xjk, yjk);
		double angleOfBend = xij * xjk / (rij * rjk) + yij * yjk / (rij * rjk);
		angleOfBend = Math.PI - Math.acos(angleOfBend);
		AngularBond aBond = new AngularBond(atom[origin], atom[destin], atom[middle], angleOfBend);
		bends.add(aBond);
		model.notifyChange();
		if (!doNotFireUndoEvent) {
			model.getUndoManager().undoableEditHappened(
					new UndoableEditEvent(model, new UndoableInsertion(BBEN_ID, x, y)));
			updateUndoUIComponents();
		}
		repaint();
		return true;
	}

	protected void copySelectedComponent() {
		super.copySelectedComponent();
		if (selectedComponent instanceof RectangularObstacle) {
			pasteBuffer = ((RectangularObstacle) selectedComponent).clone();
		}
		else if (selectedComponent instanceof Atom) {
			pasteBuffer = selectedComponent;
		}
		else if (selectedComponent instanceof Molecule) {
			restoreMolecule = false;
			pasteBuffer = selectedComponent;
		}
		Action a = getActionMap().get(PASTE);
		if (!a.isEnabled() && pasteBuffer != null)
			a.setEnabled(true);
	}

	void prepareToUndoPositioning() {
		if (selectedComponent == null)
			return;
		if (!doNotFireUndoEvent) {
			UndoableMoving a = new UndoableMoving(selectedComponent);
			a.setPresentationName("Translation");
			model.getUndoManager().undoableEditHappened(new UndoableEditEvent(model, a));
			updateUndoUIComponents();
		}
		selectedComponent.setSelected(false);
	}

	void selectVelocity(Atom a) {
		for (int i = 0; i < nAtom; i++)
			atom[i].setVelocitySelection(false);
		if (a != null)
			a.setVelocitySelection(true);
	}

	int getNumberOfAppearingAtoms() {
		if (boundary.isPeriodic())
			return nAtomPBC;
		return nAtom;
	}

	private void renderVectors(Atom at, Graphics2D g) {
		if (at.getID() != Element.ID_MO && at.isVisible()) {
			if (velocityVector)
				at.drawVelocityVector(g);
			if (momentumVector)
				at.drawMomentumVector(g);
			if (accelerationVector)
				at.drawAccelerationVector(g);
			if (forceVector)
				at.drawForceVector(g);
			if (drawExternalForce)
				at.drawExternalForceVector(g);
		}
		if (at.velocitySelected())
			at.drawSelectedVelocityVector(g, getBackground(), readyToAdjustVelocityVector);
		at.renderMeasurements(g);
	}

	void refreshForces() {
		if (model.isRunning())
			return;
		if (forceVector || accelerationVector || showVDWLines || showChargeLines || showSSLines || showBPLines) {
			model.computeForce(-1); // refresh force vectors
		}
	}

	/*
	 * If noOverlapTolerance is true, then decline when the VDW spheres overlap. Otherwise, allow some degree of
	 * overlapping (currently 50% of the VDW diameter).
	 */
	private boolean finalizeAtomLocation(Atom a, boolean noOverlapTolerance) {
		if (a == null)
			throw new IllegalArgumentException("Null atom");
		boundary.setRBC(a);
		if (intersects(a, noOverlapTolerance)) {
			a.restoreState();
			refreshForces();
			repaint();
			return false;
		}
		refreshForces();
		return true;
	}

	private boolean finalizeAtomDuplication() {
		if (!(selectedComponent instanceof Atom))
			throw new RuntimeException("not an atom");
		if (!finalizeAtomLocation((Atom) selectedComponent, false)) {
			nAtom--;
			setSelectedComponent(null);
			repaint();
			return false;
		}
		if (!model.getRecorderDisabled())
			atom[nAtom - 1].initializeMovieQ(model.getMovie().getCapacity());
		return true;
	}

	private boolean finalizeMoleculeLocation(Molecule mol) {
		boundary.setRBC(mol);
		if (intersects(mol)) {
			errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
			mol.restoreState();
			repaint();
			return false;
		}
		return true;
	}

	private boolean finalizeMoleculeDuplication() {
		if (!(selectedComponent instanceof Molecule))
			throw new RuntimeException("target not a molecule");
		Molecule mol = (Molecule) selectedComponent;
		if (!finalizeMoleculeLocation(mol)) {
			nAtom -= mol.size();
			List list = mol.getBonds();
			if (list != null && !list.isEmpty()) {
				for (Iterator it = list.iterator(); it.hasNext();)
					bonds.remove((RadialBond) it.next());
			}
			list = mol.getBends();
			if (list != null && !list.isEmpty()) {
				for (Iterator it = list.iterator(); it.hasNext();)
					bends.remove((AngularBond) it.next());
			}
			if (molecules.contains(mol))
				molecules.remove(mol);
			setSelectedComponent(null);
			repaint();
			return false;
		}
		molecules.add(mol);
		if (!model.getRecorderDisabled())
			mol.initializeMovieQ(model.getMovie().getCapacity());
		return true;
	}

	private boolean finalizeRotation() {
		if (!(selectedComponent instanceof Rotatable))
			throw new RuntimeException("The selected component is not rotatable");
		if (selectedComponent instanceof Molecule) {
			/* check if atoms are out of bound */
			Molecule mol = (Molecule) selectedComponent;
			return finalizeMoleculeRotation(mol);
		}
		else if (selectedComponent instanceof ImageComponent) {
			ImageComponent ic = (ImageComponent) selectedComponent;
			ModelComponent host = ic.getHost();
			if (host instanceof RadialBond) {
				Molecule mol = ((RadialBond) host).getMolecule();
				boolean b = finalizeMoleculeRotation(mol);
				if (!b)
					ic.restoreState();
				return b;
			}
		}
		return true;
	}

	private boolean finalizeMoleculeRotation(Molecule mol) {
		Atom at;
		synchronized (mol.getSynchronizedLock()) {
			for (Iterator it = mol.iterator(); it.hasNext();) {
				at = (Atom) it.next();
				if (!boundary.contains(at.getRx(), at.getRy())) {
					errorReminder.show(ErrorReminder.OUT_OF_BOUND);
					mol.restoreState();
					repaint();
					return false;
				}
			}
		}
		if (intersects(mol)) {
			errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
			mol.restoreState();
			repaint();
			return false;
		}
		return true;
	}

	private boolean finalizeObstacleLocation(RectangularObstacle r2d) {
		if (!model.getBoundary().contains(r2d) || intersects(r2d)) {
			errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
			r2d.restoreState();
			repaint();
			return false;
		}
		return true;
	}

	private boolean finalizeObstacleDuplication() {
		if (!(selectedComponent instanceof RectangularObstacle))
			throw new RuntimeException("The selected component is not an obstacle");
		if (!finalizeObstacleLocation((RectangularObstacle) selectedComponent)) {
			obstacles.remove((RectangularObstacle) selectedComponent);
			setSelectedComponent(null);
			repaint();
		}
		return true;
	}

	private List removeAssociatedBends(RadialBond killedBond) {
		if (bends.isEmpty())
			return null;
		List<AngularBond> list = new ArrayList<AngularBond>();
		Atom atomOne = killedBond.getAtom1();
		Atom atomTwo = killedBond.getAtom2();
		synchronized (bends.getSynchronizationLock()) {
			for (Iterator i = bends.iterator(); i.hasNext();) {
				AngularBond aBond = (AngularBond) i.next();
				if (aBond.contains(atomOne) && aBond.contains(atomTwo)
						&& (aBond.indexOf(atomOne) == 2 || aBond.indexOf(atomTwo) == 2))
					list.add(aBond);
			}
			if (list.isEmpty())
				return null;
			for (AngularBond a : list)
				bends.remove(a);
		}
		return list;
	}

	private boolean selectRotatable(int x, int y) {
		Molecule mol = whichMolecule(x, y);
		if (mol == null)
			mol = whichMolecularObject(x, y);
		if (mol != null) {
			mol.setSelected(true);
			clickPoint.setLocation(x - mol.getRx(), y - mol.getRy());
			return true;
		}
		ModelComponent mc = whichLayeredComponent(x, y);
		if (mc instanceof Rotatable) {
			setSelectedComponent(mc);
			mc.setSelected(true);
			((Rotatable) mc).setSelectedToRotate(true);
			return true;
		}
		return false;
	}

	private boolean pasteMoleculeAt(int x, int y) {

		Molecule oldMol = (Molecule) pasteBuffer;
		int oldNOA = nAtom;
		Molecule mol = oldMol.duplicate();
		if (mol == null)
			return false;
		nAtom += mol.size();
		mol.translateTo(x, y);

		if (mol.isOutside(boundary)) {
			errorReminder.show(ErrorReminder.OUT_OF_BOUND);
			nAtom = oldNOA;
			bonds.removeAll(mol.getBonds());
			bends.removeAll(mol.getBends());
			return false;
		}

		if (intersects(mol)) {
			errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
			nAtom = oldNOA;
			bonds.removeAll(mol.getBonds());
			bends.removeAll(mol.getBends());
			return false;
		}

		/* move the index to the latest */
		if (oldMol instanceof MolecularObject) {
			if (oldMol instanceof CurvedSurface) {
				CurvedSurface cs = new CurvedSurface(mol);
				cs.setModel(model);
				cs.setSelected(true);
				molecules.add(cs);
			}
			else if (oldMol instanceof CurvedRibbon) {
				CurvedRibbon cr = new CurvedRibbon(mol);
				cr.setModel(model);
				cr.setSelected(true);
				molecules.add(cr);
			}
		}
		else {
			mol.setSelected(true);
			molecules.add(mol);
		}
		if (!model.getRecorderDisabled()) {
			int n = model.getMovie().getCapacity();
			for (int i = oldNOA; i < nAtom; i++)
				atom[i].initializeMovieQ(n);
		}
		model.setNumberOfParticles(nAtom);
		model.notifyChange();
		repaint();
		return true;

	}

	private boolean restoreRemovedMoleculeAt(int x, int y) {
		if (!(pasteBuffer instanceof Molecule))
			return false;
		int oldNOA = nAtom;
		nAtom += ((Molecule) pasteBuffer).size();
		Molecule mol = new Molecule();
		mol.setModel(model);
		int incr = 0;
		for (int i = oldNOA; i < nAtom; i++) {
			atom[i].set(atomBufferArray[atom.length - 1 - incr]);
			mol.addAtom(atom[i]);
			incr++;
		}
		if (incr == 0)
			return false;
		mol.translateTo(x, y);
		for (int i = oldNOA; i < nAtom; i++) {
			if (!boundary.contains(atom[i].getRx(), atom[i].getRy())) {
				nAtom = oldNOA;
				molecules.clearSelection();
				repaint();
				errorReminder.show(ErrorReminder.OUT_OF_BOUND);
				return false;
			}
		}
		if (intersects(mol)) {
			nAtom = oldNOA;
			molecules.clearSelection();
			repaint();
			errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
			return false;
		}
		int n = atom.length - 1 + oldNOA;
		RadialBond.Delegate rbd = null;
		if (!deadBonds.isEmpty()) {
			synchronized (deadBonds) {
				for (Iterator it = deadBonds.iterator(); it.hasNext();) {
					rbd = (RadialBond.Delegate) it.next();
					restoreDeadBond(n - rbd.getAtom1(), n - rbd.getAtom2(), rbd);
				}
			}
		}
		if (!deadBends.isEmpty()) {
			AngularBond.Delegate abd;
			synchronized (deadBends) {
				for (Iterator it = deadBends.iterator(); it.hasNext();) {
					abd = (AngularBond.Delegate) it.next();
					restoreDeadBend(n - abd.getAtom1(), n - abd.getAtom2(), n - abd.getAtom3(), abd);
				}
			}
		}
		if (rbd != null && rbd.isSmart()) {
			MolecularObject ss = rbd.isSolid() ? new CurvedSurface(mol) : new CurvedRibbon(mol);
			ss.setModel(model);
			molecules.add(ss);
			ss.setSelected(true);
		}
		else {
			if (pasteBuffer instanceof DNAStrand) {
				mol = new DNAStrand(mol);
			}
			else if (pasteBuffer instanceof Polypeptide) {
				mol = new Polypeptide(mol);
			}
			molecules.add(mol);
			mol.setSelected(true);
		}
		if (!model.getRecorderDisabled()) {
			int m = model.getMovie().getCapacity();
			for (int i = oldNOA; i < nAtom; i++)
				atom[i].initializeMovieQ(m);
		}
		model.setNumberOfParticles(nAtom);
		model.notifyChange();
		return true;
	}

	private boolean undoRemoveMarkedAtoms(int nRemoved) {
		int oldNOA = nAtom;
		nAtom += nRemoved;
		int incr = 0;
		for (int i = oldNOA; i < nAtom; i++) {
			Atom a = atomBufferArray[atom.length - 1 - incr];
			atom[i].set(a);
			atom[i].setUserField(a.getUserField());
			List<Layered> l = deadLayered.get(a);
			if (l != null) {
				for (Layered x : l) {
					layerBasket.add(x);
					x.setHost(atom[i]);
				}
			}
			incr++;
		}
		if (incr == 0)
			return false;
		int n = atom.length - 1 + oldNOA;
		RadialBond.Delegate rbd = null;
		int iAtom1, iAtom2;
		if (!deadBonds.isEmpty()) {
			synchronized (deadBonds) {
				for (Iterator it = deadBonds.iterator(); it.hasNext();) {
					rbd = (RadialBond.Delegate) it.next();
					iAtom1 = rbd.getAtom1();
					iAtom2 = rbd.getAtom2();
					if (iAtom1 < atom.length)
						iAtom1 = n - iAtom1;
					else iAtom1 -= atom.length;
					if (iAtom2 < atom.length)
						iAtom2 = n - iAtom2;
					else iAtom2 -= atom.length;
					restoreDeadBond(iAtom1, iAtom2, rbd);
				}
			}
		}
		if (!deadBends.isEmpty()) {
			int iAtom3;
			AngularBond.Delegate abd;
			synchronized (deadBends) {
				for (Iterator it = deadBends.iterator(); it.hasNext();) {
					abd = (AngularBond.Delegate) it.next();
					iAtom1 = abd.getAtom1();
					iAtom2 = abd.getAtom2();
					iAtom3 = abd.getAtom3();
					if (iAtom1 < atom.length)
						iAtom1 = n - iAtom1;
					else iAtom1 -= atom.length;
					if (iAtom2 < atom.length)
						iAtom2 = n - iAtom2;
					else iAtom2 -= atom.length;
					if (iAtom3 < atom.length)
						iAtom3 = n - iAtom3;
					else iAtom3 -= atom.length;
					restoreDeadBend(iAtom1, iAtom2, iAtom3, abd);
				}
			}
		}
		if (!model.getRecorderDisabled()) {
			int m = model.getMovie().getCapacity();
			for (int i = oldNOA; i < nAtom; i++)
				atom[i].initializeMovieQ(m);
		}
		model.setNumberOfParticles(nAtom);
		MoleculeCollection.sort(model);
		if (nRemoved == 1) {
			model.getAtom(nAtom - 1).setSelected(true);
		}
		else {
			Molecule m1 = molecules.getMolecule(model.getAtom(nAtom - 1));
			Molecule m2 = molecules.getMolecule(model.getAtom(nAtom - nRemoved));
			if (m1 != null && m1 == m2)
				m1.setSelected(true);
		}
		return true;
	}

	private void fillAreaWithAtoms(Rectangle area, int lattice, int id) {
		Element e = model.getElement(id);
		if (e == null)
			throw new IllegalArgumentException("atom id incorrect");
		double sigma = e.getSigma();
		int k = model.getNumberOfParticles();
		if (k >= atom.length) {
			errorReminder.show(ErrorReminder.EXCEED_CAPACITY);
			return;
		}
		int k0 = k;
		double w = area.width;
		double h = area.height;
		double x = area.x;
		double y = area.y;
		switch (lattice) {
		case SQUARE_LATTICE:
			double spacing = sigma * TWO_TO_ONE_SIXTH;
			int m = (int) Math.round(w / spacing);
			int n = (int) Math.round(h / spacing);
			double rx,
			ry;
			terminate: for (int i = 0; i < m; i++) {
				for (int j = 0; j < n; j++) {
					rx = x + i * spacing;
					ry = y + j * spacing;
					if (rx - 0.5 * sigma >= x && rx + 0.5 * sigma < x + w && ry - 0.5 * sigma >= y
							&& ry + 0.5 * sigma < y + h) {
						atom[k].setRx(rx);
						atom[k].setRy(ry);
						atom[k].setElement(e);
						atom[k].setCharge(0);
						atom[k].setRestraint(null);
						atom[k].setFriction(0);
						atom[k].setRadical(true);
						atom[k].setUserField(null);
						k++;
						if (k >= atom.length) {
							errorReminder.show(ErrorReminder.EXCEED_CAPACITY);
							break terminate;
						}
					}
				}
			}
			break;
		case HEXAGONAL_LATTICE:
			double COS30 = 0.5 * Math.sqrt(3.0);
			double xspacing = sigma * TWO_TO_ONE_SIXTH;
			double yspacing = xspacing * COS30;
			m = (int) Math.round(w / xspacing);
			n = (int) Math.round(h / yspacing);
			terminate: for (int i = 0; i < n; i++) {
				for (int j = 0; j < m; j++) {
					if (i % 2 == 0) {
						rx = x + j * xspacing;
					}
					else {
						rx = x + (j + 0.5) * xspacing;
					}
					ry = y + i * yspacing;
					if (rx - 0.5 * sigma >= x && rx + 0.5 * sigma < x + w && ry - 0.5 * sigma >= y
							&& ry + 0.5 * sigma < y + h) {
						atom[k].setRx(rx);
						atom[k].setRy(ry);
						atom[k].setElement(e);
						atom[k].setCharge(0);
						atom[k].setRestraint(null);
						atom[k].setFriction(0);
						atom[k].setRadical(true);
						atom[k].setUserField(null);
						k++;
						if (k >= atom.length) {
							errorReminder.show(ErrorReminder.EXCEED_CAPACITY);
							break terminate;
						}
					}
				}
			}
			break;
		}
		model.setNumberOfAtoms(k);
		model.notifyChange();
		if (!doNotFireUndoEvent) {
			model.getUndoManager().undoableEditHappened(
					new UndoableEditEvent(model, new UndoableInsertion(UndoAction.FILL_AREA_WITH_PARTICLES, x, y, k
							- k0, area, lattice, id)));
			updateUndoUIComponents();
		}
	}

	/** paints the mirror bonds for molecules under periodic boundary conditions. */
	private void paintMirrorBonds(Graphics2D g2) {
		if (!showMirrorImages)
			return;
		g2.setColor(contrastBackground());
		g2.setStroke(ViewAttribute.THIN_DOTTED);
		float len, cos, sin, x1, x2, y1, y2;
		Line2D.Float l2d;
		synchronized (mirrorBonds) {
			for (Iterator it = mirrorBonds.iterator(); it.hasNext();) {
				l2d = (Line2D.Float) it.next();
				x1 = l2d.x1;
				x2 = l2d.x2;
				y1 = l2d.y1;
				y2 = l2d.y2;
				len = (float) Math.hypot(x1 - x2, y1 - y2);
				cos = (x2 - x1) / len;
				sin = (y2 - y1) / len;
				g2.drawLine((int) (x2 - sin * 2), (int) (y2 + cos * 2), (int) (x1 - sin * 2), (int) (y1 + cos * 2));
				g2.drawLine((int) (x2 + sin * 2), (int) (y2 - cos * 2), (int) (x1 + sin * 2), (int) (y1 - cos * 2));
			}
		}
	}

	public boolean intersects(RectangularObstacle obs) {
		if (obstacles != null && !obstacles.isEmpty()) {
			RectangularObstacle r;
			synchronized (obstacles.getSynchronizationLock()) {
				for (Iterator it = obstacles.iterator(); it.hasNext();) {
					r = (RectangularObstacle) it.next();
					if (!r.equals(obs) && r.intersects(obs))
						return true;
				}
			}
		}
		for (int i = 0; i < nAtom; i++) {
			if (!obs.isPermeable((byte) atom[i].getID()) && atom[i].intersects(obs))
				return true;
		}
		if (!molecules.isEmpty()) {
			Object obj = null;
			synchronized (molecules.getSynchronizationLock()) {
				for (Iterator it = molecules.iterator(); it.hasNext();) {
					obj = it.next();
					if (obj instanceof CurvedSurface) {
						if (((CurvedSurface) obj).intersects(obs.getX(), obs.getY(), obs.getWidth(), obs.getHeight()))
							return true;
					}
				}
			}
		}
		return false;
	}

	private boolean intersects(Atom a, boolean noOverlapTolerance) {
		if (obstacles != null && !obstacles.isEmpty()) {
			RectangularObstacle obs;
			synchronized (obstacles.getSynchronizationLock()) {
				for (Iterator it = obstacles.iterator(); it.hasNext();) {
					obs = (RectangularObstacle) it.next();
					if (!obs.isPermeable((byte) a.getID()) && obs.intersects(a.getBounds2D()))
						return true;
				}
			}
		}
		for (int i = 0; i < nAtom; i++)
			if (atom[i] != a && atom[i].intersects(a, noOverlapTolerance))
				return true;
		Object obj = null;
		if (!molecules.isEmpty()) {
			synchronized (molecules.getSynchronizationLock()) {
				for (Iterator it = molecules.iterator(); it.hasNext();) {
					obj = it.next();
					if (obj instanceof CurvedSurface) {
						if (((CurvedSurface) obj).contains(a.getRx(), a.getRy()))
							return true;
					}
				}
			}
		}
		return false;
	}

	private boolean intersects(Line2D line) {
		for (int i = 0; i < nAtom; i++) {
			if (line.intersects(atom[i].getBounds2D()))
				return true;
		}
		RectangularObstacle r2d;
		if (obstacles != null && obstacles.size() > 0) {
			synchronized (obstacles.getSynchronizationLock()) {
				for (Iterator it = obstacles.iterator(); it.hasNext();) {
					r2d = (RectangularObstacle) it.next();
					if (line.intersects(r2d))
						return true;
				}
			}
		}
		if (!molecules.isEmpty()) {
			Object obj = null;
			synchronized (molecules.getSynchronizationLock()) {
				for (Iterator it = molecules.iterator(); it.hasNext();) {
					obj = it.next();
					if (obj instanceof CurvedSurface) {
						if (((CurvedSurface) obj).intersects(line.getBounds()))
							return true;
					}
				}
			}
		}
		return false;
	}

	private boolean intersects(Polygon p) {
		double[] xy = new double[6];
		PathIterator pi = p.getPathIterator(null);
		if (tempLine == null)
			tempLine = new Line2D.Double();
		int count = 0;
		double x1 = 0, y1 = 0;
		while (!pi.isDone()) {
			int type = pi.currentSegment(xy);
			if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO) {
				if (count == 0) {
					x1 = xy[0];
					y1 = xy[1];
				}
				else {
					tempLine.x1 = x1;
					tempLine.y1 = y1;
					x1 = xy[0];
					y1 = xy[1];
					tempLine.x2 = x1;
					tempLine.y2 = y1;
					if (intersects(tempLine))
						return true;
				}
			}
			count++;
			pi.next();
		}
		return false;
	}

	private boolean intersects(Molecule mol) {
		RectangularObstacle r2d;
		Rectangle2D.Double molBox = new Rectangle2D.Double();
		molBox.setRect(mol.getBounds2D());
		if (obstacles != null && obstacles.size() > 0) {
			synchronized (obstacles.getSynchronizationLock()) {
				for (Iterator it = obstacles.iterator(); it.hasNext();) {
					r2d = (RectangularObstacle) it.next();
					if (r2d.intersects(molBox))
						return true;
				}
			}
		}
		if (!(mol instanceof MolecularObject)) {
			Atom a = null;
			synchronized (mol.getSynchronizedLock()) {
				for (Iterator it = mol.iterator(); it.hasNext();) {
					a = (Atom) it.next();
					for (int i = 0; i < nAtom; i++) {
						if (a != atom[i]) {
							if (!mol.contains(atom[i])) {
								if (atom[i].intersects(a, false))
									return true;
							}
						}
					}
				}
			}
			if (!molecules.isEmpty()) {
				Object obj = null;
				synchronized (molecules.getSynchronizationLock()) {
					for (Iterator it = molecules.iterator(); it.hasNext();) {
						obj = it.next();
						if (obj instanceof CurvedSurface) {
							if (((CurvedSurface) obj).intersects(mol.getBounds()))
								return true;
						}
					}
				}
			}
		}
		else {
			for (int i = 0; i < nAtom; i++) {
				if (!mol.contains(atom[i])) {
					if (((MolecularObject) mol).intersects(atom[i]))
						return true;
				}
				else {
					Molecule mol2 = null;
					synchronized (molecules.getSynchronizationLock()) {
						for (Iterator it = molecules.iterator(); it.hasNext();) {
							mol2 = (Molecule) it.next();
							if (mol2 != mol && (mol2 instanceof MolecularObject)) {
								if (((MolecularObject) mol2).intersects(atom[i]))
									return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	private Atom whichAtomOtherThan(int x, int y, Atom a) {
		for (int i = 0; i < nAtom; i++) {
			if (atom[i] != a) {
				if (atom[i].contains(x, y))
					return atom[i];
			}
		}
		return null;
	}

	private ModelComponent getRolloverComponent(int x, int y) {
		ModelComponent lc = whichLayeredComponent(x, y);
		if (lc != null && ((Layered) lc).getLayer() == Layered.IN_FRONT_OF_PARTICLES)
			return lc;
		Atom at = whichAtom(x, y);
		if (at != null) {
			if (molecules.isEmpty())
				return at;
			Molecule mol = molecules.getMolecule(at);
			if (mol != null)
				return mol;
			return at;
		}
		MolecularObject mo = whichMolecularObject(x, y);
		if (mo != null)
			return mo;
		RectangularObstacle obs = whichObstacle(x, y);
		if (obs != null)
			return obs;
		RadialBond rBond = whichBond(x, y);
		if (rBond != null)
			return rBond;
		AngularBond aBond = whichAngle(x, y);
		if (aBond != null)
			return aBond;
		if (lc != null && ((Layered) lc).getLayer() == Layered.BEHIND_PARTICLES)
			return lc;
		return null;
	}

	private ModelComponent getSelectedComponent(int modifiers, int x, int y) {
		ModelComponent lc = null;
		if ((modifiers & MouseEvent.SHIFT_DOWN_MASK) != MouseEvent.SHIFT_DOWN_MASK) {
			lc = whichLayeredComponent(x, y);
			if (lc != null && ((Layered) lc).getLayer() == Layered.IN_FRONT_OF_PARTICLES) {
				lc.setSelected(true);
				clickPoint.setLocation(x - lc.getRx(), y - lc.getRy());
				return selectedComponent;
			}
		}
		Atom at = whichAtom(x, y);
		if (at != null) {
			if ((modifiers & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK) {
				at.setSelected(true);
				clickPoint.setLocation(x - at.getRx(), y - at.getRy());
			}
			else {
				Molecule mol = molecules.getMolecule(at);
				if (mol == null) {
					at.setSelected(true);
					clickPoint.setLocation(x - at.getRx(), y - at.getRy());
				}
				else {
					at.setSelected(false);
					if (!mol.isSelected())
						mol.setSelected(true);
					clickPoint.setLocation(x - mol.getRx(), y - mol.getRy());
				}
			}
			return selectedComponent;
		}
		// if the user clicks on the interior of a molecular object
		MolecularObject mo = whichMolecularObject(x, y);
		if (mo != null) {
			if (!mo.isSelected())
				mo.setSelected(true);
			clickPoint.setLocation(x - mo.getRx(), y - mo.getRy());
			return selectedComponent;
		}
		RectangularObstacle obs = whichObstacle(x, y);
		if (obs != null && obs.contains(x, y)) {
			clickPoint.setLocation(x - obs.getMinX(), y - obs.getMinY());
			obs.setSelected(true);
			return selectedComponent;
		}
		RadialBond rBond = whichBond(x, y);
		if (rBond != null) {
			rBond.setSelected(true);
			return selectedComponent;
		}
		AngularBond aBond = whichAngle(x, y);
		if (aBond != null) {
			aBond.setSelected(true);
			return selectedComponent;
		}
		if (lc != null && ((Layered) lc).getLayer() == Layered.BEHIND_PARTICLES) {
			lc.setSelected(true);
			clickPoint.setLocation(x - lc.getRx(), y - lc.getRy());
			return selectedComponent;
		}
		return null;
	}

	protected void pasteBufferedComponent(int x, int y) {
		super.pasteBufferedComponent(x, y);
		if (pasteBuffer instanceof RectangularObstacle) {
			RectangularObstacle pastedObstacle = (RectangularObstacle) ((RectangularObstacle) pasteBuffer).clone();
			// pastedObstacle.translateCenterTo(x, y);
			pastedObstacle.translateTo(x, y);
			if (!intersects(pastedObstacle) && boundary.contains(pastedObstacle)) {
				obstacles.add(pastedObstacle);
				if (!doNotFireUndoEvent) {
					model.getUndoManager().undoableEditHappened(
							new UndoableEditEvent(model, new UndoableInsertion(ADOB_ID)));
					updateUndoUIComponents();
				}
			}
			else {
				errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
			}
		}
		else if (pasteBuffer instanceof Atom) {
			if (insertAnAtom(x, y, ((Atom) pasteBuffer).getID()))
				copyAttachedLayeredComponents((Atom) pasteBuffer, atom[nAtom - 1]);
		}
		else if (pasteBuffer instanceof Molecule) {
			if (!restoreMolecule) {
				pasteMoleculeAt(x, y);
			}
			else {
				if (!deadBonds.isEmpty()) {
					restoreRemovedMoleculeAt(x, y);
				}
			}
			if (!doNotFireUndoEvent) {
				model.getUndoManager().undoableEditHappened(
						new UndoableEditEvent(model, new UndoableInsertion(UndoAction.INSERT_A_MOLECULE, x, y,
								((Molecule) pasteBuffer).size())));
				updateUndoUIComponents();
			}
		}
		repaint();
	}

	void mutateSelectedAtom() {
		if (!(selectedComponent instanceof Atom))
			return;
		final Atom at = (Atom) selectedComponent;
		if (at.isAminoAcid()) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					if (acidPopupMenu == null)
						acidPopupMenu = new AminoAcidPopupMenu();
					acidPopupMenu.setAtom(at);
					acidPopupMenu.show(AtomisticView.this, (int) at.getRx(), (int) at.getRy());
				}
			});
		}
		else if (at.isNucleotide() && at.getID() != Element.ID_SP) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					if (nucleotidePopupMenu == null)
						nucleotidePopupMenu = new NucleotidePopupMenu();
					nucleotidePopupMenu.setAtom(at);
					nucleotidePopupMenu.show(AtomisticView.this, (int) at.getRx(), (int) at.getRy());
				}
			});
		}
		else if (at.getID() != Element.ID_SP && at.getID() != Element.ID_MO) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					if (atomMutationPopupMenu == null)
						atomMutationPopupMenu = new AtomMutationPopupMenu();
					atomMutationPopupMenu.setAtom(at);
					atomMutationPopupMenu.show(AtomisticView.this, (int) at.getRx(), (int) at.getRy());
				}
			});
		}
	}

	// the following methods are for constructing molecular objects, which should have been redone.

	private void addPointToPolygon(Polygon p, int x, int y) {
		if (p.npoints > 0) {
			int dx, dy;
			boolean tooClose = false;
			int min = (int) (model.getElement(MolecularObject.getElement()).getSigma());
			min *= min;
			for (int i = 0; i < p.npoints; i++) {
				dx = x - p.xpoints[i];
				dy = y - p.ypoints[i];
				if (dx * dx + dy * dy < min) {
					tooClose = true;
					break;
				}
			}
			if (!tooClose)
				p.addPoint(x, y);
		}
		else {
			p.addPoint(x, y);
		}
	}

	private void updateSmartShape(int x, int y) {

		switch (actionID) {

		case SCIR_ID:
		case RCIR_ID:
			if (smartShape instanceof Ellipse) {
				Ellipse e = (Ellipse) smartShape;
				if (x > e.getX0()) {
					e.width = x - e.getX0();
					e.x = e.getX0();
				}
				else {
					e.width = e.getX0() - x;
					e.x = e.getX0() - e.width;
				}
				if (y > e.getY0()) {
					e.height = y - e.getY0();
					e.y = e.getY0();
				}
				else {
					e.height = e.getY0() - y;
					e.y = e.getY0() - e.height;
				}
			}
			break;

		case SREC_ID:
		case RREC_ID:
			if (smartShape instanceof RoundRectangle) {
				RoundRectangle rr = (RoundRectangle) smartShape;
				if (x > rr.getX0()) {
					rr.width = x - rr.getX0();
					rr.x = rr.getX0();
				}
				else {
					rr.width = rr.getX0() - x;
					rr.x = rr.getX0() - rr.width;
				}
				if (y > rr.getY0()) {
					rr.height = y - rr.getY0();
					rr.y = rr.getY0();
				}
				else {
					rr.height = rr.getY0() - y;
					rr.y = rr.getY0() - rr.height;
				}
			}
			break;

		case SFRE_ID:
			if (smartPoly == null)
				return;
			int n = smartPoly.npoints;
			if (n == 0)
				return;
			if (smartShape instanceof GeneralPath) {
				((GeneralPath) smartShape).reset();
			}
			else {
				smartShape = new GeneralPath();
			}
			((GeneralPath) smartShape).moveTo(smartPoly.xpoints[0], smartPoly.ypoints[0]);
			for (int i = 1; i < smartPoly.npoints; i++) {
				((GeneralPath) smartShape).lineTo(smartPoly.xpoints[i], smartPoly.ypoints[i]);
			}
			((GeneralPath) smartShape).lineTo(x, y);
			break;

		case SCUR_ID:
		case RCUR_ID:
			if (smartPoly == null)
				return;
			n = smartPoly.npoints;
			if (n == 0)
				return;
			if (smartShape instanceof GeneralPath) {
				((GeneralPath) smartShape).reset();
			}
			else {
				smartShape = new GeneralPath();
			}
			if (n == 1) {
				((GeneralPath) smartShape).moveTo(smartPoly.xpoints[0], smartPoly.ypoints[0]);
				((GeneralPath) smartShape).lineTo(x, y);
			}
			else {
				Polygon tempPoly = new Polygon();
				for (int i = 0; i < smartPoly.npoints; i++) {
					tempPoly.addPoint(smartPoly.xpoints[i], smartPoly.ypoints[i]);
				}
				tempPoly.addPoint(x, y);
				if (actionID == SCUR_ID) {
					CurvedSurface.createSpline(tempPoly, (GeneralPath) smartShape, 50, true);
				}
				else if (actionID == RCUR_ID) {
					CurvedRibbon.createSpline(tempPoly, (GeneralPath) smartShape, 50, false);
				}
			}
			break;

		}

	}

	private void populateSmartShape(boolean closed) {

		if (smartShape == null)
			return;

		if (model.getNumberOfParticles() >= atom.length) {
			errorReminder.show(ErrorReminder.EXCEED_CAPACITY);
			smartShape = null;
			return;
		}

		Polygon tempPoly = new Polygon();
		double vdw = model.getElement(MolecularObject.getElement()).getSigma();
		double vdwsq = vdw * vdw;

		switch (actionID) {

		case SFRE_ID:

			double dx,
			dy,
			sq1,
			sq2;
			PathIterator pi = smartShape.getPathIterator(null);
			double[] xy = new double[6];
			int nd = 0,
			nPrevSegm = 0;
			double x1 = 0.0,
			y1 = 0.0;
			while (!pi.isDone()) {
				int type = pi.currentSegment(xy);
				if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO) {
					if (tempPoly.npoints == 0) {
						tempPoly.addPoint((int) xy[0], (int) xy[1]);
					}
					else {
						nPrevSegm = tempPoly.npoints - 1; // densify the sampling points
						dx = xy[0] - tempPoly.xpoints[nPrevSegm];
						dy = xy[1] - tempPoly.ypoints[nPrevSegm];
						sq1 = dx * dx + dy * dy;
						nd = (int) (Math.sqrt(sq1) / vdw) - 1;
						for (int i = 1; i < nd; i++) {
							x1 = tempPoly.xpoints[nPrevSegm] + dx * i / nd;
							y1 = tempPoly.ypoints[nPrevSegm] + dy * i / nd;
							tempPoly.addPoint((int) x1, (int) y1);
						}
						dx = xy[0] - tempPoly.xpoints[0];
						dy = xy[1] - tempPoly.ypoints[0];
						if (dx * dx + dy * dy > vdwsq)
							tempPoly.addPoint((int) xy[0], (int) xy[1]);
					}
				}
				pi.next();
			}
			break;

		case SCUR_ID:
		case RCUR_ID:

			pi = smartShape.getPathIterator(null);
			xy = new double[6];
			Polygon poly = new Polygon();
			int type;
			while (!pi.isDone()) {
				type = pi.currentSegment(xy);
				if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO)
					poly.addPoint((int) xy[0], (int) xy[1]);
				pi.next();
			}
			CurvedSurface.shrinkPolygon(poly, 0.6 * vdw);
			tempPoly.addPoint(poly.xpoints[0], poly.ypoints[0]);
			for (int i = 1; i < poly.npoints; i++) {
				dx = tempPoly.xpoints[tempPoly.npoints - 1] - poly.xpoints[i];
				dy = tempPoly.ypoints[tempPoly.npoints - 1] - poly.ypoints[i];
				sq1 = dx * dx + dy * dy;
				if (tempPoly.npoints > 1) {
					dx = tempPoly.xpoints[0] - poly.xpoints[i];
					dy = tempPoly.ypoints[0] - poly.ypoints[i];
					sq2 = dx * dx + dy * dy;
					if (sq1 >= vdwsq && sq2 >= vdwsq)
						tempPoly.addPoint(poly.xpoints[i], poly.ypoints[i]);
				}
				else {
					if (sq1 >= vdwsq)
						tempPoly.addPoint(poly.xpoints[i], poly.ypoints[i]);
				}
			}
			break;

		case SCIR_ID:
		case RCIR_ID:

			if (smartShape instanceof Ellipse) {
				Ellipse2D el = (Ellipse2D) smartShape;
				double sigma = 0.6 * vdw;
				double w2 = el.getWidth() * 0.5 - sigma;
				double h2 = el.getHeight() * 0.5 - sigma;
				double x0 = el.getX() + w2 + sigma;
				double y0 = el.getY() + h2 + sigma;
				int n = (int) (5 * Math.PI * (w2 + h2) / vdw);
				double px = x0 + w2, py = y0;
				dx = dy = 0.0;
				double theta = 0.0;
				tempPoly.addPoint((int) px, (int) py);
				for (int i = 1; i < n - 1; i++) {
					theta = 2.0 * i / n * Math.PI;
					px = x0 + w2 * Math.cos(theta);
					py = y0 + h2 * Math.sin(theta);
					dx = px - tempPoly.xpoints[tempPoly.npoints - 1];
					dy = py - tempPoly.ypoints[tempPoly.npoints - 1];
					if (dx * dx + dy * dy >= vdwsq) {
						dx = px - tempPoly.xpoints[0];
						dy = py - tempPoly.ypoints[0];
						if (dx * dx + dy * dy >= vdwsq)
							tempPoly.addPoint((int) px, (int) py);
					}
				}
			}
			break;

		case SREC_ID:
		case RREC_ID:

			if (smartShape instanceof RoundRectangle) {
				RoundRectangle rt = (RoundRectangle) smartShape;
				double sigma = 0.6 * vdw;
				double w = rt.getWidth() - 2 * sigma;
				double h = rt.getHeight() - 2 * sigma;
				double x = rt.getX() + sigma;
				double y = rt.getY() + sigma;
				tempPoly.addPoint((int) x, (int) y);
				int n = (int) (w / vdw * 0.95);
				for (int i = 1; i < n; i++)
					tempPoly.addPoint((int) (x + w * i / n), (int) y);
				int c1 = tempPoly.npoints;
				tempPoly.addPoint((int) (x + w), (int) y);
				n = (int) (h / vdw * 0.95);
				for (int i = 1; i < n; i++)
					tempPoly.addPoint((int) (x + w), (int) (y + h * i / n));
				int c2 = tempPoly.npoints;
				tempPoly.addPoint((int) (x + w), (int) (y + h));
				for (int i = c1 - 1; i > 0; i--)
					tempPoly.addPoint(tempPoly.xpoints[i], (int) (y + h));
				tempPoly.addPoint((int) x, (int) (y + h));
				for (int i = c2 - 1; i > c1; i--)
					tempPoly.addPoint((int) x, tempPoly.ypoints[i]);
			}
			break;

		}

		int min = 3;
		switch (actionID) {
		case RCUR_ID:
			min = 2;
			/*
			 * if the path is a straight line, PathIterator will not find the intermediate points, therefore, we need to
			 * divide the line manually.
			 */
			if (tempPoly.npoints == 2) {
				int x0 = tempPoly.xpoints[0];
				int y0 = tempPoly.ypoints[0];
				int x1 = tempPoly.xpoints[1];
				int y1 = tempPoly.ypoints[1];
				float xx = x1 - x0;
				float yy = y1 - y0;
				int n = (int) Math.sqrt((xx * xx + yy * yy) / vdwsq * 0.95);
				xx /= n;
				yy /= n;
				tempPoly.reset();
				for (int i = 0; i < n; i++)
					tempPoly.addPoint((int) (x0 + i * xx), (int) (y0 + i * yy));
				tempPoly.addPoint(x1, y1);
			}
			break;
		case SCUR_ID:
			min = 3;
			break;
		}
		if (tempPoly.npoints >= min) {
			if (!isSelfCrossed(tempPoly) && isShapeFine(tempPoly, (int) (0.8 * vdw))) {
				if (!intersects(tempPoly)) {
					if (boundary.contains(tempPoly.getBounds2D())) {
						switch (actionID) {
						case SFRE_ID:
							molecules.add(closed ? new CurvedSurface(tempPoly, model) : new CurvedRibbon(tempPoly,
									model, false));
							break;
						case RCUR_ID:
							molecules.add(new CurvedRibbon(tempPoly, model, closed));
							break;
						case RCIR_ID:
						case RREC_ID:
							molecules.add(new CurvedRibbon(tempPoly, model, true));
							break;
						case SCUR_ID:
						case SCIR_ID:
						case SREC_ID:
							molecules.add(new CurvedSurface(tempPoly, model));
							break;
						}
						model.notifyChange();
						if (!doNotFireUndoEvent) {
							molecules.get(molecules.size() - 1).setSelected(true);
							model.getUndoManager().undoableEditHappened(
									new UndoableEditEvent(model, new UndoableInsertion(actionID, new Object[] {
											new Boolean(closed), tempPoly })));
							updateUndoUIComponents();
						}
					}
					else {
						errorReminder.show(ErrorReminder.OUT_OF_BOUND);
					}
				}
				else {
					errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
				}
			}
			else {
				errorReminder.show(ErrorReminder.SELF_CROSS);
			}
			smartShape = null;
		}
	}

	private void drawForceLines(Graphics2D g, List lines, Stroke stroke) {
		if (lines == null || lines.isEmpty())
			return;
		g.setColor(contrastBackground());
		g.setStroke(stroke);
		int[] pair;
		synchronized (lines) {
			for (Iterator it = lines.iterator(); it.hasNext();) {
				pair = (int[]) it.next();
				g.drawLine((int) atom[pair[0]].getRx(), (int) atom[pair[0]].getRy(), (int) atom[pair[1]].getRx(),
						(int) atom[pair[1]].getRy());
			}
		}
	}

	public void refreshJmol() {
		if (useJmol && jmolRenderer != null)
			jmolRenderer.refresh();
	}

	public boolean isVoronoiStyle() {
		return styleManager.isVoronoiStyle();
	}

	void update2(Graphics g) {

		if (repaintBlocked) {
			paintPleaseWait(g);
			return;
		}

		super.update2(g);

		Graphics2D g2 = (Graphics2D) g;

		if (grid != null && gridMode > Grid.ATOMIC) {
			boundary.drawReservoir(g2);
			grid.paint(gridMode, g2);
			if (showMirrorImages)
				boundary.paintGridMirrors(getWidth(), getHeight(), g2);
			if (getClockPainted())
				paintInfo(g2);
			return;
		}

		if (isVoronoiStyle()) {
			styleManager.renderFortune(g2);
			return;
		}

		if (gridMode == Grid.ATOMIC && grid != null) {
			float gx0 = grid.getx0();
			float gy0 = grid.gety0();
			float gdx = grid.getdx();
			float gdy = grid.getdy();
			int gnx = grid.getDivisions()[0];
			int gny = grid.getDivisions()[1];
			g2.setColor(Color.lightGray);
			g2.setStroke(ViewAttribute.THIN_DASHED);
			float gw = gnx * gdx;
			float gh = gny * gdy;
			if (tempLine == null)
				tempLine = new Line2D.Double();
			for (int i = 0; i < gny; i++) {
				tempLine.setLine(gx0, gy0 + gdy * i, gx0 + gw, gy0 + gdy * i);
				g2.draw(tempLine);
			}
			for (int i = 0; i < gnx; i++) {
				tempLine.setLine(gx0 + gdx * i, gy0, gx0 + gdx * i, gy0 + gh);
				g2.draw(tempLine);
			}
		}

		boundary.render(g2, actionID);
		boundary.drawReservoir(g2);

		if (obstacles != null && !obstacles.isEmpty()) {
			g2.setStroke(ViewAttribute.THIN);
			synchronized (obstacles.getSynchronizationLock()) {
				for (int iobs = 0, nobs = obstacles.size(); iobs < nobs; iobs++)
					obstacles.get(iobs).render(g2);
			}
		}

		if (nAtom > 0) {

			if (contourPlot && isoCurves != null) {
				g2.setStroke(ViewAttribute.THIN);
				g2.setColor(contrastBackground());
				synchronized (contourLock) {
					int numberOfCurves = isoCurves.length;
					double[] data;
					if (contourLine == null)
						contourLine = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 50);
					for (int i = 0; i < numberOfCurves; i++) {
						int itemp = isoCurves[i].size();
						for (int j = 0; j < itemp; j++) {
							data = (double[]) isoCurves[i].elementAt(j);
							if (data.length > 1) {
								contourLine.reset();
								contourLine.moveTo((float) data[0], (float) data[1]);
								for (int n = 2; n < data.length - 1; n += 2) {
									contourLine.lineTo((float) data[n], (float) data[n + 1]);
								}
								g2.draw(contourLine);
							}
						}
					}
				}
			}

			if (eFieldLines) {
				if (model.getNonLocalField(ElectricField.class.getName()) != null || model.checkCharges()) {
					if (eField == null)
						eField = new ElectricForceField();
					eField.setWindow(getWidth(), getHeight());
					eField.computeForceGrid(model);
					eField.render(g);
				}
			}

			if (showVDWLines)
				drawForceLines(g2, model.getVDWLines(), vdwLineStroke);
			if (showChargeLines)
				drawForceLines(g2, model.getChargeLines(), ViewAttribute.THICKER_DASHED);
			if (showSSLines)
				drawForceLines(g2, model.getSSLines(), ViewAttribute.DASHED);
			if (showBPLines)
				drawForceLines(g2, model.getBPLines(), ViewAttribute.DASHED);

			if (!bonds.isEmpty()) {
				synchronized (bonds.getSynchronizationLock()) {
					RadialBond rBond;
					for (Iterator i = bonds.iterator(); i.hasNext();) {
						rBond = (RadialBond) i.next();
						rBond.render(g2);
					}
					if (boundary.isPeriodic() && showMirrorImages) {
						mirrorBonds = boundary.createMirrorBonds();
						paintMirrorBonds(g2);
					}
				}
			}
			if (!bends.isEmpty()) {
				synchronized (bends.getSynchronizationLock()) {
					if (actionID == BBEN_ID) {
						for (Iterator i = bends.iterator(); i.hasNext();) {
							((AngularBond) i.next()).render(g2, contrastBackground());
						}
					}
					else {
						AngularBond aBond;
						for (Iterator i = bends.iterator(); i.hasNext();) {
							aBond = ((AngularBond) i.next());
							if (aBond.isSelected())
								aBond.render(g2, contrastBackground());
						}
					}
				}
			}

			if (boundary.isPeriodic() && showMirrorImages) { // create mirror images
				nAtomPBC = boundary.createMirrorAtoms();
			}
			else {
				nAtomPBC = nAtom;
			}

			if (useJmol)
				jmolRenderer.render(g2);
			mw2dRenderer.render(g2);

			/*
			 * if the selected component is a molecule but not in the molecule's list, render it here (this is used to
			 * render the molecule being generated but not yet finalized).
			 */
			if (selectedComponent instanceof Molecule) {
				if (!molecules.contains(selectedComponent))
					((Molecule) selectedComponent).render(g2);
			}

			/* render atomic vectors */
			int noa = getNumberOfAppearingAtoms();
			if (selectedComponent instanceof Atom) {
				int iat = ((Atom) selectedComponent).getIndex();
				for (int i = 0; i < iat; i++)
					renderVectors(atom[i], g2);
				for (int i = iat + 1; i < noa; i++)
					renderVectors(atom[i], g2);
				renderVectors(atom[iat], g2);
			}
			else {
				for (int i = 0; i < noa; i++)
					renderVectors(atom[i], g2);
			}

		}

		/* render photons */
		List<Photon> photons = model.getPhotons();
		if (photons != null && !photons.isEmpty()) {
			synchronized (photons) {
				for (Photon p : photons)
					p.render(g2);
			}
		}

		/* render free electrons */
		List<Electron> electrons = model.getFreeElectrons();
		if (electrons != null && !electrons.isEmpty()) {
			synchronized (electrons) {
				for (Electron e : electrons)
					e.render(g2);
			}
		}

		// draw the polymer or molecular surface currently being shaped
		if (smartShape != null) {
			if (actionID == SCUR_ID) {
				g2.setColor(MolecularObject.getDefaultBackground());
				g2.fill(smartShape);
			}
			g2.setColor(Color.black);
			g2.setStroke(ViewAttribute.THIN);
			g2.draw(smartShape);
		}

		molecules.render(g2);

		if (shading) {
			// Mac OS X has bugs in GradientPaint (09/10/2007)
			if (System.getProperty("os.name").startsWith("Mac")) {
				g2.drawImage(biRect, 10, 10, null);
			}
			else {
				g2.setPaint(colorScale);
				g2.fillRect(10, 10, 10, 50);
			}
			g2.setStroke(ViewAttribute.THIN);
			g2.setColor(contrastBackground());
			g2.drawRect(10, 10, 10, 50);
			g2.setFont(ViewAttribute.LITTLE_FONT);
			String s = getInternationalText("High");
			g2.drawString(s != null ? s : "High", 25, 20);
			s = getInternationalText("Low");
			g2.drawString(s != null ? s : "Low", 25, 60);
		}

		paintSteering(g2);

		if (actionID == DELE_ID || actionID == ADOB_ID || actionID == FILA_ID || actionID == FILB_ID
				|| actionID == FILC_ID || actionID == FILD_ID || actionID == MARK_ID || actionID == COUN_ID) {
			g2.setStroke(ViewAttribute.THIN);
			g2.setColor(contrastBackground());
			g2.draw(selectedArea);
		}

		if (getEnergizer())
			energizer.paint(g2);

		if (model.heatBathActivated()) {
			if (drawString) {
				g2.setFont(ViewAttribute.SMALL_FONT);
				g2.setColor(contrastBackground());
				// String s = MDView.getInternationalText("HeatBath");
				// g2.drawString(s != null ? s : "Heat bath", 10, 15);
				IconPool.getIcon("heat bath").paintIcon(this, g2, 8, 8);
			}
		}

		if (actionID == CPOS_ID || actionID == CNEG_ID) {
			if (selectedComponent instanceof Atom) {
				Atom at = (Atom) selectedComponent;
				String str = at.getCharge() + "";
				int sw = g2.getFontMetrics().stringWidth(str) + 8;
				int sh = g2.getFontMetrics().getHeight() + 6;
				g2.setStroke(ViewAttribute.THIN);
				g2.setColor(SystemColor.info);
				g2.fillRect((int) at.getRx(), (int) at.getRy() - sh, sw, sh);
				g2.setColor(contrastBackground());
				g2.drawRect((int) at.getRx(), (int) at.getRy() - sh, sw, sh);
				g2.setColor(Color.black);
				g2.drawString(str, (int) at.getRx() + 4, (int) at.getRy() - sh / 2 + 4);
			}
		}
		else if (actionID == IRES_ID || actionID == DRES_ID) {
			if (selectedComponent instanceof Atom) {
				Atom at = (Atom) selectedComponent;
				if (at.getRestraint() != null) {
					g.setColor(contrastBackground());
					g.drawString(at.getRestraint().toString(), (int) at.getRx(), (int) at.getRy());
				}
			}
		}
		else if (actionID == IDMP_ID || actionID == DDMP_ID) {
			if (selectedComponent instanceof Atom) {
				Atom at = (Atom) selectedComponent;
				g.setColor(contrastBackground());
				g.drawString(at.getFriction() + "", (int) at.getRx(), (int) at.getRy());
			}
		}
		else if (actionID == ADDA_ID || actionID == ADDB_ID || actionID == ADDC_ID || actionID == ADDD_ID) {
			if (addAtomIndicator.x >= 0 && addAtomIndicator.y >= 0) {
				g2.setStroke(ViewAttribute.THIN_DASHED);
				g2.setColor(contrastBackground(128));
				g2.draw(addAtomIndicator);
			}
		}
		else if (actionID == ADDI_ID || actionID == WATE_ID || actionID == BENZ_ID || actionID == ADCH_ID) {
			if (addObjectIndicator != null && addObjectIndicator.isPainted()) {
				g2.setStroke(ViewAttribute.THIN_DASHED);
				g2.setColor(contrastBackground(128));
				addObjectIndicator.paint(g2);
			}
		}
		else if (actionID == HEAT_ID) {
			g2.setStroke(ViewAttribute.THIN);
			pointHeater.paint(g2, true);
		}
		else if (actionID == COOL_ID) {
			g2.setStroke(ViewAttribute.THIN);
			pointHeater.paint(g2, false);
		}

		g2.setStroke(ViewAttribute.THIN);
		if (getClockPainted())
			paintInfo(g);
		paintLayeredComponents(g, Layered.IN_FRONT_OF_PARTICLES);
		paintShapeDrawing(g);

	}

	void processMousePressed(MouseEvent e) {

		super.processMousePressed(e);

		if (energizerButtonPressed)
			return;
		if (model.getJob() != null && !model.getJob().isStopped())
			return;

		int clickCount = e.getClickCount();
		if (clickCount >= 2) {
			if (actionID != HEAT_ID && actionID != COOL_ID && actionID != IDMP_ID && actionID != DDMP_ID
					&& actionID != IRES_ID && actionID != DRES_ID && actionID != CPOS_ID && actionID != CNEG_ID
					&& actionID != AACD_ID && actionID != SACD_ID && actionID != ANTD_ID && actionID != SNTD_ID
					&& actionID != SCUR_ID && actionID != RCUR_ID && actionID != SCIR_ID && actionID != RCIR_ID
					&& actionID != SREC_ID && actionID != RREC_ID && actionID != SFRE_ID) {
				resetUndoManager();
				setAction(SELE_ID);
			}
		}

		final int x = e.getX();
		final int y = e.getY();

		if (callOutMousePressed(x, y))
			return;
		if (handleMousePressed(x, y))
			return;

		/* right-click is for invoking popup menus ONLY. */
		if (ModelerUtilities.isRightClick(e)) {
			if (!popupMenuEnabled)
				return;
			if (e.isShiftDown()) {
				Atom at = whichAtom(x, y);
				if (at != null) {
					if (selectedComponent != null && selectedComponent != at)
						selectedComponent.setSelected(false);
					at.setSelected(true);
					repaint();
					if (atomPopupMenu == null)
						atomPopupMenu = new AtomPopupMenu(this);
					atomPopupMenu.setTrajSelected(at.getShowRTraj());
					atomPopupMenu.setRMeanSelected(at.getShowRMean());
					atomPopupMenu.setFMeanSelected(at.getShowFMean());
					atomPopupMenu.setCoor(x, y);
					atomPopupMenu.show(this, x, y);
					return;
				}
				RectangularObstacle obs = whichObstacle(x, y);
				if (obs != null) {
					obs.setSelected(true);
					if (obstaclePopupMenu == null)
						obstaclePopupMenu = new ObstaclePopupMenu(this);
					obstaclePopupMenu.setCoor(x, y);
					obstaclePopupMenu.show(this, x, y);
					return;
				}
				selectedComponent = null;
				defaultPopupMenu.setCoor(x, y);
				defaultPopupMenu.show(this, x, y);
				return;
			}
			if (selectedComponent != null)
				selectedComponent.setSelected(false);
			if (openLayeredComponentPopupMenus(x, y, Layered.IN_FRONT_OF_PARTICLES))
				return;
			final Atom at = whichAtom(x, y);
			if (at != null) {
				Molecule mol = molecules.getMolecule(at);
				if (mol == null) {
					at.setSelected(true);
					if (atomPopupMenu == null)
						atomPopupMenu = new AtomPopupMenu(this);
					atomPopupMenu.setTrajSelected(at.getShowRTraj());
					atomPopupMenu.setRMeanSelected(at.getShowRMean());
					atomPopupMenu.setFMeanSelected(at.getShowFMean());
					atomPopupMenu.setCoor(x, y);
					atomPopupMenu.show(this, x, y);
					return;
				}
				if (!(mol instanceof MolecularObject)) {
					mol.setSelected(true);
					if (moleculePopupMenu == null)
						moleculePopupMenu = new MoleculePopupMenu(this);
					moleculePopupMenu.setCoor(x, y);
					moleculePopupMenu.show(this, x, y);
					return;
				}
			}
			MolecularObject ss = whichMolecularObject(x, y);
			if (ss != null) {
				ss.setSelected(true);
				if (molecularObjectPopupMenu == null)
					molecularObjectPopupMenu = new MolecularObjectPopupMenu(this);
				molecularObjectPopupMenu.setCoor(x, y);
				molecularObjectPopupMenu.pack();
				molecularObjectPopupMenu.show(this, x, y);
				return;
			}
			RectangularObstacle obs = whichObstacle(x, y);
			if (obs != null) {
				obs.setSelected(true);
				if (obstaclePopupMenu == null)
					obstaclePopupMenu = new ObstaclePopupMenu(this);
				obstaclePopupMenu.setCoor(x, y);
				obstaclePopupMenu.show(this, x, y);
				return;
			}
			RadialBond rBond = whichBond(x, y);
			if (rBond != null) {
				rBond.setSelected(true);
				if (radialBondPopupMenu == null)
					radialBondPopupMenu = new RadialBondPopupMenu(this);
				radialBondPopupMenu.setCoor(x, y);
				radialBondPopupMenu.show(this, x, y);
				return;
			}
			AngularBond aBond = whichAngle(x, y);
			if (aBond != null) {
				aBond.setSelected(true);
				if (angularBondPopupMenu == null)
					angularBondPopupMenu = new AngularBondPopupMenu(this);
				angularBondPopupMenu.setCoor(x, y);
				angularBondPopupMenu.show(this, x, y);
				return;
			}
			if (openLayeredComponentPopupMenus(x, y, Layered.BEHIND_PARTICLES))
				return;
			selectedComponent = null;
			defaultPopupMenu.setCoor(x, y);
			defaultPopupMenu.show(this, x, y);
			return;
		}

		switch (actionID) {

		case SELE_ID:

			/* FIXME: the special popup menus that do not use right-click. This will be removed in 2010. */
			if (e.isAltDown()) {
				final Atom at = whichAtom(x, y);
				if (at != null) {
					if (at.isAminoAcid()) {
						at.setSelected(true);
						if (acidPopupMenu == null)
							acidPopupMenu = new AminoAcidPopupMenu();
						acidPopupMenu.setAtom(at);
						acidPopupMenu.show(this, x, y);
					}
					else if (at.isNucleotide() && at.getID() != Element.ID_SP) {
						at.setSelected(true);
						if (nucleotidePopupMenu == null)
							nucleotidePopupMenu = new NucleotidePopupMenu();
						nucleotidePopupMenu.setAtom(at);
						nucleotidePopupMenu.show(this, x, y);
					}
				}
				return;
			}

			if (selectedComponent instanceof RectangularObstacle) {
				obsRectSelected = -1;
				switch (((RectangularObstacle) selectedComponent).getPositionCode(x, y)) {
				case RectangularObstacle.NW:
					obsRectSelected = RectangularObstacle.NW;
					break;
				case RectangularObstacle.NORTH:
					obsRectSelected = RectangularObstacle.NORTH;
					break;
				case RectangularObstacle.NE:
					obsRectSelected = RectangularObstacle.NE;
					break;
				case RectangularObstacle.EAST:
					obsRectSelected = RectangularObstacle.EAST;
					break;
				case RectangularObstacle.SE:
					obsRectSelected = RectangularObstacle.SE;
					break;
				case RectangularObstacle.SOUTH:
					obsRectSelected = RectangularObstacle.SOUTH;
					break;
				case RectangularObstacle.SW:
					obsRectSelected = RectangularObstacle.SW;
					break;
				case RectangularObstacle.WEST:
					obsRectSelected = RectangularObstacle.WEST;
					break;
				case RectangularObstacle.INSIDE:
					obsRectSelected = RectangularObstacle.INSIDE;
					break;
				}
				if (obsRectSelected > RectangularObstacle.INSIDE) {
					((RectangularObstacle) selectedComponent).storeCurrentState();
				}
				else if (obsRectSelected == -1) {
					selectedComponent.setSelected(false);
					selectedComponent = null;
				}
			}
			else if (selectedComponent != null) {
				selectedComponent.setSelected(false);
				selectedComponent = null;
			}
			ModelComponent mc = getSelectedComponent(e.getModifiersEx(), x, y);
			if (mc != null) {
				mc.storeCurrentState();
				if (clickCount >= 2)
					DialogFactory.showDialog(mc);
			}
			repaint();
			break;

		case RCUR_ID:
		case SCUR_ID:
		case SFRE_ID:
			if (clickCount == 1) {
				if (smartPoly == null) {
					smartPoly = new Polygon();
					smartPoly.addPoint(x, y);
				}
				else {
					addPointToPolygon(smartPoly, x, y);
				}
			}
			break;

		case SCIR_ID:
		case RCIR_ID:
			if (clickCount == 1) {
				if (!(smartShape instanceof Ellipse)) {
					smartShape = new Ellipse(x, y, 10, 10);
				}
			}
			break;

		case SREC_ID:
		case RREC_ID:
			if (clickCount == 1) {
				if (!(smartShape instanceof RoundRectangle)) {
					smartShape = new RoundRectangle(x, y, 10, 10, 20, 20);
				}
			}
			break;

		case COUN_ID:
			showActionTip("Drag the mouse to specify an area within which objects will be counted", x + 10, y + 10);
			if (clickCount == 1)
				selectedArea.setLocation(x, y);
			break;

		case MARK_ID:
			showActionTip("Drag the mouse to specify an area within which objects will be marked", x + 10, y + 10);
			if (clickCount == 1)
				selectedArea.setLocation(x, y);
			break;

		case RECT_ID:
			showActionTip("Drag the mouse to draw a rectangle", x + 10, y + 10);
			if (clickCount == 1) {
				selectedArea.setLocation(x, y);
				anchorPoint.setLocation(x, y);
			}
			break;

		case TRIA_ID:
			showActionTip("Drag the mouse to draw a triangle", x + 10, y + 10);
			if (clickCount == 1) {
				selectedArea.setLocation(x, y);
				anchorPoint.setLocation(x, y);
			}
			break;

		case ELLI_ID:
			showActionTip("Drag the mouse to draw an ellipse", x + 10, y + 10);
			if (clickCount == 1) {
				selectedArea.setLocation(x, y);
				anchorPoint.setLocation(x, y);
			}
			break;

		case FILA_ID:
		case FILB_ID:
		case FILC_ID:
		case FILD_ID:
			showActionTip("Drag the mouse to specify an area which will be filled with atoms", x + 10, y + 10);
			if (clickCount == 1)
				selectedArea.setLocation(x, y);
			break;

		case DELE_ID:
			showActionTip("Drag the mouse to specify an area within which objects will be removed", x + 10, y + 10);
			if (clickCount == 1)
				selectedArea.setLocation(x, y);
			break;

		case ADOB_ID:
			if (clickCount == 1)
				selectedArea.setLocation(x, y);
			showActionTip("Drag your mouse to specify size", x + 10, y + 10);
			break;

		case WHAT_ID:
			mc = getSelectedComponent(e.getModifiersEx(), x, y);
			String str = null;
			if (mc instanceof Obstacle) {
				str = mc.toString();
			}
			else if (mc instanceof Molecule) {
				if (mc instanceof MolecularObject) {
					if (mc instanceof CurvedSurface)
						str = "Curved Surface #" + molecules.indexOf(mc) + ":" + mc;
					else if (mc instanceof CurvedRibbon)
						str = "Curved Ribbon #" + molecules.indexOf(mc) + ":" + mc;
				}
				else {
					str = "Molecule #" + molecules.indexOf(mc) + ": " + mc;
				}
			}
			else if (mc instanceof Atom) {
				str = "Atom #" + mc;
			}
			else if (mc instanceof RadialBond) {
				str = "#" + bonds.indexOf(mc) + " " + mc.toString();
			}
			else if (mc instanceof AngularBond) {
				str = "#" + bends.indexOf(mc) + " " + mc.toString();
			}
			else if (mc instanceof ImageComponent) {
				str = "Image #" + getLayeredComponentIndex((ImageComponent) mc) + ": "
						+ FileUtilities.getFileName(mc.toString());
			}
			else if (mc instanceof TextBoxComponent) {
				str = "Text box #" + getLayeredComponentIndex((TextBoxComponent) mc);
			}
			else if (mc instanceof LineComponent) {
				str = "Line #" + getLayeredComponentIndex((LineComponent) mc);
			}
			else if (mc instanceof RectangleComponent) {
				str = "Rectangle #" + getLayeredComponentIndex((RectangleComponent) mc);
			}
			else if (mc instanceof TriangleComponent) {
				str = "Triangle #" + getLayeredComponentIndex((TriangleComponent) mc);
			}
			else if (mc instanceof EllipseComponent) {
				str = "Ellipse #" + getLayeredComponentIndex((EllipseComponent) mc);
			}
			if (str != null) {
				str += " @ (" + ViewAttribute.ANGSTROM_FORMAT.format(x * 0.1) + ", "
						+ ViewAttribute.ANGSTROM_FORMAT.format(y * 0.1) + ")";
			}
			else {
				str = "(" + ViewAttribute.ANGSTROM_FORMAT.format(x * 0.1) + ", "
						+ ViewAttribute.ANGSTROM_FORMAT.format(y * 0.1) + ")";
			}
			showActionTip(str, x + 10, y + 10);
			repaint();
			break;

		case ROTA_ID:
			if (selectedComponent instanceof Rotatable) {
				Rotatable r = (Rotatable) selectedComponent;
				rotationHandle = r.getRotationHandle(x, y);
				if (rotationHandle != -1) {
					r.setSelectedToRotate(true);
					setCursor(rotateCursor3);
					selectedComponent.storeCurrentState();
				}
				else {
					r.setSelectedToRotate(false);
					setCursor(rotateCursor2);
					selectRotatable(x, y);
				}
			}
			else {
				selectRotatable(x, y);
			}
			repaint();
			break;

		case MEAS_ID:
			if (clickCount == 1) {
				if (!readyToAdjustDistanceVector) {
					Atom at = whichAtom(x, y);
					if (at != null) {
						at.setSelected(true);
						if (e.isShiftDown()) {
							at.addMeasurement(new Point(2 * at.getRx() < getWidth() ? (int) (at.getRx() + 20)
									: (int) (at.getRx() - 20), (int) at.getRy()));
						}
						else {
							showActionTip(
									at.getNumberOfMeasurements() <= 0 ? "Hold down SHIFT and click to add a measurement."
											: "<html>Drag the green hotspot at the tip to measure, or<br>hold down SHIFT and click to add one more measurement.</html>",
									x + 10, y + 10);
						}
					}
				}
				else {
					showActionTip(
							"<html>To remove this measurement, drag the<br>green hotspot out of this window.</html>",
							x + 10, y + 10);
				}
			}
			break;

		case VELO_ID:
			if (clickCount == 1) {
				if (!readyToAdjustVelocityVector) {
					Atom at = whichAtom(x, y);
					if (at != null) {
						showActionTip("Drag the red hotspot at the tip of the velocity vector", x + 10, y + 10);
						selectVelocity(at);
						at.setSelected(true);
						repaint();
					}
				}
			}
			break;

		case MUTA_ID:
			Atom at = whichAtom(x, y);
			if (at != null) {
				at.setSelected(true);
				mutateSelectedAtom();
			}
			break;

		case TRAJ_ID:
			at = whichAtom(x, y);
			if (at != null) {
				at.setSelected(true);
				at.setShowRTraj(!at.getShowRTraj());
				if (at.getShowRTraj()) {
					showActionTip("Click the atom to hide its trajectory", x + 20, y + 20);
				}
				else {
					showActionTip("Click the atom to show its trajectory", x + 20, y + 20);
				}
				repaint();
			}
			break;

		case RAVE_ID:
			at = whichAtom(x, y);
			if (at != null) {
				at.setSelected(true);
				at.setShowRMean(!at.getShowRMean());
				if (at.getShowRMean()) {
					showActionTip("Click the atom to hide its current average position", x + 20, y + 20);
				}
				else {
					showActionTip("Click the atom to show its current average position", x + 20, y + 20);
				}
				repaint();
			}
			break;

		case BBON_ID:
			if (e.isAltDown()) {
				if (bondBeingMade)
					createABond(x, y);
			}
			else {
				at = whichAtom(x, y);
				if (at != null) {
					at.setSelected(true);
					bondBeingMade = true;
					showActionTip("Hold down the ALT key and click another atom", x + 10, y + 10);
					repaint();
				}
				else {
					bondBeingMade = false;
				}
			}
			break;

		case BBEN_ID:
			if (e.isAltDown()) {
				if (bendBeingMade)
					createABend(x, y);
			}
			else {
				if (bonds.size() >= 2) {
					RadialBond rBond = whichBond(x, y);
					if (rBond != null) {
						rBond.setSelected(true);
						bendBeingMade = true;
						showActionTip("Hold down the ALT key and click another radial bond", x + 10, y + 10);
						repaint();
					}
					else {
						bendBeingMade = false;
					}
				}
			}
			break;

		case DUPL_ID:
			mc = getSelectedComponent(e.getModifiersEx(), x, y);
			if (mc instanceof Atom) {
				if (nAtom < atom.length) {
					showActionTip("Drag the selected atom to duplicate one", x + 10, y + 10);
					atom[nAtom].duplicate((Atom) mc);
					atom[nAtom].setSelected(true);
					nAtom++;
				}
			}
			else if (mc instanceof Molecule) {
				Molecule mol = ((Molecule) mc).duplicate();
				showActionTip("Drag the selected molecule to duplicate one", x + 10, y + 10);
				nAtom += mol.size();
				if (!(mc instanceof MolecularObject)) {
					mol.setSelected(true);
				}
				else {
					MolecularObject ss = mc instanceof CurvedSurface ? new CurvedSurface(mol) : new CurvedRibbon(mol);
					ss.setModel(model);
					ss.setSelected(true);
					ss.setBackground(((MolecularObject) mc).getBackground());
				}
			}
			else if (mc instanceof RectangularObstacle) {
				showActionTip("Drag the selected obstacle to duplicate one", x + 10, y + 10);
				RectangularObstacle r0 = (RectangularObstacle) mc;
				RectangularObstacle r1 = (RectangularObstacle) r0.clone();
				r1.x = x - 10 - clickPoint.x;
				r1.y = y - 10 - clickPoint.y;
				obstacles.add(r1);
				r1.setSelected(true);
			}
			else if (mc instanceof ImageComponent) {
				showActionTip("Drag the selected image to duplicate one", x + 10, y + 10);
				ImageComponent ic = null;
				try {
					ic = new ImageComponent((ImageComponent) mc);
				}
				catch (IOException ioe) {
					ioe.printStackTrace();
				}
				if (ic != null) {
					ic.setSelected(true);
					ic.setLocation(x - 10 - clickPoint.x, y - 10 - clickPoint.y);
					addLayeredComponent(ic);
				}
			}
			else if (mc instanceof TextBoxComponent) {
				showActionTip("Drag the selected text box to duplicate one", x + 10, y + 10);
				TextBoxComponent tb = new TextBoxComponent((TextBoxComponent) mc);
				tb.setSelected(true);
				tb.setLocation(x - 10 - clickPoint.x, y - 10 - clickPoint.y);
				addLayeredComponent(tb);
			}
			else if (mc instanceof LineComponent) {
				showActionTip("Drag the selected line to duplicate one", x + 10, y + 10);
				LineComponent lc = new LineComponent((LineComponent) mc);
				lc.setSelected(true);
				lc.setLocation(x - 10 - clickPoint.x, y - 10 - clickPoint.y);
				addLayeredComponent(lc);
			}
			else if (mc instanceof RectangleComponent) {
				showActionTip("Drag the selected rectangle to duplicate one", x + 10, y + 10);
				RectangleComponent rc = new RectangleComponent((RectangleComponent) mc);
				rc.setSelected(true);
				rc.setLocation(x - 10 - clickPoint.x, y - 10 - clickPoint.y);
				addLayeredComponent(rc);
			}
			else if (mc instanceof TriangleComponent) {
				showActionTip("Drag the selected triangle to duplicate one", x + 10, y + 10);
				TriangleComponent tc = new TriangleComponent((TriangleComponent) mc);
				tc.setSelected(true);
				tc.setLocation(x - 10 - clickPoint.x, y - 10 - clickPoint.y);
				addLayeredComponent(tc);
			}
			else if (mc instanceof EllipseComponent) {
				showActionTip("Drag the selected ellipse to duplicate one", x + 10, y + 10);
				EllipseComponent ec = new EllipseComponent((EllipseComponent) mc);
				ec.setSelected(true);
				ec.setLocation(x - 10 - clickPoint.x, y - 10 - clickPoint.y);
				addLayeredComponent(ec);
			}
			repaint();
			break;

		case ADDA_ID:
			addAtomIndicator.x = addAtomIndicator.y = -1;
			if (clickCount == 1)
				insertAnAtom(x, y, Element.ID_NT);
			break;

		case ADDB_ID:
			addAtomIndicator.x = addAtomIndicator.y = -1;
			if (clickCount == 1)
				insertAnAtom(x, y, Element.ID_PL);
			break;

		case ADDC_ID:
			addAtomIndicator.x = addAtomIndicator.y = -1;
			if (clickCount == 1)
				insertAnAtom(x, y, Element.ID_WS);
			break;

		case ADDD_ID:
			addAtomIndicator.x = addAtomIndicator.y = -1;
			if (clickCount == 1)
				insertAnAtom(x, y, Element.ID_CK);
			break;

		case BENZ_ID:
			if (addObjectIndicator instanceof AddObjectIndicator.AddBenzeneMoleculeIndicator)
				addObjectIndicator.setPainted(false);
			if (clickCount == 1)
				insertAMolecule(x, y, new Benzene());
			break;

		case WATE_ID:
			if (addObjectIndicator instanceof AddObjectIndicator.AddTriatomicMoleculeIndicator)
				addObjectIndicator.setPainted(false);
			if (clickCount == 1)
				insertAMolecule(x, y, new TriatomicMolecule(TriatomicConfigure.typeOfA, TriatomicConfigure.typeOfB,
						TriatomicConfigure.typeOfC, TriatomicConfigure.d12, TriatomicConfigure.s12,
						TriatomicConfigure.d23, TriatomicConfigure.s23, TriatomicConfigure.angle,
						TriatomicConfigure.strength));
			break;

		case ADDI_ID:
			if (addObjectIndicator instanceof AddObjectIndicator.AddDiatomicMoleculeIndicator)
				addObjectIndicator.setPainted(false);
			if (clickCount == 1)
				insertAMolecule(x, y, new DiatomicMolecule(DiatomicConfigure.typeOfA, DiatomicConfigure.typeOfB,
						DiatomicConfigure.distance, DiatomicConfigure.strength));
			break;

		case ADCH_ID:
			if (addObjectIndicator instanceof AddObjectIndicator.AddChainMoleculeIndicator)
				addObjectIndicator.setPainted(false);
			if (clickCount == 1 && nAtom < atom.length - ChainConfigure.MAXIMUM)
				insertAMolecule(x, y, new ChainMolecule(ChainConfigure.typeOfAtom, ChainConfigure.number,
						ChainConfigure.growMode, ChainConfigure.distance, ChainConfigure.angle));
			break;

		case IRES_ID:
			at = whichAtom(x, y);
			if (at != null) {
				at.setSelected(true);
				if (at.getRestraint() == null) {
					at.setRestraint(new PointRestraint(10, at.getRx(), at.getRy()));
				}
				else {
					at.getRestraint().changeK(10);
				}
				repaint();
				model.notifyChange();
			}
			break;

		case DRES_ID:
			at = whichAtom(x, y);
			if (at != null && at.getRestraint() != null) {
				at.setSelected(true);
				if (at.getRestraint().getK() >= 10) {
					at.getRestraint().changeK(-10);
					if (at.getRestraint().getK() <= ZERO)
						at.setRestraint(null);
					repaint();
					model.notifyChange();
				}
			}
			break;

		case IDMP_ID:
			at = whichAtom(x, y);
			if (at != null) {
				at.setSelected(true);
				if (at.addFriction(0.5f)) {
					repaint();
					model.notifyChange();
				}
			}
			break;

		case DDMP_ID:
			at = whichAtom(x, y);
			if (at != null) {
				at.setSelected(true);
				if (at.addFriction(-0.5f)) {
					repaint();
					model.notifyChange();
				}
			}
			break;

		case CPOS_ID:
			at = whichAtom(x, y);
			if (at != null) {
				at.setSelected(true);
				if (at.addCharge(e.isShiftDown() ? -chargeIncrement : chargeIncrement)) {
					repaint();
					model.notifyChange();
				}
			}
			break;

		case CNEG_ID:
			at = whichAtom(x, y);
			if (at != null) {
				at.setSelected(true);
				if (at.addCharge(e.isShiftDown() ? chargeIncrement : -chargeIncrement)) {
					repaint();
					model.notifyChange();
				}
			}
			break;

		case HEAT_ID:
		case COOL_ID:
			pointHeater.setLocation(x, y);
			pointHeater.equiPartitionEnergy(model);
			repaint();
			break;

		case SBOU_ID:
			if (boundary.getType() != RectangularBoundary.DBC_ID) {
				if (!model.isEmpty() && boundary.getHandle() > 0)
					model.getBoundsOfObjects();
			}
			break;

		case AACD_ID:
			mc = getSelectedComponent(e.getModifiersEx(), x, y);
			if (mc instanceof Polypeptide) {
				Atom[] a = ((Polypeptide) mc).getTermini();
				double d1 = (a[0].getRx() - x) * (a[0].getRx() - x) + (a[0].getRy() - y) * (a[0].getRy() - y);
				double d2 = (a[1].getRx() - x) * (a[1].getRx() - x) + (a[1].getRy() - y) * (a[1].getRy() - y);
				((Polypeptide) mc).attachRandomAminoAcidToTerminus(d1 < d2 ? a[0] : a[1]);
				Atom a2 = atom[model.getNumberOfParticles() - 1];
				if (!doNotFireUndoEvent) {
					molecules.getMolecule(model.getAtom(model.getNumberOfAtoms() - 1)).setSelected(true);
					model.getUndoManager().undoableEditHappened(
							new UndoableEditEvent(model, new UndoableInsertion(AACD_ID, a2.getRx(), a2.getRy())));
					updateUndoUIComponents();
				}
			}
			else {
				if (mc == null) {
					insertAnAtom(x, y, Math.round(Element.ID_ALA + (float) Math.random() * 19));
				}
				else if (mc instanceof Atom) {
					((Atom) mc).attachRandomAminoAcid();
					molecules.getMolecule(model.getAtom(model.getNumberOfAtoms() - 1)).setSelected(true);
				}
			}
			break;

		case SACD_ID:
			mc = getSelectedComponent(e.getModifiersEx(), x, y);
			if (mc instanceof Polypeptide) {
				Atom[] a = ((Polypeptide) mc).getTermini();
				double d1 = (a[0].getRx() - x) * (a[0].getRx() - x) + (a[0].getRy() - y) * (a[0].getRy() - y);
				double d2 = (a[1].getRx() - x) * (a[1].getRx() - x) + (a[1].getRy() - y) * (a[1].getRy() - y);
				setSelectedComponent(d1 < d2 ? a[0] : a[1]);
				removeSelectedComponent();
				repaint();
			}
			break;

		case ANTD_ID:
			mc = getSelectedComponent(e.getModifiersEx(), x, y);
			if (mc instanceof DNAStrand) {
				DNAStrand mol = (DNAStrand) mc;
				Atom a1 = null, a2 = null;
				for (Iterator it = mol.iterator(); it.hasNext();) {
					at = (Atom) it.next();
					if (at.getID() == Element.ID_SP) {
						if (bonds.getBondedPartnerCount(at) == 2) {
							if (a1 == null) {
								a1 = at;
							}
							else {
								a2 = at;
								break;
							}
						}
					}
				}
				if (a1 == null && a2 == null) {
					mol.attachRandomNucleotide(null);
				}
				else if (a1 != null && a2 != null) {
					boolean b = (a1.getRx() - x) * (a1.getRx() - x) + (a1.getRy() - y) * (a1.getRy() - y) > (a2.getRx() - x)
							* (a2.getRx() - x) + (a2.getRy() - y) * (a2.getRy() - y);
					mol.attachRandomNucleotide(b ? a2 : a1);
				}
				// WARNING!!! After calling attachRandomNucleotide, molecules will be re-generated.
				// As a result, the mol reference is not valid any more.
				if (!doNotFireUndoEvent) {
					molecules.getMolecule(model.getAtom(model.getNumberOfAtoms() - 1)).setSelected(true);
					model.getUndoManager().undoableEditHappened(
							new UndoableEditEvent(model, new UndoableInsertion(ANTD_ID)));
					updateUndoUIComponents();
				}
			}
			else {
				if (mc == null) {
					int id1 = Element.ID_SP;
					int id2 = Math.round(Element.ID_A + (float) Math.random() * 3);
					int d = (int) ((model.getElement(id1).getSigma() + model.getElement(id2).getSigma()) * 0.6);
					boolean b = insertAnAtom(x, y, id1);
					if (b) {
						b = insertAnAtom(x, y + d, id2);
						int noa = model.getNumberOfAtoms();
						if (b) {
							(model).getBonds().add(
									new RadialBond.Builder(model.getAtom(noa - 2), model.getAtom(noa - 1))
											.bondLength(d).build());
							MoleculeCollection.sort(model);
							if (!doNotFireUndoEvent) {
								molecules.getMolecule(model.getAtom(noa - 1)).setSelected(true);
								model.getUndoManager().undoableEditHappened(
										new UndoableEditEvent(model, new UndoableInsertion(ANTD_ID)));
								updateUndoUIComponents();
							}
						}
						else {
							model.getAtom(noa - 1).setSelected(true);
							removeSelectedComponent();
						}
					}
				}
			}
			break;

		case SNTD_ID:
			mc = getSelectedComponent(e.getModifiersEx(), x, y);
			if (mc instanceof DNAStrand) {
				DNAStrand mol = (DNAStrand) mc;
				Atom a1 = null, a2 = null;
				for (Iterator it = mol.iterator(); it.hasNext();) {
					at = (Atom) it.next();
					if (at.getID() == Element.ID_SP) {
						if (bonds.getBondedPartnerCount(at) == 2) {
							if (a1 == null) {
								a1 = at;
							}
							else {
								a2 = at;
								break;
							}
						}
					}
				}
				if (a1 != null && a2 != null) {
					int idna = molecules.indexOf(mol);
					List<Integer> list = new ArrayList<Integer>();
					if ((a1.getRx() - x) * (a1.getRx() - x) + (a1.getRy() - y) * (a1.getRy() - y) > (a2.getRx() - x)
							* (a2.getRx() - x) + (a2.getRy() - y) * (a2.getRy() - y)) {
						list.add(a2.getIndex());
						Atom[] ats = model.getBonds().getBondedPartners(a2, false);
						if (ats != null) {
							for (Atom i : ats) {
								if (i.getID() != Element.ID_SP)
									list.add(i.getIndex());
							}
						}
					}
					else {
						list.add(a1.getIndex());
						Atom[] ats = model.getBonds().getBondedPartners(a1, false);
						if (ats != null) {
							for (Atom i : ats) {
								if (i.getID() != Element.ID_SP)
									list.add(i.getIndex());
							}
						}
					}
					removeMarkedAtoms(list);
					setSelectedComponent(null);
					repaint();
					model.notifyChange();
					if (!doNotFireUndoEvent) {
						molecules.get(idna).setSelected(true);
						model.getUndoManager().undoableEditHappened(
								new UndoableEditEvent(model, new UndoableDeletion(SNTD_ID)));
						updateUndoUIComponents();
					}
				}
			}
			break;

		}

		e.consume();

	}

	void processMouseMoved(MouseEvent e) {

		super.processMouseMoved(e);

		if (model.getJob() != null && !model.getJob().isStopped())
			return;

		int x = e.getX();
		int y = e.getY();

		if (callOutMouseMoved(x, y))
			return;
		if (handleMouseMoved(x, y))
			return;

		switch (actionID) {

		case SELE_ID:
			if (selectedComponent instanceof RectangularObstacle) {
				switch (((RectangularObstacle) selectedComponent).getPositionCode(x, y)) {
				case RectangularObstacle.NW:
					setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
					return;
				case RectangularObstacle.NORTH:
					setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
					return;
				case RectangularObstacle.NE:
					setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
					return;
				case RectangularObstacle.EAST:
					setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
					return;
				case RectangularObstacle.SE:
					setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
					return;
				case RectangularObstacle.SOUTH:
					setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
					return;
				case RectangularObstacle.SW:
					setCursor(Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));
					return;
				case RectangularObstacle.WEST:
					setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
					return;
				}
			}
			ModelComponent mc = getRolloverComponent(x, y);
			setCursor(mc != null ? UserAction.getCursor(actionID) : Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			break;

		case ADDA_ID:
		case ADDB_ID:
		case ADDC_ID:
		case ADDD_ID:
			addAtomIndicator.x = x - addAtomIndicator.width * 0.5f;
			addAtomIndicator.y = y - addAtomIndicator.height * 0.5f;
			repaint();
			break;

		case ADDI_ID:
		case WATE_ID:
		case BENZ_ID:
		case ADCH_ID:
			if (addObjectIndicator != null) {
				addObjectIndicator.setPainted(true);
				addObjectIndicator.setLocation(x, y);
				repaint();
			}
			break;

		case RCUR_ID:
		case SCUR_ID:
		case SFRE_ID:
			if (smartPoly != null) {
				updateSmartShape(x, y);
				repaint();
			}
			break;

		case RCIR_ID:
		case SCIR_ID:
		case RREC_ID:
		case SREC_ID:
			updateSmartShape(x, y);
			repaint();
			break;

		case HEAT_ID:
		case COOL_ID:
			pointHeater.setLocation(x, y);
			repaint();
			break;

		case ROTA_ID:
			if (selectedComponent instanceof Rotatable) {
				setCursor(((Rotatable) selectedComponent).getRotationHandle(x, y) != -1 ? rotateCursor1
						: previousCursor);
			}
			break;

		case VELO_ID:
			if (selectedComponent instanceof Atom) {
				Atom a = (Atom) selectedComponent;
				if (a.velocitySelected()) {
					if (a.isVelocityHandleSelected(x, y)) {
						setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
						readyToAdjustVelocityVector = true;
					}
					else {
						setCursor(previousCursor);
						readyToAdjustVelocityVector = false;
					}
				}
			}
			break;

		case MEAS_ID:
			if (selectedComponent instanceof Atom) {
				Atom a = (Atom) selectedComponent;
				indexOfSelectedMeasurement = a.getMeasurement(x, y);
				if (indexOfSelectedMeasurement >= 0) {
					setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
					readyToAdjustDistanceVector = true;
				}
				else {
					setCursor(previousCursor);
					readyToAdjustDistanceVector = false;
				}
			}
			break;

		case SBOU_ID:
			if (boundary.getBoundRectUpperLeft().contains(x, y)) {
				boundary.setHandle(RectangularBoundary.UPPER_LEFT);
				setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
				return;
			}
			if (boundary.getBoundRectLowerLeft().contains(x, y)) {
				boundary.setHandle(RectangularBoundary.LOWER_LEFT);
				setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
				return;
			}
			if (boundary.getBoundRectUpperRight().contains(x, y)) {
				boundary.setHandle(RectangularBoundary.UPPER_RIGHT);
				setCursor(Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));
				return;
			}
			if (boundary.getBoundRectLowerRight().contains(x, y)) {
				boundary.setHandle(RectangularBoundary.LOWER_RIGHT);
				setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
				return;
			}
			boundary.setHandle(-1);
			setCursor(previousCursor);
			break;

		}

		e.consume();

	}

	void processMouseDragged(MouseEvent e) {

		if (ModelerUtilities.isRightClick(e))
			return;
		if (System.currentTimeMillis() - mouseHeldTime < MINIMUM_MOUSE_DRAG_RESPONSE_INTERVAL)
			return;
		int x = e.getX();
		int y = e.getY();
		model.runMouseScript(MouseEvent.MOUSE_DRAGGED, x, y);
		if (model.getJob() != null && !model.getJob().isStopped()) {
			steerParticleUsingMouse(x - mouseHeldX, y - mouseHeldY);
			return;
		}

		super.processMouseDragged(e);

		if (callOutMouseDragged(x, y))
			return;
		if (handleMouseDragged(x, y))
			return;

		switch (actionID) {

		case SELE_ID:

			if (selectedComponent != null) {
				if (!isEditable() && !selectedComponent.isDraggable()) {
					showActionTip("<html><font color=red>The selected object is not draggable!</font></html>", x, y);
				}
				else {
					if (selectedComponent instanceof RectangularObstacle) {
						if (obsRectSelected > RectangularObstacle.INSIDE) {
							RectangularObstacle r2d = (RectangularObstacle) selectedComponent;
							int xbox = (int) r2d.getWidth();
							int ybox = (int) r2d.getHeight();
							int xmin = (int) r2d.getX();
							int ymin = (int) r2d.getY();
							switch (obsRectSelected) {
							case RectangularObstacle.NW:
								xbox = xbox + xmin - x;
								ybox = ybox + ymin - y;
								xmin = x;
								ymin = y;
								break;
							case RectangularObstacle.NORTH:
								ybox = ybox + ymin - y;
								ymin = y;
								break;
							case RectangularObstacle.NE:
								xbox = x - xmin;
								ybox = ybox + ymin - y;
								ymin = y;
								break;
							case RectangularObstacle.EAST:
								xbox = x - xmin;
								break;
							case RectangularObstacle.SE:
								xbox = x - xmin;
								ybox = y - ymin;
								break;
							case RectangularObstacle.SOUTH:
								ybox = y - ymin;
								break;
							case RectangularObstacle.SW:
								xbox = xbox + xmin - x;
								ybox = y - ymin;
								xmin = x;
								break;
							case RectangularObstacle.WEST:
								xbox = xbox + xmin - x;
								xmin = x;
								break;
							}
							if (xbox < 0) {
								xmin = x;
								xbox = -xbox;
							}
							if (ybox < 0) {
								ymin = y;
								ybox = -ybox;
							}
							if (xbox == 0)
								xbox = 1;
							if (ybox == 0)
								ybox = 1;
							r2d.setRect(xmin, ymin, xbox, ybox);
							model.notifyChange();
							repaint();
							return;
						}
					}

					dragSelected = false;
					if (selectedComponent instanceof Molecule) {
						dragSelected = true;
						Molecule mol = (Molecule) selectedComponent;
						mol.translateTo(x - clickPoint.x, y - clickPoint.y);
						boundary.setRBC(mol);
						refreshForces();
					}
					else if (selectedComponent instanceof Atom) {
						dragSelected = true;
						Atom a = (Atom) selectedComponent;
						if (a.getRestraint() != null) {
							int amp = a.isBonded() ? 5 : (int) (400.0 / a.getRestraint().getK());
							Vector2D loc = moveSpring(x, y, (int) a.getRestraint().getX0(), (int) a.getRestraint()
									.getY0(), 0, amp);
							if (loc == null)
								return;
							a.translateTo(loc.getX(), loc.getY());
						}
						else {
							a.translateTo(x - clickPoint.x, y - clickPoint.y);
						}
						boundary.setRBC(a);
						refreshForces();
					}
					else if (selectedComponent instanceof RectangularObstacle) {
						if (obsRectSelected <= RectangularObstacle.INSIDE) {
							((RectangularObstacle) selectedComponent).translateTo(x - clickPoint.x, y - clickPoint.y);
							dragSelected = true;
						}
					}
					else if (selectedComponent instanceof ImageComponent) {
						ImageComponent ic = (ImageComponent) selectedComponent;
						ic.translateTo(x - clickPoint.x, y - clickPoint.y);
						dragSelected = true;
						moveHostTo(ic.getHost(), ic.getRx() + ic.getWidth() * 0.5, ic.getRy() + ic.getHeight() * 0.5);
					}
					else if (selectedComponent instanceof TextBoxComponent) {
						TextBoxComponent tb = (TextBoxComponent) selectedComponent;
						dragSelected = true;
						tb.translateTo(x - clickPoint.x, y - clickPoint.y);
						if (tb.getAttachmentPosition() == TextBoxComponent.BOX_CENTER)
							moveHostTo(tb.getHost(), tb.getRx() + 0.5 * tb.getWidth(), tb.getRy() + 0.5
									* tb.getHeight());
					}
					else if (selectedComponent instanceof LineComponent) {
						LineComponent lc = (LineComponent) selectedComponent;
						lc.translateTo(x - clickPoint.x, y - clickPoint.y);
						dragSelected = true;
						moveHostTo(lc.getHost(), lc.getRx(), lc.getRy());
					}
					else if (selectedComponent instanceof RectangleComponent) {
						RectangleComponent rc = (RectangleComponent) selectedComponent;
						rc.translateTo(x - clickPoint.x, y - clickPoint.y);
						dragSelected = true;
						moveHostTo(rc.getHost(), rc.getRx(), rc.getRy());
					}
					else if (selectedComponent instanceof TriangleComponent) {
						TriangleComponent tc = (TriangleComponent) selectedComponent;
						tc.translateTo(x - clickPoint.x, y - clickPoint.y);
						dragSelected = true;
						moveHostTo(tc.getHost(), tc.getRx(), tc.getRy());
					}
					else if (selectedComponent instanceof EllipseComponent) {
						EllipseComponent ec = (EllipseComponent) selectedComponent;
						ec.translateTo(x - clickPoint.x, y - clickPoint.y);
						dragSelected = true;
						moveHostTo(ec.getHost(), ec.getRx(), ec.getRy());
					}
					if (dragSelected) {
						repaint();
						setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
					}
				}
			}

			break;

		case ADDA_ID:
		case ADDB_ID:
		case ADDC_ID:
		case ADDD_ID:
			addAtomIndicator.x = addAtomIndicator.y = -1;
			repaint();
			break;

		case ADDI_ID:
		case WATE_ID:
		case BENZ_ID:
		case ADCH_ID:
			if (addObjectIndicator != null) {
				addObjectIndicator.setPainted(false);
				repaint();
			}
			break;

		case RCUR_ID:
		case SCUR_ID:
		case SFRE_ID:
			if (smartPoly != null) {
				updateSmartShape(x, y);
				repaint();
			}
			break;

		case RCIR_ID:
		case SCIR_ID:
		case RREC_ID:
		case SREC_ID:
			updateSmartShape(x, y);
			repaint();
			break;

		case DELE_ID:
		case ADOB_ID:
		case MARK_ID:
		case COUN_ID:
		case RECT_ID:
		case TRIA_ID:
		case ELLI_ID:
		case FILA_ID:
		case FILB_ID:
		case FILC_ID:
		case FILD_ID:
			dragRect(x, y);
			break;

		case ROTA_ID:
			if (selectedComponent instanceof Rotatable) {
				Rotatable r = (Rotatable) selectedComponent;
				if (r.isSelectedToRotate() && rotationHandle >= 0) {
					r.rotateTo(x, y, rotationHandle);
					if (r instanceof Molecule)
						refreshForces();
					repaint();
				}
				else {
					if (selectedComponent instanceof Molecule) {
						dragSelected = true;
						Molecule mol = (Molecule) selectedComponent;
						if (mol.contains(x, y)) {
							mol.setSelected(true);
							mol.translateTo(x - clickPoint.x, y - clickPoint.y);
							boundary.setRBC(mol);
							refreshForces();
							repaint();
						}
					}
				}
			}
			break;

		case VELO_ID:
			if (readyToAdjustVelocityVector) {
				if (selectedComponent instanceof Atom) {
					((Atom) selectedComponent).setVelocityHandleLocation(x, y);
					repaint();
				}
			}
			break;

		case HEAT_ID:
		case COOL_ID:
			pointHeater.setLocation(x, y);
			repaint();
			break;

		case MEAS_ID:
			if (readyToAdjustDistanceVector && indexOfSelectedMeasurement >= 0) {
				if (selectedComponent instanceof Atom) {
					Atom a = (Atom) selectedComponent;
					Atom a1 = whichAtomOtherThan(x, y, a);
					if (a1 != null) {
						a.setMeasurement(indexOfSelectedMeasurement, new Integer(a1.getIndex()));
					}
					else {
						a.setMeasurement(indexOfSelectedMeasurement, new Point(x, y));
					}
					repaint();
				}
			}
			break;

		case DUPL_ID:
			if (selectedComponent instanceof Atom) {
				((Atom) selectedComponent).translateTo(x, y);
				boundary.setRBC((Atom) selectedComponent);
			}
			else if (selectedComponent instanceof Molecule) {
				Molecule mol = (Molecule) selectedComponent;
				mol.translateTo(x, y);
				boundary.setRBC(mol);
			}
			else if (selectedComponent instanceof RectangularObstacle) {
				((RectangularObstacle) selectedComponent).translateTo(x - clickPoint.x, y - clickPoint.y);
			}
			else if (selectedComponent instanceof Layered) {
				((Layered) selectedComponent).setLocation(x - clickPoint.x, y - clickPoint.y);
			}
			repaint();
			break;

		case SBOU_ID:
			if (boundary.getType() != RectangularBoundary.DBC_ID) {
				double xmin = 0.0, ymin = 0.0, xbox = getSize().width, ybox = getSize().height;
				double x1, y1, x2, y2;
				int handle = boundary.getHandle();
				switch (handle) {
				case RectangularBoundary.UPPER_LEFT:
					if (model.isEmpty()) {
						xmin = x;
						xbox = boundary.width + boundary.x - x;
						ymin = y;
						ybox = boundary.height + boundary.y - y;
					}
					else {
						x1 = x;
						x2 = model.getIDMinX() >= 0 ? model.getMinX() - 0.5 * atom[model.getIDMinX()].getSigma()
								: model.getMinX();
						if (x1 > x2)
							x1 = x2;
						y1 = y;
						y2 = model.getIDMinY() >= 0 ? model.getMinY() - 0.5 * atom[model.getIDMinY()].getSigma()
								: model.getMinY();
						if (y1 > y2)
							y1 = y2;
						xmin = x1;
						xbox = boundary.width + boundary.x - x1;
						ymin = y1;
						ybox = boundary.height + boundary.y - y1;
					}
					break;
				case RectangularBoundary.UPPER_RIGHT:
					if (model.isEmpty()) {
						xmin = boundary.x;
						xbox = x - xmin;
						ymin = y;
						ybox = boundary.y + boundary.height - y;
					}
					else {
						x1 = x;
						x2 = model.getIDMaxX() >= 0 ? model.getMaxX() + 0.5 * atom[model.getIDMaxX()].getSigma()
								: model.getMaxX();
						if (x1 < x2)
							x1 = x2;
						y1 = y;
						y2 = model.getIDMinY() >= 0 ? model.getMinY() - 0.5 * atom[model.getIDMinY()].getSigma()
								: model.getMinY();
						if (y1 > y2)
							y1 = y2;
						xmin = boundary.x;
						xbox = x1 - xmin;
						ymin = y1;
						ybox = boundary.y + boundary.height - y1;
					}
					break;
				case RectangularBoundary.LOWER_LEFT:
					if (model.isEmpty()) {
						xmin = x;
						xbox = boundary.x + boundary.width - x;
						ymin = boundary.y;
						ybox = y - ymin;
					}
					else {
						x1 = x;
						x2 = model.getIDMinX() >= 0 ? model.getMinX() - 0.5 * atom[model.getIDMinX()].getSigma()
								: model.getMinX();
						if (x1 > x2)
							x1 = x2;
						y1 = y;
						y2 = model.getIDMaxY() >= 0 ? model.getMaxY() + 0.5 * atom[model.getIDMaxY()].getSigma()
								: model.getMaxY();
						if (y1 < y2)
							y1 = y2;
						xmin = x1;
						xbox = boundary.x + boundary.width - x1;
						ymin = boundary.y;
						ybox = y1 - ymin;
					}
					break;
				case RectangularBoundary.LOWER_RIGHT:
					if (model.isEmpty()) {
						xmin = boundary.x;
						xbox = x - xmin;
						ymin = boundary.y;
						ybox = y - ymin;
					}
					else {
						x1 = x;
						x2 = model.getIDMaxX() >= 0 ? model.getMaxX() + 0.5 * atom[model.getIDMaxX()].getSigma()
								: model.getMaxX();
						if (x1 < x2)
							x1 = x2;
						y1 = y;
						y2 = model.getIDMaxY() >= 0 ? model.getMaxY() + 0.5 * atom[model.getIDMaxY()].getSigma()
								: model.getMaxY();
						if (y1 < y2)
							y1 = y2;
						xmin = boundary.x;
						xbox = x1 - xmin;
						ymin = boundary.y;
						ybox = y1 - ymin;
					}
					break;
				}
				if (handle == RectangularBoundary.UPPER_LEFT || handle == RectangularBoundary.LOWER_LEFT
						|| handle == RectangularBoundary.UPPER_RIGHT || handle == RectangularBoundary.LOWER_RIGHT) {
					boundary.setRect(xmin, ymin, xbox, ybox);
					repaint();
				}
			}
			break;

		}

		if (useJmol)
			refreshJmol();
		e.consume();

	}

	void processMouseReleased(MouseEvent e) {

		super.processMouseReleased(e);

		if (model.getJob() != null && !model.getJob().isStopped()) {
			processUserFieldsUponKeyOrMouseReleased();
			return;
		}
		if (ModelerUtilities.isRightClick(e))
			return;
		int x = e.getX();
		int y = e.getY();

		/* actions without an ID */

		if (obsRectSelected > RectangularObstacle.INSIDE) {
			if (selectedComponent instanceof RectangularObstacle) {
				if (finalizeObstacleLocation((RectangularObstacle) selectedComponent)) {
					if (!doNotFireUndoEvent) {
						model.getUndoManager().undoableEditHappened(
								new UndoableEditEvent(model, new UndoableResizing(selectedComponent)));
						updateUndoUIComponents();
					}
				}
				obsRectSelected = -1;
			}
		}

		/* actions with an ID */

		switch (actionID) {

		case SELE_ID:
			if (dragSelected) {
				boolean b = false;
				if (selectedComponent instanceof Molecule) {
					if (finalizeMoleculeLocation((Molecule) selectedComponent))
						b = true;
				}
				else if (selectedComponent instanceof Atom) {
					if (finalizeAtomLocation((Atom) selectedComponent, false)) {
						b = true;
					}
					else {
						errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
					}
				}
				else if (selectedComponent instanceof RectangularObstacle) {
					if (finalizeObstacleLocation((RectangularObstacle) selectedComponent))
						b = true;
				}
				else if (selectedComponent instanceof Layered) {
					b = true;
					ModelComponent host = ((Layered) selectedComponent).getHost();
					if (host instanceof Atom) {
						b = finalizeAtomLocation((Atom) host, false);
						if (!b)
							errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
					}
					else if (host instanceof RadialBond) {
						b = finalizeMoleculeLocation(((RadialBond) host).getMolecule());
					}
					else if (host instanceof RectangularObstacle) {
						b = finalizeObstacleLocation((RectangularObstacle) host);
					}
				}
				if (b) {
					model.notifyChange();
					setCursor(UserAction.getCursor(actionID));
					if (selectedComponent != null) {
						if (!doNotFireUndoEvent) {
							model.getUndoManager().undoableEditHappened(
									new UndoableEditEvent(model, new UndoableMoving(selectedComponent)));
							updateUndoUIComponents();
						}
					}
				}
				dragSelected = false;
			}
			break;

		case ADDA_ID:
		case ADDB_ID:
		case ADDC_ID:
		case ADDD_ID:
			addAtomIndicator.x = addAtomIndicator.y = -1;
			repaint();
			break;

		case ADDI_ID:
		case WATE_ID:
		case BENZ_ID:
		case ADCH_ID:
			if (addObjectIndicator != null) {
				addObjectIndicator.setPainted(false);
				repaint();
			}
			break;

		case RCUR_ID:
		case SFRE_ID:
			if (e.getClickCount() > 1) {
				if (smartPoly != null && smartShape != null) {
					if (smartPoly.npoints >= 2) {
						int dx = x - smartPoly.xpoints[0];
						int dy = y - smartPoly.ypoints[0];
						populateSmartShape(dx * dx + dy * dy < 25);
						smartPoly = null;
					}
				}
				repaint();
			}
			break;

		case SCUR_ID:
			if (e.getClickCount() > 1) {
				if (smartPoly != null && smartShape != null) {
					if (smartPoly.npoints >= 3) {
						populateSmartShape(true);
						smartPoly = null;
					}
				}
				repaint();
			}
			break;

		case RCIR_ID:
		case SCIR_ID:
		case RREC_ID:
		case SREC_ID:
			if (e.getClickCount() > 1) {
				populateSmartShape(true);
				repaint();
			}
			break;

		case HEAT_ID:
			pointHeater.doWork(model, true);
			break;

		case COOL_ID:
			pointHeater.doWork(model, false);
			break;

		case MEAS_ID:
			if (readyToAdjustDistanceVector) {
				if (selectedComponent instanceof Atom && indexOfSelectedMeasurement >= 0) {
					if (x < 0 || x > getWidth() || y < 0 || y > getHeight()) {
						((Atom) selectedComponent).removeMeasurement(indexOfSelectedMeasurement);
						indexOfSelectedMeasurement = -1;
						setCursor(previousCursor);
						repaint();
					}
				}
				readyToAdjustDistanceVector = false;
			}
			break;

		case VELO_ID:
			if (readyToAdjustVelocityVector) {
				if (selectedComponent instanceof Atom) {
					Atom a = (Atom) selectedComponent;
					a.storeCurrentState();
					a.setVx((x - a.getRx()) / velocityFlavor.getLength());
					a.setVy((y - a.getRy()) / velocityFlavor.getLength());
					model.notifyChange();
					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					repaint();
					if (!doNotFireUndoEvent) {
						model.getUndoManager().undoableEditHappened(
								new UndoableEditEvent(model, new UndoableResizing(selectedComponent)));
						updateUndoUIComponents();
					}
				}
				readyToAdjustVelocityVector = false;
			}
			break;

		case ROTA_ID:
			if (selectedComponent instanceof Rotatable) {
				if (rotationHandle >= 0) {
					Rotatable r = (Rotatable) selectedComponent;
					if (r.isSelectedToRotate()) {
						if (finalizeRotation()) {
							model.notifyChange();
							// r.setSelectedToRotate(false);
							if (selectedComponent != null && !doNotFireUndoEvent) {
								model.getUndoManager().undoableEditHappened(
										new UndoableEditEvent(model, new UndoableMoving(selectedComponent)));
								updateUndoUIComponents();
							}
						}
					}
					rotationHandle = -1;
				}
				else {
					if (selectedComponent instanceof Molecule) {
						if (finalizeMoleculeLocation((Molecule) selectedComponent)) {
							model.notifyChange();
							if (selectedComponent != null) {
								if (!doNotFireUndoEvent) {
									model.getUndoManager().undoableEditHappened(
											new UndoableEditEvent(model, new UndoableMoving(selectedComponent)));
									updateUndoUIComponents();
								}
							}
							dragSelected = false;
						}
						else {
							((Molecule) selectedComponent).setSelected(true);
						}
					}
				}
			}
			break;

		case DUPL_ID:
			if (selectedComponent instanceof Atom) {
				if (finalizeAtomDuplication()) {
					model.notifyChange();
					model.setNumberOfParticles(nAtom);
					if (!doNotFireUndoEvent) {
						model.getUndoManager()
								.undoableEditHappened(
										new UndoableEditEvent(model, new UndoableInsertion(
												UndoAction.INSERT_A_PARTICLE, x, y)));
						updateUndoUIComponents();
					}
				}
				else {
					errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
				}
			}
			else if (selectedComponent instanceof Molecule) {
				if (finalizeMoleculeDuplication()) {
					model.notifyChange();
					model.setNumberOfParticles(nAtom);
					if (!doNotFireUndoEvent) {
						model.getUndoManager().undoableEditHappened(
								new UndoableEditEvent(model, new UndoableInsertion(UndoAction.INSERT_A_MOLECULE, x, y,
										((Molecule) selectedComponent).size())));
						updateUndoUIComponents();
					}
				}
			}
			else if (selectedComponent instanceof RectangularObstacle) {
				if (finalizeObstacleDuplication()) {
					model.notifyChange();
					if (!doNotFireUndoEvent) {
						model.getUndoManager().undoableEditHappened(
								new UndoableEditEvent(model, new UndoableInsertion(ADOB_ID)));
						updateUndoUIComponents();
					}
				}
			}
			else if (selectedComponent instanceof Layered) {
				model.notifyChange();
				if (!doNotFireUndoEvent) {
					model.getUndoManager().undoableEditHappened(
							new UndoableEditEvent(model, new UndoableLayeredComponentOperation(
									UndoAction.INSERT_LAYERED_COMPONENT, (Layered) selectedComponent)));
					updateUndoUIComponents();
				}
			}
			setSelectedComponent(null);
			break;

		case MARK_ID:
			for (int k = 0; k < nAtom; k++)
				atom[k].setMarked(selectedArea.contains(atom[k].getRx(), atom[k].getRy()));
			if (!molecules.isEmpty()) {
				Object obj = null;
				synchronized (molecules.getSynchronizationLock()) {
					for (Iterator it = molecules.iterator(); it.hasNext();) {
						obj = it.next();
						if (obj instanceof MolecularObject) {
							((MolecularObject) obj).setMarked(((MolecularObject) obj).intersects(selectedArea));
						}
					}
				}
			}
			selectedArea.setSize(0, 0);
			repaint();
			break;

		case FILA_ID:
			fillSelectedArea(Element.ID_NT);
			break;
		case FILB_ID:
			fillSelectedArea(Element.ID_PL);
			break;
		case FILC_ID:
			fillSelectedArea(Element.ID_WS);
			break;
		case FILD_ID:
			fillSelectedArea(Element.ID_CK);
			break;

		case COUN_ID:
			int n = 0,
			nNt = 0,
			nPl = 0,
			nWs = 0,
			nCk = 0,
			m = 0;
			for (int k = 0; k < nAtom; k++) {
				if (selectedArea.contains(atom[k].getRx(), atom[k].getRy())) {
					n++;
					switch (atom[k].getID()) {
					case Element.ID_NT:
						nNt++;
						break;
					case Element.ID_PL:
						nPl++;
						break;
					case Element.ID_WS:
						nWs++;
						break;
					case Element.ID_CK:
						nCk++;
						break;
					}
				}
			}
			if (!bonds.isEmpty()) {
				RadialBond rb = null;
				synchronized (bonds.getSynchronizationLock()) {
					for (Iterator it = bonds.iterator(); it.hasNext();) {
						rb = (RadialBond) it.next();
						if (selectedArea.contains(0.5 * (rb.getAtom1().getRx() + rb.getAtom2().getRx()), 0.5 * (rb
								.getAtom1().getRy() + rb.getAtom2().getRy())))
							m++;
					}
				}
			}
			if (n != 0 || m != 0) {
				String s = MDView.getInternationalText("CountingResult");
				String s1 = MDView.getInternationalText("SelectedAreaContains");
				String s2 = MDView.getInternationalText("Particle");
				JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(this), (s1 != null ? s1
						: "The selected area contains ")
						+ n
						+ (s2 != null ? s2 : " particles")
						+ ":\n"
						+ (nNt == 0 ? "" : nNt + " Nt, ")
						+ (nPl == 0 ? "" : nPl + " Pl, ")
						+ (nWs == 0 ? "" : nWs + " Ws, ")
						+ (nCk == 0 ? "" : nCk + " Ck") + (m == 0 ? "" : '\n' + "and " + m + " bonds."), s != null ? s
						: "Counting result", JOptionPane.INFORMATION_MESSAGE);
			}
			selectedArea.setSize(0, 0);
			repaint();
			break;

		case DELE_ID:
			removeSelectedArea();
			break;

		case SBOU_ID:
			int handle = boundary.getHandle();
			if (handle == RectangularBoundary.UPPER_LEFT || handle == RectangularBoundary.UPPER_RIGHT
					|| handle == RectangularBoundary.LOWER_LEFT || handle == RectangularBoundary.LOWER_RIGHT) {
				boundary.setHandle(-1);
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				model.notifyChange();
				repaint();
			}
			break;

		case ADOB_ID:
			if (selectedArea.width > 0 && selectedArea.height > 0) {
				RectangularObstacle r2d = new RectangularObstacle(selectedArea);
				if (!intersects(r2d) && boundary.contains(r2d)) {
					obstacles.add(r2d);
					r2d.setSelected(true);
					model.notifyChange();
					if (!doNotFireUndoEvent) {
						model.getUndoManager().undoableEditHappened(
								new UndoableEditEvent(model, new UndoableInsertion(ADOB_ID)));
						updateUndoUIComponents();
					}
				}
				else {
					errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
				}
				selectedArea.setSize(0, 0);
				repaint();
			}
			break;

		}

		if (useJmol)
			refreshJmol();
		e.consume();

	}

	void processMouseExited(MouseEvent e) {
		if (model.getJob() != null && !model.getJob().isStopped())
			return;
		super.processMouseExited(e);
		if (actionID == ADDA_ID || actionID == ADDB_ID || actionID == ADDC_ID || actionID == ADDD_ID) {
			addAtomIndicator.x = addAtomIndicator.y = -1;
		}
		else if (actionID == ADDI_ID || actionID == WATE_ID || actionID == BENZ_ID || actionID == ADCH_ID) {
			if (addObjectIndicator != null)
				addObjectIndicator.setPainted(false);
		}
		else if (actionID == HEAT_ID || actionID == COOL_ID) {
			pointHeater.setLocation(-1, -1);
		}
		repaint();
		e.consume();
	}

	private void moveHostTo(ModelComponent host, double x, double y) {
		if (host instanceof Atom) {
			Atom a = (Atom) host;
			if (a.isBonded()) {
				Molecule m = model.getMolecules().getMolecule(a);
				m.translateAtomTo(a, x, y);
				boundary.setRBC(m);
			}
			else {
				a.translateTo(x, y);
				boundary.setRBC(a);
			}
			refreshForces();
		}
		else if (host instanceof RadialBond) {
			RadialBond rBond = (RadialBond) host;
			Molecule m = rBond.getMolecule();
			m.translateBondCenterTo(rBond, x, y);
			boundary.setRBC(m);
			refreshForces();
		}
		else if (host instanceof RectangularObstacle) {
			RectangularObstacle obs = (RectangularObstacle) host;
			obs.translateCenterTo(x, y);
		}
	}

	private void fillSelectedArea(int element) {
		boolean b = false;
		for (int k = 0; k < nAtom; k++) {
			if (selectedArea.contains(atom[k].getRx(), atom[k].getRy())) {
				b = true;
				break;
			}
		}
		if (obstacles != null) {
			int n = obstacles.size();
			if (n > 0) {
				for (int k = 0; k < n; k++) {
					if (selectedArea.intersects(obstacles.get(k))) {
						b = true;
						break;
					}
				}
			}
		}
		if (b) {
			JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(this),
					"There are objects in this area. Please select an empty area.", "Message",
					JOptionPane.ERROR_MESSAGE);
		}
		else {
			fillAreaWithAtoms(selectedArea, HEXAGONAL_LATTICE, element);
		}
		selectedArea.setSize(0, 0);
		repaint();
	}

	private Runnable jmolNotifier = new Runnable() {
		public void run() {
			// System.out.println(getSize());
			jmolRenderer.openClientObject(model);
		}
	};

	// FIXME: this method is called too many times when the model is loaded.
	void notifyJmol() {
		if (useJmol && jmolRenderer != null)
			EventQueue.invokeLater(jmolNotifier);
	}

	public void bondChanged(BondChangeEvent e) {
		if (useJmol && jmolRenderer != null) {
			jmolRenderer.renderBonds();
		}
	}

	public void notifyNOPChange() {
		nAtom = model.getNumberOfParticles();
		if (useJmol && nAtom > 0)
			notifyJmol();
	}

	int getSteeringForceScale() {
		int s = 0;
		for (int i = 0; i < nAtom; i++) {
			if (atom[i].getUserField() != null) {
				s = (int) (atom[i].getUserField().getGear() * 4.0);
				break;
			}
		}
		RectangularObstacle obs = null;
		if (s == 0 && obstacles != null && !obstacles.isEmpty()) {
			synchronized (obstacles.getSynchronizationLock()) {
				for (int iobs = 0, nobs = obstacles.size(); iobs < nobs; iobs++) {
					obs = obstacles.get(iobs);
					if (obs.getUserField() != null) {
						s = (int) (obs.getUserField().getGear() * 4.0);
						break;
					}
				}
			}
		}
		return s;
	}

	private void moveOrSteerObjectUsingKeys(int keyID) {

		switch (keyID) {
		case KeyEvent.VK_UP:
			keyPressedCode = keyPressedCode | UP_PRESSED;
			break;
		case KeyEvent.VK_DOWN:
			keyPressedCode = keyPressedCode | DOWN_PRESSED;
			break;
		case KeyEvent.VK_LEFT:
			keyPressedCode = keyPressedCode | LEFT_PRESSED;
			break;
		case KeyEvent.VK_RIGHT:
			keyPressedCode = keyPressedCode | RIGHT_PRESSED;
			break;
		}
		byte dx = 0, dy = 0;
		if ((keyPressedCode & UP_PRESSED) == UP_PRESSED)
			dy--;
		if ((keyPressedCode & DOWN_PRESSED) == DOWN_PRESSED)
			dy++;
		if ((keyPressedCode & LEFT_PRESSED) == LEFT_PRESSED)
			dx--;
		if ((keyPressedCode & RIGHT_PRESSED) == RIGHT_PRESSED)
			dx++;
		if (dx == 0 && dy == 0)
			return;

		if (selectedComponent != null) {
			if (!isEditable() && !selectedComponent.isDraggable()) {
				showActionTip("<html><font color=red>The selected object is not nudgable!</font></html>", 10, 10);
			}
			else {
				if (selectedComponent instanceof Atom) {
					Atom a = (Atom) selectedComponent;
					if (!a.isBonded()) {
						a.storeCurrentState();
						a.translateBy(dx, dy);
						int x = (int) a.getRx();
						int y = (int) a.getRy();
						if (a.getRestraint() != null) {
							int amp = (int) (400.0 / a.getRestraint().getK());
							Vector2D loc = moveSpring(x, y, (int) a.getRestraint().getX0(), (int) a.getRestraint()
									.getY0(), 0, amp);
							if (loc == null)
								return;
							a.translateTo(loc.getX(), loc.getY());
						}
						finalizeAtomLocation(a, false);
					}
				}
				else if (selectedComponent instanceof Molecule) {
					Molecule mol = (Molecule) selectedComponent;
					mol.storeCurrentState();
					mol.translateBy(dx, dy);
					finalizeMoleculeLocation((Molecule) selectedComponent);
					refreshForces();
				}
				else if (selectedComponent instanceof RectangularObstacle) {
					RectangularObstacle r = (RectangularObstacle) selectedComponent;
					r.storeCurrentState();
					r.translateBy(dx, dy);
					finalizeObstacleLocation(r);
				}
				else if (selectedComponent instanceof Layered) {
					selectedComponent.storeCurrentState();
					((Layered) selectedComponent).translateBy(dx, dy);
				}
				model.notifyChange();
			}
		}
		else {
			UserField uf = null;
			for (int i = 0; i < nAtom; i++) {
				uf = atom[i].getUserField();
				if (uf != null) {
					uf.setAngle(dx, dy);
					switch (uf.getMode()) {
					case UserField.FORCE_MODE:
						uf.setIntensity(UserField.INCREMENT * uf.getGear());
						break;
					case UserField.IMPULSE1_MODE:
						uf.increaseGear(1);
						break;
					}
				}
			}
			if (obstacles != null && !obstacles.isEmpty()) {
				RectangularObstacle obs = null;
				synchronized (obstacles.getSynchronizationLock()) {
					for (Iterator it = obstacles.iterator(); it.hasNext();) {
						obs = (RectangularObstacle) it.next();
						uf = obs.getUserField();
						if (uf != null) {
							uf.setAngle(dx, dy);
							switch (uf.getMode()) {
							case UserField.FORCE_MODE:
								uf.setIntensity(UserField.INCREMENT * uf.getGear());
								break;
							case UserField.IMPULSE1_MODE:
								uf.increaseGear(1);
								break;
							}
						}
					}
				}
			}
			refreshForces();
		}

		if (useJmol)
			refreshJmol();

	}

	void processKeyPressed(KeyEvent e) {
		super.processKeyPressed(e);
		int keyID = e.getKeyCode();
		moveOrSteerObjectUsingKeys(keyID);
		if (model.getJob() == null || model.getJob().isStopped())
			repaint();
		// KeyEvent must be consumed, otherwise the keyboard manager will be confused when the KeyEvent
		// should be applied to this view or to its parent component. WARNING!!! This treatment can cause
		// key binding to fail. You MUST set the onKeyRelease flag to be true when setting the key stroke
		// for a binding.
		if (hasFocus())
			e.consume();
	}

	private class UndoableInsertion extends AbstractUndoableEdit {

		private String presentationName = "";
		private int undoID;
		private double x, y;
		private int nInserted;
		private Object buffer;
		private Rectangle area;
		private int latticeType = HEXAGONAL_LATTICE;
		private int elementID = Element.ID_NT;

		UndoableInsertion(int undoID) {
			switch (actionID) {
			case ADDA_ID:
				presentationName = "Inserting Nt";
				break;
			case ADDB_ID:
				presentationName = "Inserting Pl";
				break;
			case ADDC_ID:
				presentationName = "Inserting Ws";
				break;
			case ADDD_ID:
				presentationName = "Inserting Ck";
				break;
			case FILA_ID:
				presentationName = "Filling Area with Nt Atoms";
				break;
			case FILB_ID:
				presentationName = "Filling Area with Pl Atoms";
				break;
			case FILC_ID:
				presentationName = "Filling Area with Ws Atoms";
				break;
			case FILD_ID:
				presentationName = "Filling Area with Ck Atoms";
				break;
			case ADDI_ID:
				presentationName = "Inserting Diatomic Molecule";
				break;
			case WATE_ID:
				presentationName = "Inserting Triatomic Molecule";
				break;
			case BENZ_ID:
				presentationName = "Inserting Benzene Molecule";
				break;
			case ADCH_ID:
				presentationName = "Inserting Chain Molecule";
				break;
			case ADOB_ID:
				presentationName = "Inserting Rectangular Obstacle";
				break;
			case BBON_ID:
				presentationName = "Making Radial Bond";
				break;
			case BBEN_ID:
				presentationName = "Making Angular Bond";
				break;
			case DUPL_ID:
				presentationName = "Duplication";
				break;
			case SREC_ID:
				presentationName = "Inserting Rectangular Molecular Surface";
				break;
			case SCIR_ID:
				presentationName = "Inserting Circular Molecular Surface";
				break;
			case SCUR_ID:
				presentationName = "Inserting Curved Molecular Surface";
				break;
			case SFRE_ID:
				presentationName = "Inserting Free-Form Molecular Surface";
				break;
			case RREC_ID:
				presentationName = "Inserting Rectangular Molecular Ribbon";
				break;
			case RCIR_ID:
				presentationName = "Inserting Circular Molecular Ribbon";
				break;
			case RCUR_ID:
				presentationName = "Inserting Curved Molecular Ribbon";
				break;
			case AACD_ID:
				presentationName = "Growing Polypeptide";
				break;
			case ANTD_ID:
				presentationName = "Growing DNA Strand";
				break;
			}
			this.undoID = undoID;
			if (presentationName.equals("")) {
				switch (undoID) {
				case UndoAction.INSERT_A_MOLECULE:
					presentationName = "Inserting Molecule";
					break;
				case UndoAction.INSERT_A_PARTICLE:
					presentationName = "Inserting Atom";
					break;
				}
			}
		}

		UndoableInsertion(int undoID, Object buffer) {
			this(undoID);
			this.buffer = buffer;
		}

		UndoableInsertion(int undoID, double x, double y) {
			this(undoID);
			this.x = x;
			this.y = y;
		}

		UndoableInsertion(int undoID, double x, double y, int nInserted) {
			this(undoID, x, y);
			this.nInserted = nInserted;
		}

		UndoableInsertion(int undoID, double x, double y, int nInserted, Rectangle a, int lattice, int id) {
			this(undoID, x, y, nInserted);
			area = new Rectangle(a);
			latticeType = lattice;
			elementID = id;
		}

		public String getPresentationName() {
			return presentationName;
		}

		public void undo() {
			super.undo();
			switch (undoID) {
			case UndoAction.INSERT_A_PARTICLE:
				nAtom--;
				if (!model.getRecorderDisabled())
					atom[nAtom].initializeMovieQ(-1);
				if (atom[nAtom].isSelected())
					setSelectedComponent(null);
				model.setNumberOfParticles(nAtom);
				break;
			case ADOB_ID:
				RectangularObstacle ro = obstacles.get(obstacles.size() - 1);
				if (!model.getRecorderDisabled())
					ro.initializeMovieQ(-1);
				if (ro.isSelected())
					setSelectedComponent(null);
				obstacles.remove(ro);
				buffer = ro;
				break;
			case ADDI_ID:
				List<Integer> list = new ArrayList<Integer>();
				int n = model.getNumberOfParticles();
				list.add(n - 1);
				list.add(n - 2);
				removeMarkedAtoms(list);
				break;
			case WATE_ID:
				list = new ArrayList<Integer>();
				n = model.getNumberOfParticles();
				list.add(n - 1);
				list.add(n - 2);
				list.add(n - 3);
				removeMarkedAtoms(list);
				break;
			case BENZ_ID:
				list = new ArrayList<Integer>();
				n = model.getNumberOfParticles();
				for (int i = 0; i < 12; i++)
					list.add(n - 1 - i);
				removeMarkedAtoms(list);
				break;
			case ADCH_ID:
				list = new ArrayList<Integer>();
				n = model.getNumberOfParticles();
				for (int i = 0; i < ChainConfigure.number; i++)
					list.add(n - 1 - i);
				removeMarkedAtoms(list);
				break;
			case UndoAction.INSERT_A_MOLECULE:
				if (nInserted > 0) {
					list = new ArrayList<Integer>();
					n = model.getNumberOfParticles();
					Molecule mol = molecules.getMolecule(atom[n - 1]);
					if (mol.isSelected())
						setSelectedComponent(null);
					for (int i = 0; i < nInserted; i++)
						list.add(n - 1 - i);
					removeMarkedAtoms(list);
				}
				break;
			case UndoAction.FILL_AREA_WITH_PARTICLES:
				if (nInserted > 0) {
					list = new ArrayList<Integer>();
					n = model.getNumberOfParticles();
					for (int i = n - nInserted; i < n; i++)
						list.add(i);
					removeMarkedAtoms(list);
				}
				break;
			case BBON_ID:
				buffer = bonds.get(bonds.size() - 1);
				bonds.remove((RadialBond) buffer);
				if (((RadialBond) buffer).isSelected())
					setSelectedComponent(null);
				MoleculeCollection.sort(model);
				break;
			case BBEN_ID:
				buffer = bends.get(bends.size() - 1);
				bends.remove((AngularBond) buffer);
				if (((AngularBond) buffer).isSelected())
					setSelectedComponent(null);
				break;
			case AACD_ID:
				list = new ArrayList<Integer>();
				list.add(model.getNumberOfParticles() - 1);
				removeMarkedAtoms(list);
				break;
			case ANTD_ID:
				list = new ArrayList<Integer>();
				list.add(model.getNumberOfParticles() - 1);
				list.add(model.getNumberOfParticles() - 2);
				removeMarkedAtoms(list);
				break;
			case SREC_ID:
			case SCUR_ID:
			case SCIR_ID:
			case SFRE_ID:
			case RREC_ID:
			case RCIR_ID:
			case RCUR_ID:
				list = new ArrayList<Integer>();
				n = model.getNumberOfParticles();
				Molecule mol = molecules.getMolecule(atom[n - 1]);
				if (mol.isSelected())
					setSelectedComponent(null);
				for (int i = 0; i < mol.size(); i++)
					list.add(n - 1 - i);
				removeMarkedAtoms(list);
				break;
			}
			model.notifyChange();
			repaint();
		}

		public void redo() {
			super.redo();
			doNotFireUndoEvent = true;
			switch (undoID) {
			case UndoAction.INSERT_A_PARTICLE:
				if (!model.getRecorderDisabled())
					atom[nAtom].initializeMovieQ(model.getMovie().getCapacity());
				atom[nAtom].setSelected(true);
				nAtom++;
				model.setNumberOfParticles(nAtom);
				break;
			case ADOB_ID:
				if (buffer instanceof RectangularObstacle) {
					RectangularObstacle r = (RectangularObstacle) buffer;
					if (!model.getRecorderDisabled())
						r.initializeMovieQ(model.getMovie().getCapacity());
					obstacles.add(r);
					r.setSelected(true);
				}
				break;
			case ADDI_ID:
				insertAMolecule(x, y, new DiatomicMolecule(DiatomicConfigure.typeOfA, DiatomicConfigure.typeOfB,
						DiatomicConfigure.distance, DiatomicConfigure.strength));
				break;
			case WATE_ID:
				insertAMolecule(x, y, new TriatomicMolecule(TriatomicConfigure.typeOfA, TriatomicConfigure.typeOfB,
						TriatomicConfigure.typeOfC, TriatomicConfigure.d12, TriatomicConfigure.s12,
						TriatomicConfigure.d23, TriatomicConfigure.s23, TriatomicConfigure.angle,
						TriatomicConfigure.strength));
				break;
			case BENZ_ID:
				insertAMolecule(x, y, new Benzene());
				break;
			case ADCH_ID:
				insertAMolecule(x, y, new ChainMolecule(ChainConfigure.typeOfAtom, ChainConfigure.number,
						ChainConfigure.growMode, ChainConfigure.distance, ChainConfigure.angle));
				break;
			case UndoAction.INSERT_A_MOLECULE:
				undoRemoveMarkedAtoms(nInserted);
				molecules.getMolecule(model.getAtom(model.getNumberOfParticles() - 1)).setSelected(true);
				break;
			case UndoAction.FILL_AREA_WITH_PARTICLES:
				if (area != null)
					fillAreaWithAtoms(area, latticeType, elementID);
				break;
			case BBON_ID:
				if (buffer instanceof RadialBond) {
					bonds.add((RadialBond) buffer);
					((RadialBond) buffer).setSelected(true);
					MoleculeCollection.sort(model);
				}
				break;
			case BBEN_ID:
				if (buffer instanceof AngularBond) {
					bends.add((AngularBond) buffer);
					((AngularBond) buffer).setSelected(true);
				}
				break;
			case AACD_ID:
				undoRemoveMarkedAtoms(1);
				break;
			case ANTD_ID:
				undoRemoveMarkedAtoms(2);
				break;
			case SREC_ID:
			case SCUR_ID:
			case SCIR_ID:
				if (buffer instanceof Object[]) {
					Object[] o = (Object[]) buffer;
					molecules.add(new CurvedSurface((Polygon) o[1], model));
					molecules.get(molecules.size() - 1).setSelected(true);
				}
				break;
			case RREC_ID:
			case RCIR_ID:
				if (buffer instanceof Object[]) {
					Object[] o = (Object[]) buffer;
					molecules.add(new CurvedRibbon((Polygon) o[1], model, true));
					molecules.get(molecules.size() - 1).setSelected(true);
				}
				break;
			case RCUR_ID:
				if (buffer instanceof Object[]) {
					Object[] o = (Object[]) buffer;
					molecules.add(new CurvedRibbon((Polygon) o[1], model, ((Boolean) o[0]).booleanValue()));
					molecules.get(molecules.size() - 1).setSelected(true);
				}
				break;
			case SFRE_ID:
				if (buffer instanceof Object[]) {
					Object[] o = (Object[]) buffer;
					boolean closed = ((Boolean) o[0]).booleanValue();
					molecules.add(closed ? new CurvedSurface((Polygon) o[1], model) : new CurvedRibbon((Polygon) o[1],
							model, false));
					molecules.get(molecules.size() - 1).setSelected(true);
				}
				break;
			}
			doNotFireUndoEvent = false;
			model.notifyChange();
			repaint();
		}

	}

	private class UndoableDeletion extends AbstractUndoableEdit {

		private String presentationName;
		private int undoID, nRemoved;
		private Object removedObject;
		private RectangularObstacle[] removedObstacles;
		private Layered[] removedLayers;

		UndoableDeletion(int undoID) {
			this.undoID = undoID;
			switch (undoID) {
			case UndoAction.BLOCK_REMOVE:
				presentationName = "Deletion";
				break;
			case SACD_ID:
				presentationName = "Shortening Polypeptide";
				break;
			case SNTD_ID:
				presentationName = "Shottening DNA Strand";
				break;
			case UndoAction.REMOVE_RADIAL_BOND:
				presentationName = "Removing Radial Bond";
				break;
			case UndoAction.REMOVE_ANGULAR_BOND:
				presentationName = "Removing Angular Bond";
				break;
			case UndoAction.REMOVE_OBSTACLE:
				presentationName = "Removing Rectangular Obstacle";
				break;
			}
		}

		UndoableDeletion(int undoID, Object removedObject) {
			this(undoID);
			this.removedObject = removedObject;
		}

		UndoableDeletion(int undoID, int nRemoved) {
			this(undoID);
			this.nRemoved = nRemoved;
		}

		UndoableDeletion(int undoID, int nRemoved, RectangularObstacle[] removedObstacles) {
			this(undoID, nRemoved);
			this.removedObstacles = removedObstacles;
		}

		UndoableDeletion(int undoID, int nRemoved, RectangularObstacle[] removedObstacles, Layered[] removedLayers) {
			this(undoID, nRemoved, removedObstacles);
			this.removedLayers = removedLayers;
		}

		public String getPresentationName() {
			return presentationName;
		}

		public void undo() {
			super.undo();
			switch (undoID) {
			case UndoAction.BLOCK_REMOVE:
				undoRemoveMarkedAtoms(nRemoved);
				if (removedObstacles != null) {
					for (RectangularObstacle i : removedObstacles)
						obstacles.add(i);
					if (nRemoved <= 0 && removedObstacles.length == 1)
						removedObstacles[0].setSelected(true);
				}
				if (removedLayers != null) {
					for (Layered i : removedLayers)
						layerBasket.add(i);
					if (nRemoved <= 0 && removedLayers.length == 1)
						((ModelComponent) removedLayers[0]).setSelected(true);
				}
				break;
			case UndoAction.REMOVE_RADIAL_BOND:
				if (removedObject instanceof Object[]) {
					Object[] o = (Object[]) removedObject;
					if (o[0] instanceof RadialBond) {
						bonds.add((RadialBond) o[0]);
						((RadialBond) o[0]).setSelected(true);
					}
					if (o[1] instanceof List) {
						List x = (List) o[1];
						for (Iterator it = x.iterator(); it.hasNext();)
							bends.add((AngularBond) it.next());
					}
					MoleculeCollection.sort(model);
				}
				break;
			case UndoAction.REMOVE_ANGULAR_BOND:
				if (removedObject instanceof AngularBond) {
					bends.add((AngularBond) removedObject);
					((AngularBond) removedObject).setSelected(true);
				}
				break;
			case UndoAction.REMOVE_OBSTACLE:
				if (removedObject instanceof RectangularObstacle) {
					RectangularObstacle r = (RectangularObstacle) removedObject;
					if (!model.getRecorderDisabled())
						r.initializeMovieQ(model.getMovie().getCapacity());
					obstacles.add(r);
					r.setSelected(true);
				}
				break;
			case SACD_ID:
				undoRemoveMarkedAtoms(1);
				break;
			case SNTD_ID:
				undoRemoveMarkedAtoms(2);
				break;
			}
			model.notifyChange();
			repaint();
		}

		public void redo() {
			super.redo();
			doNotFireUndoEvent = true;
			switch (undoID) {
			case UndoAction.BLOCK_REMOVE:
				List<Integer> list = new ArrayList<Integer>();
				int n = model.getNumberOfParticles();
				for (int i = 0; i < nRemoved; i++)
					list.add(n - 1 - i);
				removeMarkedAtoms(list);
				if (removedObstacles != null)
					for (RectangularObstacle o : removedObstacles)
						obstacles.remove(o);
				if (removedLayers != null)
					for (Layered l : removedLayers)
						layerBasket.remove(l);
				break;
			case UndoAction.REMOVE_RADIAL_BOND:
				if (removedObject instanceof Object[]) {
					Object[] o = (Object[]) removedObject;
					if (o[0] instanceof RadialBond) {
						bonds.remove((RadialBond) o[0]);
						if (((RadialBond) o[0]).isSelected())
							setSelectedComponent(null);
					}
					if (o[1] instanceof List) {
						List x = (List) o[1];
						for (Iterator it = x.iterator(); it.hasNext();)
							bends.remove((AngularBond) it.next());
					}
					MoleculeCollection.sort(model);
				}
				break;
			case UndoAction.REMOVE_ANGULAR_BOND:
				if (removedObject instanceof AngularBond) {
					bends.remove((AngularBond) removedObject);
					if (((AngularBond) removedObject).isSelected())
						setSelectedComponent(null);
				}
				break;
			case UndoAction.REMOVE_OBSTACLE:
				if (removedObject instanceof RectangularObstacle) {
					RectangularObstacle r = (RectangularObstacle) removedObject;
					if (!model.getRecorderDisabled())
						r.initializeMovieQ(-1);
					obstacles.remove(r);
					if (r.isSelected())
						setSelectedComponent(null);
				}
				break;
			case SACD_ID:
				list = new ArrayList<Integer>();
				list.add(model.getNumberOfParticles() - 1);
				removeMarkedAtoms(list);
				break;
			case SNTD_ID:
				list = new ArrayList<Integer>();
				list.add(model.getNumberOfParticles() - 2);
				list.add(model.getNumberOfParticles() - 1);
				removeMarkedAtoms(list);
				break;
			}
			doNotFireUndoEvent = false;
			model.notifyChange();
			repaint();
		}
	}

	private class UndoableMoving extends AbstractUndoableEdit {

		private ModelComponent mc;
		private double x, y, angle;
		private String presentationName;
		private Point2D redoCOM;
		private Map<Atom, Point2D> redoCRD;

		private void storeRedoState(Rotatable r) {
			if (r instanceof Molecule) {
				Molecule mol = (Molecule) r;
				redoCOM = mol.getCenterOfMass2D();
				if (redoCRD == null) {
					redoCRD = new HashMap<Atom, Point2D>();
				}
				else {
					redoCRD.clear();
				}
				Atom at;
				synchronized (mol.getSynchronizedLock()) {
					for (Iterator it = mol.iterator(); it.hasNext();) {
						at = (Atom) it.next();
						redoCRD.put(at, new Point2D.Double(at.getRx(), at.getRy()));
					}
				}
			}
			else if (r instanceof ImageComponent) {

			}
		}

		private void redo(Rotatable r) {
			if (r instanceof Molecule) {
				if (redoCOM == null || redoCRD.isEmpty())
					return;
				Molecule mol = (Molecule) r;
				Atom at;
				Point2D point;
				synchronized (mol.getSynchronizedLock()) {
					for (Iterator it = mol.iterator(); it.hasNext();) {
						at = (Atom) it.next();
						point = redoCRD.get(at);
						if (point != null) {
							at.setRx(point.getX());
							at.setRy(point.getY());
						}
					}
				}
			}
			else if (r instanceof ImageComponent) {
				if (angle != 0)
					((ImageComponent) r).setAngle((float) angle);
			}
		}

		UndoableMoving(ModelComponent mc) {
			this.mc = mc;
			x = mc.getRx();
			y = mc.getRy();
			if (mc instanceof ImageComponent) {
				angle = ((ImageComponent) mc).getAngle();
			}
			switch (actionID) {
			case ROTA_ID:
				presentationName = "Rotation";
				if (mc instanceof Rotatable)
					storeRedoState((Rotatable) mc);
				break;
			case SELE_ID:
				presentationName = "Translation";
				break;
			}
		}

		public void setPresentationName(String s) {
			presentationName = s;
		}

		public String getPresentationName() {
			return presentationName;
		}

		public void undo() {
			super.undo();
			mc.restoreState();
			if (!mc.isSelected())
				mc.setSelected(true);
			model.notifyChange();
			if (useJmol)
				refreshJmol();
			repaint();
		}

		public void redo() {
			super.redo();
			doNotFireUndoEvent = true;
			if (presentationName.equals("Translation")) {
				if (mc instanceof Atom)
					((Atom) mc).translateTo(x, y);
				else if (mc instanceof Molecule)
					((Molecule) mc).translateTo(x, y);
				else if (mc instanceof RectangularObstacle)
					((RectangularObstacle) mc).translateCenterTo(x, y);
				else if (mc instanceof Layered) {
					((Layered) mc).setLocation(x, y);
					ModelComponent host = ((Layered) mc).getHost();
					if (host instanceof Atom) {
						Atom a = (Atom) host;
						a.translateTo(((Layered) mc).getCenter());
					}
				}
			}
			else if (presentationName.equals("Rotation")) {
				if (mc instanceof Rotatable)
					redo((Rotatable) mc);
			}
			doNotFireUndoEvent = false;
			if (!mc.isSelected())
				mc.setSelected(true);
			model.notifyChange();
			if (useJmol)
				refreshJmol();
			repaint();
		}

	}

	private class UndoableResizing extends AbstractUndoableEdit {

		private ModelComponent mc;
		private double x, y, w, h;
		private String presentationName = "";

		UndoableResizing(ModelComponent mc) {
			this.mc = mc;
			if (mc instanceof RectangularObstacle) {
				x = ((RectangularObstacle) mc).getX();
				y = ((RectangularObstacle) mc).getY();
				w = ((RectangularObstacle) mc).getWidth();
				h = ((RectangularObstacle) mc).getHeight();
				presentationName = "Resizing Rectangular Obstacle";
			}
			else if (mc instanceof Atom) {
				if (actionID == VELO_ID) {
					x = ((Atom) mc).getVx();
					y = ((Atom) mc).getVy();
					presentationName = "Changing Velocity";
				}
			}
		}

		public String getPresentationName() {
			return presentationName;
		}

		public void undo() {
			super.undo();
			mc.restoreState();
			if (!mc.isSelected())
				mc.setSelected(true);
			model.notifyChange();
			repaint();
		}

		public void redo() {
			super.redo();
			doNotFireUndoEvent = true;
			if (mc instanceof RectangularObstacle) {
				((RectangularObstacle) mc).setRect(x, y, w, h);
			}
			else if (mc instanceof Atom) {
				((Atom) mc).setVx(x);
				((Atom) mc).setVy(y);
			}
			doNotFireUndoEvent = false;
			if (!mc.isSelected())
				mc.setSelected(true);
			model.notifyChange();
			repaint();
		}

	}

	/** Serializable state. */
	public static class State extends MDView.State {

		private byte displayStyle = StyleConstant.SPACE_FILLING;
		private int probeID = Element.ID_NT;
		private int efCellSize = 10;
		private boolean shading, chargeShading, showVVectors, showVDWCircles, showVDWLines, showChargeLines,
				showSSLines, showBPLines, showPVectors, showAVectors, showFVectors, showContour, showExcitation = true,
				showEFieldLines, useJmol;
		private double probeCharge;
		private int[] elementColors; // an array to store the colors of the four adjustable elements
		private Color[] moColors; // an array to store the colors of the molecular surface objects
		private float vdwLinesRatio = 2.0f, vdwLineThickness = 1;
		private byte vdwCircleStyle = StyleConstant.VDW_DOTTED_CIRCLE;

		public State() {
			super();
			elementColors = new int[Element.getNumberOfElements()];
			Arrays.fill(elementColors, 0xffffff);
			elementColors[Element.ID_PL] = 0x00ff00;
			elementColors[Element.ID_WS] = 0x0000ff;
			elementColors[Element.ID_CK] = 0xff00ff;
			elementColors[Element.ID_MO] = 0xffc800;
			elementColors[Element.ID_SP] = 0xffff00;
			elementColors[Element.ID_A] = 0x7da7d9;
			elementColors[Element.ID_C] = 0xc4df9a;
			elementColors[Element.ID_G] = 0xfdc588;
			elementColors[Element.ID_T] = 0xfff699;
			elementColors[Element.ID_U] = 0xfff699;
		}

		public void setUseJmol(boolean b) {
			useJmol = b;
		}

		public boolean getUseJmol() {
			return useJmol;
		}

		public void setDisplayStyle(byte i) {
			displayStyle = i;
		}

		public byte getDisplayStyle() {
			return displayStyle;
		}

		public void setMonochromatic(boolean b) {
			useJmol = !b;
		}

		public boolean getMonochromatic() {
			return !useJmol;
		}

		public void setShowVDWLines(boolean b) {
			showVDWLines = b;
		}

		public boolean getShowVDWLines() {
			return showVDWLines;
		}

		public void setVDWLinesRatio(float ratio) {
			vdwLinesRatio = ratio;
		}

		public float getVDWLinesRatio() {
			return vdwLinesRatio;
		}

		public void setVDWLineThickness(float thickness) {
			vdwLineThickness = thickness;
		}

		public float getVDWLineThickness() {
			return vdwLineThickness;
		}

		public void setShowVDWCircles(boolean b) {
			showVDWCircles = b;
		}

		public boolean getShowVDWCircles() {
			return showVDWCircles;
		}

		public void setVDWCircleStyle(byte i) {
			vdwCircleStyle = i;
		}

		public byte getVDWCircleStyle() {
			return vdwCircleStyle;
		}

		public void setShowChargeLines(boolean b) {
			showChargeLines = b;
		}

		public boolean getShowChargeLines() {
			return showChargeLines;
		}

		public void setShowSSLines(boolean b) {
			showSSLines = b;
		}

		public boolean getShowSSLines() {
			return showSSLines;
		}

		public void setShowBPLines(boolean b) {
			showBPLines = b;
		}

		public boolean getShowBPLines() {
			return showBPLines;
		}

		public void setShowEFieldLines(boolean b) {
			showEFieldLines = b;
		}

		public boolean getShowEFieldLines() {
			return showEFieldLines;
		}

		public void setShading(boolean b) {
			shading = b;
		}

		public boolean getShading() {
			return shading;
		}

		public void setChargeShading(boolean b) {
			chargeShading = b;
		}

		public boolean getChargeShading() {
			return chargeShading;
		}

		public void setShowExcitation(boolean b) {
			showExcitation = b;
		}

		public boolean getShowExcitation() {
			return showExcitation;
		}

		public void setShowVVectors(boolean b) {
			showVVectors = b;
		}

		public boolean getShowVVectors() {
			return showVVectors;
		}

		public void setShowPVectors(boolean b) {
			showPVectors = b;
		}

		public boolean getShowPVectors() {
			return showPVectors;
		}

		public void setShowAVectors(boolean b) {
			showAVectors = b;
		}

		public boolean getShowAVectors() {
			return showAVectors;
		}

		public void setShowFVectors(boolean b) {
			showFVectors = b;
		}

		public boolean getShowFVectors() {
			return showFVectors;
		}

		public void setElementColors(int[] i) {
			elementColors = i;
		}

		public int[] getElementColors() {
			return elementColors;
		}

		public void setShowContour(boolean b) {
			showContour = b;
		}

		public boolean getShowContour() {
			return showContour;
		}

		public void setProbeID(int id) {
			probeID = id;
		}

		public int getProbeID() {
			return probeID;
		}

		public void setProbeCharge(double c) {
			probeCharge = c;
		}

		public double getProbeCharge() {
			return probeCharge;
		}

		public void setEFCellSize(int i) {
			efCellSize = i;
		}

		public int getEFCellSize() {
			return efCellSize;
		}

		public void setMolecularObjectColors(Color[] c) {
			moColors = c;
		}

		public Color[] getMolecularObjectColors() {
			return moColors;
		}

	}

}