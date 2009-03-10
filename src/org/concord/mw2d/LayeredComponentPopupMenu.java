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

package org.concord.mw2d;

import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.draw.DrawingElement;
import org.concord.modeler.process.Executable;
import org.concord.modeler.ui.IconPool;
import org.concord.mw2d.models.ElectricField;
import org.concord.mw2d.models.FieldArea;
import org.concord.mw2d.models.ImageComponent;
import org.concord.mw2d.models.Layered;
import org.concord.mw2d.models.LineComponent;
import org.concord.mw2d.models.MagneticField;
import org.concord.mw2d.models.TriangleComponent;

class LayeredComponentPopupMenu extends JPopupMenu {

	private JMenuItem attachMI;
	private JMenuItem attachToMI;
	private JMenuItem detachMI;
	private JMenuItem bringForwardMI;
	private JMenuItem sendBackwardMI;
	private JMenuItem bringToFrontMI;
	private JMenuItem sendToBackMI;
	private JMenuItem sendBehindMI;
	private JMenuItem bringFrontMI;
	private JMenu positionMenu;
	private JMenuItem centerMI;
	private JMenuItem northMI;
	private JMenuItem southMI;
	private JMenuItem eastMI;
	private JMenuItem westMI;
	private JMenu physicsMenu;
	private JMenuItem miEField, miBField, miLineReflect, miViscosity, miShapeReflect;
	private JMenuItem miVisible, miDraggable;

