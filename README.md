Taverna Component Profile Editor
================================

**A GUI Tool for creating and editing Taverna Component Profiles.**

You *should* set a name and description for your profile, and if you are going to use any semantic annotations on anything, you *must* load the profile in (with a short nickname, as an RDF file in a format accepted by Jena, so `application/rdf+xml` preferred) from a URL first using the "Annotation Ontologies" tab.

You are recommended to use a local copy of the [Taverna Workbench](http://www.taverna.org.uk/) to see if the profile does what you want, particularly if working with error handling mappings, prior to making it available for use by other people via the public component repository ([myExperiment](http://www.myexperiment.org/)).

Building
--------
Use [Apache Maven](http://maven.apache.org/download.cgi) 3.0 or later:

	mvn clean package

Running
-------
	java -jar target/component-profile-creator-0.0.1-SNAPSHOT-jar-with-dependencies.jar
