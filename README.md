# Wymagania

- JDK 21
- IntelliJ IDEA
- Maven, może być wbudowany w IntelliJ

## Uruchomienie w IntelliJ IDEA

1. Otwórz IntelliJ IDEA.
2. Wybierz `File -> Open`.
3. Wskaż folder projektu albo plik `pom.xml`.
4. Poczekaj, aż Maven pobierze zależności.
5. Uruchom aplikację przez Maven:

```bash
mvn javafx:run
```

Możesz też w IntelliJ otworzyć panel `Maven`, znaleźć plugin `javafx` i uruchomić goal `javafx:run`.

## Struktura

```text
src/
  main/
    java/
      module-info.java
      com/example/app/
        App.java
        MainController.java
    resources/
      com/example/app/
        main-view.fxml
        style.css
pom.xml
```

