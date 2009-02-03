/* $RCSfile: Viewer.java,v $
 * $Author: qxie $
 * $Date: 2007-12-05 14:19:13 $
 * $Revision: 1.139 $
 *
 * Copyright (C) 2002-2006  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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
package org.myjmol.viewer;

import org.jmol.api.Attachment;
import org.jmol.api.InteractionCenter;
import org.jmol.api.SiteAnnotation;
import org.myjmol.api.*;
import org.myjmol.g3d.*;
import org.myjmol.i18n.GT;
import org.myjmol.symmetry.UnitCell;
import org.myjmol.util.*;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Component;
import java.awt.Event;
import java.awt.geom.Line2D;
import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.BitSet;
import java.util.Properties;
import java.util.Vector;
import java.util.Enumeration;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3i;
import javax.vecmath.Matrix4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.AxisAngle4f;
import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.io.Reader;

/*
 * The JmolViewer can be used to render client molecules. Clients implement the JmolAdapter. JmolViewer uses this
 * interface to extract information from the client data structures and render the molecule to the supplied
 * java.awt.Component
 * 
 * The JmolViewer runs on Java 1.1 virtual machines. The 3d graphics rendering package is a software implementation of a
 * z-buffer. It does not use Java3D and does not use Graphics2D from Java 1.2. Therefore, it is well suited to building
 * web browser applets that will run on a wide variety of system configurations.
 * 
 * public here is a test for applet-applet and JS-applet communication the idea being that
 * applet.getProperty("jmolViewer") returns this Viewer object, allowing direct inter-process access to public methods.
 * 
 * e.g.
 * 
 * applet.getProperty("jmolApplet").getFullPathName()
 * 
 */

public class Viewer extends JmolViewer {

	public void finalize() {
		Logger.debug("viewer finalize " + this);
	}

	// these are all private now so we are certain they are not
	// being accesed by any other classes

	private Component display;
	private Graphics3D g3d;
	JmolAdapter modelAdapter;

	private CommandHistory commandHistory = new CommandHistory();
	ColorManager colorManager;
	private Eval eval;
	private FileManager fileManager;
	ModelManager modelManager;
	private MouseManager mouseManager;
	private PickingManager pickingManager;
	private PropertyManager propertyManager;
	RepaintManager repaintManager;
	private ScriptManager scriptManager;
	private SelectionManager selectionManager;
	private StateManager stateManager;
	private StateManager.GlobalSettings global;
	private StatusManager statusManager;
	private TempManager tempManager;
	MyTransformManager transformManager; // XIE
	int clickedAtom = -1; // XIE
	int clickedBond = -1; // XIE
	boolean depthCueing; // XIE
	int activeAtomAnnotation = -1; // XIE
	int activeBondAnnotation = -1; // XIE
	int activeAtomInteraction = -1; // XIE
	int activeBondInteraction = -1; // XIE
	boolean engineOn; // XIE
	boolean interactionCentersVisible = true; // XIE
	boolean keShading;

	private String strJavaVendor;
	private String strJavaVersion;
	private String strOSName;
	private String htmlName = "";

	private boolean jvm11orGreater = false;
	private boolean jvm12orGreater = false;
	private boolean jvm14orGreater = false;

	Viewer(Component display, JmolAdapter modelAdapter) {
		// XIE: silent the logger
		Logger.setSilent(true);
		Logger.debug("Viewer constructor " + this);
		this.display = display;
		this.modelAdapter = modelAdapter;
		strJavaVendor = System.getProperty("java.vendor");
		strOSName = System.getProperty("os.name");
		strJavaVersion = System.getProperty("java.version");
		// Netscape on MacOS does not implement 1.1 event model
		jvm11orGreater = (strJavaVersion.compareTo("1.1") >= 0 && !(strJavaVendor.startsWith("Netscape")
				&& strJavaVersion.compareTo("1.1.5") <= 0 && "Mac OS".equals(strOSName)));
		jvm12orGreater = (strJavaVersion.compareTo("1.2") >= 0);
		jvm14orGreater = (strJavaVersion.compareTo("1.4") >= 0);
		stateManager = new StateManager(this);
		global = stateManager.getGlobalSettings();
		g3d = new Graphics3D(display);
		colorManager = new ColorManager(this, g3d);
		setStringProperty("backgroundColor", "black");
		statusManager = new StatusManager(this);
		scriptManager = new ScriptManager(this);
		transformManager = new MyTransformManager(this); // XIE
		selectionManager = new SelectionManager(this);
		if (jvm14orGreater)
			mouseManager = MouseWrapper14.alloc(display, this);
		else if (jvm11orGreater)
			mouseManager = MouseWrapper11.alloc(display, this);
		else mouseManager = new MouseManager10(display, this);
		modelManager = new ModelManager(this);
		propertyManager = new PropertyManager(this);
		tempManager = new TempManager(this);
		pickingManager = new PickingManager(this);
		fileManager = new FileManager(this, modelAdapter);
		repaintManager = new RepaintManager(this);
		eval = new Eval(this);
	}

	public void setKEShading(boolean b) {
		keShading = b;
	}

	/** TODO: clean up used objects to reclaim memory */
	public void cleanUp() {
		modelManager.clear();
	}

	/** when loading starts */
	public void loadingStarted() {
		mouseManager.setHoverSuspended(true);
	}

	/** when loading completes */
	public void loadingCompleted() {
		mouseManager.setHoverSuspended(false);
	}

	/**
	 * NOTE: for APPLICATION (not APPLET) call
	 * 
	 * setModeMouse(JmolConstants.MOUSE_NONE);
	 * 
	 * before setting viewer=null
	 * 
	 * @param display
	 *            either DisplayPanel or WrappedApplet
	 * @param modelAdapter
	 *            the model reader
	 * @return a viewer instance
	 */
	public static JmolViewer allocateViewer(Component display, JmolAdapter modelAdapter) {
		return new Viewer(display, modelAdapter);
	}

	boolean isSilent = false;
	boolean isApplet = false;
	boolean autoExit = false;
	String writeInfo;
	boolean haveDisplay = true;
	boolean mustRender = true;

	public static JmolViewer allocateExtendedViewer(Component display, JmolAdapter modelAdapter) {
		return new ExtendedViewer(display, modelAdapter);
	}

	/** XIE: run string scripts without using the script queue and invoking a thread. */
	public void runScriptImmediatelyWithoutThread(String script) {
		if (eval.loadScriptString(script, true)) {
			eval.runEval();
		}
	}

	public void setRoverPainted(boolean b) {
		if (repaintManager.frameRenderer instanceof ExtendedFrameRenderer) {
			((ExtendedFrameRenderer) repaintManager.frameRenderer).setRoverPainted(b);
		}
	}

	public void setRoverPosition(float x, float y, float z) {
		if (repaintManager.frameRenderer instanceof ExtendedFrameRenderer) {
			((ExtendedFrameRenderer) repaintManager.frameRenderer).rover.setPosition(x, y, z);
		}
	}

	/** XIE */
	public void setRoverColor(int argb) {
		if (repaintManager.frameRenderer instanceof ExtendedFrameRenderer) {
			((ExtendedFrameRenderer) repaintManager.frameRenderer).rover.setColix(argb);
		}
	}

	/** XIE */
	public void setInteractionCentersVisible(boolean b) {
		interactionCentersVisible = b;
	}

	/** XIE */
	public void setEngineOn(boolean b) {
		engineOn = b;
	}

	/** XIE */
	public void setActiveKey(byte type, int i, Class c) {
		if (SiteAnnotation.class == c) {
			switch (type) {
			case Attachment.ATOM_HOST:
				activeAtomAnnotation = i;
				break;
			case Attachment.BOND_HOST:
				activeBondAnnotation = i;
				break;
			}
		}
		else if (InteractionCenter.class == c) {
			switch (type) {
			case Attachment.ATOM_HOST:
				activeAtomInteraction = i;
				break;
			case Attachment.BOND_HOST:
				activeBondInteraction = i;
				break;
			}
		}
	}

	/**
	 * XIE: return the host ID of the specified type of attachment on the specified type of object at the selected
	 * screen position
	 */
	public int getAttachmentHost(byte type, int x, int y, Class c) {
		if (modelManager.frame == null)
			return -1;
		int zmin = Integer.MAX_VALUE;
		int imin = -1;
		switch (type) {
		case Attachment.ATOM_HOST:
			Atom[] atoms = modelManager.frame.atoms;
			if (SiteAnnotation.class == c) {
				int atomCount = modelManager.frame.atomCount;
				for (int i = 0; i < atomCount; i++) {
					if (atoms[i] == null)
						continue;
					if (atoms[i].annotationKey && atoms[i].pin != null && atoms[i].pin.withinHandle(x, y)) {
						if (atoms[i].screenZ < zmin) {
							zmin = atoms[i].screenZ;
							imin = i;
						}
					}
				}
			}
			else if (InteractionCenter.class == c) {
				for (int i = 0; i < modelManager.frame.atomCount; i++) {
					if (atoms[i].interactionKey && atoms[i].pin != null && atoms[i].pin.withinHandle(x, y)) {
						if (atoms[i].screenZ < zmin) {
							zmin = atoms[i].screenZ;
							imin = i;
						}
					}
				}
			}
			break;
		case Attachment.BOND_HOST:
			Bond[] bonds = modelManager.frame.bonds;
			int z;
			if (SiteAnnotation.class == c) {
				for (int i = 0; i < modelManager.frame.bondCount; i++) {
					if (bonds[i].annotationKey && bonds[i].pin != null && bonds[i].pin.withinHandle(x, y)) {
						z = (bonds[i].atom1.screenZ + bonds[i].atom2.screenZ) >> 1;
						if (z < zmin) {
							zmin = z;
							imin = i;
						}
					}
				}
			}
			else if (InteractionCenter.class == c) {
				for (int i = 0; i < modelManager.frame.bondCount; i++) {
					if (bonds[i].interactionKey && bonds[i].pin != null && bonds[i].pin.withinHandle(x, y)) {
						z = (bonds[i].atom1.screenZ + bonds[i].atom2.screenZ) >> 1;
						if (z < zmin) {
							zmin = z;
							imin = i;
						}
					}
				}
			}
			break;
		}
		return imin;
	}

	/** XIE */
	public void setAttachmentKeyColor(byte type, int i, int rgb, Class c) {
		if (modelManager.frame == null)
			return;
		switch (type) {
		case Attachment.ATOM_HOST:
			Atom[] atoms = modelManager.frame.atoms;
			if (i >= 0 && i < modelManager.frame.atomCount) {
				if (SiteAnnotation.class == c)
					atoms[i].annotationKeyColix = Graphics3D.getColix(rgb);
				else if (InteractionCenter.class == c) {
					atoms[i].interactionKeyColix = Graphics3D.getColix(rgb);
				}
			}
			break;
		case Attachment.BOND_HOST:
			Bond[] bonds = modelManager.frame.bonds;
			if (i >= 0 && i < modelManager.frame.bondCount) {
				if (SiteAnnotation.class == c)
					bonds[i].annotationKeyColix = Graphics3D.getColix(rgb);
				else if (InteractionCenter.class == c)
					bonds[i].interactionKeyColix = Graphics3D.getColix(rgb);
			}
			break;
		}
	}

	/** XIE */
	public void setAttachment(byte type, int i, boolean b, Class c) {
		if (modelManager.frame == null)
			return;
		switch (type) {
		case Attachment.ATOM_HOST:
			Atom[] atoms = modelManager.frame.atoms;
			if (i >= 0 && i < modelManager.frame.atomCount) {
				if (SiteAnnotation.class == c)
					atoms[i].annotationKey = b;
				else if (InteractionCenter.class == c)
					atoms[i].interactionKey = b;
			}
			break;
		case Attachment.BOND_HOST:
			Bond[] bonds = modelManager.frame.bonds;
			if (i >= 0 && i < modelManager.frame.bondCount) {
				if (SiteAnnotation.class == c)
					bonds[i].annotationKey = b;
				else if (InteractionCenter.class == c)
					bonds[i].interactionKey = b;
			}
			break;
		}
	}

	/** XIE */
	public boolean hasAttachment(byte type, int i, Class c) {
		if (modelManager.frame == null)
			return false;
		switch (type) {
		case Attachment.ATOM_HOST:
			Atom[] atoms = modelManager.frame.atoms;
			if (i >= 0 && i < modelManager.frame.atomCount) {
				if (SiteAnnotation.class == c)
					return atoms[i].annotationKey;
				if (InteractionCenter.class == c)
					return atoms[i].interactionKey;
			}
			break;
		case Attachment.BOND_HOST:
			Bond[] bonds = modelManager.frame.bonds;
			if (i >= 0 && i < modelManager.frame.bondCount) {
				if (SiteAnnotation.class == c)
					return bonds[i].annotationKey;
				if (InteractionCenter.class == c)
					return bonds[i].interactionKey;
			}
			break;
		}
		return false;
	}

	/** XIE */
	public Point getAttachmentScreenCenter(byte type, int i, Class c) {
		if (modelManager.frame == null)
			return new Point();
		switch (type) {
		case Attachment.ATOM_HOST:
			Atom[] atoms = modelManager.frame.atoms;
			if (i >= 0 && i < modelManager.frame.atomCount) {
				if (SiteAnnotation.class == c)
					return atoms[i].pin.handleCenter();
				if (InteractionCenter.class == c)
					return atoms[i].pin.handleCenter();
			}
			break;
		case Attachment.BOND_HOST:
			Bond[] bonds = modelManager.frame.bonds;
			if (i >= 0 && i < modelManager.frame.bondCount) {
				if (SiteAnnotation.class == c)
					return bonds[i].pin.handleCenter();
				if (InteractionCenter.class == c)
					return bonds[i].pin.handleCenter();
			}
			break;
		}
		return new Point();
	}

	/** XIE */
	public void clearKeys(Class c) {
		if (modelManager.frame == null)
			return;
		Atom[] atoms = modelManager.frame.atoms;
		for (int i = 0; i < modelManager.frame.atomCount; i++) {
			if (SiteAnnotation.class == c)
				atoms[i].annotationKey = false;
			else if (InteractionCenter.class == c)
				atoms[i].interactionKey = false;
		}
		Bond[] bonds = modelManager.frame.bonds;
		for (int i = 0; i < modelManager.frame.bondCount; i++) {
			if (SiteAnnotation.class == c)
				bonds[i].annotationKey = false;
			else if (InteractionCenter.class == c)
				bonds[i].interactionKey = false;
		}
	}

	/** XIE */
	public void setDepthCueing(boolean b) {
		if (g3d != null)
			g3d.setDepthCueing(b);
	}

	/** XIE */
	public boolean getDepthCueing() {
		if (g3d == null)
			return false;
		return g3d.getDepthCueing();
	}

	/** XIE: indicate that an atom is clicked but NOT selected. */
	public void setClickedAtom(int i) {
		clickedAtom = i;
	}

	/** XIE: indicate that a bond is clicked. */
	public void setClickedBond(int i) {
		clickedBond = i;
	}

	public void setBondSelected(int i, boolean b) {
		if (modelManager.frame.bondCount <= 0)
			return;
		Bond[] bond = modelManager.frame.bonds;
		if (bond == null)
			return;
		if (i < 0 || i >= bond.length)
			return;
		bond[i].selected = b;
	}

	public void setABondSelected(int i, boolean b) {
		if (modelManager.frame.frameRenderer instanceof ExtendedFrameRenderer) {
			ExtendedFrameRenderer fr = (ExtendedFrameRenderer) modelManager.frame.frameRenderer;
			ABond x = fr.abonds.getABond(i);
			if (x != null)
				x.selected = b;
		}
	}

	public void setTBondSelected(int i, boolean b) {
		if (modelManager.frame.frameRenderer instanceof ExtendedFrameRenderer) {
			ExtendedFrameRenderer fr = (ExtendedFrameRenderer) modelManager.frame.frameRenderer;
			TBond x = fr.tbonds.getTBond(i);
			if (x != null)
				x.selected = b;
		}
	}

	/** XIE */
	public void setZDepthMagnification(int i) {
		transformManager.setZDepthMagnification(i);
	}

	/** XIE */
	public int getZDepthMagnification() {
		return transformManager.getZDepthMagnification();
	}

	/** XIE */
	public void translateByScreenPixels(int dx, int dy, int dz) {
		if (getNavigationMode()) {
			if (transformManager.isRotationCenterOnCamera()) {
				transformManager.translateByScreenPixels(dx, dy, dz);
			}
			else {
				transformManager.translateXYBy(-dx, -dy);
				transformManager.translateByScreenPixels(0, 0, dz);
			}
		}
		else {
			transformManager.translateXYBy(dx, dy);
		}
		refresh();
	}

	/** XIE */
	private void clearEcho() {
		runScriptImmediatelyWithoutThread("echo \"\"");
	}

	/** XIE */
	public void setCameraSpin(boolean b) {
		transformManager.setRotationCenterOnCamera(b);
	}

	/** XIE */
	public boolean isCameraSpin() {
		return transformManager.isRotationCenterOnCamera();
	}

	/** XIE */
	public void moveCameraTo(float timeInSeconds, float axisX, float axisY, float axisZ, float degrees, float navX,
			float navY, float navZ) {
		transformManager.moveCameraTo(timeInSeconds, axisX, axisY, axisZ, degrees, navX, navY, navZ);
	}

	/** XIE: must be run with a thread */
	public void moveCameraToScene(Scene scene, boolean immediately) {
		Scene previous = scene.getPrevious();
		if (previous != null) {
			String s = previous.getDepartInformation();
			if (s != null) {
				runScriptImmediatelyWithoutThread("set echo bottom center;color echo yellow;echo \"" + s + "\"");
			}
			else {
				clearEcho();
			}
			s = previous.getDepartScript();
			if (s != null)
				runScriptImmediatelyWithoutThread(s);
		}
		Vector3f a = scene.getRotationAxis();
		if (getNavigationMode()) {
			Point3f c = scene.getCameraPosition();
			moveCameraTo(immediately ? 0 : scene.getTransitionTime(), a.x, a.y, a.z, scene.getRotationAngle(), c.x,
					c.y, c.z);
		}
		else {
			moveTo(immediately ? 0 : scene.getTransitionTime(), a.x, a.y, a.z, scene.getRotationAngle(), (int) scene
					.getZoomPercent(), scene.getXTrans(), scene.getYTrans());
		}
		String s = scene.getArriveInformation();
		if (s != null) {
			runScriptImmediatelyWithoutThread("set echo bottom center;color echo yellow;echo \"" + s + "\"");
		}
		else {
			clearEcho();
		}
		s = scene.getArriveScript();
		if (s != null)
			runScriptImmediatelyWithoutThread(s);
	}

	/** XIE */
	public void setCameraPosition(float x, float y, float z) {
		transformManager.setCameraPosition(x, y, z);
	}

	/** XIE */
	public Point3f getCameraPosition() {
		return transformManager.getCameraPosition();
	}

	/** XIE v10.2 */
	public void setShowBbcage(boolean showBbcage) {
		setShapeShow(JmolConstants.SHAPE_BBCAGE, showBbcage);
	}

	/** XIE v10.2 */
	public void setShowAxes(boolean showAxes) {
		if (showAxes) {
			if (getAxisStyle() == Axes.OFFSET) {
				setFrankOn(false);
			}
		}
		setShapeShow(JmolConstants.SHAPE_AXES, showAxes);
	}

	/** XIE */
	public Matrix3f getRotationMatrix() {
		return transformManager.getRotationMatrix();
	}

	/** XIE: skeleton method */
	public void setABondRendered(boolean b) {
	}

	/** XIE: skeleton method */
	public void setTBondRendered(boolean b) {
	}

	/** XIE: skeleton method */
	public void clearABonds() {
	}

	/** XIE: skeleton method */
	public void clearTBonds() {
	}

