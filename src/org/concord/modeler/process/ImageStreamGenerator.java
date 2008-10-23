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

package org.concord.modeler.process;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.util.FileChooser;

/**
 * Create a sequence of images for a component to be concatenated into a stream vedio, such as SMIL.
 * 
 * @author Charles Xie
 */

public final class ImageStreamGenerator {

	private Component view;
	private Job job;
	private int indexOfFrame;
	private String name;
	private File dir;
	private String path;

	private static FileChooser fileChooser;
	private final static FileFilter filter = new FileFilter() {
		public boolean accept(File f) {
			return f.isDirectory();
		}

		public String getDescription() {
			return "DIRECTORY";
		}
	};

	/* the subtask of updating the cache data store */
	private final Loadable snapshoter = new AbstractLoadable(500, 100) {
		public void execute() {
			name = String.format("%06d", indexOfFrame);
			ModelerUtilities.screenshot(view, dir + System.getProperty("file.separator") + name + ".png", false);
			indexOfFrame++;
			if (indexOfFrame >= getLifetime()) {
				setCompleted(true);
				indexOfFrame = 0;
			}
		}

		public String getName() {
			return "Producing a series of snapshots";
		}

		public String getDescription() {
			return "This task produces a series of snapshot images.";
		}
	};

	public ImageStreamGenerator(Component v, Job j) {
		if (v == null || j == null)
			throw new IllegalArgumentException("Argument cannot be null.");
		job = j;
		view = v;
	}

	public void chooseDirectory() {

		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(view), "SMIL Movie Creator", true);
		String s = JobTable.getInternationalText("SMILMovieCreator");
		if (s != null)
			dialog.setTitle(s);
		dialog.setSize(400, 200);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		final JLabel text = new JLabel();
		text.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(), BorderFactory
				.createEmptyBorder(2, 6, 2, 6)));

		if (fileChooser == null) {
			fileChooser = new FileChooser();
			fileChooser.setAcceptAllFileFilterUsed(false);
			fileChooser.addChoosableFileFilter(filter);
			fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			s = JobTable.getInternationalText("SMILMovieCreator");
			fileChooser.setDialogTitle(s != null ? s : "Save image files in a directory");
			fileChooser.setApproveButtonMnemonic('J');
		}

		path = fileChooser.getLastVisitedPath();
		if (path != null) {
			fileChooser.setCurrentDirectory(new File(path));
		}
		else {
			path = fileChooser.getCurrentDirectory().toString();
		}
		text.setText(path);

		JPanel contentPane = new JPanel(new BorderLayout(5, 5));
		contentPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		dialog.setContentPane(contentPane);

		s = JobTable.getInternationalText("Browse");
		JButton button = new JButton(s != null ? s : "Browse");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (fileChooser.showSaveDialog(dialog) == JFileChooser.APPROVE_OPTION) {
					File file = fileChooser.getSelectedFile();
					if (!file.isDirectory()) {
						JOptionPane.showMessageDialog(fileChooser, "Input must be a directory. Please try again.",
								"Input Directory", JOptionPane.ERROR_MESSAGE);
						return;
					}
					path = file.toString();
					text.setText(path);
					if (path != null)
						fileChooser.rememberPath(path);
				}
			}
		});

		s = JobTable.getInternationalText("SelectParentDirectoryForImageStream");
		contentPane.add(new JLabel(s != null ? s
				: "Select the parent directory under which the image stream will be exported.", new ImageIcon(Job.class
				.getResource("images/recording.gif")), SwingConstants.LEFT), BorderLayout.NORTH);

		JPanel p = new JPanel();
		p.add(text);
		p.add(button);

		s = JobTable.getInternationalText("Frame");
		Object[] lengthChoices = new String[] { "100 " + (s != null ? s : "frames"),
				"200 " + (s != null ? s : "frames"), "400 " + (s != null ? s : "frames"),
				"800 " + (s != null ? s : "frames"), "1600 " + (s != null ? s : "frames"),
				"3200 " + (s != null ? s : "frames") };
		JComboBox comboBox = new JComboBox(lengthChoices);
		comboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					JComboBox cb = (JComboBox) e.getSource();
					switch (cb.getSelectedIndex()) {
					case 0:
						snapshoter.setLifetime(100);
						break;
					case 1:
						snapshoter.setLifetime(200);
						break;
					case 2:
						snapshoter.setLifetime(400);
						break;
					case 3:
						snapshoter.setLifetime(800);
						break;
					case 4:
						snapshoter.setLifetime(1600);
						break;
					case 5:
						snapshoter.setLifetime(3200);
						break;
					}
				}
			}
		});
		p.add(comboBox);

		String s1 = JobTable.getInternationalText("Step");
		s = JobTable.getInternationalText("Every");
		Object[] intervalChoices = new String[] { (s != null ? s : "Every") + "  100 " + (s1 != null ? s1 : "steps"),
				(s != null ? s : "Every") + "  200 " + (s1 != null ? s1 : "steps"),
				(s != null ? s : "Every") + "  300 " + (s1 != null ? s1 : "steps"),
				(s != null ? s : "Every") + "  400 " + (s1 != null ? s1 : "steps"),
				(s != null ? s : "Every") + "  500 " + (s1 != null ? s1 : "steps"),
				(s != null ? s : "Every") + "  600 " + (s1 != null ? s1 : "steps"),
				(s != null ? s : "Every") + "  700 " + (s1 != null ? s1 : "steps"),
				(s != null ? s : "Every") + "  800 " + (s1 != null ? s1 : "steps"),
				(s != null ? s : "Every") + "  900 " + (s1 != null ? s1 : "steps"),
				(s != null ? s : "Every") + " 1000 " + (s1 != null ? s1 : "steps") };
		comboBox = new JComboBox(intervalChoices);
		comboBox.setSelectedIndex(4);
		comboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					JComboBox cb = (JComboBox) e.getSource();
					snapshoter.setInterval(100 * (1 + cb.getSelectedIndex()));
				}
			}
		});
		p.add(comboBox);

		contentPane.add(p, BorderLayout.CENTER);

		p = new JPanel();

		s = JobTable.getInternationalText("OK");
		button = new JButton(s != null ? s : "OK");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Calendar c = Calendar.getInstance();
				String s = Integer.toString(c.get(Calendar.YEAR)) + Integer.toString(c.get(Calendar.MONTH) + 1)
						+ Integer.toString(c.get(Calendar.DATE)) + Integer.toString(c.get(Calendar.HOUR_OF_DAY))
						+ Integer.toString(c.get(Calendar.MINUTE)) + Integer.toString(c.get(Calendar.SECOND));
				dir = new File(path, s);
				if (!dir.mkdir()) {
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							JOptionPane.showMessageDialog(dialog, "The directory " + dir.getPath()
									+ " cannot be created.\nCheck if the parent directory exists.", "Directory Error",
									JOptionPane.ERROR_MESSAGE);
						}
					});
					return;
				}
				job.add(snapshoter);
				dialog.dispose();
			}
		});
		p.add(button);

		s = JobTable.getInternationalText("Cancel");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});
		p.add(button);

		contentPane.add(p, BorderLayout.SOUTH);

		dialog.pack();
		dialog.setLocationRelativeTo(view);
		dialog.setVisible(true);

	}

}