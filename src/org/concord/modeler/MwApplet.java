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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.net.URL;

import javax.swing.JApplet;
import javax.swing.UIManager;

import org.concord.jmol.JmolContainer;
import org.concord.modeler.text.Page;
import org.concord.mw2d.ui.MDContainer;
import org.concord.mw3d.MolecularContainer;

/**
 * @author Charles Xie
 * 
 */
public class MwApplet extends JApplet {

	Editor editor;

	public MwApplet() {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		Page.setApplet(true);
		Page.setSoftwareName(Modeler.NAME);
		ConnectionManager.sharedInstance().setCachingAllowed(false);
		MDContainer.setApplet(Page.isApplet());
		MolecularContainer.setApplet(Page.isApplet());
		JmolContainer.setApplet(Page.isApplet());

		editor = new Editor(null) {
			@Override
			public URL getCodeBase() {
				return MwApplet.this.getCodeBase();
			}
		};
		editor.setPreferredSize(new Dimension(600, 600));
		getContentPane().add(editor, BorderLayout.CENTER);

	}

	@Override
	public void init() {
		String s = null;
		try {
			s = getParameter("script");
		}
		catch (Exception e) {
			s = null;
		}
		if (s != null) {
			runMwScript(s);
		}
	}

	@Override
	public void destroy() {
		editor.destroy();
	}

	public String runMwScript(String script) {
		if (script == null)
			return null;
		// special treatment of the get command
		String[] token = script.split(":");
		if (token.length >= 3) {
			// reconnect "http://......" and others that should not have been broken up
			if (token.length >= 4) {
				for (int k = 3; k < token.length; k++)
					token[2] += ":" + token[k];
			}
			if (token[2] != null) {
				token[2] = token[2].trim();
				if (token[2].length() > 0) {
					String t2 = token[2].toLowerCase();
					if (t2.startsWith("get")) {
						String t0 = token[0].trim().intern();
						if (t0 == "jmol")
							return get(token, PageMolecularViewer.class);
						if (t0 == "mw2d")
							return get(token, ModelCanvas.class);
						if (t0 == "mw3d")
							return get(token, PageMd3d.class);
					}
				}
			}
		}
		return editor.getPage().executeMwScripts(script);
	}

	/* the get command should not use the scripting thread, otherwise, it is unpredictable what result it will get. */
	private String get(String[] token, Class klass) {
		String t2 = token[2].toLowerCase().substring(3).trim();
		if (t2 != null && t2.length() > 0) {
			int n = -1;
			try {
				n = Integer.valueOf(token[1].trim()).intValue();
			}
			catch (NumberFormatException e) {
			}
			Object o = null;
			if (n > 0) { // use index
				o = editor.getPage().getEmbeddedComponent(klass, n - 1);
			}
			else { // try UID
				o = editor.getPage().getEmbeddedComponent(token[1].trim());
			}
			if (o instanceof Scriptable) {
				Scriptable s = (Scriptable) o;
				s.runScriptImmediately(token[2]);
				Object o2 = s.get(t2);
				if (o2 != null)
					return o2.toString();
			}
		}
		return "Error: " + t2 + " is not found.";
	}

}