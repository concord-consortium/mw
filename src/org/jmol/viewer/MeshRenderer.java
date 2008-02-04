/* $RCSfile: MeshRenderer.java,v $
 * $Author: qxie $
 * $Date: 2007-11-11 22:54:20 $
 * $Revision: 1.12 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.viewer;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3i;
import org.jmol.g3d.Graphics3D;

abstract class MeshRenderer extends ShapeRenderer {

	boolean iShowTriangles;
	boolean iShowNormals;
	boolean iHideBackground;
	short backgroundColix;
	Point3f[] vertices;
	Point3i[] screens;
	float[] vertexValues;
	Point3i pt0 = new Point3i();
	Point3i pt3 = new Point3i();
	boolean frontOnly;
	Vector3f[] transformedVectors;

	boolean render1(Mesh mesh) {
		if (mesh == null)
			return false;
		if (mesh.visibilityFlags == 0)
			return false;
		int vertexCount = mesh.vertexCount;
		if (vertexCount == 0)
			return false;
		vertices = mesh.vertices;
		screens = viewer.allocTempScreens(vertexCount);
		vertexValues = mesh.vertexValues;
		transformedVectors = g3d.getTransformedVertexVectors();
		for (int i = vertexCount; --i >= 0;)
			if (vertexValues == null || !Float.isNaN(vertexValues[i]) || mesh.hasGridPoints) {
				viewer.transformPoint(vertices[i], screens[i]);
				// System.out.println(i + " meshRender " + vertices[i] + screens[i]);
			}
		iShowTriangles = viewer.getTestFlag3();
		iShowNormals = viewer.getTestFlag4();
		iHideBackground = (mesh.jvxlPlane != null && mesh.hideBackground);
		if (iHideBackground) {
			backgroundColix = Graphics3D.getColix(viewer.getBackgroundArgb());
		}
		boolean isDrawPickMode = (mesh.meshType == "draw" && viewer.getPickingMode() == JmolConstants.PICKING_DRAW);
		int drawType = mesh.drawType;
		short colix = mesh.colix;
		if ((drawType == Mesh.DRAW_CURVE || drawType == Mesh.DRAW_ARROW) && mesh.vertexCount >= 2) {
			for (int i = 0, i0 = 0; i < mesh.vertexCount - 1; i++) {
				g3d.fillHermite(colix, 5, 3, 3, 3, screens[i0], screens[i], screens[i + 1], screens[i
						+ (i + 2 == mesh.vertexCount ? 1 : 2)]);
				i0 = i;
			}
		}
		switch (drawType) {
		case Mesh.DRAW_ARROW:
			Point3i pt1 = screens[mesh.vertexCount - 2];
			Point3i pt2 = screens[mesh.vertexCount - 1];
			Vector3f tip = new Vector3f(pt2.x - pt1.x, pt2.y - pt1.y, pt2.z - pt1.z);
			float d = tip.length();
			if (d > 0) {
				tip.scale(5 / d);
				pt0.x = pt2.x - (int) Math.floor(4 * tip.x);
				pt0.y = pt2.y - (int) Math.floor(4 * tip.y);
				pt0.z = pt2.z - (int) Math.floor(4 * tip.z);
				pt3.x = pt2.x + (int) Math.floor(tip.x);
				pt3.y = pt2.y + (int) Math.floor(tip.y);
				pt3.z = pt2.z + (int) Math.floor(tip.z);
				g3d.fillCone(colix, Graphics3D.ENDCAPS_FLAT, 15, pt0, pt3);
			}
			break;
		case Mesh.DRAW_CIRCLE:
			// unimplemented
			break;
		case Mesh.DRAW_CURVE:
			// unnecessary
			break;
		default:
			if (mesh.showPoints)
				renderPoints(mesh, screens, vertexCount);
			if (iShowNormals)
				renderNormals(mesh, screens, vertexCount);
			if (mesh.drawTriangles)
				renderTriangles(mesh, screens, false);
			if (mesh.fillTriangles)
				renderTriangles(mesh, screens, true);
		}
		if (isDrawPickMode) {
			renderHandles(mesh, screens, vertexCount);
		}
		viewer.freeTempScreens(screens);
		return true;
	}

	void renderHandles(Mesh mesh, Point3i[] screens, int vertexCount) {
		switch (mesh.drawType) {
		case Mesh.DRAW_POINT:
		case Mesh.DRAW_ARROW:
		case Mesh.DRAW_CURVE:
		case Mesh.DRAW_LINE:
		case Mesh.DRAW_PLANE:
		case Mesh.DRAW_CIRCLE:
		case Mesh.DRAW_MULTIPLE:
			for (int i = mesh.polygonCount; --i >= 0;) {
				if (!mesh.isPolygonDisplayable(i))
					continue;
				int[] vertexIndexes = mesh.polygonIndexes[i];
				if (vertexIndexes == null)
					continue;
				for (int j = vertexIndexes.length; --j >= 0;) {
					int k = vertexIndexes[j];
					g3d.fillScreenedCircleCentered(Graphics3D.GOLD, 10, screens[k].x, screens[k].y, screens[k].z);
				}
				break;
			}
		}
	}

	void renderPoints(Mesh mesh, Point3i[] screens, int vertexCount) {
		short colix = mesh.colix;
		short[] vertexColixes = mesh.vertexColixes;
		int iCount = (mesh.lastViewableVertex > 0 ? mesh.lastViewableVertex + 1 : vertexCount);
		int iFirst = mesh.firstViewableVertex;
		for (int i = iCount; --i >= iFirst;)
			if (vertexValues != null && !Float.isNaN(vertexValues[i]))
				g3d.fillSphereCentered(vertexColixes != null ? vertexColixes[i] : colix, 4, screens[i]);
		if (mesh.hasGridPoints)
			for (int i = 0; i < iFirst; i++)
				g3d.fillSphereCentered(Graphics3D.GRAY, 2, screens[i]);
		if (mesh.hasGridPoints && !mesh.isContoured) {
			for (int i = 1; i < vertexCount; i += 3) {
				g3d.fillCylinder(Graphics3D.GRAY, Graphics3D.ENDCAPS_SPHERICAL, 1, screens[i], screens[i + 1]);
			}
		}
	}

	final Point3f ptTemp = new Point3f();
	final Point3i ptTempi = new Point3i();

	void renderNormals(Mesh mesh, Point3i[] screens, int vertexCount) {
		// Logger.debug("mesh renderPoints: " + vertexCount);
		for (int i = vertexCount; --i >= 0;)
			if (true || vertexValues != null && !Float.isNaN(vertexValues[i]))
				if ((i % 3) == 0) { // investigate vertex normixes
					ptTemp.set(mesh.vertices[i]);
					short n = mesh.normixes[i];
					// -n is an intensity2sided and does not correspond to a true normal index
					if (n > 0) {
						ptTemp.add(g3d.getNormixVector(n));
						viewer.transformPoint(ptTemp, ptTempi);
						g3d.fillCylinder(Graphics3D.WHITE, Graphics3D.ENDCAPS_SPHERICAL, 1, screens[i], ptTempi);
					}
				}
	}

	void renderTriangles(Mesh mesh, Point3i[] screens, boolean fill) {
		int[][] polygonIndexes = mesh.polygonIndexes;
		short[] normixes = mesh.normixes;
		short colix = mesh.colix;

		short[] vertexColixes = mesh.vertexColixes;
		short hideColix = 0;
		try {
			hideColix = vertexColixes[mesh.polygonIndexes[0][0]];
		}
		catch (Exception e) {
		}
		for (int i = mesh.polygonCount; --i >= 0;) {
			if (!mesh.isPolygonDisplayable(i))
				continue;
			int[] vertexIndexes = polygonIndexes[i];
			if (vertexIndexes == null)
				continue;
			int iA = vertexIndexes[0];
			int iB = vertexIndexes[1];
			int iC = vertexIndexes[2];
			short colixA, colixB, colixC;
			if (vertexColixes != null) {
				colixA = vertexColixes[iA];
				colixB = vertexColixes[iB];
				colixC = vertexColixes[iC];
			}
			else {
				colixA = colixB = colixC = colix;
			}
			if (iHideBackground) {
				if (colixA == hideColix && colixB == hideColix && colixC == hideColix)
					continue;
				if (colixA == hideColix)
					colixA = backgroundColix;
				if (colixB == hideColix)
					colixB = backgroundColix;
				if (colixC == hideColix)
					colixC = backgroundColix;
			}
			if (iB == iC) {
				g3d.fillCylinder(colixA, Graphics3D.ENDCAPS_SPHERICAL, (iA == iB ? 6 : 3), screens[iA], screens[iB]);
			}
			else if (vertexIndexes.length == 3) {
				if (fill)
					if (iShowTriangles)
						g3d.fillTriangle(screens[iA], colixA, normixes[iA], screens[iB], colixB, normixes[iB],
								screens[iC], colixC, normixes[iC], 0.1f);
					else {
						if (frontOnly && transformedVectors[normixes[iA]].z < 0
								&& transformedVectors[normixes[iB]].z < 0 && transformedVectors[normixes[iC]].z < 0)
							continue;
						if (screens != null && normixes != null) // XIE
							g3d.fillTriangle(screens[iA], colixA, normixes[iA], screens[iB], colixB, normixes[iB],
									screens[iC], colixC, normixes[iC]);
					}
				else
				// FIX ME ... need a drawTriangle routine with multiple colors
				g3d.drawTriangle(colixA, screens[iA], screens[iB], screens[iC]);

			}
			else if (vertexIndexes.length == 4) {
				int iD = vertexIndexes[3];
				short colixD = vertexColixes != null ? vertexColixes[iD] : colix;
				if (fill) {
					if (frontOnly && transformedVectors[normixes[iA]].z < 0 && transformedVectors[normixes[iB]].z < 0
							&& transformedVectors[normixes[iC]].z < 0 && transformedVectors[normixes[iD]].z < 0)
						continue;
					g3d.fillQuadrilateral(screens[iA], colixA, normixes[iA], screens[iB], colixB, normixes[iB],
							screens[iC], colixC, normixes[iC], screens[iD], colixD, normixes[iD]);
				}
				else g3d.drawQuadrilateral(colixA, screens[iA], screens[iB], screens[iC], screens[iD]);

				// } else {
				// Logger.debug("MeshRenderer: polygon with > 4 sides");
			}
		}
	}
}
