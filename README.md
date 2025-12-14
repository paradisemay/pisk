# Pisk

## Запуск
1. Требуется JDK 21+ и Maven Wrapper.
2. Собрать и запустить: `./mvnw spring-boot:run`
3. Swagger UI доступен после старта по адресу [`/swagger-ui.html`](http://localhost:8080/swagger-ui.html).

## Фронтенд
Интерфейс собран на Spring MVC + Thymeleaf, статические ресурсы лежат в `src/main/resources/static`, шаблоны — в `src/main/resources/templates`.

- Обновления фронта автоматически упаковываются при `./mvnw package`/`./mvnw spring-boot:run`, отдельный npm-билд не требуется.
- Для разработки достаточно менять CSS/JS/HTML в статике и перезапускать приложение (или использовать devtools).

## Основные страницы
- `/catalog` — каталог с фильтрами/сортировкой/пагинацией через query-параметры.
- `/tracks/{id}` — карточка трека с плеером и рейтингом.
- `/authors/{id}` — профиль автора.
- `/upload` — загрузка трека с валидацией и сообщениями рядом с полями.
- `/admin` — админ-панель с настройками.

Переключение языка RU/EN осуществляется через параметр `lang` в URL (MessageSource + CookieLocaleResolver).
