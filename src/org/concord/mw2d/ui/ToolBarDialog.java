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

package org.concord.mw2d.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.concord.modeler.ui.ComboCheckBox;

class ToolBarDialog extends JDialog {

	private JList categoryList;
	private JComponent buttonList;
	private MDContainer container;
	private JButton descriptionButton, customizeButton;
	private JLabel descriptionLabel;

	ToolBarDialog(MDContainer container) {

		super(JOptionPane.getFrameForComponent(container), "Customize", true);
		String s = MDContainer.getInternationalText("CustomizeToolBar");
		if (s != null)
			setTitle(s);
		this.container = container;
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		JTabbedPane tabbedPane = new JTabbedPane();
		getContentPane().add(tabbedPane, BorderLayout.CENTER);

		JPanel p = new JPanel(new BorderLayout(10, 5));
		p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		s = MDContainer.getInternationalText("ToolBarButton");
		tabbedPane.addTab(s != null ? s : "Toolbar Buttons", p);

		JPanel p1 = new JPanel(new BorderLayout(2, 2));
		p.add(p1, BorderLayout.CENTER);

		buttonList = Box.createVerticalBox();
		buttonList.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				switch (e.getKeyCode()) {
				case KeyEvent.VK_ENTER:
					Runnable run = getCustomizationAction();
					if (run != null)
						EventQueue.invokeLater(run);
					break;
				case KeyEvent.VK_UP:
					ComboCheckBox box = previousComponent();
					if (box != null) {
						highlightComponent(box);
						buttonList.repaint();
					}
					break;
				case KeyEvent.VK_DOWN:
					box = nextComponent();
					if (box != null) {
						highlightComponent(box);
						buttonList.repaint();
					}
					break;
				case KeyEvent.VK_PAGE_UP:
					highlightComponent((ComboCheckBox) buttonList.getComponent(0));
					buttonList.repaint();
					break;
				case KeyEvent.VK_PAGE_DOWN:
					highlightComponent((ComboCheckBox) buttonList.getComponent(buttonList.getComponentCount() - 1));
					buttonList.repaint();
					break;
				}
			}
		});
		JScrollPane scroller = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		JPanel p2 = new JPanel(new BorderLayout());
		p2.add(buttonList, BorderLayout.NORTH);
		scroller.setViewportView(p2);
		scroller.setPreferredSize(new Dimension(300, 240));
		p1.add(scroller, BorderLayout.CENTER);

		s = MDContainer.getInternationalText("Actions");
		p1.add(new JLabel(s != null ? s : "Actions", SwingConstants.LEFT), BorderLayout.NORTH);

		p1 = new JPanel(new BorderLayout(2, 2));
		p.add(p1, BorderLayout.WEST);

		categoryList = new JList();
		categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scroller = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroller.setViewportView(categoryList);
		scroller.setPreferredSize(new Dimension(160, 240));
		p1.add(scroller, BorderLayout.CENTER);

		s = MDContainer.getInternationalText("Categories");
		p1.add(new JLabel(s != null ? s : "Categories", SwingConstants.LEFT), BorderLayout.NORTH);

		p1 = new JPanel();
		p.add(p1, BorderLayout.SOUTH);
		s = MDContainer.getInternationalText("SelectedTool");
		p1.add(new JLabel((s != null ? s : "Selected tool") + ": "));

		final JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.setBackground(SystemColor.info);
		popupMenu.setBorder(BorderFactory.createLineBorder(Color.black));
		descriptionLabel = new JLabel();
		descriptionLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
		popupMenu.add(descriptionLabel);

		s = MDContainer.getInternationalText("Description");
		descriptionButton = new JButton(s != null ? s : "Description");
		descriptionButton.setEnabled(false);
		descriptionButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				descriptionLabel.setText(getDescription());
				popupMenu.pack();
				popupMenu.show(descriptionButton, 10, 10);
			}
		});
		p1.add(descriptionButton);

		s = MDContainer.getInternationalText("Customize");
		customizeButton = new JButton(s != null ? s : "Customize");
		customizeButton.setEnabled(false);
		customizeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Runnable run = getCustomizationAction();
				if (run != null)
					EventQueue.invokeLater(run);
			}
		});
		p1.add(customizeButton);

		s = MDContainer.getInternationalText("Close");
		JButton button = new JButton(s != null ? s : "Close");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ToolBarDialog.this.dispose();
			}
		});
		p1.add(button);

		categoryList.setListData(container.getActionCategories().keySet().toArray());
		categoryList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				displayButtonList();
			}
		});
		categoryList.setSelectedIndex(0); // initialize

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				ToolBarDialog.this.dispose();
			}

			public void windowActivated(WindowEvent e) {
				categoryList.requestFocus();
			}
		});

	}

	private void highlightComponent(ComboCheckBox c) {
		for (int i = 0; i < buttonList.getComponentCount(); i++) {
			if (buttonList.getComponent(i) != c)
				((ComboCheckBox) buttonList.getComponent(i)).setHighlighted(false);
		}
		c.setHighlighted(true);
		customizeButton.setEnabled(getCustomizationAction() != null);
	}

	private ComboCheckBox nextComponent() {
		int n = buttonList.getComponentCount();
		ComboCheckBox box;
		for (int i = 0; i < n; i++) {
			box = (ComboCheckBox) buttonList.getComponent(i);
			if (box.isHighlighted() && i < n - 1)
				return (ComboCheckBox) buttonList.getComponent(i + 1);
		}
		return null;
	}

	private ComboCheckBox previousComponent() {
		int n = buttonList.getComponentCount();
		ComboCheckBox box;
		for (int i = 0; i < n; i++) {
			box = (ComboCheckBox) buttonList.getComponent(i);
			if (box.isHighlighted() && i > 0)
				return (ComboCheckBox) buttonList.getComponent(i - 1);
		}
		return null;
	}

	private String getDescription() {
		int n = buttonList.getComponentCount();
		ComboCheckBox box;
		for (int i = 0; i < n; i++) {
			box = (ComboCheckBox) buttonList.getComponent(i);
			if (box.isHighlighted())
				return box.getCheckBox().getToolTipText();
		}
		return null;
	}

	private Runnable getCustomizationAction() {
		int n = buttonList.getComponentCount();
		ComboCheckBox box;
		for (int i = 0; i < n; i++) {
			box = (ComboCheckBox) buttonList.getComponent(i);
			if (box.isHighlighted())
				return (Runnable) container.getCustomizationAction().get(box.getLabel().getText());
		}
		return null;
	}

	void displayButtonList() {

		buttonList.removeAll();
		descriptionButton.setEnabled(false);
		customizeButton.setEnabled(false);

		List list = (List) container.getActionCategories().get(categoryList.getSelectedValue());
		Action action;
		for (Iterator it = list.iterator(); it.hasNext();) {
			final AbstractButton button = (AbstractButton) it.next();
			action = button.getAction();
			final ComboCheckBox c = new ComboCheckBox();
			c.getLabel().setIcon((Icon) action.getValue(Action.SMALL_ICON));
			c.getLabel().setText((String) action.getValue(Action.SHORT_DESCRIPTION));
			c.getLabel().addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					buttonList.requestFocus();
					highlightComponent(c);
					descriptionButton.setEnabled(true);
				}
			});
			final Runnable run = (Runnable) container.getCustomizationAction().get(c.getLabel().getText());
			if (run != null)
				c.getLabel().addMouseListener(new MouseAdapter() {
					public void mousePressed(MouseEvent e) {
						if (e.getClickCount() >= 2)
							EventQueue.invokeLater(run);
					}
				});
			c.getCheckBox().setToolTipText((String) action.getValue(Action.LONG_DESCRIPTION));
			c.getCheckBox().setSelected(button.getParent() != null);
			c.getCheckBox().addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						container.addToolBarButton(button);
					}
					else if (e.getStateChange() == ItemEvent.DESELECTED) {
						container.removeToolBarButton(button);
					}
				}
			});
			buttonList.add(c);
		}

		pack();

	}

}