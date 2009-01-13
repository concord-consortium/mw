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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.concord.modeler.FileFilterFactory;
import org.concord.modeler.event.ImageEvent;
import org.concord.modeler.event.ImageImporter;
import org.concord.modeler.ui.ImagePreview;

/**
 * A <tt>ImageReader</tt> fires a <code>PropertyChangeEvent</code> to the listeners. The property name is
 * <code>INPUT_IMAGE</code>. The file selected by the associated file chooser is passed as the new value of the
 * <code>PropertyChangeEvent</code>. This class does not know what to do with the selected file. You will have to
 * create an image object from the specified file by yourself.
 * 
 * @author Charles Xie
 */

public class ImageReader extends AbstractAction {

	private static final Icon INSERT_IMAGE_ICON = new ImageIcon(ImageReader.class
			.getResource("images/InsertPicture.gif"));
	private static final String[] IMAGE_FILTERS = new String[] { "jpg", "png", "gif", "all images" };
	private Component parent;
	private FileChooser fileChooser;
	private List<ImageImporter> importerStore;
	private BufferedImage image;
	private String description;
	private String selectedExtension;
	private static ImagePreview imagePreview;

	public ImageReader(String name, FileChooser fc, Component parent) {
		super(name);
		this.parent = parent;
		setFileChooser(fc);
		putValue(MNEMONIC_KEY, KeyEvent.VK_I);
		putValue(NAME, "Input Image");
		putValue(SHORT_DESCRIPTION, "Input image");
		putValue(SMALL_ICON, INSERT_IMAGE_ICON);
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
	}

	public void setParent(Component c) {
		parent = c;
	}

	public static void setImagePreview(ImagePreview ip) {
		imagePreview = ip;
	}

	public static ImagePreview getImagePreview() {
		return imagePreview;
	}

	public void setFileChooser(FileChooser fc) {
		if (fc == null) {
			fileChooser = new FileChooser();
		}
		else {
			fileChooser = fc;
		}
	}

	public FileChooser getFileChooser() {
		return fileChooser;
	}

	public void addImageImporter(ImageImporter importer) {
		if (importerStore == null)
			importerStore = new ArrayList<ImageImporter>();
		importerStore.add(importer);
	}

	public void removeImageImporter(ImageImporter importer) {
		if (importerStore == null)
			return;
		importerStore.remove(importer);
	}

	public List<ImageImporter> getImageImporters() {
		return importerStore;
	}

	protected void notifyImageImporters() {
		if (importerStore == null)
			return;
		ImageEvent e = new ImageEvent(this, image, description);
		for (ImageImporter i : importerStore)
			i.imageImported(e);
	}

	public void actionPerformed(ActionEvent e) {
		read(IMAGE_FILTERS);
	}

	protected boolean read(String[] filters) {

		File lastFile = fileChooser.getSelectedFile();

		if (filters == null || filters.length == 0) {
			fileChooser.setAcceptAllFileFilterUsed(true);
		}
		else {
			fileChooser.setAcceptAllFileFilterUsed(false);
			fileChooser.addChoosableFileFilters(filters);
		}
		fileChooser.setFileFilter(FileFilterFactory.getFilter(selectedExtension == null ? "all images"
				: selectedExtension));

		fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
		Object o = getValue("i18n");
		fileChooser.setDialogTitle(o != null ? (String) o : "Select an image");
		fileChooser.setApproveButtonMnemonic('O');
		String latestPath = fileChooser.getLastVisitedPath();
		if (latestPath != null)
			fileChooser.setCurrentDirectory(new File(latestPath));
		if (lastFile != null && !lastFile.isDirectory())
			fileChooser.handleFileTypeSwitching(lastFile);
		if (imagePreview != null) {
			fileChooser.setAccessory(imagePreview);
			imagePreview.setFile(lastFile);
		}

		if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
			selectedExtension = fileChooser.getFileFilter().getDescription().toLowerCase();
			File file = fileChooser.getSelectedFile();
			if (!fileChooser.getFileFilter().accept(file))
				file = new File(file.getAbsolutePath() + "." + fileChooser.getFileFilter());
			if (file.exists()) {
				try {
					image = ImageIO.read(file);
					description = file.getAbsolutePath();
					notifyImageImporters();
				}
				catch (IOException ioe) {
					ioe.printStackTrace(System.err);
				}
				fileChooser.rememberPath(fileChooser.getCurrentDirectory().toString());
			}
			else {
				JOptionPane.showMessageDialog(parent, file + " does not exist.", "File not found",
						JOptionPane.ERROR_MESSAGE);
			}
		}

		fileChooser.resetChoosableFileFilters();
		return true;

	}

}