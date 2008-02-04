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
package org.jmol.viewer;

import java.awt.Rectangle;

import org.jmol.g3d.Graphics3D;

/**
 * @author Charles Xie
 * 
 */
class ExtendedFrameRenderer extends FrameRenderer {

	Planes planes;
	Triangles triangles;
	Ellipses ellipses;
	Cuboids cuboids;
	Cylinders cylinders;
	Obstacles obstacles;
	Trajectories trajectories;
	VdwForceLines vdwForceLines;
	ABonds abonds;
	TBonds tbonds;
	Rover rover;

	private PlanesRenderer planesRenderer;
	private TrianglesRenderer trianglesRenderer;
	private EllipsesRenderer ellipsesRenderer;
	private CuboidsRenderer cuboidsRenderer;
	private CylindersRenderer cylindersRenderer;
	private ObstaclesRenderer obstaclesRenderer;
	private TrajectoriesRenderer trajectoriesRenderer;
	private VdwForceLinesRenderer vdwForceLinesRenderer;
	private ABondsRenderer abondsRenderer;
	private TBondsRenderer tbondsRenderer;
	private RoverRenderer roverRenderer;

	private boolean roverPainted;

	ExtendedFrameRenderer(Viewer viewer) {
		super(viewer);
		planes = new Planes();
		triangles = new Triangles();
		ellipses = new Ellipses();
		cuboids = new Cuboids();
		cylinders = new Cylinders();
		obstacles = new Obstacles();
		trajectories = new Trajectories();
		vdwForceLines = new VdwForceLines();
		abonds = new ABonds();
		tbonds = new TBonds();
		rover = new Rover();
	}

	void clear() {
		trajectories.clear();
		cylinders.clear();
		planes.clear();
		triangles.clear();
		obstacles.clear();
		ellipses.clear();
		abonds.clear();
		tbonds.clear();
		roverPainted = false;
		if (viewer instanceof ExtendedViewer) { // always keep the glass box
			Cuboid c = cuboids.getCuboid(0);
			cuboids.clear();
			if (c != null)
				cuboids.addCuboid(c);
		}
	}

	void setRoverPainted(boolean b) {
		roverPainted = b;
	}

	void render(Graphics3D g3d, Rectangle rectClip, Frame frame, int displayModelIndex) {

		if (frame == null)
			return;

		super.render(g3d, rectClip, frame, displayModelIndex);

		if (!planes.isEmpty()) {
			if (planesRenderer == null) {
				planesRenderer = new PlanesRenderer();
				planesRenderer.setViewerFrameRenderer(viewer, this, g3d);
			}
			planesRenderer.render(g3d, rectClip, frame, displayModelIndex, planes);
		}

		if (!triangles.isEmpty()) {
			if (trianglesRenderer == null) {
				trianglesRenderer = new TrianglesRenderer();
				trianglesRenderer.setViewerFrameRenderer(viewer, this, g3d);
			}
			trianglesRenderer.render(g3d, rectClip, frame, displayModelIndex, triangles);
		}

		if (!ellipses.isEmpty()) {
			if (ellipsesRenderer == null) {
				ellipsesRenderer = new EllipsesRenderer();
				ellipsesRenderer.setViewerFrameRenderer(viewer, this, g3d);
			}
			ellipsesRenderer.render(g3d, rectClip, frame, displayModelIndex, ellipses);
		}

		if (!cuboids.isEmpty()) {
			if (cuboidsRenderer == null) {
				cuboidsRenderer = new CuboidsRenderer();
				cuboidsRenderer.setViewerFrameRenderer(viewer, this, g3d);
			}
			cuboidsRenderer.render(g3d, rectClip, frame, displayModelIndex, cuboids);
		}

		if (!cylinders.isEmpty()) {
			if (cylindersRenderer == null) {
				cylindersRenderer = new CylindersRenderer();
				cylindersRenderer.setViewerFrameRenderer(viewer, this, g3d);
			}
			cylindersRenderer.render(g3d, rectClip, frame, displayModelIndex, cylinders);
		}

		if (!obstacles.isEmpty()) {
			if (obstaclesRenderer == null) {
				obstaclesRenderer = new ObstaclesRenderer();
				obstaclesRenderer.setViewerFrameRenderer(viewer, this, g3d);
			}
			obstaclesRenderer.render(g3d, rectClip, frame, displayModelIndex, obstacles);
		}

		if (!trajectories.isEmpty()) {
			if (trajectoriesRenderer == null) {
				trajectoriesRenderer = new TrajectoriesRenderer();
				trajectoriesRenderer.setViewerFrameRenderer(viewer, this, g3d);
			}
			trajectoriesRenderer.render(g3d, rectClip, frame, displayModelIndex, trajectories);
		}

		if (vdwForceLines.pairs != null) {
			if (vdwForceLinesRenderer == null) {
				vdwForceLinesRenderer = new VdwForceLinesRenderer();
				vdwForceLinesRenderer.setViewerFrameRenderer(viewer, this, g3d);
			}
			vdwForceLinesRenderer.render(g3d, rectClip, frame, displayModelIndex, vdwForceLines);
		}

		if (!abonds.isEmpty()) {
			if (abondsRenderer == null) {
				abondsRenderer = new ABondsRenderer();
				abondsRenderer.setViewerFrameRenderer(viewer, this, g3d);
			}
			abondsRenderer.render(g3d, rectClip, frame, displayModelIndex, abonds);
		}

		if (!tbonds.isEmpty()) {
			if (tbondsRenderer == null) {
				tbondsRenderer = new TBondsRenderer();
				tbondsRenderer.setViewerFrameRenderer(viewer, this, g3d);
			}
			tbondsRenderer.render(g3d, rectClip, frame, displayModelIndex, tbonds);
		}

		if (roverPainted) {
			if (rover != null) {
				if (roverRenderer == null) {
					roverRenderer = new RoverRenderer(rover);
					roverRenderer.setViewerFrameRenderer(viewer, this, g3d);
				}
				roverRenderer.render(g3d, rectClip, frame, displayModelIndex, rover);
			}
		}

	}

}