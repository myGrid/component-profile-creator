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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.io.IOUtils;

import uk.org.taverna.ns._2012.component.profile.Ontology;
import uk.org.taverna.ns._2012.component.profile.SemanticAnnotation;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
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
		possibles = generateTerms();
		pcs.firePropertyChange(ont.getId(), old, model);
	}

	public void removeOntology(Ontology ont) {
		OntModel model = models.remove(ont.getId());
		if (model != null) {
			onts.remove(ont.getId());
			possibles = generateTerms();
			pcs.firePropertyChange(ont.getId(), model, null);
		}
	}

	public List<PossibleStatement> getPossibleStatements() {
		return unmodifiableList(possibles);
	}

	public PossibleStatement getStatementFor(SemanticAnnotation sa) {
		OntModel om = models.get(sa.getOntology());
		if (om == null)
			return null;// TODO throw exception?
		Property p = om.createProperty(sa.getPredicate());
		Resource r = sa.getValue().trim().isEmpty() ? null : om
				.createResource(sa.getValue().trim());
		return new PossibleStatement(sa.getOntology(), p, r, sa);
	}

	public class PossibleStatement implements Comparable<PossibleStatement> {
		public final String ontologyId;
		final String humanReadableForm;
		private final SemanticAnnotation annotation;

		private String name(Resource res) {
			if (res == null)
				return "?";
			Property altLabel = res.getModel().createProperty(
					"http://www.w3.org/2004/02/skos/core#prefLabel");
			for (Statement s : list(res.getModel().listStatements(res,
					altLabel, (String) null)))
				return s.getObject().asLiteral().toString();
			return res.getLocalName();
		}

		PossibleStatement(String id, Resource predicate, Resource object,
				SemanticAnnotation sa) {
			ontologyId = id;
			humanReadableForm = String.format("%s: %s \u2192 %s\u2237%s", id,
					name(predicate),
					sa.getClazz() == null ? "null" : name(predicate.getModel()
							.createResource(sa.getClazz())), name(object));
			annotation = sa;
		}

		@Override
		public String toString() {
			return humanReadableForm;
		}

		@Override
		public int hashCode() {
			return humanReadableForm.hashCode();
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof PossibleStatement))
				return false;
			PossibleStatement ps = (PossibleStatement) o;
			return annotation.getOntology().equals(ps.annotation.getOntology())
					&& annotation.getPredicate().equals(
							ps.annotation.getPredicate())
					&& ((annotation.getClazz() == null && ps.annotation
							.getClazz() == null) || (annotation.getClazz() != null
							&& ps.annotation.getClazz() != null && annotation
							.getClazz().equals(ps.annotation.getClazz())))
					&& ((annotation.getValue() == null && ps.annotation
							.getValue() == null) || (annotation != null && annotation
							.getValue().equals(ps.annotation.getValue())));
		}

		public SemanticAnnotation getAnnotation() {
			SemanticAnnotation sa = new SemanticAnnotation();
			sa.setClazz(annotation.getClazz());
			sa.setOntology(ontologyId);
			sa.setPredicate(annotation.getOntology());
			sa.setValue(annotation.getValue());
			return sa;
		}

		@Override
		public int compareTo(PossibleStatement o) {
			return humanReadableForm.compareTo(o.humanReadableForm);
		}
	}

	private SemanticAnnotation sa(String ontology, RDFNode predicate) {
		SemanticAnnotation sa = new SemanticAnnotation();
		sa.setOntology(ontology);
		sa.setPredicate(predicate.toString());
		return sa;
	}

	private void initClass(SemanticAnnotation sa, RDFNode object) {
		if (object == null)
			return;
		if (object instanceof Individual)
			sa.setClazz(((Individual) object).getOntClass().getURI());
		else if (object.isResource())
			sa.setClazz(object.asResource().getURI());
	}

	private boolean initValue(SemanticAnnotation sa, RDFNode object) {
		if (object.isLiteral())
			sa.setValue(object.asLiteral().getLexicalForm());
		else if (object.isResource())
			sa.setValue(object.asResource().getURI());
		else
			return false;
		return true;
	}

	private <T> List<T> list(Iterator<T> iter) {
		List<T> list = new ArrayList<>();
		while (iter.hasNext())
			list.add(iter.next());
		return list;
	}

	private List<PossibleStatement> generateTerms() {
		Set<String> predicates = new HashSet<>();
		Set<PossibleStatement> added = new HashSet<>();
		for (Entry<String, OntModel> entry : models.entrySet()) {
			OntModel model = entry.getValue();
			Property type = model
					.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
			Property range = model
					.createProperty("http://www.w3.org/2000/01/rdf-schema#range");
			Property objectProperty = model
					.createProperty("http://www.w3.org/2002/07/owl#ObjectProperty");
			for (Statement s : list(model.listStatements(null, type,
					objectProperty)))
				for (Statement rangeStatement : list(model.listStatements(
						s.getSubject(), range, (RDFNode) null)))
					if (!predicates.contains(rangeStatement.getSubject())) {
						SemanticAnnotation sa = sa(entry.getKey(),
								rangeStatement.getSubject());
						initClass(sa, rangeStatement.getObject());
						sa.setValue("");
						added.add(new PossibleStatement(entry.getKey(),
								rangeStatement.getSubject(), null, sa));
						if (rangeStatement.getObject().isResource())
							for (Individual st : list(model
									.listIndividuals((Resource) rangeStatement
											.getObject()))) {
								sa = sa(entry.getKey(),
										rangeStatement.getSubject());
								initClass(sa, rangeStatement.getObject());
								initValue(sa, st);
								added.add(new PossibleStatement(entry.getKey(),
										rangeStatement.getSubject(), st, sa));
							}

					}
		}
		List<PossibleStatement> result = new ArrayList<>(added);
		Collections.sort(result);
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

	@SuppressWarnings("serial")
	public TableCellRenderer tableRenderer() {
		return new DefaultTableCellRenderer() {
			@Override
			public void setValue(Object value) {
				super.setValue(((PossibleStatement) value).humanReadableForm);
			}
		};
	}

	@SuppressWarnings({ "serial" })
	public ListCellRenderer<Object> listRenderer() {
		return (ListCellRenderer<Object>) new DefaultListCellRenderer() {
			@Override
			public JComponent getListCellRendererComponent(JList<?> list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				if (isSelected) {
					setBackground(list.getSelectionBackground());
					setForeground(list.getSelectionForeground());
				} else {
					setBackground(list.getBackground());
					setForeground(list.getForeground());
				}
				setText(((PossibleStatement) value).humanReadableForm);
				return this;
			}
		};
	}

	public TableCellEditor tableEditor() {
		JComboBox<PossibleStatement> statements = new JComboBox<>();
		statements.setRenderer(listRenderer());
		for (PossibleStatement ps : getPossibleStatements())
			statements.addItem(ps);
		return new DefaultCellEditor(statements);
	}

	public static void main(String... strings)
			throws OntologyCollectionException {
		OntologyCollection oc = new OntologyCollection();
		Ontology o = new Ontology();
		o.setId("scape");
		o.setValue("http://purl.org/DP/components");
		oc.addOntology(o);
		for (PossibleStatement ps : oc.getPossibleStatements())
			System.out.println(ps.humanReadableForm);
	}
}
