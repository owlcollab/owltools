package org.bbop.util;

import java.text.CollationElementIterator;
import java.text.Collator;
import java.text.RuleBasedCollator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.*;

public class StringUtil {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(StringUtil.class);

	public static boolean equals(String a, String b) {
		if (a.length() != b.length())
			return false;
		return unicodeIndexOf(a, b) == 0;
	}

	public static int unicodeIndexOf(String string, String findme) {
		return unicodeIndexOf(string, findme, 0);
	}
	
	public static int [] getWordIndicesSurrounding(String str, int startIndex,
			int endIndex) {
		for( ; startIndex >= 0; startIndex--) {
			if (Character.isWhitespace(str.charAt(startIndex)))
				break;
		}
		for( ; endIndex < str.length(); endIndex++)
			if (Character.isWhitespace(str.charAt(endIndex)))
				break;
		int [] out = {startIndex+1, endIndex};
		return out;
	}

	public static String getWordSurrounding(String str, int startIndex,
			int endIndex) {
		int [] indices = getWordIndicesSurrounding(str, startIndex, endIndex);
		return str.substring(indices[0], indices[1]);
	}

	public static int unicodeIndexOf(String string, String findme, int startPos) {
		RuleBasedCollator c = (RuleBasedCollator) Collator.getInstance();
		c.setDecomposition(Collator.NO_DECOMPOSITION);
		CollationElementIterator stringIterator = c
				.getCollationElementIterator(string.substring(startPos));
		CollationElementIterator findmeIterator = c
				.getCollationElementIterator(findme);
		while (true) {
			int startOffset = stringIterator.getOffset();
			findmeIterator.reset();
			int sc;
			int fc;
			while (true) {
				do {
					sc = stringIterator.next();
				} while (CollationElementIterator.primaryOrder(sc) == 0
						&& CollationElementIterator.secondaryOrder(sc) > 1);
				do {
					fc = findmeIterator.next();
				} while (CollationElementIterator.primaryOrder(fc) == 0
						&& CollationElementIterator.secondaryOrder(fc) > 1);
				if (fc == -1)
					return startOffset;
				if (sc == -1)
					return -1;
				if (CollationElementIterator.primaryOrder(fc) != CollationElementIterator
						.primaryOrder(sc)) {
					break;
				}
			}
			if (sc == -1)
				return -1;
			stringIterator.setOffset(startOffset + 1);
		}
	}

	/**
	 * The default line-break string used by the methods in this class.
	 */
	public static final String LINE_BREAK = System
			.getProperty("line.separator");

	public static int compareToIgnoreCase(String a, String b) {
		if (a == null) {
			if (b == null)
				return 0;
			else
				return 1;
		}
		if (b == null)
			return -1;

		int length = a.length();
		if (b.length() < length)
			length = b.length();
		for (int i = 0; i < length; i++) {
			char achar = Character.toUpperCase(a.charAt(i));
			char bchar = Character.toUpperCase(b.charAt(i));
			if (achar < bchar)
				return -1;
			else if (achar > bchar)
				return 1;
		}
		if (a.length() < b.length())
			return -1;
		else if (a.length() > b.length())
			return 1;
		else
			return 0;
	}

