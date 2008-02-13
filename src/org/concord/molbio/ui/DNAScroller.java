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
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.ItemSelectable;
import java.awt.Font;
import java.awt.Shape;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.border.Border;

import org.concord.molbio.engine.Aminoacid;
import org.concord.molbio.engine.Codon;
import org.concord.molbio.engine.DNA;
import org.concord.molbio.engine.DNAScrollerModel;
import org.concord.molbio.engine.Mutator;
import org.concord.molbio.engine.Nucleotide;
import org.concord.molbio.engine.Protein;
import org.concord.molbio.event.MutationListener;

public class DNAScroller extends JPanel implements ItemSelectable, PropertyChangeListener, Printable, DNAScrollerDrawer {

	static final int M_SUBSTITUTION_A = 0;
	static final int M_SUBSTITUTION_C = 1;
	static final int M_SUBSTITUTION_G = 2;
	static final int M_SUBSTITUTION_T = 3;
	static final int M_SUBSTITUTION_RANDOM = 4;
	static final int M_INSERTION_A = 5;
	static final int M_INSERTION_C = 6;
	static final int M_INSERTION_G = 7;
	static final int M_INSERTION_T = 8;
	static final int M_INSERTION_RANDOM = 9;
	static final int M_DELETION = 10;
	static final int M_MIXED = 11;

	static final Color A_COLOR = Nucleotide.A_COLOR;
	static final Color T_COLOR = Nucleotide.T_COLOR;
	static final Color U_COLOR = Nucleotide.U_COLOR;
	static final Color G_COLOR = Nucleotide.G_COLOR;
	static final Color C_COLOR = Nucleotide.C_COLOR;

	static final int DEFAULT_CODON_DISTANCE = 2;
	static final int DEFAULT_CURRENT_BASE_OFFSETY = 5;

	DNAScrollerModel model;

	private JPopupMenu mutationMenu;
	private JMenu insertionMenu;
	private JMenu substitutionMenu;
	private DNAScrollerMenuItem deletionMenuItem;
	private DNAScrollerMenuItem randomMenuItem;

	JPanel scroller;
	JScrollBar scrollBar;

	int charw = 14;
	int charh = 19;
	private int scrollOffset = 20;
	Rectangle[] charRectangles53;
	Rectangle[] charRectangles35;
	BufferedImage bim;
	private BufferedImage bim2;
	MagnifyGlassOp op;
	private Box box;
	private Point startPoint;
	private float initOpX;

	private int minWidth = -1;
	private GradientPaint gplr;
	private GradientPaint gprl;
	private boolean needDragging;

	Color disableColor = Color.lightGray;
	Color stopCodonColor = Color.red;
	Color[] codonColors = new Color[] { Color.black, Color.darkGray };

	private DNA needSetDNA;
	private boolean wasFirstPainting;

	boolean mutationEnabled = true;

	private DNAScrollerDrawer drawer;

	private boolean needRecalculateAfterResizing;
	private boolean randomMutationSupport;

