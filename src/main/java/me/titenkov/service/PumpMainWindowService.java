package me.titenkov.service;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import javafx.collections.ObservableList;
import me.titenkov.model.Spool;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Objects;

public class PumpMainWindowService {

    synchronized public int getCurrentDocumentCount(MongoClient mongoClient) {
        return (int) mongoClient
                .getDatabase("pump-rebuild")
                .getCollection("data")
                .countDocuments();
    }

    synchronized public ArrayList<Spool> getLastSpoolDocuments(MongoCollection<Spool> table, int numberOfDocuments) {

        System.out.println("new spools number" + numberOfDocuments);
        MongoCursor<Spool> spoolCursor = table.find().skip(numberOfDocuments).cursor();
        ArrayList<Spool> spools = new ArrayList<>();
        while(spoolCursor.hasNext()) {
            System.out.println("spool");
            spools.add(0, spoolCursor.next());
        }
        return spools;
    }

    synchronized public String getFormattedTomorrowDate(String date) {
        Instant instantTomorrowDate = null;
        try {
            instantTomorrowDate = new SimpleDateFormat("dd-MM-yyyy").parse(date).toInstant();
        } catch (ParseException e) {
            System.out.println(e.toString());
        }
        Instant tomorrowDay = Objects.requireNonNull(instantTomorrowDate).plus(2, ChronoUnit.DAYS);
        System.out.println("tomorrow:" + tomorrowDay);
        LocalDateTime datetime = LocalDateTime.ofInstant(tomorrowDay, ZoneOffset.UTC);
        return DateTimeFormatter.ofPattern("dd-MM-yyyy").format(datetime);

    }

    synchronized public String getSpoolsStanCountForShift(String stanNumber, ObservableList<Spool> spools) {
        int count = 0;
        for (Spool spool : spools) {
            if (spool.getStanNumber().equals(stanNumber)) {
                count++;
            }
        }
        return String.valueOf(count);
    }

}
