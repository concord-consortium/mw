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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.concord.modeler.event.ModelEvent;
import org.concord.mw2d.models.Atom;
import org.concord.mw2d.models.GayBerneParticle;
import org.concord.mw2d.models.Layered;
import org.concord.mw2d.models.LineComponent;
import org.concord.mw2d.models.MDModel;
import org.concord.mw2d.models.MesoModel;
import org.concord.mw2d.models.ModelComponent;
import org.concord.mw2d.models.MolecularModel;
import org.concord.mw2d.models.RadialBond;
import org.concord.mw2d.models.RadialBondCollection;
import org.concord.mw2d.models.RectangularObstacle;
import org.concord.mw2d.models.TextBoxComponent;

class AttachDialog extends JDialog {

	private JList particleList;
	private JList obstacleList;
	private JList bondList;
	private JRadioButton centerButton, endpoint1Button, endpoint2Button;

	AttachDialog(final MDModel model) {

		super(JOptionPane.getFrameForComponent(model.getView()), "Attach to an Object", true);
		String s = MDView.getInternationalText("AttachTo");
		if (s != null)
			setTitle(s);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLocation(200, 200);

		s = MDView.getInternationalText("OKButton");
		final JButton okButton = new JButton(s != null ? s : "OK");

		Container container = getContentPane();

		final MDView view = (MDView) model.getView();
		final Layered mc = (Layered) view.getSelectedComponent();

		Dimension listDim = new Dimension(200, 100);
		int n = model.getNumberOfParticles();
		Box box = Box.createVerticalBox();

		if (model instanceof MolecularModel) {
			Atom[] a = new Atom[n];
			MolecularModel mm = (MolecularModel) model;
			for (int i = 0; i < n; i++)
				a[i] = mm.getAtom(i);
			particleList = new JList(a);
			RadialBondCollection bonds = mm.getBonds();
			if (bonds != null) {
				int size = bonds.size();
				if (size > 0) {
					if (mc instanceof LineComponent) {
						RadialBond[] rb = new RadialBond[size];
						for (int i = 0; i < size; i++) {
							rb[i] = bonds.get(i);
						}
						bondList = new JList(rb);
					}
				}
			}
			if (model.getObstacles() != null && !model.getObstacles().isEmpty())
				obstacleList = new JList(model.getObstacles().toArray());
		}
		else if (model instanceof MesoModel) {
			GayBerneParticle[] g = new GayBerneParticle[n];
			MesoModel mm = (MesoModel) model;
			for (int i = 0; i < n; i++)
				g[i] = (GayBerneParticle) mm.getParticle(i);
			particleList = new JList(g);
		}

		particleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		particleList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (particleList.hasFocus()) {
					if (!e.getValueIsAdjusting()) {
						if (obstacleList != null)
							obstacleList.clearSelection();
						if (bondList != null)
							bondList.clearSelection();
						((ModelComponent) particleList.getSelectedValue()).blink();
						setAttachmentPositionOptionsEnabled(true);
					}
				}
			}
		});
		particleList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() >= 2)
					okButton.doClick();
			}
		});
		particleList.setBorder(BorderFactory.createLoweredBevelBorder());
		JScrollPane scroller = new JScrollPane(particleList);

		s = MDView.getInternationalText("Particle");
		scroller.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Particle"));
		scroller.setPreferredSize(listDim);
		box.add(scroller);

		if (bondList != null) {
			bondList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			bondList.setBorder(BorderFactory.createLoweredBevelBorder());
			bondList.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					if (bondList.hasFocus()) {
						if (!e.getValueIsAdjusting()) {
							particleList.clearSelection();
							if (obstacleList != null)
								obstacleList.clearSelection();
							((RadialBond) bondList.getSelectedValue()).blink();
							setAttachmentPositionOptionsEnabled(false);
						}
					}
				}
			});
			bondList.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() >= 2)
						okButton.doClick();
				}
			});
			JScrollPane scroller2 = new JScrollPane(bondList);
			s = MDView.getInternationalText("RadialBond");
			scroller2.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Radial Bond"));
			scroller2.setPreferredSize(listDim);
			box.add(scroller2);
		}

		if (obstacleList != null) {
			obstacleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			obstacleList.setBorder(BorderFactory.createLoweredBevelBorder());
			obstacleList.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					if (obstacleList.hasFocus()) {
						if (!e.getValueIsAdjusting()) {
							particleList.clearSelection();
							if (bondList != null)
								bondList.clearSelection();
							((RectangularObstacle) obstacleList.getSelectedValue()).blink();
							setAttachmentPositionOptionsEnabled(true);
						}
					}
				}
			});
			obstacleList.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() >= 2)
						okButton.doClick();
				}
			});
			JScrollPane scroller2 = new JScrollPane(obstacleList);
			s = MDView.getInternationalText("Obstacle");
			scroller2.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Obstacle"));
			scroller2.setPreferredSize(listDim);
			box.add(scroller2);
		}

		container.add(box, BorderLayout.NORTH);

		if (mc.getHost() instanceof Atom) {
			particleList.setSelectedValue(mc.getHost(), true);
		}
		if (mc.getHost() instanceof RadialBond) {
			bondList.setSelectedValue(mc.getHost(), true);
		}
		else if (mc.getHost() instanceof RectangularObstacle) {
			obstacleList.setSelectedValue(mc.getHost(), true);
		}

		if (mc instanceof TextBoxComponent) {

			final TextBoxComponent t = (TextBoxComponent) mc;

			JPanel p = new JPanel();
			container.add(p, BorderLayout.CENTER);

			s = MDView.getInternationalText("AttachedPoint");
			p.add(new JLabel(s != null ? s : "Attached Point:"));

			ButtonGroup bg = new ButtonGroup();

			s = MDView.getInternationalText("BoxCenter");
			JRadioButton b = new JRadioButton(s != null ? s : "Box Center");
			b.setSelected(t.getAttachmentPosition() == TextBoxComponent.BOX_CENTER);
			b.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						t.setAttachmentPosition(TextBoxComponent.BOX_CENTER);
						view.repaint();
						model.notifyModelListeners(new ModelEvent(model, ModelEvent.MODEL_CHANGED));
						model.notifyChange();
					}
				}
			});
			bg.add(b);
			p.add(b);

			s = MDView.getInternationalText("ArrowHead");
			b = new JRadioButton(s != null ? s : "Arrow Head");
			b.setSelected(t.getAttachmentPosition() == TextBoxComponent.ARROW_HEAD);
			b.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						t.setAttachmentPosition(TextBoxComponent.ARROW_HEAD);
						view.repaint();
						model.notifyModelListeners(new ModelEvent(model, ModelEvent.MODEL_CHANGED));
						model.notifyChange();
					}
				}
			});
			bg.add(b);
			p.add(b);

		}
		else if (mc instanceof LineComponent) {

			final LineComponent l = (LineComponent) mc;

			JPanel p = new JPanel();
			container.add(p, BorderLayout.CENTER);

			s = MDView.getInternationalText("AttachedPoint");
			p.add(new JLabel(s != null ? s : "Attached Point:"));

			ButtonGroup bg = new ButtonGroup();

			s = MDView.getInternationalText("LineCenter");
			centerButton = new JRadioButton(s != null ? s : "Center");
			centerButton.setSelected(l.getAttachmentPosition() == LineComponent.CENTER);
			centerButton.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						l.setAttachmentPosition(LineComponent.CENTER);
						view.repaint();
						model.notifyModelListeners(new ModelEvent(model, ModelEvent.MODEL_CHANGED));
						model.notifyChange();
					}
				}
			});
			bg.add(centerButton);
			p.add(centerButton);

			s = MDView.getInternationalText("EndPoint1");
			endpoint1Button = new JRadioButton(s != null ? s : "Endpoint 1");
			endpoint1Button.setSelected(l.getAttachmentPosition() == LineComponent.ENDPOINT1);
			endpoint1Button.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						l.setAttachmentPosition(LineComponent.ENDPOINT1);
						view.repaint();
						model.notifyModelListeners(new ModelEvent(model, ModelEvent.MODEL_CHANGED));
						model.notifyChange();
					}
				}
			});
			bg.add(endpoint1Button);
			p.add(endpoint1Button);

			s = MDView.getInternationalText("EndPoint2");
			endpoint2Button = new JRadioButton(s != null ? s : "Endpoint 2");
			endpoint2Button.setSelected(l.getAttachmentPosition() == LineComponent.ENDPOINT2);
			endpoint2Button.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						l.setAttachmentPosition(LineComponent.ENDPOINT2);
						view.repaint();
						model.notifyModelListeners(new ModelEvent(model, ModelEvent.MODEL_CHANGED));
						model.notifyChange();
					}
				}
			});
			bg.add(endpoint2Button);
			p.add(endpoint2Button);

		}

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (particleList.getSelectedValue() != null) {
					mc.setHost((ModelComponent) particleList.getSelectedValue());
				}
				else if (bondList != null && bondList.getSelectedValue() != null) {
					mc.setHost((ModelComponent) bondList.getSelectedValue());
				}
				else if (obstacleList != null && obstacleList.getSelectedValue() != null) {
					mc.setHost((RectangularObstacle) obstacleList.getSelectedValue());
				}
				model.notifyModelListeners(new ModelEvent(model, ModelEvent.MODEL_CHANGED));
				model.notifyChange();
				view.repaint();
				dispose();
			}
		});
		buttonPanel.add(okButton);

		s = MDView.getInternationalText("CancelButton");
		JButton button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		buttonPanel.add(button);

		container.add(buttonPanel, BorderLayout.SOUTH);

	}

	private void setAttachmentPositionOptionsEnabled(boolean b) {
		if (endpoint1Button != null) {
			endpoint1Button.setEnabled(b);
			endpoint2Button.setEnabled(b);
			centerButton.setEnabled(b);
			if (!b)
				centerButton.setSelected(true);
		}
	}

}