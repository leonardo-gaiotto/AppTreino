# AppTreino

## Objetivo do aplicativo

O **AppTreino** conecta o personal trainer (**coach**) aos seus **alunos**. O coach monta o treino
de cada aluno, escolhendo os exercícios em um catálogo com vídeos do YouTube, e acompanha a
evolução das cargas semana a semana. O aluno abre o app na academia, vê o treino do dia, assiste ao
vídeo de execução de cada exercício e **registra cada série** (repetições e carga). O app compara o
que foi feito com o que o coach pediu.

- Android nativo em **Kotlin**, com **Views e layouts XML** (sem Jetpack Compose)
- Material Design 3, com tema escuro por padrão e destaque verde-limão
- Dados de demonstração (mocks) em memória, **sem API e sem banco de dados**

---

## Como rodar o projeto localmente

### Pré-requisitos
- **Android Studio 2026.1.4 ou mais recente**. O projeto usa o Android Gradle Plugin 9.4.1 e o
  Gradle 9.6.0, que o próprio projeto baixa pelo *Gradle Wrapper*.
- **Android SDK Platform 37** (Android 17). Se não estiver instalado, o Android Studio oferece a
  instalação ao sincronizar o projeto (*Tools › SDK Manager*).
- **Internet na primeira compilação**, para baixar o Gradle e as bibliotecas, e no app, para
  carregar as miniaturas dos vídeos.
- O Gradle roda com o **JDK 25**, que já vem embutido no Android Studio. Se a máquina não tiver um
  JDK 25, o Gradle baixa um automaticamente (configurado em `gradle/gradle-daemon-jvm.properties`).

### Opção 1: Android Studio (recomendado)
1. Clone o repositório:
   ```bash
   git clone https://github.com/leonardo-gaiotto/AppTreino.git
   ```
2. No Android Studio: **File › Open** e selecione a pasta `AppTreino`.
3. Aguarde o **Gradle Sync** terminar. Na primeira vez ele baixa as dependências.
4. Escolha um emulador ou um celular com a depuração USB ativada e clique em **Run ▶**
   (configuração `app`).

### Opção 2: linha de comando
Com o Android SDK instalado e a variável `ANDROID_HOME` apontando para ele. O Android Studio
também cria o arquivo `local.properties` com esse caminho automaticamente.

```bash
# Windows (PowerShell)
.\gradlew.bat testDebugUnitTest assembleDebug

# macOS / Linux
./gradlew testDebugUnitTest assembleDebug
```

O APK fica em `app/build/outputs/apk/debug/app-debug.apk` e pode ser instalado com
`adb install -r app/build/outputs/apk/debug/app-debug.apk`.

### Usando o app
- O app abre no **modo Aluno** (Leonardo): abas **Hoje**, **Semana** e **Perfil**.
- Para ver o **modo Coach**, vá em **Perfil › Usar o app como › Coach**: aparecem as abas
  **Alunos** e **Perfil**.
- Os dados ficam só na memória. Ao fechar o app, tudo volta ao estado de demonstração (também dá
  para restaurar em **Perfil › Restaurar dados de demonstração**).

---

## Bibliotecas externas

Todas vêm dos repositórios públicos Google Maven e Maven Central e estão declaradas em
`gradle/libs.versions.toml`.

