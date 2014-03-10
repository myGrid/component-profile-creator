package org.taverna.component.profile_creator.utils;

import static com.hp.hpl.jena.rdf.model.ModelFactory.createOntologyModel;
import static java.util.Collections.unmodifiableList;
import static org.apache.commons.io.IOUtils.closeQuietly;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;

import uk.org.taverna.ns._2012.component.profile.Ontology;
import uk.org.taverna.ns._2012.component.profile.SemanticAnnotation;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Statement;

public class OntologyCollection {
	private final PropertyChangeSupport pcs;
	Map<String, Ontology> onts = new HashMap<>();
	Map<String, OntModel> models = new TreeMap<>();
	private List<PossibleStatement> possibles = new ArrayList<>();

	public OntologyCollection() {
		pcs = new PropertyChangeSupport(this);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}

	public void addOntology(Ontology ont) throws OntologyCollectionException {
		OntModel old, model = readOntologyFromURI(ont.getId(), ont.getValue());
		onts.put(ont.getId(), ont);
		old = models.put(ont.getId(), model);
		possibles=generateTerms();
		pcs.firePropertyChange(ont.getId(), old, model);
	}

	public void removeOntology(Ontology ont) {
		OntModel model = models.remove(ont.getId());
		if (model != null) {
			onts.remove(ont.getId());
			possibles=generateTerms();
			pcs.firePropertyChange(ont.getId(), model, null);
		}
	}

	public List<PossibleStatement> getPossibleStatements() {
		return unmodifiableList(possibles);
	}

	public static class PossibleStatement {
		public final String ontologyId;
		public final String humanReadableForm;
		private final SemanticAnnotation annotation;

		PossibleStatement(String id, String name, SemanticAnnotation sa) {
			ontologyId = id;
			humanReadableForm = name;
			annotation = sa;
		}

		public SemanticAnnotation getAnnotation() {
			SemanticAnnotation sa = new SemanticAnnotation();
			sa.setClazz(annotation.getClazz());
			sa.setOntology(ontologyId);
			sa.setPredicate(annotation.getOntology());
			sa.setValue(annotation.getValue());
			return sa;
		}
	}

	private List<PossibleStatement> generateTerms() {
		List<PossibleStatement> result = new ArrayList<>();
		for (Entry<String, OntModel> entry : models.entrySet()) {
			Iterator<Statement> it = entry.getValue().listStatements();
			while (it.hasNext()) {
				Statement s = it.next();
				Property pred = s.getPredicate();
				SemanticAnnotation sa = new SemanticAnnotation();
				sa.setPredicate(pred.toString());
				if (s.getObject() instanceof Individual)
					sa.setClazz(((Individual) s.getObject()).getOntClass()
							.getURI());
				if (s.getObject().isLiteral())
					sa.setValue(s.getObject().asLiteral().getLexicalForm());
				else if (s.getObject().isResource())
					sa.setValue(s.getObject().asResource().getURI());
				else
					continue;
				result.add(new PossibleStatement(entry.getKey(), entry.getKey()
						+ ": " + pred.getLocalName() + " => " + s.getObject(),
						sa));
			}
		}
		return result;
	}

	@SuppressWarnings("serial")
	public static class OntologyCollectionException extends Exception {
		OntologyCollectionException(String message, Exception cause) {
			super(message, cause);
		}
	}

	private OntModel readOntologyFromURI(String ontologyId, String ontologyURI)
			throws OntologyCollectionException {
		OntModel model = createOntologyModel();
		InputStream in = null;
		try {
			URL url = new URL(ontologyURI);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			/* CRITICAL: must be retrieved as correct content type */
			conn.addRequestProperty("Accept",
					"application/rdf+xml,application/xml;q=0.9");
			in = conn.getInputStream();
			// TODO Consider whether the encoding is handled right
			// ontologyModel.read(in, url.toString());
			model.read(new StringReader(IOUtils.toString(in, "UTF-8")),
					url.toString());
		} catch (NullPointerException | IOException e) {
			throw new OntologyCollectionException("Problem reading ontology "
					+ ontologyId, e);
		} finally {
			closeQuietly(in);
		}
		return model;
	}

}
