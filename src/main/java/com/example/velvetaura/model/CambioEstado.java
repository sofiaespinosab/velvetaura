package com.example.velvetaura.model;


public record CambioEstado(Long pedidoId, EstadoPedido estadoAnterior, EstadoPedido estadoNuevo) {
}
