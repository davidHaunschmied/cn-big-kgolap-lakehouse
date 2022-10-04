package at.jku.dke.bigkgolap.shared.messaging;

import at.jku.dke.bigkgolap.shared.config.LakehouseConfig;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SqsMessagingService implements MessagingService {

    private final AmazonSQS sqs;
    private final String fileIngestionQueue;

    public SqsMessagingService(LakehouseConfig config) {
        this.sqs = AmazonSQSClientBuilder.standard()
                .withRegion(config.getFileIngestionSqsQueueRegion())
                .build();
        this.fileIngestionQueue = sqs.getQueueUrl(config.getFileIngestionSqsQueue()).getQueueUrl();
    }

    @Override
    public void registerNewFile(String storedName, String fileType) {
        this.sqs.sendMessage(fileIngestionQueue, toSqsMessage(storedName, fileType));
    }

    @Override
    public List<FileIngestMessage> pollNextFiles(int max) {
        return this.sqs.receiveMessage(new ReceiveMessageRequest().withQueueUrl(fileIngestionQueue)
                .withMaxNumberOfMessages(max))
                .getMessages().stream().map(msg -> {
                    String[] messageParts = fromSqsMessage(msg.getBody());
                    return new FileIngestMessage(messageParts[0], messageParts[1], msg.getReceiptHandle());
                })
                .collect(Collectors.toList());
    }

    @Override
    public void deleteMessage(String receiptHandle) {
        this.sqs.deleteMessage(fileIngestionQueue, receiptHandle);
    }

    private static String toSqsMessage(String... messageParts) {
        return String.join("::", messageParts);
    }

    private static String[] fromSqsMessage(String message) {
        return message.split("::");
    }
}
