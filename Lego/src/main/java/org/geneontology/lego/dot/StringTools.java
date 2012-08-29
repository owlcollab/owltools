package org.geneontology.lego.dot;

public class StringTools {

	static CharSequence insertLineBrakes(String s, final int lineLength, final CharSequence insert) {
		int lastInsert = 0;
		int pos = lineLength;
		StringBuilder sb = new StringBuilder();
		while (pos < s.length()) {
			int split = searchSplit(pos, s);
			sb.append(s.substring(lastInsert, split));
			sb.append(insert);
			lastInsert = split;
			pos += lineLength;
		}
		if (lastInsert < s.length()) {
			sb.append(s.substring(lastInsert));
		}
		return sb;
	}
	
	static int searchSplit(int pos, String s) {
		for (int i = pos; i < s.length(); i++) {
			char c = s.charAt(i);
			if (Character.isWhitespace(c) || '-' == c || '_' == c) {
				return i + 1;
			}
		}
		return s.length();
	}
}
