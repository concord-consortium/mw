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
package org.concord.mw3d;

import java.io.File;

import org.concord.modeler.draw.FillMode;
import org.concord.modeler.util.FileUtilities;
import org.concord.modeler.util.ImageSaver;

/**
 * @author Charles Xie
 * 
 */
class MyImageSaver extends ImageSaver {

	private MyImageSaver() {
	}

	/*
	 * if background image is set using the filechooser the URL field of the ImageFill will be set to be the real HD
	 * address of the image. If background image transfers from another model because of downloading or saving, the URL
	 * field of the ImageFill will be set to the file name of that image ONLY.
	 */
	static void saveImages(MolecularView view, File parentFile) {
		if (view.getFillMode() instanceof FillMode.ImageFill) {
			String url = ((FillMode.ImageFill) view.getFillMode()).getURL();
			String base = FileUtilities.getCodeBase(url);
			if (base == null)
				url = FileUtilities.getCodeBase(view.getResourceAddress()) + url;
			File newFile = new File(parentFile, FileUtilities.getFileName(url));
			if (FileUtilities.isRemote(url)) {
				saveImage(view, url, parentFile);
			}
			else {
				copyFile(view, url, newFile);
			}
			((FillMode.ImageFill) view.getFillMode()).setURL(newFile.toString());
		}
	}

}