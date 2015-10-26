/* 
 * 
 * Copyright (c) 2010, Regents of the University of California 
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Neither the name of the Lawrence Berkeley National Lab nor the names of its contributors may be used to endorse 
 * or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package owltools.gaf.species;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import owltools.gaf.io.ResourceLoader;

/**
 * Used for reading previous or default user settings from property file and storing current user settings
 */

public class TaxonFinder {

	public final static String TAXON_PREFIX = "taxon:";
	/*
         * Get the NCBI taxon ID from their FTP-ed file dump
         */
	private static Map<String, Species> taxa2IDs;
	private static Map<String, Species> IDs2taxa;

	private static final String NCBI_TAXA = "ncbi_taxa_ids.txt";
	private static final String UNIPROT_TAXA = "speclist.txt";
	
	private static final Logger log = Logger.getLogger(TaxonFinder.class);

	public static String getTaxonID(String species_name) {
		if (taxa2IDs == null) {
			loadTaxaMapping();
		}
		Species taxon = null;
		if (species_name != null && species_name.length() > 0) {
			if (!species_name.equals("root"))
				species_name = species_name.substring(0, 1).toUpperCase() + species_name.substring(1);
			taxon = taxa2IDs.get(speciesNameHack(species_name));
			if (taxon == null) {
				taxon = taxa2IDs.get(species_name);
			}
			if (taxon == null) {
				taxon = taxa2IDs.get(species_name.toLowerCase());
			}
		}
		if (taxon != null)
			return TAXON_PREFIX+taxon.getNcbi_taxon_id();
		else
			return  "1";
	}

	public static String getSpecies(String taxon_id) {
		Species taxon = IDs2taxa.get(taxon_id);
		if (taxon != null)
			return taxon.getLabel();
		else if (taxon_id.indexOf(':') >= 0) {
			taxon_id = taxon_id.substring(taxon_id.indexOf(':') + 1);
			taxon = IDs2taxa.get(taxon_id);
		}
		if (taxon != null)
			return taxon.getLabel();
		else
			return "";
	}

	public static String getCode(String taxon_id) {
		if (taxon_id.startsWith(TAXON_PREFIX)) {
			taxon_id = taxon_id.substring(TAXON_PREFIX.length());
		}
		Species taxon = IDs2taxa.get(taxon_id);
		if (taxon != null)
			return taxon.getFive_code();
		else {
			return "";
		}
	}

	private static void loadTaxaMapping() {
		taxa2IDs = new HashMap<String, Species>();
		IDs2taxa = new HashMap<String, Species>();
		Species ancestor = new Species();
		ancestor.setLabel("LUCA");
		ancestor.setNcbi_taxon_id("1");
		ancestor.setSpecies("LUCA");
		taxa2IDs.put("LUCA", ancestor);
		IDs2taxa.put("1", ancestor);
		loadUniProtTaxa();
		loadNCBITaxa();
	}

	private static void loadNCBITaxa() {
		ResourceLoader loader = ResourceLoader.inst();
		BufferedReader reader = loader.loadResource(NCBI_TAXA+".gz", true);
		if (reader == null) {
			reader = ResourceLoader.inst().loadResource(NCBI_TAXA);
		}
		if (reader != null) {
			try {
				String id_pair = reader.readLine();
				while (id_pair != null) {
					if (!id_pair.contains("authority")) {
						id_pair = id_pair.replace('\t', ' ');
						String ids[] = id_pair.split("\\|");
						String taxon_id = ids[0].trim();
						String name = ids[1].trim();
						if (!ids[2].contains(name)) {
							name = (name + " " + ids[2].trim()).trim();
						} else if (ids[2].trim().length() > name.length()) {
							name = ids[2].trim();
						}
						if (!isNumeric(taxon_id)) {
                            System.err.println("Stop right here");
						} else {
							Species taxon = getSpecies(taxon_id, name);
							if (id_pair.contains("scientific name")) {
								taxon.setScientificName(name);
							taxon.setNcbi_taxon_id(taxon_id);
							IDs2taxa.put(taxon_id, taxon);
							taxa2IDs.put(name, taxon);
							}
						}
					}
					id_pair = reader.readLine();
				}
				reader.close();
			} catch (Exception e) {
				log.error("Unable to read " + NCBI_TAXA + " exception=" + e.getMessage());
			}
		}
	}

