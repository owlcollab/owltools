package owltools.gfx;

import java.awt.*;
import java.util.*;

/**
 * hacked crudely from QuickGO
 * 
 * TODO: use config object to map relationship types to colors
*/
public enum RelationType {
    ANCESTOR("?", "Ancestor","ancestor"),
    IDENTITY("=", "Identity","equals"),
    ISA("I", "Is a", "is_a",new Color(0,0,0)),
    PARTOF("P", "Part of", "part_of",new Color(0,0,255)),
    REGULATES("R", "Regulates", "regulated",new Color(255,192,0)),
    POSITIVEREGULATES("+", "Positively regulates","positively_regulates","PR", new Color(0,255,0)),
    NEGATIVEREGULATES("-", "Negatively regulates","negatively_regulates","NR", new Color(255,0,0)),
    DEVELOPSFROM(">", "Develops from","develops_from","develops_from", new Color(255,0,255)),
    REPLACEDBY(">", "Replaced by","replaced_by","replaced_by", new Color(255,0,255)),
    CONSIDER("~", "Consider","consider","consider", new Color(192,0,255)),
	HASPART("H", "Has part", "has_part",new Color(128,0,128), Polarity.NEGATIVE);

	public enum Polarity {
		POSITIVE,   // relation is unidirectional from child to parent
		NEGATIVE,   // relation is unidirectional from parent to child
		NEUTRAL,    // relation is non-directional
		BIPOLAR     // relation is bi-directional 
	}

    public String code;
    public String description;
    public String formalCode;
    public String alternativeCode;
    public Color color;
	public Polarity polarity;

    RelationType(String code, String description, String formalCode, String alternativeCode, Color color, Polarity polarity) {
        this.code = code;
        this.description = description;
        this.alternativeCode = alternativeCode;
        this.color = color;
	    this.polarity = polarity;
        this.formalCode=formalCode;
    }

	RelationType(String code, String description, String formalCode, String alternativeCode, Color color) {
		this(code, description, formalCode,alternativeCode, color, Polarity.POSITIVE);
	}

    RelationType(String code, String description, String formalCode, Color color, Polarity polarity) {
	    this(code, description, formalCode, code, color, polarity);
    }

	RelationType(String code, String description, String formalCode, Color color) {
		this(code, description, formalCode, code, color, Polarity.POSITIVE);
	}

    RelationType(String code, String description,String formalCode) {
	    this(code, description, formalCode, code, new Color(0, 0, 0), Polarity.POSITIVE);
    }

    boolean ofType(RelationType query) {
        return (query == RelationType.ANCESTOR) || (this == IDENTITY) || (query == this) || (query == REGULATES && (this == POSITIVEREGULATES || this == NEGATIVEREGULATES));
    }

    boolean ofAnyType(EnumSet<RelationType> types) {
        for (RelationType type : types) {
            if (ofType(type)) return true;
        }
        return false;
    }

    static RelationType byCode(String code) {
        for (RelationType rt : values()) {
            if (rt.code.equals(code) || code.equals(rt.alternativeCode)) return rt;
        }
        throw new IllegalArgumentException("No such relation type as "+code);
    }

    public static EnumSet<RelationType> forCodes(String types) {
        Set<RelationType> rt=new HashSet<RelationType>();
        for (int i=0;i<types.length();i++) {
            rt.add(byCode(""+types.charAt(i)));
        }
        return EnumSet.copyOf(rt);
    }
}
