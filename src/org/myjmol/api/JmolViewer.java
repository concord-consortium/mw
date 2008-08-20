/* $RCSfile: JmolViewer.java,v $
 * $Author: qxie $
 * $Date: 2007-11-12 17:16:45 $
 * $Revision: 1.115 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

package org.myjmol.api;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.net.URL;
import java.util.BitSet;
import java.util.Properties;
import java.util.Hashtable;
import java.io.Reader;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

import org.myjmol.viewer.Viewer;

/**
 * This is the high-level API for the JmolViewer for simple access.
 * <p>
 * We will implement a low-level API at some point
 */

abstract public class JmolViewer extends JmolSimpleViewer {

	/** XIE */
	abstract public void cleanUp();

	abstract public void loadingStarted();

	abstract public void loadingCompleted();

	/** XIE */
	abstract public void runScriptImmediatelyWithoutThread(String s);

	/** XIE */
	abstract public float getVdwRadius(int atomIndex);

	/** XIE */
	abstract public void setRoverPainted(boolean b);

	/** XIE */
	abstract public void setRoverPosition(float x, float y, float z);

	/** XIE */
	abstract public void setRoverColor(int argb);

	/** XIE */
	abstract public void setInteractionCentersVisible(boolean b);

	/** XIE */
	abstract public void setEngineOn(boolean b);

	/** XIE: indicate that the atom is clicked but NOT selected */
	abstract public void setClickedAtom(int i);

	/** XIE: indicate that the bond is clicked */
	abstract public void setClickedBond(int i);

	abstract public void setBondSelected(int i, boolean b);

	abstract public void setABondSelected(int i, boolean b);

	abstract public void setTBondSelected(int i, boolean b);

	/** XIE */
	abstract public int getAttachmentHost(byte type, int x, int y, Class c);

	/** XIE */
	abstract public void setActiveKey(byte type, int i, Class c);

	/** XIE */
	abstract public void setAttachment(byte type, int i, boolean b, Class c);

	/** XIE */
	abstract public void setAttachmentKeyColor(byte type, int i, int rgb, Class c);

	/** XIE */
	abstract public boolean hasAttachment(byte type, int i, Class c);

	/** XIE */
	abstract public Point getAttachmentScreenCenter(byte type, int i, Class c);

	/** XIE */
	abstract public void clearKeys(Class c);

	/** XIE */
	abstract public void setDepthCueing(boolean b);

	/** XIE */
	abstract public boolean getDepthCueing();

	/**
	 * XIE: In the Jmol perspective model, the sizes of shapes are determined only by their z-depth's. And the
	 * determination is independent of those of the coordinates. This results in a discrepancy of the projection of size
	 * onto z-direction with the z-components of the screen coordinates. This problem is noticeable only when the shape
	 * is close to the z = 1 plane within a certain distance and angle. In most cases, the consequence is the "loss of
	 * space" between shapes. To fix this problem, we introduce the following magnification to stretch the z-depth. The
	 * recommended value of this parameter is 1-10. The Jmol default is 1, and our default is 5.
	 * 
	 * NOTE: This parameter applies only in navigation mode.
	 */
	abstract public void setZDepthMagnification(int i);

	abstract public int getZDepthMagnification();

	/** XIE */
	abstract public void translateByScreenPixels(int dx, int dy, int dz);

	/** XIE */
	abstract public void setCameraSpin(boolean b);

	/** XIE */
	abstract public boolean isCameraSpin();

	/** XIE: */
	abstract public void moveCameraTo(float sec, float ax, float ay, float az, float deg, float cx, float cy, float cz);

	/** XIE */
	abstract public void moveCameraToScene(Scene s, boolean immediately);

	/** XIE */
	abstract public void clearScriptQueue();

	/** XIE */
	abstract public void setCameraPosition(float x, float y, float z);

	/** XIE */
	abstract public Point3f getCameraPosition();

	/** XIE */
	abstract public Point3f getRotationCenter();

	abstract public void translateToXPercent(float percent);

	abstract public float getTranslationXPercent();

	abstract public void translateToYPercent(float percent);

	abstract public float getTranslationYPercent();

	/** XIE */
	abstract public void setNavigationMode(boolean b);

	/** XIE */
	abstract public boolean getNavigationMode();

