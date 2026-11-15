# Курсограф. Установка и запуск на Windows

Порядок рассчитан на Windows 10 или 11 x64 с процессором Intel или AMD. Для Android Studio с эмулятором Google указывает минимум 16 ГБ оперативной памяти и 16 ГБ свободного места. Удобнее выделить 32 ГБ на SDK, образ Android, кэш Gradle и сборки. Для установки нужен интернет. [Требования Android Studio](https://developer.android.com/studio/install#windows)

## 1. Включи ускорение эмулятора

1. Открой **Диспетчер задач → Производительность → ЦП**. Проверь строку **Виртуализация**. Если она выключена, включи в UEFI параметр **Intel Virtualization Technology / VT-x** или **AMD SVM Mode** и снова загрузи Windows.
2. Открой **PowerShell от имени администратора** и выполни команду.

   ```powershell
   Enable-WindowsOptionalFeature -Online -FeatureName HypervisorPlatform -All
   ```

3. Перезагрузи Windows. Эмулятор будет использовать Windows Hypervisor Platform (WHPX). [Ускорение Android Emulator на Windows](https://developer.android.com/studio/run/emulator-acceleration#vm-windows-whpx)

## 2. Установи Git, Java и Android Studio

1. Скачай и запусти **Git for Windows x64 Setup**. На шаге выбора PATH сохрани **Git from the command line and also from 3rd-party software**, на шаге credential helper — **Git Credential Manager**. Открой новое окно PowerShell и проверь `git --version`. [Установщик Git](https://git-scm.com/install/windows)
2. На странице Microsoft в разделе **OpenJDK 21** выбери **Windows x64 MSI**. В настройках установщика включи добавление Java в PATH и настройку переменной `JAVA_HOME`. Запомни папку установки, обычно это `C:\Program Files\Microsoft\jdk-21.<версия>`. После установки открой новое окно PowerShell. [Установщик JDK 21](https://learn.microsoft.com/en-us/java/openjdk/download#openjdk-21)

   ```powershell
   java -version
   javac -version
   $env:JAVA_HOME
   ```

   Обе команды должны показать версию **21**. Если используется другая версия, задай `JAVA_HOME` для текущего окна PowerShell. Замени значение на путь к установленному JDK 21.

   ```powershell
   $env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-21.ПОДСТАВЬ_СВОЮ_ВЕРСИЮ'
   & "$env:JAVA_HOME\bin\javac.exe" -version
   ```

3. Скачай стабильный **Android Studio** для Windows в формате `.exe`. В установщике оставь **Android Studio** и **Android Virtual Device**. При первом запуске пройди мастер **Standard** и прими лицензии SDK. [Android Studio](https://developer.android.com/studio)
4. На стартовом экране открой **More Actions → SDK Manager**. Из открытого проекта тот же экран доступен через **Tools → SDK Manager**. Запиши путь в поле **Android SDK Location**. Обычный путь — `%LOCALAPPDATA%\Android\Sdk`.
5. На вкладке **SDK Platforms** установи **Android 16.0 (API 36)**. На вкладке **SDK Tools** отметь **Android SDK Platform-Tools** и **Android Emulator**. Включи **Show Package Details** и под **Android SDK Build-Tools** отметь **35.0.0**. Нажми **Apply** и дождись установки.
6. В обычном PowerShell проверь ускорение. Если SDK установлен в другой каталог, подставь его путь вместо указанного.

   ```powershell
   $Sdk = "$env:LOCALAPPDATA\Android\Sdk"
   & "$Sdk\emulator\emulator.exe" -accel-check
   ```

   В выводе должна быть строка, что **WHPX** установлен и доступен.

## 3. Распакуй проект и восстанови локальные ветки

Скачай `Lab4_Kursograf_Android.zip` в папку «Загрузки». Выполни команды в обычном PowerShell. Для другой папки загрузок измени путь к ZIP в первой команде распаковки.

```powershell
$Lab = Join-Path $env:USERPROFILE 'Projects\Lab4'
New-Item -ItemType Directory -Force -Path $Lab | Out-Null
Expand-Archive -LiteralPath "$env:USERPROFILE\Downloads\Lab4_Kursograf_Android.zip" -DestinationPath $Lab
Set-Location $Lab
git clone --branch 04-testy-i-otchet .\lab4-kursograf.bundle .\kursograf-android
Set-Location .\kursograf-android
git branch --no-track main origin/main
git branch --no-track 01-proekt origin/01-proekt
git branch --no-track 02-konverter-i-kesh origin/02-konverter-i-kesh
git branch --no-track 03-analitika origin/03-analitika
git remote remove origin
git branch
```

В папке `kursograf-android` появятся исходники и пять локальных веток. Звёздочка в выводе `git branch` должна стоять у `04-testy-i-otchet`. До окончания слияний это ветка с полной версией.

## 4. Создай новый репозиторий на GitHub и открой четыре Pull Request

1. В браузере войди в GitHub и выбери **+ → New repository**. Назови его `lab4-kursograf-android`. Видимость выбери по условиям преподавателя. При создании оставь **Add a README** выключенным, **.gitignore** и **License** — **None**. [Создание репозитория GitHub](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-new-repository)
2. На странице нового репозитория скопируй URL **HTTPS**. В PowerShell из папки `kursograf-android` выполни команды ниже. Вместо `ТВОЙ_ЛОГИН` подставь GitHub-логин или вставь скопированный URL целиком. При первом push Git Credential Manager предложит вход в браузере.

   ```powershell
   git remote add origin https://github.com/ТВОЙ_ЛОГИН/lab4-kursograf-android.git
   git push -u origin main
   git push -u origin 01-proekt
   git push -u origin 02-konverter-i-kesh
   git push -u origin 03-analitika
   git push -u origin 04-testy-i-otchet
   ```

   Первый push публикует начальный README и создаёт базовую ветку `main`. Исходники попадают в `main` через следующие четыре PR.

3. В GitHub открой **Pull requests → New pull request**. Выбирай **base: main** и ветку **compare** из таблицы. Нажми **Create pull request**, заполни заголовок и создай PR. Открой меню слияния, выбери **Create a merge commit**, затем **Merge pull request → Confirm merge**. Сначала заверши текущий PR и переходи к следующему. Оставь ветки в репозитории для защиты. Если пункт слияния недоступен, открой **Settings → General → Pull Requests** и включи **Allow merge commits**. [Слияние Pull Request](https://docs.github.com/en/pull-requests/how-tos/merge-and-close-pull-requests/merging-a-pull-request)

   | Порядок | Ветка compare | Заголовок |
   |---|---|---|
   | 1 | `01-proekt` | Настройка проекта Android |
   | 2 | `02-konverter-i-kesh` | Конвертер, НБМ и локальный кэш |
   | 3 | `03-analitika` | Аналитика курса за неделю и месяц |
   | 4 | `04-testy-i-otchet` | Unit-тесты и отчёт |

4. Обнови `main` на компьютере после четвёртого PR.

   ```powershell
   git switch main
   git pull --ff-only origin main
   git log --oneline --graph --all
   git status
   ```

   В GitHub раздел **Pull requests → Closed** должен показывать четыре PR со статусом **Merged**. В корне `main` должны быть `README.md`, `REPORT.md`, `WINDOWS.md`, Gradle Wrapper и папка `app`.

## 5. Открой проект и создай эмулятор Android 16

1. В Android Studio нажми **Open** и выбери `%USERPROFILE%\Projects\Lab4\kursograf-android`, где лежит `settings.gradle.kts`. Подтверди **Trust Project**.
2. Открой **File → Settings → Build, Execution, Deployment → Build Tools → Gradle**. В поле **Gradle JDK** выбери установленный **JDK 21**. Если его нет в списке, выбери **Add JDK** и укажи папку JDK 21 из шага 2. Нажми **Apply**. Выполни **File → Sync Project with Gradle Files** и дождись завершения синхронизации. Gradle 8.13 загружается через Wrapper проекта. [Настройка JDK для Android Studio](https://developer.android.com/build/jdks)
3. Открой **View → Tool Windows → Device Manager → + → Create Virtual Device**. В категории **Phone** выбери **Pixel 7**. Для образа выбери **Android 16.0 (API 36)**, **Google APIs**, архитектуру **x86_64**. При необходимости нажми значок скачивания. Подтверди настройки кнопкой **Finish**. [Device Manager](https://developer.android.com/studio/run/managing-avds)
4. В **Device Manager** нажми ▶ рядом с новым устройством и дождись домашнего экрана Android. Сверху в Android Studio выбери конфигурацию **app**, затем эмулятор. Нажми **Run ▶** или **Shift+F10**.
5. Дождись запуска «Курсографа» и окончания первой загрузки курсов. Для проверки выбери `EUR → MDL`, введи `100` и посмотри сумму, курс и его дату. Через нижние вкладки открой **Неделя** и **Месяц**. При наличии ответов за все дни счётчик покажет **7 из 7** и **30 из 30**. Прокрути экран до минимума, максимума, среднего, двух величин изменения и направления тенденции.

## 6. Запусти тесты и проверь офлайн

1. Открой PowerShell в корне проекта. Если `ANDROID_HOME` ещё не задан, укажи путь из **SDK Manager**. Запусти тесты.

   ```powershell
   Set-Location "$env:USERPROFILE\Projects\Lab4\kursograf-android"
   $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
   .\gradlew.bat :app:testDebugUnitTest
   ```

   Ожидаемый результат — **BUILD SUCCESSFUL**. Подробности в `app\build\reports\tests\testDebugUnitTest\index.html`.

2. После успешной загрузки курсов открой шторку Android, включи **Режим полёта** и отключи **Wi-Fi**, если Android его сохранил включённым.
3. Перезапусти процесс приложения. В том же окне PowerShell выполни команды, когда запущен один эмулятор.

   ```powershell
   $Adb = "$env:ANDROID_HOME\platform-tools\adb.exe"
   & $Adb -e shell am force-stop md.kursograf.lab4
   & $Adb -e shell am start -n md.kursograf.lab4/.MainActivity
   ```

4. На конвертере проверь надпись **Данные из кэша** с датой. Снова открой **Неделя** и **Месяц**. Ранее полученные графики и статистика должны открыться. Отключи режим полёта, включи Wi-Fi и нажми **Обновить** для повторной синхронизации.
5. Для отчёта зафиксируй результат своего запуска и добавь в раздел 6 собственный пример работы с ИИ при переходе на Kotlin. Для изменения после слияния создай отдельную ветку из актуального `main`.

   ```powershell
   git switch main
   git pull --ff-only origin main
   git switch -c 05-lichnaya-proverka
   notepad REPORT.md
   ```

   Сохрани отчёт. Если Git впервые создаёт коммит в этом проекте, задай имя и почту в конфигурации проекта. Можно использовать адрес `noreply`, указанный в **GitHub → Settings → Emails**. Замени примеры своими данными.

   ```powershell
   git config user.name "Имя Фамилия"
   git config user.email "адрес-из-настроек-GitHub"
   git add REPORT.md
   git commit -m "Дополнить отчёт результатами запуска в эмуляторе"
   git push -u origin 05-lichnaya-proverka
   ```

   Создай PR с **base: main** и **compare: 05-lichnaya-proverka**, затем выполни слияние так же, как на шаге 4.

## Если запуск остановился на ошибке

| Сообщение или симптом | Что проверить |
|---|---|
| `git` не распознаётся | Открой новый PowerShell после установки Git и выполни `git --version`. |
| `JAVA_HOME is not set` или ошибка версии Java | В PowerShell и в настройках **Gradle JDK** укажи JDK 21. Проверь `javac -version` и `$env:JAVA_HOME`. |
| `SDK location not found` или отсутствует Platform 36 | Проверь фактический путь в **SDK Manager**, установи API 36 и снова синхронизируй Gradle. |
| Эмулятор сообщает об отсутствии ускорения | Проверь виртуализацию, WHPX и перезагрузку Windows. Повтори `emulator.exe -accel-check`. |
| Gradle не может скачать зависимости | Проверь интернет и открой повторную синхронизацию в Android Studio. При корпоративном прокси задай его через **Settings → Appearance & Behavior → System Settings → HTTP Proxy**. |
| НБМ недоступен при первом запуске | Отключи режим полёта, проверь Wi-Fi и интернет в браузере эмулятора, затем нажми **Обновить** в приложении. Кэш появляется после первой успешной загрузки. |
