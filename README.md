# Tarantool gRPC storage

Key-Value хранилище данных на базе [Tarantool](https://www.tarantool.io/) с возможностью общения через gRPC API.
Tarantool объединяет СУБД, работающую в оперативной памяти, и сервер Lua на единой платформе, обеспечивая хранилище, соответствующее принципам ACID. Tarantool позволяет выполнять код одновременно с данными, что помогает повысить скорость операций.
## Стек технологий

**Приложение:**
- Java 17
- Maven
- Lombok

**Транспорт / API:**
- gRPC 1.72.0
- Protobuf 4.30.2
- gRPC Server Reflection

**База данных:**
- Tarantool 3.2.3
- Tarantool Java SDK 1.5.0

**Инфраструктура:**
- Docker & Docker Compose
- gRPC UI (веб-интерфейс для тестирования эндпоинтов)

**Тестирование:**
- JUnit Jupiter
- Mockito
- Testcontainers

## API

| RPC | Описание                                              |
|---|-------------------------------------------------------|
| `Put(key, value?)` | Создать или обновить пару ключ-значение               |
| `Get(key)` | Получить запись по ключу |
| `Delete(key)` | Удалить по ключу, возвращает флаг `deleted`           |
| `Range(from, to)` | GRPC-stream записей в диапазоне `[from, to)`          |
| `Count()` | Количество записей в хранилище                        |

Ключ - `string`, значение - `optional bytes`.

Подробный контракт описан в `src/main/proto/kv.proto`.

### Схема

Единственный space - `KV` (имя настраивается через `KV_SPACE_NAME`):

| Поле | Тип | Nullable | Описание |
|---|---|---|---|
| `key` | `string` | нет | Первичный ключ |
| `value` | `varbinary` | да | Значение (произвольные байты) |

Индекс: `primary` - TREE по полю `key`. Обеспечивает O(log n) поиск по ключу и упорядоченный обход для range-запросов.

### Инициализация

Схема создаётся автоматически при старте контейнера скриптом `src/main/resources/tarantool/init.lua`. Скрипт использует `if_not_exists = true`, что делает его идемпотентным — повторный запуск не приводит к ошибкам и не теряет данные.

Данные Tarantool сохраняются в Docker volume `tarantool_data`. Для полного сброса данных:

```bash
docker compose down -v
```

# Быстрый старт

## Конфигурация

Все настройки задаются через переменные окружения. Описание каждой переменной - в [`.env.example`](.env.example).

```bash
cp .env.example .env
```
## Поднятие докер котейнера
Сборка приложения:
```bash
docker-compose build --no-cache kv-service     
```
Запуск котейнера:
```bash
docker compose up -d
```

gRPC-сервис будет доступен на `localhost:9090`.

Для запуска gRPC UI (веб-интерфейс):

```bash
docker compose --profile tools up -d
```

Затем открыть `http://localhost:8080` в браузере.

## Структура проекта

```
src/main/proto/kv.proto                    -- gRPC-контракт
src/main/java/ru/thedenfire/
  Application.java                         -- точка входа
  config/AppConfig.java                    -- конфигурация из env
  controller/KvGrpcController.java         -- gRPC-обработчики
  service/KvService.java                   -- интерфейс сервиса
  service/KvServiceImpl.java               -- реализация сервиса
  repository/KvRepository.java             -- интерфейс репозитория
  repository/TarantoolKvRepository.java    -- реализация для Tarantool
  model/KeyValue.java                      -- доменная модель
src/main/resources/tarantool/init.lua      -- скрипт инициализации схемы Tarantool
```
