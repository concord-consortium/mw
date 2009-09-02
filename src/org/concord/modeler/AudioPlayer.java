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

package org.concord.modeler;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.concord.modeler.text.Page;
import org.concord.modeler.text.XMLCharacterEncoder;
import org.concord.modeler.util.FileUtilities;

public class AudioPlayer extends JPanel implements Embeddable, MetaEventListener, LineListener {

	public static FileFilter fileFilter = new FileFilter() {
		public boolean accept(File file) {
			if (file == null)
				return false;
			String s = file.toString().toLowerCase();
			if (s.endsWith(".mid") || s.endsWith(".wav"))
				return true;
			return false;
		}
	};

	private static Icon MUTE_ICON, NOT_MUTE_ICON;

	Page page;
	String clipName;
	private int index;
	private String uid;
	private boolean showVolumeControl;
	private boolean marked;
	private String borderType;
	private boolean isMidi;
	private Color playerBackground;
	private File currentFile;
	private Color defaultPlayerBackground, defaultPlayerForeground;
	private JLabel textLabel;
	private AbstractButton muteButton, playButton, pauseButton, stopButton;
	private JSlider volumeSlider;
	private MidiPlayer midiPlayer;
	private SampledAudioPlayer sampledAudioPlayer;
	private JPopupMenu popupMenu;
	private static AudioPlayerMaker maker;
	private MouseListener popupMouseListener;

	public AudioPlayer() {
		super();
		init();
	}

	public AudioPlayer(AudioPlayer player, Page parent) {
		this();
		setPage(parent);
		setBorderType(player.getBorderType());
		setPreferredSize(player.getPreferredSize());
		setClipName(player.clipName);
		setUid(player.uid);
		setText(player.getText());
		setToolTipText(player.getToolTipText());
		setBackground(player.getBackground());
		setChangable(page.isEditable());
	}

	public void setClipName(String s) {
		if (s == null)
			throw new IllegalArgumentException("null input");
		clipName = s;
		isMidi = clipName.toLowerCase().endsWith(".mid");
		showVolumeControl = !isMidi;
		toggleVolumeControl();
		File f = null;
		if (page.isRemote()) {
			URL u = null;
			try {
				u = new URL(FileUtilities.getCodeBase(page.getAddress()) + clipName);
			}
			catch (MalformedURLException e) {
				e.printStackTrace();
				return;
			}
			try {
				f = ConnectionManager.sharedInstance().shouldUpdate(u);
				if (f == null)
					f = ConnectionManager.sharedInstance().cache(u);
			}
			catch (IOException iox) {
				iox.printStackTrace();
				return;
			}
		}
		else {
			f = new File(FileUtilities.getCodeBase(page.getAddress()), clipName);
		}
		currentFile = f;
	}

	public String getClipName() {
		return clipName;
	}

	private void playSound() {
		if (isMidi) {
			if (midiPlayer == null) {
				midiPlayer = new MidiPlayer();
				midiPlayer.addMetaEventListener(this);
			}
			midiPlayer.play(currentFile);
		}
		else {
			if (sampledAudioPlayer == null) {
				sampledAudioPlayer = new SampledAudioPlayer();
				sampledAudioPlayer.addLineListener(this);
			}
			sampledAudioPlayer.play(currentFile);
		}
	}

	private void stopSound() {
		if (isMidi) {
			if (midiPlayer != null)
				midiPlayer.stop();
		}
		else {
			if (sampledAudioPlayer != null)
				sampledAudioPlayer.stop();
		}
	}

	private void pauseSound() {
		if (isMidi) {
			if (midiPlayer != null)
				midiPlayer.pause();
		}
		else {
			if (sampledAudioPlayer != null)
				sampledAudioPlayer.pause();
		}
	}

	private void muteSound(boolean on) {
		if (isMidi) {
			if (midiPlayer != null)
				midiPlayer.mute(on);
		}
		else {
			if (sampledAudioPlayer != null)
				sampledAudioPlayer.mute(on);
		}
	}

	private void changeVolume(int i) {
		if (isMidi) {
			if (midiPlayer != null)
				midiPlayer.changeVolume(i);
		}
		else {
			if (sampledAudioPlayer != null)
				sampledAudioPlayer.changeVolume(i);
		}
	}

	void requestStop() {
		if (isMidi) {
			if (midiPlayer != null)
				midiPlayer.requestStop();
		}
		else {
			if (sampledAudioPlayer != null)
				sampledAudioPlayer.requestStop();
		}
	}

