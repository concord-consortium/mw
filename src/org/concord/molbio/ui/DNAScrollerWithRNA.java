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

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

import org.concord.molbio.engine.Protein;
import org.concord.molbio.engine.DNA;
import org.concord.molbio.engine.RNA;
import org.concord.molbio.engine.Codon;
import org.concord.molbio.engine.Aminoacid;
import org.concord.molbio.engine.DNAScrollerModel;
import org.concord.molbio.event.RNATranscriptionEvent;
import org.concord.molbio.event.RNATranscriptionListener;
import org.concord.molbio.event.RNATranslationEvent;
import org.concord.molbio.event.RNATranslationListener;

/* http://www.csu.edu.au/faculty/health/biomed/subjects/molbol/images/7_9.jpg 5->3 demonstration */

public class DNAScrollerWithRNA extends DNAScroller {

	public static final int SCROLLER_NORMAL_STATE = 0;
	public static final int SCROLLER_TRANSCRIPTION_READY_STATE = 1;
	public static final int SCROLLER_TRANSLATION_READY_STATE = 2;

	Rectangle[] rRNA;
	int scrollerState = SCROLLER_TRANSCRIPTION_READY_STATE;

	int UPP_OFFSET = 35;
	int DOWN_OFFSET = 5;
	int LEFT_OFFSET = 20;

	DNAScrollerEffect currentEffect;
	DNAScrollerEffect transcriptionBeginEffect;
	DNAScrollerEffect transcriptionEndEffect;

	int currentCodon = -1;

	Thread transcriptionThread;
	int transcriptionDT = 50;
	boolean transcriptionEndedInternal = true;
	boolean transcriptionEnded;

	Thread translationThread;
	int translationDT = 200;
	boolean translationEndedInternal = true;
	boolean translationEnded;

	boolean drawLastRNABase = true;

	Protein protein;
	Aminoacid[] aminoacids;
	Vector<RNATranslationListener> RNATranslationListeners;
	Vector<RNATranscriptionListener> RNATranscriptionListeners;
	boolean running;

	boolean startTranscriptionWithEffect;
	boolean startTranslationWithEffect;
	boolean gotoTranslationAfterTranscription;
	boolean mutationAfterTranslationDoneAllowed;

	BufferedImage ribosomeImage;

	boolean oneStepMode;

	protected DNAComparator dnaComparator;

	public DNAScrollerWithRNA() {
		this(true);
	}

	public DNAScrollerWithRNA(boolean randomMutationSupport) {
		super(randomMutationSupport);
		transcriptionEndEffect = new EndTranscriptionEffect(this, SCROLLER_TRANSCRIPTION_READY_STATE,
				SCROLLER_TRANSLATION_READY_STATE);
		transcriptionBeginEffect = new BeginTranscriptionEffect(this, SCROLLER_NORMAL_STATE,
				SCROLLER_TRANSCRIPTION_READY_STATE);
		addRNATranscriptionListener(new RNATranscriptionListener() {
			public void baseTranscripted(RNATranscriptionEvent evt) {
				evt.setConsumed(true);
				if (evt.getMode() == RNATranscriptionListener.MODE_TRANSCRIPTION_END
						&& isGotoTranslationAfterTranscription()) {
					if (!oneStepMode)
						startTranslation();
				}
			}
		});
	}

	public void destroy() {
		if (transcriptionThread != null)
			transcriptionThread.interrupt();
		if (translationThread != null)
			translationThread.interrupt();
		if (currentEffect != null)
			currentEffect.destroy();
		if (transcriptionBeginEffect != null)
			transcriptionBeginEffect.destroy();
		if (transcriptionEndEffect != null)
			transcriptionEndEffect.destroy();
	}

	public void setRibosomeImage(BufferedImage bim) {
		ribosomeImage = bim;
		if (op != null)
			op.setImage(ribosomeImage);
	}

	public synchronized boolean isRunning() {
		return running;
	}

	public synchronized void setRunning(boolean val) {
		running = val;
	}

	public void setStartTranscriptionWithEffect(boolean startTranscriptionWithEffect) {
		this.startTranscriptionWithEffect = startTranscriptionWithEffect;
	}

	public boolean isStartTranscriptionWithEffect() {
		return startTranscriptionWithEffect;
	}

	public void setStartTranslationWithEffect(boolean startTranslationWithEffect) {
		this.startTranslationWithEffect = startTranslationWithEffect;
	}

	public boolean isStartTranslationWithEffect() {
		return startTranslationWithEffect;
	}

	public void setGotoTranslationAfterTranscription(boolean gotoTranslationAfterTranscription) {
		this.gotoTranslationAfterTranscription = gotoTranslationAfterTranscription;
	}

	public boolean isGotoTranslationAfterTranscription() {
		return gotoTranslationAfterTranscription;
	}

	public void setStartTranscriptionEffectDt(int dt) {
		if (dt > 0 && transcriptionBeginEffect != null) {
			transcriptionBeginEffect.setEffectDelay(dt);
		}
	}

	public void setStartTranslationEffectDt(int dt) {
		if (dt > 0 && transcriptionEndEffect != null) {
			transcriptionEndEffect.setEffectDelay(dt);
		}
	}

	public void setStartTranscriptionEffectMaximumSteps(int n) {
		if (n > 0 && transcriptionBeginEffect != null) {
			transcriptionBeginEffect.setMaximumSteps(n);
		}
	}

	public void setStartTranslationEffectMaximumSteps(int n) {
		if (n > 0 && transcriptionEndEffect != null) {
			transcriptionEndEffect.setMaximumSteps(n);
		}
	}

	public int getStartTranscriptionEffectDt() {
		if (transcriptionBeginEffect != null) {
			return transcriptionBeginEffect.getEffectDelay();
		}
		return 0;
	}

	public int getStartTranslationEffectDt() {
		if (transcriptionEndEffect != null) {
			return transcriptionEndEffect.getEffectDelay();
		}
		return 0;
	}

	public int getStartTranscriptionMaximumSteps() {
		if (transcriptionBeginEffect != null) {
			return transcriptionBeginEffect.getMaximumStep();
		}
		return 0;
	}

	public int getStartTranslationMaximumSteps() {
		if (transcriptionEndEffect != null) {
			return transcriptionEndEffect.getMaximumStep();
		}
		return 0;
	}

	public void suspendSimulation() {
		if (isInEffect()) {
			currentEffect.forceEndEffect();
			return;
		}
		if (scrollerState == SCROLLER_NORMAL_STATE || isInEffect())
			return;
		if (scrollerState == SCROLLER_TRANSCRIPTION_READY_STATE) {
			if (transcriptionThread != null && transcriptionThread.isAlive()) {
				if (isRunning())
					setRunning(false);
			}
		}
		else if (scrollerState == SCROLLER_TRANSLATION_READY_STATE) {
			if (translationThread != null && translationThread.isAlive()) {
				if (isRunning())
					setRunning(false);
			}
		}
	}

	public void resumeSimulation() {
		if (isInEffect())
			return;
		oneStepMode = false;
		if (scrollerState == SCROLLER_NORMAL_STATE) {
			resetToStartTranscription();
		}
		if (scrollerState == SCROLLER_TRANSCRIPTION_READY_STATE) {
			if (transcriptionThread == null || !transcriptionThread.isAlive()) {
				startTranscription();
			}
			else if (!isRunning())
				setRunning(true);
		}
		else if (scrollerState == SCROLLER_TRANSLATION_READY_STATE) {
			if (translationThread == null || !translationThread.isAlive()) {
				startTranslation();
			}
			else if (!isRunning())
				setRunning(true);
		}
	}

