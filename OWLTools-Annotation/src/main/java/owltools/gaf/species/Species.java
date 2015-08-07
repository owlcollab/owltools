package owltools.gaf.species;


public class Species {

	private String label;
	private String ncbi_taxon_id;
	private String genus;
	private String species;
	private String five_code;
	
	public String getLabel() {
		String suffix = (getSpecies() == null || getSpecies().trim().length() == 0) ? "" : " " + getSpecies().trim();
		label = getGenus() + suffix;
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getNcbi_taxon_id() {
		return ncbi_taxon_id;
	}
	public void setNcbi_taxon_id(String ncbi_taxon_id) {
		this.ncbi_taxon_id = ncbi_taxon_id;
	}
	public String getGenus() {
		return genus;
	}
	public void setGenus(String genus) {
		this.genus = genus;
	}
	public String getSpecies() {
		return species;
	}
	public void setSpecies(String species) {
		this.species = species;
	}
	public String getFive_code() {
		return five_code;
	}
	public void setFive_code(String five_code) {
		this.five_code = five_code;
	}
	public void setScientificName(String name) {
		String [] parts = name.split(" ");
		setGenus(parts[0]);
		if (parts.length > 1) {
			setSpecies(parts[1]);
			for (int i = 2; i < parts.length; i++) {
			setSpecies(getSpecies() + ' ' + parts[i]);
			}
		}
	}
}
	
