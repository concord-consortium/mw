/* $RCSfile: ScriptManager.java,v $
 * $Author: qxie $
 * $Date: 2007-12-05 16:01:40 $
 * $Revision: 1.5 $
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

import java.awt.EventQueue;
import java.util.Vector;

import javax.swing.JOptionPane;

import org.myjmol.util.Logger;

class ScriptManager {

	private Viewer viewer;
	private Thread queueThread;
	private Vector scriptQueue = new Vector();
	boolean useQueue = true; // new default

	ScriptManager(Viewer viewer) {
		this.viewer = viewer;
	}

	public void setQueue(boolean TF) {
		useQueue = TF;
		if (!TF)
			clearQueue();
	}

	public String addScript(String strScript) {
		return (String) addScript("string", strScript, "", false, false);
	}

	public String addScript(String strScript, boolean isScriptFile, boolean isQuiet) {
		return (String) addScript("String", strScript, "", isScriptFile, isQuiet);
	}

	@SuppressWarnings("unchecked")
	public Object addScript(String returnType, String strScript, String statusList, boolean isScriptFile,
			boolean isQuiet) {
		Object tokenInfo = null;
		/*
		 * just an idea -- precompile
		 * 
		 * if (!isScriptFile) { tokenInfo = viewer.compileInfo(strScript); if (tokenInfo instanceof String) return
		 * tokenInfo; }
		 */
		Vector scriptItem = new Vector();
		scriptItem.add(strScript);
		scriptItem.add(statusList);
		scriptItem.add(returnType);
		scriptItem.add(isScriptFile ? Boolean.TRUE : Boolean.FALSE);
		scriptItem.add(isQuiet ? Boolean.TRUE : Boolean.FALSE);
		scriptItem.add(tokenInfo);

		if (!useQueue) {
			clearQueue();
			viewer.haltScriptExecution();
		}
		scriptQueue.add(scriptItem);
		Logger.debug(scriptQueue.size() + " scripts; added: " + strScript);
		startScriptQueue();
		return "pending";
	}

	public int getScriptCount() {
		return scriptQueue.size();
	}

	public void clearQueue() {
		scriptQueue.clear();
	}

	public void waitForQueue() {
		int n = 0;
		while (queueThread != null) {
			try {
				Thread.sleep(100);
				if (((n++) % 10) == 0)
					Logger.debug("...scriptManager waiting for queue: " + scriptQueue.size());
			}
			catch (InterruptedException e) {
			}
		}
	}

	Object runNextScript() {
		if (scriptQueue.isEmpty())
			return null;
		Vector scriptItem = (Vector) scriptQueue.get(0);
		String script = (String) scriptItem.get(0);
		String statusList = (String) scriptItem.get(1);
		String returnType = (String) scriptItem.get(2);
		boolean isScriptFile = ((Boolean) scriptItem.get(3)).booleanValue();
		boolean isQuiet = ((Boolean) scriptItem.get(4)).booleanValue();
		Vector tokenInfo = (Vector) scriptItem.get(5);
		Logger.debug(scriptQueue.size() + " scripts; running: " + script);
		// XIE if (scriptQueue.isEmpty()) return null;
		scriptQueue.remove(0);
		Object returnInfo = runScript(returnType, script, statusList, isScriptFile, isQuiet, tokenInfo);
		if (scriptQueue.isEmpty()) // might have been cleared with an exit
			return null;
		return returnInfo;
	}

	private Object runScript(String returnType, String strScript, String statusList, boolean isScriptFile,
			boolean isQuiet, Vector tokenInfo) {
		return viewer.evalStringWaitStatus(returnType, strScript, statusList, isScriptFile, isQuiet, tokenInfo);
	}

	private void startScriptQueue() {
		if (scriptQueueRunning)
			return;
		scriptQueueRunning = true;
		if (queueThread != null)
			queueThread.interrupt();
		queueThread = new Thread(new ScriptQueueRunnable());
		queueThread.setName("Jmol Script " + queueThread.getName());
		// System.out.println(queueThread.getName());
		// XIE: handle uncaught exceptions
		queueThread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, final Throwable e) {
				e.printStackTrace();
				scriptQueueRunning = false;
				queueThread = null;
				if (viewer != null)
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(viewer.getAwtComponent()),
									"Fatal script error, thrown by: " + e, "Script Error", JOptionPane.ERROR_MESSAGE);
						}
					});
			}
		});
		queueThread.start();
	}

	boolean scriptQueueRunning;
	int level;

	class ScriptQueueRunnable implements Runnable {
		public void run() {
			while (!scriptQueue.isEmpty()) {
				runNextScript();
			}
			scriptQueueRunning = false;
		}

		public void stop() {
			scriptQueueRunning = false;
		}
	}

}
