package me.titenkov.controllers;

import com.mongodb.*;
import com.mongodb.client.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.Duration;
import me.titenkov.model.Spool;
import me.titenkov.service.PumpMainWindowService;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    public ObservableList<String> spoolComboBoxList = FXCollections.observableArrayList();
    public ObservableList<String> stanComboBoxList = FXCollections.observableArrayList();
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
    public Label stanSpoolShiftCount;
    public Label stanSpoolsShiftCountTextField;

    public MongoCollection<Spool> dataCollection;

    CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
    CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);
    MongoClientSettings clientSettings = MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString("mongodb://localhost/pump-rebuild"))
            .codecRegistry(codecRegistry)
            .build();

    public PumpMainWindowService pumpService = new PumpMainWindowService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dataCollection = MongoClients
                .create(clientSettings)
                .getDatabase("pump-rebuild").getCollection("data")
                .withDocumentClass(Spool.class);
        currentDocumentCount = dataCollection.countDocuments();
        scanningBackground = scanningForNewSpoolsService();
        scanningBackground.setPeriod(Duration.seconds(GUI_UPDATE_RATE));
        scanningBackground.start();
        spoolSelect.setItems(spoolComboBoxList);
        stanSelect.setItems(stanComboBoxList);
        shiftPicker.setItems(shiftPickerList);
        backToCurrentShift.setVisible(false);
        historySpoolsList.setVisible(false);
        spoolsShiftCountTextField.setVisible(false);
        spoolShiftCount.setVisible(false);
        stanSpoolShiftCount.setVisible(false);
        stanSpoolsShiftCountTextField.setVisible(false);
        stanSpoolShiftCount.setText("");
        setStanComboBox();
        setSpoolFields(null);
        historySpoolsList.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> setSpoolFields(newValue));
        newSpoolList
                .getSelectionModel()
                .selectedItemProperty().addListener((observable, oldValue, newValue) -> setSpoolFields(newValue));
    }

    public PumpMainWindowController() {
    }

    public void stanChange() {
        if(spoolSelect.getValue() == null && spoolSelect.getPromptText().equals("#")) {
            System.out.println("No spool selected!");
        }
        else {
            if(usualBackground != null) usualBackground.cancel();
            spoolSelect.setValue("#");
            setSpoolFields(null);
        }
        spoolComboBoxList.clear();
        setSpoolComboBox(stanSelect.getValue());
    }

    public void spoolChange() {
        if(stanSelect.getValue() == null && stanSelect.getPromptText().equals("#")) {
            System.out.println("No stan selected!");
        }
        else {
            if(usualBackground != null) usualBackground.cancel();
            usualBackground = createBackgroundTaskOfChosenSpool(spoolSelect.getValue(), stanSelect.getValue());
            usualBackground.start();
        }

    }

    public ScheduledService<String> scanningForNewSpoolsService() {
        return new ScheduledService<String>() {
            @Override
            protected Task<String> createTask() {
                return new Task<String>() {
                    @Override
                    protected String call() {
                        Platform.runLater(() -> {
                            int currentCount = (int) dataCollection.countDocuments();
                            if(currentCount > currentDocumentCount) {
                                int newSpoolsCount = currentCount - (int) currentDocumentCount;
                                List<Spool> newSpools = pumpService
                                        .getLastSpoolDocuments(
                                                dataCollection,
                                                currentCount - newSpoolsCount
                                        );
                                for (Spool spool: newSpools) {
                                    newSpoolList.getItems().add(0, spool);
                                }
                                currentDocumentCount = currentCount;
                                setStanComboBox();
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
                    protected String call() {
                        Platform.runLater(() -> {
                            BasicDBObject criteria = new BasicDBObject();
                            criteria.append("date", date);
                            String tomorrowDate = pumpService.getFormattedTomorrowDate(date);
                            if(shift.equals("2")) {
                                System.out.println(tomorrowDate);
                                criteria.append("date", tomorrowDate);
                            }
                            MongoCursor<Spool> spools = dataCollection.find(criteria).cursor();
                            while(spools.hasNext()) {
                                Spool spool = spools.next();
                                System.out.println(spool.toString());
                                if(shift.equals("2")){
                                    if(spool.getDate().equals(date)
                                            && Integer.parseInt(spool.getTime().split(":")[0]) >= 19
                                            && Integer.parseInt(spool.getTime().split(":")[0]) < 24
                                    ) {
                                        historySpoolsList.getItems().add(spool);
                                    }
                                    else if(spool.getDate().equals(tomorrowDate)
                                            && Integer.parseInt(spool.getTime().split(":")[0]) >= 0
                                            && Integer.parseInt(spool.getTime().split(":")[0]) < 7
                                    ) {
                                        historySpoolsList.getItems().add(spool);
                                    }
                                }
                                else {
                                    if(Integer.parseInt(spool.getTime().split(":")[0]) >= 7
                                            && Integer.parseInt(spool.getTime().split(":")[0]) < 19
                                    ) {
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
                    protected String call() {
                        Platform.runLater(() -> {
                            BasicDBObject criteria = new BasicDBObject();
                            LocalDateTime datetime = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
                            String formattedDate = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(datetime);
                            criteria.append("spoolNumber", spoolValue)
                                    .append("stanNumber", stanValue)
                                    .append("date", formattedDate);
                            System.out.println(stanValue + " spool change " + spoolValue + "\n");
                            Spool spool = dataCollection.find(criteria).first();
                            if(spool != null) {
                                stanNumber.setText(stanValue);
                                spoolNumber.setText(spoolValue);
                                stanSelect.setPromptText(stanValue);
                                setSpoolFields(spool);
                            }
                        });
                        return null;
                    }
                };
            }
        };

    }

    public void datePickerAction() {
        final String pattern = "dd-MM-yyyy";
        final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(pattern);
        System.out.println(datePicker.getValue().format(dateFormatter));
    }

    public void initializeHistoryWindow(boolean setVisible) {
        if(setVisible) {
            if(historyBackground != null) historyBackground.cancel();
            setSpoolFields(null);
            historySpoolsList.getItems().clear();
            spoolShiftCount.setText("0");
            spoolShiftCount.setVisible(true);
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
            stanSpoolShiftCount.setVisible(true);
            stanSpoolsShiftCountTextField.setVisible(true);
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
            spoolShiftCount.setVisible(false);
            historyLabel.setText("");
            stanSpoolShiftCount.setVisible(false);
            stanSpoolsShiftCountTextField.setVisible(false);
            setSpoolFields(null);
            historyBackground.cancel();
        }
    }

    public void historyStart() {
        if(datePicker.getValue() != null && shiftPicker.getValue() != null) {
            initializeHistoryWindow(true);
        }
    }

    public void backToCurrentShiftButtonClick() {
        initializeHistoryWindow(false);
    }

    public void historyListClick() {
        setSpoolFields(historySpoolsList.getSelectionModel().getSelectedItem());
    }

    public void newSpoolListClick() {
        setSpoolFields(newSpoolList.getSelectionModel().getSelectedItem());
    }

    public void setSpoolFields(Spool spool) {
            spoolNumber.setText(spool != null ? spool.getSpoolNumber() : "");
            stanNumber.setText(spool != null ? spool.getStanNumber() : "");
            brigade.setText(spool != null ? spool.getBrigade() : "");
            shift.setText(spool != null ? spool.getShift() : "");
            personalNumber.setText(spool != null ? spool.getPersonalNumber() : "");
            materialCode.setText(spool != null ? spool.getMaterialCode() : "");
            ordinalShift.setText(spool != null ? spool.getOrdinalShift() : "");
            fusionNumber.setText(spool != null ? spool.getFusionNumber() : "");
            parentFlangeNumber.setText(spool != null ? spool.getParentFlangeNumber() : "");
            childFlangeNumber.setText(spool != null ? spool.getChildFlangeNumber() : "");
            diameter.setText(spool != null ? spool.getDiameter() : "");
            length.setText(spool != null ? spool.getLength() : "");
            weight.setText(spool != null ? spool.getWeight() : "");
            ordinalDraw.setText(spool != null ? spool.getOrdinalDraw() : "");
            quality.setText(spool != null ? spool.getQuality() : "");
            orderCode.setText(spool != null ? spool.getOrderCode() : "");
            date.setText(spool != null ? spool.getDate() : "");
            time.setText(spool != null ? spool.getTime() : "");
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
            lastUpdatedLabel.setText(dateFormatter.format(LocalDateTime.now()));
            stanSpoolShiftCount.setText(
                    pumpService.getSpoolsStanCountForShift(
                            spool != null ? spool.getStanNumber() : null, historySpoolsList.getItems()));
    }

    public void setStanComboBox() {
        BasicDBObject criteria = new BasicDBObject();
        LocalDateTime datetime = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        String formattedDate = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(datetime);
        criteria.append("date", formattedDate);
        Spool spool = dataCollection.find(criteria).first();
        if(spool != null) {
            Set<String> stanNumberSet = new HashSet<>();
            stanComboBoxList.clear();
            MongoCursor<Spool> mongoCursor = dataCollection.find(criteria).cursor();
            while(mongoCursor.hasNext()) {
                stanNumberSet.add(mongoCursor.next().getStanNumber());
            }
            stanComboBoxList.addAll(stanNumberSet);
            stanSelect.setItems(stanComboBoxList);
        }
    }

    public void setSpoolComboBox(String stanNumber) {
        BasicDBObject criteriaWithoutSpool = new BasicDBObject();
        LocalDateTime datetime = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        String formattedDate = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(datetime);
        criteriaWithoutSpool.append("stanNumber", stanNumber).append("date", formattedDate);
        MongoCursor<Spool> spools = dataCollection.find(criteriaWithoutSpool).cursor();
        while (spools.hasNext()) {
            spoolComboBoxList.add(spools.next().getSpoolNumber());
        }
        spoolSelect.setItems(spoolComboBoxList);
    }

}