	/** XIE: skeleton method */
	public void setRectangleVisible(boolean b) {
	}

	/** XIE: skeleton method */
	public void setEllipseVisible(boolean b) {
	}

	/** XIE: skeleton method */
	public void setHighlightPlaneVisible(boolean b) {
	}

	/** XIE: skeleton method */
	public void setHighlightCylinderVisible(boolean b) {
	}

	/** XIE: skeleton method */
	public void setHighlightTriangleVisible(boolean b) {
	}

	/** XIE: skeleton method */
	public void setHighlightTriangleVertices(Point3f p1, Point3f p2, Point3f p3) {
	}

	/** XIE: skeleton method */
	public void setHighlightTBondVisible(boolean b) {
	}

	/** XIE: skeleton method */
	public void setHighlightTBond(int i, int j, int k, int l) {
	}

	/** XIE: skeleton method */
	public void setRectangle(char axis, Point3f p1, Point3f p2) {
	}

	/** XIE: skeleton method */
	public void setEllipse(char axis, Point3f p1, Point3f p2) {
	}

	/** XIE: skeleton method */
	public void addCuboidObstacle() {
	}

	/** XIE: skeleton method */
	public void addCylinderObstacle() {
	}

	/** XIE: skeleton method */
	public void addCuboidObstacle(float rx, float ry, float rz, float lx, float ly, float lz) {
	}

	/** XIE: skeleton method */
	public void addCylinderObstacle(float rx, float ry, float rz, char axis, float h, float r) {
	}

	/** XIE: skeleton method */
	public void removeObstacle(int index) {
	}

	/** XIE: skeleton method */
	public void setObstacleLocation(int index, Point3f p) {
	}

	/** skeleton method */
	public void setObstacleGeometry(int index, float rx, float ry, float rz, float lx, float ly, float lz) {
	}

	/** XIE: skeleton method */
	public void setObstacleColor(int index, Color color, boolean translucent) {
	}

	/** XIE */
	public BitSet findAtomsInOval(Rectangle rectangle) {
		return modelManager.findAtomsInOval(rectangle);
	}

	/** XIE: skeleton method */
	public void setCpkPercent(int index, int percentage) {
	}

	/** XIE: skeleton method */
	public void setVdwForceLines(Pair[] pairs) {
	}

	/** XIE: skeleton method */
	public void setTrajectory(int index, int m, float[] x, float[] y, float[] z) {
	}

	/** XIE: skeleton method */
	public void setTrajectoryBitSet(BitSet bs) {
	}

	/** XIE: skeleton method */
	public void setVelocityBitSet(BitSet bs) {
	}

	/** XIE: skeleton method */
	public void setVelocityVectorScalingFactor(short s) {
	}

	/** XIE: skeleton method */
	public void setIndexOfAtomOfSelectedVelocity(int i) {
	}

	/** XIE: skeleton method */
	public void setTranslucentBitSet(BitSet bs) {
	}

	/** XIE: skeleton method */
	public void setHidenBitSet(BitSet bs) {
	}

	/** XIE: skeleton method */
	public void setShowAtomIndex(boolean b) {
	}

	/** XIE: skeleton method */
	public void setShowCharge(boolean b) {
	}

	/** XIE: skeleton method */
	public void setSimulationBoxVisible(boolean b) {
	}

	/** XIE: skeleton method */
	public void setSimulationBox(float xlen, float ylen, float zlen) {
	}

	/** XIE: skeleton method */
	public byte getSimulationBoxFace(int x, int y) {
		return -1;
	}

	/** XIE: skeleton method */
	public void updateSimulationBoxFace(byte face) {
	}

	/** XIE: skeleton method */
	public byte getVectorBoxFace(int x, int y) {
		return -1;
	}

	/** XIE: skeleton method */
	public void updateVectorBoxFace(byte face) {
	}

	/** XIE: skeleton method */
	public void updateCuboidObstacleFace(char axis, float center, float corner) {
	}

	/** XIE: skeleton method */
	public void updateCylinderObstacleFace(char axis, float center, float a, float b, float height) {
	}

	/** XIE: return the index of the clicked bond that is closest to the passed screen position. */
	public int findNearestBondIndex(int x, int y) {
		if (modelManager.frame == null)
			return -1;
		Bond[] bonds = modelManager.frame.bonds;
		if (bonds == null)
			return -1;
		int bondCount = modelManager.frame.bondCount;
		if (bondCount <= 0)
			return -1;
		int xA, yA, zA, xB, yB, zB;
		double distSq;
		float width;
		Atom atom1, atom2;
		int zmin = Integer.MAX_VALUE;
		int zDepth;
		int foundIndex = -1;
		for (int i = 0; i < bondCount; i++) {
			atom1 = bonds[i].atom1;
			atom2 = bonds[i].atom2;
			xA = atom1.screenX;
			yA = atom1.screenY;
			zA = atom1.screenZ;
			xB = atom2.screenX;
			yB = atom2.screenY;
			zB = atom2.screenZ;
			distSq = Line2D.ptSegDistSq(xA, yA, xB, yB, x, y);
			if (distSq < 0.001) {
				zDepth = zA + zB;
				if (zDepth < zmin) {
					zmin = zDepth;
					foundIndex = i;
				}
			}
			else {
				width = scaleToScreen((zA + zB) >> 1, bonds[i].mad);
				if (distSq < 2 * width) {
					zDepth = zA + zB;
					if (zDepth < zmin) {
						zmin = zDepth;
						foundIndex = i;
					}
				}
			}
		}
		return foundIndex;
	}

	/** XIE: skeleton method */
	public int findNearestABondIndex(int x, int y) {
		return -1;
	}

	/** XIE: skeleton method */
	public int findNearestTBondIndex(int x, int y) {
		return -1;
	}

	/** XIE: skeleton method */
	public short[] findNearestObstacleIndexAndFace(int x, int y) {
		return null;
	}

	/** XIE: skeleton method */
	public Point3f findPointOnDropPlane(char axis, int x, int y) {
		return null;
	}

	/** XIE: skeleton method */
	public Point3f findPointOnPlane(char axis, int x, int y, float c) {
		return null;
	}

	/** XIE: skeleton method */
	public void setDropPlaneVisible(char axis, boolean visible) {
	}

	/** XIE: skeleton method */
	public void translateDropPlane(float dx, float dy, float dz) {
	}

	/** XIE: skeleton method */
	public void moveDropPlaneTo(char axis, float coordinate) {
	}

	/** XIE: skeleton method */
	public float getDropPlanePosition(char axis) {
		return 0;
	}

	/** XIE: skeleton method */
	public int findNearestAtomIndexOnDropPlane(char axis, int x, int y) {
		return -1;
	}

	/** XIE */
	public void setHoverEnabled(boolean b) {
		mouseManager.setHoverWatcherEnabled(b);
	}

	public boolean isHoverEnabled() {
		return mouseManager.isHoverWatcherEnabled();
	}

	/** XIE */
	public void setMeasurementEnabled(boolean b) {
		mouseManager.setMeasurementEnabled(b);
	}

	/** XIE */
	public void clearBounds() {
		// modelManager.getFrame().clearBounds();
	}

	/** XIE */
	public void resetDefaultAtomColors() {
		Atom[] atoms = modelManager.frame.atoms;
		for (int i = 0; i < modelManager.frame.atomCount; i++) {
			atoms[i].colixAtom = getColixAtom(atoms[i]);
		}
	}

	/** XIE: skeleton method */
	public void setAxisDiameter(int i) {
	}

	// XIE
	private byte axisStyle = Axes.CENTERED;

	/** XIE */
	public void setAxisStyle(byte style) {
		axisStyle = style;
		if (axisStyle == Axes.OFFSET)
			setFrankOn(false);
	}

	/** XIE */
	public byte getAxisStyle() {
		return axisStyle;
	}

	/** XIE */
	public void fit2DScreen(float pixelsPerAngstrom) {
		transformManager.fit2DScreen(pixelsPerAngstrom);
	}

	/** XIE */
	public void setCenter(float x, float y, float z) {
		transformManager.setCenter("absolute", new Point3f(x, y, z));
	}

	/** XIE */
	public void removeAtoms(BitSet bs) {
		Frame frame = modelManager.frame;
		if (frame == null)
			return;
		frame.removeAtoms(bs);
	}

	/** XIE: setLabel(null); */
	public void removeAll() {
		setLabel(null);
		zap(); // this releases the memory
	}

	/** XIE */
	public String getCurrentOrientation() {
		return transformManager.getCurrentOrientation();
	}

	/** XIE */
	public Point3f getCurrentRotationXyz() {
		return transformManager.getRotationXyz();
	}

	/** XIE */
	public void setShowRebondTime(boolean b) {
		Frame.showRebondTimes = b;
	}

	/** XIE */
	public Point3i getAtomScreen(int index) {
		if (index < 0 || index >= modelManager.frame.atomCount)
			return null;
		Atom a = modelManager.frame.atoms[index];
		return new Point3i(a.screenX, a.screenY, a.screenZ);
	}

	/** XIE */
	public Point3i getBondCenterScreen(int index) {
		if (index < 0 || index >= modelManager.frame.bondCount)
			return null;
		Bond b = modelManager.frame.bonds[index];
		return new Point3i((b.atom1.screenX + b.atom2.screenX) >> 1, (b.atom1.screenY + b.atom2.screenY) >> 1,
				(b.atom1.screenZ + b.atom2.screenZ) >> 1);
	}

	/** XIE */
	public int[] getBondAtoms(int index) {
		if (index < 0 || index >= modelManager.frame.bondCount)
			return null;
		Bond b = modelManager.frame.bonds[index];
		return new int[] { b.atom1.atomIndex, b.atom2.atomIndex };
	}

	/** XIE: skeleton method */
	public void setAtomVelocities(int index, float vx, float vy, float vz) {
	}

	/** set the specified atom's coordinate and color (used for, e.g. kinetic energy shading) */
	public void setAtomCoordinates(int index, float x, float y, float z, int argb) {
		Frame frame = modelManager.frame;
		if (frame == null)
			return;
		Atom atom = frame.getAtomAt(index);
		atom.x = x;
		atom.y = y;
		atom.z = z;
		atom.colixAtom = Graphics3D.getColix(argb);
	}

	public void setAtomCoordinates(int index, float x, float y, float z) {
		Frame frame = modelManager.frame;
		if (frame == null)
			return;
		Atom atom = frame.getAtomAt(index);
		if (atom != null) {
			atom.x = x;
			atom.y = y;
			atom.z = z;
		}
	}

	public void setAtomCoordinates(int index, Point3f p) {
		Frame frame = modelManager.frame;
		if (frame == null)
			return;
		Atom atom = frame.getAtomAt(index);
		if (atom != null)
			atom.set(p);
	}

	/** set the specified atom's coordinates, size and color */
	public void setAtomCoordinates(int index, float x, float y, float z, float d, int argb) {
		Frame frame = modelManager.frame;
		if (frame == null)
			return;
		Atom atom = frame.getAtomAt(index);
		if (atom != null) {
			atom.x = x;
			atom.y = y;
			atom.z = z;
			atom.sigma = (short) d;
			atom.colixAtom = Graphics3D.getColix(argb);
		}
	}

	// FIXME: doesn't work yet
	public void setAtomType(int index, short element, String symbol) {
		Frame frame = modelManager.frame;
		if (frame == null)
			return;
		Atom atom = frame.getAtomAt(index);
		if (atom != null) {
			atom.setAtomicAndIsotopeNumber(element);
			frame.atomNames[index] = symbol.intern();
		}
	}

	/** set the specified atom's size */
	public void setAtomSize(int index, float d) {
		Frame frame = modelManager.frame;
		if (frame == null)
			return;
		Atom atom = frame.getAtomAt(index);
		if (atom != null)
			atom.sigma = (short) d;
	}

	/** set the specified atom's color */
	public void setAtomColor(int index, int argb) {
		Frame frame = modelManager.frame;
		if (frame == null)
			return;
		Atom atom = frame.getAtomAt(index);
		if (atom != null)
			atom.colixCustom = Graphics3D.getColix(argb);
	}

	/** set the specified atom's visibility */
	public void setAtomVisibility(int index, boolean b) {
		Frame frame = modelManager.frame;
		if (frame == null)
			return;
		Atom atom = frame.getAtomAt(index);
		if (atom != null)
			atom.visible = b;
	}

	/** set the specified radial bond's visibility */
	public void setBondVisibility(int index, boolean b) {
		Frame frame = modelManager.frame;
		if (frame == null)
			return;
		Bond bond = frame.getBondAt(index);
		if (bond != null)
			bond.visible = b;
	}

	/**
	 * XIE: This method is used when the data is read from a client object, instead of a file on disk. Since this
	 * involves no job from Eval, Eval should not be used at all. This fixes the bugs with Compiler.
	 */
	public void openClientObject(Object clientObject) {
		pushHoldRepaint();
		modelManager.setClientFile("", "", modelAdapter, clientObject);
		// Watch out this: Eval uses a separate thread to execute scripts. If using the original method,
		// homePosition(), which uses Eval.reset(), it will cause a race condition here.
		reset();
		if (!(this instanceof ExtendedViewer))
			selectAll();
		popHoldRepaint();
		// flag must be set to -1 in order for the status to be fired
		setStatusFileLoaded(-1, "client object", "", modelManager.getModelSetName(), null, null);
		// System.out.println("Viewer:openClientObject - "+Thread.currentThread());
	}

	public void addAtom(Object atomUid, short atomicNumber, String atomName, int formalCharge, float partialCharge,
			float x, float y, float z, float vx, float vy, float vz, Object clientObject) {
		if (modelManager.frame == null)
			modelManager.frame = new Frame(this, modelAdapter, clientObject);
		modelManager.frame.addAtom(atomUid, atomicNumber, atomName, formalCharge, partialCharge, x, y, z, vx, vy, vz,
				clientObject);
	}

	public void addRBond(Object atomUid1, Object atomUid2) {
		if (modelManager.frame != null)
			modelManager.frame.bondAtoms(atomUid1, atomUid2, (short) 1);
	}

	/** XIE: skeleton method */
	public void removeRBond(int index) {
	}

	/** XIE */
	public void deleteAllBonds() {
		if (modelManager.frame != null)
			modelManager.frame.deleteAllBonds();
	}

	/** XIE: skeleton method */
	public void removeABond(int index) {
	}

	/** XIE: skeleton method */
	public void addABond(int i, int j, int k) {
	}

	/** XIE: skeleton method */
	public void removeTBond(int index) {
	}

	/** XIE: skeleton method */
	public void addTBond(int i, int j, int k, int l) {
	}

	/** XIE: skeleton method */
	public void setCharge(int index, float charge) {
	}

	/** XIE */
	public Point getTranslationCenter() {
		return new Point((int) transformManager.xFixedTranslation, (int) transformManager.yFixedTranslation);
	}

	/** XIE */
	public void translateTo(int x, int y) {
		transformManager.translateCenterTo(x, y);
		refresh();
	}

	/** XIE */
	public void translateBy(int dx, int dy) {
		transformManager.translateXYBy(dx, dy);
		refresh();
	}

	/** XIE */
	public void setZoomPercent(int i) {
		zoomToPercent(i);
	}

	/** XIE */
	public void moveTo(float seconds, float ax, float ay, float az, float deg, int zoom, float dx, float dy) {
		transformManager.myMoveTo(seconds, ax, ay, az, deg, zoom, dx, dy);
	}

	/** XIE */
	public void stopMotion(boolean b) {
		transformManager.stopMotion(b);
	}

	/** XIE */
	public void rotateXBy(float x) {
		transformManager.rotateXRadians(x);
		refresh();
	}

	/** XIE */
	public void rotateYBy(float x) {
		transformManager.rotateYRadians(x);
		refresh();
	}

	/** XIE */
	public void rotateZBy(float x) {
		transformManager.rotateZRadians(x);
		refresh();
	}

	public void setAppletContext(String htmlName, URL documentBase, URL codeBase, String appletProxyOrCommandOptions) {
		this.htmlName = htmlName;
		isApplet = (documentBase != null);
		String str = appletProxyOrCommandOptions;
		if (!isApplet) {
			// not an applet -- used to pass along command line options
			if (str.indexOf("-i") >= 0) {
				setLogLevel(3); // no info, but warnings and errors
				isSilent = true;
			}
			if (str.indexOf("-x") >= 0) {
				autoExit = true;
			}
			if (str.indexOf("-n") >= 0) {
				haveDisplay = false;
			}
			writeInfo = null;
			if (str.indexOf("-w") >= 0) {
				int i = str.indexOf("\1");
				int j = str.lastIndexOf("\1");
				writeInfo = str.substring(i + 1, j);
			}
			mustRender = (haveDisplay || writeInfo != null);
		}

		/*
		 * Logger.info("jvm11orGreater=" + jvm11orGreater + "\njvm12orGreater=" + jvm12orGreater + "\njvm14orGreater=" +
		 * jvm14orGreater);
		 */
		if (!isSilent) {
			Logger.info(JmolConstants.copyright + "\nJmol Version " + getJmolVersion() + "\njava.vendor:"
					+ strJavaVendor + "\njava.version:" + strJavaVersion + "\nos.name:" + strOSName + "\n" + htmlName);
		}

		if (isApplet)
			fileManager.setAppletContext(documentBase, codeBase, appletProxyOrCommandOptions);
		zap(); // here to allow echos
	}

	String getHtmlName() {
		return htmlName;
	}

	boolean mustRenderFlag() {
		return mustRender && refreshing;
	}

	static void setLogLevel(int ilevel) {
		for (int i = Logger.NB_LEVELS; --i >= 0;)
			Logger.setActiveLevel(i, (Logger.NB_LEVELS - i) <= ilevel);
	}

	public Component getAwtComponent() {
		return display;
	}

	public boolean handleOldJvm10Event(Event e) {
		return mouseManager.handleOldJvm10Event(e);
	}

	void reset() {
		// Eval.reset()
		// initializeModel
		transformManager.homePosition();
		if (modelManager.modelsHaveSymmetry())
			stateManager.setCrystallographicDefaults();
		refresh(1, "Viewer:homePosition()");
	}

	public void homePosition() {
		script("reset");
	}

	final Hashtable imageCache = new Hashtable();

	void flushCachedImages() {
		imageCache.clear();
		colorManager.flushCachedColors();
	}

	Hashtable getAppletInfo() {
		Hashtable<String, String> info = new Hashtable<String, String>();
		info.put("htmlName", htmlName);
		info.put("version", JmolConstants.version);
		info.put("date", JmolConstants.date);
		info.put("javaVendor", strJavaVendor);
		info.put("javaVersion", strJavaVersion);
		info.put("operatingSystem", strOSName);
		return info;
	}

	String getJmolVersion() {
		return JmolConstants.version + "  " + JmolConstants.date;
	}

	// ///////////////////////////////////////////////////////////////
	// delegated to StateManager
	// ///////////////////////////////////////////////////////////////

	void initialize() {
		resetAllParameters();
	}

	void resetAllParameters() {
		global = stateManager.getGlobalSettings();
		colorManager.resetElementColors();
	}

	String listSavedStates() {
		return stateManager.listSavedStates();
	}

	void saveOrientation(String saveName) {
		// from Eval
		stateManager.saveOrientation(saveName);
	}

	boolean restoreOrientation(String saveName, float timeSeconds) {
		// from Eval
		return stateManager.restoreOrientation(saveName, timeSeconds);
	}

	void saveBonds(String saveName) {
		// from Eval
		stateManager.saveBonds(saveName);
	}

	boolean restoreBonds(String saveName) {
		// from Eval
		return stateManager.restoreBonds(saveName);
	}

	void saveState(String saveName) {
		// from Eval
		stateManager.saveState(saveName);
	}

