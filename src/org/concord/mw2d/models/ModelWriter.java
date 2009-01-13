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
import org.concord.modeler.util.FileUtilities;
import org.concord.modeler.util.SwingWorker;
import org.concord.mw2d.MDView;

public class ModelWriter extends AbstractAction {

	private MDModel model;
	private boolean revertTape;
	private FileChooser fileChooser;

	public ModelWriter(String name, MDModel model) {
		super(name);
		this.model = model;
		init();
	}

	public ModelWriter(FileChooser fc, MDModel model) {
		super();
		this.model = model;
		setFileChooser(fc);
		init();
	}

	public ModelWriter(FileChooser fc, String name, MDModel model) {
		super(name);
		this.model = model;
		setFileChooser(fc);
		init();
	}

	private void init() {
		putValue(NAME, "Save Model As");
		putValue(SHORT_DESCRIPTION, "Save the current state of the model");
		putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		putValue(SMALL_ICON, IconPool.getIcon("save"));
		putValue(ACCELERATOR_KEY, System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(
				KeyEvent.VK_S, KeyEvent.META_MASK | KeyEvent.SHIFT_MASK, true) : KeyStroke.getKeyStroke(KeyEvent.VK_S,
				KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK, true));
	}

	public void setFileChooser(FileChooser fc) {
		if (fc == null)
			throw new RuntimeException("file chooser can't be null");
		fileChooser = fc;
	}

	public FileChooser getFileChooser() {
		return fileChooser;
	}

	public void setRevertTape(boolean b) {
		revertTape = b;
	}

	public boolean getRevertTape() {
		return revertTape;
	}

	public void setModel(MDModel model) {
		this.model = model;
	}

	public MDModel getModel() {
		return model;
	}

	public void actionPerformed(ActionEvent e) {
		if (model instanceof AtomicModel) {
			write(FileFilterFactory.getFilter("mml"));
		}
		else if (model instanceof MesoModel) {
			write(FileFilterFactory.getFilter("gbl"));
		}
	}

	protected boolean write(FileFilter filter) {

		boolean success = true;

		File lastFile = fileChooser.getSelectedFile();
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.addChoosableFileFilter(filter);
		fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
		Object o = getValue("i18n");
		fileChooser.setDialogTitle(o != null ? (String) o : "Save the model");
		fileChooser.setApproveButtonMnemonic('S');
		fileChooser.setAccessory(null);
		String latestPath = fileChooser.getLastVisitedPath();
		if (latestPath != null)
			fileChooser.setCurrentDirectory(new File(latestPath));
		if (lastFile != null && !lastFile.isDirectory())
			fileChooser.handleFileTypeSwitching(lastFile);
		/*
		 * It is determined that recalling the last file is more important than setting the file name to be the current
		 * one. else { String url=(String)model.getProperty("url"); System.out.println(url); if(url==null) {
		 * fileChooser.resetTextField(); } else { try { File f2=new File(fileChooser.getCurrentDirectory(),
		 * FileUtilities.getFileName(url)); if(f2.exists()){ fileChooser.setSelectedFile(new
		 * File(fileChooser.getCurrentDirectory(), "Copy of "+FileUtilities.getFileName(url))); } else {
		 * fileChooser.setSelectedFile(f2); } } catch(NullPointerException npe){ npe.printStackTrace(System.err);
		 * fileChooser.resetTextField(); } } }
		 */

		if (fileChooser.showSaveDialog(JOptionPane.getFrameForComponent(model.getView())) == JFileChooser.APPROVE_OPTION) {
			final File file = new File(FileUtilities.fileNameAutoExtend(filter, fileChooser.getSelectedFile()));
			if (file.exists()) {
				String s = MDView.getInternationalText("FileExists");
				String s1 = MDView.getInternationalText("File");
				String s2 = MDView.getInternationalText("Overwrite");
				String s3 = MDView.getInternationalText("Exists");
				if (JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(model.getView()), (s1 != null ? s1
						: "File")
						+ " "
						+ file.getName()
						+ " "
						+ (s3 != null ? s3 : "exists")
						+ ", "
						+ (s2 != null ? s2 : "overwrite") + "?", s != null ? s : "File exists",
						JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
					fileChooser.resetChoosableFileFilters();
					return false;
				}
			}
			if (!filter.accept(file)) {
				JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(model.getView()),
						"Sorry, extension name should be " + filter.getDescription() + ". Write aborted.");
				success = false;
			}
			else {
				new SwingWorker("Model Writer") {
					public Object construct() {
						model.output(file);
						return file;
					}

					public void finished() {
						if (revertTape)
							model.insertNewTape();
						ModelWriter.this.firePropertyChange("write model", new File(file.getParent()), file);
					}
				}.start();
				fileChooser.rememberPath(fileChooser.getCurrentDirectory().toString());
				// fileChooser.rememberFile(file.getAbsolutePath());
			}
		}

		fileChooser.resetChoosableFileFilters();
		return success;

	}

}