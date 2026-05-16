package com.memoreal.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;

@Slf4j
@Service
public class ClovaSpeechService {

    @Value("${clova.api-url}")
    private String apiUrl;

    @Value("${clova.secret-key}")
    private String secretKey;

    @Value("${clova.language}")
    private String language;

    private final ObjectMapper objectMapper;

    public ClovaSpeechService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 음성 파일 → 텍스트 변환 (CLOVA Speech)
     * 어르신 한국어 억양에 특화된 STT
     *
     * @param audioFile 앱에서 녹음한 음성 파일 (wav, mp3, m4a 등)
     * @return 변환된 텍스트
     */
    public String transcribe(MultipartFile audioFile) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            // Content-Type은 음성 파일 형식에 맞게 설정
            String contentType = resolveContentType(audioFile.getOriginalFilename());

            HttpPost request = new HttpPost(apiUrl + "?lang=" + language);
            request.setHeader("X-CLOVASPEECH-API-GW-SERVICE-SECRET", secretKey);
            request.setHeader("Content-Type", contentType);

            byte[] audioBytes = audioFile.getBytes();
            request.setEntity(new ByteArrayEntity(audioBytes, ContentType.create(contentType)));

            log.info("CLOVA STT 요청 - 파일크기: {}bytes, contentType: {}", audioBytes.length, contentType);

            return httpClient.execute(request, response -> {
                int statusCode = response.getCode();
                InputStream responseBody = response.getEntity().getContent();
                String responseStr = new String(responseBody.readAllBytes());

                log.debug("CLOVA STT 응답 - status: {}, body: {}", statusCode, responseStr);

                if (statusCode != 200) {
                    log.error("CLOVA STT 실패 - status: {}, body: {}", statusCode, responseStr);
                    throw new RuntimeException("CLOVA STT 실패: " + statusCode);
                }

                JsonNode json = objectMapper.readTree(responseStr);
                String text = json.path("text").asText();

                log.info("CLOVA STT 성공 - 변환 텍스트: {}", text);
                return text;
            });

        } catch (Exception e) {
            log.error("CLOVA STT 오류: {}", e.getMessage(), e);
            throw new RuntimeException("음성 인식 실패: " + e.getMessage());
        }
    }

    private String resolveContentType(String filename) {
        if (filename == null) return "audio/wav";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".mp3"))  return "audio/mpeg";
        if (lower.endsWith(".m4a"))  return "audio/mp4";
        if (lower.endsWith(".aac"))  return "audio/aac";
        if (lower.endsWith(".flac")) return "audio/flac";
        if (lower.endsWith(".ogg"))  return "audio/ogg";
        return "audio/wav"; // 기본값
    }
}