| Biblioteca | Versão | Para que serve no app |
|---|---|---|
| **AndroidX Core KTX** (`androidx.core:core-ktx`) | 1.19.1 | Extensões Kotlin para APIs do Android (ex.: `isVisible`, `updatePadding`, tratamento de *window insets* no edge-to-edge) |
| **AndroidX Core SplashScreen** (`androidx.core:core-splashscreen`) | 1.2.0 | Splash screen oficial, compatível com versões antigas do Android |
| **AndroidX AppCompat** (`androidx.appcompat:appcompat`) | 1.8.0 | Base das Activities (`AppCompatActivity`) e troca entre tema claro e escuro |
| **AndroidX Activity KTX** (`androidx.activity:activity-ktx`) | 1.13.0 | `enableEdgeToEdge()`, `by viewModels()`, botão voltar (`OnBackPressedCallback`) e Activity Result API (catálogo → editor) |
| **AndroidX Fragment KTX** (`androidx.fragment:fragment-ktx`) | 1.9.1 | Fragments das abas e dos formulários, `activityViewModels()` e Fragment Result API |
| **AndroidX Lifecycle** (`lifecycle-runtime-ktx` e `lifecycle-viewmodel-ktx`) | 2.11.0 | `ViewModel` (sobrevive à rotação), `SavedStateHandle` e `repeatOnLifecycle` (só atualiza a tela quando ela está visível) |
| **Kotlin Coroutines** (via Lifecycle) | — | `StateFlow` e `combine`: o repositório publica o estado e as telas reagem às mudanças |
| **AndroidX RecyclerView** (`androidx.recyclerview:recyclerview`) | 1.4.0 | Todas as listas, com `ListAdapter` + `DiffUtil` e arrastar para reordenar (`ItemTouchHelper`) |
| **AndroidX ViewPager2** (`androidx.viewpager2:viewpager2`) | 1.1.0 | Abas deslizantes do painel do aluno (Resumo, Treinos, Evolução) |
| **Material Components** (`com.google.android.material:material`) | 1.14.0 | Componentes Material 3: Bottom Navigation, cards, botões, chips, campos de texto, abas, bottom sheets, diálogos e Snackbar |
| **Coil 3** (`io.coil-kt.coil3:coil`) | 3.6.3 | Download, cache e exibição das miniaturas dos vídeos do YouTube |
| **Coil Network OkHttp** (`io.coil-kt.coil3:coil-network-okhttp`) | 3.6.3 | Camada de rede do Coil (usa o OkHttp para baixar as imagens) |
| **JUnit 4** (`junit:junit`) — só testes | 4.13.2 | Testes unitários que rodam no computador, sem emulador |

Plugins de build: **Android Gradle Plugin 9.4.1** e **Kotlin 2.4.20**. O plugin Kotlin é declarado
só para fixar a versão do compilador; o AGP 9 já traz o Kotlin embutido.

Nenhuma biblioteca de gráficos foi usada: o gráfico de evolução (`LoadChartView`) é desenhado à mão
com `Canvas`.

---

## Funcionalidades

O modo é escolhido no **Perfil** (seletor **Aluno | Coach**) e muda as abas da Bottom Navigation.

| Modo | Abas | Para quê |
|---|---|---|
| **Aluno** (padrão) | Hoje · Semana · Perfil | Seguir o treino do dia e registrar as séries |
| **Coach** | Alunos · Perfil | Acompanhar os alunos, montar treinos e ver a evolução |

O aluno do modo Aluno (**Leonardo**) é um dos 5 alunos do coach. Os dois modos leem o **mesmo
repositório**, então o que o Leonardo registra aparece na hora na evolução dele no painel do
coach, e o que o coach muda no treino dele aparece na hora nas abas Hoje e Semana.

### Modo Aluno
- **Hoje**: treino do dia, progresso (exercícios + cardio) e cards de exercício e de cardio.
- **Detalhe do exercício**: vídeo, prescrição do coach, observação, passo a passo, erros comuns e o
  **registro das séries**:
  - cada série vem com o valor do coach; o aluno ajusta com − / + e toca em ✓;
  - selos mostram se ficou **acima**, **no alvo** ou **abaixo** do previsto;
  - com todas as séries registradas, o exercício fica **feito automaticamente**.
- **Semana**: os 7 dias com o treino e o progresso de cada um.

### Modo Coach
- **Alunos**: 5 alunos mockados, **busca por nome** e **cadastro** de aluno com validação.
- **Painel do aluno**, com três abas:
  - **Resumo**: semana, sequência de treinos e dados físicos (com IMC);
  - **Treinos**: criar, editar e excluir;
  - **Evolução**: gráfico e histórico de cada exercício, com cerca de 6 semanas de dados mockados.