	String getSavedState(String saveName) {
		return stateManager.getSavedState(saveName);
	}

	boolean restoreState(String saveName) {
		// from Eval
		return stateManager.restoreState(saveName);
	}

	void saveSelection(String saveName) {
		// from Eval
		stateManager.saveSelection(saveName, selectionManager.bsSelection);
		stateManager.restoreSelection(saveName); // just to register the # of selected atoms
	}

	boolean restoreSelection(String saveName) {
		// from Eval
		return stateManager.restoreSelection(saveName);
	}

	// ///////////////////////////////////////////////////////////////
	// delegated to TransformManager
	// ///////////////////////////////////////////////////////////////

	public float getRotationRadius() {
		return transformManager.getRotationRadius();
	}

	// XIE: made public
	public Point3f getRotationCenter() {
		return transformManager.getRotationCenter();
	}

	void setCenter(String relativeTo, Point3f pt) {
		// Eval ???
		transformManager.setCenter(relativeTo, pt);
		refresh(0, "Viewer:setCenter(" + relativeTo + ")");
	}

	void setCenterBitSet(BitSet bsCenter, boolean doScale) {
		// Eval ???
		// setCenterSelected
		transformManager.setCenterBitSet(bsCenter, doScale);
		refresh(0, "Viewer:setCenterBitSet()");
	}

	void setNewRotationCenter(String axisID) {
		// eval for center [line1] ???
		Point3f center = getDrawObjectCenter(axisID);
		if (center == null)
			return;
		setNewRotationCenter(center);
	}

	void setNewRotationCenter(Point3f center) {
		// eval ???
		transformManager.setNewRotationCenter(center, true);
		refresh(0, "Viewer:setCenterBitSet()");
	}

	void move(Vector3f dRot, int dZoom, Vector3f dTrans, int dSlab, float floatSecondsTotal, int fps) {
		// from Eval
		transformManager.move(dRot, dZoom, dTrans, dSlab, floatSecondsTotal, fps);
	}

	public void moveTo(float floatSecondsTotal, Point3f center, Point3f pt, float degrees, float zoom, float xTrans,
			float yTrans, float rotationRadius) {
		// from Eval
		transformManager.moveTo(floatSecondsTotal, center, pt, degrees, zoom, xTrans, yTrans, rotationRadius);
	}

	public void moveTo(float floatSecondsTotal, Matrix3f rotationMatrix, Point3f center, float zoom, float xTrans,
			float yTrans, float rotationRadius) {
		// from StateManager
		transformManager.moveTo(floatSecondsTotal, rotationMatrix, center, zoom, xTrans, yTrans, rotationRadius);
	}

	String getMoveToText(float timespan) {
		return transformManager.getMoveToText(timespan);
	}

	void rotateXYBy(int xDelta, int yDelta) {
		// mouseSinglePressDrag
		transformManager.rotateXYBy(xDelta, yDelta);
		refresh(1, "Viewer:rotateXYBy()");
	}

	void rotateZBy(int zDelta) {
		// mouseSinglePressDrag
		transformManager.rotateZBy(zDelta);
		refresh(1, "Viewer:rotateZBy()");
	}

	public void rotateFront() {
		// deprecated
		transformManager.rotateFront();
		refresh(1, "Viewer:rotateFront()");
	}

	public void rotateToX(float angleRadians) {
		// deprecated
		transformManager.rotateToX(angleRadians);
		refresh(1, "Viewer:rotateToX()");
	}

	public void rotateToY(float angleRadians) {
		// deprecated
		transformManager.rotateToY(angleRadians);
		refresh(1, "Viewer:rotateToY()");
	}

	public void rotateToZ(float angleRadians) {
		// deprecated
		transformManager.rotateToZ(angleRadians);
		refresh(1, "Viewer:rotateToZ()");
	}

	final static float radiansPerDegree = (float) (2 * Math.PI / 360);

	public void rotateToX(int angleDegrees) {
		// deprecated
		rotateToX(angleDegrees * radiansPerDegree);
	}

	public void rotateToY(int angleDegrees) {
		// deprecated
		rotateToY(angleDegrees * radiansPerDegree);
	}

	void translateXYBy(int xDelta, int yDelta) {
		// mouseDoublePressDrag, mouseSinglePressDrag
		transformManager.translateXYBy(xDelta, yDelta);
		refresh(1, "Viewer:translateXYBy()");
	}

	public void translateToXPercent(float percent) {
		// Eval.translate()
		transformManager.translateToXPercent(percent);
		refresh(1, "Viewer:translateToXPercent()");
	}

	public void translateToYPercent(float percent) {
		// Eval.translate()
		transformManager.translateToYPercent(percent);
		refresh(1, "Viewer:translateToYPercent()");
	}

	void translateToZPercent(float percent) {
		transformManager.translateToZPercent(percent);
		refresh(1, "Viewer:translateToZPercent()");
		// Eval.translate()
	}

	public float getTranslationXPercent() {
		return transformManager.getTranslationXPercent();
	}

	public float getTranslationYPercent() {
		return transformManager.getTranslationYPercent();
	}

	float getTranslationZPercent() {
		return transformManager.getTranslationZPercent();
	}

	String getTranslationScript() {
		return transformManager.getTranslationScript();
	}

	void zoomBy(int pixels) {
		// MouseManager.mouseSinglePressDrag
		transformManager.zoomBy(pixels);
		refresh(1, "Viewer:zoomBy()");
	}

	public int getZoomPercent() {
		return transformManager.getZoomPercent();
	}

	float getZoomPercentFloat() {
		return transformManager.getZoomPercentFloat();
	}

	float getZoomPercentSetting() {
		return transformManager.getZoomPercentSetting();
	}

	float getMaxZoomPercent() {
		return TransformManager.MAXIMUM_ZOOM_PERCENTAGE;
	}

	void zoomToPercent(float percent) {
		transformManager.zoomToPercent(percent);
		refresh(1, "Viewer:zoomToPercent()");
	}

	void zoomByPercent(int percent) {
		// Eval.zoom
		// MouseManager.mouseWheel
		// stateManager.setCommonDefaults
		transformManager.zoomByPercent(percent);
		refresh(1, "Viewer:zoomByPercent()");
	}

	private void setZoomEnabled(boolean zoomEnabled) {
		transformManager.setZoomEnabled(zoomEnabled);
		refresh(1, "Viewer:setZoomEnabled()");
	}

	boolean getZoomEnabled() {
		return transformManager.zoomEnabled;
	}

	boolean getSlabEnabled() {
		return transformManager.slabEnabled;
	}

	int getSlabPercentSetting() {
		return transformManager.slabPercentSetting;
	}

	void slabByPixels(int pixels) {
		// MouseManager.mouseSinglePressDrag
		transformManager.slabByPercentagePoints(pixels);
		refresh(0, "Viewer:slabByPixels()");
	}

	void depthByPixels(int pixels) {
		// MouseManager.mouseDoublePressDrag
		transformManager.depthByPercentagePoints(pixels);
		refresh(0, "Viewer:depthByPixels()");
	}

	void slabDepthByPixels(int pixels) {
		// MouseManager.mouseSinglePressDrag
		transformManager.slabDepthByPercentagePoints(pixels);
		refresh(0, "Viewer:slabDepthByPixels()");
	}

	void slabToPercent(int percentSlab) {
		// Eval.slab
		transformManager.slabToPercent(percentSlab);
		refresh(0, "Viewer:slabToPercent()");
	}

	void depthToPercent(int percentDepth) {
		// Eval.depth
		transformManager.depthToPercent(percentDepth);
		refresh(0, "Viewer:depthToPercent()");
	}

	private void setSlabEnabled(boolean slabEnabled) {
		// Eval.slab
		transformManager.setSlabEnabled(slabEnabled);
		refresh(0, "Viewer:setSlabEnabled()");
	}

	public Matrix4f getUnscaledTransformMatrix() {
		return transformManager.getUnscaledTransformMatrix();
	}

	void finalizeTransformParameters() {
		// FrameRenderer
		transformManager.finalizeTransformParameters();
		g3d.setSlabAndDepthValues(transformManager.slabValue, transformManager.depthValue);
	}

	Point3i transformPoint(Point3f pointAngstroms) {
		return transformManager.transformPoint(pointAngstroms);
	}

	Point3i transformPoint(Point3f pointAngstroms, Vector3f vibrationVector) {
		return transformManager.transformPoint(pointAngstroms, vibrationVector);
	}

	void transformPoint(Point3f pointAngstroms, Vector3f vibrationVector, Point3i pointScreen) {
		transformManager.transformPoint(pointAngstroms, vibrationVector, pointScreen);
	}

	void transformPoint(Point3f pointAngstroms, Point3i pointScreen) {
		transformManager.transformPoint(pointAngstroms, pointScreen);
	}

	void transformPoint(Point3f pointAngstroms, Point3f pointScreen) {
		transformManager.transformPoint(pointAngstroms, pointScreen);
	}

	void transformPoints(Point3f[] pointsAngstroms, Point3i[] pointsScreens) {
		transformManager.transformPoints(pointsAngstroms.length, pointsAngstroms, pointsScreens);
	}

	void transformVector(Vector3f vectorAngstroms, Vector3f vectorTransformed) {
		transformManager.transformVector(vectorAngstroms, vectorTransformed);
	}

	void unTransformPoint(Point3i pointScreen, Point3f pointAngstroms) {
		transformManager.unTransformPoint(pointScreen, pointAngstroms);
	}

	float getScalePixelsPerAngstrom() {
		return transformManager.scalePixelsPerAngstrom;
	}

	float scaleToScreen(int z, float sizeAngstroms) {
		// Sticks renderer
		return transformManager.scaleToScreen(z, sizeAngstroms);
	}

	short scaleToScreen(int z, int milliAngstroms) {
		// all shapes
		return transformManager.scaleToScreen(z, milliAngstroms);
	}

	float scaleToPerspective(int z, float sizeAngstroms) {
		// DotsRenderer
		return transformManager.scaleToPerspective(z, sizeAngstroms);
	}

	void scaleFitToScreen() {
		// setCenter
		transformManager.scaleFitToScreen();
	}

	private void setScaleAngstromsPerInch(float angstromsPerInch) {
		// Eval.setScale3d
		transformManager.setScaleAngstromsPerInch(angstromsPerInch);
	}

	void setSpinX(int value) {
		// Eval
		transformManager.setSpinX(value);
	}

	float getSpinX() {
		return transformManager.spinX;
	}

	void setSpinY(int value) {
		// Eval
		transformManager.setSpinY(value);
	}

	float getSpinY() {
		return transformManager.spinY;
	}

	void setSpinZ(int value) {
		// Eval
		transformManager.setSpinZ(value);
	}

	float getSpinZ() {
		return transformManager.spinZ;
	}

	void setSpinFps(int value) {
		// Eval
		transformManager.setSpinFps(value);
	}

	float getSpinFps() {
		return transformManager.spinFps;
	}

	public void setSpinOn(boolean spinOn) {
		// Eval
		// startSpinningAxis
		transformManager.setSpinOn(spinOn);
	}

	// XIE: made public
	public boolean getSpinOn() {
		return transformManager.spinOn;
	}

	String getOrientationText() {
		return transformManager.getOrientationText();
	}

	Hashtable getOrientationInfo() {
		return transformManager.getOrientationInfo();
	}

	Matrix3f getMatrixRotate() {
		return transformManager.getMatrixRotate();
	}

	void getAxisAngle(AxisAngle4f axisAngle) {
		transformManager.getAxisAngle(axisAngle);
	}

	String getTransformText() {
		return transformManager.getTransformText();
	}

	void getRotation(Matrix3f matrixRotation) {
		transformManager.getRotation(matrixRotation);
	}

	// ///////////////////////////////////////////////////////////////
	// delegated to ColorManager
	// ///////////////////////////////////////////////////////////////

	private void setDefaultColors(String colorScheme) {
		colorManager.setDefaultColors(colorScheme);
	}

	int getColixArgb(short colix) {
		return g3d.getColixArgb(colix);
	}

	void setRubberbandArgb(int argb) {
		// Eval
		colorManager.setRubberbandArgb(argb);
	}

	short getColixRubberband() {
		return colorManager.colixRubberband;
	}

	void setElementArgb(int elementNumber, int argb) {
		// Eval
		global.setParameterValue("_color " + JmolConstants.elementNameFromNumber(elementNumber), StateManager
				.escapeColor(argb));
		colorManager.setElementArgb(elementNumber, argb);
	}

	float getDefaultVectorScale() {
		return global.defaultVectorScale;
	}

	private void setDefaultVectorScale(float scale) {
		global.defaultVectorScale = scale;
	}

	float getDefaultVibrationScale() {
		return global.defaultVibrationScale;
	}

	private void setDefaultVibrationScale(float scale) {
		global.defaultVibrationScale = scale;
	}

	float getDefaultVibrationPeriod() {
		return global.defaultVibrationPeriod;
	}

	private void setDefaultVibrationPeriod(float period) {
		global.defaultVibrationPeriod = period;
	}

	private void setVibrationScale(float scale) {
		// Eval
		transformManager.setVibrationScale(scale);
	}

	private void setVibrationPeriod(float period) {
		// Eval
		transformManager.setVibrationPeriod(period);
	}

	public void setBackgroundArgb(int argb) {
		// Eval
		global.argbBackground = argb;
		g3d.setBackgroundArgb(argb);
		colorManager.setColixBackgroundContrast(argb);
	}

	public int getBackgroundArgb() {
		return global.argbBackground;
	}

	private void setColorBackground(String colorName) {
		if (colorName != null && colorName.length() > 0)
			setBackgroundArgb(Graphics3D.getArgbFromString(colorName));
	}

	short getColixBackgroundContrast() {
		return colorManager.colixBackgroundContrast;
	}

	int getArgbFromString(String colorName) {
		return Graphics3D.getArgbFromString(colorName);
	}

	private void setSpecular(boolean specular) {
		// Eval
		colorManager.setSpecular(specular);
	}

	boolean getSpecular() {
		return colorManager.getSpecular();
	}

	private void setSpecularPower(int specularPower) {
		// Eval
		colorManager.setSpecularPower(specularPower);
	}

	private void setAmbientPercent(int ambientPercent) {
		// Eval
		colorManager.setAmbientPercent(ambientPercent);
	}

	private void setDiffusePercent(int diffusePercent) {
		// Eval
		colorManager.setDiffusePercent(diffusePercent);
	}

	private void setSpecularPercent(int specularPercent) {
		// Eval
		colorManager.setSpecularPercent(specularPercent);
	}

	// XIE
	short getColixAtom(Atom atom) {
		return colorManager.getColixAtomPalette(atom, JmolConstants.PALETTE_CPK);
	}

	short getColixAtomPalette(Atom atom, byte pid) {
		return colorManager.getColixAtomPalette(atom, pid);
	}

	short getColixHbondType(short order) {
		return colorManager.getColixHbondType(order);
	}

	short getColixFromPalette(float val, float rangeMin, float rangeMax, String palette) {
		return colorManager.getColixFromPalette(val, rangeMin, rangeMax, palette);
	}

	// ///////////////////////////////////////////////////////////////
	// delegated to SelectionManager
	// ///////////////////////////////////////////////////////////////

	void select(BitSet bs, boolean isQuiet) {
		// Eval
		selectionManager.select(bs, isQuiet);
	}

	void selectBonds(BitSet bs) {
		selectionManager.selectBonds(bs);
	}

	boolean isBondSelection() {
		return !selectionManager.selectionModeAtoms;
	}

	BitSet getSelectedAtomsOrBonds() {
		return selectionManager.getSelectedAtomsOrBonds();
	}

	BitSet getSelectedBonds() {
		return selectionManager.bsBonds;
	}

	void hide(BitSet bs, boolean isQuiet) {
		// Eval
		selectionManager.hide(bs, isQuiet);
	}

	void display(BitSet bsAll, BitSet bs, boolean isQuiet) {
		// Eval
		selectionManager.display(bsAll, bs, isQuiet);
	}

	BitSet getHiddenSet() {
		return selectionManager.getHiddenSet();
	}

	boolean isSelected(int atomIndex) {
		return selectionManager.isSelected(atomIndex);
	}

	boolean isInSelectionSubset(int atomIndex) {
		return selectionManager.isInSelectionSubset(atomIndex);
	}

	void reportSelection(String msg) {
		if (modelManager.getSelectionHaloEnabled())
			setTainted(true);
		scriptStatus(msg);
	}

	public void selectAll() {
		// initializeModel
		selectionManager.selectAll();
		refresh(0, "Viewer:selectAll()");
	}

	public void clearSelection() {
		// not used in this project; in jmolViewer interface, though
		selectionManager.clearSelection();
		refresh(0, "Viewer:clearSelection()");
	}

	public void setSelectionSet(BitSet set) {
		// not used in this project; in jmolViewer interface, though
		selectionManager.setSelectionSet(set);
		refresh(0, "Viewer:setSelectionSet()");
	}

	void setSelectionSubset(BitSet subset) {
		selectionManager.setSelectionSubset(subset);
	}

	private void setHideNotSelected(boolean TF) {
		selectionManager.setHideNotSelected(TF);
	}

	void invertSelection() {
		// Eval
		selectionManager.invertSelection();
		// only used from a script, so I do not think a refresh() is necessary
	}

	public BitSet getSelectionSet() {
		return selectionManager.bsSelection;
	}

	public int getSelectionCount() {
		return selectionManager.getSelectionCount();
	}

	void setFormalCharges(int formalCharge) {
		modelManager.setFormalCharges(selectionManager.bsSelection, formalCharge);
	}

	public void addSelectionListener(JmolSelectionListener listener) {
		selectionManager.addListener(listener);
	}

	public void removeSelectionListener(JmolSelectionListener listener) {
		selectionManager.addListener(listener);
	}

	BitSet getAtomBitSet(String atomExpression) {
		return selectionManager.getAtomBitSet(atomExpression);
	}

	int firstAtomOf(BitSet bs) {
		return modelManager.firstAtomOf(bs);
	}

	Point3f getAtomSetCenter(BitSet bs) {
		return modelManager.getAtomSetCenter(bs);
	}

	Vector getAtomBitSetVector(String atomExpression) {
		return selectionManager.getAtomBitSetVector(atomExpression);
	}

	// ///////////////////////////////////////////////////////////////
	// delegated to MouseManager
	// ///////////////////////////////////////////////////////////////

	public void setModeMouse(int modeMouse) {
		// call before setting viewer=null
		mouseManager.setModeMouse(modeMouse);
		if (modeMouse == JmolConstants.MOUSE_NONE) {
			// applet is being destroyed
			clearScriptQueue();
			haltScriptExecution();
			// g3d.destroy();
		}
	}

	Rectangle getRubberBandSelection() {
		return mouseManager.getRubberBand();
	}

	int getCursorX() {
		return mouseManager.xCurrent;
	}

	int getCursorY() {
		return mouseManager.yCurrent;
	}

	// ///////////////////////////////////////////////////////////////
	// delegated to FileManager
	// ///////////////////////////////////////////////////////////////

	private void setAppletProxy(String appletProxy) {
		// Eval
		fileManager.setAppletProxy(appletProxy);
	}

	public boolean isApplet() {
		return (htmlName.length() > 0);
	}

	private void setDefaultDirectory(String dir) {
		global.defaultDirectory = (dir == null || dir.length() == 0 ? null : dir);
	}

	String getDefaultDirectory() {
		return global.defaultDirectory;
	}

	Object getInputStreamOrErrorMessageFromName(String name) {
		return fileManager.getInputStreamOrErrorMessageFromName(name);
	}

	Object getUnzippedBufferedReaderOrErrorMessageFromName(String name) {
		return fileManager.getUnzippedBufferedReaderOrErrorMessageFromName(name);
	}

	Object getBufferedReaderForString(String string) {
		return fileManager.getBufferedReaderForString(string);
	}

