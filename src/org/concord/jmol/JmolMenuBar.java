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
package org.concord.jmol;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.BitSet;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.IconPool;
import org.concord.modeler.ui.IntegerTextField;
import org.concord.modeler.ui.PastableTextField;
import org.jmol.viewer.JmolConstants;

import static org.concord.jmol.JmolContainer.getInternationalText;

/**
 * @author Charles Xie
 * 
 */
class JmolMenuBar extends JMenuBar {

	private JmolContainer jmolContainer;

	private ButtonGroup proteinGroup;
	private JMenuItem miScreenshot;
	private JMenuItem snapshotMenuItem;
	private JMenu colorMenu1, colorMenu2;
	private JMenu atomColorMenu1, atomColorMenu2;
	private JMenu bondColorMenu1, bondColorMenu2;
	private JMenu hbondColorMenu1, hbondColorMenu2;
	private JMenu ssbondColorMenu1, ssbondColorMenu2;
	private JMenuItem atomSingleColorMenuItem;
	private JMenuItem bondColorInheritMenuItem;
	private JMenuItem hbondColorInheritMenuItem;
	private JMenuItem ssbondColorInheritMenuItem;
	private JMenu elementsComputedMenu1, residuesComputedMenu1;
	private JMenu elementsComputedMenu2, residuesComputedMenu2;

	JmolMenuBar(JmolContainer container) {
		super();
		jmolContainer = container;
		add(createFileMenu());
		add(createViewMenu());
		add(createAnimationMenu());
		add(createSelectMenu(false));
		add(createSchemeMenu());
		add(createColorMenu(false));
		add(createOptionMenu());
	}

	void setScreenshotAction(Action a) {
		miScreenshot.setAction(a);
		String s = getInternationalText("Screenshot");
		miScreenshot.setText((s != null ? s : "Save Screenshot of Model") + "...");
	}

	void setSnapshotListener(ActionListener a) {
		snapshotMenuItem.addActionListener(a);
	}

	void updateMenusAfterLoading() {
		updateComputedMenus(false);
		ModelerUtilities.setWithoutNotifyingListeners(atomSingleColorMenuItem, false);
		ModelerUtilities.setWithoutNotifyingListeners(bondColorInheritMenuItem, true);
		ModelerUtilities.setWithoutNotifyingListeners(hbondColorInheritMenuItem, true);
		ModelerUtilities.setWithoutNotifyingListeners(ssbondColorInheritMenuItem, true);
	}

	void setAtomColorSelectionMenu(JMenu menu, boolean popup) {
		if (popup) {
			atomColorMenu2.addSeparator();
			atomColorMenu2.add(menu);
		}
		else {
			atomColorMenu1.addSeparator();
			atomColorMenu1.add(menu);
		}
	}

	void setBondColorSelectionMenu(JMenu menu, boolean popup) {
		if (popup) {
			bondColorMenu2.addSeparator();
			bondColorMenu2.add(menu);
		}
		else {
			bondColorMenu1.addSeparator();
			bondColorMenu1.add(menu);
		}
	}

	void setHBondColorSelectionMenu(JMenu menu, boolean popup) {
		if (popup) {
			hbondColorMenu2.addSeparator();
			hbondColorMenu2.add(menu);
		}
		else {
			hbondColorMenu1.addSeparator();
			hbondColorMenu1.add(menu);
		}
	}

	void setSSBondColorSelectionMenu(JMenu menu, boolean popup) {
		if (popup) {
			ssbondColorMenu2.addSeparator();
			ssbondColorMenu2.add(menu);
		}
		else {
			ssbondColorMenu1.addSeparator();
			ssbondColorMenu1.add(menu);
		}
	}

	void setBackgroundMenu(JMenu menu, boolean popup) {
		String s = getInternationalText("Background");
		if (s != null)
			menu.setText(s);
		if (popup) {
			colorMenu2.add(menu);
		}
		else {
			colorMenu1.add(menu);
		}
	}

	void setAtomColorMenu(byte b) {
		if (atomColorMenu1 != null) {
			if (b != JmolContainer.COLOR_ATOM_CUSTOM) {
				JMenuItem mi = atomColorMenu1.getItem(b);
				if (mi != null)
					ModelerUtilities.setWithoutNotifyingListeners(mi, true);
			}
			else {
				if (atomSingleColorMenuItem != null)
					ModelerUtilities.setWithoutNotifyingListeners(atomSingleColorMenuItem, false);
			}
		}
		if (atomColorMenu2 != null) {
			if (b != JmolContainer.COLOR_ATOM_CUSTOM) {
				JMenuItem mi = atomColorMenu2.getItem(b);
				if (mi != null)
					ModelerUtilities.setWithoutNotifyingListeners(mi, true);
			}
			else {
				if (atomSingleColorMenuItem != null)
					ModelerUtilities.setWithoutNotifyingListeners(atomSingleColorMenuItem, false);
			}
		}
	}

	void updateComputedMenus(boolean popup) {
		updateElementsComputedMenu(jmolContainer.jmol.viewer.getElementsPresentBitSet(), popup);
		if (jmolContainer.modelSetInfo != null) {
			int n = (jmolContainer.modelIndex < 0 ? jmolContainer.modelCount : jmolContainer.modelIndex);
			String[] lists = ((String[]) jmolContainer.modelSetInfo.get("group3Lists"));
			String group3List = (lists == null ? null : lists[n]);
			int[] group3Counts = (lists == null ? null : ((int[][]) jmolContainer.modelSetInfo.get("group3Counts"))[n]);
			updateResiduesComputedMenu(group3List, group3Counts, popup);
		}
	}

