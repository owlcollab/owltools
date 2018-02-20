FROM maven:alpine

RUN mkdir /app
WORKDIR /app

# this trick doesn't seem to work for multi-project repos
#ADD pom.xml /app
#RUN ["/usr/local/bin/mvn-entrypoint.sh", "mvn", "verify", "clean", "--fail-never"]

ADD OWLTools-Parent/pom.xml /app/OWLTools-Parent/
WORKDIR /app/OWLTools-Parent
RUN ["/usr/local/bin/mvn-entrypoint.sh", "mvn", "verify", "clean", "--fail-never"]
WORKDIR /app

ADD . /app
WORKDIR /app/OWLTools-Parent
RUN mvn -DskipTests clean install && cp ../OWLTools-Runner/target/owltools ../OWLTools-Runner/bin/
ENV PATH="$PATH:/app/OWLTools-Runner/bin/"

CMD owltools -h
