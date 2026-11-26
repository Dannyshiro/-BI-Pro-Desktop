package com.ejemplo.bipro.ui;

import com.ejemplo.bipro.model.RangoFechas;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.springframework.stereotype.Component;

@Component
public class EstadoApp {
    private final ObjectProperty<RangoFechas> rango = new SimpleObjectProperty<>(RangoFechas.ULTIMOS_30_DIAS);
    private final IntegerProperty cambios = new SimpleIntegerProperty();
    private final ObjectProperty<Modulo> navegacion = new SimpleObjectProperty<>();

    public ObjectProperty<RangoFechas> rangoProperty() {
        return rango;
    }

    public RangoFechas getRango() {
        return rango.get();
    }

    public IntegerProperty cambiosProperty() {
        return cambios;
    }

    public ObjectProperty<Modulo> navegacionProperty() {
        return navegacion;
    }

    public void notificarCambio() {
        cambios.set(cambios.get() + 1);
    }

    public void ir(Modulo modulo) {
        navegacion.set(null);
        navegacion.set(modulo);
    }
}
