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

package org.concord.mw2d.models;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;

import org.concord.modeler.FileFilterFactory;
import org.concord.modeler.ui.IconPool;
import org.concord.modeler.util.FileChooser;
import org.concord.modeler.util.SwingWorker;

public class ModelReader extends AbstractAction {

	private MDModel model;
	private FileChooser fileChooser;

	public ModelReader(String name, MDModel model) {
		super(name);
		this.model = model;
		init();
	}

	public ModelReader(FileChooser fc, MDModel model) {
		super();
		this.model = model;
		setFileChooser(fc);
		init();
	}

	public ModelReader(FileChooser fc, String name, MDModel model) {
		super(name);
		this.model = model;
		setFileChooser(fc);
		init();
	}

	private void init() {
		putValue(NAME, "Open Model");
		putValue(SHORT_DESCRIPTION, "Open a model on disk");
		putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_O));
		putValue(SMALL_ICON, IconPool.getIcon("open"));
		putValue(ACCELERATOR_KEY, System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(
				KeyEvent.VK_O, KeyEvent.META_MASK, true) : KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_MASK,
				true));
	}

	public void setFileChooser(FileChooser fc) {
		if (fc == null)
			throw new RuntimeException("file chooser can't be null");
		fileChooser = fc;
	}

	public FileChooser getFileChooser() {
		return fileChooser;
	}

	public void setModel(MDModel model) {
		this.model = model;
	}

	public MDModel getModel() {
		return model;
	}

	public void actionPerformed(ActionEvent e) {
		if (model instanceof AtomicModel) {
			read(FileFilterFactory.getFilter("mml"));
		}
		else if (model instanceof MesoModel) {
			read(FileFilterFactory.getFilter("gbl"));
		}
	}

	protected boolean read(FileFilter filter) {

		File lastFile = fileChooser.getSelectedFile();
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.addChoosableFileFilter(filter);
		fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
		Object o = getValue("i18n");
		fileChooser.setDialogTitle(o != null ? (String) o : "Load a model");
		fileChooser.setApproveButtonMnemonic('O');
		String latestPath = fileChooser.getLastVisitedPath();
		if (latestPath != null)
			fileChooser.setCurrentDirectory(new File(latestPath));
		fileChooser.recallLastFile(lastFile);
		fileChooser.setAccessory(null);
		if (fileChooser.showOpenDialog(JOptionPane.getFrameForComponent(model.getView())) == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();
			if (!filter.accept(selectedFile))
				selectedFile = new File(selectedFile.getAbsolutePath() + "." + filter);
			final File file = selectedFile;
			new SwingWorker("Model Reader") {
				public Object construct() {
					model.input(file);
					return file;
				}

				public void finished() {
					ModelReader.this.firePropertyChange("read model", new File(file.getParent()), file);
				}
			}.start();
			fileChooser.rememberPath(fileChooser.getCurrentDirectory().toString());
			// fileChooser.rememberFile(file.getAbsolutePath());
		}

		fileChooser.resetChoosableFileFilters();
		return true;

	}

}