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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

final class SnapshotManager {

	private String selectedImageName;
	private List<String> imageNames;
	private Map<String, String> pageMap;
	private File annotatedImageFolder, originalImageFolder;
	private Map<Object, Object> propertiesMap;
	private List<SnapshotListener> listeners;

	SnapshotManager() {
		imageNames = Collections.synchronizedList(new ArrayList<String>());
		pageMap = Collections.synchronizedMap(new HashMap<String, String>());
		propertiesMap = Collections.synchronizedMap(new HashMap<Object, Object>());
		originalImageFolder = new File(Initializer.sharedInstance().getGalleryDirectory(), "original");
		if (!originalImageFolder.exists())
			originalImageFolder.mkdir();
		annotatedImageFolder = new File(Initializer.sharedInstance().getGalleryDirectory(), "annotated");
		if (!annotatedImageFolder.exists())
			annotatedImageFolder.mkdir();
	}

	File getAnnotatedImageFolder() {
		return annotatedImageFolder;
	}

	File getAnnotatedImageFile(String name) {
		return new File(annotatedImageFolder, name);
	}

	File getOriginalImageFolder() {
		return originalImageFolder;
	}

	private File getOriginalImageFile(String name) {
		return new File(originalImageFolder, name);
	}

	void clear() {
		selectedImageName = null;
		imageNames.clear();
		pageMap.clear();
		propertiesMap.clear();
	}

	boolean isEmpty() {
		return imageNames.isEmpty();
	}

	int size() {
		return imageNames.size();
	}

	boolean containsImageName(String name) {
		return imageNames.contains(name);
	}

	void addImageName(String name, String pageAddress) {
		if (containsImageName(name))
			return;
		imageNames.add(name);
		if (pageAddress != null)
			pageMap.put(name, pageAddress);
	}

	void setImageName(int i, String name, String pageAddress) {
		if (i < 0 || i >= imageNames.size())
			return;
		if (pageAddress != null)
			pageMap.remove(imageNames.get(i));
		imageNames.set(i, name);
		if (pageAddress != null)
			pageMap.put(name, pageAddress);
	}

	String getImageName(int i) {
		if (i < 0 || i >= imageNames.size())
			return null;
		return imageNames.get(i);
	}

	String getOwnerPage(String name) {
		return pageMap.get(name);
	}

	String[] getImageNames() {
		Object[] o = imageNames.toArray();
		String[] s = new String[o.length];
		System.arraycopy(o, 0, s, 0, o.length);
		return s;
	}

	int getSelectedIndex() {
		if (selectedImageName == null)
			return -1;
		return imageNames.indexOf(selectedImageName);
	}

	void setSelectedIndex(int i) {
		if (i < 0 || i >= imageNames.size()) {
			selectedImageName = null;
		}
		else {
			selectedImageName = imageNames.get(i);
		}
	}

	void setSelectedImageName(String name) {
		selectedImageName = name;
	}

	String getSelectedImageName() {
		return selectedImageName;
	}

	/* return the removed annoated image. */
	String removeSelectedImageName() {
		if (selectedImageName == null)
			return null;
		String s = selectedImageName;
		selectedImageName = null;
		imageNames.remove(s);
		return s;
	}

	boolean stepBack() {
		if (selectedImageName != null) {
			int i = imageNames.indexOf(selectedImageName);
			if (i > 0) {
				selectedImageName = imageNames.get(i - 1);
				return true;
			}
		}
		return false;
	}

	boolean stepForward() {
		if (selectedImageName != null) {
			int i = imageNames.indexOf(selectedImageName);
			if (i >= 0 && i < imageNames.size() - 1) {
				selectedImageName = imageNames.get(i + 1);
				return true;
			}
		}
		return false;
	}

	ImageIcon loadSelectedOriginalImage() {
		return loadOriginalImage(selectedImageName);
	}

	ImageIcon loadOriginalImage(int i) {
		return loadOriginalImage(getImageName(i));
	}

	ImageIcon loadOriginalImage(String name) {
		if (name == null)
			return null;
		ImageIcon icon = ModelerUtilities.createNonCachedImageIcon(getOriginalImageFile(name));
		icon.setDescription(name);
		return icon;
	}

	ImageIcon loadSelectedAnnotatedImage() {
		return loadAnnotatedImage(selectedImageName);
	}

	ImageIcon loadAnnotatedImage(int i) {
		return loadAnnotatedImage(getImageName(i));
	}

	ImageIcon loadAnnotatedImage(String name) {
		if (name == null)
			return null;
		ImageIcon icon = ModelerUtilities.createNonCachedImageIcon(getAnnotatedImageFile(name));
		icon.setDescription(name);
		return icon;
	}

	void putProperty(Object key, Object val) {
		propertiesMap.put(key, val);
	}

	Object getProperty(Object key) {
		return propertiesMap.get(key);
	}

	void removeProperty(Object key) {
		propertiesMap.remove(key);
	}

	boolean hasNoProperty() {
		return propertiesMap.isEmpty();
	}

	void addSnapshotListener(SnapshotListener listener) {
		if (listener == null)
			return;
		if (listeners == null)
			listeners = new ArrayList<SnapshotListener>();
		listeners.add(listener);
	}

	void removeSnapshotListener(SnapshotListener listener) {
		if (listeners == null)
			return;
		listeners.remove(listener);
	}

	void notifyListeners(SnapshotEvent e) {
		if (listeners == null || listeners.isEmpty())
			return;
		for (SnapshotListener l : listeners) {
			switch (e.getType()) {
			case SnapshotEvent.SNAPSHOT_ADDED:
				l.snapshotAdded(e);
				break;
			case SnapshotEvent.SNAPSHOT_CHANGED:
				l.snapshotChanged(e);
				break;
			case SnapshotEvent.SNAPSHOT_REMOVED:
				l.snapshotRemoved(e);
				break;
			}
		}
	}

}