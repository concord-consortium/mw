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
package org.myjmol.viewer;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

/**
 * @author Charles Xie
 * 
 */
class MyTransformManager extends TransformManager {

	private final static float CAMERA_DEFAULT_DEPTH = 3.0f;
	private final static float CAMERA_NEAR_DEPTH = 0.5f;

	private Point3f revokedPoint;
	private Point3f untransformPointTemp;
	private Matrix4f untransformMatrixTemp;
	private Vector3f myMoveToAxis;
	private volatile boolean stopMotion;
	private final Point3f cameraCenterScreen = new Point3f();
	private Point3f cameraCenter = new Point3f();
	private boolean isNavigationMode;
	private boolean rotationCenterOnCamera;
	private int zDepthMagnification = 5;

	MyTransformManager(Viewer viewer) {
		super(viewer);
	}

	void homePosition() {
		// reset
		super.homePosition();
		setNavigationMode(isNavigationMode);
	}

	void setZDepthMagnification(int i) {
		if (zDepthMagnification == i)
			return;
		zDepthMagnification = i;
		scaleFitToScreen();
	}

	int getZDepthMagnification() {
		return zDepthMagnification;
	}

	void setRotationCenterOnCamera(boolean b) {
		rotationCenterOnCamera = b;
		if (!b)
			setDefaultRotation();
	}

	boolean isRotationCenterOnCamera() {
		return rotationCenterOnCamera;
	}

	private void setCameraDistance(float depth) {
		if (perspectiveDepth) {
			// there is a chance that screenPixelCount can be zero if the screen size hasn't been set
			cameraDistanceFloat = screenPixelCount == 0 ? 1000 : depth * screenPixelCount;
			cameraDistance = (int) cameraDistanceFloat;
		}
	}

	void setNavigationMode(boolean b) {
		isNavigationMode = b;
		if (b) {
			perspectiveDepth = true;
			setCameraDistance(CAMERA_NEAR_DEPTH);
			if (cameraCenter.x == 0 && cameraCenter.y == 0 && cameraCenter.z == 0)
				cameraCenter.set(fixedRotationCenter);
		}
		else {
			setCameraDistance(CAMERA_DEFAULT_DEPTH);
			cameraCenter.set(0, 0, 0);
			setDefaultRotation();
		}
		scaleFitToScreen();
	}

	void setCameraPosition(float x, float y, float z) {
		cameraCenter.set(x, y, z);
		if (isNavigationMode && rotationCenterOnCamera) {
			fixedRotationCenter.set(cameraCenter);
		}
	}

	Point3f getCameraPosition() {
		return cameraCenter;
	}

	void translateByScreenPixels(int dx, int dy, int dz) {
		boolean p = perspectiveDepth;
		perspectiveDepth = false;
		transformPoint(cameraCenter);
		if (dx != 0)
			point3iScreenTemp.x += dx;
		if (dy != 0)
			point3iScreenTemp.y += dy;
		if (dz != 0)
			point3iScreenTemp.z += dz;
		unTransformPoint(point3iScreenTemp, cameraCenter);
		perspectiveDepth = p;
		if (rotationCenterOnCamera)
			fixedRotationCenter.set(cameraCenter);
	}

	void rotateXYBy(int xDelta, int yDelta) {
		// from mouse action
		if (isNavigationMode) {
			rotateXRadians(yDelta * radiansPerDegree * 0.1f);
			rotateYRadians(xDelta * radiansPerDegree * 0.1f);
		}
		else {
			rotateXRadians(yDelta * radiansPerDegree);
			rotateYRadians(xDelta * radiansPerDegree);
		}
		// System.out.println(viewer.getBoundBoxCenter() + ", " + fixedRotationCenter + " --- " + cameraCenter);
	}

	void zoomBy(int pixels) {
		int max = 20;
		if (pixels > max)
			pixels = max;
		else if (pixels < -max)
			pixels = -max;
		if (isNavigationMode) {
			translateByScreenPixels(0, 0, pixels * 5);
			return;
		}
		float deltaPercent = pixels * zoomPercentSetting * 0.02f;
		if (deltaPercent == 0)
			deltaPercent = pixels > 0 ? 1 : (deltaPercent < 0 ? -1 : 0);
		float percent = deltaPercent + zoomPercentSetting;
		zoomToPercent(percent);
	}

