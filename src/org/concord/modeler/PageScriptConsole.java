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
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.concord.modeler.event.ModelEvent;
import org.concord.modeler.event.ScriptEvent;
import org.concord.modeler.event.ScriptListener;
import org.concord.modeler.text.Page;
import org.concord.modeler.util.FileUtilities;

public class PageScriptConsole extends JPanel implements Embeddable, ModelCommunicator, EnterListener, ScriptListener {

	Page page;
	String modelClass;
	int modelID = -1;
	private int index;
	private String id;
	private boolean marked;
	private boolean changable;
	private JPopupMenu popupMenu;
	private ConsoleTextPane console;
	private JLabel label;
	private static PageScriptConsoleMaker maker;
	private MouseListener popupMouseListener;

	public PageScriptConsole() {

		super(new BorderLayout());

		console = new ConsoleTextPane(this);
		console.setBorder(BorderFactory.createLoweredBevelBorder());
		console.setPrompt();
		console.appendNewline();
		console.setPrompt();
		add(new JScrollPane(console), BorderLayout.CENTER);

		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 1));
		add(p, BorderLayout.SOUTH);
		label = new JLabel(new ImageIcon(getClass().getResource("images/script.gif")), SwingConstants.LEFT);
		p.add(label);

		popupMouseListener = new PopupMouseListener(this);
		addMouseListener(popupMouseListener);

	}

	public PageScriptConsole(PageScriptConsole console, Page parent) {
		this();
		setPage(parent);
		setBorderType(console.getBorderType());
		setModelID(console.modelID);
		setModelClass(console.modelClass);
		setPreferredSize(console.getPreferredSize());
		setChangable(page.isEditable());
		setId(console.id);
	}

	boolean isTargetClass() {
		if (modelClass == null)
			return true;
		// historical reason: script console used to work only for Jmol
		for (Class c : targetClass) {
			if (modelClass.equals(c.getName()))
				return true;
		}
		return false;
	}

	private BasicModel getBasicModel() {
		if (isTargetClass()) {
			// historical reason: script console used to work only for Jmol
			if (modelClass == null || modelClass.equals(PageMolecularViewer.class.getName())) {
				Object o = page.getEmbeddedComponent(PageMolecularViewer.class, modelID);
				if (o instanceof PageMolecularViewer) {
					PageMolecularViewer mv = (PageMolecularViewer) o;
					mv.setScriptConsole(this);
					return mv;
				}
			}
			else if (modelClass.equals(PageMd3d.class.getName())) {
				Object o = page.getEmbeddedComponent(PageMd3d.class, modelID);
				if (o instanceof PageMd3d) {
					PageMd3d md = (PageMd3d) o;
					// md.setScriptConsole(this);
					return md;
				}
			}
			else if (modelClass.equals(PageJContainer.class.getName())) {
				Object o = page.getEmbeddedComponent(PageJContainer.class, modelID);
				if (o instanceof PageJContainer)
					return (PageJContainer) o;
			}
			else if (modelClass.equals(PageApplet.class.getName())) {
				Object o = page.getEmbeddedComponent(PageApplet.class, modelID);
				if (o instanceof PageApplet)
					return (PageApplet) o;
			}
		}
		else {
			if (modelID != -1) {
				return page.getComponentPool().get(modelID).getContainer().getModel();
			}
		}
		return null;
	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	public void createPopupMenu() {

		popupMenu = new JPopupMenu();
		popupMenu.setInvoker(this);

		String s = Modeler.getInternationalText("CustomizeScriptConsole");
		final JMenuItem miCustom = new JMenuItem((s != null ? s : "Customize This Script Console") + "...");
		miCustom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null) {
					maker = new PageScriptConsoleMaker(PageScriptConsole.this);
				}
				else {
					maker.setObject(PageScriptConsole.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(miCustom);

		s = Modeler.getInternationalText("RemoveScriptConsole");
		final JMenuItem miRemove = new JMenuItem(s != null ? s : "Remove This Script Console");
		miRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(PageScriptConsole.this);
			}
		});
		popupMenu.add(miRemove);

		s = Modeler.getInternationalText("CopyScriptConsole");
		JMenuItem miCopy = new JMenuItem(s != null ? s : "Copy This Script Console");
		miCopy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(PageScriptConsole.this);
			}
		});
		popupMenu.add(miCopy);

		popupMenu.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				miCustom.setEnabled(changable);
				miRemove.setEnabled(changable);
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}
		});

		popupMenu.pack();

	}

	public void destroy() {
		if (maker != null)
			maker.setObject(null);
	}

	public void setIndex(int i) {
		index = i;
	}

	public int getIndex() {
		return index;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setMarked(boolean b) {
		marked = b;
	}

	public boolean isMarked() {
		return marked;
	}

	public String getBorderType() {
		return BorderManager.getBorder(this);
	}

	public void setBorderType(String s) {
		BorderManager.setBorder(this, s, page.getBackground());
	}

	public void setModelClass(String s) {
		modelClass = s;
		setLabel();
	}

	public String getModelClass() {
		return modelClass;
	}

	public void setModelID(int i) {
		modelID = i;
		setLabel();
	}

	public int getModelID() {
		return modelID;
	}

	private void setLabel() {
		if (modelClass == null || modelID == -1)
			return;
		String text = Modeler.getInternationalText("ScriptConsole");
		if (text == null)
			text = "Script Console";
		text += ": ";
		if (modelClass.equals(PageMolecularViewer.class.getName())) {
			text += "Molecular Viewer";
		}
		else if (modelClass.equals(PageMd3d.class.getName())) {
			text += "3D Molecular Dynamics";
		}
		else if (modelClass.equals(PageJContainer.class.getName())) {
			text += "Plugin";
		}
		else if (modelClass.equals(PageApplet.class.getName())) {
			text += "Applet";
		}
		else {
			text += FileUtilities.getSuffix(modelClass);
		}
		label.setText(text + "#" + modelID);
	}

	public void setPage(Page p) {
		page = p;
	}

	public Page getPage() {
		return page;
	}

	public void setChangable(boolean b) {
		changable = b;
	}

	public boolean isChangable() {
		return changable;
	}

	public static PageScriptConsole create(Page page) {
		if (page == null)
			return null;
		PageScriptConsole s = new PageScriptConsole();
		if (maker == null) {
			maker = new PageScriptConsoleMaker(s);
		}
		else {
			maker.setObject(s);
		}
		maker.invoke(page);
		if (maker.cancel)
			return null;
		return s;
	}

	public void setPreferredSize(Dimension dim) {
		if (dim == null)
			throw new IllegalArgumentException("null object");
		if (dim.width == 0 || dim.height == 0)
			throw new IllegalArgumentException("zero dimension");
		super.setPreferredSize(dim);
		super.setMinimumSize(dim);
		super.setMaximumSize(dim);
	}

	public void scriptEcho(String strEcho) {
		if (strEcho != null)
			console.outputEcho(strEcho);
	}

	public void scriptStatus(String strStatus) {
		if (strStatus != null)
			console.outputStatus(strStatus);
	}

	public void notifyScriptTermination(String strMsg, int msWalltime) {
		if (strMsg != null)
			console.outputError(strMsg);
	}

	private void executeCommand() {
		String strCommand = console.getCommandString().trim();
		if (strCommand.length() == 0)
			return;
		console.requestFocus();
		if (strCommand.equalsIgnoreCase("clear") || strCommand.equalsIgnoreCase("cls")) {
			console.clearContent();
			console.appendNewline();
			console.setPrompt();
			return;
		}
		console.appendNewline();
		console.setPrompt();
		BasicModel m = getBasicModel();
		if (m == null)
			return;
		String strErrorMessage = m instanceof PagePlugin ? ((PagePlugin) m).runNativeScript(strCommand) : m
				.runScript(strCommand);
		if (strErrorMessage != null)
			console.outputError(strErrorMessage);
	}

	public void enterPressed() {
		executeCommand();
	}

	public void outputScriptResult(final ScriptEvent e) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				switch (e.getStatus()) {
				case ScriptEvent.FAILED:
				case ScriptEvent.HARMLESS:
					console.outputError(e.getDescription());
					break;
				case ScriptEvent.SUCCEEDED:
					console.outputEcho(e.getDescription());
					break;
				}
			}
		});
	}

	public void modelUpdate(ModelEvent e) {
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("<class>" + getClass().getName() + "</class>\n");
		if (modelClass != null)
			sb.append("<modelclass>" + modelClass + "</modelclass>\n");
		sb.append("<model>" + getModelID() + "</model>\n");
		sb.append("<width>" + getWidth() + "</width><height>" + getHeight() + "</height>\n");
		if (!getBorderType().equals(BorderManager.BORDER_TYPE[0]))
			sb.append("<border>" + getBorderType() + "</border>");
		return sb.toString();
	}

}