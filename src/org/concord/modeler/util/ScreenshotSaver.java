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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.border.Border;

import org.concord.modeler.Modeler;
import org.concord.modeler.FileFilterFactory;
import org.concord.modeler.ui.ImagePreview;

/** A <tt>ScreenshotSaver</tt> can save the graphics context of a component to an image file. */

public class ScreenshotSaver extends AbstractAction {

	private final static Icon SCREENSHOT_ICON = new ImageIcon(ScreenshotSaver.class
			.getResource("images/screenshot.gif"));

	private Component component;
	private boolean borderless;
	private Border savedBorder;
	private static ImagePreview imagePreview;
	private FileChooser fileChooser;

	/*
	 * @param c the component to be output @param noframe true if no frame is needed for the output image. If false, a
	 * black line frame will be added to the output image.
	 */
	public ScreenshotSaver(FileChooser fc, Component c, boolean noframe) {
		super();
		component = c;
		borderless = noframe;
		setFileChooser(fc);
		init();
	}

	public ScreenshotSaver(FileChooser fc, String name, Component c, boolean noframe) {
		super(name);
		component = c;
		borderless = noframe;
		setFileChooser(fc);
		init();
	}

	public ScreenshotSaver(FileChooser fc, String name, Icon icon, Component c, boolean noframe) {
		super(name, icon);
		component = c;
		borderless = noframe;
		setFileChooser(fc);
		init();
	}

	private void init() {
		if (imagePreview == null)
			imagePreview = new ImagePreview(fileChooser);
		putValue(NAME, "Make Screenshot");
		putValue(SHORT_DESCRIPTION, "Save a screenshot to disk");
		putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_M));
		putValue(SMALL_ICON, SCREENSHOT_ICON);
		putValue(ACCELERATOR_KEY, System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(
				KeyEvent.VK_M, KeyEvent.META_MASK, true) : KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_MASK,
				true));
	}

	public void destroy() {
		if (imagePreview != null && fileChooser != null)
			fileChooser.removePropertyChangeListener(imagePreview);
		component = null;
		fileChooser = null;
	}

	public void setFileChooser(FileChooser fc) {
		if (fc == null)
			throw new RuntimeException("file chooser can't be null");
		fileChooser = fc;
	}

	public FileChooser getFileChooser() {
		return fileChooser;
	}

	public void setComponent(Component c) {
		component = c;
	}

	public Component getComponent() {
		return component;
	}

	public void setFrame(boolean b) {
		borderless = !b;
	}

	public boolean getFrame() {
		return !borderless;
	}

	public void actionPerformed(ActionEvent e) {

		// if(!(e.getSource() instanceof AbstractButton) && !component.hasFocus()) return;

		File lastFile = fileChooser.getSelectedFile();
		List list = Arrays.asList(ImageIO.getWriterFormatNames());
		if (list == null || list.isEmpty()) {
			fileChooser.setAcceptAllFileFilterUsed(true);
		}
		else {
			fileChooser.setAcceptAllFileFilterUsed(false);
			for (int i = 0, n = list.size(); i < n; i++) {
				fileChooser.addChoosableFileFilter(FileFilterFactory.getFilter(list.get(i).toString()));
			}
		}
		fileChooser.setFileFilter(FileFilterFactory.getFilter("jpg"));
		fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
		Object o = getValue("i18n");
		fileChooser.setDialogTitle(o != null ? (String) o : "Save image");
		fileChooser.setApproveButtonMnemonic('S');
		String latestPath = fileChooser.getLastVisitedPath();
		if (latestPath != null)
			fileChooser.setCurrentDirectory(new File(latestPath));
		if (lastFile != null && !lastFile.isDirectory())
			fileChooser.handleFileTypeSwitching(lastFile);
		fileChooser.setAccessory(imagePreview);

		if (fileChooser.showSaveDialog(JOptionPane.getFrameForComponent(component)) == JFileChooser.APPROVE_OPTION) {
			final File file = fileChooser.getSelectedFile();
			String filename = FileUtilities.fileNameAutoExtend(fileChooser.getFileFilter(), file);
			final File temp = new File(filename);
			if (temp.exists()) {
				String s = Modeler.getInternationalText("FileExists");
				String s1 = Modeler.getInternationalText("File");
				String s2 = Modeler.getInternationalText("Overwrite");
				String s3 = Modeler.getInternationalText("Exists");
				if (JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(component),
						(s1 != null ? s : "File") + " " + temp.getName() + " " + (s3 != null ? s3 : "exists") + ", "
								+ (s2 != null ? s2 : "overwrite") + "?", s != null ? s : "File exists",
						JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
					fileChooser.resetChoosableFileFilters();
					return;
				}
			}
			if (!borderless) {
				if (component instanceof JComponent) {
					savedBorder = ((JComponent) component).getBorder();
					((JComponent) component).setBorder(BorderFactory.createLineBorder(Color.black));
				}
			}
			new SwingWorker() {
				public Object construct() {
					write(temp.getPath());
					return null;
				}

				public void finished() {
					fileChooser.rememberPath(file.getParent());
					if (savedBorder != null) {
						((JComponent) component).setBorder(savedBorder);
						savedBorder = null;
					}
				}
			}.start();
		}
		// fileChooser.resetChoosableFileFilters();
	}

	/**
	 * export the component to a JPEG file.
	 * 
	 * @param name
	 *            the name of the output JPEG file
	 */
	protected void write(String name) {
		Dimension size = component.getSize();
		BufferedImage bufferedImage = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = bufferedImage.createGraphics();
		component.paint(g2);
		OutputStream out = null;
		try {
			out = new FileOutputStream(name);
			ImageIO.write(bufferedImage, FileUtilities.getSuffix(name), out);
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
		}
		finally {
			if (out != null) {
				try {
					out.close();
				}
				catch (IOException iox) {
				}
			}
		}
	}

}