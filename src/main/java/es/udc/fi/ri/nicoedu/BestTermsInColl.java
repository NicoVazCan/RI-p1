package es.udc.fi.ri.nicoedu;

import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BestTermsInColl {
    public static void main(String[] args) {
        String usage = "java org.apache.lucene.demo.IndexFiles"
                + " [-index INDEX_PATH] [-field FIELD_NAME]\n"
                + " [-rev] [-outputfile FILE_PATH]\n\n";
        String indexPath = null, filePath = null, field = null;
        Integer top = null;
        boolean rev = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-index":
                    indexPath = args[++i];
                    break;
                case "-field":
                    field = args[++i];
                    break;
                case "-top":
                    top = Integer.parseInt(args[++i]);
                    break;
                case "-outputfile":
                    filePath =  args[++i];
                    break;
                case "-rev":
                    rev = true;
                    break;
                default:
                    throw new IllegalArgumentException("unknown parameter " + args[i]);
            }
        }

        if (indexPath == null ||
                field == null ||
                top == null) {
            System.err.println("Usage: " + usage);
            System.exit(1);
        }

        try (Directory indexDir = FSDirectory.open(Path.of(indexPath));
             IndexReader indexReader = DirectoryReader.open(indexDir);
             PrintStream output = filePath == null? System.out:
                     new PrintStream(Files.newOutputStream(Path.of(filePath)))) {

            Terms terms = MultiTerms.getTerms(indexReader, field);
            TermsEnum termsEnum;
            List<String> tns = new ArrayList<>();
            List<Float> fs = new ArrayList<>();
            BytesRef term;
            int df, N = indexReader.getDocCount(field);
            float f;

            if (terms != null) {
                int i;
                termsEnum = terms.iterator();

                while ((term = termsEnum.next()) != null) {
                    df = termsEnum.docFreq();
                    f = (float) (rev? df: Math.log10(((float) N)/((float) df)));
                    try {
                        for (i = 0; i < top && fs.get(i) < f; i++);

                        if(i < top) {
                            fs.add(i, f);
                            tns.add(i, term.utf8ToString());
                        }
                    } catch (IndexOutOfBoundsException ignore) {
                        fs.add(f);
                        tns.add(term.utf8ToString());
                    }
                }

                for (i = 0; i < fs.size(); i++) {
                    output.println(i+1+"º campo: "+tns.get(i));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
