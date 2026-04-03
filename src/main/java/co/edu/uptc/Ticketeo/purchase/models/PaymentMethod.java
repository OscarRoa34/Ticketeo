package co.edu.uptc.Ticketeo.purchase.models;

public enum PaymentMethod {
    CARD("Tarjeta"),
    PSE("PSE"),
    CASH("Efectivo");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static PaymentMethod fromValue(String value) {
        if (value == null) {
            return CARD;
        }
        try {
            return PaymentMethod.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return CARD;
        }
    }
}

