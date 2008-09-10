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

package org.concord.mw2d.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.SimulatorMenuBar;
import org.concord.modeler.util.FileChooser;
import org.concord.mw2d.MDView;
import org.concord.mw2d.MesoView;
import org.concord.mw2d.UserAction;
import org.concord.mw2d.models.MDModel;
import org.concord.mw2d.models.MesoModel;

public class GBContainer extends MDContainer {

	protected MesoView view;
	protected MesoModel model;

	private MB mb;
	private TB tb;
	private FileChooser fileChooser;
	private GayBerneConfigure gbConfigure;

	public GBContainer() {
		super();
		if (prefs != null) {
			init(400, 250, prefs.getInt("Tape Length", 200));
		}
		else {
			init(400, 250, 200);
		}
	}

	public GBContainer(int tapeLength) {
		super();
		init(400, 250, tapeLength);
	}

	public String getRepresentationName() {
		return getCompatibleName();
	}

	/**
	 * return a representation name backward compatible to Version 1.3. What a stupid example of using class names to
	 * represent data.
	 */
	public final static String getCompatibleName() {
		return "org.concord.mw2d.activity.GBContainer";
	}

	private void init(int width, int height, int tapeLength) {

		setLayout(new BorderLayout());

		view = new MesoView();
		model = new MesoModel(width, height, tapeLength);
		view.setModel(model);
		view.enablePopupMenu(true);
		view.addActionStateListener(this);
		Action a = new ShowEnergyAction(model);
		model.getActions().put((String) a.getValue(Action.SHORT_DESCRIPTION), a);
		model.setReminder(reminder);

		setFileChooser(ModelerUtilities.fileChooser);
		createMenuBar();
		createMoviePanel();

		add(view, BorderLayout.CENTER);
		add(moviePanel, BorderLayout.SOUTH);

		model.getMovie().setCapacity(tapeLength);
		if (!model.hasEmbeddedMovie())
			initTape(tapeLength);

		view.setToolBar(createToolBar());

		model.getActions().put((String) resizeModelAction.getValue(Action.SHORT_DESCRIPTION), resizeModelAction);

	}

	private void initTape(int length) {
		model.getModelTimeQueue().setLength(length);
		for (int i = 0; i < model.getNumberOfParticles(); i++)
			model.getParticle(i).initializeMovieQ(length);
	}

	/**
	 * When the parent of this container is closed, destroy it to prevent memory leak.
	 */
	public void destroy() {
		super.destroy();
		if (model != null)
			model.destroy();
		if (mb != null)
			mb.destroy();
		if (gbConfigure != null)
			gbConfigure = null;
	}

	public MDView getView() {
		return view;
	}

	public MDModel getModel() {
		return model;
	}

	public void setFileChooser(FileChooser fc) {
		fileChooser = fc;
	}

	public FileChooser getFileChooser() {
		return fileChooser;
	}

	public void setProgressBar(JProgressBar progressBar) {
		model.setIOProgressBar(progressBar);
	}

	public JMenuBar getMenuBar() {
		return mb;
	}

	public JMenuBar createMenuBar() {
		if (mb == null)
			mb = new MB(model);
		return mb;
	}

	public JPanel getToolBar() {
		return tb;
	}

	public boolean removeToolbar() {
		if (super.removeToolbar()) {
			mb.removeToolBarItem.setEnabled(false);
			return true;
		}
		return false;
	}

	public boolean addToolbar() {
		if (super.addToolbar()) {
			if (isAuthorable()) {
				mb.removeToolBarItem.setEnabled(true);
				return true;
			}
		}
		return false;
	}

	public JPanel createToolBar() {
		if (tb == null)
			tb = new TB();
		return tb;
	}

	public int enableRecorder(boolean b) {
		if (super.enableRecorder(b) == JOptionPane.NO_OPTION)
			return JOptionPane.NO_OPTION;
		mb.enableMovieMenuItems(b);
		ModelerUtilities.selectWithoutNotifyingListeners(mb.disableRecorderItem, !b);
		return JOptionPane.YES_OPTION;
	}

	private class MB extends SimulatorMenuBar {

