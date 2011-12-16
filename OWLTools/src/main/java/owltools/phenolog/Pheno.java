package owltools.phenolog;


import java.util.HashSet;
import java.util.Set;

//import org.semanticweb.owlapi.model.OWLObject;

/**
 * represents an attribute-bearing entity; for example, a phenotype.
 */


public class Pheno implements Comparable<Pheno>{

	private String id;
	private String label;
        private Pheno closest=null;
        private double closestdistance=1;
        private int closestoverlap;
        private HashSet<IndividualPair> closestoverlappairs;
	//private OWLObject owlObject; // for future use
	private HashSet<Individual> individuals = new HashSet<Individual>();
        private boolean isFromTC = true;
        private HashSet<Pheno> ancestors = null;
        private int NonTCIndividualSize = 0;


//        @Override
        public boolean equals(Object aPheno){
            Pheno p = (Pheno) aPheno;
            return getId().equals(p.getId());
        }
//        @Override
        public int hashCode(){
            return id.hashCode();
        }
//        @Override
        public int compareTo(Pheno p){
            return id.compareTo(p.getId());
        }

        public Pheno(){

        }
        public Pheno(String id){
            this.id = id;
        }
        public Pheno(String id, String label){
            this.id = id;
            this.label = label;
        }
        public Pheno(String id, String label, HashSet<Individual> individuals){
            this.id = id;
            this.label = label;
            this.individuals = individuals;
        }
        public Pheno(String id, String label, HashSet<Individual> individuals, boolean isFromTC){
            this.id = id;
            this.label = label;
            this.individuals = individuals;
            this.isFromTC = isFromTC;
        }

	public String getId() {
            return id;
	}
	public void setId(String id) {
            this.id = id;
	}

	public String getLabel() {
            return label;
	}
	public void setLabel(String label) {
            this.label = label;
	}

        public void setClosest(Pheno closest){
            this.closest = closest;
        }
        public Pheno getClosest(){
            return closest;
        }

        public void setClosestDistance(double closestdistance){
            this.closestdistance = closestdistance;
        }
        public double getClosestDistance(){
            return closestdistance;
        }

        public void setClosestOverlap(int closestoverlap){
            this.closestoverlap = closestoverlap;
        }
        public int getClosestOverlap(){
            return closestoverlap;
        }

        public void setClosestOverlapPairs(HashSet<IndividualPair> hsip){
            this.closestoverlappairs = hsip;
        }
        public HashSet<IndividualPair> getClosestOverlapPairs(){
            return closestoverlappairs;
        }

	public HashSet<Individual> getIndividuals() {
		return individuals;
	}

	public void setIndividuals(HashSet<Individual> individuals) {
		this.individuals = individuals;
	}

        public void setisFromTC(boolean isFromTC){
            this.isFromTC = isFromTC;
        }
        public boolean getisFromTC(){
            return this.isFromTC;
        }

        public HashSet<Pheno> getancestors(){
            return this.ancestors;
        }

	public void setancestors(HashSet<Pheno> ancestors) {
		this.ancestors = ancestors;
	}

        public void setNonTCIndividualSize(int NonTCIndividualSize){
            this.NonTCIndividualSize = NonTCIndividualSize;
        }
        public int getNonTCIndividualSize(){
            return this.NonTCIndividualSize;
        }
}