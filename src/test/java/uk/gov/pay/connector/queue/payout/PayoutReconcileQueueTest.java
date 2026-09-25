package uk.gov.pay.connector.queue.payout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import uk.gov.pay.connector.app.ConnectorConfiguration;
import uk.gov.pay.connector.app.SqsConfig;
import uk.gov.pay.connector.app.config.PayoutReconcileProcessConfig;
import uk.gov.service.payments.commons.queue.exception.QueueException;
import uk.gov.service.payments.commons.queue.model.QueueMessage;
import uk.gov.service.payments.commons.queue.sqs.SqsQueueService;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.connector.queue.payout.AdyenPayoutReconciliationPayloadFixture.anAdyenPayoutReconciliationPayloadFixture;

@ExtendWith(MockitoExtension.class)
class PayoutReconcileQueueTest {

    private static ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SqsQueueService sqsQueueService;
    @Mock
    private ConnectorConfiguration connectorConfiguration;
    @Captor
    ArgumentCaptor<String> payloadArgumentCaptor;

    private PayoutReconcileQueue payoutReconcileQueue;

    @BeforeEach
    void setUp() {
        PayoutReconcileProcessConfig payoutReconcileProcessConfig = mock(PayoutReconcileProcessConfig.class);
        SqsConfig sqsConfig = mock(SqsConfig.class);
        when(sqsConfig.getPayoutReconcileQueueUrl()).thenReturn("");
        when(connectorConfiguration.getSqsConfig()).thenReturn(sqsConfig);
        when(connectorConfiguration.getPayoutReconcileProcessConfig()).thenReturn(payoutReconcileProcessConfig);

        payoutReconcileQueue = new PayoutReconcileQueue(sqsQueueService, connectorConfiguration, objectMapper);
    }

    @Test
    void shouldParseStripePayoutFromQueueGivenWellFormattedJSON() throws QueueException {
        String validJsonMessage = "{ \"gateway_payout_id\": \"payout-id\", \"connect_account_id\": \"connect-accnt-id\",\"payment_provider\":\"stripe\",\"created_date\":\"2020-05-01T10:30:00.000000Z\"}";
        SendMessageResponse messageResult = mock(SendMessageResponse.class);

        List<QueueMessage> messages = Arrays.asList(
                QueueMessage.of(messageResult, validJsonMessage)
        );
        when(sqsQueueService.receiveMessages(anyString(), anyString())).thenReturn(messages);

        List<PayoutReconcileMessage> payoutReconcileMessages = payoutReconcileQueue.retrievePayoutMessages();


        assertNotNull(payoutReconcileMessages);
        assertInstanceOf(StripePayoutReconciliationPayload.class, payoutReconcileMessages.getFirst().getPayout());
        assertEquals("payout-id", payoutReconcileMessages.getFirst().getGatewayPayoutId());
        assertEquals("connect-accnt-id", payoutReconcileMessages.getFirst().getConnectAccountId());
        assertEquals("stripe", payoutReconcileMessages.getFirst().getPaymentProvider());
        assertEquals(Instant.parse("2020-05-01T10:30:00.000Z"), payoutReconcileMessages.getFirst().getCreatedDate());
    }

    @Test
    void shouldParseAdyenPayoutFromQueueGivenWellFormattedJSON() throws QueueException {
        String validJsonMessage = """
                {
                  "payment_provider": "adyen",
                  "report_type": "balanceplatform_payout_report",
                  "balance_platform": "pay_test",
                  "creation_date": "2026-09-24T00:04:12+02:00",
                  "download_url": "some-url",
                  "file_name": "balanceplatform_payout_report_2026_09_24.csv",
                  "environment": "live"
                }
                """;
        SendMessageResponse messageResult = mock(SendMessageResponse.class);

        List<QueueMessage> messages = List.of(
                QueueMessage.of(messageResult, validJsonMessage)
        );
        when(sqsQueueService.receiveMessages(anyString(), anyString())).thenReturn(messages);

        List<PayoutReconcileMessage> payoutReconcileMessages = payoutReconcileQueue.retrievePayoutMessages();

        assertNotNull(payoutReconcileMessages);
        assertInstanceOf(AdyenPayoutReconciliationPayload.class, payoutReconcileMessages.getFirst().getPayout());
        AdyenPayoutReconciliationPayload payout = (AdyenPayoutReconciliationPayload) payoutReconcileMessages.getFirst().getPayout();
        assertEquals("adyen", payout.getPaymentProvider());
        assertEquals("balanceplatform_payout_report", payout.getReportType());
        assertEquals("pay_test", payout.getBalancePlatform());
        assertEquals("2026-09-24T00:04:12+02:00", payout.getCreationDate());
        assertEquals("some-url", payout.getDownloadUrl());
        assertEquals("balanceplatform_payout_report_2026_09_24.csv", payout.getFileName());
        assertEquals("live", payout.getEnvironment());
    }

    @Test
    void shouldSendValidSerialisedStripePayoutToQueue() throws QueueException, JsonProcessingException {
        StripePayoutReconciliationPayload payout = new StripePayoutReconciliationPayload("payout-id", "connect-accnt-id", Instant.parse("2020-05-01T10:30:00.000Z"));
        when(sqsQueueService.sendMessage(anyString(), anyString())).thenReturn(mock(QueueMessage.class));

        payoutReconcileQueue.sendPayout(payout);

        verify(sqsQueueService).sendMessage(connectorConfiguration.getSqsConfig().getPayoutReconcileQueueUrl(),
                "{\"payment_provider\":\"stripe\",\"gateway_payout_id\":\"payout-id\",\"connect_account_id\":\"connect-accnt-id\",\"created_date\":\"2020-05-01T10:30:00.000000Z\"}");
    }

    @Test
    void shouldSendValidSerialisedAdyenPayoutToQueue() throws QueueException, JsonProcessingException {
        AdyenPayoutReconciliationPayload payout = anAdyenPayoutReconciliationPayloadFixture().build();
        when(sqsQueueService.sendMessage(anyString(), anyString())).thenReturn(mock(QueueMessage.class));

        payoutReconcileQueue.sendPayout(payout);

        verify(sqsQueueService).sendMessage(any(), payloadArgumentCaptor.capture());

        String payload = payloadArgumentCaptor.getValue();

        assertThat(payload, hasJsonPath("$.payment_provider", equalTo("adyen")));
        assertThat(payload, hasJsonPath("$.report_type", equalTo("balanceplatform_payout_report")));
        assertThat(payload, hasJsonPath("$.balance_platform", equalTo("pay_test")));
        assertThat(payload, hasJsonPath("$.creation_date", equalTo("2026-09-24T00:04:12+02:00")));
        assertThat(payload, hasJsonPath("$.download_url", equalTo("https://some-url")));
        assertThat(payload, hasJsonPath("$.file_name", equalTo("balanceplatform_payout_report_2026_09_24.csv")));
        assertThat(payload, hasJsonPath("$.environment", equalTo("test")));
    }
}
