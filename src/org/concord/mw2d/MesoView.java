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
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.AbstractUndoableEdit;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.math.Vector2D;
import org.concord.modeler.ui.IconPool;
import org.concord.modeler.util.FileUtilities;
import org.concord.mw2d.models.Boundary;
import org.concord.mw2d.models.EllipseComponent;
import org.concord.mw2d.models.GayBerneParticle;
import org.concord.mw2d.models.ImageComponent;
import org.concord.mw2d.models.Layered;
import org.concord.mw2d.models.LineComponent;
import org.concord.mw2d.models.MDModel;
import org.concord.mw2d.models.MesoModel;
import org.concord.mw2d.models.ModelComponent;
import org.concord.mw2d.models.PointRestraint;
import org.concord.mw2d.models.RectangleComponent;
import org.concord.mw2d.models.RectangularBoundary;
import org.concord.mw2d.models.RectangularObstacle;
import org.concord.mw2d.models.TextBoxComponent;
import org.concord.mw2d.models.TriangleComponent;
import org.concord.mw2d.models.UserField;
import org.concord.mw2d.ui.GayBerneConfigure;

import static org.concord.mw2d.UserAction.*;

public class MesoView extends MDView {

	MesoModel model;
	GayBerneParticle[] gb;

	private int rotationHandle = -1, resizeHandle = -1;
	private float dipoleIncrement = 5.0f;
	private boolean showAngularMomenta, showLinearMomenta, drawDipole = true;
	private boolean readyToAdjustOmegaVector;
	private Ellipse2D.Float addGBIndicator = new Ellipse2D.Float();
	private GayBerneParticle[] gbBufferArray;
	private AffineTransform at, savedAT;
	private int nParticle;
	private GbPopupMenu gbPopupMenu;
	private DefaultMesoPopupMenu defaultPopupMenu;

	public MesoView() {
		super();
		augmentMaps();
		at = new AffineTransform();
	}

	public void setModel(MDModel mod) {
		if (mod == null) {
			model = null;
			return;
		}
		if (!(mod instanceof MesoModel))
			throw new IllegalArgumentException("Input model error");
		model = (MesoModel) mod;
		super.setModel(mod);
		gb = model.getParticles();
		initEditFieldActions();
		if (!layerBasket.isEmpty()) {
			for (Layered l : layerBasket) {
				((ModelComponent) l).setModel(model);
			}
		}
		defaultPopupMenu = new DefaultMesoPopupMenu(this);
	}

	public MDModel getModel() {
		return model;
	}

