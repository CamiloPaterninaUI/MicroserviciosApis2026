package com.unibague.pagoservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.unibague.pagoservice.model.Pago;
import com.unibague.pagoservice.model.Pago.Estado;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByPedidoId(String pedidoId);

    boolean existsByPedidoId(String pedidoId);

    List<Pago> findByEstado(Estado estado);
}
