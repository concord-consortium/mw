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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
class PageJContainerMaker extends ComponentMaker {

	private final static FileFilter jarFilter = new FileFilter() {
		public boolean accept(File file) {
			if (file == null)
				return false;
			if (file.toString().toLowerCase().endsWith(".jar"))
				return true;
			return false;
		}
	};

	PageJContainer pageJContainer;
	private JDialog dialog;
	private JList jarList;
	private JComboBox borderComboBox, classComboBox;
	private ColorComboBox bgComboBox;
	private JButton okButton;
	private JTextArea parameterArea;
	private IntegerTextField widthField, heightField;
	private JTextField resourceField;
	private JTabbedPane tabbedPane;
	private JComboBox knownPluginComboBox;
	private JTextField codeBaseField, jarField, mainClassField;
	private static Map<String, List<String>> jarClassMap;
	private JPanel contentPane;

	PageJContainerMaker(PageJContainer pp) {
		setJContainer(pp);
	}

	void setJContainer(PageJContainer pp) {
		pageJContainer = pp;
	}

	boolean confirm() {
		boolean remote = tabbedPane.getSelectedIndex() == 0;
		if (pageJContainer.jarName == null)
			pageJContainer.jarName = new ArrayList<String>();
		else pageJContainer.jarName.clear();
		if (remote) {
			String codeBase = codeBaseField.getText();
			if (codeBase == null || codeBase.trim().equals("")) {
				JOptionPane.showMessageDialog(dialog, "Code base must be provided.", "Code base required",
						JOptionPane.ERROR_MESSAGE);
				return false;
			}
			pageJContainer.setCodeBase(codeBase);
			String mainClass = mainClassField.getText();
			if (mainClass == null || mainClass.trim().equals("")) {
				JOptionPane.showMessageDialog(dialog, "Main class must be provided.", "Main class required",
						JOptionPane.ERROR_MESSAGE);
				return false;
			}
			pageJContainer.className = mainClass;
			String jar = jarField.getText();
			if (jar == null || jar.trim().equals("")) {
				JOptionPane.showMessageDialog(dialog, "At least one jar file must be provided.", "Jar file required",
						JOptionPane.ERROR_MESSAGE);
				return false;
			}
			String[] s = jar.split(",{1}(\\s*)");
			for (String x : s) {
				if (!x.trim().equals(""))
					pageJContainer.jarName.add(x.trim());
			}
		}
		else {
			if (jarList.getModel().getSize() <= 0) {
				JOptionPane.showMessageDialog(dialog, "No local jar file is found in this folder.",
						"Jar files required", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			if (jarList.isSelectionEmpty()) {
				JOptionPane.showMessageDialog(dialog, "You must select a local jar file.", "Jar files required",
						JOptionPane.ERROR_MESSAGE);
				return false;
			}
			Object[] o = jarList.getSelectedValues();
			for (Object i : o)
				pageJContainer.jarName.add((String) i);
			pageJContainer.className = (String) classComboBox.getSelectedItem();
		}
		String s = parameterArea.getText();
		if (s != null && !s.trim().equals("")) {
			pageJContainer.parseParameters(s);
		}
		else {
			pageJContainer.removeAllParameters();
		}
		pageJContainer.setPreferredSize(new Dimension((widthField.getValue()), (heightField.getValue())));
		if (!resourceField.getText().trim().equals(""))
			pageJContainer.setCachedFileNames(resourceField.getText());
		pageJContainer.setBorderType((String) borderComboBox.getSelectedItem());
		pageJContainer.setBackground(bgComboBox.getSelectedColor());
		pageJContainer.page.getSaveReminder().setChanged(true);
		pageJContainer.page.reload();
		pageJContainer.start();
		return true;
	}

	void invoke(Page page) {

		pageJContainer.page = page;
		page.deselect();
		createContentPane();

		boolean remote = pageJContainer.getCodeBase() != null;

		if (needNewDialog(dialog, page)) {
			String s = Modeler.getInternationalText("CustomizePluginDialogTitle");
			dialog = ModelerUtilities.getChildDialog(page, s != null ? s : "Customize plugin", true);
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
		classComboBox.setSelectedItem(pageJContainer.className);
		parameterArea.setText(pageJContainer.parametersToString());
		parameterArea.setCaretPosition(0);
		widthField.setValue(pageJContainer.getPreferredSize().width);
		heightField.setValue(pageJContainer.getPreferredSize().height);
		resourceField.setText(pageJContainer.getCachedFileNames());
		borderComboBox.setSelectedItem(pageJContainer.getBorderType());
		bgComboBox.setColor(pageJContainer.getBackground());
		tabbedPane.setSelectedIndex(remote ? 0 : 1);
		if (remote) {
			jarList.clearSelection();
			codeBaseField.setText(pageJContainer.getCodeBase());
			mainClassField.setText(pageJContainer.className);
			String jarNames = "";
			for (String x : pageJContainer.jarName)
				jarNames += x + ", ";
			jarField.setText(jarNames.substring(0, jarNames.length() - 2));
			PluginInfo pi = PluginManager.getPluginInfoByMainClass(pageJContainer.className);
			if (pi != null)
				knownPluginComboBox.setSelectedItem(pi.getName());
			okButton.setEnabled(pi != null);
		}
		else {
			knownPluginComboBox.setSelectedIndex(0);
			codeBaseField.setText(null);
			mainClassField.setText(null);
			jarField.setText(null);
			okButton.setEnabled(jarList.getModel().getSize() > 0);
		}

		dialog.setVisible(true);

	}

	private void fillJarList() {
		File parent = new File(pageJContainer.page.getAddress()).getParentFile();
		if (parent == null)
			return;
		File[] files = parent.listFiles(jarFilter);
		if (files == null || files.length == 0)
			return;
		String[] s = new String[files.length];
		for (int i = 0; i < files.length; i++)
			s[i] = FileUtilities.getFileName(files[i].toString());
		jarList.setListData(s);
		if (pageJContainer.jarName == null || pageJContainer.jarName.isEmpty())
			return;
		BitSet bs = new BitSet(s.length);
		for (int i = 0; i < s.length; i++) {
			for (String x : pageJContainer.jarName) {
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
		String parent = FileUtilities.getCodeBase(pageJContainer.page.getAddress());
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
				if (confirm()) {
					dialog.dispose();
					cancel = false;
				}
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

		JPanel p2 = new JPanel(new BorderLayout());
		tabbedPane = new JTabbedPane();
		tabbedPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		tabbedPane.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				switch (tabbedPane.getSelectedIndex()) {
				case 0:
					break;
				case 1:
					break;
				}
			}
		});
		p2.add(tabbedPane, BorderLayout.NORTH);
		contentPane.add(p2, BorderLayout.NORTH);

		// jars are on a remote site

		JPanel p3 = new JPanel(new BorderLayout());
		s = Modeler.getInternationalText("RemoteSite");
		tabbedPane.addTab(s != null ? s : "Remote site", p3);

		p = new JPanel(new SpringLayout());
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		p3.add(p, BorderLayout.NORTH);

		// row 1
		s = Modeler.getInternationalText("SelectKnownPlugin");
		p.add(new JLabel(s != null ? s : "Select known plugin", SwingConstants.LEFT));

		knownPluginComboBox = new JComboBox();
		knownPluginComboBox.addItem("None");
		if (PluginManager.getPlugins() != null) {
			for (PluginInfo x : PluginManager.getPlugins())
				knownPluginComboBox.addItem(x.getName());
		}
		knownPluginComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					PluginInfo pi = PluginManager.getPluginInfoByName(e.getItem().toString());
					if (pi != null) {
						mainClassField.setText(pi.getMainClass());
						List<String> list = pi.getFileList();
						codeBaseField.setText(FileUtilities.getCodeBase(list.get(0)));
						String x = "";
						for (String s : list) {
							x += FileUtilities.getFileName(s) + ", ";
						}
						jarField.setText(x.substring(0, x.length() - 2));
						if (!okButton.isEnabled())
							okButton.setEnabled(true);
					}
					else {
						mainClassField.setText(null);
						codeBaseField.setText(null);
						jarField.setText(null);
					}
				}
			}
		});
		p.add(knownPluginComboBox);

		// row 2
		s = Modeler.getInternationalText("CodeBase");
		p.add(new JLabel(s != null ? s : "Code base", SwingConstants.LEFT));
		codeBaseField = new JTextField();
		codeBaseField.setToolTipText("Type the code base of the plugin");
		codeBaseField.addActionListener(okListener);
		p.add(codeBaseField);

		// row 3
		s = Modeler.getInternationalText("NamesOfJarFiles");
		p.add(new JLabel(s != null ? s : "Names of jar files", SwingConstants.LEFT));
		jarField = new JTextField();
		jarField.setToolTipText("Type the names of the jar files needed for the plugin, separated by commas");
		jarField.addActionListener(okListener);
		p.add(jarField);

		// row 4
		s = Modeler.getInternationalText("SelectPluginMainClass");
		p.add(new JLabel(s != null ? s : "Select plugin's main class", SwingConstants.LEFT));
		mainClassField = new JTextField();
		mainClassField.setToolTipText("Type the full name of the main class, e.g. com.dot.app.MyMainClass");
		mainClassField.addActionListener(okListener);
		p.add(mainClassField);

		ModelerUtilities.makeCompactGrid(p, 4, 2, 5, 5, 10, 2);

		// page-scope plugin settings

		p = new JPanel(new SpringLayout());
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		s = Modeler.getInternationalText("LocalFiles");
		tabbedPane.addTab(s != null ? s : "Local files", p);

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
		s = Modeler.getInternationalText("SelectPluginMainClass");
		p.add(new JLabel(s != null ? s : "Select plugin's main class", SwingConstants.LEFT));
		classComboBox = new JComboBox();
		p.add(classComboBox);

		ModelerUtilities.makeCompactGrid(p, 2, 2, 5, 5, 10, 2);

		// common settings

		p = new JPanel(new SpringLayout());
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		p2.add(p, BorderLayout.CENTER);

		// row 1
		s = Modeler.getInternationalText("WidthLabel");
		p.add(new JLabel(s != null ? s : "Width", SwingConstants.LEFT));
		widthField = new IntegerTextField(pageJContainer.getPreferredSize().width, 10, 1000);
		widthField.setToolTipText("Type in the width.");
		widthField.addActionListener(okListener);
		p.add(widthField);

		// row 2
		s = Modeler.getInternationalText("HeightLabel");
		p.add(new JLabel(s != null ? s : "Height", SwingConstants.LEFT));
		heightField = new IntegerTextField(pageJContainer.getPreferredSize().height, 10, 1000);
		heightField.setToolTipText("Type in the height.");
		heightField.addActionListener(okListener);
		p.add(heightField);

		// row 3
		s = Modeler.getInternationalText("BackgroundColorLabel");
		p.add(new JLabel(s != null ? s : "Background color", SwingConstants.LEFT));
		bgComboBox = new ColorComboBox(pageJContainer);
		bgComboBox.setSelectedIndex(ColorComboBox.INDEX_MORE_COLOR);
		bgComboBox.setRequestFocusEnabled(false);
		bgComboBox.setToolTipText("Select the background color.");
		p.add(bgComboBox);

		// row 4
		s = Modeler.getInternationalText("BorderLabel");
		p.add(new JLabel(s != null ? s : "Border", SwingConstants.LEFT));
		borderComboBox = new JComboBox(BorderManager.BORDER_TYPE);
		borderComboBox.setRenderer(new ComboBoxRenderer.BorderCell());
		borderComboBox.setBackground(p.getBackground());
		borderComboBox.setToolTipText("Select the border type.");
		borderComboBox.setPreferredSize(new Dimension(200, 24));
		p.add(borderComboBox);

		// row 5
		s = Modeler.getInternationalText("CacheFiles");
		p.add(new JLabel(s != null ? s : "Cache Files", SwingConstants.LEFT));
		resourceField = new JTextField();
		resourceField
				.setToolTipText("Type in the file names of the resources needed to be cached. Leave blank if none.");
		resourceField.addActionListener(okListener);
		p.add(resourceField);

		ModelerUtilities.makeCompactGrid(p, 5, 2, 5, 5, 10, 2);

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

	}

}