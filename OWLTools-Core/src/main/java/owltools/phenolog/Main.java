package owltools.phenolog;

import java.io.*;
import java.util.*;
import org.apache.commons.math.distribution.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;


/**
 *
 * Author        : Neeral Beladia
 * Date Created  : September 5, 2010
 * Class Purpose : The Main Class is the kickoff Class consisting of Main() method
 * Methods       : Main()
 *                 printhash()
 * Program Logic : 1. Read fly_genepheno.tsv data
 *                 2. Read through HashSet of GeneIDs read from fly_GenePheno.txt and compare with
 *                    all the Gene-Phenotype associations read from HashSet
 *                 3. Read Mice Gene ID - Phenotype ID data
 *                 4. Read Mice Gene ID - Gene Label data in a HashMap
 *                 5. Read Mice Phenotype ID - Phenotype Label data in a HashMap
 *                 6. Read through HashSet of GeneID-Phenotype ID
 *                    Assign the Gene Label and Phenotype Label to the corresponding ID
 *                    Create one Individual at a time and collect it in the HashSet of Individuals.
 *                 7. Read Ortholog Fly Gene ID - Mice Gene ID data
 */
public class Main {

    public static Pheno calculate_overlap(Pheno p1, Pheno p2, HashMap<String, IndividualPair> hm) {
        HashSet<IndividualPair> ip = new HashSet<IndividualPair>();
        Pheno p = new Pheno();

        int overlap = 0;
        for (Individual g1 : p1.getIndividuals()) {
            if (p2.getIndividuals().contains(hm.get(g1.getId()).getMember2())) {
                overlap = overlap + 1;
                ip.add(hm.get(g1.getId()));
            }
        }

        p.setClosestOverlap(overlap);
        p.setClosestOverlapPairs(ip);

        return p;
    }