	public void openFile(String name) {
		// Jmol app file dropper, main, OpenUrlAction, RecentFilesAction
		// app Jmol BYPASSES SCRIPTING **
		openFile(name, null, null);
	}

	void openFile(String name, int[] params, String loadScript) {
		// Eval
		if (name == null)
			return;
		if (name.equalsIgnoreCase("string")) {
			openStringInline(fileManager.inlineData, params);
			return;
		}
		if (name.equalsIgnoreCase("string[]")) {
			openStringInline(fileManager.inlineDataArray, params);
			return;
		}
		zap();
		long timeBegin = System.currentTimeMillis();
		fileManager.openFile(name, params, loadScript);
		long ms = System.currentTimeMillis() - timeBegin;
		setStatusFileLoaded(1, name, "", modelManager.getModelSetName(), null, null);
		String sp = "";
		if (params != null)
			for (int i = 0; i < params.length; i++)
				sp += "," + params[i];
		Logger.info("openFile(" + name + sp + ")" + ms + " ms");
	}

	public void openFiles(String modelName, String[] names) {
		openFiles(modelName, names, null);
	}

	void openFiles(String modelName, String[] names, String loadScript) {
		// Eval
		zap();
		// keep old screen image while new file is being loaded
		// forceRefresh();
		long timeBegin = System.currentTimeMillis();
		fileManager.openFiles(modelName, names, loadScript);
		long ms = System.currentTimeMillis() - timeBegin;
		for (int i = 0; i < names.length; i++) {
			setStatusFileLoaded(1, names[i], "", modelManager.getModelSetName(), null, null);
		}
		Logger.info("openFiles(" + names.length + ") " + ms + " ms");
	}

	public void openStringInline(String strModel) {
		// Jmol app file dropper
		openStringInline(strModel, null);
	}

	private void openStringInline(String strModel, int[] params) {
		// loadInline, openFile, openStringInline
		clear();
		fileManager.openStringInline(strModel, params);
		String errorMsg = getOpenFileError();
		if (errorMsg == null)
			setStatusFileLoaded(1, "string", "", modelManager.getModelSetName(), null, null);
	}

	private void openStringInline(String[] arrayModels, int[] params) {
		// loadInline, openFile, openStringInline
		clear();
		fileManager.openStringInline(arrayModels, params);
		String errorMsg = getOpenFileError();
		if (errorMsg == null)
			setStatusFileLoaded(1, "string[]", "", modelManager.getModelSetName(), null, null);
	}

	public char getInlineChar() {
		return global.inlineNewlineChar;
	}

	public void loadInline(String strModel) {
		// applet Console, loadInline, app PasteClipboard
		loadInline(strModel, global.inlineNewlineChar);
	}

	public void loadInline(String strModel, char newLine) {
		// Eval data
		// loadInline
		if (strModel == null)
			return;
		int i;
		int[] A = global.getDefaultLatticeArray();
		Logger.debug(strModel);
		if (newLine != 0 && newLine != '\n') {
			int len = strModel.length();
			for (i = 0; i < len && strModel.charAt(i) == ' '; ++i) {
			}
			if (i < len && strModel.charAt(i) == newLine)
				strModel = strModel.substring(i + 1);
			strModel = simpleReplace(strModel, "" + newLine, "\n");
		}
		String datasep = (String) global.getParameter("dataseparator");
		if (datasep != null && (i = strModel.indexOf(datasep)) >= 0) {
			int n = 2;
			while ((i = strModel.indexOf(datasep, i + 1)) >= 0)
				n++;
			String[] strModels = new String[n];
			int pt = 0, pt0 = 0;
			for (i = 0; i < n; i++) {
				pt = strModel.indexOf(datasep, pt0);
				if (pt < 0)
					pt = strModel.length();
				strModels[i] = strModel.substring(pt0, pt);
				pt0 = pt + datasep.length();
			}
			openStringInline(strModels, A);
			return;
		}
		openStringInline(strModel, A);
	}

	public void loadInline(String[] arrayModels) {
		// Eval data
		// loadInline
		if (arrayModels == null || arrayModels.length == 0)
			return;
		int[] A = global.getDefaultLatticeArray();
		openStringInline(arrayModels, A);
	}

	public void openDOM(Object DOMNode) {
		// applet.loadDOMNode
		clear();
		long timeBegin = System.currentTimeMillis();
		fileManager.openDOM(DOMNode);
		long ms = System.currentTimeMillis() - timeBegin;
		Logger.info("openDOM " + ms + " ms");
		setStatusFileLoaded(1, "JSNode", "", modelManager.getModelSetName(), null, getOpenFileError());
	}

	/**
	 * Opens the file, given the reader.
	 * 
	 * name is a text name of the file ... to be displayed in the window no need to pass a BufferedReader ... ... the
	 * FileManager will wrap a buffer around it
	 * 
	 * not referenced in this project
	 * 
	 * @param fullPathName
	 * @param name
	 * @param reader
	 */
	public void openReader(String fullPathName, String name, Reader reader) {
		clear();
		fileManager.openReader(fullPathName, name, reader);
		getOpenFileError();
		System.gc();
	}

	/**
	 * misnamed -- really this opens the file, gets the data, and returns error or null
	 * 
	 * @return errorMsg
	 */
	public String getOpenFileError() {
		String fullPathName = getFullPathName();
		String fileName = getFileName();
		Object clientFile = fileManager.waitForClientFileOrErrorMessage();
		if (clientFile instanceof String || clientFile == null) {
			String errorMsg = (String) clientFile;
			setStatusFileNotLoaded(fullPathName, errorMsg);
			if (errorMsg != null) {
				String msg = errorMsg;
				int pt = msg.lastIndexOf("/");
				if (pt > 0)
					msg = msg.substring(0, pt + 1) + '\n' + msg.substring(pt + 1);
				pt = msg.lastIndexOf("\\");
				if (pt > 0)
					msg = msg.substring(0, pt + 1) + '\n' + msg.substring(pt + 1);
				for (int i = 0; i < 2; i++) {
					pt = msg.indexOf(" ");
					if (pt > 0)
						msg = msg.substring(0, pt) + '\n' + msg.substring(pt + 1);
				}
				zap(msg);
			}
			return errorMsg;
		}
		openClientFile(fullPathName, fileName, clientFile);
		return null;
	}

	public void openClientFile(String fullPathName, final String fileName, Object clientFile) {
		// maybe there needs to be a call to clear()
		// or something like that here for when CdkEditBus calls this directly
		setStatusFileLoaded(2, fullPathName, fileName, modelManager.getModelSetName(), clientFile, null);
		pushHoldRepaint();
		modelManager.setClientFile(fullPathName, fileName, modelAdapter, clientFile);
		initializeModel();
		popHoldRepaint();
		setStatusFileLoaded(3, fullPathName, fileName, modelManager.getModelSetName(), clientFile, null);
	}

	public String getCurrentFileAsString() {
		if (getFullPathName() == "string") {
			return fileManager.inlineData;
		}
		if (getFullPathName() == "string[]") {
			int modelIndex = getDisplayModelIndex();
			if (modelIndex < 0)
				return "";
			return fileManager.inlineDataArray[modelIndex];
		}
		if (getFullPathName() == "JSNode") {
			return "<DOM NODE>";
		}
		String pathName = modelManager.getModelSetPathName();
		if (pathName == null)
			return null;
		return fileManager.getFileAsString(pathName);
	}

	public String getFileAsString(String pathName) {
		return fileManager.getFileAsString(pathName);
	}

	public String getFullPathName() {
		return fileManager.getFullPathName();
	}

	public String getFileName() {
		return fileManager.getFileName();
	}

	// ///////////////////////////////////////////////////////////////
	// delegated to ModelManager
	// ///////////////////////////////////////////////////////////////

	void addStateScript(String script) {
		modelManager.addStateScript(script);
	}

	public boolean getEchoStateActive() {
		return modelManager.getEchoStateActive();
	}

	void setEchoStateActive(boolean TF) {
		modelManager.setEchoStateActive(TF);
	}

	public void zap() {
		// Eval
		// setAppletContext
		clear();
		modelManager.zap();
		initializeModel();
	}

	private void zap(String msg) {
		zap();
		echoMessage(msg);
	}

	void echoMessage(String msg) {
		int iShape = JmolConstants.SHAPE_ECHO;
		loadShape(iShape);
		setShapeProperty(iShape, "font", getFont3D("SansSerif", "Plain", 9));
		setShapeProperty(iShape, "target", "error");
		setShapeProperty(iShape, "text", msg);
	}

	void clear() {
		setRefreshing(true); // XIE: move up for refreshing in the first time
		if (modelManager.getFrame() == null)
			return;
		setBooleanProperty("slabEnabled", false);
		fileManager.clear();
		repaintManager.clear();
		transformManager.clear();
		pickingManager.clear();
		selectionManager.clear();
		clearAllMeasurements();
		modelManager.clear();
		statusManager.clear();
		stateManager.clear(global);
		refresh(0, "Viewer:clear()");
		// g3d.destroy();
		System.gc();
	}

	private void initializeModel() {
		reset();
		selectAll();
		transformManager.setCenter();
		if (eval != null)
			eval.clearDefinitionsAndLoadPredefined();
		// there probably needs to be a better startup mechanism for shapes
		if (modelSetHasVibrationVectors())
			setShapeSize(JmolConstants.SHAPE_VECTORS, global.defaultVectorMad);
		setFrankOn(global.frankOn);
		repaintManager.initializePointers(1);
		setDisplayModelIndex(0);
		setBackgroundModelIndex(-1);
		setTainted(true);
	}

	public String getModelSetName() {
		return modelManager.getModelSetName();
	}

	public String getModelSetFileName() {
		return modelManager.getModelSetFileName();
	}

	public String getUnitCellInfoText() {
		return modelManager.getUnitCellInfoText();
	}

	public String getSpaceGroupInfoText(String spaceGroup) {
		return modelManager.getSpaceGroupInfoText(spaceGroup);
	}

	public int getSpaceGroupIndexFromName(String spaceGroup) {
		return modelManager.getSpaceGroupIndexFromName(spaceGroup);
	}

	public String getModelSetProperty(String strProp) {
		return modelManager.getModelSetProperty(strProp);
	}

	public Object getModelSetAuxiliaryInfo(String strKey) {
		return modelManager.getModelSetAuxiliaryInfo(strKey);
	}

	public String getModelSetPathName() {
		return modelManager.getModelSetPathName();
	}

	public String getModelSetTypeName() {
		return modelManager.getModelSetTypeName();
	}

	public boolean haveFrame() {
		return modelManager.frame != null;
	}

	public void calculateStructures() {
		// Eval
		modelManager.calculateStructures();
		addStateScript("calculate structure");
	}

	void clearBfactorRange() {
		// Eval
		modelManager.clearBfactorRange();
	}

	boolean getPrincipalAxes(int atomIndex, Vector3f z, Vector3f x, String lcaoType, boolean hybridizationCompatible) {
		return modelManager.getPrincipalAxes(atomIndex, z, x, lcaoType, hybridizationCompatible);
	}

	BitSet getModelAtomBitSet(int modelIndex) {
		return modelManager.getModelAtomBitSet(modelIndex);
	}

	BitSet getModelBitSet(BitSet atomList) {
		return modelManager.getModelBitSet(atomList);
	}

	Object getClientFile() {
		// DEPRECATED - use getExportJmolAdapter()
		return null;
	}

	// this is a problem. SmarterJmolAdapter doesn't implement this;
	// it can only return null. Do we need it?

	String getClientAtomStringProperty(Object clientAtom, String propertyName) {
		if (modelAdapter == null)
			return null;
		return modelAdapter.getClientAtomStringProperty(clientAtom, propertyName);
	}

	/*******************************************************************************************************************
	 * This is the method that should be used to extract the model data from Jmol. Note that the API provided by
	 * JmolAdapter is used to import data into Jmol and to export data out of Jmol.
	 * 
	 * When exporting, a few of the methods in JmolAdapter do not make sense. openBufferedReader(...) Others may be
	 * implemented in the future, but are not currently all pdb specific things Just pass in null for the methods that
	 * want a clientFile. The main methods to use are getFrameCount(null) -> currently always returns 1
	 * getAtomCount(null, 0) getAtomIterator(null, 0) getBondIterator(null, 0)
	 * 
	 * The AtomIterator and BondIterator return Objects as unique IDs to identify the atoms. atomIterator.getAtomUid()
	 * bondIterator.getAtomUid1() & bondIterator.getAtomUid2() The ExportJmolAdapter will return the 0-based atom index
	 * as a boxed Integer. That means that you can cast the results to get a zero-based atom index int atomIndex =
	 * ((Integer)atomIterator.getAtomUid()).intValue(); ... int bondedAtom1 =
	 * ((Integer)bondIterator.getAtomUid1()).intValue(); int bondedAtom2 =
	 * ((Integer)bondIterator.getAtomUid2()).intValue();
	 * 
	 * post questions to jmol-developers@lists.sf.net
	 * 
	 * @return A JmolAdapter
	 ******************************************************************************************************************/

	JmolAdapter getExportJmolAdapter() {
		return modelManager.getExportJmolAdapter();
	}

	public Frame getFrame() {
		return modelManager.getFrame();
	}

	Point3f getBoundBoxCenter() {
		return modelManager.getBoundBoxCenter();
	}

	Point3f getAverageAtomPoint() {
		return modelManager.getAverageAtomPoint();
	}

	float calcRotationRadius(Point3f center) {
		return modelManager.calcRotationRadius(center);
	}

	Vector3f getBoundBoxCornerVector() {
		return modelManager.getBoundBoxCornerVector();
	}

	Hashtable getBoundBoxInfo() {
		return modelManager.getBoundBoxInfo();
	}

	int getBoundBoxCenterX() {
		// FIXME mth 2003 05 31
		// used by the labelRenderer for rendering labels away from the center
		// for now this is returning the center of the screen
		// need to transform the center of the bounding box and return that point
		return dimScreen.width / 2;
	}

	int getBoundBoxCenterY() {
		return dimScreen.height / 2;
	}

	public int getModelCount() {
		return modelManager.getModelCount();
	}

	String getModelInfoAsString() {
		return modelManager.getModelInfoAsString();
	}

	String getSymmetryInfoAsString() {
		return modelManager.getSymmetryInfoAsString();
	}

	public Properties getModelSetProperties() {
		return modelManager.getModelSetProperties();
	}

	public Hashtable getModelSetAuxiliaryInfo() {
		return modelManager.getModelSetAuxiliaryInfo();
	}

	public int getModelNumber(int modelIndex) {
		return modelManager.getModelNumber(modelIndex);
	}

	public String getModelName(int modelIndex) {
		return modelManager.getModelName(modelIndex);
	}

	public Properties getModelProperties(int modelIndex) {
		return modelManager.getModelProperties(modelIndex);
	}

	public String getModelProperty(int modelIndex, String propertyName) {
		return modelManager.getModelProperty(modelIndex, propertyName);
	}

	public Hashtable getModelAuxiliaryInfo(int modelIndex) {
		return modelManager.getModelAuxiliaryInfo(modelIndex);
	}

	public Object getModelAuxiliaryInfo(int modelIndex, String keyName) {
		return modelManager.getModelAuxiliaryInfo(modelIndex, keyName);
	}

	int getModelNumberIndex(int modelNumber) {
		return modelManager.getModelNumberIndex(modelNumber);
	}

	boolean modelSetHasVibrationVectors() {
		return modelManager.modelSetHasVibrationVectors();
	}

	public boolean modelHasVibrationVectors(int modelIndex) {
		return modelSetHasVibrationVectors() && modelManager.modelHasVibrationVectors(modelIndex);
	}

	public int getChainCount() {
		return modelManager.getChainCount();
	}

	public int getChainCountInModel(int modelIndex) {
		return modelManager.getChainCountInModel(modelIndex);
	}

	public int getGroupCount() {
		return modelManager.getGroupCount();
	}

	public int getGroupCountInModel(int modelIndex) {
		return modelManager.getGroupCountInModel(modelIndex);
	}

	public int getPolymerCount() {
		return modelManager.getPolymerCount();
	}

	public int getPolymerCountInModel(int modelIndex) {
		return modelManager.getPolymerCountInModel(modelIndex);
	}

	public int getAtomCount() {
		return modelManager.getAtomCount();
	}

	public int getAtomCountInModel(int modelIndex) {
		return modelManager.getAtomCountInModel(modelIndex);
	}

	/**
	 * For use in setting a for() construct max value
	 * 
	 * @return used size of the bonds array;
	 */
	public int getBondCount() {
		return modelManager.getBondCount();
	}

	/**
	 * from JmolPopup.udateModelSetComputedMenu
	 * 
	 * @param modelIndex
	 *            the model of interest or -1 for all
	 * @return the actual number of connections
	 */
	public int getBondCountInModel(int modelIndex) {
		return modelManager.getBondCountInModel(modelIndex);
	}

	boolean frankClicked(int x, int y) {
		return modelManager.frankClicked(x, y);
	}

	public int findNearestAtomIndex(int x, int y) {
		return modelManager.findNearestAtomIndex(x, y);
	}

	public BitSet findAtomsInRectangle(Rectangle rectRubberBand) {
		return modelManager.findAtomsInRectangle(rectRubberBand);
	}

	void convertFractionalCoordinates(Point3f pt) {
		int modelIndex = getDisplayModelIndex();
		if (modelIndex < 0)
			return;
		modelManager.convertFractionalCoordinates(modelIndex, pt);
	}

	public void setCenterSelected() {
		// depricated
		script("center (selected)");
	}

	public void rebond() {
		// Eval, PreferencesDialog
		modelManager.rebond();
		refresh(0, "Viewer:rebond()");
	}

	private void setBondTolerance(float bondTolerance) {
		global.bondTolerance = bondTolerance;
	}

	public float getBondTolerance() {
		return global.bondTolerance;
	}

	private void setMinBondDistance(float minBondDistance) {
		// PreferencesDialog
		global.minBondDistance = minBondDistance;
	}

	public float getMinBondDistance() {
		return global.minBondDistance;
	}

	BitSet getAtomBits(String setType) {
		return modelManager.getAtomBits(setType);
	}

	BitSet getAtomBits(String setType, String specInfo) {
		return modelManager.getAtomBits(setType, specInfo);
	}

	BitSet getAtomBits(String setType, int specInfo) {
		return modelManager.getAtomBits(setType, specInfo);
	}

	BitSet getAtomBits(String setType, int[] specInfo) {
		return modelManager.getAtomBits(setType, specInfo);
	}

	BitSet getAtomsWithin(String withinWhat, BitSet bs) {
		return modelManager.getAtomsWithin(withinWhat, bs);
	}

	BitSet getAtomsWithin(float distance, Point3f coord) {
		// select within(distance, coord) not compilable at the present time
		return modelManager.getAtomsWithin(distance, coord);
	}

	BitSet getAtomsWithin(String withinWhat, String specInfo, BitSet bs) {
		return modelManager.getAtomsWithin(withinWhat, specInfo, bs);
	}

	BitSet getAtomsWithin(float distance, BitSet bs) {
		return modelManager.getAtomsWithin(distance, bs);
	}

	BitSet getAtomsConnected(float min, float max, BitSet bs) {
		return modelManager.getAtomsConnected(min, max, bs);
	}

	int getAtomIndexFromAtomNumber(int atomNumber) {
		return modelManager.getAtomIndexFromAtomNumber(atomNumber);
	}

	public BitSet getElementsPresentBitSet() {
		return modelManager.getElementsPresentBitSet();
	}

	public Hashtable getHeteroList(int modelIndex) {
		return modelManager.getHeteroList(modelIndex);
	}

	BitSet getVisibleSet() {
		return modelManager.getVisibleSet();
	}