	private static Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);

	private ArrayList<MutationListenerHolder> mutationListeners;
	private Vector<ItemListener> itemListeners;

	private static boolean doDebugPrinting;

	private static Polygon pAUpper;
	private static Polygon pTULower;
	private static Polygon pGUpper;
	private static Polygon pCLower;
	private static Polygon pALower;
	private static Polygon pTUUpper;
	private static Polygon pCUpper;
	private static Polygon pGLower;

	private static Font codonFont = new Font("Dialog", Font.BOLD, 12);

	private static DNAScroller globalDNAScroller;

	private boolean flashState;
	private int numberFlashes = 3;
	private int flashIntervalMillisec = 300;
	private boolean codonFlashingAfterClickEnable;
	private FlashThread flashThread;
	private boolean mutationMenuWasRequired;

	int currentBase = -1;
	int currentStrand = -1;

	private Color highlightColor;
	private Color highlightColorFlash = Color.green;

	boolean colorSchemeByUsage;

	private Color backboneColor;
	private Color RNAbackboneColor;
	private Timer usageModeTimer;
	private Rectangle backboneTipRectUp;
	private Rectangle backboneTipRectDown;
	private Rectangle rBackBone;

	private static ResourceBundle bundle;
	private static boolean isUSLocale = Locale.getDefault().equals(Locale.US);

	public DNAScroller() {
		this(true);
	}

	public DNAScroller(boolean randomMutationSupport) {

		if (bundle == null && !isUSLocale) {
			try {
				bundle = ResourceBundle.getBundle("org.concord.molbio.ui.properties.DNAScroller", Locale.getDefault());
			}
			catch (MissingResourceException e) {
			}
		}

		mutationMenuWasRequired = false;
		this.randomMutationSupport = randomMutationSupport;
		drawer = this;
		setLayout(new BorderLayout());
		scroller = new JPanel() {
			public void paint(Graphics g) {
				super.paint(g);
				if (!wasFirstPainting) {
					wasFirstPainting = true;
					if (needSetDNA != null) {
						setDNA(needSetDNA);
						needSetDNA = null;
					}
				}
				if (needRecalculateAfterResizing) {
					needRecalculateAfterResizing = false;
					recalculateInternalComponents();
				}
				if (mutationMenu == null)
					createMutationMenu();
				g.setColor(DNAScroller.this.getBackground());
				drawer.draw(g);
			}

			public String getToolTipText(MouseEvent e) {
				return getScrollerToolTipText(e);
			}
		};
		scroller.setToolTipText("DNA Scroller");
		scrollBar = new JScrollBar(JScrollBar.HORIZONTAL) {
			public void paint(Graphics g) {
				super.paint(g);
				DNAScrollerModel dnamodel = DNAScroller.this.getModel();
				if (!isEnabled() || dnamodel == null)
					return;
				int w = minWidth;
				int nbasesinscroller = 3 * (w / getCodonWidth());
				int index = dnamodel.getCurrIndex();
				int startIndex = dnamodel.getStartWindowIndex();
				if (index < startIndex || index >= startIndex + nbasesinscroller) {
					int xl = scrollOffset
							+ (int) Math.round((double) ((w - 2 * scrollOffset) * index)
									/ (double) dnamodel.getDNALength());
					Graphics gc = g.create();
					gc.setColor(Color.green);
					gc.drawLine(xl, 0, xl, getSize().height);
					gc.dispose();
				}
			}
		};

		box = Box.createHorizontalBox();

		scrollBar.getModel().addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				DNAScrollerModel dnamodel = getModel();
				if (dnamodel == null)
					return;
				Object source = e.getSource();
				if (source instanceof BoundedRangeModel) {
					int value = ((BoundedRangeModel) source).getValue();
					dnamodel.setStartWindowIndex(value * 3);
					createRectangles();
					setOpOffset();
					scroller.repaint();
					scrollBar.repaint();
				}
			}

		});
		MouseListener ml = getScrollerMouseListener();
		if (ml != null)
			scroller.addMouseListener(ml);
		MouseMotionListener mml = getScrollerMouseMotionListener();
		if (mml != null)
			scroller.addMouseMotionListener(mml);
		addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent evt) {
				DNAScrollerModel dnamodel = getModel();
				if (dnamodel == null)
					return;
				if (!dnamodel.isStrandsAvailable())
					return;

				DNAScrollerItem dsi = null;
				if (evt.getKeyCode() == KeyEvent.VK_LEFT) {
					adjustCurrentIndex(-3);
					dsi = new DNAScrollerItem();
				}
				else if (evt.getKeyCode() == KeyEvent.VK_RIGHT) {
					adjustCurrentIndex(3);
					dsi = new DNAScrollerItem();
				}
				if (dsi != null) {
					dsi.strandType = DNA.DNA_STRAND_53;
					dsi.baseIndex = dnamodel.getCurrIndex();
					dsi.codon = dnamodel.get53Codon(dsi.baseIndex);
					notifyItemListener(new ItemEvent(DNAScroller.this, ItemEvent.ITEM_STATE_CHANGED, dsi,
							ItemEvent.SELECTED));
				}
			}
		});

		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				if (model != null) {
					resetDNA();
				}
			}
		});
		addAllComponents();
		setBorder(getDefaultBorder());
	}

	public void destroy() {
		if (flashThread != null)
			flashThread.interrupt();
		if (itemListeners != null)
			itemListeners.clear();
		if (mutationListeners != null)
			mutationListeners.clear();
	}

	static String getInternationalText(String name) {
		if (bundle == null)
			return null;
		if (isUSLocale)
			return null;
		if (name == null)
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

	protected Border getDefaultBorder() {
		if (emptyBorder == null)
			emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
		return emptyBorder;
	}

	public MouseMotionListener getScrollerMouseMotionListener() {
		return new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				if (!needDragging || getColorSchemeByUsage())
					return;

				DNAScrollerModel dnamodel = getModel();
				if (dnamodel == null)
					return;
				if (!dnamodel.isStrandsAvailable())
					return;

				int w = minWidth;
				Point curentPoint = e.getPoint();
				if (curentPoint.x >= w - charw || curentPoint.x < 0) {
					defineScrollBar(curentPoint.x);
				}
				else if (startPoint != null) {
					int deltax = curentPoint.x - startPoint.x;
					op.setX(initOpX + deltax);
					scroller.repaint();
					scrollBar.repaint();
				}
			}
		};
	}

	public MouseListener getScrollerMouseListener() {
		return new MouseAdapter() {
			public void mouseReleased(MouseEvent evt) {
				if (usageModeTimer != null && usageModeTimer.isRunning())
					usageModeTimer.stop();
				if (getColorSchemeByUsage()) {
					setColorSchemeByUsage(false);
					return;
				}
				setColorSchemeByUsage(false);
				Shape clipShape = getDefaultClipForStrands();
				if (clipShape != null) {
					Rectangle rClip = clipShape.getBounds();
					if (!rClip.contains(evt.getPoint()))
						return;
				}
				// if(wasRightClick(evt)) return;
				startPoint = null;
				if (needDragging) {
					DNAScrollerModel dnamodel = getModel();
					if (dnamodel == null)
						return;
					if (!dnamodel.isStrandsAvailable())
						return;
					defineScrollBar(evt.getX());
					if (currentBase / 3 != dnamodel.getCurrIndex() / 3) {
						currentBase = 3 * (dnamodel.getCurrIndex() / 3) + (currentBase % 3);
					}
					if (isCodonFlashingAfterClickEnable()) {
						if (!mutationMenuWasRequired) {
							flashCodon();
						}
						else {
							mutationMenuWasRequired = false;
						}
					}
				}
				else {
					needDragging = true;
				}
			}

			public void mousePressed(MouseEvent evt) {
				if (usageModeTimer == null) {
					usageModeTimer = new javax.swing.Timer(2000, new ActionListener() {
						public void actionPerformed(ActionEvent te) {
							setColorSchemeByUsage(true);
						}
					});
					usageModeTimer.setRepeats(false);
				}
				usageModeTimer.restart();
				Shape clipShape = getDefaultClipForStrands();
				if (clipShape != null) {
					Rectangle rClip = clipShape.getBounds();
					if (!rClip.contains(evt.getPoint()))
						return;
				}
				currentBase = -1;
				currentStrand = -1;
				stopFlashingThread();
				mutationMenuWasRequired = false;
				needDragging = false;
				requestFocus();
				DNAScrollerModel dnamodel = getModel();
				if (dnamodel == null)
					return;
				if (!dnamodel.isStrandsAvailable())
					return;
				if (dnamodel.isStrand53Available() && charRectangles53 != null) {
					for (int i = 0; i < charRectangles53.length; i++) {
						if (charRectangles53[i].contains(evt.getPoint())) {
							boolean normalCodons = dnamodel.setCurrIndexToCodonStartFromOffset(i);
							DNAScrollerItem dsi = new DNAScrollerItem();
							dsi.strandType = DNA.DNA_STRAND_53;
							dsi.baseIndex = i + dnamodel.getStartWindowIndex();
							dsi.codon = dnamodel.get53Codon(dsi.baseIndex);
							dsi.rect = charRectangles53[i];
							currentBase = dsi.baseIndex;
							currentStrand = dsi.strandType;
							if (!inPredefinedFragment(i + dnamodel.getStartWindowIndex()) && isMutationEnabled()
									&& needPopupMenu(evt)) {
								handleMutationMenu(evt, dsi);
								// dnamodel.setCurrIndex(oldIndex);
							}
							if (normalCodons) {
								notifyItemListener(new ItemEvent(DNAScroller.this, ItemEvent.ITEM_STATE_CHANGED, dsi,
										ItemEvent.SELECTED));
								setOpOffset();
								scroller.repaint();
								scrollBar.repaint();
								startPoint = evt.getPoint();
								initOpX = op.getX();
								needDragging = true;
							}
							return;
						}
					}
				}
				if (dnamodel.isStrand35Available() && charRectangles35 != null) {
					for (int i = 0; i < charRectangles35.length; i++) {
						if (charRectangles35[i].contains(evt.getPoint())) {
							boolean normalCodons = dnamodel.setCurrIndexToCodonStartFromOffset(i);
							DNAScrollerItem dsi = new DNAScrollerItem();
							dsi.strandType = DNA.DNA_STRAND_35;
							dsi.baseIndex = i + dnamodel.getStartWindowIndex();
							dsi.codon = dnamodel.get53Codon(dsi.baseIndex);
							dsi.rect = charRectangles35[i];
							currentBase = dsi.baseIndex;
							currentStrand = dsi.strandType;
							if (!inPredefinedFragment(i + dnamodel.getStartWindowIndex()) && isMutationEnabled()
									&& needPopupMenu(evt)) {
								handleMutationMenu(evt, dsi);
								// dnamodel.setCurrIndex(oldIndex);
							}
							if (normalCodons) {
								notifyItemListener(new ItemEvent(DNAScroller.this, ItemEvent.ITEM_STATE_CHANGED, dsi,
										ItemEvent.SELECTED));
								setOpOffset();
								scroller.repaint();
								scrollBar.repaint();
								startPoint = evt.getPoint();
								initOpX = op.getX();
								needDragging = true;

							}
							return;
						}
					}
				}
			}
		};
	}

	protected boolean inPromoterInterestingPlace(int currentInd) {
		DNAScrollerModel dnamodel = getModel();
		if (dnamodel == null)
			return false;
		DNA dna = dnamodel.getDNA();
		if (dna == null)
			return false;
		boolean dnaHasPromoter = dna.startWithPromoter();
		if (!dnaHasPromoter)
			return false;
		return (currentInd < 6) || ((currentInd >= 24) && (currentInd < 30));
	}

	protected boolean inPredefinedFragment(int currentInd) {
		return false;
	}

	void stopFlashingThread() {
		if (flashThread != null && flashThread.isAlive()) {
			flashThread.exit();
			try {
				flashThread.join();
			}
			catch (Throwable t) {
			}
			flashThread = null;
		}
	}

	public void flashCodon() {
		stopFlashingThread();
		flashThread = new FlashThread(this);
		/*
		 * try{ thread.join(); }catch(Throwable t){}
		 */
	}

	void addAllComponents() {
		remove(box);
		remove(scroller);
		box.removeAll();
		box.add(scrollBar);
		add(box, BorderLayout.SOUTH);
		add(scroller, BorderLayout.CENTER);
	}

	void createMutationMenu() {
		mutationMenu = new JPopupMenu("");
		populateMenu();
	}

	void clearMutationMenus() {
		if (substitutionMenu != null)
			substitutionMenu.removeAll();
		if (insertionMenu != null)
			insertionMenu.removeAll();
		try {
			Thread.sleep(200);
		}
		catch (Throwable t) {
		}
	}

	synchronized void populateMutationMenus(DNAScrollerItem dnaScrollerItem) {
		if (model == null)
			return;
		char baseChar = 0;
		if (dnaScrollerItem.strandType == DNA.DNA_STRAND_53) {
			baseChar = model.getDNA53String().charAt(dnaScrollerItem.baseIndex - model.getStartWindowIndex());
		}
		else {
			baseChar = model.getDNA35String().charAt(dnaScrollerItem.baseIndex - model.getStartWindowIndex());
		}
		if (baseChar == 0) {
			if (doDebugPrinting)
				System.out.println("baseChar == 0");
			return;
		}
		if (substitutionMenu != null) {
			String insertionString = "Insert ";
			for (int i = 0; i < 4; i++) {
				switch (i) {
				case 0:
					if (baseChar != Nucleotide.ADENINE_NAME) {
						substitutionMenu.add(new DNAScrollerMenuItem(baseChar + " -> A", this, M_SUBSTITUTION_A,
								dnaScrollerItem));
					}
					insertionMenu.add(new DNAScrollerMenuItem(insertionString + " A", this, M_INSERTION_A,
							dnaScrollerItem));
					break;
				case 1:
					if (baseChar != Nucleotide.CYTOSINE_NAME) {
						substitutionMenu.add(new DNAScrollerMenuItem(baseChar + " -> C", this, M_SUBSTITUTION_C,
								dnaScrollerItem));
					}
					insertionMenu.add(new DNAScrollerMenuItem(insertionString + " C", this, M_INSERTION_C,
							dnaScrollerItem));
					break;
				case 2:
					if (baseChar != Nucleotide.GUANINE_NAME) {
						substitutionMenu.add(new DNAScrollerMenuItem(baseChar + " -> G", this, M_SUBSTITUTION_G,
								dnaScrollerItem));
					}
					insertionMenu.add(new DNAScrollerMenuItem(insertionString + " G", this, M_INSERTION_G,
							dnaScrollerItem));
					break;
				case 3:
					if (baseChar != Nucleotide.THYMINE_NAME) {
						substitutionMenu.add(new DNAScrollerMenuItem(baseChar + " -> T", this, M_SUBSTITUTION_T,
								dnaScrollerItem));
					}
					insertionMenu.add(new DNAScrollerMenuItem(insertionString + " T", this, M_INSERTION_T,
							dnaScrollerItem));
					break;
				}
			}
			if (randomMutationSupport) {
				insertionMenu.add(new DNAScrollerMenuItem(baseChar + " -> " + "Random base", this, M_INSERTION_RANDOM,
						dnaScrollerItem));
				substitutionMenu.add(new DNAScrollerMenuItem(baseChar + " -> " + "Random base", this,
						M_SUBSTITUTION_RANDOM, dnaScrollerItem));
			}
			deletionMenuItem.setDNAScrollerMenuItem(dnaScrollerItem);
			if (randomMutationSupport)
				randomMenuItem.setDNAScrollerMenuItem(dnaScrollerItem);
		}
	}

	void handleMutationMenu(MouseEvent evt, final DNAScrollerItem dnaScrollerItem) {
		if (isCodonFlashingAfterClickEnable())
			mutationMenuWasRequired = true;
		clearMutationMenus();
		populateMutationMenus(dnaScrollerItem);
		final MouseEvent mEvent = evt;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				int ym = mEvent.getY();
				int xm = mEvent.getX();
				Rectangle r = dnaScrollerItem.getRect();
				if (r != null) {
					ym = r.y + r.height;
					xm = r.x + r.width;
				}
				if (mutationMenu != null)
					mutationMenu.show(DNAScroller.this, xm, ym);
			}
		});
	}

	protected void populateMenu() {
		if (mutationMenu == null)
			return;
		String s = getInternationalText("SubstitutionMutation");
		substitutionMenu = new JMenu(s != null ? s : "Substitution mutation");
		mutationMenu.add(substitutionMenu);
		s = getInternationalText("InsertionMutation");
		insertionMenu = new JMenu(s != null ? s : "Insertion mutation");
		mutationMenu.add(insertionMenu);
		s = getInternationalText("DeletionMutation");
		deletionMenuItem = new DNAScrollerMenuItem(s != null ? s : "Deletion mutation", this, M_DELETION);
		mutationMenu.add(deletionMenuItem);
		if (randomMutationSupport) {
			s = getInternationalText("RandomMutation");
			randomMenuItem = new DNAScrollerMenuItem(s != null ? s : "Random mutation", this, M_MIXED);
			mutationMenu.add(randomMenuItem);
		}
	}

	public DNAScrollerModel getModel() {
		return model;
	}

	public String get53CurrAminoacidAbbreviation() {
		DNAScrollerModel dnamodel = getModel();
		if (dnamodel == null)
			return null;
		return get53CurrAminoacidAbbreviation(dnamodel.getCurrIndex());
	}

	public String get53CurrAminoacidAbbreviation(int index) {
		DNAScrollerModel dnamodel = getModel();
		if (dnamodel == null)
			return null;
		Codon codon = dnamodel.get53Codon(index);
		if (codon == null)
			return null;
		Aminoacid amino = codon.createAminoacid();
		if (amino == null)
			return null;
		return amino.getAbbreviation();
	}

	public String get35CurrAminoacidAbbreviation() {
		DNAScrollerModel dnamodel = getModel();
		if (dnamodel == null)
			return null;
		return get35CurrAminoacidAbbreviation(dnamodel.getCurrIndex());
	}

	public String get35CurrAminoacidAbbreviation(int index) {
		DNAScrollerModel dnamodel = getModel();
		if (dnamodel == null)
			return null;
		Codon codon = dnamodel.get35Codon(index);
		if (codon == null)
			return null;
		Aminoacid amino = codon.createAminoacid();
		if (amino == null)
			return null;
		return amino.getAbbreviation();
	}

	String getRNAToolTip(MouseEvent evt) {
		return null;
	}

	String getScrollerToolTipText(MouseEvent evt) {
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
		StringBuffer sb = null;
		Codon codon = null;
		if (dnamodel.isStrand53Available() && charRectangles53 != null) {
			for (int i = 0; i < charRectangles53.length; i++) {
				if (charRectangles53[i].contains(evt.getPoint())) {
					String tooltip53 = dnamodel.get53ToolTipString(i, true);
					if (tooltip53 != null) {
						sb = new StringBuffer("(3')");
						sb.append(tooltip53);
						sb.append("(5') ");
						codon = dnamodel.get53CodonFromOffset(i).getTranscripted(true);
					}
					break;
				}
			}
		}
		if (dnamodel.isStrand35Available() && sb == null && charRectangles35 != null) {
			for (int i = 0; i < charRectangles35.length; i++) {
				if (charRectangles35[i].contains(evt.getPoint())) {
					String tooltip35 = dnamodel.get35ToolTipString(i, true);
					if (tooltip35 != null) {
						sb = new StringBuffer("(3')");
						sb.append(tooltip35);
						sb.append("(5') ");
						codon = dnamodel.get35CodonFromOffset(i).getTranscripted(true);
					}
					break;
				}
			}
		}
		if (sb != null && codon != null) {
			sb.append(" -> ");
			if (codon.isCodonStop()) {
				sb.append("Stop");
			}
			else {
				Aminoacid amino = codon.createAminoacid();
				if (amino == null)
					sb.append("???");
				else sb.append(amino.getAbbreviation());
			}
		}

		return sb == null ? null : sb.toString();

	}

	public void setCurrIndex(int index) {
		DNAScrollerModel dnamodel = getModel();
		if (dnamodel == null)
			return;
		dnamodel.setCurrIndex(index);
		updateScrollBar();
	}

	void defineScrollBar(int mx) {
		DNAScrollerModel dnamodel = getModel();
		if (dnamodel == null)
			return;
		int ncodons = (mx - getLeftOffset()) / getCodonWidth();
		dnamodel.setCurrIndexFromOffset(3 * ncodons);
		updateScrollBar();
	}

	void updateScrollBar() {
		DNAScrollerModel dnamodel = getModel();
		if (dnamodel == null)
			return;
		if (dnamodel.getDNA() == null) {
			scrollBar.setValue(0);
			return;
		}
		else if (!scrollBar.isVisible()) {
			scrollBar.setVisible(true);
		}
		int index = dnamodel.getCurrIndex();
		int startindex = dnamodel.getStartWindowIndex();
		int w = minWidth;
		int ncodonsinscroller = w / getCodonWidth();
		if (index < startindex) {
			int oldValue = scrollBar.getValue();
			if (index > 0) {
				dnamodel.setStartIndexToCurrent();
			}
			else {
				dnamodel.setStartWindowIndex(0);
				dnamodel.setCurrIndex(0);
			}
			setOpOffset();
			startindex = dnamodel.getStartWindowIndex();
			if (oldValue * 3 == startindex) {
				scroller.repaint();
				scrollBar.repaint();
			}
			else {
				scrollBar.setValue(startindex / 3);
			}
		}
		else if (index >= startindex + ncodonsinscroller * 3) {
			dnamodel.setStartWindowIndex(index - ncodonsinscroller * 3 + 3);
			setOpOffset();
			scrollBar.setValue(dnamodel.getStartWindowIndex() / 3);
		}
		else {
			setOpOffset();
			scroller.repaint();
			scrollBar.repaint();
		}
	}

	protected void adjustCurrentIndex(int delta) {
		DNAScrollerModel dnamodel = getModel();
		if (dnamodel == null)
			return;
		int index = dnamodel.getCurrIndex();
		dnamodel.setCurrIndex(index + delta);
		updateScrollBar();
	}

	synchronized Rectangle[] getRects53(Graphics g) {
		if (g == null || model == null) {
			repaint(200);
			return null;
		}
		if (model != null && model.getDNA() == null)
			return null;
		Rectangle[] rs = new Rectangle[model.get53StrandLengthFromCurrIndex()];
		FontMetrics fm = g.getFontMetrics();
		int sy = /* scroller.getLocation().y+ */scroller.getSize().height / 2 - 1;
		Rectangle r = fm.getStringBounds(model.getDNA53String(), g).getBounds();
		r.width = charw;
		r.height = charh;
		r.translate(-r.x, -r.y + sy - charh);
		char[] chars = model.get53Chars();
		for (int i = 0; i < chars.length; i++) {
			if (i == 0) {
				r.translate(getCodonDistance(), 0);
			}
			else if ((i % 3) == 0) {
				r.translate(2 * getCodonDistance(), 0);
			}
			rs[i] = new Rectangle(r);
			r.translate(r.width, 0);
		}
		return rs;
	}

	synchronized Rectangle[] getRects35(Graphics g) {
		Rectangle[] rs = getRects53(g);
		if (rs == null)
			return null;
		for (int i = 0; i < rs.length; i++) {
			rs[i].translate(0, (rs[i].height));
		}
		return rs;
	}

	float getYop() {
		return bim.getHeight() / 2;
	}

	void createBufferedImage() {
		Dimension size = scroller.getSize();
		bim = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB_PRE);
		bim2 = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB_PRE);
		createMagnifyGlassOp();
		gplr = new GradientPaint(10, 5, Color.darkGray, bim.getWidth() - 10, 5, Color.lightGray);
		gprl = new GradientPaint(10, bim.getHeight() - 5, Color.lightGray, bim.getWidth() - 10, bim.getHeight() - 5,
				Color.darkGray);
	}

	void createMagnifyGlassOp() {
		float xop = getLeftOffset() + (charw * 3 + 2 * getCodonDistance()) / 2;
		float yop = getYop();
		float rh = charh + 1;
		if (op == null) {
			op = new MagnifyGlassOp(1f, xop, yop, (3 * charw + 2 * getCodonDistance()) / 2, rh,
					MagnifyGlassOp.GLASS_AS_RECTANGLE);
		}
		else {
			op.mx = xop;
			op.my = yop;
			op.rh = rh;
		}
		setHighlightColor(highlightColor);

	}

	void setOpOffset() {
		if (op == null || model == null)
			return;
		float xop = (model.getCurrIndex() - model.getStartWindowIndex()) * getCodonWidth() / 3 + getCodonWidth() / 2;
		op.setX(xop);
	}

	void fillBackground(Graphics2D gbim) {
		if (isOpaque()) {
			gbim.fillRect(0, 0, bim.getWidth(), bim.getHeight());
		}
		else {
			Color bColor = getBackground();
			Composite oldComp = gbim.getComposite();
			gbim.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0));
			gbim.setColor(new Color(bColor.getRed(), bColor.getGreen(), bColor.getBlue(), 0));
			gbim.fillRect(0, 0, bim.getWidth(), bim.getHeight());
			gbim.setComposite(oldComp);
		}
	}

	void drawInImage(Graphics g) {

		if (model == null || g == null)
			return;
		if (bim == null)
			createBufferedImage();
		if (bim == null)
			return;

		Graphics2D gbim = bim.createGraphics();
		gbim.setFont(codonFont);
		gbim.setColor(getBackground());
		fillBackground(gbim);
		if (model != null && model.getDNA() != null) {
			if (charRectangles53 == null)
				charRectangles53 = getRects53(gbim);
			if (charRectangles35 == null)
				charRectangles35 = getRects35(gbim);
			FontMetrics fm = gbim.getFontMetrics();
			char[] chars53 = model.get53Chars();
			char[] chars35 = model.get35Chars();
			if (chars53 == null)
				return;
			Shape oldClip = gbim.getClip();
			Shape clipShape = getDefaultClipForStrands();
			if (clipShape != null)
				gbim.setClip(clipShape);

			if (getColorSchemeByUsage()) {
				drawInUsageMode(gbim);
			}
			else {
				for (int i = 0; i < chars53.length; i++) {
					draw53Codon(i, gbim, chars53, fm);
					draw35Codon(i, gbim, chars35, fm);
				}
				drawRNA(gbim);
			}
			if (clipShape != null) {
				gbim.setClip(oldClip);
			}
			drawArrows(gbim);
		}
		gbim.dispose();
	}

	Shape getDefaultClipForStrands() {
		return null;
	}

	synchronized void drawInUsageMode(Graphics g) {
		Color oldColor = g.getColor();
		int startBase = currentBase;
		if (startBase < 0)
			currentBase = 0;
		try {
			if (charRectangles53 == null || charRectangles53[0] == null)
				return;
			if (charRectangles35 == null || charRectangles35[0] == null)
				return;
			Shape clip = g.getClip();
			Rectangle visRect = clip.getBounds();
			visRect.y = Math.min(charRectangles53[0].y, charRectangles35[0].y);
			visRect.height = Math.max(charRectangles53[0].y + charRectangles53[0].height, charRectangles35[0].y
					+ charRectangles35[0].height)
					- visRect.y;
			DNA dna = model.getDNA();
			if (dna == null)
				return;
			int wGeom = visRect.width;
			int nBases = dna.getLength();
			if (startBase > nBases - 1)
				currentBase = nBases - 1;
			int needOffset = startBase % 3;
			String dnaString = model.getFullDNA53String();
			int promoterMaxIndex = 0;
			if (dna.startWithPromoter() && (needOffset == 0))
				promoterMaxIndex += DNA.PROMOTER_LENGTH;
			int terminatorBeginIndex = dna.getLength();
			if (dna.endWithTerminator() && (needOffset == 0))
				terminatorBeginIndex -= DNA.TERMINATOR_LENGTH;

			double koeff = (double) nBases / (double) wGeom;
			Codon currCodon = null, prevCodon = null;
			Color codonColor = null;
			for (int i = 0; i < wGeom; i++) {
				int codonIndex = (int) Math.round(i * koeff);
				if (codonIndex > nBases - 1)
					break;
				int needOffsetToCodon = 3 * (codonIndex / 3) + needOffset;
				if (needOffsetToCodon + 2 > nBases - 1)
					continue;
				currCodon = Codon.getCodon(dnaString.charAt(needOffsetToCodon),
						dnaString.charAt(needOffsetToCodon + 1), dnaString.charAt(needOffsetToCodon + 2));
				if (currCodon != prevCodon || (codonColor == null)) {
					prevCodon = currCodon;
					if (currCodon == null) {
						codonColor = Color.white;
					}
					else {
						if (currCodon.isCodonStop()) {
							codonColor = Color.red;
						}
						else {
							if (codonIndex < promoterMaxIndex || codonIndex >= terminatorBeginIndex) {
								codonColor = Color.lightGray;
							}
							else if (currCodon.isCodonStart()) {
								codonColor = Color.green;
							}
							else {
								codonColor = Aminoacid.getUsageColor(currCodon.toString());
							}
						}
					}
				}
				if (codonColor == null)
					continue;
				g.setColor(codonColor);
				g.drawLine(visRect.x + i, visRect.y, visRect.x + i, visRect.y + visRect.height);
			}
		}
		catch (Throwable t) {
		}
		g.setColor(oldColor);
	}

	void draw35Codon(int i, Graphics g, char[] chars, FontMetrics fm) {
		if (fm == null)
			fm = g.getFontMetrics();
		int offsety = fm.getDescent();
		int startIndex = model.getStartWindowIndex();
		Codon codon = model.get35CodonFromOffset(i);
		if (codon == null)
			return;
		codon = codon.getTranscripted(true);
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
			drawCodonFrame(g, false, currChar, r, currentBaseBoolean);
			Color needColor = Color.black;
			if (!model.isStrand35Available() || codon == null)
				needColor = disableColor;
			else if (codon.isCodonStop())
				needColor = stopCodonColor;
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
			if (codon == null || codon.isCodonStop() || !model.isStrand35Available())
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
		codon = codon.getTranscripted(true);
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
			drawCodonFrame(g, true, currChar, r, currentBaseBoolean);

			Color needColor = Color.black;
			if (!model.isStrand53Available() || codon == null)
				needColor = disableColor;
			else if (codon.isCodonStop())
				needColor = stopCodonColor;
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
			if (codon == null || codon.isCodonStop() || !model.isStrand53Available())
				g.setColor(currColor);

		}
	}

	void drawRNA(Graphics g) {
	}

	void drawCodonFrame(Graphics g, boolean upperPart, char n, Rectangle r) {
		drawCodonFrame(g, upperPart, n, r, false, null);
	}

	void drawCodonFrame(Graphics g, boolean upperPart, char n, Rectangle r, boolean currentBaseBoolean) {
		drawCodonFrame(g, upperPart, n, r, currentBaseBoolean, null);
	}

	public boolean highlightCurrentBase() {
		return true;
	}

	void drawCodonFrame(Graphics g, boolean upperPart, char n, Rectangle r, boolean currentBaseBoolean, Color forceColor) {
		Color oldColor = g.getColor();
		Polygon poly = null;
		if (n == 'A' || n == 'T' || n == 'U') {
			if (upperPart) {
				poly = (n == 'A') ? pAUpper : pTUUpper;
			}
			else {
				poly = (n == 'A') ? pALower : pTULower;
			}
		}
		else {
			if (upperPart) {
				poly = (n == 'G') ? pGUpper : pCUpper;
			}
			else {
				poly = (n == 'G') ? pGLower : pCLower;
			}
		}
		if (poly == null)
			return;
		poly.translate(r.x, r.y);
		if (currentBaseBoolean && highlightCurrentBase()) {
			g.setColor((forceColor == null) ? getRectColor(n) : forceColor);
		}
		else {
			g.setColor((forceColor == null) ? getRectColor(n) : forceColor);
		}
		g.fillPolygon(poly);
		g.setColor(Color.black);
		g.drawPolygon(poly);
		poly.translate(-r.x, -r.y);
		g.setColor(oldColor);
	}

	void drawBackbone(Graphics g, Rectangle r, boolean upperPart, boolean inRNA, boolean codonBorder) {
		if (g == null)
			return;
		Rectangle backboneTipRect = null;
		Color oldColor = g.getColor();
		Color rColor = getBackboneColor(inRNA);
		if (rBackBone == null)
			rBackBone = new Rectangle(r.x, 0, r.width, 4);
		if (upperPart) {
			rBackBone.translate(r.x - rBackBone.x, r.y - rBackBone.height - rBackBone.y);
			if (codonBorder) {
				if (backboneTipRectUp == null) {
					backboneTipRectUp = new Rectangle(rBackBone.x, rBackBone.y - 2, 2, 2);
				}
				else {
					backboneTipRectUp.translate(rBackBone.x - backboneTipRectUp.x, rBackBone.y - 2
							- backboneTipRectUp.y);
				}
				backboneTipRect = backboneTipRectUp;
			}
		}
		else {
			rBackBone.translate(r.x - rBackBone.x, r.y + r.height - rBackBone.y);
			if (codonBorder) {
				if (backboneTipRectDown == null) {
					backboneTipRectDown = new Rectangle(rBackBone.x, rBackBone.y, 2, 2);
				}
				else {
					backboneTipRectDown.translate(rBackBone.x - backboneTipRectDown.x, rBackBone.y + rBackBone.height
							+ 1 - backboneTipRectDown.y);
				}
				backboneTipRect = backboneTipRectDown;
			}
		}
		g.setColor(rColor);
		((Graphics2D) g).fill(rBackBone);
		g.setColor(rColor.darker());
		((Graphics2D) g).draw(rBackBone);
		g.setColor(Color.black);
		if (backboneTipRect != null)
			((Graphics2D) g).fill(backboneTipRect);
		g.setColor(oldColor);
	}

	static void createPolygonsPoints(Rectangle r) {
		int pw = 5;
		int[] px = new int[8];
		int[] py = new int[8];
		px[0] = 0;
		py[0] = 0;
		px[1] = r.width;
		py[1] = 0;
		px[2] = r.width;
		py[2] = r.height;
		px[3] = r.width - pw;
		py[3] = r.height;
		px[4] = r.width - pw;
		py[4] = r.height - pw;
		px[5] = pw;
		py[5] = r.height - pw;
		px[6] = pw;
		py[6] = r.height;
		px[7] = 0;
		py[7] = r.height;
		pAUpper = new Polygon(px, py, 8);// should be for A upper

		px[0] = 0;
		py[0] = 0;
		px[1] = pw;
		py[1] = 0;
		px[2] = pw;
		py[2] = -pw;
		px[3] = r.width - pw;
		py[3] = -pw;
		px[4] = r.width - pw;
		py[4] = 0;
		px[5] = r.width;
		py[5] = 0;
		px[6] = r.width;
		py[6] = r.height;
		px[7] = 0;
		py[7] = r.height;
		pTULower = new Polygon(px, py, 8);// should be for TU lower

		px[0] = 0;
		py[0] = 0;
		px[1] = r.width;
		py[1] = 0;
		px[2] = r.width;
		py[2] = r.height;
		px[3] = r.width - pw;
		py[3] = r.height;
		px[4] = r.width - pw;
		py[4] = r.height + pw;
		px[5] = pw;
		py[5] = r.height + pw;
		px[6] = pw;
		py[6] = r.height;
		px[7] = 0;
		py[7] = r.height;
		pTUUpper = new Polygon(px, py, 8);// should be for TU upper

		px[0] = 0;
		py[0] = 0;
		px[1] = pw;
		py[1] = 0;
		px[2] = pw;
		py[2] = pw;
		px[3] = r.width - pw;
		py[3] = pw;
		px[4] = r.width - pw;
		py[4] = 0;
		px[5] = r.width;
		py[5] = 0;
		px[6] = r.width;
		py[6] = r.height;
		px[7] = 0;
		py[7] = r.height;
		pALower = new Polygon(px, py, 8);// should be for A lower

		px[0] = 0;
		py[0] = 0;
		px[1] = r.width;
		py[1] = 0;
		px[2] = r.width;
		py[2] = r.height;
		px[3] = r.width;
		py[3] = r.height;
		px[4] = r.width - pw;
		py[4] = r.height - pw;
		px[5] = pw;
		py[5] = r.height - pw;
		px[6] = 0;
		py[6] = r.height;
		px[7] = 0;
		py[7] = r.height;
		pGUpper = new Polygon(px, py, 8); // for G upper

		px[0] = 0;
		py[0] = 0;
		px[1] = 1;
		py[1] = 0;
		px[2] = 1 + pw;
		py[2] = -pw;
		px[3] = r.width - 1 - pw;
		py[3] = -pw;
		px[4] = r.width - 1;
		py[4] = 0;
		px[5] = r.width;
		py[5] = 0;
		px[6] = r.width;
		py[6] = r.height;
		px[7] = 0;
		py[7] = r.height;
		pCLower = new Polygon(px, py, 8); // for C Lower

		px[0] = 0;
		py[0] = 0;
		px[1] = r.width;
		py[1] = 0;
		px[2] = r.width;
		py[2] = r.height - 1;
		px[3] = r.width - 1;
		py[3] = r.height - 1;
		px[4] = r.width - pw - 1;
		py[4] = r.height + pw - 1;
		px[5] = 1 + pw;
		py[5] = r.height + pw - 1;
		px[6] = 1;
		py[6] = r.height - 1;
		px[7] = 0;
		py[7] = r.height - 1;
		pCUpper = new Polygon(px, py, 8); // for C Upper

		px[0] = 0;
		py[0] = 0;
		px[1] = 1;
		py[1] = 0;
		px[2] = 1 + pw;
		py[2] = pw;
		px[3] = r.width - 1 - pw;
		py[3] = pw;
		px[4] = r.width - 1;
		py[4] = 0;
		px[5] = r.width;
		py[5] = 0;
		px[6] = r.width;
		py[6] = r.height;
		px[7] = 0;
		py[7] = r.height;
		pGLower = new Polygon(px, py, 8); // for G Lower

	}

	void drawArrows(Graphics g) {
		if (g == null || bim == null || gprl == null || gplr == null)
			return;
		int initxoffset = 15;
		int inityoffset = 5;
		int l0 = 3;
		int l = l0;

		Dimension scrollerSize = scroller.getSize();

		Graphics2D g2d = (Graphics2D) g;
		FontMetrics fm = g2d.getFontMetrics();
		int arrowlength = scrollerSize.width - 2 * initxoffset;
		Paint oldc = g2d.getPaint();
		g2d.setPaint(gplr);
		g2d.fillRect(initxoffset, inityoffset - 1, arrowlength, 3);
		g2d.setPaint(Color.lightGray);
		int xend = initxoffset + arrowlength;
		for (int i = 0; i < 4; i++) {
			g2d.drawLine(xend, inityoffset - l, xend, inityoffset + l);
			xend++;
			l--;
		}
		l = l0;
		g2d.setPaint(gprl);
		g2d.fillRect(initxoffset, bim.getHeight() - inityoffset - 1, arrowlength, 3);
		g2d.setPaint(Color.lightGray);
		xend = initxoffset + 2;
		for (int i = 0; i < 4; i++) {
			g2d.drawLine(xend, bim.getHeight() - inityoffset - l, xend, bim.getHeight() - inityoffset + l);
			xend--;
			l--;
		}
		g2d.setPaint(Color.darkGray);
		g2d.drawString("5'", 3f, inityoffset / 2f + fm.getHeight() / 2f);
		g2d.drawString("5'", initxoffset + arrowlength + 3, bim.getHeight() - inityoffset / 2f);
		g2d.setPaint(Color.lightGray);
		g2d.drawString("3'", initxoffset + arrowlength + 3, +inityoffset / 2f + fm.getHeight() / 2f);
		g2d.drawString("3'", 3f, bim.getHeight() - inityoffset / 2f);

		g2d.setPaint(oldc);

	}

	public MagnifyGlassOp getMagnifyGlassOp() {
		if (getColorSchemeByUsage())
			return null;
		return op;
	}

	public void setOpaque(boolean val) {
		super.setOpaque(val);
		if (scroller != null)
			scroller.setOpaque(val);
	}

	public synchronized void draw(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		Graphics2D gbim2 = (bim2 == null) ? (Graphics2D) g2 : bim2.createGraphics();
		if (!isOpaque() && bim2 != null) {
			Composite oldComp = gbim2.getComposite();
			gbim2.setComposite(AlphaComposite.Clear);
			gbim2.setColor(new Color(0, 0, 0, 0));
			gbim2.fillRect(0, 0, bim2.getWidth(), bim2.getHeight());
			gbim2.setComposite(oldComp);
		}
		drawInImage(gbim2);
		if (bim != null) {
			if (model != null && model.getDNA() != null) {
				MagnifyGlassOp currOp = getMagnifyGlassOp();
				if (currOp != null)
					currOp.setNeedClip(getDefaultClipForStrands());
				gbim2.drawImage(bim, currOp, 0, 0);
				if (currOp != null)
					currOp.setNeedClip(null);
			}
			else {
				gbim2.drawImage(bim, null, 0, 0);
			}
		}
		if (gbim2 != g2)
			gbim2.dispose();
		if (bim2 != null) {
			g2.drawImage(bim2, null, 0, 0);
		}
	}

	int getCodonDistance() {
		return DEFAULT_CODON_DISTANCE;
	}

	int getCodonWidth() {
		return 3 * charw + 2 * getCodonDistance();
	}

	public void resetDNA() {
		if (model == null)
			return;
		revalidate();
		model.resetDNA();
		needRecalculateAfterResizing = true;
		repaint();
	}

	void recalculateInternalComponents() {
		if (bim != null) {
			bim.flush();
			bim = null;
		}
		if (bim2 != null) {
			bim2.flush();
			bim2 = null;
		}
		createBufferedImage();
		createRectangles();
		setGeometry();
		updateScrollBar();
	}

	public void setDNA(DNA dna) {
		if (!wasFirstPainting) {
			needSetDNA = dna;
			return;
		}
		if (model == null) {
			model = new DNAScrollerModel(dna);
			model.addPropertyChangeListener(this);
		}
		else {
			model.clearMutationListeners();
			model.setDNA(dna);
		}
		createRectangles();
		setGeometry();
		if (dna != null) {
			if (mutationListeners != null) {
				for (MutationListenerHolder h : mutationListeners) {
					if (h == null)
						continue;
					if (h.strandIndex < 0)
						model.addMutationListener(h.l);
					else model.addMutationListener(h.strandIndex, h.l);
				}
			}
		}
		else {
			updateScrollBar();
			repaint();
		}

	}

	int getMaxCodonsInScroller() {
		int codonWidth = getCodonWidth();
		int w = scroller.getSize().width;
		return w / codonWidth;
	}

	void setGeometry() {
		int codonWidth = getCodonWidth();
		int w = scroller.getSize().width - 2 * getLeftOffset();
		int ncodons = model.getCodonsNumber();
		if (model.isUnfinishedCodon())
			ncodons++;
		int dnawidth = codonWidth * ncodons;
		minWidth = Math.min(w, dnawidth);
		model.setNBaseInWindow(minWidth / getCodonWidth());
		if (scrollBar == null)
			return;
		if (dnawidth < w) {
			scrollBar.setEnabled(false);
			return;
		}
		scrollBar.setEnabled(true);
		scrollBar.setMaximum(ncodons);
		scrollBar.setVisibleAmount(minWidth / codonWidth);
	}

	int getLeftOffset() {
		return 0;
	}

	void createRectangles() {
		Graphics g = getGraphics();
		if (g != null) {
			charRectangles53 = getRects53(g);
			charRectangles35 = getRects35(g);
			g.dispose();
			if (charRectangles53 != null && charRectangles53.length > 0)
				createPolygonsPoints(charRectangles53[0]);
		}
	}

	protected Dimension pPreferredSize = null;

	public void setPreferredSize(Dimension d) {
		super.setPreferredSize(d);
		pPreferredSize = d;
	}

	public Dimension getPreferredSize() {
		return (pPreferredSize != null) ? pPreferredSize : new Dimension(400, 110);
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("strandAvailability")) {
			repaint();
		}
		else if (evt.getPropertyName().equals("wasMutation")) {
			createRectangles();
			setGeometry();
			repaint();
			Object mutationSource = evt.getNewValue();
			if (mutationSource instanceof Mutator) {
				if (doDebugPrinting)
					System.out.println(mutationSource);
			}

		}
	}

	public Object[] getSelectedObjects() {
		if (model == null)
			return null;
		DNAScrollerItem dsi = new DNAScrollerItem();
		dsi.strandType = currentStrand;
		dsi.baseIndex = currentBase;
		dsi.codon = model.get53Codon(dsi.baseIndex);
		int baseIndex = dsi.baseIndex - model.getStartWindowIndex();
		Rectangle[] rects = null;
		if (currentStrand == DNA.DNA_STRAND_53)
			rects = charRectangles53;
		else if (currentStrand == DNA.DNA_STRAND_35)
			rects = charRectangles35;
		if (rects != null && baseIndex >= 0 && baseIndex < rects.length)
			dsi.rect = rects[baseIndex];
		return new Object[] { dsi };
	}

	public int getCurrentBase() {
		return currentBase;
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
		if (l == null)
			return;
		if (itemListeners == null)
			return;
		itemListeners.removeElement(l);
	}

	public void notifyItemListener(ItemEvent ie) {
		if (itemListeners == null || itemListeners.isEmpty())
			return;
		for (ItemListener l : itemListeners)
			l.itemStateChanged(ie);
	}

	public void setMutatorToStrand(int strandType, int kind, Mutator mutator) throws IllegalArgumentException {
		if (model != null)
			model.setMutatorToStrand(strandType, kind, mutator);
	}

	public void addMutationListener(int strandType, MutationListener l) throws IllegalArgumentException {
		if (mutationListeners == null)
			mutationListeners = new ArrayList<MutationListenerHolder>();
		if (!mutationListeners.contains(l))
			mutationListeners.add(new MutationListenerHolder(strandType, l));
		if (model != null)
			model.addMutationListener(strandType, l);
	}

	public void addMutationListener(MutationListener l) throws IllegalArgumentException {
		if (mutationListeners == null)
			mutationListeners = new ArrayList<MutationListenerHolder>();
		if (!mutationListeners.contains(l))
			mutationListeners.add(new MutationListenerHolder(l));
		if (model != null)
			model.addMutationListener(l);
	}

	public void removeMutationListener(int strandType, MutationListener l) throws IllegalArgumentException {
		if (mutationListeners != null)
			mutationListeners.remove(l);
		if (model != null)
			model.removeMutationListener(strandType, l);
	}

	public void removeMutationListener(MutationListener l) throws IllegalArgumentException {
		if (mutationListeners != null)
			mutationListeners.remove(l);
		if (model != null)
			model.removeMutationListener(l);
	}

	public void setNucleotide(int strandIndex, int nucleotideIndex, Nucleotide nucleotide) {
		if (model == null)
			return;
		model.setNucleotide(strandIndex, nucleotideIndex, nucleotide);
		repaint();
	}

	protected void doMutation(int mutationKind, DNAScrollerItem dnaScrollerItem) {
		if (model == null)
			return;
		int index = dnaScrollerItem.baseIndex;
		Nucleotide nucleotideTo = null;
		int mutatorKind = Mutator.MUTATOR_UNKNOWN;
		switch (mutationKind) {
		case M_SUBSTITUTION_A:
		case M_INSERTION_A:
			nucleotideTo = Nucleotide.getAdenine();
			if (mutationKind == M_SUBSTITUTION_A) {
				mutatorKind = Mutator.MUTATOR_SUBSTITUTION;
			}
			else {
				mutatorKind = Mutator.MUTATOR_INSERTION;
			}
			break;
		case M_SUBSTITUTION_C:
		case M_INSERTION_C:
			nucleotideTo = Nucleotide.getCytosine();
			if (mutationKind == M_SUBSTITUTION_C) {
				mutatorKind = Mutator.MUTATOR_SUBSTITUTION;
			}
			else {
				mutatorKind = Mutator.MUTATOR_INSERTION;
			}
			break;
		case M_SUBSTITUTION_G:
		case M_INSERTION_G:
			nucleotideTo = Nucleotide.getGuanine();
			if (mutationKind == M_SUBSTITUTION_G) {
				mutatorKind = Mutator.MUTATOR_SUBSTITUTION;
			}
			else {
				mutatorKind = Mutator.MUTATOR_INSERTION;
			}
			break;
		case M_SUBSTITUTION_T:
		case M_INSERTION_T:
			nucleotideTo = Nucleotide.getThymine();
			if (mutationKind == M_SUBSTITUTION_T) {
				mutatorKind = Mutator.MUTATOR_SUBSTITUTION;
			}
			else {
				mutatorKind = Mutator.MUTATOR_INSERTION;
			}
			break;
		case M_SUBSTITUTION_RANDOM:
			mutatorKind = Mutator.MUTATOR_SUBSTITUTION;
			break;
		case M_INSERTION_RANDOM:
			mutatorKind = Mutator.MUTATOR_INSERTION;
			break;
		case M_DELETION:
			mutatorKind = Mutator.MUTATOR_DELETION;
			break;
		case M_MIXED:
			mutatorKind = Mutator.MUTATOR_MIXED;
			break;
		}
		mutate(dnaScrollerItem.strandType, mutatorKind, index, nucleotideTo);
	}

	public void mutate(int strand, int mutatorKind, int index, Nucleotide nucleotideTo) {
		if (model != null)
			model.mutate(strand, mutatorKind, index, nucleotideTo);
		repaint();
	}

	public static class DNAScrollerMenuItem extends JMenuItem {
		DNAScrollerItem dnaScrollerItem;
		DNAScroller owner;
		int mutationKind;

		public DNAScrollerMenuItem(String name, DNAScroller owner, int mutationKind) {
			this(name, owner, mutationKind, null);
		}

		public DNAScrollerMenuItem(String name, DNAScroller owner, int mutationKind, DNAScrollerItem dnaScrollerItem) {
			super(name);
			this.owner = owner;
			this.mutationKind = mutationKind;
			setDNAScrollerMenuItem(dnaScrollerItem);
			addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					DNAScrollerMenuItem.this.owner.doMutation(DNAScrollerMenuItem.this.mutationKind,
							DNAScrollerMenuItem.this.dnaScrollerItem);
				}
			});
		}

		public void setDNAScrollerMenuItem(DNAScrollerItem dnaScrollerItem) {
			this.dnaScrollerItem = dnaScrollerItem;
		}
	}

	public static class DNAScrollerItem {
		int baseIndex;
		Codon codon;
		int strandType = DNA.DNA_STRAND_53;
		Rectangle rect;

		public int getBaseIndex() {
			return baseIndex;
		}

		public int getStrandType() {
			return strandType;
		}

		public Codon getCodon() {
			return codon;
		}

		public Rectangle getRect() {
			return rect;
		}

	}

	static boolean wasRightClick(MouseEvent e) {
		return (((e.getModifiers() & InputEvent.CTRL_MASK) != 0) || SwingUtilities.isRightMouseButton(e));
	}

	static boolean needPopupMenu(MouseEvent e) {
		return (((e.getModifiers() & InputEvent.CTRL_MASK) != 0) || e.isPopupTrigger() || SwingUtilities
				.isRightMouseButton(e));
	}

	public int print(Graphics g, PageFormat pf, int pi) throws PrinterException {
		if (pi >= 1) {
			return Printable.NO_SUCH_PAGE;
		}
		Graphics2D g2 = (Graphics2D) g;
		g2.translate(pf.getImageableX(), pf.getImageableY());
		g2.setColor(Color.black);
		paint(g2);
		return Printable.PAGE_EXISTS;
	}

	public void setStrandAvailability(int strandAvailability) {
		if (model != null)
			model.setStrandAvailability(strandAvailability);
	}

	public Protein express() {
		return expressFromStrand(DNA.DNA_STRAND_35);
	}

	public Protein expressFromStrand(int strandType) {
		if (model != null)
			return model.expressFromStrand(strandType);
		return null;
	}

	static void createMenubar(JFrame f, DNAScroller dnaScroller) {
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		JMenuBar menubar = new JMenuBar();
		f.setJMenuBar(menubar);
		JMenu actionmenu = new JMenu("Actions");
		menubar.add(actionmenu);
		ActionListener actionListener = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				if (globalDNAScroller == null)
					return;
				DNAScrollerModel model = globalDNAScroller.getModel();
				if (model == null)
					return;
				String actionString = evt.getActionCommand();
				int strandMode = DNAScrollerModel.DNA_STRAND_AVAILABLE_NONE - 1;
				if (actionString.equals("Enable all strands")) {
					strandMode = DNAScrollerModel.DNA_STRAND_AVAILABLE_BOTH;
				}
				else if (actionString.equals("Enable 53 strand")) {
					if (model.isStrand35Available()) {
						strandMode = DNAScrollerModel.DNA_STRAND_AVAILABLE_BOTH;
					}
					else {
						strandMode = DNAScrollerModel.DNA_STRAND_AVAILABLE_53;
					}
				}
				else if (actionString.equals("Disable 53 strand")) {
					if (model.isStrand35Available()) {
						strandMode = DNAScrollerModel.DNA_STRAND_AVAILABLE_35;
					}
					else {
						strandMode = DNAScrollerModel.DNA_STRAND_AVAILABLE_NONE;
					}
				}
				else if (actionString.equals("Enable 35 strand")) {
					if (model.isStrand53Available()) {
						strandMode = DNAScrollerModel.DNA_STRAND_AVAILABLE_BOTH;
					}
					else {
						strandMode = DNAScrollerModel.DNA_STRAND_AVAILABLE_35;
					}
				}
				else if (actionString.equals("Disable 35 strand")) {
					if (model.isStrand53Available()) {
						strandMode = DNAScrollerModel.DNA_STRAND_AVAILABLE_53;
					}
					else {
						strandMode = DNAScrollerModel.DNA_STRAND_AVAILABLE_NONE;
					}
				}
				if (strandMode != DNAScrollerModel.DNA_STRAND_AVAILABLE_NONE - 1)
					globalDNAScroller.setStrandAvailability(strandMode);
			}
		};

		JMenuItem mi = new JMenuItem("Enable all strands");
		mi.addActionListener(actionListener);
		actionmenu.add(mi);
		mi = new JMenuItem("Enable 53 strand");
		mi.addActionListener(actionListener);
		actionmenu.add(mi);
		mi = new JMenuItem("Disable 53 strand");
		mi.addActionListener(actionListener);
		actionmenu.add(mi);
		mi = new JMenuItem("Enable 35 strand");
		mi.addActionListener(actionListener);
		actionmenu.add(mi);
		mi = new JMenuItem("Disable 35 strand");
		mi.addActionListener(actionListener);
		actionmenu.add(mi);
		JMenu mmi = new JMenu("Mutations");
		actionmenu.add(mmi);

		ActionListener mutationEnablingListener = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				String actionString = evt.getActionCommand();
				if (actionString.equals("Enable")) {
					globalDNAScroller.setMutationEnabled(true);
				}
				else if (actionString.equals("Disable")) {
					globalDNAScroller.setMutationEnabled(false);
				}
			}
		};
		mi = new JMenuItem("Enable");
		mi.addActionListener(mutationEnablingListener);
		mmi.add(mi);
		mi = new JMenuItem("Disable");
		mi.addActionListener(mutationEnablingListener);
		mmi.add(mi);

		mi = new JMenuItem("ResizeTest");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				Dimension d = globalDNAScroller.getSize();
				d.width /= 2;
				d.height *= 2;
				globalDNAScroller.setSize(d);
			}
		});
		mmi.add(mi);

		mi = new JMenuItem("SetNucleotide Test");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				globalDNAScroller.setNucleotide(0, 13, Nucleotide.getCytosine());
			}
		});
		mmi.add(mi);

		JMenu pmenu = new JMenu("PrinterTest");
		menubar.add(pmenu);
		mi = new JMenuItem("Print");
		pmenu.add(mi);

		final Printable scrollerForPrint = dnaScroller;

		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				if (doDebugPrinting)
					System.out.println("print");
				PrinterJob printJob = PrinterJob.getPrinterJob();
				printJob.setPrintable(scrollerForPrint);
				if (printJob.printDialog()) {
					try {
						printJob.print();
					}
					catch (Exception PrintException) {
					}
				}
			}
		});
	}

	public void setDisableColor(Color c) {
		if (c != null)
			disableColor = c;
		else disableColor = Color.lightGray;
	}

	public void setDefaultDisableColor() {
		setDisableColor(null);
	}

	public void setStopCodonColor(Color c) {
		if (c != null)
			stopCodonColor = c;
		else stopCodonColor = Color.red;
	}

	public void setDefaultStopCodonColor() {
		setStopCodonColor(null);
	}

	public void setCodonColor(Color c) {
		if (c != null) {
			codonColors[0] = c;
			if (codonColors[0].equals(Color.black)) {
				codonColors[1] = Color.darkGray;
			}
			else {
				float[] hsb = new float[3];
				Color.RGBtoHSB(codonColors[0].getRed(), codonColors[0].getGreen(), codonColors[0].getBlue(), hsb);
				hsb[2] *= 1.3f;
				if (hsb[2] > 1)
					hsb[2] = 1;
				codonColors[1] = new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
			}
		}
		else {
			codonColors[0] = Color.black;
			codonColors[1] = Color.darkGray;
		}
	}

	public void setDefaultCodonColor() {
		setCodonColor(null);
	}

	public void setMutationEnabled(boolean mutationEnabled) {
		this.mutationEnabled = mutationEnabled;
	}

	public boolean isMutationEnabled() {
		return mutationEnabled;
	}

	public boolean isFocusable() {
		return true;
	}

	public DNAScrollerDrawer getDrawer() {
		return drawer;
	}

	public void setDrawer(DNAScrollerDrawer drawer) {
		this.drawer = drawer;
	}

	public JComponent getScroller() {
		return scroller;
	}

	public void setBorder(Border border) {
		if (border == getDefaultBorder())
			super.setBorder(border);
		else super.setBorder(BorderFactory.createCompoundBorder(border, emptyBorder));
	}

	Color getRectColor(char n) {
		switch (n) {
		case 'A':
		case 'a':
			return A_COLOR;
		case 'U':
		case 'u':
			return U_COLOR;
		case 'G':
		case 'g':
			return G_COLOR;
		case 'T':
		case 't':
			return T_COLOR;
		case 'C':
		case 'c':
			return C_COLOR;
		}
		return Color.white;
	}

	public static Color getCodonColor(char n) {
		switch (n) {
		case 'A':
		case 'a':
			return A_COLOR;
		case 'U':
		case 'u':
			return U_COLOR;
		case 'G':
		case 'g':
			return G_COLOR;
		case 'T':
		case 't':
			return T_COLOR;
		case 'C':
		case 'c':
			return C_COLOR;
		}
		return Color.white;
	}

	synchronized void toggleFlashState() {
		flashState = !flashState;
	}

	synchronized boolean getFlashState() {
		return flashState;
	}

	synchronized void setFlashState(boolean flashState) {
		this.flashState = flashState;
	}

	void clearFlashingState() {
		setFlashState(false);
		setHighlightColor(highlightColor);
	}

	void drawForFlashing() {
		if (scroller == null)
			return;
		Graphics g = scroller.getGraphics();
		g.setColor(getBackground());
		if (op != null) {
			if (getFlashState()) {
				if (highlightColorFlash == null) {
					op.setColorComponents(0, 1, 0);
				}
				else {
					float[] components = highlightColorFlash.getComponents(null);
					op.setColorComponents(components[0], components[1], components[2]);
				}
			}
			else {
				setHighlightColor(highlightColor);
			}
		}
		drawer.draw(g);
		g.dispose();
	}

	public void setHighlightColor(Color c) {
		highlightColor = c;
		if (op == null)
			return;
		if (highlightColor == null) {
			op.setColorComponents();
		}
		else {
			float[] components = c.getComponents(null);
			op.setColorComponents(components[0], components[1], components[2]);
		}
	}

	public void setHighlightColorFlash(Color c) {
		highlightColorFlash = c;
	}

	public int getNumberFlashes() {
		return numberFlashes;
	}

	public void setNumberFlashes(int numberFlashes) {
		this.numberFlashes = (numberFlashes < 1) ? 0 : numberFlashes;
	}

	public int getFlashIntervalMillisec() {
		return flashIntervalMillisec;
	}

	public void setFlashIntervalMillisec(int flashIntervalMillisec) {
		this.flashIntervalMillisec = (flashIntervalMillisec < 100) ? 100 : flashIntervalMillisec;
	}

	public boolean isCodonFlashingAfterClickEnable() {
		return codonFlashingAfterClickEnable;
	}

	public void setCodonFlashingAfterClickEnable(boolean codonFlashingAfterClickEnable) {
		this.codonFlashingAfterClickEnable = codonFlashingAfterClickEnable;
	}

	static BufferedImage createImageFromImage(Image img) {
		return createImageFromImage(img, null);
	}

	static BufferedImage createImageFromImage(Image img, BufferedImageOp bop) {
		BufferedImage retImage = null;
		if (img == null)
			return retImage;
		try {
			GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice gd = genv.getDefaultScreenDevice();
			GraphicsConfiguration gc = gd.getDefaultConfiguration();
			ColorModel cm = gc.getColorModel();
			boolean hasAlpha = cm.hasAlpha();
			int cw = img.getWidth(null);
			int ch = img.getHeight(null);
			if (hasAlpha) {
				retImage = gc.createCompatibleImage(cw, ch);
			}
			else {
				retImage = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_ARGB);
			}
			if (retImage == null)
				return retImage;
			Graphics2D og = retImage.createGraphics();
			if (img instanceof BufferedImage) {
				og.drawImage((BufferedImage) img, bop, 0, 0);
			}
			else {
				og.drawImage(img, 0, 0, null);
			}
			og.dispose();
		}
		catch (Throwable t) {
		}
		return retImage;

	}

	/**
	 * Set coloration scheme of the DNA codons
	 * 
	 * @param colorSchemeByUsage
	 *            if true then codon will be colored by its usage in the genome
	 */

	public void setColorSchemeByUsage(boolean colorSchemeByUsage) {
		if (this.colorSchemeByUsage != colorSchemeByUsage) {
			this.colorSchemeByUsage = colorSchemeByUsage;
			if (scrollBar != null)
				scrollBar.setVisible(!colorSchemeByUsage);
			repaint();
		}
	}

	/**
	 * Returns the coloration scheme of the codons
	 * 
	 * @return colorSchemeByUsage
	 */
	public boolean getColorSchemeByUsage() {
		return colorSchemeByUsage;
	}

	public synchronized Color getBackboneColor() {
		return getBackboneColor(false);
	}

	public synchronized Color getBackboneColor(boolean inRNA) {
		if (!inRNA) {
			if (backboneColor == null)
				backboneColor = new Color(200, 170, 90);
			return backboneColor;
		}
		if (RNAbackboneColor == null)
			RNAbackboneColor = new Color(255, 0, 0);
		return RNAbackboneColor;

	}

}