	/** XIE */
	abstract public void setDisablePopupMenu(boolean b);

	/** XIE */
	abstract public void setTainted(boolean b);

	/** XIE */
	abstract public void deleteAllBonds();

	/** XIE */
	abstract public void setShowAxes(boolean showAxes);

	/** XIE */
	abstract public void setShowBbcage(boolean showBbcage);

	/** XIE */
	abstract public void setPercentVdwAtom(int percentVdwAtom);

	/** XIE */
	abstract public void setFrankOn(boolean frankOn);

	/** XIE */
	abstract public void setPerspectiveDepth(boolean perspectiveDepth);

	/** XIE */
	abstract public void selectAll();

	/** XIE */
	abstract public void setSelectionSet(BitSet newSelection);

	/** XIE */
	abstract public void setMarBond(short marBond);

	/** XIE */
	abstract public void setAutoBond(boolean autoBond);

	/** XIE */
	abstract public Matrix3f getRotationMatrix();

	/** XIE */
	abstract public void clearABonds();

	/** XIE */
	abstract public void clearTBonds();

	/** XIE */
	abstract public void setABondRendered(boolean b);

	/** XIE */
	abstract public void setTBondRendered(boolean b);

	/** XIE */
	abstract public void setCpkPercent(int index, int percentage);

	/** XIE */
	abstract public void setSpinOn(boolean spinOn);

	/** XIE */
	abstract public boolean getSpinOn();

	/** XIE */
	abstract public void resetDefaultAtomColors();

	/** XIE */
	abstract public void removeAll();

	/** XIE */
	abstract public void removeAtoms(BitSet bs);

	/** XIE */
	abstract public String getCurrentOrientation();

	/** XIE */
	abstract public Point3f getCurrentRotationXyz();

	/** XIE */
	abstract public void setVdwForceLines(Pair[] pairs);

	/** XIE */
	abstract public void setVelocityBitSet(BitSet bs);

	/** XIE */
	abstract public void setVelocityVectorScalingFactor(short s);

	/** XIE */
	abstract public void setIndexOfAtomOfSelectedVelocity(int i);

	/** XIE */
	abstract public void setTranslucentBitSet(BitSet bs);

	/** XIE */
	abstract public void setHidenBitSet(BitSet bs);

	/** XIE */
	abstract public void setTrajectoryBitSet(BitSet bs);

	/** XIE */
	abstract public void setTrajectory(int index, int m, float[] x, float[] y, float[] z);

	/** XIE */
	abstract public void setCharge(int index, float charge);

	/** XIE */
	abstract public void addRBond(Object atomUid1, Object atomUid2);

	/** XIE */
	abstract public void removeRBond(int index);

	/** XIE */
	abstract public void addABond(int i, int j, int k);

	/** XIE */
	abstract public void removeABond(int index);

	/** XIE */
	abstract public void addTBond(int i, int j, int k, int l);

	/** XIE */
	abstract public void removeTBond(int index);

	abstract public void addAtom(Object atomUid, short atomicNumber, String atomName, int formalCharge,
			float partialCharge, float x, float y, float z, float vx, float vy, float vz, Object clientAtomReference);

	abstract public void setAtomCoordinates(int index, float x, float y, float z);

	abstract public void setAtomVelocities(int index, float vx, float vy, float vz);

	abstract public void setAtomCoordinates(int index, Point3f p);

	abstract public void setAtomCoordinates(int index, float x, float y, float z, int argb);

	abstract public void setAtomCoordinates(int index, float x, float y, float z, float d, int argb);

	abstract public void setAtomType(int index, short element, String symbol);

	abstract public void setAtomSize(int index, float d);

	abstract public void setAtomColor(int index, int argb);

	abstract public void setAtomVisibility(int index, boolean b);

	abstract public void setBondVisibility(int index, boolean b);

	/** XIE */
	abstract public Point3i getAtomScreen(int atomIndex);

	/** XIE */
	abstract public Point3i getBondCenterScreen(int bondIndex);

	/** XIE */
	abstract public int[] getBondAtoms(int bondIndex);

	/** XIE */
	abstract public void openClientObject(Object clientObject);

	/** XIE */
	abstract public void setShowRebondTime(boolean b);

	/** XIE */
	abstract public Point getTranslationCenter();

	/** XIE */
	abstract public void translateTo(int x, int y);

