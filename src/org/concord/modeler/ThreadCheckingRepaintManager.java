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
package org.concord.modeler;

import java.awt.EventQueue;

import javax.swing.JComponent;
import javax.swing.RepaintManager;

/**
 * @see http://weblogs.java.net/blog/alexfromsun/archive/2006/02/index.html
 * 
 */

class ThreadCheckingRepaintManager extends RepaintManager {

	// it is recommended to pass the complete check
	private boolean completeCheck = false;

	public boolean isCompleteCheck() {
		return completeCheck;
	}

	public void setCompleteCheck(boolean completeCheck) {
		this.completeCheck = completeCheck;
	}

	public synchronized void addInvalidComponent(JComponent component) {
		checkThreadViolations(component);
		super.addInvalidComponent(component);
	}

	public void addDirtyRegion(JComponent component, int x, int y, int w, int h) {
		checkThreadViolations(component);
		super.addDirtyRegion(component, x, y, w, h);
	}

	private void checkThreadViolations(JComponent c) {
		if (!EventQueue.isDispatchThread() && (completeCheck || c.isShowing())) {
			Exception exception = new Exception();
			boolean repaint = false;
			boolean fromSwing = false;
			boolean imageFetcher = false;
			StackTraceElement[] stackTrace = exception.getStackTrace();
			for (StackTraceElement st : stackTrace) {
				if (repaint && st.getClassName().startsWith("javax.swing.")) {
					fromSwing = true;
				}
				if ("repaint".equals(st.getMethodName())) {
					repaint = true;
				}
				if (st.getClassName().startsWith("sun.awt.image.ImageFetcher")) {
					imageFetcher = true;
				}
			}
			if (repaint && !fromSwing) {
				// no problems here, since repaint() is thread safe
				return;
			}
			if (imageFetcher) // skip ImageFetcher
				return;
			exception.printStackTrace();
		}
	}

}