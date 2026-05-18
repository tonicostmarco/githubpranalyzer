package com.tonicostmarco.githubpranalyzer.controller;

import com.tonicostmarco.githubpranalyzer.dtos.messaging.PrEventMessage;
import com.tonicostmarco.githubpranalyzer.dtos.messaging.PrWebhookPayload;
import com.tonicostmarco.githubpranalyzer.factory.PrWebhookPayloadFactory;
import com.tonicostmarco.githubpranalyzer.services.PrEventProducer;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;


@WebMvcTest(PrEventController.class)
@TestPropertySource(properties = "github.webhook.secret=test-secret")
public class PrEventControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PrEventProducer producer;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String SECRET = "test-secret";

    private String signature(byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
    }

    @Test
    void shouldReturn200AndCallProducer_whenValidRequest() throws Exception {
        PrWebhookPayload payload = PrWebhookPayloadFactory.createPayload();

        byte[] bodyBytes = objectMapper.writeValueAsBytes(payload);

        mockMvc.perform(post("/webhook/notify")
                        .header("X-GitHub-Delivery", "test-delivery-id")
                        .header("X-Hub-Signature-256", signature(bodyBytes))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyBytes))
                .andExpect(status().isOk());

        ArgumentCaptor<PrEventMessage> captor = ArgumentCaptor.forClass(PrEventMessage.class);
        verify(producer).send(captor.capture());
        assertEquals("test-delivery-id", captor.getValue().deliveryId());
    }

    @Test
    void shouldReturn401_whenInvalidSignature() throws Exception {
        PrWebhookPayload payload = PrWebhookPayloadFactory.createPayload();

        mockMvc.perform(post("/webhook/notify")
                        .header("X-GitHub-Delivery", "test-delivery-id")
                        .header("X-Hub-Signature-256", "sha256=invalido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn400_whenMissingDeliveryHeader() throws Exception {
        PrWebhookPayload payload = PrWebhookPayloadFactory.createPayload();
        byte[] bodyBytes = objectMapper.writeValueAsBytes(payload);

        mockMvc.perform(post("/webhook/notify")
                        .header("X-Hub-Signature-256", signature(bodyBytes))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyBytes))
                .andExpect(status().isBadRequest());
    }
}
