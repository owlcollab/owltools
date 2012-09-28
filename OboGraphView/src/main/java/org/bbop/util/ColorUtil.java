package org.bbop.util;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.*;

public class ColorUtil {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(ColorUtil.class);

	public static final Map<String, Color> FANCY_HTML_COLOR_MAP = new LinkedHashMap<String, Color>();
	public static final Map<String, Color> HTML_COLOR_MAP = new LinkedHashMap<String, Color>();
	public static final Map<String, Color> JAVA_COLOR_MAP = new LinkedHashMap<String, Color>();

	static {
		HTML_COLOR_MAP.put("black", getColor("#000000"));
		HTML_COLOR_MAP.put("silver", getColor("#C0C0C0"));
		HTML_COLOR_MAP.put("gray", getColor("#808080"));
		HTML_COLOR_MAP.put("white", getColor("#FFFFFF"));
		HTML_COLOR_MAP.put("maroon", getColor("#800000"));
		HTML_COLOR_MAP.put("red", getColor("#FF0000"));
		HTML_COLOR_MAP.put("purple", getColor("#800080"));
		HTML_COLOR_MAP.put("fuchsia", getColor("#FF00FF"));
		HTML_COLOR_MAP.put("green", getColor("#008000"));
		HTML_COLOR_MAP.put("lime", getColor("#00FF00"));
		HTML_COLOR_MAP.put("olive", getColor("#808000"));
		HTML_COLOR_MAP.put("yellow", getColor("#FFFF00"));
		HTML_COLOR_MAP.put("navy", getColor("#000080"));
		HTML_COLOR_MAP.put("blue", getColor("#0000FF"));
		HTML_COLOR_MAP.put("teal", getColor("#008080"));
		HTML_COLOR_MAP.put("aqua", getColor("#00FFFF"));

		FANCY_HTML_COLOR_MAP.put("Alice Blue", getColor("#F0F8FF"));
		FANCY_HTML_COLOR_MAP.put("Antique White", getColor("#FAEBD7"));
		FANCY_HTML_COLOR_MAP.put("Aqua", getColor("#00FFFF"));
		FANCY_HTML_COLOR_MAP.put("Aquamarine", getColor("#7FFFD4"));
		FANCY_HTML_COLOR_MAP.put("Azure", getColor("#F0FFFF"));
		FANCY_HTML_COLOR_MAP.put("Beige", getColor("#F5F5DC"));
		FANCY_HTML_COLOR_MAP.put("Bisque", getColor("#FFE4C4"));
		FANCY_HTML_COLOR_MAP.put("Black", getColor("#000000"));
		FANCY_HTML_COLOR_MAP.put("Blanched Almond", getColor("#FFEBCD"));
		FANCY_HTML_COLOR_MAP.put("Blue", getColor("#0000FF"));
		FANCY_HTML_COLOR_MAP.put("BlueViolet", getColor("#8A2BE2"));
		FANCY_HTML_COLOR_MAP.put("Brown", getColor("#A52A2A"));
		FANCY_HTML_COLOR_MAP.put("Burly Wood", getColor("#DEB887"));
		FANCY_HTML_COLOR_MAP.put("Cadet Blue", getColor("#5F9EA0"));
		FANCY_HTML_COLOR_MAP.put("Chartreuse", getColor("#7FFF00"));
		FANCY_HTML_COLOR_MAP.put("Chocolate", getColor("#D2691E"));
		FANCY_HTML_COLOR_MAP.put("Coral", getColor("#FF7F50"));
		FANCY_HTML_COLOR_MAP.put("Cornflower Blue", getColor("#6495ED"));
		FANCY_HTML_COLOR_MAP.put("Cornsilk", getColor("#FFF8DC"));
		FANCY_HTML_COLOR_MAP.put("Crimson", getColor("#DC143C"));
		FANCY_HTML_COLOR_MAP.put("Cyan", getColor("#00FFFF"));
		FANCY_HTML_COLOR_MAP.put("Dark Blue", getColor("#00008B"));
		FANCY_HTML_COLOR_MAP.put("Dark Cyan", getColor("#008B8B"));
		FANCY_HTML_COLOR_MAP.put("Dark GoldenRod", getColor("#B8860B"));
		FANCY_HTML_COLOR_MAP.put("Dark Gray", getColor("#A9A9A9"));
		FANCY_HTML_COLOR_MAP.put("Dark Green", getColor("#006400"));
		FANCY_HTML_COLOR_MAP.put("Dark Khaki", getColor("#BDB76B"));
		FANCY_HTML_COLOR_MAP.put("Dark Magenta", getColor("#8B008B"));
		FANCY_HTML_COLOR_MAP.put("Dark Olive Green", getColor("#556B2F"));
		FANCY_HTML_COLOR_MAP.put("Dark orange", getColor("#FF8C00"));
		FANCY_HTML_COLOR_MAP.put("Dark Orchid", getColor("#9932CC"));
		FANCY_HTML_COLOR_MAP.put("Dark Red", getColor("#8B0000"));
		FANCY_HTML_COLOR_MAP.put("Dark Salmon", getColor("#E9967A"));
		FANCY_HTML_COLOR_MAP.put("Dark Sea Green", getColor("#8FBC8F"));
		FANCY_HTML_COLOR_MAP.put("Dark Slate Blue", getColor("#483D8B"));
		FANCY_HTML_COLOR_MAP.put("Dark Slate Gray", getColor("#2F4F4F"));
		FANCY_HTML_COLOR_MAP.put("Dark Turquoise", getColor("#00CED1"));
		FANCY_HTML_COLOR_MAP.put("Dark Violet", getColor("#9400D3"));
		FANCY_HTML_COLOR_MAP.put("Deep Pink", getColor("#FF1493"));
		FANCY_HTML_COLOR_MAP.put("Deep Sky Blue", getColor("#00BFFF"));
		FANCY_HTML_COLOR_MAP.put("Dim Gray", getColor("#696969"));
		FANCY_HTML_COLOR_MAP.put("Dodger Blue", getColor("#1E90FF"));
		FANCY_HTML_COLOR_MAP.put("Fire Brick", getColor("#B22222"));
		FANCY_HTML_COLOR_MAP.put("Floral White", getColor("#FFFAF0"));
		FANCY_HTML_COLOR_MAP.put("Forest Green", getColor("#228B22"));
		FANCY_HTML_COLOR_MAP.put("Fuchsia", getColor("#FF00FF"));
		FANCY_HTML_COLOR_MAP.put("Gainsboro", getColor("#DCDCDC"));
		FANCY_HTML_COLOR_MAP.put("Ghost White", getColor("#F8F8FF"));
		FANCY_HTML_COLOR_MAP.put("Gold", getColor("#FFD700"));
		FANCY_HTML_COLOR_MAP.put("GoldenRod", getColor("#DAA520"));
		FANCY_HTML_COLOR_MAP.put("Gray", getColor("#808080"));
		FANCY_HTML_COLOR_MAP.put("Grey", getColor("#808080"));
		FANCY_HTML_COLOR_MAP.put("Green", getColor("#008000"));
		FANCY_HTML_COLOR_MAP.put("Green Yellow", getColor("#ADFF2F"));
		FANCY_HTML_COLOR_MAP.put("Honey Dew", getColor("#F0FFF0"));
		FANCY_HTML_COLOR_MAP.put("Hot Pink", getColor("#FF69B4"));
		FANCY_HTML_COLOR_MAP.put("Indian Red", getColor("#CD5C5C"));
		FANCY_HTML_COLOR_MAP.put("Indigo", getColor("#4B0082"));
		FANCY_HTML_COLOR_MAP.put("Ivory", getColor("#FFFFF0"));
		FANCY_HTML_COLOR_MAP.put("Khaki", getColor("#F0E68C"));
		FANCY_HTML_COLOR_MAP.put("Lavender", getColor("#E6E6FA"));
		FANCY_HTML_COLOR_MAP.put("Lavender Blush", getColor("#FFF0F5"));
		FANCY_HTML_COLOR_MAP.put("Lawn Green", getColor("#7CFC00"));
		FANCY_HTML_COLOR_MAP.put("Lemon Chiffon", getColor("#FFFACD"));
		FANCY_HTML_COLOR_MAP.put("Light Blue", getColor("#ADD8E6"));
		FANCY_HTML_COLOR_MAP.put("Light Coral", getColor("#F08080"));
		FANCY_HTML_COLOR_MAP.put("Light Cyan", getColor("#E0FFFF"));
		FANCY_HTML_COLOR_MAP.put("Light GoldenRod Yellow", getColor("#FAFAD2"));
		FANCY_HTML_COLOR_MAP.put("Light Gray", getColor("#D3D3D3"));
		FANCY_HTML_COLOR_MAP.put("Light Grey", getColor("#D3D3D3"));
		FANCY_HTML_COLOR_MAP.put("Light Green", getColor("#90EE90"));
		FANCY_HTML_COLOR_MAP.put("Light Pink", getColor("#FFB6C1"));
		FANCY_HTML_COLOR_MAP.put("Light Salmon", getColor("#FFA07A"));
		FANCY_HTML_COLOR_MAP.put("Light Sea Green", getColor("#20B2AA"));
		FANCY_HTML_COLOR_MAP.put("Light Sky Blue", getColor("#87CEFA"));
		FANCY_HTML_COLOR_MAP.put("Light Slate Gray", getColor("#778899"));
		FANCY_HTML_COLOR_MAP.put("Light Slate Grey", getColor("#778899"));
		FANCY_HTML_COLOR_MAP.put("Light Steel Blue", getColor("#B0C4DE"));
		FANCY_HTML_COLOR_MAP.put("Light Yellow", getColor("#FFFFE0"));
		FANCY_HTML_COLOR_MAP.put("Lime", getColor("#00FF00"));
		FANCY_HTML_COLOR_MAP.put("Lime Green", getColor("#32CD32"));
		FANCY_HTML_COLOR_MAP.put("Linen", getColor("#FAF0E6"));
		FANCY_HTML_COLOR_MAP.put("Magenta", getColor("#FF00FF"));
		FANCY_HTML_COLOR_MAP.put("Maroon", getColor("#800000"));
		FANCY_HTML_COLOR_MAP.put("Medium Aqua Marine", getColor("#66CDAA"));
		FANCY_HTML_COLOR_MAP.put("Medium Blue", getColor("#0000CD"));
		FANCY_HTML_COLOR_MAP.put("Medium Orchid", getColor("#BA55D3"));
		FANCY_HTML_COLOR_MAP.put("Medium Purple", getColor("#9370D8"));
		FANCY_HTML_COLOR_MAP.put("Medium Sea Green", getColor("#3CB371"));
		FANCY_HTML_COLOR_MAP.put("Medium Slate Blue", getColor("#7B68EE"));
		FANCY_HTML_COLOR_MAP.put("Medium Spring Green", getColor("#00FA9A"));
		FANCY_HTML_COLOR_MAP.put("Medium Turquoise", getColor("#48D1CC"));
		FANCY_HTML_COLOR_MAP.put("Medium Violet Red", getColor("#C71585"));
		FANCY_HTML_COLOR_MAP.put("Midnight Blue", getColor("#191970"));
		FANCY_HTML_COLOR_MAP.put("Mint Cream", getColor("#F5FFFA"));
		FANCY_HTML_COLOR_MAP.put("Misty Rose", getColor("#FFE4E1"));
		FANCY_HTML_COLOR_MAP.put("Moccasin", getColor("#FFE4B5"));
		FANCY_HTML_COLOR_MAP.put("Navajo White", getColor("#FFDEAD"));
		FANCY_HTML_COLOR_MAP.put("Navy", getColor("#000080"));
		FANCY_HTML_COLOR_MAP.put("Old Lace", getColor("#FDF5E6"));
		FANCY_HTML_COLOR_MAP.put("Olive", getColor("#808000"));
		FANCY_HTML_COLOR_MAP.put("Olive Drab", getColor("#6B8E23"));
		FANCY_HTML_COLOR_MAP.put("Orange", getColor("#FFA500"));
		FANCY_HTML_COLOR_MAP.put("Orange Red", getColor("#FF4500"));
		FANCY_HTML_COLOR_MAP.put("Orchid", getColor("#DA70D6"));
		FANCY_HTML_COLOR_MAP.put("Pale Golden Rod", getColor("#EEE8AA"));
		FANCY_HTML_COLOR_MAP.put("Pale Green", getColor("#98FB98"));
		FANCY_HTML_COLOR_MAP.put("Pale Turquoise", getColor("#AFEEEE"));
		FANCY_HTML_COLOR_MAP.put("Pale Violet Red", getColor("#D87093"));
		FANCY_HTML_COLOR_MAP.put("Papaya Whip", getColor("#FFEFD5"));
		FANCY_HTML_COLOR_MAP.put("Peach Puff", getColor("#FFDAB9"));
		FANCY_HTML_COLOR_MAP.put("Peru", getColor("#CD853F"));
		FANCY_HTML_COLOR_MAP.put("Pink", getColor("#FFC0CB"));
		FANCY_HTML_COLOR_MAP.put("Plum", getColor("#DDA0DD"));
		FANCY_HTML_COLOR_MAP.put("Powder Blue", getColor("#B0E0E6"));
		FANCY_HTML_COLOR_MAP.put("Purple", getColor("#800080"));
		FANCY_HTML_COLOR_MAP.put("Red", getColor("#FF0000"));
		FANCY_HTML_COLOR_MAP.put("Rosy Brown", getColor("#BC8F8F"));
		FANCY_HTML_COLOR_MAP.put("Royal Blue", getColor("#4169E1"));
		FANCY_HTML_COLOR_MAP.put("Saddle Brown", getColor("#8B4513"));
		FANCY_HTML_COLOR_MAP.put("Salmon", getColor("#FA8072"));
		FANCY_HTML_COLOR_MAP.put("Sandy Brown", getColor("#F4A460"));
		FANCY_HTML_COLOR_MAP.put("Sea Green", getColor("#2E8B57"));
		FANCY_HTML_COLOR_MAP.put("Sea Shell", getColor("#FFF5EE"));
		FANCY_HTML_COLOR_MAP.put("Sienna", getColor("#A0522D"));
		FANCY_HTML_COLOR_MAP.put("Silver", getColor("#C0C0C0"));
		FANCY_HTML_COLOR_MAP.put("Sky Blue", getColor("#87CEEB"));
		FANCY_HTML_COLOR_MAP.put("Slate Blue", getColor("#6A5ACD"));
		FANCY_HTML_COLOR_MAP.put("Slate Gray", getColor("#708090"));
		FANCY_HTML_COLOR_MAP.put("Snow", getColor("#FFFAFA"));
		FANCY_HTML_COLOR_MAP.put("Spring Green", getColor("#00FF7F"));
		FANCY_HTML_COLOR_MAP.put("Steel Blue", getColor("#4682B4"));
		FANCY_HTML_COLOR_MAP.put("Tan", getColor("#D2B48C"));
		FANCY_HTML_COLOR_MAP.put("Teal", getColor("#008080"));
		FANCY_HTML_COLOR_MAP.put("Thistle", getColor("#D8BFD8"));
		FANCY_HTML_COLOR_MAP.put("Tomato", getColor("#FF6347"));
		FANCY_HTML_COLOR_MAP.put("Turquoise", getColor("#40E0D0"));
		FANCY_HTML_COLOR_MAP.put("Violet", getColor("#EE82EE"));
		FANCY_HTML_COLOR_MAP.put("Wheat", getColor("#F5DEB3"));
		FANCY_HTML_COLOR_MAP.put("White", getColor("#FFFFFF"));
		FANCY_HTML_COLOR_MAP.put("White Smoke", getColor("#F5F5F5"));
		FANCY_HTML_COLOR_MAP.put("Yellow", getColor("#FFFF00"));
		FANCY_HTML_COLOR_MAP.put("Yellow Green", getColor("#9ACD32"));

		JAVA_COLOR_MAP.put("black", Color.black);
		JAVA_COLOR_MAP.put("blue", Color.blue);
		JAVA_COLOR_MAP.put("cyan", Color.cyan);
		JAVA_COLOR_MAP.put("darkGray", Color.darkGray);
		JAVA_COLOR_MAP.put("gray", Color.gray);
		JAVA_COLOR_MAP.put("green", Color.green);
		JAVA_COLOR_MAP.put("lightGray", Color.lightGray);
		JAVA_COLOR_MAP.put("magenta", Color.magenta);
		JAVA_COLOR_MAP.put("orange", Color.orange);
		JAVA_COLOR_MAP.put("pink", Color.pink);
		JAVA_COLOR_MAP.put("red", Color.red);
		JAVA_COLOR_MAP.put("white", Color.white);
		JAVA_COLOR_MAP.put("yellow", Color.yellow);

	}

