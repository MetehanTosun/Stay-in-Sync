package de.unistuttgart.stayinsync.core.configuration.rest;

import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jboss.resteasy.reactive.RestForm;

/**
 * Represents the multipart form used for uploading an OpenAPI specification file.
 * <p>
 * This DTO is bound automatically when a request with Content-Type
 * multipart/form-data is sent to the upload endpoint. The uploaded file
 * is accessible via the <code>file</code> field.
 * </p>
 */
public class OpenApiUploadForm {

    /**
     * The uploaded OpenAPI specification file.
     * Must be provided in the form field named "file".
     */
    @RestForm("file")
    public FileUpload file;


       @RestForm("url")
    public String openApiSpecUrl;

    /**
     * Read the entire uploaded file into a byte array.
     * @return the raw bytes of the uploaded OpenAPI spec
     * @throws IOException if reading fails
     */
    public byte[] uploadedFileBytes() throws IOException {
        Path path = file.uploadedFile();
        return Files.readAllBytes(path);
    }
}