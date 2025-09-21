package de.unistuttgart.stayinsync.util;


import org.slf4j.MDC;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Utility zum einfachen Setzen einer TransformationId im MDC für Logs.
 */
public class MdcUtil {

    private static final String TRANSFORMATION_ID_KEY = "transformationId";

    /**
     * Führt eine Aktion aus, nachdem die transformationId im MDC gesetzt wurde.
     * Entfernt die transformationId danach automatisch.
     *
     * @param transformationId TransformationId, die im MDC gesetzt werden soll
     * @param action Aktion als Runnable (keine Rückgabe)
     */
    public static void withTransformationId(String transformationId, Runnable action) {
        MDC.put(TRANSFORMATION_ID_KEY, transformationId);
        try {
            action.run();
        } finally {
            MDC.remove(TRANSFORMATION_ID_KEY);
        }
    }

    /**
     * Führt eine Aktion aus, nachdem die transformationId im MDC gesetzt wurde.
     * Entfernt die transformationId danach automatisch.
     * @param transformationId TransformationId
     * @param supplier Aktion als Supplier, der ein Ergebnis liefert
     * @param <T> Rückgabetyp
     * @return Ergebnis der Aktion
     */
    public static <T> T withTransformationId(String transformationId, Supplier<T> supplier) {
        MDC.put(TRANSFORMATION_ID_KEY, transformationId);
        try {
            return supplier.get();
        } finally {
            MDC.remove(TRANSFORMATION_ID_KEY);
        }
    }

    /**
     * Führt eine Aktion aus, die Exceptions werfen kann, nachdem die transformationId im MDC gesetzt wurde.
     * Entfernt die transformationId danach automatisch.
     *
     * @param transformationId TransformationId
     * @param callable Aktion als Callable
     * @param <T> Rückgabetyp
     * @return Ergebnis der Aktion
     * @throws Exception Weitergabe von Exceptions aus der Aktion
     */
    public static <T> T withTransformationIdThrows(String transformationId, Callable<T> callable) throws Exception {
        MDC.put(TRANSFORMATION_ID_KEY, transformationId);
        try {
            return callable.call();
        } finally {
            MDC.remove(TRANSFORMATION_ID_KEY);
        }
    }
}
