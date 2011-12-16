package owltools.ontologyrelease.reports.go;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.semanticweb.owlapi.model.OWLClass;

import owltools.graph.OWLGraphWrapper;
import owltools.ontologyrelease.reports.OntologyReportGenerator.AbstractReport;
import owltools.ontologyrelease.reports.OntologyReportGenerator.OntologyReport;

/**
 * Provide reports for all external to go mappings.
 */
public class External2GoReports {

	private External2GoReports() {
		// no instances
	}

	/**
	 * Get all external reports. Creates always a list of new instances.
	 * 
	 * @return list of {@link OntologyReport}
	 */
	public static List<OntologyReport> getReports() {
		List<OntologyReport> external2GoReports = new ArrayList<OntologyReport>();
		external2GoReports.add(new External2GoEC());
		external2GoReports.add(new External2GoKegg());
		external2GoReports.add(new External2GoMetaCyc());
		external2GoReports.add(new External2GoReactome());
		external2GoReports.add(new External2GoResid());
		external2GoReports.add(new External2GoRhea());
		external2GoReports.add(new External2GoUmBbdEnzymeId());
		external2GoReports.add(new External2GoUmBbdPathwayId());
		external2GoReports.add(new External2GoUmBbdReactionId());
		external2GoReports.add(new External2GoWikipedia());
		return external2GoReports;
	}

	private static class External2Go extends AbstractReport {

		private final String reportFileName;
		private final String fileHeader;
		private final String prefix;
		
		private List<ExternalReference> references = null;
		private static final Comparator<ExternalReference> COMPARATOR = new Comparator<ExternalReference>() {
			
			@Override
			public int compare(ExternalReference ref1, ExternalReference ref2) {
				return ref1.externalId.compareTo(ref2.externalId);
			}
		};
		
		private static class ExternalReference {
			
			final String externalId;
			final String name;
			final String goId;
			
			/**
			 * @param externalId
			 * @param name
			 * @param goId
			 */
			ExternalReference(String externalId, String name, String goId) {
				this.externalId = externalId;
				this.name = name;
				this.goId = goId;
			}
		}

		/**
		 * @param reportFileName
		 * @param fileHeaderMain
		 * @param prefix
		 */
		public External2Go(String reportFileName, String fileHeaderMain,
				String prefix) {
			super();
			this.reportFileName = reportFileName;
			this.prefix = prefix;
			fileHeader = fileHeaderMain + "! Last update at " + getDate()
					+ " by OORT \n!\n";
		}

		@Override
		public String getReportFileName() {
			return reportFileName;
		}

		@Override
		public String getReportSubFolderName() {
			return "external2go";
		}

		@Override
		protected String getFileHeader() {
			return fileHeader;
		}

		@Override
		public void start(PrintWriter writer, OWLGraphWrapper graph)
				throws IOException {
			super.start(writer, graph);
			references = new ArrayList<ExternalReference>();
		}

		@Override
		public void handleTerm(PrintWriter writer, OWLClass owlClass,
				OWLGraphWrapper graph) throws IOException {
			List<String> xrefs = graph.getXref(owlClass);
			if (graph.isObsolete(owlClass) == false) {
				for (String xref : xrefs) {
					if (xref.length() > 1) {
						String prefix = null;
						String suffix = null;
						int pos = xref.indexOf(':');
						if (pos > 0 && (pos + 1) < xref.length()) {
							prefix = xref.substring(0, pos);
							suffix = xref.substring(pos + 1);
						}
						if (this.prefix.equals(prefix)) {
							String id = graph.getIdentifier(owlClass);
							String label = graph.getLabel(owlClass);
							references.add(new ExternalReference(suffix, "GO:"+label, id));
						}
					}
				}
			}
		}
		
