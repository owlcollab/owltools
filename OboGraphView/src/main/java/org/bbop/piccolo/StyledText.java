package org.bbop.piccolo;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.html.HTMLDocument;

import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolox.nodes.PStyledText;

public class StyledText extends PStyledText {

	// generated
	private static final long serialVersionUID = -4886389766972446870L;

	/**
	 * Compute the bounds of the text wrapped by this node. The text layout
	 * is wrapped based on the bounds of this node. If the shrinkBoundsToFit parameter
	 * is true then after the text has been laid out the bounds of this node are shrunk
	 * to fit around those text bounds.
	 */
	@Override
	public void recomputeLayout() {
		if (stringContents == null) return;

		ArrayList<LineInfo> linesList = new ArrayList<LineInfo>();

		double textWidth = 0;
		double textHeight = 0;
		
		for(int i=0; i<stringContents.size(); i++) {		

			AttributedString ats = (AttributedString)stringContents.get(i);
			AttributedCharacterIterator itr = ats.getIterator();
						
			LineBreakMeasurer measurer;
			ArrayList<Integer> breakList = null;
			
			// First we have to do an initial pass with a LineBreakMeasurer to
			// find out where Swing is going to break the lines - i.e.
			// because it doesn't use fractional metrics

			measurer = new LineBreakMeasurer(itr, SWING_FRC);
			breakList = new ArrayList<Integer>();
			while(measurer.getPosition() < itr.getEndIndex()) {
				if (constrainWidthToTextWidth) {
					measurer.nextLayout(Float.MAX_VALUE);
				} 
				else {
					measurer.nextLayout((float)Math.ceil(getWidth()-insets.left-insets.right));
				}

				breakList.add(new Integer(measurer.getPosition()));
			}

			measurer = new LineBreakMeasurer(itr, PPaintContext.RENDER_QUALITY_HIGH_FRC);

			// Need to change the lineinfo data structure to know about multiple
			// text layouts per line
			
			LineInfo lineInfo = null;
			boolean newLine = true;
			double lineWidth = 0;
			while (measurer.getPosition() < itr.getEndIndex()) {
				TextLayout aTextLayout = null;
				
				if (newLine) {
				    newLine = false;
				    
				    // Add in the old line dimensions
				    double lineHeight = (lineInfo == null) ? 0 : lineInfo.maxAscent+lineInfo.maxDescent+lineInfo.leading;
				    textHeight = textHeight+lineHeight;
				    textWidth = Math.max(textWidth,lineWidth);
				    
				    // Now create a new line
				    lineInfo = new LineInfo();
				    linesList.add(lineInfo);				    
				}
				
			    int lineEnd = breakList.get(0);			    
			    if (lineEnd <= itr.getRunLimit()) {
			        breakList.remove(0);
			        newLine = true;
			    }
			    float wrapWidth;
			    if (constrainWidthToTextWidth) {
			    	wrapWidth = Float.MAX_VALUE;
				} 
				else {
					wrapWidth = (float)Math.ceil(getWidth()-insets.left-insets.right);
				}
			    
				aTextLayout = measurer.nextLayout(wrapWidth,Math.min(lineEnd,itr.getRunLimit()),false);
				
				SegmentInfo sInfo = new SegmentInfo();
				sInfo.font = (Font)itr.getAttribute(TextAttribute.FONT);
				sInfo.foreground = (Color)itr.getAttribute(TextAttribute.FOREGROUND);
				sInfo.background = (Color)itr.getAttribute(TextAttribute.BACKGROUND);
				sInfo.underline = (Boolean)itr.getAttribute(TextAttribute.UNDERLINE);
				sInfo.layout = aTextLayout;
								
				FontMetrics metrics = StyleContext.getDefaultStyleContext().getFontMetrics((Font)itr.getAttribute(TextAttribute.FONT));
				lineInfo.maxAscent = Math.max(lineInfo.maxAscent,metrics.getMaxAscent());
				lineInfo.maxDescent = Math.max(lineInfo.maxDescent,metrics.getMaxDescent());
				lineInfo.leading = Math.max(lineInfo.leading,metrics.getLeading());
				
				lineInfo.segments.add(sInfo);
				
				itr.setIndex(measurer.getPosition());
				lineWidth = lineWidth+aTextLayout.getAdvance();
			}
			
		    double lineHeight = (lineInfo == null) ? 0 : lineInfo.maxAscent+lineInfo.maxDescent+lineInfo.leading;
		    textHeight = textHeight+lineHeight;
		    textWidth = Math.max(textWidth,lineWidth);
		}
		
		lines = linesList.toArray(new LineInfo[linesList.size()]);
		
		if (constrainWidthToTextWidth || constrainHeightToTextHeight) {
			double newWidth = getWidth();
			double newHeight = getHeight();
			
			if (constrainWidthToTextWidth) {
				newWidth = textWidth + insets.left + insets.right;
			}
			
			if (constrainHeightToTextHeight) {
				newHeight = Math.max(textHeight,getInitialFontHeight()) + insets.top + insets.bottom;
			}
	
			super.setBounds(getX(), getY(), newWidth, newHeight);
		}	
	}
	@Override
	public void syncWithDocument() {
		// The paragraph start and end indices
		ArrayList<RunInfoOld> pEnds = null;
		
		// The current position in the specified range
		int pos = 0;
	
		// First get the actual text and stick it in an Attributed String
		try {

			stringContents = new ArrayList<AttributedString>();
			pEnds = new ArrayList<RunInfoOld>();
			
			String s = document.getText(0,document.getLength());
			StringTokenizer tokenizer = new StringTokenizer(s,"\n",true);
	
			// lastNewLine is used to detect the case when two newlines follow in direct succession
			// & lastNewLine should be true to start in case the first character is a newline
			boolean lastNewLine = true;
			for(int i=0; tokenizer.hasMoreTokens(); i++) {
				String token = tokenizer.nextToken();
				
				// If the token 
				if (token.equals("\n")) {
					if (lastNewLine) {
						stringContents.add(new AttributedString(" "));
						pEnds.add(new RunInfoOld(pos,pos+1));
						
						pos = pos + 1;

						lastNewLine = true;
					}
					else {
						pos = pos + 1;
						
						lastNewLine = true;
					}
				}
				// If the token is empty - create an attributed string with a single space
				// since LineBreakMeasurers don't work with an empty string
				// - note that this case should only arise if the document is empty
				else if (token.equals("")) {
					stringContents.add(new AttributedString(" "));
					pEnds.add(new RunInfoOld(pos,pos));

					lastNewLine = false;					
				}
				// This is the normal case - where we have some text
				else {
					stringContents.add(new AttributedString(token));
					pEnds.add(new RunInfoOld(pos,pos+token.length()));

					// Increment the position
					pos = pos+token.length();
					
					lastNewLine = false;
				}
			}
			
			// Add one more newline if the last character was a newline
			if (lastNewLine) {
				stringContents.add(new AttributedString(" "));
				pEnds.add(new RunInfoOld(pos,pos+1));
				
				lastNewLine = false;
			}
		}
		catch (Exception e) {
			e.printStackTrace();	
		}

		// The default style context - which will be reused
		StyleContext style;
		if (document instanceof HTMLDocument)
			style = ((HTMLDocument) document).getStyleSheet();
		else
			style = StyleContext.getDefaultStyleContext();

		RunInfoOld pEnd = null;
		for (int i = 0; i < stringContents.size(); i++) {
			pEnd = pEnds.get(i);
			pos = pEnd.runStart;

			// The current element will be used as a temp variable while searching
			// for the leaf element at the current position
			Element curElement = null;

			// Small assumption here that there is one root element - can fix
			// for more general support later
			Element rootElement = document.getDefaultRootElement();

			// If the string is length 0 then we just need to add the attributes once
			if (pEnd.runStart != pEnd.runLimit) {
				// OK, now we loop until we find all the leaf elements in the range
				while (pos < pEnd.runLimit) {
					
					// Before each pass, start at the root
					curElement = rootElement;
	
					// Now we descend the hierarchy until we get to a leaf
					while (!curElement.isLeaf()) {
						curElement =
							curElement.getElement(curElement.getElementIndex(pos));
					}

					// These are the mandatory attributes

					AttributeSet attributes = curElement.getAttributes();
					Color foreground = style.getForeground(attributes);

					((AttributedString)stringContents.get(i)).addAttribute(
						TextAttribute.FOREGROUND,
						foreground,
						Math.max(0,curElement.getStartOffset()-pEnd.runStart),
						Math.min(pEnd.runLimit-pEnd.runStart,curElement.getEndOffset()-pEnd.runStart));
					
					Font font = (attributes.isDefined(StyleConstants.FontSize) || attributes.isDefined(StyleConstants.FontFamily)) ? style.getFont(attributes) : null;
					if (font == null) {
					    if (document instanceof DefaultStyledDocument) {
					        font = style.getFont(((DefaultStyledDocument)document).getCharacterElement(pos).getAttributes());
					        if (font == null) {
					            font = style.getFont(((DefaultStyledDocument)document).getParagraphElement(pos).getAttributes());
					        }
					        if (font == null) {
					            font = style.getFont(rootElement.getAttributes());
					        }
					    }
					    else {
					        font = style.getFont(rootElement.getAttributes());
					    }
					}					
					if (font != null) {
						((AttributedString)stringContents.get(i)).addAttribute(
								TextAttribute.FONT,
								font,
								Math.max(0,curElement.getStartOffset()-pEnd.runStart),
								Math.min(pEnd.runLimit-pEnd.runStart,curElement.getEndOffset()-pEnd.runStart));				    
					}
					
					// These are the optional attributes
					
					Color background = (attributes.isDefined(StyleConstants.Background)) ? style.getBackground(attributes) : null;
					if (background != null) {
						((AttributedString)stringContents.get(i)).addAttribute(
								TextAttribute.BACKGROUND,
								background,
								Math.max(0,curElement.getStartOffset()-pEnd.runStart),
								Math.min(pEnd.runLimit-pEnd.runStart,curElement.getEndOffset()-pEnd.runStart));					    
					}
					
					boolean underline = StyleConstants.isUnderline(attributes);
					if (underline) {
						((AttributedString)stringContents.get(i)).addAttribute(
							TextAttribute.UNDERLINE,
							Boolean.TRUE,
							Math.max(0,curElement.getStartOffset()-pEnd.runStart),
							Math.min(pEnd.runLimit-pEnd.runStart,curElement.getEndOffset()-pEnd.runStart));						
					}
					
					boolean strikethrough = StyleConstants.isStrikeThrough(attributes);
					if (strikethrough) {
						((AttributedString)stringContents.get(i)).addAttribute(
							TextAttribute.STRIKETHROUGH,
							Boolean.TRUE,
							Math.max(0,curElement.getStartOffset()-pEnd.runStart),
							Math.min(pEnd.runLimit-pEnd.runStart,curElement.getEndOffset()-pEnd.runStart));						
					}
	
					// And set the position to the end of the given attribute
					pos = curElement.getEndOffset();
				}
			}
			else {
				// Before each pass, start at the root
				curElement = rootElement;

				// Now we descend the hierarchy until we get to a leaf
				while (!curElement.isLeaf()) {
					curElement =
						curElement.getElement(curElement.getElementIndex(pos));
				}

				// These are the mandatory attributes
				
				AttributeSet attributes = curElement.getAttributes();
				Color foreground = style.getForeground(attributes);

				((AttributedString)stringContents.get(i)).addAttribute(
					TextAttribute.FOREGROUND,
					foreground,
					Math.max(0,curElement.getStartOffset()-pEnd.runStart),
					Math.min(pEnd.runLimit-pEnd.runStart,curElement.getEndOffset()-pEnd.runStart));			

				// These are the optional attributes

				Font font = (attributes.isDefined(StyleConstants.FontSize) || attributes.isDefined(StyleConstants.FontFamily)) ? style.getFont(attributes) : null;
				if (font == null) {
				    if (document instanceof DefaultStyledDocument) {
				        font = style.getFont(((DefaultStyledDocument)document).getCharacterElement(pos).getAttributes());
				        if (font == null) {
				            font = style.getFont(((DefaultStyledDocument)document).getParagraphElement(pos).getAttributes());
				        }
				        if (font == null) {
				            font = style.getFont(rootElement.getAttributes());
				        }
				    }
				    else {
				        font = style.getFont(rootElement.getAttributes());
				    }
				}					
				if (font != null) {
					((AttributedString)stringContents.get(i)).addAttribute(
							TextAttribute.FONT,
							font,
							Math.max(0,curElement.getStartOffset()-pEnd.runStart),
							Math.min(pEnd.runLimit-pEnd.runStart,curElement.getEndOffset()-pEnd.runStart));				    
				}				
				
				Color background = (attributes.isDefined(StyleConstants.Background)) ? style.getBackground(attributes) : null;
				if (background != null) {
					((AttributedString)stringContents.get(i)).addAttribute(
							TextAttribute.BACKGROUND,
							background,
							Math.max(0,curElement.getStartOffset()-pEnd.runStart),
							Math.min(pEnd.runLimit-pEnd.runStart,curElement.getEndOffset()-pEnd.runStart));					    
				}
				
				boolean underline = StyleConstants.isUnderline(attributes);
				if (underline) {
					((AttributedString)stringContents.get(i)).addAttribute(
						TextAttribute.UNDERLINE,
						Boolean.TRUE,
						Math.max(0,curElement.getStartOffset()-pEnd.runStart),
						Math.min(pEnd.runLimit-pEnd.runStart,curElement.getEndOffset()-pEnd.runStart));						
				}
					
				boolean strikethrough = StyleConstants.isStrikeThrough(attributes);
				if (strikethrough) {
					((AttributedString)stringContents.get(i)).addAttribute(
						TextAttribute.STRIKETHROUGH,
						Boolean.TRUE,
						Math.max(0,curElement.getStartOffset()-pEnd.runStart),
						Math.min(pEnd.runLimit-pEnd.runStart,curElement.getEndOffset()-pEnd.runStart));						
				}
			}
		}

		recomputeLayout();
	}
	
	private static class RunInfoOld extends RunInfo {

		public final int runStart;
		public final int runLimit;

		public RunInfoOld(final int runStart, final int runLimit) {
			super(runStart, runLimit);
			this.runStart = runStart;
			this.runLimit = runLimit;
		}
		
	}

}
