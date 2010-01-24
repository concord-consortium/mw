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

import org.concord.modeler.text.Page;

/**
 * @author Charles Xie
 * 
 */
public class MwApplet extends JApplet {

	private Page page;

	public MwApplet() {

		Page.setApplet(true);
		Page.setSoftwareName(Modeler.NAME);
		ConnectionManager.sharedInstance().setCachingAllowed(false);

		page = new Page() {
			public URL getCodeBase() {
				return MwApplet.this.getCodeBase();
			}
		};
		page.setPreferredSize(new Dimension(600, 600));
		getContentPane().add(page, BorderLayout.CENTER);

		ComponentPool pool = new ComponentPool(page);
		page.setComponentPool(pool);

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
			page.executeMwScripts(s);
		}

	}

	@Override
	public void destroy() {
		// page.destroy();
	}

}