	private void updateElementsComputedMenu(BitSet elementsPresentBitSet, boolean popup) {
		if (elementsPresentBitSet == null)
			return;
		JMenu menu = popup ? elementsComputedMenu2 : elementsComputedMenu1;
		if (menu == null)
			return;
		menu.removeAll();
		ButtonGroup bg = new ButtonGroup();
		String elementName, elementSymbol, entryName;
		for (int i = 0, n = JmolConstants.elementNames.length; i < n; ++i) {
			if (elementsPresentBitSet.get(i)) {
				elementName = JmolConstants.elementNames[i];
				elementSymbol = JmolConstants.elementSymbols[i];
				entryName = elementSymbol + " - " + elementName;
				final String script = "select " + elementName;
				JMenuItem mi = new JRadioButtonMenuItem(entryName);
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						jmolContainer.jmol.viewer.runScriptImmediatelyWithoutThread(script);
					}
				});
				menu.add(mi);
				bg.add(mi);
			}
		}
	}

	private void updateResiduesComputedMenu(String group3List, int[] group3Counts, boolean popup) {
		if (group3List == null || group3Counts == null)
			return;
		JMenu menu = popup ? residuesComputedMenu2 : residuesComputedMenu1;
		if (menu == null)
			return;
		for (int i = 0; i < menu.getItemCount(); i++) {
			proteinGroup.remove(menu.getItem(i));
		}
		menu.removeAll();
		for (int i = 1, n = JmolConstants.GROUPID_AMINO_MAX; i < n; ++i) {
			final String residueName = JmolConstants.predefinedGroup3Names[i];
			if (group3List.indexOf("p>" + residueName) != -1) {
				int m = group3Counts[group3List.indexOf(residueName) / 6];
				JMenuItem mi = new JRadioButtonMenuItem(residueName + " (" + m + ")");
				mi.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent e) {
						if (e.getStateChange() == ItemEvent.SELECTED) {
							jmolContainer.jmol.viewer.runScriptImmediatelyWithoutThread("select " + residueName);
						}
					}
				});
				menu.add(mi);
				proteinGroup.add(mi);
			}
		}
	}

	void selectBondColorInheritMode(boolean b) {
		bondColorInheritMenuItem.setSelected(b);
	}

	void selectHBondColorInheritMode(boolean b) {
		hbondColorInheritMenuItem.setSelected(b);
	}

	void selectSSBondColorInheritMode(boolean b) {
		ssbondColorInheritMenuItem.setSelected(b);
	}

	void selectAtomSingleColorMode(boolean b) {
		atomSingleColorMenuItem.setSelected(b);
	}

	private JMenu createFileMenu() {

		String s = getInternationalText("File");
		JMenu menu = new JMenu(s != null ? s : "File");

		s = getInternationalText("OpenLocation");
		JMenuItem menuItem = new JMenuItem((s != null ? s : "Open Model From Location") + "...", IconPool
				.getIcon("openweb"));
		menuItem.setMnemonic(KeyEvent.VK_L);
		menuItem.setAccelerator(System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(KeyEvent.VK_L,
				KeyEvent.META_MASK) : KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_MASK));
		menuItem.setToolTipText("Open a structure on the Internet");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PastableTextField tf = new PastableTextField("http://");
				if (JOptionPane.showConfirmDialog(jmolContainer, tf, "Please input a URL:",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
					String s = tf.getText();
					if (s != null && !s.trim().equals(""))
						jmolContainer.runScript("load " + s);
				}
			}
		});
		menu.add(menuItem);

		menuItem = new JMenuItem(jmolContainer.opener);
		s = getInternationalText("OpenModel");
		menuItem.setText((s != null ? s : menuItem.getText()) + "...");
		menu.add(menuItem);
		menu.addSeparator();

		miScreenshot = new JMenuItem("Save Screenshot of Model");
		menu.add(miScreenshot);

		s = getInternationalText("Print");
		menuItem = new JMenuItem((s != null ? s : "Print Model") + "...");
		menuItem.setIcon(IconPool.getIcon("printer"));
		menuItem.setMnemonic(KeyEvent.VK_P);
		menuItem.setAccelerator(System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(KeyEvent.VK_P,
				KeyEvent.META_MASK) : KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_MASK));
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.jmol.print();
			}
		});
		menu.add(menuItem);

		s = getInternationalText("TakeSnapshot");
		snapshotMenuItem = new JMenuItem((s != null ? s : "Take a Snapshot") + "...");
		snapshotMenuItem.setIcon(IconPool.getIcon("camera"));
		menu.add(snapshotMenuItem);

		return menu;

	}

	private JMenu createAnimationMenu() {

		String s = getInternationalText("Animation");
		JMenu menu = new JMenu(s != null ? s : "Animation");

		s = getInternationalText("NextScene");
		final JMenuItem miNext = new JMenuItem(s != null ? s : "Next Scene");
		miNext.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.moveOneStep(true);
			}
		});
		menu.add(miNext);

		s = getInternationalText("PreviousScene");
		final JMenuItem miPrev = new JMenuItem(s != null ? s : "Previous Scene");
		miPrev.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.moveOneStep(false);
			}
		});
		menu.add(miPrev);

		s = getInternationalText("FirstScene");
		final JMenuItem miFirst = new JMenuItem(s != null ? s : "First Scene");
		miFirst.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!jmolContainer.scenes.isEmpty())
					jmolContainer.moveToScene(0, false);
			}
		});
		menu.add(miFirst);

		s = getInternationalText("LastScene");
		final JMenuItem miLast = new JMenuItem(s != null ? s : "Last Scene");
		miLast.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!jmolContainer.scenes.isEmpty())
					jmolContainer.moveToScene(jmolContainer.scenes.size() - 1, false);
			}
		});
		menu.add(miLast);
		menu.addSeparator();

		s = getInternationalText("NonstopTour");
		final JMenuItem miNonstop = new JMenuItem(s != null ? s : "Nonstop Tour");
		miNonstop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.moveNonstop();
			}
		});
		menu.add(miNonstop);

		s = getInternationalText("RequestStop");
		final JMenuItem miStop = new JMenuItem(s != null ? s : "Request Stop At Next Scene");
		miStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.requestStopMoveTo();
			}
		});
		menu.add(miStop);
		menu.addSeparator();

		s = getInternationalText("SaveScene");
		final JMenuItem miSaveScene = new JMenuItem(s != null ? s : "Save Scene");
		miSaveScene.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.saveScene();
				jmolContainer.notifyChange();
			}
		});
		menu.add(miSaveScene);

		s = getInternationalText("SaveAsNewScene");
		JMenuItem mi = new JMenuItem(s != null ? s : "Save As New Scene");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.saveAsNewScene();
				jmolContainer.notifyChange();
			}
		});
		menu.add(mi);

		s = getInternationalText("ClearScenes");
		final JMenuItem miClear = new JMenuItem(s != null ? s : "Clear Scenes");
		miClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(jmolContainer),
						"Are you sure you want to remove all scenes?", "Clear", JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
					jmolContainer.scenes.clear();
					jmolContainer.notifyChange();
					jmolContainer.notifyNavigationListeners(new NavigationEvent(jmolContainer, NavigationEvent.ARRIVAL,
							-1, -1, 0, null, null));
				}
			}
		});
		menu.add(miClear);
		menu.addSeparator();

		s = getInternationalText("EditTitle");
		mi = new JMenuItem((s != null ? s : "Edit Title") + "...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.editInformation();
			}
		});
		menu.add(mi);

		s = getInternationalText("EditItinerary");
		final JMenuItem miEdit = new JMenuItem((s != null ? s : "Edit Itinerary") + "...");
		miEdit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.editItinerary();
			}
		});
		menu.add(miEdit);

		menu.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				int n = jmolContainer.scenes.size();
				if (n == 0) {
					miClear.setEnabled(false);
					miNext.setEnabled(false);
					miPrev.setEnabled(false);
					miLast.setEnabled(false);
					miFirst.setEnabled(false);
					miNonstop.setEnabled(false);
					miEdit.setEnabled(false);
					miStop.setEnabled(false);
				}
				else {
					miClear.setEnabled(true);
					int index = jmolContainer.scenes.indexOf(jmolContainer.getCurrentScene());
					miNext.setEnabled(index < n - 1);
					miLast.setEnabled(index < n - 1);
					miNonstop.setEnabled(index < n - 1);
					miPrev.setEnabled(index > 0);
					miFirst.setEnabled(index > 0);
					miEdit.setEnabled(true);
					miStop.setEnabled(true);
				}
				miSaveScene.setEnabled(n != 0 && jmolContainer.getCurrentScene() != null);
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}
		});

		return menu;

	}

	private JMenu createViewMenu() {

		String s = getInternationalText("View");
		JMenu menu = new JMenu(s != null ? s : "View");

		s = getInternationalText("PerspectiveDepth");
		final JMenuItem miPerspectiveDepth = new JCheckBoxMenuItem(s != null ? s : "Perspective Depth");
		miPerspectiveDepth.setSelected(true);
		miPerspectiveDepth.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				jmolContainer.jmol.setPerspectiveDepth(e.getStateChange() == ItemEvent.SELECTED);
				jmolContainer.notifyChange();
			}
		});

		s = getInternationalText("NavigationMode");
		final JMenuItem miNav = new JCheckBoxMenuItem(s != null ? s : "Navigation Mode");
		miNav.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean b = e.getStateChange() == ItemEvent.SELECTED;
				if (b && !jmolContainer.jmol.viewer.getPerspectiveDepth()) {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(jmolContainer),
							"Perspective depth is required for the navigation mode. It will be turned on.");
					miPerspectiveDepth.setSelected(true);
				}
				jmolContainer.changeNavigationMode(b);
				jmolContainer.notifyChange();
			}
		});
		menu.add(miNav);

		s = getInternationalText("NavigationOptions");
		JMenu subMenu = new JMenu(s != null ? s : "Navigation Options");
		menu.add(subMenu);

		s = getInternationalText("AutonomousMode");
		final JMenuItem miRover = new JCheckBoxMenuItem(s != null ? s : "Autonomous Mode");
		miRover.setEnabled(jmolContainer.getNavigationMode());
		miRover.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.setRoverMode(miRover.isSelected());
			}
		});
		subMenu.add(miRover);

		s = getInternationalText("CollisionDetectionForAllAtoms");
		final JMenuItem miPauli = new JCheckBoxMenuItem(s != null ? s : "Collision Detection for All Atoms");
		miPauli.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				jmolContainer.setCollisionDetectionForAllAtoms(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		subMenu.add(miPauli);

		s = getInternationalText("BlinkInteractionCenters");
		final JMenuItem miBlink = new JCheckBoxMenuItem(s != null ? s : "Blink Interaction Centers");
		miBlink.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				jmolContainer.setBlinkInteractionCenters(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		subMenu.add(miBlink);

		s = getInternationalText("RoverSettings");
		final JMenuItem miRoverSettings = new JMenuItem(s != null ? s : "Rover Settings");
		miRoverSettings.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.editRover();
			}
		});
		subMenu.add(miRoverSettings);

		subMenu.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				miRover.setEnabled(jmolContainer.getNavigationMode());
				miRoverSettings.setEnabled(jmolContainer.isRoverMode());
				ModelerUtilities.setWithoutNotifyingListeners(miRover, jmolContainer.isRoverMode());
				ModelerUtilities.setWithoutNotifyingListeners(miPauli, jmolContainer.getPauliRepulsionForAllAtoms());
				ModelerUtilities.setWithoutNotifyingListeners(miBlink, jmolContainer.getBlinkInteractionCenters());
			}
		});

		menu.add(miPerspectiveDepth);

		s = getInternationalText("DepthCueing");
		final JMenuItem miDepthCueing = new JCheckBoxMenuItem(s != null ? s : "Depth Cueing");
		miDepthCueing.setEnabled(false);
		miDepthCueing.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				jmolContainer.jmol.viewer.setDepthCueing(e.getStateChange() == ItemEvent.SELECTED);
				jmolContainer.jmol.repaint();
				jmolContainer.notifyChange();
			}
		});
		menu.add(miDepthCueing);
		menu.addSeparator();

		s = getInternationalText("DetachCameraFromAtom");
		final JMenuItem miDetach = new JMenuItem(s != null ? s : "Detach Camera from Atom");
		miDetach.setEnabled(false);
		miDetach.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.jmol.setCameraAtom(-1);
				jmolContainer.notifyChange();
			}
		});
		menu.add(miDetach);

		s = getInternationalText("ZDepthMagnification");
		final JMenu zDepthMenu = new JMenu(s != null ? s : "Z-Depth Magnification");
		menu.add(zDepthMenu);
		menu.addSeparator();

		ButtonGroup bg = new ButtonGroup();

		final JMenuItem miOne = new JRadioButtonMenuItem("1x");
		miOne.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					jmolContainer.jmol.viewer.setZDepthMagnification(1);
					jmolContainer.jmol.repaint();
					jmolContainer.notifyChange();
				}
			}
		});
		zDepthMenu.add(miOne);
		bg.add(miOne);

		final JMenuItem miFive = new JRadioButtonMenuItem("5x");
		miFive.setSelected(true);
		miFive.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					jmolContainer.jmol.viewer.setZDepthMagnification(5);
					jmolContainer.jmol.repaint();
					jmolContainer.notifyChange();
				}
			}
		});
		zDepthMenu.add(miFive);
		bg.add(miFive);

		final JMenuItem miTen = new JRadioButtonMenuItem("10x");
		miTen.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					jmolContainer.jmol.viewer.setZDepthMagnification(10);
					jmolContainer.jmol.repaint();
					jmolContainer.notifyChange();
				}
			}
		});
		zDepthMenu.add(miTen);
		bg.add(miTen);

		s = getInternationalText("ClearAnnotations");
		final JMenuItem miClearAnnotations = new JMenuItem(s != null ? s : "Clear Annotations");
		miClearAnnotations.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (jmolContainer.atomAnnotations.isEmpty() && jmolContainer.bondAnnotations.isEmpty())
					return;
				String s2 = getInternationalText("ClearAnnotations");
				if (JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(jmolContainer),
						"Are you sure you want to remove all annotations?", s2 != null ? s2 : "Clear Annotations",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
					jmolContainer.clearAnnotations();
					jmolContainer.notifyChange();
				}
			}
		});
		menu.add(miClearAnnotations);

		s = getInternationalText("ClearInteractions");
		final JMenuItem miClearInteractions = new JMenuItem(s != null ? s : "Clear Interactions");
		miClearInteractions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (jmolContainer.atomInteractions.isEmpty() && jmolContainer.bondInteractions.isEmpty())
					return;
				String s2 = getInternationalText("ClearInteractions");
				if (JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(jmolContainer),
						"Are you sure you want to remove all interactions?", s2 != null ? s2 : "Clear Interactions",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
					jmolContainer.clearInteractions();
					jmolContainer.notifyChange();
				}
			}
		});
		menu.add(miClearInteractions);

		s = getInternationalText("ClearMeasurements");
		JMenuItem mi = new JMenuItem(s != null ? s : "Clear Measurements");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.jmol.viewer.clearMeasurements();
			}
		});
		menu.add(mi);

		s = getInternationalText("ResizeContainer");
		mi = new JMenuItem((s != null ? s : "Resize Container") + "...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createSizeDialog().setVisible(true);
			}
		});
		menu.add(mi);
		menu.addSeparator();

		s = getInternationalText("ViewAngle");
		subMenu = new JMenu(s != null ? s : "View Angle");
		menu.add(subMenu);

		s = getInternationalText("FrontView");
		mi = new JMenuItem(s != null ? s : "Front");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.jmol.viewer.frontView(1.5f * jmolContainer.jmol.viewer.getRotationRadius());
			}
		});
		subMenu.add(mi);

		s = getInternationalText("BackView");
		mi = new JMenuItem(s != null ? s : "Back");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.jmol.viewer.backView(1.5f * jmolContainer.jmol.viewer.getRotationRadius());
			}
		});
		subMenu.add(mi);

		s = getInternationalText("TopView");
		mi = new JMenuItem(s != null ? s : "Top");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.jmol.viewer.topView(1.5f * jmolContainer.jmol.viewer.getRotationRadius());
			}
		});
		subMenu.add(mi);

		s = getInternationalText("BottomView");
		mi = new JMenuItem(s != null ? s : "Bottom");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.jmol.viewer.bottomView(1.5f * jmolContainer.jmol.viewer.getRotationRadius());
			}
		});
		subMenu.add(mi);

		s = getInternationalText("LeftView");
		mi = new JMenuItem(s != null ? s : "Left");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.jmol.viewer.leftView(1.5f * jmolContainer.jmol.viewer.getRotationRadius());
			}
		});
		subMenu.add(mi);

		s = getInternationalText("RightView");
		mi = new JMenuItem(s != null ? s : "Right");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.jmol.viewer.rightView(1.5f * jmolContainer.jmol.viewer.getRotationRadius());
			}
		});
		subMenu.add(mi);

		menu.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				boolean b = jmolContainer.jmol.viewer.getNavigationMode();
				ModelerUtilities.setWithoutNotifyingListeners(miNav, b);
				if (b) {
					switch (jmolContainer.jmol.viewer.getZDepthMagnification()) {
					case 1:
						ModelerUtilities.setWithoutNotifyingListeners(miOne, true);
						break;
					case 5:
						ModelerUtilities.setWithoutNotifyingListeners(miFive, true);
						break;
					case 10:
						ModelerUtilities.setWithoutNotifyingListeners(miTen, true);
						break;
					}
				}
				zDepthMenu.setEnabled(b);
				miPerspectiveDepth.setEnabled(!b);
				miDetach.setEnabled(jmolContainer.getCameraAtom() != -1);
				miClearAnnotations.setEnabled(!jmolContainer.atomAnnotations.isEmpty()
						|| !jmolContainer.bondAnnotations.isEmpty());
				miClearInteractions.setEnabled(!jmolContainer.atomInteractions.isEmpty()
						|| !jmolContainer.bondInteractions.isEmpty());
				ModelerUtilities
						.setWithoutNotifyingListeners(miDepthCueing, jmolContainer.jmol.viewer.getDepthCueing());
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}
		});

		return menu;

	}

	private JMenu createOptionMenu() {

		String s = getInternationalText("Option");
		JMenu menu = new JMenu(s != null ? s : "Option");

		s = getInternationalText("ShowAnimationControls");
		final JMenuItem miAnimationPanel = new JCheckBoxMenuItem(s != null ? s : "Show Animation Controls");
		miAnimationPanel.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				jmolContainer.showAnimationPanel(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		menu.add(miAnimationPanel);
		menu.addSeparator();

		final JMenuItem miHover = new JCheckBoxMenuItem(jmolContainer.jmol.getActionMap().get("hover"));
		s = getInternationalText("EnableHoverText");
		if (s != null)
			miHover.setText(s);
		menu.add(miHover);

		final JMenuItem miAxes = new JCheckBoxMenuItem(jmolContainer.jmol.getActionMap().get("axes"));
		s = getInternationalText("ShowAxes");
		if (s != null)
			miAxes.setText(s);
		menu.add(miAxes);

		final JMenuItem miBoundBox = new JCheckBoxMenuItem(jmolContainer.jmol.getActionMap().get("bound box"));
		s = getInternationalText("ShowBoundBox");
		if (s != null)
			miBoundBox.setText(s);
		menu.add(miBoundBox);

		final JMenuItem miShowHBonds = new JCheckBoxMenuItem(jmolContainer.jmol.getActionMap().get("show hbonds"));
		s = getInternationalText("ShowHBonds");
		if (s != null)
			miShowHBonds.setText(s);
		menu.add(miShowHBonds);

		final JMenuItem miShowSSBonds = new JCheckBoxMenuItem(jmolContainer.jmol.getActionMap().get("show ssbonds"));
		s = getInternationalText("ShowSSBonds");
		if (s != null)
			miShowSSBonds.setText(s);
		menu.add(miShowSSBonds);
		menu.addSeparator();

		s = getInternationalText("AxisPosition");
		JMenu subMenu = new JMenu(s != null ? s : "Axis Position");
		menu.add(subMenu);

		ButtonGroup bg = new ButtonGroup();
		s = getInternationalText("Center");
		final JMenuItem miCenter = new JRadioButtonMenuItem(s != null ? s : "Center");
		miCenter.setSelected(true);
		miCenter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.jmol.viewer.setAxisStyle((byte) 0);
				jmolContainer.jmol.repaint();
				jmolContainer.notifyChange();
			}
		});
		subMenu.add(miCenter);
		bg.add(miCenter);

		s = getInternationalText("Corner");
		final JMenuItem miCorner = new JRadioButtonMenuItem(s != null ? s : "Corner");
		miCorner.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.jmol.viewer.setAxisStyle((byte) 1);
				jmolContainer.jmol.repaint();
				jmolContainer.notifyChange();
			}
		});
		subMenu.add(miCorner);
		bg.add(miCorner);

		menu.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				ModelerUtilities.setWithoutNotifyingListeners(miAnimationPanel, jmolContainer.hasAnimationPanel());
				ModelerUtilities.setWithoutNotifyingListeners(miShowHBonds, jmolContainer.areHydrogenBondsShown());
				ModelerUtilities.setWithoutNotifyingListeners(miShowSSBonds, jmolContainer.areSSBondsShown());
				ModelerUtilities.setWithoutNotifyingListeners(miHover, jmolContainer.isHoverEnabled());
				ModelerUtilities.setWithoutNotifyingListeners(miAxes, jmolContainer.areAxesShown());
				ModelerUtilities.setWithoutNotifyingListeners(miBoundBox, jmolContainer.isBoundBoxShown());
				byte axisStyle = jmolContainer.jmol.viewer.getAxisStyle();
				ModelerUtilities.setWithoutNotifyingListeners(miCenter, axisStyle == 0);
				ModelerUtilities.setWithoutNotifyingListeners(miCorner, axisStyle == 1);
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}
		});

		return menu;

	}

	JMenu createSelectMenu(boolean popup) {

		String s = getInternationalText("Select");
		JMenu menu = new JMenu(s != null ? s : "Select");

		s = getInternationalText("All");
		JMenuItem menuItem = new JMenuItem(s != null ? s : "All");
		menuItem.setAccelerator(System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(KeyEvent.VK_A,
				KeyEvent.META_MASK) : KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_MASK));
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.setAtomSelection(JmolContainer.SELECT_ALL);
			}
		});
		menu.add(menuItem);

		s = getInternationalText("ClearSelection");
		menuItem = new JMenuItem(s != null ? s : "Clear Selection");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.setAtomSelection((byte) -1);
			}
		});
		menu.add(menuItem);

		s = getInternationalText("Inverse");
		menuItem = new JMenuItem(s != null ? s : "Inverse");
		menuItem.setAccelerator(System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(KeyEvent.VK_I,
				KeyEvent.SHIFT_MASK | KeyEvent.META_MASK) : KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.SHIFT_MASK
				| KeyEvent.CTRL_MASK));
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.setAtomSelection(JmolContainer.SELECT_NOT_SELECTED);
			}
		});
		menu.add(menuItem);
		menu.addSeparator();

		s = getInternationalText("Element");
		if (popup) {
			elementsComputedMenu2 = new JMenu(s != null ? s : "Element");
			menu.add(elementsComputedMenu2);
		}
		else {
			elementsComputedMenu1 = new JMenu(s != null ? s : "Element");
			menu.add(elementsComputedMenu1);
		}

		// amino acids

		s = getInternationalText("AminoAcid");
		final JMenu aaMenu = new JMenu(s != null ? s : "Amino Acids");
		menu.add(aaMenu);

		s = getInternationalText("ByResidue");
		if (popup) {
			residuesComputedMenu2 = new JMenu(s != null ? s : "By Residue");
			aaMenu.add(residuesComputedMenu2);
		}
		else {
			residuesComputedMenu1 = new JMenu(s != null ? s : "By Residue");
			aaMenu.add(residuesComputedMenu1);
		}
		aaMenu.addSeparator();

		proteinGroup = new ButtonGroup();

		s = getInternationalText("All");
		final JMenuItem miAll = new JRadioButtonMenuItem(s != null ? s : "All");
		miAll.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					jmolContainer.setAtomSelection(JmolContainer.SELECT_PROTEIN);
			}
		});
		aaMenu.add(miAll);
		proteinGroup.add(miAll);

		s = getInternationalText("Backbone");
		final JMenuItem miBackbone = new JRadioButtonMenuItem(s != null ? s : "Backbone");
		miBackbone.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					jmolContainer.setAtomSelection(JmolContainer.SELECT_PROTEIN_BACKBONE);
			}
		});
		aaMenu.add(miBackbone);
		proteinGroup.add(miBackbone);

		s = getInternationalText("SideChain");
		final JMenuItem miSideChain = new JRadioButtonMenuItem(s != null ? s : "Side Chains");
		miSideChain.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					jmolContainer.setAtomSelection(JmolContainer.SELECT_PROTEIN_SIDECHAIN);
			}
		});
		aaMenu.add(miSideChain);
		proteinGroup.add(miSideChain);

		s = getInternationalText("PolarResidue");
		final JMenuItem miPolar = new JRadioButtonMenuItem(s != null ? s : "Polar Residues");
		miPolar.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					jmolContainer.setAtomSelection(JmolContainer.SELECT_PROTEIN_POLAR);
			}
		});
		aaMenu.add(miPolar);
		proteinGroup.add(miPolar);

		s = getInternationalText("NonPolarResidue");
		final JMenuItem miNonpolar = new JRadioButtonMenuItem(s != null ? s : "Non-polar Residues");
		miNonpolar.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					jmolContainer.setAtomSelection(JmolContainer.SELECT_PROTEIN_NONPOLAR);
			}
		});
		aaMenu.add(miNonpolar);
		proteinGroup.add(miNonpolar);

		s = getInternationalText("BasicResidue");
		final JMenuItem miBasic = new JRadioButtonMenuItem(s != null ? s : "Basic Residues");
		miBasic.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					jmolContainer.setAtomSelection(JmolContainer.SELECT_PROTEIN_BASIC);
			}
		});
		aaMenu.add(miBasic);
		proteinGroup.add(miBasic);

		s = getInternationalText("AcidicResidue");
		final JMenuItem miAcidic = new JRadioButtonMenuItem(s != null ? s : "Acidic Residues");
		miAcidic.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					jmolContainer.setAtomSelection(JmolContainer.SELECT_PROTEIN_ACIDIC);
			}
		});
		aaMenu.add(miAcidic);
		proteinGroup.add(miAcidic);

		s = getInternationalText("NeutralResidue");
		final JMenuItem miNeutral = new JRadioButtonMenuItem(s != null ? s : "Neutral Residues");
		miNeutral.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					jmolContainer.setAtomSelection(JmolContainer.SELECT_PROTEIN_NEUTRAL);
			}
		});
		aaMenu.add(miNeutral);
		proteinGroup.add(miNeutral);

		aaMenu.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				switch (jmolContainer.getAtomSelection()) {
				case JmolContainer.SELECT_PROTEIN:
					ModelerUtilities.setWithoutNotifyingListeners(miAll, true);
					break;
				case JmolContainer.SELECT_PROTEIN_BACKBONE:
					ModelerUtilities.setWithoutNotifyingListeners(miBackbone, true);
					break;
				case JmolContainer.SELECT_PROTEIN_SIDECHAIN:
					ModelerUtilities.setWithoutNotifyingListeners(miSideChain, true);
					break;
				case JmolContainer.SELECT_PROTEIN_POLAR:
					ModelerUtilities.setWithoutNotifyingListeners(miPolar, true);
					break;
				case JmolContainer.SELECT_PROTEIN_NONPOLAR:
					ModelerUtilities.setWithoutNotifyingListeners(miNonpolar, true);
					break;
				case JmolContainer.SELECT_PROTEIN_BASIC:
					ModelerUtilities.setWithoutNotifyingListeners(miBasic, true);
					break;
				case JmolContainer.SELECT_PROTEIN_ACIDIC:
					ModelerUtilities.setWithoutNotifyingListeners(miAcidic, true);
					break;
				case JmolContainer.SELECT_PROTEIN_NEUTRAL:
					ModelerUtilities.setWithoutNotifyingListeners(miNeutral, true);
					break;
				}
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}
		});

		// nucleic acids

		s = getInternationalText("NucleicAcid");
		final JMenu naMenu = new JMenu(s != null ? s : "Nucleic Acids");
		menu.add(naMenu);

		ButtonGroup bg = new ButtonGroup();

		s = getInternationalText("All");
		final JMenuItem miNucleicAll = new JRadioButtonMenuItem(s != null ? s : "All");
		miNucleicAll.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					jmolContainer.setAtomSelection(JmolContainer.SELECT_NUCLEIC_ALL);
			}
		});
		naMenu.add(miNucleicAll);
		bg.add(miNucleicAll);

		s = getInternationalText("Backbone");
		final JMenuItem miNucleicBackbone = new JRadioButtonMenuItem(s != null ? s : "Backbone");
		miNucleicBackbone.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					jmolContainer.setAtomSelection(JmolContainer.SELECT_NUCLEIC_BACKBONE);
			}
		});
		naMenu.add(miNucleicBackbone);
		bg.add(miNucleicBackbone);

		s = getInternationalText("Base");
		final JMenuItem miNucleicBase = new JRadioButtonMenuItem(s != null ? s : "Bases");
		miNucleicBase.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					jmolContainer.setAtomSelection(JmolContainer.SELECT_NUCLEIC_BASE);
			}
		});
		naMenu.add(miNucleicBase);
		bg.add(miNucleicBase);
		naMenu.addSeparator();

		s = getInternationalText("Adenine");
		final JMenuItem miAdenine = new JRadioButtonMenuItem(s != null ? s : "A - Adenine");
		miAdenine.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					jmolContainer.setAtomSelection(JmolContainer.SELECT_NUCLEIC_ADENINE);
			}
		});
		naMenu.add(miAdenine);
		bg.add(miAdenine);

		s = getInternationalText("Cytosine");
		final JMenuItem miCytosine = new JRadioButtonMenuItem(s != null ? s : "C - Cytosine");
		miCytosine.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					jmolContainer.setAtomSelection(JmolContainer.SELECT_NUCLEIC_CYTOSINE);
			}
		});
		naMenu.add(miCytosine);
		bg.add(miCytosine);

		s = getInternationalText("Guanine");
		final JMenuItem miGuanine = new JRadioButtonMenuItem(s != null ? s : "G - Guanine");
		miGuanine.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					jmolContainer.setAtomSelection(JmolContainer.SELECT_NUCLEIC_GUANINE);
			}
		});
		naMenu.add(miGuanine);
		bg.add(miGuanine);

		s = getInternationalText("Thymine");
		final JMenuItem miThymine = new JRadioButtonMenuItem(s != null ? s : "T - Thymine");
		miThymine.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					jmolContainer.setAtomSelection(JmolContainer.SELECT_NUCLEIC_THYMINE);
			}
		});
		naMenu.add(miThymine);
		bg.add(miThymine);

		s = getInternationalText("Uracil");
		final JMenuItem miUracil = new JRadioButtonMenuItem(s != null ? s : "U - Uracil");
		miUracil.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					jmolContainer.setAtomSelection(JmolContainer.SELECT_NUCLEIC_URACIL);
			}
		});
		naMenu.add(miUracil);
		bg.add(miUracil);
		naMenu.addSeparator();

		s = getInternationalText("ATPair");
		final JMenuItem miAT = new JRadioButtonMenuItem(s != null ? s : "A-T Pairs");
		miAT.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					jmolContainer.setAtomSelection(JmolContainer.SELECT_NUCLEIC_AT);
			}
		});
		naMenu.add(miAT);
		bg.add(miAT);

		s = getInternationalText("CGPair");
		final JMenuItem miCG = new JRadioButtonMenuItem(s != null ? s : "C-G Pairs");
		miCG.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					jmolContainer.setAtomSelection(JmolContainer.SELECT_NUCLEIC_CG);
			}
		});
		naMenu.add(miCG);
		bg.add(miCG);

		s = getInternationalText("AUPair");
		final JMenuItem miAU = new JRadioButtonMenuItem(s != null ? s : "A-U Pairs");
		miAU.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					jmolContainer.setAtomSelection(JmolContainer.SELECT_NUCLEIC_AU);
			}
		});
		naMenu.add(miAU);
		bg.add(miAU);

		naMenu.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				switch (jmolContainer.getAtomSelection()) {
				case JmolContainer.SELECT_NUCLEIC_ALL:
					ModelerUtilities.setWithoutNotifyingListeners(miNucleicAll, true);
					break;
				case JmolContainer.SELECT_NUCLEIC_BACKBONE:
					ModelerUtilities.setWithoutNotifyingListeners(miNucleicBackbone, true);
					break;
				case JmolContainer.SELECT_NUCLEIC_BASE:
					ModelerUtilities.setWithoutNotifyingListeners(miNucleicBase, true);
					break;
				case JmolContainer.SELECT_NUCLEIC_ADENINE:
					ModelerUtilities.setWithoutNotifyingListeners(miAdenine, true);
					break;
				case JmolContainer.SELECT_NUCLEIC_CYTOSINE:
					ModelerUtilities.setWithoutNotifyingListeners(miCytosine, true);
					break;
				case JmolContainer.SELECT_NUCLEIC_GUANINE:
					ModelerUtilities.setWithoutNotifyingListeners(miGuanine, true);
					break;
				case JmolContainer.SELECT_NUCLEIC_THYMINE:
					ModelerUtilities.setWithoutNotifyingListeners(miThymine, true);
					break;
				case JmolContainer.SELECT_NUCLEIC_URACIL:
					ModelerUtilities.setWithoutNotifyingListeners(miUracil, true);
					break;
				case JmolContainer.SELECT_NUCLEIC_AT:
					ModelerUtilities.setWithoutNotifyingListeners(miAT, true);
					break;
				case JmolContainer.SELECT_NUCLEIC_CG:
					ModelerUtilities.setWithoutNotifyingListeners(miCG, true);
					break;
				case JmolContainer.SELECT_NUCLEIC_AU:
					ModelerUtilities.setWithoutNotifyingListeners(miAU, true);
					break;
				}
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}
		});

		return menu;

	}

	JMenu createSchemeMenu() {

		String s = getInternationalText("Style");
		JMenu menu = new JMenu(s != null ? s : "Style");

		ButtonGroup bg = new ButtonGroup();

		s = getInternationalText("BallAndStick");
		final JMenuItem miBS = new JRadioButtonMenuItem(s != null ? s : JmolContainer.BALL_AND_STICK);
		miBS.setSelected(true);
		miBS.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.setScheme(JmolContainer.BALL_AND_STICK);
				jmolContainer.notifyChange();
			}
		});
		menu.add(miBS);
		bg.add(miBS);

		s = getInternationalText("SpaceFilling");
		final JMenuItem miCPK = new JRadioButtonMenuItem(s != null ? s : JmolContainer.SPACE_FILLING);
		miCPK.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.setScheme(JmolContainer.SPACE_FILLING);
				jmolContainer.notifyChange();
			}
		});
		menu.add(miCPK);
		bg.add(miCPK);

		s = getInternationalText("Stick");
		final JMenuItem miStick = new JRadioButtonMenuItem(s != null ? s : JmolContainer.STICKS);
		miStick.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.setScheme(JmolContainer.STICKS);
				jmolContainer.notifyChange();
			}
		});
		menu.add(miStick);
		bg.add(miStick);

		s = getInternationalText("Wireframe");
		final JMenuItem miWF = new JRadioButtonMenuItem(s != null ? s : JmolContainer.WIREFRAME);
		miWF.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.setScheme(JmolContainer.WIREFRAME);
				jmolContainer.notifyChange();
			}
		});
		menu.add(miWF);
		bg.add(miWF);
		menu.addSeparator();

		s = getInternationalText("Backbone");
		final JMenuItem miBackbone = new JRadioButtonMenuItem(s != null ? s : JmolContainer.BACKBONE);
		miBackbone.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.setScheme(JmolContainer.BACKBONE);
				jmolContainer.notifyChange();
			}
		});
		menu.add(miBackbone);
		bg.add(miBackbone);

		s = getInternationalText("Trace");
		final JMenuItem miTrace = new JRadioButtonMenuItem(s != null ? s : JmolContainer.TRACE);
		miTrace.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.setScheme(JmolContainer.TRACE);
				jmolContainer.notifyChange();
			}
		});
		menu.add(miTrace);
		bg.add(miTrace);

		s = getInternationalText("Strand");
		final JMenuItem miStrands = new JRadioButtonMenuItem(s != null ? s : JmolContainer.STRANDS);
		miStrands.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.setScheme(JmolContainer.STRANDS);
				jmolContainer.notifyChange();
			}
		});
		menu.add(miStrands);
		bg.add(miStrands);

		s = getInternationalText("Ribbon");
		final JMenuItem miRibbon = new JRadioButtonMenuItem(s != null ? s : JmolContainer.RIBBON);
		miRibbon.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.setScheme(JmolContainer.RIBBON);
				jmolContainer.notifyChange();
			}
		});
		menu.add(miRibbon);
		bg.add(miRibbon);

		s = getInternationalText("MeshRibbon");
		final JMenuItem miMeshRibbon = new JRadioButtonMenuItem(s != null ? s : JmolContainer.MESHRIBBON);
		miMeshRibbon.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.setScheme(JmolContainer.MESHRIBBON);
				jmolContainer.notifyChange();
			}
		});
		menu.add(miMeshRibbon);
		bg.add(miMeshRibbon);

		s = getInternationalText("Cartoon");
		final JMenuItem miCartoon = new JRadioButtonMenuItem(s != null ? s : JmolContainer.CARTOON);
		miCartoon.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.setScheme(JmolContainer.CARTOON);
				jmolContainer.notifyChange();
			}
		});
		menu.add(miCartoon);
		bg.add(miCartoon);

		s = getInternationalText("Rocket");
		final JMenuItem miRocket = new JRadioButtonMenuItem(s != null ? s : JmolContainer.ROCKET);
		miRocket.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.setScheme(JmolContainer.ROCKET);
				jmolContainer.notifyChange();
			}
		});
		menu.add(miRocket);
		bg.add(miRocket);
		menu.addSeparator();

		s = getInternationalText("Surfaces");
		JMenu subMenu = new JMenu(s != null ? s : "Surfaces");
		menu.add(subMenu);

		ButtonGroup surfaceGroup = new ButtonGroup();

		s = getInternationalText("VdwDottedSurface");
		final JMenuItem miDots = new JRadioButtonMenuItem(s != null ? s : "Van der Waals Dotted Surface");
		miDots.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean b = e.getStateChange() == ItemEvent.SELECTED;
				jmolContainer.jmol.viewer.runScriptImmediatelyWithoutThread("dots " + (b ? "ON" : "OFF"));
				jmolContainer.setDotsEnabled(b);
			}
		});
		subMenu.add(miDots);
		surfaceGroup.add(miDots);

		s = getInternationalText("VdwSurface");
		final JMenuItem miVdwSurf = new JRadioButtonMenuItem(s != null ? s : "Van der Waals Surface");
		miVdwSurf.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean b = e.getStateChange() == ItemEvent.SELECTED;
				jmolContainer.runScript(b ? "isosurface delete resolution 0 solvent 0" : "isosurface delete");
			}
		});
		subMenu.add(miVdwSurf);
		surfaceGroup.add(miVdwSurf);

		s = getInternationalText("SolventAccessibleSurface");
		final JMenuItem miSolvSurf = new JRadioButtonMenuItem(s != null ? s : "Solvent Accessible Surface");
		miSolvSurf.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean b = e.getStateChange() == ItemEvent.SELECTED;
				jmolContainer.runScript(b ? "isosurface delete resolution 0 solvent 1.4" : "isosurface delete");
			}
		});
		subMenu.add(miSolvSurf);
		surfaceGroup.add(miSolvSurf);

		s = getInternationalText("MolecularSurface");
		final JMenuItem miMolSurf = new JRadioButtonMenuItem(s != null ? s : "Molecular Surface");
		miMolSurf.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean b = e.getStateChange() == ItemEvent.SELECTED;
				jmolContainer.runScript(b ? "isosurface delete resolution 0 molecular" : "isosurface delete");
			}
		});
		subMenu.add(miMolSurf);
		surfaceGroup.add(miMolSurf);
		subMenu.addSeparator();

		final JMenuItem miOff = new JRadioButtonMenuItem();
		surfaceGroup.add(miOff);

		s = getInternationalText("SurfaceOff");
		JMenuItem mi = new JMenuItem(s != null ? s : "Turn Off");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.jmol.viewer.runScriptImmediatelyWithoutThread("isosurface delete;select *;dots off");
				jmolContainer.setDotsEnabled(false);
				miOff.doClick();
			}
		});
		subMenu.add(mi);
		subMenu.addSeparator();

		ButtonGroup bg2 = new ButtonGroup();
		s = getInternationalText("SurfaceOpaque");
		mi = new JRadioButtonMenuItem(s != null ? s : "Opaque");
		mi.setSelected(true);
		mi.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					jmolContainer.runScript("isosurface opaque");
			}
		});
		subMenu.add(mi);
		bg2.add(mi);

		s = getInternationalText("SurfaceTranslucent");
		mi = new JRadioButtonMenuItem(s != null ? s : "Translucent");
		mi.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					jmolContainer.runScript("isosurface translucent");
			}
		});
		subMenu.add(mi);
		bg2.add(mi);

		menu.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				miDots.setSelected(jmolContainer.isDotsEnabled());
				String s = jmolContainer.getScheme();
				if (s != null)
					s = s.intern();
				if (s == JmolContainer.BALL_AND_STICK)
					miBS.setSelected(true);
				else if (s == JmolContainer.SPACE_FILLING)
					miCPK.setSelected(true);
				else if (s == JmolContainer.STICKS)
					miStick.setSelected(true);
				else if (s == JmolContainer.WIREFRAME)
					miWF.setSelected(true);
				else if (s == JmolContainer.CARTOON)
					miCartoon.setSelected(true);
				else if (s == JmolContainer.RIBBON)
					miRibbon.setSelected(true);
				else if (s == JmolContainer.MESHRIBBON)
					miMeshRibbon.setSelected(true);
				else if (s == JmolContainer.ROCKET)
					miRocket.setSelected(true);
				else if (s == JmolContainer.TRACE)
					miTrace.setSelected(true);
				else if (s == JmolContainer.STRANDS)
					miStrands.setSelected(true);
				else if (s == JmolContainer.BACKBONE)
					miBackbone.setSelected(true);
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}
		});

		return menu;

	}

	JMenu createColorMenu(boolean popup) {

		JMenu menu;
		JMenu subMenu;

		if (popup) {
			colorMenu2 = new JMenu("Color");
			menu = colorMenu2;
			atomColorMenu2 = new JMenu("Atoms");
			subMenu = atomColorMenu2;
		}
		else {
			colorMenu1 = new JMenu("Color");
			menu = colorMenu1;
			atomColorMenu1 = new JMenu("Atoms");
			subMenu = atomColorMenu1;
		}

		String s = getInternationalText("Color");
		if (s != null)
			menu.setText(s);

		s = getInternationalText("Atom");
		if (s != null)
			subMenu.setText(s);

		menu.add(subMenu);

		/* color atoms */

		ButtonGroup bg = new ButtonGroup();

		s = getInternationalText("ByElement");
		JMenuItem menuItem = new JRadioButtonMenuItem(s != null ? s : "By Element");
		menuItem.setSelected(true);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.setAtomColoring(JmolContainer.COLOR_ATOM_BY_ELEMENT);
				jmolContainer.notifyChange();
			}
		});
		subMenu.add(menuItem);
		bg.add(menuItem);

		s = getInternationalText("ByAminoAcid");
		menuItem = new JRadioButtonMenuItem(s != null ? s : "By Amino Acid");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.setAtomColoring(JmolContainer.COLOR_ATOM_BY_AMINO_ACID);
				jmolContainer.notifyChange();
			}
		});
		subMenu.add(menuItem);
		bg.add(menuItem);

		s = getInternationalText("BySecondaryStructure");
		menuItem = new JRadioButtonMenuItem(s != null ? s : "By Secondary Structure");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.setAtomColoring(JmolContainer.COLOR_ATOM_BY_STRUCTURE);
				jmolContainer.notifyChange();
			}
		});
		subMenu.add(menuItem);
		bg.add(menuItem);

		s = getInternationalText("ByChain");
		menuItem = new JRadioButtonMenuItem(s != null ? s : "By Chain");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.setAtomColoring(JmolContainer.COLOR_ATOM_BY_CHAIN);
				jmolContainer.notifyChange();
			}
		});
		subMenu.add(menuItem);
		bg.add(menuItem);

		s = getInternationalText("ByFormalCharge");
		menuItem = new JRadioButtonMenuItem(s != null ? s : "By Formal Charge");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.setAtomColoring(JmolContainer.COLOR_ATOM_BY_FORMAL_CHARGE);
				jmolContainer.notifyChange();
			}
		});
		subMenu.add(menuItem);
		bg.add(menuItem);

		s = getInternationalText("ByPartialCharge");
		menuItem = new JRadioButtonMenuItem(s != null ? s : "By Partial Charge");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.setAtomColoring(JmolContainer.COLOR_ATOM_BY_PARTIAL_CHARGE);
				jmolContainer.notifyChange();
			}
		});
		subMenu.add(menuItem);
		bg.add(menuItem);

		atomSingleColorMenuItem = new JRadioButtonMenuItem();
		bg.add(atomSingleColorMenuItem);

		/* color bonds */

		if (popup) {
			bondColorMenu2 = new JMenu("Bonds");
			menu.add(bondColorMenu2);
			subMenu = bondColorMenu2;
		}
		else {
			bondColorMenu1 = new JMenu("Bonds");
			menu.add(bondColorMenu1);
			subMenu = bondColorMenu1;
		}

		s = getInternationalText("Bond");
		if (s != null)
			subMenu.setText(s);

		s = getInternationalText("BondColorInherit");
		bondColorInheritMenuItem = new JCheckBoxMenuItem(s != null ? s : "Inherit");
		bondColorInheritMenuItem.setSelected(true);
		bondColorInheritMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					jmolContainer.jmol.viewer.runScriptImmediatelyWithoutThread("color bonds none;");
			}
		});
		subMenu.add(bondColorInheritMenuItem);

		/* color hbonds */

		if (popup) {
			hbondColorMenu2 = new JMenu("Hydrogen Bonds");
			menu.add(hbondColorMenu2);
			subMenu = hbondColorMenu2;
		}
		else {
			hbondColorMenu1 = new JMenu("Hydrogen Bonds");
			menu.add(hbondColorMenu1);
			subMenu = hbondColorMenu1;
		}

		s = getInternationalText("HydrogenBond");
		if (s != null)
			subMenu.setText(s);

		s = getInternationalText("HydrogenBondColorInherit");
		hbondColorInheritMenuItem = new JCheckBoxMenuItem(s != null ? s : "Inherit");
		hbondColorInheritMenuItem.setSelected(true);
		hbondColorInheritMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					jmolContainer.jmol.viewer.runScriptImmediatelyWithoutThread("color hbonds none;");
			}
		});
		subMenu.add(hbondColorInheritMenuItem);

		/* color ssbonds */

		if (popup) {
			ssbondColorMenu2 = new JMenu("Disulfide Bonds");
			menu.add(ssbondColorMenu2);
			subMenu = ssbondColorMenu2;
		}
		else {
			ssbondColorMenu1 = new JMenu("Disulfide Bonds");
			menu.add(ssbondColorMenu1);
			subMenu = ssbondColorMenu1;
		}

		s = getInternationalText("SSBond");
		if (s != null)
			subMenu.setText(s);

		s = getInternationalText("SSBondColorInherit");
		ssbondColorInheritMenuItem = new JCheckBoxMenuItem(s != null ? s : "Inherit");
		ssbondColorInheritMenuItem.setSelected(true);
		ssbondColorInheritMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					jmolContainer.jmol.viewer.runScriptImmediatelyWithoutThread("color ssbonds none;");
			}
		});
		subMenu.add(ssbondColorInheritMenuItem);

		return menu;

	}

	private JDialog createSizeDialog() {

		String s = getInternationalText("ResizeContainer");
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(this),
				s != null ? s : "Set Container Size", true);
		dialog.setSize(200, 200);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		final IntegerTextField wField = new IntegerTextField(jmolContainer.getWidth(), 300, 1200);
		final IntegerTextField hField = new IntegerTextField(jmolContainer.getHeight(), 200, 1000);
		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.setViewerSize(new Dimension(wField.getValue(), hField.getValue()));
				dialog.dispose();
				jmolContainer.notifyChange();
			}
		};
		wField.addActionListener(okListener);
		hField.addActionListener(okListener);

		JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
		s = getInternationalText("Width");
		p.add(new JLabel((s != null ? s : "Width") + " (" + wField.getMinValue() + "-" + wField.getMaxValue()
				+ " pixels):"));
		p.add(wField);
		s = getInternationalText("Height");
		p.add(new JLabel((s != null ? s : "Height") + " (" + hField.getMinValue() + "-" + hField.getMaxValue()
				+ " pixels):"));
		p.add(hField);

		dialog.getContentPane().add(p, BorderLayout.CENTER);

		p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		s = getInternationalText("OK");
		JButton b = new JButton(s != null ? s : "OK");
		b.addActionListener(okListener);
		p.add(b);

		s = getInternationalText("Cancel");
		b = new JButton(s != null ? s : "Cancel");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});
		p.add(b);

		dialog.getContentPane().add(p, BorderLayout.SOUTH);

		dialog.pack();
		dialog.setLocationRelativeTo(this);

		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dialog.dispose();
			}

			public void windowActivated(WindowEvent e) {
				wField.selectAll();
				wField.requestFocus();
			}
		});

		return dialog;

	}

}
