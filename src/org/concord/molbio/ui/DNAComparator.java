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

package org.concord.molbio.ui;

import java.util.*;
import java.text.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import org.concord.molbio.engine.*;
import org.concord.molbio.event.*;

public class DNAComparator extends JPanel implements DNAHistoryListener {
	protected JScrollBar scrollBar;
	protected Dimension pPreferredSize = null;

	DNALetters upDNALetters;
	DNALetters downDNALetters;

	DNAScrollerModel dnaModel;

	public DNAComparator(DNAScrollerModel dnaModel) {
		setLayout(new BorderLayout());
		scrollBar = new JScrollBar(JScrollBar.HORIZONTAL) {
			public void paint(Graphics g) {
				super.paint(g);
			}
		};
		add(scrollBar, BorderLayout.SOUTH);
		setDNAModel(dnaModel);
		scrollBar.getModel().addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				Object source = e.getSource();
				if (DNAComparator.this.dnaModel == null)
					return;
				if (source instanceof BoundedRangeModel) {
					int value = ((BoundedRangeModel) source).getValue();
					int offsetLetter = 3 * value;
					if (upDNALetters != null)
						upDNALetters.setOffsetLetter(offsetLetter);
					if (downDNALetters != null)
						downDNALetters.setOffsetLetter(offsetLetter);
					scrollBar.repaint();
					repaint();
				}
			}
		});
	}

	public void setDNAModel(DNAScrollerModel dnaModel) {
		if (this.dnaModel != null)
			this.dnaModel.removeDNAHistoryListener(this);
		this.dnaModel = dnaModel;
		if (dnaModel != null) {
			Stack history = dnaModel.getHistoryStack();
			int nMaxCodons = 0;
			int historySize = history.size();
			for (int i = 0; i < historySize; i++) {
				DNA hDNA = (DNA) history.elementAt(i);
				int nCodon = hDNA.getLength() / 3 + 1;
				if (nCodon > nMaxCodons)
					nMaxCodons = nCodon;

			}
			if (scrollBar != null) {
				scrollBar.setMaximum(nMaxCodons);
				int w = scrollBar.getSize().width - DNALetters.getStrutsWidth();
				scrollBar.setVisibleAmount(w / DNALetterLayer.getRectCodonWidth());
			}
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					populateGUI();
				}
			});
			this.dnaModel.addDNAHistoryListener(this);
		}
	}

	public Dimension getPreferredSize() {
		return (pPreferredSize != null) ? pPreferredSize : new Dimension(400, 110);
	}

	public void setPreferredSize(Dimension d) {
		super.setPreferredSize(d);
		pPreferredSize = d;
	}

	public static void main(String[] args) {
		JFrame f = new JFrame("DNAComparator");
		String mainDNAString = "CCCGGGCGGGTCGGAGTTGGCGTAGATTTTCAAATGGCG";
		DNA scrollerDNA = new DNA(mainDNAString, false);
		DNAScrollerModel scrollerModel = new DNAScrollerModel(scrollerDNA);
		scrollerModel.mutateWithSubstitution(0.3f);
		scrollerModel.mutateWithSubstitution(0.3f);
		scrollerModel.mutateWithSubstitution(0.3f);
		scrollerModel.mutateWithSubstitution(0.3f);
		DNAComparator dnaComparator = new DNAComparator(scrollerModel);
		f.getContentPane().setLayout(new BorderLayout());
		f.getContentPane().add(dnaComparator, BorderLayout.CENTER);
		f.setResizable(false);
		f.pack();
		f.setVisible(true);
		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				System.exit(0);
			}
		});
	}

	public void historyChanged(DNAHistoryEvent evt) {
		if (upDNALetters != null)
			upDNALetters.historyChanged(evt);
		if (downDNALetters != null)
			downDNALetters.historyChanged(evt);
	}

	protected void populateGUI() {
		removeAll();
		if (dnaModel == null)
			return;
		add(scrollBar, BorderLayout.SOUTH);
		Box box = Box.createVerticalBox();
		upDNALetters = new DNALetters(dnaModel, true);
		downDNALetters = new DNALetters(dnaModel, false);
		upDNALetters.setDNAPeer(downDNALetters);
		downDNALetters.setDNAPeer(upDNALetters);
		box.add(upDNALetters);
		Component c = Box.createVerticalStrut(5);
		c.setBackground(Color.gray);
		box.add(c);
		box.add(downDNALetters);

		add(box, BorderLayout.CENTER);
		setBackground(Color.gray);
		revalidate();
		repaint();
	}

}

