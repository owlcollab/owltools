/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package owltools.phenolog;

import java.util.*;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;

/**
 *
 * @author beladia
 */
public class PhenoTransitiveClosure {
    private String obolink;
    private String prefix;
    private HashSet<Pheno> hsp;
    private HashMap<String, Pheno> hmp;
    private HashMap<String, IndividualPair> hmpair;

    public String getobolink(){
        return this.obolink;
    }
    public String getprefix(){
        return this.prefix;
    }
    public HashSet<Pheno> gethsp(){
        return this.hsp;
    }
    public HashMap<String, Pheno> gethmp(){
        return this.hmp;
    }
    public HashMap<String, IndividualPair> gethmpair(){
        return this.hmpair;
    }

    public void setobolink(String obolink){
        this.obolink = obolink;
    }
    public void setprefix(String prefix){
        this.prefix = prefix;
    }
    public void sethsp(HashSet<Pheno> hsp){
        this.hsp = hsp;
    }
    public void sethmp(HashMap<String, Pheno> hmp){
        this.hmp = hmp;
    }
    public void sethmpair(HashMap<String, IndividualPair> hmpair){
        this.hmpair = hmpair;
    }

    private HashSet<Pheno> ptc(String obolink, String prefix, HashSet<Pheno> hsp, HashMap<String, Pheno> hmp, HashMap<String, IndividualPair> hmpair) {
        HashSet<Pheno> aux = new HashSet<Pheno>();
        try {
            ParserWrapper pw = new ParserWrapper();
            OWLGraphWrapper owlg = pw.parseToOWLGraph(obolink);

            Set<OWLObject> ancs = null;

            HashSet<Pheno> chsp = new HashSet<Pheno>();
            HashSet<Pheno> tset = null;
            chsp.addAll(hsp);
            HashSet<Individual> tmpi = null;
            Pheno tmp = null;
            for (Pheno p : hsp) {
            	//System.err.println("p="+p);
                p.setancestors(new HashSet<Pheno>());
                tset = p.getancestors();
                ancs = owlg.getAncestorsReflexive(owlg.getOWLObjectByIdentifier(p.getId()));

                for (OWLObject c : ancs) {
                	if (!(c instanceof OWLClass))
                		continue;
                	//System.err.println("  c="+c);
                    if (owlg.getIdentifier(c).equals(p.getId()) && p.getLabel()==null)
                        p.setLabel(owlg.getLabel(c));
                    if (owlg.getIdentifier(c).contains(prefix) && !(owlg.getIdentifier(c).equals(p.getId()))) {
                        if (hmp.get(owlg.getIdentifier(c)) == null) {
                            tmpi = new HashSet<Individual>();
                            tmpi.addAll(p.getIndividuals());
                            tmp = new Pheno(owlg.getIdentifier(c), owlg.getLabel(c), tmpi, true);
                            chsp.add(tmp);
                            hmp.put(tmp.getId(), tmp);
                            tset.add(tmp);
                            p.setancestors(tset);
                        } else {
                            tmp = hmp.get(owlg.getIdentifier(c));
                            tmpi = (HashSet<Individual>) tmp.getIndividuals();
                            tmpi.addAll(p.getIndividuals());
                            tmp.setIndividuals(tmpi);
                            tset.add(tmp);
                            p.setancestors(tset);
                        }
                    }
                }
            }

            aux.addAll(chsp);

            chsp.clear();
            for (Pheno p : aux) {
                if (((p.getIndividuals().size() / hmpair.size()) <= 0.1) && (owlg.getAncestorsReflexive(owlg.getOWLObjectByIdentifier(p.getId())).size() > 1)) {
                    chsp.add(p);
                } 
            }
                        
        } catch (Exception e) {
        	e.printStackTrace();
            System.out.println("EXCEPTION IN OWL GRAPH:" + e);
        }

        return aux;
    }

    public HashSet<Pheno> performtransiviteclosure(){
        return ptc(this.obolink, this.prefix, this.hsp, this.hmp, this.hmpair);
    }
    public HashSet<Pheno> performtransiviteclosure(String obolink, String prefix, HashSet<Pheno> hsp, HashMap<String, Pheno> hmp, HashMap<String, IndividualPair> hmpair){
        return ptc(obolink, prefix, hsp, hmp, hmpair);
    }
}