	synchronized void finalizeTransformParameters() {
		calcTransformMatrix();
		calcSlabAndDepthValues();
		haveNotifiedNaN = false;
		// lock in the perspective so that when you change centers there is no jump
		if (windowCentered) {
			matrixTransform.transform(rotationCenterDefault, pointT);
			matrixTransform.transform(fixedRotationCenter, pointT2);
			perspectiveOffset.sub(pointT, pointT2);
		}
		perspectiveOffset.x = xFixedTranslation;
		perspectiveOffset.y = yFixedTranslation;
		if (windowCentered) {
			if (!viewer.isCameraAdjustable()) {
				perspectiveOffset.z = 0;
			}
			else if (isNavigationMode) {
				matrixTransform.transform(cameraCenter, cameraCenterScreen);
				perspectiveOffset.z = cameraCenterScreen.z;
			}
		}
	}

	Point3f reversePerspectiveAdjustments(int x, int y, int z) {
		float x1 = x - perspectiveOffset.x;
		float y1 = y - perspectiveOffset.y;
		float z1 = z;
		if (perspectiveDepth) {
			z1 += perspectiveOffset.z;
			// FIXME: This doesn't use the perspectiveFactor(int z) method to calculate
			float perspectiveFactor = z1 / cameraDistanceFloat;
			x1 *= perspectiveFactor;
			y1 *= perspectiveFactor;
		}
		if (revokedPoint == null)
			revokedPoint = new Point3f(x1, y1, z1);
		else revokedPoint.set(x1, y1, z1);
		return revokedPoint;
	}

	Matrix3f getRotationMatrix() {
		return matrixRotate;
	}

	void fit2DScreen(float pixelsPerAngstrom) {
		if (width == 0 || height == 0 || !viewer.haveFrame())
			return;
		translateCenterTo(width >> 1, height >> 1);
		screenPixelCount = width;
		if (height > screenPixelCount)
			screenPixelCount = height;
		scaleDefaultPixelsPerAngstrom = pixelsPerAngstrom;
		setZoomEnabled(true);
		zoomToPercent(100);
	}

	String getCurrentOrientation() {
		axisangleT.set(matrixRotate);
		float degrees = axisangleT.angle * degreesPerRadian;
		StringBuffer sb = new StringBuffer();
		if (degrees < 0.01f) {
			if (matrixRotate.m00 == -1.0f || matrixRotate.m11 == -1.0f || matrixRotate.m22 == -1.0f) {
				sb.append(" 1000 0 0 -180"); // rear view
			}
			else {
				sb.append(" 0 0 0 0"); // front view
			}
		}
		else {
			vectorT.set(axisangleT.x, axisangleT.y, axisangleT.z);
			vectorT.normalize();
			vectorT.scale(1000);
			truncate0(sb, vectorT.x);
			truncate0(sb, vectorT.y);
			truncate0(sb, vectorT.z);
			truncate1(sb, degrees);
		}
		int zoom = getZoomPercent();
		int tX = (int) getTranslationXPercent();
		int tY = (int) getTranslationYPercent();
		if (zoom != 100 || tX != 0 || tY != 0) {
			sb.append(" ");
			sb.append(zoom);
			if (tX != 0 || tY != 0) {
				sb.append(" ");
				sb.append(tX);
				sb.append(" ");
				sb.append(tY);
			}
		}
		return sb.toString().trim();
	}

	Point3f getRotationXyz() {
		float m20 = matrixRotate.m20;
		float rY = -(float) Math.asin(m20);
		float rX, rZ;
		if (Math.abs(m20 - 1) < Float.MIN_VALUE * 1000000) {
			rX = -(float) Math.atan2(matrixRotate.m12, matrixRotate.m11);
			rZ = 0;
		}
		else {
			rX = (float) Math.atan2(matrixRotate.m21, matrixRotate.m22);
			rZ = (float) Math.atan2(matrixRotate.m10, matrixRotate.m00);
		}
		return new Point3f(rX, rY, rZ);
	}

