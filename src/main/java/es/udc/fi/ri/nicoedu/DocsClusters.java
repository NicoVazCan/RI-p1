package es.udc.fi.ri.nicoedu;

import org.apache.commons.math3.linear.RealVector;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kmeans.Cluster;
import kmeans.KMeans;
import kmeans.KMeansResultado;
import kmeans.Punto;

public class DocsClusters {
    private DocsClusters(){
    }
    private static double getCosineSimilarity(RealVector v1, RealVector v2){
        return (v1.dotProduct(v2)) / (v1.getNorm() * v2.getNorm() );
    }
    public static void main(String[] args) throws IOException{
        String usage = "java DocsClusters <-index INDEX_PATH> <-field field>" +
                        "<-doc doc> <-top n> <-rep bin|tf|tfxidf>";

        String indexPath = null;
        String fieldString = null;
        String doc = null;
        int n = 0;
        String rep = null;
        int k = 0;
        boolean docExists = false;

        Directory dir = null;
        DirectoryReader indexReader = null;

        if(args.length != 12){
            System.err.println("Usage: " + usage);
            System.exit(1);
        }
        for (int i = 0; i < args.length; i++){
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
            }else if ("-k".equals(args[i])){
                k = Integer.parseInt(args[i+1]);
                i++;
            }
        }
        try {
            dir = FSDirectory.open(Paths.get(indexPath));
            indexReader = DirectoryReader.open(dir);
        } catch (CorruptIndexException e){
            System.out.println("Graceful message: exception " + e);
            e.printStackTrace();
        }catch (IOException e) {
            System.out.println("Graceful message: exception " + e);
            e.printStackTrace();
        }

        Date start = new Date();
        /*
        * K-means implementado por xetorthio en Github
        *
        * https://github.com/xetorthio/kmeans
        *
        * Añadimos un nuevo atributo a la clase Punto con su contructor, getter y
        * una modificación en el toString
        */
        System.out.println("\nClasificados mediante el algoritmo k-means con "+k+ " clusters:\n");
        List<Punto> puntos = new ArrayList<Punto>();

        for (DocsSimilarity colect : collection){
            Punto p = new Punto(String.valueOf(colect.similarity), colect.name);
            puntos.add(p);
        }

        KMeans kmeans = new KMeans();

        KMeansResultado resultado = kmeans.calcular(puntos, k);

        int i = 0;
        for (Cluster cluster : resultado.getClusters()){
            i++;
            System.out.println("-- Cluster "+ i + "--\n");
            for (Punto punto : cluster.getPuntos()){
                System.out.println(punto.toString() + "\n");
            }
            System.out.println("\n");
        }
        Date end = new Date();
        System.out.println(end.getTime() - start.getTime() + " total milliseconds");

    }
}