	private ColorUtil() {
	}

	public static String getName(Color color) {
		return getName(color, HTML_COLOR_MAP);
	}

	public static String getFancyName(Color color) {
		return getName(color, FANCY_HTML_COLOR_MAP);
	}

	public static String getJavaName(Color color) {
		return getName(color, JAVA_COLOR_MAP);
	}

	public static String getName(Color color, Map<String, Color> colorMap) {
		String minName = null;
		double minDistance = Double.MAX_VALUE;
		double x = color.getRed();
		double y = color.getGreen();
		double z = color.getBlue();
		for (String name : colorMap.keySet()) {
			Color color2 = colorMap.get(name);
			double a = color2.getRed();
			double b = color2.getGreen();
			double c = color2.getBlue();
			double dist = ShapeUtil.distance(x, y, z, a, b, c);
			if (dist < minDistance) {
				minDistance = dist;
				minName = name;
			}
		}
		return minName.toLowerCase();
	}

	public static Color mergeColors(Color... colors) {
		int red = 0;
		int blue = 0;
		int green = 0;
		int alpha = 0;
		for (Color c : colors) {
			red += c.getRed();
			blue += c.getBlue();
			green += c.getGreen();
			alpha += c.getAlpha();
		}
		return new Color(red / colors.length, green / colors.length, blue
				/ colors.length, alpha / colors.length);
	}