class DNALetters extends JPanel implements DNAHistoryListener {
	boolean upLayout = false;
	DNAAminoLayer aminoLayer;
	DNALetterLayer letterLayer;
	DNA dna;
	static int widthStrutsLeft = 18;
	static int widthStrutsRight = 16;

	int offsetLetter = 0;

	DNALetters dnaPeer = null;
	JPopupMenu popupMenu;
	DNAScrollerModel dnaModel;
	int selectedDNA = -1;

	static NumberFormat dnaNumerationFomat;

	static {
		dnaNumerationFomat = new DecimalFormat("00");
	}

	DNALetters(DNAScrollerModel dnaModel, boolean upLayout) {
		setLayout(new BorderLayout());
		this.upLayout = upLayout;
		this.dnaModel = dnaModel;
		dna = dnaModel.getDNA();
		selectedDNA = -1;
		Stack history = dnaModel.getHistoryStack();
		if (history != null) {
			int historySize = history.size();
			if (historySize > 0) {
				dna = (DNA) history.elementAt(0);
				selectedDNA = 0;
			}
		}
		Box boxH = Box.createHorizontalBox();
		Box boxV = Box.createVerticalBox();

		setBackground(Color.lightGray);
		letterLayer = new DNALetterLayer(this, dna);
		aminoLayer = new DNAAminoLayer(dna, letterLayer);
		if (upLayout) {
			add(aminoLayer, BorderLayout.SOUTH);
		}
		else {
			add(aminoLayer, BorderLayout.NORTH);
		}

		final ImageIcon iconArrowToLeft = new ImageIcon(DNAComparator.class
				.getResource("/org/concord/molbio/ui/images/ArrowToLeft.gif"));
		final ImageIcon iconArrowToRight = new ImageIcon(DNAComparator.class
				.getResource("/org/concord/molbio/ui/images/ArrowToRight.gif"));
		final ImageIcon iconArrowToCenter = new ImageIcon(DNAComparator.class
				.getResource("/org/concord/molbio/ui/images/ArrowToCenter.gif"));
		JLabel labelToLeft = new JLabel(iconArrowToLeft) {
			public Dimension getPreferredSize() {
				return new Dimension(widthStrutsLeft, iconArrowToLeft.getIconHeight());
			}
		};
		JLabel labelToRight = new JLabel(iconArrowToRight) {
			public Dimension getPreferredSize() {
				return new Dimension(widthStrutsLeft, iconArrowToRight.getIconHeight());
			}
		};
		JLabel labelToCenter = new JLabel(iconArrowToCenter) {
			public Dimension getPreferredSize() {
				return new Dimension(widthStrutsLeft, iconArrowToCenter.getIconHeight());
			}
		};
		Box menuBox = Box.createVerticalBox();
		menuBox.add(labelToLeft);
		menuBox.add(labelToCenter);
		menuBox.add(labelToRight);
		boxH.add(menuBox);

		if (this.upLayout) {
			boxV.add(letterLayer);
			boxV.add(aminoLayer);
		}
		else {
			boxV.add(aminoLayer);
			boxV.add(letterLayer);
		}
		boxH.add(boxV);

		final ImageIcon menuArrow = new ImageIcon(DNAComparator.class
				.getResource("/org/concord/molbio/ui/images/MenuArrow.gif"));
		final JLabel label = new JLabel(menuArrow) {
			public Dimension getPreferredSize() {
				return new Dimension(widthStrutsRight, menuArrow.getIconHeight());
			}
		};
		boxH.add(label);
		label.addMouseListener(new MouseAdapter() {
			public void mouseReleased(final MouseEvent evt) {
				if (popupMenu != null) {
					if (selectedDNA >= 0 && selectedDNA < popupMenu.getComponentCount()) {
						Component c = popupMenu.getComponent(selectedDNA);
						for (int i = 0; i < popupMenu.getComponentCount(); i++) {
							Component cc = popupMenu.getComponent(i);
							if (cc instanceof JMenuItem)
								((JMenuItem) cc).setSelected(false);
							cc.setForeground(Color.black);
						}
						if (c instanceof JMenuItem)
							((JMenuItem) c).setSelected(true);
						c.setForeground(Color.blue);
					}
					popupMenu.show(label, evt.getX(), evt.getY());
				}
			}
		});

		labelToLeft.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent evt) {
				letterLayer.setHorizontalShift(letterLayer.getHorizontalShift() - 1);
				repaint();
				if (dnaPeer != null)
					dnaPeer.repaint();
			}
		});

		labelToRight.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent evt) {
				letterLayer.setHorizontalShift(letterLayer.getHorizontalShift() + 1);
				repaint();
				if (dnaPeer != null)
					dnaPeer.repaint();
			}
		});

		labelToCenter.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent evt) {
				letterLayer.setHorizontalShift(0);
				repaint();
				if (dnaPeer != null)
					dnaPeer.repaint();
			}
		});

		add(boxH, BorderLayout.CENTER);
		initPopupMenu();
	}

	public void historyChanged(DNAHistoryEvent evt) {
		if (dnaModel == null)
			return;
		initPopupMenu();
		dna = dnaModel.getDNA();
		selectedDNA = -1;
		Stack history = dnaModel.getHistoryStack();
		if (history != null) {
			int historySize = history.size();
			if (historySize > 0) {
				dna = (DNA) history.elementAt(0);
				selectedDNA = 0;
			}
		}
		if (aminoLayer != null)
			aminoLayer.setDNA(dna);
		if (letterLayer != null)
			letterLayer.setDNA(dna);
		repaint();
	}

	protected void initPopupMenu() {
		if (popupMenu == null) {
			popupMenu = new JPopupMenu();
		}
		else {
			popupMenu.removeAll();
		}
		if (dnaModel == null)
			return;
		Stack history = dnaModel.getHistoryStack();
		if (history == null)
			return;
		int historySize = history.size();
		int indexDNA = 1;
		for (int i = 0; i < historySize; i++) {
			DNA hDNA = (DNA) history.elementAt(i);

			String str = hDNA.getCodingRegionAsString().toUpperCase();
			StringBuffer sb = new StringBuffer(dnaNumerationFomat.format(indexDNA));
			sb.append("  ");
			if (str.length() <= 5) {
				sb.append(str);
			}
			else {
				sb.append(str.substring(0, 6));
				sb.append("...");
			}
			JCheckBoxMenuItem mi = new JCheckBoxMenuItem(sb.toString());
			popupMenu.add(mi);
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					int dnaIndex = popupMenu.getComponentIndex((Component) evt.getSource());
					Stack stackHistory = dnaModel.getHistoryStack();
					if (stackHistory == null)
						return;
					if (dnaIndex < 0 || dnaIndex >= stackHistory.size())
						return;
					dna = (DNA) stackHistory.elementAt(dnaIndex);
					selectedDNA = dnaIndex;
					aminoLayer.setDNA(dna);
					letterLayer.setDNA(dna);
					repaint();
					if (dnaPeer != null) {
						dnaPeer.getLetterLayer().setHorizontalShift(0);
						dnaPeer.repaint();
					}
				}
			});
			indexDNA++;
		}
	}

	public void setDNAPeer(DNALetters dnaPeer) {
		this.dnaPeer = dnaPeer;
	}

	public DNALetters getDNAPeer() {
		return dnaPeer;
	}

	public DNALetterLayer getLetterLayer() {
		return letterLayer;
	}

	public void setOffsetLetter(int offsetLetter) {
		this.offsetLetter = 3 * (offsetLetter / 3);
		if (letterLayer != null)
			letterLayer.setOffsetLetter(this.offsetLetter);
	}

	public Dimension getPreferredSize() {
		int needW = getStrutsWidth();
		if (letterLayer != null) {
			Rectangle[] rs = letterLayer.rLetters;
			if (rs != null && rs.length > 0) {
				needW += rs[rs.length - 1].x + rs[rs.length - 1].width - rs[0].x;
			}
		}
		Dimension d = new Dimension(Math.max(400, needW), 40);
		aminoLayer.setPreferredSize(d);
		return d;
	}

	public static int getStrutsWidth() {
		return (widthStrutsLeft + widthStrutsRight);
	}

}

