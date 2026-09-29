# Decisões técnicas — perguntas e respostas

Perguntas que o professor pode fazer sobre o AppTreino, com respostas curtas e onde olhar no código.

---

## Interface e navegação

**1. Por que Views com XML e não Jetpack Compose?**
É um requisito da disciplina. Todas as telas usam layouts XML (`res/layout`) com TextView,
ImageView, Space, LinearLayout, FrameLayout e RecyclerView, além de componentes do Material
(cards, chips, Bottom Navigation).

**2. Como é a navegação entre as duas telas principais?**
Por **Intent explícita**, que informa exatamente a classe a abrir:
`Intent(context, ExerciseDetailActivity::class.java)`. Ela é criada em
`ExerciseDetailActivity.newIntent()` e chamada em `TodayFragment.openExerciseDetail()`.
Centralizar a criação em `newIntent()` garante que quem abre a tela sempre envie os extras certos
(ID do exercício e data). Na tela aberta, os extras chegam no `SavedStateHandle` do ViewModel.

**3. Qual a diferença entre Intent explícita e implícita? O app usa as duas?**
A explícita diz a **classe** de destino (navegação dentro do app). A implícita diz a **ação** e o
Android escolhe quem trata. O app usa implícita só para abrir o vídeo
(`Intent.ACTION_VIEW` com o link do YouTube, em `util/YouTube.kt`), que abre no app do YouTube
ou no navegador.

**4. Por que a tela principal usa Fragments?**
A Bottom Navigation troca entre abas dentro da mesma Activity. Cada aba é um Fragment. A
`MainActivity` adiciona cada Fragment **uma vez** e depois só mostra/esconde (`show`/`hide`), o que
preserva a posição de rolagem de cada aba. As abas mudam conforme o modo (Aluno ou Coach).

**5. Como as abas conversam entre si?**
De duas formas:
- **ViewModel compartilhado** (`activityViewModels()`): a aba Semana escolhe o dia e a aba Hoje
  mostra esse dia;
- **Fragment Result API**: um Fragment pede à Activity para trocar de aba
  (`MainActivity.requestTab`), sem depender diretamente dela.

**6. Como o catálogo devolve o exercício escolhido para o editor?**
Pela **Activity Result API**: `ExerciseCatalogActivity.PickExercise` é um contrato que abre o
catálogo com Intent explícita e devolve o ID do exercício (ou `null` se o coach voltar sem
escolher). O método antigo `startActivityForResult` está depreciado.

---

## Listas

**7. Por que `ListAdapter` + `DiffUtil` e não `notifyDataSetChanged()`?**
O `notifyDataSetChanged()` redesenha a lista inteira e perde as animações. Com `ListAdapter`,
basta chamar `submitList(novaLista)`: o `DiffUtil` compara a lista antiga com a nova em segundo
plano e atualiza **só os itens que mudaram**, com animação.

Cada adapter tem um `DiffUtil.ItemCallback` com duas regras:
- `areItemsTheSame`: é o mesmo item? Compara o ID;
- `areContentsTheSame`: o conteúdo mudou? Compara a data class inteira.

**8. O que é o *payload* do `TodayAdapter`?**
Quando só o status "feito" de um exercício muda, o `getChangePayload` devolve
`PAYLOAD_STATUS` e o adapter redesenha **só o check e a borda**, sem recarregar a miniatura
(evita que a imagem pisque).

**9. Por que o `TodayAdapter` tem vários tipos de item?**
A lista do dia mistura exercícios, um título "Cardio" e cards de cardio. O `getItemViewType`
escolhe o layout de cada item, e uma `sealed interface TodayListItem` garante que todos os
tipos sejam tratados.

**10. Por que o `EditorExerciseAdapter` não é um `ListAdapter`?**
Por causa do **arrastar para reordenar**. No `ListAdapter` o diff roda em segundo plano, e durante
o arraste o `ItemTouchHelper` poderia pedir a mesma troca duas vezes antes de o diff chegar, e o
item pularia duas posições. Nessa lista o `DiffUtil` roda de forma **síncrona**: a lista da tela
muda na hora e a ordem final vai para o ViewModel ao soltar o item.

---

## Arquitetura e estado

**11. Como o código está organizado?**
Em camadas:
- `data`: modelos, repositório e mocks;
- `domain`: regras de negócio puras, sem Android;
- `ui`: Activities, Fragments, ViewModels e adapters.

Os dados seguem **fluxo em uma direção só**: o repositório publica o estado, o ViewModel
transforma no estado da tela, e a tela só desenha e repassa as ações do usuário ao ViewModel.

**12. Por que ViewModel? O que acontece ao girar a tela?**
Ao girar, a Activity é destruída e recriada, mas o **ViewModel sobrevive**. Os dados e o
rascunho do editor não se perdem. Informações pequenas, como o dia selecionado e os extras da
Intent, ficam também no `SavedStateHandle`.

**13. O que é `StateFlow` e `repeatOnLifecycle(STARTED)`?**
O `StateFlow` é um valor observável: quem coleta recebe o valor atual e cada atualização. O
`repeatOnLifecycle(STARTED)` coleta **só enquanto a tela está visível**. Quando ela vai para
segundo plano, a coleta para e retoma sozinha ao voltar, o que evita trabalho à toa e
vazamentos de memória.

**14. Como o modo Aluno e o modo Coach enxergam os mesmos dados?**
Existe **um único** `GymRepository`, criado no `AppTreinoApplication` e entregue aos ViewModels
pelas fábricas (`viewModelFactory`), uma injeção de dependências manual. Quando o Leonardo
registra uma série, o `StateFlow` do repositório emite um novo estado e o painel do coach se
atualiza. O mesmo vale ao contrário.

