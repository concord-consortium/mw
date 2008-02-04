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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;

import org.concord.modeler.ui.ListOrMapTransferHandler;

public class BookmarkManager {

	private final static BookmarkManager sharedInstance = new BookmarkManager();
	private LinkedHashMap<String, String> bookmarks;
	private List<BookmarkListener> listenerList;
	private JList list;
	private JDialog dialog;

	private BookmarkManager() {
		bookmarks = new LinkedHashMap<String, String>();
	}

	public final static BookmarkManager sharedInstance() {
		return sharedInstance;
	}

	public void addBookmarkListener(BookmarkListener listener) {
		if (listenerList == null)
			listenerList = new ArrayList<BookmarkListener>();
		listenerList.add(listener);
	}

	public void removeBookmarkListener(BookmarkListener listener) {
		if (listenerList == null)
			return;
		listenerList.remove(listener);
	}

	public LinkedHashMap<String, String> getBookmarks() {
		return bookmarks;
	}

	public void showDialog(Component parent) {
		if (dialog == null)
			createDialog(parent);
		fillList();
		dialog.setVisible(true);
	}

	private void fillList() {
		DefaultListModel listModel = new DefaultListModel();
		for (String key : bookmarks.keySet()) {
			listModel.addElement(key);
		}
		list.setModel(listModel);
	}

	private void removeSelectedItems() {
		Object[] tp = list.getSelectedValues();
		if (tp == null)
			return;
		DefaultListModel model = (DefaultListModel) list.getModel();
		for (Object o : tp) {
			model.removeElement(o);
			bookmarks.remove(o);
		}
		if (listenerList == null || listenerList.isEmpty())
			return;
		BookmarkEvent be = new BookmarkEvent(BookmarkManager.this, BookmarkEvent.BOOKMARK_SORTED);
		for (BookmarkListener listener : listenerList) {
			listener.bookmarkUpdated(be);
		}
	}

	private void createDialog(Component parent) {

		dialog = new JDialog(JOptionPane.getFrameForComponent(parent), "Bookmark Manager", true);
		String s = Modeler.getInternationalText("ManageBookmark");
		if (s != null)
			dialog.setTitle(s);
		dialog.setResizable(true);
		dialog.getContentPane().setLayout(new BorderLayout());

		list = new JList();
		list.setCellRenderer(new BookmarkCellRenderer());
		list.setDragEnabled(true);
		ToolTipManager.sharedInstance().registerComponent(list);
		list.setBorder(BorderFactory.createLoweredBevelBorder());
		list.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				int code = e.getKeyCode();
				if (code == KeyEvent.VK_DELETE || code == KeyEvent.VK_BACK_SPACE) {
					removeSelectedItems();
				}
			}
		});
		// drag and drop support
		ListOrMapTransferHandler handler = new ListOrMapTransferHandler(bookmarks);
		handler.setJobAfterTransfer(new Runnable() {
			public void run() {
				BookmarkEvent be = new BookmarkEvent(BookmarkManager.this, BookmarkEvent.BOOKMARK_SORTED);
				for (BookmarkListener listener : listenerList) {
					listener.bookmarkUpdated(be);
				}
			}
		});
		list.setTransferHandler(handler);
		list.setDragEnabled(true);

		JScrollPane scroll = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		scroll.setViewportView(list);
		scroll.setPreferredSize(new Dimension(500, 400));

		dialog.getContentPane().add(scroll, BorderLayout.CENTER);

		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		s = Modeler.getInternationalText("CtrlOrShiftClickToSelectMultipleBookmarks");
		panel.add(new JLabel(s != null ? s : "Ctrl/Shift+Click to select multiple. Drag to sort.  "));

		s = Modeler.getInternationalText("Remove");
		JButton button = new JButton(s != null ? s : "Remove");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeSelectedItems();
			}
		});
		panel.add(button);

		s = Modeler.getInternationalText("CloseButton");
		button = new JButton(s != null ? s : "Close");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});
		panel.add(button);

		dialog.getContentPane().add(panel, BorderLayout.SOUTH);

		dialog.pack();
		dialog.setLocationRelativeTo(parent);

		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dialog.dispose();
			}
		});

	}

	@SuppressWarnings("unchecked")
	public void readBookmarks(File file) {
		XMLDecoder in = null;
		try {
			in = new XMLDecoder(new BufferedInputStream(new FileInputStream(file)));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		if (in == null)
			return;
		try {
			bookmarks = (LinkedHashMap) in.readObject();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			in.close();
		}
	}

	public void writeBookmarks(File file) {
		XMLEncoder out = null;
		try {
			out = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(file)));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		if (out == null)
			return;
		out.writeObject(bookmarks);
		out.close();
	}

}