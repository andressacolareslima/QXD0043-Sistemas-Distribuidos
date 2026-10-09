# Serviço remoto de biblioteca — questões 1 a 4

Solução em Java 17+, sem bibliotecas externas. POJOs: `Livro` e `Usuario`,
com construtor vazio, atributos privados, getters e setters.
Serviços: `CatalogoService` (cadastrar, buscar, listar) e
`AutenticacaoService` (autenticar). O catálogo fica em memória.

## Compilar e testar

Na pasta `Capitulo4/atividade-biblioteca`:

```bash
mkdir -p target/classes
javac -encoding UTF-8 -d target/classes src/biblioteca/*.java
java -cp target/classes biblioteca.TesteIntegrado
```

O teste verifica quantidade parcial, cabeçalho inválido, arquivo, streams via TCP,
serialização request/reply via TCP, credenciais e codificação de notificações.
O multicast real deve ser demonstrado com os terminais abaixo.

## 1 e 2 — OutputStream e InputStream

`LivroOutputStream` estende `OutputStream` e recebe destino, array de livros e
quantidade. `enviar()` escreve o conjunto e faz flush. `LivroInputStream` estende
`InputStream`, recebe a origem e reconstrói o conjunto em `lerLivros()`.
As classes também implementam as operações básicas de bytes e fechamento.
Os POJOs permanecem classes de dados; as subclasses de stream são classes
separadas, nomeadas de acordo com o POJO escolhido.

Formato binário: cabeçalho `0x42494231`, quantidade, e, para cada livro,
id inteiro, título e autor em UTF modificado do Java. Inteiros usam big-endian.
`readFully`/`DataInputStream` permitem ler mensagens fragmentadas pelo TCP.
Quantidade negativa, acima do array ou acima de 10.000 é rejeitada.

### 1b e 2b: saída e entrada padrão

```bash
java -cp target/classes biblioteca.Main stdout | java -cp target/classes biblioteca.Main stdin
```

O primeiro processo usa `System.out`; o segundo usa `System.in` e imprime
os dois livros reconstruídos. Os bytes de `stdout` são binários, por isso
não se deve digitá-los manualmente nem esperar texto legível nesse comando.
Também é possível redirecionar:

```bash
java -cp target/classes biblioteca.Main stdout > livros.bin
java -cp target/classes biblioteca.Main stdin < livros.bin
```

### 1c e 2c: arquivo

```bash
java -cp target/classes biblioteca.Main arquivo-escrever livros.bin
java -cp target/classes biblioteca.Main arquivo-ler livros.bin
```

Usam respectivamente `FileOutputStream` e `FileInputStream`.

### 1d e 2d: TCP

Terminal do servidor:

```bash
java -cp target/classes biblioteca.Main streams-servidor
```

Terminal do cliente:

```bash
java -cp target/classes biblioteca.Main streams-cliente localhost
```

Porta 5000. O cliente envia dois livros e o servidor os lê e imprime.
Esse servidor encerra após atender um cliente; inicie-o novamente para repetir.
Substitua `localhost` pelo IP do servidor para usar máquinas diferentes.

## 3 — serialização e comunicação cliente-servidor

Terminal do servidor:

```bash
java -cp target/classes biblioteca.Main rpc-servidor
```

Terminal do cliente:

```bash
java -cp target/classes biblioteca.Main rpc-cliente localhost
```

Porta 5001. O cliente serializa um request `CADASTRAR` contendo um livro.
O servidor desserializa, executa o serviço, serializa um reply com sucesso,
mensagem e livro, e o cliente desserializa e imprime a resposta.
`BUSCAR` também está implementado: recebe um livro com o id desejado.

`Protocolo` faz representação externa binária explícita, sem serialização
nativa de objetos. Cada mensagem TCP tem tamanho inteiro seguido pelo payload.
Request: operação UTF + livro. Reply: sucesso booleano, mensagem UTF,
booleano indicando presença de livro e, se presente, livro.
Os tamanhos são limitados a 1 MiB. O servidor atende clientes em um pool de
threads e o catálogo usa `ConcurrentHashMap`.

## 4 — autenticação TCP e notificações UDP multicast

Terminal do servidor:

```bash
java -cp target/classes biblioteca.Main multicast-servidor
```

Abra pelo menos dois terminais de clientes e execute em cada um:

```bash
java -cp target/classes biblioteca.Main multicast-cliente localhost aluno 1234
```

O cliente autentica por TCP na porta 5003. Se autorizado, recebe o grupo
classe D `230.0.0.1` e a porta UDP fixa 5002, e chama `joinGroup`.
A reutilização da porta permite vários clientes na mesma máquina.
Se necessário, passe o nome da interface de rede como último argumento:

```bash
java -cp target/classes biblioteca.Main multicast-cliente localhost aluno 1234 eth0
```

No console do servidor, envie:

```text
NOTIFICACAO|Novo livro disponível
ALERTA|Biblioteca fecha em 10 minutos
ATUALIZACAO|Catálogo atualizado
```

Os dois clientes mostram as mensagens em tempo real. A representação é binária:
`tipo` UTF, `mensagem` UTF e `timestamp` long em milissegundos desde a época Unix.
Exemplo exibido: `[NOTIFICACAO] Novo livro disponível (timestamp=...)`.
O servidor usa um pool para enviar mensagens concorrentemente e outro para
as autenticações. Cada cliente tem uma thread de escuta e outra de interação.
Digite `sair` no cliente para executar `leaveGroup`, fechar o socket e encerrar
as threads. Digite `sair` no servidor para encerrar seus recursos.

Para demonstrar recusa, execute um cliente com senha `errada`.
As credenciais `aluno`/`1234` são demonstrativas. A autenticação controla o
join feito por este cliente; UDP multicast não impede outro programa de
entrar diretamente no grupo nem oferece confidencialidade das mensagens.
O tráfego multicast depende de suporte da interface e da rede; libere as
portas e use máquinas na mesma rede para a demonstração remota.

## Validação realizada

Compilado com JDK 17. O teste integrado passou. Também foram executados dois
clientes multicast simultâneos, que receberam os três tipos de mensagem;
a senha incorreta foi recusada e ambos os clientes encerraram após `sair`.
O fluxo `System.out` → `System.in` também foi executado com sucesso.
Nesta validação, o JDK foi baixado temporariamente em `/tmp/biblioteca-jdk`,
pois não havia Java instalado no PATH do ambiente.