	public static String getHTMLCode(Color color) {
		StringBuffer out = new StringBuffer();
		out.append(ColorUtil.getHexDigit(color.getRed() / 16));
		out.append(ColorUtil.getHexDigit(color.getRed() % 16));
		out.append(ColorUtil.getHexDigit(color.getGreen() / 16));
		out.append(ColorUtil.getHexDigit(color.getGreen() % 16));
		out.append(ColorUtil.getHexDigit(color.getBlue() / 16));
		out.append(ColorUtil.getHexDigit(color.getBlue() % 16));
		return out.toString();
	}

	public static Color getColor(String htmlCode) {
		int out = 0;
		htmlCode = htmlCode.trim();
		if (htmlCode.startsWith("#"))
			htmlCode = htmlCode.substring(1, 7);
		for (int i = 0; i < htmlCode.length(); i++) {
			out += Math.pow(16, htmlCode.length() - i - 1)
					* ColorUtil.getHexVal(htmlCode.charAt(i));
		}
		return new Color(out);
	}

	protected static int getHexVal(char digit) {
		digit = Character.toUpperCase(digit);
		if (Character.isDigit(digit))
			return digit - '0';
		else
			return 10 + (digit - 'A');
	}

	protected static String getHexDigit(int val) {
		if (val < 10)
			return val + "";
		else if (val == 10)
			return "A";
		else if (val == 11)
			return "B";
		else if (val == 12)
			return "C";
		else if (val == 13)
			return "D";
		else if (val == 14)
			return "E";
		else if (val == 15)
			return "F";
		return null;
	}
}