class DNALetterLayer extends JPanel {
	public static final Color DEFAULT_COLOR = new Color(200, 200, 255);
	char[] dnaLetters;
	Rectangle[] rLetters;
	static Font codonFont = new Font("Dialog", Font.BOLD, 12);
	int[] letterWidth;
	DNA dna;
	int offsetLetter = 0;
	int horizontalShift = 0;
	static int rectangleWidth = codonFont.getSize() + 2;
	DNALetters owner = null;

	DNALetterLayer(DNALetters owner, DNA dna) {
		this.owner = owner;
		this.dna = dna;
		String mainDNAString = dna.getFragmentAsString();
		dnaLetters = mainDNAString.toCharArray();
		setOpaque(false);
		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				createRectangles();
			}
		});
		// System.out.println(dnaLetters);
	}

	public void setDNA(DNA dna) {
		this.dna = dna;
		String mainDNAString = dna.getFragmentAsString();
		dnaLetters = mainDNAString.toCharArray();
		createRectangles();
		shiftRectangles();
	}

	public void paintComponent(Graphics g) {
		drawLetters(g);
	}

	public void createRectangles() {
		createRectangles(null);
	}

	public void setOffsetLetter(int offsetLetter) {
		this.offsetLetter = offsetLetter;
		shiftRectangles();
	}

	public static int getRectangleWidth() {
		return rectangleWidth;
	}

	public char getLetter(int i) throws IllegalArgumentException {
		if (i < 0 || i >= dnaLetters.length)
			throw new IllegalArgumentException("DNALetterLayer getLetter " + i);
		return dnaLetters[i];
	}

	public void shiftRectangles() {
		if (rLetters == null)
			return;
		int x0 = getRectShift();
		for (int i = 0; i < rLetters.length; i++) {
			rLetters[i].translate(x0 - rLetters[i].x, 0);
			x0 += (getRectangleWidth() - 1);
		}
	}

	public static int getRectCodonWidth() {
		return 3 * (getRectangleWidth() - 1);
	}

	public void createRectangles(Graphics g) {
		horizontalShift = 0;
		if (dnaLetters == null)
			return;
		Graphics g1 = (g == null) ? getGraphics() : g;
		if (g1 == null)
			return;
		rLetters = new Rectangle[dnaLetters.length];
		FontMetrics fm = g1.getFontMetrics(codonFont);

		letterWidth = fm.getWidths();

		Rectangle2D rm = fm.getMaxCharBounds(g);

		Rectangle r = new Rectangle(0, 0, rectangleWidth, Math.max((int) rm.getHeight() + 2, getSize().height - 2));
		int x0 = 0;
		int y = getSize().height / 2;
		for (int i = 0; i < rLetters.length; i++) {
			r.translate(x0 - r.x, y - (r.y + r.height / 2));
			rLetters[i] = new Rectangle(r);
			x0 = r.x + r.width - 1;
		}

		if (g == null)
			g1.dispose();
	}

	public int getRectShift() {
		int maxLetter = offsetLetter;
		int shift = (getRectangleWidth() - 1) * (getHorizontalShift() - maxLetter);
		return shift;
	}

	public Color getRectColor(int index) {
		if (owner == null || owner.getDNAPeer() == null) {
			return DEFAULT_COLOR;
		}
		DNALetterLayer peerLayer = owner.getDNAPeer().getLetterLayer();
		if (peerLayer == null)
			return DEFAULT_COLOR;
		try {
			char myLetter = getLetter(index);
			char peerLetter = peerLayer.getLetter(index + horizontalShift - peerLayer.getHorizontalShift());
			if (myLetter == peerLetter) {
				Color rectColor = null;
				switch (myLetter) {
				case 'A':
					rectColor = Nucleotide.A_COLOR;
					break;
				case 'C':
					rectColor = Nucleotide.C_COLOR;
					break;
				case 'T':
					rectColor = Nucleotide.T_COLOR;
					break;
				case 'G':
					rectColor = Nucleotide.G_COLOR;
					break;
				}
				if (rectColor != null)
					return rectColor;
			}
		}
		catch (Throwable t) {
		}
		return Color.red;
	}

	public void drawLetters(Graphics g) {
		if (rLetters == null)
			createRectangles(g);
		if (rLetters == null)
			return;
		Font oldFont = g.getFont();
		Shape oldClip = g.getClip();
		Color oldColor = g.getColor();
		g.setFont(codonFont);
		Rectangle clipRect = new Rectangle(0, 0, getSize().width, getSize().height);
		FontMetrics fm = g.getFontMetrics();
		int hLetter = fm.getHeight() - fm.getDescent();
		for (int i = 0; i < rLetters.length; i++) {
			Rectangle r = rLetters[i];
			g.setClip(r.intersection(clipRect));
			g.setColor(getRectColor(i));
			g.fillRect(r.x, r.y, r.width, r.height);
			g.setColor(Color.darkGray);
			g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
			if (dnaLetters == null || i >= dnaLetters.length)
				continue;
			g.setColor(Color.black);
			g.drawChars(dnaLetters, i, 1, r.x + r.width / 2 - letterWidth[dnaLetters[i]] / 2, r.y + r.height / 2
					+ hLetter / 2);
		}
		g.setColor(oldColor);
		g.setClip(oldClip);
		g.setFont(oldFont);

	}

	public void setHorizontalShift(int horizontalShift) {
		this.horizontalShift = horizontalShift;
		shiftRectangles();
	}

	public int getHorizontalShift() {
		return horizontalShift;
	}

}