	/** XIE */
	abstract public void translateBy(int dx, int dy);

	/** XIE */
	abstract public void setZoomPercent(int i);

	/** XIE: moveTo in the specified time in seconds */
	abstract public void moveTo(float timeInSeconds, float axisX, float axisY, float axisZ, float degrees, int zoom,
			float dx, float dy);

	/** XIE */
	abstract public void stopMotion(boolean b);

	/** XIE */
	abstract public void setRectangleVisible(boolean b);

	/** XIE */
	abstract public void setEllipseVisible(boolean b);

	/** XIE */
	abstract public void setRectangle(char axis, Point3f p1, Point3f p2);

	/** XIE */
	abstract public void setEllipse(char axis, Point3f p1, Point3f p2);

	/** XIE */
	abstract public void setHighlightPlaneVisible(boolean b);

	/** XIE */
	abstract public void setHighlightCylinderVisible(boolean b);

	/** XIE */
	abstract public void setHighlightTriangleVisible(boolean b);

	/** XIE */
	abstract public void setHighlightTriangleVertices(Point3f p1, Point3f p2, Point3f p3);

	/** XIE */
	abstract public void setHighlightTBondVisible(boolean b);

	/** XIE */
	abstract public void setHighlightTBond(int i, int j, int k, int l);

	/** XIE */
	abstract public void addCuboidObstacle();

	/** XIE */
	abstract public void addCylinderObstacle();

	/** XIE */
	abstract public void addCuboidObstacle(float rx, float ry, float rz, float lx, float ly, float lz);

	/** XIE */
	abstract public void addCylinderObstacle(float rx, float ry, float rz, char axis, float h, float r);

	/** XIE */
	abstract public void removeObstacle(int index);

	abstract public void setObstacleLocation(int index, Point3f p);

	abstract public void setObstacleGeometry(int index, float rx, float ry, float rz, float lx, float ly, float lz);

	/** XIE */
	abstract public void setObstacleColor(int index, Color color, boolean translucent);

	/** XIE */
	abstract public void rotateXBy(float x);

	/** XIE */
	abstract public void rotateYBy(float x);

	/** XIE */
	abstract public void rotateZBy(float x);

	/** XIE */
	abstract public void setCenter(float x, float y, float z);

	/** XIE */
	abstract public void fit2DScreen(float pixelsPerAngstrom);

	/** XIE */
	abstract public void setShowCharge(boolean b);

	/** XIE */
	abstract public void setShowAtomIndex(boolean b);

	/** XIE */
	abstract public void setAxisStyle(byte style);

	/** XIE */
	abstract public byte getAxisStyle();

	/** XIE */
	abstract public void setAxisDiameter(int i);

	/** XIE */
	abstract public boolean isHoverEnabled();

	/** XIE */
	abstract public void setHoverEnabled(boolean b);

	/** XIE */
	abstract public void setMeasurementEnabled(boolean b);

	/** XIE */
	abstract public void setBackgroundArgb(int i);

	/** XIE */
	public void setColorBackground(Color c) {
		setBackgroundArgb(c.getRGB());
	}

	/** XIE */
	public Color getColorBackground() {
		return new Color(getBackgroundArgb());
	}

	/** XIE */
	abstract public int findNearestAtomIndex(int x, int y);

	/** XIE */
	abstract public int findNearestBondIndex(int x, int y);

	/** XIE */
	abstract public int findNearestABondIndex(int x, int y);

	/** XIE */
	abstract public int findNearestTBondIndex(int x, int y);

	/** XIE */
	abstract public void updateCuboidObstacleFace(char axis, float center, float corner);

	/** XIE */
	abstract public void updateCylinderObstacleFace(char axis, float center, float a, float b, float height);

	/** XIE */
	abstract public short[] findNearestObstacleIndexAndFace(int x, int y);

	/** XIE */
	abstract public int findNearestAtomIndexOnDropPlane(char direction, int x, int y);

	/** XIE */
	abstract public BitSet findAtomsInRectangle(Rectangle rectRubberBand);

	/** XIE */
	abstract public BitSet findAtomsInOval(Rectangle rectangle);

	/** XIE */
	abstract public void setSimulationBoxVisible(boolean b);

	/** XIE */
	abstract public void setSimulationBox(float xlen, float ylen, float zlen);