    public static HashSet<Individual> getpermutedgenes(int sampsz, int pop_size, ArrayList<Individual> ls_ind) {
        HashSet<Individual> hsg = new HashSet<Individual>();
        Random rand = new Random(System.currentTimeMillis());
        int newIndex;

        while (hsg.size() <= sampsz) {
            newIndex = rand.nextInt(pop_size);
            Individual ind = (Individual) ls_ind.get(newIndex);
            hsg.add(new Individual(ind.getId(), ind.getLabel(), ind.getOrthologs()));
        }
        return hsg;
    }


    
    public static void printresults(String fname, ArrayList<Pheno> rs_list, double cutoff, int flip) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(fname));
            out.write("Pheno1 ID(Label)\t Pheno2 ID(Label)\t p-Value\t Overlap\n");
            for (Pheno mp : rs_list) {
                if ((mp.getClosest() != null) && (mp.getClosestDistance() <= cutoff)) {
                    out.write(mp.getId() + " (" + mp.getLabel() + ") \t" + mp.getClosest().getId() + " (" + mp.getClosest().getLabel() + ") \t" + mp.getClosestDistance() + "\t" + mp.getClosestOverlap());

                    out.write("\n\nGene1 ID(Label)\tGene2 ID(Label)");
                    for (IndividualPair indpair : (HashSet<IndividualPair>)mp.getClosestOverlapPairs()) {
                        if (flip == 1) {
                            out.write("\n" + indpair.getMember2().getId() + " (" + indpair.getMember2().getLabel() + ") " + "\t");
                            out.write(indpair.getMember1().getId() + " (" + indpair.getMember1().getLabel() + ") ");
                        } else {
                            out.write("\n" + indpair.getMember1().getId() + " (" + indpair.getMember1().getLabel() + ") " + "\t");
                            out.write(indpair.getMember2().getId() + " (" + indpair.getMember2().getLabel() + ") ");
                        }
                    }
                    out.write("\n\n\n\n\n");
                }
            }
            out.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    /*
     * The main() method is the first method that gets called.
     * All relevant programming blocks are either implemented inline within the method
     * or through functionality offered through methods residing within other classes.
     */
    public static void main(String[] args) {
        long mainstart = System.currentTimeMillis();
        long mainend;
        double timespent;

        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        Date startdate = new Date();


        HashSet<GenePheno> gpset = new HashSet<GenePheno>();
        HashMap<String, Individual> hm_ind;
        HashSet<IndividualPair> hs_indpair;
        String sp1_gl, sp2_gl, sp1_gp, sp2_gp, ortho, sp1_obo, sp2_obo, sp1_ph_pfx, sp2_ph_pfx, sp1_gene_pfx, sp2_gene_pfx, sp1_name, sp2_name, out_path;
        sp1_gl = null;
        sp2_gl = null;
        sp1_gp = null;
        sp2_gp = null;
        ortho = null;
        sp1_obo = null;
        sp2_obo = null;
        sp1_ph_pfx = null;
        sp2_ph_pfx = null;
        sp1_gene_pfx = null;
        sp2_gene_pfx = null;
        sp1_name = null;
        sp2_name = null;
        out_path = null;

        try {
            Properties prop = new Properties();
            String fileName = args[0];
            InputStream is = new FileInputStream(fileName);

            prop.load(is);

            sp1_gl = prop.getProperty("sp1_gene_label_file");
            sp2_gl = prop.getProperty("sp2_gene_label_file");
            sp1_gp = prop.getProperty("sp1_gene_pheno_file");
            sp2_gp = prop.getProperty("sp2_gene_pheno_file");
            ortho = prop.getProperty("ortho_file");
            sp1_obo = prop.getProperty("sp1_obo_file");
            sp2_obo = prop.getProperty("sp2_obo_file");
            sp1_gene_pfx = prop.getProperty("sp1_gene_prefix");
            sp2_gene_pfx = prop.getProperty("sp2_gene_prefix");
            sp1_ph_pfx = prop.getProperty("sp1_pheno_prefix");
            sp2_ph_pfx = prop.getProperty("sp2_pheno_prefix");
            sp1_name = prop.getProperty("sp1_name");
            sp2_name = prop.getProperty("sp2_name");
            out_path = prop.getProperty("output_path");

            System.out.println(prop.getProperty("sp1_gene_label_file"));
            System.out.println(prop.getProperty("sp2_gene_label_file"));
            System.out.println(prop.getProperty("sp1_gene_pheno_file"));
            System.out.println(prop.getProperty("sp2_gene_pheno_file"));
            System.out.println(prop.getProperty("ortho_file"));
            System.out.println(prop.getProperty("sp1_obo_file"));
            System.out.println(prop.getProperty("sp2_obo_file"));
            System.out.println(prop.getProperty("sp1_gene_prefix"));
            System.out.println(prop.getProperty("sp2_gene_prefix"));
            System.out.println(prop.getProperty("sp1_pheno_prefix"));
            System.out.println(prop.getProperty("sp2_pheno_prefix"));
            System.out.println(prop.getProperty("sp1_name"));
            System.out.println(prop.getProperty("sp2_name"));
            System.out.println(prop.getProperty("output_path"));
        } catch (Exception e) {
        }


        //STEP 1: Read Species I Gene ID - Pheno ID data
        try {
            File myFile = new File(sp1_gp);
            FileReader fileReader = new FileReader(myFile);
            BufferedReader reader = new BufferedReader(fileReader);
            String[] result;
            String line = null;


            while ((line = reader.readLine()) != null) {
                result = line.split("\t");

                // result[0] : Gene ID
                // result[1] : Phenotype ID
                if (result.length == 2) {
                    if ((result[0] != null) && (result[1] != null)) {
                        //System.out.println("1: "+result[0]+", 2: "+result[1]+", prefix: "+sp1_ph_pfx);
                        if (result[1].contains(sp1_ph_pfx)) {
                            gpset.add(new GenePheno(result[0], result[1]));
                        }
                    }
                }
            }
            reader.close();

        } catch (FileNotFoundException e) {
            System.out.println("Could not open Species - I Gene ID - Pheno ID File");
        } catch (Exception ex) {
            ex.printStackTrace();
        }


        System.out.println("Species I (Gene-Pheno) file : # of obs read: " + gpset.size());


        // STEP 2: Read through HashSet of GeneIDs read from Species - I (Gene - Pheno) and compare with
        //         all the Gene-Phenotype associations read from HashSet

        HashSet<Attribute> gp_at;
        HashSet<Individual> ind1;

        ind1 = null;
        hm_ind = null;

        for (GenePheno tmp2_gp : gpset) {
            Individual tmp2_ind;
            if (ind1 == null) {
                ind1 = new HashSet<Individual>();
                hm_ind = new HashMap<String, Individual>();
            }

            if (hm_ind.get(tmp2_gp.getid()) != null) {
                tmp2_ind = (Individual) hm_ind.get(tmp2_gp.getid());
                tmp2_ind.getAttributes().add(new Attribute(tmp2_gp.getphenoid()));
            } else {
                gp_at = new HashSet<Attribute>();
                gp_at.add(new Attribute(tmp2_gp.getphenoid()));
                tmp2_ind = new Individual(tmp2_gp.getid(), gp_at);
                ind1.add(tmp2_ind);
                hm_ind.put(tmp2_gp.getid(), tmp2_ind);
            }
        }// End of Gene-Phenotype loop.




        //STEP 3: Read Species - II Gene ID - Phenotype ID data
        gpset.clear();
        try {
            File myFile = new File(sp2_gp);
            FileReader fileReader = new FileReader(myFile);
            BufferedReader reader = new BufferedReader(fileReader);

            String[] result;

            String line = null;
            while ((line = reader.readLine()) != null) {
                result = line.split("\t");

                if (result.length == 2) {
                    // result[0] : Gene ID
                    // result[1] : Phenotype ID
                    if ((result[0] != null) && (result[1] != null)) {
                        if (result[1].contains(sp2_ph_pfx)) {
                            gpset.add(new GenePheno(result[0], result[1]));
                        }
                    }
                }
            }
            reader.close();
        } catch (FileNotFoundException e) {
            System.out.println("Could not open Species II Gene ID - Pheno ID File");
        } catch (Exception ex) {
            ex.printStackTrace();
        }


        System.out.println("Species II (Gene-Pheno) file : # of obs read: " + gpset.size());


        // STEP4: Read through HashSet of GeneID - Phenotype ID
        //        Assign the Gene Label and Phenotype Label to the corresponding ID
        //        Create one Individual at a time and collect it in the HashSet of Individuals.

        HashSet<Individual> ind2;

        ind2 = null;

        for (GenePheno tmp2_gp : gpset) {
            Individual tmp2_ind;
            if (ind2 == null) {
                ind2 = new HashSet<Individual>();
            }

            if (hm_ind.get(tmp2_gp.getid()) != null) {
                tmp2_ind = (Individual) hm_ind.get(tmp2_gp.getid());
                tmp2_ind.getAttributes().add(new Attribute(tmp2_gp.getphenoid()));
            } else {
                gp_at = new HashSet<Attribute>();
                gp_at.add(new Attribute(tmp2_gp.getphenoid()));
                tmp2_ind = new Individual(tmp2_gp.getid(), gp_at);
                ind2.add(tmp2_ind);
                hm_ind.put(tmp2_gp.getid(), tmp2_ind);
            }
        }// End of Gene-Phenotype loop.



        //STEP 5: Read Species - I Gene ID - Gene Label data
        try {
            File myFile = new File(sp1_gl);
            FileReader fileReader = new FileReader(myFile);
            BufferedReader reader = new BufferedReader(fileReader);

            String[] result;
            Individual tmp = null;

            String line = null;
            while ((line = reader.readLine()) != null) {
                result = line.split("\t");

                if (result.length == 2) {
                    // result[0] : Gene ID
                    // result[1] : Phenotype ID
                    if ((result[0] != null) && (result[1] != null)) {
                        if (hm_ind.get(result[0]) != null) {
                            tmp = hm_ind.get(result[0]);
                            tmp.setLabel(result[1]);
                        }
                    }
                }
            }
            reader.close();
        } catch (FileNotFoundException e) {
            System.out.println("Could not open Species I Gene ID - Gene Label File");
        } catch (Exception ex) {
            ex.printStackTrace();
        }


        //STEP 6: Read Species - II Gene ID - Gene Label data
        try {
            File myFile = new File(sp2_gl);
            FileReader fileReader = new FileReader(myFile);
            BufferedReader reader = new BufferedReader(fileReader);

            String[] result;
            Individual tmp = null;

            String line = null;
            while ((line = reader.readLine()) != null) {
                result = line.split("\t");

                if (result.length == 2) {
                    // result[0] : Gene ID
                    // result[1] : Phenotype ID
                    if ((result[0] != null) && (result[1] != null)) {
                        if (hm_ind.get(result[0]) != null) {
                            tmp = hm_ind.get(result[0]);
                            tmp.setLabel(result[1]);
                        }
                    }
                }
            }
            reader.close();
        } catch (FileNotFoundException e) {
            System.out.println("Could not open Species II Gene ID - Gene Label File");
        } catch (Exception ex) {
            ex.printStackTrace();
        }


        System.out.println("Size of HashMap: " + hm_ind.size());

        hs_indpair = null;
        //STEP 7: Read Ortholog Species I Gene ID - Species II Gene ID data
        try {
            File myFile = new File(ortho);
            FileReader fileReader = new FileReader(myFile);
            BufferedReader reader = new BufferedReader(fileReader);
            IndividualPair rd_indpair;

            String geneid1;
            String geneid2;
            String[] result;

            String line = null;
            rd_indpair = null;
            while ((line = reader.readLine()) != null) {
                result = line.split("\t");

                geneid1 = null;
                geneid2 = null;

                if (result.length == 2 && result[1].contains(sp2_gene_pfx)) {
                    geneid1 = result[0];
                    geneid2 = result[1];
                }

                if (hs_indpair == null) {
                    hs_indpair = new HashSet<IndividualPair>();
                }

                if ((geneid1 != null) && (geneid2 != null)) {
                    if ((hm_ind.get(geneid1) != null) && (hm_ind.get(geneid2) != null)) {
                        rd_indpair = new IndividualPair(hm_ind.get(geneid1), hm_ind.get(geneid2));
                        rd_indpair.getMember1().setOrthologs(rd_indpair.getMember1().getOrthologs() + 1);
                        rd_indpair.getMember2().setOrthologs(rd_indpair.getMember2().getOrthologs() + 1);
                        hs_indpair.add(rd_indpair);
                    } else if (hm_ind.get(geneid1) != null) {
                        rd_indpair = new IndividualPair(hm_ind.get(geneid1), new Individual(geneid2));
                        rd_indpair.getMember1().setOrthologs(rd_indpair.getMember1().getOrthologs() + 1);
                        rd_indpair.getMember2().setOrthologs(rd_indpair.getMember2().getOrthologs() + 1);
                        hs_indpair.add(rd_indpair);
                    } else if (hm_ind.get(geneid2) != null) {
                        rd_indpair = new IndividualPair(new Individual(geneid1), hm_ind.get(geneid2));
                        rd_indpair.getMember1().setOrthologs(rd_indpair.getMember1().getOrthologs() + 1);
                        rd_indpair.getMember2().setOrthologs(rd_indpair.getMember2().getOrthologs() + 1);
                        hs_indpair.add(rd_indpair);
                    }
                }
            }
            reader.close();

        } catch (FileNotFoundException e) {
            System.out.println("Could not open Orthologs File");
        } catch (Exception ex) {
            ex.printStackTrace();
        }



        // Loop through Ortholog pairs and create a new hashset of 1:1 individual pairs
        HashMap<String, IndividualPair> hm_indpair1 = new HashMap<String, IndividualPair>();
        HashSet<IndividualPair> hs_indpair1 = new HashSet<IndividualPair>();
        for (IndividualPair indpair : hs_indpair) {
            if ((indpair.getMember1().getOrthologs() == 1) && (indpair.getMember2().getOrthologs() == 1)
                    && (indpair.getMember1().getAttributes().size() > 0) && (indpair.getMember2().getAttributes().size() > 0)) {
                hs_indpair1.add(indpair);
                hm_indpair1.put(indpair.getMember1().getId(), indpair);

            }
        }

        System.out.println("Size of Original Orthologs: " + hs_indpair.size());
        System.out.println("Size of New 1:1 Orthologs: " + hs_indpair1.size());


        // Calculate distance between each phenotype in Species 1 vs. each phenotype in Species 2
        // To do this, create a hashset for Species I and II, Phenotype -> Set of associated Individuals

        HashSet<Pheno> ph1 = null; // phenotypes in set 1
        HashSet<Pheno> ph2 = null; // phenotypes in set 2
        HashMap<String, Pheno> hm1 = new HashMap<String, Pheno>();
        HashMap<String, Pheno> hm2 = new HashMap<String, Pheno>();
        HashSet<Individual> hs_ind = null;
        Individual g;
        Pheno ph = null;

        int mval = 0;
        int nval = 0;
        int bigN = 0;
        int overlap = 0;

        long start, end, total;
        Individual mem1;
        Individual mem2;

        hs_ind = null;
        start = System.currentTimeMillis();
        for (IndividualPair itmp2 : hs_indpair1) {
            mem1 = itmp2.getMember1();
            mem2 = itmp2.getMember2();

            // Species I
            for (Attribute at : mem1.getAttributes()) {
                if (ph1 == null) {
                    ph1 = new HashSet<Pheno>();
                }
                //if Phenotype Set does not consist of the current phenotype
                //create a new <Pheno> Instance and add to the hashset.
                if (hm1.get(at.getId()) == null) {
                    hs_ind = new HashSet<Individual>();
                    hs_ind.add(new Individual(mem1.getId(), mem1.getLabel(), mem1.getOrthologs()));
                    ph = new Pheno(at.getId(), at.getLabel(), hs_ind, false);
                    ph1.add(ph);
                    hm1.put(at.getId(), ph);
                } //else if phonotype id already exists in the set
                else {
                    ph = hm1.get(at.getId());
                    hs_ind = (HashSet<Individual>) ph.getIndividuals();
                    g = new Individual(mem1.getId(), mem1.getLabel(), mem1.getOrthologs());
                    hs_ind.add(g);
                    ph.setIndividuals(hs_ind);
                }

            }//end of loop through Species-I Individual phenotypes


            // Species II
            for (Attribute at : mem2.getAttributes()) {
                if (ph2 == null) {
                    ph2 = new HashSet<Pheno>();
                }
                //if Phenotype Set does not consist of the current phenotype
                //create a new <Pheno> Instance and add to the hashset.
                if (hm2.get(at.getId()) == null) {
                    hs_ind = new HashSet<Individual>();
                    hs_ind.add(new Individual(mem2.getId(), mem2.getLabel(), mem2.getOrthologs()));
                    ph = new Pheno(at.getId(), at.getLabel(), hs_ind, false);
                    ph2.add(ph);
                    hm2.put(at.getId(), ph);
                } //else if phonotype id already exists in the set
                else {
                    ph = hm2.get(at.getId());
                    hs_ind = (HashSet<Individual>) ph.getIndividuals();
                    g = new Individual(mem2.getId(), mem2.getLabel(), mem2.getOrthologs());
                    hs_ind.add(g);
                    ph.setIndividuals(hs_ind);
                }

            }//end of loop through Species-II Individual phenotypes

        }//end of loop through Individual-Pairs

        end = System.currentTimeMillis();
        total = end - start;

        System.out.println("Total Time Taken: " + total + " SP1 size=" + ph1.size() + " , SP2 size=" + ph2.size());
        System.out.println("Total orthologs: " + hs_indpair1.size());

        PhenoTransitiveClosure ptc = new PhenoTransitiveClosure();
        HashSet<Pheno> oldph1 = new HashSet<Pheno>();
        HashSet<Pheno> oldph2 = new HashSet<Pheno>();

        /* Backup Pheno hashsets for each species to be used in bootstrap later on */
        oldph1.addAll(ph1);
        oldph2.addAll(ph2);

        for (Pheno t_ph1 : ph1) {
            t_ph1.setNonTCIndividualSize(t_ph1.getIndividuals().size());
        }
        for (Pheno t_ph2 : ph2) {
            t_ph2.setNonTCIndividualSize(t_ph2.getIndividuals().size());
        }
        ph1 = ptc.performtransiviteclosure(sp1_obo, sp1_ph_pfx, ph1, hm1, hm_indpair1);
        ph2 = ptc.performtransiviteclosure(sp2_obo, sp2_ph_pfx, ph2, hm2, hm_indpair1);

        System.out.println("Done with PH2 Graph");

        System.out.println("SP1 size=" + ph1.size() + " , SP2 size=" + ph2.size());
        System.out.println("Total orthologs: " + hs_indpair1.size());


        //For each Phenotype in Species I
        //  For each Phenotype in Species II
        //    Calculate distance using hypergeometric probability

        double pvalue = 0;

        HypergeometricDistributionImpl hg = new HypergeometricDistributionImpl(100, 20, 10);
        HashSet<IndividualPair> clpair = null;



        for (Pheno t_ph1 : ph1) {
            t_ph1.setClosest(null);
            t_ph1.setClosestDistance(1);
            t_ph1.setClosestOverlap(0);
            t_ph1.setClosestOverlapPairs(null);
        }

        for (Pheno t_ph2 : ph2) {
            t_ph2.setClosest(null);
            t_ph2.setClosestDistance(1);
            t_ph2.setClosestOverlap(0);
            t_ph2.setClosestOverlapPairs(null);
        }

        int i;
        Pheno p = null;
        start = System.currentTimeMillis();
        for (Pheno t_ph1 : ph1) {
            for (Pheno t_ph2 : ph2) {
                overlap = 0;
                clpair = null;

                if ((t_ph1.getIndividuals() != null) && (t_ph2.getIndividuals() != null)) {
                    p = calculate_overlap(t_ph1, t_ph2, hm_indpair1);
                    overlap = p.getClosestOverlap();
                    clpair = p.getClosestOverlapPairs();
                } else {
                    overlap = 0;
                }

                if (overlap > 1) {
                    mval = t_ph1.getIndividuals().size();
                    nval = t_ph2.getIndividuals().size();

                    bigN = hs_indpair1.size();

                    hg.setPopulationSize(bigN);
                    hg.setNumberOfSuccesses(mval);
                    hg.setSampleSize(nval);


                    // Calculate HyperGeometric probability between t_ph1 and t_ph2
                    pvalue = 0;
                    for (i = overlap; i <= Math.min(mval, nval); i++) {
                        pvalue += hg.probability(overlap);
                    }
                    //pvalue = hg.probability(overlap);

                    if (pvalue < t_ph1.getClosestDistance()) {
                        t_ph1.setClosest(t_ph2);
                        t_ph1.setClosestDistance(pvalue);
                        t_ph1.setClosestOverlap(overlap);
                        t_ph1.setClosestOverlapPairs(clpair);
                    }
                    if (pvalue < t_ph2.getClosestDistance()) {
                        t_ph2.setClosest(t_ph1);
                        t_ph2.setClosestDistance(pvalue);
                        t_ph2.setClosestOverlap(overlap);
                        t_ph2.setClosestOverlapPairs(clpair);
                    }
                }
            }
        }

        end = System.currentTimeMillis();
        total = end - start;
        System.out.println("2) Total Time Taken: " + total);

        HashSet<Pheno> rs_ph1 = new HashSet<Pheno>();
        HashSet<IndividualPair> ip_tmp = null;
        Pheno p_tmp = null;
        HashMap<String, Pheno> hm_cp = new HashMap<String, Pheno>();
        HashMap<String, Individual> hm_cind = new HashMap<String, Individual>();
        HashMap<String, IndividualPair> hm_cindpair = new HashMap<String, IndividualPair>();
        IndividualPair cindpair = null;
        Individual cind1 = null;
        Individual cind2 = null;
        Pheno cp = null;

        for (Pheno p1 : ph1) {
            if (hm_cp.get(p1.getId())==null){
                p_tmp = new Pheno(p1.getId(), p1.getLabel());
                hm_cp.put(p1.getId(), p_tmp);
            } else {
                p_tmp = hm_cp.get(p1.getId());
            }
            p_tmp.setClosestDistance(p1.getClosestDistance());
            p_tmp.setClosestOverlap(p1.getClosestOverlap());
            
            if (hm_cp.get(p1.getId())==null)
                hm_cp.put(p1.getId(), p1);

            if (p1.getClosestOverlapPairs() != null) {
                ip_tmp = new HashSet<IndividualPair>();
                for (IndividualPair ip : p1.getClosestOverlapPairs()) {
                    if (hm_cindpair.get(ip.getMember1().getId()+"*"+ip.getMember1().getId()) != null)
                        ip_tmp.add(hm_cindpair.get(ip.getMember1().getId()+"*"+ip.getMember2().getId()));
                    else {
                        // First Member of Individual Pair
                        if (hm_cind.get(ip.getMember1().getId()) != null)
                            cind1 = hm_cind.get(ip.getMember1().getId());
                        else {
                            cind1 = new Individual(ip.getMember1().getId(), ip.getMember1().getLabel());
                            hm_cind.put(ip.getMember1().getId(), cind1);
                        }
                        // Second Member of Individual Pair
                        if (hm_cind.get(ip.getMember2().getId()) != null)
                            cind2 = hm_cind.get(ip.getMember2().getId());
                        else {
                            cind2 = new Individual(ip.getMember2().getId(), ip.getMember2().getLabel());
                            hm_cind.put(ip.getMember2().getId(), cind2);
                        }

                        // Add Individual Pair
                        cindpair = new IndividualPair(cind1, cind2);
                        hm_cindpair.put(cind1.getId()+"*"+cind2.getId(), cindpair);
                        ip_tmp.add(cindpair);                        
                    }
                }
                p_tmp.setClosestOverlapPairs(ip_tmp);
            }

            if (p1.getClosest() != null) {
                if (hm_cp.get(p1.getClosest().getId()) != null)
                    p_tmp.setClosest(hm_cp.get(p1.getClosest().getId()));
                else {
                    cp = new Pheno(p1.getClosest().getId(), p1.getClosest().getLabel());
                    hm_cp.put(p1.getClosest().getId(), cp);
                    p_tmp.setClosest(cp);
                }                
            }
            rs_ph1.add(p_tmp);
        }


        HashSet<Pheno> rs_ph2 = new HashSet<Pheno>();
        ip_tmp = null;
        p_tmp = null;
        for (Pheno p2 : ph2) {
            if (hm_cp.get(p2.getId())==null){
                p_tmp = new Pheno(p2.getId(), p2.getLabel());
                hm_cp.put(p2.getId(), p_tmp);
            } else {
                p_tmp = hm_cp.get(p2.getId());
            }
            p_tmp.setClosestDistance(p2.getClosestDistance());
            p_tmp.setClosestOverlap(p2.getClosestOverlap());


            if (p2.getClosestOverlapPairs() != null) {
                ip_tmp = new HashSet<IndividualPair>();
                for (IndividualPair ip : p2.getClosestOverlapPairs()) {
                    if (hm_cindpair.get(ip.getMember1().getId()+"*"+ip.getMember1().getId()) != null)
                        ip_tmp.add(hm_cindpair.get(ip.getMember1().getId()+"*"+ip.getMember2().getId()));
                    else {
                        // First Member of Individual Pair
                        if (hm_cind.get(ip.getMember1().getId()) != null)
                            cind1 = hm_cind.get(ip.getMember1().getId());
                        else {
                            cind1 = new Individual(ip.getMember1().getId(), ip.getMember1().getLabel());
                            hm_cind.put(ip.getMember1().getId(), cind1);
                        }
                        // Second Member of Individual Pair
                        if (hm_cind.get(ip.getMember2().getId()) != null)
                            cind2 = hm_cind.get(ip.getMember2().getId());
                        else {
                            cind2 = new Individual(ip.getMember2().getId(), ip.getMember2().getLabel());
                            hm_cind.put(ip.getMember2().getId(), cind2);
                        }

                        // Add Individual Pair
                        cindpair = new IndividualPair(cind1, cind2);
                        hm_cindpair.put(cind1.getId()+"*"+cind2.getId(), cindpair);
                        ip_tmp.add(cindpair);
                    }
                }
                p_tmp.setClosestOverlapPairs(ip_tmp);
            }

            if (p2.getClosest() != null) {
                if (hm_cp.get(p2.getClosest().getId()) != null)
                    p_tmp.setClosest(hm_cp.get(p2.getClosest().getId()));
                else {
                    cp = new Pheno(p2.getClosest().getId(), p2.getClosest().getLabel());
                    hm_cp.put(p2.getClosest().getId(), cp);
                    p_tmp.setClosest(cp);
                }
            }
            rs_ph2.add(p_tmp);
        }


        ArrayList<Pheno> overall_list1 = new ArrayList<Pheno>(rs_ph1);
        DistanceCompare distancecompare = new DistanceCompare();
        Collections.sort(overall_list1, distancecompare);

        printresults(out_path.concat("/overall_sp1.xls"), overall_list1, 1, 0);

        ArrayList<Pheno> overall_list2 = new ArrayList<Pheno>(rs_ph2);
        distancecompare = new DistanceCompare();
        Collections.sort(overall_list2, distancecompare);

        printresults(out_path.concat("/overall_sp2.xls"), overall_list2, 1, 1);


        /* CODE FOR BOOTSTRAPING */
        /* Get list of distinct genes from Individual pair : ls_ind1 ls_ind2 for Species1 and Species2 respectively */
        ind1 = new HashSet<Individual>();
        ind2 = new HashSet<Individual>();
        int itersz = 1000;

        ArrayList<Double> phenolist1 = new ArrayList<Double>();
        ArrayList<Double> phenolist2 = new ArrayList<Double>();
        double[] cutoff1 = new double[itersz];
        double[] cutoff2 = new double[itersz];

        for (IndividualPair ip1 : hs_indpair1) {
            ind1.add(ip1.getMember1());
            ind2.add(ip1.getMember2());
        }

        ArrayList<Individual> ls_ind1 = new ArrayList<Individual>(ind1);
        ArrayList<Individual> ls_ind2 = new ArrayList<Individual>(ind2);
        HashSet<Individual> hsg;

        start = System.currentTimeMillis();
        for (int iter = 0; iter <= itersz - 1; iter++) {
            

            phenolist1.clear();
            phenolist2.clear();
            //For each Phenotype in Species I
            //  For each Phenotype in Species II
            //    Calculate distance using hypergeometric probability

            hsg = null;
            pvalue = 0;


            for (Pheno t_ph1 : ph1) {
                t_ph1.getIndividuals().clear();
            }
            for (Pheno t_ph2 : ph2) {
                t_ph2.getIndividuals().clear();
            }

            for (Pheno t_ph1 : ph1) {
                t_ph1.setClosest(null);
                t_ph1.setClosestDistance(1);
                t_ph1.setClosestOverlap(0);
                t_ph1.setClosestOverlapPairs(null);

                // Permute Genese for Species I - Phenotypes
                if (t_ph1.getIndividuals() != null && t_ph1.getisFromTC() == false) {
                    hsg = (HashSet<Individual>) getpermutedgenes(t_ph1.getNonTCIndividualSize(), ls_ind1.size(), ls_ind1);
                    t_ph1.setIndividuals(hsg);

                    for (Pheno anc : t_ph1.getancestors()) {
                        hsg = anc.getIndividuals();
                        hsg.addAll(t_ph1.getIndividuals());
                        anc.setIndividuals(hsg);
                    }
                }
            }

            for (Pheno t_ph2 : ph2) {
                t_ph2.setClosest(null);
                t_ph2.setClosestDistance(1);
                t_ph2.setClosestOverlap(0);
                t_ph2.setClosestOverlapPairs(null);

                // Permute Genese for Species II - Phenotypes
                if (t_ph2.getIndividuals() != null && t_ph2.getisFromTC() == false) {
                    hsg = (HashSet<Individual>) getpermutedgenes(t_ph2.getNonTCIndividualSize(), ls_ind2.size(), ls_ind2);
                    t_ph2.setIndividuals(hsg);

                    for (Pheno anc : t_ph2.getancestors()) {
                        hsg = anc.getIndividuals();
                        hsg.addAll(t_ph2.getIndividuals());
                        anc.setIndividuals(hsg);
                    }
                }
            }

            for (Pheno t_ph1 : ph1) {

                for (Pheno t_ph2 : ph2) {
                    overlap = 0;
                    clpair = null;

                    if ((t_ph1.getIndividuals() != null) && (t_ph2.getIndividuals() != null)) {
                        p = calculate_overlap(t_ph1, t_ph2, hm_indpair1);
                        overlap = p.getClosestOverlap();
                        clpair = p.getClosestOverlapPairs();
                    } else {
                        overlap = 0;
                    }

                    if (overlap > 1) {
                        mval = t_ph1.getIndividuals().size();
                        nval = t_ph2.getIndividuals().size();

                        bigN = hs_indpair1.size();

                        hg.setPopulationSize(bigN);
                        hg.setNumberOfSuccesses(mval);
                        hg.setSampleSize(nval);


                        // Calculate HyperGeometric probability between t_ph1 and t_ph2
                        pvalue = 0;
                        for (i = overlap; i <= Math.min(mval, nval); i++) {
                            pvalue += hg.probability(overlap);
                        }
                        //pvalue = hg.probability(overlap);

                        if (pvalue < t_ph1.getClosestDistance()) {
                            t_ph1.setClosest(t_ph2);
                            t_ph1.setClosestDistance(pvalue);
                            t_ph1.setClosestOverlap(overlap);
                            t_ph1.setClosestOverlapPairs(clpair);
                        }
                        if (pvalue < t_ph2.getClosestDistance()) {
                            t_ph2.setClosest(t_ph1);
                            t_ph2.setClosestDistance(pvalue);
                            t_ph2.setClosestOverlap(overlap);
                            t_ph2.setClosestOverlapPairs(clpair);
                        }
                    }
                }

                if (t_ph1.getClosestOverlap() > 1) {
                    phenolist1.add(t_ph1.getClosestDistance());
                }
            }// end of looping through each phenotype of species 1

            for (Pheno t_ph2 : ph2) {
                if (t_ph2.getClosestOverlap() > 1) {
                    phenolist2.add(t_ph2.getClosestDistance());
                }
            }

            Collections.sort(phenolist1);
            Collections.sort(phenolist2);
            
            System.out.println("iter="+iter);

            cutoff1[iter] = phenolist1.get((int) Math.ceil(phenolist1.size() * 0.05));
            cutoff2[iter] = phenolist2.get((int) Math.ceil(phenolist2.size() * 0.05));

            if (iter % 100 == 0) {
                end = System.currentTimeMillis();
                total = end - start;
                System.out.println("Iteration " + iter + ") Total Time Taken in minutes: " + total / (1000*60));
                start = System.currentTimeMillis();
            }

            //System.out.println("Cutoff1["+ iter + "] = "+cutoff1[iter]);
            //System.out.println("Cutoff2["+ iter + "] = "+cutoff2[iter]);

        } // end of iterator for 1 to 1000 bootstrap samples

        double avgcutoff1, avgcutoff2;

        avgcutoff1 = 0;
        avgcutoff2 = 0;
        for (int iter = 0; iter <= itersz - 1; iter++) {
            avgcutoff1 = avgcutoff1 + cutoff1[iter];
            avgcutoff2 = avgcutoff2 + cutoff2[iter];
        }
        avgcutoff1 = avgcutoff1 / itersz;
        avgcutoff2 = avgcutoff2 / itersz;

        System.out.println("Bootstrap Cutoff p-Value for Species 1 : " + avgcutoff1);
        System.out.println("Bootstrap Cutoff p-Value for Species 2 : " + avgcutoff2);



        overall_list1 = new ArrayList<Pheno>(rs_ph1);
        distancecompare = new DistanceCompare();
        Collections.sort(overall_list1, distancecompare);

        overall_list2 = new ArrayList<Pheno>(rs_ph2);
        distancecompare = new DistanceCompare();
        Collections.sort(overall_list2, distancecompare);

        ArrayList<Pheno> reciprocal_list = new ArrayList<Pheno>();
        printresults(out_path.concat("/bootstrap_sp1.xls"), overall_list1, avgcutoff1, 0);
        for (Pheno mp : overall_list1) {
            //If (mp, mp.closest()) is a reciprocal hit pair, add mp to reciprocal best hit list
            p = mp.getClosest();
            if (p !=null)
                p = p.getClosest();
            if (p != null) {
                if (mp.getId().equals(p.getId())) {
                    reciprocal_list.add(mp);
                }
            }
        }
        printresults(out_path.concat("/bootstrap_sp2.xls"), overall_list2, avgcutoff2, 1);
        printresults(out_path.concat("/reciprocal_pairs.xls"), reciprocal_list, avgcutoff1, 0);

        /*
        try {
            String host = "smtp.gmail.com";
            String from = "phenolog.dispatch@gmail.com";
            String pass = "berkeleybop";
            Properties props = System.getProperties();
            props.put("mail.smtp.starttls.enable", "true"); 
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.user", from);
            props.put("mail.smtp.password", pass);
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.auth", "true");

            String[] to = {"beladia@gmail.com", "beladia@stanford.edu"}; 

            Session session = Session.getDefaultInstance(props, null);
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));

            InternetAddress[] toAddress = new InternetAddress[to.length];

            // To get the array of addresses
            for (i = 0; i < to.length; i++) { 
                toAddress[i] = new InternetAddress(to[i]);
            }
            System.out.println(Message.RecipientType.TO);

            for (i = 0; i < toAddress.length; i++) { 
                message.addRecipient(Message.RecipientType.TO, toAddress[i]);
            }

            Date enddate = new Date();

            message.setSubject("Job Done !");
            mainend = System.currentTimeMillis();
            timespent = (mainend - mainstart) / (1000 * 60);
            message.setText("Hi, \n\nThe scheduled phenologs job perfomed on " + sp1_name + "(Species - I) and " + sp2_name + "(Species - II), intitated on "
                    + dateFormat.format(startdate) + " was completed on "+ dateFormat.format(enddate) +" and the relevant files have been copied to Dropbox. The total execution time was approximately "
                    + Math.round(timespent) + " minute(s). \n\n"
                    + "The Bootstrap Cutoffs were as follows:\n"
                    + "  Species - I(" + sp1_name + ") : " + avgcutoff1 + "\n"
                    + "  Species - II(" + sp2_name + ") : " + avgcutoff2 + "\n\n "
                    + "File Description:\n"
                    + "  1. overall_sp1: List of phenologs from Species - II associated with Species - I phenotypes\n"
                    + "  2. overall_sp2: List of phenologs from Species - I associated with Species - II phenotypes\n"
                    + "  3. bootstrap_sp1: List of phenologs from Species - II associated with Species - I phenotypes, with bootstrap cutoff applied on p-Value\n"
                    + "  4. bootstrap_sp2: List of phenologs from Species - I associated with Species - II phenotypes, with bootstrap cutoff applied on p-Value\n"
                    + "  5. reciprocal_pairs: List of reciprocal best hit phenologs with bootstrap cutoff applied on p-Value\n"
                    + "\n\n* reciprocal best hit phenolog pair = (p1, p2) is called a reciprocal best hit phenolog pair if p2 is the best p-Value match for p1; and p1 is the best p-Value match for p2"
                    + "\n\nThanks !");
            Transport transport = session.getTransport("smtp");
            transport.connect(host, from, pass);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();

        } catch (Exception e) {
        }
        */
    } // end of main method
} // end of main class