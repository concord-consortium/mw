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
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.concord.modeler.event.ModelEvent;
import org.concord.modeler.ui.RestrictedTextField;
import org.concord.molbio.engine.Aminoacid;
import org.concord.mw2d.models.AngularBond;
import org.concord.mw2d.models.AngularBondCollection;
import org.concord.mw2d.models.Atom;
import org.concord.mw2d.models.DNAStrand;
import org.concord.mw2d.models.Element;
import org.concord.mw2d.models.MolecularModel;
import org.concord.mw2d.models.Molecule;
import org.concord.mw2d.models.MoleculeCollection;
import org.concord.mw2d.models.Polypeptide;
import org.concord.mw2d.models.RadialBond;
import org.concord.mw2d.models.RadialBondCollection;

class MoleculePropertiesPanel extends PropertiesPanel {

	private final static char[] AMINO_ACID_CHARS = new char[] { 'A', 'R', 'N', 'D', 'C', 'Q', 'E', 'G', 'H', 'I', 'L',
			'K', 'M', 'F', 'P', 'S', 'T', 'W', 'Y', 'V' };
	private final static char[] NUCLEOTIDE_CHARS = new char[] { 'A', 'C', 'G', 'T', 'U' };

	private JDialog dialog;
	private RestrictedTextField seqField;
	private RestrictedTextField dnaField;

	RadialBondCollection rbc;
	AngularBondCollection abc;
	MoleculeCollection mc;
	AtomisticView av;

	void destroy() {
		rbc = null;
		abc = null;
		mc = null;
		av = null;
		if (dialog != null)
			dialog.dispose();
	}

	MoleculePropertiesPanel(LayoutManager layout) {
		super(layout);
	}