	/** XIE */
	abstract public byte getSimulationBoxFace(int x, int y);

	/** XIE */
	abstract public void updateSimulationBoxFace(byte face);

	/** XIE */
	abstract public byte getVectorBoxFace(int x, int y);

	/** XIE */
	abstract public void updateVectorBoxFace(byte face);

	/** XIE */
	abstract public Point3f findPointOnDropPlane(char axis, int x, int y);

	/** XIE */
	abstract public Point3f findPointOnPlane(char axis, int x, int y, float c);

	/** XIE */
	abstract public void setDropPlaneVisible(char axis, boolean visible);

	/** XIE */
	abstract public void translateDropPlane(float dx, float dy, float dz);

	/** XIE */
	abstract public void moveDropPlaneTo(char axis, float coordinate);

	/** XIE */
	abstract public float getDropPlanePosition(char axis);

	/** XIE */
	abstract public void clearMeasurements();

	/** XIE */
	abstract public void clearSelection();

	private boolean mw2dFlag;

	public void setMw2dFlag(boolean b) {
		mw2dFlag = b;
	}

	public boolean getMw2dFlag() {
		return mw2dFlag;
	}

	/** XIE */
	static public JmolViewer allocateExtendedViewer(Component c, JmolAdapter jmolAdapter) {
		return Viewer.allocateExtendedViewer(c, jmolAdapter);
	}

	static public JmolViewer allocateViewer(Component c, JmolAdapter jmolAdapter) {
		return Viewer.allocateViewer(c, jmolAdapter);
	}

	abstract public void setJmolStatusListener(JmolStatusListener jmolStatusListener);

	abstract public void setAppletContext(String htmlName, URL documentBase, URL codeBase, String appletProxy);

	abstract public boolean checkHalt(String strCommand);

	abstract public void haltScriptExecution();

	abstract public boolean isJvm12orGreater();

	abstract public String getOperatingSystemName();

	abstract public String getJavaVersion();

	abstract public String getJavaVendor();

	abstract public boolean haveFrame();

	abstract public void pushHoldRepaint();

	abstract public void popHoldRepaint();

	// change this to width, height
	abstract public void setScreenDimension(Dimension dim);

	abstract public int getScreenWidth();

	abstract public int getScreenHeight();

	abstract public Image getScreenImage();

	abstract public void releaseScreenImage();

	abstract public boolean handleOldJvm10Event(Event e);

	abstract public int getMotionEventNumber();

	abstract public void openReader(String fullPathName, String name, Reader reader);

	abstract public void openClientFile(String fullPathName, String fileName, Object clientFile);

	abstract public void showUrl(String urlString);

	abstract public int getMeasurementCount();

	abstract public String getMeasurementStringValue(int i);

	abstract public int[] getMeasurementCountPlusIndices(int i);

	abstract public Component getAwtComponent();

	abstract public BitSet getElementsPresentBitSet();

	abstract public int getAnimationFps();

	abstract public String script(String script);

	abstract public String scriptCheck(String script);

	abstract public String scriptWait(String script);

	abstract public Object scriptWaitStatus(String script, String statusList);

	abstract public void loadInline(String strModel);

	abstract public void loadInline(String[] arrayModels);

	abstract public void loadInline(String strModel, char newLine);

	abstract public String evalStringQuiet(String script);

	abstract public boolean isScriptExecuting();

	abstract public String getModelSetName();

	abstract public String getModelSetFileName();

	abstract public String getModelSetPathName();

	abstract public Properties getModelSetProperties();

	abstract public Hashtable getModelSetAuxiliaryInfo();

	abstract public int getModelNumber(int atomSetIndex);

	abstract public String getModelName(int atomSetIndex);

	abstract public Properties getModelProperties(int atomSetIndex);

	abstract public String getModelProperty(int atomSetIndex, String propertyName);

	abstract public Hashtable getModelAuxiliaryInfo(int atomSetIndex);

	abstract public Object getModelAuxiliaryInfo(int atomSetIndex, String keyName);

	abstract public boolean modelHasVibrationVectors(int atomSetIndex);

	abstract public int getModelCount();

	abstract public int getDisplayModelIndex(); // can return -2 - modelIndex if a background model is displayed

	abstract public int getAtomCount();

	abstract public int getBondCount(); // NOT THE REAL BOND COUNT -- just an array maximum

