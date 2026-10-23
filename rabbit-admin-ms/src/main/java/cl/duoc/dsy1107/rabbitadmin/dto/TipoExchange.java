package cl.duoc.dsy1107.rabbitadmin.dto;

import org.springframework.amqp.core.ExchangeTypes;

public enum TipoExchange {
    DIRECT(ExchangeTypes.DIRECT),
    TOPIC(ExchangeTypes.TOPIC),
    FANOUT(ExchangeTypes.FANOUT),
    HEADERS(ExchangeTypes.HEADERS);

    private final String tipoAmqp;

    TipoExchange(String tipoAmqp) {
        this.tipoAmqp = tipoAmqp;
    }

    public String tipoAmqp() {
        return tipoAmqp;
    }
}
