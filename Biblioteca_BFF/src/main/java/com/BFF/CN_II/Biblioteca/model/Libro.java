package com.BFF.CN_II.Biblioteca.model;

public class Libro {

    private Integer idLibro;
    private String nombre;
    private String codigoLibro; //
    private Integer disponible; // 1 = disponible, 0 = no disponible

    public Libro() {
    }

    public Libro(Integer idLibro, String nombre, String codigoLibro, Integer disponible) {
        this.idLibro = idLibro;
        this.nombre = nombre;
        this.codigoLibro = codigoLibro;
        this.disponible = disponible;
    }

    public Integer getIdLibro() {
        return idLibro;
    }

    public void setIdLibro(Integer idLibro) {
        this.idLibro = idLibro;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCodigoLibro() {
        return codigoLibro;
    }

    public void setCodigoLibro(String codigoLibro) {
        this.codigoLibro = codigoLibro;
    }

    public Integer getDisponible() {
        return disponible;
    }

    public void setDisponible(Integer disponible) {
        this.disponible = disponible;
    }
}