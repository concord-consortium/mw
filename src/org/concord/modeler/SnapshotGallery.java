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

import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.concord.modeler.ui.IconPool;
import org.concord.modeler.util.Blinker;
import org.concord.modeler.util.FileUtilities;

public class SnapshotGallery {

	private final static AudioClip CAMERA_SOUND = Applet.newAudioClip(SnapshotGallery.class
			.getResource("images/camera.wav"));
	private final static SnapshotGallery sharedInstance = new SnapshotGallery();

	private SnapshotManager snapshotManager;
	private ThumbnailManager thumbnailManager;
	private ThumbnailImagePanel thumbnailImagePanel;
	private SnapshotEditor editor;
	private Editor pageEditor;
	private JScrollPane scroller;
	private JPanel buttonPanel, bottomPanel;
	private JButton editButton, exportButton, insertButton, removeButton, clearButton, exportAllButton;
	private int option = -1;
	private JDialog dialog;
	private List<JComponent> flashComponents;

	private SnapshotGallery() {
		snapshotManager = new SnapshotManager();
		thumbnailManager = new ThumbnailManager();
	}

	public final static SnapshotGallery sharedInstance() {
		return sharedInstance;
	}

	public File getAnnotatedImageFolder() {
		return snapshotManager.getAnnotatedImageFolder();
	}

	void clear() {
		snapshotManager.clear();
		thumbnailManager.clear();
	}

	boolean isEmpty() {
		return snapshotManager.isEmpty();
	}

	int size() {
		return snapshotManager.size();
	}

	boolean containsImageName(String name) {
		return snapshotManager.containsImageName(name);
	}

	void addImageName(String name, String pageAddress) {
		snapshotManager.addImageName(name, pageAddress);
	}

	void setImageName(int i, String name, String pageAddress) {
		snapshotManager.setImageName(i, name, pageAddress);
	}

	String getImageName(int i) {
		return snapshotManager.getImageName(i);
	}

	public String getOwnerPage(String name) {
		return snapshotManager.getOwnerPage(name);
	}

	String getSelectedImageName() {
		return snapshotManager.getSelectedImageName();
	}

	void setSelectedIndex(int i) {
		snapshotManager.setSelectedIndex(i);
	}

	int getSelectedIndex() {
		return snapshotManager.getSelectedIndex();
	}

	void setSelectedImageName(String name) {
		snapshotManager.setSelectedImageName(name);
	}

	public ImageIcon loadAnnotatedImage(String name) {
		return snapshotManager.loadAnnotatedImage(name);
	}

	ImageIcon loadSelectedAnnotatedImage() {
		return snapshotManager.loadSelectedAnnotatedImage();
	}

	ImageIcon loadAnnotatedImage(int i) {
		return snapshotManager.loadAnnotatedImage(i);
	}

	public String[] getImageNames() {
		return snapshotManager.getImageNames();
	}

	ImageIcon getSelectedOriginalImage() {
		return snapshotManager.loadSelectedOriginalImage();
	}

	void addOriginalImage(ImageIcon image, String pageAddress) {
		snapshotManager.addImageName(image.getDescription(), pageAddress);
		ModelerUtilities.saveImageIcon(image,
				new File(snapshotManager.getOriginalImageFolder(), image.getDescription()), false);
	}

	public Image getThumbnail(int i) {
		return thumbnailManager.getThumbnail(getImageName(i));
	}

	public Image getThumbnail(String name) {
		return thumbnailManager.getThumbnail(name);
	}

	void putThumbnail(String name, Image image) {
		thumbnailManager.putThumbnail(name, image);
	}

	void stepBack() {
		snapshotManager.stepBack();
	}

	void stepForward() {
		snapshotManager.stepForward();
	}

	void putProperty(Object key, Object val) {
		snapshotManager.putProperty(key, val);
	}

	void removeProperty(Object key) {
		snapshotManager.removeProperty(key);
	}

	public Object getProperty(Object key) {
		return snapshotManager.getProperty(key);
	}

