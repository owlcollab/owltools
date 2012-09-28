package org.bbop.graph;

import java.util.List;

import org.bbop.util.IDUtil;
import org.semanticweb.owlapi.model.OWLObject;

public class HTMLNodeLabelProvider implements NodeLabelProvider {

	private final String htmlExpression;
	private final NodeLabelProvider labelProvider;

	public HTMLNodeLabelProvider(NodeLabelProvider labelProvider) {
		this("$name$", labelProvider);
	}
	
	public HTMLNodeLabelProvider(String htmlExpression, NodeLabelProvider labelProvider) {
		this.htmlExpression = htmlExpression;
		this.labelProvider = labelProvider;
	}

	public String resolveHTMLExpression(String exp, OWLObject lo) {
		StringBuffer out = new StringBuffer();
		List<?> tokens = IDUtil.parseVarString(exp);
		for (Object token : tokens) {
			if (token instanceof IDUtil.Variable) {
				IDUtil.Variable var = (IDUtil.Variable) token;
				if ("name".equals(var.getName())) {
					out.append(labelProvider.getLabel(lo));
				}
				else {
					out.append("??cannot-resolve-"+var.getName()+"??");
				}
			} else
				out.append(token.toString());
		}
		return out.toString();
	}

	@Override
	public String getLabel(OWLObject obj) {
		return resolveHTMLExpression(htmlExpression, obj);
	}

	public String getHtmlExpression() {
		return htmlExpression;
	}

}
