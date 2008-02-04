/*
 *   Copyright (C) 2007  The Concord Consortium, Inc.,
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
package org.concord.modeler;

import java.awt.Component;
import java.awt.EventQueue;

import javax.swing.JOptionPane;

/**
 * @author Charles Xie
 * 
 */
public class DisasterHandler implements Thread.UncaughtExceptionHandler {

	public final static byte LOAD_ERROR = 0;
	public final static byte SCRIPT_ERROR = 1;

	private Runnable cleanUpCallback;
	private Runnable messageCallback;
	private Component parent;
	private byte errorType = LOAD_ERROR;

	public DisasterHandler(byte errorType, Runnable cleanUpCallback, Runnable messageCallback, Component parent) {
		this.errorType = errorType;
		this.cleanUpCallback = cleanUpCallback;
		this.messageCallback = messageCallback;
		this.parent = parent;
	}

	public void uncaughtException(final Thread t, final Throwable e) {

		e.printStackTrace();

		if (cleanUpCallback != null)
			cleanUpCallback.run();

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (messageCallback == null) {
					showErrorMessage(t, e);
				}
				else {
					messageCallback.run();
				}
			}
		});

	}

	// the default messsage to tell the user something is wrong on loading the model(s)
	private void showErrorMessage(Thread t, Throwable e) {
		String s1 = null, s2 = null, s3 = "";
		switch (errorType) {
		case LOAD_ERROR:
			String s = Modeler.getInternationalText("LoadingError");
			s1 = s != null ? s : "Loading Error";
			s = Modeler.getInternationalText("EncounteredErrorInLoadingDataCausedBy");
			s2 = s != null ? s : "Sorry, we have encountered errors in loading data, caused by:";
			if (e instanceof OutOfMemoryError) {
				s3 = "\n\nSuggestion:\nIt may be good to restart " + Modeler.NAME + " if this error keeps occuring.";
			}
			break;
		case SCRIPT_ERROR:
			s = Modeler.getInternationalText("ScriptError");
			s1 = s != null ? s : "Script Error";
			s = Modeler.getInternationalText("EncounteredErrorInScriptsCausedBy");
			s2 = s != null ? s : "Sorry, we have encountered errors in scripts, caused by:";
			break;
		}
		JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(parent), s2 + "\n" + e + "\n in thread: "
				+ t.getName() + s3, s1, JOptionPane.ERROR_MESSAGE);
	}

}