# ОТЧЁТ ПО ЛАБОРАТОРНОЙ РАБОТЕ №8

АО «Международный университет информационных технологий»  
Факультет бизнеса, медиа и менеджмента. Кафедра информационных систем.

**Дисциплина:** RWPSF 3305 — Web Application Development with Spring Framework.  
**Тема:** Слой доступа к данным: PostgreSQL, сущности и связи, Spring Data JPA, запросы, пагинация и сортировка.  
**Выполнила:** Diara Kegenbay.  
**Группа:** IT-2409.  
**Вариант:** 9 — именованный EntityGraph в двух методах репозитория.  
**Преподаватель:** Yemberdiyeva Aknur.  
**Год:** 2026.

## 1. Цель, исходный проект и ветка Git

Цель — заменить хранение каталога в памяти базой PostgreSQL, отобразить Author, Book и Tag с двумя видами связей, выполнить пять derived-запросов и два JPQL-запроса, измерить N+1 и реализовать постраничную выдачу REST API.

Исходный проект — spring-lab-01. Приложение проверено с Java 25 и PostgreSQL 16. При замене слоя хранения сохранены существующие страницы, форма, CRUD-операции и локализация.

Репозиторий: https://github.com/arinka0303kk-ui/lab5  
Ветка по заданию: lab08. Ссылка после публикации: https://github.com/arinka0303kk-ui/lab5/tree/lab08

Для работы получен checkout main, выполнен git pull и создана ветка lab08. Публикация удалённой ветки из этой сессии блокируется доступом GitHub: подключённому аккаунту 11reasons ранее возвращён HTTP 403. Это отдельный незавершённый шаг; локальный коммит и готовые файлы не означают, что код опубликован на GitHub.

### Условия получения доказательств

Скриншоты и результаты ниже получены при повторной проверке той же версии проекта в изолированной облачной среде; это не снимки локального компьютера. Для изоляции использованы runtime-переопределения портов: приложение 8083, PostgreSQL 5434. В сохранённом проекте остаются адреса из задания: HTTP 8080, PostgreSQL localhost:5432, база и пользователь springlab. На изображениях API реальный ответ только отформатирован для чтения; значения не изменены.

## 2. Задание 1 — PostgreSQL и сущность Book

### 2.1. Подключение

В pom.xml подключены ровно требуемые зависимости доступа к данным; их версии управляются Spring Boot 4.0.0:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

Настройки application.yml соответствуют пункту 1.3:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/springlab
    username: springlab
    password: springlab
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate.format_sql: true
```

Оригинальные настройки приложения, сообщений и локализации сохранены в application.properties; выбран профиль dev. Исходный application-dev.properties сохранён, его значения JPA совпадают с application.yml. create-drop используется только для отдельной лабораторной базы: таблицы создаются при запуске и удаляются при остановке. Вывод SQL включён, open-in-view отключён.

### 2.2. Book и репозиторий

Book — обычный не-final класс с @Entity, защищённым конструктором без аргументов, @Id и @GeneratedValue(IDENTITY). PostgreSQL генерирует идентификатор. Genre хранится строкой через EnumType.STRING; изменение порядка enum-констант не переинтерпретирует сохранённые данные. createdAt хранит время создания и не обновляется при обычном update. @Version используется для оптимистической блокировки обычных изменений сущности. Полный листинг Book приведён в приложении А.

BookRepository расширяет JpaRepository<Book, Long>. Хранение книг в ConcurrentHashMap и AtomicLong заменено операциями с PostgreSQL. Реализацию репозитория создаёт Spring Data. Успешный запуск, реальное подключение к PostgreSQL и доступность /api/books проверены.

## 3. Задание 2 — ассоциации и начальные данные

### 3.1. Диаграмма и владельцы связей

Рисунок 1 — сущности, кратности и физическая таблица связи. Изображение включено в отдельный отчёт Word/PDF.

Связи: Author (1) — Book (N); Book (N) — Tag (N) через book_tag. Владелец связи с автором — Book: именно book.author_id является внешним ключом. Author.books — обратная сторона mappedBy="author". Владелец связи с тегами — Book, поскольку в нём объявлен @JoinTable.

```java
// Book — владелец обеих связей
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "author_id")
private Author author;

@ManyToMany
@JoinTable(name = "book_tag",
    joinColumns = @JoinColumn(name = "book_id"),
    inverseJoinColumns = @JoinColumn(name = "tag_id"))