	abstract public int getGroupCount();

	abstract public int getChainCount();

	abstract public int getPolymerCount();

	abstract public int getAtomCountInModel(int modelIndex);

	abstract public int getBondCountInModel(int modelIndex); // use -1 here for "all"

	abstract public int getGroupCountInModel(int modelIndex);

	abstract public int getChainCountInModel(int modelIindex);

	abstract public int getPolymerCountInModel(int modelIndex);

	abstract public int getSelectionCount();

	abstract public void addSelectionListener(JmolSelectionListener listener);

	abstract public void removeSelectionListener(JmolSelectionListener listener);

	abstract public BitSet getSelectionSet();

	abstract public void homePosition();

	abstract public Hashtable getHeteroList(int modelIndex);

	abstract public boolean getPerspectiveDepth();

	abstract public boolean getShowHydrogens();

	abstract public boolean getShowMeasurements();

	abstract public boolean getShowAxes();

	abstract public boolean getShowBbcage();

	abstract public int getAtomNumber(int atomIndex);

	abstract public String getAtomName(int atomIndex);

	abstract public String getAtomInfo(int atomIndex);

	abstract public float getRotationRadius();

	abstract public int getZoomPercent();

	abstract public Matrix4f getUnscaledTransformMatrix();

	abstract public int getBackgroundArgb();

	abstract public float getAtomRadius(int atomIndex);

	abstract public Point3f getAtomPoint3f(int atomIndex);

	abstract public int getAtomArgb(int atomIndex);

	abstract public int getAtomModelIndex(int atomIndex);

	abstract public float getBondRadius(int bondIndex);

	abstract public Point3f getBondPoint3f1(int bondIndex);

	abstract public Point3f getBondPoint3f2(int bondIndex);

	abstract public int getBondArgb1(int bondIndex);

	abstract public int getBondArgb2(int bondIndex);

	abstract public short getBondOrder(int bondIndex);

	abstract public int getBondModelIndex(int bondIndex);

	abstract public Point3f[] getPolymerLeadMidPoints(int modelIndex, int polymerIndex);

	abstract public boolean getAxesOrientationRasmol();

	abstract public int getPercentVdwAtom();

	abstract public boolean getAutoBond();

	abstract public short getMadBond();

	abstract public float getBondTolerance();

	abstract public void rebond();

	abstract public float getMinBondDistance();

	abstract public void refresh();

	abstract public void refresh(int isOrientationChange, String strWhy);

	abstract public boolean getBooleanProperty(String propertyName);

	abstract public boolean showModelSetDownload();

	abstract public void repaintView();

	abstract public Object getProperty(String returnType, String infoType, String paramInfo);

	abstract public String getSetHistory(int howFarBack);

	abstract public boolean havePartialCharges();

	abstract public boolean isApplet();

	abstract public String getAltLocListInModel(int modelIndex);

	abstract public String getStateInfo();

	// not really implemented:

	abstract public void setSyncDriver(int syncMode);

	abstract public int getSyncMode();

	// viewer.script("set " + propertyName + " " + value);

	// but NOTE that if you use the following, you are
	// bypassing the script history:
	abstract public void setBooleanProperty(String propertyName, boolean value);

	abstract public void setIntProperty(String propertyName, int value);

	abstract public void setFloatProperty(String propertyName, float value);

	abstract public void setStringProperty(String propertyName, String value);

	abstract public void setModeMouse(int modeMouse); // only MOUSEMODE_NONE, prior to nulling viewer

	// alright, all the following are gone. This is because we need to
	// access Viewer states ONLY via setXXXProperty()

	// abstract public void setColorBackground(String colorName);

	// these are still used by preferences dialog
	// abstract public void setShowHydrogens(boolean showHydrogens);
	// abstract public void setShowMeasurements(boolean showMeasurements);
	// abstract public void setPerspectiveDepth(boolean perspectiveDepth);
	// abstract public void setShowAxes(boolean showAxes);
	// abstract public void setShowBbcage(boolean showBbcage);
	// abstract public void setJmolDefaults();
	// abstract public void setRasmolDefaults();
	// abstract public void setAutoBond(boolean autoBond);
	// abstract public void setMarBond(short marBond);
	// abstract public void setBondTolerance(float bondTolerance);
	// abstract public void setMinBondDistance(float minBondDistance);
	// abstract public void setAxesOrientationRasmol(boolean axesMessedUp);
	// abstract public void setPercentVdwAtom(int percentVdwAtom);