	public void doOneStep() {
		if (isInEffect())
			return;
		if (isRunning())
			return;
		oneStepMode = true;
		if (scrollerState == SCROLLER_NORMAL_STATE) {
			resetToStartTranscription();
		}
		if (scrollerState == SCROLLER_TRANSCRIPTION_READY_STATE) {
			if (isTranscriptionEnded()) {
				resetToStartTranslation();
			}
			else {
				if (transcriptionThread == null || !transcriptionThread.isAlive()) {
					startTranscription(false);
				}
				Thread t = new Thread(new Runnable() {
					public void run() {
						drawLastRNABase = false;
						nextTranscriptionStep();
						try {
							Thread.sleep(transcriptionDT / 2);
						}
						catch (Throwable tt) {
						}
						drawLastRNABase = true;
						repaint();
					}
				});
				t.setPriority(Thread.MIN_PRIORITY);
				t.start();
			}
			return;
		}
		if (scrollerState == SCROLLER_TRANSLATION_READY_STATE) {
			if (translationThread == null || !translationThread.isAlive()) {
				startTranslation(false);
			}
			Thread t = new Thread(new Runnable() {
				public void run() {
					nextTranslationStep();
				}
			});
			t.setPriority(Thread.MIN_PRIORITY);
			t.start();
			return;
		}
	}

	public void waitUntilEffectEnd() {
		if (currentEffect == null)
			return;
		while (true) {
			if (!isInEffect())
				break;
			try {
				Thread.sleep(200);
				Thread.yield();
			}
			catch (Throwable t) {
			}
		}
	}

	public void startTranscriptionEffectEnd() {
		if (currentEffect == null) {
			currentEffect = transcriptionEndEffect;
			transcriptionEndEffect.startEffect();
		}
	}

	public void startTranscriptionEffectBegin() {
		if (currentEffect == null) {
			currentEffect = transcriptionBeginEffect;
			transcriptionBeginEffect.startEffect();
		}
	}

	public void addRNATranslationListener(RNATranslationListener l) {
		if (RNATranslationListeners == null)
			RNATranslationListeners = new Vector<RNATranslationListener>();
		if (l == null)
			return;
		if (RNATranslationListeners.contains(l))
			return;
		RNATranslationListeners.add(l);
	}

	public void removeRNATranslationListener(RNATranslationListener l) {
		if (l == null)
			return;
		if (!RNATranslationListeners.contains(l))
			return;
		RNATranslationListeners.remove(l);
	}

	public void addRNATranscriptionListener(RNATranscriptionListener l) {
		if (RNATranscriptionListeners == null)
			RNATranscriptionListeners = new Vector<RNATranscriptionListener>();
		if (l == null)
			return;
		if (RNATranscriptionListeners.contains(l))
			return;
		RNATranscriptionListeners.add(l);
	}

	public void removeRNATranscriptionListener(RNATranscriptionListener l) {
		if (l == null)
			return;
		if (!RNATranscriptionListeners.contains(l))
			return;
		RNATranscriptionListeners.remove(l);
	}

	public synchronized boolean isInEffect() {
		return currentEffect != null && currentEffect.isInEffect();
	}

	public void clearEffect() {
		if (currentEffect != null && !currentEffect.isEffectDone()) {
			currentEffect.forceEndEffect();
		}
		currentEffect = null;
	}

	public void effectJustEnded(DNAScrollerEffect effect) {
		if (effect == transcriptionEndEffect && isStartTranslationWithEffect()) {
			resetToStartTranslation();
			setStartTranslationWithEffect(false);
			startTranslation();
			setStartTranslationWithEffect(true);
		}
		if (effect == transcriptionBeginEffect && isStartTranscriptionWithEffect()) {
			resetToStartTranscription();
			setStartTranscriptionWithEffect(false);
			startTranscription();
			setStartTranscriptionWithEffect(true);
		}
	}

	synchronized void setScrollerState(int state) {
		scrollerState = state;
		resetDNA();
		repaint();
		translationEnded = false;
		transcriptionEnded = (scrollerState == SCROLLER_TRANSLATION_READY_STATE);
		if (op == null)
			return;
		switch (scrollerState) {
		case SCROLLER_TRANSCRIPTION_READY_STATE:
			op.setR((charw + 2 * getCodonDistance()) / 2);
			break;
		case SCROLLER_TRANSLATION_READY_STATE:
			op.setR(getCodonWidth());
			break;
		case SCROLLER_NORMAL_STATE:
		default:
			op.setR((3 * charw + 2 * getCodonDistance()) / 2);
			break;
		}
	}

	public synchronized int getScrollerState() {
		return scrollerState;
	}

	void drawArrows(Graphics g) {
		if (getColorSchemeByUsage())
			return;
		Rectangle r53 = null;
		Rectangle r35 = null;
		Rectangle[] r53s = getRects53(g);
		if (r53s != null && r53s.length > 0) {
			r53 = r53s[0];
		}
		Rectangle[] r35s = getRects35(g);
		if (r35s != null && r35s.length > 0) {
			r35 = r35s[0];
		}

		if (g == null || bim == null)
			return;

		Dimension scrollerSize = scroller.getSize();

		Graphics2D g2d = (Graphics2D) g;
		FontMetrics fm = g2d.getFontMetrics();

		Paint oldc = g2d.getPaint();

		g2d.setPaint(Color.darkGray);
		if (r53 != null) {
			g2d.drawString("5'", 3, r53.y + fm.getHeight());
			g2d.drawString("3'", scrollerSize.width - getLeftOffset() + 3, r53.y + fm.getHeight());
		}
		if (r35 != null) {
			g2d.drawString("5'", scrollerSize.width - getLeftOffset() + 3, r35.y + fm.getHeight());
			g2d.drawString("3'", 3f, r35.y + fm.getHeight());
		}
		if (scrollerState != SCROLLER_NORMAL_STATE) {
			if (rRNA != null && rRNA.length > 0) {
				Rectangle rectRNA0 = rRNA[0];
				if (rectRNA0 != null) {
					g2d.drawString("5'", 3, rectRNA0.y + fm.getHeight());
					g2d.drawString("3'", scrollerSize.width - getLeftOffset() + 3, rectRNA0.y + fm.getHeight());
				}
			}
		}

		g2d.setPaint(oldc);

	}

	protected boolean inPredefinedFragment(int currentInd) {
		DNAScrollerModel dnamodel = getModel();
		if (dnamodel == null)
			return false;
		DNA dna = dnamodel.getDNA();
		if (dna == null)
			return false;
		boolean dnaHasPromoter = dna.startWithPromoter();
		boolean dnaHasTerminator = dna.endWithTerminator();
		int dnaLength = dna.getLength();
		if (dnaHasPromoter && currentInd < DNA.PROMOTER_LENGTH)
			return true;
		if (dnaHasTerminator && currentInd >= dnaLength - DNA.TERMINATOR_LENGTH)
			return true;
		int startBaseInd = (dnaHasPromoter) ? DNA.PROMOTER_LENGTH : 0;
		startBaseInd += DNA.START_LENGTH;
		if (dna.hasStartFragment() && currentInd < startBaseInd)
			return true;
		startBaseInd = (dnaHasTerminator) ? dnaLength - DNA.TERMINATOR_LENGTH : dnaLength;
		if (dna.hasEndFragment() && currentInd >= startBaseInd - DNA.END_LENGTH)
			return true;
		return false;
	}

