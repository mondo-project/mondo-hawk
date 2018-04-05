/**
 * Copyright (c) 2018 Aston University.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 3.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-3.0
 *
 * Contributors:
 *   Antonio Garcia-Dominguez - initial API and implementation
 */
package org.hawk.examples.docgen.generator;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.hawk.examples.docgen.model.document.Author;
import org.hawk.examples.docgen.model.document.Collection;
import org.hawk.examples.docgen.model.document.Document;
import org.hawk.examples.docgen.model.document.DocumentFactory;
import org.hawk.examples.docgen.model.document.Tag;

/**
 * Simple demo program that generates a collection of linked documents
 */
public class Generator {

	private final File targetFolder;
	private final Finnegan textGenerator;

	private final ResourceSet resources;
	private Random random = new Random();
	private int authorCount = 10, authorLinkCount = authorCount * 2;
	private int tagCount = 20, tagLinkCount = tagCount / 2;
	private int documentCount = 10_000,
		minAuthors = 1, maxAuthors = 3,
		minTags = 1, maxTags = 10,
		minCites = 5, maxCites = 80;

	public static void main(String[] args) {
		if (args.length == 0) {
			System.err.println("Usage: java (-cp .../-jar ...) [target directory]");
			System.exit(1);
		}
		final File targetFolder = new File(args[0]);

		try {
			new Generator(targetFolder).run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Generator(File targetFolder) {
		this.targetFolder = targetFolder;
		this.textGenerator = new Finnegan();

		this.resources = new ResourceSetImpl();
		resources.getResourceFactoryRegistry()
			.getExtensionToFactoryMap()
			.put("*", new XMIResourceFactoryImpl());
	}

	public int getTagCount() {
		return tagCount;
	}

	public void setTagCount(int tagCount) {
		this.tagCount = tagCount;
	}

	public int getAuthorLinkCount() {
		return authorLinkCount;
	}

	public void setAuthorLinkCount(int authorLinkCount) {
		this.authorLinkCount = authorLinkCount;
	}

	public int getTagLinkCount() {
		return tagLinkCount;
	}

	public void setTagLinkCount(int tagLinkCount) {
		this.tagLinkCount = tagLinkCount;
	}

	public int getAuthorCount() {
		return authorCount;
	}

	public void setAuthorCount(int authorCount) {
		this.authorCount = authorCount;
	}

	public Random getRandom() {
		return random;
	}

	public void setRandom(Random prng) {
		this.random = prng;
	}

	public int getDocumentCount() {
		return documentCount;
	}

	public void setDocumentCount(int documentCount) {
		this.documentCount = documentCount;
	}

	public int getMinCites() {
		return minCites;
	}

	public void setMinCites(int minCites) {
		this.minCites = minCites;
	}

	public int getMaxCites() {
		return maxCites;
	}

	public void setMaxCites(int maxCites) {
		this.maxCites = maxCites;
	}

	public int getMinAuthors() {
		return minAuthors;
	}

	public void setMinAuthors(int minAuthors) {
		this.minAuthors = minAuthors;
	}

	public int getMaxAuthors() {
		return maxAuthors;
	}

	public void setMaxAuthors(int maxAuthors) {
		this.maxAuthors = maxAuthors;
	}

	public int getMinTags() {
		return minTags;
	}

	public void setMinTags(int minTags) {
		this.minTags = minTags;
	}

	public int getMaxTags() {
		return maxTags;
	}

	public void setMaxTags(int maxTags) {
		this.maxTags = maxTags;
	}

	public void run() throws IOException {
		deleteRecursively(targetFolder);

		EList<Tag> tags = generateTags();
		EList<Author> authors = generateAuthors();

		final List<Resource> rDocs = new ArrayList<>();
		for (int iDoc = 0; iDoc < documentCount; iDoc++) {
			final File fDoc = new File(targetFolder, String.format("d%d/d%02d/%04d.xmi", iDoc/1000, iDoc/100, iDoc));
			Resource rDoc = generateDocument(fDoc, tags, authors);
			System.out.println(String.format("Created document %d/%d", iDoc + 1, documentCount));

			rDocs.add(rDoc);
		}
		for (int iDoc = 0; iDoc < rDocs.size(); iDoc++) {
			generateCitations(rDocs.get(iDoc), rDocs);
			System.out.println(String.format("Linked document %d/%d", iDoc + 1, rDocs.size()));
		}
	}

	private void generateCitations(Resource fromResource, List<Resource> rDocuments) throws IOException {
		fromResource.load(null);

		final int nCites = minCites + random.nextInt(maxCites - minCites + 1);
		final Set<Resource> targetDocuments = new HashSet<>();
		while (targetDocuments.size() < nCites) {
			Resource targetDocument = rDocuments.get(random.nextInt(rDocuments.size()));
			if (targetDocument != fromResource) {
				targetDocuments.add(targetDocument);
			}
		}

		final Document fromDoc = getFirstDocument(fromResource);
		for (Resource rToDoc : targetDocuments) {
			rToDoc.load(null);
			final Document toDoc = getFirstDocument(rToDoc);
			if (toDoc == null) {
				throw new IOException("Could not find document in " + rToDoc);
			}
			fromDoc.getCites().add(toDoc);
		}

		fromResource.save(null);
		fromResource.unload();
		for (Resource rToDoc : targetDocuments) {
			rToDoc.unload();
		}
	}

	private Document getFirstDocument(Resource fromResource) {
		for (TreeIterator<EObject> itContents = fromResource.getAllContents(); itContents.hasNext(); ) {
			EObject eob = itContents.next();
			if (eob instanceof Document) {
				return (Document)eob;
			}
		}
		return null;
	}

	private Resource generateDocument(File fDoc, EList<Tag> tags, EList<Author> authors) throws IOException {
		final Resource rDocs = resources.createResource(URI.createFileURI(fDoc.getAbsolutePath()));
		final Collection docCollection = DocumentFactory.eINSTANCE.createCollection();
		rDocs.getContents().add(docCollection);

		final Document doc = DocumentFactory.eINSTANCE.createDocument();
		doc.setText(textGenerator.sentence(1, 2000));
		docCollection.getDocuments().add(doc);

		final int nAuthors = minAuthors + random.nextInt(maxAuthors - minAuthors + 1);
		for (int iAuthor = 0; iAuthor < nAuthors; iAuthor++) {
			doc.getWrittenBy().add(authors.get(random.nextInt(authors.size())));
		}

		final int nTags = minTags + random.nextInt(maxTags - minTags + 1);
		for (int iTags = 0; iTags < nTags; iTags++) {
			doc.getTags().add(tags.get(iTags));
		}

		rDocs.save(null);
		rDocs.unload();

		return rDocs;
	}

	private EList<Author> generateAuthors() throws IOException {
		final File authorsFile = new File(targetFolder, "authors.xmi");
		final Resource rAuthors = resources.createResource(URI.createFileURI(authorsFile.getAbsolutePath()));

		final Collection authorCollection = DocumentFactory.eINSTANCE.createCollection();
		rAuthors.getContents().add(authorCollection);

		final EList<Author> authors = authorCollection.getAuthors();
		for (int iAuthor = 0; iAuthor < authorCount; iAuthor++) {
			final Author author = DocumentFactory.eINSTANCE.createAuthor();
			author.setName(textGenerator.word(true) + " " + textGenerator.word(true));
			authors.add(author);
		}

		int iLink = 0;
		while (iLink < authorLinkCount) {
			final Author fromAuthor = authors.get(random.nextInt(authorCount));
			final Author toAuthor = authors.get(random.nextInt(authorCount));
			if (fromAuthor != toAuthor) {
				fromAuthor.getKnows().add(toAuthor);
				++iLink;
			}
		}

		rAuthors.save(null);
		return authors;
	}

	private EList<Tag> generateTags() throws IOException {
		final File tagsFile = new File(targetFolder, "tags.xmi");
		final Resource rTags = resources.createResource(URI.createFileURI(tagsFile.getAbsolutePath()));

		final Collection tagCollection = DocumentFactory.eINSTANCE.createCollection();
		rTags.getContents().add(tagCollection);

		// Create random tags
		final EList<Tag> tags = tagCollection.getTags();
		for (int iTag = 0; iTag < tagCount; iTag++) {
			final String tagName = textGenerator.word(false);
			final Tag tag = DocumentFactory.eINSTANCE.createTag();
			tag.setName(tagName);
			tags.add(tag);
		}

		// Relate tags to each other
		int iTagLink = 0;
		while (iTagLink < tagLinkCount) {
			final Tag fromTag = tags.get(random.nextInt(tagCount));
			final Tag toTag = tags.get(random.nextInt(tagCount));

			if (fromTag != toTag) {
				fromTag.getIsKindOf().add(toTag);
				++iTagLink;
			}
		}

		// Save into a file
		rTags.save(null);

		return tags;
	}

	private static void deleteRecursively(File f) throws IOException {
		if (!f.exists()) return;

		Files.walkFileTree(f.toPath(), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}

		});
	}
}