class MagnifyGlassOp implements BufferedImageOp {

	BasicStroke stroke = new BasicStroke(4);

	public final static int GLASS_AS_CIRCLE = 0;
	public final static int GLASS_AS_RECTANGLE = 1;

	int drawMode = GLASS_AS_CIRCLE;

	float power = 1;
	float mx;
	float my;
	float rw = 0.5f;
	float rh = 0.5f;

	float red = 0;
	float green = 1;
	float blue = 0;
	float red2 = 0;
	float green2 = 0.7f;
	float blue2 = 0;

	Shape needClip = null;

	BufferedImage internalImage;
	boolean drawImage = false;

	MagnifyGlassOp(float power, float mx, float my, float rw, float rh, int drawMode) {
		this.power = (power < 0) ? 0 : power;
		this.mx = mx;
		this.my = my;
		this.rw = rw;
		this.rh = rh;
		this.drawMode = (drawMode < GLASS_AS_CIRCLE || GLASS_AS_CIRCLE > GLASS_AS_RECTANGLE) ? GLASS_AS_RECTANGLE
				: drawMode;
		setColorComponents(red, green, blue);

	}

	public synchronized BufferedImage filter(BufferedImage src, BufferedImage dest) {
		if (dest == null)
			dest = createCompatibleDestImage(src, null);
		float xc = mx;
		float yc = my;
		float r0 = rw - 2;
		float rrh = rh;
		Graphics2D g2d = dest.createGraphics();
		g2d.drawImage(src, null, 0, 0);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setStroke(stroke);
		g2d.setPaint(new Color(red, green, blue, 0.2f));
		Shape oldClip = g2d.getClip();
		g2d.setClip(needClip);
		if (drawImage && internalImage != null && (drawMode == GLASS_AS_RECTANGLE)) {
			Composite oldComp = g2d.getComposite();
			Color oldColor = g2d.getColor();
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
			g2d.drawImage(internalImage, null, Math.round(xc - r0), Math.round(yc - rrh - 1));
			g2d.setComposite(oldComp);
			g2d.setColor(oldColor);
		}
		else {
			if (drawMode == GLASS_AS_RECTANGLE) {
				g2d.fillRoundRect(Math.round(xc - r0), Math.round(yc - rrh - 1), Math.round(2 * r0), Math
						.round(2 * rrh), 2, 2);
			}
			else {
				g2d.fillOval(Math.round(xc - r0), Math.round(yc - r0), 2 * Math.round(r0), 2 * Math.round(r0));
			}
			g2d.setPaint(new Color(red2, green2, blue2, 0.5f));
			if (drawMode == GLASS_AS_RECTANGLE) {
				g2d.drawRoundRect(Math.round(xc - r0), Math.round(yc - rrh - 1), Math.round(2 * r0), Math
						.round(2 * rrh), 2, 2);
			}
			else {
				g2d.drawOval(Math.round(xc - r0), Math.round(yc - r0), 2 * Math.round(r0), 2 * Math.round(r0));
			}
		}
		g2d.setClip(oldClip);
		g2d.drawImage(dest, 0, 0, null);
		g2d.dispose();
		return dest;
	}

