/*
 *   Copyright (C) 2010  The Concord Consortium, Inc.,
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

import org.concord.mw2d.ui.AtomContainer;
import org.concord.mw2d.ui.MDContainer;

/**
 * @author Charles Xie
 * 
 */
public class MwApplet2 extends MwApplet {

	public MwApplet2() {
		super();
	}

	public String runMwScript(String script) {
		if (script == null)
			return null;
		String[] token = script.split(":");
		if (token.length >= 3) {
			// reconnect "http://......" and others that should not have been broken up
			if (token.length >= 4) {
				for (int k = 3; k < token.length; k++)
					token[2] += ":" + token[k];
			}
			String t0 = token[0].trim().intern();
			if (t0 == "jmol") {
			}
			else if (t0 == "mw2d") {
				int n = -1;
				try {
					n = Integer.valueOf(token[1].trim()).intValue();
				}
				catch (NumberFormatException e) {
				}
				ModelCanvas mc = null;
				if (n > 0) { // use index
					mc = (ModelCanvas) editor.getPage().getEmbeddedComponent(ModelCanvas.class, n - 1);
				}
				else { // try UID
					mc = (ModelCanvas) editor.getPage().getEmbeddedComponent(token[1].trim());
				}
				if (mc != null) {
					if (token[2] != null) {
						MDContainer container = mc.getMdContainer();
						token[2] = token[2].trim();
						if (token[2].toLowerCase().startsWith("getdna")) {
							if (container instanceof AtomContainer) {
								AtomContainer ac = (AtomContainer) container;
								return ac.getTranscribedDNA();
							}
						}
					}
				}
			}
			else if (t0 == "mw3d") {
			}
		}
		return super.runMwScript(script);
	}

}