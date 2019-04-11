import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.UUID;

public class Technician {

    private static int maxExaminationTypes = 2;
    private final static String EXCHANGE_HOSPITAL = "hospital_channel";
    private final static String QUEUE_PREFIX = "tech_";
    private final static String TECH_ID = UUID.randomUUID().toString();

    public static void main(String[] argv) throws Exception {
        System.out.println("I AM A TECHNICIAN");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();


        channel.exchangeDeclare(EXCHANGE_HOSPITAL, BuiltinExchangeType.TOPIC);


        ArrayList<String> examinationTypes = new ArrayList<String>();

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter examination types: ");
        for(int i = 0; i<maxExaminationTypes; i++) {
            examinationTypes.add(br.readLine());
        }

        for(String examinationType : examinationTypes) {
            channel.queueDeclare(QUEUE_PREFIX+examinationType, false, false, true, null);
            channel.queueBind(QUEUE_PREFIX+examinationType, EXCHANGE_HOSPITAL, EXCHANGE_HOSPITAL + ".tech." + examinationType + ".#");
   }
        channel.queueDeclare( TECH_ID, false, false, true, null);
        channel.queueBind(TECH_ID, EXCHANGE_HOSPITAL, EXCHANGE_HOSPITAL + ".admin.info.#");


        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("Received : " + message);
                processRequest(4);
                channel.basicPublish(EXCHANGE_HOSPITAL, properties.getReplyTo(), null, (message + " done").getBytes("UTF-8"));
                channel.basicPublish(EXCHANGE_HOSPITAL, EXCHANGE_HOSPITAL + ".admin.log", null, (message + " done").getBytes("UTF-8"));
                System.out.println("Sent: " + (message + " done"));
            }
        };

        Consumer consumerAdmin = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("Received from ADMIN: " + message);
            }
        };


        //CONSUME
        System.out.println("Waiting for messages");
        for(String examinationType : examinationTypes) {
            channel.basicConsume(QUEUE_PREFIX+examinationType, true, consumer);
        }
        channel.basicConsume( TECH_ID, true, consumerAdmin);

        while (true) {
            br = new BufferedReader(new InputStreamReader(System.in));
            String line = br.readLine();
            if (line.contains("exitChannel")) {
                channel.close();
                connection.close();
                break;
            }
        }
    }

    private static void processRequest(int time) {
        try {
            Thread.sleep(time * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