	public void destroy() {
		page = null;
		if (midiPlayer != null) {
			midiPlayer.removeMetaEventListener(this);
			midiPlayer.destroy();
			midiPlayer = null;
		}
		if (sampledAudioPlayer != null) {
			sampledAudioPlayer.removeLineListener(this);
			sampledAudioPlayer.destroy();
			sampledAudioPlayer = null;
		}
		if (maker != null)
			maker.setObject(null);
	}

	private void toggleVolumeControl() {
		if (showVolumeControl) {
			if (volumeSlider == null) {
				volumeSlider = new JSlider(1, 10, 10);
				volumeSlider.addChangeListener(new ChangeListener() {
					public void stateChanged(ChangeEvent e) {
						changeVolume(volumeSlider.getValue());
					}
				});
				volumeSlider.setPreferredSize(new Dimension(50, 24));
			}
			add(volumeSlider, 1);
			add(muteButton, 2);
			validate();
		}
		else {
			if (volumeSlider != null) {
				remove(volumeSlider);
				remove(muteButton);
				validate();
			}
		}
	}

	public void meta(MetaMessage meta) {
		if (meta.getType() == 47) { // Sequencer is done playing
			setButtonState(false);
			stopSound();
		}
	}

	public void update(LineEvent event) {
		if (event.getType() == LineEvent.Type.STOP) {
			setButtonState(false);
			if (sampledAudioPlayer != null) {
				sampledAudioPlayer.closeClip();
			}
		}
	}

	private void loadIcons() {
		if (MUTE_ICON != null)
			return;
		MUTE_ICON = new ImageIcon(AudioPlayer.class.getResource("images/audio_mute.gif"));
		NOT_MUTE_ICON = new ImageIcon(AudioPlayer.class.getResource("images/audio_notmute.gif"));
	}