class DNAAminoLayer extends JPanel {
	DNALetterLayer letterLayer;
	Aminoacid[] aminoacids;
	int firstNucleotideIndex = 0;

	DNAAminoLayer(DNA dna, DNALetterLayer letterLayer) {
		setBackground(Color.lightGray);
		this.letterLayer = letterLayer;
		RNA rna = dna.transcript(DNA.DNA_STRAND_COMPL);
		Protein protein = rna.translate();
		if (protein != null)
			aminoacids = protein.getAminoacids();
		if (dna.startWithPromoter())
			firstNucleotideIndex += DNA.PROMOTER_LENGTH;
	}

	public void setDNA(DNA dna) {
		firstNucleotideIndex = 0;
		RNA rna = dna.transcript(DNA.DNA_STRAND_COMPL);
		Protein protein = rna.translate();
		if (protein != null)
			aminoacids = protein.getAminoacids();
		if (dna.startWithPromoter())
			firstNucleotideIndex += DNA.PROMOTER_LENGTH;
	}

	public Dimension getPreferredSize() {
		Dimension superD = new Dimension(super.getPreferredSize());
		if (superD.height > 14)
			superD.height = 14;
		return superD;
	}

	public Dimension getMaximumSize() {
		Dimension superD = new Dimension(super.getMaximumSize());
		if (superD.height > 14)
			superD.height = 14;
		return superD;
	}

