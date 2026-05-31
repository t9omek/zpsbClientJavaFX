package com.example.app;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class MainController {

    private final ApiClient apiClient = new ApiClient();
    private final List<EntityConfig> entities = createEntityConfigs();
    private final Map<EntityConfig, TableView<Map<String, Object>>> tables = new HashMap<>();
    private final Map<EntityConfig, Map<String, TextField>> forms = new HashMap<>();

    private ComboBox<Map<String, Object>> saleKlientCombo;
    private ComboBox<Map<String, Object>> salePracownikCombo;
    private ComboBox<Map<String, Object>> saleStatusCombo;
    private ComboBox<Map<String, Object>> saleFormaPlatnosciCombo;
    private ComboBox<Map<String, Object>> saleFormaDostawyCombo;
    private ComboBox<Map<String, Object>> saleFirmaDostawczaCombo;
    private ComboBox<Map<String, Object>> saleProduktCombo;
    private TextField saleIloscField;
    private TableView<CartItem> saleCartTable;
    private Label saleTotalLabel;
    private final ObservableList<CartItem> saleCartItems = FXCollections.observableArrayList();
    private final List<Map<String, Object>> saleProduktMagazynRows = new ArrayList<>();

    private TableView<SalesRow> salesTable;
    private TableView<SalesDetailRow> salesDetailsTable;
    private Label salesSummaryLabel;
    private final ObservableList<SalesRow> salesRows = FXCollections.observableArrayList();
    private final ObservableList<SalesDetailRow> allSalesDetails = FXCollections.observableArrayList();

    @FXML
    private ListView<String> navigationList;

    @FXML
    private StackPane contentPane;

    private final Map<String, Node> pages = new LinkedHashMap<>();

    @FXML
    private Label statusLabel;

    @FXML
    private Label apiInfoLabel;

    @FXML
    private void initialize() {
        apiInfoLabel.setText("API: " + apiClient.getBaseUrl());
        buildNavigation();
        entities.forEach(this::loadEntity);
        refreshSaleDictionaries();
        loadSalesOverview();
    }

    private void buildNavigation() {
        pages.clear();
        pages.put("Sprzedaż", createSaleView());
        pages.put("Sprzedaże", createSalesOverviewView());

        for (EntityConfig entity : entities) {
            pages.put(entity.name(), createEntityView(entity));
        }

        navigationList.setItems(FXCollections.observableArrayList(pages.keySet()));
        navigationList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> showPage(newValue));

        if (!navigationList.getItems().isEmpty()) {
            navigationList.getSelectionModel().selectFirst();
        }
    }

    private void showPage(String pageName) {
        if (pageName == null) {
            return;
        }

        Node page = pages.get(pageName);
        if (page != null) {
            contentPane.getChildren().setAll(page);
            setStatus("Widok: " + pageName);
        }
    }


    private VBox createSaleView() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(12));

        Label title = new Label("Nowa sprzedaż / zamówienie");
        title.getStyleClass().add("section-title");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);

        saleKlientCombo = createLookupCombo("id_klienta", "imie", "nazwisko");
        salePracownikCombo = createLookupCombo("id_pracownika", "imie", "nazwisko");
        saleStatusCombo = createLookupCombo("id_statusu", "nazwa", null);
        saleFormaPlatnosciCombo = createLookupCombo("id_formy_platnosci", "nazwa", null);
        saleFormaDostawyCombo = createLookupCombo("id_formy_dostawy", "nazwa", null);
        saleFirmaDostawczaCombo = createLookupCombo("id_firmy", "nazwa", null);
        saleProduktCombo = createLookupCombo("id_produktu", "nazwa", "cena");
        saleIloscField = new TextField("1");
        saleIloscField.setPromptText("Ilość");

        addLabeledControl(form, 0, 0, "Klient *", saleKlientCombo);
        addLabeledControl(form, 1, 0, "Pracownik *", salePracownikCombo);
        addLabeledControl(form, 2, 0, "Status *", saleStatusCombo);
        addLabeledControl(form, 0, 1, "Forma płatności *", saleFormaPlatnosciCombo);
        addLabeledControl(form, 1, 1, "Forma dostawy *", saleFormaDostawyCombo);
        addLabeledControl(form, 2, 1, "Firma dostawcza *", saleFirmaDostawczaCombo);
        addLabeledControl(form, 0, 2, "Produkt *", saleProduktCombo);
        addLabeledControl(form, 1, 2, "Ilość *", saleIloscField);

        Button refreshButton = new Button("Odśwież dane");
        Button addToCartButton = new Button("Dodaj produkt do koszyka");
        Button removeFromCartButton = new Button("Usuń pozycję z koszyka");
        Button clearCartButton = new Button("Wyczyść koszyk");
        Button submitSaleButton = new Button("Zatwierdź sprzedaż");

        refreshButton.setOnAction(event -> refreshSaleDictionaries());
        addToCartButton.setOnAction(event -> addSelectedProductToCart());
        removeFromCartButton.setOnAction(event -> removeSelectedCartItem());
        clearCartButton.setOnAction(event -> clearSaleCart());
        submitSaleButton.setOnAction(event -> submitSale());

        HBox productButtons = new HBox(10, refreshButton, addToCartButton, removeFromCartButton, clearCartButton, submitSaleButton);

        saleCartTable = new TableView<>();
        saleCartTable.setPrefHeight(260);
        saleCartTable.setItems(saleCartItems);
        saleCartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<CartItem, String> productColumn = new TableColumn<>("Produkt");
        productColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getProduktLabel()));

        TableColumn<CartItem, String> quantityColumn = new TableColumn<>("Ilość");
        quantityColumn.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getIlosc())));

        TableColumn<CartItem, String> priceColumn = new TableColumn<>("Cena");
        priceColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCenaZakupu().toPlainString()));

        TableColumn<CartItem, String> valueColumn = new TableColumn<>("Wartość");
        valueColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getWartosc().toPlainString()));

        saleCartTable.getColumns().addAll(productColumn, quantityColumn, priceColumn, valueColumn);

        saleTotalLabel = new Label("Suma: 0.00");
        saleTotalLabel.getStyleClass().add("section-title");

        Label hint = new Label("Flow bez zmian w backendzie: aplikacja kolejno wywołuje POST /add_dostawa, POST /zamowienia i POST /pozycje_zamowienia dla każdej pozycji koszyka.");
        hint.getStyleClass().add("api-info");

        VBox.setVgrow(saleCartTable, Priority.ALWAYS);
        root.getChildren().addAll(title, form, productButtons, saleCartTable, saleTotalLabel, hint);
        return root;
    }

    private ComboBox<Map<String, Object>> createLookupCombo(String idKey, String labelKey, String optionalSecondKey) {
        ComboBox<Map<String, Object>> combo = new ComboBox<>();
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Map<String, Object> item) {
                if (item == null) {
                    return "";
                }
                return formatLookup(item, idKey, labelKey, optionalSecondKey);
            }

            @Override
            public Map<String, Object> fromString(String string) {
                return null;
            }
        });
        return combo;
    }

    private String formatLookup(Map<String, Object> item, String idKey, String labelKey, String optionalSecondKey) {
        Object id = item.get(idKey);
        Object label = item.get(labelKey);
        String value = label == null ? "" : String.valueOf(label);

        if ("imie".equals(labelKey)) {
            Object nazwisko = item.get(optionalSecondKey);
            value = ((label == null ? "" : label) + " " + (nazwisko == null ? "" : nazwisko)).trim();
        } else if (optionalSecondKey != null && item.get(optionalSecondKey) != null && !"nazwisko".equals(optionalSecondKey)) {
            value += " (" + item.get(optionalSecondKey) + ")";
        }

        return "#" + id + " - " + value;
    }

    private void addLabeledControl(GridPane grid, int col, int row, String label, Control control) {
        VBox wrapper = new VBox(4);
        wrapper.getChildren().addAll(new Label(label), control);
        GridPane.setHgrow(wrapper, Priority.ALWAYS);
        grid.add(wrapper, col, row);
    }

    private void refreshSaleDictionaries() {
        setStatus("Odświeżam dane do sprzedaży...");
        runInBackground(
                () -> {
                    Map<String, List<Map<String, Object>>> data = new HashMap<>();
                    data.put("klienci", apiClient.getList("/klienci"));
                    data.put("pracownicy", apiClient.getList("/pracownicy"));
                    data.put("statusy", apiClient.getList("/statusy"));
                    data.put("formyPlatnosci", apiClient.getList("/formy_platnosci"));
                    data.put("formyDostawy", apiClient.getList("/formyDostawy"));
                    data.put("firmyDostawcze", apiClient.getList("/firmyDostawcze"));
                    data.put("produkty", apiClient.getList("/produkty"));
                    data.put("produktMagazyn", apiClient.getList("/produktMagazyn"));
                    return data;
                },
                data -> {
                    setComboItems(saleKlientCombo, data.get("klienci"));
                    setComboItems(salePracownikCombo, data.get("pracownicy"));
                    setComboItems(saleStatusCombo, data.get("statusy"));
                    setComboItems(saleFormaPlatnosciCombo, data.get("formyPlatnosci"));
                    setComboItems(saleFormaDostawyCombo, data.get("formyDostawy"));
                    setComboItems(saleFirmaDostawczaCombo, data.get("firmyDostawcze"));
                    setComboItems(saleProduktCombo, data.get("produkty"));

                    saleProduktMagazynRows.clear();
                    saleProduktMagazynRows.addAll(data.getOrDefault("produktMagazyn", List.of()));

                    setStatus("Dane do sprzedaży odświeżone. Stany magazynowe pobrane.");
                }
        );
    }

    private void setComboItems(ComboBox<Map<String, Object>> combo, List<Map<String, Object>> rows) {
        if (combo == null) {
            return;
        }
        Object previouslySelectedId = null;
        Map<String, Object> selected = combo.getSelectionModel().getSelectedItem();
        if (selected != null) {
            previouslySelectedId = findFirstIdValue(selected);
        }

        combo.setItems(FXCollections.observableArrayList(rows == null ? List.of() : rows));

        if (previouslySelectedId != null) {
            for (Map<String, Object> row : combo.getItems()) {
                if (String.valueOf(previouslySelectedId).equals(String.valueOf(findFirstIdValue(row)))) {
                    combo.getSelectionModel().select(row);
                    return;
                }
            }
        }

        if (!combo.getItems().isEmpty() && combo.getSelectionModel().isEmpty()) {
            combo.getSelectionModel().selectFirst();
        }
    }

    private Object findFirstIdValue(Map<String, Object> row) {
        for (String key : row.keySet()) {
            if (key.startsWith("id")) {
                return row.get(key);
            }
        }
        return null;
    }

    private void addSelectedProductToCart() {
        Map<String, Object> product = saleProduktCombo.getSelectionModel().getSelectedItem();
        if (product == null) {
            setStatus("Wybierz produkt.");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(saleIloscField.getText().trim());
        } catch (Exception e) {
            setStatus("Ilość musi być liczbą całkowitą.");
            return;
        }

        if (quantity <= 0) {
            setStatus("Ilość musi być większa od zera.");
            return;
        }

        Object priceValue = product.get("cena");
        BigDecimal price;
        try {
            price = new BigDecimal(String.valueOf(priceValue).replace(',', '.'));
        } catch (Exception e) {
            setStatus("Produkt nie ma poprawnej ceny.");
            return;
        }

        int productId = toInt(product.get("id_produktu"));
        String productName = String.valueOf(product.getOrDefault("nazwa", "Produkt #" + productId));

        int availableStock = getAvailableStock(productId, saleProduktMagazynRows);
        int quantityAlreadyInCart = getQuantityInCart(productId);

        if (availableStock <= 0) {
            setStatus("Produkt niedostępny w magazynie: " + productName);
            return;
        }

        if (quantityAlreadyInCart + quantity > availableStock) {
            setStatus(
                    "Nie można dodać produktu. Dostępne: "
                            + availableStock
                            + ", w koszyku: "
                            + quantityAlreadyInCart
                            + ", próba dodania: "
                            + quantity
            );
            return;
        }

        for (CartItem item : saleCartItems) {
            if (item.getIdProduktu() == productId && item.getCenaZakupu().compareTo(price) == 0) {
                item.setIlosc(item.getIlosc() + quantity);
                saleCartTable.refresh();
                updateSaleTotal();
                setStatus("Zwiększono ilość produktu w koszyku: " + productName);
                return;
            }
        }

        saleCartItems.add(new CartItem(productId, productName, quantity, price));
        updateSaleTotal();
        setStatus("Dodano produkt do koszyka: " + productName);
    }

    private void removeSelectedCartItem() {
        CartItem selected = saleCartTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("Najpierw zaznacz pozycję koszyka.");
            return;
        }
        saleCartItems.remove(selected);
        updateSaleTotal();
        setStatus("Usunięto pozycję z koszyka.");
    }

    private void clearSaleCart() {
        saleCartItems.clear();
        updateSaleTotal();
        setStatus("Koszyk wyczyszczony.");
    }

    private void updateSaleTotal() {
        BigDecimal total = saleCartItems.stream()
                .map(CartItem::getWartosc)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (saleTotalLabel != null) {
            saleTotalLabel.setText("Suma: " + total.toPlainString());
        }
    }

    private void submitSale() {
        if (saleCartItems.isEmpty()) {
            setStatus("Dodaj przynajmniej jeden produkt do koszyka.");
            return;
        }

        Map<String, Object> klient = requireSelected(saleKlientCombo, "klienta");
        Map<String, Object> pracownik = requireSelected(salePracownikCombo, "pracownika");
        Map<String, Object> status = requireSelected(saleStatusCombo, "status");
        Map<String, Object> formaPlatnosci = requireSelected(saleFormaPlatnosciCombo, "formę płatności");
        Map<String, Object> formaDostawy = requireSelected(saleFormaDostawyCombo, "formę dostawy");
        Map<String, Object> firma = requireSelected(saleFirmaDostawczaCombo, "firmę dostawczą");

        if (klient == null || pracownik == null || status == null || formaPlatnosci == null || formaDostawy == null || firma == null) {
            return;
        }

        setStatus("Tworzę sprzedaż...");
        runInBackground(
                () -> createSaleRequest(klient, pracownik, status, formaPlatnosci, formaDostawy, firma),
                result -> {
                    saleCartItems.clear();
                    updateSaleTotal();
                    refreshCrudTablesAfterSale();
                    loadSalesOverview();
                    setStatus(result);
                }
        );
    }

    private Map<String, Object> requireSelected(ComboBox<Map<String, Object>> combo, String label) {
        Map<String, Object> selected = combo.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("Wybierz " + label + ".");
        }
        return selected;
    }

    private String createSaleRequest(
            Map<String, Object> klient,
            Map<String, Object> pracownik,
            Map<String, Object> status,
            Map<String, Object> formaPlatnosci,
            Map<String, Object> formaDostawy,
            Map<String, Object> firma
    ) throws Exception {
        Integer createdDostawaId = null;
        Integer createdZamowienieId = null;

        List<Map<String, Object>> currentStockRows = apiClient.getList("/produktMagazyn");
        validateCartAgainstStock(currentStockRows);

        try {
            Map<String, Object> dostawaPayload = new LinkedHashMap<>();
            dostawaPayload.put("id_formy_dostawy", toInt(formaDostawy.get("id_formy_dostawy")));
            dostawaPayload.put("id_firmy", toInt(firma.get("id_firmy")));
            Map<String, Object> createdDostawa = apiClient.create("/add_dostawa", dostawaPayload);
            createdDostawaId = toInt(createdDostawa.get("id_dostawy"));

            Map<String, Object> orderPayload = new LinkedHashMap<>();
            orderPayload.put("data", LocalDate.now().toString());
            orderPayload.put("id_statusu", toInt(status.get("id_statusu")));
            orderPayload.put("id_pracownika", toInt(pracownik.get("id_pracownika")));
            orderPayload.put("id_klienta", toInt(klient.get("id_klienta")));
            orderPayload.put("id_formy_platnosci", toInt(formaPlatnosci.get("id_formy_platnosci")));
            orderPayload.put("id_dostawy", createdDostawaId);
            Map<String, Object> createdOrder = apiClient.create("/zamowienia", orderPayload);
            createdZamowienieId = toInt(createdOrder.get("id_zamowienia"));

            for (CartItem item : saleCartItems) {
                Map<String, Object> positionPayload = new LinkedHashMap<>();
                positionPayload.put("id_zamowienia", createdZamowienieId);
                positionPayload.put("id_produktu", item.getIdProduktu());
                positionPayload.put("ilosc", item.getIlosc());
                positionPayload.put("cena_zakupu", item.getCenaZakupu());
                apiClient.create("/pozycje_zamowienia", positionPayload);
            }

            decreaseStockAfterSale(currentStockRows);

            BigDecimal total = saleCartItems.stream()
                    .map(CartItem::getWartosc)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            saleProduktMagazynRows.clear();
            saleProduktMagazynRows.addAll(apiClient.getList("/produktMagazyn"));

            return "Sprzedaż zapisana. ID zamówienia: " + createdZamowienieId + ", suma: " + total.toPlainString();
        } catch (Exception e) {
            if (createdZamowienieId != null) {
                try {
                    apiClient.delete("/zamowienia/" + createdZamowienieId);
                } catch (Exception ignored) {
                    // Próba sprzątania po błędzie. Backend nie obsługuje jednej transakcji dla całej sprzedaży.
                }
            }
            if (createdDostawaId != null) {
                try {
                    apiClient.delete("/del_dostawa/" + createdDostawaId);
                } catch (Exception ignored) {
                    // Dostawa może być już powiązana z zamówieniem albo usunięta przez wcześniejsze sprzątanie.
                }
            }
            throw e;
        }
    }

    private void validateCartAgainstStock(List<Map<String, Object>> stockRows) {
        for (CartItem item : saleCartItems) {
            int availableStock = getAvailableStock(item.getIdProduktu(), stockRows);
            int requestedQuantity = getQuantityInCart(item.getIdProduktu());

            if (requestedQuantity > availableStock) {
                throw new IllegalArgumentException(
                        "Brak wystarczającej ilości produktu: "
                                + item.getProduktLabel()
                                + ". Dostępne: "
                                + availableStock
                                + ", wymagane: "
                                + requestedQuantity
                );
            }
        }
    }

    private int getAvailableStock(int productId, List<Map<String, Object>> stockRows) {
        if (stockRows == null) {
            return 0;
        }

        int total = 0;
        for (Map<String, Object> row : stockRows) {
            Object rowProductId = row.get("id_produktu");
            Object quantity = row.get("ilosc");

            if (rowProductId != null && quantity != null && toInt(rowProductId) == productId) {
                total += toInt(quantity);
            }
        }
        return total;
    }

    private int getQuantityInCart(int productId) {
        int total = 0;
        for (CartItem item : saleCartItems) {
            if (item.getIdProduktu() == productId) {
                total += item.getIlosc();
            }
        }
        return total;
    }

    private void decreaseStockAfterSale(List<Map<String, Object>> stockRows) throws Exception {
        Map<Integer, Integer> quantitiesToDecrease = new LinkedHashMap<>();

        for (CartItem item : saleCartItems) {
            quantitiesToDecrease.merge(item.getIdProduktu(), item.getIlosc(), Integer::sum);
        }

        for (Map.Entry<Integer, Integer> entry : quantitiesToDecrease.entrySet()) {
            int productId = entry.getKey();
            int remainingToDecrease = entry.getValue();

            for (Map<String, Object> stockRow : stockRows) {
                if (remainingToDecrease <= 0) {
                    break;
                }

                if (toInt(stockRow.get("id_produktu")) != productId) {
                    continue;
                }

                int stockRowId = toInt(stockRow.get("id"));
                int currentQuantity = toInt(stockRow.get("ilosc"));
                int usedQuantity = Math.min(currentQuantity, remainingToDecrease);
                int newQuantity = currentQuantity - usedQuantity;

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("id_produktu", toInt(stockRow.get("id_produktu")));
                payload.put("id_magazynu", toInt(stockRow.get("id_magazynu")));
                payload.put("ilosc", newQuantity);

                apiClient.update("/produktMagazyn/" + stockRowId, payload);

                remainingToDecrease -= usedQuantity;
            }

            if (remainingToDecrease > 0) {
                throw new IllegalStateException(
                        "Nie udało się zaktualizować stanu magazynowego produktu #" + productId
                );
            }
        }
    }

    private int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private void refreshCrudTablesAfterSale() {
        for (EntityConfig entity : entities) {
            if ("Dostawy".equals(entity.name())
                    || "Zamówienia".equals(entity.name())
                    || "Pozycje zamówienia".equals(entity.name())
                    || "Produkt-magazyn".equals(entity.name())) {
                loadEntity(entity);
            }
        }
    }


    private VBox createSalesOverviewView() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(12));

        Label title = new Label("Sprzedaże i statusy zamówień");
        title.getStyleClass().add("section-title");

        Button refreshButton = new Button("Odśwież sprzedaże");
        refreshButton.setOnAction(event -> loadSalesOverview());

        salesTable = new TableView<>();
        salesTable.setPrefHeight(280);
        salesTable.setItems(salesRows);
        salesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        addSalesColumn(salesTable, "ID", SalesRow::getIdZamowienia, 70);
        addSalesColumn(salesTable, "Data", SalesRow::getData, 100);
        addSalesColumn(salesTable, "Klient", SalesRow::getKlient, 170);
        addSalesColumn(salesTable, "Pracownik", SalesRow::getPracownik, 170);
        addSalesColumn(salesTable, "Status", SalesRow::getStatus, 120);
        addSalesColumn(salesTable, "Płatność", SalesRow::getFormaPlatnosci, 130);
        addSalesColumn(salesTable, "Dostawa", SalesRow::getDostawa, 120);
        addSalesColumn(salesTable, "Wartość", SalesRow::getWartosc, 100);

        Label detailsTitle = new Label("Pozycje zaznaczonej sprzedaży");
        detailsTitle.getStyleClass().add("section-title");

        salesDetailsTable = new TableView<>();
        salesDetailsTable.setPrefHeight(220);
        salesDetailsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        addSalesDetailColumn(salesDetailsTable, "Produkt", SalesDetailRow::getProdukt, 220);
        addSalesDetailColumn(salesDetailsTable, "Ilość", SalesDetailRow::getIlosc, 80);
        addSalesDetailColumn(salesDetailsTable, "Cena", SalesDetailRow::getCenaZakupu, 100);
        addSalesDetailColumn(salesDetailsTable, "Wartość", SalesDetailRow::getWartosc, 100);

        salesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, newRow) -> showSalesDetails(newRow));

        salesSummaryLabel = new Label("Brak danych sprzedaży.");
        salesSummaryLabel.getStyleClass().add("api-info");

        VBox.setVgrow(salesTable, Priority.ALWAYS);
        VBox.setVgrow(salesDetailsTable, Priority.ALWAYS);
        root.getChildren().addAll(title, refreshButton, salesTable, detailsTitle, salesDetailsTable, salesSummaryLabel);
        return root;
    }

    private void addSalesColumn(TableView<SalesRow> table, String title, java.util.function.Function<SalesRow, String> getter, double width) {
        TableColumn<SalesRow, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new SimpleStringProperty(getter.apply(cell.getValue())));
        table.getColumns().add(column);
    }

    private void addSalesDetailColumn(TableView<SalesDetailRow> table, String title, java.util.function.Function<SalesDetailRow, String> getter, double width) {
        TableColumn<SalesDetailRow, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new SimpleStringProperty(getter.apply(cell.getValue())));
        table.getColumns().add(column);
    }

    private void loadSalesOverview() {
        if (salesTable == null) {
            return;
        }

        setStatus("Pobieram widok sprzedaży...");
        runInBackground(
                () -> {
                    Map<String, List<Map<String, Object>>> data = new HashMap<>();
                    data.put("zamowienia", apiClient.getList("/zamowienia"));
                    data.put("pozycje", apiClient.getList("/pozycje_zamowienia"));
                    data.put("klienci", apiClient.getList("/klienci"));
                    data.put("pracownicy", apiClient.getList("/pracownicy"));
                    data.put("statusy", apiClient.getList("/statusy"));
                    data.put("formyPlatnosci", apiClient.getList("/formy_platnosci"));
                    data.put("dostawy", apiClient.getList("/dostawy"));
                    data.put("produkty", apiClient.getList("/produkty"));
                    return buildSalesOverviewRows(data);
                },
                overview -> {
                    salesRows.setAll(overview.rows());
                    allSalesDetails.setAll(overview.details());
                    if (!salesRows.isEmpty()) {
                        salesTable.getSelectionModel().selectFirst();
                    } else {
                        salesDetailsTable.setItems(FXCollections.observableArrayList());
                    }
                    salesSummaryLabel.setText("Sprzedaże: " + salesRows.size() + ", łączna wartość: " + overview.total().toPlainString());
                    setStatus("Odświeżono widok sprzedaży.");
                }
        );
    }

    private SalesOverview buildSalesOverviewRows(Map<String, List<Map<String, Object>>> data) {
        List<Map<String, Object>> orders = data.getOrDefault("zamowienia", List.of());
        List<Map<String, Object>> positions = data.getOrDefault("pozycje", List.of());

        Map<Integer, Map<String, Object>> clientsById = mapById(data.get("klienci"), "id_klienta");
        Map<Integer, Map<String, Object>> employeesById = mapById(data.get("pracownicy"), "id_pracownika");
        Map<Integer, Map<String, Object>> statusesById = mapById(data.get("statusy"), "id_statusu");
        Map<Integer, Map<String, Object>> paymentsById = mapById(data.get("formyPlatnosci"), "id_formy_platnosci");
        Map<Integer, Map<String, Object>> deliveriesById = mapById(data.get("dostawy"), "id_dostawy");
        Map<Integer, Map<String, Object>> productsById = mapById(data.get("produkty"), "id_produktu");

        Map<Integer, List<Map<String, Object>>> positionsByOrderId = new HashMap<>();
        for (Map<String, Object> position : positions) {
            int orderId = toInt(position.get("id_zamowienia"));
            positionsByOrderId.computeIfAbsent(orderId, ignored -> new ArrayList<>()).add(position);
        }

        List<SalesRow> rows = new ArrayList<>();
        List<SalesDetailRow> details = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Map<String, Object> order : orders) {
            int orderId = toInt(order.get("id_zamowienia"));
            BigDecimal orderTotal = BigDecimal.ZERO;
            List<Map<String, Object>> orderPositions = positionsByOrderId.getOrDefault(orderId, List.of());

            for (Map<String, Object> position : orderPositions) {
                int productId = toInt(position.get("id_produktu"));
                int quantity = toInt(position.get("ilosc"));
                BigDecimal price = toBigDecimal(position.get("cena_zakupu"));
                BigDecimal value = price.multiply(BigDecimal.valueOf(quantity));
                orderTotal = orderTotal.add(value);

                Map<String, Object> product = productsById.get(productId);
                String productLabel = product == null
                        ? "Produkt #" + productId
                        : String.valueOf(product.getOrDefault("nazwa", "Produkt #" + productId));

                details.add(new SalesDetailRow(
                        String.valueOf(orderId),
                        productLabel,
                        String.valueOf(quantity),
                        price.toPlainString(),
                        value.toPlainString()
                ));
            }

            grandTotal = grandTotal.add(orderTotal);

            int clientId = toInt(order.get("id_klienta"));
            int employeeId = toInt(order.get("id_pracownika"));
            int statusId = toInt(order.get("id_statusu"));
            int paymentId = toInt(order.get("id_formy_platnosci"));
            int deliveryId = toInt(order.get("id_dostawy"));

            rows.add(new SalesRow(
                    String.valueOf(orderId),
                    stringValue(order.get("data")),
                    personLabel(clientsById.get(clientId), "Klient #" + clientId),
                    personLabel(employeesById.get(employeeId), "Pracownik #" + employeeId),
                    nameLabel(statusesById.get(statusId), "Status #" + statusId),
                    nameLabel(paymentsById.get(paymentId), "Płatność #" + paymentId),
                    deliveryLabel(deliveriesById.get(deliveryId), deliveryId),
                    orderTotal.toPlainString()
            ));
        }

        rows.sort((a, b) -> Integer.compare(Integer.parseInt(b.getIdZamowienia()), Integer.parseInt(a.getIdZamowienia())));
        return new SalesOverview(rows, details, grandTotal);
    }

    private Map<Integer, Map<String, Object>> mapById(List<Map<String, Object>> rows, String idKey) {
        Map<Integer, Map<String, Object>> result = new HashMap<>();
        if (rows == null) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            if (row.get(idKey) != null) {
                result.put(toInt(row.get(idKey)), row);
            }
        }
        return result;
    }

    private String personLabel(Map<String, Object> row, String fallback) {
        if (row == null) {
            return fallback;
        }
        String firstName = stringValue(row.get("imie"));
        String lastName = stringValue(row.get("nazwisko"));
        String combined = (firstName + " " + lastName).trim();
        return combined.isBlank() ? fallback : combined;
    }

    private String nameLabel(Map<String, Object> row, String fallback) {
        if (row == null) {
            return fallback;
        }
        String name = stringValue(row.get("nazwa"));
        return name.isBlank() ? fallback : name;
    }

    private String deliveryLabel(Map<String, Object> row, int deliveryId) {
        if (row == null) {
            return "Dostawa #" + deliveryId;
        }
        Object formId = row.get("id_formy_dostawy");
        Object companyId = row.get("id_firmy");
        return "#" + deliveryId + " (forma " + formId + ", firma " + companyId + ")";
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(value).replace(',', '.'));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private void showSalesDetails(SalesRow row) {
        if (salesDetailsTable == null) {
            return;
        }
        if (row == null) {
            salesDetailsTable.setItems(FXCollections.observableArrayList());
            return;
        }
        List<SalesDetailRow> filtered = allSalesDetails.stream()
                .filter(detail -> detail.getIdZamowienia().equals(row.getIdZamowienia()))
                .toList();
        salesDetailsTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private VBox createEntityView(EntityConfig entity) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(12));

        TableView<Map<String, Object>> table = new TableView<>();
        table.setPrefHeight(330);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tables.put(entity, table);

        for (FieldConfig column : entity.columns()) {
            TableColumn<Map<String, Object>, String> tableColumn = new TableColumn<>(column.label());
            tableColumn.setPrefWidth(130);
            tableColumn.setCellValueFactory(cellData -> {
                Object value = cellData.getValue().get(column.key());
                return new SimpleStringProperty(value == null ? "" : String.valueOf(value));
            });
            table.getColumns().add(tableColumn);
        }

        HBox buttons = new HBox(10);
        Button refreshButton = new Button("Odśwież");
        Button addButton = new Button("Dodaj");
        Button updateButton = new Button("Zapisz zmiany zaznaczonego");
        Button deleteButton = new Button("Usuń zaznaczone");

        refreshButton.setOnAction(event -> loadEntity(entity));
        addButton.setOnAction(event -> addEntity(entity));
        updateButton.setOnAction(event -> updateSelectedEntity(entity));
        deleteButton.setOnAction(event -> deleteSelectedEntity(entity));
        buttons.getChildren().addAll(refreshButton, addButton, updateButton, deleteButton);

        Label formTitle = new Label("Formularz: " + entity.name());
        formTitle.getStyleClass().add("section-title");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        Map<String, TextField> fieldMap = new LinkedHashMap<>();
        forms.put(entity, fieldMap);

        int row = 0;
        int col = 0;
        for (FieldConfig field : entity.formFields()) {
            TextField input = new TextField();
            input.setPromptText(field.label() + promptSuffix(field));
            fieldMap.put(field.key(), input);

            VBox wrapper = new VBox(4);
            Label label = new Label(field.label() + (field.required() ? " *" : ""));
            wrapper.getChildren().addAll(label, input);

            form.add(wrapper, col, row);
            col++;
            if (col >= 3) {
                col = 0;
                row++;
            }
        }

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                fillForm(entity, newValue);
            }
        });

        VBox.setVgrow(table, Priority.ALWAYS);
        root.getChildren().addAll(buttons, table, formTitle, form);
        return root;
    }

    private String promptSuffix(FieldConfig field) {
        return switch (field.type()) {
            case INTEGER -> " (liczba)";
            case DECIMAL -> " (np. 19.99)";
            case DATE -> " (YYYY-MM-DD)";
            case TEXT -> "";
        };
    }

    private void fillForm(EntityConfig entity, Map<String, Object> row) {
        Map<String, TextField> fields = forms.get(entity);
        for (FieldConfig field : entity.formFields()) {
            Object value = row.get(field.key());
            TextField input = fields.get(field.key());
            input.setText(value == null ? "" : String.valueOf(value));
        }
    }

    private void clearForm(EntityConfig entity) {
        forms.get(entity).values().forEach(TextInputControl::clear);
    }

    private void loadEntity(EntityConfig entity) {
        setStatus("Pobieram dane: " + entity.name() + "...");
        runInBackground(
                () -> apiClient.getList(entity.listPath()),
                rows -> {
                    tables.get(entity).setItems(FXCollections.observableArrayList(rows));
                    setStatus("Pobrano: " + entity.name() + " (" + rows.size() + ")");
                }
        );
    }

    private void addEntity(EntityConfig entity) {
        Map<String, Object> payload;
        try {
            payload = readForm(entity);
        } catch (IllegalArgumentException e) {
            setStatus(e.getMessage());
            return;
        }

        setStatus("Dodaję: " + entity.name() + "...");
        runInBackground(
                () -> apiClient.create(entity.createPath(), payload),
                created -> {
                    clearForm(entity);
                    loadEntity(entity);
                    setStatus("Dodano rekord w: " + entity.name());
                }
        );
    }

    private void updateSelectedEntity(EntityConfig entity) {
        Map<String, Object> selected = tables.get(entity).getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("Najpierw zaznacz rekord do edycji: " + entity.name());
            return;
        }

        Object id = selected.get(entity.idField());
        if (id == null) {
            setStatus("Nie można odczytać ID zaznaczonego rekordu.");
            return;
        }

        Map<String, Object> payload;
        try {
            payload = readForm(entity);
        } catch (IllegalArgumentException e) {
            setStatus(e.getMessage());
            return;
        }

        setStatus("Aktualizuję rekord: " + entity.name() + "...");
        runInBackground(
                () -> apiClient.update(entity.updatePath(id), payload),
                updated -> {
                    loadEntity(entity);
                    setStatus("Zaktualizowano rekord w: " + entity.name());
                }
        );
    }

    private void deleteSelectedEntity(EntityConfig entity) {
        Map<String, Object> selected = tables.get(entity).getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("Najpierw zaznacz rekord do usunięcia: " + entity.name());
            return;
        }

        Object id = selected.get(entity.idField());
        if (id == null) {
            setStatus("Nie można odczytać ID zaznaczonego rekordu.");
            return;
        }

        setStatus("Usuwam rekord: " + entity.name() + "...");
        runInBackground(
                () -> {
                    apiClient.delete(entity.deletePath(id));
                    return null;
                },
                ignored -> {
                    clearForm(entity);
                    loadEntity(entity);
                    setStatus("Usunięto rekord z: " + entity.name());
                }
        );
    }

    private Map<String, Object> readForm(EntityConfig entity) {
        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, TextField> fields = forms.get(entity);

        for (FieldConfig field : entity.formFields()) {
            String raw = fields.get(field.key()).getText();
            String value = raw == null ? "" : raw.trim();

            if (value.isEmpty()) {
                if (field.required()) {
                    throw new IllegalArgumentException("Pole wymagane: " + field.label());
                }
                payload.put(field.key(), null);
                continue;
            }

            payload.put(field.key(), parseValue(field, value));
        }

        return payload;
    }

    private Object parseValue(FieldConfig field, String value) {
        try {
            return switch (field.type()) {
                case TEXT -> value;
                case INTEGER -> Integer.parseInt(value);
                case DECIMAL -> new BigDecimal(value.replace(',', '.'));
                case DATE -> LocalDate.parse(value).toString();
            };
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Nieprawidłowa liczba w polu: " + field.label());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Nieprawidłowa data w polu: " + field.label() + ". Użyj formatu YYYY-MM-DD.");
        }
    }

    private <T> void runInBackground(Callable<T> action, Consumer<T> onSuccess) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return action.call();
            }
        };

        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            setStatus("Błąd: " + (error == null ? "nieznany" : error.getMessage()));
            if (error != null) {
                error.printStackTrace();
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void setStatus(String message) {
        if (Platform.isFxApplicationThread()) {
            statusLabel.setText(message);
        } else {
            Platform.runLater(() -> statusLabel.setText(message));
        }
    }



    public record SalesOverview(List<SalesRow> rows, List<SalesDetailRow> details, BigDecimal total) {
    }

    public static class SalesRow {
        private final String idZamowienia;
        private final String data;
        private final String klient;
        private final String pracownik;
        private final String status;
        private final String formaPlatnosci;
        private final String dostawa;
        private final String wartosc;

        public SalesRow(String idZamowienia, String data, String klient, String pracownik, String status, String formaPlatnosci, String dostawa, String wartosc) {
            this.idZamowienia = idZamowienia;
            this.data = data;
            this.klient = klient;
            this.pracownik = pracownik;
            this.status = status;
            this.formaPlatnosci = formaPlatnosci;
            this.dostawa = dostawa;
            this.wartosc = wartosc;
        }

        public String getIdZamowienia() { return idZamowienia; }
        public String getData() { return data; }
        public String getKlient() { return klient; }
        public String getPracownik() { return pracownik; }
        public String getStatus() { return status; }
        public String getFormaPlatnosci() { return formaPlatnosci; }
        public String getDostawa() { return dostawa; }
        public String getWartosc() { return wartosc; }
    }

    public static class SalesDetailRow {
        private final String idZamowienia;
        private final String produkt;
        private final String ilosc;
        private final String cenaZakupu;
        private final String wartosc;

        public SalesDetailRow(String idZamowienia, String produkt, String ilosc, String cenaZakupu, String wartosc) {
            this.idZamowienia = idZamowienia;
            this.produkt = produkt;
            this.ilosc = ilosc;
            this.cenaZakupu = cenaZakupu;
            this.wartosc = wartosc;
        }

        public String getIdZamowienia() { return idZamowienia; }
        public String getProdukt() { return produkt; }
        public String getIlosc() { return ilosc; }
        public String getCenaZakupu() { return cenaZakupu; }
        public String getWartosc() { return wartosc; }
    }

    public static class CartItem {
        private final int idProduktu;
        private final String produktLabel;
        private int ilosc;
        private final BigDecimal cenaZakupu;

        public CartItem(int idProduktu, String produktLabel, int ilosc, BigDecimal cenaZakupu) {
            this.idProduktu = idProduktu;
            this.produktLabel = produktLabel;
            this.ilosc = ilosc;
            this.cenaZakupu = cenaZakupu;
        }

        public int getIdProduktu() {
            return idProduktu;
        }

        public String getProduktLabel() {
            return produktLabel;
        }

        public int getIlosc() {
            return ilosc;
        }

        public void setIlosc(int ilosc) {
            this.ilosc = ilosc;
        }

        public BigDecimal getCenaZakupu() {
            return cenaZakupu;
        }

        public BigDecimal getWartosc() {
            return cenaZakupu.multiply(BigDecimal.valueOf(ilosc));
        }
    }

    private static FieldConfig field(String key, String label, FieldType type, boolean required) {
        return new FieldConfig(key, label, type, required);
    }

    private static List<FieldConfig> list(FieldConfig... fields) {
        return List.of(fields);
    }

    private static List<EntityConfig> createEntityConfigs() {
        List<EntityConfig> configs = new ArrayList<>();

        configs.add(new EntityConfig(
                "Klienci",
                "/klienci",
                "/klienci",
                "/klienci/{id}",
                "/klienci/{id}",
                "id_klienta",
                list(
                        field("id_klienta", "ID", FieldType.INTEGER, false),
                        field("imie", "Imię", FieldType.TEXT, true),
                        field("nazwisko", "Nazwisko", FieldType.TEXT, true),
                        field("telefon", "Telefon", FieldType.TEXT, false),
                        field("email", "Email", FieldType.TEXT, false)
                ),
                list(
                        field("imie", "Imię", FieldType.TEXT, true),
                        field("nazwisko", "Nazwisko", FieldType.TEXT, true),
                        field("telefon", "Telefon", FieldType.TEXT, false),
                        field("email", "Email", FieldType.TEXT, false)
                )
        ));

        configs.add(new EntityConfig(
                "Adresy",
                "/adresy",
                "/adresy",
                "/adresy/{id}",
                "/adresy/{id}",
                "id_adresu",
                list(
                        field("id_adresu", "ID", FieldType.INTEGER, false),
                        field("id_klienta", "ID klienta", FieldType.INTEGER, true),
                        field("miasto", "Miasto", FieldType.TEXT, true),
                        field("ulica", "Ulica", FieldType.TEXT, true),
                        field("kod_pocztowy", "Kod pocztowy", FieldType.TEXT, true)
                ),
                list(
                        field("id_klienta", "ID klienta", FieldType.INTEGER, true),
                        field("miasto", "Miasto", FieldType.TEXT, true),
                        field("ulica", "Ulica", FieldType.TEXT, true),
                        field("kod_pocztowy", "Kod pocztowy", FieldType.TEXT, true)
                )
        ));

        configs.add(new EntityConfig(
                "Pracownicy",
                "/pracownicy",
                "/add_pracownik",
                "/edit_pracownik/{id}",
                "/del_pracownik/{id}",
                "id_pracownika",
                list(
                        field("id_pracownika", "ID", FieldType.INTEGER, false),
                        field("imie", "Imię", FieldType.TEXT, true),
                        field("nazwisko", "Nazwisko", FieldType.TEXT, true),
                        field("stanowisko", "Stanowisko", FieldType.TEXT, false)
                ),
                list(
                        field("imie", "Imię", FieldType.TEXT, true),
                        field("nazwisko", "Nazwisko", FieldType.TEXT, true),
                        field("stanowisko", "Stanowisko", FieldType.TEXT, false)
                )
        ));

        configs.add(new EntityConfig(
                "Statusy",
                "/statusy",
                "/add_status",
                "/edit_status/{id}",
                "/del_status/{id}",
                "id_statusu",
                list(
                        field("id_statusu", "ID", FieldType.INTEGER, false),
                        field("nazwa", "Nazwa", FieldType.TEXT, true)
                ),
                list(field("nazwa", "Nazwa", FieldType.TEXT, true))
        ));

        configs.add(new EntityConfig(
                "Formy płatności",
                "/formy_platnosci",
                "/formy_platnosci",
                "/formy_platnosci/{id}",
                "/formy_platnosci/{id}",
                "id_formy_platnosci",
                list(
                        field("id_formy_platnosci", "ID", FieldType.INTEGER, false),
                        field("nazwa", "Nazwa", FieldType.TEXT, true)
                ),
                list(field("nazwa", "Nazwa", FieldType.TEXT, true))
        ));

        configs.add(new EntityConfig(
                "Formy dostawy",
                "/formyDostawy",
                "/add_formaDostawy",
                "/edit_formaDostawy/{id}",
                "/del_formaDostawy/{id}",
                "id_formy_dostawy",
                list(
                        field("id_formy_dostawy", "ID", FieldType.INTEGER, false),
                        field("nazwa", "Nazwa", FieldType.TEXT, true)
                ),
                list(field("nazwa", "Nazwa", FieldType.TEXT, true))
        ));

        configs.add(new EntityConfig(
                "Firmy dostawcze",
                "/firmyDostawcze",
                "/add_firmaDostawcza",
                "/edit_firmaDostawcza/{id}",
                "/del_firmaDostawcza/{id}",
                "id_firmy",
                list(
                        field("id_firmy", "ID", FieldType.INTEGER, false),
                        field("nazwa", "Nazwa", FieldType.TEXT, true)
                ),
                list(field("nazwa", "Nazwa", FieldType.TEXT, true))
        ));

        configs.add(new EntityConfig(
                "Dostawy",
                "/dostawy",
                "/add_dostawa",
                "/edit_dostawa/{id}",
                "/del_dostawa/{id}",
                "id_dostawy",
                list(
                        field("id_dostawy", "ID", FieldType.INTEGER, false),
                        field("id_formy_dostawy", "ID formy dostawy", FieldType.INTEGER, true),
                        field("id_firmy", "ID firmy", FieldType.INTEGER, true)
                ),
                list(
                        field("id_formy_dostawy", "ID formy dostawy", FieldType.INTEGER, true),
                        field("id_firmy", "ID firmy", FieldType.INTEGER, true)
                )
        ));

        configs.add(new EntityConfig(
                "Produkty",
                "/produkty",
                "/produkty",
                "/produkty/{id}",
                "/produkty/{id}",
                "id_produktu",
                list(
                        field("id_produktu", "ID", FieldType.INTEGER, false),
                        field("nazwa", "Nazwa", FieldType.TEXT, true),
                        field("cena", "Cena", FieldType.DECIMAL, true)
                ),
                list(
                        field("nazwa", "Nazwa", FieldType.TEXT, true),
                        field("cena", "Cena", FieldType.DECIMAL, true)
                )
        ));

        configs.add(new EntityConfig(
                "Magazyny",
                "/magazyny",
                "/magazyny",
                "/magazyny/{id}",
                "/magazyny/{id}",
                "id_magazynu",
                list(
                        field("id_magazynu", "ID", FieldType.INTEGER, false),
                        field("nazwa", "Nazwa", FieldType.TEXT, true)
                ),
                list(field("nazwa", "Nazwa", FieldType.TEXT, true))
        ));

        configs.add(new EntityConfig(
                "Produkt-magazyn",
                "/produktMagazyn",
                "/produktMagazyn",
                "/produktMagazyn/{id}",
                "/produktMagazyn/{id}",
                "id",
                list(
                        field("id", "ID", FieldType.INTEGER, false),
                        field("id_produktu", "ID produktu", FieldType.INTEGER, true),
                        field("id_magazynu", "ID magazynu", FieldType.INTEGER, true),
                        field("ilosc", "Ilość", FieldType.INTEGER, true)
                ),
                list(
                        field("id_produktu", "ID produktu", FieldType.INTEGER, true),
                        field("id_magazynu", "ID magazynu", FieldType.INTEGER, true),
                        field("ilosc", "Ilość", FieldType.INTEGER, true)
                )
        ));

        configs.add(new EntityConfig(
                "Zamówienia",
                "/zamowienia",
                "/zamowienia",
                "/zamowienia/{id}",
                "/zamowienia/{id}",
                "id_zamowienia",
                list(
                        field("id_zamowienia", "ID", FieldType.INTEGER, false),
                        field("data", "Data", FieldType.DATE, true),
                        field("id_statusu", "ID statusu", FieldType.INTEGER, true),
                        field("id_pracownika", "ID pracownika", FieldType.INTEGER, true),
                        field("id_klienta", "ID klienta", FieldType.INTEGER, true),
                        field("id_formy_platnosci", "ID płatności", FieldType.INTEGER, true),
                        field("id_dostawy", "ID dostawy", FieldType.INTEGER, true)
                ),
                list(
                        field("data", "Data", FieldType.DATE, true),
                        field("id_statusu", "ID statusu", FieldType.INTEGER, true),
                        field("id_pracownika", "ID pracownika", FieldType.INTEGER, true),
                        field("id_klienta", "ID klienta", FieldType.INTEGER, true),
                        field("id_formy_platnosci", "ID płatności", FieldType.INTEGER, true),
                        field("id_dostawy", "ID dostawy", FieldType.INTEGER, true)
                )
        ));

        configs.add(new EntityConfig(
                "Pozycje zamówienia",
                "/pozycje_zamowienia",
                "/pozycje_zamowienia",
                "/pozycje_zamowienia/{id}",
                "/pozycje_zamowienia/{id}",
                "id",
                list(
                        field("id", "ID", FieldType.INTEGER, false),
                        field("id_zamowienia", "ID zamówienia", FieldType.INTEGER, true),
                        field("id_produktu", "ID produktu", FieldType.INTEGER, true),
                        field("ilosc", "Ilość", FieldType.INTEGER, true),
                        field("cena_zakupu", "Cena zakupu", FieldType.DECIMAL, true)
                ),
                list(
                        field("id_zamowienia", "ID zamówienia", FieldType.INTEGER, true),
                        field("id_produktu", "ID produktu", FieldType.INTEGER, true),
                        field("ilosc", "Ilość", FieldType.INTEGER, true),
                        field("cena_zakupu", "Cena zakupu", FieldType.DECIMAL, true)
                )
        ));

        return configs;
    }
}
