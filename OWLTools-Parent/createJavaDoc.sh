mvn clean compile install -DskipTests javadoc:aggregate javadoc:aggregate-jar
echo "Hint: The generated javadoc is available in the folder: target/apidocs/"
echo "Do *NOT* forget to copy to the ../doc/api folder"
echo "Before committing the new javadoc, check with svn status for unversioned files!"
