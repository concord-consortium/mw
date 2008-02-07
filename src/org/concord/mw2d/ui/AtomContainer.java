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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.process.AbstractLoadable;
import org.concord.modeler.process.Loadable;
import org.concord.modeler.ui.CustomBevelBorder;
import org.concord.modeler.ui.CustomBorderLayout;
import org.concord.modeler.ui.IconPool;
import org.concord.modeler.ui.SimulatorMenuBar;
import org.concord.modeler.util.FileChooser;
import org.concord.modeler.util.SwingWorker;
import org.concord.molbio.engine.DeletionMutator;
import org.concord.molbio.engine.DNA;
import org.concord.molbio.engine.InsertionMutator;
import org.concord.molbio.engine.SubstitutionMutator;
import org.concord.molbio.event.MutationEvent;
import org.concord.molbio.event.MutationListener;
import org.concord.molbio.event.RNATranslationEvent;
import org.concord.molbio.event.RNATranslationListener;
import org.concord.molbio.event.RNATranscriptionEvent;
import org.concord.molbio.event.RNATranscriptionListener;
import org.concord.molbio.ui.DNAScrollerWithRNA;
import org.concord.mw2d.AtomisticView;
import org.concord.mw2d.BoundarySetup;
import org.concord.mw2d.DiffractionInstrument;
import org.concord.mw2d.MDView;
import org.concord.mw2d.UserAction;
import org.concord.mw2d.models.AminoAcidAdapter;
import org.concord.mw2d.models.Atom;
import org.concord.mw2d.models.Codon;
import org.concord.mw2d.models.Element;
import org.concord.mw2d.models.Grid;
import org.concord.mw2d.models.MDModel;
import org.concord.mw2d.models.MolecularModel;
import org.concord.mw2d.models.Molecule;
import org.concord.mw2d.models.MoleculeCollection;
import org.concord.mw2d.models.PointRestraint;
import org.concord.mw2d.models.Polypeptide;
import org.concord.mw2d.models.RadialBond;
import org.concord.mw2d.models.StructureFactor;

