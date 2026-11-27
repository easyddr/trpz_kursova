package editor;

import com.example.EncodingService;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EncodingServiceTest {

    private static final Charset WIN1251 = Charset.forName("windows-1251");
    private static final String TEXT = "Привіт, світе!";

    private final EncodingService service = new EncodingService();

    @Test
    void asciiIsReportedAsUtf8() throws Exception {
        assertEquals("UTF-8", service.detectEncoding("plain ascii\n".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    void utf8WithCyrillicIsDetected() throws Exception {
        assertEquals("UTF-8", service.detectEncoding(TEXT.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void windows1251IsDetected() throws Exception {
        assertEquals("windows-1251", service.detectEncoding(TEXT.getBytes(WIN1251)));
    }

    @Test
    void bomSelectsTheEncodingImmediately() throws Exception {
        assertEquals("UTF-8", service.detectEncoding(
                new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a'}));
        assertEquals("UTF-16LE", service.detectEncoding(
                new byte[]{(byte) 0xFF, (byte) 0xFE, 'a', 0}));
        assertEquals("UTF-16BE", service.detectEncoding(
                new byte[]{(byte) 0xFE, (byte) 0xFF, 0, 'a'}));
    }

    @Test
    void binaryDataIsRejected() {
        byte[] binary = {0x50, 0x4B, 0x03, 0x04, 0x00, 0x00, 0x01};

        assertThrows(UnsupportedEncodingException.class, () -> service.detectEncoding(binary));
    }

    @Test
    void strategiesAreReplaceable() {
        EncodingService utf8Only = new EncodingService(List.of(new Utf8Strategy()));

        assertThrows(UnsupportedEncodingException.class,
                () -> utf8Only.detectEncoding(TEXT.getBytes(WIN1251)));
    }

    @Test
    void utf16WithoutBomIsDetected() throws Exception {
        assertEquals("UTF-16LE", service.detectEncoding(TEXT.getBytes(StandardCharsets.UTF_16LE)));
        assertEquals("UTF-16BE", service.detectEncoding(TEXT.getBytes(StandardCharsets.UTF_16BE)));
    }

    @Test
    void unknownSingleByteDataFallsBackToIso88591() throws Exception {
        assertEquals("ISO-8859-1", service.detectEncoding("caf\u00e9 \u0098 x".getBytes(StandardCharsets.ISO_8859_1)));
    }
}
