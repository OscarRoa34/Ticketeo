package co.edu.uptc.Ticketeo.user.models;

public enum DocumentType {
    CC("Cedula de ciudadania"),
    PASSPORT("Pasaporte");

    private final String label;

    DocumentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

