import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import com.rabbitmq.client.AMQP.BasicProperties;

public class Doctor{
    private Channel channel;
    private Connection connection;
    private static String EXCHANGE_NAME = "hospital_channel";
    private final static String QUEUE_PREFIX = "doctor_";
    private final static String QUEUE_ADMIN = "admin";
    private final static String corrId = UUID.randomUUID().toString();


    public static void main(String[] argv) throws IOException, TimeoutException {
        System.out.println("I AM A DOCTOR");

        AMQP.BasicProperties props = new BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(corrId)
                .build();


        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();


        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

        channel.queueDeclare(QUEUE_PREFIX + corrId, false, false, true, null);
        channel.queueBind(QUEUE_PREFIX + corrId, EXCHANGE_NAME, "#." + corrId + ".#");

        channel.queueDeclare(QUEUE_ADMIN + corrId, false, false, true, null);
        channel.queueBind(QUEUE_ADMIN + corrId, EXCHANGE_NAME, EXCHANGE_NAME + ".admin.info.#");


        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("Received from TECH: " + message);
            }
        };

        Consumer consumerAdmin = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("Received from ADMIN: " + message);
            }
        };

        channel.basicConsume(QUEUE_PREFIX + corrId, true, consumer);
        channel.basicConsume(QUEUE_ADMIN + corrId, true, consumerAdmin);

        while (true) {
            System.out.println("Enter examination type");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String examinationType = br.readLine();

            System.out.println("Enter patient name");
            String patient = br.readLine();

            String message =  examinationType + " " + patient;

            if (message.contains("exitChannel")) {
                break;
            }
            // send
            if (message.contains("knee") || message.contains("elbow") || message.contains("hip")) {
                channel.basicPublish(EXCHANGE_NAME, EXCHANGE_NAME + ".tech." + examinationType, props, message.getBytes("UTF-8"));
                channel.basicPublish(EXCHANGE_NAME, EXCHANGE_NAME + ".admin.log." + examinationType, props, message.getBytes("UTF-8"));
                System.out.println("Sent: " + message);
            } else {
                System.out.println("Invalid examination type");
            }

        }

        channel.close();
        connection.close();
    }
}