private Set<Tag> tags = new HashSet<>();

// Author — обратная сторона
@OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Book> books = new ArrayList<>();

public void addBook(Book book) {
    books.add(book);
    book.setAuthor(this);
}
```

addBook обновляет обе стороны в Java; если изменить только обратную коллекцию, owning side не получит новое значение author_id. mappedBy предотвращает создание лишней таблицы Author–Book. CascadeType.ALL распространяет операции автора на книги; orphanRemoval удаляет книгу при исключении из коллекции. Общие теги не удаляются каскадно вместе с отдельной книгой.

### 3.2. Начальные данные

CommandLineRunner DevelopmentData действует в профиле dev. Созданы 3 автора, 8 книг и 4 тега. У Isaac Asimov три книги; у I, Robot два тега. В book_tag зарегистрировано 11 связей.

| Автор | Страна | Книг |
| --- | --- | --- |
| Isaac Asimov | USA | 3 |
| Yuval Noah Harari | Israel | 2 |
| J. K. Rowling | United Kingdom | 3 |

Рисунок 2 — каталог из восьми начальных книг. Изображение включено в отдельный отчёт Word/PDF.

### 3.3. Заполненная таблица из пункта 2.3

| Вопрос | Ответ по реально созданной базе | Как проверено |
| --- | --- | --- |
| Какие таблицы созданы? | author, book, tag, book_tag | psql: \dt |
| Где внешний ключ автора? | book.author_id → author.id | psql: \d book |
| Какие колонки у book_tag? | book_id и tag_id, составной PK и два FK | psql: \d book_tag |
| Есть ли в author колонка books? | Нет: только id, country, name | psql: \d author |

Фактический текст схемы приведён в приложении В.

## 4. Задание 3 — derived, JPQL, bulk update и N+1

### 4.1. Пять derived-запросов

В BookRepository реализованы findByGenre, findByTitleContainingIgnoreCase, findByYearBetweenOrderByYearDesc, findTop3ByGenreOrderByYearDesc и countByAuthorName. Все методы вызваны в тесте; каждый дал непустой результат или положительный счётчик на начальных данных.

### 4.2. JPQL и массовое обновление

```java
@Query("""
       select b from Book b
       where b.genre = :genre and b.year >= :year
       order by b.title""")
List<Book> search(@Param("genre") Genre genre, @Param("year") int year);

@Modifying(clearAutomatically = true)
@Query("update Book b set b.archived = true where b.year < :year")
int archiveOlderThan(@Param("year") int year);
```

Вызов bulk update находится в сервисе:

```java
@Transactional
public int archiveOlderThan(int year) {
    return repository.archiveOlderThan(year);
}
```

@Modifying включает выполнение обновления. Транзакцию обеспечивает вызывающий сервис. clearAutomatically удаляет из контекста устаревшие managed-объекты после bulk update; тест проверяет отсоединение ранее загруженного объекта и изменённый archived после повторного чтения. Bulk JPQL автоматически не увеличивает поле @Version.

### 4.3. Заполненная таблица из пункта 3.3

| Метод и параметры | Возвращено / обновлено | SQL-запросов |
| --- | --- | --- |
| findByGenre(SCIENCE) | 3 книги | 1 |
| findByTitleContainingIgnoreCase("HARRY") | 3 книги | 1 |
| findTop3ByGenreOrderByYearDesc(SCIENCE) | 3 книги, начиная с 1954 года | 1 |
| search(SCIENCE,1951), JPQL | 2 книги: Foundation, The Caves of Steel | 1 |
| archiveOlderThan(1960), bulk | 3 обновлённые строки | 1 |
| findByYearBetweenOrderByYearDesc(1950,1954) | 3 книги, годы 1954,1951,1950 | 1 |
| countByAuthorName("Isaac Asimov") | значение 3 | 1 |

Измерения сделаны Hibernate Statistics в Lab08Tests. Перед ними контекст сброшен и очищен, счётчики обнулены. Вставки seed-данных не включены; изменения тестов откатываются. SQL, напечатанный Hibernate для findByTitleContainingIgnoreCase("HARRY"):

```sql
select
        b1_0.id,
        b1_0.archived,
        a1_0.id,
        a1_0.country,
        a1_0.name,
        b1_0.created_at,
        b1_0.genre,
        b1_0.title,
        b1_0.version,
        b1_0.year 
    from
        book b1_0 
    left join
        author a1_0 
            on a1_0.id=b1_0.author_id 
    where
        upper(b1_0.title) like upper(?) escape '\'
