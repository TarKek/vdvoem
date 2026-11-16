# Шаблон плагина

Скопируй эту папку в `feature/my`, замени:

- `my` в пути пакета и в `namespace`/`applicationId`-подобных полях `build.gradle.kts`;
- `My` в именах классов (`MyModule`, `MyViewModel`, `MyScreen`);
- `core.my` в `manifest.id` и `"my"` в `route`;
- строки в `src/main/res/values/strings.xml` и `values-en/strings.xml`.

Дальше зарегистрируй модуль (см. `docs/PLUGIN_SDK.md`, раздел «Регистрация»)
и собери `.\gradlew.bat :feature:my:assembleDebug`.

Файлы шаблона намеренно не входят в `settings.gradle.kts` — иначе проект
собирал бы недописанный пример.