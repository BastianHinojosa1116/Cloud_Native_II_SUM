package com.BFF.CN_II.Biblioteca.model;

public class Prestamo {
    private Integer idPrestamo;
    private Integer idUsuario;
    private Integer idLibro;

    public Prestamo() {
    }

    public Prestamo(Integer idPrestamo, Integer idUsuario, Integer idLibro) {
        this.idPrestamo = idPrestamo;
        this.idUsuario = idUsuario;
        this.idLibro = idLibro;
    }

    public Integer getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Integer idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Integer getIdLibro() {
        return idLibro;
    }

    public void setIdLibro(Integer idLibro) {
        this.idLibro = idLibro;
    }
}