	private void init() {

		loadIcons();

		setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		textLabel = new JLabel("Instruction here");
		textLabel.setIcon(new ImageIcon(getClass().getResource("images/sound.gif")));
		add(textLabel);

		Dimension dim = Modeler.isMac() ? new Dimension(28, 28) : new Dimension(24, 24);

		String s = Modeler.getInternationalText("MuteAudioClip");
		muteButton = new JToggleButton(NOT_MUTE_ICON);
		muteButton.setToolTipText(s != null ? s : "Mute");
		muteButton.setPreferredSize(dim);
		muteButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (muteButton.isSelected()) {
					try {
						muteSound(true);
					}
					catch (Throwable t) {
						t.printStackTrace();
					}
					muteButton.setIcon(MUTE_ICON);
				}
				else {
					try {
						muteSound(false);
					}
					catch (Throwable t) {
						t.printStackTrace();
					}
					muteButton.setIcon(NOT_MUTE_ICON);
				}
			}
		});
		// add(muteButton);

		s = Modeler.getInternationalText("PlayAudioClip");
		playButton = new JButton(new ImageIcon(getClass().getResource("images/audio_play.gif")));
		playButton.setToolTipText(s != null ? s : "Play");
		playButton.setPreferredSize(dim);
		playButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					playSound();
				}
				catch (Throwable t) {
					t.printStackTrace();
				}
				setButtonState(true);
			}
		});
		add(playButton);

		s = Modeler.getInternationalText("PauseAudioClip");
		pauseButton = new JButton(new ImageIcon(getClass().getResource("images/audio_pause.gif")));
		pauseButton.setToolTipText(s != null ? s : "Pause");
		pauseButton.setPreferredSize(dim);
		pauseButton.setEnabled(false);
		pauseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					pauseSound();
				}
				catch (Throwable t) {
					t.printStackTrace();
				}
				setButtonState(false);
			}
		});
		add(pauseButton);

		s = Modeler.getInternationalText("StopAudioClip");
		stopButton = new JButton(new ImageIcon(getClass().getResource("images/audio_stop.gif")));
		stopButton.setToolTipText(s != null ? s : "Stop");
		stopButton.setPreferredSize(dim);
		stopButton.setEnabled(false);
		stopButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					stopSound();
				}
				catch (Throwable t) {
					t.printStackTrace();
				}
				setButtonState(false);
				muteButton.setSelected(false);
				muteButton.setIcon(NOT_MUTE_ICON);
			}
		});
		add(stopButton);

		if (defaultPlayerBackground == null)
			defaultPlayerBackground = getBackground();
		if (defaultPlayerForeground == null)
			defaultPlayerForeground = getForeground();
		popupMouseListener = new PopupMouseListener(this);
		addMouseListener(popupMouseListener);

	}

	private void setButtonState(final boolean playing) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				playButton.setEnabled(!playing);
				stopButton.setEnabled(playing);
				pauseButton.setEnabled(playing);
			}
		});
	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	/** side effect of implementing Embeddable */
	public void createPopupMenu() {

		popupMenu = new JPopupMenu();
		popupMenu.setInvoker(this);

		String s = Modeler.getInternationalText("CustomizeAudioPlayer");
		JMenuItem mi = new JMenuItem((s != null ? s : "Customize This Audio Player") + "...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null) {
					maker = new AudioPlayerMaker(AudioPlayer.this);
				}
				else {
					maker.setObject(AudioPlayer.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(mi);

		s = Modeler.getInternationalText("RemoveAudioPlayer");
		mi = new JMenuItem(s != null ? s : "Remove This Audio Player");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(AudioPlayer.this);
			}
		});
		popupMenu.add(mi);

		s = Modeler.getInternationalText("CopyAudioPlayer");
		mi = new JMenuItem(s != null ? s : "Copy This Audio Player");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(AudioPlayer.this);
			}
		});
		popupMenu.add(mi);

		popupMenu.pack();

	}

	public void setIndex(int i) {
		index = i;
	}

	public int getIndex() {
		return index;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getUid() {
		return uid;
	}

	public void setMarked(boolean b) {
		marked = b;
		if (page == null)
			return;
		if (b)
			playerBackground = getBackground();
		setBackground(b ? page.getSelectionColor() : playerBackground);
		setForeground(b ? page.getSelectedTextColor() : defaultPlayerForeground);
	}

	public boolean isMarked() {
		return marked;
	}

	public String getBorderType() {
		if (borderType == null)
			return BorderManager.getBorder(this);
		return borderType;
	}

	public void setBorderType(String s) {
		borderType = s;
		BorderManager.setBorder(this, s, page.getBackground());
	}

	public void setPage(Page p) {
		page = p;
	}

	public Page getPage() {
		return page;
	}

	public void setChangable(boolean b) {
		if (b) {
			if (!isChangable())
				addMouseListener(popupMouseListener);
		}
		else {
			if (isChangable())
				removeMouseListener(popupMouseListener);
		}
	}

	public boolean isChangable() {
		MouseListener[] ml = getMouseListeners();
		for (MouseListener l : ml) {
			if (l == popupMouseListener)
				return true;
		}
		return false;
	}

	public static AudioPlayer create(Page page) {
		if (page == null)
			return null;
		AudioPlayer player = new AudioPlayer();
		if (maker == null) {
			maker = new AudioPlayerMaker(player);
		}
		else {
			maker.setObject(player);
		}
		maker.invoke(page);
		if (maker.cancel)
			return null;
		return player;
	}

	public void setText(String text) {
		textLabel.setText(text);
		Dimension dim1 = playButton.getPreferredSize();
		Dimension dim2 = textLabel.getPreferredSize();
		Dimension d;
		if (showVolumeControl) {
			Dimension dim3 = volumeSlider.getPreferredSize();
			d = new Dimension(4 * dim1.width + dim2.width + dim3.width + 40, dim2.height + 22);
		}
		else {
			d = new Dimension(3 * dim1.width + dim2.width + 40, dim2.height + 22);
		}
		setPreferredSize(d);
		setMinimumSize(d);
		setMaximumSize(d);
	}

	public String getText() {
		return textLabel.getText();
	}

	public void setBackground(Color c) {
		super.setBackground(c);
		if (textLabel != null) {
			textLabel.setOpaque(true);
			textLabel.setBackground(c);
			muteButton.setBackground(c);
			pauseButton.setBackground(c);
			playButton.setBackground(c);
			stopButton.setBackground(c);
		}
		if (volumeSlider != null) {
			volumeSlider.setOpaque(true);
			volumeSlider.setBackground(c);
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("<class>" + getClass().getName() + "</class>\n");
		if (uid != null)
			sb.append("<uid>" + uid + "</uid>\n");
		sb.append("<title>" + XMLCharacterEncoder.encode(getText()) + "</title>\n");
		if (getToolTipText() != null) {
			sb.append("<tooltip>" + XMLCharacterEncoder.encode(getToolTipText()) + "</tooltip>\n");
		}
		sb.append("<description>" + XMLCharacterEncoder.encode(clipName) + "</description>\n");
		if (!getBackground().equals(defaultPlayerBackground)) {
			sb.append("<bgcolor>" + Integer.toString(getBackground().getRGB(), 16) + "</bgcolor>\n");
		}
		if (borderType != null) {
			sb.append("<border>" + borderType + "</border>");
		}
		return sb.toString();
	}

}