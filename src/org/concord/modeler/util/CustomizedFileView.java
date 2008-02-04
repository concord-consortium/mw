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

package org.concord.modeler.util;

import java.io.File;

import javax.swing.Icon;
import javax.swing.filechooser.FileView;

class CustomizedFileView extends FileView {

	public String getName(File file) {
		return null;
	}

	public String getDescription(File file) {
		return null;
	}

	public Boolean isTraversable(File file) {
		return null;
	}

	public Icon getIcon(File file) {
		return null;
	}

	public String getTypeDescription(File file) {

		String filename = file.getName();
		int index = filename.lastIndexOf('.');
		String extension = filename.substring(index + 1).toLowerCase();
		String type = null;
		if (extension != null) {
			if (extension.equals("mml")) {
				type = "MML";
			}
			else if (extension.equals("gbl")) {
				type = "GBL";
			}
			else if (extension.equals("jpg")) {
				type = "JPG";
			}
			else if (extension.equals("jpeg")) {
				type = "JPG";
			}
			else if (extension.equals("yox")) {
				type = "YOX";
			}
		}
		return type;

	}

}