	BitSet getClickableSet() {
		return modelManager.getClickableSet();
	}

	void calcSelectedGroupsCount() {
		modelManager.calcSelectedGroupsCount(selectionManager.bsSelection);
	}

	void calcSelectedMonomersCount() {
		modelManager.calcSelectedMonomersCount(selectionManager.bsSelection);
	}

	void calcSelectedMoleculesCount() {
		modelManager.calcSelectedMoleculesCount(selectionManager.bsSelection);
	}

	String getFileHeader() {
		return modelManager.getFileHeader();
	}

	String getPDBHeader() {
		return modelManager.getPDBHeader();
	}

	public Hashtable getModelInfo() {
		return modelManager.getModelInfo();
	}

	public Hashtable getAuxiliaryInfo() {
		return modelManager.getAuxiliaryInfo();
	}

	public Hashtable getShapeInfo() {
		return modelManager.getShapeInfo();
	}

	int getShapeIdFromObjectName(String objectName) {
		return modelManager.getShapeIdFromObjectName(objectName);
	}

	Vector getAllAtomInfo(String atomExpression) {
		BitSet bs = getAtomBitSet(atomExpression);
		return modelManager.getAllAtomInfo(bs);
	}

	Vector getAllBondInfo(String atomExpression) {
		BitSet bs = getAtomBitSet(atomExpression);
		return modelManager.getAllBondInfo(bs);
	}

	Vector getMoleculeInfo(String atomExpression) {
		BitSet bs = getAtomBitSet(atomExpression);
		return modelManager.getMoleculeInfo(bs);
	}

	public Hashtable getAllChainInfo(String atomExpression) {
		BitSet bs = getAtomBitSet(atomExpression);
		return modelManager.getAllChainInfo(bs);
	}

	public Hashtable getAllPolymerInfo(String atomExpression) {
		BitSet bs = getAtomBitSet(atomExpression);
		return modelManager.getAllPolymerInfo(bs);
	}

	String loadScript;

	void setLoadScript(String script) {
		loadScript = script;
	}

	public String getStateInfo() {
		StringBuffer s = new StringBuffer("# Jmol state version " + getJmolVersion() + ";\n\n");
		// window state
		s.append(global.getWindowState());
		// file state
		s.append(fileManager.getState());
		// numerical values
		s.append(global.getState());
		// definitions, connections, atoms, bonds, labels, echos, shapes
		s.append(modelManager.getState());
		// frame information
		s.append(repaintManager.getState());
		// orientation and slabbing
		s.append(transformManager.getState());
		// display and selections
		s.append(selectionManager.getState());
		s.append("set refreshing true;\n");
		return s.toString();
	}

	static Hashtable dataValues = new Hashtable();

	@SuppressWarnings("unchecked")
	public void setData(String type, String[] data) {
		// Eval
		if (type == null) {
			dataValues.clear();
			return;
		}
		dataValues.put(type, data);
	}

	public String[] getData(String type) {
		if (dataValues == null)
			return null;
		if (type.equalsIgnoreCase("types")) {
			String[] info = new String[2];
			info[0] = "types";
			info[1] = "";
			Enumeration e = (dataValues.keys());
			while (e.hasMoreElements())
				info[1] += "," + e.nextElement();
			if (info[1].length() > 0)
				info[1] = info[1].substring(1);
			return info;
		}
		return (String[]) dataValues.get(type);
	}

	public String getAltLocListInModel(int modelIndex) {
		return modelManager.getAltLocListInModel(modelIndex);
	}

	public BitSet setConformation() {
		// user has selected some atoms, now this sets that as a conformation
		// with the effect of rewriting the cartoons to match

		return modelManager.setConformation(-1, getSelectionSet());
	}

	// AKA "configuration"
	public BitSet setConformation(int conformationIndex) {
		return modelManager.setConformation(getDisplayModelIndex(), conformationIndex);
	}

	public void autoHbond() {
		// Eval
		BitSet bs = getSelectionSet();
		autoHbond(bs, bs);
		addStateScript("calculate hbonds");
	}

	public void autoHbond(BitSet bsFrom, BitSet bsTo) {
		// Eval
		modelManager.autoHbond(bsFrom, bsTo);
	}

	boolean hbondsAreVisible() {
		return modelManager.hbondsAreVisible(getDisplayModelIndex());
	}

	public boolean havePartialCharges() {
		return modelManager.havePartialCharges();
	}

	UnitCell getCurrentUnitCell() {
		return modelManager.getUnitCell(getDisplayModelIndex());
	}

	Point3f getCurrentUnitCellOffset() {
		return modelManager.getUnitCellOffset(getDisplayModelIndex());
	}

	void setCurrentUnitCellOffset(int offset) {
		int modelIndex = getDisplayModelIndex();
		if (modelManager.setUnitCellOffset(modelIndex, offset))
			global.setParameterValue("_frame " + getModelNumber(modelIndex) + "; set unitcell", offset);
	}

	void setCurrentUnitCellOffset(Point3f pt) {
		int modelIndex = getDisplayModelIndex();
		if (modelManager.setUnitCellOffset(modelIndex, pt))
			global
					.setParameterValue("_frame " + getModelNumber(modelIndex) + "; set unitcell", StateManager
							.escape(pt));
	}

	/*******************************************************************************************************************
	 * delegated to MeasurementManager
	 ******************************************************************************************************************/

	String getDefaultMeasurementLabel(int nPoints) {
		switch (nPoints) {
		case 2:
			return global.defaultDistanceLabel;
		case 3:
			return global.defaultAngleLabel;
		default:
			return global.defaultTorsionLabel;
		}
	}

	private void setDefaultMeasurementLabel(int nPoints, String format) {
		switch (nPoints) {
		case 2:
			global.defaultDistanceLabel = format;
		case 3:
			global.defaultAngleLabel = format;
		case 4:
			global.defaultTorsionLabel = format;
		}
	}

	public int getMeasurementCount() {
		int count = getShapePropertyAsInt(JmolConstants.SHAPE_MEASURES, "count");
		return count <= 0 ? 0 : count;
	}

	public String getMeasurementStringValue(int i) {
		String str = "" + getShapeProperty(JmolConstants.SHAPE_MEASURES, "stringValue", i);
		return str;
	}

	Vector getMeasurementInfo() {
		return (Vector) getShapeProperty(JmolConstants.SHAPE_MEASURES, "info");
	}

	public String getMeasurementInfoAsString() {
		return (String) getShapeProperty(JmolConstants.SHAPE_MEASURES, "infostring");
	}

	public int[] getMeasurementCountPlusIndices(int i) {
		int[] List = (int[]) getShapeProperty(JmolConstants.SHAPE_MEASURES, "countPlusIndices", i);
		return List;
	}

	void setPendingMeasurement(int[] atomCountPlusIndices) {
		setShapeProperty(JmolConstants.SHAPE_MEASURES, "pending", atomCountPlusIndices);
	}

	void clearAllMeasurements() {
		// Eval only
		setShapeProperty(JmolConstants.SHAPE_MEASURES, "clear", null);
		refresh(0, "Viewer:clearAllMeasurements()");
	}

	public void clearMeasurements() {
		// depricated but in the API -- use "script" directly
		// see clearAllMeasurements()
		script("measures delete");
	}

	private void setJustifyMeasurements(boolean TF) {
		global.justifyMeasurements = TF;
	}

	boolean getJustifyMeasurements() {
		return global.justifyMeasurements;
	}

	void setMeasurementFormats(String strFormat) {
		setShapeProperty(JmolConstants.SHAPE_MEASURES, "setFormats", strFormat);
	}

	void defineMeasurement(Vector monitorExpressions, float[] rangeMinMax, boolean isDelete, boolean isAllConnected,
			boolean isShowHide, boolean isHidden, String strFormat) {
		// Eval.monitor()
		setShapeProperty(JmolConstants.SHAPE_MEASURES, "setConnected", new Boolean(isAllConnected));
		setShapeProperty(JmolConstants.SHAPE_MEASURES, "setRange", rangeMinMax);
		setShapeProperty(JmolConstants.SHAPE_MEASURES, "setFormat", strFormat);
		setShapeProperty(JmolConstants.SHAPE_MEASURES, isDelete ? "deleteVector"
				: isShowHide ? (isHidden ? "hideVector" : "showVector") : "defineVector", monitorExpressions);
		setStatusNewDefaultModeMeasurement("scripted", 1, "?");
	}

	public void deleteMeasurement(int i) {
		// Eval
		setShapeProperty(JmolConstants.SHAPE_MEASURES, "delete", new Integer(i));
	}

	void deleteMeasurement(int[] atomCountPlusIndices) {
		// Eval
		setShapeProperty(JmolConstants.SHAPE_MEASURES, "delete", atomCountPlusIndices);
	}

	public void showMeasurement(int[] atomCountPlusIndices, boolean isON) {
		// Eval
		setShapeProperty(JmolConstants.SHAPE_MEASURES, isON ? "show" : "hide", atomCountPlusIndices);
		refresh(0, "Viewer:showMeasurements()");
	}

	void hideMeasurements(boolean isOFF) {
		// Eval
		setShapeProperty(JmolConstants.SHAPE_MEASURES, "hideAll", new Boolean(isOFF));
		refresh(0, "hideMeasurements()");
	}

	void toggleMeasurement(int[] atomCountPlusIndices, String strFormat) {
		// Eval
		if (strFormat != null)
			setShapeProperty(JmolConstants.SHAPE_MEASURES, "setFormat", strFormat);
		setShapeProperty(JmolConstants.SHAPE_MEASURES, "toggle", atomCountPlusIndices);
	}

	// ///////////////////////////////////////////////////////////////
	// delegated to RepaintManager
	// ///////////////////////////////////////////////////////////////

	void repaint() {
		// from RepaintManager
		display.repaint();
	}

	void setAnimationDirection(int direction) {// 1 or -1
		// Eval
		repaintManager.setAnimationDirection(direction);
	}

	void reverseAnimation() {
		// Eval
		repaintManager.reverseAnimation();
	}

	int getAnimationDirection() {
		return repaintManager.animationDirection;
	}

	Hashtable getAnimationInfo() {
		return repaintManager.getAnimationInfo();
	}

	private void setAnimationFps(int fps) {
		// Eval
		// app AtomSetChooser
		repaintManager.setAnimationFps(fps);
	}

	public int getAnimationFps() {
		return repaintManager.animationFps;
	}

	void setAnimationReplayMode(int replay, float firstFrameDelay, float lastFrameDelay) {
		// Eval

		// 0 means once
		// 1 means loop
		// 2 means palindrome
		repaintManager.setAnimationReplayMode(replay, firstFrameDelay, lastFrameDelay);
	}

	int getAnimationReplayMode() {
		return repaintManager.animationReplayMode;
	}

	void setAnimationOn(boolean animationOn) {
		// Eval
		boolean wasAnimating = repaintManager.animationOn;
		if (animationOn == wasAnimating)
			return;
		repaintManager.setAnimationOn(animationOn);
	}

	void resumeAnimation() {
		// Eval
		if (repaintManager.animationOn) {
			Logger.debug("animation is ON in resumeAnimation");
			return;
		}
		repaintManager.resumeAnimation();
		refresh(0, "Viewer:resumeAnimation()");
	}

	void pauseAnimation() {
		// Eval
		if (!repaintManager.animationOn || repaintManager.animationPaused) {
			return;
		}
		repaintManager.pauseAnimation();
		refresh(0, "Viewer:pauseAnimation()");
	}

	void setAnimationRange(int modelIndex1, int modelIndex2) {
		repaintManager.setAnimationRange(modelIndex1, modelIndex2);
	}

	BitSet getVisibleFramesBitSet() {
		return repaintManager.getVisibleFramesBitSet();
	}

	boolean isAnimationOn() {
		return repaintManager.animationOn;
	}

	void setAnimationNext() {
		// Eval
		if (repaintManager.setAnimationNext())
			refresh(0, "Viewer:setAnimationNext()");
	}

	void setAnimationPrevious() {
		// Eval
		if (repaintManager.setAnimationPrevious())
			refresh(0, "Viewer:setAnimationPrevious()");
	}

	void rewindAnimation() {
		// Eval
		repaintManager.rewindAnimation();
		refresh(0, "Viewer:rewindAnimation()");
	}

	void setDisplayModelIndex(int modelIndex) {
		// Eval
		// initializeModel
		repaintManager.setDisplayModelIndex(modelIndex);
	}

	int getCurrentModelIndex() {
		return repaintManager.displayModelIndex;
	}

	public int getDisplayModelIndex() {
		// modified to indicate if there is also a background model index
		int modelIndex = repaintManager.displayModelIndex;
		int backgroundIndex = getBackgroundModelIndex();
		return (backgroundIndex >= 0 ? -2 - modelIndex : modelIndex);
	}

	private void setBackgroundModel(int modelNumber) {
		// Eval
		int modelIndex = getModelNumberIndex(modelNumber);
		setBackgroundModelIndex(modelIndex);
	}

	private void setBackgroundModelIndex(int modelIndex) {
		// initializeModel
		repaintManager.setBackgroundModelIndex(modelIndex);
	}

	public int getBackgroundModelIndex() {
		return repaintManager.backgroundModelIndex;
	}

	FrameRenderer getFrameRenderer() {
		return repaintManager.frameRenderer;
	}

	boolean wasInMotion = false;
	int motionEventNumber;

	public int getMotionEventNumber() {
		return motionEventNumber;
	}

	void setInMotion(boolean inMotion) {
		// MouseManager, TransformManager
		// Logger.debug("viewer.setInMotion("+inMotion+")");
		if (wasInMotion ^ inMotion) {
			if (inMotion)
				++motionEventNumber;
			repaintManager.setInMotion(inMotion);
			wasInMotion = inMotion;
		}
	}

	boolean getInMotion() {
		return repaintManager.inMotion;
	}

	public void pushHoldRepaint() {
		repaintManager.pushHoldRepaint();
	}

	public void popHoldRepaint() {
		repaintManager.popHoldRepaint();
	}

	private boolean refreshing = true; // XIE: had better set this to be true by default!!!

	private void setRefreshing(boolean TF) {
		refreshing = TF;
	}

	public void refresh() {
		// Draw, pauseScriptExecution
		repaintManager.refresh();
	}

	public void refresh(int isOrientationChange, String strWhy) {
		repaintManager.refresh();
		statusManager.setStatusViewerRefreshed(isOrientationChange, strWhy);
	}

	void requestRepaintAndWait() {
		if (haveDisplay)
			repaintManager.requestRepaintAndWait();
	}

	public void repaintView() {
		repaintManager.repaintDone();
	}

	private boolean axesAreTainted = false;

	boolean areAxesTainted() {
		boolean TF = axesAreTainted;
		axesAreTainted = false;
		return TF;
	}

	// //////////// screen/image methods ///////////////

	final Dimension dimScreen = new Dimension();

	final Rectangle rectClip = new Rectangle();

	public void setScreenDimension(Dimension dim) {
		// There is a bug in Netscape 4.7*+MacOS 9 when comparing dimension objects
		// so don't try dim1.equals(dim2)
		int height = dim.height;
		int width = dim.width;
		if (getStereoMode() == JmolConstants.STEREO_DOUBLE)
			width = (width + 1) / 2;
		if (dimScreen.width == width && dimScreen.height == height)
			return;
		dimScreen.width = width;
		dimScreen.height = height;
		transformManager.setScreenDimension(width, height);
		transformManager.scaleFitToScreen();
		g3d.setWindowSize(width, height, global.enableFullSceneAntialiasing);
	}

	public int getScreenWidth() {
		return dimScreen.width;
	}

	public int getScreenHeight() {
		return dimScreen.height;
	}

	void setRectClip(Rectangle clip) {
		if (clip == null) {
			rectClip.x = rectClip.y = 0;
			rectClip.setSize(dimScreen);
		}
		else {
			rectClip.setBounds(clip);
			// on Linux platform with Sun 1.4.2_02 I am getting a clipping rectangle
			// that is wider than the current window during window resize
			if (rectClip.x < 0)
				rectClip.x = 0;
			if (rectClip.y < 0)
				rectClip.y = 0;
			if (rectClip.x + rectClip.width > dimScreen.width)
				rectClip.width = dimScreen.width - rectClip.x;
			if (rectClip.y + rectClip.height > dimScreen.height)
				rectClip.height = dimScreen.height - rectClip.y;
		}
	}

	public void renderScreenImage(Graphics g, Dimension size, Rectangle clip) {
		if (isTainted || getSlabEnabled())
			setModelVisibility();
		isTainted = false;
		if (size != null)
			setScreenDimension(size);
		setRectClip(null);
		int stereoMode = getStereoMode();
		switch (stereoMode) {
		case JmolConstants.STEREO_DOUBLE:
			render1(g, getImage(true, false), dimScreen.width, 0);
		case JmolConstants.STEREO_NONE:
			render1(g, getImage(false, false), 0, 0);
			break;
		case JmolConstants.STEREO_REDCYAN:
		case JmolConstants.STEREO_REDBLUE:
		case JmolConstants.STEREO_REDGREEN:
		case JmolConstants.STEREO_CUSTOM:
			render1(g, getStereoImage(stereoMode, false), 0, 0);
			break;
		}
		repaintView();
	}

	private Image getImage(boolean isDouble, boolean antialias) {
		Matrix3f matrixRotate = transformManager.getStereoRotationMatrix(isDouble);
		g3d.beginRendering(rectClip.x, rectClip.y, rectClip.width, rectClip.height, matrixRotate, antialias);
		repaintManager.render(g3d, rectClip, modelManager.getFrame(), repaintManager.displayModelIndex);
		// mth 2003-01-09 Linux Sun JVM 1.4.2_02
		// Sun is throwing a NullPointerExceptions inside graphics routines
		// while the window is resized.
		g3d.endRendering();
		return g3d.getScreenImage();
	}

	private Image getStereoImage(int stereoMode, boolean antialias) {
		g3d.beginRendering(rectClip.x, rectClip.y, rectClip.width, rectClip.height, transformManager
				.getStereoRotationMatrix(true), antialias);
		repaintManager.render(g3d, rectClip, modelManager.getFrame(), repaintManager.displayModelIndex);
		g3d.endRendering();
		g3d.snapshotAnaglyphChannelBytes();
		g3d.beginRendering(rectClip.x, rectClip.y, rectClip.width, rectClip.height, transformManager
				.getStereoRotationMatrix(false), antialias);
		repaintManager.render(g3d, rectClip, modelManager.getFrame(), repaintManager.displayModelIndex);
		g3d.endRendering();
		switch (stereoMode) {
		case JmolConstants.STEREO_REDCYAN:
			g3d.applyCyanAnaglyph();
			break;
		case JmolConstants.STEREO_CUSTOM:
			g3d.applyCustomAnaglyph(transformManager.stereoColors);
			break;
		case JmolConstants.STEREO_REDBLUE:
			g3d.applyBlueAnaglyph();
			break;
		default:
			g3d.applyGreenAnaglyph();
		}
		return g3d.getScreenImage();
	}

	private void render1(Graphics g, Image img, int x, int y) {
		if (g == null)
			return;
		try {
			g.drawImage(img, x, y, null);
		}
		catch (NullPointerException npe) {
			Logger.error("Sun!! ... fix graphics your bugs!");
		}
		g3d.releaseScreenImage();
	}

	public Image getScreenImage() {
		boolean antialias = true;
		boolean isStereo = false;
		setRectClip(null);
		int stereoMode = getStereoMode();
		switch (stereoMode) {
		case JmolConstants.STEREO_DOUBLE:
			// this allows for getting both eye views in two images
			// because you can adjust using "stereo -2.5", then "stereo +2.5"
			isStereo = true;
			break;
		case JmolConstants.STEREO_REDCYAN:
		case JmolConstants.STEREO_REDBLUE:
		case JmolConstants.STEREO_REDGREEN:
		case JmolConstants.STEREO_CUSTOM:
			return getStereoImage(stereoMode, false);
		}
		return getImage(isStereo, antialias);
	}

