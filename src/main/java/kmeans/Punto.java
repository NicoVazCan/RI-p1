package kmeans;

import java.util.ArrayList;
import java.util.List;

public class Punto {
    private Float[] data;
    private String doc;

    public Punto(String[] strings, String doc) {
        super();
        List<Float> puntos = new ArrayList<Float>();
        for (String string : strings) {
            puntos.add(Float.parseFloat(string));
        }
        this.data = puntos.toArray(new Float[strings.length]);
        this.doc = doc;
    }

    public Punto(String string, String doc) {
        super();
        List<Float> puntos = new ArrayList<Float>();

        puntos.add(Float.parseFloat(string));

        this.data = puntos.toArray(new Float[1]);
        this.doc = doc;
    }

    public Punto(Float[] data) {
        this.data = data;
    }

    public float get(int dimension) {
        return data[dimension];
    }

    public String getDoc() {
        return doc;
    }

    public int getGrado() {
        return data.length;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(doc);
        sb.append(" ");
        sb.append(data[0]);
        for (int i = 1; i < data.length; i++) {
            sb.append(", ");
            sb.append(data[i]);
        }
        return sb.toString();
    }

    public Double distanciaEuclideana(Punto destino) {
        Double d = 0d;
        for (int i = 0; i < data.length; i++) {
            d += Math.pow(data[i] - destino.get(i), 2);
        }
        return Math.sqrt(d);
    }

    @Override
    public boolean equals(Object obj) {
        Punto other = (Punto) obj;
        for (int i = 0; i < data.length; i++) {
            if (data[i] != other.get(i)) {
                return false;
            }
        }
        return true;
    }
}