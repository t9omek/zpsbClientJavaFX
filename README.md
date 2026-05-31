# JavaFX klient zpsbAPI

Aplikacja JavaFX konsumująca API FastAPI z projektu `zpsbAPI`.

## Funkcje

- CRUD dla głównych encji API: klienci, adresy, pracownicy, statusy, dostawy, produkty, magazyny, zamówienia i pozycje zamówień.
- Zakładka **Sprzedaż** pozwalająca przejść flow sprzedaży bez zmian w backendzie:
  1. wybór klienta, pracownika, statusu, płatności i dostawy,
  2. dodanie produktów do koszyka,
  3. utworzenie dostawy,
  4. utworzenie zamówienia,
  5. utworzenie pozycji zamówienia.
- Zakładka **Sprzedaże** pokazująca listę sprzedaży/zamówień, ich status, klienta, pracownika, formę płatności, dostawę oraz wartość. Po kliknięciu sprzedaży widoczne są jej pozycje.

## Uruchomienie

Najpierw uruchom backend:

```bash
uvicorn server:app --reload
```

Potem uruchom klienta:

```bash
mvn javafx:run
```

Domyślna konfiguracja API:

```text
http://127.0.0.1:8000
x-api-key: tajny-klucz-123
```

Możesz zmienić adres i klucz przez parametry JVM:

```bash
mvn javafx:run -Dapi.baseUrl=http://127.0.0.1:8000 -Dapi.key=tajny-klucz-123
```
