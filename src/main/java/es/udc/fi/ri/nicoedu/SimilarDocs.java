package es.udc.fi.ri.nicoedu;

import org.apache.commons.math3.linear.RealVector;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.MultiTerms;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import javax.print.Doc;
import javax.print.MultiDoc;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SimilarDocs {
    private SimilarDocs(){
        //-doc, field, top, rep,
    }
    private double getCosineSimilarity(RealVector p, RealVector q){
        return (p.dotProduct(q)) / (p.getNorm()*q.getNorm());
    }

    public static void main(String[] args) throws IOException {
        String usage = "java SimilarDocs <-index INDEX_PATH> <-field field>" +
                        "<-doc doc> <-top n> <-rep bin|tf|tfxidf>";
        String indexPath = null;
        String fieldString = null;
        String doc = null;
        int n = 0;
        String rep = null;
        boolean docExists = false;

        Directory dir = null;
        DirectoryReader indexReader = null;

        if (args.length != 10){
            System.err.println("Usage: "+ usage);
            System.exit(1);
        }
        for(int i = 0; i< args.length; i++){
            if ("-index".equals(args[i])){
                indexPath = args[i+1];
                i++;
            }else if ("-field".equals(args[i])){
                fieldString = args[i+1];
                i++;
            }else if ("-doc".equals(args[i])){
                doc = args[i+1];
                i++;
            }else if ("-top".equals(args[i])){
                n = Integer.parseInt(args[i+1]);
                i++;
            }else if ("-rep".equals(args[i])){
                rep = args[i+1];
                i++;
            }
        }
        try{
            dir = FSDirectory.open(Paths.get(indexPath));
            indexReader = DirectoryReader.open(dir);
        }catch (CorruptIndexException e){
            System.out.println("Graceful message: exception "+ e);
            e.printStackTrace();
        }catch (IOException e){
            System.out.println("Graceful message: exception " + e);
            e.printStackTrace();
        }

    }
}