	private static Species getSpecies(String taxon_id, String name) {
		Species taxon = IDs2taxa.get(taxon_id);
		if (taxon == null) {
			taxon = taxa2IDs.get(name);
		}
		if (taxon == null) {
			taxon = new Species();
		}
		return taxon;
	}
	
    private static boolean isNumeric(String str)
    {
        return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }

    private static void loadUniProtTaxa() {
		ResourceLoader loader = ResourceLoader.inst();
		BufferedReader reader = loader.loadResource(UNIPROT_TAXA);
		if (reader != null) {
			try {
				String line = reader.readLine();
				while (line != null) {
					if (line.contains("N=") && !line.contains("Official")) {
						int index = line.indexOf(' ');
						String code = line.substring(0, index);
                        String [] parts = line.split(":");
                        index = parts[0].lastIndexOf(' ') + 1;
                        String taxon_id = parts[0].substring(index).trim();
						index = parts[1].indexOf("N=") + 2;
						String name = parts[1].substring(index).trim();
                        if (!isNumeric(taxon_id))
                            System.err.println("Stop right here");

                        Species taxon = getSpecies(taxon_id, name);

                        taxon.setNcbi_taxon_id(taxon_id);
                        taxon.setFive_code(code);
                        taxon.setScientificName(name);
                        if (!IDs2taxa.containsKey(taxon_id))
							IDs2taxa.put(taxon_id, taxon);
						if (!taxa2IDs.containsKey(name))
							taxa2IDs.put(name, taxon);
						if (!taxa2IDs.containsKey(code))
							taxa2IDs.put(code, taxon);
					}
					line = reader.readLine();
				}
				reader.close();
			} catch (Exception e) {
				log.error("Unable to read " + UNIPROT_TAXA + " exception=" + e.getMessage());
			}
		}
	}

