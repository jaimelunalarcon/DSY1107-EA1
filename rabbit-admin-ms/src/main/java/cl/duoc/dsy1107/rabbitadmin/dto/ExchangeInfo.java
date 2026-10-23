package cl.duoc.dsy1107.rabbitadmin.dto;

public record ExchangeInfo(String nombre, TipoExchange tipo, boolean durable, boolean autoDelete) {

    public static ExchangeInfo de(CrearExchangeRequest datos) {
        return new ExchangeInfo(datos.nombre(), datos.tipo(), datos.durableEfectivo(), datos.autoDeleteEfectivo());
    }
}
