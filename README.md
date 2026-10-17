# distributed-cache

> Serviço de consulta de saldo e limite de contas com cache distribuído Redis (padrão Cache-Aside).

---

### Sobre
O **distributed-cache** expõe uma API para consultar saldo e limite de contas. Para reduzir a latência e a carga no banco de dados, as leituras passam por um cache distribuído em Redis usando o padrão **Cache-Aside**:

1. A aplicação consulta o Redis.
2. **Hit:** retorna direto do cache.
3. **Miss:** busca na fonte da verdade, grava no Redis com TTL e retorna.

### Stack
- Java 21
- Spring Boot 4.0.8
- Redis 7.4
- Docker Compose
- PostgreSQL 17
- Flyway

### Pré-requisitos
- JDK 21
- Docker e Docker Compose

### Como executar
```bash
# 1. Subir o Redis
docker compose up -d

# 2. Rodar a aplicação
./mvnw spring-boot:run

# 3. Verificar a saúde
curl http://localhost:8080/actuator/health
```

### Testes
```bash
./mvnw verify
```

### Arquitetura
Arquitetura hexagonal (ports & adapters), com o domínio isolado de frameworks:

```
domain → application → adapter (in/out) → infrastructure
```

### API

`GET /api/v1/accounts/{accountId}/summary`

Retorna saldo, limite contratado, limite utilizado, limite disponível e total disponível da conta.

| Status | Situação |
|---|---|
| 200 | Conta encontrada |
| 400 | `accountId` não é um UUID válido |
| 404 | Conta não encontrada |
| 500 | Erro inesperado |

Os erros seguem o padrão RFC 9457 (`application/problem+json`).

### Executando com dados de exemplo

```bash
docker-compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
curl http://localhost:8080/api/v1/accounts/22222222-2222-2222-2222-222222222222/summary
```

### Cache

Leituras passam por Redis com o padrão Cache-Aside. As chaves são versionadas (`dcache:account:v1:{accountId}`) e expiram após o TTL configurado.

| Propriedade | Padrão | Descrição |
|---|---|---|
| `dcache.cache.enabled` | `true` | Desativa o cache quando `false` |
| `dcache.cache.ttl` | `5m` | Tempo de vida de cada entrada |

Exemplo para rodar sem cache:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments=--dcache.cache.enabled=false
```

### Roadmap
- [x] Fase 0: scaffold, Redis local, CI
- [x] Fase 1: domínio
- [x] Fase 2: casos de uso e ports
- [x] Fase 3: persistência (PostgreSQL + Flyway)
- [x] Fase 4: API REST
- [x] Fase 5: Cache-Aside com Redis
- [ ] Fase 6: TTL, invalidação e resiliência
- [ ] Fase 7: observabilidade
- [ ] Fase 8: testes de integração (Testcontainers)
- [ ] Fase 9: Docker e documentação final
