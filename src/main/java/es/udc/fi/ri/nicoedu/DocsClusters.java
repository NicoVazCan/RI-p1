package es.udc.fi.ri.nicoedu;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

enum Rep { bin, tf, tfxidf; }

public class DocsClusters {
    public static void main(String[] args) {
        String usage = "java org.apache.lucene.demo.IndexFiles"
                + " [-index INDEX_PATH] [-docID NºDOC] [-field FIELD_NAME]\n"
                + " [-top NºTERM] [-order tf|df|idflog10|\"tf x idflog10\"]\n"
                + " [-outputfile FILE_PATH]\n\n";
        String indexPath = null, filePath = null, field = null;
        Integer docID = null, top = null, k = null;
        Rep rep = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-index":
                    indexPath = args[++i];
                    break;
                case "-docID":
                    docID = Integer.parseInt(args[++i]);
                    break;
                case "-field":
                    field = args[++i];
                    break;
                case "-top":
                    top = Integer.parseInt(args[++i]);
                    break;
                case "-rep":
                    rep = Rep.valueOf(args[++i]);
                    break;
                case "-k":
                    k = Integer.parseInt(args[++i]);
                    break;
                case "-outputfile":
                    filePath = args[++i];
                    break;
                default:
                    throw new IllegalArgumentException("unknown parameter " + args[i]);
            }
        }

        if (indexPath == null ||
                docID == null ||
                field == null ||
                top == null ||
                rep == null ||
                k == null
        ) {
            System.err.println("Usage: " + usage);
            System.exit(1);
        }

        try (Directory indexDir = FSDirectory.open(Path.of(indexPath));
             IndexReader indexReader = DirectoryReader.open(indexDir);
             PrintStream output = filePath == null? System.out:
                     new PrintStream(Files.newOutputStream(Path.of(filePath)))) {

        } catch (
        IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
