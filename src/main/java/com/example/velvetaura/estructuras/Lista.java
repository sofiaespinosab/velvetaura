package com.example.velvetaura.estructuras;

import java.util.ArrayList;
import java.util.List;

/**
 * Lista enlazada simple propia (como la vista en clase).
 * Insertar al inicio es O(1), así que el elemento más reciente queda siempre de primero.
 */
public class Lista<T> {

    private Nodo<T> cabeza;
    private int tamanio;

    public void agregarAlInicio(T dato) {
        Nodo<T> nodo = new Nodo<>(dato);
        nodo.setSiguiente(cabeza);
        cabeza = nodo;
        tamanio++;
    }

    public int tamanio() {
        return tamanio;
    }

    public boolean estaVacia() {
        return cabeza == null;
    }

    // Recorre la lista de la cabeza al final, O(n)
    public List<T> aLista() {
        List<T> resultado = new ArrayList<>();
        Nodo<T> actual = cabeza;
        while (actual != null) {
            resultado.add(actual.getDato());
            actual = actual.getSiguiente();
        }
        return resultado;
    }
}