	public boolean isMutationEnabled() {
		if (mutationEnabled && (scrollerState == SCROLLER_NORMAL_STATE))
			return true;
		if (mutationAfterTranslationDoneAllowed && mutationEnabled
				&& (scrollerState == SCROLLER_TRANSLATION_READY_STATE) && isTranslationEnded())
			return true;
		return false;
	}

	void recalculateInternalComponents() {
		super.recalculateInternalComponents();
	}

	public synchronized void setDNA(DNA dna) {
		super.setDNA(dna);
		if (getModel() != null) {
			getModel().setStopProduceRNAonStopCodon(false);
			createRectangles();
		}
		if (dnaComparator != null)
			dnaComparator.setDNAModel(model);
	}

	void setGeometry() {
		super.setGeometry();
		model.setNBaseInWindow(0);
	}

	int getLeftOffset() {
		return LEFT_OFFSET;
	}

	Shape getDefaultClipForStrands() {
		Rectangle r = new Rectangle(getInsets().left + getLeftOffset(), 0, getSize().width - 2 * getLeftOffset()
				- getInsets().left - getInsets().right, getSize().height);
		return r;
	}

	protected javax.swing.border.Border getDefaultBorder() {
		return null;
	}

	public String getScrollerToolTipText(MouseEvent evt) {
		Shape clipShape = getDefaultClipForStrands();
		if (clipShape != null) {
			Rectangle rClip = clipShape.getBounds();
			if (!rClip.contains(evt.getPoint()))
				return null;
		}
		String rnaToolTip = getRNAToolTip(evt);
		if (rnaToolTip != null)
			return rnaToolTip;
		DNAScrollerModel dnamodel = getModel();
		if (dnamodel == null)
			return null;
		String text = "<html>";
		Codon codon = null;
		if (dnamodel.isStrand53Available() && charRectangles53 != null) {
			int startIndex = model.getStartWindowIndex();
			DNA dna = dnamodel.getDNA();
			for (int i = 0; i < charRectangles53.length; i++) {
				if (charRectangles53[i].contains(evt.getPoint())) {
					codon = dnamodel.get53CodonFromOffset(i);
					if (inPredefinedFragment(i + startIndex)) {
						if (codon.isCodonStart()) {
							return "Start codon";
						}
						else if (dna.hasStartFragment() && (i + startIndex < DNA.PROMOTER_LENGTH)) {
							return "Promoter";
						}
						else if (i + startIndex >= (dna.getLength() - DNA.TERMINATOR_LENGTH - DNA.END_LENGTH)) {
							return "Terminator";
						}
					}
					String tooltip53 = dnamodel.get53ToolTipString(i, true);
					if (tooltip53 != null) {
						text += "(5')" + tooltip53 + "(3') ";
						break;
					}
				}
			}
		}
		if (dnamodel.isStrand35Available() && charRectangles35 != null) {
			int startIndex = model.getStartWindowIndex();
			DNA dna = dnamodel.getDNA();
			for (int i = 0; i < charRectangles35.length; i++) {
				if (charRectangles35[i].contains(evt.getPoint())) {
					codon = dnamodel.get35CodonFromOffset(i);
					if (inPredefinedFragment(i + startIndex)) {
						if (codon.isCodonStart()) {
							return "Start codon";
						}
						else if (dna.hasStartFragment() && (i + startIndex < DNA.PROMOTER_LENGTH)) {
							return "Promoter";
						}
						else if (i + startIndex >= (dna.getLength() - DNA.TERMINATOR_LENGTH - DNA.END_LENGTH)) {
							return "Terminator";
						}
					}
					String tooltip35 = dnamodel.get35ToolTipString(i, true);
					if (tooltip35 != null) {
						text += "(3')" + tooltip35 + "(5') ";
						codon = codon.getTranscripted(true);
						text += " &#10132; mRNA:" + codon + " ";
						break;
					}
				}
			}
		}
		if (codon != null) {
			text += " &#10132; ";
			if (codon.isCodonStop()) {
				text += "Stop";
			}
			else {
				Aminoacid amino = codon.createAminoacid();
				if (amino == null)
					text += "???";
				else text += amino.getAbbreviation();
			}
		}

		text += "</html>";
		return text;

	}

	synchronized void createRectangles() {
		super.createRectangles();
		Graphics g = getGraphics();
		if (g != null) {
			Rectangle[] rs = getRects35(g);
			if (rs != null) {
				int rnaRN = model.getRNALengthFromCurrIndex();
				if (rnaRN > 0) {
					rRNA = new Rectangle[rnaRN];
					int nt = rnaRN;
					if (rs.length < nt)
						nt = rs.length;
					System.arraycopy(rs, 0, rRNA, 0, nt);
					for (int i = 0; i < rRNA.length; i++) {
						if (rRNA[i] == null)
							continue;
						int dy = -rRNA[i].y + UPP_OFFSET;
						switch (scrollerState) {
						case SCROLLER_TRANSCRIPTION_READY_STATE:
							if (isInEffect()) {
								dy = -rRNA[i].y
										+ UPP_OFFSET
										+ charh
										- (int) Math.round((double) currentEffect.getCurrentStep() * (double) (charh)
												/ currentEffect.getMaximumStep());
							}
							else {
								dy = -rRNA[i].y + scroller.getSize().height - DOWN_OFFSET - 2 * charh;
							}
							break;
						case SCROLLER_TRANSLATION_READY_STATE:
						case SCROLLER_NORMAL_STATE:
						default:
							dy = -rRNA[i].y + UPP_OFFSET;
							break;
						}
						rRNA[i].translate(0, dy);
					}
				}
			}
			g.dispose();
		}
	}

	void drawInImage(Graphics g) {
		if (scrollerState != SCROLLER_TRANSLATION_READY_STATE) {
			setStopCodonColor(Color.black);
		}
		else {
			setDefaultStopCodonColor();
		}
		super.drawInImage(g);
	}

	public MagnifyGlassOp getMagnifyGlassOp() {
		MagnifyGlassOp superOp = super.getMagnifyGlassOp();
		if (scrollerState != SCROLLER_TRANSLATION_READY_STATE) {
			if ((scrollerState != SCROLLER_NORMAL_STATE || isInEffect()) && transcriptionEndedInternal)
				return null;
			if (superOp != null)
				superOp.setDrawImage(scrollerState == SCROLLER_TRANSLATION_READY_STATE);
			return superOp;
		}
		if (isTranslationEnded())
			return null;
		int initCodonNumber = -1;
		DNAScrollerModel model = getModel();
		if (model != null) {
			DNA dna = model.getDNA();
			if (dna != null) {
				if (dna.startWithPromoter())
					initCodonNumber = DNA.PROMOTER_LENGTH / 3;
			}
		}
		if (currentCodon < initCodonNumber)
			return null;

		if (superOp != null)
			superOp.setDrawImage(scrollerState == SCROLLER_TRANSLATION_READY_STATE);
		return superOp;
	}

	void createMagnifyGlassOp() {
		float xop = getCodonDistance() + charw / 2;
		float yop = getYop();
		float rh = charh + 1;
		if (op == null) {
			if (scrollerState == SCROLLER_NORMAL_STATE) {
				super.createMagnifyGlassOp();
			}
			else {
				op = new MagnifyGlassOp(1f, xop, yop, (charw + 2 * getCodonDistance()), rh,
						MagnifyGlassOp.GLASS_AS_RECTANGLE);
			}
		}
		else {
			op.mx = xop;
			op.my = yop;
			op.rh = rh;
		}
		if (op != null)
			op.setImage(ribosomeImage);
	}

