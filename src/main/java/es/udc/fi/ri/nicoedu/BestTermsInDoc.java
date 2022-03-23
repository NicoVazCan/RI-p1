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

enum Order {
    TF, DF, IDFLOG10, TFxIDFLOG10;

    Order parse(String value) {
        switch (value) {
            case "tf":
                return TF;
            case "df":
                return DF;
            case "idflog10":
                return IDFLOG10;
            case "tf x idflog10":
                return TFxIDFLOG10;
            default:
                throw new IllegalArgumentException("unknown value " + value);
        }
    }
}

public class BestTermsInDoc {
    public static void main(String[] args) {
        String usage = "java org.apache.lucene.demo.IndexFiles"
                + " [-index INDEX_PATH] [-docID NºDOC] [-field FIELD_NAME]\n"
                + " [-top NºTERM] [-order tf|df|idflog10|\"tf x idflog10\"]\n"
                + " [-outputfile FILE_PATH]\n\n";
        String indexPath = null, filePath = null, field = null;
        Integer docID = null, top = null;
        Order order = null;

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
                case "-order":
                    order = Order.DF.parse(args[++i]);
                    break;
                case "-outputfile":
                    filePath =  args[++i];
                    break;
                default:
                    throw new IllegalArgumentException("unknown parameter " + args[i]);
            }
        }

        if (indexPath == null ||
            docID == null ||
            field == null ||
            top == null ||
            order == null) {
            System.err.println("Usage: " + usage);
            System.exit(1);
        }

        try (Directory indexDir = FSDirectory.open(Path.of(indexPath));
             IndexReader indexReader = DirectoryReader.open(indexDir);
             PrintStream output = filePath == null? System.out:
                     new PrintStream(Files.newOutputStream(Path.of(filePath)))) {
            Terms terms = MultiTerms.getTerms(indexReader, field);
            int di, i;
            TermsEnum te;
            BytesRef term;
            int tf, df, N;
            float idf, tfXidf;

            List<Integer> tfs = new ArrayList<>(), dfs = new ArrayList<>();
            List<Float> idfs = new ArrayList<>();
            List<String> tns =  new ArrayList<>();

            if (terms != null) {
                te = terms.iterator();

                while ((term = te.next()) != null) {
                    PostingsEnum posting = MultiTerms.getTermPostingsEnum(indexReader,field, term);
                    di = posting.advance(docID);
                    i = 0;

                    if (di == docID) {
                        tf = posting.freq();
                        df = te.docFreq();
                        N = indexReader.getDocCount(field);
                        idf = (float) Math.log10(((float) N)/((float) df));
                        tfXidf = tf*idf;

                        try {
                            switch (order) {
                                case TF:
                                    for (; i < top && tfs.get(i) > tf; i++);
                                    break;
                                case DF:
                                    for (; i < top && dfs.get(i) > df; i++);
                                    break;
                                case IDFLOG10:
                                    for (; i < top && idfs.get(i) > idf; i++);
                                    break;
                                case TFxIDFLOG10:
                                    for (; i < top && tfs.get(i)*idfs.get(i) > tfXidf; i++);
                                    break;
                            }
                        } catch (IndexOutOfBoundsException ignore) {
                            tfs.add(tf);
                            dfs.add(df);
                            idfs.add(idf);
                            tns.add(term.utf8ToString());
                        } finally {
                            if(i < top) {
                                tfs.set(i, tf);
                                dfs.set(i, df);
                                idfs.set(i, idf);
                                tns.set(i, term.utf8ToString());
                            }
                        }
                    }
                }

                for (int j = 0; j < top && j < tfs.size(); j++) {
                    tf = tfs.get(j);
                    df = dfs.get(j);
                    idf = idfs.get(j);
                    tfXidf = tf*idf;

                    output.println("termino: "+tns.get(j));
                    output.println("\ttf: "+tf+", df: "+df+
                            ", idflog10: "+idf+", tf*idflog10: "+tfXidf);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
