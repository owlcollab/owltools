package owltools.phenolog;

//import org.semanticweb.owlapi.model.OWLObject;

/**
 * Represents a characteristic of an individual.
 * This could be represented using an ID for an ontology class.
 *
 */
public class GenePheno implements Comparable<GenePheno>{
    private String id;
    private String label;
    private String phenoid;
    private String phenolabel;

//    @Override
    public boolean equals(Object aGenePheno){
    	if (aGenePheno == null) {
			return false;
		}
    	if (aGenePheno instanceof GenePheno == false) {
			return false;
		}
        GenePheno gp = (GenePheno) aGenePheno;
        String cc1 = this.id.concat(this.phenoid);
        return cc1.equals(gp.getid().concat(gp.getphenoid()));
    }
//    @Override
    public int hashCode(){
        return id.hashCode();
    }
//    @Override
    public int compareTo(GenePheno g){
        String cc1 = this.id.concat(this.phenoid);
        String cc2 = g.getid().concat(g.getphenoid());
        return cc1.compareTo(cc2);
    }

    
    public GenePheno(String id, String label, String phenoid, String phenolabel){
        this.id = id;
        this.label = label;
        this.phenoid = phenoid;
        this.phenolabel = phenolabel;
    }

    public GenePheno(String id, String phenoid){
        this.id = id;
        this.phenoid = phenoid;
    }

    public void setid(String id){
        this.id = id;
    }
    public void setphenoid(String phenoid){
        this.phenoid = phenoid;
    }
    public void setlabel(String label){
        this.label = label;
    }
    public void setphenolabel(String phenolabel){
        this.phenolabel = phenolabel;
    }

    public String getid(){
        return this.id;
    }
    public String getlabel(){
        return this.label;
    }
    public String getphenoid(){
        return this.phenoid;
    }
    public String getphenolabel(){
        return this.phenolabel;
    }
}