	void setOpOffset() {
		if (op == null || model == null)
			return;
		if (scrollerState == SCROLLER_TRANSLATION_READY_STATE) {
			float xop = getLeftOffset() + (model.getCurrIndex() - model.getStartWindowIndex()) / 3 * getCodonWidth()
					+ getCodonWidth();
			op.setX(xop);
			return;
		}
		if (scrollerState == SCROLLER_NORMAL_STATE) {
			float xop = getLeftOffset() + (model.getCurrIndex() - model.getStartWindowIndex()) * getCodonWidth() / 3
					+ getCodonWidth() / 2;
			op.setX(xop);
			return;
		}

		float xop = getLeftOffset() + (model.getCurrIndex() - model.getStartWindowIndex()) * getCodonWidth() / 3
				+ charw / 2 + getCodonDistance();
		int reminder = (currentBase % 3);
		xop += (reminder * getCodonWidth() / 3);
		op.setX(xop);
	}

	public String getRNAToolTip(MouseEvent e) {
		if (scrollerState == SCROLLER_NORMAL_STATE)
			return null;
		Shape clipShape = getDefaultClipForStrands();
		if (clipShape != null) {
			Rectangle rClip = clipShape.getBounds();
			if (!rClip.contains(e.getPoint()))
				return null;
		}
		if (model != null && model.getDNA() != null && rRNA != null) {
			char[] charsRNA = model.getRNAChars();
			if (charsRNA == null)
				return null;
			int startIndex = model.getStartWindowIndex();
			int endIndex = charsRNA.length;
			if (scrollerState == SCROLLER_TRANSCRIPTION_READY_STATE) {
				endIndex = Math.min(currentBase + 1 - startIndex, charsRNA.length);
			}
			for (int i = 0; i < 3 * (endIndex / 3); i++) {
				Rectangle r = (rRNA != null && i >= 0 && i < rRNA.length) ? rRNA[i] : null;
				if (r == null)
					continue;
				if (r.contains(e.getPoint())) {
					Codon c53 = model.get53CodonFromOffset(i);
					String text = "<html>mRNA: " + c53 + " &#10132; ";
					if (c53.isCodonStop()) {
						text += "Stop";
					}
					else {
						Aminoacid amino = c53.createAminoacid();
						text += amino == null ? "???" : amino.getAbbreviation();
					}
					return text + "</html>";
				}
			}
		}
		return null;
	}

