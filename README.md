# Java_Project_Skillbox

# Поисковый движок

Локальный поисковый движок для индексации и поиска по веб-сайтам. Даный поисковый движок разрабатывался информационно-новостного портала. Он позволяет быстро и удобно найти нужную информацию, используя браузер.

## Описание проекта

Spring Boot приложение, которое индексирует веб-сайты и предоставляет API для поиска по ним. Движок использует многопоточную индексацию, лемматизацию текста и расчет релевантности для точного поиска.

## Технологический стек

- **Java 17**
- **Spring Boot 2.7.1**
- **MySQL 8.0**
- **Hibernate/JPA** 
- **JSoup**
- **Apache Lucene Morphology**
- **Maven**
- **Lombok**

## Использование
Для поиска и индексации нужно перейти в браузере на следующий адрес: http://localhost:8080.
### Структура интерфейса
- **Dashboard** - статистика по всем сайтам
- **Management** - управление индексацией
- **Search** - поиск по проиндексированным сайтам
- 
## Запуск
- Клонирование репозитория: 
 В среде разработки IntelliJ IDEA Clone Repository по URL ссылке https://github.com/Sergio12473/Java_Project_Skillbox
- Установить библиотеки зависимостей указанных в pom.xml
- Создание базы данных:
 Создайте пустую базу данных в MySQL Workbench \ CREATE DATABASE search_engine;

- Настройка конфигурации

Отредактируйте `application.yaml` **в корне проекта**:

```yaml
server:
  port: 8080

spring:
  datasource:
    username: root           # ваш логин MySQL
    password: your_password  # ваш пароль MySQL
    url: jdbc:mysql://localhost:3306/search_engine?useSSL=false&requir
      eSSL=false&allowPublicKeyRetrieval=true
indexing-settings:
  sites:
    - url: https://playback.ru
      name: PlayBack.Ru
    # добавьте свои сайты
```
- Собрать и запустить проект
