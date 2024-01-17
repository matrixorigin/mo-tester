package io.mo.stream;

import org.apache.kafka.clients.consumer.KafkaConsumer;

public class Consumer {
    KafkaConsumer<String,String> consumer;
    
    public Consumer(KafkaConsumer consumer){
        this.consumer = consumer;
    }
}
