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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.concord.modeler.event.ModelEvent;
import org.concord.modeler.text.Page;
import org.concord.mw2d.PhotonSpectrometer;
import org.concord.mw2d.models.Photon;

public class PagePhotonSpectrometer extends PhotonSpectrometer implements Scriptable, Embeddable, ModelCommunicator {

	Page page;
	String modelClass;
	int modelID = -1;
	private int index;
	private boolean marked;
	private boolean changable;
	private JPopupMenu popupMenu;
	private JMenuItem miCustom, miRemove;
	private static PagePhotonSpectrometerMaker maker;
	private MouseListener popupMouseListener;
	private SpectrometerScripter scripter;

	public PagePhotonSpectrometer() {
		super();
		popupMouseListener = new PopupMouseListener(this);
		addMouseListener(popupMouseListener);
	}

	public PagePhotonSpectrometer(PagePhotonSpectrometer pps, Page parent) {
		this();
		setPage(parent);
		setBorderType(pps.getBorderType());
		setModelID(pps.modelID);
		setType(pps.getType());
		setNumberOfTicks(pps.getNumberOfTicks());
		setLowerBound(pps.getLowerBound());
		setUpperBound(pps.getUpperBound());
		setPreferredSize(pps.getPreferredSize());
		ModelCanvas mc = page.getComponentPool().get(modelID);
		if (mc != null)
			mc.getContainer().getModel().addModelListener(this);
		setChangable(page.isEditable());
	}

	public String runScript(String script) {
		if (scripter == null)
			scripter = new SpectrometerScripter(this);
		return scripter.runScript(script);
	}

	public void destroy() {
		if (modelID != -1) {
			ModelCanvas mc = page.getComponentPool().get(modelID);
			if (mc != null) {
				mc.getContainer().getModel().removeModelListener(this);
			}
		}
		page = null;
	}

	public void setType(int i) {
		super.setType(i);
		if (i == PhotonSpectrometer.EMISSION) {
			if (miCustom != null)
				miCustom.setText("Customize This Emission Spectrometer");
			if (miRemove != null)
				miRemove.setText("Remove This Emission Spectrometer");
		}
		else if (i == PhotonSpectrometer.ABSORPTION) {
			if (miCustom != null)
				miCustom.setText("Customize This Absorption Spectrometer");
			if (miRemove != null)
				miRemove.setText("Remove This Absorption Spectrometer");
		}
	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	public void createPopupMenu() {

		popupMenu = new JPopupMenu();
		popupMenu.setInvoker(this);

		String s = Modeler.getInternationalText("CustomizeThisSpectrometer");
		miCustom = new JMenuItem((s != null ? s : "Customize This "
				+ (getType() == PhotonSpectrometer.EMISSION ? "Emission" : "Absorption") + " Spectrometer")
				+ "...");
		miCustom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null) {
					maker = new PagePhotonSpectrometerMaker(PagePhotonSpectrometer.this);
				}
				else {
					maker.setObject(PagePhotonSpectrometer.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(miCustom);

		s = Modeler.getInternationalText("RemoveThisSpectrometer");
		miRemove = new JMenuItem(s != null ? s : "Remove This "
				+ (getType() == PhotonSpectrometer.EMISSION ? "Emission" : "Absorption") + " Spectrometer");
		miRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(PagePhotonSpectrometer.this);
			}
		});
		popupMenu.add(miRemove);

		s = Modeler.getInternationalText("CopyThisSpectrometer");
		JMenuItem mi = new JMenuItem(s != null ? s : "Copy This Spectrometer");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(PagePhotonSpectrometer.this);
			}
		});
		popupMenu.add(mi);
		popupMenu.addSeparator();

		s = Modeler.getInternationalText("TakeSnapshot");
		mi = new JMenuItem((s != null ? s : "Take a Snapshot") + "...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SnapshotGallery.sharedInstance().takeSnapshot(page.getAddress(), PagePhotonSpectrometer.this);
			}
		});
		popupMenu.add(mi);
		popupMenu.addSeparator();

		s = Modeler.getInternationalText("ClearLines");
		mi = new JMenuItem(s != null ? s : "Clear Lines");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearLines();
			}
		});
		popupMenu.add(mi);

		final JMenuItem miIntensity = new JMenuItem("Show Line Intensity...");
		miIntensity.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showLineIntensity();
			}
		});
		popupMenu.add(miIntensity);

		popupMenu.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				miCustom.setEnabled(changable);
				miRemove.setEnabled(changable);
				miIntensity.setEnabled(getNumberOfLines() > 0);
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

	public void setPreferredSize(Dimension dim) {
		if (dim == null)
			throw new IllegalArgumentException("null object");
		if (dim.width == 0 || dim.height == 0)
			throw new IllegalArgumentException("zero dimension");
		super.setPreferredSize(dim);
		super.setMinimumSize(dim);
		super.setMaximumSize(dim);
	}

	public static PagePhotonSpectrometer create(Page page) {
		if (page == null)
			return null;
		PagePhotonSpectrometer ps = new PagePhotonSpectrometer();
		if (maker == null) {
			maker = new PagePhotonSpectrometerMaker(ps);
		}
		else {
			maker.setObject(ps);
		}
		maker.invoke(page);
		if (maker.cancel)
			return null;
		return ps;
	}

	public void modelUpdate(ModelEvent e) {
		int id = e.getID();
		if (id == ModelEvent.MODEL_INPUT) {
			clearLines();
		}
		String description = e.getDescription();
		if (description != null) {
			if (getType() == PhotonSpectrometer.EMISSION) {
				if (description.equals("Photon emitted")) {
					if (modelID != -1) {
						ModelCanvas mc = page.getComponentPool().get(modelID);
						if (mc != null) {
							if (mc.getContainer().getModel() == e.getSource()) {
								receivePhoton((Photon) e.getCurrentState());
							}
						}
					}
				}
			}
			else if (getType() == PhotonSpectrometer.ABSORPTION) {
				if (description.equals("Photon absorbed")) {
					if (modelID != -1) {
						ModelCanvas mc = page.getComponentPool().get(modelID);
						if (mc != null) {
							if (mc.getContainer().getModel() == e.getSource()) {
								Photon p = (Photon) e.getCurrentState();
								if (p.isFromLightSource())
									receivePhoton(p);
							}
						}
					}
				}
			}
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("<class>" + getClass().getName() + "</class>\n");
		sb.append("<model>" + getModelID() + "</model>\n");
		sb.append("<minimum>" + getLowerBound() + "</minimum>\n");
		sb.append("<maximum>" + getUpperBound() + "</maximum>\n");
		sb.append("<nstep>" + getNumberOfTicks() + "</nstep>\n");
		if (getType() != PhotonSpectrometer.EMISSION)
			sb.append("<type>" + getType() + "</type>\n");
		sb.append("<width>" + getWidth() + "</width><height>" + getHeight() + "</height>\n");
		if (!getBorderType().equals(BorderManager.BORDER_TYPE[0]))
			sb.append("<border>" + getBorderType() + "</border>");
		return sb.toString();
	}

}
