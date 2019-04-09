import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

public class Doctor implements AutoCloseable{
    private Channel channel;
    private Connection connection;
    private String EXCHANGE_NAME = "hospital_channel";

    public Doctor() throws IOException, TimeoutException {
        // connection & channel
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        connection = factory.newConnection();
        channel = connection.createChannel();

        // exchange
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

        // queue & bind
        String queueName = channel.queueDeclare().getQueue();
        System.out.println("binded to : " + queueName);
    }

    public static void main(String[] argv) throws Exception {

        // info
        System.out.println("I AM A DOCTOR");

        try (Doctor doctor = new Doctor()) {


            while (true) {

                // read msg
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("Enter examination type: ");
                String examinationType = br.readLine();

                System.out.println("Enter pacient's name:");
                String patientName = br.readLine();

                // break condition
                if ("exit".equals(examinationType)) {
                    break;
                }

                String key = "toTech.type." + examinationType + ".name." + patientName;
                String message = patientName.concat(" has problem with ").concat(examinationType);

                doctor.sendMessage(key,message);
            }
        }catch(IOException | TimeoutException | InterruptedException e){
            e.printStackTrace();
        }
        }

    public String sendMessage(String key,String message) throws IOException, InterruptedException {
        final String corrId = UUID.randomUUID().toString();

        String replyQueueName = channel.queueDeclare().getQueue();
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();

        channel.basicPublish(EXCHANGE_NAME, key, props, message.getBytes(StandardCharsets.UTF_8));
        System.out.println("Sent: " + message);
        final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);

        String ctag = channel.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
            if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                response.offer(new String(delivery.getBody(), "UTF-8"));
            }else{
                response.offer(new String(delivery.getBody(), "UTF-8"));
            }
        }, consumerTag -> {
        });

        String result = response.take();
        channel.basicCancel(ctag);
        return result;
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }
}
