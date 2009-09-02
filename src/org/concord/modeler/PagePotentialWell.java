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
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.concord.modeler.event.ModelEvent;
import org.concord.modeler.text.Page;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.IntegerTextField;
import org.concord.modeler.ui.RealNumberTextField;
import org.concord.mw2d.PotentialWell;
import org.concord.mw2d.models.Reaction;
import org.concord.mw2d.models.ReactionModel;

public class PagePotentialWell extends PotentialWell implements Embeddable, ModelCommunicator {

	private Page page;
	private int index;
	private String uid;
	private String modelClass;
	private int modelID = -1;
	private boolean marked;
	private boolean wasOpaque;
	private boolean changable;
	private Color originalBackground, originalForeground;

	private JComboBox modelComboBox, reactionComboBox, borderComboBox, opaqueComboBox;
	private IntegerTextField widthField, heightField;
	private RealNumberTextField depthField;
	private JDialog dialog;
	private boolean cancel;
	private JButton okButton;
	private ColorComboBox colorComboBox;
	private JPopupMenu popupMenu;

	private ItemListener modelSelectionListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			Model m = (Model) modelComboBox.getSelectedItem();
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				m.removeModelListener(PagePotentialWell.this);
			}
			else {
				setModelID(page.getComponentPool().getIndex(m));
				m.addModelListener(PagePotentialWell.this);
			}
		}
	};

	public PagePotentialWell() {
		super();
	}

	public PagePotentialWell(PagePotentialWell well, Page parent) {
		this();
		setUid(well.uid);
		setPage(parent);
		setOpaque(well.isOpaque());
		setBorderType(well.getBorderType());
		setModelID(well.modelID);
		setColor(well.getBackground());
		setMaximumDepth(well.getMaximumDepth());
		setOwner(well.getOwner());
		setPreferredSize(well.getPreferredSize());
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
	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	public void createPopupMenu() {

		popupMenu = new JPopupMenu();
		popupMenu.setInvoker(this);

		final JMenuItem miCustom = new JMenuItem("Customize This Graph...");
		miCustom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialogBox(page);
			}
		});
		popupMenu.add(miCustom);

		final JMenuItem miRemove = new JMenuItem("Remove This Graph");
		miRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(PagePotentialWell.this);
			}
		});
		popupMenu.add(miRemove);

		JMenuItem mi = new JMenuItem("Copy This Graph");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(PagePotentialWell.this);
			}
		});
		popupMenu.add(mi);
		popupMenu.addSeparator();

		mi = new JMenuItem("Take a Snapshot...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SnapshotGallery.sharedInstance().takeSnapshot(page.getAddress(), PagePotentialWell.this);
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

	public String getBorderType() {
		return BorderManager.getBorder(this);
	}

	public void setBorderType(String s) {
		BorderManager.setBorder(this, s, page.getBackground());
	}

	public static PagePotentialWell create(Page page) {
		if (page == null)
			return null;
		PagePotentialWell pw = new PagePotentialWell();
		pw.setFont(page.getFont());
		pw.dialogBox(page);
		if (pw.cancel)
			return null;
		return pw;
	}

	public void setPreferredSize(Dimension dim) {
		if (dim == null)
			throw new IllegalArgumentException("You must input a non-null object");
		if (dim.width == 0 || dim.height == 0)
			throw new IllegalArgumentException("You must input a non-zero dimension");
		super.setPreferredSize(dim);
		super.setMinimumSize(dim);
		super.setMaximumSize(dim);
		if (widthField != null)
			widthField.setValue(dim.width);
		if (heightField != null)
			heightField.setValue(dim.height);
	}

	private boolean confirm() {
		Model m = (Model) modelComboBox.getSelectedItem();
		if (!(m instanceof ReactionModel)) {
			JOptionPane.showMessageDialog(this,
					"This controller can be applied only to\nthe Chemical Reaction Kinetics Model.",
					"Bond-Breaking Barrier", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		m.addModelListener(this);
		setModelID(page.getComponentPool().getIndex(m));
		setOwner((String) reactionComboBox.getSelectedItem());
		double de = ((ReactionModel) m).getType().getParameter(getOwner()).doubleValue();
		if (de > depthField.getValue()) {
			JOptionPane.showMessageDialog(this,
					"The Maximum Depth must not be smaller than the\ncurrent depth. Please set a greater value.",
					"Error in Maximum Depth Field", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		setBorderType((String) borderComboBox.getSelectedItem());
		setOpaque(opaqueComboBox.getSelectedIndex() == 0);
		setColor(colorComboBox.getSelectedColor());
		setDissociationEnergy((float) de);
		setMaximumDepth((float) depthField.getValue());
		setPreferredSize(new Dimension(widthField.getValue(), heightField.getValue()));
		setChangable(true);
		page.getSaveReminder().setChanged(true);
		page.settleComponentSize();
		return true;
	}

	private void dialogBox(Page page) {

		this.page = page;
		page.deselect();

		if (dialog == null) {

			ActionListener okListener = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (confirm()) {
						dialog.setVisible(false);
						cancel = false;
					}
				}
			};

			dialog = ModelerUtilities.getChildDialog(page, "Customize bond-breaking barrier controller", true);

			Container container = dialog.getContentPane();
			container.setLayout(new BorderLayout());

			JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			container.add(p, BorderLayout.SOUTH);

			String s = Modeler.getInternationalText("OKButton");
			okButton = new JButton(s != null ? s : "OK");
			okButton.addActionListener(okListener);
			p.add(okButton);

			s = Modeler.getInternationalText("CancelButton");
			JButton button = new JButton(s != null ? s : "Cancel");
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dialog.setVisible(false);
					cancel = true;
				}
			});
			p.add(button);

			p = new JPanel(new BorderLayout(10, 10)) {
				public Insets getInsets() {
					return new Insets(10, 10, 10, 10);
				}
			};
			container.add(p, BorderLayout.CENTER);

			JPanel p1 = new JPanel(new GridLayout(8, 1, 3, 3));
			p1.add(new JLabel("Select a model", SwingConstants.LEFT));
			p1.add(new JLabel("Select a well", SwingConstants.LEFT));
			p1.add(new JLabel("Maximum depth", SwingConstants.LEFT));
			p1.add(new JLabel("Width", SwingConstants.LEFT));
			p1.add(new JLabel("Height", SwingConstants.LEFT));
			p1.add(new JLabel("Color", SwingConstants.LEFT));
			p1.add(new JLabel("Border", SwingConstants.LEFT));
			p1.add(new JLabel("Opaque", SwingConstants.LEFT));
			p.add(p1, BorderLayout.WEST);

			p1 = new JPanel(new GridLayout(8, 1, 3, 3));

			modelComboBox = new JComboBox();
			modelComboBox.setPreferredSize(new Dimension(200, 20));
			modelComboBox
					.setToolTipText("If there are multiple models on the page, select the one this barrier will interact with.");
			p1.add(modelComboBox);

			reactionComboBox = new JComboBox();
			reactionComboBox.setToolTipText("Select the elementary reaction this barrier will control.");
			p1.add(reactionComboBox);

			depthField = new RealNumberTextField(0.1, 0.001, 100.0);
			depthField.setToolTipText("Type in a value for the depth of the potential well.");
			depthField.addActionListener(okListener);
			p1.add(depthField);

			widthField = new IntegerTextField(200, 20, 800);
			widthField.setToolTipText("Type in an integer to set the width of this barrier.");
			widthField.addActionListener(okListener);
			p1.add(widthField);

			heightField = new IntegerTextField(200, 20, 800);
			heightField.setToolTipText("Type in an integer to set the height of this barrier.");
			heightField.addActionListener(okListener);
			p1.add(heightField);

			colorComboBox = new ColorComboBox(this);
			colorComboBox.setSelectedIndex(6);
			colorComboBox.setRequestFocusEnabled(false);
			colorComboBox.setToolTipText("Select color.");
			p1.add(colorComboBox);

			borderComboBox = new JComboBox(BorderManager.BORDER_TYPE);
			borderComboBox.setRenderer(new ComboBoxRenderer.BorderCell());
			borderComboBox.setBackground(p1.getBackground());
			borderComboBox.setToolTipText("Select the border type for this component.");
			p1.add(borderComboBox);

			opaqueComboBox = new JComboBox(new Object[] { "Yes", "No" });
			opaqueComboBox
					.setToolTipText("Select yes to set this barrier to be opaque; select no to set it to be transparent.");
			p1.add(opaqueComboBox);

			p.add(p1, BorderLayout.CENTER);

			dialog.pack();
			dialog.setLocationRelativeTo(dialog.getOwner());

			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					cancel = true;
					dialog.setVisible(false);
				}

				public void windowActivated(WindowEvent e) {
					widthField.selectAll();
					widthField.requestFocusInWindow();
				}
			});

		}

		final ComponentPool componentPool = page.getComponentPool();

		modelComboBox.removeItemListener(modelSelectionListener);
		modelComboBox.removeAllItems();
		reactionComboBox.removeAllItems();

		if (componentPool != null) {

			synchronized (componentPool) {
				for (ModelCanvas mc : componentPool.getModels()) {
					if (mc.isUsed()) {
						modelComboBox.addItem(mc.getMdContainer().getModel());
					}
				}
			}
			if (modelID != -1) {
				ModelCanvas mc = componentPool.get(modelID);
				modelComboBox.setSelectedItem(mc.getMdContainer().getModel());
			}
			else {
				Model m = (Model) modelComboBox.getSelectedItem();
				PagePotentialWell.this.setModelID(componentPool.getIndex(m));
			}
			modelComboBox.addItemListener(modelSelectionListener);

			if (modelComboBox.getSelectedItem() instanceof ReactionModel) {
				ReactionModel m = (ReactionModel) modelComboBox.getSelectedItem();
				int n = m.getType().getWells().size();
				if (n > 0) {
					for (int i = 0; i < n; i++) {
						reactionComboBox.addItem(m.getType().getWells().get(i));
					}
				}
			}

		}

		if (getOwner() != null)
			reactionComboBox.setSelectedItem(getOwner());
		depthField.setValue(getMaximumDepth());
		if (isPreferredSizeSet()) {
			widthField.setValue(getPreferredSize().width);
			heightField.setValue(getPreferredSize().height);
		}
		opaqueComboBox.setSelectedIndex(isOpaque() ? 0 : 1);
		borderComboBox.setSelectedItem(getBorderType());
		colorComboBox.setColor(getColor());
		okButton.setEnabled(modelComboBox.getItemCount() > 0 && reactionComboBox.getItemCount() > 0);

		dialog.setVisible(true);

	}

	public void modelUpdate(ModelEvent e) {
		Object src = e.getSource();
		int id = e.getID();
		if (src instanceof ReactionModel) {
			if (id == ModelEvent.MODEL_INPUT) {
				ReactionModel rm = (ReactionModel) src;
				setDissociationEnergy((float) rm.getType().getParameter(getOwner()).doubleValue());
			}
		}
	}

	public void setOwner(String s) {
		super.setOwner(s);
		if (s.equalsIgnoreCase("VAA"))
			setReaction(new Reaction.A2__2A());
		else if (s.equalsIgnoreCase("VBB"))
			setReaction(new Reaction.B2__2B());
		else if (s.equalsIgnoreCase("VAB"))
			setReaction(new Reaction.AB__A_B());
	}

	public void mousePressed(MouseEvent e) {
		if (ModelerUtilities.isRightClick(e)) {
			if (popupMenu == null)
				createPopupMenu();
			popupMenu.show(popupMenu.getInvoker(), e.getX() + 5, e.getY() + 5);
		}
		else {
			super.mousePressed(e);
		}
	}

	public void mouseReleased(MouseEvent e) {
		if (isHandleSelected()) {
			if (modelID != -1) {
				ModelCanvas mc = page.getComponentPool().get(modelID);
				if (mc.getMdContainer().getModel() instanceof ReactionModel) {
					ReactionModel m = (ReactionModel) mc.getMdContainer().getModel();
					m.getType().putParameter(getOwner(), new Double(getDissociationEnergy()));
					m.changeChemicalEnergies();
				}
			}
		}
		super.mouseReleased(e);
	}

	public String toString() {
		return "<class>"
				+ getClass().getName()
				+ "</class>\n"
				+ "<width>"
				+ getWidth()
				+ "</width>\n"
				+ "<height>"
				+ getHeight()
				+ "</height>\n"
				+ "<description>"
				+ getOwner()
				+ "</description>\n"
				+ "<maximum>"
				+ getMaximumDepth()
				+ "</maximum>\n"
				+ (getBorderType().equals(BorderManager.BORDER_TYPE[0]) ? "" : "<border>" + getBorderType()
						+ "</border>\n")
				+ (!isOpaque() ? "<opaque>false</opaque>\n" : "")
				+ "<model>"
				+ getModelID()
				+ "</model>\n"
				+ (getColor().equals(new Color(255, 204, 0)) ? "" : "<bgcolor>"
						+ Integer.toString(getColor().getRGB(), 16) + "</bgcolor>");
	}

}
