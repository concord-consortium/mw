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
package org.concord.jmol;

import java.io.File;
import java.util.List;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.draw.FillMode;
import org.concord.modeler.ui.MyEditorPane;
import org.concord.modeler.util.FileUtilities;
import org.concord.modeler.util.ImageSaver;
import org.jmol.api.SiteAnnotation;

/**
 * @author Charles Xie
 * 
 */
class MyImageSaver extends ImageSaver {

	private MyImageSaver() {
	}

	private static void saveHTMLImages(String address, String html, File parent) {
		if (html == null || address == null || parent == null)
			return;
		String s = html.trim();
		if (s.length() <= 0)
			return;
		if (!s.substring(0, 5).equalsIgnoreCase("<html"))
			return;
		MyEditorPane mep = new MyEditorPane("text/html", html);
		List<String> list = mep.getImageNames();
		if (list == null || list.isEmpty())
			return;
		for (String name : list) {
			if (FileUtilities.isRemote(name))
				continue;
			if (FileUtilities.isRelative(name)) {
				name = FileUtilities.getCodeBase(address) + name;
				ModelerUtilities.copyResourceToDirectory(name, parent);
			}
		}
	}

	/*
	 * if background image is set using the filechooser the URL field of the ImageFill will be set to be the real HD
	 * address of the image. If background image transfers from another model because of downloading or saving, the URL
	 * field of the ImageFill will be set to the file name of that image ONLY.
	 */
	static void saveImages(JmolContainer container, File parentFile) {
		if (container.getFillMode() instanceof FillMode.ImageFill) {
			String url = ((FillMode.ImageFill) container.getFillMode()).getURL();
			String base = FileUtilities.getCodeBase(url);
			if (base == null)
				url = FileUtilities.getCodeBase(container.getPageAddress()) + url;
			File newFile = new File(parentFile, FileUtilities.getFileName(url));
			if (FileUtilities.isRemote(url)) {
				saveImage(container, url, parentFile);
			}
			else {
				copyFile(container, url, newFile);
			}
			((FillMode.ImageFill) container.getFillMode()).setURL(newFile.toString());
		}
		if (!container.atomAnnotations.isEmpty()) {
			for (SiteAnnotation a : container.atomAnnotations.values()) {
				saveHTMLImages(container.getPageAddress(), a.getText(), parentFile);
			}
		}
		if (!container.bondAnnotations.isEmpty()) {
			for (SiteAnnotation a : container.bondAnnotations.values()) {
				saveHTMLImages(container.getPageAddress(), a.getText(), parentFile);
			}
		}
	}

}