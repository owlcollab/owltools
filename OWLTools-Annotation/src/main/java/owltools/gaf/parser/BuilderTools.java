package owltools.gaf.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import owltools.gaf.Bioentity;
import owltools.gaf.ExtensionExpression;
import owltools.gaf.GeneAnnotation;

public class BuilderTools {

	static String handleTaxonPrefix(String s) {
		if (s != null && !s.isEmpty()) {
			int colPos = s.indexOf(':');
			StringBuilder sb = new StringBuilder("NCBITaxon:");
			if (colPos > 0) {
				sb.append(s.substring(colPos+1));
			}
			else {
				sb.append(s);
			}
			return sb.toString();
		}
		return s;
	}
	
	public static String removePrefix(String s, char marker) {
		int pos = s.indexOf(marker);
		if (pos > 0) {
			if ((pos + 1) < s.length()) {
				pos += 1;
			}
			return s.substring(pos);
		}
		return s;
	}
	
	static void addProperties(String s, Bioentity entity) {
		s = StringUtils.trimToNull(s);
		if (s != null) {
			String[] pairs = StringUtils.split(s, '|');
			for (String pairString : pairs) {
				String[] pair = StringUtils.split(pairString, "=", 2);
				if (pair.length == 2) {
					entity.addProperty(pair[0], pair[1]);
				}
			}
		}
	}
	
	static void addProperties(String s, GeneAnnotation ga) {
		s = StringUtils.trimToNull(s);
		if (s != null) {
			String[] pairs = StringUtils.split(s, '|');
			for (String pairString : pairs) {
				String[] pair = StringUtils.split(pairString, "=", 2);
				if (pair.length == 2) {
					ga.addProperty(pair[0], pair[1]);
				}
			}
		}
	}
	
	static void addXrefs(String s, Bioentity entity) {
		s = StringUtils.trimToNull(s);
		if (s != null) {
			String[] refs = StringUtils.split(s, '|');
			for (String ref : refs) {
				ref = StringUtils.trimToNull(ref);
				if (ref != null) {
					entity.addDbXref(ref);
				}
			}
		}
	}
	
	static void addXrefs(String s, GeneAnnotation ga) {
		s = StringUtils.trimToNull(s);
		if (s != null) {
			String[] refs = StringUtils.split(s, '|');
			for (String ref : refs) {
				ref = StringUtils.trimToNull(ref);
				if (ref != null) {
					ga.addReferenceId(ref);
				}
			}
		}
	}
	
	static void addSynonyms(String s, Bioentity entity) {
		s = StringUtils.trimToNull(s);
		if (s != null) {
			String[] synonyms = StringUtils.split(s, '|');
			for (String syn : synonyms) {
				syn = StringUtils.trimToNull(syn);
				if (syn != null) {
					entity.addSynonym(syn);
				}
			}
		}
	}

	/**
	 * Parse the string into a collection of with strings
	 * 
	 * @param withInfoString
	 * @return collection, never null
	 */
	public static Collection<String> parseWithInfo(final String withInfoString){
		Collection<String> infos = Collections.emptySet();
		if(withInfoString.length()>0){
			infos = new ArrayList<String>();
			String tokens[] = withInfoString.split("[\\||,]");
			for(String token: tokens){
				infos.add(token);
			}
		}
		return infos;
	}

	/**
	 * Parse the string into a list of qualifier strings
	 * 
	 * @param qualifierString
	 * @return collection, never null
	 */
	public static List<String> parseCompositeQualifier(String qualifierString){
		List<String> qualifiers = Collections.emptyList();
		if(qualifierString.length()>0){
			qualifiers = new ArrayList<String>();
			String tokens[] = qualifierString.split("[\\||,]");
			for(String token: tokens){
				qualifiers.add(token);
			}
		}
		return qualifiers;
	}

	public static Pair<String, String> parseTaxonRelationshipPair(String source) {
		source = StringUtils.trimToNull(source);
		if (source != null) {
			int open = source.indexOf('(');
			if (open > 0) {
				int close = source .indexOf(')', open);
				if (close > 0) {
					String rel = StringUtils.trimToNull(source.substring(0, open));
					String tax = StringUtils.trimToNull(source.substring(open+1, close));
					if (tax != null && rel != null) {
						return Pair.of(tax, rel);
					}
				}
			}
			else {
				return Pair.<String, String>of(source, null);
			}
			
		}
		return null;
	}
	
