import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class Administrator {
    private final static String QUEUE_ADMIN = "admin";
    private final static String EXCHANGE_HOSPITAL = "hospital_channel";
    private final static String ADMIN_ID = UUID.randomUUID().toString();

    public static void main(String[] argv) throws Exception {
        // info
        System.out.println("I AM AN ADMINISTRATOR");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // EXCHANGE
        channel.exchangeDeclare(EXCHANGE_HOSPITAL, BuiltinExchangeType.TOPIC);


        // RECEIVE ALL MESSAGES
        channel.queueDeclare(QUEUE_ADMIN + ADMIN_ID, false, false, true, null);
        channel.queueBind(QUEUE_ADMIN + ADMIN_ID, EXCHANGE_HOSPITAL, "#.admin.log.#");

        // CONSUMER
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("log: " + message);
            }
        };

        // PROGRAM
        channel.basicConsume(QUEUE_ADMIN + ADMIN_ID, true, consumer);

        while (true) {
            // read new
            System.out.println("Send message to all :");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String message = br.readLine();
            if (message.contains("exitChannel")) {
                break;
            }
                channel.basicPublish(EXCHANGE_HOSPITAL, EXCHANGE_HOSPITAL + ".admin.info", null, message.getBytes("UTF-8"));
                System.out.println("Sent: " + message);

        }

        channel.close();
        connection.close();
    }
}