	public void setColorComponents() {
		green = blue = 0;
		red = 0.5f;
		green2 = blue2 = 0;
		red2 = 0.35f;
	}

	public void setColorComponents(float red, float green, float blue) {
		this.red = red;
		this.green = green;
		this.blue = blue;
		red2 = red * 0.7f;
		green2 = green * 0.7f;
		blue2 = blue * 0.7f;
	}

	public Rectangle2D getBounds2D(BufferedImage src) {
		return src.getRaster().getBounds();
	}

	public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
		if (destCM == null) {
			destCM = src.getColorModel();
			if (destCM instanceof IndexColorModel) {
				destCM = ColorModel.getRGBdefault();
			}
		}
		int w = src.getWidth();
		int h = src.getHeight();
		return new BufferedImage(destCM, destCM.createCompatibleWritableRaster(w, h), destCM.isAlphaPremultiplied(),
				null);
	}

	public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
		if (dstPt == null)
			dstPt = new Point2D.Float();
		dstPt.setLocation(srcPt);
		return dstPt;
	}

	public RenderingHints getRenderingHints() {
		return null;
	}

	static float d2(float x1, float y1, float x2, float y2) {
		return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
	}

	public void setX(float x) {
		mx = x;
	}

	public void setY(float y) {
		my = y;
	}

	public void setR(float r) {
		rw = r;
	}

	public float getX() {
		return mx;
	}

	public float getR() {
		return rw;
	}

	public float getH() {
		return rh;
	}

	public void setImage(BufferedImage img) {
		if (img == null) {
			internalImage = null;
			return;
		}
		BufferedImageOp bop = new MyAlphaOp();
		internalImage = DNAScroller.createImageFromImage(img, bop);
	}

	public void setDrawImage(boolean drawImage) {
		this.drawImage = drawImage;
	}

	public void setNeedClip(Shape needClip) {
		this.needClip = needClip;
	}

}

