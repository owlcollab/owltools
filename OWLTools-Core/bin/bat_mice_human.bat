cd "C:\Berkeley Research\OWLTools"
javac -d "C:\classpaths" -classpath "lib\runlibs\*;" src\owltools\graph\*.java
javac -d "C:\classpaths" -classpath "lib\runlibs\*;C:\classpaths;" src\owltools\io\*.java
javac -d "C:\classpaths" -classpath "lib\runlibs\*;C:\classpaths;" src\owltools\phenolog\*.java

cd "C:\classpaths"
java -Xms7G -cp "C:\Berkeley Research\OWLTools\lib\runlibs\*;C:\classpaths;" owltools.phenolog.Main "C:\Berkeley Research\mice_human.conf"