package br.com.infotera.gerarcertificado.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SimpleMultipartFile {

    private String filename;
    private String nameArchive;
    private String contentType;
    private byte[] content;


    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }
}