- **Editor de treino**:
  - nome e dias da semana (chips); cada dia tem no máximo **um** treino;
  - exercícios do **catálogo** (29 exercícios, busca + filtro por grupo muscular), com vídeo e
    miniatura automáticos;
  - séries, repetições, carga, descanso e observação para cada exercício; dá para reordenar e remover;
  - cardio (esteira, bicicleta, elíptico, escada, corda, remo);
  - validações: nome, dia, conteúdo e valores não negativos.

---

## Onde está cada requisito da disciplina

| Requisito | Onde |
|---|---|
| Views (TextView, ImageView, Space) e ViewGroups (LinearLayout, FrameLayout, RecyclerView) em XML | Todos os `app/src/main/res/layout/*.xml` |
| Duas telas principais | `ui/main/MainActivity.kt` (abas) e `ui/exercise/ExerciseDetailActivity.kt` |
| **Intent explícita** entre as telas principais | `ExerciseDetailActivity.newIntent()`, chamada em `TodayFragment.openExerciseDetail()` |
| Intent explícita nas telas novas | `newIntent()` de `StudentFormActivity`, `StudentDashboardActivity`, `ExerciseHistoryActivity` e `WorkoutEditorActivity`; `ExerciseCatalogActivity.PickExercise` (Activity Result API) |
| **ListAdapter + DiffUtil** | `TodayAdapter` (3 tipos de item + atualização parcial por *payload*), `WeekDayAdapter`, `SetLogAdapter`, `StudentAdapter`, `WorkoutCardAdapter`, `EvolutionAdapter`, `SessionAdapter`, `CatalogAdapter`, `EditorCardioAdapter` |
| **ViewBinding** | `buildFeatures.viewBinding = true`; nos Fragments o binding é zerado em `onDestroyView()` |
| **Ciclo de vida** | `repeatOnLifecycle(STARTED)` em todas as telas; ViewModels sobrevivem à rotação |
| **Data classes imutáveis** | `data/model/*`: só `val` e listas somente leitura; cada mudança cria um novo `GymState` |
| **Mocks, sem API e sem banco** | `data/MockData.kt` e `data/ExerciseCatalog.kt`; o `GymRepository` guarda tudo em memória |
| Textos, cores e dimensões em resources | `values/strings.xml`, `colors.xml` (+ `values-night`), `dimens.xml`, `styles.xml` |

---

## Arquitetura

```
data/model/   Modelos imutáveis (Student, Workout, PrescribedExercise, Cardio, CatalogExercise,
              SetLog/ExerciseLog, UiText) e enums
data/         GymRepository (estado único em memória, StateFlow), MockData, ExerciseCatalog, AppSettings
domain/       Regras puras e testáveis: progresso, sequência, evolução, comparação coach × aluno, validações
ui/main       MainActivity (Bottom Navigation) + MainViewModel compartilhado pelas abas
ui/today|week|profile   Abas do aluno e Perfil (troca de modo)
ui/exercise   Detalhe do exercício + registro das séries
ui/students   Aba Alunos + cadastro
ui/dashboard  Painel do aluno (Resumo, Treinos, Evolução) + histórico do exercício
ui/editor     Editor de treino, catálogo e formulários (bottom sheets)
ui/common     Formatação de textos, gráfico (Canvas), insets, miniaturas
util/         Links e abertura de vídeos do YouTube (Intent implícita)
```

Os dados fluem em uma só direção: o **repositório** expõe um `StateFlow<GymState>`, cada
**ViewModel** transforma esse estado no estado da tela, e a **tela** apenas o desenha.

As decisões técnicas estão explicadas em perguntas e respostas no arquivo
[`DECISOES_TECNICAS.md`](DECISOES_TECNICAS.md).

---

## Testes

```bash
./gradlew testDebugUnitTest   # testes unitários (JVM, sem emulador)
./gradlew lintDebug           # análise estática do Android
```

Os testes cobrem:
- catálogo e dados mockados;
- repositório: registro das séries, treinos e alunos;
- progresso e sequência de treinos;
- evolução e comparação coach × aluno;
- validações e estados de tela;
- regras de DiffUtil.
