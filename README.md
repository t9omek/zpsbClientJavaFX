# JavaFX API Client - kontrola magazynu

Wersja zawiera zmianę w flow sprzedaży:

- pobiera stany z endpointu `GET /produktMagazyn`,
- blokuje dodanie do koszyka ilości większej niż dostępna w magazynie,
- ponownie sprawdza stan magazynu przed zatwierdzeniem sprzedaży,
- po zapisaniu sprzedaży zmniejsza stan magazynowy przez `PUT /produktMagazyn/{id}`.

Konfiguracja API znajduje się w:

```text
src/main/resources/app.properties
```

Uruchomienie:

```bash
mvn javafx:run
```

Backend FastAPI powinien działać na adresie z `app.properties`.

## Dodatkowe poprawki UX

W tej wersji dodano:
- wyszukiwarkę nad każdą tabelą CRUD,
- okno potwierdzenia przed usunięciem rekordu,
- blokadę przycisku „Zatwierdź sprzedaż” podczas zapisu, aby uniknąć podwójnego utworzenia zamówienia.
