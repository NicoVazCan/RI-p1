package es.udc.fi.ri.nicoedu;

import kmeans.Cluster;
import kmeans.KMeans;
import kmeans.KMeansResultado;
import kmeans.Punto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DocsClusters {
    public static void main(String[] args) {
        String usage = "java DocsClusters <-index INDEX_PATH> <-field field>" +
                        "<-doc doc> <-top n> <-rep bin|tf|tfxidf>";

        String indexPath = null;
        String fieldString = null;
        Integer doc = null;
        int n = 0;
        Rep rep = null;
        int k = 0;

        for (int i = 0; i < args.length; i++){
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
            }else if ("-k".equals(args[i])){
                k = Integer.parseInt(args[i+1]);
                i++;
            }else {
                throw new IllegalArgumentException("unknown parameter " + args[i]);
            }
        }

        if(indexPath == null ||
                fieldString == null ||
                doc == null ||
                n == 0 ||
                rep == null ||
                k == 0){
            System.err.println("Usage: " + usage);
            System.exit(1);
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
        List<Punto> puntos = new ArrayList<>();
        SimilarDocs similarDocs = new SimilarDocs();
        String vector;
        similarDocs.getTopRealVector(indexPath, fieldString, rep, doc, n);

        for (int j = 0; j < similarDocs.topVectors.size(); j++) {
            vector = similarDocs.topVectors.get(j).toString().replace(',', '.');
            Punto p = new Punto(vector.substring(1,vector.length()-1).split(";"),
                    similarDocs.docs.get(j).toString());
            puntos.add(p);
        }

        System.out.println("\nClasificados mediante el algoritmo k-means con "+k+ " clusters:\n");

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
