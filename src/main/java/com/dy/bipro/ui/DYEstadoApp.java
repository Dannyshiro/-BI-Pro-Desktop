package com.dy.bipro.ui;

import com.dy.bipro.model.DYRangoFechas;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.springframework.stereotype.Component;

@Component
public class DYEstadoApp {
    private final ObjectProperty<DYRangoFechas> rango = new SimpleObjectProperty<>(DYRangoFechas.ULTIMOS_30_DIAS);
    private final IntegerProperty cambios = new SimpleIntegerProperty();
    private final ObjectProperty<DYModulo> navegacion = new SimpleObjectProperty<>();

    public ObjectProperty<DYRangoFechas> rangoProperty() {
        return rango;
    }

    public DYRangoFechas getRango() {
        return rango.get();
    }

    public IntegerProperty cambiosProperty() {
        return cambios;
    }

    public ObjectProperty<DYModulo> navegacionProperty() {
        return navegacion;
    }

    public void notificarCambio() {
        cambios.set(cambios.get() + 1);
    }

    public void ir(DYModulo modulo) {
        navegacion.set(null);
        navegacion.set(modulo);
    }
}