class MutationListenerHolder {
	MutationListener l;
	int strandIndex = -1;

	MutationListenerHolder(int strandIndex, MutationListener l) {
		this.l = l;
		this.strandIndex = strandIndex;
	}

	MutationListenerHolder(MutationListener l) {
		this(-1, l);
	}
}

class FlashThread extends Thread {

	DNAScroller owner;
	boolean needExit = false;
	int counter = 0;

	FlashThread(DNAScroller owner) {
		super();
		this.owner = owner;
		start();
	}

	synchronized void exit() {
		needExit = true;
	}

	synchronized boolean doExit() {
		return needExit;
	}

	public void run() {
		int max_count = owner.getNumberFlashes() * 2;
		if (owner == null)
			return;
		while (!doExit() && counter < max_count) {
			try {
				drawFlash(true);
				sleep(owner.getFlashIntervalMillisec());
			}
			catch (Throwable t) {
				exit();
			}
			counter++;
		}
		owner.clearFlashingState();
		drawFlash(false);
	}

	void drawFlash(final boolean doToggle) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (FlashThread.this.doExit())
					return;
				owner.drawForFlashing();
				if (doToggle)
					owner.toggleFlashState();
			}
		});
	}

}

class MyAlphaOp implements BufferedImageOp {

	MyAlphaOp() {
	}

