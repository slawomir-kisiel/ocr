package pl.sk.ocr.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import pl.sk.ocr.config.dto.CsvOutputDto;

class JsonConfigurationMapperTest {

    @Test
    void writesPrettyJsonWithFinalNewline() {
        var json = new JsonConfigurationMapper().write(new CsvOutputDto("./out.csv", "UTF-8", ";", "\"", true, false));

        assertThat(json).contains(System.lineSeparator());
        assertThat(json).endsWith(System.lineSeparator());
        assertThat(json).contains("\"charset\"");
    }
}
