package kmeans;

import java.util.ArrayList;
import java.util.List;

public class Cluster {
    private List<Punto> puntos = new ArrayList<Punto>();
    private Punto centroide;
    private boolean doc = false;

    public Punto getCentroide() {
        return centroide;
    }

    public void setCentroide(Punto centroide) {
        this.centroide = centroide;
    }

    public List<Punto> getPuntos() {
        return puntos;
    }

    public boolean isDoc() {
        return doc;
    }

    public void setDoc(boolean doc) {
        this.doc = doc;
    }

    public void limpiarPuntos() {
        puntos.clear();
    }

    @Override
    public String toString() {
        return centroide.toString();
    }
}
