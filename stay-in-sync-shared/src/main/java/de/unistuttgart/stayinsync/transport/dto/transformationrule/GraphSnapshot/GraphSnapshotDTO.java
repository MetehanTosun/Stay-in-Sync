package de.unistuttgart.stayinsync.transport.dto.transformationrule.GraphSnapshot;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import de.unistuttgart.stayinsync.transport.dto.transformationrule.GraphDTO;
import de.unistuttgart.stayinsync.transport.exception.GraphEvaluationException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)

/**
 * Snapshot der Ausführung eines Logic Graphs zu einem bestimmten Zeitpunkt.
 * um im UI den Zustand während und zum Zeitpunkt eines Fehlers nachvollziehen
 * zu können.
 */
public class GraphSnapshotDTO {
    /**
     * Eindeutige ID des Snapshots.
     * Dient zur Identifikation und Referenzierung des Snapshots.
     */
    private String snapshotId;

    /**
     * ID des Jobs, zu dem dieser Snapshot gehört.
     * Hilft bei der Zuordnung von Snapshots zu übergeordneten Ausführungsprozessen.
     */
    private String jobId;

    /**
     * Zeitstempel, wann der Snapshot erstellt wurde.
     * Gibt den exakten Zeitpunkt der Snapshot-Erfassung an.
     */
    private Instant createdAt;

    /**
     * Statischer Zustand des Graphen zur Laufzeit.
     * Enthält die Struktur und Konfiguration des Graphen, wie sie bei der
     * Ausführung vorlag.
     */
    private GraphDTO graph;

    /**
     * Runtime-Inputs des Graphen als Map von Schlüssel zu JSON-Werten.
     * Bei Redaction werden nur die Schlüssel gefüllt, um sensible Daten zu
     * schützen.
     */
    private Map<String, JsonNode> dataContext;

    /**
     * Titel der aufgetretenen Fehlersituation.
     * Kurzbeschreibung des Fehlers für UI und Logs.
     */
    private String errorTitle;

    /**
     * Ausführliche Fehlermeldung.
     * Detaillierte Beschreibung des Fehlers zur Diagnose.
     */
    private String errorMessage;

    /**
     * Typ des Fehlers, klassifiziert nach GraphEvaluationException.ErrorType.
     * Ermöglicht differenzierte Fehlerbehandlung und -anzeige.
     */
    private GraphEvaluationException.ErrorType errorType;

}
