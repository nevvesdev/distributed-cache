# Decisões de arquitetura

Registro das principais decisões do projeto, com o contexto e as consequências de cada uma.

## 1. Arquitetura hexagonal com regras verificadas por teste

**Contexto:** as regras de negócio não devem depender de Spring, Redis ou JPA, e trocar uma tecnologia de infraestrutura não pode exigir mexer no domínio.

**Decisão:** o código é dividido em `domain`, `application`, `adapter` e `infrastructure`. As dependências só apontam para dentro, e um teste com ArchUnit quebra o build se alguém violar isso (por exemplo, importar Spring no domínio ou o Micrometer na aplicação).

**Consequências:** há mais classes (portas, mappers, decorators), em troca de isolamento, testabilidade e uma arquitetura que não se degrada com o tempo.

## 2. Cache-Aside orquestrado no caso de uso

**Contexto:** o padrão Cache-Aside diz que a aplicação decide quando consultar o cache, o banco e quando popular o cache.

**Decisão:** o `GetAccountSummaryService` conduz a sequência (cache, depois banco, depois escrita no cache). O Redis é apenas um adaptador da porta `AccountCache`.

**Consequências:** o fluxo fica explícito e é testado com fakes, sem Redis. Trocar a tecnologia de cache não altera a regra.

## 3. Formato de cache próprio e chaves versionadas

**Contexto:** o formato guardado no Redis não deve mudar por acidente quando a configuração de JSON da API mudar, e dinheiro não pode perder precisão.

**Decisão:** o adaptador usa um DTO interno (`CachedAccount`, com valores em texto) e um `JsonMapper` dedicado. As chaves seguem `dcache:account:v1:{id}`. Entradas ilegíveis ou com dados inválidos são descartadas e tratadas como *miss*.

**Consequências:** mudar o formato exige subir a versão da chave, e as entradas antigas expiram sozinhas. Um Redis com dados corrompidos nunca derruba uma leitura.

## 4. Degradação graciosa com circuit breaker próprio

**Contexto:** o cache é uma otimização. Se o Redis cair, a consulta de saldo deve continuar funcionando, e sem esperar o timeout a cada requisição.

**Decisão:** o `FailSafeAccountCache` transforma falhas de leitura em *miss* e ignora falhas de escrita. Um circuit breaker pequeno (falhas consecutivas, janela aberta, uma única tentativa de recuperação) evita chamar o Redis enquanto ele está fora. O relógio é injetável, o que torna os testes determinísticos.

**Consequências:** há código de resiliência próprio para manter, com escopo restrito ao cache. Em contrapartida, não há dependência extra e o comportamento está totalmente coberto por testes.

## 5. Invalidação nunca é silenciosa

**Contexto:** uma leitura que falha pode degradar sem problemas, mas uma invalidação perdida deixa dado antigo no cache sem ninguém saber.

**Decisão:** falhas na remoção explícita viram `CacheUnavailableException` e a API responde `503`. A invalidação também ignora o circuit breaker, o que a faz verificar se o Redis já voltou.

**Consequências:** quem chama o endpoint sempre sabe se a entrada foi removida.

## 6. Proteção contra cache stampede por instância

**Contexto:** quando uma entrada popular expira, muitas requisições simultâneas dão *miss* e atingem o banco ao mesmo tempo.

**Decisão:** o `SingleFlightAccountRepository` faz a primeira requisição consultar o banco enquanto as demais aguardam o mesmo resultado. Se a consulta falha, todas recebem a falha, e a próxima chamada tenta de novo.

**Consequências:** com várias instâncias da aplicação, cada uma faz no máximo uma consulta por conta, o que resolve o problema sem lock distribuído.

## 7. Dinheiro como value object estrito

**Contexto:** erros de arredondamento em valores monetários são difíceis de rastrear.

**Decisão:** `Money` mantém sempre duas casas decimais e rejeita entradas com mais casas, em vez de arredondar em silêncio. `Balance` e `CreditLimit` são tipos distintos, e o limite nunca é negativo.

**Consequências:** dados inválidos falham na borda, inclusive os que vêm do banco ou do cache.

## 8. Observabilidade por decorators na infraestrutura

**Contexto:** a camada de aplicação não pode depender de bibliotecas de métricas.

**Decisão:** decorators na camada `infrastructure` medem o cache e o banco. A ordem importa: o contador de cache fica dentro do `FailSafeAccountCache`, para enxergar as falhas reais do Redis, e o do banco fica dentro do `SingleFlightAccountRepository`, para contar só as consultas que de fato chegam ao PostgreSQL.

**Consequências:** as métricas refletem o comportamento real, e o estado do circuit breaker é exposto como gauge.