	public static String toTitleCase(String s) {
		StringBuffer out = new StringBuffer();
		char last = ' ';
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (Character.isWhitespace(last))
				out.append(Character.toTitleCase(c));
			else
				out.append(c);
			last = c;
		}
		return out.toString();
	}

	public static String wordToTitleCase(String s) {
		StringBuffer out = new StringBuffer();
		out.append(Character.toTitleCase(s.charAt(0)));
		out.append(s.substring(1, s.length()));
		return out.toString();
	}

	public static String pad(String s, char padChar, int size) {
		return pad(s, padChar, size, false);
	}

	public static String pad(String s, char padChar, int size, boolean padLeft) {
		StringBuffer out = new StringBuffer();
		if (!padLeft)
			out.append(s);
		for (int i = s.length(); i < size; i++) {
			out.append(padChar);
		}
		if (padLeft)
			out.append(s);
		return out.toString();
	}

	/**
	 * Hard-wraps flow-text. This is a convenience method that equivalent with
	 * <code>wrap(text, screenWidth, 0, 0, LINE_BREAK, true)</code>.
	 * 
	 * @see #wrap(String, int, int, int, String, boolean)
	 */
	public static String wrap(String text, int screenWidth) {
		return wrap(text, screenWidth, 0, 0, LINE_BREAK, false);
	}

	/**
	 * Hard-wraps flow-text. This is a convenience method that equivalent with
	 * <code>wrap(text, screenWidth, 0, 0, lineBreak, true)</code>.
	 * 
	 * @see #wrap(String, int, int, int, String, boolean)
	 */
	public static String wrap(String text, int screenWidth, String lineBreak) {
		return wrap(text, screenWidth, 0, 0, lineBreak, false);
	}

	/**
	 * Hard-wraps flow-text.
	 * 
	 * @param text
	 *            The flow-text to wrap. The explicit line-breaks of the source
	 *            text will be kept. All types of line-breaks (UN*X, Mac,
	 *            DOS/Win) are understood.
	 * @param screenWidth
	 *            The (minimum) width of the screen. It does not utilize the
	 *            <code>screenWidth</code>-th column of the screen to store
	 *            characters, except line-breaks (because some terminals/editors
	 *            do an automatic line-break when you write visible character
	 *            there, and some doesn't... so it is unpredicalbe if an
	 *            explicit line-break is needed or not.).
	 * @param firstIndent
	 *            The indentation of the first line
	 * @param indent
	 *            The indentation of all lines but the first line
	 * @param lineBreak
	 *            The String used for line-breaks
	 * @param traceMode
	 *            Set this true if the input text is a Java stack trace. In this
	 *            mode, all lines starting with optional indentation +
	 *            <tt>'at'</tt> + space are treated as location lines, and
	 *            will be indented and wrapped in a silghtly special way.
	 * @throws IllegalArgumentException
	 *             if the number of columns remaining for the text is less than
	 *             2.
	 */
	public static String wrap(String text, int screenWidth, int firstIndent,
			int indent, String lineBreak, boolean traceMode) {
		return wrap(new StringBuffer(text), screenWidth, firstIndent, indent,
				lineBreak, traceMode).toString();
	}

	/**
	 * Hard-wraps flow-text. Uses StringBuffer-s instead of String-s. This is
	 * the method that is internally used by all other <code>wrap</code>
	 * variations, so if you are working with StringBuffers anyway, it gives
	 * better performance.
	 * 
	 * @see #wrap(String, int, int, int, String, boolean)
	 */
	public static StringBuffer wrap(StringBuffer text, int screenWidth,
			int firstIndent, int indent, String lineBreak, boolean traceMode) {

		if (firstIndent < 0 || indent < 0 || screenWidth < 0) {
			throw new IllegalArgumentException("Negative dimension");
		}

		int allowedCols = screenWidth - 1;

		if ((allowedCols - indent) < 2 || (allowedCols - firstIndent) < 2) {
			throw new IllegalArgumentException("Usable columns < 2");
		}

		int ln = text.length();
		int defaultNextLeft = allowedCols - indent;
		int b = 0;
		int e = 0;

		StringBuffer res = new StringBuffer((int) (ln * 1.2));
		int left = allowedCols - firstIndent;
		for (int i = 0; i < firstIndent; i++) {
			res.append(' ');
		}
		StringBuffer tempb = new StringBuffer(indent + 2);
		tempb.append(lineBreak);
		for (int i = 0; i < indent; i++) {
			tempb.append(' ');
		}
		String defaultBreakAndIndent = tempb.toString();

		boolean firstSectOfSrcLine = true;
		boolean firstWordOfSrcLine = true;
		int traceLineState = 0;
		int nextLeft = defaultNextLeft;
		String breakAndIndent = defaultBreakAndIndent;
		int wln = 0, x;
		char c, c2;
		all: do {
			word: while (e <= ln) {
				if (e != ln) {
					c = text.charAt(e);
				} else {
					c = ' ';
				}
				if (traceLineState > 0 && e > b) {
					if (c == '.' && traceLineState == 1) {
						c = ' ';
					} else {
						c2 = text.charAt(e - 1);
						if (c2 == ':') {
							c = ' ';
						} else if (c2 == '(') {
							traceLineState = 2;
							c = ' ';
						}
					}
				}
				if (c != ' ' && c != '\n' && c != '\r' && c != '\t') {
					e++;
				} else {
					wln = e - b;
					if (left >= wln) {
						res.append(text.substring(b, e));
						left -= wln;
						b = e;
					} else {
						wln = e - b;
						if (wln > nextLeft || firstWordOfSrcLine) {
							int ob = b;
							while (wln > left) {
								if (left > 2
										|| (left == 2 && (firstWordOfSrcLine || !(b == ob && nextLeft > 2)))) {
									res.append(text.substring(b, b + left - 1));
									res.append("-");
									res.append(breakAndIndent);
									wln -= left - 1;
									b += left - 1;
									left = nextLeft;
								} else {
									x = res.length() - 1;
									if (x >= 0 && res.charAt(x) == ' ') {
										res.delete(x, x + 1);
									}
									res.append(breakAndIndent);
									left = nextLeft;
								}
							}
							res.append(text.substring(b, b + wln));
							b += wln;
							left -= wln;
						} else {
							x = res.length() - 1;
							if (x >= 0 && res.charAt(x) == ' ') {
								res.delete(x, x + 1);
							}
							res.append(breakAndIndent);
							res.append(text.substring(b, e));
							left = nextLeft - wln;
							b = e;
						}
					}
					firstSectOfSrcLine = false;
					firstWordOfSrcLine = false;
					break word;
				}
			}
			int extra = 0;
			space: while (e < ln) {
				c = text.charAt(e);
				if (c == ' ') {
					e++;
				} else if (c == '\t') {
					e++;
					extra += 7;
				} else if (c == '\n' || c == '\r') {
					nextLeft = defaultNextLeft;
					breakAndIndent = defaultBreakAndIndent;
					res.append(breakAndIndent);
					e++;
					if (e < ln) {
						c2 = text.charAt(e);
						if ((c2 == '\n' || c2 == '\r') && c != c2) {
							e++;
						}
					}
					left = nextLeft;
					b = e;
					firstSectOfSrcLine = true;
					firstWordOfSrcLine = true;
					traceLineState = 0;
				} else {
					wln = e - b + extra;
					if (firstSectOfSrcLine) {
						int y = allowedCols - indent - wln;
						if (traceMode && ln > e + 2 && text.charAt(e) == 'a'
								&& text.charAt(e + 1) == 't'
								&& text.charAt(e + 2) == ' ') {
							if (y > 5 + 3) {
								y -= 3;
							}
							traceLineState = 1;
						}
						if (y > 5) {
							y = allowedCols - y;
							nextLeft = allowedCols - y;
							tempb = new StringBuffer(indent + 2);
							tempb.append(lineBreak);
							for (int i = 0; i < y; i++) {
								tempb.append(' ');
							}
							breakAndIndent = tempb.toString();
						}
					}
					if (wln <= left) {
						res.append(text.substring(b, e));
						left -= wln;
						b = e;
					} else {
						res.append(breakAndIndent);
						left = nextLeft;
						b = e;
					}
					firstSectOfSrcLine = false;
					break space;
				}
			}
		} while (e < ln);

		return res;
	}

	public static String createRandomString(int maxLength) {
		int length = (int) (Math.random() * maxLength) + 1;
		StringBuffer out = new StringBuffer();
		for (int i = 0; i < length; i++) {
			char c = (char) (97 + (int) (Math.random() * 26d));
			out.append(c);
		}
		return out.toString();
	}

	public static String repeat(String s, int n) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < n; i++) {
			sb.append(s);
		}
		return sb.toString();
	}

	public static String repeat(char c, int n) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < n; i++) {
			sb.append(c);
		}
		return sb.toString();
	}

	public static Map<String, int[]> getMatchMap(String str, List<String> vals,
			boolean matchAll, boolean ignoreCase) {
		if (ignoreCase)
			str = str.toLowerCase();
		Map<String, int[]> map = new HashMap<String, int[]>();
		LinkedList<Integer> temp = new LinkedList<Integer>();
		for (String val : vals) {
			if (ignoreCase)
				val = val.toLowerCase();
			int index = -str.length();
			while ((index = str.indexOf(val, index + str.length())) != -1) {
				temp.add(index);
			}
			if (temp.size() == 0)
				if (matchAll) {
					map.clear();
					return map;
				} else
					continue;
			int[] ints = new int[temp.size()];
			for (int i = 0; i < ints.length; i++) {
				ints[i] = temp.removeFirst();
			}
			map.put(val, ints);
		}
		return map;
	}

	public static double score(String str, List<String> tokens,
			Map<String, int[]> hits) {
		return score(str, tokens, hits, 1, 2, 5, 4, 10);
	}

	public static double score(String str, List<String> tokens,
			Map<String, int[]> hits, double basicMatchWeight,
			double startMatchMultiplier, double fullMatchMultiplier,
			double inOrderMultiplier, double matchesAllMultiplier) {
		double score = 0;
		boolean matchesAll = true;
		int hitIndex = 0;
		int[] firstIndices = new int[hits.size()];
		for (int tokenIndex = 0; tokenIndex < tokens.size(); tokenIndex++) {
			String s = tokens.get(tokenIndex);
			int[] hitIndices = hits.get(s);
			if (hitIndices == null) {
				matchesAll = false;
				continue;
			}
			for (int i = 0; i < hitIndices.length; i++) {
				int charIndex = hitIndices[i];
				if (i == 0) {
					firstIndices[hitIndex++] = charIndex;
				}
				score += getScore(str, s, charIndex, basicMatchWeight,
						startMatchMultiplier, fullMatchMultiplier);

			}
		}
		boolean inOrder = true;
		if (firstIndices.length > 0) {
			int lastVal = firstIndices[0];
			for (int i = 1; i < firstIndices.length; i++) {
				if (firstIndices[i] < lastVal) {
					inOrder = false;
					break;
				}
				lastVal = firstIndices[i];
			}
		}

		if (inOrder)
			score *= inOrderMultiplier;
		if (matchesAll)
			score *= matchesAllMultiplier;
		return score;
	}

	protected static double getScore(String str, String token, int charIndex,
			double basicMatchWeight, double startMatchMultiplier,
			double fullMatchMultiplier) {
		double score = basicMatchWeight;
		if (charIndex == 0 || Character.isWhitespace(str.charAt(charIndex - 1))) {
			if (str.substring(charIndex, charIndex + token.length()).equals(
					token)) {
				score *= fullMatchMultiplier;
			} else
				score *= startMatchMultiplier;
		}
		return score;
	}

	// In OE1, % was not on the "valid" list.  Now it is.  Why?
	protected static Pattern p = Pattern
			.compile("[A-Za-z0-9%_\\-!.~\\\\'\\(\\)\\*,;#:\\$&\\+=\\?/\\[\\]@]*");

	public static boolean isValidURICharacter(char c) {
		return p.matcher(c + "").matches();
	}
	
	public static boolean containsOnlyValidURICharacters(String s) {
		return p.matcher(s).matches();
	}

	public static boolean requals(String a, String b) {
		if (a == b) {
			return true;
		}
		if (a.length() != b.length())
			return false;
		int n = a.length();
		if (n == b.length()) {
			for (int i = n - 1; i >= 0; i--) {
				if (a.charAt(i) != b.charAt(i)) {
					return false;
				}
			}
		}
		return true;
	}

	public static String replace(String string, String original,
			String replacement) {
		int index = 0;
		while ((index = string.indexOf(original, index)) != -1) {
			string = string.substring(0, index) + replacement
					+ string.substring(index + original.length());
			index += replacement.length();
		}
		return string;
	}

	public static final String escapeHTML(String s) {
		StringBuffer sb = new StringBuffer();
		int n = s.length();
		for (int i = 0; i < n; i++) {
			char c = s.charAt(i);
			switch (c) {
			case '<':
				sb.append("&lt;");
				break;
			case '>':
				sb.append("&gt;");
				break;
			case '&':
				sb.append("&amp;");
				break;
			case '"':
				sb.append("&quot;");
				break;
			// be carefull with this one (non-breaking whitee space)
			case ' ':
				sb.append("&nbsp;");
				break;

			default:
				sb.append(c);
				break;
			}
		}
		return sb.toString();
	}

	public static String join(String c, String[] tokens, int p) {
		StringBuffer s = new StringBuffer();
		while (p < tokens.length) {
			s.append(tokens[p]);
			p++;
			if (p < tokens.length)
				s.append(c);
		}
		return s.toString();
	}
}
