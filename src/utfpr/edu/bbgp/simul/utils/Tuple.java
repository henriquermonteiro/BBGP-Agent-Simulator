package utfpr.edu.bbgp.simul.utils;

import java.util.Objects;

/**
 *
 * @author henri
 */
public class Tuple<T,U> {
    private T t;
    private U u;

    public Tuple(T t, U u) {
        this.t = t;
        this.u = u;
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

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.t);
        hash = 29 * hash + Objects.hashCode(this.u);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Tuple<?, ?> other = (Tuple<?, ?>) obj;
        if (!Objects.equals(this.t, other.t)) {
            return false;
        }
        if (!Objects.equals(this.u, other.u)) {
            return false;
        }
        return true;
    }
}
