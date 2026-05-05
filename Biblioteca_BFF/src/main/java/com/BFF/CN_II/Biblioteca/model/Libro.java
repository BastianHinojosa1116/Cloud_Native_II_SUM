package com.BFF.CN_II.Biblioteca.model;

public class Libro {

    private Integer idLibro;
    private String titulo;      // Coincide con la columna de Oracle y la Function
    private String autor;       // Coincide con la columna de Oracle y la Function
    private String codigoLibro; 
    private Integer disponible; // 1 = disponible, 0 = no disponible

    public Libro() {
    }

    public Libro(Integer idLibro, String titulo, String autor, String codigoLibro, Integer disponible) {
        this.idLibro = idLibro;
        this.titulo = titulo;
        this.autor = autor;
        this.codigoLibro = codigoLibro;
        this.disponible = disponible;
    }

    public Integer getIdLibro() {
        return idLibro;
    }

    public void setIdLibro(Integer idLibro) {
        this.idLibro = idLibro;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
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