package es.udc.fi.ri.nicoedu;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.demo.knn.DemoEmbeddings;
import org.apache.lucene.demo.knn.KnnVectorDict;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Index all text files under a directory.
 *
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing. Run
 * it with no command-line arguments for usage information.
 */
public class IndexFiles implements AutoCloseable {
	static final String KNN_DICT = "knn-dict";

	// Calculates embedding vectors for KnnVector search
	private final DemoEmbeddings demoEmbeddings;
	private final KnnVectorDict vectorDict;
	private final Properties fileProp = new Properties();

	public static class WorkerThread implements Runnable {
		private final IndexWriter writer;
		private final IndexFiles indexFiles;
		private final Path folder;
		private final int maxDepth;

		public WorkerThread(final IndexWriter writer,
							final IndexFiles indexFiles,
							final Path folder,
							final int maxDepth) {
			this.writer = writer;
			this.indexFiles = indexFiles;
			this.folder = folder;
			this.maxDepth = maxDepth;
		}

		/**
		 * This is the work that the current thread will do when processed by the pool.
		 * In this case, it will only print some information.
		 */
		@Override
		public void run() {
			try {
				indexFiles.indexDocs(writer, folder, maxDepth);
			} catch (final IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			System.out.println(String.format("I am the thread '%s' and I am responsible for folder '%s'",
					Thread.currentThread().getName(), folder));
		}
	}

	private IndexFiles(KnnVectorDict vectorDict) throws IOException {
		fileProp.load(Files.newInputStream(
				Path.of("src/main/resources/config.properties")));
		if (vectorDict != null) {
			this.vectorDict = vectorDict;
			demoEmbeddings = new DemoEmbeddings(vectorDict);
		} else {
			this.vectorDict = null;
			demoEmbeddings = null;
		}
	}

	/** Index all text files under a directory. */
	public static void main(String[] args) throws Exception {
		String usage = "java org.apache.lucene.demo.IndexFiles"
				+ " [-index INDEX_PATH] [-docs DOCS_PATH] [-update] [-knn_dict DICT_PATH] \n"
				+ "[-openmode append|create|create_append] [-numThreads Nº] [-deep Nº] \n\n"
				+ "This indexes the documents in DOCS_PATH, creating a Lucene index"
				+ "in INDEX_PATH that can be searched with SearchFiles\n"
				+ "IF DICT_PATH contains a KnnVector dictionary, the index will also support KnnVector search";
		String indexPath = "index";
		String docsPath = null;
		String vectorDictSource = null;
		IndexWriterConfig.OpenMode openMode = OpenMode.CREATE;
		int numThreads = Runtime.getRuntime().availableProcessors();
		boolean partialIndexes = false;
		int maxDepth = Integer.MAX_VALUE;
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
				case "-index":
					indexPath = args[++i];
					break;
				case "-docs":
					docsPath = args[++i];
					break;
				case "-knn_dict":
					vectorDictSource = args[++i];
					break;
				case "-update":
					openMode = OpenMode.CREATE_OR_APPEND;
					break;
				case "-create":
					openMode = OpenMode.CREATE;
					break;
				case "-openmode":
					switch (args[++i]) {
						case "append":
							openMode = OpenMode.APPEND;
							break;
						case "create":
							openMode = OpenMode.CREATE;
							break;
						case "create_or_append":
							openMode = OpenMode.CREATE_OR_APPEND;
							break;
						default:
							throw new IllegalArgumentException("unknown parameter " + args[i]);
					}
					break;
				case "-numThreads":
					numThreads = Integer.parseInt(args[++i]);
					break;
				case "-partialIndexes":
					partialIndexes = true;
					break;
				case "-deep":
					maxDepth = Integer.parseInt(args[++i]);
					break;
				default:
					throw new IllegalArgumentException("unknown parameter " + args[i]);
			}
		}

		if (docsPath == null) {
			System.err.println("Usage: " + usage);
			System.exit(1);
		}

		final Path docDir = Paths.get(docsPath);
		if (!Files.isReadable(docDir)) {
			System.out.println("Document directory '" + docDir.toAbsolutePath()
					+ "' does not exist or is not readable, please check the path");
			System.exit(1);
		}