public class AtomContainer extends MDContainer implements RNATranscriptionListener, RNATranslationListener,
		MutationListener, ItemListener {

	protected AtomisticView view;
	protected MolecularModel model;

	private static Icon xrayIcon;
	private static Icon neutronScatteringIcon;
	private static Icon pcfIcon;
	private static Icon neutralContourIcon;
	private static Icon chargedContourIcon;
	private static Icon inputDNAIcon;
	private static Icon animationIcon;
	private static Icon efieldIcon;

	private MB mb;
	private TB tb;
	private FileChooser fileChooser;
	private DNAScrollerWithRNA dnaScroller;
	private String dnaString;
	private boolean contextDNA = true;
	private JPanel dnaPanel;
	private JPanel dnaButtonPanel;
	private JButton dnaPlayButton;
	private JButton dnaStopButton;
	private JButton dnaStepButton;
	private CodonTextField dnaField;
	private ProteinSynthesisModelProperties proSynProp;
	private DiatomicConfigure diatomicConfigure;
	private TriatomicConfigure triatomicConfigure;
	private ChainConfigure chainConfigure;
	private BoundarySetup boundarySetup;
	private JDialog growthModeDialog;
	private AtomFlowPanel atomFlowPanel;

	private Action xrayAction, neutronAction;
	private Action transcriptionAction, translationAction, translationAction2;
	private Action resetDNAScrollerAction, inputDNAAction, transcribeStepAction, translateStepAction, stopDNAAction;

	private Loadable translationNotifier;

	public AtomContainer() {
		super();
		if (prefs != null) {
			init(400, 250, prefs.getInt("Tape Length", 200));
		}
		else {
			init(400, 250, 200);
		}
	}

	public AtomContainer(int tapeLength) {
		super();
		init(400, 250, tapeLength);
	}

	private void createActions() {

		translationNotifier = new AbstractLoadable(1000) {
			public void execute() {
				dnaScroller.notifyTranslation();
			}

			public int getPriority() {
				return Thread.MIN_PRIORITY;
			}

			public String getName() {
				return "Translation Notifier";
			}

			public int getLifetime() {
				return ETERNAL;
			}

			public String getDescription() {
				return "This task notifies the translation of mRNA.";
			}
		};

		xrayAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				showDiffractionPattern("X-ray diffraction pattern", StructureFactor.X_RAY);
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		xrayAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_X);
		xrayAction.putValue(Action.NAME, "X-ray Spectrum");
		xrayAction.putValue(Action.SHORT_DESCRIPTION, "X-ray spectrum");
		xrayAction.putValue(Action.SMALL_ICON, xrayIcon);

		neutronAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				showDiffractionPattern("Neutron diffraction pattern", StructureFactor.NEUTRON);
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		neutronAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_N);
		neutronAction.putValue(Action.NAME, "Neutron-Scattering Spectrum");
		neutronAction.putValue(Action.SHORT_DESCRIPTION, "Neutron-scattering spectrum");
		neutronAction.putValue(Action.SMALL_ICON, neutronScatteringIcon);

		transcriptionAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (dnaScroller == null)
					return;
				// view.removeAllObjects();
				dnaScroller.setGotoTranslationAfterTranscription(false);
				dnaScroller.startTranscription();
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		transcriptionAction.putValue(Action.NAME, "Transcription");
		transcriptionAction.putValue(Action.SHORT_DESCRIPTION, "Transcribe DNA to mRNA");

		translationAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (dnaScroller == null)
					return;
				model.stopImmediately();
				dnaScroller.suspendSimulation();
				view.removeAllObjects();
				dnaScroller.startTranslation();
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		translationAction.putValue(Action.NAME, "Translation");
		translationAction.putValue(Action.SHORT_DESCRIPTION, "Translate mRNA to protein");

		translationAction2 = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (dnaScroller == null)
					return;
				model.stopImmediately();
				dnaScroller.suspendSimulation();
				dnaScroller.startTranslation();
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		translationAction2.putValue(Action.NAME, "Translation2");
		translationAction2.putValue(Action.SHORT_DESCRIPTION,
				"Translate mRNA to protein (without removing existing setup)");

		resetDNAScrollerAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (dnaScroller == null)
					return;
				model.stopImmediately();
				view.removeAllObjects();
				dnaScroller.reset();
				enableTranslationActions(true);
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		resetDNAScrollerAction.putValue(Action.NAME, "Reset Protein Synthesis Simulation");
		resetDNAScrollerAction.putValue(Action.SHORT_DESCRIPTION, "Reset protein synthesis simulation");

		inputDNAAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (dnaScroller == null)
					return;
				model.stopImmediately();
				inputDNAString();
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		inputDNAAction.putValue(Action.NAME, "Input DNA String");
		inputDNAAction.putValue(Action.SHORT_DESCRIPTION, "Input DNA string");

		transcribeStepAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (dnaScroller == null)
					return;
				if (dnaScroller.getScrollerState() == DNAScrollerWithRNA.SCROLLER_TRANSLATION_READY_STATE)
					return;
				dnaScroller.doOneStep();
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		transcribeStepAction.putValue(Action.NAME, "Transcribe Step By Step");
		transcribeStepAction.putValue(Action.SHORT_DESCRIPTION, "Transcribe step by step");

		translateStepAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (dnaScroller == null)
					return;
				if (dnaScroller.isTranslationEnded())
					return;
				if (!dnaScroller.isTranscriptionEnded()) {
					JOptionPane
							.showMessageDialog(
									JOptionPane.getFrameForComponent(view),
									"Translation cannot start without transcription, or before the\ncompletion of transcription.",
									"Message", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				dnaScroller.doOneStep();
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		translateStepAction.putValue(Action.NAME, "Translate Step By Step");
		translateStepAction.putValue(Action.SHORT_DESCRIPTION, "Translate step by step");

		stopDNAAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				dnaPlayButton.setEnabled(true);
				dnaStepButton.setEnabled(true);
				dnaScroller.suspendSimulation();
				view.getActionMap().get("Stop").actionPerformed(e);
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		stopDNAAction.putValue(Action.NAME, "Stop DNA Transcription or Translation");
		stopDNAAction.putValue(Action.SHORT_DESCRIPTION, "Stop DNA transcription or translation");

	}

	void loadImages() {
		super.loadImages();
		if (xrayIcon == null) {
			xrayIcon = new ImageIcon(AtomContainer.class.getResource("images/xray.gif"));
			neutronScatteringIcon = new ImageIcon(AtomContainer.class.getResource("images/neutronScattering.gif"));
			pcfIcon = new ImageIcon(AtomContainer.class.getResource("images/pcf.gif"));
			neutralContourIcon = new ImageIcon(AtomContainer.class.getResource("images/NeutralContour.gif"));
			chargedContourIcon = new ImageIcon(AtomContainer.class.getResource("images/ChargedContour.gif"));
			inputDNAIcon = new ImageIcon(AtomContainer.class.getResource("images/DNACode.gif"));
			animationIcon = new ImageIcon(AtomContainer.class.getResource("images/Animation.gif"));
			efieldIcon = new ImageIcon(AtomContainer.class.getResource("images/efield.gif"));
		}
	}

	public String getRepresentationName() {
		return getCompatibleName();
	}

	/** return a representation name backward compatible to Version 1.3 */
	public static String getCompatibleName() {
		return "org.concord.mw2d.activity.AtomContainer";
	}

	private void init(int width, int height, int tapeLength) {

		createActions();

		setLayout(new BorderLayout());

		view = new AtomisticView(); // view must be initialized before model for i18n.
		model = new MolecularModel(width, height, tapeLength);
		view.setModel(model);
		view.enablePopupMenu(true);
		view.addActionStateListener(this);
		Action a = new ShowEnergyAction(model);
		model.getActions().put((String) a.getValue(Action.SHORT_DESCRIPTION), a);

		setFileChooser(ModelerUtilities.fileChooser);
		createMenuBar();
		createMoviePanel();

		add(view, BorderLayout.CENTER);
		add(moviePanel, BorderLayout.SOUTH);

		model.getMovie().setCapacity(tapeLength);
		if (!model.hasEmbeddedMovie())
			initTape(tapeLength);

		view.setToolBar(createToolBar());

		model.getActions().put((String) resizeModelAction.getValue(Action.SHORT_DESCRIPTION), resizeModelAction);

	}

	private void initTape(int length) {
		model.getModelTimeQueue().setLength(length);
		for (int i = 0; i < model.getNumberOfParticles(); i++)
			model.getAtom(i).initializeMovieQ(length);
	}

	private void enableTranslationActions(boolean b) {
		translationAction.setEnabled(b);
		translationAction2.setEnabled(b);
	}

	/**
	 * When the parent of this container is closed, destroy it to prevent memory leak.
	 */
	public void destroy() {
		super.destroy();
		if (model != null)
			model.destroy();
		if (mb != null) {
			mb.destroy();
			mb = null;
		}
		xrayAction = null;
		neutronAction = null;
		translationNotifier = null;
	}

	public MDView getView() {
		return view;
	}

	public MDModel getModel() {
		return model;
	}

	public void setFileChooser(FileChooser fc) {
		fileChooser = fc;
	}

	public FileChooser getFileChooser() {
		return fileChooser;
	}

	public void setProgressBar(JProgressBar progressBar) {
		model.setIOProgressBar(progressBar);
	}

	public void disableGridMode() {
		if (mb != null)
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					mb.rbMenuItem0.setSelected(true);
				}
			});
	}

	public JMenuBar getMenuBar() {
		return mb;
	}

	public JMenuBar createMenuBar() {
		if (mb == null)
			mb = new MB(model);
		return mb;
	}

	public JPanel getToolBar() {
		return tb;
	}

	public boolean removeToolbar() {
		if (super.removeToolbar()) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					mb.removeToolBarItem.setEnabled(false);
				}
			});
			return true;
		}
		return false;
	}

	public boolean addToolbar() {
		if (super.addToolbar()) {
			if (isAuthorable()) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						mb.removeToolBarItem.setEnabled(true);
					}
				});
				return true;
			}
		}
		return false;
	}

	public JPanel createToolBar() {
		tb = new TB();
		return tb;
	}

	public void addBottomBar() {
		if (dnaString != null) {
			if (dnaPanel != null) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						dnaPanel.add(dnaButtonPanel, BorderLayout.SOUTH);
						validate();
					}
				});
			}
		}
		else {
			super.addBottomBar();
		}
	}

	public void removeBottomBar() {
		if (dnaString != null) {
			if (dnaPanel != null) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						dnaPanel.remove(dnaButtonPanel);
						validate();
					}
				});
			}
		}
		else {
			super.removeBottomBar();
		}
	}

	public boolean hasBottomBar() {
		if (dnaString != null && hasDNAScroller()) {
			int n = dnaPanel.getComponentCount();
			for (int i = 0; i < n; i++) {
				if (dnaButtonPanel == dnaPanel.getComponent(i))
					return true;
			}
			return false;
		}
		return super.hasBottomBar();
	}

	public int enableRecorder(final boolean b) {
		if (super.enableRecorder(b) == JOptionPane.NO_OPTION)
			return JOptionPane.NO_OPTION;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				mb.enableMovieMenuItems(b);
				mb.disableRecorderItem.setSelected(!b);
			}
		});
		return JOptionPane.YES_OPTION;
	}

	public void setDNAString(String s) {
		dnaString = s;
	}

	public String getDNAString() {
		return dnaString;
	}

	public void setDNAContextEnabled(boolean b) {
		contextDNA = b;
	}

	public boolean getDNAContextEnabled() {
		return contextDNA;
	}

	/**
	 * Caution: this method must be called in the event thread.
	 * 
	 * @throws RuntimeException
	 *             if this method is not called in the event thread.
	 */
	public void enableDNAScroller(boolean b) {

		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("must be called in event thread.");

		removeAll();

		if (b) {
			if (dnaPanel == null)
				createDNAPanel();
			model.getActions()
					.put((String) transcriptionAction.getValue(Action.SHORT_DESCRIPTION), transcriptionAction);
			model.getActions().put((String) translationAction.getValue(Action.SHORT_DESCRIPTION), translationAction);
			model.getActions().put((String) translationAction2.getValue(Action.SHORT_DESCRIPTION), translationAction2);
			model.getActions().put((String) inputDNAAction.getValue(Action.SHORT_DESCRIPTION), inputDNAAction);
			model.getActions().put((String) transcribeStepAction.getValue(Action.SHORT_DESCRIPTION),
					transcribeStepAction);
			model.getActions()
					.put((String) translateStepAction.getValue(Action.SHORT_DESCRIPTION), translateStepAction);
			model.getActions().put((String) stopDNAAction.getValue(Action.SHORT_DESCRIPTION), stopDNAAction);
			model.getActions().put((String) resetDNAScrollerAction.getValue(Action.SHORT_DESCRIPTION),
					resetDNAScrollerAction);
			Dimension dim = dnaPanel.getPreferredSize();
			CustomBorderLayout layout = new CustomBorderLayout(0, -dim.height / 2);
			layout.setOverlapableSide(BorderLayout.SOUTH);
			layout.setCenterComponent(view);
			layout.setSouthComponent(dnaPanel);
			if (hasToolbar())
				layout.setNorthComponent(tb);
			setLayout(layout);
			add(dnaPanel, BorderLayout.SOUTH);
			dnaScroller.reset();
			model.activateEmbeddedMovie(false);
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					if (dnaString != null) {
						dnaScroller.setDNA(new DNA(dnaString, contextDNA));
					}
					else {
						dnaScroller.setDNA(null);
					}
					if (dnaPlayButton != null)
						dnaPlayButton.setEnabled(true);
					mb.enableMovieMenuItems(false);
					ItemListener[] listener = mb.disableRecorderItem.getItemListeners();
					for (int i = 0; i < listener.length; i++) {
						mb.disableRecorderItem.removeItemListener(listener[i]);
					}
					mb.disableRecorderItem.setSelected(true);
					for (int i = 0; i < listener.length; i++) {
						mb.disableRecorderItem.addItemListener(listener[i]);
					}
				}
			});
		}
		else {
			model.getActions().remove(transcriptionAction.getValue(Action.SHORT_DESCRIPTION));
			model.getActions().remove(translationAction.getValue(Action.SHORT_DESCRIPTION));
			model.getActions().remove(translationAction2.getValue(Action.SHORT_DESCRIPTION));
			model.getActions().remove(inputDNAAction.getValue(Action.SHORT_DESCRIPTION));
			model.getActions().remove(transcribeStepAction.getValue(Action.SHORT_DESCRIPTION));
			model.getActions().remove(translateStepAction.getValue(Action.SHORT_DESCRIPTION));
			model.getActions().remove(stopDNAAction.getValue(Action.SHORT_DESCRIPTION));
			model.getActions().remove(resetDNAScrollerAction.getValue(Action.SHORT_DESCRIPTION));
			setLayout(new BorderLayout());
			if (isStatusBarShown()) {
				if (model.getRecorderDisabled()) {
					if (runPanel == null)
						createRunPanel();
					add(runPanel, BorderLayout.SOUTH);
				}
				else {
					add(moviePanel, BorderLayout.SOUTH);
				}
			}
		}

		add(view, BorderLayout.CENTER);
		if (hasToolbar())
			add(tb, BorderLayout.NORTH);

		validate();
		if (view.getAncestor() != null)
			view.getAncestor().validate();

	}

	public boolean hasDNAScroller() {
		int n = getComponentCount();
		for (int i = 0; i < n; i++) {
			if (getComponent(i) == dnaPanel)
				return true;
		}
		return false;
	}

	public void stopDNAAnimation() {
		if (dnaScroller == null)
			return;
		dnaScroller.suspendSimulation();
		enableTranslationActions(true);
	}

	public String getDNA() {
		if (dnaScroller == null)
			return null;
		if (dnaScroller.getModel() == null)
			return null;
		DNA dna = dnaScroller.getModel().getDNA();
		if (dna == null)
			return null;
		return dna.getCodingRegionAsString();
	}

	/** set the transcription animation speed: every step per i milliseconds. */
	public void setTranscriptionTimeStep(int i) {
		if (dnaScroller == null) {
			dnaScroller = new DNAScrollerWithRNA();
			dnaScroller.setMutationAfterTranslationDoneAllowed(true);
			dnaScroller.addItemListener(this);
			dnaScroller.addMutationListener(this);
		}
		dnaScroller.setTranscriptionDT(i);
	}

	/**
	 * get the transcription animation speed: every step per i milliseconds. Return -1, if there is no DNA scroller.
	 */
	public int getTranscriptionTimeStep() {
		if (dnaScroller == null)
			return -1;
		return dnaScroller.getTranscriptionDT();
	}

	/**
	 * set the translation animation speed: every translation step per i molecular dynamics steps.
	 */
	public void setTranslationMDStep(int i) {
		translationNotifier.setInterval(i);
	}

	/**
	 * get the translation animation speed: every translation step per i molecular dynamics steps.
	 */
	public int getTranslationMDStep() {
		return translationNotifier.getInterval();
	}

	public void resetScrollerParameters() {
		if (dnaScroller == null)
			return;
		dnaScroller.setTranscriptionDT(20);
		translationNotifier.setInterval(1000);
	}

	public void baseTranscripted(RNATranscriptionEvent e) {
		switch (e.getMode()) {
		case RNATranscriptionListener.MODE_TRANSCRIPTION_START:
			e.setConsumed(true);
			break;
		case RNATranscriptionListener.MODE_TRANSCRIPTION_END:
			e.setConsumed(true);
			break;
		}
	}

	public void itemStateChanged(ItemEvent e) {
		int i = e.getStateChange();
		if (i == ItemEvent.SELECTED) {
			if (model.getMolecules().isEmpty())
				return;
			Molecule m = model.getMolecules().get(0);
			if (m != null) {
				int startOffset = dnaScroller.getModel().getDNA().getOffsetToTheCodingRegion();
				int iat = (dnaScroller.getCurrentBase() - startOffset) / 3;
				if (iat < m.size())
					m.getAtom(iat).blink();
			}
		}
	}

	public void aminoacidAdded(final RNATranslationEvent e) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				addAminoAcid(e);
			}
		});
	}

	private void addAminoAcid(RNATranslationEvent e) {
		switch (e.getMode()) {
		case RNATranslationListener.MODE_TRANSLATION_START:
			enableTranslationActions(false);
			model.putProperty("NOA_Translation", new Integer(model.getNumberOfAtoms()));
			model.getJob().add(translationNotifier);
			e.setConsumed(true);
			break;
		case RNATranslationListener.MODE_TRANSLATION_END:
			enableTranslationActions(true);
			model.getJob().remove(translationNotifier);
			PointRestraint.releaseParticle(model.getAtom(model.getNumberOfAtoms() - 1));
			e.setConsumed(true);
			break;
		case RNATranslationListener.MODE_TRANSLATION_NEW_AMINO:
			Rectangle ribosomeArea = SwingUtilities
					.convertRectangle(dnaScroller.getParent(), e.getRibosomeRect(), view);
			boolean success = view.growPolypeptide(ribosomeArea.x + ribosomeArea.width / 3, ribosomeArea.y
					+ ribosomeArea.height / 2 - 8, Math.PI * 0.25, AminoAcidAdapter.getElementID(e.getAminoacid()));
			Atom at = model.getAtom(model.getNumberOfAtoms() - 1);
			if (at != null) {
				at.setCodon(e.getCodon().toString().replaceAll("U", "T"));
			}
			else {
				System.err.println("<ERROR> null atom in translation event: " + success);
			}
			if (model.getProperty("NOA_Translation") != null) {
				int n0 = ((Integer) model.getProperty("NOA_Translation")).intValue();
				if (model.getNumberOfAtoms() >= n0 + 1) {
					if (model.getJob().isStopped() || model.getJob().isTurnedOff()) {
						final Action play = view.getActionMap().get("Play");
						if (play != null)
							EventQueue.invokeLater(new Runnable() {
								public void run() {
									play.actionPerformed(null);
								}
							});
					}
				}
			}
			e.setConsumed(model.getNumberOfAtoms() <= 0);
			break;
		}
	}

	public void mutationOccurred(MutationEvent e) {
		if (model.getMolecules().isEmpty())
			return;
		Molecule m = model.getMolecules().get(0);
		if (m == null)
			return;
		if (dnaScroller == null)
			return;
		DNA dna = dnaScroller.getModel().getDNA();
		int offset = dna.getOffsetToTheCodingRegion();
		Object src = e.getSource();
		int i = e.getNucleotideIndex() - offset;
		Atom a = m.getAtom(i / 3);
		if (src instanceof SubstitutionMutator) {
			int strand = e.getStrandIndex();
			if (strand == DNA.DNA_STRAND_53) {
				char[] q = a.getCodon().toCharArray();
				q[i % 3] = e.getNewNucleotide().getName();
				if (Codon.isStopCodon(q)) {
					List<Integer> list = new ArrayList<Integer>();
					for (int k = m.indexOfAtom(a); k < m.size(); k++)
						list.add(m.getAtom(k).getIndex());
					view.removeMarkedAtoms(list);
				}
				else {
					a.setElement(model.getElement(Codon.express(q).getAbbreviation()));
					a.setCodon(new String(q));
					adjustBondLength(a);
					view.paintImmediately(a.getBounds(10));
				}
			}
			else {
				char[] q = Codon.getComplementaryCode(a.getCodon().toCharArray());
				q[i % 3] = e.getNewNucleotide().getName();
				char[] q1 = Codon.getComplementaryCode(q);
				if (Codon.isStopCodon(q1)) {
					List<Integer> list = new ArrayList<Integer>();
					for (int k = m.indexOfAtom(a); k < m.size(); k++)
						list.add(m.getAtom(k).getIndex());
					view.removeMarkedAtoms(list);
					view.repaint();
				}
				else {
					a.setElement(model.getElement(Codon.express(q1).getAbbreviation()));
					a.setCodon(new String(q1));
					adjustBondLength(a);
					view.paintImmediately(a.getBounds(10));
				}
			}
		}
		else if (src instanceof InsertionMutator || src instanceof DeletionMutator) {
			int startOffset = dnaScroller.getModel().getDNA().getOffsetToTheCodingRegion();
			int endOffset = startOffset + dnaScroller.getModel().getDNA().getLengthOfTheCodingRegion();
			org.concord.molbio.engine.Codon c;
			int iStop = -1;
			for (int k = startOffset / 3, p = endOffset / 3; k < p; k++) {
				c = dnaScroller.getModel().get53Codon(k * 3);
				if (c.isCodonStop()) {
					iStop = k;
					break;
				}
			}
			if (iStop != -1) {
				List<Integer> list = new ArrayList<Integer>();
				for (int k = iStop; k < m.size(); k++)
					list.add(m.getAtom(k).getIndex());
				view.removeMarkedAtoms(list);
				view.repaint();
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						fillDNA();
					}
				});
			}
			else {
				String s = dnaScroller.getModel().getFullDNA53String();
				String s2 = s.substring(0, s.length() - 1);
				final DNA dna2 = new DNA(s2, contextDNA);
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						dnaScroller.setDNA(dna2);
					}
				});
			}
		}
		model.notifyChange();
	}

	private void adjustBondLength(Atom a) {
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

	private void fillDNA() {
		MoleculeCollection c = model.getMolecules();
		if (c.isEmpty()) {
			dnaScroller.setDNA(null);
			return;
		}
		Molecule mol = c.get(0);
		if (mol instanceof Polypeptide) {
			String s = ((Polypeptide) mol).getDNACode();
			if (s == null) {
				dnaScroller.setDNA(null);
				return;
			}
			dnaScroller.setDNA(new DNA(s, contextDNA));
		}
	}

	private boolean inputDNAString() {
		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("must be called in the event thread.");
		if (dnaField == null) {
			dnaField = new CodonTextField();
			dnaField.setPreferredSize(new Dimension(250, 20));
		}
		try {
			dnaField.setText(dnaScroller.getModel().getDNA().getCodingRegionAsString());
		}
		catch (NullPointerException npe) {
			dnaField.setText(null);
		}
		if (JOptionPane.showConfirmDialog(view, dnaField, "Type/copy/paste DNA code on sense (5'-3') strand",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
			dnaPlayButton.setEnabled(true);
			dnaScroller.reset();
			dnaScroller.setDNA(new DNA(dnaField.getText(), contextDNA));
			dnaScroller.repaint();
			return true;
		}
		return false;
	}

	public void createDNAPanel() {

		if (dnaPanel != null)
			return;

		dnaPanel = new JPanel(new BorderLayout(0, 0));
		dnaPanel.setOpaque(false);
		if (dnaScroller == null) {
			dnaScroller = new DNAScrollerWithRNA();
			dnaScroller.setTranscriptionDT(20);
		}
		dnaScroller.setOpaque(false);
		dnaScroller.setHighlightColor(Color.green);
		dnaScroller.setTranslationDT(100);
		dnaScroller.setStartTranslationEffectDt(10);
		dnaScroller.setStartTranscriptionEffectDt(10);
		dnaScroller.addRNATranscriptionListener(this);
		dnaScroller.addRNATranslationListener(this);
		dnaScroller.setStartTranscriptionWithEffect(true);
		dnaScroller.setStartTranslationWithEffect(true);
		dnaScroller.setGotoTranslationAfterTranscription(true);
		dnaScroller.reset();
		CustomBevelBorder border = new CustomBevelBorder(CustomBevelBorder.LOWERED);
		border.hideSide(BorderLayout.NORTH);
		dnaPanel.setBorder(border);
		dnaPanel.add(dnaScroller, BorderLayout.CENTER);

		Dimension buttonDimension = new Dimension(20, 20);

		dnaButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		dnaPanel.add(dnaButtonPanel, BorderLayout.SOUTH);

		JButton button = new JButton(animationIcon);
		button.setPreferredSize(buttonDimension);
		button.setToolTipText("Customize protein synthesis animations");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (proSynProp == null)
					proSynProp = new ProteinSynthesisModelProperties(AtomContainer.this);
				proSynProp.createDialog(AtomContainer.this).setVisible(true);
			}
		});
		dnaButtonPanel.add(button);

		button = new JButton(inputDNAIcon);
		button.setToolTipText("Type, copy or paste the DNA code on the sense strand (the 5'-3' strand)");
		button.setPreferredSize(buttonDimension);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				inputDNAString();
				view.removeAllObjects();
			}
		});
		dnaButtonPanel.add(button);

		final Action reset = view.getActionMap().get("Reload");
		button = new JButton((Icon) reset.getValue(Action.SMALL_ICON));
		button.setPreferredSize(buttonDimension);
		button.setToolTipText("Reset");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				enableTranslationActions(true);
				dnaPlayButton.setEnabled(true);
				dnaScroller.reset();
				reset.actionPerformed(e);
			}
		});
		dnaButtonPanel.add(button);

		final Action stop = view.getActionMap().get("Stop");
		dnaStopButton = new JButton((Icon) stop.getValue(Action.SMALL_ICON));
		dnaStopButton.setPreferredSize(buttonDimension);
		dnaStopButton.setToolTipText("Pause");
		dnaStopButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dnaPlayButton.setEnabled(true);
				dnaStepButton.setEnabled(true);
				dnaScroller.suspendSimulation();
				stop.actionPerformed(e);
			}
		});
		dnaButtonPanel.add(dnaStopButton);

		dnaStepButton = new JButton(new ImageIcon(ModelerUtilities.class.getResource("images/StepForward.gif")));
		dnaStepButton.setPreferredSize(buttonDimension);
		dnaStepButton.setToolTipText("Step by step");
		dnaStepButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dnaScroller.doOneStep();
			}
		});
		dnaButtonPanel.add(dnaStepButton);

		final Action play = view.getActionMap().get("Play");
		dnaPlayButton = new JButton((Icon) play.getValue(Action.SMALL_ICON));
		dnaPlayButton.setToolTipText("Run");
		dnaPlayButton.setPreferredSize(buttonDimension);
		dnaPlayButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dnaScroller.setGotoTranslationAfterTranscription(true);
				dnaPlayButton.setEnabled(false);
				dnaStepButton.setEnabled(false);
				if (!dnaScroller.isTranscriptionEnded()) {
					dnaScroller.resumeSimulation();
				}
				else {
					if (!dnaScroller.isTranslationEnded()) {
						dnaScroller.resumeSimulation();
					}
					if (model.getJob().isStopped() || model.getJob().isTurnedOff()) {
						Action play = view.getActionMap().get("Play");
						if (play != null)
							play.actionPerformed(e);
					}
				}
			}
		});
		dnaButtonPanel.add(dnaPlayButton);

	}

	private void showDiffractionPattern(String title, final int type) {
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(view), title, false);
		dialog.getContentPane().setLayout(new BorderLayout());
		dialog.setResizable(false);
		final JLabel label = new JLabel("Calculating......", SwingConstants.CENTER);
		label.setPreferredSize(new Dimension(300, 200));
		dialog.getContentPane().add(label, BorderLayout.CENTER);
		dialog.pack();
		dialog.setLocationRelativeTo(this);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dialog.dispose();
			}

			public void windowOpened(WindowEvent e) {
				new SwingWorker() {
					public Object construct() {
						DiffractionInstrument d = new DiffractionInstrument(true);
						d.setModel(model);
						d.setType(type);
						d.createImage();
						return d;
					}

					public void finished() {
						dialog.getContentPane().remove(label);
						DiffractionInstrument d = (DiffractionInstrument) getValue();
						d.setOwner(dialog);
						dialog.getContentPane().add(d, BorderLayout.CENTER);
						dialog.pack();
						view.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					}
				}.start();
			}
		});
		dialog.setVisible(true);
	}

	/** inner classes */

	private class MB extends SimulatorMenuBar {

		JRadioButtonMenuItem rbMenuItem0;
		JMenu gridMenu;
		JMenuItem movieTSItem;
		JMenuItem energyTSItem;
		JMenuItem disableRecorderItem;
		JMenuItem removeToolBarItem;
		JMenuItem computeMSDMenuItem;
		JMenuItem computePhotonMenuItem;
		JMenuItem setupFlowMenuItem;
		JMenuItem enableFlowMenuItem;
		JMenuItem eFieldLineMenuItem;

		private void enableMovieMenuItems(boolean b) {
			movieTSItem.setEnabled(b);
			energyTSItem.setEnabled(b);
		}

		MB(MolecularModel m) {

			super(m);

			/* file menu */

			add(createFileMenu());

			/* edit menu */
			JMenu menu = createEditMenu();
			add(menu);

			String s = getInternationalText("LightMatterInteraction");
			JMenu subMenu = new JMenu(s != null ? s : "Light-Matter Interactions");
			subMenu.setMnemonic(KeyEvent.VK_L);
			subMenu.setIcon(new ImageIcon(getClass().getResource("images/LMI.gif")));
			menu.add(subMenu);
			enabledComponentsWhenEditable.add(subMenu);

			JMenuItem menuItem = new JMenuItem(model.getActions().get("Edit the light source"));
			s = getInternationalText("LightSource");
			menuItem.setText((s != null ? s : "Light Source") + "...");
			menuItem.setIcon(null);
			subMenu.add(menuItem);

			menuItem = new JMenuItem(model.getActions().get("Edit rules of electronic dynamics"));
			s = getInternationalText("ElectronicDynamicsRules");
			menuItem.setText((s != null ? s : "Rules of Electronic Dynamics") + "...");
			menuItem.setIcon(null);
			subMenu.add(menuItem);

			/* tool bar menu */

			removeToolBarItem = new JMenuItem("Remove Toolbar");
			add(createToolBarMenu(removeToolBarItem));

			/* compute menu */

			s = getInternationalText("Compute");
			menu = new JMenu(s != null ? s : "Compute");
			menu.setMnemonic(KeyEvent.VK_C);
			menu.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
				public void popupMenuCanceled(PopupMenuEvent e) {
				}

				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					setMenuItemWithoutNotifyingListeners(computeMSDMenuItem, model.isComputed(MDModel.COMPUTE_MSD));
					setMenuItemWithoutNotifyingListeners(computePhotonMenuItem, model.isPhotonEnabled());
				}

				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				}
			});
			add(menu);

			computeMSDMenuItem = new JCheckBoxMenuItem(MDModel.COMPUTE_MSD);
			computeMSDMenuItem.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						model.addCompute(MDModel.COMPUTE_MSD);
					}
					else {
						model.removeCompute(MDModel.COMPUTE_MSD);
					}
				}
			});
			menu.add(computeMSDMenuItem);

			computePhotonMenuItem = new JCheckBoxMenuItem(MolecularModel.COMPUTE_PHOTON);
			computePhotonMenuItem.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					model.setPhotonEnabled(e.getStateChange() == ItemEvent.SELECTED);
				}
			});
			menu.add(computePhotonMenuItem);

			// menuItem=new JCheckBoxMenuItem("Ionization"); menuItem.setEnabled(false); menu.add(menuItem);
			// menuItem=new JCheckBoxMenuItem("Electron Transfer"); menuItem.setEnabled(false); menu.add(menuItem);

			/* analysis menu */

			s = getInternationalText("Analysis");
			menu = new JMenu(s != null ? s : "Analysis");
			menu.setMnemonic(KeyEvent.VK_A);
			add(menu);

			s = getInternationalText("Grid");
			gridMenu = new JMenu(s != null ? s : "Grid");
			gridMenu.setMnemonic(KeyEvent.VK_G);
			menu.add(gridMenu);

			ButtonGroup bg = new ButtonGroup();

			s = getInternationalText("NoGrid");
			rbMenuItem0 = new JRadioButtonMenuItem(s != null ? s : "No Grid");
			rbMenuItem0.setMnemonic(KeyEvent.VK_N);
			rbMenuItem0.setSelected(true);
			rbMenuItem0.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					view.setGridMode((byte) -1);
					model.setupGrid(0, 0);
					view.repaint();
				}
			});
			gridMenu.add(rbMenuItem0);
			bg.add(rbMenuItem0);

			s = getInternationalText("AtomsOnGrid");
			JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem(s != null ? s : "Atoms on Grid", new ImageIcon(
					getClass().getResource("images/gridShow.gif")));
			rbMenuItem.setMnemonic(KeyEvent.VK_A);
			rbMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					view.setGridMode(Grid.ATOMIC);
					model.setupGrid();
					view.repaint();
				}
			});
			gridMenu.add(rbMenuItem);
			bg.add(rbMenuItem);

			s = getInternationalText("DensityDistribution");
			rbMenuItem = new JRadioButtonMenuItem(s != null ? s : "Density Distribution", new ImageIcon(getClass()
					.getResource("images/gridDens.gif")));
			rbMenuItem.setMnemonic(KeyEvent.VK_D);
			rbMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					view.setGridMode(Grid.DENSITY);
					model.setupGrid();
					view.repaint();
				}
			});
			gridMenu.add(rbMenuItem);
			bg.add(rbMenuItem);

			s = getInternationalText("MassDistribution");
			rbMenuItem = new JRadioButtonMenuItem(s != null ? s : "Mass Distribution", new ImageIcon(getClass()
					.getResource("images/gridMass.gif")));
			rbMenuItem.setMnemonic(KeyEvent.VK_M);
			rbMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					view.setGridMode(Grid.MASS);
					model.setupGrid();
					view.repaint();
				}
			});
			gridMenu.add(rbMenuItem);
			bg.add(rbMenuItem);

			s = getInternationalText("ChargeDistribution");
			rbMenuItem = new JRadioButtonMenuItem(s != null ? s : "Charge Distribution", new ImageIcon(getClass()
					.getResource("images/gridChar.gif")));
			rbMenuItem.setMnemonic(KeyEvent.VK_C);
			rbMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					view.setGridMode(Grid.CHARGE);
					model.setupGrid();
					view.repaint();
				}
			});
			gridMenu.add(rbMenuItem);
			bg.add(rbMenuItem);

			s = getInternationalText("TemperatureDistribution");
			rbMenuItem = new JRadioButtonMenuItem(s != null ? s : "Temperature Distribution", new ImageIcon(getClass()
					.getResource("images/gridKine.gif")));
			rbMenuItem.setMnemonic(KeyEvent.VK_T);
			rbMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					view.setGridMode(Grid.TEMPERATURE);
					model.setupGrid();
					view.repaint();
				}
			});
			gridMenu.add(rbMenuItem);
			bg.add(rbMenuItem);

			s = getInternationalText("VelocityDistribution");
			rbMenuItem = new JRadioButtonMenuItem(s != null ? s : "Velocity Distribution", new ImageIcon(getClass()
					.getResource("images/gridVelo.gif")));
			rbMenuItem.setMnemonic(KeyEvent.VK_V);
			rbMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					view.setGridMode(Grid.VELOCITY);
					model.setupGrid();
					view.repaint();
				}
			});
			gridMenu.add(rbMenuItem);
			bg.add(rbMenuItem);

			s = getInternationalText("ForceDistribution");
			rbMenuItem = new JRadioButtonMenuItem(s != null ? s : "Force Distribution", new ImageIcon(getClass()
					.getResource("images/gridStrs.gif")));
			rbMenuItem.setMnemonic(KeyEvent.VK_F);
			rbMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					view.setGridMode(Grid.FORCE);
					model.setupGrid();
					view.repaint();
				}
			});
			gridMenu.add(rbMenuItem);
			bg.add(rbMenuItem);

			s = getInternationalText("Crystallography");
			subMenu = new JMenu(s != null ? s : "Crystallography");
			subMenu.setMnemonic(KeyEvent.VK_C);
			menu.add(subMenu);

			menuItem = new JMenuItem(xrayAction);
			s = getInternationalText("Xray");
			menuItem.setText((s != null ? s : "X-ray Spectrum") + "...");
			subMenu.add(menuItem);

			menuItem = new JMenuItem(neutronAction);
			s = getInternationalText("Neutron");
			menuItem.setText((s != null ? s : "Neutron-Scattering Spectrum") + "...");
			subMenu.add(menuItem);

			s = getInternationalText("SpeedDistribution");
			subMenu = new JMenu(s != null ? s : "Speed Distribution Functions");
			menu.add(subMenu);

			menuItem = new JMenuItem(model.getActions().get("Show Maxwell speed distribution function: Nt"));
			s = getInternationalText("NtSpeedDistribution");
			menuItem.setText((s != null ? s : menuItem.getText()) + "...");
			subMenu.add(menuItem);

			menuItem = new JMenuItem(model.getActions().get("Show Maxwell speed distribution function: Pl"));
			s = getInternationalText("PlSpeedDistribution");
			menuItem.setText((s != null ? s : menuItem.getText()) + "...");
			subMenu.add(menuItem);

			menuItem = new JMenuItem(model.getActions().get("Show Maxwell speed distribution function: Ws"));
			s = getInternationalText("WsSpeedDistribution");
			menuItem.setText((s != null ? s : menuItem.getText()) + "...");
			subMenu.add(menuItem);

			menuItem = new JMenuItem(model.getActions().get("Show Maxwell speed distribution function: Ck"));
			s = getInternationalText("CkSpeedDistribution");
			menuItem.setText((s != null ? s : menuItem.getText()) + "...");
			subMenu.add(menuItem);

			s = getInternationalText("VelocityDistribution");
			subMenu = new JMenu(s != null ? s : "Velocity Distribution Functions");
			menu.add(subMenu);

			menuItem = new JMenuItem(model.getActions().get("Show Maxwell velocity distribution function: Nt"));
			s = getInternationalText("NtVelocityDistribution");
			menuItem.setText((s != null ? s : menuItem.getText()) + "...");
			subMenu.add(menuItem);

			menuItem = new JMenuItem(model.getActions().get("Show Maxwell velocity distribution function: Pl"));
			s = getInternationalText("PlVelocityDistribution");
			menuItem.setText((s != null ? s : menuItem.getText()) + "...");
			subMenu.add(menuItem);

			menuItem = new JMenuItem(model.getActions().get("Show Maxwell velocity distribution function: Ws"));
			s = getInternationalText("WsVelocityDistribution");
			menuItem.setText((s != null ? s : menuItem.getText()) + "...");
			subMenu.add(menuItem);

			menuItem = new JMenuItem(model.getActions().get("Show Maxwell velocity distribution function: Ck"));
			s = getInternationalText("CkVelocityDistribution");
			menuItem.setText((s != null ? s : menuItem.getText()) + "...");
			subMenu.add(menuItem);

			s = getInternationalText("PairCorrelationFunction");
			subMenu = new JMenu(s != null ? s : "Pair Correlation Functions");
			menu.add(subMenu);

			for (byte i = 0; i < 4; i++) {
				for (byte j = i; j < 4; j++) {
					menuItem = new JMenuItem((Element.idToName(i) + "-" + Element.idToName(j)) + "...");
					menuItem.setIcon(pcfIcon);
					final byte i2 = i;
					final byte j2 = j;
					menuItem.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							int rmax = (int) (Math
									.max(model.getElement(i2).getSigma(), model.getElement(j2).getSigma()) * .5);
							model.runScript("pcf " + i2 + " " + j2 + " " + rmax);
						}
					});
					subMenu.add(menuItem);
				}
			}

			s = getInternationalText("ForceFieldVisualization");
			subMenu = new JMenu(s != null ? s : "Force Field Visualization");
			subMenu.setMnemonic(KeyEvent.VK_F);
			subMenu.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
				public void popupMenuCanceled(PopupMenuEvent e) {
				}

				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					setMenuItemWithoutNotifyingListeners(eFieldLineMenuItem, view.eFieldLinesShown());
				}

				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				}
			});
			menu.add(subMenu);

			s = getInternationalText("EField");
			eFieldLineMenuItem = new JCheckBoxMenuItem(s != null ? s : "Electric Field");
			eFieldLineMenuItem.setIcon(efieldIcon);
			eFieldLineMenuItem.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					view.showEFieldLines(e.getStateChange() == ItemEvent.SELECTED, view.getCellSizeForEFieldLines());
					view.repaint();
				}
			});
			subMenu.add(eFieldLineMenuItem);

			s = getInternationalText("PotentialFieldContourPlot");
			subMenu = new JMenu(s != null ? s : "Potential Field Contour Plot");
			subMenu.setMnemonic(KeyEvent.VK_E);
			menu.add(subMenu);

			bg = new ButtonGroup();

			menuItem = new JMenuItem(new PlotContourAction(Element.ID_NT, 0));
			menuItem.setText("Neutral probe Nt");
			s = getInternationalText("NeutralProbeNt");
			if (s != null)
				menuItem.setText(s);
			menuItem.setMnemonic(KeyEvent.VK_N);
			menuItem.setIcon(neutralContourIcon);
			subMenu.add(menuItem);
			bg.add(menuItem);

			menuItem = new JMenuItem(new PlotContourAction(Element.ID_PL, 0));
			menuItem.setText("Neutral probe Pl");
			s = getInternationalText("NeutralProbePl");
			if (s != null)
				menuItem.setText(s);
			menuItem.setMnemonic(KeyEvent.VK_P);
			menuItem.setIcon(neutralContourIcon);
			subMenu.add(menuItem);
			bg.add(menuItem);

			menuItem = new JMenuItem(new PlotContourAction(Element.ID_WS, 0));
			menuItem.setText("Neutral probe Ws");
			s = getInternationalText("NeutralProbeWs");
			if (s != null)
				menuItem.setText(s);
			menuItem.setMnemonic(KeyEvent.VK_W);
			menuItem.setIcon(neutralContourIcon);
			subMenu.add(menuItem);
			bg.add(menuItem);

			menuItem = new JMenuItem(new PlotContourAction(Element.ID_CK, 0));
			menuItem.setText("Neutral probe Ck");
			s = getInternationalText("NeutralProbeCk");
			if (s != null)
				menuItem.setText(s);
			menuItem.setMnemonic(KeyEvent.VK_C);
			menuItem.setIcon(neutralContourIcon);
			subMenu.add(menuItem);
			bg.add(menuItem);
			subMenu.addSeparator();

			menuItem = new JMenuItem(new PlotContourAction(Element.ID_NT, 1));
			menuItem.setText("Probe Nt +e");
			s = getInternationalText("ChargedProbeNt");
			if (s != null)
				menuItem.setText(s);
			menuItem.setMnemonic(KeyEvent.VK_T);
			menuItem.setIcon(chargedContourIcon);
			subMenu.add(menuItem);
			bg.add(menuItem);

			menuItem = new JMenuItem(new PlotContourAction(Element.ID_PL, 1));
			menuItem.setText("Probe Pl +e");
			s = getInternationalText("ChargedProbePl");
			if (s != null)
				menuItem.setText(s);
			menuItem.setMnemonic(KeyEvent.VK_L);
			menuItem.setIcon(chargedContourIcon);
			subMenu.add(menuItem);
			bg.add(menuItem);

			menuItem = new JMenuItem(new PlotContourAction(Element.ID_WS, 1));
			menuItem.setText("Probe Ws +e");
			s = getInternationalText("ChargedProbeWs");
			if (s != null)
				menuItem.setText(s);
			menuItem.setMnemonic(KeyEvent.VK_S);
			menuItem.setIcon(chargedContourIcon);
			subMenu.add(menuItem);
			bg.add(menuItem);

			menuItem = new JMenuItem(new PlotContourAction(Element.ID_CK, 1));
			menuItem.setText("Probe Ck +e");
			s = getInternationalText("ChargedProbeCk");
			if (s != null)
				menuItem.setText(s);
			menuItem.setMnemonic(KeyEvent.VK_K);
			menuItem.setIcon(chargedContourIcon);
			subMenu.add(menuItem);
			bg.add(menuItem);

			/* option menu */

			s = getInternationalText("Option");
			menu = new JMenu(s != null ? s : "Options");
			menu.setMnemonic(KeyEvent.VK_O);
			menu.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
				public void popupMenuCanceled(PopupMenuEvent e) {
				}

				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					setMenuItemWithoutNotifyingListeners(enableFlowMenuItem, model.isAtomFlowEnabled());
					setupFlowMenuItem.setEnabled(model.getRecorderDisabled() && model.isAtomFlowEnabled());
					enableFlowMenuItem.setEnabled(model.getRecorderDisabled());
					disableRecorderItem.setEnabled(!model.hasGraphs() && !hasDNAScroller()
							&& !model.isAtomFlowEnabled());
				}

				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				}
			});
			add(menu);

			s = getInternationalText("DisableRecorder");
			disableRecorderItem = new JCheckBoxMenuItem(s != null ? s : "Disable Recorder");
			disableRecorderItem.setMnemonic(KeyEvent.VK_D);
			disableRecorderItem.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (enableRecorder(e.getStateChange() == ItemEvent.DESELECTED) == JOptionPane.NO_OPTION) {
						ItemListener[] lis = disableRecorderItem.getItemListeners();
						for (int i = 0; i < lis.length; i++)
							disableRecorderItem.removeItemListener(lis[i]);
						disableRecorderItem.setSelected(false);
						for (int i = 0; i < lis.length; i++)
							disableRecorderItem.addItemListener(lis[i]);
					}
					model.notifyChange();
				}
			});
			menu.add(disableRecorderItem);

			s = getInternationalText("EnableFlow");
			enableFlowMenuItem = new JCheckBoxMenuItem(s != null ? s : "Enable Flow");
			enableFlowMenuItem.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					model.setAtomFlowEnabled(e.getStateChange() == ItemEvent.SELECTED);
				}
			});
			menu.add(enableFlowMenuItem);

			s = getInternationalText("ShowActionTip");
			menuItem = new JCheckBoxMenuItem(s != null ? s : "Show Action Tip");
			menuItem.setMnemonic(KeyEvent.VK_A);
			menuItem.setSelected(true);
			menuItem.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					view.setActionTipEnabled(e.getStateChange() == ItemEvent.SELECTED);
				}
			});
			menu.add(menuItem);
			menu.addSeparator();

			menuItem = new JMenuItem(view.getActionMap().get("Snapshot"));
			s = getInternationalText("Snapshot");
			menuItem.setText((s != null ? s : menuItem.getText()) + "...");
			menuItem.setIcon(null);
			menu.add(menuItem);
			menu.addSeparator();

			s = getInternationalText("FlowControlPanel");
			setupFlowMenuItem = new JMenuItem((s != null ? s : "Flow Control Panel") + "...");
			setupFlowMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (atomFlowPanel == null)
						atomFlowPanel = new AtomFlowPanel(model);
					atomFlowPanel.createDialog().setVisible(true);
				}
			});
			menu.add(setupFlowMenuItem);

			s = getInternationalText("AutomaticReminder");
			menuItem = new JMenuItem((s != null ? s : "Set Up Automatic Reminder") + "...");
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					model.setupAutomaticReminder();
				}
			});
			menu.add(menuItem);

			menuItem = new JMenuItem(view.getActionMap().get("View Options"));
			s = getInternationalText("ViewOption");
			menuItem.setText((s != null ? s : menuItem.getText()) + "...");
			menuItem.setIcon(null);
			menu.add(menuItem);

			s = getInternationalText("TaskManager");
			menuItem = new JMenuItem((s != null ? s : "Task Manager") + "...");
			menuItem.setMnemonic(KeyEvent.VK_M);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					view.showTaskManager();
				}
			});
			menu.add(menuItem);

			s = getInternationalText("TimeSeries");
			movieTSItem = new JMenuItem((s != null ? s : "Access Time Series") + "...");
			movieTSItem.setMnemonic(KeyEvent.VK_T);
			movieTSItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (model == null)
						return;
					model.showTimeSeries();
				}
			});
			menu.add(movieTSItem);
			menu.addSeparator();

			s = getInternationalText("ToolBox");
			subMenu = new JMenu(s != null ? s : "Toolbox");
			subMenu.setMnemonic(KeyEvent.VK_T);
			menu.add(subMenu);

			menuItem = new JMenuItem(view.getActionMap().get("Energizer"));
			s = getInternationalText("HeatCool");
			if (s != null)
				menuItem.setText(s);
			subMenu.add(menuItem);

			menuItem = new JMenuItem(view.getActionMap().get("Heat Bath"));
			s = getInternationalText("HeatBath");
			menuItem.setText((s != null ? s : menuItem.getText()) + "...");
			subMenu.add(menuItem);

			s = getInternationalText("EnergyMinimization");
			menuItem = new JMenuItem((s != null ? s : "Run Energy Minimization") + "...");
			menuItem.setMnemonic(KeyEvent.VK_R);
			menuItem.setIcon(IconPool.getIcon("steepest descent"));
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					AAFFMinimizer m = new AAFFMinimizer(model);
					m.pack();
					m.setLocationRelativeTo(view);
					m.setVisible(true);
				}
			});
			subMenu.add(menuItem);

			energyTSItem = new JMenuItem(model.getActions().get("Show kinetic, potential and total energies"));
			energyTSItem.setMnemonic(KeyEvent.VK_V);
			s = getInternationalText("EnergyTimeSeries");
			energyTSItem.setText((s != null ? s : "View Time Series of Energies") + "...");
			subMenu.add(energyTSItem);

			/*
			 * menuItem=new JMenuItem("Quasiharmonic Analysis"); menuItem.addActionListener(new ActionListener(){ public
			 * void actionPerformed(ActionEvent e){
			 * org.concord.mw2d.models.CovarianceMatrix.sharedInstance().generateMatrix(model); } });
			 * subMenu.add(menuItem);
			 */

			threadPreempt();

			addMouseListener(new MouseAdapter() {
				private boolean popupTrigger;

				public void mousePressed(MouseEvent e) {
					popupTrigger = e.isPopupTrigger();
				}

				public void mouseReleased(MouseEvent e) {
					if (popupTrigger || e.isPopupTrigger())
						defaultPopupMenu.show(MB.this, e.getX(), e.getY());
				}
			});

		}
	}

	public void addDefaultToolBar() {
		super.addDefaultToolBar();
		addToolBarButton(tb.selectObjectButton);
		addToolBarButton(tb.removeObjectsButton);
		addToolBarButton(tb.duplicateButton);
		addToolBarButton(tb.rotateObjectButton);
		addToolBarButton(tb.dropNtButton);
		addToolBarButton(tb.dropPlButton);
		addToolBarButton(tb.dropWsButton);
		addToolBarButton(tb.dropCkButton);
		addToolBarButton(tb.heatButton);
		addToolBarButton(tb.coolButton);
	}

	private class TB extends ToolBar {

		private AbstractButton dropNtButton;
		private AbstractButton dropPlButton;
		private AbstractButton dropWsButton;
		private AbstractButton dropCkButton;
		private AbstractButton dropDiatomicButton;
		private AbstractButton dropTriatomicButton;
		private AbstractButton dropBenzeneButton;
		private AbstractButton dropChainButton;
		private AbstractButton fillAreaWithNtButton;
		private AbstractButton fillAreaWithPlButton;
		private AbstractButton fillAreaWithWsButton;
		private AbstractButton fillAreaWithCkButton;
		private AbstractButton dropRectangularSurfaceButton;
		private AbstractButton dropRectangularSplineButton;
		private AbstractButton dropCircularSurfaceButton;
		private AbstractButton dropCircularSplineButton;
		private AbstractButton dropCurvedSurfaceButton;
		private AbstractButton dropCurvedSplineButton;
		private AbstractButton dropFreeFormSurfaceButton;
		private AbstractButton dropRectangleButton;
		private AbstractButton setBoundaryButton;
		private AbstractButton buildBondButton;
		private AbstractButton buildBendButton;
		private AbstractButton mutateButton;
		private AbstractButton attachAminoAcidButton;
		private AbstractButton detachAminoAcidButton;
		private AbstractButton attachNucleotideButton;
		private AbstractButton detachNucleotideButton;

		TB() {

			super();

			/** drop Nt */
			dropNtButton = createButton(UserAction.getAction(UserAction.ADDA_ID, model));
			final Runnable runNt = new Runnable() {
				public void run() {
					view.editElements(Element.ID_NT).actionPerformed(null);
				}
			};
			dropNtButton.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() >= 2)
						runNt.run();
				}
			});
			customizationAction.put(dropNtButton.getAction().getValue(Action.SHORT_DESCRIPTION), runNt);
			toolBarButtonGroup.add(dropNtButton);

			/** drop Pl */
			dropPlButton = createButton(UserAction.getAction(UserAction.ADDB_ID, model));
			final Runnable runPl = new Runnable() {
				public void run() {
					view.editElements(Element.ID_PL).actionPerformed(null);
				}
			};
			dropPlButton.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() >= 2)
						runPl.run();
				}
			});
			customizationAction.put(dropPlButton.getAction().getValue(Action.SHORT_DESCRIPTION), runPl);
			toolBarButtonGroup.add(dropPlButton);

			/** drop Ws */
			dropWsButton = createButton(UserAction.getAction(UserAction.ADDC_ID, model));
			final Runnable runWs = new Runnable() {
				public void run() {
					view.editElements(Element.ID_WS).actionPerformed(null);
				}
			};
			dropWsButton.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() >= 2)
						runWs.run();
				}
			});
			customizationAction.put(dropWsButton.getAction().getValue(Action.SHORT_DESCRIPTION), runWs);
			toolBarButtonGroup.add(dropWsButton);

			/** drop Ck */
			dropCkButton = createButton(UserAction.getAction(UserAction.ADDD_ID, model));
			final Runnable runCk = new Runnable() {
				public void run() {
					view.editElements(Element.ID_CK).actionPerformed(null);
				}
			};
			dropCkButton.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() >= 2)
						runCk.run();
				}
			});
			customizationAction.put(dropCkButton.getAction().getValue(Action.SHORT_DESCRIPTION), runCk);
			toolBarButtonGroup.add(dropCkButton);

			/** drop diatomic molecule */
			dropDiatomicButton = createButton(UserAction.getAction(UserAction.ADDI_ID, model));
			final Runnable run1 = new Runnable() {
				public void run() {
					if (diatomicConfigure == null) {
						diatomicConfigure = new DiatomicConfigure(JOptionPane.getFrameForComponent(view));
						diatomicConfigure.pack();
						diatomicConfigure.setLocationRelativeTo(view);
					}
					diatomicConfigure.setCurrentValues();
					diatomicConfigure.setVisible(true);
					view.resetAddObjectIndicator();
				}
			};
			dropDiatomicButton.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() >= 2)
						run1.run();
				}
			});
			customizationAction.put(dropDiatomicButton.getAction().getValue(Action.SHORT_DESCRIPTION), run1);
			toolBarButtonGroup.add(dropDiatomicButton);

			/* drop triatomic molecule */
			dropTriatomicButton = createButton(UserAction.getAction(UserAction.WATE_ID, model));
			final Runnable run2 = new Runnable() {
				public void run() {
					if (triatomicConfigure == null) {
						triatomicConfigure = new TriatomicConfigure(JOptionPane.getFrameForComponent(view));
						triatomicConfigure.pack();
						triatomicConfigure.setLocationRelativeTo(view);
					}
					triatomicConfigure.setCurrentValues();
					triatomicConfigure.setVisible(true);
					view.resetAddObjectIndicator();
				}
			};
			dropTriatomicButton.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() >= 2)
						run2.run();
				}
			});
			customizationAction.put(dropTriatomicButton.getAction().getValue(Action.SHORT_DESCRIPTION), run2);
			toolBarButtonGroup.add(dropTriatomicButton);

			/** drop benzene molecule */
			dropBenzeneButton = createButton(UserAction.getAction(UserAction.BENZ_ID, model));
			toolBarButtonGroup.add(dropBenzeneButton);

			/* drop chain molecule */
			dropChainButton = createButton(UserAction.getAction(UserAction.ADCH_ID, model));
			final Runnable run3 = new Runnable() {
				public void run() {
					if (chainConfigure == null) {
						chainConfigure = new ChainConfigure(JOptionPane.getFrameForComponent(view));
						chainConfigure.pack();
						chainConfigure.setLocationRelativeTo(view);
					}
					chainConfigure.setCurrentValues();
					chainConfigure.setVisible(true);
					view.resetAddObjectIndicator();
				}
			};
			dropChainButton.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() >= 2)
						run3.run();
				}
			});
			customizationAction.put(dropChainButton.getAction().getValue(Action.SHORT_DESCRIPTION), run3);
			toolBarButtonGroup.add(dropChainButton);

			/** fill the selected area with Nt atoms */
			fillAreaWithNtButton = createButton(UserAction.getAction(UserAction.FILA_ID, model));
			toolBarButtonGroup.add(fillAreaWithNtButton);

			/** fill the selected area with Pl atoms */
			fillAreaWithPlButton = createButton(UserAction.getAction(UserAction.FILB_ID, model));
			toolBarButtonGroup.add(fillAreaWithPlButton);

			/** fill the selected area with Ws atoms */
			fillAreaWithWsButton = createButton(UserAction.getAction(UserAction.FILC_ID, model));
			toolBarButtonGroup.add(fillAreaWithWsButton);

			/** fill the selected area with Ck atoms */
			fillAreaWithCkButton = createButton(UserAction.getAction(UserAction.FILD_ID, model));
			toolBarButtonGroup.add(fillAreaWithCkButton);

			/** drop rectangular molecular surface */
			dropRectangularSurfaceButton = createButton(UserAction.getAction(UserAction.SREC_ID, model));
			toolBarButtonGroup.add(dropRectangularSurfaceButton);

			dropRectangularSplineButton = createButton(UserAction.getAction(UserAction.RREC_ID, model));
			toolBarButtonGroup.add(dropRectangularSplineButton);

			dropCircularSurfaceButton = createButton(UserAction.getAction(UserAction.SCIR_ID, model));
			toolBarButtonGroup.add(dropCircularSurfaceButton);

			dropCircularSplineButton = createButton(UserAction.getAction(UserAction.RCIR_ID, model));
			toolBarButtonGroup.add(dropCircularSplineButton);

			dropCurvedSurfaceButton = createButton(UserAction.getAction(UserAction.SCUR_ID, model));
			toolBarButtonGroup.add(dropCurvedSurfaceButton);

			dropCurvedSplineButton = createButton(UserAction.getAction(UserAction.RCUR_ID, model));
			toolBarButtonGroup.add(dropCurvedSplineButton);

			dropFreeFormSurfaceButton = createButton(UserAction.getAction(UserAction.SFRE_ID, model));
			toolBarButtonGroup.add(dropFreeFormSurfaceButton);

			dropRectangleButton = createButton(UserAction.getAction(UserAction.ADOB_ID, model));
			toolBarButtonGroup.add(dropRectangleButton);

			setBoundaryButton = createButton(UserAction.getAction(UserAction.SBOU_ID, model));
			final Runnable run5 = new Runnable() {
				public void run() {
					if (boundarySetup == null) {
						boundarySetup = new BoundarySetup(model);
						boundarySetup.pack();
						boundarySetup.setLocationRelativeTo(view);
					}
					else {
						boundarySetup.setModel(model);
					}
					boundarySetup.setCurrentValues();
					boundarySetup.setVisible(true);
				}
			};
			setBoundaryButton.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() >= 2)
						run5.run();
				}
			});
			customizationAction.put(setBoundaryButton.getAction().getValue(Action.SHORT_DESCRIPTION), run5);
			toolBarButtonGroup.add(setBoundaryButton);

			buildBondButton = createButton(UserAction.getAction(UserAction.BBON_ID, model));
			toolBarButtonGroup.add(buildBondButton);

			buildBendButton = createButton(UserAction.getAction(UserAction.BBEN_ID, model));
			toolBarButtonGroup.add(buildBendButton);

			mutateButton = createButton(UserAction.getAction(UserAction.MUTA_ID, model));
			toolBarButtonGroup.add(mutateButton);

			attachAminoAcidButton = createButton(UserAction.getAction(UserAction.AACD_ID, model));
			final Runnable run8 = new Runnable() {
				public void run() {
					if (growthModeDialog == null) {
						growthModeDialog = new GrowthModeDialog(JOptionPane.getFrameForComponent(AtomContainer.this),
								model);
						growthModeDialog.pack();
						growthModeDialog.setLocationRelativeTo(view);
					}
					growthModeDialog.setVisible(true);
				}
			};
			attachAminoAcidButton.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() >= 2)
						run8.run();
				}
			});
			customizationAction.put(attachAminoAcidButton.getAction().getValue(Action.SHORT_DESCRIPTION), run8);
			toolBarButtonGroup.add(attachAminoAcidButton);

			detachAminoAcidButton = createButton(UserAction.getAction(UserAction.SACD_ID, model));
			toolBarButtonGroup.add(detachAminoAcidButton);

			attachNucleotideButton = createButton(UserAction.getAction(UserAction.ANTD_ID, model));
			toolBarButtonGroup.add(attachNucleotideButton);

			detachNucleotideButton = createButton(UserAction.getAction(UserAction.SNTD_ID, model));
			toolBarButtonGroup.add(detachNucleotideButton);

			List<AbstractButton> list = new ArrayList<AbstractButton>();
			list.add(dropNtButton);
			list.add(dropPlButton);
			list.add(dropWsButton);
			list.add(dropCkButton);
			list.add(dropDiatomicButton);
			list.add(dropTriatomicButton);
			list.add(dropBenzeneButton);
			list.add(dropChainButton);
			list.add(dropRectangleButton);
			list.add(fillAreaWithNtButton);
			list.add(fillAreaWithPlButton);
			list.add(fillAreaWithWsButton);
			list.add(fillAreaWithCkButton);
			list.add(dropRectangularSurfaceButton);
			list.add(dropRectangularSplineButton);
			list.add(dropCircularSurfaceButton);
			list.add(dropCircularSplineButton);
			list.add(dropCurvedSurfaceButton);
			list.add(dropCurvedSplineButton);
			list.add(dropFreeFormSurfaceButton);
			list.add(attachAminoAcidButton);
			list.add(detachAminoAcidButton);
			list.add(attachNucleotideButton);
			list.add(detachNucleotideButton);
			String s = getInternationalText("DropObjectActions");
			actionCategory.put(s != null ? s : "Drop-Object Actions", list);

			list = new ArrayList<AbstractButton>();
			list.add(heatButton);
			list.add(coolButton);
			list.add(pcharButton);
			list.add(ncharButton);
			list.add(iresButton);
			list.add(dresButton);
			list.add(idmpButton);
			list.add(ddmpButton);
			list.add(rotateObjectButton);
			list.add(duplicateButton);
			list.add(removeObjectsButton);
			list.add(setBoundaryButton);
			list.add(mutateButton);
			list.add(changeVelocityButton);
			list.add(buildBondButton);
			list.add(buildBendButton);
			s = getInternationalText("EditingActions");
			actionCategory.put(s != null ? s : "Editing Actions", list);

		}

	}

	private class PlotContourAction extends AbstractAction {
		int probeID;
		float charge;

		PlotContourAction(int probeID, float charge) {
			this.probeID = probeID;
			this.charge = charge;
		}

		public void actionPerformed(ActionEvent e) {
			Atom probe = model.createAtomOfElement(probeID);
			probe.setCharge(charge);
			view.showContourPlot(true, probe);
		}
	}

}