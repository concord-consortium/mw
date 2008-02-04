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

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class FileFilterFactory {

	private final static FileFilter[] FILE_FILTERS = new FileFilter[] { createFilter("jnlp"), createFilter("html"),
			createFilter("txt"), createFilter("mws"), createFilter("pdb"), createFilter("xyz"), createFilter("mdd"),
			createFilter("cml"), createFilter("mml"), createFilter("gbl"), createFilter("swf"), createFilter("gif"),
			createFilter("jpg"), createFilter("png"), createFilter("out"), createFilter("zip"), createFilter("gz"),
			createFilter("mol"), createFilter("cif"), createFilter("mol2"), createFilter("all images") };

	private FileFilterFactory() {
	}

	public static FileFilter getFilter(String suffix) {
		if (suffix == null)
			return null;
		if (suffix.equalsIgnoreCase("jpeg")) {
			suffix = "jpg";
		}
		else if (suffix.equalsIgnoreCase("htm")) {
			suffix = "html";
		}
		for (FileFilter i : FILE_FILTERS) {
			if (i.getDescription().equalsIgnoreCase(suffix))
				return i;
		}
		return null;
	}

	private static final FileFilter createFilter(final String ext) {

		return new FileFilter() {

			public boolean accept(File file) {
				if (file == null)
					return false;
				if (file.isDirectory())
					return true;
				String filename = file.getName();
				int index = filename.lastIndexOf('.');
				if (index == -1)
					return false;
				String postfix = filename.substring(index + 1);
				if (ext.equalsIgnoreCase(postfix))
					return true;
				if ("jpeg".equalsIgnoreCase(postfix) && "jpg".equalsIgnoreCase(ext))
					return true;
				if ("jpg".equalsIgnoreCase(postfix) && "jpeg".equalsIgnoreCase(ext))
					return true;
				if ("html".equalsIgnoreCase(postfix) && "htm".equalsIgnoreCase(ext))
					return true;
				if ("htm".equalsIgnoreCase(postfix) && "html".equalsIgnoreCase(ext))
					return true;
				if ("all images".equalsIgnoreCase(ext)) {
					if ("gif".equalsIgnoreCase(postfix) || "jpg".equalsIgnoreCase(postfix)
							|| "png".equalsIgnoreCase(postfix))
						return true;
				}
				return false;
			}

			public String getDescription() {
				return ext.toUpperCase();
			}

			public String toString() {
				return ext;
			}

		};

	}

}