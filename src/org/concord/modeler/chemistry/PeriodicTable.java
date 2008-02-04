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

package org.concord.modeler.chemistry;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.ItemSelectable;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class PeriodicTable extends JPanel implements ItemSelectable {

	private final static Insets MARGIN = new Insets(0, 0, 0, 0);
	private final static Font font = new Font(null, Font.PLAIN, 12);
	private final static Font smallFont = new Font(null, Font.PLAIN, 11);
	private final static Color RARE_GAS_COLOR = new Color(0xf0c0c1);
	private final static Color METAL_COLOR = new Color(0xf9c282);
	private final static Color METALLOID_COLOR = new Color(0xc7fdcb);
	private final static Color NON_METAL_COLOR = new Color(0x7effff);
	private final static Color TRANSITION_METAL_COLOR = new Color(0xffff7a);
	private static final String[] labels = { "IA", "IIA", "IIIA", "IVA", "VA", "VIA", "VIIA", "---------VIII---------",
			"IB", "IIB", "IIIB", "IVB", "VB", "VIB", "VIIB", "VIIIB" };
	private static NumberFormat format;
	private static ResourceBundle bundle;
	private static boolean isUSLocale;

	private JButton[] button;
	private JButton lastSelectedButton;
	private MidiChannel channel;
	private Synthesizer synthesizer;
	private boolean multipleSelectionAllowed;
	private boolean mute = true;
	private Vector<Integer> selectedObjects = new Vector<Integer>();
	private Vector<ItemListener> itemListeners;
	private JTextField tf_name, tf_number, tf_weight, tf_shell, tf_structure, tf_covrad, tf_atorad, tf_electropaul;

	public PeriodicTable() {

		super(null);

		if (bundle == null) {
			isUSLocale = Locale.getDefault().equals(Locale.US);
			try {
				bundle = ResourceBundle.getBundle("org.concord.modeler.chemistry.resources.PeriodicTable", Locale
						.getDefault());
			}
			catch (MissingResourceException e) {
			}
		}

		setBackground(Color.white);
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		int x0 = 10, y0 = 30, w = 40, h = 40;
		setPreferredSize(new Dimension(20 + 18 * w, 10 * h + 20 + 4 * 30));
		setMaximumSize(getPreferredSize());

		if (format == null) {
			format = NumberFormat.getNumberInstance();
			format.setMaximumFractionDigits(5);
		}

		initTextFields();

		// table buttons

		String s = null;
		button = new JButton[getMaxElementIndex()];
		ButtonKeyListener bKeyListener = new ButtonKeyListener(this);
		for (int n = 0; n < getMaxElementIndex(); n++) {
			s = getInternationalText(ElementData.elements[n]);
			if (s != null) {
				button[n] = new JButton("<html><center>" + (n + 1) + "<br><font size=\"3\">" + s + "</font> "
						+ ElementData.elements[n] + "</center></html>");
			}
			else {
				button[n] = new JButton("<html><center>" + (n + 1) + "<br>" + ElementData.elements[n]
						+ "</center></html>");
			}
			s = getInternationalText(ElementData.fullNames[n]);
			button[n].setToolTipText(s != null ? s : ElementData.fullNames[n]);
			button[n].setMargin(MARGIN);
			button[n].setFocusPainted(false);
			button[n].setContentAreaFilled(false);
			button[n].setOpaque(true);
			button[n].setBackground(getButtonColor(n));
			button[n].addKeyListener(bKeyListener);
			button[n].setName("" + n);
			button[n].addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JButton eventButton = (JButton) e.getSource();
					int nButton = -1;
					try {
						nButton = Integer.parseInt(eventButton.getName());
					}
					catch (NumberFormatException ex) {
						nButton = -1;
					}
					setSelectedButton(nButton, e.getModifiers());

				}
			});
			if (n == 0) {
				button[n].setBounds(x0, y0, w, h);
			}
			else if (n == 1) {
				button[n].setBounds(x0 + w * 17, y0, w, h);
			}
			else if (n >= 2 && n < 4) {
				button[n].setBounds(x0 + (n - 2) * w, y0 + h, w, h);
			}
			else if (n >= 4 && n < 10) {
				button[n].setBounds(x0 + (n + 8) * w, y0 + h, w, h);
			}
			else if (n >= 10 && n < 12) {
				button[n].setBounds(x0 + (n - 10) * w, y0 + h * 2, w, h);
			}
			else if (n >= 12 && n < 18) {
				button[n].setBounds(x0 + n * w, y0 + h * 2, w, h);
			}
			else if (n >= 18 && n < 36) {
				button[n].setBounds(x0 + (n - 18) * w, y0 + h * 3, w, h);
			}
			else if (n >= 36 && n < 54) {
				button[n].setBounds(x0 + (n - 36) * w, y0 + h * 4, w, h);
			}
			else if (n >= 54 && n < 57) {
				button[n].setBounds(x0 + (n - 54) * w, y0 + h * 5, w, h);
			}
			else if (n >= 71 && n < 86) {
				button[n].setBounds(x0 + (n - 68) * w, y0 + h * 5, w, h);
			}
			else if (n >= 86 && n < 89) {
				button[n].setBounds(x0 + (n - 86) * w, y0 + h * 6, w, h);
			}
			else if (n >= 57 && n < 71) {
				button[n].setBounds(x0 + (n - 54) * w, y0 + 10 + h * 7, w, h);
			}
			else if (n >= 89 && n < 103) {
				button[n].setBounds(x0 + (n - 86) * w, y0 + 10 + h * 8, w, h);
			}
			else {
				button[n].setBounds(x0 + (n - 100) * w, y0 + h * 6, w, h);
			}
			add(button[n]);
		}

		JLabel label = new JLabel(createIcon(RARE_GAS_COLOR));
		s = getInternationalText("RareGas");
		label.setText(s != null ? s : "Rare Gas");
		label.setToolTipText(label.getText());
		label.setFont(smallFont);
		label.setHorizontalAlignment(SwingConstants.LEFT);
		label.setBounds(x0 + w * 4, y0, w * 2, h);
		add(label);

		label = new JLabel(createIcon(METAL_COLOR));
		s = getInternationalText("Metal");
		label.setText(s != null ? s : "Metal");
		label.setFont(smallFont);
		label.setToolTipText(label.getText());
		label.setHorizontalAlignment(SwingConstants.LEFT);
		label.setBounds(x0 + w * 6, y0, w * 2, h);
		add(label);

		label = new JLabel(createIcon(METALLOID_COLOR));
		s = getInternationalText("Metalloid");
		label.setText(s != null ? s : "Metalloid");
		label.setToolTipText(label.getText());
		label.setFont(smallFont);
		label.setHorizontalAlignment(SwingConstants.LEFT);
		label.setBounds(x0 + w * 8, y0, w * 2, h);
		add(label);

		label = new JLabel(createIcon(TRANSITION_METAL_COLOR));
		s = getInternationalText("TransitionMetal");
		label.setText(s != null ? s : "Transition Metal");
		label.setToolTipText(label.getText());
		label.setFont(smallFont);
		label.setHorizontalAlignment(SwingConstants.LEFT);
		label.setBounds(x0 + w * 4, y0 + h, w * 4, h);
		add(label);

		label = new JLabel(createIcon(NON_METAL_COLOR));
		s = getInternationalText("Nonmetal");
		label.setText(s != null ? s : "Nonmetal");
		label.setToolTipText(label.getText());
		label.setFont(smallFont);
		label.setHorizontalAlignment(SwingConstants.LEFT);
		label.setBounds(x0 + w * 8, y0 + h, w * 2, h);
		add(label);

		label = new JLabel(labels[0]);
		label.setFont(font);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setBounds(x0, 0, w, h);
		add(label);

		label = new JLabel(labels[1]);
		label.setFont(font);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setBounds(x0 + w, y0, w, h);
		add(label);

		for (int i = 2; i < 7; i++) {
			label = new JLabel(labels[i]);
			label.setHorizontalAlignment(SwingConstants.CENTER);
			label.setFont(font);
			label.setBounds(x0 + i * w, y0 + h * 2, w, h);
			add(label);
		}

		label = new JLabel(labels[7]);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setFont(font);
		label.setBounds(x0 + w * 7, y0 + h * 2, w * 3, h);
		add(label);

		label = new JLabel(labels[8]);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setFont(font);
		label.setBounds(x0 + w * 9, y0 + h * 2, w * 3, h);
		add(label);

		label = new JLabel(labels[9]);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setFont(font);
		label.setBounds(x0 + w * 10, y0 + h * 2, w * 3, h);
		add(label);

		for (int i = 10; i < 15; i++) {
			label = new JLabel(labels[i]);
			label.setHorizontalAlignment(SwingConstants.CENTER);
			label.setFont(font);
			label.setBounds(x0 + (i + 2) * w, y0, w, h);
			add(label);
		}

		label = new JLabel(labels[15]);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setFont(font);
		label.setBounds(x0 + w * 16, 0, w * 3, h);
		add(label);

		s = getInternationalText("Lanthanoids");
		label = new JLabel("* " + (s != null ? s : "Lanthanoids"));
		label.setFont(font);
		label.setBounds(x0, 10 + h * 8, w * 3, h);
		add(label);

		s = getInternationalText("Actinoids");
		label = new JLabel("+ " + (s != null ? s : "Actinoids"));
		label.setFont(font);
		label.setBounds(x0, 10 + h * 9, w * 3, h);
		add(label);

		// property fields

		w = 18 * w / 4;
		y0 += 9 * h + 20;
		h = 24;

		// row 1
		s = getInternationalText("Name");
		label = new JLabel(s != null ? s : "Name");
		label.setFont(font);
		label.setBounds(x0, y0, w, h);
		add(label);

		s = getInternationalText("Number");
		label = new JLabel(s != null ? s : "No.");
		label.setFont(font);
		label.setBounds(x0 + w, y0, w, h);
		add(label);

		s = getInternationalText("Mass");
		label = new JLabel(s != null ? s : "Mass");
		label.setFont(font);
		label.setBounds(x0 + w * 2, y0, w, h);
		add(label);

		s = getInternationalText("ElectronConfiguration");
		label = new JLabel(s != null ? s : "Electron Configuration");
		label.setFont(font);
		label.setBounds(x0 + w * 3, y0, w, h);
		add(label);

		// row 2
		tf_name.setBounds(x0, y0 + h + 4, w, h);
		add(tf_name);

		tf_number.setBounds(x0 + w, y0 + h + 4, w, h);
		add(tf_number);

		tf_weight.setBounds(x0 + w * 2, y0 + h + 4, w, h);
		add(tf_weight);

		tf_shell.setBounds(x0 + w * 3, y0 + h + 4, w, h);
		add(tf_shell);

		// row 3
		s = getInternationalText("CovalentRadius");
		label = new JLabel(s != null ? s : "Covalent Radius");
		label.setFont(font);
		label.setBounds(x0, y0 + h * 2 + 8, w, h);
		add(label);

		s = getInternationalText("AtomicRadius");
		label = new JLabel(s != null ? s : "Atomic Radius");
		label.setFont(font);
		label.setBounds(x0 + w, y0 + h * 2 + 8, w, h);
		add(label);

		s = getInternationalText("CrystalStructure");
		label = new JLabel(s != null ? s : "Crystal Structure");
		label.setFont(font);
		label.setBounds(x0 + w * 2, y0 + h * 2 + 8, w, h);
		add(label);

		s = getInternationalText("Electronegativity");
		label = new JLabel(s != null ? s : "Electronegativity");
		label.setFont(font);
		label.setBounds(x0 + w * 3, y0 + h * 2 + 8, w, h);
		add(label);

		// row 4
		tf_covrad.setBounds(x0, y0 + h * 3 + 12, w, h);
		add(tf_covrad);

		tf_atorad.setBounds(x0 + w, y0 + h * 3 + 12, w, h);
		add(tf_atorad);

		tf_structure.setBounds(x0 + w * 2, y0 + h * 3 + 12, w, h);
		add(tf_structure);

		tf_electropaul.setBounds(x0 + w * 3, y0 + h * 3 + 12, w, h);
		add(tf_electropaul);

	}

	private static String getInternationalText(String name) {
		if (bundle == null)
			return null;
		if (name == null)
			return null;
		if (isUSLocale)
			return null;
		String s = null;
		try {
			s = bundle.getString(name);
		}
		catch (MissingResourceException e) {
			s = null;
		}
		return s;
	}

	private void initTextFields() {

		tf_name = new JTextField();
		tf_number = new JTextField();
		tf_weight = new JTextField();
		tf_shell = new JTextField();
		tf_structure = new JTextField();
		tf_covrad = new JTextField();
		tf_atorad = new JTextField();
		tf_electropaul = new JTextField();

		tf_name.setEditable(false);
		tf_number.setEditable(false);
		tf_weight.setEditable(false);
		tf_shell.setEditable(false);
		tf_structure.setEditable(false);
		tf_covrad.setEditable(false);
		tf_atorad.setEditable(false);
		tf_electropaul.setEditable(false);

		tf_name.setBackground(Color.white);
		tf_number.setBackground(Color.white);
		tf_weight.setBackground(Color.white);
		tf_shell.setBackground(Color.white);
		tf_structure.setBackground(Color.white);
		tf_covrad.setBackground(Color.white);
		tf_atorad.setBackground(Color.white);
		tf_electropaul.setBackground(Color.white);

	}

	public void mute(boolean b) {
		mute = b;
		if (!mute) {
			initSoundChannel();
		}
		else {
			if (synthesizer != null)
				synthesizer.close();
			channel = null;
		}
	}

	public boolean isMuted() {
		return mute;
	}

	void setSelectedButton(int n) {
		setSelectedButton(n, 0);
	}

	Color getButtonColor(int i) {
		int n = i + 1;
		if (n == 2 || n == 10 || n == 18 || n == 36 || n == 54 || n == 86)
			return RARE_GAS_COLOR;
		if (n == 3 || n == 4 || n == 11 || n == 12 || n == 13 || n == 19 || n == 20 || n == 31 || n == 37 || n == 38
				|| n == 49 || n == 55 || n == 56 || n == 81 || n == 82 || n == 87 || n == 88)
			return METAL_COLOR;
		if (n == 1 || n == 7 || n == 8 || n == 9 || n == 16 || n == 17 || n == 35 || n == 53)
			return NON_METAL_COLOR;
		if (n == 5 || n == 6 || n == 14 || n == 15 || n == 32 || n == 33 || n == 34 || n == 50 || n == 51 || n == 52
				|| n == 83 || n == 84 || n == 85)
			return METALLOID_COLOR;
		if (n <= 30 && n >= 21)
			return TRANSITION_METAL_COLOR;
		if (n <= 48 && n >= 39)
			return TRANSITION_METAL_COLOR;
		if (n <= 80 && n >= 57)
			return TRANSITION_METAL_COLOR;
		if (n <= 112 && n >= 89)
			return TRANSITION_METAL_COLOR;
		return Color.white;
	}

	public void clearSelection() {
		if (selectedObjects != null) {
			Iterator it = selectedObjects.iterator();
			int i;
			while (it.hasNext()) {
				i = ((Integer) it.next()).intValue();
				button[i].setBackground(getButtonColor(i));
				button[i].setForeground(SystemColor.textText);
			}
			selectedObjects.removeAllElements();
		}
		lastSelectedButton = null;
		clearAlltextFields();
		notifyTableItemListeners(null);
		repaint();
	}

	void setSelectedButton(int n, int modifiers) {
		if (n < 0 || n >= button.length)
			return;
		boolean multipleSelection = false;
		if (isMultipleSelectionAllowed()) {
			if ((modifiers & ActionEvent.SHIFT_MASK) != 0) {
				multipleSelection = true;
			}
			if ((modifiers & ActionEvent.META_MASK) != 0) {
				multipleSelection = true;
			}
			if ((modifiers & ActionEvent.ALT_MASK) != 0) {
				multipleSelection = true;
			}
		}
		Integer newNumber = new Integer(n);
		if (!multipleSelection) {
			Iterator it = selectedObjects.iterator();
			int i;
			while (it.hasNext()) {
				i = ((Integer) it.next()).intValue();
				button[i].setBackground(getButtonColor(i));
				button[i].setForeground(SystemColor.textText);
			}
			selectedObjects.removeAllElements();
			selectedObjects.addElement(newNumber);
			button[n].setBackground(SystemColor.textHighlight);
			button[n].setForeground(SystemColor.textHighlightText);
			lastSelectedButton = button[n];
		}
		else {
			if (selectedObjects.contains(newNumber)) {
				selectedObjects.removeElement(newNumber);
				button[n].setBackground(getButtonColor(n));
				button[n].setForeground(SystemColor.textText);
				if (selectedObjects.size() < 1) {
					lastSelectedButton = null;
				}
				else {
					n = selectedObjects.elementAt(selectedObjects.size() - 1).intValue();
					lastSelectedButton = button[n];
				}
			}
			else {
				selectedObjects.addElement(newNumber);
				button[n].setBackground(SystemColor.textHighlight);
				button[n].setForeground(SystemColor.textHighlightText);
				lastSelectedButton = button[n];
			}
		}
		if (lastSelectedButton == null) {
			clearAlltextFields();
			notifyTableItemListeners(null);
			return;
		}
		tf_name.setText(button[n].getToolTipText());
		tf_shell.setText(ElementData.shells[n]);
		tf_structure.setText(ElementData.crystals[n]);
		tf_number.setText(Integer.toString(n + 1));
		tf_weight.setText(format.format(ElementData.weights[n]));
		tf_covrad.setText(format.format(ElementData.covalentRadii[n]));
		tf_atorad.setText(format.format(ElementData.atomicRadii[n]));
		tf_electropaul.setText(format.format(ElementData.electroNegativityPauling[n]));
		playSound(ElementData.sounds[n]);
		notifyTableItemListeners(ElementData.elements[n]);
	}

	void clearAlltextFields() {
		tf_name.setText("");
		tf_shell.setText("");
		tf_structure.setText("");
		tf_number.setText("");
		tf_weight.setText("");
		tf_covrad.setText("");
		tf_atorad.setText("");
		tf_electropaul.setText("");
	}

	public boolean isMultipleSelectionAllowed() {
		return multipleSelectionAllowed;
	}

	public void setMultipleSelectionAllowed(boolean multipleSelectionAllowed) {
		this.multipleSelectionAllowed = multipleSelectionAllowed;
	}

	int getMaxElementIndex() {
		return 112;
	}

	int getDownElementIndex(int n) {
		if (n >= 80 && n < getMaxElementIndex())
			return n;
		if (n >= 39 && n < 81)
			return n + 32;
		if (n >= 12 && n < 39)
			return n + 18;
		if (n >= 1 && n < 12)
			return n + 8;
		if (n == 0)
			return n + 2;
		return n;
	}

	int getUpElementIndex(int n) {
		if ((n >= 0 && n < 2) || (n >= 3 && n < 9) || (n >= 20 && n < 30))
			return n;
		if (n == 2)
			return n - 2;
		if (n >= 9 && n < 20)
			return n - 8;
		if (n >= 30 && n < 57)
			return n - 18;
		if (n >= 57 && n < 71)
			return 38;
		if (n >= 71 && n < getMaxElementIndex())
			return n - 32;
		return n;
	}

	private void initSoundChannel() {
		try {
			synthesizer = MidiSystem.getSynthesizer();
			synthesizer.open();
			Soundbank sb = synthesizer.getDefaultSoundbank();
			int instIndex = 0;
			if (sb != null) {
				Instrument[] instruments = sb.getInstruments();
				for (int i = 0; i < instruments.length; i++) {
					if ("Piano".equals(instruments[i].getName())) {
						instIndex = i;
						break;
					}
				}
				synthesizer.loadInstrument(instruments[instIndex]);
			}
			channel = synthesizer.getChannels()[0];
			channel.setChannelPressure(127);
			channel.programChange(instIndex);
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	void playSound(int note) {
		if (channel == null || mute)
			return;
		channel.noteOn(note, 30);
	}

	public void selectAtom(String atomSymbol) {
		if (button == null || atomSymbol == null)
			return;
		String buttonName;
		for (int i = 0; i < button.length; i++) {
			buttonName = button[i].getText();
			if (atomSymbol.equalsIgnoreCase(buttonName)) {
				setSelectedButton(i);
				break;
			}
		}
	}

	public Object[] getSelectedObjects() {
		int nObjects = selectedObjects.size();
		if (nObjects < 1)
			return null;
		String[] objects = new String[nObjects];
		for (int i = 0; i < nObjects; i++) {
			objects[i] = ElementData.elements[selectedObjects.elementAt(i).intValue()];

		}
		return objects;
	}

	public void addItemListener(ItemListener l) {
		if (l == null)
			return;
		if (itemListeners == null)
			itemListeners = new Vector<ItemListener>();
		if (!itemListeners.contains(l))
			itemListeners.addElement(l);
	}

	public void removeItemListener(ItemListener l) {
		if (itemListeners == null || l == null)
			return;
		if (itemListeners.contains(l))
			itemListeners.removeElement(l);
	}

	public void notifyTableItemListeners(Object item) {
		if (itemListeners != null) {
			ItemEvent e = new ItemEvent(this, ItemEvent.ITEM_FIRST, item, ItemEvent.SELECTED);
			for (ItemListener l : itemListeners)
				l.itemStateChanged(e);
		}
	}

	private static Icon createIcon(final Color color) {
		return new Icon() {
			public void paintIcon(Component c, Graphics g, int x, int y) {
				g.setColor(color);
				g.fillRect(x, y, getIconWidth(), getIconHeight());
				g.setColor(Color.black);
				g.drawRect(x, y, getIconWidth(), getIconHeight());
			}

			public int getIconWidth() {
				return 24;
			}

			public int getIconHeight() {
				return 12;
			}
		};
	}

	static class ButtonKeyListener extends KeyAdapter {

		PeriodicTable owner;

		public ButtonKeyListener(PeriodicTable owner) {
			this.owner = owner;
		}

		public void keyPressed(KeyEvent e) {
			super.keyPressed(e);
			e.consume();
		}

		public void keyReleased(KeyEvent evt) {
			boolean doSelection = false;
			int keyCode = evt.getKeyCode();
			int nElement = 0;
			if (owner.lastSelectedButton == null) {
				evt.consume();
				return;
			}
			try {
				nElement = Integer.parseInt(owner.lastSelectedButton.getName()) + 1;
			}
			catch (Exception e) {
			}
			if (nElement == 0) {
				evt.consume();
				return;
			}
			switch (keyCode) {
			case KeyEvent.VK_DOWN:
				nElement = owner.getDownElementIndex(nElement - 1) + 1;
				doSelection = true;
				break;
			case KeyEvent.VK_UP:
				nElement = owner.getUpElementIndex(nElement - 1) + 1;
				doSelection = true;
				break;
			case KeyEvent.VK_LEFT:
				nElement--;
				if (nElement < 0)
					nElement = 0;
				doSelection = true;
				break;
			case KeyEvent.VK_RIGHT:
				nElement++;
				if (nElement >= owner.getMaxElementIndex()) {
					nElement = owner.getMaxElementIndex();
				}
				doSelection = true;
				break;
			}
			if (doSelection)
				owner.setSelectedButton(nElement - 1);
			evt.consume();
		}
	}

}
