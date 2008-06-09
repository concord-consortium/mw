/* $RCSfile: RepaintManager.java,v $
 * $Author: qxie $
 * $Date: 2007-10-31 17:36:15 $
 * $Revision: 1.14 $
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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.myjmol.viewer;


import org.myjmol.g3d.*;
import org.myjmol.util.Logger;

import java.awt.Rectangle;
import java.util.Hashtable;
import java.util.BitSet;

class RepaintManager {

	Viewer viewer;
	FrameRenderer frameRenderer;

	RepaintManager(Viewer viewer) {
		this.viewer = viewer;
		frameRenderer = new ExtendedFrameRenderer(viewer); // XIE
	}

	int displayModelIndex = 0;

	void setDisplayModelIndex(int modelIndex) {
		Frame frame = viewer.getFrame();
		if (frame == null || modelIndex < 0 || modelIndex >= frame.getModelCount())
			displayModelIndex = -1;
		else displayModelIndex = modelIndex;
		if (displayModelIndex == -1)
			setBackgroundModelIndex(-1);
		viewer.setTainted(true);
		setStatusFrameChanged();
		setFrameRangeVisible();
	}

	void setStatusFrameChanged() {
		viewer.setStatusFrameChanged(animationOn ? -2 - displayModelIndex : displayModelIndex);
	}

	int backgroundModelIndex = -1;

	void setBackgroundModelIndex(int modelIndex) {
		// no background unless only a SINGLE model is being displayed (for now)
		Frame frame = viewer.getFrame();
		if (frame == null || modelIndex < 0 || modelIndex >= frame.getModelCount() || displayModelIndex == -1)
			modelIndex = -1;
		backgroundModelIndex = modelIndex;
		viewer.setTainted(true);
		setFrameRangeVisible();
	}

	private BitSet bsVisibleFrames = new BitSet();

	BitSet getVisibleFramesBitSet() {
		return bsVisibleFrames;
	}

	private void setFrameRangeVisible() {
		bsVisibleFrames.clear();
		if (displayModelIndex >= 0) {
			bsVisibleFrames.set(displayModelIndex);
			if (backgroundModelIndex >= 0)
				bsVisibleFrames.set(backgroundModelIndex);
			return;
		}
		if (frameStep == 0)
			return;
		for (int i = firstModelIndex; i != lastModelIndex; i += frameStep)
			bsVisibleFrames.set(i);
		bsVisibleFrames.set(lastModelIndex);
		return;
	}

	AnimationThread animationThread;

	boolean inMotion = false;

	void setInMotion(boolean inMotion) {
		this.inMotion = inMotion;
		if (!inMotion)
			refresh();
	}

	int holdRepaint = 0;
	boolean repaintPending;

	void pushHoldRepaint() {
		++holdRepaint;
	}

	void popHoldRepaint() {
		--holdRepaint;
		if (holdRepaint <= 0) {
			holdRepaint = 0;
			repaintPending = true;
			viewer.repaint();
			// System.out.println("RepaintManager:popHoldRepaint - "+Thread.currentThread());
		}
	}

	void refresh() {
		if (repaintPending)
			return;
		repaintPending = true;
		if (holdRepaint == 0) {
			viewer.repaint();
		}
	}

	synchronized void requestRepaintAndWait() {
		viewer.repaint();
		try {
			wait();
		}
		catch (InterruptedException e) {
		}
	}

	synchronized void repaintDone() {
		repaintPending = false;
		notify(); // to cancel any wait in requestRepaintAndWait()
	}

	final Rectangle rectOversample = new Rectangle();
	boolean tOversample;

	void setOversample(boolean tOversample) {
		this.tOversample = tOversample;
	}

	void render(Graphics3D g3d, Rectangle rectClip, Frame frame, int displayModelID) {
		frameRenderer.render(g3d, rectClip, frame, displayModelID);
		Rectangle band = viewer.getRubberBandSelection();
		if (band != null)
			g3d.drawRect(viewer.getColixRubberband(), band.x, band.y, 0, 0, band.width, band.height);
	}

	/*******************************************************************************************************************
	 * Animation support
	 ******************************************************************************************************************/

	int firstModelIndex;
	int lastModelIndex;
	int frameStep;
	int modelCount;

	void initializePointers(int frameStep) {
		firstModelIndex = 0;
		modelCount = (frameStep == 0 ? 0 : viewer.getModelCount());
		lastModelIndex = modelCount - 1;
		this.frameStep = frameStep;
	}

	void clear() {
		clearAnimation();
		frameRenderer.clear();
	}

	void clearAnimation() {
		setAnimationOn(false);
		setDisplayModelIndex(0);
		setAnimationDirection(1);
		setAnimationFps(10);
		setAnimationReplayMode(0, 0, 0);
		initializePointers(0);
	}

	Hashtable getAnimationInfo() {
		Hashtable<String, Object> info = new Hashtable<String, Object>();
		info.put("firstModelIndex", new Integer(firstModelIndex));
		info.put("lastModelIndex", new Integer(lastModelIndex));
		info.put("animationDirection", new Integer(animationDirection));
		info.put("currentDirection", new Integer(currentDirection));
		info.put("displayModelIndex", new Integer(displayModelIndex));
		info.put("displayModelNumber", new Integer(displayModelIndex >= 0 ? viewer.getModelNumber(displayModelIndex)
				: 0));
		info.put("displayModelName", (displayModelIndex >= 0 ? viewer.getModelName(displayModelIndex) : ""));
		info.put("animationFps", new Integer(animationFps));
		info.put("animationReplayMode", new Integer(animationReplayMode));
		info.put("firstFrameDelay", new Float(firstFrameDelay));
		info.put("lastFrameDelay", new Float(lastFrameDelay));
		info.put("animationOn", new Boolean(animationOn));
		info.put("animationPaused", new Boolean(animationPaused));
		return info;
	}

	String getState() {
		if (modelCount < 2)
			return "";
		StringBuffer commands = new StringBuffer("# frame state;\n# modelCount " + modelCount + ";\n# first "
				+ viewer.getModelNumber(0) + ";\n# last " + viewer.getModelNumber(modelCount - 1) + ";\n");
		if (backgroundModelIndex >= 0)
			commands.append("background model " + backgroundModelIndex + ";\n");
		if (displayModelIndex >= 0) {
			commands.append("frame RANGE " + viewer.getModelNumber(firstModelIndex) + " "
					+ viewer.getModelNumber(lastModelIndex) + ";\n");
			commands.append("animation DIRECTION " + (animationDirection == 1 ? "+1" : "-1") + ";\n");
			commands.append("animation " + (animationOn ? "ON" : "OFF") + ";\n");
			if (animationOn && animationPaused)
				commands.append("animation PAUSE;\n");
			commands.append("frame " + viewer.getModelNumber(displayModelIndex) + ";\n");
		}
		else {
			commands.append("frame ALL;\n");
		}
		commands.append("\n");
		return commands.toString();
	}

	int animationDirection = 1;
	int currentDirection = 1;

	void setAnimationDirection(int animationDirection) {
		this.animationDirection = animationDirection;
		currentDirection = 1;
	}

	int animationFps = 10;

	void setAnimationFps(int animationFps) {
		if (animationFps >= 1 && animationFps <= 50)
			this.animationFps = animationFps;
		else Logger.error("invalid animationFps:" + animationFps);
	}

	// 0 = once
	// 1 = loop
	// 2 = palindrome
	int animationReplayMode = 0;
	float firstFrameDelay, lastFrameDelay;
	int firstFrameDelayMs, lastFrameDelayMs;

	void setAnimationReplayMode(int animationReplayMode, float firstFrameDelay, float lastFrameDelay) {
		this.firstFrameDelay = firstFrameDelay > 0 ? firstFrameDelay : 0;
		firstFrameDelayMs = (int) (this.firstFrameDelay * 1000);
		this.lastFrameDelay = lastFrameDelay > 0 ? lastFrameDelay : 0;
		lastFrameDelayMs = (int) (this.lastFrameDelay * 1000);
		if (animationReplayMode >= 0 && animationReplayMode <= 2)
			this.animationReplayMode = animationReplayMode;
		else Logger.error("invalid animationReplayMode:" + animationReplayMode);
	}

	void setAnimationRange(int framePointer, int framePointer2) {
		modelCount = viewer.getModelCount();
		if (framePointer < 0)
			framePointer = 0;
		if (framePointer2 < 0)
			framePointer2 = modelCount;
		if (framePointer >= modelCount)
			framePointer = modelCount - 1;
		if (framePointer2 >= modelCount)
			framePointer2 = modelCount - 1;
		firstModelIndex = framePointer;
		lastModelIndex = framePointer2;
		frameStep = (framePointer2 < framePointer ? -1 : 1);
		rewindAnimation();
	}

	boolean animationOn = false;
	boolean animationPaused = false;

	void setAnimationOn(boolean animationOn) {
		if (!animationOn || !viewer.haveFrame()) {
			setAnimationOff(false);
			return;
		}
		viewer.refresh(0, "Viewer:setAnimationOn");
		setAnimationRange(-1, -1);
		resumeAnimation();
	}

	void setAnimationOff(boolean isPaused) {
		if (animationThread != null) {
			animationThread.interrupt();
			animationThread = null;
		}
		animationPaused = isPaused;
		viewer.refresh(0, "Viewer:setAnimationOff");
		animationOn = false;
		setStatusFrameChanged();
	}

	void pauseAnimation() {
		setAnimationOff(true);
	}

	void reverseAnimation() {
		currentDirection = -currentDirection;
		if (!animationOn)
			resumeAnimation();
	}

	int intAnimThread = 0;

	void resumeAnimation() {
		if (displayModelIndex < 0)
			setAnimationRange(firstModelIndex, lastModelIndex);
		if (modelCount <= 1) {
			animationOn = false;
			return;
		}
		animationOn = true;
		animationPaused = false;
		if (animationThread == null) {
			intAnimThread++;
			animationThread = new AnimationThread(firstModelIndex, lastModelIndex, intAnimThread);
			animationThread.start();
		}
	}

	boolean setAnimationNext() {
		return setAnimationRelative(animationDirection);
	}

	void rewindAnimation() {
		setDisplayModelIndex(animationDirection > 0 ? firstModelIndex : lastModelIndex);
		currentDirection = 1;
	}

	boolean setAnimationPrevious() {
		return setAnimationRelative(-animationDirection);
	}

	boolean setAnimationRelative(int direction) {
		int frameStep = this.frameStep * direction * currentDirection;
		int modelIndexNext = displayModelIndex + frameStep;
		boolean isDone = (modelIndexNext > firstModelIndex && modelIndexNext > lastModelIndex || modelIndexNext < firstModelIndex
				&& modelIndexNext < lastModelIndex);

		/*
		 * Logger.debug("setAnimationRelative: " + " firstModelIndex=" + firstModelIndex + " displayModelIndex=" +
		 * displayModelIndex + " lastModelIndex=" + lastModelIndex + " currentDirection=" + currentDirection + "
		 * direction=" + direction + " isDone="+isDone + " modelIndexNext=" + modelIndexNext + " modelCount=" +
		 * modelCount + " animationReplayMode=" + animationReplayMode + " animationDirection=" + animationDirection);
		 */
		if (isDone) {
			switch (animationReplayMode) {
			case 0: // once through
				return false;
			case 1: // repeat
				modelIndexNext = (animationDirection > 0 ? firstModelIndex : lastModelIndex);
				break;
			case 2: // palindrome
				currentDirection = -currentDirection;
				modelIndexNext -= 2 * frameStep;
			}
		}
		// Logger.debug("next="+modelIndexNext+" dir="+currentDirection+" isDone="+isDone);
		if (modelIndexNext < 0 || modelIndexNext >= modelCount)
			return false;
		setDisplayModelIndex(modelIndexNext);
		return true;
	}

	class AnimationThread extends Thread implements Runnable {
		final int framePointer;
		final int framePointer2;
		int intThread;

		AnimationThread(int framePointer, int framePointer2, int intAnimThread) {
			this.framePointer = framePointer;
			this.framePointer2 = framePointer2;
			intThread = intAnimThread;
		}

		public void run() {
			long timeBegin = System.currentTimeMillis();
			int targetTime = 0;
			int sleepTime;
			Logger.debug("animation thread " + intThread + " running");
			requestRepaintAndWait();
			try {
				sleepTime = targetTime - (int) (System.currentTimeMillis() - timeBegin);
				if (sleepTime > 0)
					Thread.sleep(sleepTime);
				while (!isInterrupted()) {
					if (displayModelIndex == framePointer) {
						targetTime += firstFrameDelayMs;
						sleepTime = targetTime - (int) (System.currentTimeMillis() - timeBegin);
						if (sleepTime > 0)
							Thread.sleep(sleepTime);
					}
					if (displayModelIndex == framePointer2) {
						targetTime += lastFrameDelayMs;
						sleepTime = targetTime - (int) (System.currentTimeMillis() - timeBegin);
						if (sleepTime > 0)
							Thread.sleep(sleepTime);
					}
					if (!setAnimationNext()) {
						Logger.debug("animation thread " + intThread + " exiting");
						setAnimationOff(false);
						return;
					}
					targetTime += (1000 / animationFps);
					sleepTime = targetTime - (int) (System.currentTimeMillis() - timeBegin);
					if (sleepTime < 0)
						continue;
					refresh();
					sleepTime = targetTime - (int) (System.currentTimeMillis() - timeBegin);
					if (sleepTime > 0)
						Thread.sleep(sleepTime);
				}
			}
			catch (InterruptedException ie) {
				Logger.debug("animation thread interrupted!");
				setAnimationOn(false);
			}
		}
	}

}
