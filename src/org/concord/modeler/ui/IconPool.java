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

package org.concord.modeler.ui;

import java.util.HashMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;

public final class IconPool {

	private static HashMap<String, Icon> map;
	private static Class c;

	private IconPool() {
	}

	private static void init() {

		map = new HashMap<String, Icon>();
		c = IconPool.class;

		Icon icon = new ImageIcon(c.getResource("images/new.gif"));
		map.put("new", icon);

		icon = new ImageIcon(c.getResource("images/update.gif"));
		map.put("update", icon);

		icon = new ImageIcon(c.getResource("images/cut.gif"));
		map.put("cut", icon);

		icon = new ImageIcon(c.getResource("images/copy.gif"));
		map.put("copy", icon);

		icon = new ImageIcon(c.getResource("images/paste.gif"));
		map.put("paste", icon);

		icon = new ImageIcon(c.getResource("images/selectall.gif"));
		map.put("selectall", icon);

		icon = new ImageIcon(c.getResource("images/erase.gif"));
		map.put("erase", icon);

		icon = new ImageIcon(c.getResource("images/undo.gif"));
		map.put("undo", icon);

		icon = new ImageIcon(c.getResource("images/redo.gif"));
		map.put("redo", icon);

		icon = new ImageIcon(c.getResource("images/open.gif"));
		map.put("open", icon);

		icon = new ImageIcon(c.getResource("images/openweb.gif"));
		map.put("openweb", icon);

		icon = new ImageIcon(c.getResource("images/save.gif"));
		map.put("save", icon);

		icon = new ImageIcon(c.getResource("images/properties.gif"));
		map.put("properties", icon);

		icon = new ImageIcon(c.getResource("images/select.gif"));
		map.put("select", icon);

		icon = new ImageIcon(c.getResource("images/exit.gif"));
		map.put("exit", icon);

		icon = new ImageIcon(c.getResource("images/play.gif"));
		map.put("play", icon);

		icon = new ImageIcon(c.getResource("images/pause.gif"));
		map.put("pause", icon);

		icon = new ImageIcon(c.getResource("images/reset.gif"));
		map.put("reset", icon);

		icon = new ImageIcon(c.getResource("images/zoom_in.gif"));
		map.put("zoom_in", icon);

		icon = new ImageIcon(c.getResource("images/zoom_out.gif"));
		map.put("zoom_out", icon);

		icon = new ImageIcon(c.getResource("images/printer.gif"));
		map.put("printer", icon);

		icon = new ImageIcon(c.getResource("images/heat.gif"));
		map.put("heat", icon);

		icon = new ImageIcon(c.getResource("images/cool.gif"));
		map.put("cool", icon);

		icon = new ImageIcon(c.getResource("images/spin.gif"));
		map.put("spin", icon);

		icon = new ImageIcon(c.getResource("images/camera.gif"));
		map.put("camera", icon);

		icon = new ImageIcon(c.getResource("images/house.gif"));
		map.put("house", icon);

		icon = new ImageIcon(c.getResource("images/button.gif"));
		map.put("button", icon);

		icon = new ImageIcon(c.getResource("images/radiobutton.gif"));
		map.put("radiobutton", icon);

		icon = new ImageIcon(c.getResource("images/checkbox.gif"));
		map.put("checkbox", icon);

		icon = new ImageIcon(c.getResource("images/slider.gif"));
		map.put("slider", icon);

		icon = new ImageIcon(c.getResource("images/spinner.gif"));
		map.put("spinner", icon);

		icon = new ImageIcon(c.getResource("images/combobox.gif"));
		map.put("combobox", icon);

		icon = new ImageIcon(c.getResource("images/console.gif"));
		map.put("console", icon);

		icon = new ImageIcon(c.getResource("images/numeric.gif"));
		map.put("numeric", icon);

		icon = new ImageIcon(c.getResource("images/bargraph.gif"));
		map.put("bargraph", icon);

		icon = new ImageIcon(c.getResource("images/linegraph.gif"));
		map.put("linegraph", icon);

		icon = new ImageIcon(c.getResource("images/gauge.gif"));
		map.put("gauge", icon);

		icon = new ImageIcon(c.getResource("images/piechart.gif"));
		map.put("piechart", icon);

		icon = new ImageIcon(c.getResource("images/linetool.gif"));
		map.put("linetool", icon);

		icon = new ImageIcon(c.getResource("images/arrowtool.gif"));
		map.put("arrowtool", icon);

		icon = new ImageIcon(c.getResource("images/ellipsetool.gif"));
		map.put("ellipsetool", icon);

		icon = new ImageIcon(c.getResource("images/recttool.gif"));
		map.put("recttool", icon);

		icon = new ImageIcon(c.getResource("images/triangletool.gif"));
		map.put("triangletool", icon);

		icon = new ImageIcon(c.getResource("images/view.gif"));
		map.put("view", icon);

		icon = new ImageIcon(c.getResource("images/taskmanager.gif"));
		map.put("taskmanager", icon);

		icon = new ImageIcon(c.getResource("images/restrain.gif"));
		map.put("restrain", icon);

		icon = new ImageIcon(c.getResource("images/release.gif"));
		map.put("release", icon);

		icon = new ImageIcon(c.getResource("images/charge.gif"));
		map.put("charge", icon);

		icon = new ImageIcon(c.getResource("images/traj.gif"));
		map.put("traj", icon);

		icon = new ImageIcon(c.getResource("images/velocity.gif"));
		map.put("velocity", icon);

		icon = new ImageIcon(c.getResource("images/removerect.gif"));
		map.put("remove rect", icon);

		icon = new ImageIcon(c.getResource("images/resize.gif"));
		map.put("resize", icon);

		icon = new ImageIcon(c.getResource("images/timestep.gif"));
		map.put("time step", icon);

		icon = new ImageIcon(c.getResource("images/movie.gif"));
		map.put("movie", icon);

		icon = new ImageIcon(c.getResource("images/annotation.gif"));
		map.put("annotation", icon);

		icon = new ImageIcon(c.getResource("images/thermometer.gif"));
		map.put("thermometer", icon);

		icon = new ImageIcon(c.getResource("images/heatbath.gif"));
		map.put("heat bath", icon);

		icon = new ImageIcon(c.getResource("images/ecurve.gif"));
		map.put("e curve", icon);

		icon = new ImageIcon(c.getResource("images/steepestdescent.gif"));
		map.put("steepest descent", icon);

		icon = new ImageIcon(c.getResource("images/radialbond.gif"));
		map.put("radial bond", icon);

		icon = new ImageIcon(c.getResource("images/radialbondcursor.gif"));
		map.put("radial bond cursor", icon);

		icon = new ImageIcon(c.getResource("images/angularbond.gif"));
		map.put("angular bond", icon);

		icon = new ImageIcon(c.getResource("images/angularbondcursor.gif"));
		map.put("angular bond cursor", icon);

		icon = new ImageIcon(c.getResource("images/userdraggable.gif"));
		map.put("user draggable", icon);

	}

	public static Icon getIcon(String name) {
		if (map == null)
			init();
		return map.get(name);
	}

}