	/**
	 * @param quality
	 * @return base64-encoded version of the image
	 */
	public String getJpegBase64(int quality) {
		Image eImage = getScreenImage();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		JpegEncoder jc = new JpegEncoder(eImage, quality, os);
		jc.Compress();
		byte[] jpeg = os.toByteArray();
		releaseScreenImage();
		return "" + Base64.getBase64(jpeg);
	}

	public void releaseScreenImage() {
		g3d.releaseScreenImage();
	}

	// ///////////////////////////////////////////////////////////////
	// routines for script support
	// ///////////////////////////////////////////////////////////////

	public String evalFile(String strFilename) {
		// from app only
		return scriptManager.addScript(strFilename, true, false);
	}

	public String script(String strScript) {
		return evalString(strScript);
	}

	public String evalString(String strScript) {
		if (checkResume(strScript))
			return "script processing resumed";
		if (checkHalt(strScript))
			return "script execution halted";
		return scriptManager.addScript(strScript, false, false);
	}

	boolean usingScriptQueue() {
		return scriptManager.useQueue;
	}

	public void clearScriptQueue() {
		// Eval
		// checkHalt **
		scriptManager.clearQueue();
	}

	public boolean checkResume(String strScript) {
		if (strScript.equalsIgnoreCase("resume")) {
			resumeScriptExecution();
			return true;
		}
		return false;
	}

	public boolean checkHalt(String strScript) {
		String str = strScript.toLowerCase();
		if (str.equals("pause")) {
			pauseScriptExecution();
			return true;
		}
		if (str.startsWith("exit")) {
			haltScriptExecution();
			clearScriptQueue();
			return str.equals("exit");
		}
		if (str.startsWith("quit")) {
			haltScriptExecution();
			return str.equals("quit");
		}
		return false;
	}

	public String evalStringQuiet(String strScript) {
		if (checkResume(strScript))
			return "script processing resumed";
		if (checkHalt(strScript))
			return "script execution halted";
		return scriptManager.addScript(strScript, false, true);
	}

	// / direct no-queue use:

	public String scriptWait(String strScript) {
		scriptManager.waitForQueue();
		boolean doTranslateTemp = GT.getDoTranslate();
		GT.setDoTranslate(false);
		String str = (String) evalStringWaitStatus("JSON", strScript,
				"+scriptStarted,+scriptStatus,+scriptEcho,+scriptTerminated", false, false, null);
		GT.setDoTranslate(doTranslateTemp);
		return str;
	}

	public Object scriptWaitStatus(String strScript, String statusList) {
		scriptManager.waitForQueue();
		boolean doTranslateTemp = GT.getDoTranslate();
		GT.setDoTranslate(false);
		Object ret = evalStringWaitStatus("object", strScript, statusList, false, false, null);
		GT.setDoTranslate(doTranslateTemp);
		return ret;
	}

	public Object evalStringWaitStatus(String returnType, String strScript, String statusList) {
		scriptManager.waitForQueue();
		return evalStringWaitStatus(returnType, strScript, statusList, false, false, null);
	}

	synchronized Object evalStringWaitStatus(String returnType, String strScript, String statusList,
			boolean isScriptFile, boolean isQuiet, Vector tokenInfo) {
		// System.out.println(Thread.currentThread()+" --- "+strScript);
		// from the scriptManager only!
		if (checkResume(strScript))
			return "script processing resumed"; // be very odd if this fired
		if (checkHalt(strScript))
			return "script execution halted";
		if (strScript == null)
			return null;

		// typically request: "+scriptStarted,+scriptStatus,+scriptEcho,+scriptTerminated"
		// set up first with applet.jmolGetProperty("jmolStatus",statusList)

		// flush list
		String oldStatusList = statusManager.statusList;
		getProperty("String", "jmolStatus", statusList);
		boolean isOK = (tokenInfo != null ? eval.loadTokenInfo(strScript, tokenInfo) : isScriptFile ? eval
				.loadScriptFile(strScript, isQuiet) : eval.loadScriptString(strScript, isQuiet));
		if (isOK) {
			eval.runEval();
			String strErrorMessage = eval.getErrorMessage();
			int msWalltime = eval.getExecutionWalltime();
			statusManager.setStatusScriptTermination(strErrorMessage, msWalltime);
			if (isScriptFile && writeInfo != null)
				createImage(writeInfo);
		}
		if (isScriptFile && autoExit) {
			System.out.flush();
			System.exit(0);
		}
		if (returnType.equalsIgnoreCase("String"))
			return eval.getErrorMessage();
		// get Vector of Vectors of Vectors info
		Object info = getProperty(returnType, "jmolStatus", statusList);
		// reset to previous status list
		getProperty("object", "jmolStatus", oldStatusList);
		return info;
	}

	synchronized public String scriptCheck(String strScript) {
		if (strScript == null)
			return null;
		Object obj = eval.checkScript(strScript);
		if (obj instanceof String)
			return (String) obj;
		return "";
	}

	synchronized public Object compileInfo(String strScript) {
		if (strScript == null)
			return null;
		return eval.checkScript(strScript);
	}

	public boolean isScriptExecuting() {
		return eval.isScriptExecuting();
	}

	public void haltScriptExecution() {
		eval.haltExecution();
	}

	public void resumeScriptExecution() {
		eval.resumePausedExecution();
	}

	public void pauseScriptExecution() {
		refresh();
		eval.pauseExecution();
	}

	private void setDefaultLoadScript(String script) {
		// Eval
		global.defaultLoadScript = script;
	}

	String getDefaultLoadScript() {
		return global.defaultLoadScript;
	}

	String getStandardLabelFormat() {
		return stateManager.getStandardLabelFormat();
	}

	int getRibbonAspectRatio() {
		return global.ribbonAspectRatio;
	}

	private void setRibbonAspectRatio(int ratio) {
		// Eval
		global.ribbonAspectRatio = ratio;
	}

	float getSheetSmoothing() {
		return global.sheetSmoothing;
	}

	private void setSheetSmoothing(float factor0To1) {
		// Eval
		global.sheetSmoothing = factor0To1;
	}

	boolean getSsbondsBackbone() {
		return global.ssbondsBackbone;
	}

	private void setHbondsBackbone(boolean TF) {
		// Eval
		global.hbondsBackbone = TF;
	}

	boolean getHbondsBackbone() {
		return global.hbondsBackbone;
	}

	private void setHbondsSolid(boolean TF) {
		// Eval
		global.hbondsSolid = TF;
	}

	boolean getHbondsSolid() {
		return global.hbondsSolid;
	}

	public void setMarBond(short marBond) {
		global.marBond = marBond;
		setShapeSize(JmolConstants.SHAPE_STICKS, marBond * 2);
	}

	int hoverAtomIndex = -1;
	String hoverText;

	void hoverOn(int atomIndex) {
		if ((eval == null || !isScriptExecuting()) && atomIndex != hoverAtomIndex) {
			loadShape(JmolConstants.SHAPE_HOVER);
			setShapeProperty(JmolConstants.SHAPE_HOVER, "target", new Integer(atomIndex));
			hoverAtomIndex = atomIndex;
		}
	}

	void hoverOn(int x, int y, String text) {
		if (eval != null && isScriptExecuting())
			return;
		loadShape(JmolConstants.SHAPE_HOVER);
		setShapeProperty(JmolConstants.SHAPE_HOVER, "xy", new Point3i(x, y, 0));
		setShapeProperty(JmolConstants.SHAPE_HOVER, "target", null);
		setShapeProperty(JmolConstants.SHAPE_HOVER, "text", text);
		hoverAtomIndex = -1;
		hoverText = text;
	}

	void hoverOff() {
		if (hoverAtomIndex >= 0) {
			setShapeProperty(JmolConstants.SHAPE_HOVER, "target", null);
			hoverAtomIndex = -1;
		}
	}

	void setLabel(String strLabel) {
		// Eval
		if (strLabel != null) // force the class to load and display
			setShapeSize(JmolConstants.SHAPE_LABELS, 0);
		setShapeProperty(JmolConstants.SHAPE_LABELS, "label", strLabel);
	}

	void togglePickingLabel(BitSet bs) {
		// eval set toggleLabel (atomset)
		setShapeSize(JmolConstants.SHAPE_LABELS, 0);
		modelManager.setShapeProperty(JmolConstants.SHAPE_LABELS, "toggleLabel", null, bs);
		refresh(0, "Viewer:");
	}

	BitSet getBitSetSelection() {
		return selectionManager.bsSelection;
	}

	private void setShapeShow(int shapeID, boolean show) {
		setShapeSize(shapeID, show ? -1 : 0);
	}

	boolean getShapeShow(int shapeID) {
		return getShapeSize(shapeID) != 0;
	}

	void loadShape(int shapeID) {
		modelManager.loadShape(shapeID);
	}

	void setShapeSize(int shapeID, int size) {
		// Eval - many
		// stateManager.setCrystallographicDefaults
		// Viewer - many
		setShapeSize(shapeID, size, selectionManager.bsSelection);
	}

	void setShapeSize(int shapeID, int size, BitSet bsAtoms) {
		// above,
		// Eval.configuration
		modelManager.setShapeSize(shapeID, size, bsAtoms);
		refresh(0, "Viewer:setShapeSize(" + shapeID + "," + size + ")");
	}

	int getShapeSize(int shapeID) {
		return modelManager.getShapeSize(shapeID);
	}

	void setShapeProperty(int shapeID, String propertyName, Object value) {
		// Eval
		// many local

		/*
		 * Logger.debug("JmolViewer.setShapeProperty("+ JmolConstants.shapeClassBases[shapeID]+ "," + propertyName + ","
		 * + value + ")");
		 */
		if (shapeID < 0)
			return; // not applicable
		modelManager.setShapeProperty(shapeID, propertyName, value, selectionManager.bsSelection);
		refresh(0, "Viewer:setShapeProperty()");
	}

	void setShapeProperty(int shapeID, String propertyName, Object value, BitSet bs) {
		// Eval color
		if (shapeID < 0)
			return; // not applicable
		modelManager.setShapeProperty(shapeID, propertyName, value, bs);
		refresh(0, "Viewer:setShapeProperty()");
	}

	void setShapePropertyArgb(int shapeID, String propertyName, int argb) {
		// Eval
		setShapeProperty(shapeID, propertyName, argb == 0 ? null : new Integer(argb | 0xFF000000));
	}

	Object getShapeProperty(int shapeType, String propertyName) {
		return modelManager.getShapeProperty(shapeType, propertyName, Integer.MIN_VALUE);
	}

	Object getShapeProperty(int shapeType, String propertyName, int index) {
		return modelManager.getShapeProperty(shapeType, propertyName, index);
	}

	int getShapePropertyAsInt(int shapeID, String propertyName) {
		Object value = getShapeProperty(shapeID, propertyName);
		return value == null || !(value instanceof Integer) ? Integer.MIN_VALUE : ((Integer) value).intValue();
	}

	int getShapeID(String shapeName) {
		for (int i = JmolConstants.SHAPE_MAX; --i >= 0;)
			if (JmolConstants.shapeClassBases[i].equals(shapeName))
				return i;
		String msg = "Unrecognized shape name:" + shapeName;
		Logger.error(msg);
		throw new NullPointerException(msg);
	}

	short getColix(Object object) {
		return Graphics3D.getColix(object);
	}

	private void setRasmolHydrogenSetting(boolean b) {
		// Eval
		global.rasmolHydrogenSetting = b;
	}

	boolean getRasmolHydrogenSetting() {
		return global.rasmolHydrogenSetting;
	}

	private void setRasmolHeteroSetting(boolean b) {
		// Eval
		global.rasmolHeteroSetting = b;
	}

	boolean getRasmolHeteroSetting() {
		return global.rasmolHeteroSetting;
	}

	boolean getDebugScript() {
		return global.debugScript;
	}

	private void setDebugScript(boolean debugScript) {
		global.debugScript = debugScript;
		Logger.setActiveLevel(Logger.LEVEL_DEBUG, debugScript);
	}

	void atomPicked(int atomIndex, int modifiers) {
		if (!isInSelectionSubset(atomIndex))
			return;
		pickingManager.atomPicked(atomIndex, modifiers);
	}

	void clearClickCount() {
		// MouseManager.clearclickCount()
		mouseManager.clearClickCount();
		setTainted(true);
	}

	private void setPickingMode(String mode) {
		int pickingMode = JmolConstants.GetPickingMode(mode);
		if (pickingMode < 0)
			pickingMode = JmolConstants.PICKING_IDENT;
		pickingManager.setPickingMode(pickingMode);
	}

	int getPickingMode() {
		return pickingManager.getPickingMode();
	}

	private void setPickingStyle(String style) {
		int pickingStyle = JmolConstants.GetPickingMode(style);
		if (pickingStyle < 0)
			pickingStyle = JmolConstants.PICKINGSTYLE_SELECT_JMOL;
		pickingManager.setPickingStyle(pickingStyle);
	}

	public String getAtomInfo(int atomIndex) {
		return modelManager.getAtomInfo(atomIndex);
	}

	public String getAtomInfoXYZ(int atomIndex) {
		return modelManager.getAtomInfoXYZ(atomIndex);
	}

	// //////////////status manager dispatch//////////////

	public Hashtable getMessageQueue() {
		return statusManager.messageQueue;
	}

	Viewer getViewer() {
		return this;
	}

	private void setCallbackFunction(String callbackType, String callbackFunction) {
		// Eval
		if (callbackFunction.equalsIgnoreCase("none"))
			callbackFunction = null;
		statusManager.setCallbackFunction(callbackType, callbackFunction);
	}

	void setStatusAtomPicked(int atomIndex, String info) {
		statusManager.setStatusAtomPicked(atomIndex, info);
	}

	void setStatusAtomHovered(int atomIndex, String info) {
		statusManager.setStatusAtomHovered(atomIndex, info);
	}

	void setStatusNewPickingModeMeasurement(int iatom, String strMeasure) {
		statusManager.setStatusNewPickingModeMeasurement(iatom, strMeasure);
	}

	void setStatusNewDefaultModeMeasurement(String status, int count, String strMeasure) {
		statusManager.setStatusNewDefaultModeMeasurement(status, count, strMeasure);
	}

	void setStatusScriptStarted(int iscript, String script, String strError) {
		statusManager.setStatusScriptStarted(iscript, script, strError);
	}

	void setStatusUserAction(String info) {
		statusManager.setStatusUserAction(info);
	}

	Vector getStatusChanged(String statusNameList) {
		return statusManager.getStatusChanged(statusNameList);
	}

	void popupMenu(int x, int y) {
		if (global.disablePopupMenu)
			return;
		setFrankOn(true);
		statusManager.popupMenu(x, y);
	}

	public void setJmolStatusListener(JmolStatusListener jmolStatusListener) {
		statusManager.setJmolStatusListener(jmolStatusListener);
	}

	void setStatusFrameChanged(int frameNo) {
		statusManager.setStatusFrameChanged(frameNo);
	}

	void setStatusFileLoaded(int ptLoad, String fullPathName, String fileName, String modelName, Object clientFile,
			String strError) {
		statusManager.setStatusFileLoaded(fullPathName, fileName, modelName, clientFile, strError, ptLoad);
	}

	void setStatusFileNotLoaded(String fullPathName, String errorMsg) {
		setStatusFileLoaded(-1, fullPathName, null, null, null, errorMsg);
	}

	public void scriptEcho(String strEcho) {
		statusManager.setScriptEcho(strEcho);
	}

	void scriptStatus(String strStatus) {
		statusManager.setScriptStatus(strStatus);
	}

	public void showUrl(String urlString) {
		// applet.Jmol
		// app Jmol
		// StatusManager
		if (urlString.indexOf(":") < 0) {
			String base = fileManager.getAppletDocumentBase();
			if (base == "")
				base = fileManager.getFullPathName();
			if (base.indexOf("/") >= 0) {
				base = base.substring(0, base.lastIndexOf("/") + 1);
			}
			else if (base.indexOf("\\") >= 0) {
				base = base.substring(0, base.lastIndexOf("\\") + 1);
			}
			urlString = base + urlString;
		}
		Logger.info("showUrl:" + urlString);
		statusManager.showUrl(urlString);
	}

	void showConsole(boolean showConsole) {
		// Eval
		statusManager.showConsole(showConsole);
	}

	void clearConsole() {
		// Eval
		statusManager.clearConsole();
	}

	/*******************************************************************************************************************
	 * mth 2003 05 31 - needs more work this should be implemented using properties or as a hashtable using
	 * boxed/wrapped values so that the values could be shared
	 * 
	 * @param key
	 * @return the boolean property mth 2005 06 24 and/or these property names should be interned strings so that we can
	 *         just do == comparisions between strings
	 ******************************************************************************************************************/

	public boolean getBooleanProperty(String key) {
		// JmolPopup
		if (key.equalsIgnoreCase("hideNotSelected"))
			return selectionManager.getHideNotSelected();
		if (key.equalsIgnoreCase("colorRasmol"))
			return colorManager.getDefaultColorRasmol();
		if (key.equalsIgnoreCase("perspectiveDepth"))
			return getPerspectiveDepth();
		if (key.equalsIgnoreCase("showAxes"))
			return getShapeShow(JmolConstants.SHAPE_AXES);
		if (key.equalsIgnoreCase("showBoundBox"))
			return getShapeShow(JmolConstants.SHAPE_BBCAGE);
		if (key.equalsIgnoreCase("showUnitcell"))
			return getShapeShow(JmolConstants.SHAPE_UCCAGE);
		if (key.equalsIgnoreCase("debugScript"))
			return getDebugScript();
		if (key.equalsIgnoreCase("showHydrogens"))
			return getShowHydrogens();
		if (key.equalsIgnoreCase("frank"))
			return getFrankOn();
		if (key.equalsIgnoreCase("showMultipleBonds"))
			return getShowMultipleBonds();
		if (key.equalsIgnoreCase("showMeasurements"))
			return getShowMeasurements();
		if (key.equalsIgnoreCase("showSelections"))
			return getSelectionHaloEnabled();
		if (key.equalsIgnoreCase("axesOrientationRasmol"))
			return getAxesOrientationRasmol();
		if (key.equalsIgnoreCase("zeroBasedXyzRasmol"))
			return getZeroBasedXyzRasmol();
		if (key.equalsIgnoreCase("testFlag1"))
			return getTestFlag1();
		if (key.equalsIgnoreCase("testFlag2"))
			return getTestFlag2();
		if (key.equalsIgnoreCase("testFlag3"))
			return getTestFlag3();
		if (key.equalsIgnoreCase("testFlag4"))
			return getTestFlag4();
		if (key.equalsIgnoreCase("chainCaseSensitive"))
			return getChainCaseSensitive();
		if (key.equalsIgnoreCase("hideNameInPopup"))
			return getHideNameInPopup();
		if (key.equalsIgnoreCase("autobond"))
			return getAutoBond();
		if (key.equalsIgnoreCase("greyscaleRendering"))
			return getGreyscaleRendering();
		if (key.equalsIgnoreCase("disablePopupMenu"))
			return getDisablePopupMenu();

		if (global.htPropertyFlags.containsKey(key)) {
			return ((Boolean) global.htPropertyFlags.get(key)).booleanValue();
		}

		Logger.error("viewer.getBooleanProperty(" + key + ") - unrecognized");
		return false;
	}