		@Override
		public void end(PrintWriter writer, OWLGraphWrapper graph)
				throws IOException {
			Collections.sort(references, COMPARATOR);
			for (ExternalReference ref : references) {
				writer.print(ref.externalId);
				writer.print(" > ");
				writer.print(ref.name);
				writer.print(" ; ");
				writer.print(ref.goId);
			}
			super.end(writer, graph);
		}

		private static String getDate() {
			return DateFormat.getDateInstance().format(new Date());
		}
	}

	static class External2GoEC extends External2Go {

		External2GoEC() {
			super(
					"ec2go",
					"! Mapping of Gene Ontology terms to Enzyme Commission entries\n"
							+ "! Enzyme Commission: http://www.chem.qmul.ac.uk/iubmb/enzyme/\n",
					"EC");
		}
	}

	public static class External2GoKegg extends External2Go {

		External2GoKegg() {
			super(
					"kegg2go",
					"! Mapping of Gene Ontology terms to KEGG database entities.\n"
							+ "! KEGG, the Kyoto Encyclopedia of Genes and Genomes: http://www.genome.jp/kegg/\n",
					"KEGG");
		}
	}

	public static class External2GoMetaCyc extends External2Go {

		External2GoMetaCyc() {
			super("metacyc2go",
					"! Mapping of Gene Ontology terms to MetaCyc database references.\n"
							+ "! MetaCyc: http://metacyc.org/\n", "MetaCyc");
		}
	}

	public static class External2GoReactome extends External2Go {

		External2GoReactome() {
			super(
					"reactome2go",
					"! Mapping of Reactome entries to Gene Ontology terms\n"
							+ "! Manually created by Reactome staff and integrated into GO\n"
							+ "! http://www.reactome.org/\n", "Reactome");
		}
	}

	public static class External2GoResid extends External2Go {

		External2GoResid() {
			super("resid2go",
					"! Mapping of RESID entries to Gene Ontology terms\n"
							+ "! RESID: http://www.ebi.ac.uk/RESID/\n", "RESID");
		}
	}

	public static class External2GoRhea extends External2Go {

		External2GoRhea() {
			super(
					"rhea2go",
					"! Mapping of Gene Ontology terms to RHEA database references.\n"
							+ "! RHEA, the Annotated Reactions Database: http://www.ebi.ac.uk/rhea/\n",
					"RHEA");
		}
	}

	public static class External2GoUmBbdEnzymeId extends External2Go {

		External2GoUmBbdEnzymeId() {
			super(
					"um-bbd_enzymeid2go",
					"! Mapping of Gene Ontology terms to UM-BBD enzyme IDs\n"
							+ "! UM-BBD (The University of Minnesota Biocatalysis/Biodegradation Database): http://umbbd.msi.umn.edu/\n",
					"UM-BBD_enzymeID");
		}
	}

	public static class External2GoUmBbdPathwayId extends External2Go {

		External2GoUmBbdPathwayId() {
			super(
					"um-bbd_pathwayid",
					"! Mapping of Gene Ontology terms to UM-BBD pathway IDs\n"
							+ "! UM-BBD (The University of Minnesota Biocatalysis/Biodegradation Database): http://umbbd.msi.umn.edu/\n",
					"UM-BBD_pathwayID");
		}
	}

	public static class External2GoUmBbdReactionId extends External2Go {

		External2GoUmBbdReactionId() {
			super(
					"um-bbd_reactionid",
					"! Mapping of Gene Ontology terms to UM-BBD reaction IDs\n"
							+ "! UM-BBD (The University of Minnesota Biocatalysis/Biodegradation Database): http://umbbd.msi.umn.edu/\n",
					"UM-BBD_reactionID");
		}
	}

	public static class External2GoWikipedia extends External2Go {

		External2GoWikipedia() {
			super("wikipedia2go",
					"! Mapping of Gene Ontology terms to Wikipedia entries.\n"
							+ "! Wikipedia: http://en.wikipedia.org\n",
					"Wikipedia");
		}
	}
}