**15. Por que as data classes são imutáveis?**
Todas as propriedades são `val` e as listas são somente leitura. Para mudar algo, cria-se uma
cópia (`copy()`). Isso evita bugs de "alguém alterou a lista sem avisar" e é o que faz o
`DiffUtil` e o `StateFlow` funcionarem: um estado novo é um objeto novo.

**16. Por que não usar banco de dados ou API?**
É uma regra do trabalho: tudo é mockado e fica em memória, e ao fechar o app os dados voltam ao
estado inicial. A arquitetura facilita a troca: bastaria o `GymRepository` ler e gravar no
Room ou numa API, sem mudar as telas.

**17. Para que serve o `UiText`?**
O professor exige que os textos fiquem em `strings.xml`, mas o coach também digita textos (nome
de aluno, nome de treino). O `UiText` tem dois casos:
- `Resource`: texto do `strings.xml`, usado pelos mocks;
- `Dynamic`: texto digitado pelo usuário.

A tela converte o `UiText` em texto com `resolve()`.

**18. Como o histórico mockado é gerado?**
Em `MockData.kt`, com um `Random` de **semente fixa**: a cada execução os números são os mesmos
(há um teste que garante isso). A carga das semanas antigas é um pouco menor, com pequenas
oscilações, para o gráfico de evolução ficar realista.

---

## Regras de negócio

**19. Quando um exercício fica "feito"?**
Quando **todas as séries** prescritas foram registradas (`ProgressCalculator.isExerciseDone`).
Por isso registrar a última série marca o exercício sozinho. O check do card é um atalho que
registra as séries que faltam com os valores do coach.

**20. Como funciona a comparação "coach × aluno"?**
Em `SetComparator`. Nas repetições vale a **faixa** pedida: se o coach pediu 8–10, então 9 está
no alvo, 7 está abaixo e 11 está acima. Na carga, compara-se com o valor prescrito. A tela
mostra selos verde-limão (acima), neutros (no alvo) ou laranja (abaixo).

**21. Como é calculada a sequência de treinos?**
A contagem volta de hoje para trás, somando os dias de treino concluídos. Dias de descanso não
quebram a sequência, e o treino de hoje ainda em andamento também não. Um treino perdido
encerra a contagem (`ProgressCalculator.streak`).

**22. Por que um treino por dia no máximo?**
Para a aba Hoje saber exatamente qual treino mostrar. O cardio faz parte do treino do dia. No
editor, os dias ocupados por outro treino aparecem bloqueados, e o `WorkoutValidator` também
rejeita conflitos.

**23. Onde ficam as validações?**
Na camada `domain` (`StudentValidator`, `WorkoutValidator`, `PrescriptionValidator`,
`CardioValidator`), em funções puras e testadas. Elas devolvem o **ID da mensagem** em
`strings.xml`, e a tela só exibe o erro no campo certo. Números negativos, vazios ou fora da
faixa são rejeitados.

---

## Visual

**24. Como o tema escuro com verde-limão foi feito?**
Um único tema Material 3 DayNight. As cores estão em `values/colors.xml` (claro) e
`values-night/colors.xml` (escuro), e o app inicia no escuro (`AppSettings`). As cores são fixas:
não há Dynamic Color, e o *elevation overlay* do Material 3 foi desligado porque misturava o
verde-limão nas superfícies e deixava a barra inferior verde-oliva.

**25. O que é edge-to-edge e como foi tratado?**
O app desenha atrás da status bar e da barra de navegação. Cada tela soma o tamanho dessas
barras ao próprio padding (`applySystemBarsPadding` em `ui/common/InsetsExtensions.kt`), e o
conteúdo rolável é cortado antes do relógio. Nos formulários, o teclado também entra no cálculo.

**26. Por que o gráfico foi feito à mão?**
Para não depender de biblioteca externa e mostrar domínio de View customizada. `LoadChartView`
estende `View` e desenha linha, pontos, degradê e linhas de referência com `Canvas` e `Paint`.

**27. Para que servem os *plurals* e os textos com formatação?**
Para o português ficar correto: "1 exercício" e "2 exercícios" (`getQuantityString`). Números e
datas usam placeholders (`%1$d`) em vez de concatenação, como recomenda o lint.

**28. E a acessibilidade?**
- Imagens e botões de ícone têm `contentDescription` com o nome do exercício.
- As áreas de toque têm no mínimo 48dp.
- Os textos respeitam o tamanho de fonte do sistema (sp).

---

## Build e qualidade

**29. Por que o plugin Kotlin 2.4.20 aparece no `build.gradle.kts` da raiz?**
O AGP 9 já traz o Kotlin embutido, mas numa versão (2.2) que não lê as bibliotecas AndroidX e
Coil mais recentes, compiladas com Kotlin 2.4. Declarar o plugin com `apply false` só fixa a
versão do compilador.

**30. Como o projeto foi testado?**
Com **testes unitários** (JUnit) que rodam no computador, sem emulador. Eles cobrem:
- catálogo e mocks;
- repositório;
- progresso, sequência e evolução;
- comparação coach × aluno;
- validações e estados de tela;
- regras de DiffUtil.

As regras estão na camada `domain`, sem Android, justamente para serem testáveis. O **lint** do
Android roda sem nenhum aviso.

**31. O projeto roda em outro computador sem alterações?**
Sim:
- o Gradle Wrapper baixa a versão certa do Gradle;
- as bibliotecas vêm de repositórios públicos;
- arquivos específicos da máquina (`local.properties`, `.idea`, `build`) não vão para o git.

O projeto foi validado clonando o repositório numa pasta nova, com um cache do Gradle vazio, e
rodando build e testes do zero.