```

Параметр LIKE — %HARRY%. Именованный граф добавляет загрузку автора в тот же SQL. Остальные фактические SQL-запросы приведены в приложении Г.

### 4.4. N+1: до и после

В свежем контексте выполнено select b from Book b, затем для каждой книги напечатано book.getAuthor().getName(). Получены 4 SQL-запроса: один для книг и три для трёх разных авторов. Повторное обращение к тому же автору использует контекст, поэтому запросов четыре, а не девять.

После добавления fetch join и повторения обхода в новом контексте выполнен один запрос:

```java
@Query("select b from Book b join fetch b.author")
List<Book> findAllWithAuthor();
```

| Сценарий | Книг | Обращений SQL |
| --- | --- | --- |
| Обычная загрузка и печать имён авторов | 8 | 4 |
| findAllWithAuthor и печать имён авторов | 8 | 1 |

Фрагмент фактического результата теста:

```text
LAB08 N+1 before=4
LAB08 N+1 after=1
```

## 5. Задание 4 — Page, сортировка и эксперимент Slice

### 5.1. Итоговый endpoint

Коллекционный endpoint возвращает Page<BookResponse>. Spring MVC строит Pageable из page, size и sort. Значения по умолчанию: size=5, sort=title. DTO не раскрывает ленивые сущности напрямую в JSON. Исходный фильтр автора сохранён.

```java
    @GetMapping
    public Page<BookResponse> findAll(@RequestParam(required = false) Genre genre,
                                      @RequestParam(required = false) String author,
                                      @PageableDefault(size = 5, sort = "title") Pageable pageable) {
        return service.find(genre, author, pageable).map(BookResponse::from);
    }

```

### 5.2. Проверка первой и второй страниц

GET /api/books?page=0&size=3 вернул три книги, number=0, totalElements=8, totalPages=3.

Рисунок 3 — первая страница Page, три книги и общее количество. Изображение включено в отдельный отчёт Word/PDF.

GET /api/books?page=1&size=3&sort=year,desc вернул вторую страницу с годами 1998, 1997, 1954, number=1, totalElements=8, totalPages=3.

Рисунок 4 — вторая страница с убывающей сортировкой по году. Изображение включено в отдельный отчёт Word/PDF.

Полный фактический JSON второй страницы:

```json
{
  "content": [
    {
      "id": 7,
      "title": "Harry Potter and the Chamber of Secrets",
      "author": "J. K. Rowling",
      "year": 1998,
      "genre": "FANTASY",
      "archived": false,
      "createdAt": "2026-10-26T17:45:25.839235Z",
      "version": 0
    },
    {
      "id": 6,
      "title": "Harry Potter and the Philosopher's Stone",
      "author": "J. K. Rowling",
      "year": 1997,
      "genre": "FANTASY",
      "archived": false,
      "createdAt": "2026-10-26T17:45:25.839233Z",
      "version": 0
    },
    {
      "id": 3,
      "title": "The Caves of Steel",
      "author": "Isaac Asimov",
      "year": 1954,
      "genre": "SCIENCE",
      "archived": false,
      "createdAt": "2026-10-26T17:45:25.839220Z",
      "version": 0
    }
  ],
  "empty": false,
  "first": false,
  "last": false,
  "number": 1,
  "numberOfElements": 3,
  "pageable": {
    "offset": 3,
    "pageNumber": 1,
    "pageSize": 3,
    "paged": true,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "unpaged": false
  },
  "size": 3,
  "sort": {
    "empty": false,
    "sorted": true,
    "unsorted": false
  },
  "totalElements": 8,
  "totalPages": 3
}
```

Для этого запроса зарегистрированы два SQL — данные и count. Ниже именно вывод Hibernate, а не предполагаемый SQL:

```sql
Hibernate: 
    select
        b1_0.id,
        b1_0.archived,
        a1_0.id,
        a1_0.country,
        a1_0.name,
        b1_0.created_at,
        b1_0.genre,
        b1_0.title,
        b1_0.version,
        b1_0.year 
    from
        book b1_0 
    left join
        author a1_0 
            on a1_0.id=b1_0.author_id 
    order by
        b1_0.year desc 
    offset
        ? rows 
    fetch
        first ? rows only
Hibernate: 
    select
        count(b1_0.id) 
    from
        book b1_0
