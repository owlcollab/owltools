package owltools.io;

import org.coode.owlapi.obo.parser.IDSpaceManager;
import org.semanticweb.owlapi.model.OWLOntologyFormat;


public class OWLJSONFormat extends OWLOntologyFormat {

    private IDSpaceManager idSpaceManager = new IDSpaceManager();

    public OWLJSONFormat() {
    }

    /**
     * Constructs an OBOOntologyFormat object.
     * @param idSpaceManager An {@link IDSpaceManager} which specifies mappings between id prefixes and IRI prefixes.
     */
    public OWLJSONFormat(IDSpaceManager idSpaceManager) {
        this.idSpaceManager = idSpaceManager;
    }

    /**
     * Returns a string representation of the object. In general, the
     * <code>toString</code> method returns a string that
     * "textually represents" this object. The result should
     * be a concise but informative representation that is easy for a
     * person to read.
     * It is recommended that all subclasses override this method.
     * <p/>
     * The <code>toString</code> method for class <code>Object</code>
     * returns a string consisting of the name of the class of which the
     * object is an instance, the at-sign character `<code>@</code>', and
     * the unsigned hexadecimal representation of the hash code of the
     * object. In other words, this method returns a string equal to the
     * value of:
     * <blockquote>
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre></blockquote>
     * @return a string representation of the object.
     */
    @Override
	public String toString() {
        return "OWL JSON Format";
    }

    /**
     * Gets the OBO id-space manager.  This is NOT the same as a prefix manager.
     * @return The {@link IDSpaceManager} for this format.  For ontologies parsed from an OBO file this will contain
     * any id prefix to IRI prefix mappings that were parsed out of the file (from id-space tags).  Not null.
     */
    public IDSpaceManager getIdSpaceManager() {
        return idSpaceManager;
    }
}
