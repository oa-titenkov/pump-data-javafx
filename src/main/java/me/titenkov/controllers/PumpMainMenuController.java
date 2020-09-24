package me.titenkov.controllers;

import com.mongodb.BasicDBObject;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.util.Duration;
import me.titenkov.model.Spool;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class PumpMainMenuController implements Initializable {

    public Label stanNumber;
    public Label spoolNumber;
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
    public ComboBox<String> stanSelect;
    public ListView<String> newSpoolList;

    final static double GUI_UPDATE_RATE = 10;
    static long fieldsCount = 0;

    public ObservableList<String> spoolComboBoxList = FXCollections.observableArrayList("1");
    public ObservableList<String> stanComboBoxList = FXCollections.observableArrayList("338");

    static ScheduledService<String> scanningBackground;
    static Service<String> usualBackground;


    CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
    CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);
    MongoClientSettings clientSettings = MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString("mongodb://172.16.13.14/pump-rebuild"))
            .codecRegistry(codecRegistry)
            .build();
    MongoClient mongoClient = MongoClients.create(clientSettings);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        spoolSelect.setItems(spoolComboBoxList);
        stanSelect.setItems(stanComboBoxList);
    }

    public PumpMainMenuController() {
//        MongoDatabase database = mongoClient.getDatabase("pump-rebuild");
//        MongoCollection<Spool> table = database.getCollection("data", Spool.class);
//        Spool defaultSpool = table.find().first();
        fieldsCount = mongoClient.getDatabase("pump-rebuild").getCollection("data").countDocuments();
        scanningBackground = scanningForNewSpools();
        scanningBackground.setPeriod(Duration.seconds(GUI_UPDATE_RATE));
        scanningBackground.start();


    }

    public void stanChange() {
        if(spoolSelect.getValue() == null && spoolSelect.getPromptText().equals("#")) {
            System.out.println("No spool selected!");
        }
        else {
            if(usualBackground != null) usualBackground.cancel();
            String s = spoolSelect.getPromptText();
            if(!stanSelect.getPromptText().equals("#")){
                usualBackground = createBackgroundTaskOfChosenSpool(spoolSelect.getPromptText(), stanSelect.getValue());
            }
            else {
                usualBackground = createBackgroundTaskOfChosenSpool(spoolSelect.getValue(), stanSelect.getValue());
            }
            usualBackground.setOnSucceeded(event -> {
                System.out.println("Inside stan succeeded:");
            });
            usualBackground.start();
        }
    }

    public void spoolChange() {
        if(stanSelect.getValue() == null && stanSelect.getPromptText().equals("#")) {
            System.out.println("No stan selected!");
        }
        else {
            if(usualBackground != null) usualBackground.cancel();
            String s = stanSelect.getPromptText();
            if(!spoolSelect.getPromptText().equals("#")) {
                usualBackground = createBackgroundTaskOfChosenSpool(spoolSelect.getValue(), stanSelect.getPromptText());
            }
            else {
                usualBackground = createBackgroundTaskOfChosenSpool(spoolSelect.getValue(), stanSelect.getValue());
            }
            usualBackground.setOnSucceeded(event -> {
                System.out.println("Inside spool succeeded:");
            });
            usualBackground.start();
        }

    }

    public ScheduledService<String> scanningForNewSpools() {
        return new ScheduledService<String>() {
            @Override
            protected Task<String> createTask() {
                return new Task<String>() {
                    @Override
                    protected String call() throws Exception {
                        Platform.runLater(() -> {
                            int currentCount = (int) mongoClient
                                    .getDatabase("pump-rebuild")
                                    .getCollection("data")
                                    .countDocuments();
                            if(currentCount > fieldsCount) {
                                MongoDatabase database = mongoClient.getDatabase("pump-rebuild");
                                MongoCollection<Spool> table = database.getCollection("data", Spool.class);
                                Spool spool = table.find().skip(currentCount - 1).first();
                                if (spool != null) {
                                    newSpoolList.getItems().add(spool.toString());
                                }
                                fieldsCount = currentCount;
                            }
                        });
                        return null;
                    }
                };
            }
        };

    }

    public Service<String> createBackgroundTaskOfChosenSpool(String spoolValue, String stanValue) {
        return new Service<String>() {
            @Override
            protected Task<String> createTask() {
                return new Task<String>() {
                    @Override
                    protected String call() throws Exception {
                        Platform.runLater(() -> {
                            MongoDatabase database = mongoClient.getDatabase("pump-rebuild");
                            MongoCollection<Spool> table = database.getCollection("data", Spool.class);
                            BasicDBObject criteria = new BasicDBObject();
                            criteria.append("spoolNumber", spoolValue)
                                    .append("stanNumber", stanValue);
                            Spool spool = table.find(criteria).first();
                            if(spool != null) {
                                stanComboBoxList.clear();
                                stanSelect.setItems(stanComboBoxList);
                                for (String s : table.distinct("stanNumber", String.class)) {
                                    stanComboBoxList.add(s);
                                }
                                stanSelect.setItems(stanComboBoxList);
                                System.out.println(stanValue + " " + spoolValue);
                                stanNumber.setText(stanValue);
                                spoolNumber.setText(spoolValue);
                                stanSelect.setPromptText(stanValue);
                                spoolSelect.setPromptText(spoolValue);
                                spoolComboBoxList.clear();
                                spoolSelect.setItems(spoolComboBoxList);
                                long count = mongoClient
                                        .getDatabase("pump-rebuild")
                                        .getCollection("data")
                                        .countDocuments(Filters.eq("stanNumber", stanValue));
                                for(int i = 0; i < count; i++) {
                                    spoolComboBoxList.add(String.valueOf(i + 1));
                                }
                                spoolSelect.setItems(spoolComboBoxList);
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
                            }
                        });
                        return null;
                    }
                };
            }
        };

    }


}