	public void setStringProperty(String key, String value) {
		// Eval
		while (true) {
			if (key.equalsIgnoreCase("defaultDistanceLabel")) {
				setDefaultMeasurementLabel(2, value);
				break;
			}
			if (key.equalsIgnoreCase("defaultAngleLabel")) {
				setDefaultMeasurementLabel(3, value);
				break;
			}
			if (key.equalsIgnoreCase("defaultTorsionLabel")) {
				setDefaultMeasurementLabel(4, value);
				break;
			}
			if (key.equalsIgnoreCase("defaultLoadScript")) {
				setDefaultLoadScript(value);
				break;
			}
			if (key.equalsIgnoreCase("appletProxy")) {
				setAppletProxy(value);
				break;
			}
			if (key.equalsIgnoreCase("defaultDirectory")) {
				setDefaultDirectory(value);
				break;
			}
			if (key.equalsIgnoreCase("help")) {
				setHelpPath(value);
				break;
			}
			if (key.equalsIgnoreCase("backgroundColor")) {
				setColorBackground(value);
				break;
			}
			if (key.equalsIgnoreCase("defaults")) {
				setDefaults(value);
				break;
			}
			if (key.equalsIgnoreCase("defaultColorScheme")) {
				setDefaultColors(value);
				break;
			}
			if (key.equalsIgnoreCase("picking")) {
				setPickingMode(value);
				break;
			}
			if (key.equalsIgnoreCase("pickingStyle")) {
				setPickingStyle(value);
				break;
			}
			if (key.equalsIgnoreCase("dataSeparator")) {
				// just saving this
				break;
			}
			if (key.toLowerCase().indexOf("callback") >= 0) {
				setCallbackFunction(key, value);
				break;
			}
			// not found
			if (key.charAt(0) != '@') {
				if (!global.htParameterValues.containsKey(key)) {
					Logger.warn("viewer.setStringProperty(" + key + "," + value + ") - new SET option");
					return;
				}
			}
			break;
		}
		global.setParameterValue(key, value);
	}

	public void setFloatProperty(String key, float value) {
		// Eval
		while (true) {
			if (key.equalsIgnoreCase("sheetSmoothing")) {
				setSheetSmoothing(value);
				break;
			}
			if (key.equalsIgnoreCase("dipoleScale")) {
				setDipoleScale(value);
				break;
			}
			if (key.equalsIgnoreCase("stereoDegrees")) {
				setStereoDegrees(value);
				break;
			}
			if (key.equalsIgnoreCase("defaultVectorScale")) {
				setDefaultVectorScale(value);
				break;
			}
			if (key.equalsIgnoreCase("defaultVibrationPeriod")) {
				setDefaultVibrationPeriod(value);
				break;
			}
			if (key.equalsIgnoreCase("defaultVibrationScale")) {
				setDefaultVibrationScale(value);
				break;
			}
			if (key.equalsIgnoreCase("vibrationPeriod")) {
				setVibrationPeriod(value);
				break;
			}
			if (key.equalsIgnoreCase("vibrationScale")) {
				setVibrationScale(value);
				break;
			}
			if (key.equalsIgnoreCase("bondTolerance")) {
				setBondTolerance(value);
				break;
			}
			if (key.equalsIgnoreCase("minBondDistance")) {
				setMinBondDistance(value);
				break;
			}
			if (key.equalsIgnoreCase("scaleAngstromsPerInch")) {
				setScaleAngstromsPerInch(value);
				break;
			}
			if (key.equalsIgnoreCase("solventProbeRadius")) {
				setSolventProbeRadius(value);
				break;
			}
			if (key.equalsIgnoreCase("radius")) { // deprecated
				setFloatProperty("solventProbeRadius", value);
				return;
			}
			// not found
			if (!global.htParameterValues.containsKey(key)) {
				Logger.warn("viewer.setFloatProperty(" + key + "," + value + ") - new SET option");
				return;
			}
			break;
		}
		global.setParameterValue(key, value);
	}

	public void setIntProperty(String key, int value) {
		// Eval
		setIntProperty(key, value, true);
	}

	void setIntProperty(String key, int value, boolean defineNew) {
		while (true) {
			if (key.equalsIgnoreCase("backgroundModel")) {
				setBackgroundModel(value);
				break;
			}
			if (key.equalsIgnoreCase("specPower")) {
				setSpecularPower(value);
				break;
			}
			if (key.equalsIgnoreCase("specular")) {
				setSpecularPercent(value);
				break;
			}
			if (key.equalsIgnoreCase("diffuse")) {
				setDiffusePercent(value);
				break;
			}
			if (key.equalsIgnoreCase("ambient")) {
				setAmbientPercent(value);
				break;
			}
			if (key.equalsIgnoreCase("ribbonAspectRatio")) {
				setRibbonAspectRatio(value);
				break;
			}
			if (key.equalsIgnoreCase("pickingSpinRate")) {
				setPickingSpinRate(value);
				break;
			}
			if (key.equalsIgnoreCase("animationFps")) {
				setAnimationFps(value);
				break;
			}
			if (key.equalsIgnoreCase("percentVdwAtom")) {
				setPercentVdwAtom(value);
				break;
			}
			if (key.equalsIgnoreCase("bondRadiusMilliAngstroms")) {
				setMarBond((short) value);
				break;
			}
			if (key.equalsIgnoreCase("hermiteLevel")) {
				setHermiteLevel(value);
				break;
			}
			if (value != 0 || value != 1 || !setBooleanProperty(key, false, false))
				setFloatProperty(key, value);
			return;
		}
		if (defineNew)
			global.setParameterValue(key, value);
	}

	public void setBooleanProperty(String key, boolean value) {
		setBooleanProperty(key, value, true);
	}

	boolean setBooleanProperty(String key, boolean value, boolean defineNew) {
		boolean notFound = false;
		while (true) {
			if (key.equalsIgnoreCase("refreshing")) {
				setRefreshing(value);
				break;
			}
			if (key.equalsIgnoreCase("navigationMode")) {
				setNavigationMode(value);
				break;
			}
			if (key.equalsIgnoreCase("justifyMeasurements")) {
				setJustifyMeasurements(value);
				break;
			}
			if (key.equalsIgnoreCase("ssBondsBackbone")) {
				setSsbondsBackbone(value);
				break;
			}
			if (key.equalsIgnoreCase("hbondsBackbone")) {
				setHbondsBackbone(value);
				break;
			}
			if (key.equalsIgnoreCase("hbondsSolid")) {
				setHbondsSolid(value);
				break;
			}
			if (key.equalsIgnoreCase("specular")) {
				setSpecular(value);
				break;
			}
			if (key.equalsIgnoreCase("slabEnabled")) {
				setSlabEnabled(value);
				break;
			}
			if (key.equalsIgnoreCase("zoomEnabled")) {
				setZoomEnabled(value);
				break;
			}
			if (key.equalsIgnoreCase("solvent")) {
				setSolventOn(value);
				break;
			}
			if (key.equalsIgnoreCase("highResolution")) {
				setHighResolution(value);
				break;
			}
			if (key.equalsIgnoreCase("traceAlpha")) {
				setTraceAlpha(value);
				break;
			}
			if (key.equalsIgnoreCase("zoomLarge")) {
				setZoomLarge(value);
				break;
			}
			if (key.equalsIgnoreCase("languageTranslation")) {
				GT.setDoTranslate(value);
				break;
			}
			if (key.equalsIgnoreCase("hideNotSelected")) {
				setHideNotSelected(value);
				break;
			}
			if (key.equalsIgnoreCase("colorRasmol")) {
				setDefaultColors(value ? "rasmol" : "jmol");
				break;
			}
			if (key.equalsIgnoreCase("perspectiveDepth")) {
				setPerspectiveDepth(value);
				break;
			}
			if (key.equalsIgnoreCase("scriptQueue")) {
				scriptManager.setQueue(value);
				break;
			}
			if (key.equalsIgnoreCase("dotSurface")) {
				setDotSurfaceFlag(value);
				break;
			}
			if (key.equalsIgnoreCase("dotsSelectedOnly")) {
				setDotsSelectedOnlyFlag(value);
				break;
			}
			if (key.equalsIgnoreCase("showAxes")) { // deprecated -- see "axes" command
				setShapeShow(JmolConstants.SHAPE_AXES, value);
				break;
			}
			if (key.equalsIgnoreCase("showBoundBox")) { // deprecated -- see "boundBox"
				setShapeShow(JmolConstants.SHAPE_BBCAGE, value);
				break;
			}
			if (key.equalsIgnoreCase("showUnitcell")) { // deprecated -- see "unitcell"
				setShapeShow(JmolConstants.SHAPE_UCCAGE, value);
				break;
			}
			if (key.equalsIgnoreCase("selectionHalos")) {
				setSelectionHaloEnabled(value); // volatile
				break;
			}
			if (key.equalsIgnoreCase("debugScript")) {
				setDebugScript(value);
				break;
			}
			if (key.equalsIgnoreCase("frank")) {
				setFrankOn(value);
				break;
			}
			if (key.equalsIgnoreCase("showHydrogens")) {
				setShowHydrogens(value);
				break;
			}
			if (key.equalsIgnoreCase("defaultSelectHydrogen")) {
				setRasmolHydrogenSetting(value);
				break;
			}
			if (key.equalsIgnoreCase("defaultSelectHetero")) {
				setRasmolHeteroSetting(value);
				break;
			}
			if (key.equalsIgnoreCase("showMultipleBonds")) {
				setShowMultipleBonds(value);
				break;
			}
			if (key.equalsIgnoreCase("showHiddenSelectionHalos")) {
				setShowHiddenSelectionHalos(value);
				break;
			}
			if (key.equalsIgnoreCase("showMeasurements")) {
				setShowMeasurements(value);
				break;
			}
			if (key.equalsIgnoreCase("axesOrientationRasmol")) {
				setAxesOrientationRasmol(value);
				break;
			}
			if (key.equalsIgnoreCase("windowCentered")) {
				setWindowCentered(value);
				break;
			}
			if (key.equalsIgnoreCase("adjustCamera")) {
				setAdjustCamera(value);
				break;
			}
			if (key.equalsIgnoreCase("axesWindow")) {
				setAxesModeMolecular(!value);
				break;
			}
			if (key.equalsIgnoreCase("axesMolecular")) {
				setAxesModeMolecular(value);
				break;
			}
			if (key.equalsIgnoreCase("axesUnitCell")) {
				setAxesModeUnitCell(value);
				break;
			}
			if (key.equalsIgnoreCase("displayCellParameters")) {
				setDisplayCellParameters(value);
				break;
			}
			if (key.equalsIgnoreCase("testFlag1")) {
				setTestFlag1(value);
				break;
			}
			if (key.equalsIgnoreCase("testFlag2")) {
				setTestFlag2(value);
				break;
			}
			if (key.equalsIgnoreCase("testFlag3")) {
				setTestFlag3(value);
				break;
			}
			if (key.equalsIgnoreCase("testFlag4")) {
				setTestFlag4(value);
				break;
			}
			if (key.equalsIgnoreCase("ribbonBorder")) {
				setRibbonBorder(value);
				break;
			}
			if (key.equalsIgnoreCase("cartoonRockets")) {
				setCartoonRocketFlag(value);
				break;
			}
			if (key.equalsIgnoreCase("greyscaleRendering")) {
				setGreyscaleRendering(value);
				break;
			}
			// these next are deprecated because they don't
			// give much indication what they really do:
			if (key.equalsIgnoreCase("bonds")) {
				return setBooleanProperty("showMultipleBonds", value, true);
			}
			if (key.equalsIgnoreCase("hydrogen")) { // deprecated
				return setBooleanProperty("defaultSelectHydrogen", value, true);
			}
			if (key.equalsIgnoreCase("hetero")) { // deprecated
				return setBooleanProperty("defaultSelectHetero", value, true);
			}
			if (key.equalsIgnoreCase("showSelections")) { // deprecated -- see "selectionHalos"
				return setBooleanProperty("selectionHalos", value, true);
			}
			// these next return, because there is no need to repaint
			while (true) {
				if (key.equalsIgnoreCase("bondModeOr")) {
					setBondSelectionModeOr(value);
					break;
				}
				if (key.equalsIgnoreCase("zeroBasedXyzRasmol")) {
					setZeroBasedXyzRasmol(value);
					break;
				}
				if (key.equalsIgnoreCase("rangeSelected")) {
					setRangeSelected(value);
					break;
				}
				if (key.equalsIgnoreCase("cameraMove")) {
					setAllowCameraMove(value);
					break;
				}
				if (key.equalsIgnoreCase("measureAllModels")) {
					setMeasureAllModels(value);
					break;
				}
				if (key.equalsIgnoreCase("statusReporting")) {
					setAllowStatusReporting(value);
					break;
				}
				if (key.equalsIgnoreCase("chainCaseSensitive")) {
					setChainCaseSensitive(value);
					break;
				}
				if (key.equalsIgnoreCase("hideNameInPopup")) {
					setHideNameInPopup(value);
					break;
				}
				if (key.equalsIgnoreCase("autobond")) {
					setAutoBond(value);
					break;
				}
				if (key.equalsIgnoreCase("disablePopupMenu")) {
					setDisablePopupMenu(value);
					break;
				}
				if (key.equalsIgnoreCase("forceAutoBond")) {
					setForceAutoBond(value);
					break;
				}
				notFound = true;
				break;
			}
			if (!defineNew)
				return !notFound;
			if (!notFound) {
				global.setPropertyFlag(key, value);
				return true;
			}
			notFound = true;
			break;
		}
		if (!defineNew)
			return !notFound;
		if (notFound) {
			if (!value && !global.htPropertyFlags.containsKey(key)) {
				Logger.error("viewer.setBooleanProperty(" + key + "," + value + ") - unrecognized SET option");
				scriptStatus("Script ERROR: unrecognized SET option: set " + key);
				return false;
			}
		}
		global.setPropertyFlag(key, value);
		if (notFound)
			return false;
		setTainted(true);
		refresh(0, "viewer.setBooleanProperty");
		return true;
	}

	// ////// flags and settings ////////

	boolean getDotSurfaceFlag() {
		return global.dotSurfaceFlag;
	}

	private void setDotSurfaceFlag(boolean TF) {
		global.dotSurfaceFlag = TF;
	}

	boolean getDotsSelectedOnlyFlag() {
		return global.dotsSelectedOnlyFlag;
	}

	private void setDotsSelectedOnlyFlag(boolean TF) {
		global.dotsSelectedOnlyFlag = TF;
	}

	boolean isRangeSelected() {
		return global.rangeSelected;
	}

	private void setRangeSelected(boolean TF) {
		global.rangeSelected = TF;
	}

	boolean isWindowCentered() {
		return transformManager.isWindowCentered();
	}

	private void setWindowCentered(boolean TF) {
		// setBooleanProperty
		transformManager.setWindowCentered(TF);
	}

	boolean isCameraAdjustable() {
		return global.adjustCameraFlag;
	}

	private void setAdjustCamera(boolean TF) {
		global.adjustCameraFlag = TF;
	}

	boolean allowCameraMove() {
		return global.allowCameraMoveFlag;
	}

	private void setAllowCameraMove(boolean TF) {
		global.allowCameraMoveFlag = TF;
	}

	private void setSolventProbeRadius(float radius) {
		// Eval
		global.solventProbeRadius = radius;
	}

	float getSolventProbeRadius() {
		return global.solventProbeRadius;
	}

	float getCurrentSolventProbeRadius() {
		return global.solventOn ? global.solventProbeRadius : 0;
	}

	private void setSolventOn(boolean isOn) {
		// Eval
		global.solventOn = isOn;
	}

	boolean getSolventOn() {
		return global.solventOn;
	}

	private void setAllowStatusReporting(boolean TF) {
		statusManager.setAllowStatusReporting(TF);
	}

	private void setTestFlag1(boolean value) {
		global.testFlag1 = value;
	}

	boolean getTestFlag1() {
		return global.testFlag1;
	}

	boolean getTestFlag2() {
		return global.testFlag2;
	}

	private void setTestFlag2(boolean value) {
		global.testFlag2 = value;
	}

	boolean getTestFlag3() {
		return global.testFlag3;
	}

	private void setTestFlag3(boolean value) {
		global.testFlag3 = value;
	}

	boolean getTestFlag4() {
		return global.testFlag4;
	}

	private void setTestFlag4(boolean value) {
		global.testFlag4 = value;
	}

	public void setPerspectiveDepth(boolean perspectiveDepth) {
		// setBooleanProperty
		// stateManager.setCrystallographicDefaults
		// app preferences dialog
		transformManager.setPerspectiveDepth(perspectiveDepth);
		refresh(0, "Viewer:setPerspectiveDepth()");
	}

	private void setAxesOrientationRasmol(boolean axesOrientationRasmol) {
		// app PreferencesDialog
		// stateManager
		// setBooleanproperty
		transformManager.setAxesOrientationRasmol(axesOrientationRasmol);
		refresh(0, "Viewer:setAxesOrientationRasmol()");
	}

	public boolean getAxesOrientationRasmol() {
		return transformManager.axesOrientationRasmol;
	}

	private void setAxesModeMolecular(boolean TF) {
		global.axesMode = (TF ? JmolConstants.AXES_MODE_MOLECULAR : JmolConstants.AXES_MODE_BOUNDBOX);
		axesAreTainted = true;
	}

	void setAxesModeUnitCell(boolean TF) {
		// stateManager
		// setBooleanproperty
		global.axesMode = (TF ? JmolConstants.AXES_MODE_UNITCELL : JmolConstants.AXES_MODE_BOUNDBOX);
		axesAreTainted = true;
	}

	int getAxesMode() {
		return global.axesMode;
	}

	private void setDisplayCellParameters(boolean displayCellParameters) {
		global.displayCellParameters = displayCellParameters;
	}

	boolean getDisplayCellParameters() {
		return global.displayCellParameters;
	}

	public boolean getPerspectiveDepth() {
		return transformManager.getPerspectiveDepth();
	}

	public void setSelectionHalos(boolean TF) {
		if (getSelectionHaloEnabled() != TF)
			script("selectionHalos " + TF);
	}

	public void setSelectionHaloEnabled(boolean selectionHaloEnabled) {
		loadShape(JmolConstants.SHAPE_HALOS);
		// a frame property, so it is automatically reset
		modelManager.setSelectionHaloEnabled(selectionHaloEnabled);
	}

	public boolean getSelectionHaloEnabled() {
		return modelManager.getSelectionHaloEnabled();
	}

	private void setBondSelectionModeOr(boolean bondSelectionModeOr) {
		// Eval
		global.bondSelectionModeOr = bondSelectionModeOr;
		refresh(0, "Viewer:setBondSelectionModeOr()");
	}

	boolean getBondSelectionModeOr() {
		return global.bondSelectionModeOr;
	}

	boolean getChainCaseSensitive() {
		return global.chainCaseSensitive;
	}

	private void setChainCaseSensitive(boolean chainCaseSensitive) {
		global.chainCaseSensitive = chainCaseSensitive;
	}

	boolean getRibbonBorder() {
		return global.ribbonBorder;
	}

	private void setRibbonBorder(boolean borderOn) {
		global.ribbonBorder = borderOn;
	}

	boolean getCartoonRocketFlag() {
		return global.cartoonRocketFlag;
	}

	private void setCartoonRocketFlag(boolean TF) {
		global.cartoonRocketFlag = TF;
	}

	boolean getHideNameInPopup() {
		return global.hideNameInPopup;
	}

	private void setHideNameInPopup(boolean hideNameInPopup) {
		global.hideNameInPopup = hideNameInPopup;
	}

	public void setNavigationMode(boolean TF) {
		global.navigationMode = TF;
		transformManager.setNavigationMode(TF);
	}

	public boolean getNavigationMode() {
		return global.navigationMode;
	}

	private void setZoomLarge(boolean TF) {
		global.zoomLarge = TF;
		scaleFitToScreen();
	}

	boolean getZoomLarge() {
		return global.zoomLarge;
	}

	private void setTraceAlpha(boolean TF) {
		global.traceAlpha = TF;
	}