	/**
	 * @param extensionExpressionString
	 * @return list, never null
	 */
	public static List<List<ExtensionExpression>> parseExtensionExpression(String extensionExpressionString){
		List<List<ExtensionExpression>> groupedExpressions = Collections.emptyList();
		if(extensionExpressionString != null && extensionExpressionString.length() > 0){
			// first split by '|' to get groups
			String[] groups = StringUtils.split(extensionExpressionString, '|');
			groupedExpressions = new ArrayList<List<ExtensionExpression>>(groups.length);
			for (int i = 0; i < groups.length; i++) {
				// split by ',' to get individual entries
				String[] expressionStrings = StringUtils.split(groups[i], ',');
				List<ExtensionExpression> expressions = new ArrayList<ExtensionExpression>(expressionStrings.length);
				for (int j = 0; j < expressionStrings.length; j++) {
					String token = expressionStrings[j];
					int index = token.indexOf("(");
					if(index > 0){
						String relation = token.substring(0, index);
						String cls = token.substring(index+1, token.length()-1);
						expressions.add(new ExtensionExpression(relation, cls));
					}
				}
				if (expressions.isEmpty() == false) {
					groupedExpressions.add(expressions);
				}
			}
			if (groupedExpressions.isEmpty()) {
				groupedExpressions = Collections.emptyList();
			}
		}
		return groupedExpressions;
	}

	public static String buildExtensionExpression(List<List<ExtensionExpression>> groupedExpressions) {
		StringBuilder sb = new StringBuilder();
		if (groupedExpressions != null && !groupedExpressions.isEmpty()) {
			for (List<ExtensionExpression> group : groupedExpressions) {
				if (sb.length() > 0) {
					sb.append('|');
				}
				for (int i = 0; i < group.size(); i++) {
					ExtensionExpression expression = group.get(i);
					if (i > 0) {
						sb.append(',');
					}
					sb.append(expression.getRelation()).append('(').append(expression.getCls()).append(')');
				}
			}
		}
		return sb.toString();
	}
	
	public static String buildPropertyExpression(List<Pair<String, String>> properties) {
		if (properties != null) {
			StringBuilder sb = new StringBuilder();
			for (Pair<String, String> pair : properties) {
				if (sb.length() > 0) {
					sb.append('|');
				}
				sb.append(pair.getLeft()).append('=').append(pair.getRight());
			}
			return sb.toString();
		}
		return null;
	}
	
	public static String buildTaxonString(String bioEntityTaxon, Pair<String, String> actsOnTaxonRelPair) {
		StringBuilder sb = new StringBuilder();
		if (bioEntityTaxon != null) {
			sb.append("taxon:").append(removePrefix(bioEntityTaxon, ':'));
		}
		if (actsOnTaxonRelPair != null) {
			if (sb.length() > 0) {
				sb.append('|');
			}
			String taxId = "taxon:"+removePrefix(actsOnTaxonRelPair.getLeft(), ':');
			String rel = actsOnTaxonRelPair.getRight();
			sb.append(rel).append("(").append(taxId).append(")");
		}
		if (sb.length() > 0) {
			return sb.toString();
		}
		return null;
	}
	
	public static String buildTaxonString(Pair<String, String> taxonRelPair) {
		if (taxonRelPair != null) {
			String taxId = "taxon:"+removePrefix(taxonRelPair.getLeft(), ':');
			String rel = taxonRelPair.getRight();
			return rel+"("+taxId+")";
		}
		return null;
	}
	
	public static String buildWithString(Collection<String> withInfos) {
		if (withInfos != null && !withInfos.isEmpty()) {
			return StringUtils.join(withInfos, '|');
		}
		return null;
	}
	
	public static String buildQualifierString(List<String> qualifierList) {
		if (qualifierList != null && !qualifierList.isEmpty()) {
			return StringUtils.join(qualifierList, '|');
		}
		return null;
	}
	
	public static String buildReferenceIdsString(List<String> referenceIds) {
		if (referenceIds != null && !referenceIds.isEmpty()) {
			return StringUtils.join(referenceIds, '|');
		}
		return null;
	}
}
