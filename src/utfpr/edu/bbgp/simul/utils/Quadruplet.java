/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.simul.utils;

/**
 *
 * @author henri
 */
public class Quadruplet<T,U,V,W> {
    private T t;
    private U u;
    private V v;
    private W w;

    public Quadruplet(T t, U u, V v, W w) {
        this.t = t;
        this.u = u;
        this.v = v;
        this.w = w;
    }

    public T getT() {
        return t;
    }

    public void setT(T t) {
        this.t = t;
    }

    public U getU() {
        return u;
    }

    public void setU(U u) {
        this.u = u;
    }

    public V getV() {
        return v;
    }

    public void setV(V v) {
        this.v = v;
    }

    public W getW() {
        return w;
    }

    public void setW(W w) {
        this.w = w;
    }
}
