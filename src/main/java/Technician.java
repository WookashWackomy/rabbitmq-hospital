import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Technician {

    private static int maxExaminationTypes = 2;

    public static void main(String[] argv) throws Exception {
        System.out.println("I AM A TECHNICIAN");


        ArrayList<String> examinationTypes = new ArrayList<String>();

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter examination types: ");
        for(int i = 0; i<maxExaminationTypes; i++) {
            examinationTypes.add(br.readLine());
        }

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


        //message handling
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");

            channel.basicPublish(EXCHANGE_NAME, key, null, message.getBytes(StandardCharsets.UTF_8));
            System.out.println("Sent: " + message);
        };

        // start listening
        System.out.println("Waiting for messages...");
        channel.basicConsume(queueName, true, deliverCallback, consumerTag ->{});


    }
}
