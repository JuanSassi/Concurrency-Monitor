public enum ClientStates {
    PENDING_ENTRY,          // Cliente por ingresar
    ENTERED,                // Cliente ingresado
    RESERVATION_MANAGING,   // managing reservation
    WITH_RESERVATION,               // with reservation
    CONFIRMED,              // Cliente confirmado
    CANCELED,               // Cliente cancelado
    PAID                    // Cliente pagó
}