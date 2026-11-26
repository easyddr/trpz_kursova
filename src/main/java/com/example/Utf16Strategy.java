package com.example;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;

public class Utf16Strategy implements EncodingStrategy {

    private final String name;
    private final int highByteOffset;

    public Utf16Strategy(boolean littleEndian) {
        this.name = littleEndian ? "UTF-16LE" : "UTF-16BE";
        this.highByteOffset = littleEndian ? 1 : 0;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean supports(byte[] bytes) {
        if (bytes.length < 4 || bytes.length % 2 != 0) {
            return false;
        }
        int units = bytes.length / 2;
        int plausible = 0;
        for (int i = 0; i < units; i++) {
            int high = bytes[2 * i + highByteOffset] & 0xFF;
            if (high == 0x00 || high == 0x04) {
                plausible++;
            }
        }
        if (plausible * 10 < units * 9) {
            return false;
        }
        try {
            String text = Charset.forName(name).newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c < 0x20 && c != '\t' && c != '\n' && c != '\r' && c != '\f') {
                    return false;
                }
            }
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }
}