	public void destroy() {
		super.destroy();
		layerBasket.clear();
		if (gbBufferArray != null) {
			for (GayBerneParticle p : gbBufferArray) {
				p.setModel(null);
				p = null;
			}
		}
		setModel(null);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				destroyPopupMenu(defaultPopupMenu);
				destroyPopupMenu(gbPopupMenu);
				defaultPopupMenu = null;
				gbPopupMenu = null;
			}
		});
	}

	public void clear() {
		super.clear();
		showLinearMomenta = false;
		showAngularMomenta = false;
	}

	void refreshForces() {

	}

	JPopupMenu[] getPopupMenus() {
		return new JPopupMenu[] { defaultPopupMenu, gbPopupMenu, popupMenuForLayeredComponent };
	}

	/**
	 * which GB particle is at this position?
	 * 
	 * @param x
	 *            x coordinate of the position
	 * @param y
	 *            y coordinate of the position
	 * @return the selected particle (null if none is selected)
	 */
	public GayBerneParticle whichParticle(int x, int y) {
		Rectangle2D r2d = new Rectangle2D.Double();
		for (int i = 0; i < nParticle; i++) {
			at.setToRotation(gb[i].getTheta(), gb[i].getRx(), gb[i].getRy());
			r2d.setRect(gb[i].getRx() - gb[i].getLength() * 0.5 - 3.0, gb[i].getRy() - gb[i].getBreadth() * 0.5 - 3.0,
					gb[i].getLength() + 6.0, gb[i].getBreadth() + 6.0);
			if (at.createTransformedShape(r2d).contains(x, y))
				return gb[i];
		}
		return null;
	}

	/* remove any particles in the selected area */
	void removeSelectedArea() {

		super.removeSelectedArea();

		List<Layered> lay = removeSelectedLayeredComponents();
		List<Integer> list = null;
		for (int k = 0; k < model.getNumberOfParticles(); k++) {
			if (selectedArea.contains(gb[k].getRx(), gb[k].getRy())) {
				if (list == null)
					list = new ArrayList<Integer>();
				list.add(k);
			}
		}

		boolean particleRemoved = list != null && !list.isEmpty();
		boolean layerRemoved = lay != null && !lay.isEmpty();

		if (particleRemoved)
			removeMarkedParticles(list);

		if (particleRemoved || layerRemoved) {
			model.notifyChange();
			if (!doNotFireUndoEvent) {
				Layered[] i2 = null;
				if (layerRemoved && lay != null) {
					int n = lay.size();
					if (n > 0) {
						i2 = new Layered[n];
						for (int j = 0; j < n; j++)
							i2[j] = lay.get(j);
					}
				}
				model.getUndoManager().undoableEditHappened(
						new UndoableEditEvent(model, new UndoableDeletion(UndoAction.BLOCK_REMOVE, list != null ? list
								.size() : 0, i2)));
				updateUndoUIComponents();
			}
		}

		selectedArea.setSize(0, 0);
		repaint();

	}

	public void removeMarkedParticles(List<Integer> list) {
		if (selectedComponent != null) {
			selectedComponent.setSelected(false);
			selectedComponent = null;
		}
		if (gbBufferArray == null)
			gbBufferArray = new GayBerneParticle[MesoModel.getMaximumNumberOfParticles()];
		int temp = 0, temq = 0, ii = 0;
		for (int i = 0; i < nParticle; i++) {
			if (!list.contains(i)) {
				int nMeasure = gb[i].getNumberOfMeasurements();
				if (nMeasure > 0) {
					Object measure;
					for (int nm = 0; nm < nMeasure; nm++) {
						measure = gb[i].getMeasurement(nm);
						if (measure instanceof Integer && list.contains(measure)) {
							gb[i].removeMeasurement(nm);
						}
					}
				}
				if (gbBufferArray[temp] == null) {
					gbBufferArray[temp] = new GayBerneParticle();
					gbBufferArray[temp].setModel(model);
					gbBufferArray[temp].setIndex(temp);
				}
				gbBufferArray[temp].set(gb[i]);
				gbBufferArray[temp].setSelected(gb[i].isSelected());
				gbBufferArray[temp].setUserField(gb[i].getUserField());
				List<Layered> l = getLayeredComponentHostedBy(gb[i]);
				if (l != null) {
					for (Layered c : l)
						c.setHost(gbBufferArray[temp]);
				}
				// map the old indices of the surviving particles to the new ones
				// liveParticleMap.put(i, new Integer(temp));
				temp++;
			}
			else {
				if (!layerBasket.isEmpty()) {
					List<Layered> l = getLayeredComponentHostedBy(gb[i]);
					if (l != null) {
						for (Layered c : l) {
							c.setHost(null);
							layerBasket.remove(c);
						}
					}
				}
				ii = gb.length - 1 - temq;
				if (gbBufferArray[ii] == null) {
					gbBufferArray[ii] = new GayBerneParticle();
					gbBufferArray[ii].setModel(model);
					gbBufferArray[ii].setIndex(ii);
				}
				gbBufferArray[ii].set(gb[i]);
				gbBufferArray[ii].setUserField(gb[i].getUserField());
				gb[i].eraseProperties();
				temq++;
			}
		}
		for (int i = temp; i < nParticle; i++)
			gb[i].erase();
		nParticle = temp;
		for (int i = 0; i < nParticle; i++) {
			gb[i].set(gbBufferArray[i]);
			gb[i].setSelected(gbBufferArray[i].isSelected());
			gb[i].setUserField(gbBufferArray[i].getUserField());
			if (!layerBasket.isEmpty()) {
				List<Layered> l = getLayeredComponentHostedBy(gbBufferArray[i]);
				if (l != null) {
					for (Layered c : l)
						c.setHost(gb[i]);
				}
			}
		}
		model.setNumberOfParticles(nParticle);
	}

	private boolean undoRemoveMarkedParticles(int nRemoved) {
		int oldNOP = nParticle;
		nParticle += nRemoved;
		int incr = 0;
		for (int i = oldNOP; i < nParticle; i++) {
			gb[i].set(gbBufferArray[gb.length - 1 - incr]);
			gb[i].setUserField(gbBufferArray[gb.length - 1 - incr].getUserField());
			incr++;
		}
		if (incr == 0)
			return false;
		if (!model.getRecorderDisabled()) {
			int m = model.getMovie().getCapacity();
			for (int i = oldNOP; i < nParticle; i++)
				gb[i].initializeMovieQ(m);
		}
		model.setNumberOfParticles(nParticle);
		return true;
	}

	/**
	 * insert a Gay-Berne particle at a given position.
	 * 
	 * @param x
	 *            x coordinate of the position
	 * @param y
	 *            y coordinate of the position
	 * @param p
	 *            the type of particle to be inserted. If null, then use the parameters specified by
	 *            <code>GayBerneConfigure</code>
	 * @return 'true' if the action is done, 'false' if not
	 */
	public boolean insertAnGB(int x, int y, GayBerneParticle p) {
		if (nParticle >= gb.length)
			return false;
		if (p == null) {
			if (GayBerneConfigure.length >= GayBerneConfigure.breadth) {
				gb[nParticle].setLength(GayBerneConfigure.length);
				gb[nParticle].setBreadth(GayBerneConfigure.breadth);
				gb[nParticle].setTheta(GayBerneConfigure.theta);
			}
			else {
				gb[nParticle].setLength(GayBerneConfigure.breadth);
				gb[nParticle].setBreadth(GayBerneConfigure.length);
				gb[nParticle].setTheta(Math.PI * 0.5 + GayBerneConfigure.theta);
			}
		}
		else {
			gb[nParticle].setLength(p.getLength());
			gb[nParticle].setBreadth(p.getBreadth());
			gb[nParticle].setTheta(p.getTheta());
		}
		gb[nParticle].setRx(x);
		gb[nParticle].setRy(y);
		if (finalizeGBInsertion()) {
			if (gb[nParticle].getLength() - gb[nParticle].getBreadth() < GayBerneConfigure.GB_LIMIT) {
				gb[nParticle].setLength(gb[nParticle].getBreadth() + GayBerneConfigure.GB_LIMIT);
			}
			gb[nParticle].setVx(0.0);
			gb[nParticle].setVy(0.0);
			gb[nParticle].setOmega(0.0);
			gb[nParticle].setRestraint(null);
			if (!model.getRecorderDisabled())
				gb[nParticle].initializeMovieQ(model.getMovie().getCapacity());
			if (p == null) {
				gb[nParticle].setColor(GayBerneConfigure.color);
				gb[nParticle].setEpsilon0(GayBerneConfigure.epsilon0);
				gb[nParticle].setEeVsEs(GayBerneConfigure.eeVsEs);
				gb[nParticle].setCharge(0.0);
				gb[nParticle].setDipoleMoment(0.0);
				gb[nParticle].setInertia(0.5 * gb[nParticle].getMass() * gb[nParticle].getLength()
						* gb[nParticle].getBreadth());
			}
			else {
				gb[nParticle].setColor(p.getColor());
				gb[nParticle].setEpsilon0(p.getEpsilon0());
				gb[nParticle].setEeVsEs(p.getEeVsEs());
				gb[nParticle].setCharge(p.getCharge());
				gb[nParticle].setDipoleMoment(p.getDipoleMoment());
				gb[nParticle].setInertia(p.getInertia());
			}
			nParticle++;
			model.notifyChange();
			model.setNumberOfParticles(nParticle);
			if (!doNotFireUndoEvent) {
				gb[nParticle - 1].setSelected(true);
				model.getUndoManager().undoableEditHappened(
						new UndoableEditEvent(model, new UndoableInsertion(UndoAction.INSERT_A_PARTICLE, x, y)));
				updateUndoUIComponents();
			}
			repaint();
			return true;
		}
		return false;
	}

	public void setAction(short id) {
		super.setAction(id);
		resetAddObjectIndicator();
		if (actionID == ROTA_ID) {
			if (selectedComponent instanceof GayBerneParticle) {
				((GayBerneParticle) selectedComponent).setSelectedToRotate(true);
			}
		}
		else {
			if (selectedComponent instanceof GayBerneParticle) {
				((GayBerneParticle) selectedComponent).setSelectedToRotate(false);
			}
		}
		if (actionID == RESI_ID) {
			if (selectedComponent instanceof GayBerneParticle) {
				((GayBerneParticle) selectedComponent).setSelectedToResize(true);
			}
		}
		else {
			if (selectedComponent instanceof GayBerneParticle) {
				((GayBerneParticle) selectedComponent).setSelectedToResize(false);
			}
		}
		if (actionID == DELE_ID) {
			if (selectedComponent != null) {
				selectedComponent.setSelected(false);
				selectedComponent = null;
			}
		}
		if (actionID != OMEG_ID)
			selectOmega(null);
		if (actionID != VELO_ID)
			selectVelocity(null);
		repaint();
	}

	public void resetAddObjectIndicator() {
		if (actionID == ADGB_ID) {
			addGBIndicator.setFrame(-1, -1, GayBerneConfigure.length, GayBerneConfigure.breadth);
		}
	}

	public void setDrawDipole(boolean b) {
		drawDipole = b;
		model.notifyChange();
		repaint();
	}

	public boolean getDrawDipole() {
		return drawDipole;
	}

	public void showAngularMomenta(boolean b) {
		showAngularMomenta = b;
		model.notifyChange();
		repaint();
	}

	public boolean angularMomentaShown() {
		return showAngularMomenta;
	}

	public void showLinearMomenta(boolean b) {
		showLinearMomenta = b;
		model.notifyChange();
		repaint();
	}

	public boolean linearMomentaShown() {
		return showLinearMomenta;
	}

	void prepareToUndoPositioning() {
		if (selectedComponent != null) {
			if (!doNotFireUndoEvent) {
				UndoableMoving a = new UndoableMoving(selectedComponent);
				a.setPresentationName("Translation");
				model.getUndoManager().undoableEditHappened(new UndoableEditEvent(model, a));
				updateUndoUIComponents();
			}
			selectedComponent.setSelected(false);
		}
	}

	public void clearSelection() {
		super.clearSelection();
		for (int i = 0; i < nParticle; i++) {
			gb[i].setSelected(false);
			gb[i].setSelectedToRotate(false);
		}
	}

	protected void copySelectedComponent() {
		super.copySelectedComponent();
		if (selectedComponent instanceof GayBerneParticle) {
			pasteBuffer = ((GayBerneParticle) selectedComponent).clone();
		}
		Action a = getActionMap().get(PASTE);
		if (!a.isEnabled() && pasteBuffer != null)
			a.setEnabled(true);
	}

	public void removeSelectedComponent() {

		if (selectedComponent == null)
			return;
		super.removeSelectedComponent();

		if (selectedComponent instanceof GayBerneParticle) {
			pasteBuffer = ((GayBerneParticle) selectedComponent).clone();
			List<Integer> list = new ArrayList<Integer>();
			list.add(((GayBerneParticle) selectedComponent).getIndex());
			removeMarkedParticles(list);
			model.notifyChange();
			if (!doNotFireUndoEvent) {
				model.getUndoManager().undoableEditHappened(
						new UndoableEditEvent(model, new UndoableDeletion(UndoAction.BLOCK_REMOVE, 1)));
				updateUndoUIComponents();
			}
		}

		if (selectedComponent != null) {
			selectedComponent.setSelected(false);
			selectedComponent = null;
		}
		repaint();

	}

	protected void pasteBufferedComponent(int x, int y) {
		super.pasteBufferedComponent(x, y);
		if (pasteBuffer instanceof GayBerneParticle) {
			insertAnGB(x, y, (GayBerneParticle) pasteBuffer);
		}
	}

	private void augmentMaps() {

		Action a = new AbstractAction() {
			public Object getValue(String key) {
				if (key.equalsIgnoreCase("state"))
					return linearMomentaShown() ? Boolean.TRUE : Boolean.FALSE;
				return super.getValue(key);
			}

			public void actionPerformed(ActionEvent e) {
				if (ModelerUtilities.stopFiring(e))
					return;
				Object o = e.getSource();
				if (o instanceof JToggleButton)
					showLinearMomenta(((JToggleButton) o).isSelected());
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.NAME, "Momentum Vector of Center of Mass");
		a.putValue(Action.SHORT_DESCRIPTION, "Momentum Vector of Center of Mass");
		String o = (String) a.getValue(Action.SHORT_DESCRIPTION);
		getActionMap().put(o, a);
		booleanSwitches.put(o, a);

		a = new AbstractAction() {
			public Object getValue(String key) {
				if (key.equalsIgnoreCase("state"))
					return angularMomentaShown() ? Boolean.TRUE : Boolean.FALSE;
				return super.getValue(key);
			}

			public void actionPerformed(ActionEvent e) {
				if (ModelerUtilities.stopFiring(e))
					return;
				Object o = e.getSource();
				if (o instanceof JToggleButton)
					showAngularMomenta(((JToggleButton) o).isSelected());
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.NAME, "Angular Momentum");
		a.putValue(Action.SHORT_DESCRIPTION, "Angular Momentum");
		o = (String) a.getValue(Action.SHORT_DESCRIPTION);
		getActionMap().put(o, a);
		booleanSwitches.put(o, a);

		a = new AbstractAction() {
			public Object getValue(String key) {
				if (key.equalsIgnoreCase("state"))
					return getDrawDipole() ? Boolean.TRUE : Boolean.FALSE;
				return super.getValue(key);
			}

			public void actionPerformed(ActionEvent e) {
				if (ModelerUtilities.stopFiring(e))
					return;
				Object o = e.getSource();
				if (o instanceof JToggleButton)
					setDrawDipole(((JToggleButton) o).isSelected());
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.NAME, "Show Dipole Moment");
		a.putValue(Action.SHORT_DESCRIPTION, "Show Dipole Moment");
		o = (String) a.getValue(Action.SHORT_DESCRIPTION);
		getActionMap().put(o, a);
		booleanSwitches.put(o, a);

	}

	private GayBerneParticle whichParticleOtherThan(int x, int y, GayBerneParticle p) {
		Rectangle2D r2d = new Rectangle2D.Double();
		for (int i = 0; i < nParticle; i++) {
			if (gb[i] != p) {
				at.setToRotation(gb[i].getTheta(), gb[i].getRx(), gb[i].getRy());
				r2d.setRect(gb[i].getRx() - gb[i].getLength() * 0.5 - 3.0, gb[i].getRy() - gb[i].getBreadth() * 0.5
						- 3.0, gb[i].getLength() + 6.0, gb[i].getBreadth() + 6.0);
				if (at.createTransformedShape(r2d).contains(x, y))
					return gb[i];
			}
		}
		return null;
	}

	private void renderParticle(GayBerneParticle p, Graphics2D g) {
		p.render(g, at, savedAT);
		if (showAngularMomenta)
			p.drawOmega(g, getBackground());
		if (showLinearMomenta)
			p.drawVelocityVector(g);
		if (drawExternalForce)
			p.drawExternalForceVector(g);
		if (p.velocitySelected())
			p.drawSelectedVelocityVector(g, getBackground(), readyToAdjustVelocityVector);
		if (p.getOmegaSelection())
			p.drawSelectedOmegaVector(g, getBackground(), readyToAdjustOmegaVector);
	}

	void selectVelocity(GayBerneParticle p) {
		for (int i = 0; i < nParticle; i++)
			gb[i].setVelocitySelection(false);
		if (p != null)
			p.setVelocitySelection(true);
	}

	void selectOmega(GayBerneParticle p) {
		for (int i = 0; i < nParticle; i++)
			gb[i].setOmegaSelection(false);
		if (p != null)
			p.setOmegaSelection(true);
	}

	private boolean finalizeSize(GayBerneParticle p) {
		if (overlap(p) || p.intersects(boundary)) {
			p.restoreState();
			return false;
		}
		return true;
	}

	private boolean finalizeLocation(GayBerneParticle p) {
		if (overlap(p) || p.intersects(boundary)) {
			p.restoreState();
			return false;
		}
		return true;
	}

	private boolean finalizeOrientation(GayBerneParticle p) {
		if (overlap(p) || p.intersects(boundary)) {
			p.restoreState();
			return false;
		}
		return true;
	}

	private boolean finalizeDuplication() {
		if (selectedComponent instanceof GayBerneParticle) {
			if (!finalizeLocation((GayBerneParticle) selectedComponent)) {
				nParticle--;
				repaint();
				setSelectedComponent(null);
				return false;
			}
			if (!model.getRecorderDisabled())
				gb[nParticle - 1].initializeMovieQ(model.getMovie().getCapacity());
			return true;
		}
		return false;
	}

	private boolean finalizeGBInsertion() {
		if (gb[nParticle].intersects(boundary)) {
			errorReminder.show(ErrorReminder.OUT_OF_BOUND);
			return false;
		}
		if (overlap(gb[nParticle])) {
			errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
			return false;
		}
		return true;
	}

	private boolean overlap(GayBerneParticle p) {
		if (p == null)
			throw new IllegalArgumentException("p is null");
		for (int i = 0; i < nParticle; i++) {
			if (gb[i] != p) {
				if (gb[i].intersects(p))
					return true;
			}
		}
		return false;
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
		GayBerneParticle p = whichParticle(x, y);
		if (p != null) {
			p.setSelected(true);
			clickPoint.setLocation(x - p.getRx(), y - p.getRy());
			return p;
		}
		if (lc != null && ((Layered) lc).getLayer() == Layered.BEHIND_PARTICLES) {
			lc.setSelected(true);
			clickPoint.setLocation(x - lc.getRx(), y - lc.getRy());
			return selectedComponent;
		}
		return null;
	}

	private ModelComponent getRolloverComponent(int x, int y) {
		ModelComponent lc = whichLayeredComponent(x, y);
		if (lc != null && ((Layered) lc).getLayer() == Layered.IN_FRONT_OF_PARTICLES)
			return lc;
		GayBerneParticle p = whichParticle(x, y);
		if (p != null)
			return p;
		if (lc != null && ((Layered) lc).getLayer() == Layered.BEHIND_PARTICLES)
			return lc;
		return null;
	}

	void update2(Graphics g) {

		if (repaintBlocked) {
			paintPleaseWait(g);
			return;
		}

		super.update2(g);

		Graphics2D g2 = (Graphics2D) g;

		savedAT = g2.getTransform();
		if (selectedComponent instanceof GayBerneParticle) {
			int k = ((GayBerneParticle) selectedComponent).getIndex();
			for (int i = 0; i < k; i++)
				renderParticle(gb[i], g2);
			for (int i = k + 1; i < nParticle; i++)
				renderParticle(gb[i], g2);
			renderParticle(gb[k], g2);
		}
		else {
			for (int i = 0; i < nParticle; i++)
				renderParticle(gb[i], g2);
		}

		for (int i = 0; i < nParticle; i++) {
			gb[i].renderMeasurements(g2);
			gb[i].renderMeanPosition(g2);
			gb[i].renderMeanForce(g2);
		}

		if (showAngularMomenta) {
			g2.setStroke(ViewAttribute.THIN);
			g2.setColor(Color.green);
			g2.fillRoundRect(10, 10, 20, 10, 5, 5);
			g2.setColor(Color.red);
			g2.fillRoundRect(10, 30, 20, 10, 5, 5);
			g2.setColor(contrastBackground());
			g2.drawRoundRect(10, 10, 20, 10, 5, 5);
			g2.drawRoundRect(10, 30, 20, 10, 5, 5);
			g2.setFont(ViewAttribute.SMALL_FONT);
			g2.drawString("Anti-Clockwise", 35, 20);
			g2.drawString("Clockwise", 35, 40);
		}

		paintSteering(g2);

		if (actionID == DELE_ID || actionID == MARK_ID || actionID == COUN_ID) {
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
			if (selectedComponent instanceof GayBerneParticle) {
				GayBerneParticle p = (GayBerneParticle) selectedComponent;
				String str = p.getCharge() + "";
				int sw = g2.getFontMetrics().stringWidth(str) + 8;
				int sh = g2.getFontMetrics().getHeight() + 6;
				g2.setStroke(ViewAttribute.THIN);
				g2.setColor(SystemColor.info);
				g2.fillRect((int) p.getRx(), (int) p.getRy() - sh, sw, sh);
				g2.setColor(contrastBackground());
				g2.drawRect((int) p.getRx(), (int) p.getRy() - sh, sw, sh);
				g2.setColor(Color.black);
				g2.drawString(str, (int) p.getRx() + 4, (int) p.getRy() - sh / 2 + 4);
			}
		}
		else if (actionID == IPOL_ID || actionID == DPOL_ID) {
			if (selectedComponent instanceof GayBerneParticle) {
				GayBerneParticle p = (GayBerneParticle) selectedComponent;
				String str = p.getDipoleMoment() + "";
				int sw = g2.getFontMetrics().stringWidth(str) + 8;
				int sh = g2.getFontMetrics().getHeight() + 6;
				g2.setStroke(ViewAttribute.THIN);
				g2.setColor(SystemColor.info);
				g2.fillRect((int) p.getRx(), (int) p.getRy() - sh, sw, sh);
				g2.setColor(contrastBackground());
				g2.drawRect((int) p.getRx(), (int) p.getRy() - sh, sw, sh);
				g.setColor(Color.black);
				g.drawString(str, (int) p.getRx() + 4, (int) p.getRy() - sh / 2 + 4);
			}
		}
		else if (actionID == IRES_ID || actionID == DRES_ID) {
			if (selectedComponent instanceof GayBerneParticle) {
				GayBerneParticle p = (GayBerneParticle) selectedComponent;
				if (p.getRestraint() != null) {
					g.setColor(contrastBackground());
					g.drawString(p.getRestraint().toString(), (int) p.getRx(), (int) p.getRy());
				}
			}
		}
		else if (actionID == IDMP_ID || actionID == DDMP_ID) {
			if (selectedComponent instanceof GayBerneParticle) {
				GayBerneParticle p = (GayBerneParticle) selectedComponent;
				g.setColor(contrastBackground());
				g.drawString(p.getFriction() + "", (int) p.getRx(), (int) p.getRy());
			}
		}
		else if (actionID == ADGB_ID) {
			if (addGBIndicator.x >= 0 && addGBIndicator.y >= 0
					&& addGBIndicator.x + addGBIndicator.width <= boundary.getWidth()
					&& addGBIndicator.y + addGBIndicator.height <= boundary.getHeight()) {
				AffineTransform savedAT = g2.getTransform();
				AffineTransform at = new AffineTransform();
				at.setToRotation(GayBerneConfigure.theta, addGBIndicator.x + 0.5f * addGBIndicator.width,
						addGBIndicator.y + 0.5f * addGBIndicator.height);
				g2.transform(at);
				g2.setStroke(ViewAttribute.THIN_DASHED);
				g2.setColor(contrastBackground());
				g2.draw(addGBIndicator);
				g2.setTransform(savedAT);
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

	private void showGbPopupMenu(GayBerneParticle p, int x, int y) {
		if (gbPopupMenu == null)
			gbPopupMenu = new GbPopupMenu(this);
		gbPopupMenu.setTrajSelected(p.getShowRTraj());
		gbPopupMenu.setRMeanSelected(p.getShowRMean());
		gbPopupMenu.setFMeanSelected(p.getShowFMean());
		gbPopupMenu.setCoor(x, y);
		gbPopupMenu.show(this, x, y);
	}

	void processMousePressed(MouseEvent e) {

		super.processMousePressed(e);

		if (energizerButtonPressed)
			return;
		if (model.getJob() != null && !model.getJob().isStopped())
			return;

		int clickCount = e.getClickCount();
		if (clickCount >= 2) {
			if (actionID != HEAT_ID && actionID != COOL_ID && actionID != IRES_ID && actionID != DRES_ID
					&& actionID != IDMP_ID && actionID != DDMP_ID && actionID != IPOL_ID && actionID != DPOL_ID
					&& actionID != CPOS_ID && actionID != CNEG_ID) {
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

		if (ModelerUtilities.isRightClick(e)) {
			if (!popupMenuEnabled)
				return;
			if (e.isShiftDown()) {
				GayBerneParticle p = whichParticle(x, y);
				if (p != null) {
					if (selectedComponent != null && selectedComponent != p)
						selectedComponent.setSelected(false);
					p.setSelected(true);
					repaint();
					showGbPopupMenu(p, x, y);
					return;
				}
				if (selectedComponent != null) {
					selectedComponent.setSelected(false);
					selectedComponent = null;
				}
				defaultPopupMenu.setCoor(x, y);
				defaultPopupMenu.show(this, x, y);
				return;
			}
			if (openLayeredComponentPopupMenus(x, y, Layered.IN_FRONT_OF_PARTICLES))
				return;
			if (getSelectedComponent(e.getModifiersEx(), x, y) instanceof GayBerneParticle) {
				GayBerneParticle p = (GayBerneParticle) selectedComponent;
				p.setSelected(true);
				p.storeCurrentState();
				repaint();
				showGbPopupMenu(p, x, y);
				return;
			}
			if (openLayeredComponentPopupMenus(x, y, Layered.BEHIND_PARTICLES))
				return;
			defaultPopupMenu.setCoor(x, y);
			defaultPopupMenu.show(this, x, y);
			return;
		}

		switch (actionID) {

		case WHAT_ID:
			ModelComponent mc = getSelectedComponent(e.getModifiersEx(), x, y);
			String str = null;
			if (mc instanceof GayBerneParticle) {
				((GayBerneParticle) mc).setSelectedToResize(false);
				((GayBerneParticle) mc).setSelectedToRotate(false);
				str = mc.toString();
			}
			else if (mc instanceof ImageComponent) {
				str = "Image #" + getLayeredComponentIndex((ImageComponent) mc) + ":"
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
			break;

		case SELE_ID:
			if (selectedComponent != null) {
				selectedComponent.setSelected(false);
				selectedComponent = null;
			}
			mc = getSelectedComponent(e.getModifiersEx(), x, y);
			if (mc != null) {
				mc.storeCurrentState();
				if (mc instanceof GayBerneParticle) {
					((GayBerneParticle) mc).setSelectedToRotate(false);
					((GayBerneParticle) mc).setSelectedToResize(false);
				}
				if (clickCount >= 2)
					DialogFactory.showDialog(mc);
			}
			repaint();
			break;

		case RESI_ID:
			if (selectedComponent instanceof GayBerneParticle) {
				resizeHandle = ((GayBerneParticle) selectedComponent).getResizeHandle(x, y);
			}
			if (resizeHandle == -1) {
				mc = getSelectedComponent(e.getModifiersEx(), x, y);
				if (mc instanceof GayBerneParticle) {
					GayBerneParticle p = (GayBerneParticle) mc;
					p.setSelectedToResize(true);
					p.storeCurrentState();
				}
			}
			else {
				if (selectedComponent instanceof GayBerneParticle) {
					((GayBerneParticle) selectedComponent).storeCurrentState();
				}
			}
			break;

		case ROTA_ID:
			if (selectedComponent instanceof GayBerneParticle) {
				rotationHandle = ((GayBerneParticle) selectedComponent).getRotationHandle(x, y);
			}
			if (rotationHandle == -1) {
				mc = getSelectedComponent(e.getModifiersEx(), x, y);
				if (mc instanceof GayBerneParticle) {
					GayBerneParticle p = (GayBerneParticle) mc;
					p.setSelectedToRotate(true);
					p.storeCurrentState();
				}
			}
			else {
				if (selectedComponent instanceof GayBerneParticle) {
					((GayBerneParticle) selectedComponent).storeCurrentState();
				}
			}
			break;

		case HEAT_ID:
		case COOL_ID:
			pointHeater.setLocation(x, y);
			pointHeater.equiPartitionEnergy(model);
			repaint();
			break;

		case ADGB_ID:
			addGBIndicator.x = addGBIndicator.y = -1;
			if (clickCount == 1 && nParticle < gb.length - 1)
				insertAnGB(x, y, null);
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
			if (clickCount == 1)
				selectedArea.setLocation(x, y);
			break;

		case TRIA_ID:
			showActionTip("Drag the mouse to draw a triangle", x + 10, y + 10);
			if (clickCount == 1)
				selectedArea.setLocation(x, y);
			break;

		case ELLI_ID:
			showActionTip("Drag the mouse to draw an ellipse", x + 10, y + 10);
			if (clickCount == 1)
				selectedArea.setLocation(x, y);
			break;

		case DELE_ID:
			if (clickCount == 1)
				selectedArea.setLocation(x, y);
			break;

		case CPOS_ID:
			GayBerneParticle p = whichParticle(x, y);
			if (p != null) {
				p.setSelected(true);
				if (p.addCharge(e.isShiftDown() ? -chargeIncrement : chargeIncrement)) {
					repaint();
					model.notifyChange();
				}
			}
			break;

		case CNEG_ID:
			p = whichParticle(x, y);
			if (p != null) {
				p.setSelected(true);
				if (p.addCharge(e.isShiftDown() ? chargeIncrement : -chargeIncrement)) {
					repaint();
					model.notifyChange();
				}
			}
			break;

		case IPOL_ID:
			p = whichParticle(x, y);
			if (p != null) {
				p.setSelected(true);
				if (p.addDipoleMoment(e.isShiftDown() ? -dipoleIncrement : dipoleIncrement)) {
					repaint();
					model.notifyChange();
				}
			}
			break;

		case DPOL_ID:
			p = whichParticle(x, y);
			if (p != null) {
				p.setSelected(true);
				if (p.addDipoleMoment(e.isShiftDown() ? dipoleIncrement : -dipoleIncrement)) {
					repaint();
					model.notifyChange();
				}
			}
			break;

		case IRES_ID:
			p = whichParticle(x, y);
			if (p != null) {
				p.setSelected(true);
				if (p.getRestraint() == null) {
					p.setRestraint(new PointRestraint(10, p.getRx(), p.getRy()));
				}
				else {
					p.getRestraint().changeK(10);
				}
				repaint();
				model.notifyChange();
			}
			break;

		case DRES_ID:
			p = whichParticle(x, y);
			if (p != null && p.getRestraint() != null) {
				p.setSelected(true);
				if (p.getRestraint().getK() >= 10) {
					p.getRestraint().changeK(-10);
					if (p.getRestraint().getK() <= ZERO)
						p.setRestraint(null);
					repaint();
					model.notifyChange();
				}
			}
			break;

		case IDMP_ID:
			p = whichParticle(x, y);
			if (p != null) {
				p.setSelected(true);
				if (p.addFriction(0.5f)) {
					repaint();
					model.notifyChange();
				}
			}
			break;

		case DDMP_ID:
			p = whichParticle(x, y);
			if (p != null) {
				p.setSelected(true);
				if (p.addFriction(-0.5f)) {
					repaint();
					model.notifyChange();
				}
			}
			break;

		case VELO_ID:
			if (clickCount == 1) {
				if (!readyToAdjustVelocityVector) {
					p = (GayBerneParticle) getSelectedComponent(e.getModifiersEx(), x, y);
					if (p != null) {
						showActionTip("Drag the small red rectangle at the tip of the velocity vector", x + 10, y + 10);
						selectVelocity(p);
					}
				}
			}
			break;

		case OMEG_ID:
			if (clickCount == 1) {
				mc = getSelectedComponent(e.getModifiersEx(), x, y);
				if (mc instanceof GayBerneParticle) {
					p = (GayBerneParticle) mc;
					if (!readyToAdjustOmegaVector) {
						showActionTip(
								"<html>Drag the red rectangle to change the magnitude,<br>click the center to reverse the direction</html>",
								x + 10, y + 10);
						selectOmega(p);
					}
					if (p.getFHotSpot().contains(x, y)) {
						p.setOmega(-p.getOmega());
						repaint();
					}
				}
			}
			break;

		case MEAS_ID:
			if (clickCount == 1) {
				if (!readyToAdjustDistanceVector) {
					Object o = getSelectedComponent(e.getModifiersEx(), x, y);
					if (o instanceof GayBerneParticle) {
						p = (GayBerneParticle) o;
						p.setSelected(true);
						if (e.isShiftDown()) {
							p.addMeasurement(new Point(2 * p.getRx() < getWidth() ? (int) (p.getRx() + 20) : (int) (p
									.getRx() - 20), (int) p.getRy()));
						}
						else {
							showActionTip(
									p.getNumberOfMeasurements() <= 0 ? "Hold down SHIFT and click to add a measurement."
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

		case TRAJ_ID:
			mc = getSelectedComponent(e.getModifiersEx(), x, y);
			if (mc instanceof GayBerneParticle) {
				p = (GayBerneParticle) mc;
				p.setSelected(true);
				p.setShowRTraj(!p.getShowRTraj());
				if (p.getShowRTraj()) {
					showActionTip("Click the particle to hide its trajectory", x + 20, y + 20);
				}
				else {
					showActionTip("Click the particle to show its trajectory", x + 20, y + 20);
				}
				repaint();
			}
			break;

		case RAVE_ID:
			mc = getSelectedComponent(e.getModifiersEx(), x, y);
			if (mc instanceof GayBerneParticle) {
				p = (GayBerneParticle) mc;
				p.setSelected(true);
				p.setShowRMean(!p.getShowRMean());
				if (p.getShowRMean()) {
					showActionTip("Click the particle to hide its current average position", x + 20, y + 20);
				}
				else {
					showActionTip("Click the particle to show its current average position", x + 20, y + 20);
				}
				repaint();
			}
			break;

		case DUPL_ID:
			mc = getSelectedComponent(e.getModifiersEx(), x, y);
			if (mc instanceof GayBerneParticle) {
				if (nParticle < gb.length) {
					showActionTip("Drag the selected Gay-Berne particle to duplicate one", x + 10, y + 10);
					gb[nParticle].duplicate((GayBerneParticle) mc);
					gb[nParticle].setSelected(true);
					nParticle++;
				}
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
					ic.setLocation(x - 10, y - 10);
					addLayeredComponent(ic);
				}
			}
			else if (mc instanceof TextBoxComponent) {
				showActionTip("Drag the selected text box to duplicate one", x + 10, y + 10);
				TextBoxComponent tb = new TextBoxComponent((TextBoxComponent) mc);
				tb.setSelected(true);
				tb.setLocation(x - 10, y - 10);
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

		case HEAT_ID:
		case COOL_ID:
			pointHeater.setLocation(x, y);
			repaint();
			break;

		case ADGB_ID:
			addGBIndicator.x = x - addGBIndicator.width * 0.5f;
			addGBIndicator.y = y - addGBIndicator.height * 0.5f;
			repaint();
			break;

		case SELE_ID:
			ModelComponent mc = getRolloverComponent(x, y);
			setCursor(mc != null ? UserAction.getCursor(actionID) : Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			break;

		case ROTA_ID:
			if (selectedComponent instanceof GayBerneParticle) {
				if (((GayBerneParticle) selectedComponent).isSelectedToRotate()) {
					setCursor(((GayBerneParticle) selectedComponent).getRotationHandle(x, y) != -1 ? rotateCursor1
							: previousCursor);
				}
			}
			break;

		case RESI_ID:
			if (selectedComponent instanceof GayBerneParticle) {
				if (((GayBerneParticle) selectedComponent).isSelectedToResize()) {
					switch (((GayBerneParticle) selectedComponent).getResizeHandle(x, y)) {
					case 0:
						setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
						break;
					case 1:
						setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
						break;
					default:
						setCursor(previousCursor);
					}
				}
			}
			break;

		case VELO_ID:
			if (selectedComponent instanceof GayBerneParticle) {
				if (((GayBerneParticle) selectedComponent).velocitySelected()) {
					if (((GayBerneParticle) selectedComponent).isVelocityHandleSelected(x, y)) {
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

		case OMEG_ID:
			if (selectedComponent instanceof GayBerneParticle) {
				if (((GayBerneParticle) selectedComponent).getOmegaSelection()) {
					if (((GayBerneParticle) selectedComponent).getOHotSpot().contains(x, y)) {
						setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
						readyToAdjustOmegaVector = true;
					}
					else {
						setCursor(previousCursor);
						readyToAdjustOmegaVector = false;
					}
				}
			}
			break;

		case MEAS_ID:
			if (selectedComponent instanceof GayBerneParticle) {
				GayBerneParticle p = (GayBerneParticle) selectedComponent;
				indexOfSelectedMeasurement = p.getMeasurement(x, y);
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

		case HEAT_ID:
		case COOL_ID:
			pointHeater.setLocation(x, y);
			repaint();
			break;

		case ADGB_ID:
			addGBIndicator.x = addGBIndicator.y = -1;
			repaint();
			break;

		case RESI_ID:
			if (selectedComponent instanceof GayBerneParticle) {
				if (resizeHandle >= 0) {
					((GayBerneParticle) selectedComponent).resizeTo(x, y, resizeHandle);
				}
			}
			break;

		case ROTA_ID:
			if (selectedComponent instanceof GayBerneParticle) {
				if (rotationHandle >= 0) {
					setCursor(rotateCursor3);
					((GayBerneParticle) selectedComponent).rotateTo(x, y, rotationHandle);
				}
			}
			break;

		case SELE_ID:
			if (selectedComponent != null) {
				if (!isEditable() && !selectedComponent.isDraggable()) {
					showActionTip("<html><font color=red>The selected object is not draggable!</font></html>", x, y);
				}
				else {
					dragSelected = false;
					if (selectedComponent instanceof GayBerneParticle) {
						GayBerneParticle p = (GayBerneParticle) selectedComponent;
						if (p.getRestraint() != null) {
							int amp = (int) (400.0 / p.getRestraint().getK());
							Vector2D loc = moveSpring(x, y, (int) p.getRestraint().getX0(), (int) p.getRestraint()
									.getY0(), 0, amp);
							if (loc == null)
								return;
							p.setRx(loc.getX());
							p.setRy(loc.getY());
						}
						else {
							p.setRx(x - clickPoint.x);
							p.setRy(y - clickPoint.y);
						}
						boundary.setRBC(p, RectangularBoundary.TRANSLATION);
						dragSelected = true;
					}
					else if (selectedComponent instanceof ImageComponent) {
						ImageComponent ic = (ImageComponent) selectedComponent;
						ic.translateTo(x - clickPoint.x, y - clickPoint.y);
						dragSelected = true;
						moveHostTo(ic.getHost(), ic.getRx() + ic.getWidth() * 0.5, ic.getRy() + ic.getHeight() * 0.5);
					}
					else if (selectedComponent instanceof TextBoxComponent) {
						TextBoxComponent tb = (TextBoxComponent) selectedComponent;
						tb.translateTo(x - clickPoint.x, y - clickPoint.y);
						dragSelected = true;
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

		case COUN_ID:
		case MARK_ID:
		case DELE_ID:
		case RECT_ID:
		case TRIA_ID:
		case ELLI_ID:
			dragRect(x, y);
			break;

		case VELO_ID:
			if (readyToAdjustVelocityVector) {
				if (selectedComponent instanceof GayBerneParticle) {
					((GayBerneParticle) selectedComponent).setVelocityHandleLocation(x, y);
					repaint();
				}
			}
			break;

		case OMEG_ID:
			if (readyToAdjustOmegaVector) {
				if (selectedComponent instanceof GayBerneParticle) {
					((GayBerneParticle) selectedComponent).getOHotSpot().setRect(x - 3, y - 3, 6, 6);
				}
				repaint();
			}
			break;

		case MEAS_ID:
			if (readyToAdjustDistanceVector && indexOfSelectedMeasurement >= 0) {
				if (selectedComponent instanceof GayBerneParticle) {
					GayBerneParticle p = (GayBerneParticle) selectedComponent;
					GayBerneParticle p1 = whichParticleOtherThan(x, y, p);
					if (p1 != null) {
						p.setMeasurement(indexOfSelectedMeasurement, new Integer(p1.getIndex()));
					}
					else {
						p.setMeasurement(indexOfSelectedMeasurement, new Point(x, y));
					}
					repaint();
				}
			}
			break;

		case DUPL_ID:
			if (selectedComponent instanceof GayBerneParticle) {
				((GayBerneParticle) selectedComponent).translateTo(x, y);
				boundary.setRBC((GayBerneParticle) selectedComponent, RectangularBoundary.TRANSLATION);
			}
			else if (selectedComponent instanceof Layered) {
				Layered l = (Layered) selectedComponent;
				l.setLocation(x - l.getWidth() * 0.5, y - l.getHeight() * 0.5);
			}
			repaint();
			break;

		}

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

		switch (actionID) {

		case HEAT_ID:
			pointHeater.doWork(model, true);
			break;

		case COOL_ID:
			pointHeater.doWork(model, false);
			break;

		case ADGB_ID:
			addGBIndicator.x = addGBIndicator.y = -1;
			repaint();
			break;

		case RESI_ID:
			if (selectedComponent instanceof GayBerneParticle) {
				if (resizeHandle >= 0) {
					resizeHandle = -1;
					if (finalizeSize((GayBerneParticle) selectedComponent)) {
						model.notifyChange();
					}
					if (!doNotFireUndoEvent) {
						model.getUndoManager().undoableEditHappened(
								new UndoableEditEvent(model, new UndoableResizing(selectedComponent)));
						updateUndoUIComponents();
					}
				}
			}
			break;

		case ROTA_ID:
			if (selectedComponent instanceof GayBerneParticle) {
				if (rotationHandle >= 0) {
					rotationHandle = -1;
					finalizeOrientation((GayBerneParticle) selectedComponent);
					model.notifyChange();
					if (selectedComponent != null && !doNotFireUndoEvent) {
						model.getUndoManager().undoableEditHappened(
								new UndoableEditEvent(model, new UndoableMoving(selectedComponent)));
						updateUndoUIComponents();
					}
				}
			}
			break;

		case DELE_ID:
			removeSelectedArea();
			break;

		case COUN_ID:
			int n = 0;
			for (int k = 0; k < nParticle; k++) {
				if (selectedArea.contains(gb[k].getRx(), gb[k].getRy()))
					n++;
			}
			if (n != 0) {
				String s = MDView.getInternationalText("CountingResult");
				JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(this),
						"The selected area contains the center of mass of " + n + " particles.", s != null ? s
								: "Counting result", JOptionPane.INFORMATION_MESSAGE);
			}
			selectedArea.setSize(0, 0);
			repaint();
			break;

		case MARK_ID:
			for (int k = 0; k < nParticle; k++)
				gb[k].setMarked(selectedArea.contains(gb[k].getRx(), gb[k].getRy()));
			selectedArea.setSize(0, 0);
			repaint();
			break;

		case SELE_ID:
			if (dragSelected) {
				boolean b = false;
				if (selectedComponent instanceof GayBerneParticle) {
					if (finalizeLocation((GayBerneParticle) selectedComponent)) {
						b = true;
					}
					else {
						errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
					}
				}
				else if (selectedComponent instanceof Layered) {
					b = true;
					ModelComponent host = ((Layered) selectedComponent).getHost();
					if (host instanceof GayBerneParticle) {
						b = finalizeLocation((GayBerneParticle) host);
						if (!b)
							errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
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

		case VELO_ID:
			if (readyToAdjustVelocityVector) {
				if (selectedComponent instanceof GayBerneParticle) {
					GayBerneParticle p = (GayBerneParticle) selectedComponent;
					p.storeCurrentState();
					p.setVx((x - p.getRx()) / velocityFlavor.getLength());
					p.setVy((y - p.getRy()) / velocityFlavor.getLength());
					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					repaint();
					model.notifyChange();
					if (!doNotFireUndoEvent) {
						model.getUndoManager().undoableEditHappened(
								new UndoableEditEvent(model, new UndoableResizing(selectedComponent)));
						updateUndoUIComponents();
					}
				}
				readyToAdjustVelocityVector = false;
			}
			break;

		case OMEG_ID:
			if (selectedComponent instanceof GayBerneParticle) {
				GayBerneParticle p = (GayBerneParticle) selectedComponent;
				p.storeCurrentState();
				if (readyToAdjustOmegaVector) {
					double angle = Math.acos((x - p.getRx()) / Math.hypot(x - p.getRx(), y - p.getRy()));
					if (p.getOmega() < 0.0) {
						if (y > p.getRy()) {
							angle = 2.0 * Math.PI - angle;
						}
					}
					else {
						if (y > p.getRy()) {
							angle = -angle;
						}
						else {
							angle -= 2.0 * Math.PI;
						}
					}
					angle *= 180.0 / Math.PI;
					p.setOmega((p.getOmega() < 0.0 ? -1.0 : 1.0)
							* Math.sqrt(Math.abs(angle) / (p.getInertia() * 20000)));
					readyToAdjustOmegaVector = false;
					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					model.notifyChange();
					if (!doNotFireUndoEvent) {
						model.getUndoManager().undoableEditHappened(
								new UndoableEditEvent(model, new UndoableResizing(selectedComponent)));
						updateUndoUIComponents();
					}
				}
			}
			break;

		case MEAS_ID:
			if (readyToAdjustDistanceVector) {
				if (selectedComponent instanceof GayBerneParticle && indexOfSelectedMeasurement >= 0) {
					if (x < 0 || x > getWidth() || y < 0 || y > getHeight()) {
						((GayBerneParticle) selectedComponent).removeMeasurement(indexOfSelectedMeasurement);
						indexOfSelectedMeasurement = -1;
						setCursor(previousCursor);
						repaint();
					}
				}
				readyToAdjustDistanceVector = false;
			}
			break;

		case DUPL_ID:
			if (selectedComponent instanceof GayBerneParticle) {
				if (finalizeDuplication()) {
					model.setNumberOfParticles(nParticle);
					model.notifyChange();
					if (!doNotFireUndoEvent) {
						model.getUndoManager()
								.undoableEditHappened(
										new UndoableEditEvent(model, new UndoableInsertion(
												UndoAction.INSERT_A_PARTICLE, x, y)));
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
			break;

		}

		repaint();
		e.consume();

	}

	void processMouseExited(MouseEvent e) {
		if (model.getJob() != null && !model.getJob().isStopped())
			return;
		if (actionID == ADGB_ID) {
			addGBIndicator.x = addGBIndicator.y = -1;
		}
		else if (actionID == HEAT_ID || actionID == COOL_ID) {
			pointHeater.setLocation(-1, -1);
		}
		repaint();
		e.consume();
	}

	void processKeyPressed(KeyEvent e) {

		super.processKeyPressed(e);

		int keyID = e.getKeyCode();

		if (selectedComponent != null) {
			if (!isEditable() && !selectedComponent.isDraggable()) {
				showActionTip("<html><font color=red>The selected object is not nudgable!</font></html>", 10, 10);
			}
			else {
				int dx = 0, dy = 0;
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
				model.notifyChange();
				if (selectedComponent instanceof GayBerneParticle) {
					GayBerneParticle p = (GayBerneParticle) selectedComponent;
					p.storeCurrentState();
					p.translateBy(dx, dy);
					boundary.setRBC(p, RectangularBoundary.TRANSLATION);
					finalizeLocation(p);
				}
				else if (selectedComponent instanceof Layered) {
					selectedComponent.storeCurrentState();
					((Layered) selectedComponent).translateBy(dx, dy);
				}
			}

		}
		else {
			boolean keyIsRight = false;
			UserField uf = null;
			for (int i = 0; i < nParticle; i++) {
				uf = gb[i].getUserField();
				if (uf != null) {
					keyIsRight = false;
					switch (keyID) {
					case KeyEvent.VK_UP:
						uf.setAngle(0, -1);
						keyIsRight = true;
						break;
					case KeyEvent.VK_DOWN:
						uf.setAngle(0, 1);
						keyIsRight = true;
						break;
					case KeyEvent.VK_LEFT:
						uf.setAngle(-1, 0);
						keyIsRight = true;
						break;
					case KeyEvent.VK_RIGHT:
						uf.setAngle(1, 0);
						keyIsRight = true;
						break;
					case KeyEvent.VK_OPEN_BRACKET:
						// uf.setOrientation(UserField.INWARD);
						keyIsRight = true;
						break;
					case KeyEvent.VK_CLOSE_BRACKET:
						// uf.setOrientation(UserField.OUTWARD);
						keyIsRight = true;
						break;
					}
					if (keyIsRight) {
						switch (uf.getMode()) {
						case UserField.FORCE_MODE:
							uf.setIntensity(UserField.INCREMENT * uf.getGear());
							break;
						case UserField.IMPULSE_MODE:
							uf.increaseGear();
							break;
						}
					}
				}
			}
		}

		if (model.getJob() == null || model.getJob().isStopped())
			repaint();

		// KeyEvent must be consumed, otherwise the keyboard manager will be confused when the KeyEvent
		// should be applied to this view or to its parent component. WARNING!!! This treatment can cause
		// key binding to fail. You MUST set the onKeyRelease flag to be true when setting the key stroke
		// for a binding.
		if (hasFocus())
			e.consume();

	}

	private void moveHostTo(ModelComponent host, double x, double y) {
		if (host instanceof GayBerneParticle) {
			GayBerneParticle p = (GayBerneParticle) host;
			p.translateTo(x, y);
			boundary.setRBC(p, Boundary.TRANSLATION);
			refreshForces();
		}
		else if (host instanceof RectangularObstacle) {
			RectangularObstacle obs = (RectangularObstacle) host;
			obs.translateCenterTo(x, y);
		}
	}

	public void notifyNOPChange() {
		nParticle = model.getNumberOfParticles();
	}

	int getSteeringForceScale() {
		int s = 0;
		for (int i = 0; i < nParticle; i++) {
			if (gb[i].getUserField() != null) {
				s = (int) (gb[i].getUserField().getGear() * 4.0);
				break;
			}
		}
		return s;
	}

	/** Delegate for serializing this class. */
	public static class State extends MDView.State {

		private boolean showVVectors, showOmegas, drawDipole = true;

		public State() {
			super();
		}

		public void setShowVVectors(boolean b) {
			showVVectors = b;
		}

		public boolean getShowVVectors() {
			return showVVectors;
		}

		public void setShowOmegas(boolean b) {
			showOmegas = b;
		}

		public boolean getShowOmegas() {
			return showOmegas;
		}

		public void setDrawDipole(boolean b) {
			drawDipole = b;
		}

		public boolean getDrawDipole() {
			return drawDipole;
		}

	}

	private class UndoableInsertion extends AbstractUndoableEdit {

		private String presentationName = "";
		private int undoID;

		public UndoableInsertion(int undoID, double x, double y) {
			switch (actionID) {
			case ADGB_ID:
				presentationName = "Inserting Particle";
				break;
			case DUPL_ID:
				presentationName = "Duplicating Particle";
				break;
			}
			this.undoID = undoID;
		}

		public String getPresentationName() {
			return presentationName;
		}

		public void undo() {
			super.undo();
			switch (undoID) {
			case UndoAction.INSERT_A_PARTICLE:
				nParticle--;
				if (!model.getRecorderDisabled())
					gb[nParticle].initializeMovieQ(-1);
				if (gb[nParticle].isSelected())
					setSelectedComponent(null);
				model.setNumberOfParticles(nParticle);
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
					gb[nParticle].initializeMovieQ(model.getMovie().getCapacity());
				gb[nParticle].setSelected(true);
				nParticle++;
				model.setNumberOfParticles(nParticle);
				break;
			}
			doNotFireUndoEvent = false;
			model.notifyChange();
			repaint();
		}

	}

	private class UndoableDeletion extends AbstractUndoableEdit {

		private int undoID, nRemoved;
		private Layered[] removedLayers;

		public UndoableDeletion(int undoID, int nRemoved) {
			this.undoID = undoID;
			this.nRemoved = nRemoved;
		}

		public UndoableDeletion(int undoID, int nRemoved, Layered[] removedLayers) {
			this(undoID, nRemoved);
			this.removedLayers = removedLayers;
		}

		public String getPresentationName() {
			return "Deletion";
		}

		public void undo() {
			super.undo();
			switch (undoID) {
			case UndoAction.BLOCK_REMOVE:
				undoRemoveMarkedParticles(nRemoved);
				if (removedLayers != null) {
					for (Layered l : removedLayers)
						layerBasket.add(l);
					if (nRemoved <= 0 && removedLayers.length == 1)
						((ModelComponent) removedLayers[0]).setSelected(true);
				}
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
				removeMarkedParticles(list);
				if (removedLayers != null) {
					for (Layered l : removedLayers)
						layerBasket.remove(l);
				}
				break;
			}
			doNotFireUndoEvent = false;
			model.notifyChange();
			repaint();
		}

	}

	private class UndoableMoving extends AbstractUndoableEdit {

		private ModelComponent mc;
		private double x, y, theta;
		private String presentationName;

		UndoableMoving(ModelComponent mc) {
			this.mc = mc;
			switch (actionID) {
			case ROTA_ID:
				presentationName = "Rotation";
				if (mc instanceof GayBerneParticle)
					theta = ((GayBerneParticle) mc).getTheta();
				break;
			case SELE_ID:
				presentationName = "Translation";
				x = mc.getRx();
				y = mc.getRy();
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
			mc.setSelected(true);
			model.notifyChange();
			repaint();
		}

		public void redo() {
			super.redo();
			doNotFireUndoEvent = true;
			if (presentationName.equals("Translation")) {
				if (mc instanceof GayBerneParticle) {
					((GayBerneParticle) mc).translateTo(x, y);
				}
				else if (mc instanceof Layered) {
					((Layered) mc).setLocation(x, y);
				}
			}
			else if (presentationName.equals("Rotation")) {
				if (mc instanceof GayBerneParticle) {
					((GayBerneParticle) mc).setTheta(theta);
					((GayBerneParticle) mc).setSelectedToRotate(true);
				}
			}
			doNotFireUndoEvent = false;
			mc.setSelected(true);
			model.notifyChange();
			repaint();
		}

	}

	private class UndoableResizing extends AbstractUndoableEdit {

		private ModelComponent mc;
		private double x, y;
		private String presentationName = "";

		UndoableResizing(ModelComponent mc) {
			this.mc = mc;
			if (mc instanceof GayBerneParticle) {
				switch (actionID) {
				case RESI_ID:
					x = ((GayBerneParticle) mc).getBreadth();
					y = ((GayBerneParticle) mc).getLength();
					presentationName = "Resizing Particle";
					break;
				case VELO_ID:
					x = ((GayBerneParticle) mc).getVx();
					y = ((GayBerneParticle) mc).getVy();
					presentationName = "Changing Velocity";
					break;
				case OMEG_ID:
					x = ((GayBerneParticle) mc).getOmega();
					presentationName = "Changing Angular Velocity";
					break;
				}
			}
		}

		public String getPresentationName() {
			return presentationName;
		}

		public void undo() {
			super.undo();
			mc.restoreState();
			mc.setSelected(true);
			model.notifyChange();
			repaint();
		}

		public void redo() {
			super.redo();
			doNotFireUndoEvent = true;
			if (mc instanceof GayBerneParticle) {
				if (presentationName.equals("Resizing Particle")) {
					((GayBerneParticle) mc).setBreadth(x);
					((GayBerneParticle) mc).setLength(y);
				}
				else if (presentationName.equals("Changing Velocity")) {
					((GayBerneParticle) mc).setVx(x);
					((GayBerneParticle) mc).setVy(y);
				}
				else if (presentationName.equals("Changing Angular Velocity")) {
					((GayBerneParticle) mc).setOmega(x);
				}
			}
			mc.setSelected(true);
			doNotFireUndoEvent = false;
			model.notifyChange();
			repaint();
		}

	}

}