	// unused in Jmol.java; DO NOT USE if you want a complete command history:
	// for each of these the script equivalent is shown
	// abstract public void setAnimationFps(int framesPerSecond);
	// viewer.script("animation fps x.x")
	// abstract public void setFrankOn(boolean frankOn);
	// viewer.script("frank on")
	// abstract public void setDebugScript(boolean debugScript);
	// viewer.script("set logLevel 5/4")
	// viewer.script("set debugScript on/off")
	// abstract public void deleteMeasurement(int i);
	// viewer.script("measures delete " + (i + 1));
	// abstract public void clearMeasurements();
	// viewer.script("measures delete");
	// abstract public void setVectorScale(float vectorScaleValue);
	// viewer.script("vector scale " + vectorScaleValue);
	// abstract public void setVibrationScale(float vibrationScaleValue);
	// viewer.script("vibration scale " + vibrationScaleValue);
	// abstract public void setVibrationPeriod(float vibrationPeriod);
	// viewer.script("vibration " + vibrationPeriod);
	// abstract public void selectAll();
	// viewer.script("select all");
	// abstract public void clearSelection();
	// viewer.script("select none");
	// viewer.script("select ({2 3:6})");
	// abstract public void setSelectionSet(BitSet newSelection);

	// implemented as script equivalents:
	abstract public void setSelectionHaloEnabled(boolean haloEnabled);

	abstract public void setCenterSelected();

	// not used:

	abstract public void rotateFront();

	abstract public void rotateToX(int degrees);

	abstract public void rotateToY(int degrees);

	abstract public void rotateToX(float radians);

	abstract public void rotateToY(float radians);

	abstract public void rotateToZ(float radians);

	/** XIE */
	public void frontView(float distance) {
		if (getNavigationMode()) {
			runScriptImmediatelyWithoutThread("reset");
			Point3f p = getRotationCenter();
			setCameraPosition(p.x, p.y, p.z + distance);
		}
		else {
			evalStringQuiet("moveto 1.0 back;moveto 1.0 front;");
		}
	}

	/** XIE */
	public void backView(float distance) {
		if (getNavigationMode()) {
			runScriptImmediatelyWithoutThread("reset");
			Point3f p = getRotationCenter();
			setCameraPosition(p.x, p.y, p.z - distance);
			evalStringQuiet("moveto 0 back");
		}
		else {
			evalStringQuiet("moveto 1.0 front;moveto 1.0 back;");
		}
	}

	/** XIE */
	public void topView(float distance) {
		if (getNavigationMode()) {
			runScriptImmediatelyWithoutThread("reset");
			Point3f p = getRotationCenter();
			setCameraPosition(p.x, p.y + distance, p.z);
			evalStringQuiet("moveto 0 top");
		}
		else {
			evalStringQuiet("moveto 1.0 front;moveto 1.0 top;");
		}
	}

	/** XIE */
	public void bottomView(float distance) {
		if (getNavigationMode()) {
			runScriptImmediatelyWithoutThread("reset");
			Point3f p = getRotationCenter();
			setCameraPosition(p.x, p.y - distance, p.z);
			evalStringQuiet("moveto 0 bottom");
		}
		else {
			evalStringQuiet("moveto 1.0 front;moveto 1.0 bottom;");
		}
	}

	/** XIE */
	public void leftView(float distance) {
		if (getNavigationMode()) {
			runScriptImmediatelyWithoutThread("reset");
			Point3f p = getRotationCenter();
			setCameraPosition(p.x - distance, p.y, p.z);
			evalStringQuiet("moveto 0 left");
		}
		else {
			evalStringQuiet("moveto 1.0 front;moveto 1.0 left;");
		}
	}

	/** XIE */
	public void rightView(float distance) {
		if (getNavigationMode()) {
			runScriptImmediatelyWithoutThread("reset");
			Point3f p = getRotationCenter();
			setCameraPosition(p.x + distance, p.y, p.z);
			evalStringQuiet("moveto 0 right");
		}
		else {
			evalStringQuiet("moveto 1.0 front;moveto 1.0 right;");
		}
	}

}