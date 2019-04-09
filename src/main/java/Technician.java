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



        // queue & bind
        String queueName = channel.queueDeclare().getQueue();
        for(String examinationType : examinationTypes) {
            String key = "toTech.type." + examinationType + ".name.*"; // TODO
            channel.queueBind(queueName, EXCHANGE_NAME, key);
        }

        System.out.println("binded to : " + queueName);

        //message handling
        Object monitor = new Object();
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(delivery.getProperties().getCorrelationId())
                    .build();

            String response = "";

            try {
                String message = new String(delivery.getBody(), "UTF-8");

                System.out.println(" [.] " + message);
                response = message + " done";
            } catch (RuntimeException e) {
                System.out.println(" [.] " + e.toString());
            } finally {
                channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, response.getBytes("UTF-8"));
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                // RabbitMq consumer worker thread notifies the RPC server owner thread
                synchronized (monitor) {
                    monitor.notify();
                }
            }
        };

        System.out.println("Waiting for messages...");
        channel.basicConsume(queueName, false, deliverCallback, (consumerTag -> { }));


        // Waiting for the messages
        while (true) {
            synchronized (monitor) {
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