	private static String speciesNameHack(String name) {
		String lcName = name.toLowerCase();
		/* The GO database is not using the suffix */
		if (lcName.equals("human")) {
			name = "Homo sapiens";
		} else if (lcName.equals("pantr")) {
			name = "Pan troglodytes";
		} else if (lcName.equals("homo-pan")) {
			name = "Homininae";
		} else if (lcName.equals("mouse")) {
			name = "Mus musculus";
		} else if (lcName.equals("rat")) {
			name = "Rattus norvegicus";
		} else if (lcName.equals("bovin")) {
			name = "Bos taurus";
		} else if (lcName.equals("canis familiaris") || lcName.equals("canfa")) {
			name = "Canis lupus familiaris";
		} else if (lcName.equals("mondo")) {
			name = "Monodelphis domestica";
		} else if (lcName.equals("ornan")) {
			name = "Ornithorhynchus anatinus";
		} else if (lcName.equals("chick")) {
			name = "Gallus gallus";
		} else if (lcName.equals("xentr")) {
			name = "Xenopus (Silurana) tropicalis";
		} else if (lcName.equals("fugu rubripes") || lcName.equals("fugru")) {
			name = "Takifugu rubripes";
		} else if (lcName.equals("brachydanio rerio") || lcName.equals("danre")) {
			name = "Danio rerio";
		} else if (lcName.equals("cioin")) {
			name = "Ciona intestinalis";
		} else if (lcName.equals("strpu")) {
			name = "Strongylocentrotus purpuratus";
		} else if (lcName.equals("caenorhabditis")) {
			name = "Caenorhabditis elegans";
		} else if (lcName.equals("briggsae") || lcName.equals("caebr")) {
			name = "Caenorhabditis briggsae";
		} else if (lcName.equals("drome")) {
			name = "Drosophila melanogaster";
		} else if (lcName.equals("anopheles gambiae str. pest") || lcName.equals("anoga")) {
			name = "Anopheles gambiae";
		} else if (lcName.equals("yeast")) {
			name = "Saccharomyces cerevisiae";
		} else if (lcName.equals("ashbya gossypii") || lcName.equals("ashgo")) {
			name = "Eremothecium gossypii";
		} else if (lcName.equals("neucr")) {
			name = "Neurospora crassa";
		} else if (lcName.startsWith("schizosaccharomyces pombe 927")) {
			name = "Schizosaccharomyces pombe";
		} else if (lcName.startsWith("schpo")) {
			name = "SCHPM";
		} else if (lcName.equals("dicdi")) {
			name = "Dictyostelium discoideum";
		} else if (lcName.equals("aspergillus nidulans")) {
			name = "Emericella nidulans";
		} else if (lcName.equals("chlre")) {
			name = "Chlamydomonas reinhardtii";
		} else if (lcName.equals("orysj")) {
			name = "Oryza sativa";
		} else if (lcName.equals("arath")) {
			name = "Arabidopsis thaliana";
		} else if (lcName.equals("metac")) {
			name = "Methanosarcina acetivorans";
		} else if (lcName.equals("strco")) {
			name = "Streptomyces coelicolor";
		} else if (lcName.equals("glovi")) {
			name = "Gloeobacter violaceus";
		} else if (lcName.equals("lepin")) {
			name = "Leptospira interrogans";
		} else if (lcName.equals("braja")) {
			name = "Bradyrhizobium japonicum";
		} else if (lcName.equals("escherichia coli coli str. K-12 substr. MG1655") || lcName.equals("ecoli")) {
			name = "Escherichia coli";
		} else if (lcName.equals("enthi")) {
			name = "Entamoeba histolytica";
		} else if (lcName.equals("bacsu")) {
			name = "Bacillus subtilis";
		} else if (lcName.equals("deira")) {
			name = "Deinococcus radiodurans";
		} else if (lcName.equals("thema")) {
			name = "Thermotoga maritima";
		} else if (lcName.equals("opisthokonts")) {
			name = "Opisthokonta";
		} else if (lcName.equals("bactn")) {
			name = "Bacteroides thetaiotaomicron";
		} else if (lcName.equals("leima")) {
			name = "Leishmania major";
		} else if (lcName.equals("eubacteria")) {
			name = "Bacteria <prokaryote>";
		} else if (lcName.equals("theria")) {
			name = "Theria <Mammalia>";
		} else if (lcName.equals("geobacter sufurreducens") || lcName.equals("geosl")) {
			name = "Geobacter sulfurreducens";
		} else if (lcName.equals("psea7")) {
			name = "Pseudomonas aeruginosa";
		} else if (lcName.equals("aquae") || lcName.equals("aquifex aeolicus vf5")) {
			name = "Aquifex aeolicus";
		} else if (lcName.equals("metac") || lcName.equals("methanosarcina acetivorans c2a")) {
			name = "Methanosarcina acetivorans";
		} else if (lcName.equals("sulso") || lcName.equals("sulfolobus solfataricus p2")) {
			name = "Sulfolobus solfataricus";
		} else if (lcName.equals("saccharomycetaceae-candida")) {
			name = "mitosporic Nakaseomyces";
		} else if (lcName.equals("sordariomycetes-leotiomycetes")) {
			name = "Leotiomycetes";
		} else if (lcName.equals("excavates")) {
			name = "Excavarus";
		} else if (lcName.equals("metazoa-choanoflagellida")) {
			name = "Opisthokonta";
		} else if (lcName.equals("alveolata-stramenopiles")) {
			name = "Eukaryota";
		} else if (lcName.equals("pezizomycotina-saccharomycotina")) {
			name = "saccharomyceta";
		} else if (lcName.equals("unikonts")) {
			name = "Eukaryota";
		} else if (lcName.equals("archaea-eukaryota")) {
			name = "cellular organisms";
		} else if (lcName.equals("osteichthyes")) {
			name = "Euteleostomi";
		} else if (lcName.equals("luca")) { // last universal common ancestor
			name = "root";
		} else if (lcName.equals("craniata-cephalochordata")) {
			name = "Chordata";
		} else if (lcName.equals("hexapoda-crustacea")) {
			name = "Pancrustacea";
		} else if (lcName.equals("rhabditida-chromadorea")) {
			name = "Chromadorea";
		} else if (lcName.startsWith("artiodactyla")) {
			name = "Cetartiodactyla";
		}
		return name;
	}

}