	public synchronized BufferedImage filter(BufferedImage src, BufferedImage dest) {
		if (dest == null)
			dest = createCompatibleDestImage(src, null);
		int w = src.getWidth();
		int h = src.getHeight();
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				int px = src.getRGB(x, y);
				int dpx = px & 0xFFFFFF;
				int r = (px & 0xFF0000) >> 16;
				int g = (px & 0xFF00) >> 8;
				int b = (px & 0xFF);
				if (r > 0xF0 && g > 0xF0 && b > 0xF0) {
					px = dpx;
				}
				dest.setRGB(x, y, px);
			}
		}
		return dest;
	}

	public Rectangle2D getBounds2D(BufferedImage src) {
		return src.getRaster().getBounds();
	}

	public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
		if (destCM == null) {
			destCM = src.getColorModel();
			if (destCM instanceof IndexColorModel) {
				destCM = ColorModel.getRGBdefault();
			}
		}
		int w = src.getWidth();
		int h = src.getHeight();
		return new BufferedImage(destCM, destCM.createCompatibleWritableRaster(w, h), destCM.isAlphaPremultiplied(),
				null);
	}

	public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
		if (dstPt == null)
			dstPt = new Point2D.Float();
		dstPt.setLocation(srcPt);
		return dstPt;
	}

	public RenderingHints getRenderingHints() {
		return null;
	}

}

// A 204,204,255
// T 204,255,204 I think U should be the same the same
// G 255,204,204
// C 204,255,255
