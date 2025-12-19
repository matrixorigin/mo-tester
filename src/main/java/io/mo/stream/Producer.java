package io.mo.stream;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.concurrent.ExecutionException;

public class Producer {
    private KafkaProducer<String,String> producer;
    
    public Producer(KafkaProducer producer){
        this.producer = producer;
    }
    
    public boolean send(String topic, String message){
        
        ProducerRecord<String,String> record = new ProducerRecord<String,String>(topic,message);
        try {
            producer.send(record).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return false;
        }
        producer.close();
        
        return true;
    }

    public boolean send(TopicAndRecords tar){
        for(int i = 0; i < tar.size(); i++){
            try {
                ProducerRecord<String,String> record = new ProducerRecord<String,String>(tar.getTopic(),tar.getRecord(i));
                producer.send(record).get();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            } catch (ExecutionException e) {
                e.printStackTrace();
                return false;
            }
        }

        producer.close();
        return true;
    }
    
    public void close(){
        this.producer.close();
    }
    
    
    public static void main(String[] args){
        Producer producer = KafkaManager.getProducer();
        producer.send("mo-stream-test","First message to topic mo-stream-test");
        
    }
}
