package br.com.infotera.gerarcertificado.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Nested
@SpringBootTest(classes = ModelMapperConfig.class)
class ModelMapperConfigTest {

    @Autowired
    private ModelMapper modelMapper;

    @Test
    void deveInjetarModelMapper() {
        assertNotNull(modelMapper);
    }
}