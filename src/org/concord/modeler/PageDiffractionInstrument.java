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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.concord.modeler.event.ModelEvent;
import org.concord.modeler.text.Page;
import org.concord.mw2d.DiffractionInstrument;
import org.concord.mw2d.models.MesoModel;
import org.concord.mw2d.models.MolecularModel;
import org.concord.mw2d.models.StructureFactor;

public class PageDiffractionInstrument extends DiffractionInstrument implements Embeddable, ModelCommunicator {

	Page page;
	String modelClass;
	int modelID = -1;
	boolean loadScan, scriptScan;
	private int index;
	private String uid;
	private boolean marked;
	private boolean changable;
	private JPopupMenu popupMenu;
	private static PageDiffractionInstrumentMaker maker;
	private MouseListener popupMouseListener;

	public PageDiffractionInstrument() {
		super(false);
		popupMouseListener = new PopupMouseListener(this);
		addMouseListener(popupMouseListener);
	}

	public PageDiffractionInstrument(PageDiffractionInstrument di, Page parent) {
		this();
		setPage(parent);
		setBorderType(di.getBorderType());
		setModelID(di.modelID);
		setUid(di.uid);
		ModelCanvas mc = page.getComponentPool().get(modelID);
		if (mc != null)
			mc.getMdContainer().getModel().addModelListener(this);
		setChangable(page.isEditable());
	}

	public void destroy() {
		if (modelID != -1) {
			ModelCanvas mc = page.getComponentPool().get(modelID);
			if (mc != null) {
				mc.getMdContainer().getModel().removeModelListener(this);
			}
		}
		page = null;
		if (maker != null)
			maker.setObject(null);
	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	public void createPopupMenu() {

		popupMenu = new JPopupMenu();
		popupMenu.setInvoker(this);

		String s = Modeler.getInternationalText("CustomizeThisDiffractionInstrument");
		final JMenuItem miCustom = new JMenuItem((s != null ? s : "Customize This Diffraction Instrument") + "...");
		miCustom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null) {
					maker = new PageDiffractionInstrumentMaker(PageDiffractionInstrument.this);
				}
				else {
					maker.setObject(PageDiffractionInstrument.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(miCustom);

		s = Modeler.getInternationalText("RemoveThisDiffractionInstrument");
		final JMenuItem miRemove = new JMenuItem(s != null ? s : "Remove This Diffraction Instrument");
		miRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(PageDiffractionInstrument.this);
			}
		});
		popupMenu.add(miRemove);

		s = Modeler.getInternationalText("CopyThisDiffractionInstrument");
		JMenuItem mi = new JMenuItem(s != null ? s : "Copy This Diffraction Instrument");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(PageDiffractionInstrument.this);
			}
		});
		popupMenu.add(mi);
		popupMenu.addSeparator();

		s = Modeler.getInternationalText("TakeSnapshot");
		mi = new JMenuItem((s != null ? s : "Take a Snapshot") + "...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SnapshotGallery.sharedInstance().takeSnapshot(page.getAddress(), pattern);
			}
		});
		popupMenu.add(mi);

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

	public void setIndex(int i) {
		index = i;
	}

	public int getIndex() {
		return index;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getUid() {
		return uid;
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

	public void setModelID(int i) {
		modelID = i;
		if (modelID != -1) {
			ModelCanvas mc = page.getComponentPool().get(modelID);
			if (mc != null) {
				Model m = mc.getMdContainer().getModel();
				if (m instanceof MesoModel) {
					// no meso model yet
					return;
				}
				setModel((MolecularModel) m);
			}
		}
	}

	public void setModelClass(String s) {
		modelClass = s;
	}

	public String getModelClass() {
		return modelClass;
	}

	public int getModelID() {
		return modelID;
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

	public void setLoadScan(boolean b) {
		loadScan = b;
	}

	public boolean getLoadScan() {
		return loadScan;
	}

	public void setScriptScan(boolean b) {
		scriptScan = b;
	}

	public boolean getScriptScan() {
		return scriptScan;
	}

	public static PageDiffractionInstrument create(Page page) {
		if (page == null)
			return null;
		PageDiffractionInstrument di = new PageDiffractionInstrument();
		if (maker == null) {
			maker = new PageDiffractionInstrumentMaker(di);
		}
		else {
			maker.setObject(di);
		}
		maker.invoke(page);
		if (maker.cancel)
			return null;
		return di;
	}

	public void modelUpdate(ModelEvent e) {
		switch (e.getID()) {
		case ModelEvent.MODEL_RUN:
			if (modelID != -1) {
				ModelCanvas mc = page.getComponentPool().get(modelID);
				if (mc != null) {
					if (mc.getMdContainer().getModel() == e.getSource()) {
						EventQueue.invokeLater(new Runnable() {
							public void run() {
								enableScan(false);
							}
						});
					}
				}
			}
			break;
		case ModelEvent.MODEL_STOP:
			if (modelID != -1) {
				ModelCanvas mc = page.getComponentPool().get(modelID);
				if (mc != null) {
					if (mc.getMdContainer().getModel() == e.getSource()) {
						EventQueue.invokeLater(new Runnable() {
							public void run() {
								enableScan(true);
							}
						});
					}
				}
			}
			break;
		case ModelEvent.MODEL_INPUT:
			if (loadScan && EventQueue.isDispatchThread())
				scan();
			break;
		case ModelEvent.SCRIPT_END:
			if (scriptScan && EventQueue.isDispatchThread())
				scan();
			break;
		}
	}

	public String toString() {
		return "<class>"
				+ getClass().getName()
				+ "</class>\n"
				+ "<model>"
				+ getModelID()
				+ "</model>"
				+ (getType() == StructureFactor.X_RAY ? "" : "<type>" + getType() + "</type>")
				+ (getLevelOfDetails() != StructureFactor.LINEAR_SCALING ? "<level_of_details>" + getLevelOfDetails()
						+ "</level_of_details>" : "")
				+ (getScale() == 8 ? "" : "<scale>" + getScale() + "</scale>")
				+ (loadScan ? "<loadscan>true</loadscan>" : "")
				+ (scriptScan ? "<scriptscan>true</scriptscan>" : "")
				+ (getBorderType().equals(BorderManager.BORDER_TYPE[0]) ? "" : "<border>" + getBorderType()
						+ "</border>");
	}

}
