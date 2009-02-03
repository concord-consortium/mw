/* $RCSfile: StatusManager.java,v $
 * $Author: qxie $
 * $Date: 2007-03-27 18:22:42 $
 * $Revision: 1.2 $
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
package org.myjmol.viewer;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.myjmol.api.*;
import org.myjmol.i18n.GT;
import org.myjmol.util.Logger;

/**
 * 
 * The StatusManager class handles all details of status reporting, including:
 * 
 * 1) saving the message in a queue that replaces the "callback" mechanism, 2) sending messages off to the console, and
 * 3) delivering messages back to the main Jmol.java class in app or applet to handle differences in capabilities,
 * including true callbacks.
 * 
 * atomPicked
 * 
 * fileLoaded fileLoadError frameChanged
 * 
 * measureCompleted measurePending measurePicked
 * 
 * newOrientation
 * 
 * scriptEcho scriptError scriptMessage scriptStarted scriptStatus scriptTerminated
 * 
 * userAction viewerRefreshed
 * 
 * 
 * Bob Hanson hansonr@stolaf.edu 2/2006
 * 
 */

class StatusManager {

	boolean allowStatusReporting = true;

	void setAllowStatusReporting(boolean TF) {
		allowStatusReporting = TF;
	}

	Viewer viewer;
	JmolStatusListener jmolStatusListener;
	String statusList = "";
	Hashtable messageQueue = new Hashtable();
	int statusPtr = 0;
	static int MAXIMUM_QUEUE_LENGTH = 16;
	String compileError;

	StatusManager(Viewer viewer) {
		this.viewer = viewer;
	}

	void clear() {
		setStatusFileLoaded(null, null, null, null, null, 0);
	}

	synchronized boolean resetMessageQueue(String statusList) {
		boolean isRemove = (statusList.length() > 0 && statusList.charAt(0) == '-');
		boolean isAdd = (statusList.length() > 0 && statusList.charAt(0) == '+');
		String oldList = this.statusList;
		if (isRemove) {
			this.statusList = viewer.simpleReplace(oldList, statusList.substring(1, statusList.length()), "");
			messageQueue = new Hashtable();
			statusPtr = 0;
			return true;
		}
		statusList = viewer.simpleReplace(statusList, "+", "");
		if (oldList.equals(statusList) || isAdd && oldList.indexOf(statusList) >= 0)
			return false;
		if (!isAdd) {
			messageQueue = new Hashtable();
			statusPtr = 0;
			this.statusList = "";
		}
		this.statusList += statusList;
		Logger.debug(oldList + "\nmessageQueue = " + this.statusList);
		return true;
	}

	synchronized void setJmolStatusListener(JmolStatusListener jmolStatusListener) {
		this.jmolStatusListener = jmolStatusListener;
	}

	synchronized boolean setStatusList(String statusList) {
		return resetMessageQueue(statusList);
	}

	synchronized void setCallbackFunction(String callbackType, String callbackFunction) {
		if (jmolStatusListener != null)
			jmolStatusListener.setCallbackFunction(callbackType, callbackFunction);
	}

	synchronized void setStatusAtomPicked(int atomIndex, String strInfo) {
		if (atomIndex < 0)
			return;
		Logger.info("setStatusAtomPicked(" + atomIndex + "," + strInfo + ")");
		setStatusChanged("atomPicked", atomIndex, strInfo, false);
		if (jmolStatusListener != null)
			jmolStatusListener.notifyAtomPicked(atomIndex, strInfo);
	}

	synchronized void setStatusAtomHovered(int iatom, String strInfo) {
		if (jmolStatusListener != null)
			jmolStatusListener.notifyAtomHovered(iatom, strInfo);
	}

	synchronized void setStatusFileLoaded(String fullPathName, String fileName, String modelName, Object clientFile,
			String errorMsg, int ptLoad) {
		setStatusChanged("fileLoaded", ptLoad, fullPathName, false);
		if (errorMsg != null)
			setStatusChanged("fileLoadError", ptLoad, errorMsg, false);
		if (jmolStatusListener != null && (ptLoad == -1 || ptLoad == 3))
			jmolStatusListener.notifyFileLoaded(fullPathName, fileName, modelName, clientFile, errorMsg);
	}

