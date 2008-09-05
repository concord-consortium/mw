/*
 *   Copyright (C) 2007  The Concord Consortium, Inc.,
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.concord.modeler.text.Page;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.IntegerTextField;
import org.concord.modeler.ui.PastableTextArea;
import org.concord.modeler.util.FileUtilities;

/**
 * @author Charles Xie
 * 
 */
class PageAppletMaker extends ComponentMaker {

	private final static FileFilter jarFilter = new FileFilter() {
		public boolean accept(File file) {
			if (file == null)
				return false;
			if (file.toString().toLowerCase().endsWith(".jar"))
				return true;
			return false;
		}
	};

	PageApplet pageApplet;
	private JDialog dialog;
	private JList jarList;
	private JComboBox borderComboBox, classComboBox;
	private ColorComboBox bgComboBox;
	private JButton okButton;
	private JTextArea parameterArea;
	private IntegerTextField widthField, heightField;
	private JCheckBox cachingCheckBox;
	private static Map<String, List<String>> jarClassMap;
	private JPanel contentPane;

	PageAppletMaker(PageApplet pa) {
		setApplet(pa);
	}

	void setApplet(PageApplet pa) {
		pageApplet = pa;
	}

	void confirm() {
		if (jarList.isSelectionEmpty())
			return;
		if (pageApplet.jarName == null)
			pageApplet.jarName = new ArrayList<String>();
		else pageApplet.jarName.clear();
		Object[] o = jarList.getSelectedValues();
		for (Object i : o)
			pageApplet.jarName.add((String) i);
		pageApplet.className = (String) classComboBox.getSelectedItem();
		String s = parameterArea.getText();
		if (s != null && !s.trim().equals("")) {
			pageApplet.parseParameters(s);
		}
		else {
			pageApplet.removeAllParameters();
		}
		pageApplet.setPreferredSize(new Dimension((widthField.getValue()), (heightField.getValue())));
		pageApplet.setBorderType((String) borderComboBox.getSelectedItem());
		pageApplet.setBackground(bgComboBox.getSelectedColor());
		pageApplet.setCachingAllowed(cachingCheckBox.isSelected());
		pageApplet.page.getSaveReminder().setChanged(true);
		pageApplet.page.settleComponentSize();
		pageApplet.start();
	}