	// for use in AxesRenderer. Important: scaleFitToScreen resets the x,y-FixedTranslation
	void setPerspectiveDepth2(boolean perspectiveDepth) {
		if (this.perspectiveDepth == perspectiveDepth)
			return;
		this.perspectiveDepth = perspectiveDepth;
	}

	void scaleFitToScreen() {
		// must set this for cases in which viewer doesn't have a frame yet
		xFixedTranslation = width >> 1;
		yFixedTranslation = height >> 1;
		if (width == 0 || height == 0 || !viewer.haveFrame())
			return;
		setTranslationCenterToScreen();
		if (!viewer.getMw2dFlag()) {
			if (viewer.getNavigationMode()) {
				scaleDefaultPixelsPerAngstrom = zDepthMagnification * defaultScaleToScreen(rotationRadius);
			}
			else {
				scaleDefaultPixelsPerAngstrom = defaultScaleToScreen(rotationRadius);
			}
		}
		calcScale("scaleFitToScreen rotrad=" + rotationRadius);
	}

	// reuse untransformPointTemp to reduce memory use and speed up
	void unTransformPoint(Point3i screenPt, Point3f coordPt) {
		if (untransformPointTemp == null)
			untransformPointTemp = new Point3f();
		untransformPointTemp.set(screenPt.x - perspectiveOffset.x, screenPt.y - perspectiveOffset.y, screenPt.z);
		if (perspectiveDepth) {
			float inversePerspectiveFactor = 1.0f / perspectiveFactor(untransformPointTemp.z);
			untransformPointTemp.x *= inversePerspectiveFactor;
			untransformPointTemp.y *= inversePerspectiveFactor;
		}
		untransformPointTemp.z += perspectiveOffset.z;
		if (untransformMatrixTemp == null)
			untransformMatrixTemp = new Matrix4f();
		untransformMatrixTemp.invert(matrixTransform);
		untransformMatrixTemp.transform(untransformPointTemp, coordPt);
	}

	void stopMotion(boolean b) {
		stopMotion = b;
	}

	/*
	 * WARNING: this method is intended to be used by an external thread. This moves the camera to the specified
	 * coordinates.
	 */
	void moveCameraTo(float seconds, float ax, float ay, float az, float deg, float cx, float cy, float cz) {
		setMatrixEnd(ax, ay, az, deg);
		move(seconds, zoomPercent, 0, 0, cx, cy, cz);
	}