	String getPeerAminoacid(int index) {
		if (letterLayer == null)
			return null;
		if (letterLayer.owner == null)
			return null;
		DNALetters dnal = letterLayer.owner.getDNAPeer();
		if (dnal == null)
			return null;
		if (dnal.aminoLayer == null)
			return null;
		if (dnal.aminoLayer.aminoacids == null)
			return null;
		if (index < 0 || index >= dnal.aminoLayer.aminoacids.length)
			return null;
		Aminoacid amino = dnal.aminoLayer.aminoacids[index];
		if (amino == null)
			return null;
		return amino.getAbbreviation();
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Color lineColor = Color.darkGray;
		if (letterLayer == null || aminoacids == null)
			return;
		Rectangle[] rs = letterLayer.rLetters;
		if (rs == null)
			return;
		Font oldFont = g.getFont();
		Color oldColor = g.getColor();
		g.setFont(DNALetterLayer.codonFont);
		FontMetrics fm = g.getFontMetrics();
		int hLetter = fm.getHeight() - fm.getDescent();

		for (int i = 0; i < aminoacids.length; i++) {
			if (i * 3 < firstNucleotideIndex)
				continue;
			Point p = SwingUtilities.convertPoint(letterLayer, rs[3 * i].x, 0, this);
			g.setColor(lineColor);
			g.drawLine(p.x, 0, p.x, getSize().height);
			g.setColor(Color.black);
			String str = aminoacids[i].getAbbreviation();
			String strPeer = getPeerAminoacid(i);
			if (str != null && strPeer != null && !strPeer.equalsIgnoreCase(str)) {
				g.setColor(Color.red);
			}
			g.drawString(str, p.x + 3 * rs[3 * i].width / 2
					- (int) Math.round(fm.getStringBounds(str, g).getWidth() / 2), getSize().height / 2 + hLetter / 2
					- 1);
		}
		int rIndex = 3 * aminoacids.length - 1;
		Point p = SwingUtilities.convertPoint(letterLayer, rs[rIndex].x + rs[rIndex].width, 0, this);
		g.setColor(lineColor);
		g.drawLine(p.x, 0, p.x, getSize().height);
		g.setColor(oldColor);
		g.setFont(oldFont);
	}
}
