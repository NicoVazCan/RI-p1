package es.udc.fi.ri.nicoedu;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class WriteIndex {

    public static void main(String[] args) {
        String usage = "java org.apache.lucene.demo.IndexFiles"
                + " [-index INDEX_PATH] [-outputfile FILE_PATH]\n\n";
        String indexPath = null, filePath = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-index":
                    indexPath = args[++i];
                    break;
                case "-outputfile":
                    filePath =  args[++i];
                    break;
                default:
                    throw new IllegalArgumentException("unknown parameter " + args[i]);
            }
        }

        if (indexPath == null) {
            System.err.println("Usage: " + usage);
            System.exit(1);
        }

        try (Directory indexDir = FSDirectory.open(Path.of(indexPath));
             IndexReader indexReader = DirectoryReader.open(indexDir);
             PrintStream output = filePath == null? System.out:
                     new PrintStream(Files.newOutputStream(Path.of(filePath)))) {

            for (int i = 0; i < indexReader.numDocs(); i++) {
                Document doc = null;
                String fieldValue;

                try {
                    doc = indexReader.document(i);
                } catch (IOException e1) {
                    System.out.println("Graceful message: exception " + e1);
                    e1.printStackTrace();
                }
                output.println("Documento: " + i);

                if(doc != null) {
                    for (IndexableField field : doc) {
                        output.println("\tCampo: " + field.name());
                        if(field.fieldType().stored()) {
                            if (field.numericValue() != null) {
                                fieldValue = String.valueOf(field.numericValue().floatValue());
                            } else if (field.binaryValue() != null) {
                                fieldValue = field.binaryValue().toString();
                            } else {
                                fieldValue = field.stringValue();
                            }

                            output.println("\t\tValor:");
                            output.println("\t\t" + fieldValue);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
