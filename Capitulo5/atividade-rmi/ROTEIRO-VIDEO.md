# Roteiro do vídeo — aproximadamente 8 a 10 minutos

Prepare o editor aberto nesta pasta, o código compilado e quatro terminais. Use fonte grande e deixe os dois assinantes visíveis ao publicar. Os comandos completos estão no README. Não é necessário digitar a compilação do zero durante toda a gravação; mostre o comando e o resultado.

## 0:00–0:45 — Apresentação

**Mostrar:** título do trabalho e identificação dos integrantes.

**Fala sugerida:** “Este é o Trabalho 2 de Sistemas Distribuídos. Reimplementamos a autenticação e as notificações da biblioteca do Trabalho 1 usando Java RMI. A solução funciona em modo texto. Os clientes se autenticam, recebem uma referência para sua sessão e recebem notificações por callbacks remotos.”

## 0:45–2:00 — Entidades e requisitos

**Mostrar:** `Usuario`, `Aluno`, `Funcionario`, `Notificacao` e `Canal`.

**Fala:** “Temos cinco entidades. Aluno é um usuário e funcionário é um usuário: essas são as duas relações por extensão. Uma notificação tem um autor e o canal tem uma coleção de notificações: essas são as duas agregações. Os dados ficam em memória.”

**Mostrar:** as três interfaces remotas.

**Fala:** “As interfaces estendem Remote e declaram RemoteException. A autenticação retorna uma sessão remota; o callback também é uma referência remota. Já uma notificação é convertida para JSON e reconstruída como um novo objeto no servidor, demonstrando passagem por valor.”

## 2:00–3:15 — Protocolo

**Mostrar:** `Cliente.doOperation`, `Sessao.doOperation`, `getRequest` e `sendReply` no servidor; exemplo de envelope no relatório.

**Fala:** “Cada mensagem leva tipo, identificador, referência lógica do serviço, método e argumentos. O cliente gera um UUID e confere se a resposta devolve o mesmo identificador. O servidor obtém e decodifica a requisição, despacha o método e monta a resposta. Adaptamos as assinaturas sugeridas pelo livro porque o RMI já entrega a chamada e devolve o resultado ao cliente. As quatro operações são perfil, publicar, histórico e sair.”

**Fala:** “A representação externa é JSON, uma das opções aceitas pelo enunciado. O campo de argumentos e o campo de resultado carregam JSON como texto. O transporte é administrado pelo RMI; o código da aplicação não cria sockets.”

## 3:15–4:15 — Compilar e iniciar

**Mostrar:** `java -version`, `javac -version` e compilação indicada no README.

**Terminal 1:**

```text
java -cp target/classes biblioteca.Servidor
```

**Fala:** “O servidor cria o registro na porta 1099 e exporta os objetos de serviço na porta 1100. Não precisamos executar rmiregistry separadamente.”

**Terminais 2 e 3, em cada um:**

```text
java -cp target/classes biblioteca.Cliente 127.0.0.1 aluno 1234
```

**Terminal 4:**

```text
java -cp target/classes biblioteca.Cliente 127.0.0.1 admin 1234
```

## 4:15–5:45 — Demonstrar distribuição

**Digitar no funcionário, esperando os callbacks entre as linhas:**

```text
perfil
publicar NOTIFICACAO Novo livro disponível
publicar ALERTA Biblioteca fecha em 10 minutos
publicar ATUALIZACAO Catálogo atualizado
```

**Mostrar:** cada mensagem nos dois terminais de alunos.

**Fala:** “Os dois assinantes recebem os três tipos sem fazer consultas periódicas. O servidor chama o método receber do objeto remoto exportado por cada cliente. O pool de threads permite entregas concorrentes. Isso substitui o multicast UDP do trabalho anterior por distribuição via RMI.”

## 5:45–6:45 — Consultas e falhas

**Digitar em um aluno:**

```text
perfil
historico
publicar ALERTA Tentativa sem autorização
inexistente
```

**Fala:** “Perfil e histórico retornam dados externos em JSON. O aluno recebe erro ao publicar porque essa operação exige funcionário. Um método desconhecido também retorna erro no protocolo, preservando o identificador da requisição.”

**Em um terminal adicional:**

```text
java -cp target/classes biblioteca.Cliente 127.0.0.1 aluno errada
```

**Fala:** “Com senha incorreta, a autenticação é recusada e nenhuma sessão é entregue.”

## 6:45–7:30 — Saída e teste

**Digitar `sair` em um dos alunos. Publicar no funcionário:**

```text
publicar NOTIFICACAO Apenas assinantes ativos recebem
```

**Mostrar:** o outro aluno recebe, o cliente encerrado não participa mais.

Encerre os demais clientes com `sair` e o servidor com `Ctrl+C`. Opcionalmente, rode `python3 validar.py` ou `py -3 validar.py` para mostrar a validação automatizada com dois assinantes. O teste precisa das portas livres.

## 7:30–8:30 — Limitações e encerramento

**Mostrar:** seção de execução em rede do README e conclusão do relatório.

**Fala:** “Para executar em máquinas diferentes, informamos o IP do servidor e o IP do cliente, porque o callback exige comunicação de volta ao cliente. As portas precisam estar acessíveis. Nesta validação usamos processos separados na mesma máquina. O histórico é volátil, as credenciais são demonstrativas e não garantimos a ordem dos callbacks. O requestId correlaciona requisição e resposta, mas não garante execução exatamente uma vez. O repositório contém o código, o relatório e as instruções para reproduzir a demonstração.”

Antes de entregar: preencher nomes e matrículas no relatório, incluir o URL do repositório e enviar os materiais solicitados no Moodle. Este roteiro não substitui o relatório pedido pelo PDF.
