package me.titenkov.controllers;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.util.Duration;
import me.titenkov.model.Spool;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class PumpMainMenuController {

    public Label stanNumber;
    public Label shift;
    public Label brigade;
    public Label personalNumber;
    public Label materialCode;
    public Label ordinalShift;
    public Label fusionNumber;
    public Label parentFlangeNumber;
    public Label childFlangeNumber;
    public Label diameter;
    public Label length;
    public Label weight;
    public Label ordinalDraw;
    public Label quality;
    public Label orderCode;
    public Label date;
    public Label time;
    public Label lastUpdatedLabel;
    public ComboBox<String> spoolSelect;

    final static double GUI_UPDATE_RATE = 10;

    static ObservableList<String> comboBoxList = FXCollections.observableArrayList();

    ScheduledService<String> defaultBackground;
    ScheduledService<String> usualBackground;
    CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
    CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);
    MongoClientSettings clientSettings = MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString("mongodb://172.16.13.14/pump-rebuild"))
            .codecRegistry(codecRegistry)
            .build();
    MongoClient mongoClient = MongoClients.create(clientSettings);

    public PumpMainMenuController() {
        defaultBackground = createBackgroundTaskOfChosenSpool("1");
        defaultBackground.setPeriod(Duration.seconds(GUI_UPDATE_RATE));
        defaultBackground.start();

    }

    public void spoolChange(ActionEvent event) {
        if(defaultBackground != null) defaultBackground.cancel();
        if(usualBackground != null) usualBackground.cancel();
        usualBackground = createBackgroundTaskOfChosenSpool(spoolSelect.getValue());
        usualBackground.setPeriod(Duration.seconds(GUI_UPDATE_RATE));
        usualBackground.start();
    }

    public long getSpoolCount() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        return mongoClient
                .getDatabase("pump-rebuild")
                .getCollection("data")
                .countDocuments(Filters.eq("date", dateFormatter.format(LocalDate.now())));
    }

    public ScheduledService<String> createBackgroundTaskOfChosenSpool(String value) {
        return new ScheduledService<String>() {
            @Override
            protected Task<String> createTask() {
                return new Task<String>() {
                    @Override
                    protected String call() throws Exception {
                        Platform.runLater(() -> {
                            MongoDatabase database = mongoClient.getDatabase("pump-rebuild");
                            MongoCollection<Spool> table = database.getCollection("data", Spool.class);
                            if(comboBoxList.size() == 0) {
                                for(int i = 0; i < getSpoolCount(); i++) {
                                    comboBoxList.add(String.valueOf(i + 1));
                                }
                                spoolSelect.setItems(comboBoxList);
                            }
                            Spool spool = table.find(Filters.eq("spoolNumber", value)).first();
                            if(spool != null) {
                                stanNumber.setText(spool.getStanNumber());
                                brigade.setText(spool.getBrigade());
                                shift.setText(spool.getShift());
                                personalNumber.setText(spool.getPersonalNumber());
                                materialCode.setText(spool.getMaterialCode());
                                ordinalShift.setText(spool.getOrdinalShift());
                                fusionNumber.setText(spool.getFusionNumber());
                                parentFlangeNumber.setText(spool.getParentFlangeNumber());
                                childFlangeNumber.setText(spool.getChildFlangeNumber());
                                diameter.setText(spool.getDiameter());
                                length.setText(spool.getLength());
                                weight.setText(spool.getWeight());
                                ordinalDraw.setText(spool.getOrdinalDraw());
                                quality.setText(spool.getQuality());
                                orderCode.setText(spool.getOrderCode());
                                date.setText(spool.getDate());
                                time.setText(spool.getTime());
                                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                                lastUpdatedLabel.setText(dateFormatter.format(LocalDateTime.now()));
                                System.out.println(Thread.currentThread() + " " + spool.getSpoolNumber());
                            }
                        });
                        return null;
                    }
                };
            }
        };

    }

}