	synchronized void setStatusFrameChanged(int frameNo) {
		boolean isAnimationRunning = (frameNo <= -2);
		int f = frameNo;
		if (isAnimationRunning)
			f = -2 - f;
		setStatusChanged("frameChanged", frameNo, (f >= 0 ? viewer.getModelName(f) : ""), false);
		if (jmolStatusListener != null)
			jmolStatusListener.notifyFrameChanged(frameNo);
	}

	synchronized void setStatusNewPickingModeMeasurement(int iatom, String strMeasure) {
		setStatusChanged("measurePicked", iatom, strMeasure, false);
		Logger.info("measurePicked " + iatom + " " + strMeasure);
		if (jmolStatusListener != null)
			jmolStatusListener.notifyNewPickingModeMeasurement(iatom, strMeasure);
	}

	synchronized void setStatusNewDefaultModeMeasurement(String status, int count, String strMeasure) {
		setStatusChanged(status, count, strMeasure, false);
		if (status == "measureCompleted")
			Logger.info("measurement[" + count + "] = " + strMeasure);
		if (jmolStatusListener != null)
			jmolStatusListener.notifyNewDefaultModeMeasurement(count, status + ": " + strMeasure);
	}

	synchronized void setStatusScriptStarted(int iscript, String script, String compileError) {
		this.compileError = compileError;
		if (compileError == null)
			compileError = GT._("Jmol executing script ...");
		else compileError = "Script ERROR: " + compileError;
		setStatusChanged("scriptStarted", iscript, script, false);
		setStatusChanged("scriptMessage", 0, compileError, false);
		if (jmolStatusListener != null)
			jmolStatusListener.notifyScriptStart(compileError, script);
	}

	synchronized void setStatusScriptTermination(String statusMessage, int msWalltime) {
		statusMessage = "Jmol script terminated" + (compileError == null ? "" : " ERROR: " + compileError);
		// UNRELIABLE setStatusChanged("scriptTerminated", msWalltime, statusMessage, false);
		if (jmolStatusListener == null)
			return;
		jmolStatusListener.notifyScriptTermination(statusMessage, msWalltime);
	}

	synchronized void setStatusUserAction(String strInfo) {
		Logger.info("userAction(" + strInfo + ")");
		if (isSynced)
			syncSend("SLAVE", null);
		drivingSync = true;
		setStatusChanged("userAction", 0, strInfo, false);
	}

	synchronized void setScriptEcho(String strEcho) {
		if (strEcho == null)
			return;
		setStatusChanged("scriptEcho", 0, strEcho, false);
		if (jmolStatusListener != null)
			jmolStatusListener.sendConsoleEcho(strEcho);
	}

	synchronized void setScriptStatus(String strStatus) {
		if (strStatus == null)
			return;
		boolean isError = strStatus.indexOf("ERROR:") >= 0;
		setStatusChanged((isError ? "scriptError" : "scriptStatus"), 0, strStatus, false);

		if (isError || strStatus.equals("Script completed"))
			setStatusChanged("scriptTerminated", 1, "Jmol script terminated"
					+ (isError ? " unsuccesscully: " + strStatus : " successfully"), false);

		if (jmolStatusListener != null)
			jmolStatusListener.sendConsoleMessage(strStatus);
	}

	int minSyncRepeatMs = 100;
	int lastSyncTimeMs = 0;

	synchronized void setStatusViewerRefreshed(int isOrientationChange, String strWhy) {
		if (isOrientationChange == 1) {
			setStatusChanged("newOrientation", 0, strWhy, true);
			if (isSynced && drivingSync) {
				int time = (int) System.currentTimeMillis();
				Logger.debug(" syncing" + time + " " + lastSyncTimeMs + " " + minSyncRepeatMs);
				if (lastSyncTimeMs == 0 || time - lastSyncTimeMs >= minSyncRepeatMs) {
					lastSyncTimeMs = time;
					Logger.debug("sending sync");
					syncSend(viewer.getMoveToText(minSyncRepeatMs / 1000f), null);
				}
			}
		}
		else {
			setStatusChanged("viewerRefreshed", 0, strWhy, false);
		}
	}

