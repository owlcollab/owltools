package owltools.phenolog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

public class GenePhenoTsvParser {
	private int geneIdColumnNo;
	private int geneLabelColumnNo;
	private int phenoIdColumnNo;
	private int phenoLabelColumnNo;
	private String phenoIdPrefix = null;

	// result[0] : Gene ID
	// result[1] : Gene Label
	// result[6] : Phenotype ID
	// result[7] : Phenotype Label
	public void setConfigForFlyMine() {
		geneIdColumnNo = 0;
		geneLabelColumnNo = 1;
		phenoIdColumnNo = 6;
		phenoLabelColumnNo = 7;
		phenoIdPrefix = "FBbt";
	}
	
    // result[5] : Gene ID
    // result[3] : Phenotype ID
	public void setConfigForMGIPhenoGeno() {
		geneIdColumnNo = 5;
		geneLabelColumnNo = -1;
		phenoIdColumnNo = 3;
		phenoLabelColumnNo = -7;
		phenoIdPrefix = "MP";
	}


	public HashSet<GenePheno> parse(String fileName) throws IOException {
		HashSet<GenePheno> gpset = new HashSet<GenePheno>();

		File myFile = new File(fileName);
		FileReader fileReader = new FileReader(myFile);
		BufferedReader reader = new BufferedReader(fileReader);

		String[] result;
		String line = null;
		int maxGCol = Math.max(geneIdColumnNo, geneLabelColumnNo);
		int maxPCol = Math.max(phenoIdColumnNo, phenoLabelColumnNo);
		int maxCol = Math.max(maxGCol, maxPCol);
		
		while ((line = reader.readLine()) != null) {
			result = line.split("\t");

			if (result.length >= maxCol) {
				if ((result[geneIdColumnNo] != null) &&
						(result[phenoIdColumnNo] != null)) {
					if (result[phenoIdColumnNo].contains(phenoIdPrefix)) {
						gpset.add(new GenePheno(result[geneIdColumnNo], 
								geneLabelColumnNo >= 0 ? result[geneLabelColumnNo] : "", 
								result[phenoIdColumnNo], 
								phenoLabelColumnNo >= 0 ? result[phenoLabelColumnNo] : ""));
					}
				}
			}
		}
		reader.close();
		return gpset;
	}


}
