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

package org.concord.modeler.text;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.concord.modeler.Modeler;

class SymbolDialog extends JDialog {

	private Page page;
	private String selectedCharacter;
	private JTable table1, table2, table3;
	private JLabel label;
	private JPopupMenu popupMenu;

	SymbolDialog(Page page0) {

		super(JOptionPane.getFrameForComponent(page0), "Insert symbol: Unicode", false);
		String s = Modeler.getInternationalText("InsertSymbol");
		if (s != null)
			setTitle(s);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		page = page0;

		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (table1 == null)
					return;
				if (table2 == null)
					return;
				if (table3 == null)
					return;
				switch (tabbedPane.getSelectedIndex()) {
				case 0:
					table2.clearSelection();
					table3.clearSelection();
					break;
				case 1:
					table1.clearSelection();
					table3.clearSelection();
					break;
				case 2:
					table1.clearSelection();
					table2.clearSelection();
					break;
				}
			}
		});
		getContentPane().add(tabbedPane, BorderLayout.CENTER);

		createMathOpTable();
		s = Modeler.getInternationalText("MathOperators");
		tabbedPane.addTab(s != null ? s : "Math Operators", createPanel(table1));

		createArrowAndShapeTable();
		s = Modeler.getInternationalText("ArrowsAndShapes");
		tabbedPane.addTab(s != null ? s : "Arrows and Shapes", createPanel(table2));

		createSpecialCharacterTable();
		s = Modeler.getInternationalText("SpecialCharacters");
		tabbedPane.addTab(s != null ? s : "Special Characters", createPanel(table3));

		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(p, BorderLayout.SOUTH);

		s = Modeler.getInternationalText("Insert");
		JButton button = new JButton(s != null ? s : "Insert");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (selectedCharacter != null)
					page.insertString(selectedCharacter);
			}
		});
		p.add(button);

		s = Modeler.getInternationalText("CloseButton");
		button = new JButton(s != null ? s : "Close");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				table1.clearSelection();
				table2.clearSelection();
				table3.clearSelection();
				dispose();
			}
		});
		p.add(button);

		pack();
		setLocationRelativeTo(getOwner());

	}

	private JPanel createPanel(JTable table) {
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		JScrollPane sp = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		sp.getViewport().setPreferredSize(new Dimension(500, 300));
		p.add(sp, BorderLayout.CENTER);
		return p;
	}

	private JTable createTable(String[][] s, String[] c) {
		final JTable table = new JTable();
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setColumnSelectionAllowed(false);
		table.setRowSelectionAllowed(false);
		table.setCellSelectionEnabled(true);
		table.setTableHeader(null);
		table.setDragEnabled(false);
		table.setFont(new Font("Symbols", Font.PLAIN, 15));
		table.setRowHeight(24);
		table.setModel(new DefaultTableModel(s, c) {
			public boolean isCellEditable(int row, int col) {
				return false;
			}
		});
		table.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				react(table, e.getX(), e.getY(), e.getClickCount());
			}
		});
		((DefaultTableCellRenderer) table.getDefaultRenderer(table.getColumnClass(0)))
				.setHorizontalAlignment(JLabel.CENTER);
		return table;
	}

	private void react(JTable table, int x, int y, int clickCount) {
		int row = table.getSelectedRow();
		int col = table.getSelectedColumn();
		if (row != -1 && col != -1) {
			selectedCharacter = (String) table.getValueAt(row, col);
		}
		else {
			selectedCharacter = null;
		}
		if (selectedCharacter != null) {
			if (clickCount >= 2) {
				page.insertString(selectedCharacter);
			}
			else {
				if (popupMenu == null)
					createPopupMenu();
				label.setText(selectedCharacter);
				popupMenu.pack();
				Point t = table.getLocationOnScreen();
				Dimension d = label.getPreferredSize();
				popupMenu.setLocation(t.x + x - d.width / 2, t.y + y - d.height / 2);
				popupMenu.setVisible(true);
			}
		}
	}

	private void createPopupMenu() {
		JPanel p = new JPanel(new BorderLayout());
		label = new JLabel();
		label.setOpaque(true);
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setFont(new Font("Symbols", Font.BOLD, 50));
		label.setBackground(SystemColor.textHighlight);
		label.setForeground(SystemColor.textHighlightText);
		label.setPreferredSize(new Dimension(60, 60));
		label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1), BorderFactory
				.createLineBorder(label.getForeground())));
		label.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				if (selectedCharacter != null)
					page.insertString(selectedCharacter);
			}
		});
		p.add(label, BorderLayout.CENTER);
		popupMenu = new JPopupMenu();
		popupMenu.setBorder(BorderFactory.createEmptyBorder());
		popupMenu.add(p);
		popupMenu.setInvoker(this);
	}

	private void createSpecialCharacterTable() {

		String[][] s = new String[16][16];
		String[] c = new String[16];
		for (int i = 0; i < 16; i++)
			c[i] = "" + i;

		// Greek characters
		int k = 0x0391;
		for (int i = 0; i < 16; i++)
			s[0][i] = ((char) k++) + "";
		s[1][0] = ((char) k++) + "";
		k = 0x03a3;
		for (int i = 1; i < 9; i++)
			s[1][i] = ((char) k++) + "";
		k = 0x03b1;
		for (int i = 9; i < 16; i++)
			s[1][i] = ((char) k++) + "";
		for (int i = 0; i < 16; i++)
			s[2][i] = ((char) k++) + "";
		s[3][0] = ((char) k++) + "";
		s[3][1] = ((char) k++) + "";

		// Latin-1
		k = 0x00a1;
		for (int i = 2; i < 16; i++)
			s[3][i] = ((char) k++) + "";
		for (int j = 4; j < 9; j++) {
			for (int i = 0; i < 16; i++)
				s[j][i] = ((char) k++) + "";
		}
		s[9][0] = ((char) k++) + "";

		// punctuation
		s[9][1] = ((char) 0x2020) + "";
		s[9][2] = ((char) 0x2021) + "";
		s[9][3] = ((char) 0x2022) + "";
		s[9][4] = ((char) 0x2023) + "";
		s[9][5] = ((char) 0x2030) + "";
		s[9][6] = ((char) 0x2031) + "";
		s[9][7] = ((char) 0x2032) + "";
		s[9][8] = ((char) 0x2033) + "";
		s[9][9] = ((char) 0x2034) + "";
		s[9][10] = ((char) 0x203b) + "";
		s[9][11] = ((char) 0x3001) + "";
		s[9][12] = ((char) 0x3002) + "";
		s[9][13] = ((char) 0x3008) + "";
		s[9][14] = ((char) 0x3009) + "";
		s[9][15] = ((char) 0x300a) + "";
		s[10][0] = ((char) 0x300b) + "";
		s[10][1] = ((char) 0x3010) + "";
		s[10][2] = ((char) 0x3011) + "";
		s[10][3] = ((char) 0x3016) + "";
		s[10][4] = ((char) 0x3017) + "";

		// dingbats
		k = 0x2776;
		for (int i = 5; i < 16; i++)
			s[10][i] = ((char) k++) + "";
		for (int i = 0; i < 16; i++)
			s[11][i] = ((char) k++) + "";
		s[12][0] = ((char) 0x2791) + "";
		s[12][1] = ((char) 0x2792) + "";
		s[12][2] = ((char) 0x2793) + "";

		// letterlike symbols
		s[12][3] = ((char) 0x2103) + "";
		s[12][4] = ((char) 0x2109) + "";
		s[12][5] = ((char) 0x210f) + "";
		s[12][6] = ((char) 0x212b) + "";
		s[12][7] = ((char) 0x2116) + "";
		s[12][8] = ((char) 0x2117) + "";
		s[12][9] = ((char) 0x2107) + "";
		s[12][10] = ((char) 0x210a) + "";
		s[12][11] = ((char) 0x210b) + "";
		s[12][12] = ((char) 0x2110) + "";
		s[12][13] = ((char) 0x2111) + "";
		s[12][14] = ((char) 0x2112) + "";
		s[12][15] = ((char) 0x2113) + "";
		s[13][0] = ((char) 0x2118) + "";
		s[13][1] = ((char) 0x211b) + "";
		s[13][2] = ((char) 0x2126) + "";
		s[13][3] = ((char) 0x212c) + "";
		s[13][4] = ((char) 0x2130) + "";
		s[13][5] = ((char) 0x2131) + "";
		s[13][6] = ((char) 0x2133) + "";
		s[13][7] = ((char) 0x2121) + "";
		s[13][8] = ((char) 0x2122) + "";
		s[13][9] = ((char) 0x212e) + "";
		s[13][10] = ((char) 0x212f) + "";

		table3 = createTable(s, c);

	}

	private void createArrowAndShapeTable() {

		String[][] s = new String[16][16];
		String[] c = new String[16];
		int k = 0x2190;
		for (int i = 0; i < 16; i++)
			c[i] = "" + i;
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 16; j++) {
				s[i][j] = ((char) k++) + "";
			}
		}

		// dingbats
		s[5][11] = ((char) 0x2701) + "";
		s[5][12] = ((char) 0x2702) + "";
		s[5][13] = ((char) 0x2703) + "";
		s[5][14] = ((char) 0x2704) + "";
		s[5][15] = ((char) 0x2706) + "";
		s[6][0] = ((char) 0x2707) + "";
		s[6][1] = ((char) 0x2708) + "";
		s[6][2] = ((char) 0x2709) + "";
		k = 0x270c;
		for (int i = 3; i < 16; i++)
			s[6][i] = ((char) k++) + "";
		for (int i = 0; i < 15; i++)
			s[7][i] = ((char) k++) + "";
		s[7][15] = ((char) 0x2729) + "";
		k = 0x272a;
		for (int i = 0; i < 16; i++)
			s[8][i] = ((char) k++) + "";
		for (int i = 0; i < 16; i++)
			s[9][i] = ((char) k++) + "";
		s[9][14] = ((char) 0x274d) + "";
		s[9][15] = ((char) 0x274f) + "";
		s[10][0] = ((char) 0x2750) + "";
		s[10][1] = ((char) 0x2751) + "";
		s[10][2] = ((char) 0x2752) + "";
		s[10][3] = ((char) 0x2756) + "";
		s[10][4] = ((char) 0x2761) + "";
		s[10][5] = ((char) 0x2762) + "";
		s[10][6] = ((char) 0x2763) + "";
		s[10][7] = ((char) 0x2764) + "";
		s[10][8] = ((char) 0x2765) + "";
		s[10][9] = ((char) 0x2766) + "";
		s[10][10] = ((char) 0x2794) + "";
		s[10][11] = ((char) 0x2798) + "";
		s[10][12] = ((char) 0x2799) + "";
		s[10][13] = ((char) 0x279a) + "";
		s[10][14] = ((char) 0x279b) + "";
		s[10][15] = ((char) 0x279c) + "";
		k = 0x279d;
		for (int i = 0; i < 16; i++)
			s[11][i] = ((char) k++) + "";
		for (int i = 0; i < 3; i++)
			s[12][i] = ((char) k++) + "";
		k++;
		for (int i = 3; i < 16; i++)
			s[12][i] = ((char) k++) + "";
		s[13][0] = ((char) 0x27be) + "";
		k = 0x2758;
		for (int i = 1; i < 8; i++)
			s[13][i] = ((char) k++) + "";

		table2 = createTable(s, c);

	}

	private void createMathOpTable() {
		int k = 0x2200;
		String[][] s = new String[16][16];
		String[] c = new String[16];
		for (int i = 0; i < 16; i++) {
			c[i] = "" + i;
			for (int j = 0; j < 16; j++) {
				s[j][i] = ((char) k++) + "";
			}
		}
		table1 = createTable(s, c);
	}

}