	void invoke(Page page) {

		pageApplet.page = page;
		page.deselect();
		createContentPane();

		if (needNewDialog(dialog, page)) {
			String s = Modeler.getInternationalText("CustomizeAppletDialogTitle");
			dialog = ModelerUtilities.getChildDialog(page, s != null ? s : "Customize applet", true);
			dialog.setContentPane(contentPane);
			dialog.pack();
			dialog.setLocationRelativeTo(dialog.getOwner());
			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					cancel = true;
					dialog.dispose();
				}

				public void windowActivated(WindowEvent e) {
					widthField.selectAll();
					widthField.requestFocusInWindow();
				}
			});
		}

		fillJarList();
		classComboBox.setSelectedItem(pageApplet.className);
		parameterArea.setText(pageApplet.parametersToString());
		parameterArea.setCaretPosition(0);
		widthField.setValue(pageApplet.getPreferredSize().width);
		heightField.setValue(pageApplet.getPreferredSize().height);
		borderComboBox.setSelectedItem(pageApplet.getBorderType());
		bgComboBox.setColor(pageApplet.getBackground());
		cachingCheckBox.setSelected(pageApplet.isCachingAllowed());
		okButton.setEnabled(jarList.getModel().getSize() > 0);

		dialog.setVisible(true);

	}

	private void fillJarList() {
		File parent = new File(pageApplet.page.getAddress()).getParentFile();
		if (parent == null)
			return;
		File[] files = parent.listFiles(jarFilter);
		if (files == null || files.length == 0)
			return;
		String[] s = new String[files.length];
		for (int i = 0; i < files.length; i++)
			s[i] = FileUtilities.getFileName(files[i].toString());
		jarList.setListData(s);
		if (pageApplet.jarName == null || pageApplet.jarName.isEmpty())
			return;
		BitSet bs = new BitSet(s.length);
		for (int i = 0; i < s.length; i++) {
			for (String x : pageApplet.jarName) {
				if (s[i].equals(x)) {
					bs.set(i);
					break;
				}
			}
		}
		int[] n = new int[bs.cardinality()];
		int k = 0;
		for (int i = 0; i < bs.size(); i++) {
			if (bs.get(i))
				n[k++] = i;
		}
		jarList.setSelectedIndices(n);
	}

	private void fillClassComboBox(String[] jar) {
		classComboBox.removeAllItems();
		if (jar == null || jar.length == 0)
			return;
		JarFile jarFile;
		String file;
		String parent = FileUtilities.getCodeBase(pageApplet.page.getAddress());
		if (jarClassMap == null)
			jarClassMap = new HashMap<String, List<String>>();
		JarEntry entry;
		String entryName;
		List<String> list;
		for (String x : jar) {
			file = parent + x;
			list = jarClassMap.get(file);
			if (list == null) {
				try {
					jarFile = new JarFile(file);
				}
				catch (IOException e) {
					e.printStackTrace();
					jarFile = null;
				}
				if (jarFile == null)
					continue;
				list = new ArrayList<String>();
				for (Enumeration e = jarFile.entries(); e.hasMoreElements();) {
					entry = (JarEntry) e.nextElement();
					entryName = entry.getName();
					if (entryName.indexOf("$") != -1)
						continue;
					if (!entryName.endsWith(".class"))
						continue;
					entryName = entryName.replaceAll("/", ".").substring(0, entryName.length() - 6);
					list.add(entryName);
				}
				jarClassMap.put(file, list);
			}
			for (String s : list) {
				classComboBox.addItem(s);
			}
		}
	}

	private void createContentPane() {

		if (contentPane != null)
			return;

		contentPane = new JPanel(new BorderLayout());

		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				confirm();
				dialog.dispose();
				cancel = false;
			}
		};

		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		contentPane.add(p, BorderLayout.SOUTH);

		String s = Modeler.getInternationalText("OKButton");
		okButton = new JButton(s != null ? s : "OK");
		okButton.addActionListener(okListener);
		p.add(okButton);

		s = Modeler.getInternationalText("CancelButton");
		JButton button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
				cancel = true;
			}
		});
		p.add(button);

		p = new JPanel(new SpringLayout());
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.add(p, BorderLayout.NORTH);

		// row 1
		s = Modeler.getInternationalText("SelectJars");
		p.add(new JLabel(s != null ? s : "Select jar(s)", SwingConstants.LEFT));
		jarList = new JList();
		jarList.setFont(new Font(jarList.getFont().getFamily(), jarList.getFont().getStyle(), 10));
		jarList.setToolTipText("Select jar files from the current directory.");
		jarList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting())
					return;
				int[] index = jarList.getSelectedIndices();
				if (index == null || index.length == 0)
					return;
				Object[] o = jarList.getSelectedValues();
				String[] s = new String[o.length];
				for (int i = 0; i < s.length; i++)
					s[i] = (String) o[i];
				fillClassComboBox(s);
			}
		});
		p.add(new JScrollPane(jarList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));

		// row 2
		s = Modeler.getInternationalText("SelectAppletClass");
		p.add(new JLabel(s != null ? s : "Select applet's main class", SwingConstants.LEFT));
		classComboBox = new JComboBox();
		p.add(classComboBox);

		// row 3
		s = Modeler.getInternationalText("WidthLabel");
		p.add(new JLabel(s != null ? s : "Width", SwingConstants.LEFT));
		widthField = new IntegerTextField(pageApplet.getPreferredSize().width, 10, 1000);
		widthField.setToolTipText("Type in the width.");
		widthField.addActionListener(okListener);
		p.add(widthField);

		// row 4
		s = Modeler.getInternationalText("HeightLabel");
		p.add(new JLabel(s != null ? s : "Height", SwingConstants.LEFT));
		heightField = new IntegerTextField(pageApplet.getPreferredSize().height, 10, 1000);
		heightField.setToolTipText("Type in the height.");
		heightField.addActionListener(okListener);
		p.add(heightField);

		// row 5
		s = Modeler.getInternationalText("BackgroundColorLabel");
		p.add(new JLabel(s != null ? s : "Background color", SwingConstants.LEFT));
		bgComboBox = new ColorComboBox(pageApplet);
		bgComboBox.setSelectedIndex(ColorComboBox.INDEX_MORE_COLOR);
		bgComboBox.setRequestFocusEnabled(false);
		bgComboBox.setToolTipText("Select the background color.");
		p.add(bgComboBox);

		// row 6
		s = Modeler.getInternationalText("BorderLabel");
		p.add(new JLabel(s != null ? s : "Border", SwingConstants.LEFT));
		borderComboBox = new JComboBox(BorderManager.BORDER_TYPE);
		borderComboBox.setRenderer(new ComboBoxRenderer.BorderCell());
		borderComboBox.setBackground(p.getBackground());
		borderComboBox.setToolTipText("Select the border type.");
		borderComboBox.setPreferredSize(new Dimension(200, 24));
		p.add(borderComboBox);

		ModelerUtilities.makeCompactGrid(p, 6, 2, 5, 5, 10, 2);

		// parameter setting area
		p = new JPanel(new BorderLayout(4, 4));
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.add(p, BorderLayout.CENTER);

		JPanel p1 = new JPanel(new BorderLayout(4, 4));
		p.add(p1, BorderLayout.SOUTH);

		s = Modeler.getInternationalText("EnterParametersInNameValuePairs");
		p1.add(new JLabel(s != null ? s : "Enter parameters in name-value pairs:"), BorderLayout.NORTH);
		parameterArea = new PastableTextArea(5, 10);
		parameterArea.setBorder(BorderFactory.createLoweredBevelBorder());
		p1.add(new JScrollPane(parameterArea), BorderLayout.CENTER);
		JEditorPane ep = new JEditorPane("text/html",
				"<html><body face=Verdana><font size=2>e.g. {name=\"x\" value=\"100\"} {name=\"y\" value=\"50\"}</font></body></html>");
		ep.setEditable(false);
		ep.setBackground(p1.getBackground());
		p1.add(ep, BorderLayout.SOUTH);

		p1 = new JPanel();
		p.add(p1, BorderLayout.CENTER);
		p1.add(new JLabel("If the applet doesn't read files, select this:"));
		cachingCheckBox = new JCheckBox("Allow caching");
		p1.add(cachingCheckBox);

	}

}