	/*
	 * WARNING: this method is intended to be used by an external thread. This rotates and zooms the view as the old
	 * jmol moveTo.
	 */
	void myMoveTo(float floatSecondsTotal, float axisX, float axisY, float axisZ, float degrees, float zoom,
			float xTrans, float yTrans) {
		setMatrixEnd(axisX, axisY, axisZ, degrees);
		move(floatSecondsTotal, zoom, xTrans, yTrans, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
	}

	private void setMatrixEnd(float axisX, float axisY, float axisZ, float degrees) {
		if (myMoveToAxis == null)
			myMoveToAxis = new Vector3f(axisX, axisY, axisZ);
		else myMoveToAxis.set(axisX, axisY, axisZ);
		initializeMoveTo();
		if (Float.isNaN(degrees)) {
			getRotation(matrixEnd);
		}
		else if (degrees < 0.01f && degrees > -0.01f) {
			matrixEnd.setIdentity();
		}
		else {
			if (myMoveToAxis.x == 0 && myMoveToAxis.y == 0 && myMoveToAxis.z == 0)
				return;
			aaMoveTo.set(myMoveToAxis, degrees * (float) Math.PI / 180);
			matrixEnd.set(aaMoveTo);
		}
		stopMotion = false;
	}

	private void move(float floatSecondsTotal, float zoom, float xTrans, float yTrans, float navX, float navY,
			float navZ) {
		boolean navInit = navX != Float.MAX_VALUE && navY != Float.MAX_VALUE && navZ != Float.MAX_VALUE;
		ptCenter = fixedRotationCenter;
		float startRotationRadius = rotationRadius;
		float targetRotationRadius = rotationRadius;
		float startPixelScale = scaleDefaultPixelsPerAngstrom;
		float targetPixelScale = startPixelScale;
		getRotation(matrixStart);
		matrixInverse.invert(matrixStart);
		matrixStep.mul(matrixEnd, matrixInverse);
		aaTotal.set(matrixStep);
		int fps = 20;
		int totalSteps = (int) (floatSecondsTotal * fps);
		viewer.setInMotion(true);
		if (totalSteps > 1) {
			int frameTimeMillis = 1000 / fps;
			long targetTime = System.currentTimeMillis();
			float zoomStart = zoomPercent;
			float zoomDelta = zoom - zoomStart;
			float xTransStart = getTranslationXPercent();
			float xTransDelta = xTrans - xTransStart;
			float yTransStart = getTranslationYPercent();
			float yTransDelta = yTrans - yTransStart;
			float xNavDelta = 0;
			float yNavDelta = 0;
			float zNavDelta = 0;
			float xNavStart = Float.MIN_VALUE;
			float yNavStart = Float.MIN_VALUE;
			float zNavStart = Float.MIN_VALUE;
			if (cameraCenter != null && navInit) {
				xNavStart = cameraCenter.x;
				yNavStart = cameraCenter.y;
				zNavStart = cameraCenter.z;
				xNavDelta = navX - xNavStart;
				yNavDelta = navY - yNavStart;
				zNavDelta = navZ - zNavStart;
			}
			aaStepCenter.set(ptCenter);
			aaStepCenter.sub(fixedRotationCenter);
			aaStepCenter.scale(1f / totalSteps);
			float pixelScaleDelta = targetPixelScale - startPixelScale;
			float rotationRadiusDelta = targetRotationRadius - startRotationRadius;
			for (int iStep = 1; iStep < totalSteps; ++iStep) {
				if (stopMotion)
					break;
				getRotation(matrixStart);
				matrixInverse.invert(matrixStart);
				matrixStep.mul(matrixEnd, matrixInverse);
				aaTotal.set(matrixStep);
				aaStep.set(aaTotal);
				aaStep.angle /= (totalSteps - iStep);
				if (aaStep.angle == 0)
					matrixStep.setIdentity();
				else matrixStep.set(aaStep);
				matrixStep.mul(matrixStart);
				float fStep = iStep / (totalSteps - 1f);
				if (cameraCenter != null && xNavStart != Float.MIN_VALUE && yNavStart != Float.MIN_VALUE
						&& zNavStart != Float.MIN_VALUE) {
					setCameraPosition(xNavStart + xNavDelta * fStep, yNavStart + yNavDelta * fStep, zNavStart
							+ zNavDelta * fStep);
				}
				rotationRadius = startRotationRadius + rotationRadiusDelta * fStep;
				scaleDefaultPixelsPerAngstrom = startPixelScale + pixelScaleDelta * fStep;
				zoomToPercent(zoomStart + zoomDelta * fStep);
				translateToXPercent(xTransStart + xTransDelta * fStep);
				translateToYPercent(yTransStart + yTransDelta * fStep);
				setRotation(matrixStep);
				targetTime += frameTimeMillis;
				if (!stopMotion) {
					if (System.currentTimeMillis() < targetTime) {
						viewer.requestRepaintAndWait();
						int sleepTime = (int) (targetTime - System.currentTimeMillis());
						if (sleepTime > 0) {
							try {
								Thread.sleep(sleepTime);
							}
							catch (InterruptedException ie) {
							}
						}
					}
				}
			}
		}
		if (navInit && cameraCenter != null)
			setCameraPosition(navX, navY, navZ);
		rotationRadius = targetRotationRadius;
		scaleDefaultPixelsPerAngstrom = targetPixelScale;
		zoomToPercent(zoom);
		translateToXPercent(xTrans);
		translateToYPercent(yTrans);
		setRotation(matrixEnd);
		viewer.setInMotion(false);
		stopMotion = false;
	}

}