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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseListener;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.concord.modeler.event.ModelEvent;
import org.concord.modeler.text.Page;
import org.concord.modeler.text.XMLCharacterEncoder;
import org.concord.mw2d.ElectronicStructureViewer;
import org.concord.mw2d.event.ParameterChangeEvent;
import org.concord.mw2d.event.UpdateEvent;
import org.concord.mw2d.models.Atom;
import org.concord.mw2d.models.Element;
import org.concord.mw2d.models.EnergyLevel;
import org.concord.mw2d.models.MolecularModel;

public class PageElectronicStructureViewer extends ElectronicStructureViewer implements Scriptable, Embeddable,
		ModelCommunicator {

	Page page;
	String modelClass;
	int modelID = -1;
	int elementID = Element.ID_NT;
	private int index;
	private String uid;
	private boolean marked;
	private boolean wasOpaque;
	private boolean changable;
	private Color originalBackground, originalForeground;
	private JPopupMenu popupMenu;
	private static PageElectronicStructureViewerMaker maker;
	private MouseListener popupMouseListener;
	private ElectronicStructureViewerScripter scripter;

	public PageElectronicStructureViewer() {
		super();
		popupMouseListener = new PopupMouseListener(this);
		addMouseListener(popupMouseListener);
	}

	public PageElectronicStructureViewer(PageElectronicStructureViewer viewer, Page parent) {
		this();
		setUid(viewer.uid);
		setElementID(viewer.getElementID());
		setUpperBound(viewer.getUpperBound());
		setLowerBound(viewer.getLowerBound());
		setNumberOfTicks(viewer.getNumberOfTicks());
		setDrawTicks(viewer.getDrawTicks());
		setTitle(viewer.getTitle());
		setPage(parent);
		setOpaque(viewer.isOpaque());
		setBorderType(viewer.getBorderType());
		setModelID(viewer.modelID);
		setBackground(viewer.getBackground());
		setForeground(viewer.getForeground());
		setPreferredSize(viewer.getPreferredSize());
		ModelCanvas mc = page.getComponentPool().get(modelID);
		if (mc != null) {
			mc.getMdContainer().getModel().addModelListener(this);
			mc.getMdContainer().getModel().addUpdateListener(this);
			setElement(((MolecularModel) mc.getMdContainer().getModel()).getElement(getElementID()));
			addParameterChangeListener(mc.getMdContainer().getModel());
			scaleViewer();
		}
		setChangable(page.isEditable());
	}

	public String runScript(String script) {
		if (scripter == null)
			scripter = new ElectronicStructureViewerScripter(this);
		return scripter.runScript(script);
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

		if (popupMenu != null)
			return;

		popupMenu = new JPopupMenu();
		popupMenu.setInvoker(this);

		String s = Modeler.getInternationalText("CustomizeElectronicStructure");
		final JMenuItem miCustom = new JMenuItem((s != null ? s : "Customize This Electronic Structure") + "...");
		miCustom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null) {
					maker = new PageElectronicStructureViewerMaker(PageElectronicStructureViewer.this);
				}
				else {
					maker.setObject(PageElectronicStructureViewer.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(miCustom);

		s = Modeler.getInternationalText("RemoveElectronicStructure");
		final JMenuItem miRemove = new JMenuItem(s != null ? s : "Remove This Electronic Structure");
		miRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(PageElectronicStructureViewer.this);
			}
		});
		popupMenu.add(miRemove);

		s = Modeler.getInternationalText("CopyElectronicStructure");
		JMenuItem mi = new JMenuItem(s != null ? s : "Copy This Electronic Structure");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(PageElectronicStructureViewer.this);
			}
		});
		popupMenu.add(mi);
		popupMenu.addSeparator();

		s = Modeler.getInternationalText("TakeSnapshot");
		mi = new JMenuItem((s != null ? s : "Take a Snapshot") + "...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SnapshotGallery.sharedInstance().takeSnapshot(page.getAddress(), PageElectronicStructureViewer.this);
			}
		});
		popupMenu.add(mi);
		popupMenu.addSeparator();

		s = Modeler.getInternationalText("RemoveSelectedExcitedState");
		final JMenuItem miDeleteLevel = new JMenuItem(s != null ? s : "Remove Selected Excited State");
		miDeleteLevel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeSelectedEnergyLevel();
			}
		});
		popupMenu.add(miDeleteLevel);

		s = Modeler.getInternationalText("RemoveAllExcitedStates");
		final JMenuItem miRemoveAll = new JMenuItem(s != null ? s : "Remove All Excited States");
		miRemoveAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeAllExcitedEnergyLevels();
			}
		});
		popupMenu.add(miRemoveAll);

		s = Modeler.getInternationalText("InsertEnergyLevel");
		final JMenuItem miInsertLevel = new JMenuItem(s != null ? s : "Insert Energy Level");
		miInsertLevel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				insertEnergyLevel(getMouseY());
			}
		});
		popupMenu.add(miInsertLevel);

		s = Modeler.getInternationalText("CreateContinuum");
		final JMenuItem miContinuum = new JMenuItem(s != null ? s : "Create a Continuum");
		miContinuum.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createContinuum();
			}
		});
		popupMenu.add(miContinuum);
		popupMenu.addSeparator();

		s = Modeler.getInternationalText("SetLifetimeOfSelectedEnergyLevel");
		final JMenu lifetimeMenu = new JMenu(s != null ? s : "Set Lifetime of Selected Energy Level");
		popupMenu.add(lifetimeMenu);

		ButtonGroup bg = new ButtonGroup();

		s = Modeler.getInternationalText("Short");
		final JMenuItem miShort = new JRadioButtonMenuItem(s != null ? s : "Short");
		miShort.setSelected(true);
		miShort.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					EnergyLevel level = getSelectedEnergyLevel();
					if (level != null) {
						level.setLifetime(EnergyLevel.SHORT_LIFETIME);
						notifyListeners(new ParameterChangeEvent(PageElectronicStructureViewer.this,
								"Lifetime changed", level, new Integer(elementID)));
					}
				}
			}
		});
		lifetimeMenu.add(miShort);
		bg.add(miShort);

		s = Modeler.getInternationalText("Metastable");
		final JMenuItem miMedium = new JRadioButtonMenuItem(s != null ? s : "Metastable");
		miMedium.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					EnergyLevel level = getSelectedEnergyLevel();
					if (level != null) {
						level.setLifetime(EnergyLevel.MEDIUM_LIFETIME);
						notifyListeners(new ParameterChangeEvent(PageElectronicStructureViewer.this,
								"Lifetime changed", level, new Integer(elementID)));
					}
				}
			}
		});
		lifetimeMenu.add(miMedium);
		bg.add(miMedium);

		s = Modeler.getInternationalText("Long");
		final JMenuItem miLong = new JRadioButtonMenuItem(s != null ? s : "Long");
		miLong.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					EnergyLevel level = getSelectedEnergyLevel();
					if (level != null) {
						level.setLifetime(EnergyLevel.LONG_LIFETIME);
						notifyListeners(new ParameterChangeEvent(PageElectronicStructureViewer.this,
								"Lifetime changed", level, new Integer(elementID)));
					}
				}
			}
		});
		lifetimeMenu.add(miLong);
		bg.add(miLong);

		popupMenu.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				miCustom.setEnabled(changable);
				miRemove.setEnabled(changable);
				EnergyLevel level = getSelectedEnergyLevel();
				miDeleteLevel.setEnabled(level != null && !getLockEnergyLevels());
				lifetimeMenu.setEnabled(level != null);
				miInsertLevel.setEnabled(level == null && !getLockEnergyLevels());
				miContinuum.setEnabled(!getLockEnergyLevels());
				if (level != null) {
					switch (level.getLifetime()) {
					case EnergyLevel.SHORT_LIFETIME:
						miShort.setSelected(true);
						break;
					case EnergyLevel.MEDIUM_LIFETIME:
						miMedium.setSelected(true);
						break;
					case EnergyLevel.LONG_LIFETIME:
						miLong.setSelected(true);
						break;
					}
				}
				miRemoveAll.setEnabled(getElement().getElectronicStructure().getNumberOfEnergyLevels() > 1
						&& !getLockEnergyLevels());
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

	public void setPage(Page p) {
		page = p;
	}

	public Page getPage() {
		return page;
	}

	public void setModelClass(String s) {
		modelClass = s;
	}

	public String getModelClass() {
		return modelClass;
	}

	public void setModelID(int i) {
		modelID = i;
	}

	public int getModelID() {
		return modelID;
	}

	public void setMarked(boolean b) {
		marked = b;
		if (page == null)
			return;
		if (b) {
			originalBackground = getBackground();
			originalForeground = getForeground();
			wasOpaque = isOpaque();
			setOpaque(true);
		}
		else {
			setOpaque(wasOpaque);
		}
		setBackground(b ? page.getSelectionColor() : originalBackground);
		setForeground(b ? page.getSelectedTextColor() : originalForeground);
	}

	public boolean isMarked() {
		return marked;
	}

	public void setChangable(boolean b) {
		changable = b;
	}

	public boolean isChangable() {
		return changable;
	}

	public void setElementID(int elementID) {
		this.elementID = elementID;
	}

	public int getElementID() {
		return elementID;
	}

	public String getBorderType() {
		return BorderManager.getBorder(this);
	}

	public void setBorderType(String s) {
		BorderManager.setBorder(this, s, page.getBackground());
	}

	public static PageElectronicStructureViewer create(Page page) {
		if (page == null)
			return null;
		PageElectronicStructureViewer es = new PageElectronicStructureViewer();
		es.setFont(page.getFont());
		if (maker == null) {
			maker = new PageElectronicStructureViewerMaker(es);
		}
		else {
			maker.setObject(es);
		}
		maker.invoke(page);
		if (maker.cancel)
			return null;
		return es;
	}

	public void setPreferredSize(Dimension dim) {
		if (dim == null)
			throw new IllegalArgumentException("You must input a non-null object");
		if (dim.width == 0 || dim.height == 0)
			throw new IllegalArgumentException("You must input a non-zero dimension");
		super.setPreferredSize(dim);
		super.setMinimumSize(dim);
		super.setMaximumSize(dim);
	}

	public void modelUpdate(ModelEvent e) {
		Object src = e.getSource();
		int id = e.getID();
		if (id == ModelEvent.MODEL_INPUT) {
			setElement(((MolecularModel) src).getElement(elementID));
			viewUpdated(new UpdateEvent(src));
		}
	}

	public void viewUpdated(UpdateEvent e) {
		Object src = e.getSource();
		if (src instanceof MolecularModel) {
			clearElectronView();
			MolecularModel mm = (MolecularModel) src;
			int n = mm.getNumberOfAtoms();
			Atom a = null;
			for (int i = 0; i < n; i++) {
				a = mm.getAtom(i);
				if (!a.isExcitable())
					continue;
				if (a.getID() == elementID && !a.getElectrons().isEmpty())
					addElectron(a.getElectron(0));
			}
			repaint();
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("<class>" + getClass().getName() + "</class>\n");
		sb.append("<model>" + modelID + "</model>\n");
		sb.append("<type>" + elementID + "</type>\n");
		if (uid != null)
			sb.append("<uid>" + uid + "</uid>\n");
		if (getTitle() != null)
			sb.append("<title>" + XMLCharacterEncoder.encode(getTitle()) + "</title>\n");
		sb.append("<minimum>" + getLowerBound() + "</minimum>\n");
		sb.append("<maximum>" + getUpperBound() + "</maximum>\n");
		sb.append("<nstep>" + getNumberOfTicks() + "</nstep>\n");
		if (getDrawTicks())
			sb.append("<tick>true</tick>\n");
		if (getLockEnergyLevels())
			sb.append("<lockenergylevel>true</lockenergylevel>\n");
		sb.append("<width>" + getWidth() + "</width>\n");
		sb.append("<height>" + (getHeight() - getVerticalMargin() * 2) + "</height>\n");
		if (!getBorderType().equals(BorderManager.BORDER_TYPE[0]))
			sb.append("<border>" + getBorderType() + "</border>\n");
		if (!isOpaque())
			sb.append("<opaque>false</opaque>\n");
		if (!getForeground().equals(Color.black))
			sb.append("<fgcolor>" + Integer.toString(getForeground().getRGB(), 16) + "</fgcolor>");
		if (!getBackground().equals(Color.white))
			sb.append("<bgcolor>" + Integer.toString(getBackground().getRGB(), 16) + "</bgcolor>");
		return sb.toString();
	}

}