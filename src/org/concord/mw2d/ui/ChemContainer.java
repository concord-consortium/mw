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
import java.awt.EventQueue;
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
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.SimulatorMenuBar;
import org.concord.modeler.util.FileChooser;
import org.concord.mw2d.AtomisticView;
import org.concord.mw2d.MDView;
import org.concord.mw2d.UserAction;
import org.concord.mw2d.models.Element;
import org.concord.mw2d.models.MDModel;
import org.concord.mw2d.models.Reaction;
import org.concord.mw2d.models.ReactionModel;

public class ChemContainer extends MDContainer {

	protected AtomisticView view;
	protected ReactionModel model;

	private MB mb;
	private TB tb;
	private FileChooser fileChooser;
	private ReactionDialog reactionDialog;

	public ChemContainer() {
		super();
		if (prefs != null) {
			init(400, 250, prefs.getInt("Tape Length", 200));
		}
		else {
			init(400, 250, 200);
		}
	}

	public ChemContainer(int tapeLength) {
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
		return "org.concord.mw2d.activity.ChemContainer";
	}

	private void init(int width, int height, int tapeLength) {

		setLayout(new BorderLayout());

		view = new AtomisticView();
		model = new ReactionModel(width, height, tapeLength);
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
		for (int i = 0, n = model.getNumberOfParticles(); i < n; i++) {
			model.getAtom(i).initializeMovieQ(length);
			model.getAtom(i).initializeRadicalQ(length);
		}
		model.getBonds().initializeBondQ(length);
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

	private void setReaction(final Reaction r) {
		if (r.getClass().isInstance(model.getType()))
			return;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (JOptionPane
						.showConfirmDialog(
								view,
								"Caution! Changing the reaction type for\nthe model may affect other components on\nthe page. Do you want to continue?",
								"Changing reaction type", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
					model.setType(r);
					if (model.getRecorderDisabled())
						return;
					r.init(model.getMovie().getCapacity(), model.getModelTimeQueue());
				}
			}
		});
	}

	/* inner classes */

	private class MB extends SimulatorMenuBar {

		private JMenuItem energyTSItem, disableRecorderItem, removeToolBarItem;

		private void enableMovieMenuItems(boolean b) {
			energyTSItem.setEnabled(b);
		}

		MB(ReactionModel m) {

			super(m);

			/* file menu */

			add(createFileMenu());

			/* edit menu */

			add(createEditMenu());

			/* tool bar menu */

			removeToolBarItem = new JMenuItem("Remove Toolbar");
			add(createToolBarMenu(removeToolBarItem));

			/* reaction menu */

			final JMenu directionMenu = new JMenu("Direction Setting");

			String s = getInternationalText("Reaction");
			JMenu menu = new JMenu(s != null ? s : "Reaction");
			menu.setMnemonic(KeyEvent.VK_R);
			menu.addMenuListener(new MenuListener() {
				public void menuSelected(MenuEvent e) {
					if (model.getType() instanceof Reaction.A2_B2__2AB) {
						directionMenu.setEnabled(true);
					}
					else if (model.getType() instanceof Reaction.A2_B2_C__2AB_C) {
						directionMenu.setEnabled(false);
					}
					else if (model.getType() instanceof Reaction.nA__An) {
						directionMenu.setEnabled(false);
					}
					else if (model.getType() instanceof Reaction.O2_2H2__2H2O) {
						directionMenu.setEnabled(false);
					}
				}

				public void menuCanceled(MenuEvent e) {
				}

				public void menuDeselected(MenuEvent e) {
				}
			});
			add(menu);

			final JCheckBoxMenuItem rmi1 = new JCheckBoxMenuItem(
					"<html>Free Radical Substitution: A<sub>2</sub> + B<sub>2</sub> &#8660; 2AB</html>");
			final JCheckBoxMenuItem rmi2 = new JCheckBoxMenuItem(
					"<html>Homogenous Catalysis: A<sub>2</sub> + B<sub>2</sub> + C &#8660; 2AB + C</html>");
			final JCheckBoxMenuItem rmi3 = new JCheckBoxMenuItem(
					"<html>Polymerization: <i>n</i>A &#8660; A<sub><i>n</i></sub></html>");
			final JCheckBoxMenuItem rmi4 = new JCheckBoxMenuItem(
					"<html>Hydrogen-Oxygen Reaction: 2H<sub>2</sub> + O<sub>2</sub> &#8594; 2H<sub>2</sub>O</html>");

			JMenu subMenu = new JMenu("Choose a Reaction");
			subMenu.setMnemonic(KeyEvent.VK_C);
			subMenu.addMenuListener(new MenuListener() {
				public void menuSelected(MenuEvent e) {
					if (model.getType() instanceof Reaction.A2_B2__2AB)
						rmi1.setSelected(true);
					else if (model.getType() instanceof Reaction.A2_B2_C__2AB_C)
						rmi2.setSelected(true);
					else if (model.getType() instanceof Reaction.nA__An)
						rmi3.setSelected(true);
					else if (model.getType() instanceof Reaction.O2_2H2__2H2O)
						rmi4.setSelected(true);
				}

				public void menuCanceled(MenuEvent e) {
				}

				public void menuDeselected(MenuEvent e) {
				}
			});
			menu.add(subMenu);

			ButtonGroup bgReaction = new ButtonGroup();

			rmi1.setSelected(true);
			rmi1.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setReaction(new Reaction.A2_B2__2AB());
				}
			});
			subMenu.add(rmi1);
			bgReaction.add(rmi1);

			rmi2.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setReaction(new Reaction.A2_B2_C__2AB_C());
				}
			});
			subMenu.add(rmi2);
			bgReaction.add(rmi2);

			rmi3.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setReaction(new Reaction.nA__An());
				}
			});
			subMenu.add(rmi3);
			bgReaction.add(rmi3);

			rmi4.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setReaction(new Reaction.O2_2H2__2H2O());
				}
			});
			subMenu.add(rmi4);
			bgReaction.add(rmi4);

			menu.add(directionMenu);

			JMenuItem menuItem;

			String[] t = (String[]) (model.getChoices().get("Reaction Direction")).getValue("options");
			ButtonGroup bg = new ButtonGroup();
			for (int i = 0; i < t.length; i++) {
				menuItem = new JRadioButtonMenuItem();
				menuItem.setAction(model.getChoices().get("Reaction Direction"));
				menuItem.setText(t[i]);
				if (i == 0)
					menuItem.setSelected(true);
				directionMenu.add(menuItem);
				bg.add(menuItem);
			}

			menuItem = new JMenuItem("Reaction Parameters...");
			menuItem.setMnemonic(KeyEvent.VK_P);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (reactionDialog == null) {
						reactionDialog = new ReactionDialog(JOptionPane.getFrameForComponent(view));
						reactionDialog.addParameterChangeListener(model);
					}
					reactionDialog.setType(model.getType());
					reactionDialog.setTitle(model.getType().toString());
					reactionDialog.pack();
					reactionDialog.setLocationRelativeTo(ChemContainer.this);
					reactionDialog.setVisible(true);
				}
			});
			menu.add(menuItem);

			/* option menu */

			s = getInternationalText("Option");
			menu = new JMenu(s != null ? s : "Options");
			menu.setMnemonic(KeyEvent.VK_O);
			menu.addMenuListener(new MenuListener() {
				public void menuSelected(MenuEvent e) {
					disableRecorderItem.setEnabled(!model.hasGraphs());
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

			s = getInternationalText("ShowActionTip");
			menuItem = new JCheckBoxMenuItem(s != null ? s : "Show Action Tip");
			menuItem.setMnemonic(KeyEvent.VK_A);
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
			subMenu = new JMenu(s != null ? s : "Toolbox");
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
		addToolBarButton(tb.rotateObjectButton);
		addToolBarButton(tb.duplicateButton);
		addToolBarButton(tb.dropNtButton);
		addToolBarButton(tb.dropPlButton);
		addToolBarButton(tb.dropWsButton);
		addToolBarButton(tb.dropCkButton);
		addToolBarButton(tb.heatButton);
		addToolBarButton(tb.coolButton);
	}

	private class TB extends ToolBar {

		private AbstractButton dropNtButton;
		private AbstractButton dropPlButton;
		private AbstractButton dropWsButton;
		private AbstractButton dropCkButton;
		private AbstractButton dropRectangleButton;
		private AbstractButton buildBondButton;
		private AbstractButton buildBendButton;
		private AbstractButton mutateButton;

		TB() {

			super();

			dropNtButton = createButton(UserAction.getAction(UserAction.ADDA_ID, model));
			final Runnable runNt = new Runnable() {
				public void run() {
					view.editElements(Element.ID_NT).actionPerformed(null);
				}
			};
			dropNtButton.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() >= 2)
						runNt.run();
				}
			});
			customizationAction.put(dropNtButton.getAction().getValue(Action.SHORT_DESCRIPTION), runNt);
			toolBarButtonGroup.add(dropNtButton);

			dropPlButton = createButton(UserAction.getAction(UserAction.ADDB_ID, model));
			final Runnable runPl = new Runnable() {
				public void run() {
					view.editElements(Element.ID_PL).actionPerformed(null);
				}
			};
			dropPlButton.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() >= 2)
						runPl.run();
				}
			});
			customizationAction.put(dropPlButton.getAction().getValue(Action.SHORT_DESCRIPTION), runPl);
			toolBarButtonGroup.add(dropPlButton);

			dropWsButton = createButton(UserAction.getAction(UserAction.ADDC_ID, model));
			final Runnable runWs = new Runnable() {
				public void run() {
					view.editElements(Element.ID_WS).actionPerformed(null);
				}
			};
			dropWsButton.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() >= 2)
						runWs.run();
				}
			});
			customizationAction.put(dropWsButton.getAction().getValue(Action.SHORT_DESCRIPTION), runWs);
			toolBarButtonGroup.add(dropWsButton);

			dropCkButton = createButton(UserAction.getAction(UserAction.ADDD_ID, model));
			final Runnable runCk = new Runnable() {
				public void run() {
					view.editElements(Element.ID_CK).actionPerformed(null);
				}
			};
			dropCkButton.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() >= 2)
						runCk.run();
				}
			});
			customizationAction.put(dropCkButton.getAction().getValue(Action.SHORT_DESCRIPTION), runCk);
			toolBarButtonGroup.add(dropCkButton);

			dropRectangleButton = createButton(UserAction.getAction(UserAction.ADOB_ID, model));
			toolBarButtonGroup.add(dropRectangleButton);

			buildBondButton = createButton(UserAction.getAction(UserAction.BBON_ID, model));
			toolBarButtonGroup.add(buildBondButton);

			buildBendButton = createButton(UserAction.getAction(UserAction.BBEN_ID, model));
			toolBarButtonGroup.add(buildBendButton);

			mutateButton = createButton(UserAction.getAction(UserAction.MUTA_ID, model));
			toolBarButtonGroup.add(mutateButton);

			List<AbstractButton> list = new ArrayList<AbstractButton>();
			list.add(dropNtButton);
			list.add(dropPlButton);
			list.add(dropWsButton);
			list.add(dropCkButton);
			list.add(dropRectangleButton);
			String s = getInternationalText("DropObjectActions");
			actionCategory.put(s != null ? s : "Drop-Object Actions", list);

			list = new ArrayList<AbstractButton>();
			list.add(heatButton);
			list.add(coolButton);
			list.add(pcharButton);
			list.add(ncharButton);
			list.add(iresButton);
			list.add(dresButton);
			list.add(duplicateButton);
			list.add(removeObjectsButton);
			list.add(rotateObjectButton);
			list.add(changeVelocityButton);
			list.add(buildBondButton);
			list.add(buildBendButton);
			s = getInternationalText("EditingActions");
			actionCategory.put(s != null ? s : "Editing Actions", list);

		}

	}

}