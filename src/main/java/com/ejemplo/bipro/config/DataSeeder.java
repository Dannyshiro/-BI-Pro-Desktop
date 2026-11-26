package com.ejemplo.bipro.config;

import com.ejemplo.bipro.model.*;
import com.ejemplo.bipro.repository.*;
import com.ejemplo.bipro.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final ProveedorRepository proveedorRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final TurnoCajaRepository turnoRepository;
    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final OrdenCompraRepository ordenRepository;
    private final DetalleOrdenRepository detalleOrdenRepository;
    private final MovimientoStockRepository movimientoRepository;
    private final GastoRepository gastoRepository;
    private final TareaRepository tareaRepository;

    private final Random rnd = new Random(42);
    private int numeroVenta = 0;

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.count() == 0) {
            log.info("Base de datos vacía: cargando datos de demostración...");
            sembrar();
        }
    }

    @Transactional
    public void reiniciar() {
        movimientoRepository.deleteAllInBatch();
        detalleVentaRepository.deleteAllInBatch();
        ventaRepository.deleteAllInBatch();
        turnoRepository.deleteAllInBatch();
        detalleOrdenRepository.deleteAllInBatch();
        ordenRepository.deleteAllInBatch();
        gastoRepository.deleteAllInBatch();
        tareaRepository.deleteAllInBatch();
        clienteRepository.deleteAllInBatch();
        productoRepository.deleteAllInBatch();
        categoriaRepository.deleteAllInBatch();
        proveedorRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
        empresaRepository.deleteAllInBatch();
        numeroVenta = 0;
        rnd.setSeed(42);
        sembrar();
    }

    private void sembrar() {
        LocalDate hoy = LocalDate.now();

        Usuario ana = usuario("atorres", "Ana Torres", Rol.ADMIN, true);
        Usuario luis = usuario("lramos", "Luis Ramos", Rol.VENDEDOR, true);
        Usuario carla = usuario("cmedina", "Carla Medina", Rol.ADMIN, true);
        usuario("pvega", "Pedro Vega", Rol.VENDEDOR, false);

        empresaRepository.save(Empresa.builder()
                .razonSocial("Comercial Andina SAC").ruc("20601234567")
                .direccion("Av. Grau 1240, Lima").telefono("014567890")
                .igv(new BigDecimal("18")).serieBoleta("B001").build());

        Proveedor alicorp = proveedor("Alicorp SAA", "20100055237", "Jorge Villanueva", "987120450", 3);
        Proveedor lindley = proveedor("Corporación Lindley SA", "20101024645", "Patricia Ríos", "956778120", 2);
        Proveedor gloria = proveedor("Leche Gloria SA", "20100190797", "Martín Zegarra", "944501322", 2);
        Proveedor intradevco = proveedor("Intradevco Industrial SA", "20100162815", "Silvia Castro", "933210987", 4);
        Proveedor delSur = proveedor("Distribuidora Del Sur SAC", "20512876345", "Renato Flores", "921334567", 3);

        Categoria abarrotes = categoria("Abarrotes", alicorp);
        Categoria bebidas = categoria("Bebidas", lindley);
        Categoria lacteos = categoria("Lácteos", gloria);
        Categoria limpieza = categoria("Limpieza", intradevco);
        Categoria snacks = categoria("Snacks", delSur);

        List<Producto> productos = List.of(
                producto("Arroz Costeño 5 kg", "ABA-001", abarrotes, 0, 20, 120, "18.50", "23.90"),
                producto("Aceite Primor 1 L", "ABA-014", abarrotes, 6, 25, 150, "7.80", "10.50"),
                producto("Leche Gloria 400 g", "LAC-003", lacteos, 84, 48, 240, "3.20", "4.20"),
                producto("Azúcar Rubia 1 kg", "ABA-022", abarrotes, 41, 30, 200, "3.60", "4.80"),
                producto("Detergente Bolívar 2 kg", "LIM-007", limpieza, 0, 12, 80, "21.00", "27.50"),
                producto("Inca Kola 1.5 L", "BEB-011", bebidas, 132, 60, 300, "4.30", "6.50"),
                producto("Atún Florida 170 g", "ABA-031", abarrotes, 9, 36, 180, "4.90", "6.80"),
                producto("Papel Higiénico Elite x4", "LIM-012", limpieza, 57, 24, 120, "5.20", "7.20"),
                producto("Yogurt Laive 1 L", "LAC-009", lacteos, 3, 18, 90, "5.40", "7.40"),
                producto("Galletas Soda Field", "SNK-004", snacks, 210, 40, 160, "0.90", "1.50"),
                producto("Agua San Luis 625 ml", "BEB-002", bebidas, 0, 48, 240, "1.10", "2.00"),
                producto("Café Altomayo 200 g", "ABA-040", abarrotes, 22, 15, 60, "14.20", "19.90"),
                producto("Jabón Protex x3", "LIM-018", limpieza, 31, 20, 100, "8.10", "11.50"),
                producto("Mantequilla Gloria 200 g", "LAC-015", lacteos, 14, 16, 64, "6.00", "8.30"));
        for (Producto p : productos) {
            movimiento(hoy.minusDays(121).atTime(9, 0), p, TipoMovimiento.AJUSTE, p.getStockActual(), "Inventario inicial");
        }

        List<Cliente> activos = List.of(
                cliente("Bodega Don Lucho", "20545871236", "987654321", "pedidos@donlucho.pe", 48, "9840", 0),
                cliente("María Quispe Huamán", "45872136", "954112233", "", 31, "2310", 0),
                cliente("Restaurante El Tumi SAC", "20603344551", "962334455", "compras@eltumi.pe", 26, "7420", 1),
                cliente("Hostal Los Andes EIRL", "20609876543", "934556677", "reservas@losandes.pe", 15, "3980", 2),
                cliente("Carlos Huerta", "46598712", "912345678", "", 12, "1045", 3),
                cliente("Rosa Mendoza", "41236587", "999887766", "rosa.mendoza@gmail.com", 2, "96", 5),
                cliente("Lucía Paredes", "72145896", "", "lucia.p@hotmail.com", 1, "58", 1));
        cliente("Panadería La Espiga", "20512398745", "976543210", "", 22, "4870", 66);
        cliente("Minimarket Santa Rosa", "20481239087", "945667788", "admin@santarosa.pe", 18, "5130", 75);
        cliente("Jorge Salazar", "70214589", "", "", 4, "312", 103);

        List<Producto> vendibles = productos.stream().filter(p -> p.getStockActual() > 0).toList();
        for (int d = 120; d >= 1; d--) {
            LocalDate fecha = hoy.minusDays(d);
            int cantidad = switch (fecha.getDayOfWeek()) {
                case SATURDAY -> 10;
                case FRIDAY -> 8;
                case SUNDAY -> 4;
                default -> 6;
            } + rnd.nextInt(3);
            Usuario cajero = fecha.getDayOfWeek() == DayOfWeek.SUNDAY ? ana : luis;
            TurnoCaja turno = turnoRepository.save(TurnoCaja.builder().usuario(cajero)
                    .apertura(fecha.atTime(8, 0)).montoInicial(new BigDecimal("200"))
                    .estado(EstadoCaja.CERRADA).build());

            List<Venta> ventas = new ArrayList<>();
            for (int i = 0; i < cantidad; i++) {
                ventas.add(venta(fecha.atTime(8 + rnd.nextInt(12), rnd.nextInt(60)), turno, cajero, vendibles, activos));
            }
            ventaRepository.saveAll(ventas);
            cerrarTurno(turno, ventas, fecha.atTime(21, 0));
        }

        TurnoCaja turnoHoy = turnoRepository.save(TurnoCaja.builder().usuario(luis)
                .apertura(hoy.atTime(8, 2)).montoInicial(new BigDecimal("200"))
                .estado(EstadoCaja.ABIERTA).build());
        List<Venta> ventasHoy = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            ventasHoy.add(venta(hoy.atTime(9 + i / 2, (i * 23) % 60), turnoHoy, luis, vendibles, activos));
        }
        ventaRepository.saveAll(ventasHoy);
        for (Venta v : ventasHoy) {
            for (DetalleVenta dv : v.getDetalles()) {
                movimiento(v.getFecha(), dv.getProducto(), TipoMovimiento.VENTA, -dv.getCantidad(), v.getNumero());
            }
        }

        orden("OC-" + hoy.getYear() + "-0001", hoy.minusDays(26), intradevco, EstadoOrden.ANULADA, null,
                Map.of(productos.get(12), 60));
        orden("OC-" + hoy.getYear() + "-0002", hoy.minusDays(19), alicorp, EstadoOrden.RECIBIDA, hoy.minusDays(17),
                Map.of(productos.get(3), 150, productos.get(11), 40));
        orden("OC-" + hoy.getYear() + "-0003", hoy.minusDays(7), gloria, EstadoOrden.RECIBIDA, hoy.minusDays(5),
                Map.of(productos.get(2), 120, productos.get(13), 40));

        for (int m = 4; m >= 0; m--) {
            YearMonth mes = YearMonth.from(hoy).minusMonths(m);
            gasto(mes.atDay(1), CategoriaGasto.ALQUILER, "Alquiler local Av. Grau 1240", "3200", "factura-alquiler.pdf");
            gasto(mes.atDay(3), CategoriaGasto.SERVICIOS, "Luz - Enel", m == 0 ? "640" : "560", null);
            gasto(mes.atDay(4), CategoriaGasto.SERVICIOS, "Agua - Sedapal", "180", null);
            gasto(mes.atDay(6), CategoriaGasto.TRANSPORTE, "Flete proveedor Alicorp", m == 0 ? "820" : "640", "guia-remision.pdf");
            gasto(mes.atDay(8), CategoriaGasto.SERVICIOS, "Internet fibra 200 Mbps", "150", null);
            gasto(mes.atDay(15), CategoriaGasto.PLANILLA, "Planilla 1.ª quincena", "2150", null);
            if (m % 2 == 0) gasto(mes.atDay(17), CategoriaGasto.MARKETING, "Volantes y banner de promoción", "180", null);
            gasto(mes.atDay(20), CategoriaGasto.TRANSPORTE, "Flete proveedor Gloria", m == 0 ? "390" : "300", null);
            gasto(mes.atEndOfMonth().minusDays(1), CategoriaGasto.PLANILLA, "Planilla 2.ª quincena", "2150", null);
        }

        tarea("Generar orden de compra de abarrotes", EstadoTarea.PENDIENTE, Prioridad.ALTA, hoy.plusDays(1), ana, 0);
        tarea("Llamar a Minimarket Santa Rosa", EstadoTarea.PENDIENTE, Prioridad.MEDIA, hoy.plusDays(3), luis, 1);
        tarea("Renegociar tarifa de transporte", EstadoTarea.PENDIENTE, Prioridad.MEDIA, hoy.plusDays(5), null, 2);
        tarea("Armar combo galletas + bebida", EstadoTarea.PENDIENTE, Prioridad.BAJA, null, carla, 3);
        tarea("Inventario físico de limpieza", EstadoTarea.EN_PROCESO, Prioridad.ALTA, hoy.minusDays(2), luis, 0);
        tarea("Actualizar precios de lácteos", EstadoTarea.EN_PROCESO, Prioridad.MEDIA, hoy.plusDays(2), ana, 1);
        tarea("Pagar alquiler del mes", EstadoTarea.HECHO, Prioridad.ALTA, hoy.withDayOfMonth(1), ana, 0);
        tarea("Declaración mensual SUNAT", EstadoTarea.HECHO, Prioridad.ALTA, hoy.minusDays(5), carla, 1);

        log.info("Datos de demostración cargados: {} ventas.", numeroVenta);
    }

    private Usuario usuario(String login, String nombre, Rol rol, boolean activo) {
        return usuarioRepository.save(Usuario.builder().usuario(login).nombre(nombre)
                .passwordHash(AuthService.hash("1234")).rol(rol).activo(activo).build());
    }

    private Proveedor proveedor(String nombre, String ruc, String contacto, String tel, int plazo) {
        return proveedorRepository.save(Proveedor.builder().nombre(nombre).ruc(ruc)
                .contacto(contacto).telefono(tel).plazoEntregaDias(plazo).build());
    }

    private Categoria categoria(String nombre, Proveedor proveedor) {
        return categoriaRepository.save(Categoria.builder().nombre(nombre).proveedor(proveedor).build());
    }

    private Producto producto(String nombre, String sku, Categoria cat, int stock, int min, int max, String pc, String pv) {
        return productoRepository.save(Producto.builder().nombre(nombre).sku(sku).categoria(cat)
                .stockActual(stock).stockMinimo(min).stockMaximo(max)
                .precioCompra(new BigDecimal(pc)).precioVenta(new BigDecimal(pv)).build());
    }

    private Cliente cliente(String nombre, String doc, String tel, String email, int compras, String total, int diasUltima) {
        return clienteRepository.save(Cliente.builder().nombre(nombre).documento(doc).telefono(tel)
                .email(email).numeroCompras(compras).totalComprado(new BigDecimal(total))
                .ultimaCompra(LocalDate.now().minusDays(diasUltima)).build());
    }

    private Venta venta(LocalDateTime fecha, TurnoCaja turno, Usuario usuario, List<Producto> productos, List<Cliente> clientes) {
        double r = rnd.nextDouble();
        MetodoPago metodo = r < 0.45 ? MetodoPago.EFECTIVO : r < 0.70 ? MetodoPago.YAPE
                : r < 0.88 ? MetodoPago.TARJETA : MetodoPago.TRANSFERENCIA;
        Cliente cliente = rnd.nextDouble() < 0.35 ? clientes.get(rnd.nextInt(clientes.size())) : null;

        Venta v = Venta.builder().numero(String.format("V-%06d", ++numeroVenta)).fecha(fecha)
                .cliente(cliente).usuario(usuario).turno(turno).metodoPago(metodo)
                .estado(EstadoVenta.EMITIDA).total(BigDecimal.ZERO).build();

        int lineas = 1 + rnd.nextInt(4);
        Set<Producto> usados = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < lineas; i++) {
            Producto p = productos.get(rnd.nextInt(productos.size()));
            if (!usados.add(p)) continue;
            int cant = 1 + rnd.nextInt(cliente != null && cliente.getDocumento().length() == 11 ? 8 : 3);
            v.agregarDetalle(DetalleVenta.builder().producto(p).cantidad(cant)
                    .precioUnitario(p.getPrecioVenta()).costoUnitario(p.getPrecioCompra()).build());
            total = total.add(p.getPrecioVenta().multiply(BigDecimal.valueOf(cant)));
        }
        v.setTotal(total);
        return v;
    }

    private void cerrarTurno(TurnoCaja turno, List<Venta> ventas, LocalDateTime cierre) {
        BigDecimal total = ventas.stream().map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal efectivo = ventas.stream().filter(v -> v.getMetodoPago() == MetodoPago.EFECTIVO)
                .map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal esperado = turno.getMontoInicial().add(efectivo);
        BigDecimal[] diferencias = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("-10"), new BigDecimal("2.50"), new BigDecimal("-5")};
        BigDecimal dif = diferencias[rnd.nextInt(diferencias.length)];
        turno.setCierre(cierre);
        turno.setTotalVentas(total);
        turno.setNumeroVentas(ventas.size());
        turno.setEfectivoEsperado(esperado);
        turno.setEfectivoContado(esperado.add(dif).setScale(2, RoundingMode.HALF_UP));
        turno.setDiferencia(dif);
        turnoRepository.save(turno);
    }

    private void movimiento(LocalDateTime fecha, Producto p, TipoMovimiento tipo, int cantidad, String ref) {
        movimientoRepository.save(MovimientoStock.builder().fecha(fecha).producto(p).tipo(tipo)
                .cantidad(cantidad).saldo(p.getStockActual()).referencia(ref).build());
    }

    private void orden(String numero, LocalDate fecha, Proveedor prov, EstadoOrden estado, LocalDate recepcion,
                       Map<Producto, Integer> lineas) {
        OrdenCompra o = OrdenCompra.builder().numero(numero).fecha(fecha).proveedor(prov)
                .estado(estado).fechaRecepcion(recepcion).build();
        lineas.forEach((p, cant) -> o.agregarDetalle(DetalleOrden.builder()
                .producto(p).cantidad(cant).costoUnitario(p.getPrecioCompra()).build()));
        ordenRepository.save(o);
        if (estado == EstadoOrden.RECIBIDA) {
            lineas.forEach((p, cant) -> movimiento(recepcion.atTime(10, 0), p, TipoMovimiento.COMPRA, cant, numero));
        }
    }

    private void gasto(LocalDate fecha, CategoriaGasto cat, String desc, String monto, String comprobante) {
        if (fecha.isAfter(LocalDate.now())) return;
        gastoRepository.save(Gasto.builder().fecha(fecha).categoria(cat).descripcion(desc)
                .monto(new BigDecimal(monto)).comprobante(comprobante).build());
    }

    private void tarea(String titulo, EstadoTarea estado, Prioridad prioridad, LocalDate limite, Usuario resp, int orden) {
        tareaRepository.save(Tarea.builder().titulo(titulo).estado(estado).prioridad(prioridad)
                .fechaLimite(limite).responsable(resp).orden(orden).build());
    }
}