	synchronized void drawRNA(Graphics g) {
		if (scrollerState == SCROLLER_NORMAL_STATE)
			return;
		if (model != null && model.getDNA() != null && rRNA != null) {
			boolean isTranslationState = (scrollerState == SCROLLER_TRANSLATION_READY_STATE);
			FontMetrics fm = g.getFontMetrics();
			int offsety = isTranslationState ? fm.getDescent() : fm.getHeight() - fm.getDescent();
			char[] charsRNA = model.getRNAChars();
			if (charsRNA == null)
				return;
			int startIndex = model.getStartWindowIndex();
			int endIndex = charsRNA.length;
			if (scrollerState == SCROLLER_TRANSCRIPTION_READY_STATE) {
				endIndex = Math.min(currentBase + 1 - startIndex, charsRNA.length);
			}
			DNA dna = model.getDNA();
			int dnaLength = dna.getLength();
			boolean dnaHasPromoter = dna.startWithPromoter();
			boolean dnaHasTerminator = dna.endWithTerminator();
			for (int i = 0; i < endIndex; i++) {
				if (dnaHasPromoter && (startIndex + i < DNA.PROMOTER_LENGTH))
					continue;
				if (dnaHasTerminator && (startIndex + i >= dnaLength - DNA.TERMINATOR_LENGTH))
					continue;
				if (!drawLastRNABase && i == endIndex - 1)
					continue;
				Codon c53 = model.get53CodonFromOffset(i);
				Composite oldComp = null;
				Color codonColor = codonColors[((startIndex + i) / 3) % 2];
				if (isInEffect()) {
					oldComp = ((Graphics2D) g).getComposite();
					float alpha = 1;
					((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
				}
				if (c53 != null && c53.isCodonStop())
					codonColor = stopCodonColor;
				g.setColor(codonColor);
				char currChar = charsRNA[i];
				int cw = fm.charWidth(currChar);
				Rectangle r = (rRNA != null && i >= 0 && i < rRNA.length) ? rRNA[i] : null;
				if (r != null) {
					drawCodonFrame(g, !isTranslationState, currChar, r);
					g.drawChars(charsRNA, i, 1, r.x + r.width / 2 - cw / 2, isTranslationState ? r.y + r.height
							- offsety : r.y + offsety);
					drawBackbone(g, r, !isTranslationState, true, ((startIndex + i) % 3 == 0));
				}
				if (oldComp != null) {
					((Graphics2D) g).setComposite(oldComp);
				}
			}
		}
	}

	public void setPreferredSize(Dimension d) {
		super.setPreferredSize(d);
		pPreferredSize = d;
	}

	public Dimension getPreferredSize() {
		return (pPreferredSize != null) ? pPreferredSize : new Dimension(500, 130);
	}

	public boolean highlightCurrentBase() {
		return (scrollerState == SCROLLER_NORMAL_STATE);
	}

	synchronized Rectangle[] getRects53(Graphics g) {
		if (g == null || model == null) {
			repaint(200);
			return null;
		}
		if (model != null && model.getDNA() == null)
			return null;
		Rectangle[] r53 = new Rectangle[model.get53StrandLengthFromCurrIndex()];
		FontMetrics fm = g.getFontMetrics();
		Rectangle r = fm.getStringBounds(model.getDNA53String(), g).getBounds();
		r.width = charw;
		r.height = charh;
		int dy = -r.y + scroller.getSize().height - DOWN_OFFSET - charh;
		switch (scrollerState) {
		case SCROLLER_TRANSCRIPTION_READY_STATE:
			if (isInEffect()) {
				dy = -r.y
						+ UPP_OFFSET
						+ (int) Math.round((double) currentEffect.getCurrentStep()
								* (double) (scroller.getSize().height - DOWN_OFFSET - 2 * charh - UPP_OFFSET)
								/ currentEffect.getMaximumStep());
			}
			else {
				dy = -r.y + UPP_OFFSET;
			}
			break;
		case SCROLLER_TRANSLATION_READY_STATE:
		default:
			dy = -r.y + scroller.getSize().height - DOWN_OFFSET - 2 * charh;
			break;
		case SCROLLER_NORMAL_STATE:
			dy = -r.y + scroller.getSize().height - DOWN_OFFSET - 2 * charh;
			if (isInEffect()) {
				dy -= (int) Math.round((double) currentEffect.getCurrentStep()
						* (double) (scroller.getSize().height - DOWN_OFFSET - 2 * charh - UPP_OFFSET)
						/ currentEffect.getMaximumStep());
			}
			break;
		}
		r.translate(-r.x + LEFT_OFFSET, dy);
		char[] chars = model.get53Chars();
		for (int i = 0; i < chars.length; i++) {
			if (i >= r53.length)
				break;
			if (i == 0) {
				r.translate(getCodonDistance(), 0);
			}
			else if ((i % 3) == 0) {
				r.translate(2 * getCodonDistance(), 0);
			}
			r53[i] = new Rectangle(r);
			r.translate(r.width, 0);
		}
		return r53;
	}

	synchronized Rectangle[] getRects35(Graphics g) {
		Rectangle[] r35 = getRects53(g);
		if (r35 == null)
			return null;
		for (int i = 0; i < r35.length; i++) {
			if (r35[i] != null)
				r35[i].translate(0, -r35[i].height);
		}
		for (int i = 0; i < r35.length; i++) {
			int dy = 0;
			if (r35[i] != null) {
				dy = -r35[i].y + scroller.getSize().height - DOWN_OFFSET - charh;
				r35[i].translate(0, dy);
			}
		}
		return r35;
	}

	void draw35Codon(int i, Graphics g, char[] chars, FontMetrics fm) {
		if (fm == null)
			fm = g.getFontMetrics();
		int offsety = fm.getDescent();
		int startIndex = model.getStartWindowIndex();
		Codon codon = model.get35CodonFromOffset(i);
		if (codon == null)
			return;
		Codon codonTranscripted = codon.getTranscripted(true);
		g.setColor(codonColors[((startIndex + i) / 3) % 2]);
		Color currColor = g.getColor();
		char currChar;
		if (charRectangles35 != null && chars != null && i < charRectangles35.length && i < chars.length) {
			boolean currentBaseBoolean = ((currentStrand == DNA.DNA_STRAND_35 && i == currentBase - startIndex) && highlightCurrentBase());
			currChar = chars[i];
			int cw = fm.charWidth(currChar);
			Rectangle r = charRectangles35[i];
			if (currentBaseBoolean) {
				r.translate(0, DEFAULT_CURRENT_BASE_OFFSETY);
			}
			Color forceColor = null;
			if (inPredefinedFragment(i + startIndex)) {
				if (codonTranscripted.isCodonStart() || inPromoterInterestingPlace(i + startIndex)) {
					forceColor = Color.green;
				}
				else {
					forceColor = Color.lightGray;
				}
			}
			else if (colorSchemeByUsage)
				forceColor = Aminoacid.getUsageColor(codonTranscripted.toString());
			drawCodonFrame(g, false, currChar, r, currentBaseBoolean, forceColor);
			drawBackbone(g, r, false, false, ((startIndex + i) % 3 == 0));
			Color needColor = Color.black;
			if (!model.isStrand35Available())
				needColor = disableColor;
			Font oldFont = g.getFont();
			if (currentBaseBoolean) {
				g.setFont(oldFont.deriveFont(Font.BOLD | Font.ITALIC));
				g.setColor(needColor);
			}
			else g.setColor(needColor);
			g.drawChars(chars, i, 1, r.x + r.width / 2 - cw / 2, r.y + r.height - offsety);
			if (currentBaseBoolean) {
				g.setFont(oldFont);
				r.translate(0, -DEFAULT_CURRENT_BASE_OFFSETY);
			}
			if (!model.isStrand35Available())
				g.setColor(currColor);
		}
	}

	void draw53Codon(int i, Graphics g, char[] chars, FontMetrics fm) {
		if (fm == null)
			fm = g.getFontMetrics();
		int offsety = fm.getHeight() - fm.getDescent();
		int startIndex = model.getStartWindowIndex();
		Codon codon = model.get53CodonFromOffset(i);
		if (codon == null)
			return;
		g.setColor(codonColors[((startIndex + i) / 3) % 2]);
		Color currColor = g.getColor();
		char currChar;
		if (charRectangles53 != null && chars != null && i >= 0 && i < charRectangles53.length && i < chars.length) {
			boolean currentBaseBoolean = ((currentStrand == DNA.DNA_STRAND_53 && i == currentBase - startIndex) && highlightCurrentBase());
			currChar = chars[i];
			int cw = fm.charWidth(currChar);
			Rectangle r = charRectangles53[i];
			if (currentBaseBoolean) {
				r.translate(0, -DEFAULT_CURRENT_BASE_OFFSETY);
			}
			Color forceColor = null;
			if (inPredefinedFragment(i + startIndex)) {
				if (codon.isCodonStart() || inPromoterInterestingPlace(i + startIndex)) {
					forceColor = Color.green;
				}
				else {
					forceColor = Color.lightGray;
				}
			}
			else if (colorSchemeByUsage)
				forceColor = Aminoacid.getUsageColor(codon.toString());
			drawCodonFrame(g, true, currChar, r, currentBaseBoolean, forceColor);
			drawBackbone(g, r, true, false, ((startIndex + i) % 3 == 0));

			Color needColor = Color.black;
			if (!model.isStrand53Available())
				needColor = disableColor;
			Font oldFont = g.getFont();
			if (currentBaseBoolean) {
				g.setFont(oldFont.deriveFont(Font.BOLD | Font.ITALIC));
				g.setColor(needColor);
			}
			else g.setColor(needColor);
			g.drawChars(chars, i, 1, r.x + r.width / 2 - cw / 2, r.y + offsety);
			if (currentBaseBoolean) {
				g.setFont(oldFont);
				r.translate(0, DEFAULT_CURRENT_BASE_OFFSETY);
			}
			if (!model.isStrand53Available())
				g.setColor(currColor);

		}
	}

	float getYop() {
		int ret = bim.getHeight() - DOWN_OFFSET - charh;
		switch (scrollerState) {
		case SCROLLER_TRANSCRIPTION_READY_STATE:
			if (isInEffect()) {
				ret = UPP_OFFSET
						+ charh
						+ (int) Math.round((double) currentEffect.getCurrentStep()
								* (double) (bim.getHeight() - DOWN_OFFSET - 2 * charh - UPP_OFFSET)
								/ currentEffect.getMaximumStep());
			}
			else {
				ret = bim.getHeight() - DOWN_OFFSET - charh;
			}
			break;
		case SCROLLER_TRANSLATION_READY_STATE:
			ret = UPP_OFFSET;
			break;
		case SCROLLER_NORMAL_STATE:
			ret = bim.getHeight() - DOWN_OFFSET - charh;
			break;
		}
		return ret;
	}

	int getCodonDistance() {
		return 0;
	}

	public static void sayString(String str) {
		try {
			Runtime.getRuntime().exec(
					new String[] { "osascript", "-e",
							"say \"" + str + "\" using \"Agnes\" waiting until completion false" });
		}
		catch (Throwable t) {
		}
	}

	public void showFrameComparator() {
		if (dnaComparator == null)
			return;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				final JFrame f = new JFrame("DNAComparator");
				f.getContentPane().setLayout(new BorderLayout());
				final DNAComparator newComparator = new DNAComparator(getModel());
				f.getContentPane().add(newComparator, BorderLayout.CENTER);
				f.setResizable(false);
				f.pack();
				f.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent evt) {
						getModel().removeDNAHistoryListener(newComparator);
						f.dispose();
					}
				});
				f.setVisible(true);
			}
		});
	}

	public DNAComparator getDNAComparator() {
		if (dnaComparator == null) {
			dnaComparator = new DNAComparator(getModel());
		}
		return dnaComparator;
	}

	public void setDefaultRibosomeImage() {
		try {
			URL imageURL = DNAScrollerWithRNA.class.getResource("/org/concord/molbio/ui/images/ribosome.jpg");
			Image img = Toolkit.getDefaultToolkit().createImage(imageURL);
			MediaTracker mt = new MediaTracker(this);
			mt.addImage(img, 0);
			mt.waitForAll();
			BufferedImage bim = createImageFromImage(img);
			setRibosomeImage(bim);
		}
		catch (Throwable t) {
			System.out.println("grabbingImage Throwable " + t);
		}
	}

	static void createButtons(JFrame f, final DNAScrollerWithRNA scroller) {
		JButton prevButton = null;
		JButton resetButton = new JButton(new ResetDNAScrollerWithRNAAction(scroller));
		resetButton.setSize(resetButton.getPreferredSize());
		int yButton = f.getSize().height - 140 - resetButton.getSize().height;
		int xButton = 5;
		resetButton.setLocation(xButton, yButton);
		f.getContentPane().add(resetButton);

		prevButton = resetButton;
		xButton = prevButton.getLocation().x + prevButton.getSize().width + 5;
		JButton startTranscriptionButton = new JButton(new StartTranscriptionDNAScrollerWithRNAAction(scroller));
		startTranscriptionButton.setSize(startTranscriptionButton.getPreferredSize());
		startTranscriptionButton.setLocation(xButton, yButton);
		f.getContentPane().add(startTranscriptionButton);

		prevButton = startTranscriptionButton;
		xButton = prevButton.getLocation().x + prevButton.getSize().width + 5;
		JButton startTranslationButton = new JButton(new StartTranslationDNAScrollerWithRNAAction(scroller));
		startTranslationButton.setSize(startTranslationButton.getPreferredSize());
		startTranslationButton.setLocation(xButton, yButton);
		f.getContentPane().add(startTranslationButton);

		prevButton = null;
		yButton += 26;
		xButton = 5;
		JButton startTranscription = new JButton(new BeginTranscriptionDNAScrollerWithRNAAction(scroller));
		startTranscription.setSize(startTranscription.getPreferredSize());
		startTranscription.setLocation(xButton, yButton);
		f.getContentPane().add(startTranscription);

		prevButton = startTranscription;
		xButton = prevButton.getLocation().x + prevButton.getSize().width + 5;
		JButton effect1Transcription = new JButton("Transcription Eff. 1");
		effect1Transcription.setSize(effect1Transcription.getPreferredSize());
		effect1Transcription.setLocation(xButton, yButton);
		f.getContentPane().add(effect1Transcription);
		effect1Transcription.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				scroller.startTranscriptionEffectBegin();
			}
		});

		prevButton = effect1Transcription;
		xButton = prevButton.getLocation().x + prevButton.getSize().width + 5;
		JButton transcriptionButton = new JButton(new TranscriptionDNAScrollerWithRNAAction(scroller));
		transcriptionButton.setSize(transcriptionButton.getPreferredSize());
		transcriptionButton.setLocation(xButton, yButton);
		f.getContentPane().add(transcriptionButton);

		prevButton = transcriptionButton;
		xButton = prevButton.getLocation().x + prevButton.getSize().width + 5;
		JButton effect2Transcription = new JButton("Transcription Eff. 2");
		effect2Transcription.setSize(effect2Transcription.getPreferredSize());
		effect2Transcription.setLocation(xButton, yButton);
		f.getContentPane().add(effect2Transcription);
		effect2Transcription.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				scroller.startTranscriptionEffectEnd();
			}
		});

		yButton += 26;
		prevButton = null;
		xButton = 5;
		JButton startTranslation = new JButton("Begin Translation");
		startTranslation.setSize(startTranslation.getPreferredSize());
		startTranslation.setLocation(xButton, yButton);
		f.getContentPane().add(startTranslation);

		prevButton = startTranslation;
		xButton = prevButton.getLocation().x + prevButton.getSize().width + 5;
		JButton transclationButton = new JButton(new TransclationDNAScrollerWithRNAAction(scroller));
		transclationButton.setSize(transclationButton.getPreferredSize());
		transclationButton.setLocation(xButton, yButton);
		f.getContentPane().add(transclationButton);

		prevButton = transclationButton;
		xButton = prevButton.getLocation().x + prevButton.getSize().width + 5;
		JButton comparatorFromFrameButton = new JButton("Comparator In frame");
		comparatorFromFrameButton.setSize(comparatorFromFrameButton.getPreferredSize());
		comparatorFromFrameButton.setLocation(xButton, yButton);
		f.getContentPane().add(comparatorFromFrameButton);
		comparatorFromFrameButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				scroller.showFrameComparator();
			}
		});

		prevButton = null;
		yButton += 26;
		xButton = 5;
		JButton suspendButton = new JButton("Suspend");
		suspendButton.setSize(suspendButton.getPreferredSize());
		suspendButton.setLocation(xButton, yButton);
		f.getContentPane().add(suspendButton);
		suspendButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				scroller.suspendSimulation();
			}
		});

		prevButton = suspendButton;
		xButton = prevButton.getLocation().x + prevButton.getSize().width + 5;
		JButton resumeButton = new JButton("Resume");
		resumeButton.setSize(resumeButton.getPreferredSize());
		resumeButton.setLocation(xButton, yButton);
		f.getContentPane().add(resumeButton);
		resumeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				scroller.resumeSimulation();
			}
		});

		prevButton = resumeButton;
		xButton = prevButton.getLocation().x + prevButton.getSize().width + 5;
		JButton doOneStepButton = new JButton("One Step");
		doOneStepButton.setSize(doOneStepButton.getPreferredSize());
		doOneStepButton.setLocation(xButton, yButton);
		f.getContentPane().add(doOneStepButton);
		doOneStepButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				scroller.doOneStep();
			}
		});

		prevButton = doOneStepButton;
		xButton = prevButton.getLocation().x + prevButton.getSize().width + 5;
		JButton clearHistoryButton = new JButton("Clear History");
		clearHistoryButton.setSize(clearHistoryButton.getPreferredSize());
		clearHistoryButton.setLocation(xButton, yButton);
		f.getContentPane().add(clearHistoryButton);
		clearHistoryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				scroller.restoreToOriginalDNA();
			}
		});

		prevButton = clearHistoryButton;
		xButton = prevButton.getLocation().x + prevButton.getSize().width + 5;
		JButton mutationSubstitutionButton = new JButton("Substitute");
		mutationSubstitutionButton.setSize(mutationSubstitutionButton.getPreferredSize());
		mutationSubstitutionButton.setLocation(xButton, yButton);
		f.getContentPane().add(mutationSubstitutionButton);
		mutationSubstitutionButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				scroller.mutateWithSubstitution(0.3f);
			}
		});

		prevButton = mutationSubstitutionButton;
		xButton = prevButton.getLocation().x + prevButton.getSize().width + 5;
		JButton mutationDeletionInsertionButton = new JButton("Del/Insert");
		mutationDeletionInsertionButton.setSize(mutationDeletionInsertionButton.getPreferredSize());
		mutationDeletionInsertionButton.setLocation(xButton, yButton);
		f.getContentPane().add(mutationDeletionInsertionButton);
		mutationDeletionInsertionButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				scroller.mutateWithDeletionInsertion(1);
			}
		});

	}

	public void resetGUIScroller() {
		final DNAScrollerModel model = getModel();
		if (model != null) {
			model.setStartWindowIndex(0);
			int needCurrIndex = 0;
			if (scrollerState == SCROLLER_TRANSLATION_READY_STATE) {
				if (currentCodon >= 0)
					needCurrIndex = 3 * currentCodon;
				int oldCurrentCodon = currentCodon;
				int nCodons = getMaxCodonsInScroller();
				checkInitPromoterForTranslation();
				currentCodon = oldCurrentCodon;
				int needStart = currentCodon - nCodons / 4;
				if (needStart < 0)
					needStart = 0;
				model.setStartWindowIndex(needStart * 3);
			}
			else if (scrollerState == SCROLLER_TRANSCRIPTION_READY_STATE) {
				if (currentBase >= 0)
					needCurrIndex = 3 * (currentBase / 3);
			}
			model.setCurrIndex(needCurrIndex);
			setOpOffset();
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					updateScrollBar();
					scrollBar.setValue(model.getStartWindowIndex() / 3);
				}
			});
		}
		createRectangles();
	}

	void checkInitPromoterForTranslation() {
		if (scrollerState != SCROLLER_TRANSLATION_READY_STATE)
			return;
		if (currentCodon < 0) {
			DNAScrollerModel model = getModel();
			if (model == null)
				return;
			DNA dna = model.getDNA();
			if (dna == null)
				return;
			if (dna.startWithPromoter()) {
				currentCodon = DNA.PROMOTER_LENGTH / 3 - 1;
			}
			// if(dna.hasStartFragment()) currentCodon++;
		}
	}

	void checkInitPromoterForTranscription() {

		if (scrollerState != SCROLLER_TRANSCRIPTION_READY_STATE)
			return;
		DNAScrollerModel model = getModel();
		if (model == null)
			return;
		DNA dna = model.getDNA();
		if (dna == null)
			return;
		if (currentBase < 0) {
			if (dna.startWithPromoter()) {
				currentBase = DNA.PROMOTER_LENGTH - 1;
			}
		}
	}

	public void reset() {
		stopAllThreads();
		currentBase = -1;
		currentCodon = -1;
		checkInitPromoterForTranslation();
		checkInitPromoterForTranscription();
		clearProtein();
		setScrollerState(SCROLLER_NORMAL_STATE);
		resetGUIScroller();
	}

	public void resetToStartTranscription() {
		stopAllThreads();
		clearProtein();
		currentBase = -1;
		setScrollerState(SCROLLER_TRANSCRIPTION_READY_STATE);
		checkInitPromoterForTranscription();
		resetGUIScroller();
	}

	public void resetToStartTranslation() {
		stopAllThreads();
		currentBase = -1;
		currentCodon = -1;
		clearProtein();
		setScrollerState(SCROLLER_TRANSLATION_READY_STATE);
		checkInitPromoterForTranslation();
		resetGUIScroller();
	}

	void clearProtein() {
		protein = null;
		aminoacids = null;
	}

	void createProtein() {
		clearProtein();
		DNAScrollerModel model = getModel();
		if (model == null)
			return;
		DNA dna = model.getDNA();
		if (dna == null)
			return;
		RNA rna = model.getRNA();
		if (rna == null)
			return;
		protein = rna.translate();
		if (protein != null)
			aminoacids = protein.getAminoacids();
	}

	/*
	 * mutate dna with given percent of number of nucleotide to be mutated @param ratioToMutate how many nucleotides to
	 * be mutated should be less than 1 and more that 0
	 * 
	 */
	public void mutateWithSubstitution(float ratioToMutate) {
		if (getModel() != null)
			getModel().mutateWithSubstitution(ratioToMutate);
		repaint();
	}

	/*
	 * mutate dna with given percent of number of nucleotide to be mutated @param ratioToMutate probability of
	 * insertion/deletion for every transcription
	 * 
	 */
	public void mutateWithDeletionInsertion(float ratioToMutate) {
		if (getModel() != null)
			getModel().mutateWithDeletionInsertion(ratioToMutate);
		repaint();
	}

	/**
	 * restore to the original DNA (before mutations)
	 */
	public void restoreToOriginalDNA() {
		if (getModel() != null)
			getModel().restoreToOriginalDNA();
		repaint();
	}

	public synchronized void startTranscription() {
		if (startTranscriptionWithEffect) {
			startTranscriptionEffectBegin();
		}
		else {
			startTranscription(true);
		}
	}

	public boolean isTranscriptionEnded() {
		return transcriptionEnded;
	}

	/*
	 * Returns the current transcription dt in ms @return the current transcription dt in ms.
	 * 
	 * @see #setTranscriptionDT
	 */
	public int getTranscriptionDT() {
		return transcriptionDT;
	}

	/*
	 * set the current transcription dt in ms @param transcriptionDT the current transcription dt in ms. if
	 * transcriptionDT is zero then transcription occurs immediately after every transcription step transcription
	 * listeners will be notified
	 */
	public void setTranscriptionDT(int transcriptionDT) {
		this.transcriptionDT = (transcriptionDT >= 0) ? transcriptionDT : 0;
	}

	public void startTranscription(boolean withRunning) {
		oneStepMode = !withRunning;
		transcriptionEndedInternal = false;
		transcriptionEnded = false;
		clearTranscriptionThread();
		notifyTranscriptionListeners(RNATranscriptionListener.MODE_TRANSCRIPTION_START);
		if (transcriptionDT == 0) {
			DNAScrollerModel model = getModel();
			if (model == null)
				return;
			DNA dna = model.getDNA();
			if (dna == null)
				return;
			int maxIndex = dna.getLength();
			currentBase = maxIndex - 1;
			nextTranscriptionStep();
		}
		else {
			transcriptionThread = new Thread(new Runnable() {
				public void run() {
					try {
						int postDelay = (transcriptionDT < 1000) ? transcriptionDT / 2 : 500;
						while (!transcriptionEndedInternal) {
							if (isRunning()) {
								drawLastRNABase = false;
								nextTranscriptionStep();
								Thread.sleep(postDelay);
								drawLastRNABase = true;
								repaint();
							}
							Thread.sleep(transcriptionDT - postDelay);
						}
					}
					catch (Throwable t) {
					}
				}
			});
			setRunning(withRunning);
			transcriptionThread.setName("Transcription thread");
			transcriptionThread.setPriority(Thread.MIN_PRIORITY);
			transcriptionThread.start();
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					scrollBar.setEnabled(false);
				}
			});
		}
	}

	public void nextTranscriptionStep() {
		final DNAScrollerModel model = getModel();
		if (model == null)
			return;
		DNA dna = model.getDNA();
		if (dna == null)
			return;
		int maxIndex = dna.getLength();
		if (dna.endWithTerminator()) {
			maxIndex -= DNA.TERMINATOR_LENGTH;
		}
		checkInitPromoterForTranscription();
		if (currentBase < maxIndex - 1) {
			currentBase++;
			int startindex = model.getStartWindowIndex();
			int maxCodons = getMaxCodonsInScroller();
			if ((maxIndex - startindex >= 3 * maxCodons) && (currentBase - startindex > 3 * maxCodons / 2)) {
				model.setStartWindowIndex(startindex + 3);
			}
			model.setCurrIndex(3 * (currentBase / 3));
			setOpOffset();
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					updateScrollBar();
					scrollBar.setValue(model.getStartWindowIndex() / 3);
					repaint();
				}
			});
			notifyTranscriptionListeners(currentBase, RNATranscriptionListener.MODE_TRANSCRIPTION_BASE);
		}
		else {
			drawLastRNABase = true;
			notifyTranscriptionListeners(RNATranscriptionListener.MODE_TRANSCRIPTION_END);
			stopTranscription();
			transcriptionEnded = true;
			repaint();
		}
	}

	public void clearThreads() {
		clearTranscriptionThread();
	}

	public void stopTranscription() {
		setRunning(false);
		if (!transcriptionEndedInternal) {
			notifyTranscriptionListeners(RNATranscriptionListener.MODE_TRANSCRIPTION_STOP);
		}
		transcriptionEndedInternal = true;
		clearTranscriptionThread();
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				scrollBar.setEnabled(true);
			}
		});
	}

	void clearTranscriptionThread() {
		if (transcriptionThread != null && transcriptionThread.isAlive()) {
			transcriptionThread.interrupt();
			try {
				transcriptionThread.join();
			}
			catch (Throwable t) {
			}
		}
		transcriptionThread = null;
	}

	public synchronized void startTranslation() {
		if (startTranslationWithEffect) {
			startTranscriptionEffectEnd();
		}
		else {
			startTranslation(true);
		}
	}

	public boolean isTranslationEnded() {
		return translationEnded;
	}

	/*
	 * Returns the current translation dt in ms @return the current translation dt in ms.
	 * 
	 * @see #setTranscriptionDT
	 */
	public int getTranslationDT() {
		return translationDT;
	}

	/*
	 * set the current translation dt in ms @param translationDT the current transcription dt in ms. if translationDT is
	 * zero then translation occurs immediately after every translation step translation listeners will be notified
	 * however translation listener should mark translation event as consumed if listener will fail to do so then next
	 * translation step will never occurred unless listener will call notifyTranslation you can take a look at
	 * TestComponent example about how listener can communicate with DNAScrollerWithRNA
	 */
	public void setTranslationDT(int translationDT) {
		this.translationDT = (translationDT >= 0) ? translationDT : 0;
	}

	public void startTranslation(boolean withRunning) {
		oneStepMode = !withRunning;
		createProtein();
		DNAScrollerModel model = getModel();
		if (model == null)
			return;
		DNA dna = model.getDNA();
		if (dna == null)
			return;
		if (protein == null || aminoacids == null)
			return;

		translationEndedInternal = false;
		translationEnded = false;
		clearTranslationThread();
		notifyTranslationListeners(RNATranslationListener.MODE_TRANSLATION_START);
		if (translationDT == 0) {
			int maxIndex = aminoacids.length;
			currentCodon = maxIndex - 1;
			nextTranslationStep();
		}
		else {
			translationThread = new Thread(new Runnable() {
				public void run() {
					try {
						while (!translationEndedInternal) {
							if (isRunning()) {
								nextTranslationStep();
								repaint();
							}
							Thread.sleep(translationDT);
						}
					}
					catch (Throwable t) {
					}
				}
			});
			setRunning(withRunning);
			translationThread.setName("Translation thread");
			translationThread.setPriority(Thread.MIN_PRIORITY);
			translationThread.start();
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					scrollBar.setEnabled(false);
				}
			});
		}
	}

	public void nextTranslationStep() {
		if (aminoacids == null)
			return;
		final DNAScrollerModel model = getModel();
		if (model == null)
			return;
		DNA dna = model.getDNA();
		if (dna == null)
			return;
		int maxIndex = aminoacids.length;
		int maxDNAIndex = dna.getLength() / 3;
		checkInitPromoterForTranslation();
		if (currentCodon < maxIndex - 1) {
			currentCodon++;
			int startindex = model.getStartWindowIndex();
			int maxCodons = getMaxCodonsInScroller();
			if ((3 * maxDNAIndex - startindex >= 3 * maxCodons) && (3 * currentCodon - startindex > 3 * maxCodons / 4)) {
				model.setStartWindowIndex(startindex + 3);
			}
			model.setCurrIndex(3 * currentCodon);
			setOpOffset();
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					updateScrollBar();
					scrollBar.setValue(model.getStartWindowIndex() / 3);
					repaint();
				}
			});
			if (op != null && rRNA != null && rRNA.length > 0) {
				Insets insets = getInsets();
				int xop = insets.left + (int) op.getX() - getCodonWidth() / 2;
				int yop = insets.top + rRNA[0].y;
				Point pt = SwingUtilities.convertPoint(this, xop, yop, getParent());
				Rectangle ribosomeRect = new Rectangle(insets.left + (int) op.getX() - Math.round(op.getR() - 2), yop
						- Math.round(op.getH()), Math.round(2 * (op.getR() - 2)), Math.round(2 * op.getH()));
				ribosomeRect = SwingUtilities.convertRectangle(this, ribosomeRect, getParent());
				notifyTranslationListeners(aminoacids[currentCodon], model.get53Codon(3 * currentCodon), pt,
						ribosomeRect, RNATranslationListener.MODE_TRANSLATION_NEW_AMINO);
			}
		}
		else {
			notifyTranslationListeners(RNATranslationListener.MODE_TRANSLATION_END);
			stopTranslation();
			translationEnded = true;
			repaint();
		}
		if (translationDT > 0 && translationThread != null) {
			synchronized (translationThread) {
				try {
					translationThread.wait();
				}
				catch (Throwable t) {
				}
			}
		}
	}

	void clearTranslationThread() {
		if (translationThread != null && translationThread.isAlive()) {
			translationThread.interrupt();
			try {
				translationThread.join();
			}
			catch (Throwable t) {
			}
		}
		translationThread = null;
	}

	public void stopTranslation() {
		setRunning(false);
		if (!translationEndedInternal) {
			notifyTranslationListeners(RNATranslationListener.MODE_TRANSLATION_STOP);
		}
		translationEndedInternal = true;
		clearTranslationThread();
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				scrollBar.setEnabled(true);
			}
		});
	}

	public void stopAllThreads() {
		stopTranscription();
		stopTranslation();
	}

	void notifyTranscriptionListeners(int mode) {
		notifyTranscriptionListeners(currentBase, mode);
	}

	void notifyTranscriptionListeners(int baseIndex, int mode) {
		if (RNATranscriptionListeners == null || RNATranscriptionListeners.size() < 1)
			return;
		RNATranscriptionEvent evt = new RNATranscriptionEvent(this, baseIndex, mode);
		boolean totalConsumed = true;
		for (RNATranscriptionListener l : RNATranscriptionListeners) {
			evt.setConsumed(false);
			l.baseTranscripted(evt);
			totalConsumed = totalConsumed && evt.isConsumed();
		}
		// if (totalConsumed && mode == RNATranscriptionListener.MODE_TRANSCRIPTION_BASE)
		// notifyTranslation();
	}

	private void notifyTranslationListeners(int mode) {
		notifyTranslationListeners(null, null, null, null, mode);
	}

	private void notifyTranslationListeners(Aminoacid aminoacid, Codon codon, Point where, Rectangle ribosomeRect,
			int mode) {
		if (RNATranslationListeners == null || RNATranslationListeners.size() < 1)
			return;
		RNATranslationEvent evt = new RNATranslationEvent(this, aminoacid, codon, where, ribosomeRect, mode);
		boolean totalConsumed = true;
		for (RNATranslationListener l : RNATranslationListeners) {
			evt.setConsumed(false);
			l.aminoacidAdded(evt);
			totalConsumed = totalConsumed && evt.isConsumed();
		}
		if (totalConsumed && mode == RNATranslationListener.MODE_TRANSLATION_NEW_AMINO) {
			notifyTranslation();
		}
	}

	public void notifyTranslation() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (translationThread != null) {
					synchronized (translationThread) {
						translationThread.notifyAll();
					}
				}
			}
		});
	}

	public boolean isMutationAfterTranslationDoneAllowed() {
		return mutationAfterTranslationDoneAllowed;
	}

	public void setMutationAfterTranslationDoneAllowed(boolean mutationAfterTranslationDoneAllowed) {
		this.mutationAfterTranslationDoneAllowed = mutationAfterTranslationDoneAllowed;
	}

}