	MoleculePropertiesPanel(final Molecule mol) {

		super(new BorderLayout(5, 5));

		final Point2D com = mol.getCenterOfMass2D();
		final float rg = mol.getRadiusOfGyration(com);
		rbc = ((MolecularModel) mol.getHostModel()).getBonds();
		abc = ((MolecularModel) mol.getHostModel()).getBends();
		mc = ((MolecularModel) mol.getHostModel()).getMolecules();
		av = (AtomisticView) mol.getHostModel().getView();

		if (mol instanceof DNAStrand) {
			dnaField = new RestrictedTextField(NUCLEOTIDE_CHARS, mol.size());
			dnaField.setPreferredSize(new Dimension(100, 20));
			dnaField.setText(mol.toString());
			dnaField.setAction(new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					boolean b = true;
					if (!((DNAStrand) mol).getNucleotideCode().equals(dnaField.getText().trim())) {
						b = setDNA((DNAStrand) mol);
					}
					if (b)
						destroy();
				}
			});
		}

		else if (mol instanceof Polypeptide) {
			seqField = new RestrictedTextField(AMINO_ACID_CHARS, mol.size());
			seqField.setPreferredSize(new Dimension(100, 20));
			seqField.setText(mol.toString());
			seqField.setAction(new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					boolean b = true;
					if (!((Polypeptide) mol).getAminoAcidCode(true).equalsIgnoreCase(seqField.getText().trim())) {
						b = setProtein((Polypeptide) mol);
					}
					if (b)
						destroy();
				}
			});
		}

		/* lay out components */

		JTabbedPane tabbedPane = new JTabbedPane();
		add(tabbedPane, BorderLayout.CENTER);

		JPanel p = new JPanel(new BorderLayout());
		String s = MDView.getInternationalText("GeneralTab");
		tabbedPane.add(s != null ? s : "General", p);

		JPanel panel = new JPanel(new SpringLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		p.add(panel, BorderLayout.NORTH);

		s = MDView.getInternationalText("ObjectTypeLabel");
		panel.add(new JLabel(s != null ? s : "Object Type"));
		s = MDView.getInternationalText("Molecule");
		String str = s != null ? s : "Molecule";
		if (mol instanceof Polypeptide) {
			s = MDView.getInternationalText("Polypeptide");
			str = s != null ? s : "Polypeptide";
		}
		else if (mol instanceof DNAStrand) {
			s = MDView.getInternationalText("DNAStrand");
			str = s != null ? s : "DNA Strand";
		}
		panel.add(createLabel(str));
		panel.add(new JPanel());

		s = MDView.getInternationalText("IndexLabel");
		panel.add(new JLabel(s != null ? s : "Index"));
		panel.add(createLabel(mc.indexOf(mol) + ""));
		panel.add(new JPanel());

		s = MDView.getInternationalText("NumberOfAtomsLabel");
		str = s != null ? s : "Number of Atoms";
		if (mol instanceof Polypeptide) {
			s = MDView.getInternationalText("NumberOfResiduesLabel");
			str = s != null ? s : "Number of Residues";
		}
		else if (mol instanceof DNAStrand) {
			s = MDView.getInternationalText("NumberOfNucleotidesLabel");
			str = s != null ? s : "Number of Nucleotides";
		}
		panel.add(new JLabel(str));
		panel.add(createLabel(mol instanceof DNAStrand ? mol.size() / 2 : mol.size()));
		panel.add(new JPanel());

		if (mol instanceof Polypeptide) {
			s = MDView.getInternationalText("AminoAcidSequenceLabel");
			panel.add(new JLabel(s != null ? s : "Amino acid sequence"));
			panel.add(seqField);
			s = MDView.getInternationalText("Copy");
			JButton button = new JButton(s != null ? s : "Copy");
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
							new StringSelection(((Polypeptide) mol).getAminoAcidCode(false)), null);
				}
			});
			button.setToolTipText("Copy the sequence of amino acids in three-letter code");
			panel.add(button);
		}
		else if (mol instanceof DNAStrand) {
			s = MDView.getInternationalText("NucleotideSequenceLabel");
			panel.add(new JLabel(s != null ? s : "Nucleotide sequence"));
			panel.add(dnaField);
			panel.add(new JPanel());
		}

		panel.add(new JLabel("<html><em>X</em><sub>center of mass</sub></html>"));
		panel.add(createLabel(DECIMAL_FORMAT.format(com.getX() * 0.1)));
		panel.add(createSmallerFontLabel("\u00c5"));

		panel.add(new JLabel("<html><em>Y</em><sub>center of mass</sub></html>"));
		panel.add(createLabel(DECIMAL_FORMAT.format(com.getY() * 0.1)));
		panel.add(createSmallerFontLabel("\u00c5"));

		panel.add(new JLabel("Radius of Gyration"));
		panel.add(createLabel(DECIMAL_FORMAT.format(rg * 0.1)));
		panel.add(createSmallerFontLabel("\u00c5"));

		boolean b = mol instanceof DNAStrand || mol instanceof Polypeptide;
		makeCompactGrid(panel, b ? 7 : 6, 3, 5, 5, 10, 2);

		panel = createBondPanel(mol);
		if (panel != null) {
			s = MDView.getInternationalText("RadialBond");
			tabbedPane.add(s != null ? s : "Radial Bonds", panel);
		}

		panel = createBendPanel(mol);
		if (panel != null) {
			s = MDView.getInternationalText("AngularBond");
			tabbedPane.add(s != null ? s : "Angular Bonds", panel);
		}

		panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		s = MDView.getInternationalText("CloseButton");
		JButton button = new JButton(s != null ? s : "Close");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean b = true;
				if (mol instanceof DNAStrand) {
					if (!((DNAStrand) mol).getNucleotideCode().equals(dnaField.getText().trim())) {
						b = setDNA((DNAStrand) mol);
					}
				}
				else if (mol instanceof Polypeptide) {
					if (!((Polypeptide) mol).getDNACode().equals(seqField.getText().trim())) {
						b = setProtein((Polypeptide) mol);
					}
				}
				if (b)
					destroy();
			}
		});
		panel.add(button);

		add(panel, BorderLayout.SOUTH);

	}

	private boolean setProtein(final Polypeptide mol) {
		String s = seqField.getText();
		if (s.trim().equals("") || s.length() != mol.size()) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(dialog, "The length of sequence must be " + mol.size());
					seqField.setText(mol.toString());
					seqField.requestFocusInWindow();
				}
			});
			return false;
		}
		char[] c = s.toCharArray();
		Atom a = null;
		Element elem = null;
		for (int i = 0; i < mol.size(); i++) {
			a = mol.getAtom(i);
			elem = ((MolecularModel) mol.getHostModel()).getElement(Aminoacid.getBySymbol(c[i]).getAbbreviation());
			a.setElement(elem);
		}
		mol.adjustPeptideBondLengths();
		mol.getHostModel().getView().repaint();
		mol.getHostModel().notifyModelListeners(new ModelEvent(mol, "primary structure changed"));
		mol.getHostModel().notifyChange();
		return true;
	}

	private boolean setDNA(final DNAStrand mol) {
		final int n = mol.size() / 2;
		String s = dnaField.getText();
		if (s.trim().equals("") || s.length() != n) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(dialog, "The length of sequence must be " + n);
					dnaField.setText(mol.toString());
					dnaField.requestFocusInWindow();
				}
			});
			return false;
		}
		mol.setNucleotideCode(s);
		mol.getHostModel().getView().repaint();
		mol.getHostModel().notifyModelListeners(new ModelEvent(mol, "nucleotide code changed"));
		mol.getHostModel().notifyChange();
		return true;
	}

	/* set second panel for radial bonds */
	JPanel createBondPanel(Molecule mol) {

		List<RadialBond> bondList = mol.getBonds();

		if (bondList == null || bondList.isEmpty())
			return null;

		final JTable table = new JTable();
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane scroller = new JScrollPane(table);
		scroller.setPreferredSize(new Dimension(160, 200));

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		panel.add(scroller, BorderLayout.NORTH);

		Object[][] data = new Object[bondList.size()][5];
		int i = 0;
		for (RadialBond rBond : bondList) {
			data[i][0] = rbc.indexOf(rBond);
			data[i][1] = rBond.getAtom1().getIndex();
			data[i][2] = rBond.getAtom2().getIndex();
			data[i][3] = rBond.getBondLength() * 0.1;
			data[i][4] = rBond.getBondStrength();
			i++;
		}
		DefaultTableModel tm = new DefaultTableModel(data, new String[] { "#", "I", "J", "d0", "k" }) {
			public Class<?> getColumnClass(int columnIndex) {
				return getValueAt(0, columnIndex).getClass();
			}

			public boolean isCellEditable(int row, int col) {
				return col < 3 ? false : true;
			}
		};
		tm.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				DefaultTableModel t = (DefaultTableModel) e.getSource();
				int col = e.getColumn();
				int row = e.getFirstRow();
				RadialBond rBond = rbc.get((Integer) t.getValueAt(row, 0));
				if (rBond == null)
					return;
				boolean changed = false;
				if (col == 3) {
					double v = (Double) t.getValueAt(row, col);
					if (Math.abs(rBond.getBondLength() - v * 10) > ZERO) {
						rBond.setBondLength(v * 10);
						changed = true;
					}
				}
				else if (col == 4) {
					double v = (Double) t.getValueAt(row, col);
					if (Math.abs(rBond.getBondStrength() - v) > ZERO) {
						rBond.setBondStrength(v);
						changed = true;
					}
				}
				if (changed)
					rBond.getHostModel().notifyChange();
			}
		});
		table.setModel(tm);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting())
					return;
				ListSelectionModel sm = (ListSelectionModel) e.getSource();
				if (!sm.isSelectionEmpty()) {
					int row = sm.getMinSelectionIndex();
					RadialBond rBond = rbc.get((Integer) table.getValueAt(row, 0));
					rbc.select(rBond);
					av.repaint();
				}
			}
		});
		TableColumn tc = table.getColumnModel().getColumn(0);
		tc.setPreferredWidth(30);
		tc.setMaxWidth(30);
		tc.setMinWidth(30);

		return panel;

	}

	/* set third panel for angular bonds */
	JPanel createBendPanel(Molecule mol) {

		List<AngularBond> bendList = mol.getBends();

		if (bendList == null || bendList.isEmpty())
			return null;

		final JTable table = new JTable();
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane scroller = new JScrollPane(table);
		scroller.setPreferredSize(new Dimension(160, 200));

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.add(scroller, BorderLayout.NORTH);

		Object[][] data = new Object[bendList.size()][6];
		int i = 0;
		for (AngularBond aBond : bendList) {
			data[i][0] = abc.indexOf(aBond);
			data[i][1] = aBond.getAtom1().getIndex();
			data[i][2] = aBond.getAtom2().getIndex();
			data[i][3] = aBond.getAtom3().getIndex();
			data[i][4] = Math.round((float) Math.toDegrees(aBond.getBondAngle()));
			data[i][5] = aBond.getBondStrength();
			i++;
		}
		DefaultTableModel tm = new DefaultTableModel(data, new String[] { "#", "I", "J", "K", "a0", "k" }) {
			public Class<?> getColumnClass(int columnIndex) {
				return getValueAt(0, columnIndex).getClass();
			}

			public boolean isCellEditable(int row, int col) {
				return col < 4 ? false : true;
			}
		};
		tm.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				DefaultTableModel t = (DefaultTableModel) e.getSource();
				int col = e.getColumn();
				int row = e.getFirstRow();
				AngularBond aBond = abc.get((Integer) t.getValueAt(row, 0));
				if (aBond == null)
					return;
				boolean changed = false;
				if (col == 4) {
					double v = Math.toRadians((Integer) t.getValueAt(row, col));
					if (Math.abs(v - aBond.getBondAngle()) > ZERO) {
						aBond.setBondAngle(v);
						changed = true;
					}
				}
				else if (col == 5) {
					double v = (Double) t.getValueAt(row, col);
					if (Math.abs(v - aBond.getBondStrength()) > ZERO) {
						aBond.setBondStrength(v);
						changed = true;
					}
				}
				if (changed)
					aBond.getHostModel().notifyChange();
			}
		});
		table.setModel(tm);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting())
					return;
				ListSelectionModel sm = (ListSelectionModel) e.getSource();
				if (!sm.isSelectionEmpty()) {
					int row = sm.getMinSelectionIndex();
					AngularBond aBond = abc.get((Integer) table.getValueAt(row, 0));
					abc.select(aBond);
					av.repaint();
				}
			}
		});
		TableColumn tc = table.getColumnModel().getColumn(0);
		tc.setPreferredWidth(30);
		tc.setMaxWidth(30);
		tc.setMinWidth(30);

		return panel;

	}

	void setDialog(JDialog d) {
		dialog = d;
	}

	void windowActivated() {
	}

}