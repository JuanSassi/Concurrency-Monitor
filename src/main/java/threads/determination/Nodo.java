public class Nodo<T> {
    private T info;
    private Nodo<T> izq;
    private Nodo<T> der;

    public Nodo(T info) {
        this.info = info;
        this.izq = null;
        this.der = null;
    }

    public T getInfo() { return info; }
    public Nodo<T> getIzq() { return izq; }
    public Nodo<T> getDer() { return der; }

    public void setIzq(Nodo<T> izq) { this.izq = izq; }
    public void setDer(Nodo<T> der) { this.der = der; }
}