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
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.util.BitSet;
import java.util.Iterator;

import org.concord.mw2d.models.Atom;
import org.concord.mw2d.models.MolecularModel;
import org.concord.mw2d.models.RadialBond;
import org.concord.mw2d.models.RadialBondCollection;
import org.concord.mw2d.models.ReactionModel;

import org.myjmol.api.JmolAdapter;
import org.myjmol.api.JmolViewer;

class JmolRenderer {

	private final Object lock = new Object();
	private AtomisticView view;
	private JmolAdapter adapter;
	private JmolViewer viewer;
	private final Dimension currentSize = new Dimension();
	private final Rectangle rectClip = new Rectangle();
	private BitSet translucentBitSet;

	private Runnable fitWindow = new Runnable() {
		public void run() {
			if (!view.getSize().equals(currentSize))
				viewer.setScreenDimension(view.getSize(currentSize));
			viewer.fit2DScreen(10);
			viewer.setCenter(0, 0, 0);
			viewer.rotateFront();
			setDisplayStyle(view.getDisplayStyle()); // this method already calls fresh().
		}
	};

	JmolRenderer(AtomisticView view) {

		this.view = view;
		adapter = new Mw2dJmolAdapter(null);
		viewer = JmolViewer.allocateExtendedViewer(view, adapter);
		viewer.setMw2dFlag(true);
		viewer.setAutoBond(false);
		viewer.setShowBbcage(false);
		viewer.setPerspectiveDepth(false);
		viewer.setShowRebondTime(false);
		viewer.setPercentVdwAtom(100);
		viewer.setHoverEnabled(false);
		viewer.setDisablePopupMenu(true);
		viewer.setColorBackground(new Color(0x00ffffff, true));
		viewer.setSimulationBox((float) view.model.getBoundary().getWidth(), (float) view.model.getBoundary()
				.getHeight(), 1);

		// remove the jmol listeners
		MouseListener[] m = view.getMouseListeners();
		for (MouseListener i : m) {
			if (i.getClass().getName().startsWith("org.myjmol"))
				view.removeMouseListener(i);
		}
		MouseMotionListener[] a = view.getMouseMotionListeners();
		for (MouseMotionListener i : a) {
			if (i.getClass().getName().startsWith("org.myjmol"))
				view.removeMouseMotionListener(i);
		}
		MouseWheelListener[] b = view.getMouseWheelListeners();
		for (MouseWheelListener i : b) {
			if (i.getClass().getName().startsWith("org.myjmol"))
				view.removeMouseWheelListener(i);
		}

	}

	void setDisplayStyle(byte i) {
		viewer.selectAll();
		switch (i) {
		case StyleConstant.SPACE_FILLING:
			viewer.setPercentVdwAtom(100);
			viewer.setMarBond((short) 800);
			break;
		case StyleConstant.BALL_AND_STICK:
			viewer.setPercentVdwAtom(50);
			viewer.setMarBond((short) 800);
			break;
		case StyleConstant.WIRE_FRAME:
			viewer.setPercentVdwAtom(10);
			viewer.setMarBond((short) 300);
			break;
		case StyleConstant.STICK:
			viewer.setPercentVdwAtom(10);
			viewer.setMarBond((short) 1400);
			break;
		}
		refresh();
	}

	void setTranslucent(boolean b) {
		int n = view.model.getNumberOfAtoms();
		if (n <= 0)
			return;
		if (b) {
			if (translucentBitSet == null) {
				translucentBitSet = new BitSet(n);
				viewer.setTranslucentBitSet(translucentBitSet);
			}
			for (int i = 0; i < n; i++) {
				translucentBitSet.set(i, view.model.getAtom(i).isSelected());
			}
		}
		else {
			if (translucentBitSet != null)
				translucentBitSet.clear();
		}
	}

	void setBackground(Color c) {
		viewer.setColorBackground(c);
	}

	void openClientObject(MolecularModel model) {
		try {
			viewer.openClientObject(model);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		// this has to be invoked later in order to match the UI's current size
		EventQueue.invokeLater(fitWindow);
	}

	void renderBonds() {
		synchronized (lock) {
			viewer.deleteAllBonds();
			RadialBond bond = null;
			RadialBondCollection bondCollection = view.model.getBonds();
			synchronized (bondCollection.getSynchronizationLock()) {
				for (Iterator it = bondCollection.iterator(); it.hasNext();) {
					bond = (RadialBond) it.next();
					if (!bond.isVisible())
						continue;
					if (bond.getBondStyle() == RadialBond.STANDARD_STICK_STYLE
							|| bond.getBondStyle() == RadialBond.UNICOLOR_STICK_STYLE)
						viewer.addRBond(bond.getAtom1(), bond.getAtom2());
				}
			}
		}
	}

	private float getCurrentSigma(Atom a) {
		int vdw = viewer.getPercentVdwAtom();
		return vdw == 100 ? (float) a.getSigma() : (float) a.getSigma() * vdw * 0.01f;
	}

	void refresh() {
		int n = view.getModel().getNumberOfParticles();
		if (n > viewer.getAtomCount())
			n = viewer.getAtomCount();
		Atom at;
		int argb = 0;
		float sigma = 0;
		for (int i = 0; i < n; i++) {
			at = view.atom[i];
			if (at.isVisible()) {
				sigma = getCurrentSigma(at);
				if (view.model instanceof ReactionModel) {
					sigma *= 2; // special treatment to deal with covalent bond is much shorter than vdw bond
				}
			}
			else {
				sigma = 0;
			}
			if (sigma > 0.000001f) {
				if (at.isMarked()) {
					argb = view.getMarkColor().getRGB();
				}
				else if (view.shadingShown()) {
					argb = view.getKeShadingColor((at.getVx() * at.getVx() + at.getVy() * at.getVy()) * at.getMass())
							.getRGB();
				}
				else if (view.chargeShadingShown()) {
					argb = view.getChargeShadingColor(at.getCharge()).getRGB();
				}
				else {
					// argb = view.getColor(at);
					argb = at.getColor().getRGB();
				}
			}
			viewer.setAtomCoordinates(i, 0.1f * (float) (at.getRx() - view.getWidth() * 0.5),
					0.1f * (float) (0.5 * view.getHeight() - at.getRy()), 0, 100 * sigma, argb | 0xff000000);
			// set alpha to 255
		}
		viewer.refresh();
	}

	void render(Graphics2D g) {

		if (viewer.getPercentVdwAtom() < 100) {
			if (view.vdwCirclesShown()) {
				view.styleManager.drawVdwCircles(g);
				view.styleManager.showColorGradientEffect(g);
			}
		}

		// render using jmol
		synchronized (lock) {
			viewer.setScreenDimension(view.getSize(currentSize));
			g.getClipBounds(rectClip); // what is the clip bound for?
			viewer.renderScreenImage(g, currentSize, rectClip);
		}

	}

}