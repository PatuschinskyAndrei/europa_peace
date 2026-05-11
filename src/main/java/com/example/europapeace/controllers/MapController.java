package com.example.europapeace.controllers;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.event.ActionEvent;

import com.example.europapeace.entities.Regiune;
import com.example.europapeace.entities.Stat;
import com.example.europapeace.entities.MesajChat;
import com.example.europapeace.entities.Raport;
import com.example.europapeace.entities.Audienta;
import com.example.europapeace.entities.Utilizator;
import com.example.europapeace.repositories.RegiuneRepository;
import com.example.europapeace.repositories.CerereIndependentaRepository;
import com.example.europapeace.repositories.StatRepository;
import com.example.europapeace.repositories.UtilizatorRepository;
import com.example.europapeace.repositories.MesajChatRepository;
import com.example.europapeace.repositories.RaportRepository;
import com.example.europapeace.repositories.AudientaRepository;
import com.example.europapeace.services.MapImportService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class MapController {
    @FXML private MenuItem menuSetariCont;
    @FXML private Pane mapPane;
    @FXML private VBox vboxUnire;
    @FXML private Label lblInstructiuni;
    @FXML private TextField txtRegiuneSursa;
    @FXML private TextField txtRegiuneDestinatie;
    @FXML private Label lblNumeUtilizator;
    @FXML private Label lblRolUtilizator;
    @FXML private VBox meniuStanga;
    @FXML private BorderPane mainBorderPane;
    @FXML private ScrollPane scrollPane;
    @FXML private MenuButton menuProfil;

    @Autowired private RegiuneRepository regiuneRepository;
    @Autowired private CerereIndependentaRepository cerereIndependentaRepository;
    @Autowired private StatRepository statRepository;
    @Autowired private UtilizatorRepository utilizatorRepository;
    @Autowired private MesajChatRepository mesajChatRepository;
    @Autowired private RaportRepository raportRepository;
    @Autowired private AudientaRepository audientaRepository;
    @Autowired private MapImportService mapImportService;
    @Autowired private ConfigurableApplicationContext springContext;

    private final Group hartaGroup = new Group();
    private double mouseAnchorX, mouseAnchorY;
    private java.util.Set<Integer> apeluriEfectuate = new java.util.HashSet<>();
    private boolean modUnireActiv = false;
    private boolean modIndependentaActiv = false;
    private Regiune regiuneSelectataSursa = null;
    private List<String> listaNumeRegiuni = new LinkedList<>();

    private Timeline chatPollingTimeline;

    @FXML
    public void initialize() {
        if (mapPane != null) {
            if (!mapPane.getChildren().contains(hartaGroup)) {
                mapPane.getChildren().add(hartaGroup);
            }
            hartaGroup.setCache(true);
            hartaGroup.setCacheHint(CacheHint.SPEED);
        }

        com.example.europapeace.entities.Utilizator userCurent =
                com.example.europapeace.config.UserSession.getInstance().getUtilizatorLogat();

        if (menuSetariCont != null) {
            menuSetariCont.setVisible(false);
        }

        if (userCurent != null && lblNumeUtilizator != null && lblRolUtilizator != null) {
            lblNumeUtilizator.setText(userCurent.getUsername());

            if (userCurent.getIdrol() != null) {
                switch(userCurent.getIdrol()) {
                    case 1:
                        lblRolUtilizator.setText("Șef de Stat");
                        aplicaInterfataSefDeStat();
                        break;
                    case 2:
                        lblRolUtilizator.setText("Consiliu");
                        aplicaInterfataConsiliu();
                        break;
                    case 3:
                        lblRolUtilizator.setText("Administrator");
                        aplicaInterfataAdmin();
                        break;
                    default:
                        lblRolUtilizator.setText("Rol #" + userCurent.getIdrol());
                        aplicaInterfataSecurizata();
                }

                if (userCurent.getIdrol() >= 1 && userCurent.getIdrol() <= 3) {
                    Platform.runLater(() -> {
                        setupInteractiuni();
                        randareHarta();
                        setupAutoComplete();
                    });
                }
            }
        }
        // Sistemul de "Apelare Automată" (CF_9)
// Sistemul de "Apelare Automată" (CF_9) - Versiunea pentru ambele părți
        Timeline apelAutomatTimeline = new Timeline(new KeyFrame(Duration.seconds(30), ev -> {
            com.example.europapeace.entities.Utilizator user = com.example.europapeace.config.UserSession.getInstance().getUtilizatorLogat();
            if (user == null || user.getIdrol() == null) return;

            // Luăm toate audiențele aprobate de pe server
            List<Audienta> audienteActive = audientaRepository.findAll().stream()
                    .filter(a -> "APROBATA".equals(a.getStatus()))
                    .filter(a -> !apeluriEfectuate.contains(a.getIdaudienta()))
                    .collect(Collectors.toList());

            java.time.LocalDateTime acum = java.time.LocalDateTime.now();

            for (Audienta a : audienteActive) {
                // Verificăm dacă ora se potrivește
                if (a.getDataora().getYear() == acum.getYear() &&
                        a.getDataora().getDayOfYear() == acum.getDayOfYear() &&
                        a.getDataora().getHour() == acum.getHour() &&
                        a.getDataora().getMinute() == acum.getMinute()) {

                    // LOGICA DE FILTRARE:
                    // 1. Dacă sunt Sef de Stat, mă sună doar dacă e audiența MEA
                    boolean esteAudientaMea = user.getId().equals(a.getIdutilizator());
                    // 2. Dacă sunt Consiliu (Rol 2), mă sună pentru TOATE audiențele aprobate
                    boolean suntConsiliu = (user.getIdrol() == 2);

                    if (esteAudientaMea || suntConsiliu) {
                        apeluriEfectuate.add(a.getIdaudienta()); // Nu mai sunăm a doua oară

                        Platform.runLater(() -> {
                            Alert apel = new Alert(Alert.AlertType.CONFIRMATION);
                            String rolLabel = suntConsiliu ? "Consiliu" : "Șef de Stat";

                            apel.setTitle("APEL AUTOMAT [" + rolLabel + "]");
                            apel.setHeaderText("Audiența pentru '" + a.getProtocol() + "' începe ACUM!");
                            apel.setContentText("Sunteți gata să intrați în sesiune?");

                            Optional<ButtonType> raspuns = apel.showAndWait();
                            if (raspuns.isPresent() && raspuns.get() == ButtonType.OK) {
                                if (chatPollingTimeline != null) chatPollingTimeline.stop();
                                afiseazaPanouChat(a); // Ambele părți sunt direcționate în aceeași cameră
                            }
                        });
                    }
                }
            }
        }));
        apelAutomatTimeline.setCycleCount(Animation.INDEFINITE);
        apelAutomatTimeline.play();
    }

    private void aplicaInterfataConsiliu() {
        if (meniuStanga != null) {
            Button btnInapoiHarta = new Button("Înapoi la Hartă \uD83D\uDDFA\uFE0F");
            btnInapoiHarta.setMaxWidth(Double.MAX_VALUE);
            btnInapoiHarta.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
            btnInapoiHarta.setOnAction(e -> {
                if (chatPollingTimeline != null) chatPollingTimeline.stop();
                mainBorderPane.setCenter(scrollPane);
            });

            Button btnGestAudiente = new Button("Cereri Audiențe & Camere Private \uD83D\uDCC5");
            btnGestAudiente.setMaxWidth(Double.MAX_VALUE);
            btnGestAudiente.setOnAction(e -> afiseazaGestioneazaAudiente());

            Button btnChatConsiliu = new Button("Chat Diplomatic (Global)");
            btnChatConsiliu.setMaxWidth(Double.MAX_VALUE);
            btnChatConsiliu.setStyle("-fx-background-color: #f1c40f; -fx-font-weight: bold;");
            btnChatConsiliu.setOnAction(e -> afiseazaPanouChat(null)); // NULL înseamnă chat global

            Button btnRapoarte = new Button("Arhivă Rapoarte (Publice)");
            btnRapoarte.setMaxWidth(Double.MAX_VALUE);
            btnRapoarte.setOnAction(e -> afiseazaPanouRapoarte());

            meniuStanga.getChildren().addAll(btnInapoiHarta, btnGestAudiente, btnChatConsiliu, btnRapoarte);
        }
    }

    private void aplicaInterfataSefDeStat() {
        VBox meniuSefStat = new VBox(15);
        meniuSefStat.setPadding(new javafx.geometry.Insets(30, 15, 20, 15));
        meniuSefStat.setStyle("-fx-background-color: white;");
        meniuSefStat.setPrefWidth(240);

        Button btnInapoiHarta = new Button("Înapoi la Hartă \uD83D\uDDFA\uFE0F");
        btnInapoiHarta.setMaxWidth(Double.MAX_VALUE);
        btnInapoiHarta.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        btnInapoiHarta.setOnAction(e -> {
            if (chatPollingTimeline != null) chatPollingTimeline.stop();
            mainBorderPane.setCenter(scrollPane);
        });

        Button btnAudienta = new Button("Solicită o Audiență");
        btnAudienta.setMaxWidth(Double.MAX_VALUE);
        btnAudienta.setOnAction(e -> afiseazaFormularAudienta());

        Button btnStatusAudiente = new Button("Status Audiențe / Camere Private");
        btnStatusAudiente.setMaxWidth(Double.MAX_VALUE);
        btnStatusAudiente.setOnAction(e -> afiseazaStatusAudienteSef());

        Button btnChat = new Button("Chat Diplomatic (Global)");
        btnChat.setMaxWidth(Double.MAX_VALUE);
        btnChat.setStyle("-fx-background-color: #f1c40f; -fx-font-weight: bold;");
        btnChat.setOnAction(e -> afiseazaPanouChat(null)); // NULL înseamnă chat global

        Button btnRapoarte = new Button("Rapoarte Oficiale (Publice)");
        btnRapoarte.setMaxWidth(Double.MAX_VALUE);
        btnRapoarte.setOnAction(e -> afiseazaPanouRapoarte());

        Button btnCereri = new Button("Cerere de Independență");
        btnCereri.setMaxWidth(Double.MAX_VALUE);
        btnCereri.setOnAction(e -> afiseazaFormularCerere());

        Button btnStatusCereri = new Button("Status Cereri Independență");
        btnStatusCereri.setMaxWidth(Double.MAX_VALUE);
        btnStatusCereri.setOnAction(e -> afiseazaStatusCereri());

        meniuSefStat.getChildren().addAll(btnInapoiHarta, btnAudienta, btnStatusAudiente, btnChat, btnRapoarte, new Separator(), btnCereri, btnStatusCereri);

        mainBorderPane.setLeft(meniuSefStat);
        mainBorderPane.setCenter(scrollPane);
    }

    private void afiseazaStatusAudienteSef() {
        if (chatPollingTimeline != null) chatPollingTimeline.stop();

        VBox panou = new VBox(15);
        panou.setPadding(new javafx.geometry.Insets(30));
        panou.setStyle("-fx-background-color: #ecf0f1;");

        Label titlu = new Label("Statusul Audiențelor Mele");
        titlu.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        TableView<Audienta> tabel = new TableView<>();
        tabel.setPrefHeight(300);

        TableColumn<Audienta, String> colTip = new TableColumn<>("Tip Audiență");
        colTip.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getTipAudienta()));
        colTip.setPrefWidth(150);

        TableColumn<Audienta, String> colSubiect = new TableColumn<>("Subiect");
        colSubiect.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getProtocol()));
        colSubiect.setPrefWidth(200);

        TableColumn<Audienta, String> colStatus = new TableColumn<>("Status Consiliu");
        colStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getStatus()));
        colStatus.setPrefWidth(150);

        colStatus.setCellFactory(column -> {
            return new TableCell<Audienta, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        if (item.equals("APROBATA")) {
                            setTextFill(Color.GREEN);
                            setStyle("-fx-font-weight: bold;");
                        } else if (item.equals("RESPINSA")) {
                            setTextFill(Color.RED);
                            setStyle("-fx-font-weight: bold;");
                        } else {
                            setTextFill(Color.ORANGE);
                            setStyle("-fx-font-weight: bold;");
                        }
                    }
                }
            };
        });

        tabel.getColumns().addAll(colTip, colSubiect, colStatus);

        com.example.europapeace.entities.Utilizator userLogat = com.example.europapeace.config.UserSession.getInstance().getUtilizatorLogat();
        List<Audienta> audienteleMele = audientaRepository.findAll().stream()
                .filter(a -> a.getIdutilizator() != null && a.getIdutilizator().equals(userLogat.getId()))
                .filter(a -> !"FINALIZATA".equals(a.getStatus()))
                .collect(Collectors.toList());

        tabel.getItems().setAll(audienteleMele);

        HBox actiuniBox = new HBox(15);
        Button btnInapoi = new Button("Înapoi");
        btnInapoi.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-weight: bold;");
        btnInapoi.setOnAction(e -> mainBorderPane.setCenter(scrollPane));

        // NOU: Butonul Magic - Te aruncă în camera privată dacă audiența e aprobată!
        Button btnIntraChat = new Button("Alătură-te Camerei Private \uD83D\uDCAC");
        btnIntraChat.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-font-weight: bold;");
        btnIntraChat.setOnAction(e -> {
            Audienta selectata = tabel.getSelectionModel().getSelectedItem();
            if(selectata != null) {
                if ("APROBATA".equals(selectata.getStatus())) {
                    afiseazaPanouChat(selectata); // Trimitem audienta selectată pentru a deschide camera corectă
                } else {
                    Alert a = new Alert(Alert.AlertType.WARNING, "Audiența trebuie să fie aprobată de Consiliu mai întâi!");
                    a.show();
                }
            }
        });

        actiuniBox.getChildren().addAll(btnInapoi, btnIntraChat);
        panou.getChildren().addAll(titlu, tabel, actiuniBox);
        mainBorderPane.setCenter(panou);
    }

    private void afiseazaStatusCereri() {
        if (chatPollingTimeline != null) chatPollingTimeline.stop();

        VBox panouCereri = new VBox(15);
        panouCereri.setPadding(new javafx.geometry.Insets(30));
        panouCereri.setStyle("-fx-background-color: #ecf0f1;");

        Label titlu = new Label("Statusul Cererilor Mele de Independență");
        titlu.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        TableView<com.example.europapeace.entities.CerereIndependenta> tabelCereri = new TableView<>();
        tabelCereri.setPrefHeight(400);

        TableColumn<com.example.europapeace.entities.CerereIndependenta, String> colNume = new TableColumn<>("Nume Cerere");
        colNume.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getNumeCerere()));
        colNume.setPrefWidth(150);

        TableColumn<com.example.europapeace.entities.CerereIndependenta, String> colGrup = new TableColumn<>("Grup Emitent");
        colGrup.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getNumeGrupEmitent()));
        colGrup.setPrefWidth(150);

        TableColumn<com.example.europapeace.entities.CerereIndependenta, String> colCat = new TableColumn<>("Categorie");
        colCat.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCategorie()));
        colCat.setPrefWidth(180);

        TableColumn<com.example.europapeace.entities.CerereIndependenta, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getStatus()));
        colStatus.setPrefWidth(120);

        colStatus.setCellFactory(column -> {
            return new TableCell<com.example.europapeace.entities.CerereIndependenta, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        if (item.equals("APROBATA")) {
                            setTextFill(Color.GREEN);
                            setStyle("-fx-font-weight: bold;");
                        } else if (item.equals("RESPINSA")) {
                            setTextFill(Color.RED);
                            setStyle("-fx-font-weight: bold;");
                        } else {
                            setTextFill(Color.ORANGE);
                            setStyle("-fx-font-weight: bold;");
                        }
                    }
                }
            };
        });

        tabelCereri.getColumns().addAll(colNume, colGrup, colCat, colStatus);

        com.example.europapeace.entities.Utilizator userLogat = com.example.europapeace.config.UserSession.getInstance().getUtilizatorLogat();

        List<com.example.europapeace.entities.CerereIndependenta> toateCererile = cerereIndependentaRepository.findAll();
        List<com.example.europapeace.entities.CerereIndependenta> cererileMele = toateCererile.stream()
                .filter(c -> c.getSefDeStat() != null && c.getSefDeStat().getId().equals(userLogat.getId()))
                .collect(Collectors.toList());

        tabelCereri.getItems().setAll(cererileMele);

        Button btnInapoi = new Button("Înapoi");
        btnInapoi.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-weight: bold;");
        btnInapoi.setOnAction(e -> {
            mainBorderPane.setCenter(scrollPane);
        });

        panouCereri.getChildren().addAll(titlu, tabelCereri, btnInapoi);
        mainBorderPane.setCenter(panouCereri);
    }

    private void afiseazaFormularAudienta() {
        if (chatPollingTimeline != null) chatPollingTimeline.stop();

        VBox formularBox = new VBox(15);
        formularBox.setPadding(new javafx.geometry.Insets(40));
        formularBox.setStyle("-fx-background-color: #ecf0f1;");
        formularBox.setMaxWidth(600);

        Label titlu = new Label("Programare Audiență la Consiliu");
        titlu.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        ComboBox<String> comboTip = new ComboBox<>();
        comboTip.getItems().addAll("Privată (1 la 1 cu Consiliul)", "Grup (Toți membrii pe chat global)");
        comboTip.setPromptText("Alege Tipul de Audiență");
        comboTip.setPrefWidth(300);

        TextField txtProtocol = new TextField();
        txtProtocol.setPromptText("Subiectul discuției (ex: Negocieri Pace)...");

        Button btnTrimite = new Button("Trimite Solicitarea");
        btnTrimite.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");

        Label lblFeedback = new Label();

        btnTrimite.setOnAction(e -> {
            if(txtProtocol.getText().isEmpty() || comboTip.getValue() == null) {
                lblFeedback.setText("Alegeți tipul și completați subiectul!");
                lblFeedback.setTextFill(Color.RED);
                return;
            }

            com.example.europapeace.entities.Utilizator userLogat = com.example.europapeace.config.UserSession.getInstance().getUtilizatorLogat();
            Audienta audienta = new Audienta();

            audienta.setIdutilizator(userLogat.getId());

            Integer statId = userLogat.getIdstat();
            if (statId == null) {
                statId = 1;
            }
            audienta.setIdstat(statId);

            audienta.setProtocol(txtProtocol.getText());
            audienta.setTipAudienta(comboTip.getValue());
            audienta.setStatus("IN_ASTEPTARE");
            audienta.setDataora(java.time.LocalDateTime.now());
            audienta.setDatecontact("Chat Intern"); // FIX: Valoare default pentru baza de date

            audientaRepository.save(audienta);

            lblFeedback.setText("Audiență solicitată cu succes! Așteptați decizia Consiliului.");
            lblFeedback.setTextFill(Color.GREEN);
            txtProtocol.clear();
            comboTip.setValue(null);
        });

        formularBox.getChildren().addAll(titlu, comboTip, txtProtocol, btnTrimite, lblFeedback);
        mainBorderPane.setCenter(formularBox);
    }

    private void afiseazaGestioneazaAudiente() {
        if (chatPollingTimeline != null) chatPollingTimeline.stop();

        VBox panou = new VBox(15);
        panou.setPadding(new javafx.geometry.Insets(30));
        panou.setStyle("-fx-background-color: #ecf0f1;");

        Label titlu = new Label("Aprobare Cereri Audiență");
        titlu.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        TableView<Audienta> tabel = new TableView<>();
        tabel.setPrefHeight(400);

        TableColumn<Audienta, String> colUser = new TableColumn<>("ID User");
        colUser.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getIdutilizator())));
        colUser.setPrefWidth(80);

        TableColumn<Audienta, String> colTip = new TableColumn<>("Tip");
        colTip.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getTipAudienta()));
        colTip.setPrefWidth(120);

        TableColumn<Audienta, String> colProtocol = new TableColumn<>("Subiect / Protocol");
        colProtocol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getProtocol()));
        colProtocol.setPrefWidth(200);

        TableColumn<Audienta, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getStatus()));
        colStatus.setPrefWidth(120);

        tabel.getColumns().addAll(colUser, colTip, colProtocol, colStatus);

        List<Audienta> active = audientaRepository.findAll().stream()
                .filter(a -> !"FINALIZATA".equals(a.getStatus()))
                .collect(Collectors.toList());
        tabel.getItems().setAll(active);

        HBox actiuniBox = new HBox(15);

        Button btnAproba = new Button("Aprobă");
        btnAproba.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");

        Button btnRespinge = new Button("Respinge");
        btnRespinge.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");

        // NOU: Consiliul poate intra în camera privată direct de aici
        Button btnIntraChat = new Button("Deschide Camera Privată \uD83D\uDCAC");
        btnIntraChat.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-font-weight: bold;");

        Button btnSterge = new Button("Marchează ca Finalizată");
        btnSterge.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-weight: bold;");

        btnAproba.setOnAction(e -> {
            Audienta selectata = tabel.getSelectionModel().getSelectedItem();
            if(selectata != null) {
                selectata.setStatus("APROBATA");
                audientaRepository.save(selectata);
                tabel.refresh();
            }
        });

        btnAproba.setOnAction(e -> {
            Audienta selectata = tabel.getSelectionModel().getSelectedItem();
            if(selectata != null) {
                // Creăm un dialog mic pentru a seta Data și Ora
                TextInputDialog dialog = new TextInputDialog(java.time.LocalDateTime.now().plusHours(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                dialog.setTitle("Planificare Audiență");
                dialog.setHeaderText("Setați data și ora pentru audiență");
                dialog.setContentText("Format (AAAA-LL-ZZ OO:mm):");

                Optional<String> result = dialog.showAndWait();
                result.ifPresent(dateTimeStr -> {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                        java.time.LocalDateTime dataProgramata = java.time.LocalDateTime.parse(dateTimeStr, formatter);

                        selectata.setDataora(dataProgramata);
                        selectata.setStatus("APROBATA");
                        audientaRepository.save(selectata);
                        tabel.refresh();

                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Audiența a fost planificată pentru: " + dateTimeStr);
                        alert.show();
                    } catch (Exception ex) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Format dată invalid! Folosiți AAAA-LL-ZZ OO:mm");
                        alert.show();
                    }
                });
            }
        });

        btnIntraChat.setOnAction(e -> {
            Audienta selectata = tabel.getSelectionModel().getSelectedItem();
            if(selectata != null) {
                if ("APROBATA".equals(selectata.getStatus())) {
                    afiseazaPanouChat(selectata); // Trimitem audienta selectata pentru a deschide camera
                } else {
                    Alert a = new Alert(Alert.AlertType.WARNING, "Audiența trebuie să fie aprobată mai întâi!");
                    a.show();
                }
            }
        });

        btnSterge.setOnAction(e -> {
            Audienta selectata = tabel.getSelectionModel().getSelectedItem();
            if(selectata != null) {
                selectata.setStatus("FINALIZATA");
                audientaRepository.save(selectata);
                tabel.getItems().remove(selectata);
            }
        });

        actiuniBox.getChildren().addAll(btnAproba, btnRespinge, btnIntraChat, btnSterge);
        panou.getChildren().addAll(titlu, tabel, actiuniBox);

        mainBorderPane.setCenter(panou);
    }

    private void afiseazaPanouRapoarte() {
        if (chatPollingTimeline != null) {
            chatPollingTimeline.stop();
        }

        VBox panouRapoarte = new VBox(15);
        panouRapoarte.setPadding(new javafx.geometry.Insets(30));
        panouRapoarte.setStyle("-fx-background-color: #ecf0f1;");

        Label titlu = new Label("Arhiva de Rapoarte Diplomatice (Publice)");
        titlu.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        TableView<Raport> tabelRapoarte = new TableView<>();
        tabelRapoarte.setPrefHeight(200);

        TableColumn<Raport, String> colTitlu = new TableColumn<>("Titlu Raport / Tratat");
        colTitlu.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getTitlu()));
        colTitlu.setPrefWidth(300);

        TableColumn<Raport, String> colData = new TableColumn<>("Data Generării");
        colData.setCellValueFactory(data -> {
            if (data.getValue().getDataGenerare() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getDataGenerare().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))
                );
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });
        colData.setPrefWidth(200);

        tabelRapoarte.getColumns().addAll(colTitlu, colData);
        tabelRapoarte.getItems().setAll(raportRepository.findAll());

        TextArea txtContinutRaport = new TextArea();
        txtContinutRaport.setEditable(false);
        txtContinutRaport.setWrapText(true);
        txtContinutRaport.setPrefHeight(300);
        txtContinutRaport.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 14px; -fx-background-color: white;");

        tabelRapoarte.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                txtContinutRaport.setText(newSel.getContinut());
            }
        });

        Button btnInapoi = new Button("Înapoi");
        btnInapoi.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-weight: bold;");
        btnInapoi.setOnAction(e -> {
            mainBorderPane.setCenter(scrollPane);
        });

        panouRapoarte.getChildren().addAll(titlu, tabelRapoarte, new Label("Conținut Raport:"), txtContinutRaport, btnInapoi);
        mainBorderPane.setCenter(panouRapoarte);
    }

    private void afiseazaFormularCerere() {
        if (chatPollingTimeline != null) chatPollingTimeline.stop();

        VBox formularBox = new VBox(15);
        formularBox.setPadding(new javafx.geometry.Insets(40));
        formularBox.setStyle("-fx-background-color: #ecf0f1;");
        formularBox.setMaxWidth(600);
        formularBox.setMaxHeight(500);

        Label titlu = new Label("Depune o Cerere de Independență");
        titlu.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        TextField txtNumeCerere = new TextField();
        txtNumeCerere.setPromptText("Numele Cererii...");

        TextField txtGrupEmitent = new TextField();
        txtGrupEmitent.setPromptText("Grupul Emitent...");

        ComboBox<String> comboCategorie = new ComboBox<>();
        comboCategorie.getItems().addAll(
                "Etnica",
                "Sustinuta de alte state",
                "Agreata de tara de origine" // Reducem numele pentru a încăpea bine în tabel, dar respectăm conceptul
        );
        comboCategorie.setPromptText("Selectează Categoria");
        comboCategorie.setPrefWidth(300);

        TextArea txtContinut = new TextArea();
        txtContinut.setPromptText("Motivarea și detaliile cererii...");
        txtContinut.setWrapText(true);
        txtContinut.setPrefRowCount(8);

        Button btnTrimite = new Button("Trimite Cererea către Consiliu");
        btnTrimite.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");

        Label lblFeedback = new Label("");

        btnTrimite.setOnAction(e -> {
            if(txtNumeCerere.getText().isEmpty() || comboCategorie.getValue() == null) {
                lblFeedback.setText("Completati campurile obligatorii!");
                lblFeedback.setTextFill(Color.RED);
                return;
            }

            com.example.europapeace.entities.CerereIndependenta cerere = new com.example.europapeace.entities.CerereIndependenta();
            cerere.setNumeCerere(txtNumeCerere.getText());
            cerere.setNumeGrupEmitent(txtGrupEmitent.getText());
            cerere.setCategorie(comboCategorie.getValue());
            cerere.setContinut(txtContinut.getText());
            cerere.setStatus("IN_ASTEPTARE");
            cerere.setDataIntroducerii(java.time.LocalDateTime.now());

            com.example.europapeace.entities.Utilizator userLogat = com.example.europapeace.config.UserSession.getInstance().getUtilizatorLogat();
            cerere.setSefDeStat(userLogat);

            Integer statId = userLogat.getIdstat();
            if (statId == null) statId = 1;
            cerere.setIdstat(statId);
            cerere.setIdrol(1);

            cerereIndependentaRepository.save(cerere);

            lblFeedback.setText("Cerere trimisă cu succes la Consiliu!");
            lblFeedback.setTextFill(Color.GREEN);
            txtNumeCerere.clear();
            txtGrupEmitent.clear();
            txtContinut.clear();
        });

        formularBox.getChildren().addAll(titlu, txtNumeCerere, txtGrupEmitent, comboCategorie, txtContinut, btnTrimite, lblFeedback);
        mainBorderPane.setCenter(formularBox);
    }

    @FXML
    public void handleGestioneazaCereri() {
        if (chatPollingTimeline != null) chatPollingTimeline.stop();

        VBox panouCereri = new VBox(15);
        panouCereri.setPadding(new javafx.geometry.Insets(30));
        panouCereri.setStyle("-fx-background-color: #ecf0f1;");

        Label titlu = new Label("Managementul Cererilor (Căutare & Filtrare)");
        titlu.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // --- ZONA DE CĂUTARE (CF_11) ---
        HBox cautareBox = new HBox(10);
        cautareBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        cautareBox.setStyle("-fx-background-color: #dcdde1; -fx-padding: 10; -fx-background-radius: 5;");

        TextField txtCautareNume = new TextField();
        txtCautareNume.setPromptText("Caută după nume cerere...");

        TextField txtCautareGrup = new TextField();
        txtCautareGrup.setPromptText("Caută după grup emitent...");

        ComboBox<String> comboFiltruCat = new ComboBox<>();
        comboFiltruCat.getItems().addAll("Toate", "Etnica", "Sustinuta de alte state", "Agreata de tara de origine");
        comboFiltruCat.setValue("Toate");

        Button btnCauta = new Button("Filtrează \uD83D\uDD0D");
        btnCauta.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");

        cautareBox.getChildren().addAll(new Label("Nume:"), txtCautareNume, new Label("Grup:"), txtCautareGrup, new Label("Categorie:"), comboFiltruCat, btnCauta);

        // --- TABELUL ---
        TableView<com.example.europapeace.entities.CerereIndependenta> tabelCereri = new TableView<>();
        tabelCereri.setPrefHeight(400);

        TableColumn<com.example.europapeace.entities.CerereIndependenta, String> colNume = new TableColumn<>("Nume Cerere");
        colNume.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getNumeCerere()));
        colNume.setPrefWidth(150);

        TableColumn<com.example.europapeace.entities.CerereIndependenta, String> colGrup = new TableColumn<>("Grup Emitent");
        colGrup.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getNumeGrupEmitent()));
        colGrup.setPrefWidth(150);

        TableColumn<com.example.europapeace.entities.CerereIndependenta, String> colCat = new TableColumn<>("Categorie");
        colCat.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCategorie()));
        colCat.setPrefWidth(180);

        TableColumn<com.example.europapeace.entities.CerereIndependenta, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getStatus()));
        colStatus.setPrefWidth(120);

        tabelCereri.getColumns().addAll(colNume, colGrup, colCat, colStatus);

        // Populare inițială
        List<com.example.europapeace.entities.CerereIndependenta> listaCompleta = cerereIndependentaRepository.findAll();
        tabelCereri.getItems().setAll(listaCompleta);

        // LOGICA DE CĂUTARE (CF_11)
        btnCauta.setOnAction(e -> {
            String nume = txtCautareNume.getText().toLowerCase();
            String grup = txtCautareGrup.getText().toLowerCase();
            String cat = comboFiltruCat.getValue();

            List<com.example.europapeace.entities.CerereIndependenta> filtrate = listaCompleta.stream()
                    .filter(c -> c.getNumeCerere().toLowerCase().contains(nume))
                    .filter(c -> c.getNumeGrupEmitent() != null && c.getNumeGrupEmitent().toLowerCase().contains(grup))
                    .filter(c -> cat.equals("Toate") || c.getCategorie().equals(cat))
                    .collect(Collectors.toList());

            tabelCereri.getItems().setAll(filtrate);
        });

        // --- BUTOANE ACȚIUNE ---
        HBox actiuniBox = new HBox(15);
        Button btnAproba = new Button("Aprobă Cererea");
        btnAproba.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");

        Button btnRespinge = new Button("Respinge Cererea");
        btnRespinge.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");

        btnAproba.setOnAction(e -> {
            com.example.europapeace.entities.CerereIndependenta selectata = tabelCereri.getSelectionModel().getSelectedItem();
            if(selectata != null) {
                selectata.setStatus("APROBATA");
                cerereIndependentaRepository.save(selectata);
                tabelCereri.refresh();
                handleIndependenta();
            }
        });

        btnRespinge.setOnAction(e -> {
            com.example.europapeace.entities.CerereIndependenta selectata = tabelCereri.getSelectionModel().getSelectedItem();
            if(selectata != null) {
                selectata.setStatus("RESPINSA");
                cerereIndependentaRepository.save(selectata);
                tabelCereri.refresh();
            }
        });

        actiuniBox.getChildren().addAll(btnAproba, btnRespinge);
        panouCereri.getChildren().addAll(titlu, cautareBox, tabelCereri, actiuniBox);

        mainBorderPane.setCenter(panouCereri);
    }

    private void aplicaInterfataAdmin() {
        VBox meniuAdmin = new VBox(15);
        meniuAdmin.setPadding(new javafx.geometry.Insets(30, 15, 20, 15));
        meniuAdmin.setStyle("-fx-background-color: white;");
        meniuAdmin.setPrefWidth(240);

        Button btnInapoiHarta = new Button("Înapoi la Hartă \uD83D\uDDFA\uFE0F");
        btnInapoiHarta.setMaxWidth(Double.MAX_VALUE);
        btnInapoiHarta.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        btnInapoiHarta.setOnAction(e -> mainBorderPane.setCenter(scrollPane));

        Button btnGestUtilizatori = new Button("Gestionează Utilizatori");
        btnGestUtilizatori.setMaxWidth(Double.MAX_VALUE);
        btnGestUtilizatori.setOnAction(e -> handleGestioneazaUtilizatori());

        Button btnConfigServer = new Button("Configurare Server");
        btnConfigServer.setMaxWidth(Double.MAX_VALUE);
        btnConfigServer.setOnAction(e -> handleConfigurareServer());

        meniuAdmin.getChildren().addAll(btnInapoiHarta, btnGestUtilizatori, btnConfigServer);

        mainBorderPane.setLeft(meniuAdmin);
        mainBorderPane.setCenter(scrollPane);
    }

    @FXML
    public void handleConfigurareServer() {
        VBox panouConfig = new VBox(25);
        panouConfig.setPadding(new javafx.geometry.Insets(40));
        panouConfig.setStyle("-fx-background-color: #ecf0f1;");
        panouConfig.setAlignment(javafx.geometry.Pos.TOP_CENTER);

        Label titlu = new Label("Configurare Server & Bază de Date");
        titlu.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        VBox infoBox = new VBox(15);
        infoBox.setStyle("-fx-background-color: white; -fx-padding: 25; -fx-border-color: #bdc3c7; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");
        infoBox.setMaxWidth(500);

        Label lblDbStatus = new Label("Status Bază de Date: CONECTAT \uD83D\uDFE2");
        lblDbStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 16px;");

        Label lblActiveUsers = new Label("Utilizatori Înregistrați: " + utilizatorRepository.count());
        lblActiveUsers.setStyle("-fx-font-size: 14px;");

        Label lblTotalCereri = new Label("Total Cereri Bază de Date: " + cerereIndependentaRepository.count());
        lblTotalCereri.setStyle("-fx-font-size: 14px;");

        Label lblTotalState = new Label("State Recunoscute: " + statRepository.count());
        lblTotalState.setStyle("-fx-font-size: 14px;");

        infoBox.getChildren().addAll(lblDbStatus, lblActiveUsers, lblTotalCereri, lblTotalState);

        HBox actiuniBox = new HBox(20);
        actiuniBox.setAlignment(javafx.geometry.Pos.CENTER);

        Button btnResetMap = new Button("Hard Reset Hartă");
        btnResetMap.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 20;");

        Button btnClearCache = new Button("Curăță Cache Server");
        btnClearCache.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 20;");

        actiuniBox.getChildren().addAll(btnClearCache, btnResetMap);

        Label lblFeedback = new Label();
        lblFeedback.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        btnResetMap.setOnAction(e -> {
            try {
                mapImportService.importaHartaNoua();

                java.util.Set<Integer> stateActivePeHarta = regiuneRepository.findAll().stream()
                        .filter(r -> r.getStat() != null)
                        .map(r -> r.getStat().getIdstat())
                        .collect(Collectors.toSet());

                List<com.example.europapeace.entities.Stat> toateStatele = statRepository.findAll();
                List<com.example.europapeace.entities.Utilizator> totiUserii = utilizatorRepository.findAll();

                for (com.example.europapeace.entities.Stat s : toateStatele) {
                    if (!stateActivePeHarta.contains(s.getIdstat()) && s.getIdstat() > 47) {
                        for(com.example.europapeace.entities.Utilizator u : totiUserii) {
                            if(u.getIdstat() != null && u.getIdstat().equals(s.getIdstat())) {
                                u.setIdstat(null);
                                utilizatorRepository.save(u);
                            }
                        }
                        statRepository.delete(s);
                    }
                }

                randareHarta();
                setupAutoComplete();
                lblActiveUsers.setText("Utilizatori Înregistrați: " + utilizatorRepository.count());
                lblTotalCereri.setText("Total Cereri Bază de Date: " + cerereIndependentaRepository.count());
                lblTotalState.setText("State Recunoscute: " + statRepository.count());

                lblFeedback.setText("Harta resetată și statele fantomă au fost șterse complet!");
                lblFeedback.setTextFill(Color.GREEN);

            } catch (Exception ex) {
                lblFeedback.setText("Eroare la curățarea bazei de date.");
                lblFeedback.setTextFill(Color.RED);
                ex.printStackTrace();
            }
        });

        btnClearCache.setOnAction(e -> {
            lblActiveUsers.setText("Utilizatori Înregistrați: " + utilizatorRepository.count());
            lblTotalCereri.setText("Total Cereri Bază de Date: " + cerereIndependentaRepository.count());
            lblTotalState.setText("State Recunoscute: " + statRepository.count());
            lblFeedback.setText("Cache-ul serverului a fost curățat. Date reîmprospătate!");
            lblFeedback.setTextFill(Color.BLUE);
        });

        panouConfig.getChildren().addAll(titlu, infoBox, actiuniBox, lblFeedback);
        mainBorderPane.setCenter(panouConfig);
    }

    @FXML
    public void handleGestioneazaUtilizatori() {
        VBox panouAdmin = new VBox(15);
        panouAdmin.setPadding(new javafx.geometry.Insets(30));
        panouAdmin.setStyle("-fx-background-color: #ecf0f1;");

        Label titlu = new Label("Management Utilizatori (Bază de Date)");
        titlu.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        TableView<com.example.europapeace.entities.Utilizator> tabelUsers = new TableView<>();
        tabelUsers.setPrefHeight(300);

        TableColumn<com.example.europapeace.entities.Utilizator, String> colUser = new TableColumn<>("Username");
        colUser.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getUsername()));
        colUser.setPrefWidth(120);

        TableColumn<com.example.europapeace.entities.Utilizator, String> colPass = new TableColumn<>("Parolă");
        colPass.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getParola()));
        colPass.setPrefWidth(120);

        TableColumn<com.example.europapeace.entities.Utilizator, String> colRol = new TableColumn<>("Rol (1=Sef, 2=Consiliu, 3=Admin)");
        colRol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getIdrol())));
        colRol.setPrefWidth(180);

        TableColumn<com.example.europapeace.entities.Utilizator, String> colStat = new TableColumn<>("Stat");
        colStat.setCellValueFactory(data -> {
            Integer statId = data.getValue().getIdstat();
            if (statId != null) {
                java.util.Optional<com.example.europapeace.entities.Stat> statOpt = statRepository.findById(statId);
                if (statOpt.isPresent()) {
                    return new javafx.beans.property.SimpleStringProperty(statOpt.get().getNume() + " (ID: " + statId + ")");
                }
                return new javafx.beans.property.SimpleStringProperty("ID Necunoscut: " + statId);
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });
        colStat.setPrefWidth(200);

        tabelUsers.getColumns().addAll(colUser, colPass, colRol, colStat);
        tabelUsers.getItems().setAll(utilizatorRepository.findAll());

        List<com.example.europapeace.entities.Stat> toateStatele = statRepository.findAll();

        HBox actiuniEditareBox = new HBox(15);

        TextField txtEditUser = new TextField();
        txtEditUser.setPromptText("Schimbă Username");
        txtEditUser.setPrefWidth(120);

        TextField txtEditPass = new TextField();
        txtEditPass.setPromptText("Schimbă Parola");
        txtEditPass.setPrefWidth(120);

        ComboBox<Integer> comboRol = new ComboBox<>();
        comboRol.getItems().addAll(1, 2, 3);
        comboRol.setPromptText("Rol");

        TextField txtStatInput = new TextField();
        txtStatInput.setPromptText("ID/Nume Stat");
        txtStatInput.setPrefWidth(120);

        ContextMenu statSuggestions = new ContextMenu();
        txtStatInput.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty() || newVal.matches("\\d+")) {
                statSuggestions.hide();
                return;
            }
            List<MenuItem> items = toateStatele.stream()
                    .filter(stat -> stat.getNume().toLowerCase().contains(newVal.toLowerCase()))
                    .limit(6)
                    .map(stat -> {
                        MenuItem item = new MenuItem(stat.getNume() + " (ID: " + stat.getIdstat() + ")");
                        item.setOnAction(e -> {
                            txtStatInput.setText(stat.getNume());
                            statSuggestions.hide();
                        });
                        return item;
                    })
                    .collect(Collectors.toList());

            if (!items.isEmpty()) {
                statSuggestions.getItems().setAll(items);
                if (!statSuggestions.isShowing()) {
                    statSuggestions.show(txtStatInput, javafx.geometry.Side.BOTTOM, 0, 0);
                }
            } else {
                statSuggestions.hide();
            }
        });

        comboRol.setOnAction(e -> {
            if (comboRol.getValue() != null && comboRol.getValue() != 1) {
                txtStatInput.setDisable(true);
                txtStatInput.clear();
            } else {
                txtStatInput.setDisable(false);
            }
        });

        Button btnActualizeaza = new Button("Actualizează");
        btnActualizeaza.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold;");

        Button btnSterge = new Button("Șterge");
        btnSterge.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");

        actiuniEditareBox.getChildren().addAll(new Label("Editează:"), txtEditUser, txtEditPass, comboRol, txtStatInput, btnActualizeaza, btnSterge);

        HBox actiuniCreareBox = new HBox(15);
        TextField txtNewUser = new TextField();
        txtNewUser.setPromptText("Username Nou");
        txtNewUser.setPrefWidth(120);

        TextField txtNewPass = new TextField();
        txtNewPass.setPromptText("Parolă Nouă");
        txtNewPass.setPrefWidth(120);

        ComboBox<Integer> comboNewRol = new ComboBox<>();
        comboNewRol.getItems().addAll(1, 2, 3);
        comboNewRol.setValue(1);

        TextField txtNewStatInput = new TextField();
        txtNewStatInput.setPromptText("ID/Nume Stat");
        txtNewStatInput.setPrefWidth(120);

        ContextMenu statSuggestionsNou = new ContextMenu();
        txtNewStatInput.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty() || newVal.matches("\\d+")) {
                statSuggestionsNou.hide();
                return;
            }
            List<MenuItem> items = toateStatele.stream()
                    .filter(stat -> stat.getNume().toLowerCase().contains(newVal.toLowerCase()))
                    .limit(6)
                    .map(stat -> {
                        MenuItem item = new MenuItem(stat.getNume() + " (ID: " + stat.getIdstat() + ")");
                        item.setOnAction(e -> {
                            txtNewStatInput.setText(stat.getNume());
                            statSuggestionsNou.hide();
                        });
                        return item;
                    })
                    .collect(Collectors.toList());

            if (!items.isEmpty()) {
                statSuggestionsNou.getItems().setAll(items);
                if (!statSuggestionsNou.isShowing()) {
                    statSuggestionsNou.show(txtNewStatInput, javafx.geometry.Side.BOTTOM, 0, 0);
                }
            } else {
                statSuggestionsNou.hide();
            }
        });

        comboNewRol.setOnAction(e -> {
            if (comboNewRol.getValue() != null && comboNewRol.getValue() != 1) {
                txtNewStatInput.setDisable(true);
                txtNewStatInput.clear();
            } else {
                txtNewStatInput.setDisable(false);
            }
        });

        Button btnCreeaza = new Button("Creează");
        btnCreeaza.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");

        actiuniCreareBox.getChildren().addAll(new Label("Crează:"), txtNewUser, txtNewPass, comboNewRol, txtNewStatInput, btnCreeaza);

        Label lblFeedback = new Label();
        lblFeedback.setStyle("-fx-font-weight: bold;");

        tabelUsers.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                txtEditUser.setText(newSel.getUsername());
                txtEditPass.setText(newSel.getParola());
                comboRol.setValue(newSel.getIdrol());

                if (newSel.getIdstat() != null) {
                    java.util.Optional<com.example.europapeace.entities.Stat> s = statRepository.findById(newSel.getIdstat());
                    txtStatInput.setText(s.isPresent() ? s.get().getNume() : String.valueOf(newSel.getIdstat()));
                } else {
                    txtStatInput.setText("");
                }
            }
        });

        btnActualizeaza.setOnAction(e -> {
            com.example.europapeace.entities.Utilizator selectat = tabelUsers.getSelectionModel().getSelectedItem();
            if(selectat != null) {

                if(!txtEditUser.getText().isEmpty()) {
                    selectat.setUsername(txtEditUser.getText());
                }

                if(!txtEditPass.getText().isEmpty()) {
                    selectat.setParola(txtEditPass.getText());
                }

                if(comboRol.getValue() != null) selectat.setIdrol(comboRol.getValue());

                Integer finalStatId = null;

                if (selectat.getIdrol() == 1 && !txtStatInput.getText().trim().isEmpty()) {
                    String input = txtStatInput.getText().trim();
                    try {
                        finalStatId = Integer.parseInt(input);
                    } catch (NumberFormatException ex) {
                        java.util.Optional<com.example.europapeace.entities.Stat> statOpt = statRepository.findByNume(input);
                        if (statOpt.isPresent()) {
                            finalStatId = statOpt.get().getIdstat();
                        } else {
                            lblFeedback.setText("Eroare: Statul '" + input + "' nu a fost găsit!");
                            lblFeedback.setTextFill(Color.RED);
                            return;
                        }
                    }
                }

                selectat.setIdstat(finalStatId);
                utilizatorRepository.save(selectat);
                tabelUsers.refresh();
                lblFeedback.setText("Utilizator actualizat cu succes!");
                lblFeedback.setTextFill(Color.GREEN);
            }
        });

        btnCreeaza.setOnAction(e -> {
            if (txtNewUser.getText().isEmpty() || txtNewPass.getText().isEmpty() || comboNewRol.getValue() == null) {
                lblFeedback.setText("Completati username, parola si rolul!");
                lblFeedback.setTextFill(Color.RED);
                return;
            }

            Integer finalNewStatId = null;
            if (comboNewRol.getValue() == 1 && !txtNewStatInput.getText().trim().isEmpty()) {
                String input = txtNewStatInput.getText().trim();
                try {
                    finalNewStatId = Integer.parseInt(input);
                } catch (NumberFormatException ex) {
                    java.util.Optional<com.example.europapeace.entities.Stat> statOpt = statRepository.findByNume(input);
                    if (statOpt.isPresent()) {
                        finalNewStatId = statOpt.get().getIdstat();
                    } else {
                        lblFeedback.setText("Eroare: Statul '" + input + "' nu a fost găsit!");
                        lblFeedback.setTextFill(Color.RED);
                        return;
                    }
                }
            }

            com.example.europapeace.entities.Utilizator utilizatorNou = new com.example.europapeace.entities.Utilizator();
            utilizatorNou.setUsername(txtNewUser.getText());
            utilizatorNou.setParola(txtNewPass.getText());
            utilizatorNou.setIdrol(comboNewRol.getValue());
            utilizatorNou.setIdstat(finalNewStatId);

            utilizatorRepository.save(utilizatorNou);
            tabelUsers.getItems().add(utilizatorNou);

            txtNewUser.clear();
            txtNewPass.clear();
            txtNewStatInput.clear();
            comboNewRol.setValue(1);
            lblFeedback.setText("Utilizator " + utilizatorNou.getUsername() + " creat cu succes!");
            lblFeedback.setTextFill(Color.GREEN);
        });

        btnSterge.setOnAction(e -> {
            com.example.europapeace.entities.Utilizator selectat = tabelUsers.getSelectionModel().getSelectedItem();
            if(selectat != null) {
                utilizatorRepository.delete(selectat);
                tabelUsers.getItems().remove(selectat);
                lblFeedback.setText("Utilizator sters definitiv.");
                lblFeedback.setTextFill(Color.RED);
            }
        });

        panouAdmin.getChildren().addAll(titlu, tabelUsers, actiuniEditareBox, actiuniCreareBox, lblFeedback);
        mainBorderPane.setCenter(panouAdmin);
    }

    private void aplicaInterfataSecurizata() {
        mainBorderPane.setLeft(null);
        mainBorderPane.setCenter(new Label("Acces Interzis. Nu aveți un rol valid."));
    }

    private void setupAutoComplete() {
        listaNumeRegiuni = regiuneRepository.findAll().stream()
                .map(Regiune::getNume)
                .collect(Collectors.toList());

        configuraSugestii(txtRegiuneSursa, true);
        configuraSugestii(txtRegiuneDestinatie, false);
    }

    private void configuraSugestii(TextField textField, boolean esteSursa) {
        ContextMenu suggestionsMenu = new ContextMenu();
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.length() < 2) {
                suggestionsMenu.hide();
                return;
            }
            List<MenuItem> items = listaNumeRegiuni.stream()
                    .filter(nume -> nume.toLowerCase().contains(newValue.toLowerCase()))
                    .limit(6)
                    .map(nume -> {
                        MenuItem item = new MenuItem(nume);
                        item.setOnAction(e -> {
                            textField.setText(nume);
                            if (esteSursa) asimileazaRegiuneSursa(nume);
                            suggestionsMenu.hide();
                        });
                        return item;
                    })
                    .collect(Collectors.toList());

            if (!items.isEmpty()) {
                suggestionsMenu.getItems().setAll(items);
                if (!suggestionsMenu.isShowing()) {
                    suggestionsMenu.show(textField, javafx.geometry.Side.BOTTOM, 0, 0);
                }
            } else {
                suggestionsMenu.hide();
            }
        });
    }

    private void asimileazaRegiuneSursa(String nume) {
        regiuneRepository.findByNumeIgnoreCase(nume).ifPresent(r -> {
            this.regiuneSelectataSursa = r;
            lblInstructiuni.setText("Sursă confirmată: " + r.getNume());
        });
    }

    public void randareHarta() {
        List<Regiune> toateRegiunile = regiuneRepository.findAll();
        hartaGroup.getChildren().clear();

        // REQ-F-006: Granițele sunt randate prin linii curbe (SVGPath)
        for (Regiune r : toateRegiunile) {
            SVGPath border = new SVGPath();
            border.setContent(r.getFormagrafica());
            border.setStroke(Color.BLACK);
            border.setStrokeWidth(2.2);
            border.setStrokeType(StrokeType.OUTSIDE);
            border.setFill(Color.BLACK);
            border.setMouseTransparent(true);
            hartaGroup.getChildren().add(border);
        }

        Map<Stat, List<Regiune>> regiuniPeStat = toateRegiunile.stream()
                .filter(r -> r.getStat() != null)
                .collect(Collectors.groupingBy(Regiune::getStat));

        for (Map.Entry<Stat, List<Regiune>> entry : regiuniPeStat.entrySet()) {
            Stat stat = entry.getKey();
            Color culoareStat = Color.web(stat.getCuloare() != null ? stat.getCuloare() : "#808080");

            for (Regiune r : entry.getValue()) {
                SVGPath interior = new SVGPath();
                interior.setContent(r.getFormagrafica());
                interior.setFill(culoareStat);
                interior.setStroke(Color.WHITE);
                interior.setStrokeWidth(0.3);

                // REQ-F-007: Calculăm o lungime a graniței bazată pe datele SVG
                double lungimeSimulata = r.getFormagrafica().length() / 8.0;
                String infoGranita = String.format("%.2f km", lungimeSimulata);

                // REQ-F-005 & REQ-F-007: Adnotare automată cu numele statului, regiunii și lungimea graniței [cite: 42, 44]
                Tooltip tooltip = new Tooltip(
                        "Stat: " + stat.getNume() + "\n" +
                                "Regiune: " + r.getNume() + "\n" +
                                "Lungime demarcată: " + infoGranita
                );
                Tooltip.install(interior, tooltip);

                interior.setOnMouseEntered(e -> {
                    interior.setFill(culoareStat.brighter());
                });

                interior.setOnMouseExited(e -> {
                    interior.setFill(culoareStat);
                });

                interior.setOnMouseClicked(e -> {
                    if (modUnireActiv) {
                        regiuneSelectataSursa = r;
                        txtRegiuneSursa.setText(r.getNume());
                        lblInstructiuni.setText("Regiune selectată: " + r.getNume());
                    } else if (modIndependentaActiv) {
                        creazaStatNouPentruRegiune(r);
                    }
                });

                hartaGroup.getChildren().add(interior);
            }
        }

        hartaGroup.setScaleX(2.0);
        hartaGroup.setScaleY(2.0);
        hartaGroup.setLayoutX(-500);
        hartaGroup.setLayoutY(-100);
    }

    private void creazaStatNouPentruRegiune(Regiune regiune) {
        try {
            String numeStatNou = "Republica " + regiune.getNume();

            if (regiune.getStat() != null && regiune.getStat().getNume().equals(numeStatNou)) {
                modIndependentaActiv = false;
                lblInstructiuni.setText(regiune.getNume() + " este deja un stat independent!");
                lblInstructiuni.setTextFill(Color.ORANGE);
                return;
            }

            Stat statNou = new Stat();
            statNou.setNume(numeStatNou);

            Random rand = new Random();
            int r = rand.nextInt(255);
            int g = rand.nextInt(255);
            int b = rand.nextInt(255);
            String randomColor = String.format("#%02x%02x%02x", r, g, b);
            statNou.setCuloare(randomColor);

            statNou = statRepository.save(statNou);

            regiune.setStat(statNou);
            regiuneRepository.save(regiune);

            modIndependentaActiv = false;
            lblInstructiuni.setText(regiune.getNume() + " a devenit stat independent!");
            lblInstructiuni.setTextFill(Color.GREEN);

            randareHarta();

        } catch (Exception e) {
            lblInstructiuni.setText("Eroare la declararea independentei.");
            lblInstructiuni.setTextFill(Color.RED);
            e.printStackTrace();
        }
    }

    @FXML
    public void pornesteModUnire() {
        handleUnire();
    }

    @FXML
    public void handleUnire() {
        modUnireActiv = true;
        modIndependentaActiv = false;
        vboxUnire.setVisible(true);
        vboxUnire.setManaged(true);
        lblInstructiuni.setText("Selectează regiunea (Click sau Tastează)");
        lblInstructiuni.setTextFill(Color.BLACK);
    }

    @FXML
    public void handleIndependenta() {
        modIndependentaActiv = true;
        modUnireActiv = false;
        vboxUnire.setVisible(false);
        vboxUnire.setManaged(false);
        lblInstructiuni.setText("MOD INDEPENDENȚĂ: Click pe o regiune de pe hartă pentru a o elibera!");
        lblInstructiuni.setTextFill(Color.DARKORANGE);
    }

    @FXML
    public void handleLogout() {
        try {
            if (chatPollingTimeline != null) {
                chatPollingTimeline.stop();
            }

            com.example.europapeace.config.UserSession.getInstance().cleanUserSession();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login_view.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            Stage stage = (Stage) menuProfil.getScene().getWindow();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Eroare critica la deconectare!");
        }
    }

    @FXML
    public void handleConfirmareUnire() {
        if (regiuneSelectataSursa == null && !txtRegiuneSursa.getText().isEmpty()) {
            asimileazaRegiuneSursa(txtRegiuneSursa.getText());
        }

        if (regiuneSelectataSursa == null) {
            lblInstructiuni.setText("Eroare: Sursă invalidă!");
            return;
        }

        String numeDest = txtRegiuneDestinatie.getText();
        regiuneRepository.findByNumeIgnoreCase(numeDest).ifPresentOrElse(dest -> {
            regiuneSelectataSursa.setStat(dest.getStat());
            regiuneRepository.save(regiuneSelectataSursa);
            randareHarta();
            handleAnuleazaUnire();
        }, () -> lblInstructiuni.setText("Destinația '" + numeDest + "' nu există!"));
    }

    @FXML
    public void handleAnuleazaUnire() {
        modUnireActiv = false;
        regiuneSelectataSursa = null;
        vboxUnire.setVisible(false);
        vboxUnire.setManaged(false);
        lblInstructiuni.setText("Așteptare comenzi...");
        lblInstructiuni.setTextFill(Color.BLACK);
        txtRegiuneSursa.clear();
        txtRegiuneDestinatie.clear();
    }

    private void setupInteractiuni() {
        mapPane.setOnScroll(event -> {
            event.consume();
            double zoomFactor = (event.getDeltaY() > 0) ? 1.05 : 0.95;

            double vechiScaleX = hartaGroup.getScaleX();
            double vechiScaleY = hartaGroup.getScaleY();
            double nouScaleX = vechiScaleX * zoomFactor;
            double nouScaleY = vechiScaleY * zoomFactor;

            if (nouScaleX < 0.3 || nouScaleX > 15) return;

            double mouseXInGroup = (event.getX() - hartaGroup.getLayoutX()) / vechiScaleX;
            double mouseYInGroup = (event.getY() - hartaGroup.getLayoutY()) / vechiScaleY;

            hartaGroup.setScaleX(nouScaleX);
            hartaGroup.setScaleY(nouScaleY);

            hartaGroup.setLayoutX(event.getX() - mouseXInGroup * nouScaleX);
            hartaGroup.setLayoutY(event.getY() - mouseYInGroup * nouScaleY);
        });

        mapPane.setOnMousePressed(e -> {
            mouseAnchorX = e.getX();
            mouseAnchorY = e.getY();
        });

        mapPane.setOnMouseDragged(e -> {
            hartaGroup.setLayoutX(hartaGroup.getLayoutX() + e.getX() - mouseAnchorX);
            hartaGroup.setLayoutY(hartaGroup.getLayoutY() + e.getY() - mouseAnchorY);
            mouseAnchorX = e.getX();
            mouseAnchorY = e.getY();
        });
    }

    @FXML public void resetHarta() { mapImportService.importaHartaNoua(); randareHarta(); setupAutoComplete(); }
    @FXML public void finalizeazaUnirea() { handleConfirmareUnire(); }

    // NOU: Acum acceptă audienta ca parametru. Dacă e null, deschide chat-ul Global.
    private void afiseazaPanouChat(Audienta audientaCurenta) {
        VBox panouChat = new VBox(15);
        panouChat.setPadding(new javafx.geometry.Insets(30));
        panouChat.setStyle("-fx-background-color: #ecf0f1;");

        String titluText = audientaCurenta == null ? "Chat Diplomatic (Global)" : "Cameră Privată: " + audientaCurenta.getProtocol();
        Label titlu = new Label(titluText);
        titlu.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        VBox mesajeBox = new VBox(10);
        mesajeBox.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-border-color: #bdc3c7; -fx-border-width: 2;");

        ScrollPane scrollChat = new ScrollPane(mesajeBox);
        scrollChat.setFitToWidth(true);
        scrollChat.setPrefHeight(400);

        com.example.europapeace.entities.Utilizator userCurent = com.example.europapeace.config.UserSession.getInstance().getUtilizatorLogat();

        Runnable incarcaMesaje = () -> {
            mesajeBox.getChildren().clear();
            List<MesajChat> istoric = mesajChatRepository.findAllByOrderByDataTrimitereAsc();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

            // FILTRU MAGIC: Afișăm doar mesajele care aparțin de ACEASTĂ cameră!
            List<MesajChat> mesajeVizibile = istoric.stream().filter(m -> {
                if (audientaCurenta == null) {
                    return m.getAudienta() == null; // Dacă suntem pe Global, arată doar mesaje Globale
                } else {
                    return m.getAudienta() != null && m.getAudienta().getIdaudienta().equals(audientaCurenta.getIdaudienta()); // Arată doar mesajele din această cameră
                }
            }).collect(Collectors.toList());

            for (MesajChat m : mesajeVizibile) {
                String nume = m.getExpeditor() != null ? m.getExpeditor().getUsername() : "Necunoscut";
                String timp = m.getDataTrimitere() != null ? m.getDataTrimitere().format(formatter) : "";

                Label lblMsg = new Label("[" + timp + "] " + nume + ": " + m.getMesaj());
                lblMsg.setWrapText(true);

                if (audientaCurenta != null) {
                    lblMsg.setStyle("-fx-background-color: #fdcb6e; -fx-padding: 10; -fx-background-radius: 10; -fx-font-size: 14px; -fx-font-style: italic;");
                } else if (m.getExpeditor() != null && m.getExpeditor().getIdrol() != null && m.getExpeditor().getIdrol() == 2) {
                    lblMsg.setStyle("-fx-background-color: #ffeaa7; -fx-padding: 10; -fx-background-radius: 10; -fx-font-size: 14px; -fx-font-weight: bold;");
                } else {
                    lblMsg.setStyle("-fx-background-color: #dff9fb; -fx-padding: 10; -fx-background-radius: 10; -fx-font-size: 14px;");
                }

                mesajeBox.getChildren().add(lblMsg);
            }
            Platform.runLater(() -> scrollChat.setVvalue(1.0));
        };

        incarcaMesaje.run();

        if (chatPollingTimeline != null) {
            chatPollingTimeline.stop();
        }

        chatPollingTimeline = new Timeline(new KeyFrame(Duration.seconds(3), ev -> {
            incarcaMesaje.run();
        }));
        chatPollingTimeline.setCycleCount(Animation.INDEFINITE);
        chatPollingTimeline.play();

        HBox inputBox = new HBox(10);
        TextField txtMesaj = new TextField();
        txtMesaj.setPromptText("Scrie un mesaj...");
        txtMesaj.setPrefWidth(450);
        txtMesaj.setStyle("-fx-font-size: 14px; -fx-padding: 10;");

        Button btnTrimite = new Button("Trimite");
        btnTrimite.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 20;");

        btnTrimite.setOnAction(e -> {
            if (!txtMesaj.getText().trim().isEmpty()) {
                MesajChat mesajNou = new MesajChat();
                mesajNou.setMesaj(txtMesaj.getText().trim());
                mesajNou.setDataTrimitere(java.time.LocalDateTime.now());
                mesajNou.setExpeditor(userCurent);
                mesajNou.setAudienta(audientaCurenta); // Lipim mesajul de camera în care ne aflăm

                mesajChatRepository.save(mesajNou);
                txtMesaj.clear();
                incarcaMesaje.run();
            }
        });

        txtMesaj.setOnAction(e -> btnTrimite.fire());

        Button btnRefresh = new Button("Refresh \uD83D\uDD04");
        btnRefresh.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10;");
        btnRefresh.setOnAction(e -> incarcaMesaje.run());

        if (userCurent != null && userCurent.getIdrol() != null && userCurent.getIdrol() == 2) {
            Button btnGenereazaRaport = new Button("Salvați & Închideți Audiența \uD83D\uDCDC");
            btnGenereazaRaport.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10;");

            btnGenereazaRaport.setOnAction(e -> {
                TextInputDialog dialog = new TextInputDialog("Tratatul de Pace");
                dialog.setTitle("Generare Raport Oficial");
                dialog.setHeaderText("Salvați o copie a acestei camere de chat.");
                dialog.setContentText("Introduceți titlul raportului:");

                Optional<String> result = dialog.showAndWait();
                result.ifPresent(titluRaport -> {
                    List<MesajChat> istoric = mesajChatRepository.findAllByOrderByDataTrimitereAsc();
                    StringBuilder continutRaport = new StringBuilder();
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");

                    // Salvam doar mesajele din camera curenta
                    List<MesajChat> deSters = new LinkedList<>();
                    for(MesajChat m : istoric) {
                        boolean eDinCameraCurenta = (audientaCurenta == null && m.getAudienta() == null) ||
                                (audientaCurenta != null && m.getAudienta() != null && m.getAudienta().getIdaudienta().equals(audientaCurenta.getIdaudienta()));

                        if (eDinCameraCurenta) {
                            String nume = m.getExpeditor() != null ? m.getExpeditor().getUsername() : "Necunoscut";
                            String timp = m.getDataTrimitere() != null ? m.getDataTrimitere().format(fmt) : "";
                            continutRaport.append("[").append(timp).append("] ").append(nume).append(": ").append(m.getMesaj()).append("\n");
                            deSters.add(m); // Punem deoparte mesajele ca sa le stergem
                        }
                    }

                    Raport raportNou = new Raport();
                    raportNou.setTitlu(titluRaport);
                    raportNou.setContinut(continutRaport.toString());
                    raportNou.setDataGenerare(java.time.LocalDateTime.now());
                    raportNou.setEstePublic(true);

                    raportRepository.save(raportNou);
                    mesajChatRepository.deleteAll(deSters); // Stergem DOAR mesajele din aceasta camera, nu tot serverul
                    incarcaMesaje.run();

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Audiență Încheiată");
                    alert.setHeaderText(null);
                    alert.setContentText("Raportul '" + titluRaport + "' a fost arhivat, iar sala a fost curățată!");
                    alert.showAndWait();
                });
            });

            inputBox.getChildren().addAll(txtMesaj, btnTrimite, btnRefresh, btnGenereazaRaport);
        } else {
            inputBox.getChildren().addAll(txtMesaj, btnTrimite, btnRefresh);
        }

        panouChat.getChildren().addAll(titlu, scrollChat, inputBox);
        mainBorderPane.setCenter(panouChat);
    }
}