if (java.lang.System.getenv().containsKey("OWLTOOLS_JAR_PATH")) {
    var jarpath = java.lang.System.getenv().get("OWLTOOLS_JAR_PATH");
    //print("JP:"+jarpath);
    addToClasspath(jarpath);
}
