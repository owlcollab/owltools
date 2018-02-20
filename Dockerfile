FROM maven:alpine

RUN mkdir /app
WORKDIR /app
ADD pom.xml /app
RUN ["/usr/local/bin/mvn-entrypoint.sh", "mvn", "verify", "clean", "--fail-never"]
ADD . /app
WORKDIR /app/OWLTools-Parent
RUN mvn -DskipTests clean install && cp ../OWLTools-Runner/target/owltools ../OWLTools-Runner/bin/
ENV PATH="$PATH:/app/OWLTools-Runner/bin/"

CMD owltools -h