		JMenuItem energyTSItem, disableRecorderItem, removeToolBarItem, dragOnlyWhenEditingMenuItem;

		private void enableMovieMenuItems(boolean b) {
			energyTSItem.setEnabled(b);
		}

		MB(MesoModel m) {

			super(m);

			/* file menu */

			add(createFileMenu());

			/* edit menu */

			add(createEditMenu());

			/* tool bar menu */

			removeToolBarItem = new JMenuItem("Remove Toolbar");
			add(createToolBarMenu(removeToolBarItem));

			/* option menu */

			String s = getInternationalText("Option");
			JMenu menu = new JMenu(s != null ? s : "Options");
			menu.setMnemonic(KeyEvent.VK_O);
			menu.addMenuListener(new MenuListener() {
				public void menuSelected(MenuEvent e) {
					disableRecorderItem.setEnabled(!model.hasGraphs());
					setMenuItemWithoutNotifyingListeners(dragOnlyWhenEditingMenuItem, view
							.getDragObjectOnlyWhenEditing());
				}

				public void menuCanceled(MenuEvent e) {
				}

				public void menuDeselected(MenuEvent e) {
				}
			});
			add(menu);

			s = getInternationalText("DisableRecorder");
			disableRecorderItem = new JCheckBoxMenuItem(s != null ? s : "Disable Recorder");
			disableRecorderItem.setMnemonic(KeyEvent.VK_D);
			disableRecorderItem.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (enableRecorder(e.getStateChange() == ItemEvent.DESELECTED) == JOptionPane.NO_OPTION) {
						ModelerUtilities.selectWithoutNotifyingListeners(disableRecorderItem, false);
					}
					model.notifyChange();
				}
			});
			menu.add(disableRecorderItem);

			s = getInternationalText("DragObjectsOnlyWhenEditing");
			dragOnlyWhenEditingMenuItem = new JCheckBoxMenuItem(s != null ? s : "Drag Objects Only When Editing");
			dragOnlyWhenEditingMenuItem.setMnemonic(KeyEvent.VK_D);
			dragOnlyWhenEditingMenuItem.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					view.setDragObjectOnlyWhenEditing(e.getStateChange() == ItemEvent.SELECTED);
					model.notifyChange();
				}
			});
			menu.add(dragOnlyWhenEditingMenuItem);

			s = getInternationalText("ShowActionTip");
			JMenuItem menuItem = new JCheckBoxMenuItem(s != null ? s : "Show Action Tip");
			menuItem.setMnemonic(KeyEvent.VK_A);
			menuItem.setSelected(true);
			menuItem.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					view.setActionTipEnabled(e.getStateChange() == ItemEvent.SELECTED);
				}
			});
			menu.add(menuItem);
			menu.addSeparator();

			menuItem = new JMenuItem(view.getActionMap().get("Snapshot"));
			s = getInternationalText("Snapshot");
			menuItem.setText((s != null ? s : menuItem.getText()) + "...");
			menuItem.setIcon(null);
			menu.add(menuItem);
			menu.addSeparator();

			s = getInternationalText("AutomaticReminder");
			menuItem = new JMenuItem((s != null ? s : "Set Up Automatic Reminder") + "...");
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setupAutomaticReminder();
				}
			});
			menu.add(menuItem);

			menuItem = new JMenuItem(view.getActionMap().get("Properties"));
			s = getInternationalText("Properties");
			menuItem.setText((s != null ? s : menuItem.getText()) + "...");
			menuItem.setIcon(null);
			menu.add(menuItem);

			menuItem = new JMenuItem(view.getActionMap().get("View Options"));
			s = getInternationalText("ViewOption");
			menuItem.setText((s != null ? s : menuItem.getText()) + "...");
			menuItem.setIcon(null);
			menu.add(menuItem);

			s = getInternationalText("TaskManager");
			menuItem = new JMenuItem((s != null ? s : "Task Manager") + "...");
			menuItem.setMnemonic(KeyEvent.VK_M);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					view.showTaskManager();
				}
			});
			menu.add(menuItem);
			menu.addSeparator();

			s = getInternationalText("ToolBox");
			JMenu subMenu = new JMenu(s != null ? s : "Toolbox");
			subMenu.setMnemonic(KeyEvent.VK_T);
			menu.add(subMenu);

			menuItem = new JMenuItem(view.getActionMap().get("Energizer"));
			s = getInternationalText("HeatCool");
			if (s != null)
				menuItem.setText(s);
			subMenu.add(menuItem);

			menuItem = new JMenuItem(view.getActionMap().get("Heat Bath"));
			s = getInternationalText("HeatBath");
			menuItem.setText((s != null ? s : menuItem.getText()) + "...");
			subMenu.add(menuItem);

			energyTSItem = new JMenuItem(model.getActions().get("Show kinetic, potential and total energies"));
			s = getInternationalText("EnergyTimeSeries");
			energyTSItem.setText((s != null ? s : "View Time Series of Energies") + "...");
			energyTSItem.setMnemonic(KeyEvent.VK_E);
			subMenu.add(energyTSItem);

			threadPreempt();

			addMouseListener(new MouseAdapter() {
				private boolean popupTrigger;

				public void mousePressed(MouseEvent e) {
					popupTrigger = e.isPopupTrigger();
				}

				public void mouseReleased(MouseEvent e) {
					if (popupTrigger || e.isPopupTrigger())
						defaultPopupMenu.show(MB.this, e.getX(), e.getY());
				}
			});

		}
	}

	public void addDefaultToolBar() {
		super.addDefaultToolBar();
		addToolBarButton(tb.selectObjectButton);
		addToolBarButton(tb.removeObjectsButton);
		addToolBarButton(tb.duplicateButton);
		addToolBarButton(tb.dropGBButton);
		addToolBarButton(tb.rotateObjectButton);
		addToolBarButton(tb.resizeButton);
		addToolBarButton(tb.heatButton);
		addToolBarButton(tb.coolButton);
	}

	private class TB extends ToolBar {

		private AbstractButton dropGBButton;
		private AbstractButton resizeButton;
		private AbstractButton changeOmegaButton;
		private AbstractButton ipolButton;
		private AbstractButton dpolButton;

		TB() {

			super();

			dropGBButton = createButton(UserAction.getAction(UserAction.ADGB_ID, model));
			final Runnable run1 = new Runnable() {
				public void run() {
					if (gbConfigure == null)
						gbConfigure = new GayBerneConfigure();
					JDialog d = gbConfigure.showDialog(JOptionPane.getFrameForComponent(view));
					d.setLocationRelativeTo(view);
					d.setVisible(true);
					view.resetAddObjectIndicator();
				}
			};
			dropGBButton.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() >= 2)
						run1.run();
				}
			});
			customizationAction.put(dropGBButton.getAction().getValue(Action.SHORT_DESCRIPTION), run1);
			toolBarButtonGroup.add(dropGBButton);

			resizeButton = createButton(UserAction.getAction(UserAction.RESI_ID, model));
			toolBarButtonGroup.add(resizeButton);

			changeOmegaButton = createButton(UserAction.getAction(UserAction.OMEG_ID, model));
			toolBarButtonGroup.add(changeOmegaButton);

			ipolButton = createButton(UserAction.getAction(UserAction.IPOL_ID, model));
			toolBarButtonGroup.add(ipolButton);

			dpolButton = createButton(UserAction.getAction(UserAction.DPOL_ID, model));
			toolBarButtonGroup.add(dpolButton);

			List<AbstractButton> list = new ArrayList<AbstractButton>();
			list.add(dropGBButton);
			list.add(duplicateButton);
			list.add(resizeButton);
			list.add(rotateObjectButton);
			list.add(removeObjectsButton);
			list.add(changeVelocityButton);
			list.add(changeOmegaButton);
			list.add(pcharButton);
			list.add(ncharButton);
			list.add(ipolButton);
			list.add(dpolButton);
			list.add(iresButton);
			list.add(dresButton);
			list.add(idmpButton);
			list.add(ddmpButton);
			list.add(heatButton);
			list.add(coolButton);
			String s = getInternationalText("EditingActions");
			actionCategory.put(s != null ? s : "Editing Actions", list);

		}

	}

}