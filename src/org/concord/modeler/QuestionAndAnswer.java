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

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * This is used to store question and answer for
 * </p>
 * <ul>
 * <li><code>PageTextField</code></li>
 * <li><code>PageTextArea</code></li>
 * <li><code>PageMultipleChoice</code></li>
 * <li><code>ImageQuestion</code></li>
 * </ul>
 * <p>
 * For a multiple choice question, the question includes the choices, and the answer text is the indices of answers
 * separated by a space character. For an image question, the answer is an address that can be used to reference the
 * image.
 * <p>
 * <p>
 * A reference answer can be provided for assessment.
 * </p>
 */

public class QuestionAndAnswer {

	public static String NO_ANSWER = "NOT ANSWERED.";

	private String question;
	private String answer;
	private String referenceAnswer;
	private long timestamp = -1;
	private List<String> guessList;

	public QuestionAndAnswer(String question, String answer) {
		if (question == null || question.trim().equals(""))
			throw new IllegalArgumentException("Question cannot be empty");
		setQuestion(question);
		setAnswer(answer);
	}

	public QuestionAndAnswer(String question, String answer, String referenceAnswer) {
		this(question, answer);
		this.referenceAnswer = referenceAnswer;
	}

	public QuestionAndAnswer(QuestionAndAnswer q) {
		question = q.question;
		answer = q.answer;
		referenceAnswer = q.referenceAnswer;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setReferenceAnswer(String referenceAnswer) {
		this.referenceAnswer = referenceAnswer;
	}

	public String getReferenceAnswer() {
		return referenceAnswer;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public String getQuestion() {
		return question;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

	public String getAnswer() {
		return answer;
	}

	public void addGuess(String s) {
		if (guessList == null)
			guessList = new ArrayList<String>();
		guessList.add(s);
	}

	public List<String> getGuesses() {
		return guessList;
	}

	public String toString() {
		if (referenceAnswer != null)
			return "[" + question + " --- " + answer + " : " + referenceAnswer + "]";
		return "[" + question + " --- " + answer + "]";
	}

}