```

GET /api/books?sort=nosuchfield вернул HTTP 500: Spring Data не нашёл указанное свойство. Это наблюдение отрицательной проверки из задания 4.2; такой ответ не считается успешным.

### 5.3. Временное переключение endpoint на Slice

По пункту 4.3 тип результата endpoint и соответствующих методов сервиса/репозитория временно изменён на Slice. Временная версия собрана отдельно, без запуска Page-специфичных тестов. Реально выполнен тот же HTTP-запрос второй страницы. Он вернул те же три книги, но без totalElements и totalPages; получен один SQL-запрос. Затем исходники Page восстановлены, выполнены clean verify и повторная HTTP-проверка наличия totalElements=8.

Рисунок 5 — реальный ответ временной версии Slice без общего количества. Изображение включено в отдельный отчёт Word/PDF.

SQL временной версии Slice:

```sql
Hibernate: 
    select
        b1_0.id,
        b1_0.archived,
        a1_0.id,
        a1_0.country,
        a1_0.name,
        b1_0.created_at,
        b1_0.genre,
        b1_0.title,
        b1_0.version,
        b1_0.year 
    from
        book b1_0 
    left join
        author a1_0 
            on a1_0.id=b1_0.author_id 
    order by
        b1_0.year desc 
    offset
        ? rows 
    fetch
        first ? rows only
```

| Версия HTTP-endpoint | Книг в ответе | SQL | Общее количество |
| --- | --- | --- | --- |
| Page, page=1&size=3&sort=year,desc | 3 | 2 | totalElements=8, totalPages=3 |
| Slice, тот же запрос | 3 | 1 | Поля totalElements и totalPages отсутствуют |
| Итог после возврата к Page | 3 | Наличие Page повторно проверено | totalElements=8 |

Для публикации выбран Page: по заданию и для каталога нужно знать общее количество страниц. Slice обходится без count, когда достаточно знать наличие следующей порции. Два SQL — результат именно полной промежуточной страницы; для некоторых неполных страниц Spring Data может пропустить count.

## 6. Индивидуальный вариант 9

Именованный граф объявлен на Book и используется двумя разными методами репозитория:

```java
@NamedEntityGraph(name = "Book.author",
    attributeNodes = @NamedAttributeNode("author"))

@EntityGraph(value = "Book.author")
List<Book> findByGenre(Genre genre);

@EntityGraph(value = "Book.author")
List<Book> findByTitleContainingIgnoreCase(String fragment);
```

| Метод | Результат | Автор загружен сразу | SQL |
| --- | --- | --- | --- |
| findByGenre(SCIENCE) | 3 книги | Проверено PersistenceUnitUtil.isLoaded | 1 |
| findByTitleContainingIgnoreCase("Harry") | 3 книги | Проверено PersistenceUnitUtil.isLoaded | 1 |

Граф загружает связь к одному автору; коллекция тегов не добавлена в fetch join страницы. Поэтому пагинация остаётся на стороне базы, а не переносится в память из-за загрузки коллекции.

## 7. Итоговая проверка и команды Git из методички

После эксперимента Slice восстановлена итоговая версия Page. Выполнено .\mvnw.cmd clean verify с реальным PostgreSQL; семь тестов завершились без ошибок и пропусков. Повторный HTTP-запрос подтвердил, что конечный endpoint снова возвращает Page с totalElements=8. Существующие операции создания, чтения, изменения, включая автора, и удаления проверяются тестом; HTML/API 404 и доступность каталога проверены отдельно.

```text
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 -- Lab08Tests
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- SpringLab01ApplicationTests
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Команды начала работы из пункта 1.1:

```shell
cd spring-lab-01
git checkout main && git pull
git checkout -b lab08
```

В Windows PowerShell 5.1, где нет оператора &&, git checkout main и git pull выполняются двумя отдельными строками. Остальные команды не меняются.

Команды завершения из пункта 4.4:

```shell
git add .
git commit -m "lab08: PostgreSQL, entities, associations, queries, paging"
git push -u origin lab08
```

Эти команды выполняются внутри Git-checkout, а не внутри архива без .git. Для подготовленной копии создана ветка lab08; git add . и git commit выполнены, сообщение коммита совпадает с пунктом 4.4. Выполнена попытка git push -u origin lab08: GitHub вернул Permission denied to 11reasons, HTTP 403. Удалённая публикация не завершена. В предоставляемый архив репозитория включена .git, поэтому локальная ветка и коммит сохраняются после распаковки; доступ к GitHub требует авторизации пользователя на его компьютере.