	LayeredComponentPopupMenu(final MDView view) {

		addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
				view.requestFocusInWindow();
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				if (view.selectedComponent instanceof Layered) {
					if (((Layered) view.selectedComponent).getHost() != null) {
						attachMI.setEnabled(false);
						detachMI.setEnabled(true);
					}
					else {
						attachMI.setEnabled(true);
						detachMI.setEnabled(false);
					}
					if (((Layered) view.selectedComponent).getHost() != null
							&& view.selectedComponent instanceof ImageComponent) {
						centerMI.setEnabled(false);
						northMI.setEnabled(false);
						southMI.setEnabled(false);
						eastMI.setEnabled(false);
						westMI.setEnabled(false);
					}
					else {
						centerMI.setEnabled(true);
						northMI.setEnabled(true);
						southMI.setEnabled(true);
						eastMI.setEnabled(true);
						westMI.setEnabled(true);
					}
					if (view.layerBasket.size() <= 1) {
						bringForwardMI.setEnabled(false);
						sendBackwardMI.setEnabled(false);
						bringToFrontMI.setEnabled(false);
						sendToBackMI.setEnabled(false);
					}
					else {
						int order = view.layerBasket.indexOf(view.selectedComponent);
						if (order == 0) {
							bringForwardMI.setEnabled(true);
							bringToFrontMI.setEnabled(true);
							sendBackwardMI.setEnabled(false);
							sendToBackMI.setEnabled(false);
						}
						else if (order == view.layerBasket.size() - 1) {
							bringForwardMI.setEnabled(false);
							bringToFrontMI.setEnabled(false);
							sendBackwardMI.setEnabled(true);
							sendToBackMI.setEnabled(true);
						}
						else {
							bringForwardMI.setEnabled(true);
							bringToFrontMI.setEnabled(true);
							sendBackwardMI.setEnabled(true);
							sendToBackMI.setEnabled(true);
						}
					}
					boolean b = ((Layered) view.selectedComponent).getLayer() == Layered.IN_FRONT_OF_PARTICLES;
					bringFrontMI.setEnabled(!b);
					sendBehindMI.setEnabled(b);
					b = view.selectedComponent instanceof LineComponent
							|| view.selectedComponent instanceof TriangleComponent;
					positionMenu.setEnabled(!b);
					if (b)
						attachMI.setEnabled(false);
					if (view.selectedComponent instanceof FieldArea) {
						physicsMenu.setEnabled(true);
						physicsMenu.add(miShapeReflect);
						physicsMenu.add(miViscosity);
						physicsMenu.add(miEField);
						physicsMenu.add(miBField);
						physicsMenu.remove(miLineReflect);
						FieldArea fa = (FieldArea) view.selectedComponent;
						if (fa.getVectorField() instanceof ElectricField) {
							miEField.setBackground(SystemColor.controlHighlight);
							miBField.setBackground(miLineReflect.getBackground());
						}
						else if (fa.getVectorField() instanceof MagneticField) {
							miEField.setBackground(miLineReflect.getBackground());
							miBField.setBackground(SystemColor.controlHighlight);
						}
						else {
							miEField.setBackground(physicsMenu.getBackground());
							miBField.setBackground(physicsMenu.getBackground());
						}
						miViscosity.setBackground(fa.getViscosity() > 0 ? SystemColor.controlHighlight : physicsMenu
								.getBackground());
						miShapeReflect.setBackground(fa.getReflection() ? SystemColor.controlHighlight : physicsMenu
								.getBackground());
					}
					else if (view.selectedComponent instanceof LineComponent) {
						physicsMenu.setEnabled(true);
						physicsMenu.remove(miShapeReflect);
						physicsMenu.remove(miViscosity);
						physicsMenu.remove(miEField);
						physicsMenu.remove(miBField);
						physicsMenu.add(miLineReflect);
						miLineReflect.setSelected(((LineComponent) view.selectedComponent).isReflector());
					}
					else {
						physicsMenu.setEnabled(false);
					}
					ModelerUtilities.setWithoutNotifyingListeners(miDraggable, view.selectedComponent.isDraggable());
					ModelerUtilities.setWithoutNotifyingListeners(miVisible, view.selectedComponent.isVisible());
				}
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				view.requestFocusInWindow();
			}
		});

		String s = MDView.getInternationalText("Copy");
		JMenuItem mi = new JMenuItem(s != null ? s : "Copy", UserAction.getIcon(UserAction.DUPL_ID));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				view.copySelectedComponent();
			}
		});
		add(mi);

		mi = new JMenuItem(view.getActionMap().get(MDView.CUT));
		s = MDView.getInternationalText("Cut");
		if (s != null)
			mi.setText(s);
		add(mi);

		Action a = new DefaultModelAction(view.getModel(), new Executable() {
			public void execute() {
				if (view.selectedComponent instanceof Layered) {
					view.attach((Layered) view.selectedComponent);
				}
			}
		});
		a.putValue(Action.SMALL_ICON, UserAction.getIcon(UserAction.IRES_ID));
		a.putValue(Action.NAME, "Attach");
		a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A);
		attachMI = new JMenuItem(a);
		s = MDView.getInternationalText("Attach");
		if (s != null)
			attachMI.setText(s);
		add(attachMI);

		a = new DefaultModelAction(view.getModel(), new Executable() {
			public void execute() {
				if (view.selectedComponent instanceof Layered) {
					view.attachTo((Layered) view.selectedComponent);
				}
			}
		});
		a.putValue(Action.SMALL_ICON, new ImageIcon(MDView.class.getResource("images/editor/AttachTo.gif")));
		a.putValue(Action.NAME, "Attach to");
		a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_T);
		attachToMI = new JMenuItem(a);
		s = MDView.getInternationalText("AttachTo");
		if (s != null)
			attachToMI.setText(s);
		add(attachToMI);

		a = new DefaultModelAction(view.getModel(), new Executable() {
			public void execute() {
				if (view.selectedComponent instanceof Layered) {
					view.detach((Layered) view.selectedComponent);
				}
			}
		});
		a.putValue(Action.SMALL_ICON, UserAction.getIcon(UserAction.DRES_ID));
		a.putValue(Action.NAME, "Detach");
		a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_D);
		detachMI = new JMenuItem(a);
		s = MDView.getInternationalText("Detach");
		if (s != null)
			detachMI.setText(s);
		add(detachMI);

		// fields

		s = MDView.getInternationalText("Physics");
		physicsMenu = new JMenu(s != null ? s : "Physics");
		physicsMenu.setIcon(new ImageIcon(getClass().getResource("images/leftField.gif")));
		add(physicsMenu);
		addSeparator();

		miShapeReflect = new JMenuItem("Reflection");
		s = MDView.getInternationalText("Reflection");
		if (s != null)
			miShapeReflect.setText(s);
		miShapeReflect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof FieldArea)
					new FieldAreaShapeReflectionAction((FieldArea) view.selectedComponent).createDialog(view)
							.setVisible(true);
			}
		});
		physicsMenu.add(miShapeReflect);

		miViscosity = new JMenuItem("Viscosity");
		s = MDView.getInternationalText("MediumViscosityLabel");
		if (s != null)
			miViscosity.setText(s);
		miViscosity.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof FieldArea)
					new FieldAreaViscosityAction((FieldArea) view.selectedComponent).createDialog(view)
							.setVisible(true);
			}
		});
		physicsMenu.add(miViscosity);

		miEField = new JMenuItem(new EditEFieldAction(view.getModel(), true));
		s = MDView.getInternationalText("ElectricFieldLabel");
		if (s != null)
			miEField.setText(s);
		physicsMenu.add(miEField);

		miBField = new JMenuItem(new EditBFieldAction(view.getModel(), true));
		s = MDView.getInternationalText("MagneticFieldLabel");
		if (s != null)
			miBField.setText(s);
		physicsMenu.add(miBField);

		s = MDView.getInternationalText("Reflection");
		miLineReflect = new JCheckBoxMenuItem(s != null ? s : "Reflection");
		miLineReflect.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (view.selectedComponent instanceof LineComponent) {
					((LineComponent) view.selectedComponent).setReflector(e.getStateChange() == ItemEvent.SELECTED);
				}
			}
		});
		physicsMenu.add(miLineReflect);

		// order

		s = MDView.getInternationalText("Order");
		JMenu menu = new JMenu(s != null ? s : "Order");
		menu.setIcon(new ImageIcon(getClass().getResource("images/LayerOrder.gif")));
		add(menu);

		a = new DefaultModelAction(view.getModel(), new Executable() {
			public void execute() {
				if (view.selectedComponent instanceof Layered) {
					view.bringLayerToFront((Layered) view.selectedComponent);
					view.repaint();
				}
			}
		});
		a.putValue(Action.SMALL_ICON, new ImageIcon(MDView.class.getResource("images/BringToFront.gif")));
		a.putValue(Action.NAME, "Bring to Front");
		bringToFrontMI = new JMenuItem(a);
		s = MDView.getInternationalText("BringToFront");
		if (s != null)
			bringToFrontMI.setText(s);
		menu.add(bringToFrontMI);

		a = new DefaultModelAction(view.getModel(), new Executable() {
			public void execute() {
				if (view.selectedComponent instanceof Layered) {
					view.sendLayerToBack((Layered) view.selectedComponent);
					view.repaint();
				}
			}
		});
		a.putValue(Action.SMALL_ICON, new ImageIcon(MDView.class.getResource("images/SendToBack.gif")));
		a.putValue(Action.NAME, "Send to Back");
		sendToBackMI = new JMenuItem(a);
		s = MDView.getInternationalText("SendToBack");
		if (s != null)
			sendToBackMI.setText(s);
		menu.add(sendToBackMI);

		a = new DefaultModelAction(view.getModel(), new Executable() {
			public void execute() {
				if (view.selectedComponent instanceof Layered) {
					view.layerForward((Layered) view.selectedComponent);
					view.repaint();
				}
			}
		});
		a.putValue(Action.SMALL_ICON, new ImageIcon(MDView.class.getResource("images/BringForward.gif")));
		a.putValue(Action.NAME, "Bring Forward");
		bringForwardMI = new JMenuItem(a);
		s = MDView.getInternationalText("BringForward");
		if (s != null)
			bringForwardMI.setText(s);
		menu.add(bringForwardMI);

		a = new DefaultModelAction(view.getModel(), new Executable() {
			public void execute() {
				if (view.selectedComponent instanceof Layered) {
					view.layerBack((Layered) view.selectedComponent);
					view.repaint();
				}
			}
		});
		a.putValue(Action.SMALL_ICON, new ImageIcon(MDView.class.getResource("images/SendBackward.gif")));
		a.putValue(Action.NAME, "Send Backward");
		sendBackwardMI = new JMenuItem(a);
		s = MDView.getInternationalText("SendBackward");
		if (s != null)
			sendBackwardMI.setText(s);
		menu.add(sendBackwardMI);

		a = new DefaultModelAction(view.getModel(), new Executable() {
			public void execute() {
				if (view.selectedComponent instanceof Layered) {
					view.bringInFrontOfParticles((Layered) view.selectedComponent);
					view.repaint();
				}
			}
		});
		a.putValue(Action.SMALL_ICON, new ImageIcon(MDView.class.getResource("images/BringFront.gif")));
		a.putValue(Action.NAME, "Bring in Front of Physical Objects");
		bringFrontMI = new JMenuItem(a);
		s = MDView.getInternationalText("BringInFrontOfPhysicalObjects");
		if (s != null)
			bringFrontMI.setText(s);
		menu.add(bringFrontMI);

		a = new DefaultModelAction(view.getModel(), new Executable() {
			public void execute() {
				if (view.selectedComponent instanceof Layered) {
					view.sendBehindParticles(((Layered) view.selectedComponent));
					view.repaint();
				}
			}
		});
		a.putValue(Action.SMALL_ICON, new ImageIcon(MDView.class.getResource("images/SendBehind.gif")));
		a.putValue(Action.NAME, "Send Behind Physical Objects");
		sendBehindMI = new JMenuItem(a);
		s = MDView.getInternationalText("SendBehindPhysicalObjects");
		if (s != null)
			sendBehindMI.setText(s);
		menu.add(sendBehindMI);

		s = MDView.getInternationalText("Position");
		positionMenu = new JMenu(s != null ? s : "Position");
		positionMenu.setIcon(new ImageIcon(getClass().getResource("images/LayerPosition.gif")));

		a = new DefaultModelAction(view.getModel(), new Executable() {
			public void execute() {
				if (view.selectedComponent instanceof DrawingElement) {
					view.selectedComponent.storeCurrentState();
					DrawingElement c = (DrawingElement) view.selectedComponent;
					c.snapPosition(DrawingElement.SNAP_TO_CENTER);
					view.getModel().notifyChange();
					view.prepareToUndoPositioning();
					view.repaint();
				}
				else if (view.selectedComponent instanceof ImageComponent) {
					view.selectedComponent.storeCurrentState();
					ImageComponent c = (ImageComponent) view.selectedComponent;
					c.snapPosition(DrawingElement.SNAP_TO_CENTER);
					view.getModel().notifyChange();
					view.prepareToUndoPositioning();
					view.repaint();
				}
			}
		});
		a.putValue(Action.SMALL_ICON, new ImageIcon(MDView.class.getResource("images/CenterImage.gif")));
		a.putValue(Action.NAME, "Center");
		a.putValue(Action.SHORT_DESCRIPTION, "Put at the center of the model");
		centerMI = new JMenuItem(a);
		s = MDView.getInternationalText("Center");
		if (s != null)
			centerMI.setText(s);
		positionMenu.add(centerMI);

		a = new DefaultModelAction(view.getModel(), new Executable() {
			public void execute() {
				if (view.selectedComponent instanceof DrawingElement) {
					view.selectedComponent.storeCurrentState();
					DrawingElement c = (DrawingElement) view.selectedComponent;
					c.snapPosition(DrawingElement.SNAP_TO_NORTH_SIDE);
					view.getModel().notifyChange();
					view.prepareToUndoPositioning();
					view.repaint();
				}
				else if (view.selectedComponent instanceof ImageComponent) {
					view.selectedComponent.storeCurrentState();
					ImageComponent c = (ImageComponent) view.selectedComponent;
					c.snapPosition(DrawingElement.SNAP_TO_NORTH_SIDE);
					view.getModel().notifyChange();
					view.prepareToUndoPositioning();
					view.repaint();
				}
			}
		});
		a.putValue(Action.SMALL_ICON, new ImageIcon(MDView.class.getResource("images/NorthImage.gif")));
		a.putValue(Action.NAME, "Stick to North Side");
		a.putValue(Action.SHORT_DESCRIPTION, "Stick to the north side of the model");
		northMI = new JMenuItem(a);
		s = MDView.getInternationalText("North");
		if (s != null)
			northMI.setText(s);
		positionMenu.add(northMI);

		a = new DefaultModelAction(view.getModel(), new Executable() {
			public void execute() {
				if (view.selectedComponent instanceof DrawingElement) {
					view.selectedComponent.storeCurrentState();
					DrawingElement c = (DrawingElement) view.selectedComponent;
					c.snapPosition(DrawingElement.SNAP_TO_SOUTH_SIDE);
					view.getModel().notifyChange();
					view.prepareToUndoPositioning();
					view.repaint();
				}
				else if (view.selectedComponent instanceof ImageComponent) {
					view.selectedComponent.storeCurrentState();
					ImageComponent c = (ImageComponent) view.selectedComponent;
					c.snapPosition(DrawingElement.SNAP_TO_SOUTH_SIDE);
					view.getModel().notifyChange();
					view.prepareToUndoPositioning();
					view.repaint();
				}
			}
		});
		a.putValue(Action.SMALL_ICON, new ImageIcon(MDView.class.getResource("images/SouthImage.gif")));
		a.putValue(Action.NAME, "Stick to South Side");
		a.putValue(Action.SHORT_DESCRIPTION, "Stick to the south side of the model");
		southMI = new JMenuItem(a);
		s = MDView.getInternationalText("South");
		if (s != null)
			southMI.setText(s);
		positionMenu.add(southMI);

		a = new DefaultModelAction(view.getModel(), new Executable() {
			public void execute() {
				if (view.selectedComponent instanceof DrawingElement) {
					view.selectedComponent.storeCurrentState();
					DrawingElement c = (DrawingElement) view.selectedComponent;
					c.snapPosition(DrawingElement.SNAP_TO_EAST_SIDE);
					view.getModel().notifyChange();
					view.prepareToUndoPositioning();
					view.repaint();
				}
				else if (view.selectedComponent instanceof ImageComponent) {
					view.selectedComponent.storeCurrentState();
					ImageComponent c = (ImageComponent) view.selectedComponent;
					c.snapPosition(DrawingElement.SNAP_TO_EAST_SIDE);
					view.getModel().notifyChange();
					view.prepareToUndoPositioning();
					view.repaint();
				}
			}
		});
		a.putValue(Action.SMALL_ICON, new ImageIcon(MDView.class.getResource("images/EastImage.gif")));
		a.putValue(Action.NAME, "Stick to East Side");
		a.putValue(Action.SHORT_DESCRIPTION, "Stick to the east side of the model");
		eastMI = new JMenuItem(a);
		s = MDView.getInternationalText("East");
		if (s != null)
			eastMI.setText(s);
		positionMenu.add(eastMI);

		a = new DefaultModelAction(view.getModel(), new Executable() {
			public void execute() {
				if (view.selectedComponent instanceof DrawingElement) {
					view.selectedComponent.storeCurrentState();
					DrawingElement c = (DrawingElement) view.selectedComponent;
					c.snapPosition(DrawingElement.SNAP_TO_WEST_SIDE);
					view.getModel().notifyChange();
					view.prepareToUndoPositioning();
					view.repaint();
				}
				if (view.selectedComponent instanceof ImageComponent) {
					view.selectedComponent.storeCurrentState();
					ImageComponent c = (ImageComponent) view.selectedComponent;
					c.snapPosition(DrawingElement.SNAP_TO_WEST_SIDE);
					view.getModel().notifyChange();
					view.prepareToUndoPositioning();
					view.repaint();
				}
			}
		});
		a.putValue(Action.SMALL_ICON, new ImageIcon(MDView.class.getResource("images/WestImage.gif")));
		a.putValue(Action.NAME, "Stick to West Side");
		a.putValue(Action.SHORT_DESCRIPTION, "Stick to the west side of the model");
		westMI = new JMenuItem(a);
		s = MDView.getInternationalText("West");
		if (s != null)
			westMI.setText(s);
		positionMenu.add(westMI);

		add(positionMenu);
		addSeparator();

		s = MDView.getInternationalText("Properties");
		mi = new JMenuItem(s != null ? s : "Properties", IconPool.getIcon("properties"));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DialogFactory.showDialog(view.selectedComponent);
			}
		});
		add(mi);

		s = MDView.getInternationalText("Visible");
		miVisible = new JCheckBoxMenuItem(s != null ? s : "Visible");
		miVisible.setIcon(IconPool.getIcon("view"));
		miVisible.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (view.selectedComponent instanceof Layered) {
					view.selectedComponent.setVisible(e.getStateChange() == ItemEvent.SELECTED);
					view.repaint();
					view.getModel().notifyChange();
				}
			}
		});
		add(miVisible);

		s = MDView.getInternationalText("DraggableByUserInNonEditingMode");
		miDraggable = new JCheckBoxMenuItem(s != null ? s : "Draggable by User in Non-Editing Mode");
		miDraggable.setIcon(IconPool.getIcon("user draggable"));
		miDraggable.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (view.selectedComponent instanceof Layered) {
					view.selectedComponent.setDraggable(e.getStateChange() == ItemEvent.SELECTED);
					view.repaint();
					view.getModel().notifyChange();
				}
			}
		});
		add(miDraggable);

	}

}