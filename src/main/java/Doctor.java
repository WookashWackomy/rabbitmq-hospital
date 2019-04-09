import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Doctor {

    public static void main(String[] argv) throws Exception {

        // info
        System.out.println("I AM A DOCTOR");

        // connection & channel
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // exchange
        String EXCHANGE_NAME = "hospital_channel";
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

        String key = ""; // TODO

        // queue & bind
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, key);
        System.out.println("binded to : " + queueName);

        while (true) {

            // read msg
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Enter message: ");
            String examinationType = br.readLine();

            System.out.println("Enter pacient's name:");
            String patientName = br.readLine();

            // break condition
            if ("exit".equals(examinationType)) {
                break;
            }

            String message = examinationType.concat(".").concat(patientName);

            // publish
            channel.basicPublish(EXCHANGE_NAME, key, null, message.getBytes(StandardCharsets.UTF_8));
            System.out.println("Sent: " + message);
        }


        //message handling
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };

        // start listening
        System.out.println("Waiting for messages...");
        channel.basicConsume(queueName, true, deliverCallback, consumerTag ->{});

    }
}