## 8. Выводы

Spring Data JPA позволил заменить память приложения PostgreSQL и получить стандартные CRUD-операции через репозиторий без ручной реализации SQL. Для правильного хранения ассоциаций нужно явно задавать владельца внешнего ключа и поддерживать обе стороны связи в Java. Измерения показали уменьшение числа запросов с четырёх до одного при загрузке авторов через fetch join и корректную работу именованного графа. Page даёт общее количество элементов ценой count-запроса, а Slice позволяет обойтись без него, когда достаточно информации о следующей порции данных.

## Приложение А. Полный листинг Book

```java
package kz.iitu.springlab.catalog;

import jakarta.persistence.*;
import kz.iitu.springlab.web.Genre;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "book")
@NamedEntityGraph(name = "Book.author", attributeNodes = @NamedAttributeNode("author"))
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String title;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Genre genre;
    private int year;
    private boolean archived;
    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();
    @Version
    private long version;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Author author;
    @ManyToMany
    @JoinTable(name = "book_tag", joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    protected Book() { }

    public Book(String title, Genre genre, int year) {
        this.title = title;
        this.genre = genre;
        this.year = year;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public Genre getGenre() { return genre; }
    public int getYear() { return year; }
    public boolean isArchived() { return archived; }
    public Instant getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }
    public Author getAuthor() { return author; }
    public Set<Tag> getTags() { return tags; }
    public void setTitle(String title) { this.title = title; }
    public void setGenre(Genre genre) { this.genre = genre; }
    public void setYear(int year) { this.year = year; }
    public void setArchived(boolean archived) { this.archived = archived; }

    public void setAuthor(Author author) { this.author = author; }

    public void addTag(Tag tag) { tags.add(tag); }
}
```

## Приложение Б. Полный листинг BookRepository

```java
package kz.iitu.springlab.catalog;

import kz.iitu.springlab.web.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {
    @EntityGraph(value = "Book.author")
    List<Book> findByGenre(Genre genre);

    @EntityGraph(value = "Book.author")
    List<Book> findByTitleContainingIgnoreCase(String fragment);

    List<Book> findByYearBetweenOrderByYearDesc(int from, int to);
    List<Book> findTop3ByGenreOrderByYearDesc(Genre genre);
    long countByAuthorName(String name);

    @Query("""
            select b from Book b
            where b.genre = :genre and b.year >= :year
            order by b.title
            """)
    List<Book> search(@Param("genre") Genre genre, @Param("year") int year);

    @Modifying(clearAutomatically = true)
    @Query("update Book b set b.archived = true where b.year < :year")
    int archiveOlderThan(@Param("year") int year);

    @Query("select b from Book b join fetch b.author")
    List<Book> findAllWithAuthor();

    @Override
    @EntityGraph(value = "Book.author")
    Page<Book> findAll(Pageable pageable);

    @EntityGraph(value = "Book.author")
    Page<Book> findByGenre(Genre genre, Pageable pageable);

    @EntityGraph(value = "Book.author")
    @Query("select b from Book b where lower(b.author.name) = lower(:author)")
    Page<Book> findPageByAuthor(@Param("author") String author, Pageable pageable);

    @EntityGraph(value = "Book.author")
    @Query("select b from Book b where b.genre = :genre and lower(b.author.name) = lower(:author)")
    Page<Book> findPageByGenreAndAuthor(@Param("genre") Genre genre, @Param("author") String author, Pageable pageable);

    @EntityGraph(value = "Book.author")
    Slice<Book> findAllBy(Pageable pageable);
}
```

## Приложение В. Схема, полученная от PostgreSQL

Выполнены psql-команды \dt, \d book, \d book_tag и \d author. Фактический вывод:

```text
           List of relations
 Schema |   Name   | Type  |   Owner   
--------+----------+-------+-----------
 public | author   | table | springlab
 public | book     | table | springlab
 public | book_tag | table | springlab
 public | tag      | table | springlab
(4 rows)

                                        Table "public.book"
   Column   |            Type             | Collation | Nullable |             Default              
------------+-----------------------------+-----------+----------+----------------------------------
 archived   | boolean                     |           | not null | 
 year       | integer                     |           | not null | 
 author_id  | bigint                      |           |          | 
 created_at | timestamp(6) with time zone |           |          | 
 id         | bigint                      |           | not null | generated by default as identity
 version    | bigint                      |           | not null | 
 genre      | character varying(20)       |           | not null | 
 title      | character varying(120)      |           | not null | 
Indexes:
    "book_pkey" PRIMARY KEY, btree (id)
Check constraints:
    "book_genre_check" CHECK (genre::text = ANY (ARRAY['FICTION'::character varying, 'FANTASY'::character varying, 'SCIENCE'::character varying, 'HISTORY'::character varying, 'ROMANCE'::character varying]::text[]))
Foreign-key constraints:
    "fkklnrv3weler2ftkweewlky958" FOREIGN KEY (author_id) REFERENCES author(id)
Referenced by:
    TABLE "book_tag" CONSTRAINT "fkdrc33u5ufw8rdvajeveowgx7g" FOREIGN KEY (book_id) REFERENCES book(id)

              Table "public.book_tag"
 Column  |  Type  | Collation | Nullable | Default 
---------+--------+-----------+----------+---------
 book_id | bigint |           | not null | 
 tag_id  | bigint |           | not null | 
Indexes:
    "book_tag_pkey" PRIMARY KEY, btree (book_id, tag_id)
Foreign-key constraints:
    "fkdrc33u5ufw8rdvajeveowgx7g" FOREIGN KEY (book_id) REFERENCES book(id)
    "fkrxw4xl05l6ns1763bq284e7m2" FOREIGN KEY (tag_id) REFERENCES tag(id)

                                   Table "public.author"
 Column  |          Type          | Collation | Nullable |             Default              
---------+------------------------+-----------+----------+----------------------------------
 id      | bigint                 |           | not null | generated by default as identity
 country | character varying(255) |           |          | 
 name    | character varying(255) |           | not null | 
Indexes:
    "author_pkey" PRIMARY KEY, btree (id)
Referenced by:
    TABLE "book" CONSTRAINT "fkklnrv3weler2ftkweewlky958" FOREIGN KEY (author_id) REFERENCES author(id)
```

## Приложение Г. SQL всех запросов задания 3

### findByGenre(SCIENCE)

```sql
select
        b1_0.id,
        b1_0.archived,
        a1_0.id,
        a1_0.country,
        a1_0.name,
        b1_0.created_at,
        b1_0.genre,
        b1_0.title,
        b1_0.version,
        b1_0.year 
    from
        book b1_0 
    left join
        author a1_0 
            on a1_0.id=b1_0.author_id 
    where
        b1_0.genre=?
```

### findByTitleContainingIgnoreCase("HARRY")

```sql
select
        b1_0.id,
        b1_0.archived,
        a1_0.id,
        a1_0.country,
        a1_0.name,
        b1_0.created_at,
        b1_0.genre,
        b1_0.title,
        b1_0.version,
        b1_0.year 
    from
        book b1_0 
    left join
        author a1_0 
            on a1_0.id=b1_0.author_id 
    where
        upper(b1_0.title) like upper(?) escape '\'
```

### findByYearBetweenOrderByYearDesc(1950,1954)

```sql
select
        b1_0.id,
        b1_0.archived,
        b1_0.author_id,
        b1_0.created_at,
        b1_0.genre,
        b1_0.title,
        b1_0.version,
        b1_0.year 
    from
        book b1_0 
    where
        b1_0.year between ? and ? 
    order by
        b1_0.year desc
```

### findTop3ByGenreOrderByYearDesc(SCIENCE)

```sql
select
        b1_0.id,
        b1_0.archived,
        b1_0.author_id,
        b1_0.created_at,
        b1_0.genre,
        b1_0.title,
        b1_0.version,
        b1_0.year 
    from
        book b1_0 
    where
        b1_0.genre=? 
    order by
        b1_0.year desc 
    fetch
        first ? rows only
```

### countByAuthorName("Isaac Asimov")

```sql
select
        count(b1_0.id) 
    from
        book b1_0 
    left join
        author a1_0 
            on a1_0.id=b1_0.author_id 
    where
        a1_0.name=?
```

### search(SCIENCE,1951)

```sql
select
        b1_0.id,
        b1_0.archived,
        b1_0.author_id,
        b1_0.created_at,
        b1_0.genre,
        b1_0.title,
        b1_0.version,
        b1_0.year 
    from
        book b1_0 
    where
        b1_0.genre=? 
        and b1_0.year>=? 
    order by
        b1_0.title
```

### archiveOlderThan(1960)

```sql
update
        book b1_0 
    set
        archived=true 
    where
        b1_0.year<?
```