	boolean getTraceAlpha() {
		return global.traceAlpha;
	}

	int getHermiteLevel() {
		return global.hermiteLevel;
	}

	private void setHermiteLevel(int level) {
		global.hermiteLevel = level;
	}

	boolean getHighResolution() {
		return global.highResolutionFlag;
	}

	private void setHighResolution(boolean TF) {
		global.highResolutionFlag = TF;
	}

	private void setSsbondsBackbone(boolean TF) {
		// Eval
		global.ssbondsBackbone = TF;
	}

	String getLoadState() {
		return global.getLoadState();
	}

	public void setAutoBond(boolean TF) {
		// setBooleanProperties
		global.autoBond = TF;
	}

	public boolean getAutoBond() {
		return global.autoBond;
	}

	int makeConnections(float minDistance, float maxDistance, short order, int connectOperation, BitSet bsA, BitSet bsB) {
		// eval
		clearAllMeasurements(); // necessary for serialization
		return modelManager.makeConnections(minDistance, maxDistance, order, connectOperation, bsA, bsB);
	}

	// //////////////////////////////////////////////////////////////
	// Graphics3D
	// //////////////////////////////////////////////////////////////

	private void setGreyscaleRendering(boolean greyscaleRendering) {
		global.greyscaleRendering = greyscaleRendering;
		g3d.setGreyscaleMode(greyscaleRendering);
		refresh(0, "Viewer:setGreyscaleRendering()");
	}

	boolean getGreyscaleRendering() {
		return global.greyscaleRendering;
	}

	// XIE: made public
	public void setDisablePopupMenu(boolean disablePopupMenu) {
		global.disablePopupMenu = disablePopupMenu;
	}

	boolean getDisablePopupMenu() {
		return global.disablePopupMenu;
	}

	private void setForceAutoBond(boolean forceAutoBond) {
		global.forceAutoBond = forceAutoBond;
	}

	boolean getForceAutoBond() {
		return global.forceAutoBond;
	}

	// ///////////////////////////////////////////////////////////////
	// delegated to stateManager
	// ///////////////////////////////////////////////////////////////

	public void setPercentVdwAtom(int percentVdwAtom) {
		global.percentVdwAtom = percentVdwAtom;
		setShapeSize(JmolConstants.SHAPE_BALLS, -percentVdwAtom);
	}

	public void setFrankOn(boolean TF) {
		// initializeModel
		global.frankOn = TF;
		setShapeSize(JmolConstants.SHAPE_FRANK, TF ? 1 : 0);
	}

	boolean getFrankOn() {
		return global.frankOn;
	}

	public int getPercentVdwAtom() {
		return global.percentVdwAtom;
	}

	short getMadAtom() {
		return (short) -global.percentVdwAtom;
	}

	public short getMadBond() {
		return (short) (global.marBond * 2);
	}

	public short getMarBond() {
		return global.marBond;
	}

	/*
	 * void setModeMultipleBond(byte modeMultipleBond) { //not implemented global.modeMultipleBond = modeMultipleBond;
	 * refresh(0, "Viewer:setModeMultipleBond()"); }
	 */

	byte getModeMultipleBond() {
		// sticksRenderer
		return global.modeMultipleBond;
	}

	private void setShowMultipleBonds(boolean TF) {
		// Eval.setBonds
		// stateManager
		global.showMultipleBonds = TF;
		refresh(0, "Viewer:setShowMultipleBonds()");
	}

	boolean getShowMultipleBonds() {
		return global.showMultipleBonds;
	}

	private void setShowHydrogens(boolean TF) {
		// PreferencesDialog
		// setBooleanProperty
		global.showHydrogens = TF;
		refresh(0, "Viewer:setShowHydrogens()");
	}

	public boolean getShowHydrogens() {
		return global.showHydrogens;
	}

	private void setShowHiddenSelectionHalos(boolean TF) {
		// setBooleanProperty
		global.showHiddenSelectionHalos = TF;
		refresh(0, "Viewer:setShowHiddenSelectionHalos()");
	}

	public boolean getShowHiddenSelectionHalos() {
		return global.showHiddenSelectionHalos;
	}

	public boolean getShowBbcage() {
		return getShapeShow(JmolConstants.SHAPE_BBCAGE);
	}

	public boolean getShowAxes() {
		return getShapeShow(JmolConstants.SHAPE_AXES);
	}

	private void setShowMeasurements(boolean TF) {
		// setbooleanProperty
		global.showMeasurements = TF;
		refresh(0, "setShowMeasurements()");
	}

	public boolean getShowMeasurements() {
		return global.showMeasurements;
	}

	private void setMeasureAllModels(boolean TF) {
		global.measureAllModels = TF;
	}

	boolean getMeasureAllModelsFlag() {
		return global.measureAllModels;
	}

	boolean setMeasureDistanceUnits(String units) {
		// stateManager
		// Eval
		if (!global.setMeasureDistanceUnits(units))
			return false;
		setShapeProperty(JmolConstants.SHAPE_MEASURES, "reformatDistances", null);
		return true;
	}

	String getMeasureDistanceUnits() {
		return global.getMeasureDistanceUnits();
	}

	private void setDefaults(String type) {
		if (type.equalsIgnoreCase("RasMol")) {
			stateManager.setRasMolDefaults();
			return;
		}
		stateManager.setJmolDefaults();
	}

	private void setZeroBasedXyzRasmol(boolean zeroBasedXyzRasmol) {
		// stateManager
		// setBooleanProperty
		global.zeroBasedXyzRasmol = zeroBasedXyzRasmol;
		modelManager.setZeroBased();
	}

	boolean getZeroBasedXyzRasmol() {
		return global.zeroBasedXyzRasmol;
	}

	// //////////////////////////////////////////////////////////////
	// temp manager
	// //////////////////////////////////////////////////////////////

	Point3f[] allocTempPoints(int size) {
		return tempManager.allocTempPoints(size);
	}

	void freeTempPoints(Point3f[] tempPoints) {
		tempManager.freeTempPoints(tempPoints);
	}

	Point3i[] allocTempScreens(int size) {
		return tempManager.allocTempScreens(size);
	}

	void freeTempScreens(Point3i[] tempScreens) {
		tempManager.freeTempScreens(tempScreens);
	}

	boolean[] allocTempBooleans(int size) {
		return tempManager.allocTempBooleans(size);
	}

	void freeTempBooleans(boolean[] tempBooleans) {
		tempManager.freeTempBooleans(tempBooleans);
	}

	byte[] allocTempBytes(int size) {
		return tempManager.allocTempBytes(size);
	}

	void freeTempBytes(byte[] tempBytes) {
		tempManager.freeTempBytes(tempBytes);
	}

	// //////////////////////////////////////////////////////////////
	// font stuff
	// //////////////////////////////////////////////////////////////
	Font3D getFont3D(int fontSize) {
		return g3d.getFont3D(JmolConstants.DEFAULT_FONTFACE, JmolConstants.DEFAULT_FONTSTYLE, fontSize);
	}

	Font3D getFont3D(String fontFace, String fontStyle, int fontSize) {
		return g3d.getFont3D(fontFace, fontStyle, fontSize);
	}

	private DecimalFormat[] formatters;

	private static String[] formattingStrings = { "0", "0.0", "0.00", "0.000", "0.0000", "0.00000", "0.000000",
			"0.0000000", "0.00000000", "0.000000000" };

	String formatDecimal(float value, int decimalDigits) {
		if (decimalDigits < 0)
			return "" + value;
		if (formatters == null)
			formatters = new DecimalFormat[formattingStrings.length];
		if (decimalDigits >= formattingStrings.length)
			decimalDigits = formattingStrings.length - 1;
		DecimalFormat formatter = formatters[decimalDigits];
		if (formatter == null)
			formatter = formatters[decimalDigits] = new DecimalFormat(formattingStrings[decimalDigits]);
		return formatter.format(value);
	}

	// //////////////////////////////////////////////////////////////
	// Access to atom properties for clients
	// //////////////////////////////////////////////////////////////

	String getElementSymbol(int i) {
		return modelManager.getElementSymbol(i);
	}

	int getElementNumber(int i) {
		return modelManager.getElementNumber(i);
	}

	public String getAtomName(int i) {
		return modelManager.getAtomName(i);
	}

	public int getAtomNumber(int i) {
		return modelManager.getAtomNumber(i);
	}

	float getAtomX(int i) {
		return modelManager.getAtomX(i);
	}

	float getAtomY(int i) {
		return modelManager.getAtomY(i);
	}

	float getAtomZ(int i) {
		return modelManager.getAtomZ(i);
	}

	public Point3f getAtomPoint3f(int i) {
		return modelManager.getAtomPoint3f(i);
	}

	public float getAtomRadius(int i) {
		return modelManager.getAtomRadius(i);
	}

	// XIE
	public float getVdwRadius(int i) {
		return modelManager.getVdwRadius(i);
	}

	public int getAtomArgb(int i) {
		return g3d.getColixArgb(modelManager.getAtomColix(i));
	}

	String getAtomChain(int i) {
		return modelManager.getAtomChain(i);
	}

	public int getAtomModelIndex(int i) {
		return modelManager.getAtomModelIndex(i);
	}

	String getAtomSequenceCode(int i) {
		return modelManager.getAtomSequenceCode(i);
	}

	public Point3f getBondPoint3f1(int i) {
		return modelManager.getBondPoint3f1(i);
	}

	public Point3f getBondPoint3f2(int i) {
		return modelManager.getBondPoint3f2(i);
	}

	public float getBondRadius(int i) {
		return modelManager.getBondRadius(i);
	}

	public short getBondOrder(int i) {
		return modelManager.getBondOrder(i);
	}

	public int getBondArgb1(int i) {
		return g3d.getColixArgb(modelManager.getBondColix1(i));
	}

	public int getBondModelIndex(int i) {
		return modelManager.getBondModelIndex(i);
	}

	public int getBondArgb2(int i) {
		return g3d.getColixArgb(modelManager.getBondColix2(i));
	}

	public Point3f[] getPolymerLeadMidPoints(int modelIndex, int polymerIndex) {
		return modelManager.getPolymerLeadMidPoints(modelIndex, polymerIndex);
	}

	// //////////////////////////////////////////////////////////////
	// stereo support
	// //////////////////////////////////////////////////////////////

	void setStereoMode(int stereoMode, String state) {
		// Eval -- ok; this is set specially
		global.stereoState = state;
		transformManager.setStereoMode(stereoMode);
		setBooleanProperty("greyscaleRendering", stereoMode > JmolConstants.STEREO_DOUBLE);
	}

	void setStereoMode(int[] twoColors, String state) {
		// Eval -- also set specially
		global.stereoState = state;
		transformManager.setStereoMode(twoColors);
		setBooleanProperty("greyscaleRendering", true);
	}

	int getStereoMode() {
		return transformManager.stereoMode;
	}

	float getStereoDegrees() {
		return transformManager.stereoDegrees;
	}

	private void setStereoDegrees(float degrees) {
		// Eval
		transformManager.setStereoDegrees(degrees);
	}

	// //////////////////////////////////////////////////////////////
	//
	// //////////////////////////////////////////////////////////////

	public boolean isJvm12orGreater() {
		return jvm12orGreater;
	}

	public String getOperatingSystemName() {
		return strOSName;
	}

	public String getJavaVendor() {
		return strJavaVendor;
	}

	public String getJavaVersion() {
		return strJavaVersion;
	}

	Graphics3D getGraphics3D() {
		return g3d;
	}

	public boolean showModelSetDownload() {
		return true; // deprecated
	}

	// /////////////// getProperty /////////////

	public Object getProperty(String returnType, String infoType, String paramInfo) {
		// return types include "JSON", "string", "readable", and anything else returns the Java object.
		return propertyManager.getProperty(returnType, infoType, paramInfo);
	}

	String getModelExtract(String atomExpression) {
		BitSet bs = selectionManager.getAtomBitSet(atomExpression);
		return fileManager.getFullPathName() + "\nJmol version " + getJmolVersion() + "\nEXTRACT: " + atomExpression
				+ "\n" + modelManager.getModelExtract(bs);
	}

	String simpleReplace(String str, String strFrom, String strTo) {
		if (str == null)
			return str;
		int fromLength = strFrom.length();
		if (fromLength == 0)
			return str;
		boolean isOnce = (strTo.indexOf(strFrom) >= 0);
		int ipt;
		String stemp = "";
		while (str.indexOf(strFrom) >= 0) {
			int ipt0 = 0;
			while ((ipt = str.indexOf(strFrom, ipt0)) >= 0) {
				stemp += str.substring(ipt0, ipt) + strTo;
				ipt0 = ipt + fromLength;
			}
			str = stemp + str.substring(ipt0, str.length());
			if (isOnce)
				break;
		}
		return str;
	}

	String getHexColorFromIndex(short colix) {
		return g3d.getHexColorFromIndex(colix);
	}

	// ////////////////////////////////////////////////

	void setModelVisibility() {
		// Eval -- ok - handled specially
		modelManager.setModelVisibility();
	}

	boolean isTainted = true;

	public void setTainted(boolean TF) { // XIE
		isTainted = TF && refreshing;
		axesAreTainted = TF && refreshing;
	}

	void checkObjectClicked(int x, int y, int modifiers) {
		modelManager.checkObjectClicked(x, y, modifiers);
	}

	void checkObjectDragged(int prevX, int prevY, int deltaX, int deltaY, int modifiers) {
		modelManager.checkObjectDragged(prevX, prevY, deltaX, deltaY, modifiers);
	}

	int cardinalityOf(BitSet bs) {
		int nbitset = 0;
		for (int i = bs.size(); --i >= 0;)
			if (bs.get(i))
				nbitset++;
		return nbitset;
	}

	/*******************************************************************************************************************
	 * 
	 * methods for spinning and rotating
	 * 
	 ******************************************************************************************************************/

	void rotateAxisAngleAtCenter(Point3f rotCenter, Vector3f rotAxis, float degrees, float endDegrees, boolean isSpin) {
		// Eval: rotate FIXED
		transformManager.rotateAxisAngleAtCenter(rotCenter, rotAxis, degrees, endDegrees, isSpin);
	}

	void rotateAboutPointsInternal(Point3f point1, Point3f point2, float nDegrees, float endDegrees, boolean isSpin) {
		// Eval: rotate INTERNAL
		transformManager.rotateAboutPointsInternal(point1, point2, nDegrees, endDegrees, false, isSpin);
	}

	private void setPickingSpinRate(int rate) {
		// Eval
		if (rate < 1)
			rate = 1;
		global.pickingSpinRate = rate;
	}

	void startSpinningAxis(int atomIndex1, int atomIndex2, boolean isClockwise) {
		// PickingManager.setAtomPicked "set picking SPIN"
		Point3f pt1 = modelManager.getAtomPoint3f(atomIndex1);
		Point3f pt2 = modelManager.getAtomPoint3f(atomIndex2);
		startSpinningAxis(pt1, pt2, isClockwise);
	}

	void startSpinningAxis(Point3f pt1, Point3f pt2, boolean isClockwise) {
		// Draw.checkObjectClicked ** could be difficult
		// from draw object click
		if (getSpinOn()) {
			setSpinOn(false);
			return;
		}
		transformManager
				.rotateAboutPointsInternal(pt1, pt2, global.pickingSpinRate, Float.MAX_VALUE, isClockwise, true);
	}

	Point3f getDrawObjectCenter(String axisID) {
		return modelManager.getSpinCenter(axisID, repaintManager.displayModelIndex);
	}

	Vector3f getDrawObjectAxis(String axisID) {
		return modelManager.getSpinAxis(axisID, repaintManager.displayModelIndex);
	}

	Vector3f getModelDipole() {
		return modelManager.getModelDipole();
	}

	void getBondDipoles() {
		modelManager.getBondDipoles();
		return;
	}

	private void setDipoleScale(float scale) {
		// Eval
		loadShape(JmolConstants.SHAPE_DIPOLES);
		setShapeProperty(JmolConstants.SHAPE_DIPOLES, "dipoleVectorScale", new Float(scale));
	}

	void getAtomIdentityInfo(int atomIndex, Hashtable info) {
		modelManager.getAtomIdentityInfo(atomIndex, info);
	}

	void setDefaultLattice(Point3f ptLattice) {
		// Eval -- handled separately
		global.setDefaultLattice(ptLattice);
	}

	Point3f getDefaultLattice() {
		return global.getDefaultLatticePoint();
	}

	// these functions will throw off the state.
	public void setAtomCoord(int atomIndex, float x, float y, float z) {
		// not implemented -- no script equivalent
		modelManager.setAtomCoord(atomIndex, x, y, z);
	}

	public void setAtomCoordRelative(int atomIndex, float x, float y, float z) {
		// not implemented
		modelManager.setAtomCoordRelative(atomIndex, x, y, z);
	}

	public void setAtomCoordRelative(Point3f offset) {
		// Eval
		modelManager.setAtomCoordRelative(offset, selectionManager.bsSelection);
	}

	float functionXY(String functionName, int x, int y) {
		return statusManager.functionXY(functionName, x, y);
	}

	String eval(String strEval) {
		return statusManager.eval(strEval);
	}

	Point3f[] getAdditionalHydrogens(BitSet atomSet) {
		return modelManager.getAdditionalHydrogens(atomSet);
	}

	private void setHelpPath(String url) {
		// Eval
		global.helpPath = url;
	}

	void getHelp(String what) {
		if (global.helpPath == null)
			global.helpPath = global.defaultHelpPath;
		showUrl(global.helpPath + what);
	}

	// ///////////////////////////////////////////////////////////////
	// delegated to stateManager
	// ///////////////////////////////////////////////////////////////

	/*
	 * Moved from the consoles to viewer, since this could be of general interest, it's more a property of Eval/Viewer,
	 * and the consoles are really just a mechanism for getting user input and sending results, not saving a history of
	 * it all. Ultimately I hope to integrate the mouse picking and possibly periodic updates of position into this
	 * history to get a full history. We'll see! BH 9/2006
	 */

	/**
	 * Adds one or more commands to the command history
	 * 
	 * @param command
	 *            the command to add
	 */
	void addCommand(String command) {
		commandHistory.addCommand(command);
	}

	/**
	 * Removes one command from the command history
	 * 
	 * @return command removed
	 */
	String removeCommand() {
		return commandHistory.removeCommand();
	}

	/**
	 * Options include: ; all n == Integer.MAX_VALUE ; n prev n >= 1 ; next n == -1 ; set max to -2 - n n <= -3 ; just
	 * clear n == -2 ; clear and turn off; return "" n == 0 ; clear and turn on; return "" n == Integer.MIN_VALUE;
	 * 
	 * @param howFarBack
	 *            number of lines (-1 for next line)
	 * @return one or more lines of command history
	 */
	public String getSetHistory(int howFarBack) {
		return commandHistory.getSetHistory(howFarBack);
	}

	// ///////////////////////////////////////////////////////////////
	// image export
	// ///////////////////////////////////////////////////////////////

	void createImage(String type_name) { // or script now
		if (type_name == null)
			return;
		if (type_name.length() == 0)
			type_name = "JPG:jmol.jpg";
		int i = type_name.indexOf(":");
		if (i < 0) {
			i = type_name.length();
			type_name += ":jmol.jpg";
		}
		String type = type_name.substring(0, i);
		String file = type_name.substring(i + 1);
		createImage(file, type, 100);
	}

	public void createImage(String file, String type, int quality) {
		setModelVisibility();
		statusManager.createImage(file, type, quality);
	}

	// ////////unimplemented

	public void setSyncDriver(int syncMode) {
		// it was an idea...
		Logger.debug(htmlName + " viewer setting sync driver " + syncMode);
		statusManager.setSyncDriver(syncMode);
	}

	public int getSyncMode() {
		return statusManager.getSyncMode();
	}
}
