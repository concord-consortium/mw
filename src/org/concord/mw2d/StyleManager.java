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
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.concord.modeler.draw.EllipticalGradientPaint;
import org.concord.mw2d.geometry.Fortune;
import org.concord.mw2d.models.Atom;
import org.concord.mw2d.models.Element;
import org.concord.mw2d.models.MolecularModel;
import org.concord.mw2d.models.ReactionModel;

class StyleManager {

	private byte style = StyleConstant.SPACE_FILLING;
	private byte vdwSphereStyle = StyleConstant.VDW_DOTTED_CIRCLE;
	private boolean translucent;

	private BufferedImage[] gradientImages;
	private Fortune fortune;
	private AtomisticView view;

	StyleManager(AtomisticView view) {
		this.view = view;
	}

	void reset() {
		setStyle(StyleConstant.SPACE_FILLING);
		fortune = null;
		setTranslucent(false);
	}

	void setTranslucent(boolean b) {
		translucent = b;
	}

	boolean isTranslucent() {
		return translucent;
	}

	byte getVdwPercentage() {
		switch (style) {
		case StyleConstant.SPACE_FILLING:
			return 100;
		case StyleConstant.BALL_AND_STICK:
			return 50;
		}
		return 25;
	}

	void setStyle(byte i) {
		style = i;
		switch (style) {
		case StyleConstant.DELAUNAY:
			if (fortune == null)
				fortune = new Fortune();
			fortune.showDelaunay(true);
			fortune.showVoronoi(false);
			break;
		case StyleConstant.VORONOI:
			if (fortune == null)
				fortune = new Fortune();
			fortune.showDelaunay(false);
			fortune.showVoronoi(true);
			break;
		case StyleConstant.DELAUNAY_AND_VORONOI:
			if (fortune == null)
				fortune = new Fortune();
			fortune.showDelaunay(true);
			fortune.showVoronoi(true);
			break;
		}
	}

	byte getStyle() {
		return style;
	}

	void setVDWSphereStyle(byte i) {
		vdwSphereStyle = i;
	}

	byte getVDWSphereStyle() {
		return vdwSphereStyle;
	}

	boolean isVoronoiStyle() {
		return style >= StyleConstant.DELAUNAY && style <= StyleConstant.DELAUNAY_AND_VORONOI;
	}

	Color getKeShadingColor(double ke) {
		int icolor = 0;
		if (view.getRelativeKEForShading() > MDView.ZERO)
			icolor = (int) ((ke * 62.5 / view.getRelativeKEForShading()) * 1024.0);
		if (icolor > 0xff)
			icolor = 0xff;
		return new Color((0xff << 24) | (0xff << 16) | ((0xff ^ icolor) << 8) | (0xff ^ icolor));
	}

	Color getChargeShadingColor(double charge) {
		int icolor = (int) (Math.abs(charge) * 51);
		if (icolor > 0xff)
			icolor = 0xff;
		return charge > 0 ? new Color((0xff << 24) | (0xff << 16) | ((0xff ^ icolor) << 8) | (0xff ^ icolor))
				: new Color((0xff << 24) | ((0xff ^ icolor) << 16) | ((0xff ^ icolor) << 8) | 0xff);
	}

	void renderFortune(Graphics2D g) {
		if (fortune == null)
			return;
		fortune.init(view.nAtom, view.atom, view.getWidth(), view.getHeight());
		fortune.compute();
		g.setColor(Color.blue);
		g.setStroke(ViewAttribute.THIN);
		fortune.paintVoronoi(g);
		g.setColor(Color.gray);
		g.setStroke(ViewAttribute.THIN_DASHED);
		fortune.paintDelaunay(g);
	}

	void drawVdwCircles(Graphics2D g) {
		if ((vdwSphereStyle & StyleConstant.VDW_DOTTED_CIRCLE) != StyleConstant.VDW_DOTTED_CIRCLE)
			return;
		int n = view.getModel().getNumberOfParticles();
		g.setColor(view.contrastBackground());
		g.setStroke(ViewAttribute.THIN_DOTTED);
		Atom a;
		double d;
		boolean reaction = view.model instanceof ReactionModel;
		for (int i = 0; i < n; i++) {
			a = view.atom[i];
			if (a.isVisible()) {
				d = a.getSigma();
				if (reaction) {
					g.drawOval((int) (a.getRx() - d), (int) (a.getRy() - d), (int) (d + d), (int) (d + d));
				}
				else {
					g.drawOval((int) (a.getRx() - 0.5 * d), (int) (a.getRy() - 0.5 * d), (int) d, (int) d);
				}
			}
		}
	}

	private void createGradientImage(int id) {
		if (id < 0 || id >= Element.getNumberOfElements())
			return;
		if (gradientImages == null)
			gradientImages = new BufferedImage[Element.getNumberOfElements()];
		int d = (int) ((MolecularModel) view.getModel()).getElement(id).getSigma();
		if (gradientImages[id] == null || gradientImages[id].getWidth() != d) {
			Color c1 = view.contrastBackground();
			Color c2 = new Color(view.getBackground().getRGB() & 0x00ffffff, true);
			gradientImages[id] = new BufferedImage(d, d, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = gradientImages[id].createGraphics();
			g.setPaint(new EllipticalGradientPaint(0.5 * d, 0.5 * d, 0.6 * d, 0.6 * d, 0, c1, c2));
			g.fillOval(0, 0, d, d);
			g.dispose();
		}
	}

	void showColorGradientEffect(Graphics2D g) {
		if ((vdwSphereStyle & StyleConstant.VDW_RADIAL_COLOR_GRADIENT) != StyleConstant.VDW_RADIAL_COLOR_GRADIENT)
			return;
		int id;
		for (int i = 0; i < view.nAtom; i++) {
			if (view.atom[i].isVisible()) {
				id = view.atom[i].getID();
				createGradientImage(id);
				g.drawImage(gradientImages[id], (int) (view.atom[i].getRx() - view.atom[i].getSigma() * 0.5),
						(int) (view.atom[i].getRy() - view.atom[i].getSigma() * 0.5), view);
			}
		}
	}

}