	boolean hasNoProperty() {
		return snapshotManager.hasNoProperty();
	}

	void addSnapshotListener(SnapshotListener listener) {
		snapshotManager.addSnapshotListener(listener);
	}

	void removeSnapshotListener(SnapshotListener listener) {
		snapshotManager.removeSnapshotListener(listener);
	}

	void notifyListeners(SnapshotEvent e) {
		snapshotManager.notifyListeners(e);
	}

	// old methods not passed to the SnapshotManager

	ThumbnailImagePanel createThumbnailImagePanel() {
		return new ThumbnailImagePanel(true);
	}

	void addFlashComponent(JComponent c) {
		if (flashComponents == null)
			flashComponents = new ArrayList<JComponent>();
		flashComponents.add(c);
	}

	void removeFlashComponent(JComponent c) {
		if (flashComponents == null)
			return;
		flashComponents.remove(c);
	}

	public void takeSnapshot(String address, Object source) {
		takeSnapshot(address, source, true);
	}

	void takeSnapshot(final String address, final Object source, final boolean inputDescription) {
		CAMERA_SOUND.play();
		String name = FileUtilities.removeSuffix(FileUtilities.getFileName(address));
		final String filename = name + "_" + ModelerUtilities.currentTimeToString() + ".png";
		if (source instanceof Component) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					addSnapshot((Component) source, filename, inputDescription, address);
				}
			});
		}
		else if (source instanceof ImageIcon) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					ImageIcon ii = new ImageIcon(((ImageIcon) source).getImage(), filename);
					addSnapshotImage(pageEditor, ii, filename, inputDescription, address);
				}
			});
		}
	}

	private void addSnapshot(Component c, String filename, boolean inputDescription, String pageAddress) {
		boolean b = c.isOpaque();
		if (c instanceof JComponent)
			((JComponent) c).setOpaque(true);
		ImageIcon image = ModelerUtilities.componentToImageIcon(c, filename, true);
		if (c instanceof JComponent)
			((JComponent) c).setOpaque(b);
		if (image == null)
			return;
		addSnapshotImage(c, image, filename, inputDescription, pageAddress);
	}

	private void addSnapshotImage(Component parent, ImageIcon image, String filename, boolean inputDescription,
			String pageAddress) {
		if (editor == null)
			editor = new SnapshotEditor(this);
		String s = Modeler.getInternationalText("TakeSnapshot");
		if (editor.showInputDialog(JOptionPane.getFrameForComponent(parent), image, s != null ? s : "Add Snapshot",
				inputDescription, false, pageAddress)) {
			if (flashComponents != null && !flashComponents.isEmpty()) {
				for (JComponent a : flashComponents) {
					if (a.isShowing()
							&& (a.getClientProperty("flashing") == null || a.getClientProperty("flashing") == Boolean.FALSE))
						Blinker.getDefault().createTimer(a, null, "Click me!").start();
				}
			}
		}
	}

	void setOwner(Editor pageEditor) {
		this.pageEditor = pageEditor;
	}

	void exportAll() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int w = Initializer.sharedInstance().getPreferences().getInt("Width", screenSize.width - 200);
		int h = Initializer.sharedInstance().getPreferences().getInt("Height", screenSize.height - 300);
		Modeler m = Modeler.openNewWindow(false, "Untitled.cml", 40, 20, w, h, true, true, true, true, false);
		m.editor.setEditable(true);
		m.editor.getPage().importSnapshots();
		m.editor.setViewPosition(0, 0);
	}

	int show() {

		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("Not dispatch thread");

		if (snapshotManager.isEmpty()) {
			String s1 = Modeler.getInternationalText("NoSnapshotAvailable");
			String s2 = Modeler.getInternationalText("GalleryIsEmpty");
			JOptionPane.showMessageDialog(pageEditor, s1 != null ? s1 : "No snapshot is available.", s2 != null ? s2
					: "Gallery is empty", JOptionPane.INFORMATION_MESSAGE);
			return -1;
		}

		if (dialog == null) {
			if (pageEditor != null) {
				dialog = new JDialog(JOptionPane.getFrameForComponent(pageEditor));
			}
			else {
				dialog = new JDialog();
			}
			String s = Modeler.getInternationalText("OpenSnapshotGalleryButton");
			dialog.setTitle(s == null ? "Snapshot Gallery" : s);
			dialog.setModal(true);
			dialog.addWindowListener(new WindowAdapter() {
				public void windowActivated(WindowEvent e) {
					thumbnailImagePanel.requestFocusInWindow();
				}
			});
		}
		else {
			if (pageEditor != null && dialog.getOwner() != JOptionPane.getFrameForComponent(pageEditor)) {
				Container c = dialog.getContentPane();
				dialog = new JDialog(JOptionPane.getFrameForComponent(pageEditor));
				dialog.setContentPane(c);
				String s = Modeler.getInternationalText("OpenSnapshotGalleryButton");
				dialog.setTitle(s == null ? "Snapshot Gallery" : s);
				dialog.setModal(true);
				dialog.addWindowListener(new WindowAdapter() {
					public void windowActivated(WindowEvent e) {
						thumbnailImagePanel.requestFocusInWindow();
					}
				});
			}
		}

		if (scroller == null) {
			scroller = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			scroller.setPreferredSize(new Dimension(600, ThumbnailImagePanel.IMAGE_HEIGHT + 36));
			dialog.getContentPane().add(scroller, BorderLayout.CENTER);
		}

		if (buttonPanel == null) {

			buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 5));
			dialog.getContentPane().add(buttonPanel, BorderLayout.NORTH);

			Dimension dim = Modeler.isMac() ? new Dimension(30, 30) : new Dimension(24, 24);

			editButton = new JButton(new ImageIcon(getClass().getResource("text/images/EditorMode.gif")));
			editButton.setPreferredSize(dim);
			editButton.setToolTipText("Annotate the selected image");
			editButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					invokeSnapshotEditor(thumbnailImagePanel, true, true);
				}
			});
			buttonPanel.add(editButton);

			exportButton = new JButton(IconPool.getIcon("save"));
			exportButton.setPreferredSize(dim);
			exportButton.setToolTipText("Save the selected image to disk");
			exportButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (snapshotManager.getSelectedImageName() == null)
						return;
					int i = snapshotManager.getSelectedIndex();
					if (i < 0)
						return;
					File file = snapshotManager.getAnnotatedImageFile(snapshotManager.getImageName(i));
					ModelerUtilities.saveTargetAs(pageEditor, file.toString());
					dialog.dispose();
				}
			});
			buttonPanel.add(exportButton);

			insertButton = new JButton(IconPool.getIcon("paste"));
			insertButton.setPreferredSize(dim);
			insertButton.setToolTipText("Paste the selected image into the current caret position on the page");
			insertButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (pageEditor.getPage().isEditable()) {
						pageEditor.getPage().insertIcon(
								snapshotManager.loadAnnotatedImage(snapshotManager.getSelectedImageName()));
						pageEditor.getPage().insertLineBreak();
						Object o = snapshotManager.getProperty("comment:" + snapshotManager.getSelectedImageName());
						if (o instanceof String)
							pageEditor.getPage().insertString((String) o);
						pageEditor.getPage().insertLineBreak();
						dialog.dispose();
					}
					else {
						JOptionPane.showMessageDialog(pageEditor,
								"Before inserting a snapshot image, please make the current page editable.",
								"Insertion aborted", JOptionPane.INFORMATION_MESSAGE);
					}
				}
			});
			buttonPanel.add(insertButton);

			removeButton = new JButton(IconPool.getIcon("cut"));
			removeButton.setToolTipText("Remove the selected image from the Snapshot Gallery");
			removeButton.setPreferredSize(dim);
			removeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					removeSelectedImage();
				}
			});
			buttonPanel.add(removeButton);

			clearButton = new JButton(IconPool.getIcon("erase"));
			clearButton.setToolTipText("Clear the Snapshot Gallery");
			clearButton.setPreferredSize(dim);
			clearButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (snapshotManager.isEmpty())
						return;
					String s1 = Modeler.getInternationalText("AreYouSureToClearSnapshotGallery");
					String s2 = Modeler.getInternationalText("ClearSnapshotGallery");
					if (JOptionPane.showConfirmDialog(pageEditor, s1 != null ? s1
							: "Are you sure you want to remove all snapshot images?", s2 != null ? s2
							: "Clear Snapshot Gallery", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
						return;
					enableButtons(false);
					clear();
					snapshotManager.notifyListeners(new SnapshotEvent(this, SnapshotEvent.SNAPSHOT_REMOVED));
				}
			});
			buttonPanel.add(clearButton);

			exportAllButton = new JButton(new ImageIcon(getClass().getResource("images/DocIn.gif")));
			exportAllButton.setPreferredSize(dim);
			exportAllButton.setToolTipText("Export all snapshot images in the Gallery to a report");
			exportAllButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dialog.dispose();
					exportAll();
				}
			});
			buttonPanel.add(exportAllButton);

		}

		if (bottomPanel == null) {

			bottomPanel = new JPanel();
			dialog.getContentPane().add(bottomPanel, BorderLayout.SOUTH);

			String s = Modeler.getInternationalText("CloseButton");
			JButton button = new JButton(s != null ? s : "Close");
			button.setToolTipText("Close the Snapshot Gallery");
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					option = -1;
					dialog.dispose();
				}
			});
			bottomPanel.add(button);

		}

		createThumbnailImages();

		enableButtons(snapshotManager.getSelectedImageName() != null);

		dialog.pack();
		dialog.setLocationRelativeTo(pageEditor);
		dialog.setVisible(true);

		return option;

	}

	void removeSelectedImage() {
		if (snapshotManager.getSelectedImageName() == null)
			return;
		String s1 = Modeler.getInternationalText("AreYouSureToRemoveSnapshot");
		String s2 = Modeler.getInternationalText("RemoveSnapshot");
		if (JOptionPane.showConfirmDialog(pageEditor, s1 != null ? s1
				: "Are you sure you want to remove this snapshot image?", s2 != null ? s2 : "Remove Snapshot",
				JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
			return;
		enableButtons(false);
		thumbnailManager.removeThumbnail(snapshotManager.removeSelectedImageName());
		snapshotManager.notifyListeners(new SnapshotEvent(this, SnapshotEvent.SNAPSHOT_REMOVED));
	}

	void invokeSnapshotEditor(Component parent, boolean inputDescription, boolean showSlideButtons) {
		if (snapshotManager.getSelectedImageName() == null || editor == null)
			return;
		String s = Modeler.getInternationalText("AnnotateSnapshot");
		editor.showInputDialog(parent, snapshotManager.loadSelectedOriginalImage(),
				s != null ? s : "Annotate Snapshot", inputDescription, showSlideButtons, null);
	}

	private void enableButtons(boolean b) {
		exportButton.setEnabled(b);
		insertButton.setEnabled(b);
		editButton.setEnabled(b);
		removeButton.setEnabled(b);
	}

	private boolean areButtonsEnabled() {
		return editButton.isEnabled();
	}

	void createThumbnailImages() {
		if (thumbnailImagePanel == null) {
			thumbnailImagePanel = new ThumbnailImagePanel();
			scroller.getViewport().setView(thumbnailImagePanel);
			addThumbnailListeners();
		}
		thumbnailImagePanel.scaleImages(null);
	}

	private void addThumbnailListeners() {
		thumbnailImagePanel.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				switch (e.getKeyCode()) {
				case KeyEvent.VK_DELETE:
				case KeyEvent.VK_BACK_SPACE:
					removeSelectedImage();
					break;
				}
			}
		});
		thumbnailImagePanel.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				thumbnailImagePanel.processMousePressedEvent(e);
				if (snapshotManager.getSelectedImageName() != null && !areButtonsEnabled())
					enableButtons(true);
			}
		});
	}

}