		Date start = new Date();
		try {
			System.out.println("Indexing to directory '" + indexPath + "'...");

			Path indexDir = Paths.get(indexPath);
			Directory dir = FSDirectory.open(indexDir);
			Analyzer analyzer = new StandardAnalyzer();
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
			iwc.setOpenMode(openMode);

			// Optional: for better indexing performance, if you
			// are indexing many documents, increase the RAM
			// buffer. But if you do this, increase the max heap
			// size to the JVM (eg add -Xmx512m or -Xmx1g):
			//
			// iwc.setRAMBufferSizeMB(256.0);

			KnnVectorDict vectorDictInstance = null;
			long vectorDictSize = 0;
			if (vectorDictSource != null) {
				KnnVectorDict.build(Paths.get(vectorDictSource), dir, KNN_DICT);
				vectorDictInstance = new KnnVectorDict(dir, KNN_DICT);
				vectorDictSize = vectorDictInstance.ramBytesUsed();
			}

			final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
			final List<IndexWriter> subwriters = new ArrayList<>();
			IndexWriter subwriter;

			try (IndexWriter writer = new IndexWriter(dir, iwc);
				IndexFiles indexFiles = new IndexFiles(vectorDictInstance);
				DirectoryStream<Path> directoryStream = Files.newDirectoryStream(docDir)) {

				for (final Path path : directoryStream) {
					if (Files.isDirectory(path)) {
						if(partialIndexes) {
							Path subIndexDir = Path.of(indexDir.getParent().toString() + "/" +
									path.toFile().getName());

							if(!Files.exists(subIndexDir)) {
								if(openMode == OpenMode.CREATE ||
										openMode == OpenMode.CREATE_OR_APPEND) {
									Files.createDirectory(subIndexDir);
								} else {
									throw new IOException();
								}
							}
							Directory subdir = FSDirectory.open(subIndexDir);

							subwriter = new IndexWriter(subdir,
									new IndexWriterConfig(analyzer).setOpenMode(openMode));
							subwriters.add(subwriter);
						} else {
							subwriter = writer;
						}

						executor.execute(new WorkerThread(subwriter, indexFiles, path, maxDepth));
					}
				}

				executor.shutdown();
				executor.awaitTermination(1, TimeUnit.HOURS);

				if(partialIndexes) {
					int dirs = subwriters.size();
					IndexWriter subw;
					Directory[] subdirectories = new Directory[dirs];

					for (int i = 0; i < dirs; i++) {
						subw = subwriters.get(i);
						subdirectories[i] = subw.getDirectory();
						subw.close();
					}
					writer.addIndexes(subdirectories);
				}

			} catch (final IOException e) {
				e.printStackTrace();
				System.exit(-1);
			} catch (final InterruptedException e) {
				e.printStackTrace();
				System.exit(-2);
			} finally {
				IOUtils.close(vectorDictInstance);
			}

			Date end = new Date();
			try (IndexReader reader = DirectoryReader.open(dir)) {
				System.out.println("Indexed " + reader.numDocs() + " documents in " + (end.getTime() - start.getTime())
						+ " milliseconds");
				/*if (reader.numDocs() > 100 && vectorDictSize < 1_000_000 && System.getProperty("smoketester") == null) {
					throw new RuntimeException(
							"Are you (ab)using the toy vector dictionary? See the package javadocs to understand why you got this exception.");
				}*/
			}
		} catch (IOException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		}
	}

	/**
	 * Indexes the given file using the given writer, or if a directory is given,
	 * recurses over files and directories found under the given directory.
	 *
	 * <p>
	 * NOTE: This method indexes one document per input file. This is slow. For good
	 * throughput, put multiple documents into your input file(s). An example of
	 * this is in the benchmark module, which can create "line doc" files, one
	 * document per line, using the <a href=
	 * "../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
	 * >WriteLineDocTask</a>.
	 *
	 * @param writer Writer to the index where the given file/dir info will be
	 *               stored
	 * @param path   The file to index, or the directory to recurse into to find
	 *               files to indt
	 * @throws IOException If there is a low-level I/O error
	 */
	void indexDocs(final IndexWriter writer, Path path, int maxDepth) throws IOException {
		String aux = this.fileProp.getProperty("onlyFiles");
		String[] onlyFiles = aux == null? null: aux.split(" ");

		if (Files.isDirectory(path)) {
			Files.walkFileTree(path, EnumSet.of(FileVisitOption.FOLLOW_LINKS), maxDepth,
					new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					try {
						if(onlyFiles == null ||
								Arrays.stream(onlyFiles).anyMatch((ext) -> file.toString().endsWith(ext))) {
							indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
						}
					} catch (@SuppressWarnings("unused") IOException ignore) {
						ignore.printStackTrace(System.err);
						// don't index files that can't be read.
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} else {
			indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
		}
	}

	/** Indexes a single document */
	void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException {
		try (InputStream stream = Files.newInputStream(file)) {
			String aux = this.fileProp.getProperty("onlyTopLines");
			int topLines = aux != null? Integer.parseInt(aux): Integer.MAX_VALUE;
			aux = this.fileProp.getProperty("onlyBottomLines");
			int bottonLines = aux != null? Integer.parseInt(aux): Integer.MAX_VALUE;

			// make a new, empty document
			Document doc = new Document();

			// Add the path of the file as a field named "path". Use a
			// field that is indexed (i.e. searchable), but don't tokenize
			// the field into separate words and don't index term frequency
			// or positional information:
			Field pathField = new StringField("path", file.toString(), Field.Store.YES);
			doc.add(pathField);

			// Add the last modified date of the file a field named "modified".
			// Use a LongPoint that is indexed (i.e. efficiently filterable with
			// PointRangeQuery). This indexes to milli-second resolution, which
			// is often too fine. You could instead create a number based on
			// year/month/day/hour/minutes/seconds, down the resolution you require.
			// For example the long value 2011021714 would mean
			// February 17, 2011, 2-3 PM.
			doc.add(new LongPoint("modified", lastModified));

			// Add the contents of the file to a field named "contents". Specify a Reader,
			// so that the text of the file is tokenized and indexed, but not stored.
			// Note that FileReader expects the file to be in UTF-8 encoding.
			// If that's not the case searching for special characters will fail.

			BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
			doc.add(new TextField("contents", reader));

			if (reader.markSupported()) {
				reader.mark(0);
				reader.reset();
			} else {
				throw new IOException();
			}

			if (topLines != Integer.MAX_VALUE || bottonLines != Integer.MAX_VALUE) {
				String line = reader.readLine();
				List<String> lines = new ArrayList<>();
				String[] lastLines = new String[bottonLines];
				int i;
				for (i = 0; i < topLines && line != null && !line.isEmpty(); i++) {
					lines.add(line);
					line = reader.readLine();
				}
				while (line != null && !line.isEmpty()) {
					for (i = 0; i < bottonLines && line != null && !line.isEmpty(); i++) {
						lastLines[i] = line;
						line = reader.readLine();
					}
				}
				for (int j = 0; j < bottonLines; j++) {
					aux = lastLines[(i + j) % bottonLines];

					if (aux != null) {
						lines.add(lastLines[(i + j) % bottonLines]);
					}
				}
				StringBuilder content = new StringBuilder();
				for (String s : lines) {
					content.append(s).append('\n');
				}

				doc.add(new StoredField("contentsStored", content.toString().getBytes()));
			} else {
				doc.add(new StoredField("contentsStored", stream.readAllBytes()));
			}

			doc.add(new StringField("hostname",
					InetAddress.getLocalHost().getHostName(), Field.Store.YES));

			doc.add(new StringField("thread",
					Thread.currentThread().getName(), Field.Store.YES));

			doc.add(new StringField("type",
					Files.isRegularFile(file)? "regular file":
					Files.isDirectory(file)? "directory":
					Files.isSymbolicLink(file)? "symbolic link":
							"otro"
					, Field.Store.YES));

			BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
			double size = ((double) attrs.size())/1000;

			doc.add(new DoublePoint("sizeKb", size));

			FileTime creationTime = attrs.creationTime(),
					lastAccessTime = attrs.lastAccessTime(),
					lastModifiedTime = attrs.lastModifiedTime();

			doc.add(new StringField("creationTime",
					creationTime.toString(), Field.Store.YES));

			doc.add(new StringField("lastAccessTime",
					lastAccessTime.toString(), Field.Store.YES));

			doc.add(new StringField("lastModifiedTime",
					lastModifiedTime.toString(), Field.Store.YES));

			doc.add(new StringField("creationTimeLucene",
					DateTools.dateToString(new Date(creationTime.toMillis()),
							DateTools.Resolution.MILLISECOND), Field.Store.YES));

			doc.add(new StringField("lastAccessTimeLucene",
					DateTools.dateToString(new Date(lastAccessTime.toMillis()),
							DateTools.Resolution.MILLISECOND), Field.Store.YES));

			doc.add(new StringField("lastModifiedTimeLucene",
					DateTools.dateToString(new Date(lastModifiedTime.toMillis()),
							DateTools.Resolution.MILLISECOND), Field.Store.YES));

			if (demoEmbeddings != null) {
				try (InputStream in = Files.newInputStream(file)) {
					float[] vector = demoEmbeddings
							.computeEmbedding(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
					doc.add(new KnnVectorField("contents-vector", vector, VectorSimilarityFunction.DOT_PRODUCT));
				}
			}

			if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
				// New index, so we just add the document (no old document can be there):
				System.out.println("adding " + file);
				writer.addDocument(doc);
			} else {
				// Existing index (an old copy of this document may have been indexed) so
				// we use updateDocument instead to replace the old one matching the exact
				// path, if present:
				System.out.println("updating " + file);
				writer.updateDocument(new Term("path", file.toString()), doc);
			}
		}
	}

	@Override
	public void close() throws IOException {
		IOUtils.close(vectorDict);
	}
}
