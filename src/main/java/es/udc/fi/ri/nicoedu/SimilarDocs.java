package es.udc.fi.ri.nicoedu;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

enum Rep { bin, tf, tfxidf }

public class SimilarDocs {
    public List<RealVector> topVectors = new ArrayList<>();
    public List<Float> scores = new ArrayList<>();
    public List<Integer> docs = new ArrayList<>();

    static double getCosineSimilarity(RealVector v1, RealVector v2) {
        return (v1.dotProduct(v2)) / (v1.getNorm() * v2.getNorm());
    }

    static Map<String, Float> getTermFrequencies(IndexReader reader, int docId, String field, Set<String> terms) throws IOException {
        Terms vector = reader.getTermVector(docId, field);

        TermsEnum termsEnum = null;
        termsEnum = vector.iterator();
        Map<String, Float> frequencies = new HashMap<>();
        BytesRef text = null;
        while ((text = termsEnum.next()) != null) {
            String term = text.utf8ToString();
            float freq = (float) termsEnum.totalTermFreq();
            frequencies.put(term, freq);
        }
        return frequencies;
    }

    static Map<String, Float> getTermBin(IndexReader reader, int docId, String field, Set<String> terms) throws IOException {
        Terms vector = reader.getTermVector(docId, field);

        TermsEnum termsEnum = null;
        termsEnum = vector.iterator();
        Map<String, Float> frequencies = new HashMap<>();
        BytesRef text = null;
        while ((text = termsEnum.next()) != null) {
            String term = text.utf8ToString();

            frequencies.put(term, 1F);
        }
        return frequencies;
    }

    static Map<String, Float> getTfXIdf(IndexReader reader, int docId, String field, Set<String> terms) throws IOException {
        Terms vector = reader.getTermVector(docId, field);

        TermsEnum termsEnum = null;
        termsEnum = vector.iterator();
        Map<String, Float> frequencies = new HashMap<>();
        BytesRef text = null;
        while ((text = termsEnum.next()) != null) {
            String term = text.utf8ToString();
            frequencies.put(term,
                    (float) (termsEnum.totalTermFreq()*Math.log10(((float) reader.getDocCount(field))/
                            ((float) termsEnum.docFreq()))));
        }
        return frequencies;
    }

    static RealVector toRealVector(Map<String, Float> map, Set<String> terms) {
        RealVector vector = new ArrayRealVector(terms.size());
        int i = 0;
        for (String term : terms) {
            float value = map.containsKey(term) ? map.get(term) : 0;
            vector.setEntry(i++, value);
        }
        return vector;
    }

    public void getTopRealVector(String indexPath, String fieldString, Rep rep, int doc, int n) {
        try(Directory dir = FSDirectory.open(Paths.get(indexPath));
            DirectoryReader indexReader = DirectoryReader.open(dir)) {

            int nDocs = indexReader.numDocs(), pos;
            float similar;
            Map<String, Float> tr1 = null, tr2 = null;
            RealVector v1, v2;

            Set<String> terms = new HashSet<>();
            TermsEnum te = MultiTerms.getTerms(indexReader, fieldString).iterator();
            BytesRef term;

            while ((term = te.next()) != null) {
                terms.add(term.utf8ToString());
            }

            switch (rep) {
                case tf:
                    tr1 = getTermFrequencies(indexReader, doc, fieldString, terms);
                    break;

                case bin:
                    tr1 = getTermBin(indexReader, doc, fieldString, terms);
                    break;

                case tfxidf:
                    tr1 = getTfXIdf(indexReader, doc, fieldString, terms);
                    break;
            }
            v1 = toRealVector(tr1, terms);

            for (int i = 0; (i==doc? ++i: i) < nDocs; i++) {

                switch (rep) {
                    case tf:
                        tr2 = getTermFrequencies(indexReader, i, fieldString, terms);
                        break;

                    case bin:
                        tr2 = getTermBin(indexReader, i, fieldString, terms);
                        break;

                    case tfxidf:
                        tr2 = getTfXIdf(indexReader, i, fieldString, terms);
                        break;
                }
                v2 = toRealVector(tr2, terms);
                similar = (float) getCosineSimilarity(v1, v2);

                for (pos = 0;
                     pos < n && pos < scores.size() && scores.get(pos) > similar;
                     pos++);


                scores.add(pos, similar);
                topVectors.add(pos, v2);
                docs.add(pos, i);

                if (scores.size()-1 == n) {
                    scores.remove(n);
                    topVectors.remove(n);
                    docs.remove(n);
                }
            }

            System.out.println("Vector de terminos de la coleccion="+Arrays.toString(terms.toArray()) + "\n"
                    + "Documentos similares al de id=" + doc
                    + " con el campo " + fieldString + ", ruta "
                    + indexReader.document(doc).get("path")
                    + "y vector de terminos="+Arrays.toString(v1.toArray())
                    + ", ordenados por "+rep+":\n");
            for (int i = 0; i < scores.size(); i++) {
                System.out.println("\t" + (i + 1) + "º documento con id=" + docs.get(i)
                        + " y ruta " + indexReader.document(doc).get("path")
                        + " tiene el vector de terminos="
                        + Arrays.toString(topVectors.get(i).toArray()));
            }

        } catch (IOException e){
            System.out.println("Graceful message: exception "+ e);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String usage = "java SimilarDocs <-index INDEX_PATH> <-field field>" +
                        "<-doc doc> <-top n> <-rep bin|tf|tfxidf>";
        String indexPath = null;
        String fieldString = null;
        Integer doc = null;
        int n = 0;
        Rep rep = null;

        for(int i = 0; i< args.length; i++){
            if ("-index".equals(args[i])){
                indexPath = args[i+1];
                i++;
            }else if ("-field".equals(args[i])){
                fieldString = args[i+1];
                i++;
            }else if ("-doc".equals(args[i])){
                doc = Integer.parseInt(args[i+1]);
                i++;
            }else if ("-top".equals(args[i])){
                n = Integer.parseInt(args[i+1]);
                i++;
            }else if ("-rep".equals(args[i])){
                rep = Rep.valueOf(args[i+1]);
                i++;
            }else{
                throw new IllegalArgumentException("unknown parameter " + args[i]);
            }
        }

        if (indexPath == null ||
            fieldString == null ||
            doc == null ||
            n == 0 ||
            rep == null
        ) {
            System.err.println("Usage: " + usage);
            System.exit(1);
        }

        SimilarDocs similarDocs = new SimilarDocs();
        similarDocs.getTopRealVector(indexPath, fieldString, rep, doc, n);
    }
}
