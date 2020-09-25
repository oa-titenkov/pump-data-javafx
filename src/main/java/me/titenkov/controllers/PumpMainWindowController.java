package me.titenkov.controllers;

import com.mongodb.BasicDBObject;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import me.titenkov.model.Spool;
import me.titenkov.service.PumpMainWindowService;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.ResourceBundle;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class PumpMainWindowController implements Initializable {

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
    public ListView<Spool> newSpoolList;
    public DatePicker datePicker;
    public ComboBox<String> shiftPicker;
    public Button historyButton;

    final static double GUI_UPDATE_RATE = 10;
    static long currentDocumentCount = 0;

    public ObservableList<String> spoolComboBoxList = FXCollections.observableArrayList("1");
    public ObservableList<String> stanComboBoxList = FXCollections.observableArrayList("338");
    public ObservableList<String> shiftPickerList = FXCollections.observableArrayList("1", "2");

    static ScheduledService<String> scanningBackground;
    static Service<String> usualBackground;
    static Service<String> historyBackground;

    public Button backToCurrentShift;
    public ListView<Spool> historySpoolsList;
    public Label spoolListTextField;
    public Label spoolNumberTextField;
    public Label stanNumberTextField;
    public Label lastDBConnectionTextField;
    public Label spoolShiftCount;
    public Label historyLabel;
    public Label spoolsShiftCountTextField;


    CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
    CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);
    MongoClientSettings clientSettings = MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString("mongodb://172.16.13.14/pump-rebuild"))
            .codecRegistry(codecRegistry)
            .build();
    MongoClient mongoClient = MongoClients.create(clientSettings);

    public PumpMainWindowService pumpService = new PumpMainWindowService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        spoolSelect.setItems(spoolComboBoxList);
        stanSelect.setItems(stanComboBoxList);
        shiftPicker.setItems(shiftPickerList);
        backToCurrentShift.setVisible(false);
        historySpoolsList.setVisible(false);
        spoolsShiftCountTextField.setVisible(false);
        setStanCombobox();
        setSpoolFieldBlank();
        historySpoolsList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> setSpoolFields(newValue));
        newSpoolList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> setSpoolFields(newValue));
    }

    public PumpMainWindowController() {
        currentDocumentCount = pumpService.getCurrentDocumentCount(mongoClient);
        scanningBackground = scanningForNewSpoolsService();
        scanningBackground.setPeriod(Duration.seconds(GUI_UPDATE_RATE));
        scanningBackground.start();
    }

    public void stanChange() {
        if(spoolSelect.getValue() == null && spoolSelect.getPromptText().equals("#")) {
            System.out.println("No spool selected!");
        }
        else {
            if(usualBackground != null) usualBackground.cancel();
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

    public ScheduledService<String> scanningForNewSpoolsService() {
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
                            if(currentCount > currentDocumentCount) {
                                int newSpoolsCount = currentCount - (int) currentDocumentCount;
                                MongoDatabase database = mongoClient.getDatabase("pump-rebuild");
                                MongoCollection<Spool> table = database.getCollection("data", Spool.class);
                                MongoCursor<Spool> spoolCursor = table.find().skip(currentCount - newSpoolsCount).cursor();
                                while(spoolCursor.hasNext()) {
                                    newSpoolList.getItems().add(0, spoolCursor.next());
                                }
                                currentDocumentCount = currentCount;
                            }
                        });
                        return null;
                    }
                };
            }
        };

    }

    public Service<String> findHistorySpools(String date, String shift) {
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
                            Instant instantTomorrowDate = null;
                            try{
                                instantTomorrowDate = new SimpleDateFormat("dd-MM-yyyy").parse(date).toInstant();
                            }catch (ParseException e){
                                System.out.println(e.toString());
                            }

                            Instant tomorrowDay = Objects.requireNonNull(instantTomorrowDate).plus(2, ChronoUnit.DAYS);
                            System.out.println("tomorrow:" + tomorrowDay);
                            LocalDateTime datetime = LocalDateTime.ofInstant(tomorrowDay, ZoneOffset.UTC);
                            String formattedDate = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(datetime);
                            criteria.append("date", date);
                            if(shift.equals("2")) {
                                System.out.println(formattedDate);
                                criteria.append("date", formattedDate);
                            }
                            MongoCursor<Spool> spools = table.find(criteria).cursor();
                            while(spools.hasNext()) {
                                Spool spool = spools.next();
                                System.out.println(spool.toString());
                                if(shift.equals("2")){
                                    if(spool.getDate().equals(date) && Integer.parseInt(spool.getTime().split(":")[0]) >= 20 && Integer.parseInt(spool.getTime().split(":")[0]) < 24) {
                                        historySpoolsList.getItems().add(spool);
                                    }
                                    else if(spool.getDate().equals(formattedDate) && Integer.parseInt(spool.getTime().split(":")[0]) >= 0 && Integer.parseInt(spool.getTime().split(":")[0]) < 8) {
                                        historySpoolsList.getItems().add(spool);
                                    }
                                }
                                else {
                                    if(Integer.parseInt(spool.getTime().split(":")[0]) >= 8 && Integer.parseInt(spool.getTime().split(":")[0]) < 20) {
                                        historySpoolsList.getItems().add(spool);
                                    }
                                }

                            }
                            historySpoolsList.getItems().sort(Comparator
                                    .comparing(Spool::getIntegerStanNumber)
                                    .thenComparing(Spool::getIntegerSpoolNumber));
                            spoolShiftCount.setText(String.valueOf(historySpoolsList.getItems().size()));
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
                            BasicDBObject criteriaWithoutSpool = new BasicDBObject();
                            LocalDateTime datetime = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
                            String formattedDate = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(datetime);
                            criteria.append("spoolNumber", spoolValue)
                                    .append("stanNumber", stanValue)
                                    .append("date", formattedDate);
                            criteriaWithoutSpool.append("stanNumber", stanValue).append("date", formattedDate);
                            Spool spool = table.find(criteria).first();
                            if(spool != null) {
                                stanComboBoxList.clear();
                                stanSelect.setItems(stanComboBoxList);
                                for (String s : table.distinct("stanNumber", String.class)) {
                                    stanComboBoxList.add(s);
                                }
                                spoolSelect.setItems(spoolComboBoxList);
                                stanSelect.setItems(stanComboBoxList);
                                System.out.println(stanValue + " " + spoolValue);
                                stanNumber.setText(stanValue);
                                spoolNumber.setText(spoolValue);
                                stanSelect.setPromptText(stanValue);
                                spoolSelect.setPromptText(spoolValue);
                                spoolComboBoxList.clear();
                                MongoCursor<Spool> spools = table.find(criteriaWithoutSpool).cursor();
                                while (spools.hasNext()) {
                                    spoolComboBoxList.add(spools.next().getSpoolNumber());
                                }
                                setSpoolFields(spool);
                            }
                        });
                        return null;
                    }
                };
            }
        };

    }


    public void datePickerAction(ActionEvent actionEvent) {
        final String pattern = "dd-MM-yyyy";
        final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(pattern);
        System.out.println(datePicker.getValue().format(dateFormatter));
    }

    public void initializeHistoryWindow(boolean isVisible) {
        if(isVisible) {
            if(historyBackground != null) historyBackground.cancel();
            setSpoolFieldBlank();
            historySpoolsList.getItems().clear();
            spoolShiftCount.setText("0");
            backToCurrentShift.setVisible(true);
            historySpoolsList.setVisible(true);
            spoolsShiftCountTextField.setVisible(true);
            newSpoolList.setVisible(false);
            spoolSelect.setVisible(false);
            stanSelect.setVisible(false);
            spoolNumberTextField.setVisible(false);
            stanNumberTextField.setVisible(false);
            lastUpdatedLabel.setVisible(false);
            lastDBConnectionTextField.setVisible(false);
            spoolListTextField.setText("Катушки за выбранную смену:");
            historyLabel.setText("HISTORY");
            final String pattern = "dd-MM-yyyy";
            final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(pattern);
            System.out.println(datePicker.getValue().format(dateFormatter) + " " + shiftPicker.getValue());
            historyBackground = findHistorySpools(datePicker.getValue().format(dateFormatter), shiftPicker.getValue());
            historyBackground.start();
        }
        else {
            backToCurrentShift.setVisible(false);
            historySpoolsList.setVisible(false);
            spoolsShiftCountTextField.setVisible(false);
            newSpoolList.setVisible(true);
            spoolSelect.setVisible(true);
            stanSelect.setVisible(true);
            spoolNumberTextField.setVisible(true);
            stanNumberTextField.setVisible(true);
            lastUpdatedLabel.setVisible(true);
            lastDBConnectionTextField.setVisible(true);
            spoolListTextField.setText("Новые катушки:");
            historyLabel.setText("");
            setSpoolFieldBlank();
            historyBackground.cancel();
        }
    }

    public void historyStart(ActionEvent actionEvent) {
        if(datePicker.getValue() != null && shiftPicker.getValue() != null) {
            initializeHistoryWindow(true);
        }
    }

    public void backToCurrentShiftButtonClick(ActionEvent actionEvent) {
        initializeHistoryWindow(false);
    }

    public void historyListClick(MouseEvent mouseEvent) {
        setSpoolFields(historySpoolsList.getSelectionModel().getSelectedItem());
    }

    public void newSpoolListClick(MouseEvent mouseEvent) {
        setSpoolFields(newSpoolList.getSelectionModel().getSelectedItem());
    }

    public void setSpoolFields(Spool spool) {
        if(spool != null) {
            spoolNumber.setText(spool.getSpoolNumber());
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
    }

    public void setSpoolFieldBlank() {
        spoolNumber.setText("");
        stanNumber.setText("");
        brigade.setText("");
        shift.setText("");
        personalNumber.setText("");
        materialCode.setText("");
        ordinalShift.setText("");
        fusionNumber.setText("");
        parentFlangeNumber.setText("");
        childFlangeNumber.setText("");
        diameter.setText("");
        length.setText("");
        weight.setText("");
        ordinalDraw.setText("");
        quality.setText("");
        orderCode.setText("");
        date.setText("");
        time.setText("");
        lastUpdatedLabel.setText("");
        spoolShiftCount.setText("");
    }

    public void setStanCombobox() {
        MongoDatabase database = mongoClient.getDatabase("pump-rebuild");
        MongoCollection<Spool> table = database.getCollection("data", Spool.class);
        BasicDBObject criteria = new BasicDBObject();
        LocalDateTime datetime = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        String formattedDate = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(datetime);
        criteria.append("date", formattedDate);
        Spool spool = table.find(criteria).first();
        if(spool != null) {
            stanComboBoxList.clear();
            stanSelect.setItems(stanComboBoxList);
            for (String s : table.distinct("stanNumber", String.class)) {
                stanComboBoxList.add(s);
            }
        }
    }

}


