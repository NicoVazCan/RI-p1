package es.udc.fi.ri.nicoedu;

import org.apache.lucene.index.*;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;

public class StatsField {

    private StatsField(){    
    }

    public static void main(String[] args) {

        String usage = "java StatsField <-index INDEX_PATH> [-field FIELD]";

        Directory dir = null;
        String indexPath = null;
        String fieldString = null;
        CollectionStatistics cs = null;
        DirectoryReader indexReader = null;
        IndexSearcher indexSearcher = null;

        for (int i=0; i < args.length; i++){
            if("-index".equals(args[i])){
                indexPath = args[i+1];
                i++;
            }
            else if("-field".equals(args[i])){
                fieldString = args[i+1];
                i++;
            }
        }
        if (indexPath == null){
            System.err.println("Error:" + usage); //usage muestra info sobre los args de la linea de comandos
            System.exit(1);
        }
        try{
            dir = FSDirectory.open(Paths.get(indexPath));  //abre los archivos de indice del sistema de archivos
            indexReader = DirectoryReader.open(dir);    //lee el indice  en un directorio
            indexSearcher = new IndexSearcher(indexReader);
        } catch (CorruptIndexException e){
            System.out.println("Exception: " + e);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Exception: " + e);
            e.printStackTrace();
        }

        if (fieldString != null){
            try{
                cs = indexSearcher.collectionStatistics(fieldString);
                if(cs == null){
                    System.err.println("Campo " + fieldString + " no existe o no tiene terminos");
                }
                else{
                    System.out.println(cs.toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else{
            final FieldInfos fieldInfos = FieldInfos.getMergedFieldInfos(indexReader);
            for(final FieldInfo fieldInfo : fieldInfos){
                try {
                    cs = indexSearcher.collectionStatistics(fieldInfo.name);
                    System.out.println(cs.toString() + "\n");
                } catch (Exception ignore) {}
            }
        }
    }
}
