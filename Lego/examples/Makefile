OBO= http://purl.obolibrary.org/obo

# create table lookup table from ontology
go.labels: go.owl
	owltools $< --object-to-label-table > $@

# same for genes in GAF
%.labels: ../gene-associations/gene_association.%.gz
	gzip -dc $< | cut -f1,2,3 | sort -u | perl -npe 's/\t/_/' > $@

# translate lego macro syntax
%.owl: %.owl.in	 props-in.owl
	../utils/expand-owl-label-macros.pl wb.labels sgd.labels goa_human.labels  go.labels $< props-in.owl > $@

lego-relations.owl: props-in.owl
	../utils/expand-owl-label-macros.pl props-base.owl $< > $@

%-merged.owl: %.owl
	ontology-release-runner --allow-overwrite --outdir main --reasoner elk --no-subsets --skip-format owx $< $(OBO)/go.owl && cp main/go/examples/$@ .

%-rdf.owl: %.owl
	owltools $< -o file://`pwd`/$@
