/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package owltools.phenolog;

import java.util.*;


class DistanceCompare implements Comparator<Pheno>{
    public int compare(Pheno p1, Pheno p2){
        return (new Double(p1.getClosestDistance())).compareTo((new Double(p2.getClosestDistance())));
    }
}