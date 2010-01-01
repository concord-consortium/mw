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
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.concord.modeler.event.ModelEvent;
import org.concord.modeler.event.PageComponentEvent;
import org.concord.modeler.text.Page;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.IntegerTextField;
import org.concord.molbio.engine.DNA;
import org.concord.molbio.engine.Nucleotide;
import org.concord.molbio.engine.DeletionMutator;
import org.concord.molbio.engine.InsertionMutator;
import org.concord.molbio.engine.SubstitutionMutator;
import org.concord.molbio.event.MutationEvent;
import org.concord.molbio.event.MutationListener;
import org.concord.molbio.ui.DNAScroller;
import org.concord.mw2d.AtomisticView;
import org.concord.mw2d.models.Atom;
import org.concord.mw2d.models.Codon;
import org.concord.mw2d.models.MolecularModel;
import org.concord.mw2d.models.Molecule;
import org.concord.mw2d.models.MoleculeCollection;
import org.concord.mw2d.models.Polypeptide;
import org.concord.mw2d.models.RadialBond;

public class PageDNAScroller extends DNAScroller implements Embeddable, ModelCommunicator, ItemListener,
		MutationListener {

	private Page page;
	private int index;
	private String uid;
	private String modelClass;
	private int modelID = -1;
	private int proteinID = -1;
	private boolean marked;
	private boolean changable;
	private Color originalBackground;
	private static Color defaultBackground, defaultForeground;

	private JComboBox modelComboBox, proteinComboBox, borderComboBox, opaqueComboBox;
	private IntegerTextField widthField, heightField;
	private JDialog dialog;
	private boolean cancel;
	private JButton okButton;
	private ColorComboBox colorComboBox;
	private JPopupMenu popupMenu;
	private MouseListener popupMouseListener;

	private ItemListener modelSelectionListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			Model m = (Model) modelComboBox.getSelectedItem();
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				m.removeModelListener(PageDNAScroller.this);
			}
			else {
				setModelID(page.getComponentPool().getIndex(m));
				m.addModelListener(PageDNAScroller.this);
			}
		}
	};

	public PageDNAScroller() {
		super();
		setHighlightColor(Color.green);
		setDNA(new DNA("ACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGT"));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		popupMouseListener = new PopupMouseListener(this);
		addMouseListener(popupMouseListener);
		addItemListener(this);
		addMutationListener(this);
		if (defaultBackground == null)
			defaultBackground = getBackground();
		if (defaultForeground == null)
			defaultForeground = getForeground();
	}

	public PageDNAScroller(PageDNAScroller s, Page parent) {
		this();
		setPage(parent);
		setModelID(s.modelID);
		setUid(s.uid);
		setBorderType(s.getBorderType());
		setOpaque(s.isOpaque());
		setProteinID(s.proteinID);
		setBackground(s.getBackground());
		setPreferredSize(s.getPreferredSize());
		fillDNA();
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

		if (popupMenu != null)
			return;

		popupMenu = new JPopupMenu();
		popupMenu.setInvoker(this);

		final JMenuItem miCustom = new JMenuItem("Customize This DNA Scroller...");
		miCustom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialogBox(page);
			}
		});
		popupMenu.add(miCustom);

		final JMenuItem miRemove = new JMenuItem("Remove This DNA Scroller");
		miRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(PageDNAScroller.this);
			}
		});
		popupMenu.add(miRemove);

		JMenuItem mi = new JMenuItem("Copy This DNA Scroller");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(PageDNAScroller.this);
			}
		});
		popupMenu.add(mi);
		popupMenu.addSeparator();

		mi = new JMenuItem("Take a Snapshot...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SnapshotGallery.sharedInstance().takeSnapshot(page.getAddress(), PageDNAScroller.this);
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

	public int getProteinID() {
		return proteinID;
	}

	public void setProteinID(int id) {
		proteinID = id;
	}

	/** populate this DNA scroller that corresponds to a protein */
	public void fillDNA() {
		if (modelID < 0 || proteinID < 0)
			return;
		ModelCanvas mc = page.getComponentPool().get(modelID);
		if (mc == null)
			return;
		MoleculeCollection c = ((MolecularModel) mc.getMdContainer().getModel()).getMolecules();
		if (c.isEmpty() || proteinID >= c.size()) {
			setDNA(null);
			return;
		}
		Molecule mol = c.get(proteinID);
		if (mol instanceof Polypeptide) {
			String s = ((Polypeptide) mol).getDNACode();
			if (s == null) {
				setDNA(null);
				return;
			}
			setDNA(new DNA(s));
		}
		else {
			setDNA(null);
		}
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

	public void setTransparent(boolean b) {
		setOpaque(!b);
	}

	public boolean isTransparent() {
		return !isOpaque();
	}

	public void setMarked(boolean b) {
		marked = b;
		if (page == null)
			return;
		if (b)
			originalBackground = getBackground();
		setBackground(b ? page.getSelectionColor() : originalBackground);
		setForeground(b ? page.getSelectedTextColor() : defaultForeground);
	}

	public boolean isMarked() {
		return marked;
	}

	public void setChangable(boolean b) {
		changable = b;
		setMutationEnabled(!b);
		if (b) {
			getScroller().addMouseListener(popupMouseListener);
		}
		else {
			getScroller().removeMouseListener(popupMouseListener);
		}
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

	public static PageDNAScroller create(Page page) {
		if (page == null)
			return null;
		PageDNAScroller pd = new PageDNAScroller();
		pd.setFont(page.getFont());
		pd.dialogBox(page);
		if (pd.cancel)
			return null;
		return pd;
	}

	public void setPreferredSize(Dimension dim) {
		if (dim == null)
			throw new IllegalArgumentException("null object");
		if (dim.width == 0 || dim.height == 0)
			throw new IllegalArgumentException("zero dimension");
		super.setPreferredSize(dim);
		super.setMinimumSize(dim);
		super.setMaximumSize(dim);
		if (widthField != null)
			widthField.setValue(dim.width);
		if (heightField != null)
			heightField.setValue(dim.height);
	}

	private boolean confirm() {
		setChangable(true);
		Model m = (Model) modelComboBox.getSelectedItem();
		m.addModelListener(this);
		setModelID(page.getComponentPool().getIndex(m));
		if (proteinComboBox.getSelectedItem() instanceof Molecule) {
			Molecule mol = (Molecule) proteinComboBox.getSelectedItem();
			proteinID = ((MolecularModel) m).getMolecules().indexOf(mol);
			if (mol instanceof Polypeptide) {
				setDNA(new DNA(((Polypeptide) mol).getDNACode()));
			}
			else {
				setDNA(null);
			}
		}
		setTransparent(opaqueComboBox.getSelectedIndex() == 0);
		setBorderType((String) borderComboBox.getSelectedItem());
		setBackground(colorComboBox.getSelectedColor());
		setPreferredSize(new Dimension(widthField.getValue(), heightField.getValue()));
		setMutationEnabled(false);
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

			dialog = ModelerUtilities.getChildDialog(page, "Customize DNA scroller", true);

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

			JPanel p1 = new JPanel(new GridLayout(7, 1, 3, 3));
			p1.add(new JLabel("Select a model", SwingConstants.LEFT));
			p1.add(new JLabel("Select a protein", SwingConstants.LEFT));
			p1.add(new JLabel("Width", SwingConstants.LEFT));
			p1.add(new JLabel("Height", SwingConstants.LEFT));
			p1.add(new JLabel("Transparent", SwingConstants.LEFT));
			p1.add(new JLabel("Color", SwingConstants.LEFT));
			p1.add(new JLabel("Border", SwingConstants.LEFT));
			p.add(p1, BorderLayout.WEST);

			p1 = new JPanel(new GridLayout(7, 1, 3, 3));

			modelComboBox = new JComboBox();
			modelComboBox.setPreferredSize(new Dimension(200, 20));
			modelComboBox
					.setToolTipText("If there are multiple models on the page, select the one this DNA scroller will interact with.");
			p1.add(modelComboBox);

			proteinComboBox = new JComboBox();
			proteinComboBox
					.setToolTipText("<html>Select the protein this DNA scroller will map to.<br>The displayed string in the pulldown menu is the primary sequence.");
			p1.add(proteinComboBox);

			widthField = new IntegerTextField(360, 100, 800);
			widthField.setToolTipText("Type in an integer to set the width of this DNA scroller.");
			widthField.addActionListener(okListener);
			p1.add(widthField);

			heightField = new IntegerTextField(120, 100, 800);
			heightField.setToolTipText("Type in an integer to set the height of this DNA scroller.");
			heightField.addActionListener(okListener);
			p1.add(heightField);

			opaqueComboBox = new JComboBox(new Object[] { "Yes", "No" });
			opaqueComboBox.setSelectedIndex(1);
			opaqueComboBox
					.setToolTipText("Select yes to set this DNA scroller to be opaque; select no to set it to be transparent.");
			p1.add(opaqueComboBox);

			colorComboBox = new ColorComboBox(this);
			colorComboBox.setSelectedIndex(ColorComboBox.INDEX_MORE_COLOR);
			colorComboBox.setRequestFocusEnabled(false);
			colorComboBox.setToolTipText("Select color.");
			p1.add(colorComboBox);

			borderComboBox = new JComboBox(BorderManager.BORDER_TYPE);
			borderComboBox.setToolTipText("Select the border type for this button.");
			borderComboBox.setRenderer(new ComboBoxRenderer.BorderCell());
			borderComboBox.setBackground(p1.getBackground());
			p1.add(borderComboBox);

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
		proteinComboBox.removeAllItems();

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
				setModelID(componentPool.getIndex(m));
			}
			modelComboBox.addItemListener(modelSelectionListener);

			if (modelComboBox.getSelectedItem() instanceof MolecularModel) {
				MolecularModel m = (MolecularModel) modelComboBox.getSelectedItem();
				MoleculeCollection c = m.getMolecules();
				if (!c.isEmpty()) {
					Molecule mol = null;
					synchronized (c) {
						for (Iterator i = c.iterator(); i.hasNext();) {
							mol = (Molecule) i.next();
							if (mol instanceof Polypeptide) {
								proteinComboBox.addItem(mol);
							}
						}
					}
					if (proteinID != -1) {
						mol = c.get(proteinID);
						proteinComboBox.setSelectedItem(mol);
					}
					else {
						mol = (Molecule) proteinComboBox.getSelectedItem();
						proteinID = c.indexOf(mol);
					}
					if (mol instanceof Polypeptide) {
						setDNA(new DNA(((Polypeptide) mol).getDNACode()));
					}
					else {
						setDNA(null);
					}
				}
			}

		}

		if (isPreferredSizeSet()) {
			widthField.setValue(getPreferredSize().width);
			heightField.setValue(getPreferredSize().height);
		}
		opaqueComboBox.setSelectedIndex(isTransparent() ? 0 : 1);
		borderComboBox.setSelectedItem(getBorderType());
		colorComboBox.setColor(getBackground());
		okButton.setEnabled(modelComboBox.getItemCount() > 0 && proteinComboBox.getItemCount() > 0);

		dialog.setVisible(true);

	}

	private void adjustBondLength(Atom a) {
		MolecularModel model = (MolecularModel) a.getHostModel();
		List list = model.getBonds().getBonds(a);
		if (list != null && !list.isEmpty()) {
			RadialBond rb = null;
			Atom a1 = null, a2 = null;
			for (Iterator it = list.iterator(); it.hasNext();) {
				rb = (RadialBond) it.next();
				a1 = rb.getAtom1();
				a2 = rb.getAtom2();
				rb.setBondLength(RadialBond.PEPTIDE_BOND_LENGTH_PARAMETER * (a1.getSigma() + a2.getSigma()));
			}
		}
	}

	public void itemStateChanged(ItemEvent e) {
		int i = e.getStateChange();
		if (i == ItemEvent.SELECTED) {
			MolecularModel model = (MolecularModel) page.getComponentPool().get(modelID).getMdContainer().getModel();
			if (model.getMolecules().isEmpty())
				return;
			Molecule m = model.getMolecules().get(proteinID);
			if (m != null)
				m.getAtom(getCurrentBase() / 3).blink();
		}
	}

	public void mutationOccurred(MutationEvent e) {
		MolecularModel model = (MolecularModel) page.getComponentPool().get(modelID).getMdContainer().getModel();
		if (model.getMolecules().isEmpty())
			return;
		Molecule m = model.getMolecules().get(proteinID);
		if (m == null)
			return;
		model.notifyPageComponentListeners(new PageComponentEvent(model, PageComponentEvent.COMPONENT_CHANGED));
		Object src = e.getSource();
		int i = e.getNucleotideIndex();
		Atom a = m.getAtom(i / 3);
		int strand = e.getStrandIndex();
		if (src instanceof SubstitutionMutator) {
			if (strand == DNA.DNA_STRAND_53) {
				char[] q = a.getCodon().toCharArray();
				q[i % 3] = e.getNewNucleotide().getName();
				if (Codon.isStopCodon(q)) {
					setCurrIndex(0);
					List<Integer> list = new ArrayList<Integer>();
					for (int k = m.indexOfAtom(a); k < m.size(); k++) {
						list.add(m.getAtom(k).getIndex());
					}
					((AtomisticView) a.getHostModel().getView()).removeMarkedAtoms(list);
					a.getHostModel().getView().repaint();
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							fillDNA();
						}
					});
				}
				else {
					a.setElement(model.getElement(Codon.expressFromDNA(q).getAbbreviation()));
					a.setCodon(new String(q));
					adjustBondLength(a);
					a.getHostModel().getView().paintImmediately(a.getBounds(10));
				}
			}
			else {
				char[] q = Codon.getComplementaryCode(a.getCodon().toCharArray());
				q[i % 3] = e.getNewNucleotide().getName();
				char[] q1 = Codon.getComplementaryCode(q);
				if (Codon.isStopCodon(q1)) {
					setCurrIndex(0);
					List<Integer> list = new ArrayList<Integer>();
					for (int k = m.indexOfAtom(a); k < m.size(); k++) {
						list.add(m.getAtom(k).getIndex());
					}
					((AtomisticView) a.getHostModel().getView()).removeMarkedAtoms(list);
					a.getHostModel().getView().repaint();
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							fillDNA();
						}
					});
				}
				else {
					a.setElement(model.getElement(Codon.expressFromDNA(q1).getAbbreviation()));
					a.setCodon(new String(q1));
					adjustBondLength(a);
					a.getHostModel().getView().paintImmediately(a.getBounds(10));
				}
			}
		}
		else if (src instanceof InsertionMutator) {
			Molecule mol = model.getMolecules().getMolecule(a);
			String s = getModel().getFullDNA53String();
			String s2 = s.substring(0, s.length() - 1);
			int iStop = ((Polypeptide) mol).setDNACode(s2);
			if (iStop != -1) {
				setCurrIndex(0);
				List<Integer> list = new ArrayList<Integer>();
				for (int k = iStop; k < m.size(); k++) {
					list.add(m.getAtom(k).getIndex());
				}
				((AtomisticView) a.getHostModel().getView()).removeMarkedAtoms(list);
				a.getHostModel().getView().repaint();
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						fillDNA();
					}
				});
			}
			else {
				final DNA dna = new DNA(s2);
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						setDNA(dna);
					}
				});
			}
		}
		else if (src instanceof DeletionMutator) {
			final Molecule mol = model.getMolecules().getMolecule(a);
			String s = getModel().getFullDNA53String();
			double ran = Math.random(); // the last nucleotide is randomly chosen
			if (ran < 0.25)
				s += Codon.A;
			else if (ran < 0.5)
				s += Codon.C;
			else if (ran < 0.75)
				s += Codon.G;
			else s += Codon.T;
			int iStop = ((Polypeptide) mol).setDNACode(s);
			if (iStop != -1) {
				setCurrIndex(0);
				List<Integer> list = new ArrayList<Integer>();
				for (int k = iStop; k < m.size(); k++) {
					list.add(m.getAtom(k).getIndex());
				}
				((AtomisticView) a.getHostModel().getView()).removeMarkedAtoms(list);
				a.getHostModel().getView().repaint();
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						fillDNA();
					}
				});
			}
			else {
				final DNA dna = new DNA(s);
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						setDNA(dna);
					}
				});
			}
		}
	}

	public void modelUpdate(ModelEvent e) {
		Object src = e.getSource();
		if (src instanceof Model) {
			if (e.getID() == ModelEvent.MODEL_INPUT) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						fillDNA();
					}
				});
			}
		}
		else if (src instanceof Atom) {
			MolecularModel model = (MolecularModel) page.getComponentPool().get(modelID).getMdContainer().getModel();
			if (proteinID >= model.getMolecules().size())
				return;
			Molecule m = model.getMolecules().get(proteinID);
			if (!m.contains(src))
				return;
			if (e.getDescription().equals("Selected index")) {
				Object o = e.getCurrentState();
				if (o instanceof Integer) {
					int i = ((Integer) o).intValue();
					setCurrIndex(i * 3);
					char[] code = null;
					if (((Atom) src).isAminoAcid())
						code = ((Atom) src).getCodon().toCharArray();
					if (code != null) {
						for (int k = 0; k < 3; k++) {
							setNucleotide(DNA.DNA_STRAND_53, i * 3 + k, Nucleotide.getNucleotide(code[k]));
						}
						model.notifyPageComponentListeners(new PageComponentEvent(model,
								PageComponentEvent.COMPONENT_CHANGED));
						flashCodon();
					}
				}
			}
		}
		else if (src instanceof Polypeptide) {
			switch (e.getID()) {
			case -1:
				if (e.getDescription().equals("primary structure changed")) {
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							fillDNA();
							repaint();
						}
					});
				}
				break;
			case ModelEvent.MODEL_CHANGED:
				if (e.getCurrentState() != null) {
					if (((Integer) e.getCurrentState()).intValue() == proteinID) {
						EventQueue.invokeLater(new Runnable() {
							public void run() {
								fillDNA();
								repaint();
							}
						});
					}
				}
				break;
			case ModelEvent.COMPONENT_REMOVED:
				if (e.getCurrentState() != null) {
					if (((Integer) e.getCurrentState()).intValue() == proteinID) {
						setDNA(null);
						repaint();
					}
				}
				break;
			}
		}
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
				+ (getBorderType().equals(BorderManager.BORDER_TYPE[0]) ? "" : "<border>" + getBorderType()
						+ "</border>\n")
				+ "<model>"
				+ getModelID()
				+ "</model>\n"
				+ "<selectedIndex>"
				+ proteinID
				+ "</selectedIndex>\n"
				+ (!isOpaque() ? "<opaque>false</opaque>\n" : (getBackground().equals(Color.white) ? "" : "<bgcolor>"
						+ Integer.toString(getBackground().getRGB(), 16) + "</bgcolor>"));
	}

}
