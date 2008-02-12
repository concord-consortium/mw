/*
 *   Copyright (C) 2008  The Concord Consortium, Inc.,
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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

/**
 * @author Charles Xie
 * 
 */
abstract class DNAScrollerWithRNAAction extends AbstractAction {
	DNAScrollerWithRNA owner;

	public DNAScrollerWithRNAAction() {
		initProperties();
	}

	public DNAScrollerWithRNAAction(DNAScrollerWithRNA owner) {
		this();
		this.owner = owner;
	}

	public abstract void initProperties();

}

class ResetDNAScrollerWithRNAAction extends DNAScrollerWithRNAAction {

	ResetDNAScrollerWithRNAAction() {
		super();
	}

	public ResetDNAScrollerWithRNAAction(DNAScrollerWithRNA owner) {
		super(owner);
	}

	public void actionPerformed(ActionEvent evt) {
		if (owner == null)
			return;
		owner.reset();
	}

	public void initProperties() {
		putValue(Action.NAME, "Reset");
		putValue(Action.SHORT_DESCRIPTION, "Reset Scroller");
		putValue(Action.ACTION_COMMAND_KEY, "Reset Scroller");
	}

}

class BeginTranscriptionDNAScrollerWithRNAAction extends DNAScrollerWithRNAAction {

	BeginTranscriptionDNAScrollerWithRNAAction() {
		super();
	}

	public BeginTranscriptionDNAScrollerWithRNAAction(DNAScrollerWithRNA owner) {
		super(owner);
	}

	public void actionPerformed(ActionEvent evt) {
		if (owner == null)
			return;
		owner.reset();
	}

	public void initProperties() {
		putValue(Action.NAME, "Begin Transcription");
		putValue(Action.SHORT_DESCRIPTION, "Begin Transcription Scroller");
		putValue(Action.ACTION_COMMAND_KEY, "Begin Transcription Scroller");
	}
}

class TranscriptionDNAScrollerWithRNAAction extends DNAScrollerWithRNAAction {

	TranscriptionDNAScrollerWithRNAAction() {
		super();
	}

	public TranscriptionDNAScrollerWithRNAAction(DNAScrollerWithRNA owner) {
		super(owner);
	}

	public void actionPerformed(ActionEvent evt) {
		if (owner == null)
			return;
		owner.resetToStartTranscription();
	}

	public void initProperties() {
		putValue(Action.NAME, "Transcription");
		putValue(Action.SHORT_DESCRIPTION, "Transcription Scroller");
		putValue(Action.ACTION_COMMAND_KEY, "Transcription Scroller");
	}
}

class StartTranscriptionDNAScrollerWithRNAAction extends DNAScrollerWithRNAAction {

	StartTranscriptionDNAScrollerWithRNAAction() {
		super();
	}

	public StartTranscriptionDNAScrollerWithRNAAction(DNAScrollerWithRNA owner) {
		super(owner);
	}

	public void actionPerformed(ActionEvent evt) {
		if (owner == null)
			return;
		owner.resetToStartTranscription();
		owner.startTranscription();
	}

	public void initProperties() {
		putValue(Action.NAME, "Start Transcription");
		putValue(Action.SHORT_DESCRIPTION, "Start Transcription Scroller");
		putValue(Action.ACTION_COMMAND_KEY, "Start Transcription Scroller");
	}
}

class StartTranslationDNAScrollerWithRNAAction extends DNAScrollerWithRNAAction {

	StartTranslationDNAScrollerWithRNAAction() {
		super();
	}

	public StartTranslationDNAScrollerWithRNAAction(DNAScrollerWithRNA owner) {
		super(owner);
	}

	public void actionPerformed(ActionEvent evt) {
		if (owner == null)
			return;
		owner.resetToStartTranslation();
		owner.startTranslation();
	}

	public void initProperties() {
		putValue(Action.NAME, "Start Translation");
		putValue(Action.SHORT_DESCRIPTION, "Start Translation Scroller");
		putValue(Action.ACTION_COMMAND_KEY, "Start Translation Scroller");
	}
}

class TransclationDNAScrollerWithRNAAction extends DNAScrollerWithRNAAction {

	TransclationDNAScrollerWithRNAAction() {
		super();
	}

	public TransclationDNAScrollerWithRNAAction(DNAScrollerWithRNA owner) {
		super(owner);
	}

	public void actionPerformed(ActionEvent evt) {
		if (owner == null)
			return;
		owner.resetToStartTranslation();
	}

	public void initProperties() {
		putValue(Action.NAME, "Translation");
		putValue(Action.SHORT_DESCRIPTION, "Translation Scroller");
		putValue(Action.ACTION_COMMAND_KEY, "Translation Scroller");
	}

}