	synchronized void popupMenu(int x, int y) {
		if (jmolStatusListener != null)
			jmolStatusListener.handlePopupMenu(x, y);
	}

	boolean drivingSync = false;
	boolean isSynced = false;

	public void setSyncDriver(int syncMode) {

		// -1 slave turn off driving, but not syncing
		// 0 off
		// 1 driving on as driver

		// 2 sync turn on, but set as slave

		drivingSync = (syncMode == 1 ? true : false);
		isSynced = (syncMode > 0 || isSynced && syncMode < 0 ? true : false);
		Logger.debug(viewer.getHtmlName() + " " + syncMode + " synced? " + isSynced + " driving?" + drivingSync);
	}

	public void syncSend(String script, String appletName) {
		if (jmolStatusListener != null)
			jmolStatusListener.sendSyncScript(script, appletName);
	}

	public int getSyncMode() {
		if (!isSynced)
			return 0;
		return (drivingSync ? 1 : -1);
	}

	synchronized void showUrl(String urlString) {
		if (jmolStatusListener != null)
			jmolStatusListener.showUrl(urlString);
	}

	synchronized void clearConsole() {
		if (jmolStatusListener != null)
			jmolStatusListener.sendConsoleMessage(null);
	}

	synchronized void showConsole(boolean showConsole) {
		if (jmolStatusListener != null)
			jmolStatusListener.showConsole(showConsole);
	}

	// //////////////////Jmol status //////////////

	@SuppressWarnings("unchecked")
	synchronized void setStatusChanged(String statusName, int intInfo, Object statusInfo, boolean isReplace) {
		if (!allowStatusReporting || statusList != "all" && statusList.indexOf(statusName) < 0)
			return;
		statusPtr++;
		Vector statusRecordSet;
		Vector msgRecord = new Vector();
		msgRecord.add(new Integer(statusPtr));
		msgRecord.add(statusName);
		msgRecord.add(new Integer(intInfo));
		msgRecord.add(statusInfo);
		if (isReplace && messageQueue.containsKey(statusName)) {
			messageQueue.remove(statusName);
		}
		if (messageQueue.containsKey(statusName)) {
			statusRecordSet = (Vector) messageQueue.remove(statusName);
		}
		else {
			statusRecordSet = new Vector();
		}
		if (statusRecordSet.size() == MAXIMUM_QUEUE_LENGTH)
			statusRecordSet.remove(0);

		statusRecordSet.add(msgRecord);
		messageQueue.put(statusName, statusRecordSet);
	}

	@SuppressWarnings("unchecked")
	synchronized Vector getStatusChanged(String statusNameList) {
		/*
		 * returns a Vector of statusRecordSets, one per status type, where each statusRecordSet is itself a vector of
		 * vectors: [int statusPtr,String statusName,int intInfo, String statusInfo]
		 * 
		 * This allows selection of just the type desired as well as sorting by time overall.
		 */
		Vector msgList = new Vector();
		if (setStatusList(statusNameList))
			return msgList;
		Enumeration e = messageQueue.keys();
		int n = 0;
		while (e.hasMoreElements()) {
			String statusName = (String) e.nextElement();
			msgList.add(messageQueue.remove(statusName));
			n++;
		}
		return msgList;
	}

	float functionXY(String functionName, int x, int y) {
		return (jmolStatusListener == null ? 0 : jmolStatusListener.functionXY(functionName, x, y));
	}

	String eval(String strEval) {
		return (jmolStatusListener == null ? "" : jmolStatusListener.eval(strEval));
	}

	public void createImage(String file, String type, int quality) {
		if (jmolStatusListener == null)
			return;
		jmolStatusListener.createImage(file, type, quality);
	}

}
