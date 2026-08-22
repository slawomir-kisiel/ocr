package pl.sk.ocr.adapter.tess4j;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HocrParserTest {
    @Test
    void parsesAreasLinesWordsAndRawHocr() {
        var hocr = """
            <html>
              <body>
                <div class='ocr_page' title='bbox 0 0 800 1000'>
                  <div class='ocr_carea' title='bbox 10 20 300 80'>
                    <p class='ocr_par' title='bbox 10 20 300 80'>
                      <span class='ocr_line' title='bbox 10 20 300 45'>
                        <span class='ocrx_word' title='bbox 10 20 90 45; x_wconf 96'>Invoice</span>
                        <span class='ocrx_word' title='bbox 100 20 170 45; x_wconf 88'>123</span>
                      </span>
                    </p>
                  </div>
                </div>
              </body>
            </html>
            """;

        var text = new HocrParser().parse(hocr);

        assertThat(text.value()).isEqualTo("Invoice 123");
        assertThat(text.hocr()).isEqualTo(hocr);
        assertThat(text.areas()).hasSize(1);
        assertThat(text.lines()).hasSize(1);
        assertThat(text.words()).hasSize(2);
        assertThat(text.words().getFirst().text()).isEqualTo("Invoice");
        assertThat(text.words().getFirst().confidence().value()).isEqualTo(0.96);
        assertThat(text.words().getFirst().boundingBox().region().x()).isEqualTo(10);
        assertThat(text.words().getFirst().boundingBox().region().width()).isEqualTo(80);
    }
}
