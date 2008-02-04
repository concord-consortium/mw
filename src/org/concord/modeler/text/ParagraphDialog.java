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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicBorders;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.concord.modeler.Modeler;
import org.concord.modeler.ui.FloatNumberTextField;
import org.concord.modeler.ui.IntegerTextField;

class ParagraphDialog extends JDialog {

	private final static Border aButtonBorder = new BasicBorders.ButtonBorder(Color.gray, Color.black, Color.white,
			Color.lightGray);
	private final static Border bButtonBorder = new BasicBorders.ButtonBorder(Color.lightGray, Color.white,
			Color.black, Color.gray);

	private int option = JOptionPane.CLOSED_OPTION;
	private MutableAttributeSet attributes;
	private IntegerTextField lineSpacing;
	private IntegerTextField spaceAbove;
	private IntegerTextField spaceBelow;
	private IntegerTextField firstIndent;
	private FloatNumberTextField leftIndent;
	private FloatNumberTextField rightIndent;
	private SmallToggleButton btLeft;
	private SmallToggleButton btCenter;
	private SmallToggleButton btRight;
	private SmallToggleButton btJustified;
	private ParagraphPreview preview;
	private Page page;

	public ParagraphDialog(Page page0) {

		super(JOptionPane.getFrameForComponent(page0), "Paragraph", true);
		String s = Modeler.getInternationalText("ParagraphDialogTitle");
		if (s != null)
			setTitle(s);
		setResizable(false);

		page = page0;

		getContentPane().setLayout(new BorderLayout(5, 5));

		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				option = JOptionPane.OK_OPTION;
				setVisible(false);
			}
		};

		JPanel p = new JPanel(new GridLayout(1, 2, 5, 2));

		JPanel ps = new JPanel(new GridLayout(3, 2, 10, 2));
		s = Modeler.getInternationalText("Space");
		ps.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Space"));
		s = Modeler.getInternationalText("LineSpacing");
		ps.add(new JLabel((s != null ? s : "  Line spacing") + ":"));
		lineSpacing = new IntegerTextField(0, 0, 100);
		lineSpacing.setEnabled(false);
		lineSpacing.addActionListener(okListener);
		ps.add(lineSpacing);
		s = Modeler.getInternationalText("SpaceAbove");
		ps.add(new JLabel((s != null ? s : "  Space above") + ":"));
		spaceAbove = new IntegerTextField(0, 0, 1000);
		spaceAbove.addActionListener(okListener);
		ps.add(spaceAbove);
		s = Modeler.getInternationalText("SpaceBelow");
		ps.add(new JLabel((s != null ? s : "  Space below") + ":"));
		spaceBelow = new IntegerTextField(0, 0, 1000);
		spaceBelow.addActionListener(okListener);
		ps.add(spaceBelow);
		p.add(ps);

		JPanel pi = new JPanel(new GridLayout(3, 2, 10, 2));
		s = Modeler.getInternationalText("Indent");
		pi.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Indent"));
		s = Modeler.getInternationalText("FirstLineIndent");
		pi.add(new JLabel((s != null ? s : "  First indent") + ":"));
		firstIndent = new IntegerTextField(0, 0, 1000);
		firstIndent.setEnabled(false);
		firstIndent.addActionListener(okListener);
		pi.add(firstIndent);
		s = Modeler.getInternationalText("LeftIndent");
		pi.add(new JLabel((s != null ? s : "  Left indent") + ":"));
		leftIndent = new FloatNumberTextField(0, 0, 1000);
		leftIndent.addActionListener(okListener);
		pi.add(leftIndent);
		s = Modeler.getInternationalText("RightIndent");
		pi.add(new JLabel((s != null ? s : "  Right indent") + ":"));
		rightIndent = new FloatNumberTextField(0, 0, 1000);
		rightIndent.addActionListener(okListener);
		pi.add(rightIndent);
		p.add(pi);
		getContentPane().add(p, BorderLayout.NORTH);

		p = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
		s = Modeler.getInternationalText("Alignment");
		p.add(new JLabel((s != null ? s : "Alignment") + ":"));

		ButtonGroup bg = new ButtonGroup();
		ImageIcon img = new ImageIcon(getClass().getResource("images/AlignLeft.gif"));
		btLeft = new SmallToggleButton(false, img, img, "Left");
		bg.add(btLeft);
		p.add(btLeft);
		img = new ImageIcon(getClass().getResource("images/AlignCenter.gif"));
		btCenter = new SmallToggleButton(false, img, img, "Center");
		bg.add(btCenter);
		p.add(btCenter);
		img = new ImageIcon(getClass().getResource("images/AlignRight.gif"));
		btRight = new SmallToggleButton(false, img, img, "Right");
		bg.add(btRight);
		p.add(btRight);
		img = new ImageIcon(getClass().getResource("images/Justify.gif"));
		btJustified = new SmallToggleButton(false, img, img, "Justify");
		btJustified.setEnabled(false);
		bg.add(btJustified);
		p.add(btJustified);
		getContentPane().add(p, BorderLayout.CENTER);

		p = new JPanel(new BorderLayout());
		s = Modeler.getInternationalText("Preview");
		p.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Preview"));
		preview = new ParagraphPreview();
		p.add(preview, BorderLayout.CENTER);
		getContentPane().add(p, BorderLayout.SOUTH);

		JPanel p2 = new JPanel();
		JPanel p1 = new JPanel(new GridLayout(1, 2, 10, 2));

		s = Modeler.getInternationalText("OKButton");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(okListener);
		p1.add(button);

		s = Modeler.getInternationalText("CancelButton");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				option = JOptionPane.CANCEL_OPTION;
				setVisible(false);
			}
		});
		p1.add(button);
		p2.add(p1);
		p.add(p2, BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(getOwner());

		FocusAdapter focusAdapter = new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				setPreviewParameters();
				preview.repaint();
			}
		};
		lineSpacing.addFocusListener(focusAdapter);
		spaceAbove.addFocusListener(focusAdapter);
		spaceBelow.addFocusListener(focusAdapter);
		firstIndent.addFocusListener(focusAdapter);
		leftIndent.addFocusListener(focusAdapter);
		rightIndent.addFocusListener(focusAdapter);

		ActionListener actionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setPreviewParameters();
				preview.repaint();
			}
		};
		btLeft.addActionListener(actionListener);
		btCenter.addActionListener(actionListener);
		btRight.addActionListener(actionListener);
		btJustified.addActionListener(actionListener);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				page.requestFocus();
				ParagraphDialog.this.dispose();
			}

			public void windowActivated(WindowEvent e) {
				if (page.isEditable()) {
					leftIndent.selectAll();
					leftIndent.requestFocus();
				}
			}
		});

	}

	private void setPreviewParameters() {
		preview.iLineSpacing = lineSpacing.getValue();
		preview.iSpaceAbove = spaceAbove.getValue();
		preview.iSpaceBelow = spaceBelow.getValue();
		preview.iFirstIndent = firstIndent.getValue();
		preview.iLeftIndent = leftIndent.getValue();
		preview.iRightIndent = rightIndent.getValue();
	}

	void setAttributes(AttributeSet a) {
		attributes = new SimpleAttributeSet(a);
		lineSpacing.setValue((int) StyleConstants.getLineSpacing(a));
		spaceAbove.setValue((int) StyleConstants.getSpaceAbove(a));
		spaceBelow.setValue((int) StyleConstants.getSpaceBelow(a));
		firstIndent.setValue((int) StyleConstants.getFirstLineIndent(a));
		leftIndent.setValue(StyleConstants.getLeftIndent(a));
		rightIndent.setValue(StyleConstants.getRightIndent(a));
		switch (StyleConstants.getAlignment(a)) {
		case StyleConstants.ALIGN_LEFT:
			btLeft.setSelected(true);
			break;
		case StyleConstants.ALIGN_CENTER:
			btCenter.setSelected(true);
			break;
		case StyleConstants.ALIGN_RIGHT:
			btRight.setSelected(true);
			break;
		case StyleConstants.ALIGN_JUSTIFIED:
			btJustified.setSelected(true);
			break;
		}
		setPreviewParameters();
		preview.repaint();
	}

	AttributeSet getAttributes() {
		if (attributes == null)
			return null;
		StyleConstants.setLineSpacing(attributes, lineSpacing.getValue());
		StyleConstants.setSpaceAbove(attributes, spaceAbove.getValue());
		StyleConstants.setSpaceBelow(attributes, spaceBelow.getValue());
		StyleConstants.setFirstLineIndent(attributes, firstIndent.getValue());
		StyleConstants.setLeftIndent(attributes, leftIndent.getValue());
		StyleConstants.setRightIndent(attributes, rightIndent.getValue());
		StyleConstants.setAlignment(attributes, getAlignment());
		return attributes;
	}

	int getOption() {
		return option;
	}

	private int getAlignment() {
		if (btLeft.isSelected())
			return StyleConstants.ALIGN_LEFT;
		if (btCenter.isSelected())
			return StyleConstants.ALIGN_CENTER;
		if (btRight.isSelected())
			return StyleConstants.ALIGN_RIGHT;
		return StyleConstants.ALIGN_JUSTIFIED;
	}

	class ParagraphPreview extends JPanel {

		private Font fn = new Font("Monospace", Font.PLAIN, 6);
		private String dummy = "abcdefghjklm";
		private float scaleX = 0.25f;
		private float scaleY = 0.25f;
		private Random random = new Random();

		int iLineSpacing, iSpaceAbove, iSpaceBelow;
		float iFirstIndent, iLeftIndent, iRightIndent;

		public ParagraphPreview() {
			setBackground(Color.white);
			setForeground(Color.black);
			setOpaque(true);
			setBorder(new LineBorder(Color.black));
			setPreferredSize(new Dimension(120, 56));
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);

			random.setSeed(1959); // Use same seed every time

			g.setFont(fn);
			FontMetrics fm = g.getFontMetrics();
			int h = fm.getAscent();
			int s = Math.max((int) (iLineSpacing * scaleY), 1);
			int s1 = Math.max((int) (iSpaceAbove * scaleY), 0) + s;
			int s2 = Math.max((int) (iSpaceBelow * scaleY), 0) + s;
			int y = 5 + h;

			int xMarg = 20;
			int x0 = Math.max((int) (iFirstIndent * scaleX) + xMarg, 3);
			int x1 = Math.max((int) (iLeftIndent * scaleX) + xMarg, 3);
			int x2 = Math.max((int) (iRightIndent * scaleX) + xMarg, 3);
			int xm0 = getWidth() - xMarg;
			int xm1 = getWidth() - x2;

			int n = (getHeight() - (2 * h + s1 + s2 - s + 10)) / (h + s);
			n = Math.max(n, 1);

			g.setColor(Color.lightGray);
			int x = xMarg;
			drawLine(g, x, y, xm0, xm0, fm, StyleConstants.ALIGN_LEFT);
			y += h + s1;

			g.setColor(Color.gray);
			int alignment = getAlignment();
			for (int k = 0; k < n; k++) {
				x = (k == 0 ? x0 : x1);
				int xLen = (k == n - 1 ? xm1 / 2 : xm1);
				if (k == n - 1 && alignment == StyleConstants.ALIGN_JUSTIFIED)
					alignment = StyleConstants.ALIGN_LEFT;
				drawLine(g, x, y, xm1, xLen, fm, alignment);
				y += h + s;
			}

			y += s2 - s;
			x = xMarg;
			g.setColor(Color.lightGray);
			drawLine(g, x, y, xm0, xm0, fm, StyleConstants.ALIGN_LEFT);

		}

		private void drawLine(Graphics g, int x, int y, int xMax, int xLen, FontMetrics fm, int alignment) {
			if (y > getHeight() - 3)
				return;
			StringBuffer s = new StringBuffer();
			String str1;
			int xx = x;
			while (true) {
				int m = random.nextInt(10) + 1;
				str1 = dummy.substring(0, m) + " ";
				int len = fm.stringWidth(str1);
				if (xx + len >= xLen)
					break;
				xx += len;
				s.append(str1);
			}
			String str = s.toString();

			switch (alignment) {
			case StyleConstants.ALIGN_LEFT:
				g.drawString(str, x, y);
				break;
			case StyleConstants.ALIGN_CENTER:
				xx = (xMax + x - fm.stringWidth(str)) / 2;
				g.drawString(str, xx, y);
				break;
			case StyleConstants.ALIGN_RIGHT:
				xx = xMax - fm.stringWidth(str);
				g.drawString(str, xx, y);
				break;
			case StyleConstants.ALIGN_JUSTIFIED:
				while (x + fm.stringWidth(str) < xMax)
					str += "a";
				g.drawString(str, x, y);
				break;
			}
		}
	}

	class SmallToggleButton extends JToggleButton implements ItemListener {

		public SmallToggleButton(boolean selected, ImageIcon imgUnselected, ImageIcon imgSelected, String tip) {

			super(imgUnselected, selected);
			setHorizontalAlignment(CENTER);
			setBorderPainted(true);
			setBorder(selected ? bButtonBorder : aButtonBorder);
			setBackground(selected ? Color.white : ParagraphDialog.this.getBackground());
			setMargin(new Insets(1, 1, 1, 1));
			setToolTipText(tip);
			setRequestFocusEnabled(false);
			setSelectedIcon(imgSelected);
			addItemListener(this);
		}

		public float getAlignmentY() {
			return 0.5f;
		}

		public void itemStateChanged(ItemEvent e) {
			setBorder(isSelected() ? bButtonBorder : aButtonBorder);
			setBackground(isSelected() ? Color.white : ParagraphDialog.this.getBackground());
		}

	}

}