package shareyourstory.domain.event.model;

// Tipo de cuestionario embebido en un evento: opcion multiple (CHOICE) o texto
// libre (TEXT). Se serializa en minusculas hacia el front ("choice"/"text").
public enum FormKind {
    CHOICE,
    TEXT
}
