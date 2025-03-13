package com.example.demo;

import lombok.Data;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class TextStripper extends PDFTextStripper {

    private List<Map<String, Object>> fontData = new ArrayList<>();

    public TextStripper() throws IOException {
        super();
        this.fontData = new ArrayList<>();  // 기본 리스트 초기화
    }

    @Override
    protected void processTextPosition(TextPosition text) {
        Map<String, Object> fontInfo = new HashMap<>();
        fontInfo.put("page", this.getCurrentPageNo());
        fontInfo.put("text", text.getUnicode());
        fontInfo.put("size", text.getFontSizeInPt());
        fontInfo.put("width", text.getWidth());
        fontInfo.put("x", (int)text.getX());
        fontInfo.put("y", (int)text.getY());
        fontData.add(fontInfo);
    }

}
