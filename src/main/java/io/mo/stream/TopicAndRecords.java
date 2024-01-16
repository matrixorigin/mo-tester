package io.mo.stream;

import java.util.ArrayList;
import java.util.List;

public class TopicAndRecords {
    private String topic;
    private List<String> records = new ArrayList<>();
    
    public TopicAndRecords(){
        
    }
    
    public TopicAndRecords(String topic){
        this.topic = topic;
    }
    
    public void addRecord(String record){
        records.add(record);
    }
    
    public int size(){
        return records.size();
    }
    
    public String getRecord(int i){
        return records.get(i);
    }
    
    public String getTopic(){
        return topic;
    }
    
    public void setTopic(String topic){
        this.topic = topic;
    }
    
    public String[] getRecords(){
        if(records.size() > 0){
            return records.toArray(new String[records.size()]);
        }
        
        return null;
    }

    public String getRecordsStr(){
        StringBuffer buffer = new StringBuffer();
        for(int i = 0; i < records.size();i++){
            buffer.append(records.get(i));
            buffer.append("\n");
        }

        if(buffer.length() > 0 )
            return buffer.toString();
        return null;
    }
}
