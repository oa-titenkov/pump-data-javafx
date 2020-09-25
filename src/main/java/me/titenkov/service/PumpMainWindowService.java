package me.titenkov.service;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import me.titenkov.model.Spool;

import java.util.ArrayList;

public class PumpMainWindowService {

    synchronized public int getCurrentDocumentCount(MongoClient mongoClient) {
        return (int) mongoClient
                .getDatabase("pump-rebuild")
                .getCollection("data")
                .countDocuments();
    }

    synchronized public ArrayList<Spool> getLastSpoolDocuments(MongoClient mongoClient, int numberOfDocuments) {
        MongoDatabase database = mongoClient.getDatabase("pump-rebuild");
        MongoCollection<Spool> table = database.getCollection("data", Spool.class);
        System.out.println("new spools number" + numberOfDocuments);
        MongoCursor<Spool> spoolCursor = table.find().skip(numberOfDocuments).cursor();
        ArrayList<Spool> spools = new ArrayList<>();
        while(spoolCursor.hasNext()) {
            spools.add(0, spoolCursor.next());
        }
        return spools;
    }

}
