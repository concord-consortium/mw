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

import java.net.URL;
import java.util.List;

/**
 * @author Charles Xie
 * 
 */
public interface HtmlService {

	/** set the base URL for the HTML document */
	public void setBase(URL u);

	/** get the base URL for the HTML document */
	public URL getBase();

	/** get the background image name from the HTML document */
	public String getBackgroundImage();

	/** return a list of the names of all the embedded images in the HTML document */
	public List<String> getImageNames();

	/**
	 * cache the embedded images and the background image.
	 * 
	 * @param codeBase
	 *            the current parent directory
	 */
	public void cacheImages(String codeBase);

	/**
	 * If b is true, it is used to modify the HTML document so that the images point to the cached ones. Otherwise, it
	 * is used to remove the attribute changes related to caching before saving the page.
	 */
	public void useCachedImages(boolean b, String codeBase);

}
